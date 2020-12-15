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

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.files.ccsds.ndm.adm.ADMMetadata;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** This class gathers the meta-data present in the Attitude Data Message (ADM).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AEMMetadata extends ADMMetadata {

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

    /**
     * Get the reference frame A specifier as it appeared in the file.
     *
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
     *
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
     * Get the reference frame from which attitude is defined.
     * @param frame reference frame
     */
    public void setReferenceFrame(final Frame frame) {
        this.refFrame = frame;

    }

    /**
     * Get the reference frame from which attitude is defined.
     *
     * @return the reference frame from which attitude is defined
     */
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

    /**
     * Get the rotation direction of the attitude.
     *
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
     *
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
     *
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
     * Get the time scale for this ephemeris segment.
     *
     * @return the time scale identifier, as specified in the ephemeris file, or
     * {@code null} if the ephemeris file does not specify a time scale.
     */
    public String getTimeScaleString() {
        return getTimeSystem() == null ? null : getTimeSystem().toString();
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
     * Get the start date of this ephemeris segment.
     *
     * @return ephemeris segment start date.
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
     * Get the end date of this ephemeris segment.
     *
     * @return ephemeris segment end date.
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
     *
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

    /**
     * Get the number of samples to use in interpolation.
     *
     * @return the number of points to use for interpolation.
     */
    public int getInterpolationSamples() {
        // From the standard it is not entirely clear how to interpret the degree.
        return getInterpolationDegree() + 1;
    }

    /**
     * Get which derivatives of angular data are available in this attitude ephemeris segment.
     *
     * @return a value indicating if the file contains rotation and/or rotation rate
     *         and/or acceleration data.
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
