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

package org.orekit.models.earth.atmosphere.data;

import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.JB2008;
import org.orekit.models.earth.atmosphere.JB2008InputParameters;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.orekit.OrekitMatchers.closeTo;
import static org.orekit.OrekitMatchers.pvCloseTo;

/*
 * Test code based on the CssiSpaceWeatherDataTest class
 * by Clément Jonglez.
 *
 * @author Louis Aucouturier
 * @since 11.2
 */

public class SOLFSMYDataLoaderTest {

    private TimeScale utc;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere");
        utc = TimeScalesFactory.getUTC();
    }

    // DataLoader
    private JB2008SpaceEnvironmentData loadJB() {
        JB2008SpaceEnvironmentData JBData = new JB2008SpaceEnvironmentData("SOLFSMY_trunc.txt", "DTCFILE_trunc.TXT");
        return JBData;
    }


    @Test
    public void testNoDataException() {
        try {
            new JB2008SpaceEnvironmentData("SOLFSMY_nodata.txt", "DTCFILE_trunc.TXT");
            Assertions.fail("No Data In File exception should have been raised");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_DATA_IN_FILE, oe.getSpecifier());
        }
    }

    @Test
    public void tesDuplicatedEntry() {
        JB2008SpaceEnvironmentData JBData = new JB2008SpaceEnvironmentData("SOLFSMY_double.txt", "DTCFILE_trunc.TXT");
        final AbsoluteDate julianDate = AbsoluteDate.createJDDate(2453000, Constants.JULIAN_DAY, utc);
        assertThat(137.2, closeTo(JBData.getF10(julianDate), 1e-10));
    }

    @Test
    public void testUnableParse() {
        try {
            new JB2008SpaceEnvironmentData("SOLFSMY_badparse.txt", "DTCFILE_trunc.TXT");
            Assertions.fail("UNABLE_TO_PARSE_LINE_IN_FILE exception should have been raised");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
        }
    }



    @Test
    public void testMinDate() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        final AbsoluteDate startDate = new AbsoluteDate(2003, 12, 26, 12, 0, 0.0, utc);
        Assertions.assertEquals(startDate, JBData.getMinDate());
    }

    @Test
    public void testMaxDate() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        final AbsoluteDate lastDate = new AbsoluteDate(2007, 1, 1, 12, 0, 0.0, utc);
        Assertions.assertEquals(lastDate, JBData.getMaxDate());
    }

    @Test
    public void testF10Interp() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        final AbsoluteDate julianDate = AbsoluteDate.createJDDate(2453006, 12*3600, utc);
        assertThat((116.0 + 105.6)/2, closeTo(JBData.getF10(julianDate), 1e-10));
    }

    @Test
    public void testF10() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        final AbsoluteDate julianDate = AbsoluteDate.createJDDate(2453006, 0, utc);
        assertThat(105.6, closeTo(JBData.getF10(julianDate), 1e-10));
    }

    @Test
    public void testF10B() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        final AbsoluteDate julianDate = AbsoluteDate.createJDDate(2453006, 0, utc);
        assertThat(120.6, closeTo(JBData.getF10B(julianDate), 1e-10));
    }

    @Test
    public void testS10() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        final AbsoluteDate julianDate = AbsoluteDate.createJDDate(2453006, 0, utc);
        assertThat(111.0, closeTo(JBData.getS10(julianDate), 1e-10));
    }

    @Test
    public void testS10B() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        final AbsoluteDate julianDate = AbsoluteDate.createJDDate(2453006, 0, utc);
        assertThat(116.8, closeTo(JBData.getS10B(julianDate), 1e-10));
    }

    @Test
    public void testM10() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        final AbsoluteDate julianDate = AbsoluteDate.createJDDate(2453006, 0, utc);
        assertThat(117.9, closeTo(JBData.getXM10(julianDate), 1e-10));
    }

    @Test
    public void testM10B() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        final AbsoluteDate julianDate = AbsoluteDate.createJDDate(2453006, 0, utc);
        assertThat(121.2, closeTo(JBData.getXM10B(julianDate), 1e-10));
    }

    @Test
    public void testY10() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        final AbsoluteDate julianDate = AbsoluteDate.createJDDate(2453006, 0, utc);
        assertThat(139.9, closeTo(JBData.getY10(julianDate), 1e-10));
    }

    @Test
    public void testY10B() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        final AbsoluteDate julianDate = AbsoluteDate.createJDDate(2453006, 0, utc);
        assertThat(129.5, closeTo(JBData.getY10B(julianDate), 1e-10));
    }


    /**
     * Check integration error is small when integrating the same equations over the same
     * interval.
     */
    @Test
    public void testWithPropagator() {
        CelestialBody sun = CelestialBodyFactory.getSun();
        final Frame eci = FramesFactory.getGCRF();
        final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        AbsoluteDate date = new AbsoluteDate(2004, 1, 2, utc);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, ecef);
        Orbit orbit = new KeplerianOrbit(6378137 + 400e3, 1e-3, FastMath.toRadians(50), 0, 0, 0, PositionAngleType.TRUE,
                eci, date, Constants.EIGEN5C_EARTH_MU);
        final SpacecraftState ic = new SpacecraftState(orbit);

        final AbsoluteDate end = date.shiftedBy(5 * Constants.JULIAN_DAY);
        final AbsoluteDate resetDate = date.shiftedBy(0.8 * Constants.JULIAN_DAY + 0.1);

        final SpacecraftState[] lastState = new SpacecraftState[1];
        final OrekitStepHandler stepSaver = interpolator -> {
            final AbsoluteDate start = interpolator.getPreviousState().getDate();
            if (start.compareTo(resetDate) < 0) {
                lastState[0] = interpolator.getPreviousState();
            }
        };

        // propagate with state rest to take slightly different path
        NumericalPropagator propagator = getNumericalPropagatorWithJB2008(sun, earth, ic);
        propagator.setStepHandler(stepSaver);
        propagator.propagate(resetDate);
        propagator.resetInitialState(lastState[0]);
        propagator.clearStepHandlers();
        SpacecraftState actual = propagator.propagate(end);

        // propagate straight through
        propagator = getNumericalPropagatorWithJB2008(sun, earth, ic);
        propagator.resetInitialState(ic);
        propagator.clearStepHandlers();
        SpacecraftState expected = propagator.propagate(end);

        assertThat(actual.getPVCoordinates(), pvCloseTo(expected.getPVCoordinates(), 1.0));

    }

    /**
     * Configure a numerical propagator with DTM2000 atmosphere.
     *
     * @param sun   Sun.
     * @param earth Earth.
     * @param ic    initial condition.
     * @return a propagator with DTM2000 atmosphere.
     */
    private NumericalPropagator getNumericalPropagatorWithJB2008(CelestialBody sun, OneAxisEllipsoid earth,
            SpacecraftState ic) {
        // some non-integer step size to induce truncation error in flux interpolation
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(120 + 0.1);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        JB2008InputParameters JBData = loadJB();
        final Atmosphere atmosphere = new JB2008(JBData, sun, earth);
        final IsotropicDrag satellite = new IsotropicDrag(1, 3.2);
        propagator.addForceModel(new DragForce(atmosphere, satellite));

        propagator.setInitialState(ic);
        propagator.setOrbitType(OrbitType.CARTESIAN);

        return propagator;
    }

    @Test
    /**
     * Testing for non-present day in the data
     * Testing getF10
     */
    public void testF10EphemerisException() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 1, 5, 17, 0.0, utc);
        try {
            JBData.getF10(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Testing for non-present day in the data
     * Testing getF10B
     */
    public void testF10BEphemerisException() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 1, 5, 17, 0.0, utc);
        try {
            JBData.getF10B(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Testing for non-present day in the data
     * Testing getS10
     */
    public void testS10EphemerisException() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 1, 5, 17, 0.0, utc);
        try {
            JBData.getS10(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Testing for non-present day in the data
     * Testing getS10B
     */
    public void testS10BEphemerisException() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 1, 5, 17, 0.0, utc);
        try {
            JBData.getS10(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Testing for non-present day in the data
     * Testing getXM10
     */
    public void testXM10EphemerisException() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 1, 5, 17, 0.0, utc);
        try {
            JBData.getXM10(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Testing for non-present day in the data
     * Testing getXM10B
     */
    public void testXM10BEphemerisException() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 1, 5, 17, 0.0, utc);
        try {
            JBData.getXM10B(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Testing for non-present day in the data
     * Testing getY10
     */
    public void testY10EphemerisException() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 1, 5, 17, 0.0, utc);
        try {
            JBData.getY10(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Testing for non-present day in the data
     * Testing getY10B
     */
    public void testY10BEphemerisException() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 1, 5, 17, 0.0, utc);
        try {
            JBData.getY10B(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, oe.getSpecifier());
        }
    }

    @Test
    public void testBracketDateSOL_lastDate() {
        JB2008SpaceEnvironmentData JBData = loadJB();
        AbsoluteDate date = new AbsoluteDate(2050, 10, 1, 5, 17, 0.0, utc);
        try {
            JBData.getY10B(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_AFTER, oe.getSpecifier());
        }
    }

}
