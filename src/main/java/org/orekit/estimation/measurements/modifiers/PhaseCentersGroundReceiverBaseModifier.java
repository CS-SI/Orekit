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

import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundReceiverMeasurement;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Ground and on-board antennas offsets effect on range measurements.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 12.0
 */
public class PhaseCentersGroundReceiverBaseModifier<T extends GroundReceiverMeasurement<T>> {

    /** Uplink offset model. */
    private final PhaseCentersOffsetComputer uplink;

    /** Downlink offset model. */
    private final PhaseCentersOffsetComputer downlink;

    /** Simple constructor.
     * @param stationPattern station pattern
     * @param satellitePattern satellite pattern
     */
    public PhaseCentersGroundReceiverBaseModifier(final FrequencyPattern stationPattern,
                                                  final FrequencyPattern satellitePattern) {
        this.uplink   = new PhaseCentersOffsetComputer(stationPattern, satellitePattern);
        this.downlink = new PhaseCentersOffsetComputer(satellitePattern, stationPattern);
    }

    /** Compute distance modification for one way measurement.
     * @param estimated estimated measurement to modify
     * @return distance modification to add to raw measurement
     */
    public double oneWayDistanceModification(final EstimatedMeasurementBase<T> estimated) {

        // get all participants
        // note that clock offset is compensated in participants,
        // so the dates included there are more accurate than the measurement date
        final TimeStampedPVCoordinates[] participants = estimated.getParticipants();

        // station at reception date
        final Frame         inertial       = estimated.getStates()[0].getFrame();
        final GroundStation station        = estimated.getObservedMeasurement().getStation();
        final AbsoluteDate  receptionDate  = participants[1].getDate();
        final Transform     stationToInert = station.getOffsetToInertial(inertial, receptionDate, false);

        // spacecraft at emission date
        final AbsoluteDate    emissionDate      = participants[0].getDate();
        final SpacecraftState refState          = estimated.getStates()[0];
        final SpacecraftState emissionState     = refState.shiftedBy(emissionDate.durationFrom(refState.getDate()));
        final Transform       spacecraftToInert = emissionState.toTransform().getInverse();

        // compute offset due to phase centers
        return downlink.offset(spacecraftToInert, stationToInert);

    }

    /** Apply a modifier to a two-way range measurement.
     * @param estimated estimated measurement to modify
     * @return distance modification to add to raw measurement
     */
    public double twoWayDistanceModification(final EstimatedMeasurementBase<T> estimated) {

        // get all participants
        // note that clock offset is compensated in participants,
        // so the dates included there are more accurate than the measurement date
        final TimeStampedPVCoordinates[] participants = estimated.getParticipants();

        // station at reception date
        final Frame         inertial                = estimated.getStates()[0].getFrame();
        final GroundStation station                 = estimated.getObservedMeasurement().getStation();
        final AbsoluteDate  receptionDate           = participants[2].getDate();
        final Transform     stationToInertReception = station.getOffsetToInertial(inertial, receptionDate, false);

        // transform from spacecraft to inertial frame at transit date
        final AbsoluteDate    transitDate           = participants[1].getDate();
        final SpacecraftState refState              = estimated.getStates()[0];
        final SpacecraftState transitState          = refState.shiftedBy(transitDate.durationFrom(refState.getDate()));
        final Transform       spacecraftToInert     = transitState.toTransform().getInverse();

        // station at emission date
        final AbsoluteDate emissionDate             = participants[0].getDate();
        final Transform    stationToInertEmission   = station.getOffsetToInertial(inertial, emissionDate, true);

        // compute offsets due to phase centers
        final double uplinkOffset   = uplink.offset(stationToInertEmission, spacecraftToInert);
        final double downlinkOffset = downlink.offset(spacecraftToInert, stationToInertReception);

        return 0.5 * (uplinkOffset + downlinkOffset);

    }

}
