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

import java.util.List;

import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.gnss.Phase;
import org.orekit.estimation.measurements.gnss.PhaseMeasurementCreator;
import org.orekit.gnss.Frequency;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;

public class ShapiroPhaseModifierTest {

    /** Frequency of the measurements. */
    private static final Frequency FREQUENCY = Frequency.G01;

    @Test
    public void testShapiro() {
        doTestShapiro(0.006850703, 0.008320738, 0.010297509);
    }

    private void doTestShapiro(final double expectedMin, final double expectedMean, final double expectedMax) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final int    ambiguity         = 1234;
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final double satClockOffset    = 345.0e-6;
        List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PhaseMeasurementCreator(context, FREQUENCY,
                                                                                           ambiguity,
                                                                                           satClockOffset),
                                                               1.0, 3.0, 300.0);

        propagator.clearStepHandlers();


        final ShapiroPhaseModifier modifier = new ShapiroPhaseModifier(context.initialOrbit.getMu());

        DescriptiveStatistics stat = new DescriptiveStatistics();
        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refstate = propagator.propagate(date);

            Phase phase = (Phase) measurement;
            EstimatedMeasurementBase<Phase> evalNoMod = phase.estimateWithoutDerivatives(12, 17, new SpacecraftState[] { refstate });
            Assertions.assertEquals(12, evalNoMod.getIteration());
            Assertions.assertEquals(17, evalNoMod.getCount());

            // add modifier
            phase.addModifier(modifier);
            boolean found = false;
            for (final EstimationModifier<Phase> existing : phase.getModifiers()) {
                found = found || existing == modifier;
            }
            Assertions.assertTrue(found);
            EstimatedMeasurementBase<Phase> eval = phase.estimateWithoutDerivatives(0, 0,  new SpacecraftState[] { refstate });

            stat.addValue(eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0]);

        }

        // wavelength
        final double wavelength = ((Phase) measurements.get(0)).getWavelength();

        Assertions.assertEquals(expectedMin,  stat.getMin() * wavelength,  1.0e-9);
        Assertions.assertEquals(expectedMean, stat.getMean() * wavelength, 1.0e-9);
        Assertions.assertEquals(expectedMax,  stat.getMax() * wavelength,  1.0e-9);

    }

}


