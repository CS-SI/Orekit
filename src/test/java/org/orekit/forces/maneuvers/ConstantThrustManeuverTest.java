/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.forces.maneuvers;


import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.UncorrelatedRandomVectorGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

public class ConstantThrustManeuverTest extends AbstractLegacyForceModelTest {

    // Body mu
    private double mu;

    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel,
                                                                         final AbsoluteDate date, final  Frame frame,
                                                                         final FieldVector3D<DerivativeStructure> position,
                                                                         final FieldVector3D<DerivativeStructure> velocity,
                                                                         final FieldRotation<DerivativeStructure> rotation,
                                                                         final DerivativeStructure mass) {
        try {
            double thrust = forceModel.getParameterDriver(ConstantThrustManeuver.THRUST).getValue();
            java.lang.reflect.Field directionField = ConstantThrustManeuver.class.getDeclaredField("direction");
            directionField.setAccessible(true);
            Vector3D direction;
            direction = (Vector3D) directionField.get(forceModel);
            if (((ConstantThrustManeuver) forceModel).isFiring(date)) {
                return new FieldVector3D<>(mass.reciprocal().multiply(thrust), rotation.applyInverseTo(direction));
            } else {
                // constant (and null) acceleration when not firing
                return FieldVector3D.getZero(mass.getField());
            }
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            Assert.fail(e.getLocalizedMessage());
            return null;
        }
    }

    private CircularOrbit dummyOrbit(AbsoluteDate date) {
        return new CircularOrbit(new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J),
                                 FramesFactory.getEME2000(), date, mu);
    }

    @Test
    public void testJacobianVs80Implementation() {
        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = FastMath.toRadians(7);
        final double omega = FastMath.toRadians(180);
        final double OMEGA = FastMath.toRadians(261);
        final double lv = 0;

        final double duration = 3653.99;
        final double f = 420;
 
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                       new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), initDate, mu);
        final AttitudeProvider law = new LofOffset(orbit.getFrame(), LOFType.VVLH);
       final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(fireDate, duration, f, isp, Vector3D.PLUS_I);

        // before maneuver (Jacobian wrt. state is zero)
        checkStateJacobianVs80Implementation(initialState, maneuver, law, 1.0e-50, false);

        // in maneuver
        SpacecraftState startState = initialState.shiftedBy(fireDate.durationFrom(initDate));
        maneuver.getEventsDetectors().findFirst().get().eventOccurred(startState, true);
        SpacecraftState midState = startState.shiftedBy(duration / 2.0);
        checkStateJacobianVs80Implementation(midState, maneuver, law, 1.0e-20, false);

    }

    @Test
    public void testPositiveDuration() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                             new TimeComponents(23, 30, 00.000),
                                             TimeScalesFactory.getUTC());
        ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(date, 10.0, 400.0, 300.0, Vector3D.PLUS_K);
        Assert.assertFalse(maneuver.dependsOnPositionOnly());
        Assert.assertNull(maneuver.getAttitudeOverride());
        Assert.assertEquals(0.0, Vector3D.distance(maneuver.getDirection(), Vector3D.PLUS_K), 1.0e-15);
        Assert.assertEquals(10.0, maneuver.getDuration(), 1.0e-15);
        Assert.assertEquals(0.0, date.durationFrom(maneuver.getStartDate()), 1.0e-15);
        Assert.assertEquals(0.0, date.shiftedBy(10.0).durationFrom(maneuver.getEndDate()), 1.0e-15);
        Assert.assertEquals("", maneuver.getName());
        ParameterDriver[] drivers = maneuver.getParametersDrivers();
        Assert.assertEquals(2, drivers.length);
        Assert.assertEquals("thrust", drivers[0].getName());
        Assert.assertEquals("flow rate", drivers[1].getName());
        EventDetector[] switches = maneuver.getEventsDetectors().toArray(EventDetector[]::new);

        Orbit o1 = dummyOrbit(date.shiftedBy(- 1.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        Orbit o2 = dummyOrbit(date.shiftedBy(  1.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        Orbit o3 = dummyOrbit(date.shiftedBy(  9.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        Orbit o4 = dummyOrbit(date.shiftedBy( 11.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);
    }

    @Test
    public void testNegativeDuration() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                             new TimeComponents(23, 30, 00.000),
                                             TimeScalesFactory.getUTC());
        ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(date, -10.0, 400.0, 300.0, Vector3D.PLUS_K,
                                       "1A-");
        ParameterDriver[] drivers = maneuver.getParametersDrivers();
        Assert.assertEquals(2, drivers.length);
        Assert.assertEquals("1A-thrust", drivers[0].getName());
        Assert.assertEquals("1A-flow rate", drivers[1].getName());
        EventDetector[] switches = maneuver.getEventsDetectors().toArray(EventDetector[]::new);

        Orbit o1 = dummyOrbit(date.shiftedBy(-11.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        Orbit o2 = dummyOrbit(date.shiftedBy( -9.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        Orbit o3 = dummyOrbit(date.shiftedBy( -1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        Orbit o4 = dummyOrbit(date.shiftedBy(  1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);
    }

    @Test
    public void testRoughBehaviour() {
        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = FastMath.toRadians(7);
        final double omega = FastMath.toRadians(180);
        final double OMEGA = FastMath.toRadians(261);
        final double lv = 0;

        final double duration = 3653.99;
        final double f = 420;
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        final AttitudeProvider law = new InertialProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                       new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), initDate, mu);
        final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(fireDate, duration, f, isp, Vector3D.PLUS_I);
        Assert.assertEquals(f,   maneuver.getThrust(), 1.0e-10);
        Assert.assertEquals(isp, maneuver.getISP(),    1.0e-10);

        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);
        propagator.setAttitudeProvider(law);
        propagator.addForceModel(maneuver);
        final SpacecraftState finalorb = propagator.propagate(fireDate.shiftedBy(3800));

        final double massTolerance =
                FastMath.abs(maneuver.getFlowRate()) * maneuver.getEventsDetectors().findFirst().get().getThreshold();
        Assert.assertEquals(2007.8824544261233, finalorb.getMass(), massTolerance);
        Assert.assertEquals(2.6872, FastMath.toDegrees(MathUtils.normalizeAngle(finalorb.getI(), FastMath.PI)), 1e-4);
        Assert.assertEquals(28970, finalorb.getA()/1000, 1);

    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
    @Test
    public void testRealField() {
        DSFactory factory = new DSFactory(6, 5);
        DerivativeStructure a_0 = factory.variable(0, 7e7);
        DerivativeStructure e_0 = factory.variable(1, 0.4);
        DerivativeStructure i_0 = factory.variable(2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngle.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();

        final OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(10.0, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        final ConstantThrustManeuver forceModel =  new ConstantThrustManeuver(J2000.toAbsoluteDate().shiftedBy(100), 100.0, 400.0, 300.0, Vector3D.PLUS_K);

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(1000.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assert.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getX(), finPVC_R.getPosition().getX(), FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assert.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getY(), finPVC_R.getPosition().getY(), FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assert.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getZ(), finPVC_R.getPosition().getZ(), FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);

        long number = 23091991;
        RandomGenerator RG = new Well19937a(number);
        GaussianRandomGenerator NGG = new GaussianRandomGenerator(RG);
        UncorrelatedRandomVectorGenerator URVG = new UncorrelatedRandomVectorGenerator(new double[] {0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.0 },
                                                                                       new double[] {1e3, 0.01, 0.01, 0.01, 0.01, 0.01},
                                                                                       NGG);
        double a_R = a_0.getReal();
        double e_R = e_0.getReal();
        double i_R = i_0.getReal();
        double R_R = R_0.getReal();
        double O_R = O_0.getReal();
        double n_R = n_0.getReal();
        for (int ii = 0; ii < 1; ii++){
            double[] rand_next = URVG.nextVector();
            double a_shift = a_R + rand_next[0];
            double e_shift = e_R + rand_next[1];
            double i_shift = i_R + rand_next[2];
            double R_shift = R_R + rand_next[3];
            double O_shift = O_R + rand_next[4];
            double n_shift = n_R + rand_next[5];

            KeplerianOrbit shiftedOrb = new KeplerianOrbit(a_shift, e_shift, i_shift, R_shift, O_shift, n_shift,
                                                           PositionAngle.MEAN,
                                                           EME,
                                                           J2000.toAbsoluteDate(),
                                                           Constants.EIGEN5C_EARTH_MU
                                                           );

            SpacecraftState shift_iSR = new SpacecraftState(shiftedOrb);

            NumericalPropagator shift_NP = new NumericalPropagator(RIntegrator);

            shift_NP.setInitialState(shift_iSR);

            shift_NP.addForceModel(forceModel);

            SpacecraftState finalState_shift = shift_NP.propagate(target.toAbsoluteDate());


            PVCoordinates finPVC_shift = finalState_shift.getPVCoordinates();

            //position check

            FieldVector3D<DerivativeStructure> pos_DS = finPVC_DS.getPosition();
            double x_DS = pos_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double y_DS = pos_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double z_DS = pos_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);

            //System.out.println(pos_DS.getX().getPartialDerivative(1));

            double x = finPVC_shift.getPosition().getX();
            double y = finPVC_shift.getPosition().getY();
            double z = finPVC_shift.getPosition().getZ();
            Assert.assertEquals(x_DS, x, FastMath.abs(x - pos_DS.getX().getReal()) * 1e-8);
            Assert.assertEquals(y_DS, y, FastMath.abs(y - pos_DS.getY().getReal()) * 1e-8);
            Assert.assertEquals(z_DS, z, FastMath.abs(z - pos_DS.getZ().getReal()) * 1e-8);

            //velocity check

            FieldVector3D<DerivativeStructure> vel_DS = finPVC_DS.getVelocity();
            double vx_DS = vel_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vy_DS = vel_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vz_DS = vel_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vx = finPVC_shift.getVelocity().getX();
            double vy = finPVC_shift.getVelocity().getY();
            double vz = finPVC_shift.getVelocity().getZ();
            Assert.assertEquals(vx_DS, vx, FastMath.abs(vx) * 1e-9);
            Assert.assertEquals(vy_DS, vy, FastMath.abs(vy) * 1e-9);
            Assert.assertEquals(vz_DS, vz, FastMath.abs(vz) * 1e-9);
            //acceleration check

            FieldVector3D<DerivativeStructure> acc_DS = finPVC_DS.getAcceleration();
            double ax_DS = acc_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double ay_DS = acc_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double az_DS = acc_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double ax = finPVC_shift.getAcceleration().getX();
            double ay = finPVC_shift.getAcceleration().getY();
            double az = finPVC_shift.getAcceleration().getZ();
            Assert.assertEquals(ax_DS, ax, FastMath.abs(ax) * 1e-8);
            Assert.assertEquals(ay_DS, ay, FastMath.abs(ay) * 1e-8);
            Assert.assertEquals(az_DS, az, FastMath.abs(az) * 1e-8);
        }
    }

    /**Same test as the previous one but not adding the ForceModel to the NumericalPropagator
    it is a test to validate the previous test.
    (to test if the ForceModel it's actually
    doing something in the Propagator and the FieldPropagator)*/
    @Test
    public void RealFieldExpectErrorTest() {
        DSFactory factory = new DSFactory(6, 0);
        DerivativeStructure a_0 = factory.variable(0, 7e7);
        DerivativeStructure e_0 = factory.variable(1, 0.4);
        DerivativeStructure i_0 = factory.variable(2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngle.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();

        final OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(10.0, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setInitialState(iSR);

        final ConstantThrustManeuver forceModel =  new ConstantThrustManeuver(J2000.toAbsoluteDate().shiftedBy(100), 100.0, 400.0, 300.0, Vector3D.PLUS_K);

        FNP.addForceModel(forceModel);
     //NOT ADDING THE FORCE MODEL TO THE NUMERICAL PROPAGATOR   NP.addForceModel(forceModel);

        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(1000.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getX() - finPVC_R.getPosition().getX()) < FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getY() - finPVC_R.getPosition().getY()) < FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getZ() - finPVC_R.getPosition().getZ()) < FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);
    }

    @Test
    public void testForwardAndBackward() {
        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = FastMath.toRadians(7);
        final double omega = FastMath.toRadians(180);
        final double OMEGA = FastMath.toRadians(261);
        final double lv = 0;

        final double duration = 3653.99;
        final double f = 420;
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        final AttitudeProvider law = new InertialProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                       new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), initDate, mu);
        final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(fireDate, duration, f, isp, Vector3D.PLUS_I);
        Assert.assertEquals(f,   maneuver.getThrust(), 1.0e-10);
        Assert.assertEquals(isp, maneuver.getISP(),    1.0e-10);

        double[][] tol = NumericalPropagator.tolerances(1.0, orbit, OrbitType.KEPLERIAN);
        AdaptiveStepsizeIntegrator integrator1 =
            new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator1.setInitialStepSize(60);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator1);
        propagator1.setInitialState(initialState);
        propagator1.setAttitudeProvider(law);
        propagator1.addForceModel(maneuver);
        final SpacecraftState finalState = propagator1.propagate(fireDate.shiftedBy(3800));
        Assert.assertFalse(maneuver.isFiring(fireDate.shiftedBy(-1.0e-6)));
        Assert.assertTrue(maneuver.isFiring(fireDate.shiftedBy(+1.0e-6)));
        Assert.assertTrue(maneuver.isFiring(fireDate.shiftedBy(0.5 * duration)));
        Assert.assertTrue(maneuver.isFiring(fireDate.shiftedBy(duration - 1.0e-6)));
        Assert.assertFalse(maneuver.isFiring(fireDate.shiftedBy(duration + 1.0e-6)));

        AdaptiveStepsizeIntegrator integrator2 =
                        new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator2.setInitialStepSize(60);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator2);
        propagator2.setInitialState(finalState);
        propagator2.setAttitudeProvider(law);
        propagator2.addForceModel(maneuver);
        final SpacecraftState recoveredState = propagator2.propagate(orbit.getDate());
        final Vector3D refPosition = initialState.getPVCoordinates().getPosition();
        final Vector3D recoveredPosition = recoveredState.getPVCoordinates().getPosition();
        Assert.assertEquals(0.0, Vector3D.distance(refPosition, recoveredPosition), 30.0);
        Assert.assertEquals(initialState.getMass(), recoveredState.getMass(), 1.5e-10);
        Assert.assertFalse(maneuver.isFiring(fireDate.shiftedBy(-1.0e-6)));
        Assert.assertTrue(maneuver.isFiring(fireDate.shiftedBy(+1.0e-6)));
        Assert.assertTrue(maneuver.isFiring(fireDate.shiftedBy(0.5 * duration)));
        Assert.assertTrue(maneuver.isFiring(fireDate.shiftedBy(duration - 1.0e-6)));
        Assert.assertFalse(maneuver.isFiring(fireDate.shiftedBy(duration + 1.0e-6)));

    }

    @Test
    public void testParameterDerivative() {

        // pos-vel (from a ZOOM ephemeris reference)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final ConstantThrustManeuver maneuver =
                new ConstantThrustManeuver(state.getDate().shiftedBy(-10), 100.0, 20.0, 350.0,
                                           Vector3D.PLUS_I, "along-X-");
        maneuver.init(state, state.getDate().shiftedBy(3600.0));

        checkParameterDerivative(state, maneuver, "along-X-thrust",    1.0e-3, 3.0e-14);
        checkParameterDerivative(state, maneuver, "along-X-flow rate", 1.0e-3, 1.0e-15);

    }

    @Test
    public void testInertialManeuver() {
        final double isp = 318;
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
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), initDate, mu);

        final double duration = 3653.99;
        final double f = 420;
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        final AttitudeProvider inertialLaw = new InertialProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));
        final AttitudeProvider lofLaw = new LofOffset(orbit.getFrame(), LOFType.VNC);

        final SpacecraftState initialState =
            new SpacecraftState(orbit, inertialLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final ConstantThrustManeuver maneuverWithoutOverride =
            new ConstantThrustManeuver(fireDate, duration, f, isp, Vector3D.PLUS_I);
        Assert.assertEquals(f,   maneuverWithoutOverride.getThrust(), 1.0e-10);
        Assert.assertEquals(isp, maneuverWithoutOverride.getISP(),    1.0e-10);

        // reference propagation:
        // propagator already uses inertial law
        // maneuver does not need to override it to get an inertial maneuver
        double[][] tol = NumericalPropagator.tolerances(1.0, orbit, OrbitType.KEPLERIAN);
        AdaptiveStepsizeIntegrator integrator1 =
            new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator1.setInitialStepSize(60);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator1);
        propagator1.setInitialState(initialState);
        propagator1.setAttitudeProvider(inertialLaw);
        propagator1.addForceModel(maneuverWithoutOverride);
        final SpacecraftState finalState1 = propagator1.propagate(fireDate.shiftedBy(3800));


        // test propagation:
        // propagator uses a LOF-aligned law
        // maneuver needs to override it to get an inertial maneuver
        final ConstantThrustManeuver maneuverWithOverride =
                        new ConstantThrustManeuver(fireDate, duration, f, isp,
                                                   inertialLaw, Vector3D.PLUS_I);
        Assert.assertEquals(f,   maneuverWithoutOverride.getThrust(), 1.0e-10);
        Assert.assertEquals(isp, maneuverWithoutOverride.getISP(),    1.0e-10);

        AdaptiveStepsizeIntegrator integrator2 =
                        new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator2.setInitialStepSize(60);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator2);
        propagator2.setInitialState(initialState);
        propagator2.setAttitudeProvider(lofLaw);
        propagator2.addForceModel(maneuverWithOverride);
        final SpacecraftState finalState2 = propagator2.propagate(finalState1.getDate());
        Assert.assertThat(finalState2.getPVCoordinates(),
                          OrekitMatchers.pvCloseTo(finalState1.getPVCoordinates(),
                                                   1.0e-10));
        Assert.assertFalse(maneuverWithoutOverride.isFiring(fireDate.shiftedBy(-1.0e-6)));
        Assert.assertTrue(maneuverWithoutOverride.isFiring(fireDate.shiftedBy(+1.0e-6)));
        Assert.assertTrue(maneuverWithoutOverride.isFiring(fireDate.shiftedBy(0.5 * duration)));
        Assert.assertTrue(maneuverWithoutOverride.isFiring(fireDate.shiftedBy(duration - 1.0e-6)));
        Assert.assertFalse(maneuverWithoutOverride.isFiring(fireDate.shiftedBy(duration + 1.0e-6)));

        // intentionally wrong propagation, that will produce a very different state
        // propagator uses LOF attitude,
        // maneuver forget to override it, so maneuver will be LOF-aligned in this case
        AdaptiveStepsizeIntegrator integrator3 =
                        new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator3.setInitialStepSize(60);
        final NumericalPropagator propagator3 = new NumericalPropagator(integrator3);
        propagator3.setInitialState(initialState);
        propagator3.setAttitudeProvider(lofLaw);
        propagator3.addForceModel(maneuverWithoutOverride);
        final SpacecraftState finalState3 = propagator3.propagate(finalState1.getDate());
        Assert.assertEquals(345859.0,
                           Vector3D.distance(finalState1.getPVCoordinates().getPosition(),
                                             finalState3.getPVCoordinates().getPosition()),
                           1.0);
    }

    @Test
    public void testStopInMiddle() {
        final double isp = 318;
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
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), initDate, mu);

        final double duration = 3653.99;
        final double f = 420;
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        final AttitudeProvider inertialLaw = new InertialProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));

        final SpacecraftState initialState =
            new SpacecraftState(orbit, inertialLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(fireDate, duration, f, isp, Vector3D.PLUS_I);
        Assert.assertEquals(f,   maneuver.getThrust(), 1.0e-10);
        Assert.assertEquals(isp, maneuver.getISP(),    1.0e-10);

        // before events have been encountered, the maneuver is not yet allowed to generate non zero acceleration
        for (double dt = 0 ; dt < fireDate.durationFrom(initDate) + duration + 100; dt += 0.1) {
            Assert.assertFalse(maneuver.isFiring(initDate.shiftedBy(dt)));
            Assert.assertEquals(0.0,
                                maneuver.acceleration(initialState.shiftedBy(dt), maneuver.getParameters()).getNorm(),
                                1.0e-15);
        }

        // reference propagation:
        // propagator already uses inertial law
        // maneuver does not need to override it to get an inertial maneuver
        double[][] tol = NumericalPropagator.tolerances(1.0, orbit, OrbitType.KEPLERIAN);
        AdaptiveStepsizeIntegrator integrator1 =
            new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator1.setInitialStepSize(60);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator1);
        propagator1.setInitialState(initialState);
        propagator1.setAttitudeProvider(inertialLaw);
        propagator1.addForceModel(maneuver);
        final SpacecraftState middleState = propagator1.propagate(fireDate.shiftedBy(0.5 * duration));

        Assert.assertFalse(maneuver.isFiring(initialState));
        Assert.assertEquals(0.0,
                            maneuver.acceleration(initialState, maneuver.getParameters()).getNorm(),
                            1.0e-15);
        Assert.assertTrue(maneuver.isFiring(middleState));
        Assert.assertEquals(0.186340263,
                            maneuver.acceleration(middleState, maneuver.getParameters()).getNorm(),
                            1.0e-9);
        Assert.assertTrue(maneuver.isFiring(middleState.shiftedBy(3 * duration)));
        Assert.assertEquals(0.186340263,
                            maneuver.acceleration(middleState.shiftedBy(3 * duration), maneuver.getParameters()).getNorm(),
                            1.0e-9);
        

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");

        // Body mu
        mu = 3.9860047e14;

    }

}
