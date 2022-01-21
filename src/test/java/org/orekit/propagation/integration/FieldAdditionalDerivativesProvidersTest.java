/* Copyright 2002-2022 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.MathArrays;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.FieldDSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinates;

public class FieldAdditionalDerivativesProvidersTest {

    private double                       mu;
    private AbsoluteDate                 initDate;
    private SpacecraftState              initialState;
    private double[][]                   tolerance;

    /** Test for issue #401
     *  with a numerical propagator */
    @Test
    public void testInitNumerical() {
        doTestInitNumerical(Decimal64Field.getInstance());
    }

    /** Test for issue #401
     *  with a DSST propagator */
    @Test
    public void testInitDSST() {
        doTestInitDSST(Decimal64Field.getInstance());
    }

    @Test
    public void testResetState() {
        doTestResetState(Decimal64Field.getInstance());
    }

    @Test
    public void testYield() {
        doTestYield(Decimal64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestInitNumerical(Field<T> field) {
        // setup
        final double reference = 1.25;
        final double rate      = 1.5;
        final double dt        = 600.0;
        Linear<T> linear = new Linear<>("linear", reference, rate);
        Assert.assertFalse(linear.wasCalled());

        // action
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 0.001, 200,
                                                                                              tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagatorNumerical = new FieldNumericalPropagator<>(field, integrator);
        propagatorNumerical.setInitialState(new FieldSpacecraftState<>(field, initialState).
                                            addAdditionalState(linear.getName(), field.getZero().newInstance(reference)));
        propagatorNumerical.addAdditionalDerivativesProvider(linear);
        FieldSpacecraftState<T> finalState = propagatorNumerical.propagate(new FieldAbsoluteDate<>(field, initDate).shiftedBy(dt));

        // verify
        Assert.assertTrue(linear.wasCalled());
        Assert.assertEquals(reference + dt * rate, finalState.getAdditionalState(linear.getName())[0].getReal(), 1.0e-10);

    }

    private <T extends CalculusFieldElement<T>> void doTestInitDSST(Field<T> field) {
        // setup
        final double reference = 3.5;
        final double rate      = 1.5;
        final double dt        = 600.0;
        Linear<T> linear = new Linear<>("linear", reference, rate);
        Assert.assertFalse(linear.wasCalled());

        // action
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 0.001, 200,
                                                                                              tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        FieldDSSTPropagator<T> propagatorDSST = new FieldDSSTPropagator<>(field, integrator);
        propagatorDSST.setInitialState(new FieldSpacecraftState<>(field, initialState).
                                       addAdditionalState(linear.getName(), field.getZero().newInstance(reference)));
        propagatorDSST.addAdditionalDerivativesProvider(linear);
        FieldSpacecraftState<T> finalState = propagatorDSST.propagate(new FieldAbsoluteDate<>(field, initDate).shiftedBy(dt));

        // verify
        Assert.assertTrue(linear.wasCalled());
        Assert.assertEquals(reference + dt * rate, finalState.getAdditionalState(linear.getName())[0].getReal(), 1.0e-10);

    }

    private <T extends CalculusFieldElement<T>> void doTestResetState(Field<T> field) {
        // setup
        final double reference1 = 3.5;
        final double rate1      = 1.5;
        Linear<T> linear1 = new Linear<>("linear-1", reference1, rate1);
        Assert.assertFalse(linear1.wasCalled());
        final double reference2 = 4.5;
        final double rate2      = 1.25;
        Linear<T> linear2 = new Linear<>("linear-2", reference2, rate2);
        Assert.assertFalse(linear2.wasCalled());
        final double dt = 600;

        // action
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 0.001, 200,
                                                                                              tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagatorNumerical = new FieldNumericalPropagator<>(field, integrator);
        propagatorNumerical.setInitialState(new FieldSpacecraftState<>(field, initialState).
                                            addAdditionalState(linear1.getName(), field.getZero().newInstance(reference1)).
                                            addAdditionalState(linear2.getName(), field.getZero().newInstance(reference2)));
        propagatorNumerical.addAdditionalDerivativesProvider(linear1);
        propagatorNumerical.addAdditionalDerivativesProvider(linear2);
        FieldSpacecraftState<T> finalState = propagatorNumerical.propagate(new FieldAbsoluteDate<>(field, initDate).shiftedBy(dt));

        // verify
        Assert.assertTrue(linear1.wasCalled());
        Assert.assertTrue(linear2.wasCalled());
        Assert.assertEquals(reference1 + dt * rate1, finalState.getAdditionalState(linear1.getName())[0].getReal(), 1.0e-10);
        Assert.assertEquals(reference2 + dt * rate2, finalState.getAdditionalState(linear2.getName())[0].getReal(), 1.0e-10);

    }

    private <T extends CalculusFieldElement<T>> void doTestYield(Field<T> field) {

        // setup
        final double init1 = 1.0;
        final double init2 = 2.0;
        final double rate  = 0.5;
        final double dt    = 600;
        Yield<T> yield1 = new Yield<>(null, "yield-1", rate);
        Yield<T> yield2 = new Yield<>(yield1.getName(), "yield-2", Double.NaN);

        // action
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 0.001, 200,
                        tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagatorNumerical = new FieldNumericalPropagator<>(field, integrator);
        propagatorNumerical.setInitialState(new FieldSpacecraftState<>(field, initialState).
                                            addAdditionalState(yield1.getName(), field.getZero().newInstance(init1)).
                                            addAdditionalState(yield2.getName(), field.getZero().newInstance(init2)));
        propagatorNumerical.addAdditionalDerivativesProvider(yield2); // we intentionally register yield2 before yield 1 to check reordering
        propagatorNumerical.addAdditionalDerivativesProvider(yield1);
        FieldSpacecraftState<T> finalState = propagatorNumerical.propagate(new FieldAbsoluteDate<>(field, initDate).shiftedBy(dt));

        // verify
        Assert.assertEquals(init1 + dt * rate, finalState.getAdditionalState(yield1.getName())[0].getReal(),           1.0e-10);
        Assert.assertEquals(init2 + dt * rate, finalState.getAdditionalState(yield2.getName())[0].getReal(),           1.0e-10);
        Assert.assertEquals(rate,              finalState.getAdditionalStateDerivative(yield1.getName())[0].getReal(), 1.0e-10);
        Assert.assertEquals(rate,              finalState.getAdditionalStateDerivative(yield2.getName())[0].getReal(), 1.0e-10);

    }

    @Before
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

    @After
    public void tearDown() {
        initDate     = null;
        initialState = null;
        tolerance    = null;
    }

    private static class Linear<T extends CalculusFieldElement<T>> implements FieldAdditionalDerivativesProvider<T> {

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
        public void init(FieldSpacecraftState<T> initiaState, FieldAbsoluteDate<T> target) {
            Assert.assertEquals(expectedAtInit, initiaState.getAdditionalState(getName())[0].getReal(), 1.0e-15);
            called = true;
        }

        @Override
        public T[] derivatives(FieldSpacecraftState<T> s) {
            final T[] pDot = MathArrays.buildArray(s.getDate().getField(), 1);
            pDot[0] = s.getDate().getField().getZero().newInstance(rate);
            return pDot;
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

    private static class Yield<T extends CalculusFieldElement<T>> implements FieldAdditionalDerivativesProvider<T> {

        private String dependency;
        private String name;
        private double rate;

        public Yield(final String dependency, final String name, final double rate) {
            this.dependency = dependency;
            this.name       = name;
            this.rate       = rate;
        }

        @Override
        public T[] derivatives(final FieldSpacecraftState<T> s) {
            final T[] pDot;
            if (dependency == null) {
                pDot = MathArrays.buildArray(s.getDate().getField(), 1);
                pDot[0] = s.getDate().getField().getZero().newInstance(rate);
            } else {
                pDot = s.getAdditionalStateDerivative(dependency);
            }
            return pDot;
        }

        @Override
        public boolean yield(final FieldSpacecraftState<T> state) {
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

}
