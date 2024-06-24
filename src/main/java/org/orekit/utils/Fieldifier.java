/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldStateCovariance;
import org.orekit.propagation.StateCovariance;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Utility class used to convert class to their Field equivalent.
 *
 * @author Vincent Cucchietti
 */
public class Fieldifier {

    /** Private constructor. */
    private Fieldifier() {
        // Empty constructor
    }

    /**
     * Fieldify given orbit with given field.
     * <p>
     * Conserve derivatives and return orbit in same orbit type as input orbit.
     *
     * @param field field to fieldify with
     * @param orbit orbit to fieldify
     * @param <T> type of the elements
     *
     * @return fielded orbit
     * @deprecated
     */
    @Deprecated
    public static <T extends CalculusFieldElement<T>> FieldOrbit<T> fieldify(final Field<T> field, final Orbit orbit) {

        return orbit.getType().convertToFieldOrbit(field, orbit);
    }

    /**
     * Fieldify given matrix with given field.
     *
     * @param field field to fieldify with
     * @param matrix matrix to fieldify
     * @param <T> type of the elements
     *
     * @return fielded matrix
     */
    public static <T extends CalculusFieldElement<T>> FieldMatrix<T> fieldify(final Field<T> field,
                                                                              final RealMatrix matrix) {

        final int rowDim    = matrix.getRowDimension();
        final int columnDim = matrix.getColumnDimension();

        final FieldMatrix<T> fieldMatrix = MatrixUtils.createFieldMatrix(field, rowDim, columnDim);

        for (int i = 0; i < rowDim; i++) {
            for (int j = 0; j < columnDim; j++) {
                fieldMatrix.setEntry(i, j, field.getOne().newInstance(matrix.getEntry(i, j)));
            }
        }

        return fieldMatrix;
    }

    /**
     * Fieldify given state covariance with given field.
     *
     * @param field field to which the
     * @param stateCovariance state covariance to fieldify
     * @param <T> type of the elements
     *
     * @return fielded state covariance
     *
     * @since 12.0
     */
    public static <T extends CalculusFieldElement<T>> FieldStateCovariance<T> fieldify(final Field<T> field,
                                                                                       final StateCovariance stateCovariance) {
        final FieldMatrix<T>       fieldMatrix = fieldify(field, stateCovariance.getMatrix());
        final FieldAbsoluteDate<T> fieldEpoch  = new FieldAbsoluteDate<>(field, stateCovariance.getDate());
        if (stateCovariance.getLOF() == null) {
            return new FieldStateCovariance<>(fieldMatrix, fieldEpoch, stateCovariance.getFrame(),
                                              stateCovariance.getOrbitType(), stateCovariance.getPositionAngleType());
        }
        return new FieldStateCovariance<>(fieldMatrix, fieldEpoch, stateCovariance.getLOF());
    }

}
