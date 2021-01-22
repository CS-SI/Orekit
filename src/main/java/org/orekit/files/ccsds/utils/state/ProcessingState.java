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
package org.orekit.files.ccsds.utils.state;

import java.util.Deque;

import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.lexical.MessageParser;
import org.orekit.files.ccsds.utils.lexical.ParseToken;

/**
 * Interface for processing parsing tokens for CCSDS NDM files.
 * <p>
 * This interface is intended for use as the state in
 * state design pattern, the {@link MessageParser message parser}
 * itself being used as the context that holds the active state.
 * </p>
 * @see MessageParser
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface ProcessingState {

    /** Process one token.
     * <p>
     * The state may either:
     *   <ul>
     *     <li>process the token and ignore the next tokens queue</li>
     *     <li>process the token and insert new tokens in the queue to replace it
     *     (for example when a {@link TokenType#RAW_LINE raw line} is split into fields
     *     that will be parsed later on)</li>
     *     <li>push the token back into the queue if unable to process it</li>
     *   </ul>
     * </p>
     * <p>
     * The state must always return a non-null state that ill become the active state to use
     * after itself. In most cases when the current token was successfully parsed, it will
     * return itself as processing states are mainly set up for complete sections and therefore
     * will process several entries in rows. If the state was not able to process the token
     * and have pushed it back into the {@code next} queue then it should return another state that
     * is expected to be able to parse it, otherwise an infinite loop would occur.
     * </p>
     * @param token token to process
     * @param next queue for pending tokens waiting processing after this one, may be updated
     * @return next state to use for processing upcoming tokens
     */
    ProcessingState processToken(ParseToken token, Deque<ParseToken> next);

}
