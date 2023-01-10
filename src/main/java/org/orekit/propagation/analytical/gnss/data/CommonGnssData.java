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

import org.orekit.time.AbsoluteDate;

/**
 * Container for common GNSS data contained in almanac and navigation messages.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class CommonGnssData {

    /** PRN number of the satellite. */
    private int prn;

    /** Reference Week of the orbit. */
    private int week;

    /** Reference Time. */
    private double time;

    /** Semi-Major Axis (m). */
    private double sma;

    /** Eccentricity. */
    private double ecc;

    /** Inclination Angle at Reference Time (rad). */
    private double i0;

    /** Longitude of Ascending Node of Orbit Plane at Weekly Epoch (rad). */
    private double om0;

    /** Rate of Right Ascension (rad/s). */
    private double dom;

    /** Argument of Perigee (rad). */
    private double aop;

    /** Mean Anomaly at Reference Time (rad). */
    private double anom;

    /** SV Clock Bias Correction Coefficient (s). */
    private double af0;

    /** SV Clock Drift Correction Coefficient (s/s). */
    private double af1;

    /** Reference epoch. */
    private AbsoluteDate date;

    /** Mean angular velocity of the Earth for the GNSS model. */
    private final double angularVelocity;

    /** Duration of the GNSS cycle in seconds. */
    private final double cycleDuration;

    /** Earth's universal gravitational parameter. */
    private final double mu;

    /**
     * Constructor.
     * @param mu Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weekNumber number of weeks in the GNSS cycle
     */
    public CommonGnssData(final double mu,
                          final double angularVelocity,
                          final int weekNumber) {
        this.mu              = mu;
        this.angularVelocity = angularVelocity;
        this.cycleDuration   = GNSSConstants.GNSS_WEEK_IN_SECONDS * weekNumber;
    }

    /**
     * Getter for the Earth's universal gravitational parameter.
     * @return the Earth's universal gravitational parameter
     */
    public double getMu() {
        return mu;
    }

    /**
     * Getter for the mean angular velocity of the Earth for the GNSS model.
     * @return the mean angular velocity of the Earth for the GNSS model
     */
    public double getAngularVelocity() {
        return angularVelocity;
    }

    /**
     * Getter for the duration of the GNSS cycle in seconds.
     * @return the duration of the GNSS cycle in seconds
     */
    public double getCycleDuration() {
        return cycleDuration;
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
        return sma;
    }

    /**
     * Setter for the semi-major axis.
     * @param sma the semi-major axis (m)
     */
    public void setSma(final double sma) {
        this.sma = sma;
    }

    /**
     * Getter for the reference time of the GNSS orbit as a duration from week start.
     * @return the reference time in seconds
     */
    public double getTime() {
        return time;
    }

    /**
     * Setter for the reference time of the orbit as a duration from week start.
     * @param time the time to set in seconds
     */
    public void setTime(final double time) {
        this.time = time;
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
     * Getter for the ephemeris reference date.
     * @return the ephemeris reference date
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /**
     * Setter for the reference epoch.
     * @param date the epoch to set
     */
    public void setDate(final AbsoluteDate date) {
        this.date = date;
    }

}
