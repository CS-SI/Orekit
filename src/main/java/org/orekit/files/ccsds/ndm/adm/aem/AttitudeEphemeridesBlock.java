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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.files.ccsds.ndm.NDMData;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.general.AttitudeEphemerisFile.AttitudeEphemerisSegment;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * The Attitude Ephemerides Blocks class contain metadata
 * and the list of attitude data lines.
 * @author Bryan Cazabonne
 */
public class AttitudeEphemeridesBlock implements NDMData, AttitudeEphemerisSegment {

    /** Center of the coordinate system the ephemeris is provided in. */
    private final String frameCenter;

    /** Time system. */
    private final CcsdsTimeScale timeSystem;

    /** Time scale. */
    private final TimeScale timeScale;

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
    private final List<TimeStampedAngularCoordinates> attitudeDataLines;

    /** Data Lines comments. The list contains a string for each line of comment. */
    private List<String> attitudeDataLinesComment;

    /** Enumerate for selecting which derivatives to use in {@link #attitudeDataLines}. */
    private AngularDerivativesFilter angularDerivativesFilter;

    /**
     * Constructor.
     * @param file message file these data belong to
     * @param metadata metadata associated with these data
     */
    public AttitudeEphemeridesBlock(final AEMFile file, final AEMMetadata metadata) {
        this.frameCenter       = metadata.getCenterName();
        this.timeSystem        = metadata.getTimeSystem();
        this.timeScale         = timeSystem.getTimeScale(file.getConventions(),
                                                         file.getDataContext().getTimeScales());
        this.attitudeDataLines = new ArrayList<>();
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

    /** {@inheritDoc} */
    @Override
    public String getFrameCenterString() {
        return frameCenter;
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
        return timeSystem.toString();
    }

    /** {@inheritDoc} */
    @Override
    public TimeScale getTimeScale() {
        return timeScale;
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
