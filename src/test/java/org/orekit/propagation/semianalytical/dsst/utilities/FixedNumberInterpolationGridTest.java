/* Copyright 2022-2026 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.semianalytical.dsst.utilities;

import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FixedNumberInterpolationGridTest {

    @Test
    void testComputeGridPoints() {
        // Initialize
        final int numberOfPoints = 10;
        final FixedNumberInterpolationGrid grid = new FixedNumberInterpolationGrid(numberOfPoints);
        // Action
        final double[] gridPoints = grid.getGridPoints(100.0, 200.0);
        // Verify
        Assertions.assertEquals(numberOfPoints, gridPoints.length);
        Assertions.assertEquals(100.0, gridPoints[0], 1e-10);
        Assertions.assertEquals(200.0, gridPoints[9], 1e-10);
    }

    @Test
    void testComputeGridPointsField() {
        // Initialize
        Binary64 zero = Binary64.ZERO;
        final int numberOfPoints = 10;
        final FixedNumberInterpolationGrid grid = new FixedNumberInterpolationGrid(numberOfPoints);
        // Action
        final Binary64[] gridPoints = grid.getGridPoints(zero.add(100.0), zero.add(200.0));
        // Verify
        Assertions.assertEquals(numberOfPoints, gridPoints.length);
        Assertions.assertEquals(100.0, gridPoints[0].getReal(), 1e-10);
        Assertions.assertEquals(200.0, gridPoints[9].getReal(), 1e-10);
    }
}