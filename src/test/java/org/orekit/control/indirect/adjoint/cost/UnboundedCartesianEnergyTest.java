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
package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.ode.events.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.events.EventDetector;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class UnboundedCartesianEnergyTest {

    @Test
    void testGetMassFlowRateFactor() {
        // GIVEN
        final double expectedRateFactor = 1.;
        final UnboundedCartesianEnergy unboundedCartesianEnergy = new UnboundedCartesianEnergy("", expectedRateFactor);
        // WHEN
        final double actualRateFactor = unboundedCartesianEnergy.getMassFlowRateFactor();
        // THEN
        Assertions.assertEquals(expectedRateFactor, actualRateFactor);
    }

    @Test
    void getEventDetectorsSizeAndActionTest() {
        // GIVEN
        final double massFlowRateFactor = 1.;
        final UnboundedCartesianEnergy unboundedCartesianEnergy = new UnboundedCartesianEnergy("", massFlowRateFactor);
        // WHEN
        final Stream<EventDetector> eventDetectorStream = unboundedCartesianEnergy.getEventDetectors();
        // THEN
        final List<EventDetector> eventDetectors = eventDetectorStream.collect(Collectors.toList());
        Assertions.assertEquals(1, eventDetectors.size());
        Assertions.assertInstanceOf(CartesianEnergyConsideringMass.SingularityDetector.class, eventDetectors.get(0));
        final CartesianEnergyConsideringMass.SingularityDetector singularityDetector =
                (CartesianEnergyConsideringMass.SingularityDetector) eventDetectors.get(0);
        Assertions.assertEquals(Action.RESET_DERIVATIVES, singularityDetector.getHandler().eventOccurred(null, null, false));
    }

}
