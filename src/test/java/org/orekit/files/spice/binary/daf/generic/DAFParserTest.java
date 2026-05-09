/* Contributed in the public domain.
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
package org.orekit.files.spice.binary.daf.generic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.spice.binary.daf.generic.DAF;
import org.orekit.files.spice.binary.daf.generic.DAFArray;
import org.orekit.files.spice.binary.daf.generic.DAFArraySummary;
import org.orekit.files.spice.binary.daf.generic.DAFConstants;
import org.orekit.files.spice.binary.daf.generic.DAFFileRecord;
import org.orekit.files.spice.binary.daf.generic.DAFParser;
import org.orekit.files.spice.binary.daf.generic.DAFWriter;

class DAFParserTest {

    // For this and other tests involving files based on the DAF format, files were obtained from
    // one of the following sources (in this order if available):
    // - Publicly available "real" files (such as SPK files for ephemerides of actual missions
    //   or real celestial bodies PCK files)
    // - Files generated using CSPICE routines replicating code found in SPICE docs/code comments
    // - Files generated using custom CSPICE-based code
    // Parsing and evaluation of all files is matched against CSPICE results.
    // Additionally, the results produced by writers of DAF and DAF-based file types
    // have been verified byte-by-byte against the original SPICE-generated files.
    @Test
    void testParseDAF() {
        // test for generic DAF
        final String ex = "/spice/binary/daf/vgr2_jup230.bsp";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DAF parsedFile = new DAFParser().parse(source);
        // verify that element arrays of the DAF file is a list of length 1
        Assertions.assertEquals(1, parsedFile.getArrays().size());
        final DAFFileRecord testFileRecord = parsedFile.getMetadata();
        final String testComments = parsedFile.getComments();
        final DAFArray testArray = parsedFile.getArrays().getFirst();
        final DAFArraySummary testArraySummary = testArray.getArraySummary();
        final List<Double> testArrayElements = testArray.getArrayElements();
        // file record tests
        Assertions.assertEquals("NIO2SPK", testFileRecord.getDescription());
        Assertions.assertEquals("LTL-IEEE", testFileRecord.getEndianString());
        Assertions.assertEquals("DAF/SPK", testFileRecord.getFileType());
        Assertions.assertEquals(41271, testFileRecord.getFirstFreeAddress());
        Assertions.assertEquals(3, testFileRecord.getFirstSummaryRecNum());
        Assertions.assertEquals(3, testFileRecord.getLastSummaryRecNum());
        Assertions.assertEquals(40, testFileRecord.getNumCharsName());
        Assertions.assertEquals(2, testFileRecord.getNumDoublesSummary());
        Assertions.assertEquals(6, testFileRecord.getNumIntsSummary());
        Assertions.assertEquals(5, testFileRecord.getSingleSummarySizeDoubles());
        // comments tests
        Assertions.assertEquals(350, testComments.length());
        // array tests
        Assertions.assertEquals("vgr2.jup230.nio", testArray.getArrayName());
        Assertions.assertEquals(4097, testArraySummary.getInitialArrayAddress());
        Assertions.assertEquals(330161, testArraySummary.getFinalArrayAddress());
        Assertions.assertEquals(2, testArraySummary.getSummaryDoubles().size());
        Assertions.assertEquals(6, testArraySummary.getSummaryInts().size());
        Assertions.assertEquals(-649364400.000000, testArraySummary.getSummaryDoubles().getFirst(), 1e-6);
        Assertions.assertEquals(-645364800.000000, testArraySummary.getSummaryDoubles().get(1), 1e-6);
        Assertions.assertEquals(-32, testArraySummary.getSummaryInts().getFirst());
        Assertions.assertEquals(5, testArraySummary.getSummaryInts().get(1));
        Assertions.assertEquals(1, testArraySummary.getSummaryInts().get(2));
        Assertions.assertEquals(1, testArraySummary.getSummaryInts().get(3));
        Assertions.assertEquals(513, testArraySummary.getSummaryInts().get(4));
        Assertions.assertEquals(41270, testArraySummary.getSummaryInts().get(5));
        Assertions.assertEquals(40758, testArrayElements.size());
        Assertions.assertEquals(-649364399.000000, testArrayElements.getFirst(), 1e-6);
        Assertions.assertEquals(1.000000, testArrayElements.get(1), 1e-6);
        Assertions.assertEquals(-0.603684, testArrayElements.get(7401), 1e-6);
        Assertions.assertEquals(-645842469.316287, testArrayElements.get(40756), 1e-6);
        Assertions.assertEquals(566.000000, testArrayElements.get(40757), 1e-6);
    }

    @Test
    void testParseBigEndianDAF() {
        // parse a DAF file written with big-endian byte order
        final String ex = "/spice/binary/daf/big_endian_test.bsp";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DAF parsedFile = new DAFParser().parse(source);

        // file record
        final DAFFileRecord fileRecord = parsedFile.getMetadata();
        Assertions.assertEquals("DAF/SPK", fileRecord.getFileType());
        Assertions.assertEquals("BIG-IEEE", fileRecord.getEndianString());
        Assertions.assertEquals("test", fileRecord.getDescription());
        Assertions.assertEquals(2, fileRecord.getNumDoublesSummary());
        Assertions.assertEquals(6, fileRecord.getNumIntsSummary());
        Assertions.assertEquals(2, fileRecord.getFirstSummaryRecNum());
        Assertions.assertEquals(2, fileRecord.getLastSummaryRecNum());
        Assertions.assertEquals(388, fileRecord.getFirstFreeAddress());

        // single array
        Assertions.assertEquals(1, parsedFile.getArrays().size());
        final DAFArray array = parsedFile.getArrays().getFirst();
        Assertions.assertEquals("TEST", array.getArrayName());

        // summary
        // doubles
        final DAFArraySummary summary = array.getArraySummary();
        Assertions.assertEquals(0.0, summary.getSummaryDoubles().getFirst(), 1e-6);
        Assertions.assertEquals(100.0, summary.getSummaryDoubles().get(1), 1e-6);
        // ints
        Assertions.assertEquals(1, summary.getSummaryInts().getFirst());
        Assertions.assertEquals(2, summary.getSummaryInts().get(1));
        Assertions.assertEquals(3, summary.getSummaryInts().get(2));
        Assertions.assertEquals(2, summary.getSummaryInts().get(3));
        Assertions.assertEquals(385, summary.getSummaryInts().get(4));
        Assertions.assertEquals(387, summary.getSummaryInts().get(5));

        // elements
        Assertions.assertEquals(3, array.getArrayElements().size());
        Assertions.assertEquals(1.0, array.getArrayElements().getFirst(), 1e-10);
        Assertions.assertEquals(2.0, array.getArrayElements().get(1), 1e-10);
        Assertions.assertEquals(3.0, array.getArrayElements().get(2), 1e-10);
    }

    @Test
    void testParseIOException() {
        final DataSource source = new DataSource("error", (DataSource.StreamOpener) () -> { throw new IOException("test IO error"); });
        final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> new DAFParser().parse(source));
        Assertions.assertInstanceOf(IOException.class, ex.getCause());
        Assertions.assertEquals("test IO error", ex.getCause().getMessage());
    }

    @Test
    void testParseFileRecordInvalidFileType() {
        final byte[] rawData = new byte[DAFConstants.RECORD_LENGTH_BYTES];
        System.arraycopy("INVALID\0".getBytes(StandardCharsets.US_ASCII), 0, rawData, 0, DAFConstants.TYPE_STRING_LENGTH);

        final DataSource source = new DataSource("test", () -> new ByteArrayInputStream(rawData));
        final DAFParser parser = new DAFParser();
        final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> {
            parser.parse(source);
        });
        Assertions.assertEquals(OrekitMessages.INVALID_DAF_FILETYPE_STRING, ex.getSpecifier());
    }

    @Test
    void testParseFileRecordInvalidFTPString() {
        final byte[] rawData = new byte[DAFConstants.RECORD_LENGTH_BYTES];
        final ByteBuffer buf = ByteBuffer.wrap(rawData);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        // Valid type string
        final byte[] typeBytes = new byte[DAFConstants.TYPE_STRING_LENGTH];
        System.arraycopy("DAF/SPK".getBytes(StandardCharsets.US_ASCII), 0, typeBytes, 0, 7);
        buf.put(typeBytes);
        // ND=2, NI=6
        buf.putInt(2);
        buf.putInt(6);
        // Description (60 bytes)
        buf.put(new byte[DAFConstants.DESCRIPTION_LENGTH]);
        // firstSummaryRecNum, lastSummaryRecNum, firstFreeAddress
        buf.putInt(2);
        buf.putInt(2);
        buf.putInt(1);
        // endianString
        final byte[] endianBytes = new byte[DAFConstants.ENDIAN_STRING_LENGTH];
        System.arraycopy("LTL-IEEE".getBytes(StandardCharsets.US_ASCII), 0, endianBytes, 0, DAFConstants.ENDIAN_STRING_LENGTH);
        buf.put(endianBytes);
        // FTP string left as zero bytes
        final DataSource source = new DataSource("test", () -> new ByteArrayInputStream(rawData));
        final DAFParser parser = new DAFParser();
        final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> {
            parser.parse(source);
        });
        Assertions.assertEquals(OrekitMessages.INVALID_DAF_FTPSTR, ex.getSpecifier());
    }

    @Test
    void testParseCommentsIncompleteRecord() throws IOException {
        // write a valid DAF with comments (firstSummaryRecNum=3, so 1 comment record), then truncate
        final DAFFileRecord fileRecord = new DAFFileRecord("DAF/SPK", 2, 6, 40, 5, "test",
                3, 3, 1, "LTL-IEEE", DAFConstants.FTPSTR);
        final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
        final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
        final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
        final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
        final DAF daf = new DAF(fileRecord, "test", Arrays.asList(array));

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new DAFWriter().write(new DataOutputStream(baos), daf);
        // truncate to just the file record (comment record missing)
        final byte[] rawData = Arrays.copyOf(baos.toByteArray(), DAFConstants.RECORD_LENGTH_BYTES);

        final DataSource source = new DataSource("test", () -> new ByteArrayInputStream(rawData));
        final DAFParser parser = new DAFParser();
        final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> {
            parser.parse(source);
        });
        Assertions.assertEquals(OrekitMessages.INCOMPLETE_DAF_COMMENT_RECORD, ex.getSpecifier());
    }

    @Test
    void testParseSummaryRecordInvalidEndianness() throws IOException {
        // write a valid big-endian DAF, then corrupt the endianness string
        final DAFFileRecord fileRecord = new DAFFileRecord("DAF/SPK", 2, 6, 40, 5, "test",
                2, 2, 1, "BIG-IEEE", DAFConstants.FTPSTR);
        final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
        final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
        final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
        final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
        final DAF daf = new DAF(fileRecord, null, Arrays.asList(array));

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new DAFWriter(true).write(new DataOutputStream(baos), daf);
        final byte[] rawData = baos.toByteArray();

        // overwrite the endianness string with an invalid value
        final int endianOffset = DAFConstants.TYPE_STRING_LENGTH + 5 * DAFConstants.INT_SIZE_BYTES + DAFConstants.DESCRIPTION_LENGTH;
        System.arraycopy("INVALID_".getBytes(StandardCharsets.US_ASCII), 0, rawData, endianOffset, DAFConstants.ENDIAN_STRING_LENGTH);

        final DataSource source = new DataSource("test", () -> new ByteArrayInputStream(rawData));
        final DAFParser parser = new DAFParser();
        final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> {
            parser.parse(source);
        });
        Assertions.assertEquals(OrekitMessages.INVALID_DAF_ENDIANNESS, ex.getSpecifier());
    }

    @Test
    void testParseSummaryRecordIncomplete() throws IOException {
        // write a valid DAF with no comments (firstSummaryRecNum=2), then truncate before summary record
        final DAFFileRecord fileRecord = new DAFFileRecord("DAF/SPK", 2, 6, 40, 5, "test",
                2, 2, 1, "LTL-IEEE", DAFConstants.FTPSTR);
        final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
        final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
        final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
        final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
        final DAF daf = new DAF(fileRecord, null, Arrays.asList(array));

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new DAFWriter().write(new DataOutputStream(baos), daf);
        // truncate to just the file record
        final byte[] rawData = Arrays.copyOf(baos.toByteArray(), DAFConstants.RECORD_LENGTH_BYTES);

        final DataSource source = new DataSource("test", () -> new ByteArrayInputStream(rawData));
        final DAFParser parser = new DAFParser();
        final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> {
            parser.parse(source);
        });
        Assertions.assertEquals(OrekitMessages.INCOMPLETE_DAF_SUMMARY_RECORD, ex.getSpecifier());
    }

    @Test
    void testParseNameRecordIncomplete() throws IOException {
        // write a valid DAF with no comments (firstSummaryRecNum=2), then truncate after summary record
        final DAFFileRecord fileRecord = new DAFFileRecord("DAF/SPK", 2, 6, 40, 5, "test",
                2, 2, 1, "LTL-IEEE", DAFConstants.FTPSTR);
        final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
        final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
        final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
        final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
        final DAF daf = new DAF(fileRecord, null, Arrays.asList(array));

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new DAFWriter().write(new DataOutputStream(baos), daf);
        // truncate to file record + summary record (no name record)
        final byte[] rawData = Arrays.copyOf(baos.toByteArray(), 2 * DAFConstants.RECORD_LENGTH_BYTES);

        final DataSource source = new DataSource("test", () -> new ByteArrayInputStream(rawData));
        final DAFParser parser = new DAFParser();
        final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> {
            parser.parse(source);
        });
        Assertions.assertEquals(OrekitMessages.INCOMPLETE_DAF_NAME_RECORD, ex.getSpecifier());
    }
}
