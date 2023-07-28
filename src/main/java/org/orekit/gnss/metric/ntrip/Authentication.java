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
package org.orekit.gnss.metric.ntrip;

import java.util.HashMap;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Enumerate for authentication method in {@link DataStreamRecord}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum Authentication {

    /** None. */
    NONE("N"),

    /** Basic. */
    BASIC("B"),

    /** Digest. */
    DIGEST("D");

    /** Keywords map. */
    private static final Map<String, Authentication> KEYWORDS_MAP = new HashMap<String, Authentication>();
    static {
        for (final Authentication type : values()) {
            KEYWORDS_MAP.put(type.getKeyword(), type);
        }
    }

    /** Keyword. */
    private final String keyword;

    /** Simple constructor.
     * @param keyword keyword in the sourcetable records
     */
    Authentication(final String keyword) {
        this.keyword = keyword;
    }

    /** Get keyword.
     * @return keyword
     */
    private String getKeyword() {
        return keyword;
    }

    /** Get the authentication type corresponding to a keyword.
     * @param keyword authentication keyword
     * @return the authentication type corresponding to the keyword
     */
    public static Authentication getAuthentication(final String keyword) {
        final Authentication authentication = KEYWORDS_MAP.get(keyword);
        if (authentication == null) {
            throw new OrekitException(OrekitMessages.UNKNOWN_AUTHENTICATION_METHOD, keyword);
        }
        return authentication;
    }

}
