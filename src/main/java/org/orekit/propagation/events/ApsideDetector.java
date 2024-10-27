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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.utils.PVCoordinates;

/** Finder for apside crossing events.
 * <p>This class finds apside crossing events (i.e. apogee or perigee crossing).</p>
 * <p>The default implementation behavior is to {@link Action#CONTINUE continue}
 * propagation at apogee crossing and to {@link Action#STOP stop} propagation
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

    /** Build a new instance.
     * <p>The Keplerian period is used only to set an upper bound for the
     * max check interval to period/3 and to set the convergence threshold.</p>
     * @param keplerianPeriod estimate of the Keplerian period
     * @since 12.1
     */
    public ApsideDetector(final double keplerianPeriod) {
        super(keplerianPeriod / 3, 1e-13 * keplerianPeriod, DEFAULT_MAX_ITER, new StopOnIncreasing());
    }

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the
     * max check interval to period/3 and to set the convergence
     * threshold according to orbit size</p>
     * @param orbit initial orbit
     */
    public ApsideDetector(final Orbit orbit) {
        this(orbit.getKeplerianPeriod());
    }

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the
     * max check interval to period/3</p>
     * @param threshold convergence threshold (s)
     * @param orbit initial orbit
     */
    public ApsideDetector(final double threshold, final Orbit orbit) {
        super(orbit.getKeplerianPeriod() / 3, threshold, DEFAULT_MAX_ITER, new StopOnIncreasing());
    }

    /** Public constructor with full parameters.
     * <p>
     * This constructor is public because otherwise all accessible ones would require an orbit.
     * </p>
     * @param detectionSettings detection settings
     * @param handler event handler to call at event occurrences
     * @since 13.0
     */
    public ApsideDetector(final EventDetectionSettings detectionSettings, final EventHandler handler) {
        super(detectionSettings, handler);
    }

    /** {@inheritDoc} */
    @Override
    protected ApsideDetector create(final EventDetectionSettings detectionSettings, final EventHandler newHandler) {
        return new ApsideDetector(detectionSettings, newHandler);
    }

    /** Compute the value of the switching function.
     * This function computes the dot product of the 2 vectors : position.velocity.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    public double g(final SpacecraftState s) {
        final PVCoordinates pv = s.getPVCoordinates();
        return Vector3D.dotProduct(pv.getPosition(), pv.getVelocity());
    }

}
