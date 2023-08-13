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

package org.orekit.files.ccsds.ndm.odm.opm;

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;

/** Maneuver in an OPM file.
 * @author sports
 * @since 6.1
 */
public class Maneuver extends CommentsContainer {

    /** Epoch ignition. */
    private AbsoluteDate epochIgnition;

    /** Coordinate system for velocity increment vector, for absolute frames. */
    private FrameFacade referenceFrame;

    /** Duration (value is 0 for impulsive maneuver). */
    private double duration;

    /** Mass change during maneuver (value is < 0). */
    private double deltaMass;

    /** Velocity increment. */
    private double[] dV;

    /** Simple constructor.
     */
    public Maneuver() {
        duration  = Double.NaN;
        deltaMass = Double.NaN;
        dV        = new double[3];
        Arrays.fill(dV, Double.NaN);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(epochIgnition,  ManeuverKey.MAN_EPOCH_IGNITION.name());
        checkNotNull(referenceFrame, ManeuverKey.MAN_REF_FRAME.name());
        checkNotNaN(duration,        ManeuverKey.MAN_DURATION.name());
        checkNotNaN(deltaMass,       ManeuverKey.MAN_DELTA_MASS.name());
        checkNotNaN(dV[0],           ManeuverKey.MAN_DV_1.name());
        checkNotNaN(dV[1],           ManeuverKey.MAN_DV_2.name());
        checkNotNaN(dV[2],           ManeuverKey.MAN_DV_3.name());
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
    public void setEpochIgnition(final AbsoluteDate epochIgnition) {
        this.epochIgnition = epochIgnition;
    }

    /** Get Coordinate system for velocity increment vector.
     * @return coordinate system for velocity increment vector
     */
    public FrameFacade getReferenceFrame() {
        return referenceFrame;
    }

    /** Set Coordinate system for velocity increment vector.
     * @param referenceFrame coordinate system for velocity increment vector
     */
    public void setReferenceFrame(final FrameFacade referenceFrame) {
        this.referenceFrame = referenceFrame;
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
                 referenceFrame == null ||
                 Double.isNaN(duration + deltaMass + dV[0] + dV[1] + dV[2]));
    }

}
