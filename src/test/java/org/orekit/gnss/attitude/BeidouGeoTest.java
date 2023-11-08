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
package org.orekit.gnss.attitude;

import org.junit.jupiter.api.Test;


public class BeidouGeoTest extends AbstractGNSSAttitudeProviderTest {

    @Test
    public void testPatchedLargeNegativeBeta()  {
        doTestAxes("patched-eclips/beta-large-negative-BEIDOU-2G.txt", 6.6e-15, 7.8e-16, false);
    }

    @Test
    public void testPatchedSmallNegativeBeta() {
        doTestAxes("patched-eclips/beta-small-negative-BEIDOU-2G.txt", 8.0e-15, 8.3e-16, false);
    }

    @Test
    public void testPatchedCrossingBeta() {
        doTestAxes("patched-eclips/beta-crossing-BEIDOU-2G.txt", 6.2e-15, 8.6e-16, false);
    }

    @Test
    public void testPatchedSmallPositiveBeta() {
        doTestAxes("patched-eclips/beta-small-positive-BEIDOU-2G.txt", 7.9e-15, 7.1e-16, false);
    }

    @Test
    public void testOriginalLargeNegativeBeta()  {
        doTestAxes("original-eclips/beta-large-negative-BEIDOU-2G.txt", 7.6e-4, 7.8e-16, false);
    }

    @Test
    public void testOriginalSmallNegativeBeta() {
        doTestAxes("original-eclips/beta-small-negative-BEIDOU-2G.txt", 5.0e-4, 8.6e-16, false);
    }

    @Test
    public void testOriginalCrossingBeta() {
        doTestAxes("original-eclips/beta-crossing-BEIDOU-2G.txt", 9.0e-4, 8.6e-16, false);
    }

    @Test
    public void testOriginalSmallPositiveBeta() {
        doTestAxes("original-eclips/beta-small-positive-BEIDOU-2G.txt", 9.4e-4, 7.1e-16, false);
    }

}
