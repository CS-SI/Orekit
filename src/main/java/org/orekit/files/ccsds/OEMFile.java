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
import java.util.Map.Entry;

import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/** This class stocks all the information of the OEM File parsed by OEMParser. It
 * contains the header and a list of Ephemerides Blocks each containing
 * metadata, a list of ephemerides data lines and optional covariance matrices
 * (and their metadata).
 * @author sports
 * @author Evan Ward
 * @since 6.1
 */
public class OEMFile extends ODMFile implements EphemerisFile {

    /** List of ephemeris blocks. */
    private List<EphemeridesBlock> ephemeridesBlocks;

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

    /** Check that, according to the CCSDS standard, every OEMBlock has the same time system.
     *  @exception OrekitException if some blocks do not have the same time system
     */
    void checkTimeSystems() throws OrekitException {
        final CcsdsTimeScale timeSystem = getEphemeridesBlocks().get(0).getMetaData().getTimeSystem();
        for (final EphemeridesBlock block : ephemeridesBlocks) {
            if (!timeSystem.equals(block.getMetaData().getTimeSystem())) {
                throw new OrekitException(OrekitMessages.CCSDS_OEM_INCONSISTENT_TIME_SYSTEMS,
                                          timeSystem, block.getMetaData().getTimeSystem());
            }
        }
    }

    @Override
    public Map<String, OemSatelliteEphemeris> getSatellites() {
        final Map<String, List<EphemeridesBlock>> satellites = new HashMap<>();
        for (final EphemeridesBlock ephemeridesBlock : ephemeridesBlocks) {
            final String id = ephemeridesBlock.getMetaData().getObjectID();
            satellites.putIfAbsent(id, new ArrayList<>());
            satellites.get(id).add(ephemeridesBlock);
        }
        final Map<String, OemSatelliteEphemeris> ret = new HashMap<>();
        for (final Entry<String, List<EphemeridesBlock>> entry : satellites.entrySet()) {
            final String id = entry.getKey();
            ret.put(id, new OemSatelliteEphemeris(id, getMuUsed(), entry.getValue()));
        }
        return ret;
    }

    /** The Ephemerides Blocks class contain metadata, the list of ephemerides data
     * lines and optional covariance matrices (and their metadata). The reason
     * for which the ephemerides have been separated into blocks is that the
     * ephemerides of two different blocks are not suited for interpolation.
     * @author sports
     */
    public class EphemeridesBlock implements EphemerisSegment {

        /** Meta-data for the block. */
        private ODMMetaData metaData;

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
        private List<TimeStampedPVCoordinates> ephemeridesDataLines;

        /** True iff all data points in this block have accelleration data. */
        private boolean hasAcceleration;

        /** List of covariance matrices. */
        private List<CovarianceMatrix> covarianceMatrices;

        /** Tests whether the reference frame has an epoch associated to it. */
        private boolean hasRefFrameEpoch;

        /** Ephemerides Data Lines comments. The list contains a string for each
         * line of comment. */
        private List<String> ephemeridesDataLinesComment;

        /** EphemeridesBlock constructor. */
        public EphemeridesBlock() {
            metaData = new ODMMetaData(OEMFile.this);
            ephemeridesDataLines = new ArrayList<>();
            covarianceMatrices = new ArrayList<CovarianceMatrix>();
            hasAcceleration = true;
        }

        /** Get the list of Ephemerides data lines.
         * @return a reference to the internal list of Ephemerides data lines
         */
        List<TimeStampedPVCoordinates> getEphemeridesDataLines() {
            return this.ephemeridesDataLines;
        }

        @Override
        public CartesianDerivativesFilter getAvailableDerivatives() {
            return hasAcceleration ? CartesianDerivativesFilter.USE_PVA :
                    CartesianDerivativesFilter.USE_PV;
        }

        /**
         * Update the value of {@link #hasAcceleration}.
         *
         * @param pointHasAcceleration true iff the current data point has acceleration
         *                             data.
         */
        void updateHasAcceleration(final boolean pointHasAcceleration) {
            this.hasAcceleration = this.hasAcceleration && pointHasAcceleration;
        }

        @Override
        public List<TimeStampedPVCoordinates> getCoordinates() {
            return Collections.unmodifiableList(this.ephemeridesDataLines);
        }

        /** Get the list of Covariance Matrices.
         * @return the list of Covariance Matrices
         */
        public List<CovarianceMatrix> getCovarianceMatrices() {
            return covarianceMatrices;
        }

        /** Get the meta-data for the block.
         * @return meta-data for the block
         */
        public ODMMetaData getMetaData() {
            return metaData;
        }

        @Override
        public double getMu() {
            return getMuUsed();
        }

        @Override
        public String getFrameString() {
            return this.getMetaData().getFrameString();
        }

        @Override
        public Frame getFrame() throws OrekitException {
            return this.getMetaData().getFrame();
        }

        @Override
        public String getTimeScaleString() {
            return this.getMetaData().getTimeSystem().toString();
        }

        @Override
        public TimeScale getTimeScale() throws OrekitException {
            return this.getMetaData().getTimeSystem().getTimeScale(getConventions());
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

        @Override
        public AbsoluteDate getStart() {
            return this.getUseableStartTime();
        }

        @Override
        public AbsoluteDate getStop() {
            return this.getUseableStopTime();
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

        @Override
        public int getInterpolationSamples() {
            // From the standard it is not entirely clear how to interpret the degree.
            return getInterpolationDegree() + 1;
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

    /** OEM ephemeris blocks for a single satellite. */
    public static class OemSatelliteEphemeris implements SatelliteEphemeris {

        /** ID of the satellite. */
        private final String id;
        /** Gravitational prameter for the satellite, in m^3 / s^2. */
        private final double mu;
        /** The ephemeris data for the satellite. */
        private final List<EphemeridesBlock> blocks;

        /**
         * Create a container for the set of ephemeris blocks in the file that pertain to
         * a single satellite.
         *
         * @param id     of the satellite.
         * @param mu     standard gravitational parameter used to create orbits for the
         *               satellite, in m^3 / s^2.
         * @param blocks containing ephemeris data for the satellite.
         */
        OemSatelliteEphemeris(final String id,
                              final double mu,
                              final List<EphemeridesBlock> blocks) {
            this.id = id;
            this.mu = mu;
            this.blocks = blocks;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public double getMu() {
            return this.mu;
        }

        @Override
        public List<EphemeridesBlock> getSegments() {
            return Collections.unmodifiableList(blocks);
        }

        @Override
        public AbsoluteDate getStart() {
            return blocks.get(0).getStart();
        }

        @Override
        public AbsoluteDate getStop() {
            return blocks.get(blocks.size() - 1).getStop();
        }

    }

}
