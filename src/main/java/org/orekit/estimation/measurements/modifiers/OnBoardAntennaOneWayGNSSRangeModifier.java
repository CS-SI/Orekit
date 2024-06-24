/* Copyright 2002-2024 CS GROUP
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
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.gnss.OneWayGNSSRange;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.utils.ParameterDriver;

/** On-board antenna offset effect on one-way GNSS range measurements.
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class OnBoardAntennaOneWayGNSSRangeModifier
    extends PhaseCentersOneWayGNSSBaseModifier<OneWayGNSSRange>
    implements EstimationModifier<OneWayGNSSRange> {

    /** Simple constructor.
     * @param receiverPhaseCenter position of the Antenna Phase Center in satellite 1 frame
     * (i.e. the satellite which receives the signal and performs the measurement)
     * @param emitterPhaseCenter position of the Antenna Phase Center in satellite 2 frame
     * (i.e. the satellite which simply emits the signal)
     * @param attitudeProvider attitude provider of the emitting satellite
     */
    public OnBoardAntennaOneWayGNSSRangeModifier(final Vector3D receiverPhaseCenter,
                                                 final Vector3D emitterPhaseCenter,
                                                 final AttitudeProvider attitudeProvider) {
        this(new FrequencyPattern(receiverPhaseCenter, null),
             new FrequencyPattern(emitterPhaseCenter, null),
             attitudeProvider);
    }

    /** Simple constructor.
     * @param receiverPattern pattern for receiver satellite
     * @param emitterPattern  pattern for emitter satellite
     * @param attitudeProvider attitude provider of the emitting satellite
     * @since 12.1
     */
    public OnBoardAntennaOneWayGNSSRangeModifier(final FrequencyPattern receiverPattern,
                                                 final FrequencyPattern emitterPattern,
                                                 final AttitudeProvider attitudeProvider) {
        super(receiverPattern, emitterPattern, attitudeProvider);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<OneWayGNSSRange> estimated) {
        estimated.modifyEstimatedValue(this,
                                       estimated.getEstimatedValue()[0] +
                                       oneWayDistanceModification(estimated));
    }

}
