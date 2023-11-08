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
package org.orekit.forces.inertia;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class InertialForcesTest extends AbstractLegacyForceModelTest {

    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel, final FieldSpacecraftState<DerivativeStructure> state) {
        try {
            final FieldVector3D<DerivativeStructure> position = state.getPVCoordinates().getPosition();
            final FieldVector3D<DerivativeStructure> velocity = state.getPVCoordinates().getVelocity();
            java.lang.reflect.Field refInertialFrameField = InertialForces.class.getDeclaredField("referenceInertialFrame");
            refInertialFrameField.setAccessible(true);
            Frame refInertialFrame = (Frame) refInertialFrameField.get(forceModel);

            final FieldTransform<DerivativeStructure> inertToStateFrame = refInertialFrame.getTransformTo(state.getFrame(),
                                                                                                          state.getDate());
            final FieldVector3D<DerivativeStructure>  a1                = inertToStateFrame.getCartesian().getAcceleration();
            final FieldRotation<DerivativeStructure>  r1                = inertToStateFrame.getAngular().getRotation();
            final FieldVector3D<DerivativeStructure>  o1                = inertToStateFrame.getAngular().getRotationRate();
            final FieldVector3D<DerivativeStructure>  oDot1             = inertToStateFrame.getAngular().getRotationAcceleration();

            final FieldVector3D<DerivativeStructure>  p2                = position;
            final FieldVector3D<DerivativeStructure>  v2                = velocity;

            final FieldVector3D<DerivativeStructure> crossCrossP        = FieldVector3D.crossProduct(o1,    FieldVector3D.crossProduct(o1, p2));
            final FieldVector3D<DerivativeStructure> crossV             = FieldVector3D.crossProduct(o1,    v2);
            final FieldVector3D<DerivativeStructure> crossDotP          = FieldVector3D.crossProduct(oDot1, p2);

            // we intentionally DON'T include s.getPVCoordinates().getAcceleration()
            // because we want only the coupling effect of the frames transforms
            return r1.applyTo(a1).subtract(new FieldVector3D<>(2, crossV, 1, crossCrossP, 1, crossDotP));

        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    @Override
    protected FieldVector3D<Gradient> accelerationDerivativesGradient(final ForceModel forceModel, final FieldSpacecraftState<Gradient> state) {
        try {
            final FieldVector3D<Gradient> position = state.getPVCoordinates().getPosition();
            final FieldVector3D<Gradient> velocity = state.getPVCoordinates().getVelocity();
            java.lang.reflect.Field refInertialFrameField = InertialForces.class.getDeclaredField("referenceInertialFrame");
            refInertialFrameField.setAccessible(true);
            Frame refInertialFrame = (Frame) refInertialFrameField.get(forceModel);

            final FieldTransform<Gradient> inertToStateFrame = refInertialFrame.getTransformTo(state.getFrame(),
                                                                                               state.getDate());
            final FieldVector3D<Gradient>  a1                = inertToStateFrame.getCartesian().getAcceleration();
            final FieldRotation<Gradient>  r1                = inertToStateFrame.getAngular().getRotation();
            final FieldVector3D<Gradient>  o1                = inertToStateFrame.getAngular().getRotationRate();
            final FieldVector3D<Gradient>  oDot1             = inertToStateFrame.getAngular().getRotationAcceleration();

            final FieldVector3D<Gradient>  p2                = position;
            final FieldVector3D<Gradient>  v2                = velocity;

            final FieldVector3D<Gradient> crossCrossP        = FieldVector3D.crossProduct(o1,    FieldVector3D.crossProduct(o1, p2));
            final FieldVector3D<Gradient> crossV             = FieldVector3D.crossProduct(o1,    v2);
            final FieldVector3D<Gradient> crossDotP          = FieldVector3D.crossProduct(oDot1, p2);

            // we intentionally DON'T include s.getPVCoordinates().getAcceleration()
            // because we want only the coupling effect of the frames transforms
            return r1.applyTo(a1).subtract(new FieldVector3D<>(2, crossV, 1, crossCrossP, 1, crossDotP));

        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
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
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        final AbsolutePVCoordinates pva = new AbsolutePVCoordinates(orbit.getFrame(), orbit.getPVCoordinates());
        final InertialForces forceModel = new InertialForces(pva.getFrame());
        Assertions.assertFalse(forceModel.dependsOnPositionOnly());
        checkStateJacobianVs80Implementation(new SpacecraftState(pva), forceModel,
                                             Utils.defaultLaw(),
                                             1.0e-50, false);
    }

    @Test
    public void testJacobianVs80ImplementationGradient() {
        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        final AbsolutePVCoordinates pva = new AbsolutePVCoordinates(orbit.getFrame(), orbit.getPVCoordinates());
        final InertialForces forceModel = new InertialForces(pva.getFrame());
        Assertions.assertFalse(forceModel.dependsOnPositionOnly());
        checkStateJacobianVs80ImplementationGradient(new SpacecraftState(pva), forceModel,
                                             Utils.defaultLaw(),
                                             1.0e-50, false);
    }

    @Test
    public void RealFieldTest() {
        DSFactory factory = new DSFactory(6, 5);
        DerivativeStructure fpx = factory.variable(0, 0.8);
        DerivativeStructure fpy = factory.variable(1, 0.2);
        DerivativeStructure fpz = factory.variable(2, 0.0);
        DerivativeStructure fvx = factory.variable(3, 0.0);
        DerivativeStructure fvy = factory.variable(4, 0.0);
        DerivativeStructure fvz = factory.variable(5, 0.1);

        final FieldPVCoordinates<DerivativeStructure> initialConditions =
                        new FieldPVCoordinates<>(new FieldVector3D<>(fpx, fpy, fpz),
                                          new FieldVector3D<>(fvx, fvy, fvz));

        final double minStep = 0.00001;
        final double maxstep = 3600.0;

        Field<DerivativeStructure> field = fpx.getField();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        // PVCoordinates linked to a Frame and a Date
        final FieldAbsolutePVCoordinates<DerivativeStructure> initialAbsPV =
            new FieldAbsolutePVCoordinates<>(EME, J2000, initialConditions);


        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(initialAbsPV);

        SpacecraftState iSR = initialState.toSpacecraftState();

        final double positionTolerance = 0.01;
        final double velocityTolerance = 0.01;
        final double massTolerance = 1.0e-6;
        final double[] vecAbsoluteTolerances =
            {
                positionTolerance, positionTolerance, positionTolerance,
                velocityTolerance, velocityTolerance, velocityTolerance,
                massTolerance
            };
        final double[] vecRelativeTolerances =
            new double[vecAbsoluteTolerances.length];

        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field,minStep, maxstep,
                                        vecAbsoluteTolerances,
                                        vecRelativeTolerances);
        integrator.setInitialStepSize(60);
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(minStep, maxstep,
                                                       vecAbsoluteTolerances,
                                                       vecRelativeTolerances);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(null);
        FNP.setIgnoreCentralAttraction(true);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(null);
        NP.setIgnoreCentralAttraction(true);
        NP.setInitialState(iSR);

        final InertialForces forceModel = new InertialForces(EME);

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);


        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(1000.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assertions.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getX(), finPVC_R.getPosition().getX(), FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assertions.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getY(), finPVC_R.getPosition().getY(), FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assertions.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getZ(), finPVC_R.getPosition().getZ(), FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);

        long number = 23091991;
        RandomGenerator RG = new Well19937a(number);
        GaussianRandomGenerator NGG = new GaussianRandomGenerator(RG);
        UncorrelatedRandomVectorGenerator URVG = new UncorrelatedRandomVectorGenerator(new double[] {0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.0 },
                                                                                       new double[] {1e3, 0.01, 0.01, 0.01, 0.01, 0.01},
                                                                                       NGG);
        double px_R = fpx.getReal();
        double py_R = fpy.getReal();
        double pz_R = fpz.getReal();
        double vx_R = fvx.getReal();
        double vy_R = fvy.getReal();
        double vz_R = fvz.getReal();
        double maxP = 0;
        double maxV = 0;
        double maxA = 0;
        for (int ii = 0; ii < 1; ii++){
            double[] rand_next = URVG.nextVector();
            double px_shift = px_R + rand_next[0];
            double py_shift = py_R + rand_next[1];
            double pz_shift = pz_R + rand_next[2];
            double vx_shift = vx_R + rand_next[3];
            double vy_shift = vy_R + rand_next[4];
            double vz_shift = vz_R + rand_next[5];

            final PVCoordinates shiftedConditions =
                            new PVCoordinates(new Vector3D(px_shift, py_shift, pz_shift),
                                              new Vector3D(vx_shift, vy_shift, vz_shift));
            // PVCoordinates linked to a Frame and a Date
            final AbsolutePVCoordinates shiftedAbsPV =
                new AbsolutePVCoordinates(EME, J2000.toAbsoluteDate(), shiftedConditions);

            SpacecraftState shift_iSR = new SpacecraftState(shiftedAbsPV);



            NumericalPropagator shift_NP = new NumericalPropagator(RIntegrator);

            shift_NP.setInitialState(shift_iSR);

            shift_NP.setOrbitType(null);
            shift_NP.setIgnoreCentralAttraction(true);
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
            if (ax != 0 || ay !=0 || az != 0) {
                maxA = FastMath.max(maxA, FastMath.abs((ax_DS - ax) / ax));
                maxA = FastMath.max(maxA, FastMath.abs((ay_DS - ay) / ay));
                maxA = FastMath.max(maxA, FastMath.abs((az_DS - az) / az));
            } else {
                maxA = 0;
            }
        }
        Assertions.assertEquals(0, maxP, 1.1e-14);
        Assertions.assertEquals(0, maxV, 1.0e-15);
        Assertions.assertEquals(0, maxA, 1.0e-15);
    }

    @Test
    public void testNoParametersDrivers() {
        try {
            // initialization
            AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                                 new TimeComponents(13, 59, 27.816),
                                                 TimeScalesFactory.getUTC());
            double i     = FastMath.toRadians(98.7);
            double omega = FastMath.toRadians(93.0);
            double OMEGA = FastMath.toRadians(15.0 * 22.5);
            Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                             0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                             Constants.EIGEN5C_EARTH_MU);
            final AbsolutePVCoordinates pva = new AbsolutePVCoordinates(orbit.getFrame(), orbit.getPVCoordinates());
            final InertialForces forceModel = new InertialForces(pva.getFrame());
            forceModel.getParameterDriver(" ");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oe.getSpecifier());
        }
    }

    @Test
    public void testNonInertialFrame() {
        try {
            // ECEF frame
            final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            // Initialize inertial force with a non-inertial frame
            final InertialForces force = new InertialForces(ecef);
            force.getParametersDrivers();
        } catch (OrekitIllegalArgumentException oe) {
            Assertions.assertEquals(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_AS_REFERENCE_FOR_INERTIAL_FORCES,
                                oe.getSpecifier());
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
