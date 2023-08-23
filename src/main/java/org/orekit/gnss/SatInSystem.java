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
package org.orekit.gnss;

/** Container for satellite system and PRN.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SatInSystem {

    /** Satellite system. */
    private final SatelliteSystem system;

    /** PRN number. */
    private final int prn;

    /** Simple constructor.
     * @param system satellite system
     * @param prn Pseudo Random Number
     */
    public SatInSystem(final SatelliteSystem system, final int prn) {
        this.system = system;
        this.prn    = prn;
    }

    /** Get the system this satellite belongs to.
     * @return system this satellite belongs to
     */
    public SatelliteSystem getSystem() {
        return system;
    }

    /** Get the Pseudo Random Number of the satellite.
     * @return Pseudo Random Number of the satellite
     */
    public int getPRN() {
        return prn;
    }

    /** Get a 2-digits Pseudo Random Number for RINEX files.
     * @return 2-digits Pseudo Random Number for RINEX files
     */
    public int getTwoDigitsRinexPRN() {
        return system == SatelliteSystem.SBAS ? prn - 100 : (system == SatelliteSystem.QZSS ? prn - 192 : prn);
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
        return getSystem().hashCode() ^ getPRN();
    }

}
