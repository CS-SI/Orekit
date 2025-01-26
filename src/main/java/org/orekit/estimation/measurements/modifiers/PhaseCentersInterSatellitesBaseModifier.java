/* Copyright 2022-2025 Thales Alenia Space
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

import org.orekit.estimation.measurements.AbstractMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.frames.StaticTransform;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** On-board antenna offset effect on inter-satellites phase measurements.
 * @param <T> type of the measurement
 * @author Luc Maisonobe
 * @since 12.1
 */
public class PhaseCentersInterSatellitesBaseModifier<T extends AbstractMeasurement<T>> {

    /** Uplink offset model. */
    private final PhaseCentersOffsetComputer uplink;

    /** Downlink offset model. */
    private final PhaseCentersOffsetComputer downlink;

    /** Simple constructor.
     * @param pattern1 pattern for satellite 1
     * (i.e. the satellite which receives the signal and performs the measurement)
     * @param pattern2  pattern for satellite 2
     * (i.e. the satellite which simply emits the signal in the one-way
     * case, or reflects the signal in the two-way case)
     */
    public PhaseCentersInterSatellitesBaseModifier(final FrequencyPattern pattern1,
                                                   final FrequencyPattern pattern2) {
        this.uplink   = new PhaseCentersOffsetComputer(pattern1, pattern2);
        this.downlink = new PhaseCentersOffsetComputer(pattern2, pattern1);
    }

    /** Get the name of the effect modifying the measurement.
     * @return name of the effect modifying the measurement
     * @since 13.0
     */
    public String getEffectName() {
        return "mean phase center";
    }

    /** Compute distance modification for one way measurement.
     * @param estimated estimated measurement to modify
     * @return distance modification to add to raw measurement
     */
    public double oneWayDistanceModification(final EstimatedMeasurementBase<T> estimated) {

        // The participants are satellite 2 at emission, satellite 1 at reception
        final TimeStampedPVCoordinates[] participants  = estimated.getParticipants();
        final AbsoluteDate               emissionDate  = participants[0].getDate();
        final AbsoluteDate               receptionDate = participants[1].getDate();

        // transforms from spacecraft to inertial frame at emission/reception dates
        final SpacecraftState localState                 = estimated.getStates()[0];
        final SpacecraftState receptionState             = localState.shiftedBy(receptionDate.durationFrom(localState.getDate()));
        final StaticTransform receptionSpacecraftToInert = receptionState.toStaticTransform().getInverse();
        final SpacecraftState remoteState                = estimated.getStates()[1];
        final SpacecraftState emissionState              = remoteState.shiftedBy(emissionDate.durationFrom(remoteState.getDate()));
        final StaticTransform emissionSpacecraftToInert  = emissionState.toStaticTransform().getInverse();

        // compute offset due to phase centers
        return downlink.offset(emissionSpacecraftToInert, receptionSpacecraftToInert);

    }

    /** Compute distance modification for two way measurement.
     * @param estimated estimated measurement to modify
     * @return distance modification to add to raw measurement
     */
    public double twoWayDistanceModification(final EstimatedMeasurementBase<InterSatellitesRange> estimated) {

        // the participants are satellite 1 at emission, satellite 2 at transit, satellite 1 at reception
        final TimeStampedPVCoordinates[] participants  = estimated.getParticipants();
        final AbsoluteDate               emissionDate  = participants[0].getDate();
        final AbsoluteDate               transitDate   = participants[1].getDate();
        final AbsoluteDate               receptionDate = participants[2].getDate();

        // transforms from spacecraft to inertial frame at emission/reception dates
        final SpacecraftState refState1                  = estimated.getStates()[0];
        final SpacecraftState receptionState             = refState1.shiftedBy(receptionDate.durationFrom(refState1.getDate()));
        final StaticTransform receptionSpacecraftToInert = receptionState.toStaticTransform().getInverse();
        final SpacecraftState refState2                  = estimated.getStates()[1];
        final SpacecraftState transitState               = refState2.shiftedBy(transitDate.durationFrom(refState2.getDate()));
        final StaticTransform transitSpacecraftToInert   = transitState.toStaticTransform().getInverse();
        final SpacecraftState emissionState              = refState1.shiftedBy(emissionDate.durationFrom(refState1.getDate()));
        final StaticTransform emissionSpacecraftToInert  = emissionState.toStaticTransform().getInverse();

        // compute offsets due to phase centers
        final double uplinkOffset   = uplink.offset(emissionSpacecraftToInert, transitSpacecraftToInert);
        final double downlinkOffset = downlink.offset(transitSpacecraftToInert, receptionSpacecraftToInert);

        return 0.5 * (uplinkOffset + downlinkOffset);

    }

}
