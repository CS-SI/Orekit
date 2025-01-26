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
package org.orekit.forces.radiation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Abstract class for the definition of the solar flux model with a single occulting body of spherical shape.
 *
 * @author Romain Serra
 * @see LightFluxModel
 * @since 12.2
 */
public abstract class AbstractSolarLightFluxModel extends AbstractLightFluxModel {

    /** Radius of central, occulting body (approximated as spherical).
     * Its center is assumed to be at the origin of the frame linked to the state. */
    private final double occultingBodyRadius;

    /** Reference flux normalized for a 1m distance (N). */
    private final double kRef;

    /** Eclipse detection settings. */
    private final EventDetectionSettings eventDetectionSettings;

    /**
     * Constructor.
     * @param kRef reference flux
     * @param occultedBody position provider for light source
     * @param occultingBodyRadius radius of central, occulting body
     * @param eventDetectionSettings user-defined detection settings for eclipses (if ill-tuned, events might be missed or performance might drop)
     */
    protected AbstractSolarLightFluxModel(final double kRef, final ExtendedPositionProvider occultedBody,
                                          final double occultingBodyRadius, final EventDetectionSettings eventDetectionSettings) {
        super(occultedBody);
        this.kRef = kRef;
        this.occultingBodyRadius = occultingBodyRadius;
        this.eventDetectionSettings = eventDetectionSettings;
    }

    /**
     * Constructor with default value for reference flux.
     * @param occultedBody position provider for light source
     * @param occultingBodyRadius radius of central, occulting body
     * @param eventDetectionSettings user-defined detection settings for eclipses (if ill-tuned, events might be missed or performance might drop)
     */
    protected AbstractSolarLightFluxModel(final ExtendedPositionProvider occultedBody, final double occultingBodyRadius,
                                          final EventDetectionSettings eventDetectionSettings) {
        this(4.56e-6 * FastMath.pow(149597870000.0, 2), occultedBody, occultingBodyRadius,
                eventDetectionSettings);
    }

    /**
     * Getter for occulting body radius.
     * @return radius
     */
    public double getOccultingBodyRadius() {
        return occultingBodyRadius;
    }

    /**
     * Getter for eclipse event detection settings used for eclipses.
     * @return event detection settings
     */
    public EventDetectionSettings getEventDetectionSettings() {
        return eventDetectionSettings;
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D getUnoccultedFluxVector(final Vector3D relativePosition) {
        final double squaredRadius = relativePosition.getNormSq();
        final double factor = kRef / (squaredRadius * FastMath.sqrt(squaredRadius));
        return relativePosition.scalarMultiply(factor);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getUnoccultedFluxVector(final FieldVector3D<T> relativePosition) {
        final T squaredRadius = relativePosition.getNormSq();
        final T factor = (squaredRadius.multiply(squaredRadius.sqrt())).reciprocal().multiply(kRef);
        return relativePosition.scalarMultiply(factor);
    }

}
