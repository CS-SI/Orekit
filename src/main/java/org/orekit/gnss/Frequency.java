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

import org.orekit.utils.Constants;

/**
 * Enumerate for GNSS frequencies.
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
public enum Frequency {

    // CHECKSTYLE: stop MultipleStringLiterals check
    /** GPS L1 (1575.42 MHz). */
    G01(SatelliteSystem.GPS,     "L1", 154),

    /** GPS L2 (1227.6 MHz). */
    G02(SatelliteSystem.GPS,     "L2", 120),

    /** GPS L5 (1176.45 MHz). */
    G05(SatelliteSystem.GPS,     "L5", 115),

    /** GLONASS, "G1" (1602 MHZ). */
    R01(SatelliteSystem.GLONASS, "G1", 1602.0 / 10.23),

    /** GLONASS, "G2" (1246 MHz). */
    R02(SatelliteSystem.GLONASS, "G2", 1246.0 / 10.23),

    /** GLONASS, "G3" (1202.025 MHz). */
    R03(SatelliteSystem.GLONASS, "G3", 117.5),

    /** GLONASS, "G1a" (1600.995 MHZ). */
    R04(SatelliteSystem.GLONASS, "G1a", 156.5),

    /** GLONASS, "G2a" (1248.06 MHz). */
    R06(SatelliteSystem.GLONASS, "G2a", 122),

    /** Galileo, "E1" (1575.42 MHz). */
    E01(SatelliteSystem.GALILEO, "E1", 154),

    /** Galileo E5a (1176.45 MHz). */
    E05(SatelliteSystem.GALILEO, "E5a", 115),

    /** Galileo E5b (1207.14 MHz). */
    E07(SatelliteSystem.GALILEO, "E5b", 118),

    /** Galileo E5 (E5a + E5b) (1191.795MHz). */
    E08(SatelliteSystem.GALILEO, "E5 (E5a+E5b)", 116.5),

    /** Galileo E6 (1278.75 MHz). */
    E06(SatelliteSystem.GALILEO, "E6", 125),

    /** In the ANTEX files, both C01 and C02 refer to Beidou B1 signal (1561.098 MHz). */
    C01(SatelliteSystem.BEIDOU, "B1", 152.6),

    /** In the ANTEX files, both C01 and C02 refer to Beidou B1 signal (1561.098 MHz). */
    C02(SatelliteSystem.BEIDOU, "B1", 152.6),

    /** In the ANTEX files, C05 appears without much reference
     * for consistency with Rinex 4 tables, we assume it is B2a (1176.45 MHz).
     */
    C05(SatelliteSystem.BEIDOU, "B2a", 115),

    /** In the ANTEX files, C06 appears without much reference, we assume it is B2 (1207.14 MHz). */
    C06(SatelliteSystem.BEIDOU, "B2", 118),

    /** In the ANTEX files, C07 seems to refer to a signal close to E06, probably B3... (1268.52 MHz). */
    C07(SatelliteSystem.BEIDOU, "B3", 124),

    /** In the ANTEX files, C08 appears without much reference
     * for consistency with Rinex 4 tables, we assume it is B2 (B2a+B2b) (1191.795 MHz).
     */
    C08(SatelliteSystem.BEIDOU, "B2 (B2a+B2b)", 116.5),

    /** Beidou B1 (1561.098 MHz). */
    B01(SatelliteSystem.BEIDOU,  "B1", 152.6),

    /** Beidou B2 (1207.14 MHz). */
    B02(SatelliteSystem.BEIDOU,  "B2", 118),

    /** Beidou B3 (1268.52 MHz). */
    B03(SatelliteSystem.BEIDOU,  "B3", 124),

    /** Beidou B1C (1575.42 MHz). */
    B1C(SatelliteSystem.BEIDOU,  "B1C", 154),

    /** Beidou B1A (1575.42 MHz). */
    B1A(SatelliteSystem.BEIDOU,  "B1A", 154),

    /** Beidou B2a (1176.45 MHz). */
    B2A(SatelliteSystem.BEIDOU, "B2a", 115),

    /** Beidou B2b (1207.14 MHz). */
    B2B(SatelliteSystem.BEIDOU, "B2b", 118),

    /** Beidou B2 (B2a + B2b) (1191.795MHz). */
    B08(SatelliteSystem.BEIDOU, "B2 (B2a+B2b)", 116.5),

    /** Beidou B3A (1268.52 MHz). */
    B3A(SatelliteSystem.BEIDOU, "B3A", 124),

    /** QZSS L1 (1575.42 MHz). */
    J01(SatelliteSystem.QZSS,    "L1", 154),

    /** QZSS L2 (1227.6 MHz). */
    J02(SatelliteSystem.QZSS,    "L2", 120),

    /** QZSS L5 (1176.45 MHz). */
    J05(SatelliteSystem.QZSS,    "L5", 115),

    /** QZSS LEX (1278.75 MHz). */
    J06(SatelliteSystem.QZSS,    "LEX", 125),

    /** IRNSS L5. (1176.45 MHz) */
    I05(SatelliteSystem.IRNSS,   "L5", 115),

    /** IRNSS S (2492.028 MHz). */
    I09(SatelliteSystem.IRNSS,   "S", 243.6),

    /** SBAS L1 (1575.42 MHz). */
    S01(SatelliteSystem.SBAS,    "L1", 154),

    /** SBAS L5 (1176.45 MHz). */
    S05(SatelliteSystem.SBAS,    "L5", 115);
    // CHECKSTYLE: resume MultipleStringLiterals check

    /** Common frequency F0 in MHz (10.23 MHz). */
    public static final double F0 = 10.23;

    /** Satellite system. */
    private final SatelliteSystem satelliteSystem;

    /** RINEX name for the frequency. */
    private final String name;

    /** Ratio f/f0, where {@link #F0 f0} is the common frequency. */
    private final double ratio;

    /** Simple constructor.
     * @param name for the frequency
     * @param satelliteSystem satellite system for which this frequency is defined
     * @param ratio ratio f/f0, where {@link #F0 f0} is the common frequency
     */
    Frequency(final SatelliteSystem satelliteSystem, final String name, final double ratio) {
        this.satelliteSystem = satelliteSystem;
        this.name            = name;
        this.ratio           = ratio;
    }

    /** Get the RINEX name for the frequency.
     * @return RINEX name for the frequency
     */
    public String getName() {
        return name;
    }

    /** Get the satellite system for which this frequency is defined.
     * @return satellite system for which this frequency is defined
     */
    public SatelliteSystem getSatelliteSystem() {
        return satelliteSystem;
    }

    /** Get the ratio f/f0, where {@link #F0 f0} is the common frequency.
     * @return ratio f/f0, where {@link #F0 f0} is the common frequency
     * @see #F0
     * @see #getMHzFrequency()
     */
    public double getRatio() {
        return ratio;
    }

    /** Get the value of the frequency in MHz.
     * @return value of the frequency in MHz
     * @see #F0
     * @see #getRatio()
     * @see #getWavelength()
     */
    public double getMHzFrequency() {
        return ratio * F0;
    }

    /** Get the wavelength in meters.
     * @return wavelength in meters
     * @see #getMHzFrequency()
     * @since 10.1
     */
    public double getWavelength() {
        return Constants.SPEED_OF_LIGHT / (1.0e6 * getMHzFrequency());
    }

}
