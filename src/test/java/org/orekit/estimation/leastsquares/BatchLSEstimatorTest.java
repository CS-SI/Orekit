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
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Force;
import org.orekit.estimation.measurements.AngularAzElMeasurementCreator;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.InterSatellitesRangeMeasurementCreator;
import org.orekit.estimation.measurements.MultiplexedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.PhaseCentersRangeModifier;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.LOFType;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class BatchLSEstimatorTest {

    /**
     * Perfect PV measurements with a perfect start
     */
    @Test
    public void testKeplerPVMultipleDrag() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
        for (final ObservedMeasurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        EstimationTestUtils.checkFit(context, estimator, 1, 2,
                                     0.0, 7.8e-8,
                                     0.0, 6.0e-7,
                                     0.0, 3.2e-7,
                                     0.0, 1.3e-10);

        
        List<DelegatingDriver> Orbparameters = estimator.getOrbitalParametersDrivers(true).getDrivers();
        Assertions.assertEquals(context.initialOrbit.getA(), Orbparameters.get(0).getValue() , 1.0e-8);
        Assertions.assertEquals(context.initialOrbit.getE(), Orbparameters.get(1).getValue() , 1.0e-12);
        Assertions.assertEquals(context.initialOrbit.getI(), Orbparameters.get(2).getValue() , 1.0e-12);
        
        RealMatrix jacobian = estimator.getOptimum().getJacobian();
        Assertions.assertEquals(8,      jacobian.getColumnDimension());

    }

    /**
     * Perfect PV measurements with a perfect start
     */
    @Test
    public void testKeplerPV() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
        for (final ObservedMeasurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        EstimationTestUtils.checkFit(context, estimator, 1, 4,
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
    public void testKeplerPVBackward() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
        for (final ObservedMeasurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        EstimationTestUtils.checkFit(context, estimator, 1, 2,
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
    public void testKeplerRange() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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

        EstimationTestUtils.checkFit(context, estimator, 2, 3,
                                     0.0, 1.2e-6,
                                     0.0, 2.8e-6,
                                     0.0, 5.0e-7,
                                     0.0, 2.3e-10);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if ("a".equals(driver.getName())) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0,
                        driver.getReferenceDate().durationFrom(propagatorBuilder.getInitialOrbitDate()), 1.0e-15);
            }
        }

    }

    /**
     * Perfect range measurements with a biased start and an on-board antenna range offset
     */
    @Test
    public void testKeplerRangeWithOnBoardAntennaOffset() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));
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

        EstimationTestUtils.checkFit(context, estimator, 2, 3,
                                     0.0, 2.0e-5,
                                     0.0, 5.2e-5,
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
                Assertions.assertEquals(0,
                        driver.getReferenceDate().durationFrom(propagatorBuilder.getInitialOrbitDate()), 1.0e-15);
            }
        }

    }

    @Test
    public void testMultiSat() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0e-3);
        final NumericalPropagatorBuilder propagatorBuilder2 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
        for (final ObservedMeasurement<?> interSat : r12) {
            estimator.addMeasurement(interSat);
        }
        for (final ObservedMeasurement<?> range : r1) {
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
        ParameterDriver a0Driver = parameters.get(0);
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
        Assertions.assertEquals(0.0010514, Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                          before.getPVCoordinates().getVelocity()), 1.0e-6);
        EstimationTestUtils.checkFit(context, estimator, 3, 4,
                                     0.0, 2.9e-06,
                                     0.0, 1.1e-05,
                                     0.0, 8.3e-07,
                                     0.0, 3.7e-10);

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
        Assertions.assertEquals(0.0, Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                          determined.getPVCoordinates().getVelocity()), 1.6e-9);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if (driver.getName().startsWith("a[")) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0,
                        driver.getReferenceDate().durationFrom(propagatorBuilder1.getInitialOrbitDate()), 1.0e-15);
            }
        }

    }

    /** A modified version of the previous test with a selection of propagation drivers to estimate and more measurements
     *  One common (µ)
     *  Some specifics for each satellite (Cr and Ca)
     *
     */
    @Test
    public void testMultiSatWithParameters() {

        // Test: Set the propagator drivers to estimate for each satellite
        final boolean muEstimated  = true;
        final boolean crEstimated1 = false;
        final boolean caEstimated1 = true;
        final boolean crEstimated2 = true;
        final boolean caEstimated2 = false;


        // Builder sat 1
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
                        context2.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
        for (final ObservedMeasurement<?> interSat : r12) {
            estimator.addMeasurement(interSat);
        }
        for (final ObservedMeasurement<?> range : r1) {
            estimator.addMeasurement(range);
        }
        for (final ObservedMeasurement<?> angular : a1) {
            estimator.addMeasurement(angular);
        }
        estimator.setParametersConvergenceThreshold(1.0e-3);
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
        ParameterDriver a0Driver = parameters.get(0);
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
        Assertions.assertEquals(0.0010514, Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                          before.getPVCoordinates().getVelocity()), 1.0e-6);
        EstimationTestUtils.checkFit(context, estimator, 4, 5,
                                     0.0, 4.7e-06,
                                     0.0, 1.4e-05,
                                     0.0, 8.8e-07,
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
                          determined.getPosition()), 2.7e-6);
        Assertions.assertEquals(0.0, Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                          determined.getPVCoordinates().getVelocity()), 2.9e-9);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if (driver.getName().startsWith("a[")) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0,
                        driver.getReferenceDate().durationFrom(propagatorBuilder1.getInitialOrbitDate()), 1.0e-15);
            }
        }

    }


    /**
     * This test is identical to testMultiSat() but here we use multiplexed measurement theory
     */
    @Test
    public void testIssue617() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        final NumericalPropagatorBuilder propagatorBuilder2 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
        final List<ObservedMeasurement<?>> multiplexed = multiplexMeasurements(independentMeasurements, 60.0);

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
        ParameterDriver a0Driver = parameters.get(0);
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
        Assertions.assertEquals(0.0010514, Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                          before.getPVCoordinates().getVelocity()), 1.0e-6);
        EstimationTestUtils.checkFit(context, estimator, 2, 3,
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
                          determined.getPosition()), 3.3e-6);
        Assertions.assertEquals(0.0, Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                          determined.getPVCoordinates().getVelocity()), 1.6e-9);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if (driver.getName().startsWith("a[")) {
                // user-specified reference date
                Assertions.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assertions.assertEquals(0,
                        driver.getReferenceDate().durationFrom(propagatorBuilder1.getInitialOrbitDate()), 1.0e-15);
            }
        }

    }

    @Test
    public void testWrappedException() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
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
            EstimationTestUtils.checkFit(context, estimator, 3, 4,
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

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect range rate measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements1 =
                        EstimationTestUtils.createMeasurements(propagator,
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

        EstimationTestUtils.checkFit(context, estimator, 1, 2,
                                     0.0, 5.3e-7,
                                     0.0, 1.3e-6,
                                     0.0, 8.4e-4,
                                     0.0, 5.1e-7);
    }

    /**
     * Perfect range and range rate measurements with a perfect start
     */
    @Test
    public void testKeplerRangeAndRangeRate() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final List<ObservedMeasurement<?>> measurementsRange =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurementsRangeRate =
                        EstimationTestUtils.createMeasurements(propagator,
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
        EstimationTestUtils.checkFit(context, estimator, 1, 2,
                                     0.0, 4.6e7,
                                     0.0, 1.8e-6,
                                     0.0, 5.8e-7,
                                     0.0, 2.7e-10);
    }

    /**
     * Test if the parameter µ is taken into account by the builder even if no attraction force has been added yet.
     */
    @Test
    public void testIssue359() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // Select the central attraction coefficient (here there is only the central attraction coefficient)
        // as estimated parameter
        propagatorBuilder.getPropagationParametersDrivers().getDrivers().get(0).setSelected(true);
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

        for (final ObservedMeasurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }

        ParameterDriversList estimatedParameters = estimator.getPropagatorParametersDrivers(true);
        // Verify that the propagator, the builder and the estimator know mu
        final String driverName = NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT;
        Assertions.assertTrue(propagator.getAllForceModels().get(0) instanceof NewtonianAttraction);
        Assertions.assertTrue(propagatorBuilder.getAllForceModels().get(0) instanceof NewtonianAttraction);
        Assertions.assertNotNull(estimatedParameters.findByName(driverName));
        Assertions.assertTrue(propagator.getAllForceModels().get(0).getParameterDriver(driverName).isSelected());
        Assertions.assertTrue(propagatorBuilder.getAllForceModels().get(0).getParameterDriver(driverName).isSelected());
    }

    @Test
    public void testEstimateOnlyOneOrbitalParameter() {
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ true,  false, false, false, false, false });
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false,  true, false, false, false, false });
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false, false,  true, false, false, false });
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false, false, false,  true, false, false });
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false, false, false, false,  true, false });
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false, false, false, false, false,  true });
    }

    @Test
    public void testEstimateOnlyFewOrbitalParameters() {
        doTestEstimateOnlySomeOrbitalParameters(new boolean[]{ false,  true, false, true, false, false });
    }

    private void doTestEstimateOnlySomeOrbitalParameters(boolean[] orbitalParametersEstimated) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                      1.0e-6, 60.0, 1.0e-3);

        // create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                EstimationTestUtils.createMeasurements(propagator,
                                                       new PVMeasurementCreator(),
                                                       0.0, 1.0, 300.0);

        List<ParameterDriversList.DelegatingDriver> orbitalElementsDrivers = propagatorBuilder.getOrbitalParametersDrivers().getDrivers();
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
        for (final ObservedMeasurement<?> measurement : measurements) {
            estimator.addMeasurement(measurement);
        }
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

    /** Multiplex measurements.
     * @param independentMeasurements independent measurements
     * @param tol tolerance on time difference for multiplexed measurements
     * @return multiplexed measurements
     */
    private List<ObservedMeasurement<?>> multiplexMeasurements(final List<ObservedMeasurement<?>> independentMeasurements,
                                                               final double tol) {
        final List<ObservedMeasurement<?>> multiplexed = new ArrayList<>();
        independentMeasurements.sort(new ChronologicalComparator());
        List<ObservedMeasurement<?>> clump = new ArrayList<>();
        for (final ObservedMeasurement<?> measurement : independentMeasurements) {
            if (!clump.isEmpty() && measurement.getDate().durationFrom(clump.get(0).getDate()) > tol) {

                // previous clump is finished
                if (clump.size() == 1) {
                    multiplexed.add(clump.get(0));
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
            multiplexed.add(clump.get(0));
        } else {
            multiplexed.add(new MultiplexedMeasurement(clump));
        }
        return multiplexed;
    }

}


