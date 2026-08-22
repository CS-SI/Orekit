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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeInterval;
import org.orekit.utils.ParameterDriver;

/** Polynomial clock model.
 *
 * @author Luc Maisonobe
 * @since 12.1
 *
 */
public class PolynomialClockModel implements ClockModel {

    /**
     * Clock offset scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double CLOCK_OFFSET_SCALE = FastMath.scalb(1.0, -10);

    /** List of terms. */
    private final List<ParameterDriver> terms;

    /**
     * Simple constructor.
     *
     * @param referenceDate reference date
     */
    public PolynomialClockModel(final AbsoluteDate referenceDate) {
        this.terms =  new ArrayList<>();
        final ParameterDriver parameterTerm = new ParameterDriver("-clock-bias", 0.0, CLOCK_OFFSET_SCALE,
                                                                  Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                                  TimeInterval.UNLIMITED);
        parameterTerm.setValue(0);
        parameterTerm.setReferenceDate(referenceDate);
        this.terms.add(parameterTerm);
    }

    /**
     * Simple constructor.
     *
     * @param referenceDate reference date
     * @param terms the polynomial terms in order.
     */
    public PolynomialClockModel(final AbsoluteDate referenceDate,
            final double... terms) {
        Integer ii = 0;
        final List<ParameterDriver> convertedTerms = new ArrayList<>();
        for (double term : terms) {
            final String name = getAcceptedTermName(ii);
            final ParameterDriver parameterTerm = new ParameterDriver(name, 0.0, CLOCK_OFFSET_SCALE,
                                                                      Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                                      TimeInterval.UNLIMITED);
            parameterTerm.setValue(term);
            parameterTerm.setReferenceDate(referenceDate);
            convertedTerms.add(parameterTerm);
            ++ii;
        }
        this.terms = convertedTerms;
    }

    /**
     * Simple constructor.
     *
     * @param terms the parameter driver terms
     */
    public PolynomialClockModel(final ParameterDriver... terms) {
        Integer idx = 0;
        for (final ParameterDriver term : terms) {
            final String accepted_name_format = getAcceptedTermName(idx);
            if (!term.getName().contains(accepted_name_format)) {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, term.getName(), accepted_name_format);
            }
            idx++;
        }
        this.terms = Arrays.asList(terms);
    }

    /**
     * Simple constructor.
     *
     * @param terms the parameter driver terms
     */
    public PolynomialClockModel(final List<ParameterDriver> terms) {
        this(terms.toArray(new ParameterDriver[0]));
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

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return terms;
    }

    /** Add a parameter driver term in a given index to the list of parameters.
     * If parameters prior to the one requested don't exist, it will create empty ones.
     * This allows adding a velocity, acceleration, or above without explicitly defining the terms below.
     *
     * @param index the index at which to add the parameter driver
     * @param driver the parameter driver to add
     */
    public void addParameterDriver(final Integer index, final ParameterDriver driver) {
        final List<ParameterDriver> parameters = getParametersDrivers();
        if (parameters.size() < index) {
            // Recursively add empty parameters to fill gaps
            addParameterDriver(index - 1, null);
            // After filling gaps, add the driver at the target index
            addParameterDriver(index, driver);
        } else if (parameters.size() == index && driver != null) {
            // Add the driver at the correct index
            parameters.add(driver);
        } else if (parameters.size() == index && driver == null) {
            // Create empty parameter with correct name for this index
            final ParameterDriver empty = new ParameterDriver(getAcceptedTermName(index), 0.0, CLOCK_OFFSET_SCALE,
                                                              Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                              TimeInterval.UNLIMITED);
            empty.setReferenceDate(AbsoluteDate.ARBITRARY_EPOCH);
            parameters.add(empty);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ClockOffset getOffset(final AbsoluteDate date) {
        final double[] result = new double[3];
        if (terms.isEmpty()) {
            return new ClockOffset(date, result);
        }
        final double dt = date.durationFrom(getSafeReference(date));
        final double[] convertedTerms = terms.
                                        stream().
                                        map(ParameterDriver::getValue).
                                        mapToDouble(Double::doubleValue).
                                        toArray();
        // Turn the terms into a polynomial function
        PolynomialFunction function = new PolynomialFunction(convertedTerms);
        // Loop over all of the terms in order
        for (int ii = 0; ii < 3; ii++) {
            result[ii] = function.value(dt);
            function = function.polynomialDerivative();
        }
        return new ClockOffset(date, result);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldClockOffset<T> getFieldOffset(final FieldAbsoluteDate<T> date) {
        final AbsoluteDate aDate = date.toAbsoluteDate();
        final T dt = date.durationFrom(getSafeReference(aDate));
        final List<T> result = new ArrayList<>(3);
        // Loop over all of the terms in order
        // Repeat until out of terms
        final double[] convertedTerms = terms.
                                        stream().
                                        map(ParameterDriver::getValue).
                                        mapToDouble(Double::doubleValue).
                                        toArray();
        // Turn the terms into a polynomial function
        PolynomialFunction function = new PolynomialFunction(convertedTerms);
        for (int ii = 0; ii < 3; ii++) {
            final T newValue = function.value(dt);
            result.add(newValue);
            // Take the next derivative
            function = function.polynomialDerivative();
        }
        return new FieldClockOffset<>(date, result);
    }

    /** {@inheritDoc} */
    @Override
    public FieldClockModel<Gradient> getFieldModel(final int freeParameters,
            final Map<String, Integer> indices, final AbsoluteDate date) {
        if (terms.isEmpty()) {
            return null;
        }
        final Gradient[] gradients = terms.stream().map(x -> x.getValue(freeParameters, indices))
                .toArray(Gradient[]::new);
        final FieldAbsoluteDate<Gradient> referenceDate = new FieldAbsoluteDate<>(gradients[0].getField(),
                getSafeReference(date));
        return new PolynomialFieldClockModel<>(referenceDate, gradients);
    }

    /**
     * Get a safe reference date.
     * <p>
     * This method deals with parameters drivers for which no reference
     * date has been set, which is acceptable if the model is not
     * time-dependent.
     * </p>
     *
     * @param date date at which values are requested
     * @return safe reference date
     */
    private AbsoluteDate getSafeReference(final AbsoluteDate date) {
        // If there are no terms the clock model is constant and the date is safe
        final double EPS = 1e-9;
        if (terms.isEmpty()) {
            return date;
        }
        final ParameterDriver firstTerm = terms.getFirst();
        if (firstTerm.getReferenceDate() == null) {
            boolean allOtherDatesZero = true;
            for (final ParameterDriver term: terms) {
                if (FastMath.abs(term.getValue()) > EPS) {
                    allOtherDatesZero = false;
                }
            }
            if (allOtherDatesZero) {
                // it is OK to not have a reference date is clock offset is constant
                return date;
            } else {
                throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                        firstTerm.getName());
            }
        } else {
            return firstTerm.getReferenceDate();
        }
    }

}
