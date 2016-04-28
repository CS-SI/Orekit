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
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.utils.PVCoordinates;

/** Finder for apside crossing events.
 * <p>This class finds apside crossing events (i.e. apogee or perigee crossing).</p>
 * <p>The default implementation behavior is to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#CONTINUE continue}
 * propagation at apogee crossing and to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#STOP stop} propagation
 * at perigee crossing. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * <p>Beware that apside detection will fail for almost circular orbits. If
 * for example an apside detector is used to trigger an {@link
 * org.orekit.forces.maneuvers.ImpulseManeuver ImpulseManeuver} and the maneuver
 * change the orbit shape to circular, then the detector may completely fail just
 * after the maneuver has been performed!</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public class ApsideDetector extends AbstractDetector<ApsideDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131118L;

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the
     * max check interval to period/3 and to set the convergence
     * threshold according to orbit size</p>
     * @param orbit initial orbit
     */
    public ApsideDetector(final Orbit orbit) {
        this(1.0e-13 * orbit.getKeplerianPeriod(), orbit);
    }

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the
     * max check interval to period/3</p>
     * @param threshold convergence threshold (s)
     * @param orbit initial orbit
     */
    public ApsideDetector(final double threshold, final Orbit orbit) {
        super(orbit.getKeplerianPeriod() / 3, threshold,
              DEFAULT_MAX_ITER, new StopOnIncreasing<ApsideDetector>());
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
     * @since 6.1
     */
    private ApsideDetector(final double maxCheck, final double threshold,
                           final int maxIter, final EventHandler<? super ApsideDetector> handler) {
        super(maxCheck, threshold, maxIter, handler);
    }

    /** {@inheritDoc} */
    @Override
    protected ApsideDetector create(final double newMaxCheck, final double newThreshold,
                                    final int newMaxIter, final EventHandler<? super ApsideDetector> newHandler) {
        return new ApsideDetector(newMaxCheck, newThreshold, newMaxIter, newHandler);
    }

    /** Compute the value of the switching function.
     * This function computes the dot product of the 2 vectors : position.velocity.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        final PVCoordinates pv = s.getPVCoordinates();
        return Vector3D.dotProduct(pv.getPosition(), pv.getVelocity());
    }

}
