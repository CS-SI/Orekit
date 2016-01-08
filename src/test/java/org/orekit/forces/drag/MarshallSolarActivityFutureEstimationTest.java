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
package org.orekit.forces.drag;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.Month;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

public class MarshallSolarActivityFutureEstimationTest {

    @Test
    public void testFileDate() throws OrekitException {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assert.assertEquals(new DateComponents(2010, Month.NOVEMBER, 1),
                            msafe.getFileDate(new AbsoluteDate("2010-05-01", utc)));
        Assert.assertEquals(new DateComponents(2010, Month.DECEMBER, 1),
                            msafe.getFileDate(new AbsoluteDate("2010-06-01", utc)));
        Assert.assertEquals(new DateComponents(2011, Month.JANUARY, 1),
                            msafe.getFileDate(new AbsoluteDate("2010-07-01", utc)));
        Assert.assertEquals(new DateComponents(2011, Month.JANUARY, 1),
                            msafe.getFileDate(new AbsoluteDate("2030-01-01", utc)));

    }

    @Test
    public void testFluxStrong() throws OrekitException {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        Assert.assertEquals(94.2,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-10);
        Assert.assertEquals(96.6,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            1.0e-10);
        Assert.assertEquals(99.0,
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG,
                            msafe.getStrengthLevel());

    }


    @Test
    public void testFluxAverage() throws OrekitException {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assert.assertEquals(87.6,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-10);
        Assert.assertEquals(88.7,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            1.0e-10);
        Assert.assertEquals(89.8,
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE,
                            msafe.getStrengthLevel());
    }


    @Test
    public void testFluxWeak() throws OrekitException {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(80.4,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-10);
        Assert.assertEquals(80.6,
                            msafe.getMeanFlux(new AbsoluteDate("2010-10-16T12:00:00", utc)),
                            1.0e-10);
        Assert.assertEquals(80.8,
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(msafe.getInstantFlux(new AbsoluteDate("2010-11-01", utc)),
                            msafe.getMeanFlux(new AbsoluteDate("2010-11-01", utc)),
                            1.0e-10);
        Assert.assertEquals(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK,
                            msafe.getStrengthLevel());

    }

    private MarshallSolarActivityFutureEstimation loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel strength)
        throws OrekitException {

        MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation("(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}F10\\.(?:txt|TXT)",
                                                      strength);
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.feed(msafe.getSupportedNames(), msafe);
        return msafe;
    }

    @Test
    public void testKpStrong() throws OrekitException {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        Assert.assertEquals(2 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-14);
        Assert.assertEquals(3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", utc)),
                            1.0e-14);

        // this one should get exactly to an element of the AP_ARRAY: ap = 7.0
        Assert.assertEquals(2.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);
        Assert.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", utc)),
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);

    }

    @Test
    public void testKpAverage() throws OrekitException {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        Assert.assertEquals(2 - 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-14);
        Assert.assertEquals(2 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", utc)),
                            1.0e-14);
        Assert.assertEquals(2.0 - 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);
        Assert.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", utc)),
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);

    }

    @Test
    public void testKpWeak() throws OrekitException {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(1 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-10-01", utc)),
                            1.0e-14);
        Assert.assertEquals(2.0,
                            msafe.get24HoursKp(new AbsoluteDate("2011-05-01", utc)),
                            1.0e-14);
        Assert.assertEquals(1 + 1.0 / 3.0,
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);
        Assert.assertEquals(msafe.getThreeHourlyKP(new AbsoluteDate("2010-08-01", utc)),
                            msafe.get24HoursKp(new AbsoluteDate("2010-08-01", utc)),
                            1.0e-14);

    }

    @Test
    public void testMinDate() throws OrekitException {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(new AbsoluteDate("2010-05-01", utc), msafe.getMinDate());
        Assert.assertEquals(78.1,
                            msafe.getMeanFlux(msafe.getMinDate()),
                            1.0e-14);
    }

    @Test
    public void testMaxDate() throws OrekitException {

        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        Assert.assertEquals(new AbsoluteDate("2030-10-01", utc), msafe.getMaxDate());
        Assert.assertEquals(67.0,
                            msafe.getMeanFlux(msafe.getMaxDate()),
                            1.0e-14);
    }

    @Test(expected=OrekitException.class)
    public void testPastOutOfRange() throws OrekitException {
        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        msafe.get24HoursKp(new AbsoluteDate("1960-10-01", utc));
    }

    @Test(expected=OrekitException.class)
    public void testFutureOutOfRange() throws OrekitException {
        MarshallSolarActivityFutureEstimation msafe =
            loadMsafe(MarshallSolarActivityFutureEstimation.StrengthLevel.WEAK);
        msafe.get24HoursKp(new AbsoluteDate("2060-10-01", utc));
    }

    @Test(expected=OrekitException.class)
    public void testExtraData() throws OrekitException {
        MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation("Jan2011F10-extra-data\\.txt",
                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.feed(msafe.getSupportedNames(), msafe);
    }

    @Test(expected=OrekitException.class)
    public void testNoData() throws OrekitException {
        MarshallSolarActivityFutureEstimation msafe =
            new MarshallSolarActivityFutureEstimation("Jan2011F10-no-data\\.txt",
                                                      MarshallSolarActivityFutureEstimation.StrengthLevel.STRONG);
        DataProvidersManager manager = DataProvidersManager.getInstance();
        manager.feed(msafe.getSupportedNames(), msafe);
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:atmosphere");
        utc = TimeScalesFactory.getUTC();
    }

    @After
    public void tearDown() {
        utc   = null;
    }

    private TimeScale utc;

}
