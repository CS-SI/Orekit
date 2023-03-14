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
package org.orekit.gnss.navigation;

/** Container for time system corrections.
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class TimeSystemCorrection {

    /** Time system correction type. */
    private String timeSystemCorrectionType;

    /** A0 coefficient of linear polynomial for time system correction. */
    private double timeSystemCorrectionA0;

    /** A1 coefficient of linear polynomial for time system correction. */
    private double timeSystemCorrectionA1;

    /** Reference time for time system correction (seconds into GNSS week). */
    private int timeSystemCorrectionSecOfWeek;

    /** Reference week number for time system correction. */
    private int timeSystemCorrectionWeekNumber;

    /**
     * Constructor.
     * @param timeSystemCorrectionType       time system correction type
     * @param timeSystemCorrectionA0         A0 coefficient of linear polynomial for time system correction
     * @param timeSystemCorrectionA1         A1 coefficient of linear polynomial for time system correction
     * @param timeSystemCorrectionSecOfWeek  reference time for time system correction
     * @param timeSystemCorrectionWeekNumber reference week number for time system correction
     */
    public TimeSystemCorrection(final String timeSystemCorrectionType,
                                final double timeSystemCorrectionA0,
                                final double timeSystemCorrectionA1,
                                final int timeSystemCorrectionSecOfWeek,
                                final int timeSystemCorrectionWeekNumber) {
        this.timeSystemCorrectionType       = timeSystemCorrectionType;
        this.timeSystemCorrectionA0         = timeSystemCorrectionA0;
        this.timeSystemCorrectionA1         = timeSystemCorrectionA1;
        this.timeSystemCorrectionSecOfWeek  = timeSystemCorrectionSecOfWeek;
        this.timeSystemCorrectionWeekNumber = timeSystemCorrectionWeekNumber;
    }

    /**
     * Getter for the time system correction type.
     * @return the time system correction type
     */
    public String getTimeSystemCorrectionType() {
        return timeSystemCorrectionType;
    }

    /**
     * Getter for the A0 coefficient of the time system correction.
     * <p>
     * deltaT = {@link #getTimeSystemCorrectionA0() A0} +
     *          {@link #getTimeSystemCorrectionA1() A1} * (t - tref)
     * </p>
     * @return the A0 coefficient of the time system correction
     */
    public double getTimeSystemCorrectionA0() {
        return timeSystemCorrectionA0;
    }

    /**
     * Getter for the A1 coefficient of the time system correction.
     * <p>
     * deltaT = {@link #getTimeSystemCorrectionA0() A0} +
     *          {@link #getTimeSystemCorrectionA1() A1} * (t - tref)
     * </p>
     * @return the A1 coefficient of the time system correction
     */
    public double getTimeSystemCorrectionA1() {
        return timeSystemCorrectionA1;
    }

    /**
     * Getter for the reference time of the time system correction polynomial.
     * <p>
     * Seconds into GNSS week
     * </p>
     * @return the reference time of the time system correction polynomial
     */
    public int getTimeSystemCorrectionSecOfWeek() {
        return timeSystemCorrectionSecOfWeek;
    }

    /**
     * Getter for the reference week number of the time system correction polynomial.
     * <p>
     * Continuous number since the reference epoch of the corresponding GNSS constellation
     * </p>
     * @return the reference week number of the time system correction polynomial
     */
    public int getTimeSystemCorrectionWeekNumber() {
        return timeSystemCorrectionWeekNumber;
    }

}
