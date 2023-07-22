/* Copyright 2002-2023 Joseph Reed
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Joseph Reed licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.utils;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.spherical.twod.Circle;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.LoxodromeArc;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.time.AbsoluteDate;

/** Unit tests for {@link WaypointPVBuilder}. */
public class WaypointPVBuilderTest {
    /** epsilon for vector comparison. */
    private static final double EPSILON = 1e7;

    /** Date of first waypoint. */
    private AbsoluteDate date1;
    /** Date of second waypoint. */
    private AbsoluteDate date2;
    /** Date of third waypoint. */
    private AbsoluteDate date3;
    /** New york. */
    private GeodeticPoint newYork;
    /** London. */
    private GeodeticPoint london;
    /** Philadelphia. */
    private GeodeticPoint philadelphia;
    /** Reference body (earth). */
    private OneAxisEllipsoid body;

    /** Test setup. */
    @BeforeEach
    public void setUpBefore() {
        Utils.setDataRoot("regular-data");

        date1 = AbsoluteDate.ARBITRARY_EPOCH;
        date2 = date1.shiftedBy(3600.);
        date3 = date2.shiftedBy(3600.);

        newYork = new GeodeticPoint(FastMath.toRadians(40.714268), FastMath.toRadians(-74.005974), 10.0584);
        london = new GeodeticPoint(FastMath.toRadians(51.5), FastMath.toRadians(-0.16667), 10.9728);
        philadelphia = new GeodeticPoint(FastMath.toRadians(39.952330), FastMath.toRadians(-75.16379), 11.8872);

        body = ReferenceEllipsoid.getWgs84(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
    }

    /** Verify cartesian interpolation. */
    @Test
    public void cartesian() {
        final WaypointPVBuilder builder = WaypointPVBuilder.cartesianBuilder(body)
                        .addWaypoint(philadelphia, date1)
                        .addWaypoint(newYork, date2)
                        .addWaypoint(london, date3)
                        .invalidBefore()
                        .invalidAfter();
        final PVCoordinatesProvider pvProv = builder.build();

        final Vector3D expectedPhillyPos = body.transform(philadelphia);
        final Vector3D expectedNyPos = body.transform(newYork);
        final Vector3D expectedLondonPos = body.transform(london);

        final Vector3D segmentOneVel = expectedNyPos.subtract(expectedPhillyPos).scalarMultiply(1. / 3600.);
        final Vector3D segmentTwoVel = expectedLondonPos.subtract(expectedNyPos).scalarMultiply(1. / 3600.);

        // check initial waypoint
        final PVCoordinates pv1 = pvProv.getPVCoordinates(date1, body.getBodyFrame());
        Assertions.assertEquals(expectedPhillyPos, pv1.getPosition());
        Assertions.assertEquals(segmentOneVel, pv1.getVelocity());

        // check within segment 1
        final PVCoordinates pv2 = pvProv.getPVCoordinates(date1.shiftedBy(900.), body.getBodyFrame());
        Assertions.assertEquals(expectedPhillyPos.add(segmentOneVel.scalarMultiply(900.)), pv2.getPosition());
        Assertions.assertEquals(segmentOneVel, pv2.getVelocity());

        // check second waypoint
        final PVCoordinates pv3 = pvProv.getPVCoordinates(date2, body.getBodyFrame());
        Assertions.assertEquals(expectedNyPos, pv3.getPosition());
        Assertions.assertEquals(segmentTwoVel, pv3.getVelocity());

        // check within segment 2
        final TimeStampedPVCoordinates pv4 = pvProv.getPVCoordinates(date2.shiftedBy(1800.), body.getBodyFrame());
        Assertions.assertEquals(expectedNyPos.add(segmentTwoVel.scalarMultiply(1800.)),
                                pvProv.getPosition(pv4.getDate(), body.getBodyFrame()));
        Assertions.assertEquals(segmentTwoVel, pv4.getVelocity());

        // check third waypoint
        final PVCoordinates pv5 = pvProv.getPVCoordinates(date3, body.getBodyFrame());
        Assertions.assertEquals(expectedLondonPos, pvProv.getPosition(date3, body.getBodyFrame()));
        Assertions.assertEquals(Vector3D.ZERO, pv5.getVelocity());

        // check invalid before
        try {
            pvProv.getPVCoordinates(date1.shiftedBy(-1e-16), body.getBodyFrame());
            Assertions.fail("expected exception, but none was thrown.");
        }
        catch (final OrekitIllegalArgumentException ex) {
            // test passes
        }
        Vector3D pBefore = builder.constantBefore().build().getPosition(date1.shiftedBy(-60.0), body.getBodyFrame());
        Assertions.assertEquals(0.0, Vector3D.distance(expectedPhillyPos, pBefore), 1.0e-15);

        // check invalid after
        try {
            pvProv.getPVCoordinates(date3.shiftedBy(2 * Double.MIN_VALUE), body.getBodyFrame());
            Assertions.fail("expected exception, but none was thrown.");
        }
        catch (final OrekitIllegalArgumentException ex) {
            // test passes
        }
        Vector3D pAfter = builder.constantAfter().build().getPosition(date3.shiftedBy(+60.0), body.getBodyFrame());
        Assertions.assertEquals(0.0, Vector3D.distance(expectedLondonPos, pAfter), 1.0e-15);

    }

    /** Verify loxodrome interpolation. */
    @Test
    public void loxodrome() {
        final PVCoordinatesProvider pvProv = WaypointPVBuilder.loxodromeBuilder(body)
                .addWaypoint(philadelphia, date1)
                .addWaypoint(newYork, date2)
                .addWaypoint(london, date3)
                .build();

        final Vector3D expectedPhillyPos = body.transform(philadelphia);
        final Vector3D expectedNyPos = body.transform(newYork);
        final Vector3D expectedLondonPos = body.transform(london);

        final LoxodromeArc arc1 = new LoxodromeArc(philadelphia, newYork, body);
        final LoxodromeArc arc2 = new LoxodromeArc(newYork, london, body);

        final double vel1 = arc1.getDistance() / 3600.;
        final double vel2 = arc2.getDistance() / 3600.;

        // check initial waypoint
        final PVCoordinates pv1 = pvProv.getPVCoordinates(date1, body.getBodyFrame());
        assertVectorsEqual(expectedPhillyPos, pv1.getPosition());
        assertVectorsEqual(loxVelocity(arc1, expectedPhillyPos, vel1), pv1.getVelocity());

        // check within segment 1
        final PVCoordinates pv2 = pvProv.getPVCoordinates(date1.shiftedBy(900.), body.getBodyFrame());
        final Vector3D p2 = body.transform(arc1.calculatePointAlongArc(900. / 3600.));
        assertVectorsEqual(p2, pv2.getPosition());
        assertVectorsEqual(loxVelocity(arc1, p2, vel1), pv2.getVelocity());

        // check second waypoint
        final PVCoordinates pv3 = pvProv.getPVCoordinates(date2, body.getBodyFrame());
        assertVectorsEqual(expectedNyPos, pv3.getPosition());
        assertVectorsEqual(loxVelocity(arc2, expectedNyPos, vel2), pv3.getVelocity());

        // check within segment 2
        final TimeStampedPVCoordinates pv4 = pvProv.getPVCoordinates(date2.shiftedBy(1800.), body.getBodyFrame());
        final Vector3D p4 = body.transform(arc2.calculatePointAlongArc(1800. / 3600.));
        assertVectorsEqual(p4, pvProv.getPosition(pv4.getDate(),  body.getBodyFrame()));
        assertVectorsEqual(loxVelocity(arc2, p4, vel2), pv4.getVelocity());

        // check third waypoint
        final PVCoordinates pv5 = pvProv.getPVCoordinates(date3, body.getBodyFrame());
        assertVectorsEqual(expectedLondonPos, pvProv.getPosition(date3, body.getBodyFrame()));
        assertVectorsEqual(Vector3D.ZERO, pv5.getVelocity());

        // check invalid before
        try {
            pvProv.getPVCoordinates(date1.shiftedBy(-1e-16), body.getBodyFrame());
            Assertions.fail("expected exception, but none was thrown.");
        }
        catch (final OrekitIllegalArgumentException ex) {
            // test passes
        }

        // check invalid after
        try {
            pvProv.getPVCoordinates(date3.shiftedBy(2 * Double.MIN_VALUE), body.getBodyFrame());
            Assertions.fail("expected exception, but none was thrown.");
        }
        catch (final OrekitIllegalArgumentException ex) {
            // test passes
        }
    }

    /** Verify great-circle interpolation. */
    @Test
    public void greatCircle() {
        final PVCoordinatesProvider pvProv = WaypointPVBuilder.greatCircleBuilder(body)
                .addWaypoint(philadelphia, date1)
                .addWaypoint(newYork, date2)
                .addWaypoint(london, date3)
                .build();

        final Vector3D expectedPhillyPos = body.transform(philadelphia);
        final Vector3D expectedNyPos = body.transform(newYork);
        final Vector3D expectedLondonPos = body.transform(london);

        // check initial waypoint
        final PVCoordinates pv1 = pvProv.getPVCoordinates(date1, body.getBodyFrame());
        assertVectorsEqual(expectedPhillyPos, pv1.getPosition());
        assertVectorsEqual(greatCircleVelocity(philadelphia, newYork, 0), pv1.getVelocity());

        // check within segment 1
        final PVCoordinates pv2 = pvProv.getPVCoordinates(date1.shiftedBy(900.), body.getBodyFrame());
        final Vector3D p2 = interpolateGreatCircle(philadelphia, newYork, 900. / 3600.);
        assertVectorsEqual(p2, pv2.getPosition());
        assertVectorsEqual(greatCircleVelocity(philadelphia, newYork, 900. / 3600.), pv2.getVelocity());

        // check second waypoint
        final PVCoordinates pv3 = pvProv.getPVCoordinates(date2, body.getBodyFrame());
        assertVectorsEqual(expectedNyPos, pv3.getPosition());
        assertVectorsEqual(greatCircleVelocity(newYork, london, 0), pv3.getVelocity());

        // check within segment 2
        final TimeStampedPVCoordinates pv4 = pvProv.getPVCoordinates(date2.shiftedBy(1800.), body.getBodyFrame());
        final Vector3D p4 = interpolateGreatCircle(newYork, london, 1800. / 3600.);
        assertVectorsEqual(p4, pvProv.getPosition(pv4.getDate(), body.getBodyFrame()));
        assertVectorsEqual(greatCircleVelocity(newYork, london, 0.5), pv4.getVelocity());

        // check third waypoint
        final PVCoordinates pv5 = pvProv.getPVCoordinates(date3, body.getBodyFrame());
        assertVectorsEqual(expectedLondonPos, pvProv.getPosition(date3, body.getBodyFrame()));
        assertVectorsEqual(Vector3D.ZERO, pv5.getVelocity());

        // check invalid before
        try {
            pvProv.getPVCoordinates(date1.shiftedBy(-1e-16), body.getBodyFrame());
            Assertions.fail("expected exception, but none was thrown.");
        }
        catch (final OrekitIllegalArgumentException ex) {
            // test passes
        }

        // check invalid after
        try {
            pvProv.getPVCoordinates(date3.shiftedBy(2 * Double.MIN_VALUE), body.getBodyFrame());
            Assertions.fail("expected exception, but none was thrown.");
        }
        catch (final OrekitIllegalArgumentException ex) {
            // test passes
        }
    }

    private Vector3D loxVelocity(final LoxodromeArc arc, final Vector3D point, final double velMag) {
        final GeodeticPoint p = arc.getBody().transform(point, arc.getBody().getBodyFrame(), AbsoluteDate.ARBITRARY_EPOCH);

        final Vector3D p2 = arc.getBody().transform(
            new TopocentricFrame(body, p, "frame").pointAtDistance(arc.getAzimuth(), 0, velMag));
        return p2.subtract(point);
    }

    private static void assertVectorsEqual(final Vector3D expected, final Vector3D actual) {
        if (Vector3D.distance(expected, actual) > EPSILON) {
            Assertions.fail("Expected " + expected + " but was " + actual);
        }
    }

    private Vector3D interpolateGreatCircle(final GeodeticPoint p1, final GeodeticPoint p2, final double fraction) {
        final S2Point sp1 = new S2Point(p1.getLongitude(), 0.5 * FastMath.PI - p1.getLatitude());
        final S2Point sp2 = new S2Point(p2.getLongitude(), 0.5 * FastMath.PI - p2.getLatitude());

        final Circle c = new Circle(sp1, sp2, 1e-10);
        final double phase1 = c.getPhase(sp1.getVector());
        final double phase2 = c.getPhase(sp2.getVector());
        final Vector3D pv = c.getPointAt(phase1 + (phase2 - phase1) * fraction);
        final S2Point p = new S2Point(pv);

        final GeodeticPoint point = new GeodeticPoint(
                0.5 * FastMath.PI - p.getPhi(),
                p.getTheta(),
                p1.getAltitude() + (p2.getAltitude() + p1.getAltitude()) * fraction);
        return body.transform(point);
    }

    private Vector3D greatCircleVelocity(final GeodeticPoint p1, final GeodeticPoint p2, final double fraction) {
        final S2Point sp1 = new S2Point(p1.getLongitude(), 0.5 * FastMath.PI - p1.getLatitude());
        final S2Point sp2 = new S2Point(p2.getLongitude(), 0.5 * FastMath.PI - p2.getLatitude());

        final Circle c = new Circle(sp1, sp2, 1e-10);
        final double phase1 = c.getPhase(sp1.getVector());
        final double phase2 = c.getPhase(sp2.getVector());

        // compute the point at the fraction
        final Vector3D pv = c.getPointAt(phase1 + (phase2 - phase1) * fraction);
        final S2Point p = new S2Point(pv);

        final GeodeticPoint point = new GeodeticPoint(
                0.5 * FastMath.PI - p.getPhi(),
                p.getTheta(),
                p1.getAltitude() + (p2.getAltitude() + p1.getAltitude()) * fraction);

        // compute the point 1 second later
        final double oneSecondPhase = (phase2 - phase1) / 3600.; // each segment is 1 hour long
        final Vector3D pvPrime = c.getPointAt(phase1 + (phase2 - phase1) * fraction + oneSecondPhase);
        final S2Point pPrime = new S2Point(pvPrime);

        final GeodeticPoint pointPrime = new GeodeticPoint(
                0.5 * FastMath.PI - pPrime.getPhi(),
                pPrime.getTheta(),
                p1.getAltitude() + (p2.getAltitude() + p1.getAltitude()) * (fraction + 1. / 3600.));
        return body.transform(pointPrime).subtract(body.transform(point));
    }
}
