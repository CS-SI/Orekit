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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataTypeTest {

    @Test
    public void testBit1() {
        check(DataType.BIT_1,  0l,            0l);
        check(DataType.BIT_1,  1l,            0l);
        check(DataType.BIT_1, -1l,            1l);
        check(DataType.BIT_1, 0x1l << 63,     1l);
        check(DataType.BIT_1, Long.MAX_VALUE, 0l);
    }

    @Test
    public void testBit2() {
        check(DataType.BIT_2,  0l,            0l);
        check(DataType.BIT_2,  1l,            0l);
        check(DataType.BIT_2, -1l,            3l);
        check(DataType.BIT_2, 0x1l << 63,     2l);
        check(DataType.BIT_2, Long.MAX_VALUE, 1l);
    }

    @Test
    public void testBit3() {
        check(DataType.BIT_3,  0l,            0l);
        check(DataType.BIT_3,  1l,            0l);
        check(DataType.BIT_3, -1l,            7l);
        check(DataType.BIT_3, 0x1l << 63,     4l);
        check(DataType.BIT_3, Long.MAX_VALUE, 3l);
    }

    @Test
    public void testBit4() {
        check(DataType.BIT_4,  0l,             0l);
        check(DataType.BIT_4,  1l,             0l);
        check(DataType.BIT_4, -1l,            15l);
        check(DataType.BIT_4, 0x1l << 63,      8l);
        check(DataType.BIT_4, Long.MAX_VALUE,  7l);
    }

    @Test
    public void testBit6() {
        check(DataType.BIT_6,  0l,             0l);
        check(DataType.BIT_6,  1l,             0l);
        check(DataType.BIT_6, -1l,            63l);
        check(DataType.BIT_6, 0x1l << 63,     32l);
        check(DataType.BIT_6, Long.MAX_VALUE, 31l);
    }

    @Test
    public void testBit7() {
        check(DataType.BIT_7,  0l,              0l);
        check(DataType.BIT_7,  1l,              0l);
        check(DataType.BIT_7, -1l,            127l);
        check(DataType.BIT_7, 0x1l << 63,      64l);
        check(DataType.BIT_7, Long.MAX_VALUE,  63l);
    }

    @Test
    public void testBit8() {
        check(DataType.BIT_8,  0l,              0l);
        check(DataType.BIT_8,  1l,              0l);
        check(DataType.BIT_8, -1l,            255l);
        check(DataType.BIT_8, 0x1l << 63,     128l);
        check(DataType.BIT_8, Long.MAX_VALUE, 127l);
    }

    @Test
    public void testBit10() {
        check(DataType.BIT_10,  0l,               0l);
        check(DataType.BIT_10,  1l,               0l);
        check(DataType.BIT_10, -1l,            1023l);
        check(DataType.BIT_10, 0x1l << 63,     512l);
        check(DataType.BIT_10, Long.MAX_VALUE, 511l);
    }

    @Test
    public void testBit12() {
        check(DataType.BIT_12,  0l,               0l);
        check(DataType.BIT_12,  1l,               0l);
        check(DataType.BIT_12, -1l,            4095l);
        check(DataType.BIT_12, 0x1l << 63,     2048l);
        check(DataType.BIT_12, Long.MAX_VALUE, 2047l);
    }

    @Test
    public void testBit24() {
        check(DataType.BIT_24,  0l,                   0l);
        check(DataType.BIT_24,  1l,                   0l);
        check(DataType.BIT_24, -1l,            16777215l);
        check(DataType.BIT_24, 0x1l << 63,      8388608l);
        check(DataType.BIT_24, Long.MAX_VALUE,  8388607l);
    }

    @Test
    public void testBit32() {
        check(DataType.BIT_32,  0l,                     0l);
        check(DataType.BIT_32,  1l,                     0l);
        check(DataType.BIT_32, -1l,            4294967295l);
        check(DataType.BIT_32, 0x1l << 63,     2147483648l);
        check(DataType.BIT_32, Long.MAX_VALUE, 2147483647l);
    }

    @Test
    public void testInt6() {
        check(DataType.INT_6,  0l,               0l);
        check(DataType.INT_6,  1l,               0l);
        check(DataType.INT_6, -1l,              -1l);
        check(DataType.INT_6, 0x1l << 63,      null);
        check(DataType.INT_6, Long.MAX_VALUE,  31l);
    }

    @Test
    public void testInt8() {
        check(DataType.INT_8,  0l,               0l);
        check(DataType.INT_8,  1l,               0l);
        check(DataType.INT_8, -1l,              -1l);
        check(DataType.INT_8, 0x1l << 63,      null);
        check(DataType.INT_8, Long.MAX_VALUE,  127l);
    }

    @Test
    public void testInt9() {
        check(DataType.INT_9,  0l,               0l);
        check(DataType.INT_9,  1l,               0l);
        check(DataType.INT_9, -1l,              -1l);
        check(DataType.INT_9, 0x1l << 63,      null);
        check(DataType.INT_9, Long.MAX_VALUE,  255l);
    }

    @Test
    public void testInt10() {
        check(DataType.INT_10,  0l,               0l);
        check(DataType.INT_10,  1l,               0l);
        check(DataType.INT_10, -1l,              -1l);
        check(DataType.INT_10, 0x1l << 63,      null);
        check(DataType.INT_10, Long.MAX_VALUE,  511l);
    }

    @Test
    public void testInt11() {
        check(DataType.INT_11,  0l,                0l);
        check(DataType.INT_11,  1l,                0l);
        check(DataType.INT_11, -1l,               -1l);
        check(DataType.INT_11, 0x1l << 63,       null);
        check(DataType.INT_11, Long.MAX_VALUE,  1023l);
    }

    @Test
    public void testInt14() {
        check(DataType.INT_14,  0l,               0l);
        check(DataType.INT_14,  1l,               0l);
        check(DataType.INT_14, -1l,              -1l);
        check(DataType.INT_14, 0x1l << 63,      null);
        check(DataType.INT_14, Long.MAX_VALUE, 8191l);
    }

    @Test
    public void testInt15() {
        check(DataType.INT_15,  0l,               0l);
        check(DataType.INT_15,  1l,               0l);
        check(DataType.INT_15, -1l,              -1l);
        check(DataType.INT_15, 0x1l << 63,      null);
        check(DataType.INT_15, Long.MAX_VALUE, 16383l);
    }

    @Test
    public void testInt16() {
        check(DataType.INT_16,  0l,                0l);
        check(DataType.INT_16,  1l,                0l);
        check(DataType.INT_16, -1l,               -1l);
        check(DataType.INT_16, 0x1l << 63,       null);
        check(DataType.INT_16, Long.MAX_VALUE, 32767l);
    }

    @Test
    public void testInt17() {
        check(DataType.INT_17,  0l,                0l);
        check(DataType.INT_17,  1l,                0l);
        check(DataType.INT_17, -1l,               -1l);
        check(DataType.INT_17, 0x1l << 63,       null);
        check(DataType.INT_17, Long.MAX_VALUE, 65535l);
    }

    @Test
    public void testInt18() {
        check(DataType.INT_18,  0l,                 0l);
        check(DataType.INT_18,  1l,                 0l);
        check(DataType.INT_18, -1l,                -1l);
        check(DataType.INT_18, 0x1l << 63,        null);
        check(DataType.INT_18, Long.MAX_VALUE, 131071l);
    }

    @Test
    public void testInt19() {
        check(DataType.INT_19,  0l,                 0l);
        check(DataType.INT_19,  1l,                 0l);
        check(DataType.INT_19, -1l,                -1l);
        check(DataType.INT_19, 0x1l << 63,        null);
        check(DataType.INT_19, Long.MAX_VALUE, 262143l);
    }

    @Test
    public void testInt20() {
        check(DataType.INT_20,  0l,                  0l);
        check(DataType.INT_20,  1l,                  0l);
        check(DataType.INT_20, -1l,                 -1l);
        check(DataType.INT_20, 0x1l << 63,         null);
        check(DataType.INT_20, Long.MAX_VALUE,  524287l);
    }

    @Test
    public void testInt21() {
        check(DataType.INT_21,  0l,                  0l);
        check(DataType.INT_21,  1l,                  0l);
        check(DataType.INT_21, -1l,                 -1l);
        check(DataType.INT_21, 0x1l << 63,         null);
        check(DataType.INT_21, Long.MAX_VALUE, 1048575l);
    }

    @Test
    public void testInt22() {
        check(DataType.INT_22,  0l,                  0l);
        check(DataType.INT_22,  1l,                  0l);
        check(DataType.INT_22, -1l,                 -1l);
        check(DataType.INT_22, 0x1l << 63,         null);
        check(DataType.INT_22, Long.MAX_VALUE, 2097151l);
    }

    @Test
    public void testInt23() {
        check(DataType.INT_23,  0l,                  0l);
        check(DataType.INT_23,  1l,                  0l);
        check(DataType.INT_23, -1l,                 -1l);
        check(DataType.INT_23, 0x1l << 63,         null);
        check(DataType.INT_23, Long.MAX_VALUE, 4194303l);
    }

    @Test
    public void testInt24() {
        check(DataType.INT_24,  0l,                  0l);
        check(DataType.INT_24,  1l,                  0l);
        check(DataType.INT_24, -1l,                 -1l);
        check(DataType.INT_24, 0x1l << 63,         null);
        check(DataType.INT_24, Long.MAX_VALUE, 8388607l);
    }

    @Test
    public void testInt25() {
        check(DataType.INT_25,  0l,                   0l);
        check(DataType.INT_25,  1l,                   0l);
        check(DataType.INT_25, -1l,                  -1l);
        check(DataType.INT_25, 0x1l << 63,          null);
        check(DataType.INT_25, Long.MAX_VALUE, 16777215l);
    }

    @Test
    public void testInt26() {
        check(DataType.INT_26,  0l,                   0l);
        check(DataType.INT_26,  1l,                   0l);
        check(DataType.INT_26, -1l,                  -1l);
        check(DataType.INT_26, 0x1l << 63,          null);
        check(DataType.INT_26, Long.MAX_VALUE, 33554431l);
    }

    @Test
    public void testInt27() {
        check(DataType.INT_27,  0l,                   0l);
        check(DataType.INT_27,  1l,                   0l);
        check(DataType.INT_27, -1l,                  -1l);
        check(DataType.INT_27, 0x1l << 63,          null);
        check(DataType.INT_27, Long.MAX_VALUE, 67108863l);
    }

    @Test
    public void testInt30() {
        check(DataType.INT_30,  0l,                    0l);
        check(DataType.INT_30,  1l,                    0l);
        check(DataType.INT_30, -1l,                   -1l);
        check(DataType.INT_30, 0x1l << 63,           null);
        check(DataType.INT_30, Long.MAX_VALUE, 536870911l);
    }

    @Test
    public void testInt31() {
        check(DataType.INT_31,  0l,                    0l);
        check(DataType.INT_31,  1l,                    0l);
        check(DataType.INT_31, -1l,                   -1l);
        check(DataType.INT_31, 0x1l << 63,           null);
        check(DataType.INT_31, Long.MAX_VALUE, 1073741823l);
    }

    @Test
    public void testInt32() {
        check(DataType.INT_32,  0l,                     0l);
        check(DataType.INT_32,  1l,                     0l);
        check(DataType.INT_32, -1l,                    -1l);
        check(DataType.INT_32, 0x1l << 63,            null);
        check(DataType.INT_32, Long.MAX_VALUE, 2147483647l);
    }

    @Test
    public void testInt34() {
        check(DataType.INT_34,  0l,                     0l);
        check(DataType.INT_34,  1l,                     0l);
        check(DataType.INT_34, -1l,                    -1l);
        check(DataType.INT_34, 0x1l << 63,            null);
        check(DataType.INT_34, Long.MAX_VALUE, 8589934591l);
    }

    @Test
    public void testInt35() {
        check(DataType.INT_35,  0l,                     0l);
        check(DataType.INT_35,  1l,                     0l);
        check(DataType.INT_35, -1l,                    -1l);
        check(DataType.INT_35, 0x1l << 63,            null);
        check(DataType.INT_35, Long.MAX_VALUE, 17179869183l);
    }

    @Test
    public void testInt38() {
        check(DataType.INT_38,  0l,                        0l);
        check(DataType.INT_38,  1l,                        0l);
        check(DataType.INT_38, -1l,                       -1l);
        check(DataType.INT_38, 0x1l << 63,               null);
        check(DataType.INT_38, Long.MAX_VALUE,  137438953471l);
    }

    @Test
    public void testUint2() {
        check(DataType.U_INT_2,  0l,            0l);
        check(DataType.U_INT_2,  1l,            0l);
        check(DataType.U_INT_2, -1l,            3l);
        check(DataType.U_INT_2, 0x1l << 63,     2l);
        check(DataType.U_INT_2, Long.MAX_VALUE, 1l);
    }

    @Test
    public void testUint3() {
        check(DataType.U_INT_3,  0l,            0l);
        check(DataType.U_INT_3,  1l,            0l);
        check(DataType.U_INT_3, -1l,            7l);
        check(DataType.U_INT_3, 0x1l << 63,     4l);
        check(DataType.U_INT_3, Long.MAX_VALUE, 3l);
    }

    @Test
    public void testUint4() {
        check(DataType.U_INT_4,  0l,             0l);
        check(DataType.U_INT_4,  1l,             0l);
        check(DataType.U_INT_4, -1l,            15l);
        check(DataType.U_INT_4, 0x1l << 63,      8l);
        check(DataType.U_INT_4, Long.MAX_VALUE,  7l);
    }

    @Test
    public void testUint5() {
        check(DataType.U_INT_5,  0l,             0l);
        check(DataType.U_INT_5,  1l,             0l);
        check(DataType.U_INT_5, -1l,            31l);
        check(DataType.U_INT_5, 0x1l << 63,     16l);
        check(DataType.U_INT_5, Long.MAX_VALUE, 15l);
    }

    @Test
    public void testUint6() {
        check(DataType.U_INT_6,  0l,             0l);
        check(DataType.U_INT_6,  1l,             0l);
        check(DataType.U_INT_6, -1l,            63l);
        check(DataType.U_INT_6, 0x1l << 63,     32l);
        check(DataType.U_INT_6, Long.MAX_VALUE, 31l);
    }

    @Test
    public void testUint7() {
        check(DataType.U_INT_7,  0l,              0l);
        check(DataType.U_INT_7,  1l,              0l);
        check(DataType.U_INT_7, -1l,            127l);
        check(DataType.U_INT_7, 0x1l << 63,      64l);
        check(DataType.U_INT_7, Long.MAX_VALUE,  63l);
    }

    @Test
    public void testUint8() {
        check(DataType.U_INT_8,  0l,              0l);
        check(DataType.U_INT_8,  1l,              0l);
        check(DataType.U_INT_8, -1l,            255l);
        check(DataType.U_INT_8, 0x1l << 63,     128l);
        check(DataType.U_INT_8, Long.MAX_VALUE, 127l);
    }

    @Test
    public void testUint9() {
        check(DataType.U_INT_9,  0l,              0l);
        check(DataType.U_INT_9,  1l,              0l);
        check(DataType.U_INT_9, -1l,            511l);
        check(DataType.U_INT_9, 0x1l << 63,     256l);
        check(DataType.U_INT_9, Long.MAX_VALUE, 255l);
    }

    @Test
    public void testUint10() {
        check(DataType.U_INT_10,  0l,               0l);
        check(DataType.U_INT_10,  1l,               0l);
        check(DataType.U_INT_10, -1l,            1023l);
        check(DataType.U_INT_10, 0x1l << 63,      512l);
        check(DataType.U_INT_10, Long.MAX_VALUE,  511l);
    }

    @Test
    public void testUint11() {
        check(DataType.U_INT_11,  0l,               0l);
        check(DataType.U_INT_11,  1l,               0l);
        check(DataType.U_INT_11, -1l,            2047l);
        check(DataType.U_INT_11, 0x1l << 63,     1024l);
        check(DataType.U_INT_11, Long.MAX_VALUE, 1023l);
    }

    @Test
    public void testUint12() {
        check(DataType.U_INT_12,  0l,               0l);
        check(DataType.U_INT_12,  1l,               0l);
        check(DataType.U_INT_12, -1l,            4095l);
        check(DataType.U_INT_12, 0x1l << 63,     2048l);
        check(DataType.U_INT_12, Long.MAX_VALUE, 2047l);
    }

    @Test
    public void testUint13() {
        check(DataType.U_INT_13,  0l,               0l);
        check(DataType.U_INT_13,  1l,               0l);
        check(DataType.U_INT_13, -1l,            8191l);
        check(DataType.U_INT_13, 0x1l << 63,     4096l);
        check(DataType.U_INT_13, Long.MAX_VALUE, 4095l);
    }

    @Test
    public void testUint14() {
        check(DataType.U_INT_14,  0l,                0l);
        check(DataType.U_INT_14,  1l,                0l);
        check(DataType.U_INT_14, -1l,            16383l);
        check(DataType.U_INT_14, 0x1l << 63,      8192l);
        check(DataType.U_INT_14, Long.MAX_VALUE,  8191l);
    }

    @Test
    public void testUint16() {
        check(DataType.U_INT_16,  0l,                0l);
        check(DataType.U_INT_16,  1l,                0l);
        check(DataType.U_INT_16, -1l,            65535l);
        check(DataType.U_INT_16, 0x1l << 63,     32768l);
        check(DataType.U_INT_16, Long.MAX_VALUE, 32767l);
    }

    @Test
    public void testUint17() {
        check(DataType.U_INT_17,  0l,                 0l);
        check(DataType.U_INT_17,  1l,                 0l);
        check(DataType.U_INT_17, -1l,            131071l);
        check(DataType.U_INT_17, 0x1l << 63,      65536l);
        check(DataType.U_INT_17, Long.MAX_VALUE,  65535l);
    }

    @Test
    public void testUint18() {
        check(DataType.U_INT_18,  0l,                 0l);
        check(DataType.U_INT_18,  1l,                 0l);
        check(DataType.U_INT_18, -1l,            262143l);
        check(DataType.U_INT_18, 0x1l << 63,     131072l);
        check(DataType.U_INT_18, Long.MAX_VALUE, 131071l);
    }

    @Test
    public void testUint20() {
        check(DataType.U_INT_20,  0l,                  0l);
        check(DataType.U_INT_20,  1l,                  0l);
        check(DataType.U_INT_20, -1l,            1048575l);
        check(DataType.U_INT_20, 0x1l << 63,      524288l);
        check(DataType.U_INT_20, Long.MAX_VALUE,  524287l);
    }

    @Test
    public void testUint23() {
        check(DataType.U_INT_23,  0l,                  0l);
        check(DataType.U_INT_23,  1l,                  0l);
        check(DataType.U_INT_23, -1l,            8388607l);
        check(DataType.U_INT_23, 0x1l << 63,     4194304l);
        check(DataType.U_INT_23, Long.MAX_VALUE, 4194303l);
    }

    @Test
    public void testUint24() {
        check(DataType.U_INT_24,  0l,                   0l);
        check(DataType.U_INT_24,  1l,                   0l);
        check(DataType.U_INT_24, -1l,            16777215l);
        check(DataType.U_INT_24, 0x1l << 63,      8388608l);
        check(DataType.U_INT_24, Long.MAX_VALUE,  8388607l);
    }

    @Test
    public void testUint25() {
        check(DataType.U_INT_25,  0l,                   0l);
        check(DataType.U_INT_25,  1l,                   0l);
        check(DataType.U_INT_25, -1l,            33554431l);
        check(DataType.U_INT_25, 0x1l << 63,     16777216l);
        check(DataType.U_INT_25, Long.MAX_VALUE, 16777215l);
    }

    @Test
    public void testUint26() {
        check(DataType.U_INT_26,  0l,                   0l);
        check(DataType.U_INT_26,  1l,                   0l);
        check(DataType.U_INT_26, -1l,            67108863l);
        check(DataType.U_INT_26, 0x1l << 63,     33554432l);
        check(DataType.U_INT_26, Long.MAX_VALUE, 33554431l);
    }

    @Test
    public void testUint27() {
        check(DataType.U_INT_27,  0l,                    0l);
        check(DataType.U_INT_27,  1l,                    0l);
        check(DataType.U_INT_27, -1l,            134217727l);
        check(DataType.U_INT_27, 0x1l << 63,      67108864l);
        check(DataType.U_INT_27, Long.MAX_VALUE,  67108863l);
    }

    @Test
    public void testUint30() {
        check(DataType.U_INT_30,  0l,                     0l);
        check(DataType.U_INT_30,  1l,                     0l);
        check(DataType.U_INT_30, -1l,            1073741823l);
        check(DataType.U_INT_30, 0x1l << 63,      536870912l);
        check(DataType.U_INT_30, Long.MAX_VALUE,  536870911l);
    }

    @Test
    public void testUint32() {
        check(DataType.U_INT_32,  0l,                     0l);
        check(DataType.U_INT_32,  1l,                     0l);
        check(DataType.U_INT_32, -1l,            4294967295l);
        check(DataType.U_INT_32, 0x1l << 63,     2147483648l);
        check(DataType.U_INT_32, Long.MAX_VALUE, 2147483647l);
    }

    @Test
    public void testUint35() {
        check(DataType.U_INT_35,  0l,                      0l);
        check(DataType.U_INT_35,  1l,                      0l);
        check(DataType.U_INT_35, -1l,            34359738367l);
        check(DataType.U_INT_35, 0x1l << 63,     17179869184l);
        check(DataType.U_INT_35, Long.MAX_VALUE, 17179869183l);
    }

    @Test
    public void testUint36() {
        check(DataType.U_INT_36,  0l,                      0l);
        check(DataType.U_INT_36,  1l,                      0l);
        check(DataType.U_INT_36, -1l,            68719476735l);
        check(DataType.U_INT_36, 0x1l << 63,     34359738368l);
        check(DataType.U_INT_36, Long.MAX_VALUE, 34359738367l);
    }

    @Test
    public void testIntS5() {
        check(DataType.INT_S_5,  0l,              0l);
        check(DataType.INT_S_5,  1l,              0l);
        check(DataType.INT_S_5, -1l,            -15l);
        check(DataType.INT_S_5, 0x1l << 63,       0l); // negative 0, not really used in IGS SSR
        check(DataType.INT_S_5, Long.MAX_VALUE,  15l);
    }

    @Test
    public void testIntS11() {
        check(DataType.INT_S_11,  0l,                0l);
        check(DataType.INT_S_11,  1l,                0l);
        check(DataType.INT_S_11, -1l,            -1023l);
        check(DataType.INT_S_11, 0x1l << 63,         0l); // negative 0, not really used in IGS SSR
        check(DataType.INT_S_11, Long.MAX_VALUE,  1023l);
    }

    @Test
    public void testIntS22() {
        check(DataType.INT_S_22,  0l,                   0l);
        check(DataType.INT_S_22,  1l,                   0l);
        check(DataType.INT_S_22, -1l,            -2097151l);
        check(DataType.INT_S_22, 0x1l << 63,             0l); // negative 0, not really used in IGS SSR
        check(DataType.INT_S_22, Long.MAX_VALUE,  2097151l);
    }

    @Test
    public void testIntS24() {
        check(DataType.INT_S_24,  0l,                   0l);
        check(DataType.INT_S_24,  1l,                   0l);
        check(DataType.INT_S_24, -1l,            -8388607l);
        check(DataType.INT_S_24, 0x1l << 63,            0l); // negative 0, not really used in IGS SSR
        check(DataType.INT_S_24, Long.MAX_VALUE,  8388607l);
    }

    @Test
    public void testIntS27() {
        check(DataType.INT_S_27,  0l,                    0l);
        check(DataType.INT_S_27,  1l,                    0l);
        check(DataType.INT_S_27, -1l,            -67108863l);
        check(DataType.INT_S_27, 0x1l << 63,             0l); // negative 0, not really used in IGS SSR
        check(DataType.INT_S_27, Long.MAX_VALUE,  67108863l);
    }

    @Test
    public void testIntS32() {
        check(DataType.INT_S_32,  0l,                      0l);
        check(DataType.INT_S_32,  1l,                      0l);
        check(DataType.INT_S_32, -1l,            -2147483647l);
        check(DataType.INT_S_32, 0x1l << 63,               0l); // negative 0, not really used in IGS SSR
        check(DataType.INT_S_32, Long.MAX_VALUE,  2147483647l);
    }

    @Test
    public void testNumber() {
        Assertions.assertEquals(68, DataType.values().length);
    }

    private void check(DataType dataType, long rawMessage, Long expected) {
        final EncodedMessage rm = (n) -> rawMessage >>> (64 - n);
        final Long result = dataType.decode(rm);
        Assertions.assertEquals(expected, result);
    }
}
