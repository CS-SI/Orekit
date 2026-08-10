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
package org.orekit.estimation.leastsquares;

import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RMSConvergenceCheckerTest {

    @ParameterizedTest
    @ValueSource(doubles = {1.5, 1.9, 1.99, 1.999, 2, 2.05, 2.1, 2.2})
    void testConverge(final double wrms) {
        // GIVEN
        final LeastSquaresProblem.Evaluation evaluation = mock();
        when(evaluation.getRMS()).thenReturn(2.);
        final RMSConvergenceChecker convergenceChecker = new RMSConvergenceChecker(1e-2);
        final LeastSquaresProblem.Evaluation nextEvaluation = mock();
        when(nextEvaluation.getRMS()).thenReturn(wrms);
        // WHEN
        final boolean isConverged = convergenceChecker.converged(0, evaluation, nextEvaluation);
        // THEN
        final boolean expected = FastMath.abs((evaluation.getRMS() - nextEvaluation.getRMS()) / evaluation.getRMS()) < convergenceChecker.getThreshold();
        assertEquals(expected, isConverged);
    }
}
