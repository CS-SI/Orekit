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

import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.ParsingContext;

/** Keys for {@link SpinStabilized APM spin-stabilized} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum SpinStabilizedKey {

    /** Block wrapping element in XML files. */
    eulerElementsSpin((token, context, data) -> true),

    /** Comment entry. */
    COMMENT((token, context, data) ->
            token.getType() == TokenType.ENTRY ? data.addComment(token.getContent()) : true),

    /** First reference frame entry. */
    SPIN_FRAME_A((token, context, data) -> token.processAsNormalizedString(data.getEndPoints()::setFrameA)),

    /** Second reference frame entry. */
    SPIN_FRAME_B((token, context, data) -> token.processAsNormalizedString(data.getEndPoints()::setFrameB)),

    /** Rotation direction entry. */
    SPIN_DIR((token, context, data) -> token.processAsNormalizedString(data.getEndPoints()::setDirection)),

    /** Spin right ascension entry. */
    SPIN_ALPHA((token, context, data) -> token.processAsAngle(data::setSpinAlpha)),

    /** Spin declination entry. */
    SPIN_DELTA((token, context, data) -> token.processAsAngle(data::setSpinDelta)),

    /** Spin phase entry. */
    SPIN_ANGLE((token, context, data) -> token.processAsAngle(data::setSpinAngle)),

    /** Spin angular velocity entry. */
    SPIN_ANGLE_VEL((token, context, data) -> token.processAsAngle(data::setSpinAngleVel)),

    /** Nutation angle entry. */
    NUTATION((token, context, data) -> token.processAsAngle(data::setNutation)),

    /** Nutation period entry. */
    NUTATION_PER((token, context, data) -> token.processAsDouble(1.0, data::setNutationPeriod)),

    /** Nutation phase entry. */
    NUTATION_PHASE((token, context, data) -> token.processAsAngle(data::setNutationPhase));

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
     * @param context parsing context
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final SpinStabilized data) {
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
        boolean process(ParseToken token, ParsingContext context, SpinStabilized data);
    }

}
