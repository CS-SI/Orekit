/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.AttitudeEphemerisFile;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * This class stocks all the information of the Attitude Ephemeris Message (AEM) File parsed
 * by AEMParser. It contains the header and a list of Attitude Ephemerides Blocks each
 * containing metadata and a list of attitude ephemerides data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AEMFile extends ADMFile implements AttitudeEphemerisFile {

    /** List of ephemeris blocks. */
    private List<AttitudeEphemeridesBlock> attitudeBlocks;

    /**
     * AEMFile constructor.
     */
    public AEMFile() {
        attitudeBlocks = new ArrayList<AttitudeEphemeridesBlock>();
    }

    /**
     * Add a block to the list of ephemeris blocks.
     */
    public void addAttitudeBlock() {
        attitudeBlocks.add(new AttitudeEphemeridesBlock());
    }

    /**
     * Get the list of attitude ephemerides blocks as an unmodifiable list.
     * @return the list of attitude ephemerides blocks
     */
    public List<AttitudeEphemeridesBlock> getAttitudeBlocks() {
        return Collections.unmodifiableList(attitudeBlocks);
    }

    /**
     * Check that, according to the CCSDS standard, every AEMBlock has the same time system.
     */
    public void checkTimeSystems() {
        final CcsdsTimeScale timeSystem = getAttitudeBlocks().get(0).getMetaData().getTimeSystem();
        for (final AttitudeEphemeridesBlock block : attitudeBlocks) {
            if (!timeSystem.equals(block.getMetaData().getTimeSystem())) {
                throw new OrekitException(OrekitMessages.CCSDS_AEM_INCONSISTENT_TIME_SYSTEMS,
                                          timeSystem, block.getMetaData().getTimeSystem());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, AemSatelliteEphemeris> getSatellites() {
        final Map<String, List<AttitudeEphemeridesBlock>> satellites = new HashMap<>();
        for (final AttitudeEphemeridesBlock ephemeridesBlock : attitudeBlocks) {
            final String id = ephemeridesBlock.getMetaData().getObjectID();
            satellites.putIfAbsent(id, new ArrayList<>());
            satellites.get(id).add(ephemeridesBlock);
        }
        final Map<String, AemSatelliteEphemeris> ret = new HashMap<>();
        for (final Entry<String, List<AttitudeEphemeridesBlock>> entry : satellites.entrySet()) {
            final String id = entry.getKey();
            ret.put(id, new AemSatelliteEphemeris(id, entry.getValue()));
        }
        return ret;
    }


    /** AEM ephemeris blocks for a single satellite. */
    public static class AemSatelliteEphemeris implements SatelliteAttitudeEphemeris {

        /** ID of the satellite. */
        private final String id;

        /** The attitude ephemeris data for the satellite. */
        private final List<AttitudeEphemeridesBlock> blocks;

        /**
         * Create a container for the set of ephemeris blocks in the file that pertain to
         * a single satellite. The satellite's ID is set to ""
         * @param blocks containing ephemeris data for the satellite.
         * @deprecated in 10.3, replaced by AemSatelliteEphemeris(String, List)
         */
        @Deprecated
        public AemSatelliteEphemeris(final List<AttitudeEphemeridesBlock> blocks) {
            this("", blocks);
        }

        /**
         * Create a container for the set of ephemeris blocks in the file that pertain to
         * a single satellite.
         * @param id     of the satellite.
         * @param blocks containing ephemeris data for the satellite.
         * @since 10.3
         */
        public AemSatelliteEphemeris(final String id, final List<AttitudeEphemeridesBlock> blocks) {
            this.id = id;
            this.blocks = blocks;
        }

        /** {@inheritDoc} */
        @Override
        public String getId() {
            return this.id;
        }

        /** {@inheritDoc} */
        @Override
        public List<AttitudeEphemeridesBlock> getSegments() {
            return Collections.unmodifiableList(blocks);
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStart() {
            return blocks.get(0).getStart();
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStop() {
            return blocks.get(blocks.size() - 1).getStop();
        }

    }

    /**
     * The Attitude Ephemerides Blocks class contain metadata
     * and the list of attitude data lines.
     */
    public class AttitudeEphemeridesBlock implements AttitudeEphemerisSegment {

        /** Meta-data for the block. */
        private ADMMetaData metaData;

        /** The reference frame A specifier, as it appeared in the file. */
        private String refFrameAString;

        /** The reference frame B specifier, as it appeared in the file. */
        private String refFrameBString;

        /** The reference frame from which attitude is defined. */
        private Frame refFrame;

        /** Rotation direction of the attitude. */
        private String attitudeDir;

        /** Start of total time span covered by attitude data. */
        private AbsoluteDate startTime;

        /** End of total time span covered by attitude data. */
        private AbsoluteDate stopTime;

        /** Start of useable time span covered by attitude data. */
        private AbsoluteDate useableStartTime;

        /** End of useable time span covered by attitude data. */
        private AbsoluteDate useableStopTime;

        /** The format of the data lines in the message. */
        private String attitudeType;

        /** The placement of the scalar portion of the quaternion (QC) in the attitude data. */
        private boolean isFirst;

        /**
         * The rotation sequence of the Euler angles.
         * (e.g., 312, where X=1, Y=2, Z=3)
         */
        private String eulerRotSeq;

        /** The rate frame specifier, as it appeared in the file. */
        private String rateFrameString;

        /** The interpolation method to be used. */
        private String interpolationMethod;

        /** The interpolation degree. */
        private int interpolationDegree;

        /** The rotation order. */
        private RotationOrder rotationOrder;

        /** List of data lines. */
        private List<TimeStampedAngularCoordinates> attitudeDataLines;

        /** Data Lines comments. The list contains a string for each line of comment. */
        private List<String> attitudeDataLinesComment;

        /** Enumerate for selecting which derivatives to use in {@link #attitudeDataLines}. */
        private AngularDerivativesFilter angularDerivativesFilter;

        /**
         * Constructor.
         */
        public AttitudeEphemeridesBlock() {
            attitudeDataLines = new ArrayList<>();
            metaData          = new ADMMetaData(AEMFile.this);
        }

        /**
         * Get the list of attitude data lines.
         * @return a list of data
         */
        public List<TimeStampedAngularCoordinates> getAttitudeDataLines() {
            return attitudeDataLines;
        }

        /** {@inheritDoc} */
        @Override
        public List<TimeStampedAngularCoordinates> getAngularCoordinates() {
            return Collections.unmodifiableList(this.attitudeDataLines);
        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAvailableDerivatives() {
            return angularDerivativesFilter;
        }

        /**
         * Update the value of {@link #angularDerivativesFilter}.
         *
         * @param pointAngularDerivativesFilter enumerate for selecting which derivatives to use in
         *                                      attitude data.
         */
        void updateAngularDerivativesFilter(final AngularDerivativesFilter pointAngularDerivativesFilter) {
            this.angularDerivativesFilter = pointAngularDerivativesFilter;
        }

        /**
         * Get the meta-data for the block.
         * @return meta-data for the block
         */
        public ADMMetaData getMetaData() {
            return metaData;
        }

        /** {@inheritDoc} */
        @Override
        public String getFrameCenterString() {
            return this.getMetaData().getCenterName();
        }

        /** {@inheritDoc} */
        @Override
        public String getRefFrameAString() {
            return refFrameAString;
        }

        /**
         * Set the reference frame A name.
         * @param frame specifier as it appeared in the file.
         */
        public void setRefFrameAString(final String frame) {
            this.refFrameAString = frame;
        }

        /** {@inheritDoc} */
        @Override
        public String getRefFrameBString() {
            return this.refFrameBString;
        }

        /**
         * Set the reference frame B name.
         * @param frame specifier as it appeared in the file.
         */
        public void setRefFrameBString(final String frame) {
            this.refFrameBString = frame;
        }

        /**
         * Get the reference frame from which attitude is defined.
         * @param frame reference frame
         */
        public void setReferenceFrame(final Frame frame) {
            this.refFrame = frame;

        }

        /** {@inheritDoc} */
        @Override
        public Frame getReferenceFrame() {
            return refFrame;
        }

        /**
         * Get the rate frame specifier as it appeared in the file.
         * @return the rate frame name.
         */
        public String getRateFrameString() {
            return rateFrameString;
        }

        /**
         * Set the rate frame name.
         * @param frame specifier as it appeared in the file.
         */
        public void setRateFrameString(final String frame) {
            this.rateFrameString = frame;
        }

        /** {@inheritDoc} */
        @Override
        public String getAttitudeDirection() {
            return attitudeDir;
        }

        /**
         * Set the rotation direction of the attitude.
         * @param direction rotation direction to be set
         */
        public void setAttitudeDirection(final String direction) {
            this.attitudeDir = direction;
        }

        /** {@inheritDoc} */
        @Override
        public String getAttitudeType() {
            return attitudeType;
        }

        /**
         * Set the format of the data lines in the message.
         * @param type format to be set
         */
        public void setAttitudeType(final String type) {
            this.attitudeType = type;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isFirst() {
            return isFirst;
        }

        /**
         * Set the flag for the placement of the quaternion QC in the attitude data.
         * @param isFirst true if QC is the first element in the attitude data
         */
        public void setIsFirst(final boolean isFirst) {
            this.isFirst = isFirst;
        }

        /**
         * Get the rotation sequence of the Euler angles (e.g., 312, where X=1, Y=2, Z=3).
         * @return the rotation sequence of the Euler angles
         */
        public String getEulerRotSeq() {
            return eulerRotSeq;
        }

        /**
         * Set the rotation sequence of the Euler angles (e.g., 312, where X=1, Y=2, Z=3).
         * @param eulerRotSeq rotation sequence to be set
         */
        public void setEulerRotSeq(final String eulerRotSeq) {
            this.eulerRotSeq = eulerRotSeq;
        }

        /** {@inheritDoc} */
        @Override
        public String getTimeScaleString() {
            return metaData.getTimeSystem().toString();
        }

        /** {@inheritDoc} */
        @Override
        public TimeScale getTimeScale() {
            return metaData.getTimeScale();
        }

        /**
         * Get start of total time span covered by attitude data.
         * @return the start time
         */
        public AbsoluteDate getStartTime() {
            return startTime;
        }

        /**
         * Set start of total time span covered by attitude data.
         * @param startTime the time to be set
         */
        public void setStartTime(final AbsoluteDate startTime) {
            this.startTime = startTime;
        }

        /**
         * Get end of total time span covered by attitude data.
         * @return the stop time
         */
        public AbsoluteDate getStopTime() {
            return stopTime;
        }

        /**
         * Set end of total time span covered by attitude data.
         * @param stopTime the time to be set
         */
        public void setStopTime(final AbsoluteDate stopTime) {
            this.stopTime = stopTime;
        }

        /**
         * Get start of useable time span covered by attitude data.
         * @return the useable start time
         */
        public AbsoluteDate getUseableStartTime() {
            return useableStartTime;
        }

        /**
         * Set start of useable time span covered by attitude data.
         * @param useableStartTime the time to be set
         */
        public void setUseableStartTime(final AbsoluteDate useableStartTime) {
            this.useableStartTime = useableStartTime;
        }

        /**
         * Get end of useable time span covered by ephemerides data.
         * @return the useable stop time
         */
        public AbsoluteDate getUseableStopTime() {
            return useableStopTime;
        }

        /**
         * Set end of useable time span covered by ephemerides data.
         * @param useableStopTime the time to be set
         */
        public void setUseableStopTime(final AbsoluteDate useableStopTime) {
            this.useableStopTime = useableStopTime;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStart() {
            // usable start time overrides start time if it is set
            final AbsoluteDate start = this.getUseableStartTime();
            if (start != null) {
                return start;
            } else {
                return this.getStartTime();
            }
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getStop() {
            // useable stop time overrides stop time if it is set
            final AbsoluteDate stop = this.getUseableStopTime();
            if (stop != null) {
                return stop;
            } else {
                return this.getStopTime();
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getInterpolationMethod() {
            return interpolationMethod;
        }

        /**
         * Set the interpolation method to be used.
         * @param interpolationMethod the interpolation method to be set
         */
        public void setInterpolationMethod(final String interpolationMethod) {
            this.interpolationMethod = interpolationMethod;
        }

        /**
         * Get the interpolation degree.
         * @return the interpolation degree
         */
        public int getInterpolationDegree() {
            return interpolationDegree;
        }

        /**
         * Set the interpolation degree.
         * @param interpolationDegree the interpolation degree to be set
         */
        public void setInterpolationDegree(final int interpolationDegree) {
            this.interpolationDegree = interpolationDegree;
        }

        /** {@inheritDoc} */
        @Override
        public int getInterpolationSamples() {
            // From the standard it is not entirely clear how to interpret the degree.
            return getInterpolationDegree() + 1;
        }

        /** Get the attitude data lines comment.
         * @return the comment
         */
        public List<String> getAttitudeDataLinesComment() {
            return attitudeDataLinesComment;
        }

        /** Set the attitude data lines comment.
         * @param ephemeridesDataLinesComment the comment to be set
         */
        public void setAttitudeDataLinesComment(final List<String> ephemeridesDataLinesComment) {
            this.attitudeDataLinesComment = new ArrayList<String>(ephemeridesDataLinesComment);
        }

        /** {@inheritDoc} */
        @Override
        public RotationOrder getRotationOrder() {
            return rotationOrder;
        }

        /**
         * Set the rotation order for Euler angles.
         * @param order the rotation order to be set
         */
        public void setRotationOrder(final RotationOrder order) {
            this.rotationOrder = order;
        }

    }


}
