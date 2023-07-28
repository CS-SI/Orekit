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
package org.orekit.files.ccsds.section;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.AbstractConstituentParser;
import org.orekit.files.ccsds.utils.parsing.ProcessingState;

/** {@link ProcessingState} for {@link Header NDM header}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class HeaderProcessingState implements ProcessingState {

    /** Context binding for header. */
    private final ContextBinding context;

    /** Parser for the complete message. */
    private final AbstractConstituentParser<?, ?, ?> parser;

    /** Simple constructor.
     * @param parser parser for the complete message
     */
    public HeaderProcessingState(final AbstractConstituentParser<?, ?, ?> parser) {
        this.context = new ContextBinding(
            parser::getConventions, parser::isSimpleEOP,
            parser::getDataContext, parser::getParsedUnitsBehavior, () -> null,
            () -> TimeSystem.UTC, () -> 0.0, () -> 1.0);
        this.parser  = parser;
    }

    /** {@inheritDoc} */
    @Override
    public boolean processToken(final ParseToken token) {

        parser.inHeader();

        if (Double.isNaN(parser.getHeader().getFormatVersion())) {
            // the first thing we expect is the format version
            // (however, in XML files it was already set before entering header)
            if (parser.getFormatVersionKey() != null &&
                parser.getFormatVersionKey().equals(token.getName()) &&
                token.getType() == TokenType.ENTRY) {
                parser.getHeader().setFormatVersion(token.getContentAsDouble());
                return true;
            } else {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, token.getFileName());
            }
        }

        try {
            return token.getName() != null &&
                   HeaderKey.valueOf(token.getName()).process(token, context, parser.getHeader());
        } catch (IllegalArgumentException iae) {
            // token has not been recognized
            return false;
        }

    }

}
