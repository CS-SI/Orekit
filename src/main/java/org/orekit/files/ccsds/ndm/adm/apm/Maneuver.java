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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;

/**
 * Maneuver in an APM file.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class Maneuver extends CommentsContainer {

    /** Epoch of start of maneuver . */
    private AbsoluteDate epochStart;

    /** Coordinate system for the torque vector, for absolute frames. */
    private String refFrameString;

    /** Duration (value is 0 for impulsive maneuver). */
    private double duration;

    /** Torque vector (N.m). */
    private double[] torque;

    /**
     * Simple constructor.
     */
    public Maneuver() {
        duration = Double.NaN;
        torque   = new double[3];
        Arrays.fill(torque, Double.NaN);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(epochStart,     ManeuverKey.MAN_EPOCH_START);
        checkNotNaN(duration,        ManeuverKey.MAN_DURATION);
        checkNotNull(refFrameString, ManeuverKey.MAN_REF_FRAME);
        checkNotNaN(torque[0],       ManeuverKey.MAN_TOR_1);
        checkNotNaN(torque[1],       ManeuverKey.MAN_TOR_2);
        checkNotNaN(torque[2],       ManeuverKey.MAN_TOR_3);
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

    /** Check if maneuver has been completed.
     * @return true if maneuver has been completed
     */
    public boolean completed() {
        return !(epochStart     == null ||
                 refFrameString == null ||
                 Double.isNaN(duration + torque[0] + torque[1] + torque[2]));
    }

    /**
     * Get the torque vector (N.m).
     * @return torque vector
     */
    public Vector3D getTorque() {
        return new Vector3D(torque);
    }

    /**
     * Set the torque vector (N.m).
     * @param index vector component index (counting from 0)
     * @param value component value
     */
    public void setTorque(final int index, final double value) {
        refuseFurtherComments();
        this.torque[index] = value;
    }

}
