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

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;

/** Keys for {@link SpinStabilized APM spin-stabilized} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum SpinStabilizedKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** First reference frame entry. */
    SPIN_FRAME_A((token, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameA, context, true, true, true)),

    /** Second reference frame entry. */
    SPIN_FRAME_B((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.checkNotNull(container.getEndpoints().getFrameA(), SPIN_FRAME_A);
            final boolean aIsSpaceraftBody = container.getEndpoints().getFrameA().asSpacecraftBodyFrame() != null;
            return token.processAsFrame(container.getEndpoints()::setFrameB, context,
                                        aIsSpaceraftBody, aIsSpaceraftBody, !aIsSpaceraftBody);
        }
        return true;
    }),

    /** Rotation direction entry. */
    SPIN_DIR((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.getEndpoints().setA2b(token.getContentAsUppercaseCharacter() == 'A');
        }
        return true;
    }),

    /** Spin right ascension entry. */
    SPIN_ALPHA((token, context, container) -> token.processAsAngle(container::setSpinAlpha)),

    /** Spin declination entry. */
    SPIN_DELTA((token, context, container) -> token.processAsAngle(container::setSpinDelta)),

    /** Spin phase entry. */
    SPIN_ANGLE((token, context, container) -> token.processAsAngle(container::setSpinAngle)),

    /** Spin angular velocity entry. */
    SPIN_ANGLE_VEL((token, context, container) -> token.processAsAngle(container::setSpinAngleVel)),

    /** Nutation angle entry. */
    NUTATION((token, context, container) -> token.processAsAngle(container::setNutation)),

    /** Nutation period entry. */
    NUTATION_PER((token, context, container) -> token.processAsDouble(Unit.SECOND, container::setNutationPeriod)),

    /** Nutation phase entry. */
    NUTATION_PHASE((token, context, container) -> token.processAsAngle(container::setNutationPhase));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    SpinStabilizedKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final SpinStabilized container) {
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
        boolean process(ParseToken token, ContextBinding context, SpinStabilized container);
    }

}
