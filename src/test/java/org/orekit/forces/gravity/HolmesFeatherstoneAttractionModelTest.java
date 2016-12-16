/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.dfp.Dfp;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.UncorrelatedRandomVectorGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class HolmesFeatherstoneAttractionModelTest extends AbstractForceModelTest {

    @Test
    public void testRelativeNumericPrecision() throws OrekitException {

        // this test is similar in spirit to section 4.2 of Holmes and Featherstone paper,
        // but reduced to lower degree since our reference implementation is MUCH slower
        // than the one used in the paper (Clenshaw method)
        int max = 50;
        NormalizedSphericalHarmonicsProvider provider = new GleasonProvider(max, max);
        HolmesFeatherstoneAttractionModel model =
                new HolmesFeatherstoneAttractionModel(itrf, provider);

        // Note that despite it uses adjustable high accuracy, the reference model
        // uses unstable formulas and hence loses lots of digits near poles.
        // This implies that if we still want about 16 digits near the poles, we
        // need to ask for at least 30 digits in computation. Setting for example
        // the following to 28 digits makes the test fail as the relative errors
        // raise to about 10^-12 near North pole and near 10^-11 near South pole.
        // The reason for this is that the reference becomes less accurate than
        // the model we are testing!
        int digits = 30;
        ReferenceFieldModel refModel = new ReferenceFieldModel(provider, digits);

        double r = 1.25;
        for (double theta = 0.01; theta < 3.14; theta += 0.1) {
            Vector3D position = new Vector3D(r * FastMath.sin(theta), 0.0, r * FastMath.cos(theta));
            Dfp refValue = refModel.nonCentralPart(null, position);
            double value = model.nonCentralPart(null, position);
            double relativeError = error(refValue, value).divide(refValue).toDouble();
            Assert.assertEquals(0, relativeError, 7.0e-15);
        }

    }

    @Test
    public void testValue() throws OrekitException {

        int max = 50;
        NormalizedSphericalHarmonicsProvider provider = new GleasonProvider(max, max);
        HolmesFeatherstoneAttractionModel model =
                new HolmesFeatherstoneAttractionModel(itrf, provider);

        double r = 1.25;
        for (double lambda = 0; lambda < 2 * FastMath.PI; lambda += 0.5) {
            for (double theta = 0.05; theta < 3.11; theta += 0.03) {
                Vector3D position = new Vector3D(r * FastMath.sin(theta) * FastMath.cos(lambda),
                                                 r * FastMath.sin(theta) * FastMath.sin(lambda),
                                                 r * FastMath.cos(theta));
                double refValue = provider.getMu() / position.getNorm() +
                                  model.nonCentralPart(AbsoluteDate.GPS_EPOCH, position);
                double  value   = model.value(AbsoluteDate.GPS_EPOCH, position);
                Assert.assertEquals(refValue, value, 1.0e-15 * FastMath.abs(refValue));
            }
        }

    }
    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to 
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldTest() throws OrekitException{
        DerivativeStructure a_0 = new DerivativeStructure(6, 4, 0, 7201009.7124401);
        DerivativeStructure e_0 = new DerivativeStructure(6, 4, 1, 1e-3);
        DerivativeStructure i_0 = new DerivativeStructure(6, 4, 2, 98.7 * FastMath.PI / 180);
        DerivativeStructure R_0 = new DerivativeStructure(6, 4, 3, 15.0 * 22.5 * FastMath.PI / 180);
        DerivativeStructure O_0 = new DerivativeStructure(6, 4, 4, 93.0 * FastMath.PI / 180);
        DerivativeStructure n_0 = new DerivativeStructure(6, 4, 5, 0.1);
        
        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();
        
        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<DerivativeStructure>(field);
        
        Frame EME = FramesFactory.getEME2000();
        
        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<DerivativeStructure>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                                    PositionAngle.MEAN,
                                                                                                    EME,
                                                                                                    J2000,
                                                                                                    Constants.EIGEN5C_EARTH_MU);
        
        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<DerivativeStructure>(FKO); 
        
        SpacecraftState iSR = initialState.toSpacecraftState();
        
        double[][] tolerance = NumericalPropagator.tolerances(0.001, FKO.toOrbit(), OrbitType.KEPLERIAN);
        
        
        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<DerivativeStructure>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);
                
        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<DerivativeStructure>(field, integrator);
        FNP.setInitialState(initialState);
                
        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setInitialState(iSR);
        
        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = normalizedC20;
        double[][] s = new double[3][1];
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(6378136.460, mu,
                                                                                                  TideSystem.UNKNOWN,
                                                                                                  c, s);
        HolmesFeatherstoneAttractionModel forceModel =
                        new HolmesFeatherstoneAttractionModel(itrf, provider);
        
        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);
        
        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(1005.);
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
                                                                                       new double[] {1e1, 0.001, 0.001, 0.001, 0.001, 0.001}, 
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
            double x_DS = pos_DS.getX().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double y_DS = pos_DS.getY().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);                                                                               
            double z_DS = pos_DS.getZ().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            

            double x = finPVC_shift.getPosition().getX();
            double y = finPVC_shift.getPosition().getY();
            double z = finPVC_shift.getPosition().getZ();
            Assert.assertEquals(x_DS, x, FastMath.abs(x - pos_DS.getX().getReal()) * 1e-8);
            Assert.assertEquals(y_DS, y, FastMath.abs(y - pos_DS.getY().getReal()) * 1e-8);
            Assert.assertEquals(z_DS, z, FastMath.abs(z - pos_DS.getZ().getReal()) * 1e-8);
            
            //velocity check
            
            FieldVector3D<DerivativeStructure> vel_DS = finPVC_DS.getVelocity();
            double vx_DS = vel_DS.getX().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double vy_DS = vel_DS.getY().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);                                                                               
            double vz_DS = vel_DS.getZ().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double vx = finPVC_shift.getVelocity().getX();
            double vy = finPVC_shift.getVelocity().getY();
            double vz = finPVC_shift.getVelocity().getZ();
            Assert.assertEquals(vx_DS, vx, FastMath.abs(vx) * 1e-9);
            Assert.assertEquals(vy_DS, vy, FastMath.abs(vy) * 1e-9);
            Assert.assertEquals(vz_DS, vz, FastMath.abs(vz) * 1e-9);
            //acceleration check
            
            FieldVector3D<DerivativeStructure> acc_DS = finPVC_DS.getAcceleration();
            double ax_DS = acc_DS.getX().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double ay_DS = acc_DS.getY().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);                                                                               
            double az_DS = acc_DS.getZ().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double ax = finPVC_shift.getAcceleration().getX();
            double ay = finPVC_shift.getAcceleration().getY();
            double az = finPVC_shift.getAcceleration().getZ();
            Assert.assertEquals(ax_DS, ax, FastMath.abs(ax) * 1e-9);
            Assert.assertEquals(ay_DS, ay, FastMath.abs(ay) * 1e-9);
            Assert.assertEquals(az_DS, az, FastMath.abs(az) * 1e-9);
        }
    }

    /**Same test as the previous one but not adding the ForceModel to the NumericalPropagator
    it is a test to validate the previous test. 
    (to test if the ForceModel it's actually
    doing something in the Propagator and the FieldPropagator)*/
    @Test
    public void RealFieldExpectErrorTest() throws OrekitException{
        DerivativeStructure a_0 = new DerivativeStructure(6, 0, 0, 7201009.7124401);
        DerivativeStructure e_0 = new DerivativeStructure(6, 0, 1, 1e-3);
        DerivativeStructure i_0 = new DerivativeStructure(6, 0, 2, 98.7 * FastMath.PI / 180);
        DerivativeStructure R_0 = new DerivativeStructure(6, 0, 3, 15.0 * 22.5 * FastMath.PI / 180);
        DerivativeStructure O_0 = new DerivativeStructure(6, 0, 4, 93.0 * FastMath.PI / 180);
        DerivativeStructure n_0 = new DerivativeStructure(6, 0, 5, 0.1);
        
        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();
        
        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<DerivativeStructure>(field);

        Frame EME = FramesFactory.getEME2000();
        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<DerivativeStructure>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                                    PositionAngle.MEAN,
                                                                                                    EME,
                                                                                                    J2000,
                                                                                                    Constants.EIGEN5C_EARTH_MU);
        
        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<DerivativeStructure>(FKO); 
        
        SpacecraftState iSR = initialState.toSpacecraftState();
        
        double[][] tolerance = NumericalPropagator.tolerances(0.001, FKO.toOrbit(), OrbitType.KEPLERIAN);
        
        
        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<DerivativeStructure>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);
                
        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<DerivativeStructure>(field, integrator);
        FNP.setInitialState(initialState);
                
        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setInitialState(iSR);
        
        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = normalizedC20;
        double[][] s = new double[3][1];
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(6378136.460, mu,
                                                                                                  TideSystem.UNKNOWN,
                                                                                                  c, s);
        HolmesFeatherstoneAttractionModel forceModel =
                        new HolmesFeatherstoneAttractionModel(itrf, provider);
        
        //FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);
        
        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(100.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getX() - finPVC_R.getPosition().getX()) < FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getY() - finPVC_R.getPosition().getY()) < FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getZ() - finPVC_R.getPosition().getZ()) < FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);
    }
    @Test
    public void testGradient() throws OrekitException {

        int max = 50;
        NormalizedSphericalHarmonicsProvider provider = new GleasonProvider(max, max);
        HolmesFeatherstoneAttractionModel model =
                new HolmesFeatherstoneAttractionModel(itrf, provider);

        double r = 1.25;
        for (double lambda = 0; lambda < 2 * FastMath.PI; lambda += 0.5) {
            for (double theta = 0.05; theta < 3.11; theta += 0.03) {
                Vector3D position = new Vector3D(r * FastMath.sin(theta) * FastMath.cos(lambda),
                                                 r * FastMath.sin(theta) * FastMath.sin(lambda),
                                                 r * FastMath.cos(theta));
                double[] refGradient = gradient(model, null, position, 1.0e-3);
                double norm  = FastMath.sqrt(refGradient[0] * refGradient[0] +
                                             refGradient[1] * refGradient[1] +
                                             refGradient[2] * refGradient[2]);
                double[] gradient = model.gradient(null, position);
                double errorX = refGradient[0] - gradient[0];
                double errorY = refGradient[1] - gradient[1];
                double errorZ = refGradient[2] - gradient[2];
                double relativeError = FastMath.sqrt(errorX * errorX + errorY * errorY + errorZ * errorZ) /
                                       norm;
                Assert.assertEquals(0, relativeError, 3.0e-12);
            }
        }

    }

    @Test
    public void testHessian() throws OrekitException {

        int max = 50;
        NormalizedSphericalHarmonicsProvider provider = new GleasonProvider(max, max);
        HolmesFeatherstoneAttractionModel model =
                new HolmesFeatherstoneAttractionModel(itrf, provider);

        double r = 1.25;
        for (double lambda = 0; lambda < 2 * FastMath.PI; lambda += 0.5) {
            for (double theta = 0.05; theta < 3.11; theta += 0.03) {
                Vector3D position = new Vector3D(r * FastMath.sin(theta) * FastMath.cos(lambda),
                                                 r * FastMath.sin(theta) * FastMath.sin(lambda),
                                                 r * FastMath.cos(theta));
                double[][] refHessian = hessian(model, null, position, 1.0e-3);
                double[][] hessian = model.gradientHessian(null, position).getHessian();
                double normH2 = 0;
                double normE2 = 0;
                for (int i = 0; i < 3; ++i) {
                    for (int j = 0; j < 3; ++j) {
                        double error = refHessian[i][j] - hessian[i][j];
                        normH2 += refHessian[i][j] * refHessian[i][j];
                        normE2 += error * error;
                    }
                }
                Assert.assertEquals(0, FastMath.sqrt(normE2 / normH2), 5.0e-12);
            }
        }

    }

    private Dfp error(Dfp refValue, double value) {
        return refValue.getField().newDfp(value).subtract(refValue);
    }

    private double[] gradient(final HolmesFeatherstoneAttractionModel model,
                              final AbsoluteDate date, final Vector3D position, final double h)
        throws OrekitException {
        return new double[] {
            differential8(model.nonCentralPart(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_I))),
                          model.nonCentralPart(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_I))),
                          model.nonCentralPart(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_I))),
                          model.nonCentralPart(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_I))),
                          model.nonCentralPart(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_I))),
                          model.nonCentralPart(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_I))),
                          model.nonCentralPart(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_I))),
                          model.nonCentralPart(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_I))),
                          h),
            differential8(model.nonCentralPart(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_J))),
                          model.nonCentralPart(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_J))),
                          model.nonCentralPart(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_J))),
                          model.nonCentralPart(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_J))),
                          model.nonCentralPart(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_J))),
                          model.nonCentralPart(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_J))),
                          model.nonCentralPart(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_J))),
                          model.nonCentralPart(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_J))),
                          h),
            differential8(model.nonCentralPart(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_K))),
                          model.nonCentralPart(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_K))),
                          model.nonCentralPart(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_K))),
                          model.nonCentralPart(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_K))),
                          model.nonCentralPart(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_K))),
                          model.nonCentralPart(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_K))),
                          model.nonCentralPart(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_K))),
                          model.nonCentralPart(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_K))),
                          h)
        };
    }

    private double[][] hessian(final HolmesFeatherstoneAttractionModel model,
                               final AbsoluteDate date, final Vector3D position, final double h)
        throws OrekitException {
        return new double[][] {
            differential8(model.gradient(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_I))),
                          model.gradient(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_I))),
                          model.gradient(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_I))),
                          model.gradient(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_I))),
                          model.gradient(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_I))),
                          model.gradient(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_I))),
                          model.gradient(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_I))),
                          model.gradient(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_I))),
                          h),
            differential8(model.gradient(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_J))),
                          model.gradient(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_J))),
                          model.gradient(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_J))),
                          model.gradient(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_J))),
                          model.gradient(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_J))),
                          model.gradient(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_J))),
                          model.gradient(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_J))),
                          model.gradient(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_J))),
                          h),
            differential8(model.gradient(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_K))),
                          model.gradient(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_K))),
                          model.gradient(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_K))),
                          model.gradient(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_K))),
                          model.gradient(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_K))),
                          model.gradient(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_K))),
                          model.gradient(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_K))),
                          model.gradient(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_K))),
                          h)
        };
    }

    /** Dummy provider for testing purposes.
     * <p>
     * This providers correspond to the testing regime used in the
     * Holmes and Featherstone paper, who credit it to D. M. Gleason.
     * </p>
     */
    private static class GleasonProvider implements NormalizedSphericalHarmonicsProvider {

        private final int degree;
        private final int order;

        public GleasonProvider(int degree, int order) {
            this.degree = degree;
            this.order = order;
        }

        public int getMaxDegree() {
            return degree;
        }

        public int getMaxOrder() {
            return order;
        }

        public double getMu() {
            return 1;
        }

        public double getAe() {
            return 1;
        }

        public AbsoluteDate getReferenceDate() {
            return null;
        }

        public double getOffset(AbsoluteDate date) {
            return 0;
        }

        public TideSystem getTideSystem() {
            return TideSystem.UNKNOWN;
        }

        @Override
        public NormalizedSphericalHarmonics onDate(final AbsoluteDate date) throws OrekitException {
            return new NormalizedSphericalHarmonics() {
                @Override
                public double getNormalizedCnm(int n, int m) throws OrekitException {
                    return 1;
                }

                @Override
                public double getNormalizedSnm(int n, int m) throws OrekitException {
                    return 1;
                }

                @Override
                public AbsoluteDate getDate() {
                    return date;
                }
            };
        }

    }

    // rough test to determine if J2 alone creates heliosynchronism
    @Test
    public void testHelioSynchronous()
        throws OrekitException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Transform itrfToEME2000 = itrf.getTransformTo(FramesFactory.getEME2000(), date);
        Vector3D pole           = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        Frame poleAligned       = new Frame(FramesFactory.getEME2000(),
                                            new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                            "pole aligned", true);

        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, poleAligned, date, mu);
        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = normalizedC20;
        double[][] s = new double[3][1];
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(itrf,
                                                                       GravityFieldFactory.getNormalizedProvider(6378136.460, mu,
                                                                                                                 TideSystem.UNKNOWN,
                                                                                                                 c, s)));

        // let the step handler perform the test
        propagator.setMasterMode(Constants.JULIAN_DAY, new SpotStepHandler(date, mu));
        propagator.setInitialState(new SpacecraftState(orbit));
        propagator.propagate(date.shiftedBy(7 * Constants.JULIAN_DAY));
        Assert.assertTrue(propagator.getCalls() < 9200);

    }

    private static class SpotStepHandler implements OrekitFixedStepHandler {

        public SpotStepHandler(AbsoluteDate date, double mu) throws OrekitException {
            sun       = CelestialBodyFactory.getSun();
            previous  = Double.NaN;
        }

        private PVCoordinatesProvider sun;
        private double previous;
        public void handleStep(SpacecraftState currentState, boolean isLast)
            throws OrekitException {


            AbsoluteDate current = currentState.getDate();
            Vector3D sunPos = sun.getPVCoordinates(current , FramesFactory.getEME2000()).getPosition();
            Vector3D normal = currentState.getPVCoordinates().getMomentum();
            double angle = Vector3D.angle(sunPos , normal);
            if (! Double.isNaN(previous)) {
                Assert.assertEquals(previous, angle, 0.0013);
            }
            previous = angle;
        }

    }

    // test the difference with the analytical extrapolator Eckstein Hechler
    @Test
    public void testEcksteinHechlerReference()
        throws OrekitException {

        //  Definition of initial conditions with position and velocity
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        Transform itrfToEME2000 = itrf.getTransformTo(FramesFactory.getEME2000(), date);
        Vector3D pole           = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        Frame poleAligned       = new Frame(FramesFactory.getEME2000(),
                                            new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                            "pole aligned", true);

        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                poleAligned, date, mu);

        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(itrf,
                                                                       GravityFieldFactory.getNormalizedProvider(ae, mu,
                                                                                                                 TideSystem.UNKNOWN,
                                                                       new double[][] {
                { 0.0 }, { 0.0 }, { normalizedC20 }, { normalizedC30 },
                { normalizedC40 }, { normalizedC50 }, { normalizedC60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        })));

        // let the step handler perform the test
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.setMasterMode(20, new EckStepHandler(initialOrbit, ae,
                                                        unnormalizedC20, unnormalizedC30, unnormalizedC40,
                                                        unnormalizedC50, unnormalizedC60));
        propagator.propagate(date.shiftedBy(50000));
        Assert.assertTrue(propagator.getCalls() < 1100);

    }

    private static class EckStepHandler implements OrekitFixedStepHandler {

        /** Body mu */
        private static final double mu =  3.986004415e+14;

        private EckStepHandler(Orbit initialOrbit, double ae,
                               double c20, double c30, double c40, double c50, double c60)
        throws OrekitException {
            referencePropagator =
                new EcksteinHechlerPropagator(initialOrbit,
                                              ae, mu, c20, c30, c40, c50, c60);
        }

        private EcksteinHechlerPropagator referencePropagator;
        public void handleStep(SpacecraftState currentState, boolean isLast) throws OrekitException{

            SpacecraftState EHPOrbit   = referencePropagator.propagate(currentState.getDate());
            Vector3D posEHP  = EHPOrbit.getPVCoordinates().getPosition();
            Vector3D posDROZ = currentState.getPVCoordinates().getPosition();
            Vector3D velEHP  = EHPOrbit.getPVCoordinates().getVelocity();
            Vector3D dif     = posEHP.subtract(posDROZ);

            Vector3D T = new Vector3D(1 / velEHP.getNorm(), velEHP);
            Vector3D W = EHPOrbit.getPVCoordinates().getMomentum().normalize();
            Vector3D N = Vector3D.crossProduct(W, T);

            Assert.assertTrue(dif.getNorm() < 111);
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(dif, T)) < 111);
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(dif, N)) <  54);
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(dif, W)) <  12);

        }

    }

    // test the difference with the Cunningham model
    @Test
    public void testZonalWithCunninghamReference()
        throws OrekitException {
        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);

        propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(1000));
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(itrf,
                                                                       GravityFieldFactory.getNormalizedProvider(ae, mu,
                                                                                                                 TideSystem.UNKNOWN,
                                                                       new double[][] {
                { 0.0 }, { 0.0 }, { normalizedC20 }, { normalizedC30 },
                { normalizedC40 }, { normalizedC50 }, { normalizedC60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        })));

        propagator.setInitialState(new SpacecraftState(orbit));
        SpacecraftState hfOrb = propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));

        propagator.removeForceModels();

        propagator.addForceModel(new CunninghamAttractionModel(itrf,
                                                               GravityFieldFactory.getUnnormalizedProvider(ae, mu,
                                                                                                           TideSystem.UNKNOWN,
                                                               new double[][] {
                { 0.0 }, { 0.0 }, { unnormalizedC20 }, { unnormalizedC30 },
                { unnormalizedC40 }, { unnormalizedC50 }, { unnormalizedC60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        }), 1.0));

        propagator.setInitialState(new SpacecraftState(orbit));
        SpacecraftState cOrb = propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));

        Vector3D dif = hfOrb.getPVCoordinates().getPosition().subtract(cOrb.getPVCoordinates().getPosition());
        Assert.assertEquals(0, dif.getNorm(), 2e-9);
        Assert.assertTrue(propagator.getCalls() < 400);
    }

    @Test
    public void testCompleteWithCunninghamReference()
        throws OrekitException {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, OrbitType.CARTESIAN);
        AbsoluteDate targetDate = date.shiftedBy(3 * Constants.JULIAN_DAY);

        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(itrf,
                                                                       GravityFieldFactory.getNormalizedProvider(69, 69)));

        propagator.setInitialState(new SpacecraftState(orbit));
        SpacecraftState hfOrb = propagator.propagate(targetDate);

        propagator.removeForceModels();

        propagator.addForceModel(new CunninghamAttractionModel(itrf,
                                                               GravityFieldFactory.getUnnormalizedProvider(69, 69),
                                                               1.0));

        propagator.setInitialState(new SpacecraftState(orbit));
        SpacecraftState cOrb = propagator.propagate(targetDate);

        Vector3D dif = hfOrb.getPVCoordinates().getPosition().subtract(cOrb.getPVCoordinates().getPosition());
        Assert.assertEquals(0, dif.getNorm(), 4e-5);
    }

    @Test
    public void testIssue97() throws OrekitException {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // pos-vel (from a ZOOM ephemeris reference)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       GravityFieldFactory.getUnnormalizedProvider(1, 1).getMu()));

        for (int i = 2; i <= 69; i++) {
            final ForceModel holmesFeatherstoneModel =
                    new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                          GravityFieldFactory.getNormalizedProvider(i, i));
            final ForceModel cunninghamModel =
                    new CunninghamAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                  GravityFieldFactory.getUnnormalizedProvider(i, i), 1.0);
            double relativeError = accelerationRelativeError(holmesFeatherstoneModel, cunninghamModel, state);
            Assert.assertEquals(0.0, relativeError, 8.0e-15);
        }

    }

    @Test
    public void testParameterDerivative() throws OrekitException {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // pos-vel (from a ZOOM ephemeris reference)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       GravityFieldFactory.getUnnormalizedProvider(1, 1).getMu()));

        final HolmesFeatherstoneAttractionModel holmesFeatherstoneModel =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                      GravityFieldFactory.getNormalizedProvider(20, 20));

        final String name = NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT;
        checkParameterDerivative(state, holmesFeatherstoneModel, name, 1.0e-5, 5.0e-12);

    }

    private double accelerationRelativeError(ForceModel testModel, ForceModel referenceModel,
                                             SpacecraftState state)
        throws OrekitException {

        AccelerationRetriever accelerationRetriever = new AccelerationRetriever();
        testModel.addContribution(state, accelerationRetriever);
        final Vector3D testAcceleration = accelerationRetriever.getAcceleration();
        referenceModel.addContribution(state, accelerationRetriever);
        final Vector3D referenceAcceleration = accelerationRetriever.getAcceleration();

        return testAcceleration.subtract(referenceAcceleration).getNorm() /
               referenceAcceleration.getNorm();

    }

    @Test
    public void testTimeDependentField() throws OrekitException {

        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState spacecraftState =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       GravityFieldFactory.getUnnormalizedProvider(1, 1).getMu()));

        double dP = 0.1;
        double duration = 3 * Constants.JULIAN_DAY;
        BoundedPropagator fixedFieldEphemeris   = createEphemeris(dP, spacecraftState, duration,
                                                                  GravityFieldFactory.getConstantNormalizedProvider(8, 8));
        BoundedPropagator varyingFieldEphemeris = createEphemeris(dP, spacecraftState, duration,
                                                                  GravityFieldFactory.getNormalizedProvider(8, 8));

        double step = 60.0;
        double maxDeltaT = 0;
        double maxDeltaN = 0;
        double maxDeltaW = 0;
        for (AbsoluteDate date = fixedFieldEphemeris.getMinDate();
             date.compareTo(fixedFieldEphemeris.getMaxDate()) < 0;
             date = date.shiftedBy(step)) {
            PVCoordinates pvFixedField   = fixedFieldEphemeris.getPVCoordinates(date, FramesFactory.getGCRF());
            PVCoordinates pvVaryingField = varyingFieldEphemeris.getPVCoordinates(date, FramesFactory.getGCRF());
            Vector3D t = pvFixedField.getVelocity().normalize();
            Vector3D w = pvFixedField.getMomentum().normalize();
            Vector3D n = Vector3D.crossProduct(w, t);
            Vector3D delta = pvVaryingField.getPosition().subtract(pvFixedField.getPosition());
            maxDeltaT = FastMath.max(maxDeltaT, FastMath.abs(Vector3D.dotProduct(delta, t)));
            maxDeltaN = FastMath.max(maxDeltaN, FastMath.abs(Vector3D.dotProduct(delta, n)));
            maxDeltaW = FastMath.max(maxDeltaW, FastMath.abs(Vector3D.dotProduct(delta, w)));
        }
        Assert.assertTrue(maxDeltaT > 0.15);
        Assert.assertTrue(maxDeltaT < 0.25);
        Assert.assertTrue(maxDeltaN > 0.01);
        Assert.assertTrue(maxDeltaN < 0.02);
        Assert.assertTrue(maxDeltaW > 0.05);
        Assert.assertTrue(maxDeltaW < 0.10);

    }

    private BoundedPropagator createEphemeris(double dP, SpacecraftState initialState, double duration,
                                              NormalizedSphericalHarmonicsProvider provider)
        throws OrekitException {
        double[][] tol = NumericalPropagator.tolerances(dP, initialState.getOrbit(), OrbitType.CARTESIAN);
        AbstractIntegrator integrator =
                new DormandPrince853Integrator(0.001, 120.0, tol[0], tol[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setEphemerisMode();
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider));
        propagator.setInitialState(initialState);
        propagator.propagate(initialState.getDate().shiftedBy(duration));
        return propagator.getGeneratedEphemeris();
    }

    @Test
    public void testStateJacobian()
        throws OrekitException {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);

        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        HolmesFeatherstoneAttractionModel hfModel =
                new HolmesFeatherstoneAttractionModel(itrf, GravityFieldFactory.getNormalizedProvider(50, 50));
        Assert.assertEquals(TideSystem.UNKNOWN, hfModel.getTideSystem());
        propagator.addForceModel(hfModel);
        SpacecraftState state0 = new SpacecraftState(orbit);
        propagator.setInitialState(state0);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           50000, tolerances[0], 7.8e-6);
    }

    @Before
    public void setUp() {
        itrf   = null;
        propagator = null;
        Utils.setDataRoot("regular-data");
        try {
            // Eigen 6s model truncated to degree 6
            mu              =  3.986004415e+14;
            ae              =  6378136.460;
            normalizedC20   = -4.84165299820e-04;
            normalizedC30   =  9.57211326674e-07;
            normalizedC40   =  5.39990167207e-07;
            normalizedC50   =  6.86846073356e-08 ;
            normalizedC60   = -1.49953256913e-07;
            unnormalizedC20 = FastMath.sqrt( 5) * normalizedC20;
            unnormalizedC30 = FastMath.sqrt( 7) * normalizedC30;
            unnormalizedC40 = FastMath.sqrt( 9) * normalizedC40;
            unnormalizedC50 = FastMath.sqrt(11) * normalizedC50;
            unnormalizedC60 = FastMath.sqrt(13) * normalizedC60;

            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            double[] absTolerance = {
                0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
            };
            double[] relTolerance = {
                1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
            };
            AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
            integrator.setInitialStepSize(60);
            propagator = new NumericalPropagator(integrator);
        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() {
        itrf       = null;
        propagator = null;
    }

    private double unnormalizedC20;
    private double unnormalizedC30;
    private double unnormalizedC40;
    private double unnormalizedC50;
    private double unnormalizedC60;
    private double normalizedC20;
    private double normalizedC30;
    private double normalizedC40;
    private double normalizedC50;
    private double normalizedC60;
    private double mu;
    private double ae;

    private Frame   itrf;
    private NumericalPropagator propagator;

}


