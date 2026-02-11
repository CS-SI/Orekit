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

import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.numerical.NumericalPropagator;

/**
 * Test event handling with a {@link NumericalPropagator} and a {@link
 * GraggBulirschStoerIntegrator}.
 *
 * @author Evan Ward
 */
public class CloseEventsNumericalGBSTest extends AbstractCloseEventsNumericalTest {

    @Override
    ODEIntegrator getIntegrator(double stepSize, final OrbitType orbitType) {
        double[][] tol = ToleranceProvider.getDefaultToleranceProvider(1e1).getTolerances(initialOrbit, orbitType);
        return new GraggBulirschStoerIntegrator(stepSize, stepSize, tol[0], tol[1]);
    }
}
