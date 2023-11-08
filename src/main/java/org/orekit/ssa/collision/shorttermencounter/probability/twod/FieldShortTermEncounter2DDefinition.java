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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.twod.FieldVector2D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.linear.Array2DRowFieldMatrix;
import org.hipparchus.linear.BlockFieldMatrix;
import org.hipparchus.linear.FieldLUDecomposition;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.LOF;
import org.orekit.frames.LOFType;
import org.orekit.frames.encounter.EncounterLOF;
import org.orekit.frames.encounter.EncounterLOFType;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldStateCovariance;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

/**
 * Defines the encounter between two collision object at time of closest approach assuming a short-term encounter model . It
 * uses the given {@link EncounterLOFType encounter frame type} to define the encounter.
 * <p>
 * Both the primary and secondary collision object can be at the reference of the encounter frame, it is up to the user to
 * choose.
 * <p>
 * The "reference" object is the object considered at the reference of the given encounter frame while the "other" object is
 * the one <b>not placed</b> at the reference.
 * <p>
 * For example, if the user wants the primary to be at the reference of the default encounter frame, they will have to input
 * data in the following manner:
 * <pre>{@code
 * final FieldShortTermEncounter2DDefinition encounter = new FieldShortTermEncounter2DDefinition<>(primaryOrbitAtTCA, primaryCovarianceAtTCA, primaryRadius, secondaryOrbitAtTCA, secondaryCovarianceAtTCA, secondaryRadius);
 *  }
 * </pre>
 * However, if the user wants to put the secondary at the reference and use the
 * {@link org.orekit.frames.encounter.ValsecchiEncounterFrame Valsecchi encounter frame}, they will have to type :
 * <pre>{@code
 * final FieldShortTermEncounter2DDefinition encounter = new FieldShortTermEncounter2DDefinition<>(secondaryOrbitAtTCA, secondaryCovarianceAtTCA, secondaryRadius, primaryOrbitAtTCA, primaryCovarianceAtTCA, primaryRadius, EncounterLOFType.VALSECCHI_2003);
 *  }
 * </pre>
 * Note that in the current implementation, the shape of the collision objects is assumed to be a sphere.
 *
 * @author Vincent Cucchietti
 * @since 12.0
 * @param <T> type of the field elements
 */
public class FieldShortTermEncounter2DDefinition<T extends CalculusFieldElement<T>> {

    /** Default threshold below which values are considered equal to zero. */
    private static final double DEFAULT_ZERO_THRESHOLD = 1e-15;

    /** Field to which the instance elements belong. */
    private final Field<T> instanceField;

    /**
     * Time of closest approach.
     * <p>
     * Commonly called TCA.
     */
    private final FieldAbsoluteDate<T> tca;

    /** Reference collision object at time of closest approach. */
    private final FieldOrbit<T> referenceAtTCA;

    /** Reference collision object covariance matrix in its respective RTN frame. */
    private final FieldStateCovariance<T> referenceCovariance;

    /** Other collision object at time of closest approach. */
    private final FieldOrbit<T> otherAtTCA;

    /** Other collision object covariance matrix in its respective RTN frame. */
    private final FieldStateCovariance<T> otherCovariance;

    /** Combined radius (m). */
    private final T combinedRadius;

    /** Encounter local orbital frame to use. */
    private final EncounterLOF encounterFrame;

    /**
     * Constructor.
     *
     * @param referenceAtTCA reference collision object orbit at time of closest approach
     * @param referenceCovariance reference collision object covariance matrix in its respective RTN frame
     * @param referenceRadius reference collision's equivalent sphere radius
     * @param otherAtTCA other collision object  orbit at time of closest approach
     * @param otherCovariance other collision object covariance matrix in its respective RTN frame
     * @param otherRadius other collision's equivalent sphere radius
     *
     * @throws OrekitException If both collision object spacecraft state don't have the same definition date.
     */
    public FieldShortTermEncounter2DDefinition(final FieldOrbit<T> referenceAtTCA,
                                               final FieldStateCovariance<T> referenceCovariance,
                                               final T referenceRadius,
                                               final FieldOrbit<T> otherAtTCA,
                                               final FieldStateCovariance<T> otherCovariance,
                                               final T otherRadius) {
        this(referenceAtTCA, referenceCovariance, otherAtTCA, otherCovariance, referenceRadius.add(otherRadius));
    }

    /**
     * Constructor.
     *
     * @param referenceAtTCA reference collision object orbit at time of closest approach
     * @param referenceCovariance reference collision object covariance matrix in its respective RTN frame
     * @param otherAtTCA other collision object  orbit at time of closest approach
     * @param otherCovariance other collision object covariance matrix in its respective RTN frame
     * @param combinedRadius combined radius (m)
     *
     * @throws OrekitException If both collision object spacecraft state don't have the same definition date.
     */
    public FieldShortTermEncounter2DDefinition(final FieldOrbit<T> referenceAtTCA,
                                               final FieldStateCovariance<T> referenceCovariance,
                                               final FieldOrbit<T> otherAtTCA,
                                               final FieldStateCovariance<T> otherCovariance,
                                               final T combinedRadius) {
        this(referenceAtTCA, referenceCovariance, otherAtTCA, otherCovariance, combinedRadius,
             EncounterLOFType.DEFAULT, 1e-6);
    }

    /**
     * Constructor.
     *
     * @param referenceAtTCA reference collision object orbit at time of closest approach
     * @param referenceCovariance reference collision object covariance matrix in its respective RTN frame
     * @param referenceRadius reference collision's equivalent sphere radius
     * @param otherAtTCA other collision object  orbit at time of closest approach
     * @param otherCovariance other collision object covariance matrix in its respective RTN frame
     * @param otherRadius other collision's equivalent sphere radius
     * @param encounterFrameType type of encounter frame to use
     * @param tcaTolerance tolerance on reference and other times of closest approach difference
     *
     * @throws OrekitException If both collision object spacecraft state don't have the same definition date.
     */
    public FieldShortTermEncounter2DDefinition(final FieldOrbit<T> referenceAtTCA,
                                               final FieldStateCovariance<T> referenceCovariance,
                                               final T referenceRadius,
                                               final FieldOrbit<T> otherAtTCA,
                                               final FieldStateCovariance<T> otherCovariance,
                                               final T otherRadius,
                                               final EncounterLOFType encounterFrameType,
                                               final double tcaTolerance) {
        this(referenceAtTCA, referenceCovariance, otherAtTCA, otherCovariance, referenceRadius.add(otherRadius),
             encounterFrameType, tcaTolerance);
    }

    /**
     * Constructor.
     *
     * @param referenceAtTCA reference collision object orbit at time of closest approach
     * @param referenceCovariance reference collision object covariance matrix in its respective RTN frame
     * @param otherAtTCA other collision object  orbit at time of closest approach
     * @param otherCovariance other collision object covariance matrix in its respective RTN frame
     * @param combinedRadius combined radius (m)
     * @param encounterFrameType type of encounter frame to use
     * @param tcaTolerance tolerance on reference and other times of closest approach difference
     *
     * @throws OrekitException If both collision object spacecraft state don't have the same definition date.
     */
    public FieldShortTermEncounter2DDefinition(final FieldOrbit<T> referenceAtTCA,
                                               final FieldStateCovariance<T> referenceCovariance,
                                               final FieldOrbit<T> otherAtTCA,
                                               final FieldStateCovariance<T> otherCovariance,
                                               final T combinedRadius,
                                               final EncounterLOFType encounterFrameType,
                                               final double tcaTolerance) {

        if (referenceAtTCA.getDate().isCloseTo(otherAtTCA.getDate(), tcaTolerance)) {

            this.tca           = referenceAtTCA.getDate();
            this.instanceField = tca.getField();

            this.referenceAtTCA      = referenceAtTCA;
            this.referenceCovariance = referenceCovariance;

            this.otherAtTCA      = otherAtTCA;
            this.otherCovariance = otherCovariance;

            this.combinedRadius = combinedRadius;

            this.encounterFrame = encounterFrameType.getFrame(otherAtTCA.getPVCoordinates());
        } else {
            throw new OrekitException(OrekitMessages.DIFFERENT_TIME_OF_CLOSEST_APPROACH);
        }

    }

    /**
     * Compute the squared Mahalanobis distance.
     *
     * @param xm other collision object projected xm position onto the collision plane in the rotated encounter frame
     * @param ym other collision object projected ym position onto the collision plane in the rotated encounter frame
     * @param sigmaX square root of the x-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane
     * @param sigmaY square root of the y-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane
     * @param <T> type of the field elements
     *
     * @return squared Mahalanobis distance
     */
    public static <T extends CalculusFieldElement<T>> T computeSquaredMahalanobisDistance(final T xm, final T ym,
                                                                                          final T sigmaX, final T sigmaY) {

        final T[][] positionData = MathArrays.buildArray(xm.getField(), 2, 1);
        positionData[0][0] = xm;
        positionData[1][0] = ym;
        final FieldVector2D<T> position = new FieldVector2D<>(xm, ym);

        final T[][] covarianceMatrixData = MathArrays.buildArray(sigmaX.getField(), 2, 2);
        covarianceMatrixData[0][0] = sigmaX.multiply(sigmaX);
        covarianceMatrixData[1][1] = sigmaY.multiply(sigmaY);
        final FieldMatrix<T> covariance = new BlockFieldMatrix<>(covarianceMatrixData);

        return computeSquaredMahalanobisDistance(position, covariance);
    }

    /**
     * Compute the squared Mahalanobis distance.
     *
     * @param otherPosition other collision object projected position onto the collision plane in the rotated encounter
     * frame
     * @param covarianceMatrix combined covariance matrix projected onto the collision plane and diagonalized
     * @param <T> type of the field elements
     *
     * @return squared Mahalanobis distance
     */
    public static <T extends CalculusFieldElement<T>> T computeSquaredMahalanobisDistance(
            final FieldVector2D<T> otherPosition,
            final FieldMatrix<T> covarianceMatrix) {

        final FieldMatrix<T> covarianceMatrixInverse = new FieldLUDecomposition<>(covarianceMatrix).getSolver().getInverse();

        final FieldMatrix<T> otherPositionOnCollisionPlaneMatrix = new Array2DRowFieldMatrix<>(otherPosition.toArray());

        return otherPositionOnCollisionPlaneMatrix.transposeMultiply(
                covarianceMatrixInverse.multiply(otherPositionOnCollisionPlaneMatrix)).getEntry(0, 0);
    }

    /**
     * Compute the other collision position and velocity relative to the reference collision object. Expressed in the
     * reference collision object inertial frame.
     *
     * @return other collision position and velocity relative to the reference collision object, expressed in the reference
     * collision object inertial frame.
     */
    public FieldPVCoordinates<T> computeOtherRelativeToReferencePVInReferenceInertial() {

        // Extract reference inertial frame
        final Frame referenceInertial = referenceAtTCA.getFrame();

        // Get PVCoordinates in the same frame
        final FieldPVCoordinates<T> referencePV                = referenceAtTCA.getPVCoordinates();
        final FieldPVCoordinates<T> otherPVInReferenceInertial = otherAtTCA.getPVCoordinates(referenceInertial);

        // Create relative pv expressed in the reference inertial frame
        final FieldVector3D<T> relativePosition = otherPVInReferenceInertial.getPosition()
                                                                            .subtract(referencePV.getPosition());
        final FieldVector3D<T> relativeVelocity = otherPVInReferenceInertial.getVelocity()
                                                                            .subtract(referencePV.getVelocity());

        return new FieldPVCoordinates<>(relativePosition, relativeVelocity);
    }

    /**
     * Compute the projection matrix from the reference collision object inertial frame to the collision plane.
     * <p>
     * Note that this matrix will only rotate from the reference collision object inertial frame to the encounter frame and
     * project onto the collision plane, this is only a rotation.
     * </p>
     *
     * @return projection matrix from the reference collision object inertial frame to the collision plane
     */
    public FieldMatrix<T> computeReferenceInertialToCollisionPlaneProjectionMatrix() {

        // Create transform from reference inertial frame to encounter local orbital frame
        final FieldTransform<T> referenceInertialToEncounterFrameTransform =
                new FieldTransform<>(tca,
                                     computeReferenceInertialToReferenceTNWTransform(),
                                     computeReferenceTNWToEncounterFrameTransform());

        // Create rotation matrix from reference inertial frame to encounter local orbital frame
        final FieldMatrix<T> referenceInertialToEncounterFrameRotationMatrix =
                new Array2DRowFieldMatrix<>(referenceInertialToEncounterFrameTransform.getRotation().getMatrix());

        // Create projection matrix from encounter frame to collision plane
        final FieldMatrix<T> encounterFrameToCollisionPlaneProjectionMatrix =
                encounterFrame.computeProjectionMatrix(tca.getField());

        // Create projection matrix from reference inertial frame to collision plane
        return encounterFrameToCollisionPlaneProjectionMatrix.multiply(referenceInertialToEncounterFrameRotationMatrix);
    }

    /**
     * Compute the combined covariance matrix diagonalized and projected onto the collision plane.
     * <p>
     * Diagonalize projected positional covariance matrix in a specific manner to have
     * <var>&#963;<sub>xx</sub><sup>2</sup> &#8804; &#963;<sub>yy</sub><sup>2</sup></var>.
     *
     * @return combined covariance matrix diagonalized and projected onto the collision plane
     */
    public FieldMatrix<T> computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix() {

        final FieldMatrix<T> covarianceMatrixToDiagonalize = computeProjectedCombinedPositionalCovarianceMatrix();

        final T sigmaXSquared = covarianceMatrixToDiagonalize.getEntry(0, 0);
        final T sigmaYSquared = covarianceMatrixToDiagonalize.getEntry(1, 1);

        final T crossTerm = covarianceMatrixToDiagonalize.getEntry(0, 1);
        final T recurrentTerm = sigmaXSquared.subtract(sigmaYSquared).multiply(0.5).pow(2)
                                             .add(crossTerm.multiply(crossTerm)).sqrt();

        final T eigenValueX = sigmaXSquared.add(sigmaYSquared).multiply(0.5).subtract(recurrentTerm);
        final T eigenValueY = sigmaXSquared.add(sigmaYSquared).multiply(0.5).add(recurrentTerm);

        final FieldMatrix<T> projectedAndDiagonalizedCombinedPositionalCovarianceMatrix =
                new BlockFieldMatrix<>(instanceField, 2, 2);
        projectedAndDiagonalizedCombinedPositionalCovarianceMatrix.setEntry(0, 0, eigenValueX);
        projectedAndDiagonalizedCombinedPositionalCovarianceMatrix.setEntry(0, 1, instanceField.getZero());
        projectedAndDiagonalizedCombinedPositionalCovarianceMatrix.setEntry(1, 0, instanceField.getZero());
        projectedAndDiagonalizedCombinedPositionalCovarianceMatrix.setEntry(1, 1, eigenValueY);

        return projectedAndDiagonalizedCombinedPositionalCovarianceMatrix;
    }

    /**
     * Compute the projected combined covariance matrix onto the collision plane.
     *
     * @return projected combined covariance matrix onto the collision plane
     */
    public FieldMatrix<T> computeProjectedCombinedPositionalCovarianceMatrix() {

        // Compute the positional covariance in the encounter local orbital frame
        final FieldMatrix<T> combinedPositionalCovarianceMatrixInEncounterFrame =
                computeCombinedCovarianceInEncounterFrame().getMatrix().getSubMatrix(0, 2, 0, 2);

        // Project it onto the collision plane
        return encounterFrame.projectOntoCollisionPlane(combinedPositionalCovarianceMatrixInEncounterFrame);
    }

    /**
     * Compute the combined covariance expressed in the encounter frame.
     *
     * @return combined covariance expressed in the encounter frame
     */
    public FieldStateCovariance<T> computeCombinedCovarianceInEncounterFrame() {
        return computeCombinedCovarianceInReferenceTNW().changeCovarianceFrame(referenceAtTCA, encounterFrame);
    }

    /**
     * Compute the other collision object {@link FieldVector2D position} projected onto the collision plane.
     *
     * @return other collision object position projected onto the collision plane
     */
    public FieldVector2D<T> computeOtherPositionInCollisionPlane() {

        // Express other in reference inertial
        final FieldPVCoordinates<T> otherInReferenceInertial = otherAtTCA.getPVCoordinates(referenceAtTCA.getFrame());

        // Express other in reference TNW local orbital frame
        final FieldPVCoordinates<T> otherPVInReferenceTNW =
                computeReferenceInertialToReferenceTNWTransform().transformPVCoordinates(otherInReferenceInertial);

        // Express other in encounter local orbital frame
        final FieldPVCoordinates<T> otherPVInEncounterFrame =
                computeReferenceTNWToEncounterFrameTransform().transformPVCoordinates(
                        otherPVInReferenceTNW);

        return encounterFrame.projectOntoCollisionPlane(otherPVInEncounterFrame.getPosition());

    }

    /**
     * Compute the other collision object {@link FieldVector2D position} in the rotated collision plane.
     * <p>
     * Uses a default zero threshold of 1e-15.
     * <p>
     * The coordinates are often noted xm and ym in probability of collision related papers.
     * </p>
     * <p>
     * The mentioned rotation concerns the rotation that diagonalize the combined covariance matrix inside the collision
     * plane.
     * </p>
     *
     * @return other collision object position in the rotated collision plane
     */
    public FieldVector2D<T> computeOtherPositionInRotatedCollisionPlane() {
        return computeOtherPositionInRotatedCollisionPlane(DEFAULT_ZERO_THRESHOLD);

    }

    /**
     * Compute the other collision object {@link Vector2D position}  in the rotated collision plane.
     * <p>
     * The coordinates are often noted xm and ym in probability of collision related papers.
     * <p>
     * The mentioned rotation concerns the rotation that diagonalize the combined covariance matrix inside the collision
     * plane.
     *
     * @param zeroThreshold threshold below which values are considered equal to zero
     *
     * @return other collision object position in the rotated collision plane
     */
    public FieldVector2D<T> computeOtherPositionInRotatedCollisionPlane(final double zeroThreshold) {

        // Project the other position onto the collision plane
        final FieldMatrix<T> otherPositionInCollisionPlaneMatrix =
                new Array2DRowFieldMatrix<>(computeOtherPositionInCollisionPlane().toArray());

        // Express other in the rotated collision plane
        final FieldMatrix<T> otherPositionRotatedInCollisionPlane =
                computeEncounterPlaneRotationMatrix(zeroThreshold).multiply(otherPositionInCollisionPlaneMatrix);

        return new FieldVector2D<>(otherPositionRotatedInCollisionPlane.getColumn(0));

    }

    /**
     * Compute the Encounter duration (s) evaluated using Coppola's formula described in : "COPPOLA, Vincent, et al.
     * Evaluating the short encounter assumption of the probability of collision formula. 2012."
     * <p>
     * This method is to be used to check the validity of the short-term encounter model. The user is expected to compare the
     * computed duration with the orbital period from both objects and draw its own conclusions.
     * <p>
     * It uses γ = 1e-16 as the resolution of a double is nearly 1e-16 so γ smaller than that are not meaningful to compute.
     *
     * @return encounter duration (s) evaluated using Coppola's formula
     */
    public T computeCoppolaEncounterDuration() {

        // Default value for γ = 1e-16
        final T DEFAULT_ALPHA_C = instanceField.getOne().multiply(5.864);

        final FieldMatrix<T> combinedPositionalCovarianceMatrix = computeCombinedCovarianceInEncounterFrame()
                .getMatrix().getSubMatrix(0, 2, 0, 2);

        // Extract off-plane cross-term matrix
        final FieldMatrix<T> projectionMatrix = encounterFrame.computeProjectionMatrix(instanceField);
        final FieldMatrix<T> axisNormalToCollisionPlane =
                new Array2DRowFieldMatrix<>(encounterFrame.getAxisNormalToCollisionPlane(instanceField).toArray());
        final FieldMatrix<T> offPlaneCrossTermMatrix =
                projectionMatrix.multiply(combinedPositionalCovarianceMatrix.multiply(axisNormalToCollisionPlane));

        // Covariance sub-matrix of the in-plane terms
        final FieldMatrix<T> probabilityDensity =
                encounterFrame.projectOntoCollisionPlane(combinedPositionalCovarianceMatrix);
        final FieldMatrix<T> probabilityDensityInverse =
                new FieldLUDecomposition<>(probabilityDensity).getSolver().getInverse();

        // Recurrent term in Coppola's paper : bᵀb
        final FieldMatrix<T> b = offPlaneCrossTermMatrix.transposeMultiply(probabilityDensityInverse).transpose();
        final T recurrentTerm = b.multiplyTransposed(b).getEntry(0, 0);

        // Position uncertainty normal to collision plane
        final T sigmaSqNormalToPlan = axisNormalToCollisionPlane.transposeMultiply(
                combinedPositionalCovarianceMatrix.multiply(axisNormalToCollisionPlane)).getEntry(0, 0);
        final T sigmaV = sigmaSqNormalToPlan.subtract(b.multiplyTransposed(offPlaneCrossTermMatrix).getEntry(0, 0))
                                            .sqrt();

        final T relativeVelocity = computeOtherRelativeToReferencePVInReferenceInertial().getVelocity().getNorm();

        return DEFAULT_ALPHA_C.multiply(sigmaV).multiply(2 * FastMath.sqrt(2)).add(
                combinedRadius.multiply(recurrentTerm.add(1).sqrt().add(recurrentTerm.sqrt()))).divide(relativeVelocity);
    }

    /**
     * Compute the miss distance at time of closest approach.
     *
     * @return miss distance
     */
    public T computeMissDistance() {

        // Get positions expressed in the same frame at time of closest approach
        final FieldVector3D<T> referencePositionAtTCA = referenceAtTCA.getPosition();
        final FieldVector3D<T> otherPositionAtTCA     = otherAtTCA.getPosition(referenceAtTCA.getFrame());

        // Compute relative position
        final FieldVector3D<T> relativePosition = otherPositionAtTCA.subtract(referencePositionAtTCA);

        return relativePosition.getNorm();
    }

    /**
     * Compute the Mahalanobis distance computed with the other collision object projected onto the collision plane (commonly
     * called B-Plane) and expressed in the rotated encounter frame (frame in which the combined covariance matrix is
     * diagonalized, see {@link #computeEncounterPlaneRotationMatrix(double)} for more details).
     * <p>
     * Uses a default zero threshold of 1e-15 for the computation of the diagonalizing of the projected covariance matrix.
     *
     * @return Mahalanobis distance between the reference and other collision object
     *
     * @see <a href="https://en.wikipedia.org/wiki/Mahalanobis_distance">Mahalanobis distance</a>
     */
    public T computeMahalanobisDistance() {
        return computeMahalanobisDistance(DEFAULT_ZERO_THRESHOLD);
    }

    /**
     * Compute the Mahalanobis distance computed with the other collision object projected onto the collision plane (commonly
     * called B-Plane) and expressed in the rotated encounter frame (frame in which the combined covariance matrix is
     * diagonalized, see {@link #computeEncounterPlaneRotationMatrix(double)} for more details).
     *
     * @param zeroThreshold threshold below which values are considered equal to zero
     *
     * @return Mahalanobis distance between the reference and other collision object
     *
     * @see <a href="https://en.wikipedia.org/wiki/Mahalanobis_distance">Mahalanobis distance</a>
     */
    public T computeMahalanobisDistance(final double zeroThreshold) {
        return computeSquaredMahalanobisDistance(zeroThreshold).sqrt();
    }

    /**
     * Compute the squared Mahalanobis distance computed with the other collision object projected onto the collision plane
     * (commonly called B-Plane) and expressed in the rotated encounter frame (frame in which the combined covariance matrix
     * is diagonalized, see {@link #computeEncounterPlaneRotationMatrix(double)} for more details).
     * <p>
     * Uses a default zero threshold of 1e-15 for the computation of the diagonalizing of the projected covariance matrix.
     *
     * @return squared Mahalanobis distance between the reference and other collision object
     *
     * @see <a href="https://en.wikipedia.org/wiki/Mahalanobis_distance">Mahalanobis distance</a>
     */
    public T computeSquaredMahalanobisDistance() {
        return computeSquaredMahalanobisDistance(DEFAULT_ZERO_THRESHOLD);
    }

    /**
     * Compute the squared Mahalanobis distance computed with the other collision object projected onto the collision plane
     * (commonly called B-Plane) and expressed in the rotated encounter frame (frame in which the combined covariance matrix
     * is diagonalized, see {@link #computeEncounterPlaneRotationMatrix(double)} for more details).
     *
     * @param zeroThreshold threshold below which values are considered equal to zero
     *
     * @return squared Mahalanobis distance between the reference and other collision object
     *
     * @see <a href="https://en.wikipedia.org/wiki/Mahalanobis_distance">Mahalanobis distance</a>
     */
    public T computeSquaredMahalanobisDistance(final double zeroThreshold) {

        final FieldMatrix<T> otherPositionAfterRotationInCollisionPlane =
                new Array2DRowFieldMatrix<>(computeOtherPositionInRotatedCollisionPlane(zeroThreshold).toArray());

        final FieldMatrix<T> inverseCovarianceMatrix =
                new FieldLUDecomposition<>(
                        computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix()).getSolver()
                                                                                            .getInverse();

        return otherPositionAfterRotationInCollisionPlane.transpose().multiply(
                                                                 inverseCovarianceMatrix.multiply(
                                                                         otherPositionAfterRotationInCollisionPlane))
                                                         .getEntry(0, 0);
    }

    /** Get new encounter instance.
     * @return new encounter instance
     */
    public ShortTermEncounter2DDefinition toEncounter() {
        return new ShortTermEncounter2DDefinition(referenceAtTCA.toOrbit(), referenceCovariance.toStateCovariance(),
                                                  otherAtTCA.toOrbit(), otherCovariance.toStateCovariance(),
                                                  combinedRadius.getReal());
    }

    /**
     * Takes both covariance matrices (expressed in their respective RTN local orbital frame) from reference and other
     * collision object with which this instance was created and sum them in the reference collision object TNW local orbital
     * frame.
     *
     * @return combined covariance matrix expressed in the reference collision object TNW local orbital frame
     */
    public FieldStateCovariance<T> computeCombinedCovarianceInReferenceTNW() {

        // Express reference covariance in reference TNW local orbital frame
        final FieldMatrix<T> referenceCovarianceMatrixInTNW =
                referenceCovariance.changeCovarianceFrame(referenceAtTCA, LOFType.TNW_INERTIAL).getMatrix();

        // Express other covariance in reference inertial frame
        final FieldMatrix<T> otherCovarianceMatrixInReferenceInertial =
                otherCovariance.changeCovarianceFrame(otherAtTCA, referenceAtTCA.getFrame()).getMatrix();

        final FieldStateCovariance<T> otherCovarianceInReferenceInertial = new FieldStateCovariance<>(
                otherCovarianceMatrixInReferenceInertial, tca, referenceAtTCA.getFrame(),
                OrbitType.CARTESIAN, PositionAngleType.MEAN);

        // Express other covariance in reference TNW local orbital frame
        final FieldMatrix<T> otherCovarianceMatrixInReferenceTNW = otherCovarianceInReferenceInertial.changeCovarianceFrame(
                referenceAtTCA, LOFType.TNW_INERTIAL).getMatrix();

        // Return the combined covariance expressed in the reference TNW local orbital frame
        return new FieldStateCovariance<>(referenceCovarianceMatrixInTNW.add(otherCovarianceMatrixInReferenceTNW), tca,
                                          LOFType.TNW_INERTIAL);
    }

    /**
     * Compute the {@link FieldTransform transform} from the reference collision object inertial frame of reference to its
     * TNW local orbital frame.
     *
     * @return transform from the reference collision object inertial frame of reference to its TNW local orbital frame
     */
    private FieldTransform<T> computeReferenceInertialToReferenceTNWTransform() {
        return LOFType.TNW.transformFromInertial(tca, referenceAtTCA.getPVCoordinates());
    }

    /**
     * Compute the {@link FieldTransform transform} from the reference collision object TNW local orbital frame to the
     * encounter frame.
     *
     * @return transform from the reference collision object TNW local orbital frame to the encounter frame
     */
    private FieldTransform<T> computeReferenceTNWToEncounterFrameTransform() {
        return LOF.transformFromLOFInToLOFOut(LOFType.TNW_INERTIAL, encounterFrame, tca,
                                              referenceAtTCA.getPVCoordinates());
    }

    /**
     * Compute the rotation matrix that diagonalize the combined positional covariance matrix projected onto the collision
     * plane.
     *
     * @param zeroThreshold threshold below which values are considered equal to zero
     *
     * @return rotation matrix that diagonalize the combined covariance matrix projected onto the collision plane
     */
    private FieldMatrix<T> computeEncounterPlaneRotationMatrix(final double zeroThreshold) {

        final FieldMatrix<T> combinedCovarianceMatrixInEncounterFrame =
                computeCombinedCovarianceInEncounterFrame().getMatrix();

        final FieldMatrix<T> combinedPositionalCovarianceMatrixProjectedOntoBPlane =
                encounterFrame.projectOntoCollisionPlane(
                        combinedCovarianceMatrixInEncounterFrame.getSubMatrix(0, 2, 0, 2));

        final T sigmaXSquared = combinedPositionalCovarianceMatrixProjectedOntoBPlane.getEntry(0, 0);
        final T sigmaYSquared = combinedPositionalCovarianceMatrixProjectedOntoBPlane.getEntry(1, 1);
        final T crossTerm     = combinedPositionalCovarianceMatrixProjectedOntoBPlane.getEntry(0, 1);
        final T correlation   = crossTerm.divide(sigmaXSquared.multiply(sigmaYSquared).sqrt());

        // If the matrix is not initially diagonalized
        final T theta;
        if (FastMath.abs(crossTerm).getReal() > zeroThreshold) {
            final T recurrentTerm = sigmaYSquared.subtract(sigmaXSquared).divide(crossTerm.multiply(2));
            theta = recurrentTerm.subtract(correlation.sign().multiply(recurrentTerm.pow(2).add(1).sqrt())).atan();
        }
        // Else, the matrix is already diagonalized
        else {
            // Rotation in order to have sigmaXSquared < sigmaYSquared
            if (sigmaXSquared.subtract(sigmaYSquared).getReal() > 0) {
                theta = tca.getField().getOne().multiply(MathUtils.SEMI_PI);
            }
            // Else, there is no need for a rotation
            else {
                theta = tca.getField().getZero();
            }
        }

        final T                        cosTheta       = theta.cos();
        final T                        sinTheta       = theta.sin();
        final Array2DRowFieldMatrix<T> rotationMatrix = new Array2DRowFieldMatrix<>(tca.getField(), 2, 2);
        rotationMatrix.setEntry(0, 0, cosTheta);
        rotationMatrix.setEntry(0, 1, sinTheta);
        rotationMatrix.setEntry(1, 0, sinTheta.negate());
        rotationMatrix.setEntry(1, 1, cosTheta);

        return rotationMatrix;
    }

    /**
     * Get the Time of Closest Approach.
     * <p>
     * Commonly called TCA.
     *
     * @return time of closest approach
     */
    public FieldAbsoluteDate<T> getTca() {
        return tca;
    }

    /** Get reference's orbit at time of closest approach.
     * @return reference's orbit at time of closest approach
     */
    public FieldOrbit<T> getReferenceAtTCA() {
        return referenceAtTCA;
    }

    /** Get other's orbit at time of closest approach.
     *  @return other's orbit at time of closest approach
     */
    public FieldOrbit<T> getOtherAtTCA() {
        return otherAtTCA;
    }

    /** Get reference's covariance.
     * @return reference's covariance
     */
    public FieldStateCovariance<T> getReferenceCovariance() {
        return referenceCovariance;
    }

    /** Get other's covariance.
     * @return other's covariance
     */
    public FieldStateCovariance<T> getOtherCovariance() {
        return otherCovariance;
    }

    /** Get combined radius.
     * @return combined radius (m)
     */
    public T getCombinedRadius() {
        return combinedRadius;
    }

    /** Get encounter local orbital frame.
     * @return encounter local orbital frame
     */
    public EncounterLOF getEncounterFrame() {
        return encounterFrame;
    }

}
