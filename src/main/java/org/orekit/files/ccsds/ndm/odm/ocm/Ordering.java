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

/** Keys for {@link OrbitCovariance} elements ordering.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum Ordering {

    /** Lower Triangular Matrix. */
    LTM {

        /** {@inheritDoc} */
        @Override
        int nbElements(final int dimension) {
            return (dimension * (dimension + 1)) / 2;
        }

        /** {@inheritDoc} */
        @Override
        void update(final CovarianceIndexer indexer) {
            final int i = indexer.getRow();
            final int j = indexer.getColumn();
            if (j < i) {
                // continue on same row
                indexer.setColumn(j + 1);
            } else {
                // start new row
                indexer.setRow(i + 1);
                indexer.setColumn(0);
            }
        }

    },

    /** Upper Triangular Matrix. */
    UTM {

        /** {@inheritDoc} */
        @Override
        int nbElements(final int dimension) {
            return (dimension * (dimension + 1)) / 2;
        }

        /** {@inheritDoc} */
        @Override
        void update(final CovarianceIndexer indexer) {
            final int i = indexer.getRow();
            final int j = indexer.getColumn();
            if (j + 1 < indexer.getDimension()) {
                // continue on same row
                indexer.setColumn(j + 1);
            } else {
                // start new row
                indexer.setRow(i + 1);
                indexer.setColumn(i + 1);
            }
        }

    },

    /** Full symmetric Matrix. */
    FULL {

        /** {@inheritDoc} */
        @Override
        int nbElements(final int dimension) {
            return dimension * dimension;
        }

        /** {@inheritDoc} */
        @Override
        void update(final CovarianceIndexer indexer) {
            final int i = indexer.getRow();
            final int j = indexer.getColumn();
            if (j + 1 < indexer.getDimension()) {
                // continue on same row
                indexer.setColumn(j + 1);
            } else {
                // start new row
                indexer.setRow(i + 1);
                indexer.setColumn(0);
            }
        }

    },

    /** Lower Triangular Matrix conflated with cross-correlation terms. */
    LTMWCC {

        /** {@inheritDoc} */
        @Override
        int nbElements(final int dimension) {
            return FULL.nbElements(dimension);
        }

        /** {@inheritDoc} */
        @Override
        void update(final CovarianceIndexer indexer) {
            FULL.update(indexer);
            indexer.setCrossCorrelation(indexer.getColumn() > indexer.getRow());
        }

    },

    /** Upper Triangular Matrix conflated with cross-correlation terms. */
    UTMWCC {

        /** {@inheritDoc} */
        @Override
        int nbElements(final int dimension) {
            return FULL.nbElements(dimension);
        }

        /** {@inheritDoc} */
        @Override
        void update(final CovarianceIndexer indexer) {
            FULL.update(indexer);
            indexer.setCrossCorrelation(indexer.getRow() > indexer.getColumn());
        }

    };

    /** Get number of ordered elements.
     * @param dimension matrix dimension
     * @return number of ordered elements
     */
    abstract int nbElements(int dimension);

    /** Update indexer.
     * @param indexer index to update for handling next element
     */
    abstract void update(CovarianceIndexer indexer);

}
