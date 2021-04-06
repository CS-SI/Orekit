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

import org.orekit.files.ccsds.definitions.OdMethodFacade;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;


/** Keys for {@link OrbitDetermination orbit determination data} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OrbitDeterminationKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Identification number. */
    OD_ID((token, context, container) -> token.processAsNormalizedString(container::setId)),

    /** Identification of previous orbit determination. */
    OD_PREV_ID((token, context, container) -> token.processAsNormalizedString(container::setPrevId)),

    /** Orbit determination method. */
    OD_METHOD((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            container.setMethod(OdMethodFacade.parse(token.getContentAsNormalizedString()));
        }
        return true;
    }),

    /** Time tag for orbit determination solved-for state. */
    OD_EPOCH((token, context, container) -> token.processAsDate(container::setEpoch, context)),

    /** Time elapsed between first accepted observation on epoch. */
    DAYS_SINCE_FIRST_OBS((token, context, container) -> token.processAsDouble(Unit.DAY,
                                                                              container::setTimeSinceFirstObservation)),

    /** Time elapsed between last accepted observation on epoch. */
    DAYS_SINCE_LAST_OBS((token, context, container) -> token.processAsDouble(Unit.DAY,
                                                                             container::setTimeSinceLastObservation)),

    /** Sime span of observation recommended for the OD of the object. */
    RECOMMENDED_OD_SPAN((token, context, container) -> token.processAsDouble(Unit.DAY,
                                                                             container::setRecommendedOdSpan)),

    /** Actual time span used for the OD of the object. */
    ACTUAL_OD_SPAN((token, context, container) -> token.processAsDouble(Unit.DAY,
                                                                        container::setActualOdSpan)),

    /** Number of observations available within the actual OD span. */
    OBS_AVAILABLE((token, context, container) -> token.processAsInteger(container::setObsAvailable)),

    /** Number of observations accepted within the actual OD span. */
    OBS_USED((token, context, container) -> token.processAsInteger(container::setObsUsed)),

    /** Number of sensors tracks available for the OD within the actual OD span. */
    TRACKS_AVAILABLE((token, context, container) -> token.processAsInteger(container::setTracksAvailable)),

    /** Number of sensors tracks accepted for the OD within the actual OD span. */
    TRACKS_USED((token, context, container) -> token.processAsInteger(container::setTracksUsed)),

    /** Maximum time between observations in the OD of the object. */
    MAXIMUM_OBS_GAP((token, context, container) -> token.processAsDouble(Unit.DAY,
                                                                         container::setMaximumObsGap)),

    /** Positional error ellipsoid 1σ major eigenvalue at the epoch of OD. */
    OD_EPOCH_EIGMAJ((token, context, container) -> token.processAsDouble(Unit.METRE, container::setEpochEigenMaj)),

    /** Positional error ellipsoid 1σ intermediate eigenvalue at the epoch of OD. */
    OD_EPOCH_EIGMED((token, context, container) -> token.processAsDouble(Unit.METRE, container::setEpochEigenMed)),

    /** Positional error ellipsoid 1σ minor eigenvalue at the epoch of OD. */
    OD_EPOCH_EIGMIN((token, context, container) -> token.processAsDouble(Unit.METRE, container::setEpochEigenMin)),

    /** Maximum predicted major eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM. */
    OD_MAX_PRED_EIGMAJ((token, context, container) -> token.processAsDouble(Unit.METRE, container::setMaxPredictedEigenMaj)),

    /** Minimum predicted major eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM. */
    OD_MIN_PRED_EIGMIN((token, context, container) -> token.processAsDouble(Unit.METRE, container::setMinPredictedEigenMaj)),

    /** Confidence metric. */
    OD_CONFIDENCE((token, context, container) -> token.processAsDouble(Unit.PERCENT, container::setConfidence)),

    /** Generalize Dilution Of Precision. */
    GDOP((token, context, container) -> token.processAsDouble(Unit.ONE, container::setGdop)),

    /** Number of solved-for states. */
    SOLVE_N((token, context, container) -> token.processAsInteger(container::setSolveN)),

    /** Description of state elements solved-for. */
    SOLVE_STATES((token, context, container) -> token.processAsNormalizedList(container::setSolveStates)),

    /** Number of consider parameters. */
    CONSIDER_N((token, context, container) -> token.processAsInteger(container::setConsiderN)),

    /** Description of consider parameters. */
    CONSIDER_PARAMS((token, context, container) -> token.processAsNormalizedList(container::setConsiderParameters)),

    /** Number of sensors used. */
    SENSORS_N((token, context, container) -> token.processAsInteger(container::setSensorsN)),

    /** Description of sensors used. */
    SENSORS((token, context, container) -> token.processAsNormalizedList(container::setSensors)),

    /** Weighted RMS residual ratio. */
    WEIGHTED_RMS((token, context, container) -> token.processAsDouble(Unit.ONE, container::setWeightedRms)),

    /** Observation data types used. */
    DATA_TYPES((token, context, container) -> token.processAsNormalizedList(container::setDataTypes));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    OrbitDeterminationKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final OrbitDetermination container) {
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
        boolean process(ParseToken token, ContextBinding context, OrbitDetermination container);
    }

}
