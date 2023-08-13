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
package org.orekit.files.ccsds.ndm.adm.apm;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;

/** Keys for {@link ApmData APM Euler angles} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum EulerKey {

    /** Rotation angles wrapping element in XML files (ADM V1 only). */
    rotationAngles((token, context, container) -> true),

    /** Rotation rates wrapping element in XML files (ADM V1 only). */
    rotationRates((token, context, container) -> true),

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** First reference frame entry (only for ADM V1). */
    EULER_FRAME_A((token, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameA, context, true, true, true)),

    /** First reference frame entry.
     * @since 12.0
     */
    REF_FRAME_A((token, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameA, context, true, true, true)),

    /** Second reference frame entry (only for ADM V1). */
    EULER_FRAME_B((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.checkNotNull(container.getEndpoints().getFrameA(), EULER_FRAME_A.name());
            final boolean aIsSpaceraftBody = container.getEndpoints().getFrameA().asSpacecraftBodyFrame() != null;
            return token.processAsFrame(container.getEndpoints()::setFrameB, context,
                                        aIsSpaceraftBody, aIsSpaceraftBody, !aIsSpaceraftBody);
        }
        return true;
    }),

    /** Second reference frame entry.
     * @since 12.0
     */
    REF_FRAME_B((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.checkNotNull(container.getEndpoints().getFrameA(), EULER_FRAME_A.name());
            final boolean aIsSpaceraftBody = container.getEndpoints().getFrameA().asSpacecraftBodyFrame() != null;
            return token.processAsFrame(container.getEndpoints()::setFrameB, context,
                                        aIsSpaceraftBody, aIsSpaceraftBody, !aIsSpaceraftBody);
        }
        return true;
    }),

    /** Rotation direction entry (ADM V1 only). */
    EULER_DIR((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.getEndpoints().setA2b(token.getContentAsUppercaseCharacter() == 'A');
        }
        return true;
    }),

    /** Rotation sequence entry. */
    EULER_ROT_SEQ((token, context, container) -> token.processAsRotationOrder(container::setEulerRotSeq)),

    /** Reference frame for rate entry (ADM V1 only). */
    RATE_FRAME((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            final String content = token.getContentAsUppercaseString();
            final char   suffix  = content.charAt(content.length() - 1);
            container.setRateFrameIsA(suffix == 'A');
        }
        return true;
    }),

    /** X body rotation angle entry (ADM V1 only). */
    X_ANGLE((token, context, container) -> token.processAsLabeledDouble('X', Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setLabeledRotationAngle)),

    /** Y body rotation angle entry (ADM V1 only). */
    Y_ANGLE((token, context, container) -> token.processAsLabeledDouble('Y', Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setLabeledRotationAngle)),

    /** Z body rotation angle entry (ADM V1 only). */
    Z_ANGLE((token, context, container) -> token.processAsLabeledDouble('Z', Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setLabeledRotationAngle)),

    /** X body rotation rate entry (ADM V1 only). */
    X_RATE((token, context, container) -> token.processAsLabeledDouble('X', Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setLabeledRotationRate)),

    /** Y body rotation rate entry (ADM V1 only). */
    Y_RATE((token, context, container) -> token.processAsLabeledDouble('Y', Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setLabeledRotationRate)),

    /** Z body rotation rate entry (ADM V1 only). */
    Z_RATE((token, context, container) -> token.processAsLabeledDouble('Z', Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                       container::setLabeledRotationRate)),

    /** First body rotation angle entry.
     * @since 12.0
     */
    ANGLE_1((token, context, container) -> token.processAsIndexedDouble(0, Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setIndexedRotationAngle)),

    /** Second body rotation angle entry.
     * @since 12.0
     */
    ANGLE_2((token, context, container) -> token.processAsIndexedDouble(1, Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setIndexedRotationAngle)),

    /** Third body rotation angle entry.
     * @since 12.0
     */
    ANGLE_3((token, context, container) -> token.processAsIndexedDouble(2, Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                        container::setIndexedRotationAngle)),

    /** First body rotation rate entry.
     * @since 12.0
     */
    ANGLE_1_DOT((token, context, container) -> token.processAsIndexedDouble(0, Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                            container::setIndexedRotationRate)),

    /** Second body rotation rate entry.
     * @since 12.0
     */
    ANGLE_2_DOT((token, context, container) -> token.processAsIndexedDouble(1, Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                            container::setIndexedRotationRate)),

    /** Third body rotation rate entry.
     * @since 12.0
     */
    ANGLE_3_DOT((token, context, container) -> token.processAsIndexedDouble(2, Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                            container::setIndexedRotationRate));

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
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final Euler container) {
        return processor.process(token, context, container);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param container container to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, Euler container);
    }

}
