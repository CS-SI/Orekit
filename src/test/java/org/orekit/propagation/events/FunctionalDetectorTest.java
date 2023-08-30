/* Contributed in the public domain.
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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.function.ToDoubleFunction;

/**
 * Unit tests for {@link FunctionalDetector}
 *
 * @author Evan Ward
 */
public class FunctionalDetectorTest {

    /**
     * Check {@link FunctionalDetector}.
     */
    @Test
    public void testFunctionalDetector() {
        // setup
        ToDoubleFunction<SpacecraftState> g = SpacecraftState::getMass;
        EventHandler handler = (s, detector, increasing) -> Action.STOP;

        // action
        FunctionalDetector detector = new FunctionalDetector()
                .withMaxIter(1)
                .withThreshold(2)
                .withMaxCheck(3)
                .withHandler(handler)
                .withFunction(g);

        // verify
        MatcherAssert.assertThat(detector.getMaxIterationCount(), CoreMatchers.is(1));
        MatcherAssert.assertThat(detector.getThreshold(), CoreMatchers.is(2.0));
        MatcherAssert.assertThat(detector.getMaxCheckInterval().currentInterval(null), CoreMatchers.is(3.0));
        MatcherAssert.assertThat(detector.getHandler(), CoreMatchers.is(handler));
        SpacecraftState state = new SpacecraftState(
                new CartesianOrbit(
                        new PVCoordinates(
                                new Vector3D(1, 2, 3),
                                new Vector3D(4, 5, 6)),
                        FramesFactory.getGCRF(),
                        AbsoluteDate.CCSDS_EPOCH,
                        4),
                5);
        MatcherAssert.assertThat(detector.g(state), CoreMatchers.is(5.0));
        MatcherAssert.assertThat(detector.getHandler().eventOccurred(null, detector, false), CoreMatchers.is(Action.STOP));
        MatcherAssert.assertThat(detector.getFunction(), CoreMatchers.is(g));
    }

}
