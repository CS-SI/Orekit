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


public class GPSBlockIIRTest extends AbstractGNSSAttitudeProviderTest {

    @Test
    public void testPatchedLargeNegativeBeta() {
        doTestAxes("patched-eclips/beta-large-negative-BLOCK-IIR.txt", 8.0e-15, 8.8e-16, false);
    }

    @Test
    public void testPatchedSmallNegativeBeta() {
        doTestAxes("patched-eclips/beta-small-negative-BLOCK-IIR.txt", 5.4e-13, 9.6e-16, false);
    }

    @Test
    public void testPatchedCrossingBeta() {
        doTestAxes("patched-eclips/beta-crossing-BLOCK-IIR.txt", 5.2e-5, 8.7e-16, false);
    }

    @Test
    public void testPatchedSmallPositiveBeta() {
        doTestAxes("patched-eclips/beta-small-positive-BLOCK-IIR.txt", 1.2e-12, 9.4e-16, false);
    }

    @Test
    public void testPatchedLargePositiveBeta() {
        doTestAxes("patched-eclips/beta-large-positive-BLOCK-IIR.txt", 6.7e-15, 9.1e-16, false);
    }

    @Test
    public void testOriginalLargeNegativeBeta() {
        doTestAxes("original-eclips/beta-large-negative-BLOCK-IIR.txt", 8.0e-15, 8.8e-16, false);
    }

    @Test
    public void testOriginalSmallNegativeBeta() {
        doTestAxes("original-eclips/beta-small-negative-BLOCK-IIR.txt", 6.5e-4, 9.6e-16, false);
    }

    @Test
    public void testOriginalCrossingBeta() {
        // the very high threshold (1.68 radians) is due to the same probable bugs in original eclips
        // as the corresponding test for block-IIA. There are non-normalized vectors in the
        // "original-eclips/beta-crossing-BLOCK-IIR.txt" (one of them having a norm greater than 43000!)
        // leading to wrong yaw and there are sign changes for PHI/YANGLE
        // As a conclusion, we consider here that the reference output is wrong and that
        // Orekit behaviour is correct, so we increased the threshold so the test pass,
        // and wrote this big comment to explain the situation
        doTestAxes("original-eclips/beta-crossing-BLOCK-IIR.txt", 1.68, 8.7e-16, false);
    }

    @Test
    public void testOriginalSmallPositiveBeta() {
        doTestAxes("original-eclips/beta-small-positive-BLOCK-IIR.txt", 7.4e-4, 9.4e-16, false);
    }

    @Test
    public void testOriginalLargePositiveBeta() {
        doTestAxes("original-eclips/beta-large-positive-BLOCK-IIR.txt", 6.7e-15, 9.1e-16, false);
    }

}
