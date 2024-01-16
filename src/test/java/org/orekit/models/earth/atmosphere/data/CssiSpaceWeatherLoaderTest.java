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
import static org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD;
import static org.orekit.OrekitMatchers.closeTo;
import static org.orekit.OrekitMatchers.pvCloseTo;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
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
import org.orekit.models.earth.atmosphere.data.CssiSpaceWeatherDataLoader.LineParameters;
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
import org.orekit.time.TimeStampedDouble;
import org.orekit.utils.Constants;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.IERSConventions;

/**
 *
 * @author Clément Jonglez
 */
public class CssiSpaceWeatherLoaderTest {
    private TimeScale utc;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere");
        utc = TimeScalesFactory.getUTC();
    }

    private CssiSpaceWeatherData loadCswl() {
        return new CssiSpaceWeatherData(CssiSpaceWeatherData.DEFAULT_SUPPORTED_NAMES);
    }

    @Test
    public void testIssue1117() throws URISyntaxException {
        final URL url = CssiSpaceWeatherLoaderTest.class.getClassLoader().getResource("atmosphere/SpaceWeather-All-v1.2_reduced.txt");
        CssiSpaceWeatherData cswl = new CssiSpaceWeatherData(new DataSource(url.toURI()));
        Assertions.assertEquals(new AbsoluteDate("2020-02-19", utc), cswl.getMinDate());
        Assertions.assertEquals(new AbsoluteDate("2020-02-22", utc), cswl.getMaxDate());
    }

    @Test
    public void testMinDate() {
        CssiSpaceWeatherData cswl = loadCswl();
        Assertions.assertEquals(new AbsoluteDate("1957-10-01", utc), cswl.getMinDate());
    }

    @Test
    public void testMaxDate() {
        CssiSpaceWeatherData cswl = loadCswl();
        Assertions.assertEquals(new AbsoluteDate("2044-06-01", utc), cswl.getMaxDate());
    }

    @Test
    public void testThreeHourlyKpObserved() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 10, 0, 0, 0.0, utc);
        final double kp = cswl.getThreeHourlyKP(date);
        assertThat(kp, closeTo(3.0, 1e-10));
    }

    /** Requests a date between two months, requiring interpolation */
    @Test
    public void testThreeHourlyKpMonthlyPredicted() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2038, 6, 16, 0, 0, 0.0, utc);
        final double kp = cswl.getThreeHourlyKP(date);
        assertThat(kp, closeTo((2.7 + 4.1) / 2, 1e-3));
    }

    /**
     * Testing first day of data
     * Because the Ap up to 57 hours prior to the date are asked,
     * this should return an exception
     */
    @Test
    public void testThreeHourlyApObservedFirstDay() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 1, 5, 17, 0.0, utc);
        try {
            cswl.getAp(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, oe.getSpecifier());
        }
    }

    /**
     * Testing second day of data
     * Because the Ap up to 57 hours prior to the date are asked,
     * this should return an exception
     */
    @Test
    public void testThreeHourlyApObservedSecondDay() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 2, 3, 14, 0.0, utc);
        try {
            cswl.getAp(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, oe.getSpecifier());
        }
    }

    /**
     * Testing third day of data
     * Because the Ap up to 57 hours prior to the date are asked,
     * this should return an exception
     */
    @Test
    public void testThreeHourlyApObservedThirdDay() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 3, 3, 14, 0.0, utc);
        try {
            cswl.getAp(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, oe.getSpecifier());
        }
    }

    /** Here, no more side effects are expected */
    @Test
    public void testThreeHourlyApObserved() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(1957, 10, 10, 3, 14, 0.0, utc);
        final double[] apExpected = new double[] { 18, 27, 15, 9, 7, 5.625, 3.625 };
        final double[] ap = cswl.getAp(date);
        for (int i = 0; i < 7; i++) {
            assertThat(ap[i], closeTo(apExpected[i], 1e-10));
        }
    }

    /**
     * This test is very approximate, at least to check that the two proper months were used for the interpolation
     * But the manual interpolation of all 7 coefficients would have been a pain
     */
    @Test
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
        assertThat(dailyFlux, closeTo(130.1, 1e-10));
    }

    @Test
    public void testDailyFluxMonthlyPredicted() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2034, 6, 17, 0, 0, 0.0, utc);
        final double dailyFlux = cswl.getDailyFlux(date);
        assertThat(dailyFlux, closeTo((132.7 + 137.3) / 2, 1e-3));
    }

    @Test
    public void testMeanFlux() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2000, 1, 1, 0, 0, 0.0, utc);
        final double meanFlux = cswl.getMeanFlux(date);
        assertThat(meanFlux, closeTo(165.6, 1e-10));
    }

    @Test
    public void testMeanFluxMonthlyPredicted() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2034, 6, 16, 0, 0, 0.0, utc);
        final double meanFlux = cswl.getMeanFlux(date);
        assertThat(meanFlux, closeTo((132.1 + 134.9) / 2, 1e-3));
    }

    @Test
    public void testAverageFlux() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2000, 1, 1, 0, 0, 0.0, utc);
        final double averageFlux = cswl.getAverageFlux(date);
        assertThat(averageFlux, closeTo(165.6, 1e-10));
    }

    @Test
    public void testAverageFluxMonthlyPredicted() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2034, 6, 16, 0, 0, 0.0, utc);
        final double averageFlux = cswl.getAverageFlux(date);
        assertThat(averageFlux, closeTo((132.1 + 134.9) / 2, 1e-3));
    }

    @Test
    public void testInstantFlux() {
        CssiSpaceWeatherData cswl = loadCswl();
        AbsoluteDate date = new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, utc);
        final double instantFlux = cswl.getInstantFlux(date);
        assertThat(instantFlux, closeTo((129.9 + 132.9) / 2, 1e-10));
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
        NumericalPropagator propagator = getNumericalPropagatorWithDTM(sun, earth, ic);
        propagator.setStepHandler(stepSaver);
        propagator.propagate(resetDate);
        propagator.resetInitialState(lastState[0]);
        propagator.clearStepHandlers();
        SpacecraftState actual = propagator.propagate(end);

        // propagate straight through
        propagator = getNumericalPropagatorWithDTM(sun, earth, ic);
        propagator.resetInitialState(ic);
        propagator.clearStepHandlers();
        SpacecraftState expected = propagator.propagate(end);

        assertThat(actual.getPVCoordinates(), pvCloseTo(expected.getPVCoordinates(), 1.0));

        // propagate with state rest to take slightly different path
        propagator = getNumericalPropagatorWithMSIS(sun, earth, ic);
        propagator.setStepHandler(stepSaver);
        propagator.propagate(resetDate);
        propagator.resetInitialState(lastState[0]);
        propagator.clearStepHandlers();
        actual = propagator.propagate(end);

        // propagate straight through
        propagator = getNumericalPropagatorWithMSIS(sun, earth, ic);
        propagator.resetInitialState(ic);
        propagator.clearStepHandlers();
        expected = propagator.propagate(end);

        assertThat(actual.getPVCoordinates(), pvCloseTo(expected.getPVCoordinates(), 1.0));
    }

    @Test
    public void testIssue841() throws OrekitException {
        final CssiSpaceWeatherDataLoader loader = new CssiSpaceWeatherDataLoader(utc);
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.feed("SpaceWeather-All-v1.2_reduced.txt", loader);
        final SortedSet<LineParameters> set = loader.getDataSet();
        Assertions.assertEquals(4, set.size());

        CssiSpaceWeatherData cswl = new CssiSpaceWeatherData("SpaceWeather-All-v1.2_reduced.txt");
        Assertions.assertEquals(71.6, cswl.getInstantFlux(new AbsoluteDate("2020-02-20T00:00:00.000", utc)), 0.01);
    }

    /**
     * This test in a multi-threaded environment would not necessarily fail without the fix (even though it will very likely
     * fail).
     * <p>
     * However, it cannot fail with the fix.
     */
    @RepeatedTest(10)
    @DisplayName("Test in a multi-threaded environment")
    void testIssue1072() {
        // GIVEN
        final CssiSpaceWeatherData weatherData = loadCswl();

        // Create date sample at which flux will be evaluated
        final AbsoluteDate       initialDate = new AbsoluteDate();
        final List<AbsoluteDate> dates       = new ArrayList<>();
        final int                sampleSize  = 100;
        for (int i = 0; i < sampleSize + 1; i++) {
            dates.add(initialDate.shiftedBy(i * Constants.JULIAN_DAY * 30));
        }

        // Create list of tasks to run in parallel
        final AtomicReference<List<TimeStampedDouble>> results = new AtomicReference<>(new ArrayList<>());
        final List<Callable<List<TimeStampedDouble>>>  tasks   = new ArrayList<>();
        for (int i = 0; i < sampleSize + 1; i++) {
            final AbsoluteDate currentDate = dates.get(i);
            // Each task will evaluate value at specific date and store this value and associated date in a shared list
            tasks.add(() -> (results.getAndUpdate((listToUpdate) -> {
                final List<TimeStampedDouble> newList = new ArrayList<>(listToUpdate);
                newList.add(new TimeStampedDouble(weatherData.get24HoursKp(currentDate), currentDate));
                return newList;
            })));
        }

        // Create multithreading environment
        ExecutorService service = Executors.newFixedThreadPool(sampleSize);

        // WHEN & THEN
        try {
            service.invokeAll(tasks);
            results.get().sort(Comparator.comparing(TimeStampedDouble::getDate));
            final List<Double> sortedComputedResults = results.get().stream().map(TimeStampedDouble::getValue).collect(
                    Collectors.toList());

            // Compare to expected result
            for (int i = 0; i < sampleSize + 1; i++) {
                final AbsoluteDate currentDate = dates.get(i);
                Assertions.assertEquals(weatherData.get24HoursKp(currentDate), sortedComputedResults.get(i));
            }

            try {
                // wait for proper ending
                service.shutdown();
                service.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            }
        }
        catch (Exception e) {
            // Should not fail
            Assertions.fail();
        }
    }

    @Test
    @Timeout(value = 5, threadMode = SEPARATE_THREAD)
    public void testIssue1269() {
        // GIVEN
        final NRLMSISE00InputParameters solarActivity =
                new CssiSpaceWeatherData(CssiSpaceWeatherData.DEFAULT_SUPPORTED_NAMES);

        // WHEN & THEN
        solarActivity.getAverageFlux(new AbsoluteDate("2025-02-01T00:00:00.000", TimeScalesFactory.getUTC()));
        solarActivity.getDailyFlux(new AbsoluteDate("2025-02-01T00:00:00.000", TimeScalesFactory.getUTC()));
        solarActivity.getAp(new AbsoluteDate("2025-02-01T00:00:00.000", TimeScalesFactory.getUTC()));

    }

    @Test
    void testExpectedCacheConfigurationAndCalls() {
        // GIVEN
        final AbsoluteDate date = new AbsoluteDate(2020, 2, 25, 2, 0, 0, TimeScalesFactory.getUTC());

        final CssiSpaceWeatherData atm = new CssiSpaceWeatherData(CssiSpaceWeatherData.DEFAULT_SUPPORTED_NAMES);

        // WHEN
        // Call flux at instants that shall generate slots
        atm.getInstantFlux(date.shiftedBy(-1*Constants.JULIAN_DAY));
        atm.getInstantFlux(date);
        atm.getInstantFlux(date.shiftedBy(1*Constants.JULIAN_DAY));
        atm.getInstantFlux(date.shiftedBy(3*Constants.JULIAN_DAY));
        atm.getInstantFlux(date.shiftedBy(2*Constants.JULIAN_DAY));

        // Call flux at instants that shall not generate slots
        atm.getInstantFlux(date.shiftedBy(-0.6 * Constants.JULIAN_DAY));
        atm.getInstantFlux(date.shiftedBy(1.8 * Constants.JULIAN_DAY));

        // THEN
        final GenericTimeStampedCache<LineParameters> cache = atm.getCache();
        Assertions.assertEquals(5, cache.getGenerateCalls());
        Assertions.assertEquals(5, cache.getSlots());
        Assertions.assertEquals(10, cache.getEntries());
        Assertions.assertEquals(7, cache.getGetNeighborsCalls());
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
