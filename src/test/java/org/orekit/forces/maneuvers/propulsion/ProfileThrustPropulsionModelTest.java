/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.forces.maneuvers.propulsion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.forces.maneuvers.Control3DVectorCostType;
import org.orekit.forces.maneuvers.TriggeredManeuver;
import org.orekit.forces.maneuvers.trigger.DateBasedManeuverTriggers;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

import java.util.ArrayList;
import java.util.List;

class ProfileThrustPropulsionModelTest {

    @Test
    void testRoughBehaviour() {
        doRoughBehaviour(  1.0, 2008.017, 28968115.974);
        doRoughBehaviour( 10.0, 2009.229, 28950587.132);
        doRoughBehaviour(100.0, 2021.350, 28777772.266);
    }

    @Test
    void testRoughBehaviourField() {
        doRoughBehaviourField(Binary64Field.getInstance(),   1.0, 2008.017, 28968115.974);
        doRoughBehaviourField(Binary64Field.getInstance(),  10.0, 2009.229, 28950587.132);
        doRoughBehaviourField(Binary64Field.getInstance(), 100.0, 2021.350, 28777772.266);
    }

    private void doRoughBehaviour(final double rampDuration, final double expectedM, final double expectedA) {
        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = FastMath.toRadians(7);
        final double omega = FastMath.toRadians(180);
        final double OMEGA = FastMath.toRadians(261);
        final double lv = 0;

        final double duration     = 3653.99;
        final double f = 420;
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        final AttitudeProvider law = new FrameAlignedProvider(new Rotation(new Vector3D(alpha, delta),
                                                                           Vector3D.PLUS_I));

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 1, 1),
                                                       new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final Orbit initOrbit =
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, positionAngleType,
                               FramesFactory.getEME2000(), initDate, Constants.EIGEN5C_EARTH_MU);
        final SpacecraftState initialState =
            new SpacecraftState(initOrbit, law.getAttitude(initOrbit, initOrbit.getDate(), initOrbit.getFrame()), mass);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 1, 2),
                                                       new TimeComponents(4, 15, 34.080),
                                                       TimeScalesFactory.getUTC());

        final TimeSpanMap<PolynomialThrustSegment> profile = new TimeSpanMap<>(null);
        // ramp up from 0N to 420N in rampDuration
        final AbsoluteDate t0 = fireDate;
        profile.addValidAfter(new PolynomialThrustSegment(t0,
                                                          new PolynomialFunction(0.0, f / rampDuration),
                                                          new PolynomialFunction(0.0),
                                                          new PolynomialFunction(0.0)),
                              t0, false);
        // constant thrust at 420N for duration - 2 * ramp duration
        final AbsoluteDate t1 = fireDate.shiftedBy(rampDuration);
        profile.addValidAfter(new PolynomialThrustSegment(t1,
                                                          new PolynomialFunction(f),
                                                          new PolynomialFunction(0.0),
                                                          new PolynomialFunction(0.0)),
                              t1, false);
        // ramp down from 420N to 0N in rampDuration
        final AbsoluteDate t2 = fireDate.shiftedBy(duration - rampDuration);
        profile.addValidAfter(new PolynomialThrustSegment(t2,
                                                          new PolynomialFunction(f, -f / rampDuration),
                                                          new PolynomialFunction(0.0),
                                                          new PolynomialFunction(0.0)),
                              t2, false);
        // null thrust after duration
        final AbsoluteDate t3 = fireDate.shiftedBy(duration);
        profile.addValidAfter(new PolynomialThrustSegment(t3,
                                                          new PolynomialFunction(0.0),
                                                          new PolynomialFunction(0.0),
                                                          new PolynomialFunction(0.0)),
                              t3, false);
        final PropulsionModel propulsionModel = ProfileThrustPropulsionModel.of(profile, isp, Control3DVectorCostType.TWO_NORM, "ABM");

        Assertions.assertEquals("ABM", propulsionModel.getName());
        Assertions.assertEquals(0.0,     thrust(initialState, t0.shiftedBy(-0.001),              propulsionModel), 1.0e-8);
        Assertions.assertEquals(0.5 * f, thrust(initialState, t0.shiftedBy(+0.5 * rampDuration), propulsionModel), 1.0e-8);
        Assertions.assertEquals(f,       thrust(initialState, t1.shiftedBy(+0.001),              propulsionModel), 1.0e-8);
        Assertions.assertEquals(0.5 * f, thrust(initialState, t3.shiftedBy(-0.5 * rampDuration), propulsionModel), 1.0e-8);
        Assertions.assertEquals(0.0,     thrust(initialState, t3.shiftedBy(+0.001),              propulsionModel), 1.0e-8);

        final TriggeredManeuver maneuver = new TriggeredManeuver(null, new DateBasedManeuverTriggers(fireDate, duration), propulsionModel);

        double[][] tolerance = ToleranceProvider.getDefaultToleranceProvider(1e-6).getTolerances(initOrbit, initOrbit.getType());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(1.0e-6, rampDuration, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(0.1);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(initOrbit.getType());
        propagator.setInitialState(initialState);
        propagator.setAttitudeProvider(law);
        propagator.addForceModel(maneuver);
        final SpacecraftState finalorb = propagator.propagate(fireDate.shiftedBy(3800));

        Assertions.assertEquals(expectedM, finalorb.getMass(), 0.001);
        Assertions.assertEquals(expectedA, finalorb.getA(),    0.002);

    }

    private <T extends CalculusFieldElement<T>> void doRoughBehaviourField(final Field<T> field, final double rampDuration,
                                                                           final double expectedM, final double expectedA) {
        final double isp  = 318;
        final T zero = field.getZero();
        final T mass = zero.newInstance(2500);
        final T a = zero.newInstance(24396159);
        final T e = zero.newInstance(0.72831215);
        final T i = FastMath.toRadians(zero.newInstance(7));
        final T omega = FastMath.toRadians(zero.newInstance(180));
        final T OMEGA = FastMath.toRadians(zero.newInstance(261));
        final T lv = zero;

        final double duration = 3653.99;
        final double f        = 420;
        final double delta    = FastMath.toRadians(-7.4978);
        final double alpha    = FastMath.toRadians(351);
        final AttitudeProvider law = new FrameAlignedProvider(new Rotation(new Vector3D(alpha, delta),
                                                                           Vector3D.PLUS_I));

        final FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field,
                                                                      new DateComponents(2004, 1, 1),
                                                                      new TimeComponents(23, 30, 00.000),
                                                                      TimeScalesFactory.getUTC());
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final FieldOrbit<T> initOrbit =
            new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, positionAngleType,
                                      FramesFactory.getEME2000(), initDate,
                                      zero.newInstance(Constants.EIGEN5C_EARTH_MU));
        final FieldSpacecraftState<T> initialState =
            new FieldSpacecraftState<>(initOrbit, law.getAttitude(initOrbit, initOrbit.getDate(), initOrbit.getFrame()), mass);

        final FieldAbsoluteDate<T> fireDate = new FieldAbsoluteDate<>(field,
                                                                      new DateComponents(2004, 1, 2),
                                                                      new TimeComponents(4, 15, 34.080),
                                                                      TimeScalesFactory.getUTC());

        final TimeSpanMap<PolynomialThrustSegment> profile = new TimeSpanMap<>(null);
        // ramp up from 0N to 420N in rampDuration
        final AbsoluteDate t0 = fireDate.toAbsoluteDate();
        profile.addValidAfter(new PolynomialThrustSegment(t0,
                                                          new PolynomialFunction(0.0, f / rampDuration),
                                                          new PolynomialFunction(0.0),
                                                          new PolynomialFunction(0.0)),
                              t0, false);
        // constant thrust at 420N for duration - 2 * ramp duration
        final AbsoluteDate t1 = fireDate.shiftedBy(rampDuration).toAbsoluteDate();
        profile.addValidAfter(new PolynomialThrustSegment(t1,
                                                          new PolynomialFunction(f),
                                                          new PolynomialFunction(0.0),
                                                          new PolynomialFunction(0.0)),
                              t1, false);
        // ramp down from 420N to 0N in rampDuration
        final AbsoluteDate t2 = fireDate.shiftedBy(duration - rampDuration).toAbsoluteDate();
        profile.addValidAfter(new PolynomialThrustSegment(t2,
                                                          new PolynomialFunction(f, -f / rampDuration),
                                                          new PolynomialFunction(0.0),
                                                          new PolynomialFunction(0.0)),
                              t2, false);
        // null thrust after duration
        final AbsoluteDate t3 = fireDate.shiftedBy(duration).toAbsoluteDate();
        profile.addValidAfter(new PolynomialThrustSegment(t3,
                                                          new PolynomialFunction(0.0),
                                                          new PolynomialFunction(0.0),
                                                          new PolynomialFunction(0.0)),
                              t3, false);
        final PropulsionModel propulsionModel = ProfileThrustPropulsionModel.of(profile, isp, Control3DVectorCostType.TWO_NORM, "ABM");

        Assertions.assertEquals("ABM", propulsionModel.getName());
        Assertions.assertEquals(0.0,     thrust(initialState, fireDate.shiftedBy(-0.001),                        propulsionModel).getReal(), 1.0e-8);
        Assertions.assertEquals(0.5 * f, thrust(initialState, fireDate.shiftedBy(0.5 * rampDuration),            propulsionModel).getReal(), 1.0e-8);
        Assertions.assertEquals(f,       thrust(initialState, fireDate.shiftedBy( rampDuration + 0.001),         propulsionModel).getReal(), 1.0e-8);
        Assertions.assertEquals(0.5 * f, thrust(initialState, fireDate.shiftedBy(duration - 0.5 * rampDuration), propulsionModel).getReal(), 1.0e-8);
        Assertions.assertEquals(0.0,     thrust(initialState, fireDate.shiftedBy(duration + 0.001),              propulsionModel).getReal(), 1.0e-8);

        final TriggeredManeuver maneuver = new TriggeredManeuver(null, new DateBasedManeuverTriggers(fireDate.toAbsoluteDate(), duration), propulsionModel);

        double[][] tolerance = ToleranceProvider.getDefaultToleranceProvider(1e-6).getTolerances(initOrbit, initOrbit.getType());
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 1.0e-6, rampDuration, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(0.1);
        final FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(initOrbit.getType());
        propagator.setPositionAngleType(positionAngleType);
        propagator.setInitialState(initialState);
        propagator.setAttitudeProvider(law);
        propagator.addForceModel(maneuver);
        final FieldSpacecraftState<T> finalorb = propagator.propagate(fireDate.shiftedBy(3800));

        Assertions.assertEquals(expectedM, finalorb.getMass().getReal(), 0.001);
        Assertions.assertEquals(expectedA, finalorb.getA().getReal(),    0.002);

    }

    private double thrust(final SpacecraftState initialState, final AbsoluteDate targetDate, final PropulsionModel propulsionModel) {
        final SpacecraftState state = initialState.shiftedBy(targetDate.durationFrom(initialState.getDate()));
        return state.getMass() * propulsionModel.getAcceleration(state, state.getAttitude(), null).getNorm();
    }

    private <T extends CalculusFieldElement<T>> T thrust(final FieldSpacecraftState<T> initialState,
                                                         final FieldAbsoluteDate<T> targetDate,
                                                         final PropulsionModel propulsionModel) {
        final FieldSpacecraftState<T> state = initialState.shiftedBy(targetDate.durationFrom(initialState.getDate()));
        return state.getMass().multiply(propulsionModel.getAcceleration(state, state.getAttitude(), null).getNorm());
    }

    @Test
    void testGetThrustVector() {
        // GIVEN
        final ProfileThrustPropulsionModel propulsionModel = new ProfileThrustPropulsionModel(new TimeSpanMap<>(null),
                1., "");
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        // WHEN
        final Vector3D actualThrust = propulsionModel.getThrustVector(mockedState);
        // THEN
        Assertions.assertEquals(propulsionModel.getThrustVector(mockedState, new double[0]), actualThrust);
    }

    @Test
    void testGetFlowRate() {
        // GIVEN
        final ProfileThrustPropulsionModel propulsionModel = new ProfileThrustPropulsionModel(new TimeSpanMap<>(null),
                1., "");
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        // WHEN
        final double actualRate = propulsionModel.getFlowRate(mockedState);
        // THEN
        Assertions.assertEquals(propulsionModel.getFlowRate(mockedState, new double[0]), actualRate);
    }

    @Test
    void testGetControl3DVectorCostTypeDefault() {
        // GIVEN
        final ProfileThrustPropulsionModel propulsionModel = new ProfileThrustPropulsionModel(new TimeSpanMap<>(null),
                1., "");
        // WHEN
        final Control3DVectorCostType actualCostType = propulsionModel.getControl3DVectorCostType();
        // THEN
        Assertions.assertEquals(Control3DVectorCostType.TWO_NORM, actualCostType);
    }

    @Test
    void testGetControl3DVectorCostType() {
        // GIVEN
        final Control3DVectorCostType expectedCostType = Control3DVectorCostType.ONE_NORM;
        final ProfileThrustPropulsionModel propulsionModel = new ProfileThrustPropulsionModel(new TimeSpanMap<>(null),
                1., expectedCostType, "");
        // WHEN
        final Control3DVectorCostType actualCostType = propulsionModel.getControl3DVectorCostType();
        // THEN
        Assertions.assertEquals(expectedCostType, actualCostType);
    }

    @Test
    void testGetParametersDrivers() {
        // GIVEN
        final ParameterDriver driver = Mockito.mock(ParameterDriver.class);
        final String expectedName = "a";
        Mockito.when(driver.getName()).thenReturn(expectedName);
        final ThrustSegment segment = new TestThrustSegment(driver);
        final ProfileThrustPropulsionModel propulsionModel = new ProfileThrustPropulsionModel(new TimeSpanMap<>(segment),
                1., Control3DVectorCostType.NONE, "");
        // WHEN
        final List<ParameterDriver> propulsionDrivers = propulsionModel.getParametersDrivers();
        // THEN
        Assertions.assertEquals(1, propulsionDrivers.size());
        Assertions.assertEquals(expectedName, propulsionDrivers.get(0).getName());
    }

    private static class TestThrustSegment implements ThrustSegment {

        private final ParameterDriver driver;

        TestThrustSegment(final ParameterDriver driver) {
            this.driver = driver;
        }

        @Override
        public Vector3D getThrustVector(AbsoluteDate date, double mass, double[] parameters) {
            return Vector3D.PLUS_I;
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(FieldAbsoluteDate<T> date, T mass, T[] parameters) {
            return null;
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            final ArrayList<ParameterDriver> drivers = new ArrayList<>();
            drivers.add(driver);
            return drivers;
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
