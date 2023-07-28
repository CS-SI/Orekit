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
package org.orekit.files.ccsds.ndm.tdm;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test class for CCSDS Tracking Data Message parsing.<p>
 * Examples are taken from Annexe D of
 * <a href="https://public.ccsds.org/Pubs/503x0b1c1.pdf">CCSDS 503.0-B-1 recommended standard [1]</a> ("Tracking Data Message", Blue Book, Version 1.0, November 2007).<p>
 * Both KeyValue and XML formats are tested here on equivalent files.
 * @author mjournot
 *
 */
public class TdmParserTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseTdmExternalResourceIssue368() {
        // setup
        final String name = "/ccsds/tdm/xml/TDM-external-doctype.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));

        try {
            // action
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);

            // verify
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            // Malformed URL exception indicates external resource was disabled
            // file not found exception indicates parser tried to load the resource
            MatcherAssert.assertThat(e.getCause(),
                    CoreMatchers.instanceOf(MalformedURLException.class));
        }
    }

    @Test
    public void testParseTdmKeyValueExample2() {
        // Example 2 of [1]
        // See Figure D-2: TDM Example: One-Way Data w/Frequency Offset
        // Data lines number was cut down to 7
        final String name = "/ccsds/tdm/kvn/TDMExample2.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample2(file);
    }

    @Test
    public void testParseTdmKeyValueExample4() {

        // Example 4 of [1]
        // See Figure D-4: TDM Example: Two-Way Ranging Data Only
        // Data lines number was cut down to 20
        final String name = "/ccsds/tdm/kvn/TDMExample4.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExample4(file);
    }

    @Test
    public void testParseTdmKeyValueExample6() {

        // Example 6 of [1]
        // See Figure D-6: TDM Example: Four-Way Data
        // Data lines number was cut down to 16
        final String name = "/ccsds/tdm/kvn/TDMExample6.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample6(file);
    }

    @Test
    public void testParseTdmKeyValueExample8() {

        // Example 8 of [1]
        // See Figure D-8: TDM Example: Angles, Range, Doppler Combined in Single TDM
        // Data lines number was cut down to 18
        final String name = "/ccsds/tdm/kvn/TDMExample8.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExample8(file);
    }

    @Test
    public void testParseTdmKeyValueExample15() {

        // Example 15 of [1]
        // See Figure D-15: TDM Example: Clock Bias/Drift Only
        final String name = "/ccsds/tdm/kvn/TDMExample15.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample15(file);
    }

    @Test
    public void testParseTdmKeyValueExampleAllKeywordsSequential() {

        // Testing all TDM keywords
        final String name = "/ccsds/tdm/kvn/TDMExampleAllKeywordsSequential.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExampleAllKeywordsSequential(file);
    }

    @Test
    public void testParseTdmKeyValueExampleAllKeywordsSingleDiff() {

        // Testing all TDM keywords
        final String name = "/ccsds/tdm/kvn/TDMExampleAllKeywordsSingleDiff.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExampleAllKeywordsSingleDiff(file);
    }

    @Test
    public void testParseTdmXmlExample2() {

        // Example 2 of [1]
        // See Figure D-2: TDM Example: One-Way Data w/Frequency Offset
        // Data lines number was cut down to 7
        final String name = "/ccsds/tdm/xml/TDMExample2.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample2(file);
    }

    @Test
    public void testWriteTdmXmlExample2() throws IOException {

        // Example 2 of [1]
        // See Figure D-2: TDM Example: One-Way Data w/Frequency Offset
        // Data lines number was cut down to 7
        final String name = "/ccsds/tdm/xml/TDMExample2.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm original = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);

        // write the parsed file back to a characters array
        final CharArrayWriter caw = new CharArrayWriter();
        final Generator generator = new KvnGenerator(caw, TdmWriter.KVN_PADDING_WIDTH, "dummy",
                                                     Constants.JULIAN_DAY, 60);
        new WriterBuilder().withRangeUnitsConverter(null).buildTdmWriter().writeMessage(generator, original);

        // reparse the written file
        final byte[]     bytes   = caw.toString().getBytes(StandardCharsets.UTF_8);
        final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
        final Tdm    rebuilt = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source2);
        validateTDMExample2(rebuilt);

    }

    @Test
    public void testParseTdmXmlExample4() {

        // Example 4 of [1]
        // See Figure D-4: TDM Example: Two-Way Ranging Data Only
        // Data lines number was cut down to 20
        final String name = "/ccsds/tdm/xml/TDMExample4.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExample4(file);
    }

    @Test
    public void testParseTdmXmlExample6() {

        // Example 6 of [1]
        // See Figure D-6: TDM Example: Four-Way Data
        // Data lines number was cut down to 16
        final String name = "/ccsds/tdm/xml/TDMExample6.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample6(file);
    }

    @Test
    public void testParseTdmXmlExample8() {

        // Example 8 of [1]
        // See Figure D-8: TDM Example: Angles, Range, Doppler Combined in Single TDM
        // Data lines number was cut down to 18
        final String name = "/ccsds/tdm/xml/TDMExample8.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExample8(file);
    }

    @Test
    public void testParseTdmXmlExample15() {

        // Example 15 of [1]
        // See Figure D-15: TDM Example: Clock Bias/Drift Only
        final String name = "/ccsds/tdm/xml/TDMExample15.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample15(file);
    }

    @Test
    public void testIssue963() {

        // Check that a TDM with spaces in between participants in PATH is rejected
        final String name = "/ccsds/tdm/kvn/TDM-issue963.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            // Number format exception in metadata part
            new ParserBuilder().buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
        }
    }

    @Test
    public void testParseTdmXmlExampleAllKeywordsSequential() {

        // Testing all TDM keywords
        final String name = "/ccsds/tdm/xml/TDMExampleAllKeywordsSequential.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExampleAllKeywordsSequential(file);
    }

    @Test
    public void testParseTdmXmlExampleAllKeywordsSingleDiff() {

        // Testing all TDM keywords
        final String name = "/ccsds/tdm/xml/TDMExampleAllKeywordsSingleDiff.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExampleAllKeywordsSingleDiff(file);
    }

    @Test
    public void testDataNumberFormatErrorTypeKeyValue() {
        final String name = "/ccsds/tdm/kvn/TDM-data-number-format-error.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            // Number format exception in data part
            new ParserBuilder().buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals("RECEIVE_FREQ_1", oe.getParts()[0]);
            Assertions.assertEquals(26, oe.getParts()[1]);
            Assertions.assertEquals(name, oe.getParts()[2]);
        }
    }

    @Test
    public void testDataNumberFormatErrorTypeXml() {
        try {
            // Number format exception in data part
            final String name = "/ccsds/tdm/xml/TDM-data-number-format-error.xml";
            final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
            new ParserBuilder().buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals("RECEIVE_FREQ_1", oe.getParts()[0]);
            Assertions.assertEquals(47, oe.getParts()[1]);
            Assertions.assertEquals("/ccsds/tdm/xml/TDM-data-number-format-error.xml", oe.getParts()[2]);
        }
    }

    @Test
    public void testMetaDataNumberFormatErrorTypeKeyValue() {
        try {
            // Number format exception in metadata part
            final String name = "/ccsds/tdm/kvn/TDM-metadata-number-format-error.txt";
            final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An Orekit Exception \"UNABLE_TO_PARSE_LINE_IN_FILE\" should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals("TRANSMIT_DELAY_1", oe.getParts()[0]);
            Assertions.assertEquals(17, oe.getParts()[1]);
            Assertions.assertEquals("/ccsds/tdm/kvn/TDM-metadata-number-format-error.txt", oe.getParts()[2]);
        }
    }

    @Test
    public void testMetaDataNumberFormatErrorTypeXml() {
        try {
            // Number format exception in metadata part
            final String name = "/ccsds/tdm/xml/TDM-metadata-number-format-error.xml";
            final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals("TRANSMIT_DELAY_1", oe.getParts()[0]);
            Assertions.assertEquals(24, oe.getParts()[1]);
            Assertions.assertEquals("/ccsds/tdm/xml/TDM-metadata-number-format-error.xml", oe.getParts()[2]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        // Try parsing a file that does not exist
        final String realName = "/ccsds/odm/oem/OEMExample2.txt";
        final String wrongName = realName + "xxxxx";
        final DataSource source = new DataSource(wrongName, () -> TdmParserTest.class.getResourceAsStream(wrongName));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testInconsistentTimeSystemsKeyValue() {
        // Inconsistent time systems between two sets of data
        final String name = "/ccsds/tdm/kvn/TDM-inconsistent-time-systems.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        Assertions.assertEquals(3, file.getSegments().size());
        Assertions.assertEquals(TimeSystem.UTC, file.getSegments().get(0).getMetadata().getTimeSystem());
        Assertions.assertEquals(TimeSystem.TCG, file.getSegments().get(1).getMetadata().getTimeSystem());
        Assertions.assertEquals(TimeSystem.UTC, file.getSegments().get(2).getMetadata().getTimeSystem());
    }

    @Test
    public void testInconsistentTimeSystemsXml() {
        // Inconsistent time systems between two sets of data
        final String name = "/ccsds/tdm/xml/TDM-inconsistent-time-systems.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        Assertions.assertEquals(3, file.getSegments().size());
        Assertions.assertEquals(TimeSystem.UTC, file.getSegments().get(0).getMetadata().getTimeSystem());
        Assertions.assertEquals(TimeSystem.TCG, file.getSegments().get(1).getMetadata().getTimeSystem());
        Assertions.assertEquals(TimeSystem.UTC, file.getSegments().get(2).getMetadata().getTimeSystem());
    }

    @Test
    public void testWrongDataKeywordKeyValue() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/kvn/TDM-data-wrong-keyword.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(26, oe.getParts()[0]);
            Assertions.assertEquals("/ccsds/tdm/kvn/TDM-data-wrong-keyword.txt", oe.getParts()[1], "%s");
            Assertions.assertEquals("WRONG_KEYWORD", oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongDataKeywordXml() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-data-wrong-keyword.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(47, oe.getParts()[0]);
            Assertions.assertEquals(name, oe.getParts()[1]);
            Assertions.assertEquals("WRONG_KEYWORD", oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongMetaDataKeywordKeyValue() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/kvn/TDM-metadata-wrong-keyword.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(16, oe.getParts()[0]);
            Assertions.assertEquals("/ccsds/tdm/kvn/TDM-metadata-wrong-keyword.txt", oe.getParts()[1]);
            Assertions.assertEquals("WRONG_KEYWORD", oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongMetaDataKeywordXml() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-metadata-wrong-keyword.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(23, oe.getParts()[0]);
            Assertions.assertEquals("/ccsds/tdm/xml/TDM-metadata-wrong-keyword.xml", oe.getParts()[1]);
            Assertions.assertEquals("WRONG_KEYWORD", oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongTimeSystemKeyValue() {
        // Time system not implemented CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/kvn/TDM-metadata-timesystem-not-implemented.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, oe.getSpecifier());
            Assertions.assertEquals("WRONG-TIME-SYSTEM", oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongTimeSystemXml() {
        // Time system not implemented CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-metadata-timesystem-not-implemented.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, oe.getSpecifier());
            Assertions.assertEquals("WRONG-TIME-SYSTEM", oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingTimeSystemXml() {
        // Time system not implemented CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-missing-timesystem.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_READ_YET, oe.getSpecifier());
            Assertions.assertEquals(18, oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingPArticipants() {
        final String name = "/ccsds/tdm/xml/TDM-missing-participants.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assertions.assertEquals(TdmMetadataKey.PARTICIPANT_1, oe.getParts()[0]);
        }
    }

    @Test
    public void testInconsistentDataLineKeyValue() {
        // Inconsistent data line in KeyValue file (3 fields after keyword instead of 2)
        final String name = "/ccsds/tdm/kvn/TDM-data-inconsistent-line.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals("RECEIVE_FREQ_1", oe.getParts()[0]);
            Assertions.assertEquals(25, oe.getParts()[1]);
            Assertions.assertEquals("/ccsds/tdm/kvn/TDM-data-inconsistent-line.txt", oe.getParts()[2]);
        }
    }

    @Test
    public void testInconsistentDataBlockXml() {
        // Inconsistent data block in XML file
        final String name = "/ccsds/tdm/xml/TDM-data-inconsistent-block.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals("TRANSMIT_FREQ_2", oe.getParts()[0]);
            Assertions.assertEquals(32, oe.getParts()[1]);
            Assertions.assertEquals("/ccsds/tdm/xml/TDM-data-inconsistent-block.xml", oe.getParts()[2]);
        }
    }

    /**
     * Validation function for example 2.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExample2(Tdm file) {
        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assertions.assertEquals(new AbsoluteDate("2005-160T20:15:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assertions.assertEquals("NASA/JPL",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        headerComment.add("StarTrek 1-way data, Ka band down");
        Assertions.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();

        Assertions.assertEquals("UTC", metadata.getTimeSystem().name());
        Assertions.assertEquals(0.0, new AbsoluteDate("2005-159T17:41:00", utc).durationFrom(metadata.getStartTime()), 0.0);
        Assertions.assertEquals(0.0, new AbsoluteDate("2005-159T17:41:40", utc).durationFrom(metadata.getStopTime()), 0.0);
        Assertions.assertEquals("DSS-25", metadata.getParticipants().get(1));
        Assertions.assertEquals("yyyy-nnnA", metadata.getParticipants().get(2));
        Assertions.assertEquals(TrackingMode.SEQUENTIAL, metadata.getMode());
        Assertions.assertArrayEquals(new int[] { 2, 1 }, metadata.getPath());
        Assertions.assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        Assertions.assertEquals(IntegrationReference.MIDDLE, metadata.getIntegrationRef());
        Assertions.assertEquals(32021035200.0, metadata.getFreqOffset(), 0.0);
        Assertions.assertEquals(0.000077, metadata.getTransmitDelays().get(1), 0.0);
        Assertions.assertEquals(0.000077, metadata.getReceiveDelays().get(1), 0.0);
        Assertions.assertEquals(DataQuality.RAW, metadata.getDataQuality());
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("This is a meta-data comment");
        Assertions.assertEquals(metaDataComment, metadata.getComments());

        // Data
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

        // Reference data
        final String[] keywords = {"TRANSMIT_FREQ_2", "RECEIVE_FREQ_1", "RECEIVE_FREQ_1", "RECEIVE_FREQ_1",
            "RECEIVE_FREQ_1", "RECEIVE_FREQ_1", "RECEIVE_FREQ_1"};

        final String[] epochs   = {"2005-159T17:41:00", "2005-159T17:41:00", "2005-159T17:41:01", "2005-159T17:41:02",
            "2005-159T17:41:03", "2005-159T17:41:04", "2005-159T17:41:05"};

        final double[] values   = {32023442781.733, -409.2735, -371.1568, -333.0551,
            -294.9673, -256.9054, -218.7951};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assertions.assertEquals(keywords[i], observations.get(i).getType().name());
            Assertions.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assertions.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }

        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        Assertions.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());

        // check so global setters that are not used by parser (it uses successive add instead)

        metadata.setParticipants(Collections.singletonMap(12, "p12"));
        Assertions.assertNull(metadata.getParticipants().get(1));
        Assertions.assertEquals("p12", metadata.getParticipants().get(12));
        metadata.setTransmitDelays(Collections.singletonMap(12, 1.25));
        Assertions.assertNull(metadata.getTransmitDelays().get(1));
        Assertions.assertEquals(1.25, metadata.getTransmitDelays().get(12).doubleValue(), 1.0e-15);
        metadata.setReceiveDelays(Collections.singletonMap(12, 2.5));
        Assertions.assertNull(metadata.getReceiveDelays().get(1));
        Assertions.assertEquals(2.5, metadata.getReceiveDelays().get(12).doubleValue(), 1.0e-15);

    }

    /**
     * Validation function for example 4.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExample4(Tdm file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assertions.assertEquals(new AbsoluteDate("2005-191T23:00:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assertions.assertEquals("NASA/JPL",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        Assertions.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();

        Assertions.assertEquals("UTC", metadata.getTimeSystem().name());
        Assertions.assertEquals("DSS-24", metadata.getParticipants().get(1));
        Assertions.assertEquals("yyyy-nnnA", metadata.getParticipants().get(2));
        Assertions.assertEquals(TrackingMode.SEQUENTIAL, metadata.getMode());
        Assertions.assertArrayEquals(new int[] { 1, 2, 1 }, metadata.getPath());
        Assertions.assertEquals(IntegrationReference.START, metadata.getIntegrationRef());
        Assertions.assertEquals(RangeMode.COHERENT, metadata.getRangeMode());
        Assertions.assertEquals(2.0e+26, metadata.getRawRangeModulus(), 0.0);
        Assertions.assertEquals(2.0e+26, metadata.getRangeModulus(new IdentityConverter()), 0.0);
        Assertions.assertEquals(RangeUnits.RU, metadata.getRangeUnits());
        Assertions.assertEquals(7.7e-5, metadata.getTransmitDelays().get(1), 0.0);
        Assertions.assertEquals(0.0, metadata.getTransmitDelays().get(2), 0.0);
        Assertions.assertEquals(7.7e-5, metadata.getReceiveDelays().get(1), 0.0);
        Assertions.assertEquals(0.0, metadata.getReceiveDelays().get(2), 0.0);
        Assertions.assertEquals(46.7741, metadata.getCorrectionRange(new IdentityConverter()), 0.0);
        Assertions.assertEquals(46.7741, metadata.getRawCorrectionRange(), 0.0);
        Assertions.assertEquals(CorrectionApplied.YES, metadata.getCorrectionsApplied());
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("Range correction applied is range calibration to DSS-24.");
        metaDataComment.add("Estimated RTLT at begin of pass = 950 seconds");
        metaDataComment.add("Antenna Z-height correction 0.0545 km applied to uplink signal");
        metaDataComment.add("Antenna Z-height correction 0.0189 km applied to downlink signal");
        Assertions.assertEquals(metaDataComment, metadata.getComments());

        // Data
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

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
            Assertions.assertEquals(keywords[i], observations.get(i).getType().name());
            Assertions.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assertions.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        Assertions.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());
    }

    /**
     * Validation function for example 6.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExample6(Tdm file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assertions.assertEquals(new AbsoluteDate("1998-06-10T01:00:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assertions.assertEquals("JAXA",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (JAXA)");
        Assertions.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();

        Assertions.assertEquals("UTC", metadata.getTimeSystem().name());
        Assertions.assertEquals(new AbsoluteDate("1998-06-10T00:57:37", utc).durationFrom(metadata.getStartTime()), 0.0, 0.0);
        Assertions.assertEquals(new AbsoluteDate("1998-06-10T00:57:44", utc).durationFrom(metadata.getStopTime()), 0.0, 0.0);
        Assertions.assertEquals("NORTH", metadata.getParticipants().get(1));
        Assertions.assertEquals("F07R07", metadata.getParticipants().get(2));
        Assertions.assertEquals("E7", metadata.getParticipants().get(3));
        Assertions.assertEquals(TrackingMode.SEQUENTIAL, metadata.getMode());
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 2, 1 }, metadata.getPath());
        Assertions.assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        Assertions.assertEquals(IntegrationReference.MIDDLE, metadata.getIntegrationRef());
        Assertions.assertEquals(RangeMode.CONSTANT, metadata.getRangeMode());
        Assertions.assertEquals(1.0, metadata.getRawRangeModulus(), 0.0);
        Assertions.assertEquals(1000.0, metadata.getRangeModulus(new IdentityConverter()), 0.0);
        Assertions.assertEquals(RangeUnits.km, metadata.getRangeUnits());
        Assertions.assertEquals(AngleType.AZEL, metadata.getAngleType());
        Assertions.assertEquals(2.0, metadata.getRawCorrectionRange(), 0.0);
        Assertions.assertEquals(2000.0, metadata.getCorrectionRange(new IdentityConverter()), 0.0);
        Assertions.assertEquals(CorrectionApplied.YES, metadata.getCorrectionsApplied());

        // Data
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

        // Reference data
        final String[] keywords = {"RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",
            "RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",
            "RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",
            "RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",};

        final String[] epochs   = {"1998-06-10T00:57:37", "1998-06-10T00:57:37", "1998-06-10T00:57:37", "1998-06-10T00:57:37", "1998-06-10T00:57:37",
            "1998-06-10T00:57:38", "1998-06-10T00:57:38", "1998-06-10T00:57:38", "1998-06-10T00:57:38", "1998-06-10T00:57:38",
            "1998-06-10T00:57:39", "1998-06-10T00:57:39", "1998-06-10T00:57:39", "1998-06-10T00:57:39", "1998-06-10T00:57:39",
            "1998-06-10T00:57:44", "1998-06-10T00:57:44", "1998-06-10T00:57:44", "1998-06-10T00:57:44", "1998-06-10T00:57:44",};

        final double[] values   = { 80452754.2, FastMath.toRadians(256.64002393), FastMath.toRadians(13.38100016), 2106395199.07917, 2287487999.0,
            80452736.8, FastMath.toRadians(256.64002393), FastMath.toRadians(13.38100016), 2106395199.07917, 2287487999.0,
            80452719.7, FastMath.toRadians(256.64002393), FastMath.toRadians(13.38100016), 2106395199.07917, 2287487999.0,
            80452633.1, FastMath.toRadians(256.64002393), FastMath.toRadians(13.38100016), 2106395199.07917, 2287487999.0};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assertions.assertEquals(keywords[i], observations.get(i).getType().name());
            Assertions.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assertions.assertEquals(values[i], observations.get(i).getMeasurement(), 1.0e-12 * FastMath.abs(values[i]));
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        Assertions.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());
    }

    /**
     * Validation function for example 8.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExample8(Tdm file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assertions.assertEquals(new AbsoluteDate("2007-08-30T12:01:44.749", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assertions.assertEquals("GSOC",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("GEOSCX INP");
        Assertions.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data 1
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();

        Assertions.assertEquals("UTC", metadata.getTimeSystem().name());
        Assertions.assertEquals(new AbsoluteDate("2007-08-29T07:00:02.000", utc).durationFrom(metadata.getStartTime()), 0.0, 0.0);
        Assertions.assertEquals(new AbsoluteDate("2007-08-29T14:00:02.000", utc).durationFrom(metadata.getStopTime()), 0.0, 0.0);
        Assertions.assertEquals("HBSTK", metadata.getParticipants().get(1));
        Assertions.assertEquals("SAT", metadata.getParticipants().get(2));
        Assertions.assertEquals(TrackingMode.SEQUENTIAL, metadata.getMode());
        Assertions.assertArrayEquals(new int[] { 1, 2, 1 }, metadata.getPath());
        Assertions.assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        Assertions.assertEquals(IntegrationReference.END, metadata.getIntegrationRef());
        Assertions.assertEquals(AngleType.XSYE, metadata.getAngleType());
        Assertions.assertEquals(DataQuality.RAW, metadata.getDataQuality());
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("This is a meta-data comment");
        Assertions.assertEquals(metaDataComment, metadata.getComments());

        // Data 1
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

        // Reference data 1
        final String[] keywords = {"DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
            "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
            "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2"};

        final String[] epochs   = {"2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000",
            "2007-08-29T08:00:02.000", "2007-08-29T08:00:02.000", "2007-08-29T08:00:02.000",
            "2007-08-29T14:00:02.000", "2007-08-29T14:00:02.000", "2007-08-29T14:00:02.000"};

        final double[] values   = {-1498.776048, FastMath.toRadians(67.01312389), FastMath.toRadians(18.28395556),
            -2201.305217, FastMath.toRadians(67.01982278), FastMath.toRadians(21.19609167),
            929.545817, FastMath.toRadians(-89.35626083), FastMath.toRadians(2.78791667)};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assertions.assertEquals(keywords[i], observations.get(i).getType().name());
            Assertions.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assertions.assertEquals(values[i], observations.get(i).getMeasurement(), 1.0e-12 * FastMath.abs(values[i]));
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        Assertions.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());

        // Meta-Data 2
        final TdmMetadata metadata2 = file.getSegments().get(1).getMetadata();

        Assertions.assertEquals("UTC", metadata.getTimeSystem().name());
        Assertions.assertEquals(new AbsoluteDate("2007-08-29T06:00:02.000", utc).durationFrom(metadata2.getStartTime()), 0.0, 0.0);
        Assertions.assertEquals(new AbsoluteDate("2007-08-29T13:00:02.000", utc).durationFrom(metadata2.getStopTime()), 0.0, 0.0);
        Assertions.assertEquals("WHM1", metadata2.getParticipants().get(1));
        Assertions.assertEquals("SAT", metadata2.getParticipants().get(2));
        Assertions.assertEquals(TrackingMode.SEQUENTIAL, metadata2.getMode());
        Assertions.assertArrayEquals(new int[] { 1, 2, 1 }, metadata2.getPath());
        Assertions.assertEquals(1.0, metadata2.getIntegrationInterval(), 0.0);
        Assertions.assertEquals(IntegrationReference.END, metadata2.getIntegrationRef());
        Assertions.assertEquals(1.0e7, metadata2.getRawRangeModulus(), 0.0);
        Assertions.assertEquals(1.0e7 * Constants.SPEED_OF_LIGHT, metadata2.getRangeModulus(new IdentityConverter()), 0.0);
        Assertions.assertEquals(RangeUnits.s, metadata2.getRangeUnits());
        Assertions.assertEquals(AngleType.AZEL, metadata2.getAngleType());
        Assertions.assertEquals(DataQuality.RAW, metadata2.getDataQuality());
        Assertions.assertEquals(2.0, metadata2.getRawCorrectionRange(), 0.0);
        Assertions.assertEquals(2.0 * Constants.SPEED_OF_LIGHT, metadata2.getCorrectionRange(new IdentityConverter()), 0.0);
        Assertions.assertEquals(CorrectionApplied.YES, metadata2.getCorrectionsApplied());
        final List<String> metaDataComment2 = new ArrayList<String>();
        metaDataComment2.add("This is a meta-data comment");
        Assertions.assertEquals(metaDataComment2, metadata2.getComments());

        // Data 2
        final List<Observation> observations2 = file.getSegments().get(1).getData().getObservations();

        // Reference data 2
        final String[] keywords2 = {"RANGE", "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
            "RANGE", "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
            "RANGE", "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2"};

        final String[] epochs2   = {"2007-08-29T06:00:02.000", "2007-08-29T06:00:02.000", "2007-08-29T06:00:02.000", "2007-08-29T06:00:02.000",
            "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000",
            "2007-08-29T13:00:02.000", "2007-08-29T13:00:02.000", "2007-08-29T13:00:02.000", "2007-08-29T13:00:02.000"};

        final double[] values2   = {4.00165248953670E+04 * Constants.SPEED_OF_LIGHT, -885.640091,  FastMath.toRadians(99.53204250), FastMath.toRadians(1.26724167),
            3.57238793591890E+04 * Constants.SPEED_OF_LIGHT, -1510.223139, FastMath.toRadians(103.33061750), FastMath.toRadians(4.77875278),
            3.48156855860090E+04 * Constants.SPEED_OF_LIGHT,  1504.082291, FastMath.toRadians(243.73365222), FastMath.toRadians(8.78254167)};
        // Check consistency
        for (int i = 0; i < keywords2.length; i++) {
            Assertions.assertEquals(keywords2[i], observations2.get(i).getType().name());
            Assertions.assertEquals(new AbsoluteDate(epochs2[i], utc).durationFrom(observations2.get(i).getEpoch()), 0.0, 0.0);
            Assertions.assertEquals(values2[i], observations2.get(i).getMeasurement(), 1.0e-12 * FastMath.abs(values2[i]));
        }
        // Comment
        final List<String> dataComment2 = new ArrayList<String>();
        dataComment2.add("This is a data comment");
        Assertions.assertEquals(dataComment2, file.getSegments().get(1).getData().getComments());
    }

    /**
     * Validation function for example 15.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExample15(Tdm file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assertions.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assertions.assertEquals(new AbsoluteDate("2005-161T15:45:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assertions.assertEquals("NASA/JPL",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        headerComment.add("The following are clock offsets, in seconds between the");
        headerComment.add("clocks at each DSN complex relative to UTC(NIST). The offset");
        headerComment.add("is a mean of readings using several GPS space vehicles in");
        headerComment.add("common view. Value is \"station clock minus UTC”.");
        Assertions.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data 1
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();

        Assertions.assertEquals("UTC", metadata.getTimeSystem().name());
        Assertions.assertEquals(new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metadata.getStartTime()), 0.0, 0.0);
        Assertions.assertEquals(new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metadata.getStopTime()), 0.0, 0.0);
        Assertions.assertEquals("DSS-10", metadata.getParticipants().get(1));
        Assertions.assertEquals("UTC-NIST", metadata.getParticipants().get(2));
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("Note: SPC10 switched back to Maser1 from Maser2 on 2005-142");
        Assertions.assertEquals(metaDataComment, metadata.getComments());

        // Data 1
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

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
            Assertions.assertEquals(keywords[i], observations.get(i).getType().name());
            Assertions.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assertions.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        Assertions.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());


        // Meta-Data 2
        final TdmMetadata metadata2 = file.getSegments().get(1).getMetadata();

        Assertions.assertEquals("UTC", metadata2.getTimeSystem().name());
        Assertions.assertEquals(new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metadata2.getStartTime()), 0.0, 0.0);
        Assertions.assertEquals(new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metadata2.getStopTime()), 0.0, 0.0);
        Assertions.assertEquals("DSS-40", metadata2.getParticipants().get(1));
        Assertions.assertEquals("UTC-NIST", metadata2.getParticipants().get(2));
        final List<String> metaDataComment2 = new ArrayList<String>();
        metaDataComment2.add("This is a meta-data comment");
        Assertions.assertEquals(metaDataComment2, metadata2.getComments());

        // Data 2
        final List<Observation> observations2 = file.getSegments().get(1).getData().getObservations();

        // Reference data 2
        // Same keywords and dates than 1
        final double[] values2   = {-7.40e-7, -3.125e-13,
            -7.67e-7, -1.620e-13,
            -7.81e-7, -4.745e-13,
            -8.22e-7};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assertions.assertEquals(keywords[i], observations2.get(i).getType().name());
            Assertions.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations2.get(i).getEpoch()), 0.0, 0.0);
            Assertions.assertEquals(values2[i], observations2.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment2 = new ArrayList<String>();
        dataComment2.add("This is a data comment");
        Assertions.assertEquals(dataComment2, file.getSegments().get(1).getData().getComments());


        // Meta-Data 3
        final TdmMetadata metadata3 = file.getSegments().get(2).getMetadata();

        Assertions.assertEquals("UTC", metadata3.getTimeSystem().name());
        Assertions.assertEquals(new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metadata3.getStartTime()), 0.0, 0.0);
        Assertions.assertEquals(new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metadata3.getStopTime()), 0.0, 0.0);
        Assertions.assertEquals("DSS-60", metadata3.getParticipants().get(1));
        Assertions.assertEquals("UTC-NIST", metadata3.getParticipants().get(2));
        final List<String> metaDataComment3 = new ArrayList<String>();
        metaDataComment3.add("This is a meta-data comment");
        Assertions.assertEquals(metaDataComment3, metadata3.getComments());

        // Data 3
        final List<Observation> observations3 = file.getSegments().get(2).getData().getObservations();

        // Reference data 2
        // Same keywords and dates than 1
        final double[] values3   = {-1.782e-6, 1.736e-13,
            -1.767e-6, 1.157e-14,
            -1.766e-6, 8.102e-14,
            -1.759e-6};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assertions.assertEquals(keywords[i], observations3.get(i).getType().name());
            Assertions.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations3.get(i).getEpoch()), 0.0, 0.0);
            Assertions.assertEquals(values3[i], observations3.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment3 = new ArrayList<String>();
        dataComment3.add("This is a data comment");
        Assertions.assertEquals(dataComment3, file.getSegments().get(2).getData().getComments());
    }

    /**
     * Validation function for example displaying all keywords.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExampleAllKeywordsSequential(Tdm file) {
        validateTDMExampleAllKeywordsCommon(file);
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();
        Assertions.assertEquals(TrackingMode.SEQUENTIAL, metadata.getMode());
        Assertions.assertArrayEquals(new int[] { 2, 1 }, metadata.getPath());
    }

    /**
     * Validation function for example displaying all keywords.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExampleAllKeywordsSingleDiff(Tdm file) {
        validateTDMExampleAllKeywordsCommon(file);
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();
        Assertions.assertEquals(TrackingMode.SINGLE_DIFF, metadata.getMode());
        Assertions.assertArrayEquals(new int[] { 4, 5 }, metadata.getPath1());
        Assertions.assertArrayEquals(new int[] { 3, 2 }, metadata.getPath2());
    }

    /**
     * Validation function for example displaying all keywords.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExampleAllKeywordsCommon(Tdm file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assertions.assertEquals(2.0, file.getHeader().getFormatVersion(), 0.0);
        Assertions.assertEquals(new AbsoluteDate("2017-06-14T10:53:00.000", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assertions.assertEquals("CS GROUP",file.getHeader().getOriginator());
        Assertions.assertEquals("04655f62-1ba0-4ca6-92e9-eb3411db3d44", file.getHeader().getMessageId().toLowerCase());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by CS GROUP");
        headerComment.add("Testing all TDM known meta-data and data keywords");
        Assertions.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();
        Assertions.assertEquals(1, metadata.getComments().size());
        Assertions.assertEquals("All known meta-data keywords displayed", metadata.getComments().get(0));
        Assertions.assertEquals(47, metadata.getDataTypes().size());
        Assertions.assertEquals(ObservationType.CARRIER_POWER        , metadata.getDataTypes().get( 0));
        Assertions.assertEquals(ObservationType.DOPPLER_COUNT        , metadata.getDataTypes().get( 1));
        Assertions.assertEquals(ObservationType.DOPPLER_INSTANTANEOUS, metadata.getDataTypes().get( 2));
        Assertions.assertEquals(ObservationType.DOPPLER_INTEGRATED   , metadata.getDataTypes().get( 3));
        Assertions.assertEquals(ObservationType.PC_N0                , metadata.getDataTypes().get( 4));
        Assertions.assertEquals(ObservationType.RECEIVE_PHASE_CT_1   , metadata.getDataTypes().get( 5));
        Assertions.assertEquals(ObservationType.RECEIVE_PHASE_CT_2   , metadata.getDataTypes().get( 6));
        Assertions.assertEquals(ObservationType.RECEIVE_PHASE_CT_3   , metadata.getDataTypes().get( 7));
        Assertions.assertEquals(ObservationType.RECEIVE_PHASE_CT_4   , metadata.getDataTypes().get( 8));
        Assertions.assertEquals(ObservationType.RECEIVE_PHASE_CT_5   , metadata.getDataTypes().get( 9));
        Assertions.assertEquals(ObservationType.TRANSMIT_PHASE_CT_1  , metadata.getDataTypes().get(10));
        Assertions.assertEquals(ObservationType.TRANSMIT_PHASE_CT_2  , metadata.getDataTypes().get(11));
        Assertions.assertEquals(ObservationType.TRANSMIT_PHASE_CT_3  , metadata.getDataTypes().get(12));
        Assertions.assertEquals(ObservationType.TRANSMIT_PHASE_CT_4  , metadata.getDataTypes().get(13));
        Assertions.assertEquals(ObservationType.TRANSMIT_PHASE_CT_5  , metadata.getDataTypes().get(14));
        Assertions.assertEquals(ObservationType.PR_N0                , metadata.getDataTypes().get(15));
        Assertions.assertEquals(ObservationType.RANGE                , metadata.getDataTypes().get(16));
        Assertions.assertEquals(ObservationType.RECEIVE_FREQ_1       , metadata.getDataTypes().get(17));
        Assertions.assertEquals(ObservationType.RECEIVE_FREQ_2       , metadata.getDataTypes().get(18));
        Assertions.assertEquals(ObservationType.RECEIVE_FREQ_3       , metadata.getDataTypes().get(19));
        Assertions.assertEquals(ObservationType.RECEIVE_FREQ_4       , metadata.getDataTypes().get(20));
        Assertions.assertEquals(ObservationType.RECEIVE_FREQ_5       , metadata.getDataTypes().get(21));
        Assertions.assertEquals(ObservationType.RECEIVE_FREQ         , metadata.getDataTypes().get(22));
        Assertions.assertEquals(ObservationType.TRANSMIT_FREQ_1      , metadata.getDataTypes().get(23));
        Assertions.assertEquals(ObservationType.TRANSMIT_FREQ_2      , metadata.getDataTypes().get(24));
        Assertions.assertEquals(ObservationType.TRANSMIT_FREQ_3      , metadata.getDataTypes().get(25));
        Assertions.assertEquals(ObservationType.TRANSMIT_FREQ_4      , metadata.getDataTypes().get(26));
        Assertions.assertEquals(ObservationType.TRANSMIT_FREQ_5      , metadata.getDataTypes().get(27));
        Assertions.assertEquals(ObservationType.TRANSMIT_FREQ_RATE_1 , metadata.getDataTypes().get(28));
        Assertions.assertEquals(ObservationType.TRANSMIT_FREQ_RATE_2 , metadata.getDataTypes().get(29));
        Assertions.assertEquals(ObservationType.TRANSMIT_FREQ_RATE_3 , metadata.getDataTypes().get(30));
        Assertions.assertEquals(ObservationType.TRANSMIT_FREQ_RATE_4 , metadata.getDataTypes().get(31));
        Assertions.assertEquals(ObservationType.TRANSMIT_FREQ_RATE_5 , metadata.getDataTypes().get(32));
        Assertions.assertEquals(ObservationType.DOR                  , metadata.getDataTypes().get(33));
        Assertions.assertEquals(ObservationType.VLBI_DELAY           , metadata.getDataTypes().get(34));
        Assertions.assertEquals(ObservationType.ANGLE_1              , metadata.getDataTypes().get(35));
        Assertions.assertEquals(ObservationType.ANGLE_2              , metadata.getDataTypes().get(36));
        Assertions.assertEquals(ObservationType.MAG                  , metadata.getDataTypes().get(37));
        Assertions.assertEquals(ObservationType.RCS                  , metadata.getDataTypes().get(38));
        Assertions.assertEquals(ObservationType.CLOCK_BIAS           , metadata.getDataTypes().get(39));
        Assertions.assertEquals(ObservationType.CLOCK_DRIFT          , metadata.getDataTypes().get(40));
        Assertions.assertEquals(ObservationType.STEC                 , metadata.getDataTypes().get(41));
        Assertions.assertEquals(ObservationType.TROPO_DRY            , metadata.getDataTypes().get(42));
        Assertions.assertEquals(ObservationType.TROPO_WET            , metadata.getDataTypes().get(43));
        Assertions.assertEquals(ObservationType.PRESSURE             , metadata.getDataTypes().get(44));
        Assertions.assertEquals(ObservationType.RHUMIDITY            , metadata.getDataTypes().get(45));
        Assertions.assertEquals(ObservationType.TEMPERATURE          , metadata.getDataTypes().get(46));
        Assertions.assertEquals("UTC", metadata.getTimeSystem().name());
        Assertions.assertEquals(new AbsoluteDate("2017-06-14T10:53:00.000", utc).durationFrom(metadata.getStartTime()), 0.0, 0.0);
        Assertions.assertEquals(new AbsoluteDate("2017-06-15T10:53:00.000", utc).durationFrom(metadata.getStopTime()), 0.0, 0.0);
        Assertions.assertEquals("DSS-25", metadata.getParticipants().get(1));
        Assertions.assertEquals("yyyy-nnnA", metadata.getParticipants().get(2));
        Assertions.assertEquals("P3", metadata.getParticipants().get(3));
        Assertions.assertEquals("P4", metadata.getParticipants().get(4));
        Assertions.assertEquals("P5", metadata.getParticipants().get(5));
        Assertions.assertEquals("S", metadata.getTransmitBand());
        Assertions.assertEquals("L", metadata.getReceiveBand());
        Assertions.assertEquals(240, metadata.getTurnaroundNumerator(), 0);
        Assertions.assertEquals(221, metadata.getTurnaroundDenominator(), 0);
        Assertions.assertEquals(TimetagReference.TRANSMIT, metadata.getTimetagRef());
        Assertions.assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        Assertions.assertEquals(IntegrationReference.MIDDLE, metadata.getIntegrationRef());
        Assertions.assertEquals(32021035200.0, metadata.getFreqOffset(), 0.0);
        Assertions.assertEquals(RangeMode.COHERENT, metadata.getRangeMode());
        Assertions.assertEquals(32768.0, metadata.getRawRangeModulus(), 0.0);
        Assertions.assertEquals(RangeUnits.RU, metadata.getRangeUnits());
        Assertions.assertEquals(AngleType.RADEC, metadata.getAngleType());
        Assertions.assertEquals("EME2000", metadata.getReferenceFrame().getName());
        Assertions.assertEquals(CelestialBodyFrame.EME2000, metadata.getReferenceFrame().asCelestialBodyFrame());
        Assertions.assertEquals(FramesFactory.getEME2000(), metadata.getReferenceFrame().asFrame());
        Assertions.assertEquals("HERMITE", metadata.getInterpolationMethod());
        Assertions.assertEquals(5, metadata.getInterpolationDegree());
        Assertions.assertEquals(120000.0, metadata.getDopplerCountBias(), 1.0e-5);
        Assertions.assertEquals(1000.0, metadata.getDopplerCountScale(), 1.0e-10);
        Assertions.assertFalse(metadata.hasDopplerCountRollover());
        Assertions.assertEquals(0.000077, metadata.getTransmitDelays().get(1), 0.0);
        Assertions.assertEquals(0.000077, metadata.getTransmitDelays().get(2), 0.0);
        Assertions.assertEquals(0.000077, metadata.getTransmitDelays().get(3), 0.0);
        Assertions.assertEquals(0.000077, metadata.getTransmitDelays().get(4), 0.0);
        Assertions.assertEquals(0.000077, metadata.getTransmitDelays().get(5), 0.0);
        Assertions.assertEquals(0.000077, metadata.getReceiveDelays().get(1), 0.0);
        Assertions.assertEquals(0.000077, metadata.getReceiveDelays().get(2), 0.0);
        Assertions.assertEquals(0.000077, metadata.getReceiveDelays().get(3), 0.0);
        Assertions.assertEquals(0.000077, metadata.getReceiveDelays().get(4), 0.0);
        Assertions.assertEquals(0.000077, metadata.getReceiveDelays().get(5), 0.0);
        Assertions.assertEquals(DataQuality.RAW, metadata.getDataQuality());
        Assertions.assertEquals(FastMath.toRadians(1.0), metadata.getCorrectionAngle1(), 0.0);
        Assertions.assertEquals(FastMath.toRadians(2.0), metadata.getCorrectionAngle2(), 0.0);
        Assertions.assertEquals(3000.0, metadata.getCorrectionDoppler(), 0.0);
        Assertions.assertEquals(4.0, metadata.getCorrectionMagnitude(), 0.0);
        Assertions.assertEquals(5.0, metadata.getRawCorrectionRange(), 0.0);
        Assertions.assertEquals(6.0, metadata.getCorrectionRcs(), 0.0);
        Assertions.assertEquals(7.0, metadata.getCorrectionReceive(), 0.0);
        Assertions.assertEquals(8.0, metadata.getCorrectionTransmit(), 0.0);
        Assertions.assertEquals(FastMath.toRadians(9.0), metadata.getCorrectionAberrationYearly(), 0.0);
        Assertions.assertEquals(FastMath.toRadians(10.0), metadata.getCorrectionAberrationDiurnal(), 0.0);
        Assertions.assertEquals(CorrectionApplied.YES, metadata.getCorrectionsApplied());

        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("All known meta-data keywords displayed");
        Assertions.assertEquals(metaDataComment, metadata.getComments());

        // Data
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

        // Reference data
        final AbsoluteDate epoch = new AbsoluteDate("2017-06-14T10:53:00.000", utc);
        // Check consistency
        for (int i = 0; i < metadata.getDataTypes().size(); i++) {
            Assertions.assertEquals(metadata.getDataTypes().get(i), observations.get(i).getType());
            Assertions.assertEquals(epoch.shiftedBy((double) (i+1)).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assertions.assertEquals((double) (i+1), observations.get(i).getMeasurement(), 1.0e-12);
        }

        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("Data Related Keywords");
        Assertions.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());
    }

}
