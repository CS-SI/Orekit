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

/** Keys for {@link SpacecraftParameters APM spacecraft parameters} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum SpacecraftParametersKey {

    /** Block wrapping element in XML files. */
    spacecraftParameters((token, context, data) -> true),

    /** Comment entry. */
    COMMENT((token, context, data) ->
            token.getType() == TokenType.ENTRY ? data.addComment(token.getContent()) : true),

    /** Inertia reference frame entry. */
    INERTIA_REF_FRAME((token, context, data) -> token.processAsNormalizedString(data::setInertiaRefFrameString)),

    /** 1-axis moment of inertia entry. */
    I11((token, context, data) -> token.processAsDouble(1.0, data::setI11)),

    /** 2-axis moment of inertia entry. */
    I22((token, context, data) -> token.processAsDouble(1.0, data::setI22)),

    /** 3-axis moment of inertia entry. */
    I33((token, context, data) -> token.processAsDouble(1.0, data::setI33)),

    /** 1-axis / 2-axis inertia cross product entry. */
    I12((token, context, data) -> token.processAsDouble(1.0, data::setI12)),

    /** 1-axis / 3-axis inertia cross product entry. */
    I13((token, context, data) -> token.processAsDouble(1.0, data::setI13)),

    /** 2-axis / 3-axis inertia cross product entry. */
    I23((token, context, data) -> token.processAsDouble(1.0, data::setI23));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    SpacecraftParametersKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context parsing context
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final SpacecraftParameters data) {
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
        boolean process(ParseToken token, ParsingContext context, SpacecraftParameters data);
    }

}
