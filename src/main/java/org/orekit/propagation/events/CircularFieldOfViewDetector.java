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

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.PVCoordinatesProvider;

/** Finder for target entry/exit events with respect to a satellite sensor field of view.
 * <p>This class handle fields of view with a circular boundary.</p>
 * <p>The default implementation behavior is to {@link
 * EventDetector#CONTINUE continue} propagation at fov entry and to
 * {@link EventDetector#STOP stop} propagation
 * at fov exit. This can be changed by overriding the
 * {@link #eventOccurred(SpacecraftState, boolean) eventOccurred} method in a
 * derived class.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see DihedralFieldOfViewDetector
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class CircularFieldOfViewDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 4571340030201230951L;

    /** Position/velocity provider of the considered target. */
    private final PVCoordinatesProvider targetPVProvider;

    /** Direction of the fov center. */
    private final Vector3D center;

    /** Fov half aperture angle. */
    private final double halfAperture;

    /** Build a new instance.
     * <p>The maximal interval between distance to fov boundary checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal interval in seconds
     * @param pvTarget Position/velocity provider of the considered target
     * @param center Direction of the fov center, in spacecraft frame
     * @param halfAperture Fov half aperture angle
     */
    public CircularFieldOfViewDetector(final double maxCheck,
            final PVCoordinatesProvider pvTarget, final Vector3D center, final double halfAperture) {
        super(maxCheck, 1.0e-3);
        this.targetPVProvider = pvTarget;
        this.center = center;
        this.halfAperture = halfAperture;
    }

    /** Get the position/velocity provider of the target .
     * @return the position/velocity provider of the target
     */
    public PVCoordinatesProvider getPVTarget() {
        return targetPVProvider;
    }

    /** Get the direction of fov center.
     * @return the direction of fov center
     */
    public Vector3D getCenter() {
        return center;
    }

    /** Get fov half aperture angle.
     * @return the fov half aperture angle
     */
    public double getHalfAperture() {
        return halfAperture;
    }

    /** Handle an fov event and choose what to do next.
     * <p>The default implementation behavior is to {@link
     * EventDetector#CONTINUE continue} propagation at entry and to
     * {@link EventDetector#STOP stop} propagation at exit. This can
     * be changed by overriding the {@link #eventOccurred(SpacecraftState, boolean)
     * eventOccurred} method in a derived class.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases
     * when times increases around event, i.e. target enters the fov (note that increase
     * is measured with respect to physical time, not with respect to propagation which
     * may go backward in time)
     * @return one of {@link #STOP}, {@link #RESET_STATE}, {@link #RESET_DERIVATIVES}
     * or {@link #CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    public int eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        return increasing ? CONTINUE : STOP;
    }

    /** {@inheritDoc}
     * g function value is the difference between fov half aperture and the absolute value of the angle between
     * target direction and field of view center. It is positive inside the fov and negative outside. */
    public double g(final SpacecraftState s) throws OrekitException {

        // Compute target position/velocity at date in spacecraft frame */
        final Vector3D targetPosInert = new Vector3D(1, targetPVProvider.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                           -1, s.getPVCoordinates().getPosition());
        final Vector3D targetPosSat = s.getAttitude().getRotation().applyTo(targetPosInert);

        // Target is in the field of view if the absolute value that angle is smaller than fov half aperture.
        // g function value is the difference between fov half aperture and the absolute value of the angle between
        // target direction and field of view center. It is positive inside the fov and negative outside.
        return halfAperture - Vector3D.angle(targetPosSat, center);
    }

}
