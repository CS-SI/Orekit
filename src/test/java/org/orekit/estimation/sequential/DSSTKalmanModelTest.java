/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import java.util.Arrays;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.estimation.DSSTContext;
import org.orekit.estimation.DSSTEstimationTestUtils;
import org.orekit.estimation.DSSTForce;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Range;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class DSSTKalmanModelTest {

    /** Orbit type for propagation. */
    private final OrbitType orbitType = OrbitType.EQUINOCTIAL;
    
    /** Position angle for propagation. */
    private final PositionAngle positionAngle = PositionAngle.MEAN;
    
    /** Initial orbit. */
    private Orbit orbit0;
    
    /** Propagator builder. */
    private DSSTPropagatorBuilder propagatorBuilder;
    
    /** Covariance matrix provider. */
    private CovarianceMatrixProvider covMatrixProvider;
    
    /** Estimated measurement parameters list. */
    private ParameterDriversList estimatedMeasurementsParameters;
    
    /** Kalman extended estimator containing models. */
    private KalmanEstimator kalman;
    
    /** Kalman observer. */
    private ModelLogger modelLogger;
    
    /** State size. */
    private int M;
    
    /** PV at t0. */
    private PV pv;
    
    /** Range after t0. */
    private Range range;
    
    /** Driver for SRP coefficient. */
    private ParameterDriver srpCoefDriver;
    
    /** Tolerance for the test. */
    private final double tol = 1e-16;

    @Before
    public void setUp() {
        // Create context
        final DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");
        
        // Initial orbit and date
        this.orbit0 = context.initialOrbit;
        ObservableSatellite sat = new ObservableSatellite(0);
        
        // Create propagator builder
        this.propagatorBuilder = context.createBuilder(true, 1.0e-3, 6000.0, 10.,
                                                       DSSTForce.SOLAR_RADIATION_PRESSURE);

        // Create PV at t0
        final AbsoluteDate date0 = context.initialOrbit.getDate();
        this.pv = new PV(date0,
                             context.initialOrbit.getPVCoordinates().getPosition(),
                             context.initialOrbit.getPVCoordinates().getVelocity(),
                             new double[] {1., 2., 3., 1e-3, 2e-3, 3e-3}, 1.,
                             sat);
        
        // Create one 0m range measurement at t0 + 10s
        final AbsoluteDate date  = date0.shiftedBy(10.);
        final GroundStation station = context.stations.get(0);
        this.range = new Range(station, true, date, 18616150., 10., 1., sat);
        // Exact range value is 1.8616150246470984E7 m

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
        final KalmanEstimatorBuilder kalmanBuilder = new KalmanEstimatorBuilder();
        kalmanBuilder.addPropagationConfiguration(propagatorBuilder, covMatrixProvider);
        kalmanBuilder.estimatedMeasurementsParameters(estimatedMeasurementsParameters);
        this.kalman = kalmanBuilder.build();
        this.modelLogger = new ModelLogger();
        kalman.setObserver(modelLogger);
    }

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
        
        // Expected measurement matrix for a PV measurement is the 6-sized identity matrix for cartesian orbital parameters
        // + zeros for other estimated parameters
        RealMatrix expH = MatrixUtils.createRealMatrix(6, M);
        for (int i = 0; i < 6; i++) {
            expH.setEntry(i, i, 1.);
        }
        
        // Expected state transition matrix
        // State transition matrix is the identity matrix at t0
        RealMatrix expPhi = MatrixUtils.createRealIdentityMatrix(M);
        // Add PV measurement and check model afterwards
        checkModelAfterMeasurementAdded(1, pv, Ppred, orbitPred, expPhi, expH);

    }

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

    private void checkModelAtT0() {

        // Instantiate a Model from attributes
        final DSSTKalmanModel model = new DSSTKalmanModel(Arrays.asList(propagatorBuilder),
                                                  Arrays.asList(covMatrixProvider),
                                                  estimatedMeasurementsParameters,
                                                  PropagationType.MEAN,
                                                  PropagationType.MEAN);

        // Evaluate at t0
        // --------------
        
        // Time
        Assert.assertEquals(0., model.getEstimate().getTime(), 0.);
        Assert.assertEquals(0., model.getCurrentDate().durationFrom(orbit0.getDate()), 0.);
        
        // Measurement number
        Assert.assertEquals(0, model.getCurrentMeasurementNumber());
        
        // Normalized state - is zeros
        final RealVector stateN = model.getEstimate().getState();
        Assert.assertArrayEquals(new double[M], stateN.toArray(), tol);
        
        // Physical state - = initialized
        final RealVector x = model.getPhysicalEstimatedState();
        final RealVector expX = MatrixUtils.createRealVector(M);
        final double[] orbitState0 = new double[6];
        orbitType.mapOrbitToArray(orbit0, positionAngle, orbitState0, null);
        expX.setSubVector(0, MatrixUtils.createRealVector(orbitState0));
        expX.setEntry(6, srpCoefDriver.getReferenceValue());
        final double[] dX = x.subtract(expX).toArray();
        Assert.assertArrayEquals(new double[M], dX, tol);
        
        // Normalized covariance - filled with 1
        final double[][] Pn = model.getEstimate().getCovariance().getData();
        final double[][] expPn = new double[M][M];
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < M; j++) {
                expPn[i][j] = 1.;
            }
            Assert.assertArrayEquals("Failed on line " + i, expPn[i], Pn[i], tol);
        }
        
        // Physical covariance = initialized
        final RealMatrix P   = model.getPhysicalEstimatedCovarianceMatrix();
        final RealMatrix expP = covMatrixProvider.getInitialCovarianceMatrix(new SpacecraftState(orbit0));
        final double[][] dP = P.subtract(expP).getData();
        for (int i = 0; i < M; i++) {
            Assert.assertArrayEquals("Failed on line " + i, new double[M], dP[i], tol);
        }
        
        // Check that other "physical" matrices are null
        Assert.assertNull(model.getEstimate().getInnovationCovariance());
        Assert.assertNull(model.getPhysicalInnovationCovarianceMatrix());
        Assert.assertNull(model.getEstimate().getKalmanGain());
        Assert.assertNull(model.getPhysicalKalmanGain());
        Assert.assertNull(model.getEstimate().getMeasurementJacobian());
        Assert.assertNull(model.getPhysicalMeasurementJacobian());
        Assert.assertNull(model.getEstimate().getStateTransitionMatrix());
        Assert.assertNull(model.getPhysicalStateTransitionMatrix());
    }

    private void checkModelAfterMeasurementAdded(final int expMeasurementNumber,
                                                final ObservedMeasurement<?> meas,
                                                final RealMatrix expPpred,
                                                final Orbit expOrbitPred,
                                                final RealMatrix expPhi,
                                                final RealMatrix expH) {

        // Expected predicted measurement
        final double[] expMeasPred = 
                        meas.estimate(0, 0,
                                      new SpacecraftState[] {new SpacecraftState(expOrbitPred)}).getEstimatedValue();

        // Process PV measurement in Kalman and get model
        kalman.processMeasurements(Arrays.asList(meas));
        KalmanEstimation model = modelLogger.estimation;
        
        // Time
        Assert.assertEquals(0., model.getCurrentDate().durationFrom(expOrbitPred.getDate()), 0.);
        
        // Measurement number
        Assert.assertEquals(expMeasurementNumber, model.getCurrentMeasurementNumber());
        
        // State transition matrix
        final RealMatrix phi    = model.getPhysicalStateTransitionMatrix();
        final double[][] dPhi   = phi.subtract(expPhi).getData();
        for (int i = 0; i < M; i++) {
            Assert.assertArrayEquals("Failed on line " + i, new double[M], dPhi[i], tol*100);
        }

        // Predicted orbit
        final Orbit orbitPred = model.getPredictedSpacecraftStates()[0].getOrbit();
        final PVCoordinates pvOrbitPred = orbitPred.getPVCoordinates();
        final PVCoordinates expPVOrbitPred = expOrbitPred.getPVCoordinates();
        final double dpOrbitPred = Vector3D.distance(expPVOrbitPred.getPosition(), pvOrbitPred.getPosition());
        final double dvOrbitPred = Vector3D.distance(expPVOrbitPred.getVelocity(), pvOrbitPred.getVelocity());
        Assert.assertEquals(0., dpOrbitPred, tol);
        Assert.assertEquals(0., dvOrbitPred, tol);

        // Predicted measurement
        final double[] measPred = model.getPredictedMeasurement().getEstimatedValue();
        Assert.assertArrayEquals(expMeasPred, measPred, tol);

    }

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
