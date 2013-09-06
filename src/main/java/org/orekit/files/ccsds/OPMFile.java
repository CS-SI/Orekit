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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.SatelliteInformation;
import org.orekit.files.general.SatelliteTimeCoordinate;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.PVCoordinates;

/** This class gathers the informations present in the Orbital Parameter Message (OPM), and contains
 * methods to generate {@link CartesianOrbit}, {@link KeplerianOrbit} or {@link SpacecraftState}.
 * @author sports
 * @since 6.1
 */
public class OPMFile extends ODMFile {

    /** Spacecraft name for which the orbit state is provided. */
    private String objectName;

    /** Object identifier of the object for which the orbit state is provided. */
    private String objectID;

    /** Origin of reference frame. */
    private String centerName;

    /** Celestial body corresponding to the center name. */
    private CelestialBody centerBody;

    /** Reference frame in which data are given: used for state vector
     * and Keplerian elements data (and for the covariance reference frame if none is given). */
    private Frame refFrame;

    /** Epoch of reference frame, if not intrinsic to the definition of the
     * reference frame. */
    private AbsoluteDate frameEpoch;

    /** Time System: used for metadata, state vector, and maneuvers data. */
    private TimeSystem timeSystem;

    /** Time scale corresponding to the time system. */
    private TimeScale timeScale;

    /** Epoch of state vector and optional Keplerian elements. */
    private AbsoluteDate epoch;

    /** Position vector (m). */
    private Vector3D position;

    /** Velocity vector (m/s. */
    private Vector3D velocity;

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

    /** Tests whether the body corresponding to the center name can be
     * created through the {@link org.orekit.bodies.CelestialBodyFactory} in order to obtain the
     * corresponding gravitational coefficient. */
    private Boolean hasCreatableBody;

    /** Tests whether the OPM contains Keplerian elements data. */
    private boolean hasKeplerianElements;

    /** Tests whether the OPM contains covariance matrix data. */
    private boolean hasCovarianceMatrix;

    /** Metadata comments. The list contains a string for each line of comment. */
    private List<String> metadataComment;

    /** State vector data comments. The list contains a string for each line of comment. */
    private List<String> dataStateVectorComment;

    /** Keplerian elements comments. The list contains a string for each line of comment. */
    private List<String> dataKeplerianElementsComment;

    /** Spacecraft data comments. The list contains a string for each line of comment. */
    private List<String> dataSpacecraftComment;

    /** Covariance matrix data comments. The list contains a string for each line of comment. */
    private List<String> dataCovarianceComment;

    /** Maneuvers. */
    private List<Maneuver> maneuvers;

    /** Create a new OPM file object. */
    OPMFile() {
        mass = Double.NaN;
        maneuvers = new ArrayList<Maneuver>();
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
    void setObjectName(final String objectName) {
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

    /** Get the reference frame in which data are given: used for state vector
     * and Keplerian elements data (and for the covariance reference frame if none is given).
     * @return the reference frame
     */
    public Frame getFrame() {
        return refFrame;
    }

    /** Set the reference frame in which data are given: used for state vector
     * and Keplerian elements data (and for the covariance reference frame if none is given).
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

    /** Set epoch of reference frame for MET and MRT Time systems, if not intrinsic to the definition of the
     * reference frame.
     * @param offset the offset between the frame epoch and the initial date
     * */
    void setFrameEpoch(final double offset) {
        this.frameEpoch = getInitialDate().shiftedBy(offset);
    }

    /** Get the Time System: used for metadata, state vector, and maneuvers data.
     * @return the time system
     */
    public TimeSystem getTimeSystem() {
        return timeSystem;
    }

    /** Set the Time System: used for metadata, state vector, and maneuvers data.
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

    /** Set epoch of state vector and optional Keplerian elements for MET and MRT Time Systems.
     * @param offset the offset between the epoch and the initial date
     */
    void setEpoch(final double offset) {
        this.epoch = getInitialDate().shiftedBy(offset);
    }

    /** Get position vector.
     * @return the position vector
     */
    public Vector3D getPosition() {
        return position;
    }

    /** Set position vector.
     * @param position the position vector to be set
     */
    void setPosition(final Vector3D position) {
        this.position = position;
    }

    /** Get velocity vector.
     * @return the velocity vector
     */
    public Vector3D getVelocity() {
        return velocity;
    }

    /** Set velocity vector.
     * @param velocity the velocity vector to be set
     */
    void setVelocity(final Vector3D velocity) {
        this.velocity = velocity;
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

    /** Set the Position/Velocity covariance matrix.
     * @param covarianceMatrix the covariance matrix to be set
     */
    void setCovarianceMatrix(final RealMatrix covarianceMatrix) {
        this.covarianceMatrix = (Array2DRowRealMatrix) covarianceMatrix;
    }

    /** Get the number of maneuvers present in the OPM.
     * @return the number of maneuvers
     */
    public int getNbManeuvers() {
        return maneuvers.size();
    }

    /** Get a list of all maneuvers.
     * @return unmodifiable list of all maneuvers.
     */
    public List<Maneuver> getManeuvers() {
        return Collections.unmodifiableList(maneuvers);
    }

    /** Get a maneuver.
     * @param index maneuver index, counting from 0
     * @return maneuver
     */
    public Maneuver getManeuver(final int index) {
        return maneuvers.get(index);
    }

    /** Add a maneuver.
     * @param maneuver maneuver to be set
     */
    void addManeuver(final Maneuver maneuver) {
        maneuvers.add(maneuver);
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

    /** Get boolean testing whether the OPM contains Keplerian elements data.
     * @return true if OPM contains Keplerian elements data.
     *         false otherwise */
    public boolean getHasKeplerianElements() {
        return hasKeplerianElements;
    }

    /** Set boolean testing whether the OPM contains Keplerian elements data.
     * @param hasKeplerianElements the boolean to be set.
     */
    void setHasKeplerianElements(final boolean hasKeplerianElements) {
        this.hasKeplerianElements = hasKeplerianElements;
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

    /** Get boolean testing whether the OPM contains at least one maneuver.
     * @return true if OPM contains at least one maneuver
     *         false otherwise */
    public boolean getHasManeuver() {
        return !maneuvers.isEmpty();
    }

    /** {@inheritDoc}
     * Not to be used for the maneuvers data block for which there is a dedicated method.
     */
    public List<String> getComment(final ODMBlock odmBlock)
        throws OrekitException {
        switch (odmBlock) {
        case HEADER:
            return getHeaderComment();
        case METADATA:
            return metadataComment;
        case DATA_STATE_VECTOR:
            return dataStateVectorComment;
        case DATA_KEPLERIAN_ELEMENTS:
            return dataKeplerianElementsComment;
        case DATA_SPACECRAFT:
            return dataSpacecraftComment;
        case DATA_COVARIANCE:
            return dataCovarianceComment;
        default: {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_ODM_BLOCK);
        }
        }
    }

    /** {@inheritDoc}
     * Not to be used for the maneuvers data block for which there is a dedicated method.
     */
    void setComment(final ODMBlock odmBlock, final List<String> comment) throws OrekitException {
        switch (odmBlock) {
        case HEADER:
            setHeaderComment(comment);
            break;
        case METADATA:
            metadataComment = new ArrayList<String>(comment);
            break;
        case DATA_STATE_VECTOR:
            dataStateVectorComment = new ArrayList<String>(comment);
            break;
        case DATA_KEPLERIAN_ELEMENTS:
            dataKeplerianElementsComment = new ArrayList<String>(comment);
            break;
        case DATA_SPACECRAFT:
            dataSpacecraftComment = new ArrayList<String>(comment);
            break;
        case DATA_COVARIANCE:
            dataCovarianceComment = new ArrayList<String>(comment);
            break;
        case DATA_MANEUVER: {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_ODM_BLOCK);
        }
        default: {
        }
        }
    }

    /** Get the {@link SatelliteTimeCoordinate} of the OPM.
     * @return the {@link SatelliteTimeCoordinate}
     */
    public SatelliteTimeCoordinate getSatelliteCoordinatesOPM() {
        return new SatelliteTimeCoordinate(getEpoch(), getPVCoordinates());
    }

    /** Get the position/velocity coordinates contained in the OPM.
     * @return the position/velocity coordinates contained in the OPM
     */
    public PVCoordinates getPVCoordinates() {
        return new PVCoordinates(getPosition(), getVelocity());
    }

    /** {@inheritDoc} */
    @Override
    public SatelliteInformation getSatellite(final String objID) {
        return new SatelliteInformation(getObjectID());
    }

    /** {@inheritDoc} */
    @Override
    public String getCoordinateSystem() {
        return refFrame.toString();
    }

    /**
     * Generate a {@link CartesianOrbit} from the OPM state vector data. If the reference frame is not
     * pseudo-inertial, an exception is raised.
     * @return the {@link CartesianOrbit} generated from the OPM information
     * @exception OrekitException if the reference frame is not pseudo-inertial or if the central body
     * gravitational coefficient cannot be retrieved from the OPM
     */
    public CartesianOrbit generateCartesianOrbit()
        throws OrekitException {
        setMuUsed();
        if (refFrame.isPseudoInertial()) {
            return new CartesianOrbit(getPVCoordinates(), refFrame, epoch, getMuUsed());
        } else {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_FOR_DEFINING_ORBITS,
                                      refFrame);
        }
    }

    /** Generate a {@link KeplerianOrbit} from the OPM keplerian elements if hasKeplerianElements is true,
     * or from the state vector data otherwise.
     * If the reference frame is not pseudo-inertial, an exception is raised.
     * @return the {@link KeplerianOrbit} generated from the OPM information
     * @exception OrekitException if the reference frame is not pseudo-inertial or if the central body
     * gravitational coefficient cannot be retrieved from the OPM
     */
    public KeplerianOrbit generateKeplerianOrbit() throws OrekitException {
        setMuUsed();
        if (refFrame.isPseudoInertial()) {
            if (hasKeplerianElements) {
                return new KeplerianOrbit(a, e, i, pa, raan, anomaly,
                                          anomalyType, refFrame, epoch, getMuUsed());
            } else {
                return new KeplerianOrbit(getPVCoordinates(), refFrame, epoch, getMuUsed());
            }
        } else {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_FOR_DEFINING_ORBITS, refFrame);
        }
    }

    /** Generate spacecraft state from the {@link CartesianOrbit} generated by generateCartesianOrbit.
     *  Raises an exception if OPM doesn't contain spacecraft mass information.
     * @return the spacecraft state of the OPM
     * @exception OrekitException if there is no spacecraft mass associated with the OPM
     */
    public SpacecraftState generateSpacecraftState()
        throws OrekitException {
        if (Double.isNaN(mass)) {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_SPACECRAFT_MASS);
        }
        return new SpacecraftState(generateCartesianOrbit(), mass);
    }

    /** Maneuver in an OPM file.
     */
    public static class Maneuver {

        /** Epoch ignition. */
        private AbsoluteDate epochIgnition;

        /** Coordinate system for velocity increment vector, for Local Orbital Frames. */
        private LOFType refLofType;

        /** Coordinate system for velocity increment vector, for absolute frames. */
        private Frame refFrame;

        /** Duration (value is 0 for impulsive maneuver). */
        private double duration;

        /** Mass change during maneuver (value is < 0). */
        private double deltaMass;

        /** Velocity increment. */
        private Vector3D dV;

        /** Maneuvers data comment, each string in the list corresponds to one line of comment. */
        private List<String> comment;

        /** Simple constructor.
         */
        public Maneuver() {
            this.dV      = Vector3D.ZERO;
            this.comment = Collections.emptyList();
        }

        /** Get epoch ignition.
         * @return epoch ignition
         */
        public AbsoluteDate getEpochIgnition() {
            return epochIgnition;
        }

        /** Set epoch ignition.
         * @param epochIgnition epoch ignition
         */
        void setEpochIgnition(final AbsoluteDate epochIgnition) {
            this.epochIgnition = epochIgnition;
        }

        /** Get coordinate system for velocity increment vector, for Local Orbital Frames.
         * @return coordinate system for velocity increment vector, for Local Orbital Frames
         */
        public LOFType getRefLofType() {
            return refLofType;
        }

        /** Set coordinate system for velocity increment vector, for Local Orbital Frames.
         * @param refLofType coordinate system for velocity increment vector, for Local Orbital Frames
         */
        public void setRefLofType(final LOFType refLofType) {
            this.refLofType = refLofType;
            this.refFrame   = null;
        }

        /** Get Coordinate system for velocity increment vector, for absolute frames.
         * @return coordinate system for velocity increment vector, for absolute frames
         */
        public Frame getRefFrame() {
            return refFrame;
        }

        /** Set Coordinate system for velocity increment vector, for absolute frames.
         * @param refFrame coordinate system for velocity increment vector, for absolute frames
         */
        public void setRefFrame(final Frame refFrame) {
            this.refLofType = null;
            this.refFrame   = refFrame;
        }

        /** Get duration (value is 0 for impulsive maneuver).
         * @return duration (value is 0 for impulsive maneuver)
         */
        public double getDuration() {
            return duration;
        }

        /** Set duration (value is 0 for impulsive maneuver).
         * @param duration duration (value is 0 for impulsive maneuver)
         */
        public void setDuration(final double duration) {
            this.duration = duration;
        }

        /** Get mass change during maneuver (value is < 0).
         * @return mass change during maneuver (value is < 0)
         */
        public double getDeltaMass() {
            return deltaMass;
        }

        /** Set mass change during maneuver (value is < 0).
         * @param deltaMass mass change during maneuver (value is < 0)
         */
        public void setDeltaMass(final double deltaMass) {
            this.deltaMass = deltaMass;
        }

        /** Get velocity increment.
         * @return velocity increment
         */
        public Vector3D getDV() {
            return dV;
        }

        /** Set velocity increment.
         * @param dV velocity increment
         */
        public void setdV(final Vector3D dV) {
            this.dV = dV;
        }

        /** Get the maneuvers data comment, each string in the list corresponds to one line of comment.
         * @return maneuvers data comment, each string in the list corresponds to one line of comment
         */
        public List<String> getComment() {
            return Collections.unmodifiableList(comment);
        }

        /** Set the maneuvers data comment, each string in the list corresponds to one line of comment.
         * @param comment maneuvers data comment, each string in the list corresponds to one line of comment
         */
        public void setComment(final List<String> comment) {
            this.comment = new ArrayList<String>(comment);
        }

    }

}



