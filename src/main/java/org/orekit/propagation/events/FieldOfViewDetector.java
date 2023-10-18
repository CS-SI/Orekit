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
package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.geometry.fov.FieldOfView;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.utils.PVCoordinatesProvider;

/** Finder for target entry/exit events with respect to a satellite sensor
 * {@link FieldOfView Field Of View}.
 * <p>Beware that this detector is unaware of any bodies occluding line-of-sight to
 * the target. It can be therefore used for many contexts from Earth Observation to
 * interplanetary mission design. For instance, in an Earth Observation context,
 * it can be easily combined to an {@link ElevationDetector} using
 * {@link BooleanDetector#andCombine(java.util.Collection)} to calculate station
 * visibility opportunities within the satellite's field of view.
 * <p>The default implementation behavior is to {@link Action#CONTINUE continue}
 * propagation at FOV entry and to {@link Action#STOP stop} propagation
 * at FOV exit. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see FootprintOverlapDetector
 * @see VisibilityTrigger
 * @author Luc Maisonobe
 * @since 7.1
 */
public class FieldOfViewDetector extends AbstractDetector<FieldOfViewDetector> {

    /** Position/velocity provider of the considered target. */
    private final PVCoordinatesProvider targetPVProvider;

    /** Radius of the target, considered to be a spherical body (m). */
    private final double radiusTarget;

    /** Visibility trigger for spherical bodies. */
    private final VisibilityTrigger trigger;

    /** Field of view. */
    private final FieldOfView fov;

    /** Build a new instance.
     * <p>The maximal interval between distance to FOV boundary checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param pvTarget Position/velocity provider of the considered target
     * @param fov Field Of View
     * @since 10.1
     */
    public FieldOfViewDetector(final PVCoordinatesProvider pvTarget, final FieldOfView fov) {
        this(pvTarget, 0.0, VisibilityTrigger.VISIBLE_AS_SOON_AS_PARTIALLY_IN_FOV, fov);
    }

    /** Build a new instance.
     * <p>The maximal interval between distance to FOV boundary checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param pvTarget Position/velocity provider of the considered target
     * @param radiusTarget radius of the target, considered to be a spherical body (m)
     * @param trigger visibility trigger for spherical bodies
     * @param fov Field Of View
     * @since 10.1
     */
    public FieldOfViewDetector(final PVCoordinatesProvider pvTarget, final double radiusTarget,
                               final VisibilityTrigger trigger, final FieldOfView fov) {
        this(s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
             new StopOnIncreasing(),
             pvTarget, radiusTarget, trigger, fov);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param pvTarget Position/velocity provider of the considered target
     * @param radiusTarget radius of the target, considered to be a spherical body (m)
     * @param trigger visibility trigger for spherical bodies
     * @param fov Field Of View
     */
    protected FieldOfViewDetector(final AdaptableInterval maxCheck, final double threshold, final int maxIter,
                                  final EventHandler handler,
                                  final PVCoordinatesProvider pvTarget, final double radiusTarget,
                                  final VisibilityTrigger trigger, final FieldOfView fov) {
        super(maxCheck, threshold, maxIter, handler);
        this.targetPVProvider = pvTarget;
        this.radiusTarget     = radiusTarget;
        this.trigger          = trigger;
        this.fov              = fov;
    }

    /** {@inheritDoc} */
    @Override
    protected FieldOfViewDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                         final int newMaxIter,
                                         final EventHandler newHandler) {
        return new FieldOfViewDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                       targetPVProvider, radiusTarget, trigger, fov);
    }

    /** Get the position/velocity provider of the target .
     * @return the position/velocity provider of the target
     */
    public PVCoordinatesProvider getPVTarget() {
        return targetPVProvider;
    }

    /** Get the Field Of View.
     * @return Field Of View
     * @since 10.1
     */
    public FieldOfView getFOV() {
        return fov;
    }

    /** {@inheritDoc}
     * <p>
     * The g function value is the angular offset between the
     * target center and the {@link FieldOfView#offsetFromBoundary(Vector3D,
     * double, VisibilityTrigger) Field Of View boundary}, plus or minus the
     * target angular radius depending on the {@link VisibilityTrigger}, minus
     * the {@link FieldOfView#getMargin() Field Of View margin}. It is therefore
     * negative if the target is visible within the Field Of View and positive
     * if it is outside of the Field Of View.
     * </p>
     * <p>
     * As per the previous definition, when the target enters the Field Of
     * View, a decreasing event is generated, and when the target leaves
     * the Field Of View, an increasing event is generated.
     * </p>
     */
    public double g(final SpacecraftState s) {

        // get line of sight in spacecraft frame
        final Vector3D targetPosInert =
                targetPVProvider.getPosition(s.getDate(), s.getFrame());
        final Vector3D lineOfSightSC = s.toTransform().transformPosition(targetPosInert);

        final double angularRadius = FastMath.asin(radiusTarget / lineOfSightSC.getNorm());
        return fov.offsetFromBoundary(lineOfSightSC, angularRadius, trigger);

    }

}
