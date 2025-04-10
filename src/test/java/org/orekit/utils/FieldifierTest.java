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

package org.orekit.utils;

import org.hipparchus.Field;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.LOFType;
import org.orekit.propagation.FieldStateCovariance;
import org.orekit.propagation.StateCovariance;
import org.orekit.time.AbsoluteDate;

class FieldifierTest {

    final Field<Binary64> field = Binary64Field.getInstance();

    @BeforeAll
    static void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testMatrix() {
        // GIVEN
        // Create fake matrix
        final RealMatrix m = MatrixUtils.createRealMatrix(new double[][] {
                {  1.0,  2.0,  3.0 },
                { -3.0, -2.0, -1.0 }
        });

        // WHEN
        final FieldMatrix<Binary64> fieldMatrix = Fieldifier.fieldify(field, m);

        // THEN

        // Assert matrix dimension
        Assertions.assertEquals(m.getRowDimension(),    fieldMatrix.getRowDimension());
        Assertions.assertEquals(m.getColumnDimension(), fieldMatrix.getColumnDimension());

        // Assert matrix elements
        for (int i = 0; i < m.getRowDimension(); ++i) {
            for (int j = 0; j < m.getColumnDimension(); ++j) {
                Assertions.assertEquals(m.getEntry(i, j), fieldMatrix.getEntry(i, j).getReal());
            }
        }

    }

    @Test
    void testStateCovariance() {
        // GIVEN
        // Create fake covariance
        final RealMatrix rowVector = MatrixUtils.createRealMatrix(new double[][] {
                {  1.0,  2.0,  3.0, -3.0, -2.0, -1.0 }
        });
        final StateCovariance stateCovariance =
                new StateCovariance(rowVector.transposeMultiply(rowVector),
                                    AbsoluteDate.QZSS_EPOCH,
                                    LOFType.LVLH_CCSDS);

        // WHEN
        final FieldStateCovariance<Binary64> fieldStateCovariance =
                Fieldifier.fieldify(field, stateCovariance);

        // THEN

        // Assert date
        Assertions.assertEquals(0.0,
                                fieldStateCovariance.getDate().durationFrom(stateCovariance.getDate()).getReal(),
                          1.0e-15);

        // Assert matrix dimension
        final RealMatrix m = stateCovariance.getMatrix();
        final FieldMatrix<Binary64> fieldMatrix = fieldStateCovariance.getMatrix();
        Assertions.assertEquals(m.getRowDimension(),    fieldMatrix.getRowDimension());
        Assertions.assertEquals(m.getColumnDimension(), fieldMatrix.getColumnDimension());

        // Assert matrix elements
        for (int i = 0; i < m.getRowDimension(); ++i) {
            for (int j = 0; j < m.getColumnDimension(); ++j) {
                Assertions.assertEquals(m.getEntry(i, j), fieldMatrix.getEntry(i, j).getReal());
            }
        }

        // Assert types
        Assertions.assertEquals(stateCovariance.getLOF(), fieldStateCovariance.getLOF());

    }

}
