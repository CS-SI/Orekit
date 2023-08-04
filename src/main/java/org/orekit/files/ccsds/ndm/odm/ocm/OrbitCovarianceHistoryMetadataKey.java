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
package org.orekit.files.ccsds.ndm.odm.ocm;

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;


/** Keys for {@link OrbitCovarianceHistoryMetadata covariance history container} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OrbitCovarianceHistoryMetadataKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Covariance identification number. */
    COV_ID((token, context, container) -> token.processAsFreeTextString(container::setCovID)),

    /** Identification number of previous covariance. */
    COV_PREV_ID((token, context, container) -> token.processAsFreeTextString(container::setCovPrevID)),

    /** Identification number of next covariance. */
    COV_NEXT_ID((token, context, container) -> token.processAsFreeTextString(container::setCovNextID)),

    /** Basis of this covariance time history data. */
    COV_BASIS((token, context, container) -> token.processAsFreeTextString(container::setCovBasis)),

    /** Identification number of the orbit determination or simulation upon which this covariance is based.*/
    COV_BASIS_ID((token, context, container) -> token.processAsFreeTextString(container::setCovBasisID)),

    /** Reference frame of the covariance. */
    COV_REF_FRAME((token, context, container) -> token.processAsFrame(container::setCovReferenceFrame, context, true, true, false)),

    /** Epoch of the {@link #COV_REF_FRAME covariance reference frame}. */
    COV_FRAME_EPOCH((token, context, container) -> token.processAsDate(container::setCovFrameEpoch, context)),

    /** Minimum scale factor to apply to achieve realism. */
    COV_SCALE_MIN((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                       container::setCovScaleMin)),

    /** Maximum scale factor to apply to achieve realism. */
    COV_SCALE_MAX((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                       container::setCovScaleMax)),

    /** Masure of confidence in covariance error matching reality. */
    COV_CONFIDENCE((token, context, container) -> token.processAsDouble(Unit.PERCENT, context.getParsedUnitsBehavior(),
                                                                        container::setCovConfidence)),

    /** Covariance element set type.
     * @see OrbitElementsType
     */
    COV_TYPE((token, context, container) -> token.processAsEnum(OrbitElementsType.class, container::setCovType)),

    /** Covariance ordering. */
    COV_ORDERING((token, context, container) -> token.processAsEnum(Ordering.class, container::setCovOrdering)),

    /** SI units for each elements of the covariance. */
    COV_UNITS((token, context, container) -> token.processAsUnitList(container::setCovUnits));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    OrbitCovarianceHistoryMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final OrbitCovarianceHistoryMetadata container) {
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
        boolean process(ParseToken token, ContextBinding context, OrbitCovarianceHistoryMetadata container);
    }

}
