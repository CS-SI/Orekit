/* Copyright 2002-2024 Luc Maisonobe
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

import java.util.Locale;

/** Container for satellite system and PRN.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SatInSystem {

    /** Value representing all PRNs in the system. */
    public static final int ANY_PRN = -1;

    /** PRN shift for SBAS system. */
    private static final int SBAS_SHIFT = 100;

    /** PRN shift for QZSS system. */
    private static final int QZSS_SHIFT = 192;

    /** Satellite system. */
    private final SatelliteSystem system;

    /** PRN number. */
    private final int prn;

    /** Simple constructor.
     * @param system satellite system
     * @param prn Pseudo Random Number or {@link #ANY_PRN} to represent any satellite in the system
     */
    public SatInSystem(final SatelliteSystem system, final int prn) {
        this.system = system;
        this.prn    = prn;
    }

    /** Simple constructor.
     * <p>
     * The RINEX 3 characters code starts with a letter representing the
     * {@link SatelliteSystem#getKey() satellite system key} followed
     * by a 2 digits integer which represents either
     * </p>
     * <ul>
     *   <li>the Pseudo Random Number in the general case</li>
     *   <li>the Pseudo Random Number minus 100 for {@link SatelliteSystem#SBAS}</li>
     *   <li>the Pseudo Random Number minus 192 for {@link SatelliteSystem#QZSS}</li>
     * </ul>
     * <p>
     * if only the letter is present, then prn is set to {@link #ANY_PRN}
     * </p>
     * @param rinexCode RINEX 3 characters code
     * @since 13.0
     */
    public SatInSystem(final String rinexCode) {

        // parse system
        this.system = SatelliteSystem.parseSatelliteSystem(rinexCode.substring(0, 1));

        // parse PRN
        final String trimmed = rinexCode.substring(1).trim();
        if (trimmed.isEmpty()) {
            this.prn = ANY_PRN;
        } else {
            final int index = Integer.parseInt(trimmed);
            if (system == SatelliteSystem.SBAS) {
                this.prn = index + SBAS_SHIFT;
            } else if (system == SatelliteSystem.QZSS) {
                this.prn = index + QZSS_SHIFT;
            } else {
                this.prn = index;
            }
        }

    }

    /** Get the system this satellite belongs to.
     * @return system this satellite belongs to
     */
    public SatelliteSystem getSystem() {
        return system;
    }

    /** Get the Pseudo Random Number of the satellite.
     * @return Pseudo Random Number of the satellite, or {@link #ANY_PRN} to represent
     * any PRN in the system
     */
    public int getPRN() {
        return prn;
    }

    /** Get a 2-digits Pseudo Random Number for RINEX files.
     * @return 2-digits Pseudo Random Number for RINEX files
     */
    public int getTwoDigitsRinexPRN() {
        return system == SatelliteSystem.SBAS ?
               prn - SBAS_SHIFT :
               (system == SatelliteSystem.QZSS ? prn - QZSS_SHIFT : prn);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return prn < 0 ?
               String.format(Locale.US, "%c  ", system.getKey()) :
               String.format(Locale.US, "%c%02d", system.getKey(), getTwoDigitsRinexPRN());
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof SatInSystem) {
            final SatInSystem other = (SatInSystem) object;
            return getSystem().equals(other.getSystem()) && getPRN() == other.getPRN();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Character.hashCode(getSystem().getKey()) ^ getPRN();
    }

}
