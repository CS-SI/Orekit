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
    private static final int UPPERCASE_OMEGA_0 = 15;

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
        setField(E,                  3,  6, 16, words);
        setField(TOA,                4, 22,  8, words);
        setField(DELTA_I,            4,  6, 16, words);
        setField(OMEGA_DOT,          5, 14, 16, words);
        setField(SV_HEALTH,          5,  6,  8, words);
        setField(SQRT_A,             6,  6, 24, words);
        setField(UPPERCASE_OMEGA_0,  7,  6, 24, words);
        setField(LOWERCASE_OMEGA,    8,  6, 24, words);
        setField(M0,                 9,  6, 24, words);
        setField(AF0,               10, 22,  8, 10,  8,  3, words);
        setField(AF1,               10, 11, 11, words);

    }

    /** Get the PRN code phase of the SV.
     * @return PRN code phase
     */
    public int getPRN() {
        // sub-frames that contain almanacs use the SV-ID for the PRN
        return getSvId();
    }

    /** Get eccentricity.
     * @return eccentricity
     */
    public double getE() {
        return FastMath.scalb((double) getField(E), -21);
    }

    /** Get Time Of Almanac.
     * @return Time Of Almanac (seconds)
     */
    public int getToaA() {
        return getField(TOA) << 12;
    }

    /** Get Δi.
     * @return Δi (rad)
     */
    public double getDeltai() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(DELTA_I), -19));
    }

    /** Get dot(Ω).
     * @return dot(Ω) (rad/s)
     */
    public double getOmegaDot() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(OMEGA_DOT), -38));
    }

    /** Get SV health.
     * @return SV health
     */
    public int getSvHealth() {
        return getField(SV_HEALTH);
    }

    /** Get √a.
     * @return d√a (√m)
     */
    public double getSqrtA() {
        return FastMath.scalb((double) getField(SQRT_A), -11);
    }

    /** Get Ω₀.
     * @return Ω₀ (rad)
     */
    public double getUppercaseOmega0() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(UPPERCASE_OMEGA_0), -23));
    }

    /** Get ω.
     * @return ω(rad)
     */
    public double getLowercaseOmega() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(LOWERCASE_OMEGA), -23));
    }

    /** Get M₀.
     * @return M₀ (rad)
     */
    public double getM0() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(M0), -23));
    }

    /** Get af₀.
     * @return af₀ (second)
     */
    public double getAF0() {
        return FastMath.scalb((double) getField(AF0), -20);
    }

    /** Get af₁.
     * @return af₁ (second/second)
     */
    public double getAF1() {
        return FastMath.scalb((double) getField(AF1), -38);
    }

}
