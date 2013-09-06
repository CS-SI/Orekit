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
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealMatrix;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/** This class stocks all the information of the OEM File parsed by OEMParser. It
 * contains the header and a list of Ephemerides Blocks each containing
 * metadata, a list of ephemerides data lines and optional covariance matrices
 * (and their metadata).
 * @author sports
 * @since 6.1
 */
public class OEMFile
    extends ODMFile {

    /** List of ephemeris blocks. */
    private List<EphemeridesBlock> ephemeridesBlocks;

    /** Time System of the OEMFile (should be common to every ephemeris block). */
    private TimeSystem timeSystem;

    /** OEMFile constructor. */
    public OEMFile() {
        ephemeridesBlocks = new ArrayList<EphemeridesBlock>();
    }

    /** Add a block to the list of ephemeris blocks. */
    void addEphemeridesBlock() {
        ephemeridesBlocks.add(new EphemeridesBlock());
    }

    /**Get the list of ephemerides blocks as an unmodifiable list.
     * @return the list of ephemerides blocks
     */
    public List<EphemeridesBlock> getEphemeridesBlocks() {
        return Collections.unmodifiableList(ephemeridesBlocks);
    }

    @Override
    public TimeSystem getTimeSystem() {
        return timeSystem;
    }

    /** Set the OEMFile time system after verifying that, according to the CCSDS standard,
     *  every OEMBlock has the same time system.
     *  @exception OrekitException if some blocks do not have the same time system
     */
    void setTimeSystem() throws OrekitException {
        boolean sameTimeSystem = true;
        for (int i = 1; i < ephemeridesBlocks.size(); i++) {
            if (getEphemeridesBlocks().get(i).getTimeSystem()
                .equals(getEphemeridesBlocks().get(i - 1).getTimeSystem()) == false) {
                sameTimeSystem = false;
                break;
            }
        }
        if (sameTimeSystem) {
            this.timeSystem = getEphemeridesBlocks().get(0).getTimeSystem();
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_OEM_TIMESYSTEM_NOT_IDENTICAL);
        }
    }

    @Override
    public List<String> getComment(final ODMBlock odmBlock)
        throws OrekitException {
        if (odmBlock.equals(ODMBlock.HEADER)) {
            return getHeaderComment();
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_ODM_BLOCK);
        }
    }

    @Override
    public void setComment(final ODMBlock odmBlock, final List<String> comment)
        throws OrekitException {
        if (odmBlock.equals(ODMBlock.HEADER)) {
            setHeaderComment(comment);
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_ODM_BLOCK);
        }
    }

    /** The Ephemerides Blocks class contain metadata, the list of ephemerides data
     * lines and optional covariance matrices (and their metadata). The reason
     * for which the ephemerides have been separated into blocks is that the
     * ephemerides of two different blocks are not suited for interpolation.
     * @author sports
     */
    public class EphemeridesBlock {

        /** Spacecraft name for which the orbit state is provided. */
        private String objectName;

        /**Object identifier of the object for which the orbit state is
         * provided. */
        private String objectID;

        /** Origin of reference frame. */
        private String centerName;

        /** Celestial body corresponding to the centerName. */
        private CelestialBody centerBody;

        /** Reference frame in which data are given: used for ephemerides data. */
        private Frame refFrame;

        /** Epoch of reference frame, if not intrinsic to the definition of the
         * reference frame. */
        private AbsoluteDate frameEpoch;

        /** Time System: used for metadata, ephemerides and
         * covariance data. */
        private TimeSystem timeSystem;

        /** Time scale corresponding to the timeSystem. */
        private TimeScale timeScale;

        /** Start of total time span covered by ephemerides data and covariance
         * data. */
        private AbsoluteDate startTime;

        /** End of total time span covered by ephemerides data and covariance
         * data. */
        private AbsoluteDate stopTime;

        /** Start of useable time span covered by ephemerides data, it may be
         * necessary to allow for proper interpolation. */
        private AbsoluteDate useableStartTime;

        /** End of useable time span covered by ephemerides data, it may be
         * necessary to allow for proper interpolation. */
        private AbsoluteDate useableStopTime;

        /** The interpolation method to be used. */
        private String interpolationMethod;

        /** The interpolation degree. */
        private int interpolationDegree;

        /** List of ephemerides data lines. */
        private List<EphemeridesDataLine> ephemeridesDataLines;

        /** List of covariance matrices. */
        private List<CovarianceMatrix> covarianceMatrices;

        /** Tests whether the reference frame has an epoch associated to it. */
        private boolean hasRefFrameEpoch;

        /** Tests whether the body corresponding to the centerName attribute can
         * be created through the {@link org.orekit.bodies.CelestialBodyFactory} in order to
         * obtain the corresponding gravitational coefficient. */
        private boolean hasCreatableBody;

        /** Metadata comments. The list contains a string for each line of
         * comment. */
        private List<String> metadataComment;

        /** Ephemerides Data Lines comments. The list contains a string for each
         * line of comment. */
        private List<String> ephemeridesDataLinesComment;

        /** EphemeridesBlock constructor. */
        public EphemeridesBlock() {
            ephemeridesDataLines = new ArrayList<EphemeridesDataLine>();
            covarianceMatrices = new ArrayList<CovarianceMatrix>();
            hasCreatableBody = false;
        }

        /** Get the list of Ephemerides data lines.
         * @return the list of Ephemerides data lines
         */
        public List<EphemeridesDataLine> getEphemeridesDataLines() {
            return ephemeridesDataLines;
        }

        /** Get the list of Covariance Matrices.
         * @return the list of Covariance Matrices
         */
        public List<CovarianceMatrix> getCovarianceMatrices() {
            return covarianceMatrices;
        }

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
         * @param centerName the origin of reference frame.
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

        /** Get the reference frame used for ephemerides data.
         * @return the reference frame
         */
        public Frame getFrame() {
            return refFrame;
        }

        /** Set the reference frame used for ephemerides data.
         * @param refFrame the reference frame to be set
         */
        void setRefFrame(final Frame refFrame) {
            this.refFrame = refFrame;
        }

        /** Get epoch of reference frame, if not intrinsic to the definition of
         * the reference frame.
         * @return epoch of reference frame
         */
        public AbsoluteDate getFrameEpoch() {
            return frameEpoch;
        }

        /** Set epoch of reference frame, if not intrinsic to the definition of
         * the reference frame.
         * @param frameEpoch the epoch of reference frame to be set
         */
        void setFrameEpoch(final AbsoluteDate frameEpoch) {
            this.frameEpoch = frameEpoch;
        }

        /** Set epoch of reference frame for MET and MRT time systems, if not
         * intrinsic to the definition of the reference frame.
         * @param offset the offset between the epoch and the initial date
         */
        void setFrameEpoch(final double offset) {
            this.frameEpoch = getInitialDate().shiftedBy(offset);
        }

        /** Get the Time System that is used for metadata,
         * ephemeris and covariance data.
         * @return the time system
         */
        public TimeSystem getTimeSystem() {
            return timeSystem;
        }

        /** Set the Time System that is used for metadata,
         * ephemerides and covariance data.
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

        /** Get start of total time span covered by ephemerides data and
         * covariance data.
         * @return the start time
         */
        public AbsoluteDate getStartTime() {
            return startTime;
        }

        /** Set start of total time span covered by ephemerides data and
         * covariance data.
         * @param startTime the time to be set
         */
        void setStartTime(final AbsoluteDate startTime) {
            this.startTime = startTime;
        }

        /** Set start of total time span covered by ephemerides data and
         * covariance data, for MET and MRT Time systems.
         * @param offset the offset between start time and initial date.
         */
        void setStartTime(final double offset) {
            this.startTime = getInitialDate().shiftedBy(offset);
        }

        /** Get end of total time span covered by ephemerides data and covariance
         * data.
         * @return the stop time
         */
        public AbsoluteDate getStopTime() {
            return stopTime;
        }

        /** Set end of total time span covered by ephemerides data and covariance
         * data.
         * @param stopTime the time to be set
         */
        void setStopTime(final AbsoluteDate stopTime) {
            this.stopTime = stopTime;
        }

        /** Set end of total time span covered by ephemerides data and covariance
         * data, for MET and MRT Time systems.
         * @param offset the offset between stop time and initial date.
         */
        void setStopTime(final double offset) {
            this.stopTime = getInitialDate().shiftedBy(offset);
        }

        /** Get start of useable time span covered by ephemerides data, it may be
         * necessary to allow for proper interpolation.
         * @return the useable start time
         */
        public AbsoluteDate getUseableStartTime() {
            return useableStartTime;
        }

        /** Set start of useable time span covered by ephemerides data, it may be
         * necessary to allow for proper interpolation.
         * @param useableStartTime the time to be set
         */
        void setUseableStartTime(final AbsoluteDate useableStartTime) {
            this.useableStartTime = useableStartTime;
        }

        /** Set start of useable time span covered by ephemerides data, it may be
         * necessary to allow for proper interpolation, for MET and MRT Time
         * systems.
         * @param offset the offset between useable start time and initial date.
         */
        void setUseableStartTime(final double offset) {
            this.useableStartTime = getInitialDate().shiftedBy(offset);
        }

        /** Get end of useable time span covered by ephemerides data, it may be
         * necessary to allow for proper interpolation.
         * @return the useable stop time
         */
        public AbsoluteDate getUseableStopTime() {
            return useableStopTime;
        }

        /** Set end of useable time span covered by ephemerides data, it may be
         * necessary to allow for proper interpolation.
         * @param useableStopTime the time to be set
         */
        void setUseableStopTime(final AbsoluteDate useableStopTime) {
            this.useableStopTime = useableStopTime;
        }

        /** Set end of useable time span covered by ephemerides data, it may be
         * necessary to allow for proper interpolation, for MET and MRT Time
         * systems.
         * @param offset the offset between useable stop time and initial date.
         */
        void setUseableStopTime(final double offset) {
            this.useableStopTime = getInitialDate().shiftedBy(offset);
        }

        /** Get the interpolation method to be used.
         * @return the interpolation method
         */
        public String getInterpolationMethod() {
            return interpolationMethod;
        }

        /** Set the interpolation method to be used.
         * @param interpolationMethod the interpolation method to be set
         */
        void setInterpolationMethod(final String interpolationMethod) {
            this.interpolationMethod = interpolationMethod;
        }

        /** Get the interpolation degree.
         * @return the interpolation degree
         */
        public int getInterpolationDegree() {
            return interpolationDegree;
        }

        /** Set the interpolation degree.
         * @param interpolationDegree the interpolation degree to be set
         */
        void setInterpolationDegree(final int interpolationDegree) {
            this.interpolationDegree = interpolationDegree;
        }

        /** Get boolean testing whether the reference frame has an epoch associated to it.
         * @return true if the reference frame has an epoch associated to it
         *         false otherwise
         */
        public boolean getHasRefFrameEpoch() {
            return hasRefFrameEpoch;
        }

        /** Set boolean testing whether the reference frame has an epoch associated to it.
         * @param hasRefFrameEpoch the boolean to be set.
         */
        void setHasRefFrameEpoch(final boolean hasRefFrameEpoch) {
            this.hasRefFrameEpoch = hasRefFrameEpoch;
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

        /** Get the metadata comment.
         * @return the comment
         */
        public List<String> getMetadataComment() {
            return metadataComment;
        }

        /** Set the metadata comment.
         * @param metadataComment the comment to be set
         */
        void setMetadataComment(final List<String> metadataComment) {
            this.metadataComment = new ArrayList<String>(metadataComment);
        }

        /** Get the ephemerides data lines comment.
         * @return the comment
         */
        public List<String> getEphemeridesDataLinesComment() {
            return ephemeridesDataLinesComment;
        }

        /** Set the ephemerides data lines comment.
         * @param ephemeridesDataLinesComment the comment to be set
         */
        void setEphemeridesDataLinesComment(final List<String> ephemeridesDataLinesComment) {
            this.ephemeridesDataLinesComment = new ArrayList<String>(ephemeridesDataLinesComment);
        }
    }

    /** The EphemeridesDataLine class represents the content of an OEM ephemerides
     * data line and consists of a cartesian orbit and an optional acceleration
     * vector.
     * @author sports
     */
    public static class EphemeridesDataLine {

        /** The cartesian orbit relative to the ephemeris. */
        private CartesianOrbit orbit;

        /** The acceleration vector. */
        private Vector3D acceleration;

        /** The EphemeridesDataLine constructor.
         * @param orbit the orbit corresponding to the ephemeris
         * @param acceleration the acceleration vector
         */
        EphemeridesDataLine(final CartesianOrbit orbit, final Vector3D acceleration) {
            this.acceleration = acceleration;
            this.orbit = orbit;
        }

        /** Get the ephemerides data line orbit.
         * @return the orbit
         */
        public CartesianOrbit getOrbit() {
            return orbit;
        }

        /** Get the ephemerides data line acceleration vector.
         * @return the acceleration vector
         */
        public Vector3D getAcceleration() {
            return acceleration;
        }

    }

    /** The CovarianceMatrix class represents a covariance matrix and its
     * metadata: epoch and frame.
     * @author sports
     */
    public static class CovarianceMatrix {

        /** Covariance matrix. */
        private RealMatrix matrix;

        /** Epoch relative to the covariance matrix. */
        private AbsoluteDate epoch;

        /** Coordinate system for covariance matrix, for Local Orbital Frames. */
        private LOFType lofType;

        /** Coordinate system for covariance matrix, for absolute frames.
         * If not given it is set equal to refFrame. */
        private Frame frame;

        /** Covariance Matrix constructor.
         * @param epoch the epoch
         * @param lofType coordinate system for covariance matrix, for Local Orbital Frames
         * @param frame coordinate system for covariance matrix, for absolute frames
         * @param lastMatrix the covariance matrix
         */
        CovarianceMatrix(final AbsoluteDate epoch,
                         final LOFType lofType, final Frame frame,
                         final RealMatrix lastMatrix) {
            this.matrix  = lastMatrix;
            this.epoch   = epoch;
            this.lofType = lofType;
            this.frame   = frame;
        }

        /** Get the covariance matrix.
         * @return the covariance matrix
         */
        public RealMatrix getMatrix() {
            return matrix;
        }

        /** Get the epoch relative to the covariance matrix.
         * @return the epoch
         */
        public AbsoluteDate getEpoch() {
            return epoch;
        }

        /** Get coordinate system for covariance matrix, for Local Orbital Frames.
         * <p>
         * The value returned is null if the covariance matrix is given in an
         * absolute frame rather than a Local Orbital Frame. In this case, the
         * method {@link #getFrame()} must be used instead.
         * </p>
         * @return the coordinate system for covariance matrix, or null if the
         * covariance matrix is given in an absolute frame rather than a Local
         * Orbital Frame
         */
        public LOFType getLofType() {
            return lofType;
        }

        /** Get coordinate system for covariance matrix, for absolute frames.
         * <p>
         * The value returned is null if the covariance matrix is given in a
         * Local Orbital Frame rather than an absolute frame. In this case, the
         * method {@link #getLofType()} must be used instead.
         * </p>
         * @return the coordinate system for covariance matrix
         */
        public Frame getFrame() {
            return frame;
        }

    }


}
