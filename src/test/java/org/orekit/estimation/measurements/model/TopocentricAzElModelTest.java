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
package org.orekit.estimation.measurements.model;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.ConvergenceChecker;
import org.hipparchus.optim.FieldScalarConvergenceCheckerProvider;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.signal.FieldSignalReceptionCondition;
import org.orekit.signal.SignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TrackingCoordinates;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TopocentricAzElModelTest {

    @BeforeAll
    static void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testConstructorException() {
        final Frame frame = mock();
        when(frame.isPseudoInertial()).thenReturn(false);
        final BodyShape bodyShape = mock();
        final SignalTravelTimeModel signalTravelTimeModel = new SignalTravelTimeModel();
        assertThrows(OrekitException.class, () -> new TopocentricAzElModel(frame, bodyShape, signalTravelTimeModel));
    }

    @Test
    void testValueInstantaneousSignal() {
        // GIVEN
        final SignalTravelTimeModel instantaneousSignalModel = new SignalTravelTimeModel((iteration, previous, current) -> true,
                new FieldScalarConvergenceCheckerProvider() {
                    @Override
                    public <T extends CalculusFieldElement<T>> ConvergenceChecker<T> getChecker(Field<T> field) {
                        return ((iteration, previous, current) -> true);
                    }
                });
        final Frame frame = FramesFactory.getGCRF();
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, FramesFactory.getGTOD(true));
        final GeodeticPoint geodeticPoint = new GeodeticPoint(0, 0., 0);
        final TopocentricAzElModel measurementModel = new TopocentricAzElModel(frame, bodyShape, instantaneousSignalModel);
        final Vector3D emitterPosition = new Vector3D(1e7, 2e6, -3e6);
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(bodyShape.getBodyFrame(),
                AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates(emitterPosition));
        // WHEN
        final double[] azEl = measurementModel.value(geodeticPoint, absolutePVCoordinates.getDate(),
                absolutePVCoordinates);
        // THEN
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape, geodeticPoint, "");
        final TrackingCoordinates trackingCoordinates = topocentricFrame.getTrackingCoordinates(absolutePVCoordinates.getPosition(),
                absolutePVCoordinates.getFrame(), absolutePVCoordinates.getDate());
        assertEquals(trackingCoordinates.getAzimuth(), azEl[0], 1e-15);
        assertEquals(trackingCoordinates.getElevation(), azEl[1], 1e-15);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-2, -1, 0., 1., 2., 3.})
    void testValueAgainstRaDec(final double longitude) {
        // GIVEN
        final SignalTravelTimeModel signalTravelTimeModel = new SignalTravelTimeModel();
        final Frame frame = FramesFactory.getGCRF();
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, FramesFactory.getGTOD(true));
        final GeodeticPoint geodeticPoint = new GeodeticPoint(0, longitude, 0);
        final TopocentricAzElModel measurementModel = new TopocentricAzElModel(frame, bodyShape, signalTravelTimeModel);
        final Vector3D emitterPosition = new Vector3D(1e7, 2e6, -3e6);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(bodyShape.getBodyFrame(),
                date, new PVCoordinates(emitterPosition, Vector3D.MINUS_J.scalarMultiply(7)));
        // WHEN
        final double[] azEl = measurementModel.value(geodeticPoint, absolutePVCoordinates.getDate(),
                absolutePVCoordinates, absolutePVCoordinates.getDate());
        // THEN
        final TrackingCoordinates trackingCoordinates = new TrackingCoordinates(azEl[0], azEl[1], 1.);
        final Vector3D lineOfSightInTopocentric = TopocentricFrame.getTopocentricPosition(trackingCoordinates);
        final StaticTransform topocentricToInertial = new TopocentricFrame(bodyShape, geodeticPoint, "").getStaticTransformTo(frame, date);
        final Vector3D lineOfSightInGcrf = topocentricToInertial.transformVector(lineOfSightInTopocentric);
        final RaDecModel raDecModel = new RaDecModel(frame, signalTravelTimeModel);
        final StaticTransform staticTransform = bodyShape.getBodyFrame().getStaticTransformTo(frame, date);
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(absolutePVCoordinates.getDate(),
                staticTransform.transformPosition(bodyShape.transform(geodeticPoint)), frame);
        final double[] raDec = raDecModel.value(receptionCondition, absolutePVCoordinates, absolutePVCoordinates.getDate());
        assertEquals(lineOfSightInGcrf.getAlpha(), raDec[0], 1e-12);
        assertEquals(lineOfSightInGcrf.getDelta(), raDec[1], 1e-12);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, 0., 1., 1.5})
    void testFieldValueAgainstRaDec(final double latitude) {
        // GIVEN
        final SignalTravelTimeModel signalTravelTimeModel = new SignalTravelTimeModel();
        final Frame frame = FramesFactory.getGCRF();
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, FramesFactory.getGTOD(true));
        final GeodeticPoint geodeticPoint = new GeodeticPoint(latitude, 0., 0);
        final TopocentricAzElModel measurementModel = new TopocentricAzElModel(frame, bodyShape, signalTravelTimeModel);
        final GradientField field = GradientField.getField(1);
        final Vector3D emitterPosition = new Vector3D(1e7, 2e6, -3e6);
        final FieldAbsoluteDate<Gradient> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(bodyShape.getBodyFrame(),
                date.toAbsoluteDate(), new PVCoordinates(emitterPosition, Vector3D.MINUS_J.scalarMultiply(7)));
        final Gradient dt = new Gradient(0., 1);
        final FieldAbsolutePVCoordinates<Gradient> fieldAPV = new FieldAbsolutePVCoordinates<>(field, absolutePVCoordinates).shiftedBy(dt);
        final FieldGeodeticPoint<Gradient> fieldGeodeticPoint = new FieldGeodeticPoint<>(field, geodeticPoint);
        // WHEN
        final Gradient[] azEl = measurementModel.value(fieldGeodeticPoint, fieldAPV.getDate(), fieldAPV);
        // THEN
        final FieldTrackingCoordinates<Gradient> trackingCoordinates = new FieldTrackingCoordinates<>(azEl[0], azEl[1], field.getOne());
        final FieldVector3D<Gradient> lineOfSightInTopocentric = TopocentricFrame.getTopocentricPosition(trackingCoordinates);
        final FieldStaticTransform<Gradient> topocentricToInertial = new TopocentricFrame(bodyShape, geodeticPoint, "")
                .getStaticTransformTo(frame, fieldAPV.getDate());
        final FieldVector3D<Gradient> lineOfSightInGcrf = topocentricToInertial.transformVector(lineOfSightInTopocentric);
        final RaDecModel raDecModel = new RaDecModel(frame, signalTravelTimeModel);
        final FieldStaticTransform<Gradient> staticTransform = bodyShape.getBodyFrame().getStaticTransformTo(frame, fieldAPV.getDate());
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(fieldAPV.getDate(),
                staticTransform.transformPosition(bodyShape.transform(fieldGeodeticPoint)), frame);
        final Gradient[] raDec = raDecModel.value(receptionCondition, fieldAPV, fieldAPV.getDate());
        assertEquals(lineOfSightInGcrf.getAlpha().getValue(), raDec[0].getValue(), 1e-15);
        assertEquals(lineOfSightInGcrf.getDelta().getValue(), raDec[1].getValue(), 1e-15);
        assertArrayEquals(lineOfSightInGcrf.getDelta().getGradient(), raDec[1].getGradient(), 1e-16);
        assertArrayEquals(lineOfSightInGcrf.getAlpha().getGradient(), raDec[0].getGradient(), 1e-16);
    }

    @Test
    void testValueField() {
        // GIVEN
        final SignalTravelTimeModel signalTravelTimeModel = new SignalTravelTimeModel();
        final Frame earthFixedFrame  = FramesFactory.getGTOD(true);
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                earthFixedFrame);
        final TopocentricAzElModel measurementModel = new TopocentricAzElModel(bodyShape, signalTravelTimeModel);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = TestUtils.getDefaultOrbit(date);
        final GradientField field = GradientField.getField(0);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date);
        final FieldOrbit<Gradient> fieldOrbit = new FieldCartesianOrbit<>(field, orbit);
        // WHEN & THEN
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                final GeodeticPoint geodeticPoint = new GeodeticPoint(FastMath.toRadians( -90. + j * 18.),
                        FastMath.toRadians(i * 36.), 0.);
                final double[] azEl = measurementModel.value(geodeticPoint, date, orbit, date);
                final FieldGeodeticPoint<Gradient> fieldPoint = new FieldGeodeticPoint<>(field, geodeticPoint);
                final Gradient[] gradientAzEl = measurementModel.value(fieldPoint, fieldDate, fieldOrbit, fieldDate);
                assertEquals(azEl[0], gradientAzEl[0].getValue(), 1e-12);
                assertEquals(azEl[1], gradientAzEl[1].getValue(), 1e-12);
            }
        }
    }
}
