/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.propagation.events;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.models.earth.EarthStandardAtmosphereRefraction;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ElevationMask;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class ElevationDetectorTest {

    private double mu;
    private double ae;
    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;

    @Test
    public void testAgata() throws OrekitException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date, mu);

        Propagator propagator =
            new EcksteinHechlerPropagator(orbit, ae, mu, c20, c30, c40, c50, c60);

        // Earth and frame
        double ae =  6378137.0; // equatorial radius in meter
        double f  =  1.0 / 298.257223563; // flattening
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
        BodyShape earth = new OneAxisEllipsoid(ae, f, itrf);
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(48.833),
                                                FastMath.toRadians(2.333),
                                                0.0);
        TopocentricFrame topo = new TopocentricFrame(earth, point, "Gstation");
        Checking checking = new Checking(topo);
        ElevationDetector detector =
                new ElevationDetector(topo).
                withConstantElevation(FastMath.toRadians(5.0)).
                withHandler(checking);
        Assert.assertNull(detector.getElevationMask());
        Assert.assertNull(detector.getRefractionModel());
        Assert.assertSame(topo, detector.getTopocentricFrame());
        Assert.assertEquals(FastMath.toRadians(5.0), detector.getMinElevation(), 1.0e-15);

        AbsoluteDate startDate = new AbsoluteDate(2003, 9, 15, 12, 0, 0, utc);
        propagator.resetInitialState(propagator.propagate(startDate));
        propagator.addEventDetector(detector);
        propagator.setMasterMode(10.0, checking);
        propagator.propagate(startDate.shiftedBy(Constants.JULIAN_DAY));

    }

    private static class Checking implements EventHandler<ElevationDetector>, OrekitFixedStepHandler {

        private TopocentricFrame topo;
        private boolean visible;

        public Checking(final TopocentricFrame topo) {
            this.topo = topo;
            this.visible = false;
        }

        public Action eventOccurred(SpacecraftState s, ElevationDetector detector, boolean increasing) {
            visible = increasing;
            return Action.CONTINUE;
        }

        public void handleStep(SpacecraftState currentState, boolean isLast)
            throws OrekitException {
            BodyShape shape = topo.getParentShape();
            GeodeticPoint p =
                            shape.transform(currentState.getPVCoordinates().getPosition(),
                                            currentState.getFrame(), currentState.getDate());
            Vector3D subSat = shape.transform(new GeodeticPoint(p.getLatitude(), p.getLongitude(), 0.0));
            double range = topo.getRange(subSat, shape.getBodyFrame(), currentState.getDate());

            if (visible) {
                Assert.assertTrue(range < 2.45e6);
            } else {
                Assert.assertTrue(range > 2.02e6);
            }
        }

        @Override
        public void init(SpacecraftState initialState, AbsoluteDate target, double step) {
        }

        @Override
        public void init(SpacecraftState initialState, AbsoluteDate target) {
        }

    }

    @Test
    public void testEventForMask() throws OrekitException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date, mu);

        Propagator propagator =
            new EcksteinHechlerPropagator(orbit, ae, mu, c20, c30, c40, c50, c60);

        // Earth and frame
        double ae =  6378137.0; // equatorial radius in meter
        double f  =  1.0 / 298.257223563; // flattening
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
        BodyShape earth = new OneAxisEllipsoid(ae, f, itrf);
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(48.833),
                                                FastMath.toRadians(2.333),
                                                0.0);
        TopocentricFrame topo = new TopocentricFrame(earth, point, "Gstation");
        double [][] maskValues = {{FastMath.toRadians(0),FastMath.toRadians(5)},
                              {FastMath.toRadians(30),FastMath.toRadians(4)},
                              {FastMath.toRadians(60),FastMath.toRadians(3)},
                              {FastMath.toRadians(90),FastMath.toRadians(2)},
                              {FastMath.toRadians(120),FastMath.toRadians(3)},
                              {FastMath.toRadians(150),FastMath.toRadians(4)},
                              {FastMath.toRadians(180),FastMath.toRadians(5)},
                              {FastMath.toRadians(210),FastMath.toRadians(6)},
                              {FastMath.toRadians(240),FastMath.toRadians(5)},
                              {FastMath.toRadians(270),FastMath.toRadians(4)},
                              {FastMath.toRadians(300),FastMath.toRadians(3)},
                              {FastMath.toRadians(330),FastMath.toRadians(4)}};
        ElevationMask mask = new ElevationMask(maskValues);
        ElevationDetector detector = new ElevationDetector(topo)
                                            .withElevationMask(mask)
                                            .withHandler(new StopOnIncreasing<ElevationDetector>());
        Assert.assertSame(mask, detector.getElevationMask());

        AbsoluteDate startDate = new AbsoluteDate(2003, 9, 15, 20, 0, 0, utc);
        propagator.resetInitialState(propagator.propagate(startDate));
        propagator.addEventDetector(detector);
        final SpacecraftState fs = propagator.propagate(startDate.shiftedBy(Constants.JULIAN_DAY));
        double elevation = topo.getElevation(fs.getPVCoordinates().getPosition(), fs.getFrame(), fs.getDate());
        Assert.assertEquals(0.065, elevation, 2.0e-5);

    }


    @Test
    public void testHorizon() throws OrekitException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date, mu);

        Propagator propagator =
            new EcksteinHechlerPropagator(orbit, ae, mu, c20, c30, c40, c50, c60);

        // Earth and frame
        double ae =  6378137.0; // equatorial radius in meter
        double f  =  1.0 / 298.257223563; // flattening
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
        BodyShape earth = new OneAxisEllipsoid(ae, f, itrf);
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(48.833),
                                                FastMath.toRadians(2.333),
                                                0.0);
        TopocentricFrame topo = new TopocentricFrame(earth, point, "Gstation");
        AtmosphericRefractionModel refractionModel = new EarthStandardAtmosphereRefraction();
        ElevationDetector detector = new ElevationDetector(topo)
                                            .withRefraction(refractionModel)
                                            .withHandler(new StopOnIncreasing<ElevationDetector>());
        Assert.assertSame(refractionModel, detector.getRefractionModel());

        AbsoluteDate startDate = new AbsoluteDate(2003, 9, 15, 20, 0, 0, utc);
        propagator.resetInitialState(propagator.propagate(startDate));
        propagator.addEventDetector(detector);
        final SpacecraftState fs = propagator.propagate(startDate.shiftedBy(Constants.JULIAN_DAY));
        double elevation = topo.getElevation(fs.getPVCoordinates().getPosition(), fs.getFrame(), fs.getDate());
        Assert.assertEquals(FastMath.toRadians(-0.5746255623877098), elevation, 2.0e-5);

    }


    @Test
    public void testIssue136() throws OrekitException {

        //  Initial state definition : date, orbit
        AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
        Frame inertialFrame = FramesFactory.getEME2000(); // inertial frame for orbit definition
        Orbit initialOrbit = new KeplerianOrbit(6828137.005, 7.322641382145889e-10, 1.6967079057368113,
                                                0.0, 1.658054062748353,
                                                0.0001223149429077902, PositionAngle.MEAN,
                                                inertialFrame, initialDate, Constants.EIGEN5C_EARTH_MU);

        // Propagator : consider a simple keplerian motion (could be more elaborate)
        Propagator kepler = new EcksteinHechlerPropagator(initialOrbit,
                                                          Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.EGM96_EARTH_MU,
                                                          Constants.EGM96_EARTH_C20, 0.0, 0.0, 0.0, 0.0);

        // Earth and frame
        double ae =  6378137.0; // equatorial radius in meter
        double f  =  1.0 / 298.257223563; // flattening
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
        BodyShape earth = new OneAxisEllipsoid(ae, f, itrf);

        // Station
        final double longitude = FastMath.toRadians(-147.5);
        final double latitude  = FastMath.toRadians(64);
        final double altitude  = 160;
        final GeodeticPoint station1 = new GeodeticPoint(latitude, longitude, altitude);
        final TopocentricFrame sta1Frame = new TopocentricFrame(earth, station1, "station1");

        // Event definition
        final double maxcheck  = 120.0;
        final double elevation = FastMath.toRadians(5.);
        final double threshold = 10.0;
        final EventDetector rawEvent = new ElevationDetector(maxcheck, threshold, sta1Frame)
                                                .withConstantElevation(elevation)
                                                .withHandler(new ContinueOnEvent<ElevationDetector>());
        final EventsLogger logger = new EventsLogger();
        kepler.addEventDetector(logger.monitorDetector(rawEvent));

        // Propagate from the initial date to the first raising or for the fixed duration
        kepler.propagate(initialDate.shiftedBy(60*60*24.0*40));
        int countIncreasing = 0;
        int countDecreasing = 0;
        for (LoggedEvent le : logger.getLoggedEvents()) {
            if (le.isIncreasing()) {
                ++countIncreasing;
            } else {
                ++countDecreasing;
            }
        }
        Assert.assertEquals(314, countIncreasing);
        Assert.assertEquals(314, countDecreasing);

    }

    @Test
    public void testIssue110() throws OrekitException {

        // KEPLERIAN PROPAGATOR
        final Frame eme2000Frame = FramesFactory.getEME2000();
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final double a = 7000000.0;
        final Orbit initialOrbit = new KeplerianOrbit(a, 0.0,
                FastMath.PI / 2.2, 0.0, FastMath.PI / 2., 0.0,
                PositionAngle.TRUE, eme2000Frame, initDate,
                Constants.EGM96_EARTH_MU);
        final KeplerianPropagator kProp = new KeplerianPropagator(initialOrbit);

        // earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Ground station
        final GeodeticPoint stat = new GeodeticPoint(FastMath.toRadians(35.),
                FastMath.toRadians(149.8), 0.);
        final TopocentricFrame station = new TopocentricFrame(earthShape, stat,
                "GSTATION");

        // detector creation
        // =================
        final double maxCheck = 600.;
        final double threshold = 1.0e-3;
        final EventDetector rawEvent = new ElevationDetector(maxCheck, threshold, station)
                                                    .withConstantElevation(FastMath.toRadians(5.0))
                                                    .withHandler(new ContinueOnEvent<ElevationDetector>());
        final EventsLogger logger = new EventsLogger();
        kProp.addEventDetector(logger.monitorDetector(rawEvent));

        // PROPAGATION with DETECTION
        final AbsoluteDate finalDate = initDate.shiftedBy(30 * 60.);

        kProp.propagate(finalDate);
        Assert.assertEquals(2, logger.getLoggedEvents().size());
        Assert.assertTrue(logger.getLoggedEvents().get(0).isIncreasing());
        Assert.assertEquals(478.945, logger.getLoggedEvents().get(0).getState().getDate().durationFrom(initDate), 1.0e-3);
        Assert.assertFalse(logger.getLoggedEvents().get(1).isIncreasing());
        Assert.assertEquals(665.721, logger.getLoggedEvents().get(1).getState().getDate().durationFrom(initDate), 1.0e-3);

    }


    public void testPresTemp() throws OrekitException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date, mu);

        Propagator propagator =
            new EcksteinHechlerPropagator(orbit, ae, mu, c20, c30, c40, c50, c60);

        // Earth and frame
        double ae =  6378137.0; // equatorial radius in meter
        double f  =  1.0 / 298.257223563; // flattening
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
        BodyShape earth = new OneAxisEllipsoid(ae, f, itrf);
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(48.833),
                                                FastMath.toRadians(2.333),
                                                0.0);
        TopocentricFrame topo = new TopocentricFrame(earth, point, "Gstation");
        EarthStandardAtmosphereRefraction refractionModel = new EarthStandardAtmosphereRefraction();
        ElevationDetector detector = new ElevationDetector(topo)
                                                 .withRefraction(refractionModel)
                                                 .withHandler(new StopOnIncreasing<ElevationDetector>());
        refractionModel.setPressure(101325);
        refractionModel.setTemperature(290);

        AbsoluteDate startDate = new AbsoluteDate(2003, 9, 15, 20, 0, 0, utc);
        propagator.resetInitialState(propagator.propagate(startDate));
        propagator.addEventDetector(detector);
        final SpacecraftState fs = propagator.propagate(startDate.shiftedBy(Constants.JULIAN_DAY));
        double elevation = topo.getElevation(fs.getPVCoordinates().getPosition(), fs.getFrame(), fs.getDate());
        Assert.assertEquals(FastMath.toRadians(1.7026104902251749), elevation, 2.0e-5);

    }

    @Test
    public void testIssue203() throws OrekitException {

        //  Initial state definition : date, orbit
        AbsoluteDate initialDate = new AbsoluteDate("2012-01-26T07:00:00.000", TimeScalesFactory.getUTC());

        Frame inertialFrame = FramesFactory.getEME2000(); // inertial frame for orbit definition

        Orbit initialOrbit = new KeplerianOrbit(6828137.5, 7.322641060181212E-8, 1.7082667003713938, 0.0, 1.658054062748353, 1.2231496082116026E-4, PositionAngle.TRUE , inertialFrame, initialDate, Constants.WGS84_EARTH_MU);

        Propagator propagator =
                new EcksteinHechlerPropagator(initialOrbit,
                        Constants.EGM96_EARTH_EQUATORIAL_RADIUS,
                        Constants.EGM96_EARTH_MU,
                        Constants.EGM96_EARTH_C20,
                        Constants.EGM96_EARTH_C30,
                        Constants.EGM96_EARTH_C40,
                        Constants.EGM96_EARTH_C50,
                        Constants.EGM96_EARTH_C60);

        // Earth and frame
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                               Constants.WGS84_EARTH_FLATTENING,
                                               earthFrame);

        // Station
        final double longitude = FastMath.toRadians(21.0);
        final double latitude  = FastMath.toRadians(67.9);
        final double altitude  = 300.0;

        final GeodeticPoint station1 = new GeodeticPoint(latitude, longitude, altitude);
        final TopocentricFrame sta1Frame = new TopocentricFrame(earth, station1, "station1");

        double [][] maskValues = {
            {-0.017453292519943098, 0.006981317007977318}, {0.0, 0.006981317007977318},
            {0.017453292519943295, 0.006981317007977318}, {0.03490658503988659, 0.010471975511965976},
            {0.05235987755982989, 0.012217304763960306}, {0.06981317007977318, 0.010471975511965976},
            {0.08726646259971647, 0.010471975511965976}, {0.10471975511965978, 0.012217304763960306},
            {0.12217304763960307, 0.010471975511965976}, {0.13962634015954636, 0.008726646259971648},
            {0.15707963267948966, 0.008726646259971648}, {0.17453292519943295, 0.006981317007977318},
            {0.19198621771937624, 0.006981317007977318}, {0.20943951023931956, 0.006981317007977318},
            {0.22689280275926285, 0.005235987755982988}, {0.24434609527920614, 0.005235987755982988},
            {0.2617993877991494, 0.003490658503988659}, {0.2792526803190927, 0.003490658503988659},
            {0.29670597283903605, 0.0017453292519943296}, {0.3141592653589793, 0.003490658503988659},
            {0.33161255787892263, 0.0017453292519943296}, {0.3490658503988659, 0.0},
            {0.3665191429188092, 0.012217304763960306}, {0.3839724354387525, 0.012217304763960306},
            {0.4014257279586958, 0.0}, {0.4188790204786391, 0.010471975511965976},
            {0.4363323129985824, 0.029670597283903602}, {0.4537856055185257, 0.029670597283903602},
            {0.47123889803846897, 0.017453292519943295}, {0.4886921905584123, 0.0},
            {0.5061454830783556, 0.029670597283903602}, {0.5235987755982988, 0.015707963267948967},
            {0.5410520681182421, 0.005235987755982988}, {0.5585053606381855, 0.024434609527920613},
            {0.5759586531581288, 0.041887902047863905}, {0.5934119456780721, 0.06283185307179587},
            {0.6108652381980153, 0.05235987755982989}, {0.6283185307179586, 0.05759586531581287},
            {0.6457718232379019, 0.061086523819801536}, {0.6632251157578453, 0.05759586531581287},
            {0.6806784082777885, 0.04363323129985824}, {0.6981317007977318, 0.059341194567807204},
            {0.7155849933176751, 0.07504915783575616}, {0.7330382858376184, 0.08726646259971647},
            {0.7504915783575618, 0.08726646259971647}, {0.767944870877505, 0.07330382858376185},
            {0.7853981633974483, 0.07853981633974483}, {0.8028514559173916, 0.09075712110370514},
            {0.8203047484373349, 0.0942477796076938}, {0.8377580409572782, 0.11519173063162574},
            {0.8552113334772214, 0.11519173063162574}, {0.8726646259971648, 0.12566370614359174},
            {0.8901179185171081, 0.12566370614359174}, {0.9075712110370514, 0.10122909661567112},
            {0.9250245035569946, 0.11868238913561441}, {0.9424777960769379, 0.11868238913561441},
            {0.9599310885968813, 0.11868238913561441}, {0.9773843811168246, 0.10821041362364843},
            {0.9948376736367679, 0.12217304763960307}, {1.0122909661567112, 0.12740903539558607},
            {1.0297442586766545, 0.11344640137963143}, {1.0471975511965976, 0.11344640137963143},
            {1.064650843716541, 0.06632251157578452}, {1.0821041362364843, 0.12391837689159739},
            {1.0995574287564276, 0.12391837689159739}, {1.117010721276371, 0.10995574287564276},
            {1.1344640137963142, 0.09250245035569947}, {1.1519173063162575, 0.12740903539558607},
            {1.1693705988362009, 0.1308996938995747}, {1.1868238913561442, 0.1117010721276371},
            {1.2042771838760873, 0.1117010721276371}, {1.2217304763960306, 0.0942477796076938},
            {1.239183768915974, 0.10821041362364843}, {1.2566370614359172, 0.09599310885968812},
            {1.2740903539558606, 0.09948376736367678}, {1.2915436464758039, 0.09773843811168245},
            {1.3089969389957472, 0.08726646259971647}, {1.3264502315156905, 0.09250245035569947},
            {1.3439035240356338, 0.10122909661567112}, {1.361356816555577, 0.09250245035569947},
            {1.3788101090755203, 0.08552113334772216}, {1.3962634015954636, 0.08726646259971647},
            {1.413716694115407, 0.08028514559173916}, {1.4311699866353502, 0.05759586531581287},
            {1.4486232791552935, 0.054105206811824215}, {1.4660765716752369, 0.06632251157578452},
            {1.4835298641951802, 0.08028514559173916}, {1.5009831567151235, 0.061086523819801536},
            {1.5184364492350666, 0.048869219055841226}, {1.53588974175501, 0.0715584993317675},
            {1.5533430342749532, 0.07504915783575616}, {1.5707963267948966, 0.05235987755982989},
            {1.5882496193148399, 0.05235987755982989}, {1.6057029118347832, 0.06981317007977318},
            {1.6231562043547265, 0.054105206811824215}, {1.6406094968746698, 0.0645771823237902},
            {1.6580627893946132, 0.059341194567807204}, {1.6755160819145565, 0.029670597283903602},
            {1.6929693744344996, 0.03316125578789226}, {1.710422666954443, 0.059341194567807204},
            {1.7278759594743862, 0.0645771823237902}, {1.7453292519943295, 0.06283185307179587},
            {1.7627825445142729, 0.061086523819801536}, {1.7802358370342162, 0.06806784082777885},
            {1.7976891295541595, 0.06632251157578452}, {1.8151424220741028, 0.059341194567807204},
            {1.8325957145940461, 0.05759586531581287}, {1.8500490071139892, 0.05759586531581287},
            {1.8675022996339325, 0.0471238898038469}, {1.8849555921538759, 0.059341194567807204},
            {1.9024088846738192, 0.05061454830783556}, {1.9198621771937625, 0.05061454830783556},
            {1.9373154697137058, 0.024434609527920613}, {1.9547687622336491, 0.027925268031909273},
            {1.9722220547535925, 0.041887902047863905}, {1.9896753472735358, 0.024434609527920613},
            {2.007128639793479, 0.029670597283903602}, {2.0245819323134224, 0.03316125578789226},
            {2.0420352248333655, 0.03665191429188092}, {2.059488517353309, 0.04537856055185257},
            {2.076941809873252, 0.041887902047863905}, {2.0943951023931953, 0.05759586531581287},
            {2.111848394913139, 0.06283185307179587}, {2.129301687433082, 0.06806784082777885},
            {2.1467549799530254, 0.05759586531581287}, {2.1642082724729685, 0.059341194567807204},
            {2.181661564992912, 0.06806784082777885}, {2.199114857512855, 0.06283185307179587},
            {2.2165681500327987, 0.054105206811824215}, {2.234021442552742, 0.05235987755982989},
            {2.251474735072685, 0.059341194567807204}, {2.2689280275926285, 0.07330382858376185},
            {2.2863813201125716, 0.06283185307179587}, {2.303834612632515, 0.05235987755982989},
            {2.321287905152458, 0.061086523819801536}, {2.3387411976724017, 0.048869219055841226},
            {2.356194490192345, 0.06632251157578452}, {2.3736477827122884, 0.05061454830783556},
            {2.3911010752322315, 0.07679448708775051}, {2.4085543677521746, 0.06283185307179587},
            {2.426007660272118, 0.05585053606381855}, {2.443460952792061, 0.061086523819801536},
            {2.4609142453120048, 0.06806784082777885}, {2.478367537831948, 0.0645771823237902},
            {2.4958208303518914, 0.06283185307179587}, {2.5132741228718345, 0.06283185307179587},
            {2.530727415391778, 0.0645771823237902}, {2.548180707911721, 0.07504915783575616},
            {2.5656340004316642, 0.0715584993317675}, {2.5830872929516078, 0.0645771823237902},
            {2.600540585471551, 0.059341194567807204}, {2.6179938779914944, 0.06283185307179587},
            {2.6354471705114375, 0.06632251157578452}, {2.652900463031381, 0.06283185307179587},
            {2.670353755551324, 0.06632251157578452}, {2.6878070480712677, 0.0645771823237902}};
        ElevationMask mask = new ElevationMask(maskValues);


        final AbsoluteDate start = new AbsoluteDate("2012-02-10T22:00:00.000", TimeScalesFactory.getUTC());
        final AbsoluteDate end   = initialDate.shiftedBy(1000 * Constants.JULIAN_DAY);

        // Event definition
        final double maxcheck  = 60.0;
        final double threshold =  2.0; // 0.001;
        final EventDetector sta1Visi =
                new ElevationDetector(maxcheck, threshold, sta1Frame).
                withElevationMask(mask).
                withHandler(new EventHandler<ElevationDetector>() {

                    private int count = 6;
                    @Override
                    public Action eventOccurred(SpacecraftState s, ElevationDetector detector, boolean increasing) {
                        return (--count > 0) ? Action.CONTINUE : Action.STOP;
                    }

                });

        // Add event to be detected
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(sta1Visi));

        // Propagate until the sixth event
        propagator.propagate(start, end);

        List<LoggedEvent> events = logger.getLoggedEvents();
        Assert.assertEquals(6, events.size());

        // despite the events 2 and 3 are closer to each other than the convergence threshold
        // the second one is not merged into the first one
        AbsoluteDate d2 = events.get(2).getState().getDate();
        AbsoluteDate d3 = events.get(3).getState().getDate();
        Assert.assertEquals(1.529, d3.durationFrom(d2), 0.01);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
        ae  = 6.378137e6;
        c20 = -1.08263e-3;
        c30 = 2.54e-6;
        c40 = 1.62e-6;
        c50 = 2.3e-7;
        c60 = -5.5e-7;
    }

    @After
    public void tearDown() {
        mu   = Double.NaN;
        ae   = Double.NaN;
        c20  = Double.NaN;
        c30  = Double.NaN;
        c40  = Double.NaN;
        c50  = Double.NaN;
        c60  = Double.NaN;
    }

}

