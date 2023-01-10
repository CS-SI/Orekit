/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

public class AdditionalStateProviderTest {

    private double                     mu;
    private AbsoluteDate               initDate;
    private SpacecraftState            initialState;
    private AdaptiveStepsizeIntegrator integrator;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
        mu  = GravityFieldFactory.getUnnormalizedProvider(0, 0).getMu();
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit, OrbitType.EQUINOCTIAL);
        integrator = new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
    }

    @Test
    public void testIssue900Numerical() {

        // Create propagator
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);

        // Create additional state provider
        final String name          = "init";
        final TimeDifferenceProvider provider = new TimeDifferenceProvider(name);
        Assertions.assertFalse(provider.wasCalled());

        // Add the provider to the propagator
        propagator.addAdditionalStateProvider(provider);

        // Propagate
        final double dt = 600.0;
        final SpacecraftState propagated = propagator.propagate(initDate.shiftedBy(dt));

        // Verify
        Assertions.assertTrue(provider.wasCalled());
        Assertions.assertEquals(dt, propagated.getAdditionalState(name)[0], 0.01);

    }

    @Test
    public void testIssue900Dsst() {

        // Initialize propagator
        final DSSTPropagator propagator = new DSSTPropagator(integrator);
        propagator.setInitialState(initialState, PropagationType.MEAN);

        // Create additional state provider
        final String name          = "init";
        final TimeDifferenceProvider provider = new TimeDifferenceProvider(name);
        Assertions.assertFalse(provider.wasCalled());

        // Add the provider to the propagator
        propagator.addAdditionalStateProvider(provider);

        // Propagate
        final double dt = 600.0;
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(dt));

        // Verify
        Assertions.assertTrue(provider.wasCalled());
        Assertions.assertEquals(dt, propagated.getAdditionalState(name)[0], 0.01);

    }

    @Test
    public void testIssue900BrouwerLyddane() {

        // Initialize propagator
        BrouwerLyddanePropagator propagator = new BrouwerLyddanePropagator(initialState.getOrbit(), Utils.defaultLaw(),
                                                                           GravityFieldFactory.getUnnormalizedProvider(5, 0), BrouwerLyddanePropagator.M2);

        // Create additional state provider
        final String name          = "init";
        final TimeDifferenceProvider provider = new TimeDifferenceProvider(name);
        Assertions.assertFalse(provider.wasCalled());

        // Add the provider to the propagator
        propagator.addAdditionalStateProvider(provider);

        // Propagate
        final double dt = 600.0;
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(dt));

        // Verify
        Assertions.assertTrue(provider.wasCalled());
        Assertions.assertEquals(dt, propagated.getAdditionalState(name)[0], 0.01);

    }

    private static class TimeDifferenceProvider implements AdditionalStateProvider {

        private String  name;
        private boolean called;
        private double  dt;

        public TimeDifferenceProvider(final String name) {
            this.name   = name;
            this.called = false;
            this.dt     = 0.0;
        }

        @Override
        public void init(SpacecraftState initialState, AbsoluteDate target) {
            this.called = true;
            this.dt     = target.durationFrom(initialState.getDate());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public double[] getAdditionalState(SpacecraftState state) {
            return new double[] {
                dt
            };
        }

        public boolean wasCalled() {
            return called;
        }

    }

}
