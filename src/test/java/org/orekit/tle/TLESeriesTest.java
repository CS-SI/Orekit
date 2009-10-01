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
package org.orekit.tle;


import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;


public class TLESeriesTest {

    @Test(expected=OrekitException.class)
    public void testNoTopexPoseidonNumber() throws IOException, OrekitException {
        TLESeries series = new TLESeries("^spot-5\\.tle$", false);
        series.loadTLEData(22076);
    }

    @Test(expected=OrekitException.class)
    public void testNoTopexPoseidonLaunchElements() throws IOException, OrekitException {
        TLESeries series = new TLESeries("^spot-5\\.tle$", false);
        series.loadTLEData(1992, 52, "A");
    }

    @Test
    public void testSpot5() throws IOException, OrekitException {

        TLESeries series = new TLESeries("^spot-5\\.tle$", false);

        series.loadTLEData(-1);
        Assert.assertEquals(27421, series.getFirst().getSatelliteNumber());

        series.loadTLEData(27421);
        Assert.assertEquals(27421, series.getFirst().getSatelliteNumber());

        series.loadTLEData(-1, -1, null);
        Assert.assertEquals(27421, series.getFirst().getSatelliteNumber());

        series.loadTLEData(2002, 21, "A");
        Assert.assertEquals(27421, series.getFirst().getSatelliteNumber());
        Assert.assertEquals(2002, series.getFirst().getLaunchYear());
        Assert.assertEquals(21, series.getFirst().getLaunchNumber());
        Assert.assertEquals("A", series.getFirst().getLaunchPiece());
        Assert.assertEquals(27421, series.getLast().getSatelliteNumber());
        Assert.assertEquals(2002, series.getLast().getLaunchYear());
        Assert.assertEquals(21, series.getLast().getLaunchNumber());
        Assert.assertEquals("A", series.getLast().getLaunchPiece());

        Assert.assertEquals(0,
                     series.getFirstDate().durationFrom(new AbsoluteDate(new DateComponents(2002, 05, 04),
                                                                  new TimeComponents(11, 45, 15.695),
                                                                  TimeScalesFactory.getUTC())),
                                                                  1e-3);
        Assert.assertEquals(0,
                     series.getLastDate().durationFrom(new AbsoluteDate(new DateComponents(2002, 06, 24),
                                                                 new TimeComponents(18, 12, 44.592),
                                                                 TimeScalesFactory.getUTC())),
                                                                 1e-3);

        AbsoluteDate mid = new AbsoluteDate(new DateComponents(2002, 06, 02),
                                            new TimeComponents(11, 12, 15),
                                            TimeScalesFactory.getUTC());
        Assert.assertEquals(0,
                     series.getClosestTLE(mid).getDate().durationFrom(new AbsoluteDate(new DateComponents(2002, 6, 2),
                                                                                 new TimeComponents(10, 8, 25.401),
                                                                                 TimeScalesFactory.getUTC())),
                                                                                 1e-3);
        mid = new AbsoluteDate(new DateComponents(2001, 06, 02),
                               new TimeComponents(11, 12, 15),
                               TimeScalesFactory.getUTC());
        Assert.assertTrue(series.getClosestTLE(mid).getDate().equals(series.getFirstDate()));
        mid = new AbsoluteDate(new DateComponents(2003, 06, 02),
                               new TimeComponents(11, 12, 15),
                               TimeScalesFactory.getUTC());
        Assert.assertTrue(series.getClosestTLE(mid).getDate().equals(series.getLastDate()));

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}