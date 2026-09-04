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

import org.orekit.time.TimeInterval;

/** Interface for observing parameters changes (except the value itself).
 * @param <P> type of the parameter driver
 * @param <O> type of the parameter observer
 * @see BaseParameterDriver
 * @author Luc Maisonobe
 * @since 14.0
 */
public interface BaseParameterObserver<P extends BaseParameterDriver<P, O>,
                                       O extends BaseParameterObserver<P, O>> {

    /** Notify that a parameter name has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousName previous name
     * @param driver parameter driver that has been changed
     * @since 9.0
     */
    default void nameChanged(final String previousName, final P driver) {
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
    default void selectionChanged(final boolean previousSelection, final P driver) {
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
    default void minValueChanged(final double previousMinValue, final P driver) {
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
    default void maxValueChanged(final double previousMaxValue, final P driver) {
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
    default void scaleChanged(final double previousScale, final P driver) {
        // nothing by default
    }

    /** Notify that a parameter validity interval has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousValidity previous validity interval
     * @param driver parameter driver that has been changed
     * @since 14.0
     */
    default void validityChanged(final TimeInterval previousValidity, final P driver) {
        // nothing by default
    }

}
