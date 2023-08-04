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
package org.orekit.estimation.measurements.generation;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.estimation.measurements.modifiers.Bias;

public class RangeRateBuilderTest extends AbstractGroundMeasurementBuilderTest<RangeRate> {

    private static final double SIGMA = 0.01;
    private static final double BIAS  = 0.002;

    protected MeasurementBuilder<RangeRate> getBuilder(final RandomGenerator random,
                                                       final GroundStation groundStation,
                                                       final ObservableSatellite satellite) {
        final RealMatrix covariance = MatrixUtils.createRealDiagonalMatrix(new double[] { SIGMA * SIGMA });
        MeasurementBuilder<RangeRate> rrb =
                        new RangeRateBuilder(random == null ? null : new CorrelatedRandomVectorGenerator(covariance,
                                                                                                         1.0e-10,
                                                                                                         new GaussianRandomGenerator(random)),
                                             groundStation, true, SIGMA, 1.0, satellite);
        rrb.addModifier(new Bias<>(new String[] { "bias" },
                        new double[] { BIAS },
                        new double[] { 1.0 },
                        new double[] { Double.NEGATIVE_INFINITY },
                        new double[] { Double.POSITIVE_INFINITY }));
        return rrb;
    }

    @Test
    public void testForward() {
        doTest(0x02c925b8812d8992l, 0.4, 0.9, 128, 2.4 * SIGMA);
    }

    @Test
    public void testBackward() {
        doTest(0x34ce85d26d51cd91l, -0.2, -0.6, 100, 3.3 * SIGMA);
    }

}
