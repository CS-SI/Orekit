/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.files.rinex.navigation;

import java.util.HashMap;
import java.util.Map;

/** Enumerate for region code.
 * @see IonosphereKlobucharMessage
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum RegionCode {

    /** Wide Area. */
    WIDE_AREA(0, "WIDE"),

    /** Japan area (for QZSS only). */
    JAPAN(1, "JAPN");

    /** Parsing map.
     * @since 14.0
     */
    private static final Map<Integer, RegionCode> INTEGERS_MAP = new HashMap<>();

    /** Parsing map.
     * @since 14.0
     */
    private static final Map<String, RegionCode> STRINGS_MAP   = new HashMap<>();

    static {
        for (final RegionCode regionCode : values()) {
            INTEGERS_MAP.put(regionCode.getIntegerId(), regionCode);
            STRINGS_MAP.put(regionCode.getStringId(),   regionCode);
        }
    }

    /** Integer identifier for region code.
     * @since 14.0
     */
    private final int intId;

    /** String identifier for region code.
     * @since 14.0
     */
    private final String stringId;

    /** Simple constructor.
     * @param intId integer identifier for region code
     * @param stringId string identifier for region code
     * @since 14.0
     */
    RegionCode(final int intId, final String stringId) {
        this.intId    = intId;
        this.stringId = stringId;
    }

    /** Get the integer identifier.
     * @return integer identifier
     * @since 14.0
     */
    public int getIntegerId() {
        return intId;
    }

    /** Get the string identifier.
     * @return string identifier
     * @since 14.0
     */
    public String getStringId() {
        return stringId;
    }

    /** Parse the integer to get the region code.
     * @param i integer to parse
     * @return the region code corresponding to the string
     * @exception IllegalArgumentException if the string does not correspond to a region code
     * @since 14.0
     */
    public static RegionCode parseRegionCode(final int i) {
        return INTEGERS_MAP.get(i);
    }

    /** Parse the string to get the region code.
     * @param s string to parse
     * @return the region code corresponding to the string
     * @exception IllegalArgumentException if the string does not correspond to a region code
     * @since 14.0
     */
    public static RegionCode parseRegionCode(final String s) {
        return STRINGS_MAP.get(s);
    }

}
