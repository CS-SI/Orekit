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

import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.twod.FieldVector2D;
import org.hipparchus.linear.BlockFieldMatrix;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
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
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldStateCovariance;
import org.orekit.propagation.StateCovariance;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.armellinutils.ArmellinStatistics;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

import java.io.IOException;

class FieldShortTermEncounter2DDefinitionTest {

    /**
     * Threshold below which values are considered equal to zero.
     */
    private final double DEFAULT_ZERO_THRESHOLD = 5e-14;

    @Test
    @DisplayName("Test the combined radius (sum of each collision object sphere equivalent radius)")
    public void testGiveTheSumOfEachCollisionObjectRadius() {

        // GIVEN
        // Define the time of closest approach
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);

        // Define the primary collision object
        @SuppressWarnings("unchecked")
        final FieldOrbit<Binary64> primaryMock = Mockito.mock(FieldOrbit.class);

        Mockito.when(primaryMock.getDate()).thenReturn(timeOfClosestApproach);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> primaryCovariance = Mockito.mock(FieldStateCovariance.class);

        final Binary64 primaryRadius = new Binary64(5);

        // Define the secondary collision object
        @SuppressWarnings("unchecked")
        final FieldOrbit<Binary64> secondaryMock = Mockito.mock(FieldOrbit.class);

        Mockito.when(secondaryMock.getDate()).thenReturn(timeOfClosestApproach);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> secondaryCovariance = Mockito.mock(FieldStateCovariance.class);

        final Binary64 secondaryRadius = new Binary64(3);

        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primaryMock, primaryCovariance,
                                                          primaryRadius, secondaryMock,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN
        final Binary64 combinedRadius = collision.getCombinedRadius();

        // THEN
        Assertions.assertEquals(8, combinedRadius.getReal());
    }

    /**
     * Test with a primary collision object on a circular equatorial FieldOrbit<Binary64> with a radius of 6778 km with a mu
     * = 398600 km^3/s^2. The secondary object is on an intersect course with a circular polar FieldOrbit<Binary64> with the
     * same radius as primary. Moreover, they both have the same inertial frame (EME2000).
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
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7668.631425),
                                                             new Binary64(0))),
                primaryInertialFrame, timeOfClosestApproach, mu);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> primaryCovariance = Mockito.mock(FieldStateCovariance.class);

        final Binary64 primaryRadius = new Binary64(5);

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000 + 1),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(0),
                                                             new Binary64(7668.631425))),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> secondaryCovariance = Mockito.mock(FieldStateCovariance.class);
        final Binary64 secondaryRadius = new Binary64(5);

        // Collision definition
        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                          primaryRadius, secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN
        final FieldMatrix<Binary64> projectionMatrixFromPrimaryInertialToCollisionPlane =
                collision.computeReferenceInertialToCollisionPlaneProjectionMatrix();

        // THEN
        Assertions.assertEquals(1, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 0).getReal(), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 1).getReal(), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 2).getReal(), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 0).getReal(), 1e-10);
        Assertions.assertEquals(FastMath.sqrt(2) * 0.5,
                                projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 1).getReal(), 1e-7);
        Assertions.assertEquals(FastMath.sqrt(2) * 0.5,
                                projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 2).getReal(), 1e-7);
    }

    /**
     * Test with a primary collision object on a circular equatorial FieldOrbit<Binary64> with a radius of 6778 km with a mu
     * = 398600 km^3/s^2. The secondary object is on an intersect course with a circular polar FieldOrbit<Binary64> with the
     * same radius as primary. Moreover, they both have the same inertial frame (EME2000).
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
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7668.631425),
                                                             new Binary64(0))), primaryInertialFrame,
                timeOfClosestApproach, mu);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> primaryCovariance = Mockito.mock(FieldStateCovariance.class);

        final Binary64 primaryRadius = new Binary64(5);

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000 + 1),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(0),
                                                             new Binary64(7668.631425))),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> secondaryCovariance = Mockito.mock(FieldStateCovariance.class);

        final Binary64 secondaryRadius = new Binary64(5);

        // Collision definition
        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                          primaryRadius, secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius,
                                                          EncounterLOFType.VALSECCHI,
                                                          1e-6);

        // WHEN
        final FieldMatrix<Binary64> projectionMatrixFromPrimaryInertialToCollisionPlane =
                collision.computeReferenceInertialToCollisionPlaneProjectionMatrix();

        // THEN
        Assertions.assertEquals(1, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 0).getReal(), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 1).getReal(), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(0, 2).getReal(), 1e-10);
        Assertions.assertEquals(0, projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 0).getReal(), 1e-10);
        Assertions.assertEquals(-FastMath.sqrt(2) / 2,
                                projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 1).getReal(), 1e-7);
        Assertions.assertEquals(-FastMath.sqrt(2) / 2,
                                projectionMatrixFromPrimaryInertialToCollisionPlane.getEntry(1, 2).getReal(), 1e-7);
    }

    /**
     * Test with a primary collision object on a circular equatorial FieldOrbit<Binary64> with a radius of 6778 km with a mu
     * = 398600 km^3/s^2. The secondary object is on an intersect course with a circular polar FieldOrbit<Binary64> with the
     * same radius as primary. Moreover, they both have the same inertial frame (EME2000).
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
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7668.631425),
                                                             new Binary64(0))),
                primaryInertialFrame, timeOfClosestApproach, mu);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> primaryCovariance = Mockito.mock(FieldStateCovariance.class);

        final Binary64 primaryRadius = new Binary64(5);

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000 + 1),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(0),
                                                             new Binary64(7668.631425))),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> secondaryCovariance = Mockito.mock(FieldStateCovariance.class);

        final Binary64 secondaryRadius = new Binary64(5);

        // Collision definition
        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                          primaryRadius, secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN
        final FieldPVCoordinates<Binary64> secondaryRelativeToPrimaryPVInPrimaryInertial =
                collision.computeOtherRelativeToReferencePVInReferenceInertial();

        // THEN
        Assertions.assertEquals(new FieldVector3D<>(new Binary64(1),
                                                    new Binary64(0),
                                                    new Binary64(0)),
                                secondaryRelativeToPrimaryPVInPrimaryInertial.getPosition());
        Assertions.assertEquals(new FieldVector3D<>(new Binary64(0),
                                                    new Binary64(-7668.631425),
                                                    new Binary64(7668.631425)),
                                secondaryRelativeToPrimaryPVInPrimaryInertial.getVelocity());
    }

    /**
     * Test with a primary collision object on a circular equatorial FieldOrbit<Binary64> with a radius of 6778 km and u =
     * 398600 km^3/s^2. The secondary object is on an intersect course with a circular polar FieldOrbit<Binary64> of a
     * slightly different radius as primary (1 meter difference). Moreover, they both have the same inertial frame of
     * reference (EME2000).
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
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7668.631425),
                                                             new Binary64(0))),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> primaryCovarianceMatrixInPrimaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] { { new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(200), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(100) } });
        final FieldStateCovariance<Binary64> primaryCovariance =
                new FieldStateCovariance<>(primaryCovarianceMatrixInPrimaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 primaryRadius = new Binary64(5);

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();
        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000 + 1), new Binary64(0), new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0), new Binary64(0), new Binary64(7668.631425))),
                secondaryInertialFrame, timeOfClosestApproach, mu);
        final FieldMatrix<Binary64> secondaryCovarianceMatrixInSecondaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] { { new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(200), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(100) } });
        final FieldStateCovariance<Binary64> secondaryCovariance =
                new FieldStateCovariance<>(secondaryCovarianceMatrixInSecondaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);
        final Binary64 secondaryRadius = new Binary64(5);

        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                          primaryRadius, secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN
        final FieldMatrix<Binary64> projectedDiagonalizedCombinedCovarianceMatrix =
                collision.computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix();

        final Binary64 sigmaX = projectedDiagonalizedCombinedCovarianceMatrix.getEntry(
                0, 0);
        final Binary64 sigmaY = projectedDiagonalizedCombinedCovarianceMatrix.getEntry(
                1, 1);
        final Binary64 crossTerm = projectedDiagonalizedCombinedCovarianceMatrix.getEntry(
                0, 1);

        // THEN
        Assertions.assertEquals(200, sigmaX.getReal(), 1e-15);
        Assertions.assertEquals(300, sigmaY.getReal(), 1e-12);
        Assertions.assertEquals(0, crossTerm.getReal(), 1e-15);
        Assertions.assertEquals(projectedDiagonalizedCombinedCovarianceMatrix.getEntry(1, 0),
                                projectedDiagonalizedCombinedCovarianceMatrix.getEntry(0, 1));
    }

    /**
     * Test with a primary collision object on a circular equatorial FieldOrbit<Binary64> with a radius of 6778 km with a mu
     * = 398600 km^3/s^2. The secondary object is on an intersect course with a circular polar FieldOrbit<Binary64> with the
     * same radius as primary. Moreover, they both have the same inertial frame (EME2000).
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
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7668.631425),
                                                             new Binary64(0))), primaryInertialFrame,
                timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> primaryCovarianceMatrixInPrimaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] {
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(200), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) } });
        final FieldStateCovariance<Binary64> primaryCovariance =
                new FieldStateCovariance<>(primaryCovarianceMatrixInPrimaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 primaryRadius = new Binary64(5);

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000 + 1),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(0),
                                                             new Binary64(7668.631425))),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> secondaryCovarianceMatrixInSecondaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] {
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(200), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) } });
        final FieldStateCovariance<Binary64> secondaryCovariance =
                new FieldStateCovariance<>(secondaryCovarianceMatrixInSecondaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 secondaryRadius = new Binary64(5);

        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                          primaryRadius, secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN

        final FieldMatrix<Binary64> projectedDiagonalizedCombinedCovarianceMatrix =
                collision.computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix();

        final Binary64 sigmaX = projectedDiagonalizedCombinedCovarianceMatrix.getEntry(0, 0);
        final Binary64 sigmaY = projectedDiagonalizedCombinedCovarianceMatrix.getEntry(1, 1);

        // THEN
        Assertions.assertEquals(100, sigmaX.getReal(), 1e-5);
        Assertions.assertEquals(400, sigmaY.getReal(), 1e-5);
    }

    /**
     * Test with a primary collision object on a circular equatorial FieldOrbit<Binary64> with a radius of 6778 km with a mu
     * = 398600 km^3/s^2. The secondary object is on an intersect course with a circular polar FieldOrbit<Binary64> with the
     * same radius as primary. Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * Each of the collision object have an identical covariance matrix.
     * </p>
     * <p>
     * Test the computation of the secondary collision object position projected and rotated onto the collision plane with
     * identity covariance matrix as input in order to test the sigmaXSquared <= sigmaYSquared condition with correlation = 0
     * in {@link FieldShortTermEncounter2DDefinition <Binary64>}.
     * </p>
     */
    @Test
    @DisplayName("Test the computation of the secondary collision object position projected and rotated onto the collision plane with identity covariance matrix as input.")
    public void testComputeSecondaryPositionProjectedAndRotatedOntoCollisionPlaneWithIdentityCovarianceMatrixAsInput() {

        // GIVEN
        // Define the time of closest approach and mu
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7668.631425),
                                                             new Binary64(0))), primaryInertialFrame,
                timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> primaryCovarianceMatrixInPrimaryRTN = MatrixUtils.createFieldIdentityMatrix(field, 6);
        final FieldStateCovariance<Binary64> primaryCovariance =
                new FieldStateCovariance<>(primaryCovarianceMatrixInPrimaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 primaryRadius = new Binary64(5);

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000 + 1),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(0),
                                                             new Binary64(7668.631425))),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> secondaryCovarianceMatrixInSecondaryRTN =
                MatrixUtils.createFieldIdentityMatrix(field, 6);
        final FieldStateCovariance<Binary64> secondaryCovariance =
                new FieldStateCovariance<>(secondaryCovarianceMatrixInSecondaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);
        final Binary64 secondaryRadius = new Binary64(5);

        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                          primaryRadius, secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN
        final FieldVector2D<Binary64> secondaryPositionProjectedAndRotatedOntoCollisionPlane =
                collision.computeOtherPositionInRotatedCollisionPlane();

        // THEN
        Assertions.assertEquals(1, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getX().getReal(), 1e-10);
        Assertions.assertEquals(0, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getY().getReal(), 1e-10);

    }

    /**
     * Test with a primary collision object on a circular equatorial FieldOrbit<Binary64> with a radius of 6778 km with a mu
     * = 398600 km^3/s^2. The secondary object is on an intersect course with a circular polar FieldOrbit<Binary64> with the
     * same radius as primary. Moreover, they both have the same inertial frame (EME2000).
     * <p>
     * The primary collision object has an identity matrix as covariance and the secondary collision object has a specific
     * matrix.
     * <p>
     * Test the computation of the secondary collision object position projected and rotated onto the collision plane with
     * specific covariance matrix as input in order to test the sigmaXSquared > sigmaYSquared condition in
     * {@link FieldShortTermEncounter2DDefinition <Binary64>}.
     * </p>
     */
    @Test
    @DisplayName("Test the computation of the secondary collision object position projected and rotated onto the collision plane with specific covariance matrix as input")
    public void testComputeSecondaryPositionProjectedAndRotatedOntoCollisionPlaneWithSpecificCovarianceMatrixAsInput() {

        // GIVEN
        // Define the time of closest approach and mu
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7668.631425),
                                                             new Binary64(0))),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> primaryCovarianceMatrixInPrimaryRTN = MatrixUtils.createFieldIdentityMatrix(field, 6);
        final FieldStateCovariance<Binary64> primaryCovariance =
                new FieldStateCovariance<>(primaryCovarianceMatrixInPrimaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 primaryRadius = new Binary64(5);

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000 + 1),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(0),
                                                             new Binary64(7668.631425))),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> secondaryCovarianceMatrixInSecondaryRTN =
                MatrixUtils.createFieldIdentityMatrix(field, 6);
        secondaryCovarianceMatrixInSecondaryRTN.setEntry(0, 0, new Binary64(199));
        final FieldStateCovariance<Binary64> secondaryCovariance =
                new FieldStateCovariance<>(secondaryCovarianceMatrixInSecondaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 secondaryRadius = new Binary64(5);

        // Define custom threshold to test condition where combined covariance matrix is already diagonalized
        final double CUSTOMZEROTHRESHOLD = 5e-14;

        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                          primaryRadius, secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN
        final FieldVector2D<Binary64> secondaryPositionProjectedAndRotatedOntoCollisionPlane =
                collision.computeOtherPositionInRotatedCollisionPlane(CUSTOMZEROTHRESHOLD);

        // THEN
        Assertions.assertEquals(0, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getX().getReal(), 1e-10);
        Assertions.assertEquals(-1, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getY().getReal(), 1e-10);

    }

    /**
     * Test with a primary collision object on a circular equatorial FieldOrbit<Binary64> with a radius of 6778 km with a mu
     * = 398600 km^3/s^2. The secondary object is on an intersect course with a circular polar FieldOrbit<Binary64> with the
     * same radius as primary. Moreover, they both have the same inertial frame (EME2000).
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
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7668.631425),
                                                             new Binary64(0))),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> primaryCovarianceMatrixInPrimaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] {
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(200), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) } });
        final FieldStateCovariance<Binary64> primaryCovariance =
                new FieldStateCovariance<>(primaryCovarianceMatrixInPrimaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 primaryRadius = new Binary64(5);

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000 + 1),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(0),
                                                             new Binary64(7668.631425))),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> secondaryCovarianceMatrixInSecondaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] {
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(200), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) } });
        final FieldStateCovariance<Binary64> secondaryCovariance =
                new FieldStateCovariance<>(secondaryCovarianceMatrixInSecondaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 secondaryRadius = new Binary64(5);

        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                          primaryRadius, secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN
        final FieldVector2D<Binary64> secondaryPositionProjectedAndRotatedOntoCollisionPlane =
                collision.computeOtherPositionInRotatedCollisionPlane(
                        DEFAULT_ZERO_THRESHOLD);

        // THEN
        Assertions.assertEquals(0.8164965809277260, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getX().getReal());
        Assertions.assertEquals(0.5773502691896257, secondaryPositionProjectedAndRotatedOntoCollisionPlane.getY().getReal());

    }

    /**
     * Test with a primary collision object on a circular equatorial FieldOrbit<Binary64> with a radius of 6778 km with a mu
     * = 398600 km^3/s^2. The secondary object is on an intersect course with a circular polar FieldOrbit<Binary64> with the
     * (almost) same radius as primary. Moreover, they both have the same inertial frame (EME2000).
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
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();
        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7668.631425),
                                                             new Binary64(0))),
                primaryInertialFrame, timeOfClosestApproach, mu);
        final FieldMatrix<Binary64> primaryCovarianceMatrixInPrimaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] {
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(200), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) } });
        final FieldStateCovariance<Binary64> primaryCovariance =
                new FieldStateCovariance<>(primaryCovarianceMatrixInPrimaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);
        final Binary64 primaryRadiusMock = new Binary64(5);

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();
        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000 + 1),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(0),
                                                             new Binary64(7668.631425))),
                secondaryInertialFrame, timeOfClosestApproach, mu);
        final FieldMatrix<Binary64> secondaryCovarianceMatrixInSecondaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] {
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(200), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) },
                        { new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100), new Binary64(100),
                          new Binary64(100) } });
        final FieldStateCovariance<Binary64> secondaryCovariance =
                new FieldStateCovariance<>(secondaryCovarianceMatrixInSecondaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);
        final Binary64 secondaryRadiusMock = new Binary64(5);

        final FieldShortTermEncounter2DDefinition<Binary64>
                collision = new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                                      primaryRadiusMock, secondary,
                                                                      secondaryCovariance,
                                                                      secondaryRadiusMock);

        // WHEN
        final Binary64 mahalanobisDistance1 = FastMath.sqrt(collision.computeSquaredMahalanobisDistance());
        final Binary64 mahalanobisDistance2 = collision.computeMahalanobisDistance();

        // THEN
        Assertions.assertEquals(0.08660254037844388, mahalanobisDistance1.getReal(), 1e-17);
        Assertions.assertEquals(0.08660254037844388, mahalanobisDistance2.getReal(), 1e-17);
    }

    @Test
    @DisplayName("Test the computation of the mahalanobis distance on Armellin's paper appendix case")
    public void testComputeExpectedMahalanobisDistanceFromArmellinPaperAppendixCase() {

        // GIVEN
        // Define the time of closest approach and mu
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the combined radius from Armellin's paper appendix (m)
        final Binary64 combinedRadius = new Binary64(29.71);

        // Define the primary collision object according to Armellin's paper appendix
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(2.33052185175137e3),
                                                             new Binary64(-1.10370451050201e6),
                                                             new Binary64(7.10588764299718e6)),
                                         new FieldVector3D<>(new Binary64(-7.44286282871773e3),
                                                             new Binary64(-6.13734743652660e-1),
                                                             new Binary64(3.95136139293349e0))),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> primaryCovarianceMatrixInPrimaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] { { new Binary64(9.31700905887535e1), new Binary64(-2.623398113500550e2),
                                     new Binary64(2.360382173935300e1), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(-2.623398113500550e2), new Binary64(1.77796454279511e4),
                                     new Binary64(-9.331225387386501e1), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(2.360382173935300e1), new Binary64(-9.331225387386501e1),
                                     new Binary64(1.917372231880040e1), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) } });
        final FieldStateCovariance<Binary64> primaryCovariance =
                new FieldStateCovariance<>(primaryCovarianceMatrixInPrimaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 primaryRadius = combinedRadius.multiply(0.5);

        // Define the secondary collision object according to Armellin's paper appendix
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(2.333465506263321e3),
                                                             new Binary64(-1.103671212478364e6),
                                                             new Binary64(7.105914958099038e6)),
                                         new FieldVector3D<>(new Binary64(7.353740487126315e3),
                                                             new Binary64(-1.142814049765362e3),
                                                             new Binary64(-1.982472259113771e2))),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> secondaryCovarianceMatrixInSecondaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] { { new Binary64(6.346570910720371e2), new Binary64(-1.962292216245289e3),
                                     new Binary64(7.077413655227660e1), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(-1.962292216245289e3), new Binary64(8.199899363150306e5),
                                     new Binary64(1.139823810584350e3), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(7.077413655227660e1), new Binary64(1.139823810584350e3),
                                     new Binary64(2.510340829074070e2), new Binary64(0), new Binary64(0), new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) } });
        final FieldStateCovariance<Binary64> secondaryCovariance =
                new FieldStateCovariance<>(secondaryCovarianceMatrixInSecondaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 secondaryRadius = combinedRadius.multiply(0.5);

        // Defining collision
        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                          primaryRadius, secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN
        final Binary64 result = collision.computeMahalanobisDistance(DEFAULT_ZERO_THRESHOLD);

        // THEN
        Assertions.assertEquals(0.933624872, result.getReal(), 1e-9);
    }

    @Test
    @DisplayName("Test the computation of the miss distance on Armellin's paper appendix case")
    public void testComputeExpectedMissDistanceFromArmellinPaperAppendixCase() {

        // GIVEN
        // Define the time of closest approach and mu
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the combined radius from Armellin's paper appendix (m)
        final Binary64 combinedRadius = new Binary64(29.71);

        // Define the primary collision object according to Armellin's paper appendix
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(2.33052185175137e3),
                                                             new Binary64(-1.10370451050201e6),
                                                             new Binary64(7.10588764299718e6)),
                                         new FieldVector3D<>(new Binary64(-7.44286282871773e3),
                                                             new Binary64(-6.13734743652660e-1),
                                                             new Binary64(3.95136139293349e0))),
                primaryInertialFrame, timeOfClosestApproach, mu);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> primaryCovariance = Mockito.mock(FieldStateCovariance.class);

        final Binary64 primaryRadius = combinedRadius.multiply(0.5);

        // Define the secondary collision object according to Armellin's paper appendix
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(2.333465506263321e3),
                                                             new Binary64(-1.103671212478364e6),
                                                             new Binary64(7.105914958099038e6)),
                                         new FieldVector3D<>(new Binary64(7.353740487126315e3),
                                                             new Binary64(-1.142814049765362e3),
                                                             new Binary64(-1.982472259113771e2))),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> secondaryCovariance = Mockito.mock(FieldStateCovariance.class);

        final Binary64 secondaryRadius = combinedRadius.multiply(0.5);

        // Defining collision
        final FieldShortTermEncounter2DDefinition<Binary64> collision =
                new FieldShortTermEncounter2DDefinition<>(primary, primaryCovariance,
                                                          primaryRadius, secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN
        final Binary64 result = collision.computeMissDistance();

        // THEN
        Assertions.assertEquals(43.16871865, result.getReal(), 1e-8);
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
        final Field<Binary64> field = Binary64Field.getInstance();

        // Define primary collision object
        final FieldAbsoluteDate<Binary64> primaryDate = new FieldAbsoluteDate<>(field);

        @SuppressWarnings("unchecked")
        final FieldOrbit<Binary64> primaryMock = Mockito.mock(FieldOrbit.class);

        Mockito.when(primaryMock.getDate()).thenReturn(primaryDate);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> primaryCovariance = Mockito.mock(FieldStateCovariance.class);

        final Binary64 primaryRadius = new Binary64(1);

        // Define secondary collision object
        final FieldAbsoluteDate<Binary64> secondaryDate = new FieldAbsoluteDate<>(field).shiftedBy(1);

        @SuppressWarnings("unchecked")
        final FieldOrbit<Binary64> secondaryMock = Mockito.mock(FieldOrbit.class);

        Mockito.when(secondaryMock.getDate()).thenReturn(secondaryDate);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> secondaryCovariance = Mockito.mock(FieldStateCovariance.class);

        final Binary64 secondaryRadius = new Binary64(2);

        // THEN
        Assertions.assertThrows(OrekitException.class,
                                () -> new FieldShortTermEncounter2DDefinition<>(primaryMock, primaryCovariance,
                                                                                primaryRadius,
                                                                                secondaryMock, secondaryCovariance,
                                                                                secondaryRadius));

    }

    /**
     * Test with a primary collision object on a circular equatorial FieldOrbit<Binary64> with a radius of 6778 km with a mu
     * = 398600 km^3/s^2. The secondary object is on an intersect course with a circular polar FieldOrbit<Binary64> with the
     * (almost) same radius as primary. Moreover, they both have the same inertial frame (EME2000).
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
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7668.631425),
                                                             new Binary64(0))),
                primaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> primaryCovarianceMatrixInPrimaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] { { new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(200), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(100) } });
        final FieldStateCovariance<Binary64> primaryCovariance =
                new FieldStateCovariance<>(primaryCovarianceMatrixInPrimaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 primaryRadius = new Binary64(5);

        // Define the secondary collision object
        final Frame secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000 + 1),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(0),
                                                             new Binary64(7668.631425))),
                secondaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> secondaryCovarianceMatrixInSecondaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] { { new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(200), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(100) } });
        final FieldStateCovariance<Binary64> secondaryCovariance =
                new FieldStateCovariance<>(secondaryCovarianceMatrixInSecondaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 secondaryRadius = new Binary64(5);

        final FieldShortTermEncounter2DDefinition<Binary64> collisionDefinition =
                new FieldShortTermEncounter2DDefinition<>(primary,
                                                          primaryCovariance,
                                                          primaryRadius,
                                                          secondary,
                                                          secondaryCovariance,
                                                          secondaryRadius);

        // WHEN
        final Binary64 encounterTimeDuration = collisionDefinition.computeCoppolaEncounterDuration();

        // THEN
        Assertions.assertEquals(0.02741114742, encounterTimeDuration.getReal(), 1e-11);
    }

    /**
     * Test with a primary collision object on a circular equatorial FieldOrbit<Binary64> with a radius of 6778 km with a mu
     * = 398600 km^3/s^2. The secondary object is on an intersect course with a circular polar FieldOrbit<Binary64> with the
     * (almost) same radius as primary. Moreover, they both have the same inertial frame (EME2000).
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
        final Field<Binary64>             field                 = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> timeOfClosestApproach = new FieldAbsoluteDate<>(field);
        final Binary64                    mu                    = new Binary64(Constants.IERS2010_EARTH_MU);

        // Define the primary collision object
        final Frame primaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> primary =
                new FieldCartesianOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000),
                                                                                       new Binary64(0),
                                                                                       new Binary64(0)),
                                                                   new FieldVector3D<>(new Binary64(0),
                                                                                       new Binary64(7668.631425),
                                                                                       new Binary64(0))),
                                          primaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> primaryCovarianceMatrixInPrimaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] {
                        { new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                          new Binary64(0) },
                        { new Binary64(0), new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(0),
                          new Binary64(0) },
                        { new Binary64(0), new Binary64(0), new Binary64(200), new Binary64(0), new Binary64(0),
                          new Binary64(0) },
                        { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100), new Binary64(0),
                          new Binary64(0) },
                        { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100),
                          new Binary64(0) },
                        { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                          new Binary64(100) } });
        final FieldStateCovariance<Binary64> primaryCovariance =
                new FieldStateCovariance<>(primaryCovarianceMatrixInPrimaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);
        final Binary64 primaryRadius = new Binary64(5);

        // Define the secondary collision object
        final Binary64 angleInRad             = new Binary64(FastMath.toRadians(0.01));
        final Frame    secondaryInertialFrame = FramesFactory.getEME2000();

        final FieldOrbit<Binary64> secondary =
                new FieldCartesianOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6778000 + 1),
                                                                                       new Binary64(0),
                                                                                       new Binary64(0)),
                                                                   new FieldVector3D<>(new Binary64(0),
                                                                                       angleInRad.cos()
                                                                                                 .multiply(7668.631425),
                                                                                       angleInRad.sin()
                                                                                                 .multiply(-7668.631425))),
                                          secondaryInertialFrame, timeOfClosestApproach, mu);

        final FieldMatrix<Binary64> secondaryCovarianceMatrixInSecondaryRTN = new BlockFieldMatrix<>(
                new Binary64[][] { { new Binary64(100), new Binary64(50), new Binary64(40), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(50), new Binary64(100), new Binary64(30), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(40), new Binary64(30), new Binary64(200), new Binary64(0), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100), new Binary64(0),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(100),
                                     new Binary64(0) },
                                   { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0),
                                     new Binary64(100) } });
        final FieldStateCovariance<Binary64> secondaryCovariance =
                new FieldStateCovariance<>(secondaryCovarianceMatrixInSecondaryRTN,
                                           timeOfClosestApproach, LOFType.QSW_INERTIAL);

        final Binary64 secondaryRadius = new Binary64(5);

        final FieldShortTermEncounter2DDefinition<Binary64>
                collisionDefinition = new FieldShortTermEncounter2DDefinition<>(primary,
                                                                                primaryCovariance,
                                                                                primaryRadius,
                                                                                secondary,
                                                                                secondaryCovariance,
                                                                                secondaryRadius);

        // WHEN
        final Binary64 encounterTimeDuration = collisionDefinition.computeCoppolaEncounterDuration();

        // THEN
        Assertions.assertEquals(254.56056997152353, encounterTimeDuration.getReal(), 1e-14);

    }

    @Test
    @DisplayName("Test FieldShortTermEncounter2DDefinition<Binary64> getters")
    void testReturnInitialInstances() {
        // GIVEN
        final Field<Binary64>             field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> tca   = new FieldAbsoluteDate<>(field);

        @SuppressWarnings("unchecked")
        final FieldOrbit<Binary64> referenceOrbit = Mockito.mock(FieldCartesianOrbit.class);
        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> referenceCovariance = Mockito.mock(FieldStateCovariance.class);
        final Binary64 referenceRadius = new Binary64(5);

        @SuppressWarnings("unchecked")
        final FieldOrbit<Binary64> otherOrbit = Mockito.mock(FieldCartesianOrbit.class);
        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> otherCovariance = Mockito.mock(FieldStateCovariance.class);
        final Binary64 otherRadius = new Binary64(5);

        Mockito.when(referenceOrbit.getDate()).thenReturn(tca);
        Mockito.when(otherOrbit.getDate()).thenReturn(tca);

        final FieldShortTermEncounter2DDefinition<Binary64> encounter =
                new FieldShortTermEncounter2DDefinition<>(referenceOrbit, referenceCovariance,
                                                          referenceRadius, otherOrbit,
                                                          otherCovariance, otherRadius);

        // WHEN
        final FieldAbsoluteDate<Binary64> gottenTCA = encounter.getTca();

        final FieldOrbit<Binary64>           gottenReferenceOrbit      = encounter.getReferenceAtTCA();
        final FieldStateCovariance<Binary64> gottenReferenceCovariance = encounter.getReferenceCovariance();

        final FieldOrbit<Binary64>           gottenOtherOrbit      = encounter.getOtherAtTCA();
        final FieldStateCovariance<Binary64> gottenOtherCovariance = encounter.getOtherCovariance();

        // THEN
        Assertions.assertEquals(tca, gottenTCA);
        Assertions.assertEquals(referenceOrbit, gottenReferenceOrbit);
        Assertions.assertEquals(referenceCovariance, gottenReferenceCovariance);
        Assertions.assertEquals(otherOrbit, gottenOtherOrbit);
        Assertions.assertEquals(otherCovariance, gottenOtherCovariance);
    }

    @Test
    void testConversionToNonFieldEquivalent() {

        // GIVEN
        final AbsoluteDate                DEFAULT_DATE       = new AbsoluteDate();
        final FieldAbsoluteDate<Binary64> DEFAULT_FIELD_DATE = new FieldAbsoluteDate<>(Binary64Field.getInstance());

        // Create non field version
        final Orbit referenceOrbitMock = Mockito.mock(Orbit.class);
        Mockito.when(referenceOrbitMock.getDate()).thenReturn(DEFAULT_DATE);
        final StateCovariance referenceCovarianceMock = Mockito.mock(StateCovariance.class);

        final Orbit otherOrbitMock = Mockito.mock(Orbit.class);
        Mockito.when(otherOrbitMock.getDate()).thenReturn(DEFAULT_DATE);
        final StateCovariance otherCovarianceMock = Mockito.mock(StateCovariance.class);

        final double combinedRadius = 1;

        // Create field equivalent
        @SuppressWarnings("unchecked")
        final FieldOrbit<Binary64> fieldReferenceOrbitMock = Mockito.mock(FieldOrbit.class);
        Mockito.when(fieldReferenceOrbitMock.getDate()).thenReturn(DEFAULT_FIELD_DATE);
        Mockito.when(fieldReferenceOrbitMock.toOrbit()).thenReturn(referenceOrbitMock);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> fieldReferenceCovarianceMock = Mockito.mock(FieldStateCovariance.class);
        Mockito.when(fieldReferenceCovarianceMock.toStateCovariance()).thenReturn(referenceCovarianceMock);

        @SuppressWarnings("unchecked")
        final FieldOrbit<Binary64> fieldOtherOrbitMock = Mockito.mock(FieldOrbit.class);
        Mockito.when(fieldOtherOrbitMock.getDate()).thenReturn(DEFAULT_FIELD_DATE);
        Mockito.when(fieldOtherOrbitMock.toOrbit()).thenReturn(otherOrbitMock);

        @SuppressWarnings("unchecked")
        final FieldStateCovariance<Binary64> fieldOtherCovarianceMock = Mockito.mock(FieldStateCovariance.class);
        Mockito.when(fieldOtherCovarianceMock.toStateCovariance()).thenReturn(otherCovarianceMock);

        final Binary64 fieldCombinedRadiusMock = Mockito.mock(Binary64.class);
        Mockito.when(fieldCombinedRadiusMock.getReal()).thenReturn(combinedRadius);

        final FieldShortTermEncounter2DDefinition<Binary64> fieldEncounter =
                new FieldShortTermEncounter2DDefinition<>(fieldReferenceOrbitMock, fieldReferenceCovarianceMock,
                                                          fieldOtherOrbitMock, fieldOtherCovarianceMock,
                                                          fieldCombinedRadiusMock);

        // WHEN
        final ShortTermEncounter2DDefinition encounter = fieldEncounter.toEncounter();

        // THEN
        Assertions.assertEquals(referenceOrbitMock, encounter.getReferenceAtTCA());
        Assertions.assertEquals(referenceCovarianceMock, encounter.getReferenceCovariance());
        Assertions.assertEquals(otherOrbitMock, encounter.getOtherAtTCA());
        Assertions.assertEquals(otherCovarianceMock, encounter.getOtherCovariance());
        Assertions.assertEquals(combinedRadius, encounter.getCombinedRadius());
    }
}
