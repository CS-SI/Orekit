/* Copyright 2022-2025 Romain Serra
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

/**
 * Interface representing a closed time interval i.e. [a, b], possibly of infinite length.
 *
 * @author Romain Serra
 * @since 13.1
 * @see AbsoluteDate
 */
public interface TimeInterval {

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
     * Create instance from two dates in arbitrary order.
     * @param date date
     * @param otherDate other date
     * @return time interval
     */
    static TimeInterval of(final AbsoluteDate date, final AbsoluteDate otherDate) {
        if (otherDate.isBefore(date)) {
            return of(otherDate, date);
        }
        return new TimeInterval() {

            @Override
            public AbsoluteDate getStartDate() {
                return date;
            }

            @Override
            public AbsoluteDate getEndDate() {
                return otherDate;
            }
        };
    }
}
