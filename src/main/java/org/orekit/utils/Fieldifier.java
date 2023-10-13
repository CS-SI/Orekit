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
package org.orekit.utils;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
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
     */
    public static <T extends CalculusFieldElement<T>> FieldOrbit<T> fieldify(final Field<T> field, final Orbit orbit) {

        final T                    one       = field.getOne();
        final FieldAbsoluteDate<T> fieldDate = new FieldAbsoluteDate<>(field, orbit.getDate());
        final T                    fieldMu   = one.multiply(orbit.getMu());
        final Frame                frame     = orbit.getFrame();

        switch (orbit.getType()) {
            case CIRCULAR: {
                final CircularOrbit circOrbit = (CircularOrbit) OrbitType.CIRCULAR.convertType(orbit);

                // Get orbital elements
                final T a      = one.multiply(circOrbit.getA());
                final T ex     = one.multiply(circOrbit.getCircularEx());
                final T ey     = one.multiply(circOrbit.getCircularEy());
                final T i      = one.multiply(circOrbit.getI());
                final T raan   = one.multiply(circOrbit.getRightAscensionOfAscendingNode());
                final T alphaM = one.multiply(circOrbit.getAlphaM());

                // Get derivatives
                final T aDot      = one.multiply(circOrbit.getADot());
                final T exDot     = one.multiply(circOrbit.getCircularExDot());
                final T eyDot     = one.multiply(circOrbit.getCircularEyDot());
                final T iDot      = one.multiply(circOrbit.getIDot());
                final T raanDot   = one.multiply(circOrbit.getRightAscensionOfAscendingNodeDot());
                final T alphaMDot = one.multiply(circOrbit.getAlphaMDot());

                return new FieldCircularOrbit<>(a, ex, ey, i, raan, alphaM, aDot, exDot, eyDot, iDot, raanDot, alphaMDot,
                                                PositionAngleType.MEAN, frame, fieldDate, fieldMu);
            }

            case CARTESIAN: {
                final FieldPVCoordinates<T> orbitPV = new FieldPVCoordinates<>(field, orbit.getPVCoordinates());

                return new FieldCartesianOrbit<>(orbitPV, orbit.getFrame(), fieldDate, fieldMu);
            }

            case KEPLERIAN: {
                final KeplerianOrbit kepOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);

                // Get orbital elements
                final T a           = one.multiply(kepOrbit.getA());
                final T e           = one.multiply(kepOrbit.getE());
                final T i           = one.multiply(kepOrbit.getI());
                final T raan        = one.multiply(kepOrbit.getRightAscensionOfAscendingNode());
                final T pa          = one.multiply(kepOrbit.getPerigeeArgument());
                final T meanAnomaly = one.multiply(kepOrbit.getMeanAnomaly());

                // Get derivatives
                final T aDot           = one.multiply(kepOrbit.getADot());
                final T eDot           = one.multiply(kepOrbit.getEDot());
                final T iDot           = one.multiply(kepOrbit.getIDot());
                final T raanDot        = one.multiply(kepOrbit.getRightAscensionOfAscendingNodeDot());
                final T paDot          = one.multiply(kepOrbit.getPerigeeArgumentDot());
                final T meanAnomalyDot = one.multiply(kepOrbit.getMeanAnomalyDot());

                return new FieldKeplerianOrbit<>(a, e, i, pa, raan, meanAnomaly, aDot, eDot, iDot, paDot, raanDot,
                                                 meanAnomalyDot, PositionAngleType.MEAN, frame, fieldDate, fieldMu);
            }
            case EQUINOCTIAL: {
                final EquinoctialOrbit equiOrbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(orbit);

                // Get orbital elements
                final T a  = one.multiply(equiOrbit.getA());
                final T ex = one.multiply(equiOrbit.getEquinoctialEx());
                final T ey = one.multiply(equiOrbit.getEquinoctialEy());
                final T hx = one.multiply(equiOrbit.getHx());
                final T hy = one.multiply(equiOrbit.getHy());
                final T lm = one.multiply(equiOrbit.getLM());

                // Get derivatives
                final T aDot  = one.multiply(equiOrbit.getADot());
                final T exDot = one.multiply(equiOrbit.getEquinoctialExDot());
                final T eyDot = one.multiply(equiOrbit.getEquinoctialEyDot());
                final T hxDot = one.multiply(equiOrbit.getHxDot());
                final T hyDot = one.multiply(equiOrbit.getHyDot());
                final T lmDot = one.multiply(equiOrbit.getLMDot());

                return new FieldEquinoctialOrbit<>(a, ex, ey, hx, hy, lm, aDot, exDot, eyDot, hxDot, hyDot,
                                                   lmDot, PositionAngleType.MEAN, frame, fieldDate, fieldMu);
            }
            default:
                // Should never happen
                throw new OrekitInternalError(null);
        }

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
                fieldMatrix.setEntry(i, j, field.getOne().multiply(matrix.getEntry(i, j)));
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
