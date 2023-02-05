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
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.modifiers.Bias;

public class RangeBuilderTest extends AbstractGroundMeasurementBuilderTest<Range> {

    private static final double SIGMA = 10.0;
    private static final double BIAS  =  3.0;

    protected MeasurementBuilder<Range> getBuilder(final RandomGenerator random,
                                                   final GroundStation groundStation,
                                                   final ObservableSatellite satellite) {
        final RealMatrix covariance = MatrixUtils.createRealDiagonalMatrix(new double[] { SIGMA * SIGMA });
        MeasurementBuilder<Range> rb =
                        new RangeBuilder(random == null ? null : new CorrelatedRandomVectorGenerator(covariance,
                                                                                                     1.0e-10,
                                                                                                     new GaussianRandomGenerator(random)),
                                         groundStation, true, SIGMA, 1.0, satellite);
        rb.addModifier(new Bias<>(new String[] { "bias" },
                        new double[] { BIAS },
                        new double[] { 1.0 },
                        new double[] { Double.NEGATIVE_INFINITY },
                        new double[] { Double.POSITIVE_INFINITY }));
        return rb;
    }

    @Test
    public void testForward() {
        doTest(0x01e226dd859c2c9dl, 0.4, 0.9, 128, 2.7 * SIGMA);
    }

    @Test
    public void testBackward() {
        doTest(0xd4e49e3716605903l, -0.2, -0.6, 100, 3.2 * SIGMA);
    }

}
