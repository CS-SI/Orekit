/* Copyright 2002-2022 CS GROUP
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem.Evaluation;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.EcksteinHechlerContext;
import org.orekit.estimation.EcksteinHechlerEstimationTestUtils;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeMeasurementCreator;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.OnBoardAntennaRangeModifier;
import org.orekit.frames.LOFType;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.EcksteinHechlerPropagatorBuilder;
import org.orekit.utils.ParameterDriversList;

public class EcksteinHechlerBatchLSEstimatorTest {

    /**
     * Perfect PV measurements with a perfect start
     */
    @Test
    public void testPV() {

        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.smallEccentricContext("regular-data:potential:tides");

        final EcksteinHechlerPropagatorBuilder propagatorBuilder = context.createBuilder(PositionAngle.MEAN, true, 1.0);

        // create perfect PV measurements
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                          propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
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

        EcksteinHechlerEstimationTestUtils.checkFit(context, estimator, 1, 1,
                                                    0.0, 1.0e-15,
                                                    0.0, 1.0e-15,
                                                    0.0, 1.0e-15,
                                                    0.0, 1.0e-15);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assert.assertEquals(6,       normalizedCovariances.getRowDimension());
        Assert.assertEquals(6,       normalizedCovariances.getColumnDimension());
        Assert.assertEquals(6,       physicalCovariances.getRowDimension());
        Assert.assertEquals(6,       physicalCovariances.getColumnDimension());

    }

    /** Test PV measurements generation and backward propagation in least-square orbit determination. */
    @Test
    public void testKeplerPVBackward() {

        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.smallEccentricContext("regular-data:potential:tides");

        final EcksteinHechlerPropagatorBuilder propagatorBuilder =
                        context.createBuilder(PositionAngle.MEAN, true, 1.0);

        // create perfect PV measurements
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                         propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
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

        EcksteinHechlerEstimationTestUtils.checkFit(context, estimator, 1, 1,
                                                    0.0, 1.0e-15,
                                                    0.0, 1.0e-15,
                                                    0.0, 1.0e-15,
                                                    0.0, 1.0e-15);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assert.assertEquals(6,       normalizedCovariances.getRowDimension());
        Assert.assertEquals(6,       normalizedCovariances.getColumnDimension());
        Assert.assertEquals(6,       physicalCovariances.getRowDimension());
        Assert.assertEquals(6,       physicalCovariances.getColumnDimension());

    }

    /**
     * Perfect range measurements with a perfect start
     */
    @Test
    public void testKeplerRange() {

        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.smallEccentricContext("regular-data:potential:tides");

        final EcksteinHechlerPropagatorBuilder propagatorBuilder =
                        context.createBuilder(PositionAngle.MEAN, true, 1.0);

        // create perfect range measurements
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
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

        EcksteinHechlerEstimationTestUtils.checkFit(context, estimator, 1, 10,
                                                    0.0, 1.1e-6,
                                                    0.0, 3.0e-6,
                                                    0.0, 5.6e-8,
                                                    0.0, 5.4e-11);

    }

    /**
     * Perfect range measurements with a perfect start and an on-board antenna range offset
     */
    @Test
    public void testKeplerRangeWithOnBoardAntennaOffset() {

        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.smallEccentricContext("regular-data:potential:tides");

        final EcksteinHechlerPropagatorBuilder propagatorBuilder =
                        context.createBuilder(PositionAngle.MEAN, true, 1.0);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));
        final Vector3D antennaPhaseCenter = new Vector3D(-1.2, 2.3, -0.7);

        // create perfect range measurements with antenna offset
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
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

        EcksteinHechlerEstimationTestUtils.checkFit(context, estimator, 1, 11,
                                                   0.0, 4.3e-5,
                                                   0.0, 1.2e-4,
                                                   0.0, 3.0e-8,
                                                   0.0, 2.5e-11);

    }

    /**
     * Perfect range rate measurements with a perfect start
     */
    @Test
    public void testKeplerRangeRate() {

        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.smallEccentricContext("regular-data:potential:tides");

        final EcksteinHechlerPropagatorBuilder propagatorBuilder =
                        context.createBuilder(PositionAngle.MEAN, true, 1.0);

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

        EcksteinHechlerEstimationTestUtils.checkFit(context, estimator, 1, 10,
                                                   0.0, 7.4e-8,
                                                   0.0, 1.3e-7,
                                                   0.0, 3.2e-5,
                                                   0.0, 3.2e-8);
    }

    @Test
    public void testWrappedException() {

        EcksteinHechlerContext context = EcksteinHechlerEstimationTestUtils.smallEccentricContext("regular-data:potential:tides");

        final EcksteinHechlerPropagatorBuilder propagatorBuilder =
                        context.createBuilder(PositionAngle.MEAN, true, 1.0);

        // create perfect range measurements
        final Propagator propagator = EcksteinHechlerEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                         propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EcksteinHechlerEstimationTestUtils.createMeasurements(propagator,
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

}
