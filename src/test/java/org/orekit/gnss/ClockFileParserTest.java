/* Copyright 2002-2020 CS GROUP
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
package org.orekit.gnss;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.clock.ClockFile;
import org.orekit.gnss.clock.ClockFile.ClockDataLine;
import org.orekit.gnss.clock.ClockFile.ClockDataType;
import org.orekit.gnss.clock.ClockFileParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

/** This class aims at validating the correct IGS clock file parsing and error handling. */
public class ClockFileParserTest {
    
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /** First example given in the 3.04 RINEX clock file format. */
    @Test
    public void testParseExple1V304() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_1_304.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check content of the file
        final double version = 3.04;
        final SatelliteSystem satelliteSystem = SatelliteSystem.GPS;
        final String programName = "TORINEXC V9.9";
        final String agencyName = "USNO";
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
        final int numberOfReferenceClocksTransitions = 1; 
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
        checkClockFileContent(file, version, satelliteSystem, 
                              programName, agencyName, creationDateString, creationTimeString, creationZoneString, creationDate, 
                              numberOfLeapSeconds, numberOfLeapSecondsGPS, numberOfDBCS, numberOfPCVS,
                              numberOfDataTypes, numberOfObservationTypes, frameString,
                              numberOfReceivers, numberOfReferenceClocksTransitions, numberOfSatellites, numberOfDataLines, 
                              id, type, timeScale, dataEpoch, numberOfValues, 
                              clockBias, clockBiasSigma, clockRate, clockRateSigma, clockAcceleration, clockAccelerationSigma);
    }
    
    /** Second example given in the 3.04 RINEX clock file format.
     * PCVS block is not placed where it should be.
     */
    @Test
    public void testParseExple2V304() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/Exple_analysis_2_304.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check content of the file
        final double version = 3.04;
        final SatelliteSystem satelliteSystem = SatelliteSystem.GPS;
        final String programName = "CCLOCK";
        final String agencyName = "IGSACC @ GA & MIT";
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
        final int numberOfReferenceClocksTransitions = 0; 
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
        checkClockFileContent(file, version, satelliteSystem, 
                              programName, agencyName, creationDateString, creationTimeString, creationZoneString, creationDate, 
                              numberOfLeapSeconds, numberOfLeapSecondsGPS, numberOfDBCS, numberOfPCVS,
                              numberOfDataTypes, numberOfObservationTypes, frameString,
                              numberOfReceivers, numberOfReferenceClocksTransitions, numberOfSatellites, numberOfDataLines, 
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
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check content of the file
        final double version = 3.04;
        final SatelliteSystem satelliteSystem = null;
        final String programName = "TORINEXC V9.9";
        final String agencyName = "USNO";
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
        final int numberOfReferenceClocksTransitions = 0; 
        final int numberOfSatellites = 0;
        final int numberOfDataLines = 4;
        final String id = "USNO"; 
        final ClockDataType type = ClockDataType.CR; 
        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final AbsoluteDate dataEpoch = new AbsoluteDate(1995, 7, 14, 22, 23, 14.5, timeScale);
        final int numberOfValues = 2;
        final double clockBias = -0.123456789012E+01; 
        final double clockBiasSigma = 0.123456789012E+00;
        final double clockRate = 0.0;
        final double clockRateSigma = 0.0;
        final double clockAcceleration = 0.0;
        final double clockAccelerationSigma = 0.0;
        checkClockFileContent(file, version, satelliteSystem, 
                              programName, agencyName, creationDateString, creationTimeString, creationZoneString, creationDate, 
                              numberOfLeapSeconds, numberOfLeapSecondsGPS, numberOfDBCS, numberOfPCVS,
                              numberOfDataTypes, numberOfObservationTypes, frameString,
                              numberOfReceivers, numberOfReferenceClocksTransitions, numberOfSatellites, numberOfDataLines, 
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
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(10, file.getTotalNumberOfDataLines());
    }

    /** Another example of the 3.00 RINEX clock file format. 
     * It is espicially missing creation date, time system ID and satellite system.
     * PCVS block is not placed where it should be.
     */
    @Test
    public void testParseExple2V300() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/igr21101_truncated_300.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(7, file.getTotalNumberOfDataLines());
    }
  
    /** An example of the 2.00 RINEX clock file format. */
    @Test
    public void testParseExpleV200() throws URISyntaxException, IOException {
        
        // Parse file
        final String ex = "/gnss/clock/emr10491_truncated_200.clk";
    
        final ClockFileParser parser = new ClockFileParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final ClockFile file = (ClockFile) parser.parse(fileName);
        
        // Check number of data lines
        Assert.assertEquals(7, file.getTotalNumberOfDataLines());
    }

    /** Unsupported clock file version exception throwing test. */
    @Test
    public void testUnsupportedVersion() throws IOException {
        try {
            final String ex = "/gnss/clock/unsupported_clock_file_version.clk";
            final ClockFileParser parser = new ClockFileParser();
            final InputStream inEntry = getClass().getResourceAsStream(ex);
            parser.parse(inEntry);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CLOCK_FILE_UNSUPPORTED_VERSION,
                                oe.getSpecifier());
            Assert.assertEquals("0.05",  Double.toString((double) oe.getParts()[0]));
        }
    }

    /** Wrong clock data type exception throwing test. */
    @Test
    public void testWrongClockDataType() throws IOException {
        try {
            final String ex = "/gnss/clock/wrong_clock_data_type.clk";
            final ClockFileParser parser = new ClockFileParser();
            final InputStream inEntry = getClass().getResourceAsStream(ex);
            parser.parse(inEntry);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_CLOCK_DATA_TYPE,
                                oe.getSpecifier());
            Assert.assertEquals("XX",  oe.getParts()[0]);
        }
    }
    
    /** Check the content of a clock file. */
    private void checkClockFileContent(final ClockFile file,
                                       final double version, final SatelliteSystem satelliteSystem, 
                                       final String programName, final String agencyName,
                                       final String creationDateString, final String creationTimeString, final String creationZoneString,
                                       final AbsoluteDate creationDate, final int numberOfLeapSeconds, final int numberOfLeapSecondsGPS,
                                       final int numberOfDBCS, final int numberOfPCVS,
                                       final int numberOfDataTypes, final int numberOfObservationTypes, final String frameString,
                                       final int numberOfReceivers, final int numberOfReferenceClocksTransitions, final int numberOfSatellites,
                                       final int numberOfDataLines, final String id, final ClockDataType type, final TimeScale timeScale,
                                       final AbsoluteDate dataEpoch, final int numberOfValues,
                                       final double clockBias, final double clockBiasSigma,
                                       final double clockRate, final double clockRateSigma,
                                       final double clockAcceleration, final double clockAccelerationSigma) {
        
        // Check header
        Assert.assertEquals(version, file.getFormatVersion(), 1E-3);
        Assert.assertTrue(file.getSatelliteSystem() == satelliteSystem);
        Assert.assertEquals(programName, file.getProgramName());
        Assert.assertEquals(agencyName, file.getAgencyName());
        Assert.assertEquals(creationDateString, file.getCreationDateString());
        Assert.assertEquals(creationTimeString, file.getCreationTimeString());
        Assert.assertEquals(creationZoneString, file.getCreationTimeZoneString());
        if (null != creationDate) {
            Assert.assertTrue(file.getCreationDate().equals(creationDate));
        }
        Assert.assertEquals(numberOfLeapSeconds, file.getNumberOfLeapSeconds());
        Assert.assertEquals(numberOfLeapSecondsGPS, file.getNumberOfLeapSecondsGNSS());
        Assert.assertEquals(numberOfDBCS, file.getListAppliedDCBS().size());
        Assert.assertEquals(numberOfPCVS, file.getListAppliedPCVS().size());
        Assert.assertEquals(numberOfDataTypes, file.getClockDataTypes().size());
        int observationTypes = 0;
        for (SatelliteSystem system : file.getSystemObservationTypes().keySet()) {
            observationTypes += file.getSystemObservationTypes().get(system).size();
        }
        Assert.assertEquals(numberOfObservationTypes, observationTypes);
        Assert.assertEquals(frameString, file.getFrameName());
        Assert.assertEquals(numberOfReceivers, file.getNumberOfReceivers());
        if (numberOfReferenceClocksTransitions != 0) {
            Assert.assertEquals(numberOfReferenceClocksTransitions, file.getReferenceClocks().getTransitions().size());
        }
        Assert.assertEquals(numberOfSatellites, file.getNumberOfSatellites());
        
        // Check total number of data lines
        Assert.assertEquals(numberOfDataLines, file.getTotalNumberOfDataLines());
        
        // Look for a particular, random data line
        final List<ClockDataLine> clockDataLines = file.getClockData().get(id);
        boolean find = false;
        for (int i = 0; i < clockDataLines.size(); i++) {
            final ClockDataLine clockDataLine = clockDataLines.get(i);
            if (clockDataLine.getEpoch().equals(dataEpoch) &&
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
        
        Assert.assertTrue(find);
        
    }
}
