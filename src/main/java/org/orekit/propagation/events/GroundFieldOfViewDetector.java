/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.orekit.frames.Frame;
import org.orekit.geometry.fov.FieldOfView;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/**
 * Finder for satellite entry/exit events with respect to a sensor {@link
 * FieldOfView Field Of View} attached to an arbitrary frame.
 *
 * <p> If you only want to compute access times then you should probably use
 * {@link ElevationDetector}.
 *
 * <p>The default implementation behavior is to {@link Action#CONTINUE
 * continue} propagation at FOV entry and to {@link Action#STOP
 * stop} propagation at FOV exit. This can be changed by calling {@link
 * #withHandler(EventHandler)} after construction.</p>
 *
 * @author Luc Maisonobe
 * @author Evan Ward
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see FieldOfViewDetector
 * @see ElevationDetector
 * @since 7.1
 */
public class GroundFieldOfViewDetector extends AbstractDetector<GroundFieldOfViewDetector> {

    /** the reference frame attached to the sensor. */
    private final Frame frame;

    /** Field of view of the sensor. */
    private final FieldOfView fov;

    /**
     * Build a new instance.
     *
     * <p>The maximal interval between distance to FOV boundary checks should be
     * smaller than the half duration of the minimal pass to handle, otherwise
     * some short passes could be missed.</p>
     *
     * @param frame the reference frame attached to the sensor.
     * @param fov   Field Of View of the sensor.
     */
    public GroundFieldOfViewDetector(final Frame frame,
                                     final FieldOfView fov) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                new StopOnIncreasing<GroundFieldOfViewDetector>(),
                frame, fov);
    }

    /**
     * Private constructor with full parameters.
     *
     * <p> This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance in
     * a readable manner without using a huge amount of parameters. </p>
     *
     * @param maxCheck  maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter   maximum number of iterations in the event time search
     * @param handler   event handler to call at event occurrences
     * @param frame     the reference frame attached to the sensor.
     * @param fov       Field Of View of the sensor.
     */
    private GroundFieldOfViewDetector(final double maxCheck,
                                      final double threshold,
                                      final int maxIter,
                                      final EventHandler<? super GroundFieldOfViewDetector> handler,
                                      final Frame frame,
                                      final FieldOfView fov) {
        super(maxCheck, threshold, maxIter, handler);
        this.frame = frame;
        this.fov = fov;
    }

    /** {@inheritDoc} */
    @Override
    protected GroundFieldOfViewDetector create(final double newMaxCheck,
                                               final double newThreshold,
                                               final int newMaxIter,
                                               final EventHandler<? super GroundFieldOfViewDetector> newHandler) {
        return new GroundFieldOfViewDetector(newMaxCheck, newThreshold,
                newMaxIter, newHandler, this.frame, this.fov);
    }

    /**
     * Get the sensor reference frame.
     *
     * @return the reference frame attached to the sensor.
     */
    public Frame getFrame() {
        return this.frame;
    }

    /** Get the Field Of View.
     * @return Field Of View
     * @since 10.1
     */
    public FieldOfView getFOV() {
        return fov;
    }

    /** Get the Field Of View.
     * @return Field Of View, if detector has been built from a
     * {@link org.orekit.propagation.events.FieldOfView}, or null of the
     * detector was built from another implementation of {@link FieldOfView}
     * @deprecated as of 10.1, replaced by {@link #getFOV()}
     */
    @Deprecated
    public org.orekit.propagation.events.FieldOfView getFieldOfView() {
        return fov instanceof org.orekit.propagation.events.FieldOfView ?
               (org.orekit.propagation.events.FieldOfView) fov :
               null;
    }

    /**
     * {@inheritDoc}
     *
     * <p> The g function value is the angular offset between the satellite and
     * the {@link FieldOfView#offsetFromBoundary(Vector3D, double, VisibilityTrigger)
     * Field Of View boundary}. It is negative if the satellite is visible within
     * the Field Of View and positive if it is outside of the Field Of View,
     * including the margin. </p>
     *
     * <p> As per the previous definition, when the satellite enters the Field
     * Of View, a decreasing event is generated, and when the satellite leaves
     * the Field Of View, an increasing event is generated. </p>
     */
    public double g(final SpacecraftState s) {

        // get line of sight in sensor frame
        final Vector3D los = s.getPVCoordinates(this.frame).getPosition();
        return this.fov.offsetFromBoundary(los, 0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV);

    }

}
