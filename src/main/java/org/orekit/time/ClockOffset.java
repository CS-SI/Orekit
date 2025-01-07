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

/** Container for time stamped clock offset.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class ClockOffset implements TimeStamped {

    /** Date. */
    private final AbsoluteDate date;

    /** Clock offset. */
    private final double offset;

    /** Clock rate. */
    private final double rate;

    /** Clock acceleration. */
    private final double acceleration;

    /** Simple constructor.
     * @param date   date
     * @param offset clock offset
     * @param rate   clock rate (can be set to {@code Double.NaN} if unknown)
     * @param acceleration clock acceleration (can be set to {@code Double.NaN} if unknown)
     */
    public ClockOffset(final AbsoluteDate date, final double offset,
                       final double rate, final double acceleration) {
        this.date         = date;
        this.offset       = offset;
        this.rate         = rate;
        this.acceleration = acceleration;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get offset.
     * @return offset
     */
    public double getOffset() {
        return offset;
    }

    /** Get rate.
     * @return rate ({@code Double.NaN} if unknown)
     */
    public double getRate() {
        return rate;
    }

    /** Get acceleration.
     * @return acceleration ({@code Double.NaN} if unknown)
     */
    public double getAcceleration() {
        return acceleration;
    }

}
