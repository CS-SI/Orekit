/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.estimation.measurements;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ClockModel;
import org.orekit.time.ClockOffset;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldClockOffset;
import org.orekit.utils.ParameterDriver;

import java.util.Map;

/** Quadratic clock model.
 *
 * @author Luc Maisonobe
 * @since 12.1
 *
 */
public class QuadraticClockModel implements ClockModel {

    /** Clock offset scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double CLOCK_OFFSET_SCALE = FastMath.scalb(1.0, -10);

    /** Constant term. */
    private final ParameterDriver a0;

    /** Linear term. */
    private final ParameterDriver a1;

    /** Quadratic term. */
    private final ParameterDriver a2;

    /** Simple constructor.
     * @param referenceDate reference date
     * @param a0 constant term
     * @param a1 linear term
     * @param a2 quadratic term
     */
    public QuadraticClockModel(final AbsoluteDate referenceDate,
                               final double a0, final double a1, final double a2) {
        this(new ParameterDriver("a0",
                                 0.0, CLOCK_OFFSET_SCALE,
                                 Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
             new ParameterDriver("a1",
                                 0.0, CLOCK_OFFSET_SCALE,
                                 Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
             new ParameterDriver("a2",
                                 0.0, CLOCK_OFFSET_SCALE,
                                 Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        this.a0.setValue(a0);
        this.a0.setReferenceDate(referenceDate);
        this.a1.setValue(a1);
        this.a1.setReferenceDate(referenceDate);
        this.a2.setValue(a2);
        this.a2.setReferenceDate(referenceDate);
    }

    /** Simple constructor.
     * @param a0 constant term
     * @param a1 linear term
     * @param a2 quadratic term
     */
    public QuadraticClockModel(final ParameterDriver a0, final ParameterDriver a1, final ParameterDriver a2) {
        this.a0 = a0;
        this.a1 = a1;
        this.a2 = a2;
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
    public ClockOffset getOffset(final AbsoluteDate date) {
        final double dt = date.durationFrom(getSafeReference(date));
        final double c0 = a0.getValue(date);
        final double c1 = a1.getValue(date);
        final double c2 = a2.getValue(date);
        return new ClockOffset(date,
                               (c2 * dt + c1) * dt + c0,
                               2 * c2 * dt + c1,
                               2 * c2);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldClockOffset<T> getOffset(final FieldAbsoluteDate<T> date) {
        final AbsoluteDate aDate = date.toAbsoluteDate();
        final T dt = date.durationFrom(getSafeReference(aDate));
        final double c0 = a0.getValue(aDate);
        final double c1 = a1.getValue(aDate);
        final double c2 = a2.getValue(aDate);
        return new FieldClockOffset<>(date,
                                      dt.multiply(dt.multiply(c2).add(c1)).add(c0),
                                      dt.multiply(2 * c2).add(c1),
                                      dt.newInstance(2 * c2));
    }

    /** Get a safe reference date.
     * <p>
     * This method deals with parameters drivers for which no reference
     * date has been set, which is acceptable if the model is not
     * time-dependent.
     * </p>
     * @param date date at which values are requested
     * @return safe reference date
     */
    private AbsoluteDate getSafeReference(final AbsoluteDate date) {
        if (a0.getReferenceDate() == null) {
            if (a1.getValue(date) == 0 && a2.getValue(date) == 0) {
                // it is OK to not have a reference date is clock offset is constant
                return date;
            } else {
                throw new OrekitException(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER,
                                          a0.getName());
            }
        } else {
            return a0.getReferenceDate();
        }
    }

    /** Convert to gradient model.
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the differentiation parameters in derivatives computations,
     * must be span name and not driver name
     * @param date date at which model must be valid
     * @return converted clock model
     */
    public QuadraticFieldClockModel<Gradient> toGradientModel(final int freeParameters,
                                                              final Map<String, Integer> indices,
                                                              final AbsoluteDate date) {
        final Gradient g0 = a0.getValue(freeParameters, indices, date);
        final Gradient g1 = a1.getValue(freeParameters, indices, date);
        final Gradient g2 = a2.getValue(freeParameters, indices, date);
        final FieldAbsoluteDate<Gradient> referenceDate =
            new FieldAbsoluteDate<>(g0.getField(), getSafeReference(date));
        return new QuadraticFieldClockModel<>(referenceDate, g0, g1, g2);
    }

}
