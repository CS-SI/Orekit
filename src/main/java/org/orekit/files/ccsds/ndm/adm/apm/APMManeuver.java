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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;

/**
 * Maneuver in an APM file.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMManeuver extends CommentsContainer {

    /** Epoch of start of maneuver . */
    private AbsoluteDate epochStart;

    /** Coordinate system for the torque vector, for absolute frames. */
    private String refFrameString;

    /** Duration (value is 0 for impulsive maneuver). */
    private double duration;

    /** Torque vector (N.m). */
    private Vector3D torque;

    /**
     * Simple constructor.
     */
    public APMManeuver() {
        this.torque   = Vector3D.NaN;
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
        refuseFurtherComments();
        this.epochStart = epochStart;
    }

    /**
     * Get Coordinate system for the torque vector, for absolute frames.
     * @return coordinate system for the torque vector, for absolute frames
     */
    public String getRefFrameString() {
        return refFrameString;
    }

    /**
     * Set Coordinate system for the torque vector, for absolute frames.
     * @param refFrameString coordinate system for the torque vector, for absolute frames
     */
    public void setRefFrameString(final String refFrameString) {
        refuseFurtherComments();
        this.refFrameString = refFrameString;
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
        refuseFurtherComments();
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
        refuseFurtherComments();
        this.torque = vector;
    }

}
