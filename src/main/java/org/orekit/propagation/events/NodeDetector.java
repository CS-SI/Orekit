/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.propagation.events;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;

/** Finder for node crossing events.
 * <p>This class finds equator crossing events (i.e. ascending
 * or descending node crossing).</p>
 * <p>The default implementation behavior is to {@link
 * EventDetector#CONTINUE continue} propagation at descending node
 * crossing and to {@link EventDetector#STOP stop} propagation
 * at ascending node crossing. This can be changed by overriding the
 * {@link #eventOccurred(SpacecraftState, boolean) eventOccurred} method in a
 * derived class.</p>
 * <p>Beware that node detection will fail for almost equatorial orbits. If
 * for example a node detector is used to trigger an {@link
 * org.orekit.forces.maneuvers.ImpulseManeuver ImpulseManeuver} and the maneuver
 * turn the orbit plane to equator, then the detector may completely fail just
 * after the maneuver has been performed! This is a real case that has been
 * encountered during validation ...</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class NodeDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 601812664015866572L;

    /** Frame in which the equator is defined. */
    private final Frame frame;

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the max check interval
     * to period/3 and to set the convergence threshold according to orbit size.</p>
     * @param orbit initial orbit
     * @param frame frame in which the equator is defined (typical
     * values are {@link org.orekit.frames.FramesFactory#getEME2000() J<sub>2000</sub>} or
     * {@link org.orekit.frames.FramesFactory#getITRF2005() ITRF 2005})
     */
    public NodeDetector(final Orbit orbit, final Frame frame) {
        super(orbit.getKeplerianPeriod() / 3, 1.0e-13 * orbit.getKeplerianPeriod());
        this.frame  = frame;
    }

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the max check interval
     * to period/3.</p>
     * @param threshold convergence threshold (s)
     * @param orbit initial orbit
     * @param frame frame in which the equator is defined (typical
     * values are {@link org.orekit.frames.FramesFactory#getEME2000() J<sub>2000</sub>} or
     * {@link org.orekit.frames.FramesFactory#getITRF2005() ITRF 2005})
     */
    public NodeDetector(final double threshold, final Orbit orbit, final Frame frame) {
        super(orbit.getKeplerianPeriod() / 3, threshold);
        this.frame  = frame;
    }

    /** Get the frame in which the equator is defined.
     * @return the frame in which the equator is defined
     */
    public Frame getFrame() {
        return frame;
    }

    /** Handle a node crossing event and choose what to do next.
     * <p>The default implementation behavior is to {@link
     * EventDetector#CONTINUE continue} propagation at descending node
     * crossing and to {@link EventDetector#STOP stop} propagation
     * at ascending node crossing.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases
     * when times increases around event
     * @return {@link #STOP} or {@link #CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    public int eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        return increasing ? STOP : CONTINUE;
    }

    /** Compute the value of the switching function.
     * This function computes the Z position in the defined frame.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        return s.getPVCoordinates(frame).getPosition().getZ();
    }

}
