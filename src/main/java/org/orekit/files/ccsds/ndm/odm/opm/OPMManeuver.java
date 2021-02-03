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

package org.orekit.files.ccsds.ndm.odm.opm;

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.time.AbsoluteDate;

/** Maneuver in an OPM file.
 * @author sports
 * @since 6.1
 */
public class OPMManeuver extends CommentsContainer {

    /** Epoch ignition. */
    private AbsoluteDate epochIgnition;

    /** Coordinate system for velocity increment vector, for Local Orbital Frames. */
    private LOFType refLofType;

    /** Coordinate system for velocity increment vector, for absolute frames. */
    private Frame refFrame;

    /** Duration (value is 0 for impulsive maneuver). */
    private double duration;

    /** Mass change during maneuver (value is < 0). */
    private double deltaMass;

    /** Velocity increment. */
    private double[] dV;

    /** Simple constructor.
     */
    public OPMManeuver() {
        duration  = Double.NaN;
        deltaMass = Double.NaN;
        dV        = new double[3];
        Arrays.fill(dV, Double.NaN);
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        checkNotNull(epochIgnition, OPMManeuverKey.MAN_EPOCH_IGNITION);
        if (refLofType == null && refFrame == null) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY,
                                      OPMManeuverKey.MAN_REF_FRAME.name());
        }
        checkNotNaN(duration,  OPMManeuverKey.MAN_DURATION);
        checkNotNaN(deltaMass, OPMManeuverKey.MAN_DELTA_MASS);
        checkNotNaN(dV[0],     OPMManeuverKey.MAN_DV_1);
        checkNotNaN(dV[1],     OPMManeuverKey.MAN_DV_2);
        checkNotNaN(dV[2],     OPMManeuverKey.MAN_DV_3);
    }

    /** Get epoch ignition.
     * @return epoch ignition
     */
    public AbsoluteDate getEpochIgnition() {
        return epochIgnition;
    }

    /** Set epoch ignition.
     * @param epochIgnition epoch ignition
     */
    void setEpochIgnition(final AbsoluteDate epochIgnition) {
        this.epochIgnition = epochIgnition;
    }

    /** Get coordinate system for velocity increment vector, for Local Orbital Frames.
     * @return coordinate system for velocity increment vector, for Local Orbital Frames
     */
    public LOFType getRefLofType() {
        return refLofType;
    }

    /** Set coordinate system for velocity increment vector, for Local Orbital Frames.
     * @param refLofType coordinate system for velocity increment vector, for Local Orbital Frames
     */
    public void setRefLofType(final LOFType refLofType) {
        this.refLofType = refLofType;
        this.refFrame   = null;
    }

    /** Get Coordinate system for velocity increment vector, for absolute frames.
     * @return coordinate system for velocity increment vector, for absolute frames
     */
    public Frame getRefFrame() {
        return refFrame;
    }

    /** Set Coordinate system for velocity increment vector, for absolute frames.
     * @param refFrame coordinate system for velocity increment vector, for absolute frames
     */
    public void setRefFrame(final Frame refFrame) {
        this.refLofType = null;
        this.refFrame   = refFrame;
    }

    /** Get duration (value is 0 for impulsive maneuver).
     * @return duration (value is 0 for impulsive maneuver)
     */
    public double getDuration() {
        return duration;
    }

    /** Set duration (value is 0 for impulsive maneuver).
     * @param duration duration (value is 0 for impulsive maneuver)
     */
    public void setDuration(final double duration) {
        this.duration = duration;
    }

    /** Get mass change during maneuver (value is &lt; 0).
     * @return mass change during maneuver (value is &lt; 0)
     */
    public double getDeltaMass() {
        return deltaMass;
    }

    /** Set mass change during maneuver (value is &lt; 0).
     * @param deltaMass mass change during maneuver (value is &lt; 0)
     */
    public void setDeltaMass(final double deltaMass) {
        this.deltaMass = deltaMass;
    }

    /** Get velocity increment.
     * @return velocity increment
     */
    public Vector3D getDV() {
        return new Vector3D(dV);
    }

    /** Set velocity increment component.
     * @param i component index
     * @param dVi velocity increment component
     */
    public void setDV(final int i, final double dVi) {
        dV[i] = dVi;
    }

    /** Check if maneuver has been completed.
     * @return true if maneuver has been completed
     */
    public boolean completed() {
        return !(epochIgnition == null ||
                 (refLofType == null && refFrame == null) ||
                 Double.isNaN(duration + deltaMass + dV[0] + dV[1] + dV[2]));
    }

}
