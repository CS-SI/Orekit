/* 
 * Copyright 2020 Clément Jonglez
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Clément Jonglez licenses this file to You under the Apache License, Version 2.0
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.orekit.OrekitMatchers.closeTo;
import static org.orekit.OrekitMatchers.pvCloseTo;

import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
import org.orekit.models.earth.atmosphere.DTM2000;
import org.orekit.models.earth.atmosphere.DTM2000InputParameters;
import org.orekit.models.earth.atmosphere.NRLMSISE00;
import org.orekit.models.earth.atmosphere.NRLMSISE00InputParameters;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 *
 * @author Clément Jonglez
 */
public class CssiSpaceWeatherLoaderTest {
    private TimeScale utc;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere");
        utc = TimeScalesFactory.getUTC();
    }

    private CssiSpaceWeatherData loadCswl() {
        CssiSpaceWeatherData cswl = new CssiSpaceWeatherData("SpaceWeather-All-v1.2_snapshot_20200224.txt");
        return cswl;
    }

    @Test
    public void testMinDate() {
        CssiSpaceWeatherData cswl = loadCswl();
        Assert.assertEquals(new AbsoluteDate("1957-10-01", utc), cswl.getMinDate());
    }

    @Test
    public void testMaxDate() {
        CssiSpaceWeatherData cswl = loadCswl();
        Assert.assertEquals(new AbsoluteDate("2044-06-01", utc), cswl.getMaxDate());
    }

    @Test
    public void testThreeHourlyKpObserved() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 10, 0, 0, 0.0, utc);
        final double kp = cswl.getThreeHourlyKP(date);
        assertThat(kp, closeTo(3.0, 1e-10));
    }

    @Test
    /**
     * Requests a date between two months, requiring interpolation
     */
    public void testThreeHourlyKpMonthlyPredicted() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2038, 6, 16, 0, 0, 0.0, utc);
        final double kp = cswl.getThreeHourlyKP(date);
        assertThat(kp, closeTo((2.7 + 4.1) / 2, 1e-3));
    }

    @Test
    /**
     * Testing first day of data
     * Because the Ap up to 57 hours prior to the date are asked, 
     * this should return an exception
     */
    public void testThreeHourlyApObservedFirstDay() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 1, 5, 17, 0.0, utc);
        try {
            cswl.getAp(date);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Testing second day of data
     * Because the Ap up to 57 hours prior to the date are asked, 
     * this should return an exception
     */
    public void testThreeHourlyApObservedSecondDay() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 2, 3, 14, 0.0, utc);
        try {
            cswl.getAp(date);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Testing third day of data
     * Because the Ap up to 57 hours prior to the date are asked, 
     * this should return an exception
     */
    public void testThreeHourlyApObservedThirdDay() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 3, 3, 14, 0.0, utc);
        try {
            cswl.getAp(date);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Here, no more side effects are expected
     */
    public void testThreeHourlyApObserved() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 10, 3, 14, 0.0, utc);
        final double[] apExpected = new double[] { 18, 27, 15, 9, 7, 5.625, 3.625 };
        final double[] ap = cswl.getAp(date);
        for (int i = 0; i < 7; i++) {
            assertThat(ap[i], closeTo(apExpected[i], 1e-10));
        }
    }

    @Test
    /**
     * This test is very approximate, at least to check that the two proper months were used for the interpolation 
     * But the manual interpolation of all 7 coefficients would have been a pain
     */
    public void testThreeHourlyApMonthlyPredicted() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2038, 6, 16, 0, 0, 0.0, utc);
        final double[] ap = cswl.getAp(date);
        for (int i = 0; i < 7; i++) {
            assertThat(ap[i], closeTo((12 + 29) / 2, 1.0));
        }
    }

    @Test
    public void testDailyFlux() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2000, 1, 1, 0, 0, 0.0, utc);
        final double dailyFlux = cswl.getDailyFlux(date);
        // The daily flux is supposed to be the one from 1999-12-31
        assertThat(dailyFlux, closeTo(125.8, 1e-10));
    }

    @Test
    public void testDailyFluxMonthlyPredicted() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2034, 6, 17, 0, 0, 0.0, utc);
        final double dailyFlux = cswl.getDailyFlux(date);
        assertThat(dailyFlux, closeTo((136.4 + 141.9) / 2, 1e-3));
    }

    @Test
    public void testAverageFlux() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2000, 1, 1, 0, 0, 0.0, utc);
        final double averageFlux = cswl.getAverageFlux(date);
        assertThat(averageFlux, closeTo(158.6, 1e-10));
    }

    @Test
    public void testAverageFluxMonthlyPredicted() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2034, 6, 16, 0, 0, 0.0, utc);
        final double averageFlux = cswl.getAverageFlux(date);
        assertThat(averageFlux, closeTo((134.8 + 138.8) / 2, 1e-3));
    }

    @Test
    public void testInstantFlux() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, utc);
        final double instantFlux = cswl.getInstantFlux(date);
        assertThat(instantFlux, closeTo((125.6 + 128.5) / 2, 1e-10));
    }

    /**
     * This is to test that daily Kp values with three figures are correctly parsed
     */
    @Test
    public void testDailyKp2003SolarStorm() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2003, 10, 29, 23, 0, 0.0, utc);
        final double kp = cswl.get24HoursKp(date);
        assertThat(kp, closeTo(583.0 / 10.0 / 8, 1e-10));
    }

    /**
     * This is to test that Ap values with three figures are correctly parsed
     */
    @Test
    public void testAp2003SolarStorm() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2003, 10, 29, 23, 0, 0.0, utc);
        final double[] ap = cswl.getAp(date);
        assertThat(ap[0], closeTo(204, 1e-10));
        assertThat(ap[1], closeTo(300, 1e-10));
        assertThat(ap[2], closeTo(300, 1e-10));
        assertThat(ap[3], closeTo(179, 1e-10));
        assertThat(ap[4], closeTo(179, 1e-10));
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
        AbsoluteDate date = new AbsoluteDate(2004, 1, 1, utc);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, ecef);
        Orbit orbit = new KeplerianOrbit(6378137 + 400e3, 1e-3, FastMath.toRadians(50), 0, 0, 0, PositionAngle.TRUE,
                eci, date, Constants.EIGEN5C_EARTH_MU);
        final SpacecraftState ic = new SpacecraftState(orbit);

        final AbsoluteDate end = date.shiftedBy(5 * Constants.JULIAN_DAY);
        final AbsoluteDate resetDate = date.shiftedBy(0.8 * Constants.JULIAN_DAY + 0.1);

        final SpacecraftState[] lastState = new SpacecraftState[1];
        final OrekitStepHandler stepSaver = (interpolator, isLast) -> {
            final AbsoluteDate start = interpolator.getPreviousState().getDate();
            if (start.compareTo(resetDate) < 0) {
                lastState[0] = interpolator.getPreviousState();
            }
        };

        // propagate with state rest to take slightly different path
        NumericalPropagator propagator = getNumericalPropagatorWithDTM(sun, earth, ic);
        propagator.setMasterMode(stepSaver);
        propagator.propagate(resetDate);
        propagator.resetInitialState(lastState[0]);
        propagator.setSlaveMode();
        SpacecraftState actual = propagator.propagate(end);

        // propagate straight through
        propagator = getNumericalPropagatorWithDTM(sun, earth, ic);
        propagator.resetInitialState(ic);
        propagator.setSlaveMode();
        SpacecraftState expected = propagator.propagate(end);

        assertThat(actual.getPVCoordinates(), pvCloseTo(expected.getPVCoordinates(), 1.0));

        // propagate with state rest to take slightly different path
        propagator = getNumericalPropagatorWithMSIS(sun, earth, ic);
        propagator.setMasterMode(stepSaver);
        propagator.propagate(resetDate);
        propagator.resetInitialState(lastState[0]);
        propagator.setSlaveMode();
        actual = propagator.propagate(end);

        // propagate straight through
        propagator = getNumericalPropagatorWithMSIS(sun, earth, ic);
        propagator.resetInitialState(ic);
        propagator.setSlaveMode();
        expected = propagator.propagate(end);

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
    private NumericalPropagator getNumericalPropagatorWithDTM(CelestialBody sun, OneAxisEllipsoid earth,
            SpacecraftState ic) {
        // some non-integer step size to induce truncation error in flux interpolation
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(120 + 0.1);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        DTM2000InputParameters flux = loadCswl();
        final Atmosphere atmosphere = new DTM2000(flux, sun, earth);
        final IsotropicDrag satellite = new IsotropicDrag(1, 3.2);
        propagator.addForceModel(new DragForce(atmosphere, satellite));

        propagator.setInitialState(ic);
        propagator.setOrbitType(OrbitType.CARTESIAN);

        return propagator;
    }

    /**
     * Configure a numerical propagator with NRLMSISE00 atmosphere.
     *
     * @param sun   Sun.
     * @param earth Earth.
     * @param ic    initial condition.
     * @return a propagator with NRLMSISE00 atmosphere.
     */
    private NumericalPropagator getNumericalPropagatorWithMSIS(CelestialBody sun, OneAxisEllipsoid earth,
            SpacecraftState ic) {
        // some non-integer step size to induce truncation error in flux interpolation
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(120 + 0.1);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        NRLMSISE00InputParameters flux = loadCswl();
        final Atmosphere atmosphere = new NRLMSISE00(flux, sun, earth);
        final IsotropicDrag satellite = new IsotropicDrag(1, 3.2);
        propagator.addForceModel(new DragForce(atmosphere, satellite));

        propagator.setInitialState(ic);
        propagator.setOrbitType(OrbitType.CARTESIAN);

        return propagator;
    }
}
