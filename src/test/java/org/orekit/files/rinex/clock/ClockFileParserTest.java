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
package org.orekit.files.rinex.clock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.clock.RinexClock.ClockDataLine;
import org.orekit.files.rinex.clock.RinexClock.ClockDataType;
import org.orekit.files.rinex.clock.RinexClock.Receiver;
import org.orekit.files.rinex.clock.RinexClock.ReferenceClock;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeSpanMap;

/** This class aims at validating the correct IGS clock file parsing and error handling. */
public class ClockFileParserTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /** First example given in the 3.04 RINEX clock file format. */
    @Test
    public void testParseExple1V304() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_1_304.clk";

        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Check content of the file
        final double version = 3.04;
        final SatelliteSystem satelliteSystem = SatelliteSystem.GPS;
        final TimeSystem timeSystem = TimeSystem.GPS;
        final String programName = "TORINEXC V9.9";
        final String agencyName = "USNO";
        final String comments = "EXAMPLE OF A CLOCK DATA ANALYSIS FILE\n" +
                                "IN THIS CASE ANALYSIS RESULTS FROM GPS ONLY ARE INCLUDED\n" +
                                "No re-alignment of the clocks has been applied.\n";
        final String stationName = "";
        final String stationIdentifier = "";
        final String analysisCenterID = "USN";
        final String analysisCenterName = "USNO USING GIPSY/OASIS-II";
        final String externalClockReference = "";
        final String creationDateString = "19960403";
        final String creationTimeString = "001000";
        final String creationZoneString = "UTC";
        final AbsoluteDate creationDate = new AbsoluteDate(1996, 4, 3, 0, 10, 0., TimeScalesFactory.getUTC());
        final int numberOfLeapSeconds = 10;
        final int numberOfLeapSecondsGPS = 0;
        final int numberOfDBCS = 1;
        final int numberOfPCVS = 1;
        final int numberOfDataTypes = 2;
        final int numberOfObservationTypes = 4;
        final String frameString = "ITRF96";
        final int numberOfReceivers = 5; // Should be 4 as recorded in the file, however, according to format description, correct number is 5.
        final int numberOfSatellites = 27;
        final int numberOfDataLines = 5;
        final String id = "GOLD";
        final ClockDataType type = ClockDataType.AR;
        final TimeScale timeScale = TimeScalesFactory.getGPS();
        final AbsoluteDate dataEpoch = new AbsoluteDate(1994, 7, 14, 20, 59, 0.0, timeScale);
        final int numberOfValues = 4;
        final double clockBias = -0.0123456789012;
        final double clockBiasSigma = -0.00123456789012;
        final double clockRate = -0.000123456789012;
        final double clockRateSigma = -0.0000123456789012;
        final double clockAcceleration = 0.0;
        final double clockAccelerationSigma = 0.0;
        checkClockFileContent(file, version, satelliteSystem, timeSystem,
                              programName, agencyName, comments, stationName, stationIdentifier,
                              analysisCenterID, analysisCenterName, externalClockReference,
                              creationDateString, creationTimeString, creationZoneString, creationDate,
                              numberOfLeapSeconds, numberOfLeapSecondsGPS, numberOfDBCS, numberOfPCVS,
                              numberOfDataTypes, numberOfObservationTypes, frameString,
                              numberOfReceivers, numberOfSatellites, numberOfDataLines,
                              id, type, timeScale, dataEpoch, numberOfValues,
                              clockBias, clockBiasSigma, clockRate, clockRateSigma, clockAcceleration, clockAccelerationSigma);

        // In this case, time system in properlydefined, check getEpoch() methods
        // Get data line
        final ClockDataLine dataLine = file.getClockData().get(id).get(0);
        Assertions.assertTrue(dataLine.getEpoch().equals(dataLine.getEpoch(file.getTimeScale())));
    }

    /** Second example given in the 3.04 RINEX clock file format.
     * PCVS block is not placed where it should be.
     */
    @Test
    public void testParseExple2V304() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_2_304.clk";

        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);



        // Check content of the file
        final double version = 3.04;
        final SatelliteSystem satelliteSystem = SatelliteSystem.GPS;
        final TimeSystem timeSystem = TimeSystem.GPS;
        final String programName = "CCLOCK";
        final String agencyName = "IGSACC @ GA & MIT";
        final String comments = "GPS week: 1939   Day: 6   MJD: 57823\n" +
                                "THE COMBINED CLOCKS ARE A WEIGHTED AVERAGE OF:\n" +
                                "cod emr esa gfz jpl\n" +
                                "THE FOLLOWING REFERENCE CLOCKS WERE USED BY ACs:\n" +
                                "PIE1 HERS AMC2 ZECK\n" +
                                "THE COMBINED CLOCKS ARE ALIGNED TO GPS TIME\n" +
                                "USING THE SATELLITE BROADCAST EPHEMERIDES\n";
        final String stationName = "";
        final String stationIdentifier = "";
        final String analysisCenterID = "IGS";
        final String analysisCenterName = "IGSACC @ GA and MIT";
        final String externalClockReference = "";
        final String creationDateString = "20170312";
        final String creationTimeString = "052227";
        final String creationZoneString = "UTC";
        final AbsoluteDate creationDate = new AbsoluteDate(2017, 3, 12, 5, 22, 27., TimeScalesFactory.getUTC());
        final int numberOfLeapSeconds = 37;
        final int numberOfLeapSecondsGPS = 18;
        final int numberOfDBCS = 0;
        final int numberOfPCVS = 1;
        final int numberOfDataTypes = 2;
        final int numberOfObservationTypes = 0;
        final String frameString = "IGS14";
        final int numberOfReceivers = 22;
        final int numberOfSatellites = 31;
        final int numberOfDataLines = 6;
        final String id = "G02";
        final ClockDataType type = ClockDataType.AS;
        final TimeScale timeScale = TimeScalesFactory.getGPS();
        final AbsoluteDate dataEpoch = new AbsoluteDate(2017, 3, 11, 0, 0, 0.0, timeScale);
        final int numberOfValues = 2;
        final double clockBias = 0.868606546478E-04;
        final double clockBiasSigma = 0.104109157753E-10;
        final double clockRate = 0.0;
        final double clockRateSigma = 0.0;
        final double clockAcceleration = 0.0;
        final double clockAccelerationSigma = 0.0;
        checkClockFileContent(file, version, satelliteSystem, timeSystem,
                              programName, agencyName, comments, stationName, stationIdentifier,
                              analysisCenterID, analysisCenterName, externalClockReference,
                              creationDateString, creationTimeString, creationZoneString, creationDate,
                              numberOfLeapSeconds, numberOfLeapSecondsGPS, numberOfDBCS, numberOfPCVS,
                              numberOfDataTypes, numberOfObservationTypes, frameString,
                              numberOfReceivers, numberOfSatellites, numberOfDataLines,
                              id, type, timeScale, dataEpoch, numberOfValues,
                              clockBias, clockBiasSigma, clockRate, clockRateSigma, clockAcceleration, clockAccelerationSigma);

    }

    /** Third example given in the 3.04 RINEX clock file format.
     * It embeds calibration data and misses time system ID.
     */
    @Test
    public void testParseExpleCalibrationV304() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/clock/Exple_calibration_304.clk";

        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Check content of the file
        final double version = 3.04;
        final SatelliteSystem satelliteSystem = null;
        final TimeSystem timeSystem = null;
        final String programName = "TORINEXC V9.9";
        final String agencyName = "USNO";
        final String comments = "EXAMPLE OF A CLOCK DATA FILE\n" +
                                "IN THIS CASE CALIBRATION/DISCONTINUITY DATA GIVEN\n";
        final String stationName = "USNO";
        final String stationIdentifier = "40451S003";
        final String analysisCenterID = "";
        final String analysisCenterName = "";
        final String externalClockReference = "UTC(USNO) MASTER CLOCK VIA CONTINUOUS CABLE MONITOR";
        final String creationDateString = "19960403";
        final String creationTimeString = "001000";
        final String creationZoneString = "UTC";
        final AbsoluteDate creationDate = new AbsoluteDate(1996, 4, 3, 0, 10, 0., TimeScalesFactory.getUTC());
        final int numberOfLeapSeconds = 0;
        final int numberOfLeapSecondsGPS = 10;
        final int numberOfDBCS = 0;
        final int numberOfPCVS = 0;
        final int numberOfDataTypes = 2;
        final int numberOfObservationTypes = 0;
        final String frameString = "";
        final int numberOfReceivers = 0;
        final int numberOfSatellites = 0;
        final int numberOfDataLines = 4;
        final String id = "USNO";
        final ClockDataType type = ClockDataType.DR;
        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final AbsoluteDate dataEpoch = new AbsoluteDate(1995, 7, 14, 22, 23, 14.5, timeScale);
        final int numberOfValues = 2;
        final double clockBias = -0.123456789012E+01;
        final double clockBiasSigma = 0.123456789012E+00;
        final double clockRate = 0.0;
        final double clockRateSigma = 0.0;
        final double clockAcceleration = 0.0;
        final double clockAccelerationSigma = 0.0;
        checkClockFileContent(file, version, satelliteSystem, timeSystem,
                              programName, agencyName, comments, stationName, stationIdentifier,
                              analysisCenterID, analysisCenterName, externalClockReference,
                              creationDateString, creationTimeString, creationZoneString, creationDate,
                              numberOfLeapSeconds, numberOfLeapSecondsGPS, numberOfDBCS, numberOfPCVS,
                              numberOfDataTypes, numberOfObservationTypes, frameString,
                              numberOfReceivers, numberOfSatellites, numberOfDataLines,
                              id, type, timeScale, dataEpoch, numberOfValues,
                              clockBias, clockBiasSigma, clockRate, clockRateSigma, clockAcceleration, clockAccelerationSigma);
    }

    /** An example of the 3.00 RINEX clock file format.
     * It is espicially missing satellite system, creation time zoneand time system ID.
     * Creation date also does not match expected format.
     */
    @Test
    public void testParseExple1V300() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/clock/mit19044_truncated_300.clk";

        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Check content of the file
        final double version = 3.00;
        final SatelliteSystem satelliteSystem = null;
        final TimeSystem timeSystem = null;
        final String programName = "autcln 3.33+MIG";
        final String agencyName = "MIT";
        final String comments = "";
        final String stationName = "";
        final String stationIdentifier = "";
        final String analysisCenterID = "MIT";
        final String analysisCenterName = "";
        final String externalClockReference = "";
        final String creationDateString = "2016-07-14";
        final String creationTimeString = "16:28";
        final String creationZoneString = "";
        final AbsoluteDate creationDate = null;
        final int numberOfLeapSeconds = 17;
        final int numberOfLeapSecondsGPS = 0;
        final int numberOfDBCS = 0;
        final int numberOfPCVS = 1;
        final int numberOfDataTypes = 2;
        final int numberOfObservationTypes = 0;
        final String frameString = "IGS08";
        final int numberOfReceivers = 12;
        final int numberOfSatellites = 31;
        final int numberOfDataLines = 10;
        final String id = "G19";
        final ClockDataType type = ClockDataType.AS;
        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final AbsoluteDate dataEpoch = new AbsoluteDate(2016, 7, 7, 0, 0, 30.0, timeScale);
        final int numberOfValues = 1;
        final double clockBias = -0.525123304077E-03;
        final double clockBiasSigma = 0.200000000000E-09;
        final double clockRate = 0.0;
        final double clockRateSigma = 0.0;
        final double clockAcceleration = 0.0;
        final double clockAccelerationSigma = 0.0;
        checkClockFileContent(file, version, satelliteSystem, timeSystem,
                              programName, agencyName, comments, stationName, stationIdentifier,
                              analysisCenterID, analysisCenterName, externalClockReference,
                              creationDateString, creationTimeString, creationZoneString, creationDate,
                              numberOfLeapSeconds, numberOfLeapSecondsGPS, numberOfDBCS, numberOfPCVS,
                              numberOfDataTypes, numberOfObservationTypes, frameString,
                              numberOfReceivers, numberOfSatellites, numberOfDataLines,
                              id, type, timeScale, dataEpoch, numberOfValues,
                              clockBias, clockBiasSigma, clockRate, clockRateSigma, clockAcceleration, clockAccelerationSigma);
    }

    /** Another example of the 3.00 RINEX clock file format.
     * It is espicially missing creation date, time system ID and satellite system.
     * PCVS block is not placed where it should be.
     */
    @Test
    public void testParseExple2V300() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/clock/igr21101_truncated_300.clk";

        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Check content of the file
        final double version = 3.00;
        final SatelliteSystem satelliteSystem = null;
        final TimeSystem timeSystem = null;
        final String programName = "CCLOCK";
        final String agencyName = "IGSACC @ GA MIT";
        final String comments = "GPS week: 2110   Day: 1   MJD: 59015\n" +
                                "THE COMBINED CLOCKS ARE A WEIGHTED AVERAGE OF:\n" +
                                "cod emr esa gfz jpl\n" +
                                "THE FOLLOWING REFERENCE CLOCKS WERE USED BY ACs:\n" +
                                "MGUE NRC1 HERS BRUX\n" +
                                "THE COMBINED CLOCKS ARE ALIGNED TO GPS TIME\n" +
                                "USING THE SATELLITE BROADCAST EPHEMERIDES\n" +
                                "All clocks have been re-aligned to the IGS time scale: IGST\n";
        final String stationName = "";
        final String stationIdentifier = "";
        final String analysisCenterID = "IGS";
        final String analysisCenterName = "IGSACC @ GA MIT";
        final String externalClockReference = "";
        final String creationDateString = "";
        final String creationTimeString = "";
        final String creationZoneString = "";
        final AbsoluteDate creationDate = null;
        final int numberOfLeapSeconds = 18;
        final int numberOfLeapSecondsGPS = 0;
        final int numberOfDBCS = 0;
        final int numberOfPCVS = 1;
        final int numberOfDataTypes = 2;
        final int numberOfObservationTypes = 0;
        final String frameString = "IGb14";
        final int numberOfReceivers = 18;
        final int numberOfSatellites = 31;
        final int numberOfDataLines = 7;
        final String id = "GPST";
        final ClockDataType type = ClockDataType.AR;
        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final AbsoluteDate dataEpoch = new AbsoluteDate(2020, 6, 15, 0, 0, 0.0, timeScale);
        final int numberOfValues = 2;
        final double clockBias = -5.447980630520e-09;
        final double clockBiasSigma = 0.0;
        final double clockRate = 0.0;
        final double clockRateSigma = 0.0;
        final double clockAcceleration = 0.0;
        final double clockAccelerationSigma = 0.0;
        checkClockFileContent(file, version, satelliteSystem, timeSystem,
                              programName, agencyName, comments, stationName, stationIdentifier,
                              analysisCenterID, analysisCenterName,  externalClockReference,
                              creationDateString, creationTimeString, creationZoneString, creationDate,
                              numberOfLeapSeconds, numberOfLeapSecondsGPS, numberOfDBCS, numberOfPCVS,
                              numberOfDataTypes, numberOfObservationTypes, frameString,
                              numberOfReceivers, numberOfSatellites, numberOfDataLines,
                              id, type, timeScale, dataEpoch, numberOfValues,
                              clockBias, clockBiasSigma, clockRate, clockRateSigma, clockAcceleration, clockAccelerationSigma);
    }

    /** An example of the 2.00 RINEX clock file format. */
    @Test
    public void testParseExple1V200() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/clock/emr10491_truncated_200.clk";

        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Check content of the file
        final double version = 2.00;
        final SatelliteSystem satelliteSystem = null;
        final TimeSystem timeSystem = null;
        final String programName = "CLKRINEX V1.0";
        final String agencyName = "NRCan";
        final String comments = "CLK ANT Z-OFFSET(M): II/IIA 1.023; IIR 0.000\n" +
                                "No re-alignment of the clocks has been applied.\n";
        final String stationName = "";
        final String stationIdentifier = "";
        final String analysisCenterID = "EMR";
        final String analysisCenterName = "NATURAL RESOURCES CANADA";
        final String externalClockReference = "";
        final String creationDateString = "1-Mar-2000";
        final String creationTimeString = "20:36";
        final String creationZoneString = "";
        final AbsoluteDate creationDate = null;
        final int numberOfLeapSeconds = -13;
        final int numberOfLeapSecondsGPS = 0;
        final int numberOfDBCS = 0;
        final int numberOfPCVS = 0;
        final int numberOfDataTypes = 2;
        final int numberOfObservationTypes = 0;
        final String frameString = "ITRF97";
        final int numberOfReceivers = 13;
        final int numberOfSatellites = 28;
        final int numberOfDataLines = 7;
        final String id = "CHUR";
        final ClockDataType type = ClockDataType.AR;
        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final AbsoluteDate dataEpoch = new AbsoluteDate(2000, 2, 14, 0, 0, 0.0, timeScale);
        final int numberOfValues = 2;
        final double clockBias = 2.301824851111E-05;
        final double clockBiasSigma = 2.795267117761E-10;
        final double clockRate = 0.0;
        final double clockRateSigma = 0.0;
        final double clockAcceleration = 0.0;
        final double clockAccelerationSigma = 0.0;
        checkClockFileContent(file, version, satelliteSystem, timeSystem,
                              programName, agencyName, comments, stationName, stationIdentifier,
                              analysisCenterID, analysisCenterName, externalClockReference,
                              creationDateString, creationTimeString, creationZoneString, creationDate,
                              numberOfLeapSeconds, numberOfLeapSecondsGPS, numberOfDBCS, numberOfPCVS,
                              numberOfDataTypes, numberOfObservationTypes, frameString,
                              numberOfReceivers, numberOfSatellites, numberOfDataLines,
                              id, type, timeScale, dataEpoch, numberOfValues,
                              clockBias, clockBiasSigma, clockRate, clockRateSigma, clockAcceleration, clockAccelerationSigma);
    }

    /** Another example of the 2.00 RINEX clock file format with another date time zone foramt. */
    @Test
    public void testParseExple2V200() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/clock/jpl11456_truncated_200.clk";

        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Check content of the file
        final double version = 2.00;
        final SatelliteSystem satelliteSystem = null;
        final TimeSystem timeSystem = null;
        final String programName = "tdp2clk v1.13";
        final String agencyName = "JPL";
        final String comments = "G11 G13 G14 G18 G20 G28      clk ant z-offset (m):  0.000\n" +
                                "G01 G03 G04 G05 G06 G07 G08  clk ant z-offset (m):  1.023\n" +
                                "G09 G10 G21 G22 G23 G24 G25  clk ant z-offset (m):  1.023\n" +
                                "G26 G27 G29 G30 G31          clk ant z-offset (m):  1.023\n" +
                                "clk ant z-offset (m): II/IIA 1.023; IIR 0.000\n" +
                                "Re-alignment to GPS time by broadcast clocks and linear fit\n";
        final String stationName = "";
        final String stationIdentifier = "";
        final String analysisCenterID = "JPL";
        final String analysisCenterName = "Jet Propulsion Laboratory";
        final String externalClockReference = "";
        final String creationDateString = "2002 Jan 3";
        final String creationTimeString = "13:36:17";
        final String creationZoneString = "";
        final AbsoluteDate creationDate = null;
        final int numberOfLeapSeconds = 13;
        final int numberOfLeapSecondsGPS = 0;
        final int numberOfDBCS = 0;
        final int numberOfPCVS = 0;
        final int numberOfDataTypes = 1;
        final int numberOfObservationTypes = 0;
        final String frameString = "IGS00";
        final int numberOfReceivers = 1;
        final int numberOfSatellites = 0;
        final int numberOfDataLines = 1;
        final String id = "ALGO";
        final ClockDataType type = ClockDataType.AR;
        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final AbsoluteDate dataEpoch = new AbsoluteDate(2001, 12, 22, 0, 0, 0.0, timeScale);
        final int numberOfValues = 2;
        final double clockBias = 1.598690662191e-06;
        final double clockBiasSigma = 1.067405104634e-10;
        final double clockRate = 0.0;
        final double clockRateSigma = 0.0;
        final double clockAcceleration = 0.0;
        final double clockAccelerationSigma = 0.0;
        checkClockFileContent(file, version, satelliteSystem, timeSystem,
                              programName, agencyName, comments, stationName, stationIdentifier,
                              analysisCenterID, analysisCenterName, externalClockReference,
                              creationDateString, creationTimeString, creationZoneString, creationDate,
                              numberOfLeapSeconds, numberOfLeapSecondsGPS, numberOfDBCS, numberOfPCVS,
                              numberOfDataTypes, numberOfObservationTypes, frameString,
                              numberOfReceivers, numberOfSatellites, numberOfDataLines,
                              id, type, timeScale, dataEpoch, numberOfValues,
                              clockBias, clockBiasSigma, clockRate, clockRateSigma, clockAcceleration, clockAccelerationSigma);
    }

    /** Another example of the 2.00 RINEX clock file format with another date time zone foramt. */
    @Test
    public void testParseExple3V200() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/clock/cod17381_truncated_200.clk";

        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Check content of the file
        final double version = 2.00;
        final SatelliteSystem satelliteSystem = null;
        final TimeSystem timeSystem = TimeSystem.GPS;
        final String programName = "CCRNXC V5.3";
        final String agencyName = "AIUB";
        final String comments = "CODE final GPS clock information for day 119, 2013, 3D-sol\n" +
                                "Clock information consistent with phase and P1/P2 code data\n" +
                                "Satellite/receiver clock values at intervals of 30/300 sec\n" +
                                "High-rate (30 sec) clock interpolation based on phase data\n";
        final String stationName = "";
        final String stationIdentifier = "";
        final String analysisCenterID = "COD";
        final String analysisCenterName = "Center for Orbit Determination in Europe";
        final String externalClockReference = "";
        final String creationDateString = "04-MAY-13";
        final String creationTimeString = "04:39";
        final String creationZoneString = "";
        final AbsoluteDate creationDate = null;
        final int numberOfLeapSeconds = 16;
        final int numberOfLeapSecondsGPS = 0;
        final int numberOfDBCS = 1;
        final int numberOfPCVS = 1;
        final int numberOfDataTypes = 2;
        final int numberOfObservationTypes = 0;
        final String frameString = "IGb08";
        final int numberOfReceivers = 2;
        final int numberOfSatellites = 2;
        final int numberOfDataLines = 5;
        final String id = "AMC2";
        final ClockDataType type = ClockDataType.AR;
        final TimeScale timeScale = TimeScalesFactory.getGPS();
        final AbsoluteDate dataEpoch = new AbsoluteDate(2013, 04, 29, 0, 0, 30.0, timeScale);
        final int numberOfValues = 1;
        final double clockBias = 0.192333320310E-08;
        final double clockBiasSigma = 0.0;
        final double clockRate = 0.0;
        final double clockRateSigma = 0.0;
        final double clockAcceleration = 0.0;
        final double clockAccelerationSigma = 0.0;
        checkClockFileContent(file, version, satelliteSystem, timeSystem,
                              programName, agencyName, comments, stationName, stationIdentifier,
                              analysisCenterID, analysisCenterName, externalClockReference,
                              creationDateString, creationTimeString, creationZoneString, creationDate,
                              numberOfLeapSeconds, numberOfLeapSecondsGPS, numberOfDBCS, numberOfPCVS,
                              numberOfDataTypes, numberOfObservationTypes, frameString,
                              numberOfReceivers, numberOfSatellites, numberOfDataLines,
                              id, type, timeScale, dataEpoch, numberOfValues,
                              clockBias, clockBiasSigma, clockRate, clockRateSigma, clockAcceleration, clockAccelerationSigma);
    }

    /** Test parsing file with observation type continuation line. */
    @Test
    public void testParseWithObsTypeContinuationLine() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_1_304_more_obs_types.clk";

        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Theorical values
        final int numberOfObservationTypesGPS = 16;
        final int numberOfObservationTypesGAL = 0;
        final int numberOfSatelliteSystems = 1;

        // Get the satellite systems - observation types map
        final Map<SatelliteSystem, List<ObservationType>> obsTypeMap = file.getSystemObservationTypes();

        // Check map content
        Assertions.assertEquals(numberOfSatelliteSystems, obsTypeMap.keySet().size());
        Assertions.assertEquals(numberOfObservationTypesGPS, file.numberOfObsTypes(SatelliteSystem.GPS));
        Assertions.assertEquals(numberOfObservationTypesGAL, file.numberOfObsTypes(SatelliteSystem.GALILEO));
    }

    /** Check receiver inforamtion. */
    @Test
    public void testParsedReceivers() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_2_304.clk";
        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Get theorical values
        final int index = 3;
        final String designator = "GUAM";
        final String identifier = "50501M002";
        final double x = -5071312.680;
        final double y = 3568363.624;
        final double z = 1488904.394;

        // Get the receiver in the parsed clock file
        final List<Receiver> receivers = file.getReceivers();
        final Receiver receiver = receivers.get(index);

        //Check content
        Assertions.assertEquals(designator, receiver.getDesignator());
        Assertions.assertEquals(identifier, receiver.getReceiverIdentifier());
        Assertions.assertEquals(x, receiver.getX(), 1E-4);
        Assertions.assertEquals(y, receiver.getY(), 1E-4);
        Assertions.assertEquals(z, receiver.getZ(), 1E-4);
    }

    /** Test default frame loader. */
    @Test
    public void testDefaultFrameLoader() throws URISyntaxException, IOException {

        // Get frames corresponding to default frame loader
        final Frame itrf1996 = FramesFactory.getITRF(IERSConventions.IERS_1996, false);
        final Frame itrf2010 = FramesFactory.getITRF(IERSConventions.IERS_2010, false);

        // Get default clock file parser
        final RinexClockParser parser = new RinexClockParser();

        // Parse file with expected frame ITRF96
        final String ex1 = "/gnss/clock/Exple_analysis_1_304.clk";
        final String fileName1 = Paths.get(getClass().getResource(ex1).toURI()).toString();
        final RinexClock file1 = parser.parse(fileName1);

        // Parse file with default expected frame ITRF 2010
        final String ex2 = "/gnss/clock/Exple_analysis_2_304.clk";
        final String fileName2 = Paths.get(getClass().getResource(ex2).toURI()).toString();
        final RinexClock file2 = parser.parse(fileName2);

        // Check frames
        Assertions.assertTrue(itrf1996.equals(file1.getFrame()));
        Assertions.assertTrue(itrf2010.equals(file2.getFrame()));
    }

    /** Test the reference clocks.  */
    @Test
    public void testReferenceClocks() throws IOException, URISyntaxException {

        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_1_304.clk";
        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Get reference clocks
        final TimeSpanMap<List<ReferenceClock>> referenceClocksMap = file.getReferenceClocks();

        // Theorical time scale
        final TimeScale gps = TimeScalesFactory.getGPS();

        // First reference clock theoretical values
        final String referenceName1 = "USNO";
        final String clockId1 = "40451S003";
        final double clockConstraint1 = -.123456789012E+00;
        final AbsoluteDate startDate1 = new AbsoluteDate(1994, 7, 14, 0, 0, 0.0, gps);
        final AbsoluteDate endDate1 = new AbsoluteDate(1994, 7, 14, 20, 59, 0.0, gps);

        // Second reference clock theoretical values
        final String referenceName2 = "TIDB";
        final String clockId2 = "50103M108";
        final double clockConstraint2 = -0.123456789012E+00;
        final AbsoluteDate startDate2 = new AbsoluteDate(1994, 7, 14, 21, 0, 0.0, gps);
        final AbsoluteDate endDate2 = new AbsoluteDate(1994, 7, 14, 21, 59, 0.0, gps);

        // Check number of time spans
        Assertions.assertEquals(3, referenceClocksMap.getSpansNumber());

        // Get the two lists of reference clocks
        final List<ReferenceClock> referenceClocks1 = referenceClocksMap.get(new AbsoluteDate(1994, 7, 14, 15, 0, 0.0, gps));
        final List<ReferenceClock> referenceClocks2 = referenceClocksMap.get(new AbsoluteDate(1994, 7, 14, 21, 30, 0.0, gps));

        // Check total number of reference clocks
        final int totalReferenceClockNumber = referenceClocks1.size() + referenceClocks2.size();
        Assertions.assertEquals(2, totalReferenceClockNumber);

        // Check contents
        checkReferenceClock(referenceClocks1.get(0),
                            referenceName1, clockId1, clockConstraint1, startDate1, endDate1);
        checkReferenceClock(referenceClocks2.get(0),
                            referenceName2, clockId2, clockConstraint2, startDate2, endDate2);
    }

    /** Test the satelite list.  */
    @Test
    public void testSatelliteList() throws IOException, URISyntaxException {

        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_1_304.clk";
        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Get satellite list
        final List<String> satellites = file.getSatellites();

        // Expected list
        final String prnLine = "G01 G02 G03 G04 G05 G06 G07 G08 G09 G10 G13 G14 G15 G16 G17 G18 " +
                               "G19 G21 G22 G23 G24 G25 G26 G27 G29 G30 G31";
        final String[] expected = prnLine.split(" ");

        for (int i = 0; i < satellites.size(); i++) {
            Assertions.assertArrayEquals(expected, satellites.toArray());
        }
    }

    /** Test two same receivers and satellite.  */
    @Test
    public void testSameReceiversAndSatellites() throws IOException, URISyntaxException {

        // Parse file
        final String ex = "/gnss/clock/two_same_receivers_and_satellites.clk";
        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        Assertions.assertEquals(1, file.getNumberOfReceivers());
        Assertions.assertEquals(1, file.getNumberOfSatellites());
    }

    /** Test the clock data type list.  */
    @Test
    public void testClockDataTypes() throws IOException, URISyntaxException {

        // Parse file
        final String ex = "/gnss/clock/Exple_calibration_304.clk";
        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        // Get satellite list
        final List<ClockDataType> dataTypes = file.getClockDataTypes();

        // Expected list
        final List<ClockDataType> expected =  new ArrayList<ClockDataType>();
        expected.add(ClockDataType.CR);
        expected.add(ClockDataType.DR);

        for (int i = 0; i < dataTypes.size(); i++) {
            Assertions.assertArrayEquals(expected.toArray(), dataTypes.toArray());
        }
    }

    /** Test parsing error exception. */
    @Test
    public void testParsingErrorException() throws IOException {
        try {
            final String ex = "/gnss/clock/error_in_line_4.clk";
            final RinexClockParser parser = new RinexClockParser();
            final InputStream inEntry = getClass().getResourceAsStream(ex);
            parser.parse(inEntry);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(4, oe.getParts()[0]);
        }
    }

    /** Test missing block error exception. */
    @Test
    public void testMissingBlockException() throws IOException {
        try {
            final String ex = "/gnss/clock/missing_block_end_of_header.clk";
            final RinexClockParser parser = new RinexClockParser();
            final InputStream inEntry = getClass().getResourceAsStream(ex);
            parser.parse(inEntry);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(38, oe.getParts()[0]);
        }
    }

    /** Unsupported clock file version exception throwing test. */
    @Test
    public void testUnsupportedVersion() throws IOException {
        try {
            final String ex = "/gnss/clock/unsupported_clock_file_version.clk";
            final RinexClockParser parser = new RinexClockParser();
            final InputStream inEntry = getClass().getResourceAsStream(ex);
            parser.parse(inEntry);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CLOCK_FILE_UNSUPPORTED_VERSION, oe.getSpecifier());
            Assertions.assertEquals("0.05", Double.toString((double) oe.getParts()[0]));
        }
    }

    /** Wrong clock data type exception throwing test. */
    @Test
    public void testWrongClockDataType() throws IOException {
        try {
            final String ex = "/gnss/clock/wrong_clock_data_type.clk";
            final RinexClockParser parser = new RinexClockParser();
            final InputStream inEntry = getClass().getResourceAsStream(ex);
            parser.parse(inEntry);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_CLOCK_DATA_TYPE, oe.getSpecifier());
            Assertions.assertEquals("XX", oe.getParts()[0]);
        }
    }

    /** Unknown time system exception throwing test. */
    @Test
    public void testUnknownTimeSystem() throws IOException {
        try {
            final String ex = "/gnss/clock/unknown_time_system.clk";
            final RinexClockParser parser = new RinexClockParser();
            final InputStream inEntry = getClass().getResourceAsStream(ex);
            parser.parse(inEntry);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_TIME_SYSTEM, oe.getSpecifier());
            Assertions.assertEquals("WWW", oe.getParts()[0]);
        }
    }

    @Test
    public void testTimeSystem() {
        Assertions.assertEquals(TimeScalesFactory.getGPS(),
                TimeSystem.GPS.getTimeScale(DataContext.getDefault().getTimeScales()));
        Assertions.assertEquals(TimeScalesFactory.getGST(),
                TimeSystem.GALILEO.getTimeScale(DataContext.getDefault().getTimeScales()));
        Assertions.assertEquals(TimeScalesFactory.getGLONASS(),
                TimeSystem.GLONASS.getTimeScale(DataContext.getDefault().getTimeScales()));
        Assertions.assertEquals(TimeScalesFactory.getQZSS(),
                TimeSystem.QZSS.getTimeScale(DataContext.getDefault().getTimeScales()));
        Assertions.assertEquals(TimeScalesFactory.getTAI(),
                TimeSystem.TAI.getTimeScale(DataContext.getDefault().getTimeScales()));
        Assertions.assertEquals(TimeScalesFactory.getUTC(),
                TimeSystem.UTC.getTimeScale(DataContext.getDefault().getTimeScales()));
        Assertions.assertEquals(TimeScalesFactory.getBDT(),
                TimeSystem.BEIDOU.getTimeScale(DataContext.getDefault().getTimeScales()));
        Assertions.assertEquals(TimeScalesFactory.getIRNSS(),
                TimeSystem.IRNSS.getTimeScale(DataContext.getDefault().getTimeScales()));
        Assertions.assertEquals(TimeScalesFactory.getUTC(),
                                TimeSystem.GMT.getTimeScale(DataContext.getDefault().getTimeScales()));
        Assertions.assertEquals(TimeScalesFactory.getGPS(),
                                TimeSystem.UNKNOWN.getTimeScale(DataContext.getDefault().getTimeScales()));
    }

    /** Test parsing file of issue #845 (https://gitlab.orekit.org/orekit/orekit/-/issues/845). */
    @Test
    public void testIssue845() throws URISyntaxException, IOException {

        // Parse file
        final String ex = "/gnss/clock/issue845.clk";

        final RinexClockParser parser = new RinexClockParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final RinexClock file = parser.parse(fileName);

        Assertions.assertEquals(3.0, file.getFormatVersion(), 1.0e-3);
        Assertions.assertEquals("GeoForschungsZentrum Potsdam", file.getAnalysisCenterName());
        Assertions.assertEquals("GFZ", file.getAgencyName());
        Assertions.assertEquals("IGS14", file.getFrameName());
    }

    /** Check the content of a clock file. */
    private void checkClockFileContent(final RinexClock file,
                                       final double version, final SatelliteSystem satelliteSystem, final TimeSystem timeSystem,
                                       final String programName, final String agencyName, final String comments,
                                       final String stationName, final String stationIdentifier,
                                       final String analysisCenterID, final String analysisCenterName, final String externalClockReference,
                                       final String creationDateString, final String creationTimeString, final String creationZoneString,
                                       final AbsoluteDate creationDate, final int numberOfLeapSeconds, final int numberOfLeapSecondsGPS,
                                       final int numberOfDBCS, final int numberOfPCVS,
                                       final int numberOfDataTypes, final int numberOfObservationTypes, final String frameString,
                                       final int numberOfReceivers, final int numberOfSatellites,
                                       final int numberOfDataLines, final String id, final ClockDataType type, final TimeScale timeScale,
                                       final AbsoluteDate dataEpoch, final int numberOfValues,
                                       final double clockBias, final double clockBiasSigma,
                                       final double clockRate, final double clockRateSigma,
                                       final double clockAcceleration, final double clockAccelerationSigma) {

        // Check header
        Assertions.assertEquals(version, file.getFormatVersion(), 1E-3);
        Assertions.assertTrue(satelliteSystem == file.getSatelliteSystem());
        Assertions.assertTrue(timeSystem == file.getTimeSystem());
        Assertions.assertEquals(programName, file.getProgramName());
        Assertions.assertEquals(agencyName, file.getAgencyName());
        Assertions.assertEquals(comments, file.getComments());
        Assertions.assertEquals(stationName, file.getStationName());
        Assertions.assertEquals(stationIdentifier, file.getStationIdentifier());
        Assertions.assertEquals(analysisCenterID, file.getAnalysisCenterID());
        Assertions.assertEquals(analysisCenterName, file.getAnalysisCenterName());
        Assertions.assertEquals(externalClockReference, file.getExternalClockReference());
        Assertions.assertEquals(creationDateString, file.getCreationDateString());
        Assertions.assertEquals(creationTimeString, file.getCreationTimeString());
        Assertions.assertEquals(creationZoneString, file.getCreationTimeZoneString());
        if (null != creationDate) {
            Assertions.assertTrue(file.getCreationDate().equals(creationDate));
        }
        Assertions.assertEquals(numberOfLeapSeconds, file.getNumberOfLeapSeconds());
        Assertions.assertEquals(numberOfLeapSecondsGPS, file.getNumberOfLeapSecondsGNSS());
        Assertions.assertEquals(numberOfDBCS, file.getListAppliedDCBS().size());
        Assertions.assertEquals(numberOfPCVS, file.getListAppliedPCVS().size());
        Assertions.assertEquals(numberOfDataTypes, file.getNumberOfClockDataTypes());
        int observationTypes = 0;
        for (SatelliteSystem system : file.getSystemObservationTypes().keySet()) {
            observationTypes += file.getSystemObservationTypes().get(system).size();
        }
        Assertions.assertEquals(numberOfObservationTypes, observationTypes);
        Assertions.assertEquals(frameString, file.getFrameName());
        Assertions.assertEquals(numberOfReceivers, file.getNumberOfReceivers());
        Assertions.assertEquals(numberOfSatellites, file.getNumberOfSatellites());

        // Check total number of data lines
        Assertions.assertEquals(numberOfDataLines, file.getTotalNumberOfDataLines());

        // Look for a particular, random data line
        final List<ClockDataLine> clockDataLines = file.getClockData().get(id);
        boolean find = false;
        for (int i = 0; i < clockDataLines.size(); i++) {
            final ClockDataLine clockDataLine = clockDataLines.get(i);
            if (clockDataLine.getName().equals(id) &&
                clockDataLine.getDataType().equals(type) &&
                clockDataLine.getEpoch().equals(dataEpoch) &&
                clockDataLine.getNumberOfValues() == numberOfValues &&
                clockDataLine.getClockBias() == clockBias &&
                clockDataLine.getClockBiasSigma() == clockBiasSigma &&
                clockDataLine.getClockRate() == clockRate &&
                clockDataLine.getClockRateSigma() == clockRateSigma &&
                clockDataLine.getClockAcceleration() == clockAcceleration &&
                clockDataLine.getClockAccelerationSigma() == clockAccelerationSigma) {

                find = true;
            }
        }

        Assertions.assertTrue(find);

    }

    /** Check reference clock object content. */
    private void checkReferenceClock(final ReferenceClock referenceClock,
                                     final String referenceName, final String clockId,
                                     final double clockConstraint, final AbsoluteDate startDate, final AbsoluteDate endDate) {

        Assertions.assertEquals(referenceName, referenceClock.getReferenceName());
        Assertions.assertEquals(clockId, referenceClock.getClockID());
        Assertions.assertEquals(clockConstraint, referenceClock.getClockConstraint(), 1e-12);
        Assertions.assertTrue(startDate.equals(referenceClock.getStartDate()));
        Assertions.assertTrue(endDate.equals(referenceClock.getEndDate()));
    }

}
