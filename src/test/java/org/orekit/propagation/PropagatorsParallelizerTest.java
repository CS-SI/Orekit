/* Copyright 2002-2022 CS GROUP
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
package org.orekit.propagation;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class PropagatorsParallelizerTest {

    @Test
    public void testIssue717() {

        // Gravity
        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getUnnormalizedProvider(8, 8);

        // Orbit
        Orbit orbit = new KeplerianOrbit(15000000.0, 0.125, 1.25,
                                         0.250, 1.375, 0.0625, PositionAngle.MEAN,
                                         FramesFactory.getEME2000(),
                                         new AbsoluteDate(2000, 2, 24, 11, 35, 47.0, TimeScalesFactory.getUTC()),
                                         gravity.getMu());


        // Propagator
        final double[][] tol = DSSTPropagator.tolerances(0.01, orbit);
        final DSSTPropagator propagator = new DSSTPropagator(new DormandPrince853Integrator(0.01, 600.0, tol[0], tol[1]), PropagationType.OSCULATING);

        // Force models
        final DSSTForceModel zonal = new DSSTZonal(gravity, 4, 3, 9);
        propagator.addForceModel(zonal);

        propagator.setInitialState(new SpacecraftState(orbit));

        // Configure epochs in order to have a backward propagation mode
        final double deltaT = 30.0;

        final PropagatorsParallelizer parallelizer =
                        new PropagatorsParallelizer(Arrays.asList(propagator),  interpolators -> {interpolators.get(0).getCurrentState().getDate();});

        final SpacecraftState state = parallelizer.propagate(orbit.getDate().shiftedBy(deltaT).shiftedBy(+1.0), orbit.getDate().shiftedBy(-2.0 * deltaT).shiftedBy(-1.0)).get(0);

        // Verify that the backward propagation worked properly
        Assert.assertNotNull(state);

    }

    @Test
    public void testNumericalNotInitialized() {

        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(3600.0);
        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNotInitializedNumerical());

        PropagatorsParallelizer parallelizer =
                        new PropagatorsParallelizer(propagators, interpolators -> Assert.fail("should not be called"));
        try {
            parallelizer.propagate(startDate, endDate);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION,
                                oe.getSpecifier());
        }

    }

    @Test
    public void testAnalyticalAndNumericalSameOrbit() {

        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(3600.0);
        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNumerical());

        PropagatorsParallelizer parallelizer =
                        new PropagatorsParallelizer(propagators,
                                                    interpolators -> {
                                                        Assert.assertEquals(2, interpolators.size());
                                                        AbsoluteDate aPrev = interpolators.get(0).getPreviousState().getDate();
                                                        AbsoluteDate aCurr = interpolators.get(0).getCurrentState().getDate();
                                                        AbsoluteDate nPrev = interpolators.get(1).getPreviousState().getDate();
                                                        AbsoluteDate nCurr = interpolators.get(1).getCurrentState().getDate();
                                                        Assert.assertEquals(0.0, aPrev.durationFrom(nPrev), 3.0e-13);
                                                        Assert.assertEquals(0.0, aCurr.durationFrom(nCurr), 3.0e-13);
                                                        Vector3D aPos = interpolators.get(0).getCurrentState().getPVCoordinates().getPosition();
                                                        Vector3D nPos = interpolators.get(1).getCurrentState().getPVCoordinates().getPosition();
                                                        Assert.assertTrue(Vector3D.distance(aPos, nPos) < 111.0);
                                                    });
        List<SpacecraftState> results = parallelizer.propagate(startDate, endDate);

        Assert.assertEquals(2, results.size());
        for (final SpacecraftState state : results) {
            Assert.assertEquals(0.0, state.getDate().durationFrom(endDate), 1.0e-15);
        }

    }

    @Test
    public void testVsAnalyticalMonoSat() {

        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(3600.0);
        Propagator mono = buildEcksteinHechler();
        final EphemerisGenerator generator = mono.getEphemerisGenerator();
        mono.propagate(startDate, endDate);
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNumerical());

        PropagatorsParallelizer parallelizer =
                        new PropagatorsParallelizer(propagators,
                                                    interpolators -> {
                                                        AbsoluteDate aCurr = interpolators.get(0).getCurrentState().getDate();
                                                        Vector3D aPos = interpolators.get(0).getCurrentState().getPVCoordinates().getPosition();
                                                        Vector3D ePos = ephemeris.getPVCoordinates(aCurr, orbit.getFrame()).getPosition();
                                                        Assert.assertEquals(0, Vector3D.distance(ePos, aPos), 1.0e-15);
                                                    });
        List<SpacecraftState> results = parallelizer.propagate(startDate, endDate);

        Assert.assertEquals(2, parallelizer.getPropagators().size());
        Assert.assertEquals(2, results.size());
        for (final SpacecraftState state : results) {
            Assert.assertEquals(0.0, state.getDate().durationFrom(endDate), 1.0e-15);
        }

    }

    @Test
    public void testVsNumericalMonoSat() {

        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(3600.0);
        Propagator mono = buildNumerical();
        final EphemerisGenerator generator = mono.getEphemerisGenerator();
        mono.propagate(startDate, endDate);
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNumerical());

        PropagatorsParallelizer parallelizer =
                        new PropagatorsParallelizer(propagators,
                                                    interpolators -> {
                                                        AbsoluteDate nCurr = interpolators.get(1).getCurrentState().getDate();
                                                        Vector3D nPos = interpolators.get(1).getCurrentState().getPVCoordinates().getPosition();
                                                        Vector3D ePos = ephemeris.getPVCoordinates(nCurr, orbit.getFrame()).getPosition();
                                                        Assert.assertEquals(0, Vector3D.distance(ePos, nPos), 1.0e-15);
                                                    });
        List<SpacecraftState> results = parallelizer.propagate(startDate, endDate);

        Assert.assertEquals(2, results.size());
        for (final SpacecraftState state : results) {
            Assert.assertEquals(0.0, state.getDate().durationFrom(endDate), 1.0e-15);
        }

    }

    @Test
    public void testOrekitException() {
        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(3600.0);
        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNumerical());
        propagators.get(0).addEventDetector(new DateDetector(startDate.shiftedBy(900.0)).
                                            withHandler((state, detector, increasing) -> {
                                                            throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                                                      "inTest");
                                                        }));
        try {
            new PropagatorsParallelizer(propagators, interpolators -> {}).propagate(startDate, endDate);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertNull(oe.getCause());
            Assert.assertEquals(LocalizedCoreFormats.SIMPLE_MESSAGE, oe.getSpecifier());
            Assert.assertEquals("inTest", (String) oe.getParts()[0]);
        }
    }

    @Test
    public void testEarlyStop() {
        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(3600.0);
        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNumerical());
        propagators.get(0).addEventDetector(new DateDetector(startDate.shiftedBy(900.0)).
                                            withHandler((state, detector, increasing) -> {
                                                            throw new RuntimeException("boo!");
                                                        }));
        try {
            new PropagatorsParallelizer(propagators, interpolators -> {}).propagate(startDate, endDate);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertNotNull(oe.getCause());
            Assert.assertTrue(oe.getCause() instanceof RuntimeException);
            Assert.assertEquals(LocalizedCoreFormats.SIMPLE_MESSAGE, oe.getSpecifier());
            Assert.assertTrue(((String) oe.getParts()[0]).endsWith("boo!"));
        }
    }

    @Test
    public void testStopOnEarlyEvent() {
        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(3600.0);
        final AbsoluteDate stopDate  = startDate.shiftedBy(0.01);
        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNumerical());
        propagators.get(0).addEventDetector(new DateDetector(stopDate).withHandler(new StopOnEvent<>()));
        List<SpacecraftState> results = new PropagatorsParallelizer(propagators, interpolators -> {}).
                                        propagate(startDate, endDate);
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(0.0, results.get(0).getDate().durationFrom(stopDate), 1.0e-15);
        Assert.assertEquals(0.0, results.get(1).getDate().durationFrom(stopDate), 1.0e-15);
    }

    @Test
    public void testStopOnLateEvent() {
        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(3600.0);
        final AbsoluteDate stopDate  = startDate.shiftedBy(900.0);
        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNumerical());
        propagators.get(0).addEventDetector(new DateDetector(stopDate).withHandler(new StopOnEvent<>()));
        List<SpacecraftState> results = new PropagatorsParallelizer(propagators, interpolators -> {}).
                                        propagate(startDate, endDate);
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(0.0, results.get(0).getDate().durationFrom(stopDate), 1.0e-15);
        Assert.assertEquals(0.0, results.get(1).getDate().durationFrom(stopDate), 1.0e-15);
    }

    @Test
    public void testInternalStepHandler() {
        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(3600.0);
        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNumerical());
        final AtomicInteger called0 = new AtomicInteger();
        propagators.get(0).getMultiplexer().add(interpolator -> called0.set(11));
        final AtomicInteger called1 = new AtomicInteger();
        propagators.get(1).getMultiplexer().add(interpolator -> called1.set(22));
        List<SpacecraftState> results = new PropagatorsParallelizer(propagators, interpolators -> {}).
                                                                    propagate(startDate, endDate);
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(0.0, results.get(0).getDate().durationFrom(endDate), 1.0e-15);
        Assert.assertEquals(0.0, results.get(1).getDate().durationFrom(endDate), 1.0e-15);
        Assert.assertEquals(11, called0.get());
        Assert.assertEquals(22, called1.get());
    }

    @Test
    public void testIntegrableGenerator() {
        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(3600.0);
        final NumericalPropagator p0 = buildNumerical();
        final NumericalPropagator p1 = buildNumerical();
        final String name = "generator";
        final double base0 = 2.0e-3;
        final double base1 = 2.5e-3;
        p0.addAdditionalDerivativesProvider(new Exponential(name, base0));
        p0.setInitialState(p0.getInitialState().addAdditionalState(name, 1.0));
        p1.addAdditionalDerivativesProvider(new Exponential(name, base1));
        p1.setInitialState(p1.getInitialState().addAdditionalState(name, 1.0));
        List<SpacecraftState> results = new PropagatorsParallelizer(Arrays.asList(p0, p1), interpolators -> {}).
                                        propagate(startDate, endDate);
        double expected0 = FastMath.exp(base0 * endDate.durationFrom(startDate));
        double expected1 = FastMath.exp(base1 * endDate.durationFrom(startDate));
        Assert.assertEquals(expected0, results.get(0).getAdditionalState(name)[0], 6.0e-9 * expected0);
        Assert.assertEquals(expected1, results.get(1).getAdditionalState(name)[0], 5.0e-8 * expected1);
    }

    private static class Exponential implements AdditionalDerivativesProvider {
        final String name;
        final double base;
        Exponential(final String name, final double base) {
            this.name = name;
            this.base = base;
        }
        public String getName() {
            return name;
        }
        public boolean yield(final SpacecraftState state) {
            return !state.hasAdditionalState(name);
        }
        public double[] derivatives(SpacecraftState s) {
            return null;
        }
        public CombinedDerivatives combinedDerivatives(SpacecraftState state) {
            return new CombinedDerivatives(new double[] { base * state.getAdditionalState(name)[0] },
                                           null);
        }
        public int getDimension() {
            return 1;
        }
    }

    private EcksteinHechlerPropagator buildEcksteinHechler() {
        return new EcksteinHechlerPropagator(orbit, attitudeLaw, mass, unnormalizedGravityField);
    }

    private NumericalPropagator buildNumerical() {
        NumericalPropagator numericalPropagator = buildNotInitializedNumerical();
        numericalPropagator.setInitialState(new SpacecraftState(orbit,
                                                                attitudeLaw.getAttitude(orbit,
                                                                                        orbit.getDate(),
                                                                                        orbit.getFrame()),
                                                                mass));
        return numericalPropagator;
    }

    private NumericalPropagator buildNotInitializedNumerical() {
        OrbitType type = OrbitType.CARTESIAN;
        double minStep = 0.001;
        double maxStep = 300;
        double[][] tolerances = NumericalPropagator.tolerances(10.0, orbit, type);
        ODEIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tolerances[0], tolerances[1]);
        NumericalPropagator numericalPropagator = new NumericalPropagator(integrator);
        ForceModel gravity = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                                   normalizedGravityField);
        numericalPropagator.addForceModel(gravity);
        return numericalPropagator;
    }

    @Before
    public void setUp() {
        try {
        Utils.setDataRoot("regular-data:potential/icgem-format");
        unnormalizedGravityField = GravityFieldFactory.getUnnormalizedProvider(6, 0);
        normalizedGravityField   = GravityFieldFactory.getNormalizedProvider(6, 0);

        mass = 2500;
        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv = 0;

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                 TimeComponents.H00,
                                                 TimeScalesFactory.getUTC());
        orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, normalizedGravityField.getMu());
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);

        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        mass                     = Double.NaN;
        orbit                    = null;
        attitudeLaw              = null;
        unnormalizedGravityField = null;
        normalizedGravityField   = null;
    }

    private double mass;
    private Orbit orbit;
    private AttitudeProvider attitudeLaw;
    private UnnormalizedSphericalHarmonicsProvider unnormalizedGravityField;
    private NormalizedSphericalHarmonicsProvider normalizedGravityField;

}
