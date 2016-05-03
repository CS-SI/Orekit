/* Copyright 2002-2016 CS Systèmes d'Information
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

import java.io.Serializable;

import org.orekit.utils.Constants;

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
class UTCTAIOffset implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 4742190573136348054L;

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
    private final double offset;

    /** Offset slope in seconds per UTC second (TAI minus UTC / dUTC). */
    private final double slopeUTC;

    /** Offset slope in seconds per TAI second (TAI minus UTC / dTAI). */
    private final double slopeTAI;

    /** Simple constructor for a constant model.
     * @param leapDate leap date
     * @param leapDateMJD leap date in Modified Julian Day
     * @param leap value of the leap at offset validity start (in seconds)
     * @param offset offset in seconds (TAI minus UTC)
     */
    UTCTAIOffset(final AbsoluteDate leapDate, final int leapDateMJD,
                        final double leap, final double offset) {
        this(leapDate, leapDateMJD, leap, offset, 0, 0);
    }

    /** Simple constructor for a linear model.
     * @param leapDate leap date
     * @param leapDateMJD leap date in Modified Julian Day
     * @param leap value of the leap at offset validity start (in seconds)
     * @param offset offset in seconds (TAI minus UTC)
     * @param mjdRef reference date for the slope multiplication as Modified Julian Day
     * @param slope offset slope in seconds per UTC second (TAI minus UTC / dUTC)
     */
    UTCTAIOffset(final AbsoluteDate leapDate, final int leapDateMJD,
                        final double leap, final double offset,
                        final int mjdRef, final double slope) {
        this.leapDate      = leapDate;
        this.leapDateMJD   = leapDateMJD;
        this.validityStart = leapDate.shiftedBy(leap);
        this.mjdRef        = mjdRef;
        this.reference     = new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjdRef),
                                              TimeScalesFactory.getTAI()).shiftedBy(offset);
        this.leap          = leap;
        this.offset        = offset;
        this.slopeUTC      = slope;
        this.slopeTAI      = slope / (1 + slope);
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
    public double getOffset(final AbsoluteDate date) {
        return offset + date.durationFrom(reference) * slopeTAI;
    }

    /** Get the TAI - UTC offset in seconds.
     * @param date date components (in UTC) at which the offset is requested
     * @param time time components (in UTC) at which the offset is requested
     * @return TAI - UTC offset in seconds.
     */
    public double getOffset(final DateComponents date, final TimeComponents time) {
        final int    days     = date.getMJD() - mjdRef;
        final double fraction = time.getSecondsInUTCDay();
        return offset + days * (slopeUTC * Constants.JULIAN_DAY) + fraction * slopeUTC;
    }

}
