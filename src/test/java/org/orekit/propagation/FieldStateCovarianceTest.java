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
package org.orekit.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.BlockFieldMatrix;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOF;
import org.orekit.frames.LOFType;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

class FieldStateCovarianceTest {

    private final Field<Binary64> field                     = Binary64Field.getInstance();
    private final double          DEFAULT_VALLADO_THRESHOLD = 1e-6;

    @Test
    @DisplayName("Test conversion from inertial frame to RTN local orbital frame")
    public void should_return_same_covariance_matrix() {

        // Given
        final FieldAbsoluteDate<Binary64> initialDate          = new FieldAbsoluteDate<>(field);
        final Frame                       initialInertialFrame = FramesFactory.getGCRF();
        final Binary64                    mu                   = new Binary64(398600e9);

        final FieldPVCoordinates<Binary64> initialPV =
                new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(6778000, 0, 0),
                                                                  new Vector3D(0, 7668.63, 0)));

        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, mu);

        final BlockFieldMatrix<Binary64> initialCovarianceInInertialFrame = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(1), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(1e-5),
                  new Binary64(1e-4) },
                { new Binary64(0), new Binary64(1), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(1e-5) },
                { new Binary64(0), new Binary64(0), new Binary64(1), new Binary64(0), new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(1e-3), new Binary64(0), new Binary64(0) },
                { new Binary64(1e-5), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(1e-3),
                  new Binary64(0) },
                { new Binary64(1e-4), new Binary64(1e-5), new Binary64(0), new Binary64(0), new Binary64(0),
                  new Binary64(1e-3) }
        });

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceInInertialFrame, initialDate, initialInertialFrame,
                                           OrbitType.CARTESIAN, PositionAngleType.MEAN);
        // When
        final FieldMatrix<Binary64> covarianceMatrixInRTN =
                stateCovariance.changeCovarianceFrame(initialOrbit, LOFType.QSW_INERTIAL).getMatrix();

        // Then
        compareCovariance(initialCovarianceInInertialFrame, covarianceMatrixInRTN, DEFAULT_VALLADO_THRESHOLD);

    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations from
     * David A. Vallado.
     * <p>
     * More specifically, we're using the initial covariance matrix from p.14 and compare the computed result with the
     * cartesian covariance in MOD from p.17.
     * </p>
     */
    @Test
    @DisplayName("Test covariance conversion Vallado test case : ECI cartesian to MOD")
    public void should_return_Vallado_MOD_covariance_matrix_from_ECI() {

        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        final FieldAbsoluteDate<Binary64>  initialDate          = getValladoInitialDate(field);
        final FieldPVCoordinates<Binary64> initialPV            = getValladoInitialPV(field);
        final Frame                        initialInertialFrame = FramesFactory.getGCRF();
        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, getValladoMu(field));

        final FieldMatrix<Binary64> initialCovarianceMatrix = getValladoInitialCovarianceMatrix();

        final Frame outputFrame = FramesFactory.getMOD(IERSConventions.IERS_2010);

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceMatrix, initialDate, initialInertialFrame,
                                           OrbitType.CARTESIAN, PositionAngleType.MEAN);
        // When
        final FieldMatrix<Binary64> convertedCovarianceMatrixInMOD =
                stateCovariance.changeCovarianceFrame(initialOrbit, outputFrame).getMatrix();

        // Then
        final FieldMatrix<Binary64> expectedCovarianceMatrixInMOD = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(9.999939e-001), new Binary64(9.999070e-003), new Binary64(9.997861e-003),
                  new Binary64(9.993866e-005), new Binary64(9.999070e-005), new Binary64(9.997861e-005) },
                { new Binary64(9.999070e-003), new Binary64(1.000004e+000), new Binary64(1.000307e-002),
                  new Binary64(9.999070e-005), new Binary64(1.000428e-004), new Binary64(1.000307e-004) },
                { new Binary64(9.997861e-003), new Binary64(1.000307e-002), new Binary64(1.000002e+000),
                  new Binary64(9.997861e-005), new Binary64(1.000307e-004), new Binary64(1.000186e-004) },
                { new Binary64(9.993866e-005), new Binary64(9.999070e-005), new Binary64(9.997861e-005),
                  new Binary64(9.993866e-007), new Binary64(9.999070e-007), new Binary64(9.997861e-007) },
                { new Binary64(9.999070e-005), new Binary64(1.000428e-004), new Binary64(1.000307e-004),
                  new Binary64(9.999070e-007), new Binary64(1.000428e-006), new Binary64(1.000307e-006) },
                { new Binary64(9.997861e-005), new Binary64(1.000307e-004), new Binary64(1.000186e-004),
                  new Binary64(9.997861e-007), new Binary64(1.000307e-006), new Binary64(1.000186e-006) },
                });

        compareCovariance(expectedCovarianceMatrixInMOD, convertedCovarianceMatrixInMOD, DEFAULT_VALLADO_THRESHOLD);

    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations from
     * David A. Vallado.
     * <p>
     * More specifically, we're using the initial covariance matrix from p.14 and compare the computed result with the
     * cartesian covariance in NTW from p.19.
     * <p>
     * In this case, the same transformation as the one in Vallado's paper is applied (only rotation) so the same values are
     * expected.
     * <p>
     */
    @Test
    @DisplayName("Test covariance conversion Vallado test case : ECI cartesian to NTW ( considered inertial)")
    public void should_return_Vallado_NTW_covariance_matrix_from_ECI() {

        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        final FieldAbsoluteDate<Binary64>  initialDate          = getValladoInitialDate(field);
        final FieldPVCoordinates<Binary64> initialPV            = getValladoInitialPV(field);
        final Frame                        initialInertialFrame = FramesFactory.getGCRF();
        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, getValladoMu(field));

        final FieldMatrix<Binary64> initialCovarianceMatrix = getValladoInitialCovarianceMatrix();

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceMatrix, initialDate, initialInertialFrame,
                                           OrbitType.CARTESIAN, PositionAngleType.MEAN);
        // When
        final FieldMatrix<Binary64> convertedCovarianceMatrixInNTW =
                stateCovariance.changeCovarianceFrame(initialOrbit, LOFType.NTW_INERTIAL).getMatrix();

        // Then
        final FieldMatrix<Binary64> expectedCovarianceMatrixInNTW = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(9.918792e-001), new Binary64(6.679546e-003), new Binary64(-2.868345e-003),
                  new Binary64(1.879167e-005), new Binary64(6.679546e-005), new Binary64(-2.868345e-005) },
                { new Binary64(6.679546e-003), new Binary64(1.013743e+000), new Binary64(-1.019560e-002),
                  new Binary64(6.679546e-005), new Binary64(2.374262e-004), new Binary64(-1.019560e-004) },
                { new Binary64(-2.868345e-003), new Binary64(-1.019560e-002), new Binary64(9.943782e-001),
                  new Binary64(-2.868345e-005), new Binary64(-1.019560e-004), new Binary64(4.378217e-005) },
                { new Binary64(1.879167e-005), new Binary64(6.679546e-005), new Binary64(-2.868345e-005),
                  new Binary64(1.879167e-007), new Binary64(6.679546e-007), new Binary64(-2.868345e-007) },
                { new Binary64(6.679546e-005), new Binary64(2.374262e-004), new Binary64(-1.019560e-004),
                  new Binary64(6.679546e-007), new Binary64(2.374262e-006), new Binary64(-1.019560e-006) },
                { new Binary64(-2.868345e-005), new Binary64(-1.019560e-004), new Binary64(4.378217e-005),
                  new Binary64(-2.868345e-007), new Binary64(-1.019560e-006), new Binary64(4.378217e-007) } });

        compareCovariance(expectedCovarianceMatrixInNTW, convertedCovarianceMatrixInNTW, DEFAULT_VALLADO_THRESHOLD);

    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations from
     * David A. Vallado.
     * <p>
     * More specifically, we're using the initial covariance matrix from p.14 and compare the computed result with the
     * cartesian covariance in NTW from p.19.
     * <p>
     * In this case, Orekit applies the full frame transformation while Vallado's paper only take into account the rotation
     * part. Therefore, some values are different with respect to the reference ones in the paper.
     * <p>
     */
    @Test
    @DisplayName("Test covariance conversion Vallado test case : ECI cartesian to NTW (non inertial)")
    public void should_return_Vallado_NTW_non_inertial_covariance_matrix_from_ECI() {

        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        final FieldAbsoluteDate<Binary64>  initialDate          = getValladoInitialDate(field);
        final FieldPVCoordinates<Binary64> initialPV            = getValladoInitialPV(field);
        final Frame                        initialInertialFrame = FramesFactory.getGCRF();
        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, getValladoMu(field));

        final FieldMatrix<Binary64> initialCovarianceMatrix = getValladoInitialCovarianceMatrix();

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceMatrix, initialDate, initialInertialFrame,
                                           OrbitType.CARTESIAN, PositionAngleType.MEAN);

        // When
        final FieldMatrix<Binary64> convertedCovarianceMatrixInNTW =
                stateCovariance.changeCovarianceFrame(initialOrbit, LOFType.NTW).getMatrix();

        // Then
        final FieldMatrix<Binary64> expectedCovarianceMatrixInNTW = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(9.918792e-01), new Binary64(6.679546e-03), new Binary64(-2.868345e-03),
                  new Binary64(2.621921e-05), new Binary64(-1.036158e-03), new Binary64(-2.868345e-05) },
                { new Binary64(6.679546e-03), new Binary64(1.013743e+00), new Binary64(-1.019560e-02),
                  new Binary64(1.194061e-03), new Binary64(2.299986e-04), new Binary64(-1.019560e-04) },
                { new Binary64(-2.868345e-03), new Binary64(-1.019560e-02), new Binary64(9.943782e-01),
                  new Binary64(-4.002079e-05), new Binary64(-9.876648e-05), new Binary64(4.378217e-05) },
                { new Binary64(2.621921e-05), new Binary64(1.194061e-03), new Binary64(-4.002079e-05),
                  new Binary64(1.589968e-06), new Binary64(9.028133e-07), new Binary64(-4.002079e-07) },
                { new Binary64(-1.036158e-03), new Binary64(2.299986e-04), new Binary64(-9.876648e-05),
                  new Binary64(9.028133e-07), new Binary64(3.452177e-06), new Binary64(-9.876648e-07) },
                { new Binary64(-2.868345e-05), new Binary64(-1.019560e-04), new Binary64(4.378217e-05),
                  new Binary64(-4.002079e-07), new Binary64(-9.876648e-07), new Binary64(4.378217e-07) },
                });

        compareCovariance(expectedCovarianceMatrixInNTW, convertedCovarianceMatrixInNTW, DEFAULT_VALLADO_THRESHOLD);

    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations from
     * David A. Vallado.
     * <p>
     * More specifically, we're using the initial covariance matrix from p.14 and compare the computed result with the
     * cartesian covariance in ECEF from p.18.
     * </p>
     * <p>
     * <b>BEWARE: It has been found that the earth rotation in this Vallado's case is given 1 million times slower than
     * the expected value. This has been corrected and the expected covariance matrix is now the covariance matrix computed
     * by Orekit given the similarities with Vallado's results. In addition, the small differences potentially come from the
     * custom EOP that Vallado uses. Hence, this test can be considered as a <u>non regression test</u>.</b>
     * </p>
     */
    @Test
    @DisplayName("Test covariance conversion Vallado test case : ECI cartesian to PEF")
    public void should_return_Vallado_PEF_covariance_matrix_from_ECI() {

        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        // Initialize orbit
        final FieldAbsoluteDate<Binary64>  initialDate          = getValladoInitialDate(field);
        final FieldPVCoordinates<Binary64> initialPV            = getValladoInitialPV(field);
        final Frame                        initialInertialFrame = FramesFactory.getGCRF();

        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, getValladoMu(field));

        // Initialize input covariance matrix
        final FieldMatrix<Binary64> initialCovarianceMatrix = getValladoInitialCovarianceMatrix();

        // Initialize output frame
        final Frame outputFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceMatrix, initialDate, initialInertialFrame,
                                           OrbitType.CARTESIAN,
                                           PositionAngleType.MEAN);
        // When
        final FieldMatrix<Binary64> convertedCovarianceMatrixInITRF =
                stateCovariance.changeCovarianceFrame(initialOrbit, outputFrame).getMatrix();

        // Then
        final FieldMatrix<Binary64> expectedCovarianceMatrixInITRF = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(9.9340005761276870e-01), new Binary64(7.5124999798868530e-03),
                  new Binary64(5.8312675007359050e-03), new Binary64(3.454839626105493e-05),
                  new Binary64(2.6851237046859065e-06), new Binary64(5.8312677693153940e-05) },
                { new Binary64(7.5124999798868025e-03), new Binary64(1.0065990293034541e+00),
                  new Binary64(1.2884310200351924e-02), new Binary64(1.4852736004690686e-04),
                  new Binary64(1.6544247282904867e-04), new Binary64(1.2884310644320954e-04) },
                { new Binary64(5.8312675007359040e-03), new Binary64(1.2884310200351924e-02),
                  new Binary64(1.0000009130837746e+00), new Binary64(5.9252211072590390e-05),
                  new Binary64(1.2841787487219444e-04), new Binary64(1.0000913090989617e-04) },
                { new Binary64(3.4548396261054936e-05), new Binary64(1.4852736004690686e-04),
                  new Binary64(5.9252211072590403e-05), new Binary64(3.5631474857130520e-07),
                  new Binary64(7.6083489184819870e-07), new Binary64(5.9252213790760030e-07) },
                { new Binary64(2.6851237046859150e-06), new Binary64(1.6544247282904864e-04),
                  new Binary64(1.2841787487219447e-04), new Binary64(7.6083489184819880e-07),
                  new Binary64(1.6542289254142709e-06), new Binary64(1.2841787929229964e-06) },
                { new Binary64(5.8312677693153934e-05), new Binary64(1.2884310644320950e-04),
                  new Binary64(1.0000913090989616e-04), new Binary64(5.9252213790760020e-07),
                  new Binary64(1.2841787929229960e-06), new Binary64(1.0000913098203875e-06) }
        });

        compareCovariance(expectedCovarianceMatrixInITRF, convertedCovarianceMatrixInITRF, 1e-20);

    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations from
     * David A. Vallado.
     * <p>
     * More specifically, we're using the initial covariance matrix from p.14 and compare the computed result with the
     * cartesian covariance in RSW from p.19.
     * <p>
     * In this case, the same transformation as the one in Vallado's paper is applied (only rotation) so the same values are
     * expected.
     * <p>
     * Note that the followings local orbital frame are equivalent RSW=RTN=QSW.
     */
    @Test
    @DisplayName("Test covariance conversion Vallado test case : ECI cartesian to RTN")
    public void should_return_Vallado_RSW_covariance_matrix_from_ECI() {

        // Initialize Orekit
        Utils.setDataRoot("regular-data");

        // Given
        final FieldAbsoluteDate<Binary64>  initialDate          = getValladoInitialDate(field);
        final FieldPVCoordinates<Binary64> initialPV            = getValladoInitialPV(field);
        final Frame                        initialInertialFrame = FramesFactory.getGCRF();
        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, getValladoMu(field));

        final FieldMatrix<Binary64> initialCovarianceMatrix = getValladoInitialCovarianceMatrix();

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceMatrix, initialDate, initialInertialFrame,
                                           OrbitType.CARTESIAN,
                                           PositionAngleType.MEAN);
        // When
        final FieldMatrix<Binary64> convertedCovarianceMatrixInRTN =
                stateCovariance.changeCovarianceFrame(initialOrbit, LOFType.QSW_INERTIAL).getMatrix();

        // Then
        final FieldMatrix<Binary64> expectedCovarianceMatrixInRTN = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(9.918921e-001), new Binary64(6.700644e-003), new Binary64(-2.878187e-003),
                  new Binary64(1.892086e-005), new Binary64(6.700644e-005), new Binary64(-2.878187e-005) },
                { new Binary64(6.700644e-003), new Binary64(1.013730e+000), new Binary64(-1.019283e-002),
                  new Binary64(6.700644e-005), new Binary64(2.372970e-004), new Binary64(-1.019283e-004) },
                { new Binary64(-2.878187e-003), new Binary64(-1.019283e-002), new Binary64(9.943782e-001),
                  new Binary64(-2.878187e-005), new Binary64(-1.019283e-004), new Binary64(4.378217e-005) },
                { new Binary64(1.892086e-005), new Binary64(6.700644e-005), new Binary64(-2.878187e-005),
                  new Binary64(1.892086e-007), new Binary64(6.700644e-007), new Binary64(-2.878187e-007) },
                { new Binary64(6.700644e-005), new Binary64(2.372970e-004), new Binary64(-1.019283e-004),
                  new Binary64(6.700644e-007), new Binary64(2.372970e-006), new Binary64(-1.019283e-006) },
                { new Binary64(-2.878187e-005), new Binary64(-1.019283e-004), new Binary64(4.378217e-005),
                  new Binary64(-2.878187e-007), new Binary64(-1.019283e-006), new Binary64(4.378217e-007) } });

        compareCovariance(expectedCovarianceMatrixInRTN, convertedCovarianceMatrixInRTN, DEFAULT_VALLADO_THRESHOLD);

    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations from
     * David A. Vallado.
     * <p>
     * More specifically, we're using the initial covariance matrix from p.14 and compare the computed result with the
     * cartesian covariance in RSW from p.19.
     * <p>
     * In this case, Orekit applies the full frame transformation while Vallado's paper only take into account the rotation
     * part. Therefore, some values are different with respect to the reference ones in the paper.
     * <p>
     * Note that the followings local orbital frame are equivalent RSW=RTN=QSW.
     */
    @Test
    @DisplayName("Test covariance conversion Vallado test case : ECI cartesian to RTN")
    public void should_return_Vallado_RSW_non_inertial_covariance_matrix_from_ECI() {

        // Initialize Orekit
        Utils.setDataRoot("regular-data");

        // Given
        final FieldAbsoluteDate<Binary64>  initialDate          = getValladoInitialDate(field);
        final FieldPVCoordinates<Binary64> initialPV            = getValladoInitialPV(field);
        final Frame                        initialInertialFrame = FramesFactory.getGCRF();
        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, getValladoMu(field));

        final FieldMatrix<Binary64> initialCovarianceMatrix = getValladoInitialCovarianceMatrix();

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceMatrix, initialDate, initialInertialFrame,
                                           OrbitType.CARTESIAN,
                                           PositionAngleType.MEAN);
        // When
        final FieldMatrix<Binary64> convertedCovarianceMatrixInRTN =
                stateCovariance.changeCovarianceFrame(initialOrbit, LOFType.QSW).getMatrix();

        // Then
        final FieldMatrix<Binary64> expectedCovarianceMatrixInRTN = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(9.918921e-01), new Binary64(6.700644e-03), new Binary64(-2.878187e-03),
                  new Binary64(2.637186e-05), new Binary64(-1.035961e-03), new Binary64(-2.878187e-05) },
                { new Binary64(6.700644e-03), new Binary64(1.013730e+00), new Binary64(-1.019283e-02),
                  new Binary64(1.194257e-03), new Binary64(2.298460e-04), new Binary64(-1.019283e-04) },
                { new Binary64(-2.878187e-03), new Binary64(-1.019283e-02), new Binary64(9.943782e-01),
                  new Binary64(-4.011613e-05), new Binary64(-9.872780e-05), new Binary64(4.378217e-05) },
                { new Binary64(2.637186e-05), new Binary64(1.194257e-03), new Binary64(-4.011613e-05),
                  new Binary64(1.591713e-06), new Binary64(9.046096e-07), new Binary64(-4.011613e-07) },
                { new Binary64(-1.035961e-03), new Binary64(2.298460e-04), new Binary64(-9.872780e-05),
                  new Binary64(9.046096e-07), new Binary64(3.450431e-06), new Binary64(-9.872780e-07) },
                { new Binary64(-2.878187e-05), new Binary64(-1.019283e-04), new Binary64(4.378217e-05),
                  new Binary64(-4.011613e-07), new Binary64(-9.872780e-07), new Binary64(4.378217e-07) }
        });

        compareCovariance(expectedCovarianceMatrixInRTN, convertedCovarianceMatrixInRTN, DEFAULT_VALLADO_THRESHOLD);

    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations from
     * David A. Vallado.
     * <p>
     * More specifically, we're using the initial covariance matrix from p.14 and compare the computed result with the
     * cartesian covariance in PEF from p.18.
     */
    @Test
    @DisplayName("Test covariance conversion Vallado test case : PEF cartesian to ECI")
    public void should_return_Vallado_ECI_covariance_matrix_from_PEF() {

        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        final FieldAbsoluteDate<Binary64>  initialDate          = getValladoInitialDate(field);
        final FieldPVCoordinates<Binary64> initialPV            = getValladoInitialPV(field);
        final Frame                        initialInertialFrame = FramesFactory.getGCRF();
        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, getValladoMu(field));

        final FieldMatrix<Binary64> initialCovarianceMatrixInPEF = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(9.9340005761276870e-01), new Binary64(7.5124999798868530e-03),
                  new Binary64(5.8312675007359050e-03), new Binary64(3.4548396261054936e-05),
                  new Binary64(2.6851237046859200e-06), new Binary64(5.8312677693153940e-05) },
                { new Binary64(7.5124999798868025e-03), new Binary64(1.0065990293034541e+00),
                  new Binary64(1.2884310200351924e-02), new Binary64(1.4852736004690684e-04),
                  new Binary64(1.6544247282904867e-04), new Binary64(1.2884310644320954e-04) },
                { new Binary64(5.8312675007359040e-03), new Binary64(1.2884310200351924e-02),
                  new Binary64(1.0000009130837746e+00), new Binary64(5.9252211072590390e-05),
                  new Binary64(1.2841787487219444e-04), new Binary64(1.0000913090989617e-04) },
                { new Binary64(3.4548396261054936e-05), new Binary64(1.4852736004690686e-04),
                  new Binary64(5.9252211072590403e-05), new Binary64(3.5631474857130520e-07),
                  new Binary64(7.6083489184819870e-07), new Binary64(5.9252213790760030e-07) },
                { new Binary64(2.6851237046859150e-06), new Binary64(1.6544247282904864e-04),
                  new Binary64(1.2841787487219447e-04), new Binary64(7.6083489184819880e-07),
                  new Binary64(1.6542289254142709e-06), new Binary64(1.2841787929229964e-06) },
                { new Binary64(5.8312677693153934e-05), new Binary64(1.2884310644320950e-04),
                  new Binary64(1.0000913090989616e-04), new Binary64(5.9252213790760020e-07),
                  new Binary64(1.2841787929229960e-06), new Binary64(1.0000913098203875e-06) }
        });

        final Frame inputFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, false);

        // State covariance
        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceMatrixInPEF, initialDate, inputFrame, OrbitType.CARTESIAN,
                                           PositionAngleType.MEAN);

        // When
        final FieldMatrix<Binary64> convertedCovarianceMatrixInECI =
                stateCovariance.changeCovarianceFrame(initialOrbit, initialInertialFrame).getMatrix();

        // Then
        final FieldMatrix<Binary64> expectedCovarianceMatrixInECI = getValladoInitialCovarianceMatrix();

        compareCovariance(expectedCovarianceMatrixInECI, convertedCovarianceMatrixInECI, 1e-7);

    }

    @Test
    @DisplayName("Test covariance conversion from RTN local orbital frame to inertial frame")
    public void should_rotate_covariance_matrix_by_minus_ninety_degrees() {

        // Given
        final FieldAbsoluteDate<Binary64> initialDate          = new FieldAbsoluteDate<>(field);
        final Frame                       initialInertialFrame = FramesFactory.getGCRF();
        final Binary64                    mu                   = field.getOne().multiply(398600e9);

        final FieldPVCoordinates<Binary64> initialPV = new FieldPVCoordinates<>(field, new PVCoordinates(
                new Vector3D(0, 6778000, 0),
                new Vector3D(-7668.63, 0, 0)));

        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, mu);

        final FieldMatrix<Binary64> initialCovarianceInRTN = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(1), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(1), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(-1e-5) },
                { new Binary64(0), new Binary64(0), new Binary64(1), new Binary64(0), new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(1e-3), new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(1e-3), new Binary64(0) },
                { new Binary64(0), new Binary64(-1e-5), new Binary64(0), new Binary64(0), new Binary64(0),
                  new Binary64(1e-3) }
        });

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceInRTN, initialDate, LOFType.QSW);

        // When
        final FieldMatrix<Binary64> convertedCovarianceMatrixInInertialFrame =
                stateCovariance.changeCovarianceFrame(initialOrbit, initialInertialFrame).getMatrix();

        // Then

        // Expected covariance matrix obtained by rotation initial covariance matrix by -90 degrees
        final FieldMatrix<Binary64> expectedMatrixInInertialFrame = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(1.000000e+00), new Binary64(0.000000e+00), new Binary64(0.000000e+00),
                  new Binary64(0.000000e+00), new Binary64(1.131400e-03), new Binary64(1.000000e-05) },
                { new Binary64(0.000000e+00), new Binary64(1.000000e+00), new Binary64(0.000000e+00),
                  new Binary64(-1.131400e-03), new Binary64(0.000000e+00), new Binary64(0.000000e+00) },
                { new Binary64(0.000000e+00), new Binary64(0.000000e+00), new Binary64(1.000000e+00),
                  new Binary64(0.000000e+00), new Binary64(0.000000e+00), new Binary64(0.000000e+00) },
                { new Binary64(0.000000e+00), new Binary64(-1.131400e-03), new Binary64(0.000000e+00),
                  new Binary64(1.001280e-03), new Binary64(0.000000e+00), new Binary64(0.000000e+00) },
                { new Binary64(1.131400e-03), new Binary64(0.000000e+00), new Binary64(0.000000e+00),
                  new Binary64(0.000000e+00), new Binary64(1.001280e-03), new Binary64(1.131400e-08) },
                { new Binary64(1.000000e-05), new Binary64(0.000000e+00), new Binary64(0.000000e+00),
                  new Binary64(0.000000e+00), new Binary64(1.131400e-08), new Binary64(1.000000e-03) },
                });

        compareCovariance(expectedMatrixInInertialFrame, convertedCovarianceMatrixInInertialFrame,
                          DEFAULT_VALLADO_THRESHOLD);

    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations from
     * David A. Vallado.
     * <p>
     * More specifically, we're using the initial NTW covariance matrix from p.19 and compare the computed result with the
     * cartesian covariance in RSW from the same page.
     * </p>
     */
    @Test
    @DisplayName("Test covariance conversion from Vallado test case NTW to RSW")
    public void should_convert_Vallado_NTW_to_RSW() {

        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        final FieldAbsoluteDate<Binary64>  initialDate          = getValladoInitialDate(field);
        final FieldPVCoordinates<Binary64> initialPV            = getValladoInitialPV(field);
        final Frame                        initialInertialFrame = FramesFactory.getGCRF();
        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, getValladoMu(field));

        final FieldMatrix<Binary64> initialCovarianceMatrixInNTW = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(9.918792e-001), new Binary64(6.679546e-003), new Binary64(-2.868345e-003),
                  new Binary64(1.879167e-005), new Binary64(6.679546e-005), new Binary64(-2.868345e-005) },
                { new Binary64(6.679546e-003), new Binary64(1.013743e+000), new Binary64(-1.019560e-002),
                  new Binary64(6.679546e-005), new Binary64(2.374262e-004), new Binary64(-1.019560e-004) },
                { new Binary64(-2.868345e-003), new Binary64(-1.019560e-002), new Binary64(9.943782e-001),
                  new Binary64(-2.868345e-005), new Binary64(-1.019560e-004), new Binary64(4.378217e-005) },
                { new Binary64(1.879167e-005), new Binary64(6.679546e-005), new Binary64(-2.868345e-005),
                  new Binary64(1.879167e-007), new Binary64(6.679546e-007), new Binary64(-2.868345e-007) },
                { new Binary64(6.679546e-005), new Binary64(2.374262e-004), new Binary64(-1.019560e-004),
                  new Binary64(6.679546e-007), new Binary64(2.374262e-006), new Binary64(-1.019560e-006) },
                { new Binary64(-2.868345e-005), new Binary64(-1.019560e-004), new Binary64(4.378217e-005),
                  new Binary64(-2.868345e-007), new Binary64(-1.019560e-006), new Binary64(4.378217e-007) }
        });

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceMatrixInNTW, initialDate, LOFType.NTW);

        // When
        final FieldMatrix<Binary64> convertedCovarianceMatrixInRTN =
                stateCovariance.changeCovarianceFrame(initialOrbit, LOFType.QSW).getMatrix();

        // Then
        final FieldMatrix<Binary64> expectedCovarianceMatrixInRTN = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(9.918921e-001), new Binary64(6.700644e-003), new Binary64(-2.878187e-003),
                  new Binary64(1.892086e-005), new Binary64(6.700644e-005), new Binary64(-2.878187e-005) },
                { new Binary64(6.700644e-003), new Binary64(1.013730e+000), new Binary64(-1.019283e-002),
                  new Binary64(6.700644e-005), new Binary64(2.372970e-004), new Binary64(-1.019283e-004) },
                { new Binary64(-2.878187e-003), new Binary64(-1.019283e-002), new Binary64(9.943782e-001),
                  new Binary64(-2.878187e-005), new Binary64(-1.019283e-004), new Binary64(4.378217e-005) },
                { new Binary64(1.892086e-005), new Binary64(6.700644e-005), new Binary64(-2.878187e-005),
                  new Binary64(1.892086e-007), new Binary64(6.700644e-007), new Binary64(-2.878187e-007) },
                { new Binary64(6.700644e-005), new Binary64(2.372970e-004), new Binary64(-1.019283e-004),
                  new Binary64(6.700644e-007), new Binary64(2.372970e-006), new Binary64(-1.019283e-006) },
                { new Binary64(-2.878187e-005), new Binary64(-1.019283e-004), new Binary64(4.378217e-005),
                  new Binary64(-2.878187e-007), new Binary64(-1.019283e-006), new Binary64(4.378217e-007) }
        });

        compareCovariance(expectedCovarianceMatrixInRTN, convertedCovarianceMatrixInRTN, DEFAULT_VALLADO_THRESHOLD);

    }

    @Test
    @DisplayName("Test covariance conversion from inertial frame to RTN local orbital frame")
    public void should_rotate_covariance_matrix_by_ninety_degrees() {

        // Given
        final FieldAbsoluteDate<Binary64> initialDate          = new FieldAbsoluteDate<>(field);
        final Frame                       initialInertialFrame = FramesFactory.getGCRF();
        final Binary64                    mu                   = field.getOne().multiply(398600e9);

        final FieldPVCoordinates<Binary64> initialPV = new FieldPVCoordinates<>(field, new PVCoordinates(
                new Vector3D(0, 6778000, 0),
                new Vector3D(-7668.63, 0, 0)));

        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, mu);

        final FieldMatrix<Binary64> initialCovarianceInInertialFrame = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(1), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(1e-5) },
                { new Binary64(0), new Binary64(1), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(1), new Binary64(0), new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(1e-3), new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(1e-3), new Binary64(0) },
                { new Binary64(1e-5), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                  new Binary64(1e-3) }
        });

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceInInertialFrame, initialDate, initialInertialFrame,
                                           OrbitType.CARTESIAN, PositionAngleType.MEAN);

        // When
        final FieldMatrix<Binary64> convertedCovarianceMatrixInRTN =
                stateCovariance.changeCovarianceFrame(initialOrbit, LOFType.QSW).getMatrix();

        // Then
        // Expected covariance matrix obtained by rotation initial covariance matrix by 90 degrees
        final FieldMatrix<Binary64> expectedMatrixInRTN = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(1.000000e+00), new Binary64(0.000000e+00), new Binary64(0.000000e+00),
                  new Binary64(0.000000e+00), new Binary64(-1.131400e-03), new Binary64(0.000000e+00) },
                { new Binary64(0.000000e+00), new Binary64(1.000000e+00), new Binary64(0.000000e+00),
                  new Binary64(1.131400e-03), new Binary64(0.000000e+00), new Binary64(-1.000000e-05) },
                { new Binary64(0.000000e+00), new Binary64(0.000000e+00), new Binary64(1.000000e+00),
                  new Binary64(0.000000e+00), new Binary64(0.000000e+00), new Binary64(0.000000e+00) },
                { new Binary64(0.000000e+00), new Binary64(1.131400e-03), new Binary64(0.000000e+00),
                  new Binary64(1.001280e-03), new Binary64(0.000000e+00), new Binary64(-1.131400e-08) },
                { new Binary64(-1.131400e-03), new Binary64(0.000000e+00), new Binary64(0.000000e+00),
                  new Binary64(0.000000e+00), new Binary64(1.001280e-03), new Binary64(0.000000e+00) },
                { new Binary64(0.000000e+00), new Binary64(-1.000000e-05), new Binary64(0.000000e+00),
                  new Binary64(-1.131400e-08), new Binary64(0.000000e+00), new Binary64(1.000000e-03) },
                });

        compareCovariance(expectedMatrixInRTN, convertedCovarianceMatrixInRTN, DEFAULT_VALLADO_THRESHOLD);
    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations from
     * David A. Vallado.
     * <p>
     * More specifically, we're using the initial covariance matrix from p.14 as a reference to test multiple conversions.
     * <p>
     * This test aims to verify the numerical precision after various conversions and serves as a non regression test for
     * future updates.
     * <p>
     * Also, note that the conversion from the RTN to TEME tests the fact that the orbit is initially expressed in GCRF while
     * we want the covariance expressed in TEME. Hence, it tests that the rotation from RTN to TEME needs to be obtained by
     * expressing the orbit FieldPVCoordinatesin the TEME frame (hence the use of orbit.gtPVCoordinates(frameOut) ,see
     * relevant changeCovarianceFrame method).
     */
    @Test
    @DisplayName("Test custom covariance conversion Vallado test case : GCRF -> TEME -> IRTF -> NTW -> RTN -> ITRF -> GCRF")
    public void should_return_initial_covariance_after_multiple_conversion() {

        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        final FieldAbsoluteDate<Binary64>  initialDate          = getValladoInitialDate(field);
        final FieldPVCoordinates<Binary64> initialPV            = getValladoInitialPV(field);
        final Frame                        initialInertialFrame = FramesFactory.getGCRF();
        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, getValladoMu(field));

        final FieldMatrix<Binary64> initialCovarianceMatrixInGCRF = getValladoInitialCovarianceMatrix();

        final Frame teme = FramesFactory.getTEME();

        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, false);

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceMatrixInGCRF, initialDate, initialInertialFrame,
                                           OrbitType.CARTESIAN, PositionAngleType.MEAN);

        // When
        // GCRF -> TEME
        final FieldStateCovariance<Binary64> convertedCovarianceMatrixInTEME =
                stateCovariance.changeCovarianceFrame(initialOrbit, teme);

        // TEME -> ITRF
        final FieldStateCovariance<Binary64> convertedCovarianceMatrixInITRF =
                convertedCovarianceMatrixInTEME.changeCovarianceFrame(initialOrbit, itrf);

        // ITRF -> NTW
        final FieldStateCovariance<Binary64> convertedCovarianceMatrixInNTW =
                convertedCovarianceMatrixInITRF.changeCovarianceFrame(initialOrbit, LOFType.NTW);

        // NTW -> RTN
        final FieldStateCovariance<Binary64> convertedCovarianceMatrixInRTN =
                convertedCovarianceMatrixInNTW.changeCovarianceFrame(initialOrbit, LOFType.QSW);

        // RTN -> ITRF
        final FieldStateCovariance<Binary64> convertedCovarianceMatrixBackInITRF =
                convertedCovarianceMatrixInRTN.changeCovarianceFrame(initialOrbit, itrf);

        // ITRF -> TEME
        final FieldStateCovariance<Binary64> convertedCovarianceMatrixBackInTEME =
                convertedCovarianceMatrixBackInITRF.changeCovarianceFrame(initialOrbit, teme);

        // TEME -> GCRF
        final FieldStateCovariance<Binary64> convertedCovarianceMatrixInGCRF =
                convertedCovarianceMatrixBackInTEME.changeCovarianceFrame(initialOrbit, initialInertialFrame);

        // Then
        compareCovariance(initialCovarianceMatrixInGCRF, convertedCovarianceMatrixInGCRF.getMatrix(), 1e-12);

    }

    /**
     * The goal of this test is to check the shiftedBy method of {@link StateCovariance} by creating one state covariance
     * expressed in 3 different ways (inertial Equinoctial, LOF cartesian and non inertial cartesian) -> shift them ->
     * reconvert them back to the same initial frame & type -> compare them with expected, manually computed covariance
     * matrix.
     */
    @Test
    @DisplayName("Test shiftedBy method of StateCovariance")
    public void should_return_expected_shifted_state_covariance() {

        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        final FieldAbsoluteDate<Binary64>  initialDate      = new FieldAbsoluteDate<>(field);
        final Frame                        inertialFrame    = FramesFactory.getGCRF();
        final Frame                        nonInertialFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final FieldPVCoordinates<Binary64> pv               = getValladoInitialPV(field);
        final Binary64                     mu               = field.getOne().multiply(398600e9);
        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(pv, inertialFrame, initialDate, mu);

        final Binary64 timeShift = field.getOne().multiply(300); // In s

        // Initializing initial covariance matrix common to all
        final FieldStateCovariance<Binary64> initialCovarianceInCartesian =
                new FieldStateCovariance<>(getValladoInitialCovarianceMatrix(), initialDate, inertialFrame,
                                           OrbitType.CARTESIAN, PositionAngleType.MEAN);

        final FieldStateCovariance<Binary64> covarianceInEquinoctial =
                initialCovarianceInCartesian.changeCovarianceType(initialOrbit, OrbitType.EQUINOCTIAL,
                                                                  PositionAngleType.MEAN);

        final FieldStateCovariance<Binary64> covarianceInCartesianInLOF =
                initialCovarianceInCartesian.changeCovarianceFrame(initialOrbit, LOFType.QSW);

        final FieldStateCovariance<Binary64> covarianceInCartesianInNonInertial =
                initialCovarianceInCartesian.changeCovarianceFrame(initialOrbit, nonInertialFrame);

        // When
        final FieldStateCovariance<Binary64> shiftedCovarianceInEquinoctial =
                covarianceInEquinoctial.shiftedBy(field, initialOrbit, timeShift);
        final FieldMatrix<Binary64> shiftedCovarianceInEquinoctialBackToInitial =
                shiftedCovarianceInEquinoctial.changeCovarianceType(initialOrbit.shiftedBy(timeShift),
                                                                    OrbitType.CARTESIAN, PositionAngleType.MEAN)
                                              .getMatrix();

        final FieldStateCovariance<Binary64> shiftedCovarianceInCartesianInLOF =
                covarianceInCartesianInLOF.shiftedBy(field, initialOrbit, timeShift);
        final FieldMatrix<Binary64> shiftedCovarianceInCartesianInLOFBackToInitial =
                shiftedCovarianceInCartesianInLOF.changeCovarianceFrame(initialOrbit.shiftedBy(timeShift),
                                                                        inertialFrame)
                                                 .getMatrix();

        final FieldStateCovariance<Binary64> shiftedCovarianceInCartesianInNonInertial =
                covarianceInCartesianInNonInertial.shiftedBy(field, initialOrbit, timeShift);
        final FieldMatrix<Binary64> shiftedCovarianceInCartesianInNonInertialBackToInitial =
                shiftedCovarianceInCartesianInNonInertial.changeCovarianceFrame(initialOrbit.shiftedBy(timeShift),
                                                                                inertialFrame)
                                                         .getMatrix();

        // Then
        // Compute expected covariance
        final FieldMatrix<Binary64> stm          = MatrixUtils.createFieldIdentityMatrix(field, 6);
        final Binary64              sma          = initialOrbit.getA();
        final Binary64              contribution = mu.divide(sma.pow(5)).sqrt().multiply(timeShift).multiply(-1.5);
        stm.setEntry(5, 0, contribution);

        final FieldStateCovariance<Binary64> initialCovarianceInKeplerian =
                initialCovarianceInCartesian.changeCovarianceType(initialOrbit, OrbitType.KEPLERIAN,
                                                                  PositionAngleType.MEAN);
        final FieldMatrix<Binary64> referenceCovarianceMatrixInKeplerian =
                stm.multiply(initialCovarianceInKeplerian.getMatrix().multiplyTransposed(stm));

        final FieldMatrix<Binary64> referenceCovarianceMatrixInCartesian =
                new FieldStateCovariance<>(referenceCovarianceMatrixInKeplerian, initialDate.shiftedBy(timeShift),
                                           inertialFrame, OrbitType.KEPLERIAN, PositionAngleType.MEAN).changeCovarianceType(
                        initialOrbit.shiftedBy(timeShift), OrbitType.CARTESIAN, PositionAngleType.MEAN).getMatrix();

        // Compare with results
        compareCovariance(referenceCovarianceMatrixInCartesian, shiftedCovarianceInEquinoctialBackToInitial, 1e-7);
        compareCovariance(referenceCovarianceMatrixInCartesian, shiftedCovarianceInCartesianInLOFBackToInitial, 1e-7);
        compareCovariance(referenceCovarianceMatrixInCartesian, shiftedCovarianceInCartesianInNonInertialBackToInitial,
                          1e-7);

    }

    /**
     * This test is based on the following paper : Covariance Transformations for Satellite Flight Dynamics Operations from
     * David A. Vallado.
     * <p>
     * More specifically, we're using the initial NTW covariance matrix from p.19 and compare the computed result with the
     * cartesian covariance in RSW from the same page.
     * </p>
     */
    @Test
    @DisplayName("Test thrown error if input frame is not pseudo-inertial and "
            + "the covariance matrix is not expressed in cartesian elements")
    public void should_return_orekit_exception() {

        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        final FieldAbsoluteDate<Binary64>  initialDate          = getValladoInitialDate(field);
        final FieldPVCoordinates<Binary64> initialPV            = getValladoInitialPV(field);
        final Frame                        initialInertialFrame = FramesFactory.getGCRF();
        final FieldOrbit<Binary64> initialOrbit =
                new FieldCartesianOrbit<>(initialPV, initialInertialFrame, initialDate, getValladoMu(field));

        final FieldMatrix<Binary64> randomCovarianceMatrix = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(9.918792e-001), new Binary64(6.679546e-003), new Binary64(-2.868345e-003),
                  new Binary64(1.879167e-005), new Binary64(6.679546e-005), new Binary64(-2.868345e-005) },
                { new Binary64(6.679546e-003), new Binary64(1.013743e+000), new Binary64(-1.019560e-002),
                  new Binary64(6.679546e-005), new Binary64(2.374262e-004), new Binary64(-1.019560e-004) },
                { new Binary64(-2.868345e-003), new Binary64(-1.019560e-002), new Binary64(9.943782e-001),
                  new Binary64(-2.868345e-005), new Binary64(-1.019560e-004), new Binary64(4.378217e-005) },
                { new Binary64(1.879167e-005), new Binary64(6.679546e-005), new Binary64(-2.868345e-005),
                  new Binary64(1.879167e-007), new Binary64(6.679546e-007), new Binary64(-2.868345e-007) },
                { new Binary64(6.679546e-005), new Binary64(2.374262e-004), new Binary64(-1.019560e-004),
                  new Binary64(6.679546e-007), new Binary64(2.374262e-006), new Binary64(-1.019560e-006) },
                { new Binary64(-2.868345e-005), new Binary64(-1.019560e-004), new Binary64(4.378217e-005),
                  new Binary64(-2.868345e-007), new Binary64(-1.019560e-006), new Binary64(4.378217e-007) } });

        final Frame nonInertialFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        final Frame inertialFrame = FramesFactory.getGCRF();

        // When & Then
        Assertions.assertThrows(OrekitException.class,
                                () -> new FieldStateCovariance<>(randomCovarianceMatrix, initialDate, nonInertialFrame,
                                                                 OrbitType.CIRCULAR,
                                                                 PositionAngleType.MEAN).changeCovarianceFrame(initialOrbit,
                                                                                                           inertialFrame));

        Assertions.assertThrows(OrekitException.class,
                                () -> new FieldStateCovariance<>(randomCovarianceMatrix, initialDate, nonInertialFrame,
                                                                 OrbitType.EQUINOCTIAL,
                                                                 PositionAngleType.MEAN).changeCovarianceFrame(initialOrbit,
                                                                                                           LOFType.QSW));

        Assertions.assertThrows(OrekitException.class,
                                () -> new FieldStateCovariance<>(randomCovarianceMatrix, initialDate, nonInertialFrame,
                                                                 OrbitType.EQUINOCTIAL,
                                                                 PositionAngleType.MEAN).changeCovarianceType(initialOrbit,
                                                                                                          OrbitType.KEPLERIAN,
                                                                                                          PositionAngleType.MEAN));

        Assertions.assertThrows(OrekitException.class,
                                () -> new FieldStateCovariance<>(randomCovarianceMatrix, initialDate,
                                                                 LOFType.QSW).changeCovarianceType(
                                        initialOrbit, OrbitType.KEPLERIAN, PositionAngleType.MEAN));

        Assertions.assertThrows(OrekitException.class,
                                () -> new FieldStateCovariance<>(randomCovarianceMatrix, initialDate, nonInertialFrame,
                                                                 OrbitType.CARTESIAN,
                                                                 PositionAngleType.MEAN).changeCovarianceType(initialOrbit,
                                                                                                          OrbitType.KEPLERIAN,
                                                                                                          PositionAngleType.MEAN));

    }

    private FieldMatrix<Binary64> getValladoInitialCovarianceMatrix() {
        return new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(1), new Binary64(1e-2), new Binary64(1e-2), new Binary64(1e-4), new Binary64(1e-4),
                  new Binary64(1e-4) },
                { new Binary64(1e-2), new Binary64(1), new Binary64(1e-2), new Binary64(1e-4), new Binary64(1e-4),
                  new Binary64(1e-4) },
                { new Binary64(1e-2), new Binary64(1e-2), new Binary64(1), new Binary64(1e-4), new Binary64(1e-4),
                  new Binary64(1e-4) },
                { new Binary64(1e-4), new Binary64(1e-4), new Binary64(1e-4), new Binary64(1e-6), new Binary64(1e-6),
                  new Binary64(1e-6) },
                { new Binary64(1e-4), new Binary64(1e-4), new Binary64(1e-4), new Binary64(1e-6), new Binary64(1e-6),
                  new Binary64(1e-6) },
                { new Binary64(1e-4), new Binary64(1e-4), new Binary64(1e-4), new Binary64(1e-6), new Binary64(1e-6),
                  new Binary64(1e-6) }
        });
    }

    /**
     * Compare two covariance matrices
     *
     * @param reference reference covariance
     * @param computed computed covariance
     * @param threshold threshold for comparison
     */
    private <T extends CalculusFieldElement<T>> void compareCovariance(final FieldMatrix<T> reference,
                                                                       final FieldMatrix<T> computed,
                                                                       final double threshold) {
        for (int row = 0; row < reference.getRowDimension(); row++) {
            for (int column = 0; column < reference.getColumnDimension(); column++) {
                if (reference.getEntry(row, column).getReal() == 0) {
                    Assertions.assertEquals(reference.getEntry(row, column).getReal(),
                                            computed.getEntry(row, column).getReal(),
                                            threshold);
                }
                else {
                    Assertions.assertEquals(reference.getEntry(row, column).getReal(),
                                            computed.getEntry(row, column).getReal(),
                                            FastMath.abs(threshold * reference.getEntry(row, column).getReal()));
                }
            }
        }
    }

    private <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getValladoInitialDate(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, 2000, 12, 15, 16, 58, 50.208, TimeScalesFactory.getUTC());
    }

    private <T extends CalculusFieldElement<T>> FieldPVCoordinates<T> getValladoInitialPV(final Field<T> field) {
        return new FieldPVCoordinates<>(field,
                                        new PVCoordinates(new Vector3D(-605792.21660, -5870229.51108, 3493053.19896),
                                                          new Vector3D(-1568.25429, -3702.34891, -6479.48395)));
    }

    private <T extends CalculusFieldElement<T>> T getValladoMu(final Field<T> field) {
        return field.getOne().multiply(Constants.IERS2010_EARTH_MU);
    }

    @Test
    @DisplayName("Test getters")
    void should_return_mocks() {
        // Given
        // Initialize orekit
        Utils.setDataRoot("regular-data");

        // Given
        @SuppressWarnings("unchecked")
        final FieldAbsoluteDate<Binary64> initialDate = Mockito.mock(FieldAbsoluteDate.class);
        final Frame initialFrame = Mockito.mock(Frame.class);
        @SuppressWarnings("unchecked")
        final FieldMatrix<Binary64> initialCovarianceMatrixInGCRF = Mockito.mock(BlockFieldMatrix.class);

        final FieldStateCovariance<Binary64> stateCovariance =
                new FieldStateCovariance<>(initialCovarianceMatrixInGCRF, initialDate, initialFrame,
                                           OrbitType.CARTESIAN, PositionAngleType.MEAN);

        // When
        final FieldAbsoluteDate<Binary64> gottenDate          = stateCovariance.getDate();
        final Frame                       gottenFrame         = stateCovariance.getFrame();
        final LOF                         gottenLOF           = stateCovariance.getLOF();
        final OrbitType                   gottenOrbitType     = stateCovariance.getOrbitType();
        final PositionAngleType gottenPositionAngleType = stateCovariance.getPositionAngleType();

        // Then
        Assertions.assertEquals(initialDate, gottenDate);
        Assertions.assertEquals(initialFrame, gottenFrame);
        Assertions.assertNull(gottenLOF);
        Assertions.assertEquals(OrbitType.CARTESIAN, gottenOrbitType);
        Assertions.assertEquals(PositionAngleType.MEAN, gottenPositionAngleType);

    }

    @Test
    void testConversionToNonFieldEquivalentLOF() {

        // GIVEN
        final int             dim   = 6;
        final Field<Binary64> field = Binary64Field.getInstance();

        final FieldMatrix<Binary64>       matrix    = MatrixUtils.createFieldMatrix(field, dim, dim);
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(field);
        final LOF                         lofMock   = Mockito.mock(LOF.class);

        final FieldStateCovariance<Binary64> fieldStateCovariance = new FieldStateCovariance<>(matrix, fieldDate, lofMock);

        // WHEN
        final StateCovariance stateCovariance = fieldStateCovariance.toStateCovariance();

        // THEN
        // Assert covariance matrix
        final RealMatrix covarianceMatrix = stateCovariance.getMatrix();
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                Assertions.assertEquals(matrix.getEntry(i, j).getReal(), covarianceMatrix.getEntry(i, j));
            }
        }

        // Assert epoch
        Assertions.assertEquals(fieldDate.getDate().toAbsoluteDate(), stateCovariance.getDate());

        // Assert local orbital frame
        Assertions.assertEquals(lofMock, stateCovariance.getLOF());

    }

    @Test
    void testConversionToNonFieldEquivalentFrame() {

        // GIVEN
        final int             dim   = 6;
        final Field<Binary64> field = Binary64Field.getInstance();

        final FieldMatrix<Binary64>       matrix            = MatrixUtils.createFieldMatrix(field, dim, dim);
        final FieldAbsoluteDate<Binary64> fieldDate         = new FieldAbsoluteDate<>(field);
        final Frame                       frameMock         = Mockito.mock(Frame.class);
        final OrbitType                   orbitType         = OrbitType.CARTESIAN;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;

        final FieldStateCovariance<Binary64> fieldStateCovariance =
                new FieldStateCovariance<>(matrix, fieldDate, frameMock, orbitType, positionAngleType);

        // WHEN
        final StateCovariance stateCovariance = fieldStateCovariance.toStateCovariance();

        // THEN
        // Assert covariance matrix
        final RealMatrix covarianceMatrix = stateCovariance.getMatrix();
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                Assertions.assertEquals(matrix.getEntry(i, j).getReal(), covarianceMatrix.getEntry(i, j));
            }
        }

        // Assert epoch
        Assertions.assertEquals(fieldDate.getDate().toAbsoluteDate(), stateCovariance.getDate());

        // Assert frame
        Assertions.assertEquals(frameMock, stateCovariance.getFrame());

        // Assert orbit type
        Assertions.assertEquals(orbitType, stateCovariance.getOrbitType());

        // Assert position angle type
        Assertions.assertEquals(positionAngleType, stateCovariance.getPositionAngleType());

    }

}