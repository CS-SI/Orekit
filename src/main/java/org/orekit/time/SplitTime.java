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

/** This class represents a time range split into seconds and attoseconds.
 * <p>
 * Instances of this class may either be interpreted as offsets from a reference
 * date, or they may be interpreted as durations. Negative values represent
 * dates earlier than the reference date in the first interpretation, and
 * negative durations in the second interpretation.
 * </p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @see FieldAbsoluteDate
 * @since 13.1
 */
public class SplitTime implements Comparable<SplitTime>, Serializable {

    /** Split time representing 0. */
    public static final SplitTime ZERO = new SplitTime(0L, 0L);

    /** Split time representing 1 attosecond. */
    public static final SplitTime ATTOSECOND = new SplitTime(0L, 1L);

    /** Split time representing 1 femtosecond. */
    public static final SplitTime FEMTOSECOND = new SplitTime(0L, 1000L);

    /** Split time representing 1 picosecond. */
    public static final SplitTime PICOSECOND = new SplitTime(0L, 1000000L);

    /** Split time representing 1 nanosecond. */
    public static final SplitTime NANOSECOND = new SplitTime(0L, 1000000000L);

    /** Split time representing 1 microsecond. */
    public static final SplitTime MICROSECOND = new SplitTime(0L, 1000000000000L);

    /** Split time representing 1 millisecond. */
    public static final SplitTime MILLISECOND = new SplitTime(0L, 1000000000000000L);

    /** Split time representing 1 second. */
    public static final SplitTime SECOND = new SplitTime(1L, 0L);

    /** Split time representing 1 minute. */
    public static final SplitTime MINUTE = new SplitTime(60L, 0L);

    /** Split time representing 1 hour. */
    public static final SplitTime HOUR = new SplitTime(3600L, 0L);

    /** Split time representing 1 day. */
    public static final SplitTime DAY = new SplitTime(86400L, 0L);

    /** Split time representing 1 day that includes an additional leap second. */
    public static final SplitTime DAY_WITH_POSITIVE_LEAP = new SplitTime(86401L, 0L);

    // CHECKSTYLE: stop ConstantName
    /** Split time representing a NaN. */
    public static final SplitTime NaN = new SplitTime(Double.NaN);
    // CHECKSTYLE: resume ConstantName

    /** Split time representing negative infinity. */
    public static final SplitTime NEGATIVE_INFINITY = new SplitTime(Double.NEGATIVE_INFINITY);

    /** Split time representing positive infinity. */
    public static final SplitTime POSITIVE_INFINITY = new SplitTime(Double.POSITIVE_INFINITY);

    /** Indicator for NaN time (bits pattern arbitrarily selected to avoid hashcode collisions). */
    private static final long NAN_INDICATOR      = -0XFFL;

    /** Indicator for positive infinite time(bits pattern arbitrarily selected to avoid hashcode collisions). */
    private static final long POSITIVE_INFINITY_INDICATOR = -0XFF00L;

    /** Indicator for negative infinite time(bits pattern arbitrarily selected to avoid hashcode collisions). */
    private static final long NEGATIVE_INFINITY_INDICATOR = -0XFF0000L;

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

    /** Seconds part. */
    private final long seconds;

    /** AttoSeconds part. */
    private final long attoSeconds;

    /**
     * Build a time by adding several times.
     * @param times times to add
     */
    public SplitTime(final SplitTime...times) {
        SplitTime sum = SplitTime.ZERO;
        for (final SplitTime time : times) {
            sum = SplitTime.add(sum, time);
        }
        this.seconds     = sum.getSeconds();
        this.attoSeconds = sum.getAttoSeconds();
    }

    /**
     * Build a time from its components.
     * <p>
     * The components will be normalized so that {@link #getAttoSeconds()}
     * returns a value between {@code 0L} and {1000000000000000000L}
     * </p>
     * @param seconds seconds part
     * @param attoSeconds attoseconds part
     */
    public SplitTime(final long seconds, final long attoSeconds) {
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
            if (seconds < 0L) {
                normalizedSeconds     = Long.MIN_VALUE;
                normalizedAttoSeconds = NEGATIVE_INFINITY_INDICATOR;
            } else {
                normalizedSeconds     = Long.MAX_VALUE;
                normalizedAttoSeconds = POSITIVE_INFINITY_INDICATOR;
            }
        }

        // store normalized values
        this.seconds     = normalizedSeconds;
        this.attoSeconds = normalizedAttoSeconds;

    }

    /**
     * Build a time from a value in seconds.
     *
     * @param time time
     */
    public SplitTime(final double time) {
        if (Double.isNaN(time)) {
            seconds     = 0L;
            attoSeconds = NAN_INDICATOR;
        } else if (time < Long.MIN_VALUE || time > Long.MAX_VALUE) {
            if (time < 0L) {
                seconds     = Long.MIN_VALUE;
                attoSeconds = NEGATIVE_INFINITY_INDICATOR;
            } else {
                seconds     = Long.MAX_VALUE;
                attoSeconds = POSITIVE_INFINITY_INDICATOR;
            }
        } else {
            final double tiSeconds  = FastMath.rint(time);
            final double subSeconds = time - tiSeconds;
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
     * Build a time from a value defined in some time unit.
     *
     * @param time time
     * @param unit   time unit in which {@code time} is expressed
     */
    public SplitTime(final long time, final TimeUnit unit) {
        switch (unit) {
            case DAYS: {
                final long limit = (Long.MAX_VALUE - DAY.seconds / 2) / DAY.seconds;
                if (time < -limit) {
                    seconds     = Long.MIN_VALUE;
                    attoSeconds = NEGATIVE_INFINITY_INDICATOR;
                } else if (time > limit) {
                    seconds     = Long.MAX_VALUE;
                    attoSeconds = POSITIVE_INFINITY_INDICATOR;
                } else {
                    seconds = time * DAY.seconds;
                    attoSeconds = 0L;
                }
                break;
            }
            case HOURS: {
                final long limit = (Long.MAX_VALUE - HOUR.seconds / 2) / HOUR.seconds;
                if (time < -limit) {
                    seconds     = Long.MIN_VALUE;
                    attoSeconds = NEGATIVE_INFINITY_INDICATOR;
                } else if (time > limit) {
                    seconds     = Long.MAX_VALUE;
                    attoSeconds = POSITIVE_INFINITY_INDICATOR;
                } else {
                    seconds     = time * HOUR.seconds;
                    attoSeconds = 0L;
                }
                break;
            }
            case MINUTES: {
                final long limit = (Long.MAX_VALUE - MINUTE.seconds / 2) / MINUTE.seconds;
                if (time < -limit) {
                    seconds     = Long.MIN_VALUE;
                    attoSeconds = NEGATIVE_INFINITY_INDICATOR;
                } else if (time > limit) {
                    seconds     = Long.MAX_VALUE;
                    attoSeconds = POSITIVE_INFINITY_INDICATOR;
                } else {
                    seconds     = time * MINUTE.seconds;
                    attoSeconds = 0L;
                }
                break;
            }
            case SECONDS:
                seconds     = time;
                attoSeconds = 0L;
                break;
            case MILLISECONDS: {
                final long s = time / MILLIS_IN_SECOND;
                final long r = (time % MILLIS_IN_SECOND) * MILLISECOND.attoSeconds;
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
                final long s = time / MICROS_IN_SECOND;
                final long r = (time % MICROS_IN_SECOND) * MICROSECOND.attoSeconds;
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
                final long s = time / NANOS_IN_SECOND;
                final long r = (time % NANOS_IN_SECOND) * NANOSECOND.attoSeconds;
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

    /** check if the time is zero.
     * @return true if the time is zero
     */
    public boolean isZero() {
        return seconds == 0L && attoSeconds == 0L;
    }

    /** Check if time is finite (i.e. neither {@link #isNaN() NaN} nor {@link #isInfinite() infinite)}.
     * @return true if time is finite
     * @see #isNaN()
     * @see #isInfinite()
     * @see #isNegativeInfinity()
     * @see #isPositiveInfinity()
     */
    public boolean isFinite() {
        return attoSeconds >= 0L;
    }

    /** Check if time is NaN.
     * @return true if time is NaN
     * @see #isFinite()
     * @see #isInfinite()
     * @see #isNegativeInfinity()
     * @see #isPositiveInfinity()
     */
    public boolean isNaN() {
        return attoSeconds == NAN_INDICATOR;
    }

    /** Check if time is infinity.
     * @return true if time is infinity
     * @see #isFinite()
     * @see #isNaN()
     * @see #isNegativeInfinity()
     * @see #isPositiveInfinity()
     */
    public boolean isInfinite() {
        return isPositiveInfinity() || isNegativeInfinity();
    }

    /** Check if time is positive infinity.
     * @return true if time is positive infinity
     * @see #isFinite()
     * @see #isNaN()
     * @see #isInfinite()
     * @see #isNegativeInfinity()
     */
    public boolean isPositiveInfinity() {
        return attoSeconds == POSITIVE_INFINITY_INDICATOR;
    }

    /** Check if time is negative infinity.
     * @return true if time is negative infinity
     * @see #isFinite()
     * @see #isNaN()
     * @see #isInfinite()
     * @see #isPositiveInfinity()
     */
    public boolean isNegativeInfinity() {
        return attoSeconds == NEGATIVE_INFINITY_INDICATOR;
    }

    /** Build a time by adding two times.
     * @param t1 first time
     * @param t2 second time
     * @return t1+t2
     */
    public static SplitTime add(final SplitTime t1, final SplitTime t2) {
        if (t1.attoSeconds < 0 || t2.attoSeconds < 0) {
            // gather all special cases in one big check to avoid rare multiple tests
            if (t1.isNaN() ||
                t2.isNaN() ||
                t1.isPositiveInfinity() && t2.isNegativeInfinity() ||
                t1.isNegativeInfinity() && t2.isPositiveInfinity()) {
                return NaN;
            } else if (t1.isInfinite()) {
                // t2 is either a finite time or the same infinity as t1
                return t1;
            } else {
                // t1 is either a finite time or the same infinity as t2
                return t2;
            }
        } else {
            // regular addition between two finite times
            return new SplitTime(t1.seconds + t2.seconds, t1.attoSeconds + t2.attoSeconds);
        }
    }

    /** Build a time by subtracting one time from another one.
     * @param t1 first time
     * @param t2 second time
     * @return t1-t2
     */
    public static SplitTime subtract(final SplitTime t1, final SplitTime t2) {
        if (t1.attoSeconds < 0 || t2.attoSeconds < 0) {
            // gather all special cases in one big check to avoid rare multiple tests
            if (t1.isNaN() ||
                t2.isNaN() ||
                t1.isPositiveInfinity() && t2.isPositiveInfinity() ||
                t1.isNegativeInfinity() && t2.isNegativeInfinity()) {
                return NaN;
            } else if (t1.isInfinite()) {
                // t2 is either a finite time or the infinity opposite to t1
                return t1;
            } else {
                // t1 is either a finite time or the infinity opposite to t2
                return t2.isPositiveInfinity() ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
            }
        } else {
            // regular subtraction between two finite times
            return new SplitTime(t1.seconds - t2.seconds, t1.attoSeconds - t2.attoSeconds);
        }
    }

    /** Negate the instance.
     * @return new instance corresponding to opposite time
     */
    public SplitTime negate() {
        // handle special cases
        if (attoSeconds < 0) {
            // gather all special cases in one big check to avoid rare multiple tests
            return isNaN() ? this : (seconds < 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY);
        } else {
            // the negative number of attoseconds will be normalized back to positive by the constructor
            return new SplitTime(-seconds, -attoSeconds);
        }
    }

    /** Get the time in some unit.
     * @param unit time unit
     * @return time in this unit, rounded to the closest long,
     * returns arbitrarily {@link Long#MAX_VALUE} for {@link #isNaN() NaN times}
     */
    public long getRoundedTime(final TimeUnit unit) {

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

    /** Get the normalized seconds part of the time.
     * @return normalized seconds part of the time (may be negative)
     */
    public long getSeconds() {
        return seconds;
    }

    /** Get the normalized attoseconds part of the time.
     * <p>
     * The normalized attoseconds is always between {@code 0L} and
     * {@code 1000000000000000000L} for <em>finite</em> ranges. Note that it
     * may reach {@code 1000000000000000000L} if for example the time is less
     * than 1 attosecond <em>before</em> a whole second. It is negative
     * for {@link #isNaN() NaN} or {@link #isInfinite() infinite} times.
     * </p>
     * @return normalized attoseconds part of the time
     */
    public long getAttoSeconds() {
        return attoSeconds;
    }

    /** Get the time collapsed into a single double.
     * <p>
     * Beware that lots of accuracy is lost when combining {@link #getSeconds()} and {@link #getAttoSeconds()}
     * into a single double.
     * </p>
     * @return time as a single double
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
     * @param other other time to compare the instance to
     * @return a negative integer, zero, or a positive integer if applying this time
     * to reference date would result in a date being before, simultaneous, or after
     * the date obtained by applying the other time to the same reference date.
     */
    public int compareTo(final SplitTime other) {
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

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SplitTime)) {
            return false;
        }
        final SplitTime splitTime = (SplitTime) o;
        return seconds == splitTime.seconds && attoSeconds == splitTime.attoSeconds;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Long.hashCode(seconds) ^ Long.hashCode(attoSeconds);
    }

}
