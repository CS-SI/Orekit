/* Copyright 2002-2024 CS GROUP
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.BrouwerLyddaneContext;
import org.orekit.estimation.BrouwerLyddaneEstimationTestUtils;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeRateMeasurementCreator;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.PhaseCentersRangeModifier;
import org.orekit.frames.LOFType;
import org.orekit.gnss.antenna.FrequencyPattern;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.BrouwerLyddanePropagatorBuilder;
import org.orekit.utils.ParameterDriversList;

public class BrouwerLyddaneBatchLSEstimatorTest {

    /**
     * Perfect PV measurements with a perfect start
     */
    @Test
    public void testPV() {

        BrouwerLyddaneContext context = BrouwerLyddaneEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final BrouwerLyddanePropagatorBuilder propagatorBuilder = context.createBuilder(PositionAngleType.MEAN, true, 1.0);

        // create perfect PV measurements
        final Propagator propagator = BrouwerLyddaneEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                         propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        BrouwerLyddaneEstimationTestUtils.createMeasurements(propagator,
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

        BrouwerLyddaneEstimationTestUtils.checkFit(context, estimator, 1, 1,
                                                   0.0, 1.0e-15,
                                                   0.0, 1.0e-15,
                                                   0.0, 1.0e-15,
                                                   0.0, 1.0e-15);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assertions.assertEquals(6, normalizedCovariances.getRowDimension());
        Assertions.assertEquals(6, normalizedCovariances.getColumnDimension());
        Assertions.assertEquals(6, physicalCovariances.getRowDimension());
        Assertions.assertEquals(6, physicalCovariances.getColumnDimension());
        Assertions.assertEquals(0.0, physicalCovariances.getEntry(0, 0), 1.7e-15);

    }

    /** Test PV measurements generation and backward propagation in least-square orbit determination. */
    @Test
    public void testKeplerPVBackward() {

        BrouwerLyddaneContext context = BrouwerLyddaneEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final BrouwerLyddanePropagatorBuilder propagatorBuilder =
                        context.createBuilder(PositionAngleType.MEAN, true, 1.0);

        // create perfect PV measurements
        final Propagator propagator = BrouwerLyddaneEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                         propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        BrouwerLyddaneEstimationTestUtils.createMeasurements(propagator,
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

        BrouwerLyddaneEstimationTestUtils.checkFit(context, estimator, 1, 1,
                                                   0.0, 1.0e-15,
                                                   0.0, 1.0e-15,
                                                   0.0, 1.0e-15,
                                                   0.0, 1.0e-15);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assertions.assertEquals(6, normalizedCovariances.getRowDimension());
        Assertions.assertEquals(6, normalizedCovariances.getColumnDimension());
        Assertions.assertEquals(6, physicalCovariances.getRowDimension());
        Assertions.assertEquals(6, physicalCovariances.getColumnDimension());
        Assertions.assertEquals(0.0, physicalCovariances.getEntry(0, 0), 1.7e-15);

    }

    /**
     * Perfect range measurements with a perfect start
     */
    @Test
    public void testKeplerRange() {

        BrouwerLyddaneContext context = BrouwerLyddaneEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final BrouwerLyddanePropagatorBuilder propagatorBuilder =
                        context.createBuilder(PositionAngleType.TRUE, true, 1.0);

        // create perfect range measurements
        final Propagator propagator = BrouwerLyddaneEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        BrouwerLyddaneEstimationTestUtils.createMeasurements(propagator,
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

        BrouwerLyddaneEstimationTestUtils.checkFit(context, estimator, 2, 5,
                                                   0.0, 6.7e-2,
                                                   0.0, 0.12,
                                                   0.0, 9.9e-3,
                                                   0.0, 1.5e-5);

    }

    /**
     * Perfect range measurements with a perfect start and an on-board antenna range offset
     */
    @Test
    public void testKeplerRangeWithOnBoardAntennaOffset() {

        BrouwerLyddaneContext context = BrouwerLyddaneEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final BrouwerLyddanePropagatorBuilder propagatorBuilder =
                        context.createBuilder(PositionAngleType.MEAN, true, 1.0);
        propagatorBuilder.setAttitudeProvider(new LofOffset(propagatorBuilder.getFrame(), LOFType.LVLH));
        final Vector3D antennaPhaseCenter = new Vector3D(-1.2, 2.3, -0.7);

        // create perfect range measurements with antenna offset
        final Propagator propagator = BrouwerLyddaneEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        BrouwerLyddaneEstimationTestUtils.createMeasurements(propagator,
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

        BrouwerLyddaneEstimationTestUtils.checkFit(context, estimator, 4, 5,
                                                   0.0, 0.138,
                                                   0.0, 0.248,
                                                   0.0, 0.022,
                                                   0.0, 3.0e-5);

    }

    /**
     * Perfect range rate measurements with a perfect start
     */
    @Test
    public void testKeplerRangeRate() {

        BrouwerLyddaneContext context = BrouwerLyddaneEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final BrouwerLyddanePropagatorBuilder propagatorBuilder =
                        context.createBuilder(PositionAngleType.MEAN, true, 1.0);

        // create perfect range rate measurements
        final Propagator propagator = BrouwerLyddaneEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                         propagatorBuilder);
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockDriftDriver().setValue(groundClockDrift);
        }
        final double satClkDrift = 3.2e-10;
        final List<ObservedMeasurement<?>> measurements1 =
                        BrouwerLyddaneEstimationTestUtils.createMeasurements(propagator,
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
        estimator.setParametersConvergenceThreshold(1.0e-2);
        estimator.setMaxIterations(10);
        estimator.setMaxEvaluations(20);

        BrouwerLyddaneEstimationTestUtils.checkFit(context, estimator, 2, 3,
                                                   0.0, 5.1e-5,
                                                   0.0, 8.0e-5,
                                                   0.0, 1.6e-2,
                                                   0.0, 1.9e-5);
    }

    @Test
    public void testWrappedException() {

        BrouwerLyddaneContext context = BrouwerLyddaneEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final BrouwerLyddanePropagatorBuilder propagatorBuilder =
                        context.createBuilder(PositionAngleType.MEAN, true, 1.0);

        // create perfect range measurements
        final Propagator propagator = BrouwerLyddaneEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                                         propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        BrouwerLyddaneEstimationTestUtils.createMeasurements(propagator,
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
            BrouwerLyddaneEstimationTestUtils.checkFit(context, estimator, 3, 4,
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

}
