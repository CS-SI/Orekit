/* Copyright 2022-2026 Romain Serra
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
package org.orekit.utils;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class GeodeticExtendedPositionProviderTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testConstructor() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final GeodeticPoint expectedGeodeticPoint = new GeodeticPoint(1., 2., 3.);
        // WHEN
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, expectedGeodeticPoint);
        // THEN
        assertEquals(bodyShape, positionProvider.getBodyShape());
        final GeodeticPoint actualPoint = positionProvider.getGeodeticPoint();
        assertEquals(expectedGeodeticPoint, actualPoint);
    }

    @Test
    void testGetter() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final GeodeticPoint expectedGeodeticPoint = new GeodeticPoint(1., 2., 3.);
        final Vector3D position = bodyShape.transform(expectedGeodeticPoint);
        // WHEN
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, position);
        final GeodeticPoint actualPoint = positionProvider.getGeodeticPoint();
        // THEN
        assertEquals(position, positionProvider.getCartesianPoint());
        assertEquals(bodyShape, positionProvider.getBodyShape());
        assertEquals(expectedGeodeticPoint.getAltitude(), actualPoint.getAltitude(), 1e-8);
        assertEquals(expectedGeodeticPoint.getLatitude(), actualPoint.getLatitude());
        assertEquals(expectedGeodeticPoint.getLongitude(), actualPoint.getLongitude());
    }

    @Test
    void testGetPVCoordinates() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Vector3D cartesianPoint = new Vector3D(1e4, 1e5, -1e3);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, cartesianPoint);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final PVCoordinates pvCoordinates = positionProvider.getPVCoordinates(date, bodyShape.getBodyFrame());
        // THEN
        assertEquals(cartesianPoint, pvCoordinates.getPosition());
        assertEquals(Vector3D.ZERO, pvCoordinates.getVelocity());
        assertEquals(Vector3D.ZERO, pvCoordinates.getAcceleration());
    }

    @Test
    void testGetPVCoordinatesVersusTopocentricFrame() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final GeodeticPoint geodeticPoint = new GeodeticPoint(1., 2., 3.);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, geodeticPoint);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape, positionProvider.getGeodeticPoint(), "");
        // WHEN
        final PVCoordinates pvCoordinates = positionProvider.getPVCoordinates(date, topocentricFrame);
        // THEN
        final PVCoordinates expectedPV = topocentricFrame.getPVCoordinates(date, topocentricFrame);
        assertEquals(expectedPV.getVelocity(), pvCoordinates.getPosition());
        assertEquals(expectedPV.getVelocity(), pvCoordinates.getVelocity());
        assertEquals(expectedPV.getAcceleration(), pvCoordinates.getAcceleration());
    }

    @Test
    void testGetPositionBodyFrame() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Vector3D cartesianPoint = new Vector3D(1e4, 1e5, -1e3);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, cartesianPoint);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final Vector3D position = positionProvider.getPosition(date, bodyShape.getBodyFrame());
        // THEN
        assertEquals(cartesianPoint, position);
    }

    @Test
    void testGetPosition() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Vector3D cartesianPoint = new Vector3D(1e4, 1e5, -1e3);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, cartesianPoint);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        // WHEN
        final Vector3D position = positionProvider.getPosition(date, frame);
        // THEN
        final PVCoordinates pvCoordinates = positionProvider.getPVCoordinates(date, frame);
        assertEquals(pvCoordinates.getPosition(), position);
    }

    @Test
    void testGetVelocityBodyFrame() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Vector3D cartesianPoint = new Vector3D(1e4, 1e5, -1e3);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, cartesianPoint);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final Vector3D velocity = positionProvider.getVelocity(date, bodyShape.getBodyFrame());
        // THEN
        assertEquals(Vector3D.ZERO, velocity);
    }

    @Test
    void testGetVelocity() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Vector3D cartesianPoint = new Vector3D(1e4, 1e5, -1e3);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, cartesianPoint);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getGCRF();
        // WHEN
        final Vector3D velocity = positionProvider.getVelocity(date, frame);
        // THEN
        final PVCoordinates pvCoordinates = positionProvider.getPVCoordinates(date, frame);
        assertEquals(pvCoordinates.getVelocity(), velocity);
    }

    @Test
    void testFieldGetPVCoordinates() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Vector3D cartesianPoint = new Vector3D(1e4, 1e5, -1e3);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, cartesianPoint);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        // WHEN
        final FieldPVCoordinates<Binary64> pvCoordinates = positionProvider.getPVCoordinates(date, bodyShape.getBodyFrame());
        // THEN
        final FieldVector3D<Binary64> fieldZero = FieldVector3D.getZero(field);
        assertEquals(cartesianPoint, pvCoordinates.getPosition().toVector3D());
        assertEquals(fieldZero, pvCoordinates.getVelocity());
        assertEquals(fieldZero, pvCoordinates.getAcceleration());
    }

    @Test
    void testFieldGetPVCoordinatesVersusTopocentricFrame() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Vector3D cartesianPoint = new Vector3D(1e4, 1e5, -1e3);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, cartesianPoint);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape, positionProvider.getGeodeticPoint(), "");
        // WHEN
        final FieldPVCoordinates<Binary64> pvCoordinates = positionProvider.getPVCoordinates(date, topocentricFrame);
        // THEN
        final FieldPVCoordinates<Binary64> expected = topocentricFrame.getPVCoordinates(date, topocentricFrame);
        assertArrayEquals(expected.getPosition().toVector3D().toArray(), 
                pvCoordinates.getPosition().toVector3D().toArray(), 1e-7);
        assertEquals(expected.getVelocity(), pvCoordinates.getVelocity());
        assertEquals(expected.getAcceleration(), pvCoordinates.getAcceleration());
    }

    @Test
    void testFieldGetPositionBodyFrame() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Vector3D cartesianPoint = new Vector3D(1e4, 1e5, -1e3);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, cartesianPoint);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        // WHEN
        final FieldVector3D<Binary64> position = positionProvider.getPosition(date, bodyShape.getBodyFrame());
        // THEN
        assertEquals(cartesianPoint, position.toVector3D());
    }

    @Test
    void testFieldGetPosition() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Vector3D cartesianPoint = new Vector3D(1e4, 1e5, -1e3);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, cartesianPoint);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final Frame frame = FramesFactory.getGCRF();
        // WHEN
        final FieldVector3D<Binary64> position = positionProvider.getPosition(date, frame);
        // THEN
        final FieldPVCoordinates<Binary64> fieldPVCoordinates = positionProvider.getPVCoordinates(date, frame);
        assertEquals(fieldPVCoordinates.getPosition(), position);
    }

    @Test
    void testFieldGetVelocityBodyFrame() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Vector3D cartesianPoint = new Vector3D(1e4, 1e5, -1e3);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, cartesianPoint);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        // WHEN
        final FieldVector3D<Binary64> velocity = positionProvider.getVelocity(date, bodyShape.getBodyFrame());
        // THEN
        assertEquals(FieldVector3D.getZero(field), velocity);
    }

    @Test
    void testFieldGetVelocity() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Vector3D cartesianPoint = new Vector3D(1e4, 1e5, -1e3);
        final GeodeticExtendedPositionProvider positionProvider = new GeodeticExtendedPositionProvider(bodyShape, cartesianPoint);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final Frame frame = FramesFactory.getGCRF();
        // WHEN
        final FieldVector3D<Binary64> velocity = positionProvider.getVelocity(date, frame);
        // THEN
        final FieldPVCoordinates<Binary64> fieldPVCoordinates = positionProvider.getPVCoordinates(date, frame);
        assertEquals(fieldPVCoordinates.getVelocity(), velocity);
    }
}
