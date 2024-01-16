/* Copyright 2002-2020 CS GROUP
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
import org.orekit.bodies.CelestialBodies;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnDecreasing;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Detects when two moving objects come close to each other, as seen from spacecraft.
 * <p>The main use case for this detector is when the primary object is in fact a ground
 * station, modeled as a {@link org.orekit.frames.TopocentricFrame} and when the secondary
 * is the {@link CelestialBodies#getSun() Sun}, for computing
 * optical reflections.</p>
 * <p>The default handler behavior is to {@link Action#STOP stop}
 * propagation when objects enter the proximity zone. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @author Thomas Paulet
 * @since 11.0
 */
public class AngularSeparationFromSatelliteDetector extends AbstractDetector<AngularSeparationFromSatelliteDetector> {

    /** Primary object, at the center of the proximity zone. */
    private final PVCoordinatesProvider primaryObject;

    /** Secondary object, that may come close to the primary, as seen from the spacecraft . */
    private final PVCoordinatesProvider secondaryObject;

    /** Proximity angle (rad). */
    private final double proximityAngle;

    /** Build a new angular detachment detector.
     * @param primaryObject primaryObject, at the center of the proximity zone
     * @param secondaryObject secondaryObject, that may come close to
     *        the primaryObject as seen from the spacecraft
     * @param proximityAngle proximity angle as seen from spacecraft, at which events are triggered (rad)
     */
    public AngularSeparationFromSatelliteDetector(final PVCoordinatesProvider primaryObject,
                                                  final PVCoordinatesProvider secondaryObject,
                                                  final double proximityAngle) {
        this(s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER, new StopOnDecreasing(),
             primaryObject, secondaryObject, proximityAngle);
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
     * @param primaryObject primaryObject at the center of the proximity zone
     * @param secondaryObject secondaryObject, that may come close to
     *        the primaryObject as seen from the spacecraft
     * @param proximityAngle proximity angle as seen from secondaryObject, at which events are triggered (rad)
     */
    protected AngularSeparationFromSatelliteDetector(final AdaptableInterval maxCheck, final double threshold,
                                                     final int maxIter,
                                                     final EventHandler handler,
                                                     final PVCoordinatesProvider primaryObject,
                                                     final PVCoordinatesProvider secondaryObject,
                                                     final double proximityAngle) {
        super(maxCheck, threshold, maxIter, handler);
        this.primaryObject         = primaryObject;
        this.secondaryObject       = secondaryObject;
        this.proximityAngle = proximityAngle;
    }

    /** {@inheritDoc} */
    @Override
    protected AngularSeparationFromSatelliteDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                                            final int newMaxIter, final EventHandler newHandler) {
        return new AngularSeparationFromSatelliteDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                             primaryObject, secondaryObject, proximityAngle);
    }

    /** Get the primaryObject, at the center of the proximity zone.
     * @return primaryObject
     */
    public PVCoordinatesProvider getPrimaryObject() {
        return primaryObject;
    }

    /** Get the secondaryObject.
     * @return secondaryObject
     */
    public PVCoordinatesProvider getSecondaryObject() {
        return secondaryObject;
    }

    /** Get the proximity angle (rad).
     * @return the proximity angle
     */
    public double getProximityAngle() {
        return proximityAngle;
    }

    /** Compute the value of the switching function.
     * <p>
     * This function measures the angular separation between primary and secondary objects
     * as seen from the spacecraft minus the proximity angle. It therefore triggers
     * decreasing events when the secondary object enters the proximity zone and increasing
     * events when it leaves the proximity zone.
     * </p>
     * <p>
     * No shadowing effect is taken into account, so this method is computed and
     * may trigger events even when the secondary object is behind the primary.
     * If such effects must be taken into account the
     * detector must be associated with a {@link EventEnablingPredicateFilter predicate
     * filter} where the {@link EnablingPredicate predicate function} is based on eclipse conditions.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    public double g(final SpacecraftState s) {
        final PVCoordinates sPV = s.getPVCoordinates();
        final Vector3D primaryPos   = primaryObject  .getPosition(s.getDate(), s.getFrame());
        final Vector3D secondaryPos = secondaryObject.getPosition(s.getDate(), s.getFrame());
        final double separation = Vector3D.angle(primaryPos.subtract(sPV.getPosition()),
                                                 secondaryPos.subtract(sPV.getPosition()));
        return separation - proximityAngle;
    }

}
