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

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Writer for generic DAF binary files. Note that this class is not thread-safe
 * since it uses an internal buffer.
 *
 * @author Rafael Ayala
 * @since 14.0
 */
public class DAFWriter {

    /**
     * Character set for comments (US_ASCII).
     */
    private static final Charset US_ASCII = StandardCharsets.US_ASCII;

    /**
     * Byte buffer for endianness conversion.
     */
    private final ByteBuffer bb;

    /**
     * Flag to indicate if writer writes big-endian bytes.
     */
    private final boolean bigEndian;

    /**
     * Basic constructor to set up endianness.
     * @param bigEndian flag indicating if the writer should use big endianness
     */
    public DAFWriter(final boolean bigEndian) {
        this.bigEndian = bigEndian;
        // allocate 8-byte buffer (largest single element we will write are doubles) and set byte order
        this.bb = ByteBuffer.allocate(DAFConstants.DOUBLE_SIZE_BYTES);
        this.bb.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Basic constructor with default little endianness (bigEndian flag set to false).
     */
    public DAFWriter() {
        this(false);
    }

    /**
     * Write a DAF file record (global file metadata).
     * @param dos data output stream to write to
     * @param fileRecord the DAFFileRecord to write
     * @throws IOException if error occurs
     */
    private void writeFileRecord(final DataOutputStream dos, final DAFFileRecord fileRecord) throws IOException {
        int index = 0;
        // 1. LOCIDW (8 bytes)
        // fileType string must follow the pattern DAF/XXXX
        // (it is OK to have less than 4 characters after DAF/, but not more)
        if (fileRecord.getFileType() == null || !DAFConstants.FILE_TYPE_PATTERN.matcher(fileRecord.getFileType()).matches()) {
            throw new OrekitException(OrekitMessages.INVALID_DAF_FILETYPE_STRING, fileRecord.getFileType());
        }
        writePaddedAscii(dos, fileRecord.getFileType(), DAFConstants.TYPE_STRING_LENGTH);
        index += DAFConstants.TYPE_STRING_LENGTH;
        // 2. ND (int, 4 bytes)
        writeInt(dos, fileRecord.getNumDoublesSummary());
        index += DAFConstants.INT_SIZE_BYTES;
        // 3. NI (int, 4 bytes)
        writeInt(dos, fileRecord.getNumIntsSummary());
        index += DAFConstants.INT_SIZE_BYTES;
        // 4. LOCIFN (chars, 60 bytes)
        if (fileRecord.getDescription() != null && fileRecord.getDescription().length() > DAFConstants.DESCRIPTION_LENGTH) {
            throw new OrekitException(OrekitMessages.DAF_TOO_LONG_FILEDESCRIPTION_STRING, fileRecord.getDescription().length());
        }
        writePaddedAscii(dos, fileRecord.getDescription(), DAFConstants.DESCRIPTION_LENGTH);
        index += DAFConstants.DESCRIPTION_LENGTH;
        // 5. FWARD (int, 4 bytes)
        writeInt(dos, fileRecord.getFirstSummaryRecNum());
        index += DAFConstants.INT_SIZE_BYTES;
        // 6. BWARD (int, 4 bytes)
        writeInt(dos, fileRecord.getLastSummaryRecNum());
        index += DAFConstants.INT_SIZE_BYTES;
        // 7. FREE (int, 4 bytes)
        writeInt(dos, (int) fileRecord.getFirstFreeAddress());
        index += DAFConstants.INT_SIZE_BYTES;
        // 8. LOCFMT (chars, 8 bytes) endian format string
        writePaddedAscii(dos, bigEndian ? DAFConstants.BIG_ENDIAN_STRING : DAFConstants.LITTLE_ENDIAN_STRING, DAFConstants.ENDIAN_STRING_LENGTH);
        index += DAFConstants.ENDIAN_STRING_LENGTH;
        // 9. PRENUL (603 bytes) fill with zeroes until start of DAFConstants.FTPSTR
        for (; index < DAFConstants.FTP_STRING_OFFSET; index++) {
            dos.writeByte(DAFConstants.NULL_ASCII);
        }
        // 10. DAFConstants.FTPSTR (28 bytes) write the FTP string using ISO_8859_1 encoding,
        // always uses the defined DAFConstants.FTPSTR constant (unchanged unless NAIF changes DAF specs)
        final byte[] ftpBytes = DAFConstants.FTPSTR.getBytes(StandardCharsets.ISO_8859_1);
        if (ftpBytes.length >= DAFConstants.FTP_STRING_LENGTH) {
            dos.write(ftpBytes, 0, DAFConstants.FTP_STRING_LENGTH);
        } else {
            dos.write(ftpBytes);
            for (int i = ftpBytes.length; i < DAFConstants.FTP_STRING_LENGTH; i++) {
                dos.writeByte(DAFConstants.NULL_ASCII);
            }
        }
        index += DAFConstants.FTP_STRING_LENGTH;
        // 11. PSTNUL (remaining bytes up to RECORD_SIZE)
        for (; index < DAFConstants.RECORD_LENGTH_BYTES; index++) {
            dos.writeByte(DAFConstants.NULL_ASCII);
        }
    }

    /**
     * Calculate the number of comment records needed for the given comments.
     * Each comment record can hold up to 1000 characters of text, following NAIF specs.
     * @param comments the comment string (can be null or empty)
     * @return number of comment records needed (minimum 0)
     */
    private static int calculateNumCommentRecords(final String comments) {
        if (comments == null || comments.isEmpty()) {
            return 0;
        }
        final int commentLength = comments.getBytes(US_ASCII).length;
        // Each record holds DAFConstants.COMMENT_RECORD_MAX_CHARS characters, and we need
        // 1 byte for EOT in the last record
        if (commentLength == 0) {
            return 0;
        }
        // Calculate records needed: commentLength bytes of text + 1 byte for the EOT marker.
        // Each record holds COMMENT_RECORD_MAX_CHARS (1000) bytes in its comment area.
        // This matches CSPICE behavior, which never truncates comments to fit EOT.
        return (commentLength + 1 + DAFConstants.COMMENT_RECORD_MAX_CHARS - 1) / DAFConstants.COMMENT_RECORD_MAX_CHARS;
    }

    /**
     * Write the comment records. The number of comment records is
     * calculated as metadata.getFirstSummaryRecNum() - 2, and this
     * is validated against the actual space needed for the comments.
     * @param dos data output stream to write to
     * @param comments the comment string to write
     * @param fileRecord the file record metadata
     * @throws IOException if an I/O error occurs
     */
    private void writeComments(final DataOutputStream dos, final String comments, final DAFFileRecord fileRecord) throws IOException {
        final int reservedCommentRecords = FastMath.max(0, fileRecord.getFirstSummaryRecNum() - 2);
        final int neededCommentRecords = calculateNumCommentRecords(comments);

        if (neededCommentRecords > reservedCommentRecords) {
            throw new OrekitException(OrekitMessages.DAF_INSUFFICIENT_COMMENT_RECORDS, neededCommentRecords, reservedCommentRecords);
        }

        writeComments(dos, comments, reservedCommentRecords);
    }

    /**
     * Write the comment records with a specified number of records.
     *
     * @param dos data output stream to write to
     * @param comments the comment string to write (can be null or empty)
     * @param numCommentRecords number of comment records to write
     * @throws IOException if an I/O error occurs
     */
    private void writeComments(final DataOutputStream dos, final String comments, final int numCommentRecords) throws IOException {
        // Convert newlines to null bytes (DAF format uses \0 for line separators)
        final byte[] rawBytes = (comments == null || comments.isEmpty()) ? new byte[0] : comments.getBytes(US_ASCII);
        final byte[] commentBytes = new byte[rawBytes.length];
        for (int i = 0; i < rawBytes.length; i++) {
            commentBytes[i] = (rawBytes[i] == '\n') ? (byte) 0 : rawBytes[i];
        }
        int offset = 0;

        for (int rec = 0; rec < numCommentRecords; rec++) {
            final int remaining = commentBytes.length - offset;
            final boolean isLastRec = rec == numCommentRecords - 1;
            offset = writeCommentRecord(dos, commentBytes, offset, remaining, isLastRec);
        }
    }

    /**
     * Write a single comment record and return the updated offset.
     * According to NAIF specs, each comment record has 1000 bytes for text,
     * followed by 24 bytes of padding to reach 1024 bytes total.
     * An EOT (\4 byte) marker must appear after the last text in the last reserved record.
     * @param dos data output stream to write to
     * @param commentBytes the full comment text as bytes
     * @param offset current position in commentBytes
     * @param remaining number of bytes remaining to write
     * @param isLastRec true if this is the last reserved comment record
     * @return updated offset after writing this record
     * @throws IOException if an I/O error occurs
     */
    private int writeCommentRecord(final DataOutputStream dos, final byte[] commentBytes, final int offset, final int remaining, final boolean isLastRec) throws IOException {
        int currentOffset = offset; // Use local variable to avoid modifying final parameter
        if (remaining <= 0) {
            // No more text: put an EOT at the start of the comment-area and spaces for the rest
            // This matches CSPICE handling: initialize comment records with spaces, so unused parts are spaces
            // up to 1000 bytes. The always unused last 24 bytes are null (\0) bytes
            dos.writeByte(DAFConstants.EOT_ASCII);
            for (int i = 1; i < DAFConstants.COMMENT_RECORD_MAX_CHARS; i++) {
                dos.writeByte(DAFConstants.SPACE_ASCII);
            }
        } else {
            // Determine how much text to write in this record
            final int toWrite = FastMath.min(remaining, DAFConstants.COMMENT_RECORD_MAX_CHARS);

            // Write the text
            dos.write(commentBytes, currentOffset, toWrite);
            currentOffset += toWrite;

            // Write EOT and space-pad if this is the last record, or if all text has been
            // written and there is room for the EOT byte within the comment area.
            // When text fills the entire 1000-byte area exactly (toWrite == COMMENT_RECORD_MAX_CHARS),
            // the EOT is deferred to the next record (matching CSPICE behavior).
            if (isLastRec || currentOffset >= commentBytes.length && toWrite < DAFConstants.COMMENT_RECORD_MAX_CHARS) {
                dos.writeByte(DAFConstants.EOT_ASCII); // EOT
                // Pad the rest with spaces (to match CSPICE behavior)
                for (int i = toWrite + 1; i < DAFConstants.COMMENT_RECORD_MAX_CHARS; i++) {
                    dos.writeByte(DAFConstants.SPACE_ASCII); // space
                }
            } else {
                // Pad to DAFConstants.COMMENT_RECORD_MAX_CHARS if needed
                for (int i = toWrite; i < DAFConstants.COMMENT_RECORD_MAX_CHARS; i++) {
                    dos.writeByte(DAFConstants.SPACE_ASCII);
                }
            }
        }

        // Pad the rest of the 1024-byte record (from byte 1000 to 1024) with zeros
        for (int i = DAFConstants.COMMENT_RECORD_MAX_CHARS; i < DAFConstants.RECORD_LENGTH_BYTES; i++) {
            dos.writeByte(DAFConstants.NULL_ASCII);
        }
        return currentOffset;
    }

    /**
     * Write a summary record containing multiple summaries.
     * @param dos data output stream to write to
     * @param summaries list of summaries to include in this record
     * @param numDoubles number of doubles per summary (ND)
     * @param numInts number of integers per summary (NI)
     * @param nextSummaryRecNum record number of next summary record (0 if none)
     * @param prevSummaryRecNum record number of previous summary record (0 if none)
     * @throws IOException if an I/O error occurs
     */
    private void writeSummaryRecord(final DataOutputStream dos, final List<DAFArraySummary> summaries, final int numDoubles, final int numInts,
                                    final int nextSummaryRecNum, final int prevSummaryRecNum) throws IOException {
        final int singleSummarySizeDoubles = numDoubles + (numInts + 1) / 2;

        // Write control doubles (NEXT, PREV, NSUM)
        // Note that they are written as doubles but will actually always be integer numbers
        writeDouble(dos, nextSummaryRecNum);
        writeDouble(dos, prevSummaryRecNum);
        writeDouble(dos, summaries.size());

        int bytesWritten = 3 * DAFConstants.DOUBLE_SIZE_BYTES;

        // Write each summary
        for (final DAFArraySummary summary : summaries) {
            // ND doubles
            for (double d : summary.getSummaryDoubles()) {
                writeDouble(dos, d);
            }
            bytesWritten += numDoubles * DAFConstants.DOUBLE_SIZE_BYTES;

            // NI ints
            for (int i : summary.getSummaryInts()) {
                writeInt(dos, i);
            }
            bytesWritten += numInts * DAFConstants.INT_SIZE_BYTES;

            // Pad to align to singleSummarySizeDoubles * double byte size (8)
            final int summaryBytes = numDoubles * DAFConstants.DOUBLE_SIZE_BYTES + numInts * DAFConstants.INT_SIZE_BYTES;
            final int targetBytes = singleSummarySizeDoubles * DAFConstants.DOUBLE_SIZE_BYTES;
            for (int i = summaryBytes; i < targetBytes; i++) {
                dos.writeByte(DAFConstants.NULL_ASCII);
                bytesWritten++;
            }
        }

        // Pad rest of the record with null bytes
        for (int i = bytesWritten; i < DAFConstants.RECORD_LENGTH_BYTES; i++) {
            dos.writeByte(DAFConstants.NULL_ASCII);
        }
    }

    /**
     * Write a name record with multiple names.
     * @param dos data output stream to write to
     * @param names list of array names
     * @param numCharsName number of characters per name
     * @throws IOException if an I/O error occurs
     */
    private void writeNameRecord(final DataOutputStream dos, final List<String> names, final int numCharsName) throws IOException {
        int bytesWritten = 0;

        for (final String name : names) {
            final byte[] nameField = new byte[numCharsName];
            // Fill with spaces
            for (int i = 0; i < numCharsName; i++) {
                nameField[i] = DAFConstants.SPACE_ASCII;
            }
            // Copy name bytes (truncate if needed)
            final byte[] nameBytes = name == null ? new byte[0] : name.getBytes(US_ASCII);
            final int toCopy = FastMath.min(nameBytes.length, numCharsName);
            System.arraycopy(nameBytes, 0, nameField, 0, toCopy);
            dos.write(nameField);
            bytesWritten += numCharsName;
        }

        // Pad with spaces up to byte 1000
        for (int i = bytesWritten; i < DAFConstants.COMMENT_RECORD_MAX_CHARS; i++) {
            dos.writeByte(DAFConstants.SPACE_ASCII);
        }
        // Pad last 24 bytes with zeros
        for (int i = DAFConstants.COMMENT_RECORD_MAX_CHARS; i < DAFConstants.RECORD_LENGTH_BYTES; i++) {
            dos.writeByte(DAFConstants.NULL_ASCII);
        }
    }

    /**
     * Write a full {@link DAF} object to the stream.
     * This method handles multiple summaries per summary record as SPICE does,
     * with the corresponding multiple names per name record, record linking
     * through control words and calculation of data records and data addresses
     * for each data array. Data is written contiguously (like CSPICE) without
     * padding between arrays, only padding at the end of the data records for a
     * given batch.
     * For example, if we have a summary record with 20 summaries, and each array
     * has 1500 doubles, then we will have the following structure:
     * - Summary record with 20 summaries
     * - Name record with 20 names
     * - Data records for arrays 1 to 20, each with 1500 doubles, written contiguously
     *   Since each data record can contain up to 128 doubles, this means we will have
     *   as many data records as needed to store all 20 arrays contiguously. So
     *   a total of 20 * 1500 = 30000 doubles, 30000 / 128 = 235 data records (round up)
     *   The last data record will be padded with null bytes after the last double of array 20.
     * Note that this method is not thread-safe since it uses an internal buffer.
     *
     * @param dos data output stream to write to
     * @param daf the DAF object to write
     * @throws IOException if an I/O error occurs
     */
    public void write(final DataOutputStream dos, final DAF daf) throws IOException {
        try {
            if (daf == null) {
                throw new OrekitException(OrekitMessages.NULL_DAF);
            }
            final DAFFileRecord fileRecord = daf.getMetadata();
            final int numDoubles = fileRecord.getNumDoublesSummary();
            final int numInts = fileRecord.getNumIntsSummary();
            final int numCharsName = fileRecord.getNumCharsName();
            final int singleSummarySizeDoubles = fileRecord.getSingleSummarySizeDoubles();

            // validate that there is enough comment records
            final int reservedCommentRecords = FastMath.max(0, fileRecord.getFirstSummaryRecNum() - 2);
            final int neededCommentRecords = calculateNumCommentRecords(daf.getComments());

            if (neededCommentRecords > reservedCommentRecords) {
                throw new OrekitException(OrekitMessages.DAF_INSUFFICIENT_COMMENT_RECORDS, neededCommentRecords, reservedCommentRecords);
            }

            // Calculate how many summaries fit per record (125 doubles available after 3 control integers stored as doubles)
            final int maxSummariesPerRecord = DAFConstants.SUMMARY_RECORD_MAX_SUMMARY_DOUBLES / singleSummarySizeDoubles;

            // Group arrays
            final List<DAFArray> arrays = daf.getArrays();
            final int numArrays = arrays.size();
            final int numBatches = (numArrays + maxSummariesPerRecord - 1) / maxSummariesPerRecord;

            // Calculate record numbers for each summary record and data start addresses
            // Using contiguous addressing like CSPICE (no padding between arrays)
            final int[] summaryRecordNums = new int[numBatches];
            final int[][] arrayAddresses = new int[numArrays][2]; // [initialAddress, finalAddress]

            int currentRecordNum = fileRecord.getFirstSummaryRecNum();

            for (int batch = 0; batch < numBatches; batch++) {
                summaryRecordNums[batch] = currentRecordNum;
                currentRecordNum++; // summary record
                currentRecordNum++; // name record

                // Data starts at this record (address is 1-based, in double words)
                int nextFreeAddress = (currentRecordNum - 1) * DAFConstants.DOUBLES_PER_RECORD + 1;

                // Calculate contiguous addresses for each array in this batch
                final int batchStart = batch * maxSummariesPerRecord;
                final int batchEnd = FastMath.min(batchStart + maxSummariesPerRecord, numArrays);
                int batchTotalElements = 0;

                for (int i = batchStart; i < batchEnd; i++) {
                    final int numElements = arrays.get(i).getArrayElements().size();
                    arrayAddresses[i][0] = nextFreeAddress; // initial address
                    arrayAddresses[i][1] = nextFreeAddress + numElements - 1; // final address
                    nextFreeAddress += numElements;
                    batchTotalElements += numElements;
                }

                // Calculate how many complete records the data of this batch takes
                final int batchDataRecords = (batchTotalElements + DAFConstants.DOUBLES_PER_RECORD - 1) / DAFConstants.DOUBLES_PER_RECORD;
                currentRecordNum += batchDataRecords;
            }

            // First free address is immediately after the last used address
            final int lastUsedAddress = arrayAddresses[numArrays - 1][1];
            final int firstFreeAddress = lastUsedAddress + 1;

            // Create updated file record with correct lastSummaryRecNum and firstFreeAddress
            final int lastSummaryRecNum = summaryRecordNums[numBatches - 1];
            final DAFFileRecord updatedFileRecord = new DAFFileRecord(
                fileRecord.getFileType(),
                numDoubles,
                numInts,
                numCharsName,
                singleSummarySizeDoubles,
                fileRecord.getDescription(),
                fileRecord.getFirstSummaryRecNum(),
                lastSummaryRecNum,
                firstFreeAddress,
                fileRecord.getEndianString(),
                fileRecord.getFtpString()
            );

            // Write file record with updated values
            writeFileRecord(dos, updatedFileRecord);

            // Write comments using reserved space
            writeComments(dos, daf.getComments(), updatedFileRecord);

            // Write each batch (summary record -> name record -> contiguous data -> padding)
            for (int batch = 0; batch < numBatches; batch++) {
                final int batchStart = batch * maxSummariesPerRecord;
                final int batchEnd = FastMath.min(batchStart + maxSummariesPerRecord, numArrays);

                // Build summaries with pre-calculated addresses
                final List<DAFArraySummary> updatedSummaries = new ArrayList<>();
                final List<String> names = new ArrayList<>();

                for (int i = batchStart; i < batchEnd; i++) {
                    final DAFArray array = arrays.get(i);
                    final int initialAddress = arrayAddresses[i][0];
                    final int finalAddress = arrayAddresses[i][1];

                    // Create updated summary with correct addresses
                    final List<Integer> updatedInts = new ArrayList<>(array.getArraySummary().getSummaryInts());
                    // Last two ints are initial and final addresses
                    updatedInts.set(updatedInts.size() - 2, initialAddress);
                    updatedInts.set(updatedInts.size() - 1, finalAddress);

                    final DAFArraySummary updatedSummary = new DAFArraySummary(
                        array.getArraySummary().getSummaryDoubles(),
                        updatedInts,
                        (initialAddress - 1) * DAFConstants.DOUBLE_SIZE_BYTES + 1,
                        finalAddress * DAFConstants.DOUBLE_SIZE_BYTES + 1
                    );
                    updatedSummaries.add(updatedSummary);
                    names.add(array.getArrayName());
                }

                // Calculate NEXT/PREV control values
                final int prevSummaryRec = (batch == 0) ? 0 : summaryRecordNums[batch - 1];
                final int nextSummaryRec = (batch == numBatches - 1) ? 0 : summaryRecordNums[batch + 1];

                // Write summary record
                writeSummaryRecord(dos, updatedSummaries, numDoubles, numInts, nextSummaryRec, prevSummaryRec);

                // Write name record
                writeNameRecord(dos, names, numCharsName);

                // Write data contiguously for all arrays in this batch
                int batchTotalElements = 0;
                for (int i = batchStart; i < batchEnd; i++) {
                    final List<Double> elements = arrays.get(i).getArrayElements();
                    for (final Double element : elements) {
                        writeDouble(dos, element);
                    }
                    batchTotalElements += elements.size();
                }

                // Pad to complete the final data record of this batch to 128 doubles (1024 bytes)
                final int doublesInPartialRecord = batchTotalElements % DAFConstants.DOUBLES_PER_RECORD;
                if (doublesInPartialRecord > 0) {
                    final int bytesToPad = (DAFConstants.DOUBLES_PER_RECORD - doublesInPartialRecord) * DAFConstants.DOUBLE_SIZE_BYTES;
                    for (int i = 0; i < bytesToPad; i++) {
                        dos.writeByte(DAFConstants.NULL_ASCII);
                    }
                }
            }
        } finally {
            // close the stream
            dos.close();
        }
    }

    /**
     * Write a padded ASCII string (truncated if string is longer than given length).
     * @param dos data output stream to write to
     * @param string string to write
     * @param length length to write (padded with spaces or truncated as needed)
     * @throws IOException if error occurs
     */
    private void writePaddedAscii(final DataOutputStream dos, final String string, final int length) throws IOException {
        final byte[] paddedBytes = new byte[length];
        // fill with spaces
        for (int i = 0; i < length; i++) {
            paddedBytes[i] = DAFConstants.SPACE_ASCII; // pad with space
        }
        if (string != null) {
            final byte[] bytes = string.getBytes(US_ASCII);
            final int toCopy = FastMath.min(bytes.length, length);
            System.arraycopy(bytes, 0, paddedBytes, 0, toCopy);
        }
        dos.write(paddedBytes);
    }

    /**
     * Write a 4-byte integer using the writer's endianness.
     * @param dos data output stream to write to
     * @param x integer to write
     * @throws IOException if I/O error occurs
     */
    private void writeInt(final DataOutputStream dos, final int x) throws IOException {
        bb.clear();
        bb.putInt(x);
        dos.write(bb.array(), 0, DAFConstants.INT_SIZE_BYTES);
    }

    /**
     * Write an 8-byte double using the writer's endianness.
     * @param dos data output stream to write to
     * @param x double to write
     * @throws IOException if I/O error occurs
     */
    private void writeDouble(final DataOutputStream dos, final double x) throws IOException {
        bb.clear();
        bb.putDouble(x);
        dos.write(bb.array(), 0, DAFConstants.DOUBLE_SIZE_BYTES);
    }
}
