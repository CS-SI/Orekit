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


public class BeidouMeoTest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new BeidouMeo(validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testLargeNegativeBeta() {
        doTestAxes("beta-large-negative-BEIDOU-2M.txt", 1.1e-15, 1.1e-15, 5.6e-16);
    }

    @Test
    public void testSmallNegativeBeta() {
        doTestAxes("beta-small-negative-BEIDOU-2M.txt", 9.5e-16, 8.9e-16, 4.8e-16);
    }

    @Test
    public void testCrossingBeta() {
        doTestAxes("beta-crossing-BEIDOU-2M.txt", 9.6e-16, 7.8e-16, 3.7e-16);
    }

    @Test
    public void testSmallPositiveBeta() {
        doTestAxes("beta-small-positive-BEIDOU-2M.txt", 8.8e-16, 7.7e-16, 4.1e-16);
    }

    @Test
    public void testLargePositiveBeta() {
        doTestAxes("beta-large-positive-BEIDOU-2M.txt", 9.0e-16, 8.6e-16, 3.1e-16);
    }

}
