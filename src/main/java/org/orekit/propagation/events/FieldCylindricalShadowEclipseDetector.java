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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Event detector for eclipses from a single, infinitely-distant light source, occulted by a spherical central body.
 * The shadow region is cylindrical, a model less accurate than a conical one but more computationally-performant.
 * <p>
 *     The so-called g function is negative in eclipse, positive otherwise.
 * </p>
 * @author Romain Serra
 * @see FieldEclipseDetector
 * @see CylindricalShadowEclipseDetector
 * @since 12.1
 */
public class FieldCylindricalShadowEclipseDetector<T extends CalculusFieldElement<T>>
    extends FieldAbstractDetector<FieldCylindricalShadowEclipseDetector<T>, T> {

    /** Direction provider for the occulted light source i.e. the Sun (whose shadow is approximated as if the body was infinitely distant). */
    private final ExtendedPositionProvider sun;

    /** Radius of central, occulting body (approximated as spherical).
     * Its center is assumed to be at the origin of the frame linked to the state. */
    private final T occultingBodyRadius;

    /**
     * Constructor.
     * @param sun light source provider (infinitely distant)
     * @param occultingBodyRadius occulting body radius
     * @param maxCheck maximum check for event detection
     * @param threshold threshold for event detection
     * @param maxIter maximum iteration for event detection
     * @param handler event handler
     */
    public FieldCylindricalShadowEclipseDetector(final ExtendedPositionProvider sun,
                                                 final T occultingBodyRadius,
                                                 final FieldAdaptableInterval<T> maxCheck, final T threshold,
                                                 final int maxIter, final FieldEventHandler<T> handler) {
        super(maxCheck, threshold, maxIter, handler);
        this.sun = sun;
        this.occultingBodyRadius = FastMath.abs(occultingBodyRadius);
    }

    /**
     * Constructor with default detection settings.
     * @param sun light source provider
     * @param occultingBodyRadius occulting body radius
     * @param handler event handler
     */
    public FieldCylindricalShadowEclipseDetector(final ExtendedPositionProvider sun,
                                                 final T occultingBodyRadius, final FieldEventHandler<T> handler) {
        this(sun, occultingBodyRadius, FieldAdaptableInterval.of(DEFAULT_MAXCHECK), occultingBodyRadius.getField().getZero().newInstance(DEFAULT_THRESHOLD),
            DEFAULT_MAX_ITER, handler);
    }

    /**
     * Getter for occulting body radius.
     * @return radius
     */
    public T getOccultingBodyRadius() {
        return occultingBodyRadius;
    }

    /** {@inheritDoc} */
    @Override
    public T g(final FieldSpacecraftState<T> s) {
        final FieldVector3D<T> sunDirection = sun.getPosition(s.getDate(), s.getFrame()).normalize();
        final FieldVector3D<T> position = s.getPosition();
        final T dotProduct = position.dotProduct(sunDirection);
        if (dotProduct.getReal() >= 0.) {
            return position.getNorm().divide(occultingBodyRadius);
        } else {
            final T distanceToCylinderAxis = (position.subtract(sunDirection.scalarMultiply(dotProduct))).getNorm();
            return distanceToCylinderAxis.divide(occultingBodyRadius).subtract(1.);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected FieldCylindricalShadowEclipseDetector<T> create(final FieldAdaptableInterval<T> newMaxCheck, final T newThreshold,
                                                              final int newMaxIter, final FieldEventHandler<T> newHandler) {
        return new FieldCylindricalShadowEclipseDetector<>(sun, occultingBodyRadius, newMaxCheck, newThreshold, newMaxIter, newHandler);
    }
}
