/* Copyright 2002-2025 CS GROUP
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
package org.orekit.forces.radiation;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.PVCoordinates;

/**
 * This force model was developed according to "EARTH RADIATION PRESSURE EFFECTS ON SATELLITES", 1988, by P. C. Knocke, J. C. Ries, and B. D. Tapley.
 * It was confronted to the results that are presented in this paper and reached satisfying performances.
 * However, the complete reproduction of the LAGEOS-1 test case is much too long for it to be implemented in test class.
 * Then, only
 */
class KnockeRediffusedForceModelTest extends AbstractForceModelTest{

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }


    @Test
    void testJacobianVsFiniteDifferences() {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 3, 1),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);

        // Sun
        final ExtendedPositionProvider sun = CelestialBodyFactory.getSun();

        // Radiation sensitive model
        final RadiationSensitive radiationSensitive = new IsotropicRadiationSingleCoefficient(1, 1.5);

        // Set up the force model to test
        final KnockeRediffusedForceModel forceModel = new KnockeRediffusedForceModel(sun,
                                                                                     radiationSensitive,
                                                                                     Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                                     FastMath.toRadians(30));

        SpacecraftState state = new SpacecraftState(orbit,
                                                    Utils.defaultLaw().getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferences(state, forceModel, Utils.defaultLaw(), 1.0, 5.5e-9, false);

    }

    @Test
    void testParameterIsotropicSingle() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        // Sun
        final ExtendedPositionProvider sun = CelestialBodyFactory.getSun();

        // Radiation sensitive model
        final RadiationSensitive radiationSensitive = new IsotropicRadiationSingleCoefficient(1, 1.5);

        // Set up the force model to test
        final KnockeRediffusedForceModel forceModel = new KnockeRediffusedForceModel(sun,
                                                                                     radiationSensitive,
                                                                                     Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                                     FastMath.toRadians(30));

        checkParameterDerivative(state, forceModel, RadiationSensitive.REFLECTION_COEFFICIENT, 0.5, 1.8e-16);

    }

    @Test
    void testGlobalStateJacobianIsotropicSingle()
        {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 3, 1),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = ToleranceProvider.getDefaultToleranceProvider(0.01).getTolerances(orbit, integrationType);

        NumericalPropagator propagator =
                new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                       tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);

        // Sun
        final ExtendedPositionProvider sun = CelestialBodyFactory.getSun();

        // Radiation sensitive model
        final RadiationSensitive radiationSensitive = new IsotropicRadiationSingleCoefficient(1, 1.5);

        // Set up the force model to test
        final KnockeRediffusedForceModel forceModel = new KnockeRediffusedForceModel(sun,
                                                                                     radiationSensitive,
                                                                                     Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                                     FastMath.toRadians(30));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 3.2e-8);

    }

    @Test
    void testRealField() {

        // Initial field Keplerian orbit
        // The variables are the six orbital parameters
        DSFactory factory = new DSFactory(6, 4);
        DerivativeStructure a_0 = factory.variable(0, 7e6);
        DerivativeStructure e_0 = factory.variable(1, 0.01);
        DerivativeStructure i_0 = factory.variable(2, 1.2);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        // Initial date = J2000 epoch
        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        // J2000 frame
        Frame EME = FramesFactory.getEME2000();

        // Create initial field Keplerian orbit
        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngleType.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        // Initial field and classical S/Cs
        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);
        SpacecraftState iSR = initialState.toSpacecraftState();

        // Field integrator and classical integrator
        ClassicalRungeKuttaFieldIntegrator<DerivativeStructure> integrator =
                        new ClassicalRungeKuttaFieldIntegrator<>(field, zero.add(6));
        ClassicalRungeKuttaIntegrator RIntegrator =
                        new ClassicalRungeKuttaIntegrator(6);
        OrbitType type = OrbitType.KEPLERIAN;

        // Field and classical numerical propagators
        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        // Sun
        final ExtendedPositionProvider sun = CelestialBodyFactory.getSun();

        // Radiation sensitive model
        final RadiationSensitive radiationSensitive = new IsotropicRadiationSingleCoefficient(1, 1.5);

        // Set up the force model to test
        final KnockeRediffusedForceModel forceModel = new KnockeRediffusedForceModel(sun,
                                                                                     radiationSensitive,
                                                                                     Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                                     FastMath.toRadians(30));

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        checkRealFieldPropagation(FKO, PositionAngleType.MEAN, 300., NP, FNP,
                                  1.0e-15, 1.3e-8, 6.7e-11, 1.4e-10,
                                  1, false);
    }


    @Test
    void testRealFieldGradient() {

        // Initial field Keplerian orbit
        // The variables are the six orbital parameters
        final Gradient a           = Gradient.variable(6, 0, 7e6);
        final Gradient e           = Gradient.variable(6, 1, 0.01);
        final Gradient i           = Gradient.variable(6, 2, 1.2);
        final Gradient pa          = Gradient.variable(6, 3, 0.7);
        final Gradient raan        = Gradient.variable(6, 4, 0.5);
        final Gradient meanAnomaly = Gradient.variable(6, 5, 0.1);

        final Field<Gradient> field = a.getField();
        final Gradient zero = field.getZero();

        // Initial date = J2000 epoch
        final FieldAbsoluteDate<Gradient> J2000 = new FieldAbsoluteDate<>(field);

        // J2000 frame
        final Frame EME = FramesFactory.getEME2000();

        // Create initial field Keplerian orbit
        final FieldKeplerianOrbit<Gradient> FKO = new FieldKeplerianOrbit<>(a, e, i, pa, raan, meanAnomaly,
                                                                            PositionAngleType.MEAN,
                                                                            EME,
                                                                            J2000,
                                                                            zero.add(Constants.EIGEN5C_EARTH_MU));

        // Initial field and classical S/Cs
        final FieldSpacecraftState<Gradient> initialState = new FieldSpacecraftState<>(FKO);
        final SpacecraftState iSR = initialState.toSpacecraftState();

        // Field integrator and classical integrator
        final ClassicalRungeKuttaFieldIntegrator<Gradient> integrator =
                        new ClassicalRungeKuttaFieldIntegrator<>(field, zero.add(6));
        final ClassicalRungeKuttaIntegrator RIntegrator =
                        new ClassicalRungeKuttaIntegrator(6);
        final OrbitType type = OrbitType.KEPLERIAN;

        // Field and classical numerical propagators
        final FieldNumericalPropagator<Gradient> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        final NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        // Sun
        final ExtendedPositionProvider sun = CelestialBodyFactory.getSun();

        // Radiation sensitive model
        final RadiationSensitive radiationSensitive = new IsotropicRadiationSingleCoefficient(1, 1.5);

        // Set up the force model to test
        final KnockeRediffusedForceModel forceModel = new KnockeRediffusedForceModel(sun,
                                                                                     radiationSensitive,
                                                                                     Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                                     FastMath.toRadians(30));

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        checkRealFieldPropagationGradient(FKO, PositionAngleType.MEAN, 300., NP, FNP,
                                          1.0e-15, 1.3e-2, 9.6e-5, 1.4e-4,
                                          1, false);
    }

    /** Roughly compare Knocke model accelerations against results from "EARTH RADIATION PRESSURE EFFECTS ON SATELLITES",
     *  1988, by P. C. Knocke, J. C. Ries, and B. D. Tapley.
     *  The case is as close as possible from what it might be in the paper. Orbit and date have been artifically set so that the angle between
     *  the orbital plan and Earth-Sun direction is almost equal to zero.
     */
    @Test
    void testRoughAcceleration() {

        // LAGEOS-1
        final double mass = 406.9;
        final double crossSection = 7E-4 * mass;
        final double K = 1 + 0.13;

        final TLE tle = new TLE ("1 08820U 76039  A 77047.52561960  .00000002 +00000-0 +00000-0 0  9994",
                                 "2 08820 109.8332 127.3884 0044194 201.3006 158.6132 06.38663945018402");

        // Orbit
        final KeplerianOrbit keplerianTLE = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(TLEPropagator.selectExtrapolator(tle).
                                                                                      propagate(tle.getDate()).getOrbit());
        final double a    = keplerianTLE.getA();
        final double e    = keplerianTLE.getE();
        final double i    = keplerianTLE.getI();
        final double pa   = keplerianTLE.getPerigeeArgument();
        final double raan = keplerianTLE.getRightAscensionOfAscendingNode();
        final double nu   = keplerianTLE.getTrueAnomaly();

        // Date
        final AbsoluteDate date0 = new AbsoluteDate(new DateComponents(1970, 1, 18),
                                                    new TimeComponents(0, 0, 0.0),
                                                    TimeScalesFactory.getUTC());

        // Frame
        final Frame frame = FramesFactory.getTEME();

        final KeplerianOrbit keplerian = new KeplerianOrbit(a, e, i, pa, raan, nu, PositionAngleType.TRUE, frame, date0, Constants.IERS2010_EARTH_MU);
        final SpacecraftState initState = new SpacecraftState(keplerian).withMass(mass);

        // Celestial objects

        // Sun
        final ExtendedPositionProvider sun = CelestialBodyFactory.getSun();

        // Earth
        final double equatorialRadius = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;


        // Earth Radiation model
        final double angularResolution = FastMath.toRadians(15);
        final RadiationSensitive radiationSensitive  = new IsotropicRadiationSingleCoefficient(crossSection, K);
        final KnockeRediffusedForceModel knockeModel = new KnockeRediffusedForceModel(sun, radiationSensitive,
                                                                                      equatorialRadius,
                                                                                      angularResolution);

        // Propagation time
        final double duration = keplerian.getKeplerianPeriod();

        // Creation of the propagator
        final double minStep = duration * 0.01;
        final double maxStep = duration * 0.5;
        final double handlerStep = duration / 20;
        final double positionTolerance = 1e-3;
        final double[][] tolerances = ToleranceProvider.getDefaultToleranceProvider(positionTolerance).getTolerances(keplerian, OrbitType.KEPLERIAN);
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(minStep, maxStep, tolerances[0], tolerances[1]);

        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        propagator.setInitialState(initState);
        propagator.addForceModel(knockeModel);
        propagator.setStepHandler(handlerStep, new KnockeStepHandler(knockeModel));

        final SpacecraftState finalState = propagator.propagate(date0.shiftedBy(duration));

        Assertions.assertEquals(date0.shiftedBy(duration), finalState.getDate());
    }

    @Test
    void testAlbedoAndEmissivityValues() {

        // Model
        final KnockeRediffusedForceModel forceModel = createSimpleForceModel();

        // Reference epoch: December 22, 1981 (winter solstice in northern hemisphere)
        final AbsoluteDate refEpoch = new AbsoluteDate(1981, 12, 22, 0, 0, 0.0, TimeScalesFactory.getUTC());

        // Test at equator (latitude = 0)
        // At equator, Legendre P1(0) = 0, P2(0) = -0.5
        // Albedo = A0 + A2 * P2(0) = 0.34 + 0.29 * (-0.5) = 0.34 - 0.145 = 0.195
        // Emissivity = E0 + E2 * P2(0) = 0.68 + (-0.18) * (-0.5) = 0.68 + 0.09 = 0.77
        final double refAlbedoEquator = KnockeRediffusedForceModel.A0 + KnockeRediffusedForceModel.A2 * (-0.5);
        final double refEmissivityEquator = KnockeRediffusedForceModel.E0 + KnockeRediffusedForceModel.E2 * (-0.5);
        Assertions.assertEquals(refAlbedoEquator, forceModel.computeAlbedo(refEpoch, 0.0), 1e-14);
        Assertions.assertEquals(refEmissivityEquator, forceModel.computeEmissivity(refEpoch, 0.0), 1e-14);

        // Test at north pole (latitude = PI/2) on reference epoch day
        // At pole, sin(phi) = 1, P1(1) = 1, P2(1) = 1
        // On Dec 22 (winter solstice), the periodic term cos(2*pi*dd/DAYS_YEAR) = 1
        // Albedo = A0 + C1 * 1 * 1 + A2 * 1 = 0.34 + 0.10 + 0.29 = 0.73
        // Emissivity = E0 + K1 * 1 * 1 + E2 * 1 = 0.68 - 0.07 - 0.18 = 0.43
        final double refAlbedoPole = KnockeRediffusedForceModel.A0 + KnockeRediffusedForceModel.A2 * 1 + KnockeRediffusedForceModel.C1 * 1 * 1;
        final double refEmissivityPole = KnockeRediffusedForceModel.E0 + KnockeRediffusedForceModel.E2 * 1 + KnockeRediffusedForceModel.K1 * 1 * 1;
        Assertions.assertEquals(refAlbedoPole, forceModel.computeAlbedo(refEpoch, MathUtils.SEMI_PI), 1e-14);
        Assertions.assertEquals(refEmissivityPole, forceModel.computeEmissivity(refEpoch, MathUtils.SEMI_PI), 1e-14);

        // Test periodicity: values should be the same after one year
        final AbsoluteDate oneYearLater = refEpoch.shiftedBy(365.25 * 86400.0);
        Assertions.assertEquals(refAlbedoPole, forceModel.computeAlbedo(oneYearLater, FastMath.PI / 2.0), 1e-14);
        Assertions.assertEquals(refEmissivityPole, forceModel.computeEmissivity(oneYearLater, FastMath.PI / 2.0), 1e-14);

        // Test at specific date and latitude (non regression)
        final AbsoluteDate specificDate = new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getUTC());
        final double specificLat = -0.8571953966;
        final double albedoSpecific = forceModel.computeAlbedo(specificDate, specificLat);
        final double emissivitySpecific = forceModel.computeEmissivity(specificDate, specificLat);
        Assertions.assertTrue(albedoSpecific > 0.3 && albedoSpecific < 0.5, "Albedo should be in reasonable range");
        Assertions.assertTrue(emissivitySpecific > 0.5 && emissivitySpecific < 0.8, "Emissivity should be in reasonable range");
    }

    @Test
    void testSolarFluxComputation() {
        // Model
        final KnockeRediffusedForceModel forceModel = createSimpleForceModel();
        // Solar flux at 1 AU: ES_COEFF * c / 1^2
        // ES_COEFF = 4.5606E-6, c = 299792458 m/s
        final double expectedFlux = KnockeRediffusedForceModel.ES_COEFF * Constants.SPEED_OF_LIGHT;
        Assertions.assertEquals(expectedFlux, forceModel.computeSolarFlux(new Vector3D(Constants.JPL_SSD_ASTRONOMICAL_UNIT, 0, 0)), 1e-10);
    }

    @Test
    void testAcceleration() {

        // LEO Orbit
        final Vector3D pos = new Vector3D(2.06479765813527000e+06,
                                          -3.89513229411636293e+06,
                                          -5.09383750217838865e+06);
        final Vector3D vel = new Vector3D(-2.19214076040873215e+03,
                                           5.39738533682446723e+03,
                                          -5.01727016544111484e+03);
        final AbsoluteDate date = new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getUTC());
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(pos, vel), FramesFactory.getGCRF(), date, Constants.WGS84_EARTH_MU);

        // Model
        final KnockeRediffusedForceModel forceModel = new KnockeRediffusedForceModel(CelestialBodyFactory.getSun(),
                new IsotropicRadiationSingleCoefficient(1.0, 1.5),
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                FastMath.toRadians(10));

        // Compute acceleration and verify
        // Typical values are on the order of 1e-11 to 1e-10 km/s^2 (Montenbruck, Satellite Orbits, Fig 3.1)
        final Vector3D acceleration = forceModel.acceleration(new SpacecraftState(orbit), forceModel.getParameters(date));
        final double accNormKmPerSec = acceleration.getNorm() * 0.001;
        Assertions.assertTrue(accNormKmPerSec > 0, "Acceleration should be non-zero in LEO");
        Assertions.assertTrue(acceleration.dotProduct(pos.normalize()) > 0, "Radial acceleration should be positive (away from Earth)");
        Assertions.assertTrue(accNormKmPerSec > 1e-11 && accNormKmPerSec < 1e-10, "Acceleration magnitude " + accNormKmPerSec + " is outside expected range for LEO");
    }

    @Test
    void testAccelerationDecreasesWithAltitude() {

        // Computation date
        final AbsoluteDate date = new AbsoluteDate(2003, 3, 1, 13, 59, 27.816, TimeScalesFactory.getUTC());

        // Model
        final KnockeRediffusedForceModel forceModel = new KnockeRediffusedForceModel(CelestialBodyFactory.getSun(),
                new IsotropicRadiationSingleCoefficient(1.0, 1.5),
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                FastMath.toRadians(10));

        // Test at multiple altitudes
        final double[] altitudes = {400000, 800000, 2000000, 36000000};
        double previousAccNorm = Double.POSITIVE_INFINITY;

        for (final double altitude : altitudes) {
            // Create orbit
            final double r = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + altitude;
            final double v = FastMath.sqrt(Constants.WGS84_EARTH_MU / r);
            final Orbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(r, 0, 0), new Vector3D(0, v, 0)),
                    FramesFactory.getGCRF(), date, Constants.WGS84_EARTH_MU);

            // Compute acceleration
            final Vector3D acceleration = forceModel.acceleration(new SpacecraftState(orbit), forceModel.getParameters(date));
            final double accNorm = acceleration.getNorm();

            // Acceleration should decrease with altitude (See Montenbruck, Satellite Orbits, Fig 3.1)
            Assertions.assertTrue(accNorm < previousAccNorm,
                    "Acceleration at " + altitude + "m (" + accNorm +
                    ") should be less than at previous altitude (" + previousAccNorm + ")");

            previousAccNorm = accNorm;
        }
    }

    @Test
    void testFieldAccelerationConsistency() {

        // Initial field Keplerian orbit
        final AbsoluteDate realDate = new AbsoluteDate(2003, 3, 1, 13, 59, 27.816, TimeScalesFactory.getUTC());
        final Binary64 a = new Binary64(7e6);
        final Binary64 e = new Binary64(0.01);
        final Binary64 i = new Binary64(1.2);
        final Binary64 pa = new Binary64(0.7);
        final Binary64 raan = new Binary64(0.5);
        final Binary64 n = new Binary64(0.1);

        final Field<Binary64> field = a.getField();
        final Binary64 zero = field.getZero();
        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(field, realDate);
        final Frame frame = FramesFactory.getEME2000();

        final FieldKeplerianOrbit<Binary64> fieldOrbit =
                new FieldKeplerianOrbit<>(a, e, i, pa, raan, n,
                                          PositionAngleType.MEAN, frame, date,
                                          zero.add(Constants.EIGEN5C_EARTH_MU));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(fieldOrbit);

        // Model
        final KnockeRediffusedForceModel forceModel = new KnockeRediffusedForceModel(CelestialBodyFactory.getSun(),
                new IsotropicRadiationSingleCoefficient(1.0, 1.5),
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                FastMath.toRadians(15));

        // Acceleration
        final FieldVector3D<Binary64> fieldAcc = forceModel.acceleration(fieldState, forceModel.getParameters(field));
        final Vector3D realAcc = forceModel.acceleration(fieldState.toSpacecraftState(), forceModel.getParameters());

        // Verify
        Assertions.assertEquals(realAcc.getX(), fieldAcc.getX().getReal(), 1e-15);
        Assertions.assertEquals(realAcc.getY(), fieldAcc.getY().getReal(), 1e-15);
        Assertions.assertEquals(realAcc.getZ(), fieldAcc.getZ().getReal(), 1e-15);
    }

    /** Create a simple force model for testing albedo and emissivity computations. */
    private KnockeRediffusedForceModel createSimpleForceModel() {
        return new KnockeRediffusedForceModel(CelestialBodyFactory.getSun(),
                new IsotropicRadiationSingleCoefficient(1.0, 1.5),
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                FastMath.toRadians(30));
    }

    /** Knocke model specialized step handler. */
    private static class KnockeStepHandler implements OrekitFixedStepHandler {

        /** Knocke model. */
        private final KnockeRediffusedForceModel knockeModel;


        /** Simple constructor. */
        KnockeStepHandler(final KnockeRediffusedForceModel knockeModel) {

            this.knockeModel = knockeModel;
        }

        @Override
        public void handleStep(SpacecraftState currentState) {

            // Get Knocke model acceleration

            final Vector3D knockeAcceleration = knockeModel.acceleration(currentState, knockeModel.getParameters(currentState.getDate()));

            // Get radial direction
            final Vector3D radialUnit = currentState.getOrbit().getPosition().normalize();

            // Get along track direction
            final Vector3D velocity = currentState.getOrbit().getVelocity();
            final Vector3D alongTrackUnit = velocity.subtract(radialUnit.scalarMultiply(velocity.dotProduct(radialUnit))).normalize();

            // Get cross track direction
            final Vector3D crossTrackUnit = radialUnit.crossProduct(alongTrackUnit);

            // Get projected Knocke model acceleration values on 3 dimensions
            final double radialAcceleration     = knockeAcceleration.dotProduct(radialUnit);
            final double alongTrackAcceleration = knockeAcceleration.dotProduct(alongTrackUnit);
            final double crossTrackAcceleration = knockeAcceleration.dotProduct(crossTrackUnit);

            // Check values
            Assertions.assertEquals(5.0e-10, radialAcceleration, 5.0e-10);
            Assertions.assertEquals(0.0, alongTrackAcceleration, 2e-10);
            Assertions.assertEquals(0.0, crossTrackAcceleration, 2e-11);
        }

    }
}
