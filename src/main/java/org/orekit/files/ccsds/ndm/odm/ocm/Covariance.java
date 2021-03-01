/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.units.Unit;

/** Covariance entry.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class Covariance implements TimeStamped {

    /** Type of the elements. */
    private final ElementsType type;

    /** Entry date. */
    private final AbsoluteDate date;

    /** Covariance matrix. */
    private final RealMatrix matrix;

    /** Simple constructor.
     * @param type type of the elements
     * @param date entry date
     * @param fields matrix elements
     * @param first index of first field to consider
     * @param units units to use for parsing
     */
    public Covariance(final ElementsType type, final AbsoluteDate date,
                      final String[] fields, final int first, final List<Unit> units) {
        this.type   = type;
        this.date   = date;
        this.matrix = MatrixUtils.createRealMatrix(units.size(), units.size());
        int k = 0;
        for (int i = 0; i < matrix.getRowDimension(); ++i) {
            final Unit ui = units.get(i);
            for (int j = 0; j <= i; ++j) {
                final Unit   uj    = units.get(j);
                final double rawij = Double.parseDouble(fields[first + k++]);
                final double cij   = ui.toSI(uj.toSI(rawij));
                matrix.setEntry(i, j, cij);
                if (j < i) {
                    matrix.setEntry(j, i, cij);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the covariance matrix.
     * @return covariance matrix
     */
    public RealMatrix getMatrix() {
        return matrix;
    }

    /** Get the type of the elements.
     * @return type of the elements
     */
    public ElementsType getType() {
        return type;
    }

}
