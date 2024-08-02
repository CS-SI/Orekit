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
package org.orekit.estimation.sequential;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.MerweUnscentedTransform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.DSSTContext;
import org.orekit.estimation.DSSTEstimationTestUtils;
import org.orekit.estimation.DSSTForce;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SemiAnalyticalUnscentedKalmanModelTest {


    /** Orbit type for propagation. */
    private final OrbitType orbitType = OrbitType.EQUINOCTIAL;

    /** Position angle for propagation. */
    private final PositionAngleType positionAngleType = PositionAngleType.MEAN;

    /** Initial orbit. */
    private Orbit orbit0;

    /** Propagator builder. */
    private DSSTPropagatorBuilder propagatorBuilder;

    /** Covariance matrix provider. */
    private CovarianceMatrixProvider covMatrixProvider;

    /** Estimated measurement parameters list. */
    private ParameterDriversList estimatedMeasurementsParameters;

    /** Kalman estimator containing models. */
    private SemiAnalyticalUnscentedKalmanEstimator kalman;

    /** Kalman observer. */
    private ModelLogger modelLogger;

    /** State size. */
    private int M;

    /** Range after t0. */
    private Range range;

    /** Driver for satellite range bias. */
    private ParameterDriver satRangeBiasDriver;

    /** Driver for SRP coefficient. */
    private ParameterDriver srpCoefDriver;

    /** Tolerance for the test. */
    private final double tol = 5.0e-9;

    @BeforeEach
    void setup() {
        // Create context
        final DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Initial orbit and date
        this.orbit0 = context.initialOrbit;
        ObservableSatellite sat = new ObservableSatellite(0);

        // Create propagator builder
        this.propagatorBuilder = context.createBuilder(PropagationType.MEAN, PropagationType.OSCULATING, true,
                                                       1.0e-6, 60.0, 10., DSSTForce.SOLAR_RADIATION_PRESSURE);

        //  t0
        final AbsoluteDate date0 = context.initialOrbit.getDate();

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

        // Initialize Kalman
        final SemiAnalyticalUnscentedKalmanEstimatorBuilder kalmanBuilder = new SemiAnalyticalUnscentedKalmanEstimatorBuilder();
        kalmanBuilder.unscentedTransformProvider(new MerweUnscentedTransform(8));
        kalmanBuilder.addPropagationConfiguration(propagatorBuilder, covMatrixProvider);
        kalmanBuilder.estimatedMeasurementsParameters(estimatedMeasurementsParameters, null);
        this.kalman = kalmanBuilder.build();
        this.modelLogger = new ModelLogger();
        kalman.setObserver(modelLogger);
    }

    @Test
    void ModelPhysicalOutputsTest() {

        // Check model at t0 before any measurement is added
        // -------------------------------------------------
        checkModelAtT0();

    }

    /** Check the model physical outputs at t0 before any measurement is added. */
    private void checkModelAtT0() {

        // Instantiate a Model from attributes
        final SemiAnalyticalUnscentedKalmanModel model = new SemiAnalyticalUnscentedKalmanModel(propagatorBuilder,
                                                                                                covMatrixProvider,
                                                                                                estimatedMeasurementsParameters,
                                                                                                null);
        model.setObserver(modelLogger);

        // Evaluate at t0
        // --------------

        // Observer
        assertNotNull(model.getObserver());

        // Time
        assertEquals(0., model.getEstimate().getTime(), 0.);
        assertEquals(0., model.getCurrentDate().durationFrom(orbit0.getDate()), 0.);

        // Measurement number
        assertEquals(0, model.getCurrentMeasurementNumber());

        // Physical state and predicted filter correction
        final RealVector expX = MatrixUtils.createRealVector(M);
        final double[] orbitState0 = new double[6];
        orbitType.mapOrbitToArray(orbit0, positionAngleType, orbitState0, null);
        expX.setSubVector(0, MatrixUtils.createRealVector(orbitState0));
        expX.setEntry(6, srpCoefDriver.getReferenceValue());
        expX.setEntry(7, satRangeBiasDriver.getReferenceValue());
        assertArrayEquals(model.getPhysicalEstimatedState().toArray(), expX.toArray(), tol);
        assertArrayEquals(new double[8], model.getEstimate().getState().toArray(), tol);

        // Normalized covariance - filled with 1
        final double[][] Pn = model.getEstimate().getCovariance().getData();
        final double[][] expPn = covMatrixProvider.getInitialCovarianceMatrix(null).getData();
        for (int i = 0; i < M; i++) {
            assertArrayEquals(expPn[i], Pn[i], tol, "Failed on line " + i);
        }

        // Physical covariance = initialized
        final RealMatrix P   = model.getPhysicalEstimatedCovarianceMatrix();
        final RealMatrix expP = covMatrixProvider.getInitialCovarianceMatrix(new SpacecraftState(orbit0));
        final double[][] dP = P.subtract(expP).getData();
        for (int i = 0; i < M; i++) {
            assertArrayEquals(new double[M], dP[i], tol, "Failed on line " + i);
        }

        // State transition matrix (null for unscented kalman filter)
        assertNull(model.getPhysicalStateTransitionMatrix());

        // Measurement matrix (null for unscented kalman filter)
        assertNull(model.getPhysicalMeasurementJacobian());

        // Check that other "physical" matrices are null
        assertNull(model.getEstimate().getInnovationCovariance());
        assertNull(model.getPhysicalInnovationCovarianceMatrix());
        assertNull(model.getEstimate().getKalmanGain());
        assertNull(model.getPhysicalKalmanGain());
        assertNull(model.getEstimate().getMeasurementJacobian());
        assertNull(model.getPhysicalMeasurementJacobian());
        assertNull(model.getEstimate().getStateTransitionMatrix());
        assertNull(model.getPhysicalStateTransitionMatrix());
    }

    /** Get an array of the scales of the estimated parameters.
     * @param builder propagator builder
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @return array containing the scales of the estimated parameter
     */
    private double[] getParametersScale(final DSSTPropagatorBuilder builder,
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
