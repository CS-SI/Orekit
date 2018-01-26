/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.gnss.attitude;

import org.orekit.time.AbsoluteDate;

/** Holder for turn time range.
 * @see GNSSAttitudeContext#turnTimeRange()
 * @author Luc Maisonobe
 * @since 9.2
 */
class TurnTimeRange {

    /** Turn start date. */
    private final AbsoluteDate start;

    /** Turn end date. */
    private final AbsoluteDate end;

    /** Simple constructor.
     * @param start turn start date
     * @param end turn end date
     */
    TurnTimeRange(final AbsoluteDate start, final AbsoluteDate end) {
        this.start = start;
        this.end   = end;
    }

    /** Get the turn start date.
     * @return turn start date
     */
    public AbsoluteDate getStart() {
        return start;
    }

    /** Get the turn end date.
     * @return turn end date
     */
    public AbsoluteDate getEnd() {
        return end;
    }

    /** Check if a date is within range.
     * @param date date to check
     * @return true if date is within range
     */
    public boolean inRange(final AbsoluteDate date) {
        return date.compareTo(start) > 0 && date.compareTo(end) < 0;
    }

}
