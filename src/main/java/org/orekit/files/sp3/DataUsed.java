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
package org.orekit.files.sp3;

import java.util.HashMap;
import java.util.Map;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

/** Enumerate for data used.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum DataUsed {

    /** Undifferenciated carrier phase. */
    UNDIFFERENTIATED_CARRIER_PHASE("u"),

    /** Change in undifferenciated carrier phase. */
    CHANGE_IN_UNDIFFERENTIATED_CARRIER_PHASE("du"),

    /** 2-receiver/1-satellite carrier phase. */
    TWO_RECEIVER_ONE_SATELLITE_CARRIER_PHASE("s"),

    /** Change in 2-receiver/1-satellite carrier phase. */
    CHANGE_IN_TWO_RECEIVER_ONE_SATELLITE_CARRIER_PHASE("ds"),

    /** 2-receiver/2-satellite carrier phase. */
    TWO_RECEIVER_TWO_SATELLITE_CARRIER_PHASE("d"),

    /** Change in 2-receiver/2-satellite carrier phase. */
    CHANGE_IN_TWO_RECEIVER_TWO_SATELLITE_CARRIER_PHASE("dd"),

    /** Undifferenciated code phase. */
    UNDIFFERENTIATED_CODE_PHASE("U"),

    /** Change in undifferenciated code phase. */
    CHANGE_IN_UNDIFFERENTIATED_CODE_PHASE("dU"),

    /** 2-receiver/1-satellite code phase. */
    TWO_RECEIVER_ONE_SATELLITE_CODE_PHASE("S"),

    /** Change in 2-receiver/1-satellite code phase. */
    CHANGE_IN_TWO_RECEIVER_ONE_SATELLITE_CODE_PHASE("dS"),

    /** 2-receiver/2-satellite code phase. */
    TWO_RECEIVER_TWO_SATELLITE_CODE_PHASE("D"),

    /** Change in 2-receiver/2-satellite code phase. */
    CHANGE_IN_TWO_RECEIVER_TWO_SATELLITE_CODE_PHASE("dD"),

    /** Satellite Laser Ranging. */
    SATELLITE_LASER_RANGING("SLR"),

    /** Mixed data. */
    MIXED("mixed"),

    /** Orbit data. */
    ORBIT("ORBIT");

    /** Numbers map. */
    private static final Map<String, DataUsed> MAP = new HashMap<>();
    static {
        for (final DataUsed dataUsed : values()) {
            MAP.put(dataUsed.getKey(), dataUsed);
        }
    }

    /** Key for the data used. */
    private final String key;

    /** Simple constructor.
     * @param key for the data used
     */
    DataUsed(final String key) {
        this.key = key;
    }

    /** Get the key for the data used.
     * @return key for the data used
     */
    public String getKey() {
        return key;
    }

    /** Parse the string to get the data used.
     * @param s string to parse
     * @param fileName file name to generate the error message
     * @param version format version
     * @return the data used corresponding to the string
     * @exception IllegalArgumentException if the string does not correspond to a data used
     */
    public static DataUsed parse(final String s, final String fileName, final char version) {
        final DataUsed dataUsed = MAP.get(s);
        if (dataUsed == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.SP3_INVALID_HEADER_ENTRY, "data used", s, fileName, version);
        }
        return dataUsed;
    }

}
