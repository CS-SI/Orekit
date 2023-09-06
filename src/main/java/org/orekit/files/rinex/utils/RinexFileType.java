/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.rinex.utils;

import java.util.HashMap;
import java.util.Map;

/** Enumerate for RINEX files types.
 * @since 12.0
 */
public enum RinexFileType {

    /** Rinex Observation. */
    OBSERVATION("O"),

    /** Rinex navigation (G is for Glonass navigation, in Rinex 2.X). */
    NAVIGATION("N", "G");

    /** Parsing map. */
    private static final Map<String, RinexFileType> KEYS_MAP = new HashMap<>();
    static {
        for (final RinexFileType type : values()) {
            for (final String key : type.keys) {
                KEYS_MAP.put(key, type);
            }
        }
    }

    /** Key of the file type. */
    private final String[] keys;

    /** Simple constructor.
     * @param keys keys of the file type
     */
    RinexFileType(final String... keys) {
        this.keys = keys.clone();
    }

    /** Parse the string to get the type.
     * @param s string to parse (must correspond to a one-character key)
     * @return the type corresponding to the string
     */
    public static RinexFileType parseRinexFileType(final String s) {
        return KEYS_MAP.get(s);
    }

}
