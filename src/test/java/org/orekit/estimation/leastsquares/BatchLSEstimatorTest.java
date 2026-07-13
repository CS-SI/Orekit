/* Copyright 2002-2026 CS GROUP
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

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem.Evaluation;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.Well512a;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Force;
import org.orekit.estimation.measurements.AngularAzElMeasurementCreator;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.InterSatellitesRangeMeasurementCreator;
import org.orekit.estimation.measurements.MultiplexedMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Position;
import org.orekit.estimation.measurements.PositionMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.SpaceTDOAMeasurementCreator;
import org.orekit.estimation.measurements.SpaceTwoWayRangeMeasurementCreator;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.MeasurementNoise;
import org.orekit.estimation.measurements.modifiers.OutlierFilter;
import org.orekit.estimation.measurements.modifiers.PhaseCentersRangeModifier;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.LOFType;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CartesianOrbitFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.KeplerianPropagatorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

class BatchLSEstimatorTest {

    @Test
    void testIssue1864() {
        // GIVEN
        EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final KeplerianPropagatorBuilder propagatorBuilder =
            new KeplerianPropagatorBuilder(new CartesianOrbitFactory((CartesianOrbit) OrbitType.CARTESIAN.convertType(orbit),
                                                                     1.0));
        final BatchLSEstimator estimator = new BatchLSEstimator(new GaussNewtonOptimizer(), propagatorBuilder);
        estimator.setParametersConvergenceThreshold(1e-2);
        estimator.setMaxIterations(100);
        estimator.setMaxEvaluations(100);
        final ObservableSatellite satellite = new ObservableSatellite(0);
        for (int i = 0; i < 10; i++) {
            final TimeStampedPVCoordinates shifted = orbit.shiftedBy(i * 100).getPVCoordinates();
            estimator.addMeasurement(new PV(shifted.getDate(), shifted.getPosition(), shifted.getVelocity(),
                    1e1, 1.e-1, 1., satellite));
        }
        final PVCoordinates pvCoordinates = orbit.shiftedBy(1e4).getPVCoordinates();
        final PV outlier = new PV(orbit.getDate().shiftedBy(-1), pvCoordinates.getPosition(), pvCoordinates.getVelocity(),
                1e1, 1.e-1, 1., satellite);
        final OutlierFilter<PV> outlierFilter = new OutlierFilter<>(1, 1.);
        outlier.addModifier(outlierFilter);
        estimator.addMeasurement(outlier);
        // WHEN
        estimator.estimate();
        final LeastSquaresOptimizer.Optimum optimum = estimator.getOptimum();
        // THEN
        Assertions.assertEquals(6, optimum.getIterations());
        Assertions.assertEquals(3.1781e-20, optimum.getChiSquare(), 1e-22);
        final double actualReduced = optimum.getReducedChiSquare(0);
        Assertions.assertEquals(5.2099e-22, actualReduced, 1e-24);
    }

    /**
     * Perfect PV measurements with a perfect start
     */
    @Test
    void testKeplerPVMultipleDrag() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0, Force.DRAG);

        for (ParameterDriver driver:propagatorBuilder.getPropagationParametersDrivers().getDrivers()) {
            if (driver.getName().equals("drag coefficient")) {
                driver.setSelected(true);
                driver.addSpanAtDate(context.initialOrbit.getDate());
            }
        }
        
        // create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               -3.0, 3.0, 300.0);



        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        measurements.forEach(estimator::addMeasurement);
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        EstimationTestUtils.checkFit(false, context, estimator, 1, 2,
                                     0.0, 7.8e-8,
                                     0.0, 6.0e-7,
                                     0.0, 3.2e-7,
                                     0.0, 1.3e-10);

        
        List<DelegatingDriver> Orbparameters = estimator.getOrbitalParametersDrivers(true).getDrivers();
        Assertions.assertEquals(context.initialOrbit.getA(), Orbparameters.getFirst().getValue() , 1.0e-8);
        Assertions.assertEquals(context.initialOrbit.getE(), Orbparameters.get(1).getValue() , 1.0e-12);
        Assertions.assertEquals(context.initialOrbit.getI(), Orbparameters.get(2).getValue() , 1.0e-12);
        
        RealMatrix jacobian = estimator.getOptimum().getJacobian();
        Assertions.assertEquals(8,      jacobian.getColumnDimension());

    }

    /**
     * Noisy position measurements with a perfect start and an RMS convergence check
     */
    @Test
    @SuppressWarnings("unchecked")
    void testNoisyKeplerPosition() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                context.createNumerical(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, true,
                        1.0e-6, 60.0, 1.e-3);

        // create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                EstimationTestUtils.createMeasurements(propagator, new PositionMeasurementCreator(),0.0, 1.0, 300.0);
        final RealMatrix covariance = MatrixUtils.createRealIdentityMatrix(6);
        final MeasurementNoise<Position> measurementNoise = new MeasurementNoise<>(new CorrelatedRandomVectorGenerator(covariance,
                1e-16, new GaussianRandomGenerator(new Well512a(293890302))));
        measurements.forEach(measurement -> measurement.addModifier((EstimationModifier) measurementNoise));

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new GaussNewtonOptimizer(),
                propagatorBuilder);
        measurements.forEach(estimator::addMeasurement);
        final double rmsThreshold = 1e-2;
        estimator.setConvergenceChecker(new RMSConvergenceChecker(rmsThreshold));
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        // run estimation while logging RMS
        final List<Double> rms = new ArrayList<>();
        estimator.setObserver((iterationsCount, evaluationsCount, orbits,
                               estimatedOrbitalParameters, estimatedPropagatorParameters,
                               estimatedMeasurementsParameters, evaluationsProvider,
                               lspEvaluation) -> rms.add(lspEvaluation.getRMS()));
        estimator.estimate();

        // check
        Assertions.assertEquals(9, rms.size());
        for (int i = 0; i < rms.size() - 2; i++) {
            Assertions.assertFalse(FastMath.abs(1. - rms.get(i + 1) / rms.get(i)) <= rmsThreshold);
        }
        Assertions.assertTrue(FastMath.abs(1. - rms.getLast() / rms.get(rms.size() - 2)) <= rmsThreshold);
    }

    /**
     * Perfect PV measurements with a perfect start
     */
    @Test
    void testKeplerPV() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        measurements.forEach(estimator::addMeasurement);
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        EstimationTestUtils.checkFit(false, context, estimator, 1, 4,
                                     0.0, 2.2e-8,
                                     0.0, 1.1e-7,
                                     0.0, 1.4e-8,
                                     0.0, 6.3e-12);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assertions.assertEquals(6, normalizedCovariances.getRowDimension());
        Assertions.assertEquals(6, normalizedCovariances.getColumnDimension());
        Assertions.assertEquals(6, physicalCovariances.getRowDimension());
        Assertions.assertEquals(6, physicalCovariances.getColumnDimension());
        Assertions.assertEquals(0.00258, physicalCovariances.getEntry(0, 0), 1.0e-5);

    }
    
    /** Test PV measurements generation and backward propagation in least-square orbit determination. */
    @Test
    void testKeplerPVBackward() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, -1.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        measurements.forEach(estimator::addMeasurement);
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        EstimationTestUtils.checkFit(false, context, estimator, 1, 2,
                                     0.0, 8.3e-9,
                                     0.0, 5.3e-8,
                                     0.0, 5.6e-9,
                                     0.0, 1.6e-12);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assertions.assertEquals(6, normalizedCovariances.getRowDimension());
        Assertions.assertEquals(6, normalizedCovariances.getColumnDimension());
        Assertions.assertEquals(6, physicalCovariances.getRowDimension());
        Assertions.assertEquals(6, physicalCovariances.getColumnDimension());
        Assertions.assertEquals(0.00258, physicalCovariances.getEntry(0, 0), 1.0e-5);

    }

    /**
     * Perfect range measurements with a biased start
     */
    @Test
    void testKeplerRange() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        measurements.forEach(estimator::addMeasurement);
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

        ParameterDriver aDriver = estimator.getOrbitalParametersDrivers(true).getDrivers().getFirst();
        Assertions.assertEquals("a", aDriver.getName());
        aDriver.setValue(aDriver.getValue() + 1.2);
        aDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        EstimationTestUtils.checkFit(false, context, estimator, 2, 3,
                                     0.0, 1.2e-6,
                                     0.0, 2.8e-6,
                                     0.0, 7.0e-7,
                                     0.0, 3e-10);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if ("a".equals(driver.getName())) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0,
                                        driver.getReferenceDate().durationFrom(propagatorBuilder.getOrbitalParameterFactory().getDate()),
                                        1.0e-15);
            }
        }

    }

    /**
     * Perfect range measurements with a biased start
     */
    @Test
    void testKeplerObserverSatelliteTDOA() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder = context.createNumerical(OrbitType.KEPLERIAN,
                PositionAngleType.TRUE, true,
                1.0e-6, 60.0, 1.0);

        // Test for variable clock offset values
        final double primaryBias = 4e-9;
        final double primaryDrift = -1e-10;
        final double secondaryBias = -2e-9;
        final double secondaryDrift = 3.5e-11;

        // create perfect TDOA measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements = EstimationTestUtils.createMeasurements(propagator,
                new SpaceTDOAMeasurementCreator(context, primaryBias, primaryDrift, secondaryBias, secondaryDrift),
                1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                propagatorBuilder);
        measurements.forEach(estimator::addMeasurement);
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

        ParameterDriver aDriver = estimator.getOrbitalParametersDrivers(true).getDrivers().getFirst();
        Assertions.assertEquals("a", aDriver.getName());
        aDriver.setValue(aDriver.getValue() + 1.2);
        aDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        EstimationTestUtils.checkFit(false, context, estimator, 2, 3,
                0.0, 1e-12,
                0.0, 1e-12,
                0.0, 3e-6,
                0.0, 1e-9);

        // after the call to estimate, the parameters lacking a user-specified reference
        // date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if ("a".equals(driver.getName())) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0,
                                        driver.getReferenceDate().durationFrom(propagatorBuilder.getOrbitalParameterFactory().getDate()),
                                        1.0e-15);
            }
        }

    }

     /**
     * Perfect range measurements with a biased start
     */
    @Test
    void testKeplerObserverSatelliteRange() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements = EstimationTestUtils.createMeasurements(propagator,
                new SpaceTwoWayRangeMeasurementCreator(context),
                1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                propagatorBuilder);
        measurements.forEach(estimator::addMeasurement);
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

        ParameterDriver aDriver = estimator.getOrbitalParametersDrivers(true).getDrivers().getFirst();
        Assertions.assertEquals("a", aDriver.getName());
        aDriver.setValue(aDriver.getValue() + 1.2);
        aDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        EstimationTestUtils.checkFit(false, context, estimator, 2, 3,
                0.0, 2.4e-5,
                0.0, 6.0e-5,
                0.0, 1.2e-5,
                0.0, 3.0e-9);

        // after the call to estimate, the parameters lacking a user-specified reference
        // date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if ("a".equals(driver.getName())) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0,
                                        driver.getReferenceDate().durationFrom(propagatorBuilder.getOrbitalParameterFactory().getDate()),
                                        1.0e-15);
            }
        }

    }

    /**
     * Perfect range measurements with a biased start and an on-board antenna range offset
     */
    @Test
    void testKeplerRangeWithOnBoardAntennaOffset() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getOrbitalParameterFactory().getFrame(),
                                                            LOFType.LVLH));
        final Vector3D antennaPhaseCenter = new Vector3D(-1.2, 2.3, -0.7);

        // create perfect range measurements with antenna offset
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
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
        measurements.forEach(m -> {
            ((Range) m).addModifier(obaModifier);
            estimator.addMeasurement(m);
        });
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

        ParameterDriver aDriver = estimator.getOrbitalParametersDrivers(true).getDrivers().getFirst();
        Assertions.assertEquals("a", aDriver.getName());
        aDriver.setValue(aDriver.getValue() + 1.2);
        aDriver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        EstimationTestUtils.checkFit(false, context, estimator, 2, 3,
                                     0.0, 5.4e-7,
                                     0.0, 1.3e-6,
                                     0.0, 4.5e-7,
                                     0.0, 1.9e-10);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if ("a".equals(driver.getName())) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0,
                                        driver.getReferenceDate().durationFrom(propagatorBuilder.getOrbitalParameterFactory().getDate()),
                                        1.0e-15);
            }
        }

    }

    @Test
    void testMultiSat() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0e-3);
        final NumericalPropagatorBuilder propagatorBuilder2 =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0e-3);

        // Create perfect inter-satellites range measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder2);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        Propagator propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                     propagatorBuilder1);

        final double localClockOffset  = 0.137e-6;
        final double remoteClockOffset = 469.0e-6;
        final List<ObservedMeasurement<?>> r12 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris,
                                                                                                          localClockOffset,
                                                                                                          remoteClockOffset),
                                                               1.0, 3.0, 300.0);

        // create perfect range measurements for first satellite
        propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                           propagatorBuilder1);
        final List<ObservedMeasurement<?>> r1 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder1,
                                                                propagatorBuilder2);
        r1.forEach(estimator::addMeasurement);
        r12.forEach(estimator::addMeasurement);
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
                Assertions.assertEquals(r12.size() + r1.size(), evaluationsProvider.getNumber());
                try {
                    evaluationsProvider.getEstimatedMeasurement(-1);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
                }
                try {
                    evaluationsProvider.getEstimatedMeasurement(r12.size() + r1.size());
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

        List<DelegatingDriver> parameters = estimator.getOrbitalParametersDrivers(true).getDrivers();
        ParameterDriver a0Driver = parameters.getFirst();
        Assertions.assertEquals("a[0]", a0Driver.getName());
        a0Driver.setValue(a0Driver.getValue() + 1.2);
        a0Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        ParameterDriver a1Driver = parameters.get(6);
        Assertions.assertEquals("a[1]", a1Driver.getName());
        a1Driver.setValue(a1Driver.getValue() - 5.4);
        a1Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        final Orbit before = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngleType.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assertions.assertEquals(4.7246, Vector3D.distance(closeOrbit.getPosition(),
                          before.getPosition()), 1.0e-3);
        Assertions.assertEquals(0.0010514, Vector3D.distance(closeOrbit.getVelocity(),
                          before.getVelocity()), 1.0e-6);
        EstimationTestUtils.checkFit(false, context, estimator, 3, 4,
                                     0.0, 4e-06,
                                     0.0, 1.2e-05,
                                     0.0, 1.2e-07,
                                     0.0, 6.6e-11);

        final Orbit determined = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngleType.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assertions.assertEquals(0.0, Vector3D.distance(closeOrbit.getPosition(),
                          determined.getPosition()), 6.2e-6);
        Assertions.assertEquals(0.0, Vector3D.distance(closeOrbit.getVelocity(),
                          determined.getVelocity()), 5.1e-9);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if (driver.getName().startsWith("a[")) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0,
                                        driver.getReferenceDate().durationFrom(propagatorBuilder1.getOrbitalParameterFactory().getDate()),
                                        1.0e-15);
            }
        }

    }

    /** A modified version of the previous test with a selection of propagation drivers to estimate and more measurements
     *  One common (µ)
     *  Some specifics for each satellite (Cr and Ca)
     *
     */
    @Test
    void testMultiSatWithParameters() {

        // Test: Set the propagator drivers to estimate for each satellite
        final boolean muEstimated  = true;
        final boolean crEstimated1 = false;
        final boolean caEstimated1 = true;
        final boolean crEstimated2 = true;
        final boolean caEstimated2 = false;


        // Builder sat 1
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0e-3, Force.POTENTIAL, Force.SOLAR_RADIATION_PRESSURE);

        // Adding selection of parameters
        String satName = "sat 1";
        for (DelegatingDriver driver:propagatorBuilder1.getPropagationParametersDrivers().getDrivers()) {
            if (driver.getName().equals("central attraction coefficient")) {
                driver.setSelected(muEstimated);
            }
            if (driver.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)) {
                driver.setName(driver.getName() + " " + satName);
                driver.setSelected(crEstimated1);
            }
            if (driver.getName().equals(RadiationSensitive.ABSORPTION_COEFFICIENT)) {
                driver.setName(driver.getName() + " " + satName);
                driver.setSelected(caEstimated1);
            }
        }

        // Builder for sat 2
        final Context context2 = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final NumericalPropagatorBuilder propagatorBuilder2 =
                        context2.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0e-3, Force.POTENTIAL, Force.SOLAR_RADIATION_PRESSURE);

        // Adding selection of parameters
        satName = "sat 2";
        for (ParameterDriver driver:propagatorBuilder2.getPropagationParametersDrivers().getDrivers()) {
            if (driver.getName().equals("central attraction coefficient")) {
                driver.setSelected(muEstimated);
            }
            if (driver.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)) {
                driver.setName(driver.getName() + " " + satName);
                driver.setSelected(crEstimated2);
            }
            if (driver.getName().equals(RadiationSensitive.ABSORPTION_COEFFICIENT)) {
                driver.setName(driver.getName() + " " + satName);
                driver.setSelected(caEstimated2);
            }
        }

        // Create perfect inter-satellites range measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder2);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        Propagator propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                     propagatorBuilder1);
        final List<ObservedMeasurement<?>> r12 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris, 0., 0.),
                                                               1.0, 3.0, 120.0);

        // create perfect range measurements for first satellite
        propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                           propagatorBuilder1);
        final List<ObservedMeasurement<?>> r1 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 120.0);

        // create perfect angular measurements for first satellite
        propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                           propagatorBuilder1);
        final List<ObservedMeasurement<?>> a1 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new AngularAzElMeasurementCreator(context),
                                                               1.0, 3.0, 120.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder1,
                                                                propagatorBuilder2);
        r12.forEach(estimator::addMeasurement);
        r1.forEach(estimator::addMeasurement);
        a1.forEach(estimator::addMeasurement);
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

                AbsoluteDate previous = AbsoluteDate.PAST_INFINITY;
                for (int i = 0; i < evaluationsProvider.getNumber(); ++i) {
                    AbsoluteDate current = evaluationsProvider.getEstimatedMeasurement(i).getDate();
                    Assertions.assertTrue(current.compareTo(previous) >= 0);
                    previous = current;
                }
            }
        });

        List<DelegatingDriver> parameters = estimator.getOrbitalParametersDrivers(true).getDrivers();
        ParameterDriver a0Driver = parameters.getFirst();
        Assertions.assertEquals("a[0]", a0Driver.getName());
        a0Driver.setValue(a0Driver.getValue() + 1.2);
        a0Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        ParameterDriver a1Driver = parameters.get(6);
        Assertions.assertEquals("a[1]", a1Driver.getName());
        a1Driver.setValue(a1Driver.getValue() - 5.4, null);
        a1Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        final Orbit before = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                parameters.get( 7).getValue(),
                                                parameters.get( 8).getValue(),
                                                parameters.get( 9).getValue(),
                                                parameters.get(10).getValue(),
                                                parameters.get(11).getValue(),
                                                PositionAngleType.TRUE,
                                                closeOrbit.getFrame(),
                                                closeOrbit.getDate(),
                                                closeOrbit.getMu());
        Assertions.assertEquals(4.7246, Vector3D.distance(closeOrbit.getPosition(),
                          before.getPosition()), 1.0e-3);
        Assertions.assertEquals(0.0010514, Vector3D.distance(closeOrbit.getVelocity(),
                          before.getVelocity()), 1.0e-6);
        EstimationTestUtils.checkFit(false, context, estimator, 4, 5,
                                     0.0, 6.7e-06,
                                     0.0, 1.49e-05,
                                     0.0, 9.1e-07,
                                     0.0, 3.6e-10);

        final Orbit determined = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngleType.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assertions.assertEquals(0.0, Vector3D.distance(closeOrbit.getPosition(),
                          determined.getPosition()), 6.6e-6);
        Assertions.assertEquals(0.0, Vector3D.distance(closeOrbit.getVelocity(),
                          determined.getVelocity()), 4.6e-9);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if (driver.getName().startsWith("a[")) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0,
                                        driver.getReferenceDate().durationFrom(propagatorBuilder1.getOrbitalParameterFactory().getDate()),
                                        1.0e-15);
            }
        }

    }


    /**
     * This test is identical to testMultiSat() but here we use multiplexed measurement theory
     */
    @Test
    void testIssue617() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        final NumericalPropagatorBuilder propagatorBuilder2 =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // Create perfect inter-satellites range measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder2);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        Propagator propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                     propagatorBuilder1);

        final List<ObservedMeasurement<?>> r12 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris, 0., 0.),
                                                               1.0, 3.0, 300.0);

        // create perfect range measurements for first satellite
        propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                           propagatorBuilder1);
        final List<ObservedMeasurement<?>> r1 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder1,
                                                                propagatorBuilder2);
        // Create a common list of measurements
        final List<ObservedMeasurement<?>> independentMeasurements = new ArrayList<>();
        independentMeasurements.addAll(r1);
        independentMeasurements.addAll(r12);

        // List of measurements
        // The threshold is fixed to 60s in order to build multiplexed measurements
        // If it is less than 60s we cannot have mutliplexed measurement and we would not be able to
        // test the issue.
        final List<ObservedMeasurement<?>> multiplexed = multiplexMeasurements(independentMeasurements);

        for (final ObservedMeasurement<?> measurement : multiplexed) {
            estimator.addMeasurement(measurement);
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
                Assertions.assertEquals(multiplexed.size(), evaluationsProvider.getNumber());
                try {
                    evaluationsProvider.getEstimatedMeasurement(-1);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assertions.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
                }
                try {
                    evaluationsProvider.getEstimatedMeasurement(r12.size() + r1.size());
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

        List<DelegatingDriver> parameters = estimator.getOrbitalParametersDrivers(true).getDrivers();
        ParameterDriver a0Driver = parameters.getFirst();
        Assertions.assertEquals("a[0]", a0Driver.getName());
        a0Driver.setValue(a0Driver.getValue() + 1.2);
        a0Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        ParameterDriver a1Driver = parameters.get(6);
        Assertions.assertEquals("a[1]", a1Driver.getName());
        a1Driver.setValue(a1Driver.getValue() - 5.4);
        a1Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        final Orbit before = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngleType.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assertions.assertEquals(4.7246, Vector3D.distance(closeOrbit.getPosition(),
                          before.getPosition()), 1.0e-3);
        Assertions.assertEquals(0.0010514, Vector3D.distance(closeOrbit.getVelocity(),
                          before.getVelocity()), 1.0e-6);
        EstimationTestUtils.checkFit(false, context, estimator, 2, 3,
                                     0.0, 2.9e-06,
                                     0.0, 8.1e-06,
                                     0.0, 7.1e-07,
                                     0.0, 3.2e-10);

        final Orbit determined = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngleType.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assertions.assertEquals(0.0, Vector3D.distance(closeOrbit.getPosition(),
                          determined.getPosition()), 4.6e-6);
        Assertions.assertEquals(0.0, Vector3D.distance(closeOrbit.getVelocity(),
                          determined.getVelocity()), 1.6e-9);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if (driver.getName().startsWith("a[")) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0,
                                        driver.getReferenceDate().durationFrom(propagatorBuilder1.getOrbitalParameterFactory().getDate()),
                                        1.0e-15);
            }
        }

    }

    @Test
    void testWrappedException() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        measurements.forEach(estimator::addMeasurement);
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
            EstimationTestUtils.checkFit(false, context, estimator, 3, 4,
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
        @Serial
        private static final long serialVersionUID = 1L;
        public DummyException() {
            super(OrekitMessages.INTERNAL_ERROR);
        }
    }

    /**
     * Perfect range rate measurements with a perfect start
     */
    @Test
    void testKeplerRangeRate() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect range rate measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, satClkDrift),
                                                               1.0, 3.0, 300.0);


        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> rangerate : measurements) {
            estimator.addMeasurement(rangerate);
        }
        estimator.setParametersConvergenceThreshold(1.0e-3);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        EstimationTestUtils.checkFit(false, context, estimator, 1, 4,
                                     0.0, 4.0e-10,
                                     0.0, 7.0e-10,
                                     0.0, 1.2e-7,
                                     0.0, 3.6e-11);
    }

    /**
     * Perfect range and range rate measurements with a perfect start
     */
    @Test
    void testKeplerRangeAndRangeRate() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double groundClockDrift = 4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;

        //Note: had to decrease step size to 100 to keep solver from exceeding max number of evaluations.
        final List<ObservedMeasurement<?>> measurementsRange =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 100.0);
        final List<ObservedMeasurement<?>> measurementsRangeRate =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false, satClkDrift),
                                                               1.0, 3.0, 300.0);

        // concat measurements
        final List<ObservedMeasurement<?>> measurements = new ArrayList<>();
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
        EstimationTestUtils.checkFit(false, context, estimator, 1, 3,
                                     0.0, 4.4e-7,
                                     0.0, 1.4e-6,
                                     0.0, 1.9e-7,
                                     0.0, 7.7e-11);
    }

    /**
     * Test if the parameter µ is taken into account by the builder even if no attraction force has been added yet.
     */
    @Test
    void testIssue359() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // Select the central attraction coefficient (here there is only the central attraction coefficient)
        // as estimated parameter
        propagatorBuilder.getPropagationParametersDrivers().getDrivers().getFirst().setSelected(true);
        // create perfect PV measurements
        final NumericalPropagator propagator = (NumericalPropagator) EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);

        measurements.forEach(estimator::addMeasurement);

        ParameterDriversList estimatedParameters = estimator.getPropagationParametersDrivers(true);
        // Verify that the propagator, the builder and the estimator know mu
        final String driverName = NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT;
        Assertions.assertInstanceOf(NewtonianAttraction.class, propagator.getAllForceModels().getFirst());
        Assertions.assertInstanceOf(NewtonianAttraction.class, propagatorBuilder.getAllForceModels().getFirst());
        Assertions.assertNotNull(estimatedParameters.findByName(driverName));
        Assertions.assertTrue(propagator.getAllForceModels().getFirst().getParameterDriver(driverName).isSelected());
        Assertions.assertTrue(propagatorBuilder.getAllForceModels().getFirst().getParameterDriver(driverName).isSelected());
    }

    @Test
    void testEstimateOnlyOneOrbitalParameter() {
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ true,  false, false, false, false, false });
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false,  true, false, false, false, false });
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false, false,  true, false, false, false });
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false, false, false,  true, false, false });
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false, false, false, false,  true, false });
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false, false, false, false, false,  true });
    }

    @Test
    void testEstimateOnlyFewOrbitalParameters() {
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false,  true, false, true, false, false });
    }

    /**
     * Test added for coverage. Check that an error is returned when no observed measurements are available for
     * estimator.
     * @since 13.1.3
     */
    @Test
    void testErrorThrownWhenNoEnabledMeasurements() {
        // GIVEN
        // Create propagator builder for estimator
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final NumericalPropagatorBuilder propagatorBuilder =
                context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                      1.0e-6, 60.0, 1.0e-3);

        // Estimate orbital parameters
        final List<DelegatingDriver> drivers = propagatorBuilder.
                                               getOrbitalParameterFactory().
                                               getOrbitalParametersDrivers().
                                               getDrivers();
        drivers.forEach(driver -> driver.setSelected(true));
        drivers.forEach(driver -> driver.setValue(1.0001 * driver.getValue()));

        // create the estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);


        // Create measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                EstimationTestUtils.createMeasurements(propagator,
                                                       new PVMeasurementCreator(),
                                                       0.0, 1.0, 300.0);

        // Disable measurements for test purpose
        measurements.forEach(measurement -> measurement.setEnabled(false));

        // Add measurements to estimator
        measurements.forEach(estimator::addMeasurement);

        // WHEN & THEN
        Assertions.assertThrows(NoSuchElementException.class, estimator::estimate);
    }

    private void doTestEstimateOnlySomeOrbitalParameters(boolean[] orbitalParametersEstimated) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                context.createNumerical(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                      1.0e-6, 60.0, 1.0e-3);

        // create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                EstimationTestUtils.createMeasurements(propagator,
                                                       new PVMeasurementCreator(),
                                                       0.0, 1.0, 300.0);

        List<ParameterDriversList.DelegatingDriver> orbitalElementsDrivers =
            propagatorBuilder.getOrbitalParameterFactory().getOrbitalParametersDrivers().getDrivers();
        IntStream.range(0, orbitalParametersEstimated.length).forEach(i -> {
            final ParameterDriver driver = orbitalElementsDrivers.get(i);
            if (orbitalParametersEstimated[i]) {
                driver.setSelected(true);
                driver.setValue(1.0001 * driver.getReferenceValue());
            } else {
                driver.setSelected(false);
            }
        });

        // create the estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        measurements.forEach(estimator::addMeasurement);
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        // perform estimation
        estimator.estimate();

        // check the selected parameters have been estimated properly
        IntStream.range(0, orbitalParametersEstimated.length).forEach(i -> {
            if (orbitalParametersEstimated[i]) {
                final ParameterDriver driver = orbitalElementsDrivers.get(i);
                Assertions.assertEquals(driver.getReferenceValue(), driver.getValue(),
                        driver.getReferenceValue() * 1.0e-3);
            }
        });

    }

    /**
     * Multiplex measurements.
     *
     * @param independentMeasurements independent measurements
     * @return multiplexed measurements
     */
    private List<ObservedMeasurement<?>> multiplexMeasurements(final List<ObservedMeasurement<?>> independentMeasurements) {
        final List<ObservedMeasurement<?>> multiplexed = new ArrayList<>();
        independentMeasurements.sort(new ChronologicalComparator());
        List<ObservedMeasurement<?>> clump = new ArrayList<>();
        for (final ObservedMeasurement<?> measurement : independentMeasurements) {
            if (!clump.isEmpty() && measurement.getDate().durationFrom(clump.getFirst().getDate()) > 60.0) {

                // previous clump is finished
                if (clump.size() == 1) {
                    multiplexed.add(clump.getFirst());
                } else {
                    multiplexed.add(new MultiplexedMeasurement(clump));
                }

                // start new clump
                clump = new ArrayList<>();

            }
            clump.add(measurement);
        }
        // final clump is finished
        if (clump.size() == 1) {
            multiplexed.add(clump.getFirst());
        } else {
            multiplexed.add(new MultiplexedMeasurement(clump));
        }
        return multiplexed;
    }

}


