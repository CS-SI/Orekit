/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.utils.lexical;

import org.orekit.files.ccsds.ndm.NDMFile;
import org.orekit.files.ccsds.ndm.NDMHeader;
import org.orekit.files.ccsds.ndm.NDMSegment;

/** Interface for parsing CCSDS messages.
 * @param <H> type of the header
 * @param <S> type of the segment
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface MessageParser<H extends NDMHeader, S extends NDMSegment<?, ?>> {

    /** Entry or block start marker found.
     * @param name name of the entry or block
     */
    void start(String name);

    /** Handle an entry.
     * @param entry entry found
     */
    void entry(Entry entry);

    /** Entry or block end marker found.
     * @param name name of the entry or block
     */
    void end(String name);

    /** Build the file from parsed entries.
     * @return parsed file
     */
    NDMFile<H, S> build();

}
