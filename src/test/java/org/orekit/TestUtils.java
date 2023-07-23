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
package org.orekit;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.FieldVector2D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;

public class TestUtils {

    private TestUtils() {
        //Empty
    }

    public static void validateVector3D(final Vector3D expected, final Vector3D computed, final double threshold) {
        Assertions.assertEquals(expected.getX(), computed.getX(), threshold);
        Assertions.assertEquals(expected.getY(), computed.getY(), threshold);
        Assertions.assertEquals(expected.getZ(), computed.getZ(), threshold);

    }

    public static <T extends CalculusFieldElement<T>> void validateFieldVector3D(final Vector3D expected,
                                                                                 final FieldVector3D<T> computed,
                                                                                 final double threshold) {
        Assertions.assertEquals(expected.getX(), computed.getX().getReal(), threshold);
        Assertions.assertEquals(expected.getY(), computed.getY().getReal(), threshold);
        Assertions.assertEquals(expected.getZ(), computed.getZ().getReal(), threshold);
    }

    public static void validateVector2D(final Vector2D expected, final Vector2D computed, final double threshold) {
        Assertions.assertEquals(expected.getX(), computed.getX(), threshold);
        Assertions.assertEquals(expected.getY(), computed.getY(), threshold);

    }

    public static <T extends CalculusFieldElement<T>> void validateFieldVector2D(final Vector2D expected,
                                                                                 final FieldVector2D<T> computed,
                                                                                 final double threshold) {
        Assertions.assertEquals(expected.getX(), computed.getX().getReal(), threshold);
        Assertions.assertEquals(expected.getY(), computed.getY().getReal(), threshold);

    }

    public static <T extends CalculusFieldElement<T>> void validateFieldMatrix(final RealMatrix reference,
                                                                               final FieldMatrix<T> computed,
                                                                               final double threshold) {
        for (int row = 0; row < reference.getRowDimension(); row++) {
            for (int column = 0; column < reference.getColumnDimension(); column++) {
                if (reference.getEntry(row, column) == 0) {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column).getReal(),
                                            threshold);
                }
                else {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column).getReal(),
                                            FastMath.abs(threshold * reference.getEntry(row, column)));
                }
            }
        }

    }

    /**
     * Compare two covariance matrices.
     *
     * @param reference reference covariance
     * @param computed computed covariance
     * @param threshold relative threshold for comparison
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

}
