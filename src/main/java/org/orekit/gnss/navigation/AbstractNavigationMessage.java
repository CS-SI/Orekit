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
package org.orekit.gnss.navigation;

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;

/**
 * Base class for GNSS navigation messages.
 * @author Bryan Cazabonne
 * @since 11.0
 *
 * @see GPSNavigationMessage
 * @see GalileoNavigationMessage
 * @see BeidouNavigationMessage
 * @see QZSSNavigationMessage
 * @see IRNSSNavigationMessage
 */
public abstract class AbstractNavigationMessage {

    /** Ephemeris reference epoch. */
    private AbsoluteDate date;

    /** PRN number of the satellite. */
    private int prn;

    /** Reference Week of the orbit. */
    private int week;

    /** Square Root of Semi-Major Axis (m^1/2). */
    private double sqrtA;

    /** Mean Motion Difference from Computed Value. */
    private double deltaN;

    /** Longitude of Ascending Node of Orbit Plane at Weekly Epoch (rad). */
    private double om0;

    /** Rate of Right Ascension (rad/s). */
    private double dom;

    /** Argument of Perigee (rad). */
    private double aop;

    /** Mean Anomaly at Reference Time (rad). */
    private double anom;

    /** Eccentricity. */
    private double ecc;

    /** Inclination Angle at Reference Time (rad). */
    private double i0;

    /** Rate of Inclination Angle (rad/s). */
    private double iDot;

    /** Time of Ephemeris (sec of GNSS week). */
    private double toe;

    /** SV Clock Bias Correction Coefficient (s). */
    private double af0;

    /** SV Clock Drift Correction Coefficient (s/s). */
    private double af1;

    /** Drift Rate Correction Coefficient (s/s²). */
    private double af2;

    /** Time of clock epoch. */
    private AbsoluteDate epochToc;

    /** Amplitude of Cosine Harmonic Correction Term to the Argument of Latitude. */
    private double cuc;

    /** Amplitude of Sine Harmonic Correction Term to the Argument of Latitude. */
    private double cus;

    /** Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius. */
    private double crc;

    /** Amplitude of the Sine Correction Term to the Orbit Radius. */
    private double crs;

    /** Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination. */
    private double cic;

    /** Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination. */
    private double cis;

    /** Earth's universal gravitational parameter. */
    private final double mu;

    /**
     * Constructor.
     * @param mu Earth's universal gravitational parameter
     */
    public AbstractNavigationMessage(final double mu) {
        this.mu = mu;
    }

    /**
     * Getter for the ephemeris reference date.
     * @return the ephemeris reference date
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /**
     * Getter for the PRN number of the satellite.
     * @return the PRN number of the satellite
     */
    public int getPRN() {
        return prn;
    }

    /**
     * Setter for the PRN number of the satellite.
     * @param number the prn number ot set
     */
    public void setPRN(final int number) {
        this.prn = number;
    }

    /**
     * Getter for the reference week of the GNSS orbit.
     * @return the reference week of the GNSS orbit
     */
    public int getWeek() {
        return week;
    }

    /**
     * Setter for the reference week of the orbit.
     * @param week the week to set
     */
    public void setWeek(final int week) {
        this.week = week;
    }

    /**
     * Getter for the semi-major axis.
     * @return the semi-major axis in meters
     */
    public double getSma() {
        return sqrtA * sqrtA;
    }

    /**
     * Setter for the Square Root of Semi-Major Axis (m^1/2).
     * @param sqrtA the Square Root of Semi-Major Axis (m^1/2)
     */
    public void setSqrtA(final double sqrtA) {
        this.sqrtA = sqrtA;
    }

    /**
     * Getter for the mean motion.
     * @return the mean motion
     */
    public double getMeanMotion() {
        final double absA = FastMath.abs(getSma());
        return FastMath.sqrt(mu / absA) / absA + deltaN;
    }

    /**
     * Setter for the delta of satellite mean motion.
     * @param deltaN the value to set
     */
    public void setDeltaN(final double deltaN) {
        this.deltaN = deltaN;
    }

    /**
     * Getter for the longitude of ascending node of orbit plane at weekly epoch.
     * @return the longitude of ascending node of orbit plane at weekly epoch in radians
     */
    public double getOmega0() {
        return om0;
    }

    /**
     * Setter for the Longitude of Ascending Node of Orbit Plane at Weekly Epoch (rad).
     * @param omega0 the longitude of ascending node to set
     */
    public void setOmega0(final double omega0) {
        this.om0 = omega0;
    }

    /**
     * Getter for the rate of right ascension.
     * @return the rate of right ascension in rad/s
     */
    public double getOmegaDot() {
        return dom;
    }

    /**
     * Setter for the rate of Rate of Right Ascension (rad/s).
     * @param omegaDot the rate of right ascension to set
     */
    public void setOmegaDot(final double omegaDot) {
        this.dom = omegaDot;
    }

    /**
     * Getter for the argument of perigee.
     * @return the argument of perigee in radians
     */
    public double getPa() {
        return aop;
    }

    /**
     * Setter fir the Argument of Perigee (rad).
     * @param omega the argumet of perigee to set
     */
    public void setPa(final double omega) {
        this.aop = omega;
    }

    /**
     * Getter for the mean anomaly at reference time.
     * @return the mean anomaly at reference time in radians
     */
    public double getM0() {
        return anom;
    }

    /**
     * Setter for the Mean Anomaly at Reference Time (rad).
     * @param m0 the mean anomaly to set
     */
    public void setM0(final double m0) {
        this.anom = m0;
    }

    /**
     * Getter for the eccentricity.
     * @return the eccentricity
     */
    public double getE() {
        return ecc;
    }

    /**
     * Setter the eccentricity.
     * @param e the eccentricity to set
     */
    public void setE(final double e) {
        this.ecc = e;
    }

    /**
     * Getter for the inclination angle at reference time.
     * @return the inclination angle at reference time in radians
     */
    public double getI0() {
        return i0;
    }

    /**
     * Setter for the Inclination Angle at Reference Time (rad).
     * @param i0 the inclination to set
     */
    public void setI0(final double i0) {
        this.i0 = i0;
    }

    /**
     * Getter for the rate of inclination angle.
     * @return the rate of inclination angle in rad/s
     */
    public double getIDot() {
        return iDot;
    }

    /**
     * Setter for the Rate of Inclination Angle (rad/s).
     * @param iRate the rate of inclination angle to set
     */
    public void setIDot(final double iRate) {
        this.iDot = iRate;
    }

    /**
     * Getter for the reference time of the GNSS orbit as a duration from week start.
     * @return the reference time in seconds
     */
    public double getTime() {
        return toe;
    }

    /**
     * Setter for the reference time of the orbit as a duration from week start.
     * @param toe the time to set
     */
    public void setToe(final double toe) {
        this.toe = toe;
    }

    /**
     * Setter for the ephemeris reference epoch.
     * @param date the epoch to set
     */
    public void setDate(final AbsoluteDate date) {
        this.date = date;
    }

    /**
     * Getter for the the SV Clock Bias Correction Coefficient.
     * @return the SV Clock Bias Correction Coefficient (s).
     */
    public double getAf0() {
        return af0;
    }

    /**
     * Setter for the SV Clock Bias Correction Coefficient (s).
     * @param af0 the SV Clock Bias Correction Coefficient to set
     */
    public void setAf0(final double af0) {
        this.af0 = af0;
    }

    /**
     * Getter for the SV Clock Drift Correction Coefficient.
     * @return the SV Clock Drift Correction Coefficient (s/s).
     */
    public double getAf1() {
        return af1;
    }

    /**
     * Setter for the SV Clock Drift Correction Coefficient (s/s).
     * @param af1 the SV Clock Drift Correction Coefficient to set
     */
    public void setAf1(final double af1) {
        this.af1 = af1;
    }

    /**
     * Getter for the Drift Rate Correction Coefficient.
     * @return the Drift Rate Correction Coefficient (s/s²).
     */
    public double getAf2() {
        return af2;
    }

    /**
     * Setter for the Drift Rate Correction Coefficient (s/s²).
     * @param af2 the Drift Rate Correction Coefficient to set
     */
    public void setAf2(final double af2) {
        this.af2 = af2;
    }

    /**
     * Getter for the time of clock epoch.
     * @return the time of clock epoch
     */
    public AbsoluteDate getEpochToc() {
        return epochToc;
    }

    /**
     * Setter for the time of clock epoch.
     * @param epochToc the epoch to set
     */
    public void setEpochToc(final AbsoluteDate epochToc) {
        this.epochToc = epochToc;
    }

    /**
     * Getter for the Cuc parameter.
     * @return the Cuc parameter
     */
    public double getCuc() {
        return cuc;
    }

    /**
     * Setter for the Cuc parameter.
     * @param cuc the value to set
     */
    public void setCuc(final double cuc) {
        this.cuc = cuc;
    }

    /**
     * Getter for the Cus parameter.
     * @return the Cus parameter
     */
    public double getCus() {
        return cus;
    }

    /**
     * Setter for the Cus parameter.
     * @param cus the value to set
     */
    public void setCus(final double cus) {
        this.cus = cus;
    }

    /**
     * Getter for the Crc parameter.
     * @return the Crc parameter
     */
    public double getCrc() {
        return crc;
    }

    /**
     * Setter for the Crc parameter.
     * @param crc the value to set
     */
    public void setCrc(final double crc) {
        this.crc = crc;
    }

    /**
     * Getter for the Crs parameter.
     * @return the Crs parameter
     */
    public double getCrs() {
        return crs;
    }

    /**
     * Setter for the Crs parameter.
     * @param crs the value to set
     */
    public void setCrs(final double crs) {
        this.crs = crs;
    }

    /**
     * Getter for the Cic parameter.
     * @return the Cic parameter
     */
    public double getCic() {
        return cic;
    }

    /**
     * Setter for te Cic parameter.
     * @param cic the value to set
     */
    public void setCic(final double cic) {
        this.cic = cic;
    }

    /**
     * Getter for the Cis parameter.
     * @return the Cis parameter
     */
    public double getCis() {
        return cis;
    }

    /**
     * Setter for the Cis parameter.
     * @param cis the value to sets
     */
    public void setCis(final double cis) {
        this.cis = cis;
    }

}
