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
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Force;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.BurstSelector;
import org.orekit.time.TimeScalesFactory;

import java.util.SortedSet;

public class PVBuilderTest {

    private static final double SIGMA_P = 10.0;
    private static final double SIGMA_V =  0.01;
    private static final double BIAS_P  =  5.0;
    private static final double BIAS_V  = -0.003;

    private MeasurementBuilder<PV> getBuilder(final RandomGenerator random, final ObservableSatellite satellite) {
        final RealMatrix covariance = MatrixUtils.createRealDiagonalMatrix(new double[] {
            SIGMA_P * SIGMA_P, SIGMA_P * SIGMA_P, SIGMA_P * SIGMA_P,
            SIGMA_V * SIGMA_V, SIGMA_V * SIGMA_V, SIGMA_V * SIGMA_V,
        });
        MeasurementBuilder<PV> pvb =
                        new PVBuilder(random == null ? null : new CorrelatedRandomVectorGenerator(covariance,
                                                                                                  1.0e-10,
                                                                                                  new GaussianRandomGenerator(random)),
                                      SIGMA_P, SIGMA_V, 1.0, satellite);
        pvb.addModifier(new Bias<>(new String[] { "pxBias", "pyBias", "pzBias", "vxBias", "vyBias", "vzBias" },
                        new double[] { BIAS_P, BIAS_P, BIAS_P, BIAS_V, BIAS_V, BIAS_V },
                        new double[] { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 },
                        new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                                       Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY },
                        new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY }));
        return pvb;
    }

    @Test
    public void testForward() {
        doTest(0x292b6e87436fe4c7l, 0.0, 1.2, 3.7 * SIGMA_P, 3.3 * SIGMA_V);
    }

    @Test
    public void testBackward() {
        doTest(0x2f3285aa70b83c47l, 0.0, -1.0, 3.1 * SIGMA_P, 3.3 * SIGMA_V);
    }

    private Propagator buildPropagator() {
        return EstimationTestUtils.createPropagator(context.initialOrbit, propagatorBuilder);
    }

    private void doTest(long seed, double startPeriod, double endPeriod, double toleranceP, double toleranceV) {
        Generator    generator    = new Generator();
        final int    maxBurstSize = 10;
        final double highRateStep = 5.0;
        final double burstPeriod  = 300.0;

        generator.addPropagator(buildPropagator()); // dummy first propagator
        generator.addPropagator(buildPropagator()); // dummy second propagator
        ObservableSatellite satellite = generator.addPropagator(buildPropagator()); // useful third propagator
        generator.addPropagator(buildPropagator()); // dummy fourth propagator
        generator.addScheduler(new ContinuousScheduler<>(getBuilder(new Well19937a(seed), satellite),
                                                         new BurstSelector(maxBurstSize, highRateStep, burstPeriod,
                                                                           TimeScalesFactory.getUTC())));
        final GatheringSubscriber gatherer = new GatheringSubscriber();
        generator.addSubscriber(gatherer);
        final double period = context.initialOrbit.getKeplerianPeriod();
        AbsoluteDate t0     = context.initialOrbit.getDate().shiftedBy(startPeriod * period);
        AbsoluteDate t1     = context.initialOrbit.getDate().shiftedBy(endPeriod   * period);
        generator.generate(t0, t1);
        SortedSet<ObservedMeasurement<?>> measurements = gatherer.getGeneratedMeasurements();
        Propagator propagator = buildPropagator();
        double maxErrorP = 0;
        double maxErrorV = 0;
        AbsoluteDate previous = null;
        AbsoluteDate tInf = t0.isBefore(t1) ? t0 : t1;
        AbsoluteDate tSup = t0.isBefore(t1) ? t1 : t0;
        int count = 0;
        for (ObservedMeasurement<?> measurement : measurements) {
            AbsoluteDate date = measurement.getDate();
            double[] m = measurement.getObservedValue();
            Assertions.assertTrue(date.compareTo(tInf) >= 0);
            Assertions.assertTrue(date.compareTo(tSup) <= 0);
            if (previous != null) {
                // measurements are always chronological, even with backward propagation,
                // due to the SortedSet (which is intended for combining several
                // measurements types with different builders and schedulers)
                final double expected = (count % maxBurstSize == 0) ?
                                        burstPeriod - (maxBurstSize - 1) * highRateStep :
                                        highRateStep;
                if (t0.isBefore(t1)) {
                    // measurements are expected to be chronological
                    Assertions.assertEquals(expected, date.durationFrom(previous), 1.0e-10 * expected);
                } else {
                    // measurements are expected to be reverse chronological
                    Assertions.assertEquals(expected, previous.durationFrom(date), 1.0e-10 * expected);
                }
            }
            previous = date;
            ++count;
            SpacecraftState state = propagator.propagate(date);
            double[] e = measurement.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).getEstimatedValue();
            for (int i = 0; i < 3; ++i) {
                maxErrorP = FastMath.max(maxErrorP, FastMath.abs(e[i] - m[i]));
            }
            for (int i = 3; i < m.length; ++i) {
                maxErrorV = FastMath.max(maxErrorV, FastMath.abs(e[i] - m[i]));
            }
        }
        Assertions.assertEquals(0.0, maxErrorP, toleranceP);
        Assertions.assertEquals(0.0, maxErrorV, toleranceV);
     }

     @BeforeEach
     public void setUp() {
         context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

         propagatorBuilder = context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                                   1.0e-6, 300.0, 0.001, Force.POTENTIAL,
                                                   Force.THIRD_BODY_SUN, Force.THIRD_BODY_MOON);
     }

     Context context;
     NumericalPropagatorBuilder propagatorBuilder;

}
