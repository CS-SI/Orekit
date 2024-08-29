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

/** This class represents a CCSDS segmented time code.
 * @author Luc Maisonobe
 * @since 12.1
 * @see AbsoluteDate
 * @see FieldAbsoluteDate
 */
class CcsdsSegmentedTimeCode extends AbstractCcsdsTimeCode {

    /** Date part. */
    private final DateComponents date;

    /** Time part. */
    private final TimeComponents time;

    /** Create an instance CCSDS Day Segmented Time Code (CDS).
     * <p>
     * CCSDS Day Segmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * @param preambleField field specifying the format, often not transmitted in
     * data interfaces, as it is constant for a given data interface
     * @param timeField byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field
     * specifies the {@link DateComponents#CCSDS_EPOCH CCSDS reference epoch} is used
     * (and hence may be null in this case)
     */
    CcsdsSegmentedTimeCode(final byte preambleField, final byte[] timeField,
                           final DateComponents agencyDefinedEpoch) {

        // time code identification
        if ((preambleField & 0xF0) != 0x40) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }

        // reference epoch
        final DateComponents epoch;
        if ((preambleField & 0x08) == 0x00) {
            // the reference epoch is CCSDS epoch 1958-01-01T00:00:00 TAI
            epoch = DateComponents.CCSDS_EPOCH;
        } else {
            // the reference epoch is agency defined
            if (agencyDefinedEpoch == null) {
                throw new OrekitException(OrekitMessages.CCSDS_DATE_MISSING_AGENCY_EPOCH);
            }
            epoch = agencyDefinedEpoch;
        }

        // time field lengths
        final int daySegmentLength = ((preambleField & 0x04) == 0x0) ? 2 : 3;
        final int subMillisecondLength = (preambleField & 0x03) << 1;
        if (subMillisecondLength == 6) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }
        if (timeField.length != daySegmentLength + 4 + subMillisecondLength) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                                      timeField.length, daySegmentLength + 4 + subMillisecondLength);
        }


        int i   = 0;
        int day = 0;
        while (i < daySegmentLength) {
            day = day * 256 + toUnsigned(timeField[i++]);
        }

        long milliInDay = 0L;
        while (i < daySegmentLength + 4) {
            milliInDay = milliInDay * 256 + toUnsigned(timeField[i++]);
        }
        final int milli   = (int) (milliInDay % 1000L);
        final int seconds = (int) ((milliInDay - milli) / 1000L);

        long subMilli = 0;
        while (i < timeField.length) {
            subMilli = subMilli * 256 + toUnsigned(timeField[i++]);
        }
        final TimeOffset timeOffset =
            new TimeOffset(seconds, TimeOffset.SECOND,
                           milli, TimeOffset.MILLISECOND,
                           subMilli, subMillisecondLength == 2 ? TimeOffset.MICROSECOND : TimeOffset.PICOSECOND);

        this.date = new DateComponents(epoch, day);
        this.time = new TimeComponents(timeOffset);

    }

    /** Build an instance from a CCSDS Calendar Segmented Time Code (CCS).
     * <p>
     * CCSDS Calendar Segmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * @param preambleField field specifying the format, often not transmitted in
     * data interfaces, as it is constant for a given data interface
     * @param timeField byte array containing the time code
     */
    CcsdsSegmentedTimeCode(final byte preambleField, final byte[] timeField) {

        // time code identification
        if ((preambleField & 0xF0) != 0x50) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }

        // time field length
        final int length = 7 + (preambleField & 0x07);
        if (length == 14) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD,
                                      formatByte(preambleField));
        }
        if (timeField.length != length) {
            throw new OrekitException(OrekitMessages.CCSDS_DATE_INVALID_LENGTH_TIME_FIELD,
                                      timeField.length, length);
        }

        // date part in the first four bytes
        if ((preambleField & 0x08) == 0x00) {
            // month of year and day of month variation
            this.date = new DateComponents(toUnsigned(timeField[0]) * 256 + toUnsigned(timeField[1]),
                                           toUnsigned(timeField[2]),
                                           toUnsigned(timeField[3]));
        } else {
            // day of year variation
            this.date = new DateComponents(toUnsigned(timeField[0]) * 256 + toUnsigned(timeField[1]),
                                           toUnsigned(timeField[2]) * 256 + toUnsigned(timeField[3]));
        }

        // time part from bytes 5 to last (between 7 and 13 depending on precision)
        final int hour        = toUnsigned(timeField[4]);
        final int minute      = toUnsigned(timeField[5]);
        final int second      = toUnsigned(timeField[6]);
        final int secondInDay = 3600 * hour + 60 * minute + second;

        long sub                  = 0;
        long attoSecondMultiplier = 1000000000000000000L;
        for (int i = 7; i < length; ++i) {
            sub                   = sub * 100L + toUnsigned(timeField[i]);
            attoSecondMultiplier /= 100L;
        }

        this.time = new TimeComponents(new TimeOffset(secondInDay, sub * attoSecondMultiplier));

    }

    /** Get the date part.
     * @return date part
     */
    public DateComponents getDate() {
        return date;
    }

    /** Get the time part.
     * @return time part
     */
    public TimeComponents getTime() {
        return time;
    }

}
