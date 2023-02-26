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
package org.orekit.gnss;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Enumerate for RINEX files types.
 * @since 12.0
 */
public enum RinexFileType {

    /** Rinex Observation. */
    OBSERVATION("O"),

    /** Rinex navigation. */
    NAVIGATION("N");

    /** Key of the file type. */
    private final String key;

    /** Simple constructor.
     * @param key key of the file type
     */
    RinexFileType(final String key) {
        this.key = key;
    }

    /** Get the key of the file type.
     * @return key of the file type
     */
    public String getKey() {
        return key;
    }

    /** Check if a file matches type.
     * @param name name of the file (for error message generation)
     * @param parsedKey key parsed from the file
     * @exception OrekitException if file does not match type
     */
    public void complainIfNoMatch(final String name, final String parsedKey) {
        if (!key.endsWith(parsedKey)) {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
        }
    }

}
