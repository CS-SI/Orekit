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
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Detects when spacecraft comes close to a moving beacon, as seen from a moving observer.
 * <p>The main use case for this detector is when the observer is in fact a ground
 * station, modeled as a {@link org.orekit.frames.TopocentricFrame} and when the beacon
 * is the {@link org.orekit.bodies.CelestialBodyFactory#getSun() Sun}, for computing
 * interferences for the telemetry link. Another similar case is when the beacon is
 * another spacecraft, for interferences computation.</p>
 * <p>The default handler behavior is to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#STOP stop}
 * propagation when spacecraft enters the proximity zone. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @since 8.0
 */
public class AngularSeparationDetector extends AbstractDetector<AngularSeparationDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20160519L;

    /** Beacon at the center of the proximity zone. */
    private final PVCoordinatesProvider beacon;

    /** Observer for the spacecraft, that may also see the beacon at the same time if they are too close. */
    private final PVCoordinatesProvider observer;

    /** Proximity angle (rad). */
    private final double proximityAngle;

    /** Build a new angular separation detector.
     * @param beacon beacon at the center of the proximity zone
     * @param observer observer for the spacecraft, that may also see
     * the beacon at the same time if they are too close to each other
     * @param proximityAngle proximity angle as seen from observer, at which events are triggered (rad)
     */
    public AngularSeparationDetector(final PVCoordinatesProvider beacon,
                                     final PVCoordinatesProvider observer,
                                     final double proximityAngle) {
        this(60.0, 1.0e-3, 100, new StopOnDecreasing<AngularSeparationDetector>(),
             beacon, observer, proximityAngle);
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
     * @param beacon beacon at the center of the proximity zone
     * @param observer observer for the spacecraft, that may also see
     * the beacon at the same time if they are too close to each other
     * @param proximityAngle proximity angle as seen from observer, at which events are triggered (rad)
     */
    private AngularSeparationDetector(final double maxCheck, final double threshold,
                                      final int maxIter,
                                      final EventHandler<? super AngularSeparationDetector> handler,
                                      final PVCoordinatesProvider beacon,
                                      final PVCoordinatesProvider observer,
                                      final double proximityAngle) {
        super(maxCheck, threshold, maxIter, handler);
        this.beacon         = beacon;
        this.observer       = observer;
        this.proximityAngle = proximityAngle;
    }

    /** {@inheritDoc} */
    @Override
    protected AngularSeparationDetector create(final double newMaxCheck, final double newThreshold,
                                       final int newMaxIter, final EventHandler<? super AngularSeparationDetector> newHandler) {
        return new AngularSeparationDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                             beacon, observer, proximityAngle);
    }

    /** Get the beacon at the center of the proximity zone.
     * @return beacon at the center of the proximity zone
     */
    public PVCoordinatesProvider getBeacon() {
        return beacon;
    }

    /** Get the observer for the spacecraft.
     * @return observer for the spacecraft
     */
    public PVCoordinatesProvider getObserver() {
        return observer;
    }

    /** Get the proximity angle (rad).
     * @return the proximity angle
     */
    public double getProximityAngle() {
        return proximityAngle;
    }

    /** Compute the value of the switching function.
     * <p>
     * This function measures the angular separation between beacon and spacecraft
     * as seen from the observer minus the proximity angle. It therefore triggers
     * decreasing events when the spacecraft enters the proximity zone and increasing
     * events when it leaves the proximity zone.
     * </p>
     * <p>
     * No shadowing effect is taken into account, so this method is computed and
     * may trigger events even when the spacecraft is below horizon for an observer
     * which is a ground station. If such effects must be taken into account the
     * detector must be associated with a {@link EventEnablingPredicateFilter predicate
     * filter} where the {@link EnablingPredicate predicate function} is based on elevation.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        final PVCoordinates sPV = s.getPVCoordinates();
        final PVCoordinates bPV = beacon.getPVCoordinates(s.getDate(), s.getFrame());
        final PVCoordinates oPV = observer.getPVCoordinates(s.getDate(), s.getFrame());
        final double separation = Vector3D.angle(sPV.getPosition().subtract(oPV.getPosition()),
                                                 bPV.getPosition().subtract(oPV.getPosition()));
        return separation - proximityAngle;
    }

}
