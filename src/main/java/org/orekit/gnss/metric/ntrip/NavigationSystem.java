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

/** Enumerate for navigation system in {@link DataStreamRecord}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum NavigationSystem {

    /** GPS. */
    GPS("GPS"),

    /** Glonass. */
    GLO("GLO", "Glonass"),

    /** Galileo. */
    GAL("GAL", "Galileo"),

    /** Beidou. */
    BDS("BDS", "Beidou"),

    /** QZNSS. */
    QZS("QZS", "QZNSS"),

    /** SBAS. */
    SBAS("SBAS"),

    /** No navigation system for this stream. */
    EMPTY("");

    /** Keywords map. */
    private static final Map<String, NavigationSystem> KEYWORDS_MAP = new HashMap<String, NavigationSystem>();
    static {
        for (final NavigationSystem type : values()) {
            KEYWORDS_MAP.put(type.getKeyword(), type);
        }
    }

    /** Keyword. */
    private final String keyword;

    /** Name. */
    private final String name;

    /** Simple constructor.
     * @param keyword keyword in the sourcetable records
     */
    NavigationSystem(final String keyword) {
        this(keyword, keyword);
    }

    /** Simple constructor.
     * @param keyword keyword in the sourcetable records
     * @param name readable name
     */
    NavigationSystem(final String keyword, final String name) {
        this.keyword = keyword;
        this.name    = name;
    }

    /** Get keyword.
     * @return keyword
     */
    private String getKeyword() {
        return keyword;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name;
    }

    /** Get the navigation system corresponding to a keyword.
     * @param keyword navigation system keyword
     * @return the navigation system corresponding to the keyword
     */
    public static NavigationSystem getNavigationSystem(final String keyword) {
        final NavigationSystem system = KEYWORDS_MAP.get(keyword);
        if (system == null) {
            throw new OrekitException(OrekitMessages.UNKNOWN_NAVIGATION_SYSTEM, keyword);
        }
        return system;
    }

}
