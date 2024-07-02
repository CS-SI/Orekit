/* Copyright 2002-2024 CS GROUP
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
package org.orekit.files.ccsds.section;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.AbstractConstituentParser;
import org.orekit.files.ccsds.utils.parsing.ProcessingState;

/** {@link ProcessingState} for structure of {@link FileFormat#XML} CCSDS Messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class XmlStructureProcessingState implements ProcessingState {

    /** Name of the root element. */
    private final String root;

    /** Parser for the complete message. */
    private final AbstractConstituentParser<?, ?, ?> parser;

    /** Simple constructor.
     * @param root name of the root element
     * @param parser parser for the complete message
     */
    public XmlStructureProcessingState(final String root, final AbstractConstituentParser<?, ?, ?> parser) {
        this.root   = root;
        this.parser = parser;
    }

    /** {@inheritDoc} */
    @Override
    public boolean processToken(final ParseToken token) {

        if (root.equals(token.getName())) {
            parser.setEndTagSeen(token.getType() == TokenType.STOP);
            return true;
        }

        if (Double.isNaN(parser.getHeader().getFormatVersion())) {
            // the first thing we expect (after the ignored start tag for root element) is the format version
            if (parser.getFormatVersionKey() != null &&
                parser.getFormatVersionKey().equals(token.getName()) && token.getType() == TokenType.ENTRY) {
                parser.getHeader().setFormatVersion(token.getContentAsDouble());
                return true;
            } else {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, token.getFileName());
            }
        }

        try {
            return XmlStructureKey.valueOf(token.getName()).process(token, parser);
        } catch (IllegalArgumentException iae) {
            // ignored, we delegate handling this token to fallback state
            return false;
        }
    }

}
