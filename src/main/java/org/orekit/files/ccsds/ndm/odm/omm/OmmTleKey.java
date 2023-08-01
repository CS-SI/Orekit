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
package org.orekit.files.ccsds.ndm.odm.omm;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;


/** Keys for {@link OmmTle TLE} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OmmTleKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Ephemeris Type, only required if MEAN_ELEMENT_THEORY = SGP/SGP4. */
    EPHEMERIS_TYPE((token, context, container) -> token.processAsInteger(container::setEphemerisType)),

    /** Classifiation type. */
    CLASSIFICATION_TYPE((token, context, container) -> token.processAsNormalizedCharacter(container::setClassificationType)),

    /** NORAD Catalog Number. */
    NORAD_CAT_ID((token, context, container) -> token.processAsInteger(container::setNoradID)),

    /** Element set number for this satellite. */
    ELEMENT_SET_NO((token, context, container) -> token.processAsInteger(container::setElementSetNo)),

    /** Revolution number. */
    REV_AT_EPOCH((token, context, container) -> token.processAsInteger(container::setRevAtEpoch)),

    /** SGP/SGP4 drag-like coefficient. */
    BSTAR((token, context, container) -> token.processAsDouble(Units.ONE_PER_ER, context.getParsedUnitsBehavior(),
                                                               container::setBStar)),

    /** SGP4-XP drag-like coefficient.
     * @since 12.0
     */
    BTERM((token, context, container) -> token.processAsDouble(Units.M2_PER_KG, context.getParsedUnitsBehavior(),
                                                               container::setBTerm)),

    /** First time derivative of mean motion. */
    MEAN_MOTION_DOT((token, context, container) -> token.processAsDouble(Units.REV_PER_DAY2_SCALED, context.getParsedUnitsBehavior(),
                                                                         container::setMeanMotionDot)),

    /** Second time derivative of mean motion. */
    MEAN_MOTION_DDOT((token, context, container) -> token.processAsDouble(Units.REV_PER_DAY3_SCALED, context.getParsedUnitsBehavior(),
                                                                          container::setMeanMotionDotDot)),

    /** SGP4-XP solar radiation pressure-like coefficient.
     * @since 12.0
     */
    AGOM((token, context, container) -> token.processAsDouble(Units.M2_PER_KG, context.getParsedUnitsBehavior(),
                                                              container::setAGoM));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    OmmTleKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final OmmTle container) {
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
        boolean process(ParseToken token, ContextBinding context, OmmTle container);
    }

}
