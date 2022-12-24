/* Copyright 2002-2022 CS GROUP
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
package org.orekit.gnss.navigation;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.data.BeidouNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.IRNSSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.SBASNavigationMessage;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import java.io.IOException;
import java.net.URISyntaxException;

public class NavigationFileParserTest {

    private static final Double SEC_TO_MILLI = 1000.0;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testGpsRinex301() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_GPS_Rinex301.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.01,                file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                 file.getFileType());
        Assertions.assertEquals(SatelliteSystem.GPS, file.getSatelliteSystem());
        Assertions.assertEquals("XXRINEXN V3",       file.getProgramName());
        Assertions.assertEquals("AIUB",              file.getAgencyName());
        Assertions.assertEquals("19990903",          file.getCreationDateString());
        Assertions.assertEquals("152236",            file.getCreationTimeString());
        Assertions.assertEquals("UTC",               file.getCreationTimeZoneString());
        Assertions.assertEquals(0.0, file.getCreationDate().durationFrom(new AbsoluteDate(1999, 9, 3, 15, 22, 36.0, TimeScalesFactory.getUTC())), 0.0);
        Assertions.assertEquals("GPS",               file.getIonosphericCorrectionType());
        Assertions.assertEquals(0.1676e-07,          file.getKlobucharAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(0.2235e-07,          file.getKlobucharAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(0.1192e-06,          file.getKlobucharAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(0.1192e-06,          file.getKlobucharAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals(0.1208e+06,          file.getKlobucharBeta()[0],  Double.MIN_VALUE);
        Assertions.assertEquals(0.1310e+06,          file.getKlobucharBeta()[1],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.1310e+06,         file.getKlobucharBeta()[2],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.1966e+06,         file.getKlobucharBeta()[3],  Double.MIN_VALUE);
        Assertions.assertEquals("GPUT", file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals(0.1331791282e-06,    file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionA0(), Double.MIN_VALUE);
        Assertions.assertEquals(0.107469589e-12,     file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionA1(), Double.MIN_VALUE);
        Assertions.assertEquals(552960,              file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionSecOfWeek());
        Assertions.assertEquals(1025,                file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionWeekNumber());
        Assertions.assertEquals("EXAMPLE OF VERSION 3.00 FORMAT", file.getComments());
        Assertions.assertEquals(13, file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(2, file.getGPSNavigationMessages().size());

        final GPSNavigationMessage gps = file.getGPSNavigationMessages("G13").get(0);
        Assertions.assertEquals(0.0, gps.getEpochToc().durationFrom(new AbsoluteDate(1999, 9, 2, 19, 0, 0, TimeScalesFactory.getGPS())), Double.MIN_VALUE);
        Assertions.assertEquals(0.490025617182e-03,  gps.getAf0(), 1.0e-15);
        Assertions.assertEquals(0.204636307899e-11,  gps.getAf1(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  gps.getAf2(), 1.0e-15);
        Assertions.assertEquals(133,                 gps.getIODE());
        Assertions.assertEquals(-0.963125000000e+02, gps.getCrs(), 1.0e-15);
        Assertions.assertEquals(0.292961152146e+01,  gps.getM0(), 1.0e-15);
        Assertions.assertEquals(-0.498816370964e-05, gps.getCuc(), 1.0e-15);
        Assertions.assertEquals(0.200239347760e-02,  gps.getE(), 1.0e-15);
        Assertions.assertEquals(0.928156077862e-05,  gps.getCus(), 1.0e-15);
        Assertions.assertEquals(0.515328476143e+04,  FastMath.sqrt(gps.getSma()), 1.0e-15);
        Assertions.assertEquals(0.414000000000e+06,  gps.getTime(), 1.0e-15);
        Assertions.assertEquals(-0.279396772385e-07, gps.getCic(), 1.0e-15);
        Assertions.assertEquals(0.243031939942e+01,  gps.getOmega0(), 1.0e-15);
        Assertions.assertEquals(-0.558793544769e-07, gps.getCis(), 1.0e-15);
        Assertions.assertEquals(0.110192796930e+01,  gps.getI0(), 1.0e-15);
        Assertions.assertEquals(0.271187500000e+03,  gps.getCrc(), 1.0e-15);
        Assertions.assertEquals(-0.232757915425e+01, gps.getPa(), 1.0e-15);
        Assertions.assertEquals(-0.619632953057e-08, gps.getOmegaDot(), 1.0e-15);
        Assertions.assertEquals(-0.785747015231e-11, gps.getIDot(), 1.0e-15);
        Assertions.assertEquals(1025,                gps.getWeek());
        Assertions.assertEquals(0.000000000000e+00,  gps.getSvAccuracy(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  gps.getSvHealth(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  gps.getTGD(), 1.0e-15);
        Assertions.assertEquals(389,                 gps.getIODC());

        // check weeks reference in Rinex navigation are aligned with GPS weeks
        final AbsoluteDate obsRebuiltDate = new GNSSDate(gps.getWeek(), SEC_TO_MILLI * gps.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(gps.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(gps.getDate()), 1.0e-15);

    }

    @Test
    public void testSBASRinex301() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_SBAS_Rinex301.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.01,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.SBAS,    file.getSatelliteSystem());
        Assertions.assertEquals("sbf2rin-9.3.3",         file.getProgramName());
        Assertions.assertEquals("",                      file.getAgencyName());
        Assertions.assertEquals("20150106",              file.getCreationDateString());
        Assertions.assertEquals("000809",                file.getCreationTimeString());
        Assertions.assertEquals("LCL",                   file.getCreationTimeZoneString());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(2, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSNavigationMessages().size());

        final SBASNavigationMessage sbas = file.getSBASNavigationMessages("S27").get(0);
        Assertions.assertEquals(0.0, sbas.getEpochToc().durationFrom(new AbsoluteDate(2015, 1, 4, 23, 58, 56.0, TimeScalesFactory.getGPS())), Double.MIN_VALUE);
        Assertions.assertEquals(2.980232238770E-08,  sbas.getAGf0(), 1.0e-10);
        Assertions.assertEquals(1.182343112305E-11,  sbas.getAGf1(), 1.0e-10);
        Assertions.assertEquals(8.631300000000E+04,  sbas.getTime(), 1.0e-10);
        Assertions.assertEquals(2420.415392000E+04,  sbas.getX(), 1.0e-10);
        Assertions.assertEquals(-3450.000000000E-04, sbas.getXDot(), 1.0e-10);
        Assertions.assertEquals(-3375.000000000E-07, sbas.getXDotDot(), 1.0e-10);
        Assertions.assertEquals(3.100000000000E+01,  sbas.getHealth(), 1.0e-10);
        Assertions.assertEquals(3453.707432000E+04,  sbas.getY(), 1.0e-10);
        Assertions.assertEquals(-2950.625000000E-03, sbas.getYDot(), 1.0e-10);
        Assertions.assertEquals(1750.000000000E-07,  sbas.getYDotDot(), 1.0e-10);
        Assertions.assertEquals(4.096000000000E+03,  sbas.getURA(), 1.0e-10);
        Assertions.assertEquals(-3269.960000000E+01, sbas.getZ(), 1.0e-10);
        Assertions.assertEquals(-2132.000000000E-03, sbas.getZDot(), 1.0e-10);
        Assertions.assertEquals(1875.000000000E-07,  sbas.getZDotDot(), 1.0e-10);
        Assertions.assertEquals(192,                 sbas.getIODN(), 1.0e-10);

    }

    @Test
    public void testBeidouRinex302() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_Beidou_Rinex302.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.02,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.BEIDOU,  file.getSatelliteSystem());
        Assertions.assertEquals("Converto v3.5.5",       file.getProgramName());
        Assertions.assertEquals("IGN",                   file.getAgencyName());
        Assertions.assertEquals("20210224",              file.getCreationDateString());
        Assertions.assertEquals("012052",                file.getCreationTimeString());
        Assertions.assertEquals("UTC",                   file.getCreationTimeZoneString());
        Assertions.assertEquals(4,                       file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(2, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSNavigationMessages().size());

        final BeidouNavigationMessage bdt = file.getBeidouNavigationMessages("C02").get(0);
        Assertions.assertEquals(0.0, bdt.getEpochToc().durationFrom(new AbsoluteDate(2021, 2, 22, 22, 0, 0, TimeScalesFactory.getBDT())), Double.MIN_VALUE);
        Assertions.assertEquals(4.916836041957e-04,  bdt.getAf0(), 1.0e-15);
        Assertions.assertEquals(-3.058442388237e-11, bdt.getAf1(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  bdt.getAf2(), 1.0e-15);
        Assertions.assertEquals(1,                   bdt.getAODE());
        Assertions.assertEquals(2.775156250000e+02,  bdt.getCrs(), 1.0e-15);
        Assertions.assertEquals(-2.539159755499e+00, bdt.getM0(), 1.0e-15);
        Assertions.assertEquals(9.234994649887e-06,  bdt.getCuc(), 1.0e-15);
        Assertions.assertEquals(9.814361110330e-04,  bdt.getE(), 1.0e-15);
        Assertions.assertEquals(9.856652468443e-06,  bdt.getCus(), 1.0e-15);
        Assertions.assertEquals(6.493364431381e+03,  FastMath.sqrt(bdt.getSma()), 1.0e-15);
        Assertions.assertEquals(1.656000000000e+05,  bdt.getTime(), 1.0e-15);
        Assertions.assertEquals(8.055940270424e-08,  bdt.getCic(), 1.0e-15);
        Assertions.assertEquals(2.930216013841e+00,  bdt.getOmega0(), 1.0e-15);
        Assertions.assertEquals(-1.355074346066e-07, bdt.getCis(), 1.0e-15);
        Assertions.assertEquals(6.617987281734e-02,  bdt.getI0(), 1.0e-15);
        Assertions.assertEquals(-2.970000000000e+02, bdt.getCrc(), 1.0e-15);
        Assertions.assertEquals(5.859907097566e-01,  bdt.getPa(), 1.0e-15);
        Assertions.assertEquals(4.416612541069e-09,  bdt.getOmegaDot(), 1.0e-15);
        Assertions.assertEquals(-4.628764235181e-10, bdt.getIDot(), 1.0e-15);
        Assertions.assertEquals(790,                 bdt.getWeek());
        Assertions.assertEquals(2.000000000000e+00,  bdt.getSvAccuracy(), 1.0e-15);
        Assertions.assertEquals(1.500000000000e-09,  bdt.getTGD1(), 1.0e-15);
        Assertions.assertEquals(-1.370000000000e-08, bdt.getTGD2(), 1.0e-15);

        // check weeks reference in Rinex navigation are aligned with Beidou weeks (not GPS weeks as other systems)
        final AbsoluteDate obsRebuiltDate = new GNSSDate(bdt.getWeek(), SEC_TO_MILLI * bdt.getTime(), SatelliteSystem.BEIDOU).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(bdt.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(bdt.getDate()), 1.0e-15);

    }

    @Test
    public void testGalileoRinex302() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Galileo_Rinex302.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.02,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.GALILEO, file.getSatelliteSystem());
        Assertions.assertEquals("sbf2rin-10.2.0",        file.getProgramName());
        Assertions.assertEquals("",                      file.getAgencyName());
        Assertions.assertEquals("20160428",              file.getCreationDateString());
        Assertions.assertEquals("003637",                file.getCreationTimeString());
        Assertions.assertEquals("LCL",                   file.getCreationTimeZoneString());
        Assertions.assertEquals("GAL",                   file.getIonosphericCorrectionType());
        Assertions.assertEquals(3.5500E+01,              file.getNeQuickAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(-2.3438E-02,             file.getNeQuickAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(1.6632E-02,              file.getNeQuickAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,              file.getNeQuickAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals("GPGA", file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals("GAUT", file.getTimeSystemCorrections().get(1).getTimeSystemCorrectionType());
        Assertions.assertEquals(-2.9103830457E-11,       file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionA0(), Double.MIN_VALUE);
        Assertions.assertEquals(-4.440892099E-16,        file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionA1(), Double.MIN_VALUE);
        Assertions.assertEquals(918000,                  file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionSecOfWeek());
        Assertions.assertEquals(1919,                    file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionWeekNumber());
        Assertions.assertEquals("", file.getComments());
        Assertions.assertEquals(17, file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(2, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSNavigationMessages().size());

        final GalileoNavigationMessage gal = file.getGalileoNavigationMessages("E08").get(3);
        Assertions.assertEquals(0.0, gal.getEpochToc().durationFrom(new AbsoluteDate(2016, 4, 26, 5, 50, 0, TimeScalesFactory.getGST())), Double.MIN_VALUE);
        Assertions.assertEquals(1.646681921557E-03,  gal.getAf0(), 1.0e-15);
        Assertions.assertEquals(3.988276375821E-10,  gal.getAf1(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  gal.getAf2(), 1.0e-15);
        Assertions.assertEquals(285,                 gal.getIODNav());
        Assertions.assertEquals(-1.550000000000E+01, gal.getCrs(), 1.0e-15);
        Assertions.assertEquals(-1.881713322719E+00, gal.getM0(), 1.0e-15);
        Assertions.assertEquals(-9.220093488693E-07, gal.getCuc(), 1.0e-15);
        Assertions.assertEquals(2.031255280599E-04,  gal.getE(), 1.0e-15);
        Assertions.assertEquals(8.771196007729E-06,  gal.getCus(), 1.0e-15);
        Assertions.assertEquals(5.440611787796E+03,  FastMath.sqrt(gal.getSma()), 1.0e-15);
        Assertions.assertEquals(1.938000000000E+05,  gal.getTime(), 1.0e-15);
        Assertions.assertEquals(7.450580596924E-09,  gal.getCic(), 1.0e-15);
        Assertions.assertEquals(-1.589621838359E-01, gal.getOmega0(), 1.0e-15);
        Assertions.assertEquals(5.401670932770E-08,  gal.getCis(), 1.0e-15);
        Assertions.assertEquals(9.594902351453E-01,  gal.getI0(), 1.0e-15);
        Assertions.assertEquals(1.494687500000E+02,  gal.getCrc(), 1.0e-15);
        Assertions.assertEquals(-1.602015041031E+00, gal.getPa(), 1.0e-15);
        Assertions.assertEquals(-5.460941755858E-09, gal.getOmegaDot(), 1.0e-15);
        Assertions.assertEquals(-6.350264514006E-10, gal.getIDot(), 1.0e-15);
        Assertions.assertEquals(1894,                gal.getWeek());
        Assertions.assertEquals(3.120000000000E+00,  gal.getSisa(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  gal.getSvHealth(), 1.0e-15);
        Assertions.assertEquals(-7.450580596924E-09, gal.getBGDE1E5a(), 1.0e-15);
        Assertions.assertEquals(0.000000000000E+00,  gal.getBGDE5bE1(), 1.0e-15);

        // check weeks reference in Rinex navigation are aligned with GPS weeks
        final AbsoluteDate obsRebuiltDate = new GNSSDate(gal.getWeek(), SEC_TO_MILLI * gal.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(gal.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(gal.getDate()), 1.0e-15);

    }

    @Test
    public void testQZSSRinex302() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_QZSS_Rinex302.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.02,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.QZSS,    file.getSatelliteSystem());
        Assertions.assertEquals("NetR9 5.45",            file.getProgramName());
        Assertions.assertEquals("Receiver Operator",     file.getAgencyName());
        Assertions.assertEquals("20200609",              file.getCreationDateString());
        Assertions.assertEquals("000000",                file.getCreationTimeString());
        Assertions.assertEquals("UTC",                   file.getCreationTimeZoneString());
        Assertions.assertEquals("QZS",                   file.getIonosphericCorrectionType());
        Assertions.assertEquals(0.5588e-08,              file.getKlobucharAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(0.7451e-08,              file.getKlobucharAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(-0.4768e-06,             file.getKlobucharAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(-0.1013e-05,             file.getKlobucharAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals(0.8602e+05,              file.getKlobucharBeta()[0],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.4096e+06,             file.getKlobucharBeta()[1],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.8389e+07,             file.getKlobucharBeta()[2],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.8389e+07,             file.getKlobucharBeta()[3],  Double.MIN_VALUE);
        Assertions.assertEquals("QZUT", file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals(0.0,                     file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionA0(), Double.MIN_VALUE);
        Assertions.assertEquals(0.0,                     file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionA1(), Double.MIN_VALUE);
        Assertions.assertEquals(356352,                  file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionSecOfWeek());
        Assertions.assertEquals(2109,                    file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionWeekNumber());
        Assertions.assertEquals("",                      file.getComments());
        Assertions.assertEquals(18,                      file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(3, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSNavigationMessages().size());

        final QZSSNavigationMessage qzs = file.getQZSSNavigationMessages("J07").get(0);
        Assertions.assertEquals(0.0, qzs.getEpochToc().durationFrom(new AbsoluteDate(2020, 6, 9, 0, 0, 0, TimeScalesFactory.getQZSS())), Double.MIN_VALUE);
        Assertions.assertEquals(-0.214204192162e-07, qzs.getAf0(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  qzs.getAf1(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  qzs.getAf2(), 1.0e-15);
        Assertions.assertEquals(189,                 qzs.getIODE());
        Assertions.assertEquals(-0.580312500000e+03, qzs.getCrs(), 1.0e-15);
        Assertions.assertEquals(-0.104204506497e+01, qzs.getM0(), 1.0e-15);
        Assertions.assertEquals(-0.190474092960e-04, qzs.getCuc(), 1.0e-15);
        Assertions.assertEquals(0.140047399327e-03,  qzs.getE(), 1.0e-15);
        Assertions.assertEquals(0.936537981033e-05,  qzs.getCus(), 1.0e-15);
        Assertions.assertEquals(0.649355915070e+04,  FastMath.sqrt(qzs.getSma()), 1.0e-15);
        Assertions.assertEquals(0.172800000000e+06,  qzs.getTime(), 1.0e-15);
        Assertions.assertEquals(-0.241957604885e-05, qzs.getCic(), 1.0e-15);
        Assertions.assertEquals(-0.102838327972e-01, qzs.getOmega0(), 1.0e-15);
        Assertions.assertEquals(0.251457095146e-06,  qzs.getCis(), 1.0e-15);
        Assertions.assertEquals(0.107314257498e-02,  qzs.getI0(), 1.0e-15);
        Assertions.assertEquals(-0.291156250000e+03, qzs.getCrc(), 1.0e-15);
        Assertions.assertEquals(-0.298090621453e+01, qzs.getPa(), 1.0e-15);
        Assertions.assertEquals(0.116790579082e-08,  qzs.getOmegaDot(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  qzs.getIDot(), 1.0e-15);
        Assertions.assertEquals(2109,                qzs.getWeek());
        Assertions.assertEquals(0.280000000000e+01,  qzs.getSvAccuracy(), 1.0e-15);
        Assertions.assertEquals(0.620000000000e+02,  qzs.getSvHealth(), 1.0e-15);
        Assertions.assertEquals(-0.605359673500e-08, qzs.getTGD(), 1.0e-15);
        Assertions.assertEquals(957,                 qzs.getIODC(), 1.0e-15);

        // check weeks reference in Rinex navigation are aligned with GPS weeks
        final AbsoluteDate obsRebuiltDate = new GNSSDate(qzs.getWeek(), SEC_TO_MILLI * qzs.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(qzs.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(qzs.getDate()), 1.0e-15);

    }

    @Test
    public void testGLONASSRinex303() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Glonass_Rinex303.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.03,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.GLONASS, file.getSatelliteSystem());
        Assertions.assertEquals("GR25 V4.30",            file.getProgramName());
        Assertions.assertEquals("Institute of Astrono",  file.getAgencyName());
        Assertions.assertEquals("20210217",              file.getCreationDateString());
        Assertions.assertEquals("235947",                file.getCreationTimeString());
        Assertions.assertEquals("UTC",                   file.getCreationTimeZoneString());
        Assertions.assertEquals("GLUT", file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals(6.0535967350e-09,        file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionA0(), Double.MIN_VALUE);
        Assertions.assertEquals(0.000000000e+00,         file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionA1(), Double.MIN_VALUE);
        Assertions.assertEquals(0,                       file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionSecOfWeek());
        Assertions.assertEquals(0,                       file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionWeekNumber());
        Assertions.assertEquals("",                      file.getComments());
        Assertions.assertEquals(18,                      file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(3, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSNavigationMessages().size());

        final GLONASSNavigationMessage glo = file.getGlonassNavigationMessages("R02").get(0);
        Assertions.assertEquals(0.0, glo.getEpochToc().durationFrom(new AbsoluteDate(2021, 2, 17, 23, 45, 0.0, TimeScalesFactory.getGPS())), Double.MIN_VALUE);
        Assertions.assertEquals(-4.674419760704e-04, glo.getTN(), 1.0e-10);
        Assertions.assertEquals(9.094947017729e-13,  glo.getGammaN(), 1.0e-10);
        Assertions.assertEquals(84600.0,             glo.getTime(), 1.0e-10);
        Assertions.assertEquals(-1252.090332031e+04, glo.getX(), 1.0e-10);
        Assertions.assertEquals(-2661.552429199e+00, glo.getXDot(), 1.0e-10);
        Assertions.assertEquals(0.000000000000e+00,  glo.getXDotDot(), 1.0e-10);
        Assertions.assertEquals(0.000000000000e+00,  glo.getHealth(), 1.0e-10);
        Assertions.assertEquals(1045.030761719e+04,  glo.getY(), 1.0e-10);
        Assertions.assertEquals(3342.580795288e-01,  glo.getYDot(), 1.0e-10);
        Assertions.assertEquals(0.000000000000e+00,  glo.getYDotDot(), 1.0e-10);
        Assertions.assertEquals(-4,                  glo.getFrequencyNumber());
        Assertions.assertEquals(1963.127978516e+04,  glo.getZ(), 1.0e-10);
        Assertions.assertEquals(-1884.816169739e+00, glo.getZDot(), 1.0e-10);
        Assertions.assertEquals(-1862.645149231e-09, glo.getZDotDot(), 1.0e-10);

    }

    @Test
    public void testIRNSSRinex303() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_IRNSS_Rinex303.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.03,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.IRNSS,   file.getSatelliteSystem());
        Assertions.assertEquals("JPS2RIN v.2.0.168",     file.getProgramName());
        Assertions.assertEquals("JAVAD GNSS",            file.getAgencyName());
        Assertions.assertEquals("20191028",              file.getCreationDateString());
        Assertions.assertEquals("005648",                file.getCreationTimeString());
        Assertions.assertEquals("UTC",                   file.getCreationTimeZoneString());
        Assertions.assertEquals(18,                      file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(3, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSNavigationMessages().size());

        final IRNSSNavigationMessage irnss = file.getIRNSSNavigationMessages("I05").get(0);
        Assertions.assertEquals(0.0, irnss.getEpochToc().durationFrom(new AbsoluteDate(2019, 10, 27, 0, 0, 0, TimeScalesFactory.getIRNSS())), Double.MIN_VALUE);
        Assertions.assertEquals(4.232432693243e-04,  irnss.getAf0(), 1.0e-15);
        Assertions.assertEquals(2.000888343900e-11,  irnss.getAf1(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  irnss.getAf2(), 1.0e-15);
        Assertions.assertEquals(0,                   irnss.getIODEC());
        Assertions.assertEquals(4.608125000000e+02,  irnss.getCrs(), 1.0e-15);
        Assertions.assertEquals(-2.259193667639e+00, irnss.getM0(), 1.0e-15);
        Assertions.assertEquals(1.492351293564e-05,  irnss.getCuc(), 1.0e-15);
        Assertions.assertEquals(2.073186333291e-03,  irnss.getE(), 1.0e-15);
        Assertions.assertEquals(-2.183392643929e-05, irnss.getCus(), 1.0e-15);
        Assertions.assertEquals(6.493289260864e+03,  FastMath.sqrt(irnss.getSma()), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  irnss.getTime(), 1.0e-15);
        Assertions.assertEquals(-2.868473529816e-07, irnss.getCic(), 1.0e-15);
        Assertions.assertEquals(1.135843714918e+00,  irnss.getOmega0(), 1.0e-15);
        Assertions.assertEquals(-5.215406417847e-08, irnss.getCis(), 1.0e-15);
        Assertions.assertEquals(5.007869522210e-01,  irnss.getI0(), 1.0e-15);
        Assertions.assertEquals(7.530000000000e+02,  irnss.getCrc(), 1.0e-15);
        Assertions.assertEquals(3.073412769875e+00,  irnss.getPa(), 1.0e-15);
        Assertions.assertEquals(-5.227360597694e-09, irnss.getOmegaDot(), 1.0e-15);
        Assertions.assertEquals(4.421612749348e-10,  irnss.getIDot(), 1.0e-15);
        Assertions.assertEquals(2077,                irnss.getWeek());
        Assertions.assertEquals(2.000000000000e+00,  irnss.getURA(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  irnss.getSvHealth(), 1.0e-15);
        Assertions.assertEquals(-4.656613000000e-10, irnss.getTGD(), 1.0e-15);

        // check weeks reference in Rinex navigation are aligned with GPS weeks
        final AbsoluteDate obsRebuiltDate = new GNSSDate(irnss.getWeek(), SEC_TO_MILLI * irnss.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(irnss.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(irnss.getDate()), 1.0e-15);

    }

    @Test
    public void testMixedRinex304() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Mixed_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,   file.getSatelliteSystem());
        Assertions.assertEquals("Alloy 5.37",            file.getProgramName());
        Assertions.assertEquals("Receiver Operator",     file.getAgencyName());
        Assertions.assertEquals("20200211",              file.getCreationDateString());
        Assertions.assertEquals("000000",                file.getCreationTimeString());
        Assertions.assertEquals("UTC",                   file.getCreationTimeZoneString());
        Assertions.assertEquals(0.8382E-08,              file.getKlobucharAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(-0.7451E-08,             file.getKlobucharAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(-0.5960E-07,             file.getKlobucharAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(0.5960E-07,              file.getKlobucharAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals(0.8806E+05,              file.getKlobucharBeta()[0],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.1638E+05,             file.getKlobucharBeta()[1],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.1966E+06,             file.getKlobucharBeta()[2],  Double.MIN_VALUE);
        Assertions.assertEquals(0.6554E+05,              file.getKlobucharBeta()[3],  Double.MIN_VALUE);
        Assertions.assertEquals(0.4200E+02,              file.getNeQuickAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(0.1563E-01,              file.getNeQuickAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(0.2045E-02,              file.getNeQuickAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,              file.getNeQuickAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals("GPUT", file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals("GAUT", file.getTimeSystemCorrections().get(1).getTimeSystemCorrectionType());
        Assertions.assertEquals("GPGA", file.getTimeSystemCorrections().get(2).getTimeSystemCorrectionType());
        Assertions.assertEquals(18,                      file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(2, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(1, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(2, file.getGPSNavigationMessages().size());

        final GLONASSNavigationMessage glo = file.getGlonassNavigationMessages("R05").get(0);
        Assertions.assertEquals(0.0, glo.getEpochToc().durationFrom(new AbsoluteDate(2020, 2, 10, 23, 45, 0.0, TimeScalesFactory.getGPS())), Double.MIN_VALUE);
        Assertions.assertEquals(-0.447863712907e-04, glo.getTN(), 1.0e-10);
        Assertions.assertEquals(0.909494701773e-12,  glo.getGammaN(), 1.0e-10);
        Assertions.assertEquals(86370.0,             glo.getTime(), 1.0e-10);
        Assertions.assertEquals(0182.817373047e+05,  glo.getX(), 1.0e-10);
        Assertions.assertEquals(-176.770305634e+01,  glo.getXDot(), 1.0e-10);
        Assertions.assertEquals(651.925802231e-08,   glo.getXDotDot(), 1.0e-10);
        Assertions.assertEquals(0.000000000000e+00,  glo.getHealth(), 1.0e-10);
        Assertions.assertEquals(0114.389570312e+05,  glo.getY(), 1.0e-10);
        Assertions.assertEquals(-619.493484497e+00,  glo.getYDot(), 1.0e-10);
        Assertions.assertEquals(279.396772385e-08,   glo.getYDotDot(), 1.0e-10);
        Assertions.assertEquals(1,                   glo.getFrequencyNumber());
        Assertions.assertEquals(136.489028320e+05,   glo.getZ(), 1.0e-10);
        Assertions.assertEquals(288.632869720e+01,   glo.getZDot(), 1.0e-10);
        Assertions.assertEquals(0.000000000000e+00,  glo.getZDotDot(), 1.0e-10);

    }

    @Test
    public void testQZSSRinex304() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_QZSS_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.QZSS,    file.getSatelliteSystem());
        Assertions.assertEquals("JPS2RIN v.2.0.191",     file.getProgramName());
        Assertions.assertEquals("JAVAD GNSS",            file.getAgencyName());
        Assertions.assertEquals("20200610",              file.getCreationDateString());
        Assertions.assertEquals("003246",                file.getCreationTimeString());
        Assertions.assertEquals("UTC",                   file.getCreationTimeZoneString());
        Assertions.assertEquals(18,                      file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(3, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSNavigationMessages().size());

        final QZSSNavigationMessage qzs = file.getQZSSNavigationMessages("J03").get(0);
        Assertions.assertEquals(0.0, qzs.getEpochToc().durationFrom(new AbsoluteDate(2020, 6, 9, 1, 0, 0, TimeScalesFactory.getQZSS())), Double.MIN_VALUE);
        Assertions.assertEquals(-3.880355507135e-06, qzs.getAf0(), 1.0e-15);
        Assertions.assertEquals(-4.547473508865e-13, qzs.getAf1(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  qzs.getAf2(), 1.0e-15);
        Assertions.assertEquals(193,                 qzs.getIODE());
        Assertions.assertEquals(3.106250000000e+02,  qzs.getCrs(), 1.0e-15);
        Assertions.assertEquals(2.226495657955e+00,  qzs.getM0(), 1.0e-15);
        Assertions.assertEquals(7.346272468567e-06,  qzs.getCuc(), 1.0e-15);
        Assertions.assertEquals(7.470769551583e-02,  qzs.getE(), 1.0e-15);
        Assertions.assertEquals(-2.568960189819e-05, qzs.getCus(), 1.0e-15);
        Assertions.assertEquals(6.493781688690e+03,  FastMath.sqrt(qzs.getSma()), 1.0e-15);
        Assertions.assertEquals(1.764000000000e+05,  qzs.getTime(), 1.0e-15);
        Assertions.assertEquals(-1.853331923485e-06, qzs.getCic(), 1.0e-15);
        Assertions.assertEquals(2.023599801546e+00,  qzs.getOmega0(), 1.0e-15);
        Assertions.assertEquals(1.644715666771e-06,  qzs.getCis(), 1.0e-15);
        Assertions.assertEquals(7.122509413449e-01,  qzs.getI0(), 1.0e-15);
        Assertions.assertEquals(9.670937500000e+02,  qzs.getCrc(), 1.0e-15);
        Assertions.assertEquals(-1.550179221884e+00, qzs.getPa(), 1.0e-15);
        Assertions.assertEquals(-1.478633019572e-09, qzs.getOmegaDot(), 1.0e-15);
        Assertions.assertEquals(-7.193156766709e-10, qzs.getIDot(), 1.0e-15);
        Assertions.assertEquals(2109,                qzs.getWeek());
        Assertions.assertEquals(2.000000000000e+00,  qzs.getSvAccuracy(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  qzs.getSvHealth(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  qzs.getTGD(), 1.0e-15);
        Assertions.assertEquals(961,                 qzs.getIODC(), 1.0e-15);

        // check weeks reference in Rinex navigation are aligned with GPS weeks
        final AbsoluteDate obsRebuiltDate = new GNSSDate(qzs.getWeek(), SEC_TO_MILLI * qzs.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(qzs.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(qzs.getDate()), 1.0e-15);

    }

    @Test
    public void testGpsRinex304() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_GPS_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                 file.getFileType());
        Assertions.assertEquals(SatelliteSystem.GPS, file.getSatelliteSystem());
        Assertions.assertEquals("sbf2rin-13.8.0",    file.getProgramName());
        Assertions.assertEquals("",                  file.getAgencyName());
        Assertions.assertEquals("20210307",          file.getCreationDateString());
        Assertions.assertEquals("000819",            file.getCreationTimeString());
        Assertions.assertEquals("UTC",               file.getCreationTimeZoneString());
        Assertions.assertEquals(0.0, file.getCreationDate().durationFrom(new AbsoluteDate(2021, 3, 7, 0, 8, 19.0, TimeScalesFactory.getUTC())), 0.0);
        Assertions.assertEquals("GPS",               file.getIonosphericCorrectionType());
        Assertions.assertEquals(1.0245E-08,          file.getKlobucharAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,          file.getKlobucharAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(-5.9605E-08,         file.getKlobucharAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,          file.getKlobucharAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals(9.0112E+04,          file.getKlobucharBeta()[0],  Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,          file.getKlobucharBeta()[1],  Double.MIN_VALUE);
        Assertions.assertEquals(-1.9661E+05,         file.getKlobucharBeta()[2],  Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,         file.getKlobucharBeta()[3],  Double.MIN_VALUE);
        Assertions.assertEquals("GPUT", file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals(0.0000000000E+00,    file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionA0(), Double.MIN_VALUE);
        Assertions.assertEquals(9.769962617E-15,     file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionA1(), Double.MIN_VALUE);
        Assertions.assertEquals(233472,              file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionSecOfWeek());
        Assertions.assertEquals(2148,                file.getTimeSystemCorrections().get(0).getTimeSystemCorrectionWeekNumber());
        Assertions.assertEquals(18,                  file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(3, file.getGPSNavigationMessages().size());

        final GPSNavigationMessage gps = file.getGPSNavigationMessages("G01").get(0);
        Assertions.assertEquals(0.0, gps.getEpochToc().durationFrom(new AbsoluteDate(2021, 3, 5, 23, 59, 44, TimeScalesFactory.getGPS())), Double.MIN_VALUE);
        Assertions.assertEquals(7.477793842554E-04,  gps.getAf0(), 1.0e-15);
        Assertions.assertEquals(-8.412825991400E-12, gps.getAf1(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  gps.getAf2(), 1.0e-15);
        Assertions.assertEquals(9,                   gps.getIODE());
        Assertions.assertEquals(-7.434375000000E+01, gps.getCrs(), 1.0e-15);
        Assertions.assertEquals(1.258707807055E+00,  gps.getM0(), 1.0e-15);
        Assertions.assertEquals(-3.753229975700E-06, gps.getCuc(), 1.0e-15);
        Assertions.assertEquals(1.047585485503E-02,  gps.getE(), 1.0e-15);
        Assertions.assertEquals(7.394701242447E-06,  gps.getCus(), 1.0e-15);
        Assertions.assertEquals(5.153690633774E+03,  FastMath.sqrt(gps.getSma()), 1.0e-15);
        Assertions.assertEquals(5.183840000000E+05,  gps.getTime(), 1.0e-15);
        Assertions.assertEquals(-1.359730958939E-07, gps.getCic(), 1.0e-15);
        Assertions.assertEquals(-1.936900950511E+00, gps.getOmega0(), 1.0e-15);
        Assertions.assertEquals(1.136213541031E-07,  gps.getCis(), 1.0e-15);
        Assertions.assertEquals(9.833041013284E-01,  gps.getI0(), 1.0e-15);
        Assertions.assertEquals(2.525937500000E+02,  gps.getCrc(), 1.0e-15);
        Assertions.assertEquals(8.208058952773E-01,  gps.getPa(), 1.0e-15);
        Assertions.assertEquals(-8.015691028563E-09, gps.getOmegaDot(), 1.0e-15);
        Assertions.assertEquals(-1.053615315878E-10, gps.getIDot(), 1.0e-15);
        Assertions.assertEquals(2147,                gps.getWeek());
        Assertions.assertEquals(2.000000000000E+00,  gps.getSvAccuracy(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  gps.getSvHealth(), 1.0e-15);
        Assertions.assertEquals(4.656612873077E-09,  gps.getTGD(), 1.0e-15);
        Assertions.assertEquals(9,                   gps.getIODC());

        // check weeks reference in Rinex navigation are aligned with GPS weeks
        final AbsoluteDate obsRebuiltDate = new GNSSDate(gps.getWeek(), SEC_TO_MILLI * gps.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(gps.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(gps.getDate()), 1.0e-15);

    }

    @Test
    public void testGalileoRinex304() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Galileo_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.GALILEO, file.getSatelliteSystem());
        Assertions.assertEquals("JPS2RIN v.2.0.191",     file.getProgramName());
        Assertions.assertEquals("JAVAD GNSS",            file.getAgencyName());
        Assertions.assertEquals("20210307",              file.getCreationDateString());
        Assertions.assertEquals("000245",                file.getCreationTimeString());
        Assertions.assertEquals("UTC",                   file.getCreationTimeZoneString());
        Assertions.assertEquals("GAL",                   file.getIonosphericCorrectionType());
        Assertions.assertEquals(5.0500E+01,              file.getNeQuickAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(2.7344E-02,              file.getNeQuickAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(-1.5869E-03,             file.getNeQuickAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,              file.getNeQuickAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals(18,                      file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(1, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSNavigationMessages().size());

        final GalileoNavigationMessage gal = file.getGalileoNavigationMessages("E13").get(1);
        Assertions.assertEquals(0.0, gal.getEpochToc().durationFrom(new AbsoluteDate(2021, 3, 5, 22, 30, 0, TimeScalesFactory.getGST())), Double.MIN_VALUE);
        Assertions.assertEquals(4.131024470553e-04,  gal.getAf0(), 1.0e-15);
        Assertions.assertEquals(5.400124791777e-13,  gal.getAf1(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  gal.getAf2(), 1.0e-15);
        Assertions.assertEquals(87,                  gal.getIODNav());
        Assertions.assertEquals(-1.010000000000e+02, gal.getCrs(), 1.0e-15);
        Assertions.assertEquals(1.781709410229e+00,  gal.getM0(), 1.0e-15);
        Assertions.assertEquals(-4.542991518974e-06, gal.getCuc(), 1.0e-15);
        Assertions.assertEquals(3.459260333329e-04,  gal.getE(), 1.0e-15);
        Assertions.assertEquals(5.345791578293e-06,  gal.getCus(), 1.0e-15);
        Assertions.assertEquals(5.440610326767e+03,  FastMath.sqrt(gal.getSma()), 1.0e-15);
        Assertions.assertEquals(5.130000000000e+05,  gal.getTime(), 1.0e-15);
        Assertions.assertEquals(6.332993507385e-08,  gal.getCic(), 1.0e-15);
        Assertions.assertEquals(-2.165492556291e+00, gal.getOmega0(), 1.0e-15);
        Assertions.assertEquals(-4.842877388000e-08, gal.getCis(), 1.0e-15);
        Assertions.assertEquals(9.941388485934e-01,  gal.getI0(), 1.0e-15);
        Assertions.assertEquals(2.392812500000e+02,  gal.getCrc(), 1.0e-15);
        Assertions.assertEquals(-9.613560467153e-01, gal.getPa(), 1.0e-15);
        Assertions.assertEquals(-5.551302662610e-09, gal.getOmegaDot(), 1.0e-15);
        Assertions.assertEquals(-8.321775206769e-11, gal.getIDot(), 1.0e-15);
        Assertions.assertEquals(2147,                gal.getWeek());
        Assertions.assertEquals(3.119999885559e+00,  gal.getSisa(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  gal.getSvHealth(), 1.0e-15);
        Assertions.assertEquals(4.656612873077e-10,  gal.getBGDE1E5a(), 1.0e-15);
        Assertions.assertEquals(2.328306436539e-10,  gal.getBGDE5bE1(), 1.0e-15);

        // check weeks reference in Rinex navigation are aligned with GPS weeks
        final AbsoluteDate obsRebuiltDate = new GNSSDate(gal.getWeek(), SEC_TO_MILLI * gal.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(gal.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(gal.getDate()), 1.0e-15);

    }

    @Test
    public void testSBASRinex304() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_SBAS_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.SBAS,    file.getSatelliteSystem());
        Assertions.assertEquals("sbf2rin-13.4.5",        file.getProgramName());
        Assertions.assertEquals("RIGTC, GO PECNY",       file.getAgencyName());
        Assertions.assertEquals("20210219",              file.getCreationDateString());
        Assertions.assertEquals("002627",                file.getCreationTimeString());
        Assertions.assertEquals("SBAS NAVIGATION DATA FROM STATION GOP6 (RIGTC, GO PECNY)", file.getComments());
        Assertions.assertEquals("UTC",                   file.getCreationTimeZoneString());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(3, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSNavigationMessages().size());

        final SBASNavigationMessage sbas = file.getSBASNavigationMessages("S36").get(0);
        Assertions.assertEquals(0.0, sbas.getEpochToc().durationFrom(new AbsoluteDate(2021, 2, 17, 23, 58, 56.0, TimeScalesFactory.getGPS())), Double.MIN_VALUE);
        Assertions.assertEquals(0.000000000000E+00, sbas.getAGf0(), 1.0e-10);
        Assertions.assertEquals(0.000000000000E+00, sbas.getAGf1(), 1.0e-10);
        Assertions.assertEquals(3.456150000000E+05, sbas.getTime(), 1.0e-10);
        Assertions.assertEquals(4200.368800000E+04, sbas.getX(), 1.0e-10);
        Assertions.assertEquals(0.000000000000E+00, sbas.getXDot(), 1.0e-10);
        Assertions.assertEquals(0.000000000000E+00, sbas.getXDotDot(), 1.0e-10);
        Assertions.assertEquals(6.300000000000E+01, sbas.getHealth(), 1.0e-10);
        Assertions.assertEquals(3674.846960000E+03, sbas.getY(), 1.0e-10);
        Assertions.assertEquals(0.000000000000E+00, sbas.getYDot(), 1.0e-10);
        Assertions.assertEquals(0.000000000000E+00, sbas.getYDotDot(), 1.0e-10);
        Assertions.assertEquals(3.276700000000E+04, sbas.getURA(), 1.0e-10);
        Assertions.assertEquals(0.000000000000E+00, sbas.getZ(), 1.0e-10);
        Assertions.assertEquals(0.000000000000E+00, sbas.getZDot(), 1.0e-10);
        Assertions.assertEquals(0.000000000000E+00, sbas.getZDotDot(), 1.0e-10);
        Assertions.assertEquals(155,                sbas.getIODN(), 1.0e-10);

    }


    @Test
    public void testIRNSSRinex304() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_IRNSS_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.IRNSS,   file.getSatelliteSystem());
        Assertions.assertEquals("JPS2RIN v.2.0.191",     file.getProgramName());
        Assertions.assertEquals("JAVAD GNSS",            file.getAgencyName());
        Assertions.assertEquals("20210308",              file.getCreationDateString());
        Assertions.assertEquals("000304",                file.getCreationTimeString());
        Assertions.assertEquals("UTC",                   file.getCreationTimeZoneString());
        Assertions.assertEquals(18,                      file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(2, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSNavigationMessages().size());

        final IRNSSNavigationMessage irnss = file.getIRNSSNavigationMessages("I05").get(0);
        Assertions.assertEquals(0.0, irnss.getEpochToc().durationFrom(new AbsoluteDate(2021, 3, 7, 0, 0, 0, TimeScalesFactory.getIRNSS())), Double.MIN_VALUE);
        Assertions.assertEquals(6.514852866530e-04,  irnss.getAf0(), 1.0e-15);
        Assertions.assertEquals(-7.560174708487e-11, irnss.getAf1(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  irnss.getAf2(), 1.0e-15);
        Assertions.assertEquals(0,                   irnss.getIODEC());
        Assertions.assertEquals(-3.893125000000e+02, irnss.getCrs(), 1.0e-15);
        Assertions.assertEquals(-7.075087446362e-02, irnss.getM0(), 1.0e-15);
        Assertions.assertEquals(-1.282989978790e-05, irnss.getCuc(), 1.0e-15);
        Assertions.assertEquals(1.970665412955e-03,  irnss.getE(), 1.0e-15);
        Assertions.assertEquals(1.581013202667e-05,  irnss.getCus(), 1.0e-15);
        Assertions.assertEquals(6.493357162476e+03,  FastMath.sqrt(irnss.getSma()), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  irnss.getTime(), 1.0e-15);
        Assertions.assertEquals(-7.078051567078e-08, irnss.getCic(), 1.0e-15);
        Assertions.assertEquals(-1.270986014126e+00, irnss.getOmega0(), 1.0e-15);
        Assertions.assertEquals(2.160668373108e-07,  irnss.getCis(), 1.0e-15);
        Assertions.assertEquals(5.051932936599e-01,  irnss.getI0(), 1.0e-15);
        Assertions.assertEquals(-4.082500000000e+02, irnss.getCrc(), 1.0e-15);
        Assertions.assertEquals(-2.990028662993e+00, irnss.getPa(), 1.0e-15);
        Assertions.assertEquals(-2.734399613005e-09, irnss.getOmegaDot(), 1.0e-15);
        Assertions.assertEquals(6.389551864768e-10,  irnss.getIDot(), 1.0e-15);
        Assertions.assertEquals(2148,                irnss.getWeek());
        Assertions.assertEquals(4.000000000000e+00,  irnss.getURA(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  irnss.getSvHealth(), 1.0e-15);
        Assertions.assertEquals(-4.656613000000e-10, irnss.getTGD(), 1.0e-15);

        // check weeks reference in Rinex navigation are aligned with GPS weeks
        final AbsoluteDate obsRebuiltDate = new GNSSDate(irnss.getWeek(), SEC_TO_MILLI * irnss.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(irnss.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(irnss.getDate()), 1.0e-15);

    }

    @Test
    public void testBeidouRinex304() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_Beidou_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                    file.getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals("N",                     file.getFileType());
        Assertions.assertEquals(SatelliteSystem.BEIDOU,  file.getSatelliteSystem());
        Assertions.assertEquals("JPS2RIN v.2.0.191",     file.getProgramName());
        Assertions.assertEquals("JAVAD GNSS",            file.getAgencyName());
        Assertions.assertEquals("20210224",              file.getCreationDateString());
        Assertions.assertEquals("000715",                file.getCreationTimeString());
        Assertions.assertEquals("UTC",                   file.getCreationTimeZoneString());
        Assertions.assertEquals(18,                      file.getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSNavigationMessages().size());
        Assertions.assertEquals(1, file.getBeidouNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSNavigationMessages().size());

        final BeidouNavigationMessage bdt = file.getBeidouNavigationMessages("C19").get(0);
        Assertions.assertEquals(0.0, bdt.getEpochToc().durationFrom(new AbsoluteDate(2021, 2, 23, 0, 0, 0, TimeScalesFactory.getBDT())), Double.MIN_VALUE);
        Assertions.assertEquals(7.378066657111e-04,  bdt.getAf0(), 1.0e-15);
        Assertions.assertEquals(1.382893799473e-11,  bdt.getAf1(), 1.0e-15);
        Assertions.assertEquals(0.000000000000e+00,  bdt.getAf2(), 1.0e-15);
        Assertions.assertEquals(1,                   bdt.getAODE());
        Assertions.assertEquals(0,                   bdt.getAODC());
        Assertions.assertEquals(-7.420312500000e+01, bdt.getCrs(), 1.0e-15);
        Assertions.assertEquals(-2.379681558032e-01, bdt.getM0(), 1.0e-15);
        Assertions.assertEquals(-3.555789589882e-06, bdt.getCuc(), 1.0e-15);
        Assertions.assertEquals(8.384847315028e-04,  bdt.getE(), 1.0e-15);
        Assertions.assertEquals(1.072138547897e-05,  bdt.getCus(), 1.0e-15);
        Assertions.assertEquals(5.282626970291e+03,  FastMath.sqrt(bdt.getSma()), 1.0e-15);
        Assertions.assertEquals(1.728000000000e+05,  bdt.getTime(), 1.0e-15);
        Assertions.assertEquals(-2.607703208923e-08, bdt.getCic(), 1.0e-15);
        Assertions.assertEquals(-4.071039898353e-01, bdt.getOmega0(), 1.0e-15);
        Assertions.assertEquals(-6.519258022308e-09, bdt.getCis(), 1.0e-15);
        Assertions.assertEquals(9.657351895813e-01,  bdt.getI0(), 1.0e-15);
        Assertions.assertEquals(1.491093750000e+02,  bdt.getCrc(), 1.0e-15);
        Assertions.assertEquals(-1.225716188251e+00, bdt.getPa(), 1.0e-15);
        Assertions.assertEquals(-6.454554572392e-09, bdt.getOmegaDot(), 1.0e-15);
        Assertions.assertEquals(2.217949529358e-10,  bdt.getIDot(), 1.0e-15);
        Assertions.assertEquals(790,                 bdt.getWeek());
        Assertions.assertEquals(2.000000000000e+00,  bdt.getSvAccuracy(), 1.0e-15);
        Assertions.assertEquals(1.220000000000e-08,  bdt.getTGD1(), 1.0e-15);
        Assertions.assertEquals(1.220000000000e-08,  bdt.getTGD2(), 1.0e-15);

        // check weeks reference in Rinex navigation are aligned with Beidou weeks (not GPS weeks as other systems)
        final AbsoluteDate obsRebuiltDate = new GNSSDate(bdt.getWeek(), SEC_TO_MILLI * bdt.getTime(), SatelliteSystem.BEIDOU).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(bdt.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);

    }

    @Test
    public void testUnknownHeaderKey() throws IOException {
        try {
            final String ex = "/gnss/navigation/unknown-key-header.n";
            new RinexNavigationParser().
                            parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals(4,  oe.getParts()[0]);
        }
    }

    @Test
    public void testUnknownRinexVersion() throws IOException {
        try {
            final String ex = "/gnss/navigation/unknown-rinex-version.n";
            new RinexNavigationParser().
                            parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NAVIGATION_FILE_UNSUPPORTED_VERSION,
                                oe.getSpecifier());
            Assertions.assertEquals(9.99,  oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongFormat() throws IOException {
        try {
            final String ex = "/gnss/navigation/wrong-format.n";
            new RinexNavigationParser().
                            parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals(4,  oe.getParts()[0]);
        }
    }

}
