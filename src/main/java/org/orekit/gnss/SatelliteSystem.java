/* Copyright 2002-2023 CS GROUP
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
import java.util.function.Function;

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
    GPS('G', timescales -> timescales.getGPS()),

    /** GLONASS system. */
    GLONASS('R', timescales -> timescales.getGLONASS()),

    /** Galileo system. */
    GALILEO('E', timescales -> timescales.getGST()),

    /** Beidou system. */
    BEIDOU('C', timescales -> timescales.getBDT()),

    /** Quasi-Zenith Satellite System system. */
    QZSS('J', timescales -> timescales.getQZSS()),

    /** Indian Regional Navigation Satellite System system. */
    IRNSS('I', timescales -> timescales.getIRNSS()),

    /** SBAS system. */
    SBAS('S', timescales -> null),

    /** Mixed system. */
    MIXED('M', timescales -> null);

    /** Parsing map. */
    private static final Map<Character, SatelliteSystem> KEYS_MAP = new HashMap<>();
    static {
        for (final SatelliteSystem satelliteSystem : values()) {
            KEYS_MAP.put(satelliteSystem.getKey(), satelliteSystem);
        }
    }

    /** Key for the system. */
    private final char key;

    /** Provider for default time scale.
     * @since 12.0
     */
    private final Function<TimeScales, TimeScale> defaultTimeScaleProvider;

    /** Simple constructor.
     * @param key key letter
     * @param defaultTimeScaleProvider provider for default time scale
     */
    SatelliteSystem(final char key, final Function<TimeScales, TimeScale> defaultTimeScaleProvider) {
        this.key                      = key;
        this.defaultTimeScaleProvider = defaultTimeScaleProvider;
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

    /** Parse a string to get the satellite system.
     * <p>
     * The string first character must be the satellite system, or empty to get GPS as default
     * </p>
     * @param s string to parse
     * @return the satellite system
     * @since 12.0
     */
    public static SatelliteSystem parseSatelliteSystemWithGPSDefault(final String s) {
        return s.isEmpty() ? SatelliteSystem.GPS : parseSatelliteSystem(s);
    }

    /** Get default time scale for satellite system.
     * @param timeScales the set of timeScales to use
     * @return the default time scale among the given set matching to satellite system,
     *         null if there are not
     */
    public TimeScale getDefaultTimeSystem(final TimeScales timeScales) {
        return defaultTimeScaleProvider.apply(timeScales);
    }

}
