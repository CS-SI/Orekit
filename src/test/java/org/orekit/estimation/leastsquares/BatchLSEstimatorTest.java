/* Copyright 2002-2017 CS Systèmes d'Information
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
import java.util.List;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem.Evaluation;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Force;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.InterSatellitesRangeMeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.OnBoardAntennaRangeModifier;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

public class BatchLSEstimatorTest {

    @Test
    public void testKeplerPV() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
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
        Assert.assertEquals(6,       normalizedCovariances.getRowDimension());
        Assert.assertEquals(6,       normalizedCovariances.getColumnDimension());
        Assert.assertEquals(6,       physicalCovariances.getRowDimension());
        Assert.assertEquals(6,       physicalCovariances.getColumnDimension());
        Assert.assertEquals(0.00258, physicalCovariances.getEntry(0, 0), 1.0e-5);

    }

    @Test
    public void testKeplerRange() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
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
                                            EstimationsProvider evaluationsProvider, Evaluation lspEvaluation)
                throws OrekitException {
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

        EstimationTestUtils.checkFit(context, estimator, 2, 3,
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
    public void testKeplerRangeWithOnBoardAntennaOffset() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));
        final Vector3D antennaPhaseCenter = new Vector3D(-1.2, 2.3, -0.7);

        // create perfect range measurements with antenna offset
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context, antennaPhaseCenter),
                                                               1.0, 3.0, 300.0);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        final OnBoardAntennaRangeModifier obaModifier = new OnBoardAntennaRangeModifier(antennaPhaseCenter);
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
                                            EstimationsProvider evaluationsProvider, Evaluation lspEvaluation)
                throws OrekitException {
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
                Assert.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assert.assertEquals(0, driver.getReferenceDate().durationFrom(propagatorBuilder.getInitialOrbitDate()), 1.0e-15);
            }
        }

    }

    @Test
    public void testMultiSat() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 1.0);
        final NumericalPropagatorBuilder propagatorBuilder2 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
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
        closePropagator.setEphemerisMode();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = closePropagator.getGeneratedEphemeris();
        Propagator propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                     propagatorBuilder1);
        final List<ObservedMeasurement<?>> r12 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris),
                                                               1.0, 3.0, 300.0);

        // create perfect range measurements for first satellite
        propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                           propagatorBuilder1);
        final List<ObservedMeasurement<?>> r1 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new RangeMeasurementCreator(context),
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
                                            EstimationsProvider evaluationsProvider, Evaluation lspEvaluation)
                throws OrekitException {
                if (iterationsCount == lastIter) {
                    Assert.assertEquals(lastEval + 1, evaluationscount);
                } else {
                    Assert.assertEquals(lastIter + 1, iterationsCount);
                }
                lastIter = iterationsCount;
                lastEval = evaluationscount;
                Assert.assertEquals(r12.size() + r1.size(), evaluationsProvider.getNumber());
                try {
                    evaluationsProvider.getEstimatedMeasurement(-1);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assert.assertEquals(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE, oe.getSpecifier());
                }
                try {
                    evaluationsProvider.getEstimatedMeasurement(r12.size() + r1.size());
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

        List<DelegatingDriver> parameters = estimator.getOrbitalParametersDrivers(true).getDrivers();
        ParameterDriver a0Driver = parameters.get(0);
        Assert.assertEquals("a[0]", a0Driver.getName());
        a0Driver.setValue(a0Driver.getValue() + 1.2);
        a0Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        ParameterDriver a1Driver = parameters.get(6);
        Assert.assertEquals("a[1]", a1Driver.getName());
        a1Driver.setValue(a1Driver.getValue() - 5.4);
        a1Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        final Orbit before = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngle.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assert.assertEquals(4.7246,
                            Vector3D.distance(closeOrbit.getPVCoordinates().getPosition(),
                                              before.getPVCoordinates().getPosition()),
                            1.0e-3);
        Assert.assertEquals(0.0010514,
                            Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                                              before.getPVCoordinates().getVelocity()),
                            1.0e-6);
        EstimationTestUtils.checkFit(context, estimator, 2, 3,
                                     0.0, 2.3e-06,
                                     0.0, 6.6e-06,
                                     0.0, 6.2e-07,
                                     0.0, 2.8e-10);

        final Orbit determined = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngle.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assert.assertEquals(0.0,
                            Vector3D.distance(closeOrbit.getPVCoordinates().getPosition(),
                                              determined.getPVCoordinates().getPosition()),
                            1.6e-6);
        Assert.assertEquals(0.0,
                            Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                                              determined.getPVCoordinates().getVelocity()),
                            1.6e-9);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if (driver.getName().startsWith("a[")) {
                // user-specified reference date
                Assert.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assert.assertEquals(0, driver.getReferenceDate().durationFrom(propagatorBuilder1.getInitialOrbitDate()), 1.0e-15);
            }
        }

    }

    /** A modified version of the previous test with a selection of propagation drivers to estimate
     *  One common (µ)
     *  Some specifics for each satellite (Cr and Ca)
     * 
     * @throws OrekitException
     */
    @Test
    public void testMultiSatWithParameters() throws OrekitException {

        // Test: Set the propagator drivers to estimate for each satellite
        final boolean muEstimated  = true;
        final boolean crEstimated1 = true;
        final boolean caEstimated1 = true;
        final boolean crEstimated2 = true;
        final boolean caEstimated2 = false;
        

        // Builder sat 1
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final NumericalPropagatorBuilder propagatorBuilder1 =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 1.0, Force.POTENTIAL, Force.SOLAR_RADIATION_PRESSURE);

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
                        context2.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 1.0, Force.POTENTIAL, Force.SOLAR_RADIATION_PRESSURE);

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
        closePropagator.setEphemerisMode();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = closePropagator.getGeneratedEphemeris();
        Propagator propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                     propagatorBuilder1);
        final List<ObservedMeasurement<?>> r12 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new InterSatellitesRangeMeasurementCreator(ephemeris),
                                                               1.0, 3.0, 300.0);

        // create perfect range measurements for first satellite
        propagator1 = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                           propagatorBuilder1);
        final List<ObservedMeasurement<?>> r1 =
                        EstimationTestUtils.createMeasurements(propagator1,
                                                               new RangeMeasurementCreator(context),
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
                                            EstimationsProvider evaluationsProvider, Evaluation lspEvaluation)
                throws OrekitException {
                if (iterationsCount == lastIter) {
                    Assert.assertEquals(lastEval + 1, evaluationscount);
                } else {
                    Assert.assertEquals(lastIter + 1, iterationsCount);
                }
                lastIter = iterationsCount;
                lastEval = evaluationscount;

                AbsoluteDate previous = AbsoluteDate.PAST_INFINITY;
                for (int i = 0; i < evaluationsProvider.getNumber(); ++i) {
                    AbsoluteDate current = evaluationsProvider.getEstimatedMeasurement(i).getDate();
                    Assert.assertTrue(current.compareTo(previous) >= 0);
                    previous = current;
                }
            }
        });

        List<DelegatingDriver> parameters = estimator.getOrbitalParametersDrivers(true).getDrivers();
        ParameterDriver a0Driver = parameters.get(0);
        Assert.assertEquals("a[0]", a0Driver.getName());
        a0Driver.setValue(a0Driver.getValue() + 1.2);
        a0Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        ParameterDriver a1Driver = parameters.get(6);
        Assert.assertEquals("a[1]", a1Driver.getName());
        a1Driver.setValue(a1Driver.getValue() - 5.4);
        a1Driver.setReferenceDate(AbsoluteDate.GALILEO_EPOCH);

        final Orbit before = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngle.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assert.assertEquals(4.7246,
                            Vector3D.distance(closeOrbit.getPVCoordinates().getPosition(),
                                              before.getPVCoordinates().getPosition()),
                            1.0e-3);
        Assert.assertEquals(0.0010514,
                            Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                                              before.getPVCoordinates().getVelocity()),
                            1.0e-6);
        EstimationTestUtils.checkFit(context, estimator, 4, 5,
                                     0.0, 6.0e-06,
                                     0.0, 1.7e-05,
                                     0.0, 4.4e-07,
                                     0.0, 1.7e-10);

        final Orbit determined = new KeplerianOrbit(parameters.get( 6).getValue(),
                                                    parameters.get( 7).getValue(),
                                                    parameters.get( 8).getValue(),
                                                    parameters.get( 9).getValue(),
                                                    parameters.get(10).getValue(),
                                                    parameters.get(11).getValue(),
                                                    PositionAngle.TRUE,
                                                    closeOrbit.getFrame(),
                                                    closeOrbit.getDate(),
                                                    closeOrbit.getMu());
        Assert.assertEquals(0.0,
                            Vector3D.distance(closeOrbit.getPVCoordinates().getPosition(),
                                              determined.getPVCoordinates().getPosition()),
                            5.8e-6);
        Assert.assertEquals(0.0,
                            Vector3D.distance(closeOrbit.getPVCoordinates().getVelocity(),
                                              determined.getPVCoordinates().getVelocity()),
                            3.5e-9);

        // after the call to estimate, the parameters lacking a user-specified reference date
        // got a default one
        for (final ParameterDriver driver : estimator.getOrbitalParametersDrivers(true).getDrivers()) {
            if (driver.getName().startsWith("a[")) {
                // user-specified reference date
                Assert.assertEquals(0, driver.getReferenceDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
            } else {
                // default reference date
                Assert.assertEquals(0, driver.getReferenceDate().durationFrom(propagatorBuilder1.getInitialOrbitDate()), 1.0e-15);
            }
        }

    }
    
    @Test
    public void testWrappedException() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
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
            EstimationTestUtils.checkFit(context, estimator, 3, 4,
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

    @Test
    public void testKeplerRangeRate() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect range rate measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements1 =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false),
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

        EstimationTestUtils.checkFit(context, estimator, 2, 3,
                                     0.0, 1.6e-2,
                                     0.0, 3.4e-2,
                                     0.0, 170.0,  // we only have range rate...
                                     0.0, 6.5e-2);
    }

    @Test
    public void testKeplerRangeAndRangeRate() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 1.0);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final List<ObservedMeasurement<?>> measurementsRange =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        final List<ObservedMeasurement<?>> measurementsRangeRate =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeRateMeasurementCreator(context, false),
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
                                     0.0, 0.16,
                                     0.0, 0.40,
                                     0.0, 2.1e-3,
                                     0.0, 8.1e-7);
    }

}


