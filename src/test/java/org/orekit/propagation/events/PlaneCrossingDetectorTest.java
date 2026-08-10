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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.functions.EventFunction;
import org.orekit.propagation.events.functions.PlaneCrossingFunction;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.StopOnDecreasing;
import org.orekit.time.AbsoluteDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaneCrossingDetectorTest {

    @Test
    void testG() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final PlaneCrossingFunction crossingFunction = new PlaneCrossingFunction(Vector3D.MINUS_K, frame);
        final PlaneCrossingDetector crossingDetector = new PlaneCrossingDetector(EventDetectionSettings.getDefaultEventDetectionSettings(),
                new ContinueOnEvent(), crossingFunction);
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        // WHEN
        final double g = crossingDetector.g(state);
        // THEN
        assertEquals(crossingFunction.value(state), g);
    }

    @Test
    void testCreate() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final PlaneCrossingFunction crossingFunction = new PlaneCrossingFunction(Vector3D.MINUS_I, frame);
        final PlaneCrossingDetector crossingDetector = new PlaneCrossingDetector(EventDetectionSettings.getDefaultEventDetectionSettings(),
                new ContinueOnEvent(), crossingFunction);
        final StopOnDecreasing expectedHandler = new StopOnDecreasing();
        // WHEN
        final PlaneCrossingDetector otherDetector = crossingDetector.withHandler(expectedHandler);
        // THEN
        final EventFunction actualFunction = otherDetector.getEventFunction();
        assertEquals(crossingFunction, actualFunction);
        assertEquals(expectedHandler, otherDetector.getHandler());
    }
}
