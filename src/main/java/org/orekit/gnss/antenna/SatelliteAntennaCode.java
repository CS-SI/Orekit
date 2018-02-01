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
package org.orekit.gnss.antenna;

import java.util.HashMap;
import java.util.Map;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

/**
 * Enumerate for satellite antenna codes.
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
public enum SatelliteAntennaCode {

    /** BeiDou-2 GEO. */
    BEIDOU_2G("BEIDOU-2G"),

    /** BeiDou-2 IGSO. */
    BEIDOU_2I("BEIDOU-2I"),

    /** BeiDou-2 MEO. */
    BEIDOU_2M("BEIDOU-2M"),

    /** BeiDou-3 IGSO. */
    BEIDOU_3I("BEIDOU-3I"),

    /** GPS Block I     : SVN 01-11. */
    BLOCK_I("BLOCK I"),

    /** GPS Block II    : SVN 13-21. */
    BLOCK_II("BLOCK II"),

    /** GPS Block IIA   : SVN 22-40. */
    BLOCK_IIA("BLOCK IIA"),

    /** GPS Block IIR   : SVN 41, 43-46, 51, 54, 56. */
    BLOCK_IIR_A("BLOCK IIR-A"),

    /** GPS Block IIR   : SVN 47, 59-61. */
    BLOCK_IIR_B("BLOCK IIR-B"),

    /** GPS Block IIR-M : SVN 48-50, 52-53, 55, 57-58. */
    BLOCK_IIR_M("BLOCK IIR-M"),

    /** GPS Block IIF   : SVN 62-73. */
    BLOCK_IIF("BLOCK IIF"),

    /** GPS Block IIIA  : SVN 74-81. */
    BLOCK_IIIA("BLOCK IIIA"),

    /** Galileo In-Orbit Validation Element A (GIOVE-A). */
    GALILEO_0A("GALILEO-0A"),

    /** Galileo In-Orbit Validation Element B (GIOVE-B). */
    GALILEO_0B("GALILEO-0B"),

    /** Galileo IOV     : GSAT 0101-0104. */
    GALILEO_1("GALILEO-1"),

    /** Galileo FOC     : GSAT 0201-0222. */
    GALILEO_2("GALILEO-2"),

    /** GLONASS         : GLONASS no. 201-249, 750-798. */
    GLONASS("GLONASS"),

    /** GLONASS-M       : GLONASS no. 701-749, IGS SVN R850-R861 (GLO no. + 100). */
    GLONASS_M("GLONASS-M"),

    /** GLONASS-K1      : IGS SVN R801-R802 (GLO no. + 100). */
    GLONASS_K1("GLONASS-K1"),

    /** GLONASS-K2. */
    GLONASS_K2("GLONASS-K2"),

    /** IRNSS-1 GEO. */
    IRNSS_1GEO("IRNSS-1GEO"),

    /** IRNSS-1 IGSO. */
    IRNSS_1IGSO("IRNSS-1IGSO"),

    /** QZSS Block I (Michibiki-1). */
    QZSS("QZSS"),

    /** QZSS Block II IGSO (Michibiki-2,4). */
    QZSS_2I("QZSS-2I"),

    /** QZSS Block II GEO (Michibiki-3). */
    QZSS_2G("QZSS-2G");

    /** Parsing map. */
    private static final Map<String, SatelliteAntennaCode> NAMES_MAP = new HashMap<>();
    static {
        for (final SatelliteAntennaCode satelliteAntennaCode : values()) {
            NAMES_MAP.put(satelliteAntennaCode.getName(), satelliteAntennaCode);
        }
    }

    /** IGS name for the antenna code. */
    private final String name;

    /** Simple constructor.
     * @param name IGS name for the antenna code
     */
    SatelliteAntennaCode(final String name) {
        this.name = name;
    }

    /** Get the IGS name for the antenna code.
     * @return IGS name for the antenna code
     */
    public String getName() {
        return name;
    }

    /** Parse a string to get the satellite antenna code.
     * @param s string to parse (must be a strict IGS name)
     * @return the satellite antenna code
     * @exception OrekitIllegalArgumentException if the string does not correspond to a satellite antenna code
     */
    public static SatelliteAntennaCode parseSatelliteAntennaCode(final String s)
        throws OrekitIllegalArgumentException {
        final SatelliteAntennaCode satelliteAntennaCode = NAMES_MAP.get(s);
        if (satelliteAntennaCode == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_SATELLITE_ANTENNA_CODE, s);
        }
        return satelliteAntennaCode;
    }

}
