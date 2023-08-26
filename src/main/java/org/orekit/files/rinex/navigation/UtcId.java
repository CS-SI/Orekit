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
package org.orekit.files.rinex.navigation;

import java.util.HashMap;
import java.util.Map;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

/** Enumerate for the UTC ids.
 *
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum UtcId {

    /** UTC(USNO). */
    USNO("UTC(USNO)"),

    /** UTC(SU). */
    SU("UTC(SU)"),

    /** UTCGAL. */
    GAL("UTCGAL"),

    /** UTC(NTSC). */
    NTSC("UTC(NTSC)"),

    /** UTC(NICT). */
    NICT("UTC(NICT)"),

    /** UTCIRN / UTC(NPLI). */
    IRN("UTCIRN", "UTC(NPLI)"),

    /** UTC(OP). */
    OP("UTC(OP)"),

    /** UTC(NIST).
     * <p>
     * In Rinex 4.00, this entry is not present in table 23, but appears in table A30.
     * </p>
     */
    NIST("UTC(NIST)");

    /** Parsing map. */
    private static final Map<String, UtcId> MAP = new HashMap<>();

    static {
        for (final UtcId utc : values()) {
            for (final String id : utc.ids) {
                MAP.put(id, utc);
            }
        }
    }

    /** Valid ids. */
    private final String[] ids;

    /** Simple constructor.
     * @param ids valid ids
     */
    UtcId(final String... ids) {
        this.ids = ids.clone();
    }

    /** Parse a string to get the UTC id.
     * @param id string to parse
     * @return the UTC id
     * @exception OrekitIllegalArgumentException if the string does not correspond to a UTC id
     */
    public static UtcId parseUtcId(final String id)
        throws OrekitIllegalArgumentException {
        final UtcId utcId = MAP.get(id);
        if (utcId == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_UTC_ID, id);
        }
        return utcId;
    }

}
