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
package org.orekit.forces.radiation;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
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
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;

/**
 * This force model was developed according to "EARTH RADIATION PRESSURE EFFECTS ON SATELLITES", 1988, by P. C. Knocke, J. C. Ries, and B. D. Tapley.
 * It was confronted to the results that are presented in this paper and reached satisfying performances.
 * However, the complete reproduction of the LAGEOS-1 test case is much too long for it to be implemented in test class.
 * Then, only
 */
public class KnockeRediffusedForceModelTest extends AbstractForceModelTest{

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }


    @Test
    public void testJacobianVsFiniteDifferences() {

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

        // Sun
        final ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

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
    public void testParameterIsotropicSingle() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        // Sun
        final ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        // Radiation sensitive model
        final RadiationSensitive radiationSensitive = new IsotropicRadiationSingleCoefficient(1, 1.5);

        // Set up the force model to test
        final KnockeRediffusedForceModel forceModel = new KnockeRediffusedForceModel(sun,
                                                                                     radiationSensitive,
                                                                                     Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                                     FastMath.toRadians(30));

        checkParameterDerivative(state, forceModel, RadiationSensitive.REFLECTION_COEFFICIENT, 0.25, 1.8e-16);

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
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);

        NumericalPropagator propagator =
                new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                       tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);

        // Sun
        final ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

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
    public void testRealField() {

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
        final ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

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
                                  1.0e-30, 1.3e-8, 6.7e-11, 1.4e-10,
                                  1, false);
    }


    @Test
    public void testRealFieldGradient() {

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
        final ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

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
                                          1.0e-30, 1.3e-2, 9.6e-5, 1.4e-4,
                                          1, false);
    }

    /** Roughtly compare Knocke model accelerations against results from "EARTH RADIATION PRESSURE EFFECTS ON SATELLITES",
     *  1988, by P. C. Knocke, J. C. Ries, and B. D. Tapley.
     *  The case is as close as possible from what it might be in the paper. Orbit and date have been artifically set so that the angle between
     *  the orbital plan and Earth-Sun direction is almost equal to zero.
     */
    @Test
    public void testRoughtAcceleration() {

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
        final SpacecraftState initState = new SpacecraftState(keplerian, mass);

        // Celestial objects

        // Sun
        final ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();

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
        final double[][] tolerances =
                        NumericalPropagator.tolerances(positionTolerance, keplerian, OrbitType.KEPLERIAN);
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(minStep, maxStep, tolerances[0], tolerances[1]);

        final NumericalPropagator propagator = new NumericalPropagator(integrator);

        propagator.setInitialState(initState);
        propagator.addForceModel(knockeModel);
        propagator.setStepHandler(handlerStep, new KnockeStepHandler(knockeModel));

        final SpacecraftState finalState = propagator.propagate(date0.shiftedBy(duration));

        Assertions.assertTrue(finalState.getDate().equals(date0.shiftedBy(duration)));
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
            final Vector3D velocity = currentState.getOrbit().getPVCoordinates().getVelocity();
            final Vector3D alongTrackUnit = velocity.subtract(radialUnit.scalarMultiply(velocity.dotProduct(radialUnit))).normalize();

            // Get cross track direction
            final Vector3D crossTrackUnit = radialUnit.crossProduct(alongTrackUnit);

            // Get projected Knocke model acceleration values on 3 dimensions
            final double radialAcceleration     = knockeAcceleration.dotProduct(radialUnit);
            final double alongTrackAcceleration = knockeAcceleration.dotProduct(alongTrackUnit);
            final double crossTrackAcceleration = knockeAcceleration.dotProduct(crossTrackUnit);

            // Check values
            Assertions.assertEquals(2.5e-10, radialAcceleration, 1.5e-10);
            Assertions.assertEquals(0.0, alongTrackAcceleration, 5e-11);
            Assertions.assertEquals(0.0, crossTrackAcceleration, 5e-12);
        }

    }
}
