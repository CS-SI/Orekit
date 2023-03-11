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

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;

/**
 * Base class for GNSS navigation messages.
 * @author Bryan Cazabonne
 * @since 11.0
 *
 * @see GPSLegacyNavigationMessage
 * @see GalileoNavigationMessage
 * @see BeidouLegacyNavigationMessage
 * @see QZSSLegacyNavigationMessage
 * @see IRNSSNavigationMessage
 */
public abstract class AbstractNavigationMessage extends CommonGnssData implements GNSSOrbitalElements {

    /** Square root of a. */
    private double sqrtA;

    /** Mean Motion Difference from Computed Value. */
    private double deltaN;

    /** Rate of Inclination Angle (rad/s). */
    private double iDot;

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

    /** Transmission time.
     * @since 12.0
     */
    private double transmissionTime;

    /**
     * Constructor.
     * @param mu Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weekNumber number of weeks in the GNSS cycle
     */
    public AbstractNavigationMessage(final double mu,
                                     final double angularVelocity,
                                     final int weekNumber) {
        super(mu, angularVelocity, weekNumber);
    }

    /**
     * Getter for Square Root of Semi-Major Axis (√m).
     * @return Square Root of Semi-Major Axis (√m)
     */
    public double getSqrtA() {
        return sqrtA;
    }

    /**
     * Setter for the Square Root of Semi-Major Axis (√m).
     * <p>
     * In addition, this method set the value of the Semi-Major Axis.
     * </p>
     * @param sqrtA the Square Root of Semi-Major Axis (√m)
     */
    public void setSqrtA(final double sqrtA) {
        this.sqrtA = sqrtA;
        setSma(sqrtA * sqrtA);
    }

    /**
     * Getter for the mean motion.
     * @return the mean motion
     */
    public double getMeanMotion() {
        final double absA = FastMath.abs(getSma());
        return FastMath.sqrt(getMu() / absA) / absA + deltaN;
    }

    /**
     * Getter for the delta of satellite mean motion.
     * @return delta of satellite mean motion
     */
    public double getDeltaN() {
        return deltaN;
    }

    /**
     * Setter for the delta of satellite mean motion.
     * @param deltaN the value to set
     */
    public void setDeltaN(final double deltaN) {
        this.deltaN = deltaN;
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

    /**
     * Getter for transmission time.
     * @return transmission time
     * @since 12.0
     */
    public double getTransmissionTime() {
        return transmissionTime;
    }

    /**
     * Setter for transmission time.
     * @param transmissionTime transmission time
     * @since 12.0
     */
    public void setTransmissionTime(final double transmissionTime) {
        this.transmissionTime = transmissionTime;
    }

}
