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
 * Container for sub-frames 2.
 * <p>
 * Table 20-1, sheet 2 and table 40-1, sheet 2 in
 * <a href="https://navcen.uscg.gov/sites/default/files/pdf/gps/IS-GPS-200N.pdf">NAVSTAR
 * GPS Space Segment/Navigation User Segment Interface</a>, IS-GPS-200N, 22 Aug 2022
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SubFrame2 extends SubFrame {

    /** Index of IODE field. */
    private static final int IODE = 7;

    /** Index of Crs field. */
    private static final int CRS = 8;

    /** Index of Δn field. */
    private static final int DELTA_N = 9;

    /** Index of MO field. */
    private static final int M0 = 10;

    /** Index of Cuc field. */
    private static final int CUC = 11;

    /** Index of e field. */
    private static final int E = 12;

    /** Index of Cus field. */
    private static final int CUS = 13;

    /** Index of √a field. */
    private static final int SQRT_A = 14;

    /** Index of TOE field. */
    private static final int TOE = 15;

    /** Index of it interval field. */
    private static final int FIT_INTERVAL = 16;

    /** Index of AODO field. */
    private static final int AODO = 17;

    /** Simple constructor.
     * @param words raw words
     */
    SubFrame2(final int[] words) {

        // create raw container
        super(words, AODO + 1);

        // populate container
        setField(IODE,          3, 22,  8, words);
        setField(CRS,           3,  6, 16, words);
        setField(DELTA_N,       4, 14, 16, words);
        setField(M0,            4,  6,  8, 5,  6, 24, words);
        setField(CUC,           6, 14, 16, words);
        setField(E,             6,  6,  8, 7,  6, 24, words);
        setField(CUS,           8, 14, 16, words);
        setField(SQRT_A,        8,  6,  8, 9,  6, 24, words);
        setField(TOE,          10, 14, 16, words);
        setField(FIT_INTERVAL, 10, 13,  1, words);
        setField(AODO,         10,  8,  5, words);

    }

    /** Get Issue Of Data (ephemeris).
     * @return Issue Of Data (ephemeris)
     */
    public int getIODE() {
        return getField(IODE);
    }

    /** Get Crs.
     * @return crs (m)
     */
    public double getCrs() {
        return FastMath.scalb((double) getField(CRS), -5);
    }

    /** Get Δn.
     * @return Δn (rad/s)
     */
    public double getDeltaN() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(DELTA_N), -43));
    }

    /** Get M₀.
     * @return M₀ (rad)
     */
    public double getM0() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(M0), -31));
    }

    /** Get Cuc.
     * @return Cuc (rad)
     */
    public double getCuc() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(CUC), -29));
    }

    /** Get e.
     * @return e
     */
    public double getE() {
        return FastMath.scalb((double) getField(E), -33);
    }

    /** Get Cus.
     * @return Cus (rad)
     */
    public double getCus() {
        return Units.SEMI_CIRCLE.toSI(FastMath.scalb((double) getField(CUS), -29));
    }

    /** Get √a.
     * @return d√a (√m)
     */
    public double getSqrtA() {
        return FastMath.scalb((double) getField(SQRT_A), -19);
    }

    /** Get Time Of Ephemeris.
     * @return Time Of Ephemeris
     */
    public int getToe() {
        return getField(TOE) << 4;
    }

    /** Get fit interval.
     * @return fit interval
     */
    public int getFitInterval() {
        return getField(FIT_INTERVAL);
    }

    /** Get Age Of data Offset.
     * @return Age Of Data Offset (s)
     */
    public int getAODO() {
        return getField(AODO) * 900;
    }

}
