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

import org.hipparchus.util.FastMath;

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

    /** Index of URA_INDEX index field. */
    private static final int URA_INDEX = 9;

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
        setField(WEEK_NUMBER,  3, 20, 10, words);
        setField(CA_OR_P,      3, 18,  2, words);
        setField(URA_INDEX,          3, 14,  4, words);
        setField(SV_HEALTH,    3,  8,  6, words);
        setField(IODC,         3,  6,  2, 8, 22, 8, words);
        setField(L2_P_DATA,    4, 29,  1, words);
        setField(RESERVED_4,   4,  6, 23, words);
        setField(RESERVED_5,   5,  6, 24, words);
        setField(RESERVED_6,   6,  6, 24, words);
        setField(RESERVED_7,   7, 14, 16, words);
        setField(TGD,          7,  6,  8, words);
        setField(TOC,          8,  6, 16, words);
        setField(AF2,          9, 22,  8, words);
        setField(AF1,          9,  6, 16, words);
        setField(AF0,         10,  8, 22, words);

    }

    /** Get Week Number.
     * @return week number
     */
    public int getWeekNumber() {
        return getField(WEEK_NUMBER);
    }

    /** Get C/A or P flag.
     * @return C/A or P flag
     */
    public int getCaOrPFlag() {
        return getField(CA_OR_P);
    }

    /** Get URA index.
     * @return URA index
     */
    public int getUraIndex() {
        return getField(URA_INDEX);
    }

    /** Get SV health.
     * @return SV health
     */
    public int getSvHealth() {
        return getField(SV_HEALTH);
    }

    /** Get IODC.
     * @return IODC
     */
    public int getIODC() {
        return getField(IODC);
    }

    /** Get L2 P data flag.
     * @return L2 P data flag
     */
    public int getL2PDataFlag() {
        return getField(L2_P_DATA);
    }

    /** Get the reserved field in word 4.
     * @return reserved field in word 4
     */
    public int getReserved04() {
        return getField(RESERVED_4);
    }

    /** Get the reserved field in word 5.
     * @return reserved field in word 5
     */
    public int getReserved05() {
        return getField(RESERVED_5);
    }

    /** Get the reserved field in word 6.
     * @return reserved field in word 6
     */
    public int getReserved06() {
        return getField(RESERVED_6);
    }

    /** Get the reserved field in word 7.
     * @return reserved field in word 7
     */
    public int getReserved07() {
        return getField(RESERVED_7);
    }

    /** Get the TGD.
     * @return TGD
     */
    public int getTGD() {
        return getField(TGD);
    }

    /** Get the TOC.
     * @return TOC
     */
    public int getTOC() {
        return getField(TOC);
    }

    /** Get af₂.
     * @return af₂ (second/second²)
     */
    public double getAF2() {
        return FastMath.scalb((double) getField(AF2), -55);
    }

    /** Get af₁.
     * @return af₁ (second/second)
     */
    public double getAF1() {
        return FastMath.scalb((double) getField(AF1), -43);
    }

    /** Get af₀.
     * @return af₀
     */
    public double getAF0() {
        return FastMath.scalb((double) getField(AF0), -31);
    }

}
