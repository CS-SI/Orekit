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

package org.orekit.files.ccsds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;

/** This class gathers the general state data present in both OPM and OMM files.
 * <p>
 * This class does not appear in the CCSDS standard, it is only a design
 * feature of Orekit to reduce code duplication.
 * </p>
 * @author sports
 * @since 6.1
 */
public abstract class OGMFile extends ODMFile {

    /** Epoch of state vector and optional Keplerian elements. */
    private AbsoluteDate epoch;

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

    /** Spacecraft mass. */
    private double mass;

    /** Solar radiation pressure area (m^2). */
    private double solarRadArea;

    /** Solar radiation pressure coefficient. */
    private double solarRadCoeff;

    /** Drag area (m^2). */
    private double dragArea;

    /** Drag coefficient. */
    private double dragCoeff;

    /** Coordinate system for covariance matrix, for Local Orbital Frames. */
    private LOFType covRefLofType;

    /** Coordinate system for covariance matrix, for absolute frames.
     * If not given it is set equal to refFrame. */
    private Frame covRefFrame;

    /** Position/Velocity covariance matrix. */
    private RealMatrix covarianceMatrix;

    /** Map of user defined parameter keywords and corresponding values. */
    private Map<String, String> userDefinedParameters;

    /** Tests whether the OPM contains Keplerian elements data. */
    private boolean hasKeplerianElements;

    /** Epoch comments. The list contains a string for each line of comment. */
    private List<String> epochComment;

    /** Keplerian elements comments. The list contains a string for each line of comment. */
    private List<String> keplerianElementsComment;

    /** Spacecraft data comments. The list contains a string for each line of comment. */
    private List<String> spacecraftComment;

    /** Covariance matrix data comments. The list contains a string for each line of comment. */
    private List<String> covarianceComment;

    /** Create a new OPM file object. */
    OGMFile() {
        mass                     = Double.NaN;
        userDefinedParameters    = new HashMap<String, String>();
        epochComment             = Collections.emptyList();
        keplerianElementsComment = Collections.emptyList();
        spacecraftComment        = Collections.emptyList();
        covarianceComment        = Collections.emptyList();
    };

    /** Get epoch of state vector, Keplerian elements and covariance matrix data.
     * @return epoch the epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Set epoch of state vector, Keplerian elements and covariance matrix data.
     * @param epoch the epoch to be set
     */
    void setEpoch(final AbsoluteDate epoch) {
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
    void setA(final double a) {
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
        this.anomaly = anomaly;
    }

    /** Get the type of anomaly (true or mean).
     * @return the type of anomaly
     */
    public PositionAngle getAnomalyType() {
        return anomalyType;
    }

    /** Set the type of anomaly (true or mean).
     * @param anomalyType the type of anomaly to be set
     */
    void setAnomalyType(final String anomalyType) {
        this.anomalyType = PositionAngle.valueOf(anomalyType);
    }

    /** Get the spacecraft mass.
     * @return the spacecraft mass
     * @exception OrekitException if mass is unknown
     */
    public double getMass() throws OrekitException {
        if (Double.isNaN(mass)) {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_SPACECRAFT_MASS);
        }
        return mass;
    }

    /** Set the spacecraft mass.
     * @param mass the spacecraft mass to be set
     */
    void setMass(final double mass) {
        this.mass = mass;
    }

    /** Get the solar radiation pressure area.
     * @return the solar radiation pressure area
     */
    public double getSolarRadArea() {
        return solarRadArea;
    }

    /** Set the solar radiation pressure area.
     * @param solarRadArea the area to be set
     */
    void setSolarRadArea(final double solarRadArea) {
        this.solarRadArea = solarRadArea;
    }

    /** Get the solar radiation pressure coefficient.
     * @return the solar radiation pressure coefficient
     */
    public double getSolarRadCoeff() {
        return solarRadCoeff;
    }

    /** Get the solar radiation pressure coefficient.
     * @param solarRadCoeff the coefficient to be set
     */
    void setSolarRadCoeff(final double solarRadCoeff) {
        this.solarRadCoeff = solarRadCoeff;
    }

    /** Get the drag area.
     * @return the drag area
     */
    public double getDragArea() {
        return dragArea;
    }

    /** Set the drag area.
     * @param dragArea the area to be set
     */
    void setDragArea(final double dragArea) {
        this.dragArea = dragArea;
    }

    /** Get the drag coefficient.
     * @return the drag coefficient
     */
    public double getDragCoeff() {
        return dragCoeff;
    }

    /** Set the drag coefficient.
     * @param dragCoeff the coefficient to be set
     */
    void setDragCoeff(final double dragCoeff) {
        this.dragCoeff = dragCoeff;
    }

    /** Get coordinate system for covariance matrix, for Local Orbital Frames.
     * <p>
     * The value returned is null if the covariance matrix is given in an
     * absolute frame rather than a Local Orbital Frame. In this case, the
     * method {@link #getCovRefFrame()} must be used instead.
     * </p>
     * @return the coordinate system for covariance matrix, or null if the
     * covariance matrix is given in an absolute frame rather than a Local
     * Orbital Frame
     */
    public LOFType getCovRefLofType() {
        return covRefLofType;
    }

    /** Set coordinate system for covariance matrix, for Local Orbital Frames.
     * @param covRefLofType the coordinate system to be set
     */
    void setCovRefLofType(final LOFType covRefLofType) {
        this.covRefLofType = covRefLofType;
        this.covRefFrame   = null;
    }

    /** Get coordinate system for covariance matrix, for absolute frames.
     * <p>
     * The value returned is null if the covariance matrix is given in a
     * Local Orbital Frame rather than an absolute frame. In this case, the
     * method {@link #getCovRefLofType()} must be used instead.
     * </p>
     * @return the coordinate system for covariance matrix
     */
    public Frame getCovRefFrame() {
        return covRefFrame;
    }

    /** Set coordinate system for covariance matrix.
     * @param covRefFrame the coordinate system to be set
     */
    void setCovRefFrame(final Frame covRefFrame) {
        this.covRefLofType = null;
        this.covRefFrame   = covRefFrame;
    }

    /** Get the Position/Velocity covariance matrix.
     * @return the Position/Velocity covariance matrix
     */
    public RealMatrix getCovarianceMatrix() {
        return covarianceMatrix;
    }

    /** Set an entry in the Position/Velocity covariance matrix.
     * <p>
     * Both m(j, k) and m(k, j) are set.
     * </p>
     * @param j row index (must be between 0 and 5 (inclusive)
     * @param k column index (must be between 0 and 5 (inclusive)
     * @param entry value of the matrix entry
     */
    void setCovarianceMatrixEntry(final int j, final int k, final double entry) {
        covarianceMatrix.setEntry(j, k, entry);
        covarianceMatrix.setEntry(k, j, entry);
    }

    /** Get the map of user defined parameter keywords and their corresponding values.
     * @return the map of user defined parameter keywords and their corresponding values.
     */
    public Map<String, String> getUserDefinedParameters() {
        return userDefinedParameters;
    }

    /** Add a pair keyword-value in the map of user defined parameter keywords and their corresponding values.
     * @param keyword the user defined parameter keyword to be set. Starts with USER_DEFINED_
     * @param value the user defined parameter value to be set
     */
    void setUserDefinedParameters(final String keyword,
                                         final String value) {
        userDefinedParameters.put(keyword, value);
    }

    /** Check whether the OPM contains Keplerian elements data.
     * @return true if OPM contains Keplerian elements data.
     */
    public boolean hasKeplerianElements() {
        return hasKeplerianElements;
    }

    /** Set boolean testing whether the OPM contains Keplerian elements data.
     * @param hasKeplerianElements the boolean to be set.
     */
    void setHasKeplerianElements(final boolean hasKeplerianElements) {
        this.hasKeplerianElements = hasKeplerianElements;
    }

    /** Check whether the OPM contains covariance matrix data.
     * @return true if OPM contains covariance matrix data.
     */
    public boolean hasCovarianceMatrix() {
        return covarianceMatrix != null;
    }

    /** Create a covariance matrix, initialized to zero.
     */
    void createCovarianceMatrix() {
        covarianceMatrix = MatrixUtils.createRealMatrix(6, 6);
    }

    /** Get the comment for epoch.
     * @return comment for epoch
     */
    public List<String> getEpochComment() {
        return Collections.unmodifiableList(epochComment);
    }

    /** Set the comment for epoch.
     * @param comment comment to set
     */
    void setEpochComment(final List<String> comment) {
        epochComment = new ArrayList<String>(comment);
    }

    /** Get the comment for Keplerian elements.
     * @return comment for Keplerian elements
     */
    public List<String> getKeplerianElementsComment() {
        return Collections.unmodifiableList(keplerianElementsComment);
    }

    /** Set the comment for Keplerian elements.
     * @param comment comment to set
     */
    void setKeplerianElementsComment(final List<String> comment) {
        keplerianElementsComment = new ArrayList<String>(comment);
    }

    /** Get the comment for spacecraft.
     * @return comment for spacecraft
     */
    public List<String> getSpacecraftComment() {
        return Collections.unmodifiableList(spacecraftComment);
    }

    /** Set the comment for spacecraft.
     * @param comment comment to set
     */
    void setSpacecraftComment(final List<String> comment) {
        spacecraftComment = new ArrayList<String>(comment);
    }

    /** Get the comment for covariance.
     * @return comment for covariance
     */
    public List<String> getCovarianceComment() {
        return Collections.unmodifiableList(covarianceComment);
    }

    /** Set the comment for covariance.
     * @param comment comment to set
     */
    void setCovarianceComment(final List<String> comment) {
        covarianceComment = new ArrayList<String>(comment);
    }

    /** Get the meta data.
     * @return meta data
     */
    public abstract ODMMetaData getMetaData();

}
