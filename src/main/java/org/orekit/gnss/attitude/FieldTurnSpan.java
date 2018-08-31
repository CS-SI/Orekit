/* Copyright 2002-2018 CS Systèmes d'Information
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

import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
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
class FieldTurnSpan<T extends RealFieldElement<T>> implements TimeStamped {

    /** Margin in seconds after turn end. */
    private final double endMargin;

    /** Best estimate of the start of the turn. */
    private FieldAbsoluteDate<T> start;

    /** Best estimate of the start of the turn. */
    private AbsoluteDate startDouble;

    /** Best estimate of the end of the turn, excluding margin. */
    private FieldAbsoluteDate<T> end;

    /** Best estimate of the end of the turn, including margin. */
    private FieldAbsoluteDate<T> endPlusMargin;

    /** Best estimate of the end of the turn, including margin. */
    private AbsoluteDate endPlusMarginDouble;

    /** Time between start and its estimation date. */
    private double startProjection;

    /** Time between end and its estimation date. */
    private double endProjection;

    /** Simple constructor.
     * @param start estimate of the start of the turn
     * @param end estimate of the end of the turn, excluding margin
     * @param estimationDate date at which turn boundaries have been estimated
     * @param endMargin margin in seconds after turn end
     */
    FieldTurnSpan(final FieldAbsoluteDate<T> start, final FieldAbsoluteDate<T> end,
                  final AbsoluteDate estimationDate, final double endMargin) {
        this.start               = start;
        this.startDouble         = start.toAbsoluteDate();
        this.end                 = end;
        this.endPlusMargin       = end.shiftedBy(endMargin);
        this.endPlusMarginDouble = endPlusMargin.toAbsoluteDate();
        this.startProjection     = FastMath.abs(start.durationFrom(estimationDate).getReal());
        this.endProjection       = FastMath.abs(endPlusMargin.durationFrom(estimationDate).getReal());
        this.endMargin           = endMargin;
    }

    /** Update the estimate of the turn boundaries.
     * <p>
     * Start and end boundaries are updated only if they are performed
     * at a time closer to the boundary than the previous estimate.
     * </p>
     * @param newStart new estimate of the start of the turn
     * @param newEnd new estimate of the end of the turn, excluding margin
     * @param estimationDate date at which turn boundaries have been estimated
     */
    public void update(final FieldAbsoluteDate<T> newStart, final FieldAbsoluteDate<T> newEnd,
                       final AbsoluteDate estimationDate) {

        // update the start date if this estimate is closer than the previous one
        final double newStartProjection = FastMath.abs(newStart.toAbsoluteDate().durationFrom(estimationDate));
        if (newStartProjection <= startProjection) {
            this.start           = newStart;
            this.startDouble     = newStart.toAbsoluteDate();
            this.startProjection = newStartProjection;
        }

        // update the end date if this estimate is closer than the previous one
        final double newEndProjection = FastMath.abs(newEnd.toAbsoluteDate().durationFrom(estimationDate));
        if (newEndProjection <= endProjection) {
            this.end                 = newEnd;
            this.endPlusMargin       = newEnd.shiftedBy(endMargin);
            this.endPlusMarginDouble = endPlusMargin.toAbsoluteDate();
            this.endProjection       = newEndProjection;
        }

    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return endPlusMarginDouble;
    }

    /** Get turn duration.
     * @return turn duration
     */
    public T getTurnDuration() {
        return end.durationFrom(start);
    }

    /** Get elapsed time since turn start.
     * @param date date to check
     * @return elapsed time from turn start to specified date
     */
    public T timeSinceTurnStart(final FieldAbsoluteDate<T> date) {
        return date.durationFrom(start);
    }

    /** Get turn start date.
     * @return turn start date
     */
    public FieldAbsoluteDate<T> getTurnStartDate() {
        return start;
    }

    /** Get turn end date (without margin).
     * @return turn end date (without margin)
     */
    public FieldAbsoluteDate<T> getTurnEndDate() {
        return end;
    }

    /** Check if a date is within range.
     * @param date date to check
     * @return true if date is within range extended by end margin,
     * both start and end + margin dates are included
     */
    public boolean inTurnTimeRange(final AbsoluteDate date) {
        return date.durationFrom(startDouble) >= 0 && date.durationFrom(endPlusMarginDouble) <= 0;
    }

}
