/* Copyright 2002-2015 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.leastsquares;

import java.util.List;
import java.util.Map;

import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeMeasurementCreator;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.utils.PVCoordinates;

public class BatchLSEstimatorTest {

    @Test
    public void testKeplerPV() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE);

        // create perfect PV measurements
        final List<Measurement> measurements =
                        EstimationTestUtils.createMeasurements(context, propagatorBuilder,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(propagatorBuilder,
                                                                new LevenbergMarquardtOptimizer());
        for (final Measurement measurement : measurements) {
            estimator.addMeasurement(measurement);
        }
        estimator.setConvergenceThreshold(1.0e-12, 1.0e-12);
        estimator.setMaxIterations(20);

        // estimate orbit, starting from a wrong point
        Vector3D position = context.initialOrbit.getPVCoordinates().getPosition().add(new Vector3D(100.0, 0, 0));
        Vector3D velocity = context.initialOrbit.getPVCoordinates().getVelocity().add(new Vector3D(0, 0, 0.01));
        Orbit wrongOrbit  = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                               context.initialOrbit.getFrame(),
                                               context.initialOrbit.getDate(),
                                               context.initialOrbit.getMu());
        Orbit estimated   = estimator.estimate(wrongOrbit);
        for (final Map.Entry<Measurement, Evaluation> entry :
             estimator.getLastEvaluations().entrySet()) {
            PV pv = (PV) entry.getKey();
            System.out.format(java.util.Locale.US, "%s %13.3f %13.3f %13.3f  %13.6f %13.6f %13.6f%n",
                              pv.getDate(),
                              pv.getObservedValue()[0] - entry.getValue().getValue()[0],
                              pv.getObservedValue()[1] - entry.getValue().getValue()[1],
                              pv.getObservedValue()[2] - entry.getValue().getValue()[2],
                              pv.getObservedValue()[3] - entry.getValue().getValue()[3],
                              pv.getObservedValue()[4] - entry.getValue().getValue()[4],
                              pv.getObservedValue()[5] - entry.getValue().getValue()[5]);
        }
        System.out.println(context.initialOrbit);
        System.out.println(wrongOrbit);
        System.out.println(estimated);

    }

    @Test
    public void testKeplerDistances() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE);

        // create perfect range measurements
        final List<Measurement> measurements =
                        EstimationTestUtils.createMeasurements(context, propagatorBuilder,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(propagatorBuilder,
                                                                new LevenbergMarquardtOptimizer());
        for (final Measurement range : measurements) {
            estimator.addMeasurement(range);
        }
        estimator.setConvergenceThreshold(1.0e-10, 1.0e-10);
        estimator.setMaxIterations(20);

        // estimate orbit, starting from a wrong point
        Vector3D position = context.initialOrbit.getPVCoordinates().getPosition().add(new Vector3D(100.0, 0, 0));
        Vector3D velocity = context.initialOrbit.getPVCoordinates().getVelocity().add(new Vector3D(0, 0, 0.1));
        Orbit wrongOrbit  = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                               context.initialOrbit.getFrame(),
                                               context.initialOrbit.getDate(),
                                               context.initialOrbit.getMu());
        Orbit estimated   = estimator.estimate(wrongOrbit);
        for (final Map.Entry<Measurement, Evaluation> entry :
             estimator.getLastEvaluations().entrySet()) {
            Range range = (Range) entry.getKey();
            System.out.println(range.getDate() +
                               " " + range.getStation().getBaseFrame().getName() +
                               " " + range.getObservedValue()[0] +
                               " " + entry.getValue().getValue()[0] +
                               " " + (range.getObservedValue()[0] - entry.getValue().getValue()[0]));
        }
        System.out.println(context.initialOrbit);
        System.out.println(wrongOrbit);
        System.out.println(estimated);

    }

}


