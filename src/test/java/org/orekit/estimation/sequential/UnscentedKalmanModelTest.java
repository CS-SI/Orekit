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
package org.orekit.estimation.sequential;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.MerweUnscentedTransform;
import org.hipparchus.util.UnscentedTransformProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Force;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class UnscentedKalmanModelTest {

    /** Orbit type for propagation. */
    private final OrbitType orbitType = OrbitType.CARTESIAN;

    /** Position angle for propagation. */
    private final PositionAngleType positionAngleType = PositionAngleType.TRUE;

    /** Initial orbit. */
    private Orbit orbit0;

    /** Propagator builder. */
    private NumericalPropagatorBuilder propagatorBuilder;

    /** Covariance matrix provider. */
    private CovarianceMatrixProvider covMatrixProvider;

    /** Estimated measurement parameters list. */
    private ParameterDriversList estimatedMeasurementsParameters;

    /** Kalman estimator containing models. */
    private UnscentedKalmanEstimator kalman;

    /** Unscented transform provider. */
    private UnscentedTransformProvider utProvider;

    /** Kalman observer. */
    private ModelLogger modelLogger;

    /** State size. */
    private int M;

    /** PV at t0. */
    private PV pv;

    /** Range after t0. */
    private Range range;

    /** Driver for satellite range bias. */
    private ParameterDriver satRangeBiasDriver;

    /** Driver for SRP coefficient. */
    private ParameterDriver srpCoefDriver;

    /** Tolerance for the test. */
    private final double tol = 5.0e-9;

    @BeforeEach
    public void setup() {
        // Create context
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Initial orbit and date
        this.orbit0 = context.initialOrbit;
        ObservableSatellite sat = new ObservableSatellite(0);

        // Create propagator builder
        this.propagatorBuilder = context.createBuilder(orbitType, positionAngleType, true,
                                                       1.0e-6, 60.0, 10., Force.SOLAR_RADIATION_PRESSURE);

        // Create PV at t0
        final AbsoluteDate date0 = context.initialOrbit.getDate();
        this.pv = new PV(date0,
                             context.initialOrbit.getPosition(),
                             context.initialOrbit.getPVCoordinates().getVelocity(),
                             new double[] {1., 1., 1., 1.e-3, 1.e-3, 1.e-3}, 1.,
                             sat);

        // Create one 0m range measurement at t0 + 10s
        final AbsoluteDate date  = date0.shiftedBy(10.);
        final GroundStation station = context.stations.get(0);
        this.range = new Range(station, true, date, 18616150., 10., 1., sat);
        // Exact range value is 1.8616150246470984E7 m

        // Add sat range bias to PV and select it
        final Bias<Range> satRangeBias = new Bias<Range>(new String[] {"sat range bias"},
                                                         new double[] {100.},
                                                         new double[] {10.},
                                                         new double[] {0.},
                                                         new double[] {100.});
        this.satRangeBiasDriver = satRangeBias.getParametersDrivers().get(0);
        satRangeBiasDriver.setSelected(true);
        satRangeBiasDriver.setReferenceDate(date);
        range.addModifier(satRangeBias);
        for (ParameterDriver driver : range.getParametersDrivers()) {
            driver.setReferenceDate(date);
        }

        // Gather list of meas parameters (only sat range bias here)
        this.estimatedMeasurementsParameters = new ParameterDriversList();
        for (final ParameterDriver driver : range.getParametersDrivers()) {
            if (driver.isSelected()) {
                estimatedMeasurementsParameters.add(driver);
            }
        }
        // Select SRP coefficient
        this.srpCoefDriver = propagatorBuilder.getPropagationParametersDrivers().
                        findByName(RadiationSensitive.REFLECTION_COEFFICIENT);
        srpCoefDriver.setReferenceDate(date);
        srpCoefDriver.setSelected(true);

        // Create a covariance matrix using the scales of the estimated parameters
        final double[] scales = getParametersScale(propagatorBuilder, estimatedMeasurementsParameters);
        this.M = scales.length;
        this.covMatrixProvider = setInitialCovarianceMatrix(scales);

        // Unscented  transform provider
        this.utProvider = new MerweUnscentedTransform(8);

        // Initialize Kalman
        final UnscentedKalmanEstimatorBuilder kalmanBuilder = new UnscentedKalmanEstimatorBuilder();
        kalmanBuilder.unscentedTransformProvider(utProvider);
        kalmanBuilder.addPropagationConfiguration(propagatorBuilder, covMatrixProvider);
        kalmanBuilder.estimatedMeasurementsParameters(estimatedMeasurementsParameters, null);
        this.kalman = kalmanBuilder.build();
        this.modelLogger = new ModelLogger();
        kalman.setObserver(modelLogger);
    }

    /** Test of the physical matrices and vectors returned by the methods from KalmanEstimation interface.
     *  First, we perform a check before any measurement is added. Most of the matrices should be null.
     *  Then we process a perfect PV at t0 in the Kalman and check the matrices.
     *  Finally we process a range measurement after t0 in the Kalman and check the matrices.
     */
    @Test
    public void ModelPhysicalOutputsTest() {

        // Check model at t0 before any measurement is added
        // -------------------------------------------------
        checkModelAtT0();


        // Check model after PV measurement at t0 is added
        // -----------------------------------------------

        // Constant process noise covariance matrix Q
        final RealMatrix Q = covMatrixProvider.getProcessNoiseMatrix(new SpacecraftState(orbit0),
                                                                     new SpacecraftState(orbit0));

        // Initial covariance matrix
        final RealMatrix P0 = covMatrixProvider.getInitialCovarianceMatrix(new SpacecraftState(orbit0));

        // Physical predicted covariance matrix at t0
        // State transition matrix is the identity matrix at t0
        RealMatrix Ppred = P0.add(Q);

        // Predicted orbit is equal to initial orbit at t0
        Orbit orbitPred = orbit0;

        // Add PV measurement and check model afterwards
        checkModelAfterMeasurementAdded(1, pv, Ppred, orbitPred);
    }

    /** Check the model physical outputs at t0 before any measurement is added. */
    private void checkModelAtT0() {

        // Instantiate a Model from attributes
        final UnscentedKalmanModel model = new UnscentedKalmanModel(Collections.singletonList(propagatorBuilder),
                                                  Collections.singletonList(covMatrixProvider),
                                                  estimatedMeasurementsParameters,
                                                  null);

        // Evaluate at t0
        // --------------

        // Time
        Assertions.assertEquals(0., model.getEstimate().getTime(), 0.);
        Assertions.assertEquals(0., model.getCurrentDate().durationFrom(orbit0.getDate()), 0.);

        // Measurement number
        Assertions.assertEquals(0, model.getCurrentMeasurementNumber());

        // Physical state
        final RealVector expX = MatrixUtils.createRealVector(M);
        final double[] orbitState0 = new double[6];
        orbitType.mapOrbitToArray(orbit0, positionAngleType, orbitState0, null);
        expX.setSubVector(0, MatrixUtils.createRealVector(orbitState0));
        expX.setEntry(6, srpCoefDriver.getReferenceValue());
        expX.setEntry(7, satRangeBiasDriver.getReferenceValue());
        Assertions.assertArrayEquals(model.getPhysicalEstimatedState().toArray(), expX.toArray(), tol);
        Assertions.assertArrayEquals(model.getEstimate().getState().toArray(),    expX.toArray(), tol);

        // Covariance - filled with 1
        final double[][] Pn = model.getEstimate().getCovariance().getData();
        final double[][] expPn = covMatrixProvider.getInitialCovarianceMatrix(null).getData();
        for (int i = 0; i < M; i++) {
            Assertions.assertArrayEquals(expPn[i], Pn[i], tol, "Failed on line " + i);
        }

        // Physical covariance = initialized
        final RealMatrix P   = model.getPhysicalEstimatedCovarianceMatrix();
        final RealMatrix expP = covMatrixProvider.getInitialCovarianceMatrix(new SpacecraftState(orbit0));
        final double[][] dP = P.subtract(expP).getData();
        for (int i = 0; i < M; i++) {
            Assertions.assertArrayEquals(new double[M], dP[i], tol, "Failed on line " + i);
        }

        // Check that other "physical" matrices are null
        Assertions.assertNull(model.getEstimate().getInnovationCovariance());
        Assertions.assertNull(model.getPhysicalInnovationCovarianceMatrix());
        Assertions.assertNull(model.getEstimate().getKalmanGain());
        Assertions.assertNull(model.getPhysicalKalmanGain());
        Assertions.assertNull(model.getEstimate().getMeasurementJacobian());
        Assertions.assertNull(model.getPhysicalMeasurementJacobian());
        Assertions.assertNull(model.getEstimate().getStateTransitionMatrix());
        Assertions.assertNull(model.getPhysicalStateTransitionMatrix());
    }

    /** Add a measurement to the Kalman filter.
     * Check model physical outputs afterwards.
     */
    private void checkModelAfterMeasurementAdded(final int expMeasurementNumber,
                                                final ObservedMeasurement<?> meas,
                                                final RealMatrix expPpred,
                                                final Orbit expOrbitPred) {

        // Predicted value of SRP coef and sat range bias
        // (= value before adding measurement to the filter)
        final double srpCoefPred = srpCoefDriver.getValue();
        final double satRangeBiasPred = satRangeBiasDriver.getValue();

        // Expected predicted measurement
        final double[] expMeasPred =
                        meas.estimate(0, 0,
                                      new SpacecraftState[] {new SpacecraftState(expOrbitPred)}).getEstimatedValue();

        // Process PV measurement in Kalman and get model
        kalman.processMeasurements(Collections.singletonList(meas));
        KalmanEstimation model = modelLogger.estimation;

        // Measurement size
        final int N = meas.getDimension();

        // Time
        Assertions.assertEquals(0., model.getCurrentDate().durationFrom(expOrbitPred.getDate()), 0.);

        // Measurement number
        Assertions.assertEquals(expMeasurementNumber, model.getCurrentMeasurementNumber());

        // State transition matrix (null for unscented kalman filter)
        Assertions.assertNull(model.getPhysicalStateTransitionMatrix());

        // Measurement matrix (null for unscented kalman filter)
        Assertions.assertNull(model.getPhysicalMeasurementJacobian());

        // Measurement covariance matrix
        final double[] measSigmas = meas.getTheoreticalStandardDeviation();
        final RealMatrix R = MatrixUtils.createRealMatrix(N, N);
        for (int i = 0; i < N; i++) {
            R.setEntry(i, i, measSigmas[i] * measSigmas[i]);
        }

        // Predicted orbit
        final Orbit orbitPred = model.getPredictedSpacecraftStates()[0].getOrbit();
        final PVCoordinates pvOrbitPred = orbitPred.getPVCoordinates();
        final PVCoordinates expPVOrbitPred = expOrbitPred.getPVCoordinates();
        final double dpOrbitPred = Vector3D.distance(expPVOrbitPred.getPosition(), pvOrbitPred.getPosition());
        final double dvOrbitPred = Vector3D.distance(expPVOrbitPred.getVelocity(), pvOrbitPred.getVelocity());
        Assertions.assertEquals(0., dpOrbitPred, tol);
        Assertions.assertEquals(0., dvOrbitPred, tol);

        // Predicted measurement
        final double[] measPred = model.getPredictedMeasurement().getEstimatedValue();
        Assertions.assertArrayEquals(expMeasPred, measPred, 3.0e-6);

        // Predicted state
        final double[] orbitPredState = new double[6];
        orbitPred.getType().mapOrbitToArray(orbitPred, PositionAngleType.TRUE, orbitPredState, null);
        final RealVector expXpred = MatrixUtils.createRealVector(M);
        for (int i = 0; i < 6; i++) {
            expXpred.setEntry(i, orbitPredState[i]);
        }
        expXpred.setEntry(6, srpCoefPred);
        expXpred.setEntry(7, satRangeBiasPred);

    }

    /** Get an array of the scales of the estimated parameters.
     * @param builder propagator builder
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @return array containing the scales of the estimated parameter
     */
    private double[] getParametersScale(final NumericalPropagatorBuilder builder,
                                       ParameterDriversList estimatedMeasurementsParameters) {
        final List<Double> scaleList = new ArrayList<>();

        // Orbital parameters
        for (ParameterDriver driver : builder.getOrbitalParametersDrivers().getDrivers()) {
            if (driver.isSelected()) {
                scaleList.add(driver.getScale());
            }
        }

        // Propagation parameters
        for (ParameterDriver driver : builder.getPropagationParametersDrivers().getDrivers()) {
            if (driver.isSelected()) {
                scaleList.add(driver.getScale());
            }
        }

        // Measurement parameters
        for (ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            if (driver.isSelected()) {
                scaleList.add(driver.getScale());
            }
        }

        final double[] scales = new double[scaleList.size()];
        for (int i = 0; i < scaleList.size(); i++) {
            scales[i] = scaleList.get(i);
        }
        return scales;
    }

    /** Create a covariance matrix provider with initial and process noise matrix constant and identical.
     * Each element Pij of the initial covariance matrix equals:
     * Pij = scales[i]*scales[j]
     * @param scales scales of the estimated parameters
     * @return covariance matrix provider
     */
    private CovarianceMatrixProvider setInitialCovarianceMatrix(final double[] scales) {

        final int n = scales.length;
        final RealMatrix cov = MatrixUtils.createRealMatrix(n, n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                cov.setEntry(i, j, scales[i] * scales[j]);
            }
        }
        return new ConstantProcessNoise(cov);
    }


    /** Observer allowing to get Kalman model after a measurement was processed in the Kalman filter. */
    public class ModelLogger implements KalmanObserver {
        KalmanEstimation estimation;

        @Override
        public void evaluationPerformed(KalmanEstimation estimation) {
            this.estimation = estimation;
        }
    }

}
