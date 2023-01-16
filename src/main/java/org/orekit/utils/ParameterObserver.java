/* Copyright 2002-2023 CS GROUP
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
package org.orekit.utils;

import org.orekit.time.AbsoluteDate;


/** Interface for observing parameters changes.
 * @see ParameterDriver
 * @author Luc Maisonobe
 * @since 8.0
 */
public interface ParameterObserver {

    /** Notify that a parameter value has been changed.
     * @param previousValue previous value
     * @param driver parameter driver that has been changed
     * @param date date for which the parameter value have been updated
     */
    void valueChanged(double previousValue, ParameterDriver driver, AbsoluteDate date);

    /** Notify that a parameter value span map has been changed.
     * @param previousValueSpanMap previous value
     * @param driver parameter driver that has been changed
     */
    void valueSpanMapChanged(TimeSpanMap<Double> previousValueSpanMap, ParameterDriver driver);

    /** Notify that a parameter reference date has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousReferenceDate previous date (null if it is the first time
     * the reference date is changed)
     * @param driver parameter driver that has been changed
     * @since 9.0
     */
    default void referenceDateChanged(final AbsoluteDate previousReferenceDate, final ParameterDriver driver) {
        // nothing by default
    }

    /** Notify that a parameter name has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousName previous name
     * @param driver parameter driver that has been changed
     * @since 9.0
     */
    default void nameChanged(final String previousName, final ParameterDriver driver) {
        // nothing by default
    }

    /** Notify that a parameter selection status has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousSelection previous selection
     * @param driver parameter driver that has been changed
     * @since 9.0
     */
    default void selectionChanged(final boolean previousSelection, final ParameterDriver driver) {
        // nothing by default
    }

    /** Notify that a parameter estimation type (continuous or step) has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousIsContinuous previous estimation type, continuous estimation if true,
     * step estimation if not.
     * @param driver parameter driver that has been changed
     * @since 9.0
     */
    default void estimationTypeChanged(final boolean previousIsContinuous, final ParameterDriver driver) {
        // nothing by default
    }

    /** Notify that a parameter reference value has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousReferenceValue previous reference value
     * @param driver parameter driver that has been changed
     * @since 9.0
     */
    default void referenceValueChanged(final double previousReferenceValue, final ParameterDriver driver) {
        // nothing by default
    }

    /** Notify that a parameter minimum value has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousMinValue previous minimum value
     * @param driver parameter driver that has been changed
     * @since 9.0
     */
    default void minValueChanged(final double previousMinValue, final ParameterDriver driver) {
        // nothing by default
    }

    /** Notify that a parameter maximum value has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousMaxValue previous maximum value
     * @param driver parameter driver that has been changed
     * @since 9.0
     */
    default void maxValueChanged(final double previousMaxValue, final ParameterDriver driver) {
        // nothing by default
    }

    /** Notify that a parameter scale has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousScale previous scale
     * @param driver parameter driver that has been changed
     * @since 9.0
     */
    default void scaleChanged(final double previousScale, final ParameterDriver driver) {
        // nothing by default
    }

}
