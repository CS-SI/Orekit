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

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

/**
 * Enumerate for satellite system.
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
public enum SatelliteSystem {

    /** User-defined system A.
     * @since 12.0
     */
    USER_DEFINED_A('A', null),

    /** User-defined system B.
     * @since 12.0
     */
    USER_DEFINED_B('B', null),

    /** Beidou system. */
    BEIDOU('C', ObservationTimeScale.BDT),

    /** User-defined system D.
     * @since 12.0
     */
    USER_DEFINED_D('D', null),

    /** Galileo system. */
    GALILEO('E', ObservationTimeScale.GAL),

    /** User-defined system F.
     * @since 12.0
     */
    USER_DEFINED_F('F', null),

    /** GPS system. */
    GPS('G', ObservationTimeScale.GPS),

    /** User-defined system H.
     * @since 12.0
     */
    USER_DEFINED_H('H', null),

    /** Indian Regional Navigation Satellite System system (NavIC). */
    IRNSS('I', ObservationTimeScale.IRN),

    /** Quasi-Zenith Satellite System system. */
    QZSS('J', ObservationTimeScale.QZS),

    /** User-defined system K.
     * @since 12.0
     */
    USER_DEFINED_K('K', null),

    /** User-defined system L.
     * @since 12.0
     */
    USER_DEFINED_L('L', null),

    /** Mixed system. */
    MIXED('M', null),

    /** User-defined system N.
     * @since 12.0
     */
    USER_DEFINED_N('N', null),

    /** User-defined system O.
     * @since 12.0
     */
    USER_DEFINED_O('O', null),

    /** User-defined system P.
     * @since 12.0
     */
    USER_DEFINED_P('P', null),

    /** User-defined system Q.
     * @since 12.0
     */
    USER_DEFINED_Q('Q', null),

    /** GLONASS system. */
    GLONASS('R', ObservationTimeScale.GLO),

    /** SBAS system. */
    SBAS('S', null),

    /** User-defined system T.
     * @since 12.0
     */
    USER_DEFINED_T('T', null),

    /** User-defined system U.
     * @since 12.0
     */
    USER_DEFINED_U('U', null),

    /** User-defined system V.
     * @since 12.0
     */
    USER_DEFINED_V('V', null),

    /** User-defined system W.
     * @since 12.0
     */
    USER_DEFINED_W('W', null),

    /** User-defined system X.
     * @since 12.0
     */
    USER_DEFINED_X('X', null),

    /** User-defined system Y.
     * @since 12.0
     */
    USER_DEFINED_Y('Y', null),

    /** User-defined system Z.
     * @since 12.0
     */
    USER_DEFINED_Z('Z', null);

    /** Parsing map. */
    private static final Map<Character, SatelliteSystem> KEYS_MAP = new HashMap<>();
    static {
        for (final SatelliteSystem satelliteSystem : values()) {
            KEYS_MAP.put(satelliteSystem.getKey(), satelliteSystem);
        }
    }

    /** Key for the system. */
    private final char key;

    /** Observation time scale.
     * @since 12.0
     */
    private final ObservationTimeScale observationTimeScale;

    /** Simple constructor.
     * @param key key letter
     * @param observationTimeScale observation time scale (may be null)
     */
    SatelliteSystem(final char key, final ObservationTimeScale observationTimeScale) {
        this.key                  = key;
        this.observationTimeScale = observationTimeScale;
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

    /** Get observation time scale for satellite system.
     * @return observation time scale, null if there are not
     * @since 12.0
     */
    public ObservationTimeScale getObservationTimeScale() {
        return observationTimeScale;
    }

}
