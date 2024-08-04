/* Copyright 2002-2024 CS GROUP
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

import java.io.Serializable;

import org.hipparchus.CalculusFieldElement;

/** Offset between {@link UTCScale UTC} and  {@link TAIScale TAI} time scales.
 * <p>The {@link UTCScale UTC} and  {@link TAIScale TAI} time scales are two
 * scales offset with respect to each other. The {@link TAIScale TAI} scale is
 * continuous whereas the {@link UTCScale UTC} includes some discontinuity when
 * leap seconds are introduced by the <a href="http://www.iers.org/">International
 * Earth Rotation Service</a> (IERS).</p>
 * <p>This class represents the offset between the two scales that is
 * valid between two leap seconds occurrences. It handles both the linear offsets
 * used from 1961-01-01 to 1971-12-31 and the constant integer offsets used since
 * 1972-01-01.</p>
 * @author Luc Maisonobe
 * @see UTCScale
 * @see UTCTAIHistoryFilesLoader
 */
public class UTCTAIOffset implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20240720L;

    /** Nanoseconds in one second. */
    private static final int NANOS_IN_SECOND = 1000000000;

    /** Leap date. */
    private final AbsoluteDate leapDate;

    /** Leap date in Modified Julian Day. */
    private final int leapDateMJD;

    /** Offset start of validity date. */
    private final AbsoluteDate validityStart;

    /** Reference date for the slope multiplication as Modified Julian Day. */
    private final int mjdRef;

    /** Reference date for the slope multiplication. */
    private final AbsoluteDate reference;

    /** Value of the leap at offset validity start (in seconds). */
    private final double leap;

    /** Offset at validity start in seconds (TAI minus UTC). */
    private final SplitTime offset;

    /** Offset slope in nanoseconds per UTC second (TAI minus UTC / dUTC). */
    private final int slope;

    /** Simple constructor for a linear model.
     * @param leapDate leap date
     * @param leapDateMJD leap date in Modified Julian Day
     * @param leap value of the leap at offset validity start (in seconds)
     * @param offset offset in seconds (TAI minus UTC)
     * @param mjdRef reference date for the slope multiplication as Modified Julian Day
     * @param slope offset slope in nanoseconds per UTC second (TAI minus UTC / dUTC)
     * @param reference date for slope computations.
     */
    UTCTAIOffset(final AbsoluteDate leapDate, final int leapDateMJD,
                 final double leap, final SplitTime offset,
                 final int mjdRef, final int slope, final AbsoluteDate reference) {
        this.leapDate      = leapDate;
        this.leapDateMJD   = leapDateMJD;
        this.validityStart = leapDate.shiftedBy(leap);
        this.mjdRef        = mjdRef;
        this.reference     = reference;
        this.leap          = leap;
        this.offset        = offset;

        // at some absolute instant t₀, we can associate reading a₀ on a TAI clock and u₀ on a UTC clock
        // at this instant, the offset between TAI and UTC is therefore τ₀ = a₀ - u₀
        // at another absolute instant t₁, we can associate reading a₁ on a TAI clock and u₁ on a UTC clock
        // at this instant, the offset between TAI and UTC is therefore τ₁ = a₁ - u₁
        // the slope is defined according to offsets counted in UTC, i.e.:
        // τ₁ = τ₀ + (u₁ - u₀) * slope/n (where n = 10⁹ because the slope is in ns/s)
        // if we have a₁ - a₀ (i.e. dates in TAI) instead of u₁ - u₀, we need to invert the expression
        // we get: τ₁ = τ₀ + (a₁ - a₀) * slope / (n + slope)
        this.slope = slope;

    }

    /** Get the date of the start of the leap.
     * @return date of the start of the leap
     * @see #getValidityStart()
     */
    public AbsoluteDate getDate() {
        return leapDate;
    }

    /** Get the date of the start of the leap as Modified Julian Day.
     * @return date of the start of the leap as Modified Julian Day
     */
    public int getMJD() {
        return leapDateMJD;
    }

    /** Get the start time of validity for this offset.
     * <p>The start of the validity of the offset is {@link #getLeap()}
     * seconds after the start of the leap itself.</p>
     * @return start of validity date
     * @see #getDate()
     */
    public AbsoluteDate getValidityStart() {
        return validityStart;
    }

    /** Get the value of the leap at offset validity start (in seconds).
     * @return value of the leap at offset validity start (in seconds)
     */
    public double getLeap() {
        return leap;
    }

    /** Get the TAI - UTC offset in seconds.
     * @param date date at which the offset is requested
     * @return TAI - UTC offset in seconds.
     */
    public SplitTime getOffset(final AbsoluteDate date) {
        if (slope == 0) {
            // we use an if statement here so the offset computation returns
            // a finite value when date is AbsoluteDate.FUTURE_INFINITY
            // without this if statement, the multiplication between an
            // infinite duration and a zero slope would induce a NaN offset
            return offset;
        } else {

            // time during which slope applies
            final SplitTime delta = date.splitDurationFrom(reference);

            // accumulated drift
            final SplitTime drift = delta.multiply(slope).divide(slope + NANOS_IN_SECOND);

            return offset.add(drift);

        }
    }

    /** Get the TAI - UTC offset in seconds.
     * @param date date at which the offset is requested
     * @param <T> type of the filed elements
     * @return TAI - UTC offset in seconds.
     * @since 9.0
     */
    public <T extends CalculusFieldElement<T>> T getOffset(final FieldAbsoluteDate<T> date) {
        if (slope == 0) {
            // we use an if statement here so the offset computation returns
            // a finite value when date is FieldAbsoluteDate.getFutureInfinity(field)
            // without this if statement, the multiplication between an
            // infinite duration and a zero slope would induce a NaN offset
            return date.getField().getZero().newInstance(offset.toDouble());
        } else {
            // TODO perform complete computation
            return date.getField().getZero().newInstance(getOffset(date.toAbsoluteDate()).toDouble());
        }
    }

    /** Get the TAI - UTC offset in seconds.
     * @param date date components (in UTC) at which the offset is requested
     * @param time time components (in UTC) at which the offset is requested
     * @return TAI - UTC offset in seconds.
     */
    public SplitTime getOffset(final DateComponents date, final TimeComponents time) {
        if (slope == 0) {
            return offset;
        } else {

            // time during which slope applies
            final SplitTime delta = new SplitTime((date.getMJD() - mjdRef) * SplitTime.DAY.getSeconds() +
                                                  time.getSplitSecond().getSeconds(),
                                                  time.getSplitSecond().getAttoSeconds());

            // accumulated drift
            final SplitTime drift = delta.multiply(slope).divide(NANOS_IN_SECOND);

            return offset.add(drift);

        }
    }

}
