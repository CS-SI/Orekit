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

/**
 * Enumerate for satellite system.
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
public enum SatelliteSystem {

    /** GPS system. */
    GPS('G'),

    /** GLONASS system. */
    GLONASS('R'),

    /** Galileo system. */
    GALILEO('E'),

    /** Beidou system. */
    BEIDOU('C'),

    /** Quasi-Zenith Satellite System system. */
    QZSS('J'),

    /** Indian Regional Navigation Satellite System system. */
    IRNSS('I'),

    /** SBAS system. */
    SBAS('S'),

    /** Mixed system. */
    MIXED('M');

    /** Parsing map. */
    private static final Map<Character, SatelliteSystem> KEYS_MAP = new HashMap<>();
    static {
        for (final SatelliteSystem satelliteSystem : values()) {
            KEYS_MAP.put(satelliteSystem.getKey(), satelliteSystem);
        }
    }

    /** Key for the system. */
    private final char key;

    /** Simple constructor.
     * @param key key letter
     */
    SatelliteSystem(final char key) {
        this.key = key;
    }

    /** Get the key for the system.
     * @return key for the system
     */
    public char getKey() {
        return key;
    }

    /** Parse a string to get the satellite system.
     * <p>
     * The string first character must be the satellite system.
     * </p>
     * @param s string to parse
     * @return the satellite system
     * @exception OrekitIllegalArgumentException if the string does not correspond to a satellite system key
     */
    public static SatelliteSystem parseSatelliteSystem(final String s)
        throws OrekitIllegalArgumentException {
        final SatelliteSystem satelliteSystem = KEYS_MAP.get(s.charAt(0));
        if (satelliteSystem == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_SATELLITE_SYSTEM, s.charAt(0));
        }
        return satelliteSystem;
    }

    /** Get default time scale for satellite system.
     * @param timeScales the set of timeScales to use
     * @return the default time scale among the given set matching to satellite system,
     *         null if there are not
     */
    public TimeScale getDefaultTimeSystem(final TimeScales timeScales) {

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

            case BEIDOU:
                timeScale = timeScales.getBDT();
                break;

            case IRNSS:
                timeScale = timeScales.getIRNSS();
                break;

            // Default value is null
            default:
                timeScale = null;
                break;
        }

        return timeScale;
    }

}
