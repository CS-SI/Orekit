/* Copyright 2025-2026 Hawkeye 360 (HE360)
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
package org.orekit.time.clocks;

import org.hipparchus.CalculusFieldElement;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Interface for clock field model.
 *
 * @param <T> type of the field elements
 * @author Brian Carter
 * @since 14.0
 */
public interface FieldClockModel<T extends CalculusFieldElement<T>> {

    /** Get validity start.
     * @return model validity start
     */
    AbsoluteDate getValidityStart();

    /** Get validity end.
     * @return model validity end
     */
    AbsoluteDate getValidityEnd();

    /** The field clock offset at a given time.
     *
     * @param date date at which offset is requested
     * @return the field clock offset
    */
    FieldClockOffset<T> getOffset(FieldAbsoluteDate<T> date);

    /** Get the clock offset value at date.
     * @param date date at which offset value is requested
     * @return clock offset value
     */
    default T getOffsetValue(final FieldAbsoluteDate<T> date) {
        return getOffset(date).getValue(date);
    }

    /** Create a non-field version of the instance.
     * @return non-field version of the instance
     */
    ClockModel toNonField();

}
