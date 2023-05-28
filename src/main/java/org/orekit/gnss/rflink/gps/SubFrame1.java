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

/**
 * Container for sub-frames 1.
 * <p>
 * Table 20-1, sheet 1 and table 40-1, sheet 1 in
 * <a href="https://navcen.uscg.gov/sites/default/files/pdf/gps/IS-GPS-200N.pdf">NAVSTAR
 * GPS Space Segment/Navigation User Segment Interface</a>, IS-GPS-200N, 22 Aug 2022
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SubFrame1 extends SubFrame {

    /** Index of Week Number field. */
    private static final int WEEK_NUMBER = 7;

    /** Index of C/A or P code field. */
    private static final int CA_OR_P = 8;

    /** Index of URA index field. */
    private static final int URA = 9;

    /** Index of SV Health field. */
    private static final int SV_HEALTH = 10;

    /** Index of IODC field. */
    private static final int IODC  = 11;

    /** Index of L2 P data flag field. */
    private static final int L2_P_DATA = 12;

    /** Index of reserved field in word 4. */
    private static final int RESERVED_4 = 13;

    /** Index of reserved field in word 5. */
    private static final int RESERVED_5 = 14;

    /** Index of reserved field in word 6. */
    private static final int RESERVED_6 = 15;

    /** Index of reserved field in word 7. */
    private static final int RESERVED_7 = 16;

    /** Index of TGD field. */
    private static final int TGD = 17;

    /** Index of TOC field. */
    private static final int TOC  = 18;

    /** Index of AF2 field. */
    private static final int AF2  = 19;

    /** Index of AF1 field. */
    private static final int AF1  = 20;

    /** Index of AF0 field. */
    private static final int AF0  = 21;

    /** */
    /** Simple constructor.
     * @param words raw words
     */
    SubFrame1(final int[] words) {

        // create raw container
        super(words, AF0 + 1);

        // populate container
        setField(WEEK_NUMBER,  3, 20, 10);
        setField(CA_OR_P,      3, 18,  2);
        setField(URA,          3, 14,  4);
        setField(SV_HEALTH,    3,  8,  6);
        setField(IODC,         3,  6,  2, 8, 22, 8);
        setField(L2_P_DATA,    4, 29,  1);
        setField(RESERVED_4,   4,  6, 23);
        setField(RESERVED_5,   5,  6, 24);
        setField(RESERVED_6,   6,  6, 24);
        setField(RESERVED_7,   7, 14, 16);
        setField(TGD,          7,  6,  8);
        setField(TOC,          8,  6, 16);
        setField(AF2,          9, 22,  8);
        setField(AF1,          9,  6, 16);
        setField(AF0,         20,  8, 22);

    }

}
