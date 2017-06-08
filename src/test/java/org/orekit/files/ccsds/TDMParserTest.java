/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.files.ccsds;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.OEMFile.EphemeridesBlock;
import org.orekit.files.ccsds.OEMFile.OemSatelliteEphemeris;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Test class for CCSDS Tracking Data Message parsing.<p>
 * Examples are taken from Annexe D of 
 * <a href="https://public.ccsds.org/Pubs/503x0b1c1.pdf">CCSDS 503.0-B-1 recommended standard [1]</a> ("Tracking Data Message", Blue Book, Version 1.0, November 2007).
 * @author mjournot
 *
 */
public class TDMParserTest {

    @Before
    public void setUp()
        throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseTDMExample2() throws OrekitException, IOException {
        
        // Example 2 of [1]
        // See Figure D-2: TDM Example: One-Way Data w/Frequency Offset
        // Data lines number was cut down to 7
        final String ex = "/ccsds/TDMExample2.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final TDMParser parser = new TDMParser();
        final TDMFile file = parser.parse(inEntry, ex);
        
        final TimeScale utc = TimeScalesFactory.getUTC();
        
        // Header
        Assert.assertEquals(1.0, file.getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate("2005-160T20:15:00", utc).durationFrom(file.getCreationDate()), 0.0, 0.0);
        Assert.assertEquals("NASA/JPL",file.getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        headerComment.add("StarTrek 1-way data, Ka band down");
        Assert.assertEquals(headerComment, file.getHeaderComment());
        
        // Meta-Data
        final TDMFile.TDMMetaData metaData = file.getObservationsBlocks().get(0).getMetaData();
        
        Assert.assertEquals(CcsdsTimeScale.UTC, metaData.getTimeSystem());
        Assert.assertEquals("2005-159T17:41:00", metaData.getStartTimeString());
        Assert.assertEquals(new AbsoluteDate("2005-159T17:41:00", utc).durationFrom(metaData.getStartTime()), 0.0, 0.0);
        Assert.assertEquals("2005-159T17:41:40", metaData.getStopTimeString());
        Assert.assertEquals(new AbsoluteDate("2005-159T17:41:40", utc).durationFrom(metaData.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("DSS-25", metaData.getParticipants().get(1));
        Assert.assertEquals("YYYY-NNNA", metaData.getParticipants().get(2));
        Assert.assertEquals("SEQUENTIAL", metaData.getMode());
        Assert.assertEquals("2,1", metaData.getPath());
        Assert.assertEquals(1.0, metaData.getIntegrationInterval(), 0.0);
        Assert.assertEquals("MIDDLE", metaData.getIntegrationRef());
        Assert.assertEquals(32021035200.0, metaData.getFreqOffset(), 0.0);
        Assert.assertEquals(0.000077, metaData.getTransmitDelays().get(1), 0.0);
        Assert.assertEquals(0.000077, metaData.getReceiveDelays().get(1), 0.0);
        Assert.assertEquals("RAW", metaData.getDataQuality());
        
        // Data
        final List<TDMFile.Observation> observations = file.getObservationsBlocks().get(0).getObservations();
        
        // Reference data
        final String[] keywords = {"TRANSMIT_FREQ_2", "RECEIVE_FREQ_1", "RECEIVE_FREQ_1", "RECEIVE_FREQ_1",
                                   "RECEIVE_FREQ_1", "RECEIVE_FREQ_1", "RECEIVE_FREQ_1"};

        final String[] epochs   = {"2005-159T17:41:00", "2005-159T17:41:00", "2005-159T17:41:01", "2005-159T17:41:02",
                                   "2005-159T17:41:03", "2005-159T17:41:04", "2005-159T17:41:05"};
        
        final double[] values   = {32023442781.733, -409.2735, -371.1568, -333.0551,
                                   -294.9673, -256.9054, -218.7951};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) { 
            Assert.assertEquals(keywords[i], observations.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
    }
    
    @Test
    public void testParseTDMExample4() throws OrekitException, IOException {
        
        // Example 4 of [1]
        // See Figure D-4: TDM Example: Two-Way Ranging Data Only
        // Data lines number was cut down to 20
        final String ex = "/ccsds/TDMExample4.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final TDMParser parser = new TDMParser();
        final TDMFile file = parser.parse(inEntry, ex);
        
        final TimeScale utc = TimeScalesFactory.getUTC();
        
        // Header
        Assert.assertEquals(1.0, file.getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate("2005-191T23:00:00", utc).durationFrom(file.getCreationDate()), 0.0, 0.0);
        Assert.assertEquals("NASA/JPL",file.getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        Assert.assertEquals(headerComment, file.getHeaderComment());
        
        // Meta-Data
        final TDMFile.TDMMetaData metaData = file.getObservationsBlocks().get(0).getMetaData();
        
        Assert.assertEquals(CcsdsTimeScale.UTC, metaData.getTimeSystem());
        Assert.assertEquals("DSS-24", metaData.getParticipants().get(1));
        Assert.assertEquals("YYYY-NNNA", metaData.getParticipants().get(2));
        Assert.assertEquals("SEQUENTIAL", metaData.getMode());
        Assert.assertEquals("1,2,1", metaData.getPath());
        Assert.assertEquals("START", metaData.getIntegrationRef());
        Assert.assertEquals("COHERENT", metaData.getRangeMode());
        Assert.assertEquals(2.0e+26, metaData.getRangeModulus(), 0.0);
        Assert.assertEquals("RU", metaData.getRangeUnits());
        Assert.assertEquals(7.7e-5, metaData.getTransmitDelays().get(1), 0.0);
        Assert.assertEquals(0.0, metaData.getTransmitDelays().get(2), 0.0);
        Assert.assertEquals(7.7e-5, metaData.getReceiveDelays().get(1), 0.0);
        Assert.assertEquals(0.0, metaData.getReceiveDelays().get(2), 0.0);
        Assert.assertEquals(46.7741, metaData.getCorrectionRange(), 0.0);
        Assert.assertEquals("YES", metaData.getCorrectionsApplied());
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("Range correction applied is range calibration to DSS-24.");
        metaDataComment.add("Estimated RTLT at begin of pass = 950 seconds");
        metaDataComment.add("Antenna Z-height correction 0.0545 km applied to uplink signal");
        metaDataComment.add("Antenna Z-height correction 0.0189 km applied to downlink signal");
        Assert.assertEquals(metaDataComment, metaData.getComment());
        
        // Data
        final List<TDMFile.Observation> observations = file.getObservationsBlocks().get(0).getObservations();
        
        // Reference data
        final String[] keywords = {"TRANSMIT_FREQ_1", "TRANSMIT_FREQ_RATE_1", "RANGE", "PR_N0",
                                   "TRANSMIT_FREQ_1", "TRANSMIT_FREQ_RATE_1", "RANGE", "PR_N0",
                                   "TRANSMIT_FREQ_1", "TRANSMIT_FREQ_RATE_1", "RANGE", "PR_N0",
                                   "TRANSMIT_FREQ_1", "TRANSMIT_FREQ_RATE_1", "RANGE", "PR_N0"};

        final String[] epochs   = {"2005-191T00:31:51", "2005-191T00:31:51", "2005-191T00:31:51", "2005-191T00:31:51",
                                   "2005-191T00:34:48", "2005-191T00:34:48", "2005-191T00:34:48", "2005-191T00:34:48",
                                   "2005-191T00:37:45", "2005-191T00:37:45", "2005-191T00:37:45", "2005-191T00:37:45",
                                   "2005-191T00:40:42", "2005-191T00:40:42", "2005-191T00:40:42", "2005-191T00:40:42",
                                   "2005-191T00:58:24", "2005-191T00:58:24", "2005-191T00:58:24", "2005-191T00:58:24"};
        
        final double[] values   = {7180064367.3536 , 0.59299, 39242998.5151986, 28.52538,
                                   7180064472.3146 , 0.59305, 61172265.3115234, 28.39347,
                                   7180064577.2756 , 0.59299, 15998108.8168328, 28.16193,
                                   7180064682.2366 , 0.59299, 37938284.4138008, 29.44597,
                                   7180065327.56141, 0.62085, 35478729.4012973, 30.48199};

        // Check consistency
        for (int i = 0; i < keywords.length; i++) { 
            Assert.assertEquals(keywords[i], observations.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
    }

    @Test
    public void testParseTDMExample6() throws OrekitException, IOException {
        
        // Example 6 of [1]
        // See Figure D-6: TDM Example: Four-Way Data
        // Data lines number was cut down to 16
        final String ex = "/ccsds/TDMExample6.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final TDMParser parser = new TDMParser();
        final TDMFile file = parser.parse(inEntry, ex);
        
        final TimeScale utc = TimeScalesFactory.getUTC();
        
        // Header
        Assert.assertEquals(1.0, file.getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate("1998-06-10T01:00:00", utc).durationFrom(file.getCreationDate()), 0.0, 0.0);
        Assert.assertEquals("JAXA",file.getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (JAXA)");
        Assert.assertEquals(headerComment, file.getHeaderComment());
        
        // Meta-Data
        final TDMFile.TDMMetaData metaData = file.getObservationsBlocks().get(0).getMetaData();
        
        Assert.assertEquals(CcsdsTimeScale.UTC, metaData.getTimeSystem());
        Assert.assertEquals("1998-06-10T00:57:37", metaData.getStartTimeString());
        Assert.assertEquals(new AbsoluteDate("1998-06-10T00:57:37", utc).durationFrom(metaData.getStartTime()), 0.0, 0.0);
        Assert.assertEquals("1998-06-10T00:57:44", metaData.getStopTimeString());
        Assert.assertEquals(new AbsoluteDate("1998-06-10T00:57:44", utc).durationFrom(metaData.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("NORTH", metaData.getParticipants().get(1));
        Assert.assertEquals("F07R07", metaData.getParticipants().get(2));
        Assert.assertEquals("E7", metaData.getParticipants().get(3));
        Assert.assertEquals("SEQUENTIAL", metaData.getMode());
        Assert.assertEquals("1,2,3,2,1", metaData.getPath());
        Assert.assertEquals(1.0, metaData.getIntegrationInterval(), 0.0);
        Assert.assertEquals("MIDDLE", metaData.getIntegrationRef());
        Assert.assertEquals("CONSTANT", metaData.getRangeMode());
        Assert.assertEquals(0.0, metaData.getRangeModulus(), 0.0);
        Assert.assertEquals("KM", metaData.getRangeUnits());
        Assert.assertEquals("AZEL", metaData.getAngleType());
        
        // Data
        final List<TDMFile.Observation> observations = file.getObservationsBlocks().get(0).getObservations();
        
        // Reference data
        final String[] keywords = {"RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",
                                   "RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",
                                   "RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",
                                   "RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",};

        final String[] epochs   = {"1998-06-10T00:57:37", "1998-06-10T00:57:37", "1998-06-10T00:57:37", "1998-06-10T00:57:37", "1998-06-10T00:57:37", 
                                   "1998-06-10T00:57:38", "1998-06-10T00:57:38", "1998-06-10T00:57:38", "1998-06-10T00:57:38", "1998-06-10T00:57:38",
                                   "1998-06-10T00:57:39", "1998-06-10T00:57:39", "1998-06-10T00:57:39", "1998-06-10T00:57:39", "1998-06-10T00:57:39",
                                   "1998-06-10T00:57:44", "1998-06-10T00:57:44", "1998-06-10T00:57:44", "1998-06-10T00:57:44", "1998-06-10T00:57:44",};
        
        final double[] values   = { 80452.7542, 256.64002393, 13.38100016, 2106395199.07917, 2287487999.0,
                                    80452.7368, 256.64002393, 13.38100016, 2106395199.07917, 2287487999.0,
                                    80452.7197, 256.64002393, 13.38100016, 2106395199.07917, 2287487999.0,
                                    80452.6331, 256.64002393, 13.38100016, 2106395199.07917, 2287487999.0};

        // Check consistency
        for (int i = 0; i < keywords.length; i++) { 
            Assert.assertEquals(keywords[i], observations.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
    }

    @Test
    public void testParseTDMExample8() throws OrekitException, IOException {
        
        // Example 8 of [1]
        // See Figure D-8: TDM Example: Angles, Range, Doppler Combined in Single TDM
        // Data lines number was cut down to 18
        final String ex = "/ccsds/TDMExample8.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final TDMParser parser = new TDMParser();
        final TDMFile file = parser.parse(inEntry, ex);
        
        final TimeScale utc = TimeScalesFactory.getUTC();
        
        // Header
        Assert.assertEquals(1.0, file.getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate("2007-08-30T12:01:44.749", utc).durationFrom(file.getCreationDate()), 0.0, 0.0);
        Assert.assertEquals("GSOC",file.getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("GEOSCX_INP");
        Assert.assertEquals(headerComment, file.getHeaderComment());
        
        // Meta-Data 1
        final TDMFile.TDMMetaData metaData = file.getObservationsBlocks().get(0).getMetaData();
        
        Assert.assertEquals(CcsdsTimeScale.UTC, metaData.getTimeSystem());
        Assert.assertEquals("2007-08-29T07:00:02.000", metaData.getStartTimeString());
        Assert.assertEquals(new AbsoluteDate("2007-08-29T07:00:02.000", utc).durationFrom(metaData.getStartTime()), 0.0, 0.0);
        Assert.assertEquals("2007-08-29T14:00:02.000", metaData.getStopTimeString());
        Assert.assertEquals(new AbsoluteDate("2007-08-29T14:00:02.000", utc).durationFrom(metaData.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("HBSTK", metaData.getParticipants().get(1));
        Assert.assertEquals("SAT", metaData.getParticipants().get(2));
        Assert.assertEquals("SEQUENTIAL", metaData.getMode());
        Assert.assertEquals("1,2,1", metaData.getPath());
        Assert.assertEquals(1.0, metaData.getIntegrationInterval(), 0.0);
        Assert.assertEquals("END", metaData.getIntegrationRef());
        Assert.assertEquals("XSYE", metaData.getAngleType());
        Assert.assertEquals("RAW", metaData.getDataQuality());
        
        // Data 1
        final List<TDMFile.Observation> observations = file.getObservationsBlocks().get(0).getObservations();
        
        // Reference data 1
        final String[] keywords = {"DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
                                   "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
                                   "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2"};

        final String[] epochs   = {"2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000",
                                   "2007-08-29T08:00:02.000", "2007-08-29T08:00:02.000", "2007-08-29T08:00:02.000",
                                   "2007-08-29T14:00:02.000", "2007-08-29T14:00:02.000", "2007-08-29T14:00:02.000"};
        
        final double[] values   = {-1.498776048, 67.01312389, 18.28395556,
                                   -2.201305217, 67.01982278, 21.19609167,
                                   0.929545817, -89.35626083, 2.78791667};

        // Check consistency
        for (int i = 0; i < keywords.length; i++) { 
            Assert.assertEquals(keywords[i], observations.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
        
        // Meta-Data 2
        final TDMFile.TDMMetaData metaData2 = file.getObservationsBlocks().get(1).getMetaData();
        
        Assert.assertEquals(CcsdsTimeScale.UTC, metaData2.getTimeSystem());
        Assert.assertEquals("2007-08-29T06:00:02.000", metaData2.getStartTimeString());
        Assert.assertEquals(new AbsoluteDate("2007-08-29T06:00:02.000", utc).durationFrom(metaData2.getStartTime()), 0.0, 0.0);
        Assert.assertEquals("2007-08-29T13:00:02.000", metaData2.getStopTimeString());
        Assert.assertEquals(new AbsoluteDate("2007-08-29T13:00:02.000", utc).durationFrom(metaData2.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("WHM1", metaData2.getParticipants().get(1));
        Assert.assertEquals("SAT", metaData2.getParticipants().get(2));
        Assert.assertEquals("SEQUENTIAL", metaData2.getMode());
        Assert.assertEquals("1,2,1", metaData2.getPath());
        Assert.assertEquals(1.0, metaData2.getIntegrationInterval(), 0.0);
        Assert.assertEquals("END", metaData2.getIntegrationRef());
        Assert.assertEquals("AZEL", metaData2.getAngleType());
        Assert.assertEquals("RAW", metaData2.getDataQuality());
        
        // Data 2
        final List<TDMFile.Observation> observations2 = file.getObservationsBlocks().get(1).getObservations();
        
        // Reference data 2
        final String[] keywords2 = {"RANGE", "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
                                    "RANGE", "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
                                    "RANGE", "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2"};

        final String[] epochs2   = {"2007-08-29T06:00:02.000", "2007-08-29T06:00:02.000", "2007-08-29T06:00:02.000", "2007-08-29T06:00:02.000",
                                    "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000",
                                    "2007-08-29T13:00:02.000", "2007-08-29T13:00:02.000", "2007-08-29T13:00:02.000", "2007-08-29T13:00:02.000"};
        
        final double[] values2   = {4.00165248953670E+04, -0.885640091,  99.53204250, 1.26724167,
                                    3.57238793591890E+04, -1.510223139, 103.33061750, 4.77875278,
                                    3.48156855860090E+04,  1.504082291, 243.73365222, 8.78254167};

        // Check consistency
        for (int i = 0; i < keywords2.length; i++) { 
            Assert.assertEquals(keywords2[i], observations2.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs2[i], utc).durationFrom(observations2.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values2[i], observations2.get(i).getMeasurement(), 0.0);
        }
    }
    
    @Test
    public void testParseTDMExample15() throws OrekitException, IOException {
        
        // Example 15 of [1]
        // See Figure D-15: TDM Example: Clock Bias/Drift Only
        final String ex = "/ccsds/TDMExample15.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final TDMParser parser = new TDMParser();
        final TDMFile file = parser.parse(inEntry, ex);
        
        final TimeScale utc = TimeScalesFactory.getUTC();
        
        // Header
        Assert.assertEquals(1.0, file.getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate("2005-161T15:45:00", utc).durationFrom(file.getCreationDate()), 0.0, 0.0);
        Assert.assertEquals("NASA/JPL",file.getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        headerComment.add("The following are clock offsets, in seconds between the");
        headerComment.add("clocks at each DSN complex relative to UTC(NIST). The offset");
        headerComment.add("is a mean of readings using several GPS space vehicles in");
        headerComment.add("common view. Value is \"station clock minus UTC”.");
        Assert.assertEquals(headerComment, file.getHeaderComment());
        
        // Meta-Data 1
        final TDMFile.TDMMetaData metaData = file.getObservationsBlocks().get(0).getMetaData();
        
        Assert.assertEquals(CcsdsTimeScale.UTC, metaData.getTimeSystem());
        Assert.assertEquals("2005-142T12:00:00", metaData.getStartTimeString());
        Assert.assertEquals(new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metaData.getStartTime()), 0.0, 0.0);
        Assert.assertEquals("2005-145T12:00:00", metaData.getStopTimeString());
        Assert.assertEquals(new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metaData.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("DSS-10", metaData.getParticipants().get(1));
        Assert.assertEquals("UTC-NIST", metaData.getParticipants().get(2));
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("Note: SPC10 switched back to Maser1 from Maser2 on 2005-142");
        Assert.assertEquals(metaDataComment, metaData.getComment());
        
        // Data 1
        final List<TDMFile.Observation> observations = file.getObservationsBlocks().get(0).getObservations();
        
        // Reference data 1
        final String[] keywords = {"CLOCK_BIAS", "CLOCK_DRIFT",
                                   "CLOCK_BIAS", "CLOCK_DRIFT",
                                   "CLOCK_BIAS", "CLOCK_DRIFT",
                                   "CLOCK_BIAS"};

        final String[] epochs   = {"2005-142T12:00:00", "2005-142T12:00:00",
                                   "2005-143T12:00:00", "2005-143T12:00:00",
                                   "2005-144T12:00:00", "2005-144T12:00:00",
                                   "2005-145T12:00:00"};
        
        final double[] values   = {9.56e-7,  6.944e-14,
                                   9.62e-7, -2.083e-13,
                                   9.44e-7, -2.778e-13,
                                   9.20e-7};

        // Check consistency
        for (int i = 0; i < keywords.length; i++) { 
            Assert.assertEquals(keywords[i], observations.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
        
        // Meta-Data 2
        final TDMFile.TDMMetaData metaData2 = file.getObservationsBlocks().get(1).getMetaData();
        
        Assert.assertEquals(CcsdsTimeScale.UTC, metaData2.getTimeSystem());
        Assert.assertEquals("2005-142T12:00:00", metaData2.getStartTimeString());
        Assert.assertEquals(new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metaData2.getStartTime()), 0.0, 0.0);
        Assert.assertEquals("2005-145T12:00:00", metaData2.getStopTimeString());
        Assert.assertEquals(new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metaData2.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("DSS-40", metaData2.getParticipants().get(1));
        Assert.assertEquals("UTC-NIST", metaData2.getParticipants().get(2));
        
        // Data 2
        final List<TDMFile.Observation> observations2 = file.getObservationsBlocks().get(1).getObservations();
        
        // Reference data 2
        // Same keywords and dates than 1        
        final double[] values2   = {-7.40e-7, -3.125e-13,
                                    -7.67e-7, -1.620e-13,
                                    -7.81e-7, -4.745e-13,
                                    -8.22e-7};

        // Check consistency
        for (int i = 0; i < keywords.length; i++) { 
            Assert.assertEquals(keywords[i], observations2.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations2.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values2[i], observations2.get(i).getMeasurement(), 0.0);
        }
        
        // Meta-Data 3
        final TDMFile.TDMMetaData metaData3 = file.getObservationsBlocks().get(2).getMetaData();
        
        Assert.assertEquals(CcsdsTimeScale.UTC, metaData3.getTimeSystem());
        Assert.assertEquals("2005-142T12:00:00", metaData3.getStartTimeString());
        Assert.assertEquals(new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metaData3.getStartTime()), 0.0, 0.0);
        Assert.assertEquals("2005-145T12:00:00", metaData3.getStopTimeString());
        Assert.assertEquals(new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metaData3.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("DSS-60", metaData3.getParticipants().get(1));
        Assert.assertEquals("UTC-NIST", metaData3.getParticipants().get(2));
        
        // Data 2
        final List<TDMFile.Observation> observations3 = file.getObservationsBlocks().get(2).getObservations();
        
        // Reference data 2
        // Same keywords and dates than 1        
        final double[] values3   = {-1.782e-6, 1.736e-13,
                                    -1.767e-6, 1.157e-14,
                                    -1.766e-6, 8.102e-14,
                                    -1.759e-6};

        // Check consistency
        for (int i = 0; i < keywords.length; i++) { 
            Assert.assertEquals(keywords[i], observations3.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations3.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values3[i], observations3.get(i).getMeasurement(), 0.0);
        }
    }
    
    @Test
    public void testDataNumberFormatErrorType() {
        try {
            // Number format exception in data part
            final String ex = "/ccsds/TDM-data-number-format-error.txt";
            final InputStream inEntry = getClass().getResourceAsStream(ex);
            final TDMParser parser = new TDMParser();
            parser.parse(inEntry, ex);
            Assert.fail("An Orekit Exception \"UNABLE_TO_PARSE_LINE_IN_FILE\" should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(26, oe.getParts()[0]);
            Assert.assertEquals("/ccsds/TDM-data-number-format-error.txt", oe.getParts()[1]);
            Assert.assertEquals("RECEIVE_FREQ_1 = 2005-159T17:41:03 this-is-not-a-number", oe.getParts()[2]);
        }
    }
    
    @Test
    public void testMetaDataNumberFormatErrorType() {
        try {
            // Number format exception in metadata part
            final String ex = "/ccsds/TDM-metadata-number-format-error.txt";
            final InputStream inEntry = getClass().getResourceAsStream(ex);
            final TDMParser parser = new TDMParser();
            parser.parse(inEntry, ex);
            Assert.fail("An Orekit Exception \"UNABLE_TO_PARSE_LINE_IN_FILE\" should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(17, oe.getParts()[0]);
            Assert.assertEquals("/ccsds/TDM-metadata-number-format-error.txt", oe.getParts()[1]);
            Assert.assertEquals("TRANSMIT_DELAY_1 = this-is-not-a-number", oe.getParts()[2]);
        }
    }
    
    @Test
    public void testNonExistentFile() throws URISyntaxException {
        // Try parsing a file that does not exist
        final String realName = getClass().getResource("/ccsds/OEMExample2.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        try {
            new TDMParser().parse(wrongName);
            Assert.fail("An Orekit Exception \"UNABLE_TO_FIND_FILE\" should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }
    
    @Test
    public void testInconsistentTimeSystems() {
        // Inconsistent time systems between two sets of data
        try {
            new TDMParser().parse(getClass().getResourceAsStream("/ccsds/TDM-inconsistent-time-systems.txt"));
            Assert.fail("An Orekit Exception \"CCSDS_TDM_INCONSISTENT_TIME_SYSTEMS\" should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_TDM_INCONSISTENT_TIME_SYSTEMS, oe.getSpecifier());
            Assert.assertEquals(CcsdsTimeScale.UTC, oe.getParts()[0]);
            Assert.assertEquals(CcsdsTimeScale.TCG, oe.getParts()[1]);
        }
    }
    
    @Test
    public void testWrongDataKeyword()
        throws OrekitException, URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String ex = "/ccsds/TDM-data-wrong-keyword.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        //final String name = getClass().getResource(ex).toURI().getPath();
        try {
            //TDMFile file = new TDMParser().parse(name);
            new TDMParser().parse(inEntry, ex);
            Assert.fail("An exception \"CCSDS_UNEXPECTED_KEYWORD\"should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(26, oe.getParts()[0]);
            Assert.assertEquals("/ccsds/TDM-data-wrong-keyword.txt", oe.getParts()[1]);
            Assert.assertEquals("WRONG_KEYWORD  = 2005-159T17:41:03 -294.9673", oe.getParts()[2]);
        }
    }
    
    @Test
    public void testWrongMetaDataKeyword()
        throws OrekitException, URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String ex = "/ccsds/TDM-metadata-wrong-keyword.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        try {
            new TDMParser().parse(inEntry, ex);
            Assert.fail("An exception \"CCSDS_UNEXPECTED_KEYWORD\"should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(16, oe.getParts()[0]);
            Assert.assertEquals("/ccsds/TDM-metadata-wrong-keyword.txt", oe.getParts()[1]);
            Assert.assertEquals("WRONG_KEYWORD = 32021035200.0", oe.getParts()[2]);
        }
    }
    
    @Test
    public void testWrongTimeSystem() {
        // Time system not implemented CCSDS keyword was read in data part
        final String ex = "/ccsds/TDM-metadata-timesystem-not-implemented.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        try {
            new TDMParser().parse(inEntry, ex);
            Assert.fail("An exception \"CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED\"should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals("WRONG-TIME-SYSTEM", oe.getParts()[0]);
        }
    }
    
    @Test
    public void testInconsistentDataLine() {
        // Inconsistent line in date (3 fields after keyword instead of one
        final String ex = "/ccsds/TDM-data-inconsistent-line.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        try {
            new TDMParser().parse(inEntry, ex);
            Assert.fail("An exception \"CCSDS_TDM_INCONSISTENT_DATA_LINE\"should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_TDM_INCONSISTENT_DATA_LINE, oe.getSpecifier());
            Assert.assertEquals(25, oe.getParts()[0]);
            Assert.assertEquals("/ccsds/TDM-data-inconsistent-line.txt", oe.getParts()[1]);
            Assert.assertEquals("RECEIVE_FREQ_1 = 2005-159T17:41:02 -333.0551 this-should-not-be-here", oe.getParts()[2]);
        }
    }
}
