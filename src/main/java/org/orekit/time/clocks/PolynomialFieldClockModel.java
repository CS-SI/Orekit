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
package org.orekit.time.clocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.polynomials.FieldPolynomialFunction;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.exception.NullArgumentException;
import org.hipparchus.util.MathUtils;
import org.orekit.time.FieldAbsoluteDate;

/** Field Polynomial clock model.
 *
 * @author Luc Maisonobe
 * @since 12.1
 *
 */
public class PolynomialFieldClockModel<T extends CalculusFieldElement<T>> extends AbstractFieldClockModel<T> {
    /** All term. */
    private final List<T> terms;

    /** Simple constructor.
     * @param referenceDate reference date
     * @param terms polynomial terms
     */
    @SafeVarargs
    public PolynomialFieldClockModel(final FieldAbsoluteDate<T> referenceDate,
                                    final T... terms) {
        super(referenceDate);
        this.terms = Arrays.asList(terms);
    }

    /** Get the clock offset at date.
     * @param date date at which offset is requested
     * @return clock offset at specified date
     */
    @Override
    @SuppressWarnings("unchecked")
    public FieldClockOffset<T> getOffset(final FieldAbsoluteDate<T> date) {
        final T dt = date.durationFrom(getReferenceDate());
        final List<T> result = new ArrayList<>(3);
        // Loop over all of the terms in order
        // The value of the first offset is the value of the function at time dt from
        // date
        // The value of the second offset is the derivative of the function at time dt
        // from date
        // Repeat until out of terms
        T[] copyCoefficients = terms.toArray((T[]) new CalculusFieldElement[terms.size()]);
        for (int ii = 0; ii < 3; ii++) {
            final T newValue = value(copyCoefficients, dt);
            result.add(newValue);
            // Take the next derivative
            copyCoefficients = differentiate(copyCoefficients);
        }
        return new FieldClockOffset<>(date, result);
    }


    /** Get the derivative of the given coefficients.
     * @param coefficients the list of provided coefficients
     * @return the differentiated coefficients
     * @throws MathIllegalArgumentException if the derivative is not possible to calculate
     * @throws NullArgumentException if the coefficients is null
     */
    private T[] differentiate(final T[] coefficients) throws MathIllegalArgumentException, NullArgumentException {
        MathUtils.checkNotNull(coefficients);
        final FieldPolynomialFunction<T> func = new FieldPolynomialFunction<>(coefficients);
        final FieldPolynomialFunction<T> derivative = func.polynomialDerivative();
        return derivative.getCoefficients();
    }

    /** Get the value of the function created by the given coeffients.
     * @param coefficients the provided coefficients starting at some degree
     * @param t the time
     * @return the value of the function at that time
     * @throws MathIllegalArgumentException if the derivative is not possible to calculate
     * @throws NullArgumentException if the coefficients are null
     */
    private T value(final T[] coefficients, final T t) throws MathIllegalArgumentException, NullArgumentException {
        MathUtils.checkNotNull(coefficients);
        return new FieldPolynomialFunction<>(coefficients).value(t);

    }
}
