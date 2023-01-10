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


public class GPSBlockIIFTest extends AbstractGNSSAttitudeProviderTest {

    @Test
    public void testPatchedLargeNegativeBeta() {
        doTestAxes("patched-eclips/beta-large-negative-BLOCK-IIF.txt",  6.8e-15, 7.6e-16, false);
    }

    @Test
    public void testPatchedSmallNegativeBeta() {
        doTestAxes("patched-eclips/beta-small-negative-BLOCK-IIF.txt", 1.8e-12, 9.2e-16, false);
    }

    @Test
    public void testPatchedCrossingBeta() {
        doTestAxes("patched-eclips/beta-crossing-BLOCK-IIF.txt", 5.7e-4, 7.8e-16, false);
    }

    @Test
    public void testPatchedSmallPositiveBeta() {
        doTestAxes("patched-eclips/beta-small-positive-BLOCK-IIF.txt", 2.9e-12, 6.0e-16, false);
    }

    @Test
    public void testPatchedLargePositiveBeta() {
        doTestAxes("patched-eclips/beta-large-positive-BLOCK-IIF.txt", 7.4e-15, 6.7e-16, false);
    }

    @Test
    public void testOriginalLargeNegativeBeta() {
        doTestAxes("original-eclips/beta-large-negative-BLOCK-IIF.txt", 6.8e-15, 7.6e-16, false);
    }

    @Test
    public void testOriginalSmallNegativeBeta() {
        doTestAxes("original-eclips/beta-small-negative-BLOCK-IIF.txt", 2.8e-3, 9.2e-16, false);
    }

    @Test
    public void testOriginalCrossingBeta() {
        // the very high threshold (0.24 radians) is due to a probable bug in original eclips
        // the output of the routine is limited to the x-sat vector, the yaw angle itself
        // is not output. However, in some cases the x-sat vector is not normalised at all.
        // looking in the reference data file original-eclips/beta-crossing-BLOCK-IIF.txt,
        // one can see that the axis at line 8 is about (0.4380, 0.9578, 1.3430). The yaw
        // angle extracted from these wrong vector and written as the last field in the same
        // line reads -107.3651°, whereas Orekit value is -120.6269°. However, looking at the
        // log from the original routine, we get:
        // R           6   348427.10900000000       -179.97172706071467       -120.62317675422287 ...
        // so we see that the yaw value is -120.6232°, very close to Orekit value.
        // As the testOriginal...() series of tests explicitly do *not* patch the original routine
        // at all, it was not possible to output the internal phi variable to write reference
        // data properly. We also decided to not edit the file to set the correct angle value,
        // as this would imply cheating on the reference
        // As a conclusion, we consider here that the reference output is wrong and that
        // Orekit behaviour is correct, so we increased the threshold so the test pass,
        // and wrote this big comment to explain the situation
        doTestAxes("original-eclips/beta-crossing-BLOCK-IIF.txt", 0.24, 7.8e-16, false);
    }

    @Test
    public void testOriginalSmallPositiveBeta() {
        doTestAxes("original-eclips/beta-small-positive-BLOCK-IIF.txt", 2.8e-4, 6.0e-16, false);
    }

    @Test
    public void testOriginalLargePositiveBeta() {
        doTestAxes("original-eclips/beta-large-positive-BLOCK-IIF.txt", 7.4e-15, 6.7e-16, false);
    }

}
