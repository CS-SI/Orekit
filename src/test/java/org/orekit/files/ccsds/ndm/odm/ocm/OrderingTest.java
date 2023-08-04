/* Copyright 2002-2023 CS GROUP
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OrderingTest {

    @Test
    public void testLTM() {
        Assertions.assertEquals(10, Ordering.LTM.nbElements(4));
        final CovarianceIndexer indexer = new CovarianceIndexer(4);
        checkIndexer(indexer, 0, 0, false);
        Ordering.LTM.update(indexer);
        checkIndexer(indexer, 1, 0, false);
        Ordering.LTM.update(indexer);
        checkIndexer(indexer, 1, 1, false);
        Ordering.LTM.update(indexer);
        checkIndexer(indexer, 2, 0, false);
        Ordering.LTM.update(indexer);
        checkIndexer(indexer, 2, 1, false);
        Ordering.LTM.update(indexer);
        checkIndexer(indexer, 2, 2, false);
        Ordering.LTM.update(indexer);
        checkIndexer(indexer, 3, 0, false);
        Ordering.LTM.update(indexer);
        checkIndexer(indexer, 3, 1, false);
        Ordering.LTM.update(indexer);
        checkIndexer(indexer, 3, 2, false);
        Ordering.LTM.update(indexer);
        checkIndexer(indexer, 3, 3, false);
    }

    @Test
    public void testUTM() {
        Assertions.assertEquals(10, Ordering.UTM.nbElements(4));
        final CovarianceIndexer indexer = new CovarianceIndexer(4);
        checkIndexer(indexer, 0, 0, false);
        Ordering.UTM.update(indexer);
        checkIndexer(indexer, 0, 1, false);
        Ordering.UTM.update(indexer);
        checkIndexer(indexer, 0, 2, false);
        Ordering.UTM.update(indexer);
        checkIndexer(indexer, 0, 3, false);
        Ordering.UTM.update(indexer);
        checkIndexer(indexer, 1, 1, false);
        Ordering.UTM.update(indexer);
        checkIndexer(indexer, 1, 2, false);
        Ordering.UTM.update(indexer);
        checkIndexer(indexer, 1, 3, false);
        Ordering.UTM.update(indexer);
        checkIndexer(indexer, 2, 2, false);
        Ordering.UTM.update(indexer);
        checkIndexer(indexer, 2, 3, false);
        Ordering.UTM.update(indexer);
        checkIndexer(indexer, 3, 3, false);
    }

    @Test
    public void testFULL() {
        Assertions.assertEquals(9, Ordering.FULL.nbElements(3));
        final CovarianceIndexer indexer = new CovarianceIndexer(3);
        checkIndexer(indexer, 0, 0, false);
        Ordering.FULL.update(indexer);
        checkIndexer(indexer, 0, 1, false);
        Ordering.FULL.update(indexer);
        checkIndexer(indexer, 0, 2, false);
        Ordering.FULL.update(indexer);
        checkIndexer(indexer, 1, 0, false);
        Ordering.FULL.update(indexer);
        checkIndexer(indexer, 1, 1, false);
        Ordering.FULL.update(indexer);
        checkIndexer(indexer, 1, 2, false);
        Ordering.FULL.update(indexer);
        checkIndexer(indexer, 2, 0, false);
        Ordering.FULL.update(indexer);
        checkIndexer(indexer, 2, 1, false);
        Ordering.FULL.update(indexer);
        checkIndexer(indexer, 2, 2, false);
    }

    @Test
    public void testLTMWCC() {
        Assertions.assertEquals(9, Ordering.LTMWCC.nbElements(3));
        final CovarianceIndexer indexer = new CovarianceIndexer(3);
        checkIndexer(indexer, 0, 0, false);
        Ordering.LTMWCC.update(indexer);
        checkIndexer(indexer, 0, 1, true);
        Ordering.LTMWCC.update(indexer);
        checkIndexer(indexer, 0, 2, true);
        Ordering.LTMWCC.update(indexer);
        checkIndexer(indexer, 1, 0, false);
        Ordering.LTMWCC.update(indexer);
        checkIndexer(indexer, 1, 1, false);
        Ordering.LTMWCC.update(indexer);
        checkIndexer(indexer, 1, 2, true);
        Ordering.LTMWCC.update(indexer);
        checkIndexer(indexer, 2, 0, false);
        Ordering.LTMWCC.update(indexer);
        checkIndexer(indexer, 2, 1, false);
        Ordering.LTMWCC.update(indexer);
        checkIndexer(indexer, 2, 2, false);
    }

    @Test
    public void testUTMWCC() {
        Assertions.assertEquals(9, Ordering.UTMWCC.nbElements(3));
        final CovarianceIndexer indexer = new CovarianceIndexer(3);
        checkIndexer(indexer, 0, 0, false);
        Ordering.UTMWCC.update(indexer);
        checkIndexer(indexer, 0, 1, false);
        Ordering.UTMWCC.update(indexer);
        checkIndexer(indexer, 0, 2, false);
        Ordering.UTMWCC.update(indexer);
        checkIndexer(indexer, 1, 0, true);
        Ordering.UTMWCC.update(indexer);
        checkIndexer(indexer, 1, 1, false);
        Ordering.UTMWCC.update(indexer);
        checkIndexer(indexer, 1, 2, false);
        Ordering.UTMWCC.update(indexer);
        checkIndexer(indexer, 2, 0, true);
        Ordering.UTMWCC.update(indexer);
        checkIndexer(indexer, 2, 1, true);
        Ordering.UTMWCC.update(indexer);
        checkIndexer(indexer, 2, 2, false);
    }

    private void checkIndexer(final CovarianceIndexer indexer,
                              final int row, final int column, final boolean crossCorrelation) {
        Assertions.assertEquals(row,              indexer.getRow());
        Assertions.assertEquals(column,           indexer.getColumn());
        Assertions.assertEquals(crossCorrelation, indexer.isCrossCorrelation());
    }

}
