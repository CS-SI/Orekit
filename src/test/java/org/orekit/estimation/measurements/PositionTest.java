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
package org.orekit.estimation.measurements;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.stat.descriptive.rank.Median;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Differentiation;
import org.orekit.utils.StateFunction;

public class PositionTest {

    /** Compare observed values and estimated values.
     *  Both are calculated with a different algorithm
     */
    @Test
    public void testValues() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, false,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect right-ascension/declination measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PositionMeasurementCreator(),
                                                               1.0, 3.0, 300.0);

        propagator.clearStepHandlers();

        // Prepare statistics for position values difference
        final StreamingStatistics[] pvDiffStat = new StreamingStatistics[3];
        for (int i = 0; i < 3; i++) {
            pvDiffStat[i] = new StreamingStatistics();
        }

        for (final ObservedMeasurement<?> measurement : measurements) {

            // Propagate to measurement date
            final AbsoluteDate datemeas  = measurement.getDate();
            SpacecraftState    state     = propagator.propagate(datemeas);

            // Estimate the position value
            final EstimatedMeasurementBase<?> estimated = measurement.estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state });

            // Store the difference between estimated and observed values in the stats
            for (int i = 0; i < 3; i++) {
                pvDiffStat[i].addValue(FastMath.abs(estimated.getEstimatedValue()[i] - measurement.getObservedValue()[i]));
            }
        }

        // Mean and std errors check
        for (int i = 0; i < 3; i++) {
            // Check position values
            Assertions.assertEquals(0.0, pvDiffStat[i].getMean(), 3.8e-7);
            Assertions.assertEquals(0.0, pvDiffStat[i].getStandardDeviation(), 2.3e-7);
        }

        // Test measurement type
        Assertions.assertEquals(Position.MEASUREMENT_TYPE, measurements.get(0).getMeasurementType());
    }

    /** Test the values of the state derivatives using a numerical.
     * finite differences calculation as a reference
     */
    @Test
    public void testStateDerivatives() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PositionMeasurementCreator(),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double[] errorsP = new double[3 * 6 * measurements.size()];
        int indexP = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            final AbsoluteDate    date      = measurement.getDate();
            final SpacecraftState state     = propagator.propagate(date);
            final double[][]      jacobian  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getStateDerivatives(0);

            // compute a reference value using finite differences
            final double[][] finiteDifferencesJacobian =
                Differentiation.differentiate(new StateFunction() {
                    public double[] value(final SpacecraftState state) {
                        return measurement.
                               estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).
                               getEstimatedValue();
                    }
                                                  }, measurement.getDimension(),
                                                  propagator.getAttitudeProvider(), OrbitType.CARTESIAN,
                                                  PositionAngleType.TRUE, 1.0, 3).value(state);

            Assertions.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            Assertions.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);
            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    final double relativeError = FastMath.abs((finiteDifferencesJacobian[i][j] - jacobian[i][j]) /
                                                              finiteDifferencesJacobian[i][j]);
                    errorsP[indexP++] = relativeError;
                }
            }

        }

        // median errors
        Assertions.assertEquals(0.0, new Median().evaluate(errorsP), 2.1e-100);

    }

    /** Test the position constructor with standard deviations for position given as one double.
     */
    @Test
    public void testPositionWithSingleStandardDeviations() {

        // Context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Dummy P, T
        final Vector3D     position = context.initialOrbit.getPosition();
        final AbsoluteDate date     = context.initialOrbit.getDate();

        // Initialize standard deviations and weight
        final double sigmaP     = 10.;
        final double baseWeight = 0.5;

        // Reference covariance matrix and correlation coefficients
        final double[][] Pref = new double[3][3];
        for (int i = 0; i < 3; i++) {
            Pref[i][i]     = FastMath.pow(sigmaP, 2);
        }
        final double[][] corrCoefRef = MatrixUtils.createRealIdentityMatrix(3).getData();

        // Reference propagator numbers
        final ObservableSatellite[] sats = {
            new ObservableSatellite(0),
            new ObservableSatellite(2)
        };

        // Create PV measurements
        final Position[] ps = new Position[2];
        ps[0] = new Position(date, position, sigmaP, baseWeight, sats[0]);
        ps[1] = new Position(date, position, sigmaP, baseWeight, sats[1]);

        // Tolerance
        final double eps = 1e-20; // tolerance

        // Check data
        for (int k = 0; k < ps.length; k++) {
            final Position p = ps[k];

            // Propagator numbers
            Assertions.assertEquals(sats[k].getPropagatorIndex(), p.getSatellites().get(0).getPropagatorIndex());

            // Weights
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(baseWeight, p.getBaseWeight()[i], eps);
            }
            // Sigmas
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(sigmaP, p.getTheoreticalStandardDeviation()[i]  , eps);
            }
            // Covariances
            final double[][] P = p.getCovarianceMatrix();
            // Substract with ref and get the norm
            final double normP = MatrixUtils.createRealMatrix(P).subtract(MatrixUtils.createRealMatrix(Pref)).getNorm1();
            Assertions.assertEquals(0., normP, eps);

            // Correlation coef
            final double[][] corrCoef = p.getCorrelationCoefficientsMatrix();
            // Substract with ref and get the norm
            final double normCorrCoef = MatrixUtils.createRealMatrix(corrCoef).subtract(MatrixUtils.createRealMatrix(corrCoefRef)).getNorm1();
            Assertions.assertEquals(0., normCorrCoef, eps);
        }
    }

    /** Test the Position constructor with standard deviations for position given as a 3-sized vector.
     */
    @Test
    public void testPositionWithVectorStandardDeviations() {

        // Context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Dummy P, T
        final Vector3D     position = context.initialOrbit.getPosition();
        final AbsoluteDate date     = context.initialOrbit.getDate();

        // Initialize standard deviations and weight
        final double[] sigmaP  = {10., 20., 30.};
        final double baseWeight = 0.5;

        // Reference covariance matrix and correlation coefficients
        final double[][] Pref = new double[3][3];
        for (int i = 0; i < 3; i++) {
            Pref[i][i]     = FastMath.pow(sigmaP[i], 2);
        }
        final double[][] corrCoefRef = MatrixUtils.createRealIdentityMatrix(3).getData();

        // Reference propagator numbers
        final ObservableSatellite[] sats = {
            new ObservableSatellite(0),
            new ObservableSatellite(2)
        };

        // Create PV measurements
        final Position[] ps = new Position[2];
        ps[0] = new Position(date, position, sigmaP, baseWeight, sats[0]);
        ps[1] = new Position(date, position, sigmaP, baseWeight, sats[1]);

        // Tolerance
        final double eps = 1e-20; // tolerance

        // Check data
        for (int k = 0; k < ps.length; k++) {
            final Position p = ps[k];

            // Propagator numbers
            Assertions.assertEquals(sats[k].getPropagatorIndex(), p.getSatellites().get(0).getPropagatorIndex());

            // Weights
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(baseWeight, p.getBaseWeight()[i], eps);
            }
            // Sigmas
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(sigmaP[i], p.getTheoreticalStandardDeviation()[i]  , eps);
            }
            // Covariances
            final double[][] P = p.getCovarianceMatrix();
            // Substract with ref and get the norm
            final double normP = MatrixUtils.createRealMatrix(P).subtract(MatrixUtils.createRealMatrix(Pref)).getNorm1();
            Assertions.assertEquals(0., normP, eps);

            // Correlation coef
            final double[][] corrCoef = p.getCorrelationCoefficientsMatrix();
            // Substract with ref and get the norm
            final double normCorrCoef = MatrixUtils.createRealMatrix(corrCoef).subtract(MatrixUtils.createRealMatrix(corrCoefRef)).getNorm1();
            Assertions.assertEquals(0., normCorrCoef, eps);
        }
    }

    /** Test the Position constructor with 3x3 covariance matrix as input.
     */
    @Test
    public void testPositionWithCovarianceMatrix() {
        // Context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Dummy P, T
        final Vector3D     position = context.initialOrbit.getPosition();
        final AbsoluteDate date     = context.initialOrbit.getDate();

        // Initialize standard deviations and weight
        final double[][] positionP = {{100., 400., 1200.}, {400., 400., 1800.}, {1200., 1800., 900.}};
        final double baseWeight = 0.5;

        // Reference covariance matrix and correlation coefficients
        final double[][] Pref = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = i; j < 3; j++) {
                Pref[i][j]     = positionP[i][j];
                Pref[j][i]     = positionP[i][j];
            }
        }
        final double[][] corrCoefRef3 = {{1., 2., 4.}, {2., 1., 3.}, {4., 3., 1.}};
        final double[][] corrCoefRef  = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = i; j < 3; j++) {
                corrCoefRef[i][j]     = corrCoefRef3[i][j];
                corrCoefRef[j][i]     = corrCoefRef3[i][j];
            }
        }

        // Reference propagator numbers
        final ObservableSatellite[] sats = {
            new ObservableSatellite(0),
            new ObservableSatellite(2)
        };

        // Reference standard deviations
        final double[] sigmaP = {10., 20., 30.};

        // Create Position measurements
        final Position[] ps = new Position[2];
        ps[0] = new Position(date, position, positionP, baseWeight, sats[0]);
        ps[1] = new Position(date, position, positionP, baseWeight, sats[1]);

        // Tolerance
        final double eps = 6.7e-16; // tolerance

        // Check data
        for (int k = 0; k < ps.length; k++) {
            final Position p = ps[k];

            // Propagator numbers
            Assertions.assertEquals(sats[k].getPropagatorIndex(), p.getSatellites().get(0).getPropagatorIndex());

            // Weights
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(baseWeight, p.getBaseWeight()[i], eps);
            }
            // Sigmas
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(sigmaP[i], p.getTheoreticalStandardDeviation()[i]  , eps);
            }
            // Covariances
            final double[][] P = p.getCovarianceMatrix();
            // Substract with ref and get the norm
            final double normP = MatrixUtils.createRealMatrix(P).subtract(MatrixUtils.createRealMatrix(Pref)).getNorm1();
            Assertions.assertEquals(0., normP, eps);

            // Correlation coef
            final double[][] corrCoef = p.getCorrelationCoefficientsMatrix();
            // Substract with ref and get the norm
            final double normCorrCoef = MatrixUtils.createRealMatrix(corrCoef).subtract(MatrixUtils.createRealMatrix(corrCoefRef)).getNorm1();
            Assertions.assertEquals(0., normCorrCoef, eps);
        }

    }

    /** Test exceptions raised if the covariance matrix does not have the proper size. */
    @Test
    public void testExceptions() {
        // Context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Dummy P, T
        final Vector3D     position = context.initialOrbit.getPosition();
        final AbsoluteDate date     = context.initialOrbit.getDate();
        final double       weight   = 1.;

        // Build with one 3-sized vectors
        try {
            new Position(date, position, new double[] {1.}, weight, new ObservableSatellite(0));
            Assertions.fail("An OrekitException should have been thrown");
        } catch (OrekitException e) {
            // An exception should indeed be raised here
        }

        // Build with one 3x3 matrix
        try {
            new Position(date, position, new double[][] {{0., 0.}, {0., 0.}}, weight, new ObservableSatellite(0));
            Assertions.fail("An OrekitException should have been thrown");
        } catch (OrekitException e) {
            // An exception should indeed be raised here
        }
    }
}


