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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndpoints;
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for {@link Euler Euler rotations} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class Euler extends CommentsContainer {

    /** Key for angles in ADM V1.
     * @since 12.0
     */
    private static final String KEY_ANGLES_V1 = "{X|Y|Z}_ANGLE";

    /** Key for angles in ADM V2.
     * @since 12.0
     */
    private static final String KEY_ANGLES_V2 = "ANGLE_{1|2|3}";

    /** Key for rates in ADM V1.
     * @since 12.0
     */
    private static final String KEY_RATES_V1 = "{X|Y|Z}_RATE";

    /** Key for rates in ADM V2.
     * @since 12.0
     */
    private static final String KEY_RATES_V2 = "ANGLE_{1|2|3}_DOT";

    /** Endpoints (i.e. frames A, B and their relationship). */
    private final AttitudeEndpoints endpoints;

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
        this.endpoints        = new AttitudeEndpoints();
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
        if (version < 2.0) {
            endpoints.checkMandatoryEntriesExceptExternalFrame(version,
                                                               EulerKey.EULER_FRAME_A,
                                                               EulerKey.EULER_FRAME_B,
                                                               EulerKey.EULER_DIR);
            endpoints.checkExternalFrame(EulerKey.EULER_FRAME_A, EulerKey.EULER_FRAME_B);
        } else {
            endpoints.checkMandatoryEntriesExceptExternalFrame(version,
                                                               EulerKey.REF_FRAME_A,
                                                               EulerKey.REF_FRAME_B,
                                                               EulerKey.EULER_DIR);
            endpoints.checkExternalFrame(EulerKey.REF_FRAME_A, EulerKey.REF_FRAME_B);
        }
        checkNotNull(eulerRotSeq, EulerKey.EULER_ROT_SEQ.name());

        if (!hasAngles()) {
            // if at least one angle is missing, all must be NaN (i.e. not initialized)
            for (final double ra : rotationAngles) {
                if (!Double.isNaN(ra)) {
                    throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY,
                                              version < 2.0 ? KEY_ANGLES_V1 : KEY_ANGLES_V2);
                }
            }
        }

        if (!hasRates()) {
            // if at least one rate is missing, all must be NaN (i.e. not initialized)
            for (final double rr : rotationRates) {
                if (!Double.isNaN(rr)) {
                    throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY,
                                              version < 2.0 ? KEY_RATES_V1 : KEY_RATES_V2);
                }
            }
        }

        if (version < 2.0) {
            // in ADM V1, either angles or rates must be specified
            // (angles may be missing in the quaternion/Euler rate case)
            if (!hasAngles() && !hasRates()) {
                throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, KEY_ANGLES_V1 + "/" + KEY_RATES_V1);
            }
        } else {
            // in ADM V2, angles are mandatory
            if (!hasAngles()) {
                throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, KEY_ANGLES_V2);
            }
        }

    }

    /** Get the endpoints (i.e. frames A, B and their relationship).
     * @return endpoints
     */
    public AttitudeEndpoints getEndpoints() {
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
     * Get the coordinates of the Euler angles.
     * @return rotation angles (rad)
     */
    public double[] getRotationAngles() {
        return rotationAngles.clone();
    }

    /**
     * Set the Euler angle about axis.
     * @param axis rotation axis
     * @param angle angle to set (rad)
     */
    public void setLabeledRotationAngle(final char axis, final double angle) {
        if (eulerRotSeq != null) {
            for (int i = 0; i < rotationAngles.length; ++i) {
                if (eulerRotSeq.name().charAt(i) == axis && Double.isNaN(rotationAngles[i])) {
                    setIndexedRotationAngle(i, angle);
                    return;
                }
            }
        }
    }

    /**
     * Set the Euler angle about axis.
     * @param axis rotation axis
     * @param angle angle to set (rad)
     * @since 12.0
     */
    public void setIndexedRotationAngle(final int axis, final double angle) {
        refuseFurtherComments();
        rotationAngles[axis] = angle;
    }

    /**
     * Get the rates of the Euler angles.
     * @return rotation rates (rad/s)
     */
    public double[] getRotationRates() {
        return rotationRates.clone();
    }

    /**
     * Set the rate of Euler angle about axis.
     * @param axis rotation axis
     * @param rate angle rate to set (rad/s)
     */
    public void setLabeledRotationRate(final char axis, final double rate) {
        if (eulerRotSeq != null) {
            for (int i = 0; i < rotationRates.length; ++i) {
                if (eulerRotSeq.name().charAt(i) == axis && Double.isNaN(rotationRates[i])) {
                    setIndexedRotationRate(i, rate);
                    return;
                }
            }
        }
    }

    /**
     * Set the rate of Euler angle about axis.
     * @param axis rotation axis
     * @param rate angle rate to set (rad/s)
     * @since 12.0
     */
    public void setIndexedRotationRate(final int axis, final double rate) {
        refuseFurtherComments();
        rotationRates[axis] = rate;
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

    /** Check if the logical block includes angles.
     * <p>
     * This can be false only for ADM V1, as angles are mandatory since ADM V2.
     * </p>
     * @return true if logical block includes angles
     * @since 12.0
     */
    public boolean hasAngles() {
        return !Double.isNaN(rotationAngles[0] + rotationAngles[1] + rotationAngles[2]);
    }

    /** Check if the logical block includes rates.
     * @return true if logical block includes rates
     */
    public boolean hasRates() {
        return !Double.isNaN(rotationRates[0] + rotationRates[1] + rotationRates[2]);
    }

}
