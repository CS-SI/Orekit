/* Copyright 2022-2025 Thales Alenia Space
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

/** Enumerate for message type.
 * @author Luc Maisonobe
 * @since 14.0
 */
public enum MessageType {

    /** Ephemeris message. */
    EPH("> EPH"),

    /** System Time Offset. */
    STO("> STO"),

    /** Earth Orientation Parameter. */
    EOP("> EOP"),

    /** Ionosphere. */
    ION("> ION"),

    /** Broadcast orbit. */
    ORBIT("   ");

    /** Line prefix in navigation files. */
    private final String prefix;

    /** Simple constructor.
     * @param prefix line prefix
     */
    MessageType(final String prefix) {
        this.prefix = prefix;
    }

    /** Get the line prefix.
     * @return line prefix.
     */
    public String getPrefix() {
        return prefix;
    }

    /** Check if a line matches type.
     * @param line line to check
     * @return true if line matches type
     */
    public boolean matches(final String line) {
        return line.startsWith(prefix);
    }

}
