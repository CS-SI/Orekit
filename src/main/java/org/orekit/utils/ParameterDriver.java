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

import org.orekit.errors.OrekitException;


/** Class allowing to drive the value of a parameter.
 * <p>
 * This class is typically used as a bridge between an estimation
 * algorithm (typically orbit determination or optimizer) and an
 * internal parameter in a physical model that needs to be tuned,
 * or a bridge between a finite differences algorithm and an
 * internal parameter in a physical model that needs to be slightly
 * offset. The physical model will expose to the algorithm a
 * set of instances of this class so the algorithm can call the
 * {@link #setValue(double[]) setValue} method to update the
 * parameter value. Each time the value is set, the physical model
 * will be notified as it will implement a specialized version of
 * the {@link #valueChanged(double[]) valueChanged} method.
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
 * @author Luc Maisonobe
 * @since 8.0
 */
public abstract class ParameterDriver {

    /** Name of the parameter. */
    private final String name;

    /** Initial value. */
    private final double initialValue;

    /** Scaling factor. */
    private final double scale;

    /** Current value. */
    private double value;

    /** Selection status.
     * <p>
     * Selection is used for estimated parameters in orbit determination,
     * or to compute the Jacobian matrix in partial derivatives computation.
     * </p>
     */
    private boolean selected;

    /** Simple constructor.
     * <p>
     * At construction, the parameter is configured as <em>not</em> selected.
     * </p>
     * @param name name of the parameter
     * @param initialValue initial value of the parameter
     * @param scale scaling factor to convert the parameters value to
     * non-dimensional (typically set to the expected standard deviation of the
     * parameter)
     * @exception OrekitException if value is invalid for the driven model
     */
    protected ParameterDriver(final String name, final double initialValue,
                              final double scale)
        throws OrekitException {
        this.name         = name;
        this.initialValue = initialValue;
        this.scale        = scale;
        this.value        = initialValue;
        this.selected    = false;

        // ensure the physical model known about the initial value
        valueChanged(this.value);

    }

    /** Get name.
     * @return name
     */
    public String getName() {
        return name;
    }

    /** Get initial parameter value.
     * @return initial parameter value
     */
    public double getInitialValue() {
        return initialValue;
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
     * process. It is computed as {@code (current - initial)/scale}.
     * </p>
     * @return normalized value
     */
    public double getNormalizedValue() {
        return (value - initialValue) / scale;
    }

    /** Set normalized value.
     * <p>
     * The normalized value is a non-dimensional value
     * suitable for use as part of a vector in an optimization
     * process. It is computed as {@code (current - initial)/scale}.
     * </p>
     * @param normalized value
     * @exception OrekitException if normalized is invalid
     */
    public void setNormalizedValue(final double normalized) throws OrekitException {
        value = initialValue + scale * normalized;
        valueChanged(value);
    }

    /** Get current parameter value.
     * @return current parameter value
     */
    public double getValue() {
        return value;
    }

    /** Set parameter value.
     * @param newValue new value
     * @exception OrekitException if newValue is invalid
     */
    public void setValue(final double newValue) throws OrekitException {
        value = newValue;
        valueChanged(value);
    }

    /** Notify that the values has been changed.
     * @param newValue new value
     * @exception OrekitException if value is invalid for the driven model
     */
    protected abstract void valueChanged(final double newValue) throws OrekitException;

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

}
