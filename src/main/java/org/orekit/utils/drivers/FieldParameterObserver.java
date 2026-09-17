/* Copyright 20Z2-2026 Luc Maisonobe
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
import org.orekit.time.FieldAbsoluteDate;

/** Interface for observing parameters changes.
 * @param <T> type of the field elements
 * @see FieldParameterDriver
 * @author Luc Maisonobe
 * @since 14.0
 */
public interface FieldParameterObserver<T extends CalculusFieldElement<T>>
    extends BaseParameterObserver<FieldParameterDriver<T>, FieldParameterObserver<T>> {

    /** Notify that a parameter reference date has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousReferenceDate previous date (null if it is the first time
     * the reference date is changed)
     * @param driver parameter driver that has been changed
     */
    default void referenceDateChanged(final FieldAbsoluteDate<T> previousReferenceDate,
                                      final FieldParameterDriver<T> driver) {
        // nothing by default
    }

    /** Notify that a parameter reference value has been changed.
     * <p>
     * The default implementation does nothing
     * </p>
     * @param previousReferenceValue previous reference value
     * @param driver parameter driver that has been changed
     */
    default void referenceValueChanged(final T previousReferenceValue, final FieldParameterDriver<T> driver) {
        // nothing by default
    }

    /** Notify that a parameter value has been changed.
     * @param previousValue previous value
     * @param driver        parameter driver that has been changed
     */
    void valueChanged(T previousValue, FieldParameterDriver<T> driver);

}
