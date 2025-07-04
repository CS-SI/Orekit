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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;

public class CartesianDerivativesFilterTest {

    @Test
    public void testList() {
        Assertions.assertEquals(3, CartesianDerivativesFilter.values().length);
    }

    @Test
    public void testOrder() {
        Assertions.assertEquals(0, CartesianDerivativesFilter.USE_P.getMaxOrder(), 0);
        Assertions.assertEquals(1, CartesianDerivativesFilter.USE_PV.getMaxOrder(), 0);
        Assertions.assertEquals(2, CartesianDerivativesFilter.USE_PVA.getMaxOrder(), 0);
    }

    @Test
    public void testBuildFromOrder() {
        Assertions.assertEquals(CartesianDerivativesFilter.USE_P,  CartesianDerivativesFilter.getFilter(0));
        Assertions.assertEquals(CartesianDerivativesFilter.USE_PV, CartesianDerivativesFilter.getFilter(1));
        Assertions.assertEquals(CartesianDerivativesFilter.USE_PVA, CartesianDerivativesFilter.getFilter(2));
    }

    @Test
    public void testNoNegativeOrder() {
        try {
            CartesianDerivativesFilter.getFilter(-1);
            Assertions.fail("an exception should have been thrown");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testNoOrder3() {
        try {
            CartesianDerivativesFilter.getFilter(3);
            Assertions.fail("an exception should have been thrown");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

