/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.rinex.clock;

import org.orekit.time.AbsoluteDate;

/** Reference clock with its validity time span.
 * @since 14.0
 */
class ReferenceClock {

    /** Receiver/satellite embedding the reference clock name. */
    private final String referenceName;

    /** Clock ID. */
    private final String clockID;

    /** A priori clock constraint (in seconds). */
    private final double clockConstraint;

    /** Start date of the validity period. */
    private final AbsoluteDate startDate;

    /** End date of the validity period. */
    private final AbsoluteDate endDate;

    /** Constructor.
     * @param referenceName   the name of the receiver/satellite embedding the reference clock
     * @param clockID         the clock ID
     * @param clockConstraint the a priori clock constraint
     * @param startDate       the validity period start date
     * @param endDate         the validity period end date
     */
    public ReferenceClock(final String referenceName, final String clockID, final double clockConstraint,
                          final AbsoluteDate startDate, final AbsoluteDate endDate) {
        this.referenceName = referenceName;
        this.clockID = clockID;
        this.clockConstraint = clockConstraint;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /** Getter for the name of the receiver/satellite embedding the reference clock.
     * @return the name of the receiver/satellite embedding the reference clock
     */
    public String getReferenceName() {
        return referenceName;
    }

    /** Getter for the clock ID.
     * @return the clock ID
     */
    public String getClockID() {
        return clockID;
    }

    /** Getter for the clock constraint.
     * @return the clock constraint
     */
    public double getClockConstraint() {
        return clockConstraint;
    }

    /** Getter for the validity period start date.
     * @return the validity period start date
     */
    public AbsoluteDate getStartDate() {
        return startDate;
    }

    /** Getter for the validity period end date.
     * @return the validity period end date
     */
    public AbsoluteDate getEndDate() {
        return endDate;
    }

}
