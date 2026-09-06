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

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.TimeInterval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Common features for both {@link ParameterDriver} and {@link FieldParameterDriver}.
 * @param <P> type of the parameter driver
 * @param <O> type of the parameter observer
 * @see ParameterDriver
 * @see FieldParameterDriver
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class BaseParameterDriver<P extends BaseParameterDriver<P, O>,
                                          O extends BaseParameterObserver<P, O>> {

    /** Name of the parameter. */
    private String name;

    /** Scaling factor. */
    private double scale;

    /** Minimum value. */
    private double minValue;

    /** Maximum value. */
    private double maxValue;

    /** Validity interval.
     * @since 14.0
     */
    private TimeInterval validity;

    /** Selection status.
     * <p>
     * Selection is used for estimated parameters in orbit determination,
     * or to compute the Jacobian matrix in partial derivatives computation.
     * </p>
     */
    private boolean selected;

    /** Observers observing this driver. */
    private final List<O> observers;

    /**
     * Simple constructor.
     * <p>
     * At construction, the parameter is configured as <em>not</em> selected, the reference date is set to {@code null},
     * the value is set to the {@code referenceValue}.
     * </p>
     * @param name           name of the parameter
     * @param scale          scaling factor to convert the parameters value to non-dimensional (typically set to the
     *                       expected standard deviation of the parameter), it must be non-zero
     * @param minValue       minimum value allowed
     * @param maxValue       maximum value allowed
     * @param validity       validity interval
     */
    public BaseParameterDriver(final String name, final double scale,
                               final double minValue, final double maxValue,
                               final TimeInterval validity) {

        if (FastMath.abs(scale) <= Precision.SAFE_MIN) {
            throw new OrekitException(OrekitMessages.TOO_SMALL_SCALE_FOR_PARAMETER, name, scale);
        }

        this.name      = name;
        this.scale     = scale;
        this.minValue  = minValue;
        this.maxValue  = maxValue;
        this.validity  = validity;
        this.selected  = false;
        this.observers = new ArrayList<>();
    }

    /** Add an observer for this driver.
     * @param observer observer to add
     */
    public void addObserver(final O observer) {
        observers.add(observer);
    }

    /** Remove an observer.
     * @param observer observer to remove
     * @since 9.1
     */
    public void removeObserver(final O observer) {
        for (final Iterator<O> iterator = observers.iterator(); iterator.hasNext();) {
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
    public void replaceObserver(final O oldObserver, final O newObserver) {
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
    public List<O> getObservers() {
        return Collections.unmodifiableList(observers);
    }

    /** Get parameter driver general name.
     * @return name
     */
    public String getName() {
        return name;
    }

    /** Change the general name of this parameter driver.
     * @param name new name
     */
    public void setName(final String name) {
        final String previousName = this.name;
        this.name = name;
        @SuppressWarnings("unchecked")
        final P self = (P) this;
        for (final O observer : observers) {
            observer.nameChanged(previousName, self);
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

        // safety check
        if (minValue > maxValue) {
            throw new OrekitException(LocalizedCoreFormats.NUMBER_TOO_LARGE, minValue, maxValue);
        }

        final double previousMinValue = this.minValue;
        this.minValue = minValue;
        @SuppressWarnings("unchecked")
        final P self = (P) this;
        for (final O observer : observers) {
            observer.minValueChanged(previousMinValue, self);
        }

    }

    /** Get maximum parameter value.
     * @return maximum parameter value
     */
    public double getMaxValue() {
        return maxValue;
    }

    /** Set maximum parameter value.
     * @param maxValue the maximum value to set.
     */
    public void setMaxValue(final double maxValue) {

        // safety check
        if (maxValue < minValue) {
            throw new OrekitException(LocalizedCoreFormats.NUMBER_TOO_SMALL, maxValue, minValue);
        }

        final double previousMaxValue = this.maxValue;
        this.maxValue = maxValue;
        @SuppressWarnings("unchecked")
        final P self = (P) this;
        for (final O observer : observers) {
            observer.maxValueChanged(previousMaxValue, self);
        }

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
        @SuppressWarnings("unchecked")
        final P self = (P) this;
        for (final O observer : observers) {
            observer.scaleChanged(previousScale, self);
        }
    }

    /** Get the validity interval.
     * @return validity interval
     * @since 14.0
     */
    public TimeInterval getValidity() {
        return validity;
    }

    /** Set the validity interval.
     * @param validity validity interval
     * @since 14.0
     */
    public void setValidity(final TimeInterval validity) {
        final TimeInterval previousValidity = getValidity();
        this.validity = validity;
        @SuppressWarnings("unchecked")
        final P self = (P) this;
        for (final O observer : observers) {
            observer.validityChanged(previousValidity, self);
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
        @SuppressWarnings("unchecked")
        final P self = (P) this;
        for (final O observer : observers) {
            observer.selectionChanged(previousSelection, self);
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

}
