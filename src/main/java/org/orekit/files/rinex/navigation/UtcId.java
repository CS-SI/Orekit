/* Copyright 2022-2025 Luc Maisonobe
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
 * <p>
 * In addition to the ids listed here, Rinex 4.01 table 23 allowed UTC(BIPM) as a
 * possible UTC id for SBAS. This was added in June 2023 and Rinex 4.01 was officially
 * published in July 2023. However, this was quickly removed, in July 2023, i.e. just
 * after publication of Rinex 4.01, as directed by BIPM. It does not appear anymore in
 * Rinex 4.02 which was officially published in October 2024. Due to its transient
 * appearance in the standard, we decided to not include UTC(BIPM) in this enumerate.
 * </p>
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

    /** UTC(CRL).
     * @since 13.0
     */
    CRL("UTC(CRL)"),

    /** UTC(NIST).
     * @since 13.0
     */
    NIST("UTC(NIST)"),

    /** UTCIRN / UTC(NPLI). */
    IRN("UTC(NPLI)", "UTCIRN"),

    /** UTC(OP). */
    OP("UTC(OP)");

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

    /** Get reference ID.
     * @return reference ID
     * @since 14.0
     */
    public String getId() {
        return ids[0];
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
