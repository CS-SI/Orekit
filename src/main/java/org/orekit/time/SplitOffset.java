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
import org.orekit.errors.OrekitInternalError;

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
     *
     * @param seconds seconds part
     * @param attoSeconds attoseconds part
     */
    public SplitOffset(final long seconds, final long attoSeconds) {
        this.seconds     = seconds;
        this.attoSeconds = attoSeconds;
    }

    /**
     * Build an offset from a value in seconds.
     *
     * @param offset offset
     */
    public SplitOffset(final double offset) {
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
                seconds     = offset * SECONDS_IN_DAY;
                attoSeconds = 0L;
                break;
            case HOURS:
                seconds     = offset * SECONDS_IN_HOUR;
                attoSeconds = 0L;
                break;
            case MINUTES:
                seconds     = offset * SECONDS_IN_MINUTE;
                attoSeconds = 0L;
                break;
            case SECONDS:
                seconds     = offset;
                attoSeconds = 0L;
                break;
            case MILLISECONDS:
                seconds     = offset / MILLIS_IN_SECOND;
                attoSeconds = (offset % MILLIS_IN_SECOND) * ATTOS_IN_MILLI;
                break;
            case MICROSECONDS:
                seconds     = offset / MICROS_IN_SECOND;
                attoSeconds = (offset % MICROS_IN_SECOND) * ATTOS_IN_MICRO;
                break;
            case NANOSECONDS:
                seconds     = offset / NANOS_IN_SECOND;
                attoSeconds = (offset % NANOS_IN_SECOND) * ATTOS_IN_NANO;
                break;
            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Build an offset by adding two offsets.
     * @param o1 first offset
     * @param o2 second offset
     * @return o1+o2
     */
    public static SplitOffset add(final SplitOffset o1, final SplitOffset o2) {
        final long seconds     = o1.seconds     + o2.seconds;
        final long attoSeconds = o1.attoSeconds + o2.attoSeconds;
        if (attoSeconds < 0) {
            return new SplitOffset(seconds - 1, attoSeconds + ATTOS_IN_SECOND);
        } else if (attoSeconds >= ATTOS_IN_SECOND) {
            return new SplitOffset(seconds + 1, attoSeconds - ATTOS_IN_SECOND);
        } else {
            return new SplitOffset(seconds, attoSeconds);
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
