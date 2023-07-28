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
package org.orekit.files.ccsds.ndm.odm;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;


/** Keys for {@link StateVector ODM state vector container} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum StateVectorKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Epoch of state vector and optional Keplerian elements. */
    EPOCH((token, context, container) -> token.processAsDate(container::setEpoch, context)),

    /** Position vector X-component. */
    X((token, context, container) -> token.processAsIndexedDouble(0, Unit.KILOMETRE, context.getParsedUnitsBehavior(),
                                                                  container::setP)),

    /** Position vector Y-component. */
    Y((token, context, container) -> token.processAsIndexedDouble(1, Unit.KILOMETRE, context.getParsedUnitsBehavior(),
                                                                  container::setP)),

    /** Position vector Z-component. */
    Z((token, context, container) -> token.processAsIndexedDouble(2, Unit.KILOMETRE, context.getParsedUnitsBehavior(),
                                                                  container::setP)),

    /** Velocity vector X-component. */
    X_DOT((token, context, container) -> token.processAsIndexedDouble(0, Units.KM_PER_S, context.getParsedUnitsBehavior(),
                                                                      container::setV)),

    /** Velocity vector Y-component. */
    Y_DOT((token, context, container) -> token.processAsIndexedDouble(1, Units.KM_PER_S, context.getParsedUnitsBehavior(),
                                                                      container::setV)),

    /** Velocity vector Z-component. */
    Z_DOT((token, context, container) -> token.processAsIndexedDouble(2, Units.KM_PER_S, context.getParsedUnitsBehavior(),
                                                                      container::setV)),

    /** Acceleration vector X-component. */
    X_DDOT((token, context, container) -> token.processAsIndexedDouble(0, Units.KM_PER_S2, context.getParsedUnitsBehavior(),
                                                                       container::setA)),

    /** Acceleration vector Y-component. */
    Y_DDOT((token, context, container) -> token.processAsIndexedDouble(1, Units.KM_PER_S2, context.getParsedUnitsBehavior(),
                                                                       container::setA)),

    /** Acceleration vector Z-component. */
    Z_DDOT((token, context, container) -> token.processAsIndexedDouble(2, Units.KM_PER_S2, context.getParsedUnitsBehavior(),
                                                                       container::setA));

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
