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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleFunction;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeInterval;
import org.orekit.utils.drivers.ParameterDriver;

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

    /** Prefix for parameters names. */
    private final String prefix;

    /** List of terms. */
    private final List<ParameterDriver> terms;

    /** Cached field-based models.
     * @since 14.0
     */
    private final Map<Field<? extends CalculusFieldElement<?>>, PolynomialFieldClockModel<?>> fieldModels;

    /** Simple constructor.
     * @param referenceDate reference date (may be null)
     * @param prefix        prefix for the parameter names
     */
    public PolynomialClockModel(final AbsoluteDate referenceDate,
                                final String prefix) {
        this(referenceDate, prefix, 0.0);
    }

    /** Simple constructor.
     * @param referenceDate reference date (may be null if there is only one term)
     * @param prefix        prefix for the parameter names
     * @param terms         the polynomial terms in order.
     */
    public PolynomialClockModel(final AbsoluteDate referenceDate, final String prefix,
                                final double... terms) {

        this.prefix = prefix;

        // set up a safe reference date
        final AbsoluteDate safeReferenceDate;
        if (referenceDate == null) {
            if (terms.length == 1) {
                // we accept null reference date for constant polynomials
                // we just change it to arbitrary to avoid null pointer exceptions
                safeReferenceDate = AbsoluteDate.ARBITRARY_EPOCH;
            } else {
                throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                          prefix + getAcceptedTermSuffix(0));
            }
        } else {
            safeReferenceDate = referenceDate;
        }

        final List<ParameterDriver> convertedTerms = new ArrayList<>();
        for (int i = 0; i < terms.length; ++i) {
            final String name = prefix + getAcceptedTermSuffix(i);
            final ParameterDriver parameterTerm =
                new ParameterDriver(name, 0.0, CLOCK_OFFSET_SCALE,
                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                    TimeInterval.UNLIMITED);
            parameterTerm.setValue(terms[i]);
            parameterTerm.setReferenceDate(safeReferenceDate);
            convertedTerms.add(parameterTerm);
        }
        this.terms = convertedTerms;

        this.fieldModels = new HashMap<>();

    }

    /**
     * Simple constructor.
     * <p>
     * The reference date for computing the polynom is the reference
     * date of the first parameter driver.
     * </p>
     * @param terms the parameter driver terms (they must have names that end
     *              with the {@link #getAcceptedTermSuffix(int) accepted suffixes})
     */
    public PolynomialClockModel(final ParameterDriver... terms) {

        // extract prefix
        if (terms.length == 0) {
            prefix = "";
        } else {
            final String firstName   = terms[0].getName();
            final String firstSuffix = getAcceptedTermSuffix(0);
            if (firstName.endsWith(firstSuffix)) {
                prefix = firstName.substring(0, firstName.length() - firstSuffix.length());
            } else {
                // there will be an error triggered in the following loop
                prefix = firstName;
            }
        }

        int idx = 0;
        for (final ParameterDriver term : terms) {
            final String acceptedName = prefix + getAcceptedTermSuffix(idx);
            if (!term.getName().equals(acceptedName)) {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                          term.getName(), acceptedName);
            }
            idx++;
        }
        this.terms  = Arrays.asList(terms);

        this.fieldModels = new HashMap<>();

    }

    /**
     * Simple constructor.
     *
     * @param terms the parameter driver terms
     */
    public PolynomialClockModel(final List<ParameterDriver> terms) {
        this(terms.toArray(new ParameterDriver[0]));
    }

    /** {@inheritDoc}
     * <p>
     * Validity is extracted from the first parameter driver.
     * </p>
     */
    @Override
    public AbsoluteDate getValidityStart() {
        return terms.getFirst().getValidity().getStartDate();
    }

    /** {@inheritDoc}
     * <p>
     * Validity is extracted from the first parameter driver.
     * </p>
     */
    @Override
    public AbsoluteDate getValidityEnd() {
        return terms.getFirst().getValidity().getEndDate();
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
    public void addParameterDriver(final int index, final ParameterDriver driver) {
        final List<ParameterDriver> parameters = getParametersDrivers();
        if (parameters.size() < index) {
            // Recursively add empty parameters to fill gaps
            addParameterDriver(index - 1, null);
            // After filling gaps, add the driver at the target index
            addParameterDriver(index, driver);
        } else if (parameters.size() == index) {
            if (driver != null) {
                // Add the driver at the correct index
                parameters.add(driver);
            } else {
                // Create empty parameter with correct name for this index
                final ParameterDriver empty = new ParameterDriver(prefix + getAcceptedTermSuffix(index),
                                                                  0.0, CLOCK_OFFSET_SCALE,
                                                                  Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                                                                  TimeInterval.UNLIMITED);
                empty.setReferenceDate(AbsoluteDate.ARBITRARY_EPOCH);
                parameters.add(empty);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public ClockOffset getOffset(final AbsoluteDate date) {
        final double[] result = new double[3];
        if (terms.isEmpty()) {
            return new ClockOffset(date, result);
        }

        final AbsoluteDate referenceDate = terms.getFirst().getReferenceDate();
        final double dt;
        if (referenceDate == null) {
            if (terms.size() == 1) {
                // this is a constant polynomial, we accept a missing reference date
                dt = 0;
            } else {
                throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                          terms.getFirst().getName());
            }
        } else {
            dt = date.durationFrom(referenceDate);
        }

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
    public <T extends CalculusFieldElement<T>> PolynomialFieldClockModel<T> toField(final DoubleFunction<T> converter) {
        // build aggregated models may be costly, so we cache the results
        return (PolynomialFieldClockModel<T>) fieldModels.computeIfAbsent(converter.apply(0.0).getField(),
                                                                          f -> buildFieldModel(converter));
    }

    /**
     * Build a field model.
     * @param <T> type of the field elements
     * @param converter converter to field elements
     * @return field version of the instance
     * @since 14.0
     */
    private <T extends CalculusFieldElement<T>> PolynomialFieldClockModel<T> buildFieldModel(final DoubleFunction<T> converter) {

        final Field<T> field = converter.apply(0.0).getField();

        // handle special case
        if (terms.isEmpty()) {
            return new PolynomialFieldClockModel<>(FieldAbsoluteDate.getArbitraryEpoch(field), "");
        }

        // convert reference date
        if (terms.size() > 1 && terms.getFirst().getReferenceDate() == null) {
            throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                      terms.getFirst().getName());
        }
        final FieldAbsoluteDate<T> fieldDate =
            new FieldAbsoluteDate<>(field, terms.getFirst().getReferenceDate());

        // convert polynomial coefficients
        final T[] fieldTerms = MathArrays.buildArray(field, terms.size());
        for (int i = 0; i < terms.size(); ++i) {
            fieldTerms[i] = converter.apply(terms.get(i).getValue());
        }

        return new PolynomialFieldClockModel<>(fieldDate, prefix, fieldTerms);

    }

    /** {@inheritDoc} */
    @Override
    public PolynomialFieldClockModel<Gradient> toGradient(final int freeParameters, final Map<String, Integer> indices) {

        final GradientField field = GradientField.getField(freeParameters);

        // handle special case
        if (terms.isEmpty()) {
            return new PolynomialFieldClockModel<>(FieldAbsoluteDate.getArbitraryEpoch(field), "");
        }

        // convert reference date
        if (terms.size() > 1 && terms.getFirst().getReferenceDate() == null) {
            throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                      terms.getFirst().getName());
        }
        final FieldAbsoluteDate<Gradient> fieldDate =
            new FieldAbsoluteDate<>(field, terms.getFirst().getReferenceDate());

        // convert polynomial coefficients
        final Gradient[] fieldTerms = new Gradient[terms.size()];
        for (int i = 0; i < terms.size(); ++i) {
            fieldTerms[i] = terms.get(i).getValue(freeParameters, indices);
        }

        return new PolynomialFieldClockModel<>(fieldDate, prefix, fieldTerms);

    }

}
