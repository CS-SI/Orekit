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
package org.orekit.files.ccsds.ndm.adm.aem;

import org.orekit.files.ccsds.ndm.adm.AdmMetadataKey;
import org.orekit.files.ccsds.ndm.adm.AdmParser;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.ParsingContext;


/** Keys for {@link AemMetadata AEM metadata} entries.
 * <p>
 * Additional metadata are also listed in {@link AdmMetadataKey}.
 * </p>
 * @author Bryan Cazabonne
 * @since 11.0
 */
public enum AemMetadataKey {

    /** First reference frame. */
    REF_FRAME_A((token, context, metadata) -> token.processAsFrame(metadata.getEndpoints()::setFrameA, context, true, true, true)),

    /** Second reference frame. */
    REF_FRAME_B((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            metadata.checkNotNull(metadata.getEndpoints().getFrameA(), REF_FRAME_A);
            final boolean aIsSpaceraftBody = metadata.getEndpoints().getFrameA().asSpacecraftBodyFrame() != null;
            return token.processAsFrame(metadata.getEndpoints()::setFrameB, context,
                                        aIsSpaceraftBody, aIsSpaceraftBody, !aIsSpaceraftBody);
        }
        return true;
    }),

    /** Rotation direction entry. */
    ATTITUDE_DIR((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            metadata.getEndpoints().setA2b(token.getContentAsNormalizedCharacter() == 'A');
        }
        return true;
    }),

    /** Start time entry. */
    START_TIME((token, context, metadata) -> token.processAsDate(metadata::setStartTime, context)),

    /** Stop time entry. */
    STOP_TIME((token, context, metadata) -> token.processAsDate(metadata::setStopTime, context)),

    /** Useable start time entry. */
    USEABLE_START_TIME((token, context, metadata) -> token.processAsDate(metadata::setUseableStartTime, context)),

    /** Useable stop time entry. */
    USEABLE_STOP_TIME((token, context, metadata) -> token.processAsDate(metadata::setUseableStopTime, context)),

    /** Format of the data line entry. */
    ATTITUDE_TYPE((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                metadata.setAttitudeType(AemAttitudeType.parseType(token.getContent()));
                return true;
            } catch (IllegalArgumentException iae) {
                return false;
            }
        }
        return true;
    }),

    /** Placement of the scalar component in quaternion entry. */
    QUATERNION_TYPE((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            metadata.setIsFirst("FIRST".equals(token.getContentAsNormalizedString()));
        }
        return true;
    }),

    /** Rotation order entry for Euler angles. */
    EULER_ROT_SEQ((token, context, metadata) -> AdmParser.processRotationOrder(token, metadata::setEulerRotSeq)),

    /** Reference frame for Euler rates. */
    RATE_FRAME((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            final String content = token.getContentAsNormalizedString();
            final char   suffix  = content.charAt(content.length() - 1);
            metadata.setRateFrameIsA(suffix == 'A');
        }
        return true;
    }),

    /** Interpolation method in ephemeris. */
    INTERPOLATION_METHOD((token, context, metadata) -> token.processAsNormalizedString(metadata::setInterpolationMethod)),

    /** Interpolation degree in ephemeris. */
    INTERPOLATION_DEGREE((token, context, metadata) -> token.processAsInteger(metadata::setInterpolationDegree));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AemMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context parsing context
     * @param metadata metadata to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final AemMetadata metadata) {
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
        boolean process(ParseToken token, ParsingContext context, AemMetadata metadata);
    }

}
