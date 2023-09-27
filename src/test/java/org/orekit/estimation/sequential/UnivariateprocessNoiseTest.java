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

import java.util.Arrays;
import java.util.Locale;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.Force;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.PVCoordinates;

/** Tests for the class UnivariateprocessNoise. */
public class UnivariateprocessNoiseTest {

    /** Basic test for getters. */
    @Test
    public void testUnivariateProcessNoiseGetters() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Define the univariate functions for the standard deviations
        final UnivariateFunction[] lofCartesianOrbitalParametersEvolution = new UnivariateFunction[6];
        // Evolution for position error
        lofCartesianOrbitalParametersEvolution[0] = new PolynomialFunction(new double[] {100., 0., 1e-4});
        lofCartesianOrbitalParametersEvolution[1] = new PolynomialFunction(new double[] {100., 1e-1, 0.});
        lofCartesianOrbitalParametersEvolution[2] = new PolynomialFunction(new double[] {100., 0., 0.});
        // Evolution for velocity error
        lofCartesianOrbitalParametersEvolution[3] = new PolynomialFunction(new double[] {1., 0., 1.e-6});
        lofCartesianOrbitalParametersEvolution[4] = new PolynomialFunction(new double[] {1., 1e-3, 0.});
        lofCartesianOrbitalParametersEvolution[5] = new PolynomialFunction(new double[] {1., 0., 0.});

        // Evolution for propagation parameters error
        final UnivariateFunction[] propagationParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {10., 1., 1e-4}),
                            new PolynomialFunction(new double[] {1000., 0., 0.})};

        // Evolution for measurements parameters error
        final UnivariateFunction[] measurementsParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {100., 1., 1e-3})};

        // Create a dummy initial covariance matrix
        final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(9);

        // Set the process noise object
        // Define input LOF and output position angle
        final LOFType lofType = LOFType.TNW;
        final UnivariateProcessNoise processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                                               lofType,
                                                                               PositionAngleType.TRUE,
                                                                               lofCartesianOrbitalParametersEvolution,
                                                                               propagationParametersEvolution,
                                                                               measurementsParametersEvolution);

        Assertions.assertEquals(LOFType.TNW, processNoise.getLofType());
        Assertions.assertEquals(PositionAngleType.TRUE, processNoise.getPositionAngleType());
        Assertions.assertEquals(initialCovarianceMatrix,
                                processNoise.getInitialCovarianceMatrix(new SpacecraftState(context.initialOrbit)));
        Assertions.assertArrayEquals(lofCartesianOrbitalParametersEvolution, processNoise.getLofCartesianOrbitalParametersEvolution());
        Assertions.assertArrayEquals(propagationParametersEvolution, processNoise.getPropagationParametersEvolution());
        Assertions.assertArrayEquals(measurementsParametersEvolution, processNoise.getMeasurementsParametersEvolution());

        final UnivariateProcessNoise processNoiseOld = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                                                  lofType,
                                                                                  PositionAngleType.TRUE,
                                                                                  lofCartesianOrbitalParametersEvolution,
                                                                                  propagationParametersEvolution);

        Assertions.assertEquals(LOFType.TNW, processNoiseOld.getLofType());
        Assertions.assertEquals(PositionAngleType.TRUE, processNoiseOld.getPositionAngleType());
        Assertions.assertEquals(initialCovarianceMatrix,
                                processNoiseOld.getInitialCovarianceMatrix(new SpacecraftState(context.initialOrbit)));
        Assertions.assertArrayEquals(lofCartesianOrbitalParametersEvolution, processNoiseOld.getLofCartesianOrbitalParametersEvolution());
        Assertions.assertArrayEquals(propagationParametersEvolution, processNoiseOld.getPropagationParametersEvolution());
    }

    /** Test UnivariateProcessNoise class with Cartesian parameters. */
    @Test
    public void testProcessNoiseCartesian() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Print result on console ?
        final boolean print = false;

        // Initial orbit type and position angle
        final OrbitType     orbitType     = OrbitType.CARTESIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE; // Not used here

        // LOF type
        final LOFType lofType  = LOFType.TNW;

        // Add a measurement parameter ?
        final boolean hasMeasurementParameter = true;

        // Sample number and relative tolerance
        final int sampleNumber = 10000;
        final double relativeTolerance = 1.05e-2;

        // Do the test
        doTestProcessNoise(context, orbitType, positionAngleType, lofType, hasMeasurementParameter,
                           print, sampleNumber, relativeTolerance);
    }

    /** Test UnivariateProcessNoise class with Keplerian parameters. */
    @Test
    public void testProcessNoiseKeplerian() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Print result on console ?
        final boolean print = false;

        // Initial orbit type and position angle
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;

        // LOF type
        final LOFType lofType  = LOFType.QSW;

        // Add a measurement parameter ?
        final boolean hasMeasurementParameter = false;

        // Sample number and relative tolerance
        final int sampleNumber = 10000;
        final double relativeTolerance = 1.16e-2;

        // Do the test
        doTestProcessNoise(context, orbitType, positionAngleType, lofType, hasMeasurementParameter,
                           print, sampleNumber, relativeTolerance);
    }

    /** Test UnivariateProcessNoise class with Circular parameters. */
    @Test
    public void testProcessNoiseCircular() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Print result on console ?
        final boolean print = false;

        // Initial orbit type and position angle
        final OrbitType     orbitType     = OrbitType.CIRCULAR;
        final PositionAngleType positionAngleType = PositionAngleType.ECCENTRIC;

        // LOF type
        final LOFType lofType  = LOFType.LVLH;

        // Add a measurement parameter ?
        final boolean hasMeasurementParameter = true;

        // Sample number and relative tolerance
        final int sampleNumber = 10000;
        final double relativeTolerance = 1.65e-2;

        // Do the test
        doTestProcessNoise(context, orbitType, positionAngleType, lofType, hasMeasurementParameter,
                           print, sampleNumber, relativeTolerance);
    }

    /** Test UnivariateProcessNoise class with Equinoctial parameters. */
    @Test
    public void testProcessNoiseEquinoctial() {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Print result on console ?
        final boolean print = false;

        // Initial orbit type and position angle
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;

        // LOF type
        final LOFType lofType  = LOFType.VVLH;

        // Add a measurement parameter ?
        final boolean hasMeasurementParameter = false;

        // Sample number and relative tolerance
        final int sampleNumber = 10000;
        final double relativeTolerance = 0.80e-2;

        // Do the test
        doTestProcessNoise(context, orbitType, positionAngleType, lofType, hasMeasurementParameter,
                           print, sampleNumber, relativeTolerance);
    }

    /** Test UnivariateProcessNoise class.
     * - Initialize with different univariate functions for orbital/propagation parameters variation
     * - Check that the inertial process noise covariance matrix is consistent with the inputs
     * 
     * @param context context
     * @param orbitType orbit type
     * @param positionAngleType position angle
     * @param lofType LOF type
     * @param hasMeasurementParameter add also a measurement parameter (this tests the 2nd constructor)
     * @param print print outputs ?
     * @param sampleNumber sample numbers for the statistics
     * @param relativeTolerance relative tolerance for errors (< 1.5% for 10000 samples)
     */
    private void doTestProcessNoise(final Context context,
                                    final OrbitType orbitType,
                                    final PositionAngleType positionAngleType,
                                    final LOFType lofType,
                                    final boolean hasMeasurementParameter,
                                    final boolean print,
                                    final int sampleNumber,
                                    final double relativeTolerance) {

        // Create propagator builder
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder = context.createBuilder(orbitType, positionAngleType, perfectStart,
                                                                                   minStep, maxStep, dP,
                                                                                   Force.POTENTIAL, Force.THIRD_BODY_MOON,
                                                                                   Force.THIRD_BODY_SUN,
                                                                                   Force.SOLAR_RADIATION_PRESSURE);
        // Build propagator
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        // Define the univariate functions for the standard deviations
        final UnivariateFunction[] lofCartesianOrbitalParametersEvolution = new UnivariateFunction[6];

        // Evolution for position error
        lofCartesianOrbitalParametersEvolution[0] = new PolynomialFunction(new double[] {500., 0., 1e-4});
        lofCartesianOrbitalParametersEvolution[1] = new PolynomialFunction(new double[] {400., 1e-1, 0.});
        lofCartesianOrbitalParametersEvolution[2] = new PolynomialFunction(new double[] {200., 2e-1, 0.});

        // Evolution for velocity error
        lofCartesianOrbitalParametersEvolution[3] = new PolynomialFunction(new double[] {1., 0., 1e-6});
        lofCartesianOrbitalParametersEvolution[4] = new PolynomialFunction(new double[] {1., 1e-3, 0.});
        lofCartesianOrbitalParametersEvolution[5] = new PolynomialFunction(new double[] {1., 0., 1e-5});

        // Evolution for propagation parameters error
        final UnivariateFunction[] propagationParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {100., 1., 1e-4}),
                            new PolynomialFunction(new double[] {2000., 3., 0.})};


        // Build process noise
        // -------------------
        final UnivariateProcessNoise processNoise;

        if (hasMeasurementParameter) {
            // Evolution for measurements parameters error
            final UnivariateFunction[] measurementsParametersEvolution =
                            new UnivariateFunction[] {new PolynomialFunction(new double[] {100., 1., 1e-3})};

            // Create a dummy initial covariance matrix
            final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(9);

            // Set the process noise object
            // Define input LOF and output position angle
            processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                      lofType,
                    positionAngleType,
                                                      lofCartesianOrbitalParametersEvolution,
                                                      propagationParametersEvolution,
                                                      measurementsParametersEvolution);
        } else {
            // No measurement parameters
            // Create a dummy initial covariance matrix
            final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(8);

            // Set the process noise object
            // Define input LOF and output position angle
            processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                      lofType,
                    positionAngleType,
                                                      lofCartesianOrbitalParametersEvolution,
                                                      propagationParametersEvolution);

        }

        // Test on initial value, after 1 hour and after 2 orbits
        final SpacecraftState state0 = propagator.getInitialState();
        final SpacecraftState state1 = propagator.propagate(context.initialOrbit.getDate().shiftedBy(3600.));
        final SpacecraftState state2 = propagator.propagate(context.initialOrbit.getDate()
                                                            .shiftedBy(2*context.initialOrbit.getKeplerianPeriod()));

        if (print) {
            System.out.println("Orbit Type    : " + orbitType);
            System.out.println("Position Angle: " + positionAngleType);
            System.out.println("LOF Type      : " + lofType + "\n");
        }

        // Check the covariance value
        checkCovarianceValue(print, state0, state0, processNoise, 2, sampleNumber, relativeTolerance);
        checkCovarianceValue(print, state0, state1, processNoise, 2, sampleNumber, relativeTolerance);
        checkCovarianceValue(print, state0, state2, processNoise, 2, sampleNumber, relativeTolerance);
    }

    /** Check the values of the covariance given in inertial frame by the UnivariateProcessNoise object.
     *  - Get the process noise covariance matrix P in inertial frame
     *  - Use a random vector generator based on P to produce a set of N error vectors in inertial frame
     *  - Compare the standard deviations of the noisy data to the univariate functions given in input of the UnivariateProcessNoise class.
     *  - For orbit parameters this involve converting the inertial orbital vector to LOF frame and Cartesian formalism first
     * @param print print results in console ?
     * @param previous previous state
     * @param current current state
     * @param univariateProcessNoise UnivariateProcessNoise object to test
     * @param propagationParametersSize size of propagation parameters submatrix in UnivariateProcessNoise
     * @param sampleNumber number of sample vectors for the statistics
     * @param relativeTolerance relative tolerance on the standard deviations for the tests
     */
    private void checkCovarianceValue(final boolean print,
                                      final SpacecraftState previous,
                                      final SpacecraftState current,
                                      final UnivariateProcessNoise univariateProcessNoise,
                                      final int propagationParametersSize,
                                      final int sampleNumber,
                                      final double relativeTolerance) {

        // Get the process noise matrix in "current orbit" orbital parameters
        final RealMatrix processNoiseMatrixParam = univariateProcessNoise.getProcessNoiseMatrix(previous, current);

        // Convert to Cartesian (this is done because, afterwards when summing random vector and
        // orbit, using directly Keplerian parameters wouldn't work)
        // -------

        // Extract orbital part
        final RealMatrix processOrbitalParam = processNoiseMatrixParam.getSubMatrix(0, 5, 0, 5);

        // Jacobian of Cartesian parameters with respect to orbital parameters
        final double[][] dCdY = new double[6][6];
        current.getOrbit().getJacobianWrtParameters(univariateProcessNoise.getPositionAngleType(), dCdY);
        final RealMatrix jacdCdY = MatrixUtils.createRealMatrix(dCdY);

        // Transform orbital part to Cartesian
        final RealMatrix processOrbitalCart = jacdCdY.
                        multiply(processOrbitalParam).
                        multiplyTransposed(jacdCdY);

        // Create new full process noise matrix with Cartesian orbital part
        final RealMatrix processNoiseMatrix = processNoiseMatrixParam.copy();
        processNoiseMatrix.setSubMatrix(processOrbitalCart.getData(), 0, 0);

        // Initialize a random vector generator
        final CorrelatedRandomVectorGenerator randomVectorGenerator = createSampler(processNoiseMatrix);

        // Measurements parameters length
        int measurementsParametersSize = processNoiseMatrix.getColumnDimension() - (6 + propagationParametersSize);
        if ( measurementsParametersSize < 0) {
            measurementsParametersSize = 0;
        }

        // Prepare the statistics
        final StreamingStatistics[] orbitalStatistics = new StreamingStatistics[6];
        for (int i = 0; i < 6; i++) {
            orbitalStatistics[i] = new StreamingStatistics();
        }
        StreamingStatistics[] propagationStatistics;
        if (propagationParametersSize > 0) {
            propagationStatistics = new StreamingStatistics[propagationParametersSize];
            for (int i = 0; i < propagationParametersSize; i++) {
                propagationStatistics[i] = new StreamingStatistics();
            }
        } else {
            propagationStatistics = null;
        }
        StreamingStatistics[] measurementsStatistics;
        if (propagationParametersSize > 0) {
            measurementsStatistics = new StreamingStatistics[measurementsParametersSize];
            for (int i = 0; i < measurementsParametersSize; i++) {
                measurementsStatistics[i] = new StreamingStatistics();
            }
        } else {
            measurementsStatistics = null;
        }

        // Current PV in inertial frame at "current" date
        final PVCoordinates currentPv = current.getOrbit().getPVCoordinates();

        // Transform from inertial to current spacecraft LOF frame
        final Transform inertialToLof = univariateProcessNoise.getLofType().transformFromInertial(current.getDate(),
                                                                                                  current.getOrbit().getPVCoordinates());
        // Create the vectors and compute the states
        for (int i = 0; i < sampleNumber; i++) {

            // Create a random vector
            final double[] randomVector = randomVectorGenerator.nextVector();

            // Random delta PV in inertial frame at "current" date
            final PVCoordinates deltaPv = new PVCoordinates(new Vector3D(randomVector[0],
                                                                         randomVector[1],
                                                                         randomVector[2]),
                                                            new Vector3D(randomVector[3],
                                                                         randomVector[4],
                                                                         randomVector[5]));
            // Modified PV in inertial frame at "current" date
            PVCoordinates modifiedPV = new PVCoordinates(currentPv.getPosition().add(deltaPv.getPosition()),
                                                         currentPv.getVelocity().add(deltaPv.getVelocity()),
                                                         currentPv.getAcceleration().add(deltaPv.getAcceleration()));

            // Transform from inertial to current LOF
            // The value obtained is the Cartesian error vector in LOF (w/r to current spacecraft position)
            final PVCoordinates lofPV = inertialToLof.transformPVCoordinates(modifiedPV);

            // Store the LOF PV values in the statistics summaries
            orbitalStatistics[0].addValue(lofPV.getPosition().getX());
            orbitalStatistics[1].addValue(lofPV.getPosition().getY());
            orbitalStatistics[2].addValue(lofPV.getPosition().getZ());
            orbitalStatistics[3].addValue(lofPV.getVelocity().getX());
            orbitalStatistics[4].addValue(lofPV.getVelocity().getY());
            orbitalStatistics[5].addValue(lofPV.getVelocity().getZ());


            // Propagation parameters values
            // -----------------------------

            // Extract the propagation parameters random vector and store them in the statistics
            if (propagationParametersSize > 0) {
                for (int j = 6; j < randomVector.length - measurementsParametersSize; j++) {
                    propagationStatistics[j - 6].addValue(randomVector[j]);
                }
            }

            // Measurements parameters values
            // -----------------------------

            // Extract the measurements parameters random vector and store them in the statistics
            if (measurementsParametersSize > 0) {
                for (int j = 6 + propagationParametersSize ; j < randomVector.length; j++) {
                    measurementsStatistics[j - (6 + propagationParametersSize)].addValue(randomVector[j]);
                }
            }
        }

        // Get the real values and the statistics
        // --------------------------------------

        // DT
        final double dt = current.getDate().durationFrom(previous.getDate());

        // Max relative sigma
        double maxRelativeSigma = 0;

        // Get the values of the orbital functions and the statistics
        final double[] orbitalValues = new double[6];
        final double[] orbitalStatisticsValues = new double[6];
        final double[] orbitalRelativeValues = new double[6];

        for (int i = 0; i < 6; i++) {
            orbitalValues[i] = FastMath.abs(univariateProcessNoise.getLofCartesianOrbitalParametersEvolution()[i].value(dt));
            orbitalStatisticsValues[i] = orbitalStatistics[i].getStandardDeviation();

            if (FastMath.abs(orbitalValues[i]) > 1e-15) {
                orbitalRelativeValues[i] = FastMath.abs(1. - orbitalStatisticsValues[i]/orbitalValues[i]);
            } else {
                orbitalRelativeValues[i] = orbitalStatisticsValues[i];
            }
            maxRelativeSigma = FastMath.max(maxRelativeSigma, orbitalRelativeValues[i]);
        }

        // Get the values of the propagation functions and statistics
        final double[] propagationValues = new double[propagationParametersSize];
        final double[] propagationStatisticsValues = new double[propagationParametersSize];
        final double[] propagationRelativeValues = new double[propagationParametersSize];
        for (int i = 0; i < propagationParametersSize; i++) {
            propagationValues[i] = FastMath.abs(univariateProcessNoise.getPropagationParametersEvolution()[i].value(dt));
            propagationStatisticsValues[i] = propagationStatistics[i].getStandardDeviation();

            if (FastMath.abs(propagationValues[i]) > 1e-15) {
                propagationRelativeValues[i] = FastMath.abs(1. - propagationStatisticsValues[i]/propagationValues[i]);
            } else {
                propagationRelativeValues[i] = propagationStatisticsValues[i];
            }
            maxRelativeSigma = FastMath.max(maxRelativeSigma, propagationRelativeValues[i]);
        }

        // Get the values of the measurements functions and statistics
        final double[] measurementsValues = new double[measurementsParametersSize];
        final double[] measurementsStatisticsValues = new double[measurementsParametersSize];
        final double[] measurementsRelativeValues = new double[measurementsParametersSize];
        for (int i = 0; i < measurementsParametersSize; i++) {
            measurementsValues[i] = FastMath.abs(univariateProcessNoise.getMeasurementsParametersEvolution()[i].value(dt));
            measurementsStatisticsValues[i] = measurementsStatistics[i].getStandardDeviation();

            if (FastMath.abs(propagationValues[i]) > 1e-15) {
                measurementsRelativeValues[i] = FastMath.abs(1. - measurementsStatisticsValues[i]/measurementsValues[i]);
            } else {
                measurementsRelativeValues[i] = measurementsStatisticsValues[i];
            }
            maxRelativeSigma = FastMath.max(maxRelativeSigma, measurementsRelativeValues[i]);
        }

        // Print values
        if (print) {
            System.out.println("\tdt      = " + dt + " / N = " + sampleNumber);
            System.out.println("\tMax σ % = " + (maxRelativeSigma * 100) + "\n");

            System.out.println("\tσ orbit ref   = " + Arrays.toString(orbitalValues));
            System.out.println("\tσ orbit stat  = " + Arrays.toString(orbitalStatisticsValues));
            System.out.println("\tσ orbit %     = " + Arrays.toString(Arrays.stream(orbitalRelativeValues).map(i -> i*100.).toArray()) + "\n");

            System.out.println("\tσ propag ref   = " + Arrays.toString(propagationValues));
            System.out.println("\tσ propag stat  = " + Arrays.toString(propagationStatisticsValues));
            System.out.println("\tσ propag %     = " + Arrays.toString(Arrays.stream(propagationRelativeValues).map(i -> i*100.).toArray()) + "\n");

            System.out.println("\tσ meas ref   = " + Arrays.toString(propagationValues));
            System.out.println("\tσ meas stat  = " + Arrays.toString(propagationStatisticsValues));
            System.out.println("\tσ meas %     = " + Arrays.toString(Arrays.stream(propagationRelativeValues).map(i -> i*100.).toArray()) + "\n");
        }

        // Test the values
        Assertions.assertArrayEquals(new double[6],
                                     orbitalRelativeValues,
                                     relativeTolerance);
        Assertions.assertArrayEquals(new double[propagationParametersSize],
                                     propagationRelativeValues,
                                     relativeTolerance);
        Assertions.assertArrayEquals(new double[measurementsParametersSize],
                                     measurementsRelativeValues,
                                     relativeTolerance);
    }

    /** Create a Gaussian random vector generator based on an input covariance matrix.
     * @param covarianceMatrix input covariance matrix
     * @return correlated gaussian random vectors generator
     */
    private CorrelatedRandomVectorGenerator createSampler(final RealMatrix covarianceMatrix) {
        double small = 10e-20 * covarianceMatrix.getNorm1();
        return new CorrelatedRandomVectorGenerator(
                                                   covarianceMatrix,
                                                   small,
                                                   new GaussianRandomGenerator(new Well1024a(0x366a26b94e520f41l)));
    }
    
    /** Test LOF orbital covariance matrix value from Cartesian formalism definition. */
    @Test
    public void testLofOrbitalCovarianceFromCartesian() {
        
        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Print result on console ?
        final boolean print = false;

        // Initial orbit type and position angle
        final OrbitType     orbitType     = OrbitType.CARTESIAN;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE; // Not used here

        // LOF type
        final LOFType lofType  = LOFType.VNC_INERTIAL;

        // Relative tolerance
        final double relativeTolerance = 2.93e-11;

        // Do the test
        doTestLofCartesianOrbitalCovarianceFormal(context, orbitType, positionAngleType, lofType,
                           print, relativeTolerance);
    }
    
    /** Test LOF orbital covariance matrix value from Keplerian formalism definition. */
    @Test
    public void testLofOrbitalCovarianceFromKeplerian() {
        
        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Print result on console ?
        final boolean print = false;

        // Initial orbit type and position angle
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;

        // LOF type
        final LOFType lofType  = LOFType.LVLH_CCSDS;

        // Relative tolerance
        final double relativeTolerance = 1.13e-6;

        // Do the test
        doTestLofCartesianOrbitalCovarianceFormal(context, orbitType, positionAngleType, lofType,
                           print, relativeTolerance);
    }
    
    /** Test LOF orbital covariance matrix value from Circular formalism definition. */
    @Test
    public void testLofOrbitalCovarianceFromCircular() {
        
        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Print result on console ?
        final boolean print = false;

        // Initial orbit type and position angle
        final OrbitType     orbitType     = OrbitType.CIRCULAR;
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;

        // LOF type
        final LOFType lofType  = LOFType.EQW;

        // Relative tolerance
        final double relativeTolerance = 6.61e-9;

        // Do the test
        doTestLofCartesianOrbitalCovarianceFormal(context, orbitType, positionAngleType, lofType,
                           print, relativeTolerance);
    }
    
    /** Test LOF orbital covariance matrix value from Equinoctial formalism definition. */
    @Test
    public void testLofOrbitalCovarianceFromEquinoctial() {
        
        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Print result on console ?
        final boolean print = false;

        // Initial orbit type and position angle
        final OrbitType     orbitType     = OrbitType.EQUINOCTIAL;
        final PositionAngleType positionAngleType = PositionAngleType.ECCENTRIC;

        // LOF type
        final LOFType lofType  = LOFType.NTW;

        // Relative tolerance
        final double relativeTolerance = 1.43e-10;

        // Do the test
        doTestLofCartesianOrbitalCovarianceFormal(context, orbitType, positionAngleType, lofType,
                           print, relativeTolerance);
    }

    /** Test LOF orbital covariance matrix value against reference.
     * 1. Initialize with different univariate functions for orbital/propagation parameters variation
     * 2. Get the inertial process noise covariance matrix
     * 3. Convert the inertial covariance from 2 in Cartesian and LOF
     * 4. Compare values of 3 with reference values from 1
     *
     * @param context context
     * @param orbitType orbit type
     * @param positionAngleType position angle
     * @param lofType LOF type
     * @param print print results on console ?
     * @param relativeTolerance relative tolerance
     */
    private void doTestLofCartesianOrbitalCovarianceFormal(final Context context,
                                                        final OrbitType orbitType,
                                                        final PositionAngleType positionAngleType,
                                                        final LOFType lofType,
                                                        final boolean print,
                                                        final double relativeTolerance) {

        // Create initial orbit and propagator builder
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder = context.createBuilder(orbitType, positionAngleType, perfectStart,
                                                                                   minStep, maxStep, dP,
                                                                                   Force.POTENTIAL, Force.THIRD_BODY_MOON,
                                                                                   Force.THIRD_BODY_SUN,
                                                                                   Force.SOLAR_RADIATION_PRESSURE);

        // Create a propagator
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        // Define the univariate functions for the standard deviations
        final UnivariateFunction[] lofCartesianOrbitalParametersEvolution = new UnivariateFunction[6];
        // Evolution for position error
        lofCartesianOrbitalParametersEvolution[0] = new PolynomialFunction(new double[] {100., 0., 1e-4});
        lofCartesianOrbitalParametersEvolution[1] = new PolynomialFunction(new double[] {100., 1e-1, 0.});
        lofCartesianOrbitalParametersEvolution[2] = new PolynomialFunction(new double[] {100., 0., 0.});
        // Evolution for velocity error
        lofCartesianOrbitalParametersEvolution[3] = new PolynomialFunction(new double[] {1., 0., 1.e-6});
        lofCartesianOrbitalParametersEvolution[4] = new PolynomialFunction(new double[] {1., 1e-3, 0.});
        lofCartesianOrbitalParametersEvolution[5] = new PolynomialFunction(new double[] {1., 0., 0.});

        // Evolution for propagation parameters error
        final UnivariateFunction[] propagationParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {10., 1., 1e-4}),
                            new PolynomialFunction(new double[] {1000., 0., 0.})};


        // Create a dummy initial covariance matrix
        final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(8);

        // Set the process noise object
        // Define input LOF and output position angle
        final UnivariateProcessNoise processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                                               lofType,
                positionAngleType,
                                                                               lofCartesianOrbitalParametersEvolution,
                                                                               propagationParametersEvolution);
        // Initial value
        final SpacecraftState state0 = propagator.getInitialState();

        // 1 orbit
        final SpacecraftState state1 = propagator.propagate(context.initialOrbit.getDate()
                                                            .shiftedBy(context.initialOrbit.getKeplerianPeriod()));
        // 2 orbits
        final SpacecraftState state2 = propagator.propagate(context.initialOrbit.getDate().
                                                            shiftedBy(2. * context.initialOrbit.getKeplerianPeriod()));

        if (print) {
            System.out.println("Orbit Type    : " + orbitType);
            System.out.println("Position Angle: " + positionAngleType);
            System.out.println("LOF Type      : " + lofType.toString() + "\n");
        }

        // Checks
        checkLofOrbitalCovarianceMatrix(print, state0, state0, processNoise, relativeTolerance);
        checkLofOrbitalCovarianceMatrix(print, state0, state1, processNoise, relativeTolerance);
        checkLofOrbitalCovarianceMatrix(print, state0, state2, processNoise, relativeTolerance);
    }

    /** Check orbital covariance matrix in LOF wrt reference.
     * Here we do the reverse calculation
     */
    private void checkLofOrbitalCovarianceMatrix(final boolean print,
                                                 final SpacecraftState previous,
                                                 final SpacecraftState current,
                                                 final UnivariateProcessNoise univariateProcessNoise,
                                                 final double relativeTolerance) {

        // Get the process noise matrix in inertial frame
        final RealMatrix inertialQ = univariateProcessNoise.getProcessNoiseMatrix(previous, current);

        // Extract the orbit part
        final RealMatrix inertialOrbitalQ = inertialQ.getSubMatrix(0, 5, 0, 5);

        // Jacobian from parameters to Cartesian
        final double[][] dCdY = new double[6][6];
        current.getOrbit().getJacobianWrtParameters(univariateProcessNoise.getPositionAngleType(), dCdY);
        final RealMatrix jacOrbToCar = new Array2DRowRealMatrix(dCdY, false);

        // Jacobian from inertial to LOF
        final double[][] dLOFdI = new double[6][6];
        univariateProcessNoise.getLofType().transformFromInertial(current.getDate(),
                                                                  current.getOrbit().getPVCoordinates()).getJacobian(CartesianDerivativesFilter.USE_PV, dLOFdI);
        final RealMatrix jacIToLOF = new Array2DRowRealMatrix(dLOFdI, false);

        // Complete Jacobian
        final RealMatrix jac = jacIToLOF.multiply(jacOrbToCar);

        // Q in LOF and Cartesian
        final RealMatrix lofQ = jac.multiply(inertialOrbitalQ).multiplyTransposed(jac);

        // Build reference LOF Cartesian orbital sigmas
        final RealVector refLofSig = new ArrayRealVector(6);
        double dt = current.getDate().durationFrom(previous.getDate());
        for (int i = 0; i < 6; i++) {
            refLofSig.setEntry(i, univariateProcessNoise.getLofCartesianOrbitalParametersEvolution()[i].value(dt));
        }

        // Compare diagonal values with reference (relative difference) and suppress them from the matrix
        // Ensure non-diagonal values are into numerical noise sensitivity
        RealVector dLofDiag = new ArrayRealVector(6);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                double refI = refLofSig.getEntry(i);
                if (i == j) {
                    // Diagonal term
                    dLofDiag.setEntry(i, FastMath.abs(1. - lofQ.getEntry(i, i) / refI / refI));
                    lofQ.setEntry(i, i, 0.);
                } else {
                    // Non-diagonal term, ref is 0.
                    lofQ.setEntry(i, j, FastMath.abs(lofQ.getEntry(i,j) / refI / refLofSig.getEntry(j)));
                }
            }

        }

        // Norm of diagonal and non-diagonal
        double dDiag = dLofDiag.getNorm();
        double dNonDiag = lofQ.getNorm1();

        // Print values ?
        if (print) {
            System.out.println("\tdt = " + dt);
            System.out.format(Locale.US, "\tΔDiagonal norm in Cartesian LOF     = %10.4e%n", dDiag);
            System.out.format(Locale.US, "\tΔNon-Diagonal norm in Cartesian LOF = %10.4e%n%n", dNonDiag);
        }
        Assertions.assertEquals(0.,  dDiag   , relativeTolerance);
        Assertions.assertEquals(0.,  dNonDiag, relativeTolerance);
    }
}
