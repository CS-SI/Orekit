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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * This class stocks all the information of the Attitude Ephemeris Message (AEM) File parsed
 * by AEMParser. It contains the header and a list of Attitude Ephemerides Blocks each
 * containing metadata and a list of attitude ephemerides data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AEMFile extends ADMFile {

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


    /**
     * Get the attitude loaded ephemeris for each satellite in the file.
     * @return a map from the satellite's ID to the information about that satellite
     * contained in the file.
     */
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
            ret.put(id, new AemSatelliteEphemeris(entry.getValue()));
        }
        return ret;
    }

    /**
     * The Attitude Ephemerides Blocks class contain metadata
     * and the list of attitude data lines.
     */
    public class AttitudeEphemeridesBlock {

        /** Meta-data for the block. */
        private ADMMetaData metaData;

        /** The reference frame A specifier, as it appeared in the file. */
        private String refFrameAString;

        /** The reference frame B specifier, as it appeared in the file. */
        private String refFrameBString;

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

        /**
         * Get an unmodifiable list of attitude data lines.
         * @return a list of attitude data
         */
        public List<TimeStampedAngularCoordinates> getAngularCoordinates() {
            return Collections.unmodifiableList(this.attitudeDataLines);
        }

        /**
         * Get the meta-data for the block.
         * @return meta-data for the block
         */
        public ADMMetaData getMetaData() {
            return metaData;
        }

        /**
         * Get the name of the center of the coordinate system the ephemeris is provided in.
         * This may be a natural origin, such as the center of the Earth, another satellite, etc.
         * @return the name of the frame center
         */
        public String getFrameCenterString() {
            return this.getMetaData().getCenterName();
        }


        /**
         * Get the reference frame A specifier as it appeared in the file.
         * @return the frame name as it appeared in the file (A).
         */
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

        /**
         * Get the reference frame B specifier as it appeared in the file.
         * @return the frame name as it appeared in the file (B).
         */
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

        /**
         * Get the rotation direction of the attitude.
         * @return the rotation direction of the attitude
         */
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

        /**
         * Get the format of the data lines in the message.
         * @return the format of the data lines in the message
         */
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

        /**
         * Get the flag for the placement of the quaternion QC in the attitude data.
         * @return true if QC is the first element in the attitude data
         */
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

        /**
         * Get the time scale for this data segment.
         * @return the time scale identifier, as specified in the file, or
         * {@code null} if the data file does not specify a time scale.
         */
        public String getTimeScaleString() {
            return metaData.getTimeSystem().toString();
        }

        /**
         * Get the time scale for this data segment.
         * @return the time scale for this data. Never {@code null}.
         */
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

        /**
         * Get the start date of this attitude data segment.
         * @return attitude data segment start date.
         */
        public AbsoluteDate getStart() {
            // usable start time overrides start time if it is set
            final AbsoluteDate start = this.getUseableStartTime();
            if (start != null) {
                return start;
            } else {
                return this.getStartTime();
            }
        }

        /**
         * Get the end date of this attitude data segment.
         * @return attitude data segment end date.
         */
        public AbsoluteDate getStop() {
            // useable stop time overrides stop time if it is set
            final AbsoluteDate stop = this.getUseableStopTime();
            if (stop != null) {
                return stop;
            } else {
                return this.getStopTime();
            }
        }

        /**
         * Get the interpolation method to be used.
         * @return the interpolation method
         */
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

        /**
         * Get the rotation order for Euler angles.
         * @return rotation order
         */
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

    /** AEM ephemeris blocks for a single satellite. */
    public static class AemSatelliteEphemeris {

        /** The attitude ephemeris data for the satellite. */
        private final List<AttitudeEphemeridesBlock> blocks;

        /**
         * Create a container for the set of ephemeris blocks in the file that pertain to
         * a single satellite.
         * @param blocks containing ephemeris data for the satellite.
         */
        public AemSatelliteEphemeris(final List<AttitudeEphemeridesBlock> blocks) {
            this.blocks = blocks;
        }

        /**
         * Get the segments of the attitude ephemeris.
         * <p> Ephemeris segments are typically used to split an attitude ephemeris around
         * discontinuous events, such as maneuvers.
         * @return the segments contained in the attitude ephemeris file for this satellite.
         */
        public List<AttitudeEphemeridesBlock> getSegments() {
            return Collections.unmodifiableList(blocks);
        }

        /**
         * Get the start date of the attitude ephemeris.
         * @return attitude ephemeris start date.
         */
        public AbsoluteDate getStart() {
            return blocks.get(0).getStart();
        }

        /**
         * Get the end date of the attitude ephemeris.
         * @return attitude ephemeris end date.
         */
        public AbsoluteDate getStop() {
            return blocks.get(blocks.size() - 1).getStop();
        }

    }

}
