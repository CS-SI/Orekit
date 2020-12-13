/* Copyright 2002-2020 CS GROUP
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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Container for a simple entry in a KVN (Key-Value Notation) file.
 * @author Luc Maisonobe
 * @since 11.0
 */
class KVNEntry extends Entry {

    /** Line from which pair is extracted. */
    private final String line;

    /** Number of the line from which pair is extracted. */
    private final int lineNumber;

    /** Name of the file. */
    private final String fileName;

    /** Simple constructor.
     * @param name name of the entry
     * @param content content of the entry
     * @param line to split
     * @param lineNumber number of the line in the CCSDS data message
     * @param fileName name of the file
     */
    KVNEntry(final String name, final String content,
             final String line, final int lineNumber, final String fileName) {
        super(name, content);
        this.line       = line;
        this.lineNumber = lineNumber;
        this.fileName   = fileName;
    }

    /** {@inheritDoc} */
    @Override
    protected OrekitException generateException() {
        return new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                   lineNumber, fileName, line);
    }

}
