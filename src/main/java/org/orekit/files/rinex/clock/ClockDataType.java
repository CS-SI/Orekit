/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.rinex.clock;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

import java.util.HashMap;
import java.util.Map;

/** Clock data type.
 * In case of a DR type, clock data are in the sense of clock value after discontinuity minus prior.
 * In other cases, clock data are in the sense of reported station/satellite clock minus reference clock value. */
public enum ClockDataType {

    /** Data analysis for receiver clocks. Clock Data are */
    AR("AR"),

    /** Data analysis for satellite clocks. */
    AS("AS"),

    /** Calibration measurement for a single GPS receiver. */
    CR("CR"),

    /** Discontinuity measurements for a single GPS receiver. */
    DR("DR"),

    /** Monitor measurements for the broadcast satellite clocks. */
    MS("MS");

    /** Parsing map. */
    private static final Map<String, ClockDataType> KEYS_MAP = new HashMap<>();

    static {
        for (final ClockDataType timeSystem : values()) {
            KEYS_MAP.put(timeSystem.getKey(), timeSystem);
        }
    }

    /** Key for the system. */
    private final String key;

    /** Simple constructor.
     * @param key key letter
     */
    ClockDataType(final String key) {
        this.key = key;
    }

    /** Get the key for the system.
     * @return key for the system
     */
    public String getKey() {
        return key;
    }

    /** Parse a string to get the time system.
     * <p>
     * The string must be the time system.
     * </p>
     * @param s string to parse
     * @return the time system
     * @exception OrekitIllegalArgumentException if the string does not correspond to a time system key
     */
    public static ClockDataType parseClockDataType(final String s)
        throws
        OrekitIllegalArgumentException {
        final ClockDataType clockDataType = KEYS_MAP.get(s);
        if (clockDataType == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_CLOCK_DATA_TYPE, s);
        }
        return clockDataType;
    }

}

