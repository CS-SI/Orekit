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
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.clocks.ClockModel;
import org.orekit.time.clocks.FieldClockModel;
import org.orekit.utils.ParameterDriversProvider;

/** Interface underlying both observed and observing measurement objects. Contains the clock model.
 *
 * @author Brianna Aubin
 * @since 14.0
 */
public interface MeasurementParticipant extends ParameterDriversProvider {

    /** Suffix for ground station position parameters names. */
    String OFFSET_SUFFIX = "-offset";

    /** Suffix for clock bias (a.k.a. systematic offset) parameters name. */
    String BIAS_SUFFIX = "-bias";

    /** Suffix for ground clock drift parameters name. */
    String DRIFT_SUFFIX = "-drift";

    /** Suffix for ground clock drift parameters name. */
    String ACCELERATION_SUFFIX = "-acceleration";

    /** Get the MeasurementObject name.
     * @return name for the object
     */
    String getName();

    /** Get a clock model valid at some date.
     * @return clock model
     */
    ClockModel getClockModel();

    /** Get the current clock offset as a function of time.
     * @param date time of computations
     * @return current clock offset value
     */
    default double getOffsetValue(final AbsoluteDate date) {
        return getClockModel().getOffset(date).getBias();
    }

    /** Get the current clock drift as a function of time.
     * @param date time of computations
     * @return current clock drift value
     */
    default double getOffsetRate(final AbsoluteDate date) {
        return getClockModel().getOffset(date).getRate();
    }

    /** Get the current gradient clock offset as a function of time.
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the differentiation parameters in derivatives computations
     * @param date time of computations
     * @return current gradient clock offset value
     */
    default Gradient getFieldOffsetValue(final int freeParameters, final Map<String, Integer> indices,
                                        final AbsoluteDate date) {
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(GradientField.getField(freeParameters), date);
        return getFieldClockModel(freeParameters, indices, date).getOffset(fieldDate).getBias();
    }

    /** Get the current gradient clock drift as a function of time.
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the differentiation parameters in derivatives computations
     * @param date time of computations
     * @return current gradient clock drift value
     */
    default Gradient getFieldOffsetRate(final int freeParameters,
                                        final Map<String, Integer> indices,
                                        final AbsoluteDate date) {
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(GradientField.getField(freeParameters), date);
        return getFieldClockModel(freeParameters, indices, date).getOffset(fieldDate).getRate();
    }

    /** Get Gradient clock model.
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the differentiation parameters in derivatives computations,
     * must be span name and not driver name
     * @param date time of computations
     * @return clock gradient
     */
    default FieldClockModel<Gradient> getFieldClockModel(final int freeParameters, final Map<String, Integer> indices,
                                                         final AbsoluteDate date) {
        return getClockModel().getFieldModel(freeParameters, indices, date);
    }

}
