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
package org.orekit.files.ccsds.ndm.odm.ocm;

import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.ParsingContext;


/** Keys for {@link CovarianceHistoryMetadata covariance history metadata} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum CovarianceHistoryMetadataKey {

    /** Comment entry. */
    COMMENT((token, context, metadata) ->
            token.getType() == TokenType.ENTRY ? metadata.addComment(token.getContent()) : true),

    /** Covariance identification number. */
    COV_ID((token, context, metadata) -> token.processAsNormalizedString(metadata::setCovID)),

    /** Identification number of previous covariance. */
    COV_PREV_ID((token, context, metadata) -> token.processAsNormalizedString(metadata::setCovPrevID)),

    /** Identification number of next covariance. */
    COV_NEXT_ID((token, context, metadata) -> token.processAsNormalizedString(metadata::setCovNextID)),

    /** Basis of this covariance time history data. */
    COV_BASIS((token, context, metadata) -> token.processAsNormalizedString(metadata::setCovBasis)),

    /** Identification number of the orbit determination or simulation upon which this covariance is based.*/
    COV_BASIS_ID((token, context, metadata) -> token.processAsNormalizedString(metadata::setCovBasisID)),

    /** Reference frame of the covariance. */
    COV_REF_FRAME((token, context, metadata) -> token.processAsFrame(metadata::setCovReferenceFrame, context, true, false, false)),

    /** Epoch of the {@link #COV_REF_FRAME covariance reference frame}. */
    COV_FRAME_EPOCH((token, context, metadata) -> token.processAsDate(metadata::setCovFrameEpoch, context)),

    /** Minimum scale factor to apply to achieve realism. */
    COV_SCALE_MIN((token, context, metadata) -> token.processAsDouble(1.0, metadata::setCovScaleMin)),

    /** Maximum scale factor to apply to achieve realism. */
    COV_SCALE_MAX((token, context, metadata) -> token.processAsDouble(1.0, metadata::setCovScaleMax)),

    /** Masure of confidence in covariance error matching reality. */
    COV_CONFIDENCE((token, context, metadata) -> token.processAsFreeTextString(metadata::setCovConfidence)),

    /** Covariance element set type.
     * @see ElementsType
     */
    COV_TYPE((token, context, metadata) -> {
        try {
            metadata.setCovType(ElementsType.valueOf(token.getContentAsNormalizedString()));
        } catch (IllegalArgumentException iae) {
            throw token.generateException(iae);
        }
        return true;
    }),

    /** SI units for each elements of the covariance. */
    COV_UNITS((token, context, metadata) -> token.processAsUnitList(metadata::setCovUnits));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    CovarianceHistoryMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context parsing context
     * @param metadata metadata to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final CovarianceHistoryMetadata metadata) {
        return processor.process(token, context, metadata);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param metadata metadata to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ParsingContext context, CovarianceHistoryMetadata metadata);
    }

}
