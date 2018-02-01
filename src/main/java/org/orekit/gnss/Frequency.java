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
    G01(SatelliteSystem.GPS,     "L1"),

    /** GPS L2. */
    G02(SatelliteSystem.GPS,     "L2"),

    /** GPS L5. */
    G05(SatelliteSystem.GPS,     "L5"),

    /** GLONASS, "G1". */
    R01(SatelliteSystem.GLONASS, "G1"),

    /** GLONASS, "G2". */
    R02(SatelliteSystem.GLONASS, "G2"),

    /** Galileo, "E1". */
    E01(SatelliteSystem.GALILEO, "E1"),

    /** Galileo E5a. */
    E05(SatelliteSystem.GALILEO, "E5a"),

    /** Galileo E5b. */
    E07(SatelliteSystem.GALILEO, "E5b"),

    /** Galileo E5 (E5a + E5b). */
    E08(SatelliteSystem.GALILEO, "E5 (E5a+E5b)"),

    /** Galileo E6. */
    E06(SatelliteSystem.GALILEO, "E6"),

    /** Galileo L1. */
    C01(SatelliteSystem.GALILEO, "E1"),

    /** Galileo E2. */
    C02(SatelliteSystem.GALILEO, "E2"),

    /** Galileo E5b. */
    C07(SatelliteSystem.GALILEO, "E5b"),

    /** Galileo E6. */
    C06(SatelliteSystem.GALILEO, "E6"),

    /** QZSS L1. */
    J01(SatelliteSystem.QZSS,    "L1"),

    /** QZSS L2. */
    J02(SatelliteSystem.QZSS,    "L2"),

    /** QZSS L5. */
    J05(SatelliteSystem.QZSS,    "L5"),

    /** QZSS LEX. */
    J06(SatelliteSystem.QZSS,    "LEX"),

    /** IRNSS L1. */
    I05(SatelliteSystem.QZSS,    "L1"),

    /** IRNSS S. */
    I09(SatelliteSystem.QZSS,    "S"),

    /** SBAS L1. */
    S01(SatelliteSystem.SBAS,    "L1"),

    /** SBAS L5. */
    S05(SatelliteSystem.SBAS,    "L5");
    // CHECKSTYLE: resume MultipleStringLiterals check

    /** Satellite system. */
    private final SatelliteSystem satelliteSystem;

    /** RINEX name for the frequency. */
    private final String name;

    /** Simple constructor.
     * @param name for the frequency
     * @param satelliteSystem satellite system for which this frequency is defined
     */
    Frequency(final SatelliteSystem satelliteSystem, final String name) {
        this.satelliteSystem = satelliteSystem;
        this.name            = name;
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

}
