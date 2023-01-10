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
package org.orekit.files.ccsds.ndm.cdm;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;

/** Keys for {@link StateVector CDM state vector} entries.
 * @author Melina Vanel
 * @since 11.2
 */
public enum StateVectorKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Object Position Vector X component. */
    X((token, context, container) -> token.processAsDouble(Unit.KILOMETRE, context.getParsedUnitsBehavior(),
                                                             container::setX)),
    /** Object Position Vector Y component. */
    Y((token, context, container) -> token.processAsDouble(Unit.KILOMETRE, context.getParsedUnitsBehavior(),
                                                             container::setY)),
    /** Object Position Vector Z component. */
    Z((token, context, container) -> token.processAsDouble(Unit.KILOMETRE, context.getParsedUnitsBehavior(),
                                                             container::setZ)),
    /** Object Velocity Vector X component. */
    X_DOT((token, context, container) -> token.processAsDouble(Units.KM_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setXdot)),
    /** Object Velocity Vector Y component. */
    Y_DOT((token, context, container) -> token.processAsDouble(Units.KM_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setYdot)),
    /** Object Velocity Vector Z component. */
    Z_DOT((token, context, container) -> token.processAsDouble(Units.KM_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setZdot));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    StateVectorKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final StateVector container) {
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
        boolean process(ParseToken token, ContextBinding context, StateVector container);
    }

}
