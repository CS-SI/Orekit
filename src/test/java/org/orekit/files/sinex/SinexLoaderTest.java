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
package org.orekit.files.sinex;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sinex.Station.ReferenceSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SinexLoaderTest {

    private TimeScale utc;

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss:sinex");
        utc = TimeScalesFactory.getUTC();
    }

    @Test
    public void testSmallIGSSinexFile() {

        SinexLoader loader = new SinexLoader("cod20842-small.snx");

        Assertions.assertEquals(2, loader.getStations().size());

        checkStation(loader.getStation("ABMF"), 2019, 350, 0.0, 2019, 352, 86370, 2019, 351, 43185,
                     "ABMF", "97103M001", Vector3D.ZERO,
                     new Vector3D(0.291978579235962e07, -0.538374495897593e07, 0.177460486102077e07),
                     Vector3D.ZERO, "TRM57971.00     NONE");

        checkStation(loader.getStation("ABPO"), 2019, 350, 0.0, 2019, 352, 86370, 2019, 351, 43185,
                     "ABPO", "33302M001", new Vector3D(0.0083, 0., 0.),
                     new Vector3D(0.409721654480569e07, 0.442911920899428e07, -.206577118054971e07),
                     Vector3D.ZERO, "ASH701945G_M    SCIT");

    }

    @Test
    public void testSLRSinexFile() {

        SinexLoader loader = new SinexLoader("SLRF2008_150928_2015.09.28.snx");

        // Test date computation using format description
        try {
            Method method = SinexLoader.class.getDeclaredMethod("stringEpochToAbsoluteDate", String.class, boolean.class, TimeScale.class);
            method.setAccessible(true);
            final AbsoluteDate date    = (AbsoluteDate) method.invoke(loader, "95:120:86399", false, utc);
            final AbsoluteDate refDate = new AbsoluteDate("1995-04-30T23:59:59.000", TimeScalesFactory.getUTC());
            Assertions.assertEquals(0., refDate.durationFrom(date), 0.);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
        }

        // Test some values
        checkStation(loader.getStation("1885"), 1996, 310, 71317, 1996, 320, 73221, 2005, 1, 0.,
                     "1885", "12302S006", null,
                     new Vector3D(0.318389220590831e07, 0.142146588920043e07, 0.532281398355808e07),
                     new Vector3D(-.239370506815545e-01 / Constants.JULIAN_YEAR,
                                  0.114173567092327e-01 / Constants.JULIAN_YEAR,
                                  -.145139658580209e-02 / Constants.JULIAN_YEAR),
                     null);

        checkStation(loader.getStation("7082"), 1983, 313, 13398, 1984, 4, 83080, 2005, 1, 0.,
                     "7082", "40438M001", null,
                     new Vector3D(-.173599736285899e07, -.442504854754010e07, 0.424143058893134e07),
                     new Vector3D(-.142509359401051e-01 / Constants.JULIAN_YEAR,
                                  -.975043019205914e-02 / Constants.JULIAN_YEAR,
                                  -.506419781207987e-03 / Constants.JULIAN_YEAR),
                     null);
    }

    @Test
    public void testStationEccentricityXYZFile() {

        // Load file (it corresponds to a small version of the real complete file)
        SinexLoader loader = new SinexLoader("ecc_xyz-small.snx");
        Assertions.assertEquals(3, loader.getStations().size());

        // Reference values
        final Vector3D ecc1148 = Vector3D.ZERO;
        final Vector3D ecc7035 = new Vector3D(-0.9670, -1.9490, 1.3990);
        final Vector3D ecc7120 = new Vector3D(-3.0850, -1.3670, 1.2620);

        final AbsoluteDate interm1148 = new AbsoluteDate(1990, 2, 14, TimeScalesFactory.getGPS());
        final AbsoluteDate interm7035 = new AbsoluteDate(1988, 7,  8, TimeScalesFactory.getGPS());
        final AbsoluteDate interm7120 = new AbsoluteDate(1981, 9, 26, TimeScalesFactory.getGPS());

        // Verify
        Assertions.assertEquals(ReferenceSystem.XYZ, loader.getStation("1148").getEccRefSystem());
        Assertions.assertEquals(0., ecc1148.distance(loader.getStation("1148").getEccentricities(interm1148)), 1.0e-15);
        Assertions.assertEquals(ReferenceSystem.XYZ, loader.getStation("7035").getEccRefSystem());
        Assertions.assertEquals(0., ecc7035.distance(loader.getStation("7035").getEccentricities(interm7035)), 1.0e-15);
        Assertions.assertEquals(ReferenceSystem.XYZ, loader.getStation("7120").getEccRefSystem());
        Assertions.assertEquals(0., ecc7120.distance(loader.getStation("7120").getEccentricities(interm7120)), 1.0e-15);

    }

    @Test
    public void testStationEccentricityUNEFile() {

        // Load file (it corresponds to a small version of the real complete file)
        SinexLoader loader = new SinexLoader("ecc_une-small.snx");
        Assertions.assertEquals(3, loader.getStations().size());

        // Reference values
        final Vector3D ecc1148 = Vector3D.ZERO;
        final Vector3D ecc7035 = new Vector3D(2.5870, 0.0060, 0.0170);
        final Vector3D ecc7120 = new Vector3D(3.6020, -0.0130, 0.0090);

        final AbsoluteDate interm1148 = new AbsoluteDate(1990, 2, 14, TimeScalesFactory.getGPS());
        final AbsoluteDate interm7035 = new AbsoluteDate(1988, 7,  8, TimeScalesFactory.getGPS());
        final AbsoluteDate interm7120 = new AbsoluteDate(1981, 9, 26, TimeScalesFactory.getGPS());

        // Verify
        Assertions.assertEquals(ReferenceSystem.UNE, loader.getStation("1148").getEccRefSystem());
        Assertions.assertEquals(0., ecc1148.distance(loader.getStation("1148").getEccentricities(interm1148)), 1.0e-15);
        Assertions.assertEquals(ReferenceSystem.UNE, loader.getStation("7035").getEccRefSystem());
        Assertions.assertEquals(0., ecc7035.distance(loader.getStation("7035").getEccentricities(interm7035)), 1.0e-15);
        Assertions.assertEquals(ReferenceSystem.UNE, loader.getStation("7120").getEccRefSystem());
        Assertions.assertEquals(0., ecc7120.distance(loader.getStation("7120").getEccentricities(interm7120)), 1.0e-15);

    }

    @Test
    public void testIssue867() {

        // Load file (it corresponds to a small version of the real complete file)
        SinexLoader loader = new SinexLoader("ecc_xyz-small-multiple-ecc.snx");
        Assertions.assertEquals(4, loader.getStations().size());

        // Verify station 7236
        final Station  station7236    = loader.getStation("7236");
        final Vector3D refStation7236 = Vector3D.ZERO;
        Assertions.assertEquals(0.0, refStation7236.distance(station7236.getEccentricities(new AbsoluteDate("1995-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, station7236.getEccentricitiesTimeSpanMap().getFirstTransition().getDate().durationFrom(new AbsoluteDate("1988-01-01T00:00:00.000", TimeScalesFactory.getUTC())), 1.0e-15);
        Assertions.assertEquals(0.0, station7236.getEccentricitiesTimeSpanMap().getLastTransition().getDate().durationFrom(new AbsoluteDate("1999-09-30T23:59:59.000", TimeScalesFactory.getUTC())), 1.0e-15);

        // Verify station 7237
        final Station station7237 = loader.getStation("7237");
        final Vector3D refStation7237 = Vector3D.ZERO;
        Assertions.assertEquals(0.0, refStation7237.distance(station7237.getEccentricities(new AbsoluteDate("1995-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7237.distance(station7237.getEccentricities(new AbsoluteDate("2021-12-06T17:30:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7237.distance(station7237.getEccentricities(new AbsoluteDate("2999-12-06T17:30:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, station7237.getEccentricitiesTimeSpanMap().getFirstTransition().getDate().durationFrom(new AbsoluteDate("1988-01-01T00:00:00.000", TimeScalesFactory.getUTC())), 1.0e-15);
        Assertions.assertTrue(station7237.getEccentricitiesTimeSpanMap().getLastTransition().getDate() == AbsoluteDate.FUTURE_INFINITY);

        // Verify station 7090
        final Station station7090 = loader.getStation("7090");
        Vector3D refStation7090 = new Vector3D(-1.2030, 2.5130, -1.5440);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1982-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1984-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1985-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1986-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1987-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.1990, 2.5070, -1.5400);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1988-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1990-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1991-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1992-01-01T12:00:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2060, 2.5010, -1.5530);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1992-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1995-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("1998-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2048, 2.5019, -1.5516);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2002-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2058, 2.5026, -1.5522);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2005-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2069, 2.5034, -1.5505);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2008-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2043, 2.5040, -1.5509);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2012-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        refStation7090 = new Vector3D(-1.2073, 2.5034, -1.5509);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2015-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);

        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2021-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, refStation7090.distance(station7090.getEccentricities(new AbsoluteDate("2999-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, station7090.getEccentricitiesTimeSpanMap().getFirstTransition().getDate().durationFrom(new AbsoluteDate("1979-07-01T00:00:00.000", TimeScalesFactory.getUTC())), 1.0e-15);

        Assertions.assertTrue(station7090.getEccentricitiesTimeSpanMap().getLastTransition().getDate() == AbsoluteDate.FUTURE_INFINITY);

        // Verify station 7092
        final Station station7092 = loader.getStation("7092");
        Vector3D refStation7092 = new Vector3D(-3.0380, 0.6290, 0.4980);
        Assertions.assertEquals(0.0, refStation7092.distance(station7092.getEccentricities(new AbsoluteDate("1980-07-05T07:50:00.000", TimeScalesFactory.getUTC()))), 1.0e-15);
        Assertions.assertEquals(0.0, station7092.getEccentricitiesTimeSpanMap().getFirstTransition().getDate().durationFrom(new AbsoluteDate("1979-08-15T00:00:00.000", TimeScalesFactory.getUTC())), 1.0e-15);
        Assertions.assertEquals(0.0, station7092.getEccentricitiesTimeSpanMap().getLastTransition().getDate().durationFrom(new AbsoluteDate("1980-10-31T23:59:59.000", TimeScalesFactory.getUTC())), 1.0e-15);

    }

    @Test
    public void testIssue1149() {
        SinexLoader loader = new SinexLoader("JAX0MGXFIN_20202440000_01D_000_SOL.SNX");
        Assertions.assertEquals(133, loader.getStations().size());
    }

    @Test
    public void testCorruptedHeader() {
        try {
            new SinexLoader("corrupted-header.snx");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testNoDataForEpoch() {

        // Load file (it corresponds to a small version of the real complete file)
        SinexLoader loader = new SinexLoader("ecc_xyz-small-multiple-ecc.snx");

        // Station 7236
        final Station station7236 = loader.getStation("7236");

        // Epoch of exception
        final AbsoluteDate exceptionEpoch = new AbsoluteDate("1987-01-11T00:00:00.000", TimeScalesFactory.getUTC());

        // Test the exception
        try {
            station7236.getEccentricities(exceptionEpoch);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.MISSING_STATION_DATA_FOR_EPOCH, oe.getSpecifier());
        }

        // Test the exception
        try {
            station7236.getAntennaType(exceptionEpoch);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.MISSING_STATION_DATA_FOR_EPOCH, oe.getSpecifier());
        }

    }

    @Test
    public void testIssue1150A() {
        SinexLoader        loader = new SinexLoader("JAX0MGXFIN_20202440000_01D_000_SOL.SNX");
        final AbsoluteDate date   = new AbsoluteDate(2020, 8, 31, 12, 0, 0.0, TimeScalesFactory.getGPS());
        final Vector3D     ecc    = loader.getStation("ABPO").getEccentricities(date);
        Assertions.assertEquals(0.0083, ecc.getX(), 1.0e-10);
        Assertions.assertEquals(0.0000, ecc.getY(), 1.0e-10);
        Assertions.assertEquals(0.0000, ecc.getZ(), 1.0e-10);
    }

    @Test
    public void testIssue1150B() {

        // Load file
        SinexLoader loader = new SinexLoader("issue1150.snx");

        // Verify start epoch for station "1148" is equal to the file start epoch
        final Station station1148 = loader.getStation("1148");
        Assertions.assertEquals(0.0, loader.getFileEpochStartTime().durationFrom(station1148.getEccentricitiesTimeSpanMap().getFirstTransition().getDate()));

        // Verify end epoch for station "7035" is equal future infinity
        final Station station7035 = loader.getStation("7035");
        Assertions.assertTrue(station7035.getEccentricitiesTimeSpanMap().getLastTransition().getDate() == AbsoluteDate.FUTURE_INFINITY);

        // Verify start epoch for station "7120" is equal to the file start epoch
        final Station station7120 = loader.getStation("7120");
        Assertions.assertEquals(0.0, loader.getFileEpochStartTime().durationFrom(station7120.getEccentricitiesTimeSpanMap().getFirstTransition().getDate()));
        Assertions.assertTrue(station7120.getEccentricitiesTimeSpanMap().getLastTransition().getDate() == AbsoluteDate.FUTURE_INFINITY);

    }

    @Test
    public void testCorruptedFile() {
        try {
            new SinexLoader("cod20842-corrupted.snx");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(52, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    private void checkStation(final Station station, final int startYear, final int startDay, final double secInStartDay,
                              final int endYear, final int endDay, final double secInEndDay,
                              final int epochYear, final int epochDay, final double secInEpoch,
                              final String siteCode, final String refDomes, final Vector3D refEcc,
                              final Vector3D refPos, final Vector3D refVel, final String antennaType) {

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

        Assertions.assertEquals(0., start.durationFrom(station.getValidFrom()), 1.0e-10);
        Assertions.assertEquals(0., end.durationFrom(station.getValidUntil()),  1.0e-10);
        Assertions.assertEquals(0., epoch.durationFrom(station.getEpoch()),     1.0e-10);
        Assertions.assertEquals(siteCode, station.getSiteCode());
        Assertions.assertEquals(refDomes, station.getDomes());
        if (refEcc != null) {
            Assertions.assertEquals(0., refEcc.distance(station.getEccentricities(midDate)), 1.0e-10);
        }
        Assertions.assertEquals(0., refPos.distance(station.getPosition()), 1.0e-10);
        Assertions.assertEquals(0., refVel.distance(station.getVelocity()), 1.0e-10);
        if (antennaType != null) {
            Assertions.assertEquals(antennaType, station.getAntennaType(midDate));
        }

    }

}
