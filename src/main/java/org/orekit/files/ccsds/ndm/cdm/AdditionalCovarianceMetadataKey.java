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

import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;

/** Keys for {@link AdditionalCovarianceMetadata covariance metadatail y a} entries.
 */
public enum AdditionalCovarianceMetadataKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** The atmospheric density forecast error. */
    DENSITY_FORECAST_UNCERTAINTY((token, context, container) -> token.processAsDouble(Unit.NONE, context.getParsedUnitsBehavior(),
                                                                                      container::setDensityForecastUncertainty)),

    /** The minimum suggested covariance scale factor. */
    CSCALE_FACTOR_MIN((token, context, container) -> token.processAsDouble(Unit.NONE, context.getParsedUnitsBehavior(),
                                                                           container::setcScaleFactorMin)),

    /** The (median) suggested covariance scale factor. */
    CSCALE_FACTOR((token, context, container) -> token.processAsDouble(Unit.NONE, context.getParsedUnitsBehavior(),
                                                                       container::setcScaleFactor)),

    /** The maximum suggested covariance scale factor. */
    CSCALE_FACTOR_MAX((token, context, container) -> token.processAsDouble(Unit.NONE, context.getParsedUnitsBehavior(),
                                                                           container::setcScaleFactorMax)),

    /** The source (or origin) of the specific orbital data for this object. */
    SCREENING_DATA_SOURCE((token, context, container) -> token.processAsFreeTextString(container::setScreeningDataSource)),

    /** The Drag Consider Parameter (DCP) sensitivity vector (position errors at TCA). */
    DCP_SENSITIVITY_VECTOR_POSITION((token, context, container) -> token.processAsDoubleArray(Unit.NONE, context.getParsedUnitsBehavior(),
                                                                                              container::setDcpSensitivityVectorPosition)),

     /** The Drag Consider Parameter (DCP) sensitivity vector (velocity errors at TCA). */
    DCP_SENSITIVITY_VECTOR_VELOCITY((token, context, container) -> token.processAsDoubleArray(Unit.NONE, context.getParsedUnitsBehavior(),
                                                                                              container::setDcpSensitivityVectorVelocity));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    AdditionalCovarianceMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final AdditionalCovarianceMetadata container) {
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
        boolean process(ParseToken token, ContextBinding context, AdditionalCovarianceMetadata container);
    }

}
