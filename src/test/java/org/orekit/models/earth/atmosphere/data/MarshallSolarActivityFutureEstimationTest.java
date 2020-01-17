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
package org.orekit.models.earth.atmosphere.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.orekit.OrekitMatchers.closeTo;
import static org.orekit.OrekitMatchers.pvCloseTo;

import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
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
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.Month;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

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
                PositionAngle.TRUE, eci, date, Constants.EIGEN5C_EARTH_MU);
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
        MarshallSolarActivityFutureEstimation flux =
                new MarshallSolarActivityFutureEstimation(
                        "Jan2000F10-edited-data.txt$",
                        StrengthLevel.AVERAGE);
        DataContext.getDefault().getDataProvidersManager().feed(flux.getSupportedNames(), flux);
        return flux;
    }

    @Test
    public void testFileDate() {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assert.assertEquals(new DateComponents(2010, Month.NOVEMBER, 1),
                            msafe.getFileDate(new AbsoluteDate("2010-05-01", utc)));
        Assert.assertEquals(new DateComponents(2010, Month.DECEMBER, 1),
                            msafe.getFileDate(new AbsoluteDate("2010-06-01", utc)));
        Assert.assertEquals(new DateComponents(2011, Month.JANUARY, 1),
                            msafe.getFileDate(new AbsoluteDate("2010-07-01", utc)));
        Assert.assertEquals(new DateComponents(2011, Month.JANUARY, 1),
                            msafe.getFileDate(new AbsoluteDate("2030-01-01", utc)));

    }

    @Test
    public void testFluxStrong() {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        Assert.assertEquals(94.2,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-02", utc)),
                            1.0e-10);
        Assert.assertEquals(96.6,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-17T12:00:00", utc)),
                            1.0e-10);
        Assert.assertEquals(99.0,
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assert.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG,
                            msafe.getStrengthLevel());
    }


    @Test
    public void testFluxAverage() {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assert.assertEquals(87.6,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-02", utc)),
                            1.0e-10);
        Assert.assertEquals(88.7,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-17T12:00:00", utc)),
                            1.0e-10);
        Assert.assertEquals(89.8,
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assert.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE,
                            msafe.getStrengthLevel());
    }


    @Test
    public void testFluxWeak() {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(80.4,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-02", utc)),
                            1.0e-10);
        Assert.assertEquals(80.6,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-10-17T12:00:00", utc)),
                            1.0e-10);
        Assert.assertEquals(80.8,
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getDailyFlux(new AbsoluteDate("2010-11-02", utc)),
                            1.0e-10);
        Assert.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK,
                            msafe.getStrengthLevel());

    }

    private MarshallSolarActivityFutureEstimation loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel strength)
        {

        MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES, strength);
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.feed(msafe.getSupportedNames(), msafe);
        return msafe;
    }

    @Test
    public void testKpStrong() {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        Assert.assertEquals(2 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", utc)),
                            0.2);
        Assert.assertEquals(3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", utc)),
                            0.1);

        // this one should get exactly to an element of the AP_ARRAY: ap = 7.0
        Assert.assertEquals(2.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            0.3);
        Assert.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", utc)),
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);

    }

    @Test
    public void testKpAverage() {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assert.assertEquals(2 - 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", utc)),
                            0.1);
        Assert.assertEquals(2 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", utc)),
                            0.1);
        Assert.assertEquals(2.0 - 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            0.1);
        Assert.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", utc)),
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);

    }

    @Test
    public void testKpWeak() {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(1 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", utc)),
                            0.1);
        Assert.assertEquals(2.0,
                            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", utc)),
                            0.3);
        Assert.assertEquals(1 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            0.1);
        Assert.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", utc)),
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);

    }

    @Test
    public void testApStrong() {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        for (double ap: msafe.getAp(new AbsoluteDate("2010-10-01", utc))) {
            Assert.assertEquals(9.1, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2011-05-01", utc))) {
            Assert.assertEquals(14.4, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2010-08-01", utc))) {
            Assert.assertEquals(7.0, ap, 1e-10);
        }
    }

    @Test
    public void testApAverage() {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        for (double ap: msafe.getAp(new AbsoluteDate("2010-10-01", utc))) {
            Assert.assertEquals(6.4, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2011-05-01", utc))) {
            Assert.assertEquals(9.6, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2010-08-01", utc))) {
            Assert.assertEquals(6.1, ap, 1e-10);
        }
    }

    @Test
    public void testApWeak() {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        for (double ap: msafe.getAp(new AbsoluteDate("2010-10-01", utc))) {
            Assert.assertEquals(4.9, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2011-05-01", utc))) {
            Assert.assertEquals(6.9, ap, 1e-10);
        }
        for (double ap: msafe.getAp(new AbsoluteDate("2010-08-01", utc))) {
            Assert.assertEquals(4.9, ap, 1e-10);
        }
    }

    @Test
    public void testMinDate() {

        MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(new AbsoluteDate("2010-05-01", utc), msafe.getMinDate());
        Assert.assertEquals(78.1,
                            msafe.getMeanFlux(msafe.getMinDate()),
                            1.0e-14);
    }

    @Test
    public void testMaxDate() {

        MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation(MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(new AbsoluteDate("2030-10-01", utc), msafe.getMaxDate());
        Assert.assertEquals(67.0,
                            msafe.getMeanFlux(msafe.getMaxDate()),
                            1.0e-14);
    }

    @Test(expected=OrekitException.class)
    public void testPastOutOfRange() {
        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        msafe.get24HoursKp(new AbsoluteDate("1960-10-01", utc));
    }

    @Test(expected=OrekitException.class)
    public void testFutureOutOfRange() {
        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        msafe.get24HoursKp(new AbsoluteDate("2060-10-01", utc));
    }

    @Test(expected=OrekitException.class)
    public void testExtraData() {
        MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation("Jan2011F10-extra-data\\.txt",
                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.feed(msafe.getSupportedNames(), msafe);
    }

    @Test(expected=OrekitException.class)
    public void testNoData() {
        MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation("Jan2011F10-no-data\\.txt",
                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.feed(msafe.getSupportedNames(), msafe);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere");
        utc = TimeScalesFactory.getUTC();
    }

    @After
    public void tearDown() {
        utc = null;
    }

    private TimeScale utc;

}
