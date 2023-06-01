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
 * Container for sub-frames 4, pages 10, 14, 15, 17.
 * <p>
 * Table 40-1, sheet 11 in
 * <a href="https://navcen.uscg.gov/sites/default/files/pdf/gps/IS-GPS-200N.pdf">NAVSTAR
 * GPS Space Segment/Navigation User Segment Interface</a>, IS-GPS-200N, 22 Aug 2022
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SubFrame4B extends SubFrame45 {

    /** Index of reserved field in word 3. */
    private static final int RESERVED_3 = 9;

    /** Index of reserved field in word 4. */
    private static final int RESERVED_4 = 10;

    /** Index of reserved field in word 5. */
    private static final int RESERVED_5 = 11;

    /** Index of reserved field in word 6. */
    private static final int RESERVED_6 = 12;

    /** Index of reserved field in word 7. */
    private static final int RESERVED_7 = 13;

    /** Index of reserved field in word 8. */
    private static final int RESERVED_8 = 14;

    /** Index of reserved field in word 9. */
    private static final int RESERVED_9 = 15;

    /** Index of reserved field in word 10. */
    private static final int RESERVED_10 = 16;

    /** Simple constructor.
     * @param words raw words
     */
    SubFrame4B(final int[] words) {

        // create raw container
        super(words, RESERVED_10 + 1);

        // populate container
        setField(RESERVED_3,    3,  6, 16, words);
        setField(RESERVED_4,    4,  6, 24, words);
        setField(RESERVED_5,    5,  6, 24, words);
        setField(RESERVED_6,    6,  6, 24, words);
        setField(RESERVED_7,    7,  6, 24, words);
        setField(RESERVED_8,    8,  6, 24, words);
        setField(RESERVED_9,    9,  6, 24, words);
        setField(RESERVED_10,  10,  8, 22, words);

    }

    /** Get the reserved field in word 3.
     * @return reserved field in word 3
     */
    public int getReserved03() {
        return getField(RESERVED_3);
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

    /** Get the reserved field in word 8.
     * @return reserved field in word 8
     */
    public int getReserved08() {
        return getField(RESERVED_8);
    }

    /** Get the reserved field in word 9.
     * @return reserved field  in word 9
     */
    public int getReserved09() {
        return getField(RESERVED_9);
    }

    /** Get the reserved field in word 10.
     * @return reserved field in word 10
     */
    public int getReserved10() {
        return getField(RESERVED_10);
    }

}
