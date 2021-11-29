/* Copyright 2002-2021 CS GROUP
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

public class FieldIntegrableGeneratorTest {

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

    private <T extends CalculusFieldElement<T>> void doTestInitNumerical(Field<T> field) {
        // setup
        final double reference = 1.25;
        InitCheckerEquations<T> checker = new InitCheckerEquations<>(reference);
        Assert.assertFalse(checker.wasCalled());

        // action
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 0.001, 200,
                                                                                              tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagatorNumerical = new FieldNumericalPropagator<>(field, integrator);
        propagatorNumerical.setInitialState(new FieldSpacecraftState<>(field, initialState).
                                            addAdditionalState(checker.getName(), field.getZero().newInstance(reference)));
        propagatorNumerical.addIntegrableGenerator(checker);
        propagatorNumerical.propagate(new FieldAbsoluteDate<>(field, initDate).shiftedBy(600));

        // verify
        Assert.assertTrue(checker.wasCalled());

    }

    /** Test for issue #401
     *  with a DSST propagator */
    @Test
    public void testInitDSST() {
        doTestIniDSST(Decimal64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIniDSST(Field<T> field) {
        // setup
        final double reference = 3.5;
        InitCheckerEquations<T> checker = new InitCheckerEquations<>(reference);
        Assert.assertFalse(checker.wasCalled());

        // action
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 0.001, 200,
                                                                                              tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        FieldDSSTPropagator<T> propagatorDSST = new FieldDSSTPropagator<>(field, integrator);
        propagatorDSST.setInitialState(new FieldSpacecraftState<>(field, initialState).
                                       addAdditionalState(checker.getName(), field.getZero().newInstance(reference)));
        propagatorDSST.addIntegrableGenerator(checker);
        propagatorDSST.propagate(new FieldAbsoluteDate<>(field, initDate).shiftedBy(600));

        // verify
        Assert.assertTrue(checker.wasCalled());

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

    public static class InitCheckerEquations<T extends CalculusFieldElement<T>> implements FieldIntegrableGenerator<T> {

        private double expected;
        private boolean called;

        public InitCheckerEquations(final double expected) {
            this.expected = expected;
            this.called   = false;
        }

        @Override
        public void init(FieldSpacecraftState<T> initiaState, FieldAbsoluteDate<T> target) {
            Assert.assertEquals(expected, initiaState.getAdditionalState(getName())[0].getReal(), 1.0e-15);
            called = true;
        }

        @Override
        public T[] generate(FieldSpacecraftState<T> s)  {
            final T[] pDot = MathArrays.buildArray(s.getDate().getField(), 1);
            pDot[0] = s.getDate().getField().getZero().newInstance(1.5);
            return pDot;
        }

        @Override
        public String getName() {
            return "linear";
        }

        public boolean wasCalled() {
            return called;
        }

    }

}
