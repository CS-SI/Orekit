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
package org.orekit.files.ccsds.ndm.odm;

import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;


/** Keys for {@link CommonMetadata common ODM metadata} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum CommonMetadataKey {

    /** Object ID entry. */
    OBJECT_ID((token, context, metadata) -> token.processAsNormalizedString(metadata::setObjectID)),

    /** Center name entry. */
    CENTER_NAME((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            metadata.setCenterName(token.getContentAsNormalizedString(), context.getDataContext().getCelestialBodies());
        }
        return true;
    }),

    /** Name of the reference frame in which the state vector and optional Keplerian element data are given. */
    REF_FRAME((token, context, metadata) -> token.processAsFrame(metadata::setReferenceFrame, context, true, false, false)),

    /** Epoch of reference frame, if not intrinsic to the definition of the reference frame. */
    REF_FRAME_EPOCH((token, context, metadata) -> token.processAsNormalizedString(metadata::setFrameEpochString));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    CommonMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context parsing context
     * @param metadata metadata to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final CommonMetadata metadata) {
        return processor.process(token, context, metadata);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param metadata metadata to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ParsingContext context, CommonMetadata metadata);
    }

}
