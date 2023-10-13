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

import java.util.SortedSet;

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
import org.orekit.estimation.measurements.FDOA;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.events.BooleanDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FixedStepSelector;
import org.orekit.time.TimeScalesFactory;

public class FDOABuilderTest {

    // Satellite transmission frequency
    private static final double CENTRE_FREQUENCY = 2.3e9;

    private static final double SIGMA = 0.01;
    private static final double BIAS  = 1.e-8;

    private MeasurementBuilder<FDOA> getBuilder(final RandomGenerator random,
                                                final GroundStation primary,
                                                final GroundStation secondary,
                                                final ObservableSatellite satellite) {
        final RealMatrix covariance = MatrixUtils.createRealDiagonalMatrix(new double[] { SIGMA * SIGMA });
        MeasurementBuilder<FDOA> fdoab =
                        new FDOABuilder(random == null ? null : new CorrelatedRandomVectorGenerator(covariance,
                                                                                                    1.0e-10,
                                                                                                    new GaussianRandomGenerator(random)),
                                        primary, secondary, CENTRE_FREQUENCY, SIGMA, 1.0, satellite);
        fdoab.addModifier(new Bias<>(new String[] { "bias" },
                                     new double[] { BIAS },
                                     new double[] { 1.0 },
                                     new double[] { Double.NEGATIVE_INFINITY },
                                     new double[] { Double.POSITIVE_INFINITY }));
        return fdoab;
    }

    @Test
    public void testForward() {
        doTest(0xf50c0ce7c8c1dab2l, 0.0, 1.2, 3.2 * SIGMA);
    }

    @Test
    public void testBackward() {
        doTest(0x453a681440d01832l, 0.0, -1.2, 2.9 * SIGMA);
    }

    private Propagator buildPropagator() {
        return EstimationTestUtils.createPropagator(context.initialOrbit, propagatorBuilder);
    }

    private void doTest(long seed, double startPeriod, double endPeriod, double tolerance) {
        Generator generator = new Generator();
        final double step = 60.0;
        final GroundStation emitter  = context.FDOAstations.getFirst();
        final GroundStation receiver = context.FDOAstations.getSecond();
        generator.addPropagator(buildPropagator()); // dummy first propagator
        generator.addPropagator(buildPropagator()); // dummy second propagator
        ObservableSatellite satellite = generator.addPropagator(buildPropagator()); // useful third propagator
        generator.addPropagator(buildPropagator()); // dummy fourth propagator
        generator.addScheduler(new EventBasedScheduler<>(getBuilder(new Well19937a(seed), emitter, receiver, satellite),
                                                         new FixedStepSelector(step, TimeScalesFactory.getUTC()),
                                                         generator.getPropagator(satellite),
                                                         BooleanDetector.andCombine(new ElevationDetector(emitter.getBaseFrame()).
                                                                                    withConstantElevation(FastMath.toRadians(5.0)),
                                                                                    new ElevationDetector(receiver.getBaseFrame()).
                                                                                    withConstantElevation(FastMath.toRadians(5.0))),
                                                         SignSemantic.FEASIBLE_MEASUREMENT_WHEN_POSITIVE));
        final GatheringSubscriber gatherer = new GatheringSubscriber();
        generator.addSubscriber(gatherer);
        final double period = context.initialOrbit.getKeplerianPeriod();
        AbsoluteDate t0     = context.initialOrbit.getDate().shiftedBy(startPeriod * period);
        AbsoluteDate t1     = context.initialOrbit.getDate().shiftedBy(endPeriod   * period);
        generator.generate(t0, t1);
        SortedSet<ObservedMeasurement<?>> measurements = gatherer.getGeneratedMeasurements();
        Propagator propagator = buildPropagator();
        double maxError = 0;
        AbsoluteDate previous = null;
        AbsoluteDate tInf = t0.isBefore(t1) ? t0 : t1;
        AbsoluteDate tSup = t0.isBefore(t1) ? t1 : t0;
        for (ObservedMeasurement<?> measurement : measurements) {
            AbsoluteDate date = measurement.getDate();
            double[] m = measurement.getObservedValue();
            Assertions.assertTrue(date.compareTo(tInf) >= 0);
            Assertions.assertTrue(date.compareTo(tSup) <= 0);
            if (previous != null) {
                if (t0.isBefore(t1)) {
                    // measurements are expected to be chronological
                    Assertions.assertTrue(date.durationFrom(previous) >= 0.999999 * step);
                } else {
                    // measurements are expected to be reverse chronological
                    Assertions.assertTrue(previous.durationFrom(date) >= 0.999999 * step);
                }
            }
            previous = date;
            SpacecraftState state = propagator.propagate(date);
            double[] e = measurement.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).getEstimatedValue();
            for (int i = 0; i < m.length; ++i) {
                maxError = FastMath.max(maxError, FastMath.abs(e[i] - m[i]));
            }
        }
        Assertions.assertEquals(0.0, maxError, tolerance);
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
