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

import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;


/** Detector for inter-satellites direct view (i.e. no masking by central body limb).
 * <p>
 * As this detector needs two satellites, it embeds one {@link
 * PVCoordinatesProvider coordinates provider} for the secondary satellite
 * and is registered as an event detector in the propagator of the primary
 * satellite. The secondary satellite provider will therefore be driven by this
 * detector (and hence by the propagator in which this detector is registered).
 * </p>
 * <p>
 * In order to avoid infinite recursion, care must be taken to have the secondary
 * satellite provider being <em>completely independent</em> from anything else.
 * In particular, if the provider is a propagator, it should <em>not</em> be run
 * together in a {@link PropagatorsParallelizer propagators parallelizer} with
 * the propagator this detector is registered in. It is fine however to configure
 * two separate propagators PsA and PsB with similar settings for the secondary satellite
 * and one propagator Pm for the primary satellite and then use Psa in this detector
 * registered within Pm while Pm and Psb are run in the context of a {@link
 * PropagatorsParallelizer propagators parallelizer}.
 * </p>
 * <p>
 * For efficiency reason during the event search loop, it is recommended to have
 * the secondary provider be an analytical propagator or an ephemeris. A numerical propagator
 * as a secondary propagator works but is expected to be computationally costly.
 * </p>
 * <p>
 * The {@code g} function of this detector is positive when satellites can see
 * each other directly and negative when the central body limb is in between and
 * blocks the direct view.
 * </p>
 * <p>
 * This detector only checks masking by central body limb, it does not take into
 * account satellites antenna patterns. If these patterns must be considered, then
 * this detector can be {@link BooleanDetector#andCombine(EventDetector...) and combined}
 * with  the {@link BooleanDetector#notCombine(EventDetector) logical not} of
 * {@link FieldOfViewDetector field of view detectors}.
 * </p>
 * @author Luc Maisonobe
 * @since 9.3
 */
public class InterSatDirectViewDetector extends AbstractDetector<InterSatDirectViewDetector> {

    /** Central body. */
    private final OneAxisEllipsoid body;

    /** Skimming altitude.
     * @since 12.0
     */
    private final double skimmingAltitude;

    /** Coordinates provider for the secondary satellite. */
    private final PVCoordinatesProvider secondary;

    /** simple constructor.
     *
     * @param body central body
     * @param secondary provider for the secondary satellite
     */
    public InterSatDirectViewDetector(final OneAxisEllipsoid body, final PVCoordinatesProvider secondary) {
        this(body, 0.0, secondary, s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
             new ContinueOnEvent());
    }

    /** Private constructor.
     * @param body central body
     * @param skimmingAltitude skimming altitude at which events are triggered
     * @param secondary provider for the secondary satellite
     * @param maxCheck  maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter   maximum number of iterations in the event time search
     * @param handler   event handler to call at event occurrences
     * @since 12.0
     */
    protected InterSatDirectViewDetector(final OneAxisEllipsoid body,
                                         final double skimmingAltitude,
                                         final PVCoordinatesProvider secondary,
                                         final AdaptableInterval maxCheck,
                                         final double threshold,
                                         final int maxIter,
                                         final EventHandler handler) {
        super(maxCheck, threshold, maxIter, handler);
        this.body             = body;
        this.skimmingAltitude = skimmingAltitude;
        this.secondary        = secondary;
    }

    /** Get the central body.
     * @return central body
     */
    public OneAxisEllipsoid getCentralBody() {
        return body;
    }

    /** Get the skimming altitude.
     * @return skimming altitude at which events are triggered
     * @since 12.0
     */
    public double getSkimmingAltitude() {
        return skimmingAltitude;
    }

    /** Get the provider for the secondary satellite.
     * @return provider for the secondary satellite
     */
    public PVCoordinatesProvider getSecondary() {
        return secondary;
    }

    /** {@inheritDoc} */
    @Override
    protected InterSatDirectViewDetector create(final AdaptableInterval newMaxCheck,
                                                final double newThreshold,
                                                final int newMaxIter,
                                                final EventHandler newHandler) {
        return new InterSatDirectViewDetector(body, skimmingAltitude, secondary,
                                              newMaxCheck, newThreshold, newMaxIter, newHandler);
    }

    /**
     * Setup the skimming altitude.
     * <p>
     * The skimming altitude is the lowest altitude of the path between satellites
     * at which events should be triggered. If set to 0.0, events are triggered
     * exactly when the path passes just at central body limb.
     * </p>
     * @param newSkimmingAltitude skimming altitude (m)
     * @return a new detector with updated configuration (the instance is not changed)
     * @see #getSkimmingAltitude()
     * @since 12.0
     */
    public InterSatDirectViewDetector withSkimmingAltitude(final double newSkimmingAltitude) {
        return new InterSatDirectViewDetector(body, newSkimmingAltitude, secondary,
                                              getMaxCheckInterval(), getThreshold(),
                                              getMaxIterationCount(), getHandler());
    }

    /** {@inheritDoc}
     * <p>
     * The {@code g} function of this detector is the difference between the minimum
     * altitude of intermediate points along the line of sight between satellites and the
     * {@link #getSkimmingAltitude() skimming altitude}. It is therefore positive when
     * all intermediate points are above the skimming altitude, meaning satellites can see
     * each other and it is negative when some intermediate points (which may be either
     * endpoints) dive below this altitude, meaning satellites cannot see each other.
     * </p>
     */
    @Override
    public double g(final SpacecraftState state) {

        // get the lowest point between primary and secondary
        final AbsoluteDate  date   = state.getDate();
        final Frame         frame  = body.getBodyFrame();
        final GeodeticPoint lowest = body.lowestAltitudeIntermediate(state.getPosition(frame),
                                                                     secondary.getPosition(date, frame));

        // compute switching function value as altitude difference
        return lowest.getAltitude() - skimmingAltitude;

    }

}
