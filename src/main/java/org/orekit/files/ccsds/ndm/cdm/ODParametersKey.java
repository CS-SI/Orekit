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

/** Keys for {@link ODParameters CDM OD parameters} entries.
 * @author Melina Vanel
 * @since 11.2
 */
public enum ODParametersKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** The start of a time interval (UTC) that contains the time of the last accepted observation. */
    TIME_LASTOB_START((token, context, container) -> token.processAsDate(container::setTimeLastObsStart, context)),

    /** The start of a time interval (UTC) that contains the time of the last accepted observation. */
    TIME_LASTOB_END((token, context, container) -> token.processAsDate(container::setTimeLastObsEnd, context)),

    /** The recommended OD time span calculated for the object. */
    RECOMMENDED_OD_SPAN((token, context, container) -> token.processAsDouble(Unit.DAY, context.getParsedUnitsBehavior(),
                                                                             container::setRecommendedOdSpan)),

    /** Based on the observations available and the RECOMMENDED_OD_SPAN, the actual time span used for the OD of the object. */
    ACTUAL_OD_SPAN((token, context, container) -> token.processAsDouble(Unit.DAY, context.getParsedUnitsBehavior(),
                                                                             container::setActualOdSpan)),

    /** The number of observations available for the OD of the object. */
    OBS_AVAILABLE((token, context, container) -> token.processAsInteger(container::setObsAvailable)),

    /** The number of observations accepted for the OD of the object. */
    OBS_USED((token, context, container) -> token.processAsInteger(container::setObsUsed)),

    /** The number of sensor tracks available for the OD of the object. */
    TRACKS_AVAILABLE((token, context, container) -> token.processAsInteger(container::setTracksAvailable)),

    /** The number of sensor tracks accepted for the OD of the object. */
    TRACKS_USED((token, context, container) -> token.processAsInteger(container::setTracksUsed)),

    /** The percentage of residuals accepted in the OD of the object (from 0 to 100). */
    RESIDUALS_ACCEPTED((token, context, container) -> token.processAsDouble(Unit.PERCENT, context.getParsedUnitsBehavior(),
                                                                             container::setResidualsAccepted)),

    /** The weighted Root Mean Square (RMS) of the residuals from a batch least squares OD. */
    WEIGHTED_RMS((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                             container::setWeightedRMS)),

    /** The epoch of the orbit determination used for this message (UTC). */
    OD_EPOCH((token, context, container) -> token.processAsDate(container::setOdEpoch, context));


    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    ODParametersKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final ODParameters container) {
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
        boolean process(ParseToken token, ContextBinding context, ODParameters container);
    }

}
