/* Copyright 2002-2022 CS GROUP
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
package org.orekit.files.ccsds.ndm.cdm;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keys for {@link CovarianceMatrix CDM covariance matrix} entries.
 * @author Melina Vanel
 * @since 11.2
 */
public enum CovarianceMatrixKey {
    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Object covariance matrix [1,1]. */
    CR_R((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCRR)),
    /** Object covariance matrix [2,1]. */
    CT_R((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCTR)),
    /** Object covariance matrix [2,2]. */
    CT_T((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCTT)),
    /** Object covariance matrix [3,1]. */
    CN_R((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCNR)),
    /** Object covariance matrix [3,2]. */
    CN_T((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCNT)),
    /** Object covariance matrix [3,3]. */
    CN_N((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCNN)),
    /** Object covariance matrix [4,1]. */
    CRDOT_R((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCRdotR)),
    /** Object covariance matrix [4,2]. */
    CRDOT_T((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCRdotT)),
    /** Object covariance matrix [4,3]. */
    CRDOT_N((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCRdotN)),
    /** Object covariance matrix [4,4]. */
    CRDOT_RDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCRdotRdot)),
    /** Object covariance matrix [5,1]. */
    CTDOT_R((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCTdotR)),
    /** Object covariance matrix [5,2]. */
    CTDOT_T((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCTdotT)),
    /** Object covariance matrix [5,3]. */
    CTDOT_N((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCTdotN)),
    /** Object covariance matrix [5,4]. */
    CTDOT_RDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCTdotRdot)),
    /** Object covariance matrix [5,5]. */
    CTDOT_TDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCTdotTdot)),
    /** Object covariance matrix [6,1]. */
    CNDOT_R((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCNdotR)),
    /** Object covariance matrix [6,2]. */
    CNDOT_T((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCNdotT)),
    /** Object covariance matrix [6,3]. */
    CNDOT_N((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCNdotN)),
    /** Object covariance matrix [6,4]. */
    CNDOT_RDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCNdotRdot)),
    /** Object covariance matrix [6,5]. */
    CNDOT_TDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCNdotTdot)),
    /** Object covariance matrix [6,6]. */
    CNDOT_NDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCNdotNdot)),
    /** Object covariance matrix [7,1]. */
    CDRG_R((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCDRGR)),
    /** Object covariance matrix [7,2]. */
    CDRG_T((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCDRGT)),
    /** Object covariance matrix [7,3]. */
    CDRG_N((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCDRGN)),
    /** Object covariance matrix [7,4]. */
    CDRG_RDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCDRGRdot)),
    /** Object covariance matrix [7,5]. */
    CDRG_TDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCDRGTdot)),
    /** Object covariance matrix [7,6]. */
    CDRG_NDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCDRGNdot)),
    /** Object covariance matrix [7,7]. */
    CDRG_DRG((token, context, container) -> token.processAsDouble(Units.M4_PER_KG2, context.getParsedUnitsBehavior(),
                                                             container::setCDRGDRG)),
    /** Object covariance matrix [8,1]. */
    CSRP_R((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCSRPR)),
    /** Object covariance matrix [8,2]. */
    CSRP_T((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCSRPT)),
    /** Object covariance matrix [8,3]. */
    CSRP_N((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCSRPN)),
    /** Object covariance matrix [8,4]. */
    CSRP_RDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCSRPRdot)),
    /** Object covariance matrix [8,5]. */
    CSRP_TDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCSRPTdot)),
    /** Object covariance matrix [8,6]. */
    CSRP_NDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCSRPNdot)),
    /** Object covariance matrix [8,7]. */
    CSRP_DRG((token, context, container) -> token.processAsDouble(Units.M4_PER_KG2, context.getParsedUnitsBehavior(),
                                                             container::setCSRPDRG)),
    /** Object covariance matrix [8,9]. */
    CSRP_SRP((token, context, container) -> token.processAsDouble(Units.M4_PER_KG2, context.getParsedUnitsBehavior(),
                                                             container::setCSRPSRP)),
    /** Object covariance matrix [9,1]. */
    CTHR_R((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCTHRR)),
    /** Object covariance matrix [9,2]. */
    CTHR_T((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCTHRT)),
    /** Object covariance matrix [9,3]. */
    CTHR_N((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCTHRN)),
    /** Object covariance matrix [9,4]. */
    CTHR_RDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S3, context.getParsedUnitsBehavior(),
                                                             container::setCTHRRdot)),
    /** Object covariance matrix [9,5]. */
    CTHR_TDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S3, context.getParsedUnitsBehavior(),
                                                             container::setCTHRTdot)),
    /** Object covariance matrix [9,6]. */
    CTHR_NDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S3, context.getParsedUnitsBehavior(),
                                                             container::setCTHRNdot)),
    /** Object covariance matrix [9,7]. */
    CTHR_DRG((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS2, context.getParsedUnitsBehavior(),
                                                             container::setCTHRDRG)),
    /** Object covariance matrix [9,8]. */
    CTHR_SRP((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS2, context.getParsedUnitsBehavior(),
                                                             container::setCTHRSRP)),
    /** Object covariance matrix [9,9]. */
    CTHR_THR((token, context, container) -> token.processAsDouble(Units.M2_PER_S4, context.getParsedUnitsBehavior(),
                                                             container::setCTHRTHR));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    CovarianceMatrixKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final CovarianceMatrix container) {
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
        boolean process(ParseToken token, ContextBinding context, CovarianceMatrix container);
    }

}
