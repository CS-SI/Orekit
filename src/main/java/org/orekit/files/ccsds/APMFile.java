/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
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
    private Frame qFrameA;

    /** Reference frame specifying the second portion of the transformation. */
    private Frame qFrameB;

    /** Rotation direction of the attitude quaternion. */
    private String qDir;

    /** First coordinate of the vectorial part of the quaternion. */
    private double q1;

    /** Second coordinate of the vectorial part of the quaternion. */
    private double q2;

    /** Third coordinate of the vectorial part of the quaternion. */
    private double q3;

    /** Scalar coordinate of the quaternion. */
    private double qc;

    /** Derivative of {@link #q1} (s-1). */
    private double q1Dot;

    /** Derivative of {@link #q2} (s-1). */
    private double q2Dot;

    /** Derivative of {@link #q3} (s-1). */
    private double q3Dot;

    /** Derivative of {@link #qc} (s-1). */
    private double qcDot;

    /** Name of the reference frame specifying one frame of the transformation. */
    private Frame eulerFrameA;

    /** Name of the reference frame specifying the second portion of the transformation. */
    private Frame eulerFrameB;

    /** Rotation direction of the attitude Euler angles. */
    private String eulerDir;

    /**
     * Rotation order of the {@link #eulerFrameA} to {@link #eulerFrameB} or vice versa.
     * (e.g., 312, where X=1, Y=2, Z=3)
     */
    private int eulerRotSeq;

    /** Frame of reference in which the {@link #rotationAngles} are expressed. */
    private Frame rateFrame;

    /** Euler angles [rad]. */
    private Vector3D rotationAngles;

    /** Rotation rate [rad/s]. */
    private Vector3D rotationRates;

    /** Name of the reference frame specifying one frame of the transformation. */
    private Frame spinFrameA;

    /** Name of the reference frame specifying the second portion of the transformation. */
    private Frame spinFrameB;

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
    private Frame inertiaRefFrame;

    /** Moment of Inertia about the 1-axis (kg.m²). */
    private double i11;

    /** Moment of Inertia about the 2-axis (kg.m²). */
    private double i22;

    /** Moment of Inertia about the 3-axis (kg.m²). */
    private double i33;

    /** Inertia Cross Product of the 1 & 2 axes (kg.m²). */
    private double i12;

    /** Inertia Cross Product of the 1 & 3 axes (kg.m²). */
    private double i13;

    /** Inertia Cross Product of the 2 & 3 axes (kg.m²). */
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
     * AEMFile constructor.
     */
    public APMFile() {
        this.metaData          = new ADMMetaData(this);
        this.epochComment      = Collections.emptyList();
        this.eulerComment      = Collections.emptyList();
        this.spinComment       = Collections.emptyList();
        this.spacecraftComment = Collections.emptyList();
        this.maneuvers         = new ArrayList<APMManeuver>();
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
    void setEpoch(final AbsoluteDate epoch) {
        this.epoch = epoch;
    }

    /**
     * Get the reference frame specifying one frame of the transformation.
     * @return the reference frame A
     */
    public Frame getQuaternionFrameA() {
        return qFrameA;
    }

    /**
     * Set the reference frame specifying one frame of the transformation.
     * @param frameA the frame to be set
     */
    void setQuaternionFrameA(final Frame frameA) {
        this.qFrameA = frameA;
    }

    /**
     * Get the reference frame specifying the second portion of the transformation.
     * @return the reference frame B
     */
    public Frame getQuaternionFrameB() {
        return qFrameB;
    }

    /**
     * Set the reference frame specifying the second portion of the transformation.
     * @param frameB the frame to be set
     */
    void setQuaternionFrameB(final Frame frameB) {
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
    void setAttitudeQuaternionDirection(final String direction) {
        this.qDir = direction;
    }

    /**
     * Get the first coordinate of the vectorial part of the quaternion.
     * @return q1
     */
    public double getQ1() {
        return q1;
    }

    /**
     * Set the first coordinate of the vectorial part of the quaternion.
     * @param q1 value to set
     */
    void setQ1(final double q1) {
        this.q1 = q1;
    }

    /**
     * Get the second coordinate of the vectorial part of the quaternion.
     * @return q2
     */
    public double getQ2() {
        return q2;
    }

    /**
     * Set the second coordinate of the vectorial part of the quaternion.
     * @param q2 value to set
     */
    void setQ2(final double q2) {
        this.q2 = q2;
    }

    /**
     * Get the third coordinate of the vectorial part of the quaternion.
     * @return q3
     */
    public double getQ3() {
        return q3;
    }

    /**
     * Set the third coordinate of the vectorial part of the quaternion.
     * @param q3 value to set
     */
    void setQ3(final double q3) {
        this.q3 = q3;
    }

    /**
     * Get the scalar coordinate of the quaternion.
     * @return qc
     */
    public double getQC() {
        return qc;
    }

    /**
     * Set the scalar coordinate of the quaternion.
     * @param q value to set
     */
    void setQC(final double q) {
        this.qc = q;
    }

    /**
     * Get the derivative of {@link #q1}.
     * @return q1Dot
     */
    public double getQ1Dot() {
        return q1Dot;
    }

    /**
     * Set the derivative of {@link #q1}.
     * @param q1Dot value to set
     */
    void setQ1Dot(final double q1Dot) {
        this.q1Dot = q1Dot;
    }

    /**
     * Get the derivative of {@link #q2}.
     * @return q2Dot
     */
    public double getQ2Dot() {
        return q2Dot;
    }

    /**
     * Set the derivative of {@link #q2}.
     * @param q2Dot value to set
     */
    void setQ2Dot(final double q2Dot) {
        this.q2Dot = q2Dot;
    }

    /**
     * Get the derivative of {@link #q3}.
     * @return q3Dot
     */
    public double getQ3Dot() {
        return q3Dot;
    }

    /**
     * Set the derivative of {@link #q3}.
     * @param q3Dot value to set
     */
    void setQ3Dot(final double q3Dot) {
        this.q3Dot = q3Dot;
    }

    /**
     * Get the derivative of {@link #qc}.
     * @return qcDot
     */
    public double getQCDot() {
        return qcDot;
    }

    /**
     * Set the scalar coordinate of the quaternion.
     * @param qDot value to set
     */
    void setQCDot(final double qDot) {
        this.qcDot = qDot;
    }

    /**
     * Get the reference frame specifying one frame of the transformation.
     * @return reference frame A
     */
    public Frame getEulerFrameA() {
        return eulerFrameA;
    }

    /**
     * Set the reference frame specifying one frame of the transformation.
     * @param eulerFrameA the frame to be set
     */
    void setEulerFrameA(final Frame eulerFrameA) {
        this.eulerFrameA = eulerFrameA;
    }

    /**
     * Get the reference frame specifying the second portion of the transformation.
     * @return reference frame B
     */
    public Frame getEulerFrameB() {
        return eulerFrameB;
    }

    /**
     * Set the reference frame specifying the second portion of the transformation.
     * @param eulerFrameB the frame to be set
     */
    void setEulerFrameB(final Frame eulerFrameB) {
        this.eulerFrameB = eulerFrameB;
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
    void setEulerDirection(final String direction) {
        this.eulerDir = direction;
    }

    /**
     * Get the rotation order of Euler angles (X=1, Y=2, Z=3).
     * @return rotation order
     */
    public int getEulerRotSeq() {
        return eulerRotSeq;
    }

    /**
     * Set the rotation order for Euler angles (X=1, Y=2, Z=3).
     * @param eulerRotSeq order to be setS
     */
    void setEulerRotSeq(final int eulerRotSeq) {
        this.eulerRotSeq = eulerRotSeq;
    }

    /**
     * Get the frame of reference in which the Euler angles are expressed.
     * @return the frame of reference
     */
    public Frame getRateFrame() {
        return rateFrame;
    }

    /**
     * Set the frame of reference in which the Euler angles are expressed.
     * @param rateFrame frame to be set
     */
    void setRateFrame(final Frame rateFrame) {
        this.rateFrame = rateFrame;
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
    void setRotationAngles(final Vector3D rotationAngles) {
        this.rotationAngles = rotationAngles;
    }

    /**
     * Get the rates of the Euler angles.
     * @return rotation rates
     */
    public Vector3D getRotationRates() {
        return rotationRates;
    }

    /**
     * Set the rates of the Euler angles.
     * @param rotationRates coordinates to be set
     */
    void setRotationRates(final Vector3D rotationRates) {
        this.rotationRates = rotationRates;
    }

    /**
     * Get the reference frame specifying one frame of the transformation (spin).
     * @return reference frame
     */
    public Frame getSpinFrameA() {
        return spinFrameA;
    }

    /**
     * Set the reference frame specifying one frame of the transformation (spin).
     * @param spinFrameA frame to be set
     */
    void setSpinFrameA(final Frame spinFrameA) {
        this.spinFrameA = spinFrameA;
    }

    /**
     * Get the reference frame specifying the second portion of the transformation (spin).
     * @return reference frame
     */
    public Frame getSpinFrameB() {
        return spinFrameB;
    }

    /**
     * Set the reference frame specifying the second portion of the transformation (spin).
     * @param spinFrameB frame to be set
     */
    void setSpinFrameB(final Frame spinFrameB) {
        this.spinFrameB = spinFrameB;
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
    void setSpinDirection(final String direction) {
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
    void setSpinAlpha(final double spinAlpha) {
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
    void setSpinDelta(final double spinDelta) {
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
    void setSpinAngle(final double spinAngle) {
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
    void setSpinAngleVel(final double spinAngleVel) {
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
    void setNutation(final double nutation) {
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
    void setNutationPeriod(final double period) {
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
    void setNutationPhase(final double nutationPhase) {
        this.nutationPhase = nutationPhase;
    }

    /**
     * Get the coordinate system for the inertia tensor.
     * @return the coordinate system for the inertia tensor
     */
    public Frame getInertiaRefFrame() {
        return inertiaRefFrame;
    }

    /**
     * Set the coordinate system for the inertia tensor.
     * @param inertiaRefFrame frame to be set
     */
    void setInertiaRefFrame(final Frame inertiaRefFrame) {
        this.inertiaRefFrame = inertiaRefFrame;
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
    void setI11(final double i11) {
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
    void setI22(final double i22) {
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
    void setI33(final double i33) {
        this.i33 = i33;
    }

    /**
     * Get the moment of Inertia about the 1 & 2 axes (N.m²).
     * @return the moment of Inertia about the 1 & 2 axes.
     */
    public double getI12() {
        return i12;
    }

    /**
     * Set the moment of Inertia about the 1 & 2 axes (N.m²).
     * @param i12 moment of Inertia about the 1 & 2 axes
     */
    void setI12(final double i12) {
        this.i12 = i12;
    }

    /**
     * Get the moment of Inertia about the 1 & 3 axes (N.m²).
     * @return the moment of Inertia about the 1 & 3 axes.
     */
    public double getI13() {
        return i13;
    }

    /**
     * Set the moment of Inertia about the 1 & 3 axes (N.m²).
     * @param i13 moment of Inertia about the 1 & 3 axes
     */
    void setI13(final double i13) {
        this.i13 = i13;
    }

    /**
     * Get the moment of Inertia about the 2 & 3 axes (N.m²).
     * @return the moment of Inertia about the 2 & 3 axes.
     */
    public double getI23() {
        return i23;
    }

    /**
     * Set the moment of Inertia about the 2 & 3 axes (N.m²).
     * @param i23 moment of Inertia about the 2 & 3 axes
     */
    void setI23(final double i23) {
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
    void setEpochComment(final List<String> comment) {
        epochComment = new ArrayList<String>(comment);
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
    void setEulerComment(final List<String> comment) {
        eulerComment = new ArrayList<String>(comment);
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
    void setSpinComment(final List<String> comment) {
        spinComment = new ArrayList<String>(comment);
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
    void setSpacecraftComment(final List<String> comment) {
        spacecraftComment = new ArrayList<String>(comment);
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
    void addManeuver(final APMManeuver maneuver) {
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
        private Frame refFrame;

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
        void setEpochStart(final AbsoluteDate epochStart) {
            this.epochStart = epochStart;
        }


        /**
         * Get Coordinate system for the torque vector, for absolute frames.
         * @return coordinate system for the torque vector, for absolute frames
         */
        public Frame getRefFrame() {
            return refFrame;
        }

        /**
         * Set Coordinate system for the torque vector, for absolute frames.
         * @param refFrame coordinate system for the torque vector, for absolute frames
         */
        void setRefFrame(final Frame refFrame) {
            this.refFrame = refFrame;
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
        void setDuration(final double duration) {
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
        void setTorque(final Vector3D vector) {
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
        void setComment(final List<String> comment) {
            this.comment = new ArrayList<String>(comment);
        }

    }

}
