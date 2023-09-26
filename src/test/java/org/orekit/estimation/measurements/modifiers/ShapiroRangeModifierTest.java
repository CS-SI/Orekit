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
package org.orekit.estimation.measurements.modifiers;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;

public class ShapiroRangeModifierTest {

    @Test
    public void testShapiroOneWay() {
        doTestShapiro(false, 0.006850703, 0.008320738, 0.010297508);
    }

    @Test
    public void testShapiroTwoWay() {
        doTestShapiro(true, 0.006850703, 0.008320739, 0.010297503);
    }

    private void doTestShapiro(final boolean twoWay,
                               final double expectedMin, final double expectedMean, final double expectedMax) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        if (!twoWay) {
            // convert default two way measurements to one way measurements
            final List<ObservedMeasurement<?>> converted = new ArrayList<>();
            for (final ObservedMeasurement<?> m : measurements) {
                final Range range = (Range) m;
                converted.add(new Range(range.getStation(), false, range.getDate(),
                                        range.getObservedValue()[0],
                                        range.getTheoreticalStandardDeviation()[0],
                                        range.getBaseWeight()[0],
                                        range.getSatellites().get(0)));
            }
            measurements = converted;
        }
        propagator.clearStepHandlers();


        final ShapiroRangeModifier modifier = new ShapiroRangeModifier(context.initialOrbit.getMu());

        DescriptiveStatistics stat = new DescriptiveStatistics();
        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            Range range = (Range) measurement;
            EstimatedMeasurementBase<Range> evalNoMod = range.estimateWithoutDerivatives(12, 17, new SpacecraftState[] { refstate });
            Assertions.assertEquals(12, evalNoMod.getIteration());
            Assertions.assertEquals(17, evalNoMod.getCount());

            // add modifier
            range.addModifier(modifier);
            boolean found = false;
            for (final EstimationModifier<Range> existing : range.getModifiers()) {
                found = found || existing == modifier;
            }
            Assertions.assertTrue(found);
            EstimatedMeasurementBase<Range> eval = range.estimateWithoutDerivatives(0, 0,  new SpacecraftState[] { refstate });

            stat.addValue(eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0]);

        }

        Assertions.assertEquals(expectedMin,  stat.getMin(),  1.0e-9);
        Assertions.assertEquals(expectedMean, stat.getMean(), 1.0e-9);
        Assertions.assertEquals(expectedMax,  stat.getMax(),  1.0e-9);

    }

}


