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
package org.orekit.propagation.conversion.averaging;

import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.propagation.conversion.averaging.elements.AveragedKeplerianWithMeanAngle;

/**
 * Class representing an averaged orbital state as in the Brouwer-Lyddane theory.
 *
 * @author Romain Serra
 * @see AveragedOrbitalState
 * @see BrouwerLyddanePropagator
 * @since 12.1
 */
public class BrouwerLyddaneOrbitalState extends AbstractHarmonicsBasedOrbitalState {

    /** So-called M2 coefficient, set to zero. */
    private static final double M2 = 0.;

    /** Averaged Keplerian elements. */
    private final AveragedKeplerianWithMeanAngle averagedElements;

    /**
     * Constructor.
     * @param date epoch
     * @param elements averaged orbital elements
     * @param frame reference frame
     * @param harmonicsProvider spherical harmonics provider
     */
    public BrouwerLyddaneOrbitalState(final AbsoluteDate date,
                                      final AveragedKeplerianWithMeanAngle elements,
                                      final Frame frame,
                                      final UnnormalizedSphericalHarmonicsProvider harmonicsProvider) {
        super(date, frame, harmonicsProvider);
        this.averagedElements = elements;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getOrbitType() {
        return OrbitType.KEPLERIAN;
    }

    /** {@inheritDoc} */
    @Override
    public PositionAngleType getPositionAngleType() {
        return PositionAngleType.MEAN;
    }

    /** {@inheritDoc} */
    @Override
    public AveragedKeplerianWithMeanAngle getAveragedElements() {
        return averagedElements;
    }

    /** {@inheritDoc} */
    @Override
    public Orbit toOsculatingOrbit() {
        final BrouwerLyddanePropagator propagator = createPropagator();
        return propagator.propagateOrbit(getDate());
    }

    /**
     * Create Brouwer-Lyddane propagator.
     * @return propagator using relevant theory
     */
    private BrouwerLyddanePropagator createPropagator() {
        final KeplerianOrbit orekitOrbit = createOrekitOrbit();
        return new BrouwerLyddanePropagator(orekitOrbit, getHarmonicsProvider(),
                PropagationType.MEAN, M2);
    }

    /**
     * Create Keplerian orbit representation of averaged state.
     * @return Keplerian orbit
     */
    private KeplerianOrbit createOrekitOrbit() {
        return new KeplerianOrbit(averagedElements.getAveragedSemiMajorAxis(),
                averagedElements.getAveragedEccentricity(), averagedElements.getAveragedInclination(),
                averagedElements.getAveragedPerigeeArgument(),
                averagedElements.getAveragedRightAscensionOfTheAscendingNode(),
                averagedElements.getAveragedMeanAnomaly(), getPositionAngleType(), getFrame(),
                getDate(), getMu());
    }

}
