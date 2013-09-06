/* Copyright 2002-2013 CS Systèmes d'Information
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/**
 * This class gathers the informations present in the Orbital Mean-Elements Message (OMM),
 * and contains methods to generate a {@link CartesianOrbit}, a {@link KeplerianOrbit},
 * a {@link SpacecraftState} and, eventually, a {@link TLE}.
 * @author sports
 * @since 6.1
 */
public class OMMFile
    extends ODMFile {

    /** Spacecraft name for which the orbit state is provided. */
    private String objectName;

    /** Object identifier of the object for which the orbit state is provided. */
    private String objectID;

    /** Origin of reference frame. */
    private String centerName;

    /** Celestial body corresponding to the centerName. */
    private CelestialBody centerBody;

    /** Reference frame in which data are given: used for Keplerian element data. */
    private Frame refFrame;

    /** Epoch of reference frame, if not intrinsic to the definition of the
     * reference frame. */
    private AbsoluteDate frameEpoch;

    /** Time System: used for metadata, orbit state and covariance data. */
    private TimeSystem timeSystem;

    /** Time scale corresponding to the timeSystem. */
    private TimeScale timeScale;

    /** Description of the Mean Element Theory. Indicates the proper method to employ
     * to propagate the state. */
    private String meanElementTheory;

    /** Epoch of Mean Keplerian elements. */
    private AbsoluteDate epoch;

    /** Mean semi-major axis (m). */
    private double a;

    /** Mean motion (the Keplerian Mean motion in revolutions per day). To be used instead of semi-major
     * axis if MEAN_ELEMENT_THEORY = SGP/SGP4. */
    private double meanMotion;

    /** Mean eccentricity. */
    private double e;

    /** Mean inclination (rad). */
    private double i;

    /** Mean right ascension of ascending node (rad). */
    private double raan;

    /** Mean argument of pericenter (rad). */
    private double pa;

    /** Mean mean anomaly (rad). */
    private double anomaly;

    /** Spacecraft mass. */
    private double mass;

    /** Solar radiation pressure area. */
    private double solarRadArea;

    /** Solar radiation pressure coefficient. */
    private double solarRadCoeff;

    /** Drag area. */
    private double dragArea;

    /** Drag coefficient. */
    private double dragCoeff;

    /** Ephemeris Type, only required if MEAN_ELEMENT_THEORY = SGP/SGP4. Some sources suggest the coding for
     * the EPHEMERIS_TYPE keyword: 1 = SGP, 2 = SGP4, 3 = SDP4, 4 = SGP8, 5 = SDP8. Default value = 0.
     */
    private int ephemerisType;

    /** Classification Type, only required if MEAN_ELEMENT_THEORY = SGP/SGP4. Some sources suggest the
     *  following coding for the CLASSIFICATION_TYPE keyword: U = unclassified, S = secret. Default value = U.
     */
    private char classificationType;

    /** Launch year. Not parsed but set manually in the OMMParser. */
    private int launchYear;

    /** Launch number. Not parsed but set manually in the OMMParser. */
    private int launchNumber;

    /** Piece of launch (from "A" to "ZZZ"). Not parsed but set manually in the OMMParser. */
    private String launchPiece;

    /** NORAD Catalog Number ("Satellite Number"), an integer of up to nine digits. */
    private Integer noradID;

    /** Element set number for this satellite, only required if MEAN_ELEMENT_THEORY = SGP/SGP4.
     * Normally incremented sequentially, but may be out of sync if it is generated from a backup source.
     * Used to distinguish different TLEs, and therefore only meaningful if TLE based data is being exchanged. */
    private String elementSetNo;

    /** Revolution Number, only required if MEAN_ELEMENT_THEORY = SGP/SGP4. */
    private int revAtEpoch;

    /** SGP/SGP4 drag-like coefficient (in units 1/[Earth radii]), only required if MEAN_ELEMENT_THEORY = SGP/SGP4. */
    private Double bStar;

    /** First Time Derivative of the Mean Motion, only required if MEAN_ELEMENT_THEORY = SGP. */
    private Double meanMotionDot;

    /** Second Time Derivative of Mean Motion, only required if MEAN_ELEMENT_THEORY = SGP. */
    private Double meanMotionDotDot;

    /** Coordinate system for covariance matrix, for Local Orbital Frames. */
    private LOFType covRefLofType;

    /** Coordinate system for covariance matrix, for absolute frames.
     * If not given it is set equal to refFrame. */
    private Frame covRefFrame;

    /** Position/Velocity covariance matrix. */
    private RealMatrix covarianceMatrix;

    /** Map of user defined parameter keywords and corresponding values.    */
    private Map<String, String> userDefinedParameters;

    /** Tests whether the body corresponding to the centerName attribute can be
     * created through the {@link org.orekit.bodies.CelestialBodyFactory} in order to obtain the
     * corresponding gravitational coefficient. */
    private Boolean hasCreatableBody;

    /** Tests whether the OPM contains covariance matrix data. */
    private boolean hasCovarianceMatrix;

    /** Metadata comments. The list contains a string for each line of comment. */
    private List<String> metadataComment;

    /** Mean Keplerian elements data comments. The list contains a string for each line of comment. */
    private List<String> dataMeanKeplerianElementsComment;

    /** Spacecraft data comments. The list contains a string for each line of comment. */
    private List<String> dataSpacecraftComment;

    /** TLE related parameters comments. The list contains a string for each line of comment. */
    private List<String> dataTleRelatedParametersComment;

    /** Covariance matrix data comments. The list contains a string for each line of comment. */
    private List<String> dataCovarianceComment;

    /** Create a new OMM file object. */
    OMMFile() {
        mass = Double.NaN;
        userDefinedParameters = new HashMap<String, String>();
    };

    /** Get the spacecraft name for which the orbit state is provided.
     * @return the spacecraft name
     */
    public String getObjectName() {
        return objectName;
    }

    /** Set the spacecraft name for which the orbit state is provided.
     * @param objectName the spacecraft name to be set
     */
    public void setObjectName(final String objectName) {
        this.objectName = objectName;
    }

    /** Get the spacecraft ID for which the orbit state is provided.
     * @return the spacecraft ID
     */
    public String getObjectID() {
        return objectID;
    }

    /** Set the spacecraft ID for which the orbit state is provided.
     * @param objectID the spacecraft ID to be set
     */
    void setObjectID(final String objectID) {
        this.objectID = objectID;
    }

    /** Get the origin of reference frame.
     * @return the origin of reference frame.
     */
    public String getCenterName() {
        return centerName;
    }

    /** Set the origin of reference frame.
     * @param centerName the origin of reference frame to be set
     */
    void setCenterName(final String centerName) {
        this.centerName = centerName;
    }

    /** Get the {@link CelestialBody} corresponding to the center name.
     * @return the center body
     */
    public CelestialBody getCenterBody() {
        return centerBody;
    }

    /** Set the {@link CelestialBody} corresponding to the center name.
     * @param centerBody the {@link CelestialBody} to be set
     */
    void setCenterBody(final CelestialBody centerBody) {
        this.centerBody = centerBody;
    }

    /** Get the reference frame in which data are given: used for Keplerian element data.
     * @return the reference frame
     */
    public Frame getFrame() {
        return refFrame;
    }

    /** Set the reference frame in which data are given: used for Keplerian element data.
     * @param refFrame the reference frame to be set
     */
    void setRefFrame(final Frame refFrame) {
        this.refFrame = refFrame;
    }

    /** Get epoch of reference frame, if not intrinsic to the definition of the
     * reference frame.
     * @return epoch of reference frame
     */
    public AbsoluteDate getFrameEpoch() {
        return frameEpoch;
    }

    /** Set epoch of reference frame, if not intrinsic to the definition of the
     * reference frame.
     * @param frameEpoch the epoch of reference frame to be set
     */
    void setFrameEpoch(final AbsoluteDate frameEpoch) {
        this.frameEpoch = frameEpoch;
    }

    /** Set epoch of reference frame for MET and MRT Timesystems, if not intrinsic to the definition of the
     * reference frame.
     * @param offset the offset between the reference frame epoch and the initial date
     */
    void setFrameEpoch(final double offset) {
        this.frameEpoch = getInitialDate().shiftedBy(offset);
    }

    /** Get the Time System that: for OPM, is used for metadata, state vector,
     * maneuver and covariance data, for OMM, is used for metadata, orbit state
     * and covariance data, for OEM, is used for metadata, ephemeris and
     * covariance data.
     * @return the time system
     */
    public TimeSystem getTimeSystem() {
        return timeSystem;
    }

    /** Set the Time System that: for OPM, is used for metadata, state vector,
     * maneuver and covariance data, for OMM, is used for metadata, orbit state
     * and covariance data, for OEM, is used for metadata, ephemeris and
     * covariance data.
     * @param timeSystem the time system to be set
     */
    void setTimeSystem(final TimeSystem timeSystem) {
        this.timeSystem = timeSystem;
    }

    /** Get time scale corresponding to the timeSystem.
     * @return the time scale
     */
    public TimeScale getTimeScale() {
        return timeScale;
    }

    /** Set time scale corresponding to the timeSystem.
     * @param timeScale the time scale to be set
     */
    void setTimeScale(final TimeScale timeScale) {
        this.timeScale = timeScale;
    }

    /** Get the description of the Mean Element Theory.
     * @return the mean element theory
     */
    public String getMeanElementTheory() {
        return meanElementTheory;
    }

    /** Set the description of the Mean Element Theory.
     * @param meanElementTheory the mean element theory to be set
     */
    void setMeanElementTheory(final String meanElementTheory) {
        this.meanElementTheory = meanElementTheory;
    }

    /** Get epoch of Mean Keplerian elements.
     * @return epoch the epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Set epoch of Mean Keplerian elements.
     * @param epoch the epoch to be set
     */
    void setEpoch(final AbsoluteDate epoch) {
        this.epoch = epoch;
    }

    /**Set epoch of state vector for MET and MRT Time Systems.
     * @param offset the offset between the epoch and the initial date
     */
    void setEpoch(final double offset) {
        this.epoch = getInitialDate().shiftedBy(offset);
    }

    /** Get the orbit mean semi-major axis.
     * @return the orbit mean semi-major axis
     */
    public double getA() {
        return a;
    }

    /** Set the orbit mean semi-major axis.
     * @param a the mean semi-major axis to be set
     */
    void setA(final double a) {
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
    void setMeanMotion(final double motion) {
        this.meanMotion = motion;
    }

    /** Get the orbit mean eccentricity.
     * @return the orbit mean eccentricity
     */
    public double getE() {
        return e;
    }

    /** Set the orbit mean eccentricity.
     * @param e the mean eccentricity to be set
     */
    void setE(final double e) {
        this.e = e;
    }

    /** Get the orbit mean inclination.
     * @return the orbit mean inclination
     */
    public double getI() {
        return i;
    }

    /**Set the orbit mean inclination.
     * @param i the mean inclination to be set
     */
    void setI(final double i) {
        this.i = i;
    }

    /** Get the orbit mean right ascension of ascending node.
     * @return the orbit mean right ascension of ascending node
     */
    public double getRaan() {
        return raan;
    }

    /** Set the orbit mean right ascension of ascending node.
     * @param raan the mean right ascension of ascending node to be set
     */
    void setRaan(final double raan) {
        this.raan = raan;
    }

    /** Get the orbit mean argument of pericenter.
     * @return the orbit mean argument of pericenter
     */
    public double getPa() {
        return pa;
    }

    /** Set the orbit mean argument of pericenter.
     * @param pa the mean argument of pericenter to be set
     */
    void setPa(final double pa) {
        this.pa = pa;
    }

    /** Get the orbit mean mean (very mean) anomaly.
     * @return the orbit mean mean anomaly
     */
    public double getAnomaly() {
        return anomaly;
    }

    /** Set the orbit mean mean anomaly.
     * @param anomaly the mean mean anomaly to be set
     */
    void setAnomaly(final double anomaly) {
        this.anomaly = anomaly;
    }

    /** Get the spacecraft mass.
     * @return the spacecraft mass
     */
    public double getMass() {
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

    /** Set the solar radiation pressure coefficient.
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

    /** Get the ephemeris type.
     * @return the ephemerisType
     */
    public int getEphemerisType() {
        return ephemerisType;
    }

    /** Set the ephemeris type.
     * @param ephemerisType the ephemeris type to be set
     */
    void setEphemerisType(final int ephemerisType) {
        this.ephemerisType = ephemerisType;
    }

    /** Get the classification type.
     * @return the classificationType
     */
    public char getClassificationType() {
        return classificationType;
    }

    /** Set the classification type.
     * @param classificationType the classification type to be set
     */
    void setClassificationType(final char classificationType) {
        this.classificationType = classificationType;
    }

    /** Get the launch year.
     * @return the launch year
     */
    public int getLaunchYear() {
        return launchYear;
    }

    /** Set the launch year.
     * @param launchYear the launch year to be set
     */
    void setLaunchYear(final int launchYear) {
        this.launchYear = launchYear;
    }

    /** Get the launch number.
     * @return the launch number
     */
    public int getLaunchNumber() {
        return launchNumber;
    }

    /** Set the launch number.
     * @param launchNumber the launch number to be set
     */
    void setLaunchNumber(final int launchNumber) {
        this.launchNumber = launchNumber;
    }

    /** Get the launch piece.
     * @return the launch piece
     */
    public String getLaunchPiece() {
        return launchPiece;
    }

    /** Set the launch piece.
     * @param launchPiece the launch piece to be set
     */
    void setLaunchPiece(final String launchPiece) {
        this.launchPiece = launchPiece;
    }

    /** Get the NORAD Catalog Number ("Satellite Number").
     * @return the NORAD Catalog Number
     */
    public Integer getNoradID() {
        return noradID;
    }

    /** Set the NORAD Catalog Number ("Satellite Number").
     * @param noradID the element set number to be set
     */
    void setNoradID(final Integer noradID) {
        this.noradID = noradID;
    }

    /** Get the element set number for this satellite.
     * @return the element set number for this satellite
     */
    public String getElementSetNumber() {
        return elementSetNo;
    }

    /** Set the element set number for this satellite.
     * @param elementSetNo the element set number to be set
     */
    void setElementSetNo(final String elementSetNo) {
        this.elementSetNo = elementSetNo;
    }

    /** Get the revolution rumber.
     * @return the revolution rumber
     */
    public int getRevAtEpoch() {
        return revAtEpoch;
    }

    /** Set the revolution rumber.
     * @param revAtEpoch the Revolution Number to be set
     */
    void setRevAtEpoch(final int revAtEpoch) {
        this.revAtEpoch = revAtEpoch;
    }

    /** Get the SGP/SGP4 drag-like coefficient.
     * @return the SGP/SGP4 drag-like coefficient
     */
    public double getBStar() {
        return bStar;
    }

    /** Set the SGP/SGP4 drag-like coefficient.
     * @param bStar the SGP/SGP4 drag-like coefficient to be set
     */
    void setbStar(final double bStar) {
        this.bStar = bStar;
    }

    /** Get the first time derivative of the mean motion.
     * @return the first time derivative of the mean motion
     */
    public double getMeanMotionDot() {
        return meanMotionDot;
    }

    /** Set the first time derivative of the mean motion.
     * @param meanMotionDot the first time derivative of the mean motion to be set
     */
    void setMeanMotionDot(final double meanMotionDot) {
        this.meanMotionDot = meanMotionDot;
    }

    /** Get the second time derivative of the mean motion.
     * @return the second time derivative of the mean motion
     */
    public double getMeanMotionDotDot() {
        return meanMotionDotDot;
    }

    /** Set the second time derivative of the mean motion.
     * @param meanMotionDotDot the second time derivative of the mean motion to be set
     */
    void setMeanMotionDotDot(final double meanMotionDotDot) {
        this.meanMotionDotDot = meanMotionDotDot;
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

    /** Set the Position/Velocity covariance matrix.
     * @param covarianceMatrix the covariance matrix to be set
     */
    void setCovarianceMatrix(final RealMatrix covarianceMatrix) {
        this.covarianceMatrix = (Array2DRowRealMatrix) covarianceMatrix;
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

    /** Get boolean testing whether the body corresponding to the centerName
     * attribute can be created through the {@link org.orekit.bodies.CelestialBodyFactory}.
     * @return true if {@link CelestialBody} can be created from centerName
     *         false otherwise
     */
    public boolean getHasCreatableBody() {
        return hasCreatableBody;
    }

    /** Set boolean testing whether the body corresponding to the centerName
     * attribute can be created through the {@link org.orekit.bodies.CelestialBodyFactory}.
     * @param hasCreatableBody the boolean to be set.
     */
    void setHasCreatableBody(final boolean hasCreatableBody) {
        this.hasCreatableBody = hasCreatableBody;
    }

    /** Get boolean testing whether the OPM contains covariance matrix data.
    * @return true if OPM contains covariance matrix data.
    *         false otherwise */
    public boolean getHasCovarianceMatrix() {
        return hasCovarianceMatrix;
    }

    /** Set boolean testing whether the OPM contains covariance matrix data.
     * @param hasCovarianceMatrix the boolean to be set.
     */
    void setHasCovarianceMatrix(final boolean hasCovarianceMatrix) {
        this.hasCovarianceMatrix = hasCovarianceMatrix;
    }

    /** {@inheritDoc} */
    public List<String> getComment(final ODMBlock odmBlock)
        throws OrekitException {
        switch (odmBlock) {
        case HEADER:
            return getHeaderComment();
        case METADATA:
            return metadataComment;
        case DATA_MEAN_KEPLERIAN_ELEMENTS:
            return dataMeanKeplerianElementsComment;
        case DATA_SPACECRAFT:
            return dataSpacecraftComment;
        case DATA_TLE_RELATED_PARAMETERS:
            return dataTleRelatedParametersComment;
        case DATA_COVARIANCE:
            return dataCovarianceComment;
        default: {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_ODM_BLOCK);
        }
        }
    }

    /** {@inheritDoc} */
    void setComment(final ODMBlock odmBlock, final List<String> comment) throws OrekitException {
        switch (odmBlock) {
        case HEADER:
            setHeaderComment(comment);
            break;
        case METADATA:
            metadataComment = new ArrayList<String>(comment);
            break;
        case DATA_MEAN_KEPLERIAN_ELEMENTS:
            dataMeanKeplerianElementsComment = new ArrayList<String>(comment);
            break;
        case DATA_SPACECRAFT:
            dataSpacecraftComment = new ArrayList<String>(comment);
            break;
        case DATA_TLE_RELATED_PARAMETERS:
            dataTleRelatedParametersComment = new ArrayList<String>(comment);
            break;
        case DATA_COVARIANCE:
            dataCovarianceComment = new ArrayList<String>(comment);
            break;
        default: {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_ODM_BLOCK);
        }
        }
    }

    /** Generate a {@link KeplerianOrbit} based on the OMM mean keplerian elements.
     * If the reference frame is not pseudo-inertial, an exception is raised.
     * @return the {@link KeplerianOrbit} generated from the OMM information
     * @exception OrekitException if the reference frame is not pseudo-inertial or if the central body
     * gravitational coefficient cannot be retrieved from the OMM
     */
    public KeplerianOrbit generateKeplerianOrbit() throws OrekitException {
        setMuUsed();
        if (refFrame.isPseudoInertial() ) {
            if (Double.isNaN(a)) {
                a = FastMath.cbrt(getMuUsed() / (meanMotion * meanMotion));
            }
            return new KeplerianOrbit(a, e, i, pa, raan, anomaly, PositionAngle.MEAN, refFrame, epoch, getMuUsed());
        } else {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_FOR_DEFINING_ORBITS, refFrame);
        }
    }

    /** Generate a {@link CartesianOrbit} from the {@link KeplerianOrbit}.
     * @return the {@link CartesianOrbit} generated from the OPM information
     * @throws OrekitException if the Keplerian Orbit cannot be generated
     */
    public CartesianOrbit generateCartesianOrbit() throws OrekitException {
        return new CartesianOrbit(generateKeplerianOrbit());
    }

    /** Generate spacecraft state from the {@link KeplerianOrbit} generated by generateKeplerianOrbit.
     *  Raises an exception if OMM doesn't contain spacecraft mass information.
     * @return the spacecraft state of the OMM
     * @exception OrekitException if there is no spacecraft mass associated with the OMM
     */
    public SpacecraftState generateSpacecraftState()
        throws OrekitException {
        if (Double.isNaN(mass)) {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_SPACECRAFT_MASS);
        }
        return new SpacecraftState(generateKeplerianOrbit(), mass);
    }

    /** Generate TLE from OMM file. Launch Year, Launch Day and Launch Piece are not present in the
     * OMM file, they have to be set manually by the user with the AdditionalData static class.
     * @return the tle
     */
    public TLE generateTLE() {
        return new TLE(noradID, classificationType, launchYear, launchNumber, launchPiece, ephemerisType,
                       Integer.parseInt(elementSetNo), epoch, meanMotion, meanMotionDot, meanMotionDotDot,
                       e, i, pa, raan, anomaly, revAtEpoch, bStar);
    }

}
