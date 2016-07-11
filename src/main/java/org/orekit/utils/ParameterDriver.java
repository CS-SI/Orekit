/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.utils;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;


/** Class allowing to drive the value of a parameter.
 * <p>
 * This class is typically used as a bridge between an estimation
 * algorithm (typically orbit determination or optimizer) and an
 * internal parameter in a physical model that needs to be tuned,
 * or a bridge between a finite differences algorithm and an
 * internal parameter in a physical model that needs to be slightly
 * offset. The physical model will expose to the algorithm a
 * set of instances of this class so the algorithm can call the
 * {@link #setValue(double)} method to update the
 * parameter value. Each time the value is set, the physical model
 * will be notified as it will register a {@link ParameterObserver
 * ParameterObserver} for this purpose.
 * </p>
 * <p>
 * This design has two major goals. First, it allows an external
 * algorithm to drive internal parameters almost anonymously, as it only
 * needs to get a list of instances of this class, without knowing
 * what they really drive. Second, it allows the physical model to
 * not expose directly setters methods for its parameters. In order
 * to be able to modify the parameter value, the algorithm
 * <em>must</em> retrieve a parameter driver.
 * </p>
 * @see ParameterObserver
 * @author Luc Maisonobe
 * @since 8.0
 */
public class ParameterDriver {

    /** Name of the parameter. */
    private final String name;

    /** Reference value. */
    private final double referenceValue;

    /** Scaling factor. */
    private final double scale;

    /** Minimum value. */
    private final double minValue;

    /** Maximum value. */
    private final double maxValue;

    /** Current value. */
    private double value;

    /** Selection status.
     * <p>
     * Selection is used for estimated parameters in orbit determination,
     * or to compute the Jacobian matrix in partial derivatives computation.
     * </p>
     */
    private boolean selected;

    /** Observers observing this driver. */
    private final List<ParameterObserver> observers;

    /** Simple constructor.
     * <p>
     * At construction, the parameter is configured as <em>not</em> selected,
     * and the value is set to the {@code referenceValue}.
     * </p>
     * @param name name of the parameter
     * @param referenceValue reference value of the parameter
     * @param scale scaling factor to convert the parameters value to
     * non-dimensional (typically set to the expected standard deviation of the
     * parameter), it must be non-zero
     * @param minValue minimum value
     * @param maxValue maximum value
     * @exception OrekitException if scale is too close to zero
     */
    public ParameterDriver(final String name, final double referenceValue,
                           final double scale, final double minValue,
                           final double maxValue)
        throws OrekitException {
        if (FastMath.abs(scale) <= Precision.SAFE_MIN) {
            throw new OrekitException(OrekitMessages.TOO_SMALL_SCALE_FOR_PARAMETER,
                                      name, scale);
        }
        this.name           = name;
        this.referenceValue = referenceValue;
        this.scale          = scale;
        this.minValue       = minValue;
        this.maxValue       = maxValue;
        this.value          = referenceValue;
        this.selected       = false;
        this.observers      = new ArrayList<ParameterObserver>();
    }


    /** Add an observer for this driver.
     * <p>
     * The observer {@link ParameterObserver#valueChanged(double, ParameterDriver)
     * valueChanged} method is called once automatically when the
     * observer is added, and then called at each value change.
     * </p>
     * @param observer observer to add
     * @exception OrekitException if the observer triggers one
     * while being updated
     */
    public void addObserver(final ParameterObserver observer)
        throws OrekitException {
        observers.add(observer);
        observer.valueChanged(getValue(), this);
    }

    /** Get name.
     * @return name
     */
    public String getName() {
        return name;
    }

    /** Get reference parameter value.
     * @return reference parameter value
     */
    public double getReferenceValue() {
        return referenceValue;
    }

    /** Get minimum parameter value.
     * @return minimum parameter value
     */
    public double getMinValue() {
        return minValue;
    }

    /** Get maximum parameter value.
     * @return maximum parameter value
     */
    public double getMaxValue() {
        return maxValue;
    }

    /** Get scale.
     * @return scale
     */
    public double getScale() {
        return scale;
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
        return (value - referenceValue) / scale;
    }

    /** Set normalized value.
     * <p>
     * The normalized value is a non-dimensional value
     * suitable for use as part of a vector in an optimization
     * process. It is computed as {@code (current - reference)/scale}.
     * </p>
     * @param normalized value
     * @exception OrekitException if an observer throws one
     */
    public void setNormalizedValue(final double normalized) throws OrekitException {
        setValue(referenceValue + scale * normalized);
    }

    /** Get current parameter value.
     * @return current parameter value
     */
    public double getValue() {
        return value;
    }

    /** Set parameter value.
     * <p>
     * If {@code newValue} is below {@link #getMinValue()}, it will
     * be silently to {@link #getMinValue()}. If {@code newValue} is
     * above {@link #getMaxValue()}, it will be silently to {@link
     * #getMaxValue()}.
     * </p>
     * @param newValue new value
     * @exception OrekitException if an observer throws one
     */
    public void setValue(final double newValue) throws OrekitException {
        final double previousValue = getValue();
        value = FastMath.max(minValue, FastMath.min(maxValue, newValue));
        for (final ParameterObserver observer : observers) {
            observer.valueChanged(previousValue, this);
        }
    }

    /** Configure a parameter selection status.
     * <p>
     * Selection is used for estimated parameters in orbit determination,
     * or to compute the Jacobian matrix in partial derivatives computation.
     * </p>
     * @param selected if true the parameter is selected,
     * otherwise it will be fixed
     */
    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    /** Check if parameter is selected.
     * <p>
     * Selection is used for estimated parameters in orbit determination,
     * or to compute the Jacobian matrix in partial derivatives computation.
     * </p>
     * @return true if parameter is selected, false if it is not
     */
    public boolean isSelected() {
        return selected;
    }

    /** Get a text representation of the parameter.
     * @return text representation of the parameter, in the form name = value.
     */
    public String toString() {
        return name + " = " + value;
    }

}
