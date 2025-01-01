/* Copyright 2002-2025 CS GROUP
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
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.InterSatellitesRange;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.utils.ParameterDriver;

/** On-board antenna offset effect on inter-satellites range measurements.
 * @author Luc Maisonobe
 * @since 9.0
 */
public class OnBoardAntennaInterSatellitesRangeModifier
    extends PhaseCentersInterSatellitesBaseModifier<InterSatellitesRange>
    implements EstimationModifier<InterSatellitesRange> {

    /** Simple constructor.
     * @param receiverPhaseCenter position of the Antenna Phase Center in emitter satellite frame
     * @param emitterPhaseCenter position of the Antenna Phase Center in receiver satellite frame
     */
    public OnBoardAntennaInterSatellitesRangeModifier(final Vector3D receiverPhaseCenter,
                                                      final Vector3D emitterPhaseCenter) {
        this(new FrequencyPattern(receiverPhaseCenter, null),
             new FrequencyPattern(emitterPhaseCenter, null));
    }

    /** Simple constructor.
     * @param receiverPattern pattern for receiver satellite
     * @param emitterPattern  pattern for emitter satellite
     * @since 12.1
     */
    public OnBoardAntennaInterSatellitesRangeModifier(final FrequencyPattern receiverPattern,
                                                      final FrequencyPattern emitterPattern) {
        super(receiverPattern, emitterPattern);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<InterSatellitesRange> estimated) {
        final double delta = estimated.getParticipants().length < 3 ?
                             oneWayDistanceModification(estimated) :
                             twoWayDistanceModification(estimated);
        estimated.modifyEstimatedValue(this, estimated.getEstimatedValue()[0] + delta);
    }

}
