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
package org.orekit.time;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** This class represents a CCSDS unsegmented time code.
 * @param <T> type of the date
 * @author Luc Maisonobe
 * @since 12.1
 * @see AbsoluteDate
 * @see FieldAbsoluteDate
 */
class CcsdsUnsegmentedTimeCode<T> extends AbstractCcsdsTimeCode {

    /** Epoch part. */
    private final T epoch;

    /** Seconds part. */
    private final long seconds;

    /** Sub-second part. */
    private final double subSecond;

    /** Create an instance CCSDS Day Unegmented Time Code (CDS).
     * <p>
     * CCSDS Unsegmented Time Code is defined in the blue book: CCSDS Time Code Format
     * (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * <p>
     * If the date to be parsed is formatted using version 3 of the standard (CCSDS
     * 301.0-B-3 published in 2002) or if the extension of the preamble field introduced
     * in version 4 of the standard is not used, then the {@code preambleField2} parameter
     * can be set to 0.
     * </p>
     * @param preambleField1     first byte of the field specifying the format, often not
     *                           transmitted in data interfaces, as it is constant for a
     *                           given data interface
     * @param preambleField2     second byte of the field specifying the format (added in
     *                           revision 4 of the CCSDS standard in 2010), often not
     *                           transmitted in data interfaces, as it is constant for a
     *                           given data interface (value ignored if presence not
     *                           signaled in {@code preambleField1})
     * @param timeField          byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field specifies
     *                           the {@link DateComponents#CCSDS_EPOCH CCSDS reference epoch} is used
     *                           (and hence may be null in this case)
     * @param ccsdsEpoch         reference epoch, ignored if the preamble field specifies
     *                           the agency epoch is used.
     */
    CcsdsUnsegmentedTimeCode(final byte preambleField1,
                             final byte preambleField2,
                             final byte[] timeField,
                             final T agencyDefinedEpoch,
                             final T ccsdsEpoch) {

        // time code identification and reference epoch
        switch (preambleField1 & 0x70) {
            case 0x10:
                // the reference epoch is CCSDS epoch 1958-01-01T00:00:00 TAI
                epoch = ccsdsEpoch;
                break;
            case 0x20:
                // the reference epoch is agency defined
                if (agencyDefinedEpoch == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_DATE_MISSING_AGENCY_EPOCH);
                }
                epoch = agencyDefinedEpoch;
                break;
            default :
                throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                          formatByte(preambleField1));
        }

        // time field lengths
        int coarseTimeLength = 1 + ((preambleField1 & 0x0C) >>> 2);
        int fineTimeLength   = preambleField1 & 0x03;

        if ((preambleField1 & 0x80) != 0x0) {
            // there is an additional octet in preamble field
            coarseTimeLength += (preambleField2 & 0x60) >>> 5;
            fineTimeLength   += (preambleField2 & 0x1C) >>> 2;
        }

        if (timeField.length != coarseTimeLength + fineTimeLength) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                                      timeField.length, coarseTimeLength + fineTimeLength);
        }

        long s = 0L;
        for (int i = 0; i < coarseTimeLength; ++i) {
            s = s * 256 + toUnsigned(timeField[i]);
        }
        seconds = s;

        double sub = 0;
        for (int i = timeField.length - 1; i >= coarseTimeLength; --i) {
            sub = (sub + toUnsigned(timeField[i])) / 256;
        }
        subSecond = sub;

    }

    /** Get the epoch part.
     * @return epoch part
     */
    public T getEpoch() {
        return epoch;
    }

    /** Get the seconds part.
     * @return seconds part
     */
    public long getSeconds() {
        return seconds;
    }

    /** Get the sub-second part.
     * @return sub-second part
     */
    public double getSubSecond() {
        return subSecond;
    }

}
