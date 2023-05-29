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
 * Container for sub-frames 4, page 13.
 * <p>
 * Table 40-1, sheet 10 in
 * <a href="https://navcen.uscg.gov/sites/default/files/pdf/gps/IS-GPS-200N.pdf">NAVSTAR
 * GPS Space Segment/Navigation User Segment Interface</a>, IS-GPS-200N, 22 Aug 2022
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SubFrame4C extends SubFrame45 {

    /** Number of Estimated Range Deviations. */
    public static final int NB_ERD = 30;

    /** Index of availability indicator field. */
    private static final int AVAILABILITY_INDICATOR = 9;

    /** Size of Estimated Range Deviations. */
    private static final int ERD_SIZE = 6;

    /** Simple constructor.
     * @param words raw words
     */
    SubFrame4C(final int[] words) {

        // create raw container
        super(words, AVAILABILITY_INDICATOR + NB_ERD + 1);

        // populate container
        int field = AVAILABILITY_INDICATOR;
        int word  = 3;
        int shift = 20;
        setField(field, word, shift, 2, words);
        for (int i = 0; i < NB_ERD; ++i) {
            if (shift >= ERD_SIZE + PARITY_SIZE) {
                // current word contains a complete ERD
                shift -= ERD_SIZE;
                setField(++field, word, shift, ERD_SIZE, words);
            } else {
                // current word contains only the MSF of next ERD
                final int msbBits = shift - PARITY_SIZE;
                shift += WORD_SIZE - ERD_SIZE;
                setField(++field,
                         word,     PARITY_SIZE,                      msbBits,
                         word + 1, WORD_SIZE - (ERD_SIZE - msbBits), ERD_SIZE - msbBits, words);
                ++word;
            }
        }

    }

    /** Get an Estimated Range Deviation.
     * @param index index of the ERD (between 1 and {@link #NB_ERD})
     * @return Estimated Range Deviation
     */
    public int getERD(final int index) {
        MathUtils.checkRangeInclusive(index, 1, NB_ERD);
        return getField(AVAILABILITY_INDICATOR + index);
    }

}
