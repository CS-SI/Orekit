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

package org.orekit.files.ccsds.ndm.odm;

import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;

/** Container for Keplerian elements.
 * @author sports
 * @since 6.1
 */
public class KeplerianElements extends CommentsContainer implements Data {

    /** Epoch of state vector and optional Keplerian elements. */
    private AbsoluteDate epoch;

    /** Orbit semi-major axis (m). */
    private double a;

    /** Mean motion (the Keplerian Mean motion in rad/s).
     * <p>
     * Used in OMM instead of semi-major axis if MEAN_ELEMENT_THEORY = SGP/SGP4.
     * </p>
     */
    private double meanMotion;

    /** Orbit eccentricity. */
    private double e;

    /** Orbit inclination (rad). */
    private double i;

    /** Orbit right ascension of ascending node (rad). */
    private double raan;

    /** Orbit argument of pericenter (rad). */
    private double pa;

    /** Orbit anomaly (rad). */
    private double anomaly;

    /** Orbit anomaly type (mean or true). */
    private PositionAngleType anomalyType;

    /** Gravitational coefficient. */
    private double mu;

    /** Simple constructor.
     */
    public KeplerianElements() {
        a          = Double.NaN;
        meanMotion =  Double.NaN;
        e          = Double.NaN;
        i          = Double.NaN;
        raan       = Double.NaN;
        pa         = Double.NaN;
        anomaly    = Double.NaN;
        mu         = Double.NaN;
    }

    /** {@inheritDoc}
     * <p>
     * We check neither semi-major axis nor mean motion here,
     * they must be checked separately in OPM and OMM parsers
     * </p>
     */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(epoch,  StateVectorKey.EPOCH.name());
        checkNotNaN(e,       KeplerianElementsKey.ECCENTRICITY.name());
        checkNotNaN(i,       KeplerianElementsKey.INCLINATION.name());
        checkNotNaN(raan,    KeplerianElementsKey.RA_OF_ASC_NODE.name());
        checkNotNaN(pa,      KeplerianElementsKey.ARG_OF_PERICENTER.name());
        checkNotNaN(anomaly, KeplerianElementsKey.MEAN_ANOMALY.name());
    }

    /** Get epoch of state vector, Keplerian elements and covariance matrix data.
     * @return epoch the epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Set epoch of state vector, Keplerian elements and covariance matrix data.
     * @param epoch the epoch to be set
     */
    public void setEpoch(final AbsoluteDate epoch) {
        refuseFurtherComments();
        this.epoch = epoch;
    }

    /** Get the orbit semi-major axis.
     * @return the orbit semi-major axis
     */
    public double getA() {
        return a;
    }

    /** Set the orbit semi-major axis.
     * @param a the semi-major axis to be set
     */
    public void setA(final double a) {
        refuseFurtherComments();
        this.a = a;
    }

    /** Get the orbit mean motion.
     * @return the orbit mean motion
     */
    public double getMeanMotion() {
        return meanMotion;
    }

    /** Set the orbit mean motion.
     * @param motion the mean motion to be set
     */
    public void setMeanMotion(final double motion) {
        this.meanMotion = motion;
    }

    /** Get the orbit eccentricity.
     * @return the orbit eccentricity
     */
    public double getE() {
        return e;
    }

    /** Set the orbit eccentricity.
     * @param e the eccentricity to be set
     */
    public void setE(final double e) {
        refuseFurtherComments();
        this.e = e;
    }

    /** Get the orbit inclination.
     * @return the orbit inclination
     */
    public double getI() {
        return i;
    }

    /**Set the orbit inclination.
     * @param i the inclination to be set
     */
    public void setI(final double i) {
        refuseFurtherComments();
        this.i = i;
    }

    /** Get the orbit right ascension of ascending node.
     * @return the orbit right ascension of ascending node
     */
    public double getRaan() {
        return raan;
    }

    /** Set the orbit right ascension of ascending node.
     * @param raan the right ascension of ascending node to be set
     */
    public void setRaan(final double raan) {
        refuseFurtherComments();
        this.raan = raan;
    }

    /** Get the orbit argument of pericenter.
     * @return the orbit argument of pericenter
     */
    public double getPa() {
        return pa;
    }

    /** Set the orbit argument of pericenter.
     * @param pa the argument of pericenter to be set
     */
    public void setPa(final double pa) {
        refuseFurtherComments();
        this.pa = pa;
    }

    /** Get the orbit anomaly.
     * @return the orbit anomaly
     */
    public double getAnomaly() {
        return anomaly;
    }

    /** Set the orbit anomaly.
     * @param anomaly the anomaly to be set
     */
    public void setAnomaly(final double anomaly) {
        refuseFurtherComments();
        this.anomaly = anomaly;
    }

    /** Get the type of anomaly (true or mean).
     * @return the type of anomaly
     */
    public PositionAngleType getAnomalyType() {
        return anomalyType;
    }

    /** Set the type of anomaly.
     * @param anomalyType the type of anomaly to be set
     */
    public void setAnomalyType(final PositionAngleType anomalyType) {
        refuseFurtherComments();
        this.anomalyType = anomalyType;
    }

    /**
     * Set the gravitational coefficient.
     * @param mu the coefficient to be set
     */
    public void setMu(final double mu) {
        refuseFurtherComments();
        this.mu = mu;
    }

    /**
     * Get the gravitational coefficient.
     * @return gravitational coefficient
     */
    public double getMu() {
        return mu;
    }

    /** Generate a keplerian orbit.
     * @param frame inertial frame for orbit
     * @return generated orbit
     */
    public KeplerianOrbit generateKeplerianOrbit(final Frame frame) {
        return new KeplerianOrbit(a, e, i, pa, raan, anomaly, anomalyType, frame, epoch, mu);
    }

}
