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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
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

class DAFWriterTest {

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
    void testWriteAndReadDAF() throws IOException, URISyntaxException {
        // Parse original file
        final String ex = "/spice/binary/daf/vgr2_jup230.bsp";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DAF original = new DAFParser().parse(source);

        final File tempFile = File.createTempFile("test_daf_", ".daf");
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile);
             DataOutputStream dos = new DataOutputStream(fos)) {

            final DAFWriter writer = new DAFWriter();

            writer.write(dos, original);
        }

        // Read back from temp file
        final DataSource tempSource = new DataSource(tempFile.getAbsolutePath(), () -> {
            try {
                return new FileInputStream(tempFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        final DAF reread = new DAFParser().parse(tempSource);

        // Compare file record fields
        Assertions.assertEquals(original.getMetadata().getDescription(), reread.getMetadata().getDescription());
        Assertions.assertEquals(original.getMetadata().getEndianString(), reread.getMetadata().getEndianString());
        Assertions.assertEquals(original.getMetadata().getFileType(), reread.getMetadata().getFileType());
        Assertions.assertEquals(original.getMetadata().getFirstFreeAddress(), reread.getMetadata().getFirstFreeAddress());
        Assertions.assertEquals(original.getMetadata().getFirstSummaryRecNum(), reread.getMetadata().getFirstSummaryRecNum());
        Assertions.assertEquals(original.getMetadata().getLastSummaryRecNum(), reread.getMetadata().getLastSummaryRecNum());
        Assertions.assertEquals(original.getMetadata().getNumCharsName(), reread.getMetadata().getNumCharsName());
        Assertions.assertEquals(original.getMetadata().getNumDoublesSummary(), reread.getMetadata().getNumDoublesSummary());
        Assertions.assertEquals(original.getMetadata().getNumIntsSummary(), reread.getMetadata().getNumIntsSummary());
        Assertions.assertEquals(original.getMetadata().getSingleSummarySizeDoubles(), reread.getMetadata().getSingleSummarySizeDoubles());

        // Compare comments
        Assertions.assertEquals(original.getComments(), reread.getComments());

        // Compare number of arrays
        Assertions.assertEquals(original.getArrays().size(), reread.getArrays().size());

        // Compare array data
        for (int i = 0; i < original.getArrays().size(); i++) {
            Assertions.assertEquals(original.getArrays().get(i).getArrayName(),
                                  reread.getArrays().get(i).getArrayName());
            Assertions.assertEquals(original.getArrays().get(i).getArrayElements().size(),
                                  reread.getArrays().get(i).getArrayElements().size());
            for (int j = 0; j < original.getArrays().get(i).getArrayElements().size(); j++) {
                final double originalElement = original.getArrays().get(i).getArrayElements().get(j);
                final double rereadElement = reread.getArrays().get(i).getArrayElements().get(j);
                Assertions.assertEquals(originalElement, rereadElement, 1e-6);
            }
        }

        // Byte-by-byte comparison between original and re-written file
        final byte[] rewrittenBytes = Files.readAllBytes(tempFile.toPath());
        final byte[] originalBytes = Files.readAllBytes(Path.of(getClass().getResource(ex).toURI()));
        Assertions.assertEquals(originalBytes.length, rewrittenBytes.length);
        Assertions.assertArrayEquals(originalBytes, rewrittenBytes);
    }

    @Test
    void testWriteAndReadMultiSegmentDAF() throws IOException, URISyntaxException {
        // Parse file with multiple segments contained in several summary records
        final String ex = "/spice/binary/daf/multisegment_multirecord_type1.bsp";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DAF original = new DAFParser().parse(source);

        // Verify original file has 30 arrays (segments)
        Assertions.assertEquals(30, original.getArrays().size());

        final File tempFile = File.createTempFile("test_daf_multisegment_", ".daf");
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile);
             DataOutputStream dos = new DataOutputStream(fos)) {
            final DAFWriter writer = new DAFWriter();
            writer.write(dos, original);
        }

        // Read back from temp file
        final DataSource tempSource = new DataSource(tempFile.getAbsolutePath(), () -> {
            try {
                return new FileInputStream(tempFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        final DAF reread = new DAFParser().parse(tempSource);

        // Compare file record fields
        Assertions.assertEquals(original.getMetadata().getDescription(), reread.getMetadata().getDescription());
        Assertions.assertEquals(original.getMetadata().getEndianString(), reread.getMetadata().getEndianString());
        Assertions.assertEquals(original.getMetadata().getFileType(), reread.getMetadata().getFileType());
        Assertions.assertEquals(original.getMetadata().getNumCharsName(), reread.getMetadata().getNumCharsName());
        Assertions.assertEquals(original.getMetadata().getNumDoublesSummary(), reread.getMetadata().getNumDoublesSummary());
        Assertions.assertEquals(original.getMetadata().getNumIntsSummary(), reread.getMetadata().getNumIntsSummary());
        Assertions.assertEquals(original.getMetadata().getSingleSummarySizeDoubles(), reread.getMetadata().getSingleSummarySizeDoubles());

        // Compare comments
        Assertions.assertEquals(original.getComments(), reread.getComments());

        // Compare number of arrays
        Assertions.assertEquals(30, reread.getArrays().size());

        // Compare all array data
        for (int i = 0; i < original.getArrays().size(); i++) {
            Assertions.assertEquals(original.getArrays().get(i).getArrayName(),
                                  reread.getArrays().get(i).getArrayName());
            Assertions.assertEquals(original.getArrays().get(i).getArrayElements().size(),
                                  reread.getArrays().get(i).getArrayElements().size());
            for (int j = 0; j < original.getArrays().get(i).getArrayElements().size(); j++) {
                final double originalElement = original.getArrays().get(i).getArrayElements().get(j);
                final double rereadElement = reread.getArrays().get(i).getArrayElements().get(j);
                Assertions.assertEquals(originalElement, rereadElement, 1e-6);
            }
        }

        // Byte-by-byte comparison between original and re-written file
        final byte[] rewrittenBytes = Files.readAllBytes(tempFile.toPath());
        final byte[] originalBytes = Files.readAllBytes(Path.of(getClass().getResource(ex).toURI()));
        Assertions.assertEquals(originalBytes.length, rewrittenBytes.length);
        Assertions.assertArrayEquals(originalBytes, rewrittenBytes);
    }

    @Test
    void testBigEndianWriter() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter(true);

            final DAFFileRecord fileRecord = new DAFFileRecord(
                    "DAF/SPK", 2, 6, 40, 4, "test",
                    3, 3, 1, "BIG-IEEE", DAFConstants.FTPSTR);
            final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
            final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
            final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
            final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
            final DAF daf = new DAF(fileRecord, null, Arrays.asList(array));
            writer.write(dos, daf);

            final byte[] bytes = baos.toByteArray();
            // 1 file record + 1 comment record + 1 summary record + 1 name record + 1 data record = 5 records
            Assertions.assertEquals(5 * DAFConstants.RECORD_LENGTH_BYTES, bytes.length);
            // offset for location of the endian string is TYPE_STRING_LENGTH + 5 * INT_SIZE_BYTES (ND, NI, FWARD, BWARD, FREE) + DESCRIPTION_LENGTH
            final int endianOffset = DAFConstants.TYPE_STRING_LENGTH + 5 * DAFConstants.INT_SIZE_BYTES + DAFConstants.DESCRIPTION_LENGTH;
            final String endianStr = new String(bytes, endianOffset, DAFConstants.ENDIAN_STRING_LENGTH, java.nio.charset.StandardCharsets.US_ASCII);
            Assertions.assertEquals(DAFConstants.BIG_ENDIAN_STRING, endianStr);
        }
    }

    @Test
    void testInvalidFileTypeNull() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter();

            final DAFFileRecord nullTypeRecord = new DAFFileRecord(
                    null, 2, 6, 40, 4, "test",
                    3, 3, 1, "LTL-IEEE", DAFConstants.FTPSTR);
            final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
            final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
            final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
            final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
            final DAF daf = new DAF(nullTypeRecord, null, Arrays.asList(array));
            final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> {
                writer.write(dos, daf);
            });
            Assertions.assertEquals(OrekitMessages.INVALID_DAF_FILETYPE_STRING, ex.getSpecifier());
        }
    }

    @Test
    void testInvalidFileTypeBadPattern() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter();

            final DAFFileRecord badTypeRecord = new DAFFileRecord(
                    "INVALID", 2, 6, 40, 4, "test",
                    3, 3, 1, "LTL-IEEE", DAFConstants.FTPSTR);
            final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
            final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
            final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
            final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
            final DAF daf = new DAF(badTypeRecord, null, Arrays.asList(array));
            final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> {
                writer.write(dos, daf);
            });
            Assertions.assertEquals(OrekitMessages.INVALID_DAF_FILETYPE_STRING, ex.getSpecifier());
        }
    }

    @Test
    void testDescriptionTooLong() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter();

            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 61; i++) {
                sb.append('A');
            }
            final DAFFileRecord record = new DAFFileRecord(
                    "DAF/SPK", 2, 6, 40, 4, sb.toString(),
                    3, 3, 1, "LTL-IEEE", DAFConstants.FTPSTR);
            final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
            final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
            final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
            final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
            final DAF daf = new DAF(record, null, Arrays.asList(array));
            final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> {
                writer.write(dos, daf);
            });
            Assertions.assertEquals(OrekitMessages.DAF_TOO_LONG_FILEDESCRIPTION_STRING, ex.getSpecifier());
        }
    }

    @Test
    void testWriteNullComments() throws IOException {
        // Null comments with 1 reserved record (firstSummaryRecNum=3)
        final DAFFileRecord fileRecord = new DAFFileRecord("DAF/SPK", 2, 6, 40, 5, "test",
                3, 3, 1, "LTL-IEEE", DAFConstants.FTPSTR);
        final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
        final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
        final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
        final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
        final DAF daf = new DAF(fileRecord, null, Arrays.asList(array));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter();
            writer.write(dos, daf);

            final byte[] bytes = baos.toByteArray();
            // 1 file record + 1 comment record + 1 summary record + 1 name record + 1 data record = 5 records
            Assertions.assertEquals(5 * DAFConstants.RECORD_LENGTH_BYTES, bytes.length);

            final String parsed = new DAFParser().parse(new DataSource("test", () -> new ByteArrayInputStream(bytes))).getComments();
            Assertions.assertEquals("", parsed);

            final int rec0 = DAFConstants.RECORD_LENGTH_BYTES;
            // EOT at byte 0
            Assertions.assertEquals(4, bytes[rec0]);
            // space
            Assertions.assertEquals(32, bytes[rec0 + 1]);
            // space at last comment area byte
            Assertions.assertEquals(32, bytes[rec0 + 999]);
            // null bytes until end of record
            Assertions.assertEquals(0, bytes[rec0 + 1000]);
        }
    }

    @Test
    void testWriteEmptyComments() throws IOException {
        // Empty string comments with 1 reserved record (firstSummaryRecNum=3)
        final DAFFileRecord fileRecord = new DAFFileRecord("DAF/SPK", 2, 6, 40, 5, "test",
                3, 3, 1, "LTL-IEEE", DAFConstants.FTPSTR);
        final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
        final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
        final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
        final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
        final DAF daf = new DAF(fileRecord, "", Arrays.asList(array));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter();
            writer.write(dos, daf);

            final byte[] bytes = baos.toByteArray();
            // 1 file record + 1 comment record + 1 summary record + 1 name record + 1 data record = 5 records
            Assertions.assertEquals(5 * DAFConstants.RECORD_LENGTH_BYTES, bytes.length);

            final String parsed = new DAFParser().parse(new DataSource("test", () -> new ByteArrayInputStream(bytes))).getComments();
            Assertions.assertEquals("", parsed);

            final int rec0 = DAFConstants.RECORD_LENGTH_BYTES;
            Assertions.assertEquals(4, bytes[rec0]);
            Assertions.assertEquals(32, bytes[rec0 + 1]);
            Assertions.assertEquals(32, bytes[rec0 + 999]);
            Assertions.assertEquals(0, bytes[rec0 + 1000]);
        }
    }

    @Test
    void testInsufficientCommentRecords() throws IOException {
        // Create a comment that needs at least 2 records (more than 1000 characters)
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1001; i++) {
            sb.append('A');
        }
        final String longComment = sb.toString();

        // File record with firstSummaryRecNum=2 and reservedCommentRecords = 0
        final DAFFileRecord record = new DAFFileRecord(
                "DAF/SPK", 2, 6, 40, 4, "test",
                2, 2, 1, "LTL-IEEE", DAFConstants.FTPSTR);
        final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
        final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
        final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
        final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
        final DAF daf = new DAF(record, longComment, Arrays.asList(array));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter();
            final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> {
                writer.write(dos, daf);
            });
            Assertions.assertEquals(OrekitMessages.DAF_INSUFFICIENT_COMMENT_RECORDS, ex.getSpecifier());
        }
    }

    @Test
    void testExtraReservedCommentRecords() throws IOException {
        // Short comment "test" (1 record needed) with 3 reserved records (firstSummaryRecNum=5)
        final DAFFileRecord fileRecord = new DAFFileRecord("DAF/SPK", 2, 6, 40, 5, "test",
                5, 5, 1, "LTL-IEEE", DAFConstants.FTPSTR);
        final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
        final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
        final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
        final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
        final DAF daf = new DAF(fileRecord, "test", Arrays.asList(array));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter();
            writer.write(dos, daf);

            final byte[] bytes = baos.toByteArray();
            // 1 file record + 3 comment records + 1 summary record + 1 name record + 1 data record = 7 records
            Assertions.assertEquals(7 * DAFConstants.RECORD_LENGTH_BYTES, bytes.length);

            final String parsed = new DAFParser().parse(new DataSource("test", () -> new ByteArrayInputStream(bytes))).getComments();
            Assertions.assertEquals("test", parsed);

            // in the first comment record, we should have: first "test" string as comments,
            // then EOT (4), then spaces (32) until the 1000th byte, then nulls until the 1024th byte
            final int rec1 = DAFConstants.RECORD_LENGTH_BYTES;
            Assertions.assertEquals((byte) 't', bytes[rec1]);
            Assertions.assertEquals(4, bytes[rec1 + 4]);
            Assertions.assertEquals(32, bytes[rec1 + 999]);
            Assertions.assertEquals(0, bytes[rec1 + 1000]);
            // extra unused records just have EOT at the beginning (no remaining text)
            Assertions.assertEquals(4, bytes[2 * DAFConstants.RECORD_LENGTH_BYTES]);
            Assertions.assertEquals(4, bytes[3 * DAFConstants.RECORD_LENGTH_BYTES]);
        }
    }

    @Test
    void testCommentsExactlyMaxChars() throws IOException {
        // exactly 1000 characters require 2 comment records (firstSummaryRecNum=4):
        // this is because the text fills the first record's 1000-byte area completely,
        // and the EOT byte then has to go into a second record.
        final DAFFileRecord fileRecord = new DAFFileRecord("DAF/SPK", 2, 6, 40, 5, "test",
                4, 4, 1, "LTL-IEEE", DAFConstants.FTPSTR);

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DAFConstants.COMMENT_RECORD_MAX_CHARS; i++) {
            sb.append('A');
        }

        final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
        final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
        final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
        final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
        final DAF daf = new DAF(fileRecord, sb.toString(), Arrays.asList(array));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter();
            writer.write(dos, daf);

            final byte[] bytes = baos.toByteArray();
            // 1 file record + 2 comment records + 1 summary record + 1 name record + 1 data record = 6 records
            Assertions.assertEquals(6 * DAFConstants.RECORD_LENGTH_BYTES, bytes.length);

            final String parsed = new DAFParser().parse(new DataSource("test", () -> new ByteArrayInputStream(bytes))).getComments();
            Assertions.assertEquals(1000, parsed.length());

            // record 1 has 1000 'A's (byte value 65) fill the comment area (no EOT),
            // null bytes until end of record
            final int rec1 = DAFConstants.RECORD_LENGTH_BYTES;
            Assertions.assertEquals(65, bytes[rec1]);
            Assertions.assertEquals(65, bytes[rec1 + 999]);
            Assertions.assertEquals(0, bytes[rec1 + 1000]);

            // record 2 just contains the EOT at the start, then spaces, then nulls
            final int rec2 = 2 * DAFConstants.RECORD_LENGTH_BYTES;
            Assertions.assertEquals(4, bytes[rec2]);
            Assertions.assertEquals(32, bytes[rec2 + 1]);
            Assertions.assertEquals(0, bytes[rec2 + 1000]);
        }
    }

    @Test
    void testCommentsMultipleRecords() throws IOException {
        // 1500 'A' characters requiring 2 records (firstSummaryRecNum=4)
        final DAFFileRecord fileRecord = new DAFFileRecord("DAF/SPK", 2, 6, 40, 5, "test",
                4, 4, 1, "LTL-IEEE", DAFConstants.FTPSTR);

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1500; i++) {
            sb.append('A');
        }

        final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
        final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 2, 1, 10);
        final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
        final DAFArray array = new DAFArray("TEST", summary, Arrays.asList(1.0, 2.0, 3.0));
        final DAF daf = new DAF(fileRecord, sb.toString(), Arrays.asList(array));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter();
            writer.write(dos, daf);

            final byte[] bytes = baos.toByteArray();
            // 1 file record + 2 comment records + 1 summary record + 1 name record + 1 data record = 6 records
            Assertions.assertEquals(6 * DAFConstants.RECORD_LENGTH_BYTES, bytes.length);

            final String parsed = new DAFParser().parse(new DataSource("test", () -> new ByteArrayInputStream(bytes))).getComments();
            Assertions.assertEquals(1500, parsed.length());

            final int rec1 = DAFConstants.RECORD_LENGTH_BYTES;
            Assertions.assertEquals(65, bytes[rec1]);
            Assertions.assertEquals(65, bytes[rec1 + 999]);
            Assertions.assertEquals(0, bytes[rec1 + 1000]);

            final int rec2 = 2 * DAFConstants.RECORD_LENGTH_BYTES;
            Assertions.assertEquals(65, bytes[rec2 + 499]);
            Assertions.assertEquals(4, bytes[rec2 + 500]);
            Assertions.assertEquals(32, bytes[rec2 + 501]);
            Assertions.assertEquals(0, bytes[rec2 + 1000]);
        }
    }

    @Test
    void testSummaryIntAlignmentPadding() throws IOException {
        // create a DAF with NI=5 (odd) to force a required summary alignment padding
        // summaryBytes = 2*8 + 5*4 = 36, targetBytes = 5*8 = 40 -> 4 bytes padding
        final DAFFileRecord fileRecord = new DAFFileRecord(
                "DAF/SPK", 2, 5, 40, 5, "test",
                2, 2, 1, "LTL-IEEE", DAFConstants.FTPSTR);

        final List<Double> summaryDoubles = Arrays.asList(0.0, 100.0);
        final List<Integer> summaryInts = Arrays.asList(1, 2, 3, 1, 10);
        final DAFArraySummary summary = new DAFArraySummary(summaryDoubles, summaryInts, 1, 80);
        final List<Double> elements = Arrays.asList(1.0, 2.0, 3.0);
        final DAFArray array = new DAFArray("TEST", summary, elements);

        final DAF daf = new DAF(fileRecord, null, Arrays.asList(array));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter();
            writer.write(dos, daf);

            // verify output has correct size
            Assertions.assertTrue(baos.size() > 0);
            Assertions.assertEquals(0, baos.size() % DAFConstants.RECORD_LENGTH_BYTES);
        }
    }

    @Test
    void testNullDAF() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            final DAFWriter writer = new DAFWriter();

            final OrekitException ex = Assertions.assertThrows(OrekitException.class, () -> {
                writer.write(dos, null);
            });
            Assertions.assertEquals(OrekitMessages.NULL_DAF, ex.getSpecifier());
        }
    }
}
