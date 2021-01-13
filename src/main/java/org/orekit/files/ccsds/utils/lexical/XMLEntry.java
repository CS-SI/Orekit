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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.xml.sax.Locator;

/** Container for a simple entry in a KVN (Key-Value Notation) file.
 * @author Luc Maisonobe
 * @since 11.0
 */
class XMLEntry extends Entry {

    /** Number of the line from which pair is extracted. */
    private final int lineNumber;

    /** Name of the file. */
    private final String fileName;

    /** Simple constructor.
     * @param name name of the entry
     * @param content content of the entry
     * @param locator SAX event locator
     * @param fileName name of the file
     */
    XMLEntry(final String name, final String content,
             final Locator locator, final String fileName) {
        super(name, content);
        this.lineNumber = locator.getLineNumber();
        this.fileName   = fileName;
    }

    /** {@inheritDoc} */
    @Override
    protected OrekitException generateException() {
        return new OrekitException(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE,
                                   getName(), lineNumber, fileName);
    }

}
