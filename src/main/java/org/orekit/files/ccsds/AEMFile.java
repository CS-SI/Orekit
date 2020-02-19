/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/**
 * This class stocks all the information of the Attitude Ephemeris Message (AEM) File parsed
 * by AEMParser. It contains the header and a list of Attitude Ephemerides Blocks each
 * containing metadata and a list of ephemerides data lines.
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
    void addAttitudeBlock() {
        attitudeBlocks.add(new AttitudeEphemeridesBlock());
    }

    /**
     * Get the list of attitude ephemerides blocks as an unmodifiable list.
     * @return the list of ephemerides blocks
     */
    public List<AttitudeEphemeridesBlock> getAttitudeBlocks() {
        return Collections.unmodifiableList(attitudeBlocks);
    }

    /**
     * Check that, according to the CCSDS standard, every AEMBlock has the same time system.
     */
    void checkTimeSystems() {
        final CcsdsTimeScale timeSystem = getAttitudeBlocks().get(0).getMetaData().getTimeSystem();
        for (final AttitudeEphemeridesBlock block : attitudeBlocks) {
            if (!timeSystem.equals(block.getMetaData().getTimeSystem())) {
                throw new OrekitException(OrekitMessages.CCSDS_AEM_INCONSISTENT_TIME_SYSTEMS,
                                          timeSystem, block.getMetaData().getTimeSystem());
            }
        }
    }

    /**
     * The Attitude Ephemerides Blocks class contain metadata
     * and the list of attitude data lines.
     */
    public class AttitudeEphemeridesBlock {

        /** Meta-data for the block. */
        private ADMMetaData metaData;

        /** The reference frame specifying one frame of the transformation. */
        private Frame refFrameA;

        /** The reference frame specifying the second portion of the transformation. */
        private Frame refFrameB;

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
        private int eulerRotSeq;

        /** The frame of reference in which Euler rates are specified. */
        private Frame rateFrame;

        /** The interpolation method to be used. */
        private String interpolationMethod;

        /** The interpolation degree. */
        private int interpolationDegree;

        /** List of data lines. */
        private List<Attitude> attitudeDataLines;

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
        public List<Attitude> getAttitudeDataLines() {
            return attitudeDataLines;
        }

        /**
         * Get the meta-data for the block.
         * @return meta-data for the block
         */
        public ADMMetaData getMetaData() {
            return metaData;
        }

        /**
         * Get the reference frame specifying one frame of the transformation.
         * @return the reference frame A
         */
        public Frame getRefFrameA() {
            return refFrameA;
        }

        /**
         * Set the reference frame specifying one frame of the transformation.
         * @param refFrameA the frame to be set
         */
        void setRefFrameA(final Frame refFrameA) {
            this.refFrameA = refFrameA;
        }

        /**
         * Get the reference frame specifying the second portion of the transformation.
         * @return the reference frame B
         */
        public Frame getRefFrameB() {
            return refFrameB;
        }

        /**
         * Set the reference frame specifying the second portion of the transformation.
         * @param refFrameB the frame to be set
         */
        void setRefFrameB(final Frame refFrameB) {
            this.refFrameB = refFrameB;
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
        void setAttitudeDirection(final String direction) {
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
        void setAttitudeType(final String type) {
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
        void setIsFirst(final boolean isFirst) {
            this.isFirst = isFirst;
        }

        /**
         * Get the rotation sequence of the Euler angles (e.g., 312, where X=1, Y=2, Z=3).
         * @return the rotation sequence of the Euler angles
         */
        public int getEulerRotSeq() {
            return eulerRotSeq;
        }

        /**
         * Set the rotation sequence of the Euler angles (e.g., 312, where X=1, Y=2, Z=3).
         * @param eulerRotSeq rotation sequence to be set
         */
        void setEulerRotSeq(final int eulerRotSeq) {
            this.eulerRotSeq = eulerRotSeq;
        }

        /**
         * Get the frame of reference in which Euler rates are specified.
         * @return reference frame in which Euler rates are specified
         */
        public Frame getRateFrame() {
            return rateFrame;
        }

        /**
         * Set the frame of reference in which Euler rates are specified.
         * @param rateFrame frame to be set
         */
        void setRateFrame(final Frame rateFrame) {
            this.rateFrame = rateFrame;
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
        void setStartTime(final AbsoluteDate startTime) {
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
        void setStopTime(final AbsoluteDate stopTime) {
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
        void setUseableStartTime(final AbsoluteDate useableStartTime) {
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
        void setUseableStopTime(final AbsoluteDate useableStopTime) {
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
        void setInterpolationMethod(final String interpolationMethod) {
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
        void setInterpolationDegree(final int interpolationDegree) {
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
        void setAttitudeDataLinesComment(final List<String> ephemeridesDataLinesComment) {
            this.attitudeDataLinesComment = new ArrayList<String>(ephemeridesDataLinesComment);
        }

    }
}
