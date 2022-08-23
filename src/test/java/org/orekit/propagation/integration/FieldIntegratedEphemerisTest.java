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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.FieldDenseOutputModel;
import org.hipparchus.ode.FieldExpandableODE;
import org.hipparchus.ode.FieldODEState;
import org.hipparchus.ode.FieldOrdinaryDifferentialEquation;
import org.hipparchus.ode.FieldSecondaryODE;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.EulerFieldIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.InertialProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldAdditionalStateProvider;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldEphemerisGenerator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;


public class FieldIntegratedEphemerisTest {

    @Test
    public void testNormalKeplerIntegration() {
        doTestNormalKeplerIntegration(Decimal64Field.getInstance());
    }

    @Test
    public void testGetFrame() {
        doTestGetFrame(Decimal64Field.getInstance());
    }

    @Test
    public void testAdditionalState() {
        doTestAdditionalState(Decimal64Field.getInstance());
    }

    @Test
    public void testNoReset() {
        doTestNoReset(Decimal64Field.getInstance());
    }

    @Deprecated
    @Test
    public void testDeprecated() {
        doTestDeprecated(Decimal64Field.getInstance());
    }

    @Test
    public void testAdditionalDerivatives() {
        doTestAdditionalDerivatives(Decimal64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNormalKeplerIntegration(Field<T> field) {
        FieldOrbit<T> initialOrbit = createOrbit(field);
        FieldNumericalPropagator<T> numericalPropagator = createPropagator(field);
        // Keplerian propagator definition
        FieldKeplerianPropagator<T> keplerEx = new FieldKeplerianPropagator<>(initialOrbit);

        // Integrated ephemeris

        // Propagation
        FieldAbsoluteDate<T> finalDate = initialOrbit.getDate().shiftedBy(3600.0);
        final FieldEphemerisGenerator<T> generator1 = numericalPropagator.getEphemerisGenerator();
        numericalPropagator.setInitialState(new FieldSpacecraftState<>(initialOrbit));
        numericalPropagator.propagate(finalDate);
        Assertions.assertTrue(numericalPropagator.getCalls() < 3200);
        FieldBoundedPropagator<T> ephemeris = generator1.getGeneratedEphemeris();

        // tests
        for (double dt = 1; dt <= 3600.0; dt += 1) {
            FieldAbsoluteDate<T> intermediateDate = initialOrbit.getDate().shiftedBy(dt);
            FieldSpacecraftState<T> keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
            FieldSpacecraftState<T> numericIntermediateOrbit = ephemeris.propagate(intermediateDate);
            FieldVector3D<T> kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
            FieldVector3D<T> numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();

            Assertions.assertEquals(0, kepPosition.subtract(numPosition).getNorm().getReal(), 5.0e-2);
        }

        // test inv
        FieldAbsoluteDate<T> intermediateDate = initialOrbit.getDate().shiftedBy(1589);
        FieldSpacecraftState<T> keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
        FieldSpacecraftState<T> state = keplerEx.propagate(finalDate);
        numericalPropagator.setInitialState(state);
        final FieldEphemerisGenerator<T> generator2 = numericalPropagator.getEphemerisGenerator();
        numericalPropagator.propagate(initialOrbit.getDate());
        FieldBoundedPropagator<T> invEphemeris = generator2.getGeneratedEphemeris();
        FieldSpacecraftState<T> numericIntermediateOrbit = invEphemeris.propagate(intermediateDate);
        FieldVector3D<T> kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
        FieldVector3D<T> numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();

        Assertions.assertEquals(0, kepPosition.subtract(numPosition).getNorm().getReal(), 3.0e-3);

    }

    private <T extends CalculusFieldElement<T>>  void doTestGetFrame(Field<T> field) {
        FieldOrbit<T> initialOrbit = createOrbit(field);
        FieldNumericalPropagator<T> numericalPropagator = createPropagator(field);
        // setup
        FieldAbsoluteDate<T> finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        final FieldEphemerisGenerator<T> generator = numericalPropagator.getEphemerisGenerator();
        numericalPropagator.setInitialState(new FieldSpacecraftState<>(initialOrbit));
        numericalPropagator.propagate(finalDate);
        Assertions.assertTrue(numericalPropagator.getCalls() < 3200);
        FieldBoundedPropagator<T> ephemeris = generator.getGeneratedEphemeris();

        //action
        Assertions.assertNotNull(ephemeris.getFrame());
        Assertions.assertSame(ephemeris.getFrame(), numericalPropagator.getFrame());
    }

    private <T extends CalculusFieldElement<T>>  void doTestAdditionalState(Field<T> field) {
        final FieldOrbit<T> initialOrbit = createOrbit(field);
        FieldNumericalPropagator<T> numericalPropagator = createPropagator(field);

        // setup
        FieldAbsoluteDate<T> finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        final FieldEphemerisGenerator<T> generator = numericalPropagator.getEphemerisGenerator();
        numericalPropagator.setInitialState(new FieldSpacecraftState<>(initialOrbit));
        numericalPropagator.propagate(finalDate);
        Assertions.assertTrue(numericalPropagator.getCalls() < 3200);
        FieldBoundedPropagator<T> ephemeris = generator.getGeneratedEphemeris();
        ephemeris.addAdditionalStateProvider(new FieldAdditionalStateProvider<T>() {

            @Override
            public String getName() {
                return "time-since-start";
            }

            @Override
            public T[] getAdditionalState(FieldSpacecraftState<T> state) {
                T[] array = MathArrays.buildArray(state.getDate().getField(), 1);
                array[0] = state.getDate().durationFrom(initialOrbit.getDate());
                return array;
            }
        });

        //action
        FieldSpacecraftState<T> s = ephemeris.propagate(initialOrbit.getDate().shiftedBy(20.0));
        Assertions.assertEquals(20.0, s.getAdditionalState("time-since-start")[0].getReal(), 1.0e-10);

        // check various protected methods
        try {

            Method getDrivers = ephemeris.getClass().getDeclaredMethod("getParametersDrivers", (Class[]) null);
            getDrivers.setAccessible(true);
            Assertions.assertTrue(((List<?>) getDrivers.invoke(ephemeris, (Object[]) null)).isEmpty());

            Method getMass = ephemeris.getClass().getDeclaredMethod("getMass", FieldAbsoluteDate.class);
            getMass.setAccessible(true);
            @SuppressWarnings("unchecked")
            T mass = (T) getMass.invoke(ephemeris, finalDate);
            Assertions.assertEquals(1000.0, mass.getReal(), 1.0e-10);

            Method propagateOrbit = ephemeris.getClass().getDeclaredMethod("propagateOrbit",
                                                                           FieldAbsoluteDate.class,
                                                                           CalculusFieldElement[].class);
            propagateOrbit.setAccessible(true);
            @SuppressWarnings("unchecked")
            FieldOrbit<T> orbit = (FieldOrbit<T>) propagateOrbit.invoke(ephemeris, finalDate, null);
            Assertions.assertEquals(initialOrbit.getA().getReal(), orbit.getA().getReal(), 1.0e-10);

        } catch (IllegalAccessException | NoSuchMethodException | SecurityException |
                 IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
        }

    }

    private <T extends CalculusFieldElement<T>>  void doTestNoReset(Field<T> field) {
        final FieldOrbit<T> initialOrbit = createOrbit(field);
        FieldNumericalPropagator<T> numericalPropagator = createPropagator(field);

        // setup
        FieldAbsoluteDate<T> finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        final FieldEphemerisGenerator<T> generator = numericalPropagator.getEphemerisGenerator();
        numericalPropagator.setInitialState(new FieldSpacecraftState<>(initialOrbit));
        numericalPropagator.propagate(finalDate);
        FieldBoundedPropagator<T> ephemeris = generator.getGeneratedEphemeris();
        ephemeris.addEventDetector(new FieldDateDetector<>(initialOrbit.getDate().shiftedBy(10)).
                                   withHandler((FieldSpacecraftState<T> s,
                                                FieldDateDetector<T> detector,
                                                boolean increasing) -> Action.RESET_STATE));

        try {
            ephemeris.propagate(initialOrbit.getDate(), finalDate);
            Assertions.fail("an exception should habe been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }

    }

    @Deprecated
    private <T extends CalculusFieldElement<T>> void doTestDeprecated(Field<T> field) {

        EulerFieldIntegrator<T> integ = new EulerFieldIntegrator<>(field, field.getZero().newInstance(1.0));
        FieldDenseOutputModel<T> dom = new FieldDenseOutputModel<>();
        integ.addStepHandler(dom);
        FieldExpandableODE<T> eode = new FieldExpandableODE<T>(new FieldOrdinaryDifferentialEquation<T>() {
            public int getDimension() { return 1; }
            public T[] computeDerivatives(T t, T[] y) { return y; }
        });
        eode.addSecondaryEquations(new FieldSecondaryODE<T>() {
            public int getDimension() { return 1; }
            public T[] computeDerivatives(T t, T[] primary, T[] primaryDot, T[] secondary) { return secondary; }
        });
        integ.integrate(eode,
                        new FieldODEState<>(field.getZero().newInstance(0.0),
                                            MathArrays.buildArray(field, 1),
                                            MathArrays.buildArray(field, 1, 1)),
                        field.getZero().newInstance(1.0));

        FieldStateMapper<T> mapper = new FieldStateMapper<T>(FieldAbsoluteDate.getArbitraryEpoch(field),
                                                             field.getZero().newInstance(Constants.EIGEN5C_EARTH_MU),
                                                             OrbitType.CARTESIAN, PositionAngle.TRUE,
                                                             new InertialProvider(FramesFactory.getEME2000()),
                                                             FramesFactory.getEME2000()) {
            public void mapStateToArray(FieldSpacecraftState<T> state, T[] y, T[] yDot) {}
            public FieldSpacecraftState<T> mapArrayToState(FieldAbsoluteDate<T> date, T[] y, T[] yDot, PropagationType type) {
                return null;
            }
        };

        try {
            new FieldIntegratedEphemeris<T>(FieldAbsoluteDate.getArbitraryEpoch(field),
                                            FieldAbsoluteDate.getPastInfinity(field),
                                            FieldAbsoluteDate.getFutureInfinity(field),
                                            mapper, PropagationType.OSCULATING,
                                            dom, Collections.emptyMap(), Collections.emptyList(),
                                            new String[] { "equation-1", "equation-2" });
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitInternalError oie) {
            // expected as only one equation could be handled properly by this deprecated constructor
        }

        FieldIntegratedEphemeris<T> ie = new FieldIntegratedEphemeris<T>(FieldAbsoluteDate.getArbitraryEpoch(field),
                                                                         FieldAbsoluteDate.getPastInfinity(field),
                                                                         FieldAbsoluteDate.getFutureInfinity(field),
                                                                         mapper, PropagationType.OSCULATING,
                                                                         dom, Collections.emptyMap(), Collections.emptyList(),
                                                                         new String[] { "equation-1" });
        Assertions.assertNotNull(ie);

    }

    private <T extends CalculusFieldElement<T>> void doTestAdditionalDerivatives(final Field<T> field) {

        final FieldOrbit<T> initialOrbit = createOrbit(field);
        FieldAbsoluteDate<T> finalDate = initialOrbit.getDate().shiftedBy(10.0);
        double[][] tolerances = FieldNumericalPropagator.tolerances(field.getZero().newInstance(1.0e-3),
                                                                    initialOrbit, OrbitType.CARTESIAN);
        DormandPrince853FieldIntegrator<T> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 1.0e-6, 10.0, tolerances[0], tolerances[1]);
        integrator.setInitialStepSize(1.0e-3);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        final DerivativesProvider<T> provider1 = new DerivativesProvider<>(field, "provider-1", 3);
        propagator.addAdditionalDerivativesProvider(provider1);
        final DerivativesProvider<T> provider2 = new DerivativesProvider<>(field, "provider-2", 1);
        propagator.addAdditionalDerivativesProvider(provider2);
        final FieldEphemerisGenerator<T> generator = propagator.getEphemerisGenerator();
        propagator.setInitialState(new FieldSpacecraftState<>(initialOrbit).
                                   addAdditionalState(provider1.getName(), MathArrays.buildArray(field, provider1.getDimension())).
                                   addAdditionalState(provider2.getName(), MathArrays.buildArray(field, provider2.getDimension())));
        propagator.propagate(finalDate);
        FieldBoundedPropagator<T> ephemeris = generator.getGeneratedEphemeris();

        for (double dt = 0; dt < ephemeris.getMaxDate().durationFrom(ephemeris.getMinDate()).getReal(); dt += 0.1) {
            FieldSpacecraftState<T> state = ephemeris.propagate(ephemeris.getMinDate().shiftedBy(dt));
            checkState(dt, state, provider1);
            checkState(dt, state, provider2);
        }

    }

    private <T extends CalculusFieldElement<T>> void checkState(final double dt, final FieldSpacecraftState<T> state,
                                                                final DerivativesProvider<T> provider) {

        Assertions.assertTrue(state.hasAdditionalState(provider.getName()));
        Assertions.assertEquals(provider.getDimension(), state.getAdditionalState(provider.getName()).length);
        for (int i = 0; i < provider.getDimension(); ++i) {
            Assertions.assertEquals(i * dt,
                                state.getAdditionalState(provider.getName())[i].getReal(),
                                4.0e-15 * i * dt);
        }

        Assertions.assertTrue(state.hasAdditionalStateDerivative(provider.getName()));
        Assertions.assertEquals(provider.getDimension(), state.getAdditionalStateDerivative(provider.getName()).length);
        for (int i = 0; i < provider.getDimension(); ++i) {
            Assertions.assertEquals(i,
                                state.getAdditionalStateDerivative(provider.getName())[i].getReal(),
                                2.0e-14 * i);
        }

    }

    @BeforeEach
    public void setUp() {
        mu = 3.9860047e14;
        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
    }

    private <T extends CalculusFieldElement<T>> FieldNumericalPropagator<T> createPropagator(Field<T> field) {
        double[] absTolerance= {
            0.0001, 1.0e-11, 1.0e-11, 1.0e-8, 1.0e-8, 1.0e-8, 0.001
        };
        double[] relTolerance = {
            1.0e-8, 1.0e-8, 1.0e-8, 1.0e-9, 1.0e-9, 1.0e-9, 1.0e-7
        };
        OrbitType type = OrbitType.EQUINOCTIAL;
        AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 0.001, 500, absTolerance, relTolerance);
        integrator.setInitialStepSize(100);
        FieldNumericalPropagator<T> numericalPropagator = new FieldNumericalPropagator<>(field, integrator);
        numericalPropagator.setOrbitType(type);
        return numericalPropagator;
    }

    private <T extends CalculusFieldElement<T>> FieldOrbit<T> createOrbit(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        double mu = 3.9860047e14;
        FieldAbsoluteDate<T> initDate = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(584.);
        return new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                           FramesFactory.getEME2000(), initDate, zero.add(mu));
    }

    double mu;

    private static class DerivativesProvider<T extends CalculusFieldElement<T>> implements FieldAdditionalDerivativesProvider<T> {
        private final String name;
        private final T[] derivatives;
        DerivativesProvider(final Field<T> field, final String name, final int dimension) {
            this.name        = name;
            this.derivatives = MathArrays.buildArray(field, dimension);
            for (int i = 0; i < dimension; ++i) {
                derivatives[i] = field.getZero().newInstance(i);
            }
        }
        public String getName() {
            return name;
        }
        public int getDimension() {
            return derivatives.length;
        }
        public T[] derivatives(final FieldSpacecraftState<T> s) {
            return derivatives;
        }
    }

}
