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
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;

/** Finder for satellite raising/setting events.
 * <p>This class finds elevation events (i.e. satellite raising and setting).</p>
 * <p>The default implementation behavior is to {@link
 * EventDetector#CONTINUE continue} propagation at raising and to
 * {@link EventDetector#STOP stop} propagation
 * at setting. This can be changed by overriding the
 * {@link #eventOccurred(SpacecraftState, boolean) eventOccurred} method in a
 * derived class.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class ElevationDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 4571340030201230951L;

    /** Threshold elevation value. */
    private final double elevation;

    /** Topocentric frame in which elevation should be evaluated. */
    private final TopocentricFrame topo;

    /** Build a new elevation detector.
     * <p>This simple constructor takes default values for maximal checking
     *  interval ({@link #DEFAULT_MAXCHECK}) and convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     * @param elevation threshold elevation value (°)
     * @param topo topocentric frame in which elevation should be evaluated
     */
    public ElevationDetector(final double elevation, final TopocentricFrame topo) {
        super(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
        this.elevation = elevation;
        this.topo = topo;
    }

    /** Build a new elevation detector.
     * <p>This constructor takes default value for convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     * <p>The maximal interval between elevation checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param elevation threshold elevation value (°)
     * @param topo topocentric frame in which elevation should be evaluated
     */
    public ElevationDetector(final double maxCheck,
                             final double elevation,
                             final TopocentricFrame topo) {
        super(maxCheck, DEFAULT_THRESHOLD);
        this.elevation = elevation;
        this.topo = topo;
    }

    /** Build a new elevation detector.
     * <p>The maximal interval between elevation checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param elevation threshold elevation value (°)
     * @param topo topocentric frame in which elevation should be evaluated
     */
    public ElevationDetector(final double maxCheck,
                             final double threshold,
                             final double elevation,
                             final TopocentricFrame topo) {
        super(maxCheck, threshold);
        this.elevation = elevation;
        this.topo = topo;
    }

    /** Get the threshold elevation value (°).
     * @return the threshold elevation value
     */
    public double getElevation() {
        return elevation;
    }

    /** Get the topocentric frame.
     * @return the topocentric frame
     */
    public TopocentricFrame getTopocentricFrame() {
        return topo;
    }

    /** Handle an elevation event and choose what to do next.
     * <p>The default implementation behavior is to {@link
     * EventDetector#CONTINUE continue} propagation at raising and to
     * {@link EventDetector#STOP stop} propagation at setting.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases
     * when times increases around event.
     * @return {@link #STOP} or {@link #CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    public int eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        return increasing ? CONTINUE : STOP;
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the current elevation
     * and the threshold elevation.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        return topo.getElevation(s.getPVCoordinates().getPosition(), s.getFrame(), s.getDate()) - elevation;
    }

}
