/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.utils.drivers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FieldGradient;
import org.hipparchus.util.FastMath;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeInterval;

import java.util.Map;

/** Field version of {@link ParameterDriver}.
 * @param <T> type of the field elements
 * @see FieldParameterObserver
 * @author Luc Maisonobe
 * @since 14.0
 */
public class FieldParameterDriver<T extends CalculusFieldElement<T>>
    extends BaseParameterDriver<FieldParameterDriver<T>, FieldParameterObserver<T>> {

    /** Reference date. */
    private FieldAbsoluteDate<T> referenceDate;

    /** Reference value. */
    private T referenceValue;

    /** Current value. */
    private T value;

    /**
     * Simple constructor.
     * <p>
     * At construction, the parameter is configured as <em>not</em> selected, the reference date is set to {@code null},
     * the value is set to the {@code referenceValue}.
     * </p>
     * @param name           name of the parameter
     * @param referenceValue reference value of the parameter
     * @param scale          scaling factor to convert the parameters value to non-dimensional (typically set to the
     *                       expected standard deviation of the parameter), it must be non-zero
     * @param minValue       minimum value allowed
     * @param maxValue       maximum value allowed
     * @param validity       validity interval
     */
    public FieldParameterDriver(final String name,
                                final T referenceValue, final double scale,
                                final double minValue, final double maxValue,
                                final TimeInterval validity) {
        super(name, scale, minValue, maxValue, validity);
        this.referenceValue = referenceValue;
        this.value          = referenceValue;
    }

    /** Get current reference date.
     * @return current reference date (null if it was never set)
     */
    public FieldAbsoluteDate<T> getReferenceDate() {
        return referenceDate;
    }

    /** Set reference date.
     * @param newReferenceDate new reference date
     */
    public void setReferenceDate(final FieldAbsoluteDate<T> newReferenceDate) {
        final FieldAbsoluteDate<T> previousReferenceDate = getReferenceDate();
        referenceDate = newReferenceDate;
        for (final FieldParameterObserver<T> observer : getObservers()) {
            observer.referenceDateChanged(previousReferenceDate, this);
        }
    }

    /** Get reference parameter value.
     * @return reference parameter value
     */
    public T getReferenceValue() {
        return referenceValue;
    }

    /** Set reference parameter value.
     * @param referenceValue the reference value to set.
     */
    public void setReferenceValue(final T referenceValue) {
        final T previousReferenceValue = this.referenceValue;
        this.referenceValue = referenceValue;
        for (final FieldParameterObserver<T> observer : getObservers()) {
            observer.referenceValueChanged(previousReferenceValue, this);
        }
    }

    /** Get current parameter value.
     * @return current parameter value
     */
    public T getValue() {
        return value;
    }

    /** Get the value as a gradient.
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the differentiation parameters in derivatives computations
     * @return value with derivatives
     */
    public FieldGradient<T> getValue(final int freeParameters, final Map<String, Integer> indices) {
        final Integer index = indices.get(getName());
        return (index == null) ?
               FieldGradient.constant(freeParameters, getValue()) :
               FieldGradient.variable(freeParameters, index, getValue());
    }

    /** Set parameter value.
     * <p>
     * If {@code newValue} is below {@link #getMinValue()}, it will
     * be silently set to {@link #getMinValue()}. If {@code newValue} is
     * above {@link #getMaxValue()}, it will be silently set to {@link
     * #getMaxValue()}.
     * </p>
     * @param newValue new value to set
     */
    public void setValue(final T newValue) {
        final T previousValue = value;
        value = FastMath.max(FastMath.min(newValue, getMaxValue()), getMinValue());
        for (final FieldParameterObserver<T> observer : getObservers()) {
            observer.valueChanged(previousValue, this);
        }
    }

    /** Get normalized value.
     * <p>
     * The normalized value is a non-dimensional value
     * suitable for use as part of a vector in an optimization
     * process. It is computed as {@code (current - reference)/scale}.
     * </p>
     * @return normalized value
     */
    public T getNormalizedValue() {
        return value.subtract(referenceValue).divide(getScale());
    }

    /** Set normalized value.
     * <p>
     * The normalized value is a non-dimensional value
     * suitable for use as part of a vector in an optimization
     * process. It is computed as {@code (current - reference)/scale}.
     * </p>
     * @param normalized value
     */
    public void setNormalizedValue(final T normalized) {
        setValue(referenceValue.add(normalized.multiply(getScale())));
    }

    /** Set minimum parameter value.
     * @param minValue the minimum value to set.
     */
    public void setMinValue(final double minValue) {

        // base handling of the new minimum value
        super.setMinValue(minValue);

        if (value.getReal() < minValue) {
            // clip value to minimum
            setValue(value.newInstance(minValue));
        }

    }

    /** Set maximum parameter value.
     * @param maxValue the maximum value to set.
     */
    public void setMaxValue(final double maxValue) {

        // base handling of the new maximum value
        super.setMaxValue(maxValue);

        if (value.getReal() > maxValue) {
            // clip value to maximum
            setValue(value.newInstance(maxValue));
        }

    }

    /** Get a text representation of the parameter.
     * @return text representation of the parameter, in the form name = value.
     */
    public String toString() {
        return getName() + " = " + value.getReal();
    }

}
