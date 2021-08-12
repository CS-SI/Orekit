/* Copyright 2002-2021 CS GROUP
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
package org.orekit.gnss;

import java.util.HashMap;
import java.util.Map;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;

/** Enumerate for the time systems used in navigation files.
 *
 * @author Thomas Neidhart
 * @author Evan Ward
 * @author Thomas Paulet
 * @since 11.0
 */
public enum TimeSystem {

    /** Global Positioning System. */
    GPS("GPS"),

    /** GLONASS. */
    GLONASS("GLO"),

    /** GALILEO. */
    GALILEO("GAL"),

    /** International Atomic Time. */
    TAI("TAI"),

    /** Coordinated Universal Time. */
    UTC("UTC"),

    /** Quasi-Zenith System. */
    QZSS("QZS"),

    /** Beidou. */
    BEIDOU("BDS"),

    /** IRNSS. */
    IRNSS("IRN"),

    /** Unknown. */
    UNKNOWN("LCL");

    /** Parsing map. */
    private static final Map<String, TimeSystem> KEYS_MAP = new HashMap<>();
    static {
        for (final TimeSystem timeSystem : values()) {
            KEYS_MAP.put(timeSystem.getKey(), timeSystem);
        }
    }

    /** Key for the system. */
    private final String key;

    /** Simple constructor.
     * @param key key letter
     */
    TimeSystem(final String key) {
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
    public static TimeSystem parseTimeSystem(final String s)
        throws OrekitIllegalArgumentException {
        final TimeSystem timeSystem = KEYS_MAP.get(s);
        if (timeSystem == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_TIME_SYSTEM, s);
        }
        return timeSystem;
    }

    /** Get the time scale corresponding to time system.
     * @param timeScales the set of time scales to use
     * @return the time scale corresponding to time system in the set of time scales
     */
    public TimeScale getTimeScale(final TimeScales timeScales) {

        TimeScale timeScale = null;
        switch (this) {
            case GPS:
                timeScale = timeScales.getGPS();
                break;

            case GALILEO:
                timeScale = timeScales.getGST();
                break;

            case GLONASS:
                timeScale = timeScales.getGLONASS();
                break;

            case QZSS:
                timeScale = timeScales.getQZSS();
                break;

            case TAI:
                timeScale = timeScales.getTAI();
                break;

            case UTC:
                timeScale = timeScales.getUTC();
                break;

            case BEIDOU:
                timeScale = timeScales.getBDT();
                break;

            case IRNSS:
                timeScale = timeScales.getIRNSS();
                break;

            // Default value is GPS time scale, even in unknown case.
            default:
                timeScale = timeScales.getGPS();
                break;
        }

        return timeScale;
    }
}
