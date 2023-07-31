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
package org.orekit.ssa.collision.shorttermencounter.probability.twod.armellinutils;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.BlockFieldMatrix;
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.Binary64;
import org.orekit.utils.PVCoordinates;

/**
 * Container for a data row from Roberto Armellin's data.
 */
public class ArmellinDataRow {
    // CHECKSTYLE: stop JavadocVariable check
    private final double combinedRadius;
    private final double primaryPositionX;
    private final double primaryPositionY;
    private final double primaryPositionZ;
    private final double primaryVelocityX;
    private final double primaryVelocityY;
    private final double primaryVelocityZ;
    private final double primaryCrr;
    private final double primaryCtt;
    private final double primaryCnn;
    private final double primaryCrt;
    private final double primaryCrn;
    private final double primaryCtn;
    private final double secondaryPositionX;
    private final double secondaryPositionY;
    private final double secondaryPositionZ;
    private final double secondaryVelocityX;
    private final double secondaryVelocityY;
    private final double secondaryVelocityZ;
    private final double secondaryCrr;
    private final double secondaryCtt;
    private final double secondaryCnn;
    private final double secondaryCrt;
    private final double secondaryCrn;
    private final double secondaryCtn;
    private final double alfanoProbabilityOfCollision;
    private final double probabilityOfCollisionApprox;
    private final double probabilityOfCollisionMax;
    private final double missDistance;
    private final double relativeVelocity;
    private final double mahalanobisDistance;
    private final int    id;
    // CHECKSTYLE: resume JavadocVariable check

    public ArmellinDataRow(final int id, final double combinedRadius, final double primaryPositionX,
                           final double primaryPositionY,
                           final double primaryPositionZ,
                           final double primaryVelocityX,
                           final double primaryVelocityY,
                           final double primaryVelocityZ,
                           final double primaryCrr,
                           final double primaryCtt,
                           final double primaryCnn,
                           final double primaryCrt,
                           final double primaryCrn,
                           final double primaryCtn,
                           final double secondaryPositionX,
                           final double secondaryPositionY,
                           final double secondaryPositionZ,
                           final double secondaryVelocityX,
                           final double secondaryVelocityY,
                           final double secondaryVelocityZ,
                           final double secondaryCrr,
                           final double secondaryCtt,
                           final double secondaryCnn,
                           final double secondaryCrt,
                           final double secondaryCrn,
                           final double secondaryCtn,
                           final double alfanoProbabilityOfCollision,
                           final double probabilityOfCollisionApprox,
                           final double probabilityOfCollisionMax,
                           final double missDistance,
                           final double relativeVelocity,
                           final double mahalanobisDistance) {
        this.id                           = id;
        this.combinedRadius               = combinedRadius;
        this.primaryPositionX             = primaryPositionX;
        this.primaryPositionY             = primaryPositionY;
        this.primaryPositionZ             = primaryPositionZ;
        this.primaryVelocityX             = primaryVelocityX;
        this.primaryVelocityY             = primaryVelocityY;
        this.primaryVelocityZ             = primaryVelocityZ;
        this.primaryCrr                   = primaryCrr;
        this.primaryCtt                   = primaryCtt;
        this.primaryCnn                   = primaryCnn;
        this.primaryCrt                   = primaryCrt;
        this.primaryCrn                   = primaryCrn;
        this.primaryCtn                   = primaryCtn;
        this.secondaryPositionX           = secondaryPositionX;
        this.secondaryPositionY           = secondaryPositionY;
        this.secondaryPositionZ           = secondaryPositionZ;
        this.secondaryVelocityX           = secondaryVelocityX;
        this.secondaryVelocityY           = secondaryVelocityY;
        this.secondaryVelocityZ           = secondaryVelocityZ;
        this.secondaryCrr                 = secondaryCrr;
        this.secondaryCtt                 = secondaryCtt;
        this.secondaryCnn                 = secondaryCnn;
        this.secondaryCrt                 = secondaryCrt;
        this.secondaryCrn                 = secondaryCrn;
        this.secondaryCtn                 = secondaryCtn;
        this.alfanoProbabilityOfCollision = alfanoProbabilityOfCollision;
        this.probabilityOfCollisionApprox = probabilityOfCollisionApprox;
        this.probabilityOfCollisionMax    = probabilityOfCollisionMax;
        this.missDistance                 = missDistance;
        this.relativeVelocity             = relativeVelocity;
        this.mahalanobisDistance          = mahalanobisDistance;
    }

    /**
     * @return Primary collision object 6x6 positional covariance matrix in its RTN
     * {@link org.orekit.frames.LOFType local orbital frame.}
     */
    public RealMatrix getPrimaryCovarianceMatrixInPrimaryRTN() {
        final double[][] matrixData = {
                { primaryCrr, primaryCrt, primaryCrn, 0, 0, 0 },
                { primaryCrt, primaryCtt, primaryCtn, 0, 0, 0 },
                { primaryCrn, primaryCtn, primaryCnn, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 } };
        return new BlockRealMatrix(matrixData);
    }

    /**
     * @return Primary collision object 6x6 positional covariance matrix in its RTN
     * {@link org.orekit.frames.LOFType local orbital frame.}
     */
    public FieldMatrix<Binary64> getPrimaryFieldCovarianceMatrixInPrimaryRTN() {
        final Binary64[][] matrixData = {
                { new Binary64(primaryCrr), new Binary64(primaryCrt), new Binary64(primaryCrn), new Binary64(0),
                  new Binary64(0), new Binary64(0) },
                { new Binary64(primaryCrt), new Binary64(primaryCtt), new Binary64(primaryCtn), new Binary64(0),
                  new Binary64(0), new Binary64(0) },
                { new Binary64(primaryCrn), new Binary64(primaryCtn), new Binary64(primaryCnn), new Binary64(0),
                  new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0) } };
        return new BlockFieldMatrix<>(matrixData);
    }

    /**
     * @return Secondary collision object 6x6 positional covariance matrix in its RTN
     * {@link org.orekit.frames.LOFType local orbital frame.}
     */
    public RealMatrix getSecondaryCovarianceMatrixInSecondaryRTN() {
        final double[][] matrixData = {
                { secondaryCrr, secondaryCrt, secondaryCrn, 0, 0, 0 },
                { secondaryCrt, secondaryCtt, secondaryCtn, 0, 0, 0 },
                { secondaryCrn, secondaryCtn, secondaryCnn, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0 } };
        return new BlockRealMatrix(matrixData);
    }

    /**
     * @return Secondary collision object 6x6 positional covariance matrix in its RTN
     * {@link org.orekit.frames.LOFType local orbital frame.}
     */
    public FieldMatrix<Binary64> getSecondaryFieldCovarianceMatrixInPrimaryRTN() {
        final Binary64[][] matrixData = {
                { new Binary64(secondaryCrr), new Binary64(secondaryCrt), new Binary64(secondaryCrn), new Binary64(0),
                  new Binary64(0), new Binary64(0) },
                { new Binary64(secondaryCrt), new Binary64(secondaryCtt), new Binary64(secondaryCtn), new Binary64(0),
                  new Binary64(0), new Binary64(0) },
                { new Binary64(secondaryCrn), new Binary64(secondaryCtn), new Binary64(secondaryCnn), new Binary64(0),
                  new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0) },
                { new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0), new Binary64(0) } };
        return new BlockFieldMatrix<>(matrixData);
    }

    public PVCoordinates getPrimaryPVCoordinates() {
        return new PVCoordinates(getPrimaryPosition(), getPrimaryVelocity());
    }

    public Vector3D getPrimaryPosition() {
        return new Vector3D(primaryPositionX, primaryPositionY, primaryPositionZ);
    }

    public Vector3D getPrimaryVelocity() {
        return new Vector3D(primaryVelocityX, primaryVelocityY, primaryVelocityZ);
    }

    public PVCoordinates getSecondaryPVCoordinates() {
        return new PVCoordinates(getSecondaryPosition(), getSecondaryVelocity());
    }

    public Vector3D getSecondaryPosition() {
        return new Vector3D(secondaryPositionX, secondaryPositionY, secondaryPositionZ);
    }

    public Vector3D getSecondaryVelocity() {
        return new Vector3D(secondaryVelocityX, secondaryVelocityY, secondaryVelocityZ);
    }

    public double getAlfanoProbabilityOfcollision() {
        return alfanoProbabilityOfCollision;
    }

    public double getProbabilityOfCollisionMax() {
        return probabilityOfCollisionMax;
    }

    public double getMissDistance() {
        return missDistance;
    }

    public double getRelativeVelocity() {
        return relativeVelocity;
    }

    public double getMahalanobisDistance() {
        return mahalanobisDistance;
    }

    public double getProbabilityOfCollisionApprox() {
        return probabilityOfCollisionApprox;
    }

    public int getId() {
        return id;
    }

    public double getCombinedRadius() {
        return combinedRadius;
    }
}
