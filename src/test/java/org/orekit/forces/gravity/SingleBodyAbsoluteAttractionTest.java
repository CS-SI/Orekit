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
package org.orekit.forces.gravity;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
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

public class SingleBodyAbsoluteAttractionTest extends AbstractLegacyForceModelTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel, final AbsoluteDate date,
                                                                         final Frame frame,
                                                                         final FieldVector3D<DerivativeStructure> position,
                                                                         final FieldVector3D<DerivativeStructure> velocity,
                                                                         final FieldRotation<DerivativeStructure> rotation,
                                                                         final DerivativeStructure mass) {

        try {
            java.lang.reflect.Field bodyField = SingleBodyAbsoluteAttraction.class.getDeclaredField("body");
            bodyField.setAccessible(true);
            CelestialBody body = (CelestialBody) bodyField.get(forceModel);
            double gm = forceModel.getParameterDriver(body.getName() + SingleBodyAbsoluteAttraction.ATTRACTION_COEFFICIENT_SUFFIX).getValue();

            // compute bodies separation vectors and squared norm
            final Vector3D centralToBody    = body.getPVCoordinates(date, frame).getPosition();
            final FieldVector3D<DerivativeStructure> satToBody = position.subtract(centralToBody).negate();
            final DerivativeStructure r2Sat = satToBody.getNormSq();

            // compute relative acceleration
            final FieldVector3D<DerivativeStructure> satAcc =
                    new FieldVector3D<>(r2Sat.sqrt().multiply(r2Sat).reciprocal().multiply(gm), satToBody);
            return satAcc;


        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }

    }

    @Test
    public void RealFieldTest() {
        DSFactory factory = new DSFactory(6, 5);
        DerivativeStructure a_0 = factory.variable(0, 7e7);
        DerivativeStructure e_0 = factory.variable(1, 0.4);
        DerivativeStructure i_0 = factory.variable(2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);
        DerivativeStructure mu  = factory.constant(Constants.EIGEN5C_EARTH_MU);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngle.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 mu);

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();
        OrbitType type = OrbitType.KEPLERIAN;
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

        final SingleBodyAbsoluteAttraction forceModel = new SingleBodyAbsoluteAttraction(CelestialBodyFactory.getSun());

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
        double maxP = 0;
        double maxV = 0;
        double maxA = 0;
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
            double x = finPVC_shift.getPosition().getX();
            double y = finPVC_shift.getPosition().getY();
            double z = finPVC_shift.getPosition().getZ();
            maxP = FastMath.max(maxP, FastMath.abs((x_DS - x) / (x - pos_DS.getX().getReal())));
            maxP = FastMath.max(maxP, FastMath.abs((y_DS - y) / (y - pos_DS.getY().getReal())));
            maxP = FastMath.max(maxP, FastMath.abs((z_DS - z) / (z - pos_DS.getZ().getReal())));

            // velocity check
            FieldVector3D<DerivativeStructure> vel_DS = finPVC_DS.getVelocity();
            double vx_DS = vel_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vy_DS = vel_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vz_DS = vel_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vx = finPVC_shift.getVelocity().getX();
            double vy = finPVC_shift.getVelocity().getY();
            double vz = finPVC_shift.getVelocity().getZ();
            maxV = FastMath.max(maxV, FastMath.abs((vx_DS - vx) / vx));
            maxV = FastMath.max(maxV, FastMath.abs((vy_DS - vy) / vy));
            maxV = FastMath.max(maxV, FastMath.abs((vz_DS - vz) / vz));

            // acceleration check
            FieldVector3D<DerivativeStructure> acc_DS = finPVC_DS.getAcceleration();
            double ax_DS = acc_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double ay_DS = acc_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double az_DS = acc_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double ax = finPVC_shift.getAcceleration().getX();
            double ay = finPVC_shift.getAcceleration().getY();
            double az = finPVC_shift.getAcceleration().getZ();
            maxA = FastMath.max(maxA, FastMath.abs((ax_DS - ax) / ax));
            maxA = FastMath.max(maxA, FastMath.abs((ay_DS - ay) / ay));
            maxA = FastMath.max(maxA, FastMath.abs((az_DS - az) / az));
        }
        Assert.assertEquals(0, maxP, 4.5e-9);
        Assert.assertEquals(0, maxV, 2.1e-10);
        Assert.assertEquals(0, maxA, 6.5e-10);

    }

    @Test
    public void RealFieldExpectErrorTest() {
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
        OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, FKO.toOrbit(), type);


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

        final SingleBodyAbsoluteAttraction forceModel = new SingleBodyAbsoluteAttraction(CelestialBodyFactory.getSun());

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
    public void testParameterDerivative() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final SingleBodyAbsoluteAttraction forceModel = new SingleBodyAbsoluteAttraction(moon);
        Assert.assertTrue(forceModel.dependsOnPositionOnly());
        final String name = moon.getName() + SingleBodyAbsoluteAttraction.ATTRACTION_COEFFICIENT_SUFFIX;
        checkParameterDerivative(state, forceModel, name, 1.0, 7.0e-15);

    }

    @Test
    public void testJacobianVs80Implementation() {
        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final SingleBodyAbsoluteAttraction forceModel = new SingleBodyAbsoluteAttraction(moon);
        checkStateJacobianVs80Implementation(new SpacecraftState(orbit), forceModel,
                                             new LofOffset(orbit.getFrame(), LOFType.VVLH),
                                             1.0e-50, false);
    }

    @Test
    public void testGlobalStateJacobian()
        {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);

        NumericalPropagator propagator =
                new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                       tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final SingleBodyAbsoluteAttraction forceModel = new SingleBodyAbsoluteAttraction(moon);
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e4, tolerances[0], 2.0e-9);

    }

}
