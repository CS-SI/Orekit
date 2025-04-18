/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.sinex;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sinex.Station.ReferenceSystem;
import org.orekit.gnss.GnssSignal;
import org.orekit.gnss.PredefinedGnssSignal;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.models.earth.displacement.PsdCorrection;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeSpanMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SinexParserTest {

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testSmallIGSSinexFile() {

        final Sinex sinex = load("/sinex/cod20842-small.snx");

        assertEquals(2, sinex.getStations().size());

        checkStation(sinex.getStations().get("ABMF"), 2019, 350, 0.0, 2019, 352, 86370, 2019, 351, 43185,
                     "ABMF", "97103M001", Vector3D.ZERO,
                     new Vector3D(0.291978579235962e07, -0.538374495897593e07, 0.177460486102077e07),
                     Vector3D.ZERO, "TRM57971.00", AntennaKey.OTHER_RADOME_CODE, AntennaKey.ANY_SERIAL_NUMBER);

        checkStation(sinex.getStations().get("ABPO"), 2019, 350, 0.0, 2019, 352, 86370, 2019, 351, 43185,
                     "ABPO", "33302M001", new Vector3D(0.0083, 0., 0.),
                     new Vector3D(0.409721654480569e07, 0.442911920899428e07, -.206577118054971e07),
                     Vector3D.ZERO, "ASH701945G_M", "SCIT", AntennaKey.ANY_SERIAL_NUMBER);

    }

    @Test
    public void testSLRSinexFile() {

        final Sinex sinex = load("/sinex/SLRF2008_150928_2015.09.28.snx");

        // Test date computation using format description
        ParseInfo<AbstractSinex> parseInfo = new ParseInfo<AbstractSinex>(TimeScalesFactory.getTimeScales()) {
            /** {@inheritDoc} */
            @Override
            protected AbstractSinex build() {
                return null;
            }
        };
        parseInfo.setTimeScale(TimeScalesFactory.getUTC());
        final AbsoluteDate date    = parseInfo.stringEpochToAbsoluteDate("95:120:86399", false);
        final AbsoluteDate refDate = new AbsoluteDate("1995-04-30T23:59:59.000", TimeScalesFactory.getUTC());
        assertEquals(0., refDate.durationFrom(date), 0.);

        // Test some values
        checkStation(sinex.getStations().get("1885"), 1996, 310, 71317, 1996, 320, 73221, 2005, 1, 0.,
                     "1885", "12302S006", null,
                     new Vector3D(0.318389220590831e07, 0.142146588920043e07, 0.532281398355808e07),
                     new Vector3D(-.239370506815545e-01 / Constants.JULIAN_YEAR,
                                  0.114173567092327e-01 / Constants.JULIAN_YEAR,
                                  -.145139658580209e-02 / Constants.JULIAN_YEAR),
                     null, null, null);

        checkStation(sinex.getStations().get("7082"), 1983, 313, 13398, 1984, 4, 83080, 2005, 1, 0.,
                     "7082", "40438M001", null,
                     new Vector3D(-.173599736285899e07, -.442504854754010e07, 0.424143058893134e07),
                     new Vector3D(-.142509359401051e-01 / Constants.JULIAN_YEAR,
                                  -.975043019205914e-02 / Constants.JULIAN_YEAR,
                                  -.506419781207987e-03 / Constants.JULIAN_YEAR),
                     null, null, null);
    }

    @Test
    public void testStationEccentricityXYZFile() {

        // Load file (it corresponds to a small version of the real complete file)
        final Sinex sinex = load("/sinex/ecc_xyz-small.snx");
        assertEquals(3, sinex.getStations().size());

        // Reference values
        final Vector3D ecc1148 = Vector3D.ZERO;
        final Vector3D ecc7035 = new Vector3D(-0.9670, -1.9490, 1.3990);
        final Vector3D ecc7120 = new Vector3D(-3.0850, -1.3670, 1.2620);

        final AbsoluteDate interm1148 = new AbsoluteDate(1990, 2, 14, TimeScalesFactory.getGPS());
        final AbsoluteDate interm7035 = new AbsoluteDate(1988, 7,  8, TimeScalesFactory.getGPS());
        final AbsoluteDate interm7120 = new AbsoluteDate(1981, 9, 26, TimeScalesFactory.getGPS());

        // Verify
        assertEquals(ReferenceSystem.XYZ, sinex.getStations().get("1148").getEccRefSystem());
        assertEquals(0., ecc1148.distance(sinex.getStations().get("1148").getEccentricities(interm1148)), 1.0e-15);
        assertEquals(ReferenceSystem.XYZ, sinex.getStations().get("7035").getEccRefSystem());
        assertEquals(0., ecc7035.distance(sinex.getStations().get("7035").getEccentricities(interm7035)), 1.0e-15);
        assertEquals(ReferenceSystem.XYZ, sinex.getStations().get("7120").getEccRefSystem());
        assertEquals(0., ecc7120.distance(sinex.getStations().get("7120").getEccentricities(interm7120)), 1.0e-15);

    }

    @Test
    public void testStationEccentricityUNEFile() {

        // Load file (it corresponds to a small version of the real complete file)
        final Sinex sinex = load("/sinex/ecc_une-small.snx");
        assertEquals(3, sinex.getStations().size());

        // Reference values
        final Vector3D ecc1148 = Vector3D.ZERO;
        final Vector3D ecc7035 = new Vector3D(2.5870, 0.0060, 0.0170);
        final Vector3D ecc7120 = new Vector3D(3.6020, -0.0130, 0.0090);

        final AbsoluteDate interm1148 = new AbsoluteDate(1990, 2, 14, TimeScalesFactory.getGPS());
        final AbsoluteDate interm7035 = new AbsoluteDate(1988, 7,  8, TimeScalesFactory.getGPS());
        final AbsoluteDate interm7120 = new AbsoluteDate(1981, 9, 26, TimeScalesFactory.getGPS());

        // Verify
        assertEquals(ReferenceSystem.UNE, sinex.getStations().get("1148").getEccRefSystem());
        assertEquals(0., ecc1148.distance(sinex.getStations().get("1148").getEccentricities(interm1148)), 1.0e-15);
        assertEquals(ReferenceSystem.UNE, sinex.getStations().get("7035").getEccRefSystem());
        assertEquals(0., ecc7035.distance(sinex.getStations().get("7035").getEccentricities(interm7035)), 1.0e-15);
        assertEquals(ReferenceSystem.UNE, sinex.getStations().get("7120").getEccRefSystem());
        assertEquals(0., ecc7120.distance(sinex.getStations().get("7120").getEccentricities(interm7120)), 1.0e-15);

    }

    @Test
    public void testIssue867() {

        // Load file (it corresponds to a small version of the real complete file)
        final Sinex sinex = load("/sinex/ecc_xyz-small-multiple-ecc.snx");
        assertEquals(4, sinex.getStations().size());

        // Verify station 7236
        final Station  station7236    = sinex.getStations().get("7236");
        final Vector3D refStation7236 = Vector3D.ZERO;
        assertEquals(0.0, refStation7236.distance(station7236.getEccentricities(new AbsoluteDate("1995-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, station7236.getEccentricitiesTimeSpanMap().getFirstTransition().getDate().durationFrom(new AbsoluteDate("1988-01-01T00:00:00.000", TimeScalesFactory.getUTC())), 1.0e-15);
        assertEquals(0.0, station7236.getEccentricitiesTimeSpanMap().getLastTransition().getDate().durationFrom(new AbsoluteDate("1999-09-30T23:59:59.000", TimeScalesFactory.getUTC())), 1.0e-15);

        // Verify station 7237
        final Station station7237 = sinex.getStations().get("7237");
        final Vector3D refStation7237 = Vector3D.ZERO;
        assertEquals(0.0, refStation7237.distance(station7237.getEccentricities(new AbsoluteDate("1995-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7237.distance(station7237.getEccentricities(new AbsoluteDate("2021-12-06T17:30:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7237.distance(station7237.getEccentricities(new AbsoluteDate("2999-12-06T17:30:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, station7237.getEccentricitiesTimeSpanMap().getFirstTransition().getDate().durationFrom(new AbsoluteDate("1988-01-01T00:00:00.000", TimeScalesFactory.getUTC())), 1.0e-15);
        Assertions.assertSame(station7237.getEccentricitiesTimeSpanMap().getLastTransition().getDate(), AbsoluteDate.FUTURE_INFINITY);

        // Verify station 7090
        final Station station7090 = sinex.getStations().get("7090");
        Vector3D refStation7090 = new Vector3D(-1.2030, 2.5130, -1.5440);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1982-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1984-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1985-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1986-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1987-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.1990, 2.5070, -1.5400);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1988-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1990-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1991-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1992-01-01T12:00:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2060, 2.5010, -1.5530);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1992-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1995-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1998-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2048, 2.5019, -1.5516);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2002-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2058, 2.5026, -1.5522);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2005-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2069, 2.5034, -1.5505);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2008-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2043, 2.5040, -1.5509);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2012-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2073, 2.5034, -1.5509);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2015-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);

        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2021-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2999-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, station7090.getEccentricitiesTimeSpanMap().getFirstTransition().getDate().durationFrom(new AbsoluteDate("1979-07-01T00:00:00.000", TimeScalesFactory.getUTC())), 1.0e-15);

        Assertions.assertSame(station7090.getEccentricitiesTimeSpanMap().getLastTransition().getDate(), AbsoluteDate.FUTURE_INFINITY);

        // Verify station 7092
        final Station station7092 = sinex.getStations().get("7092");
        Vector3D refStation7092 = new Vector3D(-3.0380, 0.6290, 0.4980);
        assertEquals(0.0, refStation7092.distance(station7092.getEccentricities(new AbsoluteDate("1980-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        assertEquals(0.0, station7092.getEccentricitiesTimeSpanMap().getFirstTransition().getDate().durationFrom(new AbsoluteDate("1979-08-15T00:00:00.000", TimeScalesFactory.getUTC())), 1.0e-15);
        assertEquals(0.0, station7092.getEccentricitiesTimeSpanMap().getLastTransition().getDate().durationFrom(new AbsoluteDate("1980-10-31T23:59:59.000", TimeScalesFactory.getUTC())), 1.0e-15);

    }

    @Test
    public void testIssue1149() {
        final Sinex sinex = load("/sinex/JAX0MGXFIN_20202440000_01D_000_SOL.SNX");
        assertEquals(133, sinex.getStations().size());
    }

    @Test
    public void testIssue1649A() {
        final Sinex sinex = load("/sinex/JAX0MGXFIN_20202440000_01D_000_SOL.SNX");
        assertEquals(133, sinex.getStations().size());

        // phase centers correspond to antenna ASH700936D_M    SNOW CR143
        // which is replaced by antenna ASH700936D_M    SNOW -----
        final Map<GnssSignal, Vector3D> phaseCenters =
            sinex.getStations().get("DRAG").getPhaseCenters(sinex.getFileEpochStartTime());
        assertEquals(2, phaseCenters.size());

        // check GPS phase centers
        Vector3D pc01 = phaseCenters.get(PredefinedGnssSignal.G01);
        assertEquals( 0.0909, pc01.getX(), 1.0e-15);
        assertEquals( 0.0003, pc01.getY(), 1.0e-15);
        assertEquals(-0.0002, pc01.getZ(), 1.0e-15);
        Vector3D pc02 = phaseCenters.get(PredefinedGnssSignal.G02);
        assertEquals( 0.1192, pc02.getX(), 1.0e-15);
        assertEquals( 0.0001, pc02.getY(), 1.0e-15);
        assertEquals( 0.0001, pc02.getZ(), 1.0e-15);

    }

    @Test
    public void testIssue1649B() {
        final Sinex sinex = load("/sinex/ESA0OPSFIN_20241850000_01D_01D_SOL.SNX");
        assertEquals(150, sinex.getStations().size());

        // phase centers correspond to antenna JAVRINGANT_DM   SCIS 00842
        // which is present in the file with exact matching
        final Map<GnssSignal, Vector3D> stationsPhaseCenters =
            sinex.getStations().get("GUAM").getPhaseCenters(sinex.getFileEpochStartTime());
        assertEquals(7, stationsPhaseCenters.size());

        // check GPS phase centers
        Vector3D pc01 = stationsPhaseCenters.get(PredefinedGnssSignal.G01);
        assertEquals( 0.0859, pc01.getX(), 1.0e-15);
        assertEquals( 0.0003, pc01.getY(), 1.0e-15);
        assertEquals( 0.0015, pc01.getZ(), 1.0e-15);
        Vector3D pc02 = stationsPhaseCenters.get(PredefinedGnssSignal.G02);
        assertEquals( 0.1166, pc02.getX(), 1.0e-15);
        assertEquals( 0.0002, pc02.getY(), 1.0e-15);
        assertEquals( 0.0001, pc02.getZ(), 1.0e-15);

        // check GALILEO phase centers
        Vector3D pcE01 = stationsPhaseCenters.get(PredefinedGnssSignal.E01);
        assertEquals( 0.0859, pcE01.getX(), 1.0e-15);
        assertEquals( 0.0003, pcE01.getY(), 1.0e-15);
        assertEquals( 0.0015, pcE01.getZ(), 1.0e-15);
        Vector3D pcE05 = stationsPhaseCenters.get(PredefinedGnssSignal.E05);
        assertEquals( 0.1247, pcE05.getX(), 1.0e-15);
        assertEquals( 0.0003, pcE05.getY(), 1.0e-15);
        assertEquals( 0.0000, pcE05.getZ(), 1.0e-15);
        Vector3D pcE06 = stationsPhaseCenters.get(PredefinedGnssSignal.E06);
        assertEquals( 0.1112, pcE06.getX(), 1.0e-15);
        assertEquals( 0.0001, pcE06.getY(), 1.0e-15);
        assertEquals( 0.0003, pcE06.getZ(), 1.0e-15);
        Vector3D pcE07 = stationsPhaseCenters.get(PredefinedGnssSignal.E07);
        assertEquals( 0.1189, pcE07.getX(), 1.0e-15);
        assertEquals( 0.0003, pcE07.getY(), 1.0e-15);
        assertEquals( 0.0001, pcE07.getZ(), 1.0e-15);
        Vector3D pcE08 = stationsPhaseCenters.get(PredefinedGnssSignal.E08);
        assertEquals( 0.1247, pcE08.getX(), 1.0e-15);
        assertEquals( 0.0003, pcE08.getY(), 1.0e-15);
        assertEquals( 0.0000, pcE08.getZ(), 1.0e-15);

        // check satellite phase centers
        final Map<SatInSystem, Map<GnssSignal, Vector3D>> satellitesPhaseCenters =
            sinex.getSatellitesPhaseCenters();
        assertEquals(78, satellitesPhaseCenters.size());
        final Map<GnssSignal, Vector3D> phaseCentersE223 =
            satellitesPhaseCenters.get(new SatInSystem(SatelliteSystem.GALILEO, 223));
        assertEquals(2, phaseCentersE223.size());
        final Vector3D pc022301 = phaseCentersE223.get(PredefinedGnssSignal.E01);
        assertEquals( 0.1222, pc022301.getX(), 1.0e-15);
        assertEquals(-0.0109, pc022301.getY(), 1.0e-15);
        assertEquals( 0.7296, pc022301.getZ(), 1.0e-15);
        final Vector3D pc022305 = phaseCentersE223.get(PredefinedGnssSignal.E05);
        assertEquals( 0.1240, pc022305.getX(), 1.0e-15);
        assertEquals(-0.0109, pc022305.getY(), 1.0e-15);
        assertEquals( 0.6065, pc022305.getZ(), 1.0e-15);

    }

    @Test
    public void testUnknownGnssAntenna() {
        try {
            load("/sinex/cod-unknown-GNSS-antenna.snx");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNKNOWN_GNSS_ANTENNA, oe.getSpecifier());
            assertEquals("ASH701945G_M", oe.getParts()[0]);
            assertEquals("SCIT",         oe.getParts()[1]);
            assertEquals("12345",        oe.getParts()[2]);
        }
    }

    @Test
    public void testCorruptedHeader() {
        try {
            load("/sinex/corrupted-header.snx");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testNoDataForEpoch() {

        // Load file (it corresponds to a small version of the real complete file)
        final Sinex sinex = load("/sinex/ecc_xyz-small-multiple-ecc.snx");

        // Station 7236
        final Station station7236 = sinex.getStations().get("7236");

        // Epoch of exception
        final AbsoluteDate exceptionEpoch = new AbsoluteDate("1987-01-11T00:00:00.000", TimeScalesFactory.getUTC());

        // Test the exception
        try {
            station7236.getEccentricities(exceptionEpoch);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.MISSING_STATION_DATA_FOR_EPOCH, oe.getSpecifier());
        }

        // Test the exception
        try {
            station7236.getAntennaKey(exceptionEpoch);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.MISSING_STATION_DATA_FOR_EPOCH, oe.getSpecifier());
        }

    }

    @Test
    public void testIssue1150A() {
        final Sinex sinex = load("/sinex/JAX0MGXFIN_20202440000_01D_000_SOL.SNX");
        final AbsoluteDate date   = new AbsoluteDate(2020, 8, 31, 12, 0, 0.0, TimeScalesFactory.getGPS());
        final Vector3D     ecc    = sinex.getStations().get("ABPO").getEccentricities(date);
        assertEquals(0.0083, ecc.getX(), 1.0e-10);
        assertEquals(0.0000, ecc.getY(), 1.0e-10);
        assertEquals(0.0000, ecc.getZ(), 1.0e-10);
    }

    @Test
    public void testIssue1150B() {

        // Load file
        final Sinex sinex = load("/sinex/issue1150.snx");

        // Verify start epoch for station "1148" is equal to the file start epoch
        final Station station1148 = sinex.getStations().get("1148");
        assertEquals(0.0, sinex.getFileEpochStartTime().durationFrom(station1148.getEccentricitiesTimeSpanMap().getFirstTransition().getDate()));

        // Verify end epoch for station "7035" is equal future infinity
        final Station station7035 = sinex.getStations().get("7035");
        Assertions.assertSame(station7035.getEccentricitiesTimeSpanMap().getLastTransition().getDate(), AbsoluteDate.FUTURE_INFINITY);

        // Verify start epoch for station "7120" is equal to the file start epoch
        final Station station7120 = sinex.getStations().get("7120");
        assertEquals(0.0, sinex.getFileEpochStartTime().durationFrom(station7120.getEccentricitiesTimeSpanMap().getFirstTransition().getDate()));
        Assertions.assertSame(station7120.getEccentricitiesTimeSpanMap().getLastTransition().getDate(), AbsoluteDate.FUTURE_INFINITY);

    }

    @Test
    public void testCorruptedFile() {
        try {
            load("/sinex/cod20842-corrupted.snx");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            assertEquals(52, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testUnknownFrequencyCode() {
        try {
            load("/sinex/unknown-frequency-code.snx");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNKNOWN_GNSS_FREQUENCY, oe.getSpecifier());
            assertEquals(SatelliteSystem.GLONASS, oe.getParts()[0]);
            assertEquals(7, oe.getParts()[1]);
            assertEquals(90, (Integer) oe.getParts()[2]);
            assertTrue(((String) oe.getParts()[3]).endsWith("unknown-frequency-code.snx"));
        }
    }

    @Test
    public void testPostSeismicDeformation() {
        final Sinex sinex = load("/sinex/ITRF2020-psd-gnss.snx");

        // 2010-02-27 06:34:16 https://earthquake.usgs.gov/earthquakes/eventpage/official20100227063411530_30/executive
        final AbsoluteDate date2010 = new AbsoluteDate(2010, 2, 27, 6, 34, 16.0, TimeScalesFactory.getUTC());
        final TimeSpanMap<List<PsdCorrection>> psdAntuco = sinex.getStations().get("ANTC").getPsdTimeSpanMap();
        assertEquals(2, psdAntuco.getSpansNumber());
        final List<PsdCorrection> corr2010 = psdAntuco.getFirstNonNullSpan().getData();
        assertEquals(5, corr2010.size());
        assertEquals(0, corr2010.get(0).getEarthquakeDate().durationFrom(date2010));
        assertEquals(PsdCorrection.TimeEvolution.LOG, corr2010.get(0).getEvolution());
        assertEquals(PsdCorrection.Axis.EAST, corr2010.get(0).getAxis());
        assertEquals(-1.28699198121674e-01, corr2010.get(0).getAmplitude(), 1.0e-14);
        assertEquals(8.08455225832410e-01, corr2010.get(0).getRelaxationTime() / Constants.JULIAN_YEAR, 1.0e-14);
        assertEquals(0, corr2010.get(1).getEarthquakeDate().durationFrom(date2010));
        assertEquals(PsdCorrection.TimeEvolution.LOG, corr2010.get(1).getEvolution());
        assertEquals(PsdCorrection.Axis.EAST, corr2010.get(1).getAxis());
        assertEquals(-3.56937459818481e-02, corr2010.get(1).getAmplitude(), 1.0e-14);
        assertEquals(3.53677247694474e-03, corr2010.get(1).getRelaxationTime() / Constants.JULIAN_YEAR, 1.0e-14);
        assertEquals(0, corr2010.get(2).getEarthquakeDate().durationFrom(date2010));
        assertEquals(PsdCorrection.TimeEvolution.LOG, corr2010.get(2).getEvolution());
        assertEquals(PsdCorrection.Axis.NORTH, corr2010.get(2).getAxis());
        assertEquals(8.76938710916818e-02, corr2010.get(2).getAmplitude(), 1.0e-14);
        assertEquals(6.74719810025941e+00, corr2010.get(2).getRelaxationTime() / Constants.JULIAN_YEAR, 1.0e-14);
        assertEquals(0, corr2010.get(3).getEarthquakeDate().durationFrom(date2010));
        assertEquals(PsdCorrection.TimeEvolution.LOG, corr2010.get(3).getEvolution());
        assertEquals(PsdCorrection.Axis.NORTH, corr2010.get(3).getAxis());
        assertEquals(1.23511869822841e-02, corr2010.get(3).getAmplitude(), 1.0e-14);
        assertEquals(1.72868196121241e-02, corr2010.get(3).getRelaxationTime() / Constants.JULIAN_YEAR, 1.0e-14);
        assertEquals(0, corr2010.get(4).getEarthquakeDate().durationFrom(date2010));
        assertEquals(PsdCorrection.TimeEvolution.LOG, corr2010.get(4).getEvolution());
        assertEquals(PsdCorrection.Axis.UP, corr2010.get(4).getAxis());
        assertEquals(5.50435552310340e-02, corr2010.get(4).getAmplitude(), 1.0e-14);
        assertEquals(4.70992312239571e-01, corr2010.get(4).getRelaxationTime() / Constants.JULIAN_YEAR, 1.0e-14);

        // 2013-08-30T16:25:03 https://earthquake.usgs.gov/earthquakes/eventpage/usb000jdt7/executive
        // 2016-03-12T18:06:44 https://earthquake.usgs.gov/earthquakes/eventpage/at00o3xubc/executive
        final AbsoluteDate date2013 = new AbsoluteDate(2013, 8, 30, 16, 25,  3.0, TimeScalesFactory.getUTC());
        final AbsoluteDate date2016 = new AbsoluteDate(2016, 3, 12, 18,  6, 44.0, TimeScalesFactory.getUTC());
        final TimeSpanMap<List<PsdCorrection>> psdAtkaIsland = sinex.getStations().get("AB01").getPsdTimeSpanMap();
        assertEquals(3, psdAtkaIsland.getSpansNumber());
        final List<PsdCorrection> corr2013 = psdAtkaIsland.getFirstNonNullSpan().getData();
        assertEquals(1, corr2013.size());
        assertEquals(0, corr2013.get(0).getEarthquakeDate().durationFrom(date2013));
        assertEquals(PsdCorrection.TimeEvolution.EXP, corr2013.get(0).getEvolution());
        assertEquals(PsdCorrection.Axis.NORTH, corr2013.get(0).getAxis());
        assertEquals(-1.16779196624443e-02, corr2013.get(0).getAmplitude(), 1.0e-14);
        assertEquals(5.02510982822891e-01, corr2013.get(0).getRelaxationTime() / Constants.JULIAN_YEAR, 1.0e-14);
        final List<PsdCorrection> corr2016 = psdAtkaIsland.getFirstNonNullSpan().next().getData();
        assertEquals(1, corr2016.size());
        assertEquals(0, corr2016.get(0).getEarthquakeDate().durationFrom(date2016));
        assertEquals(PsdCorrection.TimeEvolution.EXP, corr2016.get(0).getEvolution());
        assertEquals(PsdCorrection.Axis.NORTH, corr2016.get(0).getAxis());
        assertEquals(-1.31981162574364e-02, corr2016.get(0).getAmplitude(), 1.0e-14);
        assertEquals(1.02131561331021e+00, corr2016.get(0).getRelaxationTime() / Constants.JULIAN_YEAR, 1.0e-14);

    }

    private void checkStation(final Station station, final int startYear, final int startDay, final double secInStartDay,
                              final int endYear, final int endDay, final double secInEndDay,
                              final int epochYear, final int epochDay, final double secInEpoch,
                              final String siteCode, final String refDomes, final Vector3D refEcc,
                              final Vector3D refPos, final Vector3D refVel,
                              final String antennaName, final String radomeCode, final String serialNumber) {

        final AbsoluteDate start = new AbsoluteDate(new DateComponents(startYear, startDay),
                                                    new TimeComponents(secInStartDay),
                                                    TimeScalesFactory.getUTC());
        final AbsoluteDate end = new AbsoluteDate(new DateComponents(endYear, endDay),
                                                  new TimeComponents(secInEndDay),
                                                  TimeScalesFactory.getUTC());
        final AbsoluteDate epoch = new AbsoluteDate(new DateComponents(epochYear, epochDay),
                                                    new TimeComponents(secInEpoch),
                                                    TimeScalesFactory.getUTC());

        final AbsoluteDate midDate = start.shiftedBy(0.5 * end.durationFrom(start));

        assertEquals(0., start.durationFrom(station.getValidFrom()), 1.0e-10);
        assertEquals(0., end.durationFrom(station.getValidUntil()),  1.0e-10);
        assertEquals(0., epoch.durationFrom(station.getEpoch()),     1.0e-10);
        assertEquals(siteCode, station.getSiteCode());
        assertEquals(refDomes, station.getDomes());
        if (refEcc != null) {
            assertEquals(0., refEcc.distance(station.getEccentricities(midDate)), 1.0e-10);
        }
        assertEquals(0., refPos.distance(station.getPosition()), 1.0e-10);
        assertEquals(0., refVel.distance(station.getVelocity()), 1.0e-10);
        if (antennaName != null) {
            assertEquals(new AntennaKey(antennaName, radomeCode, serialNumber),
                                    station.getAntennaKey(midDate));
        }

    }

    private Sinex load(final String name) {
        return new SinexParser(TimeScalesFactory.getTimeScales()).
               parse(new DataSource(name, () -> SinexParserTest.class.getResourceAsStream(name)));
    }

}
