/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.time;

import org.hipparchus.CalculusFieldElement;

/** Container for time stamped clock offset.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 12.1
 */
public class FieldClockOffset<T extends CalculusFieldElement<T>> implements FieldTimeStamped<T> {

    /** Date. */
    private final FieldAbsoluteDate<T> date;

    /** Clock offset. */
    private final T offset;

    /** Clock rate. */
    private final T rate;

    /** Clock acceleration. */
    private final T acceleration;

    /** Simple constructor.
     * @param date   date
     * @param offset clock offset
     * @param rate   clock rate (can be set to {@code null} if unknown)
     * @param acceleration clock acceleration (can be set to {@code null} if unknown)
     */
    public FieldClockOffset(final FieldAbsoluteDate<T> date, final T offset,
                            final T rate, final T acceleration) {
        this.date         = date;
        this.offset       = offset;
        this.rate         = rate;
        this.acceleration = acceleration;
    }

    /** {@inheritDoc} */
    @Override
    public FieldAbsoluteDate<T> getDate() {
        return date;
    }

    /** Get offset.
     * @return offset
     */
    public T getOffset() {
        return offset;
    }

    /** Get rate.
     * @return rate ({@code null} if unknown)
     */
    public T getRate() {
        return rate;
    }

    /** Get acceleration.
     * @return acceleration ({@code null} if unknown)
     */
    public T getAcceleration() {
        return acceleration;
    }

}
