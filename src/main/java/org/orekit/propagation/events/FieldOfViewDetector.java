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
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.utils.PVCoordinatesProvider;

/** Finder for target entry/exit events with respect to a satellite sensor
 * {@link FieldOfView Field Of View}.
 * <p>The default implementation behavior is to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#CONTINUE continue}
 * propagation at FOV entry and to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#STOP stop} propagation
 * at FOV exit. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see CircularFieldOfViewDetector
 * @see FootprintOverlapDetector
 * @author Luc Maisonobe
 * @since 7.1
 */
public class FieldOfViewDetector extends AbstractDetector<FieldOfViewDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20160114L;

    /** Position/velocity provider of the considered target. */
    private final PVCoordinatesProvider targetPVProvider;

    /** Field of view. */
    private final FieldOfView fov;

    /** Build a new instance.
     * <p>The maximal interval between distance to FOV boundary checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param pvTarget Position/velocity provider of the considered target
     * @param fov Field Of View
     */
    public FieldOfViewDetector(final PVCoordinatesProvider pvTarget, final FieldOfView fov) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
             new StopOnIncreasing<FieldOfViewDetector>(),
             pvTarget, fov);
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
     * @param fov Field Of View
     */
    private FieldOfViewDetector(final double maxCheck, final double threshold, final int maxIter,
                                final EventHandler<? super FieldOfViewDetector> handler,
                                final PVCoordinatesProvider pvTarget, final FieldOfView fov) {
        super(maxCheck, threshold, maxIter, handler);
        this.targetPVProvider = pvTarget;
        this.fov              = fov;
    }

    /** {@inheritDoc} */
    @Override
    protected FieldOfViewDetector create(final double newMaxCheck, final double newThreshold,
                                         final int newMaxIter,
                                         final EventHandler<? super FieldOfViewDetector> newHandler) {
        return new FieldOfViewDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                       targetPVProvider, fov);
    }

    /** Get the position/velocity provider of the target .
     * @return the position/velocity provider of the target
     */
    public PVCoordinatesProvider getPVTarget() {
        return targetPVProvider;
    }

    /** Get the Field Of View.
     * @return Field Of View
     */
    public FieldOfView getFieldOfView() {
        return fov;
    }

    /** {@inheritDoc}
     * <p>
     * The g function value is the angular offset between the
     * target and the {@link FieldOfView#offsetFromBoundary(Vector3D)
     * Field Of View boundary}. It is negative if the target is
     * visible within the Field Of View and positive if it is outside of the
     * Field Of View, including the margin.
     * </p>
     * <p>
     * As per the previous definition, when the target enters the Field Of
     * View, a decreasing event is generated, and when the target leaves
     * the Field Of View, an increasing event is generated.
     * </p>
     */
    public double g(final SpacecraftState s) throws OrekitException {

        // get line of sight in spacecraft frame
        final Vector3D targetPosInert =
                targetPVProvider.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D lineOfSightSC = s.toTransform().transformPosition(targetPosInert);

        return fov.offsetFromBoundary(lineOfSightSC);

    }

}
