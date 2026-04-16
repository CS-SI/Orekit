/* Copyright 2002-2026 CS GROUP
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

package org.orekit.propagation.relative;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.control.heuristics.lambert.LambertSolution;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeUtils;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This class stores the solution of a two-impulse rendez-vous. All the contained variables are expressed in the same frame,
 * accessible through the method getFrame().
 * Since the PVT of the chaser on both ends of the transfer orbit are stored in this object in addition to the ΔV vectors,
 * it is possible to reconstruct the orbit of the chaser before and after the transfer.
 *
 * @author Jérôme Tabeaud
 * @author Romain Cuvillon
 * @since 14.0
 */
public class TwoImpulseTransfer {
    /**
     * PVT of the chaser just after the first maneuver.
     */
    private final TimeStampedPVCoordinates pvt1;

    /**
     * PVT of the chaser just before the second maneuver.
     */
    private final TimeStampedPVCoordinates pvt2;

    /**
     * ΔV vector of first maneuver.
     */
    private final Vector3D deltaV1;

    /**
     * ΔV vector of second maneuver.
     */
    private final Vector3D deltaV2;

    /**
     * Frame in which the PVT and ΔV are expressed.
     */
    private final Frame frame;

    /**
     * Creates a new TwoImpulseTransfer object from the given PVT and ΔV vectors.
     *
     * @param pvt1    PVT at the start of the transfer.
     * @param pvt2    PVT at the start of the transfer.
     * @param deltaV1 ΔV to enter the transfer orbit.
     * @param deltaV2 ΔV to exit the transfer orbit.
     * @param frame   Frame in which the PVT and ΔV vector are expressed.
     */
    public TwoImpulseTransfer(final TimeStampedPVCoordinates pvt1, final TimeStampedPVCoordinates pvt2, final Vector3D deltaV1, final Vector3D deltaV2, final Frame frame) {
        this.pvt1 = pvt1;
        this.pvt2 = pvt2;
        this.deltaV1 = deltaV1;
        this.deltaV2 = deltaV2;
        this.frame = frame;
    }

    /**
     * <p>Creates a new {@link TwoImpulseTransfer} object from a {@link LambertSolution} and the PVs from the departure and destination orbits.</p>
     * <p>The ΔV vectors are automatically computed.</p>
     * @param solution Lambert solver's solution.
     * @param v1BeforeMan PV before departure manoeuvre on initial orbit.
     * @param v2AfterMan PV after arrival manoeuvre on destination orbit.
     */
    public TwoImpulseTransfer(final LambertSolution solution, final Vector3D v1BeforeMan, final Vector3D v2AfterMan) {
        final TimeStampedPVCoordinates pvt1AfterMan = new TimeStampedPVCoordinates(solution.getBoundaryConditions().getInitialDate(), solution.getBoundaryConditions().getInitialPosition(), solution.getBoundaryVelocities().getInitialVelocity());
        final TimeStampedPVCoordinates pvt2BeforeMan = new TimeStampedPVCoordinates(solution.getBoundaryConditions().getTerminalDate(), solution.getBoundaryConditions().getTerminalPosition(), solution.getBoundaryVelocities().getTerminalVelocity());
        this.pvt1 = pvt1AfterMan;
        this.pvt2 = pvt2BeforeMan;
        this.deltaV1 = pvt1AfterMan.getVelocity().subtract(v1BeforeMan);
        this.deltaV2 = pvt2BeforeMan.getVelocity().subtract(v2AfterMan);
        this.frame = solution.getBoundaryConditions().getReferenceFrame();
    }

    /**
     * Creates a new {@link TwoImpulseTransfer} object from the given PVT before the first maneuver and after the second maneuver (on the initial and final orbits), as well as the velocity vectors after the first maneuver and before the second maneuver (on the transfer orbit).
     *
     * @param pvt1BeforeMan PVT before the departure maneuver.
     * @param v1AfterMan    Velocity after the departure maneuver.
     * @param pvt2AfterMan  PVT after the rendez-vous maneuver.
     * @param v2BeforeMan   Velocity before the rendez-vous maneuver.
     * @param frame         Frame in which the PVT and velocity vectors are expressed.
     * @return TwoImpulseTransfer object that corresponds to the inputs.
     */
    public static TwoImpulseTransfer  fromPVTAndVelocities(final TimeStampedPVCoordinates pvt1BeforeMan, final Vector3D v1AfterMan, final TimeStampedPVCoordinates pvt2AfterMan, final Vector3D v2BeforeMan, final Frame frame) {
        final TimeStampedPVCoordinates pvt1AfterMan = new TimeStampedPVCoordinates(pvt1BeforeMan.getDate(), pvt1BeforeMan.getPosition(), v1AfterMan);
        final TimeStampedPVCoordinates pvt2BeforeMan = new TimeStampedPVCoordinates(pvt2AfterMan.getDate(), pvt2AfterMan.getPosition(), v2BeforeMan);
        final Vector3D deltaV1 = pvt1AfterMan.getVelocity().subtract(pvt1BeforeMan.getVelocity());
        final Vector3D deltaV2 = pvt2AfterMan.getVelocity().subtract(pvt2BeforeMan.getVelocity());
        return new TwoImpulseTransfer(pvt1AfterMan, pvt2BeforeMan, deltaV1, deltaV2, frame);
    }

    /**
     * @return PVT of the chaser just after the first maneuver.
     */
    public TimeStampedPVCoordinates getPvt1() {
        return pvt1;
    }

    /**
     * @return PVT of the chaser just before the second maneuver.
     */
    public TimeStampedPVCoordinates getPvt2() {
        return pvt2;
    }

    /**
     * @return The ΔV vector of first maneuver.
     */
    public Vector3D getDeltaV1() {
        return deltaV1;
    }

    /**
     * @return The ΔV vector of second maneuver.
     */
    public Vector3D getDeltaV2() {
        return deltaV2;
    }

    /**
     * @param outputFrame given frame for the first maneuver.
     * @return PVT of the chaser just after the first maneuver.
     */
    public TimeStampedPVCoordinates getPvt1(final Frame outputFrame) {
        return frame.getTransformTo(outputFrame, pvt1.getDate()).transformPVCoordinates(pvt1);
    }

    /**
     * @param outputFrame given frame for the 2nd maneuver.
     * @return PVT of the chaser just before the second maneuver.
     */
    public TimeStampedPVCoordinates getPvt2(final Frame outputFrame) {
        return frame.getTransformTo(outputFrame, pvt2.getDate()).transformPVCoordinates(pvt2);
    }

    /**
     * @param outputFrame given frame for the first maneuver.
     * @return The ΔV vector of first maneuver in the given frame at the given date.
     */
    public Vector3D getDeltaV1(final Frame outputFrame) {
        return frame.getTransformTo(outputFrame, pvt1.getDate()).transformVector(deltaV1);
    }

    /**
     * @param outputFrame given frame for the 2nd maneuver.
     * @return The ΔV vector of second maneuver in the given frame at the given date.
     */
    public Vector3D getDeltaV2(final Frame outputFrame) {
        return frame.getTransformTo(outputFrame, pvt2.getDate()).transformVector(deltaV2);
    }

    /**
     * Computes the total ΔV of the transfer.
     *
     * @return ||ΔV_total|| = ||ΔV1|| + ||ΔV2||
     */
    public double getTotalDeltaV() {
        return deltaV1.getNorm() + deltaV2.getNorm();
    }

    /**
     * @return The duration of the transfer between the two impulses.
     */
    public double getDuration() {
        return pvt2.durationFrom(pvt1);
    }

    /**
     * Returns the departure date.
     *
     * @return the departure date.
     */
    public AbsoluteDate getDepartureDate() {
        return pvt1.getDate();
    }

    /**
     * Returns the arrival date.
     *
     * @return the arrival date.
     */
    public AbsoluteDate getArrivalDate() {
        return pvt2.getDate();
    }

    /**
     * Returns the frame in which all elements contained in the object are expressed.
     *
     * @return The frame of the elements of the object.
     */
    public Frame getFrame() {
        return frame;
    }

    /**
     * @return The PVT before the injection maneuver into the transfer orbit.
     */
    public TimeStampedPVCoordinates getPvt1BeforeMan() {
        return new TimeStampedPVCoordinates(pvt1.getDate(), pvt1.getPosition(), pvt1.getVelocity().subtract(deltaV1));
    }

    /**
     * @return The PVT after the velocity-synchronization maneuver into the target orbit.
     */
    public TimeStampedPVCoordinates getPvt2AfterMan() {
        return new TimeStampedPVCoordinates(pvt2.getDate(), pvt2.getPosition(), pvt2.getVelocity().add(deltaV2));
    }

    /**
     * Converts all the PVT and ΔV vectors to the desired frame.
     *
     * @param outputFrame Desired output frame.
     * @return TwoImpulseTransfer object expressed in the desired frame.
     */
    public TwoImpulseTransfer expressInFrame(final Frame outputFrame) {
        return new TwoImpulseTransfer(
                getPvt1(outputFrame),
                getPvt2(outputFrame),
                getDeltaV1(outputFrame),
                getDeltaV2(outputFrame),
                outputFrame);
    }

    /**
     * Writes a string summarizing the transfer characteristics (PVT and ΔV vectors).
     * @return The string summary.
     */
    public String toString() {
        return "Two impulse transfer\n" +
                "\n    Frame:                       " + getFrame().getName() +
                "\n    PVT after first manoeuvre:   " + getPvt1() +
                "\n    PVT before second manoeuvre: " + getPvt2() +
                "\n    ΔV of first manoeuvre:       " + getDeltaV1() + " ; ‖ΔV₁‖ = " + getDeltaV1().getNorm() +
                "\n    ΔV of second manoeuvre:      " + getDeltaV2() + " ; ‖ΔV₂‖ = " + getDeltaV2().getNorm() +
                "\n    Total ΔV:                    " + getTotalDeltaV() +
                "\n    Duration:                    " + TimeUtils.secondsToDHMS(getDuration()) + "\n";
    }
}
