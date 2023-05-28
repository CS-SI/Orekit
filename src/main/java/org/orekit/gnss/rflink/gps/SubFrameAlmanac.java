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
 * Container for sub-frames 5, page 1-24.
 * <p>
 * Table 20-1, sheet 4 and table 40-1, sheet 4 in
 * <a href="https://navcen.uscg.gov/sites/default/files/pdf/gps/IS-GPS-200N.pdf">NAVSTAR
 * GPS Space Segment/Navigation User Segment Interface</a>, IS-GPS-200N, 22 Aug 2022
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SubFrameAlmanac extends SubFrame45 {

    /** Index of e field. */
    private static final int E = 9;

    /** Index of TOA field. */
    private static final int TOA = 10;

    /** Index of δi field. */
    private static final int DELTA_I = 11;

    /** Index of dot(Ω) field. */
    private static final int OMEGA_DOT = 12;

    /** Index of SV health field. */
    private static final int SV_HEALTH = 13;

    /** Index of √a field. */
    private static final int SQRT_A = 14;

    /** Index of Ω₀ field. */
    private static final int UPPERCASE_OMEGA = 15;

    /** Index of ω field. */
    private static final int LOWERCASE_OMEGA = 16;

    /** Index of M₀ field. */
    private static final int M0 = 17;

    /** Index of AF0 field. */
    private static final int AF0 = 18;

    /** Index of AF1 field. */
    private static final int AF1 = 19;

    /** Simple constructor.
     * @param words raw words
     */
    SubFrameAlmanac(final int[] words) {

        // create raw container
        super(words, AF1 + 1);

        // populate container
        setField(E,                3,  6, 16);
        setField(TOA,              4, 22,  8);
        setField(DELTA_I,          4,  6, 16);
        setField(OMEGA_DOT,        5, 14, 16);
        setField(SV_HEALTH,        5,  6,  8);
        setField(SQRT_A,           6,  6, 24);
        setField(UPPERCASE_OMEGA,  7,  6, 24);
        setField(LOWERCASE_OMEGA,  8,  6, 24);
        setField(M0,               9,  6, 24);
        setField(AF0,             10, 22,  8, 10,  8,  3);
        setField(AF1,             10, 11, 11);

    }

    /** Get the PRN code phase of the SV.
     * @return PRN code phase
     */
    public int getPRN() {
        // sub-frames that contain almanacs use the SV-ID for the PRN
        return getSvId();
    }

}
