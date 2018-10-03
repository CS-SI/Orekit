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


public class BeidouIGSOTest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new BeidouIGSO(validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testPatchedLargeNegativeBeta() {
        doTestAxes("patched-eclips/beta-large-negative-BEIDOU-2I.txt", 1.7e-15, 1.7e-15, 9.5e-16);
    }

    @Test
    public void testPatchedSmallNegativeBeta() {
        doTestAxes("patched-eclips/beta-small-negative-BEIDOU-2I.txt", 1.5e-15, 1.5e-15, 4.4e-16);
    }

    @Test
    public void testPatchedSmallPositiveBeta() {
        doTestAxes("patched-eclips/beta-small-positive-BEIDOU-2I.txt", 3.1e-15, 3.1e-15, 9.9e-16);
    }

    @Test
    public void testPatchedLargePositiveBeta() {
        doTestAxes("patched-eclips/beta-large-positive-BEIDOU-2I.txt", 1.5e-15, 1.5e-15, 1.2e-15);
    }

    @Test
    public void testOriginalLargeNegativeBeta() {
        doTestAxes("original-eclips/beta-large-negative-BEIDOU-2I.txt", 1.7e-15, 1.7e-15, 9.5e-16);
    }

    @Test
    public void testOriginalSmallNegativeBeta() {
        doTestAxes("original-eclips/beta-small-negative-BEIDOU-2I.txt", 3.3e-3, 3.3e-3, 4.6e-16);
    }

    @Test
    public void testOriginalSmallPositiveBeta() {
        doTestAxes("original-eclips/beta-small-positive-BEIDOU-2I.txt", 4.5e-3, 4.5e-3, 9.9e-16);
    }

    @Test
    public void testOriginalLargePositiveBeta() {
        doTestAxes("original-eclips/beta-large-positive-BEIDOU-2I.txt", 1.5e-15, 1.5e-15, 1.2e-15);
    }

}
