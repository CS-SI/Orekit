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

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitInternalError;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/** This class represents a time offset split into seconds and attoseconds.
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @see FieldAbsoluteDate
 * @since 13.1
 */
public class SplitOffset implements Serializable {

    /** Indicator for NaN offset. */
    private static final long NAN_INDICATOR      = -1L;

    /** Indicator for infinite offset. */
    private static final long INFINITY_INDICATOR = -2L;

    /** Milliseconds in one second. */
    private static final long MILLIS_IN_SECOND = 1000L;

    /** Microseconds in one second. */
    private static final long MICROS_IN_SECOND = 1000000L;

    /** Nanoseconds in one second. */
    private static final long NANOS_IN_SECOND = 1000000000L;

    /** Attoseconds in one second. */
    private static final long ATTOS_IN_SECOND = 1000000000000000000L;

    /** Attoseconds in one half-second. */
    private static final long ATTOS_IN_HALF_SECOND = 500000000000000000L;

    /** Serializable UID. */
    private static final long serialVersionUID = 20240711L;

    /** Split offset representing 0. */
    public static final SplitOffset ZERO = new SplitOffset(0L, 0L);

    /** Split offset representing 1 attosecond. */
    public static final SplitOffset ATTOSECOND = new SplitOffset(0L, 1L);

    /** Split offset representing 1 femtosecond. */
    public static final SplitOffset FEMTOSECOND = new SplitOffset(0L, 1000L);

    /** Split offset representing 1 picosecond. */
    public static final SplitOffset PICOSECOND = new SplitOffset(0L, 1000000L);

    /** Split offset representing 1 nanosecond. */
    public static final SplitOffset NANOSECOND = new SplitOffset(0L, 1000000000L);

    /** Split offset representing 1 microsecond. */
    public static final SplitOffset MICROSECOND = new SplitOffset(0L, 1000000000000L);

    /** Split offset representing 1 millisecond. */
    public static final SplitOffset MILLISECOND = new SplitOffset(0L, 1000000000000000L);

    /** Split offset representing 1 second. */
    public static final SplitOffset SECOND = new SplitOffset(1L, 0L);

    /** Split offset representing 1 minute. */
    public static final SplitOffset MINUTE = new SplitOffset(60L, 0L);

    /** Split offset representing 1 hour. */
    public static final SplitOffset HOUR = new SplitOffset(3600L, 0L);

    /** Split offset representing 1 day. */
    public static final SplitOffset DAY = new SplitOffset(86400L, 0L);

    /** Split offset representing a NaN. */
    public static final SplitOffset NaN = new SplitOffset(Double.NaN);

    /** Split offset representing netgative infinity. */
    public static final SplitOffset NEGATIVE_INFINITY = new SplitOffset(Double.NEGATIVE_INFINITY);

    /** Split offset representing netgative infinity. */
    public static final SplitOffset POSITIVE_INFINITY = new SplitOffset(Double.POSITIVE_INFINITY);

    /** Seconds part. */
    private final long seconds;

    /** AttoSeconds part. */
    private final long attoSeconds;

    /**
     * Build an offset by adding several offsets.
     * @param offsets offsets to add
     */
    public SplitOffset(final SplitOffset...offsets) {
        SplitOffset sum = SplitOffset.ZERO;
        for (final SplitOffset offset : offsets) {
            sum = SplitOffset.add(sum, offset);
        }
        this.seconds     = sum.getSeconds();
        this.attoSeconds = sum.getAttoSeconds();
    }

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
        long normalizedSeconds;
        long normalizedAttoSeconds;
        try {
            final long qAtto = attoSeconds / ATTOS_IN_SECOND;
            final long rAtto = attoSeconds % ATTOS_IN_SECOND;
            if (rAtto < 0L) {
                normalizedSeconds     = FastMath.subtractExact(FastMath.addExact(seconds, qAtto), 1L);
                normalizedAttoSeconds = ATTOS_IN_SECOND + rAtto;
            } else {
                normalizedSeconds     = FastMath.addExact(seconds, qAtto);
                normalizedAttoSeconds = rAtto;
            }
        } catch (MathRuntimeException mre) {
            // there was an overflow
            normalizedSeconds     = seconds < 0L ? Long.MIN_VALUE : Long.MAX_VALUE;
            normalizedAttoSeconds = INFINITY_INDICATOR;
        }

        // store normalized values
        this.seconds     = normalizedSeconds;
        this.attoSeconds = normalizedAttoSeconds;

    }

    /**
     * Build an offset from a value in seconds.
     *
     * @param offset offset
     */
    public SplitOffset(final double offset) {
        if (Double.isNaN(offset)) {
            seconds     = 0L;
            attoSeconds = NAN_INDICATOR;
        } else if (offset < Long.MIN_VALUE || offset > Long.MAX_VALUE) {
            seconds     = offset < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
            attoSeconds = INFINITY_INDICATOR;
        } else {
            final double tiSeconds  = FastMath.rint(offset);
            final double subSeconds = offset - tiSeconds;
            if (subSeconds < 0L) {
                seconds     = (long) tiSeconds - 1L;
                attoSeconds = FastMath.round(subSeconds * ATTOS_IN_SECOND) + ATTOS_IN_SECOND;
            } else {
                seconds     = (long) tiSeconds;
                attoSeconds = FastMath.round(subSeconds * ATTOS_IN_SECOND);
            }
        }
    }

    /**
     * Build an offset from a value defined in some time unit.
     *
     * @param offset offset
     * @param unit   time unit in which {@code offset} is expressed
     */
    public SplitOffset(final long offset, final TimeUnit unit) {
        switch (unit) {
            case DAYS: {
                final long limit = (Long.MAX_VALUE - DAY.seconds / 2) / DAY.seconds;
                if (offset < -limit) {
                    seconds     = Long.MIN_VALUE;
                    attoSeconds = INFINITY_INDICATOR;
                } else if (offset > limit) {
                    seconds     = Long.MAX_VALUE;
                    attoSeconds = INFINITY_INDICATOR;
                } else {
                    seconds = offset * DAY.seconds;
                    attoSeconds = 0L;
                }
                break;
            }
            case HOURS: {
                final long limit = (Long.MAX_VALUE - HOUR.seconds / 2) / HOUR.seconds;
                if (offset < -limit) {
                    seconds     = Long.MIN_VALUE;
                    attoSeconds = INFINITY_INDICATOR;
                } else if (offset > limit) {
                    seconds     = Long.MAX_VALUE;
                    attoSeconds = INFINITY_INDICATOR;
                } else {
                    seconds     = offset * HOUR.seconds;
                    attoSeconds = 0L;
                }
                break;
            }
            case MINUTES: {
                final long limit = (Long.MAX_VALUE - MINUTE.seconds / 2) / MINUTE.seconds;
                if (offset < -limit) {
                    seconds     = Long.MIN_VALUE;
                    attoSeconds = INFINITY_INDICATOR;
                } else if (offset > limit) {
                    seconds     = Long.MAX_VALUE;
                    attoSeconds = INFINITY_INDICATOR;
                } else {
                    seconds     = offset * MINUTE.seconds;
                    attoSeconds = 0L;
                }
                break;
            }
            case SECONDS:
                seconds     = offset;
                attoSeconds = 0L;
                break;
            case MILLISECONDS: {
                final long s = offset / MILLIS_IN_SECOND;
                final long r = (offset % MILLIS_IN_SECOND) * MILLISECOND.attoSeconds;
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
                final long r = (offset % MICROS_IN_SECOND) * MICROSECOND.attoSeconds;
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
                final long r = (offset % NANOS_IN_SECOND) * NANOSECOND.attoSeconds;
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

    /** check if the offset is zero.
     * @return true if the offset iz zero
     */
    public boolean isZero() {
        return seconds == 0L && attoSeconds == 0L;
    }

    /** Check if offset is finite (i.e. neither {@link #isNaN() NaN} nor {@link #isInfinite() infinite)}.
     * @return true if offset is finite
     * @see #isNaN()
     * @see #isInfinite()
     * @see #isNegativeInfinity()
     * @see #isPositiveInfinity()
     */
    public boolean isFinite() {
        return attoSeconds >= 0L;
    }

    /** Check if offset is NaN.
     * @return true if offset is NaN
     * @see #isFinite()
     * @see #isInfinite()
     * @see #isNegativeInfinity()
     * @see #isPositiveInfinity()
     */
    public boolean isNaN() {
        return attoSeconds == NAN_INDICATOR;
    }

    /** Check if offset is infinity.
     * @return true if offset is infinity
     * @see #isFinite()
     * @see #isNaN()
     * @see #isNegativeInfinity()
     * @see #isPositiveInfinity()
     */
    public boolean isInfinite() {
        return attoSeconds == INFINITY_INDICATOR;
    }

    /** Check if offset is positive infinity.
     * @return true if offset is positive infinity
     * @see #isFinite()
     * @see #isNaN()
     * @see #isInfinite()
     * @see #isNegativeInfinity()
     */
    public boolean isPositiveInfinity() {
        return isInfinite() && seconds > 0L;
    }

    /** Check if offset is negative infinity.
     * @return true if offset is negative infinity
     * @see #isFinite()
     * @see #isNaN()
     * @see #isInfinite()
     * @see #isPositiveInfinity()
     */
    public boolean isNegativeInfinity() {
        return isInfinite() && seconds < 0L;
    }

    /** Build an offset by adding two offsets.
     * @param o1 first offset
     * @param o2 second offset
     * @return o1+o2
     */
    public static SplitOffset add(final SplitOffset o1, final SplitOffset o2) {
        if (o1.attoSeconds < 0 || o2.attoSeconds < 0) {
            // gather all special cases in one big check to avoid rare multiple tests
            if (o1.isNaN() ||
                o2.isNaN() ||
                o1.isPositiveInfinity() && o2.isNegativeInfinity() ||
                o1.isNegativeInfinity() && o2.isPositiveInfinity()) {
                return NaN;
            } else if (o1.isInfinite()) {
                // o2 is either a finite offset or the same infinity as o1
                return o1;
            } else {
                // o1 is either a finite offset or the same infinity as o2
                return o2;
            }
        } else {
            // regular addition between two finite offsets
            return new SplitOffset(o1.seconds + o2.seconds, o1.attoSeconds + o2.attoSeconds);
        }
    }

    /** Build an offset by subtracting one offset from another one.
     * @param o1 first offset
     * @param o2 second offset
     * @return o1-o2
     */
    public static SplitOffset subtract(final SplitOffset o1, final SplitOffset o2) {
        if (o1.attoSeconds < 0 || o2.attoSeconds < 0) {
            // gather all special cases in one big check to avoid rare multiple tests
            if (o1.isNaN() ||
                o2.isNaN() ||
                o1.isPositiveInfinity() && o2.isPositiveInfinity() ||
                o1.isNegativeInfinity() && o2.isNegativeInfinity()) {
                return NaN;
            } else if (o1.isInfinite()) {
                // o2 is either a finite offset or the infinity opposite to o1
                return o1;
            } else {
                // o1 is either a finite offset or the infinity opposite to o2
                return o2.isPositiveInfinity() ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
            }
        } else {
            // regular subtraction between two finite offsets
            return new SplitOffset(o1.seconds - o2.seconds, o1.attoSeconds - o2.attoSeconds);
        }
    }

    /** Get the offset in some time unit.
     * @param unit time unit
     * @return offset in this time unit, rounded to closest long,
     * returns arbitrarily {@link Long#MAX_VALUE} for {@link #isNaN() NaN offsets}
     */
    public long getRoundedOffset(final TimeUnit unit) {

        // handle special cases
        if (attoSeconds < 0) {
            // gather all special cases in one big check to avoid rare multiple tests
            return (isNaN() || seconds >= 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
        }

        final long sign = seconds < 0L ? -1L : 1L;
        switch (unit) {
            case DAYS:
                return sign * ((sign * seconds + DAY.seconds / 2) / DAY.seconds);
            case HOURS:
                return sign * ((sign * seconds + HOUR.seconds / 2) / HOUR.seconds);
            case MINUTES:
                return sign * ((sign * seconds + MINUTE.seconds / 2) / MINUTE.seconds);
            case SECONDS:
                return seconds + ((attoSeconds >= ATTOS_IN_SECOND / 2) ? 1 : 0);
            case MILLISECONDS:
                return seconds * MILLIS_IN_SECOND +
                       (attoSeconds + MILLISECOND.attoSeconds / 2) / MILLISECOND.attoSeconds;
            case MICROSECONDS:
                return seconds * MICROS_IN_SECOND +
                       (attoSeconds + MICROSECOND.attoSeconds / 2) / MICROSECOND.attoSeconds;
            case NANOSECONDS:
                return seconds * NANOS_IN_SECOND +
                       (attoSeconds + NANOSECOND.attoSeconds / 2) / NANOSECOND.attoSeconds;
            default:
                throw new OrekitInternalError(null);
        }
    }

    /** Get the normalized seconds part of the offset.
     * @return normalized seconds part of the offset
     */
    public long getSeconds() {
        return seconds;
    }

    /** Get the normalized attoseconds part of the offset.
     * <p>
     * The normalized attoseconds is always between {@code 0L} inclusive and
     * {@code 1000000000000000000L} exclusive for finite offsets. It is negative
     * for {@link #isNaN() NaN} or {@link #isInfinite() infinite} offsets.
     * </p>
     * @return normalized attoseconds part of the offset
     */
    public long getAttoSeconds() {
        return attoSeconds;
    }

    /** Get the offset collapsed into a single double.
     * <p>
     * Beware that lots of accuracy is lost when combining {@link #getSeconds()} and {@link #getAttoSeconds()}
     * into a single double.
     * </p>
     * @return offset as a single double
     */
    public double toDouble() {
        if (isFinite()) {
            // regular value
            long closeSeconds      = seconds;
            long signedAttoSeconds = attoSeconds;
            if (attoSeconds > ATTOS_IN_HALF_SECOND) {
                // we are closer to next second than to previous one
                // take this into account in the computation
                // in order to avoid losing precision
                closeSeconds++;
                signedAttoSeconds -= ATTOS_IN_SECOND;
            }
            return closeSeconds + ((double) signedAttoSeconds) / ATTOS_IN_SECOND;
        } else {
            // special values
            return isNaN() ? Double.NaN : FastMath.copySign(Double.POSITIVE_INFINITY, seconds);
        }
    }

    /** Compare the instance with another one.
     * <p>
     * Not that in order to be consistent with {@code Double#compareTo(Double)},
     * NaN is considered equal to itself and greater the positive infinity.
     * </p>
     * @param other other offset to compare the instance to
     * @return a negative integer, zero, or a positive integer if applying this offset
     * to reference date would result in a date being before, simultaneous, or after
     * the date obtained by applying the other offset to the same reference date.
     */
    public int compareTo(final SplitOffset other) {
        if (isFinite()) {
            if (other.isFinite()) {
                return seconds == other.seconds ?
                       Long.compare(attoSeconds, other.attoSeconds) :
                       Long.compare(seconds, other.seconds);
            } else {
                // if other is ±∞ or NaN, and NaN is considered larger than +∞
                return other.isNegativeInfinity() ? 1 : -1;
            }
        } else {
            // instance is ±∞ or NaN, and NaN is considered larger than +∞
            if (isNaN()) {
                // for consistency with Double.compareTo, NaN is considered equal to itself
                return other.isNaN() ? 0 : 1;
            } else if (other.isNaN()) {
                return -1;
            } else {
                // instance is ±∞, other is either finite or ±∞ but not NaN
                // at infinity, seconds are set to either Long.MIN_VALUE or Long.MAX_VALUE
                return Long.compare(seconds, other.seconds);
            }
        }
    }

}
