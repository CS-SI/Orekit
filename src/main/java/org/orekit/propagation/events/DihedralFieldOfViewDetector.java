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

/** Finder for body entering/exiting dihedral fov events.
 * <p>This class finds dihedral field of view events (i.e. body entry and exit in fov).</p>
 * <p>The default implementation behavior is to {@link
 * EventDetector#CONTINUE continue} propagation at entry and to
 * {@link EventDetector#STOP stop} propagation
 * at exit. This can be changed by overriding the
 * {@link #eventOccurred(SpacecraftState, boolean) eventOccurred} method in a
 * derived class.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see CircularFieldOfViewDetector
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision: 2761 $ $Date: 2009-04-28 17:49:07 +0200 (mar., 28 avr. 2009) $
 */
public class DihedralFieldOfViewDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 4571340030201230951L;

    /** Position/velocity provider of the considered target. */
    private final PVCoordinatesProvider targetPVProvider;

    /** Direction of the fov center. */
    private final Vector3D center;

    /** Fov dihedral axis 1. */
    private final Vector3D normalCenterPlane1;

    /** Fov dihedral half aperture angle 1. */
    private final double halfAperture1;

    /** Fov dihedral axis 2. */
    private final Vector3D normalCenterPlane2;

    /** Fov dihedral half aperture angle 2. */
    private final double halfAperture2;

    /** Build a new instance.
     * <p>The maximal interval between distance to fov boundary checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal interval in seconds
     * @param pvTarget Position/velocity provider of the considered target
     * @param center Direction of the fov center
     * @param axis1 Fov dihedral axis 1
     * @param halfAperture1 Fov dihedral half aperture angle 1
     * @param axis2 Fov dihedral axis 2
     * @param halfAperture2 Fov dihedral half aperture angle 2
     */
    public DihedralFieldOfViewDetector(final double maxCheck,
            final PVCoordinatesProvider pvTarget, final Vector3D center, final Vector3D axis1, final double halfAperture1,
            final Vector3D axis2, final double halfAperture2) {
        super(maxCheck, 1.0e-3);
        this.targetPVProvider = pvTarget;
        this.center = center;

        // Computation of the center plane normal for dihedra 1
        this.normalCenterPlane1 = Vector3D.crossProduct(axis1, center);

        // Computation of the center plane normal for dihedra 2
        this.normalCenterPlane2 = Vector3D.crossProduct(axis2, center);

        this.halfAperture1 = halfAperture1;
        this.halfAperture2 = halfAperture2;
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

    /** Get the direction of fov 1st dihedral axis.
     * @return the direction of fov 1st dihedral axis
     */
    public Vector3D getAxis1() {
        return Vector3D.crossProduct(center, normalCenterPlane1);
    }

    /** Get the half aperture angle of fov 1st dihedra.
     * @return the half aperture angle of fov 1st dihedras
     */
    public double getHalfAperture1() {
        return halfAperture1;
    }

    /** Get the half aperture angle of fov 2nd dihedra.
     * @return the half aperture angle of fov 2nd dihedras
     */
    public double getHalfAperture2() {
        return halfAperture2;
    }

    /** Get the direction of fov 2nd dihedral axis.
     * @return the direction of fov 2nd dihedral axis
     */
    public Vector3D getAxis2() {
        return Vector3D.crossProduct(center, normalCenterPlane2);
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
     * g function value is the target signed distance to the closest fov boundary.
     * It is positive inside the fov, and negative outside. */
    public double g(final SpacecraftState s) throws OrekitException {

        // Get position of target at current date in spacecraft frame.
        final Vector3D targetPosInert = new Vector3D(1, targetPVProvider.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                               -1, s.getPVCoordinates().getPosition());
        final Vector3D targetPosSat = s.getAttitude().getRotation().applyTo(targetPosInert);

        // Compute the four angles from the four fov boundaries.
        final double angle1 = Math.atan2(Vector3D.dotProduct(targetPosSat, normalCenterPlane1),
                                   Vector3D.dotProduct(targetPosSat, center));
        final double angle2 = Math.atan2(Vector3D.dotProduct(targetPosSat, normalCenterPlane2),
                                   Vector3D.dotProduct(targetPosSat, center));

        // g function value is distance to the fov boundary, computed as a dihedral angle.
        // It is positive inside the fov, and negative outside.
        return Math.min(halfAperture1 - Math.abs(angle1) ,  halfAperture2 - Math.abs(angle2));
    }

}
