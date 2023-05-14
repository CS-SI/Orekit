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

import org.orekit.files.ccsds.definitions.AdMethodType;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;


/** Keys for {@link AttitudeDetermination attitude determination data} entries.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum AttitudeDeterminationKey {

    /** Comment entry. */
    COMMENT((token, parser, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Identification number. */
    AD_ID((token, parser, context, container) -> token.processAsFreeTextString(container::setId)),

    /** Identification of previous attitude determination. */
    AD_PREV_ID((token, parser, context, container) -> token.processAsFreeTextString(container::setPrevId)),

    /** Attitude determination method. */
    AD_METHOD((token, parser, context, container) -> token.processAsEnum(AdMethodType.class, container::setMethod)),

    /** Source of attitude estimate. */
    ATTITUDE_SOURCE((token, parser, context, container) -> token.processAsFreeTextString(container::setSource)),

    /** Rotation sequence entry. */
    EULER_ROT_SEQ((token, parser, context, container) -> token.processAsRotationOrder(container::setEulerRotSeq)),

    /** Number of states. */
    NUMBER_STATES((token, parser, context, container) -> token.processAsInteger(container::setNbStates)),

    /** Attitude states. */
    ATTITUDE_STATES((token, parser, context, container) -> token.processAsEnum(AttitudeElementsType.class, container::setAttitudeStates)),

    /** Type of attitude error state. */
    COV_TYPE((token, parser, context, container) -> token.processAsEnum(AttitudeCovarianceType.class, container::setCovarianceType)),

    /** Reference frame defining the starting point of the transformation. */
    REF_FRAME_A((token, parser, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameA, context, true, true, false)),

    /** Reference frame defining the end point of the transformation. */
    REF_FRAME_B((token, parser, context, container) -> token.processAsFrame(container.getEndpoints()::setFrameB, context, false, false, true)),

    /** Attitude rate states. */
    RATE_STATES((token, parser, context, container) -> token.processAsEnum(RateElementsType.class, container::setRateStates)),

    /** Rate random walk if {@link #RATE_STATES} is {@link RateElementsType#GYRO_BIAS}. */
    SIGMA_U((token, parser, context, container) -> token.processAsDouble(Units.DEG_PER_S_3_2, context.getParsedUnitsBehavior(),
                                                                         container::setSigmaU)),

    /** Angle random walk if {@link #RATE_STATES} is {@link RateElementsType#GYRO_BIAS}. */
    SIGMA_V((token, parser, context, container) -> token.processAsDouble(Units.DEG_PER_S_1_2, context.getParsedUnitsBehavior(),
                                                                         container::setSigmaV)),

    /** Process noise standard deviation if {@link #RATE_STATES} is {@link RateElementsType#ANGVEL}. */
    RATE_PROCESS_NOISE_STDDEV((token, parser, context, container) -> token.processAsDouble(Units.DEG_PER_S_3_2, context.getParsedUnitsBehavior(),
                                                                                           container::setRateProcessNoiseStdDev)),

    /** Sensor block sub-structure. */
    SENSOR((token, parser, context, container) -> parser.manageAttitudeDeterminationSensorSection(token.getType() == TokenType.START));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AttitudeDeterminationKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param parser ACM file parser
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final AcmParser parser, final ContextBinding context,
                           final AttitudeDetermination container) {
        return processor.process(token, parser, context, container);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param parser ACM file parser
         * @param context context binding
         * @param container container to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, AcmParser parser, ContextBinding context, AttitudeDetermination container);
    }

}
