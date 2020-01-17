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
import org.orekit.propagation.analytical.gnss.GalileoOrbitalElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;

/**
 * Class for Galileo almanac.
 *
 * @see "European GNSS (Galileo) Open Service, Signal In Space,
 *      Interface Control Document, Table 75"
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class GalileoAlmanac implements GalileoOrbitalElements {

    // Nominal parameters
    /** Nominal inclination (Ref: Galileo ICD - Table 75). */
    private static final double I0 = FastMath.toRadians(56.0);

    /** Nominal semi-major axis in meters (Ref: Galileo ICD - Table 75). */
    private static final double A0 = 29600000;

    /** PRN number. */
    private final int prn;

    /** Satellite E5a signal health status. */
    private final int healthE5a;

    /** Satellite E5b signal health status. */
    private final int healthE5b;

    /** Satellite E1-B/C signal health status. */
    private final int healthE1;

    /** Galileo week. */
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

    /** Almanac Issue Of Data. */
    private final int iod;

    /** Date of validity. */
    private final AbsoluteDate date;

    /**
     * Build a new almanac.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param prn the PRN number
     * @param week the Galileo week
     * @param toa the Almanac Time of Applicability (s)
     * @param dsqa difference between the square root of the semi-major axis
     *        and the square root of the nominal semi-major axis
     * @param ecc the eccentricity
     * @param dinc the correction of orbit reference inclination at reference time (rad)
     * @param iod the issue of data
     * @param om0 the geographic longitude of the orbital plane at the weekly epoch (rad)
     * @param dom the Rate of Right Ascension (rad/s)
     * @param aop the Argument of Perigee (rad)
     * @param anom the Mean Anomaly (rad)
     * @param af0 the Zeroth Order Clock Correction (s)
     * @param af1 the First Order Clock Correction (s/s)
     * @param healthE5a the E5a signal health status
     * @param healthE5b the E5b signal health status
     * @param healthE1 the E1-B/C signal health status
     * @see #GalileoAlmanac(int, int, double, double, double, double, int, double, double,
     * double, double, double, double, int, int, int, AbsoluteDate)
     */
    @DefaultDataContext
    public GalileoAlmanac(final int prn, final int week, final double toa,
                          final double dsqa, final double ecc, final double dinc,
                          final int iod, final double om0, final double dom,
                          final double aop, final double anom, final double af0,
                          final double af1, final int healthE5a, final int healthE5b,
                          final int healthE1) {
        this(prn, week, toa, dsqa, ecc, dinc, iod, om0, dom, aop, anom, af0, af1,
                healthE5a, healthE5b, healthE1,
                new GNSSDate(week, toa * 1000., SatelliteSystem.GALILEO,
                        DataContext.getDefault().getTimeScales()).getDate());
    }

    /**
     * Build a new almanac.
     *
     * @param prn the PRN number
     * @param week the Galileo week
     * @param toa the Almanac Time of Applicability (s)
     * @param dsqa difference between the square root of the semi-major axis
     *        and the square root of the nominal semi-major axis
     * @param ecc the eccentricity
     * @param dinc the correction of orbit reference inclination at reference time (rad)
     * @param iod the issue of data
     * @param om0 the geographic longitude of the orbital plane at the weekly epoch (rad)
     * @param dom the Rate of Right Ascension (rad/s)
     * @param aop the Argument of Perigee (rad)
     * @param anom the Mean Anomaly (rad)
     * @param af0 the Zeroth Order Clock Correction (s)
     * @param af1 the First Order Clock Correction (s/s)
     * @param healthE5a the E5a signal health status
     * @param healthE5b the E5b signal health status
     * @param healthE1 the E1-B/C signal health status
     * @param date corresponding to {@code week} and {@code toa}.
     * @since 10.1
     */
    public GalileoAlmanac(final int prn, final int week, final double toa,
                          final double dsqa, final double ecc, final double dinc,
                          final int iod, final double om0, final double dom,
                          final double aop, final double anom, final double af0,
                          final double af1, final int healthE5a, final int healthE5b,
                          final int healthE1, final AbsoluteDate date) {
        this.prn = prn;
        this.week = week;
        this.toa = toa;
        this.ecc = ecc;
        this.inc = I0 + dinc;
        this.iod = iod;
        this.om0 = om0;
        this.dom = dom;
        this.aop = aop;
        this.anom = anom;
        this.af0 = af0;
        this.af1 = af1;
        this.healthE1 = healthE1;
        this.healthE5a = healthE5a;
        this.healthE5b = healthE5b;
        this.date = date;

        // semi-major axis computation
        final double sqa = dsqa + FastMath.sqrt(A0);
        this.sma = sqa * sqa;
    }

    @Override
    public AbsoluteDate getDate() {
        return date;
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
        return FastMath.sqrt(GALILEO_MU / absA) / absA;
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

    /** Get the Issue of Data (IOD).
     * @return the Issue Of Data
     */
    public int getIOD() {
        return iod;
    }

    /**
     * Gets the E1-B/C signal health status.
     *
     * @return the E1-B/C signal health status
     */
    public int getHealthE1() {
        return healthE1;
    }

    /**
     * Gets the E5a signal health status.
     *
     * @return the E5a signal health status
     */
    public int getHealthE5a() {
        return healthE5a;
    }
    /**
     * Gets the E5b signal health status.
     *
     * @return the E5b signal health status
     */
    public int getHealthE5b() {
        return healthE5b;
    }

}
