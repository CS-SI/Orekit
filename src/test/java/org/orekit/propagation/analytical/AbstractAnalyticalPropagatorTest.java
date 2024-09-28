/* Copyright 2022-2024 Romain Serra
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
package org.orekit.propagation.analytical;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;


class AbstractAnalyticalPropagatorTest {

    @Test
    void testFinish() {
        // GIVEN
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = new KeplerianOrbit(8000000.0, 0.01, 0.87, 2.44, 0.21, -1.05, PositionAngleType.MEAN,
                eme2000, date, Constants.EIGEN5C_EARTH_MU);
        final TestAnalyticalPropagator propagator = new TestAnalyticalPropagator(orbit);
        final TestHandler handler = new TestHandler();
        propagator.addEventDetector(new DateDetector(AbsoluteDate.ARBITRARY_EPOCH).withHandler(handler));
        // WHEN
        propagator.propagate(propagator.getInitialState().getDate().shiftedBy(1.));
        // THEN
        Assertions.assertTrue(handler.isFinished);
    }

    private static class TestHandler extends ContinueOnEvent {
        boolean isFinished = false;

        @Override
        public void finish(SpacecraftState finalState, EventDetector detector) {
            isFinished = true;
        }
    }

    private static class TestAnalyticalPropagator extends AbstractAnalyticalPropagator {

        private final Orbit orbit;

        protected TestAnalyticalPropagator(Orbit orbit) {
            super(new FrameAlignedProvider(FramesFactory.getGCRF()));
            this.orbit = orbit;
            resetInitialState(new SpacecraftState(orbit));
        }

        @Override
        protected double getMass(AbsoluteDate date) {
            return 1;
        }

        @Override
        protected void resetIntermediateState(SpacecraftState state, boolean forward) {

        }

        @Override
        protected Orbit propagateOrbit(AbsoluteDate date) {
            return orbit.shiftedBy(date.durationFrom(orbit.getDate()));
        }
    }
}
