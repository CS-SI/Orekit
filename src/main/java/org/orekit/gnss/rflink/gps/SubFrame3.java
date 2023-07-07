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
 * Container for sub-frames 3.
 * <p>
 * Table 20-1, sheet 3 and table 40-1, sheet 3 in
 * <a href="https://navcen.uscg.gov/sites/default/files/pdf/gps/IS-GPS-200N.pdf">NAVSTAR
 * GPS Space Segment/Navigation User Segment Interface</a>, IS-GPS-200N, 22 Aug 2022
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SubFrame3 extends SubFrame {

    /** Index of Cic field. */
    private static final int CIC = 7;

    /** Index of Ω₀ field. */
    private static final int UPPERCASE_OMEGA_0 = 8;

    /** Index of Cis field. */
    private static final int CIS = 9;

    /** Index of i₀ field. */
    private static final int I0 = 10;

    /** Index of Crc field. */
    private static final int CRC = 11;

    /** Index of ω field. */
    private static final int LOWERCASE_OMEGA = 12;

    /** Index of dot(Ω) field. */
    private static final int OMEGA_DOT = 13;

    /** Index of IODE field. */
    private static final int IODE = 14;

    /** Index of dot(i) field. */
    private static final int I_DOT = 15;

    /** Simple constructor.
     * @param words raw words
     */
    SubFrame3(final int[] words) {

        // create raw container
        super(words, I_DOT + 1);

        // populate container
        setField(CIC,                   3, 14, 16, words);
        setField(UPPERCASE_OMEGA_0,     3,  6,  8, 4,  6, 24, words);
        setField(CIS,                   5, 14, 16, words);
        setField(I0,                    5,  6,  8, 6,  6, 24, words);
        setField(CRC,                   7, 14, 16, words);
        setField(LOWERCASE_OMEGA,       7,  6,  8, 8,  6, 24, words);
        setField(OMEGA_DOT,             9,  6, 24, words);
        setField(IODE,                 10, 22,  8, words);
        setField(I_DOT,                10,  8, 14, words);

    }

    /** Get Cic.
     * @return Cic (rad)
     */
    public double getCic() {
        return FastMath.scalb((double) getField(CIC), -29);
    }

    /** Get Ω₀.
     * @return Ω₀ (rad)
     */
    public double getUppercaseOmega0() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(UPPERCASE_OMEGA_0), -31));
    }

    /** Get Cis.
     * @return Cis (rad)
     */
    public double getCis() {
        return FastMath.scalb((double) getField(CIS), -29);
    }

    /** Get i₀.
     * @return i₀ (rad)
     */
    public double getI0() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(I0), -31));
    }

    /** Get Crc.
     * @return Crc (rad)
     */
    public double getCrc() {
        return FastMath.scalb((double) getField(CRC), -5);
    }

    /** Get ω.
     * @return ω(rad)
     */
    public double getLowercaseOmega() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(LOWERCASE_OMEGA), -31));
    }

    /** Get dot(Ω).
     * @return dot(Ω) (rad/s)
     */
    public double getOmegaDot() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(OMEGA_DOT), -43));
    }

    /** Get Issue Of Data (ephemeris).
     * @return Issue Of Data (ephemeris)
     */
    public int getIODE() {
        return getField(IODE);
    }

    /** Get dot(i).
     * @return dot(i) (rad/s)
     */
    public double getIDot() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(I_DOT), -43));
    }

}
