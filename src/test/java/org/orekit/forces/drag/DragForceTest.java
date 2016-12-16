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
package org.orekit.forces.drag;


import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
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
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.forces.drag.atmosphere.Atmosphere;
import org.orekit.forces.drag.atmosphere.HarrisPriester;
import org.orekit.forces.drag.atmosphere.SimpleExponentialAtmosphere;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
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
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class DragForceTest extends AbstractForceModelTest {

    @Test
    @Deprecated
    public void testDeprecatedMethods() throws OrekitException {
        final DragSensitive ds = new IsotropicDrag(2.5, 1.2);
        final List<String> names = ds.getDragParametersNames();
        Assert.assertEquals(1, names.size());
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT, names.get(0));
        Assert.assertEquals(1.2, ds.getDragCoefficient(), 1.0e-10);
        ds.setDragCoefficient(10.0);
        Assert.assertEquals(10.0, ds.getDragCoefficient(), 1.0e-10);
    }

    @Test
    public void testParameterDerivativeSphere() throws OrekitException {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new IsotropicDrag(2.5, 1.2));

        checkParameterDerivative(state, forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 2.0e-12);

    }

    @Test
    public void testStateJacobianSphere()
        throws OrekitException {

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
        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new IsotropicDrag(2.5, 1.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 2.0e-8);

    }

    @Test
    public void testParameterDerivativeBox() throws OrekitException {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.7, 0.2));

        checkParameterDerivative(state, forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 2.0e-12);

    }

    @Test
    public void testStateJacobianBox()
        throws OrekitException {

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
        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 3.0e-8);

    }

    @Test
    public void testIssue229() throws OrekitException {
        AbsoluteDate initialDate = new AbsoluteDate(2004, 1, 1, 0, 0, 0., TimeScalesFactory.getUTC());
        Frame frame       = FramesFactory.getEME2000();
        double rpe         = 160.e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double rap         = 2000.e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double inc         = FastMath.toRadians(0.);
        double aop         = FastMath.toRadians(0.);
        double raan        = FastMath.toRadians(0.);
        double mean        = FastMath.toRadians(180.);
        double mass        = 100.;
        KeplerianOrbit orbit = new KeplerianOrbit(0.5 * (rpe + rap), (rap - rpe) / (rpe + rap),
                                                  inc, aop, raan, mean, PositionAngle.MEAN,
                                                  frame, initialDate, Constants.EIGEN5C_EARTH_MU);

        IsotropicDrag shape = new IsotropicDrag(10., 2.2);

        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, itrf);
        Atmosphere atmosphere = new SimpleExponentialAtmosphere(earthShape, 2.6e-10, 200000, 26000);

        double[][]          tolerance  = NumericalPropagator.tolerances(0.1, orbit, OrbitType.CARTESIAN);
        AbstractIntegrator  integrator = new DormandPrince853Integrator(1.0e-3, 300, tolerance[0], tolerance[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setMu(orbit.getMu());
        propagator.addForceModel(new DragForce(atmosphere, shape));
        PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
        propagator.setInitialState(partials.setInitialJacobians(new SpacecraftState(orbit, mass), 6));

        SpacecraftState state = propagator.propagate(new AbsoluteDate(2004, 1, 1, 1, 30, 0., TimeScalesFactory.getUTC()));

        double delta = 0.1;
        Orbit shifted = new CartesianOrbit(new TimeStampedPVCoordinates(orbit.getDate(),
                                                                        orbit.getPVCoordinates().getPosition().add(new Vector3D(delta, 0, 0)),
                                                                        orbit.getPVCoordinates().getVelocity()),
                                           orbit.getFrame(), orbit.getMu());
        propagator.setInitialState(partials.setInitialJacobians(new SpacecraftState(shifted, mass), 6));
        SpacecraftState newState = propagator.propagate(new AbsoluteDate(2004, 1, 1, 1, 30, 0., TimeScalesFactory.getUTC()));
        double[] dPVdX = new double[] {
            (newState.getPVCoordinates().getPosition().getX() - state.getPVCoordinates().getPosition().getX()) / delta,
            (newState.getPVCoordinates().getPosition().getY() - state.getPVCoordinates().getPosition().getY()) / delta,
            (newState.getPVCoordinates().getPosition().getZ() - state.getPVCoordinates().getPosition().getZ()) / delta,
            (newState.getPVCoordinates().getVelocity().getX() - state.getPVCoordinates().getVelocity().getX()) / delta,
            (newState.getPVCoordinates().getVelocity().getY() - state.getPVCoordinates().getVelocity().getY()) / delta,
            (newState.getPVCoordinates().getVelocity().getZ() - state.getPVCoordinates().getVelocity().getZ()) / delta,
        };

        double[][] dYdY0 = new double[6][6];
        partials.getMapper().getStateJacobian(state, dYdY0);
        for (int i = 0; i < 6; ++i) {
            Assert.assertEquals(dPVdX[i], dYdY0[i][0], 6.2e-6 * FastMath.abs(dPVdX[i]));
        }

    }
    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to 
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldTest() throws OrekitException{
        DerivativeStructure a_0 = new DerivativeStructure(6, 4, 0, 7e6);
        DerivativeStructure e_0 = new DerivativeStructure(6, 4, 1, 0.01);
        DerivativeStructure i_0 = new DerivativeStructure(6, 4, 2, 1.2);
        DerivativeStructure R_0 = new DerivativeStructure(6, 4, 3, 0.7);
        DerivativeStructure O_0 = new DerivativeStructure(6, 4, 4, 0.5);
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
        
        ClassicalRungeKuttaFieldIntegrator<DerivativeStructure> integrator =
                        new ClassicalRungeKuttaFieldIntegrator<DerivativeStructure>(field, zero.add(6));
        ClassicalRungeKuttaIntegrator RIntegrator =
                        new ClassicalRungeKuttaIntegrator(6);
                
        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<DerivativeStructure>(field, integrator);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setInitialState(iSR);
        
        final DragForce forceModel =
                        new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                                      new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                                     Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);
        
        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(10000.);
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
                                                                                       new double[] {1e3, 0.005, 0.005, 0.01, 0.01, 0.01}, 
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
            
            //System.out.println(pos_DS.getX().getPartialDerivative(1));

            double x = finPVC_shift.getPosition().getX();
            double y = finPVC_shift.getPosition().getY();
            double z = finPVC_shift.getPosition().getZ();
            Assert.assertEquals(x_DS, x, FastMath.abs(x - pos_DS.getX().getReal()) * 1e-5);
            Assert.assertEquals(y_DS, y, FastMath.abs(y - pos_DS.getY().getReal()) * 1e-5);
            Assert.assertEquals(z_DS, z, FastMath.abs(z - pos_DS.getZ().getReal()) * 1e-5);
            
            //velocity check
            
            FieldVector3D<DerivativeStructure> vel_DS = finPVC_DS.getVelocity();
            double vx_DS = vel_DS.getX().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double vy_DS = vel_DS.getY().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);                                                                               
            double vz_DS = vel_DS.getZ().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double vx = finPVC_shift.getVelocity().getX();
            double vy = finPVC_shift.getVelocity().getY();
            double vz = finPVC_shift.getVelocity().getZ();
            Assert.assertEquals(vx_DS, vx, FastMath.abs(vx) * 1e-7);
            Assert.assertEquals(vy_DS, vy, FastMath.abs(vy) * 1e-7);
            Assert.assertEquals(vz_DS, vz, FastMath.abs(vz) * 1e-7);
            //acceleration check
            
            FieldVector3D<DerivativeStructure> acc_DS = finPVC_DS.getAcceleration();
            double ax_DS = acc_DS.getX().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double ay_DS = acc_DS.getY().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);                                                                               
            double az_DS = acc_DS.getZ().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double ax = finPVC_shift.getAcceleration().getX();
            double ay = finPVC_shift.getAcceleration().getY();
            double az = finPVC_shift.getAcceleration().getZ();
            Assert.assertEquals(ax_DS, ax, FastMath.abs(ax) * 1e-5);
            Assert.assertEquals(ay_DS, ay, FastMath.abs(ay) * 1e-5);
            Assert.assertEquals(az_DS, az, FastMath.abs(az) * 1e-5);
        }
        
        
    }
    

    /**Same test as the previous one but not adding the ForceModel to the NumericalPropagator
    it is a test to validate the previous test. 
    (to test if the ForceModel it's actually
    doing something in the Propagator and the FieldPropagator)*/
    @Test
    public void RealFieldExpectErrorTest() throws OrekitException{
        DerivativeStructure a_0 = new DerivativeStructure(6, 0, 0, 7e6);
        DerivativeStructure e_0 = new DerivativeStructure(6, 0, 1, 0.01);
        DerivativeStructure i_0 = new DerivativeStructure(6, 0, 2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = new DerivativeStructure(6, 0, 3, 0.7);
        DerivativeStructure O_0 = new DerivativeStructure(6, 0, 4, 0.5);
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
        
        final DragForce forceModel =
                        new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                                      new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                                     Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        FNP.addForceModel(forceModel);
     //NOT ADDING THE FORCE MODEL TO THE NUMERICAL PROPAGATOR   NP.addForceModel(forceModel);
        
        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(10000.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();
    
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getX() - finPVC_R.getPosition().getX()) < FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getY() - finPVC_R.getPosition().getY()) < FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getZ() - finPVC_R.getPosition().getZ()) < FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
    }

}


