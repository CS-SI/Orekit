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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GroundBasedAngularMeasurementTest {

    @BeforeAll
    static void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testWrapFirstAngle() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0., FramesFactory.getGTOD(true));
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape,
                new GeodeticPoint(0., 0., 0.), "");
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final GroundStation groundStation = new GroundStation(topocentricFrame);
        final GroundBasedAngularMeasurement<?> measurement = new TestMeasurement(groundStation, date, new SignalTravelTimeModel());
        final double angle = 7.;
        // WHEN
        final double actualAngle = measurement.wrapFirstAngle(angle);
        // THEN
        assertEquals(angle - MathUtils.TWO_PI, actualAngle);
    }

    @Test
    void testWrapFirstAngleGradient() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0., FramesFactory.getGTOD(true));
        final TopocentricFrame topocentricFrame = new TopocentricFrame(bodyShape,
                new GeodeticPoint(0., 0., 0.), "");
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final GroundStation groundStation = new GroundStation(topocentricFrame);
        final GroundBasedAngularMeasurement<?> measurement = new TestMeasurement(groundStation, date, new SignalTravelTimeModel());
        final Gradient angle = new Gradient(-7.);
        // WHEN
        final Gradient actualAngle = measurement.wrapFirstAngle(angle);
        // THEN
        assertEquals(measurement.wrapFirstAngle(angle.getValue()), actualAngle.getValue());
    }

    class TestMeasurement extends GroundBasedAngularMeasurement<AngularAzEl> {

        protected TestMeasurement(GroundStation station, AbsoluteDate date, SignalTravelTimeModel signalTravelTimeModel) {
            super(station, date, new double[2], new double[2], new double[2], signalTravelTimeModel, new ObservableSatellite(0));
        }

        @Override
        protected EstimatedMeasurement<AngularAzEl> theoreticalEvaluation(int iteration, int evaluation, SpacecraftState[] states) {
            return null;
        }
    }
}
