/* Copyright 2022-2026 Romain Serra
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Observer;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TrackingCoordinates;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParallacticRefractionModifierTest {

    @BeforeEach
    void setUp() {
        // Data root
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testConstructor() {
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        Assertions.assertEquals("parallactic refraction", modifier.getEffectName());
        Assertions.assertEquals(1.000292, modifier.getRefractionIndex());
        Assertions.assertEquals(8e3, modifier.getTroposphereAltitude());
    }

    @Test
    void testGetters() {
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        Assertions.assertEquals("parallactic refraction", modifier.getEffectName());
        Assertions.assertTrue(modifier.getParametersDrivers().isEmpty());
    }

    @Test
    void testException() {
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        final AngularRaDec angularRaDec = mock();
        when(angularRaDec.getObserver()).thenReturn(mock(Observer.class));
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = new EstimatedMeasurementBase<>(angularRaDec,
                0, 0, new SpacecraftState[0], new TimeStampedPVCoordinates[0]);
        Assertions.assertThrows(OrekitException.class, () -> modifier.modifyWithoutDerivatives(estimatedMeasurement));
        verify(angularRaDec).getObserver();
    }

    @Test
    void testZenith() {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final GeodeticPoint point = new GeodeticPoint(1., -2., 3.);
        final TrackingCoordinates coordinatesAtZenith = new TrackingCoordinates(2., MathUtils.SEMI_PI, 100e3);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(ellipsoid, point, "");
        final Vector3D topoPosition = TopocentricFrame.getTopocentricPosition(coordinatesAtZenith);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame gcrf = FramesFactory.getGCRF();
        final Transform transform = topocentricFrame.getTransformTo(gcrf, date);
        final PVCoordinates pvCoordinates = transform.transformPVCoordinates(new PVCoordinates(topoPosition, Vector3D.ZERO));
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(gcrf, date, pvCoordinates));
        final Vector3D geometricRaDec = transform.transformVector(topoPosition);
        final AngularRaDec observedMeasurement = new AngularRaDec(new GroundStation(topocentricFrame), FramesFactory.getGCRF(),
                date, new double[] {geometricRaDec.getAlpha(), geometricRaDec.getDelta()}, new double[2], new double[2], new ObservableSatellite(0));
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = new EstimatedMeasurementBase<>(observedMeasurement,
                0, 0, new SpacecraftState[] {state}, new TimeStampedPVCoordinates[0]);
        estimatedMeasurement.setEstimatedValue(geometricRaDec.getAlpha(), geometricRaDec.getDelta());
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        // WHEN
        modifier.modifyWithoutDerivatives(estimatedMeasurement);
        // THEN
        final double modifiedRightAscension = estimatedMeasurement.getEstimatedValue()[0];
        final double modifiedDeclination = estimatedMeasurement.getEstimatedValue()[1];
        Assertions.assertEquals(geometricRaDec.getAlpha(), modifiedRightAscension, 1e-10);
        Assertions.assertEquals(geometricRaDec.getDelta(), modifiedDeclination, 1e-10);
    }

    @Test
    void testAlmostInfiniteDistance() {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final GeodeticPoint point = new GeodeticPoint(1., -2., 3.);
        final double largeDistance = 1e8;
        final TrackingCoordinates coordinates = new TrackingCoordinates(2., 1., largeDistance);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(ellipsoid, point, "");
        final Vector3D topoPosition = TopocentricFrame.getTopocentricPosition(coordinates);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame gcrf = FramesFactory.getGCRF();
        final Transform transform = topocentricFrame.getTransformTo(gcrf, date);
        final PVCoordinates pvCoordinates = transform.transformPVCoordinates(new PVCoordinates(topoPosition, Vector3D.ZERO));
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(gcrf, date, pvCoordinates));
        final Vector3D geometricRaDec = transform.transformVector(topoPosition);
        final AngularRaDec observedMeasurement = new AngularRaDec(new GroundStation(topocentricFrame), FramesFactory.getICRF(),
                date, new double[] {geometricRaDec.getAlpha(), geometricRaDec.getDelta()}, new double[2], new double[2], new ObservableSatellite(0));
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = new EstimatedMeasurementBase<>(observedMeasurement,
                0, 0, new SpacecraftState[] {state}, new TimeStampedPVCoordinates[0]);
        estimatedMeasurement.setEstimatedValue(geometricRaDec.getAlpha(), geometricRaDec.getDelta());
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        // WHEN
        modifier.modifyWithoutDerivatives(estimatedMeasurement);
        // THEN
        final double modifiedRightAscension = estimatedMeasurement.getEstimatedValue()[0];
        final double modifiedDeclination = estimatedMeasurement.getEstimatedValue()[1];
        Assertions.assertEquals(geometricRaDec.getAlpha(), modifiedRightAscension, 1e-7);
        Assertions.assertEquals(geometricRaDec.getDelta(), modifiedDeclination, 1e-7);
    }

    @Test
    void testAlmostZeroTroposphere() {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final GeodeticPoint point = new GeodeticPoint(-1., 2., 3.);
        final TrackingCoordinates coordinates = new TrackingCoordinates(1., 1., 100.e3);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(ellipsoid, point, "");
        final Vector3D topoPosition = TopocentricFrame.getTopocentricPosition(coordinates);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame gcrf = FramesFactory.getGCRF();
        final Transform transform = topocentricFrame.getTransformTo(gcrf, date);
        final PVCoordinates pvCoordinates = transform.transformPVCoordinates(new PVCoordinates(topoPosition, Vector3D.ZERO));
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(gcrf, date, pvCoordinates));
        final Vector3D geometricRaDec = transform.transformVector(topoPosition);
        final AngularRaDec observedMeasurement = new AngularRaDec(new GroundStation(topocentricFrame), FramesFactory.getICRF(),
                date, new double[] {geometricRaDec.getAlpha(), geometricRaDec.getDelta()}, new double[2], new double[2], new ObservableSatellite(0));
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = new EstimatedMeasurementBase<>(observedMeasurement,
                0, 0, new SpacecraftState[] {state}, new TimeStampedPVCoordinates[0]);
        estimatedMeasurement.setEstimatedValue(geometricRaDec.getAlpha(), geometricRaDec.getDelta());
        final double smallTroposphereAltitude = 1e-10;
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier(smallTroposphereAltitude, 1.);
        // WHEN
        modifier.modifyWithoutDerivatives(estimatedMeasurement);
        // THEN
        final double modifiedRightAscension = estimatedMeasurement.getEstimatedValue()[0];
        final double modifiedDeclination = estimatedMeasurement.getEstimatedValue()[1];
        Assertions.assertEquals(geometricRaDec.getAlpha(), modifiedRightAscension, 1e-7);
        Assertions.assertEquals(geometricRaDec.getDelta(), modifiedDeclination, 1e-7);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1., 1.0})
    void testNegativeVersusPositionElevation(final double elevation) {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final GeodeticPoint point = new GeodeticPoint(1., 2., 3.);
        final TrackingCoordinates coordinates = new TrackingCoordinates(0., elevation, 42000.e3);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(ellipsoid, point, "");
        final Vector3D topoPosition = TopocentricFrame.getTopocentricPosition(coordinates);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame gcrf = FramesFactory.getGCRF();
        final Transform transform = topocentricFrame.getTransformTo(gcrf, date);
        final PVCoordinates pvCoordinates = transform.transformPVCoordinates(new PVCoordinates(topoPosition, Vector3D.ZERO));
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(gcrf, date, pvCoordinates));
        final Vector3D geometricRaDec = transform.transformVector(topoPosition);
        final AngularRaDec observedMeasurement = new AngularRaDec(new GroundStation(topocentricFrame), FramesFactory.getEME2000(),
                date, new double[] {geometricRaDec.getAlpha(), geometricRaDec.getDelta()}, new double[2], new double[2], new ObservableSatellite(0));
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = new EstimatedMeasurementBase<>(observedMeasurement,
                0, 0, new SpacecraftState[] {state}, new TimeStampedPVCoordinates[0]);
        estimatedMeasurement.setEstimatedValue(geometricRaDec.getAlpha(), geometricRaDec.getDelta());
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        // WHEN
        modifier.modifyWithoutDerivatives(estimatedMeasurement);
        // THEN
        final double modifiedRightAscension = estimatedMeasurement.getEstimatedValue()[0];
        final double modifiedDeclination = estimatedMeasurement.getEstimatedValue()[1];
        if (elevation > 0) {
            Assertions.assertNotEquals(geometricRaDec.getAlpha(), modifiedRightAscension);
            Assertions.assertNotEquals(geometricRaDec.getDelta(), modifiedDeclination);
        } else {
            Assertions.assertEquals(geometricRaDec.getAlpha(), modifiedRightAscension);
            Assertions.assertEquals(geometricRaDec.getDelta(), modifiedDeclination);
        }
    }

    @Test
    void testOrderOfMagnitude() {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final GeodeticPoint point = new GeodeticPoint(1., -2., 3.);
        final TrackingCoordinates coordinates = new TrackingCoordinates(2., FastMath.toRadians(75.), 100e3);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(ellipsoid, point, "");
        final Vector3D topoPosition = TopocentricFrame.getTopocentricPosition(coordinates);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame gcrf = FramesFactory.getGCRF();
        final Transform transform = topocentricFrame.getTransformTo(gcrf, date);
        final PVCoordinates pvCoordinates = transform.transformPVCoordinates(new PVCoordinates(topoPosition, Vector3D.ZERO));
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(gcrf, date, pvCoordinates));
        final Vector3D geometricRaDec = transform.transformVector(topoPosition);
        final AngularRaDec observedMeasurement = new AngularRaDec(new GroundStation(topocentricFrame), FramesFactory.getICRF(),
                date, new double[] {geometricRaDec.getAlpha(), geometricRaDec.getDelta()}, new double[2], new double[2], new ObservableSatellite(0));
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = new EstimatedMeasurementBase<>(observedMeasurement,
                0, 0, new SpacecraftState[] {state}, new TimeStampedPVCoordinates[0]);
        estimatedMeasurement.setEstimatedValue(geometricRaDec.getAlpha(), geometricRaDec.getDelta());
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        // WHEN
        modifier.modifyWithoutDerivatives(estimatedMeasurement);
        // THEN
        final double modifiedRightAscension = estimatedMeasurement.getEstimatedValue()[0];
        final double modifiedDeclination = estimatedMeasurement.getEstimatedValue()[1];
        Assertions.assertEquals(geometricRaDec.getAlpha(), modifiedRightAscension, FastMath.toRadians(2./3600.));
        Assertions.assertEquals(geometricRaDec.getDelta(), modifiedDeclination, FastMath.toRadians(2./3600.));
    }
}
