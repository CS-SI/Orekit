/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.sp3;

import java.util.HashMap;
import java.util.Map;

/** File type indicator.
 * @author Thomas Neidhart
 * @author Evan Ward
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum SP3FileType {

    /** GPS only file. */
    GPS("G"),

    /** Mixed file. */
    MIXED("M"),

    /** GLONASS only file. */
    GLONASS("R"),

    /** LEO only file. */
    LEO("L"),

    /** Galileo only file. */
    GALILEO("E"),

    /** SBAS only file. */
    SBAS("S"),

    /** IRNSS only file. */
    IRNSS("I"),

    /** COMPASS only file. */
    COMPASS("C"),

    /** QZSS only file. */
    QZSS("J"),

    /** undefined file format. */
    UNDEFINED("?");

    /** Numbers map. */
    private static final Map<String, SP3FileType> MAP = new HashMap<>();
    static {
        for (final SP3FileType type : values()) {
            MAP.put(type.getKey(), type);
        }
    }

    /** Key for the file type. */
    private final String key;

    /** Simple constructor.
     * @param key for the file type
     */
    SP3FileType(final String key) {
        this.key = key;
    }

    /** Get the key for the file type.
     * @return key for the file type
     */
    public String getKey() {
        return key;
    }

    /** Parse the string to get the data used.
     * @param s string to parse
     * @return the file type corresponding to the string
     */
    public static SP3FileType parse(final String s) {
        final SP3FileType type = MAP.get(s.toUpperCase());
        return (type == null) ? UNDEFINED : type;
    }

}
