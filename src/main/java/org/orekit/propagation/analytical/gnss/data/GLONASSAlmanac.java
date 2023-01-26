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
package org.orekit.propagation.analytical.gnss.data;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.propagation.analytical.gnss.GLONASSAnalyticalPropagator;
import org.orekit.propagation.analytical.gnss.GLONASSAnalyticalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.GLONASSDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.IERSConventions;

/**
 * This class holds a GLONASS almanac as read from .agl files.
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class GLONASSAlmanac implements GLONASSOrbitalElements {

    /** Frequency channel (-7...6). */
    private final int channel;

    /** Health status. */
    private final int health;

    /** Day of Almanac. */
    private final int day;

    /** Month of Almanac. */
    private final int month;

    /** Year of Almanac. */
    private final int year;

    /** Reference time of the almanac. */
    private final double ta;

    /** Greenwich longitude of ascending node of orbit. */
    private final double lambda;

    /** Correction to the mean value of inclination. */
    private final double deltaI;

    /** Argument of perigee. */
    private final double pa;

    /** Eccentricity. */
    private final double ecc;

    /** Correction to the mean value of Draconian period. */
    private final double deltaT;

    /** Rate of change of orbital period. */
    private final double deltaTDot;

    /** Correction from GLONASS to UTC. */
    private final double tGlo2UTC;

    /** Correction to GPS time relative GLONASS. */
    private final double tGPS2Glo;

    /**  Correction of time relative to GLONASS system time. */
    private final double tGlo;

    /** GLONASS time scale. */
    private final TimeScale glonass;

    /**
     * Constructor.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param channel the frequency channel from -7 to 6)
     * @param health the Health status
     * @param day the day of Almanac
     * @param month the month of Almanac
     * @param year the year of Almanac
     * @param ta the reference time of the almanac (s)
     * @param lambda the Greenwich longitude of ascending node of orbit (rad)
     * @param deltaI the correction to the mean value of inclination (rad)
     * @param pa the argument of perigee (rad)
     * @param ecc the eccentricity
     * @param deltaT the correction to the mean value of Draconian period (s)
     * @param deltaTDot the rate of change of orbital period
     * @param tGlo2UTC the correction from GLONASS to UTC (s)
     * @param tGPS2Glo the correction to GPS time relative GLONASS (s)
     * @param tGlo the correction of time relative to GLONASS system time (s)
     * @see #GLONASSAlmanac(int, int, int, int, int, double, double, double, double,
     * double, double, double, double, double, double, TimeScale)
     */
    @DefaultDataContext
    public GLONASSAlmanac(final int channel, final int health,
                          final int day, final int month, final int year,
                          final double ta, final double lambda,
                          final double deltaI, final double pa,
                          final double ecc, final double deltaT, final double deltaTDot,
                          final double tGlo2UTC, final double tGPS2Glo, final double tGlo) {
        this(channel, health, day, month, year, ta, lambda, deltaI, pa, ecc, deltaT,
                deltaTDot, tGlo2UTC, tGPS2Glo, tGlo,
                DataContext.getDefault().getTimeScales().getGLONASS());
    }

    /**
     * Constructor.
     *
     * @param channel the frequency channel from -7 to 6)
     * @param health the Health status
     * @param day the day of Almanac
     * @param month the month of Almanac
     * @param year the year of Almanac
     * @param ta the reference time of the almanac (s)
     * @param lambda the Greenwich longitude of ascending node of orbit (rad)
     * @param deltaI the correction to the mean value of inclination (rad)
     * @param pa the argument of perigee (rad)
     * @param ecc the eccentricity
     * @param deltaT the correction to the mean value of Draconian period (s)
     * @param deltaTDot the rate of change of orbital period
     * @param tGlo2UTC the correction from GLONASS to UTC (s)
     * @param tGPS2Glo the correction to GPS time relative GLONASS (s)
     * @param tGlo the correction of time relative to GLONASS system time (s)
     * @param glonass GLONASS time scale.
     * @since 10.1
     */
    public GLONASSAlmanac(final int channel, final int health,
                          final int day, final int month, final int year,
                          final double ta, final double lambda,
                          final double deltaI, final double pa,
                          final double ecc, final double deltaT, final double deltaTDot,
                          final double tGlo2UTC, final double tGPS2Glo, final double tGlo,
                          final TimeScale glonass) {
        this.channel = channel;
        this.health = health;
        this.day = day;
        this.month = month;
        this.year = year;
        this.ta = ta;
        this.lambda = lambda;
        this.deltaI = deltaI;
        this.pa = pa;
        this.ecc = ecc;
        this.deltaT = deltaT;
        this.deltaTDot = deltaTDot;
        this.tGlo2UTC = tGlo2UTC;
        this.tGPS2Glo = tGPS2Glo;
        this.tGlo = tGlo;
        this.glonass = glonass;
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * <p>
     * The attitude provider is set by default to be aligned with the EME2000 frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The data context is by default to the
     *  {@link DataContext#getDefault() default data context}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
     *  context.<br>
     * The ECEF frame is set by default to the
     *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     *  CIO/2010-based ITRF simple EOP} in the default data context.
     * </p>
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(DataContext)
     * @see #getPropagator(DataContext, AttitudeProvider, Frame, Frame, double)
     * @since 12.0
     */
    @DefaultDataContext
    public GLONASSAnalyticalPropagator getPropagator() {
        return new GLONASSAnalyticalPropagatorBuilder(this).build();
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * <p>
     * The attitude provider is set by default to be aligned with the EME2000 frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link Frames#getEME2000() EME2000 frame}.<br>
     * The ECEF frame is set by default to the
     *  {@link Frames#getITRF(IERSConventions, boolean) CIO/2010-based ITRF simple
     *  EOP}.
     * </p>
     * @param context the data context to use for frames and time scales.
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator()
     * @see #getPropagator(DataContext, AttitudeProvider, Frame, Frame, double)
     * @since 12.0
     */
    public GLONASSAnalyticalPropagator getPropagator(final DataContext context) {
        return new GLONASSAnalyticalPropagatorBuilder(this, context).build();
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * @param context the data context to use for frames and time scales.
     * @param provider attitude provider
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     * @param mass spacecraft mass in kg
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator()
     * @see #getPropagator(DataContext)
     * @since 12.0
     */
    public GLONASSAnalyticalPropagator getPropagator(final DataContext context, final AttitudeProvider provider,
                                                     final Frame inertial, final Frame bodyFixed,
                                                     final double mass) {
        return new GLONASSAnalyticalPropagatorBuilder(this, context).attitudeProvider(provider)
                                                                    .eci(inertial)
                                                                    .ecef(bodyFixed)
                                                                    .mass(mass)
                                                                    .build();
    }

    @Override
    public AbsoluteDate getDate() {
        final DateComponents date = new DateComponents(year, month, day);
        final TimeComponents time = new TimeComponents(ta);
        return new AbsoluteDate(date, time, glonass);
    }

    @Override
    public double getTime() {
        return ta;
    }

    @Override
    public double getLambda() {
        return lambda;
    }

    @Override
    public double getE() {
        return ecc;
    }

    @Override
    public double getPa() {
        return pa;
    }

    @Override
    public double getDeltaI() {
        return deltaI;
    }

    @Override
    public double getDeltaT() {
        return deltaT;
    }

    @Override
    public double getDeltaTDot() {
        return deltaTDot;
    }

    /**
     * Get the Health status.
     *
     * @return the Health status
     */
    public int getHealth() {
        return health;
    }

    /**
     * Get the frequency channel.
     *
     * @return the frequency channel
     */
    public int getFrequencyChannel() {
        return channel;
    }

    /**
     * Get the correction from GLONASS to UTC.
     *
     * @return the correction from GLONASS to UTC (s)
     */
    public double getGlo2UTC() {
        return tGlo2UTC;
    }

    /**
     * Get the correction to GPS time relative GLONASS.
     *
     * @return the to GPS time relative GLONASS (s)
     */
    public double getGPS2Glo() {
        return tGPS2Glo;
    }

    /**
     * Get the correction of time relative to GLONASS system time.
     *
     * @return the correction of time relative to GLONASS system time (s)
     */
    public double getGloOffset() {
        return tGlo;
    }

    @Override
    public int getNa() {
        final GLONASSDate gloDate = new GLONASSDate(getDate(), glonass);
        return gloDate.getDayNumber();
    }

    @Override
    public int getN4() {
        final GLONASSDate gloDate = new GLONASSDate(getDate(), glonass);
        return gloDate.getIntervalNumber();
    }
}
