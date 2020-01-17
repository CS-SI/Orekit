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
package org.orekit.propagation;


import java.util.Arrays;
import java.util.List;

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
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class PropagatorsParallelizerTest {

    @Test
    public void testNumericalNotInitialized() {

        final AbsoluteDate startDate =  orbit.getDate();
        final AbsoluteDate endDate   = startDate.shiftedBy(3600.0);
        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNotInitializedNumerical());

        PropagatorsParallelizer parallelizer =
                        new PropagatorsParallelizer(propagators,
                                    (interpolators, islast) -> Assert.fail("should not be called"));
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
                                    (interpolators, islast) -> {
                                        Assert.assertEquals(2, interpolators.size());
                                        AbsoluteDate aPrev = interpolators.get(0).getPreviousState().getDate();
                                        AbsoluteDate aCurr = interpolators.get(0).getCurrentState().getDate();
                                        AbsoluteDate nPrev = interpolators.get(1).getPreviousState().getDate();
                                        AbsoluteDate nCurr = interpolators.get(1).getCurrentState().getDate();
                                        Assert.assertEquals(0.0, aPrev.durationFrom(nPrev), 1.0e-15);
                                        Assert.assertEquals(0.0, aCurr.durationFrom(nCurr), 1.0e-15);
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
        mono.setEphemerisMode();
        mono.propagate(startDate, endDate);
        final BoundedPropagator ephemeris = mono.getGeneratedEphemeris();

        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNumerical());

        PropagatorsParallelizer parallelizer =
                        new PropagatorsParallelizer(propagators,
                                    (interpolators, islast) -> {
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
        mono.setEphemerisMode();
        mono.propagate(startDate, endDate);
        final BoundedPropagator ephemeris = mono.getGeneratedEphemeris();

        List<Propagator> propagators = Arrays.asList(buildEcksteinHechler(),
                                                     buildNumerical());

        PropagatorsParallelizer parallelizer =
                        new PropagatorsParallelizer(propagators,
                                    (interpolators, islast) -> {
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
            new PropagatorsParallelizer(propagators, (interpolators, isLast) -> {}).propagate(startDate, endDate);
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
            new PropagatorsParallelizer(propagators, (interpolators, isLast) -> {}).propagate(startDate, endDate);
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
        List<SpacecraftState> results = new PropagatorsParallelizer(propagators, (interpolators, isLast) -> {}).
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
        List<SpacecraftState> results = new PropagatorsParallelizer(propagators, (interpolators, isLast) -> {}).
                                        propagate(startDate, endDate);
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(0.0, results.get(0).getDate().durationFrom(stopDate), 1.0e-15);
        Assert.assertEquals(0.0, results.get(1).getDate().durationFrom(stopDate), 1.0e-15);
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
