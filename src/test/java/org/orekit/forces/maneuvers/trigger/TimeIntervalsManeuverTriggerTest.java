/* Copyright 2022-2025 Romain Serra
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
package org.orekit.forces.maneuvers.trigger;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.forces.maneuvers.ConstantThrustManeuver;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.ThrustPropulsionModel;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.BooleanDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldBooleanDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldTimeIntervalDetector;
import org.orekit.propagation.events.TimeIntervalDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterval;
import org.orekit.utils.TimeSpanMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeIntervalsManeuverTriggerTest {

    @Test
    void testOfDetectors() {
        // GIVEN
        final TimeInterval timeInterval = TimeInterval.of(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        final TimeIntervalDetector intervalDetector = new TimeIntervalDetector(new ContinueOnEvent(), timeInterval);
        // WHEN
        final TimeIntervalsManeuverTrigger trigger = TimeIntervalsManeuverTrigger.of(intervalDetector);
        // THEN
        final BooleanDetector booleanDetector = trigger.getFiringIntervalDetector();
        assertEquals(intervalDetector.getThreshold(), booleanDetector.getThreshold());
        assertEquals(intervalDetector.getMaxIterationCount(), booleanDetector.getMaxIterationCount());
        final SpacecraftState mockedState = mock();
        when(mockedState.getDate()).thenReturn(AbsoluteDate.ARBITRARY_EPOCH);
        final boolean forward = true;
        assertEquals(intervalDetector.getMaxCheckInterval().currentInterval(mockedState, forward),
                booleanDetector.getMaxCheckInterval().currentInterval(mockedState, forward));
    }

    @Test
    void testOfTimeIntervals() {
        // GIVEN
        final TimeInterval timeInterval = TimeInterval.of(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        // WHEN
        final TimeIntervalsManeuverTrigger trigger = TimeIntervalsManeuverTrigger.of(timeInterval, timeInterval);
        // THEN
        final BooleanDetector booleanDetector = trigger.getFiringIntervalDetector();
        for (final EventDetector detector: booleanDetector.getDetectors()) {
            assertInstanceOf(TimeIntervalDetector.class, detector);
            assertEquals(timeInterval, ((TimeIntervalDetector) detector).getTimeInterval());
        }
    }

    @Test
    void testPropagationFiringSpans() {
        // GIVEN
        final AbsoluteDate epoch = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate startFiring = epoch.shiftedBy(100.);
        final double duration = 10;
        final TimeInterval firstTimeInterval = TimeInterval.of(startFiring, startFiring.shiftedBy(duration));
        final AbsoluteDate secondFiring = firstTimeInterval.getEndDate().shiftedBy(30.);
        final TimeInterval secondTimeInterval = TimeInterval.of(secondFiring, secondFiring.shiftedBy(duration));
        final TimeIntervalsManeuverTrigger trigger = TimeIntervalsManeuverTrigger.of(firstTimeInterval, secondTimeInterval);
        final Orbit initialOrbit = TestUtils.getDefaultOrbit(epoch);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        final ThrustPropulsionModel propulsionModel = new BasicConstantThrustPropulsionModel(1e-2, 1000., Vector3D.MINUS_I, "");
        final NumericalPropagator propagator = buildPropagator(initialState, new Maneuver(null, trigger, propulsionModel));
        // WHEN
        propagator.propagate(secondTimeInterval.getEndDate().shiftedBy(1));
        // THEN
        final TimeSpanMap<Boolean> firings = trigger.getFirings();
        assertEquals(5, firings.getSpansNumber());
        assertEquals(firstTimeInterval.getStartDate(), firings.getFirstNonNullSpan().getEnd());
        assertEquals(secondTimeInterval.getEndDate(), firings.getLastNonNullSpan().getStart());
    }

    @Test
    void testPropagationAgainstConstantThrustManeuver() {
        // GIVEN
        final AbsoluteDate epoch = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate startFiring = epoch.shiftedBy(100.);
        final double duration = 60;
        final TimeInterval timeInterval = TimeInterval.of(startFiring, startFiring.shiftedBy(duration));
        final TimeIntervalsManeuverTrigger trigger = TimeIntervalsManeuverTrigger.of(new TimeIntervalDetector(new ContinueOnEvent(),
                timeInterval));
        final Orbit initialOrbit = TestUtils.getDefaultOrbit(epoch);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        final BasicConstantThrustPropulsionModel propulsionModel = new BasicConstantThrustPropulsionModel(1e-1, 100., Vector3D.PLUS_I, "");
        final NumericalPropagator propagator = buildPropagator(initialState, new Maneuver(null, trigger, propulsionModel));
        // WHEN
        final AbsoluteDate terminalDate = timeInterval.getEndDate().shiftedBy(1);
        final SpacecraftState actualState = propagator.propagate(terminalDate);
        // THEN
        final TimeSpanMap<Boolean> firings = trigger.getFirings();
        assertEquals(3, firings.getSpansNumber());
        final Maneuver maneuver = new ConstantThrustManeuver(startFiring, duration, null, propulsionModel);
        final NumericalPropagator otherPropagator = buildPropagator(initialState, maneuver);
        final SpacecraftState expectedState = otherPropagator.propagate(terminalDate);
        assertEquals(expectedState.getPosition(), actualState.getPosition());
    }

    private static NumericalPropagator buildPropagator(final SpacecraftState state, final Maneuver maneuver) {
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(20.));
        propagator.setInitialState(state);
        propagator.addForceModel(maneuver);
        return propagator;
    }

    @Test
    void testGetParametersDrivers() {
        // GIVEN
        final TimeInterval timeInterval = TimeInterval.of(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        // WHEN
        final TimeIntervalsManeuverTrigger trigger = TimeIntervalsManeuverTrigger.of(timeInterval);
        // THEN
        assertTrue(trigger.getParametersDrivers().isEmpty());
    }

    @Test
    void testConvertIntervalDetector() {
        // GIVEN
        final TimeInterval timeInterval = TimeInterval.of(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        final TimeIntervalsManeuverTrigger trigger = TimeIntervalsManeuverTrigger.of(timeInterval);
        final Binary64Field field = Binary64Field.getInstance();
        // WHEN
        final FieldEventDetector<Binary64> fieldEventDetector = trigger.convertIntervalDetector(field,
                trigger.getFiringIntervalDetector());
        // THEN
        assertInstanceOf(FieldBooleanDetector.class, fieldEventDetector);
        final FieldBooleanDetector<Binary64> fieldBooleanDetector = (FieldBooleanDetector<Binary64>) fieldEventDetector;
        assertEquals(1, fieldBooleanDetector.getDetectors().size());
        assertInstanceOf(FieldTimeIntervalDetector.class, fieldBooleanDetector.getDetectors().get(0));
    }
}
