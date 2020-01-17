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
import org.orekit.propagation.analytical.gnss.IRNSSOrbitalElements;
import org.orekit.time.AbsoluteDate;

/**
 * Class for IRNSS almanac.
 *
 * @see "Indian Regiona Navigation Satellite System, Signal In Space ICD
 *       for standard positioning service, version 1.1 - Table 28"
 *
 * @author Bryan Cazabonne
 * @since 10.1
 *
 */
public class IRNSSAlmanac implements IRNSSOrbitalElements {

    /** PRN number. */
    private final int prn;

    /** IRNSS week. */
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

    /** Date of aplicability. */
    private final AbsoluteDate date;

    /**
     * Constructor.
     *  @param prn the PRN number
     * @param week the GPS week
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
     * @param date of applicability corresponding to {@code toa}.
     */
    public IRNSSAlmanac(final int prn, final int week, final double toa,
                        final double sqa, final double ecc, final double inc,
                        final double om0, final double dom, final double aop,
                        final double anom, final double af0, final double af1,
                        final AbsoluteDate date) {
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
        this.date = date;
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
        return FastMath.sqrt(IRNSS_MU / absA) / absA;
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

}
