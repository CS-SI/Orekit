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
package org.orekit.files.ccsds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.complex.Quaternion;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;

/**
 * This class stocks all the information of the Attitude Parameter Message (APM) File parsed
 * by APMParser. It contains the header and the metadata and a the data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMFile extends ADMFile {

    /** Meta-data. */
    private final ADMMetaData metaData;

    /** Epoch of the data. */
    private AbsoluteDate epoch;

    /** Reference frame specifying one frame of the transformation. */
    private String qFrameA;

    /** Reference frame specifying the second portion of the transformation. */
    private String qFrameB;

    /** Rotation direction of the attitude quaternion. */
    private String qDir;

    /** Quaternion. */
    private Quaternion quaternion;

    /** Derivative of the Quaternion. */
    private Quaternion quaternionDot;

    /** Name of the reference frame specifying one frame of the transformation. */
    private String eulerFrameA;

    /** Name of the reference frame specifying the second portion of the transformation. */
    private String eulerFrameB;

    /** Rotation direction of the attitude Euler angles. */
    private String eulerDir;

    /**
     * Rotation order of the {@link #eulerFrameA} to {@link #eulerFrameB} or vice versa.
     * (e.g., 312, where X=1, Y=2, Z=3)
     */
    private String eulerRotSeq;

    /** Frame of reference in which the {@link #rotationAngles} are expressed. */
    private String rateFrame;

    /** Euler angles [rad]. */
    private Vector3D rotationAngles;

    /** Rotation rate [rad/s]. */
    private Vector3D rotationRates;

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

    /** Coordinate system for the inertia tensor. */
    private String inertiaRefFrame;

    /** Moment of Inertia about the 1-axis (kg.m²). */
    private double i11;

    /** Moment of Inertia about the 2-axis (kg.m²). */
    private double i22;

    /** Moment of Inertia about the 3-axis (kg.m²). */
    private double i33;

    /** Inertia Cross Product of the 1 and 2 axes (kg.m²). */
    private double i12;

    /** Inertia Cross Product of the 1 and 3 axes (kg.m²). */
    private double i13;

    /** Inertia Cross Product of the 2 and 3 axes (kg.m²). */
    private double i23;

    /** Epoch comments. The list contains a string for each line of comment. */
    private List<String> epochComment;

    /** Euler comments. The list contains a string for each line of comment. */
    private List<String> eulerComment;

    /** Spin comments. The list contains a string for each line of comment. */
    private List<String> spinComment;

    /** Spacecraft comments. The list contains a string for each line of comment. */
    private List<String> spacecraftComment;

    /** Maneuvers. */
    private List<APMManeuver> maneuvers;

    /**
     * APMFile constructor.
     */
    public APMFile() {
        this.metaData          = new ADMMetaData(this);
        this.epochComment      = Collections.emptyList();
        this.eulerComment      = Collections.emptyList();
        this.spinComment       = Collections.emptyList();
        this.spacecraftComment = Collections.emptyList();
        this.maneuvers         = new ArrayList<>();
        this.rotationAngles    = Vector3D.ZERO;
        this.rotationRates     = Vector3D.ZERO;
        this.quaternion        = new Quaternion(0.0, 0.0, 0.0, 0.0);
        this.quaternionDot     = new Quaternion(0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Get the meta data.
     * @return meta data
     */
    public ADMMetaData getMetaData() {
        return metaData;
    }

    /**
     * Get the epoch of the data.
     * @return epoch the epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /**
     * Set the epoch of the data.
     * @param epoch the epoch to be set
     */
    public void setEpoch(final AbsoluteDate epoch) {
        this.epoch = epoch;
    }

    /**
     * Get the reference frame specifying one frame of the transformation.
     * @return the reference frame A
     */
    public String getQuaternionFrameAString() {
        return qFrameA;
    }

    /**
     * Set the reference frame specifying one frame of the transformation.
     * @param frameA the frame to be set
     */
    public void setQuaternionFrameAString(final String frameA) {
        this.qFrameA = frameA;
    }

    /**
     * Get the reference frame specifying the second portion of the transformation.
     * @return the reference frame B
     */
    public String getQuaternionFrameBString() {
        return qFrameB;
    }

    /**
     * Set the reference frame specifying the second portion of the transformation.
     * @param frameB the frame to be set
     */
    public void setQuaternionFrameBString(final String frameB) {
        this.qFrameB = frameB;
    }

    /**
     * Get the rotation direction of the attitude quaternion.
     * @return the rotation direction of the attitude quaternion
     */
    public String getAttitudeQuaternionDirection() {
        return qDir;
    }

    /**
     * Set the rotation direction of the attitude quaternion.
     * @param direction rotation direction to be set
     */
    public void setAttitudeQuaternionDirection(final String direction) {
        this.qDir = direction;
    }

    /**
     * Get the quaternion.
     * @return quaternion
     */
    public Quaternion getQuaternion() {
        return quaternion;
    }

    /**
     * Set the quaternion.
     * @param q quaternion to set
     */
    public void setQuaternion(final Quaternion q) {
        this.quaternion = q;
    }

    /**
     * Get the derivative of the quaternion.
     * @return the derivative of the quaternion
     */
    public Quaternion getQuaternionDot() {
        return quaternionDot;
    }

    /**
     * Set the derivative of the quaternion.
     * @param qDot quaternion to set
     */
    public void setQuaternionDot(final Quaternion qDot) {
        this.quaternionDot = qDot;
    }

    /**
     * Get the reference frame specifying one frame of the transformation.
     * @return reference frame A
     */
    public String getEulerFrameAString() {
        return eulerFrameA;
    }

    /**
     * Set the reference frame specifying one frame of the transformation.
     * @param frame the frame to be set
     */
    public void setEulerFrameAString(final String frame) {
        this.eulerFrameA = frame;
    }

    /**
     * Get the reference frame specifying the second portion of the transformation.
     * @return reference frame B
     */
    public String getEulerFrameBString() {
        return eulerFrameB;
    }

    /**
     * Set the reference frame specifying the second portion of the transformation.
     * @param frame the frame to be set
     */
    public void setEulerFrameBString(final String frame) {
        this.eulerFrameB = frame;
    }

    /**
     * Get the rotation direction of the attitude Euler angles (A2B or B2A).
     * @return the rotation direction
     */
    public String getEulerDirection() {
        return eulerDir;
    }

    /**
     * Set the rotation direction of the attitude Euler angles (A2B or B2A).
     * @param direction direction to be set
     */
    public void setEulerDirection(final String direction) {
        this.eulerDir = direction;
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
    public Vector3D getRotationAngles() {
        return rotationAngles;
    }

    /**
     * Set the coordinates of the Euler angles (rad).
     * @param rotationAngles coordinates to be set
     */
    public void setRotationAngles(final Vector3D rotationAngles) {
        this.rotationAngles = rotationAngles;
    }

    /**
     * Get the rates of the Euler angles (rad/s).
     * @return rotation rates
     */
    public Vector3D getRotationRates() {
        return rotationRates;
    }

    /**
     * Set the rates of the Euler angles (rad/s).
     * @param rotationRates coordinates to be set
     */
    public void setRotationRates(final Vector3D rotationRates) {
        this.rotationRates = rotationRates;
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

    /**
     * Get the coordinate system for the inertia tensor.
     * @return the coordinate system for the inertia tensor
     */
    public String getInertiaRefFrameString() {
        return inertiaRefFrame;
    }

    /**
     * Set the coordinate system for the inertia tensor.
     * @param frame frame to be set
     */
    public void setInertiaRefFrameString(final String frame) {
        this.inertiaRefFrame = frame;
    }

    /**
     * Get the moment of Inertia about the 1-axis (N.m²).
     * @return the moment of Inertia about the 1-axis.
     */
    public double getI11() {
        return i11;
    }

    /**
     * Set the moment of Inertia about the 1-axis (N.m²).
     * @param i11 moment of Inertia about the 1-axis
     */
    public void setI11(final double i11) {
        this.i11 = i11;
    }

    /**
     * Get the moment of Inertia about the 2-axis (N.m²).
     * @return the moment of Inertia about the 2-axis.
     */
    public double getI22() {
        return i22;
    }

    /**
     * Set the moment of Inertia about the 2-axis (N.m²).
     * @param i22 moment of Inertia about the 2-axis
     */
    public void setI22(final double i22) {
        this.i22 = i22;
    }

    /**
     * Get the moment of Inertia about the 3-axis (N.m²).
     * @return the moment of Inertia about the 3-axis.
     */
    public double getI33() {
        return i33;
    }

    /**
     * Set the moment of Inertia about the 3-axis (N.m²).
     * @param i33 moment of Inertia about the 3-axis
     */
    public void setI33(final double i33) {
        this.i33 = i33;
    }

    /**
     * Get the moment of Inertia about the 1 and 2 axes (N.m²).
     * @return the moment of Inertia about the 1 and 2 axes.
     */
    public double getI12() {
        return i12;
    }

    /**
     * Set the moment of Inertia about the 1 and 2 axes (N.m²).
     * @param i12 moment of Inertia about the 1 and 2 axes
     */
    public void setI12(final double i12) {
        this.i12 = i12;
    }

    /**
     * Get the moment of Inertia about the 1 and 3 axes (N.m²).
     * @return the moment of Inertia about the 1 and 3 axes.
     */
    public double getI13() {
        return i13;
    }

    /**
     * Set the moment of Inertia about the 1 and 3 axes (N.m²).
     * @param i13 moment of Inertia about the 1 and 3 axes
     */
    public void setI13(final double i13) {
        this.i13 = i13;
    }

    /**
     * Get the moment of Inertia about the 2 and 3 axes (N.m²).
     * @return the moment of Inertia about the 2 and 3 axes.
     */
    public double getI23() {
        return i23;
    }

    /**
     * Set the moment of Inertia about the 2 and 3 axes (N.m²).
     * @param i23 moment of Inertia about the 2 and 3 axes
     */
    public void setI23(final double i23) {
        this.i23 = i23;
    }

    /**
     * Get the comment for epoch.
     * @return comment for epoch
     */
    public List<String> getEpochComment() {
        return Collections.unmodifiableList(epochComment);
    }

    /**
     * Set the comment for epoch.
     * @param comment comment to set
     */
    public void setEpochComment(final List<String> comment) {
        epochComment = new ArrayList<>(comment);
    }

    /**
     * Get the comment for Euler angles.
     * @return comment for Euler angles
     */
    public List<String> getEulerComment() {
        return Collections.unmodifiableList(eulerComment);
    }

    /**
     * Set the comment for Euler angles.
     * @param comment comment to set
     */
    public void setEulerComment(final List<String> comment) {
        eulerComment = new ArrayList<>(comment);
    }

    /**
     * Get the comment for spin data.
     * @return comment for spin data
     */
    public List<String> getSpinComment() {
        return Collections.unmodifiableList(spinComment);
    }

    /**
     * Set the comment for spin data.
     * @param comment comment to set
     */
    public void setSpinComment(final List<String> comment) {
        spinComment = new ArrayList<>(comment);
    }

    /**
     * Get the comment for spacecraft.
     * @return comment for spacecraft
     */
    public List<String> getSpacecraftComment() {
        return Collections.unmodifiableList(spacecraftComment);
    }

    /**
     * Set the comment for spacecraft.
     * @param comment comment to set
     */
    public void setSpacecraftComment(final List<String> comment) {
        spacecraftComment = new ArrayList<>(comment);
    }

    /**
     * Get the number of maneuvers present in the APM.
     * @return the number of maneuvers
     */
    public int getNbManeuvers() {
        return maneuvers.size();
    }

    /**
     * Get a list of all maneuvers.
     * @return unmodifiable list of all maneuvers.
     */
    public List<APMManeuver> getManeuvers() {
        return Collections.unmodifiableList(maneuvers);
    }

    /**
     * Get a maneuver.
     * @param index maneuver index, counting from 0
     * @return maneuver
     */
    public APMManeuver getManeuver(final int index) {
        return maneuvers.get(index);
    }

    /**
     * Add a maneuver.
     * @param maneuver maneuver to be set
     */
    public void addManeuver(final APMManeuver maneuver) {
        maneuvers.add(maneuver);
    }

    /**
     * Get boolean testing whether the APM contains at least one maneuver.
     * @return true if APM contains at least one maneuver
     *         false otherwise
     */
    public boolean getHasManeuver() {
        return !maneuvers.isEmpty();
    }

    /**
     * Get the comment for meta-data.
     * @return comment for meta-data
     */
    public List<String> getMetaDataComment() {
        return metaData.getComment();
    }

    /**
     * Maneuver in an APM file.
     */
    public static class APMManeuver {

        /** Epoch of start of maneuver . */
        private AbsoluteDate epochStart;

        /** Coordinate system for the torque vector, for absolute frames. */
        private String refFrame;

        /** Duration (value is 0 for impulsive maneuver). */
        private double duration;

        /** Torque vector (N.m). */
        private Vector3D torque;

        /** Maneuvers data comment, each string in the list corresponds to one line of comment. */
        private List<String> comment;

        /**
         * Simple constructor.
         */
        public APMManeuver() {
            this.torque  = Vector3D.ZERO;
            this.comment = Collections.emptyList();
        }

        /**
         * Get epoch start.
         * @return epoch start
         */
        public AbsoluteDate getEpochStart() {
            return epochStart;
        }

        /**
         * Set epoch start.
         * @param epochStart epoch start
         */
        public void setEpochStart(final AbsoluteDate epochStart) {
            this.epochStart = epochStart;
        }

        /**
         * Get Coordinate system for the torque vector, for absolute frames.
         * @return coordinate system for the torque vector, for absolute frames
         */
        public String getRefFrameString() {
            return refFrame;
        }

        /**
         * Set Coordinate system for the torque vector, for absolute frames.
         * @param frame coordinate system for the torque vector, for absolute frames
         */
        public void setRefFrameString(final String frame) {
            this.refFrame = frame;
        }

        /**
         * Get duration (value is 0 for impulsive maneuver).
         * @return duration (value is 0 for impulsive maneuver)
         */
        public double getDuration() {
            return duration;
        }

        /**
         * Set duration (value is 0 for impulsive maneuver).
         * @param duration duration (value is 0 for impulsive maneuver)
         */
        public void setDuration(final double duration) {
            this.duration = duration;
        }

        /**
         * Get the torque vector (N.m).
         * @return torque vector
         */
        public Vector3D getTorque() {
            return torque;
        }

        /**
         * Set the torque vector (N.m).
         * @param vector torque vector
         */
        public void setTorque(final Vector3D vector) {
            this.torque = vector;
        }

        /**
         * Get the maneuvers data comment, each string in the list corresponds to one line of comment.
         * @return maneuvers data comment, each string in the list corresponds to one line of comment
         */
        public List<String> getComment() {
            return Collections.unmodifiableList(comment);
        }

        /**
         * Set the maneuvers data comment, each string in the list corresponds to one line of comment.
         * @param comment maneuvers data comment, each string in the list corresponds to one line of comment
         */
        public void setComment(final List<String> comment) {
            this.comment = new ArrayList<>(comment);
        }

    }

}
