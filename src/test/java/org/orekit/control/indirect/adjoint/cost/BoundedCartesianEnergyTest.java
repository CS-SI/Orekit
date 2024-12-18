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
package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.ode.events.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.events.EventDetector;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BoundedCartesianEnergyTest {

    @Test
    void getEventDetectorsSizeAndActionTest() {
        // GIVEN
        final double maximumThrustMagnitude = 1.;
        final double massFlowRateFactor = 2.;
        final BoundedCartesianEnergy boundedCartesianEnergy = new BoundedCartesianEnergy("", massFlowRateFactor,
                maximumThrustMagnitude);
        // WHEN
        final Stream<EventDetector> eventDetectorStream = boundedCartesianEnergy.getEventDetectors();
        // THEN
        final List<EventDetector> eventDetectors = eventDetectorStream.collect(Collectors.toList());
        Assertions.assertEquals(2, eventDetectors.size());
        for (final EventDetector eventDetector : eventDetectors) {
            Assertions.assertInstanceOf(CartesianEnergyConsideringMass.SingularityDetector.class, eventDetector);
            final CartesianEnergyConsideringMass.SingularityDetector singularityDetector =
                    (CartesianEnergyConsideringMass.SingularityDetector) eventDetector;
            Assertions.assertEquals(Action.RESET_DERIVATIVES, singularityDetector.getHandler().eventOccurred(null, null, true));
        }
    }
}
