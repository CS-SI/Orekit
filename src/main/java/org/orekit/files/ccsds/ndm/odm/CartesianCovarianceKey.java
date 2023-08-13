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
package org.orekit.files.ccsds.ndm.odm;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;


/** Keys for {@link CartesianCovariance OPM/OMM/OCM Cartesian covariance} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum CartesianCovarianceKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Epoch entry (only for OEM files). */
    EPOCH((token, context, container) -> token.processAsDate(container::setEpoch, context)),

    /** Coordinate system for covariance matrix. */
    COV_REF_FRAME((token, context, container) -> token.processAsFrame(container::setReferenceFrame, context, true, true, true)),

    /** Covariance matrix [1, 1] element. */
    CX_X((token, context, container) -> token.processAsDoublyIndexedDouble(0, 0, Units.KM2, context.getParsedUnitsBehavior(),
                                                                           container::setCovarianceMatrixEntry)),

    /** Covariance matrix [2, 1] element. */
    CY_X((token, context, container) -> token.processAsDoublyIndexedDouble(1, 0, Units.KM2, context.getParsedUnitsBehavior(),
                                                                           container::setCovarianceMatrixEntry)),

    /** Covariance matrix [2, 2] element. */
    CY_Y((token, context, container) -> token.processAsDoublyIndexedDouble(1, 1, Units.KM2, context.getParsedUnitsBehavior(),
                                                                           container::setCovarianceMatrixEntry)),

    /** Covariance matrix [3, 1] element. */
    CZ_X((token, context, container) -> token.processAsDoublyIndexedDouble(2, 0, Units.KM2, context.getParsedUnitsBehavior(),
                                                                           container::setCovarianceMatrixEntry)),

    /** Covariance matrix [3, 2] element. */
    CZ_Y((token, context, container) -> token.processAsDoublyIndexedDouble(2, 1, Units.KM2, context.getParsedUnitsBehavior(),
                                                                           container::setCovarianceMatrixEntry)),

    /** Covariance matrix [3, 3] element. */
    CZ_Z((token, context, container) -> token.processAsDoublyIndexedDouble(2, 2, Units.KM2, context.getParsedUnitsBehavior(),
                                                                           container::setCovarianceMatrixEntry)),

    /** Covariance matrix [4, 1] element. */
    CX_DOT_X((token, context, container) -> token.processAsDoublyIndexedDouble(3, 0, Units.KM2_PER_S, context.getParsedUnitsBehavior(),
                                                                               container::setCovarianceMatrixEntry)),

    /** Covariance matrix [4, 2] element. */
    CX_DOT_Y((token, context, container) -> token.processAsDoublyIndexedDouble(3, 1, Units.KM2_PER_S, context.getParsedUnitsBehavior(),
                                                                               container::setCovarianceMatrixEntry)),

    /** Covariance matrix [4, 3] element. */
    CX_DOT_Z((token, context, container) -> token.processAsDoublyIndexedDouble(3, 2, Units.KM2_PER_S, context.getParsedUnitsBehavior(),
                                                                               container::setCovarianceMatrixEntry)),

    /** Covariance matrix [4, 4] element. */
    CX_DOT_X_DOT((token, context, container) -> token.processAsDoublyIndexedDouble(3, 3, Units.KM2_PER_S2, context.getParsedUnitsBehavior(),
                                                                                   container::setCovarianceMatrixEntry)),

    /** Covariance matrix [5, 1] element. */
    CY_DOT_X((token, context, container) -> token.processAsDoublyIndexedDouble(4, 0, Units.KM2_PER_S, context.getParsedUnitsBehavior(),
                                                                               container::setCovarianceMatrixEntry)),

    /** Covariance matrix [5, 2] element. */
    CY_DOT_Y((token, context, container) -> token.processAsDoublyIndexedDouble(4, 1, Units.KM2_PER_S, context.getParsedUnitsBehavior(),
                                                                               container::setCovarianceMatrixEntry)),

    /** Covariance matrix [5, 3] element. */
    CY_DOT_Z((token, context, container) -> token.processAsDoublyIndexedDouble(4, 2, Units.KM2_PER_S, context.getParsedUnitsBehavior(),
                                                                               container::setCovarianceMatrixEntry)),

    /** Covariance matrix [5, 4] element. */
    CY_DOT_X_DOT((token, context, container) -> token.processAsDoublyIndexedDouble(4, 3, Units.KM2_PER_S2, context.getParsedUnitsBehavior(),
                                                                                   container::setCovarianceMatrixEntry)),

    /** Covariance matrix [5, 5] element. */
    CY_DOT_Y_DOT((token, context, container) -> token.processAsDoublyIndexedDouble(4, 4, Units.KM2_PER_S2, context.getParsedUnitsBehavior(),
                                                                                   container::setCovarianceMatrixEntry)),

    /** Covariance matrix [6, 1] element. */
    CZ_DOT_X((token, context, container) -> token.processAsDoublyIndexedDouble(5, 0, Units.KM2_PER_S, context.getParsedUnitsBehavior(),
                                                                               container::setCovarianceMatrixEntry)),

    /** Covariance matrix [6, 2] element. */
    CZ_DOT_Y((token, context, container) -> token.processAsDoublyIndexedDouble(5, 1, Units.KM2_PER_S, context.getParsedUnitsBehavior(),
                                                                               container::setCovarianceMatrixEntry)),

    /** Covariance matrix [6, 3] element. */
    CZ_DOT_Z((token, context, container) -> token.processAsDoublyIndexedDouble(5, 2, Units.KM2_PER_S, context.getParsedUnitsBehavior(),
                                                                               container::setCovarianceMatrixEntry)),

    /** Covariance matrix [6, 4] element. */
    CZ_DOT_X_DOT((token, context, container) -> token.processAsDoublyIndexedDouble(5, 3, Units.KM2_PER_S2, context.getParsedUnitsBehavior(),
                                                                                   container::setCovarianceMatrixEntry)),

    /** Covariance matrix [6, 5] element. */
    CZ_DOT_Y_DOT((token, context, container) -> token.processAsDoublyIndexedDouble(5, 4, Units.KM2_PER_S2, context.getParsedUnitsBehavior(),
                                                                                   container::setCovarianceMatrixEntry)),

    /** Covariance matrix [6, 6] element. */
    CZ_DOT_Z_DOT((token, context, container) -> token.processAsDoublyIndexedDouble(5, 5, Units.KM2_PER_S2, context.getParsedUnitsBehavior(),
                                                                                   container::setCovarianceMatrixEntry));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    CartesianCovarianceKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final CartesianCovariance container) {
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
        boolean process(ParseToken token, ContextBinding context, CartesianCovariance container);
    }

}
