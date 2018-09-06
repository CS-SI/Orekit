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
        return new GPSBlockIIF(validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testLargeNegativeBeta() {
        doTestAxes("beta-large-negative-BLOCK-IIF.txt",  1.1e-15, 8.9e-16, 4.2e-16);
    }

    @Test
    public void testSmallNegativeBeta() {
        doTestAxes("beta-small-negative-BLOCK-IIF.txt", 8.2e-13, 8.2e-13, 9.2e-16);
    }

    @Test
    public void testCrossingBeta() {
        doTestAxes("beta-crossing-BLOCK-IIF.txt", 8.8e-3, 8.8e-3, 9.7e-16);
    }

    @Test
    public void testSmallPositiveBeta() {
        doTestAxes("beta-small-positive-BLOCK-IIF.txt", 2.9e-12, 2.9e-12, 5.0e-16);
    }

    @Test
    public void testLargePositiveBeta() {
        doTestAxes("beta-large-positive-BLOCK-IIF.txt", 1.1e-15, 7.7e-16, 3.2e-16);
    }

}
