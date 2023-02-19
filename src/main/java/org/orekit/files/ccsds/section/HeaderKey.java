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

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keywords allowed in {@link Header}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum HeaderKey {

    /** Header comment. */
    COMMENT((token, context, header) ->
            token.getType() == TokenType.ENTRY ? header.addComment(token.getContentAsNormalizedString()) : true),

    /** Classification.
     * @since 12.0
     */
    CLASSIFICATION((token, context, header) -> token.processAsFreeTextString(header::setClassification)),

    /** Creation date. */
    CREATION_DATE((token, context, header) -> token.processAsDate(header::setCreationDate, context)),

    /** Creating agency or operator. */
    ORIGINATOR((token, context, header) -> token.processAsFreeTextString(header::setOriginator)),

    /** ID that uniquely identifies a message from a given originator. */
    MESSAGE_ID((token, context, header) -> token.processAsFreeTextString(header::setMessageId));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    HeaderKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param header header to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final Header header) {
        return processor.process(token, context, header);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param header header to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, Header header);
    }

}
