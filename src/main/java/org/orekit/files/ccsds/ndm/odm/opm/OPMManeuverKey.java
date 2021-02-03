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
package org.orekit.files.ccsds.ndm.odm.opm;

import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.CCSDSFrame;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;


/** Keys for {@link OPMManeuver OPM maneuver} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OPMManeuverKey {

    /** Comment entry. */
    COMMENT((token, context, data) ->
            token.getType() == TokenType.ENTRY ? data.addComment(token.getContent()) : true),

    /** Epoch of ignition. */
    MAN_EPOCH_IGNITION((token, context, data) -> token.processAsDate(data::setEpochIgnition, context)),

    /** Coordinate system for velocity increment vector. */
    MAN_REF_FRAME((token, context, data) -> {
        if (token.getType() == TokenType.ENTRY) {
            final CCSDSFrame manFrame = CCSDSFrame.parse(token.getContent());
            if (manFrame.isLof()) {
                data.setRefLofType(manFrame.getLofType());
            } else {
                data.setRefFrame(manFrame.getFrame(context.getConventions(),
                                                   context.isSimpleEOP(),
                                                   context.getDataContext()));
            }
        }
        return true;
    }),

    /** Maneuver duration (0 for impulsive maneuvers). */
    MAN_DURATION((token, context, data) -> token.processAsDouble(1.0, data::setDuration)),

    /** Mass change during maneuver (value is &lt; 0). */
    MAN_DELTA_MASS((token, context, data) -> token.processAsDouble(1.0, data::setDeltaMass)),

    /** First component of the velocity increment. */
    MAN_DV_1((token, context, data) -> token.processAsIndexedDouble(0, 1.0e3, data::setDV)),

    /** Second component of the velocity increment. */
    MAN_DV_2((token, context, data) -> token.processAsIndexedDouble(1, 1.0e3, data::setDV)),

    /** Third component of the velocity increment. */
    MAN_DV_3((token, context, data) -> token.processAsIndexedDouble(2, 1.0e3, data::setDV));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    OPMManeuverKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context parsing context
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final OPMManeuver data) {
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
        boolean process(ParseToken token, ParsingContext context, OPMManeuver data);
    }

}
