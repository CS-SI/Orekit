/* Copyright 2002-2023 CS GROUP
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
package org.orekit.gnss.metric.parser;

import java.util.function.Function;

/** Enum containing all low level data types that can be parsed
 * to build a message.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum DataType {

    /** 1 bit. */
    BIT_1(m -> m.extractBits(1)),

    /** 2 bits. */
    BIT_2(m -> m.extractBits(2)),

    /** 3 bits. */
    BIT_3(m -> m.extractBits(3)),

    /** 4 bits. */
    BIT_4(m -> m.extractBits(4)),

    /** 6 bits. */
    BIT_6(m -> m.extractBits(6)),

    /** 7 bits. */
    BIT_7(m -> m.extractBits(7)),

    /** 8 bits. */
    BIT_8(m -> m.extractBits(8)),

    /** 10 bits. */
    BIT_10(m -> m.extractBits(10)),

    /** 12 bits. */
    BIT_12(m -> m.extractBits(12)),

    /** 24 bits. */
    BIT_24(m -> m.extractBits(24)),

    /** 32 bits. */
    BIT_32(m -> m.extractBits(32)),

    /** 6 bits signed integer. */
    INT_6(m -> {
        final long msb = 0x20l;
        final long signed = (m.extractBits(6) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 8 bits signed integer. */
    INT_8(m -> {
        final long msb = 0x80l;
        final long signed = (m.extractBits(8) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 9 bits signed integer. */
    INT_9(m -> {
        final long msb = 0x100l;
        final long signed = (m.extractBits(9) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 10 bits signed integer. */
    INT_10(m -> {
        final long msb = 0x200l;
        final long signed = (m.extractBits(10) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 11 bits signed integer. */
    INT_11(m -> {
        final long msb = 0x400l;
        final long signed = (m.extractBits(11) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 14 bits signed integer. */
    INT_14(m -> {
        final long msb = 0x2000l;
        final long signed = (m.extractBits(14) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 15 bits signed integer. */
    INT_15(m -> {
        final long msb = 0x4000l;
        final long signed = (m.extractBits(15) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 16 bits signed integer. */
    INT_16(m -> {
        final long msb    = 0x8000l;
        final long signed = (m.extractBits(16) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 17 bits signed integer. */
    INT_17(m -> {
        final long msb    = 0x10000l;
        final long signed = (m.extractBits(17) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 18 bits signed integer. */
    INT_18(m -> {
        final long msb    = 0x20000l;
        final long signed = (m.extractBits(18) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 19 bits signed integer. */
    INT_19(m -> {
        final long msb    = 0x40000l;
        final long signed = (m.extractBits(19) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 20 bits signed integer. */
    INT_20(m -> {
        final long msb    = 0x80000l;
        final long signed = (m.extractBits(20) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 21 bits signed integer. */
    INT_21(m -> {
        final long msb    = 0x100000l;
        final long signed = (m.extractBits(21) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 22 bits signed integer. */
    INT_22(m -> {
        final long msb    = 0x200000l;
        final long signed = (m.extractBits(22) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 23 bits signed integer. */
    INT_23(m -> {
        final long msb    = 0x400000l;
        final long signed = (m.extractBits(23) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 24 bits signed integer. */
    INT_24(m -> {
        final long msb    = 0x800000l;
        final long signed = (m.extractBits(24) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 25 bits signed integer. */
    INT_25(m -> {
        final long msb    = 0x1000000l;
        final long signed = (m.extractBits(25) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 26 bits signed integer. */
    INT_26(m -> {
        final long msb    = 0x2000000l;
        final long signed = (m.extractBits(26) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 27 bits signed integer. */
    INT_27(m -> {
        final long msb    = 0x4000000l;
        final long signed = (m.extractBits(27) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 30 bits signed integer. */
    INT_30(m -> {
        final long msb    = 0x20000000l;
        final long signed = (m.extractBits(30) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 31 bits signed integer. */
    INT_31(m -> {
        final long msb    = 0x40000000l;
        final long signed = (m.extractBits(31) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 32 bits signed integer. */
    INT_32(m -> {
        final long msb    = 0x80000000l;
        final long signed = (m.extractBits(32) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 34 bits signed integer. */
    INT_34(m -> {
        final long msb    = 0x200000000l;
        final long signed = (m.extractBits(34) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 35 bits signed integer. */
    INT_35(m -> {
        final long msb    = 0x400000000l;
        final long signed = (m.extractBits(35) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 38 bits signed integer. */
    INT_38(m -> {
        final long msb = 0x2000000000l;
        final long signed = (m.extractBits(38) ^ msb) - msb;
        return signed == -msb ? null : signed; // return null for the special value meaning no data
    }),

    /** 2 bits unsigned integer. */
    U_INT_2(m -> m.extractBits(2)),

    /** 3 bits unsigned integer. */
    U_INT_3(m -> m.extractBits(3)),

    /** 4 bits unsigned integer. */
    U_INT_4(m -> m.extractBits(4)),

    /** 5 bits unsigned integer. */
    U_INT_5(m -> m.extractBits(5)),

    /** 6 bits unsigned integer. */
    U_INT_6(m -> m.extractBits(6)),

    /** 7 bits unsigned integer. */
    U_INT_7(m -> m.extractBits(7)),

    /** 8 bits unsigned integer. */
    U_INT_8(m -> m.extractBits(8)),

    /** 9 bits unsigned integer. */
    U_INT_9(m -> m.extractBits(9)),

    /** 10 bits unsigned integer. */
    U_INT_10(m -> m.extractBits(10)),

    /** 11 bits unsigned integer. */
    U_INT_11(m -> m.extractBits(11)),

    /** 12 bits unsigned integer. */
    U_INT_12(m -> m.extractBits(12)),

    /** 13 bits unsigned integer. */
    U_INT_13(m -> m.extractBits(13)),

    /** 14 bits unsigned integer. */
    U_INT_14(m -> m.extractBits(14)),

    /** 16 bits unsigned integer. */
    U_INT_16(m -> m.extractBits(16)),

    /** 17 bits unsigned integer. */
    U_INT_17(m -> m.extractBits(17)),

    /** 18 bits unsigned integer. */
    U_INT_18(m -> m.extractBits(18)),

    /** 20 bits unsigned integer. */
    U_INT_20(m -> m.extractBits(20)),

    /** 23 bits unsigned integer. */
    U_INT_23(m -> m.extractBits(23)),

    /** 24 bits unsigned integer. */
    U_INT_24(m -> m.extractBits(24)),

    /** 25 bits unsigned integer. */
    U_INT_25(m -> m.extractBits(25)),

    /** 26 bits unsigned integer. */
    U_INT_26(m -> m.extractBits(26)),

    /** 27 bits unsigned integer. */
    U_INT_27(m -> m.extractBits(27)),

    /** 30 bits unsigned integer. */
    U_INT_30(m -> m.extractBits(30)),

    /** 32 bits unsigned integer. */
    U_INT_32(m -> m.extractBits(32)),

    /** 35 bits unsigned integer. */
    U_INT_35(m -> m.extractBits(35)),

    /** 36 bits unsigned integer. */
    U_INT_36(m -> m.extractBits(36)),

    /** 5 bits sign-magnitude integer. */
    INT_S_5(m -> {
        final long data = m.extractBits(5);
        final long mask = -(data >>> 4); // this mask allows avoiding a conditional below
        return (~mask & data) | (mask & (16l - data));
    }),

    /** 11 bits sign-magnitude integer. */
    INT_S_11(m -> {
        final long data = m.extractBits(11);
        final long mask = -(data >>> 10); // this mask allows avoiding a conditional below
        return (~mask & data) | (mask & (1024l - data));
    }),

    /** 22 bits sign-magnitude integer. */
    INT_S_22(m -> {
        final long data = m.extractBits(22);
        final long mask = -(data >>> 21); // this mask allows avoiding a conditional below
        return (~mask & data) | (mask & (2097152l - data));
    }),

    /** 24 bits sign-magnitude integer. */
    INT_S_24(m -> {
        final long data = m.extractBits(24);
        final long mask = -(data >>> 23); // this mask allows avoiding a conditional below
        return (~mask & data) | (mask & (8388608l - data));
    }),

    /** 27 bits sign-magnitude integer. */
    INT_S_27(m -> {
        final long data = m.extractBits(27);
        final long mask = -(data >>> 26); // this mask allows avoiding a conditional below
        return (~mask & data) | (mask & (67108864l - data));
    }),

    /** 32 bits sign-magnitude integer. */
    INT_S_32(m -> {
        final long data = m.extractBits(32);
        final long mask = -(data >>> 31); // this mask allows avoiding a conditional below
        return (~mask & data) | (mask & (2147483648l - data));
    });

    /** Decoding function. */
    private final Function<EncodedMessage, Long> decoder;

    /** Simple constructor.
     * @param decoder decoding function for the data type
     */
    DataType(final Function<EncodedMessage, Long> decoder) {
        this.decoder = decoder;
    }

    /** Decode a piece of data extracted from an encoded message.
     * @param message encoded message providing the bits to decode
     * @return data decoded as a Long object, or null if data not available
     */
    public Long decode(final EncodedMessage message) {
        return decoder.apply(message);
    }

}
