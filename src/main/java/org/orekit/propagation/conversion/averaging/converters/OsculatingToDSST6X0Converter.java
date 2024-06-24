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
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.conversion.averaging.DSST6X0OrbitalState;
import org.orekit.propagation.conversion.averaging.elements.AveragedEquinoctialWithMeanAngle;

import java.util.Collection;

/**
 * Class for osculating-to-averaged conversion according to DSST theory, using 6 zonal harmonics as
 * the only perturbations.
 *
 * @author Romain Serra
 * @see DSSTPropagator
 * @see DSST6X0OrbitalState
 * @since 12.1
 */
public class OsculatingToDSST6X0Converter
        extends FixedPointOsculatingToAveragedConverter<DSST6X0OrbitalState> {

    /** Spherical harmonics provider. */
    private final UnnormalizedSphericalHarmonicsProvider harmonicsProvider;

    /**
     * Constructor with default parameters for fixed-point algorithm.
     * @param harmonicsProvider unnormalized provider
     */
    public OsculatingToDSST6X0Converter(final UnnormalizedSphericalHarmonicsProvider harmonicsProvider) {
        this(DEFAULT_EPSILON, DEFAULT_MAX_ITERATIONS, harmonicsProvider);
    }

    /**
     * Constructor.
     * @param epsilon convergence threshold
     * @param maxIterations maximum number of iterations
     * @param harmonicsProvider unnormalized provider
     */
    public OsculatingToDSST6X0Converter(final double epsilon, final int maxIterations,
                                        final UnnormalizedSphericalHarmonicsProvider harmonicsProvider) {
        super(epsilon, maxIterations);
        this.harmonicsProvider = harmonicsProvider;
    }

    /** {@inheritDoc} */
    @Override
    public DSST6X0OrbitalState convertToAveraged(final Orbit osculatingOrbit) {
        final Orbit averagedOrbit = createAveragedOrbit(osculatingOrbit);
        final AveragedEquinoctialWithMeanAngle elements = buildElements(averagedOrbit);
        return new DSST6X0OrbitalState(averagedOrbit.getDate(), elements,
                averagedOrbit.getFrame(), harmonicsProvider);
    }

    /**
     * Build averaged orbit.
     * @param osculatingOrbit osculating orbit
     * @return averaged orbit in DSST sense.
     */
    private Orbit createAveragedOrbit(final Orbit osculatingOrbit) {
        final Collection<DSSTForceModel> forceModels = DSST6X0OrbitalState
                .createForces(harmonicsProvider);
        final SpacecraftState osculatingState = new SpacecraftState(osculatingOrbit);
        final SpacecraftState averagedState = DSSTPropagator.computeMeanState(osculatingState,
                null, forceModels, getEpsilon(), getMaxIterations());
        return averagedState.getOrbit();
    }

    /**
     * Build averaged orbital elements from orbit.
     * @param averagedOrbit averaged orbit
     * @return orbital elements
     */
    private AveragedEquinoctialWithMeanAngle buildElements(final Orbit averagedOrbit) {
        return new AveragedEquinoctialWithMeanAngle(averagedOrbit.getA(),
                averagedOrbit.getEquinoctialEx(), averagedOrbit.getEquinoctialEy(),
                averagedOrbit.getHx(), averagedOrbit.getHy(), averagedOrbit.getLM());
    }

}
