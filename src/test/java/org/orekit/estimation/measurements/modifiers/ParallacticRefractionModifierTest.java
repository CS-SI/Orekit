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
import org.junit.jupiter.params.provider.EnumSource;
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
import org.orekit.frames.Predefined;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TrackingCoordinates;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals("parallactic refraction", modifier.getEffectName());
        assertEquals(1.000292, modifier.getRefractionIndex());
        assertEquals(8e3, modifier.getTroposphereAltitude());
    }

    @Test
    void testGetters() {
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        assertEquals("parallactic refraction", modifier.getEffectName());
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

    @ParameterizedTest
    @EnumSource(value = Predefined.class, names = {"GCRF", "EME2000", "ICRF"})
    void testZenith(final Predefined predefined) {
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
        final AngularRaDec observedMeasurement = new AngularRaDec(new GroundStation(topocentricFrame), FramesFactory.getFrame(predefined),
                date, new double[] {geometricRaDec.getAlpha(), geometricRaDec.getDelta()}, new double[2], new double[2], new ObservableSatellite(0));
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = observedMeasurement.estimate(0, 0, new SpacecraftState[] {state});
        estimatedMeasurement.setEstimatedValue(geometricRaDec.getAlpha(), geometricRaDec.getDelta());
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        // WHEN
        modifier.modifyWithoutDerivatives(estimatedMeasurement);
        // THEN
        final double modifiedRightAscension = estimatedMeasurement.getEstimatedValue()[0];
        final double modifiedDeclination = estimatedMeasurement.getEstimatedValue()[1];
        assertEquals(estimatedMeasurement.getEstimatedValue()[0], modifiedRightAscension, 1e-10);
        assertEquals(estimatedMeasurement.getEstimatedValue()[1], modifiedDeclination, 1e-10);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1., 5., 10., 20., 30., 40., 50., 70., 80.})
    void testAlmostInfiniteDistance(final double elevation) {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final GeodeticPoint point = new GeodeticPoint(1., -2., 3.);
        final double largeDistance = 1e8;
        final TrackingCoordinates coordinates = new TrackingCoordinates(2., FastMath.toRadians(elevation), largeDistance);
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
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = observedMeasurement.estimate(0, 0, new SpacecraftState[] {state});
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        // WHEN
        modifier.modifyWithoutDerivatives(estimatedMeasurement);
        // THEN
        final double modifiedRightAscension = estimatedMeasurement.getEstimatedValue()[0];
        final double modifiedDeclination = estimatedMeasurement.getEstimatedValue()[1];
        assertEquals(estimatedMeasurement.getEstimatedValue()[0], modifiedRightAscension, 1e-7);
        assertEquals(estimatedMeasurement.getEstimatedValue()[1], modifiedDeclination, 1e-7);
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
        final Frame inertial = FramesFactory.getEME2000();
        final Transform transform = topocentricFrame.getTransformTo(inertial, date);
        final PVCoordinates pvCoordinates = transform.transformPVCoordinates(new PVCoordinates(topoPosition, Vector3D.ZERO));
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(inertial, date, pvCoordinates));
        final Vector3D geometricRaDec = transform.transformVector(topoPosition);
        final AngularRaDec observedMeasurement = new AngularRaDec(new GroundStation(topocentricFrame), FramesFactory.getICRF(),
                date, new double[] {geometricRaDec.getAlpha(), geometricRaDec.getDelta()}, new double[2], new double[2], new ObservableSatellite(0));
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = observedMeasurement.estimate(0, 0, new SpacecraftState[] {state});
        final double smallTroposphereAltitude = 1e-10;
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier(smallTroposphereAltitude, 1.);
        // WHEN
        modifier.modifyWithoutDerivatives(estimatedMeasurement);
        // THEN
        final double modifiedRightAscension = estimatedMeasurement.getEstimatedValue()[0];
        final double modifiedDeclination = estimatedMeasurement.getEstimatedValue()[1];
        assertEquals(estimatedMeasurement.getEstimatedValue()[0], modifiedRightAscension, 1e-7);
        assertEquals(estimatedMeasurement.getEstimatedValue()[1], modifiedDeclination, 1e-7);
    }

    @ParameterizedTest
    @ValueSource(doubles = {10., 20., 30., 40., 50., 70., 80.})
    void testOrderOfMagnitudeVersusElevation(final double elevationDeg) {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final GeodeticPoint point = new GeodeticPoint(1., 2., 3.);
        final TrackingCoordinates coordinates = new TrackingCoordinates(0., FastMath.toRadians(elevationDeg), 1000.e3);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(ellipsoid, point, "");
        final Vector3D topoPosition = TopocentricFrame.getTopocentricPosition(coordinates);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame gcrf = FramesFactory.getGCRF();
        final Transform transform = topocentricFrame.getTransformTo(gcrf, date);
        final PVCoordinates pvCoordinates = transform.transformPVCoordinates(new PVCoordinates(topoPosition, Vector3D.PLUS_K.scalarMultiply(7e3)));
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(gcrf, date, pvCoordinates));
        final Vector3D geometricRaDec = transform.transformVector(topoPosition);
        final AngularRaDec observedMeasurement = new AngularRaDec(new GroundStation(topocentricFrame), FramesFactory.getICRF(),
                date, new double[] {geometricRaDec.getAlpha(), geometricRaDec.getDelta()}, new double[2], new double[2], new ObservableSatellite(0));
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = observedMeasurement.estimate(0, 0, new SpacecraftState[] {state});
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        // WHEN
        modifier.modifyWithoutDerivatives(estimatedMeasurement);
        // THEN
        final double modifiedRightAscension = estimatedMeasurement.getEstimatedValue()[0];
        final double modifiedDeclination = estimatedMeasurement.getEstimatedValue()[1];
        assertEquals(estimatedMeasurement.getEstimatedValue()[0], modifiedRightAscension, FastMath.toRadians(20./3600.));
        assertEquals(estimatedMeasurement.getEstimatedValue()[1], modifiedDeclination, FastMath.toRadians(20./3600.));
    }

    @Test
    void testNegativeElevation() {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final GeodeticPoint point = new GeodeticPoint(1., 2., 3.);
        final TrackingCoordinates coordinates = new TrackingCoordinates(0., -1., 42000.e3);
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
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = observedMeasurement.estimate(0, 0, new SpacecraftState[] {state});
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        // WHEN
        modifier.modifyWithoutDerivatives(estimatedMeasurement);
        // THEN
        final double modifiedRightAscension = estimatedMeasurement.getEstimatedValue()[0];
        final double modifiedDeclination = estimatedMeasurement.getEstimatedValue()[1];
        assertEquals(estimatedMeasurement.getEstimatedValue()[0], modifiedRightAscension);
        assertEquals(estimatedMeasurement.getEstimatedValue()[1], modifiedDeclination);
    }

    @ParameterizedTest
    @ValueSource(doubles = {25., 30., 40., 50., 70., 80.})
    void testValueKaplan(final double elevation) {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final GeodeticPoint point = new GeodeticPoint(1., -2., 3.);
        final TrackingCoordinates coordinates = new TrackingCoordinates(2., FastMath.toRadians(elevation), 100e3);
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
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = observedMeasurement.estimate(0, 0, new SpacecraftState[] {state});
        final ParallacticRefractionModifier modifier = new ParallacticRefractionModifier();
        // WHEN
        modifier.modifyWithoutDerivatives(estimatedMeasurement);
        // THEN
        final double modifiedRightAscension = estimatedMeasurement.getEstimatedValue()[0];
        final double modifiedDeclination = estimatedMeasurement.getEstimatedValue()[1];
        final Vector3D originalLos = retrieveLos(estimatedMeasurement.getOriginalEstimatedValue()[0],
                estimatedMeasurement.getOriginalEstimatedValue()[1], gcrf, topocentricFrame, date);
        final Vector3D modifiedLos = retrieveLos(modifiedRightAscension, modifiedDeclination, gcrf, topocentricFrame, date);
        assertEquals(originalLos.getAlpha(), modifiedLos.getAlpha(), 1e-10);
        // Kaplan's formula: Eq. (3)
        final double s = 8e3;
        final double altitude = topocentricFrame.getParentShape().transform(state.getPosition(), state.getFrame(), state.getDate()).getAltitude();
        final double zPrime = FastMath.asin(FastMath.sin(MathUtils.SEMI_PI - originalLos.getDelta()) / 1.000292);
        final double correctedElevation = MathUtils.SEMI_PI - FastMath.atan((s / altitude) * FastMath.tan(zPrime) + (1. - s / altitude) * FastMath.tan(MathUtils.SEMI_PI - originalLos.getDelta()));
        final double expectedAbsoluteDifference = FastMath.abs(correctedElevation - originalLos.getDelta());
        assertEquals(expectedAbsoluteDifference, modifiedLos.getDelta() - originalLos.getDelta(), 1e-6);
    }

    @ParameterizedTest
    @ValueSource(doubles = {2000., 20000, 50000})
    void testSwapAberration(final double range) {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final GeodeticPoint point = new GeodeticPoint(1., -2., 3.);
        final TrackingCoordinates coordinates = new TrackingCoordinates(2., FastMath.toRadians(1.), range * 1e3);
        final TopocentricFrame topocentricFrame = new TopocentricFrame(ellipsoid, point, "");
        final Vector3D topoPosition = TopocentricFrame.getTopocentricPosition(coordinates);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame gcrf = FramesFactory.getGCRF();
        final Transform transform = topocentricFrame.getTransformTo(gcrf, date);
        final PVCoordinates pvCoordinates = transform.transformPVCoordinates(new PVCoordinates(topoPosition, Vector3D.PLUS_J.scalarMultiply(3.e3)));
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(gcrf, date, pvCoordinates));
        final Vector3D geometricRaDec = transform.transformVector(topoPosition);
        final GroundStation station = new GroundStation(topocentricFrame);
        station.getClockBiasDriver().setValue(0.01);
        final AngularRaDec observedMeasurement = new AngularRaDec(station, FramesFactory.getICRF(),
                date, new double[] {geometricRaDec.getAlpha(), geometricRaDec.getDelta()}, new double[2], new double[2], new ObservableSatellite(0));
        final EstimatedMeasurementBase<AngularRaDec> estimatedMeasurement = new EstimatedMeasurementBase<>(observedMeasurement,
                0, 0, new SpacecraftState[] {state}, new TimeStampedPVCoordinates[0]);
        estimatedMeasurement.setEstimatedValue(geometricRaDec.getAlpha(), geometricRaDec.getDelta());
        final ParallacticRefractionModifier refractionModifier = new ParallacticRefractionModifier();
        final AberrationModifier aberrationModifier = new AberrationModifier();
        final AngularRaDec raDecRefractionFirst = new AngularRaDec(station, gcrf, date, new double[] {geometricRaDec.getAlpha(),
                geometricRaDec.getDelta()}, new double[2], new double[2], new ObservableSatellite(0));
        raDecRefractionFirst.addModifier(refractionModifier);
        raDecRefractionFirst.addModifier(aberrationModifier);
        // WHEN
        final EstimatedMeasurementBase<AngularRaDec> estimated = raDecRefractionFirst.estimateWithoutDerivatives(new SpacecraftState[] {state});
        // THEN
        final AngularRaDec radecRefractionLast = new AngularRaDec(station, gcrf, date, raDecRefractionFirst.getObservedValue(),
                raDecRefractionFirst.getMeasurementQuality(), raDecRefractionFirst.getSignalTravelTimeModel(), new ObservableSatellite(0));
        radecRefractionLast.addModifier(aberrationModifier);
        radecRefractionLast.addModifier(refractionModifier);
        final EstimatedMeasurementBase<AngularRaDec> expected = radecRefractionLast.estimateWithoutDerivatives(new SpacecraftState[] {state});
        assertArrayEquals(expected.getEstimatedValue(), estimated.getEstimatedValue(), 1e-7);
    }

    private static Vector3D retrieveLos(final double ra, final double dec, final Frame frame,
                                        final TopocentricFrame topocentricFrame, final AbsoluteDate date) {
        final Vector3D raDec = new Vector3D(ra, dec);
        final StaticTransform transform = frame.getTransformTo(topocentricFrame, date);
        return transform.transformVector(raDec);
    }
}
