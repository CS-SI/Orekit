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


public class GPSBlockIIATest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new GPSBlockIIA(validityStart, validityEnd, sun, inertialFrame, prnNumber);
    }

    @Test
    public void testPatchedLargeNegativeBeta() {
        doTestAxes("patched-eclips/beta-large-negative-BLOCK-IIA.txt", 1.1e-15, 8.4e-15, 4.0e-16);
    }

    @Test
    public void testPatchedSmallNegativeBeta() {
        doTestAxes("patched-eclips/beta-small-negative-BLOCK-IIA.txt", 5.1e-6, 5.1e-6, 5.1e-16);
    }

    @Test
    public void testPatchedCrossingBeta() {
        doTestAxes("patched-eclips/beta-crossing-BLOCK-IIA.txt", 5.2e-4, 5.2e-4, 5.1e-16);
    }

    @Test
    public void testPatchedSmallPositiveBeta() {
        doTestAxes("patched-eclips/beta-small-positive-BLOCK-IIA.txt", 1.1e-5, 1.1e-5, 9.8e-16);
    }

    @Test
    public void testPatchedLargePositiveBeta() {
        doTestAxes("patched-eclips/beta-large-positive-BLOCK-IIA.txt", 9.0e-16, 1.2e-15, 8.0e-16);
    }

    @Test
    public void testOriginalLargeNegativeBeta() {
        doTestAxes("original-eclips/beta-large-negative-BLOCK-IIA.txt", 1.1e-15, 8.4e-15, 4.0e-16);
    }

    @Test
    public void testOriginalSmallNegativeBeta() {
        doTestAxes("original-eclips/beta-small-negative-BLOCK-IIA.txt", 1.2e-3, 1.2e-3, 5.1e-16);
    }

    @Test
    public void testOriginalCrossingBeta() {
        // the very high threshold (2.13 radians) is due to a probable bug in original eclips
        // the output of the routine is limited to the x-sat vector, the yaw angle itself
        // is not output. However, in some cases the x-sat vector is not normalised at all.
        // looking in the reference data file original-eclips/beta-crossing-BLOCK-IIA.txt,
        // one can see that the axis at line 9 is about (-11.3013, -4.4288, -8.8985). The
        // yaw angle extracted from this wrong vector and written as the last field in the
        // same line reads 86.4792°, whereas Orekit value is 21.2255°. However, looking
        // at the log from the original routine, we get:
        //  S           8   494903.73183244199        179.46493643130702        22.444487341252351 ...
        // so we see that the yaw value is 22.4444°, very close to Orekit value.
        // As the testOriginal...() series of tests explicitly do *not* patch the original routine
        // at all, it was not possible to output the internal phi variable to write reference
        // data properly. We also decided to not edit the file to set the correct angle value,
        // as this would imply cheating on the reference
        // This point however does not explain the 2.13 radians error. The 2.13 radians comes
        // from the following point. Here, the original eclips considers the turn has already
        // converged and it jump backs to nominal attitude. The reason is another probable
        // bug in original eclips (which was adressed by our patch number 04). As the Sun
        // crosses plane, the sign of beta changes and the sign of nominal yaw changes. However,
        // the sign of the *linear* yaw is normalized according to the initial beta (betaini),
        // not to the current beta. Near the end of the computation, a test (PHI/YANGLE).LT.0.d0
        // fails and the attitude is set back to nominal, despite it should not (the yaw angle
        // should be about 59° and jumps directly to 179°).
        // As a conclusion, we consider here that the reference output is wrong and that
        // Orekit behaviour is correct, so we increased the threshold so the test pass,
        // and wrote this big comment to explain the situation
        doTestAxes("original-eclips/beta-crossing-BLOCK-IIA.txt", 2.13, 2.13, 5.1e-16);
    }

    @Test
    public void testOriginalSmallPositiveBeta() {
        doTestAxes("original-eclips/beta-small-positive-BLOCK-IIA.txt", 1.2e-3, 1.2e-3, 9.8e-16);
    }

    @Test
    public void testOriginalLargePositiveBeta() {
        doTestAxes("original-eclips/beta-large-positive-BLOCK-IIA.txt", 9.0e-16, 1.2e-15, 8.0e-16);
    }

}
