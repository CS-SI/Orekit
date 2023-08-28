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
package org.orekit.files.rinex.navigation;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.data.TruncatingFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.utils.RinexFileType;
import org.orekit.frames.Frames;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.propagation.analytical.gnss.SBASPropagator;
import org.orekit.propagation.analytical.gnss.data.BeidouCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouSatelliteType;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GNSSConstants;
import org.orekit.propagation.analytical.gnss.data.GPSCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.IRNSSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.SBASNavigationMessage;
import org.orekit.propagation.numerical.GLONASSNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.units.Unit;

public class NavigationFileParserTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testWorkAroundWrongFormatNumber() throws IOException {
        // the test file tells it is in 3.05 format, but in fact
        // its GLONASS navigation messages are in 3.04 format
        // so there the 4th broadcast line expected in 3.05 is missing here
        // such a file has really been found in the wild
        final String ex = "/gnss/navigation/invalid-but-accepted.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
        Assertions.assertEquals(3.05, file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(1, file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(1, file.getGPSLegacyNavigationMessages().get("G32").size());
        Assertions.assertEquals(1, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(2, file.getGlonassNavigationMessages().get("R01").size());
    }

    @Test
    public void testGpsRinex301Truncated() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_GPS_Rinex301.n";
        try {
            new RinexNavigationParser().
                        parse(new TruncatingFilter(5).
                              filter(new DataSource(ex, () -> getClass().getResourceAsStream(ex))));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNEXPECTED_END_OF_FILE, oe.getSpecifier());
        }
    }

    @Test
    public void testGpsRinex301() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_GPS_Rinex301.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.01,                          file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,      file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.GPS,           file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("XXRINEXN V3",                 file.getHeader().getProgramName());
        Assertions.assertEquals("AIUB",                        file.getHeader().getRunByName());
        Assertions.assertEquals("1999-09-03T15:22:36.0",       file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                         file.getHeader().getCreationTimeZone());
        Assertions.assertEquals(0.0,                           file.getHeader().getCreationDate().durationFrom(new AbsoluteDate(1999, 9, 3, 15, 22, 36.0, TimeScalesFactory.getUTC())), 0.0);
        Assertions.assertEquals(IonosphericCorrectionType.GPS, file.getHeader().getIonosphericCorrectionType());
        Assertions.assertEquals(0.1676e-07,                    file.getKlobucharAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(0.2235e-07,                    file.getKlobucharAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(0.1192e-06,                    file.getKlobucharAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(0.1192e-06,                    file.getKlobucharAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals(0.1208e+06,                    file.getKlobucharBeta()[0],  Double.MIN_VALUE);
        Assertions.assertEquals(0.1310e+06,                    file.getKlobucharBeta()[1],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.1310e+06,                   file.getKlobucharBeta()[2],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.1966e+06,                   file.getKlobucharBeta()[3],  Double.MIN_VALUE);
        Assertions.assertEquals("GPUT",                        file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals(0.1331791282e-06,              file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionA0(), Double.MIN_VALUE);
        Assertions.assertEquals(0.107469589e-12,               file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionA1(), Double.MIN_VALUE);
        GNSSDate date = new GNSSDate(file.getHeader().getTimeSystemCorrections().get(0).getReferenceDate(), SatelliteSystem.GPS);
        Assertions.assertEquals(552960,                        date.getSecondsInWeek());
        Assertions.assertEquals(1025,                          date.getWeekNumber());
        Assertions.assertEquals("EXAMPLE OF VERSION 3.00 FORMAT", file.getComments().get(0).getText());
        Assertions.assertEquals(13, file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(2, file.getGPSLegacyNavigationMessages().size());

        final GPSLegacyNavigationMessage gps = file.getGPSLegacyNavigationMessages("G13").get(0);
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
        final AbsoluteDate obsRebuiltDate = new GNSSDate(gps.getWeek(), gps.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(gps.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(gps.getDate()), 1.0e-15);
        
        // check the propagator
        final GNSSPropagator propagator = gps.getPropagator();
        final AbsoluteDate date0 = gps.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = gps.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();
        Assertions.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    public void testGpsRinex400() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_GPS_Rinex400.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(4.00,                                 file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,             file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,                file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("Manual",                             file.getHeader().getProgramName());
        Assertions.assertEquals("Orekit",                             file.getHeader().getRunByName());
        Assertions.assertEquals("https://doi.org/10.xxxx",            file.getHeader().getDoi());
        Assertions.assertEquals("Apache V2",                          file.getHeader().getLicense());
        Assertions.assertEquals("not really a station",               file.getHeader().getStationInformation());
        Assertions.assertEquals(18,                                   file.getHeader().getNumberOfLeapSeconds());
        Assertions.assertEquals(102,                                  file.getHeader().getMergedFiles());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(1, file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(2, file.getGPSCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getSystemTimeOffsets().size());
        Assertions.assertEquals(0, file.getEarthOrientationParameters().size());
        Assertions.assertEquals(0, file.getKlobucharMessages().size());
        Assertions.assertEquals(0, file.getNequickGMessages().size());
        Assertions.assertEquals(0, file.getBDGIMMessages().size());

        final GPSLegacyNavigationMessage gpsL = file.getGPSLegacyNavigationMessages("G01").get(0);
        Assertions.assertEquals(0.0, gpsL.getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 0, 0, 0, TimeScalesFactory.getGPS())), Double.MIN_VALUE);
        Assertions.assertEquals(0, gpsL.getSvHealth());
        Assertions.assertEquals(4, gpsL.getFitInterval());

        final List<GPSCivilianNavigationMessage> list = file.getGPSCivilianNavigationMessages("G01");
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(0.0, list.get(0).getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 1, 30, 0, TimeScalesFactory.getGPS())), Double.MIN_VALUE);
        Assertions.assertEquals(-6, list.get(0).getUraiNed0());
        Assertions.assertEquals( 2, list.get(0).getUraiNed1());
        Assertions.assertEquals( 7, list.get(0).getUraiNed2());
        Assertions.assertEquals(-1, list.get(0).getUraiEd());
        Assertions.assertEquals(-3.492459654808e-10, list.get(0).getIscL1CA(), 1.0e-20);
        Assertions.assertEquals(-2.823071554303e-09, list.get(0).getIscL2C(),  1.0e-20);
        Assertions.assertEquals(6.810296326876e-09,  list.get(0).getIscL5I5(), 1.0e-20);
        Assertions.assertEquals(6.897607818246e-09,  list.get(0).getIscL5Q5(), 1.0e-20);
        Assertions.assertEquals(259206.0, list.get(0).getTransmissionTime(), 1.0e-10);
        Assertions.assertEquals(0.0, list.get(1).getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 3, 30, 0, TimeScalesFactory.getGPS())), Double.MIN_VALUE);

    }

    @Test
    public void testSBASRinex301() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_SBAS_Rinex301.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.01,                     file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION, file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.SBAS,     file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("sbf2rin-9.3.3",          file.getHeader().getProgramName());
        Assertions.assertEquals("",                       file.getHeader().getRunByName());
        Assertions.assertEquals("2015-01-06T00:08:09.0",  file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("LCL",                    file.getHeader().getCreationTimeZone());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(2, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSCivilianNavigationMessages().size());

        final SBASNavigationMessage sbas = file.getSBASNavigationMessages("S27").get(0);
        // BEWARE! in Rinex 3.01, the time scale for SBAS navigation is UTC
        Assertions.assertEquals(0.0, sbas.getEpochToc().durationFrom(new AbsoluteDate(2015, 1, 4, 23, 58, 56.0, TimeScalesFactory.getUTC())), Double.MIN_VALUE);
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

        // check the propagator
        final SBASPropagator propagator = sbas.getPropagator();
        final PVCoordinates pv = propagator.propagateInEcef(sbas.getDate());
        final Vector3D position = pv.getPosition();
        final Vector3D velocity = pv.getVelocity();
        final Vector3D acceleration = pv.getAcceleration();
        double eps = 1.0e-15;
        Assertions.assertEquals(sbas.getX(),       position.getX(),     eps);
        Assertions.assertEquals(sbas.getY(),       position.getY(),     eps);
        Assertions.assertEquals(sbas.getZ(),       position.getZ(),     eps);
        Assertions.assertEquals(sbas.getXDot(),    velocity.getX(),     eps);
        Assertions.assertEquals(sbas.getYDot(),    velocity.getY(),     eps);
        Assertions.assertEquals(sbas.getZDot(),    velocity.getZ(),     eps);
        Assertions.assertEquals(sbas.getXDotDot(), acceleration.getX(), eps);
        Assertions.assertEquals(sbas.getYDotDot(), acceleration.getY(), eps);
        Assertions.assertEquals(sbas.getZDotDot(), acceleration.getZ(), eps);

    }

    @Test
    public void testBeidouRinex302() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_Beidou_Rinex302.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.02,                     file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION, file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.BEIDOU,   file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("Converto v3.5.5",        file.getHeader().getProgramName());
        Assertions.assertEquals("IGN",                    file.getHeader().getRunByName());
        Assertions.assertEquals("2021-02-24T01:20:52.0",  file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                    file.getHeader().getCreationTimeZone());
        Assertions.assertEquals(4,                        file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(2, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());

        final BeidouLegacyNavigationMessage bdt = file.getBeidouLegacyNavigationMessages("C02").get(0);
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
        final AbsoluteDate obsRebuiltDate = new GNSSDate(bdt.getWeek(), bdt.getTime(), SatelliteSystem.BEIDOU).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(bdt.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(bdt.getDate()), 1.0e-15);

        // check the propagator
        final GNSSPropagator propagator = bdt.getPropagator(DataContext.getDefault().getFrames());
        final AbsoluteDate date0 = bdt.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = bdt.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();
        Assertions.assertEquals(0., p0.distance(p1), 0.);

    }

    @Test
    public void testBeidouRinex400() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Beidou_Rinex400.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(4.00,                                 file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,             file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,                file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("BCEmerge",                           file.getHeader().getProgramName());
        Assertions.assertEquals("congo",                              file.getHeader().getRunByName());
        Assertions.assertEquals("https://doi.org/10.57677/BRD400DLR", file.getHeader().getDoi());
        Assertions.assertNull(file.getHeader().getLicense());
        Assertions.assertNull(file.getHeader().getStationInformation());
        Assertions.assertEquals(18,                                   file.getHeader().getNumberOfLeapSeconds());
        Assertions.assertEquals(102,                                  file.getHeader().getMergedFiles());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(2, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(1, file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSCivilianNavigationMessages().size());

        final BeidouLegacyNavigationMessage bdtL = file.getBeidouLegacyNavigationMessages().get("C06").get(0);
        Assertions.assertEquals(0.0, bdtL.getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 0, 0, 0, TimeScalesFactory.getBDT())), Double.MIN_VALUE);

        final List<BeidouCivilianNavigationMessage> list = file.getBeidouCivilianNavigationMessages("C19");
        Assertions.assertEquals(6, list.size());
        Assertions.assertEquals(0.0, list.get(0).getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 0, 0, 0, TimeScalesFactory.getBDT())), Double.MIN_VALUE);
        Assertions.assertEquals(Frequency.B1C, list.get(0).getSignal());
        Assertions.assertEquals(-1.139640808105e-02, list.get(0).getADot(),       1.0e-15);
        Assertions.assertEquals(-1.300156250000e+02, list.get(0).getCrs(),        1.0e-15);
        Assertions.assertEquals(3.453536710809e-09,  list.get(0).getDeltaN(),     1.0e-15);
        Assertions.assertEquals(-8.439895553698e-01, list.get(0).getM0(),         1.0e-15);
        Assertions.assertEquals(-6.432645022869e-06, list.get(0).getCuc(),        1.0e-15);
        Assertions.assertEquals(5.305649829097e-04,  list.get(0).getE(),          1.0e-15);
        Assertions.assertEquals(8.089467883110e-06,  list.get(0).getCus(),        1.0e-15);
        Assertions.assertEquals(5.282638737345e+03,  list.get(0).getSqrtA(),      1.0e-15);
        Assertions.assertEquals(2.592000000000e+05,  list.get(0).getTime(),       1.0e-15);
        Assertions.assertEquals(-4.377216100693e-08, list.get(0).getCic(),        1.0e-15);
        Assertions.assertEquals(1.698788226948e+00,  list.get(0).getOmega0(),     1.0e-15);
        Assertions.assertEquals(-1.303851604462e-08, list.get(0).getCis(),        1.0e-15);
        Assertions.assertEquals(9.703601465722e-01,  list.get(0).getI0(),         1.0e-15);
        Assertions.assertEquals(2.000742187500e+02,  list.get(0).getCrc(),        1.0e-15);
        Assertions.assertEquals(-1.021547253715e+00, list.get(0).getPa(),         1.0e-15);
        Assertions.assertEquals(-6.782425372384e-09, list.get(0).getOmegaDot(),   1.0e-15);
        Assertions.assertEquals(-8.911085468192e-11, list.get(0).getIDot(),       1.0e-15);
        Assertions.assertEquals(9.367106834964e-14,  list.get(0).getDeltaN0Dot(), 1.0e-15);
        Assertions.assertEquals(BeidouSatelliteType.MEO, list.get(0).getSatelliteType());
        Assertions.assertEquals(2.592000000000e+05,  list.get(0).getTime(),       1.0e-15);
        Assertions.assertEquals( 0, list.get(0).getSisaiOe());
        Assertions.assertEquals(-5, list.get(0).getSisaiOcb());
        Assertions.assertEquals(-1, list.get(0).getSisaiOc1());
        Assertions.assertEquals(-1, list.get(0).getSisaiOc2());
        Assertions.assertEquals(-8.731149137020e-10, list.get(0).getIscB1CD(),    1.0e-15);
        Assertions.assertEquals(0.0,                 list.get(0).getIscB2AD(),    1.0e-15);
        Assertions.assertEquals(9.487848728895e-09,  list.get(0).getTgdB1Cp(),    1.0e-15);
        Assertions.assertEquals(-5.820766091347e-09, list.get(0).getTgdB2ap(),    1.0e-15);
        Assertions.assertEquals(-1, list.get(0).getSismai());
        Assertions.assertEquals(0, list.get(0).getHealth());
        Assertions.assertEquals(0, list.get(0).getIntegrityFlags());
        Assertions.assertEquals(16, list.get(0).getIODC());
        Assertions.assertEquals(259200.0, list.get(0).getTransmissionTime(), 1.0e-10);
        Assertions.assertEquals(16, list.get(0).getIODE());

        Assertions.assertEquals(0.0, list.get(1).getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 1, 0, 0, TimeScalesFactory.getBDT())), Double.MIN_VALUE);
        Assertions.assertEquals(Frequency.B1C, list.get(1).getSignal());
        Assertions.assertEquals(0.0, list.get(2).getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 0, 0, 0, TimeScalesFactory.getBDT())), Double.MIN_VALUE);
        Assertions.assertEquals(Frequency.B2A, list.get(2).getSignal());
        Assertions.assertEquals(0.0,                 list.get(2).getIscB1CD(),    1.0e-15);
        Assertions.assertEquals(-2.735760062933e-09, list.get(2).getIscB2AD(),    1.0e-15);
        Assertions.assertEquals(0.0, list.get(3).getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 1, 0, 0, TimeScalesFactory.getBDT())), Double.MIN_VALUE);
        Assertions.assertEquals(Frequency.B2A, list.get(3).getSignal());
        Assertions.assertEquals(0.0, list.get(4).getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 0, 0, 0, TimeScalesFactory.getBDT())), Double.MIN_VALUE);
        Assertions.assertEquals(Frequency.B2B, list.get(4).getSignal());
        Assertions.assertEquals(0.0, list.get(5).getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 1, 0, 0, TimeScalesFactory.getBDT())), Double.MIN_VALUE);
        Assertions.assertEquals(Frequency.B2B, list.get(5).getSignal());

    }

    @Test
    public void testGalileoRinex302() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Galileo_Rinex302.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.02,                          file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,      file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.GALILEO,       file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("sbf2rin-10.2.0",              file.getHeader().getProgramName());
        Assertions.assertEquals("",                            file.getHeader().getRunByName());
        Assertions.assertEquals("2016-04-28T00:36:37.0",       file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("LCL",                         file.getHeader().getCreationTimeZone());
        Assertions.assertEquals(IonosphericCorrectionType.GAL, file.getHeader().getIonosphericCorrectionType());
        Assertions.assertEquals(3.5500E+01,                    file.getNeQuickAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(-2.3438E-02,                   file.getNeQuickAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(1.6632E-02,                    file.getNeQuickAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,                    file.getNeQuickAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals("GPGA", file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals("GAUT", file.getHeader().getTimeSystemCorrections().get(1).getTimeSystemCorrectionType());
        Assertions.assertEquals(-2.9103830457E-11,             file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionA0(), Double.MIN_VALUE);
        Assertions.assertEquals(-4.440892099E-16,              file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionA1(), Double.MIN_VALUE);
        GNSSDate date = new GNSSDate(file.getHeader().getTimeSystemCorrections().get(0).getReferenceDate(), SatelliteSystem.GPS);
        Assertions.assertEquals(313200,                        date.getSecondsInWeek());
        Assertions.assertEquals(1920,                          date.getWeekNumber());

        Assertions.assertTrue(file.getComments().isEmpty());
        Assertions.assertEquals(17, file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(2, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());

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
        final AbsoluteDate obsRebuiltDate = new GNSSDate(gal.getWeek(), gal.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(gal.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(gal.getDate()), 1.0e-15);

        // check the propagator
        final GNSSPropagator propagator = gal.getPropagator(DataContext.getDefault().getFrames());
        final AbsoluteDate date0 = gal.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = gal.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();
        Assertions.assertEquals(0., p0.distance(p1), 0.);

    }

    @Test
    public void testGalileoRinex400() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Galileo_Rinex400.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(4.00,                                 file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,             file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,                file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("BCEmerge",                           file.getHeader().getProgramName());
        Assertions.assertEquals("congo",                              file.getHeader().getRunByName());
        Assertions.assertEquals("https://doi.org/10.57677/BRD400DLR", file.getHeader().getDoi());
        Assertions.assertNull(file.getHeader().getLicense());
        Assertions.assertNull(file.getHeader().getStationInformation());
        Assertions.assertEquals(18,                                   file.getHeader().getNumberOfLeapSeconds());
        Assertions.assertEquals(102,                                  file.getHeader().getMergedFiles());

        // Verify data
        Assertions.assertEquals(1, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSCivilianNavigationMessages().size());

        final GalileoNavigationMessage galL = file.getGalileoNavigationMessages().get("E01").get(0);
        Assertions.assertEquals(0.0, galL.getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 0, 30, 0, TimeScalesFactory.getGPS())), Double.MIN_VALUE);
        Assertions.assertEquals(516, galL.getDataSource());

    }

    @Test
    public void testQZSSRinex302() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_QZSS_Rinex302.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.02,                          file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,      file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.QZSS,          file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("NetR9 5.45",                  file.getHeader().getProgramName());
        Assertions.assertEquals("Receiver Operator",           file.getHeader().getRunByName());
        Assertions.assertEquals("2020-06-09T00:00:00.0",       file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                         file.getHeader().getCreationTimeZone());
        Assertions.assertEquals(IonosphericCorrectionType.QZS, file.getHeader().getIonosphericCorrectionType());
        Assertions.assertEquals(0.5588e-08,                    file.getKlobucharAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(0.7451e-08,                    file.getKlobucharAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(-0.4768e-06,                   file.getKlobucharAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(-0.1013e-05,                   file.getKlobucharAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals(0.8602e+05,                    file.getKlobucharBeta()[0],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.4096e+06,                   file.getKlobucharBeta()[1],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.8389e+07,                   file.getKlobucharBeta()[2],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.8389e+07,                   file.getKlobucharBeta()[3],  Double.MIN_VALUE);
        Assertions.assertEquals("QZUT", file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals(0.0,                           file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionA0(), Double.MIN_VALUE);
        Assertions.assertEquals(0.0,                           file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionA1(), Double.MIN_VALUE);
        GNSSDate date = new GNSSDate(file.getHeader().getTimeSystemCorrections().get(0).getReferenceDate(), SatelliteSystem.GPS);
        Assertions.assertEquals(356352,                        date.getSecondsInWeek());
        Assertions.assertEquals(2109,                          date.getWeekNumber());
        Assertions.assertEquals(0,                             file.getComments().size());
        Assertions.assertEquals(18,                            file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(3, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());

        final QZSSLegacyNavigationMessage qzs = file.getQZSSLegacyNavigationMessages("J07").get(0);
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
        final AbsoluteDate obsRebuiltDate = new GNSSDate(qzs.getWeek(), qzs.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(qzs.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(qzs.getDate()), 1.0e-15);

        // check the propagator
        final Frames frames = DataContext.getDefault().getFrames();
        final GNSSPropagator propagator = qzs.getPropagator(DataContext.getDefault().getFrames(), Propagator.getDefaultLaw(frames),
                FramesFactory.getEME2000(), FramesFactory.getITRF(IERSConventions.IERS_2010, true), Propagator.DEFAULT_MASS);
        final AbsoluteDate date0 = qzs.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = qzs.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();
        Assertions.assertEquals(0., p0.distance(p1), 0.);

    }

    @Test
    public void testQZSSRinex400() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_QZSS_Rinex400.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(4.00,                                 file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,             file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,                file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("BCEmerge",                           file.getHeader().getProgramName());
        Assertions.assertEquals("congo",                              file.getHeader().getRunByName());
        Assertions.assertEquals("https://doi.org/10.57677/BRD400DLR", file.getHeader().getDoi());
        Assertions.assertNull(file.getHeader().getLicense());
        Assertions.assertNull(file.getHeader().getStationInformation());
        Assertions.assertEquals(18,                                   file.getHeader().getNumberOfLeapSeconds());
        Assertions.assertEquals(102,                                  file.getHeader().getMergedFiles());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(1, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(1, file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSCivilianNavigationMessages().size());

        final QZSSLegacyNavigationMessage qzssl = file.getQZSSLegacyNavigationMessages("J02").get(0);
        Assertions.assertEquals(0.0, qzssl.getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 0, 0, 0, TimeScalesFactory.getQZSS())), Double.MIN_VALUE);
        Assertions.assertEquals(0, qzssl.getSvHealth());
        Assertions.assertEquals(0, qzssl.getFitInterval());

        final List<QZSSCivilianNavigationMessage> list = file.getQZSSCivilianNavigationMessages("J02");
        Assertions.assertEquals(4, list.size());
        Assertions.assertEquals(0.0, list.get(0).getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 0, 0, 0, TimeScalesFactory.getGPS())), Double.MIN_VALUE);
        Assertions.assertEquals(-3, list.get(0).getUraiNed0());
        Assertions.assertEquals( 0, list.get(0).getUraiNed1());
        Assertions.assertEquals( 0, list.get(0).getUraiNed2());
        Assertions.assertEquals(-8, list.get(0).getUraiEd());
        Assertions.assertEquals( 0.000000000000e+00, list.get(0).getIscL1CA(), 1.0e-20);
        Assertions.assertEquals(-3.783497959375e-10, list.get(0).getIscL2C(),  1.0e-20);
        Assertions.assertEquals(1.600710675120e-09,  list.get(0).getIscL5I5(), 1.0e-20);
        Assertions.assertEquals(1.688022166491e-09,  list.get(0).getIscL5Q5(), 1.0e-20);
        Assertions.assertEquals(255606.0, list.get(0).getTransmissionTime(), 1.0e-10);
        Assertions.assertEquals(0.0, list.get(1).getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 1, 0, 0, TimeScalesFactory.getGPS())), Double.MIN_VALUE);

    }

    @Test
    public void testGLONASSRinex303() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Glonass_Rinex303.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.03,                    file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.GLONASS, file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("GR25 V4.30",            file.getHeader().getProgramName());
        Assertions.assertEquals("Institute of Astrono",  file.getHeader().getRunByName());
        Assertions.assertEquals("2021-02-17T23:59:47.0", file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                   file.getHeader().getCreationTimeZone());
        Assertions.assertEquals("GLUT", file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals(6.0535967350e-09,        file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionA0(), Double.MIN_VALUE);
        Assertions.assertEquals(0.000000000e+00,         file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionA1(), Double.MIN_VALUE);
        Assertions.assertNull(file.getHeader().getTimeSystemCorrections().get(0).getReferenceDate());
        Assertions.assertEquals(0,                       file.getComments().size());
        Assertions.assertEquals(18,                      file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(3, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());

        final GLONASSNavigationMessage glo = file.getGlonassNavigationMessages("R02").get(0);
        Assertions.assertEquals(0.0, glo.getEpochToc().durationFrom(new AbsoluteDate(2021, 2, 17, 23, 45, 0.0, TimeScalesFactory.getUTC())), Double.MIN_VALUE);
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

        // check the propagator
        final GLONASSNumericalPropagator propagator1 = glo.getPropagator(60.0);
        final GLONASSNumericalPropagator propagator2 = glo.getPropagator(60, DataContext.getDefault());
        final GLONASSNumericalPropagator propagator3 = glo.getPropagator(60, DataContext.getDefault(),
                Propagator.getDefaultLaw(DataContext.getDefault().getFrames()),
                FramesFactory.getEME2000(), Propagator.DEFAULT_MASS);
        Assertions.assertNotNull(propagator1);
        Assertions.assertNotNull(propagator2);
        Assertions.assertNotNull(propagator3);
        
    }

    @Test
    public void testIRNSSRinex303() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_IRNSS_Rinex303.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.03,                    file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.IRNSS,   file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("JPS2RIN v.2.0.168",     file.getHeader().getProgramName());
        Assertions.assertEquals("JAVAD GNSS",            file.getHeader().getRunByName());
        Assertions.assertEquals("2019-10-28T00:56:48.0", file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                   file.getHeader().getCreationTimeZone());
        Assertions.assertEquals(18,                      file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(3, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());

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
        final AbsoluteDate obsRebuiltDate = new GNSSDate(irnss.getWeek(), irnss.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(irnss.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(irnss.getDate()), 1.0e-15);

        // check the propagator
        final GNSSPropagator propagator = irnss.getPropagator();
        final AbsoluteDate date0 = irnss.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = irnss.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();
        Assertions.assertEquals(0., p0.distance(p1), 0.);

    }

    @Test
    public void testIRNSSRinex400() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_IRNSS_Rinex400.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(4.00,                                 file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,             file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,                file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("BCEmerge",                           file.getHeader().getProgramName());
        Assertions.assertEquals("congo",                              file.getHeader().getRunByName());
        Assertions.assertEquals("https://doi.org/10.57677/BRD400DLR", file.getHeader().getDoi());
        Assertions.assertNull(file.getHeader().getLicense());
        Assertions.assertNull(file.getHeader().getStationInformation());
        Assertions.assertEquals(18,                                   file.getHeader().getNumberOfLeapSeconds());
        Assertions.assertEquals(102,                                  file.getHeader().getMergedFiles());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(1, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSCivilianNavigationMessages().size());

        final IRNSSNavigationMessage irnL = file.getIRNSSNavigationMessages().get("I02").get(0);
        Assertions.assertEquals(0.0, irnL.getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 0, 5, 36, TimeScalesFactory.getIRNSS())), Double.MIN_VALUE);
    }

    @Test
    public void testMixedRinex304() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Mixed_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                    file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,   file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("Alloy 5.37",            file.getHeader().getProgramName());
        Assertions.assertEquals("Receiver Operator",     file.getHeader().getRunByName());
        Assertions.assertEquals("2020-02-11T00:00:00.0", file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                   file.getHeader().getCreationTimeZone());
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
        Assertions.assertEquals("GPUT", file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals("GAUT", file.getHeader().getTimeSystemCorrections().get(1).getTimeSystemCorrectionType());
        Assertions.assertEquals("GPGA", file.getHeader().getTimeSystemCorrections().get(2).getTimeSystemCorrectionType());
        Assertions.assertEquals(18,                      file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(2, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(1, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(2, file.getGPSLegacyNavigationMessages().size());

        final GLONASSNavigationMessage glo = file.getGlonassNavigationMessages("R05").get(0);
        Assertions.assertEquals(0.0, glo.getEpochToc().durationFrom(new AbsoluteDate(2020, 2, 10, 23, 45, 0.0, TimeScalesFactory.getUTC())), Double.MIN_VALUE);
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
    public void testMixedRinex305() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Mixed_Rinex305.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.05,                    file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,   file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("XXRINEXN V3",           file.getHeader().getProgramName());
        Assertions.assertEquals("AIUB",                  file.getHeader().getRunByName());
        Assertions.assertEquals("2006-10-02T00:01:23.0", file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                   file.getHeader().getCreationTimeZone());
        Assertions.assertEquals(0.1025E-07,              file.getKlobucharAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(0.7451E-08,              file.getKlobucharAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(-0.5960E-07,             file.getKlobucharAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(-0.5960E-07,             file.getKlobucharAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals(0.8806E+05,              file.getKlobucharBeta()[0],  Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,              file.getKlobucharBeta()[1],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.1966E+06,             file.getKlobucharBeta()[2],  Double.MIN_VALUE);
        Assertions.assertEquals(-0.6554E+05,             file.getKlobucharBeta()[3],  Double.MIN_VALUE);
        Assertions.assertEquals("GPUT", file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals("GLUT", file.getHeader().getTimeSystemCorrections().get(1).getTimeSystemCorrectionType());
        Assertions.assertEquals(14,                      file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(2, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(2, file.getGPSLegacyNavigationMessages().size());

        final GLONASSNavigationMessage glo = file.getGlonassNavigationMessages("R01").get(0);
        Assertions.assertEquals(0.0, glo.getEpochToc().durationFrom(new AbsoluteDate(2006, 10, 1, 0, 15, 0.0, TimeScalesFactory.getUTC())), Double.MIN_VALUE);
        Assertions.assertEquals( 0.137668102980E-04,  glo.getTN(),      1.0e-10);
        Assertions.assertEquals(-0.454747350886E-11,  glo.getGammaN(),  1.0e-10);
        Assertions.assertEquals(90.0,                 glo.getTime(),    1.0e-10);
        Assertions.assertEquals(0.157594921875E+08,   glo.getX(),       1.0e-6);
        Assertions.assertEquals(-0.145566368103E+04,  glo.getXDot(),    1.0e-9);
        Assertions.assertEquals(0.000000000000E+00,   glo.getXDotDot(), 1.0e-12);
        Assertions.assertEquals(0.000000000000e+00,   glo.getHealth(),  1.0e-10);
        Assertions.assertEquals(-0.813711474609E+07,  glo.getY(),       1.0e-6);
        Assertions.assertEquals(0.205006790161E+04,   glo.getYDot(),    1.0e-9);
        Assertions.assertEquals(0.931322574615E-06,   glo.getYDotDot(), 1.0e-12);
        Assertions.assertEquals(7,                    glo.getFrequencyNumber());
        Assertions.assertEquals(0.183413398438E+08,   glo.getZ(),       1.0e-6);
        Assertions.assertEquals(0.215388488770E+04,   glo.getZDot(),    1.0e-9);
        Assertions.assertEquals(-0.186264514923E-05,  glo.getZDotDot(), 1.0e-12);
        Assertions.assertEquals(179,  glo.getStatusFlags());
        Assertions.assertEquals(8.381903171539E-09,  glo.getGroupDelayDifference(), 1.0e-15);
        Assertions.assertEquals(2.0,  glo.getURA(), 1.0e-10);
        Assertions.assertEquals(3,  glo.getHealthFlags());

    }

    @Test
    public void testQZSSRinex304() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_QZSS_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                    file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.QZSS,    file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("JPS2RIN v.2.0.191",     file.getHeader().getProgramName());
        Assertions.assertEquals("JAVAD GNSS",            file.getHeader().getRunByName());
        Assertions.assertEquals("2020-06-10T00:32:46.0", file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                   file.getHeader().getCreationTimeZone());
        Assertions.assertEquals(18,                      file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(3, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());

        final QZSSLegacyNavigationMessage qzs = file.getQZSSLegacyNavigationMessages("J03").get(0);
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
        final AbsoluteDate obsRebuiltDate = new GNSSDate(qzs.getWeek(), qzs.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(qzs.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(qzs.getDate()), 1.0e-15);

        // check the propagator
        final GNSSPropagator propagator = qzs.getPropagator(DataContext.getDefault().getFrames());
        final AbsoluteDate date0 = qzs.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = qzs.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();
        Assertions.assertEquals(0., p0.distance(p1), 0.);

    }

    @Test
    public void testGpsRinex304() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_GPS_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                          file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,      file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.GPS,           file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("sbf2rin-13.8.0",              file.getHeader().getProgramName());
        Assertions.assertEquals("",                            file.getHeader().getRunByName());
        Assertions.assertEquals("2021-03-07T00:08:19.0",       file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                         file.getHeader().getCreationTimeZone());
        Assertions.assertEquals(0.0,                           file.getHeader().getCreationDate().durationFrom(new AbsoluteDate(2021, 3, 7, 0, 8, 19.0, TimeScalesFactory.getUTC())), 0.0);
        Assertions.assertEquals(IonosphericCorrectionType.GPS, file.getHeader().getIonosphericCorrectionType());
        Assertions.assertEquals(1.0245E-08,                    file.getKlobucharAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,                    file.getKlobucharAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(-5.9605E-08,                   file.getKlobucharAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,                    file.getKlobucharAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals(9.0112E+04,                    file.getKlobucharBeta()[0],  Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,                    file.getKlobucharBeta()[1],  Double.MIN_VALUE);
        Assertions.assertEquals(-1.9661E+05,                   file.getKlobucharBeta()[2],  Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,                    file.getKlobucharBeta()[3],  Double.MIN_VALUE);
        Assertions.assertEquals("GPUT",                        file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionType());
        Assertions.assertEquals(0.0000000000E+00,              file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionA0(), Double.MIN_VALUE);
        Assertions.assertEquals(9.769962617E-15,               file.getHeader().getTimeSystemCorrections().get(0).getTimeSystemCorrectionA1(), Double.MIN_VALUE);
        GNSSDate date = new GNSSDate(file.getHeader().getTimeSystemCorrections().get(0).getReferenceDate(), SatelliteSystem.GPS);
        Assertions.assertEquals(233472,                        date.getSecondsInWeek());
        Assertions.assertEquals(2148,                          date.getWeekNumber());
        Assertions.assertEquals(18,                            file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(3, file.getGPSLegacyNavigationMessages().size());

        final GPSLegacyNavigationMessage gps = file.getGPSLegacyNavigationMessages("G01").get(0);
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
        Assertions.assertEquals(0,                   gps.getSvHealth());
        Assertions.assertEquals(4.656612873077E-09,  gps.getTGD(), 1.0e-15);
        Assertions.assertEquals(9,                   gps.getIODC());

        // check weeks reference in Rinex navigation are aligned with GPS weeks
        final AbsoluteDate obsRebuiltDate = new GNSSDate(gps.getWeek(), gps.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(gps.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(gps.getDate()), 1.0e-15);

        // check the propagator
        final Frames frames = DataContext.getDefault().getFrames();
        final GNSSPropagator propagator = gps.getPropagator(DataContext.getDefault().getFrames(), Propagator.getDefaultLaw(frames),
                FramesFactory.getEME2000(), FramesFactory.getITRF(IERSConventions.IERS_2010, true), Propagator.DEFAULT_MASS);
        final AbsoluteDate date0 = gps.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = gps.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();
        Assertions.assertEquals(0., p0.distance(p1), 0.);

    }

    @Test
    public void testGalileoRinex304() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Galileo_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                          file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,      file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.GALILEO,       file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("JPS2RIN v.2.0.191",           file.getHeader().getProgramName());
        Assertions.assertEquals("JAVAD GNSS",                  file.getHeader().getRunByName());
        Assertions.assertEquals("2021-03-07T00:02:45.0",       file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                         file.getHeader().getCreationTimeZone());
        Assertions.assertEquals(IonosphericCorrectionType.GAL, file.getHeader().getIonosphericCorrectionType());
        Assertions.assertEquals(5.0500E+01,                    file.getNeQuickAlpha()[0], Double.MIN_VALUE);
        Assertions.assertEquals(2.7344E-02,                    file.getNeQuickAlpha()[1], Double.MIN_VALUE);
        Assertions.assertEquals(-1.5869E-03,                   file.getNeQuickAlpha()[2], Double.MIN_VALUE);
        Assertions.assertEquals(0.0000E+00,                    file.getNeQuickAlpha()[3], Double.MIN_VALUE);
        Assertions.assertEquals(18,                            file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(1, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());

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
        final AbsoluteDate obsRebuiltDate = new GNSSDate(gal.getWeek(), gal.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(gal.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(gal.getDate()), 1.0e-15);

        // check the propagator
        final Frames frames = DataContext.getDefault().getFrames();
        final GNSSPropagator propagator = gal.getPropagator(DataContext.getDefault().getFrames(), Propagator.getDefaultLaw(frames),
                FramesFactory.getEME2000(), FramesFactory.getITRF(IERSConventions.IERS_2010, true), Propagator.DEFAULT_MASS);
        final AbsoluteDate date0 = gal.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = gal.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();
        Assertions.assertEquals(0., p0.distance(p1), 0.);
        
    }

    @Test
    public void testSBASRinex304() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_SBAS_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                    file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.SBAS,    file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("sbf2rin-13.4.5",        file.getHeader().getProgramName());
        Assertions.assertEquals("RIGTC, GO PECNY",       file.getHeader().getRunByName());
        Assertions.assertEquals("2021-02-19T00:26:27.0", file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("SBAS NAVIGATION DATA FROM STATION GOP6 (RIGTC, GO PECNY)", file.getComments().get(0).getText());
        Assertions.assertEquals("UTC",                   file.getHeader().getCreationTimeZone());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(3, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());

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

        // check the propagator
        final Frames frames = DataContext.getDefault().getFrames();
        final SBASPropagator propagator = sbas.getPropagator(frames, Propagator.getDefaultLaw(frames),
                FramesFactory.getEME2000(), FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                Propagator.DEFAULT_MASS, GNSSConstants.SBAS_MU);
        final PVCoordinates pv = propagator.propagateInEcef(sbas.getDate());
        final Vector3D position = pv.getPosition();
        final Vector3D velocity = pv.getVelocity();
        final Vector3D acceleration = pv.getAcceleration();
        double eps = 1.0e-15;
        Assertions.assertEquals(sbas.getX(),       position.getX(),     eps);
        Assertions.assertEquals(sbas.getY(),       position.getY(),     eps);
        Assertions.assertEquals(sbas.getZ(),       position.getZ(),     eps);
        Assertions.assertEquals(sbas.getXDot(),    velocity.getX(),     eps);
        Assertions.assertEquals(sbas.getYDot(),    velocity.getY(),     eps);
        Assertions.assertEquals(sbas.getZDot(),    velocity.getZ(),     eps);
        Assertions.assertEquals(sbas.getXDotDot(), acceleration.getX(), eps);
        Assertions.assertEquals(sbas.getYDotDot(), acceleration.getY(), eps);
        Assertions.assertEquals(sbas.getZDotDot(), acceleration.getZ(), eps);

    }

    @Test
    public void testSBASRinex400() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_SBAS_Rinex400.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(4.00,                                 file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,             file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,                file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("BCEmerge",                           file.getHeader().getProgramName());
        Assertions.assertEquals("congo",                              file.getHeader().getRunByName());
        Assertions.assertEquals("https://doi.org/10.57677/BRD400DLR", file.getHeader().getDoi());
        Assertions.assertNull(file.getHeader().getLicense());
        Assertions.assertNull(file.getHeader().getStationInformation());
        Assertions.assertEquals(18,                                   file.getHeader().getNumberOfLeapSeconds());
        Assertions.assertEquals(102,                                  file.getHeader().getMergedFiles());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(1, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSCivilianNavigationMessages().size());

        final SBASNavigationMessage sbas = file.getSBASNavigationMessages().get("S22").get(0);
        Assertions.assertEquals(0.0, sbas.getEpochToc().durationFrom(new AbsoluteDate(2022, 10, 5, 0, 0, 32, TimeScalesFactory.getGPS())), Double.MIN_VALUE);

    }

    @Test
    public void testIRNSSRinex304() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_IRNSS_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                    file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.IRNSS,   file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("JPS2RIN v.2.0.191",     file.getHeader().getProgramName());
        Assertions.assertEquals("JAVAD GNSS",            file.getHeader().getRunByName());
        Assertions.assertEquals("2021-03-08T00:03:04.0", file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                   file.getHeader().getCreationTimeZone());
        Assertions.assertEquals(18,                      file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(2, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());

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
        final AbsoluteDate obsRebuiltDate = new GNSSDate(irnss.getWeek(), irnss.getTime(), SatelliteSystem.GPS).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(irnss.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);
        Assertions.assertEquals(0.0, obsRebuiltDate.durationFrom(irnss.getDate()), 1.0e-15);

        // check the propagator
        final GNSSPropagator propagator = irnss.getPropagator();
        final AbsoluteDate date0 = irnss.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = irnss.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();
        Assertions.assertEquals(0., p0.distance(p1), 0.);

    }

    @Test
    public void testBeidouRinex304() throws URISyntaxException, IOException {

        final String ex = "/gnss/navigation/Example_Beidou_Rinex304.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(3.04,                    file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.BEIDOU,  file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("JPS2RIN v.2.0.191",     file.getHeader().getProgramName());
        Assertions.assertEquals("JAVAD GNSS",            file.getHeader().getRunByName());
        Assertions.assertEquals("2021-02-24T00:07:15.0", file.getHeader().getCreationDateComponents().toStringWithoutUtcOffset(60, 1));
        Assertions.assertEquals("UTC",                   file.getHeader().getCreationTimeZone());
        Assertions.assertEquals(18,                      file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0, file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0, file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(1, file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0, file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0, file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0, file.getGPSLegacyNavigationMessages().size());

        final BeidouLegacyNavigationMessage bdt = file.getBeidouLegacyNavigationMessages("C19").get(0);
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
        final AbsoluteDate obsRebuiltDate = new GNSSDate(bdt.getWeek(), bdt.getTime(), SatelliteSystem.BEIDOU).
                                            getDate();
        final double relativeTime = obsRebuiltDate.durationFrom(bdt.getEpochToc());
        Assertions.assertEquals(0.0, relativeTime / Constants.JULIAN_DAY, 7.0);

        // check the propagator
        final GNSSPropagator propagator = bdt.getPropagator();
        final AbsoluteDate date0 = bdt.getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = bdt.getCycleDuration();
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();
        Assertions.assertEquals(0., p0.distance(p1), 0.);

    }

    @Test
    public void testStoRinex400() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Sto_Rinex400.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(4.00,                                 file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,             file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,                file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("BCEmerge",                           file.getHeader().getProgramName());
        Assertions.assertEquals("congo",                              file.getHeader().getRunByName());
        Assertions.assertEquals("https://doi.org/10.57677/BRD400DLR", file.getHeader().getDoi());
        Assertions.assertNull(file.getHeader().getLicense());
        Assertions.assertNull(file.getHeader().getStationInformation());
        Assertions.assertEquals(18,                                   file.getHeader().getNumberOfLeapSeconds());
        Assertions.assertEquals(102,                                  file.getHeader().getMergedFiles());

        // Verify data
        Assertions.assertEquals(0,  file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0,  file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0,  file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGPSCivilianNavigationMessages().size());
        Assertions.assertEquals(16, file.getSystemTimeOffsets().size());
        Assertions.assertEquals(0,  file.getEarthOrientationParameters().size());
        Assertions.assertEquals(0,  file.getKlobucharMessages().size());
        Assertions.assertEquals(0,  file.getNequickGMessages().size());
        Assertions.assertEquals(0,  file.getBDGIMMessages().size());

        List<SystemTimeOffsetMessage> list = file.getSystemTimeOffsets();
        Assertions.assertEquals(SatelliteSystem.BEIDOU, list.get(0).getSystem());
        Assertions.assertEquals(35, list.get(0).getPrn());
        Assertions.assertEquals("CNVX", list.get(0).getNavigationMessageType());
        Assertions.assertEquals(TimeSystem.BEIDOU, list.get(0).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.GALILEO, list.get(0).getReferenceTimeSystem());
        Assertions.assertNull(list.get(0).getSbasId());
        Assertions.assertNull(list.get(0).getUtcId());
        Assertions.assertEquals(0.0,
                                list.get(0).getReferenceEpoch().durationFrom(new AbsoluteDate(2022, 10, 4, 23, 20, 0.0,
                                                                                              TimeScalesFactory.getBDT())),
                                1.0e-15);
        Assertions.assertEquals(259230.0,            list.get( 0).getTransmissionTime(), 1.0e-15);
        Assertions.assertEquals(-2.657179720700e-08, list.get( 0).getA0(), 1.0e-17);
        Assertions.assertEquals( 4.884981308351e-14, list.get( 0).getA1(), 1.0e-23);
        Assertions.assertEquals( 2.066760391300e-19, list.get( 0).getA2(), 1.0e-28);

        Assertions.assertEquals(TimeSystem.BEIDOU,   list.get( 1).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.GLONASS,  list.get( 1).getReferenceTimeSystem());
        Assertions.assertEquals(TimeSystem.BEIDOU,   list.get( 2).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.GPS,      list.get( 2).getReferenceTimeSystem());
        Assertions.assertEquals(TimeSystem.BEIDOU,   list.get( 3).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.UTC,      list.get( 3).getReferenceTimeSystem());
        Assertions.assertEquals(UtcId.NTSC,          list.get( 3).getUtcId());
        Assertions.assertEquals(TimeSystem.GALILEO,  list.get( 4).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.GPS,      list.get( 4).getReferenceTimeSystem());
        Assertions.assertEquals(TimeSystem.GLONASS,  list.get( 5).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.GPS,      list.get( 5).getReferenceTimeSystem());
        Assertions.assertEquals(TimeSystem.GLONASS,  list.get( 6).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.GPS,      list.get( 6).getReferenceTimeSystem());
        Assertions.assertEquals(TimeSystem.GLONASS,  list.get( 7).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.UTC,      list.get( 7).getReferenceTimeSystem());
        Assertions.assertEquals(UtcId.SU,            list.get( 7).getUtcId());
        Assertions.assertEquals(TimeSystem.GPS,      list.get( 8).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.UTC,      list.get( 8).getReferenceTimeSystem());
        Assertions.assertEquals(UtcId.USNO,          list.get( 8).getUtcId());
        Assertions.assertEquals(TimeSystem.IRNSS,    list.get( 9).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.GLONASS,  list.get( 9).getReferenceTimeSystem());
        Assertions.assertEquals(TimeSystem.IRNSS,    list.get(10).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.GPS,      list.get(10).getReferenceTimeSystem());
        Assertions.assertEquals(TimeSystem.IRNSS,    list.get(11).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.UTC,      list.get(11).getReferenceTimeSystem());
        Assertions.assertEquals(UtcId.IRN,           list.get(11).getUtcId());
        Assertions.assertEquals(TimeSystem.IRNSS,    list.get(12).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.UTC,      list.get(12).getReferenceTimeSystem());
        Assertions.assertEquals(UtcId.IRN,           list.get(12).getUtcId());
        Assertions.assertEquals(TimeSystem.QZSS,     list.get(13).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.GPS,      list.get(13).getReferenceTimeSystem());
        Assertions.assertEquals(TimeSystem.QZSS,     list.get(14).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.UTC,      list.get(14).getReferenceTimeSystem());
        Assertions.assertEquals(UtcId.NICT,          list.get(14).getUtcId());
        Assertions.assertEquals(TimeSystem.SBAS,     list.get(15).getDefinedTimeSystem());
        Assertions.assertEquals(TimeSystem.UTC,      list.get(15).getReferenceTimeSystem());
        Assertions.assertEquals(SbasId.EGNOS,        list.get(15).getSbasId());
        Assertions.assertEquals(UtcId.OP,            list.get(15).getUtcId());

    }

    @Test
    public void testEopRinex400() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Eop_Rinex400.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(4.00,                                 file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,             file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,                file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("BCEmerge",                           file.getHeader().getProgramName());
        Assertions.assertEquals("congo",                              file.getHeader().getRunByName());
        Assertions.assertEquals("https://doi.org/10.57677/BRD400DLR", file.getHeader().getDoi());
        Assertions.assertNull(file.getHeader().getLicense());
        Assertions.assertNull(file.getHeader().getStationInformation());
        Assertions.assertEquals(18,                                   file.getHeader().getNumberOfLeapSeconds());
        Assertions.assertEquals(102,                                  file.getHeader().getMergedFiles());

        // Verify data
        Assertions.assertEquals(0,  file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0,  file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0,  file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGPSCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getSystemTimeOffsets().size());
        Assertions.assertEquals(3,  file.getEarthOrientationParameters().size());
        Assertions.assertEquals(0,  file.getKlobucharMessages().size());
        Assertions.assertEquals(0,  file.getNequickGMessages().size());
        Assertions.assertEquals(0,  file.getBDGIMMessages().size());

        List<EarthOrientationParameterMessage> list = file.getEarthOrientationParameters();

        Assertions.assertEquals(SatelliteSystem.GPS, list.get(0).getSystem());
        Assertions.assertEquals(15, list.get(0).getPrn());
        Assertions.assertEquals("CNVX", list.get(0).getNavigationMessageType());
        Assertions.assertEquals(0.0,
                                new AbsoluteDate(2022, 10, 6, 16, 38, 24.0, TimeScalesFactory.getGPS()).durationFrom(list.get(0).getReferenceEpoch()),
                                1.0e-15);
        Assertions.assertEquals( 2.734127044678e-01, Unit.ARC_SECOND.fromSI(list.get(0).getXp()),            1.0e-10);
        Assertions.assertEquals(-1.487731933594e-03, Unit.parse("as/d").fromSI(list.get(0).getXpDot()),      1.0e-10);
        Assertions.assertEquals( 0.000000000000e+00, Unit.parse("as/d²").fromSI(list.get(0).getXpDotDot()),   1.0e-10);
        Assertions.assertEquals( 2.485857009888e-01, Unit.ARC_SECOND.fromSI(list.get(0).getYp()),            1.0e-10);
        Assertions.assertEquals(-1.955032348633e-03, Unit.parse("as/d").fromSI(list.get(0).getYpDot()),      1.0e-10);
        Assertions.assertEquals( 0.000000000000e+00, Unit.parse("as/d²").fromSI(list.get(0).getYpDotDot()),  1.0e-10);
        Assertions.assertEquals(259728.0,            Unit.SECOND.fromSI(list.get(0).getTransmissionTime()),  1.0e-10);
        Assertions.assertEquals(-3.280282020569e-03, Unit.SECOND.fromSI(list.get(0).getDut1()),              1.0e-10);
        Assertions.assertEquals( 1.151263713837e-04, Unit.parse("s/d").fromSI(list.get(0).getDut1Dot()),     1.0e-10);
        Assertions.assertEquals( 0.000000000000e+00, Unit.parse("s/d²").fromSI(list.get(0).getDut1DotDot()), 1.0e-10);

        Assertions.assertEquals(SatelliteSystem.QZSS, list.get(1).getSystem());
        Assertions.assertEquals(4, list.get(1).getPrn());
        Assertions.assertEquals("CNVX", list.get(1).getNavigationMessageType());
        Assertions.assertEquals(0.0,
                                new AbsoluteDate(2022, 10, 6, 0, 0, 0.0, TimeScalesFactory.getQZSS()).durationFrom(list.get(1).getReferenceEpoch()),
                                1.0e-15);
        Assertions.assertEquals( 2.723026275635e-01, Unit.ARC_SECOND.fromSI(list.get(1).getXp()),            1.0e-10);
        Assertions.assertEquals(-2.049922943115e-03, Unit.parse("as/d").fromSI(list.get(1).getXpDot()),      1.0e-10);
        Assertions.assertEquals( 0.000000000000e+00, Unit.parse("as/d²").fromSI(list.get(1).getXpDotDot()),   1.0e-10);
        Assertions.assertEquals( 2.491779327393e-01, Unit.ARC_SECOND.fromSI(list.get(1).getYp()),            1.0e-10);
        Assertions.assertEquals(-1.977920532227e-03, Unit.parse("as/d").fromSI(list.get(1).getYpDot()),      1.0e-10);
        Assertions.assertEquals( 0.000000000000e+00, Unit.parse("as/d²").fromSI(list.get(1).getYpDotDot()),  1.0e-10);
        Assertions.assertEquals(342186.0,            Unit.SECOND.fromSI(list.get(1).getTransmissionTime()),  1.0e-10);
        Assertions.assertEquals(-3.003299236298e-03, Unit.SECOND.fromSI(list.get(1).getDut1()),              1.0e-10);
        Assertions.assertEquals( 2.534389495850e-04, Unit.parse("s/d").fromSI(list.get(1).getDut1Dot()),     1.0e-10);
        Assertions.assertEquals( 0.000000000000e+00, Unit.parse("s/d²").fromSI(list.get(1).getDut1DotDot()), 1.0e-10);

        Assertions.assertEquals(SatelliteSystem.IRNSS, list.get(2).getSystem());
        Assertions.assertEquals(3, list.get(2).getPrn());
        Assertions.assertEquals("LNAV", list.get(2).getNavigationMessageType());
        Assertions.assertEquals(0.0,
                                new AbsoluteDate(2022, 10, 5, 0, 0, 0.0, TimeScalesFactory.getIRNSS()).durationFrom(list.get(2).getReferenceEpoch()),
                                1.0e-15);
        Assertions.assertEquals( 2.751779556274e-01, Unit.ARC_SECOND.fromSI(list.get(2).getXp()),            1.0e-10);
        Assertions.assertEquals(-1.739501953125e-03, Unit.parse("as/d").fromSI(list.get(2).getXpDot()),      1.0e-10);
        Assertions.assertEquals( 0.000000000000e+00, Unit.parse("as/d²").fromSI(list.get(2).getXpDotDot()),   1.0e-10);
        Assertions.assertEquals( 2.516956329346e-01, Unit.ARC_SECOND.fromSI(list.get(2).getYp()),            1.0e-10);
        Assertions.assertEquals(-1.918315887451e-03, Unit.parse("as/d").fromSI(list.get(2).getYpDot()),      1.0e-10);
        Assertions.assertEquals( 0.000000000000e+00, Unit.parse("as/d²").fromSI(list.get(2).getYpDotDot()),  1.0e-10);
        Assertions.assertEquals(259248.0,            Unit.SECOND.fromSI(list.get(2).getTransmissionTime()),  1.0e-10);
        Assertions.assertEquals(-3.213942050934e-03, Unit.SECOND.fromSI(list.get(2).getDut1()),              1.0e-10);
        Assertions.assertEquals( 1.052916049957e-04, Unit.parse("s/d").fromSI(list.get(2).getDut1Dot()),     1.0e-10);
        Assertions.assertEquals( 0.000000000000e+00, Unit.parse("s/d²").fromSI(list.get(2).getDut1DotDot()), 1.0e-10);

    }

    @Test
    public void testIonRinex400() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/Example_Ion_Rinex400.n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(4.00,                                 file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,             file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.MIXED,                file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("BCEmerge",                           file.getHeader().getProgramName());
        Assertions.assertEquals("congo",                              file.getHeader().getRunByName());
        Assertions.assertEquals("https://doi.org/10.57677/BRD400DLR", file.getHeader().getDoi());
        Assertions.assertNull(file.getHeader().getLicense());
        Assertions.assertNull(file.getHeader().getStationInformation());
        Assertions.assertEquals(18,                                   file.getHeader().getNumberOfLeapSeconds());
        Assertions.assertEquals(102,                                  file.getHeader().getMergedFiles());

        // Verify data
        Assertions.assertEquals(0,  file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0,  file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0,  file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGPSCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getSystemTimeOffsets().size());
        Assertions.assertEquals(0,  file.getEarthOrientationParameters().size());
        Assertions.assertEquals(6,  file.getKlobucharMessages().size());
        Assertions.assertEquals(1,  file.getNequickGMessages().size());
        Assertions.assertEquals(2,  file.getBDGIMMessages().size());

        List<IonosphereKlobucharMessage> listK = file.getKlobucharMessages();
        List<IonosphereNequickGMessage>  listN = file.getNequickGMessages();
        List<IonosphereBDGIMMessage>     listB = file.getBDGIMMessages();

        Assertions.assertEquals(SatelliteSystem.GPS, listK.get(0).getSystem());
        Assertions.assertEquals(17, listK.get(0).getPrn());
        Assertions.assertEquals("LNAV", listK.get(0).getNavigationMessageType());
        Assertions.assertEquals(0.0,
                                new AbsoluteDate(2022, 10, 5, 23, 33, 54.0, TimeScalesFactory.getGPS()).durationFrom(listK.get(0).getTransmitTime()),
                                1.0e-15);
        Assertions.assertEquals( 2.514570951462e-08, IonosphereKlobucharMessage.S_PER_SC_N[0].fromSI(listK.get(0).getAlpha()[0]), 1.0e-16);
        Assertions.assertEquals( 1.490116119385e-08, IonosphereKlobucharMessage.S_PER_SC_N[1].fromSI(listK.get(0).getAlpha()[1]), 1.0e-16);
        Assertions.assertEquals(-1.192092895508e-07, IonosphereKlobucharMessage.S_PER_SC_N[2].fromSI(listK.get(0).getAlpha()[2]), 1.0e-16);
        Assertions.assertEquals(-5.960464477539e-08, IonosphereKlobucharMessage.S_PER_SC_N[3].fromSI(listK.get(0).getAlpha()[3]), 1.0e-16);
        Assertions.assertEquals( 1.331200000000e+05, IonosphereKlobucharMessage.S_PER_SC_N[0].fromSI(listK.get(0).getBeta()[0]),  1.0e-10);
        Assertions.assertEquals(-1.638400000000e+04, IonosphereKlobucharMessage.S_PER_SC_N[1].fromSI(listK.get(0).getBeta()[1]),  1.0e-10);
        Assertions.assertEquals(-2.621440000000e+05, IonosphereKlobucharMessage.S_PER_SC_N[2].fromSI(listK.get(0).getBeta()[2]),  1.0e-10);
        Assertions.assertEquals( 1.966080000000e+05, IonosphereKlobucharMessage.S_PER_SC_N[3].fromSI(listK.get(0).getBeta()[3]),  1.0e-10);

        Assertions.assertEquals(SatelliteSystem.GPS, listK.get(1).getSystem());
        Assertions.assertEquals(25, listK.get(1).getPrn());
        Assertions.assertEquals("CNVX", listK.get(1).getNavigationMessageType());
        Assertions.assertEquals(0.0,
                                new AbsoluteDate(2022, 10, 5, 23, 30, 42.0, TimeScalesFactory.getGPS()).durationFrom(listK.get(1).getTransmitTime()),
                                1.0e-15);
        Assertions.assertEquals( 2.514570951462e-08, IonosphereKlobucharMessage.S_PER_SC_N[0].fromSI(listK.get(1).getAlpha()[0]), 1.0e-16);
        Assertions.assertEquals( 1.490116119385e-08, IonosphereKlobucharMessage.S_PER_SC_N[1].fromSI(listK.get(1).getAlpha()[1]), 1.0e-16);
        Assertions.assertEquals(-1.192092895508e-07, IonosphereKlobucharMessage.S_PER_SC_N[2].fromSI(listK.get(1).getAlpha()[2]), 1.0e-16);
        Assertions.assertEquals(-5.960464477539e-08, IonosphereKlobucharMessage.S_PER_SC_N[3].fromSI(listK.get(1).getAlpha()[3]), 1.0e-16);
        Assertions.assertEquals( 1.331200000000e+05, IonosphereKlobucharMessage.S_PER_SC_N[0].fromSI(listK.get(1).getBeta()[0]),  1.0e-10);
        Assertions.assertEquals(-1.638400000000e+04, IonosphereKlobucharMessage.S_PER_SC_N[1].fromSI(listK.get(1).getBeta()[1]),  1.0e-10);
        Assertions.assertEquals(-2.621440000000e+05, IonosphereKlobucharMessage.S_PER_SC_N[2].fromSI(listK.get(1).getBeta()[2]),  1.0e-10);
        Assertions.assertEquals( 1.966080000000e+05, IonosphereKlobucharMessage.S_PER_SC_N[3].fromSI(listK.get(1).getBeta()[3]),  1.0e-10);

        Assertions.assertEquals(SatelliteSystem.GALILEO, listN.get(0).getSystem());
        Assertions.assertEquals(2, listN.get(0).getPrn());
        Assertions.assertEquals("IFNV", listN.get(0).getNavigationMessageType());
        Assertions.assertEquals(0.0,
                                new AbsoluteDate(2022, 10, 5, 6, 21, 4.0, TimeScalesFactory.getGPS()).durationFrom(listN.get(0).getTransmitTime()),
                                1.0e-15);
        Assertions.assertEquals( 1.207500000000e+02, IonosphereNequickGMessage.SFU.fromSI(listN.get(0).getAi0()),          1.0e-16);
        Assertions.assertEquals( -1.953125000000e-01, IonosphereNequickGMessage.SFU_PER_DEG.fromSI(listN.get(0).getAi1()), 1.0e-16);
        Assertions.assertEquals(-7.629394531250e-04, IonosphereNequickGMessage.SFU_PER_DEG2.fromSI(listN.get(0).getAi2()), 1.0e-16);
        Assertions.assertEquals(0, listN.get(0).getFlags());

        Assertions.assertEquals(SatelliteSystem.BEIDOU, listB.get(0).getSystem());
        Assertions.assertEquals(29, listB.get(0).getPrn());
        Assertions.assertEquals("CNVX", listB.get(0).getNavigationMessageType());
        Assertions.assertEquals(0.0,
                                new AbsoluteDate(2022, 10, 5, 2, 0, 0.0, TimeScalesFactory.getBDT()).durationFrom(listB.get(0).getTransmitTime()),
                                1.0e-15);
        Assertions.assertEquals( 2.662500000000e+01, Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(listB.get(0).getAlpha()[0]), 1.0e-16);
        Assertions.assertEquals(-1.250000000000e-01, Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(listB.get(0).getAlpha()[1]), 1.0e-16);
        Assertions.assertEquals( 9.875000000000e+00, Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(listB.get(0).getAlpha()[2]), 1.0e-16);
        Assertions.assertEquals( 8.375000000000e+00, Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(listB.get(0).getAlpha()[3]), 1.0e-16);
        Assertions.assertEquals(-1.025000000000e+01, Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(listB.get(0).getAlpha()[4]), 1.0e-10);
        Assertions.assertEquals( 8.750000000000e-01, Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(listB.get(0).getAlpha()[5]), 1.0e-10);
        Assertions.assertEquals( 3.750000000000e-01, Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(listB.get(0).getAlpha()[6]), 1.0e-10);
        Assertions.assertEquals( 2.125000000000e+00, Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(listB.get(0).getAlpha()[7]), 1.0e-10);
        Assertions.assertEquals( 1.000000000000e+00, Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(listB.get(0).getAlpha()[8]), 1.0e-10);

    }

    @Test
    public void testGPSRinex2() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/brdc0130.22n";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(2.00,                                 file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,             file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.GPS,                  file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("CCRINEXN V1.6.0 UX",                 file.getHeader().getProgramName());
        Assertions.assertEquals("CDDIS",                              file.getHeader().getRunByName());
        Assertions.assertEquals(18,                                   file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0,  file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0,  file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0,  file.getSBASNavigationMessages().size());
        Assertions.assertEquals(3,  file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGPSCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getSystemTimeOffsets().size());
        Assertions.assertEquals(0,  file.getEarthOrientationParameters().size());
        Assertions.assertEquals(0,  file.getKlobucharMessages().size());
        Assertions.assertEquals(0,  file.getNequickGMessages().size());
        Assertions.assertEquals(0,  file.getBDGIMMessages().size());

        Assertions.assertEquals(0, file.getKlobucharMessages().size());
        Assertions.assertEquals(0, file.getNequickGMessages().size());
        Assertions.assertEquals(0, file.getBDGIMMessages().size());
        Assertions.assertEquals( 0.1118e-07, file.getKlobucharAlpha()[0], 1.0e-20);
        Assertions.assertEquals(-0.7451e-08, file.getKlobucharAlpha()[1], 1.0e-20);
        Assertions.assertEquals(-0.5960e-07, file.getKlobucharAlpha()[2], 1.0e-20);
        Assertions.assertEquals( 0.1192e-06, file.getKlobucharAlpha()[3], 1.0e-20);
        Assertions.assertEquals( 0.1147e+06, file.getKlobucharBeta()[0],  1.0e-7);
        Assertions.assertEquals(-0.1638e+06, file.getKlobucharBeta()[1],  1.0e-7);
        Assertions.assertEquals(-0.1966e+06, file.getKlobucharBeta()[2],  1.0e-7);
        Assertions.assertEquals( 0.9175e+06, file.getKlobucharBeta()[3],  1.0e-7);

        List<GPSLegacyNavigationMessage> list02 = file.getGPSLegacyNavigationMessages("G02");
        Assertions.assertEquals(3, list02.size());

        Assertions.assertEquals(0.0,
                                list02.get(0).getDate().durationFrom(new AbsoluteDate(2022, 1, 13, 0, 0, 0.0,
                                                                                      TimeScalesFactory.getGPS())),
                                1.0e-10);
        Assertions.assertEquals(50,              list02.get(0).getIODE());
        Assertions.assertEquals(0.0206718930276, list02.get(0).getE());
        Assertions.assertEquals(-1.37437210544,  list02.get(0).getOmega0());

    }

    @Test
    public void testGlonassRinex2() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/navigation/brdc0130.22g";
        final RinexNavigation file = new RinexNavigationParser().
                        parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));

        // Verify Header
        Assertions.assertEquals(2.01,                                 file.getHeader().getFormatVersion(), Double.MIN_VALUE);
        Assertions.assertEquals(RinexFileType.NAVIGATION,             file.getHeader().getFileType());
        Assertions.assertEquals(SatelliteSystem.GLONASS,              file.getHeader().getSatelliteSystem());
        Assertions.assertEquals("CCRINEXG V1.4 UX",                   file.getHeader().getProgramName());
        Assertions.assertEquals("CDDIS",                              file.getHeader().getRunByName());
        Assertions.assertEquals(18,                                   file.getHeader().getNumberOfLeapSeconds());

        // Verify data
        Assertions.assertEquals(0,  file.getGalileoNavigationMessages().size());
        Assertions.assertEquals(0,  file.getQZSSLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getQZSSCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getBeidouLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getBeidouCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getIRNSSNavigationMessages().size());
        Assertions.assertEquals(23, file.getGlonassNavigationMessages().size());
        Assertions.assertEquals(0,  file.getSBASNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGPSLegacyNavigationMessages().size());
        Assertions.assertEquals(0,  file.getGPSCivilianNavigationMessages().size());
        Assertions.assertEquals(0,  file.getSystemTimeOffsets().size());
        Assertions.assertEquals(0,  file.getEarthOrientationParameters().size());
        Assertions.assertEquals(0,  file.getKlobucharMessages().size());
        Assertions.assertEquals(0,  file.getNequickGMessages().size());
        Assertions.assertEquals(0,  file.getBDGIMMessages().size());

        Assertions.assertEquals(0, file.getKlobucharMessages().size());
        Assertions.assertEquals(0, file.getNequickGMessages().size());
        Assertions.assertEquals(0, file.getBDGIMMessages().size());

        List<GLONASSNavigationMessage> list04 = file.getGlonassNavigationMessages("R04");
        Assertions.assertEquals(48, list04.size());

        Assertions.assertEquals(0.0,
                                list04.get(0).getDate().durationFrom(new AbsoluteDate(2022, 1, 13, 0, 15, 0.0,
                                                                                      TimeScalesFactory.getUTC())),
                                1.0e-10);
        Assertions.assertEquals(-995521.484375,              list04.get(0).getX(),       1.0e-6);
        Assertions.assertEquals(-3089.79511261,              list04.get(0).getXDot(),    1.0e-9);
        Assertions.assertEquals(0.0,                         list04.get(0).getXDotDot(), 1.0e-12);
        Assertions.assertEquals(10825711.4258,               list04.get(0).getY(),       1.0e-6);
        Assertions.assertEquals(-648.876190186,              list04.get(0).getYDot(),    1.0e-9);
        Assertions.assertEquals(-0.0,                        list04.get(0).getYDotDot(), 1.0e-12);
        Assertions.assertEquals(23099867.6758,               list04.get(0).getZ(),       1.0e-6);
        Assertions.assertEquals(169.882774353,               list04.get(0).getZDot(),    1.0e-9);
        Assertions.assertEquals(-1.86264514923e-06,          list04.get(0).getZDotDot(), 1.0e-12);

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
        final String ex = "/gnss/navigation/unknown-rinex-version.n";
        try {
            new RinexNavigationParser().
                            parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT_VERSION, oe.getSpecifier());
            Assertions.assertEquals(9.99,  ((Double) oe.getParts()[0]).doubleValue(), 1.0e-10);
            Assertions.assertEquals(ex, oe.getParts()[1]);
        }
    }

    @Test
    public void testUnknownEphemeris() throws IOException {
        try {
            final String ex = "/gnss/navigation/unknown-ephemeris.n";
            new RinexNavigationParser().
                            parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_SATELLITE_SYSTEM, oe.getSpecifier());
            Assertions.assertEquals('Ω',  oe.getParts()[0]);
        }
    }

    @Test
    public void testDefensiveProgrammingExceptions() {
        // this test is really only meant to increase coverage with some reflection black magic
        // the methods tested here should not be called directly: they are overridden in concrete parsers
        try {

            // create ParseInfo
            final RinexNavigationParser rnp = new RinexNavigationParser();
            Class<?> parseInfoClass = null;
            for (Class<?> c : RinexNavigationParser.class.getDeclaredClasses()) {
                if (c.getName().endsWith("ParseInfo")) {
                    parseInfoClass = c;
                }
            }
            Constructor<?> ctr = parseInfoClass.getDeclaredConstructor(RinexNavigationParser.class, String.class);
            Object parseInfo = ctr.newInstance(rnp, "");

            Class<?> parserClass = null;
            for (Class<?> c : RinexNavigationParser.class.getDeclaredClasses()) {
                if (c.getName().endsWith("SatelliteSystemLineParser")) {
                    parserClass = c;
                }
            }

            // we select SBAS because it implements only the first 3 methods
            final Field sbasParserField = parserClass.getDeclaredField("SBAS");

            // get the methods inherited from base class
            for (String methodName : Arrays.asList("parseFourthBroadcastOrbit",
                                                   "parseFifthBroadcastOrbit",
                                                   "parseSixthBroadcastOrbit",
                                                   "parseSeventhBroadcastOrbit",
                                                   "parseEighthBroadcastOrbit",
                                                   "parseNinthBroadcastOrbit")) {
                Method m = parserClass.getMethod(methodName, String.class, parseInfoClass);
                m.setAccessible(true);
                try {
                    // call the method, triggering the internal error exception
                    m.invoke(sbasParserField.get(null), "", parseInfo);
                    Assertions.fail("an exception should have been thrown");
                } catch (InvocationTargetException e) {
                    Assertions.assertTrue(e.getCause() instanceof OrekitInternalError);
                }
            }

        } catch (NoSuchFieldException | NoSuchMethodException | SecurityException |
                 IllegalAccessException | IllegalArgumentException | InvocationTargetException |
                 InstantiationException e) {
            Assertions.fail(e.getLocalizedMessage());
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

    @Test
    public void testWrongTypeBeidou() throws IOException {
        doTestWrongType("/gnss/navigation/wrong-type-Beidou.n");
    }

    @Test
    public void testWrongTypeGalileo() throws IOException {
        doTestWrongType("/gnss/navigation/wrong-type-Galileo.n");
    }

    @Test
    public void testWrongTypeGlonass() throws IOException {
        doTestWrongType("/gnss/navigation/wrong-type-Glonass.n");
    }

    @Test
    public void testWrongTypeGPS() throws IOException {
        doTestWrongType("/gnss/navigation/wrong-type-GPS.n");
    }

    @Test
    public void testWrongTypeIRNSS() throws IOException {
        doTestWrongType("/gnss/navigation/wrong-type-IRNSS.n");
    }

    @Test
    public void testWrongTypeQZSS() throws IOException {
        doTestWrongType("/gnss/navigation/wrong-type-QZSS.n");
    }

    @Test
    public void testWrongTypeSBAS() throws IOException {
        doTestWrongType("/gnss/navigation/wrong-type-SBAS.n");
    }

    private void doTestWrongType(final String ex) throws IOException {
        try {
            new RinexNavigationParser().
                            parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertTrue(((String) oe.getParts()[2]).endsWith("XXXX"));
        }
    }

    @Test
    public void testMissingRunBy305() throws IOException {
        final String ex = "/gnss/navigation/missing-run-by-305.n";
        try {
            new RinexNavigationParser().
                            parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testMissingRunBy400() throws IOException {
        final String ex = "/gnss/navigation/missing-run-by-400.n";
        try {
            new RinexNavigationParser().
                            parse(new DataSource(ex, () -> getClass().getResourceAsStream(ex)));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPLETE_HEADER, oe.getSpecifier());
            Assertions.assertEquals(ex, oe.getParts()[0]);
        }
    }

}
