/* Copyright 2002-2026 CS GROUP
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

import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterval;

/** Class allowing to drive the value of a parameter.
 * <p>
 * This class is typically used as a bridge between an estimation algorithm
 * (typically orbit determination or optimizer) and an internal parameter in
 * a physical model that needs to be tuned. The physical model will expose to
 * the algorithm a set of instances of this class so the algorithm can call the
 * {@link #setValue(double)} method to update the parameter value.
 * </p>
 * <p>
 * Any object can be notified when any of value, name, selection status… are changed.
 * This is done by {@link BaseParameterDriver#addObserver(BaseParameterObserver)
 * registering} a {@link ParameterObserver parameter observer} to the parameter driver.
 * <p>
 * This design has two major goals. First, it allows an external algorithm to drive
 * internal parameters blindly, as it only needs to get a list of instances of this
 * class, without knowing what they really drive. Second, it allows the physical
 * model to not expose directly setters methods for its parameters. In order to be
 * able to modify the parameter value, the algorithm <em>must</em> retrieve a
 * parameter driver.
 * </p>
 * <p>
 * As of versions 12.X and 13.X, it was possible to set up time-dependent values
 * within a single {@code ParameterDriver}. This feature has been replaced by
 * {@link ParameterDriversSequence} as of 14.0, which is simpler and also allows
 * finer selection, making it possible to select only a subset of the parameters
 * along a timeline. Starting with version 14.0, {@code ParameterDriver} instances
 * only hold one value, which can have a restricted validity range.
 * </p>
 * @see ParameterObserver
 * @author Luc Maisonobe
 * @author Melina Vanel
 * @since 8.0
 */
public class ParameterDriver extends BaseParameterDriver<ParameterDriver, ParameterObserver> {

    /** Reference date.
     * @since 9.0
     */
    private AbsoluteDate referenceDate;

    /** Reference value. */
    private double referenceValue;

    /** Current value.
     * @since 14.0
     */
    private double value;

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
    public ParameterDriver(final String name,
                           final double referenceValue, final double scale,
                           final double minValue, final double maxValue,
                           final TimeInterval validity) {
        super(name, scale, minValue, maxValue, validity);
        this.referenceValue = referenceValue;
        this.value          = referenceValue;
    }

    /** Get current reference date.
     * @return current reference date (null if it was never set)
     * @since 9.0
     */
    public AbsoluteDate getReferenceDate() {
        return referenceDate;
    }

    /** Set reference date.
     * @param newReferenceDate new reference date
     * @since 9.0
     */
    public void setReferenceDate(final AbsoluteDate newReferenceDate) {
        final AbsoluteDate previousReferenceDate = getReferenceDate();
        referenceDate = newReferenceDate;
        for (final ParameterObserver observer : getObservers()) {
            observer.referenceDateChanged(previousReferenceDate, this);
        }
    }

    /** Get reference parameter value.
     * @return reference parameter value
     */
    public double getReferenceValue() {
        return referenceValue;
    }

    /** Set reference parameter value.
     * @since 9.3
     * @param referenceValue the reference value to set.
     */
    public void setReferenceValue(final double referenceValue) {
        final double previousReferenceValue = this.referenceValue;
        this.referenceValue = referenceValue;
        for (final ParameterObserver observer : getObservers()) {
            observer.referenceValueChanged(previousReferenceValue, this);
        }
    }

    /** Get current parameter value.
     * @return current parameter value
     */
    public double getValue() {
        return value;
    }

    /** Get the value as a gradient.
     * @param freeParameters total number of free parameters in the gradient
     * @param indices indices of the differentiation parameters in derivatives computations
     * @return value with derivatives
     * @since 10.2
     */
    public Gradient getValue(final int freeParameters, final Map<String, Integer> indices) {
        final Integer index = indices.get(getName());
        return (index == null) ?
               Gradient.constant(freeParameters, getValue()) :
               Gradient.variable(freeParameters, index, getValue());
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
    public void setValue(final double newValue) {
        final double previousValue = value;
        value = FastMath.max(FastMath.min(newValue, getMaxValue()), getMinValue());
        for (final ParameterObserver observer : getObservers()) {
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
    public double getNormalizedValue() {
        return (value - referenceValue) / getScale();
    }

    /** Set normalized value.
     * <p>
     * The normalized value is a non-dimensional value
     * suitable for use as part of a vector in an optimization
     * process. It is computed as {@code (current - reference)/scale}.
     * </p>
     * @param normalized value
     */
    public void setNormalizedValue(final double normalized) {
        setValue(referenceValue + getScale() * normalized);
    }

    /** Set minimum parameter value.
     * @since 9.3
     * @param minValue the minimum value to set.
     */
    public void setMinValue(final double minValue) {

        // base handling of the new minimum value
        super.setMinValue(minValue);

        if (value < minValue) {
            // clip value to minimum
            setValue(minValue);
        }

    }

    /** Set maximum parameter value.
     * @since 9.3
     * @param maxValue the maximum value to set.
     */
    public void setMaxValue(final double maxValue) {

        // base handling of the new maximum value
        super.setMaxValue(maxValue);

        if (value > maxValue) {
            // clip value to maximum
            setValue(maxValue);
        }

    }

    /** Get a text representation of the parameter.
     * @return text representation of the parameter, in the form name = value.
     */
    public String toString() {
        return getName() + " = " + value;
    }

}
