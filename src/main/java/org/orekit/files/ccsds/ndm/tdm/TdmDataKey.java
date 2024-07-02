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
package org.orekit.files.ccsds.ndm.tdm;

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;


/** Keys for {@link Observation TDM observations} entries, except the measurements themselves.
 * @author Maxime Journot
 * @since 11.0
 */
public enum TdmDataKey {

    /** Observation wrapper. */
    observation((token, context, observationsBlock) -> observationsBlock.addObservationEpoch(null)),

    /** Comment entry. */
    COMMENT((token, context, observationsBlock) ->
            token.getType() == TokenType.ENTRY ? observationsBlock.addComment(token.getContentAsNormalizedString()) : true),

    /** Epoch entry. */
    EPOCH((token, context, observationsBlock) -> token.processAsDate(observationsBlock::addObservationEpoch, context));

    /** Processing method. */
    private final transient TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    TdmDataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param observationsBlock observation block to fill
     * @return true if token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context,
                           final ObservationsBlock observationsBlock) {
        return processor.process(token, context, observationsBlock);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param observationsBlock observation block to fill
         * @return true if token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, ObservationsBlock observationsBlock);
    }

}
