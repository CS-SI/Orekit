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
package org.orekit.files.ccsds.ndm.odm.omm;

import org.hipparchus.util.FastMath;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;


/** Keys for {@link OMMTLE TLE} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OMMTLEKey {

    /** Ephemeris Type, only required if MEAN_ELEMENT_THEORY = SGP/SGP4. */
    EPHEMERIS_TYPE((token, context, data) -> token.processAsInteger(data::setEphemerisType)),

    /** Classifiation type. */
    CLASSIFICATION_TYPE((token, context, data) -> token.processAsNormalizedCharacter(data::setClassificationType)),

    /** NORAD Catalog Number. */
    NORAD_CAT_ID((token, context, data) -> token.processAsInteger(data::setNoradID)),

    /** Element set number for this satellite. */
    ELEMENT_SET_NO((token, context, data) -> token.processAsInteger(data::setElementSetNo)),

    /** Revolution number. */
    REV_AT_EPOCH((token, context, data) -> token.processAsInteger(data::setRevAtEpoch)),

    /** SGP/SGP4 drag-like coefficient. */
    BSTAR((token, context, data) -> token.processAsDouble(1.0, data::setBStar)),

    /** First time derivative of mean motion. */
    MEAN_MOTION_DOT((token, context, data) ->
                    token.processAsDouble(FastMath.PI / 1.86624e9, data::setMeanMotionDot)),

    /** Second time derivative of mean motion. */
    MEAN_MOTION_DDOT((token, context, data) ->
                     token.processAsDouble(FastMath.PI / 5.3747712e13, data::setMeanMotionDotDot));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    OMMTLEKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context parsing context
     * @param data data to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final OMMTLE data) {
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
        boolean process(ParseToken token, ParsingContext context, OMMTLE data);
    }

}
