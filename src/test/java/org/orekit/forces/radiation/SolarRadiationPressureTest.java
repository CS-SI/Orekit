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
package org.orekit.forces.radiation;


import java.io.FileNotFoundException;
import java.text.ParseException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class SolarRadiationPressureTest extends AbstractForceModelTest {

    @Test
    public void testLighting() throws OrekitException, ParseException {
        // Initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 3, 21),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Orbit orbit = new EquinoctialOrbit(42164000,10e-3,10e-3,
                                           FastMath.tan(0.001745329)*FastMath.cos(2*FastMath.PI/3), FastMath.tan(0.001745329)*FastMath.sin(2*FastMath.PI/3),
                                           0.1, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth =
            new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        SolarRadiationPressure SRP =
            new SolarRadiationPressure(sun, earth.getEquatorialRadius(),
                                       (RadiationSensitive) new IsotropicRadiationCNES95Convention(50.0, 0.5, 0.5));

        double period = 2*FastMath.PI*FastMath.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/orbit.getMu());
        Assert.assertEquals(86164, period,1);

        // creation of the propagator
        KeplerianPropagator k = new KeplerianPropagator(orbit);

        // intermediate variables
        AbsoluteDate currentDate;
        double changed = 1;
        int count=0;

        for(int t=1;t<3*period;t+=1000) {
            currentDate = date.shiftedBy(t);
            try {

                double ratio = SRP.getLightingRatio(k.propagate(currentDate).getPVCoordinates().getPosition(),
                                                    FramesFactory.getEME2000(), currentDate);

                if(FastMath.floor(ratio)!=changed) {
                    changed = FastMath.floor(ratio);
                    if(changed == 0) {
                        count++;
                    }
                }
            } catch (OrekitException e) {
                e.printStackTrace();
            }
        }
        Assert.assertTrue(3==count);
    }

    @Test
    public void testParameterDerivativeIsotropicSingle() throws OrekitException {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        RadiationSensitive rs = new IsotropicRadiationSingleCoefficient(2.5, 0.7);
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           rs);

        checkParameterDerivative(state, forceModel, RadiationSensitive.REFLECTION_COEFFICIENT, 1.0, 2.0e-15);

        try {
            rs.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                             state.getPVCoordinates().getPosition(),
                                             state.getAttitude().getRotation(),
                                             state.getMass(), Vector3D.ZERO,
                                             RadiationSensitive.ABSORPTION_COEFFICIENT);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oe.getSpecifier());
        }
        try {
            Assert.assertEquals(0.0, rs.getAbsorptionCoefficient(), 1.0e-15);
            rs.setAbsorptionCoefficient(0.3);
            Assert.fail("an exception should have been thrown");
        } catch (UnsupportedOperationException uso) {
            // expected
        }
    }

    @Test
    public void testParameterDerivativeIsotropicClassical() throws OrekitException {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        RadiationSensitive rs = new IsotropicRadiationClassicalConvention(2.5, 0.7, 0.2);
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           rs);

        checkParameterDerivative(state, forceModel, RadiationSensitive.ABSORPTION_COEFFICIENT, 1.0, 5.0e-16);
        checkParameterDerivative(state, forceModel, RadiationSensitive.REFLECTION_COEFFICIENT, 1.0, 2.0e-15);

        try {
            rs.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                             state.getPVCoordinates().getPosition(),
                                             state.getAttitude().getRotation(),
                                             state.getMass(), Vector3D.ZERO,
                                             "UNKNOWN");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oe.getSpecifier());
        }

    }

    @Test
    public void testParameterDerivativeIsotropicCnes() throws OrekitException {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        RadiationSensitive rs = new IsotropicRadiationCNES95Convention(2.5, 0.7, 0.2);
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           rs);

        checkParameterDerivative(state, forceModel, RadiationSensitive.ABSORPTION_COEFFICIENT, 1.0, 5.0e-16);
        checkParameterDerivative(state, forceModel, RadiationSensitive.REFLECTION_COEFFICIENT, 1.0, 2.0e-15);

        try {
            rs.radiationPressureAcceleration(state.getDate(), state.getFrame(),
                                             state.getPVCoordinates().getPosition(),
                                             state.getAttitude().getRotation(),
                                             state.getMass(), Vector3D.ZERO,
                                             "UNKNOWN");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oe.getSpecifier());
        }

    }

    @Test
    public void testStateJacobianIsotropicSingle()
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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           new IsotropicRadiationSingleCoefficient(2.5, 0.7));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 2.0e-6);

    }

    @Test
    public void testStateJacobianIsotropicClassical()
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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           new IsotropicRadiationClassicalConvention(2.5, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 2.0e-6);

    }

    @Test
    public void testStateJacobianIsotropicCnes()
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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           new IsotropicRadiationCNES95Convention(2.5, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 2.0e-6);

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

        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                               new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.7, 0.2));

        checkParameterDerivative(state, forceModel, RadiationSensitive.ABSORPTION_COEFFICIENT, 1.0, 4.0e-16);
        checkParameterDerivative(state, forceModel, RadiationSensitive.REFLECTION_COEFFICIENT, 1.0, 3.0e-15);

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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                                          Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 2.0e-5);

    }

    @Test
    public void testRoughOrbitalModifs() throws ParseException, OrekitException, FileNotFoundException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 7, 1),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Orbit orbit = new EquinoctialOrbit(42164000,10e-3,10e-3,
                                           FastMath.tan(0.001745329)*FastMath.cos(2*FastMath.PI/3),
                                           FastMath.tan(0.001745329)*FastMath.sin(2*FastMath.PI/3),
                                           0.1, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        final double period = orbit.getKeplerianPeriod();
        Assert.assertEquals(86164, period, 1);
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        // creation of the force model
        OneAxisEllipsoid earth =
            new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        SolarRadiationPressure SRP =
            new SolarRadiationPressure(sun, earth.getEquatorialRadius(),
                                       new IsotropicRadiationCNES95Convention(500.0, 0.7, 0.7));

        // creation of the propagator
        double[] absTolerance = {
            0.1, 1.0e-9, 1.0e-9, 1.0e-5, 1.0e-5, 1.0e-5, 0.001
        };
        double[] relTolerance = {
            1.0e-4, 1.0e-4, 1.0e-4, 1.0e-6, 1.0e-6, 1.0e-6, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(900.0, 60000, absTolerance, relTolerance);
        integrator.setInitialStepSize(3600);
        final NumericalPropagator calc = new NumericalPropagator(integrator);
        calc.addForceModel(SRP);

        // Step Handler
        calc.setMasterMode(FastMath.floor(period), new SolarStepHandler());
        AbsoluteDate finalDate = date.shiftedBy(10 * period);
        calc.setInitialState(new SpacecraftState(orbit, 1500.0));
        calc.propagate(finalDate);
        Assert.assertTrue(calc.getCalls() < 7100);
    }

    public static void checkRadius(double radius , double min , double max) {
        Assert.assertTrue(radius >= min);
        Assert.assertTrue(radius <= max);
    }

    private double mu = 3.98600E14;

    private static class SolarStepHandler implements OrekitFixedStepHandler {

        private SolarStepHandler() {
        }

        public void init(SpacecraftState s0, AbsoluteDate t) {
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            final double dex = currentState.getEquinoctialEx() - 0.01071166;
            final double dey = currentState.getEquinoctialEy() - 0.00654848;
            final double alpha = FastMath.toDegrees(FastMath.atan2(dey, dex));
            Assert.assertTrue(alpha > 100.0);
            Assert.assertTrue(alpha < 112.0);
            checkRadius(FastMath.sqrt(dex * dex + dey * dey), 0.003524, 0.003541);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
