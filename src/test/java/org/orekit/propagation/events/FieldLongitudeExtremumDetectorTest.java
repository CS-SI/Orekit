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
package org.orekit.propagation.events;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class FieldLongitudeExtremumDetectorTest {

    @Test
    void testCreate() {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final Binary64Field field = Binary64Field.getInstance();
        final FieldLongitudeExtremumDetector<Binary64> fieldDetector = new FieldLongitudeExtremumDetector<>(field,
                ellipsoid);
        final FieldEventHandler<Binary64> fieldHandler = new FieldStopOnEvent<>();
        final FieldEventDetectionSettings<Binary64> fieldEventDetectionSettings = new FieldEventDetectionSettings<>(field,
                EventDetectionSettings.getDefaultEventDetectionSettings());
        // WHEN
        final FieldLongitudeExtremumDetector<Binary64> newDetector = fieldDetector.create(fieldEventDetectionSettings,
                fieldHandler);
        // THEN
        assertEquals(fieldHandler, newDetector.getHandler());
        assertEquals(fieldEventDetectionSettings, newDetector.getDetectionSettings());
    }

    @Test
    void testG() {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final Binary64Field field = Binary64Field.getInstance();
        final FieldLongitudeExtremumDetector<Binary64> fieldDetector = new FieldLongitudeExtremumDetector<>(field,
                ellipsoid);
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final SpacecraftState state = new SpacecraftState(orbit);
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        // WHEN
        final Binary64 actualG = fieldDetector.g(fieldState);
        // THEN
        final double expectedG = new LongitudeExtremumDetector(ellipsoid).g(state);
        assertEquals(expectedG, actualG.getReal());
    }

    @Test
    void testDetection() {
        // GIVEN
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        final Binary64Field field = Binary64Field.getInstance();
        final FieldLongitudeExtremumDetector<Binary64> fieldDetector = new FieldLongitudeExtremumDetector<>(field,
                ellipsoid);
        final Orbit orbit = new KeplerianOrbit(42157e3, 0.001, 0.1, 0, 0, 0, PositionAngleType.MEAN, FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final FieldEventsLogger<Binary64> fieldEventsLogger = new FieldEventsLogger<>();
        final FieldKeplerianPropagator<Binary64> fieldPropagator = new FieldKeplerianPropagator<>(new FieldEquinoctialOrbit<>(field, orbit));
        fieldPropagator.addEventDetector(fieldEventsLogger.monitorDetector(fieldDetector));
        final FieldAbsoluteDate<Binary64> targetDate = fieldPropagator.getInitialState().getDate().shiftedBy(1e6);
        // WHEN
        fieldPropagator.propagate(targetDate);
        // THEN
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final EventsLogger logger = new EventsLogger();
        final LongitudeExtremumDetector latitudeExtremumDetector = new LongitudeExtremumDetector(ellipsoid);
        propagator.addEventDetector(logger.monitorDetector(latitudeExtremumDetector));
        propagator.propagate(targetDate.toAbsoluteDate());
        assertEquals(logger.getLoggedEvents().size(), fieldEventsLogger.getLoggedEvents().size());
        for (int i = 0; i < fieldEventsLogger.getLoggedEvents().size(); i++) {
            final FieldSpacecraftState<Binary64> fieldState = fieldEventsLogger.getLoggedEvents().get(i).getState();
            final SpacecraftState state = logger.getLoggedEvents().get(i).getState();
            final Binary64 duration = fieldState.durationFrom(state);
            assertEquals(0., duration.getReal(), 1e-7);
        }
    }

    @Test
    void testToEventDetector() {
        // GIVEN
        final FieldLongitudeExtremumDetector<Binary64> fieldDetector = new FieldLongitudeExtremumDetector<>(Binary64Field.getInstance(),
                mock(BodyShape.class));
        final EventHandler expectedHandler = new ContinueOnEvent();
        // WHEN
        final LongitudeExtremumDetector detector = fieldDetector.toEventDetector(expectedHandler);
        // THEN
        assertEquals(expectedHandler, detector.getHandler());
        assertEquals(fieldDetector.getBodyShape(), detector.getBodyShape());
    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
