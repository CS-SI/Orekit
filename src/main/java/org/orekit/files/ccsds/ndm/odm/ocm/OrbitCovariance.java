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
public class OrbitCovariance implements TimeStamped {

    /** Type of the elements. */
    private final OrbitElementsType type;

    /** Entry date. */
    private final AbsoluteDate date;

    /** Covariance matrix. */
    private final RealMatrix matrix;

    /** Simple constructor.
     * @param type type of the elements
     * @param ordering ordering to use
     * @param date entry date
     * @param fields matrix elements
     * @param first index of first field to consider
     */
    public OrbitCovariance(final OrbitElementsType type, final Ordering ordering, final AbsoluteDate date,
                           final String[] fields, final int first) {
        final List<Unit> units = type.getUnits();
        this.type   = type;
        this.date   = date;
        this.matrix = MatrixUtils.createRealMatrix(units.size(), units.size());
        final CovarianceIndexer indexer = new CovarianceIndexer(units.size());
        for (int k = 0; first + k < fields.length; ++k) {
            if (!indexer.isCrossCorrelation()) {
                final int    i         = indexer.getRow();
                final int    j         = indexer.getColumn();
                final double raw       = Double.parseDouble(fields[first + k]);
                final double converted = units.get(i).toSI(units.get(j).toSI(raw));
                matrix.setEntry(i, j, converted);
                if (i != j) {
                    matrix.setEntry(j, i, converted);
                }
            }
            ordering.update(indexer);
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
    public OrbitElementsType getType() {
        return type;
    }

}
