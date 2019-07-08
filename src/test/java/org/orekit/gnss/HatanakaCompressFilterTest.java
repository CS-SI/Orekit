/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.gnss;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.GzipFilter;
import org.orekit.data.NamedData;
import org.orekit.data.UnixCompressFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class HatanakaCompressFilterTest {
    
    
    @Test
    public void testNotFiltered() throws IOException {
        
        final String name = "rinex/aaaa0000.00o";
        final NamedData raw = new NamedData(name,
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        final NamedData filtered = new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw));
        Assert.assertSame(raw, filtered);
    }

    @Test
    public void testWrongVersion() throws IOException {
        doTestWrong("rinex/vers9990.01d", OrekitMessages.UNSUPPORTED_FILE_FORMAT);
    }

    @Test
    public void testWrongFirstLabel() throws IOException {
        doTestWrong("rinex/labl8880.01d", OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE);
    }

    @Test
    public void testWrongSecondLabel() throws IOException {
        doTestWrong("rinex/labl9990.01d", OrekitMessages.NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE);
    }

    private void doTestWrong(final String name, final OrekitMessages expectedError)
        throws IOException {
        final NamedData raw = new NamedData(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        try {
            new HatanakaCompressFilter().filter(raw).getStreamOpener().openStream();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(expectedError, oe.getSpecifier());
        }
    }

    @Test
    public void testHatanakaRinex2() throws IOException {

        final String name = "rinex/arol0090.01d.Z";
        final NamedData raw = new NamedData(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        NamedData filtered = new HatanakaCompressFilter().filter(new UnixCompressFilter().filter(raw));
        RinexLoader loader = new RinexLoader(filtered.getStreamOpener().openStream(), filtered.getName());

        AbsoluteDate t0 = new AbsoluteDate(2001, 1, 9, TimeScalesFactory.getGPS());
        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assert.assertEquals(921, ods.size());

        Assert.assertEquals("AROL",              ods.get(0).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.GPS, ods.get(0).getSatelliteSystem());
        Assert.assertEquals(24,                  ods.get(0).getPrnNumber());
        Assert.assertEquals(90.0,                ods.get(0).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(7,                   ods.get(0).getObservationData().size());
        Assert.assertEquals(-3351623.823,        ods.get(0).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(-2502276.763,        ods.get(0).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(21472157.836,        ods.get(0).getObservationData().get(2).getValue(), 1.0e-3);
        Assert.assertEquals(21472163.602,        ods.get(0).getObservationData().get(3).getValue(), 1.0e-3);
        Assert.assertTrue(Double.isNaN(ods.get(0).getObservationData().get(4).getValue()));
        Assert.assertEquals(18.7504,             ods.get(0).getObservationData().get(5).getValue(), 1.0e-3);
        Assert.assertEquals(19.7504,             ods.get(0).getObservationData().get(6).getValue(), 1.0e-3);

        Assert.assertEquals("AROL",              ods.get(447).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.GPS, ods.get(447).getSatelliteSystem());
        Assert.assertEquals(10,                  ods.get(447).getPrnNumber());
        Assert.assertEquals(2310.0,              ods.get(447).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(7,                   ods.get(447).getObservationData().size());
        Assert.assertEquals(-8892260.422,        ods.get(447).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(-6823186.119,        ods.get(447).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(22280029.148,        ods.get(447).getObservationData().get(2).getValue(), 1.0e-3);
        Assert.assertEquals(22280035.160,        ods.get(447).getObservationData().get(3).getValue(), 1.0e-3);
        Assert.assertTrue(Double.isNaN(ods.get(447).getObservationData().get(4).getValue()));
        Assert.assertEquals(14.2504,             ods.get(447).getObservationData().get(5).getValue(), 1.0e-3);
        Assert.assertEquals(13.2504,             ods.get(447).getObservationData().get(6).getValue(), 1.0e-3);

        Assert.assertEquals("AROL",              ods.get(920).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.GPS, ods.get(920).getSatelliteSystem());
        Assert.assertEquals(31,                  ods.get(920).getPrnNumber());
        Assert.assertEquals(71430.0,             ods.get(920).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(7,                   ods.get(920).getObservationData().size());
        Assert.assertEquals(-3993480.91843,      ods.get(920).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(-3363000.11542,      ods.get(920).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(24246301.1804,       ods.get(920).getObservationData().get(2).getValue(), 1.0e-3);
        Assert.assertEquals(24246308.9304,       ods.get(920).getObservationData().get(3).getValue(), 1.0e-3);
        Assert.assertTrue(Double.isNaN(ods.get(920).getObservationData().get(4).getValue()));
        Assert.assertEquals(6.2504,              ods.get(920).getObservationData().get(5).getValue(), 1.0e-3);
        Assert.assertEquals(2.2504,              ods.get(920).getObservationData().get(6).getValue(), 1.0e-3);

    }

    @Test
    public void testCompressedRinex3() throws IOException {
        
        //Tests Rinex 3 with Hatanaka compression
        final String name = "rinex/GANP00SVK_R_20151890000_01H_10M_MO.crx.gz";
        final NamedData raw = new NamedData(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        NamedData filtered = new HatanakaCompressFilter().filter(new GzipFilter().filter(raw));
        RinexLoader loader = new RinexLoader(filtered.getStreamOpener().openStream(), filtered.getName());

        AbsoluteDate t0 = new AbsoluteDate(2015, 7, 8, TimeScalesFactory.getGPS());
        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assert.assertEquals(188, ods.size());

        Assert.assertEquals("GANP",                  ods.get(0).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.BEIDOU,  ods.get(0).getSatelliteSystem());
        Assert.assertEquals(2,                       ods.get(0).getPrnNumber());
        Assert.assertEquals(0.0,                     ods.get(0).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(6,                       ods.get(0).getObservationData().size());
        Assert.assertEquals(40517356.773,            ods.get(0).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(40517351.688,            ods.get(0).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(210984654.306,           ods.get(0).getObservationData().get(2).getValue(), 1.0e-3);
        Assert.assertEquals(163146718.773,           ods.get(0).getObservationData().get(3).getValue(), 1.0e-3);
        Assert.assertEquals(35.400,                  ods.get(0).getObservationData().get(4).getValue(), 1.0e-3);
        Assert.assertEquals(37.900,                  ods.get(0).getObservationData().get(5).getValue(), 1.0e-3);

        Assert.assertEquals("GANP",                  ods.get(96).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.GLONASS, ods.get(96).getSatelliteSystem());
        Assert.assertEquals(20,                      ods.get(96).getPrnNumber());
        Assert.assertEquals(1200.0,                  ods.get(96).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(12,                      ods.get(96).getObservationData().size());
        Assert.assertEquals(21579038.953,            ods.get(96).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(21579038.254,            ods.get(96).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(21579044.469,            ods.get(96).getObservationData().get(2).getValue(), 1.0e-3);
        Assert.assertEquals(21579043.914,            ods.get(96).getObservationData().get(3).getValue(), 1.0e-3);
        Assert.assertEquals(115392840.925,           ods.get(96).getObservationData().get(4).getValue(), 1.0e-3);
        Assert.assertEquals(115393074.174,           ods.get(96).getObservationData().get(5).getValue(), 1.0e-3);
        Assert.assertEquals(89750072.711,            ods.get(96).getObservationData().get(6).getValue(), 1.0e-3);
        Assert.assertEquals(89750023.963,            ods.get(96).getObservationData().get(7).getValue(), 1.0e-3);
        Assert.assertEquals(43.800,                  ods.get(96).getObservationData().get(8).getValue(), 1.0e-3);
        Assert.assertEquals(42.500,                  ods.get(96).getObservationData().get(9).getValue(), 1.0e-3);
        Assert.assertEquals(44.000,                  ods.get(96).getObservationData().get(10).getValue(), 1.0e-3);
        Assert.assertEquals(44.000,                  ods.get(96).getObservationData().get(11).getValue(), 1.0e-3);

        Assert.assertEquals("GANP",                  ods.get(187).getHeader().getMarkerName());
        Assert.assertEquals(SatelliteSystem.SBAS,    ods.get(187).getSatelliteSystem());
        Assert.assertEquals(126,                     ods.get(187).getPrnNumber());
        Assert.assertEquals(3000.0,                  ods.get(187).getDate().durationFrom(t0), 1.0e-15);
        Assert.assertEquals(3,                       ods.get(187).getObservationData().size());
        Assert.assertEquals(38446689.984,            ods.get(187).getObservationData().get(0).getValue(), 1.0e-3);
        Assert.assertEquals(202027899.813,           ods.get(187).getObservationData().get(1).getValue(), 1.0e-3);
        Assert.assertEquals(40.200,                  ods.get(187).getObservationData().get(2).getValue(), 1.0e-3);

    }

    @Test
    public void testWith5thOrderDifferencesClockOffsetReinitialization() throws IOException {

        // the following file has several specific features with respect to Hatanaka compression
        //  - we created it using 5th order differences instead of standard 3rd order
        //  - epoch lines do contain a clock offset (which is a dummy value manually edited from original IGS file)
        //  - differences are reinitialized every 20 epochs
        final String name = "rinex/ZIMM00CHE_R_20190320000_15M_30S_MO.crx.gz";
        final NamedData raw = new NamedData(name.substring(name.indexOf('/') + 1),
                                            () -> Utils.class.getClassLoader().getResourceAsStream(name));
        NamedData filtered = new HatanakaCompressFilter().filter(new GzipFilter().filter(raw));
        RinexLoader loader = new RinexLoader(filtered.getStreamOpener().openStream(), filtered.getName());

        List<ObservationDataSet> ods = loader.getObservationDataSets();
        Assert.assertEquals(30, ods.size());
        for (final ObservationDataSet dataSet : ods) {
            Assert.assertEquals(0.123456789012, dataSet.getRcvrClkOffset(), 1.0e-15);
        }
        ObservationDataSet last = ods.get(ods.size() - 1);
        Assert.assertEquals( 24815572.703, last.getObservationData().get(0).getValue(), 1.0e-4);
        Assert.assertEquals(130406727.683, last.getObservationData().get(1).getValue(), 1.0e-4);
    }

}
