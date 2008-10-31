/* Copyright 2002-2008 CS Communication & Systèmes
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TAIScale;


public class TidalCorrectionTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;

    public void testPoleCorrection() {

        // compute the pole motion component for tidal correction
        final PoleCorrection tidalCorr = TidalCorrection.getInstance().getPoleCorrection(date);

        assertEquals(Math.toRadians(-204.09237446540885e-6 / 3600), tidalCorr.getXp(), 2.0e-14);
        assertEquals(Math.toRadians(-161.48436351246889e-6 / 3600), tidalCorr.getYp(), 0.7e-14);

    }

    public void testDUT1() {

        // compute the dut1 component for tidal correction
        final double tidalCorr = TidalCorrection.getInstance().getDUT1(date);

        assertEquals(13.611556854809763e-6, tidalCorr, 1.5e-10);
    }

    public void testCachePole() {

        // compute the pole motion component for tidal correction, testing cache mechanism
        final PoleCorrection poleCorr = TidalCorrection.getInstance().getPoleCorrection(date);

        final AbsoluteDate date2 = new AbsoluteDate(2008, 10, 21, TAIScale.getInstance());

        // compute the pole motion component for tidal correction, testing cache mechanism
        final PoleCorrection poleCorr2 = TidalCorrection.getInstance().getPoleCorrection(date2);

        assertFalse(poleCorr.getXp() == poleCorr2.getXp());
        assertFalse(poleCorr.getYp() == poleCorr2.getYp());

        // compute the pole motion component for tidal correction, testing cache mechanism
        final PoleCorrection poleCorr3 = TidalCorrection.getInstance().getPoleCorrection(date2);

        assertTrue(poleCorr2.getXp() == poleCorr3.getXp());
        assertTrue(poleCorr2.getYp() == poleCorr3.getYp());

    }

    public void testCacheDUT1() {

        // compute the dut1 component for tidal correction
        final double dut1Corr = TidalCorrection.getInstance().getDUT1(date);

        final AbsoluteDate date2 = new AbsoluteDate(2008, 10, 21, TAIScale.getInstance());

        // compute the dut1 component for tidal correction, testing cache mechanism
        final double dut1Corr2 = TidalCorrection.getInstance().getDUT1(date2);

        assertFalse(dut1Corr == dut1Corr2);

        // compute the dut1 component for tidal correction, testing cache mechanism
        final double dut1Corr3 = TidalCorrection.getInstance().getDUT1(date2);

        assertTrue(dut1Corr2 == dut1Corr3);

    }

    public void setUp() {
        date = new AbsoluteDate(2000, 1, 1, TAIScale.getInstance());
    }

    public void tearDown() {
        date = null;
    }

    public static Test suite() {
        return new TestSuite(TidalCorrectionTest.class);
    }

}
