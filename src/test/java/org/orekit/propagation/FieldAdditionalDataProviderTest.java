/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54FieldIntegrator;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.analytical.FieldBrouwerLyddanePropagator;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.FieldDSSTPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

public class FieldAdditionalDataProviderTest {

    private double mu;
    private static final double DURATION = 600.0;
    private static final String STRING_BEFORE = "Let's go!";
    private static final String STRING_AFTER = "Good job!";

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
        mu = GravityFieldFactory.getUnnormalizedProvider(0, 0).getMu();
    }

    private <T extends CalculusFieldElement<T>> FieldSpacecraftState<T> createState(final Field<T> field) {
        final T zero = field.getZero();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));
        final FieldAbsoluteDate<T> initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                 FramesFactory.getEME2000(), initDate, zero.add(mu));
        return new FieldSpacecraftState<>(orbit);
    }

    private <T extends CalculusFieldElement<T>> AdaptiveStepsizeFieldIntegrator<T> createIntegrator(final Field<T> field, final FieldSpacecraftState<T> state) {
        double[][] tolerance = ToleranceProvider.getDefaultToleranceProvider(0.001).getTolerances(state.getOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince54FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        return integrator;
    }

    @Test
    public void testModifyMainState() {
        doTestModifyMainState(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestModifyMainState(final Field<T> field) {

        // Create propagator
        final FieldSpacecraftState<T> state = createState(field);
        final AdaptiveStepsizeFieldIntegrator<T> integrator = createIntegrator(field, state);
        final FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setInitialState(state);

        // Create state modifier
        final MainStateModifier<T> modifier = new MainStateModifier<>();

        // Add the provider to the propagator
        propagator.addAdditionalDataProvider(modifier);

        // Propagate
        final double dt = 600.0;
        final FieldSpacecraftState<T> propagated = propagator.propagate(state.getDate().shiftedBy(dt));

        // Verify
        Assertions.assertEquals(2 * SpacecraftState.DEFAULT_MASS, propagated.getMass().getReal(), 1.0e-12);
        Assertions.assertEquals(FastMath.PI,
                                propagated.getAttitude().getRotation().getAngle().getReal(),
                                1.0e-15);

    }

    @Test
    public void testIssue900Numerical() {
        doTestIssue900Numerical(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue900Numerical(final Field<T> field) {

        // Create propagator
        final FieldSpacecraftState<T> state = createState(field);
        final AdaptiveStepsizeFieldIntegrator<T> integrator = createIntegrator(field, state);
        final FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setInitialState(state);

        // Create additional state provider
        final String name          = "init";
        final TimeDifferenceProvider<T> provider = new TimeDifferenceProvider<>(name, field);
        Assertions.assertFalse(provider.wasCalled());

        // Add the provider to the propagator
        propagator.addAdditionalDataProvider(provider);

        // Propagate
        final double dt = 600.0;
        final FieldSpacecraftState<T> propagated = propagator.propagate(state.getDate().shiftedBy(dt));

        // Verify
        Assertions.assertTrue(provider.wasCalled());
        Assertions.assertEquals(dt, propagated.getAdditionalState(name)[0].getReal(), 0.01);

    }

    @Test
    public void testIssue900Dsst() {
        doTestIssue900Dsst(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue900Dsst(final Field<T> field) {

        // Create propagator
        final FieldSpacecraftState<T> state = createState(field);
        final AdaptiveStepsizeFieldIntegrator<T> integrator = createIntegrator(field, state);
        final FieldDSSTPropagator<T> propagator = new FieldDSSTPropagator<>(field, integrator);
        propagator.setInitialState(state, PropagationType.MEAN);

        // Create additional state provider
        final String name          = "init";
        final TimeDifferenceProvider<T> provider = new TimeDifferenceProvider<>(name, field);
        Assertions.assertFalse(provider.wasCalled());

        // Add the provider to the propagator
        propagator.addAdditionalDataProvider(provider);

        // Propagate
        final double dt = 600.0;
        final FieldSpacecraftState<T> propagated = propagator.propagate(state.getDate().shiftedBy(dt));

        // Verify
        Assertions.assertTrue(provider.wasCalled());
        Assertions.assertEquals(dt, propagated.getAdditionalState(name)[0].getReal(), 0.01);

    }

    @Test
    public void testIssue900BrouwerLyddane() {
        doTestIssue900BrouwerLyddane(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue900BrouwerLyddane(final Field<T> field) {

        // Create propagator
        final FieldSpacecraftState<T> state = createState(field);
        final FieldBrouwerLyddanePropagator<T> propagator =
                        new FieldBrouwerLyddanePropagator<>(state.getOrbit(), GravityFieldFactory.getUnnormalizedProvider(5, 0), BrouwerLyddanePropagator.M2);

        // Create additional state provider
        final String name          = "init";
        final TimeDifferenceProvider<T> provider = new TimeDifferenceProvider<>(name, field);
        Assertions.assertFalse(provider.wasCalled());

        // Add the provider to the propagator
        propagator.addAdditionalDataProvider(provider);

        // Propagate
        final double dt = 600.0;
        final FieldSpacecraftState<T> propagated = propagator.propagate(state.getDate().shiftedBy(dt));

        // Verify
        Assertions.assertTrue(provider.wasCalled());
        Assertions.assertEquals(dt, propagated.getAdditionalState(name)[0].getReal(), 0.01);

    }

    @Test
    void testPropagateAdditionalStringData() {
        doTestPropagateAdditionalStringData(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestPropagateAdditionalStringData(Field<T> field) {

        final FieldSpacecraftState<T> state = createState(field);
        final AdaptiveStepsizeFieldIntegrator<T> integrator = createIntegrator(field, state);
        final FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setInitialState(state);

        final MainStringDataModifier<T> modifier = new MainStringDataModifier<>();
        propagator.addAdditionalDataProvider(modifier);

        final FieldSpacecraftState<T> propagated = propagator.propagate(state.getDate().shiftedBy(DURATION));
        Assertions.assertEquals(STRING_AFTER, propagated.getAdditionalData(MainStringDataModifier.class.getSimpleName()));
    }

    @Test
    void testInterpolationAdditionalStringData() {
        doTestInterpolationAdditionalStringData(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestInterpolationAdditionalStringData(Field<T> field) {
        final FieldSpacecraftState<T> state = createState(field);
        final AdaptiveStepsizeFieldIntegrator<T> integrator = createIntegrator(field, state);
        final FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setInitialState(state);

        final MainStringDataModifier<T> modifier = new MainStringDataModifier<>();
        propagator.addAdditionalDataProvider(modifier);
        FieldEphemerisGenerator<T> generator = propagator.getEphemerisGenerator();
        propagator.propagate(state.getDate().shiftedBy(DURATION));
        FieldBoundedPropagator<T> ephemeris = generator.getGeneratedEphemeris();

        Assertions.assertEquals(STRING_BEFORE, getAdditionalDataAt(ephemeris, state.getDate().shiftedBy(DURATION / 2 - 0.1)));
        Assertions.assertEquals(STRING_AFTER, getAdditionalDataAt(ephemeris, state.getDate().shiftedBy(DURATION / 2)));
        Assertions.assertEquals(STRING_AFTER, getAdditionalDataAt(ephemeris, state.getDate().shiftedBy(DURATION / 2 + 0.1)));
    }

    private <T extends CalculusFieldElement<T>> Object getAdditionalDataAt(FieldPropagator<T> propagator, FieldAbsoluteDate<T> date) {
        return propagator.propagate(date).getAdditionalData(MainStringDataModifier.class.getSimpleName());
    }

    private static class MainStringDataModifier<T extends CalculusFieldElement<T>> implements FieldAdditionalDataProvider<String, T> {

        private String value;

        @Override
        public void init(FieldSpacecraftState<T> initialState, FieldAbsoluteDate<T> target) {
            value = target.getDate().isBefore(initialState.getDate().shiftedBy(DURATION / 2)) ? STRING_BEFORE : STRING_AFTER;
        }

        @Override
        public String getAdditionalData(FieldSpacecraftState<T> state) {
            return value;
        }

        @Override
        public String getName() {
            return MainStringDataModifier.class.getSimpleName();
        }
    }

    private static class MainStateModifier<T extends CalculusFieldElement<T>> extends FieldAbstractStateModifier<T> {
        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> change(final FieldSpacecraftState<T> state) {
            final Field<T> field = state.getDate().getField();
            return new FieldSpacecraftState<>(state.getOrbit(),
                    new FieldAttitude<>(state.getDate(),
                            state.getFrame(),
                            new FieldRotation<>(field.getZero(),
                                    field.getZero(),
                                    field.getZero(),
                                    field.getOne(),
                                    false),
                            FieldVector3D.getZero(field),
                            FieldVector3D.getZero(field)))
                    .withMass(field.getZero().newInstance(2 * SpacecraftState.DEFAULT_MASS));
        }
    }

    private static class TimeDifferenceProvider<T extends CalculusFieldElement<T>> implements FieldAdditionalDataProvider<T[], T> {

        private final String   name;
        private boolean  called;
        private T        dt;
        private final Field<T> field;

        public TimeDifferenceProvider(final String name, final Field<T> field) {
            this.name   = name;
            this.called = false;
            this.dt     = field.getZero();
            this.field  = field;
        }

        @Override
        public void init(FieldSpacecraftState<T> initialState, FieldAbsoluteDate<T> target) {
            this.called = true;
            this.dt     = target.durationFrom(initialState.getDate());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public T[] getAdditionalData(FieldSpacecraftState<T> state) {
            final T[] array = MathArrays.buildArray(field, 1);
            array[0] = dt;
            return array;
        }

        public boolean wasCalled() {
            return called;
        }

    }

}
