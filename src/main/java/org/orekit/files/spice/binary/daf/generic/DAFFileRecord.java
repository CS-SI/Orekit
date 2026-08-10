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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Represents the first, global file record of a {@link DAF} file, containing
 * generic metadata.
 *
 * @author Rafael Ayala
 * @since 14.0
 */
public class DAFFileRecord {

    /**
     * String indicating the type of data stored in the DAF file. This should be of the format "DAF/XXXX",
     * i.e., "DAF/" followed by 4 or less characters. For example, for SPK files, it should be "DAF/SPK"
     */
    private final String fileType;

    /**
     * Number of doubles stored in each array summary.
     */
    private final int numDoublesSummary;

    /**
     * Number of integers stored in each array summary.
     */
    private final int numIntsSummary;

    /**
     * Number of characters used to store each array name.
     */
    private final int numCharsName;

    /**
     * Size of a single array summary in doubles.
     */
    private final int singleSummarySizeDoubles;

    /**
     * Internal name of the DAF file.
     */
    private final String description;

    /**
     * Record number of the first summary record in the file.
     */
    private final int firstSummaryRecNum;

    /**
     * Record number of the last summary record in the file.
     */
    private final int lastSummaryRecNum;

    /**
     * Byte address of the first free byte in the file.
     */
    private final long firstFreeAddress;

    /**
     * Endianness of the file. This should be either "LTL-IEEE" or "BIG-IEEE".
     */
    private final String endianString;

    /**
     * Integrity-check string. This should match "FTPSTR:\r:\n:\r\n:\r:\u0081:\u0010\u00ce:ENDFTP".
     */
    private final String ftpString;

    /**
     * Constructor initializing all metadata fields.
     *
     * @param fileType string of the format DAF/XXXX, i.e., "DAF/" followed by 4
     * or less characters
     * @param numDoublesSummary number of doubles in each array summary
     * @param numIntsSummary number of integers in each array summary
     * @param numCharsName number of characters used to store each array name
     * @param singleSummarySizeDoubles size of a single array summary in doubles
     * @param description internal name or description of the DAF file
     * @param firstSummaryRecNum index of the first summary record in the DAF
     * file
     * @param lastSummaryRecNum index of the last summary record in the DAF file
     * @param firstFreeAddress the first free byte address in the DAF file
     * @param endianString the file endianness string (either "LTL-IEEE" or
     * "BIG-IEEE")
     * @param ftpString integrity-check string (should match
     * "FTPSTR:\r:\n:\r\n:\r:\u0081:\u0010\u00ce:ENDFTP";)
     */
    public DAFFileRecord(final String fileType,
            final int numDoublesSummary,
            final int numIntsSummary,
            final int numCharsName,
            final int singleSummarySizeDoubles,
            final String description,
            final int firstSummaryRecNum,
            final int lastSummaryRecNum,
            final long firstFreeAddress,
            final String endianString,
            final String ftpString) {
        // Check FTP string integrity
        if (!DAFConstants.FTPSTR.equals(ftpString)) {
            throw new OrekitException(OrekitMessages.INVALID_DAF_FTPSTR, description);
        }
        this.fileType = fileType;
        this.numDoublesSummary = numDoublesSummary;
        this.numIntsSummary = numIntsSummary;
        this.numCharsName = numCharsName;
        this.singleSummarySizeDoubles = singleSummarySizeDoubles;
        this.description = description;
        this.firstSummaryRecNum = firstSummaryRecNum;
        this.lastSummaryRecNum = lastSummaryRecNum;
        this.firstFreeAddress = firstFreeAddress;
        this.endianString = endianString;
        this.ftpString = ftpString;
    }

    /**
     * Get the file type string.
     *
     * @return the file type string
     */
    public String getFileType() {
        return fileType;
    }

    /**
     * Get the number of doubles stored in each array summary.
     *
     * @return the number of doubles stored in each array summary
     */
    public int getNumDoublesSummary() {
        return numDoublesSummary;
    }

    /**
     * Get the number of integers stored in each array summary.
     *
     * @return the number of integers stored in each array summary
     */
    public int getNumIntsSummary() {
        return numIntsSummary;
    }

    /**
     * Get the number of characters used to store each array name.
     *
     * @return the number of characters used to store each array name
     */
    public int getNumCharsName() {
        return numCharsName;
    }

    /**
     * Get the size of a single array summary in doubles.
     *
     * @return the size of a single array summary in doubles
     */
    public int getSingleSummarySizeDoubles() {
        return singleSummarySizeDoubles;
    }

    /**
     * Get the internal name of the DAF file.
     *
     * @return the internal name of the DAF file
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the record number of the first summary record in the DAF file.
     *
     * @return the record number of the first summary record in the DAF file
     */
    public int getFirstSummaryRecNum() {
        return firstSummaryRecNum;
    }

    /**
     * Get the record number of the last summary record in the DAF file.
     *
     * @return the record number of the last summary record in the DAF file
     */
    public int getLastSummaryRecNum() {
        return lastSummaryRecNum;
    }

    /**
     * Get the first free byte address in the DAF file.
     *
     * @return the first free byte address in the DAF file
     */
    public long getFirstFreeAddress() {
        return firstFreeAddress;
    }

    /**
     * Get the endianness string of the file.
     *
     * @return the endianness string of the file
     */
    public String getEndianString() {
        return endianString;
    }

    /**
     * Get the integrity-check (FTP) string.
     *
     * @return the integrity-check (FTP) string
     */
    public String getFtpString() {
        return ftpString;
    }
}
