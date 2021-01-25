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
package org.orekit.files.ccsds.section;

import java.util.Deque;

import org.orekit.files.ccsds.utils.lexical.FileFormat;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.state.AbstractMessageParser;
import org.orekit.files.ccsds.utils.state.ProcessingState;

/** {@link ProcessingState} for structure of {@link FileFormat#KVN} CCSDS Messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class KVNStructureProcessingState implements ProcessingState {

    /** Parser for the complete message. */
    private final AbstractMessageParser<?, ?> parser;

    /** Simple constructor.
     * @param parser parser for the complete message
     */
    public KVNStructureProcessingState(final AbstractMessageParser<?, ?> parser) {
        this.parser = parser;
    }

    /** {@inheritDoc} */
    @Override
    public ProcessingState processToken(final ParseToken token, final Deque<ParseToken> next) {
        switch (token.getName()) {
            case "META" :
                if (token.getType() == TokenType.START) {
                    // next parse tokens will be handled as metadata
                    return parser.startMetadata();
                } else if (token.getType() == TokenType.END) {
                    // nothing to do here, we expect a DATA_START next
                    return this;
                }
                break;
            case "DATA" :
                if (token.getType() == TokenType.START) {
                    // next parse tokens will be handled as data
                    return parser.startData();
                } else if (token.getType() == TokenType.END) {
                    parser.stopData();
                    // we expect a META_START next
                    return this;
                }
                break;
            default :
                // nothing to do here, errors are handled below
        }
        throw token.generateException();
    }

}
