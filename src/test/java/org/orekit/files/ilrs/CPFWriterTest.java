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
package org.orekit.files.ilrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.OrekitEphemerisFile;
import org.orekit.files.general.OrekitEphemerisFile.OrekitSatelliteEphemeris;
import org.orekit.files.ilrs.CPF.CPFCoordinate;
import org.orekit.files.ilrs.CPF.CPFEphemeris;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class CPFWriterTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testWriteJason3Version2() throws IOException, URISyntaxException {

        // Simple test for version 2.0, only contains position entries
        final String ex = "/ilrs/jason3_cpf_180613_16401.cne";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF file = new CPFParser().parse(source);

        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader(), TimeScalesFactory.getUTC());
        writer.write(tempCPFFilePath, file);

        final CPF generatedCpfFile = new CPFParser().parse(new DataSource(tempCPFFilePath));
        compareCpfFiles(file, generatedCpfFile);

    }

    @Test
    public void testWriteLageos1Version2() throws IOException, URISyntaxException {

        // Simple test for version 2.0, only contains position entries
        final String ex = "/ilrs/lageos1_cpf_180613_16401.hts";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF file = new CPFParser().parse(source);

        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader(), TimeScalesFactory.getUTC());
        writer.write(tempCPFFilePath, file);

        final CPF generatedCpfFile = new CPFParser().parse(new DataSource(tempCPFFilePath));
        compareCpfFiles(file, generatedCpfFile);

    }

    @Test
    public void testWriteGalileoVersion1() throws IOException, URISyntaxException {

        // Simple test for version 1.0, only contains position entries
        final String ex = "/ilrs/galileo212_cpf_180613_6641.esa";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF file = new CPFParser().parse(source);

        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader(), TimeScalesFactory.getUTC());
        writer.write(tempCPFFilePath, file);

        final CPF generatedCpfFile = new CPFParser().parse(new DataSource(tempCPFFilePath));
        compareCpfFiles(file, generatedCpfFile);

    }

    @Test
    public void testIssue868v1() throws IOException, URISyntaxException {

        // Load
        final String ex = "/ilrs/galileo212_cpf_180613_6641.esa";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF file = new CPFParser().parse(source);

        // Write
        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader(), TimeScalesFactory.getUTC());
        writer.write(tempCPFFilePath, file);

        // Verify
        final DataSource tempSource = new DataSource(tempCPFFilePath);
        try (Reader reader = tempSource.getOpener().openReaderOnce();
                        BufferedReader br = (reader == null) ? null : new BufferedReader(reader)) {
            // The testWriteGalileoVersion1() already verify the content of the file
            // The objective here is just the verify the fix of issue #868
            final String line1 = br.readLine();
            Assert.assertEquals(56, line1.length());
            final String line2 = br.readLine();
            Assert.assertEquals(82, line2.length());
        }
    }

    @Test
    public void testIssue868v2() throws IOException, URISyntaxException {

        // Load
        final String ex = "/ilrs/lageos1_cpf_180613_16401.hts";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF file = new CPFParser().parse(source);

        // Write
        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader(), TimeScalesFactory.getUTC());
        writer.write(tempCPFFilePath, file);

        // Verify
        final DataSource tempSource = new DataSource(tempCPFFilePath);
        try (Reader reader = tempSource.getOpener().openReaderOnce();
                        BufferedReader br = (reader == null) ? null : new BufferedReader(reader)) {
            // The testWriteLageos1Version2() already verify the content of the file
            // The objective here is just the verify the fix of issue #868
            final String line1 = br.readLine();
            Assert.assertEquals(58, line1.length());
            final String line2 = br.readLine();
            Assert.assertEquals(85, line2.length());
        }
    }

    @Test
    public void testNullFile() throws IOException {
        final String    ex      = "/ilrs/lageos1_cpf_180613_16401.hts";
        final DataSource source  = new DataSource(ex, () ->  getClass().getResourceAsStream(ex));
        final CPF   cpfFile = new CPFParser().parse(source);
        final CPFWriter writer  = new CPFWriter(cpfFile.getHeader(), TimeScalesFactory.getUTC());
        try {
            writer.write((BufferedWriter) null, cpfFile);
            fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            assertEquals(OrekitMessages.NULL_ARGUMENT, oiae.getSpecifier());
            assertEquals("writer", oiae.getParts()[0]);
        }
    }

    @Test
    public void testNullEphemeris() throws IOException {
        File tempCPFFile = tempFolder.newFile("TestNullEphemeris.cpf");
        CPFWriter writer = new CPFWriter(null, TimeScalesFactory.getUTC());
        writer.write(tempCPFFile.toString(), null);
        assertTrue(tempCPFFile.exists());
        try (FileInputStream   fis = new FileInputStream(tempCPFFile);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            int count = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++count;
            }
            assertEquals(0, count);
        }
    }

    /** Test for issue #844 (https://gitlab.orekit.org/orekit/orekit/-/issues/844). */
    @Test
    public void testIssue844() throws IOException {

        // Create header
        final CPFHeader header = new CPFHeader();
        header.setSource("orekit");
        header.setStep(300);
        header.setStartEpoch(AbsoluteDate.J2000_EPOCH.shiftedBy(-300.0));
        header.setEndEpoch(AbsoluteDate.J2000_EPOCH.shiftedBy(300.0));
        header.setIlrsSatelliteId("070595");
        header.setName("tag");
        header.setNoradId("0705");
        header.setProductionEpoch(new DateComponents(2000, 1, 2));
        header.setProductionHour(12);
        header.setSequenceNumber(0705);
        header.setSic("0705");
        final CPFHeader headerV1 = header;
        headerV1.setVersion(1);

        // Writer
        final CPFWriter writer = new CPFWriter(headerV1, TimeScalesFactory.getUTC());

        // Create an empty CPF file
        final CPF cpf = new CPF();

        // Fast check
        assertEquals(0, cpf.getSatellites().size());

        // Add coordinates
        final int leap = 0;
        cpf.addSatelliteCoordinate(header.getIlrsSatelliteId(), new CPFCoordinate(AbsoluteDate.J2000_EPOCH.shiftedBy(-300.0), Vector3D.PLUS_I, leap));
        cpf.addSatelliteCoordinate(header.getIlrsSatelliteId(), new CPFCoordinate(AbsoluteDate.J2000_EPOCH,                   Vector3D.PLUS_J, leap));
        cpf.addSatelliteCoordinate(header.getIlrsSatelliteId(), new CPFCoordinate(AbsoluteDate.J2000_EPOCH.shiftedBy(300.0),  Vector3D.PLUS_K, leap));

        // Write the file
        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        writer.write(tempCPFFilePath, cpf);

        // Verify
        final List<CPFCoordinate> coordinatesInFile = cpf.getSatellites().get(header.getIlrsSatelliteId()).getCoordinates();
        assertEquals(0.0, Vector3D.PLUS_I.distance(coordinatesInFile.get(0).getPosition()), 1.0e-10);
        assertEquals(0.0, Vector3D.PLUS_J.distance(coordinatesInFile.get(1).getPosition()), 1.0e-10);
        assertEquals(0.0, Vector3D.PLUS_K.distance(coordinatesInFile.get(2).getPosition()), 1.0e-10);

    }

    /** Test for issue #844 (https://gitlab.orekit.org/orekit/orekit/-/issues/844). */
    @Test
    public void testIssue844Bis() throws IOException {

        // Create header
        final CPFHeader header = new CPFHeader();
        header.setSource("orekit");
        header.setStep(300);
        header.setStartEpoch(AbsoluteDate.J2000_EPOCH.shiftedBy(-300.0));
        header.setEndEpoch(AbsoluteDate.J2000_EPOCH.shiftedBy(300.0));
        header.setIlrsSatelliteId("070595");
        header.setName("tag");
        header.setNoradId("0705");
        header.setProductionEpoch(new DateComponents(2000, 1, 2));
        header.setProductionHour(12);
        header.setSequenceNumber(0705);
        header.setSic("0705");
        final CPFHeader headerV1 = header;
        headerV1.setVersion(1);

        // Writer
        final CPFWriter writer = new CPFWriter(headerV1, TimeScalesFactory.getUTC());

        // Create an empty CPF file
        final CPF cpf = new CPF();

        // Fast check
        assertEquals(0, cpf.getSatellites().size());

        // Add coordinates
        final int leap = 0;
        final List<CPFCoordinate> coordinates = new ArrayList<>();
        coordinates.add(new CPFCoordinate(AbsoluteDate.J2000_EPOCH.shiftedBy(-300.0), Vector3D.PLUS_I, leap));
        coordinates.add(new CPFCoordinate(AbsoluteDate.J2000_EPOCH,                   Vector3D.PLUS_J, leap));
        coordinates.add(new CPFCoordinate(AbsoluteDate.J2000_EPOCH.shiftedBy(300.0),  Vector3D.PLUS_K, leap));
        cpf.addSatelliteCoordinates(header.getIlrsSatelliteId(), coordinates);

        // Write the file
        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        writer.write(tempCPFFilePath, cpf);

        // Verify
        final List<CPFCoordinate> coordinatesInFile = cpf.getSatellites().get(header.getIlrsSatelliteId()).getCoordinates();
        assertEquals(0.0, Vector3D.PLUS_I.distance(coordinatesInFile.get(0).getPosition()), 1.0e-10);
        assertEquals(0.0, Vector3D.PLUS_J.distance(coordinatesInFile.get(1).getPosition()), 1.0e-10);
        assertEquals(0.0, Vector3D.PLUS_K.distance(coordinatesInFile.get(2).getPosition()), 1.0e-10);

    }
    
    /** Test for issue #790 (https://gitlab.orekit.org/orekit/orekit/-/issues/790). 
     * @throws URISyntaxException */
    @Test
    public void testIssue790() throws IOException, URISyntaxException {

    	 final TimeScale utc = TimeScalesFactory.getUTC();
         final AbsoluteDate date = new AbsoluteDate(2022, 05, 10, 00, 00, 00.000, utc);

         // General orbit
         double a = 24396159;                     // semi major axis in meters
         double e = 0.72831215;                   // eccentricity
         double i = FastMath.toRadians(7);        // inclination
         double omega = FastMath.toRadians(180);  // perigee argument
         double raan = FastMath.toRadians(261);   // right ascension of ascending node
         double lM = 0;                           // mean anomaly

         Orbit orbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
         									   FramesFactory.getEME2000(), date, 
         									   Constants.WGS84_EARTH_MU);
         
         // First, configure a propagation with the default event handler (expected to stop on event)
         Propagator propagator = new KeplerianPropagator(orbit);
         
         final double propagationDurationSeconds = 86400.0;
         final double stepSizeSeconds = 60.0;
         List<SpacecraftState> states = new ArrayList<SpacecraftState>();

         for (double dt = 0.0; dt < propagationDurationSeconds; dt += stepSizeSeconds) {
             states.add(propagator.propagate(date.shiftedBy(dt)));
         }

         OrekitEphemerisFile ephemerisFile = new OrekitEphemerisFile();
         OrekitSatelliteEphemeris satellite = ephemerisFile.addSatellite("070595");
         satellite.addNewSegment(states);

        // Create header
        final CPFHeader header = new CPFHeader();
        header.setSource("orekit");
        header.setStep(300);
        header.setStartEpoch(date);
        header.setEndEpoch(date.shiftedBy(86400.0));
        header.setIlrsSatelliteId("070595");
        header.setName("tag");
        header.setNoradId("0705");
        header.setProductionEpoch(date.getComponents(utc).getDate());
        header.setProductionHour(12);
        header.setSequenceNumber(0705);
        header.setSic("0705");
        final CPFHeader headerV1 = header;
        headerV1.setVersion(1);

        // First launch the test with velocity flag enabled
        boolean velocityFlag = true;
        
        // Write the CPF file from the generated ephemeris
        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(headerV1, TimeScalesFactory.getUTC(), velocityFlag);
        writer.write(tempCPFFilePath, ephemerisFile);
        
        // Parse the generated CPF file
        final CPF generatedCpfFile = new CPFParser().parse(new DataSource(tempCPFFilePath));
        
        
        // Extract the coordinates from the generated CPF file
        final CPFEphemeris ephemeris    = generatedCpfFile.getSatellites().get("070595");
        final List<CPFCoordinate> coord = ephemeris.getCoordinates();

        // Verify first coordinate and that it includes the velocity components
        final AbsoluteDate firstEpoch = AbsoluteDate.createMJDDate(59709, 0.0, generatedCpfFile.getTimeScale());
        final Vector3D firstPos = new Vector3D(1036869.533, 6546536.585, 0.000);
        final Vector3D firstVel = new Vector3D(-9994.355199, 1582.950355, -1242.449125);
        Assert.assertEquals(0, coord.get(0).getLeap());
        Assert.assertEquals(0.0, firstPos.distance(coord.get(0).getPosition()), 1.0e-15);
        Assert.assertEquals(0.0, firstVel.distance(coord.get(0).getVelocity()), 1.0e-15);
        Assert.assertEquals(0.0, firstEpoch.durationFrom(coord.get(0).getDate()), 1.0e-15);
        
        
        // Repeat without velocity components for regression testing
        
        // Write the CPF file from the generated ephemeris
        String tempCPFFilePathReg = tempFolder.newFile("TestWriteCPFReg.cpf").toString();
        CPFWriter writerReg = new CPFWriter(headerV1, TimeScalesFactory.getUTC());
        writerReg.write(tempCPFFilePathReg, ephemerisFile);
        
        // Parse the generated CPF file
        final CPF generatedCpfFileReg = new CPFParser().parse(new DataSource(tempCPFFilePathReg));
        
        
        // Extract the coordinates from the generated CPF file
        final CPFEphemeris ephemerisReg    = generatedCpfFileReg.getSatellites().get("070595");
        final List<CPFCoordinate> coordReg = ephemerisReg.getCoordinates();

        // Verify first coordinate and that the velocity components are zero
        Assert.assertEquals(0, coordReg.get(0).getLeap());
        Assert.assertEquals(0.0, firstPos.distance(coordReg.get(0).getPosition()), 1.0e-15);
        Assert.assertEquals(0.0, coordReg.get(0).getVelocity().getNorm(), 1.0e-15);
        Assert.assertEquals(0.0, firstEpoch.durationFrom(coordReg.get(0).getDate()), 1.0e-15);
        
    }

    @Test
    @Deprecated
    public void testDefaultId() throws IOException {

        // Initialize
        final CPF cpf = new CPF();

        // Fast check
        assertEquals(0, cpf.getSatellites().size());

        // Add coordinates
        final int leap = 0;
        cpf.addSatelliteCoordinate(new CPFCoordinate(AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I, leap));

        // Verify
        assertEquals(1, cpf.getSatellites().size());

    }

    @Test
    @Deprecated
    public void testOldConstructor() throws IOException {

        // Initialize
        // Create an empty CPF file
        final CPF cpf = new CPF();
        final CPFEphemeris ephemeris = cpf.new CPFEphemeris();

        // Fast check
        assertEquals(0, ephemeris.getCoordinates().size());
        assertEquals(CPF.DEFAULT_ID, ephemeris.getId());

    }

    public static void compareCpfFiles(CPF file1, CPF file2) {

        // Header
        final CPFHeader header1 = file1.getHeader();
        final CPFHeader header2 = file2.getHeader();
        compareCpfHeader(header1, header2);

        // Ephemeris
        final CPFEphemeris eph1 = file1.getSatellites().get(header1.getIlrsSatelliteId());
        final CPFEphemeris eph2 = file2.getSatellites().get(header2.getIlrsSatelliteId());
        Assert.assertEquals(eph1.getId(), eph2.getId());
        Assert.assertEquals(eph1.getStart(), eph2.getStart());
        Assert.assertEquals(eph1.getStop(), eph2.getStop());

        // Coordinates
        final List<CPFCoordinate> coord1 = eph1.getCoordinates();
        final List<CPFCoordinate> coord2 = eph2.getCoordinates();
        Assert.assertEquals(coord1.size(), coord1.size());
        verifyEphemerisLine(coord1.get(0), coord2.get(0));
        verifyEphemerisLine(coord1.get(1), coord2.get(1));
        verifyEphemerisLine(coord1.get(100), coord2.get(100));
        verifyEphemerisLine(coord1.get(coord1.size() - 1), coord2.get(coord2.size() - 1));

    }

    public static void compareCpfHeader(CPFHeader header1, CPFHeader header2) {
        Assert.assertEquals(header1.getFormat(), header2.getFormat());
        Assert.assertEquals(header1.getVersion(), header2.getVersion());
        Assert.assertEquals(header1.getSource(), header2.getSource());
        Assert.assertEquals(header1.getProductionEpoch().getYear(), header2.getProductionEpoch().getYear());
        Assert.assertEquals(header1.getName(), header2.getName());
        Assert.assertEquals(header1.getIlrsSatelliteId(), header2.getIlrsSatelliteId());
        Assert.assertEquals(header1.getSic(), header2.getSic());
        Assert.assertEquals(0.0, header1.getStartEpoch().durationFrom(header2.getStartEpoch()), 1.0e-15);
        Assert.assertEquals(0.0, header1.getEndEpoch().durationFrom(header2.getEndEpoch()), 1.0e-15);
    }

    public static void verifyEphemerisLine(CPFCoordinate coord1, CPFCoordinate coord2) {
        Assert.assertEquals(0.0, coord1.getDate().durationFrom(coord2.getDate()), 1.0e-10);
        Assert.assertEquals(0.0, coord1.getPosition().distance(coord2.getPosition()), 1.0e-10);
    }

}
