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
package org.orekit.gnss;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Enumerate for the time systems used in navigation files.
 *
 * @author Thomas Neidhart
 * @author Evan Ward
 * @author Thomas Paulet
 * @since 11.0
 */
public enum PredefinedTimeSystem implements TimeSystem {

    /** Global Positioning System. */
    GPS("GPS", "GP", "G", TimeScales::getGPS),

    /** GLONASS. */
    GLONASS("GLO", "GL", "R", TimeScales::getGLONASS),

    /** GALILEO. */
    GALILEO("GAL", "GA", "E", TimeScales::getGST),

    /** International Atomic Time. */
    TAI("TAI", null, null, TimeScales::getTAI),

    /** Coordinated Universal Time. */
    UTC("UTC", "UT", null, TimeScales::getUTC),

    /** Quasi-Zenith System. */
    QZSS("QZS", "QZ", "J", TimeScales::getQZSS),

    /** Beidou. */
    BEIDOU("BDT", "BD", "C", TimeScales::getBDT),

    /** NavIC. */
    NAVIC("IRN", "IR", "I", TimeScales::getNavIC),

    /** SBAS.
     * @since 12.0
     */
    SBAS(null, "SB", "S", TimeScales::getUTC),

    /** GMT (should only be used in RUN BY / DATE entries).
     * @since 12.0
     */
    GMT("GMT", null, null, TimeScales::getUTC),

    /** Unknown (should only be used in RUN BY / DATE entries). */
    UNKNOWN("LCL", null, null, TimeScales::getGPS);

    /** Parsing key map. */
    private static final Map<String, PredefinedTimeSystem> KEYS_MAP = new HashMap<>();

    /** Parsing two letters code map.
     * @since 12.0
     */
    private static final Map<String, PredefinedTimeSystem> TLC_MAP = new HashMap<>();

    /** Parsing one letters code map.
     * @since 12.0
     */
    private static final Map<String, PredefinedTimeSystem> OLC_MAP = new HashMap<>();

    static {
        for (final PredefinedTimeSystem timeSystem : values()) {
            if (timeSystem.key != null) {
                KEYS_MAP.put(timeSystem.key, timeSystem);
            }
            if (timeSystem.twoLettersCode != null) {
                TLC_MAP.put(timeSystem.twoLettersCode, timeSystem);
            }
            if (timeSystem.oneLetterCode != null) {
                OLC_MAP.put(timeSystem.oneLetterCode, timeSystem);
            }
        }
    }

    /** Key for the system. */
    private final String key;

    /** Two-letters code.
     * @since 12.0
     */
    private final String twoLettersCode;

    /** One-letter code.
     * @since 12.0
     */
    private final String oneLetterCode;

    /** Time scale provider.
     * @since 12.0
     */
    private final Function<TimeScales, TimeScale> timeScaleProvider;

    /** Simple constructor.
     * @param key key letter (may be null)
     * @param twoLettersCode two letters code (may be null)
     * @param oneLetterCode one letter code (may be null)
     * @param timeScaleProvider time scale provider
     */
    PredefinedTimeSystem(final String key, final String twoLettersCode, final String oneLetterCode,
                         final Function<TimeScales, TimeScale> timeScaleProvider) {
        this.key               = key;
        this.twoLettersCode    = twoLettersCode;
        this.oneLetterCode     = oneLetterCode;
        this.timeScaleProvider = timeScaleProvider;
    }

    /** {@inheritDoc} */
    @Override
    public String getKey() {
        return key;
    }

    /** {@inheritDoc} */
    @Override
    public String getTwoLettersCode() {
        return twoLettersCode;
    }

    /** {@inheritDoc} */
    @Override
    public String getOneLetterCode() {
        return oneLetterCode;
    }

    /** {@inheritDoc} */
    @Override
    public TimeScale getTimeScale(final TimeScales timeScales) {
        return timeScaleProvider.apply(timeScales);
    }

    /** Parse a string to get the time system.
     * <p>
     * The string must be the time system.
     * </p>
     * @param s string to parse
     * @return the time system
     * @exception OrekitIllegalArgumentException if the string does not correspond to a time system key
     */
    public static PredefinedTimeSystem parseTimeSystem(final String s)
        throws OrekitIllegalArgumentException {
        final PredefinedTimeSystem timeSystem = KEYS_MAP.get(s);
        if (timeSystem == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_TIME_SYSTEM, s);
        }
        return timeSystem;
    }

    /** Parse a string to get the time system.
     * <p>
     * The string must be the two letters code of the time system.
     * </p>
     * @param code string to parse
     * @return the time system
     * @exception OrekitIllegalArgumentException if the string does not correspond to a time system key
     */
    public static PredefinedTimeSystem parseTwoLettersCode(final String code)
        throws OrekitIllegalArgumentException {
        final PredefinedTimeSystem timeSystem = TLC_MAP.get(code);
        if (timeSystem == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_TIME_SYSTEM, code);
        }
        return timeSystem;
    }

    /** Parse a string to get the time system.
     * <p>
     * The string must be the one letters code of the time system.
     * The one letter code is the RINEX GNSS system flag.
     * </p>
     * @param code string to parse
     * @return the time system
     * @exception OrekitIllegalArgumentException if the string does not correspond to a time system key
     */
    public static PredefinedTimeSystem parseOneLetterCode(final String code)
        throws OrekitIllegalArgumentException {
        final PredefinedTimeSystem timeSystem = OLC_MAP.get(code);
        if (timeSystem == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_TIME_SYSTEM, code);
        }
        return timeSystem;
    }

}
