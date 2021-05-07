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
import org.orekit.estimation.EcksteinHechlerContext;
import org.orekit.estimation.EcksteinHechlerEstimationTestUtils;
import org.orekit.estimation.measurements.EcksteinHechlerRangeMeasurementCreator;
import org.orekit.estimation.measurements.EcksteinHechlerRangeRateMeasurementCreator;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.EcksteinHechlerPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class EcksteinHechlerBatchLSEstimatorTest {

    /**
     * Perfect PV measurements with a perfect start
     */
    @Test
    public void testKeplerPV() {

        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.myContext("regular-data:potential:tides");

        final EcksteinHechlerPropagatorBuilder propagatorBuilder = context.createBuilder(1.0);

        // create perfect PV measurements
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                          propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
                                                                              new PVMeasurementCreator(),
                                                                              0.0, 2.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        EcksteinHechlerEstimationTestUtils.checkFit(context, estimator, 1, 1,
                                                    0.0, 1.0e-15,
                                                    0.0, 1.0e-15,
                                                    0.0, 5.4e-9,
                                                    0.0, 1.1e-12);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assert.assertEquals(6,       normalizedCovariances.getRowDimension());
        Assert.assertEquals(6,       normalizedCovariances.getColumnDimension());
        Assert.assertEquals(6,       physicalCovariances.getRowDimension());
        Assert.assertEquals(6,       physicalCovariances.getColumnDimension());

    }

    /**
     * Perfect range measurements with a biased start
     */
    @Test
    public void testKeplerRange() {

        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.myContext("regular-data:potential:tides");

        final EcksteinHechlerPropagatorBuilder propagatorBuilder = context.createBuilder(1.0);

        // create perfect range measurements
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                          propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
                                                               new EcksteinHechlerRangeMeasurementCreator(context),
                                                               0.0, 2.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> range : measurements) {
            estimator.addMeasurement(range);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);
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

        ParameterDriver aDriver = estimator.getOrbitalParametersDrivers(true).getDrivers().get(0);
        Assert.assertEquals("a", aDriver.getName());
        aDriver.setValue(aDriver.getValue() + 1.2);
        aDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        EcksteinHechlerEstimationTestUtils.checkFit(context, estimator, 2, 3,
                                                    0.0, 1.1e-6,
                                                    0.0, 2.8e-6,
                                                    0.0, 4.0e-7,
                                                    0.0, 2.2e-10);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if ("a".equals(driver.getName())) {
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

        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.myContext("regular-data:potential:tides");

        final EcksteinHechlerPropagatorBuilder propagatorBuilder = context.createBuilder(1.0);

        // create perfect range measurements
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                          propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
                                                               new EcksteinHechlerRangeMeasurementCreator(context),
                                                               1.0, 2.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> range : measurements) {
            estimator.addMeasurement(range);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);
        estimator.setObserver(new BatchLSObserver() {
            /** {@inheritDoc} */
            @Override
            public void evaluationPerformed(int iterationsCount, int evaluationscount,
                                           Orbit[] orbits,
                                           ParameterDriversList estimatedOrbitalParameters,
                                           ParameterDriversList estimatedPropagatorParameters,
                                           ParameterDriversList estimatedMeasurementsParameters,
                                           EstimationsProvider evaluationsProvider, Evaluation lspEvaluation) {
                throw new DummyException();
            }
        });

        try {
            EcksteinHechlerEstimationTestUtils.checkFit(context, estimator, 3, 4,
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
     * Perfect range rate measurements with a perfect start
     */
    @Test
    public void testKeplerRangeRate() {

        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.myContext("regular-data:potential:tides");

        final EcksteinHechlerPropagatorBuilder propagatorBuilder = context.createBuilder(1.0);

        // create perfect range rate measurements
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                          propagatorBuilder);
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements1 =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
                                                               new EcksteinHechlerRangeRateMeasurementCreator(context, false, satClkDrift),
                                                               0.0, 2.0, 300.0);

        final List<ObservedMeasurement<?>> measurements = new ArrayList<ObservedMeasurement<?>>();
        measurements.addAll(measurements1);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> rangerate : measurements) {
            estimator.addMeasurement(rangerate);
        }
        estimator.setParametersConvergenceThreshold(1.0e-3);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        EcksteinHechlerEstimationTestUtils.checkFit(context, estimator, 1, 2,
                                                    0.0, 5.3e-7,
                                                    0.0, 1.3e-6,
                                                    0.0, 8.4e-4,
                                                    0.0, 5.1e-7);
    }

}


