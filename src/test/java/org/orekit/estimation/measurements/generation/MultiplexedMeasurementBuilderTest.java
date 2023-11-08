/* Copyright 2023 Luc Maisonobe
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

import java.util.Arrays;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.MultiplexedMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.modifiers.Bias;

public class MultiplexedMeasurementBuilderTest extends AbstractGroundMeasurementBuilderTest<MultiplexedMeasurement> {

    private static final double RANGE_SIGMA = 10.0;
    private static final double RANGE_BIAS  =  3.0;
    private static final double AZEL_SIGMA  = 1.0e-3;
    private static final double AZEL_BIAS   = 1.0e-4;

    protected MeasurementBuilder<MultiplexedMeasurement> getBuilder(final RandomGenerator random,
                                                                    final GroundStation groundStation,
                                                                    final ObservableSatellite satellite) {
        final RealMatrix rangeCovariance = MatrixUtils.createRealDiagonalMatrix(new double[] { RANGE_SIGMA * RANGE_SIGMA });
        MeasurementBuilder<Range> rb =
                        new RangeBuilder(random == null ? null : new CorrelatedRandomVectorGenerator(rangeCovariance,
                                                                                                     1.0e-10,
                                                                                                     new GaussianRandomGenerator(random)),
                                         groundStation, true, RANGE_SIGMA, 1.0, satellite);

        final RealMatrix azelCovariance = MatrixUtils.createRealDiagonalMatrix(new double[] {
            AZEL_SIGMA * AZEL_SIGMA, AZEL_SIGMA * AZEL_SIGMA
        });
        MeasurementBuilder<AngularAzEl> ab =
                        new AngularAzElBuilder(random == null ? null : new CorrelatedRandomVectorGenerator(azelCovariance,
                                                                                                           1.0e-10,
                                                                                                           new GaussianRandomGenerator(random)),
                                               groundStation, new double[] { AZEL_SIGMA, AZEL_SIGMA}, new double[] { 1.0, 1.0 },
                                               satellite);

        MultiplexedMeasurementBuilder mb = new MultiplexedMeasurementBuilder(Arrays.asList(rb, ab ));
        mb.addModifier(new Bias<>(new String[] { "rBias", "aBias", "eBias" },
                                  new double[] { RANGE_BIAS, AZEL_BIAS, AZEL_BIAS },
                                  new double[] { 1.0, 1.0, 1.0 },
                                  new double[] { Double.NEGATIVE_INFINITY, -FastMath.PI, -0.5 * FastMath.PI },
                                  new double[] { Double.POSITIVE_INFINITY, +FastMath.PI, +0.5 * FastMath.PI }));

      return mb;

    }

    @Test
    public void testForward() {
        doTest(0xb715507647d63318l, 0.4, 0.9, 128, 2.7 * RANGE_SIGMA);
    }

    @Test
    public void testBackward() {
        doTest(0xceac6c6c358e95d0l, -0.2, -0.6, 100, 2.8 * RANGE_SIGMA);
    }

}
