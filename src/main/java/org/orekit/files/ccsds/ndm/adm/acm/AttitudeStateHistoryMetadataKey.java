/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.ccsds.ndm.adm.acm;

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;


/** Keys for {@link AttitudeStateHistoryMetadata attitude state history container} entries.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum AttitudeStateHistoryMetadataKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Attitude identification number. */
    ATT_ID((token, context, container) -> token.processAsFreeTextString(container::setAttID)),

    /** Identification number of previous attitude. */
    ATT_PREV_ID((token, context, container) -> token.processAsFreeTextString(container::setAttPrevID)),

    /** Basis of this attitude state time history data. */
    ATT_BASIS((token, context, container) -> token.processAsFreeTextString(container::setAttBasis)),

    /** Identification number of the attitude determination or simulation upon which this attitude is based.*/
    ATT_BASIS_ID((token, context, container) -> token.processAsFreeTextString(container::setAttBasisID)),

    /** Reference frame defining the starting point of the transformation. */
    REF_FRAME_A((token, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameA, context, true, true, false)),

    /** Reference frame defining the end point of the transformation. */
    REF_FRAME_B((token, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameB, context, false, false, true)),

    /** Rotation sequence entry. */
    EULER_ROT_SEQ((token, context, container) -> token.processAsRotationOrder(container::setEulerRotSeq)),

    /** Number of data states included. */
    NUMBER_STATES((token, context, container) -> token.processAsInteger(container::setNbStates)),

    /** Attitude element set type.
     * @see AttitudeElementsType
     */
    ATT_TYPE((token, context, container) -> token.processAsEnum(AttitudeElementsType.class, container::setAttitudeType)),

    /** Attitude rate element set type.
     * @see RateElementsType
     */
    RATE_TYPE((token, context, container) -> token.processAsEnum(RateElementsType.class, container::setRateType));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AttitudeStateHistoryMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final AttitudeStateHistoryMetadata container) {
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
        boolean process(ParseToken token, ContextBinding context, AttitudeStateHistoryMetadata container);
    }

}
