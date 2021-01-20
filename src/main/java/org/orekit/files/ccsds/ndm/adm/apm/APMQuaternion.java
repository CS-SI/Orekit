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

import org.hipparchus.complex.Quaternion;
import org.orekit.time.AbsoluteDate;

/**
 * Container for Attitude Parameter Message quaternion logical block.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMQuaternion {

    /** Comments. The list contains a string for each line of comment. */
    private List<String> comments;

    /** Epoch of the data. */
    private AbsoluteDate epoch;

    /** Reference frame specifying one frame of the transformation. */
    private String qFrameA;

    /** Reference frame specifying the second portion of the transformation. */
    private String qFrameB;

    /** Rotation direction of the attitude quaternion. */
    private String qDir;

    /** Quaternion, scalar component. */
    private double q0;

    /** Quaternion, first component of the vector part. */
    private double q1;

    /** Quaternion, second component of the vector part. */
    private double q2;

    /** Quaternion, third component of the vector part. */
    private double q3;

    /** Quaternion derivative, scalar component. */
    private double q0Dot;

    /** Quaternion derivative, first component of the vector part. */
    private double q1Dot;

    /** Quaternion derivative, second component of the vector part. */
    private double q2Dot;

    /** Quaternion derivative, third component of the vector part. */
    private double q3Dot;

    /** Simple constructor.
     */
    public APMQuaternion() {
        this.comments      = new ArrayList<>();
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
        return new Quaternion(q0, q1, q2, q3);
    }

    /**
     * Set the quaternion scalar component.
     * @param q0 quaternion scalar component
     */
    public void setQ0(final double q0) {
        this.q0 = q0;
    }

    /**
     * Set the quaternion first component of the vector part.
     * @param q1 quaternion first component of the vector part
     */
    public void setQ1(final double q1) {
        this.q1 = q1;
    }

    /**
     * Set the quaternion second component of the vector part.
     * @param q2 quaternion second component of the vector part
     */
    public void setQ2(final double q2) {
        this.q2 = q2;
    }

    /**
     * Set the quaternion third component of the vector part.
     * @param q3 quaternion third component of the vector part
     */
    public void setQ3(final double q3) {
        this.q3 = q3;
    }

    /**
     * Get the quaternion derivative.
     * @return quaternion derivative
     */
    public Quaternion getQuaternionDot() {
        return new Quaternion(q0Dot, q1Dot, q2Dot, q3Dot);
    }

    /**
     * Set the quaternion derivative scalar component.
     * @param q0Dot quaternion derivative scalar component
     */
    public void setQ0Dot(final double q0Dot) {
        this.q0Dot = q0Dot;
    }

    /**
     * Set the quaternion derivative first component of the vector part.
     * @param q1Dot quaternion derivative first component of the vector part
     */
    public void setQ1Dot(final double q1Dot) {
        this.q1Dot = q1Dot;
    }

    /**
     * Set the quaternion derivative second component of the vector part.
     * @param q2Dot quaternion derivative second component of the vector part
     */
    public void setQ2Dot(final double q2Dot) {
        this.q2Dot = q2Dot;
    }

    /**
     * Set the quaternion derivative third component of the vector part.
     * @param q3Dot quaternion derivative third component of the vector part
     */
    public void setQ3Dot(final double q3Dot) {
        this.q3Dot = q3Dot;
    }

}
