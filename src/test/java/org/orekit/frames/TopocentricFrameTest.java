/* Copyright 2002-2025 CS GROUP
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
package org.orekit.frames;

import java.io.IOException;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.hipparchus.util.FieldSinCos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TrackingCoordinates;


class TopocentricFrameTest {

    // Computation date
    private AbsoluteDate date;

    // Reference frame = ITRF
    private Frame itrf;

    // Earth shape
    OneAxisEllipsoid earthSpheric;

    // Body mu
    private double mu;


    @Test
    void testZero() {

        final GeodeticPoint point = new GeodeticPoint(0., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "zero");

        // Check that frame directions are aligned
        final double xDiff = Vector3D.dotProduct(topoFrame.getEast(), Vector3D.PLUS_J);
        final double yDiff = Vector3D.dotProduct(topoFrame.getNorth(), Vector3D.PLUS_K);
        final double zDiff = Vector3D.dotProduct(topoFrame.getZenith(), Vector3D.PLUS_I);
        Assertions.assertEquals(1., xDiff, Utils.epsilonTest);
        Assertions.assertEquals(1., yDiff, Utils.epsilonTest);
        Assertions.assertEquals(1., zDiff, Utils.epsilonTest);
   }

    @Test
    void testPole() {

        final GeodeticPoint point = new GeodeticPoint(FastMath.PI/2., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "north pole");

        // Check that frame directions are aligned
        final double xDiff = Vector3D.dotProduct(topoFrame.getEast(), Vector3D.PLUS_J);
        final double yDiff = Vector3D.dotProduct(topoFrame.getSouth(), Vector3D.PLUS_I);
        final double zDiff = Vector3D.dotProduct(topoFrame.getZenith(), Vector3D.PLUS_K);
        Assertions.assertEquals(1., xDiff, Utils.epsilonTest);
        Assertions.assertEquals(1., yDiff, Utils.epsilonTest);
        Assertions.assertEquals(1., zDiff, Utils.epsilonTest);
   }

    @Test
    void testNormalLatitudes() {

        // First point at latitude 45°
        final GeodeticPoint point1 = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(earthSpheric, point1, "lat 45");
        Assertions.assertEquals(0.0,
                                Vector3D.distance(topoFrame1.getCartesianPoint(), earthSpheric.transform(point1)),
                                7.0e-9);

        // Second point at latitude -45° and same longitude
        final GeodeticPoint point2 = new GeodeticPoint(FastMath.toRadians(-45.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(earthSpheric, point2, "lat -45");
        Assertions.assertEquals(0.0,
                                Vector3D.distance(topoFrame2.getCartesianPoint(), earthSpheric.transform(point2)),
                                2.0e-9);

        // Check that frame North and Zenith directions are all normal to each other, and East are the same
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getEast());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assertions.assertEquals(1., xDiff, Utils.epsilonTest);
        Assertions.assertEquals(0., yDiff, Utils.epsilonTest);
        Assertions.assertEquals(0., zDiff, Utils.epsilonTest);
  }

    @Test
    void testOppositeLongitudes() {

        // First point at latitude 45°
        final GeodeticPoint point1 = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(earthSpheric, point1, "lon 30");
        final GeodeticPoint p1 = topoFrame1.getPoint();
        Assertions.assertEquals(point1.getLatitude(), p1.getLatitude(), 1.0e-15);
        Assertions.assertEquals(point1.getLongitude(), p1.getLongitude(), 1.0e-15);
        Assertions.assertEquals(point1.getAltitude(), p1.getAltitude(), 1.0e-15);

        // Second point at latitude -45° and same longitude
        final GeodeticPoint point2 = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(210.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(earthSpheric, point2, "lon 210");

        // Check that frame North and Zenith directions are all normal to each other,
        // and East of the one is West of the other
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getWest());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assertions.assertEquals(1., xDiff, Utils.epsilonTest);
        Assertions.assertEquals(0., yDiff, Utils.epsilonTest);
        Assertions.assertEquals(0., zDiff, Utils.epsilonTest);
  }

    @Test
    void testAntipodes() {

        // First point at latitude 45° and longitude 30
        final GeodeticPoint point1 = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame1 = new TopocentricFrame(earthSpheric, point1, "lon 30");

        // Second point at latitude -45° and longitude 210
        final GeodeticPoint point2 = new GeodeticPoint(FastMath.toRadians(-45.), FastMath.toRadians(210.), 0.);
        final TopocentricFrame topoFrame2 = new TopocentricFrame(earthSpheric, point2, "lon 210");

        // Check that frame Zenith directions are opposite to each other,
        // and East and North are the same
        final double xDiff = Vector3D.dotProduct(topoFrame1.getEast(), topoFrame2.getWest());
        final double yDiff = Vector3D.dotProduct(topoFrame1.getNorth(), topoFrame2.getNorth());
        final double zDiff = Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getZenith());

        Assertions.assertEquals(1., xDiff, Utils.epsilonTest);
        Assertions.assertEquals(1., yDiff, Utils.epsilonTest);
        Assertions.assertEquals(-1., zDiff, Utils.epsilonTest);

        Assertions.assertEquals(1, Vector3D.dotProduct(topoFrame1.getNadir(), topoFrame2.getZenith()), Utils.epsilonTest);
        Assertions.assertEquals(1, Vector3D.dotProduct(topoFrame1.getZenith(), topoFrame2.getNadir()), Utils.epsilonTest);

    }

    @Test
    void testSiteAtZenith() {

        // Surface point at latitude 45°
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 30 lat 45");

        // Point at 800 km over zenith
        final GeodeticPoint satPoint = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(30.), 800000.);
        final TrackingCoordinates tc = topoFrame.getTrackingCoordinates(earthSpheric.transform(satPoint),
                                                                        earthSpheric.getBodyFrame(),
                                                                        date);

        // Zenith point elevation = 90 deg
        Assertions.assertEquals(FastMath.PI/2., tc.getElevation(), Utils.epsilonAngle);

        // Zenith point range = defined altitude
        Assertions.assertEquals(800000., tc.getRange(), 1e-8);
  }

    @Test
    void testFieldSiteAtZenith() {
        doTestFieldSiteAtZenith(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldSiteAtZenith(final Field<T> field) {

        // zero
        final T zero = field.getZero();

        // Surface point at latitude 45°
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 30 lat 45");

        // Point at 800 km over zenith
        final FieldGeodeticPoint<T> satPoint = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(45.)),
                                                                        zero.add(FastMath.toRadians(30.)),
                                                                        zero.add(800000.));
        final FieldAbsoluteDate<T> fieldDate = new FieldAbsoluteDate<>(field, date);
        final FieldTrackingCoordinates<T> tc = topoFrame.getTrackingCoordinates(earthSpheric.transform(satPoint),
                                                                                earthSpheric.getBodyFrame(),
                                                                                fieldDate);

        // Zenith point elevation = 90 deg
        Assertions.assertEquals(FastMath.PI/2., tc.getElevation().getReal(), Utils.epsilonAngle);

        // Zenith point range = defined altitude
        Assertions.assertEquals(800000., tc.getRange().getReal(), 1e-8);

    }

    @Test
    void testAzimuthEquatorial() {

        // Surface point at latitude 0
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 30 lat 0");

        // Point at infinite, separated by +20 deg in longitude
        // *****************************************************
        GeodeticPoint infPoint = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(50.), 1000000000.);
        TrackingCoordinates tc = topoFrame.getTrackingCoordinates(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);

        // Azimuth = pi/2
        Assertions.assertEquals(FastMath.PI/2., tc.getAzimuth(), Utils.epsilonAngle);

        // Elevation = pi/2 - longitude difference
        Assertions.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude() - infPoint.getLongitude()),
                                tc.getElevation(),
                                1.e-2);

        // Point at infinite, separated by -20 deg in longitude
        // *****************************************************
        infPoint = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(10.), 1000000000.);
        tc = topoFrame.getTrackingCoordinates(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), date);

        // Azimuth = pi/2
        Assertions.assertEquals(3*FastMath.PI/2., tc.getAzimuth(), Utils.epsilonAngle);

        // Elevation = pi/2 - longitude difference
        Assertions.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude() - infPoint.getLongitude()),
                                tc.getElevation(),
                                1.e-2);

    }

    @Test
    void testFieldAzimuthEquatorial() {
        doTestFieldAzimuthEquatorial(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldAzimuthEquatorial(final Field<T> field) {

        // zero
        final T zero = field.getZero();

        // Surface point at latitude 0
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(30.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 30 lat 0");

        // Point at infinite, separated by +20 deg in longitude
        // *****************************************************
        FieldGeodeticPoint<T> infPoint = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(0.)),
                                                                  zero.add(FastMath.toRadians(50.)),
                                                                  zero.add(1000000000.));

        // Field date
        final FieldAbsoluteDate<T> fieldDate = new FieldAbsoluteDate<>(field, date);
        FieldTrackingCoordinates<T> tc = topoFrame.getTrackingCoordinates(earthSpheric.transform(infPoint),
                                                                          earthSpheric.getBodyFrame(),
                                                                          fieldDate);
        // Azimuth = pi/2
        Assertions.assertEquals(FastMath.PI/2., tc.getAzimuth().getReal(), Utils.epsilonAngle);

        // Elevation = pi/2 - longitude difference
        Assertions.assertEquals(FastMath.abs(infPoint.getLongitude().negate().add(point.getLongitude())).negate().add(FastMath.PI/2.).getReal(),
                                tc.getElevation().getReal(),
                                1.e-2);

        // Point at infinite, separated by -20 deg in longitude
        // *****************************************************
        infPoint = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(0.)),
                                            zero.add(FastMath.toRadians(10.)),
                                            zero.add(1000000000.));

        // Azimuth = pi/2
        tc = topoFrame.getTrackingCoordinates(earthSpheric.transform(infPoint), earthSpheric.getBodyFrame(), fieldDate);
        Assertions.assertEquals(3*FastMath.PI/2., tc.getAzimuth().getReal(), Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        Assertions.assertEquals(FastMath.abs(infPoint.getLongitude().negate().add(point.getLongitude())).negate().add(FastMath.PI/2.).getReal(),
                                tc.getElevation().getReal(),
                                1.e-2);

    }

    @Test
    void testAzimuthPole() {

        // Surface point at latitude 0
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(89.999), FastMath.toRadians(0.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 0 lat 90");

        // Point at 30 deg longitude
        // **************************
        GeodeticPoint satPoint = new GeodeticPoint(FastMath.toRadians(28.), FastMath.toRadians(30.), 800000.);
        TrackingCoordinates tc = topoFrame.getTrackingCoordinates(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);

        Assertions.assertEquals(FastMath.PI - satPoint.getLongitude(), tc.getAzimuth(), 1.e-5);

        // Point at -30 deg longitude
        // ***************************
        satPoint = new GeodeticPoint(FastMath.toRadians(28.), FastMath.toRadians(-30.), 800000.);
        tc = topoFrame.getTrackingCoordinates(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), date);

        Assertions.assertEquals(FastMath.PI - satPoint.getLongitude(), tc.getAzimuth(), 1.e-5);

    }

    @Test
    void testFieldAzimuthPole() {
        doTestFieldAzimuthPole(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldAzimuthPole(final Field<T> field) {

        // zero
        final T zero = field.getZero();

        // Surface point at latitude 0
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(89.999), FastMath.toRadians(0.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 0 lat 90");

        // Point at 30 deg longitude
        // **************************
        FieldGeodeticPoint<T> satPoint = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(28.)),
                                                                  zero.add(FastMath.toRadians(30.)), zero.add(800000.));
        final FieldAbsoluteDate<T> fieldDate = new FieldAbsoluteDate<>(field, date);
        FieldTrackingCoordinates<T> tc = topoFrame.getTrackingCoordinates(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), fieldDate);
        Assertions.assertEquals(satPoint.getLongitude().negate().add(FastMath.PI).getReal(),
                                tc.getAzimuth().getReal(), 1.e-5);

        // Point at -30 deg longitude
        // ***************************
        satPoint = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(28.)),
                                            zero.add(FastMath.toRadians(-30.)), zero.add(800000.));
        tc = topoFrame.getTrackingCoordinates(earthSpheric.transform(satPoint), earthSpheric.getBodyFrame(), fieldDate);

        Assertions.assertEquals(satPoint.getLongitude().negate().add(FastMath.PI).getReal(),
                                tc.getAzimuth().getReal(), 1.e-5);

    }

    @Test
    void testGetPVCoordinates() {
        // GIVEN
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(5.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 5 lat 45");
        final Frame frame = FramesFactory.getGCRF();
        // WHEN
        final PVCoordinates pv = topoFrame.getPVCoordinates(date, frame);
        // THEN
        final Vector3D position = topoFrame.getPosition(date, frame);
        Assertions.assertEquals(position, pv.getPosition());
    }

    @Test
    void testFieldGetPVCoordinates() {
        // GIVEN
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(5.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 5 lat 45");
        final Frame frame = FramesFactory.getGCRF();
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        // WHEN
        final FieldPVCoordinates<Binary64> pv = topoFrame.getPVCoordinates(fieldDate, frame);
        // THEN
        final FieldVector3D<Binary64> position = topoFrame.getPosition(fieldDate, frame);
        Assertions.assertEquals(position, pv.getPosition());
    }

    @Test
    void testDoppler() {

        // Surface point at latitude 45, longitude 5
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(5.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 5 lat 45");

        // Point at 30 deg longitude
        // ***************************
        final CircularOrbit orbit =
            new CircularOrbit(7178000.0, 0.5e-8, -0.5e-8, FastMath.toRadians(50.), FastMath.toRadians(120.),
                                   FastMath.toRadians(90.), PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Transform satellite position to position/velocity parameters in body frame
        final Transform eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), date);
        final PVCoordinates pvSatItrf = eme2000ToItrf.transformPVCoordinates(orbit.getPVCoordinates());

        // Compute range rate directly
        //********************************************
        final double dop = topoFrame.getRangeRate(pvSatItrf, earthSpheric.getBodyFrame(), date);

        // Compare to finite difference computation (2 points)
        //*****************************************************
        final double dt = 0.1;
        KeplerianPropagator extrapolator = new KeplerianPropagator(orbit);

        // Extrapolate satellite position a short while after reference date
        AbsoluteDate dateP = date.shiftedBy(dt);
        Transform j2000ToItrfP = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), dateP);
        SpacecraftState orbitP = extrapolator.propagate(dateP);
        Vector3D satPointGeoP = j2000ToItrfP.transformPosition(orbitP.getPosition());

        // Retropolate satellite position a short while before reference date
        AbsoluteDate dateM = date.shiftedBy(-dt);
        Transform j2000ToItrfM = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), dateM);
        SpacecraftState orbitM = extrapolator.propagate(dateM);
        Vector3D satPointGeoM = j2000ToItrfM.transformPosition(orbitM.getPosition());

        // Compute ranges at both instants
        double rangeP = topoFrame.getTrackingCoordinates(satPointGeoP, earthSpheric.getBodyFrame(), dateP).getRange();
        double rangeM = topoFrame.getTrackingCoordinates(satPointGeoM, earthSpheric.getBodyFrame(), dateM).getRange();
        final double dopRef2 = (rangeP - rangeM) / (2. * dt);
        Assertions.assertEquals(dopRef2, dop, 1.e-3);

    }

    @Test
    void testFieldDoppler() {
        doTestFieldDoppler(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldDoppler(final Field<T> field) {

        // zero
        final T zero = field.getZero();

        // Surface point at latitude 45, longitude 5
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.), FastMath.toRadians(5.), 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, point, "lon 5 lat 45");

        // Field date
        final FieldAbsoluteDate<T> fieldDate = new FieldAbsoluteDate<>(field, date);

        // Point at 30 deg longitude
        // ***************************
        final FieldCircularOrbit<T> orbit =
            new FieldCircularOrbit<>(zero.add(7178000.0), zero.add(0.5e-8), zero.add(-0.5e-8),
                                     zero.add(FastMath.toRadians(50.)), zero.add(FastMath.toRadians(120.)),
                                     zero.add(FastMath.toRadians(90.)), PositionAngleType.MEAN,
                                     FramesFactory.getEME2000(), fieldDate, zero.add(mu));

        // Transform satellite position to position/velocity parameters in body frame
        final FieldTransform<T> eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), fieldDate);
        final FieldPVCoordinates<T> pvSatItrf = eme2000ToItrf.transformPVCoordinates(orbit.getPVCoordinates());

        // Compute range rate directly
        //********************************************
        final T dop = topoFrame.getRangeRate(pvSatItrf, earthSpheric.getBodyFrame(), fieldDate);

        // Compare to finite difference computation (2 points)
        //*****************************************************
        final double dt = 0.1;
        FieldKeplerianPropagator<T> extrapolator = new FieldKeplerianPropagator<>(orbit);

        // Extrapolate satellite position a short while after reference date
        FieldAbsoluteDate<T> dateP = fieldDate.shiftedBy(dt);
        FieldTransform<T> j2000ToItrfP = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), dateP);
        FieldSpacecraftState<T> orbitP = extrapolator.propagate(dateP);
        FieldVector3D<T> satPointGeoP = j2000ToItrfP.transformPosition(orbitP.getPosition());

        // Retropolate satellite position a short while before reference date
        FieldAbsoluteDate<T> dateM = fieldDate.shiftedBy(-dt);
        FieldTransform<T> j2000ToItrfM = FramesFactory.getEME2000().getTransformTo(earthSpheric.getBodyFrame(), dateM);
        FieldSpacecraftState<T> orbitM = extrapolator.propagate(dateM);
        FieldVector3D<T> satPointGeoM = j2000ToItrfM.transformPosition(orbitM.getPosition());

        // Compute ranges at both instants
        T rangeP = topoFrame.getTrackingCoordinates(satPointGeoP, earthSpheric.getBodyFrame(), dateP).getRange();
        T rangeM = topoFrame.getTrackingCoordinates(satPointGeoM, earthSpheric.getBodyFrame(), dateM).getRange();
        final T dopRef2 = (rangeP.subtract(rangeM)).divide(2. * dt);
        Assertions.assertEquals(dopRef2.getReal(), dop.getReal(), 1.e-3);

    }

    @Test
    void testEllipticEarth()  {

        // Elliptic earth shape
        final OneAxisEllipsoid earthElliptic =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Satellite point
        // Caution !!! Sat point target shall be the same whatever earth shape chosen !!
        final GeodeticPoint satPointGeo = new GeodeticPoint(FastMath.toRadians(30.), FastMath.toRadians(15.), 800000.);
        final Vector3D satPoint = earthElliptic.transform(satPointGeo);

        // ****************************
        // Test at equatorial position
        // ****************************
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(5.), 0.);
        TopocentricFrame topoElliptic  = new TopocentricFrame(earthElliptic, point, "elliptic, equatorial lon 5");
        TopocentricFrame topoSpheric   = new TopocentricFrame(earthSpheric, point, "spheric, equatorial lon 5");
        TrackingCoordinates tcElliptic = topoElliptic.getTrackingCoordinates(satPoint, earthElliptic.getBodyFrame(), date);
        TrackingCoordinates tcSpheric  = topoSpheric.getTrackingCoordinates(satPoint, earthElliptic.getBodyFrame(), date);

        // Compare azimuth/elevation/range of satellite point : shall be strictly identical
        // ***************************************************
        Assertions.assertEquals(tcElliptic.getAzimuth(),   tcSpheric.getAzimuth(),   Utils.epsilonAngle);
        Assertions.assertEquals(tcElliptic.getElevation(), tcSpheric.getElevation(), Utils.epsilonAngle);
        Assertions.assertEquals(tcElliptic.getRange(),     tcSpheric.getRange(),     Utils.epsilonTest);

        // Infinite point separated by -20 deg in longitude
        // *************************************************
        GeodeticPoint infPointGeo = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(-15.), 1000000000.);
        Vector3D infPoint = earthElliptic.transform(infPointGeo);
        tcElliptic = topoElliptic.getTrackingCoordinates(infPoint, earthElliptic.getBodyFrame(), date);

        // Azimuth = pi/2
        Assertions.assertEquals(3*FastMath.PI/2., tcElliptic.getAzimuth(), Utils.epsilonAngle);

        // Elevation = pi/2 - longitude difference
        Assertions.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude() - infPointGeo.getLongitude()),
                                tcElliptic.getElevation(), 1.e-2);

        // Infinite point separated by +20 deg in longitude
        // *************************************************
        infPointGeo = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(25.), 1000000000.);
        infPoint = earthElliptic.transform(infPointGeo);
        tcElliptic = topoElliptic.getTrackingCoordinates(infPoint, earthElliptic.getBodyFrame(), date);

        // Azimuth = pi/2
        Assertions.assertEquals(FastMath.PI/2., tcElliptic.getAzimuth(), Utils.epsilonAngle);

        // Site = pi/2 - longitude difference
        Assertions.assertEquals(FastMath.PI/2. - FastMath.abs(point.getLongitude() - infPointGeo.getLongitude()),
                                tcElliptic.getElevation(), 1.e-2);

        // ************************
        // Test at polar position
        // ************************
        point = new GeodeticPoint(FastMath.toRadians(89.999), FastMath.toRadians(0.), 0.);
        topoSpheric  = new TopocentricFrame(earthSpheric, point, "lon 0 lat 90");
        topoElliptic = new TopocentricFrame(earthElliptic, point, "lon 0 lat 90");

        // Compare azimuth/elevation/range of satellite point : slight difference due to earth flatness
        // ***************************************************
        tcElliptic = topoElliptic.getTrackingCoordinates(satPoint, earthElliptic.getBodyFrame(), date);
        tcSpheric  = topoSpheric.getTrackingCoordinates(satPoint, earthSpheric.getBodyFrame(), date);
        Assertions.assertEquals(tcElliptic.getAzimuth(),   tcSpheric.getAzimuth(),   1.e-7);
        Assertions.assertEquals(tcElliptic.getElevation(), tcSpheric.getElevation(), 1.e-2);
        Assertions.assertEquals(tcElliptic.getRange(),     tcSpheric.getRange(),     20.e+3);


        // *********************
        // Test at any position
        // *********************
        point = new GeodeticPoint(FastMath.toRadians(60), FastMath.toRadians(30.), 0.);
        topoSpheric  = new TopocentricFrame(earthSpheric, point, "lon 10 lat 45");
        topoElliptic = new TopocentricFrame(earthElliptic, point, "lon 10 lat 45");

        // Compare azimuth/elevation/range of satellite point : slight difference
        // ***************************************************
        tcElliptic = topoElliptic.getTrackingCoordinates(satPoint, earthElliptic.getBodyFrame(), date);
        tcSpheric  = topoSpheric.getTrackingCoordinates(satPoint, earthSpheric.getBodyFrame(), date);
        Assertions.assertEquals(tcElliptic.getAzimuth(),   tcSpheric.getAzimuth(),   1.e-2);
        Assertions.assertEquals(tcElliptic.getElevation(), tcSpheric.getElevation(), 1.e-2);
        Assertions.assertEquals(tcElliptic.getRange(),     tcSpheric.getRange(),     20.e+3);

    }

    @Test
    void testFieldEllipticEarth() {
        doTestFieldEllipticEarth(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldEllipticEarth(final Field<T> field)  {

        // zero
        final T zero = field.getZero();

        // Elliptic earth shape
        final OneAxisEllipsoid earthElliptic =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        // Field date
        final FieldAbsoluteDate<T> fieldDate = new FieldAbsoluteDate<>(field, date);

        // Satellite point
        // Caution !!! Sat point target shall be the same whatever earth shape chosen !!
        final FieldGeodeticPoint<T> satPointGeo = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(30.)),
                                                                           zero.add(FastMath.toRadians(15.)),
                                                                           zero.add(800000.));
        final FieldVector3D<T> satPoint = earthElliptic.transform(satPointGeo);

        // ****************************
        // Test at equatorial position
        // ****************************
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(5.), 0.);
        TopocentricFrame topoElliptic = new TopocentricFrame(earthElliptic, point, "elliptic, equatorial lon 5");
        TopocentricFrame topoSpheric = new TopocentricFrame(earthSpheric, point, "spheric, equatorial lon 5");

        // Compare azimuth/elevation/range of satellite point : shall be strictly identical
        // ***************************************************
        FieldTrackingCoordinates<T> tcElliptic =
                        topoElliptic.getTrackingCoordinates(satPoint, earthElliptic.getBodyFrame(), fieldDate);
        FieldTrackingCoordinates<T> tcSpheric =
                        topoSpheric.getTrackingCoordinates(satPoint, earthSpheric.getBodyFrame(), fieldDate);
        Assertions.assertEquals(tcElliptic.getAzimuth().getReal(),   tcSpheric.getAzimuth().getReal(),   Utils.epsilonAngle);
        Assertions.assertEquals(tcElliptic.getElevation().getReal(), tcSpheric.getElevation().getReal(), Utils.epsilonAngle);
        Assertions.assertEquals(tcElliptic.getRange().getReal(),     tcSpheric.getRange().getReal(),     Utils.epsilonTest);

        // Infinite point separated by -20 deg in longitude
        // *************************************************
        FieldGeodeticPoint<T> infPointGeo = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(0.)),
                                                                     zero.add(FastMath.toRadians(-15.)),
                                                                     zero.add(1000000000.));
        FieldVector3D<T> infPoint = earthElliptic.transform(infPointGeo);
        tcElliptic = topoElliptic.getTrackingCoordinates(infPoint, earthElliptic.getBodyFrame(), fieldDate);

        // Azimuth = pi/2
        Assertions.assertEquals(3*FastMath.PI/2., tcElliptic.getAzimuth().getReal(), Utils.epsilonAngle);

        // Elevation = pi/2 - longitude difference
        Assertions.assertEquals(FastMath.abs(infPointGeo.getLongitude().negate().add(point.getLongitude())).negate().add(FastMath.PI/2.).getReal(),
                                tcElliptic.getElevation().getReal(),
                                1.e-2);

        // Infinite point separated by +20 deg in longitude
        // *************************************************
        infPointGeo = new FieldGeodeticPoint<>(zero.add(FastMath.toRadians(0.)),
                                               zero.add(FastMath.toRadians(25.)),
                                               zero.add(1000000000.));
        infPoint = earthElliptic.transform(infPointGeo);
        tcElliptic = topoElliptic.getTrackingCoordinates(infPoint, earthElliptic.getBodyFrame(), fieldDate);

        // Azimuth = pi/2
        Assertions.assertEquals(FastMath.PI/2., tcElliptic.getAzimuth().getReal(), Utils.epsilonAngle);

        // Elevation = pi/2 - longitude difference
        Assertions.assertEquals(FastMath.abs(infPointGeo.getLongitude().negate().add(point.getLongitude())).negate().add(FastMath.PI/2.).getReal(),
                                tcElliptic.getElevation().getReal(),
                                1.e-2);

        // ************************
        // Test at polar position
        // ************************
        point = new GeodeticPoint(FastMath.toRadians(89.999), FastMath.toRadians(0.), 0.);
        topoSpheric  = new TopocentricFrame(earthSpheric, point, "lon 0 lat 90");
        topoElliptic = new TopocentricFrame(earthElliptic, point, "lon 0 lat 90");
        tcElliptic = topoElliptic.getTrackingCoordinates(satPoint, earthElliptic.getBodyFrame(), fieldDate);
        tcSpheric = topoSpheric.getTrackingCoordinates(satPoint, earthSpheric.getBodyFrame(), fieldDate);

        // Compare azimuth/elevation/range of satellite point : slight difference due to earth flatness
        // ***************************************************
        Assertions.assertEquals(tcElliptic.getAzimuth().getReal(),   tcSpheric.getAzimuth().getReal(),   1.e-7);
        Assertions.assertEquals(tcElliptic.getElevation().getReal(), tcSpheric.getElevation().getReal(), 1.e-2);
        Assertions.assertEquals(tcElliptic.getRange().getReal(),     tcSpheric.getRange().getReal(),     20.e+3);


        // *********************
        // Test at any position
        // *********************
        point = new GeodeticPoint(FastMath.toRadians(60), FastMath.toRadians(30.), 0.);
        topoSpheric  = new TopocentricFrame(earthSpheric, point, "lon 10 lat 45");
        topoElliptic = new TopocentricFrame(earthElliptic, point, "lon 10 lat 45");
        tcElliptic = topoElliptic.getTrackingCoordinates(satPoint, earthElliptic.getBodyFrame(), fieldDate);
        tcSpheric = topoSpheric.getTrackingCoordinates(satPoint, earthSpheric.getBodyFrame(), fieldDate);

        // Compare azimuth/elevation/range of satellite point : slight difference
        // ***************************************************
        Assertions.assertEquals(tcElliptic.getAzimuth().getReal(),   tcSpheric.getAzimuth().getReal(),   1.e-2);
        Assertions.assertEquals(tcElliptic.getElevation().getReal(), tcSpheric.getElevation().getReal(), 1.e-2);
        Assertions.assertEquals(tcElliptic.getRange().getReal(),     tcSpheric.getRange().getReal(),     20.e+3);

    }

    @Test
    void testPointAtDistance() {

        RandomGenerator random = new Well1024a(0xa1e6bd5cd0578779l);
        final OneAxisEllipsoid earth =
            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                                 itrf);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        for (int i = 0; i < 20; ++i) {
            // we don't need uniform point on the sphere, just a few different test configurations
            double latitude  = FastMath.PI * (0.5 - random.nextDouble());
            double longitude = 2 * FastMath.PI * random.nextDouble();
            TopocentricFrame topo = new TopocentricFrame(earth,
                                                         new GeodeticPoint(latitude, longitude, 0.0),
                                                         "topo");
            Transform transform = earth.getBodyFrame().getTransformTo(topo, date);
            for (int j = 0; j < 20; ++j) {
                double elevation      = FastMath.PI * (0.5 - random.nextDouble());
                double azimuth        = 2 * FastMath.PI * random.nextDouble();
                double range          = 500000.0 * (1.0 + random.nextDouble());
                Vector3D absolutePoint = earth.transform(topo.pointAtDistance(azimuth, elevation, range));
                Vector3D relativePoint = transform.transformPosition(absolutePoint);
                TrackingCoordinates rebuiltTC = topo.getTrackingCoordinates(relativePoint, topo, AbsoluteDate.J2000_EPOCH);
                Assertions.assertEquals(elevation, rebuiltTC.getElevation(), 1.0e-12);
                Assertions.assertEquals(azimuth, MathUtils.normalizeAngle(rebuiltTC.getAzimuth(), azimuth), 1.0e-12);
                Assertions.assertEquals(range, rebuiltTC.getRange(), 1.0e-12 * range);
            }
        }
    }

    @Test
    void testIssue145() {
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                               Constants.WGS84_EARTH_FLATTENING,
                                               itrf);
        TopocentricFrame staFrame = new TopocentricFrame(earth, new GeodeticPoint(0.0, 0.0, 0.0), "test");
        GeodeticPoint gp = staFrame.computeLimitVisibilityPoint(Constants.WGS84_EARTH_EQUATORIAL_RADIUS+600000,
                                                                0.0, FastMath.toRadians(5.0));
        Assertions.assertEquals(0.0, gp.getLongitude(), 1.0e-15);
        Assertions.assertTrue(gp.getLatitude() > 0);
        Assertions.assertEquals(0.0, staFrame.getNorth().distance(Vector3D.PLUS_K), 1.0e-15);

    }

    @Test
    void testVisibilityCircle() throws IOException {

        // a few random from International Laser Ranging Service
        final BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                     Constants.WGS84_EARTH_FLATTENING,
                                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame[] ilrs = {
            new TopocentricFrame(earth,
                                 new GeodeticPoint(FastMath.toRadians(52.3800), FastMath.toRadians(3.0649), 133.745),
                                 "Potsdam"),
            new TopocentricFrame(earth,
                                 new GeodeticPoint(FastMath.toRadians(36.46273), FastMath.toRadians(-6.20619), 64.0),
                                 "San Fernando"),
            new TopocentricFrame(earth,
                                 new GeodeticPoint(FastMath.toRadians(35.5331), FastMath.toRadians(24.0705), 157.0),
                                 "Chania")
        };

        PolynomialFunction distanceModel =
                new PolynomialFunction(new double[] { 7.0892e+05, 3.1913, -8.2181e-07, 1.4033e-13 });
        for (TopocentricFrame station : ilrs) {
            for (double altitude = 500000; altitude < 2000000; altitude += 100000) {
                for (double azimuth = 0; azimuth < 2 * FastMath.PI; azimuth += 0.05) {
                    GeodeticPoint p = station.computeLimitVisibilityPoint(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + altitude,
                                                                          azimuth, FastMath.toRadians(5.0));
                    double d = station.getTrackingCoordinates(earth.transform(p), earth.getBodyFrame(), AbsoluteDate.J2000_EPOCH).getRange();
                    Assertions.assertEquals(distanceModel.value(altitude), d, 40000.0);
                }
            }
        }

    }

    private static Stream<Arguments> testGetTopocentricCoordinatesValues() {
        return Stream.of(
                Arguments.of(0,0,1),
                Arguments.of(0, FastMath.PI / 2, 1),
                Arguments.of(FastMath.PI, FastMath.PI / 3, 10),
                Arguments.of(3 * FastMath.PI / 2, FastMath.PI / 4, 1000),
                Arguments.of(FastMath.PI / 2, -FastMath.PI / 6, 500),
                Arguments.of(FastMath.PI / 7, -FastMath.PI / 5, 100)
        );
    }

    @ParameterizedTest
    @MethodSource("testGetTopocentricCoordinatesValues")
    void testGetTopocentricCoordinates(final double az, final double el, final double r) {
        final TrackingCoordinates coords = new TrackingCoordinates(az,el,r);
        final Vector3D topoPos = TopocentricFrame.getTopocentricPosition(coords);

        final SinCos sinCosAz = FastMath.sinCos(az);
        final SinCos sinCosEl = FastMath.sinCos(el);
        final double expectedX, expectedY, expectedZ;
        expectedX = sinCosAz.sin() * sinCosEl.cos() * r;
        expectedY = sinCosAz.cos() * sinCosEl.cos() * r;
        expectedZ = sinCosEl.sin() * r;

        final double distanceTolerance = 1e-7;
        Assertions.assertEquals(expectedX,topoPos.getX(), distanceTolerance);
        Assertions.assertEquals(expectedY,topoPos.getY(), distanceTolerance);
        Assertions.assertEquals(expectedZ,topoPos.getZ(), distanceTolerance);
    }

    @ParameterizedTest
    @MethodSource("testGetTopocentricCoordinatesValues")
    void testGetFieldTopocentricCoordinates(final double azimuth, final double elevation, final double range) {
        final Binary64 az, el, r;
        az = new Binary64(azimuth);
        el = new Binary64(elevation);
        r = new Binary64(range);

        final FieldTrackingCoordinates<Binary64> coords = new FieldTrackingCoordinates<>(az, el, r);
        final FieldVector3D<Binary64> topoPos = TopocentricFrame.getTopocentricPosition(coords);

        final FieldSinCos<Binary64> sinCosAz = FastMath.sinCos(az);
        final FieldSinCos<Binary64> sinCosEl = FastMath.sinCos(el);
        final Binary64 expectedX, expectedY, expectedZ;
        expectedX = r.multiply(sinCosAz.sin().multiply(sinCosEl.cos()));
        expectedY = r.multiply(sinCosEl.cos().multiply(sinCosAz.cos()));
        expectedZ = r.multiply(sinCosEl.sin());

        final double distanceTolerance = 1e-7;
        Assertions.assertEquals(expectedX.getReal(), topoPos.getX().getReal(), distanceTolerance);
        Assertions.assertEquals(expectedY.getReal(), topoPos.getY().getReal(), distanceTolerance);
        Assertions.assertEquals(expectedZ.getReal(), topoPos.getZ().getReal(), distanceTolerance);
    }

    @ParameterizedTest
    @MethodSource("testGetTopocentricCoordinatesValues")
    void testInverseGetTopocentricCoordinates(double az, double el, double r) {
        final TrackingCoordinates expectedCoords = new TrackingCoordinates(az, el, r);

        final Vector3D point = TopocentricFrame.getTopocentricPosition(expectedCoords);
        final GeodeticPoint geodeticPoint = new GeodeticPoint(0., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, geodeticPoint, "geodeticPoint");
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final TrackingCoordinates coords = topoFrame.getTrackingCoordinates(point, topoFrame, date);

        final double angularTolerance = 1e-12;
        Assertions.assertEquals(expectedCoords.getAzimuth(), coords.getAzimuth(), angularTolerance);
        Assertions.assertEquals(expectedCoords.getElevation(), coords.getElevation(), angularTolerance);
        Assertions.assertEquals(expectedCoords.getRange(), coords.getRange());
    }

    @Test
    void testGetTrackingCoordinates() {
        // GIVEN
        final GeodeticPoint geodeticPoint = new GeodeticPoint(0., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, geodeticPoint, "geodeticPoint");
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D point = new Vector3D(1., 2., -3.);
        // WHEN
        final TrackingCoordinates trackingCoordinates = topoFrame.getTrackingCoordinates(point, frame, date);
        // THEN
        final double expectedElevation = topoFrame.getElevation(point, frame, date);
        final double expectedAzimuth = topoFrame.getAzimuth(point, frame, date);
        final double expectedRange = topoFrame.getRange(point, frame, date);
        Assertions.assertEquals(expectedElevation, trackingCoordinates.getElevation());
        Assertions.assertEquals(expectedAzimuth, trackingCoordinates.getAzimuth());
        Assertions.assertEquals(expectedRange, trackingCoordinates.getRange());
    }

    @Test
    void testGetFieldTrackingCoordinates() {
        // GIVEN
        final GeodeticPoint geodeticPoint = new GeodeticPoint(0., 0., 0.);
        final TopocentricFrame topoFrame = new TopocentricFrame(earthSpheric, geodeticPoint, "geodeticPoint");
        final ComplexField field = ComplexField.getInstance();
        final FieldAbsoluteDate<Complex> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final Frame frame = FramesFactory.getGCRF();
        final FieldVector3D<Complex> point = new FieldVector3D<>(field, new Vector3D(1., 2., -3.));
        // WHEN
        final FieldTrackingCoordinates<Complex> trackingCoordinates = topoFrame.getTrackingCoordinates(point, frame, date);
        // THEN
        final Complex expectedElevation = topoFrame.getElevation(point, frame, date);
        final Complex expectedAzimuth = topoFrame.getAzimuth(point, frame, date);
        final Complex expectedRange = topoFrame.getRange(point, frame, date);
        Assertions.assertEquals(expectedElevation, trackingCoordinates.getElevation());
        Assertions.assertEquals(expectedAzimuth, trackingCoordinates.getAzimuth());
        Assertions.assertEquals(expectedRange, trackingCoordinates.getRange());
    }

    @BeforeEach
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Reference frame = ITRF
        itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        // Elliptic earth shape
        earthSpheric = new OneAxisEllipsoid(6378136.460, 0., itrf);

        // Reference date
        date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                TimeComponents.H00,
                                TimeScalesFactory.getUTC());

        // Body mu
        mu = 3.9860047e14;

    }

    @AfterEach
    public void tearDown() {
        date = null;
        itrf = null;
        earthSpheric = null;
    }


}
