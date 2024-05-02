/* Copyright 2002-2024 Joseph Reed
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Joseph Reed licenses this file to You under the Apache License, Version 2.0
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
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Finder for beta angle crossing events.
 * <p>Locate events when the beta angle (the angle between the orbit plane and the celestial body)
 * crosses a threshold. The {@link #g(SpacecraftState)} function is negative when the beta angle
 * is above the threshold and positive when the beta angle is below the threshold.</p>
 * <p>The inertial frame provided must have it's origin centered at the satellite's orbit plane. The
 * beta angle is computed as the angle between the celestial body's position in this frame with the
 * satellite's orbital momentum vector.</p>
 * <p>The default implementation behavior is to {@link Action#STOP stop}
 * propagation at the first event date occurrence. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Joe Reed
 * @since 12.1
 */
public class BetaAngleDetector extends AbstractDetector<BetaAngleDetector> {
    /** Beta angle crossing threshold. */
    private final double betaAngleThreshold;
    /** Coordinate provider for the celestial body. */
    private final PVCoordinatesProvider celestialBodyProvider;
    /** Inertial frame in which beta angle is calculated. */
    private final Frame inertialFrame;

    /**Solar beta angle constructor.
     * <p>This method uses the default data context, assigns the sun as the celestial
     * body and uses GCRF as the inertial frame.</p>
     * @param betaAngleThreshold beta angle threshold (radians)
     */
    @DefaultDataContext
    public BetaAngleDetector(final double betaAngleThreshold) {
        this(betaAngleThreshold, CelestialBodyFactory.getSun(), FramesFactory.getGCRF());
    }

    /** Class constructor.
     * @param betaAngleThreshold beta angle threshold (radians)
     * @param celestialBodyProvider coordinate provider for the celestial provider
     * @param inertialFrame inertial frame in which to compute the beta angle
     */
    public BetaAngleDetector(final double betaAngleThreshold, final PVCoordinatesProvider celestialBodyProvider,
            final Frame inertialFrame) {
        this(AdaptableInterval.of(DEFAULT_MAXCHECK), DEFAULT_THRESHOLD, DEFAULT_MAX_ITER, new StopOnEvent(),
                betaAngleThreshold, celestialBodyProvider, inertialFrame);
    }

    /** Protected constructor with full parameters.
     * <p>This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.</p>
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param betaAngleThreshold beta angle threshold (radians)
     * @param celestialBodyProvider coordinate provider for the celestial provider
     * @param inertialFrame inertial frame in which to compute the beta angle
     */
    protected BetaAngleDetector(final AdaptableInterval maxCheck, final double threshold,
                             final int maxIter, final EventHandler handler,
                             final double betaAngleThreshold, final PVCoordinatesProvider celestialBodyProvider,
                             final Frame inertialFrame) {
        super(maxCheck, threshold, maxIter, handler);
        this.betaAngleThreshold = betaAngleThreshold;
        this.celestialBodyProvider = celestialBodyProvider;
        this.inertialFrame = inertialFrame;
    }

    /** Coordinate provider for the celestial body.
     * @return celestial body's coordinate provider
     */
    public PVCoordinatesProvider getCelestialBodyProvider() {
        return this.celestialBodyProvider;
    }

    /** The inertial frame in which beta angle is computed.
     * @return the inertial frame
     */
    public Frame getInertialFrame() {
        return this.inertialFrame;
    }

    /** The beta angle threshold (radians).
     * @return the beta angle threshold (radians)
     */
    public double getBetaAngleThreshold() {
        return this.betaAngleThreshold;
    }

    /** Create a new instance with the provided coordinate provider.
     * <p>This method does not change the current instance.</p>
     * @param newProvider the new coordinate provider
     * @return the new detector instance
     */
    public BetaAngleDetector withCelestialProvider(final PVCoordinatesProvider newProvider) {
        return new BetaAngleDetector(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(),
                getHandler(), getBetaAngleThreshold(), newProvider, getInertialFrame());
    }

    /** Create a new instance with the provided beta angle threshold.
     * <p>This method does not change the current instance.</p>
     * @param newBetaAngleThreshold the beta angle threshold (radians)
     * @return the new detector instance
     */
    public BetaAngleDetector withBetaThreshold(final double newBetaAngleThreshold) {
        return new BetaAngleDetector(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(),
                getHandler(), newBetaAngleThreshold, getCelestialBodyProvider(), getInertialFrame());
    }

    /** Create a new instance with the provided inertial frame.
     * <p>This method does not change the current instance.</p>
     * @param newFrame the inertial frame
     * @return the new detector instance
     */
    public BetaAngleDetector withInertialFrame(final Frame newFrame) {
        return new BetaAngleDetector(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(),
                getHandler(), getBetaAngleThreshold(), getCelestialBodyProvider(), newFrame);
    }

    /** {@inheritDoc} */
    @Override
    public double g(final SpacecraftState s) {
        final double beta = calculateBetaAngle(s, celestialBodyProvider, inertialFrame);
        return betaAngleThreshold - beta;
    }

    /**Calculate the beta angle between the orbit plane and the celestial body.
     * <p>This method computes the beta angle using the frame from the spacecraft state.</p>
     * @param state spacecraft state
     * @param celestialBodyProvider celestial body coordinate provider
     * @return the beta angle (radians)
     */
    public static double calculateBetaAngle(final SpacecraftState state,
            final PVCoordinatesProvider celestialBodyProvider) {
        return calculateBetaAngle(state, celestialBodyProvider, state.getFrame());
    }

    /**Calculate the beta angle between the orbit plane and the celestial body.
     * @param state spacecraft state
     * @param celestialBodyProvider celestial body coordinate provider
     * @param frame inertial frame in which beta angle will be computed
     * @return the beta angle (radians)
     */
    public static double calculateBetaAngle(final SpacecraftState state,
            final PVCoordinatesProvider celestialBodyProvider, final Frame frame) {
        final Vector3D celestialP = celestialBodyProvider.getPosition(state.getDate(), frame);
        final TimeStampedPVCoordinates pv = state.getPVCoordinates(frame);
        return MathUtils.SEMI_PI - Vector3D.angle(celestialP, pv.getMomentum());
    }

    /** {@inheritDoc} */
    @Override
    protected BetaAngleDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
            final int newMaxIter, final EventHandler newHandler) {
        return new BetaAngleDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                getBetaAngleThreshold(), getCelestialBodyProvider(), getInertialFrame());
    }
}
