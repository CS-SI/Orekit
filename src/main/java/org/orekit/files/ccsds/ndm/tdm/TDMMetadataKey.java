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
package org.orekit.files.ccsds.ndm.tdm;

import org.orekit.files.ccsds.ndm.ParsingContext;
import org.orekit.files.ccsds.utils.lexical.ParseToken;


/** Keys for {@link TDMMetadata TDM metadata} entries.
 * @author Maxime Journot
 * @since 11.0
 */
public enum TDMMetadataKey {

    /** Comment entry. */
    COMMENT((token, context, metadata) -> token.processAsFreeTextString(metadata::addComment)),

    /** Time system entry. */
    TIME_SYSTEM((token, context, metadata) -> token.processAsTimeScale(metadata::setTimeSystem)),

    /** Start time entry. */
    START_TIME((token, context, metadata) -> token.processAsDate(metadata::setStartTime, context)),

    /** Stop time entry. */
    STOP_TIME((token, context, metadata) -> token.processAsDate(metadata::setStopTime, context)),

    /** First participant entry. */
    PARTICIPANT_1((token, context, metadata) -> token.processAsIndexedNormalizedString(metadata::addParticipant, 1)),

    /** Second participant entry. */
    PARTICIPANT_2((token, context, metadata) -> token.processAsIndexedNormalizedString(metadata::addParticipant, 2)),

    /** Third participant entry. */
    PARTICIPANT_3((token, context, metadata) -> token.processAsIndexedNormalizedString(metadata::addParticipant, 3)),

    /** Fourth participant entry. */
    PARTICIPANT_4((token, context, metadata) -> token.processAsIndexedNormalizedString(metadata::addParticipant, 4)),

    /** Fifth participant entry. */
    PARTICIPANT_5((token, context, metadata) -> token.processAsIndexedNormalizedString(metadata::addParticipant, 5)),

    /** Mode entry. */
    MODE((token, context, metadata) -> token.processAsNormalizedString(metadata::setMode)),

    /** Path entry. */
    PATH((token, context, metadata) -> token.processAsNormalizedString(metadata::setPath)),

    /** Path 1 entry. */
    PATH_1((token, context, metadata) -> token.processAsNormalizedString(metadata::setPath1)),

    /** Path 2 entry. */
    PATH_2((token, context, metadata) -> token.processAsNormalizedString(metadata::setPath2)),

    /** Transmit band entry. */
    TRANSMIT_BAND((token, context, metadata) -> token.processAsNormalizedString(metadata::setTransmitBand)),

    /** Receive band entry. */
    RECEIVE_BAND((token, context, metadata) -> token.processAsNormalizedString(metadata::setReceiveBand)),

    /** Turnaround numerator entry. */
    TURNAROUND_NUMERATOR((token, context, metadata) -> token.processAsInteger(metadata::setTurnaroundNumerator)),

    /** turnaround denominator entry. */
    TURNAROUND_DENOMINATOR((token, context, metadata) -> token.processAsInteger(metadata::setTurnaroundDenominator)),

    /** Timetag referene entry. */
    TIMETAG_REF((token, context, metadata) -> token.processAsNormalizedString(metadata::setTimetagRef)),

    /** Integration interval entry. */
    INTEGRATION_INTERVAL((token, context, metadata) -> token.processAsDouble(metadata::setIntegrationInterval)),

    /** Integration reference entry. */
    INTEGRATION_REF((token, context, metadata) -> token.processAsNormalizedString(metadata::setIntegrationRef)),

    /** Frequency offset entry. */
    FREQ_OFFSET((token, context, metadata) -> token.processAsDouble(metadata::setFreqOffset)),

    /** Range mode entry. */
    RANGE_MODE((token, context, metadata) -> token.processAsNormalizedString(metadata::setRangeMode)),

    /** Range modulus entry. */
    RANGE_MODULUS((token, context, metadata) -> token.processAsDouble(metadata::setRangeModulus)),

    /** Range units entry. */
    RANGE_UNITS((token, context, metadata) -> token.processAsNormalizedString(metadata::setRangeUnits)),

    /** Angle type entry. */
    ANGLE_TYPE((token, context, metadata) -> token.processAsNormalizedString(metadata::setAngleType)),

    /** reference frame entry. */
    REFERENCE_FRAME((token, context, metadata) -> token.processAsFrame(metadata::setReferenceFrame, context)),

    /** First transmit delay entry. */
    TRANSMIT_DELAY_1((token, context, metadata) -> token.processAsIndexedDouble(metadata::addTransmitDelay, 1)),

    /** Second transmit delay entry. */
    TRANSMIT_DELAY_2((token, context, metadata) -> token.processAsIndexedDouble(metadata::addTransmitDelay, 2)),

    /** Third transmit delay entry. */
    TRANSMIT_DELAY_3((token, context, metadata) -> token.processAsIndexedDouble(metadata::addTransmitDelay, 3)),

    /** Fourth transmit delay entry. */
    TRANSMIT_DELAY_4((token, context, metadata) -> token.processAsIndexedDouble(metadata::addTransmitDelay, 4)),

    /** Fifth transmit delay entry. */
    TRANSMIT_DELAY_5((token, context, metadata) -> token.processAsIndexedDouble(metadata::addTransmitDelay, 5)),

    /** First receive delay entry. */
    RECEIVE_DELAY_1((token, context, metadata) -> token.processAsIndexedDouble(metadata::addReceiveDelay, 1)),

    /** Second receive delay entry. */
    RECEIVE_DELAY_2((token, context, metadata) -> token.processAsIndexedDouble(metadata::addReceiveDelay, 2)),

    /** Third receive delay entry. */
    RECEIVE_DELAY_3((token, context, metadata) -> token.processAsIndexedDouble(metadata::addReceiveDelay, 3)),

    /** Fourth receive delay entry. */
    RECEIVE_DELAY_4((token, context, metadata) -> token.processAsIndexedDouble(metadata::addReceiveDelay, 4)),

    /** Fifth receive delay entry. */
    RECEIVE_DELAY_5((token, context, metadata) -> token.processAsIndexedDouble(metadata::addReceiveDelay, 5)),

    /** data quality entry. */
    DATA_QUALITY((token, context, metadata) -> token.processAsNormalizedString(metadata::setDataQuality)),

    /** Angle 1 correction entry. */
    CORRECTION_ANGLE_1((token, context, metadata) -> token.processAsDouble(metadata::setCorrectionAngle1)),

    /** Angle 2 correction entry. */
    CORRECTION_ANGLE_2((token, context, metadata) -> token.processAsDouble(metadata::setCorrectionAngle2)),

    /** Doppler correction entry. */
    CORRECTION_DOPPLER((token, context, metadata) -> token.processAsDouble(metadata::setCorrectionDoppler)),

    /** Range correction entry. */
    CORRECTION_RANGE((token, context, metadata) -> token.processAsDouble(metadata::setCorrectionRange)),

    /** Recive correction entry. */
    CORRECTION_RECEIVE((token, context, metadata) -> token.processAsDouble(metadata::setCorrectionReceive)),

    /** Transmit correction entry. */
    CORRECTION_TRANSMIT((token, context, metadata) -> token.processAsDouble(metadata::setCorrectionTransmit)),

    /** Applied correction entry. */
    CORRECTIONS_APPLIED((token, context, metadata) -> token.processAsNormalizedString(metadata::setCorrectionsApplied));

    /** Processing method. */
    private final MetadataEntryProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    TDMMetadataKey(final MetadataEntryProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context parsing context
     * @param metadata metadata to fill
     */
    public void parse(final ParseToken token, final ParsingContext context, final TDMMetadata metadata) {
        processor.process(token, context, metadata);
    }

    /** Interface for processing one token. */
    interface MetadataEntryProcessor {
        /** Process one token.
         * @param token token to process
         * @param context parsing context
         * @param metadata metadata to fill
         */
        void process(ParseToken token, ParsingContext context, TDMMetadata metadata);
    }

}