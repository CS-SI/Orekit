/* Copyright 2023 Thales Alenia Space
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
package org.orekit.gnss.rflink.gps;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.metric.parser.EncodedMessage;

/**
 * Container for sub-frames in a GPS navigation message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public abstract class SubFrame  {

    /** TLM preamble. */
    public static final int PREAMBLE_VALUE = 0x8b;

    /** Words size. */
    protected static final int WORD_SIZE = 30;

    /** Size of parity field. */
    protected static final int PARITY_SIZE = 6;

    /** TLM preamble index. */
    private static final int PREAMBLE = 0;

    /** Telemetry message index. */
    private static final int MESSAGE = 1;

    /** Integrity status flag index. */
    private static final int INTEGRITY_STATUS = 2;

    /** Truncated Time Of Week count index. */
    private static final int TOW_COUNT = 3;

    /** Alert flag index. */
    private static final int ALERT = 4;

    /** Anti-spoofing flag index. */
    private static final int ANTI_SPOOFING = 5;

    /** Sub-frame ID index. */
    private static final int ID = 6;

    /** Raw data fields. */
    private final int[] fields;

    /** Simple constructor.
     * @param words raw words
     * @param nbFields number of fields in the sub-frame
     * (including TLM and HOW data fields, excluding non-information and parity)
     */
    protected SubFrame(final int[] words, final int nbFields) {

        this.fields = new int[nbFields];

        // common fields present in telemetry and handover words for all sub-frames
        setField(PREAMBLE,         1, 22,  8, words);
        setField(MESSAGE,          1,  8, 14, words);
        setField(INTEGRITY_STATUS, 1,  7,  1, words);
        setField(TOW_COUNT,        2, 13, 17, words);
        setField(ALERT,            2, 12,  1, words);
        setField(ANTI_SPOOFING,    2, 11,  1, words);
        setField(ID,               2,  8,  3, words);

        if (getField(PREAMBLE) != PREAMBLE_VALUE) {
            throw new OrekitException(OrekitMessages.INVALID_GNSS_DATA, getField(PREAMBLE));
        }

    }

    /** Builder for sub-frames.
     * <p>
     * This builder creates the proper sub-frame type corresponding to the ID in handover word
     * and the SV Id for sub-frames 4 and 5.
     * </p>
     * @param encodedMessage encoded message containing exactly one sub-frame
     * @return sub-frame with TLM and HOW fields already set up
     * @see SubFrame1
     * @see SubFrame2
     * @see SubFrame3
     * @see SubFrame4A0
     * @see SubFrame4A1
     * @see SubFrame4B
     * @see SubFrame4C
     * @see SubFrame4D
     * @see SubFrame4E
     * @see SubFrameAlmanac
     * @see SubFrameDummyAlmanac
     */
    public static SubFrame parse(final EncodedMessage encodedMessage) {

        encodedMessage.start();

        // get the raw words
        final int[] words = new int[10];
        for (int i = 0; i < words.length; ++i) {
            words[i] = (int) encodedMessage.extractBits(30);
        }

        // check parity on all words
        for (int i = 0; i < words.length; ++i) {
            // we assume last word of previous sub frame had parity bits set to 0,
            // using the non_information bits at the end of each sub-frame
            if (!checkParity(i == 0 ? 0x0 : words[i - 1], words[i])) {
                throw new OrekitException(OrekitMessages.GNSS_PARITY_ERROR, i + 1);
            }
        }

        final int id = (words[1] >>>  8) & 0x7;
        switch (id) {
            case 1 : // IS-GPS-200 figure 40-1 sheet 1
                return new SubFrame1(words);
            case 2 : // IS-GPS-200 figure 40-1 sheet 2
                return new SubFrame2(words);
            case 3 : // IS-GPS-200 figure 40-1 sheet 3
                return new SubFrame3(words);
            case 4 : {
                final int svId = (words[2] >>> 22) & 0x3F;
                // see table 20-V for mapping between SV-ID and page format
                switch (svId) {
                    case 0  : // almanac for dummy Sv
                        return new SubFrameDummyAlmanac(words);
                    case 57 : // pages 1, 6, 11, 16, 21
                        // IS-GPS-200 figure 40-1 sheet 6
                        return new SubFrame4A0(words);
                    case 25 : case 26 : case 27 : case 28 : case 29 : case 30 : case 31 : case 32 : // pages 2, 3, 4, 5, 7, 8, 9, 10
                        // IS-GPS-200 figure 40-1 sheets 4 is also applicable to sub-frame 4
                        return new SubFrameAlmanac(words);
                    case 53 : case 54 : case 55 : // pages 14, 15, 17
                        // IS-GPS-200 figure 40-1 sheet 11
                        return new SubFrame4B(words);
                    case 58 : case 59 : case 60 : case 61 : case 62 : // pages 12, 19, 20, 22, 23, 24
                        // IS-GPS-200 figure 40-1 sheet 7
                        return new SubFrame4A1(words);
                    case 52 : // page 13
                        // IS-GPS-200 figure 40-1 sheet 10
                        return new SubFrame4C(words);
                    case 56 : // page 18
                        // IS-GPS-200 figure 40-1 sheet 8
                        return new SubFrame4D(words);
                    case 63 : // page 25
                        // IS-GPS-200 figure 40-1 sheet 9
                        return new SubFrame4E(words);
                    default :
                        throw new OrekitException(OrekitMessages.INVALID_GNSS_DATA, svId);
                }
            }
            case 5 : {
                // IS-GPS-200 figure 40-1 sheets 4 and 5
                final int page = (words[2] >>> 22) & 0x3F;
                return page == 25 ? new SubFrame5B(words) : new SubFrameAlmanac(words);
            }
            default : throw new OrekitException(OrekitMessages.INVALID_GNSS_DATA, id);
        }

    }

    /** Check parity.
     * <p>
     * This implements algorithm in table 20-XIV from IS-GPS-200N
     * </p>
     * @param previous previous 30 bits word (only two least significant bits are used)
     * @param current current 30 bits word
     * @return true if parity check succeeded
     */
    public static boolean checkParity(final int previous, final int current) {

        final int d29Star = previous & 0x2;
        final int d30Star = previous & 0x1;

        final int d25     = 0x1 & Integer.bitCount(d29Star | (current & 0x3B1F3480)); // 111011000111110011010010000000
        final int d26     = 0x1 & Integer.bitCount(d30Star | (current & 0x1D8F9A40)); // 011101100011111001101001000000
        final int d27     = 0x1 & Integer.bitCount(d29Star | (current & 0x2EC7CD00)); // 101110110001111100110100000000
        final int d28     = 0x1 & Integer.bitCount(d30Star | (current & 0x1763E680)); // 010111011000111110011010000000
        final int d29     = 0x1 & Integer.bitCount(d30Star | (current & 0x2BB1F340)); // 101011101100011111001101000000
        final int d30     = 0x1 & Integer.bitCount(d29Star | (current & 0x0B7A89C0)); // 001011011110101000100111000000

        final int parity  = ((((d25 << 1 | d26) << 1 | d27) << 1 | d28) << 1 | d29) << 1 | d30;

        return (parity & 0x3F) == (current & 0x3F);

    }

    /** Check if the sub-frame has parity errors.
     * @return true if frame has parity errors
     */
    public boolean hasParityErrors() {
        return false;
    }

    /** Get a field.
     * <p>
     * The field indices are defined as constants in the various sub-frames classes.
     * </p>
     * @param fieldIndex field index (counting from 0)
     * @return field value
     */
    protected int getField(final int fieldIndex) {
        return fields[fieldIndex];
    }

    /** Set a field.
     * @param fieldIndex field index (counting from 0)
     * @param wordIndex word index (counting from 1, to match IS-GPS-200 tables)
     * @param shift right shift to apply (i.e. number of LSB bits for next fields that should be removed)
     * @param nbBits number of bits in the field
     * @param words raw 30 bits words
     */
    protected void setField(final int fieldIndex, final int wordIndex,
                            final int shift, final int nbBits,
                            final int[] words) {
        fields[fieldIndex] = (words[wordIndex - 1] >>> shift) & ((0x1 << nbBits) - 1);
    }

    /** Set a field.
     * @param fieldIndex field index (counting from 0)
     * @param wordIndexMSB word index containing MSB (counting from 1, to match IS-GPS-200 tables)
     * @param shiftMSB right shift to apply to MSB (i.e. number of LSB bits for next fields that should be removed)
     * @param nbBitsMSB number of bits in the MSB
     * @param wordIndexLSB word index containing LSB (counting from 1, to match IS-GPS-200 tables)
     * @param shiftLSB right shift to apply to LSB (i.e. number of LSB bits for next fields that should be removed)
     * @param nbBitsLSB number of bits in the LSB
     * @param words raw 30 bits words
     */
    protected void setField(final int fieldIndex,
                            final int wordIndexMSB, final int shiftMSB, final int nbBitsMSB,
                            final int wordIndexLSB, final int shiftLSB, final int nbBitsLSB,
                            final int[] words) {
        final int msb = (words[wordIndexMSB - 1] >>> shiftMSB) & ((0x1 << nbBitsMSB) - 1);
        final int lsb = (words[wordIndexLSB - 1] >>> shiftLSB) & ((0x1 << nbBitsLSB) - 1);
        fields[fieldIndex] = msb << nbBitsLSB | lsb;
    }

    /** Get telemetry preamble.
     * @return telemetry preamble
     */
    public int getPreamble() {
        return getField(PREAMBLE);
    }

    /** Get telemetry message.
     * @return telemetry message
     */
    public int getMessage() {
        return getField(MESSAGE);
    }

    /** Get integrity status flag.
     * @return integrity status flag
     */
    public int getIntegrityStatus() {
        return getField(INTEGRITY_STATUS);
    }

    /** Get Time Of Week of next 12 second message.
     * @return Time Of Week of next 12 second message (s)
     */
    public int getTow() {
        return getField(TOW_COUNT) * 6;
    }

    /** Get alert flag.
     * @return alert flag
     */
    public int getAlert() {
        return getField(ALERT);
    }

    /** Get anti-spoofing flag.
     * @return anti-spoofing flag
     */
    public int getAntiSpoofing() {
        return getField(ANTI_SPOOFING);
    }

    /** Get sub-frame id.
     * @return sub-frame id
     */
    public int getId() {
        return getField(ID);
    }

}
