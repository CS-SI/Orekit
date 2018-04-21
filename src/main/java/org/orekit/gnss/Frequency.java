/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

/**
 * Enumerate for satellite system.
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
public enum Frequency {

    // CHECKSTYLE: stop MultipleStringLiterals check
    /** GPS L1. */
    G01(SatelliteSystem.GPS,     "L1", 1575.42),

    /** GPS L2. */
    G02(SatelliteSystem.GPS,     "L2", 1227.6),

    /** GPS L5. */
    G05(SatelliteSystem.GPS,     "L5", 1176.45),

    /** GLONASS, "G1". */
    R01(SatelliteSystem.GLONASS, "G1", 1602),

    /** GLONASS, "G2". */
    R02(SatelliteSystem.GLONASS, "G2", 1246),

    /** Galileo, "E1". */
    E01(SatelliteSystem.GALILEO, "E1", 1575.42),

    /** Galileo E5a. */
    E05(SatelliteSystem.GALILEO, "E5a", 1191.795),

    /** Galileo E5b. */
    E07(SatelliteSystem.GALILEO, "E5b", 1191.795),

    /** Galileo E5 (E5a + E5b). */
    E08(SatelliteSystem.GALILEO, "E5 (E5a+E5b)", 1191.795),

    /** Galileo E6. */
    E06(SatelliteSystem.GALILEO, "E6", 1278.75),

    /** In the ANTEX files, both C01 and C02 refer to Beidou B1 signal. */
    C01(SatelliteSystem.BEIDOU, "B1", 1561.098),

    /** In the ANTEX files, both C01 and C02 refer to Beidou B1 signal. */
    C02(SatelliteSystem.BEIDOU, "B1", 1561.098),

    /** In the ANTEX files, C06 appears without much reference, we assume it is B2. */
    C06(SatelliteSystem.BEIDOU, "B2", 1207.14),

    /** In the ANTEX files, C07 seems to refer to a signal close to E06, probably B3... */
    C07(SatelliteSystem.BEIDOU, "B3", 1268.52),

    /** Beidou B1. */
    B01(SatelliteSystem.BEIDOU,  "B1", 1561.098),

    /** Beidou B2. */
    B02(SatelliteSystem.BEIDOU,  "B2", 1207.14),

    /** Beidou B3. */
    B03(SatelliteSystem.BEIDOU,  "B3", 1268.52),

    /** QZSS L1. */
    J01(SatelliteSystem.QZSS,    "L1", 1575.42),

    /** QZSS L2. */
    J02(SatelliteSystem.QZSS,    "L2", 1227.6),

    /** QZSS L5. */
    J05(SatelliteSystem.QZSS,    "L5", 1176.45),

    /** QZSS LEX. */
    J06(SatelliteSystem.QZSS,    "LEX", 1278.75),

    /** IRNSS L5. */
    I05(SatelliteSystem.IRNSS,   "L5", 1176.45),

    /** IRNSS S. */
    I09(SatelliteSystem.IRNSS,   "S", 2492.028),

    /** SBAS L1. */
    S01(SatelliteSystem.SBAS,    "L1", 1575.42),

    /** SBAS L5. */
    S05(SatelliteSystem.SBAS,    "L5", 1176.45);
    // CHECKSTYLE: resume MultipleStringLiterals check

    /** Satellite system. */
    private final SatelliteSystem satelliteSystem;

    /** RINEX name for the frequency. */
    private final String name;

    /** Value of the frequency in MHz. */
    private final double mhz;

    /** Simple constructor.
     * @param name for the frequency
     * @param satelliteSystem satellite system for which this frequency is defined
     * @param mhz value of the frequency in MHz
     */
    Frequency(final SatelliteSystem satelliteSystem, final String name, final double mhz) {
        this.satelliteSystem = satelliteSystem;
        this.name            = name;
        this.mhz             = mhz;
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

    /** Get the value of the frequency in MHz.
     * @return satellite system for which this frequency is defined
     */
    public double getMHzFrequency() {
        return mhz;
    }

}
