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

import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keys for {@link APMManeuver APM maneuver} entries.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum APMManeuverKey {

    /** Block wrapping element in XML files. */
    maneuverParameters((token, context, data) -> true),

    /** Comment entry. */
    COMMENT((token, context, data) ->
            token.getType() == TokenType.ENTRY ? data.addComment(token.getContent()) : true),

    /** Epoch start entry. */
    MAN_EPOCH_START((token, context, data) -> token.processAsDate(data::setEpochStart, context)),

    /** Duration entry. */
    MAN_DURATION((token, context, data) -> token.processAsDouble(data::setDuration)),

    /** Reference frame entry. */
    MAN_REF_FRAME((token, context, data) -> token.processAsNormalizedString(data::setRefFrameString)),

    /** First torque vector component entry. */
    MAN_TOR_1((token, context, data) -> token.processAsIndexedDouble(data::setTorque, 0)),

    /** Second torque vector component entry. */
    MAN_TOR_2((token, context, data) -> token.processAsIndexedDouble(data::setTorque, 1)),

    /** Third torque vector component entry. */
    MAN_TOR_3((token, context, data) -> token.processAsIndexedDouble(data::setTorque, 2));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    APMManeuverKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context parsing context
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final APMManeuver data) {
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
        boolean process(ParseToken token, ParsingContext context, APMManeuver data);
    }

}
