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
package org.orekit.propagation;

import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
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
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.DateDetector;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class PropagatorsParallelizerEphemerisTest {

    private double mass;
    private Orbit orbit;
    private AttitudeProvider attitudeLaw;
    private UnnormalizedSphericalHarmonicsProvider unnormalizedGravityField;
    private NormalizedSphericalHarmonicsProvider normalizedGravityField;

    @BeforeEach
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
            orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                    FramesFactory.getEME2000(), date, normalizedGravityField.getMu());

            OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING,
                    FramesFactory.getITRF(IERSConventions.IERS_2010, true));
            attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);

        } catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        mass                     = Double.NaN;
        orbit                    = null;
        attitudeLaw              = null;
        unnormalizedGravityField = null;
        normalizedGravityField   = null;
    }



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
        /*
         * The closing behaviour is checked by verifying the presence of generated ephemeris as a result of the
         * following process, using PropagatorsParallelizer.
         */
        final AbsoluteDate startDate = orbit.getDate();
        final AbsoluteDate endDate = startDate.shiftedBy(3600);

        List<Propagator> propagators = Arrays.asList(buildNumerical(), buildNumerical(), buildDSST(), buildEcksteinHechler());
        List<EphemerisGenerator> generators = propagators.stream().map(Propagator::getEphemerisGenerator).collect(Collectors.toList());
        PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, interpolators -> {
            // Do nothing
        });

        parallelizer.propagate(startDate, endDate);
        for ( EphemerisGenerator generator : generators ) {
            Assertions.assertNotNull(generator.getGeneratedEphemeris());
            Assertions.assertEquals(endDate, generator.getGeneratedEphemeris().getMaxDate());
            Assertions.assertEquals(startDate, generator.getGeneratedEphemeris().getMinDate());
        }
    }

    @Test
    public void testSeveralEphemerisDateDetector() {
        /*
         * The closing behaviour is checked for a stop event occuring during the propagation.
         * The test isn't applied to analytical propagators as their behaviour differs.
         * (Analytical propagator's ephemerides are the analytical propagators.)
         */
        final AbsoluteDate startDate = orbit.getDate();
        final AbsoluteDate endDate = startDate.shiftedBy(3600.015);
        List<Propagator> propagators = Arrays.asList(buildNumerical(1,300), buildNumerical(0.001,300), buildDSST());

        // Add new instance of event with same date. DateDetector behaviour at event is stop.
        AbsoluteDate detectorDate = startDate.shiftedBy(1800);
        propagators.stream().forEach(propagator -> propagator.addEventDetector(new DateDetector(detectorDate)));

        List<EphemerisGenerator> generators = propagators.stream().map(Propagator::getEphemerisGenerator).collect(Collectors.toList());
        PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, interpolators -> {
            // Do nothing
        });

        parallelizer.propagate(startDate, endDate);

        // Check for all generators
        for ( EphemerisGenerator generator : generators ) {
            Assertions.assertNotNull(generator.getGeneratedEphemeris());
            Assertions.assertEquals(startDate, generator.getGeneratedEphemeris().getMinDate());
            Assertions.assertEquals(detectorDate, generator.getGeneratedEphemeris().getMaxDate());
        }
    }



    private EcksteinHechlerPropagator buildEcksteinHechler() {
        return new EcksteinHechlerPropagator(orbit, attitudeLaw, mass, unnormalizedGravityField);
    }

    private DSSTPropagator buildDSST(final double minStep, final double maxStep) {
        // Gravity
        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getUnnormalizedProvider(8, 8);

        // Propagator
        final double[][] tol = DSSTPropagator.tolerances(0.01, orbit);
        final DSSTPropagator propagator = new DSSTPropagator(new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]), PropagationType.MEAN);

        // Force models
        final DSSTForceModel zonal = new DSSTZonal(gravity, 4, 3, 9);
        propagator.addForceModel(zonal);

        propagator.setInitialState(new SpacecraftState(orbit));

        return propagator;
    }

    private DSSTPropagator buildDSST() {
        return buildDSST(0.01,300);
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


}
