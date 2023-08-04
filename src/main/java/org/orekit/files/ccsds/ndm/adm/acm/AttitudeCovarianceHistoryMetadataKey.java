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


/** Keys for {@link AttitudeCovarianceHistoryMetadata covariance history container} entries.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum AttitudeCovarianceHistoryMetadataKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Covariance identification number. */
    COV_ID((token, context, container) -> token.processAsFreeTextString(container::setCovID)),

    /** Identification number of previous covariance. */
    COV_PREV_ID((token, context, container) -> token.processAsFreeTextString(container::setCovPrevID)),

    /** Basis of this covariance time history data. */
    COV_BASIS((token, context, container) -> token.processAsFreeTextString(container::setCovBasis)),

    /** Identification number of the orbit determination or simulation upon which this covariance is based.*/
    COV_BASIS_ID((token, context, container) -> token.processAsFreeTextString(container::setCovBasisID)),

    /** Reference frame of the covariance. */
    COV_REF_FRAME((token, context, container) -> token.processAsFrame(container::setCovReferenceFrame, context, true, true, true)),

    /** Covariance element set type.
     * @see AttitudeCovarianceType
     */
    COV_TYPE((token, context, container) -> token.processAsEnum(AttitudeCovarianceType.class, container::setCovType));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AttitudeCovarianceHistoryMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final AttitudeCovarianceHistoryMetadata container) {
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
        boolean process(ParseToken token, ContextBinding context, AttitudeCovarianceHistoryMetadata container);
    }

}
