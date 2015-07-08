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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Parameter;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.EvaluationModifier;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;

public class BatchLSEstimatorTest {

    @Test
    public void testKeplerPV() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

        // create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<Measurement> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(propagatorBuilder,
                                                                new LevenbergMarquardtOptimizer());
        for (final Measurement measurement : measurements) {
            estimator.addMeasurement(measurement);
        }
        estimator.setConvergenceThreshold(1.0e-14, 1.0e-12);
        estimator.setMaxIterations(20);

        EstimationTestUtils.checkFit(context, estimator, 4,
                                     0.0, 1.1e-8,
                                     0.0, 6.7e-8,
                                     0.0, 3.0e-9,
                                     0.0, 3.1e-12);

    }

    @Test
    public void testKeplerRange() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<Measurement> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(propagatorBuilder,
                                                                new LevenbergMarquardtOptimizer());
        for (final Measurement range : measurements) {
            estimator.addMeasurement(range);
        }
        estimator.setConvergenceThreshold(1.0e-14, 1.0e-12);
        estimator.setMaxIterations(20);

        EstimationTestUtils.checkFit(context, estimator, 4,
                                     0.0, 6.1e-7,
                                     0.0, 1.2e-6,
                                     0.0, 5.3e-7,
                                     0.0, 2.1e-10);

    }

    @Test
    public void testKeplerRangeRate() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<Measurement> measurements1 =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        //final List<Measurement> measurements2 =
        //        EstimationTestUtils.createMeasurements(propagator,
        //                                               new RangeMeasurementCreator(context),
        //                                               1.0, 3.0, 300.0);
        
        final List<Measurement> measurements = new ArrayList<Measurement>();
        measurements.addAll(measurements1);
        //measurements.addAll(measurements2);
        
        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(propagatorBuilder,
                                                                new LevenbergMarquardtOptimizer());
        for (final Measurement rangerate : measurements) {
            estimator.addMeasurement(rangerate);
        }
        estimator.setConvergenceThreshold(1.0e-14, 1.0e-12);
        estimator.setMaxIterations(20);

        EstimationTestUtils.checkFit(context, estimator, 5,
                                     0.0, 6.1e-7,
                                     0.0, 1.2e-6,
                                     0.0, 5.3e-7,
                                     0.0, 2.1e-10);

    }
    
    @Test
    public void testDuplicatedMeasurementParameter() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

        final BatchLSEstimator estimator = new BatchLSEstimator(propagatorBuilder,
                                                                new LevenbergMarquardtOptimizer());
        final Measurement measurement = new Range(context.stations.get(0),
                                                  context.initialOrbit.getDate(),
                                                  1.0e6, 10.0, 1.0);
        final String duplicatedName = "duplicated";
        measurement.addModifier(new EvaluationModifier() {            
            @Override
            public void modify(Evaluation evaluation) {
            }
            
            @Override
            public List<Parameter> getSupportedParameters() {
                return Arrays.asList(new Parameter(duplicatedName) {
                                         protected void valueChanged(double[] newValue) {
                                         }
                                     }, new Parameter(duplicatedName) {
                                         protected void valueChanged(double[] newValue) {
                                         }
                                     });
            }
        });
        try {
            estimator.addMeasurement(measurement);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.DUPLICATED_PARAMETER_NAME, oe.getSpecifier());
            Assert.assertEquals(duplicatedName, (String) oe.getParts()[0]);
        }

    }

}


