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
package org.orekit.propagation.conversion;

import org.hipparchus.ode.AbstractIntegrator;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.utils.AbsolutePVCoordinates;

/**
 * Abstract class for integrator builder.
 *
 * @param <T> field type
 * @since 13.0
 * @author Romain Serra
 */
public abstract class AbstractIntegratorBuilder<T extends AbstractIntegrator> implements ODEIntegratorBuilder {

    @Override
    public abstract T buildIntegrator(Orbit orbit, OrbitType orbitType, PositionAngleType angleType);

    @Override
    public T buildIntegrator(final Orbit orbit, final OrbitType orbitType) {
        return buildIntegrator(orbit, orbitType, PositionAngleType.MEAN);
    }

    @Override
    public T buildIntegrator(final AbsolutePVCoordinates absolutePVCoordinates) {
        final double arbitraryMu = 1.;
        final CartesianOrbit cartesianOrbit = new CartesianOrbit(absolutePVCoordinates.getPVCoordinates(),
                absolutePVCoordinates.getFrame(), arbitraryMu);
        return buildIntegrator(cartesianOrbit, OrbitType.CARTESIAN);
    }
}