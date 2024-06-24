/* Copyright 2020-2024 Exotrail
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
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.conversion.averaging.BrouwerLyddaneOrbitalState;
import org.orekit.propagation.conversion.averaging.elements.AveragedKeplerianWithMeanAngle;

/**
 * Class for osculating-to-averaged conversion according to Brouwer-Lyddane theory.
 * Value of M2 parameter is set to zero.
 *
 * @author Romain Serra
 * @see BrouwerLyddanePropagator
 * @see BrouwerLyddaneOrbitalState
 * @since 12.1
 */
public class OsculatingToBrouwerLyddaneConverter
        extends FixedPointOsculatingToAveragedConverter<BrouwerLyddaneOrbitalState> {

    /** So-called M2 coefficient, set to zero. */
    private static final double M2 = 0.;
    /** Order for spherical harmonics. */
    private static final int HARMONICS_ORDER = 0;

    /** Spherical harmonics provider. */
    private final UnnormalizedSphericalHarmonicsProvider harmonicsProvider;

    /**
     * Constructor with default parameters for fixed-point algorithm.
     * @param harmonicsProvider unnormalized provider
     */
    public OsculatingToBrouwerLyddaneConverter(final UnnormalizedSphericalHarmonicsProvider harmonicsProvider) {
        this(DEFAULT_EPSILON, DEFAULT_MAX_ITERATIONS, harmonicsProvider);
    }

    /**
     * Constructor.
     * @param epsilon convergence threshold
     * @param maxIterations maximum number of iterations
     * @param harmonicsProvider unnormalized provider
     */
    public OsculatingToBrouwerLyddaneConverter(final double epsilon, final int maxIterations,
                                               final UnnormalizedSphericalHarmonicsProvider harmonicsProvider) {
        super(epsilon, maxIterations);
        this.harmonicsProvider = harmonicsProvider;
    }

    /** {@inheritDoc} */
    @Override
    public BrouwerLyddaneOrbitalState convertToAveraged(final Orbit osculatingOrbit) {
        final KeplerianOrbit averagedOrbit = createAveragedOrbit(osculatingOrbit);
        final AveragedKeplerianWithMeanAngle averagedElements = buildElements(averagedOrbit);
        return new BrouwerLyddaneOrbitalState(averagedOrbit.getDate(), averagedElements,
                averagedOrbit.getFrame(), harmonicsProvider);
    }

    /**
     * Build averaged orbit.
     * @param osculatingOrbit osculating orbit
     * @return averaged Keplerian orbit in Brouwer-Lyddane sense.
     */
    private KeplerianOrbit createAveragedOrbit(final Orbit osculatingOrbit) {
        final UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics harmonics =
                harmonicsProvider.onDate(osculatingOrbit.getDate());
        return BrouwerLyddanePropagator.computeMeanOrbit(osculatingOrbit,
                harmonicsProvider.getAe(), harmonicsProvider.getMu(),
                harmonics.getUnnormalizedCnm(2, HARMONICS_ORDER),
                harmonics.getUnnormalizedCnm(3, HARMONICS_ORDER),
                harmonics.getUnnormalizedCnm(4, HARMONICS_ORDER),
                harmonics.getUnnormalizedCnm(5, HARMONICS_ORDER),
                M2, getEpsilon(), getMaxIterations());
    }

    /**
     * Build averaged orbital elements from orbit.
     * @param averagedOrbit averaged orbit
     * @return orbital elements
     */
    private AveragedKeplerianWithMeanAngle buildElements(final KeplerianOrbit averagedOrbit) {
        return new AveragedKeplerianWithMeanAngle(averagedOrbit.getA(), averagedOrbit.getE(),
                averagedOrbit.getI(), averagedOrbit.getPerigeeArgument(),
                averagedOrbit.getRightAscensionOfAscendingNode(), averagedOrbit.getMeanAnomaly());
    }

}
