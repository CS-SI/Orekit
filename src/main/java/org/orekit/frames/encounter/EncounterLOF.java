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
package org.orekit.frames.encounter;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.FieldVector2D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.linear.Array2DRowFieldMatrix;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.MathArrays;
import org.orekit.frames.LOF;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface for encounter local orbital frame.
 * <p>
 * Encounter local orbital frame are defined using two objects, one of them is placed at the origin and the other is
 * expressed relatively to the origin.
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public interface EncounterLOF extends LOF {

    /**
     * {@inheritDoc} It is unnecessary to use this method when dealing with {@link EncounterLOF}, use
     * {@link #rotationFromInertial(Field, FieldPVCoordinates)} instead.
     */
    @Override
    default <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(Field<T> field,
                                                                                      FieldAbsoluteDate<T> date,
                                                                                      FieldPVCoordinates<T> pv) {
        return rotationFromInertial(field, pv);
    }

    /**
     * {@inheritDoc} It is unnecessary to use this method when dealing with {@link EncounterLOF}, use
     * {@link #rotationFromInertial(PVCoordinates)} instead.
     */
    @Override
    default Rotation rotationFromInertial(AbsoluteDate date, PVCoordinates pv) {
        return rotationFromInertial(pv);
    }

    /**
     * Get the rotation from inertial to this encounter local orbital frame.
     * <p>
     * <b>BEWARE: The given origin's position and velocity coordinates must be given in the frame in which this instance
     * has been expressed in.</b>
     *
     * @param field field to which the elements belong
     * @param origin position-velocity of the origin in the same inertial frame as the one this instance has been expressed
     * in.
     * @param <T> type of the field elements
     *
     * @return rotation from inertial to this encounter local orbital frame
     */
    default <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                      final FieldPVCoordinates<T> origin) {
        return rotationFromInertial(field, origin, getFieldOther(field));
    }

    /**
     * Get the rotation from inertial to this encounter local orbital frame.
     * <p>
     * <b>BEWARE: The given origin's position and velocity coordinates must be given in the frame in which this instance
     * has been expressed in.</b>
     *
     * @param origin position-velocity of the origin in some inertial frame
     *
     * @return rotation from inertial to this encounter local orbital frame
     */
    default Rotation rotationFromInertial(final PVCoordinates origin) {
        return rotationFromInertial(origin, getOther());
    }

    /**
     * Get the rotation from inertial to this encounter local orbital frame.
     * <p>
     * <b>BEWARE: The given origin's position and velocity coordinates must be given in the frame in which this instance
     * has been expressed in.</b>
     *
     * @param field field to which the elements belong
     * @param origin position-velocity of the origin in the same inertial frame as other
     * @param other position-velocity of the other in the same inertial frame as origin
     * @param <T> type of the field elements
     *
     * @return rotation from inertial to this encounter local orbital frame
     */
    <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(Field<T> field,
                                                                              FieldPVCoordinates<T> origin,
                                                                              FieldPVCoordinates<T> other);

    /**
     * Get the rotation from inertial to this encounter local orbital frame.
     * <p>
     * <b>BEWARE: The given origin's position and velocity coordinates must be given in the frame in which this instance
     * has been expressed in.</b>
     *
     * @param origin position-velocity of the origin in the same inertial frame as other
     * @param other position-velocity of the other instance in the same inertial frame as origin
     *
     * @return rotation from inertial to this encounter local orbital frame
     */
    Rotation rotationFromInertial(PVCoordinates origin, PVCoordinates other);

    /**
     * Project given {@link RealMatrix matrix} expressed in this encounter local orbital frame onto the collision plane.
     *
     * @param matrix matrix to project, a 3 by 3 matrix is expected
     *
     * @return projected matrix onto the collision plane defined by this encounter local orbital frame
     */
    default RealMatrix projectOntoCollisionPlane(RealMatrix matrix) {
        final RealMatrix projectionMatrix = computeProjectionMatrix();
        return projectionMatrix.multiply(matrix.multiplyTransposed(projectionMatrix));
    }

    /**
     * Get the 2x3 projection matrix that projects values expressed in this encounter local orbital frame to the collision
     * plane.
     *
     * @return 2x3 projection matrix
     */
    default RealMatrix computeProjectionMatrix() {
        // Remove axis normal to collision plane from the identity matrix
        final RealMatrix identity = MatrixUtils.createRealIdentityMatrix(3);
        final List<double[]> projectionMatrixDataList = Arrays.stream(identity.getData())
                                                              .filter(values -> !Arrays.equals(values,
                                                                                               getAxisNormalToCollisionPlane().toArray()))
                                                              .collect(Collectors.toList());

        // Map list<double[]> to double[][]
        final double[][] projectionMatrixData = new double[2][3];
        for (int i = 0; i < 2; i++) {
            projectionMatrixData[i] = projectionMatrixDataList.get(i);
        }

        return new Array2DRowRealMatrix(projectionMatrixData);
    }

    /**
     * Project given {@link RealMatrix matrix} expressed in this encounter local orbital frame onto the collision plane
     * defined by this same encounter local orbital frame.
     *
     * @param matrix matrix to project, a 3 by 3 matrix is expected
     * @param <T> type of the field elements
     *
     * @return projected matrix onto the collision plane defined by this encounter local orbital frame
     */
    default <T extends CalculusFieldElement<T>> FieldMatrix<T> projectOntoCollisionPlane(FieldMatrix<T> matrix) {
        final FieldMatrix<T> projectionMatrix = computeProjectionMatrix(matrix.getField());
        return projectionMatrix.multiply(matrix.multiplyTransposed(projectionMatrix));
    }

    /**
     * Get the 2x3 projection matrix that projects values expressed in this encounter local orbital frame to the collision
     * plane defined by this same encounter local orbital frame.
     *
     * @param field field to which the elements belong
     * @param <T> type of the field elements
     *
     * @return 2x3 projection matrix
     */
    default <T extends CalculusFieldElement<T>> FieldMatrix<T> computeProjectionMatrix(Field<T> field) {
        // Remove axis normal to collision plane from the identity matrix
        final FieldMatrix<T> identity = MatrixUtils.createFieldIdentityMatrix(field, 3);
        final List<T[]> projectionMatrixDataList = Arrays.stream(identity.getData())
                                                         .filter(values -> !Arrays.equals(values,
                                                                                          getAxisNormalToCollisionPlane(
                                                                                                  field).toArray()))
                                                         .collect(Collectors.toList());

        // Map list<C[]> to C[][]
        final T[][] projectionMatrixData = MathArrays.buildArray(field, 2, 3);
        for (int i = 0; i < 2; i++) {
            projectionMatrixData[i] = projectionMatrixDataList.get(i);
        }

        return new Array2DRowFieldMatrix<>(projectionMatrixData);
    }

    /**
     * Get the axis normal to the collision plane (i, j or k) in this encounter local orbital frame.
     *
     * @param field field of the elements
     * @param <T> type of the field elements
     *
     * @return axis normal to the collision plane (i, j or k) in this encounter local orbital frame
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> getAxisNormalToCollisionPlane(Field<T> field);

    /**
     * Project given {@link Vector3D vector} expressed in this encounter local orbital frame onto the collision plane.
     *
     * @param vector vector to project
     *
     * @return projected vector onto the collision plane defined by this encounter local orbital frame
     */
    default Vector2D projectOntoCollisionPlane(Vector3D vector) {
        final RealMatrix projectionMatrix = computeProjectionMatrix();
        final RealMatrix vectorInMatrix   = new Array2DRowRealMatrix(vector.toArray());

        return new Vector2D(projectionMatrix.multiply(vectorInMatrix).getColumn(0));
    }

    /**
     * Project given {@link Vector3D vector} expressed in this encounter local orbital frame onto the collision plane.
     *
     * @param vector vector to project
     * @param <T> type of the field elements
     *
     * @return projected vector onto the collision plane defined by this encounter local orbital frame
     */
    default <T extends CalculusFieldElement<T>> FieldVector2D<T> projectOntoCollisionPlane(
            final FieldVector3D<T> vector) {
        final FieldMatrix<T> projectionMatrix = computeProjectionMatrix(vector.getX().getField());
        final FieldMatrix<T> vectorInMatrix   = new Array2DRowFieldMatrix<>(vector.toArray());

        return new FieldVector2D<>(projectionMatrix.multiply(vectorInMatrix).getColumn(0));
    }

    /** Get other's position and velocity coordinates.
     * @param field field of the element
     * @param <T> type of the element
     *
     * @return other's position and velocity coordinates
     */
    <T extends CalculusFieldElement<T>> FieldPVCoordinates<T> getFieldOther(Field<T> field);

    /**
     * Get the axis normal to the collision plane (i, j or k) in this encounter local orbital frame.
     *
     * @return axis normal to the collision plane (i, j or k) in this encounter local orbital frame
     */
    Vector3D getAxisNormalToCollisionPlane();

    /** Get other's position and velocity coordinates.
     * @return other's position and velocity coordinates
     */
    PVCoordinates getOther();

    /** Get flag that indicates if current local orbital frame shall be treated as pseudo-inertial.
     * @return flag that indicates if current local orbital frame shall be treated as pseudo-inertial
     */
    @Override
    default boolean isQuasiInertial() {
        return true;
    }

}
