/* Copyright 2002-2022 CS GROUP
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
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Finder for extremum approach events.
 * <p>This class finds extremum approach events (i.e. closest or farthest approach).</p>
 * <p>The default implementation behavior is to {@link Action#CONTINUE continue}
 * propagation at farthest approach and to {@link Action#STOP stop} propagation at closest approach. This can be changed
 * by calling {@link #withHandler(EventHandler)} after construction.</p>
 * <p>It is also possible to detect solely one type of event using an {@link EventSlopeFilter event slope filter}. For
 * example in order to only detect closest approach, one should type the following :
 * <pre>{@code
 * ExtremumApproachDetector extremumApproachDetector = new ExtremumApproachDetector(otherPVProvider);
 * EventDetector closeApproachDetector = new EventSlopeFilter<ExtremumApproachDetector>(extremumApproachDetector,FilterType.TRIGGER_ONLY_INCREASING_EVENTS);
 *  } </pre>
 * </p>
 *
 * @author Vincent Cucchietti
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see EventSlopeFilter
 * @see FilterType
 */
public class ExtremumApproachDetector extends AbstractDetector<ExtremumApproachDetector> {

    /**
     * PVCoordinates provider of the other object with which we want to find out the extremum approach.
     */
    private final PVCoordinatesProvider otherPVProvider;

    /**
     * Constructor with default values.
     *
     * @param otherPVProvider PVCoordinates provider of the other object with which we want to find out the extremum
     *                        approach.
     */
    public ExtremumApproachDetector(final PVCoordinatesProvider otherPVProvider) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER, new StopOnIncreasing<>(), otherPVProvider);
    }

    /**
     * Constructor.
     *
     * @param maxCheck        Maximum checking interval (s).
     * @param threshold       Convergence threshold (s).
     * @param maxIter         Maximum number of iterations in the event time search.
     * @param handler         Event handler to call at event occurrences.
     * @param otherPVProvider PVCoordinates provider of the other object with which we want to find out the extremum *
     *                        approach.
     */
    public ExtremumApproachDetector(
            final double maxCheck, final double threshold, final int maxIter,
            final EventHandler<? super ExtremumApproachDetector> handler, final PVCoordinatesProvider otherPVProvider) {
        super(maxCheck, threshold, maxIter, handler);
        this.otherPVProvider = otherPVProvider;
    }

    /** {@inheritDoc} */
    public double g(final SpacecraftState s) {
        final PVCoordinates deltaPV = deltaPV(s);
        return Vector3D.dotProduct(deltaPV.getPosition(), deltaPV.getVelocity());
    }

    /**
     * @param s Spacecraft state.
     * @return Relative position between s and otherPVProvider.
     */
    protected PVCoordinates deltaPV(final SpacecraftState s) {
        return new PVCoordinates(s.getPVCoordinates(),
                otherPVProvider.getPVCoordinates(s.getDate(), s.getFrame()));
    }

    /** {@inheritDoc} */
    @Override
    protected ExtremumApproachDetector create(final double newMaxCheck, final double newThreshold, final int newMaxIter,
                                              final EventHandler<? super ExtremumApproachDetector> newHandler) {
        return new ExtremumApproachDetector(newMaxCheck, newThreshold, newMaxIter, newHandler, otherPVProvider);
    }
}
