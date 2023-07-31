/* Copyright 2002-2023 CS GROUP
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
package org.orekit.ssa.metrics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.Laas2015;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.ShortTermEncounter2DPOCMethod;

class ProbabilityOfCollisionTest {

    @Test
    @DisplayName("Test ProbabilityOfCollision constructor")
    void testCreateExpectedProbabilityOfCollisionInstance() {
        // GIVEN
        final double                        value      = 0.1;
        final double                        upperLimit = 0.2;
        final double                        lowerLimit = 0.05;
        final ShortTermEncounter2DPOCMethod method     = new Laas2015();

        // WHEN
        final ProbabilityOfCollision probabilityOfCollision = new ProbabilityOfCollision(value, lowerLimit, upperLimit,
                                                                                         method.getName(),
                                                                                         method.isAMaximumProbabilityOfCollisionMethod());

        // THEN
        Assertions.assertEquals(0.1, probabilityOfCollision.getValue());
        Assertions.assertEquals(0.2, probabilityOfCollision.getUpperLimit());
        Assertions.assertEquals(0.05, probabilityOfCollision.getLowerLimit());
        Assertions.assertEquals(new Laas2015().getName(),
                                probabilityOfCollision.getProbabilityOfCollisionMethodName());
        Assertions.assertFalse(probabilityOfCollision.isMaxProbability());
    }

    @Test
    @DisplayName("Test ProbabilityOfCollision constructor with default value for the upper and lower limit")
    void testCreateProbabilityOfCollisionWithDefaultUpperAndLowerLimit() {
        // GIVEN
        final double                        value  = 0.1;
        final ShortTermEncounter2DPOCMethod method = new Laas2015();

        // WHEN
        final ProbabilityOfCollision probabilityOfCollision = new ProbabilityOfCollision(value, method.getName());
        // THEN
        Assertions.assertEquals(0.1, probabilityOfCollision.getValue());
        Assertions.assertEquals(0, probabilityOfCollision.getUpperLimit());
        Assertions.assertEquals(0, probabilityOfCollision.getLowerLimit());
        Assertions.assertEquals(new Laas2015().getName(),
                                probabilityOfCollision.getProbabilityOfCollisionMethodName());
    }

}
