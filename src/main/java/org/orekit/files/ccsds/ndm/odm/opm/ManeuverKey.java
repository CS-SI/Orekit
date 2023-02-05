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
package org.orekit.files.ccsds.ndm.odm.opm;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;


/** Keys for {@link Maneuver OPM maneuver} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum ManeuverKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Epoch of ignition. */
    MAN_EPOCH_IGNITION((token, context, container) -> token.processAsDate(container::setEpochIgnition, context)),

    /** Coordinate system for velocity increment vector. */
    MAN_REF_FRAME((token, context, container) -> token.processAsFrame(container::setReferenceFrame, context, true, true, false)),

    /** Maneuver duration (0 for impulsive maneuvers). */
    MAN_DURATION((token, context, container) -> token.processAsDouble(Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                      container::setDuration)),

    /** Mass change during maneuver (value is &lt; 0). */
    MAN_DELTA_MASS((token, context, container) -> token.processAsDouble(Unit.KILOGRAM, context.getParsedUnitsBehavior(),
                                                                        container::setDeltaMass)),

    /** First component of the velocity increment. */
    MAN_DV_1((token, context, container) -> token.processAsIndexedDouble(0, Units.KM_PER_S, context.getParsedUnitsBehavior(),
                                                                         container::setDV)),

    /** Second component of the velocity increment. */
    MAN_DV_2((token, context, container) -> token.processAsIndexedDouble(1, Units.KM_PER_S, context.getParsedUnitsBehavior(),
                                                                         container::setDV)),

    /** Third component of the velocity increment. */
    MAN_DV_3((token, context, container) -> token.processAsIndexedDouble(2, Units.KM_PER_S, context.getParsedUnitsBehavior(),
                                                                         container::setDV));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    ManeuverKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final Maneuver container) {
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
        boolean process(ParseToken token, ContextBinding context, Maneuver container);
    }

}
