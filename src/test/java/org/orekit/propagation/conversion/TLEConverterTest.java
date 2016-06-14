/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.conversion;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;

public class TLEConverterTest {

    private TLE geoTLE;

    private TLE leoTLE;

    @Test
    public void testConversionGeoPositionVelocity() throws OrekitException {
        checkFit(geoTLE, 86400, 300, 1.0e-3, false, false, 8.540e-8);
    }

    @Test
    public void testConversionGeoPositionOnly() throws OrekitException {
        checkFit(geoTLE, 86400, 300, 1.0e-3, true, false, 1.253e-7);
    }

    @Test
    public void testConversionLeoPositionVelocityWithoutBStar() throws OrekitException {
        checkFit(leoTLE, 86400, 300, 1.0e-3, false, false, 10.77);
    }

    @Test
    public void testConversionLeoPositionOnlyWithoutBStar() throws OrekitException {
        checkFit(leoTLE, 86400, 300, 1.0e-3, true, false, 15.23);
    }

    @Test
    public void testConversionLeoPositionVelocityWithBStar() throws OrekitException {
        checkFit(leoTLE, 86400, 300, 1.0e-3, false, true, 1.614e-7);
    }

    @Test
    public void testConversionLeoPositionOnlyWithBStar() throws OrekitException {
        checkFit(leoTLE, 86400, 300, 1.0e-3, true, true, 4.285e-7);
    }

    protected void checkFit(final TLE tle,
                            final double duration,
                            final double stepSize,
                            final double threshold,
                            final boolean positionOnly,
                            final boolean withBStar,
                            final double expectedRMS)
        throws OrekitException {

        Propagator p = TLEPropagator.selectExtrapolator(tle);
        List<SpacecraftState> sample = new ArrayList<SpacecraftState>();
        for (double dt = 0; dt < duration; dt += stepSize) {
            sample.add(p.propagate(tle.getDate().shiftedBy(dt)));
        }

        TLEPropagatorBuilder builder = new TLEPropagatorBuilder(tle, PositionAngle.TRUE, 1.0);

        FiniteDifferencePropagatorConverter fitter = new FiniteDifferencePropagatorConverter(builder, threshold, 1000);

        if (withBStar) {
            fitter.convert(sample, positionOnly, TLEPropagatorBuilder.B_STAR);
        } else {
            fitter.convert(sample, positionOnly);
        }

        TLEPropagator prop = (TLEPropagator)fitter.getAdaptedPropagator();
        TLE fitted = prop.getTLE();

        Assert.assertEquals(expectedRMS, fitter.getRMS(), 0.001 * expectedRMS);

        Assert.assertEquals(tle.getSatelliteNumber(),         fitted.getSatelliteNumber());
        Assert.assertEquals(tle.getClassification(),          fitted.getClassification());
        Assert.assertEquals(tle.getLaunchYear(),              fitted.getLaunchYear());
        Assert.assertEquals(tle.getLaunchNumber(),            fitted.getLaunchNumber());
        Assert.assertEquals(tle.getLaunchPiece(),             fitted.getLaunchPiece());
        Assert.assertEquals(tle.getElementNumber(),           fitted.getElementNumber());
        Assert.assertEquals(tle.getRevolutionNumberAtEpoch(), fitted.getRevolutionNumberAtEpoch());

        final double eps = 1.0e-5;
        Assert.assertEquals(tle.getMeanMotion(), fitted.getMeanMotion(), eps * tle.getMeanMotion());
        Assert.assertEquals(tle.getE(), fitted.getE(), eps * tle.getE());
        Assert.assertEquals(tle.getI(), fitted.getI(), eps * tle.getI());
        Assert.assertEquals(tle.getPerigeeArgument(), fitted.getPerigeeArgument(), eps * tle.getPerigeeArgument());
        Assert.assertEquals(tle.getRaan(), fitted.getRaan(), eps * tle.getRaan());
        Assert.assertEquals(tle.getMeanAnomaly(), fitted.getMeanAnomaly(), eps * tle.getMeanAnomaly());

        if (withBStar) {
            Assert.assertEquals(tle.getBStar(), fitted.getBStar(), eps * tle.getBStar());
        }

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
        geoTLE = new TLE("1 27508U 02040A   12021.25695307 -.00000113  00000-0  10000-3 0  7326",
                         "2 27508   0.0571 356.7800 0005033 344.4621 218.7816  1.00271798 34501");
        leoTLE = new TLE("1 31135U 07013A   11003.00000000  .00000816  00000+0  47577-4 0    11",
                         "2 31135   2.4656 183.9084 0021119 236.4164  60.4567 15.10546832    15");
    }

}

