/* Copyright 2002-2013 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
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
import org.orekit.propagation.events.handlers.DetectorContinueOnEvent;
import org.orekit.propagation.events.handlers.DetectorEventHandler;
import org.orekit.propagation.events.handlers.DetectorStopOnIncreasing;
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
        Frame ITRF2005 = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
        BodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(48.833),
                                                FastMath.toRadians(2.333),
                                                0.0);
        TopocentricFrame topo = new TopocentricFrame(earth, point, "Gstation");
        Checking checking = new Checking(topo);
        ElevationDetector detector =
                new ElevationDetector(topo).
                withConstantElevation(FastMath.toRadians(5.0)).
                withHandler(checking);

        AbsoluteDate startDate = new AbsoluteDate(2003, 9, 15, 12, 0, 0, utc);
        propagator.resetInitialState(propagator.propagate(startDate));
        propagator.addEventDetector(detector);
        propagator.setMasterMode(10.0, checking);
        propagator.propagate(startDate.shiftedBy(Constants.JULIAN_DAY));

    }

    private static class Checking implements DetectorEventHandler<ElevationDetector>, OrekitFixedStepHandler {

        private TopocentricFrame topo;
        private boolean visible;

        public Checking(final TopocentricFrame topo) {
            this.topo = topo;
            this.visible = false;
        }

        public EventDetector.Action eventOccurred(SpacecraftState s, ElevationDetector detector, boolean increasing) {
            visible = increasing;
            return EventDetector.Action.CONTINUE;
        }

        public SpacecraftState resetState(ElevationDetector detector, SpacecraftState oldState) {
            return oldState;
        }

        public void init(SpacecraftState s0, AbsoluteDate t) {
        }

        public void handleStep(SpacecraftState currentState, boolean isLast)
        throws PropagationException {
            try {
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

            } catch (OrekitException e) {
                throw new PropagationException(e);
            }

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
        Frame ITRF2005 = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
        BodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
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
                                            .withHandler(new DetectorStopOnIncreasing<ElevationDetector>());

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
        Frame ITRF2005 = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
        BodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(48.833),
                                                FastMath.toRadians(2.333),
                                                0.0);
        TopocentricFrame topo = new TopocentricFrame(earth, point, "Gstation");
        AtmosphericRefractionModel refractionModel = new EarthStandardAtmosphereRefraction();
        ElevationDetector detector = new ElevationDetector(topo)
                                            .withRefraction(refractionModel)
                                            .withHandler(new DetectorStopOnIncreasing<ElevationDetector>());

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
        Frame ITRF2005 = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
        BodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);

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
                                                .withHandler(new DetectorContinueOnEvent<ElevationDetector>());
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
                                                    .withHandler(new DetectorContinueOnEvent<ElevationDetector>());
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
        Frame ITRF2005 = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame at an arbitrary date
        BodyShape earth = new OneAxisEllipsoid(ae, f, ITRF2005);
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(48.833),
                                                FastMath.toRadians(2.333),
                                                0.0);
        TopocentricFrame topo = new TopocentricFrame(earth, point, "Gstation");
        EarthStandardAtmosphereRefraction refractionModel = new EarthStandardAtmosphereRefraction();
        ElevationDetector detector = new ElevationDetector(topo)
                                                 .withRefraction(refractionModel)
                                                 .withHandler(new DetectorStopOnIncreasing<ElevationDetector>());
        refractionModel.setPressure(101325);
        refractionModel.setTemperature(290);

        AbsoluteDate startDate = new AbsoluteDate(2003, 9, 15, 20, 0, 0, utc);
        propagator.resetInitialState(propagator.propagate(startDate));
        propagator.addEventDetector(detector);
        final SpacecraftState fs = propagator.propagate(startDate.shiftedBy(Constants.JULIAN_DAY));
        double elevation = topo.getElevation(fs.getPVCoordinates().getPosition(), fs.getFrame(), fs.getDate());
        Assert.assertEquals(FastMath.toRadians(1.7026104902251749), elevation, 2.0e-5);

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

