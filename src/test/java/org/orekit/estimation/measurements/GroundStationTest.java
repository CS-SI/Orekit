/* Copyright 2002-2026 CS GROUP
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
package org.orekit.estimation.measurements;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hipparchus.analysis.UnivariateVectorFunction;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.clocks.QuadraticClockModel;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GroundStationTest {

    @Test
    void testMoon() {
        // GIVEN
        EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Frame moonFixed = CelestialBodyFactory.getMoon().getBodyOrientedFrame();
        final GeodeticPoint geodeticPoint = new GeodeticPoint(1., 2., 3.);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(new OneAxisEllipsoid(Constants.MOON_EQUATORIAL_RADIUS,
                0., moonFixed), geodeticPoint, "");
        // WHEN & THEN
        Assertions.assertDoesNotThrow(() -> new GroundStation(topocentricFrame));
    }

    @Test
    void getPVCoordinatesProvider() {
        // GIVEN
        EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Frame ecef = FramesFactory.getGTOD(true);
        final GeodeticPoint geodeticPoint = new GeodeticPoint(1., 2., 3.);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, ecef), geodeticPoint, "");
        final GroundStation station = new GroundStation(topocentricFrame);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        for (final ParameterDriver driver: selectAllDrivers(station)) {
            driver.setReferenceDate(date);
        }
        station.getEastOffsetDriver().setValue(1);
        station.getNorthOffsetDriver().setValue(-1);
        station.getZenithOffsetDriver().setValue(2);
        final PVCoordinatesProvider pvCoordinatesProvider = station.getPVCoordinatesProvider();
        // WHEN
        final PVCoordinates actualPV = pvCoordinatesProvider.getPVCoordinates(date, ecef);
        // THEN
        final Vector3D expectedPosition = station.getOffsetToInertial(ecef, date, true).transformPosition(Vector3D.ZERO);
        assertArrayEquals(expectedPosition.toArray(), actualPV.getPosition().toArray(), 1e-8);
        assertEquals(0., actualPV.getVelocity().getNorm2());
    }

    @Test
    void getPVCoordinatesProviderGetPosition() {
        // GIVEN
        EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Frame ecef = FramesFactory.getGTOD(true);
        final GeodeticPoint geodeticPoint = new GeodeticPoint(1., 2., 3.);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, ecef), geodeticPoint, "");
        final GroundStation station = new GroundStation(topocentricFrame);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        for (final ParameterDriver driver: selectAllDrivers(station)) {
            driver.setReferenceDate(date);
        }
        station.getEastOffsetDriver().setValue(1);
        station.getNorthOffsetDriver().setValue(-1);
        station.getZenithOffsetDriver().setValue(2);
        final PVCoordinatesProvider pvCoordinatesProvider = station.getPVCoordinatesProvider();
        final Frame eci = FramesFactory.getEME2000();
        // WHEN
        final Vector3D actualPosition = pvCoordinatesProvider.getPosition(date, eci);
        // THEN
        final PVCoordinates pvCoordinates = pvCoordinatesProvider.getPVCoordinates(date, eci);
        assertEquals(pvCoordinates.getPosition(), actualPosition);
    }

    @Test
    void getPVCoordinatesProviderGetVelocity() {
        // GIVEN
        EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Frame ecef = FramesFactory.getGTOD(true);
        final GeodeticPoint geodeticPoint = new GeodeticPoint(1., 2., 3.);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, ecef), geodeticPoint, "");
        final GroundStation station = new GroundStation(topocentricFrame);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        for (final ParameterDriver driver: selectAllDrivers(station)) {
            driver.setReferenceDate(date);
        }
        station.getEastOffsetDriver().setValue(1);
        station.getNorthOffsetDriver().setValue(-1);
        station.getZenithOffsetDriver().setValue(2);
        final PVCoordinatesProvider pvCoordinatesProvider = station.getPVCoordinatesProvider();
        final Frame eci = FramesFactory.getEME2000();
        // WHEN
        final Vector3D actualVelocity = pvCoordinatesProvider.getVelocity(date, eci);
        // THEN
        final PVCoordinates pvCoordinates = pvCoordinatesProvider.getPVCoordinates(date, eci);
        assertArrayEquals(pvCoordinates.getVelocity().toArray(), actualVelocity.toArray(), 1e-4);
    }

    @Test
    void getFieldPVCoordinatesProvider() {
        // GIVEN
        EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Frame ecef = FramesFactory.getGTOD(true);
        final GeodeticPoint geodeticPoint = new GeodeticPoint(1., 2., 3.);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, ecef), geodeticPoint, "");
        final GroundStation station = new GroundStation(topocentricFrame);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        for (final ParameterDriver driver: selectAllDrivers(station)) {
            driver.setReferenceDate(date);
        }
        station.getClockBiasDriver().setValue(-0.1);
        station.getEastOffsetDriver().setValue(1);
        station.getNorthOffsetDriver().setValue(-1);
        station.getZenithOffsetDriver().setValue(2);
        final int freeParams = 1;
        final FieldPVCoordinatesProvider<Gradient> pvCoordinatesProvider = station.getFieldPVCoordinatesProvider(freeParams,
                new HashMap<>());
        final Frame eci = FramesFactory.getEME2000();
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(GradientField.getField(freeParams), date)
                .shiftedBy(new Gradient(0., 1.));
        // WHEN
        final FieldPVCoordinates<Gradient> pvCoordinates = pvCoordinatesProvider.getPVCoordinates(fieldDate, eci);
        // THEN
        final FieldVector3D<Gradient> position = pvCoordinatesProvider.getPosition(fieldDate, eci);
        assertEquals(pvCoordinates.getPosition(), position);
        final PVCoordinates nonFieldPV = station.getPVCoordinatesProvider().getPVCoordinates(date, eci);
        assertArrayEquals(pvCoordinates.getVelocity().toVector3D().toArray(), nonFieldPV.getVelocity().toArray(), 1e-12);
    }

    @Test
    void testGetOffsetToInertial() {
        // GIVEN
        EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Frame ecef = FramesFactory.getGTOD(true);
        final GeodeticPoint geodeticPoint = new GeodeticPoint(1., 2., 3.);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, ecef), geodeticPoint, "");
        final GroundStation station = new GroundStation(topocentricFrame);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        for (final ParameterDriver driver: selectAllDrivers(station)) {
            driver.setReferenceDate(date);
        }
        station.getEastOffsetDriver().setValue(1);
        station.getNorthOffsetDriver().setValue(-1);
        station.getZenithOffsetDriver().setValue(2);
        final Frame frame = FramesFactory.getEME2000();
        // WHEN
        final FieldTransform<Gradient> fieldTransform = station.getOffsetToInertial(frame, date, 0, new HashMap<>());
        // THEN
        final Transform transform = station.getOffsetToInertial(frame, date, true);
        assertArrayEquals(transform.getTranslation().toArray(), fieldTransform.getTranslation().toVector3D().toArray(), 1e-7);
        assertEquals(0., Rotation.distance(transform.getRotation(), fieldTransform.getRotation().toRotation()), 1e-15);
        assertArrayEquals(transform.getRotationRate().toArray(), fieldTransform.getRotationRate().toVector3D().toArray(), 1e-12);
        assertArrayEquals(transform.getRotationAcceleration().toArray(), fieldTransform.getRotationAcceleration().toVector3D().toArray(), 1e-20);
    }

    @Test
    void testClockOffsetValues() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // change one station clock
        final TopocentricFrame base  = context.stations.getFirst().getBaseFrame();
        final BodyShape parent       = base.getParentShape();
        final String changedSuffix   = "-changed";
        final QuadraticClockModel quadraticClock = new QuadraticClockModel(context.initialOrbit.getDate(), 3.0e-9, 2.0e-9, 1.0e-9);
        final GroundStation changed  = new GroundStation(new TopocentricFrame(parent, base.getPoint(),
                                                                              base.getName() + changedSuffix),
                                                         quadraticClock);

        final AbsoluteDate shiftedDate = context.initialOrbit.getDate().shiftedBy(60.0); // one minute
        final double bias  = changed.getOffsetValue(shiftedDate);
        final double drift = changed.getOffsetRate(shiftedDate);

        assertEquals(3.723e-6, bias, 1e-10);
        assertEquals(1.22e-7, drift, 1e-10);

    }

    @Test
    void testClockOffsetCartesianDerivativesOctantPxPyPz() {
        double relativeTolerancePositionValue      =  2.4e-15;
        double relativeTolerancePositionDerivative =  3.7e-10;
        double relativeToleranceVelocityValue      =  4.0e-15;
        double relativeToleranceVelocityDerivative =  3.7e-10;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetAngularDerivativesOctantPxPyPz() {
        double toleranceRotationValue              =  1.9e-15;
        double toleranceRotationDerivative         =  4.9e-15;
        double toleranceRotationRateValue          =  1.8e-19;
        double toleranceRotationRateDerivative     =  7.8e-19;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetCartesianDerivativesOctantPxPyMz() {
        double relativeTolerancePositionValue      =  1.7e-15;
        double relativeTolerancePositionDerivative =  2.6e-10;
        double relativeToleranceVelocityValue      =  3.2e-15;
        double relativeToleranceVelocityDerivative =  2.6e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetAngularDerivativesOctantPxPyMz() {
        double toleranceRotationValue              =  1.8e-15;
        double toleranceRotationDerivative         =  5.0e-15;
        double toleranceRotationRateValue          =  1.6e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetCartesianDerivativesOctantPxMyPz() {
        double relativeTolerancePositionValue      =  2.6e-15;
        double relativeTolerancePositionDerivative =  3.1e-10;
        double relativeToleranceVelocityValue      =  3.3e-15;
        double relativeToleranceVelocityDerivative =  3.6e-10;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetAngularDerivativesOctantPxMyPz() {
        double toleranceRotationValue              =  1.7e-15;
        double toleranceRotationDerivative         =  4.9e-15;
        double toleranceRotationRateValue          =  6.5e-19;
        double toleranceRotationRateDerivative     =  6.5e-19;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetCartesianDerivativesOctantPxMyMz() {
        double relativeTolerancePositionValue      =  1.7e-15;
        double relativeTolerancePositionDerivative =  1.7e-10;
        double relativeToleranceVelocityValue      =  3.2e-15;
        double relativeToleranceVelocityDerivative =  2.5e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetAngularDerivativesOctantPxMyMz() {
        double toleranceRotationValue              =  1.7e-15;
        double toleranceRotationDerivative         =  5.0e-15;
        double toleranceRotationRateValue          =  1.4e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetCartesianDerivativesOctantMxPyPz() {
        double relativeTolerancePositionValue      =  2.8e-15;
        double relativeTolerancePositionDerivative =  3.4e-10;
        double relativeToleranceVelocityValue      =  3.1e-15;
        double relativeToleranceVelocityDerivative =  2.3e-10;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetAngularDerivativesOctantMxPyPz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  5.2e-15;
        double toleranceRotationRateValue          =  1.7e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetCartesianDerivativesOctantMxMyPz() {
        double relativeTolerancePositionValue      =  1.8e-15;
        double relativeTolerancePositionDerivative =  2.7e-10;
        double relativeToleranceVelocityValue      =  3.4e-15;
        double relativeToleranceVelocityDerivative =  2.6e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetAngularDerivativesOctantMxMyPz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  4.9e-15;
        double toleranceRotationRateValue          =  1.6e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetCartesianDerivativesNearPole() {
        double relativeTolerancePositionValue      =  2.8e-15;
        double relativeTolerancePositionDerivative =  4.4e-04;
        double relativeToleranceVelocityValue      =  1.3e-13;
        double relativeToleranceVelocityDerivative =  8.4e-09;
        doTestCartesianDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testClockOffsetAngularDerivativesNearPole() {
        double toleranceRotationValue              =  2.0e-15;
        double toleranceRotationDerivative         =  5.7e-15;
        double toleranceRotationRateValue          =  2.2e-19;
        double toleranceRotationRateDerivative     =  8.0e-19;
        doTestAngularDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                   ".*-clock-.*");
    }

    @Test
    void testStationOffsetCartesianDerivativesOctantPxPyPz() {
        double relativeTolerancePositionValue      =  2.3e-15;
        double relativeTolerancePositionDerivative =  1.6e-10;
        double relativeToleranceVelocityValue      =  3.3e-15;
        double relativeToleranceVelocityDerivative =  2.9e-10;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetAngularDerivativesOctantPxPyPz() {
        double toleranceRotationValue              =  1.9e-15;
        double toleranceRotationDerivative         =  3.3e-18;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  1.4e-21;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetCartesianDerivativesOctantPxPyMz() {
        double relativeTolerancePositionValue      =  1.4e-15;
        double relativeTolerancePositionDerivative =  7.3e-11;
        double relativeToleranceVelocityValue      =  2.8e-15;
        double relativeToleranceVelocityDerivative =  1.8e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetAngularDerivativesOctantPxPyMz() {
        double toleranceRotationValue            =  1.7e-15;
        double toleranceRotationDerivative       =  3.7e-18;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  4.6e-22;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetCartesianDerivativesOctantPxMyPz() {
        double relativeTolerancePositionValue      =  2.4e-15;
        double relativeTolerancePositionDerivative =  1.5e-10;
        double relativeToleranceVelocityValue      =  3.5e-15;
        double relativeToleranceVelocityDerivative =  3.0e-10;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetAngularDerivativesOctantPxMyPz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  3.6e-18;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  9.7e-22;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetCartesianDerivativesOctantPxMyMz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  6.3e-11;
        double relativeToleranceVelocityValue      =  2.7e-15;
        double relativeToleranceVelocityDerivative =  1.6e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetAngularDerivativesOctantPxMyMz() {
        double toleranceRotationValue            =  1.6e-15;
        double toleranceRotationDerivative       =  4.3e-18;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  3.8e-22;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetCartesianDerivativesOctantMxPyPz() {
        double relativeTolerancePositionValue      =  2.6e-15;
        double relativeTolerancePositionDerivative =  1.3e-10;
        double relativeToleranceVelocityValue      =  2.6e-15;
        double relativeToleranceVelocityDerivative = 1.9e-10;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetAngularDerivativesOctantMxPyPz() {
        double toleranceRotationValue            =  1.5e-15;
        double toleranceRotationDerivative       =  3.5e-18;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  1.1e-21;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetCartesianDerivativesOctantMxMyPz() {
        double relativeTolerancePositionValue      =  1.9e-15;
        double relativeTolerancePositionDerivative =  1.2e-10;
        double relativeToleranceVelocityValue      =  3.2e-15;
        double relativeToleranceVelocityDerivative =  2.0e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetAngularDerivativesOctantMxMyPz() {
        double toleranceRotationValue            =  1.6e-15;
        double toleranceRotationDerivative       =  4.1e-18;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  8.7e-22;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetCartesianDerivativesNearPole() {
        double relativeTolerancePositionValue      =  3.0e-15;
        double relativeTolerancePositionDerivative =  2e-9;
        double relativeToleranceVelocityValue      =  7.5e-14;
        double relativeToleranceVelocityDerivative =  1.1e-5;
        doTestCartesianDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    void testStationOffsetAngularDerivativesNearPole() {
        double toleranceRotationValue              =  3.9e-15;
        double toleranceRotationDerivative         =  8.1e-02; // near pole, the East and North directions are singular
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  1.5e-21;
        doTestAngularDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    private void doTestCartesianDerivatives(double latitude, double longitude, double altitude, double stepFactor,
                                            double relativeTolerancePositionValue, double relativeTolerancePositionDerivative,
                                            double relativeToleranceVelocityValue, double relativeToleranceVelocityDerivative,
                                            String... parameterPattern) {
        Utils.setDataRoot("regular-data");
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date0 = date.shiftedBy(50000);
        final OneAxisEllipsoid earth =
                        new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                             Constants.WGS84_EARTH_FLATTENING,
                                             FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final GroundStation station = new GroundStation(new TopocentricFrame(earth,
                                                                             new GeodeticPoint(latitude, longitude, altitude),
                                                                             "dummy"));
        final GradientField gradientField = GradientField.getField(parameterPattern.length);
        ParameterDriver[] selectedDrivers = new ParameterDriver[parameterPattern.length];
        UnivariateDifferentiableVectorFunction[] dFCartesian = new UnivariateDifferentiableVectorFunction[parameterPattern.length];
        final ParameterDriver[] allDrivers = selectAllDrivers(station);
        for (ParameterDriver driver : allDrivers) {
            driver.setReferenceDate(date0);
        }
        Map<String, Integer> indices = new HashMap<>();
        for (int k = 0; k < dFCartesian.length; ++k) {
            for (final ParameterDriver allDriver : allDrivers) {
                if (allDriver.getName().matches(parameterPattern[k])) {
                    selectedDrivers[k] = allDriver;
                    dFCartesian[k] = differentiatedStationPV(station, eme2000, date, selectedDrivers[k], stepFactor);
                    indices.put(selectedDrivers[k].getNameSpan(date0), k);
                }
            }
        }

        RandomGenerator generator = new Well19937a(0x084d58a19c498a54L);

        double maxPositionValueRelativeError      = 0;
        double maxPositionDerivativeRelativeError = 0;
        double maxVelocityValueRelativeError      = 0;
        double maxVelocityDerivativeRelativeError = 0;
        for (int i = 0; i < 1000; ++i) {

            // randomly change one parameter
            ParameterDriver changed = allDrivers[generator.nextInt(allDrivers.length)];
            changed.setNormalizedValue(2 * generator.nextDouble() - 1);

            // transform to check
            FieldTransform<Gradient> t = station.getOffsetToInertial(eme2000, date, parameterPattern.length, indices);
            FieldPVCoordinates<Gradient> pv = t.transformPVCoordinates(FieldPVCoordinates.getZero(gradientField));
            for (int k = 0; k < dFCartesian.length; ++k) {

                // reference values and derivatives computed using finite differences
                Gradient[] refCartesian = dFCartesian[k].value(Gradient.variable(1, 0, selectedDrivers[k].getValue()));

                // position
                final Vector3D refP = new Vector3D(refCartesian[0].getValue(),
                                                   refCartesian[1].getValue(),
                                                   refCartesian[2].getValue());
                final Vector3D resP = new Vector3D(pv.getPosition().getX().getValue(),
                                                   pv.getPosition().getY().getValue(),
                                                   pv.getPosition().getZ().getValue());
                maxPositionValueRelativeError =
                                FastMath.max(maxPositionValueRelativeError, Vector3D.distance(refP, resP) / refP.getNorm());
                final Vector3D refPD = new Vector3D(refCartesian[0].getPartialDerivative(0),
                                                    refCartesian[1].getPartialDerivative(0),
                                                    refCartesian[2].getPartialDerivative(0));
                final Vector3D resPD = new Vector3D(pv.getPosition().getX().getPartialDerivative(k),
                                                    pv.getPosition().getY().getPartialDerivative(k),
                                                    pv.getPosition().getZ().getPartialDerivative(k));
                maxPositionDerivativeRelativeError =
                                FastMath.max(maxPositionDerivativeRelativeError, Vector3D.distance(refPD, resPD) / refPD.getNorm());

                // velocity
                final Vector3D refV = new Vector3D(refCartesian[3].getValue(),
                                                   refCartesian[4].getValue(),
                                                   refCartesian[5].getValue());
                final Vector3D resV = new Vector3D(pv.getVelocity().getX().getValue(),
                                                   pv.getVelocity().getY().getValue(),
                                                   pv.getVelocity().getZ().getValue());
                maxVelocityValueRelativeError =
                                FastMath.max(maxVelocityValueRelativeError, Vector3D.distance(refV, resV) / refV.getNorm());
                final Vector3D refVD = new Vector3D(refCartesian[3].getPartialDerivative(0),
                                                    refCartesian[4].getPartialDerivative(0),
                                                    refCartesian[5].getPartialDerivative(0));
                final Vector3D resVD = new Vector3D(pv.getVelocity().getX().getPartialDerivative(k),
                                                    pv.getVelocity().getY().getPartialDerivative(k),
                                                    pv.getVelocity().getZ().getPartialDerivative(k));
                maxVelocityDerivativeRelativeError =
                                FastMath.max(maxVelocityDerivativeRelativeError, Vector3D.distance(refVD, resVD) / refVD.getNorm());

            }

        }

        if (maxPositionValueRelativeError      > relativeTolerancePositionValue      ||
            maxPositionDerivativeRelativeError > relativeTolerancePositionDerivative ||
            maxVelocityValueRelativeError      > relativeToleranceVelocityValue      ||
            maxVelocityDerivativeRelativeError > relativeToleranceVelocityDerivative) {
            print("relativeTolerancePositionValue",      maxPositionValueRelativeError);
            print("relativeTolerancePositionDerivative", maxPositionDerivativeRelativeError);
            print("relativeToleranceVelocityValue",      maxVelocityValueRelativeError);
            print("relativeToleranceVelocityDerivative", maxVelocityDerivativeRelativeError);
        }
        assertEquals(0.0, maxPositionValueRelativeError,      relativeTolerancePositionValue);
        assertEquals(0.0, maxPositionDerivativeRelativeError, relativeTolerancePositionDerivative);
        assertEquals(0.0, maxVelocityValueRelativeError,      relativeToleranceVelocityValue);
        assertEquals(0.0, maxVelocityDerivativeRelativeError, relativeToleranceVelocityDerivative);

    }

    private void doTestAngularDerivatives(double latitude, double longitude, double altitude, double stepFactor,
                                          double toleranceRotationValue,     double toleranceRotationDerivative,
                                          double toleranceRotationRateValue, double toleranceRotationRateDerivative,
                                          String... parameterPattern) {
        Utils.setDataRoot("regular-data");
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date0 = date.shiftedBy(50000);
        final OneAxisEllipsoid earth =
                        new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                             Constants.WGS84_EARTH_FLATTENING,
                                             FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final GroundStation station = new GroundStation(new TopocentricFrame(earth,
                                                                             new GeodeticPoint(latitude, longitude, altitude),
                                                                             "dummy"));
        ParameterDriver[] selectedDrivers = new ParameterDriver[parameterPattern.length];
        UnivariateDifferentiableVectorFunction[] dFAngular   = new UnivariateDifferentiableVectorFunction[parameterPattern.length];
        final ParameterDriver[] allDrivers = selectAllDrivers(station);
        for (ParameterDriver driver : allDrivers) {
            driver.setReferenceDate(date0);
        }
        Map<String, Integer> indices = new HashMap<>();
        for (int k = 0; k < dFAngular.length; ++k) {
            for (final ParameterDriver allDriver : allDrivers) {
                if (allDriver.getName().matches(parameterPattern[k])) {
                    selectedDrivers[k] = allDriver;
                    dFAngular[k] = differentiatedTransformAngular(station, eme2000, date, selectedDrivers[k], stepFactor);
                    indices.put(selectedDrivers[k].getNameSpan(date0), k);
                }
            }
        }
        RandomGenerator generator = new Well19937a(0xa01a1d8fe5d80af7L);

        double maxRotationValueError          = 0;
        double maxRotationDerivativeError     = 0;
        double maxRotationRateValueError      = 0;
        double maxRotationRateDerivativeError = 0;
        for (int i = 0; i < 1000; ++i) {

            // randomly change one parameter
            ParameterDriver changed = allDrivers[generator.nextInt(allDrivers.length)];
            changed.setNormalizedValue(2 * generator.nextDouble() - 1);

            // transform to check
            FieldTransform<Gradient> t = station.getOffsetToInertial(eme2000, date, parameterPattern.length, indices);
            for (int k = 0; k < dFAngular.length; ++k) {

                // reference values and derivatives computed using finite differences
                Gradient[] refAngular = dFAngular[k].value(Gradient.variable(1, 0, selectedDrivers[k].getValue()));

                // rotation
                final Rotation refQ = new Rotation(refAngular[0].getValue(),
                                                   refAngular[1].getValue(),
                                                   refAngular[2].getValue(),
                                                   refAngular[3].getValue(),
                                                   true);
                final Rotation resQ = t.getRotation().toRotation();
                maxRotationValueError      = FastMath.max(maxRotationValueError, Rotation.distance(refQ, resQ));
                double sign = FastMath.copySign(1.0,
                                                refAngular[0].getValue() * t.getRotation().getQ0().getValue() +
                                                refAngular[1].getValue() * t.getRotation().getQ1().getValue() +
                                                refAngular[2].getValue() * t.getRotation().getQ2().getValue() +
                                                refAngular[3].getValue() * t.getRotation().getQ3().getValue());
                maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError,
                                                          FastMath.abs(sign * refAngular[0].getPartialDerivative(0) -
                                                                       t.getRotation().getQ0().getPartialDerivative(k)));
                maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError,
                                                          FastMath.abs(sign * refAngular[1].getPartialDerivative(0) -
                                                                       t.getRotation().getQ1().getPartialDerivative(k)));
                maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError,
                                                          FastMath.abs(sign * refAngular[2].getPartialDerivative(0) -
                                                                       t.getRotation().getQ2().getPartialDerivative(k)));
                maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError,
                                                          FastMath.abs(sign * refAngular[3].getPartialDerivative(0) -
                                                                       t.getRotation().getQ3().getPartialDerivative(k)));

                // rotation rate
                final Vector3D refRate  = new Vector3D(refAngular[4].getValue(), refAngular[5].getValue(), refAngular[6].getValue());
                final Vector3D resRate  = t.getRotationRate().toVector3D();
                final Vector3D refRateD = new Vector3D(refAngular[4].getPartialDerivative(0),
                                                       refAngular[5].getPartialDerivative(0),
                                                       refAngular[6].getPartialDerivative(0));
                final Vector3D resRateD = new Vector3D(t.getRotationRate().getX().getPartialDerivative(k),
                                                       t.getRotationRate().getY().getPartialDerivative(k),
                                                       t.getRotationRate().getZ().getPartialDerivative(k));
                maxRotationRateValueError      = FastMath.max(maxRotationRateValueError, Vector3D.distance(refRate, resRate));
                maxRotationRateDerivativeError = FastMath.max(maxRotationRateDerivativeError, Vector3D.distance(refRateD, resRateD));

            }

        }

        if (maxRotationValueError          > toleranceRotationValue          ||
            maxRotationDerivativeError     > toleranceRotationDerivative     ||
            maxRotationRateValueError      > toleranceRotationRateValue      ||
            maxRotationRateDerivativeError > toleranceRotationRateDerivative) {
            print("toleranceRotationValue",          maxRotationValueError);
            print("toleranceRotationDerivative",     maxRotationDerivativeError);
            print("toleranceRotationRateValue",      maxRotationRateValueError);
            print("toleranceRotationRateDerivative", maxRotationRateDerivativeError);
        }
        assertEquals(0.0, maxRotationValueError,           toleranceRotationValue);
        assertEquals(0.0, maxRotationDerivativeError,      toleranceRotationDerivative);
        assertEquals(0.0, maxRotationRateValueError,       toleranceRotationRateValue);
        assertEquals(0.0, maxRotationRateDerivativeError,  toleranceRotationRateDerivative);

    }
    private void print(String name, double v) {
        if (v < Precision.SAFE_MIN) {
            System.out.format(Locale.US, "            double %-35s =  Precision.SAFE_MIN;%n", name);
        } else {
            double s = FastMath.pow(10.0, 1 - FastMath.floor(FastMath.log(v) / FastMath.log(10.0)));
            System.out.format(Locale.US, "            double %-35s = %8.1e;%n",
                              name, FastMath.ceil(s * v) / s);
        }
    }

    private UnivariateDifferentiableVectorFunction differentiatedStationPV(final GroundStation station,
                                                                           final Frame eme2000,
                                                                           final AbsoluteDate date,
                                                                           final ParameterDriver driver,
                                                                           final double stepFactor) {

        final FiniteDifferencesDifferentiator differentiator =
                        new FiniteDifferencesDifferentiator(5, stepFactor * driver.getScale());

        return differentiator.differentiate((UnivariateVectorFunction) x -> {
            final double[] result = new double[6];
            try {
                final double previouspI = driver.getValue(date);
                driver.setValue(x, new AbsoluteDate());
                Transform t = station.getOffsetToInertial(eme2000, date, false);
                driver.setValue(previouspI, date);
                PVCoordinates stationPV = t.transformPVCoordinates(PVCoordinates.ZERO);
                result[ 0] = stationPV.getPosition().getX();
                result[ 1] = stationPV.getPosition().getY();
                result[ 2] = stationPV.getPosition().getZ();
                result[ 3] = stationPV.getVelocity().getX();
                result[ 4] = stationPV.getVelocity().getY();
                result[ 5] = stationPV.getVelocity().getZ();
            } catch (OrekitException oe) {
                Assertions.fail(oe.getLocalizedMessage());
            }
            return result;
        });
    }

    private UnivariateDifferentiableVectorFunction differentiatedTransformAngular(final GroundStation station,
                                                                                  final Frame eme2000,
                                                                                  final AbsoluteDate date,
                                                                                  final ParameterDriver driver,
                                                                                  final double stepFactor) {

        final FiniteDifferencesDifferentiator differentiator =
                        new FiniteDifferencesDifferentiator(5, stepFactor * driver.getScale());

        return differentiator.differentiate(new UnivariateVectorFunction() {
            private double previous0 = Double.NaN;
            private double previous1 = Double.NaN;
            private double previous2 = Double.NaN;
            private double previous3 = Double.NaN;
            @Override
            public double[] value(double x) {
                final double[] result = new double[7];
                try {
                    final double previouspI = driver.getValue(date);
                    driver.setValue(x, date);
                    Transform t = station.getOffsetToInertial(eme2000, date, false);
                    driver.setValue(previouspI, date);
                    final double sign;
                    if (Double.isNaN(previous0)) {
                        sign = 1;
                    } else {
                        sign = FastMath.copySign(1.0,
                                                 previous0 * t.getRotation().getQ0() +
                                                 previous1 * t.getRotation().getQ1() +
                                                 previous2 * t.getRotation().getQ2() +
                                                 previous3 * t.getRotation().getQ3());
                    }
                    previous0 = sign * t.getRotation().getQ0();
                    previous1 = sign * t.getRotation().getQ1();
                    previous2 = sign * t.getRotation().getQ2();
                    previous3 = sign * t.getRotation().getQ3();
                    result[0] = previous0;
                    result[1] = previous1;
                    result[2] = previous2;
                    result[3] = previous3;
                    result[4] = t.getRotationRate().getX();
                    result[5] = t.getRotationRate().getY();
                    result[6] = t.getRotationRate().getZ();
                } catch (OrekitException oe) {
                    Assertions.fail(oe.getLocalizedMessage());
                }
                return result;
            }
        });
    }

    private ParameterDriver[] selectAllDrivers(final GroundStation station) {
        return new ParameterDriver[] {
            station.getClockBiasDriver(),
            station.getEastOffsetDriver(),
            station.getNorthOffsetDriver(),
            station.getZenithOffsetDriver()
        };
    }

}

