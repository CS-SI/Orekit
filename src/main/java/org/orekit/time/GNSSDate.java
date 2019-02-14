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

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.utils.Constants;

/** Container for date in GNSS form.
 * <p> This class can be used to handle {@link SatelliteSystem#GPS GPS},
 * {@link SatelliteSystem#GALILEO Galileo}, {@link SatelliteSystem#BEIDOU BeiDou}
 * and {@link SatelliteSystem#QZSS QZSS} dates. </p>
 * @author Luc Maisonobe (original code)
 * @author Bryan Cazabonne (generalization to all GNSS constellations)
 * @see AbsoluteDate
 */
public class GNSSDate implements Serializable, TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = 201902141L;

    /** Duration of a week in seconds. */
    private static final double WEEK = 7 * Constants.JULIAN_DAY;

    /** Conversion factor from seconds to milliseconds. */
    private static final double S_TO_MS = 1000.0;

    /** Week number since the GNSS reference epoch. */
    private final int weekNumber;

    /** Number of milliseconds since week start. */
    private final double milliInWeek;

    /** Satellite system to consider. */
    private final SatelliteSystem system;

    /** Corresponding date. */
    private final transient AbsoluteDate date;

    /** Build an instance corresponding to a GNSS date.
     * <p>GNSS dates are provided as a week number starting at
     * the GNSS reference epoch and as a number of milliseconds
     * since week start.</p>
     * @param weekNumber week number since the GNSS reference epoch
     * @param milliInWeek number of milliseconds since week start
     * @param system satellite system to consider
     */
    public GNSSDate(final int weekNumber, final double milliInWeek,
                    final SatelliteSystem system) {

        this.weekNumber  = weekNumber;
        this.milliInWeek = milliInWeek;
        this.system      = system;

        final int day = (int) FastMath.floor(milliInWeek / (Constants.JULIAN_DAY * S_TO_MS));
        final double secondsInDay = milliInWeek / S_TO_MS - day * Constants.JULIAN_DAY;

        TimeScale scale = null;
        DateComponents epoch = null;
        switch (system) {
            case GPS:
                scale = TimeScalesFactory.getGPS();
                epoch = DateComponents.GPS_EPOCH;
                break;
            case GALILEO:
                scale = TimeScalesFactory.getUTC();
                epoch = DateComponents.GALILEO_EPOCH;
                break;
            case QZSS:
                scale = TimeScalesFactory.getQZSS();
                epoch = DateComponents.QZSS_EPOCH;
                break;
            case BEIDOU:
                scale = TimeScalesFactory.getBDT();
                epoch = DateComponents.BEIDOU_EPOCH;
                break;
            default:
                throw new OrekitException(OrekitMessages.INVALID_SATELLITE_SYSTEM, system);
        }

        date = new AbsoluteDate(new DateComponents(epoch, weekNumber * 7 + day),
                                new TimeComponents(secondsInDay),
                                scale);

    }

    /** Build an instance from an absolute date.
     * @param date absolute date to consider
     * @param system satellite system to consider
     */
    public GNSSDate(final AbsoluteDate date, final SatelliteSystem system) {

        this.system = system;

        AbsoluteDate epoch = null;
        switch (system) {
            case GPS:
                epoch = AbsoluteDate.GPS_EPOCH;
                break;
            case GALILEO:
                epoch = AbsoluteDate.GALILEO_EPOCH;
                break;
            case QZSS:
                epoch = AbsoluteDate.QZSS_EPOCH;
                break;
            case BEIDOU:
                epoch = AbsoluteDate.BEIDOU_EPOCH;
                break;
            default:
                throw new OrekitException(OrekitMessages.INVALID_SATELLITE_SYSTEM, system);
        }

        this.weekNumber  = (int) FastMath.floor(date.durationFrom(epoch) / WEEK);
        final AbsoluteDate weekStart = new AbsoluteDate(epoch, WEEK * weekNumber);
        this.milliInWeek = date.durationFrom(weekStart) * S_TO_MS;
        this.date        = date;

    }

    /** Get the week number since the GNSS reference epoch.
     * @return week number since since the GNSS reference epoch
     */
    public int getWeekNumber() {
        return weekNumber;
    }

    /** Get the number of milliseconds since week start.
     * @return number of milliseconds since week start
     */
    public double getMilliInWeek() {
        return milliInWeek;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(weekNumber, milliInWeek, system);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 201902141L;

        /** Week number since the GNSS reference epoch. */
        private final int weekNumber;

        /** Number of milliseconds since week start. */
        private final double milliInWeek;

        /** Satellite system to consider. */
        private final SatelliteSystem system;

        /** Simple constructor.
         * @param weekNumber week number since the GNSS reference epoch
         * @param milliInWeek number of milliseconds since week start
         * @param system satellite system to consider
         */
        DataTransferObject(final int weekNumber, final double milliInWeek,
                           final SatelliteSystem system) {
            this.weekNumber  = weekNumber;
            this.milliInWeek = milliInWeek;
            this.system      = system;
        }

        /** Replace the deserialized data transfer object with a {@link GNSSDate}.
         * @return replacement {@link GNSSDate}
         */
        private Object readResolve() {
            return new GNSSDate(weekNumber, milliInWeek, system);
        }

    }

}
