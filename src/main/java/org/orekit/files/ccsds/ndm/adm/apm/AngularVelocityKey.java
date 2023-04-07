/* Copyright 2023 Luc Maisonobe
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

/** Keys for {@link AngularVelocity APM angular velocity} entries.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum AngularVelocityKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** First reference frame entry. */
    REF_FRAME_A((token, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameA, context, true, true, true)),

    /** Second reference frame entry. */
    REF_FRAME_B((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.checkNotNull(container.getEndpoints().getFrameA(), REF_FRAME_A.name());
            final boolean aIsSpaceraftBody = container.getEndpoints().getFrameA().asSpacecraftBodyFrame() != null;
            return token.processAsFrame(container.getEndpoints()::setFrameB, context,
                                        aIsSpaceraftBody, aIsSpaceraftBody, !aIsSpaceraftBody);
        }
        return true;
    }),

    /** Frame in which angular velocity is defined. */
    ANGVEL_FRAME((token, context, container) -> token.processAsFrame(container::setFrame, context, true, true, true)),

    /** Angular velocity around X axis. */
    ANGVEL_X((token, context, container) -> token.processAsDouble(Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                  container::setAngVelX)),

    /** Angular velocity around Y axis. */
    ANGVEL_Y((token, context, container) -> token.processAsDouble(Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                  container::setAngVelY)),

    /** Angular velocity around Z axis. */
    ANGVEL_Z((token, context, container) -> token.processAsDouble(Units.DEG_PER_S, context.getParsedUnitsBehavior(),
                                                                  container::setAngVelZ));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AngularVelocityKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final AngularVelocity container) {
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
        boolean process(ParseToken token, ContextBinding context, AngularVelocity container);
    }

}
