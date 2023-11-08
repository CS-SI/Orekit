/* Copyright 2002-2023 CS GROUP
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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndpoints;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.time.AbsoluteDate;

/** This class gathers the meta-data present in the Attitude Data Message (ADM).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AemMetadata extends AdmMetadata {

    /** Endpoints (i.e. frames A, B and their relationship). */
    private final AttitudeEndpoints endpoints;

    /** Start of total time span covered by attitude data. */
    private AbsoluteDate startTime;

    /** End of total time span covered by attitude data. */
    private AbsoluteDate stopTime;

    /** Start of useable time span covered by attitude data. */
    private AbsoluteDate useableStartTime;

    /** End of useable time span covered by attitude data. */
    private AbsoluteDate useableStopTime;

    /** The format of the data lines in the message. */
    private AttitudeType attitudeType;

    /** The placement of the scalar portion of the quaternion (QC) in the attitude data. */
    private Boolean isFirst;

    /** The rotation sequence of the Euler angles. */
    private RotationOrder eulerRotSeq;

    /** The frame in which rates are specified (only for ADM V1). */
    private Boolean rateFrameIsA;

    /** The frame in which angular velocities are specified.
     * @since 12.0
     */
    private FrameFacade angvelFrame;

    /** The interpolation method to be used. */
    private String interpolationMethod;

    /** The interpolation degree. */
    private int interpolationDegree;

    /** Simple constructor.
     * @param defaultInterpolationDegree default interpolation degree
     */
    public AemMetadata(final int defaultInterpolationDegree) {
        endpoints           = new AttitudeEndpoints();
        interpolationDegree = defaultInterpolationDegree;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {

        super.validate(version);

        checkMandatoryEntriesExceptDatesAndExternalFrame(version);
        endpoints.checkExternalFrame(AemMetadataKey.REF_FRAME_A, AemMetadataKey.REF_FRAME_B);

        checkNotNull(startTime, AemMetadataKey.START_TIME.name());
        checkNotNull(stopTime,  AemMetadataKey.STOP_TIME.name());

        if (version >= 2.0 && isFirst()) {
            throw new OrekitException(OrekitMessages.CCSDS_KEYWORD_NOT_ALLOWED_IN_VERSION,
                                      AemMetadataKey.QUATERNION_TYPE, version);
        }

    }

    /** Check is mandatory entries EXCEPT DATES AND EXTERNAL FRAME have been initialized.
     * <p>
     * Either frame A or frame B must be initialized with a {@link
     * org.orekit.files.ccsds.definitions.SpacecraftBodyFrame spacecraft body frame}.
     * </p>
     * <p>
     * This method should throw an exception if some mandatory entry is missing
     * </p>
     * @param version format version
     */
    void checkMandatoryEntriesExceptDatesAndExternalFrame(final double version) {

        super.validate(version);

        endpoints.checkMandatoryEntriesExceptExternalFrame(version,
                                                           AemMetadataKey.REF_FRAME_A,
                                                           AemMetadataKey.REF_FRAME_B,
                                                           AemMetadataKey.ATTITUDE_DIR);

        checkNotNull(attitudeType, AemMetadataKey.ATTITUDE_TYPE.name());
        if (version < 2.0) {
            if (attitudeType == AttitudeType.QUATERNION ||
                attitudeType == AttitudeType.QUATERNION_DERIVATIVE) {
                checkNotNull(isFirst, AemMetadataKey.QUATERNION_TYPE.name());
            }
            if (attitudeType == AttitudeType.EULER_ANGLE_DERIVATIVE) {
                checkNotNull(rateFrameIsA, AemMetadataKey.RATE_FRAME.name());
            }
        } else {
            if (attitudeType == AttitudeType.QUATERNION_ANGVEL) {
                checkNotNull(angvelFrame, AemMetadataKey.ANGVEL_FRAME.name());
            }
        }

        if (attitudeType == AttitudeType.EULER_ANGLE ||
            attitudeType == AttitudeType.EULER_ANGLE_DERIVATIVE) {
            checkNotNull(eulerRotSeq, AemMetadataKey.EULER_ROT_SEQ.name());
        }

    }

    /** Get the endpoints (i.e. frames A, B and their relationship).
     * @return endpoints
     */
    public AttitudeEndpoints getEndpoints() {
        return endpoints;
    }

    /** Check if rates are specified in {@link AttitudeEndpoints#getFrameA() frame A}.
     * @return true if rates are specified in {@link AttitudeEndpoints#getFrameA() frame A}
     */
    public boolean rateFrameIsA() {
        return rateFrameIsA == null ? false : rateFrameIsA;
    }

    /** Set the frame in which rates are specified.
     * @param rateFrameIsA if true, rates are specified in {@link AttitudeEndpoints#getFrameA() frame A}
     */
    public void setRateFrameIsA(final boolean rateFrameIsA) {
        refuseFurtherComments();
        this.rateFrameIsA = rateFrameIsA;
    }

    /** Set frame in which angular velocities are specified.
     * @param angvelFrame frame in which angular velocities are specified
     * @since 12.0
     */
    public void setAngvelFrame(final FrameFacade angvelFrame) {
        this.angvelFrame = angvelFrame;
    }

    /** Get frame in which angular velocities are specified.
     * @return frame in which angular velocities are specified
     * @since 12.0
     */
    public FrameFacade getFrameAngvelFrame() {
        return angvelFrame;
    }

    /** Check if rates are specified in spacecraft body frame.
     * <p>
     * {@link #validate(double) Mandatory entries} must have been
     * initialized properly to non-null values before this method is called,
     * otherwise {@code NullPointerException} will be thrown.
     * </p>
     * @return true if rates are specified in spacecraft body frame
     */
    public boolean isSpacecraftBodyRate() {
        return rateFrameIsA() ^ endpoints.getFrameA().asSpacecraftBodyFrame() == null;
    }

    /**
     * Get the format of the data lines in the message.
     *
     * @return the format of the data lines in the message
     */
    public AttitudeType getAttitudeType() {
        return attitudeType;
    }

    /**
     * Set the format of the data lines in the message.
     * @param type format to be set
     */
    public void setAttitudeType(final AttitudeType type) {
        refuseFurtherComments();
        this.attitudeType = type;
    }

    /**
     * Get the flag for the placement of the quaternion QC in the attitude data.
     *
     * @return true if QC is the first element in the attitude data,
     * false if not initialized
     */
    public Boolean isFirst() {
        return isFirst == null ? Boolean.FALSE : isFirst;
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

    /** Copy the instance, making sure mandatory fields have been initialized.
     * @param version format version
     * @return a new copy
     */
    AemMetadata copy(final double version) {

        checkMandatoryEntriesExceptDatesAndExternalFrame(version);

        // allocate new instance
        final AemMetadata copy = new AemMetadata(getInterpolationDegree());

        // copy comments
        for (String comment : getComments()) {
            copy.addComment(comment);
        }

        // copy object
        copy.setObjectName(getObjectName());
        copy.setObjectID(getObjectID());
        if (getCenter() != null) {
            copy.setCenter(getCenter());
        }

        // copy frames (we may copy null references here)
        copy.getEndpoints().setFrameA(getEndpoints().getFrameA());
        copy.getEndpoints().setFrameB(getEndpoints().getFrameB());
        copy.getEndpoints().setA2b(getEndpoints().isA2b());
        copy.setRateFrameIsA(rateFrameIsA());

        // copy time system only (ignore times themselves)
        copy.setTimeSystem(getTimeSystem());

        // copy attitude definitions
        copy.setAttitudeType(getAttitudeType());
        if (isFirst() != null) {
            copy.setIsFirst(isFirst());
        }
        if (getEulerRotSeq() != null) {
            copy.setEulerRotSeq(getEulerRotSeq());
        }

        // copy interpolation (degree has already been set up at construction)
        if (getInterpolationMethod() != null) {
            copy.setInterpolationMethod(getInterpolationMethod());
        }

        return copy;

    }

}
