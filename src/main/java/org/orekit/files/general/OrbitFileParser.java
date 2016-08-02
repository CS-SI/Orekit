/* Copyright 2002-2012 Space Applications Services
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

import java.io.InputStream;

import org.orekit.errors.OrekitException;

/** Interface for orbit file parsers.
 * @author Thomas Neidhart
 */
public interface OrbitFileParser {

    /** Reads an orbit file from the given stream and returns a parsed
     * {@link OrbitFile}.
     * @param stream the stream to read from
     * @return a parsed instance of {@link OrbitFile}
     * @exception OrekitException if the orbit file could not be parsed
     * successfully from the given stream
     */
    OrbitFile parse(InputStream stream) throws OrekitException;

    /** Reads the orbit file and returns a parsed {@link OrbitFile}.
     * @param fileName the file to read and parse
     * @return a parsed instance of {@link OrbitFile}
     * @exception OrekitException if the orbit file could not be parsed
     * successfully from the given file
     */
    OrbitFile parse(String fileName) throws OrekitException;
}
