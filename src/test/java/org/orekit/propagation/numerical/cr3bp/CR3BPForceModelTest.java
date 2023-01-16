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
package org.orekit.propagation.numerical.cr3bp;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
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
import org.orekit.bodies.CR3BPFactory;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

public class CR3BPForceModelTest {

    private CR3BPSystem syst;

    @Test
    public void testModel() {
                
        final double mu = new CR3BPForceModel(syst).getParameters(new AbsoluteDate())[0];
        Assertions.assertEquals(0.0121, mu, 1E-3);
        
     // Time settings
        final AbsoluteDate initialDate =
            new AbsoluteDate(1996, 06, 25, 0, 0, 00.000,
                             TimeScalesFactory.getUTC());

        final PVCoordinates initialConditions =
            new PVCoordinates(new Vector3D(0.8, 0.2, 0.0),
                              new Vector3D(0.0, 0.0, 0.1));
        //final Frame Frame = syst.getRotatingFrame();
        final Frame Frame = FramesFactory.getGCRF();
        final AbsolutePVCoordinates initialAbsPV =
            new AbsolutePVCoordinates(Frame, initialDate, initialConditions);

        // Creating the initial spacecraftstate that will be given to the
        // propagator
        final SpacecraftState initialState = new SpacecraftState(initialAbsPV);

        // Integration parameters
        // These parameters are used for the Dormand-Prince integrator, a
        // variable step integrator,
        // these limits prevent the integrator to spend too much time when the
        // equations are too stiff,
        // as well as the reverse situation.
        final double minStep = 0.00001;
        final double maxstep = 3600.0;
        final double integrationTime = 5;

        // tolerances for integrators
        // Used by the integrator to estimate its variable integration step
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

        // Defining the numerical integrator that will be used by the propagator
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(minStep, maxstep,
                                           vecAbsoluteTolerances,
                                           vecRelativeTolerances);

        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(null);
        propagator.setIgnoreCentralAttraction(true);
        propagator.addForceModel(new CR3BPForceModel(syst));
        propagator.setInitialState(initialState);
        propagator.clearStepHandlers();
        final SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(integrationTime));

        Assertions.assertNotEquals(initialState.getPosition().getX(), finalState.getPosition().getX(), 1E-2);
    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
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

        Field<DerivativeStructure> field = fpx.getField();
        DerivativeStructure zero = field.getZero();
        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        //final Frame frame = syst.getRotatingFrame();
        final Frame frame = FramesFactory.getGCRF();

        // PVCoordinates linked to a Frame and a Date
        final FieldAbsolutePVCoordinates<DerivativeStructure> initialAbsPV =
            new FieldAbsolutePVCoordinates<>(frame, J2000, initialConditions);


        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(initialAbsPV);

        SpacecraftState iSR = initialState.toSpacecraftState();

        ClassicalRungeKuttaFieldIntegrator<DerivativeStructure> integrator = new ClassicalRungeKuttaFieldIntegrator<>(field, zero.add(1.0));

        ClassicalRungeKuttaIntegrator RIntegrator = new ClassicalRungeKuttaIntegrator(1.0);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(null);
        FNP.setIgnoreCentralAttraction(true);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(null);
        NP.setIgnoreCentralAttraction(true);
        NP.setInitialState(iSR);

        final CR3BPForceModel forceModel = new CR3BPForceModel(syst);

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);


        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(1.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assertions.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getX(), finPVC_R.getPosition().getX(), FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assertions.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getY(), finPVC_R.getPosition().getY(), FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assertions.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getZ(), finPVC_R.getPosition().getZ(), FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);
        Assertions.assertTrue(forceModel.dependsOnPositionOnly());
        long number = 23091991;
        RandomGenerator RG = new Well19937a(number);
        GaussianRandomGenerator NGG = new GaussianRandomGenerator(RG);
        UncorrelatedRandomVectorGenerator URVG = new UncorrelatedRandomVectorGenerator(new double[] {0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.0 },
                                                                                       new double[] {0.001, 0.001, 0.001, 0.001, 0.001, 0.001},
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
                new AbsolutePVCoordinates(frame, J2000.toAbsoluteDate(), shiftedConditions);

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
        Assertions.assertEquals(0, maxP, 4.2e-11);
        Assertions.assertEquals(0, maxV, 1.4e-12);
        Assertions.assertEquals(0, maxA, 8.5e-12);
    }



    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");

        this.syst = CR3BPFactory.getEarthMoonCR3BP();
    }
}
