/* Contributed in the public domain.
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
package org.orekit.files.general;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.orekit.errors.OrekitException;

/**
 * Parse an ephemeris file.
 *
 * @author Evan Ward
 */
public interface EphemerisFileParser {

    /**
     * Parse an ephemeris file from a stream.
     *
     * @param reader   containing the ephemeris file.
     * @param fileName to use in error messages.
     * @return a parsed ephemeris file.
     * @throws OrekitException if the ephemeris file cannot be parsed.
     * @throws IOException     if {@code reader} throws one.
     */
    EphemerisFile parse(BufferedReader reader, String fileName)
            throws OrekitException, IOException;

    /**
     * Parse an ephemeris file from a file on the local file system.
     *
     * @param fileName path to the ephemeris file.
     * @return parsed ephemeris fiale.
     * @throws OrekitException if the ephemeris file cannot be parsed.
     * @throws IOException     if one is thrown while opening or reading from {@code
     *                         fileName}.
     */
    default EphemerisFile parse(final String fileName)
            throws IOException, OrekitException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName))) {
            return parse(reader, fileName);
        }
    }

}
