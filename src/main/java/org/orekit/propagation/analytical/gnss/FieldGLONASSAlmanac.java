/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.gnss.GLONASSAlmanac;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;

/**
 * This class holds a Field GLONASS almanac as read from .agl files.
 *
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 */
public class FieldGLONASSAlmanac<T extends RealFieldElement<T>>
    implements
    FieldGLONASSOrbitalElements<T> {

    /** Field Zero. */
    private final T zero;

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
    private final T ta;

    /** Greenwich longitude of ascending node of orbit. */
    private final T lambda;

    /** Correction to the mean value of inclination. */
    private final T deltaI;

    /** Argument of perigee. */
    private final T pa;

    /** Eccentricity. */
    private final T ecc;

    /** Correction to the mean value of Draconian period. */
    private final T deltaT;

    /** Rate of change of orbital period. */
    private final T deltaTDot;

    /** Correction from GLONASS to UTC. */
    private final T tGlo2UTC;

    /** Correction to GPS time relative GLONASS. */
    private final T tGPS2Glo;

    /** Correction of time relative to GLONASS system time. */
    private final T tGlo;

    /** GLONASS time scale. */
    private final TimeScale glonass;

    /**
     * Constructor.
     * <p>
     * This method uses the {@link DataContext#getDefault() default data
     * context}.
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
     * @see #GLONASSAlmanac(int, int, int, int, int, T, T, T, T, T, T, T, T, T,
     *      T, TimeScale)
     */
    @DefaultDataContext
    public FieldGLONASSAlmanac(final int channel, final int health,
                               final int day, final int month, final int year,
                               final T ta, final T lambda, final T deltaI,
                               final T pa, final T ecc, final T deltaT,
                               final T deltaTDot, final T tGlo2UTC,
                               final T tGPS2Glo, final T tGlo) {
        this(channel, health, day, month, year, ta, lambda, deltaI, pa, ecc,
             deltaT, deltaTDot, tGlo2UTC, tGPS2Glo, tGlo,
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
    public FieldGLONASSAlmanac(final int channel, final int health,
                               final int day, final int month, final int year,
                               final T ta, final T lambda, final T deltaI,
                               final T pa, final T ecc, final T deltaT,
                               final T deltaTDot, final T tGlo2UTC,
                               final T tGPS2Glo, final T tGlo,
                               final TimeScale glonass) {
        this.zero = ta.getField().getZero();
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
     * Constructor This constructor converts a GLONASSAlmanac into a
     * FieldGLONASSAlmanac.
     *
     * @param field
     * @param almanac
     */
    public FieldGLONASSAlmanac(final Field<T> field, final GLONASSAlmanac almanac) {
        this.zero = field.getZero();
        this.channel = almanac.getFrequencyChannel();
        this.health = almanac.getHealth();
        this.day = almanac.getDay();
        this.month = almanac.getMonth();
        this.year = almanac.getYear();
        this.ta = zero.add(almanac.getTime());
        this.lambda = zero.add(almanac.getLambda());
        this.deltaI = zero.add(almanac.getDeltaI());
        this.pa = zero.add(almanac.getPa());
        this.ecc = zero.add(almanac.getE());
        this.deltaT = zero.add(almanac.getDeltaT());
        this.deltaTDot = zero.add(almanac.getDeltaTDot());
        this.tGlo2UTC = zero.add(almanac.getGlo2UTC());
        this.tGPS2Glo = zero.add(almanac.getGPS2Glo());
        this.tGlo = zero.add(almanac.getGloOffset());
        this.glonass = almanac.getGlonass();
    }

    @Override
    public FieldAbsoluteDate<T> getDate() {
        final DateComponents date = new DateComponents(year, month, day);
        final TimeComponents time = new TimeComponents(ta.getReal());
        return new FieldAbsoluteDate<>(ta.getField(), date, time, glonass);
    }

    @Override
    public T getTime() {
        return ta;
    }

    @Override
    public T getLambda() {
        return lambda;
    }

    @Override
    public T getE() {
        return ecc;
    }

    @Override
    public T getPa() {
        return pa;
    }

    @Override
    public T getDeltaI() {
        return deltaI;
    }

    @Override
    public T getDeltaT() {
        return deltaT;
    }

    @Override
    public T getDeltaTDot() {
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
    public T getGlo2UTC() {
        return tGlo2UTC;
    }

    /**
     * Get the correction to GPS time relative GLONASS.
     *
     * @return the to GPS time relative GLONASS (s)
     */
    public T getGPS2Glo() {
        return tGPS2Glo;
    }

    /**
     * Get the correction of time relative to GLONASS system time.
     *
     * @return the correction of time relative to GLONASS system time (s)
     */
    public T getGloOffset() {
        return tGlo;
    }

    @Override
    public int getNa() {
        final FieldGLONASSDate<T> gloDate =
            new FieldGLONASSDate<>(zero.getField(), getDate(), glonass);
        return gloDate.getDayNumber();
    }

    @Override
    public int getN4() {
        final FieldGLONASSDate<T> gloDate =
            new FieldGLONASSDate<>(zero.getField(), getDate(), glonass);
        return gloDate.getIntervalNumber();
    }

    @Override
    public T getGammaN() {
        return zero;
    }

    @Override
    public T getTN() {
        return zero;
    }

    @Override
    public T getXDot() {
        return zero;
    }

    @Override
    public T getX() {
        return zero;
    }

    @Override
    public T getXDotDot() {
        return zero;
    }

    @Override
    public T getYDot() {
        return zero;
    }

    @Override
    public T getY() {
        return zero;
    }

    @Override
    public T getYDotDot() {
        return zero;
    }

    @Override
    public T getZDot() {
        return zero;
    }

    @Override
    public T getZ() {
        return zero;
    }

    @Override
    public T getZDotDot() {
        return zero;
    }

    @Override
    public int getIOD() {
        return 0;
    }

}
