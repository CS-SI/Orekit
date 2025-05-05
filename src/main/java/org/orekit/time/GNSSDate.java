/* Copyright 2002-2025 CS GROUP
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.EOPEntry;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

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
    private static final long serialVersionUID = 20221228L;

    /** Duration of a week in days. */
    private static final int WEEK_D = 7;

    /** Duration of a week in seconds. */
    private static final double WEEK_S = WEEK_D * Constants.JULIAN_DAY;

    /** Reference date for ensuring continuity across GNSS week rollover.
     * @since 9.3.1
     */
    private static final AtomicReference<DateComponents> ROLLOVER_REFERENCE = new AtomicReference<>(null);

    /** Week number since the GNSS reference epoch. */
    private final int weekNumber;

    /** Number of seconds since week start. */
    private final TimeOffset secondsInWeek;

    /** Corresponding date. */
    private final transient AbsoluteDate date;

    /** Build an instance corresponding to a GNSS date.
     * <p>
     * GNSS dates are provided as a week number starting at
     * the GNSS reference epoch and as a number of seconds
     * since week start.
     * </p>
     * <p>
     * Many interfaces provide week number modulo the constellation week cycle. In order to cope with
     * this, when the week number is smaller than the week cycle, this constructor assumes a modulo operation
     * has been performed and it will fix the week number according to the reference date set up for
     * handling rollover (see {@link #setRolloverReference(DateComponents) setRolloverReference(reference)}).
     * If the week number is equal to the week cycle or larger, it will be used without any correction.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param weekNumber week number
     * @param secondsInWeek number of seconds since week start
     * @param system satellite system to consider
     * @see #GNSSDate(int, double, SatelliteSystem, TimeScales)
     * @since 12.0
     */
    @DefaultDataContext
    public GNSSDate(final int weekNumber, final double secondsInWeek, final SatelliteSystem system) {
        this(weekNumber, new TimeOffset(secondsInWeek), system, DataContext.getDefault().getTimeScales());
    }

    /** Build an instance corresponding to a GNSS date.
     * <p>
     * GNSS dates are provided as a week number starting at
     * the GNSS reference epoch and as a number of seconds
     * since week start.
     * </p>
     * <p>
     * Many interfaces provide week number modulo the constellation week cycle. In order to cope with
     * this, when the week number is smaller than the week cycle, this constructor assumes a modulo operation
     * has been performed and it will fix the week number according to the reference date set up for
     * handling rollover (see {@link #setRolloverReference(DateComponents) setRolloverReference(reference)}).
     * If the week number is equal to the week cycle or larger, it will be used without any correction.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param weekNumber week number
     * @param secondsInWeek number of seconds since week start
     * @param system satellite system to consider
     * @see #GNSSDate(int, double, SatelliteSystem, TimeScales)
     * @since 13.0
     */
    @DefaultDataContext
    public GNSSDate(final int weekNumber, final TimeOffset secondsInWeek, final SatelliteSystem system) {
        this(weekNumber, secondsInWeek, system, DataContext.getDefault().getTimeScales());
    }

    /**
     * Build an instance corresponding to a GNSS date.
     * <p>
     * GNSS dates are provided as a week number starting at the GNSS reference epoch and
     * as a number of seconds since week start.
     * </p>
     * <p>
     * Many interfaces provide week number modulo the constellation week cycle. In order
     * to cope with this, when the week number is smaller than the week cycle, this
     * constructor assumes a modulo operation has been performed and it will fix the week
     * number according to the reference date set up for handling rollover (see {@link
     * #setRolloverReference(DateComponents) setRolloverReference(reference)}). If the
     * week number is equal to the week cycle or larger, it will be used without any
     * correction.
     * </p>
     *
     * @param weekNumber    week number
     * @param secondsInWeek number of seconds since week start
     * @param system        satellite system to consider
     * @param timeScales    the set of time scales. Used to retrieve the appropriate time
     *                      scale for the given {@code system}.
     * @since 12.0
     */
    public GNSSDate(final int weekNumber, final double secondsInWeek,
                    final SatelliteSystem system, final TimeScales timeScales) {
        this(weekNumber, new TimeOffset(secondsInWeek), system, timeScales);
    }

    /**
     * Build an instance corresponding to a GNSS date.
     * <p>
     * GNSS dates are provided as a week number starting at the GNSS reference epoch and
     * as a number of seconds since week start.
     * </p>
     * <p>
     * Many interfaces provide week number modulo the constellation week cycle. In order
     * to cope with this, when the week number is smaller than the week cycle, this
     * constructor assumes a modulo operation has been performed and it will fix the week
     * number according to the reference date set up for handling rollover (see {@link
     * #setRolloverReference(DateComponents) setRolloverReference(reference)}). If the
     * week number is equal to the week cycle or larger, it will be used without any
     * correction.
     * </p>
     *
     * @param weekNumber    week number
     * @param secondsInWeek number of seconds since week start
     * @param system        satellite system to consider
     * @param timeScales    the set of time scales. Used to retrieve the appropriate time
     *                      scale for the given {@code system}.
     * @since 13.0
     */
    public GNSSDate(final int weekNumber, final TimeOffset secondsInWeek,
                    final SatelliteSystem system, final TimeScales timeScales) {

        final int day = (int) (secondsInWeek.getSeconds() / TimeOffset.DAY.getSeconds());
        final TimeOffset secondsInDay = new TimeOffset(secondsInWeek.getSeconds() % TimeOffset.DAY.getSeconds(),
                                                       secondsInWeek.getAttoSeconds());

        int w = weekNumber;
        DateComponents dc = new DateComponents(getWeekReferenceDateComponents(system), weekNumber * 7 + day);
        final int cycleW = GNSSDateType.getRollOverWeek(system);
        if (weekNumber < cycleW) {

            DateComponents reference = ROLLOVER_REFERENCE.get();
            if (reference == null) {
                // lazy setting of a default reference, using end of EOP entries
                final UT1Scale       ut1       = timeScales.getUT1(IERSConventions.IERS_2010, true);
                final List<EOPEntry> eop       = ut1.getEOPHistory().getEntries();
                final int            lastMJD   = eop.get(eop.size() - 1).getMjd();
                reference = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, lastMJD);
                ROLLOVER_REFERENCE.compareAndSet(null, reference);
            }

            // fix GNSS week rollover
            final int cycleD = WEEK_D * cycleW;
            while (dc.getJ2000Day() < reference.getJ2000Day() - cycleD / 2) {
                dc = new DateComponents(dc, cycleD);
                w += cycleW;
            }

        }

        this.weekNumber    = w;
        this.secondsInWeek = secondsInWeek;

        date = new AbsoluteDate(dc, new TimeComponents(secondsInDay), getTimeScale(system, timeScales));

    }

    /**
     * Build an instance corresponding to a GNSS date.
     * <p>
     * GNSS dates are provided as a week number starting at the GNSS reference epoch and
     * as a number of seconds since week start.
     * </p>
     *
     * @param weekNumber    week number
     * @param secondsInWeek number of seconds since week start
     * @param system        satellite system to consider
     * @param reference     reference date for rollover, the generated date will be less
     *                      than one half cycle from this date
     * @param timeScales    the set of time scales. Used to retrieve the appropriate time
     *                      scale for the given {@code system}.
     * @since 12.0
     */
    public GNSSDate(final int weekNumber, final double secondsInWeek,
                    final SatelliteSystem system, final DateComponents reference,
                    final TimeScales timeScales) {
        this(weekNumber, new TimeOffset(secondsInWeek), system, reference, timeScales);
    }

    /**
     * Build an instance corresponding to a GNSS date.
     * <p>
     * GNSS dates are provided as a week number starting at the GNSS reference epoch and
     * as a number of seconds since week start.
     * </p>
     *
     * @param weekNumber    week number
     * @param secondsInWeek number of seconds since week start
     * @param system        satellite system to consider
     * @param reference     reference date for rollover, the generated date will be less
     *                      than one half cycle from this date
     * @param timeScales    the set of time scales. Used to retrieve the appropriate time
     *                      scale for the given {@code system}.
     * @since 13.0
     */
    public GNSSDate(final int weekNumber, final TimeOffset secondsInWeek,
                    final SatelliteSystem system, final DateComponents reference,
                    final TimeScales timeScales) {

        final int day = (int) (secondsInWeek.getSeconds() / TimeOffset.DAY.getSeconds());
        final TimeOffset secondsInDay = new TimeOffset(secondsInWeek.getSeconds() % TimeOffset.DAY.getSeconds(),
                                                       secondsInWeek.getAttoSeconds());

        int w = weekNumber;
        DateComponents dc = new DateComponents(getWeekReferenceDateComponents(system), weekNumber * 7 + day);
        final int cycleW = GNSSDateType.getRollOverWeek(system);
        if (weekNumber < cycleW) {

            // fix GNSS week rollover
            final int cycleD = WEEK_D * cycleW;
            while (dc.getJ2000Day() < reference.getJ2000Day() - cycleD / 2) {
                dc = new DateComponents(dc, cycleD);
                w += cycleW;
            }

        }

        this.weekNumber    = w;
        this.secondsInWeek = secondsInWeek;

        date = new AbsoluteDate(dc, new TimeComponents(secondsInDay), getTimeScale(system, timeScales));

    }

    /** Build an instance from an absolute date.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param date absolute date to consider
     * @param system satellite system to consider
     * @see #GNSSDate(AbsoluteDate, SatelliteSystem, TimeScales)
     */
    @DefaultDataContext
    public GNSSDate(final AbsoluteDate date, final SatelliteSystem system) {
        this(date, system, DataContext.getDefault().getTimeScales());
    }

    /**
     * Build an instance from an absolute date.
     *
     * @param date       absolute date to consider
     * @param system     satellite system to consider
     * @param timeScales the set of time scales. Used to retrieve the appropriate time
     *                   scale for the given {@code system}.
     * @since 10.1
     */
    public GNSSDate(final AbsoluteDate date,
                    final SatelliteSystem system,
                    final TimeScales timeScales) {

        final AbsoluteDate epoch = getWeekReferenceAbsoluteDate(system, timeScales);
        this.weekNumber  = (int) FastMath.floor(date.durationFrom(epoch) / WEEK_S);
        final AbsoluteDate weekStart = new AbsoluteDate(epoch, WEEK_S * weekNumber);
        this.secondsInWeek = date.accurateDurationFrom(weekStart);
        this.date          = date;

    }

    /** Set a reference date for ensuring continuity across GNSS week rollover.
     * <p>
     * Instance created using the {@link #GNSSDate(int, double, SatelliteSystem) GNSSDate(weekNumber, secondsInWeek, system)}
     * constructor and with a week number between 0 and the constellation week cycle (cycleW) after this method has been called will
     * fix the week number to ensure they correspond to dates between {@code reference - cycleW / 2 weeks}
     * and {@code reference + cycleW / 2 weeks}.
     * </p>
     * <p>
     * If this method is never called, a default reference date for rollover will be set using
     * the date of the last known EOP entry retrieved from {@link UT1Scale#getEOPHistory() UT1}
     * time scale.
     * </p>
     * @param reference reference date for GNSS week rollover
     * @see #getRolloverReference()
     * @see #GNSSDate(int, double, SatelliteSystem)
     * @since 9.3.1
     */
    public static void setRolloverReference(final DateComponents reference) {
        ROLLOVER_REFERENCE.set(reference);
    }

    /** Get the reference date ensuring continuity across GNSS week rollover.
     * @return reference reference date for GNSS week rollover
     * @see #setRolloverReference(DateComponents)
     * @see #GNSSDate(int, double, SatelliteSystem)
     * @since 9.3.1
     */
    public static DateComponents getRolloverReference() {
        return ROLLOVER_REFERENCE.get();
    }

    /** Get the week number since the GNSS reference epoch.
     * <p>
     * The week number returned here has been fixed for GNSS week rollover, i.e.
     * it may be larger than the corresponding week cycle of the constellation.
     * </p>
     * @return week number since the GNSS reference epoch
     */
    public int getWeekNumber() {
        return weekNumber;
    }

    /** Get the number of milliseconds since week start.
     * @return number of milliseconds since week start
     */
    public double getMilliInWeek() {
        return getSecondsInWeek() * 1000.0;
    }

    /** Get the number of seconds since week start.
     * @return number of seconds since week start
     * @since 12.0
     */
    public double getSecondsInWeek() {
        return getSplitSecondsInWeek().toDouble();
    }

    /** Get the number of seconds since week start.
     * @return number of seconds since week start
     * @since 13.0
     */
    public TimeOffset getSplitSecondsInWeek() {
        return secondsInWeek;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the time scale related to the given satellite system.
     * @param satellite satellite system
     * @param timeScales set of time scales.
     * @return the time scale
     */
    private TimeScale getTimeScale(final SatelliteSystem satellite,
                                   final TimeScales timeScales) {
        switch (satellite) {
            case GPS     : return timeScales.getGPS();
            case GALILEO : return timeScales.getGST();
            case QZSS    : return timeScales.getQZSS();
            case BEIDOU  : return timeScales.getBDT();
            case NAVIC   : return timeScales.getNavIC();
            case SBAS    : return timeScales.getGPS();
            default      : throw new OrekitException(OrekitMessages.INVALID_SATELLITE_SYSTEM, satellite);
        }
    }

    /** Get the reference epoch of the week number for the given satellite system.
     * <p> Returned parameter is an AbsoluteDate. </p>
     * @param satellite satellite system
     * @param timeScales set of time scales.
     * @return the reference epoch
     */
    private AbsoluteDate getWeekReferenceAbsoluteDate(final SatelliteSystem satellite,
                                                      final TimeScales timeScales) {
        switch (satellite) {
            case GPS     : return timeScales.getGpsEpoch();
            case GALILEO : return timeScales.getGalileoEpoch();
            case QZSS    : return timeScales.getQzssEpoch();
            case BEIDOU  : return timeScales.getBeidouEpoch();
            case NAVIC   : return timeScales.getNavicEpoch();
            case SBAS    : return timeScales.getGpsEpoch();
            default      : throw new OrekitException(OrekitMessages.INVALID_SATELLITE_SYSTEM, satellite);
        }
    }

    /** Get the reference epoch of the week number for the given satellite system.
     * <p> Returned parameter is a DateComponents. </p>
     * @param satellite satellite system
     * @return the reference epoch
     */
    private DateComponents getWeekReferenceDateComponents(final SatelliteSystem satellite) {
        switch (satellite) {
            case GPS     : return DateComponents.GPS_EPOCH;
            case GALILEO : return DateComponents.GALILEO_EPOCH;
            case QZSS    : return DateComponents.QZSS_EPOCH;
            case BEIDOU  : return DateComponents.BEIDOU_EPOCH;
            case NAVIC   : return DateComponents.NAVIC_EPOCH;
            case SBAS    : return DateComponents.GPS_EPOCH;
            default      : throw new OrekitException(OrekitMessages.INVALID_SATELLITE_SYSTEM, satellite);
        }
    }

    /** Enumerate for GNSS data. */
    public enum GNSSDateType {

        /** GPS. */
        GPS(SatelliteSystem.GPS, 1024),

        /** Galileo. */
        GALILEO(SatelliteSystem.GALILEO, 4096),

        /** QZSS. */
        QZSS(SatelliteSystem.QZSS, 1024),

        /** BeiDou. */
        BEIDOU(SatelliteSystem.BEIDOU, 8192),

        /** NavIC. */
        NAVIC(SatelliteSystem.NAVIC, 1024),

        /** SBAS. */
        SBAS(SatelliteSystem.SBAS, 1024);

        /** Map for the number of week in one GNSS rollover cycle. */
        private static final Map<SatelliteSystem, Integer> CYCLE_MAP = new HashMap<>();
        static {
            for (final GNSSDateType type : values()) {
                final int             val       = type.getRollOverCycle();
                final SatelliteSystem satellite = type.getSatelliteSystem();
                CYCLE_MAP.put(satellite, val);
            }
        }

        /** Number of week in one rollover cycle. */
        private final int numberOfWeek;

        /** Satellite system. */
        private final SatelliteSystem satelliteSystem;

        /**
         * Build a new instance.
         *
         * @param system satellite system
         * @param rollover number of week in one rollover cycle
         */
        GNSSDateType(final SatelliteSystem system, final int rollover) {
            this.satelliteSystem = system;
            this.numberOfWeek    = rollover;
        }

        /** Get the number of week in one rollover cycle.
         * @return  the number of week in one rollover cycle
         */
        public int getRollOverCycle() {
            return numberOfWeek;
        }

        /** Get the satellite system.
         * @return the satellite system
         */
        public SatelliteSystem getSatelliteSystem() {
            return satelliteSystem;
        }

        /** Get the number of week in one rollover cycle for the given satellite system.
         *
         * @param satellite satellite system
         * @return the number of week in one rollover cycle for the given satellite system
         */
        public static int getRollOverWeek(final SatelliteSystem satellite) {
            return CYCLE_MAP.get(satellite);
        }

    }
}
