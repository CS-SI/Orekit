/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.ode.events.Action;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnDecreasing;

/** Finder for satellite altitude crossing events.
 * <p>This class finds altitude events (i.e. satellite crossing
 * a predefined altitude level above ground).</p>
 * <p>The default implementation behavior is to {@link Action#CONTINUE
 * continue} propagation when ascending and to {@link Action#STOP stop}
 * propagation when descending. This can be changed by calling
 * {@link #withHandler(FieldEventHandler)} after construction.</p>
 * @see org.orekit.propagation.FieldPropagator#addEventDetector(FieldEventDetector)
 * @author Luc Maisonobe
 * @since 9.0
 * @param <T> type of the field elements
 */
public class FieldAltitudeDetector<T extends CalculusFieldElement<T>> extends FieldAbstractDetector<FieldAltitudeDetector<T>, T> {

    /** Threshold altitude value (m). */
    private final T altitude;

    /** Body shape with respect to which altitude should be evaluated. */
    private final BodyShape bodyShape;

    /** Build a new altitude detector.
     * <p>This simple constructor takes default values for maximal checking
     *  interval ({@link #DEFAULT_MAXCHECK}) and convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     * @param altitude threshold altitude value
     * @param bodyShape body shape with respect to which altitude should be evaluated
     */
    public FieldAltitudeDetector(final T altitude, final BodyShape bodyShape) {
        this(altitude.getField().getZero().newInstance(DEFAULT_MAXCHECK),
             altitude.getField().getZero().newInstance(DEFAULT_THRESHOLD),
             altitude, bodyShape);
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
    public FieldAltitudeDetector(final T maxCheck,
                                 final T altitude,
                                 final BodyShape bodyShape) {
        this(maxCheck, altitude.getField().getZero().newInstance(DEFAULT_THRESHOLD), altitude, bodyShape);
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
    public FieldAltitudeDetector(final T maxCheck,
                                 final T threshold,
                                 final T altitude,
                                 final BodyShape bodyShape) {
        this(FieldAdaptableInterval.of(maxCheck.getReal()), threshold, DEFAULT_MAX_ITER, new FieldStopOnDecreasing<T>(),
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
    protected FieldAltitudeDetector(final FieldAdaptableInterval<T> maxCheck, final T threshold,
                                    final int maxIter, final FieldEventHandler<T> handler,
                                    final T altitude,
                                    final BodyShape bodyShape) {
        super(new FieldEventDetectionSettings<>(maxCheck, threshold, maxIter), handler);
        this.altitude  = altitude;
        this.bodyShape = bodyShape;
    }

    /** {@inheritDoc} */
    @Override
    protected FieldAltitudeDetector<T> create(final FieldAdaptableInterval<T> newMaxCheck, final T newThreshold,
                                              final int newMaxIter,
                                              final FieldEventHandler<T> newHandler) {
        return new FieldAltitudeDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                           altitude, bodyShape);
    }

    /** Get the threshold altitude value.
     * @return the threshold altitude value (m)
     */
    public T getAltitude() {
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
    public T g(final FieldSpacecraftState<T> s) {
        final Frame bodyFrame              = bodyShape.getBodyFrame();
        final FieldGeodeticPoint<T> point  = bodyShape.transform(s.getPosition(bodyFrame),
                                                                 bodyFrame, s.getDate());
        return point.getAltitude().subtract(altitude);
    }

}
