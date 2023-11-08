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
package org.orekit.forces.maneuvers.triggers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.trigger.AbstractManeuverTriggers;
import org.orekit.forces.maneuvers.trigger.FieldManeuverTriggersResetter;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggersResetter;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractManeuverTriggersTest<T extends AbstractManeuverTriggers> {

    /** trigger dates. */
    private AbsoluteDate triggerStart;
    private AbsoluteDate triggerStop;

    protected abstract T createTrigger(AbsoluteDate start, AbsoluteDate stop);

    private T configureTrigger(final AbsoluteDate start, final AbsoluteDate stop) {
        T trigger = createTrigger(start, stop);
        // set up separate detectors, to check lists of observers are handled properly
        trigger.addResetter(new Resetter());
        trigger.addResetter(new Resetter());
        return trigger;
    }

    private <S extends CalculusFieldElement<S>> T configureTrigger(final Field<S> field,
                                                                   final AbsoluteDate start, final AbsoluteDate stop) {
        T trigger = createTrigger(start, stop);
        // set up separate detectors, to check lists of observers are handled properly
        trigger.addResetter(field, new FieldResetter<>());
        trigger.addResetter(field, new FieldResetter<>());
        return trigger;
    }

    private class Resetter implements ManeuverTriggersResetter {
        public void maneuverTriggered(SpacecraftState state, boolean start) {
            if (start)  {
                triggerStart = state.getDate();
            } else {
                triggerStop  = state.getDate();
            }
        }
        public SpacecraftState resetState(SpacecraftState state) {
            return state;
        }
    }
    private class FieldResetter<S extends CalculusFieldElement<S>> implements FieldManeuverTriggersResetter<S> {
        public void maneuverTriggered(FieldSpacecraftState<S> state, boolean start) {
            if (start)  {
                triggerStart = state.getDate().toAbsoluteDate();
            } else {
                triggerStop = state.getDate().toAbsoluteDate();
            }
        }
        public FieldSpacecraftState<S> resetState(FieldSpacecraftState<S> state) {
            return state;
        }
    }
    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        triggerStart = null;
        triggerStop  = null;
    }

    @Test
    public void testRoughBehaviour() {

        final AttitudeProvider attitudeProvider = buildAttitudeProvider();
        final SpacecraftState initialState = buildInitialState(attitudeProvider);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final double isp = 318;
        final double duration = 3653.99;
        final double f = 420;
        final Maneuver maneuver = new Maneuver(null,
                                               configureTrigger(fireDate, fireDate.shiftedBy(duration)),
                                               new BasicConstantThrustPropulsionModel(f, isp, Vector3D.PLUS_I, "ABM"));
        Assertions.assertEquals(f,   ((BasicConstantThrustPropulsionModel) maneuver.getPropulsionModel()).getThrustMagnitude(), 1.0e-10);
        Assertions.assertEquals(isp, ((BasicConstantThrustPropulsionModel) maneuver.getPropulsionModel()).getIsp(),    1.0e-10);

        final SpacecraftState finalorb = buildPropagator(attitudeProvider, initialState, maneuver).
                                         propagate(fireDate.shiftedBy(3800));

        final double flowRate = ((BasicConstantThrustPropulsionModel) maneuver.getPropulsionModel()).getFlowRate();
        final double massTolerance = FastMath.abs(flowRate) * maneuver.getEventDetectors().findFirst().get().getThreshold();
        Assertions.assertEquals(2007.8824544261233, finalorb.getMass(), massTolerance);
        Assertions.assertEquals(2.6872, FastMath.toDegrees(MathUtils.normalizeAngle(finalorb.getI(), FastMath.PI)), 1e-4);
        Assertions.assertEquals(28970, finalorb.getA()/1000, 1);

        final List<EventDetector> list1 = maneuver.getManeuverTriggers().getEventDetectors().collect(Collectors.toList());
        final List<EventDetector> list2 = maneuver.getManeuverTriggers().getEventDetectors().collect(Collectors.toList());
        Assertions.assertEquals(list1.size(), list2.size());
        for (int i = 0; i < list1.size(); ++i ) {
            Assertions.assertSame(list1.get(i), list2.get(i));
        }

        Assertions.assertEquals(0.0,      triggerStart.durationFrom(fireDate), 1.0e-10);
        Assertions.assertEquals(duration, triggerStop.durationFrom(fireDate),  1.0e-10);

    }

    @Test
    public void testBackward() {

        final AttitudeProvider attitudeProvider = buildAttitudeProvider();
        final SpacecraftState initialState = buildInitialState(attitudeProvider);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final double isp = 318;
        final double duration = 30;
        final double f = 420;
        final Maneuver maneuver = new Maneuver(null,
                                               configureTrigger(fireDate, fireDate.shiftedBy(duration)),
                                               new BasicConstantThrustPropulsionModel(f, isp, Vector3D.PLUS_I, "ABM"));
        Assertions.assertEquals(f,   ((BasicConstantThrustPropulsionModel) maneuver.getPropulsionModel()).getThrustMagnitude(), 1.0e-10);
        Assertions.assertEquals(isp, ((BasicConstantThrustPropulsionModel) maneuver.getPropulsionModel()).getIsp(),    1.0e-10);

        final SpacecraftState finalorb = buildPropagator(attitudeProvider,
                                                         initialState.shiftedBy(initialState.getKeplerianPeriod()),
                                                         maneuver).
                                         propagate(fireDate.shiftedBy(-10));

        Assertions.assertEquals(2504.040, finalorb.getMass(), 1.0e-3);

        Assertions.assertEquals(0.0,      triggerStart.durationFrom(fireDate), 1.0e-10);
        Assertions.assertEquals(duration, triggerStop.durationFrom(fireDate),  1.0e-10);

    }

    @Test
    public void testRoughBehaviourField() {
        doTestRoughBehaviourField(Binary64Field.getInstance());
    }

    private <S extends CalculusFieldElement<S>> void doTestRoughBehaviourField(Field<S> field) {
        final AttitudeProvider attitudeProvider = buildAttitudeProvider();
        final FieldSpacecraftState<S> initialState = buildInitialState(field, attitudeProvider);


        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final double isp = 318;
        final double duration = 3653.99;
        final double f = 420;
        final Maneuver maneuver = new Maneuver(null,
                                               configureTrigger(field, fireDate, fireDate.shiftedBy(duration)),
                                               new BasicConstantThrustPropulsionModel(f, isp, Vector3D.PLUS_I, "ABM"));
        Assertions.assertEquals(f,   ((BasicConstantThrustPropulsionModel) maneuver.getPropulsionModel()).getThrustMagnitude(), 1.0e-10);
        Assertions.assertEquals(isp, ((BasicConstantThrustPropulsionModel) maneuver.getPropulsionModel()).getIsp(),    1.0e-10);

        final FieldSpacecraftState<S> finalorb = buildPropagator(field, attitudeProvider, initialState, maneuver).
                                                 propagate(new FieldAbsoluteDate<>(field, fireDate).shiftedBy(3800));

        final double flowRate = ((BasicConstantThrustPropulsionModel) maneuver.getPropulsionModel()).getFlowRate();
        final double massTolerance = FastMath.abs(flowRate) * maneuver.getEventDetectors().findFirst().get().getThreshold();
        Assertions.assertEquals(2007.8824544261233, finalorb.getMass().getReal(), massTolerance);
        Assertions.assertEquals(2.6872, FastMath.toDegrees(MathUtils.normalizeAngle(finalorb.getI(), field.getZero().newInstance(FastMath.PI))).getReal(), 1e-4);
        Assertions.assertEquals(28970, finalorb.getA().divide(1000).getReal(), 1);

        final List<FieldEventDetector<?>> list1 = maneuver.getManeuverTriggers().getFieldEventDetectors(field).collect(Collectors.toList());
        final List<FieldEventDetector<?>> list2 = maneuver.getManeuverTriggers().getFieldEventDetectors(field).collect(Collectors.toList());
        Assertions.assertEquals(list1.size(), list2.size());
        for (int i = 0; i < list1.size(); ++i ) {
            Assertions.assertSame(list1.get(i), list2.get(i));
        }

        Assertions.assertEquals(0.0,      triggerStart.durationFrom(fireDate), 1.0e-10);
        Assertions.assertEquals(duration, triggerStop.durationFrom(fireDate),  1.0e-10);

    }

    @Test
    public void testBackwardField() {
        doTestBackwardField(Binary64Field.getInstance());
    }

    private <S extends CalculusFieldElement<S>> void doTestBackwardField(Field<S> field) {
        final AttitudeProvider attitudeProvider = buildAttitudeProvider();
        final FieldSpacecraftState<S> initialState = buildInitialState(field, attitudeProvider);


        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final double isp = 318;
        final double duration = 30;
        final double f = 420;
        final Maneuver maneuver = new Maneuver(null,
                                               configureTrigger(field, fireDate, fireDate.shiftedBy(duration)),
                                               new BasicConstantThrustPropulsionModel(f, isp, Vector3D.PLUS_I, "ABM"));
        Assertions.assertEquals(f,   ((BasicConstantThrustPropulsionModel) maneuver.getPropulsionModel()).getThrustMagnitude(), 1.0e-10);
        Assertions.assertEquals(isp, ((BasicConstantThrustPropulsionModel) maneuver.getPropulsionModel()).getIsp(),    1.0e-10);

        final FieldSpacecraftState<S> finalorb = buildPropagator(field, attitudeProvider,
                                                                 initialState.shiftedBy(initialState.getKeplerianPeriod()),
                                                                 maneuver).
                                                 propagate(new FieldAbsoluteDate<>(field, fireDate).shiftedBy(-10));

        Assertions.assertEquals(2504.040, finalorb.getMass().getReal(), 1.0e-3);

        Assertions.assertEquals(0.0,      triggerStart.durationFrom(fireDate), 1.0e-10);
        Assertions.assertEquals(duration, triggerStop.durationFrom(fireDate),  1.0e-10);

    }

    private AttitudeProvider buildAttitudeProvider() {
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        return new FrameAlignedProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));
    }

    private SpacecraftState buildInitialState(final AttitudeProvider attitudeProvider) {
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = FastMath.toRadians(7);
        final double omega = FastMath.toRadians(180);
        final double OMEGA = FastMath.toRadians(261);
        final double lv = 0;

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                       new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                               FramesFactory.getEME2000(), initDate, Constants.EIGEN5C_EARTH_MU);
        return new SpacecraftState(orbit, attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);
    }

    private <S extends CalculusFieldElement<S>> FieldSpacecraftState<S> buildInitialState(final Field<S> field,
                                                                       final AttitudeProvider attitudeProvider) {
        final S zero = field.getZero();
        final S mass = zero.newInstance(2500);
        final S a = zero.newInstance(24396159);
        final S e = zero.newInstance(0.72831215);
        final S i = FastMath.toRadians(zero.newInstance(7));
        final S omega = FastMath.toRadians(zero.newInstance(180));
        final S OMEGA = FastMath.toRadians(zero.newInstance(261));
        final S lv = zero.newInstance(0);

        final FieldAbsoluteDate<S> initDate = new FieldAbsoluteDate<>(field,
                        new DateComponents(2004, 01, 01),
                        new TimeComponents(23, 30, 00.000),
                        TimeScalesFactory.getUTC());
        final FieldOrbit<S> orbit =
                        new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                        FramesFactory.getEME2000(), initDate, zero.newInstance(Constants.EIGEN5C_EARTH_MU));
        return new FieldSpacecraftState<>(orbit, attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);
    }

    private NumericalPropagator buildPropagator(final AttitudeProvider attitudeProvider, final SpacecraftState initialState,
                                                final Maneuver maneuver) {
        OrbitType orbitType = OrbitType.EQUINOCTIAL;
        double[][] tol = NumericalPropagator.tolerances(1.0e-3, initialState.getOrbit(), orbitType);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(orbitType);
        propagator.setInitialState(initialState);
        propagator.setAttitudeProvider(attitudeProvider);
        propagator.addForceModel(maneuver);
        return propagator;
    }

    private <S extends CalculusFieldElement<S>> FieldNumericalPropagator<S> buildPropagator(final Field<S> field,
                                                                                            final AttitudeProvider attitudeProvider,
                                                                                            final FieldSpacecraftState<S> initialState,
                                                                                            final Maneuver maneuver) {
        OrbitType orbitType = OrbitType.EQUINOCTIAL;
        double[][] tol = FieldNumericalPropagator.tolerances(field.getZero().newInstance(1.0e-3), initialState.getOrbit(), orbitType);
        AdaptiveStepsizeFieldIntegrator<S> integrator = new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        final FieldNumericalPropagator<S> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(orbitType);
        propagator.setInitialState(initialState);
        propagator.setAttitudeProvider(attitudeProvider);
        propagator.addForceModel(maneuver);
        return propagator;
    }

}
