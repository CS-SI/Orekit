/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.gnss;

import org.hipparchus.util.FastMath;
import org.orekit.propagation.analytical.gnss.GPSOrbitalElements;
import org.orekit.time.AbsoluteDate;


/**
 * This class holds a GPS almanac as read from SEM or YUMA files.
 *
 * <p>Depending on the source (SEM or YUMA), some fields may be filled in or not.
 * An almanac read from a YUMA file doesn't hold SVN number, average URA and satellite
 * configuration.</p>
 *
 * @author Pascal Parraud
 * @since 8.0
 *
 */
public class GPSAlmanac implements GPSOrbitalElements {

    // Fields
    /** Source of the almanac. */
    private final String src;
    /** PRN number. */
    private final int prn;
    /** SVN number. */
    private final int svn;
    /** Health status. */
    private final int health;
    /** Average URA. */
    private final int ura;
    /** Satellite configuration. */
    private final int config;
    /** GPS week. */
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

    /**
     * Constructor.
     *
     * @param source the source of the almanac (SEM, YUMA, user defined)
     * @param prn the PRN number
     * @param svn the SVN number
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
     * @param health the Health status
     * @param ura the average URA
     * @param config the satellite configuration
     */
    public GPSAlmanac(final String source, final int prn, final int svn,
                      final int week, final double toa,
                      final double sqa, final double ecc, final double inc,
                      final double om0, final double dom, final double aop,
                      final double anom, final double af0, final double af1,
                      final int health, final int ura, final int config) {
        this.src = source;
        this.prn = prn;
        this.svn = svn;
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
        this.ura = ura;
        this.config = config;
    }

    @Override
    public AbsoluteDate getDate() {
        return AbsoluteDate.createGPSDate(week, toa * 1000.);
    }

    /**
     * Gets the source of this GPS almanac.
     * <p>Sources can be SEM or YUMA, when the almanac is read from a file.</p>
     *
     * @return the source of this GPS almanac
     */
    public String getSource() {
        return src;
    }

    @Override
    public int getPRN() {
        return prn;
    }

    /**
     * Gets the satellite "SVN" reference number.
     *
     * @return the satellite "SVN" reference number
     */
    public int getSVN() {
        return svn;
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
        return FastMath.sqrt(GPS_MU / FastMath.pow(sma, 3));
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

    /**
     * Gets the Zeroth Order Clock Correction.
     *
     * @return the Zeroth Order Clock Correction (s)
     */
    public double getAf0() {
        return af0;
    }

    /**
     * Gets the First Order Clock Correction.
     *
     * @return the First Order Clock Correction (s/s)
     */
    public double getAf1() {
        return af1;
    }

    /**
     * Gets the Health status.
     *
     * @return the Health status
     */
    public int getHealth() {
        return health;
    }

    /**
     * Gets the average URA number.
     *
     * @return the average URA number
     */
    public int getURA() {
        return ura;
    }

    /**
     * Gets the satellite configuration.
     *
     * @return the satellite configuration
     */
    public int getSatConfiguration() {
        return config;
    }

}
