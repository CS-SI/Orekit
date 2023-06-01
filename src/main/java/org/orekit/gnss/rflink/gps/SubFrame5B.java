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
 * Container for sub-frames 5, page 25.
 * <p>
 * Table 20-1, sheet 5 and table 40-1, sheet 5 in
 * <a href="https://navcen.uscg.gov/sites/default/files/pdf/gps/IS-GPS-200N.pdf">NAVSTAR
 * GPS Space Segment/Navigation User Segment Interface</a>, IS-GPS-200N, 22 Aug 2022
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SubFrame5B extends SubFrame45 {

    /** Number of SV health words. */
    private static final int SVH_WORDS = 6;

    /** Size of SV health fields per word. */
    private static final int SVH_FIELDS = 4;

    /** Index of TOA field. */
    private static final int TOA = 9;

    /** Index of Week Number field. */
    private static final int WEEK_NUMBER = 10;

    /** Index of reserved field A in word 10. */
    private static final int RESERVED_A_10 = WEEK_NUMBER + SVH_WORDS * SVH_FIELDS + 1;

    /** Index of reserved field B in word 10. */
    private static final int RESERVED_B_10 = RESERVED_A_10 + 1;

    /** Simple constructor.
     * @param words raw words
     */
    SubFrame5B(final int[] words) {

        // create raw container
        super(words, RESERVED_B_10 + 1);

        // populate container
        setField(TOA,              3, 14,  8, words);
        setField(WEEK_NUMBER,      3,  6,  8, words);
        int fieldIndex = WEEK_NUMBER + 1;
        for (int i = 0; i < SVH_WORDS; ++i) {
            for (int j = 0; j < SVH_FIELDS; ++j) {
                setField(fieldIndex++, 4 + i, 18 - 6 * j, 6, words);
            }
        }
        setField(RESERVED_A_10, 10, 24,  6, words);
        setField(RESERVED_B_10, 10,  8, 16, words);

    }

    /** Get Time of Almanac.
     * @return time of almanac (seconds)
     */
    public int getTOA() {
        return getField(TOA) << 12;
    }

    /** Get the almanac week number.
     * @return almanac week number
     */
    public int getWeekNumber() {
        return getField(WEEK_NUMBER);
    }

    /** Get the SV Health for a satellite.
     * @param index in the sub-frame (from 1 to 24, beware it is not the satellite number,
     * it is also related to {@link #getDataId()})
     * @return SV health
     */
    public int getSvHealth(final int index) {
        MathUtils.checkRangeInclusive(index, 1, 24);
        return getField(WEEK_NUMBER + index);
    }

    /** Get the reserved field A in word 10.
     * @return reserved field A in word 10
     */
    public int getReservedA10() {
        return getField(RESERVED_A_10);
    }

    /** Get the reserved field B in word 10.
     * @return reserved field B in word 10
     */
    public int getReservedB10() {
        return getField(RESERVED_B_10);
    }

}
