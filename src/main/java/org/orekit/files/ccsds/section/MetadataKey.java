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


/** Keys for {@link Metadata NDM Metadata} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum MetadataKey {

    /** Comment entry. */
    COMMENT((token, context, metadata) ->
             token.getType() == TokenType.ENTRY ? metadata.addComment(token.getContentAsNormalizedString()) : true),

    /** Time system entry. */
    TIME_SYSTEM((token, context, metadata) -> token.processAsTimeSystem(metadata::setTimeSystem));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    MetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param metadata metadata to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final Metadata metadata) {
        return processor.process(token, context, metadata);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param metadata metadata to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, Metadata metadata);
    }

}
