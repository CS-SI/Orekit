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
package org.orekit.propagation.events.intervals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.*;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

class ApsideDetectionAdaptableIntervalFactoryTest {

    @Test
    void testGetForwardApsideDetectionAdaptableInterval() {
        // GIVEN
        final Orbit initialOrbit = createOrbit(1.);
        final AdaptableInterval forwardAdaptableInterval = ApsideDetectionAdaptableIntervalFactory
                .getApsideDetectionAdaptableInterval();
        // WHEN
        final double value = forwardAdaptableInterval.currentInterval(new SpacecraftState(initialOrbit), true);
        // THEN
        Assertions.assertTrue(value <= initialOrbit.getKeplerianPeriod() / 2);
    }

    @Test
    void testGetBackwardApsideDetectionAdaptableInterval() {
        // GIVEN
        final Orbit initialOrbit = createOrbit(1.);
        final AdaptableInterval backwardAdaptableInterval = ApsideDetectionAdaptableIntervalFactory
                .getApsideDetectionAdaptableInterval();
        // WHEN
        final double value = backwardAdaptableInterval.currentInterval(new SpacecraftState(initialOrbit), true);
        // THEN
        Assertions.assertTrue(value <= initialOrbit.getKeplerianPeriod() / 2);
    }

    @Test
    void testGetForwardPeriapsisDetectionAdaptableInterval() {
        // GIVEN
        final Orbit initialOrbit = createOrbit(1.);
        final EventSlopeFilter<ApsideDetector> periapsisDetector = createPeriapsisDetector(initialOrbit);
        final AdaptableInterval forwardAdaptableInterval = ApsideDetectionAdaptableIntervalFactory
                .getPeriapsisDetectionAdaptableInterval();
        final AdaptableIntervalWithCounter forwardAdaptableIntervalWithCounter = new AdaptableIntervalWithCounter(
                forwardAdaptableInterval);
        final Propagator propagator = createPropagatorWithDetector(initialOrbit,
                periapsisDetector.withMaxCheck(forwardAdaptableIntervalWithCounter));
        // WHEN
        final AbsoluteDate targetDate = initialOrbit.getDate().shiftedBy(initialOrbit.getKeplerianPeriod() * 2);
        final SpacecraftState terminalState = propagator.propagate(targetDate);
        // THEN
        Assertions.assertNotEquals(targetDate, terminalState.getDate());
        final int countWithDefaultAdaptableInterval = countWithConstantAdaptableInterval(initialOrbit, targetDate,
                periapsisDetector);
        Assertions.assertTrue(countWithDefaultAdaptableInterval > forwardAdaptableIntervalWithCounter.count);
    }

    @Test
    void testGetBackwardPeriapsisDetectionAdaptableInterval() {
        // GIVEN
        final Orbit initialOrbit = createOrbit(6.);
        final EventSlopeFilter<ApsideDetector> periapsisDetector = createPeriapsisDetector(initialOrbit);
        final AdaptableInterval backwardAdaptableInterval = ApsideDetectionAdaptableIntervalFactory
                .getPeriapsisDetectionAdaptableInterval();
        final AdaptableIntervalWithCounter backwardAdaptableIntervalWithCounter = new AdaptableIntervalWithCounter(
                backwardAdaptableInterval);
        final Propagator propagator = createPropagatorWithDetector(initialOrbit,
                periapsisDetector.withMaxCheck(backwardAdaptableIntervalWithCounter));
        // WHEN
        final AbsoluteDate targetDate = initialOrbit.getDate().shiftedBy(-initialOrbit.getKeplerianPeriod() * 2);
        final SpacecraftState terminalState = propagator.propagate(targetDate);
        // THEN
        Assertions.assertNotEquals(targetDate, terminalState.getDate());
        final int countWithDefaultAdaptableInterval = countWithConstantAdaptableInterval(initialOrbit, targetDate,
                periapsisDetector);
        Assertions.assertTrue(countWithDefaultAdaptableInterval > backwardAdaptableIntervalWithCounter.count);
    }

    @Test
    void testGetForwardApoapsisDetectionAdaptableInterval() {
        // GIVEN
        final Orbit initialOrbit = createOrbit(4.);
        final EventSlopeFilter<ApsideDetector> apoapsisDetector = createApoapsisDetector(initialOrbit);
        final AdaptableInterval forwardAdaptableInterval = ApsideDetectionAdaptableIntervalFactory
                .getApoapsisDetectionAdaptableInterval();
        final AdaptableIntervalWithCounter forwardAdaptableIntervalWithCounter = new AdaptableIntervalWithCounter(
                forwardAdaptableInterval);
        final Propagator propagator = createPropagatorWithDetector(initialOrbit,
                apoapsisDetector.withMaxCheck(forwardAdaptableIntervalWithCounter));
        // WHEN
        final AbsoluteDate targetDate = initialOrbit.getDate().shiftedBy(initialOrbit.getKeplerianPeriod() * 2);
        final SpacecraftState terminalState = propagator.propagate(targetDate);
        // THEN
        Assertions.assertNotEquals(targetDate, terminalState.getDate());
        final int countWithDefaultAdaptableInterval = countWithConstantAdaptableInterval(initialOrbit, targetDate,
                apoapsisDetector);
        Assertions.assertTrue(countWithDefaultAdaptableInterval > forwardAdaptableIntervalWithCounter.count);
    }

    @Test
    void testGetBackwardApoapsisDetectionAdaptableInterval() {
        // GIVEN
        final Orbit initialOrbit = createOrbit(3.);
        final EventSlopeFilter<ApsideDetector> apoapsisDetector = createApoapsisDetector(initialOrbit);
        final AdaptableInterval backwardAdaptableInterval = ApsideDetectionAdaptableIntervalFactory
                .getApoapsisDetectionAdaptableInterval();
        final AdaptableIntervalWithCounter backwardAdaptableIntervalWithCounter = new AdaptableIntervalWithCounter(
                backwardAdaptableInterval);
        final Propagator propagator = createPropagatorWithDetector(initialOrbit,
                apoapsisDetector.withMaxCheck(backwardAdaptableIntervalWithCounter));
        // WHEN
        final AbsoluteDate targetDate = initialOrbit.getDate().shiftedBy(-initialOrbit.getKeplerianPeriod() * 2);
        final SpacecraftState terminalState = propagator.propagate(targetDate);
        // THEN
        Assertions.assertNotEquals(targetDate, terminalState.getDate());
        final int countWithDefaultAdaptableInterval = countWithConstantAdaptableInterval(initialOrbit, targetDate,
                apoapsisDetector);
        Assertions.assertTrue(countWithDefaultAdaptableInterval > backwardAdaptableIntervalWithCounter.count);
    }


    private Propagator createPropagatorWithDetector(final Orbit initialOrbit,
                                                    final EventDetector eventDetector) {
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(eventDetector);
        return propagator;
    }

    private EventSlopeFilter<ApsideDetector> createPeriapsisDetector(final Orbit initialOrbit) {
        return new EventSlopeFilter<>(new ApsideDetector(initialOrbit), FilterType.TRIGGER_ONLY_DECREASING_EVENTS)
                .withHandler(new StopOnEvent());
    }

    private EventSlopeFilter<ApsideDetector> createApoapsisDetector(final Orbit initialOrbit) {
        return new EventSlopeFilter<>(new ApsideDetector(initialOrbit), FilterType.TRIGGER_ONLY_INCREASING_EVENTS)
                .withHandler(new StopOnEvent());
    }

    private AdaptableInterval getConstantAdaptableInterval() {
        return (state, isForward) -> state.getOrbit().getKeplerianPeriod() / 3.;
    }

    private Orbit createOrbit(final double meanAnomaly) {
        return new KeplerianOrbit(1e7, 0.001, 1., 2., 3., meanAnomaly, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
    }

    private int countWithConstantAdaptableInterval(final Orbit initialOrbit, final AbsoluteDate targetDate,
                                                   final EventSlopeFilter<ApsideDetector> apsideDetector) {
        final AdaptableIntervalWithCounter adaptableIntervalWithCounter = new AdaptableIntervalWithCounter(
                getConstantAdaptableInterval());
        final Propagator otherPropagator = createPropagatorWithDetector(initialOrbit,
                apsideDetector.withMaxCheck(adaptableIntervalWithCounter));
        otherPropagator.propagate(targetDate);
        return adaptableIntervalWithCounter.count;
    }

    private static class AdaptableIntervalWithCounter implements AdaptableInterval {

        private final AdaptableInterval interval;
        int count = 0;

        AdaptableIntervalWithCounter(final AdaptableInterval adaptableInterval) {
            this.interval = adaptableInterval;
        }

        @Override
        public double currentInterval(SpacecraftState state, boolean isForward) {
            count++;
            return interval.currentInterval(state, isForward);
        }
    }

}
