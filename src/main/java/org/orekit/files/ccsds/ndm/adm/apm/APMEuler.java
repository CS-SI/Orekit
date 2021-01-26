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

import java.util.ArrayList;
import java.util.List;

import org.orekit.files.ccsds.ndm.adm.AttitudeEndPoints;

/**
 * Container for {@link APMEuler Euler rotations} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMEuler {

    /** Comments. The list contains a string for each line of comment. */
    private List<String> comments;

    /** Attitude end points. */
    private AttitudeEndPoints endPoints;

    /**
     * Rotation order of the {@link #eulerFrameA} to {@link #eulerFrameB} or vice versa.
     * (e.g., 312, where X=1, Y=2, Z=3)
     */
    private String eulerRotSeq;

    /** Frame of reference in which the {@link #rotationAngles} are expressed. */
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
        this.comments         = new ArrayList<>();
        this.endPoints        = new AttitudeEndPoints();
        this.rotationAngles   = new double[3];
        this.rotationRates    = new double[3];
        this.inRotationAngles = false;
    }

    /** Get the comments.
     * @return the comments
     */
    public List<String> getComments() {
        return comments;
    }

    /** Add comment.
     * @param comment comment to add
     */
    public void addComment(final String comment) {
        comments.add(comment);
    }

    /**
     * Set the reference frame specifying one frame of the transformation.
     * @param frame the frame to be set
     */
    public void setEulerFrameA(final String frame) {
        endPoints.setFrameA(frame);
    }

    /**
     * Set the reference frame specifying the second portion of the transformation.
     * @param frame the frame to be set
     */
    public void setEulerFrameB(final String frame) {
        endPoints.setFrameB(frame);
    }

    /**
     * Set the rotation direction of the attitude Euler angles (A2B or B2A).
     * @param direction direction to be set
     */
    public void setEulerDirection(final String direction) {
        endPoints.setDirection(direction);
    }

    /** Get the attitude end points.
     * @return attitude end points
     */
    public AttitudeEndPoints getEndPoints() {
        return endPoints;
    }

    /**
     * Get the rotation order of Euler angles (X=1, Y=2, Z=3).
     * @return rotation order
     */
    public String getEulerRotSeq() {
        return eulerRotSeq;
    }

    /**
     * Set the rotation order for Euler angles (X=1, Y=2, Z=3).
     * @param eulerRotSeq order to be setS
     */
    public void setEulerRotSeq(final String eulerRotSeq) {
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
        this.inRotationAngles = inRotationAngles;
    }

}
