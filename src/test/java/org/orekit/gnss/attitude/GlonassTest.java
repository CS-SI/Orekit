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


public class GlonassTest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new Glonass(Glonass.DEFAULT_YAW_RATE, validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testPatchedLargeNegativeBeta() {
        doTestAxes("patched-eclips/beta-large-negative-GLONASS.txt", 1.5e-15, 1.1e-15, 3.1e-16);
    }

    @Test
    public void testPatchedSmallNegativeBeta() {
        doTestAxes("patched-eclips/beta-small-negative-GLONASS.txt", 4.5e-13, 4.5e-13, 4.9e-16);
    }

    @Test
    public void testPatchedCrossingBeta() {
        doTestAxes("patched-eclips/beta-crossing-GLONASS.txt", 3.7e-6, 3.7e-6, 6.7e-16);
    }

    @Test
    public void testPatchedSmallPositiveBeta() {
        doTestAxes("patched-eclips/beta-small-positive-GLONASS.txt", 2.4e-12, 2.4e-12, 3.9e-16);
    }

    @Test
    public void testPatchedLargePositiveBeta() {
        doTestAxes("patched-eclips/beta-large-positive-GLONASS.txt", 1.3e-15, 7.7e-16, 5.4e-16);
    }

    @Test
    public void testOriginalLargeNegativeBeta() {
        doTestAxes("original-eclips/beta-large-negative-GLONASS.txt", 1.5e-15, 1.1e-15, 3.1e-16);
    }

    @Test
    public void testOriginalSmallNegativeBeta() {
        doTestAxes("original-eclips/beta-small-negative-GLONASS.txt", 1.6e-4, 1.6e-4, 4.9e-16);
    }

    @Test
    public void testOriginalCrossingBeta() {
        // the very high threshold (0.55 radians) is due to a probable bug in original eclips
        // the output of the routine is limited to the x-sat vector, the yaw angle itself
        // is not output. However, in some cases the x-sat vector is not normalised at all.
        // looking in the reference data file original-eclips/beta-crossing-GLONASS.txt,
        // one can see that the axis at line 5 is about (1.3024, 0.7286, -0.7586) and at
        // line 6 it is about (1.1027, 0.0707, 0.5247). The yaw angle extracted from these
        // wrong vectors and written as the last field in the same lines read 92.6691°
        // and 35.4145°, whereas Orekit values are 94.4601° and 4.4603°. However, looking
        // at the log from the original routine, we get:
        // R          45   103443.31548835800        179.97133503372297        94.471443460910763 ...
        // R          45   103803.31548835800        179.97486048792248        4.4714441016571413 ...
        // so we see that the yaw values are 94.4714° and 4.4714°, very close to Orekit values.
        // As the testOriginal...() series of tests explicitly do *not* patch the original routine
        // at all, it was not possible to output the internal phi variable to write reference
        // data properly. We also decided to not edit the file to set the correct angle value,
        // as this would imply cheating on the reference
        // As a conclusion, we consider here that the reference output is wrong and that
        // Orekit behaviour is correct, so we increased the threshold so the test pass,
        // and wrote this big comment to explain the situation
        doTestAxes("original-eclips/beta-crossing-GLONASS.txt", 0.55, 0.55, 6.7e-16);
    }

    @Test
    public void testOriginalSmallPositiveBeta() {
        doTestAxes("original-eclips/beta-small-positive-GLONASS.txt", 1.6e-4, 1.6e-4, 3.9e-16);
    }

    @Test
    public void testOriginalLargePositiveBeta() {
        doTestAxes("original-eclips/beta-large-positive-GLONASS.txt", 1.3e-15, 7.7e-16, 5.4e-16);
    }

}
