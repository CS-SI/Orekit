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
package org.orekit.gnss.antenna;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.antenna.AntexLoader;
import org.orekit.gnss.antenna.SatelliteAntenna;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TimeSpanMap;


public class AntexLoaderTest {

    @Before
    public void setUp() throws OrekitException {
        // Sets the root of data to read
        Utils.setDataRoot("gnss:antex");
    }

    @Test
    public void testSmallAntexFile() throws OrekitException {

        AntexLoader  loader = new AntexLoader("^igs14-small\\.atx$");

        Assert.assertEquals(16, loader.getSatellitesAntennas().size());

        checkSatellite(loader.getSatellitesAntennas().get( 0), 1992, 11, 22,
                       SatelliteSystem.GPS,     "BLOCK IIA",   32, 1,
                       Frequency.G01, 45.0, 7.0, 1.30);
        checkSatellite(loader.getSatellitesAntennas().get( 0), 2008, 10, 23,
                       SatelliteSystem.GPS,     "BLOCK IIA",   37, 1,
                       Frequency.G02, 120.0, 4.0, -0.4);
        checkSatellite(loader.getSatellitesAntennas().get( 0), 2009,  3, 24,
                       SatelliteSystem.GPS,     "BLOCK IIR-M", 49, 1,
                       Frequency.G01, 57.0, 3.0, 4.60);
        checkSatellite(loader.getSatellitesAntennas().get( 0), 2011,  6,  2,
                       SatelliteSystem.GPS,     "BLOCK IIA",   35, 1,
                       Frequency.G02, 25.0, 9.0, 1.20);
        checkSatellite(loader.getSatellitesAntennas().get( 0), 2011,  7, 16,
                       SatelliteSystem.GPS,     "BLOCK IIF",   63, 1,
                       Frequency.G01, 17.0, 2.0, 2.80);

        checkSatellite(loader.getSatellitesAntennas().get( 1), 1989,  6, 10,
                       SatelliteSystem.GPS,     "BLOCK II",    13, 2,
                       Frequency.G02, 0.0, 0.0, -0.80);
        checkSatellite(loader.getSatellitesAntennas().get( 1), 2004, 11,  6,
                       SatelliteSystem.GPS,     "BLOCK IIR-B", 61, 2,
                       Frequency.G01, 270.0, 17.0, 40.60);

        checkSatellite(loader.getSatellitesAntennas().get( 2), 1985, 10,  9,
                       SatelliteSystem.GPS,     "BLOCK I",     11, 3,
                       Frequency.G02, 3.0, 3.0, -0.90);
        checkSatellite(loader.getSatellitesAntennas().get( 2), 1996,  3, 28,
                       SatelliteSystem.GPS,     "BLOCK IIA",   33, 3,
                       Frequency.G01, 34.0, 6.0, 0.80);
        checkSatellite(loader.getSatellitesAntennas().get( 2), 2014,  9,  5,
                       SatelliteSystem.GPS,     "BLOCK IIA",   35, 3,
                       Frequency.G02, 12.0, 10.0, 0.70);
        checkSatellite(loader.getSatellitesAntennas().get( 2), 2014, 10, 29,
                       SatelliteSystem.GPS,     "BLOCK IIF",   69, 3,
                       Frequency.G01, 78.0, 0.5, 5.25);

        checkSatellite(loader.getSatellitesAntennas().get( 3), 1998, 12, 30,
                       SatelliteSystem.GLONASS, "GLONASS",    779, 1,
                       Frequency.R01, 33.0, 9.0, -1.60);
        checkSatellite(loader.getSatellitesAntennas().get( 3), 2004, 12, 26,
                       SatelliteSystem.GLONASS, "GLONASS",    796, 1,
                       Frequency.R02, 114.0, 5.0, -0.20);
        checkSatellite(loader.getSatellitesAntennas().get( 3), 2009, 12, 14,
                       SatelliteSystem.GLONASS, "GLONASS-M",  730, 1,
                       Frequency.R01, 46.0, 3.0, 0.8);

        checkSatellite(loader.getSatellitesAntennas().get( 4), 2003, 12, 10,
                       SatelliteSystem.GLONASS, "GLONASS",    794, 2,
                       Frequency.R02, 67.0, 7.0, -1.10);
        checkSatellite(loader.getSatellitesAntennas().get( 4), 2008, 12, 25,
                       SatelliteSystem.GLONASS, "GLONASS-M",  728, 2,
                       Frequency.R01, 23.0, 6.0, -0.6);
        checkSatellite(loader.getSatellitesAntennas().get( 4), 2013,  7,  1,
                       SatelliteSystem.GLONASS, "GLONASS-M",  747, 2,
                       Frequency.R02, 1.0, 14.0, 1.50);

        checkSatellite(loader.getSatellitesAntennas().get( 5), 1994, 11, 20,
                       SatelliteSystem.GLONASS, "GLONASS",    763, 3,
                       Frequency.R01, 6.0, 1.0, 1.50);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2001, 12,  1,
                       SatelliteSystem.GLONASS, "GLONASS",    789, 3,
                       Frequency.R02, 54.0, 13.0, 0.0);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2008, 12, 25,
                       SatelliteSystem.GLONASS, "GLONASS-M",  727, 3,
                       Frequency.R01, 98.0, 5.0, -0.20);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2010, 10,  1,
                       SatelliteSystem.GLONASS, "GLONASS-M",  722, 3,
                       Frequency.R02, 112.0, 2.0, 1.10);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2010, 12, 16,
                       SatelliteSystem.GLONASS, "GLONASS-M",  727, 3,
                       Frequency.R01, 134.0, 3.0, 0.8);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2011,  3, 11,
                       SatelliteSystem.GLONASS, "GLONASS-M",  715, 3,
                       Frequency.R02, 12.0, 2.5, 0.95);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2011, 10, 13,
                       SatelliteSystem.GLONASS, "GLONASS-K1", 801, 3,
                       Frequency.R01, 345.0, 3.456, 0.0);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2011, 12,  1,
                       SatelliteSystem.GLONASS, "GLONASS-M",  744, 3,
                       Frequency.R02, 360.0, 10.0 / 3.0, 0.60);

        checkSatellite(loader.getSatellitesAntennas().get( 6), 2016,  5, 24,
                       SatelliteSystem.GALILEO, "GALILEO-2",  210, 1,
                       Frequency.E01, 359.0, 4.2, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get( 7), 2016,  5, 24,
                       SatelliteSystem.GALILEO, "GALILEO-2",  211, 2,
                       Frequency.E05, 23.0, 12.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get( 8), 2011, 10, 21,
                       SatelliteSystem.GALILEO, "GALILEO-1",  101, 11,
                       Frequency.E06, 110.0, 22.0 / 3.0, 0.64);

        checkSatellite(loader.getSatellitesAntennas().get( 9), 2010,  1, 16,
                       SatelliteSystem.BEIDOU, "BEIDOU-2G",    3, 1,
                       Frequency.C01, 14.0, 7.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(10), 2010,  7, 31,
                       SatelliteSystem.BEIDOU, "BEIDOU-2I",    5, 6,
                       Frequency.C02, 245.0, 8.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(11), 2012,  4, 29,
                       SatelliteSystem.BEIDOU, "BEIDOU-2M",   12, 11,
                       Frequency.C06, 146.0, 3.2, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(12), 2010,  9, 11,
                       SatelliteSystem.QZSS,    "QZSS",         1, 193,
                       Frequency.J01, 113.0, 6.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(13), 2013,  7,  1,
                       SatelliteSystem.IRNSS,   "IRNSS-1IGSO",  1, 1,
                       Frequency.I05, 34.0, 9.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(14), 2014, 10, 15,
                       SatelliteSystem.IRNSS,   "IRNSS-1GEO",   3, 3,
                       Frequency.I09, 23.0, 10.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(15), 2015,  3, 28,
                       SatelliteSystem.IRNSS,   "IRNSS-1IGSO",  4, 4,
                       Frequency.I05, 321.0, 13.0, 0.0);

        Assert.assertEquals( 3, loader.getReceiversAntennas().size());
        Assert.assertEquals("3S-02-TSADM     NONE",  loader.getReceiversAntennas().get(0).getType());
        Assert.assertEquals("3S-02-TSATE     NONE",  loader.getReceiversAntennas().get(1).getType());
        Assert.assertEquals("AERAT1675_120   SPKE",  loader.getReceiversAntennas().get(2).getType());
    }

    @Test
    public void testWrongColumns() {
        try {
            new AntexLoader("^igs14-wrong-columns\\.atx$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.WRONG_COLUMNS_NUMBER, oe.getSpecifier());
            Assert.assertEquals(25, ((Integer) oe.getParts()[1]).intValue());
            Assert.assertEquals(17, ((Integer) oe.getParts()[2]).intValue());
            Assert.assertEquals(10, ((Integer) oe.getParts()[3]).intValue());
        }
    }

    @Test
    public void testUnknownFrequency() {
        try {
            new AntexLoader("^igs14-unknown-rinex-frequency\\.atx$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_RINEX_FREQUENCY, oe.getSpecifier());
            Assert.assertEquals("U99", (String) oe.getParts()[0]);
            Assert.assertEquals(23, ((Integer) oe.getParts()[2]).intValue());
        }
    }

    @Test
    public void testMismatchedFrequencies() {
        try {
            new AntexLoader("^igs14-mismatched-frequencies\\.atx$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.MISMATCHED_FREQUENCIES, oe.getSpecifier());
            Assert.assertEquals(88, ((Integer) oe.getParts()[1]).intValue());
            Assert.assertEquals("E01", (String) oe.getParts()[2]);
            Assert.assertEquals("E06", (String) oe.getParts()[3]);
        }
    }

    @Test
    public void testWrongLabel() {
        try {
            new AntexLoader("^igs14-unknown-label\\.atx$");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(17, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals("THIS IS NOT AN ANTEX LABEL", ((String) oe.getParts()[2]).substring(60).trim());
        }
    }

    private void checkSatellite(final TimeSpanMap<SatelliteAntenna> tsm,
                                final int year, final int month, final int day,
                                final SatelliteSystem system, final String type,
                                final int satCode, final int prnNumber,
                                final Frequency freq, final double az, final double pol,
                                final double phaseCenterVariation) {
        final double oneMilliSecond = 0.001;
        final AbsoluteDate date = new AbsoluteDate(year, month, day, 0, 0, oneMilliSecond,
                                                   TimeScalesFactory.getGPS());
        final SatelliteAntenna antenna = tsm.get(date);
        Assert.assertEquals(system,         antenna.getSatelliteSystem());
        Assert.assertEquals(type,           antenna.getType());
        Assert.assertEquals(satCode,        antenna.getSatelliteCode());
        Assert.assertEquals(prnNumber,      antenna.getPrnNumber());
        Assert.assertEquals(oneMilliSecond, date.durationFrom(antenna.getValidFrom()), 1.0e-10);
        Assert.assertEquals(phaseCenterVariation * 0.001,
                            antenna.getPhaseCenterVariation(freq,
                                                            new Vector3D(FastMath.toRadians(az),
                                                                         FastMath.toRadians(90 - pol))),
                            1.0e-10);
    }

}
