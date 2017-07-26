/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.time;

import java.util.Date;
import java.util.TimeZone;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

/** This class represents a specific instant in time.

 * <p>Instances of this class are considered to be absolute in the sense
 * that each one represent the occurrence of some event and can be compared
 * to other instances or located in <em>any</em> {@link TimeScale time scale}. In
 * other words the different locations of an event with respect to two different
 * time scales (say {@link TAIScale TAI} and {@link UTCScale UTC} for example) are
 * simply different perspective related to a single object. Only one
 * <code>FieldAbsoluteDate<T></code> instance is needed, both representations being available
 * from this single instance by specifying the time scales as parameter when calling
 * the ad-hoc methods.</p>
 *
 * <p>Since an instance is not bound to a specific time-scale, all methods related
 * to the location of the date within some time scale require to provide the time
 * scale as an argument. It is therefore possible to define a date in one time scale
 * and to use it in another one. An example of such use is to read a date from a file
 * in UTC and write it in another file in TAI. This can be done as follows:</p>
 * <pre>
 *   DateTimeComponents utcComponents = readNextDate();
 *   FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(utcComponents, TimeScalesFactory.getUTC());
 *   writeNextDate(date.getComponents(TimeScalesFactory.getTAI()));
 * </pre>
 *
 * <p>Two complementary views are available:</p>
 * <ul>
 *   <li><p>location view (mainly for input/output or conversions)</p>
 *   <p>locations represent the coordinate of one event with respect to a
 *   {@link TimeScale time scale}. The related methods are {@link
 *   #FieldAbsoluteDate(Field, DateComponents, TimeComponents, TimeScale)}, {@link
 *   #FieldAbsoluteDate(Field, int, int, int, int, int, double, TimeScale)}, {@link
 *   #FieldAbsoluteDate(Field, int, int, int, TimeScale)}, {@link #FieldAbsoluteDate(Field,
 *   Date, TimeScale)}, {@link #createGPSDate(int, RealFieldElement)}, {@link
 *   #parseCCSDSCalendarSegmentedTimeCode(byte, byte[])}, toString(){@link
 *   #toDate(TimeScale)}, {@link #toString(TimeScale) toString(timeScale)},
 *   {@link #toString()}, and {@link #timeScalesOffset}.</p>
 *   </li>
 *   <li><p>offset view (mainly for physical computation)</p>
 *   <p>offsets represent either the flow of time between two events
 *   (two instances of the class) or durations. They are counted in seconds,
 *   are continuous and could be measured using only a virtually perfect stopwatch.
 *   The related methods are {@link #FieldAbsoluteDate(FieldAbsoluteDate, double)},
 *   {@link #parseCCSDSUnsegmentedTimeCode(Field, byte, byte, byte[], FieldAbsoluteDate)},
 *   {@link #parseCCSDSDaySegmentedTimeCode(Field, byte, byte[], DateComponents)},
 *   {@link #durationFrom(FieldAbsoluteDate)}, {@link #compareTo(FieldAbsoluteDate)}, {@link #equals(Object)}
 *   and {@link #hashCode()}.</p>
 *   </li>
 * </ul>
 * <p>
 * A few reference epochs which are commonly used in space systems have been defined. These
 * epochs can be used as the basis for offset computation. The supported epochs are:
 * {@link #getJulianEpoch(Field)}, {@link #getModifiedJulianEpoch(Field)}, {@link #getFiftiesEpoch(Field)},
 * {@link #getCCSDSEpoch(Field)}, {@link #getGalileoEpoch(Field)}, {@link #getGPSEpoch(Field)},
 * {@link #getJ2000Epoch(Field)}, {@link #getJavaEpoch(Field)}. There are also two factory methods
 * {@link #createJulianEpoch(RealFieldElement)} and {@link #createBesselianEpoch(RealFieldElement)}
 * that can be used to compute other reference epochs like J1900.0 or B1950.0.
 * In addition to these reference epochs, two other constants are defined for convenience:
 * {@link #getPastInfinity(Field)} and {@link #getFutureInfinity(Field)}, which can be used either
 * as dummy dates when a date is not yet initialized, or for initialization of loops searching for
 * a min or max date.
 * </p>
 * <p>
 * Instances of the <code>FieldAbsoluteDate<T></code> class are guaranteed to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @see TimeScale
 * @see TimeStamped
 * @see ChronologicalComparator
 */
public class FieldAbsoluteDate<T extends RealFieldElement<T>>
    implements FieldTimeStamped<T>, TimeShiftable<FieldAbsoluteDate<T>>, Comparable<FieldAbsoluteDate<T>> {

    /** Reference epoch in seconds from 2000-01-01T12:00:00 TAI.
     * <p>Beware, it is not {@link #getJ2000Epoch(Field)} since it is in TAI and not in TT.</p> */
    private final long epoch;

    /** Offset from the reference epoch in seconds. */
    private final  T offset;

    /** Field used by default.*/
    private Field<T> field;

    /** Build an instance from an AbsoluteDate.
     * @param field used by default
     * @param date AbsoluteDate to instantiate as a FieldAbsoluteDate
     */
    public FieldAbsoluteDate(final Field<T> field, final AbsoluteDate date) {
        this.field  = field;
        this.epoch  = date.getEpoch();
        this.offset = field.getZero().add(date.getOffset());
    }

    /** Create an instance with a default value ({@link #getJ2000Epoch(Field)}).
     * @param field field used by default
     */
    public FieldAbsoluteDate(final Field<T> field) {
        final FieldAbsoluteDate<T> j2000 = getJ2000Epoch(field);
        this.field  = j2000.field;
        this.epoch  = j2000.epoch;
        this.offset = j2000.offset;
    }

    /** Build an instance from an elapsed duration since to another instant.
     * <p>It is important to note that the elapsed duration is <em>not</em>
     * the difference between two readings on a time scale. As an example,
     * the duration between the two instants leading to the readings
     * 2005-12-31T23:59:59 and 2006-01-01T00:00:00 in the {@link UTCScale UTC}
     * time scale is <em>not</em> 1 second, but a stop watch would have measured
     * an elapsed duration of 2 seconds between these two instances because a leap
     * second was introduced at the end of 2005 in this time scale.</p>
     * <p>This constructor is the reverse of the {@link #durationFrom(FieldAbsoluteDate)}
     * method.</p>
     * @param since start instant of the measured duration
     * @param elapsedDuration physically elapsed duration from the <code>since</code>
     * instant, as measured in a regular time scale
     * @see #durationFrom(FieldAbsoluteDate)
     */
    public FieldAbsoluteDate(final FieldAbsoluteDate<T> since, final T elapsedDuration) {
        this.field = since.field;
        final T sum = since.offset.add(elapsedDuration);
        if (Double.isInfinite(sum.getReal())) {
            offset = sum;
            epoch  = (sum.getReal() < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
        } else {
            // compute sum exactly, using Møller-Knuth TwoSum algorithm without branching
            // the following statements must NOT be simplified, they rely on floating point
            // arithmetic properties (rounding and representable numbers)
            // at the end, the EXACT result of addition since.offset + elapsedDuration
            // is sum + residual, where sum is the closest representable number to the exact
            // result and residual is the missing part that does not fit in the first number
            final double oPrime   = sum.getReal() - elapsedDuration.getReal();
            final double dPrime   = sum.getReal() - oPrime;
            final double deltaO   = since.offset.getReal() - oPrime;
            final double deltaD   = elapsedDuration.getReal() - dPrime;
            final double residual = deltaO + deltaD;
            final long   dl       = (long) FastMath.floor(sum.getReal());
            offset = sum.subtract(dl).add(residual);
            epoch  = since.epoch + dl;
        }
    }

    /** Build an instance from a location (parsed from a string) in a {@link TimeScale time scale}.
     * <p>
     * The supported formats for location are mainly the ones defined in ISO-8601 standard,
     * the exact subset is explained in {@link DateTimeComponents#parseDateTime(String)},
     * {@link DateComponents#parseDate(String)} and {@link TimeComponents#parseTime(String)}.
     * </p>
     * <p>
     * As CCSDS ASCII calendar segmented time code is a trimmed down version of ISO-8601,
     * it is also supported by this constructor.
     * </p>
     * @param field field utilized by default
     * @param location location in the time scale, must be in a supported format
     * @param timeScale time scale
     * @exception IllegalArgumentException if location string is not in a supported format
     */
    public FieldAbsoluteDate(final Field<T> field, final String location, final TimeScale timeScale) {
        this(field, DateTimeComponents.parseDateTime(location), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized by default
     * @param location location in the time scale
     * @param timeScale time scale
     */
    public FieldAbsoluteDate(final Field<T> field, final DateTimeComponents location, final TimeScale timeScale) {
        this(field, location.getDate(), location.getTime(), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized by default
     * @param date date location in the time scale
     * @param time time location in the time scale
     * @param timeScale time scale
     */
    public FieldAbsoluteDate(final Field<T> field, final DateComponents date, final TimeComponents time,
                             final TimeScale timeScale) {
        final double seconds  = time.getSecond();
        final double tsOffset = timeScale.offsetToTAI(date, time);

        // compute sum exactly, using Møller-Knuth TwoSum algorithm without branching
        // the following statements must NOT be simplified, they rely on floating point
        // arithmetic properties (rounding and representable numbers)
        // at the end, the EXACT result of addition seconds + tsOffset
        // is sum + residual, where sum is the closest representable number to the exact
        // result and residual is the missing part that does not fit in the first number
        final double sum      = seconds + tsOffset;
        final double sPrime   = sum - tsOffset;
        final double tPrime   = sum - sPrime;
        final double deltaS   = seconds  - sPrime;
        final double deltaT   = tsOffset - tPrime;
        final double residual = deltaS   + deltaT;
        final long   dl       = (long) FastMath.floor(sum);

        offset = field.getZero().add((sum - dl) + residual);

        epoch  = 60l * ((date.getJ2000Day() * 24l + time.getHour()) * 60l +
                        time.getMinute() - time.getMinutesFromUTC() - 720l) + dl;
        this.field = field;

    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized by default
     * @param year year number (may be 0 or negative for BC years)
     * @param month month number from 1 to 12
     * @param day day number from 1 to 31
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public FieldAbsoluteDate(final Field<T> field, final int year, final int month, final int day,
                             final int hour, final int minute, final double second,
                             final TimeScale timeScale) throws IllegalArgumentException {
        this(field, new DateComponents(year, month, day), new TimeComponents(hour, minute, second), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized by default
     * @param year year number (may be 0 or negative for BC years)
     * @param month month enumerate
     * @param day day number from 1 to 31
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public FieldAbsoluteDate(final Field<T> field, final int year, final Month month, final int day,
                             final int hour, final int minute, final double second,
                             final TimeScale timeScale) throws IllegalArgumentException {
        this(field, new DateComponents(year, month, day), new TimeComponents(hour, minute, second), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * <p>The hour is set to 00:00:00.000.</p>
     * @param field field utilized by default
     * @param date date location in the time scale
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public FieldAbsoluteDate(final Field<T> field, final DateComponents date, final TimeScale timeScale)
        throws IllegalArgumentException {
        this(field, date, TimeComponents.H00, timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * <p>The hour is set to 00:00:00.000.</p>
     * @param field field utilized by default
     * @param year year number (may be 0 or negative for BC years)
     * @param month month number from 1 to 12
     * @param day day number from 1 to 31
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public FieldAbsoluteDate(final Field<T> field, final int year, final int month, final int day,
                        final TimeScale timeScale) throws IllegalArgumentException {
        this(field, new DateComponents(year, month, day), TimeComponents.H00, timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * <p>The hour is set to 00:00:00.000.</p>
     * @param field field utilized by default
     * @param year year number (may be 0 or negative for BC years)
     * @param month month enumerate
     * @param day day number from 1 to 31
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public FieldAbsoluteDate(final Field<T> field, final int year, final Month month, final int day,
                        final TimeScale timeScale) throws IllegalArgumentException {
        this(field, new DateComponents(year, month, day), TimeComponents.H00, timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized as default
     * @param location location in the time scale
     * @param timeScale time scale
     */
    public FieldAbsoluteDate(final Field<T> field, final Date location, final TimeScale timeScale) {
        this(field, new DateComponents(DateComponents.JAVA_EPOCH,
                                (int) (location.getTime() / 86400000l)),
                                new TimeComponents(0.001 * (location.getTime() % 86400000l)),
             timeScale);
    }


    /** Build an instance from an elapsed duration since to another instant.
     * <p>It is important to note that the elapsed duration is <em>not</em>
     * the difference between two readings on a time scale.
     * @param since start instant of the measured duration
     * @param elapsedDuration physically elapsed duration from the <code>since</code>
     * instant, as measured in a regular time scale
     */
    public FieldAbsoluteDate(final FieldAbsoluteDate<T> since, final double elapsedDuration) {
        this.field = since.field;
        final T sum = since.offset.add(elapsedDuration);
        if (Double.isInfinite(sum.getReal())) {
            offset = sum;
            epoch  = (sum.getReal() < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
        } else {
            // compute sum exactly, using Møller-Knuth TwoSum algorithm without branching
            // the following statements must NOT be simplified, they rely on floating point
            // arithmetic properties (rounding and representable numbers)
            // at the end, the EXACT result of addition since.offset + elapsedDuration
            // is sum + residual, where sum is the closest representable number to the exact
            // result and residual is the missing part that does not fit in the first number
            final double oPrime   = sum.getReal() - elapsedDuration;
            final double dPrime   = sum.getReal() - oPrime;
            final double deltaO   = since.offset.getReal() - oPrime;
            final double deltaD   = elapsedDuration - dPrime;
            final double residual = deltaO + deltaD;
            final long   dl       = (long) FastMath.floor(sum.getReal());
            offset = sum.subtract(dl).add(residual);
            epoch  = since.epoch + dl;
        }
    }


    /** Build an instance from an elapsed duration since to another instant.
     * <p>It is important to note that the elapsed duration is <em>not</em>
     * the difference between two readings on a time scale.
     * @param since start instant of the measured duration
     * @param elapsedDuration physically elapsed duration from the <code>since</code>
     * instant, as measured in a regular time scale
     */
    public FieldAbsoluteDate(final AbsoluteDate since, final T elapsedDuration) {
        this.field = elapsedDuration.getField();
        final double dT = since.durationFrom(AbsoluteDate.J2000_EPOCH);
        final T deltaT = elapsedDuration.add(dT);
        final FieldAbsoluteDate<T> j2000 =  getJ2000Epoch(elapsedDuration.getField()).shiftedBy(deltaT);
        offset = j2000.offset;
        epoch = j2000.epoch;
    }

    /** Build an instance from an apparent clock offset with respect to another
     * instant <em>in the perspective of a specific {@link TimeScale time scale}</em>.
     * <p>It is important to note that the apparent clock offset <em>is</em> the
     * difference between two readings on a time scale and <em>not</em> an elapsed
     * duration. As an example, the apparent clock offset between the two instants
     * leading to the readings 2005-12-31T23:59:59 and 2006-01-01T00:00:00 in the
     * {@link UTCScale UTC} time scale is 1 second, but the elapsed duration is 2
     * seconds because a leap second has been introduced at the end of 2005 in this
     * time scale.</p>
     * <p>This constructor is the reverse of the {@link #offsetFrom(FieldAbsoluteDate,
     * TimeScale)} method.</p>
     * @param reference reference instant
     * @param apparentOffset apparent clock offset from the reference instant
     * (difference between two readings in the specified time scale)
     * @param timeScale time scale with respect to which the offset is defined
     * @see #offsetFrom(FieldAbsoluteDate, TimeScale)
     */
    public FieldAbsoluteDate(final FieldAbsoluteDate<T> reference, final double apparentOffset, final TimeScale timeScale) {
        this(reference.field, new DateTimeComponents(reference.getComponents(timeScale), apparentOffset),
             timeScale);
    }

    /** Build an instance from a CCSDS Unsegmented Time Code (CUC).
     * <p>
     * CCSDS Unsegmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * <p>
     * If the date to be parsed is formatted using version 3 of the standard
     * (CCSDS 301.0-B-3 published in 2002) or if the extension of the preamble
     * field introduced in version 4 of the standard is not used, then the
     * {@code preambleField2} parameter can be set to 0.
     * </p>
     * @param field field for the components
     * @param preambleField1 first byte of the field specifying the format, often
     * not transmitted in data interfaces, as it is constant for a given data interface
     * @param preambleField2 second byte of the field specifying the format
     * (added in revision 4 of the CCSDS standard in 2010), often not transmitted in data
     * interfaces, as it is constant for a given data interface (value ignored if presence
     * not signaled in {@code preambleField1})
     * @param timeField byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field
     * specifies the {@link #getCCSDSEpoch(Field) CCSDS reference epoch} is used (and hence
     * may be null in this case)
     * @return an instance corresponding to the specified date
     * @throws OrekitException if preamble is inconsistent with Unsegmented Time Code,
     * or if it is inconsistent with time field, or if agency epoch is needed but not provided
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> parseCCSDSUnsegmentedTimeCode(final Field<T> field,
                                                                                                     final byte preambleField1,
                                                                                                     final byte preambleField2,
                                                                                                     final byte[] timeField,
                                                                                                     final FieldAbsoluteDate<T> agencyDefinedEpoch)
        throws OrekitException {

        // time code identification and reference epoch
        final FieldAbsoluteDate<T> epochF;
        switch (preambleField1 & 0x70) {
            case 0x10:
                // the reference epoch is CCSDS epoch 1958-01-01T00:00:00 TAI
                epochF = getCCSDSEpoch(field);
                break;
            case 0x20:
                // the reference epoch is agency defined
                if (agencyDefinedEpoch == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_DATE_MISSING_AGENCY_EPOCH);
                }
                epochF = agencyDefinedEpoch;
                break;
            default :
                throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                          formatByte(preambleField1));
        }

        // time field lengths
        int coarseTimeLength = 1 + ((preambleField1 & 0x0C) >>> 2);
        int fineTimeLength   = preambleField1 & 0x03;

        if ((preambleField1 & 0x80) != 0x0) {
            // there is an additional octet in preamble field
            coarseTimeLength += (preambleField2 & 0x60) >>> 5;
            fineTimeLength   += (preambleField2 & 0x1C) >>> 2;
        }

        if (timeField.length != coarseTimeLength + fineTimeLength) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                                      timeField.length, coarseTimeLength + fineTimeLength);
        }

        T seconds = field.getZero();
        for (int i = 0; i < coarseTimeLength; ++i) {
            seconds = seconds.multiply(256).add(field.getZero().add(toUnsigned(timeField[i])));
        }
        T subseconds = field.getZero();
        for (int i = timeField.length - 1; i >= coarseTimeLength; --i) {
            subseconds = (subseconds.add(toUnsigned(timeField[i]))).divide(256);
        }
        return new FieldAbsoluteDate<>(epochF, seconds).shiftedBy(subseconds);

    }

    /** Build an instance from a CCSDS Day Segmented Time Code (CDS).
     * <p>
     * CCSDS Day Segmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * @param field field for the components
     * @param preambleField field specifying the format, often not transmitted in
     * data interfaces, as it is constant for a given data interface
     * @param timeField byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field
     * specifies the {@link #getCCSDSEpoch(Field) CCSDS reference epoch} is used (and hence
     * may be null in this case)
     * @return an instance corresponding to the specified date
     * @throws OrekitException if preamble is inconsistent with Day Segmented Time Code,
     * or if it is inconsistent with time field, or if agency epoch is needed but not provided,
     * or it UTC time scale cannot be retrieved
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> parseCCSDSDaySegmentedTimeCode(final Field<T> field,
                                                                                                      final byte preambleField, final byte[] timeField,
                                                                                                      final DateComponents agencyDefinedEpoch)
        throws OrekitException {

        // time code identification
        if ((preambleField & 0xF0) != 0x40) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }

        // reference epoch
        final DateComponents epochDC;
        if ((preambleField & 0x08) == 0x00) {
            // the reference epoch is CCSDS epoch 1958-01-01T00:00:00 TAI
            epochDC = DateComponents.CCSDS_EPOCH;
        } else {
            // the reference epoch is agency defined
            if (agencyDefinedEpoch == null) {
                throw new OrekitException(OrekitMessages.CCSDS_DATE_MISSING_AGENCY_EPOCH);
            }
            epochDC = agencyDefinedEpoch;
        }

        // time field lengths
        final int daySegmentLength = ((preambleField & 0x04) == 0x0) ? 2 : 3;
        final int subMillisecondLength = (preambleField & 0x03) << 1;
        if (subMillisecondLength == 6) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }
        if (timeField.length != daySegmentLength + 4 + subMillisecondLength) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                                      timeField.length, daySegmentLength + 4 + subMillisecondLength);
        }


        int i   = 0;
        int day = 0;
        while (i < daySegmentLength) {
            day = day * 256 + toUnsigned(timeField[i++]);
        }

        long milliInDay = 0l;
        while (i < daySegmentLength + 4) {
            milliInDay = milliInDay * 256 + toUnsigned(timeField[i++]);
        }
        final int milli   = (int) (milliInDay % 1000l);
        final int seconds = (int) ((milliInDay - milli) / 1000l);

        double subMilli = 0;
        double divisor  = 1;
        while (i < timeField.length) {
            subMilli = subMilli * 256 + toUnsigned(timeField[i++]);
            divisor *= 1000;
        }

        final DateComponents date = new DateComponents(epochDC, day);
        final TimeComponents time = new TimeComponents(seconds);
        return new FieldAbsoluteDate<>(field, date, time, TimeScalesFactory.getUTC()).shiftedBy(milli * 1.0e-3 + subMilli / divisor);

    }

    /** Build an instance from a CCSDS Calendar Segmented Time Code (CCS).
     * <p>
     * CCSDS Calendar Segmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * @param preambleField field specifying the format, often not transmitted in
     * data interfaces, as it is constant for a given data interface
     * @param timeField byte array containing the time code
     * @return an instance corresponding to the specified date
     * @throws OrekitException if preamble is inconsistent with Calendar Segmented Time Code,
     * or if it is inconsistent with time field, or it UTC time scale cannot be retrieved
     */
    public FieldAbsoluteDate<T> parseCCSDSCalendarSegmentedTimeCode(final byte preambleField, final byte[] timeField)
        throws OrekitException {

        // time code identification
        if ((preambleField & 0xF0) != 0x50) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }

        // time field length
        final int length = 7 + (preambleField & 0x07);
        if (length == 14) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }
        if (timeField.length != length) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                                      timeField.length, length);
        }

        // date part in the first four bytes
        final DateComponents date;
        if ((preambleField & 0x08) == 0x00) {
            // month of year and day of month variation
            date = new DateComponents(toUnsigned(timeField[0]) * 256 + toUnsigned(timeField[1]),
                                      toUnsigned(timeField[2]),
                                      toUnsigned(timeField[3]));
        } else {
            // day of year variation
            date = new DateComponents(toUnsigned(timeField[0]) * 256 + toUnsigned(timeField[1]),
                                      toUnsigned(timeField[2]) * 256 + toUnsigned(timeField[3]));
        }

        // time part from bytes 5 to last (between 7 and 13 depending on precision)
        final TimeComponents time = new TimeComponents(toUnsigned(timeField[4]),
                                                       toUnsigned(timeField[5]),
                                                       toUnsigned(timeField[6]));
        double subSecond = 0;
        double divisor   = 1;
        for (int i = 7; i < length; ++i) {
            subSecond = subSecond * 100 + toUnsigned(timeField[i]);
            divisor *= 100;
        }

        return new FieldAbsoluteDate<>(field, date, time, TimeScalesFactory.getUTC()).shiftedBy(subSecond / divisor);

    }

    /** Decode a signed byte as an unsigned int value.
     * @param b byte to decode
     * @return an unsigned int value
     */
    private static int toUnsigned(final byte b) {
        final int i = (int) b;
        return (i < 0) ? 256 + i : i;
    }

    /** Format a byte as an hex string for error messages.
     * @param data byte to format
     * @return a formatted string
     */
    private static String formatByte(final byte data) {
        return "0x" + Integer.toHexString(data).toUpperCase();
    }

    /** Build an instance corresponding to a Julian Day date.
     * @param jd Julian day
     * @param secondsSinceNoon seconds in the Julian day
     * (BEWARE, Julian days start at noon, so 0.0 is noon)
     * @param timeScale time scale in which the seconds in day are defined
     * @return a new instant
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> createJDDate(final int jd, final T secondsSinceNoon,
                                                                                    final TimeScale timeScale) {
        return new FieldAbsoluteDate<>(secondsSinceNoon.getField(), new DateComponents(DateComponents.JULIAN_EPOCH, jd),
                                       TimeComponents.H12, timeScale).shiftedBy(secondsSinceNoon);
    }

    /** Build an instance corresponding to a Modified Julian Day date.
     * @param mjd modified Julian day
     * @param secondsInDay seconds in the day
     * @param timeScale time scale in which the seconds in day are defined
     * @return a new instant
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> createMJDDate(final int mjd, final T secondsInDay,
                                                                                     final TimeScale timeScale) {
        return new FieldAbsoluteDate<>(secondsInDay.getField(),
                                       new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd),
                                       TimeComponents.H00,
                                       timeScale).shiftedBy(secondsInDay);
    }

    /** Build an instance corresponding to a GPS date.
     * <p>GPS dates are provided as a week number starting at
     * {@link #getGPSEpoch(Field) GPS epoch} and as a number of milliseconds
     * since week start.</p>
     * @param weekNumber week number since {@link #getGPSEpoch(Field) GPS epoch}
     * @param milliInWeek number of milliseconds since week start
     * @return a new instant
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> createGPSDate(final int weekNumber, final T milliInWeek) {
        final int day = (int) FastMath.floor(milliInWeek.getReal() / (1000.0 * Constants.JULIAN_DAY));
        final T secondsInDay = milliInWeek.divide(1000.0).subtract(day * Constants.JULIAN_DAY);
        return new FieldAbsoluteDate<>(milliInWeek.getField(), new DateComponents(DateComponents.GPS_EPOCH, weekNumber * 7 + day),
                                       TimeComponents.H00, TimeScalesFactory.getGPS()).shiftedBy(secondsInDay);
    }

    /** Build an instance corresponding to a Julian Epoch (JE).
     * <p>According to Lieske paper: <a
     * href="http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&defaultprint=YES&filetype=.pdf.">
     * Precession Matrix Based on IAU (1976) System of Astronomical Constants</a>, Astronomy and Astrophysics,
     * vol. 73, no. 3, Mar. 1979, p. 282-284, Julian Epoch is related to Julian Ephemeris Date as:</p>
     * <pre>
     * JE = 2000.0 + (JED - 2451545.0) / 365.25
     * </pre>
     * <p>
     * This method reverts the formula above and computes an {@code FieldAbsoluteDate<T>} from the Julian Epoch.
     * </p>
     * @param julianEpoch Julian epoch, like 2000.0 for defining the classical reference J2000.0
     * @return a new instant
     * @see #getJ2000Epoch(Field)
     * @see #createBesselianEpoch(RealFieldElement)
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> createJulianEpoch(final T julianEpoch) {
        return new FieldAbsoluteDate<>(getJ2000Epoch(julianEpoch.getField()),
                                       julianEpoch.subtract(2000.0).multiply(Constants.JULIAN_YEAR));
    }

    /** Build an instance corresponding to a Besselian Epoch (BE).
     * <p>According to Lieske paper: <a
     * href="http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&defaultprint=YES&filetype=.pdf.">
     * Precession Matrix Based on IAU (1976) System of Astronomical Constants</a>, Astronomy and Astrophysics,
     * vol. 73, no. 3, Mar. 1979, p. 282-284, Besselian Epoch is related to Julian Ephemeris Date as:</p>
     * <pre>
     * BE = 1900.0 + (JED - 2415020.31352) / 365.242198781
     * </pre>
     * <p>
     * This method reverts the formula above and computes an {@code FieldAbsoluteDate<T>} from the Besselian Epoch.
     * </p>
     * @param besselianEpoch Besselian epoch, like 1950 for defining the classical reference B1950.0
     * @return a new instant
     * @see #createJulianEpoch(RealFieldElement)
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> createBesselianEpoch(final T besselianEpoch) {
        return new FieldAbsoluteDate<>(getJ2000Epoch(besselianEpoch.getField()),
                                       besselianEpoch.subtract(1900).multiply(Constants.BESSELIAN_YEAR).add(
                                       Constants.JULIAN_DAY * (-36525) + Constants.JULIAN_DAY * 0.31352));
    }

    /** Reference epoch for julian dates: -4712-01-01T12:00:00 Terrestrial Time.
     * <p>Both <code>java.util.Date</code> and {@link DateComponents} classes
     * follow the astronomical conventions and consider a year 0 between
     * years -1 and +1, hence this reference date lies in year -4712 and not
     * in year -4713 as can be seen in other documents or programs that obey
     * a different convention (for example the <code>convcal</code> utility).</p>
     * @param field field for the components
     * @return FieldAbsoluteDate<T> FieldAbsoluteDate<T> representing {@link AbsoluteDate#JULIAN_EPOCH}
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> getJulianEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.JULIAN_EPOCH);
    }

    /** Reference epoch for modified julian dates: 1858-11-17T00:00:00 Terrestrial Time.
     * @param field field for the components
     * @return FieldAbsoluteDate<T> FieldAbsoluteDate<T> representing {@link AbsoluteDate#MODIFIED_JULIAN_EPOCH}
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> getModifiedJulianEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.MODIFIED_JULIAN_EPOCH);
    }

    /** Reference epoch for 1950 dates: 1950-01-01T00:00:00 Terrestrial Time.
     * @param field field for the components
     * @return FieldAbsoluteDate<T> FieldAbsoluteDate<T> representing {@link AbsoluteDate#FIFTIES_EPOCH}
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> getFiftiesEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.FIFTIES_EPOCH);
    }
    /** Reference epoch for CCSDS Time Code Format (CCSDS 301.0-B-4):
     * 1958-01-01T00:00:00 International Atomic Time (<em>not</em> UTC).
     * @param field field for the components
     * @return FieldAbsoluteDate<T> FieldAbsoluteDate<T> representing {@link AbsoluteDate#CCSDS_EPOCH}
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> getCCSDSEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.CCSDS_EPOCH);
    }

    /** Reference epoch for Galileo System Time: 1999-08-22T00:00:00 UTC.
     * @param field field for the components
     * @return FieldAbsoluteDate<T> FieldAbsoluteDate<T> representing {@link AbsoluteDate#GALILEO_EPOCH}
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> getGalileoEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.GALILEO_EPOCH);
    }

    /** Reference epoch for GPS weeks: 1980-01-06T00:00:00 GPS time.
     * @param field field for the components
     * @return FieldAbsoluteDate<T> FieldAbsoluteDate<T> representing {@link AbsoluteDate#GPS_EPOCH}
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> getGPSEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.GPS_EPOCH);
    }

    /** J2000.0 Reference epoch: 2000-01-01T12:00:00 Terrestrial Time (<em>not</em> UTC).
     * @param field field for the components
     * @see #createJulianEpoch(RealFieldElement)
     * @see #createBesselianEpoch(RealFieldElement)
     * @return FieldAbsoluteDate<T> FieldAbsoluteDate<T> representing {@link AbsoluteDate#J2000_EPOCH}
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> getJ2000Epoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
    }

    /** Java Reference epoch: 1970-01-01T00:00:00 Universal Time Coordinate.
     * <p>
     * Between 1968-02-01 and 1972-01-01, UTC-TAI = 4.213 170 0s + (MJD - 39 126) x 0.002 592s.
     * As on 1970-01-01 MJD = 40587, UTC-TAI = 8.000082s
     * </p>
     * @param field field for the components
     * @return FieldAbsoluteDate<T> FieldAbsoluteDate<T> representing {@link AbsoluteDate#JAVA_EPOCH}
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> getJavaEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.JAVA_EPOCH);
    }

    /** Dummy date at infinity in the past direction.
     * @param field field for the components
     * @return FieldAbsoluteDate<T> FieldAbsoluteDate<T> representing {@link AbsoluteDate#PAST_INFINITY}
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> getPastInfinity(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.PAST_INFINITY);
    }

    /** Dummy date at infinity in the future direction.
     * @param field field for the components
     * @return FieldAbsoluteDate<T> FieldAbsoluteDate<T> representing {@link AbsoluteDate#FUTURE_INFINITY}
     * @param <T> the type of the field elements
     */
    public static <T extends RealFieldElement<T>> FieldAbsoluteDate<T> getFutureInfinity(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.FUTURE_INFINITY);
    }

    /** Get a time-shifted date.
     * <p>
     * Calling this method is equivalent to call <code>new FieldAbsoluteDate<>(this, dt)</code>.
     * </p>
     * @param dt time shift in seconds
     * @return a new date, shifted with respect to instance (which is immutable)
     * @see org.orekit.utils.PVCoordinates#shiftedBy(double)
     * @see org.orekit.attitudes.Attitude#shiftedBy(double)
     * @see org.orekit.orbits.Orbit#shiftedBy(double)
     * @see org.orekit.propagation.SpacecraftState#shiftedBy(double)
     */
    public FieldAbsoluteDate<T> shiftedBy(final T dt) {
        return new FieldAbsoluteDate<>(this, dt);
    }

    /** Compute the physically elapsed duration between two instants.
     * <p>The returned duration is the number of seconds physically
     * elapsed between the two instants, measured in a regular time
     * scale with respect to surface of the Earth (i.e either the {@link
     * TAIScale TAI scale}, the {@link TTScale TT scale} or the {@link
     * GPSScale GPS scale}). It is the only method that gives a
     * duration with a physical meaning.</p>
     * <p>This method gives the same result (with less computation)
     * as calling {@link #offsetFrom(FieldAbsoluteDate, TimeScale)}
     * with a second argument set to one of the regular scales cited
     * above.</p>
     * <p>This method is the reverse of the {@link #FieldAbsoluteDate(FieldAbsoluteDate,
     * double)} constructor.</p>
     * @param instant instant to subtract from the instance
     * @return offset in seconds between the two instants (positive
     * if the instance is posterior to the argument)
     * @see #offsetFrom(FieldAbsoluteDate, TimeScale)
     * @see #FieldAbsoluteDate(FieldAbsoluteDate, double)
     */
    public T durationFrom(final FieldAbsoluteDate<T> instant) {
        return offset.subtract(instant.offset).add(epoch - instant.epoch);
    }

    /** Compute the physically elapsed duration between two instants.
     * <p>The returned duration is the number of seconds physically
     * elapsed between the two instants, measured in a regular time
     * scale with respect to surface of the Earth (i.e either the {@link
     * TAIScale TAI scale}, the {@link TTScale TT scale} or the {@link
     * GPSScale GPS scale}). It is the only method that gives a
     * duration with a physical meaning.</p>
     * <p>This method gives the same result (with less computation)
     * as calling {@link #offsetFrom(FieldAbsoluteDate, TimeScale)}
     * with a second argument set to one of the regular scales cited
     * above.</p>
     * <p>This method is the reverse of the {@link #FieldAbsoluteDate(FieldAbsoluteDate,
     * double)} constructor.</p>
     * @param instant instant to subtract from the instance
     * @return offset in seconds between the two instants (positive
     * if the instance is posterior to the argument)
     * @see #offsetFrom(FieldAbsoluteDate, TimeScale)
     * @see #FieldAbsoluteDate(FieldAbsoluteDate, double)
     */
    public T durationFrom(final AbsoluteDate instant) {
        return offset.subtract(instant.getOffset()).add(epoch - instant.getEpoch());
    }

    /** Compute the apparent clock offset between two instant <em>in the
     * perspective of a specific {@link TimeScale time scale}</em>.
     * <p>The offset is the number of seconds counted in the given
     * time scale between the locations of the two instants, with
     * all time scale irregularities removed (i.e. considering all
     * days are exactly 86400 seconds long). This method will give
     * a result that may not have a physical meaning if the time scale
     * is irregular. For example since a leap second was introduced at
     * the end of 2005, the apparent offset between 2005-12-31T23:59:59
     * and 2006-01-01T00:00:00 is 1 second, but the physical duration
     * of the corresponding time interval as returned by the {@link
     * #durationFrom(FieldAbsoluteDate)} method is 2 seconds.</p>
     * <p>This method is the reverse of the {@link #FieldAbsoluteDate(FieldAbsoluteDate,
     * double, TimeScale)} constructor.</p>
     * @param instant instant to subtract from the instance
     * @param timeScale time scale with respect to which the offset should
     * be computed
     * @return apparent clock offset in seconds between the two instants
     * (positive if the instance is posterior to the argument)
     * @see #durationFrom(FieldAbsoluteDate)
     * @see #FieldAbsoluteDate(FieldAbsoluteDate, double, TimeScale)
     */
    public T offsetFrom(final FieldAbsoluteDate<T> instant, final TimeScale timeScale) {
        final long   elapsedDurationA = epoch - instant.epoch;
        final T elapsedDurationB = offset.add(timeScale.offsetFromTAI(this)).
                                   subtract(instant.offset.add(timeScale.offsetFromTAI(instant)));
        return  elapsedDurationB.add(elapsedDurationA);
    }

    /** Compute the offset between two time scales at the current instant.
     * <p>The offset is defined as <i>l₁-l₂</i>
     * where <i>l₁</i> is the location of the instant in
     * the <code>scale1</code> time scale and <i>l₂</i> is the
     * location of the instant in the <code>scale2</code> time scale.</p>
     * @param scale1 first time scale
     * @param scale2 second time scale
     * @return offset in seconds between the two time scales at the
     * current instant
     */
    public T timeScalesOffset(final TimeScale scale1, final TimeScale scale2) {
        return scale1.offsetFromTAI(this).subtract(scale2.offsetFromTAI(this));
    }

    /** Convert the instance to a Java {@link java.util.Date Date}.
     * <p>Conversion to the Date class induces a loss of precision because
     * the Date class does not provide sub-millisecond information. Java Dates
     * are considered to be locations in some times scales.</p>
     * @param timeScale time scale to use
     * @return a {@link java.util.Date Date} instance representing the location
     * of the instant in the time scale
     */
    public Date toDate(final TimeScale timeScale) {
        final double time = epoch + (offset.getReal() + timeScale.offsetFromTAI(this).getReal());
        return new Date(FastMath.round((time + 10957.5 * 86400.0) * 1000));
    }

    /** Split the instance into date/time components.
     * @param timeScale time scale to use
     * @return date/time components
     */
    public DateTimeComponents getComponents(final TimeScale timeScale) {

        if (Double.isInfinite(offset.getReal())) {
            // special handling for past and future infinity
            if (offset.getReal() < 0) {
                return new DateTimeComponents(DateComponents.MIN_EPOCH, TimeComponents.H00);
            } else {
                return new DateTimeComponents(DateComponents.MAX_EPOCH,
                                              new TimeComponents(23, 59, 59.999));
            }
        }

        // compute offset from 2000-01-01T00:00:00 in specified time scale exactly,
        // using Møller-Knuth TwoSum algorithm without branching
        // the following statements must NOT be simplified, they rely on floating point
        // arithmetic properties (rounding and representable numbers)
        // at the end, the EXACT result of addition offset + timeScale.offsetFromTAI(this)
        // is sum + residual, where sum is the closest representable number to the exact
        // result and residual is the missing part that does not fit in the first number
        final double taiOffset = timeScale.offsetFromTAI(this).getReal();
        final double sum       = offset.getReal() + taiOffset;
        final double oPrime    = sum - taiOffset;
        final double dPrime    = sum - oPrime;
        final double deltaO    = offset.getReal() - oPrime;
        final double deltaD    = taiOffset - dPrime;
        final double residual  = deltaO + deltaD;

        // split date and time
        final long   carry = (long) FastMath.floor(sum);
        double offset2000B = (sum - carry) + residual;
        long   offset2000A = epoch + carry + 43200l;
        if (offset2000B < 0) {
            offset2000A -= 1;
            offset2000B += 1;
        }
        long time = offset2000A % 86400l;
        if (time < 0l) {
            time += 86400l;
        }
        final int date = (int) ((offset2000A - time) / 86400l);

        // extract calendar elements
        final DateComponents dateComponents = new DateComponents(DateComponents.J2000_EPOCH, date);
        TimeComponents timeComponents = new TimeComponents((int) time, offset2000B);

        if (timeScale.insideLeap(this)) {
            // fix the seconds number to take the leap into account
            timeComponents = new TimeComponents(timeComponents.getHour(), timeComponents.getMinute(),
                                                timeComponents.getSecond() + timeScale.getLeap(this).getReal());
        }

        // build the components
        return new DateTimeComponents(dateComponents, timeComponents);

    }

    /** Split the instance into date/time components for a local time.
     * @param minutesFromUTC offset in <em>minutes</em> from UTC (positive Eastwards UTC,
     * negative Westward UTC)
     * @return date/time components
     * @exception OrekitException if UTC time scale cannot be retrieved
     */
    public DateTimeComponents getComponents(final int minutesFromUTC)
        throws OrekitException {

        final DateTimeComponents utcComponents = getComponents(TimeScalesFactory.getUTC());

        // shift the date according to UTC offset, but WITHOUT touching the seconds,
        // as they may exceed 60.0 during a leap seconds introduction,
        // and we want to preserve these special cases
        final double seconds = utcComponents.getTime().getSecond();
        int minute = utcComponents.getTime().getMinute() + minutesFromUTC;
        final int hourShift;
        if (minute < 0) {
            hourShift = (minute - 59) / 60;
        } else if (minute > 59) {
            hourShift = minute / 60;
        } else {
            hourShift = 0;
        }
        minute -= 60 * hourShift;
        int hour = utcComponents.getTime().getHour() + hourShift;
        final int dayShift;
        if (hour < 0) {
            dayShift = (hour - 23) / 24;
        } else if (hour > 23) {
            dayShift = hour / 24;
        } else {
            dayShift = 0;
        }
        hour -= 24 * dayShift;

        return new DateTimeComponents(new DateComponents(utcComponents.getDate(), dayShift),
                                      new TimeComponents(hour, minute, seconds, minutesFromUTC));

    }

    /** {@inheritDoc} */
    public FieldAbsoluteDate<T> getDate() {
        return this;
    }

    /** Get the field.
     * @return field instance.
     */
    public Field<T> getField() {
        return field;
    }

    /** Split the instance into date/time components for a time zone.
     * @param timeZone time zone
     * @return date/time components
     * @exception OrekitException if UTC time scale cannot be retrieved
     */
    public DateTimeComponents getComponents( final TimeZone timeZone)
        throws OrekitException {
        final long milliseconds = FastMath.round((offsetFrom(getJavaEpoch(field), TimeScalesFactory.getUTC()).getReal()) * 1000);
        return getComponents(timeZone.getOffset(milliseconds) / 60000);
    }

    /** Compare the instance with another date.
     * @param date other date to compare the instance to
     * @return a negative integer, zero, or a positive integer as this date
     * is before, simultaneous, or after the specified date.
     */
    public int compareTo(final FieldAbsoluteDate<T> date) {
        return Double.compare(durationFrom(date).getReal(), 0.0);
    }


    /** Check if the instance represent the same time as another instance.
     * @param date other date
     * @return true if the instance and the other date refer to the same instant
     */
    @SuppressWarnings("unchecked")
    public boolean equals(final Object date) {

        if (date == this) {
            // first fast check
            return true;
        }

        if ((date != null) && (date instanceof FieldAbsoluteDate)) {
            return durationFrom((FieldAbsoluteDate<T>) date).getReal() == 0.0;
        }

        return false;

    }

    /** Get a hashcode for this date.
     * @return hashcode
     */
    public int hashCode() {
        final long l = Double.doubleToLongBits(durationFrom(AbsoluteDate.J2000_EPOCH).getReal());
        return (int) (l ^ (l >>> 32));
    }

    /** Get a String representation of the instant location in UTC time scale.
     * @return a string representation of the instance,
     * in ISO-8601 format with milliseconds accuracy
     */
    public String toString() {
        try {
            return toString(TimeScalesFactory.getUTC());
        } catch (OrekitException oe) {
            throw new RuntimeException(oe);
        }
    }

    /** Get a String representation of the instant location.
     * @param timeScale time scale to use
     * @return a string representation of the instance,
     * in ISO-8601 format with milliseconds accuracy
     */
    public String toString(final TimeScale timeScale) {
        return getComponents(timeScale).toString(timeScale.minuteDuration(this));
    }

    /** Get a String representation of the instant location for a local time.
     * @param minutesFromUTC offset in <em>minutes</em> from UTC (positive Eastwards UTC,
     * negative Westward UTC).
     * @return string representation of the instance,
     * in ISO-8601 format with milliseconds accuracy
     * @exception OrekitException if UTC time scale cannot be retrieved
     */
    public String toString(final int minutesFromUTC)
        throws OrekitException {
        final int minuteDuration = TimeScalesFactory.getUTC().minuteDuration(this);
        return getComponents(minutesFromUTC).toString(minuteDuration);
    }

    /** Get a String representation of the instant location for a time zone.
     * @param timeZone time zone
     * @return string representation of the instance,
     * in ISO-8601 format with milliseconds accuracy
     * @exception OrekitException if UTC time scale cannot be retrieved
     */
    public String toString(final TimeZone timeZone)
        throws OrekitException {
        final int minuteDuration = TimeScalesFactory.getUTC().minuteDuration(this);
        return getComponents(timeZone).toString(minuteDuration);
    }

    /** Get a time-shifted date.
     * <p>
     * Calling this method is equivalent to call <code>new AbsoluteDate(this, dt)</code>.
     * </p>
     * @param dt time shift in seconds
     * @return a new date, shifted with respect to instance (which is immutable)
     * @see org.orekit.utils.PVCoordinates#shiftedBy(double)
     * @see org.orekit.attitudes.Attitude#shiftedBy(double)
     * @see org.orekit.orbits.Orbit#shiftedBy(double)
     * @see org.orekit.propagation.SpacecraftState#shiftedBy(double)
     */

    @Override
    public FieldAbsoluteDate<T> shiftedBy(final double dt) {
        return new FieldAbsoluteDate<>(this, dt);
    }


    /** Transform the FieldAbsoluteDate in an AbsoluteDate.
     * @return AbsoluteDate of the FieldObject
     * */
    public AbsoluteDate toAbsoluteDate() {
        return new AbsoluteDate(epoch, offset.getReal());
    }

}


