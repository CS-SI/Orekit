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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.stat.descriptive.rank.Median;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.TLEContext;
import org.orekit.estimation.TLEEstimationTestUtils;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.TLEPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Differentiation;
import org.orekit.utils.StateFunction;

import java.util.List;

public class TLEPVTest {

    @Test
    public void testStateDerivatives() {

        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final TLEPropagatorBuilder propagatorBuilder =
                        context.createBuilder(1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();
        final Propagator propagator = TLEEstimationTestUtils.createPropagator(initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        TLEEstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double[] errorsP = new double[3 * 6 * measurements.size()];
        double[] errorsV = new double[3 * 6 * measurements.size()];
        int indexP = 0;
        int indexV = 0;
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
                    if (j < 3) {
                        errorsP[indexP++] = relativeError;
                    } else {
                        errorsV[indexV++] = relativeError;
                    }
                }
            }

        }

        // median errors
        Assertions.assertEquals(0.0, new Median().evaluate(errorsP), 0.0);
        Assertions.assertEquals(0.0, new Median().evaluate(errorsV), 3.8e-10);

    }
    /** Test the PV constructor with standard deviations for position and velocity given as 2 double.
     */
    @Test
    public void testPVWithSingleStandardDeviations() {

        // Context
        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();

        // Dummy P, V, T
        final Vector3D     position = initialOrbit.getPosition();
        final Vector3D     velocity = initialOrbit.getPVCoordinates().getVelocity();
        final AbsoluteDate date     = initialOrbit.getDate();

        // Initialize standard deviations and weight
        final double sigmaP     = 10.;
        final double sigmaV     = 0.1;
        final double baseWeight = 0.5;

        // Reference covariance matrix and correlation coefficients
        final double[][] Pref = new double[6][6];
        for (int i = 0; i < 3; i++) {
            Pref[i][i]     = FastMath.pow(sigmaP, 2);
            Pref[i+3][i+3] = FastMath.pow(sigmaV, 2);
        }
        final double[][] corrCoefRef = MatrixUtils.createRealIdentityMatrix(6).getData();

        // Reference propagator numbers
        final ObservableSatellite[] sats = {
            new ObservableSatellite(0),
            new ObservableSatellite(2)
        };

        // Create PV measurements
        final PV[] pvs = new PV[2];
        pvs[0] = new PV(date, position, velocity, sigmaP, sigmaV, baseWeight, sats[0]);
        pvs[1] = new PV(date, position, velocity, sigmaP, sigmaV, baseWeight, sats[1]);

        // Tolerance
        final double eps = 1e-25; // tolerance

        // Check data
        for (int k = 0; k < pvs.length; k++) {
            final PV pv = pvs[k];

            // Propagator numbers
            Assertions.assertEquals(sats[k].getPropagatorIndex(), pv.getSatellites().get(0).getPropagatorIndex(), eps);

            // Weights
            for (int i = 0; i < 6; i++) {
                Assertions.assertEquals(baseWeight, pv.getBaseWeight()[i], eps);
            }
            // Sigmas
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(sigmaP, pv.getTheoreticalStandardDeviation()[i]  , eps);
                Assertions.assertEquals(sigmaV, pv.getTheoreticalStandardDeviation()[i+3], eps);
            }
            // Covariances
            final double[][] P = pv.getCovarianceMatrix();
            // Substract with ref and get the norm
            final double normP = MatrixUtils.createRealMatrix(P).subtract(MatrixUtils.createRealMatrix(Pref)).getNorm1();
            Assertions.assertEquals(0., normP, eps);

            // Correlation coef
            final double[][] corrCoef = pv.getCorrelationCoefficientsMatrix();
            // Substract with ref and get the norm
            final double normCorrCoef = MatrixUtils.createRealMatrix(corrCoef).subtract(MatrixUtils.createRealMatrix(corrCoefRef)).getNorm1();
            Assertions.assertEquals(0., normCorrCoef, eps);
        }
    }

    /** Test the PV constructor with standard deviations for position and velocity given as a 6-sized vector.
     */
    @Test
    public void testPVWithVectorStandardDeviations() {

        // Context
        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();

        // Dummy P, V, T
        final Vector3D     position = initialOrbit.getPosition();
        final Vector3D     velocity = initialOrbit.getPVCoordinates().getVelocity();
        final AbsoluteDate date     = initialOrbit.getDate();

        // Initialize standard deviations and weight
        final double[] sigmaP     = {10., 20., 30.};
        final double[] sigmaV     = {0.1, 0.2, 0.3};
        final double[] sigmaPV    = {10., 20., 30., 0.1, 0.2, 0.3};
        final double baseWeight = 0.5;

        // Reference covariance matrix and correlation coefficients
        final double[][] Pref = new double[6][6];
        for (int i = 0; i < 3; i++) {
            Pref[i][i]     = FastMath.pow(sigmaP[i], 2);
            Pref[i+3][i+3] = FastMath.pow(sigmaV[i], 2);
        }
        final double[][] corrCoefRef = MatrixUtils.createRealIdentityMatrix(6).getData();

        // Reference propagator numbers
        final ObservableSatellite[] sats = {
            new ObservableSatellite(0),
            new ObservableSatellite(2),
            new ObservableSatellite(0),
            new ObservableSatellite(10)
        };

        // Create PV measurements
        final PV[] pvs = new PV[4];
        pvs[0] = new PV(date, position, velocity, sigmaP, sigmaV, baseWeight, sats[0]);
        pvs[1] = new PV(date, position, velocity, sigmaP, sigmaV, baseWeight, sats[1]);
        pvs[2] = new PV(date, position, velocity, sigmaPV, baseWeight, sats[2]);
        pvs[3] = new PV(date, position, velocity, sigmaPV, baseWeight, sats[3]);

        // Tolerance
        final double eps = 1e-25; // tolerance

        // Check data
        for (int k = 0; k < pvs.length; k++) {
            final PV pv = pvs[k];

            // Propagator numbers
            Assertions.assertEquals(sats[k].getPropagatorIndex(), pv.getSatellites().get(0).getPropagatorIndex(), eps);

            // Weights
            for (int i = 0; i < 6; i++) {
                Assertions.assertEquals(baseWeight, pv.getBaseWeight()[i], eps);
            }
            // Sigmas
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(sigmaP[i], pv.getTheoreticalStandardDeviation()[i]  , eps);
                Assertions.assertEquals(sigmaV[i], pv.getTheoreticalStandardDeviation()[i+3], eps);
            }
            // Covariances
            final double[][] P = pv.getCovarianceMatrix();
            // Substract with ref and get the norm
            final double normP = MatrixUtils.createRealMatrix(P).subtract(MatrixUtils.createRealMatrix(Pref)).getNorm1();
            Assertions.assertEquals(0., normP, eps);

            // Correlation coef
            final double[][] corrCoef = pv.getCorrelationCoefficientsMatrix();
            // Substract with ref and get the norm
            final double normCorrCoef = MatrixUtils.createRealMatrix(corrCoef).subtract(MatrixUtils.createRealMatrix(corrCoefRef)).getNorm1();
            Assertions.assertEquals(0., normCorrCoef, eps);
        }
    }

    /** Test the PV constructor with two 3x3 covariance matrix (one for position, the other for velocity) as input.
     */
    @Test
    public void testPVWithTwoCovarianceMatrices() {
        // Context
        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();

        // Dummy P, V, T
        final Vector3D     position = initialOrbit.getPosition();
        final Vector3D     velocity = initialOrbit.getPVCoordinates().getVelocity();
        final AbsoluteDate date     = initialOrbit.getDate();

        // Initialize standard deviations and weight
        final double[][] positionP = {{100., 400., 1200.}, {400., 400., 1800.}, {1200., 1800., 900.}};
        final double[][] velocityP = {{0.01, 0.04, 0.12 }, {0.04, 0.04, 0.18 }, {0.12 , 0.18 , 0.09}};
        final double baseWeight = 0.5;

        // Reference covariance matrix and correlation coefficients
        final double[][] Pref = new double[6][6];
        for (int i = 0; i < 3; i++) {
            for (int j = i; j < 3; j++) {
                Pref[i][j]     = positionP[i][j];
                Pref[j][i]     = positionP[i][j];
                Pref[i+3][j+3] = velocityP[i][j];
                Pref[j+3][i+3] = velocityP[i][j];
            }
        }
        final double[][] corrCoefRef3 = {{1., 2., 4.}, {2., 1., 3.}, {4., 3., 1.}};
        final double[][] corrCoefRef  = new double[6][6];
        for (int i = 0; i < 3; i++) {
            for (int j = i; j < 3; j++) {
                corrCoefRef[i][j]     = corrCoefRef3[i][j];
                corrCoefRef[j][i]     = corrCoefRef3[i][j];
                corrCoefRef[i+3][j+3] = corrCoefRef3[i][j];
                corrCoefRef[j+3][i+3] = corrCoefRef3[i][j];
            }
        }

        // Reference propagator numbers
        final ObservableSatellite[] sats = {
            new ObservableSatellite(0),
            new ObservableSatellite(2)
        };

        // Reference standard deviations
        final double[] sigmaP = {10., 20., 30.};
        final double[] sigmaV = {0.1, 0.2, 0.3};

        // Create PV measurements
        final PV[] pvs = new PV[2];
        pvs[0] = new PV(date, position, velocity, positionP, velocityP, baseWeight, sats[0]);
        pvs[1] = new PV(date, position, velocity, positionP, velocityP, baseWeight, sats[1]);

        // Tolerance
        final double eps = 6.7e-16; // tolerance

        // Check data
        for (int k = 0; k < pvs.length; k++) {
            final PV pv = pvs[k];

            // Propagator numbers
            Assertions.assertEquals(sats[k].getPropagatorIndex(), pv.getSatellites().get(0).getPropagatorIndex(), eps);

            // Weights
            for (int i = 0; i < 6; i++) {
                Assertions.assertEquals(baseWeight, pv.getBaseWeight()[i], eps);
            }
            // Sigmas
            for (int i = 0; i < 3; i++) {
                Assertions.assertEquals(sigmaP[i], pv.getTheoreticalStandardDeviation()[i]  , eps);
                Assertions.assertEquals(sigmaV[i], pv.getTheoreticalStandardDeviation()[i+3], eps);
            }
            // Covariances
            final double[][] P = pv.getCovarianceMatrix();
            // Substract with ref and get the norm
            final double normP = MatrixUtils.createRealMatrix(P).subtract(MatrixUtils.createRealMatrix(Pref)).getNorm1();
            Assertions.assertEquals(0., normP, eps);

            // Correlation coef
            final double[][] corrCoef = pv.getCorrelationCoefficientsMatrix();
            // Substract with ref and get the norm
            final double normCorrCoef = MatrixUtils.createRealMatrix(corrCoef).subtract(MatrixUtils.createRealMatrix(corrCoefRef)).getNorm1();
            Assertions.assertEquals(0., normCorrCoef, eps);
        }

    }

    /** Test the PV constructor with one 6x6 covariance matrix as input.
     */
    @Test
    public void testPVWithCovarianceMatrix() {
        // Context
        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();

        // Dummy P, V, T
        final Vector3D     position = initialOrbit.getPosition();
        final Vector3D     velocity = initialOrbit.getPVCoordinates().getVelocity();
        final AbsoluteDate date     = initialOrbit.getDate();

        // Initialize standard deviations, weight and corr coeff
        final double[] sigmaPV = {10., 20., 30., 0.1, 0.2, 0.3};
        final double baseWeight = 0.5;
        final double[][] corrCoefRef = new double[6][6];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                if (i == j) {
                    corrCoefRef[i][i] = 1.;
                } else {
                    corrCoefRef[i][j] = i + j + 1;
                }
            }
        }

        // Reference covariance matrix
        final double[][] Pref = new double[6][6];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                Pref[i][j] = corrCoefRef[i][j]*sigmaPV[i]*sigmaPV[j];
            }
        }

        // Reference propagator numbers
        final ObservableSatellite[] sats = {
            new ObservableSatellite(0),
            new ObservableSatellite(2)
        };

        // Create PV measurements
        final PV[] pvs = new PV[2];
        pvs[0] = new PV(date, position, velocity, Pref, baseWeight, sats[0]);
        pvs[1] = new PV(date, position, velocity, Pref, baseWeight, sats[1]);

        // Tolerance
        final double eps = 1.8e-15; // tolerance

        // Check data
        for (int k = 0; k < pvs.length; k++) {
            final PV pv = pvs[k];

            // Propagator numbers
            Assertions.assertEquals(sats[k].getPropagatorIndex(), pv.getSatellites().get(0).getPropagatorIndex(), eps);

            // Weights
            for (int i = 0; i < 6; i++) {
                Assertions.assertEquals(baseWeight, pv.getBaseWeight()[i], eps);
            }
            // Sigmas
            for (int i = 0; i < 6; i++) {
                Assertions.assertEquals(sigmaPV[i], pv.getTheoreticalStandardDeviation()[i]  , eps);
            }
            // Covariances
            final double[][] P = pv.getCovarianceMatrix();
            // Substract with ref and get the norm
            final double normP = MatrixUtils.createRealMatrix(P).subtract(MatrixUtils.createRealMatrix(Pref)).getNorm1();
            Assertions.assertEquals(0., normP, eps);

            // Correlation coef
            final double[][] corrCoef = pv.getCorrelationCoefficientsMatrix();
            // Substract with ref and get the norm
            final double normCorrCoef = MatrixUtils.createRealMatrix(corrCoef).subtract(MatrixUtils.createRealMatrix(corrCoefRef)).getNorm1();
            Assertions.assertEquals(0., normCorrCoef, eps);
        }

    }

    /** Test exceptions raised if the covariance matrix does not have the proper size. */
    @Test
    public void testExceptions() {
        // Context
        TLEContext context = TLEEstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final Orbit initialOrbit = TLEPropagator.selectExtrapolator(context.initialTLE).getInitialState().getOrbit();

        // Dummy P, V, T
        final Vector3D     position = initialOrbit.getPosition();
        final Vector3D     velocity = initialOrbit.getPVCoordinates().getVelocity();
        final AbsoluteDate date     = initialOrbit.getDate();
        final double       weight   = 1.;
        final ObservableSatellite sat = new ObservableSatellite(0);

        // Build with two 3-sized vectors
        try {
            new PV(date, position, velocity, new double[] {0., 0., 0.}, new double[] {1.}, weight, sat);
            Assertions.fail("An OrekitException should have been thrown");
        } catch (OrekitException e) {
            // An exception should indeed be raised here
        }

        // Build with one 6-sized vector
        try {
            new PV(date, position, velocity, new double[] {0., 0., 0.}, weight, sat);
            Assertions.fail("An OrekitException should have been thrown");
        } catch (OrekitException e) {
            // An exception should indeed be raised here
        }

        // Build with two 3x3 matrices
        try {
            new PV(date, position, velocity, new double[][] {{0., 0.}, {0., 0.}},
                   new double[][] {{0., 0.}, {0., 0.}}, weight, sat);
            Assertions.fail("An OrekitException should have been thrown");
        } catch (OrekitException e) {
            // An exception should indeed be raised here
        }

        // Build with one 6x6 matrix
        try {
            new PV(date, position, velocity, new double[][] {{0., 0.}, {0., 0.}}, weight, sat);
            Assertions.fail("An OrekitException should have been thrown");
        } catch (OrekitException e) {
            // An exception should indeed be raised here
        }
    }
}
