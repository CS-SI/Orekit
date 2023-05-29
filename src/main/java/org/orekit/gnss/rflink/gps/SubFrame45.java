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
 * Base container for sub-frames 4 and 5.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SubFrame45 extends SubFrame {

    /** Index of data ID field. */
    private static final int DATA_ID = 7;

    /** Index of SV ID field. */
    private static final int SV_ID = 8;

    /** Simple constructor.
     * @param words raw words
     * @param nbFields number of fields in the sub-frame
     * (including TLM and HOW data fields, excluding non-information and parity)
     */
    SubFrame45(final int[] words, final int nbFields) {

        // create raw container
        super(words, nbFields);

        // populate container
        setField(DATA_ID, 3, 28, 2, words);
        setField(SV_ID,   3, 22, 6, words);

    }

    /** Get data ID.
     * @return data ID
     */
    public int getDataId() {
        return getField(DATA_ID);
    }

    /** Get SV (page) ID.
     * @return SV (page) ID
     */
    public int getSvId() {
        return getField(SV_ID);
    }

}
