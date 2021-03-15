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

import org.hipparchus.util.FastMath;
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
    rotationAngles((token, context, data) -> {
        data.setInRotationAngles(token.getType() == TokenType.START);
        return true;
    }),

    /** Rotation rates wrapping element in XML files. */
    rotationRates((token, context, data) -> true),

    /** First rotation angle or first rotation rate. */
    rotation1((token, context, data) -> {
        if (token.getType() == TokenType.ENTRY) {
            if (data.inRotationAngles()) {
                data.setRotationAngle(0, FastMath.toRadians(token.getContentAsDouble()));
            } else {
                data.setRotationRate(0, FastMath.toRadians(token.getContentAsDouble()));
            }
        }
        return true;
    }),

    /** Second rotation angle or second rotation rate. */
    rotation2((token, context, data) -> {
        if (token.getType() == TokenType.ENTRY) {
            if (data.inRotationAngles()) {
                data.setRotationAngle(1, FastMath.toRadians(token.getContentAsDouble()));
            } else {
                data.setRotationRate(1, FastMath.toRadians(token.getContentAsDouble()));
            }
        }
        return true;
    }),

    /** Third rotation angle or third rotation rate. */
    rotation3((token, context, data) -> {
        if (token.getType() == TokenType.ENTRY) {
            if (data.inRotationAngles()) {
                data.setRotationAngle(2, FastMath.toRadians(token.getContentAsDouble()));
            } else {
                data.setRotationRate(2, FastMath.toRadians(token.getContentAsDouble()));
            }
        }
        return true;
    }),

    /** Comment entry. */
    COMMENT((token, context, data) ->
            token.getType() == TokenType.ENTRY ? data.addComment(token.getContentAsNormalizedString()) : true),

    /** First reference frame entry. */
    EULER_FRAME_A((token, context, data) -> token.processAsFrame(data.getEndpoints()::setFrameA, context, true, true, true)),

    /** Second reference frame entry. */
    EULER_FRAME_B((token, context, data) -> {
        if (token.getType() == TokenType.ENTRY) {
            data.checkNotNull(data.getEndpoints().getFrameA(), EULER_FRAME_A);
            final boolean aIsSpaceraftBody = data.getEndpoints().getFrameA().asSpacecraftBodyFrame() != null;
            return token.processAsFrame(data.getEndpoints()::setFrameB, context,
                                        aIsSpaceraftBody, aIsSpaceraftBody, !aIsSpaceraftBody);
        }
        return true;
    }),

    /** Rotation direction entry. */
    EULER_DIR((token, context, data) -> {
        if (token.getType() == TokenType.ENTRY) {
            data.getEndpoints().setA2b(token.getContentAsUppercaseCharacter() == 'A');
        }
        return true;
    }),

    /** Rotation sequence entry. */
    EULER_ROT_SEQ((token, context, data) -> AdmParser.processRotationOrder(token, data::setEulerRotSeq)),

    /** Reference frame for rate entry. */
    RATE_FRAME((token, context, data) -> token.processAsUppercaseString(data::setRateFrameString)),

    /** X body rotation angle entry. */
    X_ANGLE((token, context, data) -> (token.getType() == TokenType.ENTRY) ? data.setRotationAngle('X', token.getContentAsAngle()) : true),

    /** Y body rotation angle entry. */
    Y_ANGLE((token, context, data) -> (token.getType() == TokenType.ENTRY) ? data.setRotationAngle('Y', token.getContentAsAngle()) : true),

    /** Z body rotation angle entry. */
    Z_ANGLE((token, context, data) -> (token.getType() == TokenType.ENTRY) ? data.setRotationAngle('Z', token.getContentAsAngle()) : true),

    /** X body rotation rate entry. */
    X_RATE((token, context, data) -> (token.getType() == TokenType.ENTRY) ? data.setRotationRate('X', token.getContentAsAngle()) : true),

    /** Y body rotation rate entry. */
    Y_RATE((token, context, data) -> (token.getType() == TokenType.ENTRY) ? data.setRotationRate('Y', token.getContentAsAngle()) : true),

    /** Z body rotation rate entry. */
    Z_RATE((token, context, data) -> (token.getType() == TokenType.ENTRY) ? data.setRotationRate('Z', token.getContentAsAngle()) : true);

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
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final Euler data) {
        return processor.process(token, context, data);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param data data to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ParsingContext context, Euler data);
    }

}
