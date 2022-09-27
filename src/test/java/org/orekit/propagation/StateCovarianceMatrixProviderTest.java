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
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.util.Locale;

public class StateCovarianceMatrixProviderTest {

    private SpacecraftState initialState;
    private double[][]      initCov;

    final private double DEFAULT_VALLADO_THRESHOLD = 1e-6;

    /**
     * Unit test for the covariance frame transformation.
     */
    @Test
    public void testFrameConversion() {

        // Initialization
        setUp();

        // Reference
        final RealMatrix referenceCov = MatrixUtils.createRealMatrix(initCov);

        // Define frames
        final Frame frameA = FramesFactory.getEME2000();
        final Frame frameB = FramesFactory.getTEME();

        // First transformation
        RealMatrix transformedCov =
                StateCovarianceMatrixProvider.changeCovarianceFrame(initialState.getOrbit(), frameA, frameB,
                                                                    referenceCov, OrbitType.CARTESIAN,
                                                                    PositionAngle.MEAN);

        // Second transformation
        transformedCov = StateCovarianceMatrixProvider.changeCovarianceFrame(initialState.getOrbit(), frameB, frameA,
                                                                             transformedCov, OrbitType.CARTESIAN,
                                                                             PositionAngle.MEAN);

        // Verify
        compareCovariance(referenceCov, transformedCov, 5.2e-15);

    }

    public void setUp() {
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
        Orbit initialOrbit = new CartesianOrbit(
                new PVCoordinates(new Vector3D(7526993.581890527, -9646310.10026971, 1464110.4928112086),
                                  new Vector3D(3033.79456099698, 1715.265069098717, -4447.658745923895)),
                FramesFactory.getEME2000(),
                new AbsoluteDate("2016-02-13T16:00:00.000", TimeScalesFactory.getUTC()),
                Constants.WGS84_EARTH_MU);
        initialState = new SpacecraftState(initialOrbit);
        initCov = new double[][] {
                { 8.651816029e+01, 5.689987127e+01, -2.763870764e+01, -2.435617201e-02, 2.058274137e-02,
                        -5.872883051e-03 },
                { 5.689987127e+01, 7.070624321e+01, 1.367120909e+01, -6.112622013e-03, 7.623626008e-03,
                        -1.239413190e-02 },
                { -2.763870764e+01, 1.367120909e+01, 1.811858898e+02, 3.143798992e-02, -4.963106559e-02,
                        -7.420114385e-04 },
                { -2.435617201e-02, -6.112622013e-03, 3.143798992e-02, 4.657077389e-05, 1.469943634e-05,
                        3.328475593e-05 },
                { 2.058274137e-02, 7.623626008e-03, -4.963106559e-02, 1.469943634e-05, 3.950715934e-05,
                        2.516044258e-05 },
                { -5.872883051e-03, -1.239413190e-02, -7.420114385e-04, 3.328475593e-05, 2.516044258e-05,
                        3.547466120e-05 }
        };
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
                if (reference.getEntry(row, column) == 0) {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column),
                                            threshold);
                }
                else {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column),
                                            FastMath.abs(threshold * reference.getEntry(row, column)));
                }
            }
        }
    }

    @Test
    @DisplayName("Test conversion from inertial frame to RTN local orbital frame")
    void should_return_same_covariance_matrix() {

        // Given
        final AbsoluteDate initialDate   = new AbsoluteDate();
        final Frame        inertialFrame = FramesFactory.getGCRF();
        final double       mu            = 398600e9;

        final PVCoordinates initialPV = new PVCoordinates(
                new Vector3D(6778000, 0, 0),
                new Vector3D(0, 7668.63, 0));

        final Orbit initialOrbit = new CartesianOrbit(initialPV, inertialFrame, initialDate, mu);

        final RealMatrix initialCovarianceInInertialFrame = new BlockRealMatrix(new double[][] {
                { 1, 0, 0, 0, 0, 0 },
                { 0, 1, 0, 0, 0, 0 },
                { 0, 0, 1, 0, 0, 0 },
                { 0, 0, 0, 1e-3, 0, 0 },
                { 0, 0, 0, 0, 1e-3, 0 },
                { 0, 0, 0, 0, 0, 1e-3 }
        });

        // When
        final RealMatrix covarianceMatrixInRTN = StateCovarianceMatrixProvider.changeCovarianceFrame(initialOrbit,
                                                                                                     inertialFrame,
                                                                                                     LOFType.QSW,
                                                                                                     initialCovarianceInInertialFrame,
                                                                                                     OrbitType.CARTESIAN,
                                                                                                     PositionAngle.MEAN);

        // Then
        final RealMatrix expectedMatrixInRTN = new BlockRealMatrix(new double[][] {
                { 1, 0, 0, 0, 0, 0 },
                { 0, 1, 0, 0, 0, 0 },
                { 0, 0, 1, 0, 0, 0 },
                { 0, 0, 0, 1e-3, 0, 0 },
                { 0, 0, 0, 0, 1e-3, 0 },
                { 0, 0, 0, 0, 0, 1e-3 }
        });

        compareCovariance(covarianceMatrixInRTN, expectedMatrixInRTN, 1e-20);

    }

    /**
     * Unit test for the covariance type transformation.
     */
    @Test
    public void testTypeConversion() {

        // Initialization
        setUp();

        // Reference
        final RealMatrix referenceCov = MatrixUtils.createRealMatrix(initCov);

        // Define orbit types
        final OrbitType cart = OrbitType.CARTESIAN;
        final OrbitType kep  = OrbitType.KEPLERIAN;

        // First transformation
        RealMatrix transformedCov =
                StateCovarianceMatrixProvider.changeCovarianceType(initialState.getOrbit(), cart, PositionAngle.MEAN,
                                                                   kep, PositionAngle.MEAN, referenceCov);

        // Second transformation
        transformedCov =
                StateCovarianceMatrixProvider.changeCovarianceType(initialState.getOrbit(), kep, PositionAngle.MEAN,
                                                                   cart, PositionAngle.MEAN, transformedCov);

        // Verify
        compareCovariance(referenceCov, transformedCov, 3.5e-12);

    }

    /**
     * Unit test for covariance propagation in Cartesian elements.
     */
    @Test
    public void testWithNumericalPropagatorCartesian() {

        // Initialization
        setUp();

        // Integrator
        final double        step       = 60.0;
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);

        // Numerical propagator
        final String              stmName    = "STM";
        final OrbitType           propType   = OrbitType.CARTESIAN;
        final PositionAngle       angleType  = PositionAngle.MEAN;
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        // Add a force model
        final NormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getNormalizedProvider(2, 0);
        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), gravity);
        propagator.addForceModel(holmesFeatherstone);
        // Finalize setting
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, null, null);
        propagator.setOrbitType(propType);
        propagator.setPositionAngleType(angleType);

        // Create additional state
        final String     additionalName = "cartCov";
        final RealMatrix initialCov     = MatrixUtils.createRealMatrix(initCov);
        final StateCovarianceMatrixProvider provider =
                new StateCovarianceMatrixProvider(additionalName, stmName, harvester,
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
                { 5.770543135e+02, 2.316979550e+02, -5.172369105e+02, -2.585893247e-01, 2.113809017e-01,
                        -1.759509343e-01 },
                { 2.316979550e+02, 1.182942930e+02, -1.788422178e+02, -9.570305681e-02, 7.792155309e-02,
                        -7.435822327e-02 },
                { -5.172369105e+02, -1.788422178e+02, 6.996248500e+02, 2.633605389e-01, -2.480144888e-01,
                        1.908427233e-01 },
                { -2.585893247e-01, -9.570305681e-02, 2.633605389e-01, 1.419148897e-04, -8.715858320e-05,
                        1.024944399e-04 },
                { 2.113809017e-01, 7.792155309e-02, -2.480144888e-01, -8.715858320e-05, 1.069566588e-04,
                        -5.667563856e-05 },
                { -1.759509343e-01, -7.435822327e-02, 1.908427233e-01, 1.024944399e-04, -5.667563856e-05,
                        8.178356868e-05 }
        };
        final RealMatrix referenceCov = MatrixUtils.createRealMatrix(ref);

        // Verify
        compareCovariance(referenceCov, propagatedCov, 4.0e-7);
        Assertions.assertEquals(OrbitType.CARTESIAN, provider.getCovarianceOrbitType());

        ///////////
        // Test the frame transformation
        ///////////

        // Define a new output frame
        final Frame frameB = FramesFactory.getTEME();

        // Get the covariance in TEME frame
        RealMatrix transformedCovA = provider.getStateCovariance(propagated, frameB);

        // Second transformation
        RealMatrix transformedCovB =
                StateCovarianceMatrixProvider.changeCovarianceFrame(propagated.getOrbit(), propagated.getFrame(),
                                                                    frameB, propagatedCov, OrbitType.CARTESIAN,
                                                                    PositionAngle.MEAN);

        // Verify
        compareCovariance(transformedCovA, transformedCovB, 1.0e-15);

        ///////////
        // Test the orbit type transformation
        ///////////

        // Define a new output frame
        final OrbitType     outOrbitType = OrbitType.KEPLERIAN;
        final PositionAngle outAngleType = PositionAngle.MEAN;

        // Transformation using getStateJacobian() method
        RealMatrix transformedCovC = provider.getStateCovariance(propagated, outOrbitType, outAngleType);

        // Second transformation
        RealMatrix transformedCovD =
                StateCovarianceMatrixProvider.changeCovarianceType(propagated.getOrbit(), OrbitType.CARTESIAN,
                                                                   PositionAngle.MEAN, outOrbitType, outAngleType,
                                                                   propagatedCov);

        // Verify
        compareCovariance(transformedCovC, transformedCovD, 1.0e-15);

    }

    /**
     * Unit test for covariance propagation in Cartesian elements. The difference here is that the propagator uses its
     * default orbit type: EQUINOCTIAL
     */
    @Test
    public void testWithNumericalPropagatorDefault() {

        // Initialization
        setUp();

        // Integrator
        final double        step       = 60.0;
        final ODEIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);

        // Numerical propagator
        final String              stmName    = "STM";
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        // Add a force model
        final NormalizedSphericalHarmonicsProvider gravity = GravityFieldFactory.getNormalizedProvider(2, 0);
        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), gravity);
        propagator.addForceModel(holmesFeatherstone);
        // Finalize setting
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, null, null);

        // Create additional state
        final String     additionalName = "cartCov";
        final RealMatrix initialCov     = MatrixUtils.createRealMatrix(initCov);
        final StateCovarianceMatrixProvider provider =
                new StateCovarianceMatrixProvider(additionalName, stmName, harvester,
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
                { 5.770543135e+02, 2.316979550e+02, -5.172369105e+02, -2.585893247e-01, 2.113809017e-01,
                        -1.759509343e-01 },
                { 2.316979550e+02, 1.182942930e+02, -1.788422178e+02, -9.570305681e-02, 7.792155309e-02,
                        -7.435822327e-02 },
                { -5.172369105e+02, -1.788422178e+02, 6.996248500e+02, 2.633605389e-01, -2.480144888e-01,
                        1.908427233e-01 },
                { -2.585893247e-01, -9.570305681e-02, 2.633605389e-01, 1.419148897e-04, -8.715858320e-05,
                        1.024944399e-04 },
                { 2.113809017e-01, 7.792155309e-02, -2.480144888e-01, -8.715858320e-05, 1.069566588e-04,
                        -5.667563856e-05 },
                { -1.759509343e-01, -7.435822327e-02, 1.908427233e-01, 1.024944399e-04, -5.667563856e-05,
                        8.178356868e-05 }
        };
        final RealMatrix referenceCov = MatrixUtils.createRealMatrix(ref);

        // Verify
        compareCovariance(referenceCov, propagatedCov, 3.0e-5);
        Assertions.assertEquals(OrbitType.CARTESIAN, provider.getCovarianceOrbitType());

        ///////////
        // Test the frame transformation
        ///////////

        // Define a new output frame
        final Frame frameB = FramesFactory.getTEME();

        // Get the covariance in TEME frame
        RealMatrix transformedCovA = provider.getStateCovariance(propagated, frameB);

        // Second transformation
        RealMatrix transformedCovB =
                StateCovarianceMatrixProvider.changeCovarianceFrame(propagated.getOrbit(), propagated.getFrame(),
                                                                    frameB, propagatedCov, OrbitType.CARTESIAN,
                                                                    PositionAngle.MEAN);

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
                                                                   PositionAngle.MEAN, outOrbitType, outAngleType,
                                                                   propagatedCov);

        // Verify
        compareCovariance(transformedCovC, transformedCovD, 1.0e-15);

    }

    /**
     * Unit test for covariance propagation in Cartesian elements.
     */
    @Test
    public void testWithAnalyticalPropagator() {

        // Initialization
        setUp();

        // Numerical propagator
        final String        stmName   = "STM";
        final OrbitType     propType  = OrbitType.CARTESIAN;
        final PositionAngle angleType = PositionAngle.MEAN;
        final EcksteinHechlerPropagator propagator = new EcksteinHechlerPropagator(initialState.getOrbit(),
                                                                                   GravityFieldFactory.getUnnormalizedProvider(
                                                                                           6, 0));

        // Finalize setting
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmName, null, null);

        // Create additional state
        final String     additionalName = "cartCov";
        final RealMatrix initialCov     = MatrixUtils.createRealMatrix(initCov);
        final StateCovarianceMatrixProvider provider =
                new StateCovarianceMatrixProvider(additionalName, stmName, harvester,
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
                { 5.770543135e+02, 2.316979550e+02, -5.172369105e+02, -2.585893247e-01, 2.113809017e-01,
                        -1.759509343e-01 },
                { 2.316979550e+02, 1.182942930e+02, -1.788422178e+02, -9.570305681e-02, 7.792155309e-02,
                        -7.435822327e-02 },
                { -5.172369105e+02, -1.788422178e+02, 6.996248500e+02, 2.633605389e-01, -2.480144888e-01,
                        1.908427233e-01 },
                { -2.585893247e-01, -9.570305681e-02, 2.633605389e-01, 1.419148897e-04, -8.715858320e-05,
                        1.024944399e-04 },
                { 2.113809017e-01, 7.792155309e-02, -2.480144888e-01, -8.715858320e-05, 1.069566588e-04,
                        -5.667563856e-05 },
                { -1.759509343e-01, -7.435822327e-02, 1.908427233e-01, 1.024944399e-04, -5.667563856e-05,
                        8.178356868e-05 }
        };
        final RealMatrix referenceCov = MatrixUtils.createRealMatrix(ref);

        // Verify
        compareCovariance(referenceCov, propagatedCov, 5.0e-4);
        Assertions.assertEquals(OrbitType.CARTESIAN, provider.getCovarianceOrbitType());

        ///////////
        // Test the frame transformation
        ///////////

        // Define a new output frame
        final Frame frameB = FramesFactory.getTEME();

        // Get the covariance in TEME frame
        RealMatrix transformedCovA = provider.getStateCovariance(propagated, frameB);

        // Second transformation
        RealMatrix transformedCovB =
                StateCovarianceMatrixProvider.changeCovarianceFrame(propagated.getOrbit(), propagated.getFrame(),
                                                                    frameB, propagatedCov, OrbitType.CARTESIAN,
                                                                    PositionAngle.MEAN);

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
                                                                   PositionAngle.MEAN, outOrbitType, outAngleType,
                                                                   propagatedCov);

        // Verify
        compareCovariance(transformedCovC, transformedCovD, 1.0e-15);

    }

    @Test
    @DisplayName("Test conversion from inertial frame to RTN local orbital frame")
    void should_rotate_covariance_matrix_by_ninety_degrees() {

        // Given
        final AbsoluteDate initialDate   = new AbsoluteDate();
        final Frame        inertialFrame = FramesFactory.getGCRF();
        final double       mu            = 398600e9;

        final PVCoordinates initialPV = new PVCoordinates(
                new Vector3D(0, 6778000, 0),
                new Vector3D(-7668.63, 0, 0));

        final Orbit initialOrbit = new CartesianOrbit(initialPV, inertialFrame, initialDate, mu);

        final RealMatrix initialCovarianceInInertialFrame = new BlockRealMatrix(new double[][] {
                { 1, 0, 0, 0, 0, 1e-5 },
                { 0, 1, 0, 0, 0, 0 },
                { 0, 0, 1, 0, 0, 0 },
                { 0, 0, 0, 1e-3, 0, 0 },
                { 0, 0, 0, 0, 1e-3, 0 },
                { 1e-5, 0, 0, 0, 0, 1e-3 }
        });

        // When
        final RealMatrix convertedCovarianceMatrixInRTN =
                StateCovarianceMatrixProvider.changeCovarianceFrame(initialOrbit,
                                                                    inertialFrame, LOFType.QSW,
                                                                    initialCovarianceInInertialFrame,
                                                                    OrbitType.CARTESIAN,
                                                                    PositionAngle.MEAN);

        // Then

        // Expected covariance matrix obtained by rotation initial covariance matrix by 90 degrees
        final RealMatrix expectedMatrixInRTN = new BlockRealMatrix(new double[][] {
                { 1, 0, 0, 0, 0, 0 },
                { 0, 1, 0, 0, 0, -1e-5 },
                { 0, 0, 1, 0, 0, 0 },
                { 0, 0, 0, 1e-3, 0, 0 },
                { 0, 0, 0, 0, 1e-3, 0 },
                { 0, -1e-5, 0, 0, 0, 1e-3 }
        });

        compareCovariance(expectedMatrixInRTN, convertedCovarianceMatrixInRTN, 1e-20);
    }

    @Test
    @DisplayName("Test conversion from RTN local orbital frame to inertial frame")
    void should_rotate_covariance_matrix_by_minus_ninety_degrees() {

        // Given
        final AbsoluteDate initialDate   = new AbsoluteDate();
        final Frame        inertialFrame = FramesFactory.getGCRF();
        final double       mu            = 398600e9;

        final PVCoordinates initialPV = new PVCoordinates(
                new Vector3D(0, 6778000, 0),
                new Vector3D(-7668.63, 0, 0));

        final Orbit initialOrbit = new CartesianOrbit(initialPV, inertialFrame, initialDate, mu);

        final RealMatrix initialCovarianceInRTN = new BlockRealMatrix(new double[][] {
                { 1, 0, 0, 0, 0, 0 },
                { 0, 1, 0, 0, 0, -1e-5 },
                { 0, 0, 1, 0, 0, 0 },
                { 0, 0, 0, 1e-3, 0, 0 },
                { 0, 0, 0, 0, 1e-3, 0 },
                { 0, -1e-5, 0, 0, 0, 1e-3 }
        });

        // When
        final RealMatrix convertedCovarianceMatrixInInertialFrame =
                StateCovarianceMatrixProvider.changeCovarianceFrame(initialOrbit,
                                                                    LOFType.QSW, inertialFrame, initialCovarianceInRTN,
                                                                    OrbitType.CARTESIAN, PositionAngle.MEAN);

        // Then

        // Expected covariance matrix obtained by rotation initial covariance matrix by -90 degrees
        final RealMatrix expectedMatrixInInertialFrame = new BlockRealMatrix(new double[][] {
                { 1, 0, 0, 0, 0, 1e-5 },
                { 0, 1, 0, 0, 0, 0 },
                { 0, 0, 1, 0, 0, 0 },
                { 0, 0, 0, 1e-3, 0, 0 },
                { 0, 0, 0, 0, 1e-3, 0 },
                { 1e-5, 0, 0, 0, 0, 1e-3 }
        });

        compareCovariance(expectedMatrixInInertialFrame, convertedCovarianceMatrixInInertialFrame, 1e-20);

    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations
     * from David A. Vallado.
     * <p>
     * More specifically, we're using the initial covariance matrix from p.14 and compare the computed result with the
     * cartesian covariance in RSW from p.19.
     */
    @Test
    @DisplayName("Test Vallado test case : ECI cartesian to RTN")
    void should_Vallado_RSW_covariance_matrix() {

        // Initialize Orekit
        Utils.setDataRoot("regular-data");

        // Given
        final AbsoluteDate  initialDate   = getValladoInitialDate();
        final PVCoordinates initialPV     = getValladoInitialPV();
        final Frame         inertialFrame = FramesFactory.getGCRF();
        final Orbit         initialOrbit  = new CartesianOrbit(initialPV, inertialFrame, initialDate, getValladoMu());

        final RealMatrix initialCovarianceMatrix = getValladoInitialCovarianceMatrix();

        // When
        final RealMatrix convertedCovarianceMatrixInRTN =
                StateCovarianceMatrixProvider.changeCovarianceFrame(initialOrbit,
                                                                    inertialFrame, LOFType.QSW, initialCovarianceMatrix,
                                                                    OrbitType.CARTESIAN, PositionAngle.MEAN);

        // Then
        final RealMatrix expectedCovarianceMatrixInRTN = new BlockRealMatrix(new double[][] {
                { 9.918921e-001, 6.700644e-003, -2.878187e-003, 1.892086e-005, 6.700644e-005, -2.878187e-005 },
                { 6.700644e-003, 1.013730e+000, -1.019283e-002, 6.700644e-005, 2.372970e-004, -1.019283e-004 },
                { -2.878187e-003, -1.019283e-002, 9.943782e-001, -2.878187e-005, -1.019283e-004, 4.378217e-005 },
                { 1.892086e-005, 6.700644e-005, -2.878187e-005, 1.892086e-007, 6.700644e-007, -2.878187e-007 },
                { 6.700644e-005, 2.372970e-004, -1.019283e-004, 6.700644e-007, 2.372970e-006, -1.019283e-006 },
                { -2.878187e-005, -1.019283e-004, 4.378217e-005, -2.878187e-007, -1.019283e-006, 4.378217e-007 }
        });

        compareCovariance(expectedCovarianceMatrixInRTN, convertedCovarianceMatrixInRTN, DEFAULT_VALLADO_THRESHOLD);

    }

    private AbsoluteDate getValladoInitialDate() {
        return new AbsoluteDate(2000, 12, 15, 16, 58, 50.208, TimeScalesFactory.getUTC());
    }

    private PVCoordinates getValladoInitialPV() {
        return new PVCoordinates(
                new Vector3D(-605792.21660, -5870229.51108, 3493053.19896),
                new Vector3D(-1568.25429, -3702.34891, -6479.48395));
    }

    private double getValladoMu() {
        return Constants.IERS2010_EARTH_MU;
    }

    private RealMatrix getValladoInitialCovarianceMatrix() {
        return new BlockRealMatrix(new double[][] {
                { 1, 1e-2, 1e-2, 1e-4, 1e-4, 1e-4 },
                { 1e-2, 1, 1e-2, 1e-4, 1e-4, 1e-4 },
                { 1e-2, 1e-2, 1, 1e-4, 1e-4, 1e-4 },
                { 1e-4, 1e-4, 1e-4, 1e-6, 1e-6, 1e-6 },
                { 1e-4, 1e-4, 1e-4, 1e-6, 1e-6, 1e-6 },
                { 1e-4, 1e-4, 1e-4, 1e-6, 1e-6, 1e-6 }
        });
    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations
     * from David A. Vallado.
     * <p>
     * More specifically, we're using the initial covariance matrix from p.14 and compare the computed result with the
     * cartesian covariance in NTW from p.19.
     */
    @Test
    @DisplayName("Test Vallado test case : ECI cartesian to NTW")
    void should_Vallado_NTW_covariance_matrix() {

        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        final AbsoluteDate  initialDate   = getValladoInitialDate();
        final PVCoordinates initialPV     = getValladoInitialPV();
        final Frame         inertialFrame = FramesFactory.getGCRF();
        final Orbit         initialOrbit  = new CartesianOrbit(initialPV, inertialFrame, initialDate, getValladoMu());

        final RealMatrix initialCovarianceMatrix = getValladoInitialCovarianceMatrix();

        // When
        final RealMatrix convertedCovarianceMatrixInNTW =
                StateCovarianceMatrixProvider.changeCovarianceFrame(initialOrbit,
                                                                    inertialFrame, LOFType.NTW, initialCovarianceMatrix,
                                                                    OrbitType.CARTESIAN, PositionAngle.MEAN);

        // Then
        final RealMatrix expectedCovarianceMatrixInNTW = new BlockRealMatrix(new double[][] {
                { 9.918792e-001, 6.679546e-003, -2.868345e-003, 1.879167e-005, 6.679546e-005, -2.868345e-005 },
                { 6.679546e-003, 1.013743e+000, -1.019560e-002, 6.679546e-005, 2.374262e-004, -1.019560e-004 },
                { -2.868345e-003, -1.019560e-002, 9.943782e-001, -2.868345e-005, -1.019560e-004, 4.378217e-005 },
                { 1.879167e-005, 6.679546e-005, -2.868345e-005, 1.879167e-007, 6.679546e-007, -2.868345e-007 },
                { 6.679546e-005, 2.374262e-004, -1.019560e-004, 6.679546e-007, 2.374262e-006, -1.019560e-006 },
                { -2.868345e-005, -1.019560e-004, 4.378217e-005, -2.868345e-007, -1.019560e-006, 4.378217e-007 }
        });

        compareCovariance(expectedCovarianceMatrixInNTW, convertedCovarianceMatrixInNTW, DEFAULT_VALLADO_THRESHOLD);

    }

    @Test
    @DisplayName("Test conversion from Vallado test case NTW to RSW")
    void should_convert_Vallado_NTW_to_RSW() {

        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        final AbsoluteDate  initialDate   = getValladoInitialDate();
        final PVCoordinates initialPV     = getValladoInitialPV();
        final Frame         inertialFrame = FramesFactory.getGCRF();
        final Orbit         initialOrbit  = new CartesianOrbit(initialPV, inertialFrame, initialDate, getValladoMu());

        final RealMatrix initialCovarianceMatrixInNTW = new BlockRealMatrix(new double[][] {
                { 9.918792e-001, 6.679546e-003, -2.868345e-003, 1.879167e-005, 6.679546e-005, -2.868345e-005 },
                { 6.679546e-003, 1.013743e+000, -1.019560e-002, 6.679546e-005, 2.374262e-004, -1.019560e-004 },
                { -2.868345e-003, -1.019560e-002, 9.943782e-001, -2.868345e-005, -1.019560e-004, 4.378217e-005 },
                { 1.879167e-005, 6.679546e-005, -2.868345e-005, 1.879167e-007, 6.679546e-007, -2.868345e-007 },
                { 6.679546e-005, 2.374262e-004, -1.019560e-004, 6.679546e-007, 2.374262e-006, -1.019560e-006 },
                { -2.868345e-005, -1.019560e-004, 4.378217e-005, -2.868345e-007, -1.019560e-006, 4.378217e-007 }
        });

        // When
        final RealMatrix convertedCovarianceMatrixInRTN =
                StateCovarianceMatrixProvider.changeCovarianceFrame(initialOrbit, LOFType.NTW, LOFType.QSW,
                                                                    inertialFrame, initialCovarianceMatrixInNTW,
                                                                    OrbitType.CARTESIAN, PositionAngle.MEAN);

        // Then
        final RealMatrix expectedCovarianceMatrixInRTN = new BlockRealMatrix(new double[][] {
                { 9.918921e-001, 6.700644e-003, -2.878187e-003, 1.892086e-005, 6.700644e-005, -2.878187e-005 },
                { 6.700644e-003, 1.013730e+000, -1.019283e-002, 6.700644e-005, 2.372970e-004, -1.019283e-004 },
                { -2.878187e-003, -1.019283e-002, 9.943782e-001, -2.878187e-005, -1.019283e-004, 4.378217e-005 },
                { 1.892086e-005, 6.700644e-005, -2.878187e-005, 1.892086e-007, 6.700644e-007, -2.878187e-007 },
                { 6.700644e-005, 2.372970e-004, -1.019283e-004, 6.700644e-007, 2.372970e-006, -1.019283e-006 },
                { -2.878187e-005, -1.019283e-004, 4.378217e-005, -2.878187e-007, -1.019283e-006, 4.378217e-007 }
        });
        System.out.println("convertedCovarianceMatrixInRTN");
        printMatrix(convertedCovarianceMatrixInRTN);
        compareCovariance(expectedCovarianceMatrixInRTN, convertedCovarianceMatrixInRTN, 1e-3);

    }

    public static void printMatrix(final RealMatrix covariance) {

        // Create a string builder
        final StringBuilder covToPrint = new StringBuilder();
        for (int row = 0; row < covariance.getRowDimension(); row++) {
            for (int column = 0; column < covariance.getColumnDimension(); column++) {
                covToPrint.append(String.format(Locale.US, "%16.16e", covariance.getEntry(row, column)));
                covToPrint.append(" ");
            }
            covToPrint.append("\n");
        }

        // Print
        System.out.println(covToPrint);

    }

}
