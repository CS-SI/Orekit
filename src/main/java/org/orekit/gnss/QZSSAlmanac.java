/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.gnss;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.propagation.analytical.gnss.QZSSOrbitalElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;

/**
 * This class holds a QZSS almanac as read from YUMA files.
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class QZSSAlmanac implements QZSSOrbitalElements  {

    // Fields
    /** Source of the almanac. */
    private final String src;

    /** PRN number. */
    private final int prn;

    /** Health status. */
    private final int health;

    /** QZSS week. */
    private final int week;

    /** Time of applicability. */
    private final double toa;

    /** Semi-major axis. */
    private final double sma;

    /** Eccentricity. */
    private final double ecc;

    /** Inclination. */
    private final double inc;

    /** Longitude of Orbital Plane. */
    private final double om0;

    /** Rate of Right Ascension. */
    private final double dom;

    /** Argument of perigee. */
    private final double aop;

    /** Mean anomaly. */
    private final double anom;

    /** Zeroth order clock correction. */
    private final double af0;

    /** First order clock correction. */
    private final double af1;

    /** Date of validity. */
    private final AbsoluteDate date;

    /**
     * Constructor.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param source the source of the almanac (SEM, YUMA, user defined)
     * @param prn the PRN number
     * @param week the QZSS week
     * @param toa the Time of Applicability
     * @param sqa the Square Root of Semi-Major Axis (m^1/2)
     * @param ecc the eccentricity
     * @param inc the inclination (rad)
     * @param om0 the geographic longitude of the orbital plane at the weekly epoch (rad)
     * @param dom the Rate of Right Ascension (rad/s)
     * @param aop the Argument of Perigee (rad)
     * @param anom the Mean Anomaly (rad)
     * @param af0 the Zeroth Order Clock Correction (s)
     * @param af1 the First Order Clock Correction (s/s)
     * @param health the Health status
     * @see #QZSSAlmanac(String, int, int, double, double, double, double, double, double,
     * double, double, double, double, int, AbsoluteDate)
     */
    @DefaultDataContext
    public QZSSAlmanac(final String source, final int prn,
                       final int week, final double toa,
                       final double sqa, final double ecc, final double inc,
                       final double om0, final double dom, final double aop,
                       final double anom, final double af0, final double af1,
                       final int health) {
        this(source, prn, week, toa, sqa, ecc, inc, om0, dom, aop, anom, af0, af1, health,
                new GNSSDate(week, toa * 1000., SatelliteSystem.QZSS,
                        DataContext.getDefault().getTimeScales()).getDate());
    }

    /**
     * Constructor.
     *
     * @param source the source of the almanac (SEM, YUMA, user defined)
     * @param prn the PRN number
     * @param week the QZSS week
     * @param toa the Time of Applicability
     * @param sqa the Square Root of Semi-Major Axis (m^1/2)
     * @param ecc the eccentricity
     * @param inc the inclination (rad)
     * @param om0 the geographic longitude of the orbital plane at the weekly epoch (rad)
     * @param dom the Rate of Right Ascension (rad/s)
     * @param aop the Argument of Perigee (rad)
     * @param anom the Mean Anomaly (rad)
     * @param af0 the Zeroth Order Clock Correction (s)
     * @param af1 the First Order Clock Correction (s/s)
     * @param health the Health status
     * @param date of applicability corresponding to {@code week} and {@code toa}.
     * @since 10.1
     */
    public QZSSAlmanac(final String source, final int prn,
                       final int week, final double toa,
                       final double sqa, final double ecc, final double inc,
                       final double om0, final double dom, final double aop,
                       final double anom, final double af0, final double af1,
                       final int health, final AbsoluteDate date) {
        this.src = source;
        this.prn = prn;
        this.week = week;
        this.toa = toa;
        this.sma = sqa * sqa;
        this.ecc = ecc;
        this.inc = inc;
        this.om0 = om0;
        this.dom = dom;
        this.aop = aop;
        this.anom = anom;
        this.af0 = af0;
        this.af1 = af1;
        this.health = health;
        this.date = date;
    }

    /**
     * Gets the source of this QZSS almanac.
     *
     * @return the source of this QZSS almanac
     */
    public String getSource() {
        return src;
    }

    @Override
    public int getPRN() {
        return prn;
    }

    @Override
    public int getWeek() {
        return week;
    }

    @Override
    public double getTime() {
        return toa;
    }

    @Override
    public double getSma() {
        return sma;
    }

    @Override
    public double getMeanMotion() {
        final double absA = FastMath.abs(sma);
        return FastMath.sqrt(QZSS_MU / absA) / absA;
    }

    @Override
    public double getE() {
        return ecc;
    }

    @Override
    public double getI0() {
        return inc;
    }

    @Override
    public double getIDot() {
        return 0;
    }

    @Override
    public double getOmega0() {
        return om0;
    }

    @Override
    public double getOmegaDot() {
        return dom;
    }

    @Override
    public double getPa() {
        return aop;
    }

    @Override
    public double getM0() {
        return anom;
    }

    @Override
    public double getCuc() {
        return 0;
    }

    @Override
    public double getCus() {
        return 0;
    }

    @Override
    public double getCrc() {
        return 0;
    }

    @Override
    public double getCrs() {
        return 0;
    }

    @Override
    public double getCic() {
        return 0;
    }

    @Override
    public double getCis() {
        return 0;
    }

    @Override
    public double getAf0() {
        return af0;
    }

    @Override
    public double getAf1() {
        return af1;
    }

    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /**
     * Gets the Health status.
     *
     * @return the Health status
     */
    public int getHealth() {
        return health;
    }

}
