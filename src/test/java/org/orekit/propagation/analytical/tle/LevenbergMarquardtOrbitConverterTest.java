/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation.analytical.tle;

import org.junit.Test;
import org.orekit.errors.OrekitException;

public class LevenbergMarquardtOrbitConverterTest extends AbstractTLEFitterTest {

    @Test
    public void testConversionGeoPositionVelocity() throws OrekitException {
        checkFit(getGeoTLE(), 86400, 300, 1.0e-3, false, false, 8.600e-8);
    }

    @Test
    public void testConversionGeoPositionOnly() throws OrekitException {
        checkFit(getGeoTLE(), 86400, 300, 1.0e-3, true, false, 1.059e-7);
    }

    @Test
    public void testConversionLeoPositionVelocityWithoutBStar() throws OrekitException {
        checkFit(getLeoTLE(), 86400, 300, 1.0e-3, false, false, 10.77);
    }

    @Test
    public void testConversionLeoPositionOnlyWithoutBStar() throws OrekitException {
        checkFit(getLeoTLE(), 86400, 300, 1.0e-3, true, false, 15.23);
    }

    @Test
    public void testConversionLeoPositionVelocityWithBStar() throws OrekitException {
        checkFit(getLeoTLE(), 86400, 300, 1.0e-3, false, true, 9.920e-7);
    }

    @Test
    public void testConversionLeoPositionOnlyWithBStar() throws OrekitException {
        checkFit(getLeoTLE(), 86400, 300, 1.0e-3, true, true, 1.147e-6);
    }

    protected AbstractTLEFitter getFitter(final TLE tle) {
        return new LevenbergMarquardtOrbitConverter(1000,
                                                    tle.getSatelliteNumber(), tle.getClassification(),
                                                    tle.getLaunchYear(), tle.getLaunchNumber(), tle.getLaunchPiece(),
                                                    tle.getElementNumber(), tle.getRevolutionNumberAtEpoch());
    }

}

