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

import org.hipparchus.ode.events.Action;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnDecreasing;

/** Finder for satellite altitude crossing events.
 * <p>This class finds altitude events (i.e. satellite crossing
 * a predefined altitude level above ground).</p>
 * <p>The default implementation behavior is to {@link Action#CONTINUE
 * continue} propagation when ascending and to {@link Action#STOP stop}
 * propagation when descending. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public class AltitudeDetector extends AbstractDetector<AltitudeDetector> {

    /** Threshold altitude value (m). */
    private final double altitude;

    /** Body shape with respect to which altitude should be evaluated. */
    private final BodyShape bodyShape;

    /** Build a new altitude detector.
     * <p>This simple constructor takes default values for maximal checking
     *  interval ({@link #DEFAULT_MAXCHECK}) and convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     * @param altitude threshold altitude value
     * @param bodyShape body shape with respect to which altitude should be evaluated
     */
    public AltitudeDetector(final double altitude, final BodyShape bodyShape) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, altitude, bodyShape);
    }

    /** Build a new altitude detector.
     * <p>This simple constructor takes default value for convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     * <p>The maximal interval between altitude checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param altitude threshold altitude value (m)
     * @param bodyShape body shape with respect to which altitude should be evaluated
     */
    public AltitudeDetector(final double maxCheck,
                            final double altitude,
                            final BodyShape bodyShape) {
        this(maxCheck, DEFAULT_THRESHOLD, altitude, bodyShape);
    }

    /** Build a new altitude detector.
     * <p>The maximal interval between altitude checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * <p>The maximal interval between altitude checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param altitude threshold altitude value (m)
     * @param bodyShape body shape with respect to which altitude should be evaluated
     */
    public AltitudeDetector(final double maxCheck,
                            final double threshold,
                            final double altitude,
                            final BodyShape bodyShape) {
        this(s -> maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnDecreasing(),
             altitude, bodyShape);
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
     * @param altitude threshold altitude value (m)
     * @param bodyShape body shape with respect to which altitude should be evaluated
     * @since 6.1
     */
    protected AltitudeDetector(final AdaptableInterval maxCheck, final double threshold,
                               final int maxIter, final EventHandler handler,
                               final double altitude,
                               final BodyShape bodyShape) {
        super(maxCheck, threshold, maxIter, handler);
        this.altitude  = altitude;
        this.bodyShape = bodyShape;
    }

    /** {@inheritDoc} */
    @Override
    protected AltitudeDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                      final int newMaxIter, final EventHandler newHandler) {
        return new AltitudeDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                    altitude, bodyShape);
    }

    /** Get the threshold altitude value.
     * @return the threshold altitude value (m)
     */
    public double getAltitude() {
        return altitude;
    }

    /** Get the body shape.
     * @return the body shape
     */
    public BodyShape getBodyShape() {
        return bodyShape;
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the current altitude
     * and the threshold altitude.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    public double g(final SpacecraftState s) {
        final Frame bodyFrame      = bodyShape.getBodyFrame();
        final GeodeticPoint point  = bodyShape.transform(s.getPosition(bodyFrame), bodyFrame, s.getDate());
        return point.getAltitude() - altitude;
    }

}
