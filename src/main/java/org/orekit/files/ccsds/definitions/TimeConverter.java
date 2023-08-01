/* Copyright 2002-2023 CS GROUP
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
package org.orekit.files.ccsds.definitions;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.SatelliteClockScale;
import org.orekit.time.TimeScale;

/** Dates reader/writer.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class TimeConverter {

    /** Base time scale. */
    private final TimeScale timeScale;

    /** Reference date for relative dates (may be null if no relative dates are used). */
    private final AbsoluteDate referenceDate;

    /** Build a time system.
     * @param timeScale base time scale
     * @param referenceDate reference date for relative dates (may be null if no relative dates are used)
     */
    public TimeConverter(final TimeScale timeScale, final AbsoluteDate referenceDate) {
        this.timeScale     = timeScale;
        this.referenceDate = referenceDate;
    }

    /** Parse a relative or absolute date.
     * @param s string to parse
     * @return parsed date
     */
    public AbsoluteDate parse(final String s) {

        if (referenceDate != null && s.indexOf('T') < 0) {

            // relative date
            final double delta = Double.parseDouble(s);
            if (timeScale instanceof SatelliteClockScale) {
                // satellite clock scale handles drifts internally
                return ((SatelliteClockScale) timeScale).dateAtCount(delta);
            } else {
                // regular relative date from a known reference
                return referenceDate.shiftedBy(delta);
            }

        } else {

            // absolute date
            return new AbsoluteDate(s, timeScale);

        }

    }

    /** Generate calendar components.
     * @param date date to convert
     * @return date components
     */
    public DateTimeComponents components(final AbsoluteDate date) {
        return date.getComponents(timeScale);
    }

    /** Generate relative offset.
     * @param date date to convert
     * @return relative offset
     */
    public double offset(final AbsoluteDate date) {
        return date.durationFrom(referenceDate);
    }

    /** Get the base time scale.
     * @return base time scale
     */
    public TimeScale getTimeScale() {
        return timeScale;
    }

    /** Get the reference date for relative dates (may be null if no relative dates are used).
     * @return reference date for relative dates (may be null if no relative dates are used)
     * @since 12.0
     */
    public AbsoluteDate getReferenceDate() {
        return referenceDate;
    }

}
