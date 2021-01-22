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

/**
 * Container for Attitude Parameter Message data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMSpinStabilized {

    /** Comments. The list contains a string for each line of comment. */
    private List<String> comments;

    /** Name of the reference frame specifying one frame of the transformation. */
    private String spinFrameA;

    /** Name of the reference frame specifying the second portion of the transformation. */
    private String spinFrameB;

    /** Rotation direction of the Spin angles. */
    private String spinDir;

    /** Right ascension of spin axis vector (rad). */
    private double spinAlpha;

    /** Declination of the spin axis vector (rad). */
    private double spinDelta;

    /** Phase of the satellite about the spin axis (rad). */
    private double spinAngle;

    /** Angular velocity of satellite around spin axis (rad/s). */
    private double spinAngleVel;

    /** Nutation angle of spin axis (rad). */
    private double nutation;

    /** Body nutation period of the spin axis (s). */
    private double nutationPer;

    /** Inertial nutation phase (rad). */
    private double nutationPhase;

    /** Simple constructor.
     */
    public APMSpinStabilized() {
        this.comments = new ArrayList<>();
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
     * Get the reference frame specifying one frame of the transformation (spin).
     * @return reference frame
     */
    public String getSpinFrameAString() {
        return spinFrameA;
    }

    /**
     * Set the reference frame specifying one frame of the transformation (spin).
     * @param frame frame to be set
     */
    public void setSpinFrameAString(final String frame) {
        this.spinFrameA = frame;
    }

    /**
     * Get the reference frame specifying the second portion of the transformation (spin).
     * @return reference frame
     */
    public String getSpinFrameBString() {
        return spinFrameB;
    }

    /**
     * Set the reference frame specifying the second portion of the transformation (spin).
     * @param frame frame to be set
     */
    public void setSpinFrameBString(final String frame) {
        this.spinFrameB = frame;
    }

    /**
     * Get the rotation direction of the Spin angles.
     * @return the rotation direction
     */
    public String getSpinDirection() {
        return spinDir;
    }

    /**
     * Set the rotation direction of the Spin angles.
     * @param direction rotation direction to be set
     */
    public void setSpinDirection(final String direction) {
        this.spinDir = direction;
    }

    /**
     * Get the right ascension of spin axis vector (rad).
     * @return the right ascension of spin axis vector
     */
    public double getSpinAlpha() {
        return spinAlpha;
    }

    /**
     * Set the right ascension of spin axis vector (rad).
     * @param spinAlpha value to be set
     */
    public void setSpinAlpha(final double spinAlpha) {
        this.spinAlpha = spinAlpha;
    }

    /**
     * Get the declination of the spin axis vector (rad).
     * @return the declination of the spin axis vector (rad).
     */
    public double getSpinDelta() {
        return spinDelta;
    }

    /**
     * Set the declination of the spin axis vector (rad).
     * @param spinDelta value to be set
     */
    public void setSpinDelta(final double spinDelta) {
        this.spinDelta = spinDelta;
    }


    /**
     * Get the phase of the satellite about the spin axis (rad).
     * @return the phase of the satellite about the spin axis
     */
    public double getSpinAngle() {
        return spinAngle;
    }

    /**
     * Set the phase of the satellite about the spin axis (rad).
     * @param spinAngle value to be set
     */
    public void setSpinAngle(final double spinAngle) {
        this.spinAngle = spinAngle;
    }

    /**
     * Get the angular velocity of satellite around spin axis (rad/s).
     * @return the angular velocity of satellite around spin axis
     */
    public double getSpinAngleVel() {
        return spinAngleVel;
    }

    /**
     * Set the angular velocity of satellite around spin axis (rad/s).
     * @param spinAngleVel value to be set
     */
    public void setSpinAngleVel(final double spinAngleVel) {
        this.spinAngleVel = spinAngleVel;
    }

    /**
     * Get the nutation angle of spin axis (rad).
     * @return the nutation angle of spin axis
     */
    public double getNutation() {
        return nutation;
    }

    /**
     * Set the nutation angle of spin axis (rad).
     * @param nutation the nutation angle to be set
     */
    public void setNutation(final double nutation) {
        this.nutation = nutation;
    }

    /**
     * Get the body nutation period of the spin axis (s).
     * @return the body nutation period of the spin axis
     */
    public double getNutationPeriod() {
        return nutationPer;
    }

    /**
     * Set the body nutation period of the spin axis (s).
     * @param period the nutation period to be set
     */
    public void setNutationPeriod(final double period) {
        this.nutationPer = period;
    }

    /**
     * Get the inertial nutation phase (rad).
     * @return the inertial nutation phase
     */
    public double getNutationPhase() {
        return nutationPhase;
    }

    /**
     * Set the inertial nutation phase (rad).
     * @param nutationPhase the nutation phase to be set
     */
    public void setNutationPhase(final double nutationPhase) {
        this.nutationPhase = nutationPhase;
    }

}