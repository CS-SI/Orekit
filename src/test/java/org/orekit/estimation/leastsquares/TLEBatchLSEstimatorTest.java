/* Copyright 2002-2021 CS GROUP
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
package org.orekit.estimation.leastsquares;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem.Evaluation;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.TLEContext;
import org.orekit.estimation.TLEEstimationTestUtils;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.TLERangeMeasurementCreator;
import org.orekit.estimation.measurements.TLERangeRateMeasurementCreator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class TLEBatchLSEstimatorTest {

    /**
     * Perfect PV measurements with a perfect start
     */
    @Test
    public void testPV() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 1.0);

        // create perfect PV measurements
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        TLEEstimationTestUtils.checkFit(context, estimator, 1, 2,
                                     0.0, 9.44e-08,
                                     0.0, 3.57e-07,
                                     0.0, 7.81e-09,
                                     0.0, 2.74e-12);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assert.assertEquals(6,       normalizedCovariances.getRowDimension());
        Assert.assertEquals(6,       normalizedCovariances.getColumnDimension());
        Assert.assertEquals(6,       physicalCovariances.getRowDimension());
        Assert.assertEquals(6,       physicalCovariances.getColumnDimension());
        Assert.assertEquals(0.03071, physicalCovariances.getEntry(0, 0), 1.0e-5);

    }
    
    /** Test PV measurements generation and backward propagation in least-square orbit determination. */
    @Test
    public void testPVBackward() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 1.0);

        // create perfect PV measurements
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, -1.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        TLEEstimationTestUtils.checkFit(context, estimator, 1, 2,
                                     0.0, 4.35e-09,
                                     0.0, 3.08e-08,
                                     0.0, 6.99e-10,
                                     0.0, 1.46e-12);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assert.assertEquals(6,       normalizedCovariances.getRowDimension());
        Assert.assertEquals(6,       normalizedCovariances.getColumnDimension());
        Assert.assertEquals(6,       physicalCovariances.getRowDimension());
        Assert.assertEquals(6,       physicalCovariances.getColumnDimension());
        Assert.assertEquals(0.03420, physicalCovariances.getEntry(0, 0), 1.0e-5);

    }

    /**
     * Perfect range measurements with a biased start
     */
    @Test
    public void testRange() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 1.0);

        // create perfect range measurements
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TLERangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> range : measurements) {
            estimator.addMeasurement(range);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(30);
        estimator.setMaxEvaluations(30);
        estimator.setObserver(new BatchLSObserver() {
            int lastIter = 0;
            int lastEval = 0;
            /** {@inheritDoc} */
            @Override
            public void evaluationPerformed(int iterationsCount, int evaluationscount,
                                            Orbit[] orbits,
                                            ParameterDriversList estimatedOrbitalParameters,
                                            ParameterDriversList estimatedPropagatorParameters,
                                            ParameterDriversList estimatedMeasurementsParameters,
                                            EstimationsProvider evaluationsProvider, Evaluation lspEvaluation) {
                if (iterationsCount == lastIter) {
                    Assert.assertEquals(lastEval + 1, evaluationscount);
                } else {
                    Assert.assertEquals(lastIter + 1, iterationsCount);
                }
                lastIter = iterationsCount;
                lastEval = evaluationscount;
                Assert.assertEquals(measurements.size(), evaluationsProvider.getNumber());
                try {
                    evaluationsProvider.getEstimatedMeasurement(-1);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assert.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
                }
                try {
                    evaluationsProvider.getEstimatedMeasurement(measurements.size());
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assert.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
                }
                AbsoluteDate previous = AbsoluteDate.PAST_INFINITY;
                for (int i = 0; i < evaluationsProvider.getNumber(); ++i) {
                    AbsoluteDate current = evaluationsProvider.getEstimatedMeasurement(i).getDate();
                    Assert.assertTrue(current.compareTo(previous) >= 0);
                    previous = current;
                }
            }
        });

        ParameterDriver xDriver = estimator.getOrbitalParametersDrivers(true).getDrivers().get(0);
        Assert.assertEquals(OrbitType.POS_X, xDriver.getName());
        xDriver.setValue(xDriver.getValue() + 10.0);
        xDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        TLEEstimationTestUtils.checkFit(context, estimator, 2, 3,
                                        0.0, 2.29e-6,
                                        0.0, 4.90e-6,
                                        0.0, 2.75e-6,
                                        0.0, 2.84e-10);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if (OrbitType.POS_X.equals(driver.getName())) {
                // user-specified reference date
                Assert.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assert.assertEquals(0, driver.getReferenceDate().durationFrom(propagatorBuilder.getInitialOrbitDate()), 1.0e-15);
            }
        }
    }

    @Test
    public void testWrappedException() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 1.0);

        // create perfect range measurements
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                               propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TLERangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> range : measurements) {
            estimator.addMeasurement(range);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(50);
        estimator.setObserver(new BatchLSObserver() {
            /** {@inheritDoc} */
            @Override
            public void evaluationPerformed(int iterationsCount, int evaluationscount,
                                           Orbit[] orbits,
                                           ParameterDriversList estimatedOrbitalParameters,
                                           ParameterDriversList estimatedPropagatorParameters,
                                           ParameterDriversList estimatedMeasurementsParameters,
                                           EstimationsProvider evaluationsProvider, Evaluation lspEvaluation) throws DummyException {
                throw new DummyException();
            }
        });

        try {
            TLEEstimationTestUtils.checkFit(context, estimator, 3, 4,
                                         0.0, 1.5e-6,
                                         0.0, 3.2e-6,
                                         0.0, 3.8e-7,
                                         0.0, 1.5e-10);
            Assert.fail("an exception should have been thrown");
        } catch (DummyException de) {
            // expected
        }

    }

    private static class DummyException extends OrekitException {
        private static final long serialVersionUID = 1L;
        public DummyException() {
            super(OrekitMessages.INTERNAL_ERROR);
        }
    }

    /**
     * Perfect range and range rate measurements with a perfect start
     */
    @Test
    public void testRangeAndRangeRate() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 1.0);

        // create perfect range measurements
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);

        final List<ObservedMeasurement<?>> measurementsRange =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TLERangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        final List<ObservedMeasurement<?>> measurementsRangeRate =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new TLERangeRateMeasurementCreator(context, false),
                                                               1.0, 3.0, 300.0);

        // concat measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        measurements.addAll(measurementsRange);
        measurements.addAll(measurementsRangeRate);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> meas : measurements) {
            estimator.addMeasurement(meas);
        }
        estimator.setParametersConvergenceThreshold(1.0e-3);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        // we have low correlation between the two types of measurement. We can expect a good estimate.
        TLEEstimationTestUtils.checkFit(context, estimator, 3, 6,
                                     0.0, 0.26,
                                     0.0, 0.52,
                                     0.0, 4.42e-4,
                                     0.0, 1.46e-7);
    }

}
