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
package org.orekit.forces.radiation;


import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

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
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class SolarRadiationPressureTest extends AbstractLegacyForceModelTest {

    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel,
                                                                         final AbsoluteDate date, final  Frame frame,
                                                                         final FieldVector3D<DerivativeStructure> position,
                                                                         final FieldVector3D<DerivativeStructure> velocity,
                                                                         final FieldRotation<DerivativeStructure> rotation,
                                                                         final DerivativeStructure mass)
        {
        try {
            java.lang.reflect.Field kRefField = SolarRadiationPressure.class.getDeclaredField("kRef");
            kRefField.setAccessible(true);
            double kRef = kRefField.getDouble(forceModel);
            java.lang.reflect.Field sunField = SolarRadiationPressure.class.getDeclaredField("sun");
            sunField.setAccessible(true);
            PVCoordinatesProvider sun = (PVCoordinatesProvider) sunField.get(forceModel);
            java.lang.reflect.Field spacecraftField = SolarRadiationPressure.class.getDeclaredField("spacecraft");
            spacecraftField.setAccessible(true);
            RadiationSensitive spacecraft = (RadiationSensitive) spacecraftField.get(forceModel);
            java.lang.reflect.Method getLightingRatioMethod = SolarRadiationPressure.class.getDeclaredMethod("getLightingRatio",
                                                                                                             FieldVector3D.class,
                                                                                                             Frame.class,
                                                                                                             FieldAbsoluteDate.class);
            getLightingRatioMethod.setAccessible(true);

            final Field<DerivativeStructure> field = position.getX().getField();
            final FieldVector3D<DerivativeStructure> sunSatVector = position.subtract(sun.getPVCoordinates(date, frame).getPosition());
            final DerivativeStructure r2  = sunSatVector.getNormSq();

            // compute flux
            final DerivativeStructure ratio = (DerivativeStructure) getLightingRatioMethod.invoke(forceModel, position, frame, new FieldAbsoluteDate<>(field, date));
            final DerivativeStructure rawP = ratio.multiply(kRef).divide(r2);
            final FieldVector3D<DerivativeStructure> flux = new FieldVector3D<>(rawP.divide(r2.sqrt()), sunSatVector);

            // compute acceleration with all its partial derivatives
            return spacecraft.radiationPressureAcceleration(new FieldAbsoluteDate<>(field, date),
                                                            frame, position, rotation, mass, flux,
                                                            forceModel.getParameters(field));
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException |
                 SecurityException | NoSuchMethodException | InvocationTargetException e) {
            return null;
        }
    }

    @Test
    public void testLightingInterplanetary() throws ParseException {
        // Initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 3, 21),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Orbit orbit = new KeplerianOrbit(1.0e11, 0.1, 0.2, 0.3, 0.4, 0.5, PositionAngle.TRUE,
                                         CelestialBodyFactory.getSolarSystemBarycenter().getInertiallyOrientedFrame(),
                                         date, Constants.JPL_SSD_SUN_GM);
        ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        SolarRadiationPressure srp =
            new SolarRadiationPressure(sun, Constants.SUN_RADIUS,
                                       (RadiationSensitive) new IsotropicRadiationClassicalConvention(50.0, 0.5, 0.5));
        Assert.assertFalse(srp.dependsOnPositionOnly());

        Vector3D position = orbit.getPVCoordinates().getPosition();
        Frame frame       = orbit.getFrame();
        Assert.assertEquals(1.0,
                            srp.getLightingRatio(position, frame, date),
                            1.0e-15);

        Assert.assertEquals(1.0,
                            srp.getLightingRatio(new FieldVector3D<>(Decimal64Field.getInstance(), position),
                                                 frame,
                                                 new FieldAbsoluteDate<>(Decimal64Field.getInstance(), date)).getReal(),
                            1.0e-15);
    }

    @Test
    public void testLighting() throws ParseException {
            // Initialization
            AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 3, 21),
                                                 new TimeComponents(13, 59, 27.816),
                                                 TimeScalesFactory.getUTC());
            Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3,
                                               FastMath.tan(0.001745329)*FastMath.cos(2*FastMath.PI/3), FastMath.tan(0.001745329)*FastMath.sin(2*FastMath.PI/3),
                                               0.1, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
            ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();
            OneAxisEllipsoid earth =
                new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
            SolarRadiationPressure SRP =
                new SolarRadiationPressure(sun, earth.getEquatorialRadius(),
                                           (RadiationSensitive) new IsotropicRadiationCNES95Convention(50.0, 0.5, 0.5));

            double period = 2*FastMath.PI*FastMath.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/orbit.getMu());
            Assert.assertEquals(86164, period, 1);

        // creation of the propagator
        KeplerianPropagator k = new KeplerianPropagator(orbit);

        // intermediate variables
        AbsoluteDate currentDate;
        double changed = 1;
        int count=0;

        for (int t=1;t<3*period;t+=1000) {
            currentDate = date.shiftedBy(t);
            try {

                double ratio = SRP.getLightingRatio(k.propagate(currentDate).getPVCoordinates().getPosition(),
                                                    FramesFactory.getEME2000(), currentDate);

                if (FastMath.floor(ratio)!=changed) {
                    changed = FastMath.floor(ratio);
                    if (changed == 0) {
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
    public void testGlobalStateJacobianIsotropicSingle()
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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           new IsotropicRadiationSingleCoefficient(2.5, 0.7));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 2.0e-5);

    }

    @Test
    public void testLocalJacobianIsotropicClassicalVs80Implementation()
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
        final SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           new IsotropicRadiationClassicalConvention(2.5, 0.7, 0.2));

        checkStateJacobianVs80Implementation(new SpacecraftState(orbit), forceModel,
                                             new LofOffset(orbit.getFrame(), LOFType.VVLH),
                                             1.0e-15, false);

    }

    @Test
    public void testLocalJacobianIsotropicClassicalVsFiniteDifferencesFullLight()
        {
        // here, lighting ratio is exactly 1 for all points used for finite differences
        doTestLocalJacobianIsotropicClassicalVsFiniteDifferences(250.0, 1000.0, 3.0e-8, false);
    }

    @Test
    public void testLocalJacobianIsotropicClassicalVsFiniteDifferencesPenumbra()
        {
        // here, lighting ratio is about 0.57,
        // and remains strictly between 0 and 1 for all points used for finite differences
        doTestLocalJacobianIsotropicClassicalVsFiniteDifferences(275.5, 100.0, 8.0e-7, false);
    }

    @Test
    public void testLocalJacobianIsotropicClassicalVsFiniteDifferencesEclipse()
        {
        // here, lighting ratio is exactly 0 for all points used for finite differences
        doTestLocalJacobianIsotropicClassicalVsFiniteDifferences(300.0, 1000.0, 1.0e-50, false);
    }

    private void doTestLocalJacobianIsotropicClassicalVsFiniteDifferences(double deltaT, double dP,
                                                                          double checkTolerance,
                                                                          boolean print)
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
        final SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           new IsotropicRadiationClassicalConvention(2.5, 0.7, 0.2));

        checkStateJacobianVsFiniteDifferences(new SpacecraftState(orbit.shiftedBy(deltaT)), forceModel,
                                              Propagator.DEFAULT_LAW, dP, checkTolerance, print);

    }

    @Test
    public void testGlobalStateJacobianIsotropicClassical()
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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           new IsotropicRadiationClassicalConvention(2.5, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e6, tolerances[0], 2.0e-5);

    }

    @Test
    public void testGlobalStateJacobianIsotropicCnes()
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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           new IsotropicRadiationCNES95Convention(2.5, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 3.0e-5);

    }

    @Test
    public void testParameterDerivativeBox() {

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

        checkParameterDerivative(state, forceModel, RadiationSensitive.ABSORPTION_COEFFICIENT, 0.25, 1.9e-15);
        checkParameterDerivative(state, forceModel, RadiationSensitive.REFLECTION_COEFFICIENT, 0.25, 6.9e-15);

    }

    @Test
    public void testGlobalStateJacobianBox()
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
        SolarRadiationPressure forceModel =
                new SolarRadiationPressure(CelestialBodyFactory.getSun(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                           new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                                          Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 5.0e-4);

    }

    @Test
    public void testRoughOrbitalModifs() throws ParseException, OrekitException, FileNotFoundException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 7, 1),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3,
                                           FastMath.tan(0.001745329)*FastMath.cos(2*FastMath.PI/3),
                                           FastMath.tan(0.001745329)*FastMath.sin(2*FastMath.PI/3),
                                           0.1, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);
        final double period = orbit.getKeplerianPeriod();
        Assert.assertEquals(86164, period, 1);
        ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

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

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            final double dex = currentState.getEquinoctialEx() - 0.01071166;
            final double dey = currentState.getEquinoctialEy() - 0.00654848;
            final double alpha = FastMath.toDegrees(FastMath.atan2(dey, dex));
            Assert.assertTrue(alpha > 100.0);
            Assert.assertTrue(alpha < 112.0);
            checkRadius(FastMath.sqrt(dex * dex + dey * dey), 0.003524, 0.003541);
        }

    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldIsotropicTest() {
        DSFactory factory = new DSFactory(6, 5);
        DerivativeStructure a_0 = factory.variable(0, 7e7);
        DerivativeStructure e_0 = factory.variable(1, 0.4);
        DerivativeStructure i_0 = factory.variable(2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        FieldAbsoluteDate<DerivativeStructure> J2000 = FieldAbsoluteDate.getJ2000Epoch(field);

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

        ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        // creation of the force model
        OneAxisEllipsoid earth =
            new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        SolarRadiationPressure forceModel =
            new SolarRadiationPressure(sun, earth.getEquatorialRadius(),
                                       new IsotropicRadiationCNES95Convention(500.0, 0.7, 0.7));

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(1000.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assert.assertEquals(0,
                            Vector3D.distance(finPVC_DS.toPVCoordinates().getPosition(),
                                              finPVC_R.getPosition()),
                            4.0e-9);

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
            shift_NP.setOrbitType(type);
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
            Assert.assertEquals(x_DS, x, FastMath.abs(x - pos_DS.getX().getReal()) * 4e-9);
            Assert.assertEquals(y_DS, y, FastMath.abs(y - pos_DS.getY().getReal()) * 5e-9);
            Assert.assertEquals(z_DS, z, FastMath.abs(z - pos_DS.getZ().getReal()) * 6e-10);

            //velocity check

            FieldVector3D<DerivativeStructure> vel_DS = finPVC_DS.getVelocity();
            double vx_DS = vel_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vy_DS = vel_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vz_DS = vel_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vx = finPVC_shift.getVelocity().getX();
            double vy = finPVC_shift.getVelocity().getY();
            double vz = finPVC_shift.getVelocity().getZ();
            Assert.assertEquals(vx_DS, vx, FastMath.abs(vx) * 5e-11);
            Assert.assertEquals(vy_DS, vy, FastMath.abs(vy) * 3e-10);
            Assert.assertEquals(vz_DS, vz, FastMath.abs(vz) * 5e-11);
            //acceleration check

            FieldVector3D<DerivativeStructure> acc_DS = finPVC_DS.getAcceleration();
            double ax_DS = acc_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double ay_DS = acc_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double az_DS = acc_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double ax = finPVC_shift.getAcceleration().getX();
            double ay = finPVC_shift.getAcceleration().getY();
            double az = finPVC_shift.getAcceleration().getZ();
            Assert.assertEquals(ax_DS, ax, FastMath.abs(ax) * 2e-10);
            Assert.assertEquals(ay_DS, ay, FastMath.abs(ay) * 4e-10);
            Assert.assertEquals(az_DS, az, FastMath.abs(az) * 7e-10);
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
        NP.setInitialState(iSR);
        ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        // creation of the force model
        OneAxisEllipsoid earth =
            new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        SolarRadiationPressure forceModel =
            new SolarRadiationPressure(sun, earth.getEquatorialRadius(),
                                       new IsotropicRadiationCNES95Convention(500.0, 0.7, 0.7));

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

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
