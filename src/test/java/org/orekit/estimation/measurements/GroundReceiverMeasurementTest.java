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

import org.hipparchus.analysis.differentiation.Gradient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.clocks.QuadraticClockModel;
import org.orekit.utils.Constants;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GroundReceiverMeasurementTest {

    @BeforeAll
    static void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testGetCorrectedReceptionDate() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0., FramesFactory.getGTOD(true));
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape,
                new GeodeticPoint(0., 0., 0.), "");
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final QuadraticClockModel clockModel = new QuadraticClockModel(date, 1., 2., 3.);
        final GroundStation groundStation = new GroundStation(topocentricFrame, clockModel);
        final GroundReceiverMeasurement<?> measurement = new TestMeasurement(groundStation, date, new SignalTravelTimeModel());
        // WHEN
        final AbsoluteDate actualReceptionDate = groundStation.getCorrectedReceptionDate(measurement.getDate());
        // THEN
        final AbsoluteDate expectedDate = date.shiftedBy(-clockModel.getOffset(date).getOffset());
        assertEquals(expectedDate, actualReceptionDate);
    }

    @Test
    void testGetCorrectedReceptionDateField() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0., FramesFactory.getGTOD(true));
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape,
                new GeodeticPoint(0., 0., 0.), "");
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final QuadraticClockModel clockModel = new QuadraticClockModel(date.shiftedBy(10.), 1., 2., 3.);
        final GroundStation groundStation = new GroundStation(topocentricFrame, clockModel);
        final GroundReceiverMeasurement<?> measurement = new TestMeasurement(groundStation, date, new SignalTravelTimeModel());
        // WHEN
        final FieldAbsoluteDate<Gradient> actualReceptionDate = groundStation.getCorrectedReceptionDateField(measurement.getDate(), 0, new HashMap<>());
        // THEN
        final AbsoluteDate expectedDate = date.shiftedBy(-clockModel.getOffset(date).getOffset());
        assertEquals(expectedDate, actualReceptionDate.toAbsoluteDate());
    }

    static class TestMeasurement extends GroundReceiverMeasurement<AngularAzEl> {

        protected TestMeasurement(GroundStation station, AbsoluteDate date, SignalTravelTimeModel signalTravelTimeModel) {
            super(station, true, date, new double[2], new double[2], new double[2], signalTravelTimeModel, new ObservableSatellite(0));
        }

        @Override
        protected EstimatedMeasurement<AngularAzEl> theoreticalEvaluation(int iteration, int evaluation, SpacecraftState[] states) {
            return null;
        }
    }
}
