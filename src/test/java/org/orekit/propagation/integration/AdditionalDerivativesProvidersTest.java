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
package org.orekit.propagation.integration;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

public class AdditionalDerivativesProvidersTest {

    private double          mu;
    private AbsoluteDate    initDate;
    private SpacecraftState initialState;
    private double[][]      tolerance;

    /** Test for issue #401
     *  with a numerical propagator */
    @Test
    public void testInitNumerical() {

        // setup
        final double reference = 1.25;
        final double rate      = 1.5;
        final double dt        = 600.0;
        Linear linear = new Linear("linear", reference, rate);
        Assertions.assertFalse(linear.wasCalled());

        // action
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        NumericalPropagator propagatorNumerical = new NumericalPropagator(integrator);
        propagatorNumerical.setInitialState(initialState.addAdditionalState(linear.getName(), reference));
        propagatorNumerical.addAdditionalDerivativesProvider(linear);
        SpacecraftState finalState = propagatorNumerical.propagate(initDate.shiftedBy(dt));

        // verify
        Assertions.assertTrue(linear.wasCalled());
        Assertions.assertEquals(reference + dt * rate, finalState.getAdditionalState(linear.getName())[0], 1.0e-10);

    }

    /** Test for issue #401
     *  with a DSST propagator */
    @Test
    public void testInitDSST() {

        // setup
        final double reference = 3.5;
        final double rate      = 1.5;
        final double dt        = 600.0;
        Linear linear = new Linear("linear", reference, rate);
        Assertions.assertFalse(linear.wasCalled());

        // action
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        DSSTPropagator propagatorDSST = new DSSTPropagator(integrator);
        propagatorDSST.setInitialState(initialState.addAdditionalState(linear.getName(), reference));
        propagatorDSST.addAdditionalDerivativesProvider(linear);
        SpacecraftState finalState = propagatorDSST.propagate(initDate.shiftedBy(dt));

        // verify
        Assertions.assertTrue(linear.wasCalled());
        Assertions.assertEquals(reference + dt * rate, finalState.getAdditionalState(linear.getName())[0], 1.0e-10);

    }

    @Test
    public void testResetState() {

        // setup
        final double reference1 = 3.5;
        final double rate1      = 1.5;
        Linear linear1 = new Linear("linear-1", reference1, rate1);
        Assertions.assertFalse(linear1.wasCalled());
        final double reference2 = 4.5;
        final double rate2      = 1.25;
        Linear linear2 = new Linear("linear-2", reference2, rate2);
        Assertions.assertFalse(linear2.wasCalled());
        final double dt = 600;

        // action
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        NumericalPropagator propagatorNumerical = new NumericalPropagator(integrator);
        propagatorNumerical.setInitialState(initialState.
                                            addAdditionalState(linear1.getName(), reference1).
                                            addAdditionalState(linear2.getName(), reference2));
        propagatorNumerical.addAdditionalDerivativesProvider(linear1);
        propagatorNumerical.addAdditionalDerivativesProvider(linear2);
        propagatorNumerical.addEventDetector(new ImpulseManeuver(new DateDetector(initDate.shiftedBy(dt / 2.0)),
                                                                 new Vector3D(0.1, 0.2, 0.3), 350.0));
        SpacecraftState finalState = propagatorNumerical.propagate(initDate.shiftedBy(dt));

        // verify
        Assertions.assertTrue(linear1.wasCalled());
        Assertions.assertTrue(linear2.wasCalled());
        Assertions.assertEquals(reference1 + dt * rate1, finalState.getAdditionalState(linear1.getName())[0], 1.0e-10);
        Assertions.assertEquals(reference2 + dt * rate2, finalState.getAdditionalState(linear2.getName())[0], 1.0e-10);

    }

    @Test
    public void testYield() {

        // setup
        final double init1 = 1.0;
        final double init2 = 2.0;
        final double rate  = 0.5;
        final double dt    = 600;
        Yield yield1 = new Yield(null, "yield-1", rate);
        Yield yield2 = new Yield(yield1.getName(), "yield-2", Double.NaN);

        // action
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        NumericalPropagator propagatorNumerical = new NumericalPropagator(integrator);
        propagatorNumerical.setInitialState(initialState.
                                            addAdditionalState(yield1.getName(), init1).
                                            addAdditionalState(yield2.getName(), init2));
        propagatorNumerical.addAdditionalDerivativesProvider(yield2); // we intentionally register yield2 before yield 1 to check reordering
        propagatorNumerical.addAdditionalDerivativesProvider(yield1);
        SpacecraftState finalState = propagatorNumerical.propagate(initDate.shiftedBy(dt));

        // verify
        Assertions.assertEquals(init1 + dt * rate, finalState.getAdditionalState(yield1.getName())[0], 1.0e-10);
        Assertions.assertEquals(init2 + dt * rate, finalState.getAdditionalState(yield2.getName())[0], 1.0e-10);
        Assertions.assertEquals(rate, finalState.getAdditionalStateDerivative(yield1.getName())[0], 1.0e-10);
        Assertions.assertEquals(rate, finalState.getAdditionalStateDerivative(yield2.getName())[0], 1.0e-10);

    }

    @Test
    public void testCoupling() {

        // setup
        final double   dt       = 600.0;
        final Coupling coupling = new Coupling("coupling", 3.5, -2.0, 1.0);

        // action
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        NumericalPropagator propagatorNumerical = new NumericalPropagator(integrator);
        propagatorNumerical.setInitialState(initialState.
                                            addAdditionalState(coupling.getName(),
                                                               coupling.secondaryInit));
        propagatorNumerical.addAdditionalDerivativesProvider(coupling);
        SpacecraftState finalState = propagatorNumerical.propagate(initDate.shiftedBy(dt));

        // verify
        Assertions.assertEquals(coupling.secondaryInit + dt * coupling.secondaryRate,
                finalState.getAdditionalState(coupling.getName())[0], 1.0e-10);
        Assertions.assertEquals(initialState.getA() + dt * coupling.smaRate, finalState.getA(), 1.0e-10);

    }

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
        tolerance = NumericalPropagator.tolerances(0.001, orbit, OrbitType.EQUINOCTIAL);
    }

    @AfterEach
    public void tearDown() {
        initDate     = null;
        initialState = null;
        tolerance    = null;
    }

    private static class Linear implements AdditionalDerivativesProvider {

        private String  name;
        private double  expectedAtInit;
        private double  rate;
        private boolean called;

        public Linear(final String name, final double expectedAtInit, final double rate) {
            this.name           = name;
            this.expectedAtInit = expectedAtInit;
            this.rate           = rate;
            this.called         = false;
        }

        @Override
        public void init(SpacecraftState initiaState, AbsoluteDate target) {
            Assertions.assertEquals(expectedAtInit, initiaState.getAdditionalState(getName())[0], 1.0e-15);
            called = true;
        }

        @Override
        public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
            return new CombinedDerivatives(new double[] { rate }, null);
        }

        @Override
        public int getDimension() {
            return 1;
        }

        @Override
        public String getName() {
            return name;
        }

        public boolean wasCalled() {
            return called;
        }

    }

    private static class Yield implements AdditionalDerivativesProvider {

        private String dependency;
        private String name;
        private double rate;

        public Yield(final String dependency, final String name, final double rate) {
            this.dependency = dependency;
            this.name       = name;
            this.rate       = rate;
        }

        @Override
        public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
            return new CombinedDerivatives(dependency == null ?
                                           new double[] { rate } :
                                           s.getAdditionalStateDerivative(dependency),
                                           null);
        }

        @Override
        public boolean yields(final SpacecraftState state) {
            return dependency != null && !state.hasAdditionalStateDerivative(dependency);
        }

        @Override
        public int getDimension() {
            return 1;
        }

        @Override
        public String getName() {
            return name;
        }

    }

    private static class Coupling implements AdditionalDerivativesProvider {

        private final String  name;
        private final double  secondaryInit;
        private final double  secondaryRate;
        private final double  smaRate;

        public Coupling(final String name,
                        final double secondaryInit, final double secondaryRate,
                        final double smaRate) {
            this.name          = name;
            this.secondaryInit = secondaryInit;
            this.secondaryRate = secondaryRate;
            this.smaRate       = smaRate;
        }

        @Override
        public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
            return new CombinedDerivatives(new double[] { secondaryRate },
                                           new double[] { smaRate, 0.0, 0.0, 0.0, 0.0, 0.0 });
        }

        @Override
        public int getDimension() {
            return 1;
        }

        @Override
        public String getName() {
            return name;
        }

    }

}
