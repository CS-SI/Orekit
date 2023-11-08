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

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.DSSTContext;
import org.orekit.estimation.DSSTEstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.TwoWayRangeMeasurementCreator;
import org.orekit.estimation.measurements.modifiers.DynamicOutlierFilter;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import java.util.List;

public class SemiAnalyticalKalmanEstimatorTest {

    @Test
    public void testMissingPropagatorBuilder() {
        try {
            new SemiAnalyticalKalmanEstimatorBuilder().build();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_PROPAGATOR_CONFIGURED, oe.getSpecifier());
        }
    }

    @Test
    public void testMathRuntimeException() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");
        // Create initial orbit and DSST propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final boolean       perfectStart  = true;
        final double        minStep       = 120.0;
        final double        maxStep       = 1200.0;
        final double        dP            = 1.;

        // Propagator builder for measurement generation
        final DSSTPropagatorBuilder builder = context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart, minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit, builder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                                   new TwoWayRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);
        // DSST propagator builder (used for orbit determination)
        final DSSTPropagatorBuilder propagatorBuilder = context.createBuilder(perfectStart, minStep, maxStep, dP);

        // Equinictial covariance matrix initialization
        final RealMatrix equinoctialP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            0., 0., 0., 0., 0., 0.
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.MEAN, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Equinoctial initial covariance matrix
        final RealMatrix initialP = Jac.multiply(equinoctialP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final SemiAnalyticalKalmanEstimator kalman = new SemiAnalyticalKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        decomposer(new QRDecomposer(1.0e-15)).
                        build();
        kalman.setObserver(estimation -> {
            throw new MathRuntimeException(LocalizedCoreFormats.INTERNAL_ERROR, "me");
        });

        try {
            kalman.processMeasurements(measurements);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.INTERNAL_ERROR, oe.getSpecifier());
        }
    }

    /**
     * Perfect range measurements.
     * Only the Newtonian Attraction is used.
     * Case 1 of : "Cazabonne B., Bayard J., Journot M., and Cefola P. J., A Semi-analytical Approach for Orbit
     *              Determination based on Extended Kalman Filter, AAS Paper 21-614, AAS/AIAA Astrodynamics
     *              Specialist Conference, Big Sky, August 2021."
     */
    @Test
    public void testKeplerianRange() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and DSST propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 120.0;
        final double        maxStep       = 1200.0;
        final double        dP            = 1.;

        // Propagator builder for measurement generation
        final DSSTPropagatorBuilder builder = context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart, minStep, maxStep, dP);

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit, builder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                                   new TwoWayRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);
        final AbsoluteDate lastMeasurementEpoch = measurements.get(measurements.size() - 1).getDate();

        // DSST propagator builder (used for orbit determination)
        final DSSTPropagatorBuilder propagatorBuilder = context.createBuilder(perfectStart, minStep, maxStep, dP);

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Equinictial covariance matrix initialization
        final RealMatrix equinoctialP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            0., 0., 0., 0., 0., 0.
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.MEAN, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Equinoctial initial covariance matrix
        final RealMatrix initialP = Jac.multiply(equinoctialP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final SemiAnalyticalKalmanEstimator kalman = new SemiAnalyticalKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        final Observer observer = new Observer();
        kalman.setObserver(observer);

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 1.0e-15;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.0e-15;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assertions.assertEquals(0.0, observer.getMeanResidual(), 4.99e-8);
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertEquals(0.0, kalman.getCurrentDate().durationFrom(lastMeasurementEpoch), 1.0e-15);
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());
    }

    /**
     * Perfect range measurements.
     * J20 is added to the perturbation model compare to the previous test
     * Case 2 of : "Cazabonne B., Bayard J., Journot M., and Cefola P. J., A Semi-analytical Approach for Orbit
     *              Determination based on Extended Kalman Filter, AAS Paper 21-614, AAS/AIAA Astrodynamics
     *              Specialist Conference, Big Sky, August 2021."
     */
    @Test
    public void testRangeWithZonal() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 120.0;
        final double        maxStep       = 1200.0;
        final double        dP            = 1.;

        // Propagator builder for measurement generation
        final DSSTPropagatorBuilder builder = context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart, minStep, maxStep, dP);
        builder.addForceModel(new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0)));

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit, builder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                                   new TwoWayRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);
        final AbsoluteDate lastMeasurementEpoch = measurements.get(measurements.size() - 1).getDate();

        // DSST propagator builder (used for orbit determination)
        final DSSTPropagatorBuilder propagatorBuilder = context.createBuilder(perfectStart, minStep, maxStep, dP);
        propagatorBuilder.addForceModel(new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0)));

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        ParameterDriver aDriver = propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(0);
        aDriver.setValue(aDriver.getValue() + 1.2);

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Keplerian initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final SemiAnalyticalKalmanEstimator kalman = new SemiAnalyticalKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        final Observer observer = new Observer();
        kalman.setObserver(observer);

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 6.2e-2;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.0e-5;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assertions.assertEquals(0.0, observer.getMeanResidual(), 8.51e-3);
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertEquals(0.0, kalman.getCurrentDate().durationFrom(lastMeasurementEpoch), 1.0e-15);
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());
    }

    /**
     * Perfect range measurements.
     * J20 is added to the perturbation model
     * In addition, J21 and J22 are also added
     * Case 3 of : "Cazabonne B., Bayard J., Journot M., and Cefola P. J., A Semi-analytical Approach for Orbit
     *              Determination based on Extended Kalman Filter, AAS Paper 21-614, AAS/AIAA Astrodynamics
     *              Specialist Conference, Big Sky, August 2021."
     */
    @Test
    public void testRangeWithTesseral() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 120.0;
        final double        maxStep       = 1200.0;
        final double        dP            = 1.;

        // Propagator builder for measurement generation
        final UnnormalizedSphericalHarmonicsProvider gravityField = GravityFieldFactory.getUnnormalizedProvider(2, 2);
        final DSSTPropagatorBuilder builder = context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart, minStep, maxStep, dP);
        builder.addForceModel(new DSSTZonal(gravityField));
        builder.addForceModel(new DSSTTesseral(context.earth.getBodyFrame(), Constants.WGS84_EARTH_ANGULAR_VELOCITY, gravityField,
                gravityField.getMaxDegree(),
                gravityField.getMaxOrder(), 2,  FastMath.min(12, gravityField.getMaxDegree() + 2),
                gravityField.getMaxDegree(), gravityField.getMaxOrder(), FastMath.min(4, gravityField.getMaxDegree() - 2)));

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit, builder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                                   new TwoWayRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);
        final AbsoluteDate lastMeasurementEpoch = measurements.get(measurements.size() - 1).getDate();

        // DSST propagator builder (used for orbit determination)
        final DSSTPropagatorBuilder propagatorBuilder = context.createBuilder(perfectStart, minStep, maxStep, dP);
        propagatorBuilder.addForceModel(new DSSTZonal(gravityField));
        propagatorBuilder.addForceModel(new DSSTTesseral(context.earth.getBodyFrame(), Constants.WGS84_EARTH_ANGULAR_VELOCITY, gravityField,
                gravityField.getMaxDegree(),
                gravityField.getMaxOrder(), 2,  FastMath.min(12, gravityField.getMaxDegree() + 2),
                gravityField.getMaxDegree(), gravityField.getMaxOrder(), FastMath.min(4, gravityField.getMaxDegree() - 2)));

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        ParameterDriver aDriver = propagatorBuilder.getOrbitalParametersDrivers().getDrivers().get(0);
        aDriver.setValue(aDriver.getValue() + 1.2);

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);

        // Keplerian initial covariance matrix
        final RealMatrix initialP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Build the Kalman filter
        final SemiAnalyticalKalmanEstimator kalman = new SemiAnalyticalKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        final Observer observer = new Observer();
        kalman.setObserver(observer);

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 7.7e-2;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 2.5e-5;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assertions.assertEquals(0.0, observer.getMeanResidual(), 8.81e-3);
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertEquals(0.0, kalman.getCurrentDate().durationFrom(lastMeasurementEpoch), 1.0e-15);
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());
    }

    /** Observer for Kalman estimation. */
    public static class Observer implements KalmanObserver {

        /** Residuals statistics. */
        private StreamingStatistics stats;

        /** Constructor. */
        public Observer() {
            this.stats = new StreamingStatistics();
        }

        /** {@inheritDoc} */
        @Override
        public void evaluationPerformed(final KalmanEstimation estimation) {

            // Estimated and observed measurements
            final EstimatedMeasurement<?> estimatedMeasurement = estimation.getPredictedMeasurement();

            // Check
            if (estimatedMeasurement.getObservedMeasurement().getMeasurementType().equals(Range.MEASUREMENT_TYPE)) {
                final double[] estimated = estimatedMeasurement.getEstimatedValue();
                final double[] observed  = estimatedMeasurement.getObservedValue();
                // Calculate residual
                final double res = observed[0] - estimated[0];
                stats.addValue(res);
            }

        }

        /** Get the mean value of the residual.
         * @return the mean value of the residual in meters
         */
        public double getMeanResidual() {
            return stats.getMean();
        }

    }

    @Test
    public void testWithEstimatedPropagationParameters() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 120.0;
        final double        maxStep       = 1200.0;
        final double        dP            = 1.;

        // Propagator builder for measurement generation
        final DSSTPropagatorBuilder builder = context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart, minStep, maxStep, dP);
        final DSSTForceModel zonal = new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0));
        zonal.getParametersDrivers().get(0).setSelected(true);
        builder.addForceModel(zonal);

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit, builder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                                   new TwoWayRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);
        final AbsoluteDate lastMeasurementEpoch = measurements.get(measurements.size() - 1).getDate();

        // DSST propagator builder (used for orbit determination)
        final DSSTPropagatorBuilder propagatorBuilder = context.createBuilder(perfectStart, minStep, maxStep, dP);
        propagatorBuilder.addForceModel(zonal);

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // Covariance matrix on propagation parameters
        final RealMatrix propagationP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.0e-10
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);
        final RealMatrix orbitalP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Keplerian initial covariance matrix
        final RealMatrix initialP = MatrixUtils.createRealMatrix(7, 7);
        initialP.setSubMatrix(orbitalP.getData(), 0, 0);
        initialP.setSubMatrix(propagationP.getData(), 6, 6);

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(7, 7);

        // Build the Kalman filter
        final SemiAnalyticalKalmanEstimator kalman = new SemiAnalyticalKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        build();
        final Observer observer = new Observer();
        kalman.setObserver(observer);

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4.9e-2;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.6e-5;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assertions.assertEquals(0.0, observer.getMeanResidual(), 1.79e-3);
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(0, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertEquals(0.0, kalman.getCurrentDate().durationFrom(lastMeasurementEpoch), 1.0e-15);
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());
    }

    @Test
    public void testWithEstimatedMeasurementParameters() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final boolean       perfectStart  = true;
        final double        minStep       = 120.0;
        final double        maxStep       = 1200.0;
        final double        dP            = 1.;

        // Propagator builder for measurement generation
        final DSSTPropagatorBuilder builder = context.createBuilder(PropagationType.OSCULATING, PropagationType.MEAN, perfectStart, minStep, maxStep, dP);
        final DSSTForceModel zonal = new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0));
        builder.addForceModel(zonal);

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit, builder);
        final ParameterDriversList estimatedDrivers = new ParameterDriversList();
        final double groundClockDrift =  4.8e-9;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockDrift);
            station.getClockOffsetDriver().setSelected(true);
            estimatedDrivers.add(station.getClockOffsetDriver());
        }
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                                   new TwoWayRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);
        final AbsoluteDate lastMeasurementEpoch = measurements.get(measurements.size() - 1).getDate();

        // Create outlier filter
        final DynamicOutlierFilter<Range> filter = new DynamicOutlierFilter<>(10, 1.0);
        for (ObservedMeasurement<?> measurement : measurements) {
            Range range = (Range) measurement;
            range.addModifier(filter);
        }

        // DSST propagator builder (used for orbit determination)
        final DSSTPropagatorBuilder propagatorBuilder = context.createBuilder(perfectStart, minStep, maxStep, dP);
        propagatorBuilder.addForceModel(zonal);

        // Reference propagator for estimation performances
        final DSSTPropagator referencePropagator = propagatorBuilder.
                        buildPropagator(propagatorBuilder.getSelectedNormalizedParameters());

        // Reference position/velocity at last measurement date
        final Orbit refOrbit = referencePropagator.
                        propagate(measurements.get(measurements.size()-1).getDate()).getOrbit();

        // Cartesian covariance matrix initialization
        // 100m on position / 1e-2m/s on velocity
        final RealMatrix cartesianP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            100., 100., 100., 1e-2, 1e-2, 1e-2
        });

        // Jacobian of the orbital parameters w/r to Cartesian
        final Orbit initialOrbit = orbitType.convertType(context.initialOrbit);
        final double[][] dYdC = new double[6][6];
        initialOrbit.getJacobianWrtCartesian(PositionAngleType.TRUE, dYdC);
        final RealMatrix Jac = MatrixUtils.createRealMatrix(dYdC);
        final RealMatrix orbitalP = Jac.multiply(cartesianP.multiply(Jac.transpose()));

        // Keplerian initial covariance matrix
        final RealMatrix initialP = MatrixUtils.createRealMatrix(6, 6);
        initialP.setSubMatrix(orbitalP.getData(), 0, 0);

        // Process noise matrix is set to 0 here
        RealMatrix Q = MatrixUtils.createRealMatrix(6, 6);

        // Initial measurement covariance matrix and process noise matrix
        final RealMatrix measurementP = MatrixUtils.createRealDiagonalMatrix(new double [] {
           1.0e-15, 1.0e-15
        });
        final RealMatrix measurementQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.0e-25, 1.0e-25
        });

        // Build the Kalman filter
        final SemiAnalyticalKalmanEstimator kalman = new SemiAnalyticalKalmanEstimatorBuilder().
                        addPropagationConfiguration(propagatorBuilder, new ConstantProcessNoise(initialP, Q)).
                        estimatedMeasurementsParameters(estimatedDrivers, new ConstantProcessNoise(measurementP, measurementQ)).
                        build();
        final Observer observer = new Observer();
        kalman.setObserver(observer);

        // Filter the measurements and check the results
        final double   expectedDeltaPos  = 0.;
        final double   posEps            = 4.9e-2;
        final double   expectedDeltaVel  = 0.;
        final double   velEps            = 1.6e-5;
        DSSTEstimationTestUtils.checkKalmanFit(context, kalman, measurements,
                                           refOrbit, positionAngleType,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assertions.assertEquals(0.0, observer.getMeanResidual(), 1.79e-3);
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(false).getNbParams());
        Assertions.assertEquals(6, kalman.getOrbitalParametersDrivers(true).getNbParams());
        Assertions.assertEquals(1, kalman.getPropagationParametersDrivers(false).getNbParams());
        Assertions.assertEquals(0, kalman.getPropagationParametersDrivers(true).getNbParams());
        Assertions.assertEquals(2, kalman.getEstimatedMeasurementsParameters().getNbParams());
        Assertions.assertEquals(measurements.size(), kalman.getCurrentMeasurementNumber());
        Assertions.assertEquals(0.0, kalman.getCurrentDate().durationFrom(lastMeasurementEpoch), 1.0e-15);
        Assertions.assertNotNull(kalman.getPhysicalEstimatedState());
    }

}
