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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.files.ccsds.definitions.FrameFacade;
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

    /** Coordinate system for the torque vector. */
    private FrameFacade frame;

    /** Duration (value is 0 for impulsive maneuver). */
    private double duration;

    /** Torque vector (N.m). */
    private double[] torque;

    /** Mass change during maneuver (kg).
     * @since 12.0
     */
    private double deltaMass;

    /**
     * Simple constructor.
     */
    public Maneuver() {
        duration  = Double.NaN;
        torque    = new double[3];
        deltaMass = Double.NaN;
        Arrays.fill(torque, Double.NaN);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(epochStart, ManeuverKey.MAN_EPOCH_START.name());
        checkNotNaN(duration,    ManeuverKey.MAN_DURATION.name());
        checkNotNull(frame,      ManeuverKey.MAN_REF_FRAME.name());
        checkNotNaN(torque[0],   ManeuverKey.MAN_TOR_1.name());
        checkNotNaN(torque[1],   ManeuverKey.MAN_TOR_2.name());
        checkNotNaN(torque[2],   ManeuverKey.MAN_TOR_3.name());
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
     * Get Coordinate system for the torque vector.
     * @return coordinate system for the torque vector
     */
    public FrameFacade getFrame() {
        return frame;
    }

    /**
     * Set Coordinate system for the torque vector.
     * @param frame coordinate system for the torque vector
     */
    public void setFrame(final FrameFacade frame) {
        refuseFurtherComments();
        this.frame = frame;
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

    /**
     * Get mass change during maneuver.
     * @return mass change during maneuver (kg, negative)
     * @since 12.0
     */
    public double getDeltaMass() {
        return deltaMass;
    }

    /**
     * Set mass change during maneuver.
     * @param deltaMass mass change during maneuver (kg)
     * @since 12.0
     */
    public void setDeltaMass(final double deltaMass) {
        refuseFurtherComments();
        this.deltaMass = deltaMass;
    }

}
