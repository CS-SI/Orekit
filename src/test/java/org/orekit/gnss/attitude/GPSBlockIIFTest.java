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


public class GPSBlockIIFTest extends AbstractGNSSAttitudeProviderTest {

    protected GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                  final AbsoluteDate validityEnd,
                                                  final ExtendedPVCoordinatesProvider sun,
                                                  final Frame inertialFrame,
                                                  final int prnNumber) {
        return new GPSBlockIIF(validityStart, validityEnd, sun, inertialFrame);
    }

    @Test
    public void testLargeNegativeBeta() throws OrekitException {
        doTestAxes("beta-large-negative-BLOCK-IIF.txt",  1.1e-15, 8.9e-16, 4.2e-16);
    }

    @Test
    public void testSmallNegativeBeta() throws OrekitException {
        // the differences with the reference Kouba models are due to the following changes:
        // - Orekit compuptes angular velocity taking eccentricity into account
        //   Kouba assumes a perfectly circular orbit
        // - Orekit uses spherical geometry to solve some triangles (cos μ = cos α / cos β)
        //   Kouba uses projected planar geometry (μ² = α² - β²)
        // when using the Kouba equations, the order of magnitudes of the differences is about 10⁻¹²
        doTestAxes("beta-small-negative-BLOCK-IIF.txt", 4.1e-5, 4.1e-5, 9.2e-16);
    }

    @Test
    public void testCrossingBeta() throws OrekitException {
        // TODO: these results are not good,
        // however the reference data is also highly suspicious
        // this needs to be investigated
        doTestAxes("beta-crossing-BLOCK-IIF.txt", 2.8, 2.8, 7.8e-16);
    }

    @Test
    public void testSmallPositiveBeta() throws OrekitException {
        // the differences with the reference Kouba models are due to the following changes:
        // - Orekit compuptes angular velocity taking eccentricity into account
        //   Kouba assumes a perfectly circular orbit
        // - Orekit uses spherical geometry to solve some triangles (cos μ = cos α / cos β)
        //   Kouba uses projected planar geometry (μ² = α² - β²)
        // when using the Kouba equations, the order of magnitudes of the differences is about 10⁻¹²
        doTestAxes("beta-small-positive-BLOCK-IIF.txt", 4.1e-5, 4.1e-5, 4.8e-16);
    }

    @Test
    public void testLargePositiveBeta() throws OrekitException {
        doTestAxes("beta-large-positive-BLOCK-IIF.txt", 1.1e-15, 7.7e-16, 3.2e-16);
    }

}
