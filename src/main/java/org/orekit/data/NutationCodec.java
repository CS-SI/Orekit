/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.data;

import org.orekit.errors.OrekitInternalError;

/** Encoder/decoder for Delaunay and planetary multipliers keys.
 * <p>
 * As Delaunay and planetary multipliers often have a lot of zeroes
 * and the non-zero multipliers are in a small range, it makes sense
 * to encode them in a compact representation that can be used as
 * a key in hash tables. This class does the encoding/decoding of
 * such keys.
 * </p>
 * <p>
 * The encoding scheme is as follows, numbering bits from
 * 0 for least significant bit to 63 for most significant bit:
 * </p>
 * <ul>
 *   <li>bits  0 to 14: mask for the 15 coefficients</li>
 *   <li>bits 15 to 63: split into 7 slots each 7 bits long and
 *   each encoding a coefficient ci + 64, where ci is the i-th
 *   non-zero coefficient</li>
 * </ul>
 * <p>
 * This scheme allows to encode 7 non-zero integers between -64 to +63 among 15.
 * As the current Poisson series used in Orekit have at most 6 non-zero coefficients
 * and all the coefficients are between -21 and +20, we have some extension margin.
 * </p>
 */
class NutationCodec {

    /** Current multiplier flag bit. */
    private long flag;

    /** Current coefficient shift. */
    private int shift;

    /** Current key value. */
    private long key;

    /** Simple constructor.
     * @param key key
     */
    private NutationCodec(final long key) {
        flag  = 0x1l;
        shift = 15;
        this.key = key;
    }

    /** Get the key value.
     * @return key value
     */
    public long getKey() {
        return key;
    }

    /** Encode one more multiplier in the key.
     * @param multiplier multiplier to encode
     */
    private void addMultiplier(final int multiplier) {

        if (multiplier != 0) {
            // this is a coefficient we want to store
            key = key | flag;
            if (shift > 57 || multiplier < -64 || multiplier > 63) {
                // this should never happen, we exceed the encoding capacity
                throw new OrekitInternalError(null);
            }
            key    = key | (((multiplier + 64l) & 0x7Fl) << shift);
            shift += 7;
        }

        // update bit mask
        flag = flag << 1;

    }

    /** Decode one multiplier from the key.
     * @return decoded multiplier
     */
    private int nextMultiplier() {
        final int multiplier;
        if ((key & flag) == 0x0l) {
            // no values are stored for this coefficient, it is 0
            multiplier = 0;
        } else {
            // there is a stored value for this coefficient
            multiplier = ((int) ((key >>> shift) & 0x7Fl)) - 64;
            shift += 7;
        }

        // update bit mask
        flag = flag << 1;

        return multiplier;

    }

    /** Encode all tide, Delaunay and planetary multipliers into one key.
     * @param multipliers multipliers to encode
     * @return a key merging all multipliers as one long integer
     */
    public static long encode(final int ... multipliers) {
        final NutationCodec encoder = new NutationCodec(0x0l);
        for (final int multiplier : multipliers) {
            encoder.addMultiplier(multiplier);
        }
        return encoder.getKey();
    }

    /** Decode a key into all tide, Delaunay and planetary multipliers.
     * @param key key merging all multipliers as one long integer
     * @return all tide, Delaunay and planetary multiplers, in the order
     * cGamma, cL, cLPrime, cF, cD, cOmega, cMe, cVe, cE, cMa, cJu, cSa, cUr, cNe, cPa
     */
    public static int[] decode(final long key) {
        final int[] multipliers = new int[15];
        final NutationCodec decoder = new NutationCodec(key);
        for (int i = 0; i < multipliers.length; ++i) {
            multipliers[i] = decoder.nextMultiplier();
        }
        return multipliers;
    }

}
