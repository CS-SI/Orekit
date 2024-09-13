/* Copyright 2022-2024 Romain Serra
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
package org.orekit.utils;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.frames.Frame;
import org.orekit.frames.KinematicTransform;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;

/**
 * Utility class for conversions related to Cartesian covariance matrices.
 *
 * @author Romain Serra
 * @since 12.2
 */
public class CartesianCovarianceUtils {

    /**
     * Private constructor.
     */
    private CartesianCovarianceUtils() {
        // utility class
    }

    /**
     * Convert input position-velocity covariance matrix between reference frames.
     * @param inputFrame input frame
     * @param outputFrame output frame
     * @param covarianceMatrix position-velocity covariance matrix in reference frame
     * @param date epoch
     * @return converted covariance matrix
     */
    public static RealMatrix changeReferenceFrame(final Frame inputFrame, final RealMatrix covarianceMatrix,
                                                  final AbsoluteDate date, final Frame outputFrame) {
        final KinematicTransform kinematicTransform = inputFrame.getKinematicTransformTo(outputFrame, date);
        return changePositionVelocityFrame(covarianceMatrix, kinematicTransform);
    }

    /**
     * Convert input position-velocity covariance matrix from reference frame to local one.
     * @param position position vector in reference frame
     * @param velocity velocity vector in reference frame
     * @param covarianceMatrix position-velocity covariance matrix in reference frame
     * @param lofType output local orbital frame
     * @return converted covariance matrix
     */
    public static RealMatrix convertToLofType(final Vector3D position, final Vector3D velocity,
                                              final RealMatrix covarianceMatrix, final LOFType lofType) {
        final Transform transformFromInertial = transformToLofType(lofType, position, velocity);
        return changePositionVelocityFrame(covarianceMatrix, transformFromInertial);
    }

    /**
     * Convert input position-velocity covariance matrix from local frame to reference one.
     * @param position position vector in reference frame
     * @param velocity velocity vector in reference frame
     * @param covarianceMatrix position-velocity covariance matrix in local frame
     * @param lofType input local orbital frame
     * @return converted covariance matrix
     */
    public static RealMatrix convertFromLofType(final LOFType lofType, final RealMatrix covarianceMatrix,
                                                final Vector3D position, final Vector3D velocity) {
        final Transform transformFromInertial = transformToLofType(lofType, position, velocity);
        return changePositionVelocityFrame(covarianceMatrix, transformFromInertial.getInverse());
    }

    /**
     * Get the transform from local orbital frame to reference frame.
     * @param lofType input local frame type
     * @param position position in reference frame
     * @param velocity velocity in reference frame
     * @return transform
     */
    private static Transform transformToLofType(final LOFType lofType, final Vector3D position,
                                                final Vector3D velocity) {
        return lofType.transformFromInertial(null, new PVCoordinates(position, velocity));
    }

    /**
     * Convert the input position-velocity covariance matrix according to input transformation.
     * @param covarianceMatrix original covariance matrix
     * @param kinematicTransform kinematic frame transform
     * @return transformed covariance matrix
     */
    private static RealMatrix changePositionVelocityFrame(final RealMatrix covarianceMatrix,
                                                          final KinematicTransform kinematicTransform) {
        final RealMatrix jacobianTransformPV = MatrixUtils.createRealMatrix(kinematicTransform.getPVJacobian());
        return jacobianTransformPV.multiply(covarianceMatrix.multiplyTransposed(jacobianTransformPV));
    }
}
