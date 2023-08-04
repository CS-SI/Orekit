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
package org.orekit.files.ccsds.ndm.cdm;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keys for {@link XYZCovariance CDM covariance matrix} entries.
 */
public enum XYZCovarianceKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Object covariance matrix [1,1]. */
    CX_X((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCxx)),
    /** Object covariance matrix [2,1]. */
    CY_X((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCyx)),
    /** Object covariance matrix [2,2]. */
    CY_Y((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCyy)),
    /** Object covariance matrix [3,1]. */
    CZ_X((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCzx)),
    /** Object covariance matrix [3,2]. */
    CZ_Y((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCzy)),
    /** Object covariance matrix [3,3]. */
    CZ_Z((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                             container::setCzz)),
    /** Object covariance matrix [4,1]. */
    CXDOT_X((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCxdotx)),
    /** Object covariance matrix [4,2]. */
    CXDOT_Y((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCxdoty)),
    /** Object covariance matrix [4,3]. */
    CXDOT_Z((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCxdotz)),
    /** Object covariance matrix [4,4]. */
    CXDOT_XDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCxdotxdot)),
    /** Object covariance matrix [5,1]. */
    CYDOT_X((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCydotx)),
    /** Object covariance matrix [5,2]. */
    CYDOT_Y((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCydoty)),
    /** Object covariance matrix [5,3]. */
    CYDOT_Z((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCydotz)),
    /** Object covariance matrix [5,4]. */
    CYDOT_XDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCydotxdot)),
    /** Object covariance matrix [5,5]. */
    CYDOT_YDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCydotydot)),
    /** Object covariance matrix [6,1]. */
    CZDOT_X((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCzdotx)),
    /** Object covariance matrix [6,2]. */
    CZDOT_Y((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCzdoty)),
    /** Object covariance matrix [6,3]. */
    CZDOT_Z((token, context, container) -> token.processAsDouble(Units.M2_PER_S, context.getParsedUnitsBehavior(),
                                                             container::setCzdotz)),
    /** Object covariance matrix [6,4]. */
    CZDOT_XDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCzdotxdot)),
    /** Object covariance matrix [6,5]. */
    CZDOT_YDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCzdotydot)),
    /** Object covariance matrix [6,6]. */
    CZDOT_ZDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCzdotzdot)),
    /** Object covariance matrix [7,1]. */
    CDRG_X((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCdrgx)),
    /** Object covariance matrix [7,2]. */
    CDRG_Y((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCdrgy)),
    /** Object covariance matrix [7,3]. */
    CDRG_Z((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCdrgz)),
    /** Object covariance matrix [7,4]. */
    CDRG_XDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCdrgxdot)),
    /** Object covariance matrix [7,5]. */
    CDRG_YDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCdrgydot)),
    /** Object covariance matrix [7,6]. */
    CDRG_ZDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCdrgzdot)),
    /** Object covariance matrix [7,7]. */
    CDRG_DRG((token, context, container) -> token.processAsDouble(Units.M4_PER_KG2, context.getParsedUnitsBehavior(),
                                                             container::setCdrgdrg)),
    /** Object covariance matrix [8,1]. */
    CSRP_X((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCsrpx)),
    /** Object covariance matrix [8,2]. */
    CSRP_Y((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCsrpy)),
    /** Object covariance matrix [8,3]. */
    CSRP_Z((token, context, container) -> token.processAsDouble(Units.M3_PER_KG, context.getParsedUnitsBehavior(),
                                                             container::setCsrpz)),
    /** Object covariance matrix [8,4]. */
    CSRP_XDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCsrpxdot)),
    /** Object covariance matrix [8,5]. */
    CSRP_YDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCsrpydot)),
    /** Object covariance matrix [8,6]. */
    CSRP_ZDOT((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS, context.getParsedUnitsBehavior(),
                                                             container::setCsrpzdot)),
    /** Object covariance matrix [8,7]. */
    CSRP_DRG((token, context, container) -> token.processAsDouble(Units.M4_PER_KG2, context.getParsedUnitsBehavior(),
                                                             container::setCsrpdrg)),
    /** Object covariance matrix [8,9]. */
    CSRP_SRP((token, context, container) -> token.processAsDouble(Units.M4_PER_KG2, context.getParsedUnitsBehavior(),
                                                             container::setCsrpsrp)),
    /** Object covariance matrix [9,1]. */
    CTHR_X((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCthrx)),
    /** Object covariance matrix [9,2]. */
    CTHR_Y((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCthry)),
    /** Object covariance matrix [9,3]. */
    CTHR_Z((token, context, container) -> token.processAsDouble(Units.M2_PER_S2, context.getParsedUnitsBehavior(),
                                                             container::setCthrz)),
    /** Object covariance matrix [9,4]. */
    CTHR_XDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S3, context.getParsedUnitsBehavior(),
                                                             container::setCthrxdot)),
    /** Object covariance matrix [9,5]. */
    CTHR_YDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S3, context.getParsedUnitsBehavior(),
                                                             container::setCthrydot)),
    /** Object covariance matrix [9,6]. */
    CTHR_ZDOT((token, context, container) -> token.processAsDouble(Units.M2_PER_S3, context.getParsedUnitsBehavior(),
                                                             container::setCthrzdot)),
    /** Object covariance matrix [9,7]. */
    CTHR_DRG((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS2, context.getParsedUnitsBehavior(),
                                                             container::setCthrdrg)),
    /** Object covariance matrix [9,8]. */
    CTHR_SRP((token, context, container) -> token.processAsDouble(Units.M3_PER_KGS2, context.getParsedUnitsBehavior(),
                                                             container::setCthrsrp)),
    /** Object covariance matrix [9,9]. */
    CTHR_THR((token, context, container) -> token.processAsDouble(Units.M2_PER_S4, context.getParsedUnitsBehavior(),
                                                             container::setCthrthr));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    XYZCovarianceKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final XYZCovariance container) {
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
        boolean process(ParseToken token, ContextBinding context, XYZCovariance container);
    }

}
