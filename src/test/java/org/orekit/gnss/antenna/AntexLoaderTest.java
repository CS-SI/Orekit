/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import java.net.URISyntaxException;
import java.net.URL;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TimeSpanMap;


public class AntexLoaderTest {

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss:antex");
    }

    @Test
    public void testSmallAntex14File() throws URISyntaxException {

        final URL url = AntexLoaderTest.class.getClassLoader().getResource("antex/igs14-small.atx");
        AntexLoader  loader = new AntexLoader(new DataSource(url.toURI()),
                                              TimeScalesFactory.getGPS());

        Assertions.assertEquals(16, loader.getSatellitesAntennas().size());

        checkSatellite(loader.getSatellitesAntennas().get( 0), 1992, 11, 22, 2008, 10, 16,
                       SatelliteSystem.GPS,     "BLOCK IIA",   SatelliteType.BLOCK_IIA, 32, 1,
                       "1992-079A", Frequency.G01, 45.0, 7.0, 1.30);
        checkSatellite(loader.getSatellitesAntennas().get( 0), 2008, 10, 23, 2009,  1,  6,
                       SatelliteSystem.GPS,     "BLOCK IIA",   SatelliteType.BLOCK_IIA, 37, 1,
                       "1993-032A", Frequency.G02, 120.0, 4.0, -0.4);
        checkSatellite(loader.getSatellitesAntennas().get( 0), 2009,  3, 24, 2011,  5,  6,
                       SatelliteSystem.GPS,     "BLOCK IIR-M", SatelliteType.BLOCK_IIR_M, 49, 1,
                       "2009-014A", Frequency.G01, 57.0, 3.0, 4.60);
        checkSatellite(loader.getSatellitesAntennas().get( 0), 2011,  6,  2, 2011,  7, 12,
                       SatelliteSystem.GPS,     "BLOCK IIA",   SatelliteType.BLOCK_IIA, 35, 1,
                       "1993-054A", Frequency.G02, 25.0, 9.0, 1.20);
        checkSatellite(loader.getSatellitesAntennas().get( 0), 2011,  7, 16, 9999, 12, 31,
                       SatelliteSystem.GPS,     "BLOCK IIF",   SatelliteType.BLOCK_IIF, 63, 1,
                       "2011-036A", Frequency.G01, 17.0, 2.0, 2.80);

        checkSatellite(loader.getSatellitesAntennas().get( 1), 1989,  6, 10, 2004,  5,  12,
                       SatelliteSystem.GPS,     "BLOCK II",    SatelliteType.BLOCK_II, 13, 2,
                       "1989-044A", Frequency.G02, 0.0, 0.0, -0.80);
        checkSatellite(loader.getSatellitesAntennas().get( 1), 2004, 11,  6, 9999, 12, 31,
                       SatelliteSystem.GPS,     "BLOCK IIR-B", SatelliteType.BLOCK_IIR_B, 61, 2,
                       "2004-045A", Frequency.G01, 270.0, 17.0, 40.60);

        checkSatellite(loader.getSatellitesAntennas().get( 2), 1985, 10,  9, 1994,  4,  17,
                       SatelliteSystem.GPS,     "BLOCK I",     SatelliteType.BLOCK_I, 11, 3,
                       "1985-093A", Frequency.G02, 3.0, 3.0, -0.90);
        checkSatellite(loader.getSatellitesAntennas().get( 2), 1996,  3, 28, 2014,  8,  18,
                       SatelliteSystem.GPS,     "BLOCK IIA",   SatelliteType.BLOCK_IIA, 33, 3,
                       "1996-019A", Frequency.G01, 34.0, 6.0, 0.80);
        checkSatellite(loader.getSatellitesAntennas().get( 2), 2014,  9,  5, 2014,  10, 20,
                       SatelliteSystem.GPS,     "BLOCK IIA",   SatelliteType.BLOCK_IIA, 35, 3,
                       "1993-054A", Frequency.G02, 12.0, 10.0, 0.70);
        checkSatellite(loader.getSatellitesAntennas().get( 2), 2014, 10, 29, 9999, 12, 31,
                       SatelliteSystem.GPS,     "BLOCK IIF",   SatelliteType.BLOCK_IIF, 69, 3,
                       "2014-068A", Frequency.G01, 78.0, 0.5, 5.25);

        checkSatellite(loader.getSatellitesAntennas().get( 3), 1998, 12, 30, 2004, 12, 25,
                       SatelliteSystem.GLONASS, "GLONASS",     SatelliteType.GLONASS, 779, 1,
                       "1998-077A", Frequency.R01, 33.0, 9.0, -1.60);
        checkSatellite(loader.getSatellitesAntennas().get( 3), 2004, 12, 26, 2009, 12, 13,
                       SatelliteSystem.GLONASS, "GLONASS",     SatelliteType.GLONASS, 796, 1,
                       "2004-053A", Frequency.R02, 114.0, 5.0, -0.20);
        checkSatellite(loader.getSatellitesAntennas().get( 3), 2009, 12, 14, 9999, 12, 31,
                       SatelliteSystem.GLONASS, "GLONASS-M",  SatelliteType.GLONASS_M, 730, 1,
                       "2009-070A", Frequency.R01, 46.0, 3.0, 0.8);

        checkSatellite(loader.getSatellitesAntennas().get( 4), 2003, 12, 10, 2008, 12, 24,
                       SatelliteSystem.GLONASS, "GLONASS",     SatelliteType.GLONASS, 794, 2,
                       "2003-056B", Frequency.R02, 67.0, 7.0, -1.10);
        checkSatellite(loader.getSatellitesAntennas().get( 4), 2008, 12, 25, 2013,  6, 30,
                       SatelliteSystem.GLONASS, "GLONASS-M",   SatelliteType.GLONASS_M, 728, 2,
                       "2008-067C", Frequency.R01, 23.0, 6.0, -0.6);
        checkSatellite(loader.getSatellitesAntennas().get( 4), 2013,  7,  1, 9999, 12, 31,
                       SatelliteSystem.GLONASS, "GLONASS-M",   SatelliteType.GLONASS_M, 747, 2,
                       "2013-019A", Frequency.R02, 1.0, 14.0, 1.50);

        checkSatellite(loader.getSatellitesAntennas().get( 5), 1994, 11, 20, 2001, 11, 30,
                       SatelliteSystem.GLONASS, "GLONASS",     SatelliteType.GLONASS, 763, 3,
                       "1994-076A", Frequency.R01, 6.0, 1.0, 1.50);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2001, 12,  1, 2008, 12, 24,
                       SatelliteSystem.GLONASS, "GLONASS",     SatelliteType.GLONASS, 789, 3,
                       "2001-053B", Frequency.R02, 54.0, 13.0, 0.0);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2008, 12, 25, 2010,  9, 30,
                       SatelliteSystem.GLONASS, "GLONASS-M",   SatelliteType.GLONASS_M, 727, 3,
                       "2008-067A", Frequency.R01, 98.0, 5.0, -0.20);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2010, 10,  1, 2010, 12, 15,
                       SatelliteSystem.GLONASS, "GLONASS-M",   SatelliteType.GLONASS_M, 722, 3,
                       "2007-065B", Frequency.R02, 112.0, 2.0, 1.10);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2010, 12, 16, 2011,  3, 10,
                       SatelliteSystem.GLONASS, "GLONASS-M",   SatelliteType.GLONASS_M, 727, 3,
                       "2008-067A", Frequency.R01, 134.0, 3.0, 0.8);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2011,  3, 11, 2011, 10, 12,
                       SatelliteSystem.GLONASS, "GLONASS-M",   SatelliteType.GLONASS_M, 715, 3,
                       "2006-062C", Frequency.R02, 12.0, 2.5, 0.95);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2011, 10, 13, 2011, 11, 30,
                       SatelliteSystem.GLONASS, "GLONASS-K1",  SatelliteType.GLONASS_K1, 801, 3,
                       "2011-009A", Frequency.R01, 345.0, 3.456, 0.0);
        checkSatellite(loader.getSatellitesAntennas().get( 5), 2011, 12,  1, 9999, 12, 31,
                       SatelliteSystem.GLONASS, "GLONASS-M",   SatelliteType.GLONASS_M, 744, 3,
                       "2011-064A", Frequency.R02, 360.0, 10.0 / 3.0, 0.60);

        checkSatellite(loader.getSatellitesAntennas().get( 6), 2016,  5, 24, 9999, 12, 31,
                       SatelliteSystem.GALILEO, "GALILEO-2",   SatelliteType.GALILEO_2, 210, 1,
                       "2016-030B", Frequency.E01, 359.0, 4.2, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get( 7), 2016,  5, 24, 9999, 12, 31,
                       SatelliteSystem.GALILEO, "GALILEO-2",   SatelliteType.GALILEO_2, 211, 2,
                       "2016-030A", Frequency.E05, 23.0, 12.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get( 8), 2011, 10, 21, 9999, 12, 31,
                       SatelliteSystem.GALILEO, "GALILEO-1",   SatelliteType.GALILEO_1, 101, 11,
                       "2011-060A", Frequency.E06, 110.0, 22.0 / 3.0, 0.64);

        checkSatellite(loader.getSatellitesAntennas().get( 9), 2010,  1, 16, 9999, 12, 31,
                       SatelliteSystem.BEIDOU, "BEIDOU-2G",    SatelliteType.BEIDOU_2G, 3, 1,
                       "2010-001A", Frequency.C01, 14.0, 7.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(10), 2010,  7, 31, 9999, 12, 31,
                       SatelliteSystem.BEIDOU, "BEIDOU-2I",    SatelliteType.BEIDOU_2I, 5, 6,
                       "2010-036A", Frequency.C02, 245.0, 8.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(11), 2012,  4, 29, 9999, 12, 31,
                       SatelliteSystem.BEIDOU, "BEIDOU-2M",    SatelliteType.BEIDOU_2M, 12, 11,
                       "2012-018A", Frequency.C06, 146.0, 3.2, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(12), 2010,  9, 11, 9999, 12, 31,
                       SatelliteSystem.QZSS,    "QZSS",        SatelliteType.QZSS, 1, 193,
                       "2010-045A", Frequency.J01, 113.0, 6.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(13), 2013,  7,  1, 9999, 12, 31,
                       SatelliteSystem.IRNSS,   "IRNSS-1IGSO", SatelliteType.IRNSS_1IGSO, 1, 1,
                       "2013-034A", Frequency.I05, 34.0, 9.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(14), 2014, 10, 15, 9999, 12, 31,
                       SatelliteSystem.IRNSS,   "IRNSS-1GEO",  SatelliteType.IRNSS_1GEO, 3, 3,
                       "2014-061A", Frequency.I09, 23.0, 10.0, 0.0);

        checkSatellite(loader.getSatellitesAntennas().get(15), 2015,  3, 28, 9999, 12, 31,
                       SatelliteSystem.IRNSS,   "IRNSS-1IGSO", SatelliteType.IRNSS_1IGSO, 4, 4,
                       "2015-018A", Frequency.I05, 321.0, 13.0, 0.0);

        Assertions.assertEquals( 3, loader.getReceiversAntennas().size());
        Assertions.assertEquals("3S-02-TSADM     NONE",  loader.getReceiversAntennas().get(0).getType());
        Assertions.assertEquals("",                      loader.getReceiversAntennas().get(0).getSerialNumber());
        Assertions.assertEquals("3S-02-TSATE     NONE",  loader.getReceiversAntennas().get(1).getType());
        Assertions.assertEquals("",                      loader.getReceiversAntennas().get(1).getSerialNumber());
        Assertions.assertEquals("AERAT1675_120   SPKE",  loader.getReceiversAntennas().get(2).getType());
        Assertions.assertEquals("",                      loader.getReceiversAntennas().get(2).getSerialNumber());
        Assertions.assertEquals(1, loader.getReceiversAntennas().get(2).getFrequencies().size());
        Assertions.assertEquals(Frequency.G01, loader.getReceiversAntennas().get(2).getFrequencies().get(0));
        try {
            loader.getReceiversAntennas().get(2).getEccentricities(Frequency.E06);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_FREQUENCY_FOR_ANTENNA, oe.getSpecifier());
            Assertions.assertSame(Frequency.E06, oe.getParts()[0]);
            Assertions.assertEquals("AERAT1675_120   SPKE", oe.getParts()[1]);
        }
        Assertions.assertEquals(-0.00001, loader.getReceiversAntennas().get(2).getEccentricities(Frequency.G01).getX(), 1.0e-15);
        Assertions.assertEquals(+0.00057, loader.getReceiversAntennas().get(2).getEccentricities(Frequency.G01).getY(), 1.0e-15);
        Assertions.assertEquals(+0.08051, loader.getReceiversAntennas().get(2).getEccentricities(Frequency.G01).getZ(), 1.0e-15);
        Assertions.assertEquals(-0.00249,
                            loader.getReceiversAntennas().get(2).getPhaseCenterVariation(Frequency.G01,
                                                                                         new Vector3D(FastMath.toRadians(60.0),
                                                                                                      FastMath.toRadians(55.0))),
                            1.0e-15);

    }

    @Test
    public void testSmallAntex20File() throws URISyntaxException {

        final URL url = AntexLoaderTest.class.getClassLoader().getResource("antex/igs20-small.atx");
        AntexLoader  loader = new AntexLoader(new DataSource(url.toURI()),
                                              TimeScalesFactory.getGPS());
        Assertions.assertEquals(12, loader.getSatellitesAntennas().size());
        Assertions.assertEquals(3,  loader.getReceiversAntennas().size());
    }

    @Test
    public void testWrongColumns() {
        try {
            new AntexLoader("^igs14-wrong-columns\\.atx$");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.WRONG_COLUMNS_NUMBER, oe.getSpecifier());
            Assertions.assertEquals(25, ((Integer) oe.getParts()[1]).intValue());
            Assertions.assertEquals(17, ((Integer) oe.getParts()[2]).intValue());
            Assertions.assertEquals(10, ((Integer) oe.getParts()[3]).intValue());
        }
    }

    @Test
    public void testUnknownFrequency() {
        try {
            new AntexLoader("^igs14-unknown-rinex-frequency\\.atx$");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_RINEX_FREQUENCY, oe.getSpecifier());
            Assertions.assertEquals("U99", (String) oe.getParts()[0]);
            Assertions.assertEquals(23, ((Integer) oe.getParts()[2]).intValue());
        }
    }

    @Test
    public void testMismatchedFrequencies() {
        try {
            new AntexLoader("^igs14-mismatched-frequencies\\.atx$");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.MISMATCHED_FREQUENCIES, oe.getSpecifier());
            Assertions.assertEquals(88, ((Integer) oe.getParts()[1]).intValue());
            Assertions.assertEquals("E01", "" + oe.getParts()[2]);
            Assertions.assertEquals("E06", "" + oe.getParts()[3]);
        }
    }

    @Test
    public void testWrongLabel() {
        try {
            new AntexLoader("^igs14-unknown-label\\.atx$");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(17, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("THIS IS NOT AN ANTEX LABEL", ((String) oe.getParts()[2]).substring(60).trim());
        }
    }

    @Test
    /**
     * This test is related to issue-622.
     */
    public void testUnknownNumberFrequencies() {
        try {
            new AntexLoader("^igs14-unknown-nb-frequencies\\.atx$");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(21, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals("END OF FREQUENCY", ((String) oe.getParts()[2]).substring(60).trim());
        }
    }

    @Test
    public void testCorruptedFile() {
        try {
            new AntexLoader("^igs14-corrupted\\.atx$");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(21, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    private void checkSatellite(final TimeSpanMap<SatelliteAntenna> tsm,
                                final int startYear, final int startMonth, final int startDay,
                                final int endYear, final int endMonth, final int endDay,
                                final SatelliteSystem system, final String type,
                                final SatelliteType satType, final int satCode, final int prnNumber,
                                final String cosparId, final Frequency freq, final double az, final double pol,
                                final double phaseCenterVariation) {
        final double oneMilliSecond = 0.001;
        final AbsoluteDate startDate = new AbsoluteDate(startYear, startMonth, startDay,
                                                        TimeScalesFactory.getGPS());
        final AbsoluteDate endDate   = endYear > 9000 ?
                                       AbsoluteDate.FUTURE_INFINITY :
                                       new AbsoluteDate(endYear, endMonth, endDay,
                                                        23, 59, 59.9999999,
                                                        TimeScalesFactory.getGPS());
        final SatelliteAntenna antenna = tsm.get(startDate.shiftedBy(oneMilliSecond));
        Assertions.assertEquals(system,         antenna.getSatelliteSystem());
        Assertions.assertEquals(type,           antenna.getType());
        Assertions.assertEquals(satType,        antenna.getSatelliteType());
        Assertions.assertEquals(satCode,        antenna.getSatelliteCode());
        Assertions.assertEquals(prnNumber,      antenna.getPrnNumber());
        Assertions.assertEquals(cosparId,       antenna.getCosparID());
        Assertions.assertEquals(0.0,            startDate.durationFrom(antenna.getValidFrom()), 1.0e-10);
        if (endDate == AbsoluteDate.FUTURE_INFINITY) {
            Assertions.assertSame(endDate, antenna.getValidUntil());
        } else {
            Assertions.assertEquals(0.0,            endDate.durationFrom(antenna.getValidUntil()), 1.0e-10);
        }
        Assertions.assertEquals(phaseCenterVariation * 0.001,
                            antenna.getPhaseCenterVariation(freq,
                                                            new Vector3D(FastMath.toRadians(az),
                                                                         FastMath.toRadians(90 - pol))),
                            1.0e-10);
    }

}
