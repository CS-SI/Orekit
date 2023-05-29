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
 * Container for sub-frames 4, page 18.
 * <p>
 * Table 40-1, sheet 8 in
 * <a href="https://navcen.uscg.gov/sites/default/files/pdf/gps/IS-GPS-200N.pdf">NAVSTAR
 * GPS Space Segment/Navigation User Segment Interface</a>, IS-GPS-200N, 22 Aug 2022
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SubFrame4D extends SubFrame45 {

    /** Index of α₀ field. */
    private static final int ALPHA0 = 9;

    /** Index of α₁ field. */
    private static final int ALPHA1 = 10;

    /** Index of α₂ field. */
    private static final int ALPHA2 = 11;

    /** Index of α₃ field. */
    private static final int ALPHA3 = 12;

    /** Index of β₀ field. */
    private static final int BETA0 = 13;

    /** Index of β₁ field. */
    private static final int BETA1 = 14;

    /** Index of β₂ field. */
    private static final int BETA2 = 15;

    /** Index of β₃ field. */
    private static final int BETA3 = 16;

    /** Index of A₁ field. */
    private static final int A1 = 17;

    /** Index of A0 field. */
    private static final int A0 = 18;

    /** Index of TOT field. */
    private static final int TOT = 19;

    /** Index of Week Number T field. */
    private static final int WEEK_NUMBER_T = 20;

    /** Index of ΔtLS field. */
    private static final int DELTA_T_LS = 21;

    /** Index of Week Number LSF field. */
    private static final int WEEK_NUMBER_LSF = 22;

    /** Index of DN field. */
    private static final int DN = 23;

    /** Index of ΔtLSF field. */
    private static final int DELTA_T_LSF = 24;

    /** Index of reserved field in word 10. */
    private static final int RESERVED_10 = 25;

    /** Simple constructor.
     * @param words raw words
     */
    SubFrame4D(final int[] words) {

        // create raw container
        super(words, RESERVED_10 + 1);

        // populate container
        setField(ALPHA0,           3, 14,  8, words);
        setField(ALPHA1,           3,  6,  8, words);
        setField(ALPHA2,           4, 22,  8, words);
        setField(ALPHA3,           4, 14,  8, words);
        setField(BETA0,            4,  6,  8, words);
        setField(BETA1,            5, 22,  8, words);
        setField(BETA2,            5, 14,  8, words);
        setField(BETA3,            5,  6,  8, words);
        setField(A1,               6,  6, 24, words);
        setField(A0,               7,  6, 24, 8, 22, 8, words);
        setField(TOT,              8, 14,  8, words);
        setField(WEEK_NUMBER_T,    8,  6,  8, words);
        setField(DELTA_T_LS,       9, 22,  8, words);
        setField(WEEK_NUMBER_LSF,  9, 14,  8, words);
        setField(DN,               9,  6,  8, words);
        setField(DELTA_T_LSF,     10, 22,  8, words);
        setField(RESERVED_10,     10,  8, 14, words);

    }

}
