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
package org.orekit.estimation.sequential;

import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.DSSTContext;
import org.orekit.estimation.DSSTEstimationTestUtils;
import org.orekit.estimation.measurements.DSSTRangeMeasurementCreator;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

public class SemiAnalyticalKalmanEstimatorTest {

	@Test
    public void testMissingPropagatorBuilder() {
        try {
            new SemiAnalyticalKalmanEstimatorBuilder().build();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_PROPAGATOR_CONFIGURED, oe.getSpecifier());
        }
    }

    /**
     * Perfect range measurements.
     * Only the Newtonian Attraction is used.
     */
    @Test
    public void testKepleranRange() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and DSST propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngle positionAngle = PositionAngle.MEAN;
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
                                                                   new DSSTRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);

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
        initialOrbit.getJacobianWrtCartesian(PositionAngle.MEAN, dYdC);
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
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assert.assertEquals(0.0, observer.getMeanResidual(), 4.98e-8);
    }

    /**
     * Perfect range measurements.
     * J20 is added to the perturbation model compare to the previous test
     */
    @Test
    public void testRangeWithZonal() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngle positionAngle = PositionAngle.MEAN;
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
                                                                   new DSSTRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);

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
        initialOrbit.getJacobianWrtCartesian(PositionAngle.TRUE, dYdC);
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
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assert.assertEquals(0.0, observer.getMeanResidual(), 8.51-3);
    }

    /**
     * Perfect range measurements.
     * J20 is added to the perturbation model
     * In addition, J21 and J22 are also added
     */
    @Test
    public void testRangeWithTesseral() {

        // Create context
        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngle positionAngle = PositionAngle.MEAN;
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
                                                                   new DSSTRangeMeasurementCreator(context),
                                                                   0.0, 6.0, 60.0);

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
        initialOrbit.getJacobianWrtCartesian(PositionAngle.TRUE, dYdC);
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
                                           refOrbit, positionAngle,
                                           expectedDeltaPos, posEps,
                                           expectedDeltaVel, velEps);

        Assert.assertEquals(0.0, observer.getMeanResidual(), 8.81e-3);
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
            if (estimatedMeasurement.getObservedMeasurement() instanceof Range) {
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

}
