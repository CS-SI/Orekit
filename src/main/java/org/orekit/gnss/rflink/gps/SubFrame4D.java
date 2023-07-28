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
import org.orekit.gnss.metric.parser.Units;
import org.orekit.utils.units.Unit;

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

    /** Seconds per semi-circle. */
    private static final Unit S_PER_SC = Unit.SECOND.divide("s/sc", Units.SEMI_CIRCLE);

    /** Seconds per semi-circle². */
    private static final Unit S_PER_SC2 = S_PER_SC.divide("s/sc²", Units.SEMI_CIRCLE);

    /** Seconds per semi-circle³. */
    private static final Unit S_PER_SC3 = S_PER_SC2.divide("s/sc³", Units.SEMI_CIRCLE);

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

    /** Get α₀ field.
     * @return α₀ field (second).
     */
    public double getAlpha0() {
        return FastMath.scalb((double) getField(ALPHA0), -30);
    }

    /** Get α₁ field.
     * @return α₁ field (second/rad).
     */
    public double getAlpha1() {
        return S_PER_SC.toSI(FastMath.scalb((double) getField(ALPHA1), -27));
    }

    /** Get α₂ field.
     * @return α₂ field (second/rad²).
     */
    public double getAlpha2() {
        return S_PER_SC2.toSI(FastMath.scalb((double) getField(ALPHA2), -24));
    }

    /** Get α₃ field.
     * @return α₃ field (second/rad³)
     */
    public double getAlpha3() {
        return S_PER_SC3.toSI(FastMath.scalb((double) getField(ALPHA3), -24));
    }

    /** Get β₀ field.
     * @return β₀ field (second)
     */
    public double getBeta0() {
        return FastMath.scalb((double) getField(BETA0), 11);
    }

    /** Get β₁ field.
     * @return β₁ field (second/rad)
     */
    public double getBeta1() {
        return S_PER_SC.toSI(FastMath.scalb((double) getField(BETA1), 14));
    }

    /** Get β₂ field.
     * @return β₂ field (second/rad²)
     */
    public double getBeta2() {
        return S_PER_SC2.toSI(FastMath.scalb((double) getField(BETA2), 16));
    }

    /** Get β₃ field.
     * @return β₃ field (second/rad³)
     */
    public double getBeta3() {
        return S_PER_SC3.toSI(FastMath.scalb((double) getField(BETA3), 16));
    }

    /** Get A₁ field.
     * @return A₁ field (second/second).
     */
    public double getA1() {
        return FastMath.scalb((double) getField(A1), -50);
    }

    /** Get A0 field.
     * @return A0 field (seconds).
     */
    public double getA0() {
        return FastMath.scalb((double) getField(A0), -30);
    }

    /** Get TOT field.
     * @return TOT field. */
    public int getTot() {
        return getField(TOT) << 12;
    }

    /** Get Week Number T field.
     * @return Week Number T field. */
    public int getWeekNumberT() {
        return getField(WEEK_NUMBER_T);
    }

    /** Get ΔtLS field.
     * @return ΔtLS field. */
    public int getDeltaTLs() {
        return getField(DELTA_T_LS);
    }

    /** Get Week Number LSF field.
     * @return Week Number LSF field. */
    public int getWeekNumberLsf() {
        return getField(WEEK_NUMBER_LSF);
    }

    /** Get DN field.
     * @return DN field. */
    public int getDn() {
        return getField(DN);
    }

    /** Get ΔtLSF field.
     * @return ΔtLSF field. */
    public int getDeltaTLsf() {
        return getField(DELTA_T_LSF);
    }

    /** Get reserved field in word 10.
     * @return reserved field in word 10. */
    public int getReserved10() {
        return getField(RESERVED_10);
    }

}
