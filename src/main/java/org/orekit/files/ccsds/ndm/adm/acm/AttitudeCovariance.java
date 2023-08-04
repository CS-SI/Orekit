/* Copyright 2023 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import java.util.List;

import org.hipparchus.linear.DiagonalMatrix;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.units.Unit;

/** Covariance entry.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AttitudeCovariance implements TimeStamped {

    /** Type of the elements. */
    private final AttitudeCovarianceType type;

    /** Entry date. */
    private final AbsoluteDate date;

    /** Covariance matrix. */
    private final DiagonalMatrix matrix;

    /** Simple constructor.
     * @param type type of the elements
     * @param date entry date
     * @param fields matrix diagonal elements
     * @param first index of first field to consider
     */
    public AttitudeCovariance(final AttitudeCovarianceType type, final AbsoluteDate date,
                              final String[] fields, final int first) {
        final List<Unit> units = type.getUnits();
        this.type   = type;
        this.date   = date;
        this.matrix = new DiagonalMatrix(units.size());
        for (int k = 0; k < matrix.getRowDimension(); ++k) {
            matrix.setEntry(k, k, units.get(k).toSI(Double.parseDouble(fields[first + k])));
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
    public DiagonalMatrix getMatrix() {
        return matrix;
    }

    /** Get the type of the elements.
     * @return type of the elements
     */
    public AttitudeCovarianceType getType() {
        return type;
    }

}
