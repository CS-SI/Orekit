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


/** TAI UTC offset model.
 * @see UTCTAIOffsetsLoader
 * @author Luc Maisonobe
 * @since 7.1
 */
public class OffsetModel {

    /** Date of the offset start. */
    private final DateComponents start;

    /** Reference date of the linear model as a modified julian day. */
    private final int mjdRef;

    /** Offset at reference date in seconds (TAI minus UTC). */
    private final double offset;

    /** Offset slope in seconds per UTC day (TAI minus UTC / dUTC). */
    private final double slope;

    /** Constructor for a linear offset model.
     * <p>
     * These models were used prior to 1972.
     * </p>
     * @param start date of the offset start
     * @param mjdRef reference date of the linear model as a modified julian day
     * @param offset offset at reference date in seconds (TAI minus UTC)
     * @param slope offset slope in seconds per UTC day (TAI minus UTC / dUTC)
     */
    public OffsetModel(final DateComponents start,
                       final int mjdRef, final double offset, final double slope) {
        this.start  = start;
        this.mjdRef = mjdRef;
        this.offset = offset;
        this.slope  = slope;
    }

    /** Constructor for a constant offset model.
     * <p>
     * These models are used since 1972.
     * </p>
     * @param start date of the offset start
     * @param offset offset at reference date in seconds (TAI minus UTC)
     */
    public OffsetModel(final DateComponents start, final int offset) {
        this(start, 41317, offset, 0.0);
    }

    /** Get the date of the offset start.
     * @return date of the offset start
     */
    public DateComponents getStart() {
        return start;
    }

    /** Get the reference date of the linear model as a modified julian day.
     * @return reference date of the linear model as a modified julian day
     */
    public int getMJDRef() {
        return mjdRef;
    }

    /** Offset at reference date in seconds (TAI minus UTC).
     * @return offset at reference date in seconds (TAI minus UTC)
     */
    public double getOffset() {
        return offset;
    }

    /** Offset slope in seconds per UTC day (TAI minus UTC / dUTC).
     * @return offset slope in seconds per UTC day (TAI minus UTC / dUTC)
     */
    public double getSlope() {
        return slope;
    }

}
