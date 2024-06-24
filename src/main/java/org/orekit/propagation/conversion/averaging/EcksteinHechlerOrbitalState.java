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
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.propagation.conversion.averaging.elements.AveragedCircularWithMeanAngle;

/**
 * Class representing an averaged orbital state as in the Eckstein-Hechler theory.
 *
 * @author Romain Serra
 * @see AveragedOrbitalState
 * @see EcksteinHechlerPropagator
 * @since 12.1
 */
public class EcksteinHechlerOrbitalState extends AbstractHarmonicsBasedOrbitalState {

    /** Averaged circular elements. */
    private final AveragedCircularWithMeanAngle averagedElements;

    /**
     * Constructor.
     * @param date epoch
     * @param elements averaged orbital elements
     * @param frame reference frame
     * @param harmonicsProvider spherical harmonics provider
     */
    public EcksteinHechlerOrbitalState(final AbsoluteDate date,
                                       final AveragedCircularWithMeanAngle elements,
                                       final Frame frame,
                                       final UnnormalizedSphericalHarmonicsProvider harmonicsProvider) {
        super(date, frame, harmonicsProvider);
        this.averagedElements = elements;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getOrbitType() {
        return OrbitType.CIRCULAR;
    }

    /** {@inheritDoc} */
    @Override
    public PositionAngleType getPositionAngleType() {
        return PositionAngleType.MEAN;
    }

    /** {@inheritDoc} */
    @Override
    public AveragedCircularWithMeanAngle getAveragedElements() {
        return averagedElements;
    }

    /** {@inheritDoc} */
    @Override
    public Orbit toOsculatingOrbit() {
        final EcksteinHechlerPropagator propagator = createPropagator();
        return propagator.propagateOrbit(getDate());
    }

    /**
     * Create Eckstein-Hechler propagator.
     * @return propagator using relevant theory
     */
    private EcksteinHechlerPropagator createPropagator() {
        final CircularOrbit orekitOrbit = createOrekitOrbit();
        return new EcksteinHechlerPropagator(orekitOrbit, getHarmonicsProvider(),
                PropagationType.MEAN);
    }

    /**
     * Create circular orbit representation of averaged state.
     * @return circular orbit
     */
    private CircularOrbit createOrekitOrbit() {
        return new CircularOrbit(averagedElements.getAveragedSemiMajorAxis(),
                averagedElements.getAveragedCircularEx(), averagedElements.getAveragedCircularEy(),
                averagedElements.getAveragedInclination(),
                averagedElements.getAveragedRightAscensionOfTheAscendingNode(),
                averagedElements.getAveragedMeanLatitudeArgument(), getPositionAngleType(),
                getFrame(), getDate(), getMu());
    }

}
