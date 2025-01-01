/* Copyright 2020-2025 Exotrail
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
package org.orekit.propagation.conversion.averaging.converters;

import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.conversion.averaging.EcksteinHechlerOrbitalState;
import org.orekit.propagation.conversion.averaging.elements.AveragedCircularWithMeanAngle;

/**
 * Class for osculating-to-averaged conversion according to Eckstein-Hechler theory.
 *
 * @author Romain Serra
 * @see EcksteinHechlerPropagator
 * @see EcksteinHechlerOrbitalState
 * @since 12.1
 */
public class OsculatingToEcksteinHechlerConverter
        extends FixedPointOsculatingToAveragedConverter<EcksteinHechlerOrbitalState> {

    /** Order for spherical harmonics. */
    private static final int HARMONICS_ORDER = 0;

    /** Spherical harmonics provider. */
    private final UnnormalizedSphericalHarmonicsProvider harmonicsProvider;

    /**
     * Constructor with default parameters for fixed-point algorithm.
     * @param harmonicsProvider unnormalized provider
     */
    public OsculatingToEcksteinHechlerConverter(final UnnormalizedSphericalHarmonicsProvider harmonicsProvider) {
        this(DEFAULT_EPSILON, DEFAULT_MAX_ITERATIONS, harmonicsProvider);
    }

    /**
     * Constructor.
     * @param epsilon convergence threshold
     * @param maxIterations maximum number of iterations
     * @param harmonicsProvider unnormalized provider
     */
    public OsculatingToEcksteinHechlerConverter(final double epsilon, final int maxIterations,
                                                final UnnormalizedSphericalHarmonicsProvider harmonicsProvider) {
        super(epsilon, maxIterations);
        this.harmonicsProvider = harmonicsProvider;
    }

    /** {@inheritDoc} */
    @Override
    public EcksteinHechlerOrbitalState convertToAveraged(final Orbit osculatingOrbit) {
        final CircularOrbit averagedOrbit = createAveragedOrbit(osculatingOrbit);
        final AveragedCircularWithMeanAngle elements = buildElements(averagedOrbit);
        return new EcksteinHechlerOrbitalState(averagedOrbit.getDate(), elements,
                averagedOrbit.getFrame(), harmonicsProvider);
    }

    /**
     * Build averaged orbit.
     * @param osculatingOrbit osculating orbit
     * @return averaged Circular orbit in Eckstein-Hechler sense.
     */
    private CircularOrbit createAveragedOrbit(final Orbit osculatingOrbit) {
        final UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics harmonics =
                harmonicsProvider.onDate(osculatingOrbit.getDate());
        return EcksteinHechlerPropagator.computeMeanOrbit(osculatingOrbit,
                harmonicsProvider.getAe(), harmonicsProvider.getMu(),
                harmonics.getUnnormalizedCnm(2, HARMONICS_ORDER),
                harmonics.getUnnormalizedCnm(3, HARMONICS_ORDER),
                harmonics.getUnnormalizedCnm(4, HARMONICS_ORDER),
                harmonics.getUnnormalizedCnm(5, HARMONICS_ORDER),
                harmonics.getUnnormalizedCnm(6, HARMONICS_ORDER),
                getEpsilon(), getMaxIterations());
    }

    /**
     * Build averaged orbital elements from orbit.
     * @param averagedOrbit averaged orbit
     * @return orbital elements
     */
    private AveragedCircularWithMeanAngle buildElements(final CircularOrbit averagedOrbit) {
        return new AveragedCircularWithMeanAngle(averagedOrbit.getA(),
                averagedOrbit.getCircularEx(), averagedOrbit.getCircularEy(), averagedOrbit.getI(),
                averagedOrbit.getRightAscensionOfAscendingNode(), averagedOrbit.getAlphaM());
    }

}
