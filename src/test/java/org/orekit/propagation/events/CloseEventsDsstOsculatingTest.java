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
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.utils.Constants;

/** Test events with the DSST Propagator.
*
* @author Evan Ward
*/
public class CloseEventsDsstOsculatingTest extends CloseEventsAbstractTest {

    @Override
    public Propagator getPropagator(double stepSize) {
        double[][] tol = DSSTPropagator.tolerances(1, initialOrbit);
        ODEIntegrator integrator = new DormandPrince853Integrator(stepSize, stepSize, tol[0], tol[1]);
        DSSTPropagator propagator = new DSSTPropagator(integrator, PropagationType.OSCULATING);
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        double gm = Constants.EIGEN5C_EARTH_MU;
        propagator.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getMoon(), gm));
        return propagator;
    }

}
