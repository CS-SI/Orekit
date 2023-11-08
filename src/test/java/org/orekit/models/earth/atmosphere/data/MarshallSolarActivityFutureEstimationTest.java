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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.DTM2000;
import org.orekit.models.earth.atmosphere.DTM2000InputParameters;
import org.orekit.models.earth.atmosphere.NRLMSISE00;
import org.orekit.models.earth.atmosphere.NRLMSISE00InputParameters;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation.StrengthLevel;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.Month;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStampedDouble;
import org.orekit.utils.Constants;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.IERSConventions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.orekit.OrekitMatchers.closeTo;
import static org.orekit.OrekitMatchers.pvCloseTo;

public class MarshallSolarActivityFutureEstimationTest {

    /**
     * Check {@link MarshallSolarActivityFutureEstimation#get24HoursKp(AbsoluteDate)} and
     * {@link MarshallSolarActivityFutureEstimation#getThreeHourlyKP(AbsoluteDate)} are
     * continuous.
     */
    @Test
    public void testGetKp() {
        //setup
        DTM2000InputParameters flux = getFlux();
        final AbsoluteDate july = new AbsoluteDate(2008, 7, 1, utc);
        final AbsoluteDate august = new AbsoluteDate(2008, 8, 1, utc);
        final AbsoluteDate middle = july.shiftedBy(august.durationFrom(july) / 2.0);
        final double minute = 60;
        final AbsoluteDate before = middle.shiftedBy(-minute);
        final AbsoluteDate after = middle.shiftedBy(+minute);

        // action + verify
        // non-chaotic i.e. small change in input produces small change in output.
        double kpHourlyDifference =
                flux.getThreeHourlyKP(before) - flux.getThreeHourlyKP(after);
        assertThat(kpHourlyDifference, closeTo(0.0, 1e-4));
        double kpDailyDifference = flux.get24HoursKp(before) - flux.get24HoursKp(after);
        assertThat(kpDailyDifference, closeTo(0.0, 1e-4));
        assertThat(flux.getThreeHourlyKP(middle), closeTo(2.18, 0.3));
        assertThat(flux.get24HoursKp(middle), closeTo(2.18, 0.3));
    }

    /**
     * Check {@link MarshallSolarActivityFutureEstimation#getAp(AbsoluteDate)} is
     * continuous.
     */
    @Test
    public void testGetAp() {
        //setup
        NRLMSISE00InputParameters flux = getFlux();
        final AbsoluteDate july = new AbsoluteDate(2008, 7, 1, utc);
        final AbsoluteDate august = new AbsoluteDate(2008, 8, 1, utc);
        final AbsoluteDate middle = july.shiftedBy(august.durationFrom(july) / 2.0);
        final double minute = 60;
        final AbsoluteDate before = middle.shiftedBy(-minute);
        final AbsoluteDate after = middle.shiftedBy(+minute);

        // action + verify
        // non-chaotic i.e. small change in input produces small change in output.
        final double[] apBefore = flux.getAp(before);
        final double[] apAfter  = flux.getAp(after);
        for (int i = 0; i < apBefore.length; i++) {
            assertThat(apBefore[i] - apAfter[i], closeTo(0.0, 1e-4));
        }
        for (double ap: flux.getAp(middle)) {
            assertThat(ap, closeTo(8.0, 1e-10));
        }
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
        OneAxisEllipsoid earth = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                ecef);
        Orbit orbit = new KeplerianOrbit(
                6378137 + 400e3, 1e-3, FastMath.toRadians(50), 0, 0, 0,
                PositionAngleType.TRUE, eci, date, Constants.EIGEN5C_EARTH_MU);
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
    public void testIssue1118() throws URISyntaxException {
        // Setup
        final String fileName = "Jan2000F10-edited-data.txt";
        final URL url = MarshallSolarActivityFutureEstimationTest.class.getClassLoader().getResource("atmosphere/" + fileName);
        final DataSource source = new DataSource(url.toURI());
        final MarshallSolarActivityFutureEstimation msafe =
                new MarshallSolarActivityFutureEstimation(source, StrengthLevel.AVERAGE);

        // Prepare data
        final AbsoluteDate july = new AbsoluteDate(2008, 7, 1, utc);
        final AbsoluteDate august = new AbsoluteDate(2008, 8, 1, utc);
        final AbsoluteDate middle = july.shiftedBy(august.durationFrom(july) / 2.0);
        final double minute = 60;
        final AbsoluteDate before = middle.shiftedBy(-minute);
        final AbsoluteDate after = middle.shiftedBy(+minute);

        // action + verify
        // non-chaotic i.e. small change in input produces small change in output.
        double kpHourlyDifference =
                msafe.getThreeHourlyKP(before) - msafe.getThreeHourlyKP(after);
        assertThat(kpHourlyDifference, closeTo(0.0, 1e-4));
        double kpDailyDifference = msafe.get24HoursKp(before) - msafe.get24HoursKp(after);
        assertThat(kpDailyDifference, closeTo(0.0, 1e-4));
        assertThat(msafe.getThreeHourlyKP(middle), closeTo(2.18, 0.3));
        assertThat(msafe.get24HoursKp(middle), closeTo(2.18, 0.3));
    }

    /**
     * Configure a numerical propagator with DTM2000 atmosphere.
     *
     * @param sun   Sun.
     * @param earth Earth.
     * @param ic    initial condition.
     * @return a propagator with DTM2000 atmosphere.
     */
    private NumericalPropagator getNumericalPropagatorWithDTM(CelestialBody sun,
                                                              OneAxisEllipsoid earth,
                                                              SpacecraftState ic)
    {
        // some non-integer step size to induce truncation error in flux interpolation
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(120 + 0.1);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        DTM2000InputParameters flux = getFlux();
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
    private NumericalPropagator getNumericalPropagatorWithMSIS(CelestialBody sun,
                                                               OneAxisEllipsoid earth,
                                                               SpacecraftState ic)
    {
        // some non-integer step size to induce truncation error in flux interpolation
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(120 + 0.1);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        NRLMSISE00InputParameters flux = getFlux();
        final Atmosphere atmosphere = new NRLMSISE00(flux, sun, earth);
        final IsotropicDrag satellite = new IsotropicDrag(1, 3.2);
        propagator.addForceModel(new DragForce(atmosphere, satellite));

        propagator.setInitialState(ic);
        propagator.setOrbitType(OrbitType.CARTESIAN);

        return propagator;
    }

    /**
     * Load an edited flux file.
     *
     * @return loaded flux file.
     */
    private MarshallSolarActivityFutureEstimation getFlux() {
        final String fileName = "Jan2000F10-edited-data.txt";
        try {
            final URL url =
                    MarshallSolarActivityFutureEstimationTest.class.getClassLoader().getResource("atmosphere/" + fileName);

            final DataSource source = new DataSource(url.toURI());
            return new MarshallSolarActivityFutureEstimation(source, StrengthLevel.AVERAGE);
        }
        catch (URISyntaxException e) {
            return null;
        }
    }

    @Test
    public void testFileDate() {

        MarshallSolarActivityFutureEstimation msafe =
            loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assertions.assertEquals(new DateComponents(2010, Month.NOVEMBER, 1),
                            msafe.getFileDate(new AbsoluteDate("2010-05-01", utc)));
        Assertions.assertEquals(new DateComponents(2010, Month.DECEMBER, 1),
                            msafe.getFileDate(new AbsoluteDate("2010-06-01", utc)));
        Assertions.assertEquals(new DateComponents(2011, Month.JANUARY, 1),
                            msafe.getFileDate(new AbsoluteDate("2010-07-01", utc)));
        Assertions.assertEquals(new DateComponents(2011, Month.JANUARY, 1),
                            msafe.getFileDate(new AbsoluteDate("2030-01-01", utc)));

    }

    @Test
    public void testFluxStrong() {

        MarshallSolarActivityFutureEstimation msafe =
            loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        Assertions.assertEquals(94.2,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-02", utc)),
                            1.0e-10);
        Assertions.assertEquals(96.6,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-17T12:00:00", utc)),
                            1.0e-10);
        Assertions.assertEquals(99.0,
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assertions.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG,
                            msafe.getStrengthLevel());
    }


    @Test
    public void testFluxAverage() {

        MarshallSolarActivityFutureEstimation msafe =
            loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assertions.assertEquals(87.6,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-02", utc)),
                            1.0e-10);
        Assertions.assertEquals(88.7,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-17T12:00:00", utc)),
                            1.0e-10);
        Assertions.assertEquals(89.8,
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assertions.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE,
                            msafe.getStrengthLevel());
    }


    @Test
    public void testFluxWeak() {

        MarshallSolarActivityFutureEstimation msafe =
                loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assertions.assertEquals(80.4,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-02", utc)),
                            1.0e-10);
        Assertions.assertEquals(80.6,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-17T12:00:00", utc)),
                            1.0e-10);
        Assertions.assertEquals(80.8,
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assertions.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assertions.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK,
                            msafe.getStrengthLevel());

    }

    private MarshallSolarActivityFutureEstimation loadDefaultMSAFE(
            MarshallSolarActivityFutureEstimation.StrengthLevel strength) {
        return new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                                                         strength);
    }

    @Test
    public void testKpStrong() {

        MarshallSolarActivityFutureEstimation msafe =
            loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        Assertions.assertEquals(2 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", utc)),
                            0.2);
        Assertions.assertEquals(3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", utc)),
                            0.1);

        // this one should get exactly to an element of the AP_ARRAY: ap = 7.0
        Assertions.assertEquals(2.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            0.3);
        Assertions.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", utc)),
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);

    }

    @Test
    public void testKpAverage() {

        MarshallSolarActivityFutureEstimation msafe =
            loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assertions.assertEquals(2 - 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", utc)),
                            0.1);
        Assertions.assertEquals(2 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", utc)),
                            0.1);
        Assertions.assertEquals(2.0 - 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            0.1);
        Assertions.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", utc)),
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);

    }

    @Test
    public void testKpWeak() {

        MarshallSolarActivityFutureEstimation msafe =
            loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assertions.assertEquals(1 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", utc)),
                            0.1);
        Assertions.assertEquals(2.0,
                            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", utc)),
                            0.3);
        Assertions.assertEquals(1 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            0.1);
        Assertions.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", utc)),
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);

    }

    @Test
    public void testApStrong() {

        MarshallSolarActivityFutureEstimation msafe =
            loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        for (double ap: msafe.getAp(new AbsoluteDate("2010-10-01", utc))) {
            Assertions.assertEquals(9.1, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2011-05-01", utc))) {
            Assertions.assertEquals(14.4, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2010-08-01", utc))) {
            Assertions.assertEquals(7.0, ap, 1e-10);
        }
    }

    @Test
    public void testApAverage() {

        MarshallSolarActivityFutureEstimation msafe =
            loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        for (double ap: msafe.getAp(new AbsoluteDate("2010-10-01", utc))) {
            Assertions.assertEquals(6.4, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2011-05-01", utc))) {
            Assertions.assertEquals(9.6, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2010-08-01", utc))) {
            Assertions.assertEquals(6.1, ap, 1e-10);
        }
    }

    @Test
    public void testApWeak() {

        MarshallSolarActivityFutureEstimation msafe =
            loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        for (double ap: msafe.getAp(new AbsoluteDate("2010-10-01", utc))) {
            Assertions.assertEquals(4.9, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2011-05-01", utc))) {
            Assertions.assertEquals(6.9, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2010-08-01", utc))) {
            Assertions.assertEquals(4.9, ap, 1e-10);
        }
    }

    @Test
    public void testMinDate() {

        MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assertions.assertEquals(new AbsoluteDate("2010-05-01", utc), msafe.getMinDate());
        Assertions.assertEquals(78.1,
                            msafe.getMeanFlux(msafe.getMinDate()),
                            1.0e-14);
    }

    @Test
    public void testMaxDate() {

        MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assertions.assertEquals(new AbsoluteDate("2030-10-01", utc), msafe.getMaxDate());
        Assertions.assertEquals(67.0, msafe.getMeanFlux(msafe.getMaxDate()), 1.0e-14);
    }

    @Test
    public void testPastOutOfRange() {
        Assertions.assertThrows(OrekitException.class, () -> {
            MarshallSolarActivityFutureEstimation msafe =
                    loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
            msafe.get24HoursKp(new AbsoluteDate("1960-10-01", utc));
        });
    }

    @Test
    public void testFutureOutOfRange() {
        Assertions.assertThrows(OrekitException.class, () -> {
            MarshallSolarActivityFutureEstimation msafe =
                    loadDefaultMSAFE(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
            msafe.get24HoursKp(new AbsoluteDate("2060-10-01", utc));
        });
    }

    @Test
    public void testExtraData() {
        Assertions.assertThrows(OrekitException.class, () -> {
                    new MarshallSolarActivityFutureEstimation("Jan2011F10-extra-data\\.txt",
                            MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        });
    }

    @Test
    public void testNoData() {
        Assertions.assertThrows(OrekitException.class, () -> {
            new MarshallSolarActivityFutureEstimation("Jan2011F10-no-data\\.txt",
                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        });
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        MarshallSolarActivityFutureEstimation original =
                        new MarshallSolarActivityFutureEstimation("Jan2000F10-edited-data.txt$",
                                                                  StrengthLevel.AVERAGE);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(original);

        Assertions.assertTrue(bos.size() > 400);
        Assertions.assertTrue(bos.size() < 450);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        AbsoluteDate date = new AbsoluteDate(2004, 1, 1, utc);
        MarshallSolarActivityFutureEstimation deserialized = (MarshallSolarActivityFutureEstimation) ois.readObject();
        Assertions.assertEquals(original.getMeanFlux(date),    deserialized.getMeanFlux(date),    1.0e-12);
        Assertions.assertEquals(original.getDailyFlux(date),   deserialized.getDailyFlux(date),   1.0e-12);
        Assertions.assertEquals(original.getInstantFlux(date), deserialized.getInstantFlux(date), 1.0e-12);
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
        final MarshallSolarActivityFutureEstimation weatherData = loadDefaultMSAFE(StrengthLevel.AVERAGE);

        // Create date sample at which flux will be evaluated
        final AbsoluteDate       initialDate = new AbsoluteDate("2010-05-01", utc);
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

        // WHEN
        try {
            service.invokeAll(tasks);
            results.get().sort(Comparator.comparing(TimeStampedDouble::getDate));
            final List<Double> sortedComputedResults = results.get().stream().map(TimeStampedDouble::getValue).collect(
                    Collectors.toList());

            // THEN
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
    void testExpectedCacheConfigurationAndCalls() {
        // GIVEN
        final AbsoluteDate date = new AbsoluteDate(2020, 2, 25, 2, 0, 0, TimeScalesFactory.getUTC());

        final MarshallSolarActivityFutureEstimation atm =
              new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                                                        StrengthLevel.AVERAGE);

        // WHEN
        // Call flux at instants that shall generate slots
        atm.getInstantFlux(date.shiftedBy(-1 * Constants.JULIAN_DAY * 31));
        atm.getInstantFlux(date);
        atm.getInstantFlux(date.shiftedBy(1 * Constants.JULIAN_DAY * 31));
        atm.getInstantFlux(date.shiftedBy(3 * Constants.JULIAN_DAY * 31));
        atm.getInstantFlux(date.shiftedBy(2 * Constants.JULIAN_DAY * 31));

        // Call flux at instants that shall not generate slots
        atm.getInstantFlux(date.shiftedBy(-0.6 * Constants.JULIAN_DAY * 31));
        atm.getInstantFlux(date.shiftedBy(1.8 * Constants.JULIAN_DAY * 31));

        // THEN
        final GenericTimeStampedCache<MarshallSolarActivityFutureEstimationLoader.LineParameters> cache = atm.getCache();
        Assertions.assertEquals(5, cache.getGenerateCalls());
        Assertions.assertEquals(5, cache.getSlots());
        Assertions.assertEquals(10, cache.getEntries());
        Assertions.assertEquals(7, cache.getGetNeighborsCalls());
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere");
        utc = TimeScalesFactory.getUTC();
    }

    @AfterEach
    public void tearDown() {
        utc = null;
    }

    private TimeScale utc;

}
