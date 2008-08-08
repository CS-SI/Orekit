/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

/** Offset between {@link UTCScale UTC} and  {@link TAIScale TAI} time scales.
 * <p>The {@link UTCScale UTC} and  {@link TAIScale TAI} time scales are two
 * scales offset with respect to each other. The {@link TAIScale TAI} scale is
 * continuous whether the {@link UTCScale UTC} includes some discontinuity when
 * leap seconds are introduced by the <a href="http://www.iers.org/">International
 * Earth Rotation Service</a> (IERS).</p>
 * <p>This class represents the constant offset between the two scales that is
 * valid between two leap seconds occurrences./p>
 * @author Luc Maisonobe
 * @see UTCScale
 * @see UTCTAIHistoryFilesLoader
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
class UTCTAIOffset implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 4742190573136348054L;

    /** Leap date. */
    private final AbsoluteDate leapDate;

    /** Offset start of validity date. */
    private final AbsoluteDate validityStart;

    /** Offset end of validity date. */
    private AbsoluteDate validityEnd;

    /** Value of the leap at offset validity start (in seconds). */
    private final double leap;

    /** Offset in seconds (TAI minus UTC). */
    private final double offset;

    /** Simple constructor.
     * @param leapDate leap date
     * @param leap value of the leap at offset validity start (in seconds)
     * @param offset offset in seconds (TAI minus UTC)
     */
    public UTCTAIOffset(final AbsoluteDate leapDate, final double leap, final double offset) {
        this.leapDate      = leapDate;
        this.validityStart = new AbsoluteDate(leapDate, leap);
        this.validityEnd   = AbsoluteDate.FUTURE_INFINITY;
        this.leap          = leap;
        this.offset        = offset;
    }

    /** Get the date of the start of the leap.
     * @return date of the start of the leap
     * @see #getValidityStart()
     */
    public AbsoluteDate getDate() {
        return leapDate;
    }

    /** Get the start time of validity for this offset.
     * <p>The start of the validity of the offset is {@link #getLeap()}
     * seconds after the start of the leap itsef.</p>
     * @return start of validity date
     * @see #getDate()
     * @see #getValidityEnd()
     */
    public AbsoluteDate getValidityStart() {
        return validityStart;
    }

    /** Get the end time of validity for this offset.
     * <p>The end of the validity of the offset is the date of the
     * start of the leap leading to the next offset.</p>
     * @return end of validity date
     * @see #getValidityStart()
     */
    public AbsoluteDate getValidityEnd() {
        return validityEnd;
    }

    /** Set the end time of validity for this offset.
     * @param validityEnd end of validity date
     * @see #getValidityEnd()
     */
    void setValidityEnd(final AbsoluteDate validityEnd) {
        this.validityEnd = validityEnd;
    }

    /** Get the value of the leap at offset validity start (in seconds).
     * @return value of the leap at offset validity start (in seconds)
     */
    public double getLeap() {
        return leap;
    }

    /** Get the offset in seconds.
     * @return offset in seconds.
     */
    public double getOffset() {
        return offset;
    }

}
