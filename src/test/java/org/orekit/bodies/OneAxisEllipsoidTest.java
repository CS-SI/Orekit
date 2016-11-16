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
package org.orekit.bodies;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.geometry.euclidean.oned.Vector1D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.random.SobolSequenceGenerator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;


public class OneAxisEllipsoidTest {

    @Test
    public void testStandard() throws OrekitException {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    4637885.347, 121344.608, 4362452.869,
                                    0.026157811533131, 0.757987116290729, 260.455572965555);
    }

    @Test
    public void testLongitudeZero() throws OrekitException {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    6378400.0, 0, 6379000.0,
                                    0.0, 0.787815771252351, 2653416.77864152);
    }

    @Test
    public void testLongitudePi() throws OrekitException {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    -6379999.0, 0, 6379000.0,
                                    3.14159265358979, 0.787690146758403, 2654544.7767725);
    }

    @Test
    public void testNorthPole() throws OrekitException {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    0.0, 0.0, 7000000.0,
                                    0.0, 1.57079632679490, 643247.685859644);
    }

    @Test
    public void testEquator() throws OrekitException {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    6379888.0, 6377000.0, 0.0,
                                    0.785171775899913, 0.0, 2642345.24279301);
    }

    @Test
    public void testNoFlattening() throws OrekitException {
        final double r      = 7000000.0;
        final double lambda = 2.345;
        final double phi    = -1.23;
        final double cL = FastMath.cos(lambda);
        final double sL = FastMath.sin(lambda);
        final double cH = FastMath.cos(phi);
        final double sH = FastMath.sin(phi);
        checkCartesianToEllipsoidic(6378137.0, 0,
                                    r * cL * cH, r * sL * cH, r * sH,
                                    lambda, phi, r - 6378137.0);
    }

    @Test
    public void testNoFlatteningPolar() throws OrekitException {
        final double r = 1000.0;
        final double h = 100;
        checkCartesianToEllipsoidic(r, 0,
                                    0, 0, r + h,
                                    0, 0.5 * FastMath.PI, h);
        checkCartesianToEllipsoidic(r, 0,
                                    0, 0, r - h,
                                    0, 0.5 * FastMath.PI, -h);
        checkCartesianToEllipsoidic(r, 0,
                                    0, 0, -(r + h),
                                    0, -0.5 * FastMath.PI, h);
        checkCartesianToEllipsoidic(r, 0,
                                    0, 0, -(r - h),
                                    0, -0.5 * FastMath.PI, -h);
    }

    @Test
    public void testOnSurface() throws OrekitException {
        Vector3D surfacePoint = new Vector3D(-1092200.775949484,
                                             -3944945.7282234835,
                                              4874931.946956173);
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101,
                                                           FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        GeodeticPoint gp = earthShape.transform(surfacePoint, earthShape.getBodyFrame(),
                                                   AbsoluteDate.J2000_EPOCH);
        Vector3D rebuilt = earthShape.transform(gp);
        Assert.assertEquals(0, rebuilt.distance(surfacePoint), 3.0e-9);
    }

    @Test
    public void testInside3Roots() throws OrekitException {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
                                    9219.0, -5322.0, 6056743.0,
                                    5.75963470503781, 1.56905114598949, -300000.009586231);
    }

    @Test
    public void testInsideLessThan3Roots() throws OrekitException {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
                                    1366863.0, -789159.0, -5848.988,
                                    -0.523598928689, -0.00380885831963, -4799808.27951);
    }

    @Test
    public void testOutside() throws OrekitException {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
                                    5722966.0, -3304156.0, -24621187.0,
                                    5.75958652642615, -1.3089969725151, 19134410.3342696);
    }

    @Test
    public void testGeoCar() throws OrekitException {
        OneAxisEllipsoid model =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101,
                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        GeodeticPoint nsp =
            new GeodeticPoint(0.852479154923577, 0.0423149994747243, 111.6);
        Vector3D p = model.transform(nsp);
        Assert.assertEquals(4201866.69291890, p.getX(), 1.0e-6);
        Assert.assertEquals(177908.184625686, p.getY(), 1.0e-6);
        Assert.assertEquals(4779203.64408617, p.getZ(), 1.0e-6);
    }

    @Test
    public void testGroundProjectionPosition() throws OrekitException {
        OneAxisEllipsoid model =
            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                 Constants.WGS84_EARTH_FLATTENING,
                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        TimeStampedPVCoordinates initPV =
                new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(584.),
                                             new Vector3D(3220103., 69623., 6449822.),
                                             new Vector3D(6414.7, -2006., -3180.),
                                             Vector3D.ZERO);
        Frame eme2000 = FramesFactory.getEME2000();
        Orbit orbit = new EquinoctialOrbit(initPV, eme2000, Constants.EIGEN5C_EARTH_MU);

        for (double dt = 0; dt < 3600.0; dt += 60.0) {

            TimeStampedPVCoordinates pv = orbit.getPVCoordinates(orbit.getDate().shiftedBy(dt), eme2000);
            TimeStampedPVCoordinates groundPV = model.projectToGround(pv, eme2000);
            Vector3D groundP = model.projectToGround(pv.getPosition(), pv.getDate(), eme2000);

            // check methods projectToGround and transform are consistent with each other
            Assert.assertEquals(model.transform(pv.getPosition(), eme2000, pv.getDate()).getLatitude(),
                                model.transform(groundPV.getPosition(), eme2000, pv.getDate()).getLatitude(),
                                1.0e-10);
            Assert.assertEquals(model.transform(pv.getPosition(), eme2000, pv.getDate()).getLongitude(),
                                model.transform(groundPV.getPosition(), eme2000, pv.getDate()).getLongitude(),
                                1.0e-10);
            Assert.assertEquals(0.0, Vector3D.distance(groundP, groundPV.getPosition()), 1.0e-15 * groundP.getNorm());

        }

    }

    @Test
    public void testGroundProjectionDerivatives()
            throws OrekitException {
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame eme2000 = FramesFactory.getEME2000();
        OneAxisEllipsoid model =
            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                 Constants.WGS84_EARTH_FLATTENING,
                                 itrf);

        TimeStampedPVCoordinates initPV =
                new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(584.),
                                             new Vector3D(3220103., 69623., 6449822.),
                                             new Vector3D(6414.7, -2006., -3180.),
                                             Vector3D.ZERO);
        Orbit orbit = new EquinoctialOrbit(initPV, eme2000, Constants.EIGEN5C_EARTH_MU);

        double[] errors = derivativesErrors(orbit, orbit.getDate(), eme2000, model);
        Assert.assertEquals(0, errors[0], 1.0e-16);
        Assert.assertEquals(0, errors[1], 2.0e-12);
        Assert.assertEquals(0, errors[2], 2.0e-4);

    }

    @Test
    public void testGroundToGroundIssue181()
            throws OrekitException {
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame eme2000 = FramesFactory.getEME2000();
        OneAxisEllipsoid model =
            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                 Constants.WGS84_EARTH_FLATTENING,
                                 itrf);

        TimeStampedPVCoordinates initPV =
                new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(584.),
                                             new Vector3D(3220103., 69623., 6449822.),
                                             new Vector3D(6414.7, -2006., -3180.),
                                             Vector3D.ZERO);
        TimeStampedPVCoordinates body = itrf.getTransformTo(eme2000, initPV.getDate()).transformPVCoordinates(initPV);
        TimeStampedPVCoordinates ground1 = model.projectToGround(body,    itrf);
        TimeStampedPVCoordinates ground2 = model.projectToGround(ground1, itrf);
        Assert.assertEquals(0.0, Vector3D.distance(ground1.getPosition(),     ground2.getPosition()),     1.0e-12);
        Assert.assertEquals(0.0, Vector3D.distance(ground1.getVelocity(),     ground2.getVelocity()),     2.0e-12);
        Assert.assertEquals(0.0, Vector3D.distance(ground1.getAcceleration(), ground2.getAcceleration()), 1.0e-12);

    }

    private double[] derivativesErrors(PVCoordinatesProvider provider, AbsoluteDate date, Frame frame,
                                       OneAxisEllipsoid model)
        throws OrekitException {
        List<TimeStampedPVCoordinates> pvList       = new ArrayList<TimeStampedPVCoordinates>();
        List<TimeStampedPVCoordinates> groundPVList = new ArrayList<TimeStampedPVCoordinates>();
        for (double dt = -0.25; dt <= 0.25; dt += 0.125) {
            TimeStampedPVCoordinates shiftedPV = provider.getPVCoordinates(date.shiftedBy(dt), frame);
            Vector3D p = model.projectToGround(shiftedPV.getPosition(), shiftedPV.getDate(), frame);
            pvList.add(shiftedPV);
            groundPVList.add(new TimeStampedPVCoordinates(shiftedPV.getDate(),
                                                          p, Vector3D.ZERO, Vector3D.ZERO));
        }
        TimeStampedPVCoordinates computed =
                model.projectToGround(TimeStampedPVCoordinates.interpolate(date,
                                                                           CartesianDerivativesFilter.USE_P,
                                                                           pvList),
                                                                           frame);
        TimeStampedPVCoordinates reference =
                TimeStampedPVCoordinates.interpolate(date,
                                                     CartesianDerivativesFilter.USE_P,
                                                     groundPVList);

        TimeStampedPVCoordinates pv0 = provider.getPVCoordinates(date, frame);
        Vector3D p0 = pv0.getPosition();
        Vector3D v0 = pv0.getVelocity();
        Vector3D a0 = pv0.getAcceleration();

        return new double[] {
            Vector3D.distance(computed.getPosition(),     reference.getPosition())     / p0.getNorm(),
            Vector3D.distance(computed.getVelocity(),     reference.getVelocity())     / v0.getNorm(),
            Vector3D.distance(computed.getAcceleration(), reference.getAcceleration()) / a0.getNorm(),
        };

    }

    @Test
    public void testGroundProjectionTaylor()
            throws OrekitException {
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame eme2000 = FramesFactory.getEME2000();
        OneAxisEllipsoid model =
            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                 Constants.WGS84_EARTH_FLATTENING,
                                 itrf);

        TimeStampedPVCoordinates initPV =
                new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(584.),
                                             new Vector3D(3220103., 69623., 6449822.),
                                             new Vector3D(6414.7, -2006., -3180.),
                                             Vector3D.ZERO);
        Orbit orbit = new EquinoctialOrbit(initPV, eme2000, Constants.EIGEN5C_EARTH_MU);

        TimeStampedPVCoordinates pv0 = orbit.getPVCoordinates(orbit.getDate(), model.getBodyFrame());
        PVCoordinatesProvider groundTaylor =
                model.projectToGround(pv0, model.getBodyFrame()).toTaylorProvider(model.getBodyFrame());

        TimeStampedPVCoordinates g0 = groundTaylor.getPVCoordinates(orbit.getDate(), model.getBodyFrame());
        Vector3D zenith       = pv0.getPosition().subtract(g0.getPosition()).normalize();
        Vector3D acrossTrack  = Vector3D.crossProduct(zenith, g0.getVelocity()).normalize();
        Vector3D alongTrack   = Vector3D.crossProduct(acrossTrack, zenith).normalize();
        for (double dt = -1; dt < 1; dt += 0.01) {
            AbsoluteDate date = orbit.getDate().shiftedBy(dt);
            Vector3D taylorP = groundTaylor.getPVCoordinates(date, model.getBodyFrame()).getPosition();
            Vector3D refP    = model.projectToGround(orbit.getPVCoordinates(date, model.getBodyFrame()).getPosition(),
                                                     date, model.getBodyFrame());
            Vector3D delta = taylorP.subtract(refP);
            Assert.assertEquals(0.0, Vector3D.dotProduct(delta, alongTrack),  0.0015);
            Assert.assertEquals(0.0, Vector3D.dotProduct(delta, acrossTrack), 0.0007);
            Assert.assertEquals(0.0, Vector3D.dotProduct(delta, zenith),      0.00002);
        }

    }

    @Test
    public void testLineIntersection() throws OrekitException {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9, frame);
        Vector3D point         = new Vector3D(0.0, 93.7139699, 3.5930796);
        Vector3D direction     = new Vector3D(0.0, 1.0, 1.0);
        Line line = new Line(point, point.add(direction), 1.0e-10);
        GeodeticPoint gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertEquals(gp.getAltitude(), 0.0, 1.0e-12);
        Assert.assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9, frame);
        point = new Vector3D(0.0, -93.7139699, -3.5930796);
        direction = new Vector3D(0.0, -1.0, -1.0);
        line = new Line(point, point.add(direction), 1.0e-10).revert();
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9, frame);
        point = new Vector3D(0.0, -93.7139699, 3.5930796);
        direction = new Vector3D(0.0, -1.0, 1.0);
        line = new Line(point, point.add(direction), 1.0e-10);
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9, frame);
        point = new Vector3D(-93.7139699, 0.0, 3.5930796);
        direction = new Vector3D(-1.0, 0.0, 1.0);
        line = new Line(point, point.add(direction), 1.0e-10);
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertTrue(line.contains(model.transform(gp)));
        Assert.assertFalse(line.contains(new Vector3D(0, 0, 7000000)));

        point = new Vector3D(0.0, 0.0, 110);
        direction = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(point, point.add(direction), 1.0e-10);
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertEquals(gp.getLatitude(), FastMath.PI/2, 1.0e-12);

        point = new Vector3D(0.0, 110, 0);
        direction = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(point, point.add(direction), 1.0e-10);
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assert.assertEquals(gp.getLatitude(),0, 1.0e-12);

    }

    @Test
    public void testNoLineIntersection() throws OrekitException {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9, frame);
        Vector3D point     = new Vector3D(0.0, 93.7139699, 3.5930796);
        Vector3D direction = new Vector3D(0.0, 9.0, -2.0);
        Line line = new Line(point, point.add(direction), 1.0e-10);
        Assert.assertNull(model.getIntersectionPoint(line, point, frame, date));
    }

    @Test
    public void testNegativeZ() throws OrekitException {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(90.0, 5.0 / 9.0, frame);
        Vector3D point     = new Vector3D(140.0, 0.0, -30.0);
        GeodeticPoint gp = model.transform(point, frame, date);
        Vector3D rebuilt = model.transform(gp);
        Assert.assertEquals(0.0, rebuilt.distance(point), 1.0e-10);
    }

    @Test
    public void testNumerousIteration() throws OrekitException {
        // this test, which corresponds to an unrealistic extremely flat ellipsoid,
        // is designed to need more than the usual 2 or 3 iterations in the iterative
        // version of the Toshio Fukushima's algorithm. It in fact would reach
        // convergence at iteration 17. However, despite we interrupt the loop
        // at iteration 9, the result is nevertheless very accurate
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 999.0 / 1000.0, frame);
        Vector3D point     = new Vector3D(100.001, 0.0, 1.0);
        GeodeticPoint gp = model.transform(point, frame, date);
        Vector3D rebuilt = model.transform(gp);
        Assert.assertEquals(0.0, rebuilt.distance(point), 8.0e-12);
    }

    @Test
    public void testEquatorialInside() throws OrekitException {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(90.0, 5.0 / 9.0, frame);
        for (double rho = 0; rho < model.getEquatorialRadius(); rho += 0.01) {
            Vector3D point     = new Vector3D(rho, 0.0, 0.0);
            GeodeticPoint gp = model.transform(point, frame, date);
            Vector3D rebuilt = model.transform(gp);
            Assert.assertEquals(0.0, rebuilt.distance(point), 1.0e-10);
        }
    }

    @Test
    public void testFarPoint() throws OrekitException {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(90.0, 5.0 / 9.0, frame);
        Vector3D point     = new Vector3D(1.0e15, 2.0e15, -1.0e12);
        GeodeticPoint gp = model.transform(point, frame, date);
        Vector3D rebuilt = model.transform(gp);
        Assert.assertEquals(0.0, rebuilt.distance(point), 1.0e-15 * point.getNorm());
    }

    @Test
    public void testIssue141() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate("2002-03-06T20:50:20.44188731559965033", TimeScalesFactory.getUTC());
        Frame frame = FramesFactory.getGTOD(IERSConventions.IERS_1996, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      frame);
        Vector3D point     = new Vector3D(-6838696.282102453, -2148321.403361013, -0.011907944179711194);
        GeodeticPoint gp = model.transform(point, frame, date);
        Vector3D rebuilt = model.transform(gp);
        Assert.assertEquals(0.0, rebuilt.distance(point), 1.0e-15 * point.getNorm());
    }

    @Test
    public void testSerialization() throws OrekitException, IOException, ClassNotFoundException {
        OneAxisEllipsoid original = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                         Constants.WGS84_EARTH_FLATTENING,
                                                         FramesFactory.getITRFEquinox(IERSConventions.IERS_1996, true));
        original.setAngularThreshold(1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        Assert.assertTrue(bos.size() > 250);
        Assert.assertTrue(bos.size() < 350);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        OneAxisEllipsoid deserialized  = (OneAxisEllipsoid) ois.readObject();
        Assert.assertEquals(original.getEquatorialRadius(), deserialized.getEquatorialRadius(), 1.0e-12);
        Assert.assertEquals(original.getFlattening(), deserialized.getFlattening(), 1.0e-12);

    }

    @Test
    public void testIntersectionFromPoints() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 03, 21),
                                             TimeComponents.H12,
                                             TimeScalesFactory.getUTC());

        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frame);

        // Satellite on polar position
        // ***************************
        final double mu = 3.9860047e14;
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, 0., FastMath.toRadians(90.), FastMath.toRadians(60.),
                                   FastMath.toRadians(90.), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in EME2000 and ITRF200B
        PVCoordinates pvSatEME2000 = circ.getPVCoordinates();
        PVCoordinates pvSatItrf  = frame.getTransformTo(FramesFactory.getEME2000(), date).transformPVCoordinates(pvSatEME2000);
        Vector3D pSatItrf  = pvSatItrf.getPosition();

        // Test first visible surface points
        GeodeticPoint geoPoint = new GeodeticPoint(FastMath.toRadians(70.), FastMath.toRadians(60.), 0.);
        Vector3D pointItrf     = earth.transform(geoPoint);
        Line line = new Line(pSatItrf, pointItrf, 1.0e-10);
        GeodeticPoint geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test second visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(65.), FastMath.toRadians(-120.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test non visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(30.), FastMath.toRadians(60.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);

        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);

        // For polar satellite position, intersection point is at the same longitude but different latitude
        Assert.assertEquals(1.04437199, geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(1.36198012, geoInter.getLatitude(),  Utils.epsilonAngle);

        // Satellite on equatorial position
        // ********************************
        circ =
            new CircularOrbit(7178000.0, 0.5e-4, 0., FastMath.toRadians(1.e-4), FastMath.toRadians(0.),
                                   FastMath.toRadians(0.), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in EME2000 and ITRF200B
        pvSatEME2000 = circ.getPVCoordinates();
        pvSatItrf  = frame.getTransformTo(FramesFactory.getEME2000(), date).transformPVCoordinates(pvSatEME2000);
        pSatItrf  = pvSatItrf.getPosition();

        // Test first visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(5.), FastMath.toRadians(0.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        Assert.assertTrue(line.toSubSpace(pSatItrf).getX() < 0);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // With the point opposite to satellite point along the line
        GeodeticPoint geoInter2 = earth.getIntersectionPoint(line, line.toSpace(new Vector1D(-line.toSubSpace(pSatItrf).getX())), frame, date);
        Assert.assertTrue(FastMath.abs(geoInter.getLongitude() - geoInter2.getLongitude()) > FastMath.toRadians(0.1));
        Assert.assertTrue(FastMath.abs(geoInter.getLatitude() - geoInter2.getLatitude()) > FastMath.toRadians(0.1));

        // Test second visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(-5.), FastMath.toRadians(0.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test non visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(40.), FastMath.toRadians(0.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(-0.00768481, geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals( 0.32180410, geoInter.getLatitude(),  Utils.epsilonAngle);


        // Satellite on any position
        // *************************
        circ =
            new CircularOrbit(7178000.0, 0.5e-4, 0., FastMath.toRadians(50.), FastMath.toRadians(0.),
                                   FastMath.toRadians(90.), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in EME2000 and ITRF200B
        pvSatEME2000 = circ.getPVCoordinates();
        pvSatItrf  = frame.getTransformTo(FramesFactory.getEME2000(), date).transformPVCoordinates(pvSatEME2000);
        pSatItrf  = pvSatItrf.getPosition();

        // Test first visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(40.), FastMath.toRadians(90.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test second visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(60.), FastMath.toRadians(90.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test non visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(90.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assert.assertEquals(FastMath.toRadians(89.5364061088196), geoInter.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(FastMath.toRadians(35.555543683351125), geoInter.getLatitude(), Utils.epsilonAngle);

    }

    @Test
    public void testMovingGeodeticPointSymmetry() throws OrekitException {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        double lat0 = FastMath.toRadians(60.0);
        double lon0 = FastMath.toRadians(25.0);
        double alt0 = 100.0;
        double lat1 =   1.0e-3;
        double lon1 =  -2.0e-3;
        double alt1 =   1.2;
        double lat2 =  -1.0e-5;
        double lon2 =  -3.0e-5;
        double alt2 =  -0.01;
        final DerivativeStructure latDS = new DerivativeStructure(1, 2, lat0, lat1, lat2);
        final DerivativeStructure lonDS = new DerivativeStructure(1, 2, lon0, lon1, lon2);
        final DerivativeStructure altDS = new DerivativeStructure(1, 2, alt0, alt1, alt2);

        // direct computation of position, velocity and acceleration
        PVCoordinates pv = earth.transform(new FieldGeodeticPoint<DerivativeStructure>(latDS, lonDS, altDS));
        FieldGeodeticPoint<DerivativeStructure> rebuilt = earth.transform(pv, earth.getBodyFrame(),null);
        Assert.assertEquals(lat0, rebuilt.getLatitude().getReal(),                1.0e-16);
        Assert.assertEquals(lat1, rebuilt.getLatitude().getPartialDerivative(1),  5.0e-19);
        Assert.assertEquals(lat2, rebuilt.getLatitude().getPartialDerivative(2),  5.0e-14);
        Assert.assertEquals(lon0, rebuilt.getLongitude().getReal(),               1.0e-16);
        Assert.assertEquals(lon1, rebuilt.getLongitude().getPartialDerivative(1), 5.0e-19);
        Assert.assertEquals(lon2, rebuilt.getLongitude().getPartialDerivative(2), 1.0e-20);
        Assert.assertEquals(alt0, rebuilt.getAltitude().getReal(),                2.0e-11);
        Assert.assertEquals(alt1, rebuilt.getAltitude().getPartialDerivative(1),  6.0e-13);
        Assert.assertEquals(alt2, rebuilt.getAltitude().getPartialDerivative(2),  2.0e-14);

    }

    @Test
    public void testMovingGeodeticPoint() throws OrekitException {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        double lat0 = FastMath.toRadians(60.0);
        double lon0 = FastMath.toRadians(25.0);
        double alt0 = 100.0;
        double lat1 =   1.0e-3;
        double lon1 =  -2.0e-3;
        double alt1 =   1.2;
        double lat2 =  -1.0e-5;
        double lon2 =  -3.0e-5;
        double alt2 =  -0.01;
        final DerivativeStructure latDS = new DerivativeStructure(1, 2, lat0, lat1, lat2);
        final DerivativeStructure lonDS = new DerivativeStructure(1, 2, lon0, lon1, lon2);
        final DerivativeStructure altDS = new DerivativeStructure(1, 2, alt0, alt1, alt2);

        // direct computation of position, velocity and acceleration
        PVCoordinates pv = earth.transform(new FieldGeodeticPoint<DerivativeStructure>(latDS, lonDS, altDS));

        // finite differences computation
        FiniteDifferencesDifferentiator differentiator =
                new FiniteDifferencesDifferentiator(5, 0.1);
        UnivariateDifferentiableFunction fx =
                differentiator.differentiate(new UnivariateFunction() {
                    public double value(double dt) {
                        GeodeticPoint gp =
                                new GeodeticPoint(latDS.taylor(dt), lonDS.taylor(dt), altDS.taylor(dt));
                        return earth.transform(gp).getX();
                    }
                });
        UnivariateDifferentiableFunction fy =
                differentiator.differentiate(new UnivariateFunction() {
                    public double value(double dt) {
                        GeodeticPoint gp =
                                new GeodeticPoint(latDS.taylor(dt), lonDS.taylor(dt), altDS.taylor(dt));
                        return earth.transform(gp).getY();
                    }
                });
        UnivariateDifferentiableFunction fz =
                differentiator.differentiate(new UnivariateFunction() {
                    public double value(double dt) {
                        GeodeticPoint gp =
                                new GeodeticPoint(latDS.taylor(dt), lonDS.taylor(dt), altDS.taylor(dt));
                        return earth.transform(gp).getZ();
                    }
                });
        DerivativeStructure dtZero = new DerivativeStructure(1, 2, 0, 0.0);
        DerivativeStructure xDS    = fx.value(dtZero);
        DerivativeStructure yDS    = fy.value(dtZero);
        DerivativeStructure zDS    = fz.value(dtZero);
        Assert.assertEquals(xDS.getValue(),              pv.getPosition().getX(),
                            2.0e-20 * FastMath.abs(xDS.getValue()));
        Assert.assertEquals(xDS.getPartialDerivative(1), pv.getVelocity().getX(),
                            2.0e-12 * FastMath.abs(xDS.getPartialDerivative(1)));
        Assert.assertEquals(xDS.getPartialDerivative(2), pv.getAcceleration().getX(),
                            2.0e-9  * FastMath.abs(xDS.getPartialDerivative(2)));
        Assert.assertEquals(yDS.getValue(),              pv.getPosition().getY(),
                            2.0e-20 * FastMath.abs(yDS.getValue()));
        Assert.assertEquals(yDS.getPartialDerivative(1), pv.getVelocity().getY(),
                            2.0e-12 * FastMath.abs(yDS.getPartialDerivative(1)));
        Assert.assertEquals(yDS.getPartialDerivative(2), pv.getAcceleration().getY(),
                            2.0e-9  * FastMath.abs(yDS.getPartialDerivative(2)));
        Assert.assertEquals(zDS.getValue(),              pv.getPosition().getZ(),
                            2.0e-20 * FastMath.abs(zDS.getValue()));
        Assert.assertEquals(zDS.getPartialDerivative(1), pv.getVelocity().getZ(),
                            2.0e-12 * FastMath.abs(zDS.getPartialDerivative(1)));
        Assert.assertEquals(zDS.getPartialDerivative(2), pv.getAcceleration().getZ(),
                            2.0e-9  * FastMath.abs(zDS.getPartialDerivative(2)));
    }

    private void checkCartesianToEllipsoidic(double ae, double f,
                                             double x, double y, double z,
                                             double longitude, double latitude,
                                             double altitude)
        throws OrekitException {

        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(ae, f, frame);
        GeodeticPoint gp = model.transform(new Vector3D(x, y, z), frame, date);
        Assert.assertEquals(longitude, MathUtils.normalizeAngle(gp.getLongitude(), longitude), 1.0e-10);
        Assert.assertEquals(latitude,  gp.getLatitude(),  1.0e-10);
        Assert.assertEquals(altitude,  gp.getAltitude(),  1.0e-10 * FastMath.abs(ae));
        Vector3D rebuiltNadir = Vector3D.crossProduct(gp.getSouth(), gp.getWest());
        Assert.assertEquals(0, rebuiltNadir.subtract(gp.getNadir()).getNorm(), 1.0e-15);
    }

    @Test
    public void testTransformVsOldIterativeSobol()
        throws OrekitException {

        OneAxisEllipsoid model = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        SobolSequenceGenerator sobol = new SobolSequenceGenerator(3);
        final double rMax = 10 * model.getEquatorialRadius();
        Stream<Vector3D> points = Stream.generate(() -> {
            final double[] v = sobol.nextVector();
            return new Vector3D(rMax * (2 * v[0] - 1), rMax * (2 * v[1] - 1), rMax * (2 * v[2] - 1));
        }).limit(1000000);

        doTestTransformVsOldIterative(model, points, 2.0e-15, 1.0e-15, 8.0e-14 * rMax);
    }

    @Test
    public void testTransformVsOldIterativePolarAxis()
        throws OrekitException {
        OneAxisEllipsoid model = new OneAxisEllipsoid(90, 5.0 / 9.0,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Stream<Vector3D> points = DoubleStream.iterate(0, x -> x + 1.0).limit(150).mapToObj(z -> new Vector3D(0, 0, z));
        doTestTransformVsOldIterative(model, points, 2.0e-15, 1.0e-15, 1.0e-14 * model.getEquatorialRadius());
    }

    @Test
    public void testTransformVsOldIterativeEquatorial()
        throws OrekitException {
        OneAxisEllipsoid model = new OneAxisEllipsoid(90, 5.0 / 9.0,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Stream<Vector3D> points = DoubleStream.iterate(0, x -> x + 1.0).limit(150).mapToObj(x -> new Vector3D(x, 0, 0));
        doTestTransformVsOldIterative(model, points, 2.0e-15, 1.0e-15, 1.0e-14 * model.getEquatorialRadius());
    }

    private void doTestTransformVsOldIterative(OneAxisEllipsoid model,
                                               Stream<Vector3D> points,
                                               double latitudeTolerance, double longitudeTolerance,
                                               double altitudeTolerance)
        throws OrekitException {
        points.forEach(point -> {
            try {
                GeodeticPoint reference = transformOldIterative(model, point, model.getBodyFrame(), null);
                GeodeticPoint result    = model.transform(point, model.getBodyFrame(), null);
                Assert.assertEquals(reference.getLatitude(),  result.getLatitude(),  latitudeTolerance);
                Assert.assertEquals(reference.getLongitude(), result.getLongitude(), longitudeTolerance);
                Assert.assertEquals(reference.getAltitude(),  result.getAltitude(),  altitudeTolerance);
            } catch (OrekitException oe) {
                Assert.fail(oe.getLocalizedMessage());
            }
        });

    }

    /** Transform a cartesian point to a surface-relative point.
     * <p>
     * This method was the implementation used in the main Orekit library
     * as of version 8.0. It has been replaced as of 9.0 with a new version
     * using faster iterations, so it is now used only as a test reference
     * with an implementation which is different from the one in the main library.
     * </p>
     * @param point Cartesian point
     * @param frame frame in which Cartesian point is expressed
     * @param date date of the computation (used for frames conversions)
     * @return point at the same location but as a surface-relative point
     * @exception OrekitException if point cannot be converted to body frame
     */
    private GeodeticPoint transformOldIterative(final OneAxisEllipsoid model,
                                                final Vector3D point,
                                                final Frame frame,
                                                final AbsoluteDate date)
        throws OrekitException {

        // transform point to body frame
        final Vector3D pointInBodyFrame = frame.getTransformTo(model.getBodyFrame(), date).transformPosition(point);
        final double   r2               = pointInBodyFrame.getX() * pointInBodyFrame.getX() +
                                          pointInBodyFrame.getY() * pointInBodyFrame.getY();
        final double   r                = FastMath.sqrt(r2);
        final double   z                = pointInBodyFrame.getZ();

        // set up the 2D meridian ellipse
        final Ellipse meridian = new Ellipse(Vector3D.ZERO,
                                             new Vector3D(pointInBodyFrame.getX() / r, pointInBodyFrame.getY() / r, 0),
                                             Vector3D.PLUS_K,
                                             model.getA(), model.getC(), model.getBodyFrame());

        // project point on the 2D meridian ellipse
        final Vector2D ellipsePoint = meridian.projectToEllipse(new Vector2D(r, z));

        // relative position of test point with respect to its ellipse sub-point
        final double dr  = r - ellipsePoint.getX();
        final double dz  = z - ellipsePoint.getY();
        final double ae2 = model.getA() * model.getA();
        final double f   = model.getFlattening();
        final double g   = 1.0 - f;
        final double g2  = g * g;
        final double insideIfNegative = g2 * (r2 - ae2) + z * z;

        return new GeodeticPoint(FastMath.atan2(ellipsePoint.getY(), g2 * ellipsePoint.getX()),
                                 FastMath.atan2(pointInBodyFrame.getY(), pointInBodyFrame.getX()),
                                 FastMath.copySign(FastMath.hypot(dr, dz), insideIfNegative));

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

