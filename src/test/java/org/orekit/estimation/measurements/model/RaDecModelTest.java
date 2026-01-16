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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RaDecModelTest {

    @BeforeAll
    static void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testConstructorException() {
        final Frame frame = mock();
        when(frame.isPseudoInertial()).thenReturn(false);
        final SignalTravelTimeModel signalTravelTimeModel = new SignalTravelTimeModel();
        assertThrows(OrekitException.class, () -> new RaDecModel(frame, signalTravelTimeModel));
    }

    @Test
    void testValueInstantaneousSignal() {
        // GIVEN
        final SignalTravelTimeModel instantaneousSignalModel = new SignalTravelTimeModel((iteration, previous, current) -> true,
                (iteration, previous, current) -> true);
        final Frame frame = FramesFactory.getGCRF();
        final RaDecModel measurementModel = new RaDecModel(frame, instantaneousSignalModel);
        final Vector3D receiverPosition = Vector3D.MINUS_J;
        final Vector3D emitterPosition = new Vector3D(1, 2, 3);
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, AbsoluteDate.ARBITRARY_EPOCH,
                new PVCoordinates(emitterPosition));
        // WHEN
        final double[] raDec = measurementModel.value(frame, receiverPosition, absolutePVCoordinates.getDate(),
                absolutePVCoordinates, absolutePVCoordinates.getDate());
        // THEN
        final Vector3D lineOfSight = emitterPosition.subtract(receiverPosition).normalize();
        assertArrayEquals(new Vector3D(raDec[0], raDec[1]).toArray(), lineOfSight.toArray(), 1e-12);
    }

    @Test
    void testValueField() {
        // GIVEN
        final SignalTravelTimeModel signalTravelTimeModel = new SignalTravelTimeModel();
        final Frame referenceFrame  = FramesFactory.getEME2000();
        final RaDecModel measurementModel = new RaDecModel(referenceFrame, signalTravelTimeModel);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = TestUtils.getDefaultOrbit(date);
        final GradientField field = GradientField.getField(0);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date);
        final FieldOrbit<Gradient> fieldOrbit = new FieldCartesianOrbit<>(field, orbit);
        final Frame earthFixedFrame = FramesFactory.getGTOD(true);
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                earthFixedFrame);
        // WHEN & THEN
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                final GeodeticPoint geodeticPoint = new GeodeticPoint(FastMath.toRadians( -90. + j * 18.),
                        FastMath.toRadians(i * 36.), 0.);
                final Vector3D position = bodyShape.transform(geodeticPoint);
                final double[] raDec = measurementModel.value(earthFixedFrame, position, date, orbit);
                final FieldVector3D<Gradient> fieldPosition = new FieldVector3D<>(field, position);
                final Gradient[] gradientRaDec = measurementModel.value(earthFixedFrame, fieldPosition, fieldDate, fieldOrbit);
                assertEquals(raDec[0], gradientRaDec[0].getValue(), 1e-12);
                assertEquals(raDec[1], gradientRaDec[1].getValue(), 1e-12);
            }
        }
    }
}
