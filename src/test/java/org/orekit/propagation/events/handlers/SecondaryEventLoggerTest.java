/* Copyright 2022-2025 Romain Serra.
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
package org.orekit.propagation.events.handlers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.TestUtils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

import java.util.List;

import static org.mockito.Mockito.mock;

class SecondaryEventLoggerTest {

    private static final AbsoluteDate DEFAULT_DATE = AbsoluteDate.ARBITRARY_EPOCH;

    @Test
    void testGetter() {
        // GIVEN
        final SpacecraftState state = buildState(DEFAULT_DATE);
        final PVCoordinatesProvider pvCoordinatesProvider = state.getOrbit();
        final SecondaryEventLogger secondaryEventLogger = new SecondaryEventLogger(pvCoordinatesProvider);
        // WHEN
        final PVCoordinatesProvider actualProvider = secondaryEventLogger.getSecondaryPVCoordinatesProvider();
        // THEN
        Assertions.assertEquals(pvCoordinatesProvider, actualProvider);
    }

    @Test
    void testConstructor() {
        // GIVEN
        final SpacecraftState state = buildState(DEFAULT_DATE);
        final SecondaryEventLogger secondaryEventLogger = new SecondaryEventLogger(state.getOrbit());
        final EventDetector detector = mock();
        // WHEN
        final Action actualAction = secondaryEventLogger.eventOccurred(state, detector, true);
        // THEN
        Assertions.assertEquals(Action.CONTINUE, actualAction);
    }

    @Test
    void testInit() {
        // GIVEN
        final SpacecraftState state = buildState(DEFAULT_DATE);
        final SecondaryEventLogger secondaryEventLogger = new SecondaryEventLogger(state.getOrbit());
        // WHEN
        secondaryEventLogger.init(state, AbsoluteDate.FUTURE_INFINITY, mock(EventDetector.class));
        // THEN
        final List<Pair<AbsolutePVCoordinates, PVCoordinates>> logs = secondaryEventLogger.copyLogs();
        Assertions.assertEquals(1, logs.size());
        final Pair<AbsolutePVCoordinates, PVCoordinates> log = logs.get(0);
        Assertions.assertEquals(state.getDate(), log.getKey().getDate());
    }

    @Test
    void testEventOccurred() {
        // GIVEN
        final AbsoluteDate date = DEFAULT_DATE;
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.MINUS_I.scalarMultiply(10), new Vector3D(1, 2, 3));
        final SpacecraftState primaryState = new SpacecraftState(new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                date, pvCoordinates));
        final double shift = 100;
        final SpacecraftState secondaryState = buildState(date.shiftedBy(shift));
        final Orbit secondaryOrbit = secondaryState.getOrbit();
        final SecondaryEventLogger secondaryEventLogger = new SecondaryEventLogger(secondaryOrbit);
        final EventDetector detector = mock();
        // WHEN
        secondaryEventLogger.eventOccurred(primaryState, detector, true);
        // THEN
        final Pair<AbsolutePVCoordinates, PVCoordinates> log = secondaryEventLogger.copyLogs().get(0);
        final AbsolutePVCoordinates primaryPVCoordinates = log.getKey();
        final PVCoordinates secondaryPVCoordinates = log.getValue();
        Assertions.assertEquals(primaryState.getDate(), primaryPVCoordinates.getDate());
        Assertions.assertEquals(primaryState.getFrame(), primaryPVCoordinates.getFrame());
        Assertions.assertEquals(pvCoordinates.getPosition(), primaryPVCoordinates.getPosition());
        Assertions.assertEquals(pvCoordinates.getVelocity(), primaryPVCoordinates.getVelocity());
        final PVCoordinates shiftedOrbitPV = secondaryOrbit.shiftedBy(-shift).getPVCoordinates(primaryState.getFrame());
        Assertions.assertEquals(shiftedOrbitPV.getPosition(), secondaryPVCoordinates.getPosition());
        Assertions.assertEquals(shiftedOrbitPV.getVelocity(), secondaryPVCoordinates.getVelocity());
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void testEventOccurredAction(final Action expectedAction) {
        // GIVEN
        final SpacecraftState state = buildState(DEFAULT_DATE);
        final SecondaryEventLogger secondaryEventLogger = new SecondaryEventLogger(state.getOrbit(), expectedAction);
        final EventDetector detector = mock();
        // WHEN
        final Action actualAction = secondaryEventLogger.eventOccurred(state, detector, true);
        // THEN
        Assertions.assertEquals(expectedAction, actualAction);
    }

    @Test
    void testEventOccurredManyTimes() {
        // GIVEN
        final SpacecraftState state = buildState(DEFAULT_DATE);
        final SecondaryEventLogger secondaryEventLogger = new SecondaryEventLogger(state.getOrbit());
        final int expectedSize = 10;
        final EventDetector detector = mock();
        // WHEN
        for (int i = 0; i < expectedSize; i++) {
            secondaryEventLogger.eventOccurred(state.shiftedBy(i), detector, true);
        }
        // THEN
        final List<Pair<AbsolutePVCoordinates, PVCoordinates>> logs = secondaryEventLogger.copyLogs();
        Assertions.assertEquals(expectedSize, logs.size());
        for (int i = 0; i < expectedSize; i++) {
            Assertions.assertEquals(state.getDate().shiftedBy(i), logs.get(i).getKey().getDate());
        }
    }

    @Test
    void testFinish() {
        // GIVEN
        final SpacecraftState state = buildState(DEFAULT_DATE);
        final SecondaryEventLogger secondaryEventLogger = new SecondaryEventLogger(state.getOrbit());
        // WHEN
        secondaryEventLogger.finish(state, mock(EventDetector.class));
        // THEN
        final List<Pair<AbsolutePVCoordinates, PVCoordinates>> logs = secondaryEventLogger.copyLogs();
        Assertions.assertEquals(1, logs.size());
        final Pair<AbsolutePVCoordinates, PVCoordinates> log = logs.get(0);
        Assertions.assertEquals(state.getDate(), log.getKey().getDate());
    }

    @Test
    void testClearLogs() {
        // GIVEN
        final SpacecraftState state = buildState(DEFAULT_DATE);
        final SecondaryEventLogger secondaryEventLogger = new SecondaryEventLogger(state.getOrbit());
        final EventDetector detector = mock();
        // WHEN
        secondaryEventLogger.eventOccurred(state, detector, false);
        secondaryEventLogger.clearLogs();
        // THEN
        final List<Pair<AbsolutePVCoordinates, PVCoordinates>> logs = secondaryEventLogger.copyLogs();
        Assertions.assertEquals(0, logs.size());
    }

    private SpacecraftState buildState(final AbsoluteDate date) {
        return new SpacecraftState(OrbitType.EQUINOCTIAL.convertType(TestUtils.getDefaultOrbit(date)));
    }
}
