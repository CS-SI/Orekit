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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndPoints;
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for {@link APMEuler Euler rotations} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMEuler extends CommentsContainer {

    /** Attitude end points. */
    private AttitudeEndPoints endPoints;

    /** Rotation order of the Euler angles. */
    private RotationOrder eulerRotSeq;

    /** Frame of reference in which the {@link #rotationAngles} derivatives are expressed. */
    private String rateFrame;

    /** Euler angles [rad]. */
    private double[] rotationAngles;

    /** Rotation rate [rad/s]. */
    private double[] rotationRates;

    /** Indicator for rotation angles. */
    private boolean inRotationAngles;

    /** Simple constructor.
     */
    public APMEuler() {
        this.endPoints        = new AttitudeEndPoints();
        this.rotationAngles   = new double[3];
        this.rotationRates    = new double[3];
        this.inRotationAngles = false;
        Arrays.fill(rotationAngles, Double.NaN);
        Arrays.fill(rotationRates,  Double.NaN);
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        endPoints.checkMandatoryEntries();
        checkNotNull(eulerRotSeq, APMEulerKey.EULER_ROT_SEQ);
        if (Double.isNaN(rotationAngles[0] + rotationAngles[1] + rotationAngles[2])) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "{X|Y|Z}_ANGLE");
        }
        if (Double.isNaN(rotationRates[0] + rotationRates[1] + rotationRates[2])) {
            // if at least one is NaN, all must be NaN (i.e. not initialized)
            for (final double rr : rotationRates) {
                if (!Double.isNaN(rr)) {
                    throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, "{X|Y|Z}_RATE");
                }
            }
        }
    }

    /** Get the attitude end points.
     * @return attitude end points
     */
    public AttitudeEndPoints getEndPoints() {
        return endPoints;
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
     * Get the frame of reference in which the Euler angles are expressed.
     * @return the frame of reference
     */
    public String getRateFrameString() {
        return rateFrame;
    }

    /**
     * Set the frame of reference in which the Euler angles are expressed.
     * @param frame frame to be set
     */
    public void setRateFrameString(final String frame) {
        refuseFurtherComments();
        this.rateFrame = frame;
    }

    /**
     * Get the coordinates of the Euler angles (rad).
     * @return rotation angles
     */
    public double[] getRotationAngles() {
        return rotationAngles.clone();
    }

    /**
     * Set an Euler angle (rad).
     * @param index angle index (counting from 0)
     * @param angle angle to set
     */
    public void setRotationAngle(final int index, final double angle) {
        refuseFurtherComments();
        rotationAngles[index] = angle;
    }

    /**
     * Get the rates of the Euler angles (rad/s).
     * @return rotation rates
     */
    public double[] getRotationRates() {
        return rotationRates.clone();
    }

    /**
     * Set the rates of an Euler angle (rad/s).
     * @param index angle index (counting from 0)
     * @param rate angle rate to set
     */
    public void setRotationRate(final int index, final double rate) {
        refuseFurtherComments();
        rotationRates[index] = rate;
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
    void setInRotationAngles(final boolean inRotationAngles) {
        refuseFurtherComments();
        this.inRotationAngles = inRotationAngles;
    }

}
