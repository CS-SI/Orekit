/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation.measurements;

import java.util.List;

import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.utils.ParameterDriver;

public class BiasTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testEstimateBias() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<Measurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // create range biases: one bias for each station
        final RandomGenerator random = new Well19937a(0x0c4b69da5d64b35al);
        final Bias<?>[] stationsRangeBiases = new Bias<?>[context.stations.size()];
        final double[] realStationsBiases  = new double[context.stations.size()];
        for (int i = 0; i < context.stations.size(); ++i) {
            final TopocentricFrame base = context.stations.get(i).getBaseFrame();
            stationsRangeBiases[i] = new Bias<Range>(new String[] {
                                                         base.getName() + " range bias"
                                                     },
                                                     new double[] {
                                                         0.0       
                                                     },
                                                     new double[] {
                                                         1.0       
                                                     });
            realStationsBiases[i]  = 2 * random.nextDouble() - 1;
        }

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(propagatorBuilder,
                                                                new LevenbergMarquardtOptimizer());

        // add the measurements, with both spacecraft and stations biases
        for (final Measurement<?> measurement : measurements) {
            final Range range = (Range) measurement;
            for (int i = 0; i < context.stations.size(); ++i) {
                if (range.getStation() == context.stations.get(i)) {
                    double biasedRange = range.getObservedValue()[0] + realStationsBiases[i];
                    final Range m = new Range(range.getStation(),
                                              range.getDate(),
                                              biasedRange,
                                              range.getTheoreticalStandardDeviation()[0],
                                              range.getBaseWeight()[0]);
                    m.addModifier((Bias<Range>) stationsRangeBiases[i]);
                    estimator.addMeasurement(m);
                }
            }
        }

        estimator.setConvergenceThreshold(1.0e-14, 1.0e-12);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        // we want to estimate the biases
        for (Bias<?> bias : stationsRangeBiases) {
            for (final ParameterDriver driver : bias.getParametersDrivers()) {
                driver.setSelected(true);
            }
        }

        EstimationTestUtils.checkFit(context, estimator, 3, 4,
                                     0.0,  9.8e-7,
                                     0.0,  2.3e-6,
                                     0.0,  2.9e-7,
                                     0.0,  1.3e-10);
        for (int i = 0; i < stationsRangeBiases.length; ++i) {
            Assert.assertEquals(realStationsBiases[i],
                                stationsRangeBiases[i].getParametersDrivers().get(0).getValue(),
                                5.9e-8);
        }

    }

}


