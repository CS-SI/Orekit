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
package org.orekit.propagation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.DiagonalMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.util.Arrays;

public class StateCovarianceMatrixProviderTest {

    private SpacecraftState initialState;
    private double[][] initCov;

    @Before
    public void setUp() {
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
        Orbit initialOrbit = new CartesianOrbit(new PVCoordinates(new Vector3D(7526993.581890527, -9646310.10026971, 1464110.4928112086),
                                                                  new Vector3D(3033.79456099698, 1715.265069098717, -4447.658745923895)),
                                                FramesFactory.getEME2000(),
                                                new AbsoluteDate("2016-02-13T16:00:00.000", TimeScalesFactory.getUTC()),
                                                Constants.WGS84_EARTH_MU);
        initialState = new SpacecraftState(initialOrbit);
        initCov = new double[][] {
            {8.651816029e+01,   5.689987127e+01, -2.763870764e+01, -2.435617201e-02,  2.058274137e-02, -5.872883051e-03},
            {5.689987127e+01,   7.070624321e+01,  1.367120909e+01, -6.112622013e-03,  7.623626008e-03, -1.239413190e-02},
            {-2.763870764e+01,  1.367120909e+01,  1.811858898e+02,  3.143798992e-02, -4.963106559e-02, -7.420114385e-04},
            {-2.435617201e-02, -6.112622013e-03,  3.143798992e-02,  4.657077389e-05,  1.469943634e-05,  3.328475593e-05},
            {2.058274137e-02,   7.623626008e-03, -4.963106559e-02,  1.469943634e-05,  3.950715934e-05,  2.516044258e-05},
            {-5.872883051e-03, -1.239413190e-02, -7.420114385e-04,  3.328475593e-05,  2.516044258e-05,  3.547466120e-05}
        };
    }

    /**
     * Unit test for the covariance frame transformation.
     */
    @Test
    public void testFrameConversion() {

        // Reference
        final RealMatrix referenceCov = MatrixUtils.createRealMatrix(initCov);

        // Define frames
        final Frame frameA = FramesFactory.getEME2000();
        final Frame frameB = FramesFactory.getTEME();

        // First transformation
        RealMatrix transformedCov = StateCovarianceMatrixProvider.changeCovarianceFrame(initialState.getOrbit(), frameA, frameB, referenceCov, OrbitType.CARTESIAN, PositionAngle.MEAN);

        // Second transformation
        transformedCov = StateCovarianceMatrixProvider.changeCovarianceFrame(initialState.getOrbit(), frameB, frameA, transformedCov, OrbitType.CARTESIAN, PositionAngle.MEAN);

        // Verify
        compareCovariance(referenceCov, transformedCov, 5.2e-15);

    }

    /**
     * Unit test for the covariance type transformation.
     */
    @Test
    public void testTypeConversion() {

        // Reference
        final RealMatrix referenceCov = MatrixUtils.createRealMatrix(initCov);

        // Define orbit types
        final OrbitType cart = OrbitType.CARTESIAN;
        final OrbitType kep  = OrbitType.KEPLERIAN;

        // First transformation
        RealMatrix transformedCov = StateCovarianceMatrixProvider.changeCovarianceType(initialState.getOrbit(), cart, PositionAngle.MEAN,
                                                                                       kep, PositionAngle.MEAN, referenceCov);

        // Second transformation
        transformedCov = StateCovarianceMatrixProvider.changeCovarianceType(initialState.getOrbit(), kep, PositionAngle.MEAN,
                                                                            cart, PositionAngle.MEAN, transformedCov);

        // Verify
        compareCovariance(referenceCov, transformedCov, 3.5e-12);

    }

    /**
     * Unit test for covariance propagation in Cartesian elements.
     */
    @Test
    public void testWithNumericalPropagatorCartesian() {

        // Integrator
        final double        step       = 60.0;
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);

        // Numerical propagator
        final String stmName = "STM";
        final OrbitType propType = OrbitType.CARTESIAN;
        final PositionAngle angleType = PositionAngle.MEAN;
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        // Add a force model
        final NormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getNormalizedProvider(2, 0);
        final ForceModel holmesFeatherstone = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), gravity);
        propagator.addForceModel(holmesFeatherstone);
        // Finalize setting
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, null, null);
        propagator.setOrbitType(propType);
        propagator.setPositionAngleType(angleType);

        // Create additional state
        final String additionalName = "cartCov";
        final RealMatrix initialCov = MatrixUtils.createRealMatrix(initCov);
        final StateCovarianceMatrixProvider provider = new StateCovarianceMatrixProvider(additionalName, stmName, harvester,
                                                                                         propagator.getOrbitType(), propagator.getPositionAngleType(),
                                                                                         initialCov,
                                                                                         OrbitType.CARTESIAN, PositionAngle.MEAN);
        propagator.setInitialState(initialState);
        propagator.addAdditionalStateProvider(provider);

        // Propagate
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(Constants.JULIAN_DAY));

        // Get the propagated covariance
        final RealMatrix propagatedCov = provider.getStateCovariance(propagated);

        // Reference (computed using a different solution)
        final double[][] ref = new double[][] {
            { 5.770543135e+02,  2.316979550e+02, -5.172369105e+02, -2.585893247e-01,  2.113809017e-01, -1.759509343e-01},
            { 2.316979550e+02,  1.182942930e+02, -1.788422178e+02, -9.570305681e-02,  7.792155309e-02, -7.435822327e-02},
            {-5.172369105e+02, -1.788422178e+02,  6.996248500e+02,  2.633605389e-01, -2.480144888e-01,  1.908427233e-01},
            {-2.585893247e-01, -9.570305681e-02,  2.633605389e-01,  1.419148897e-04, -8.715858320e-05,  1.024944399e-04},
            { 2.113809017e-01,  7.792155309e-02, -2.480144888e-01, -8.715858320e-05,  1.069566588e-04, -5.667563856e-05},
            {-1.759509343e-01, -7.435822327e-02,  1.908427233e-01,  1.024944399e-04, -5.667563856e-05,  8.178356868e-05}
        };
        final RealMatrix referenceCov = MatrixUtils.createRealMatrix(ref);

        // Verify
        compareCovariance(referenceCov, propagatedCov, 4.0e-7);
        Assert.assertEquals(OrbitType.CARTESIAN, provider.getCovarianceOrbitType());

        ///////////
        // Test the frame transformation
        ///////////

        // Define a new output frame
        final Frame frameB = FramesFactory.getTEME();

        // Get the covariance in TEME frame
        RealMatrix transformedCovA = provider.getStateCovariance(propagated, frameB);

        // Second transformation
        RealMatrix transformedCovB = StateCovarianceMatrixProvider.changeCovarianceFrame(propagated.getOrbit(), propagated.getFrame(), frameB, propagatedCov, OrbitType.CARTESIAN, PositionAngle.MEAN);

        // Verify
        compareCovariance(transformedCovA, transformedCovB, 1.0e-15);

        ///////////
        // Test the orbit type transformation
        ///////////

        // Define a new output frame
        final OrbitType outOrbitType = OrbitType.KEPLERIAN;
        final PositionAngle outAngleType = PositionAngle.MEAN;

        // Transformation using getStateJacobian() method
        RealMatrix transformedCovC = provider.getStateCovariance(propagated, outOrbitType, outAngleType);

        // Second transformation
        RealMatrix transformedCovD = StateCovarianceMatrixProvider.changeCovarianceType(propagated.getOrbit(), OrbitType.CARTESIAN, PositionAngle.MEAN, outOrbitType, outAngleType, propagatedCov);

        // Verify
        compareCovariance(transformedCovC, transformedCovD, 1.0e-15);

    }

    /**
     * Unit test for covariance propagation in Cartesian elements.
     * The difference here is that the propagator uses its default orbit type: EQUINOCTIAL
     */
    @Test
    public void testWithNumericalPropagatorDefault() {

        // Integrator
        final double        step       = 60.0;
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);

        // Numerical propagator
        final String stmName = "STM";
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        // Add a force model
        final NormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getNormalizedProvider(2, 0);
        final ForceModel holmesFeatherstone = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), gravity);
        propagator.addForceModel(holmesFeatherstone);
        // Finalize setting
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, null, null);

        // Create additional state
        final String additionalName = "cartCov";
        final RealMatrix initialCov = MatrixUtils.createRealMatrix(initCov);
        final StateCovarianceMatrixProvider provider = new StateCovarianceMatrixProvider(additionalName, stmName, harvester,
                                                                                         propagator.getOrbitType(), propagator.getPositionAngleType(),
                                                                                         initialCov,
                                                                                         OrbitType.CARTESIAN, PositionAngle.MEAN);
        propagator.setInitialState(initialState);
        propagator.addAdditionalStateProvider(provider);

        // Propagate
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(Constants.JULIAN_DAY));

        // Get the propagated covariance
        final RealMatrix propagatedCov = provider.getStateCovariance(propagated);

        // Reference (computed using a different solution)
        final double[][] ref = new double[][] {
            { 5.770543135e+02,  2.316979550e+02, -5.172369105e+02, -2.585893247e-01,  2.113809017e-01, -1.759509343e-01},
            { 2.316979550e+02,  1.182942930e+02, -1.788422178e+02, -9.570305681e-02,  7.792155309e-02, -7.435822327e-02},
            {-5.172369105e+02, -1.788422178e+02,  6.996248500e+02,  2.633605389e-01, -2.480144888e-01,  1.908427233e-01},
            {-2.585893247e-01, -9.570305681e-02,  2.633605389e-01,  1.419148897e-04, -8.715858320e-05,  1.024944399e-04},
            { 2.113809017e-01,  7.792155309e-02, -2.480144888e-01, -8.715858320e-05,  1.069566588e-04, -5.667563856e-05},
            {-1.759509343e-01, -7.435822327e-02,  1.908427233e-01,  1.024944399e-04, -5.667563856e-05,  8.178356868e-05}
        };
        final RealMatrix referenceCov = MatrixUtils.createRealMatrix(ref);

        // Verify
        compareCovariance(referenceCov, propagatedCov, 3.0e-5);
        Assert.assertEquals(OrbitType.CARTESIAN, provider.getCovarianceOrbitType());

        ///////////
        // Test the frame transformation
        ///////////

        // Define a new output frame
        final Frame frameB = FramesFactory.getTEME();

        // Get the covariance in TEME frame
        RealMatrix transformedCovA = provider.getStateCovariance(propagated, frameB);

        // Second transformation
        RealMatrix transformedCovB = StateCovarianceMatrixProvider.changeCovarianceFrame(propagated.getOrbit(), propagated.getFrame(), frameB, propagatedCov, OrbitType.CARTESIAN, PositionAngle.MEAN);

        // Verify
        compareCovariance(transformedCovA, transformedCovB, 1.0e-15);

        // Define a new output frame
        final OrbitType outOrbitType = OrbitType.KEPLERIAN;
        final PositionAngle outAngleType = PositionAngle.MEAN;

        // Transformation using getStateJacobian() method
        RealMatrix transformedCovC = provider.getStateCovariance(propagated, outOrbitType, outAngleType);

        // Second transformation
        RealMatrix transformedCovD = StateCovarianceMatrixProvider.changeCovarianceType(propagated.getOrbit(), OrbitType.CARTESIAN, PositionAngle.MEAN, outOrbitType, outAngleType, propagatedCov);

        // Verify
        compareCovariance(transformedCovC, transformedCovD, 1.0e-15);

    }

    /**
     * Unit test for covariance propagation in Cartesian elements.
     */
    @Test
    public void testWithAnalyticalPropagator() {

        // Numerical propagator
        final String stmName = "STM";
        final OrbitType propType = OrbitType.CARTESIAN;
        final PositionAngle angleType = PositionAngle.MEAN;
        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initialState.getOrbit(),
                                                                                   GravityFieldFactory.getUnnormalizedProvider(6, 0));
        // Finalize setting
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, null, null);

        // Create additional state
        final String additionalName = "cartCov";
        final RealMatrix initialCov = MatrixUtils.createRealMatrix(initCov);
        final StateCovarianceMatrixProvider provider = new StateCovarianceMatrixProvider(additionalName, stmName, harvester,
                                                                                         propType, angleType,
                                                                                         initialCov,
                                                                                         OrbitType.CARTESIAN, PositionAngle.MEAN);
        propagator.addAdditionalStateProvider(provider);

        // Propagate
        final SpacecraftState propagated = propagator.propagate(initialState.getDate().shiftedBy(Constants.JULIAN_DAY));

        // Get the propagated covariance
        final RealMatrix propagatedCov = provider.getStateCovariance(propagated);

        // Reference (computed using a numerical solution)
        final double[][] ref = new double[][] {
            { 5.770543135e+02,  2.316979550e+02, -5.172369105e+02, -2.585893247e-01,  2.113809017e-01, -1.759509343e-01},
            { 2.316979550e+02,  1.182942930e+02, -1.788422178e+02, -9.570305681e-02,  7.792155309e-02, -7.435822327e-02},
            {-5.172369105e+02, -1.788422178e+02,  6.996248500e+02,  2.633605389e-01, -2.480144888e-01,  1.908427233e-01},
            {-2.585893247e-01, -9.570305681e-02,  2.633605389e-01,  1.419148897e-04, -8.715858320e-05,  1.024944399e-04},
            { 2.113809017e-01,  7.792155309e-02, -2.480144888e-01, -8.715858320e-05,  1.069566588e-04, -5.667563856e-05},
            {-1.759509343e-01, -7.435822327e-02,  1.908427233e-01,  1.024944399e-04, -5.667563856e-05,  8.178356868e-05}
        };
        final RealMatrix referenceCov = MatrixUtils.createRealMatrix(ref);

        // Verify
        compareCovariance(referenceCov, propagatedCov, 5.0e-4);
        Assert.assertEquals(OrbitType.CARTESIAN, provider.getCovarianceOrbitType());

        ///////////
        // Test the frame transformation
        ///////////

        // Define a new output frame
        final Frame frameB = FramesFactory.getTEME();

        // Get the covariance in TEME frame
        RealMatrix transformedCovA = provider.getStateCovariance(propagated, frameB);

        // Second transformation
        RealMatrix transformedCovB = StateCovarianceMatrixProvider.changeCovarianceFrame(propagated.getOrbit(), propagated.getFrame(), frameB, propagatedCov, OrbitType.CARTESIAN, PositionAngle.MEAN);

        // Verify
        compareCovariance(transformedCovA, transformedCovB, 1.0e-15);

        // Define a new output frame
        final OrbitType     outOrbitType = OrbitType.KEPLERIAN;
        final PositionAngle outAngleType = PositionAngle.MEAN;

        // Transformation using getStateJacobian() method
        RealMatrix transformedCovC = provider.getStateCovariance(propagated, outOrbitType, outAngleType);

        // Second transformation
        RealMatrix transformedCovD =
                StateCovarianceMatrixProvider.changeCovarianceType(propagated.getOrbit(), OrbitType.CARTESIAN,
                        PositionAngle.MEAN, outOrbitType, outAngleType, propagatedCov);

        // Verify
        compareCovariance(transformedCovC, transformedCovD, 1.0e-15);

    }

    /**
     * Compare two covariance matrices
     *
     * @param reference reference covariance
     * @param computed  computed covariance
     * @param threshold threshold for comparison
     */
    private void compareCovariance(final RealMatrix reference, final RealMatrix computed, final double threshold) {
        for (int row = 0; row < reference.getRowDimension(); row++) {
            for (int column = 0; column < reference.getColumnDimension(); column++) {
                Assert.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column),
                        FastMath.abs(threshold * reference.getEntry(row, column)));
            }
        }
    }

    /**
     * Test transform obtained from getTransform to convert a covariance matrix.
     */
    @Test
    public void testConvertCovFrame() {

        // Given
        final Orbit keplerianOrbit = new KeplerianOrbit(7.E6, 0.001, FastMath.toRadians(45.), 0., 0., 0.,
                PositionAngle.TRUE, FramesFactory.getEME2000(),
                new AbsoluteDate(2000, 1, 1, TimeScalesFactory.getUTC()),
                Constants.GRIM5C1_EARTH_MU);

        final Orbit cartesianOrbit = OrbitType.CARTESIAN.convertType(keplerianOrbit);

        final Frame pivotFrame = FramesFactory.getGCRF();

        double sig_sma = 1000;
        double sig_ecc = 1000 / cartesianOrbit.getA();
        double sig_inc = FastMath.toRadians(0.01);
        double sig_pom = FastMath.toRadians(0.01);
        double sig_gom = FastMath.toRadians(0.01);
        double sig_anm = FastMath.toRadians(0.1);

        sig_sma *= sig_sma;
        sig_ecc *= sig_ecc;
        sig_inc *= sig_inc;
        sig_pom *= sig_pom;
        sig_gom *= sig_gom;
        sig_anm *= sig_anm;

        final RealMatrix covarianceMatrix = new DiagonalMatrix(new double[] { sig_sma, sig_ecc, sig_inc,
                sig_pom, sig_gom, sig_anm });

        CelestialBodyFrame in = CelestialBodyFrame.EME2000;
        FrameFacade from = new FrameFacade(in.getFrame(IERSConventions.IERS_2010, false, DataContext.getDefault()),
                in, null, null, in.name());

        OrbitRelativeFrame out = OrbitRelativeFrame.TNW;
        FrameFacade        to  = new FrameFacade(null, null, out, null, out.name());

        System.out.println("CREATING TRANSFORM");
        final Transform inToOutTransform = FrameFacade.getTransform(from, to, pivotFrame, cartesianOrbit.getDate(),
                cartesianOrbit);

        System.out.println("rotation from transform");
        System.out.println(Arrays.deepToString(inToOutTransform.getRotation().getMatrix()));

        System.out.println("rotation rate from transform");
        System.out.println(inToOutTransform.getRotationRate());

        // When
        /*
        final RealMatrix converted = StateCovarianceMatrixProvider.changeCovarianceFrame(cartesianOrbit, transform,
                covarianceMatrix, OrbitType.CARTESIAN, PositionAngle.MEAN);

         */
        // Gets the rotation from the transform
        final double[][] rotInToOut = inToOutTransform.getRotation().getMatrix();
        // Builds the matrix to perform covariance transformation
        final RealMatrix matInToOut = MatrixUtils.createRealMatrix(6, 6);
        // Fills in the upper left and lower right blocks with the rotation
        matInToOut.setSubMatrix(rotInToOut, 0, 0);
        matInToOut.setSubMatrix(rotInToOut, 3, 3);
        final RealMatrix converted = matInToOut.multiply(covarianceMatrix.multiplyTransposed(matInToOut));

        // Get the Jacobian of the transform
        final double[][] jacobian = new double[6][6];
        inToOutTransform.getJacobian(CartesianDerivativesFilter.USE_PV, jacobian);

        System.out.println("jacobian");
        System.out.println(Arrays.deepToString(jacobian));

        // Matrix to perform the covariance transformation
        final RealMatrix j = new Array2DRowRealMatrix(jacobian, false);

        // Then
        // Reference data from CelestLab :
        // https://sourceforge.isae.fr/svn/dcas-soft-espace/support/softs/CelestLab/trunk/help/en_US/scilab_en_US_help/jacobian%20matrices.html cas-3
        // Attention : CCSDS covariance matrix are supposed to be expressed in cartesian.
        //             However,the Celestlab example uses keplerian data.
        //             So here we consider that the covariance is cartesian.
        //             The celestlab variable dpv_dkep must be overloaded so that it can be an identity matrix.

        double[][] expected = {
                { 2.543E-08, 0., 5.027E-09, 0., 0., 0. },
                { 0., 1000000., 0., 0., 0., 0. },
                { 5.027E-09, 0., 2.543E-08, 0., 0., 0. },
                { 0., 0., 0., 0.0000015, 0., 0.0000015 },
                { 0., 0., 0., 0., 3.046E-08, 0. },
                { 0., 0., 0., 0.0000015, 0., 0.0000015 } };

        System.out.println("Pure rotation matrix");
        System.out.println(Arrays.deepToString(matInToOut.getData()));

        System.out.println("Matrix from jacobian");
        System.out.println(Arrays.deepToString(j.getData()));

        // Both matrices are identical if the absolute error on each term doesn't exceed 1E-7 USI
        validateMatrix(expected, converted.getData(), 4.e-8);
    }

    /**
     * Assert that data double array is equals to expected.
     *
     * @param data      input data to assert
     * @param expected  expected data
     * @param threshold threshold for precision
     */
    private void validateMatrix(double[][] expected, double[][] data, double threshold) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                Assert.assertEquals(expected[i][j], data[i][j], threshold);
            }
        }
    }

}
