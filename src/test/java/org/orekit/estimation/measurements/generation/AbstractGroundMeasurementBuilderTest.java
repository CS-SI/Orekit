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

import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Force;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FixedStepSelector;
import org.orekit.time.TimeScalesFactory;

import java.util.SortedSet;

public abstract class AbstractGroundMeasurementBuilderTest<T extends ObservedMeasurement<T>> {

    protected abstract MeasurementBuilder<T> getBuilder(RandomGenerator random,
                                                        GroundStation groundStation,
                                                        ObservableSatellite satellite);

    private Propagator buildPropagator() {
        return EstimationTestUtils.createPropagator(context.initialOrbit, propagatorBuilder);
    }

    protected void doTest(long seed, double startPeriod, double endPeriod, int expectedMeasurements, double tolerance) {
       Generator generator = new Generator();
       final double step = 60.0;
       generator.addPropagator(buildPropagator()); // dummy propagator 1
       generator.addPropagator(buildPropagator()); // dummy propagator 2
       final ObservableSatellite satellite = generator.addPropagator(buildPropagator()); // relevant propagator 3
       generator.addPropagator(buildPropagator()); // dummy propagator 4
       generator.addScheduler(new EventBasedScheduler<>(getBuilder(new Well19937a(seed), context.stations.get(0), satellite),
                                                        new FixedStepSelector(step, TimeScalesFactory.getUTC()),
                                                        generator.getPropagator(satellite),
                                                        new ElevationDetector(context.stations.get(0).getBaseFrame()).
                                                        withConstantElevation(FastMath.toRadians(5.0)).
                                                        withHandler(new ContinueOnEvent()),
                                                        SignSemantic.FEASIBLE_MEASUREMENT_WHEN_POSITIVE));
       final GatheringSubscriber gatherer = new GatheringSubscriber();
       generator.addSubscriber(gatherer);
       final double period = context.initialOrbit.getKeplerianPeriod();
       AbsoluteDate t0     = context.initialOrbit.getDate().shiftedBy(startPeriod * period);
       AbsoluteDate t1     = context.initialOrbit.getDate().shiftedBy(endPeriod   * period);
       generator.generate(t0, t1);
       SortedSet<ObservedMeasurement<?>> measurements = gatherer.getGeneratedMeasurements();
       Assertions.assertEquals(expectedMeasurements, measurements.size());
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
