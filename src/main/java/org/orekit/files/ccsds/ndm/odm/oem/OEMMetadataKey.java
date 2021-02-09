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
package org.orekit.files.ccsds.ndm.odm.oem;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;


/** Keys for {@link OEMMetadata OEM metadata} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OEMMetadataKey {

    /** Start time entry. */
    START_TIME((token, context, metadata) -> token.processAsDate(metadata::setStartTime, context)),

    /** Stop time entry. */
    STOP_TIME((token, context, metadata) -> token.processAsDate(metadata::setStopTime, context)),

    /** Useable start time entry. */
    USEABLE_START_TIME((token, context, metadata) -> token.processAsDate(metadata::setUseableStartTime, context)),

    /** Useable stop time entry. */
    USEABLE_STOP_TIME((token, context, metadata) -> token.processAsDate(metadata::setUseableStopTime, context)),

    /** Interpolation method in ephemeris. */
    INTERPOLATION((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                metadata.setInterpolationMethod(InterpolationMethod.valueOf(token.getContentAsNormalizedString()));
            } catch (IllegalArgumentException iae) {
                throw new OrekitException(iae, OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE,
                                          token.getName(), token.getLineNumber(), token.getFileName());
            }
        }
        return true;
    }),

    /** Interpolation degree in ephemeris. */
    INTERPOLATION_DEGREE((token, context, metadata) -> token.processAsInteger(metadata::setInterpolationDegree));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    OEMMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context parsing context
     * @param metadata metadata to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final OEMMetadata metadata) {
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
        boolean process(ParseToken token, ParsingContext context, OEMMetadata metadata);
    }

}
