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


public class GPSBlockIIFTest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new GPSBlockIIF(GPSBlockIIF.DEFAULT_YAW_RATE,
                               validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testPatchedLargeNegativeBeta() {
        doTestAxes("patched-eclips/beta-large-negative-BLOCK-IIF.txt",  1.1e-15, 8.9e-16, 4.2e-16);
    }

    @Test
    public void testPatchedSmallNegativeBeta() {
        doTestAxes("patched-eclips/beta-small-negative-BLOCK-IIF.txt", 8.2e-13, 8.2e-13, 9.2e-16);
    }

    @Test
    public void testPatchedCrossingBeta() {
        doTestAxes("patched-eclips/beta-crossing-BLOCK-IIF.txt", 5.7e-4, 5.7e-4, 7.8e-16);
    }

    @Test
    public void testPatchedSmallPositiveBeta() {
        doTestAxes("patched-eclips/beta-small-positive-BLOCK-IIF.txt", 2.9e-12, 2.9e-12, 5.0e-16);
    }

    @Test
    public void testPatchedLargePositiveBeta() {
        doTestAxes("patched-eclips/beta-large-positive-BLOCK-IIF.txt", 1.1e-15, 7.7e-16, 3.2e-16);
    }

    @Test
    public void testOriginalLargeNegativeBeta() {
        doTestAxes("original-eclips/beta-large-negative-BLOCK-IIF.txt",  1.1e-15, 8.9e-16, 4.2e-16);
    }

    @Test
    public void testOriginalSmallNegativeBeta() {
        doTestAxes("original-eclips/beta-small-negative-BLOCK-IIF.txt", 2.8e-3, 2.8e-3, 9.2e-16);
    }

    @Test
    public void testOriginalCrossingBeta() {
        // the very high threshold (0.24 radians) is due to a probable bug in original eclips
        // the output of the routine is limited to the x-sat vector, the yaw angle itself
        // is not output. However, in some cases the x-sat vector is not normalised at all.
        // looking in the reference data file original-eclips/beta-crossing-BLOCK-IIF.txt,
        // one can see that the axis at line 6 is about (0.1455, 0.1232, 1.0537), at line 7
        // it is about (0.43789, 0.9574, 1.3427) and at line 24 it is about (-0.3545, -0.5993,
        // 1.0565). The yaw angles extracted from these wrong vectors and written as the last
        // field in the same lines read -136.5778°, -107.3698° and 293.7100°, whereas Orekit
        // values are -141.0539°, -120.6270° and -59.1380 (equivalent to 300.8619°). However,
        // looking at the log from the original routine, we get:
        // R           6   348067.10899436299       -179.95495754951264       -141.05951131830636...
        // R           6   348427.10899436299       -179.97175156958741       -120.62312989589805...
        // ...
        // S          24   576951.53755731299        179.85738967852436       -59.415765665963555...
        // so we see that the yaw values are -141.0595°, -120.6231 and -59.4157°, very close to Orekit values.
        // As the testOriginal...() series of tests explicitly do *not* patch the original routine
        // at all, it was not possible to output the internal phi variable to write reference
        // data properly. We also decided to not edit the file to set the correct angle value,
        // as this would imply cheating on the reference
        // As a conclusion, we consider here that the reference output is wrong and that
        // Orekit behaviour is correct, so we increased the threshold so the test pass,
        // and wrote this big comment to explain the situation
        doTestAxes("original-eclips/beta-crossing-BLOCK-IIF.txt", 0.24, 0.24, 7.8e-16);
    }

    @Test
    public void testOriginalSmallPositiveBeta() {
        doTestAxes("original-eclips/beta-small-positive-BLOCK-IIF.txt", 2.8e-4, 2.8e-4, 5.0e-16);
    }

    @Test
    public void testOriginalLargePositiveBeta() {
        doTestAxes("original-eclips/beta-large-positive-BLOCK-IIF.txt", 1.1e-15, 7.7e-16, 3.2e-16);
    }

}
