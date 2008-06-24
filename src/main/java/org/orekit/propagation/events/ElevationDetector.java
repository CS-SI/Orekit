/* Copyright 2002-2008 CS Communication & Systèmes
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
 * <p>This class finds elevation events (i.e. satellite raising
 * and setting).</p>
 * <p>The default implementation behavior is to {@link
 * EventDetector#CONTINUE continue} propagation at raising and to
 * {@link EventDetector#STOP stop} propagation
 * at setting. This can be changed by overriding the
 * {@link #eventOccurred(SpacecraftState) eventOccurred} method in a
 * derived class.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class ElevationDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -4635085773191000935L;

    /** Threshold elevation value. */
    private final double elevation;

    /** Topocentric frame in which elevation should be evaluated. */
    private final TopocentricFrame topo;

    /** Build a new instance.
     * <p>The maximal interval between elevation checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal interval in seconds
     * @param elevation threshold elevation value
     * @param topo topocentric frame in which elevation should be evaluated
     */
    public ElevationDetector(final double maxCheck, final double elevation,
                             final TopocentricFrame topo) {
        super(maxCheck, 1.0e-3);
        this.elevation = elevation;
        this.topo = topo;
    }

    /** Handle an elevation event and choose what to do next.
     * <p>The default implementation behavior is to {@link
     * EventDetector#CONTINUE continue} propagation at raising and to
     * {@link EventDetector#STOP stop} propagation at setting. This can
     * be changed by overriding the {@link #eventOccurred(SpacecraftState)
     * eventOccurred} method in a derived class.</p>
     * @param s the current state information : date, kinematics, attitude
     * @return one of {@link #STOP}, {@link #RESET_STATE}, {@link #RESET_DERIVATIVES}
     * or {@link #CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    public int eventOccurred(final SpacecraftState s) throws OrekitException {
        final double zVelocity = s.getPVCoordinates(topo).getVelocity().getZ();
        return (zVelocity > 0) ? CONTINUE : STOP;
    }

    /** {@inheritDoc} */
    public double g(final SpacecraftState s) throws OrekitException {
        return topo.getElevation(s.getPVCoordinates().getPosition(), s.getFrame(), s.getDate()) - elevation;
    }

}
