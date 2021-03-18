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
package org.orekit.files.ccsds.ndm.adm.apm;

import org.orekit.files.ccsds.ndm.adm.AdmParser;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.ParsingContext;

/** Keys for {@link ApmData APM Euler angles} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum EulerKey {

    /** Rotation angles wrapping element in XML files. */
    rotationAngles((token, context, container) -> true),

    /** Rotation rates wrapping element in XML files. */
    rotationRates((token, context, container) -> true),

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** First reference frame entry. */
    EULER_FRAME_A((token, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameA, context, true, true, true)),

    /** Second reference frame entry. */
    EULER_FRAME_B((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.checkNotNull(container.getEndpoints().getFrameA(), EULER_FRAME_A);
            final boolean aIsSpaceraftBody = container.getEndpoints().getFrameA().asSpacecraftBodyFrame() != null;
            return token.processAsFrame(container.getEndpoints()::setFrameB, context,
                                        aIsSpaceraftBody, aIsSpaceraftBody, !aIsSpaceraftBody);
        }
        return true;
    }),

    /** Rotation direction entry. */
    EULER_DIR((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.getEndpoints().setA2b(token.getContentAsUppercaseCharacter() == 'A');
        }
        return true;
    }),

    /** Rotation sequence entry. */
    EULER_ROT_SEQ((token, context, container) -> AdmParser.processRotationOrder(token, container::setEulerRotSeq)),

    /** Reference frame for rate entry. */
    RATE_FRAME((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            final String content = token.getContentAsUppercaseString();
            final char   suffix  = content.charAt(content.length() - 1);
            container.setRateFrameIsA(suffix == 'A');
        }
        return true;
    }),

    /** X body rotation angle entry. */
    X_ANGLE((token, context, container) -> (token.getType() == TokenType.ENTRY) ? container.setRotationAngle('X', token.getContentAsAngle()) : true),

    /** Y body rotation angle entry. */
    Y_ANGLE((token, context, container) -> (token.getType() == TokenType.ENTRY) ? container.setRotationAngle('Y', token.getContentAsAngle()) : true),

    /** Z body rotation angle entry. */
    Z_ANGLE((token, context, container) -> (token.getType() == TokenType.ENTRY) ? container.setRotationAngle('Z', token.getContentAsAngle()) : true),

    /** X body rotation rate entry. */
    X_RATE((token, context, container) -> (token.getType() == TokenType.ENTRY) ? container.setRotationRate('X', token.getContentAsAngle()) : true),

    /** Y body rotation rate entry. */
    Y_RATE((token, context, container) -> (token.getType() == TokenType.ENTRY) ? container.setRotationRate('Y', token.getContentAsAngle()) : true),

    /** Z body rotation rate entry. */
    Z_RATE((token, context, container) -> (token.getType() == TokenType.ENTRY) ? container.setRotationRate('Z', token.getContentAsAngle()) : true);

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    EulerKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context parsing context
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final Euler container) {
        return processor.process(token, context, container);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param container container to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ParsingContext context, Euler container);
    }

}
