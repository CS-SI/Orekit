/* Copyright 2002-2024 CS GROUP
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
import org.hipparchus.util.FastMath;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

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
class TdmParserTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testParseTdmExternalResourceIssue368() {
        // setup
        final String name = "/ccsds/tdm/xml/TDM-external-doctype.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));

        try {
            // action
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);

            // verify
            fail("Expected Exception");
        } catch (OrekitException e) {
            // Malformed URL exception indicates external resource was disabled
            // file not found exception indicates parser tried to load the resource
            assertThat(e.getCause(),
                    CoreMatchers.instanceOf(MalformedURLException.class));
        }
    }

    @Test
    void testParseTdmKeyValueExample2() {
        // Example 2 of [1]
        // See Figure D-2: TDM Example: One-Way Data w/Frequency Offset
        // Data lines number was cut down to 7
        final String name = "/ccsds/tdm/kvn/TDMExample2.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample2(file);
    }

    @Test
    void testParseTdmKeyValueExample4() {

        // Example 4 of [1]
        // See Figure D-4: TDM Example: Two-Way Ranging Data Only
        // Data lines number was cut down to 20
        final String name = "/ccsds/tdm/kvn/TDMExample4.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExample4(file);
    }

    @Test
    void testParseTdmKeyValueExample6() {

        // Example 6 of [1]
        // See Figure D-6: TDM Example: Four-Way Data
        // Data lines number was cut down to 16
        final String name = "/ccsds/tdm/kvn/TDMExample6.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample6(file);
    }

    @Test
    void testParseTdmKeyValueExample8() {

        // Example 8 of [1]
        // See Figure D-8: TDM Example: Angles, Range, Doppler Combined in Single TDM
        // Data lines number was cut down to 18
        final String name = "/ccsds/tdm/kvn/TDMExample8.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExample8(file);
    }

    @Test
    void testParseTdmKeyValueExample15() {

        // Example 15 of [1]
        // See Figure D-15: TDM Example: Clock Bias/Drift Only
        final String name = "/ccsds/tdm/kvn/TDMExample15.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample15(file);
    }

    @Test
    void testParseTdmKeyValueExampleAllKeywordsSequential() {

        // Testing all TDM keywords
        final String name = "/ccsds/tdm/kvn/TDMExampleAllKeywordsSequential.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExampleAllKeywordsSequential(file);
    }

    @Test
    void testParseTdmKeyValueExampleAllKeywordsSingleDiff() {

        // Testing all TDM keywords
        final String name = "/ccsds/tdm/kvn/TDMExampleAllKeywordsSingleDiff.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExampleAllKeywordsSingleDiff(file);
    }

    @Test
    void testParseTdmXmlExample2() {

        // Example 2 of [1]
        // See Figure D-2: TDM Example: One-Way Data w/Frequency Offset
        // Data lines number was cut down to 7
        final String name = "/ccsds/tdm/xml/TDMExample2.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample2(file);
    }

    @Test
    void testWriteTdmXmlExample2() throws IOException {

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
    void testParseTdmXmlExample4() {

        // Example 4 of [1]
        // See Figure D-4: TDM Example: Two-Way Ranging Data Only
        // Data lines number was cut down to 20
        final String name = "/ccsds/tdm/xml/TDMExample4.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExample4(file);
    }

    @Test
    void testParseTdmXmlExample6() {

        // Example 6 of [1]
        // See Figure D-6: TDM Example: Four-Way Data
        // Data lines number was cut down to 16
        final String name = "/ccsds/tdm/xml/TDMExample6.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample6(file);
    }

    @Test
    void testParseTdmXmlExample8() {

        // Example 8 of [1]
        // See Figure D-8: TDM Example: Angles, Range, Doppler Combined in Single TDM
        // Data lines number was cut down to 18
        final String name = "/ccsds/tdm/xml/TDMExample8.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExample8(file);
    }

    @Test
    void testParseTdmXmlExample15() {

        // Example 15 of [1]
        // See Figure D-15: TDM Example: Clock Bias/Drift Only
        final String name = "/ccsds/tdm/xml/TDMExample15.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        validateTDMExample15(file);
    }

    @Test
    void testIssue963() {

        // Check that a TDM with spaces in between participants in PATH is rejected
        final String name = "/ccsds/tdm/kvn/TDM-issue963.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            // Number format exception in metadata part
            new ParserBuilder().buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
        }
    }

    @Test
    void testParseTdmXmlExampleAllKeywordsSequential() {

        // Testing all TDM keywords
        final String name = "/ccsds/tdm/xml/TDMExampleAllKeywordsSequential.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExampleAllKeywordsSequential(file);
    }

    @Test
    void testParseTdmXmlExampleAllKeywordsSingleDiff() {

        // Testing all TDM keywords
        final String name = "/ccsds/tdm/xml/TDMExampleAllKeywordsSingleDiff.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        final Tdm file = new ParserBuilder().buildTdmParser().parseMessage(source);
        validateTDMExampleAllKeywordsSingleDiff(file);
    }

    @Test
    void testDataNumberFormatErrorTypeKeyValue() {
        final String name = "/ccsds/tdm/kvn/TDM-data-number-format-error.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            // Number format exception in data part
            new ParserBuilder().buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            assertEquals("RECEIVE_FREQ_1", oe.getParts()[0]);
            assertEquals(26, oe.getParts()[1]);
            assertEquals(name, oe.getParts()[2]);
        }
    }

    @Test
    void testDataNumberFormatErrorTypeXml() {
        try {
            // Number format exception in data part
            final String name = "/ccsds/tdm/xml/TDM-data-number-format-error.xml";
            final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
            new ParserBuilder().buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            assertEquals("RECEIVE_FREQ_1", oe.getParts()[0]);
            assertEquals(47, oe.getParts()[1]);
            assertEquals("/ccsds/tdm/xml/TDM-data-number-format-error.xml", oe.getParts()[2]);
        }
    }

    @Test
    void testMetaDataNumberFormatErrorTypeKeyValue() {
        try {
            // Number format exception in metadata part
            final String name = "/ccsds/tdm/kvn/TDM-metadata-number-format-error.txt";
            final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An Orekit Exception \"UNABLE_TO_PARSE_LINE_IN_FILE\" should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            assertEquals("TRANSMIT_DELAY_1", oe.getParts()[0]);
            assertEquals(17, oe.getParts()[1]);
            assertEquals("/ccsds/tdm/kvn/TDM-metadata-number-format-error.txt", oe.getParts()[2]);
        }
    }

    @Test
    void testMetaDataNumberFormatErrorTypeXml() {
        try {
            // Number format exception in metadata part
            final String name = "/ccsds/tdm/xml/TDM-metadata-number-format-error.xml";
            final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            assertEquals("TRANSMIT_DELAY_1", oe.getParts()[0]);
            assertEquals(24, oe.getParts()[1]);
            assertEquals("/ccsds/tdm/xml/TDM-metadata-number-format-error.xml", oe.getParts()[2]);
        }
    }

    @Test
    void testNonExistentFile() throws URISyntaxException {
        // Try parsing a file that does not exist
        final String realName = "/ccsds/odm/oem/OEMExample2.txt";
        final String wrongName = realName + "xxxxx";
        final DataSource source = new DataSource(wrongName, () -> TdmParserTest.class.getResourceAsStream(wrongName));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    void testInconsistentTimeSystemsKeyValue() {
        // Inconsistent time systems between two sets of data
        final String name = "/ccsds/tdm/kvn/TDM-inconsistent-time-systems.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        assertEquals(3, file.getSegments().size());
        assertEquals(TimeSystem.UTC, file.getSegments().get(0).getMetadata().getTimeSystem());
        assertEquals(TimeSystem.TCG, file.getSegments().get(1).getMetadata().getTimeSystem());
        assertEquals(TimeSystem.UTC, file.getSegments().get(2).getMetadata().getTimeSystem());
    }

    @Test
    void testInconsistentTimeSystemsXml() {
        // Inconsistent time systems between two sets of data
        final String name = "/ccsds/tdm/xml/TDM-inconsistent-time-systems.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        Tdm file = new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
        assertEquals(3, file.getSegments().size());
        assertEquals(TimeSystem.UTC, file.getSegments().get(0).getMetadata().getTimeSystem());
        assertEquals(TimeSystem.TCG, file.getSegments().get(1).getMetadata().getTimeSystem());
        assertEquals(TimeSystem.UTC, file.getSegments().get(2).getMetadata().getTimeSystem());
    }

    @Test
    void testWrongDataKeywordKeyValue() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/kvn/TDM-data-wrong-keyword.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            assertEquals(26, oe.getParts()[0]);
            assertEquals("/ccsds/tdm/kvn/TDM-data-wrong-keyword.txt", oe.getParts()[1], "%s");
            assertEquals("WRONG_KEYWORD", oe.getParts()[2]);
        }
    }

    @Test
    void testWrongDataKeywordXml() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-data-wrong-keyword.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            assertEquals(47, oe.getParts()[0]);
            assertEquals(name, oe.getParts()[1]);
            assertEquals("WRONG_KEYWORD", oe.getParts()[2]);
        }
    }

    @Test
    void testWrongMetaDataKeywordKeyValue() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/kvn/TDM-metadata-wrong-keyword.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            assertEquals(16, oe.getParts()[0]);
            assertEquals("/ccsds/tdm/kvn/TDM-metadata-wrong-keyword.txt", oe.getParts()[1]);
            assertEquals("WRONG_KEYWORD", oe.getParts()[2]);
        }
    }

    @Test
    void testWrongMetaDataKeywordXml() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-metadata-wrong-keyword.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            assertEquals(23, oe.getParts()[0]);
            assertEquals("/ccsds/tdm/xml/TDM-metadata-wrong-keyword.xml", oe.getParts()[1]);
            assertEquals("WRONG_KEYWORD", oe.getParts()[2]);
        }
    }

    @Test
    void testWrongTimeSystemKeyValue() {
        // Time system not implemented CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/kvn/TDM-metadata-timesystem-not-implemented.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, oe.getSpecifier());
            assertEquals("WRONG-TIME-SYSTEM", oe.getParts()[0]);
        }
    }

    @Test
    void testWrongTimeSystemXml() {
        // Time system not implemented CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-metadata-timesystem-not-implemented.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, oe.getSpecifier());
            assertEquals("WRONG-TIME-SYSTEM", oe.getParts()[0]);
        }
    }

    @Test
    void testMissingTimeSystemXml() {
        // Time system not implemented CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-missing-timesystem.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_READ_YET, oe.getSpecifier());
            assertEquals(18, oe.getParts()[0]);
        }
    }

    @Test
    void testMissingPArticipants() {
        final String name = "/ccsds/tdm/xml/TDM-missing-participants.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            assertEquals(TdmMetadataKey.PARTICIPANT_1, oe.getParts()[0]);
        }
    }

    @Test
    void testInconsistentDataLineKeyValue() {
        // Inconsistent data line in KeyValue file (3 fields after keyword instead of 2)
        final String name = "/ccsds/tdm/kvn/TDM-data-inconsistent-line.txt";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            assertEquals("RECEIVE_FREQ_1", oe.getParts()[0]);
            assertEquals(25, oe.getParts()[1]);
            assertEquals("/ccsds/tdm/kvn/TDM-data-inconsistent-line.txt", oe.getParts()[2]);
        }
    }

    @Test
    void testInconsistentDataBlockXml() {
        // Inconsistent data block in XML file
        final String name = "/ccsds/tdm/xml/TDM-data-inconsistent-block.xml";
        final DataSource source = new DataSource(name, () -> TdmParserTest.class.getResourceAsStream(name));
        try {
            new ParserBuilder().withRangeUnitsConverter(null).buildTdmParser().parseMessage(source);
            fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            assertEquals("TRANSMIT_FREQ_2", oe.getParts()[0]);
            assertEquals(32, oe.getParts()[1]);
            assertEquals("/ccsds/tdm/xml/TDM-data-inconsistent-block.xml", oe.getParts()[2]);
        }
    }

    /**
     * Validation function for example 2.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExample2(Tdm file) {
        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        assertEquals(0.0, new AbsoluteDate("2005-160T20:15:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0);
        assertEquals("NASA/JPL",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        headerComment.add("StarTrek 1-way data, Ka band down");
        assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();

        assertEquals("UTC", metadata.getTimeSystem().name());
        assertEquals(0.0, new AbsoluteDate("2005-159T17:41:00", utc).durationFrom(metadata.getStartTime()), 0.0);
        assertEquals(0.0, new AbsoluteDate("2005-159T17:41:40", utc).durationFrom(metadata.getStopTime()), 0.0);
        assertEquals("DSS-25", metadata.getParticipants().get(1));
        assertEquals("yyyy-nnnA", metadata.getParticipants().get(2));
        assertEquals(TrackingMode.SEQUENTIAL, metadata.getMode());
        assertArrayEquals(new int[] { 2, 1 }, metadata.getPath());
        assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        assertEquals(IntegrationReference.MIDDLE, metadata.getIntegrationRef());
        assertEquals(32021035200.0, metadata.getFreqOffset(), 0.0);
        assertEquals(0.000077, metadata.getTransmitDelays().get(1), 0.0);
        assertEquals(0.000077, metadata.getReceiveDelays().get(1), 0.0);
        assertEquals(DataQuality.RAW, metadata.getDataQuality());
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("This is a meta-data comment");
        assertEquals(metaDataComment, metadata.getComments());

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
            assertEquals(keywords[i], observations.get(i).getType().name());
            assertEquals(0.0, new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0);
            assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }

        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        assertEquals(dataComment, file.getSegments().get(0).getData().getComments());

        // check so global setters that are not used by parser (it uses successive add instead)

        metadata.setParticipants(Collections.singletonMap(12, "p12"));
        assertNull(metadata.getParticipants().get(1));
        assertEquals("p12", metadata.getParticipants().get(12));
        metadata.setTransmitDelays(Collections.singletonMap(12, 1.25));
        assertNull(metadata.getTransmitDelays().get(1));
        assertEquals(1.25, metadata.getTransmitDelays().get(12).doubleValue(), 1.0e-15);
        metadata.setReceiveDelays(Collections.singletonMap(12, 2.5));
        assertNull(metadata.getReceiveDelays().get(1));
        assertEquals(2.5, metadata.getReceiveDelays().get(12).doubleValue(), 1.0e-15);

    }

    /**
     * Validation function for example 4.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExample4(Tdm file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        assertEquals(0.0, new AbsoluteDate("2005-191T23:00:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0);
        assertEquals("NASA/JPL",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();

        assertEquals("UTC", metadata.getTimeSystem().name());
        assertEquals("DSS-24", metadata.getParticipants().get(1));
        assertEquals("yyyy-nnnA", metadata.getParticipants().get(2));
        assertEquals(TrackingMode.SEQUENTIAL, metadata.getMode());
        assertArrayEquals(new int[] { 1, 2, 1 }, metadata.getPath());
        assertEquals(IntegrationReference.START, metadata.getIntegrationRef());
        assertEquals(RangeMode.COHERENT, metadata.getRangeMode());
        assertEquals(2.0e+26, metadata.getRawRangeModulus(), 0.0);
        assertEquals(2.0e+26, metadata.getRangeModulus(new IdentityConverter()), 0.0);
        assertEquals(RangeUnits.RU, metadata.getRangeUnits());
        assertEquals(7.7e-5, metadata.getTransmitDelays().get(1), 0.0);
        assertEquals(0.0, metadata.getTransmitDelays().get(2), 0.0);
        assertEquals(7.7e-5, metadata.getReceiveDelays().get(1), 0.0);
        assertEquals(0.0, metadata.getReceiveDelays().get(2), 0.0);
        assertEquals(46.7741, metadata.getCorrectionRange(new IdentityConverter()), 0.0);
        assertEquals(46.7741, metadata.getRawCorrectionRange(), 0.0);
        assertEquals(CorrectionApplied.YES, metadata.getCorrectionsApplied());
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("Range correction applied is range calibration to DSS-24.");
        metaDataComment.add("Estimated RTLT at begin of pass = 950 seconds");
        metaDataComment.add("Antenna Z-height correction 0.0545 km applied to uplink signal");
        metaDataComment.add("Antenna Z-height correction 0.0189 km applied to downlink signal");
        assertEquals(metaDataComment, metadata.getComments());

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
            assertEquals(keywords[i], observations.get(i).getType().name());
            assertEquals(0.0, new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0);
            assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        assertEquals(dataComment, file.getSegments().get(0).getData().getComments());
    }

    /**
     * Validation function for example 6.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExample6(Tdm file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        assertEquals(0.0, new AbsoluteDate("1998-06-10T01:00:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0);
        assertEquals("JAXA",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (JAXA)");
        assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();

        assertEquals("UTC", metadata.getTimeSystem().name());
        assertEquals(0.0, new AbsoluteDate("1998-06-10T00:57:37", utc).durationFrom(metadata.getStartTime()), 0.0);
        assertEquals(0.0, new AbsoluteDate("1998-06-10T00:57:44", utc).durationFrom(metadata.getStopTime()), 0.0);
        assertEquals("NORTH", metadata.getParticipants().get(1));
        assertEquals("F07R07", metadata.getParticipants().get(2));
        assertEquals("E7", metadata.getParticipants().get(3));
        assertEquals(TrackingMode.SEQUENTIAL, metadata.getMode());
        assertArrayEquals(new int[] { 1, 2, 3, 2, 1 }, metadata.getPath());
        assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        assertEquals(IntegrationReference.MIDDLE, metadata.getIntegrationRef());
        assertEquals(RangeMode.CONSTANT, metadata.getRangeMode());
        assertEquals(1.0, metadata.getRawRangeModulus(), 0.0);
        assertEquals(1000.0, metadata.getRangeModulus(new IdentityConverter()), 0.0);
        assertEquals(RangeUnits.km, metadata.getRangeUnits());
        assertEquals(AngleType.AZEL, metadata.getAngleType());
        assertEquals(2.0, metadata.getRawCorrectionRange(), 0.0);
        assertEquals(2000.0, metadata.getCorrectionRange(new IdentityConverter()), 0.0);
        assertEquals(CorrectionApplied.YES, metadata.getCorrectionsApplied());

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
            assertEquals(keywords[i], observations.get(i).getType().name());
            assertEquals(0.0, new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0);
            assertEquals(values[i], observations.get(i).getMeasurement(), 1.0e-12 * FastMath.abs(values[i]));
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        assertEquals(dataComment, file.getSegments().get(0).getData().getComments());
    }

    /**
     * Validation function for example 8.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExample8(Tdm file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        assertEquals(0.0, new AbsoluteDate("2007-08-30T12:01:44.749", utc).durationFrom(file.getHeader().getCreationDate()), 0.0);
        assertEquals("GSOC",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("GEOSCX INP");
        assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data 1
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();

        assertEquals("UTC", metadata.getTimeSystem().name());
        assertEquals(0.0, new AbsoluteDate("2007-08-29T07:00:02.000", utc).durationFrom(metadata.getStartTime()), 0.0);
        assertEquals(0.0, new AbsoluteDate("2007-08-29T14:00:02.000", utc).durationFrom(metadata.getStopTime()), 0.0);
        assertEquals("HBSTK", metadata.getParticipants().get(1));
        assertEquals("SAT", metadata.getParticipants().get(2));
        assertEquals(TrackingMode.SEQUENTIAL, metadata.getMode());
        assertArrayEquals(new int[] { 1, 2, 1 }, metadata.getPath());
        assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        assertEquals(IntegrationReference.END, metadata.getIntegrationRef());
        assertEquals(AngleType.XSYE, metadata.getAngleType());
        assertEquals(DataQuality.RAW, metadata.getDataQuality());
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("This is a meta-data comment");
        assertEquals(metaDataComment, metadata.getComments());

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
            assertEquals(keywords[i], observations.get(i).getType().name());
            assertEquals(0.0, new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0);
            assertEquals(values[i], observations.get(i).getMeasurement(), 1.0e-12 * FastMath.abs(values[i]));
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        assertEquals(dataComment, file.getSegments().get(0).getData().getComments());

        // Meta-Data 2
        final TdmMetadata metadata2 = file.getSegments().get(1).getMetadata();

        assertEquals("UTC", metadata.getTimeSystem().name());
        assertEquals(0.0, new AbsoluteDate("2007-08-29T06:00:02.000", utc).durationFrom(metadata2.getStartTime()), 0.0);
        assertEquals(0.0, new AbsoluteDate("2007-08-29T13:00:02.000", utc).durationFrom(metadata2.getStopTime()), 0.0);
        assertEquals("WHM1", metadata2.getParticipants().get(1));
        assertEquals("SAT", metadata2.getParticipants().get(2));
        assertEquals(TrackingMode.SEQUENTIAL, metadata2.getMode());
        assertArrayEquals(new int[] { 1, 2, 1 }, metadata2.getPath());
        assertEquals(1.0, metadata2.getIntegrationInterval(), 0.0);
        assertEquals(IntegrationReference.END, metadata2.getIntegrationRef());
        assertEquals(1.0e7, metadata2.getRawRangeModulus(), 0.0);
        assertEquals(1.0e7 * Constants.SPEED_OF_LIGHT, metadata2.getRangeModulus(new IdentityConverter()), 0.0);
        assertEquals(RangeUnits.s, metadata2.getRangeUnits());
        assertEquals(AngleType.AZEL, metadata2.getAngleType());
        assertEquals(DataQuality.RAW, metadata2.getDataQuality());
        assertEquals(2.0, metadata2.getRawCorrectionRange(), 0.0);
        assertEquals(2.0 * Constants.SPEED_OF_LIGHT, metadata2.getCorrectionRange(new IdentityConverter()), 0.0);
        assertEquals(CorrectionApplied.YES, metadata2.getCorrectionsApplied());
        final List<String> metaDataComment2 = new ArrayList<String>();
        metaDataComment2.add("This is a meta-data comment");
        assertEquals(metaDataComment2, metadata2.getComments());

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
            assertEquals(keywords2[i], observations2.get(i).getType().name());
            assertEquals(0.0, new AbsoluteDate(epochs2[i], utc).durationFrom(observations2.get(i).getEpoch()), 0.0);
            assertEquals(values2[i], observations2.get(i).getMeasurement(), 1.0e-12 * FastMath.abs(values2[i]));
        }
        // Comment
        final List<String> dataComment2 = new ArrayList<String>();
        dataComment2.add("This is a data comment");
        assertEquals(dataComment2, file.getSegments().get(1).getData().getComments());
    }

    /**
     * Validation function for example 15.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExample15(Tdm file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        assertEquals(0.0, new AbsoluteDate("2005-161T15:45:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0);
        assertEquals("NASA/JPL",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        headerComment.add("The following are clock offsets, in seconds between the");
        headerComment.add("clocks at each DSN complex relative to UTC(NIST). The offset");
        headerComment.add("is a mean of readings using several GPS space vehicles in");
        headerComment.add("common view. Value is \"station clock minus UTC”.");
        assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data 1
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();

        assertEquals("UTC", metadata.getTimeSystem().name());
        assertEquals(0.0, new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metadata.getStartTime()), 0.0);
        assertEquals(0.0, new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metadata.getStopTime()), 0.0);
        assertEquals("DSS-10", metadata.getParticipants().get(1));
        assertEquals("UTC-NIST", metadata.getParticipants().get(2));
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("Note: SPC10 switched back to Maser1 from Maser2 on 2005-142");
        assertEquals(metaDataComment, metadata.getComments());

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
            assertEquals(keywords[i], observations.get(i).getType().name());
            assertEquals(0.0, new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0);
            assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        assertEquals(dataComment, file.getSegments().get(0).getData().getComments());


        // Meta-Data 2
        final TdmMetadata metadata2 = file.getSegments().get(1).getMetadata();

        assertEquals("UTC", metadata2.getTimeSystem().name());
        assertEquals(0.0, new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metadata2.getStartTime()), 0.0);
        assertEquals(0.0, new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metadata2.getStopTime()), 0.0);
        assertEquals("DSS-40", metadata2.getParticipants().get(1));
        assertEquals("UTC-NIST", metadata2.getParticipants().get(2));
        final List<String> metaDataComment2 = new ArrayList<String>();
        metaDataComment2.add("This is a meta-data comment");
        assertEquals(metaDataComment2, metadata2.getComments());

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
            assertEquals(keywords[i], observations2.get(i).getType().name());
            assertEquals(0.0, new AbsoluteDate(epochs[i], utc).durationFrom(observations2.get(i).getEpoch()), 0.0);
            assertEquals(values2[i], observations2.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment2 = new ArrayList<String>();
        dataComment2.add("This is a data comment");
        assertEquals(dataComment2, file.getSegments().get(1).getData().getComments());


        // Meta-Data 3
        final TdmMetadata metadata3 = file.getSegments().get(2).getMetadata();

        assertEquals("UTC", metadata3.getTimeSystem().name());
        assertEquals(0.0, new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metadata3.getStartTime()), 0.0);
        assertEquals(0.0, new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metadata3.getStopTime()), 0.0);
        assertEquals("DSS-60", metadata3.getParticipants().get(1));
        assertEquals("UTC-NIST", metadata3.getParticipants().get(2));
        final List<String> metaDataComment3 = new ArrayList<String>();
        metaDataComment3.add("This is a meta-data comment");
        assertEquals(metaDataComment3, metadata3.getComments());

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
            assertEquals(keywords[i], observations3.get(i).getType().name());
            assertEquals(0.0, new AbsoluteDate(epochs[i], utc).durationFrom(observations3.get(i).getEpoch()), 0.0);
            assertEquals(values3[i], observations3.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment3 = new ArrayList<String>();
        dataComment3.add("This is a data comment");
        assertEquals(dataComment3, file.getSegments().get(2).getData().getComments());
    }

    /**
     * Validation function for example displaying all keywords.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExampleAllKeywordsSequential(Tdm file) {
        validateTDMExampleAllKeywordsCommon(file);
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();
        assertEquals(TrackingMode.SEQUENTIAL, metadata.getMode());
        assertArrayEquals(new int[] { 2, 1 }, metadata.getPath());
    }

    /**
     * Validation function for example displaying all keywords.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExampleAllKeywordsSingleDiff(Tdm file) {
        validateTDMExampleAllKeywordsCommon(file);
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();
        assertEquals(TrackingMode.SINGLE_DIFF, metadata.getMode());
        assertArrayEquals(new int[] { 4, 5 }, metadata.getPath1());
        assertArrayEquals(new int[] { 3, 2 }, metadata.getPath2());
    }

    /**
     * Validation function for example displaying all keywords.
     * @param file Parsed TDM to validate
     */
    private void validateTDMExampleAllKeywordsCommon(Tdm file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        assertEquals(2.0, file.getHeader().getFormatVersion(), 0.0);
        assertEquals(0.0, new AbsoluteDate("2017-06-14T10:53:00.000", utc).durationFrom(file.getHeader().getCreationDate()), 0.0);
        assertEquals("CS GROUP",file.getHeader().getOriginator());
        assertEquals("04655f62-1ba0-4ca6-92e9-eb3411db3d44", file.getHeader().getMessageId().toLowerCase());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by CS GROUP");
        headerComment.add("Testing all TDM known meta-data and data keywords");
        assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TdmMetadata metadata = file.getSegments().get(0).getMetadata();
        assertEquals(1, metadata.getComments().size());
        assertEquals("All known meta-data keywords displayed", metadata.getComments().get(0));
        assertEquals(47, metadata.getDataTypes().size());
        assertEquals(ObservationType.CARRIER_POWER        , metadata.getDataTypes().get( 0));
        assertEquals(ObservationType.DOPPLER_COUNT        , metadata.getDataTypes().get( 1));
        assertEquals(ObservationType.DOPPLER_INSTANTANEOUS, metadata.getDataTypes().get( 2));
        assertEquals(ObservationType.DOPPLER_INTEGRATED   , metadata.getDataTypes().get( 3));
        assertEquals(ObservationType.PC_N0                , metadata.getDataTypes().get( 4));
        assertEquals(ObservationType.RECEIVE_PHASE_CT_1   , metadata.getDataTypes().get( 5));
        assertEquals(ObservationType.RECEIVE_PHASE_CT_2   , metadata.getDataTypes().get( 6));
        assertEquals(ObservationType.RECEIVE_PHASE_CT_3   , metadata.getDataTypes().get( 7));
        assertEquals(ObservationType.RECEIVE_PHASE_CT_4   , metadata.getDataTypes().get( 8));
        assertEquals(ObservationType.RECEIVE_PHASE_CT_5   , metadata.getDataTypes().get( 9));
        assertEquals(ObservationType.TRANSMIT_PHASE_CT_1  , metadata.getDataTypes().get(10));
        assertEquals(ObservationType.TRANSMIT_PHASE_CT_2  , metadata.getDataTypes().get(11));
        assertEquals(ObservationType.TRANSMIT_PHASE_CT_3  , metadata.getDataTypes().get(12));
        assertEquals(ObservationType.TRANSMIT_PHASE_CT_4  , metadata.getDataTypes().get(13));
        assertEquals(ObservationType.TRANSMIT_PHASE_CT_5  , metadata.getDataTypes().get(14));
        assertEquals(ObservationType.PR_N0                , metadata.getDataTypes().get(15));
        assertEquals(ObservationType.RANGE                , metadata.getDataTypes().get(16));
        assertEquals(ObservationType.RECEIVE_FREQ_1       , metadata.getDataTypes().get(17));
        assertEquals(ObservationType.RECEIVE_FREQ_2       , metadata.getDataTypes().get(18));
        assertEquals(ObservationType.RECEIVE_FREQ_3       , metadata.getDataTypes().get(19));
        assertEquals(ObservationType.RECEIVE_FREQ_4       , metadata.getDataTypes().get(20));
        assertEquals(ObservationType.RECEIVE_FREQ_5       , metadata.getDataTypes().get(21));
        assertEquals(ObservationType.RECEIVE_FREQ         , metadata.getDataTypes().get(22));
        assertEquals(ObservationType.TRANSMIT_FREQ_1      , metadata.getDataTypes().get(23));
        assertEquals(ObservationType.TRANSMIT_FREQ_2      , metadata.getDataTypes().get(24));
        assertEquals(ObservationType.TRANSMIT_FREQ_3      , metadata.getDataTypes().get(25));
        assertEquals(ObservationType.TRANSMIT_FREQ_4      , metadata.getDataTypes().get(26));
        assertEquals(ObservationType.TRANSMIT_FREQ_5      , metadata.getDataTypes().get(27));
        assertEquals(ObservationType.TRANSMIT_FREQ_RATE_1 , metadata.getDataTypes().get(28));
        assertEquals(ObservationType.TRANSMIT_FREQ_RATE_2 , metadata.getDataTypes().get(29));
        assertEquals(ObservationType.TRANSMIT_FREQ_RATE_3 , metadata.getDataTypes().get(30));
        assertEquals(ObservationType.TRANSMIT_FREQ_RATE_4 , metadata.getDataTypes().get(31));
        assertEquals(ObservationType.TRANSMIT_FREQ_RATE_5 , metadata.getDataTypes().get(32));
        assertEquals(ObservationType.DOR                  , metadata.getDataTypes().get(33));
        assertEquals(ObservationType.VLBI_DELAY           , metadata.getDataTypes().get(34));
        assertEquals(ObservationType.ANGLE_1              , metadata.getDataTypes().get(35));
        assertEquals(ObservationType.ANGLE_2              , metadata.getDataTypes().get(36));
        assertEquals(ObservationType.MAG                  , metadata.getDataTypes().get(37));
        assertEquals(ObservationType.RCS                  , metadata.getDataTypes().get(38));
        assertEquals(ObservationType.CLOCK_BIAS           , metadata.getDataTypes().get(39));
        assertEquals(ObservationType.CLOCK_DRIFT          , metadata.getDataTypes().get(40));
        assertEquals(ObservationType.STEC                 , metadata.getDataTypes().get(41));
        assertEquals(ObservationType.TROPO_DRY            , metadata.getDataTypes().get(42));
        assertEquals(ObservationType.TROPO_WET            , metadata.getDataTypes().get(43));
        assertEquals(ObservationType.PRESSURE             , metadata.getDataTypes().get(44));
        assertEquals(ObservationType.RHUMIDITY            , metadata.getDataTypes().get(45));
        assertEquals(ObservationType.TEMPERATURE          , metadata.getDataTypes().get(46));
        assertEquals("UTC", metadata.getTimeSystem().name());
        assertEquals(0.0, new AbsoluteDate("2017-06-14T10:53:00.000", utc).durationFrom(metadata.getStartTime()), 0.0);
        assertEquals(0.0, new AbsoluteDate("2017-06-15T10:53:00.000", utc).durationFrom(metadata.getStopTime()), 0.0);
        assertEquals("DSS-25", metadata.getParticipants().get(1));
        assertEquals("yyyy-nnnA", metadata.getParticipants().get(2));
        assertEquals("P3", metadata.getParticipants().get(3));
        assertEquals("P4", metadata.getParticipants().get(4));
        assertEquals("P5", metadata.getParticipants().get(5));
        assertEquals("S", metadata.getTransmitBand());
        assertEquals("L", metadata.getReceiveBand());
        assertEquals(240, metadata.getTurnaroundNumerator(), 0);
        assertEquals(221, metadata.getTurnaroundDenominator(), 0);
        assertEquals(TimetagReference.TRANSMIT, metadata.getTimetagRef());
        assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        assertEquals(IntegrationReference.MIDDLE, metadata.getIntegrationRef());
        assertEquals(32021035200.0, metadata.getFreqOffset(), 0.0);
        assertEquals(RangeMode.COHERENT, metadata.getRangeMode());
        assertEquals(32768.0, metadata.getRawRangeModulus(), 0.0);
        assertEquals(RangeUnits.RU, metadata.getRangeUnits());
        assertEquals(AngleType.RADEC, metadata.getAngleType());
        assertEquals("EME2000", metadata.getReferenceFrame().getName());
        assertEquals(CelestialBodyFrame.EME2000, metadata.getReferenceFrame().asCelestialBodyFrame());
        assertEquals(FramesFactory.getEME2000(), metadata.getReferenceFrame().asFrame());
        assertEquals("HERMITE", metadata.getInterpolationMethod());
        assertEquals(5, metadata.getInterpolationDegree());
        assertEquals(120000.0, metadata.getDopplerCountBias(), 1.0e-5);
        assertEquals(1000.0, metadata.getDopplerCountScale(), 1.0e-10);
        assertFalse(metadata.hasDopplerCountRollover());
        assertEquals(0.000077, metadata.getTransmitDelays().get(1), 0.0);
        assertEquals(0.000077, metadata.getTransmitDelays().get(2), 0.0);
        assertEquals(0.000077, metadata.getTransmitDelays().get(3), 0.0);
        assertEquals(0.000077, metadata.getTransmitDelays().get(4), 0.0);
        assertEquals(0.000077, metadata.getTransmitDelays().get(5), 0.0);
        assertEquals(0.000077, metadata.getReceiveDelays().get(1), 0.0);
        assertEquals(0.000077, metadata.getReceiveDelays().get(2), 0.0);
        assertEquals(0.000077, metadata.getReceiveDelays().get(3), 0.0);
        assertEquals(0.000077, metadata.getReceiveDelays().get(4), 0.0);
        assertEquals(0.000077, metadata.getReceiveDelays().get(5), 0.0);
        assertEquals(DataQuality.RAW, metadata.getDataQuality());
        assertEquals(FastMath.toRadians(1.0), metadata.getCorrectionAngle1(), 0.0);
        assertEquals(FastMath.toRadians(2.0), metadata.getCorrectionAngle2(), 0.0);
        assertEquals(3000.0, metadata.getCorrectionDoppler(), 0.0);
        assertEquals(4.0, metadata.getCorrectionMagnitude(), 0.0);
        assertEquals(5.0, metadata.getRawCorrectionRange(), 0.0);
        assertEquals(6.0, metadata.getCorrectionRcs(), 0.0);
        assertEquals(7.0, metadata.getCorrectionReceive(), 0.0);
        assertEquals(8.0, metadata.getCorrectionTransmit(), 0.0);
        assertEquals(FastMath.toRadians(9.0), metadata.getCorrectionAberrationYearly(), 0.0);
        assertEquals(FastMath.toRadians(10.0), metadata.getCorrectionAberrationDiurnal(), 0.0);
        assertEquals(CorrectionApplied.YES, metadata.getCorrectionsApplied());

        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("All known meta-data keywords displayed");
        assertEquals(metaDataComment, metadata.getComments());

        // Data
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

        // Reference data
        final AbsoluteDate epoch = new AbsoluteDate("2017-06-14T10:53:00.000", utc);
        // Check consistency
        for (int i = 0; i < metadata.getDataTypes().size(); i++) {
            assertEquals(metadata.getDataTypes().get(i), observations.get(i).getType());
            assertEquals(0.0, epoch.shiftedBy((double) (i + 1)).durationFrom(observations.get(i).getEpoch()), 0.0);
            assertEquals((double) (i+1), observations.get(i).getMeasurement(), 1.0e-12);
        }

        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("Data Related Keywords");
        assertEquals(dataComment, file.getSegments().get(0).getData().getComments());
    }

}
