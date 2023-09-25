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
package org.orekit.ssa.collision.shorttermencounter.probability.twod;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.encounter.EncounterLOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.StateCovariance;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.armellinutils.ArmellinStatistics;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import java.io.IOException;

class ShortTermEncounter2DDefinitionTest {

    /**
     * Threshold below which values are considered equal to zero.
     */
    private final double DEFAULTZEROTHRESHOLD = 5e-14;

    @Test
    @DisplayName("Test the combined radius (sum of each collision object sphere equivalent radius)")
    public void testGiveTheSumOfEachCollisionObjectRadius() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();

        // Define the primary collision object
        final Orbit primaryMock = Mockito.mock(Orbit.class);
        Mockito.when(primaryMock.getDate()).thenReturn(timeOfClosestApproach);

        final StateCovariance primaryCovariance = Mockito.mock(StateCovariance.class);

        final double primaryRadius = 5;

        // Define the secondary collision object
        final Orbit secondaryMock = Mockito.mock(Orbit.class);
        Mockito.when(secondaryMock.getDate()).thenReturn(timeOfClosestApproach);

        final StateCovariance secondaryCovariance = Mockito.mock(StateCovariance.class);

        final double secondaryRadius = 3;

        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primaryMock, primaryCovariance,
                                                                                            primaryRadius, secondaryMock,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius);

        // WHEN
        final double combinedRadius = collision.getCombinedRadius();

        // THEN
        Assertions.assertEquals(8, combinedRadius);
    }

    /**
     * Test with a primary collision object on a circular equatorial orbit with a radius of 6778 km with a mu = 398600
     * km^3/s^2. The secondary object is on an intersect course with a circular polar orbit with the same radius as primary.
     * Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * Each of the collision object have an identical diagonal covariance matrix.
     * </p>
     * <p>
     * The goal of this test is to verify if the computed projection matrix from primary inertial to default collision plane
     * is the same as expected (computed by hand).
     * </p>
     */
    @Test
    @DisplayName("Test the projection matrix from primary inertial to the default collision plane")
    public void testReturnProjectionMatrixFromPrimaryInertialToDefaultCollisionPlane() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000, 0, 0),
                                  new Vector3D(0, 7668.631425, 0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final StateCovariance primaryCovariance = Mockito.mock(StateCovariance.class);

        final double primaryRadius = 5;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000 + 1, 0, 0),
                                  new Vector3D(0, 0, 7668.631425)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final StateCovariance secondaryCovariance = Mockito.mock(StateCovariance.class);

        final double secondaryRadius = 5;

        // Collision definition
        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadius, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius);

        // WHEN
        final RealMatrix projectionMatrixFromPrimaryInertialToCollisionPlane =
                collision.computeReferenceInertialToCollisionPlaneProjectionMatrix();

        // THEN
        Assertions.assertEquals(1, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 0), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 1), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 2), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 0), 1e-10);
        Assertions.assertEquals(FastMath.sqrt(2) / 2,
                                projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 1), 1e-7);
        Assertions.assertEquals(FastMath.sqrt(2) / 2,
                                projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 2), 1e-7);
    }

    /**
     * Test with a primary collision object on a circular equatorial orbit with a radius of 6778 km with a mu = 398600
     * km^3/s^2. The secondary object is on an intersect course with a circular polar orbit with the same radius as primary.
     * Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * Each of the collision object have an identical diagonal covariance matrix.
     * </p>
     * <p>
     * The goal of this test is to verify if the computed projection matrix from primary inertial to Valsecchi collision
     * plane is the same as expected (computed by hand).
     * </p>
     */
    @Test
    @DisplayName("Test the projection matrix from primary inertial to the Valsecchi collision plane")
    public void testReturnProjectionMatrixFromPrimaryInertialToValsecchiCollisionPlane() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000, 0, 0),
                                  new Vector3D(0, 7668.631425, 0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final StateCovariance primaryCovariance = Mockito.mock(StateCovariance.class);

        final double primaryRadius = 5;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000 + 1, 0, 0),
                                  new Vector3D(0, 0, 7668.631425)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final StateCovariance secondaryCovariance = Mockito.mock(StateCovariance.class);

        final double secondaryRadius = 5;

        // Collision definition
        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadius, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius,
                                                                                            EncounterLOFType.VALSECCHI,
                                                                                            1e-6);

        // WHEN
        final RealMatrix projectionMatrixFromPrimaryInertialToCollisionPlane =
                collision.computeReferenceInertialToCollisionPlaneProjectionMatrix();

        // THEN
        Assertions.assertEquals(1, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 0), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 1), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 2), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 0), 1e-10);
        Assertions.assertEquals(-FastMath.sqrt(2) / 2,
                                projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 1), 1e-7);
        Assertions.assertEquals(-FastMath.sqrt(2) / 2,
                                projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 2), 1e-7);
    }

    /**
     * Test with a primary collision object on a circular equatorial orbit with a radius of 6778 km with a mu = 398600
     * km^3/s^2. The secondary object is on an intersect course with a circular polar orbit with the same radius as primary.
     * Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * Each of the collision object have an identical diagonal covariance matrix.
     * </p>
     * <p>
     * The goal of this test is to verify if the computed relative PVCoordinates of the secondary collision object to the
     * primary collision object is the same as expected.
     * </p>
     */
    @Test
    @DisplayName("Test the relative PVCoordinates of the secondary collision object to the primary collision object")
    public void testReturnSecondaryRelativeToPrimaryInPrimaryInertial() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000, 0, 0),
                                  new Vector3D(0, 7668.631425, 0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final StateCovariance primaryCovariance = Mockito.mock(StateCovariance.class);

        final double primaryRadius = 5;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000 + 1, 0, 0),
                                  new Vector3D(0, 0, 7668.631425)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final StateCovariance secondaryCovariance = Mockito.mock(StateCovariance.class);

        final double secondaryRadius = 5;

        // Collision definition
        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadius, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius);

        // WHEN
        final PVCoordinates secondaryRelativeToPrimaryPVInPrimaryInertial =
                collision.computeOtherRelativeToReferencePVInReferenceInertial();

        // THEN
        Assertions.assertEquals(new Vector3D(1, 0, 0), secondaryRelativeToPrimaryPVInPrimaryInertial.getPosition());
        Assertions.assertEquals(new Vector3D(0, -7668.631425, 7668.631425),
                                secondaryRelativeToPrimaryPVInPrimaryInertial.getVelocity());
    }

    /**
     * Test with a primary collision object on a circular equatorial orbit with a radius of 6778 km and u = 398600 km^3/s^2.
     * The secondary object is on an intersect course with a circular polar orbit of a slightly different radius as primary
     * (1 meter difference). Moreover, they both have the same inertial frame of reference (EME2000).
     * <p>
     * Each of the collision object have an identical diagonal covariance matrix.
     * </p>
     * <p>
     * The goal of this test is to verify if the computed projected diagonalized combined covariance matrix is the same as
     * expected.
     * </p>
     */
    @Test
    @DisplayName("Test the projection and diagonalizing method of the combined covariance matrix with diagonal input matrices")
    public void testComputeTheDiagonalizedCombinedCovarianceMatrixProjectedOntoCollisionPlaneWithDiagonalCovarianceAsInput() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000, 0, 0),
                                  new Vector3D(0, 7668.631425, 0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix primaryCovarianceMatrixInPrimaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 0, 0, 0, 0, 0 },
                                 { 0, 100, 0, 0, 0, 0 },
                                 { 0, 0, 200, 0, 0, 0 },
                                 { 0, 0, 0, 100, 0, 0 },
                                 { 0, 0, 0, 0, 100, 0 },
                                 { 0, 0, 0, 0, 0, 100 } });
        final StateCovariance primaryCovariance = new StateCovariance(primaryCovarianceMatrixInPrimaryRTN,
                                                                      timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double primaryRadius = 5;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();
        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000 + 1, 0, 0),
                                  new Vector3D(0, 0, 7668.631425)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix secondaryCovarianceMatrixInSecondaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 0, 0, 0, 0, 0 },
                                 { 0, 100, 0, 0, 0, 0 },
                                 { 0, 0, 200, 0, 0, 0 },
                                 { 0, 0, 0, 100, 0, 0 },
                                 { 0, 0, 0, 0, 100, 0 },
                                 { 0, 0, 0, 0, 0, 100 } });
        final StateCovariance secondaryCovariance = new StateCovariance(secondaryCovarianceMatrixInSecondaryRTN,
                                                                        timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double secondaryRadius = 5;

        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadius, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius);

        // WHEN
        final RealMatrix projectedDiagonalizedCombinedCovarianceMatrix =
                collision.computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix();
        final double sigmaX = projectedDiagonalizedCombinedCovarianceMatrix.getEntry(
                0, 0);
        final double sigmaY = projectedDiagonalizedCombinedCovarianceMatrix.getEntry(
                1, 1);
        final double crossTerm = projectedDiagonalizedCombinedCovarianceMatrix.getEntry(
                0, 1);

        // THEN
        Assertions.assertEquals(200, sigmaX, 1e-15);
        Assertions.assertEquals(300, sigmaY, 1e-12);
        Assertions.assertEquals(0, crossTerm, 1e-15);
        Assertions.assertEquals(projectedDiagonalizedCombinedCovarianceMatrix.getEntry(1, 0),
                                projectedDiagonalizedCombinedCovarianceMatrix.getEntry(0, 1));
    }

    /**
     * Test with a primary collision object on a circular equatorial orbit with a radius of 6778 km with a mu = 398600
     * km^3/s^2. The secondary object is on an intersect course with a circular polar orbit with the same radius as primary.
     * Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * Each of the collision object have an identical non diagonal covariance matrix.
     * </p>
     * <p>
     * The goal of this test is to verify if the computed projected diagonalized combined covariance matrix is the same as
     * expected with non diagonal covariance matrices as input.
     * </p>
     */
    @Test
    @DisplayName("Test the projection and diagonalizing method of the combined covariance matrix on non diagonal input matrices")
    public void testComputeTheDiagonalizedCombinedCovarianceMatrixProjectedOntoCollisionPlaneWithNonDiagonalCovarianceAsInput() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000, 0, 0),
                                  new Vector3D(0, 7668.631425, 0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix primaryCovarianceMatrixInPrimaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 200, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 } });
        final StateCovariance primaryCovariance = new StateCovariance(primaryCovarianceMatrixInPrimaryRTN,
                                                                      timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double primaryRadius = 5;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000 + 1, 0, 0), new Vector3D(0, 0, 7668.631425)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix secondaryCovarianceMatrixInSecondaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 200, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 } });
        final StateCovariance secondaryCovariance = new StateCovariance(secondaryCovarianceMatrixInSecondaryRTN,
                                                                        timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double secondaryRadius = 5;

        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadius, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius);

        // WHEN

        final RealMatrix projectedDiagonalizedCombinedCovarianceMatrix =
                collision.computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix();

        final double sigmaX = projectedDiagonalizedCombinedCovarianceMatrix.getEntry(0, 0);
        final double sigmaY = projectedDiagonalizedCombinedCovarianceMatrix.getEntry(1, 1);

        // THEN
        Assertions.assertEquals(100, sigmaX, 1e-12);
        Assertions.assertEquals(400, sigmaY, 1e-12);
    }

    /**
     * Test with a primary collision object on a circular equatorial orbit with a radius of 6778 km with a mu = 398600
     * km^3/s^2. The secondary object is on an intersect course with a circular polar orbit with the same radius as primary.
     * Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * Each of the collision object have an identical covariance matrix.
     * </p>
     * <p>
     * Test the computation of the secondary collision object position projected and rotated onto the collision plane with
     * identity covariance matrix as input in order to test the sigmaXSquared <= sigmaYSquared condition with correlation = 0
     * in {@link ShortTermEncounter2DDefinition}.
     * </p>
     */
    @Test
    @DisplayName("Test the computation of the secondary collision object position projected and rotated onto the collision plane with identity covariance matrix as input.")
    public void testComputeSecondaryPositionProjectedAndRotatedOntoCollisionPlaneWithIdentityCovarianceMatrixAsInput() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000, 0, 0),
                                  new Vector3D(0, 7668.631425, 0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix primaryCovarianceMatrixInPrimaryRTN = MatrixUtils.createRealIdentityMatrix(6);

        final StateCovariance primaryCovariance = new StateCovariance(primaryCovarianceMatrixInPrimaryRTN,
                                                                      timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double primaryRadius = 5;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000 + 1, 0, 0), new Vector3D(0, 0, 7668.631425)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix secondaryCovarianceMatrixInSecondaryRTN = MatrixUtils.createRealIdentityMatrix(6);

        final StateCovariance secondaryCovariance = new StateCovariance(secondaryCovarianceMatrixInSecondaryRTN,
                                                                        timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double secondaryRadius = 5;

        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadius, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius);

        // WHEN
        final Vector2D secondaryPositionProjectedAndRotatedOntoCollisionPlane =
                collision.computeOtherPositionInRotatedCollisionPlane();

        // THEN
        Assertions.assertEquals(1, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getX(), 1e-10);
        Assertions.assertEquals(0, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getY(), 1e-10);

    }

    /**
     * Test with a primary collision object on a circular equatorial orbit with a radius of 6778 km with a mu = 398600
     * km^3/s^2. The secondary object is on an intersect course with a circular polar orbit with the same radius as primary.
     * Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * The primary collision object has an identity matrix as covariance and the secondary collision object has a specific
     * matrix.
     * <p>
     * Test the computation of the secondary collision object position projected and rotated onto the collision plane with
     * specific covariance matrix as input in order to test the sigmaXSquared > sigmaYSquared condition in
     * {@link ShortTermEncounter2DDefinition}.
     * </p>
     */
    @Test
    @DisplayName("Test the computation of the secondary collision object position projected and rotated onto the collision plane with specific covariance matrix as input")
    public void testComputeSecondaryPositionProjectedAndRotatedOntoCollisionPlaneWithSpecificCovarianceMatrixAsInput() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000, 0, 0),
                                  new Vector3D(0, 7668.631425, 0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix primaryCovarianceMatrixInPrimaryRTN = MatrixUtils.createRealIdentityMatrix(6);
        final StateCovariance primaryCovariance = new StateCovariance(primaryCovarianceMatrixInPrimaryRTN,
                                                                      timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double primaryRadius = 5;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();
        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000 + 1, 0, 0),
                                  new Vector3D(0, 0, 7668.631425)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix secondaryCovarianceMatrixInSecondaryRTN = MatrixUtils.createRealIdentityMatrix(6);
        secondaryCovarianceMatrixInSecondaryRTN.setEntry(0, 0, 199);
        final StateCovariance secondaryCovariance = new StateCovariance(secondaryCovarianceMatrixInSecondaryRTN,
                                                                        timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double secondaryRadius = 5;

        // Define custom threshold to test condition where combined covariance matrix is already diagonalized
        final double CUSTOMZEROTHRESHOLD = 5e-14;

        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadius, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius);

        // WHEN
        final Vector2D secondaryPositionProjectedAndRotatedOntoCollisionPlane =
                collision.computeOtherPositionInRotatedCollisionPlane(
                        CUSTOMZEROTHRESHOLD);

        // THEN
        Assertions.assertEquals(0, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getX(), 1e-10);
        Assertions.assertEquals(-1, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getY(), 1e-10);

    }

    /**
     * Test with a primary collision object on a circular equatorial orbit with a radius of 6778 km with a mu = 398600
     * km^3/s^2. The secondary object is on an intersect course with a circular polar orbit with the same radius as primary.
     * Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * Each of the collision object have an identical non diagonal covariance matrix.
     * </p>
     * <p>
     * The goal of this test is to verify that the computed secondary collision object position projected and rotated onto
     * the collision plane is the same as expected.
     * </p>
     */
    @Test
    @DisplayName("Test the computation of the secondary collision object position projected and rotated onto the collision plane.")
    public void testComputeSecondaryPositionProjectedAndRotatedOntoCollisionPlaneWithNonDiagonalCovarianceAsInput() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000, 0, 0),
                                  new Vector3D(0, 7668.631425, 0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix primaryCovarianceMatrixInPrimaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 200, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 } });
        final StateCovariance primaryCovariance = new StateCovariance(primaryCovarianceMatrixInPrimaryRTN,
                                                                      timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double primaryRadius = 5;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000 + 1, 0, 0),
                                  new Vector3D(0, 0, 7668.631425)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix secondaryCovarianceMatrixInSecondaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 200, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 } });
        final StateCovariance secondaryCovariance = new StateCovariance(secondaryCovarianceMatrixInSecondaryRTN,
                                                                        timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double secondaryRadius = 5;

        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadius, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius);

        // WHEN
        final Vector2D secondaryPositionProjectedAndRotatedOntoCollisionPlane =
                collision.computeOtherPositionInRotatedCollisionPlane(
                        DEFAULTZEROTHRESHOLD);

        // THEN
        Assertions.assertEquals(0.8164965809277260, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getX(), 1e-16);
        Assertions.assertEquals(0.5773502691896257, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getY(), 1e-16);

    }

    /**
     * Test with a primary collision object on a circular equatorial orbit with a radius of 6778 km with a mu = 398600
     * km^3/s^2. The secondary object is on an intersect course with a circular polar orbit with the (almost) same radius as
     * primary. Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * Each of the collision object have an identical non diagonal covariance matrix.
     * </p>
     * <p>
     * The goal of this test is to verify that the computed mahalanobis distance is the same as expected.
     * </p>
     */
    @Test
    @DisplayName("Test the computation of the mahalanobis distance")
    public void testComputeMahalanobisDistance() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000, 0, 0), new Vector3D(0, 7668.631425, 0)), primaryInertialFrame,
                timeOfClosestApproach, mu);

        final RealMatrix primaryCovarianceMatrixInPrimaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 100, 100, 100, 100, 100 }, { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 200, 100, 100, 100 }, { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 }, { 100, 100, 100, 100, 100, 100 } });
        final StateCovariance primaryCovariance = new StateCovariance(primaryCovarianceMatrixInPrimaryRTN,
                                                                      timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double primaryRadiusMock = 5;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000 + 1, 0, 0),
                                  new Vector3D(0, 0, 7668.631425)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix secondaryCovarianceMatrixInSecondaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 100, 100, 100, 100, 100 }, { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 200, 100, 100, 100 }, { 100, 100, 100, 100, 100, 100 },
                                 { 100, 100, 100, 100, 100, 100 }, { 100, 100, 100, 100, 100, 100 } });
        final StateCovariance secondaryCovariance = new StateCovariance(secondaryCovarianceMatrixInSecondaryRTN,
                                                                        timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double secondaryRadiusMock = 5;

        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadiusMock, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadiusMock);

        // WHEN
        final double mahalanobisDistance1 = FastMath.sqrt(collision.computeSquaredMahalanobisDistance());
        final double mahalanobisDistance2 = collision.computeMahalanobisDistance();

        // THEN
        Assertions.assertEquals(0.08660254037844389, mahalanobisDistance1, 1e-17);
        Assertions.assertEquals(0.08660254037844389, mahalanobisDistance2, 1e-17);
    }

    @Test
    @DisplayName("Test the computation of the mahalanobis distance on Armellin's paper appendix case")
    public void testComputeExpectedMahalanobisDistanceFromArmellinPaperAppendixCase() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the combined radius from Armellin's paper appendix (m)
        final double combinedRadius = 29.71;

        // Define the primary collision object according to Armellin's paper appendix
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(2.33052185175137e3, -1.10370451050201e6, 7.10588764299718e6),
                                  new Vector3D(-7.44286282871773e3, -6.13734743652660e-1, 3.95136139293349e0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix primaryCovarianceMatrixInPrimaryRTN = new BlockRealMatrix(
                new double[][] { { 9.31700905887535e1, -2.623398113500550e2, 2.360382173935300e1, 0, 0, 0 },
                                 { -2.623398113500550e2, 1.77796454279511e4, -9.331225387386501e1, 0, 0, 0 },
                                 { 2.360382173935300e1, -9.331225387386501e1, 1.917372231880040e1, 0, 0, 0 },
                                 { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } });
        final StateCovariance primaryCovariance = new StateCovariance(primaryCovarianceMatrixInPrimaryRTN,
                                                                      timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double primaryRadius = combinedRadius / 2;

        // Define the secondary collision object according to Armellin's paper appendix
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(2.333465506263321e3, -1.103671212478364e6, 7.105914958099038e6),
                                  new Vector3D(7.353740487126315e3, -1.142814049765362e3, -1.982472259113771e2)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix secondaryCovarianceMatrixInSecondaryRTN = new BlockRealMatrix(
                new double[][] { { 6.346570910720371e2, -1.962292216245289e3, 7.077413655227660e1, 0, 0, 0 },
                                 { -1.962292216245289e3, 8.199899363150306e5, 1.139823810584350e3, 0, 0, 0 },
                                 { 7.077413655227660e1, 1.139823810584350e3, 2.510340829074070e2, 0, 0, 0 },
                                 { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0 } });
        final StateCovariance secondaryCovariance = new StateCovariance(secondaryCovarianceMatrixInSecondaryRTN,
                                                                        timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double secondaryRadius = combinedRadius / 2;

        // Defining collision
        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadius, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius);

        // WHEN
        final double result = collision.computeMahalanobisDistance(DEFAULTZEROTHRESHOLD);

        // THEN
        Assertions.assertEquals(0.933624872, result, 1e-9);
    }

    @Test
    @DisplayName("Test the computation of the miss distance on Armellin's paper appendix case")
    public void testComputeExpectedMissDistanceFromArmellinPaperAppendixCase() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the combined radius from Armellin's paper appendix (m)
        final double combinedRadius = 29.71;

        // Define the primary collision object according to Armellin's paper appendix
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(2.33052185175137e3, -1.10370451050201e6, 7.10588764299718e6),
                                  new Vector3D(-7.44286282871773e3, -6.13734743652660e-1, 3.95136139293349e0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final StateCovariance primaryCovariance = Mockito.mock(StateCovariance.class);

        final double primaryRadius = combinedRadius * 0.5;

        // Define the secondary collision object according to Armellin's paper appendix
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(2.333465506263321e3, -1.103671212478364e6, 7.105914958099038e6),
                                  new Vector3D(7.353740487126315e3, -1.142814049765362e3, -1.982472259113771e2)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final StateCovariance secondaryCovariance = Mockito.mock(StateCovariance.class);
        final double          secondaryRadius     = combinedRadius * 0.5;

        // Defining collision
        final ShortTermEncounter2DDefinition collision = new ShortTermEncounter2DDefinition(primary, primaryCovariance,
                                                                                            primaryRadius, secondary,
                                                                                            secondaryCovariance,
                                                                                            secondaryRadius);

        // WHEN
        final double result = collision.computeMissDistance();

        // THEN
        Assertions.assertEquals(43.16871865, result, 1e-8);
    }

    @Test
    @DisplayName("Test mahalanobis distance method on Armellin's data and make statistics")
    public void testCompareStatisticsAboutMahalanobisDistanceWithArmellinData() throws IOException {

        // GIVEN & When
        final DescriptiveStatistics statistics =
                ArmellinStatistics.getMahalanobisDistanceRelativeDifferenceStatistics();

        // THEN
        Assertions.assertTrue(statistics.getMean() <= 1.655252960031764E-10);
        Assertions.assertTrue(statistics.getStandardDeviation() <= 5.289370450380533E-10);
    }

    @Test
    @DisplayName("Test IllegalArgumentException thrown when collision object definition date are different")
    void testThrowIllegalArgumentException() {

        // GIVEN
        // Define primary collision object
        final AbsoluteDate primaryDate = new AbsoluteDate();

        final Orbit primaryMock = Mockito.mock(Orbit.class);
        Mockito.when(primaryMock.getDate()).thenReturn(primaryDate);

        final StateCovariance primaryCovariance = Mockito.mock(StateCovariance.class);

        final double primaryRadius = 1;

        // Define secondary collision object
        final AbsoluteDate secondaryDate = new AbsoluteDate().shiftedBy(1);

        final Orbit secondaryMock = Mockito.mock(Orbit.class);
        Mockito.when(secondaryMock.getDate()).thenReturn(secondaryDate);

        final StateCovariance secondaryCovariance = Mockito.mock(StateCovariance.class);

        final double secondaryRadius = 2;

        // THEN
        Assertions.assertThrows(OrekitException.class,
                                () -> new ShortTermEncounter2DDefinition(primaryMock, primaryCovariance, primaryRadius,
                                                                         secondaryMock, secondaryCovariance,
                                                                         secondaryRadius));

    }

    /**
     * Test with a primary collision object on a circular equatorial orbit with a radius of 6778 km with a mu = 398600
     * km^3/s^2. The secondary object is on an intersect course with a circular polar orbit with the (almost) same radius as
     * primary. Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * Each of the collision object have an identical non diagonal covariance matrix.
     * </p>
     * <p>
     * The goal of this test is to verify that the computed encounter duration evaluation is the same as expected.
     * </p>
     */
    @Test
    @DisplayName("Test Coppola's estimation of conjunction time")
    void testComputeShortEncounterDuration() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000, 0, 0),
                                  new Vector3D(0, 7668.631425, 0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix primaryCovarianceMatrixInPrimaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 0, 0, 0, 0, 0 }, { 0, 100, 0, 0, 0, 0 }, { 0, 0, 200, 0, 0, 0 },
                                 { 0, 0, 0, 100, 0, 0 }, { 0, 0, 0, 0, 100, 0 }, { 0, 0, 0, 0, 0, 100 } });
        final StateCovariance primaryCovariance = new StateCovariance(primaryCovarianceMatrixInPrimaryRTN,
                                                                      timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double primaryRadius = 5;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final Orbit secondary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000 + 1, 0, 0),
                                  new Vector3D(0, 0, 7668.631425)),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix secondaryCovarianceMatrixInSecondaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 0, 0, 0, 0, 0 },
                                 { 0, 100, 0, 0, 0, 0 },
                                 { 0, 0, 200, 0, 0, 0 },
                                 { 0, 0, 0, 100, 0, 0 },
                                 { 0, 0, 0, 0, 100, 0 },
                                 { 0, 0, 0, 0, 0, 100 } });
        final StateCovariance secondaryCovariance = new StateCovariance(secondaryCovarianceMatrixInSecondaryRTN,
                                                                        timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double secondaryRadius = 5;

        final ShortTermEncounter2DDefinition collisionDefinition = new ShortTermEncounter2DDefinition(primary,
                                                                                                      primaryCovariance,
                                                                                                      primaryRadius,
                                                                                                      secondary,
                                                                                                      secondaryCovariance,
                                                                                                      secondaryRadius);

        // WHEN
        final double encounterTimeDuration = collisionDefinition.computeCoppolaEncounterDuration();

        // THEN
        Assertions.assertEquals(0.02741114742, encounterTimeDuration, 1e-11);
    }

    /**
     * Test with a primary collision object on a circular equatorial orbit with a radius of 6778 km with a mu = 398600
     * km^3/s^2. The secondary object is on an intersect course with a circular polar orbit with the (almost) same radius as
     * primary. Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * Each of the collision object have an identical non diagonal covariance matrix.
     * </p>
     * <p>
     * The goal of this test is to verify that the computed encounter duration evaluation is the same as expected.
     * </p>
     */
    @Test
    @DisplayName("Test Coppola's estimation of conjunction time")
    void testComputeLongEncounterDuration() {

        // GIVEN
        // Define the time of closest approach and mu
        final AbsoluteDate timeOfClosestApproach = new AbsoluteDate();
        final double       mu                    = Constants.IERS2010_EARTH_MU;

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final Orbit primary = new CartesianOrbit(
                new PVCoordinates(new Vector3D(6778000, 0, 0),
                                  new Vector3D(0, 7668.631425, 0)),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix primaryCovarianceMatrixInPrimaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 0, 0, 0, 0, 0 }, { 0, 100, 0, 0, 0, 0 }, { 0, 0, 200, 0, 0, 0 },
                                 { 0, 0, 0, 100, 0, 0 }, { 0, 0, 0, 0, 100, 0 }, { 0, 0, 0, 0, 0, 100 } });
        final StateCovariance primaryCovariance = new StateCovariance(primaryCovarianceMatrixInPrimaryRTN,
                                                                      timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double primaryRadius = 5;

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final double angleInRad = FastMath.toRadians(0.01);
        final Orbit secondary = new CartesianOrbit(new PVCoordinates(new Vector3D(6778000 + 1, 0, 0),
                                                                     new Vector3D(0,
                                                                                  7668.631425 * FastMath.cos(angleInRad),
                                                                                  -7668.631425 * FastMath.sin(angleInRad))),
                                                   secondaryInertialFrame, timeOfClosestApproach, mu);

        final RealMatrix secondaryCovarianceMatrixInSecondaryRTN = new BlockRealMatrix(
                new double[][] { { 100, 50, 40, 0, 0, 0 }, { 50, 100, 30, 0, 0, 0 }, { 40, 30, 200, 0, 0, 0 },
                                 { 0, 0, 0, 100, 0, 0 }, { 0, 0, 0, 0, 100, 0 }, { 0, 0, 0, 0, 0, 100 } });
        final StateCovariance secondaryCovariance = new StateCovariance(secondaryCovarianceMatrixInSecondaryRTN,
                                                                        timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final double secondaryRadius = 5;

        final ShortTermEncounter2DDefinition collisionDefinition = new ShortTermEncounter2DDefinition(primary,
                                                                                                      primaryCovariance,
                                                                                                      primaryRadius,
                                                                                                      secondary,
                                                                                                      secondaryCovariance,
                                                                                                      secondaryRadius);

        // WHEN
        final double encounterTimeDuration = collisionDefinition.computeCoppolaEncounterDuration();

        // THEN
        Assertions.assertEquals(254.56056997152353, encounterTimeDuration, 1e-14);

    }

    @Test
    @DisplayName("Test ShortTermEncounter2DDefinition getters")
    void testReturnInitialInstances() {
        // GIVEN
        final AbsoluteDate tca = new AbsoluteDate();

        final Orbit           referenceOrbit      = Mockito.mock(CartesianOrbit.class);
        final StateCovariance referenceCovariance = Mockito.mock(StateCovariance.class);
        final double          referenceRadius     = 5;

        final Orbit           otherOrbit      = Mockito.mock(CartesianOrbit.class);
        final StateCovariance otherCovariance = Mockito.mock(StateCovariance.class);
        final double          otherRadius     = 5;

        Mockito.when(referenceOrbit.getDate()).thenReturn(tca);
        Mockito.when(otherOrbit.getDate()).thenReturn(tca);

        final ShortTermEncounter2DDefinition
                encounter = new ShortTermEncounter2DDefinition(referenceOrbit, referenceCovariance,
                                                               referenceRadius, otherOrbit,
                                                               otherCovariance, otherRadius);

        // WHEN
        final AbsoluteDate gottenTCA = encounter.getTca();

        final Orbit           gottenReferenceOrbit      = encounter.getReferenceAtTCA();
        final StateCovariance gottenReferenceCovariance = encounter.getReferenceCovariance();

        final Orbit           gottenOtherOrbit      = encounter.getOtherAtTCA();
        final StateCovariance gottenOtherCovariance = encounter.getOtherCovariance();

        // THEN
        Assertions.assertEquals(tca, gottenTCA);
        Assertions.assertEquals(referenceOrbit, gottenReferenceOrbit);
        Assertions.assertEquals(referenceCovariance, gottenReferenceCovariance);
        Assertions.assertEquals(otherOrbit, gottenOtherOrbit);
        Assertions.assertEquals(otherCovariance, gottenOtherCovariance);
    }

}
