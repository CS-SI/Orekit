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
package org.orekit.files.ilrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.orekit.Utils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ilrs.CPFFile.CPFCoordinate;
import org.orekit.files.ilrs.CPFFile.CPFEphemeris;

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

        final CPFParser parser = new CPFParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final CPFFile file = (CPFFile) parser.parse(fileName);

        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader());
        writer.write(tempCPFFilePath, file);

        final CPFFile generatedCpfFile = parser.parse(tempCPFFilePath);
        compareCpfFiles(file, generatedCpfFile);

    }

    @Test
    public void testWriteLageos1Version2() throws IOException, URISyntaxException {

        // Simple test for version 2.0, only contains position entries
        final String ex = "/ilrs/lageos1_cpf_180613_16401.hts";

        final CPFParser parser = new CPFParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final CPFFile file = (CPFFile) parser.parse(fileName);

        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader());
        writer.write(tempCPFFilePath, file);

        final CPFFile generatedCpfFile = parser.parse(tempCPFFilePath);
        compareCpfFiles(file, generatedCpfFile);

    }

    @Test
    public void testWriteGalileoVersion1() throws IOException, URISyntaxException {

        // Simple test for version 1.0, only contains position entries
        final String ex = "/ilrs/galileo212_cpf_180613_6641.esa";

        final CPFParser parser = new CPFParser();
        final String fileName = Paths.get(getClass().getResource(ex).toURI()).toString();
        final CPFFile file = (CPFFile) parser.parse(fileName);

        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader());
        writer.write(tempCPFFilePath, file);

        final CPFFile generatedCpfFile = parser.parse(tempCPFFilePath);
        compareCpfFiles(file, generatedCpfFile);

    }

    @Test
    public void testNullFile() throws IOException {
        final String ex = "/ilrs/lageos1_cpf_180613_16401.hts";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final CPFParser parser = new CPFParser();
        final CPFFile cpfFile = parser.parse(inEntry);
        CPFWriter writer = new CPFWriter(cpfFile.getHeader());
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
        CPFWriter writer = new CPFWriter(null);
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

    public static void compareCpfFiles(CPFFile file1, CPFFile file2) {

        // Header
        final CPFHeader header1 = file1.getHeader();
        final CPFHeader header2 = file2.getHeader();
        compareCpfHeader(header1, header2);

        // Ephemeris
        final CPFEphemeris eph1 = file1.getSatellites().get(header1.getIlrsSatelliteId());
        final CPFEphemeris eph2 = file2.getSatellites().get(header2.getIlrsSatelliteId());
        Assert.assertEquals(eph1.getId(), eph2.getId());
        Assert.assertEquals(eph1.getTimeScale(), eph2.getTimeScale());
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
    }

    public static void verifyEphemerisLine(CPFCoordinate coord1, CPFCoordinate coord2) {
        Assert.assertEquals(0.0, coord1.getDate().durationFrom(coord2.getDate()), 1.0e-10);
        Assert.assertEquals(0.0, coord1.getPosition().distance(coord2.getPosition()), 1.0e-10);
    }

}
