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


public class GlonassTest extends AbstractGNSSAttitudeProviderTest {

    @Test
    public void testPatchedLargeNegativeBeta() {
        doTestAxes("patched-eclips/beta-large-negative-GLONASS.txt", 7.2e-15, 1.1e-15, false);
    }

    @Test
    public void testPatchedSmallNegativeBeta() {
        doTestAxes("patched-eclips/beta-small-negative-GLONASS.txt", 7.8e-11, 9.8e-16, false);
    }

    @Test
    public void testPatchedCrossingBeta() {
        doTestAxes("patched-eclips/beta-crossing-GLONASS.txt", 5.2e-6, 9.8e-16, false);
    }

    @Test
    public void testPatchedSmallPositiveBeta() {
        doTestAxes("patched-eclips/beta-small-positive-GLONASS.txt", 2.4e-12, 7.4e-16, false);
    }

    @Test
    public void testPatchedLargePositiveBeta() {
        doTestAxes("patched-eclips/beta-large-positive-GLONASS.txt", 6.8e-15, 9.2e-16, false);
    }

    @Test
    public void testOriginalLargeNegativeBeta() {
        doTestAxes("original-eclips/beta-large-negative-GLONASS.txt", 7.2e-15, 1.1e-15, false);
    }

    @Test
    public void testOriginalSmallNegativeBeta() {
        doTestAxes("original-eclips/beta-small-negative-GLONASS.txt", 1.6e-4, 9.8e-16, false);
    }

    @Test
    public void testOriginalCrossingBeta() {
        // the very high threshold (0.54 radians) is due to a probable bug in original eclips
        // the output of the routine is limited to the x-sat vector, the yaw angle itself
        // is not output. However, in some cases the x-sat vector is not normalized at all.
        // looking in the reference data file original-eclips/beta-crossing-GLONASS.txt,
        // one can see that the axis at line 6 is about (1.3069, 0.7310, -0.7609). The yaw
        // angle extracted from this wrong vector and written as the last field in the same
        // line reads 92.6599°, whereas Orekit value is 94.4594°. However, looking
        // at the log from the original routine, we get:
        // R          45   103443.31500000000        179.97115230838418        94.470693723018371 ...
        // so we see that the yaw value is 94.4707°, very close to Orekit value.
        // As the testOriginal...() series of tests explicitly do *not* patch the original routine
        // at all, it was not possible to output the internal phi variable to write reference
        // data properly. We also decided to not edit the file to set the correct angle value,
        // as this would imply cheating on the reference
        // As a conclusion, we consider here that the reference output is wrong and that
        // Orekit behavior is correct, so we increased the threshold so the test pass,
        // and wrote this big comment to explain the situation
        doTestAxes("original-eclips/beta-crossing-GLONASS.txt", 0.54, 9.8e-16, false);
    }

    @Test
    public void testOriginalSmallPositiveBeta() {
        doTestAxes("original-eclips/beta-small-positive-GLONASS.txt", 1.6e-4, 7.4e-16, false);
    }

    @Test
    public void testOriginalLargePositiveBeta() {
        doTestAxes("original-eclips/beta-large-positive-GLONASS.txt", 6.8e-15, 9.2e-16, false);
    }

}
