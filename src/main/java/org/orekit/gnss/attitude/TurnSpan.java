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
package org.orekit.gnss.attitude;

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/**
 * Holder for time span of noon/night turns.
 *
 * <p>
 * The boundaries estimated are updated as
 * new points close to the span are evaluated.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 9.3
 */
class TurnSpan implements TimeStamped {

    /** Margin in seconds after turn end. */
    private final double endMargin;

    /** Best estimate of the start of the turn. */
    private AbsoluteDate start;

    /** Best estimate of the end of the turn, excluding margin. */
    private AbsoluteDate end;

    /** Best estimate of the end of the turn, including margin. */
    private AbsoluteDate endPlusMargin;

    /** Time between start and its estimation date. */
    private double startProjection;

    /** Time between end and its estimation date. */
    private double endProjection;

    /** Turn duration. */
    private double duration;

    /** Simple constructor.
     * @param start estimate of the start of the turn
     * @param end estimate of the end of the turn, excluding margin
     * @param estimationDate date at which turn boundaries have been estimated
     * @param endMargin margin in seconds after turn end
     */
    TurnSpan(final AbsoluteDate start, final AbsoluteDate end,
             final AbsoluteDate estimationDate, final double endMargin) {
        this.endMargin       = endMargin;
        this.start           = start;
        this.end             = end;
        this.endPlusMargin   = end.shiftedBy(endMargin);
        this.startProjection = FastMath.abs(start.durationFrom(estimationDate));
        this.endProjection   = FastMath.abs(endPlusMargin.durationFrom(estimationDate));
        this.duration        = end.durationFrom(start);
    }

    /** Update the estimate of the turn start.
     * <p>
     * Start boundary is updated only if it is estimated
     * from a time closer to the boundary than the previous estimate.
     * </p>
     * @param newStart new estimate of the start of the turn
     * @param estimationDate date at which turn start has been estimated
     */
    public void updateStart(final AbsoluteDate newStart,  final AbsoluteDate estimationDate) {

        // update the start date if this estimate is closer than the previous one
        final double newStartProjection = FastMath.abs(newStart.durationFrom(estimationDate));
        if (newStartProjection <= startProjection) {
            this.start           = newStart;
            this.startProjection = newStartProjection;
            this.duration        = end.durationFrom(start);
        }

    }

    /** Update the estimate of the turn end.
     * <p>
     * end boundary is updated only if it is estimated
     * from a time closer to the boundary than the previous estimate.
     * </p>
     * @param newEnd new estimate of the end of the turn
     * @param estimationDate date at which turn end has been estimated
     */
    public void updateEnd(final AbsoluteDate newEnd, final AbsoluteDate estimationDate) {

        // update the end date if this estimate is closer than the previous one
        final double newEndProjection = FastMath.abs(newEnd.durationFrom(estimationDate));
        if (newEndProjection <= endProjection) {
            this.end             = newEnd;
            this.endPlusMargin   = newEnd.shiftedBy(endMargin);
            this.endProjection   = newEndProjection;
            this.duration        = end.durationFrom(start);
        }

    }

    /** {@inheritDoc}
     * <p>
     * The global date of the turn is the turn end, including margin
     * </p>
     */
    @Override
    public AbsoluteDate getDate() {
        return endPlusMargin;
    }

    /** Get turn duration.
     * @return turn duration
     */
    public double getTurnDuration() {
        return duration;
    }

    /** Get turn start date.
     * @return turn start date
     */
    public AbsoluteDate getTurnStartDate() {
        return start;
    }

    /** Get turn end date (without margin).
     * @return turn end date (without margin)
     */
    public AbsoluteDate getTurnEndDate() {
        return end;
    }

    /** Check if a date is within range.
     * @param date date to check
     * @return true if date is within range extended by end margin,
     * both start and end + margin dates are included
     */
    public boolean inTurnTimeRange(final AbsoluteDate date) {
        return date.durationFrom(start) >= 0 && date.durationFrom(endPlusMargin) <= 0;
    }

}
