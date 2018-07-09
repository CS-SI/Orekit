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
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;


public class BeidouGeoTest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new BeidouGeo(validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testLargeNegativeBeta() throws OrekitException {
        doTestAxes("beta-large-negative-BEIDOU-2G.txt", 8.0e-16, 5.9e-16, 3.4e-16);
    }

    @Test
    public void testSmallNegativeBeta() throws OrekitException {
        doTestAxes("beta-small-negative-BEIDOU-2G.txt", 9.3e-16, 7.6e-16, 4.0e-16);
    }

    @Test
    public void testCrossingBeta() throws OrekitException {
        doTestAxes("beta-crossing-BEIDOU-2G.txt", 9.0e-16, 7.4e-16, 4.6e-16);
    }

    @Test
    public void testSmallPositiveBeta() throws OrekitException {
        doTestAxes("beta-small-positive-BEIDOU-2G.txt", 7.0e-16, 6.8e-16, 4.3e-16);
    }

}
