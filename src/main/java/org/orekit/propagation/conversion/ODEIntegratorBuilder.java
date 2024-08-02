/* Copyright 2002-2024 CS GROUP
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
import org.orekit.utils.AbsolutePVCoordinates;

/** This interface is the top-level abstraction to build first order integrators for propagators conversion.
 * @author Pascal Parraud
 * @since 6.0
 */
public interface ODEIntegratorBuilder {

    /** Build a first order integrator.
     * @param orbit reference orbit
     * @param orbitType orbit type to use
     * @return a first order integrator ready to use
     */
    AbstractIntegrator buildIntegrator(Orbit orbit, OrbitType orbitType);

    /**
     * Build a first order integrator. Non-orbit version.
     * @param absolutePVCoordinates absolute position-velocity vector
     * @return a first order integrator ready to use
     */
    default AbstractIntegrator buildIntegrator(final AbsolutePVCoordinates absolutePVCoordinates) {
        final double arbitraryMu = 1.;
        final CartesianOrbit cartesianOrbit = new CartesianOrbit(absolutePVCoordinates.getPVCoordinates(),
            absolutePVCoordinates.getFrame(), arbitraryMu);
        return buildIntegrator(cartesianOrbit, OrbitType.CARTESIAN);
    }
}
