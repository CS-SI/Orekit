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
import java.util.stream.Collectors;

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
import org.orekit.propagation.numerical.NumericalPropagator;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class PropagatorsParallelizerSeveralEphemerisTest {

    /*
     * Test set to check that ephemeris are produced at the end of the propagation parallelizer
     * in the case of NuericalPropagators.
     * Check existence of all ephemeris.
     * Check end time of all ephemeris for varying step times.
     * 
     * Should validate the modification, except for the retrieveNextParameters added check.
     * 
     * Tests are based on Anne-Laure LUGAN's proposed test, and on PropagatorsParallelizerTest.
     */
    
    @Test
    public void testSeveralEphemeris() {
        final AbsoluteDate startDate = orbit.getDate();
        final AbsoluteDate endDate = startDate.shiftedBy(3600.0);
        List<Propagator> propagators = Arrays.asList(buildNumerical(), buildNumerical(), buildNumerical());
        List<EphemerisGenerator> generators = propagators.stream().map(Propagator::getEphemerisGenerator).collect(Collectors.toList());
        PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, interpolators -> {
            // Do nothing
        });

        parallelizer.propagate(startDate, endDate);
        generators.stream().forEach(generator -> Assert.assertNotNull(generator.getGeneratedEphemeris()));

    }

    @Test
    public void testSeveralEphemeris2() {
        final AbsoluteDate startDate = orbit.getDate();
        final AbsoluteDate endDate = startDate.shiftedBy(3600.015);
        List<Propagator> propagators = Arrays.asList(buildNumerical(1,300), buildNumerical(0.1,300), buildNumerical(0.001,300), buildEcksteinHechler());
        List<EphemerisGenerator> generators = propagators.stream().map(Propagator::getEphemerisGenerator).collect(Collectors.toList());
        PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, interpolators -> {
            // Do nothing
        });

        parallelizer.propagate(startDate, endDate);
        generators.stream().forEach(generator -> Assert.assertNotNull(generator.getGeneratedEphemeris()));
        AbsoluteDate lastDate = generators.get(0).getGeneratedEphemeris().getMaxDate();
        AbsoluteDate firstDate = generators.get(0).getGeneratedEphemeris().getMinDate();
        for ( EphemerisGenerator generator : generators ) {
            Assert.assertEquals(lastDate, generator.getGeneratedEphemeris().getMaxDate());
            Assert.assertEquals(firstDate, generator.getGeneratedEphemeris().getMinDate());
        }
    }
    
    @Test
    public void testSeveralEphemerisDateDetector() {
        /*
         * On observe un bug pour l'analytical propagator, qui quand il n'est pas en première position
         * va provoquer un problème en indiquant comme date de fin, non pas la date de l'évènement
         */
        final AbsoluteDate startDate = orbit.getDate();
        final AbsoluteDate endDate = startDate.shiftedBy(3600);
        List<Propagator> propagators = Arrays.asList(buildNumerical(1,300), buildNumerical(0.1,300), buildNumerical(0.001,300));
        DateDetector dateDetect = new DateDetector(startDate.shiftedBy(1800));
        
        propagators.stream().forEach(propagator -> propagator.addEventDetector(dateDetect));

        List<EphemerisGenerator> generators = propagators.stream().map(Propagator::getEphemerisGenerator).collect(Collectors.toList());
        PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, interpolators -> {
            // Do nothing
        });

        parallelizer.propagate(startDate, endDate);
        generators.stream().forEach(generator -> Assert.assertNotNull(generator.getGeneratedEphemeris()));
        AbsoluteDate firstDate = generators.get(0).getGeneratedEphemeris().getMinDate();
        AbsoluteDate lastDate = generators.get(0).getGeneratedEphemeris().getMaxDate();

        for ( EphemerisGenerator generator : generators ) {
            Assert.assertEquals(firstDate, generator.getGeneratedEphemeris().getMinDate());
            Assert.assertEquals(lastDate, generator.getGeneratedEphemeris().getMaxDate());
        }
    }
    

    
    private EcksteinHechlerPropagator buildEcksteinHechler() {
        return new EcksteinHechlerPropagator(orbit, attitudeLaw, mass, unnormalizedGravityField);
    }

    
    private NumericalPropagator buildNumerical() {
        return buildNumerical(0.001,300);
    }
    
    private NumericalPropagator buildNumerical(double minStep, double maxStep) {
        NumericalPropagator numericalPropagator = buildNotInitializedNumerical(minStep, maxStep);
        numericalPropagator.setInitialState(new SpacecraftState(orbit,
                                                                attitudeLaw.getAttitude(orbit,
                                                                                        orbit.getDate(),
                                                                                        orbit.getFrame()),
                                                                mass));
        return numericalPropagator;
    }

    
    private NumericalPropagator buildNotInitializedNumerical(double minStep, double maxStep) {
        OrbitType type = OrbitType.CARTESIAN;
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
