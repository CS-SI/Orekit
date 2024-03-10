/* Copyright 2002-2024 Luc Maisonobe
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

import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.Range;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.utils.ParameterDriver;

/** Ground and on-board antennas offsets effect on range measurements.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class PhaseCentersRangeModifier
    extends PhaseCentersGroundReceiverBaseModifier<Range>
    implements EstimationModifier<Range> {

    /** Simple constructor.
     * @param stationPattern station pattern
     * @param satellitePattern satellite pattern
     */
    public PhaseCentersRangeModifier(final FrequencyPattern stationPattern,
                                     final FrequencyPattern satellitePattern) {
        super(stationPattern, satellitePattern);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<Range> estimated) {
        final double delta = estimated.getObservedMeasurement().isTwoWay() ?
                             twoWayDistanceModification(estimated) :
                             oneWayDistanceModification(estimated);
        estimated.modifyEstimatedValue(this,
                                       estimated.getEstimatedValue()[0] + delta);
    }

}
