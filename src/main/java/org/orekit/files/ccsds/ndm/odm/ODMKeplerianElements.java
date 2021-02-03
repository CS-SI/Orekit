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

package org.orekit.files.ccsds.ndm.odm;

import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;
import org.orekit.orbits.PositionAngle;

/** This class gathers the general state data present in both OPM and OMM files.
 * @param <S> type of the segments
 * @author sports
 * @since 6.1
 */
public class ODMKeplerianElements extends CommentsContainer implements Data {

    /** Orbit semi-major axis (m). */
    private double a;

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
    private PositionAngle anomalyType;

    /** Gravitational coefficient. */
    private double mu;

    /** Simple constructor.
     */
    public ODMKeplerianElements() {
        a       = Double.NaN;
        e       = Double.NaN;
        i       = Double.NaN;
        raan    = Double.NaN;
        pa      = Double.NaN;
        anomaly = Double.NaN;
        mu      = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        checkNotNaN(a,       ODMKplerianElementsKey.SEMI_MAJOR_AXIS);
        checkNotNaN(e,       ODMKplerianElementsKey.ECCENTRICITY);
        checkNotNaN(i,       ODMKplerianElementsKey.INCLINATION);
        checkNotNaN(raan,    ODMKplerianElementsKey.RA_OF_ASC_NODE);
        checkNotNaN(pa,      ODMKplerianElementsKey.ARG_OF_PERICENTER);
        checkNotNaN(anomaly, ODMKplerianElementsKey.MEAN_ANOMALY);
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
    void setA(final double a) {
        refuseFurtherComments();
        this.a = a;
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
    void setE(final double e) {
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
    void setI(final double i) {
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
    void setRaan(final double raan) {
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
    void setPa(final double pa) {
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
    void setAnomaly(final double anomaly) {
        refuseFurtherComments();
        this.anomaly = anomaly;
    }

    /** Get the type of anomaly (true or mean).
     * @return the type of anomaly
     */
    public PositionAngle getAnomalyType() {
        return anomalyType;
    }

    /** Set the type of anomaly.
     * @param anomalyType the type of anomaly to be set
     */
    void setAnomalyType(final PositionAngle anomalyType) {
        refuseFurtherComments();
        this.anomalyType = anomalyType;
    }

    /**
     * Set the gravitational coefficient.
     * @param mu the coefficient to be set
     */
    void setMu(final double mu) {
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

}
