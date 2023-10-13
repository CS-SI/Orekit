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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.geometry.euclidean.oned.Vector1D;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.random.SobolSequenceGenerator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinatesHermiteInterpolator;


public class OneAxisEllipsoidTest {

    @Test
    public void testStandard() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    4637885.347, 121344.608, 4362452.869,
                                    0.026157811533131, 0.757987116290729, 260.455572965555);
    }

    @Test
    public void testLongitudeZero() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    6378400.0, 0, 6379000.0,
                                    0.0, 0.787815771252351, 2653416.77864152);
    }

    @Test
    public void testLongitudePi() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    -6379999.0, 0, 6379000.0,
                                    3.14159265358979, 0.787690146758403, 2654544.7767725);
    }

    @Test
    public void testNorthPole() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    0.0, 0.0, 7000000.0,
                                    0.0, 1.57079632679490, 643247.685859644);
    }

    @Test
    public void testEquator() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257222101,
                                    6379888.0, 6377000.0, 0.0,
                                    0.785171775899913, 0.0, 2642345.24279301);
    }

    @Test
    public void testNoFlattening() {
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
    public void testNoFlatteningPolar() {
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
    public void testOnSurface() {
        Vector3D surfacePoint = new Vector3D(-1092200.775949484,
                                             -3944945.7282234835,
                                              4874931.946956173);
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101,
                                                           FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        GeodeticPoint gp = earthShape.transform(surfacePoint, earthShape.getBodyFrame(),
                                                   AbsoluteDate.J2000_EPOCH);
        Vector3D rebuilt = earthShape.transform(gp);
        Assertions.assertEquals(0, rebuilt.distance(surfacePoint), 3.0e-9);
    }

    @Test
    public void testInside3Roots() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
                                    9219.0, -5322.0, 6056743.0,
                                    5.75963470503781, 1.56905114598949, -300000.009586231);
    }

    @Test
    public void testInsideLessThan3Roots() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
                                    1366863.0, -789159.0, -5848.988,
                                    -0.523598928689, -0.00380885831963, -4799808.27951);
    }

    @Test
    public void testOutside() {
        checkCartesianToEllipsoidic(6378137.0, 1.0 / 298.257,
                                    5722966.0, -3304156.0, -24621187.0,
                                    5.75958652642615, -1.3089969725151, 19134410.3342696);
    }

    @Test
    public void testGeoCar() {
        OneAxisEllipsoid model =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101,
                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        GeodeticPoint nsp =
            new GeodeticPoint(0.852479154923577, 0.0423149994747243, 111.6);
        Vector3D p = model.transform(nsp);
        Assertions.assertEquals(4201866.69291890, p.getX(), 1.0e-6);
        Assertions.assertEquals(177908.184625686, p.getY(), 1.0e-6);
        Assertions.assertEquals(4779203.64408617, p.getZ(), 1.0e-6);
    }

    @Test
    public void testGroundProjectionPosition() {
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

            AbsoluteDate date = orbit.getDate().shiftedBy(dt);
            Vector3D pos = orbit.getPosition(date, eme2000);
            Vector3D groundPV = model.projectToGround(pos, date, eme2000);
            Vector3D groundP = model.projectToGround(pos, date, eme2000);

            // check methods projectToGround and transform are consistent with each other
            Assertions.assertEquals(model.transform(pos, eme2000, date).getLatitude(),
                                model.transform(groundPV, eme2000, date).getLatitude(),
                                1.0e-10);
            Assertions.assertEquals(model.transform(pos, eme2000, date).getLongitude(),
                                model.transform(groundPV, eme2000, date).getLongitude(),
                                1.0e-10);
            Assertions.assertEquals(0.0, Vector3D.distance(groundP, groundPV), 1.0e-15 * groundP.getNorm());

        }

    }

    @Test
    public void testGroundProjectionDerivatives() {
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
        Assertions.assertEquals(0, errors[0], 1.0e-16);
        Assertions.assertEquals(0, errors[1], 2.0e-12);
        Assertions.assertEquals(0, errors[2], 2.0e-4);

    }

    @Test
    public void testGroundToGroundIssue181() {
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
        Assertions.assertEquals(0.0, Vector3D.distance(ground1.getPosition(),     ground2.getPosition()),     1.0e-12);
        Assertions.assertEquals(0.0, Vector3D.distance(ground1.getVelocity(),     ground2.getVelocity()),     2.0e-12);
        Assertions.assertEquals(0.0, Vector3D.distance(ground1.getAcceleration(), ground2.getAcceleration()), 1.0e-12);

    }

    private double[] derivativesErrors(PVCoordinatesProvider provider, AbsoluteDate date, Frame frame,
                                       OneAxisEllipsoid model) {
        List<TimeStampedPVCoordinates> pvList       = new ArrayList<TimeStampedPVCoordinates>();
        List<TimeStampedPVCoordinates> groundPVList = new ArrayList<TimeStampedPVCoordinates>();
        for (double dt = -0.25; dt <= 0.25; dt += 0.125) {
            TimeStampedPVCoordinates shiftedPV = provider.getPVCoordinates(date.shiftedBy(dt), frame);
            Vector3D p = model.projectToGround(shiftedPV.getPosition(), shiftedPV.getDate(), frame);
            pvList.add(shiftedPV);
            groundPVList.add(new TimeStampedPVCoordinates(shiftedPV.getDate(),
                                                          p, Vector3D.ZERO, Vector3D.ZERO));
        }

        // create interpolators
        final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                new TimeStampedPVCoordinatesHermiteInterpolator(pvList.size(), CartesianDerivativesFilter.USE_P);

        final TimeInterpolator<TimeStampedPVCoordinates> interpolatorGround =
                new TimeStampedPVCoordinatesHermiteInterpolator(groundPVList.size(), CartesianDerivativesFilter.USE_P);

        TimeStampedPVCoordinates computed = model.projectToGround(interpolator.interpolate(date, pvList), frame);
        TimeStampedPVCoordinates reference = interpolatorGround.interpolate(date, groundPVList);

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
    public void testGroundProjectionTaylor() {
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
            Vector3D taylorP = groundTaylor.getPosition(date, model.getBodyFrame());
            Vector3D refP    = model.projectToGround(orbit.getPosition(date, model.getBodyFrame()),
                                                     date, model.getBodyFrame());
            Vector3D delta = taylorP.subtract(refP);
            Assertions.assertEquals(0.0, Vector3D.dotProduct(delta, alongTrack),  0.0015);
            Assertions.assertEquals(0.0, Vector3D.dotProduct(delta, acrossTrack), 0.0007);
            Assertions.assertEquals(0.0, Vector3D.dotProduct(delta, zenith),      0.00002);
        }

    }

    @Test
    public void testLineIntersection() {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9, frame);
        Vector3D point         = new Vector3D(0.0, 93.7139699, 3.5930796);
        Vector3D direction     = new Vector3D(0.0, 1.0, 1.0);
        Line line = new Line(point, point.add(direction), 1.0e-10);
        GeodeticPoint gp = model.getIntersectionPoint(line, point, frame, date);
        Assertions.assertEquals(gp.getAltitude(), 0.0, 1.0e-12);
        Assertions.assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9, frame);
        point = new Vector3D(0.0, -93.7139699, -3.5930796);
        direction = new Vector3D(0.0, -1.0, -1.0);
        line = new Line(point, point.add(direction), 1.0e-10).revert();
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assertions.assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9, frame);
        point = new Vector3D(0.0, -93.7139699, 3.5930796);
        direction = new Vector3D(0.0, -1.0, 1.0);
        line = new Line(point, point.add(direction), 1.0e-10);
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assertions.assertTrue(line.contains(model.transform(gp)));

        model = new OneAxisEllipsoid(100.0, 0.9, frame);
        point = new Vector3D(-93.7139699, 0.0, 3.5930796);
        direction = new Vector3D(-1.0, 0.0, 1.0);
        line = new Line(point, point.add(direction), 1.0e-10);
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assertions.assertTrue(line.contains(model.transform(gp)));
        Assertions.assertFalse(line.contains(new Vector3D(0, 0, 7000000)));

        point = new Vector3D(0.0, 0.0, 110);
        direction = new Vector3D(0.0, 0.0, 1.0);
        line = new Line(point, point.add(direction), 1.0e-10);
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assertions.assertEquals(gp.getLatitude(), FastMath.PI/2, 1.0e-12);

        point = new Vector3D(0.0, 110, 0);
        direction = new Vector3D(0.0, 1.0, 0.0);
        line = new Line(point, point.add(direction), 1.0e-10);
        gp = model.getIntersectionPoint(line, point, frame, date);
        Assertions.assertEquals(gp.getLatitude(), 0, 1.0e-12);

    }

    @Test
    public void testNoLineIntersection() {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 0.9, frame);
        Vector3D point     = new Vector3D(0.0, 93.7139699, 3.5930796);
        Vector3D direction = new Vector3D(0.0, 9.0, -2.0);
        Line line = new Line(point, point.add(direction), 1.0e-10);
        Assertions.assertNull(model.getIntersectionPoint(line, point, frame, date));
    }

    @Test
    public void testNegativeZ() {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(90.0, 5.0 / 9.0, frame);
        Vector3D point     = new Vector3D(140.0, 0.0, -30.0);
        GeodeticPoint gp = model.transform(point, frame, date);
        Vector3D rebuilt = model.transform(gp);
        Assertions.assertEquals(0.0, rebuilt.distance(point), 1.0e-10);
    }

    @Test
    public void testNumerousIteration() {
        // this test, which corresponds to an unrealistic extremely flat ellipsoid,
        // is designed to need more than the usual 2 or 3 iterations in the iterative
        // version of the Toshio Fukushima's algorithm. It reaches convergence at
        // iteration 17
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(100.0, 999.0 / 1000.0, frame);
        Vector3D point     = new Vector3D(100.001, 0.0, 1.0);
        GeodeticPoint gp = model.transform(point, frame, date);
        Vector3D rebuilt = model.transform(gp);
        Assertions.assertEquals(0.0, rebuilt.distance(point), 1.2e-11);
    }

    @Test
    public void testEquatorialInside() {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(90.0, 5.0 / 9.0, frame);
        for (double rho = 0; rho < model.getEquatorialRadius(); rho += 0.01) {
            Vector3D point     = new Vector3D(rho, 0.0, 0.0);
            GeodeticPoint gp = model.transform(point, frame, date);
            Vector3D rebuilt = model.transform(gp);
            Assertions.assertEquals(0.0, rebuilt.distance(point), 1.0e-10);
        }
    }

    @Test
    public void testFarPoint() {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(90.0, 5.0 / 9.0, frame);
        Vector3D point     = new Vector3D(1.0e15, 2.0e15, -1.0e12);
        GeodeticPoint gp = model.transform(point, frame, date);
        Vector3D rebuilt = model.transform(gp);
        Assertions.assertEquals(0.0, rebuilt.distance(point), 1.0e-15 * point.getNorm());
    }

    @Test
    public void testIssue141() {
        AbsoluteDate date = new AbsoluteDate("2002-03-06T20:50:20.44188731559965033", TimeScalesFactory.getUTC());
        Frame frame = FramesFactory.getGTOD(IERSConventions.IERS_1996, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      frame);
        Vector3D point     = new Vector3D(-6838696.282102453, -2148321.403361013, -0.011907944179711194);
        GeodeticPoint gp = model.transform(point, frame, date);
        Vector3D rebuilt = model.transform(gp);
        Assertions.assertEquals(0.0, rebuilt.distance(point), 1.0e-15 * point.getNorm());
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        OneAxisEllipsoid original = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                         Constants.WGS84_EARTH_FLATTENING,
                                                         FramesFactory.getITRFEquinox(IERSConventions.IERS_1996, true));
        original.setAngularThreshold(1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        Assertions.assertTrue(bos.size() > 250);
        Assertions.assertTrue(bos.size() < 350);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        OneAxisEllipsoid deserialized  = (OneAxisEllipsoid) ois.readObject();
        Assertions.assertEquals(original.getEquatorialRadius(), deserialized.getEquatorialRadius(), 1.0e-12);
        Assertions.assertEquals(original.getFlattening(), deserialized.getFlattening(), 1.0e-12);

    }

    @Test
    public void testIntersectionFromPoints() {
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
                                   FastMath.toRadians(90.), PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in EME2000 and ITRF200B
        PVCoordinates pvSatEME2000 = circ.getPVCoordinates();
        Vector3D pSatItrf  = frame.getStaticTransformTo(FramesFactory.getEME2000(), date).transformPosition(pvSatEME2000.getPosition());

        // Test first visible surface points
        GeodeticPoint geoPoint = new GeodeticPoint(FastMath.toRadians(70.), FastMath.toRadians(60.), 0.);
        Vector3D pointItrf     = earth.transform(geoPoint);
        Line line = new Line(pSatItrf, pointItrf, 1.0e-10);
        GeodeticPoint geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assertions.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assertions.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test second visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(65.), FastMath.toRadians(-120.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assertions.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assertions.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test non visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(30.), FastMath.toRadians(60.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);

        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);

        // For polar satellite position, intersection point is at the same longitude but different latitude
        Assertions.assertEquals(1.04437199, geoInter.getLongitude(), Utils.epsilonAngle);
        Assertions.assertEquals(1.36198012, geoInter.getLatitude(),  Utils.epsilonAngle);

        // Satellite on equatorial position
        // ********************************
        circ =
            new CircularOrbit(7178000.0, 0.5e-4, 0., FastMath.toRadians(1.e-4), FastMath.toRadians(0.),
                                   FastMath.toRadians(0.), PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in EME2000 and ITRF200B
        pvSatEME2000 = circ.getPVCoordinates();
        pSatItrf  = frame.getStaticTransformTo(FramesFactory.getEME2000(), date).transformPosition(pvSatEME2000.getPosition());

        // Test first visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(5.), FastMath.toRadians(0.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        Assertions.assertTrue(line.toSubSpace(pSatItrf).getX() < 0);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assertions.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assertions.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // With the point opposite to satellite point along the line
        GeodeticPoint geoInter2 = earth.getIntersectionPoint(line, line.toSpace(new Vector1D(-line.toSubSpace(pSatItrf).getX())), frame, date);
        Assertions.assertTrue(FastMath.abs(geoInter.getLongitude() - geoInter2.getLongitude()) > FastMath.toRadians(0.1));
        Assertions.assertTrue(FastMath.abs(geoInter.getLatitude() - geoInter2.getLatitude()) > FastMath.toRadians(0.1));

        // Test second visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(-5.), FastMath.toRadians(0.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assertions.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assertions.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test non visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(40.), FastMath.toRadians(0.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assertions.assertEquals(-0.00768481, geoInter.getLongitude(), Utils.epsilonAngle);
        Assertions.assertEquals( 0.32180410, geoInter.getLatitude(),  Utils.epsilonAngle);


        // Satellite on any position
        // *************************
        circ =
            new CircularOrbit(7178000.0, 0.5e-4, 0., FastMath.toRadians(50.), FastMath.toRadians(0.),
                                   FastMath.toRadians(90.), PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in EME2000 and ITRF200B
        pvSatEME2000 = circ.getPVCoordinates();
        pSatItrf  = frame.getStaticTransformTo(FramesFactory.getEME2000(), date).transformPosition(pvSatEME2000.getPosition());

        // Test first visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(40.), FastMath.toRadians(90.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assertions.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assertions.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test second visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(60.), FastMath.toRadians(90.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assertions.assertEquals(geoPoint.getLongitude(), geoInter.getLongitude(), Utils.epsilonAngle);
        Assertions.assertEquals(geoPoint.getLatitude(), geoInter.getLatitude(), Utils.epsilonAngle);

        // Test non visible surface points
        geoPoint = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(90.), 0.);
        pointItrf     = earth.transform(geoPoint);
        line = new Line(pSatItrf, pointItrf, 1.0e-10);
        geoInter = earth.getIntersectionPoint(line, pSatItrf, frame, date);
        Assertions.assertEquals(FastMath.toRadians(89.5364061088196), geoInter.getLongitude(), Utils.epsilonAngle);
        Assertions.assertEquals(FastMath.toRadians(35.555543683351125), geoInter.getLatitude(), Utils.epsilonAngle);

    }

    @Test
    public void testMovingGeodeticPointSymmetry() {

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
        final DSFactory factory = new DSFactory(1, 2);
        final DerivativeStructure latDS = factory.build(lat0, lat1, lat2);
        final DerivativeStructure lonDS = factory.build(lon0, lon1, lon2);
        final DerivativeStructure altDS = factory.build(alt0, alt1, alt2);

        // direct computation of position, velocity and acceleration
        PVCoordinates pv = new PVCoordinates(earth.transform(new FieldGeodeticPoint<>(latDS, lonDS, altDS)));
        FieldGeodeticPoint<DerivativeStructure> rebuilt = earth.transform(pv, earth.getBodyFrame(), null);
        Assertions.assertEquals(lat0, rebuilt.getLatitude().getReal(),                1.0e-16);
        Assertions.assertEquals(lat1, rebuilt.getLatitude().getPartialDerivative(1),  5.0e-19);
        Assertions.assertEquals(lat2, rebuilt.getLatitude().getPartialDerivative(2),  5.0e-14);
        Assertions.assertEquals(lon0, rebuilt.getLongitude().getReal(),               1.0e-16);
        Assertions.assertEquals(lon1, rebuilt.getLongitude().getPartialDerivative(1), 5.0e-19);
        Assertions.assertEquals(lon2, rebuilt.getLongitude().getPartialDerivative(2), 1.0e-20);
        Assertions.assertEquals(alt0, rebuilt.getAltitude().getReal(),                2.0e-11);
        Assertions.assertEquals(alt1, rebuilt.getAltitude().getPartialDerivative(1),  6.0e-13);
        Assertions.assertEquals(alt2, rebuilt.getAltitude().getPartialDerivative(2),  2.0e-14);

    }

    @Test
    public void testMovingGeodeticPoint() {

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
        final DSFactory factory = new DSFactory(1, 2);
        final DerivativeStructure latDS = factory.build(lat0, lat1, lat2);
        final DerivativeStructure lonDS = factory.build(lon0, lon1, lon2);
        final DerivativeStructure altDS = factory.build(alt0, alt1, alt2);

        // direct computation of position, velocity and acceleration
        PVCoordinates pv = new PVCoordinates(earth.transform(new FieldGeodeticPoint<>(latDS, lonDS, altDS)));

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
        DerivativeStructure dtZero = factory.variable(0, 0.0);
        DerivativeStructure xDS    = fx.value(dtZero);
        DerivativeStructure yDS    = fy.value(dtZero);
        DerivativeStructure zDS    = fz.value(dtZero);
        Assertions.assertEquals(xDS.getValue(),              pv.getPosition().getX(),
                            2.0e-20 * FastMath.abs(xDS.getValue()));
        Assertions.assertEquals(xDS.getPartialDerivative(1), pv.getVelocity().getX(),
                            2.0e-12 * FastMath.abs(xDS.getPartialDerivative(1)));
        Assertions.assertEquals(xDS.getPartialDerivative(2), pv.getAcceleration().getX(),
                            2.0e-9  * FastMath.abs(xDS.getPartialDerivative(2)));
        Assertions.assertEquals(yDS.getValue(),              pv.getPosition().getY(),
                            2.0e-20 * FastMath.abs(yDS.getValue()));
        Assertions.assertEquals(yDS.getPartialDerivative(1), pv.getVelocity().getY(),
                            2.0e-12 * FastMath.abs(yDS.getPartialDerivative(1)));
        Assertions.assertEquals(yDS.getPartialDerivative(2), pv.getAcceleration().getY(),
                            2.0e-9  * FastMath.abs(yDS.getPartialDerivative(2)));
        Assertions.assertEquals(zDS.getValue(),              pv.getPosition().getZ(),
                            2.0e-20 * FastMath.abs(zDS.getValue()));
        Assertions.assertEquals(zDS.getPartialDerivative(1), pv.getVelocity().getZ(),
                            2.0e-12 * FastMath.abs(zDS.getPartialDerivative(1)));
        Assertions.assertEquals(zDS.getPartialDerivative(2), pv.getAcceleration().getZ(),
                            2.0e-9  * FastMath.abs(zDS.getPartialDerivative(2)));
    }

    private void checkCartesianToEllipsoidic(double ae, double f,
                                             double x, double y, double z,
                                             double longitude, double latitude,
                                             double altitude) {

        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid model = new OneAxisEllipsoid(ae, f, frame);
        GeodeticPoint gp = model.transform(new Vector3D(x, y, z), frame, date);
        Assertions.assertEquals(longitude, MathUtils.normalizeAngle(gp.getLongitude(), longitude), 1.0e-10);
        Assertions.assertEquals(latitude,  gp.getLatitude(),  1.0e-10);
        Assertions.assertEquals(altitude,  gp.getAltitude(),  1.0e-10 * FastMath.abs(ae));
        Vector3D rebuiltNadir = Vector3D.crossProduct(gp.getSouth(), gp.getWest());
        Assertions.assertEquals(0, rebuiltNadir.subtract(gp.getNadir()).getNorm(), 1.0e-15);

        FieldGeodeticPoint<Binary64> gp64 = model.transform(new FieldVector3D<Binary64>(new Binary64(x),
                                                                                          new Binary64(y),
                                                                                          new Binary64(z)),
                                                             frame,
                                                             new FieldAbsoluteDate<>(Binary64Field.getInstance(), date));
        Assertions.assertEquals(longitude, MathUtils.normalizeAngle(gp64.getLongitude().getReal(), longitude), 1.0e-10);
        Assertions.assertEquals(latitude,  gp64.getLatitude().getReal(),  1.0e-10);
        Assertions.assertEquals(altitude,  gp64.getAltitude().getReal(),  1.0e-10 * FastMath.abs(ae));
        FieldVector3D<Binary64> rebuiltNadir64 = FieldVector3D.crossProduct(gp64.getSouth(), gp64.getWest());
        Assertions.assertEquals(0, rebuiltNadir64.subtract(gp64.getNadir()).getNorm().getReal(), 1.0e-15);

        // project to ground
        gp = model.transform(model.projectToGround(new Vector3D(x, y, z), date, frame), frame, date);
        Assertions.assertEquals(longitude, MathUtils.normalizeAngle(gp.getLongitude(), longitude), 1.0e-10);
        Assertions.assertEquals(latitude,  gp.getLatitude(),  1.0e-10);
        Assertions.assertEquals(0.0,  gp.getAltitude(),  1.0e-10 * FastMath.abs(ae));

        // project pv to ground
        FieldGeodeticPoint<DerivativeStructure> gpDs = model.transform(
                model.projectToGround(
                        new TimeStampedPVCoordinates(
                                date,
                                new Vector3D(x, y, z),
                                new Vector3D(1, 2, 3)),
                        frame),
                frame,
                date);
        Assertions.assertEquals(longitude, MathUtils.normalizeAngle(gpDs.getLongitude().getReal(), longitude), 1.0e-10);
        Assertions.assertEquals(latitude,  gpDs.getLatitude().getReal(),  1.0e-10);
        Assertions.assertEquals(0.0,  gpDs.getAltitude().getReal(),  1.0e-10 * FastMath.abs(ae));

        }

    @Test
    public void testTransformVsOldIterativeSobol() {

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
    public void testTransformVsOldIterativePolarAxis() {
        OneAxisEllipsoid model = new OneAxisEllipsoid(90, 5.0 / 9.0,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Stream<Vector3D> points = DoubleStream.iterate(0, x -> x + 1.0).limit(150).mapToObj(z -> new Vector3D(0, 0, z));
        doTestTransformVsOldIterative(model, points, 2.0e-15, 1.0e-15, 1.0e-14 * model.getEquatorialRadius());
    }

    @Test
    public void testTransformVsOldIterativeEquatorial() {
        OneAxisEllipsoid model = new OneAxisEllipsoid(90, 5.0 / 9.0,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Stream<Vector3D> points = DoubleStream.iterate(0, x -> x + 1.0).limit(150).mapToObj(x -> new Vector3D(x, 0, 0));
        doTestTransformVsOldIterative(model, points, 2.0e-15, 1.0e-15, 1.0e-14 * model.getEquatorialRadius());
    }

    @Test
    public void testIssue373() {
        final Frame            ecef   = FramesFactory.getITRF(IERSConventions.IERS_2010,true);
        final OneAxisEllipsoid earth  = new OneAxisEllipsoid(6378137, 1./298.257223563, ecef);
        final Vector3D         sunPos = new Vector3D(-149757851422.23358, 8410610314.781021, 14717269835.161688 );
        final GeodeticPoint    sunGP  = earth.transform(sunPos, ecef, null);
        Assertions.assertEquals(5.603878, FastMath.toDegrees(sunGP.getLatitude()), 1.0e-6);
    }

    @Test
    public void testIsometricLatitude() {
        final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth  = new OneAxisEllipsoid(6378137, 1. / 298.257223563, ecef);

        Assertions.assertEquals(0., earth.geodeticToIsometricLatitude(0), 1.0e-9);
        Assertions.assertEquals(0., earth.geodeticToIsometricLatitude(2.0e-13), 1.0e-9);

        for (final double lat: new double[] {
                FastMath.toRadians(10),
                FastMath.toRadians(-45),
                FastMath.toRadians(80),
                FastMath.toRadians(-90)}) {
            final double eSinLat = earth.getEccentricity() * FastMath.sin(lat);
            final double term1 = FastMath.log(FastMath.tan(FastMath.PI / 4. + lat / 2.));
            final double term2 = (earth.getEccentricity() / 2.) * FastMath.log((1. - eSinLat) / (1 + eSinLat));

            Assertions.assertEquals(term1 + term2, earth.geodeticToIsometricLatitude(lat), 1.0e-12);
        }
    }

    @Test
    public void testFieldIsometricLatitude() {
        final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth  = new OneAxisEllipsoid(6378137, 1. / 298.257223563, ecef);

        final Binary64Field field = Binary64Field.getInstance();
        Assertions.assertEquals(Binary64Field.getInstance().getZero(), earth.geodeticToIsometricLatitude(field.getOne().newInstance(0.)));
        Assertions.assertEquals(Binary64Field.getInstance().getZero(), earth.geodeticToIsometricLatitude(field.getOne().newInstance(2.0e-13)));

        final Binary64 ecc = field.getZero().newInstance(earth.getEccentricity());
        for (final Binary64 lat: new Binary64[] {
                    field.getOne().newInstance(FastMath.toRadians(10)),
                    field.getOne().newInstance(FastMath.toRadians(-45)),
                    field.getOne().newInstance(FastMath.toRadians(80)),
                    field.getOne().newInstance(FastMath.toRadians(-90))}) {
            final Binary64 eSinLat = ecc.multiply(FastMath.sin(lat));
            final Binary64 term1 = FastMath.log(FastMath.tan(lat.getPi().divide(4.).add(lat.divide(2.))));
            final Binary64 term2 = ecc.divide(2.).multiply(FastMath.log(field.getOne().subtract(eSinLat).divide(field.getOne().add(eSinLat))));

            Assertions.assertEquals(term1.add(term2), earth.geodeticToIsometricLatitude(lat));
        }
    }

    @Test
    public void testAzimuthBetweenPoints() {
        final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth  = new OneAxisEllipsoid(6378137, 1. / 298.257223563, ecef);

        // values from https://distance.to
        final GeodeticPoint newYork = new GeodeticPoint(FastMath.toRadians(40.71427), FastMath.toRadians(-74.00597), 0);
        final GeodeticPoint chicago = new GeodeticPoint(FastMath.toRadians(41.85003), FastMath.toRadians(-87.65005), 0);
        final GeodeticPoint london = new GeodeticPoint(FastMath.toRadians(51.5), FastMath.toRadians(-0.16667), 0);
        final GeodeticPoint berlin = new GeodeticPoint(FastMath.toRadians(52.523403), FastMath.toRadians(13.4114), 0);
        final GeodeticPoint perth = new GeodeticPoint(FastMath.toRadians(-31.952712), FastMath.toRadians(115.8604796), 0);

        // answers verified against RhumbSolve lib https://geographiclib.sourceforge.io/cgi-bin/RhumbSolve
        Assertions.assertEquals(276.297499, FastMath.toDegrees(earth.azimuthBetweenPoints(newYork, chicago)), 1.0e-6);
        Assertions.assertEquals(78.0854898, FastMath.toDegrees(earth.azimuthBetweenPoints(newYork, london)), 1.0e-6);
        Assertions.assertEquals(83.0357553, FastMath.toDegrees(earth.azimuthBetweenPoints(london, berlin)), 1.0e-6);
        Assertions.assertEquals(132.894864, FastMath.toDegrees(earth.azimuthBetweenPoints(berlin, perth)), 1.0e-6);
        Assertions.assertEquals(65.3853195, FastMath.toDegrees(earth.azimuthBetweenPoints(perth, newYork)), 1.0e-6);
    }

    @Test
    public void testAzimuthBetweenFieldPoints() {
        final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth  = new OneAxisEllipsoid(6378137, 1. / 298.257223563, ecef);

        final Binary64Field field = Binary64Field.getInstance();

        // values from https://distance.to
        final FieldGeodeticPoint<Binary64> newYork = new FieldGeodeticPoint<>(
                FastMath.toRadians(field.getZero().add(40.71427)),
                FastMath.toRadians(field.getZero().add(-74.00597)), field.getZero());
        final FieldGeodeticPoint<Binary64> chicago = new FieldGeodeticPoint<>(
                FastMath.toRadians(field.getZero().add(41.85003)),
                FastMath.toRadians(field.getZero().add(-87.65005)), field.getZero());

        final FieldGeodeticPoint<Binary64> london = new FieldGeodeticPoint<>(
            FastMath.toRadians(field.getZero().add(51.5)),
            FastMath.toRadians(field.getZero().add(-0.16667)), field.getZero());
        final FieldGeodeticPoint<Binary64> berlin = new FieldGeodeticPoint<>(
            FastMath.toRadians(field.getZero().add(52.523403)),
            FastMath.toRadians(field.getZero().add(13.4114)), field.getZero());
        final FieldGeodeticPoint<Binary64> perth = new FieldGeodeticPoint<>(
            FastMath.toRadians(field.getZero().add(-31.952712)),
            FastMath.toRadians(field.getZero().add(115.8604796)), field.getZero());

        // answers verified against RhumbSolve lib https://geographiclib.sourceforge.io/cgi-bin/RhumbSolve
        Assertions.assertEquals(276.297499, FastMath.toDegrees(earth.azimuthBetweenPoints(newYork, chicago)).getReal(), 1.0e-6);
        Assertions.assertEquals(78.0854898, FastMath.toDegrees(earth.azimuthBetweenPoints(newYork, london)).getReal(), 1.0e-6);
        Assertions.assertEquals(83.0357553, FastMath.toDegrees(earth.azimuthBetweenPoints(london, berlin)).getReal(), 1.0e-6);
        Assertions.assertEquals(132.894864, FastMath.toDegrees(earth.azimuthBetweenPoints(berlin, perth)).getReal(), 1.0e-6);
        Assertions.assertEquals(65.3853195, FastMath.toDegrees(earth.azimuthBetweenPoints(perth, newYork)).getReal(), 1.0e-6);
    }

    @Test
    public void testPointNearCenter1() {
        final OneAxisEllipsoid earth  = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                             Constants.WGS84_EARTH_FLATTENING,
                                                             FramesFactory.getITRF(IERSConventions.IERS_2010, false));
        final Vector3D p1   = new Vector3D( 14605530.402633,  7681001.886684,  24582223.005261);
        final Vector3D p2   = new Vector3D(-14650836.411867, -7561887.405778, -24575352.170908);
        final Vector3D pMid = new Vector3D(0.5, p1, 0.5, p2);
        final GeodeticPoint gp = earth.transform(pMid, earth.getFrame(), null);
        Vector3D rebuilt = earth.transform(gp);
        Assertions.assertEquals(0.0, rebuilt.distance(pMid), 1.5e-9);
    }

    @Test
    public void testPointNearCenter2() {
        final OneAxisEllipsoid earth  = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                             Constants.WGS84_EARTH_FLATTENING,
                                                             FramesFactory.getITRF(IERSConventions.IERS_2010, false));
        final Vector3D pMid = new Vector3D(-20923.23737959098, 56464.586571323685, -7647.317096056417);
        final GeodeticPoint gp = earth.transform(pMid, earth.getFrame(), null);
        Vector3D rebuilt = earth.transform(gp);
        // we exited loop without convergence
        Assertions.assertEquals(540.598, rebuilt.distance(pMid), 1.0e-3);
    }


    @Test
    public void testLowestAltitudeIntermediate() {
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, false));
        final Vector3D close = earth.transform(new GeodeticPoint(FastMath.toRadians(12.0),
                                                                 FastMath.toRadians(34.0),
                                                                 100000.0));
        final Vector3D far   = earth.transform(new GeodeticPoint(FastMath.toRadians(-47.0),
                                                                 FastMath.toRadians( 43.0),
                                                                 10000000.0));
        final GeodeticPoint lowestA = earth.lowestAltitudeIntermediate(close, far);
        Assertions.assertEquals(0.0, Vector3D.distance(close, earth.transform(lowestA)), 2.0e-9);
        final GeodeticPoint lowestB = earth.lowestAltitudeIntermediate(far, close);
        Assertions.assertEquals(0.0, Vector3D.distance(close, earth.transform(lowestB)), 2.0e-9);

        final double h = 5000000.0;
        final Vector3D p1 = earth.transform(new GeodeticPoint(FastMath.toRadians(-20.0),
                                                              FastMath.toRadians(-12.0),
                                                              h));
        final Vector3D p2 = earth.transform(new GeodeticPoint(FastMath.toRadians(17.0),
                                                              FastMath.toRadians(13.0),
                                                              h));
        final GeodeticPoint lowest = earth.lowestAltitudeIntermediate(p1, p2);
        Assertions.assertTrue(lowest.getAltitude() < h);
        final GeodeticPoint oneCentimeterBefore = earth.transform(new Vector3D( 1.0,    earth.transform(lowest),
                                                                               -1.0e-2, p2.subtract(p1).normalize()),
                                                                  earth.getBodyFrame(), null);
        Assertions.assertTrue(oneCentimeterBefore.getAltitude() > lowest.getAltitude());
        final GeodeticPoint oneCentimeterAfter = earth.transform(new Vector3D( 1.0,    earth.transform(lowest),
                                                                              +1.0e-2, p2.subtract(p1).normalize()),
                                                                 earth.getBodyFrame(), null);
         Assertions.assertTrue(oneCentimeterAfter.getAltitude() > lowest.getAltitude());

    }

    @Test
    public void testLowestAltitudeIntermediateField() {
        doTestLowestAltitudeIntermediateField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestLowestAltitudeIntermediateField(final Field<T> field) {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, false));
        final FieldVector3D<T> close = earth.transform(new FieldGeodeticPoint<>(FastMath.toRadians(field.getZero().newInstance(12.0)),
                                                                                FastMath.toRadians(field.getZero().newInstance(34.0)),
                                                                                field.getZero().newInstance(100000.0)));
        final FieldVector3D<T> far   = earth.transform(new FieldGeodeticPoint<>(FastMath.toRadians(field.getZero().newInstance(-47.0)),
                                                                                FastMath.toRadians(field.getZero().newInstance( 43.0)),
                                                                                field.getZero().newInstance(10000000.0)));
        final FieldGeodeticPoint<T> lowestA = earth.lowestAltitudeIntermediate(close, far);
        Assertions.assertEquals(0.0, FieldVector3D.distance(close, earth.transform(lowestA)).getReal(), 2.0e-9);
        final FieldGeodeticPoint<T> lowestB = earth.lowestAltitudeIntermediate(far, close);
        Assertions.assertEquals(0.0, FieldVector3D.distance(close, earth.transform(lowestB)).getReal(), 2.0e-9);

        final double h =5000000.0;
        final FieldVector3D<T> p1 = earth.transform(new FieldGeodeticPoint<>(FastMath.toRadians(field.getZero().newInstance(-20.0)),
                                                                             FastMath.toRadians(field.getZero().newInstance(-12.0)),
                                                                             field.getZero().newInstance(h)));
        final FieldVector3D<T> p2 = earth.transform(new FieldGeodeticPoint<>(FastMath.toRadians(field.getZero().newInstance(17.0)),
                                                                             FastMath.toRadians(field.getZero().newInstance(13.0)),
                                                                             field.getZero().newInstance(h)));
        final FieldGeodeticPoint<T> lowest = earth.lowestAltitudeIntermediate(p1, p2);
        Assertions.assertTrue(lowest.getAltitude().getReal() < h);
        final FieldGeodeticPoint<T> oneCentimeterBefore = earth.transform(new FieldVector3D<>( 1.0,    earth.transform(lowest),
                                                                                              -1.0e-2, p2.subtract(p1).normalize()),
                                                                          earth.getBodyFrame(), null);
        Assertions.assertTrue(oneCentimeterBefore.getAltitude().subtract(lowest.getAltitude()).getReal() > 0);
        final FieldGeodeticPoint<T> oneCentimeterAfter = earth.transform(new FieldVector3D<>( 1.0,    earth.transform(lowest),
                                                                                             +1.0e-2, p2.subtract(p1).normalize()),
                                                                         earth.getBodyFrame(), null);
        Assertions.assertTrue(oneCentimeterAfter.getAltitude().subtract(lowest.getAltitude()).getReal() > 0);

    }

    private void doTestTransformVsOldIterative(OneAxisEllipsoid model,
                                               Stream<Vector3D> points,
                                               double latitudeTolerance, double longitudeTolerance,
                                               double altitudeTolerance) {
        points.forEach(point -> {
            try {
                GeodeticPoint reference = transformOldIterative(model, point, model.getBodyFrame(), null);
                GeodeticPoint result    = model.transform(point, model.getBodyFrame(), null);
                Assertions.assertEquals(reference.getLatitude(),  result.getLatitude(),  latitudeTolerance);
                Assertions.assertEquals(reference.getLongitude(), result.getLongitude(), longitudeTolerance);
                Assertions.assertEquals(reference.getAltitude(),  result.getAltitude(),  altitudeTolerance);
            } catch (OrekitException oe) {
                Assertions.fail(oe.getLocalizedMessage());
            }
        });

    }

    /** Transform a Cartesian point to a surface-relative point.
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
     */
    private GeodeticPoint transformOldIterative(final OneAxisEllipsoid model,
                                                final Vector3D point,
                                                final Frame frame,
                                                final AbsoluteDate date) {

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

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

