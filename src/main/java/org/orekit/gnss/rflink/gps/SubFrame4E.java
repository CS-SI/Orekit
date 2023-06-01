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

import org.hipparchus.util.MathUtils;

/**
 * Container for sub-frames 4, page 25.
 * <p>
 * Table 20-1, sheet 9 and table 40-1, sheet 9 in
 * <a href="https://navcen.uscg.gov/sites/default/files/pdf/gps/IS-GPS-200N.pdf">NAVSTAR
 * GPS Space Segment/Navigation User Segment Interface</a>, IS-GPS-200N, 22 Aug 2022
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SubFrame4E extends SubFrame45 {

    /** Number of Anti-spoofing entries. */
    public static final int NB_AS = 32;

    /** Number of SV health entries. */
    public static final int NB_SVH = 8;

    /** Size of Anti-spoofing. */
    private static final int AS_SIZE = 4;

    /** Size of SV Health. */
    private static final int SVH_SIZE = 6;

    /** Index of first AS field. */
    private static final int FIRST_AS = 9;

    /** Index of reserved field in word 8. */
    private static final int RESERVED_8 = FIRST_AS + NB_AS;

    /** Index of reserved field in word 10. */
    private static final int RESERVED_10 = RESERVED_8 + 1 + NB_AS + NB_SVH;

    /** Simple constructor.
     * @param words raw words
     */
    SubFrame4E(final int[] words) {

        // create raw container
        super(words, RESERVED_10 + 1);

        // populate container
        int field = FIRST_AS - 1;
        int word  = 3;
        int shift = 22;

        // anti-spoofing
        for (int i = 0; i < NB_AS; ++i) {
            if (shift >= AS_SIZE + PARITY_SIZE) {
                // current word contains a complete AS
                shift -= AS_SIZE;
                setField(++field, word, shift, AS_SIZE, words);
            } else {
                // current word is exhausted
                shift = WORD_SIZE - AS_SIZE;
                setField(++field, ++word, shift, AS_SIZE, words);
            }
        }

        // 2 bits for system use
        shift -= 2;
        setField(RESERVED_8, word, shift, 2, words);

        // SV health
        for (int i = 0; i < NB_SVH; ++i) {
            if (shift >= SVH_SIZE + PARITY_SIZE) {
                // current word contains a complete SVH
                shift -= SVH_SIZE;
                setField(++field, word, shift, SVH_SIZE, words);
            } else {
                // current word is exhausted
                shift = WORD_SIZE - SVH_SIZE;
                setField(++field, ++word, shift, SVH_SIZE, words);
            }
        }

        setField(RESERVED_10, 10,  8, 4, words);

    }

    /** Get the anti-spoofing for a satellite.
     * @param index in the sub-frame (from 1 to 32, beware it is not the satellite number,
     * it is also related to {@link #getDataId()})
     * @return anti-spoofing
     */
    public int getAntiSpoofing(final int index) {
        MathUtils.checkRangeInclusive(index, 1, NB_AS);
        return getField(FIRST_AS + index - 1);
    }

    /** Get the reserved field in word 8.
     * @return reserved field in word 8
     */
    public int getReserved8() {
        return getField(RESERVED_8);
    }

    /** Get the Sv health for a satellite.
     * @param index in the sub-frame (from 1 to 7 or 1 to 8 depending on
     * {@link #getDataId()}, beware it is not the satellite number,
     * it is also related to {@link #getDataId()}), an
     * @return anti-spoofing
     */
    public int getSvHealth(final int index) {
        MathUtils.checkRangeInclusive(index, 1, NB_SVH);
        return getField(RESERVED_8 + index);
    }

    /** Get the reserved field in word 10.
     * @return reserved field in word 10
     */
    public int getReserved10() {
        return getField(RESERVED_10);
    }

}
