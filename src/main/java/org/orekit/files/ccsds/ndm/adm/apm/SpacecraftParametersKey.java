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

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keys for {@link SpacecraftParameters APM spacecraft parameters} entries.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum SpacecraftParametersKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Inertia reference frame entry. */
    INERTIA_REF_FRAME((token, context, container) -> token.processAsFrame(container::setInertiaReferenceFrame,
                                                                          context, false, false, true)),

    /** 1-axis moment of inertia entry. */
    I11((token, context, container) -> token.processAsDouble(Units.KG_M2, context.getParsedUnitsBehavior(),
                                                             container::setI11)),

    /** 2-axis moment of inertia entry. */
    I22((token, context, container) -> token.processAsDouble(Units.KG_M2, context.getParsedUnitsBehavior(),
                                                             container::setI22)),

    /** 3-axis moment of inertia entry. */
    I33((token, context, container) -> token.processAsDouble(Units.KG_M2, context.getParsedUnitsBehavior(),
                                                             container::setI33)),

    /** 1-axis / 2-axis inertia cross product entry. */
    I12((token, context, container) -> token.processAsDouble(Units.KG_M2, context.getParsedUnitsBehavior(),
                                                             container::setI12)),

    /** 1-axis / 3-axis inertia cross product entry. */
    I13((token, context, container) -> token.processAsDouble(Units.KG_M2, context.getParsedUnitsBehavior(),
                                                             container::setI13)),

    /** 2-axis / 3-axis inertia cross product entry. */
    I23((token, context, container) -> token.processAsDouble(Units.KG_M2, context.getParsedUnitsBehavior(),
                                                             container::setI23));

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
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final SpacecraftParameters container) {
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
        boolean process(ParseToken token, ContextBinding context, SpacecraftParameters container);
    }

}
