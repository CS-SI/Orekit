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


public class GalileoTest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new Galileo(validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testLargeNegativeBeta() {
        doTestAxes("beta-large-negative-GALILEO.txt", 1.3e-15, 1.2e-15, 5.5e-16);
    }

    @Test
    public void testSmallNegativeBeta() {
        doTestAxes("beta-small-negative-GALILEO.txt", 2.4e-4, 2.4e-4, 5.5e-16);
    }

    @Test
    public void testCrossingBeta() {
        // TODO: these results are not good,
        // however the reference data is also highly suspicious
        // this needs to be investigated
        doTestAxes("beta-crossing-GALILEO.txt", 2.0e-100, 2.0e-100, 5.8e-16);
    }

    @Test
    public void testSmallPositiveBeta() {
        doTestAxes("beta-small-positive-GALILEO.txt", 8.3e-4, 8.3e-4, 7.8e-16);
    }

    @Test
    public void testLargePositiveBeta() {
        doTestAxes("beta-large-positive-GALILEO.txt", 1.4e-15, 5.5e-16, 7.1e-16);
    }

}
