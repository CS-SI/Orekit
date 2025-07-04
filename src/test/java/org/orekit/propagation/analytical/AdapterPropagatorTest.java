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
package org.orekit.propagation.analytical;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.maneuvers.ConstantThrustManeuver;
import org.orekit.forces.maneuvers.SmallManeuverAnalyticalModel;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.*;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class AdapterPropagatorTest {

    @Test
    public void testLowEarthOrbit() throws ParseException, IOException {

        Orbit leo = new CircularOrbit(7200000.0, -1.0e-5, 2.0e-4,
                                      FastMath.toRadians(98.0),
                                      FastMath.toRadians(123.456),
                                      0.0, PositionAngleType.MEAN,
                                      FramesFactory.getEME2000(),
                                      new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                       new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC()),
                                      Constants.EIGEN5C_EARTH_MU);
        double mass     = 5600.0;
        AbsoluteDate t0 = leo.getDate().shiftedBy(1000.0);
        Vector3D dV     = new Vector3D(-0.1, 0.2, 0.3);
        double f        = 20.0;
        double isp      = 315.0;
        double vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
        double dt       = -(mass * vExhaust / f) * FastMath.expm1(-dV.getNorm() / vExhaust);
        BoundedPropagator withoutManeuver = getEphemeris(leo, mass, 5,
                                                         new LofOffset(leo.getFrame(), LOFType.LVLH),
                                                         t0, Vector3D.ZERO, f, isp,
                                                         false, false, null);
        BoundedPropagator withManeuver    = getEphemeris(leo, mass, 5,
                                                         new LofOffset(leo.getFrame(), LOFType.LVLH),
                                                         t0, dV, f, isp,
                                                         false, false, null);

        // we set up a model that reverts the maneuvers
        AdapterPropagator adapterPropagator = new AdapterPropagator(withManeuver);
        AdapterPropagator.DifferentialEffect effect =
                new SmallManeuverAnalyticalModel(adapterPropagator.propagate(t0), dV.negate(), isp);
        adapterPropagator.addEffect(effect);
        adapterPropagator.addAdditionalDataProvider(new AdditionalDataProvider<double[]>() {
            public String getName() {
                return "dummy 3";
            }
            public double[] getAdditionalData(SpacecraftState state) {
                return new double[3];
            }
        });

        // the adapted propagators do not manage the additional data from the reference,
        // they simply forward them
        Assertions.assertFalse(adapterPropagator.isAdditionalDataManaged("dummy 1"));
        Assertions.assertFalse(adapterPropagator.isAdditionalDataManaged("dummy 2"));
        Assertions.assertTrue(adapterPropagator.isAdditionalDataManaged("dummy 3"));

        for (AbsoluteDate t = t0.shiftedBy(0.5 * dt);
             t.compareTo(withoutManeuver.getMaxDate()) < 0;
             t = t.shiftedBy(60.0)) {
            PVCoordinates pvWithout  = withoutManeuver.getPVCoordinates(t, leo.getFrame());
            PVCoordinates pvReverted = adapterPropagator.getPVCoordinates(t, leo.getFrame());
            double revertError       = new PVCoordinates(pvWithout, pvReverted).getPosition().getNorm();
            Assertions.assertEquals(0, revertError, 0.45);
            Assertions.assertEquals(2, adapterPropagator.propagate(t).getAdditionalState("dummy 1").length);
            Assertions.assertEquals(1, adapterPropagator.propagate(t).getAdditionalState("dummy 2").length);
            Assertions.assertEquals(3, adapterPropagator.propagate(t).getAdditionalState("dummy 3").length);
        }

    }

    @Test
    public void testEccentricOrbit() throws ParseException, IOException {

        Orbit heo = new KeplerianOrbit(90000000.0, 0.92, FastMath.toRadians(98.0),
                                       FastMath.toRadians(12.3456),
                                       FastMath.toRadians(123.456),
                                       FastMath.toRadians(1.23456), PositionAngleType.MEAN,
                                       FramesFactory.getEME2000(),
                                       new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                        new TimeComponents(23, 30, 00.000),
                                                        TimeScalesFactory.getUTC()),
                                                        Constants.EIGEN5C_EARTH_MU);
        double mass     = 5600.0;
        AbsoluteDate t0 = heo.getDate().shiftedBy(1000.0);
        Vector3D dV     = new Vector3D(-0.01, 0.02, 0.03);
        double f        = 20.0;
        double isp      = 315.0;
        double vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
        double dt       = -(mass * vExhaust / f) * FastMath.expm1(-dV.getNorm() / vExhaust);
        BoundedPropagator withoutManeuver = getEphemeris(heo, mass, 5,
                                                         new LofOffset(heo.getFrame(), LOFType.LVLH),
                                                         t0, Vector3D.ZERO, f, isp,
                                                         false, false, null);
        BoundedPropagator withManeuver    = getEphemeris(heo, mass, 5,
                                                         new LofOffset(heo.getFrame(), LOFType.LVLH),
                                                         t0, dV, f, isp,
                                                         false, false, null);

        // we set up a model that reverts the maneuvers
        AdapterPropagator adapterPropagator = new AdapterPropagator(withManeuver);
        AdapterPropagator.DifferentialEffect effect =
                new SmallManeuverAnalyticalModel(adapterPropagator.propagate(t0), dV.negate(), isp);
        adapterPropagator.addEffect(effect);
        adapterPropagator.addAdditionalDataProvider(new AdditionalDataProvider<double[]>() {
            public String getName() {
                return "dummy 3";
            }
            public double[] getAdditionalData(SpacecraftState state) {
                return new double[3];
            }
        });

        // the adapted propagators do not manage the additional states from the reference,
        // they simply forward them
        Assertions.assertFalse(adapterPropagator.isAdditionalDataManaged("dummy 1"));
        Assertions.assertFalse(adapterPropagator.isAdditionalDataManaged("dummy 2"));
        Assertions.assertTrue(adapterPropagator.isAdditionalDataManaged("dummy 3"));

        for (AbsoluteDate t = t0.shiftedBy(0.5 * dt);
             t.compareTo(withoutManeuver.getMaxDate()) < 0;
             t = t.shiftedBy(300.0)) {
            PVCoordinates pvWithout  = withoutManeuver.getPVCoordinates(t, heo.getFrame());
            PVCoordinates pvReverted = adapterPropagator.getPVCoordinates(t, heo.getFrame());
            double revertError       = Vector3D.distance(pvWithout.getPosition(), pvReverted.getPosition());
            Assertions.assertEquals(0, revertError, 2.5e-5 * heo.getA());
            Assertions.assertEquals(2, adapterPropagator.propagate(t).getAdditionalState("dummy 1").length);
            Assertions.assertEquals(1, adapterPropagator.propagate(t).getAdditionalState("dummy 2").length);
            Assertions.assertEquals(3, adapterPropagator.propagate(t).getAdditionalState("dummy 3").length);
        }

    }

    @Test
    public void testNonKeplerian() throws ParseException, IOException {

        Orbit leo = new CircularOrbit(7204319.233600575, 4.434564637450575E-4, 0.0011736728299091088,
                                      1.7211611441767323, 5.5552084166959474,
                                      24950.321259193086, PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(),
                                      new AbsoluteDate(new DateComponents(2003, 9, 16),
                                                       new TimeComponents(23, 11, 20.264),
                                                       TimeScalesFactory.getUTC()),
                                      Constants.EIGEN5C_EARTH_MU);
        double mass     = 4093.0;
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2003, 9, 16),
                                           new TimeComponents(23, 14, 40.264),
                                           TimeScalesFactory.getUTC());
        Vector3D dV     = new Vector3D(0.0, 3.0, 0.0);
        double f        = 40.0;
        double isp      = 300.0;
        double vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
        double dt       = -(mass * vExhaust / f) * FastMath.expm1(-dV.getNorm() / vExhaust);
        // setup a specific coefficient file for gravity potential as it will also
        // try to read a corrupted one otherwise
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_05c_coef", false));
        NormalizedSphericalHarmonicsProvider gravityField = GravityFieldFactory.getNormalizedProvider(8, 8);
        BoundedPropagator withoutManeuver = getEphemeris(leo, mass, 10,
                                                         new LofOffset(leo.getFrame(), LOFType.VNC),
                                                         t0, Vector3D.ZERO, f, isp,
                                                         true, true, gravityField);
        BoundedPropagator withManeuver    = getEphemeris(leo, mass, 10,
                                                         new LofOffset(leo.getFrame(), LOFType.VNC),
                                                         t0, dV, f, isp,
                                                         true, true, gravityField);

        // we set up a model that reverts the maneuvers
        AdapterPropagator adapterPropagator = new AdapterPropagator(withManeuver);
        SpacecraftState state0 = adapterPropagator.propagate(t0);
        AdapterPropagator.DifferentialEffect directEffect =
                new SmallManeuverAnalyticalModel(state0, dV.negate(), isp);
        AdapterPropagator.DifferentialEffect derivedEffect =
                new J2DifferentialEffect(state0, directEffect, false,
                                         GravityFieldFactory.getUnnormalizedProvider(gravityField));
        adapterPropagator.addEffect(directEffect);
        adapterPropagator.addEffect(derivedEffect);
        adapterPropagator.addAdditionalDataProvider(new AdditionalDataProvider<double[]>() {
            public String getName() {
                return "dummy 3";
            }
            public double[] getAdditionalData(SpacecraftState state) {
                return new double[3];
            }
        });

        // the adapted propagators do not manage the additional states from the reference,
        // they simply forward them
        Assertions.assertFalse(adapterPropagator.isAdditionalDataManaged("dummy 1"));
        Assertions.assertFalse(adapterPropagator.isAdditionalDataManaged("dummy 2"));
        Assertions.assertTrue(adapterPropagator.isAdditionalDataManaged("dummy 3"));

        double maxDelta = 0;
        double maxNominal = 0;
        for (AbsoluteDate t = t0.shiftedBy(0.5 * dt);
             t.compareTo(withoutManeuver.getMaxDate()) < 0;
             t = t.shiftedBy(600.0)) {
            PVCoordinates pvWithout  = withoutManeuver.getPVCoordinates(t, leo.getFrame());
            PVCoordinates pvWith     = withManeuver.getPVCoordinates(t, leo.getFrame());
            PVCoordinates pvReverted = adapterPropagator.getPVCoordinates(t, leo.getFrame());
            double nominal           = new PVCoordinates(pvWithout, pvWith).getPosition().getNorm();
            double revertError       = new PVCoordinates(pvWithout, pvReverted).getPosition().getNorm();
            maxDelta = FastMath.max(maxDelta, revertError);
            maxNominal = FastMath.max(maxNominal, nominal);
            Assertions.assertEquals(2, adapterPropagator.propagate(t).getAdditionalState("dummy 1").length);
            Assertions.assertEquals(1, adapterPropagator.propagate(t).getAdditionalState("dummy 2").length);
            Assertions.assertEquals(3, adapterPropagator.propagate(t).getAdditionalState("dummy 3").length);
        }
        Assertions.assertTrue(maxDelta   < 120);
        Assertions.assertTrue(maxNominal > 2800);

    }

    /** Error with specific propagators & additional state provider throwing a NullPointerException when propagating */
    @Test
    public void testIssue949() {
        // GIVEN
        final AbsoluteDate initialDate  = new AbsoluteDate();
        final Orbit        initialOrbit = TestUtils.getDefaultOrbit(initialDate);

        // Setup propagator
        final Orbit                 finalOrbit = initialOrbit.shiftedBy(1);
        final List<SpacecraftState> states     = new ArrayList<>();
        states.add(new SpacecraftState(initialOrbit));
        states.add(new SpacecraftState(finalOrbit));

        final Ephemeris ephemeris = new Ephemeris(states, 2);
        final AdapterPropagator adapterPropagator = new AdapterPropagator(ephemeris);

        // Setup additional data provider which use the initial state in its init method
        final AdditionalDataProvider<double[]> additionalDataProvider = TestUtils.getAdditionalProviderWithInit();
        adapterPropagator.addAdditionalDataProvider(additionalDataProvider);

        // WHEN & THEN
        Assertions.assertDoesNotThrow(() -> adapterPropagator.propagate(finalOrbit.getDate()), "No error should have been thrown");

    }

    private BoundedPropagator getEphemeris(final Orbit orbit, final double mass, final int nbOrbits,
                                           final AttitudeProvider law,
                                           final AbsoluteDate t0, final Vector3D dV,
                                           final double f, final double isp,
                                           final boolean sunAttraction, final boolean moonAttraction,
                                           final NormalizedSphericalHarmonicsProvider gravityField)
        throws ParseException, IOException {

        SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(), orbit.getFrame())).withMass(mass);

        // add some dummy additional states
        initialState = initialState.addAdditionalData("dummy 1", new double[]{1.25, 2.5});
        initialState = initialState.addAdditionalData("dummy 2", 5.0);

        // set up numerical propagator
        final double dP = 1.0;
        double[][] tolerances = ToleranceProvider.getDefaultToleranceProvider(dP).getTolerances(orbit, orbit.getType());
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, tolerances[0], tolerances[1]);
        integrator.setInitialStepSize(orbit.getKeplerianPeriod() / 100.0);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.addAdditionalDataProvider(new AdditionalDataProvider<Double>() {
            public String getName() {
                return "dummy 2";
            }
            public Double getAdditionalData(SpacecraftState state) {
                return 5.0;
            }
        });
        propagator.setInitialState(initialState);
        propagator.setAttitudeProvider(law);

        if (dV.getNorm() > 1.0e-6) {
            // set up maneuver
            final double vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
            final double dt = -(mass * vExhaust / f) * FastMath.expm1(-dV.getNorm() / vExhaust);
            final ConstantThrustManeuver maneuver =
                    new ConstantThrustManeuver(t0.shiftedBy(-0.5 * dt), dt , f, isp, dV.normalize());
            propagator.addForceModel(maneuver);
        }

        if (sunAttraction) {
            propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        }

        if (moonAttraction) {
            propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));
        }

        if (gravityField != null) {
            propagator.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getGTOD(false),
                                                                           gravityField));
        }

        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(t0.shiftedBy(nbOrbits * orbit.getKeplerianPeriod()));

        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        // both the initial propagator and generated ephemeris manage one of the two
        // additional states, but they also contain unmanaged copies of the other one
        Assertions.assertFalse(propagator.isAdditionalDataManaged("dummy 1"));
        Assertions.assertTrue(propagator.isAdditionalDataManaged("dummy 2"));
        Assertions.assertFalse(ephemeris.isAdditionalDataManaged("dummy 1"));
        Assertions.assertTrue(ephemeris.isAdditionalDataManaged("dummy 2"));
        Assertions.assertEquals(2, ephemeris.getInitialState().getAdditionalState("dummy 1").length);
        Assertions.assertEquals(1, ephemeris.getInitialState().getAdditionalState("dummy 2").length);

        return ephemeris;

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/icgem-format");
    }

}
