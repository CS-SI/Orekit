/* Copyright 2022-2026 Thales Alenia Space
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
package org.orekit.time.clocks;

import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.drivers.ParameterDriver;
import org.orekit.utils.drivers.ParameterDriversProvider;

/** Offset clock model.
 * @author Luc Maisonobe
 * @since 12.1
 */
public interface ClockModel extends ParameterDriversProvider {

    /** Get the accepted parameter term names.
     * This creates the list of parameter driver terms that are accepted
     * @param count the count of the term name being request
     * @return clock term name
     */
    default String getAcceptedTermName(final Integer count) {
        final String[] regularNames = {"-clock-bias", "-clock-drift", "-clock-acceleration"};
        if (count < 3) {
            return regularNames[count];
        }
        return "-clock-term-" + count;
    }


    /** Get validity start.
     * @return model validity start
     */
    AbsoluteDate getValidityStart();

    /** Get validity end.
     * @return model validity end
     */
    AbsoluteDate getValidityEnd();

    /** Get the clock offset at date.
     * @param date date at which offset is requested
     * @return clock offset at specified date
     */
    ClockOffset getOffset(AbsoluteDate date);

    /** Get the clock offset value at date.
     * @param date date at which offset value is requested
     * @return clock offset value
     */
    default double getOffsetValue(final AbsoluteDate date) {
        return getOffset(date).getValue(date);
    }

    /** Get the field clock offset at date.
     * @param <T> type of the field elements
     * @param date date at which offset is requested
     * @return field clock offset
     */
    <T extends CalculusFieldElement<T>> FieldClockOffset<T> getFieldOffset(FieldAbsoluteDate<T> date);

    /** Get the parameter driver for the clock bias.
     * <p>
     * The bias represents the constant offset of the clock from the reference time scale.
     * For a polynomial clock model of the form offset(t) = a₀ + a₁·t + a₂·t² + ...,
     * the bias driver corresponds to the a₀ coefficient.
     * </p>
     * @return parameter driver for clock bias, or null if not available
     * @since 14.0
     */
    default ParameterDriver getBiasDriver() {
        return getParameterDriverWithSubstring(getAcceptedTermName(0));
    }

    /** Get the parameter driver for the clock rate (drift).
     * <p>
     * The rate represents the linear drift of the clock offset over time.
     * For a polynomial clock model of the form offset(t) = a₀ + a₁·t + a₂·t² + ...,
     * the rate driver corresponds to the a₁ coefficient. The rate is expressed
     * in seconds of offset per second of elapsed time (dimensionless).
     * </p>
     * @return parameter driver for clock rate, or null if not available
     * @since 14.0
     */
    default ParameterDriver getRateDriver() {
        return getParameterDriverWithSubstring(getAcceptedTermName(1));
    }

    /** Get the parameter driver for the clock acceleration.
     * <p>
     * The acceleration represents the quadratic component of the clock offset over time.
     * For a polynomial clock model of the form offset(t) = a₀ + a₁·t + a₂·t² + ...,
     * the acceleration driver corresponds to the a₂ coefficient. The acceleration
     * is expressed in seconds of offset per second squared of elapsed time.
     * </p>
     * @return parameter driver for clock acceleration, or null if not available
     * @since 14.0
     */
    default ParameterDriver getAccelerationDriver() {
        return getParameterDriverWithSubstring(getAcceptedTermName(2));
    }

    /** Get the parameter driver for a given index.
     * <p>
     * This gets the desired term value for the given integer.
     * For example, if the fifth term counting up from bias being zero is desired.
     * For all parameters without the standard naming convention, other methods
     * of retrieval are required.
     * </p>
     * @param term the driver term parameter index requested
     * @return parameter driver for clock acceleration, or null if not available
     * @since 14.0
     */
    default ParameterDriver getParameterDriverTerm(final Integer term) {
        return switch (term) {
            case 0 -> getBiasDriver();
            case 1 -> getRateDriver();
            case 2 -> getAccelerationDriver();
            default -> getParameterDriver(getAcceptedTermName(term));
        };
    }

    /**
     * Convert to field model.
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the differentiation parameters in derivatives computations,
     * must be span name and not driver name
     * @param date date at which model must be valid
     * @return converted clock model
     */
    FieldClockModel<Gradient> getFieldModel(int freeParameters, Map<String, Integer> indices, AbsoluteDate date);

}
