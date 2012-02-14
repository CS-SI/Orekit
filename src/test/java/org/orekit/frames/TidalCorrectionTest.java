/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.frames;



import org.apache.commons.math3.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;


public class TidalCorrectionTest {

    // Computation date 
    private AbsoluteDate date;

    @Test
    public void testPoleCorrection() {

        // compute the pole motion component for tidal correction
        final PoleCorrection tidalCorr = new TidalCorrection().getPoleCorrection(date);

        Assert.assertEquals(FastMath.toRadians(-204.09237446540885e-6 / 3600), tidalCorr.getXp(), 2.0e-14);
        Assert.assertEquals(FastMath.toRadians(-161.48436351246889e-6 / 3600), tidalCorr.getYp(), 0.7e-14);

    }

    @Test
    public void testDUT1() {

        // compute the dut1 component for tidal correction
        final double tidalCorr = new TidalCorrection().getDUT1(date);

        Assert.assertEquals(13.611556854809763e-6, tidalCorr, 1.5e-10);
    }

    @Test
    public void testCachePole() {

        TidalCorrection tc = new TidalCorrection();

        // compute the pole motion component for tidal correction, testing cache mechanism
        final PoleCorrection poleCorr = tc.getPoleCorrection(date);

        final AbsoluteDate date2 = new AbsoluteDate(2008, 10, 21, TimeScalesFactory.getTAI());

        // compute the pole motion component for tidal correction, testing cache mechanism
        final PoleCorrection poleCorr2 = tc.getPoleCorrection(date2);

        Assert.assertFalse(poleCorr.getXp() == poleCorr2.getXp());
        Assert.assertFalse(poleCorr.getYp() == poleCorr2.getYp());

        // compute the pole motion component for tidal correction, testing cache mechanism
        final PoleCorrection poleCorr3 = tc.getPoleCorrection(date2);

        Assert.assertTrue(poleCorr2.getXp() == poleCorr3.getXp());
        Assert.assertTrue(poleCorr2.getYp() == poleCorr3.getYp());

    }

    @Test
    public void testCacheDUT1() {

        TidalCorrection tc = new TidalCorrection();

        // compute the dut1 component for tidal correction
        final double dut1Corr = tc.getDUT1(date);

        final AbsoluteDate date2 = new AbsoluteDate(2008, 10, 21, TimeScalesFactory.getTAI());

        // compute the dut1 component for tidal correction, testing cache mechanism
        final double dut1Corr2 = tc.getDUT1(date2);

        Assert.assertFalse(dut1Corr == dut1Corr2);

        // compute the dut1 component for tidal correction, testing cache mechanism
        final double dut1Corr3 = tc.getDUT1(date2);

        Assert.assertTrue(dut1Corr2 == dut1Corr3);

    }

    @Before
    public void setUp() {
        date = new AbsoluteDate(2000, 1, 1, TimeScalesFactory.getTAI());
    }

    @After
    public void tearDown() {
        date = null;
    }

}
