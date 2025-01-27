/* Copyright 2002-2025 CS GROUP
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
package org.orekit;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.FieldVector2D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.FieldAdditionalStateProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinates;

import java.util.Arrays;
import java.util.Collection;

/** Utility class for tests to reduce code duplication. */
public class TestUtils {

    private TestUtils() {
        //Empty
    }

    /**
     * Create and return a default equatorial circular orbit at 400km altitude.
     *
     * @param date date of the orbit
     *
     * @return default orbit
     */
    public static Orbit getDefaultOrbit(final AbsoluteDate date) {
        final PVCoordinates pv = new PVCoordinates(
                new Vector3D(6378000 + 400000, 0, 0),
                new Vector3D(0, 7668.631425, 0));

        final Frame frame = FramesFactory.getGCRF();

        final double DEFAULT_MU = 398600e9; // m**3/s**2

        return new CartesianOrbit(pv, frame, date, DEFAULT_MU);
    }

    /**
     * Method created to test issue 949.
     *
     * @return additional state provider with custom init() method defined which use the initial state
     */
    public static AdditionalStateProvider getAdditionalProviderWithInit() {
        return new AdditionalStateProvider() {
            /**
             * Custom init method which use the initial state instance.
             *
             * @param initialState initial state information at the start of propagation
             * @param target date of propagation
             */
            @Override
            public void init(final SpacecraftState initialState, final AbsoluteDate target) {
                initialState.getMass();
            }

            @Override
            public String getName() {
                return "Test";
            }

            @Override
            public double[] getAdditionalData(final SpacecraftState state) {
                return new double[0];
            }
        };
    }

    /**
     * Method created to test issue 949.
     *
     * @return additional state provider with custom init() method defined which use the initial state
     */
    public static <T extends CalculusFieldElement<T>> FieldAdditionalStateProvider<T> getFieldAdditionalProviderWithInit() {
        return new FieldAdditionalStateProvider<T>() {

            @Override
            public void init(FieldSpacecraftState<T> initialState, FieldAbsoluteDate<T> target) {
                initialState.getMass();
            }

            @Override
            public String getName() {
                return "Test";
            }

            @Override
            public T[] getAdditionalState(FieldSpacecraftState<T> state) {
                return MathArrays.buildArray(state.getDate().getField(), 0);
            }
        };
    }

    /**
     * Validate vector 3D.
     *
     * @param expected expected vector 3D
     * @param computed computed vector 3D
     * @param threshold absolute threshold
     */
    public static void validateVector3D(final Vector3D expected, final Vector3D computed, final double threshold) {
        Assertions.assertEquals(expected.getX(), computed.getX(), threshold);
        Assertions.assertEquals(expected.getY(), computed.getY(), threshold);
        Assertions.assertEquals(expected.getZ(), computed.getZ(), threshold);

    }

    /**
     * Validate field vector 3D.
     *
     * @param expected expected vector 3D
     * @param computed computed vector 3D
     * @param threshold absolute threshold
     * @param <T> type of the vector
     */
    public static <T extends CalculusFieldElement<T>> void validateFieldVector3D(final Vector3D expected,
                                                                                 final FieldVector3D<T> computed,
                                                                                 final double threshold) {
        Assertions.assertEquals(expected.getX(), computed.getX().getReal(), threshold);
        Assertions.assertEquals(expected.getY(), computed.getY().getReal(), threshold);
        Assertions.assertEquals(expected.getZ(), computed.getZ().getReal(), threshold);
    }

    /**
     * Validate vector 2D.
     *
     * @param expected expected vector 2D
     * @param computed computed vector 2D
     * @param threshold absolute threshold
     */
    public static void validateVector2D(final Vector2D expected, final Vector2D computed, final double threshold) {
        Assertions.assertEquals(expected.getX(), computed.getX(), threshold);
        Assertions.assertEquals(expected.getY(), computed.getY(), threshold);

    }

    /**
     * Validate field vector 2D.
     *
     * @param expected expected vector 2D
     * @param computed computed vector 2D
     * @param threshold absolute threshold
     * @param <T> type of the vector
     */
    public static <T extends CalculusFieldElement<T>> void validateFieldVector2D(final Vector2D expected,
                                                                                 final FieldVector2D<T> computed,
                                                                                 final double threshold) {
        Assertions.assertEquals(expected.getX(), computed.getX().getReal(), threshold);
        Assertions.assertEquals(expected.getY(), computed.getY().getReal(), threshold);

    }

    /**
     * Compare two covariance matrices. Can work in two different modes :
     * <ul>
     *     <li>Relative threshold if reference is not equal to zero</li>
     *     <li>Absolute threshold if reference is equal to zero. </li>
     * </ul>
     *
     * @param reference reference covariance
     * @param computed computed covariance
     * @param threshold relative/absolute threshold for comparison depending on used mode
     */
    public static <T extends CalculusFieldElement<T>> void validateFieldMatrix(final RealMatrix reference,
                                                                               final FieldMatrix<T> computed,
                                                                               final double threshold) {
        for (int row = 0; row < reference.getRowDimension(); row++) {
            for (int column = 0; column < reference.getColumnDimension(); column++) {
                if (reference.getEntry(row, column) == 0) {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column).getReal(),
                                            threshold);
                } else {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column).getReal(),
                                            FastMath.abs(threshold * reference.getEntry(row, column)));
                }
            }
        }

    }

    /**
     * Compare two covariance matrices. Can work in two different modes :
     * <ul>
     *     <li>Relative threshold if reference is not equal to zero</li>
     *     <li>Absolute threshold if reference is equal to zero. </li>
     * </ul>
     *
     * @param reference reference covariance
     * @param computed computed covariance
     * @param threshold relative/absolute threshold for comparison depending on used mode
     */
    public static void validateRealMatrix(final RealMatrix reference,
                                          final RealMatrix computed,
                                          final double threshold) {
        for (int row = 0; row < reference.getRowDimension(); row++) {
            for (int column = 0; column < reference.getColumnDimension(); column++) {
                if (reference.getEntry(row, column) == 0) {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column),
                                            threshold);
                }
                else {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column),
                                            FastMath.abs(threshold * reference.getEntry(row, column)));
                }
            }
        }
    }

    /**
     * Pretty print a matrix.
     *
     * @param matrix the matrix to print.
     */
    public static void prettyPrint(RealMatrix matrix) {
        prettyPrint(matrix.getData());
    }

    /**
     * Pretty print a double[][].
     *
     * @param array the array to print.
     */
    public static void prettyPrint(double[][] array) {
        for (double[] anArray : array) {
            for (double value : anArray) {
                System.out.format("%20g ", value);
            }
            System.out.println();
        }
    }

    /**
     * Pretty print an array
     *
     * @param array the array to print.
     */
    public static void prettyPrint(double[] array) {
        System.out.println(Arrays.toString(array));
    }

    /**
     * Check if the given value contains a NaN. Useful as a condition for breakpoints.
     * Knows how to check arrays, {@link DerivativeStructure}, and {@link Gradient}.
     *
     * @param o to check for NaNs.
     * @return if the object contains any NaNs.
     */
    public static boolean isAnyNan(Object o) {
        if (o instanceof Double) {
            return Double.isNaN((Double) o);
        } else if (o instanceof double[]) {
            for (double v : (double[]) o) {
                if (Double.isNaN(v)) {
                    return true;
                }
            }
        } else if (o instanceof Object[]) {
            for (Object object : (Object[]) o) {
                if (isAnyNan(object)) {
                    return true;
                }
            }
        } else if (o instanceof Collection) {
            for (Object object : (Collection<?>) o) {
                if (isAnyNan(object)) {
                    return true;
                }
            }
        } else if (o instanceof CalculusFieldElement) {
            CalculusFieldElement<?> cfe = (CalculusFieldElement<?>) o;
            if (cfe.isNaN()) {
                return true;
            }
            if (cfe instanceof Gradient) {
                return isAnyNan(((Gradient) cfe).getGradient());
            }
            if (cfe instanceof DerivativeStructure) {
                return isAnyNan(((DerivativeStructure) cfe).getAllDerivatives());
            }
        }
        return false;
    }

}
