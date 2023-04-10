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
package org.orekit.files.ccsds.ndm;

import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test class for CCSDS Navigation Data Message writing.<p>
 * @author Luc Maisonobe
 */
public class NdmWriterTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testOpm() throws IOException {
        final String name = "/ccsds/ndm/NDM-opm.xml";
        final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
        final Ndm ndm = new ParserBuilder().buildNdmParser().parseMessage(source);
        final NdmWriter writer = new WriterBuilder().buildNdmWriter();
        final CharArrayWriter caw = new CharArrayWriter();
        try (Generator generator = new XmlGenerator(caw, XmlGenerator.DEFAULT_INDENT, "dummy.xml",
                                                    Constants.JULIAN_DAY, true,
                                                    XmlGenerator.NDM_XML_V3_SCHEMA_LOCATION)) {
            writer.writeMessage(generator, ndm);
        }
        final byte[]      bytes  = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
        NdmTestUtils.checkContainer(ndm, new ParserBuilder().buildNdmParser().parseMessage(source2));
        Assertions.assertTrue(caw.toString().contains("<ndm xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"https://sanaregistry.org/r/ndmxml_unqualified/ndmxml-3.0.0-master-3.0.xsd\">"));
    }

    @Test
    public void testOpmApm() throws IOException {
        final String name = "/ccsds/ndm/NDM-ocm-apm.xml";
        final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
        final Ndm ndm = new ParserBuilder().buildNdmParser().parseMessage(source);
        final NdmWriter writer = new WriterBuilder().buildNdmWriter();
        final CharArrayWriter caw  = new CharArrayWriter();
        try (final Generator generator = new XmlGenerator(caw, XmlGenerator.DEFAULT_INDENT, "dummy.xml",
                                                          Constants.JULIAN_DAY, true, null)) {
            writer.writeMessage(generator, ndm);
        }
        final byte[]      bytes  = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
        NdmTestUtils.checkContainer(ndm, new ParserBuilder().buildNdmParser().parseMessage(source2));
        Assertions.assertTrue(caw.toString().contains("<ndm>"));
    }

    @Test
    public void testMisplacedComments() throws IOException {
        final String name = "/ccsds/ndm/NDM-opm.xml";
        final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
        final Ndm ndm = new ParserBuilder().buildNdmParser().parseMessage(source);
        final NdmWriter writer = new WriterBuilder().buildNdmWriter();
        final CharArrayWriter caw  = new CharArrayWriter();
        try (final Generator generator = new XmlGenerator(caw, XmlGenerator.DEFAULT_INDENT, "dummy.xml",
                                                          Constants.JULIAN_DAY, true,
                                                          XmlGenerator.NDM_XML_V3_SCHEMA_LOCATION)) {
            for (final String comment : ndm.getComments()) {
                writer.writeComment(generator, comment);
            }
            for (final NdmConstituent<?, ?> constituent : ndm.getConstituents()) {
                writer.writeConstituent(generator, constituent);
            }
            try {
                writer.writeComment(generator, "we are not allowed to put comments after constituents");
                Assertions.fail("an exception should have been thrown");
            } catch (OrekitException oe) {
                Assertions.assertEquals(OrekitMessages.ATTEMPT_TO_GENERATE_MALFORMED_FILE, oe.getSpecifier());
                Assertions.assertEquals("dummy.xml", oe.getParts()[0]);
            }
        }
    }

    @Test
    public void testRandomizedNdm() throws IOException {

        final ParserBuilder pb = new ParserBuilder().
                                      withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
                                      withMu(Constants.EIGEN5C_EARTH_MU).
                                      withMissionReferenceDate(new AbsoluteDate("1996-12-17T00:00:00.000", TimeScalesFactory.getUTC()));

        final WriterBuilder wb = new WriterBuilder().
                                 withMissionReferenceDate(pb.getMissionReferenceDate());

        // pool of constituents
        List<NdmConstituent<?, ?>> pool = buildPool(pb);

        RandomGenerator random = new Well19937a(0x4a21ffc6d5b7dbe6l);
        for (int i = 0; i < 100; ++i) {

            final String[] comments = new String[random.nextInt(3)];
            for (int k = 0; k < comments.length; ++k) {
                comments[k] = Integer.toString(random.nextInt());
            }

            final NdmConstituent<?, ?>[] constituents = new NdmConstituent<?,?>[1 + random.nextInt(20)];
            for (int k = 0; k < constituents.length; ++k) {
                constituents[k] = pool.get(random.nextInt(pool.size()));
            }

            // create randomized NDM
            final Ndm original = new Ndm(Arrays.asList(comments), Arrays.asList(constituents));

            // write it
            final CharArrayWriter caw  = new CharArrayWriter();
            try (final Generator generator = new XmlGenerator(caw, XmlGenerator.DEFAULT_INDENT, "dummy.xml",
                                                              Constants.JULIAN_DAY, true,
                                                              XmlGenerator.NDM_XML_V3_SCHEMA_LOCATION)) {
                wb.buildNdmWriter().writeMessage(generator, original);
            }

            // parse the written message back
            final byte[]      bytes  = caw.toString().getBytes(StandardCharsets.UTF_8);
            final DataSource source2 = new DataSource("dummy.xml", () -> new ByteArrayInputStream(bytes));
            final Ndm rebuilt = pb.buildNdmParser().parseMessage(source2);

            // check we recovered the message properly
            NdmTestUtils.checkContainer(original, rebuilt);

        }

    }

    /** build a pool of NdmConstituent.
     * @param builder builder for constituents parsers
     * @return pool of NdmConstituen
     */
    private List<NdmConstituent<?, ?>> buildPool(final ParserBuilder builder) {

        final List<NdmConstituent<?, ?>> pool = new ArrayList<>();

        // AEM files
        for (final String name :
            Arrays.asList("/ccsds/adm/aem/AEMExample01.txt", "/ccsds/adm/aem/AEMExample02.txt", "/ccsds/adm/aem/AEMExample03.txt",
                          "/ccsds/adm/aem/AEMExample03.xml", "/ccsds/adm/aem/AEMExample04.txt", "/ccsds/adm/aem/AEMExample05.txt",
                          "/ccsds/adm/aem/AEMExample07.txt", "/ccsds/adm/aem/AEMExample08.txt", "/ccsds/adm/aem/AEMExample09.txt",
                          "/ccsds/adm/aem/AEMExample10.txt", "/ccsds/adm/aem/AEMExample11.xml", "/ccsds/adm/aem/AEMExample12.txt",
                          "/ccsds/adm/aem/AEMExample13.xml", "/ccsds/adm/aem/AEMExample14.txt", "/ccsds/adm/aem/AEMExample15.txt",
                          "/ccsds/adm/aem/AEMExample16.txt", "/ccsds/adm/aem/AEMExample17.txt")) {
            final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
            pool.add(builder.buildAemParser().parseMessage(source));
        }

        // APM files
        for (final String name :
            Arrays.asList("/ccsds/adm/apm/APMExample01.txt",  "/ccsds/adm/apm/APMExample02.txt",  "/ccsds/adm/apm/APMExample02.xml",
                          "/ccsds/adm/apm/APMExample03.txt",  "/ccsds/adm/apm/APMExample04.txt",  "/ccsds/adm/apm/APMExample05.txt",
                          "/ccsds/adm/apm/APMExample06.txt",  "/ccsds/adm/apm/APMExample07.txt",  "/ccsds/adm/apm/APMExample08.txt",
                          "/ccsds/adm/apm/APMExample09.txt",  "/ccsds/adm/apm/APMExample10.txt",  "/ccsds/adm/apm/APMExample11.txt",
                          "/ccsds/adm/apm/APMExample12.txt")) {
            final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
            pool.add(builder.buildApmParser().parseMessage(source));
        }

        // ACM files
        for (final String name :
            Arrays.asList("/ccsds/adm/acm/ACMExample01.txt",  "/ccsds/adm/acm/ACMExample02.txt",  "/ccsds/adm/acm/ACMExample03.txt",
                          "/ccsds/adm/acm/ACMExample04.txt",  "/ccsds/adm/acm/ACMExample05.txt",  "/ccsds/adm/acm/ACMExample06.txt",
                          "/ccsds/adm/acm/ACMExample07.txt",  "/ccsds/adm/acm/ACMExample08.txt",  "/ccsds/adm/acm/ACMExample09.txt")) {
            final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
            pool.add(builder.buildAcmParser().parseMessage(source));
        }

        // OCM files
        for (final String name :
            Arrays.asList("/ccsds/odm/ocm/OCMExample1.txt",  "/ccsds/odm/ocm/OCMExample2.txt",  "/ccsds/odm/ocm/OCMExample2.xml",
                          "/ccsds/odm/ocm/OCMExample3.txt",  "/ccsds/odm/ocm/OCMExample4.txt")) {
            final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
            pool.add(builder.buildOcmParser().parseMessage(source));
        }

        // OEM files
        for (final String name :
            Arrays.asList("/ccsds/odm/oem/OEMExample1.txt",  "/ccsds/odm/oem/OEMExample2.txt",  "/ccsds/odm/oem/OEMExample3.txt",
                          "/ccsds/odm/oem/OEMExample3.xml",  "/ccsds/odm/oem/OEMExample4.txt",  "/ccsds/odm/oem/OEMExample5.txt",
                          "/ccsds/odm/oem/OEMExample6.txt",  "/ccsds/odm/oem/OEMExample8.txt")) {
            final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
            pool.add(builder.buildOemParser().parseMessage(source));
        }

        // OMM files
        for (final String name :
            Arrays.asList("/ccsds/odm/omm/OMMExample1.txt",  "/ccsds/odm/omm/OMMExample2.txt",  "/ccsds/odm/omm/OMMExample2.xml",
                          "/ccsds/odm/omm/OMMExample3.txt",  "/ccsds/odm/omm/OMMExample4.txt",  "/ccsds/odm/omm/OMMExample4.xml")) {
            final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
            pool.add(builder.buildOmmParser().parseMessage(source));
        }

        // OPM files
        for (final String name :
            Arrays.asList("/ccsds/odm/opm/OPMExample1.txt",  "/ccsds/odm/opm/OPMExample2.txt",  "/ccsds/odm/opm/OPMExample3.txt",
                          "/ccsds/odm/opm/OPMExample3.xml",  "/ccsds/odm/opm/OPMExample4.txt",  "/ccsds/odm/opm/OPMExample5.txt",
                          "/ccsds/odm/opm/OPMExample6.txt",  "/ccsds/odm/opm/OPMExample6.txt",  "/ccsds/odm/opm/OPMExample8.txt")) {
            final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
            pool.add(builder.buildOpmParser().parseMessage(source));
        }

        // TDM files
        for (final String name :
            Arrays.asList("/ccsds/tdm/kvn/TDMExample15.txt", "/ccsds/tdm/kvn/TDMExample2.txt",  "/ccsds/tdm/kvn/TDMExample4.txt",
                          "/ccsds/tdm/kvn/TDMExample6.txt",  "/ccsds/tdm/kvn/TDMExample8.txt",
                          "/ccsds/tdm/xml/TDMExample15.xml", "/ccsds/tdm/xml/TDMExample2.xml",  "/ccsds/tdm/xml/TDMExample4.xml",
                          "/ccsds/tdm/xml/TDMExample6.xml",  "/ccsds/tdm/xml/TDMExample8.xml")) {
            final DataSource source = new DataSource(name, () -> NdmWriterTest.class.getResourceAsStream(name));
            pool.add(builder.buildTdmParser().parseMessage(source));
        }

        return pool;

    }

}
