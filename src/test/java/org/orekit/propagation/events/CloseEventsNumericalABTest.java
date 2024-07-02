/*
 * Licensed to the Hipparchus project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orekit.propagation.events;

import org.hipparchus.ode.nonstiff.AdamsBashforthIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;

/**
 * Test event handling with a {@link NumericalPropagator} and a {@link
 * AdamsBashforthIntegrator}.
 *
 * @author Evan Ward
 */
public class CloseEventsNumericalABTest extends CloseEventsAbstractTest {

    /**
     * Create a propagator using the {@link #initialOrbit}.
     *
     * @param stepSize   of integrator.
     * @return a usable propagator.
     */
    public Propagator getPropagator(double stepSize) {
        double[][] tol = NumericalPropagator
                .tolerances(10000, initialOrbit, OrbitType.CARTESIAN);
        final AdamsBashforthIntegrator integrator =
                        new AdamsBashforthIntegrator(4, stepSize, stepSize, tol[0], tol[1]);
        final DormandPrince853Integrator starter =
                        new DormandPrince853Integrator(stepSize / 100, stepSize / 10,
                                                       tol[0], tol[1]);
        starter.setInitialStepSize(stepSize / 20);
        integrator.setStarterIntegrator(starter);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.setOrbitType(OrbitType.CARTESIAN);
        return propagator;
    }

}
