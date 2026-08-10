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
package org.orekit.estimation.measurements;

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EarthBasedStationTransformProviderTest {

    @BeforeAll
    static void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testGetTransform() {
        // GIVEN
        final OneAxisEllipsoid bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Frame frame = bodyShape.getFrame();
        final GeodeticPoint geodeticPoint = new GeodeticPoint(1., 2., 3.);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape, geodeticPoint, "");
        final EstimatedEarthFrameProvider estimatedEarthFrameProvider = mock();
        when(estimatedEarthFrameProvider.getTransform(any(AbsoluteDate.class))).thenReturn(Transform.IDENTITY);
        when(estimatedEarthFrameProvider.getStaticTransform(any(AbsoluteDate.class))).thenReturn(Transform.IDENTITY);
        final TransformProvider provider = new EarthBasedStationTransformProvider(frame, topocentricFrame,
                mockDriver(0.), mockDriver(0.), mockDriver(0.), estimatedEarthFrameProvider, null);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final Transform transform = provider.getTransform(date);
        // THEN
        final Transform expected = topocentricFrame.getTransformTo(frame, date);
        assertEquals(expected.getDate(), transform.getDate());
        assertArrayEquals(expected.getTranslation().toArray(), transform.getTranslation().toArray(), 1.e-9);
        assertEquals(0., Rotation.distance(expected.getRotation(), transform.getRotation()), 1.e-13);
        assertEquals(expected.getRotationRate(), transform.getRotationRate());
        assertEquals(expected.getRotationAcceleration(), transform.getRotationAcceleration());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testGetStaticTransform(final boolean isInertial) {
        // GIVEN
        final OneAxisEllipsoid bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Frame frame = isInertial ? bodyShape.getFrame() : FramesFactory.getGCRF();
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape, new GeodeticPoint(1., 2., 3.), "");
        final EstimatedEarthFrameProvider estimatedEarthFrameProvider = mock();
        when(estimatedEarthFrameProvider.getTransform(any(AbsoluteDate.class))).thenReturn(Transform.IDENTITY);
        when(estimatedEarthFrameProvider.getStaticTransform(any(AbsoluteDate.class))).thenReturn(Transform.IDENTITY);
        final TransformProvider provider = new EarthBasedStationTransformProvider(frame, topocentricFrame,
                mockDriver(10.), mockDriver(-20.), mockDriver(5.), estimatedEarthFrameProvider, null);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final StaticTransform staticTransform = provider.getStaticTransform(date);
        // THEN
        final Transform transform = provider.getTransform(date);
        assertEquals(transform.getDate(), staticTransform.getDate());
        assertEquals(transform.getTranslation(), staticTransform.getTranslation());
        assertEquals(0., Rotation.distance(transform.getRotation(), staticTransform.getRotation()), 1.e-13);
    }

    @Test
    void testGetTransformField() {
        // GIVEN
        final OneAxisEllipsoid bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Frame frame = FramesFactory.getGCRF();
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape, new GeodeticPoint(1., 2., 3.), "");
        final EstimatedEarthFrameProvider estimatedEarthFrameProvider = mock();
        final int freeParameters = 0;
        final GradientField field = GradientField.getField(freeParameters);
        final FieldAbsoluteDate<Gradient> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        when(estimatedEarthFrameProvider.getTransform(date)).thenReturn(FieldTransform.getIdentity(field));
        when(estimatedEarthFrameProvider.getTransform(any(AbsoluteDate.class))).thenReturn(Transform.IDENTITY);
        final EarthBasedStationTransformProvider provider = new EarthBasedStationTransformProvider(frame, topocentricFrame,
                mockDriver(10.), mockDriver(-20.), mockDriver(5.),
                estimatedEarthFrameProvider, null);
        // WHEN
        final FieldTransform<Gradient> fieldTransform = provider.getTransform(date);
        // THEN
        final Transform transform = provider.getTransform(date.toAbsoluteDate());
        assertEquals(fieldTransform.getDate(), transform.getDate());
        assertArrayEquals(fieldTransform.getTranslation().toVector3D().toArray(), transform.getTranslation().toArray(), 1.e-6);
        assertEquals(0., Rotation.distance(fieldTransform.getRotation().toRotation(), transform.getRotation()), 1.e-13);
        assertEquals(fieldTransform.getRotationRate().toVector3D(), transform.getRotationRate());
        assertEquals(fieldTransform.getRotationAcceleration().toVector3D(), transform.getRotationAcceleration());
    }

    @Test
    void testGetTransformGradient() {
        // GIVEN
        final OneAxisEllipsoid bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Frame frame = FramesFactory.getGCRF();
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape, new GeodeticPoint(1., 2., 3.), "");
        final EstimatedEarthFrameProvider estimatedEarthFrameProvider = mock();
        final int freeParameters = 1;
        final GradientField field = GradientField.getField(freeParameters);
        final FieldAbsoluteDate<Gradient> date = FieldAbsoluteDate.getArbitraryEpoch(field).shiftedBy(new Gradient(1., 1.));
        final Map<String, Integer> parameters = new HashMap<>();
        when(estimatedEarthFrameProvider.getTransform(date)).thenReturn(FieldTransform.getIdentity(field));
        final ParameterDriver eastOffsetDriver = mockDriverWithGradient(10., freeParameters);
        final ParameterDriver northOffsetDriver = mockDriverWithGradient(-20., freeParameters);
        final ParameterDriver zenithOffsetDriver = mockDriverWithGradient(5., freeParameters);
        final EarthBasedStationTransformProvider provider = new EarthBasedStationTransformProvider(frame, topocentricFrame,
                eastOffsetDriver, northOffsetDriver, zenithOffsetDriver, estimatedEarthFrameProvider, null);
        // WHEN
        final FieldTransform<Gradient> transform = provider.getTransform(date);
        // THEN
        final GroundStation station = new GroundStation(topocentricFrame);
        station.getEastOffsetDriver().setValue(eastOffsetDriver.getValue());
        station.getNorthOffsetDriver().setValue(northOffsetDriver.getValue());
        station.getZenithOffsetDriver().setValue(zenithOffsetDriver.getValue());
        station.getParametersDrivers().forEach(driver -> driver.setReferenceDate(AbsoluteDate.ARBITRARY_EPOCH));
        final FieldTransform<Gradient> transformGradient = station.getOffsetToInertial(frame, date, freeParameters, parameters);
        assertEquals(transform.getFieldDate(), transformGradient.getFieldDate());
        assertArrayEquals(transform.getTranslation().toVector3D().toArray(), transformGradient.getTranslation().toVector3D().toArray(),
                1e-7);
        assertEquals(0., Rotation.distance(transform.getRotation().toRotation(), transformGradient.getRotation().toRotation()), 1.e-13);
        assertEquals(transform.getRotationRate(), transformGradient.getRotationRate());
        assertEquals(transform.getRotationAcceleration(), transformGradient.getRotationAcceleration());
    }

    @Test
    void testGetStaticTransformGradient() {
        // GIVEN
        final OneAxisEllipsoid bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getGTOD(true));
        final Frame frame = FramesFactory.getGCRF();
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape, new GeodeticPoint(1., 2., 3.), "");
        final EstimatedEarthFrameProvider estimatedEarthFrameProvider = mock();
        final int freeParameters = 1;
        final GradientField field = GradientField.getField(freeParameters);
        final FieldAbsoluteDate<Gradient> date = FieldAbsoluteDate.getArbitraryEpoch(field).shiftedBy(new Gradient(1., 1.));
        final Map<String, Integer> parameters = new HashMap<>();
        when(estimatedEarthFrameProvider.getStaticTransform(date, freeParameters, parameters)).thenReturn(FieldStaticTransform.getIdentity(field));
        when(estimatedEarthFrameProvider.getStaticTransform(date)).thenReturn(FieldTransform.getIdentity(field));
        when(estimatedEarthFrameProvider.getTransform(date, field.getZero().getFreeParameters(), parameters)).thenReturn(FieldTransform.getIdentity(field));
        final ParameterDriver eastOffsetDriver = mockDriverWithGradient(10., freeParameters);
        final ParameterDriver northOffsetDriver = mockDriverWithGradient(-20., freeParameters);
        final ParameterDriver zenithOffsetDriver = mockDriverWithGradient(5., freeParameters);
        final EarthBasedStationTransformProvider provider = new EarthBasedStationTransformProvider(frame, topocentricFrame,
                eastOffsetDriver, northOffsetDriver, zenithOffsetDriver, estimatedEarthFrameProvider, null);
        // WHEN
        final FieldStaticTransform<Gradient> staticTransform = provider.getStaticTransform(date);
        // THEN
        final GroundStation station = new GroundStation(topocentricFrame);
        station.getEastOffsetDriver().setValue(eastOffsetDriver.getValue());
        station.getNorthOffsetDriver().setValue(northOffsetDriver.getValue());
        station.getZenithOffsetDriver().setValue(zenithOffsetDriver.getValue());
        station.getParametersDrivers().forEach(driver -> driver.setReferenceDate(AbsoluteDate.ARBITRARY_EPOCH));
        final FieldStaticTransform<Gradient> transformGradient = station.getOffsetToInertial(frame, date, freeParameters, parameters);
        assertEquals(staticTransform.getDate(), transformGradient.getDate());
        assertArrayEquals(staticTransform.getTranslation().toVector3D().toArray(),
                transformGradient.getTranslation().toVector3D().toArray(), 1e-7);
        assertEquals(0., Rotation.distance(staticTransform.getRotation().toRotation(), transformGradient.getRotation().toRotation()), 1.e-13);
    }

    private static ParameterDriver mockDriver(final double value) {
        final ParameterDriver driver = mock();
        when(driver.getValue()).thenReturn(value);
        when(driver.getValue(any(AbsoluteDate.class))).thenReturn(value);
        return driver;
    }

    @SuppressWarnings("unchecked")
    private static ParameterDriver mockDriverWithGradient(final double value, final int freeParameters) {
        final ParameterDriver driver = mockDriver(value);
        when(driver.getValue(any(Integer.class), any(Map.class), any(AbsoluteDate.class))).thenReturn(Gradient.constant(freeParameters, value));
        return driver;
    }
}
