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

/** Container for covariance matrix elements indices.
 * @author Luc Maisonobe
 * @since 11.0
 */
class CovarianceIndexer {

    /** Matrix dimension. */
    private final int dimension;

    /** Row index. */
    private int row;

    /** Column index. */
    private int column;

    /** Flag for cross-correlation trems. */
    private boolean crossCorrelation;

    /** Build an indexer pointing at first row first column,.
     * @param dimension matrix dimension
     */
    CovarianceIndexer(final int dimension) {
        this.dimension        = dimension;
        this.row              = 0;
        this.column           = 0;
        this.crossCorrelation = false;
    }

    /** Get matrix dimension.
     * @return matrix dimension
     */
    public int getDimension() {
        return dimension;
    }

    /** Get row index.
     * @return row index
     */
    public int getRow() {
        return row;
    }

    /** Set row index.
     * @param row row index
     */
    public void setRow(final int row) {
        this.row = row;
    }

    /** Get column index.
     * @return column index
     */
    public int getColumn() {
        return column;
    }

    /** Set column index.
     * @param column column index
     */
    public void setColumn(final int column) {
        this.column = column;
    }

    /** Set cross-correlation flag.
     * @param crossCorrelation if true, element is a cross-correlation term
     */
    public void setCrossCorrelation(final boolean crossCorrelation) {
        this.crossCorrelation = crossCorrelation;
    }

    /** Check if element is a cross-correlation term.
     * @return true if element is a cross-correlation term
     */
    public boolean isCrossCorrelation() {
        return crossCorrelation;
    }

}
