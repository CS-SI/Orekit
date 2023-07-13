/* Copyright 2023 Luc Maisonobe
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.estimation.measurements.modifiers;

import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.gnss.Phase;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.gnss.antenna.PhaseCenterVariationFunction;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Ground and on-board antennas offsets effect on phase measurements.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class PhaseCentersPhaseModifier implements EstimationModifier<Phase> {

    /** Downlink offset model. */
    private final PhaseCentersOffsetComputer downlink;

    /** Simple constructor.
     * @param stationMeanPosition mean position of the station Antenna Phase Center in station frame
     * @param stationPhaseCenterVariation station phase center variation model in station frame (may be null for no variation)
     * @param satelliteMeanPosition mean position of the satellite Antenna Phase Center in satellite frame
     * @param satellitePhaseCenterVariation satellite phase center variation model in satellite frame (may be null for no variation)
     */
    public PhaseCentersPhaseModifier(final Vector3D stationMeanPosition,
                                     final PhaseCenterVariationFunction stationPhaseCenterVariation,
                                     final Vector3D satelliteMeanPosition,
                                     final PhaseCenterVariationFunction satellitePhaseCenterVariation) {
        this.downlink = new PhaseCentersOffsetComputer(satelliteMeanPosition, satellitePhaseCenterVariation,
                                                       stationMeanPosition, stationPhaseCenterVariation);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<Phase> estimated) {
        // get all participants
        // note that clock offset is compensated in participants,
        // so the dates included there are more accurate than the measurement date
        final TimeStampedPVCoordinates[] participants = estimated.getParticipants();

        // station at reception date
        final Frame         inertial       = estimated.getStates()[0].getFrame();
        final GroundStation station        = estimated.getObservedMeasurement().getStation();
        final AbsoluteDate  receptionDate  = participants[1].getDate();
        final Transform     stationToInert = station.getOffsetToInertial(inertial, receptionDate, true);

        // spacecraft at emission date
        final AbsoluteDate    emissionDate      = participants[0].getDate();
        final SpacecraftState refState          = estimated.getStates()[0];
        final SpacecraftState emissionState     = refState.shiftedBy(emissionDate.durationFrom(refState.getDate()));
        final Transform       spacecraftToInert = emissionState.toTransform().getInverse();

        // compute offset due to phase centers
        final double downlinkOffset = downlink.offset(spacecraftToInert, stationToInert);

        // get the estimated value before this modifier is applied
        final double[] value = estimated.getEstimatedValue();

        // modify the value
        value[0] += downlinkOffset;
        estimated.setEstimatedValue(value);

    }

}
