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
package org.orekit.propagation.conversion.averaging;

import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.propagation.conversion.averaging.elements.AveragedEquinoctialWithMeanAngle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class representing an averaged orbital state as in the DSST theory using only the first 6 zonal
 * harmonics as perturbations.
 *
 * @author Romain Serra
 * @see AveragedOrbitalState
 * @see DSSTPropagator
 * @see DSSTZonal
 * @since 12.1
 */
public class DSST6X0OrbitalState extends AbstractHarmonicsBasedOrbitalState {

    /** Averaged equinoctial elements. */
    private final AveragedEquinoctialWithMeanAngle averagedElements;

    /**
     * Constructor.
     * @param date epoch
     * @param elements averaged orbital elements
     * @param frame reference frame
     * @param harmonicsProvider spherical harmonics provider
     */
    public DSST6X0OrbitalState(final AbsoluteDate date,
                               final AveragedEquinoctialWithMeanAngle elements,
                               final Frame frame,
                               final UnnormalizedSphericalHarmonicsProvider harmonicsProvider) {
        super(date, frame, harmonicsProvider);
        this.averagedElements = elements;
    }

    /**
     * Create collection of fist 6 zonal DSST forces.
     * @param provider spherical harmonics provider
     * @return six first zonal forces
     */
    public static Collection<DSSTForceModel> createForces(final UnnormalizedSphericalHarmonicsProvider provider) {
        final List<DSSTForceModel> forceModels = new ArrayList<>();
        final DSSTZonal zonal = new DSSTZonal(provider);
        forceModels.add(zonal);
        return forceModels;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getOrbitType() {
        return OrbitType.EQUINOCTIAL;
    }

    /** {@inheritDoc} */
    @Override
    public PositionAngleType getPositionAngleType() {
        return PositionAngleType.MEAN;
    }

    /** {@inheritDoc} */
    @Override
    public AveragedEquinoctialWithMeanAngle getAveragedElements() {
        return averagedElements;
    }

    /** {@inheritDoc} */
    @Override
    public Orbit toOsculatingOrbit() {
        final EquinoctialOrbit orekitOrbit = createOrekitOrbit();
        final Collection<DSSTForceModel> forceModels = createForces(getHarmonicsProvider());
        final SpacecraftState osculatingState = DSSTPropagator.computeOsculatingState(
                new SpacecraftState(orekitOrbit), null, forceModels);
        return osculatingState.getOrbit();
    }

    /**
     * Create equinoctial orbit representation of averaged state.
     * @return equinoctial orbit
     */
    private EquinoctialOrbit createOrekitOrbit() {
        return new EquinoctialOrbit(averagedElements.getAveragedSemiMajorAxis(),
                averagedElements.getAveragedEquinoctialEx(),
                averagedElements.getAveragedEquinoctialEy(),
                averagedElements.getAveragedHx(), averagedElements.getAveragedHy(),
                averagedElements.getAveragedMeanLongitudeArgument(),
                getPositionAngleType(), getFrame(), getDate(), getMu());
    }

}
