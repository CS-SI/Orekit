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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Field Polynomial clock model.
 * @param <T> type of the field elements
 * @author Brian Carter
 * @since 14.0
 * @see PolynomialClockModel
 */
public class PolynomialFieldClockModel<T extends CalculusFieldElement<T>> implements FieldClockModel<T> {

    /** Reference date. */
    private final FieldAbsoluteDate<T> referenceDate;

    /** Prefix for the parameters names. */
    private final String prefix;

    /** All term. */
    private final List<T> terms;

    /** Simple constructor.
     * @param referenceDate reference date
     * @param prefix        prefix for the parameter names
     * @param terms         polynomial terms
     */
    @SafeVarargs
    public PolynomialFieldClockModel(final FieldAbsoluteDate<T> referenceDate,
                                     final String prefix,
                                     final T... terms) {
        this.referenceDate = referenceDate;
        this.prefix        = prefix;
        this.terms         = Arrays.asList(terms);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityStart() {
        return AbsoluteDate.PAST_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getValidityEnd() {
        return AbsoluteDate.FUTURE_INFINITY;
    }

    /** Get the clock offset at date.
     * @param date date at which offset is requested
     * @return clock offset at specified date
     */
    @Override
    @SuppressWarnings("unchecked")
    public FieldClockOffset<T> getOffset(final FieldAbsoluteDate<T> date) {
        final T dt = date.durationFrom(referenceDate);
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

    /** {@inheritDoc} */
    @Override
    public PolynomialClockModel toNonField() {
        final double[] nonFieldTerms = new double[terms.size()];
        for (int i = 0; i < nonFieldTerms.length; i++) {
            nonFieldTerms[i] = terms.get(i).getReal();
        }
        return new PolynomialClockModel(referenceDate.toAbsoluteDate(), prefix, nonFieldTerms);
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
