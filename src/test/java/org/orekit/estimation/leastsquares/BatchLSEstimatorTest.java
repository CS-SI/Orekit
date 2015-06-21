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
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.RangeMeasurementCreator;
import org.orekit.orbits.CartesianOrbit;
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
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

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
        estimator.setConvergenceThreshold(1.0e-14, 1.0e-12);
        estimator.setMaxIterations(20);

        checkFit(context, estimator, 4, 1.1e-8, 6.7e-8, 3.0e-9, 3.1e-12);

    }

    @Test
    public void testKeplerDistances() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE,
                                              1.0e-6, 60.0, 0.001);

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
        estimator.setConvergenceThreshold(1.0e-14, 1.0e-12);
        estimator.setMaxIterations(20);

        checkFit(context, estimator, 4, 4.8e-7, 9.0e-7, 6.1e-7, 2.4e-10);

    }

    private void checkFit(final Context context, final BatchLSEstimator estimator,
                          final int iterations, final double rmsEps, final double maxEps,
                          final double posEps, final double velEps)
        throws OrekitException {

        // estimate orbit, starting from a wrong point
        final Vector3D initialPosition = context.initialOrbit.getPVCoordinates().getPosition();
        final Vector3D initialVelocity = context.initialOrbit.getPVCoordinates().getVelocity();
        final Vector3D wrongPosition   = initialPosition.add(new Vector3D(1000.0, 0, 0));
        final Vector3D wrongVelocity   = initialVelocity.add(new Vector3D(0, 0, 0.01));
        final Orbit   wrongOrbit       = new CartesianOrbit(new PVCoordinates(wrongPosition, wrongVelocity),
                                                            context.initialOrbit.getFrame(),
                                                            context.initialOrbit.getDate(),
                                                            context.initialOrbit.getMu());
        final Orbit estimatedOrbit = estimator.estimate(wrongOrbit);
        final Vector3D estimatedPosition = estimatedOrbit.getPVCoordinates().getPosition();
        final Vector3D estimatedVelocity = estimatedOrbit.getPVCoordinates().getVelocity();

        Assert.assertEquals(iterations, estimator.getIterations());

        int    k   = 0;
        double sum = 0;
        double max = 0;
        for (final Map.Entry<Measurement, Evaluation> entry :
             estimator.getLastEvaluations().entrySet()) {
            final Measurement m           = entry.getKey();
            final Evaluation  e           = entry.getValue();
            final double[]    weight      = m.getBaseWeight();
            final double[]    sigma       = m.getTheoreticalStandardDeviation();
            final double[]    observed    = m.getObservedValue();
            final double[]    theoretical = e.getValue();
            for (int i = 0; i < m.getDimension(); ++i) {
                final double weightedResidual = weight[i] * (theoretical[i] - observed[i]) / sigma[i];
                ++k;
                sum += weightedResidual * weightedResidual;
                max = FastMath.max(max, FastMath.abs(weightedResidual));
            }
        }

        Assert.assertEquals(0.0, FastMath.sqrt(sum / k), rmsEps);
        Assert.assertEquals(0.0, max, maxEps);
        Assert.assertEquals(0.0, Vector3D.distance(initialPosition, estimatedPosition), posEps);
        Assert.assertEquals(0.0, Vector3D.distance(initialVelocity, estimatedVelocity), velEps);

    }

}


