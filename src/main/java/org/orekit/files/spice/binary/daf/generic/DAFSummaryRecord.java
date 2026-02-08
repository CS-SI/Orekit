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

/**
 * Represents a summary record of a {@link DAF} file, containing the summaries
 * for multiple arrays.
 *
 * @author Rafael Ayala
 * @since 14.0
 */
public class DAFSummaryRecord {

    /**
     * Next summary record number in the DAF file (0 if this is the final summary record).
     */
    private final int nextSummaryRecNum;

    /**
     * Previous summary record number in the DAF file (0 if this is the first summary record).
     */
    private final int previousSummaryRecNum;

    /**
     * Number of array summaries in this summary record.
     */
    private final int numberSummariesThisRec;

    /**
     * Raw data for all the summaries in this summary record.
     */
    private final byte[] summariesRaw;

    /**
     * Basic constructor.
     *
     * @param nextSummaryRecNum record number of the next summary record in the
     * file (0 if this is the final summary record)
     * @param previousSummaryRecNum record number of the previous summary record
     * in the file (0 if this is the first summary record)
     * @param numberSummariesThisRec number of summaries in this summary record
     * @param summariesRaw raw data for all the summaries in this summary record
     */
    public DAFSummaryRecord(final int nextSummaryRecNum, final int previousSummaryRecNum, final int numberSummariesThisRec, final byte[] summariesRaw) {
        this.nextSummaryRecNum = nextSummaryRecNum;
        this.previousSummaryRecNum = previousSummaryRecNum;
        this.numberSummariesThisRec = numberSummariesThisRec;
        this.summariesRaw = summariesRaw.clone();
    }

    /**
     * Get the record number of the next summary record in the DAF file.
     *
     * @return record number of the next summary record in the DAF file
     */
    public int getNextSummaryRecNum() {
        return nextSummaryRecNum;
    }

    /**
     * Get the record number of the previous summary record in the DAF file.
     *
     * @return record number of the previous summary record in the DAF file
     */
    public int getPreviousSummaryRecNum() {
        return previousSummaryRecNum;
    }

    /**
     * Get the number of summaries in this summary record.
     *
     * @return number of summaries in this summary record
     */
    public int getNumberSummariesThisRec() {
        return numberSummariesThisRec;
    }

    /**
     * Get the raw data for all the summaries in this summary record.
     *
     * @return raw data (byte array) for all the summaries in this summary
     * record
     */
    public byte[] getSummariesRaw() {
        return summariesRaw.clone();
    }
}
