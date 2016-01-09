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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.OrbitFile;
import org.orekit.files.general.SatelliteInformation;
import org.orekit.files.general.SatelliteTimeCoordinate;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.time.AbsoluteDate;

/** This class stocks all the information of the OEM File parsed by OEMParser. It
 * contains the header and a list of Ephemerides Blocks each containing
 * metadata, a list of ephemerides data lines and optional covariance matrices
 * (and their metadata).
 * @author sports
 * @since 6.1
 */
public class OEMFile extends ODMFile {

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
        final OrbitFile.TimeSystem timeSystem = getEphemeridesBlocks().get(0).getMetaData().getTimeSystem();
        for (final EphemeridesBlock block : ephemeridesBlocks) {
            if (!timeSystem.equals(block.getMetaData().getTimeSystem())) {
                throw new OrekitException(OrekitMessages.CCSDS_OEM_INCONSISTENT_TIME_SYSTEMS,
                                          timeSystem, block.getMetaData().getTimeSystem());
            }
        }
    }

    /** {@inheritDoc}
     * <p>
     * We return here only the coordinate systems of the first ephemerides block.
     * </p>
     */
    @Override
    public String getCoordinateSystem() {
        return ephemeridesBlocks.get(0).getMetaData().getFrame().toString();
    }

    /** {@inheritDoc} */
    @Override
    public OrbitFile.TimeSystem getTimeSystem() {
        return ephemeridesBlocks.get(0).getMetaData().getTimeSystem();
    }

    /** {@inheritDoc}
     * <p>
     * We return here only the start time of the first ephemerides block.
     * </p>
     */
    @Override
    public AbsoluteDate getEpoch() {
        return ephemeridesBlocks.get(0).getStartTime();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<SatelliteInformation> getSatellites() {
        final Set<String> availableSatellites = getAvailableSatelliteIds();
        final List<SatelliteInformation> satellites =
                new ArrayList<SatelliteInformation>(availableSatellites.size());
        for (String satId : availableSatellites) {
            satellites.add(new SatelliteInformation(satId));
        }
        return satellites;
    }

    /** {@inheritDoc} */
    @Override
    public int getSatelliteCount() {
        return getAvailableSatelliteIds().size();
    }

    /** {@inheritDoc} */
    @Override
    public SatelliteInformation getSatellite(final String satId) {
        final Set<String> availableSatellites = getAvailableSatelliteIds();
        if (availableSatellites.contains(satId)) {
            return new SatelliteInformation(satId);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<SatelliteTimeCoordinate> getSatelliteCoordinates(final String satId) {
        // first we collect all available EphemeridesBlocks for this satellite
        // and return a list view of the actual EphemeridesBlocks transforming the
        // EphemeridesDataLines into SatelliteTimeCoordinates in a lazy manner.
        final List<Pair<Integer, Integer>> ephemeridesBlockMapping = new ArrayList<Pair<Integer, Integer>>();
        final ListIterator<EphemeridesBlock> it = ephemeridesBlocks.listIterator();
        int totalDataLines = 0;
        while (it.hasNext()) {
            final int index = it.nextIndex();
            final EphemeridesBlock block = it.next();

            if (block.getMetaData().getObjectID().equals(satId)) {
                final int dataLines = block.getEphemeridesDataLines().size();
                totalDataLines += dataLines;
                ephemeridesBlockMapping.add(new Pair<Integer, Integer>(index, dataLines));
            }
        }

        // the total number of coordinates for this satellite
        final int totalNumberOfCoordinates = totalDataLines;

        return new AbstractList<SatelliteTimeCoordinate>() {

            @Override
            public SatelliteTimeCoordinate get(final int index) {
                if (index < 0 || index >= size()) {
                    throw new IndexOutOfBoundsException();
                }

                // find the corresponding ephemerides block and data line
                int ephemeridesBlockIndex = -1;
                int dataLineIndex = index;
                for (Pair<Integer, Integer> pair : ephemeridesBlockMapping) {
                    if (dataLineIndex < pair.getValue()) {
                        ephemeridesBlockIndex = pair.getKey();
                        break;
                    } else {
                        dataLineIndex -= pair.getValue();
                    }
                }

                if (ephemeridesBlockIndex == -1 || dataLineIndex == -1) {
                    throw new IndexOutOfBoundsException();
                }

                final EphemeridesDataLine dataLine =
                        ephemeridesBlocks.get(ephemeridesBlockIndex).getEphemeridesDataLines().get(dataLineIndex);
                final CartesianOrbit orbit = dataLine.getOrbit();
                return new SatelliteTimeCoordinate(orbit.getDate(), orbit.getPVCoordinates());
            }

            @Override
            public int size() {
                return totalNumberOfCoordinates;
            }

        };
    }

    /** Returns a set of all available satellite Ids in this OEMFile.
     * @return a set of all available satellite Ids
     */
    private Set<String> getAvailableSatelliteIds() {
        final Set<String> availableSatellites = new LinkedHashSet<String>();
        for (EphemeridesBlock block : ephemeridesBlocks) {
            availableSatellites.add(block.getMetaData().getObjectID());
        }
        return availableSatellites;
    }

    /** The Ephemerides Blocks class contain metadata, the list of ephemerides data
     * lines and optional covariance matrices (and their metadata). The reason
     * for which the ephemerides have been separated into blocks is that the
     * ephemerides of two different blocks are not suited for interpolation.
     * @author sports
     */
    public class EphemeridesBlock {

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
        private List<EphemeridesDataLine> ephemeridesDataLines;

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
            ephemeridesDataLines = new ArrayList<EphemeridesDataLine>();
            covarianceMatrices = new ArrayList<CovarianceMatrix>();
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

        /** Get the meta-data for the block.
         * @return meta-data for the block
         */
        public ODMMetaData getMetaData() {
            return metaData;
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
