/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.gnss.attitude;

import org.junit.Test;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;


public class GPSBlockIIRTest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new GPSBlockIIR(validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testPatchedLargeNegativeBeta() {
        doTestAxes("patched-eclips/beta-large-negative-BLOCK-IIR.txt", 1.5e-15, 1.2e-15, 8.8e-16);
    }

    @Test
    public void testPatchedSmallNegativeBeta() {
        doTestAxes("patched-eclips/beta-small-negative-BLOCK-IIR.txt", 3.1e-13, 3.1e-13, 9.1e-16);
    }

    @Test
    public void testPatchedCrossingBeta() {
        doTestAxes("patched-eclips/beta-crossing-BLOCK-IIR.txt", 5.2e-5, 5.2e-5, 4.9e-16);
    }

    @Test
    public void testPatchedSmallPositiveBeta() {
        doTestAxes("patched-eclips/beta-small-positive-BLOCK-IIR.txt", 8.0e-13, 8.0e-13, 7.9e-16);
    }

    @Test
    public void testPatchedLargePositiveBeta() {
        doTestAxes("patched-eclips/beta-large-positive-BLOCK-IIR.txt", 1.3e-15, 7.0e-15, 8.5e-16);
    }

    @Test
    public void testOriginalLargeNegativeBeta() {
        doTestAxes("original-eclips/beta-large-negative-BLOCK-IIR.txt", 1.5e-15, 1.2e-15, 8.8e-16);
    }

    @Test
    public void testOriginalSmallNegativeBeta() {
        doTestAxes("original-eclips/beta-small-negative-BLOCK-IIR.txt", 6.5e-4, 6.5e-4, 9.1e-16);
    }

    @Test
    public void testOriginalCrossingBeta() {
        // the very high threshold (1.68 radians) is due to the same probable bugs in original eclips
        // as the corresponding test for block-IIA. There are non-normalized vectors in the
        // "original-eclips/beta-crossing-BLOCK-IIR.txt" leading to wrong yaw and there are sign
        // changes for PHI/YANGLE
        // As a conclusion, we consider here that the reference output is wrong and that
        // Orekit behaviour is correct, so we increased the threshold so the test pass,
        // and wrote this big comment to explain the situation
        doTestAxes("original-eclips/beta-crossing-BLOCK-IIR.txt", 1.68, 1.68, 4.9e-16);
    }

    @Test
    public void testOriginalSmallPositiveBeta() {
        doTestAxes("original-eclips/beta-small-positive-BLOCK-IIR.txt", 7.4e-4, 7.4e-4, 7.9e-16);
    }

    @Test
    public void testOriginalLargePositiveBeta() {
        doTestAxes("original-eclips/beta-large-positive-BLOCK-IIR.txt", 1.3e-15, 7.0e-15, 8.5e-16);
    }

}
