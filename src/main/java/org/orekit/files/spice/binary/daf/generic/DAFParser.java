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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Parser for generic DAF files.
 *
 * @author Rafael Ayala
 * @since 14.0
 */
public class DAFParser {

    /**
     * Parse the given data source into a {@link DAF}.
     *
     * @param source data source for the DAF file
     * @return parsed {@link DAF} containing metadata, comments, and arrays
     */
    public DAF parse(final DataSource source) {
        try (InputStream is = source.getOpener().openStreamOnce()) {

            // Manually read entire file into memory
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final byte[] buffer = new byte[DAFConstants.BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            final byte[] rawData = baos.toByteArray();

            // Parse file record. DAF files contain a single file record (always the first one),
            // comprising metadata. Each DAF file record is 1024 bytes long, including the file record.
            // Following NAIF's specifications, the file might have been written using BIG or LTL endianness.
            // Since the endian string is ASCII at a fixed offset, it is readable regardless of byte order.
            // We first parse with default (little) endianness, check the endian string, and re-parse
            // with swapped endianness if the file is not little-endian.
            DAFFileRecord fileRecord = parseFileRecord(rawData, false, source);
            if (needByteSwap(fileRecord)) {
                // Re-parse using swapped endianness
                fileRecord = parseFileRecord(rawData, true, source);
            }

            final String endianString = fileRecord.getEndianString();
            final int singleSummarySizeDoubles = fileRecord.getSingleSummarySizeDoubles();
            final int numCharsName = fileRecord.getNumCharsName();
            final int numDoublesSummary = fileRecord.getNumDoublesSummary();
            final int numIntsSummary = fileRecord.getNumIntsSummary();

            // Parse comments if any (optional records from number 2 to firstSummaryRecNum - 1)
            final String comments = parseComments(rawData, fileRecord);

            // Initialize the list of DAFArray objects
            final List<DAFArray> arrays = new ArrayList<>();

            // The arrays in a DAF file contain 3 parts: summary, name and elements
            // The summary contains the metadata of the array, the name contains the name of the array,
            // and the elements contain the actual data of the array. Note that the metadata provided by each summary
            // for specific arrays should not be confused with the file-wide metadata provided in the initial file record.
            // These components are stored in separate records: summary records, name records and element records
            // A summary record contains the summaries for 1 or more arrays.
            // A summary record is always followed by a name record, which contains the names of the arrays in the summary record
            // A name record is always followed by 1 or more element records, which contain the actual data of the arrays
            // The number of element records that comes after a (summary record + name record) block is variable. It
            // will be as many as needed to store all the data of the arrays in the summary record. Each element record
            // contains up to 128 doubles.
            // The logic for parsing the arrays is as follows:
            // - Obtain the record number for the target summary record
            // - Parse the summary record. From this, we will obtain multiple summaries, and this allows us to know how many arrays
            //   we have to process from the immediately following name record and element record(s). In addition to array metadata,
            //   summary records also provide critical information for parsing the DAF file:
            //     + The record number for the next summary record. If this is 0 for a given summary, this means that record is the
            //       last summary record, and therefore the file ends after the following name and element records
            //     + The number of characters that compose each array name
            //     + The initial and final addresses of the array elements corresponding to each summary, in number of doubles
            //       (i.e., these will not be byte addresses, instead byte/8 addresses)
            // - Parse the following name record. We know from the summary record how many names we have in the name record and
            //   how many characters each name has.
            // - Parse the following element record(s). We know from the summary record the initial and final addresses of the
            //   elements for each array.
            // We repeat this until we reach the last summary record.
            // The record number for the first summary record is given in the file record, which is always the first record of a DAF file
            // If the first summary record is not the second one, all the records between the first record (the file record) and the first
            // summary record are comment records (these are optional).
            int currentSummaryRecNum = fileRecord.getFirstSummaryRecNum();

            while (currentSummaryRecNum != 0) {
                final DAFSummaryRecord summaryRecord = parseSummaryRecord(rawData, currentSummaryRecNum, singleSummarySizeDoubles, endianString);
                final DAFNameRecord nameRecord = parseNameRecord(rawData, currentSummaryRecNum + 1, numCharsName, summaryRecord.getNumberSummariesThisRec(), endianString);
                // first lets split the raw with all the summaries into individual summaries
                final int numSummaries = summaryRecord.getNumberSummariesThisRec();
                final List<byte[]> rawSummariesCurrentRecord = splitRawSummaries(summaryRecord.getSummariesRaw(),
                                                                                 numSummaries, singleSummarySizeDoubles);
                // we do the same for the raw with all the names. we can directly convert them to strings
                final List<String> names = splitRawNames(nameRecord.getNamesRaw(), nameRecord.getNumberNamesThisRec(),
                                                         numCharsName);
                // now we iterate over the summaries and parse them, getting the corresponding name from the names list
                for (int i = 0; i < numSummaries; i++) {
                    arrays.add(parseArray(rawData, rawSummariesCurrentRecord.get(i), names.get(i),
                                          endianString, numDoublesSummary, numIntsSummary));
                }
                currentSummaryRecNum = summaryRecord.getNextSummaryRecNum();
            }
            // Return the fully assembled DAFFile
            return new DAF(fileRecord, comments, arrays);
        } catch (IOException ioe) {
            throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
        }
    }

    /**
     * Parse the first 1024-byte record to create a {@link DAFFileRecord}.
     *
     * @param rawData entire DAF file raw data
     * @param swap if true, interpret multi-byte fields as big-endian
     * (byte-swapped)
     * @param source data source for the DAF file
     * @return {@link DAFFileRecord} containing file metadata
     */
    private DAFFileRecord parseFileRecord(final byte[] rawData, final boolean swap, final DataSource source) {
        final ByteBuffer buffer = ByteBuffer.wrap(rawData, 0, DAFConstants.RECORD_LENGTH_BYTES);
        buffer.order(swap ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        // 1) fileType (e.g. "DAF/SPK") as plain text (8 chars)
        final byte[] typeBytes = new byte[DAFConstants.TYPE_STRING_LENGTH];
        buffer.get(typeBytes);
        final String fileType = new String(typeBytes, StandardCharsets.US_ASCII).trim();
        // check that it is of the format "DAF/" followed by no more than 4 characters
        if (!DAFConstants.FILE_TYPE_PATTERN.matcher(fileType).matches()) {
            throw new OrekitException(OrekitMessages.INVALID_DAF_FILETYPE_STRING, fileType);
        }

        // 2) numDoublesSummary
        final int numDoublesSummary = buffer.getInt();

        // 3) numIntsSummary
        final int numIntsSummary = buffer.getInt();

        // 4) numCharsName
        // The structure is that, if we leave out an initial 24-byte offset for 3 control ints (stored as doubles)
        // at the beginning of each summary record, then each summary in the summary record is byte-aligned with each
        // name in the name record. For that, each name must take the same number of bytes as the size in bytes of a summary.
        final int numCharsName = DAFConstants.DOUBLE_SIZE_BYTES * (numDoublesSummary + (numIntsSummary + 1) / 2);

        // 5) singleSummarySizeDoubles: numDoublesSummary + (numIntsSummary + 1) / 2
        final int singleSummarySizeDoubles = numDoublesSummary + (numIntsSummary + 1) / 2;

        // 6) description (60 bytes)
        final byte[] descBytes = new byte[DAFConstants.DESCRIPTION_LENGTH];
        buffer.get(descBytes);
        final String description = new String(descBytes, StandardCharsets.US_ASCII).trim();

        // 7) firstSummaryRecNum
        final int firstSummaryRecNum = buffer.getInt();

        // 8) lastSummaryRecNum
        final int lastSummaryRecNum = buffer.getInt();

        // 9) firstFreeAddress as 4 bytes (int)
        final int firstFreeAddress = buffer.getInt();

        // 10) endianString (8 chars)
        final byte[] endianBytes = new byte[DAFConstants.ENDIAN_STRING_LENGTH];
        buffer.get(endianBytes);
        final String endianString = new String(endianBytes, StandardCharsets.US_ASCII).trim();

        // 11) ftpString
        // for this one, we need to read 28 bytes (chars) from address 699th (DAFConstants.FTP_STRING_OFFSET)
        buffer.position(DAFConstants.FTP_STRING_OFFSET);
        final byte[] ftpBytes = new byte[DAFConstants.FTP_STRING_LENGTH];
        buffer.get(ftpBytes);
        // decode as eight-bit encoding (characters beyond standard ASCII range, so have to use ISO_8859_1)
        final String ftpString = new String(ftpBytes, StandardCharsets.ISO_8859_1);
        // then we check if it matches DAFConstants.FTPSTR
        if (!DAFConstants.FTPSTR.equals(ftpString)) {
            throw new OrekitException(OrekitMessages.INVALID_DAF_FTPSTR,
                    source.getName());
        }
        return new DAFFileRecord(fileType, numDoublesSummary, numIntsSummary, numCharsName,
                singleSummarySizeDoubles, description, firstSummaryRecNum,
                lastSummaryRecNum, firstFreeAddress, endianString, ftpString);
    }

    /**
     * Determine if we should swap endianness for further parsing.
     *
     * @param fileRecord file record
     * @return true if endianness should be swapped
     */
    private boolean needByteSwap(final DAFFileRecord fileRecord) {
        // Since we always start by parsing with little-endian byte order,
        // any file whose endian string is not "LTL-IEEE" needs re-parsing with swapped endianness
        return !DAFConstants.LITTLE_ENDIAN_STRING.equalsIgnoreCase(fileRecord.getEndianString());
    }

    /**
     * Parse comments from comment records (record #2 up to firstSummaryRecNum -
     * 1, if firstSummaryRecNum > 2).
     *
     * @param rawData entire file raw data
     * @param fileRecord global metadata
     * @return parsed comments
     */
    private String parseComments(final byte[] rawData, final DAFFileRecord fileRecord) {
        // the general logic is:
        // a maximum of 1000 characters can be found per comment record
        // if we find a \0 byte, this means newLine
        // if we find a \4 (EOT) byte, this means end of comment record
        // different comment records are concatenated without any separator
        final StringBuilder commentsBuilder = new StringBuilder();
        final int startRec = 2;
        final int endRec = fileRecord.getFirstSummaryRecNum() - 1;

        for (int recNum = startRec; recNum <= endRec; recNum++) {
            final int offset = (recNum - 1) * DAFConstants.RECORD_LENGTH_BYTES;
            final int maxBytesToRead = FastMath.min(DAFConstants.COMMENT_RECORD_MAX_CHARS, DAFConstants.RECORD_LENGTH_BYTES);
            final int limit = FastMath.min(offset + maxBytesToRead, rawData.length);

            if (offset >= rawData.length) {
                throw new OrekitException(OrekitMessages.INCOMPLETE_DAF_COMMENT_RECORD);
            }

            for (int i = offset; i < limit; i++) {
                final byte b = rawData[i];

                if (b == DAFConstants.NULL_ASCII) {
                    commentsBuilder.append('\n');
                } else if (b == DAFConstants.EOT_ASCII) {
                    break;
                } else {
                    commentsBuilder.append((char) b);
                }

            }
        }
        return commentsBuilder.toString();
    }

    /**
     * Parse a summary record from the entire DAF file raw data and a specified
     * record number.
     *
     * @param rawData entire DAF file raw data
     * @param recordNumber record number for the target summary record to parse
     * @param singleSummarySizeDoubles size of a single summary record in
     * doubles
     * @param endianness string specifying endianness of the file (either
     * BIG-IEEE or LTL-IEEE)
     * @return {@link DAFSummaryRecord} object
     */
    private DAFSummaryRecord parseSummaryRecord(final byte[] rawData, final int recordNumber, final int singleSummarySizeDoubles, final String endianness) {
        if (!DAFConstants.BIG_ENDIAN_STRING.equalsIgnoreCase(endianness) && !DAFConstants.LITTLE_ENDIAN_STRING.equalsIgnoreCase(endianness)) {
            throw new OrekitException(OrekitMessages.INVALID_DAF_ENDIANNESS, endianness);
        }
        final int offset = (recordNumber - 1) * DAFConstants.RECORD_LENGTH_BYTES;
        final int limit = offset + DAFConstants.RECORD_LENGTH_BYTES;
        if (limit > rawData.length) {
            throw new OrekitException(OrekitMessages.INCOMPLETE_DAF_SUMMARY_RECORD);
        }

        final ByteBuffer buffer = ByteBuffer.wrap(rawData, offset, DAFConstants.RECORD_LENGTH_BYTES);
        buffer.order(DAFConstants.BIG_ENDIAN_STRING.equalsIgnoreCase(endianness) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        // control items are stored in doubles but are actually ints
        final int nextSummaryRecNum = (int) buffer.getDouble();
        final int previousSummaryRecNum = (int) buffer.getDouble();
        final int numberSummariesThisRec = (int) buffer.getDouble();

        // and then we get all the raw data for the summary
        final byte[] summariesRaw = new byte[singleSummarySizeDoubles * numberSummariesThisRec * DAFConstants.DOUBLE_SIZE_BYTES];
        buffer.get(summariesRaw);

        return new DAFSummaryRecord(nextSummaryRecNum, previousSummaryRecNum, numberSummariesThisRec, summariesRaw);
    }

    /**
     * Parse a name record from the entire DAF file raw data and a specified
     * record number.
     *
     * @param rawData entire DAF file raw data
     * @param recordNumber record number for the target name record to parse
     * @param numCharsName number of characters in each array name
     * @param numNames number of array names in the names record
     * @param endianness endianness of the file
     * @return {@link DAFNameRecord} object
     */
    private DAFNameRecord parseNameRecord(final byte[] rawData, final int recordNumber, final int numCharsName, final int numNames, final String endianness) {
        final int offset = (recordNumber - 1) * DAFConstants.RECORD_LENGTH_BYTES;
        final int limit = offset + DAFConstants.RECORD_LENGTH_BYTES;
        if (limit > rawData.length) {
            throw new OrekitException(OrekitMessages.INCOMPLETE_DAF_NAME_RECORD);
        }

        final ByteBuffer buffer = ByteBuffer.wrap(rawData, offset, DAFConstants.RECORD_LENGTH_BYTES);
        buffer.order(DAFConstants.BIG_ENDIAN_STRING.equalsIgnoreCase(endianness) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        // in this case, all we need to do is to get numCharsName x numNames bytes (chars) from rawData from the current position of buffer
        final byte[] namesRaw = new byte[numCharsName * numNames];
        buffer.get(namesRaw);

        return new DAFNameRecord(numNames, namesRaw);
    }

    /**
     * Split the concatenated raw summaries into individual byte arrays.
     *
     * @param summariesRaw concatenated raw bytes for all summaries in the record
     * @param numSummaries number of summaries in the record
     * @param singleSummarySizeDoubles size of a single summary in doubles
     * @return list of individual summary byte arrays
     */
    private List<byte[]> splitRawSummaries(final byte[] summariesRaw, final int numSummaries,
                                           final int singleSummarySizeDoubles) {
        final List<byte[]> rawSummariesCurrentRecord = new ArrayList<>();
        for (int i = 0; i < numSummaries; i++) {
            final byte[] summaryRaw = new byte[singleSummarySizeDoubles * DAFConstants.DOUBLE_SIZE_BYTES];
            System.arraycopy(summariesRaw, i * singleSummarySizeDoubles * DAFConstants.DOUBLE_SIZE_BYTES, summaryRaw, 0, singleSummarySizeDoubles * DAFConstants.DOUBLE_SIZE_BYTES);
            rawSummariesCurrentRecord.add(summaryRaw);
        }
        return rawSummariesCurrentRecord;
    }

    /**
     * Split the concatenated raw names into individual trimmed strings.
     *
     * @param namesRaw concatenated raw bytes for all names in the record
     * @param numNames number of names in the record
     * @param numCharsName number of characters per name
     * @return list of individual name strings
     */
    private List<String> splitRawNames(final byte[] namesRaw, final int numNames, final int numCharsName) {
        final List<String> names = new ArrayList<>();
        for (int i = 0; i < numNames; i++) {
            final byte[] nameRaw = new byte[numCharsName];
            System.arraycopy(namesRaw, i * numCharsName, nameRaw, 0, numCharsName);
            names.add(new String(nameRaw, StandardCharsets.US_ASCII).trim());
        }
        return names;
    }

    /**
     * Parse a single array from its raw summary bytes, name, and the file raw data.
     *
     * @param rawData entire DAF file raw data
     * @param summaryRaw raw bytes for this array's summary
     * @param name array name
     * @param endianString endianness string (BIG-IEEE or LTL-IEEE)
     * @param numDoublesSummary number of doubles per summary (ND)
     * @param numIntsSummary number of integers per summary (NI)
     * @return parsed {@link DAFArray}
     */
    private DAFArray parseArray(final byte[] rawData, final byte[] summaryRaw, final String name,
                                final String endianString, final int numDoublesSummary, final int numIntsSummary) {
        final DAFArraySummary summary = parseArraySummary(summaryRaw, endianString, numDoublesSummary, numIntsSummary);
        // now we just need to get the array elements using the initial and final addresses. this will be a list of doubles,
        final List<Double> arrayElements = new ArrayList<>();
        final int initialArrayAddressBytes = summary.getInitialArrayAddress();
        final int finalArrayAddressBytes = summary.getFinalArrayAddress();
        final ByteBuffer elementsBuffer = ByteBuffer.wrap(rawData, initialArrayAddressBytes - 1, finalArrayAddressBytes - initialArrayAddressBytes);
        elementsBuffer.order(DAFConstants.BIG_ENDIAN_STRING.equalsIgnoreCase(endianString) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        while (elementsBuffer.remaining() >= DAFConstants.DOUBLE_SIZE_BYTES) {
            arrayElements.add(elementsBuffer.getDouble());
        }
        // with these, we can assemble the array (summary + name + elements)
        return new DAFArray(name, summary, arrayElements);
    }

    /**
     * Parse an array summary from its raw bytes.
     *
     * @param summaryRaw raw bytes for this array's summary
     * @param endianString endianness string (BIG-IEEE or LTL-IEEE)
     * @param numDoublesSummary number of doubles per summary (ND)
     * @param numIntsSummary number of integers per summary (NI)
     * @return parsed {@link DAFArraySummary}
     */
    private DAFArraySummary parseArraySummary(final byte[] summaryRaw, final String endianString,
                                              final int numDoublesSummary, final int numIntsSummary) {
        final List<Double> summaryDoubles = new ArrayList<>();
        final ByteBuffer summaryBuffer = ByteBuffer.wrap(summaryRaw);
        summaryBuffer.order(DAFConstants.BIG_ENDIAN_STRING.equalsIgnoreCase(endianString) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        // we need to get a number of doubles equal to numDoublesSummary
        for (int j = 0; j < numDoublesSummary; j++) {
            summaryDoubles.add(summaryBuffer.getDouble());
        }
        // and then a number of ints equal to numIntsSummary
        final List<Integer> summaryInts = new ArrayList<>();
        for (int j = 0; j < numIntsSummary; j++) {
            summaryInts.add(summaryBuffer.getInt());
        }
        // the final two integers in the summaryInts are the initial and final double word addresses of the array elements
        // note we convert them to byte addresses here
        final int initialArrayAddressBytes = (summaryInts.get(summaryInts.size() - 2) - 1) * DAFConstants.DOUBLE_SIZE_BYTES + 1;
        final int finalArrayAddressBytes = summaryInts.get(summaryInts.size() - 1) * DAFConstants.DOUBLE_SIZE_BYTES + 1;
        // with these, we can initialize a DAFArraySummary object
        return new DAFArraySummary(summaryDoubles, summaryInts, initialArrayAddressBytes, finalArrayAddressBytes);
    }
}
