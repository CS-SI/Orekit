/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.propagation.analytical.gnss.data;

import java.util.HashMap;
import java.util.Map;

/** Enumerate for Beidou satellite type.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum BeidouSatelliteType {

    /** Reserved. */
    RESERVED(0),

    /** Geostationary satellite. */
    GEO(1),

    /** Inclined geosynchronous. */
    IGSO(2),

    /** Middle Earth Orbit. */
    MEO(3);

    /** Parsing map.
     * @since 14.0
     */
    private static final Map<Integer, BeidouSatelliteType> INTEGERS_MAP = new HashMap<>();
    static {
        for (final BeidouSatelliteType type : values()) {
            INTEGERS_MAP.put(type.getIntegerId(), type);
        }
    }

    /** Integer identifier.
     * @since 14.0
     */
    private final int intId;

    /** Simple constructor.
     * @param intId integer identifier
     * @since 14.0
     */
    BeidouSatelliteType(final int intId) {
        this.intId    = intId;
    }

    /** Get the integer identifier.
     * @return integer identifier
     * @since 14.0
     */
    public int getIntegerId() {
        return intId;
    }

    /** Parse the integer to get the satellite type.
     * @param i integer to parse
     * @return the satellite type corresponding to the string
     * @exception IllegalArgumentException if the string does not correspond to a satellite type
     * @since 14.0
     */
    public static BeidouSatelliteType parseSatelliteType(final int i) {
        return INTEGERS_MAP.get(i);
    }

}
