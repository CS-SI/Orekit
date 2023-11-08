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
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.EigenDecompositionSymmetric;
import org.hipparchus.linear.LUDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOF;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.frames.encounter.EncounterLOF;
import org.orekit.frames.encounter.EncounterLOFType;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.StateCovariance;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

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
 * final ShortTermEncounter2DDefinition encounter = new ShortTermEncounter2DDefinition(primaryOrbitAtTCA, primaryCovariance, primaryRadius, secondaryOrbitAtTCA, secondaryCovariance, secondaryRadius);
 *  }
 * </pre>
 * However, if the user wants to put the secondary at the reference and use the
 * {@link org.orekit.frames.encounter.ValsecchiEncounterFrame Valsecchi encounter frame}, they will have to type :
 * <pre>{@code
 * final ShortTermEncounter2DDefinition encounter = new ShortTermEncounter2DDefinition(secondaryOrbitAtTCA, secondaryCovariance, secondaryRadius, primaryOrbitAtTCA, primaryCovariance, primaryRadius, EncounterLOFType.VALSECCHI_2003);
 *  }
 * </pre>
 * Note that in the current implementation, the shape of the collision objects is assumed to be a sphere.
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public class ShortTermEncounter2DDefinition {

    /** Default threshold below which values are considered equal to zero. */
    private static final double DEFAULT_ZERO_THRESHOLD = 1e-15;

    /** Default epsilon when checking covariance matrix symmetry. */
    private static final double DEFAULT_SYMMETRY_EPSILON = 1e-8;

    /**
     * Time of closest approach.
     * <p>
     * Commonly called TCA.
     */
    private final AbsoluteDate tca;

    /** Reference collision object at time of closest approach. */
    private final Orbit referenceAtTCA;

    /** Reference collision object covariance matrix in its respective RTN frame. */
    private final StateCovariance referenceCovariance;

    /** Other collision object at time of closest approach. */
    private final Orbit otherAtTCA;

    /** Other collision object covariance matrix in its respective RTN frame. */
    private final StateCovariance otherCovariance;

    /** Combined radius (m). */
    private final double combinedRadius;

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
    public ShortTermEncounter2DDefinition(final Orbit referenceAtTCA, final StateCovariance referenceCovariance,
                                          final double referenceRadius, final Orbit otherAtTCA,
                                          final StateCovariance otherCovariance, final double otherRadius) {
        this(referenceAtTCA, referenceCovariance, otherAtTCA, otherCovariance, referenceRadius + otherRadius);
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
    public ShortTermEncounter2DDefinition(final Orbit referenceAtTCA, final StateCovariance referenceCovariance,
                                          final Orbit otherAtTCA, final StateCovariance otherCovariance,
                                          final double combinedRadius) {
        this(referenceAtTCA, referenceCovariance, otherAtTCA, otherCovariance, combinedRadius, EncounterLOFType.DEFAULT,
             1e-6);
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
    public ShortTermEncounter2DDefinition(final Orbit referenceAtTCA, final StateCovariance referenceCovariance,
                                          final double referenceRadius, final Orbit otherAtTCA,
                                          final StateCovariance otherCovariance, final double otherRadius,
                                          final EncounterLOFType encounterFrameType, final double tcaTolerance) {
        this(referenceAtTCA, referenceCovariance, otherAtTCA, otherCovariance, referenceRadius + otherRadius,
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
    public ShortTermEncounter2DDefinition(final Orbit referenceAtTCA, final StateCovariance referenceCovariance,
                                          final Orbit otherAtTCA, final StateCovariance otherCovariance,
                                          final double combinedRadius, final EncounterLOFType encounterFrameType,
                                          final double tcaTolerance) {

        if (referenceAtTCA.getDate().isCloseTo(otherAtTCA.getDate(), tcaTolerance)) {

            this.tca = referenceAtTCA.getDate();

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
     *
     * @return squared Mahalanobis distance
     */
    public static double computeSquaredMahalanobisDistance(final double xm, final double ym,
                                                           final double sigmaX, final double sigmaY) {
        final Vector2D position = new Vector2D(xm, ym);

        final RealMatrix covariance = new Array2DRowRealMatrix(new double[][] {
                { sigmaX * sigmaX, 0 },
                { 0, sigmaY * sigmaY } });

        return computeSquaredMahalanobisDistance(position, covariance);
    }

    /**
     * Compute the squared Mahalanobis distance.
     *
     * @param otherPosition other collision object projected position onto the collision plane in the rotated encounter
     * frame
     * @param covarianceMatrix combined covariance matrix projected onto the collision plane and diagonalized
     *
     * @return squared Mahalanobis distance
     */
    public static double computeSquaredMahalanobisDistance(final Vector2D otherPosition, final RealMatrix covarianceMatrix) {

        final RealMatrix covarianceMatrixInverse = new LUDecomposition(covarianceMatrix).getSolver().getInverse();

        final RealMatrix otherPositionOnCollisionPlaneMatrix = new Array2DRowRealMatrix(otherPosition.toArray());

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
    public PVCoordinates computeOtherRelativeToReferencePVInReferenceInertial() {

        // Extract reference inertial frame
        final Frame referenceInertial = referenceAtTCA.getFrame();

        // Get PVCoordinates in the same frame
        final PVCoordinates referencePV                = referenceAtTCA.getPVCoordinates();
        final PVCoordinates otherPVInReferenceInertial = otherAtTCA.getPVCoordinates(referenceInertial);

        // Create relative pv expressed in the reference inertial frame
        final Vector3D relativePosition = otherPVInReferenceInertial.getPosition().subtract(referencePV.getPosition());
        final Vector3D relativeVelocity = otherPVInReferenceInertial.getVelocity().subtract(referencePV.getVelocity());

        return new PVCoordinates(relativePosition, relativeVelocity);
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
    public RealMatrix computeReferenceInertialToCollisionPlaneProjectionMatrix() {

        // Create transform from reference inertial frame to encounter local orbital frame
        final Transform referenceInertialToEncounterFrameTransform =
                new Transform(tca,
                              computeReferenceInertialToReferenceTNWTransform(),
                              computeReferenceTNWToEncounterFrameTransform());

        // Create rotation matrix from reference inertial frame to encounter local orbital frame
        final RealMatrix referenceInertialToEncounterFrameRotationMatrix = new Array2DRowRealMatrix(
                referenceInertialToEncounterFrameTransform.getRotation().getMatrix());

        // Create projection matrix from encounter frame to collision plane
        final RealMatrix encounterFrameToCollisionPlaneProjectionMatrix = encounterFrame.computeProjectionMatrix();

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
    public RealMatrix computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix() {
        final RealMatrix covariance = computeProjectedCombinedPositionalCovarianceMatrix();
        final EigenDecompositionSymmetric ed = new EigenDecompositionSymmetric(covariance, DEFAULT_SYMMETRY_EPSILON, false);
        return ed.getD();
    }

    /**
     * Compute the projected combined covariance matrix onto the collision plane.
     *
     * @return projected combined covariance matrix onto the collision plane
     */
    public RealMatrix computeProjectedCombinedPositionalCovarianceMatrix() {

        // Compute the positional covariance in the encounter local orbital frame
        final RealMatrix combinedPositionalCovarianceMatrixInEncounterFrame =
                computeCombinedCovarianceInEncounterFrame().getMatrix().getSubMatrix(0, 2, 0, 2);

        // Project it onto the collision plane
        return encounterFrame.projectOntoCollisionPlane(combinedPositionalCovarianceMatrixInEncounterFrame);
    }

    /**
     * Compute the combined covariance expressed in the encounter frame.
     *
     * @return combined covariance expressed in the encounter frame
     */
    public StateCovariance computeCombinedCovarianceInEncounterFrame() {
        return computeCombinedCovarianceInReferenceTNW().changeCovarianceFrame(referenceAtTCA, encounterFrame);
    }

    /**
     * Compute the other collision object {@link Vector2D position} projected onto the collision plane.
     *
     * @return other collision object position projected onto the collision plane
     */
    public Vector2D computeOtherPositionInCollisionPlane() {

        // Express other in reference inertial
        final PVCoordinates otherInReferenceInertial = otherAtTCA.getPVCoordinates(referenceAtTCA.getFrame());

        // Express other in reference TNW local orbital frame
        final PVCoordinates otherPVInReferenceTNW =
                computeReferenceInertialToReferenceTNWTransform().transformPVCoordinates(otherInReferenceInertial);

        // Express other in encounter local orbital frame
        final PVCoordinates otherPVInEncounterFrame =
                computeReferenceTNWToEncounterFrameTransform().transformPVCoordinates(
                        otherPVInReferenceTNW);

        return encounterFrame.projectOntoCollisionPlane(otherPVInEncounterFrame.getPosition());

    }

    /**
     * Compute the other collision object {@link Vector2D position} in the rotated collision plane.
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
    public Vector2D computeOtherPositionInRotatedCollisionPlane() {
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
    public Vector2D computeOtherPositionInRotatedCollisionPlane(final double zeroThreshold) {

        // Project the other position onto the collision plane
        final RealMatrix otherPositionInCollisionPlaneMatrix =
                new Array2DRowRealMatrix(computeOtherPositionInCollisionPlane().toArray());

        // Express other in the rotated collision plane
        final RealMatrix otherPositionRotatedInCollisionPlane =
                computeEncounterPlaneRotationMatrix(zeroThreshold).multiply(otherPositionInCollisionPlaneMatrix);

        return new Vector2D(otherPositionRotatedInCollisionPlane.getColumn(0));

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
    public double computeCoppolaEncounterDuration() {

        // Default value for γ = 1e-16
        final double DEFAULT_ALPHA_C = 5.864;

        final RealMatrix combinedPositionalCovarianceMatrix = computeCombinedCovarianceInEncounterFrame()
                .getMatrix().getSubMatrix(0, 2, 0, 2);

        // Extract off-plane cross-term matrix
        final RealMatrix projectionMatrix = encounterFrame.computeProjectionMatrix();
        final RealMatrix axisNormalToCollisionPlane =
                new Array2DRowRealMatrix(encounterFrame.getAxisNormalToCollisionPlane().toArray());
        final RealMatrix offPlaneCrossTermMatrix =
                projectionMatrix.multiply(combinedPositionalCovarianceMatrix.multiply(axisNormalToCollisionPlane));

        // Covariance sub-matrix of the in-plane terms
        final RealMatrix probabilityDensity =
                encounterFrame.projectOntoCollisionPlane(combinedPositionalCovarianceMatrix);
        final RealMatrix probabilityDensityInverse =
                new LUDecomposition(probabilityDensity).getSolver().getInverse();

        // Recurrent term in Coppola's paper : bᵀb
        final RealMatrix b             = offPlaneCrossTermMatrix.transposeMultiply(probabilityDensityInverse).transpose();
        final double     recurrentTerm = b.multiplyTransposed(b).getEntry(0, 0);

        // Position uncertainty normal to collision plane
        final double sigmaSqNormalToPlan = axisNormalToCollisionPlane.transposeMultiply(
                combinedPositionalCovarianceMatrix.multiply(axisNormalToCollisionPlane)).getEntry(0, 0);
        final double sigmaV = FastMath.sqrt(
                sigmaSqNormalToPlan - b.multiplyTransposed(offPlaneCrossTermMatrix).getEntry(0, 0));

        final double relativeVelocity = computeOtherRelativeToReferencePVInReferenceInertial().getVelocity().getNorm();

        return (2 * FastMath.sqrt(2) * DEFAULT_ALPHA_C * sigmaV + combinedRadius * (
                FastMath.sqrt(1 + recurrentTerm) + FastMath.sqrt(recurrentTerm))) / relativeVelocity;
    }

    /**
     * Compute the miss distance at time of closest approach.
     *
     * @return miss distance
     */
    public double computeMissDistance() {

        // Get positions expressed in the same frame at time of closest approach
        final Vector3D referencePositionAtTCA = referenceAtTCA.getPosition();
        final Vector3D otherPositionAtTCA     = otherAtTCA.getPosition(referenceAtTCA.getFrame());

        // Compute relative position
        final Vector3D relativePosition = otherPositionAtTCA.subtract(referencePositionAtTCA);

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
    public double computeMahalanobisDistance() {
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
    public double computeMahalanobisDistance(final double zeroThreshold) {
        return FastMath.sqrt(computeSquaredMahalanobisDistance(zeroThreshold));
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
    public double computeSquaredMahalanobisDistance() {
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
    public double computeSquaredMahalanobisDistance(final double zeroThreshold) {

        final RealMatrix otherPositionAfterRotationInCollisionPlane =
                new Array2DRowRealMatrix(computeOtherPositionInRotatedCollisionPlane(zeroThreshold).toArray());

        final RealMatrix inverseCovarianceMatrix =
                new LUDecomposition(computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix()).getSolver()
                                                                                                        .getInverse();

        return otherPositionAfterRotationInCollisionPlane.transpose().multiply(
                inverseCovarianceMatrix.multiply(otherPositionAfterRotationInCollisionPlane)).getEntry(0, 0);
    }

    /**
     * Takes both covariance matrices (expressed in their respective RTN local orbital frame) from reference and other
     * collision object with which this instance was created and sum them in the reference collision object TNW local orbital
     * frame.
     *
     * @return combined covariance matrix expressed in the reference collision object TNW local orbital frame
     */
    public StateCovariance computeCombinedCovarianceInReferenceTNW() {

        // Express reference covariance in reference TNW local orbital frame
        final RealMatrix referenceCovarianceMatrixInTNW =
                referenceCovariance.changeCovarianceFrame(referenceAtTCA, LOFType.TNW_INERTIAL).getMatrix();

        // Express other covariance in reference inertial frame
        final RealMatrix otherCovarianceMatrixInReferenceInertial =
                otherCovariance.changeCovarianceFrame(otherAtTCA, referenceAtTCA.getFrame()).getMatrix();

        final StateCovariance otherCovarianceInReferenceInertial = new StateCovariance(
                otherCovarianceMatrixInReferenceInertial, tca, referenceAtTCA.getFrame(),
                OrbitType.CARTESIAN, PositionAngleType.MEAN);

        // Express other covariance in reference TNW local orbital frame
        final RealMatrix otherCovarianceMatrixInReferenceTNW = otherCovarianceInReferenceInertial.changeCovarianceFrame(
                referenceAtTCA, LOFType.TNW_INERTIAL).getMatrix();

        // Return the combined covariance expressed in the reference TNW local orbital frame
        return new StateCovariance(referenceCovarianceMatrixInTNW.add(otherCovarianceMatrixInReferenceTNW), tca,
                                   LOFType.TNW_INERTIAL);
    }

    /**
     * Compute the {@link Transform transform} from the reference collision object inertial frame of reference to its TNW
     * local orbital frame.
     *
     * @return transform from the reference collision object inertial frame of reference to its TNW local orbital frame
     */
    private Transform computeReferenceInertialToReferenceTNWTransform() {
        return LOFType.TNW.transformFromInertial(tca, referenceAtTCA.getPVCoordinates());
    }

    /**
     * Compute the {@link Transform transform} from the reference collision object TNW local orbital frame to the encounter
     * frame.
     *
     * @return transform from the reference collision object TNW local orbital frame to the encounter frame
     */
    private Transform computeReferenceTNWToEncounterFrameTransform() {
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
    private RealMatrix computeEncounterPlaneRotationMatrix(final double zeroThreshold) {

        final RealMatrix combinedCovarianceMatrixInEncounterFrame =
                computeCombinedCovarianceInEncounterFrame().getMatrix();

        final RealMatrix combinedPositionalCovarianceMatrixProjectedOntoBPlane =
                encounterFrame.projectOntoCollisionPlane(
                        combinedCovarianceMatrixInEncounterFrame.getSubMatrix(0, 2, 0, 2));

        final double sigmaXSquared = combinedPositionalCovarianceMatrixProjectedOntoBPlane.getEntry(0, 0);
        final double sigmaYSquared = combinedPositionalCovarianceMatrixProjectedOntoBPlane.getEntry(1, 1);
        final double crossTerm     = combinedPositionalCovarianceMatrixProjectedOntoBPlane.getEntry(0, 1);
        final double correlation   = crossTerm / (FastMath.sqrt(sigmaXSquared * sigmaYSquared));

        // If the matrix is not initially diagonalized
        final double theta;
        if (FastMath.abs(crossTerm) > zeroThreshold) {
            final double recurrentTerm = (sigmaYSquared - sigmaXSquared) / (2 * crossTerm);
            theta = FastMath.atan(
                    recurrentTerm - FastMath.signum(correlation) * FastMath.sqrt(1 + FastMath.pow(recurrentTerm, 2)));
        }
        // Else, the matrix is already diagonalized
        else {
            // Rotation in order to have sigmaXSquared < sigmaYSquared
            if (sigmaXSquared - sigmaYSquared > 0) {
                theta = MathUtils.SEMI_PI;
            }
            // Else, there is no need for a rotation
            else {
                theta = 0;
            }
        }

        final double[][] collisionPlaneRotationMatrixData = { { FastMath.cos(theta), FastMath.sin(theta) },
                                                              { -FastMath.sin(theta), FastMath.cos(theta) } };

        return new Array2DRowRealMatrix(collisionPlaneRotationMatrixData);
    }

    /**
     * Get the Time of Closest Approach.
     * <p>
     * Commonly called TCA.
     *
     * @return time of closest approach
     */
    public AbsoluteDate getTca() {
        return tca;
    }

    /** Get reference's orbit at time of closest approach.
     * @return reference's orbit at time of closest approach
     */
    public Orbit getReferenceAtTCA() {
        return referenceAtTCA;
    }

    /** Get other's orbit at time of closest approach.
     *  @return other's orbit at time of closest approach
     */
    public Orbit getOtherAtTCA() {
        return otherAtTCA;
    }

    /** Get reference's covariance.
     * @return reference's covariance
     */
    public StateCovariance getReferenceCovariance() {
        return referenceCovariance;
    }

    /** Get other's covariance.
     * @return other's covariance
     */
    public StateCovariance getOtherCovariance() {
        return otherCovariance;
    }

    /** Get combined radius.
     * @return combined radius (m)
     */
    public double getCombinedRadius() {
        return combinedRadius;
    }

    /** Get encounter local orbital frame.
     * @return encounter local orbital frame
     */
    public EncounterLOF getEncounterFrame() {
        return encounterFrame;
    }

}
