/* Copyright 2002-2022 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndoints;
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for {@link Euler Euler rotations} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class Euler extends CommentsContainer {

    /** Endpoints (i.e. frames A, B and their relationship). */
    private final AttitudeEndoints endpoints;

    /** Rotation order of the Euler angles. */
    private RotationOrder eulerRotSeq;

    /** The frame in which rates are specified. */
    private Boolean rateFrameIsA;

    /** Euler angles [rad]. */
    private double[] rotationAngles;

    /** Rotation rate [rad/s]. */
    private double[] rotationRates;

    /** Indicator for rotation angles. */
    private boolean inRotationAngles;

    /** Simple constructor.
     */
    public Euler() {
        this.endpoints        = new AttitudeEndoints();
        this.rotationAngles   = new double[3];
        this.rotationRates    = new double[3];
        this.inRotationAngles = false;
        Arrays.fill(rotationAngles, Double.NaN);
        Arrays.fill(rotationRates,  Double.NaN);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {

        super.validate(version);
        endpoints.checkMandatoryEntriesExceptExternalFrame(EulerKey.EULER_FRAME_A,
                                                           EulerKey.EULER_FRAME_B,
                                                           EulerKey.EULER_DIR);
        endpoints.checkExternalFrame(EulerKey.EULER_FRAME_A, EulerKey.EULER_FRAME_B);
        checkNotNull(eulerRotSeq, EulerKey.EULER_ROT_SEQ);

        final boolean missingAngle = Double.isNaN(rotationAngles[0] + rotationAngles[1] + rotationAngles[2]);
        if (missingAngle) {
            // if at least one is NaN, all must be NaN (i.e. not initialized)
            for (final double ra : rotationAngles) {
                if (!Double.isNaN(ra)) {
                    throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "{X|Y|Z}_ANGLE");
                }
            }
        }

        final boolean missingRate = Double.isNaN(rotationRates[0] + rotationRates[1] + rotationRates[2]);
        if (missingRate) {
            // if at least one is NaN, all must be NaN (i.e. not initialized)
            for (final double rr : rotationRates) {
                if (!Double.isNaN(rr)) {
                    throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "{X|Y|Z}_RATE");
                }
            }
        }

        // either angles or rates must be specified
        // (angles may be missing in the quaternion/Euler rate case)
        if (missingAngle && missingRate) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "{X|Y|Z}_{ANGLE|RATE}");
        }

    }

    /** Get the endpoints (i.e. frames A, B and their relationship).
     * @return endpoints
     */
    public AttitudeEndoints getEndpoints() {
        return endpoints;
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

    /** Check if rates are specified in {@link AttitudeEndoints#getFrameA() frame A}.
     * @return true if rates are specified in {@link AttitudeEndoints#getFrameA() frame A}
     */
    public boolean rateFrameIsA() {
        return rateFrameIsA == null ? false : rateFrameIsA;
    }

    /** Set the frame in which rates are specified.
     * @param rateFrameIsA if true, rates are specified in {@link AttitudeEndoints#getFrameA() frame A}
     */
    public void setRateFrameIsA(final boolean rateFrameIsA) {
        refuseFurtherComments();
        this.rateFrameIsA = rateFrameIsA;
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
        return rateFrameIsA ^ endpoints.getFrameA().asSpacecraftBodyFrame() == null;
    }

    /**
     * Get the coordinates of the Euler angles (rad).
     * @return rotation angles
     */
    public double[] getRotationAngles() {
        return rotationAngles.clone();
    }

    /**
     * Set the Euler angle about (rad).
     * @param axis rotation axis
     * @param angle angle to set
     */
    public void setRotationAngle(final char axis, final double angle) {
        refuseFurtherComments();
        setAngleOrRate(rotationAngles, axis, angle);
    }

    /**
     * Get the rates of the Euler angles (rad/s).
     * @return rotation rates
     */
    public double[] getRotationRates() {
        return rotationRates.clone();
    }

    /**
     * Set the rate of Euler angle (rad/s).
     * @param axis rotation axis
     * @param rate angle rate to set
     */
    public void setRotationRate(final char axis, final double rate) {
        refuseFurtherComments();
        setAngleOrRate(rotationRates, axis, rate);
    }

    /** Check if we are in the rotationAngles part of XML files.
     * @return true if we are in the rotationAngles part of XML files
     */
    boolean inRotationAngles() {
        return inRotationAngles;
    }

    /** Set flag for rotation angle parsing.
     * @param inRotationAngles if true, we are in the rotationAngles part of XML files
     */
    public void setInRotationAngles(final boolean inRotationAngles) {
        refuseFurtherComments();
        this.inRotationAngles = inRotationAngles;
    }

    /** Check if the logical block includes rates.
     * @return true if logical block includes rates
     */
    public boolean hasRates() {
        return !Double.isNaN(rotationRates[0] + rotationRates[1] + rotationRates[2]);
    }

    /** Set an angle or rate in an array.
     * @param array angle or rate array
     * @param axis axis name
     * @param value angle or rate to set
     */
    private void setAngleOrRate(final double[] array, final char axis, final double value) {
        refuseFurtherComments();
        if (eulerRotSeq != null) {
            if (eulerRotSeq.name().charAt(0) == axis && Double.isNaN(array[0])) {
                array[0] = value;
            } else if (eulerRotSeq.name().charAt(1) == axis && Double.isNaN(array[1])) {
                array[1] = value;
            } else if (eulerRotSeq.name().charAt(2) == axis && Double.isNaN(array[2])) {
                array[2] = value;
            }
        }
    }
}
