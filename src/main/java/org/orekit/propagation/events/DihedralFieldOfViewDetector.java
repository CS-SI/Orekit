/* Copyright 2002-2015 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnDecreasing;
import org.orekit.utils.PVCoordinatesProvider;

/** Finder for body entering/exiting dihedral FOV events.
 * <p>This class finds dihedral field of view events (i.e. body entry and exit in FOV).</p>
 * <p>The default implementation behavior is to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#CONTINUE continue}
 * propagation at entry and to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#STOP stop} propagation
 * at exit. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see CircularFieldOfViewDetector
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class DihedralFieldOfViewDetector extends AbstractDetector<DihedralFieldOfViewDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131118L;

    /** Position/velocity provider of the considered target. */
    private final PVCoordinatesProvider targetPVProvider;

    /** Direction of the FOV center. */
    private final Vector3D center;

    /** FOV dihedral axis 1. */
    private final Vector3D axis1;

    /** FOV normal to first center plane. */
    private final Vector3D normalCenterPlane1;

    /** FOV dihedral half aperture angle 1. */
    private final double halfAperture1;

    /** FOV dihedral axis 2. */
    private final Vector3D axis2;

    /** FOV normal to second center plane. */
    private final Vector3D normalCenterPlane2;

    /** FOV dihedral half aperture angle 2. */
    private final double halfAperture2;

    /** Build a new instance.
     * <p>The maximal interval between distance to FOV boundary checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal interval in seconds
     * @param pvTarget Position/velocity provider of the considered target
     * @param center Direction of the FOV center
     * @param axis1 FOV dihedral axis 1
     * @param halfAperture1 FOV dihedral half aperture angle 1
     * @param axis2 FOV dihedral axis 2
     * @param halfAperture2 FOV dihedral half aperture angle 2
     */
    public DihedralFieldOfViewDetector(final double maxCheck,
                                       final PVCoordinatesProvider pvTarget, final Vector3D center,
                                       final Vector3D axis1, final double halfAperture1,
                                       final Vector3D axis2, final double halfAperture2) {
        this(maxCheck, 1.0e-3, DEFAULT_MAX_ITER, new StopOnDecreasing<DihedralFieldOfViewDetector>(),
             pvTarget, center, axis1, halfAperture1, axis2, halfAperture2);
    }

    /** Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param pvTarget Position/velocity provider of the considered target
     * @param center Direction of the FOV center
     * @param axis1 FOV dihedral axis 1
     * @param halfAperture1 FOV dihedral half aperture angle 1
     * @param axis2 FOV dihedral axis 2
     * @param halfAperture2 FOV dihedral half aperture angle 2
     * @since 6.1
     */
    private DihedralFieldOfViewDetector(final double maxCheck, final double threshold,
                                        final int maxIter, final EventHandler<DihedralFieldOfViewDetector> handler,
                                        final PVCoordinatesProvider pvTarget, final Vector3D center,
                                        final Vector3D axis1, final double halfAperture1,
                                        final Vector3D axis2, final double halfAperture2) {
        super(maxCheck, threshold, maxIter, handler);
        this.targetPVProvider = pvTarget;
        this.center = center;

        // Computation of the center plane normal for dihedra 1
        this.axis1              = axis1;
        this.normalCenterPlane1 = Vector3D.crossProduct(axis1, center);

        // Computation of the center plane normal for dihedra 2
        this.axis2              = axis2;
        this.normalCenterPlane2 = Vector3D.crossProduct(axis2, center);

        this.halfAperture1 = halfAperture1;
        this.halfAperture2 = halfAperture2;
    }

    /** {@inheritDoc} */
    @Override
    protected DihedralFieldOfViewDetector create(final double newMaxCheck, final double newThreshold,
                                                 final int newMaxIter,
                                                 final EventHandler<DihedralFieldOfViewDetector> newHandler) {
        return new DihedralFieldOfViewDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                               targetPVProvider, center,
                                               axis1, halfAperture1,
                                               axis2, halfAperture2);
    }

    /** Get the position/velocity provider of the target .
     * @return the position/velocity provider of the target
     */
    public PVCoordinatesProvider getPVTarget() {
        return targetPVProvider;
    }

    /** Get the direction of FOV center.
     * @return the direction of FOV center
     */
    public Vector3D getCenter() {
        return center;
    }

    /** Get the direction of FOV 1st dihedral axis.
     * @return the direction of FOV 1st dihedral axis
     */
    public Vector3D getAxis1() {
        return axis1;
    }

    /** Get the half aperture angle of FOV 1st dihedra.
     * @return the half aperture angle of FOV 1st dihedras
     */
    public double getHalfAperture1() {
        return halfAperture1;
    }

    /** Get the half aperture angle of FOV 2nd dihedra.
     * @return the half aperture angle of FOV 2nd dihedras
     */
    public double getHalfAperture2() {
        return halfAperture2;
    }

    /** Get the direction of FOV 2nd dihedral axis.
     * @return the direction of FOV 2nd dihedral axis
     */
    public Vector3D getAxis2() {
        return axis2;
    }

    /** {@inheritDoc}
     * g function value is the target signed distance to the closest FOV boundary.
     * It is positive inside the FOV, and negative outside. */
    public double g(final SpacecraftState s) throws OrekitException {

        // Get position of target at current date in spacecraft frame.
        final Vector3D targetPosInert = new Vector3D(1, targetPVProvider.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                               -1, s.getPVCoordinates().getPosition());
        final Vector3D targetPosSat = s.getAttitude().getRotation().applyTo(targetPosInert);

        // Compute the four angles from the four FOV boundaries.
        final double angle1 = FastMath.atan2(Vector3D.dotProduct(targetPosSat, normalCenterPlane1),
                                   Vector3D.dotProduct(targetPosSat, center));
        final double angle2 = FastMath.atan2(Vector3D.dotProduct(targetPosSat, normalCenterPlane2),
                                   Vector3D.dotProduct(targetPosSat, center));

        // g function value is distance to the FOV boundary, computed as a dihedral angle.
        // It is positive inside the FOV, and negative outside.
        return FastMath.min(halfAperture1 - FastMath.abs(angle1),  halfAperture2 - FastMath.abs(angle2));
    }

}
