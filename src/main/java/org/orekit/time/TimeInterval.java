/* Copyright 2022-2026 Romain Serra
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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Interface representing a closed time interval i.e. [a, b], possibly of infinite length.
 *
 * @author Romain Serra
 * @since 13.1
 * @see AbsoluteDate
 */
public interface TimeInterval {

    /** Interval covering the entire timeline
     * from {@link AbsoluteDate#PAST_INFINITY} to {@link AbsoluteDate#FUTURE_INFINITY}.
     * @since 14.0
     */
    TimeInterval UNLIMITED = TimeInterval.of(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY, true);

    /**
     * Getter for the left end of the interval.
     * @return left end
     */
    AbsoluteDate getStartDate();

    /**
     * Getter for the right end of the interval.
     * @return right end
     */
    AbsoluteDate getEndDate();

    /**
     * Computes the interval length in seconds.
     * @return duration
     */
    default double duration() {
        return getEndDate().durationFrom(getStartDate());
    }

    /**
     * Method returning true if and only if the dated input is contained within the closed interval.
     * @param timeStamped time stamped object
     * @return boolean on inclusion
     */
    default boolean contains(final TimeStamped timeStamped) {
        final AbsoluteDate date = timeStamped.getDate();
        return getStartDate().isBeforeOrEqualTo(date) && getEndDate().isAfterOrEqualTo(date);
    }

    /**
     * Method returning true if and only if input (also a closed time interval) contains the instance.
     * @param interval time interval
     * @return boolean on inclusion
     */
    default boolean contains(final TimeInterval interval) {
        return (getEndDate().isAfterOrEqualTo(interval.getEndDate())) && (getStartDate().isBeforeOrEqualTo(interval.getStartDate()));
    }

    /**
     * Method returning true if and only if input (also a closed time interval) intersects the instance.
     * @param interval time interval
     * @return boolean on intersection
     */
    default boolean intersects(final TimeInterval interval) {
        return (getEndDate().isAfterOrEqualTo(interval.getStartDate())) && (getStartDate().isBeforeOrEqualTo(interval.getEndDate()));
    }

    /**
     * Create instance from two dates.
     *
     * @param date                  date
     * @param otherDate             other date
     * @param allowNonChronological if true, the two dates can be in arbitrary order,
     *                              they will be sorted internally. If false and {@code otherDate}
     *                              is before {@code date}, then an exception is triggered
     * @return time interval
     * @since 14.0
     */
    static TimeInterval of(final AbsoluteDate date, final AbsoluteDate otherDate,
                           final boolean allowNonChronological) {

        // check order
        final AbsoluteDate start;
        final AbsoluteDate end;
        if (otherDate.isBefore(date)) {
            if (allowNonChronological) {
                // reorder dates
                start = otherDate;
                end   = date;
            } else {
                throw new OrekitException(OrekitMessages.NON_CHRONOLOGICALLY_SORTED_ENTRIES,
                                          date, otherDate, date.durationFrom(otherDate));
            }
        } else {
            start = date;
            end   = otherDate;
        }

        // create instance
        return new TimeInterval() {

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getStartDate() {
                return start;
            }

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getEndDate() {
                return end;
            }

        };

    }

    /**
     * Create instance from two dates in arbitrary order.
     * @param date start (or end) date
     * @param duration duration, in seconds (if positive, time interval is from date to date + duration,
     *                 if negative, the time interval will be from date - duration to date)
     * @return time interval
     * @since 14.0
     */
    static TimeInterval of(final AbsoluteDate date, final double duration) {
        return of(date, date.shiftedBy(duration), true);
    }

}
