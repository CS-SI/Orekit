/* Copyright 2022-2025 Romain Serra
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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.utils.FieldPVCoordinatesProvider;

/**
 * Detector of specific value for the distance relative to another trajectory (using the Euclidean norm).
 * <p>
 * The default implementation behavior is to {@link Action#STOP stop} propagation.
 * This can be changed by calling {@link #withHandler(org.orekit.propagation.events.handlers.FieldEventHandler)} after construction.
 * </p>
 * <p>
 * As this detector needs two objects (moving relative to each other), it embeds one
 * {@link org.orekit.utils.FieldPVCoordinatesProvider coordinates provider} for the secondary object and is registered as an event detector in
 * the propagator of the primary object. The secondary object {@link org.orekit.utils.FieldPVCoordinatesProvider coordinates provider} will
 * therefore be driven by this detector (and hence by the propagator in which this detector is registered).
 * </p>
 * <p>
 * For efficiency reason during the event search loop, it is recommended to have the secondary provider be an analytical
 * propagator or an ephemeris. A numerical propagator as a secondary propagator works but is expected to be
 * computationally costly.
 * </p>
 *
 * @see org.orekit.propagation.FieldPropagator#addEventDetector(FieldEventDetector)
 * @author Romain Serra
 * @since 12.1
 */
public class FieldRelativeDistanceDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldRelativeDistanceDetector<T>, T> {

    /**
     * PVCoordinates provider of the other object used to define relative distance.
     */
    private final FieldPVCoordinatesProvider<T> secondaryPVProvider;

    /** Relative distance value triggering detection. */
    private final T distanceThreshold;

    /**
     * Constructor with default values.
     * <p>
     * By default, the implemented behavior is to {@link Action#STOP stop} propagation at detection.
     * </p>
     *
     * @param secondaryPVProvider PVCoordinates provider of the other object defining relative distance.
     * @param distanceThreshold Relative distance threshold for event detection
     */
    public FieldRelativeDistanceDetector(final FieldPVCoordinatesProvider<T> secondaryPVProvider,
                                         final T distanceThreshold) {
        this(new FieldEventDetectionSettings<>(distanceThreshold.getField(), EventDetectionSettings.getDefaultEventDetectionSettings()),
                new FieldStopOnEvent<>(), secondaryPVProvider, distanceThreshold);
    }

    /**
     * Constructor.
     * <p>
     * This constructor is to be used if the user wants to change the default behavior of the detector.
     * </p>
     *
     * @param detectionSettings   Detection settings.
     * @param handler             Event handler to call at event occurrences.
     * @param secondaryPVProvider PVCoordinates provider of the other object defining relative distance.
     * @param distanceThreshold Relative distance threshold for event detection
     * @see FieldEventHandler
     * @since 12.2
     */
    protected FieldRelativeDistanceDetector(final FieldEventDetectionSettings<T> detectionSettings,
                                            final FieldEventHandler<T> handler, final FieldPVCoordinatesProvider<T> secondaryPVProvider,
                                            final T distanceThreshold) {
        super(detectionSettings, handler);
        this.secondaryPVProvider = secondaryPVProvider;
        this.distanceThreshold = distanceThreshold;
    }

    /**
     * The {@code g} is positive when the relative distance is larger or equal than the threshold,
     * non-positive otherwise.
     *
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    @Override
    public T g(final FieldSpacecraftState<T> s) {
        final FieldVector3D<T> secondaryPosition = getSecondaryPVProvider().getPosition(s.getDate(), s.getFrame());
        final T relativeDistance = s.getPosition().subtract(secondaryPosition).getNorm();
        return relativeDistance.subtract(distanceThreshold);
    }

    /** {@inheritDoc} */
    @Override
    protected FieldRelativeDistanceDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                                      final FieldEventHandler<T> newHandler) {
        return new FieldRelativeDistanceDetector<>(detectionSettings, newHandler, secondaryPVProvider,
                distanceThreshold);
    }

    /**
     * Get the secondary position-velocity provider stored in this instance.
     *
     * @return the secondary position-velocity provider stored in this instance
     */
    public FieldPVCoordinatesProvider<T> getSecondaryPVProvider() {
        return secondaryPVProvider;
    }

    /**
     * Get the relative distance threshold.
     *
     * @return threshold triggering detection
     */
    public T getDistanceThreshold() {
        return distanceThreshold;
    }
}
