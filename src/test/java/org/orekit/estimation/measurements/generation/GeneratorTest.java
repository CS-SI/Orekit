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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Force;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FixedStepSelector;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import java.util.SortedSet;

public class GeneratorTest {

    @Test
    public void testIssue557() {

        final EventDetector detector = new ElevationDetector(context.stations.get(0).getBaseFrame());

        double[] azElError = new double[] {
            FastMath.toRadians(0.015),
            FastMath.toRadians(0.015)
        };
        double[] baseweight = new double[] {
            1.0,
            1.0
        };

        double rangeSigma = 40.0;
        double rangeBW = 1;
        ObservableSatellite obs = new ObservableSatellite(0);
        RangeBuilder rB = new RangeBuilder(null, context.stations.get(0), false, rangeSigma, rangeBW,obs);
        AngularAzElBuilder aAEB = new AngularAzElBuilder(null, context.stations.get(0), azElError, baseweight, obs);
        double  timeToEnd = Constants.JULIAN_DAY;

        AbsoluteDate initialDate = context.initialOrbit.getDate();
        AbsoluteDate finalDate = initialDate.shiftedBy(timeToEnd);
        Propagator numProp = EstimationTestUtils.createPropagator(context.initialOrbit, propagatorBuilder);
        FixedStepSelector fssAE = new FixedStepSelector(10., TimeScalesFactory.getUTC());
        EventBasedScheduler<Range> eBS = new EventBasedScheduler<>(rB, fssAE, numProp, detector, SignSemantic.FEASIBLE_MEASUREMENT_WHEN_NEGATIVE);
        FixedStepSelector fssR = new FixedStepSelector(10., TimeScalesFactory.getUTC());
        EventBasedScheduler<AngularAzEl> aeBS = new EventBasedScheduler<>(aAEB, fssR, numProp, detector, SignSemantic.FEASIBLE_MEASUREMENT_WHEN_NEGATIVE);
        Generator genR = new Generator();
        genR.addPropagator(numProp);
        genR.addScheduler(aeBS);
        genR.addScheduler(eBS);
        final GatheringSubscriber gatherer = new GatheringSubscriber();
        genR.addSubscriber(gatherer);

        genR.generate(initialDate, finalDate);
        SortedSet<ObservedMeasurement<?>> generated = gatherer.getGeneratedMeasurements();

        int nbAzEl  = 0;
        int nbRange = 0;
        for (final ObservedMeasurement<?> m : generated) {
            if (m.getMeasurementType().equals(AngularAzEl.MEASUREMENT_TYPE)) {
                ++nbAzEl;
            } else if (m.getMeasurementType().equals(Range.MEASUREMENT_TYPE)) {
                ++nbRange;
            } else {
                Assertions.fail("unexpected measurement type: " + m.getClass().getSimpleName());
            }
        }
        Assertions.assertEquals(740, nbAzEl);
        Assertions.assertEquals(740, nbRange);

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
