/* Copyright 2002-2024 Luc Maisonobe
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

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;

import java.util.concurrent.TimeUnit;

/** This class represents a time offset split into seconds and attoseconds.
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @see FieldAbsoluteDate
 * @since 13.1
 */
public class SplitOffset {

    /** Seconds in one day. */
    private static final long SECONDS_IN_DAY = 86400L;

    /** Seconds in one hour. */
    private static final long SECONDS_IN_HOUR = 3600L;

    /** Seconds in one minute. */
    private static final long SECONDS_IN_MINUTE = 60L;

    /** Milliseconds in one second. */
    private static final long MILLIS_IN_SECOND = 1000L;

    /** Microseconds in one second. */
    private static final long MICROS_IN_SECOND = 1000000L;

    /** Nanoseconds in one second. */
    private static final long NANOS_IN_SECOND = 1000000000L;

    /** Attoseconds in one second. */
    private static final long ATTOS_IN_SECOND = 1000000000000000000L;

    /** Attoseconds in one millisecond. */
    private static final long ATTOS_IN_MILLI  = 1000000000000000L;

    /** Attoseconds in one microsecond. */
    private static final long ATTOS_IN_MICRO  = 1000000000000L;

    /** Attoseconds in one nanosecond. */
    private static final long ATTOS_IN_NANO   = 1000000000L;

    /** Seconds part. */
    private final long seconds;

    /** AttoSeconds part. */
    private final long attoSeconds;

    /**
     * Build an offset from its components.
     * <p>
     * The components will be normalized so that {@link #getAttoSeconds()}
     * returns a value between {@code 0L} and {1000000000000000000L}
     * </p>
     * @param seconds seconds part
     * @param attoSeconds attoseconds part
     */
    public SplitOffset(final long seconds, final long attoSeconds) {
        final long qAtto = attoSeconds / ATTOS_IN_SECOND;
        final long rAtto = attoSeconds % ATTOS_IN_SECOND;
        if (rAtto < 0L) {
            this.seconds     = seconds + qAtto - 1L;
            this.attoSeconds = ATTOS_IN_SECOND + rAtto;
        } else {
            this.seconds     = seconds + qAtto;
            this.attoSeconds = rAtto;
        }
    }

    /**
     * Build an offset from a value in seconds.
     *
     * @param offset offset
     */
    public SplitOffset(final double offset) {
        if (offset < Long.MIN_VALUE || offset > Long.MAX_VALUE) {
            throw new OrekitException(OrekitMessages.OFFSET_OUT_OF_RANGE_FOR_TIME_UNIT,
                                      offset, TimeUnit.SECONDS, Long.MIN_VALUE, Long.MIN_VALUE);
        }

        final double tiSeconds = offset < 0.0 ? FastMath.ceil(offset) : FastMath.floor(offset);
        seconds     = (long) tiSeconds;
        attoSeconds = FastMath.round((offset - tiSeconds) * ATTOS_IN_SECOND);
    }

    /**
     * Build an offset from a value defined in some time unit.
     *
     * @param offset offset
     * @param unit   time unit in which {@code offset} is expressed
     */
    public SplitOffset(final long offset, final TimeUnit unit) {
        switch (unit) {
            case DAYS:
                checkOffset(offset, TimeUnit.DAYS, (Long.MAX_VALUE - SECONDS_IN_DAY / 2) / SECONDS_IN_DAY);
                seconds     = offset * SECONDS_IN_DAY;
                attoSeconds = 0L;
                break;
            case HOURS:
                checkOffset(offset, TimeUnit.HOURS, (Long.MAX_VALUE - SECONDS_IN_HOUR / 2) / SECONDS_IN_HOUR);
                seconds     = offset * SECONDS_IN_HOUR;
                attoSeconds = 0L;
                break;
            case MINUTES:
                checkOffset(offset, TimeUnit.MINUTES, (Long.MAX_VALUE - SECONDS_IN_MINUTE / 2) / SECONDS_IN_MINUTE);
                seconds     = offset * SECONDS_IN_MINUTE;
                attoSeconds = 0L;
                break;
            case SECONDS:
                seconds     = offset;
                attoSeconds = 0L;
                break;
            case MILLISECONDS: {
                final long s = offset / MILLIS_IN_SECOND;
                final long r = (offset % MILLIS_IN_SECOND) * ATTOS_IN_MILLI;
                if (r < 0L) {
                    seconds     = s - 1L;
                    attoSeconds = ATTOS_IN_SECOND + r;
                } else {
                    seconds     = s;
                    attoSeconds = r;
                }
                break;
            }
            case MICROSECONDS: {
                final long s = offset / MICROS_IN_SECOND;
                final long r = (offset % MICROS_IN_SECOND) * ATTOS_IN_MICRO;
                if (r < 0L) {
                    seconds     = s - 1L;
                    attoSeconds = ATTOS_IN_SECOND + r;
                } else {
                    seconds     = s;
                    attoSeconds = r;
                }
                break;
            }
            case NANOSECONDS: {
                final long s = offset / NANOS_IN_SECOND;
                final long r = (offset % NANOS_IN_SECOND) * ATTOS_IN_NANO;
                if (r < 0L) {
                    seconds     = s - 1L;
                    attoSeconds = ATTOS_IN_SECOND + r;
                } else {
                    seconds     = s;
                    attoSeconds = r;
                }
                break;
            }
            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Check if an offset is within bounds.
     * @param offset time offset
     * @param unit time unit
     * @param max maximum allowed offset in this time unit
     */
    private void checkOffset(final long offset, final TimeUnit unit,
                             final long max) {
        if (offset < -max || offset > max) {
            throw new OrekitException(OrekitMessages.OFFSET_OUT_OF_RANGE_FOR_TIME_UNIT,
                                      offset, unit, -max, max);
        }
    }

    /** Build an offset by adding two offsets.
     * @param o1 first offset
     * @param o2 second offset
     * @return o1+o2
     */
    public static SplitOffset add(final SplitOffset o1, final SplitOffset o2) {
        return new SplitOffset(o1.seconds + o2.seconds, o1.attoSeconds + o2.attoSeconds);
    }

/** Build an offset by subtracting one offset from another one.
     * @param o1 first offset
     * @param o2 second offset
     * @return o1-o2
     */
    public static SplitOffset subtract(final SplitOffset o1, final SplitOffset o2) {
        return new SplitOffset(o1.seconds - o2.seconds, o1.attoSeconds - o2.attoSeconds);
    }

    /** Get the offset in some time unit.
     * @param unit time unit
     * @return offset in this time unit, rounded to closest long
     */
    public long getRoundedOffset(final TimeUnit unit) {
        final long sign = seconds < 0L ? -1L : 1L;
        switch (unit) {
            case DAYS:
                return sign * ((sign * seconds + SECONDS_IN_DAY / 2) / SECONDS_IN_DAY);
            case HOURS:
                return sign * ((sign * seconds + SECONDS_IN_HOUR / 2) / SECONDS_IN_HOUR);
            case MINUTES:
                return sign * ((sign * seconds + SECONDS_IN_MINUTE / 2) / SECONDS_IN_MINUTE);
            case SECONDS:
                return seconds + ((attoSeconds >= ATTOS_IN_SECOND / 2) ? 1 : 0);
            case MILLISECONDS:
                return seconds * MILLIS_IN_SECOND +
                       (attoSeconds + ATTOS_IN_MILLI / 2) / ATTOS_IN_MILLI;
            case MICROSECONDS:
                return seconds * MICROS_IN_SECOND +
                       (attoSeconds + ATTOS_IN_MICRO / 2) / ATTOS_IN_MICRO;
            case NANOSECONDS:
                return seconds * NANOS_IN_SECOND +
                       (attoSeconds + ATTOS_IN_NANO / 2) / ATTOS_IN_NANO;
            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the seconds part of the offset.
     * @return seconds part of the offset
     */
    public long getSeconds() {
        return seconds;
    }

    /** Get the attoseconds part of the offset.
     * @return attoseconds part of the offset
     */
    public long getAttoSeconds() {
        return attoSeconds;
    }

}
