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
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.frames.encounter.EncounterLOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldStateCovariance;
import org.orekit.propagation.StateCovariance;
import org.orekit.ssa.metrics.FieldProbabilityOfCollision;
import org.orekit.ssa.metrics.ProbabilityOfCollision;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Fieldifier;
import org.orekit.utils.PVCoordinates;

/**
 * This abstract class serves as a foundation to create 2D probability of collision computing method assuming a short term
 * encounter model.
 * <p>
 * All the methods extending this class will at least assume the followings :
 * <ul>
 *     <li>Short term encounter leading to a linear relative motion.</li>
 *     <li>Spherical collision object.</li>
 *     <li>Uncorrelated positional covariance.</li>
 *     <li>Gaussian distribution of the position uncertainties.</li>
 *     <li>Deterministic velocity i.e. no velocity uncertainties.</li>
 * </ul>
 * As listed in the assumptions, methods extending this class are to be used in short encounter,
 * meaning that there must be a high relative velocity. For ease of computation, the resulting swept volume
 * is extended to infinity so that the integral becomes bivariate instead of trivariate (conservative hypothesis).
 * <p>
 * Consequently and if we consider Earth, methods implementing this interface are <u><b>recommended</b></u> for
 * collision happening in Low/Medium Earth Orbit (LEO and MEO) but are <u><b>not recommended</b></u> for collision
 * happening in Geostationary Earth Orbit (GEO).
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public abstract class AbstractShortTermEncounter2DPOCMethod implements ShortTermEncounter2DPOCMethod {

    /** Default time of closest approach difference tolerance. */
    public static final double DEFAULT_TCA_DIFFERENCE_TOLERANCE = 1e-6;

    /** Name of the method. */
    private final String name;

    /**
     * Constructor.
     *
     * @param name name of the method
     */
    protected AbstractShortTermEncounter2DPOCMethod(final String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    public ProbabilityOfCollision compute(final Cdm cdm, final double combinedRadius) {

        final CdmRelativeMetadata cdmRelativeMetadata = cdm.getRelativeMetadata();
        final CdmData             primaryData         = cdm.getDataObject1();
        final CdmData             secondaryData       = cdm.getDataObject2();
        final DataContext         cdmDataContext      = cdm.getDataContext();

        // Extract primary data
        final Orbit primaryOrbit = getObjectOrbitFromCdm(cdmRelativeMetadata, primaryData,
                                                         cdm.getMetadataObject1(), cdmDataContext);
        final StateCovariance primaryCovariance = getObjectStateCovarianceFromCdm(cdmRelativeMetadata, primaryData);

        // Extract secondary data
        final Orbit secondaryOrbit = getObjectOrbitFromCdm(cdmRelativeMetadata, secondaryData,
                                                           cdm.getMetadataObject2(), cdmDataContext);
        final StateCovariance secondaryCovariance = getObjectStateCovarianceFromCdm(cdmRelativeMetadata, secondaryData);

        return compute(primaryOrbit, primaryCovariance, secondaryOrbit, secondaryCovariance, combinedRadius,
                       DEFAULT_ZERO_THRESHOLD);
    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(final Cdm cdm,
                                                                                      final T combinedRadius,
                                                                                      final double zeroThreshold) {

        final Field<T>            field               = combinedRadius.getField();
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

        return compute(primaryOrbit, primaryCovariance, secondaryOrbit, secondaryCovariance, combinedRadius, zeroThreshold);
    }

    /** {@inheritDoc} */
    public ProbabilityOfCollision compute(final Orbit primaryAtTCA,
                                          final StateCovariance primaryCovariance,
                                          final Orbit secondaryAtTCA,
                                          final StateCovariance secondaryCovariance,
                                          final double combinedRadius,
                                          final double zeroThreshold) {

        final ShortTermEncounter2DDefinition shortTermEncounter2DDefinition = new ShortTermEncounter2DDefinition(
                primaryAtTCA, primaryCovariance, secondaryAtTCA, secondaryCovariance,
                combinedRadius, EncounterLOFType.DEFAULT, DEFAULT_TCA_DIFFERENCE_TOLERANCE);

        return compute(shortTermEncounter2DDefinition, zeroThreshold);
    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(
            final FieldOrbit<T> primaryAtTCA, final FieldStateCovariance<T> primaryCovariance,
            final FieldOrbit<T> secondaryAtTCA, final FieldStateCovariance<T> secondaryCovariance,
            final T combinedRadius, final double zeroThreshold) {

        final FieldShortTermEncounter2DDefinition<T> FieldShortTermEncounter2DDefinition =
                new FieldShortTermEncounter2DDefinition<>(
                        primaryAtTCA, primaryCovariance, secondaryAtTCA, secondaryCovariance,
                        combinedRadius, EncounterLOFType.DEFAULT, DEFAULT_TCA_DIFFERENCE_TOLERANCE);

        return compute(FieldShortTermEncounter2DDefinition, zeroThreshold);
    }

    /** {@inheritDoc} */
    public ProbabilityOfCollision compute(final ShortTermEncounter2DDefinition encounter,
                                          final double zeroThreshold) {

        final Vector2D otherPositionAfterRotationInCollisionPlane =
                encounter.computeOtherPositionInRotatedCollisionPlane(zeroThreshold);

        final RealMatrix projectedDiagonalizedCombinedPositionalCovarianceMatrix =
                encounter.computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix();

        return compute(otherPositionAfterRotationInCollisionPlane.getX(),
                       otherPositionAfterRotationInCollisionPlane.getY(),
                       FastMath.sqrt(projectedDiagonalizedCombinedPositionalCovarianceMatrix.getEntry(0, 0)),
                       FastMath.sqrt(projectedDiagonalizedCombinedPositionalCovarianceMatrix.getEntry(1, 1)),
                       encounter.getCombinedRadius());
    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(
            final FieldShortTermEncounter2DDefinition<T> encounter, final double zeroThreshold) {

        final FieldVector2D<T> otherPositionAfterRotationInCollisionPlane =
                encounter.computeOtherPositionInRotatedCollisionPlane(zeroThreshold);

        final FieldMatrix<T> projectedDiagonalizedCombinedPositionalCovarianceMatrix =
                encounter.computeProjectedAndDiagonalizedCombinedPositionalCovarianceMatrix();

        return compute(otherPositionAfterRotationInCollisionPlane.getX(),
                       otherPositionAfterRotationInCollisionPlane.getY(),
                       projectedDiagonalizedCombinedPositionalCovarianceMatrix.getEntry(0, 0).sqrt(),
                       projectedDiagonalizedCombinedPositionalCovarianceMatrix.getEntry(1, 1).sqrt(),
                       encounter.getCombinedRadius());
    }

    /** {@inheritDoc} */
    public abstract ProbabilityOfCollision compute(double xm, double ym, double sigmaX, double sigmaY, double radius);

    /** {@inheritDoc} */
    public abstract <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(T xm, T ym,
                                                                                               T sigmaX, T sigmaY,
                                                                                               T radius);

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAMaximumProbabilityOfCollisionMethod() {
        return false;
    }

    /**
     * Extract collision object spacecraft state from given {@link Cdm Conjunction Data Message} data.
     *
     * @param cdmRelativeMetadata conjunction data message relative metadata
     * @param cdmData collision object conjunction data message data
     * @param cdmMetadata collision object conjunction data message metadata
     * @param cdmDataContext conjunction data message data context
     *
     * @return basic collision object spacecraft state from conjunction data message
     */
    protected Orbit getObjectOrbitFromCdm(final CdmRelativeMetadata cdmRelativeMetadata,
                                          final CdmData cdmData,
                                          final CdmMetadata cdmMetadata,
                                          final DataContext cdmDataContext) {

        // Extract orbit
        final Frame        frame = cdmMetadata.getRefFrame().asFrame();
        final AbsoluteDate tca   = cdmRelativeMetadata.getTca();
        final PVCoordinates pvInFrame = new PVCoordinates(cdmData.getStateVectorBlock().getPositionVector(),
                                                          cdmData.getStateVectorBlock().getVelocityVector());
        final double mu = cdmMetadata.getOrbitCenter().getBody().getGM();

        // Simple case where the reference frame is already pseudo-inertial
        if (frame.isPseudoInertial()) {
            return new CartesianOrbit(pvInFrame, frame, tca, mu);
        }
        // Otherwise, convert coordinates to default inertial frame
        final Frame         inertial     = cdmDataContext.getFrames().getGCRF();
        final Transform     toInertial   = frame.getTransformTo(inertial, cdmRelativeMetadata.getTca());
        final PVCoordinates pvInInertial = toInertial.transformPVCoordinates(pvInFrame);

        return new CartesianOrbit(pvInInertial, inertial, tca, mu);
    }

    /**
     * Get collision object state covariance from given {@link Cdm Conjunction Data Message} data.
     *
     * @param cdmRelativeMetadata conjunction data message relative metadata
     * @param cdmData collision object conjunction data message data
     *
     * @return collision object state covariance
     */
    protected StateCovariance getObjectStateCovarianceFromCdm(final CdmRelativeMetadata cdmRelativeMetadata,
                                                              final CdmData cdmData) {
        final AbsoluteDate tca = cdmRelativeMetadata.getTca();
        final RealMatrix rtnCovarianceMatrix =
                cdmData.getRTNCovarianceBlock().getRTNCovarianceMatrix().getSubMatrix(0, 5, 0, 5);
        return new StateCovariance(rtnCovarianceMatrix, tca, LOFType.QSW_INERTIAL);
    }

}
