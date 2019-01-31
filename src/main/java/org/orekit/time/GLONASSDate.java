/* Copyright 2002-2019 CS Systèmes d'Information
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

/** Container for date in GLONASS form.
 * @author Bryan Cazabonne
 * @see AbsoluteDate
 */
public class GLONASSDate implements Serializable, TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = 20190131L;

    /** Constant for date computation. */
    private static final int C1 = 44195;

    /** Constant for date computation. */
    private static final int C2 = 45290;

    /** The number of the current day in a four year interval N<sub>a</sub>. */
    private final int na;

    /** The number of the current four year interval N<sub>4</sub>. */
    private final int n4;

    /** Number of seconds since N<sub>a</sub>. */
    private final double secInNa;

    /** Corresponding date. */
    private final transient AbsoluteDate date;

    /** Build an instance corresponding to a GLONASS date.
     * @param na the number of the current day in a four year interval
     * @param n4 the number of the current four year interval
     * @param secInNa the number of seconds since na start
     */
    public GLONASSDate(final int na, final int n4, final double secInNa) {
        this.na      = na;
        this.n4      = n4;
        this.secInNa = secInNa;
        this.date    = computeDate();
    }

    /** Build an instance from an absolute date.
     * @param date absolute date to consider
     */
    public GLONASSDate(final AbsoluteDate date) {
        final DateTimeComponents dateTime = date.getComponents(TimeScalesFactory.getGLONASS());

        final int year = dateTime.getDate().getYear();
        this.n4   = ((int) (year - 1996) / 4) + 1;

        final int start = 1996 + 4 * (n4 - 1);
        final double duration = date.durationFrom(new AbsoluteDate(start, 1, 1, TimeScalesFactory.getGLONASS()));
        this.na = (int) (duration / 86400) + 1;

        this.secInNa = dateTime.getTime().getSecondsInLocalDay();
        this.date    = date;
    }

    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the number of seconds since N<sub>a</sub> start.
     * @return number of seconds since N<sub>a</sub> start
     */
    public double getSecInNa() {
        return secInNa;
    }

    /** Get the number of the current day in a four year interval.
     * @return the number of the current day in a four year interval
     */
    public int getNa() {
        return na;
    }

    /** Get the number of the current four year interval.
     * @return the number of the current four year interval
     */
    public int getN4() {
        return n4;
    }

    /** Computes the GLONASS date.
     * Ref: GLONASS Interface Control Document v1.0, 2016, Appendix K
     * @return the date
     */
    private AbsoluteDate computeDate() {
        // Compute JD0
        final double jd0 = 1461 * (n4 - 1) + na + 2450082.5 - ((na - 3) / (25 + C1 + C2));
        // Compute the number of Julian day for the current date
        final double jdn = jd0 + 0.5;
        // Coefficients
        final int a = (int) (jdn + 32044);
        final int b = (4 * a + 3) / 146097;
        final int c = a - (146097 * b) / 4;
        final int d = (4 * c + 3) / 1461;
        final int e = c - (1461 * d) / 4;
        final int m = (5 * e + 2) / 153;
        // Year, month and day
        final int day   = e - (153 * m + 2) / 5 + 1;
        final int month = m + 3 - 12 * (m / 10);
        final int year  = 100 * b + d - 4800 + m / 10;
        return new AbsoluteDate(new DateComponents(year, month, day),
                                new TimeComponents(secInNa),
                                TimeScalesFactory.getGLONASS());
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(na, n4, secInNa);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20190131L;

        /** The number of the current day in a four year interval N<sub>a</sub>. */
        private final int na;

        /** The number of the current four year interval N<sub>4</sub>. */
        private final int n4;

        /** Number of seconds since N<sub>a</sub>. */
        private final double secInNa;

        /** Simple constructor.
         * @param na the number of the current day in a four year interval
         * @param n4 the number of the current four year interval
         * @param secInNa the number of seconds since na start
         */
        DataTransferObject(final int na, final int n4, final double secInNa) {
            this.na      = na;
            this.n4      = n4;
            this.secInNa = secInNa;
        }

        /** Replace the deserialized data transfer object with a {@link GPSDate}.
         * @return replacement {@link GPSDate}
         */
        private Object readResolve() {
            return new GLONASSDate(na, n4, secInNa);
        }

    }

}
