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
import org.hipparchus.analysis.integration.FieldTrapezoidIntegrator;
import org.hipparchus.analysis.integration.FieldUnivariateIntegrator;
import org.hipparchus.analysis.integration.UnivariateIntegrator;
import org.hipparchus.geometry.euclidean.twod.FieldVector2D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.cdm.Cdm;
import org.orekit.files.ccsds.ndm.cdm.CdmData;
import org.orekit.files.ccsds.ndm.cdm.CdmMetadata;
import org.orekit.files.ccsds.ndm.cdm.CdmRelativeMetadata;
import org.orekit.frames.encounter.EncounterLOFType;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldStateCovariance;
import org.orekit.propagation.StateCovariance;
import org.orekit.ssa.metrics.FieldProbabilityOfCollision;
import org.orekit.ssa.metrics.ProbabilityOfCollision;
import org.orekit.utils.Fieldifier;

/**
 * This abstract class serves as a foundation to create 1D numerical 2D probability of collision computing method.
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public abstract class AbstractShortTermEncounter1DNumerical2DPOCMethod
        extends AbstractShortTermEncounter2DPOCMethod {

    /** Default univariate function numerical integrator. */
    private final UnivariateIntegrator integrator;

    /** Default maximum number of function evaluation when integrating. */
    private final int maxNbOfEval;

    /**
     * Customizable constructor.
     *
     * @param name name of the method
     * @param integrator integrator
     * @param maxNbOfEval max number of evaluation
     */
    protected AbstractShortTermEncounter1DNumerical2DPOCMethod(final String name,
                                                               final UnivariateIntegrator integrator,
                                                               final int maxNbOfEval) {
        super(name);

        this.integrator  = integrator;
        this.maxNbOfEval = maxNbOfEval;
    }

    /**
     * Compute the probability of collision using an {@link Cdm Orekit Conjunction Data Message}.
     *
     * @param cdm conjunction data message input
     * @param primaryRadius primary collision object equivalent sphere radius (m)
     * @param secondaryRadius secondary collision object equivalent sphere radius (m)
     * @param customIntegrator different univariate function numerical integrator than the one defined in the instance
     * @param customMaxNbOfEval different maximum number of function evaluation when integrating than the one defined in the
     * instance
     * @param zeroThreshold threshold below which values are considered equal to zero
     *
     * @return probability of collision
     */
    public ProbabilityOfCollision compute(final Cdm cdm, final double primaryRadius, final double secondaryRadius,
                                          final UnivariateIntegrator customIntegrator, final int customMaxNbOfEval,
                                          final double zeroThreshold) {

        final CdmRelativeMetadata cdmRelativeMetadata = cdm.getRelativeMetadata();
        final CdmData             primaryData         = cdm.getDataObject1();
        final CdmData             secondaryData       = cdm.getDataObject2();
        final DataContext         cdmDataContext      = cdm.getDataContext();

        // Extract primary data
        final Orbit primaryOrbit =
                getObjectOrbitFromCdm(cdmRelativeMetadata, primaryData, cdm.getMetadataObject1(), cdmDataContext);
        final StateCovariance primaryCovariance = getObjectStateCovarianceFromCdm(cdmRelativeMetadata, primaryData);

        // Extract secondary data
        final Orbit secondaryOrbit =
                getObjectOrbitFromCdm(cdmRelativeMetadata, secondaryData, cdm.getMetadataObject2(), cdmDataContext);
        final StateCovariance secondaryCovariance = getObjectStateCovarianceFromCdm(cdmRelativeMetadata, secondaryData);

        return compute(primaryOrbit, primaryCovariance, primaryRadius,
                       secondaryOrbit, secondaryCovariance, secondaryRadius,
                       customIntegrator, customMaxNbOfEval, zeroThreshold);
    }

    /**
     * Compute the probability of collision using an {@link Cdm Orekit Conjunction Data Message}.
     *
     * @param cdm conjunction data message
     * @param primaryRadius primary collision object equivalent sphere radius (m)
     * @param secondaryRadius secondary collision object equivalent sphere radius (m)
     * @param zeroThreshold threshold below which values are considered equal to zero
     * @param customIntegrator different univariate function numerical integrator than the one defined in the instance
     * @param customMaxNbOfEval different maximum number of function evaluation when integrating than the one defined in the
     * instance
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    public <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(final Cdm cdm,
                                                                                      final T primaryRadius,
                                                                                      final T secondaryRadius,
                                                                                      final FieldUnivariateIntegrator<T> customIntegrator,
                                                                                      final int customMaxNbOfEval,
                                                                                      final double zeroThreshold) {

        final Field<T>            field               = primaryRadius.getField();
        final CdmRelativeMetadata cdmRelativeMetadata = cdm.getRelativeMetadata();
        final CdmData             primaryData         = cdm.getDataObject1();
        final CdmData             secondaryData       = cdm.getDataObject2();
        final CdmMetadata         primaryMetadata     = cdm.getMetadataObject1();
        final CdmMetadata         secondaryMetadata   = cdm.getMetadataObject2();
        final DataContext         cdmDataContext      = cdm.getDataContext();

        // Extract primary data
        final FieldOrbit<T> primaryOrbit =
                Fieldifier.fieldify(field, getObjectOrbitFromCdm(cdmRelativeMetadata, primaryData,
                                                                 primaryMetadata, cdmDataContext));
        final FieldStateCovariance<T> primaryCovariance =
                Fieldifier.fieldify(field, getObjectStateCovarianceFromCdm(cdmRelativeMetadata, primaryData));

        // Extract secondary data
        final FieldOrbit<T> secondaryOrbit =
                Fieldifier.fieldify(field, getObjectOrbitFromCdm(cdmRelativeMetadata, secondaryData,
                                                                 secondaryMetadata, cdmDataContext));
        final FieldStateCovariance<T> secondaryCovariance =
                Fieldifier.fieldify(field, getObjectStateCovarianceFromCdm(cdmRelativeMetadata, secondaryData));

        return compute(primaryOrbit, primaryCovariance, primaryRadius,
                       secondaryOrbit, secondaryCovariance, secondaryRadius,
                       customIntegrator, customMaxNbOfEval, zeroThreshold);
    }

    /**
     * Compute the probability of collision using parameters necessary for creating a
     * {@link ShortTermEncounter2DDefinition collision definition} instance.
     *
     * @param primaryAtTCA primary collision object spacecraft state at time of closest approach
     * @param primaryCovariance primary collision object covariance
     * @param primaryRadius primary collision object equivalent sphere radius (m)
     * @param secondaryAtTCA secondary collision object spacecraft state at time of closest approach
     * @param secondaryCovariance secondary collision object covariance
     * @param secondaryRadius secondary collision object equivalent sphere radius (m)
     * @param customIntegrator different univariate function numerical integrator than the one defined in the instance
     * @param customMaxNbOfEval different maximum number of function evaluation when integrating than the one defined in the
     * instance
     * @param zeroThreshold threshold below which values are considered equal to zero
     *
     * @return probability of collision
     */
    public ProbabilityOfCollision compute(final Orbit primaryAtTCA,
                                          final StateCovariance primaryCovariance,
                                          final double primaryRadius,
                                          final Orbit secondaryAtTCA,
                                          final StateCovariance secondaryCovariance,
                                          final double secondaryRadius,
                                          final UnivariateIntegrator customIntegrator,
                                          final int customMaxNbOfEval,
                                          final double zeroThreshold) {

        final ShortTermEncounter2DDefinition encounterDefinition = new ShortTermEncounter2DDefinition(
                primaryAtTCA, primaryCovariance, primaryRadius,
                secondaryAtTCA, secondaryCovariance, secondaryRadius,
                EncounterLOFType.DEFAULT, DEFAULT_TCA_DIFFERENCE_TOLERANCE);

        return compute(encounterDefinition, customIntegrator, customMaxNbOfEval, zeroThreshold);
    }

    /**
     * Compute the probability of collision using parameters necessary for creating a
     * {@link ShortTermEncounter2DDefinition collision definition} instance.
     *
     * @param primaryAtTCA primary collision object spacecraft state at time of closest approach
     * @param primaryCovariance primary collision object covariance
     * @param primaryRadius primary collision object equivalent sphere radius (m)
     * @param secondaryAtTCA secondary collision object spacecraft state at time of closest approach
     * @param secondaryCovariance secondary collision object covariance
     * @param secondaryRadius secondary collision object equivalent sphere radius (m)
     * @param customIntegrator different univariate function numerical integrator than the one defined in the instance
     * @param customMaxNbOfEval different maximum number of function evaluation when integrating than the one defined in the
     * instance
     * @param zeroThreshold threshold below which values are considered equal to zero
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    public <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(
            final FieldOrbit<T> primaryAtTCA,
            final FieldStateCovariance<T> primaryCovariance,
            final T primaryRadius,
            final FieldOrbit<T> secondaryAtTCA,
            final FieldStateCovariance<T> secondaryCovariance,
            final T secondaryRadius,
            final FieldUnivariateIntegrator<T> customIntegrator,
            final int customMaxNbOfEval,
            final double zeroThreshold) {

        final FieldShortTermEncounter2DDefinition<T> encounterDefinition =
                new FieldShortTermEncounter2DDefinition<>(
                        primaryAtTCA, primaryCovariance, primaryRadius,
                        secondaryAtTCA, secondaryCovariance, secondaryRadius,
                        EncounterLOFType.DEFAULT, DEFAULT_TCA_DIFFERENCE_TOLERANCE);

        return compute(encounterDefinition, customIntegrator, customMaxNbOfEval, zeroThreshold);
    }

    /**
     * Compute the probability of collision using a given collision definition.
     *
     * @param encounterDefinition probabilityOfCollision definition between a primary and a secondary collision object
     * @param customIntegrator different univariate function numerical integrator than the one defined in the instance
     * @param customMaxNbOfEval different maximum number of function evaluation when integrating than the one defined in the
     * instance
     * @param zeroThreshold threshold below which values are considered equal to zero
     *
     * @return probability of collision
     */
    public ProbabilityOfCollision compute(final ShortTermEncounter2DDefinition encounterDefinition,
                                          final UnivariateIntegrator customIntegrator,
                                          final int customMaxNbOfEval,
                                          final double zeroThreshold) {

        final Vector2D otherPositionAfterRotationInCollisionPlane =
                encounterDefinition.computeOtherPositionInRotatedCollisionPlane(zeroThreshold);

        final RealMatrix projectedDiagonalizedCombinedPositionalCovarianceMatrix =
                encounterDefinition.computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix();

        return compute(otherPositionAfterRotationInCollisionPlane.getX(),
                       otherPositionAfterRotationInCollisionPlane.getY(),
                       FastMath.sqrt(projectedDiagonalizedCombinedPositionalCovarianceMatrix.getEntry(0, 0)),
                       FastMath.sqrt(projectedDiagonalizedCombinedPositionalCovarianceMatrix.getEntry(1, 1)),
                       encounterDefinition.getCombinedRadius(),
                       customIntegrator,
                       customMaxNbOfEval);
    }

    /**
     * Compute the probability of collision using given collision definition.
     *
     * @param encounterDefinition encounter definition between a primary and a secondary collision object
     * @param customIntegrator custom integrator to use in place of the integrator from the constructor
     * @param customMaxNbOfEval custom maximum number of evaluations to use in place of the custom maximum number from the
     * @param zeroThreshold threshold below which values are considered equal to zero
     * @param <T> type of the field element
     *
     * @return probability of collision
     */
    public <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(
            final FieldShortTermEncounter2DDefinition<T> encounterDefinition,
            final FieldUnivariateIntegrator<T> customIntegrator,
            final int customMaxNbOfEval,
            final double zeroThreshold) {

        final FieldVector2D<T> otherPositionAfterRotationInCollisionPlane =
                encounterDefinition.computeOtherPositionInRotatedCollisionPlane(zeroThreshold);

        final FieldMatrix<T> projectedDiagonalizedCombinedPositionalCovarianceMatrix =
                encounterDefinition.computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix();

        return compute(otherPositionAfterRotationInCollisionPlane.getX(),
                       otherPositionAfterRotationInCollisionPlane.getY(),
                       projectedDiagonalizedCombinedPositionalCovarianceMatrix.getEntry(0, 0).sqrt(),
                       projectedDiagonalizedCombinedPositionalCovarianceMatrix.getEntry(1, 1).sqrt(),
                       encounterDefinition.getCombinedRadius(),
                       customIntegrator,
                       customMaxNbOfEval);
    }

    /**
     * {@inheritDoc}
     * <p>
     * It uses the defaults integrator and maximum number of function evaluation when integrating.
     */
    public ProbabilityOfCollision compute(final double xm, final double ym, final double sigmaX, final double sigmaY,
                                          final double radius) {
        return compute(xm, ym, sigmaX, sigmaY, radius, integrator, maxNbOfEval);
    }

    /**
     * Compute the probability of collision using arguments specific to the rotated encounter frame.
     * <p>
     * The rotated encounter frame is define by the initial encounter frame (defined in
     * {@link ShortTermEncounter2DDefinition}) rotated by the rotation matrix which is used to diagonalize the combined
     * covariance matrix.
     * </p>
     *
     * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
     * (m)
     * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
     * (m)
     * @param sigmaX square root of the x-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param sigmaY square root of the y-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
     * @param <T> type of the field elements
     *
     * @return probability of collision
     */
    public <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(final T xm, final T ym,
                                                                                      final T sigmaX, final T sigmaY,
                                                                                      final T radius) {
        return compute(xm, ym, sigmaX, sigmaY, radius, new FieldTrapezoidIntegrator<>(xm.getField()),
                       maxNbOfEval);
    }

    /**
     * Compute the probability of collision using arguments specific to the rotated encounter frame and custom numerical
     * configuration.
     * <p>
     * The rotated encounter frame is define by the initial encounter frame (defined in
     * {@link ShortTermEncounter2DDefinition}) rotated by the rotation matrix which is used to diagonalize the combined
     * covariance matrix.
     * </p>
     *
     * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
     * (m)
     * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
     * (m)
     * @param sigmaX square root of the x-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param sigmaY square root of the y-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
     * @param customIntegrator custom integrator to use in place of the integrator from the constructor
     * @param customMaxNbOfEval custom maximum number of evaluations to use in place of the custom maximum number from the
     * constructor
     *
     * @return probability of collision
     */
    public abstract ProbabilityOfCollision compute(double xm, double ym, double sigmaX, double sigmaY, double radius,
                                                   UnivariateIntegrator customIntegrator, int customMaxNbOfEval);

    /**
     * Compute the probability of collision using arguments specific to the rotated encounter frame and custom numerical
     * configuration.
     * <p>
     * The rotated encounter frame is define by the initial encounter frame (defined in
     * {@link ShortTermEncounter2DDefinition}) rotated by the rotation matrix which is used to diagonalize the combined
     * covariance matrix.
     * </p>
     *
     * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
     * (m)
     * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
     * (m)
     * @param sigmaX square root of the x-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param sigmaY square root of the y-axis eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane (m)
     * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
     * @param customIntegrator custom integrator to use in place of the integrator from the constructor
     * @param customMaxNbOfEval custom maximum number of evaluations to use in place of the custom maximum number from the
     * constructor
     * @param <T> type of the field element
     *
     * @return probability of collision
     */
    public abstract <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(T xm, T ym,
                                                                                               T sigmaX, T sigmaY,
                                                                                               T radius,
                                                                                               FieldUnivariateIntegrator<T> customIntegrator,
                                                                                               int customMaxNbOfEval);

}
