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
package org.orekit.forces.empirical;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

import java.util.List;

public class TimeSpanParametricAccelerationTest extends AbstractForceModelTest {

    @Test
    public void testGetParameterDrivers() {

        // A date
        final TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate("2000-01-01T00:00:00.000", utc);

        // One AccelerationModel added, only one driver should be in the drivers' array
        TimeSpanParametricAcceleration forceModel = new TimeSpanParametricAcceleration(Vector3D.PLUS_K, false,
                                                                                       new PolynomialAccelerationModel("driver", date, 0));

        Assertions.assertFalse(forceModel.dependsOnPositionOnly());
        List<ParameterDriver> drivers = forceModel.getParametersDrivers();
        Assertions.assertEquals(1,  drivers.size());
        Assertions.assertEquals("driver[0]",  drivers.get(0).getName());

        // Extract acceleration model at an arbitrary epoch and check it is the one added
        PolynomialAccelerationModel accModel = (PolynomialAccelerationModel) forceModel.getAccelerationModel(date);
        drivers = accModel.getParametersDrivers();
        Assertions.assertEquals(1, drivers.size());
        Assertions.assertEquals(0.0,  drivers.get(0).getValue(), 0.);
        Assertions.assertEquals("driver[0]",  drivers.get(0).getName());

        // 3 AccelerationModel added, with one default
        // ----------------------------------------------
        String prefix = "C";
        int order = 0;
        double dt = 120.;
        // Build the force model
        accModel = new PolynomialAccelerationModel(prefix, date, order);
        PolynomialAccelerationModel accModel1 = new PolynomialAccelerationModel(prefix + "1", date, order);
        PolynomialAccelerationModel accModel2 = new PolynomialAccelerationModel(prefix + "2", date, order);
        forceModel = new TimeSpanParametricAcceleration(Vector3D.PLUS_K, false, accModel);
        forceModel.addAccelerationModelValidAfter(accModel1, date.shiftedBy(dt));
        forceModel.addAccelerationModelValidBefore(accModel2, date.shiftedBy(-dt));

        // Extract the drivers and check their values and names
        drivers = forceModel.getParametersDrivers();
        Assertions.assertEquals(3,  drivers.size());
        Assertions.assertEquals(0.0,  drivers.get(0).getValue(), 0.);
        Assertions.assertEquals("C2[0]", drivers.get(0).getName());
        Assertions.assertEquals(0.0,  drivers.get(1).getValue(), 0.);
        Assertions.assertEquals("C[0]",  drivers.get(1).getName());
        Assertions.assertEquals(0.0,  drivers.get(2).getValue(), 0.);
        Assertions.assertEquals("C1[0]", drivers.get(2).getName());

        // Check that proper models are returned at significant test dates
        double eps = 1.e-14;
        Assertions.assertEquals(accModel,  forceModel.getAccelerationModel(date));
        Assertions.assertEquals(accModel,  forceModel.getAccelerationModel(date.shiftedBy(-dt)));
        Assertions.assertEquals(accModel,  forceModel.getAccelerationModel(date.shiftedBy(+dt - eps)));
        Assertions.assertEquals(accModel2, forceModel.getAccelerationModel(date.shiftedBy(-dt - eps)));
        Assertions.assertEquals(accModel2, forceModel.getAccelerationModel(date.shiftedBy(-dt - 86400.)));
        Assertions.assertEquals(accModel1, forceModel.getAccelerationModel(date.shiftedBy(+dt)));
        Assertions.assertEquals(accModel1, forceModel.getAccelerationModel(date.shiftedBy(+dt + 86400.)));

        // Test #getAccelerationModelSpan method
        Assertions.assertEquals(accModel,  forceModel.getAccelerationModelSpan(date).getData());
        Assertions.assertEquals(accModel2, forceModel.getAccelerationModelSpan(date.shiftedBy(-dt - 86400.)).getData());
        Assertions.assertEquals(accModel1, forceModel.getAccelerationModelSpan(date.shiftedBy(+dt + 1.)).getData());

        // Test #extractAccelerationModelRange
        TimeSpanMap<AccelerationModel> dragMap = forceModel.extractAccelerationModelRange(date, date.shiftedBy(dt + 1.));
        Assertions.assertEquals(accModel,  dragMap.getSpan(date).getData());
        Assertions.assertEquals(accModel1, dragMap.getSpan(date.shiftedBy(dt + 86400.)).getData());
        Assertions.assertEquals(accModel,  dragMap.getSpan(date.shiftedBy(-dt - 86400.)).getData());
    }

    @Test
    public void testParameterDerivative() {

        // Time scale
        final TimeScale tai = TimeScalesFactory.getTAI();

        // Low Earth orbit definition (about 360km altitude)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final AbsoluteDate date = new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, tai);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       date,
                                                       Constants.EIGEN5C_EARTH_MU));

        // AccelerationModel
        final PolynomialAccelerationModel accelerationModel = new PolynomialAccelerationModel("C1", null, 0);
        accelerationModel.getParametersDrivers().get(0).setValue(1.4);
        final TimeSpanParametricAcceleration forceModel = new TimeSpanParametricAcceleration(Vector3D.PLUS_I, true, accelerationModel);

        // After t2 = t + 4h
        final double dt2 = 4 * 3600.;
        final AbsoluteDate date2 = date.shiftedBy(dt2);
        final PolynomialAccelerationModel accelerationModel2 = new PolynomialAccelerationModel("C2", null, 0);
        accelerationModel2.getParametersDrivers().get(0).setValue(0.7);
        forceModel.addAccelerationModelValidAfter(accelerationModel2, date2);

        // Before t3 = t - 1day
        final double dt3 = -86400.;
        final AbsoluteDate date3 = date.shiftedBy(dt3);
        final PolynomialAccelerationModel accelerationModel3 = new PolynomialAccelerationModel("C3", null, 0);
        accelerationModel3.getParametersDrivers().get(0).setValue(2.7);
        forceModel.addAccelerationModelValidBefore(accelerationModel3, date3);

        // Initialize model
        forceModel.init(state, null);

        Assertions.assertTrue(forceModel.dependsOnPositionOnly());

        // Check parameter derivatives at initial date: only "C1" shouldn't be 0.
        checkParameterDerivative(state, forceModel, "C1[0]" , 1.0e-4, 2.0e-12);
        checkParameterDerivative(state, forceModel, "C2[0]", 1.0e-4, 0.);
        checkParameterDerivative(state, forceModel, "C3[0]", 1.0e-4, 0.);

        // Check parameter derivatives after date2: only "C2" shouldn't be 0.
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, "C1[0]", 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, "C2[0]", 1.0e-4, 2.0e-12);
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, "C3[0]", 1.0e-4, 0.);

        // Check parameter derivatives after date3: only "C3" shouldn't be 0.
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, "C1[0]", 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, "C2[0]", 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, "C3[0]", 1.0e-4, 2.0e-12);
    }

    @Test
    public void testStateJacobian() {

        // Initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);

        // Time span acceleration force model init
        double dt = 1. * 3600.;
        // Build the force model
        PolynomialAccelerationModel polyAcc0 = new PolynomialAccelerationModel("C0", null, 0);
        polyAcc0.getParametersDrivers().get(0).setValue(2.7);
        PolynomialAccelerationModel polyAcc1 = new PolynomialAccelerationModel("C1", null, 0);
        polyAcc1.getParametersDrivers().get(0).setValue(0.9);
        PolynomialAccelerationModel polyAcc2 = new PolynomialAccelerationModel("C2", null, 0);
        polyAcc2.getParametersDrivers().get(0).setValue(1.4);
        TimeSpanParametricAcceleration forceModel = new TimeSpanParametricAcceleration(Vector3D.PLUS_J, null, polyAcc0);
        forceModel.addAccelerationModelValidAfter(polyAcc1, date.shiftedBy(dt));
        forceModel.addAccelerationModelValidBefore(polyAcc2, date.shiftedBy(-dt));

        // Check state derivatives inside first AccelerationModel
        NumericalPropagator propagator =
                        new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                               tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);
        // Set target date to 0.5*dt to be inside 1st AccelerationModel
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(0.5 * dt),
                           1e3, tolerances[0], 1.0e-9);

        // Check state derivatives inside 2nd AccelerationModel
        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        // Set target date to 1.5*dt to be inside 2nd AccelerationModel
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(1.5 * dt),
                           1e3, tolerances[0], 1.5e-2);

        // Check state derivatives inside 3rd AccelerationModel
        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        // Set target date to *1.5*dt to be inside 3rd AccelerationModel
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(-1.5 * dt),
                           1e3, tolerances[0], 6.0e-3);
    }

    @Test
    public void testStateJacobianAttitudeOverride() {

        // Initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);

        // Time span acceleration force model init
        final AttitudeProvider attitudeOverride = new LofOffset(orbit.getFrame(), LOFType.VNC);
        double dt = 1. * 3600.;
        // Build the force model
        PolynomialAccelerationModel polyAcc0 = new PolynomialAccelerationModel("C0", null, 0);
        polyAcc0.getParametersDrivers().get(0).setValue(2.7);
        PolynomialAccelerationModel polyAcc1 = new PolynomialAccelerationModel("C1", null, 0);
        polyAcc1.getParametersDrivers().get(0).setValue(0.9);
        PolynomialAccelerationModel polyAcc2 = new PolynomialAccelerationModel("C2", null, 0);
        polyAcc2.getParametersDrivers().get(0).setValue(1.4);
        TimeSpanParametricAcceleration forceModel = new TimeSpanParametricAcceleration(Vector3D.PLUS_J, attitudeOverride, polyAcc0);
        forceModel.addAccelerationModelValidAfter(polyAcc1, date.shiftedBy(dt));
        forceModel.addAccelerationModelValidBefore(polyAcc2, date.shiftedBy(-dt));

        // Check state derivatives inside first AccelerationModel
        NumericalPropagator propagator =
                        new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                               tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);
        // Set target date to 0.5*dt to be inside 1st AccelerationModel
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(0.5 * dt),
                           1e3, tolerances[0], 1.7e-9);

        // Check state derivatives inside 2nd AccelerationModel
        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        // Set target date to 1.5*dt to be inside 2nd AccelerationModel
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(1.5 * dt),
                           1e3, tolerances[0], 1.8e-2);

        // Check state derivatives inside 3rd AccelerationModel
        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        // Set target date to *1.5*dt to be inside 3rd AccelerationModel
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(-1.5 * dt),
                           1e3, tolerances[0], 2.0e-2);
    }

    @Test
    public void RealFieldGradientTest() {
        // Initial field Keplerian orbit
        // The variables are the six orbital parameters
        final int freeParameters = 6;
        Gradient a_0 = Gradient.variable(freeParameters, 0, 7e6);
        Gradient e_0 = Gradient.variable(freeParameters, 1, 0.01);
        Gradient i_0 = Gradient.variable(freeParameters, 2, 1.2);
        Gradient R_0 = Gradient.variable(freeParameters, 3, 0.7);
        Gradient O_0 = Gradient.variable(freeParameters, 4, 0.5);
        Gradient n_0 = Gradient.variable(freeParameters, 5, 0.1);

        Field<Gradient> field = a_0.getField();
        Gradient zero = field.getZero();

        // Initial date = J2000 epoch
        FieldAbsoluteDate<Gradient> J2000 = new FieldAbsoluteDate<>(field);

        // J2000 frame
        Frame EME = FramesFactory.getEME2000();

        // Create initial field Keplerian orbit
        FieldKeplerianOrbit<Gradient> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                      PositionAngleType.MEAN,
                                                                      EME,
                                                                      J2000,
                                                                      zero.add(Constants.EIGEN5C_EARTH_MU));

        // Initial field and classical S/Cs
        FieldSpacecraftState<Gradient> initialState = new FieldSpacecraftState<>(FKO);
        SpacecraftState iSR = initialState.toSpacecraftState();

        // Field integrator and classical integrator
        ClassicalRungeKuttaFieldIntegrator<Gradient> integrator =
                        new ClassicalRungeKuttaFieldIntegrator<>(field, zero.add(6));
        ClassicalRungeKuttaIntegrator RIntegrator =
                        new ClassicalRungeKuttaIntegrator(6);
        OrbitType type = OrbitType.EQUINOCTIAL;

        // Field and classical numerical propagators
        FieldNumericalPropagator<Gradient> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        // Set up force model

        // AccelerationModel init
        double dt = 1000.;
        // Build the force model
        PolynomialAccelerationModel polyAcc0 = new PolynomialAccelerationModel("C0", null, 0);
        polyAcc0.getParametersDrivers().get(0).setValue(2.7);
        PolynomialAccelerationModel polyAcc1 = new PolynomialAccelerationModel("C1", null, 0);
        polyAcc1.getParametersDrivers().get(0).setValue(0.9);
        PolynomialAccelerationModel polyAcc2 = new PolynomialAccelerationModel("C2", null, 0);
        polyAcc2.getParametersDrivers().get(0).setValue(1.4);
        TimeSpanParametricAcceleration forceModel = new TimeSpanParametricAcceleration(Vector3D.PLUS_J, null, polyAcc0);
        forceModel.addAccelerationModelValidAfter(polyAcc1, J2000.toAbsoluteDate().shiftedBy(dt));
        forceModel.addAccelerationModelValidBefore(polyAcc2, J2000.toAbsoluteDate().shiftedBy(-dt));
        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        // -----------

        // Propagate inside 1st AccelerationModel
        checkRealFieldPropagationGradient(FKO, PositionAngleType.MEAN, 0.9 * dt, NP, FNP,
                                  1.0e-9, 2.4e-02, 7.8e-5, 1.5e-3,
                                  1, false);

        // Propagate to 2nd AccelerationModel (reset propagator first)
        FNP.resetInitialState(initialState);
        NP.resetInitialState(iSR);
        checkRealFieldPropagationGradient(FKO, PositionAngleType.MEAN, 1.1 * dt, NP, FNP,
                                  1.0e-9, 4.3e-02, 8.2e-5, 3.1e-4,
                                  1, false);

        // Propagate to 3rd AccelerationModel (reset propagator first)
        FNP.resetInitialState(initialState);
        NP.resetInitialState(iSR);
        checkRealFieldPropagationGradient(FKO, PositionAngleType.MEAN, -1.1 * dt, NP, FNP,
                                  1.0e-9, 2.0e-02, 3.9e-04, 4.5e-04,
                                  1, false);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
