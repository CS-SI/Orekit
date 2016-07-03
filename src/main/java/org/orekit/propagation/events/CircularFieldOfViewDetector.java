/* Copyright 2002-2016 CS Systèmes d'Information
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnDecreasing;
import org.orekit.utils.PVCoordinatesProvider;

/** Finder for target entry/exit events with respect to a satellite sensor Field Of View.
 * <p>This class handle fields of view with a circular boundary.</p>
 * <p>The default implementation behavior is to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#CONTINUE continue}
 * propagation at FOV entry and to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#STOP stop} propagation
 * at FOV exit. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see FieldOfViewDetector
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class CircularFieldOfViewDetector extends AbstractDetector<CircularFieldOfViewDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131118L;

    /** Position/velocity provider of the considered target. */
    private final PVCoordinatesProvider targetPVProvider;

    /** Direction of the FOV center. */
    private final Vector3D center;

    /** FOV half aperture angle. */
    private final double halfAperture;

    /** Build a new instance.
     * <p>The maximal interval between distance to FOV boundary checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal interval in seconds
     * @param pvTarget Position/velocity provider of the considered target
     * @param center Direction of the FOV center, in spacecraft frame
     * @param halfAperture FOV half aperture angle
     */
    public CircularFieldOfViewDetector(final double maxCheck,
                                       final PVCoordinatesProvider pvTarget,
                                       final Vector3D center,
                                       final double halfAperture) {
        this(maxCheck, 1.0e-3, DEFAULT_MAX_ITER, new StopOnDecreasing<CircularFieldOfViewDetector>(),
             pvTarget, center, halfAperture);
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
     * @param center Direction of the FOV center, in spacecraft frame
     * @param halfAperture FOV half aperture angle
     * @since 6.1
     */
    private CircularFieldOfViewDetector(final double maxCheck, final double threshold,
                                        final int maxIter, final EventHandler<? super CircularFieldOfViewDetector> handler,
                                        final PVCoordinatesProvider pvTarget,
                                        final Vector3D center,
                                        final double halfAperture) {
        super(maxCheck, threshold, maxIter, handler);
        this.targetPVProvider = pvTarget;
        this.center           = center;
        this.halfAperture     = halfAperture;
    }

    /** {@inheritDoc} */
    @Override
    protected CircularFieldOfViewDetector create(final double newMaxCheck, final double newThreshold,
                                                 final int newMaxIter, final EventHandler<? super CircularFieldOfViewDetector> newHandler) {
        return new CircularFieldOfViewDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                               targetPVProvider, center, halfAperture);
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

    /** Get FOV half aperture angle.
     * @return the FOV half aperture angle
     */
    public double getHalfAperture() {
        return halfAperture;
    }

    /** {@inheritDoc}
     * <p>
     * The g function value is the difference between FOV half aperture and the
     * absolute value of the angle between target direction and field of view center.
     * It is positive inside the FOV and negative outside.
     * </p>
     */
    public double g(final SpacecraftState s) throws OrekitException {

        // Compute target position/velocity at date in spacecraft frame */
        final Vector3D targetPosInert = new Vector3D(1, targetPVProvider.getPVCoordinates(s.getDate(), s.getFrame()).getPosition(),
                                           -1, s.getPVCoordinates().getPosition());
        final Vector3D targetPosSat = s.getAttitude().getRotation().applyTo(targetPosInert);

        // Target is in the field of view if the absolute value that angle is smaller than FOV half aperture.
        // g function value is the difference between FOV half aperture and the absolute value of the angle between
        // target direction and field of view center. It is positive inside the FOV and negative outside.
        return halfAperture - Vector3D.angle(targetPosSat, center);
    }

}
