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
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Finder for extremum approach events.
 * <p>
 * This class finds extremum approach events (i.e. closest or farthest approach).
 * </p>
 * <p>
 * The default implementation behavior is to {@link Action#CONTINUE continue} propagation at farthest approach and to
 * {@link Action#STOP stop} propagation at closest approach. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction (go to the end of the documentation to see an example).
 * </p>
 * <p>
 * As this detector needs two objects (moving relative to each other), it embeds one
 * {@link PVCoordinatesProvider coordinates provider} for the secondary object and is registered as an event detector in
 * the propagator of the primary object. The secondary object  {@link PVCoordinatesProvider coordinates provider} will
 * therefore be driven by this detector (and hence by the propagator in which this detector is registered).
 * </p>
 * <p><b>
 * In order to avoid infinite recursion, care must be taken to have the secondary object provider being <em>completely
 * independent</em> from anything else. In particular, if the provider is a propagator, it should <em>not</em> be run
 * together in a {@link PropagatorsParallelizer propagators parallelizer} with the propagator this detector is
 * registered in. It is fine however to configure two separate propagators PsA and PsB with similar settings for the
 * secondary object and one propagator Pm for the primary object and then use Psa in this detector registered within Pm
 * while Pm and Psb are run in the context of a {@link PropagatorsParallelizer propagators parallelizer}.
 * </b></p>
 * <p>
 * For efficiency reason during the event search loop, it is recommended to have the secondary provider be an analytical
 * propagator or an ephemeris. A numerical propagator as a secondary propagator works but is expected to be
 * computationally costly.
 * </p>
 * <p>
 * Also, it is possible to detect solely one type of event using an {@link EventSlopeFilter event slope filter}. For
 * example in order to only detect closest approach, one should type the following :
 * </p>
 * <pre>{@code
 * ExtremumApproachDetector extremumApproachDetector = new ExtremumApproachDetector(secondaryPVProvider);
 * EventDetector closeApproachDetector = new EventSlopeFilter<ExtremumApproachDetector>(extremumApproachDetector,FilterType.TRIGGER_ONLY_INCREASING_EVENTS);
 *  }
 * </pre>
 *
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see EventSlopeFilter
 * @see FilterType
 * @author Vincent Cucchietti
 * @since 11.3
 */
public class ExtremumApproachDetector extends AbstractDetector<ExtremumApproachDetector> {

    /**
     * PVCoordinates provider of the other object with which we want to find out the extremum approach.
     */
    private final PVCoordinatesProvider secondaryPVProvider;

    /**
     * Constructor with default values.
     * <p>
     * By default, the implemented behavior is to {@link Action#CONTINUE continue} propagation at farthest approach and
     * to {@link Action#STOP stop} propagation at closest approach.
     * </p>
     *
     * @param secondaryPVProvider PVCoordinates provider of the other object with which we want to find out the extremum
     *                            approach.
     */
    public ExtremumApproachDetector(final PVCoordinatesProvider secondaryPVProvider) {
        this(s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER, new StopOnIncreasing(), secondaryPVProvider);
    }

    /**
     * Constructor.
     * <p>
     * This constructor is to be used if the user wants to change the default behavior of the detector.
     * </p>
     *
     * @param maxCheck            Maximum checking interval.
     * @param threshold           Convergence threshold (s).
     * @param maxIter             Maximum number of iterations in the event time search.
     * @param handler             Event handler to call at event occurrences.
     * @param secondaryPVProvider PVCoordinates provider of the other object with which we want to find out the extremum
     *                            approach.
     * @see EventHandler
     */
    protected ExtremumApproachDetector(final AdaptableInterval maxCheck, final double threshold, final int maxIter,
                                       final EventHandler handler, final PVCoordinatesProvider secondaryPVProvider) {
        super(maxCheck, threshold, maxIter, handler);
        this.secondaryPVProvider = secondaryPVProvider;
    }

    /**
     * The {@code g} is positive when the primary object is getting further away from the secondary object and is
     * negative when it is getting closer to it.
     *
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    public double g(final SpacecraftState s) {
        final PVCoordinates deltaPV = computeDeltaPV(s);
        return Vector3D.dotProduct(deltaPV.getPosition(), deltaPV.getVelocity());
    }

    /**
     * Compute the relative PV between primary and secondary objects.
     *
     * @param s Spacecraft state.
     * @return Relative position between primary (=s) and secondaryPVProvider.
     */
    public PVCoordinates computeDeltaPV(final SpacecraftState s) {
        return new PVCoordinates(s.getPVCoordinates(),
                                 secondaryPVProvider.getPVCoordinates(s.getDate(), s.getFrame()));
    }

    /** {@inheritDoc} */
    @Override
    protected ExtremumApproachDetector create(final AdaptableInterval newMaxCheck, final double newThreshold, final int newMaxIter,
                                              final EventHandler newHandler) {
        return new ExtremumApproachDetector(newMaxCheck, newThreshold, newMaxIter, newHandler, secondaryPVProvider);
    }

    /**
     * Get the secondary position-velocity provider stored in this instance.
     *
     * @return the secondary position-velocity provider stored in this instance
     */
    public PVCoordinatesProvider getSecondaryPVProvider() {
        return secondaryPVProvider;
    }
}
