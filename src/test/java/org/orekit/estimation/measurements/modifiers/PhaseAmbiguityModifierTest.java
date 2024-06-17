/* Copyright 2002-2024 CS GROUP
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.gnss.Phase;
import org.orekit.estimation.measurements.gnss.PhaseMeasurementCreator;
import org.orekit.gnss.PredefinedGnssSignal;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;

import java.util.List;

@Deprecated
public class PhaseAmbiguityModifierTest {

    @Test
    public void testPhaseAmbiguityModifier() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setSelected(true);
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final int    ambiguity1        = 1234;
        final int    ambiguity2        = -45764;
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final double satClockOffset    = 345.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PhaseMeasurementCreator(context,
                                                                                           PredefinedGnssSignal.G01,
                                                                                           ambiguity1,
                                                                                           satClockOffset),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        final PhaseAmbiguityModifier modifier = new PhaseAmbiguityModifier(17, ambiguity2);

        for (final ObservedMeasurement<?> measurement : measurements) {
            final AbsoluteDate date = measurement.getDate();

            final SpacecraftState refState = propagator.propagate(date);

            Phase phase = (Phase) measurement;
            EstimatedMeasurementBase<Phase> evalNoMod = phase.estimateWithoutDerivatives(new SpacecraftState[] { refState });


            // add modifier
            phase.addModifier(modifier);
            EstimatedMeasurementBase<Phase> eval = phase.estimateWithoutDerivatives(new SpacecraftState[] { refState });

            Assertions.assertEquals(ambiguity2, eval.getEstimatedValue()[0] - evalNoMod.getEstimatedValue()[0]);

        }
    }

}
