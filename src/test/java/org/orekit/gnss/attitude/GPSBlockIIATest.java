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
    public void testLargeNegativeBeta() {
        doTestAxes("beta-large-negative-BLOCK-IIA.txt", 1.1e-15, 8.4e-15, 4.0e-16);
    }

    @Test
    public void testSmallNegativeBeta() {
        doTestAxes("beta-small-negative-BLOCK-IIA.txt", 5.1e-6, 5.1e-6, 6.2e-16);
    }

    @Test
    public void testCrossingBeta() {
        doTestAxes("beta-crossing-BLOCK-IIA.txt", 0.049e-100, 0.049e-100, 1.0e-15);
    }

    @Test
    public void testSmallPositiveBeta() {
        doTestAxes("beta-small-positive-BLOCK-IIA.txt", 1.1e-5, 1.1e-5, 9.8e-16);
    }

    @Test
    public void testLargePositiveBeta() {
        doTestAxes("beta-large-positive-BLOCK-IIA.txt", 9.0e-16, 1.2e-15, 8.0e-16);
    }

}
