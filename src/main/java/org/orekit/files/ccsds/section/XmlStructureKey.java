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

import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.AbstractConstituentParser;

/** Keys for {@link FileFormat#XML} format structure.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum XmlStructureKey {

    /** Body structure. */
    body((token, parser) -> true),

    /** Segment structure. */
    segment((token, parser) -> true),

    /** Header structure. */
    header((token, parser) -> {
        if (token.getType() == TokenType.START) {
            return parser.prepareHeader();
        } else if (token.getType() == TokenType.STOP) {
            return parser.finalizeHeader();
        }
        return false;
    }),

    /** XML metadata structure. */
    metadata((token, parser) -> {
        if (token.getType() == TokenType.START) {
            return parser.prepareMetadata();
        } else if (token.getType() == TokenType.STOP) {
            return parser.finalizeMetadata();
        }
        return false;
    }),

    /** XML data structure. */
    data((token, parser) -> {
        if (token.getType() == TokenType.START) {
            return parser.prepareData();
        } else if (token.getType() == TokenType.STOP) {
            return parser.finalizeData();
        }
        return false;
    });

    /** Processing method. */
    private final transient TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    XmlStructureKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
         * @param parser file parser
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final AbstractConstituentParser<?, ?, ?> parser) {
        return processor.process(token, parser);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param parser file parser
         * @return true of token was accepted
         */
        boolean process(ParseToken token, AbstractConstituentParser<?, ?, ?> parser);
    }

}
