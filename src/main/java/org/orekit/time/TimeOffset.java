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

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

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
 * @since 13.0
 */
public class TimeOffset
    implements Comparable<TimeOffset>, Serializable {

    /** Split time representing 0. */
    public static final TimeOffset ZERO = new TimeOffset(0L, 0L);

    /** Split time representing 1 attosecond. */
    public static final TimeOffset ATTOSECOND = new TimeOffset(0L, 1L);

    /** Split time representing 1 femtosecond. */
    public static final TimeOffset FEMTOSECOND = new TimeOffset(0L, 1000L);

    /** Split time representing 1 picosecond. */
    public static final TimeOffset PICOSECOND = new TimeOffset(0L, 1000000L);

    /** Split time representing 1 nanosecond. */
    public static final TimeOffset NANOSECOND = new TimeOffset(0L, 1000000000L);

    /** Split time representing 1 microsecond. */
    public static final TimeOffset MICROSECOND = new TimeOffset(0L, 1000000000000L);

    /** Split time representing 1 millisecond. */
    public static final TimeOffset MILLISECOND = new TimeOffset(0L, 1000000000000000L);

    /** Split time representing 1 second. */
    public static final TimeOffset SECOND = new TimeOffset(1L, 0L);

    /** Split time representing 1 minute. */
    public static final TimeOffset MINUTE = new TimeOffset(60L, 0L);

    /** Split time representing 1 hour. */
    public static final TimeOffset HOUR = new TimeOffset(3600L, 0L);

    /** Split time representing 1 day. */
    public static final TimeOffset DAY = new TimeOffset(86400L, 0L);

    /** Split time representing 1 day that includes an additional leap second. */
    public static final TimeOffset DAY_WITH_POSITIVE_LEAP = new TimeOffset(86401L, 0L);

    // CHECKSTYLE: stop ConstantName
    /** Split time representing a NaN. */
    public static final TimeOffset NaN = new TimeOffset(Double.NaN);
    // CHECKSTYLE: resume ConstantName

    /** Split time representing negative infinity. */
    public static final TimeOffset NEGATIVE_INFINITY = new TimeOffset(Double.NEGATIVE_INFINITY);

    /** Split time representing positive infinity. */
    public static final TimeOffset POSITIVE_INFINITY = new TimeOffset(Double.POSITIVE_INFINITY);

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

    /** Factor to split long for multiplications.
     * <p>
     * It is important that SPLIT * SPLIT = ATTOS_IN_SECOND.
     * </p>
     */
    private static final long SPLIT = 1000000000L;

    /** Number of digits after separator for attoseconds. */
    private static final int DIGITS_ATTOS = 18;

    /** Multipliers for parsing partial strings. */
    // CHECKSTYLE: stop Indentation check
    private static final long[] MULTIPLIERS = new long[] {
                         1L,
                        10L,
                       100L,
                      1000L,
                     10000L,
                    100000L,
                   1000000L,
                  10000000L,
                 100000000L,
                1000000000L,
               10000000000L,
              100000000000L,
             1000000000000L,
            10000000000000L,
           100000000000000L,
          1000000000000000L,
         10000000000000000L,
        100000000000000000L,
       1000000000000000000L
    };
    // CHECKSTYLE: resume Indentation check

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
    public TimeOffset(final TimeOffset... times) {
        final RunningSum runningSum = new RunningSum();
        for (final TimeOffset time : times) {
            runningSum.add(time);
        }
        final TimeOffset sum = runningSum.normalize();
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
    public TimeOffset(final long seconds, final long attoSeconds) {
        long normalizedSeconds;
        long normalizedAttoSeconds;
        try {
            final long qAtto = attoSeconds / ATTOS_IN_SECOND;
            final long rAtto = attoSeconds - qAtto * ATTOS_IN_SECOND;
            if (rAtto < 0L) {
                normalizedSeconds     = seconds + qAtto - 1L;
                normalizedAttoSeconds = ATTOS_IN_SECOND + rAtto;
            } else {
                normalizedSeconds     = seconds + qAtto;
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
    public TimeOffset(final double time) {
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
     * Multiplicative constructor.
     * <p>
     * This constructors builds a split time corresponding to {@code factor} ⨉ {@code time}
     * </p>
     * @param factor multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param time base time
     */
    public TimeOffset(final long factor, final TimeOffset time) {
        this(factor < 0 ? time.multiply(-factor).negate() : time.multiply(factor));
    }

    /**
     * Linear combination constructor.
     * <p>
     * This constructors builds a split time corresponding to
     * {@code f1} ⨉ {@code t1} + {@code f2} ⨉ {@code t2}
     * </p>
     * @param f1 first multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t1 first base time
     * @param f2 second multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t2 second base time
     */
    public TimeOffset(final long f1, final TimeOffset t1,
                      final long f2, final TimeOffset t2) {
        this(new TimeOffset(f1, t1).add(new TimeOffset(f2, t2)));
    }

    /**
     * Linear combination constructor.
     * <p>
     * This constructors builds a split time corresponding to
     * {@code f1} ⨉ {@code t1} + {@code f2} ⨉ {@code t2} + {@code f3} ⨉ {@code t3}
     * </p>
     * @param f1 first multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t1 first base time
     * @param f2 second multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t2 second base time
     * @param f3 third multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t3 third base time
     */
    public TimeOffset(final long f1, final TimeOffset t1,
                      final long f2, final TimeOffset t2,
                      final long f3, final TimeOffset t3) {
        this(new TimeOffset(f1, t1).add(new TimeOffset(f2, t2)).add(new TimeOffset(f3, t3)));
    }

    /**
     * Linear combination constructor.
     * <p>
     * This constructors builds a split time corresponding to
     * {@code f1} ⨉ {@code t1} + {@code f2} ⨉ {@code t2} + {@code f3} ⨉ {@code t3} + {@code f4} ⨉ {@code t4}
     * </p>
     * @param f1 first multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t1 first base time
     * @param f2 second multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t2 second base time
     * @param f3 third multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t3 third base time
     * @param f4 fourth multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t4 fourth base time
     */
    public TimeOffset(final long f1, final TimeOffset t1,
                      final long f2, final TimeOffset t2,
                      final long f3, final TimeOffset t3,
                      final long f4, final TimeOffset t4) {
        this(new TimeOffset(f1, t1).
             add(new TimeOffset(f2, t2)).
             add(new TimeOffset(f3, t3)).
             add(new TimeOffset(f4, t4)));
    }

    /**
     * Linear combination constructor.
     * <p>
     * This constructors builds a split time corresponding to
     * {@code f1} ⨉ {@code t1} + {@code f2} ⨉ {@code t2} + {@code f3} ⨉ {@code t3} + {@code f4} ⨉ {@code t4} + {@code f5} ⨉ {@code t5}
     * </p>
     * @param f1 first multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t1 first base time
     * @param f2 second multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t2 second base time
     * @param f3 third multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t3 third base time
     * @param f4 fourth multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t4 fourth base time
     * @param f5 fifth multiplicative factor (negative values allowed here, contrary to {@link #multiply(long)})
     * @param t5 fifth base time
     */
    public TimeOffset(final long f1, final TimeOffset t1,
                      final long f2, final TimeOffset t2,
                      final long f3, final TimeOffset t3,
                      final long f4, final TimeOffset t4,
                      final long f5, final TimeOffset t5) {
        this(new TimeOffset(f1, t1).
             add(new TimeOffset(f2, t2)).
             add(new TimeOffset(f3, t3)).
             add(new TimeOffset(f4, t4)).
             add(new TimeOffset(f5, t5)));
    }

    /**
     * Build a time from a value defined in some time unit.
     *
     * @param time time
     * @param unit   time unit in which {@code time} is expressed
     */
    public TimeOffset(final long time, final TimeUnit unit) {
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
                final long r = (time - s * MILLIS_IN_SECOND) * MILLISECOND.attoSeconds;
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
                final long r = (time - s * MICROS_IN_SECOND) * MICROSECOND.attoSeconds;
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
                final long r = (time - s * NANOS_IN_SECOND) * NANOSECOND.attoSeconds;
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
                throw new OrekitException(OrekitMessages.UNKNOWN_UNIT, unit.name());
        }
    }

    /** Copy constructor, for internal use only.
     * @param time time to copy
     */
    private TimeOffset(final TimeOffset time) {
        seconds     = time.seconds;
        attoSeconds = time.attoSeconds;
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
     * @param t time to add
     * @return this+t
     */
    public TimeOffset add(final TimeOffset t) {
        final RunningSum runningSum = new RunningSum();
        runningSum.add(this);
        runningSum.add(t);
        return runningSum.normalize();
    }

    /** Build a time by subtracting one time from the instance.
     * @param t time to subtract
     * @return this-t
     */
    public TimeOffset subtract(final TimeOffset t) {
        if (attoSeconds < 0 || t.attoSeconds < 0) {
            // gather all special cases in one big check to avoid rare multiple tests
            if (isNaN() ||
                t.isNaN() ||
                isPositiveInfinity() && t.isPositiveInfinity() ||
                isNegativeInfinity() && t.isNegativeInfinity()) {
                return NaN;
            } else if (isInfinite()) {
                // t is either a finite time or the infinity opposite to this
                return this;
            } else {
                // this is either a finite time or the infinity opposite to t
                return t.isPositiveInfinity() ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
            }
        } else {
            // regular subtraction between two finite times
            return new TimeOffset(seconds - t.seconds, attoSeconds - t.attoSeconds);
        }
    }

    /** Multiply the instance by a positive or zero constant.
     * @param p multiplication factor (must be positive)
     * @return this ⨉ p
     */
    public TimeOffset multiply(final long p) {
        if (p < 0) {
            throw new OrekitException(OrekitMessages.NOT_POSITIVE, p);
        }
        if (isFinite()) {
            final TimeOffset abs   = seconds < 0 ? negate() : this;
            final long pHigh   = p / SPLIT;
            final long pLow    = p - pHigh * SPLIT;
            final long sHigh   = abs.seconds / SPLIT;
            final long sLow    = abs.seconds - sHigh * SPLIT;
            final long aHigh   = abs.attoSeconds / SPLIT;
            final long aLow    = abs.attoSeconds - aHigh * SPLIT;
            final long ps1     = pHigh * sLow + pLow * sHigh;
            final long ps0     = pLow * sLow;
            final long pa2     = pHigh * aHigh;
            final long pa1     = pHigh * aLow + pLow * aHigh;
            final long pa1High = pa1 / SPLIT;
            final long pa1Low  = pa1 - pa1High * SPLIT;
            final long pa0     = pLow * aLow;

            // check for overflow
            if (pHigh * sHigh != 0 || ps1 / SPLIT != 0) {
                throw new OrekitException(LocalizedCoreFormats.OVERFLOW_IN_MULTIPLICATION, abs.seconds, p);
            }

            // here we use the fact that SPLIT * SPLIT = ATTOS_IN_SECOND
            final TimeOffset mul = new TimeOffset(SPLIT * ps1 + ps0 + pa2 + pa1High, SPLIT * pa1Low + pa0);
            return seconds < 0 ? mul.negate() : mul;
        } else {
            // already NaN, +∞ or -∞, unchanged except 0 ⨉ ±∞ = NaN
            return p == 0 ? TimeOffset.NaN : this;
        }
    }

    /** Divide the instance by a positive constant.
     * @param q division factor (must be strictly positive)
     * @return this ÷ q
     */
    public TimeOffset divide(final int q) {
        if (q <= 0) {
            throw new OrekitException(OrekitMessages.NOT_STRICTLY_POSITIVE, q);
        }
        if (isFinite()) {
            final long      sSec  = seconds         / q;
            final long      rSec  = seconds         - sSec * q;
            final long      sK    = ATTOS_IN_SECOND / q;
            final long      rK    = ATTOS_IN_SECOND - sK * q;
            final TimeOffset tsSec = new TimeOffset(0L, sSec);
            final TimeOffset trSec = new TimeOffset(0L, rSec);
            return new TimeOffset(tsSec.multiply(sK).multiply(q),
                                  tsSec.multiply(rK),
                                  trSec.multiply(sK),
                                  // here, we use the fact q is a positive int (not a long!)
                                  // hence rSec * rK < q² does not overflow
                                  new TimeOffset(0L, (attoSeconds + rSec * rK) / q));
        } else {
            // already NaN, +∞ or -∞, unchanged as q > 0
            return this;
        }
    }

    /** Negate the instance.
     * @return new instance corresponding to opposite time
     */
    public TimeOffset negate() {
        // handle special cases
        if (attoSeconds < 0) {
            // gather all special cases in one big check to avoid rare multiple tests
            return isNaN() ? this : (seconds < 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY);
        } else {
            // the negative number of attoseconds will be normalized back to positive by the constructor
            return new TimeOffset(-seconds, -attoSeconds);
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
                throw new OrekitException(OrekitMessages.UNKNOWN_UNIT, unit.name());
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

    /** Parse a string to produce an accurate split time.
     * <p>
     * This method is more accurate than parsing the string as a double and then
     * calling {@link TimeOffset#TimeOffset(double)} because it reads the sub-second
     * part in decimal, hence avoiding problems like for example 0.1 not being an
     * exact IEEE754 number.
     * </p>
     * @param s string to parse
     * @return parsed split time
     */
    public static TimeOffset parse(final String s) {

        // decompose the string
        // we use neither Long.parseLong nor Integer.parseInt because we want to avoid
        // performing several loops over the characters as we need to keep track of
        // delimiters decimal point and exponent marker positions
        final int length = s.length();
        long significandSign = 1L;
        int  exponentSign    = 1;
        int  separatorIndex  = length;
        int  exponentIndex   = length;
        long beforeSeparator = 0L;
        long afterSeparator  = 0L;
        int  exponent        = 0;
        int  digitsBefore    = 0;
        int  digitsAfter     = 0;
        int  digitsExponent  = 0;
        int index = 0;
        while (index < length) {

            // current character
            final char c = s.charAt(index);

            if (Character.isDigit(c)) {
                if (separatorIndex == length) {
                    // we are parsing the part before separator
                    ++digitsBefore;
                    beforeSeparator = beforeSeparator * 10 + c - '0';
                    if (digitsBefore > 19 || beforeSeparator < 0) {
                        // overflow occurred
                        break;
                    }
                } else if (exponentIndex == length) {
                    // we are parsing the part between separator and exponent
                    if (digitsAfter < DIGITS_ATTOS) {
                        // we never overflow here, we just ignore extra digits
                        afterSeparator = afterSeparator * 10 + c - '0';
                        ++digitsAfter;
                    }
                } else {
                    // we are parsing the exponent
                    ++digitsExponent;
                    exponent = exponent * 10 + c - '0';
                    if (digitsExponent > 10 || exponent < 0) {
                        // overflow occurred
                        break;
                    }
                }
            } else if (c == '.' && separatorIndex == length) {
                separatorIndex = index;
            } else if ((c == 'e' || c == 'E') && exponentIndex == length) {
                if (separatorIndex == length) {
                    separatorIndex = index;
                }
                exponentIndex = index;
            } else if (c == '-') {
                if (index == 0) {
                    significandSign = -1L;
                } else if (index == exponentIndex + 1) {
                    exponentSign = -1;
                } else {
                    break;
                }
            } else if (c == '+') {
                if (index == 0) {
                    significandSign = 1L;
                } else if (index == exponentIndex + 1) {
                    exponentSign = 1;
                } else {
                    break;
                }
            } else {
                break;
            }

            ++index;

        }

        if (length == 0 || index < length) {
            // decomposition failed, either it is a special case or an unparsable string
            if (s.equals("-∞")) {
                return TimeOffset.NEGATIVE_INFINITY;
            } else if (s.equals("+∞")) {
                return TimeOffset.POSITIVE_INFINITY;
            } else if (s.equalsIgnoreCase("NaN")) {
                return TimeOffset.NaN;
            } else {
                throw new OrekitException(OrekitMessages.CANNOT_PARSE_DATA, s);
            }
        }

        // decomposition was successful, build the split time
        long seconds;
        long attoseconds;
        if (exponentSign < 0) {
            // the part before separator must be split into seconds and attoseconds
            if (exponent >= MULTIPLIERS.length) {
                seconds = 0L;
                if (exponent - DIGITS_ATTOS >= MULTIPLIERS.length) {
                    // underflow
                    attoseconds = 0L;
                } else {
                    attoseconds = beforeSeparator / MULTIPLIERS[exponent - DIGITS_ATTOS];
                }
            } else {
                final long secondsMultiplier    = MULTIPLIERS[exponent];
                final long attoBeforeMultiplier = MULTIPLIERS[DIGITS_ATTOS - exponent];
                seconds     = beforeSeparator / secondsMultiplier;
                attoseconds = (beforeSeparator - seconds * secondsMultiplier) * attoBeforeMultiplier;
                while (digitsAfter + exponent > DIGITS_ATTOS) {
                    // drop least significant digits below one attosecond
                    afterSeparator /= 10;
                    digitsAfter--;
                }
                final long attoAfterMultiplier = MULTIPLIERS[DIGITS_ATTOS - exponent - digitsAfter];
                attoseconds += afterSeparator * attoAfterMultiplier;
            }
        } else {
            // the part after separator must be split into seconds and attoseconds
            if (exponent >= MULTIPLIERS.length) {
                if (beforeSeparator == 0L && afterSeparator == 0L) {
                    return TimeOffset.ZERO;
                } else if (significandSign < 0) {
                    return TimeOffset.NEGATIVE_INFINITY;
                } else {
                    return TimeOffset.POSITIVE_INFINITY;
                }
            } else {
                final long secondsMultiplier = MULTIPLIERS[exponent];
                seconds = beforeSeparator * secondsMultiplier;
                if (exponent > digitsAfter) {
                    seconds += afterSeparator * MULTIPLIERS[exponent - digitsAfter];
                    attoseconds = 0L;
                } else {
                    final long q = afterSeparator / MULTIPLIERS[digitsAfter - exponent];
                    seconds    += q;
                    attoseconds = (afterSeparator - q * MULTIPLIERS[digitsAfter - exponent]) *
                                  MULTIPLIERS[DIGITS_ATTOS - digitsAfter + exponent];
                }
            }
        }

        return new TimeOffset(significandSign * seconds, significandSign * attoseconds);

    }

    /** Compare the instance with another one.
     * <p>
     * Not that in order to be consistent with {@code Double#compareTo(Double)},
     * NaN is considered equal to itself and greater than positive infinity.
     * </p>
     * @param other other time to compare the instance to
     * @return a negative integer, zero, or a positive integer if applying this time
     * to reference date would result in a date being before, simultaneous, or after
     * the date obtained by applying the other time to the same reference date.
     */
    public int compareTo(final TimeOffset other) {
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
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        final TimeOffset timeOffset = (TimeOffset) o;
        return seconds == timeOffset.seconds && attoSeconds == timeOffset.attoSeconds;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Long.hashCode(seconds) ^ Long.hashCode(attoSeconds);
    }

    /** Local class for summing several instances. */
    private static class RunningSum {

        /** Number of terms that can be added before normalization is needed. */
        private static final int COUNT_DOWN_MAX = 9;

        /** Seconds part. */
        private long seconds;

        /** AttoSeconds part. */
        private long attoSeconds;

        /** Indicator for NaN presence. */
        private boolean addedNaN;

        /** Indicator for +∞ presence. */
        private boolean addedPositiveInfinity;

        /** Indicator for -∞ presence. */
        private boolean addedNegativeInfinity;

        /** Countdown for checking carry. */
        private int countDown;

        /** Simple constructor.
         */
        RunningSum() {
            countDown = COUNT_DOWN_MAX;
        }

        /** Add one term.
         * @param term term to add
         */
        public void add(final TimeOffset term) {
            if (term.isFinite()) {
                // regular addition
                seconds     += term.seconds;
                attoSeconds += term.attoSeconds;
                if (--countDown == 0) {
                    // we have added several terms, we should normalize
                    // the fields before attoseconds overflow (it may overflow after 9 additions)
                    normalize();
                }
            } else if (term.isNegativeInfinity()) {
                addedNegativeInfinity = true;
            } else if (term.isPositiveInfinity()) {
                addedPositiveInfinity = true;
            } else {
                addedNaN = true;
            }
        }

        /** Normalize current running sum.
         * @return normalized value
         */
        public TimeOffset normalize() {

            // after normalization, we will have the equivalent of one entry processed
            countDown = COUNT_DOWN_MAX - 1;

            if (addedNaN || addedNegativeInfinity && addedPositiveInfinity) {
                // we have built a NaN
                seconds     = NaN.seconds;
                attoSeconds = NaN.attoSeconds;
                return NaN;
            } else if (addedNegativeInfinity) {
                // we have built -∞
                seconds     = NEGATIVE_INFINITY.seconds;
                attoSeconds = NEGATIVE_INFINITY.attoSeconds;
                return NEGATIVE_INFINITY;
            } else if (addedPositiveInfinity) {
                // we have built +∞
                seconds     = POSITIVE_INFINITY.seconds;
                attoSeconds = POSITIVE_INFINITY.attoSeconds;
                return POSITIVE_INFINITY;
            } else {
                // this is a regular time
                final TimeOffset regular = new TimeOffset(seconds, attoSeconds);
                seconds     = regular.seconds;
                attoSeconds = regular.attoSeconds;
                return regular;
            }
        }

    }

}
