/* Copyright 2002-2020 CS Group
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
package org.orekit.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;


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
    private String name;

    /** Reference value. */
    private double referenceValue;

    /** Scaling factor. */
    private double scale;

    /** Minimum value. */
    private double minValue;

    /** Maximum value. */
    private double maxValue;

    /** Reference date.
     * @since 9.0
     */
    private AbsoluteDate referenceDate;

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
     * the reference date is set to {@code null} and the value is set to the
     * {@code referenceValue}.
     * </p>
     * @param name name of the parameter
     * @param referenceValue reference value of the parameter
     * @param scale scaling factor to convert the parameters value to
     * non-dimensional (typically set to the expected standard deviation of the
     * parameter), it must be non-zero
     * @param minValue minimum value
     * @param maxValue maximum value
     */
    public ParameterDriver(final String name, final double referenceValue,
                           final double scale, final double minValue,
                           final double maxValue) {
        if (FastMath.abs(scale) <= Precision.SAFE_MIN) {
            throw new OrekitException(OrekitMessages.TOO_SMALL_SCALE_FOR_PARAMETER,
                                      name, scale);
        }
        this.name           = name;
        this.referenceValue = referenceValue;
        this.scale          = scale;
        this.minValue       = minValue;
        this.maxValue       = maxValue;
        this.referenceDate  = null;
        this.value          = referenceValue;
        this.selected       = false;
        this.observers      = new ArrayList<>();
    }


    /** Add an observer for this driver.
     * <p>
     * The observer {@link ParameterObserver#valueChanged(double, ParameterDriver)
     * valueChanged} method is called once automatically when the
     * observer is added, and then called at each value change.
     * </p>
     * @param observer observer to add
          * while being updated
     */
    public void addObserver(final ParameterObserver observer) {
        observers.add(observer);
        observer.valueChanged(getValue(), this);
    }

    /** Remove an observer.
     * @param observer observer to remove
     * @since 9.1
     */
    public void removeObserver(final ParameterObserver observer) {
        for (final Iterator<ParameterObserver> iterator = observers.iterator(); iterator.hasNext();) {
            if (iterator.next() == observer) {
                iterator.remove();
                return;
            }
        }
    }

    /** Replace an observer.
     * @param oldObserver observer to replace
     * @param newObserver new observer to use
     * @since 10.1
     */
    public void replaceObserver(final ParameterObserver oldObserver, final ParameterObserver newObserver) {
        for (int i = 0; i < observers.size(); ++i) {
            if (observers.get(i) == oldObserver) {
                observers.set(i, newObserver);
            }
        }
    }

    /** Get the observers for this driver.
     * @return an unmodifiable view of the observers for this driver
     * @since 9.1
     */
    public List<ParameterObserver> getObservers() {
        return Collections.unmodifiableList(observers);
    }

    /** Change the name of this parameter driver.
     * @param name new name
     */
    public void setName(final String name) {
        final String previousName = this.name;
        this.name = name;
        for (final ParameterObserver observer : observers) {
            observer.nameChanged(previousName, this);
        }
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

    /** Set reference parameter value.
     * @since 9.3
     * @param referenceValue the reference value to set.
     */
    public void setReferenceValue(final double referenceValue) {
        final double previousReferenceValue = this.referenceValue;
        this.referenceValue = referenceValue;
        for (final ParameterObserver observer : observers) {
            observer.referenceValueChanged(previousReferenceValue, this);
        }
    }

    /** Get minimum parameter value.
     * @return minimum parameter value
     */
    public double getMinValue() {
        return minValue;
    }

    /** Set minimum parameter value.
     * @since 9.3
     * @param minValue the minimum value to set.
     */
    public void setMinValue(final double minValue) {
        final double previousMinValue = this.minValue;
        this.minValue = minValue;
        for (final ParameterObserver observer : observers) {
            observer.minValueChanged(previousMinValue, this);
        }
        // Check if current value is not out of min/max range
        setValue(value);
    }

    /** Get maximum parameter value.
     * @return maximum parameter value
     */
    public double getMaxValue() {
        return maxValue;
    }

    /** Set maximum parameter value.
     * @since 9.3
     * @param maxValue the maximum value to set.
     */
    public void setMaxValue(final double maxValue) {
        final double previousMaxValue = this.maxValue;
        this.maxValue = maxValue;
        for (final ParameterObserver observer : observers) {
            observer.maxValueChanged(previousMaxValue, this);
        }
        // Check if current value is not out of min/max range
        setValue(value);
    }

    /** Get scale.
     * @return scale
     */
    public double getScale() {
        return scale;
    }

    /** Set scale.
     * @since 9.3
     * @param scale the scale to set.
     */
    public void setScale(final double scale) {
        final double previousScale = this.scale;
        this.scale = scale;
        for (final ParameterObserver observer : observers) {
            observer.scaleChanged(previousScale, this);
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
        return (value - referenceValue) / scale;
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
        setValue(referenceValue + scale * normalized);
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
        for (final ParameterObserver observer : observers) {
            observer.referenceDateChanged(previousReferenceDate, this);
        }
    }

    /** Get current parameter value.
     * @return current parameter value
     */
    public double getValue() {
        return value;
    }

    /** Get the value as a derivative structure.
     * @param factory factory for the derivatives
     * @param indices indices of the differentiation parameters in derivatives computations
     * @return value with derivatives
     * @since 9.3
     */
    public DerivativeStructure getValue(final DSFactory factory, final Map<String, Integer> indices) {
        final Integer index = indices.get(name);
        return (index == null) ? factory.constant(value) : factory.variable(index, value);
    }

    /** Set parameter value.
     * <p>
     * If {@code newValue} is below {@link #getMinValue()}, it will
     * be silently set to {@link #getMinValue()}. If {@code newValue} is
     * above {@link #getMaxValue()}, it will be silently set to {@link
     * #getMaxValue()}.
     * </p>
     * @param newValue new value
     */
    public void setValue(final double newValue) {
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
        final boolean previousSelection = isSelected();
        this.selected = selected;
        for (final ParameterObserver observer : observers) {
            observer.selectionChanged(previousSelection, this);
        }
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
