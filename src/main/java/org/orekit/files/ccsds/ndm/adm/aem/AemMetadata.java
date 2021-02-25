/* Copyright 2002-2021 CS GROUP
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
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndoints;
import org.orekit.time.AbsoluteDate;

/** This class gathers the meta-data present in the Attitude Data Message (ADM).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AemMetadata extends AdmMetadata {

    /** Endpoints (i.e. frames A, B and their relationship). */
    private final AttitudeEndoints endpoints;

    /** Start of total time span covered by attitude data. */
    private AbsoluteDate startTime;

    /** End of total time span covered by attitude data. */
    private AbsoluteDate stopTime;

    /** Start of useable time span covered by attitude data. */
    private AbsoluteDate useableStartTime;

    /** End of useable time span covered by attitude data. */
    private AbsoluteDate useableStopTime;

    /** The format of the data lines in the message. */
    private AemAttitudeType attitudeType;

    /** The placement of the scalar portion of the quaternion (QC) in the attitude data. */
    private Boolean isFirst;

    /** The rotation sequence of the Euler angles. */
    private RotationOrder eulerRotSeq;

    /** The frame in which rates are specified. */
    private Boolean rateFrameIsA;

    /** The interpolation method to be used. */
    private String interpolationMethod;

    /** The interpolation degree. */
    private int interpolationDegree;

    /** Simple constructor.
     * @param defaultInterpolationDegree default interpolation degree
     */
    public AemMetadata(final int defaultInterpolationDegree) {
        endpoints           = new AttitudeEndoints();
        interpolationDegree = defaultInterpolationDegree;
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {

        checkMandatoryEntriesExceptDatesAndExternalFrame();
        endpoints.checkExternalFrame(AemMetadataKey.REF_FRAME_A, AemMetadataKey.REF_FRAME_B);

        checkNotNull(startTime, AemMetadataKey.START_TIME);
        checkNotNull(stopTime,  AemMetadataKey.STOP_TIME);

    }

    /** Check is mandatory entries EXCEPT DATES AND EXTERNAL FRAME have been initialized.
     * <p>
     * Either frame A or frame B must be initialized with a {@link
     * org.orekit.files.ccsds.definitions.SpacecraftBodyFrame spacecraft body frame}.
     * </p>
     * <p>
     * This method should throw an exception if some mandatory entry is missing
     * </p>
     */
    void checkMandatoryEntriesExceptDatesAndExternalFrame() {

        super.checkMandatoryEntries();

        endpoints.checkMandatoryEntriesExceptExternalFrame(AemMetadataKey.REF_FRAME_A,
                                                           AemMetadataKey.REF_FRAME_B,
                                                           AemMetadataKey.ATTITUDE_DIR);

        checkNotNull(attitudeType, AemMetadataKey.ATTITUDE_TYPE);
        if (attitudeType == AemAttitudeType.QUATERNION ||
            attitudeType == AemAttitudeType.QUATERNION_DERIVATIVE) {
            checkNotNull(isFirst, AemMetadataKey.QUATERNION_TYPE);
        }
        if (attitudeType == AemAttitudeType.EULER_ANGLE ||
            attitudeType == AemAttitudeType.EULER_ANGLE_RATE) {
            checkNotNull(eulerRotSeq, AemMetadataKey.EULER_ROT_SEQ);
        }
        if (attitudeType == AemAttitudeType.QUATERNION_RATE ||
            attitudeType == AemAttitudeType.EULER_ANGLE_RATE) {
            checkNotNull(rateFrameIsA, AemMetadataKey.RATE_FRAME);
        }

    }

    /** Get the endpoints (i.e. frames A, B and their relationship).
     * @return endpoints
     */
    public AttitudeEndoints getEndpoints() {
        return endpoints;
    }

    /** Check if rates are specified in {@link #getFrameA() frame A}.
     * @return true if rates are specified in {@link #getFrameA() frame A}
     */
    public boolean rateFrameIsA() {
        return rateFrameIsA == null ? false : rateFrameIsA;
    }

    /** Set the frame in which rates are specified.
     * @param rateFrameIsA if true, rates are specified in {@link #getFrameA() frame A}
     */
    public void setRateFrameIsA(final boolean rateFrameIsA) {
        refuseFurtherComments();
        this.rateFrameIsA = rateFrameIsA;
    }

    /** Check if rates are specified in spacecraft body frame.
     * <p>
     * {@link #checkMandatoryEntries() Mandatory entries} must have been
     * initialized properly to non-null values before this method is called,
     * otherwise {@code NullPointerException} will be thrown.
     * </p>
     * @return true if if rates are specified in spacecraft body frame
     */
    public boolean isSpacecraftBodyRate() {
        return rateFrameIsA ^ endpoints.getFrameA().asSpacecraftBodyFrame() == null;
    }

    /**
     * Get the format of the data lines in the message.
     *
     * @return the format of the data lines in the message
     */
    public AemAttitudeType getAttitudeType() {
        return attitudeType;
    }

    /**
     * Set the format of the data lines in the message.
     * @param type format to be set
     */
    public void setAttitudeType(final AemAttitudeType type) {
        refuseFurtherComments();
        this.attitudeType = type;
    }

    /**
     * Get the flag for the placement of the quaternion QC in the attitude data.
     *
     * @return true if QC is the first element in the attitude data,
     * null if not initialized
     */
    public Boolean isFirst() {
        return isFirst;
    }

    /**
     * Set the flag for the placement of the quaternion QC in the attitude data.
     * @param isFirst true if QC is the first element in the attitude data
     */
    public void setIsFirst(final boolean isFirst) {
        refuseFurtherComments();
        this.isFirst = isFirst;
    }

    /**
     * Get the rotation order of Euler angles.
     * @return rotation order
     */
    public RotationOrder getEulerRotSeq() {
        return eulerRotSeq;
    }

    /**
     * Set the rotation order for Euler angles.
     * @param eulerRotSeq order to be set
     */
    public void setEulerRotSeq(final RotationOrder eulerRotSeq) {
        refuseFurtherComments();
        this.eulerRotSeq = eulerRotSeq;
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
        refuseFurtherComments();
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
        refuseFurtherComments();
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
        refuseFurtherComments();
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
        refuseFurtherComments();
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
        refuseFurtherComments();
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
        refuseFurtherComments();
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

}
