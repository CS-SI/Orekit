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


public class BeidouIGSOTest extends AbstractGNSSAttitudeProviderTest {

    @Test
    public void testPatchedLargeNegativeBeta() {
        doTestAxes("patched-eclips/beta-large-negative-BEIDOU-2I.txt", 7.8e-15, 1.1e-15, false);
    }

    @Test
    public void testPatchedSmallNegativeBeta() {
        doTestAxes("patched-eclips/beta-small-negative-BEIDOU-2I.txt", 8.1e-15, 9.3e-16, false);
    }

    @Test
    public void testPatchedSmallPositiveBeta() {
        doTestAxes("patched-eclips/beta-small-positive-BEIDOU-2I.txt", 7.5e-15, 9.9e-16, false);
    }

    @Test
    public void testPatchedLargePositiveBeta() {
        doTestAxes("patched-eclips/beta-large-positive-BEIDOU-2I.txt", 7.9e-15, 1.2e-15, false);
    }

    @Test
    public void testOriginalLargeNegativeBeta() {
        doTestAxes("original-eclips/beta-large-negative-BEIDOU-2I.txt", 7.8e-15, 1.1e-15, false);
    }

    @Test
    public void testOriginalSmallNegativeBeta() {
        doTestAxes("original-eclips/beta-small-negative-BEIDOU-2I.txt", 3.9e-3, 9.3e-16, false);
    }

    @Test
    public void testOriginalSmallPositiveBeta() {
        doTestAxes("original-eclips/beta-small-positive-BEIDOU-2I.txt", 4.5e-3, 9.9e-16, false);
    }

    @Test
    public void testOriginalLargePositiveBeta() {
        doTestAxes("original-eclips/beta-large-positive-BEIDOU-2I.txt", 7.9e-15, 1.2e-15, false);
    }

}
