/* Copyright 2022-2024 Romain Serra
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
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Detector of specific value for the distance relative to another trajectory (using the Euclidean norm).
 * <p>
 * The default implementation behavior is to {@link Action#STOP stop} propagation.
 * This can be changed by calling {@link #withHandler(EventHandler)} after construction.
 * </p>
 * <p>
 * As this detector needs two objects (moving relative to each other), it embeds one
 * {@link PVCoordinatesProvider coordinates provider} for the secondary object and is registered as an event detector in
 * the propagator of the primary object. The secondary object {@link PVCoordinatesProvider coordinates provider} will
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
 *
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Romain Serra
 * @since 12.1
 */
public class RelativeDistanceDetector extends AbstractDetector<RelativeDistanceDetector> {

    /**
     * PVCoordinates provider of the other object used to define relative distance.
     */
    private final PVCoordinatesProvider secondaryPVProvider;

    /** Relative distance value triggering detection. */
    private final double distanceThreshold;

    /**
     * Constructor with default values.
     * <p>
     * By default, the implemented behavior is to {@link Action#STOP stop} propagation at detection.
     * </p>
     *
     * @param secondaryPVProvider PVCoordinates provider of the other object defining relative distance.
     * @param distanceThreshold Relative distance threshold for event detection
     */
    public RelativeDistanceDetector(final PVCoordinatesProvider secondaryPVProvider,
                                    final double distanceThreshold) {
        this(EventDetectionSettings.getDefaultEventDetectionSettings(), new StopOnEvent(), secondaryPVProvider,
                distanceThreshold);
    }

    /**
     * Constructor.
     * <p>
     * This constructor is to be used if the user wants to change the default behavior of the detector.
     * </p>
     *
     * @param detectionSettings   Detection settings
     * @param handler             Event handler to call at event occurrences.
     * @param secondaryPVProvider PVCoordinates provider of the other object defining relative distance.
     * @param distanceThreshold Relative distance threshold for event detection
     * @see EventHandler
     * @since 12.2
     */
    protected RelativeDistanceDetector(final EventDetectionSettings detectionSettings,
                                       final EventHandler handler, final PVCoordinatesProvider secondaryPVProvider,
                                       final double distanceThreshold) {
        super(detectionSettings, handler);
        this.secondaryPVProvider = secondaryPVProvider;
        this.distanceThreshold = distanceThreshold;
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
     * @param secondaryPVProvider PVCoordinates provider of the other object defining relative distance.
     * @param distanceThreshold Relative distance threshold for event detection
     * @see EventHandler
     * @deprecated as of 12.2
     */
    @Deprecated
    protected RelativeDistanceDetector(final AdaptableInterval maxCheck, final double threshold, final int maxIter,
                                       final EventHandler handler, final PVCoordinatesProvider secondaryPVProvider,
                                       final double distanceThreshold) {
        this(new EventDetectionSettings(maxCheck, threshold, maxIter), handler, secondaryPVProvider, distanceThreshold);
    }

    /**
     * The {@code g} is positive when the relative distance is larger or equal than the threshold,
     * non-positive otherwise.
     *
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    public double g(final SpacecraftState s) {
        final Vector3D secondaryPosition = getSecondaryPVProvider().getPosition(s.getDate(), s.getFrame());
        final double relativeDistance = s.getPosition().subtract(secondaryPosition).getNorm();
        return relativeDistance - distanceThreshold;
    }

    /** {@inheritDoc} */
    @Override
    protected RelativeDistanceDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                              final int newMaxIter, final EventHandler newHandler) {
        return new RelativeDistanceDetector(newMaxCheck, newThreshold, newMaxIter, newHandler, secondaryPVProvider,
                distanceThreshold);
    }

    /**
     * Get the secondary position-velocity provider stored in this instance.
     *
     * @return the secondary position-velocity provider stored in this instance
     */
    public PVCoordinatesProvider getSecondaryPVProvider() {
        return secondaryPVProvider;
    }

    /**
     * Get the relative distance threshold.
     *
     * @return threshold triggering detection
     */
    public double getDistanceThreshold() {
        return distanceThreshold;
    }
}
