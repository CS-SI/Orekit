/* Copyright 2025-2026 Hawkeye 360 (HE360)
 * Licensed to CS Group (CS) under one or more
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

package org.orekit.estimation.measurements;

import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.clocks.QuadraticClockModel;
import org.orekit.time.clocks.QuadraticFieldClockModel;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;

/** Interface underlying both observed and observing measurement objects. Contains the clock model.
 *
 * @author Brianna Aubin
 * @since 14.0
 */
public interface MeasurementParticipant extends ParameterDriversProvider {

    /** Suffix for ground station position and clock offset parameters names. */
    String OFFSET_SUFFIX = "-offset";

    /** Suffix for ground clock drift parameters name. */
    String DRIFT_SUFFIX = "-drift";

    /** Suffix for ground clock drift parameters name. */
    String ACCELERATION_SUFFIX = "-acceleration";

    /** Get the MeasurementObject name.
     * @return name for the object
     */
    String getName();

    /** Get the clock offset driver.
     * @return clock offset driver
     */
    default ParameterDriver getClockOffsetDriver() {
        return getQuadraticClockModel().getClockOffsetDriver();
    }

    /** Get the clock drift driver.
     * @return clock drift driver
     */
    default ParameterDriver getClockDriftDriver() {
        return getQuadraticClockModel().getClockDriftDriver();
    }

    /** Get the clock acceleration driver.
     * @return clock acceleration driver
     */
    default ParameterDriver getClockAccelerationDriver() {
        return getQuadraticClockModel().getClockAccelerationDriver();
    }

    /** Get a quadratic clock model valid at some date.
     * @return quadratic clock model
     */
    QuadraticClockModel getQuadraticClockModel();

    /** Get emitting satellite clock provider.
     * @param freeParameters total number of free parameters in the gradient
     * @param date time of computations
     * @param indices indices of the differentiation parameters in derivatives computations,
     * must be span name and not driver name
     * @return emitting satellite clock provider
     */
    default QuadraticFieldClockModel<Gradient> getQuadraticFieldClock(final int freeParameters,
                                                                      final AbsoluteDate date,
                                                                      final Map<String, Integer> indices) {
        return getQuadraticClockModel().toGradientModel(freeParameters, indices, date);
    }

}
