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
package org.orekit.estimation.leastsquares;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem.Evaluation;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.DSSTContext;
import org.orekit.estimation.DSSTEstimationTestUtils;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.PhaseCentersRangeModifier;
import org.orekit.frames.LOFType;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import java.util.ArrayList;
import java.util.List;

public class DSSTBatchLSEstimatorTest {

    /**
     * Perfect PV measurements with a perfect start
     */
    @Test
    public void testKeplerPV() {

        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 60.0, 600.0, 1.0);

        // create perfect PV measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
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

        DSSTEstimationTestUtils.checkFit(context, estimator, 1, 2,
                                     0.0, 4.8e-9,
                                     0.0, 2.6e-8,
                                     0.0, 8.9e-9,
                                     0.0, 4.4e-12);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assertions.assertEquals(6,       normalizedCovariances.getRowDimension());
        Assertions.assertEquals(6,       normalizedCovariances.getColumnDimension());
        Assertions.assertEquals(6,       physicalCovariances.getRowDimension());
        Assertions.assertEquals(6,       physicalCovariances.getColumnDimension());
        Assertions.assertEquals(0.00258, physicalCovariances.getEntry(0, 0), 1.0e-5);

    }

    /** Test PV measurements generation and backward propagation in least-square orbit determination. */
    @Test
    public void testKeplerPVBackward() {

        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 60.0, 600.0, 1.0);

        // create perfect PV measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
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

        DSSTEstimationTestUtils.checkFit(context, estimator, 1, 3,
                                     0.0, 4.8e-9,
                                     0.0, 2.7e-8,
                                     0.0, 3.9e-9,
                                     0.0, 1.9e-12);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assertions.assertEquals(6,       normalizedCovariances.getRowDimension());
        Assertions.assertEquals(6,       normalizedCovariances.getColumnDimension());
        Assertions.assertEquals(6,       physicalCovariances.getRowDimension());
        Assertions.assertEquals(6,       physicalCovariances.getColumnDimension());
        Assertions.assertEquals(0.00258, physicalCovariances.getEntry(0, 0), 1.0e-5);

    }

    /**
     * Perfect range measurements with a biased start
     */
    @Test
    public void testKeplerRange() {

        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 60.0, 600.0, 1.0);

        // create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

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
                    Assertions.assertEquals(lastEval + 1, evaluationscount);
                } else {
                    Assertions.assertEquals(lastIter + 1, iterationsCount);
                }
                lastIter = iterationsCount;
                lastEval = evaluationscount;
                Assertions.assertEquals(measurements.size(), evaluationsProvider.getNumber());
                try {
                    evaluationsProvider.getEstimatedMeasurement(-1);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
                }
                try {
                    evaluationsProvider.getEstimatedMeasurement(measurements.size());
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
                }
                AbsoluteDate previous = AbsoluteDate.PAST_INFINITY;
                for (int i = 0; i < evaluationsProvider.getNumber(); ++i) {
                    AbsoluteDate current = evaluationsProvider.getEstimatedMeasurement(i).getDate();
                    Assertions.assertTrue(current.compareTo(previous) >= 0);
                    previous = current;
                }
            }
        });

        ParameterDriver aDriver = estimator.getOrbitalParametersDrivers(true).getDrivers().get(0);
        Assertions.assertEquals("a", aDriver.getName());
        aDriver.setValue(aDriver.getValue() + 1.2);
        aDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        DSSTEstimationTestUtils.checkFit(context, estimator, 2, 3,
                                     0.0, 3.1e-6,
                                     0.0, 5.7e-6,
                                     0.0, 1.3e-6,
                                     0.0, 5.2e-10);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if ("a".equals(driver.getName())) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(propagatorBuilder.getInitialOrbitDate()), 1.0e-15);
            }
        }

    }

    /**
     * Perfect range measurements with a biased start and an on-board antenna range offset
     */
    @Test
    public void testKeplerRangeWithOnBoardAntennaOffset() {

        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 60.0, 600.0, 1.0);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));
        final Vector3D antennaPhaseCenter = new Vector3D(-1.2, 2.3, -0.7);

        // create perfect range measurements with antenna offset
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context,
                                                                                                 Vector3D.ZERO, null,
                                                                                                 antennaPhaseCenter, null,
                                                                                                 0),
                                                               1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        final PhaseCentersRangeModifier obaModifier = new PhaseCentersRangeModifier(FrequencyPattern.ZERO_CORRECTION,
                                                                                    new FrequencyPattern(antennaPhaseCenter,
                                                                                                         null));
        for (final ObservedMeasurement<?> range : measurements) {
            ((Range) range).addModifier(obaModifier);
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
                    Assertions.assertEquals(lastEval + 1, evaluationscount);
                } else {
                    Assertions.assertEquals(lastIter + 1, iterationsCount);
                }
                lastIter = iterationsCount;
                lastEval = evaluationscount;
                Assertions.assertEquals(measurements.size(), evaluationsProvider.getNumber());
                try {
                    evaluationsProvider.getEstimatedMeasurement(-1);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
                }
                try {
                    evaluationsProvider.getEstimatedMeasurement(measurements.size());
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
                }
                AbsoluteDate previous = AbsoluteDate.PAST_INFINITY;
                for (int i = 0; i < evaluationsProvider.getNumber(); ++i) {
                    AbsoluteDate current = evaluationsProvider.getEstimatedMeasurement(i).getDate();
                    Assertions.assertTrue(current.compareTo(previous) >= 0);
                    previous = current;
                }
            }
        });

        ParameterDriver aDriver = estimator.getOrbitalParametersDrivers(true).getDrivers().get(0);
        Assertions.assertEquals("a", aDriver.getName());
        aDriver.setValue(aDriver.getValue() + 1.2);
        aDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        DSSTEstimationTestUtils.checkFit(context, estimator, 2, 3,
                                     0.0, 2.3e-5,
                                     0.0, 5.9e-5,
                                     0.0, 2.7e-5,
                                     0.0, 1.1e-8);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if ("a".equals(driver.getName())) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(propagatorBuilder.getInitialOrbitDate()), 1.0e-15);
            }
        }

    }

    @Test
    public void testWrappedException() {

        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 60.0, 600.0, 1.0);

        // create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                               propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

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
                                           EstimationsProvider evaluationsProvider, Evaluation lspEvaluation) throws DummyException {
                throw new DummyException();
            }
        });

        try {
            DSSTEstimationTestUtils.checkFit(context, estimator, 3, 4,
                                         0.0, 1.5e-6,
                                         0.0, 3.2e-6,
                                         0.0, 3.8e-7,
                                         0.0, 1.5e-10);
            Assertions.fail("an exception should have been thrown");
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

        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 60.0, 600.0, 1.0);

        // create perfect range rate measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements1 =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, satClkDrift),
                                                               1.0, 3.0, 300.0);

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

        DSSTEstimationTestUtils.checkFit(context, estimator, 1, 2,
                                     0.0, 5.4e-7,
                                     0.0, 1.2e-6,
                                     0.0, 8.3e-4,
                                     0.0, 4.5e-7);
    }

    /**
     * Perfect range and range rate measurements with a perfect start
     */
    @Test
    public void testKeplerRangeAndRangeRate() {

        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 60.0, 600.0, 1.0);

        // create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                               propagatorBuilder);

        final List<ObservedMeasurement<?>> measurementsRange =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurementsRangeRate =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, satClkDrift),
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
        DSSTEstimationTestUtils.checkFit(context, estimator, 1, 3,
                                     0.0, 4.9e-7,
                                     0.0, 1.6e-6,
                                     0.0, 4.4e-8,
                                     0.0, 2.0e-11);
    }

    @Test
    public void testIssue359() {
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 60.0, 600.0, 1.0);

        // Select the central attraction coefficient (here there is only the central attraction coefficient)
        // as estimated parameter
        propagatorBuilder.getPropagationParametersDrivers().getDrivers().get(0).setSelected(true);
        // create perfect PV measurements
        final DSSTPropagator propagator = (DSSTPropagator) DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }
        ParameterDriversList estimatedParameters = estimator.getPropagatorParametersDrivers(true);
        // Verify that the propagator, the builder and the estimator know mu
        final String driverName = DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT;
        Assertions.assertTrue(propagator.getAllForceModels().get(0) instanceof DSSTNewtonianAttraction);
        Assertions.assertTrue(propagatorBuilder.getAllForceModels().get(0) instanceof DSSTNewtonianAttraction);
        Assertions.assertNotNull(estimatedParameters.findByName(driverName));
        Assertions.assertTrue(propagator.getAllForceModels().get(0).getParametersDrivers().get(0).isSelected());
        Assertions.assertTrue(propagatorBuilder.getAllForceModels().get(0).getParametersDrivers().get(0).isSelected());
    }

}
