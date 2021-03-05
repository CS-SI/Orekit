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

import java.util.stream.Collectors;

import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.parsing.ParsingContext;


/** Keys for {@link ManeuverHistoryMetadata maneuver history metadata} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum ManeuverHistoryMetadataKey {

    /** Comment entry. */
    COMMENT((token, context, metadata) ->
            token.getType() == TokenType.ENTRY ? metadata.addComment(token.getContent()) : true),

    /** Maneuver identification number. */
    MAN_ID((token, context, metadata) -> token.processAsFreeTextString(metadata::setManID)),

    /** Identification number of previous maneuver. */
    MAN_PREV_ID((token, context, metadata) -> token.processAsFreeTextString(metadata::setManPrevID)),

    /** Identification number of next maneuver. */
    MAN_NEXT_ID((token, context, metadata) -> token.processAsFreeTextString(metadata::setManNextID)),

    /** Basis of this maneuver history data. */
    MAN_BASIS((token, context, metadata) -> token.processAsFreeTextString(metadata::setManBasis)),

    /** Identification number of the orbit determination or simulation upon which this maneuver is based.*/
    MAN_BASIS_ID((token, context, metadata) -> token.processAsFreeTextString(metadata::setManBasisID)),

    /** Identifier of the device used for this maneuver.*/
    MAN_DEVICE_ID((token, context, metadata) -> token.processAsFreeTextString(metadata::setManDeviceID)),

    /** Completion time of previous maneuver. */
    MAN_PREV_EPOCH((token, context, metadata) -> token.processAsDate(metadata::setManPrevEpoch, context)),

    /** Start time of next maneuver. */
    MAN_NEXT_EPOCH((token, context, metadata) -> token.processAsDate(metadata::setManNextEpoch, context)),

    /** Purposes of the maneuver. */
    MAN_PURPOSE((token, context, metadata) -> token.processAsFreeTextStringList(metadata::setManPurpose)),

    /** Prediction source on which this maneuver is based. */
    MAN_PRED_SOURCE((token, context, metadata) -> token.processAsFreeTextString(metadata::setManPredSource)),

    /** Reference frame of the maneuver. */
    MAN_REF_FRAME((token, context, metadata) -> token.processAsFrame(metadata::setManReferenceFrame, context, true, false, false)),

    /** Epoch of the {@link #COV_REF_FRAME orbit reference frame}. */
    MAN_FRAME_EPOCH((token, context, metadata) -> token.processAsDate(metadata::setManFrameEpoch, context)),

    /** Origin of maneuver gravitational assist body. */
    GRAV_ASSIST_NAME((token, context, metadata) -> token.processAsCenter(metadata::setGravitationalAssistName,
                                                                         context.getDataContext().getCelestialBodies())),

    /** Type of duty cycle. */
    DC_TYPE((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                metadata.setDcType(DutyCycleType.valueOf(token.getContentAsNormalizedString()));
            } catch (IllegalArgumentException iae) {
                throw token.generateException(iae);
            }
        }
        return true;
    }),

    /** Start time of duty cycle-based maneuver window. */
    DC_WIN_OPEN((token, context, metadata) -> token.processAsDate(metadata::setDcWindowOpen, context)),

    /** Start time of duty cycle-based maneuver window. */
    DC_WIN_CLOSE((token, context, metadata) -> token.processAsDate(metadata::setDcWindowClose, context)),

    /** Minimum number of "ON" duty cycles. */
    DC_MIN_CYCLES((token, context, metadata) -> token.processAsInteger(metadata::setDcMinCycles)),

    /** Maximum number of "ON" duty cycles. */
    DC_MAX_CYCLES((token, context, metadata) -> token.processAsInteger(metadata::setDcMaxCycles)),

    /** Start time of initial duty cycle-based maneuver execution. */
    DC_EXEC_START((token, context, metadata) -> token.processAsDate(metadata::setDcExecStart, context)),

    /** End time of final duty cycle-based maneuver execution. */
    DC_EXEC_STOP((token, context, metadata) -> token.processAsDate(metadata::setDcExecStop, context)),

    /** Duty cycle thrust reference time. */
    DC_REF_TIME((token, context, metadata) -> token.processAsDate(metadata::setDcRefTime, context)),

    /** Duty cycle pulse "ON" duration. */
    DC_TIME_PULSE_DURATION((token, context, metadata) -> token.processAsDouble(1.0, metadata::setDcTimePulseDuration)),

    /** Duty cycle elapsed time between start of a pulse and start of next pulse. */
    DC_TIME_PULSE_PERIOD((token, context, metadata) -> token.processAsDouble(1.0, metadata::setDcTimePulsePeriod)),

    /** Reference direction for triggering duty cycle. */
    DC_REF_DIR((token, context, metadata) -> token.processAsVector(metadata::setDcRefDir)),

    /** Spacecraft body frame in which {@link #dcBodyTrigger} is specified. */
    DC_BODY_FRAME((token, context, metadata) -> token.processAsFrame(f -> metadata.setDcBodyFrame(f.asSpacecraftBodyFrame()),
                                                                     context, false, false, true)),

    /** Direction in {@link #dcBodyFrame body frame} for triggering duty cycle. */
    DC_BODY_TRIGGER((token, context, metadata) -> token.processAsVector(metadata::setDcBodyTrigger)),

    /** Phase angle of pulse start. */
    DC_PA_START_ANGLE((token, context, metadata) -> token.processAsAngle(metadata::setDcPhaseStartAngle)),

    /** Phase angle of pulse stop. */
    DC_PA_STOP_ANGLE((token, context, metadata) -> token.processAsAngle(metadata::setDcPhaseStopAngle)),

    /** Maneuver elements of information. */
    MAN_COMPOSITION((token, context, metadata) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                metadata.setManComposition(token.getContentAsNormalizedStringList().
                                           stream().
                                           map(s -> ManeuverFieldType.valueOf(s)).
                                           collect(Collectors.toList()));
            } catch (IllegalArgumentException iae) {
                throw token.generateException(iae);
            }
        }
        return true;
    }),

    /** SI units for each elements of the maneuver. */
    MAN_UNITS((token, context, metadata) -> token.processAsUnitList(metadata::setManUnits));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    ManeuverHistoryMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context parsing context
     * @param metadata metadata to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ParsingContext context, final ManeuverHistoryMetadata metadata) {
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
        boolean process(ParseToken token, ParsingContext context, ManeuverHistoryMetadata metadata);
    }

}
