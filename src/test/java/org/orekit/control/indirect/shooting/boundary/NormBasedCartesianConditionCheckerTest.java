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
package org.orekit.control.indirect.shooting.boundary;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.utils.PVCoordinates;

class NormBasedCartesianConditionCheckerTest {

    @Test
    void testIsConvergedTrivial() {
        // GIVEN
        final int expectedMaximumCount = 10;
        final double toleranceDistance = 0.;
        final double toleranceSpeed = 0.;
        final NormBasedCartesianConditionChecker convergenceChecker = new NormBasedCartesianConditionChecker(expectedMaximumCount,
                toleranceDistance, toleranceSpeed);
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.MINUS_J, Vector3D.MINUS_K);
        // WHEN & THEN
        Assertions.assertFalse(convergenceChecker.isConverged(pvCoordinates, pvCoordinates));
        Assertions.assertEquals(expectedMaximumCount, convergenceChecker.getMaximumIterationCount());
    }

    @Test
    void testIsConvergedPosition() {
        // GIVEN
        final int expectedMaximumCount = 10;
        final double toleranceDistance = 10.;
        final double toleranceSpeed = Double.POSITIVE_INFINITY;
        final NormBasedCartesianConditionChecker convergenceChecker = new NormBasedCartesianConditionChecker(expectedMaximumCount,
                toleranceDistance, toleranceSpeed);
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.MINUS_J.scalarMultiply(5.), Vector3D.ZERO);
        // WHEN & THEN
        Assertions.assertTrue(convergenceChecker.isConverged(pvCoordinates, new PVCoordinates()));
    }

    @Test
    void testIsConvergedVelocity() {
        // GIVEN
        final int expectedMaximumCount = 10;
        final double toleranceDistance = 10.;
        final double toleranceSpeed = 1.;
        final NormBasedCartesianConditionChecker convergenceChecker = new NormBasedCartesianConditionChecker(expectedMaximumCount,
                toleranceDistance, toleranceSpeed);
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.ZERO, Vector3D.PLUS_I.scalarMultiply(2.));
        // WHEN & THEN
        Assertions.assertFalse(convergenceChecker.isConverged(pvCoordinates, new PVCoordinates()));
    }
}
