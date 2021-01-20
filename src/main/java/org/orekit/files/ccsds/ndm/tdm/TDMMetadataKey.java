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
import org.orekit.files.ccsds.utils.lexical.ParseEvent;


/** Keys for {@link TDMMetadata TDM metadata} entries.
 * @author Maxime Journot
 * @since 11.0
 */
public enum TDMMetadataKey {

    /** Comment entry. */
    COMMENT((event, context, metadata) -> event.processAsFreeTextString(metadata::addComment)),

    /** Time system entry. */
    TIME_SYSTEM((event, context, metadata) -> event.processAsTimeScale(metadata::setTimeSystem)),

    /** Start time entry. */
    START_TIME((event, context, metadata) -> event.processAsDate(metadata::setStartTime, context)),

    /** Stop time entry. */
    STOP_TIME((event, context, metadata) -> event.processAsDate(metadata::setStopTime, context)),

    /** First participant entry. */
    PARTICIPANT_1((event, context, metadata) -> event.processAsIndexedNormalizedString(metadata::addParticipant, 1)),

    /** Second participant entry. */
    PARTICIPANT_2((event, context, metadata) -> event.processAsIndexedNormalizedString(metadata::addParticipant, 2)),

    /** Third participant entry. */
    PARTICIPANT_3((event, context, metadata) -> event.processAsIndexedNormalizedString(metadata::addParticipant, 3)),

    /** Fourth participant entry. */
    PARTICIPANT_4((event, context, metadata) -> event.processAsIndexedNormalizedString(metadata::addParticipant, 4)),

    /** Fifth participant entry. */
    PARTICIPANT_5((event, context, metadata) -> event.processAsIndexedNormalizedString(metadata::addParticipant, 5)),

    /** Mode entry. */
    MODE((event, context, metadata) -> event.processAsNormalizedString(metadata::setMode)),

    /** Path entry. */
    PATH((event, context, metadata) -> event.processAsNormalizedString(metadata::setPath)),

    /** Path 1 entry. */
    PATH_1((event, context, metadata) -> event.processAsNormalizedString(metadata::setPath1)),

    /** Path 2 entry. */
    PATH_2((event, context, metadata) -> event.processAsNormalizedString(metadata::setPath2)),

    /** Transmit band entry. */
    TRANSMIT_BAND((event, context, metadata) -> event.processAsNormalizedString(metadata::setTransmitBand)),

    /** Receive band entry. */
    RECEIVE_BAND((event, context, metadata) -> event.processAsNormalizedString(metadata::setReceiveBand)),

    /** Turnaround numerator entry. */
    TURNAROUND_NUMERATOR((event, context, metadata) -> event.processAsInteger(metadata::setTurnaroundNumerator)),

    /** turnaround denominator entry. */
    TURNAROUND_DENOMINATOR((event, context, metadata) -> event.processAsInteger(metadata::setTurnaroundDenominator)),

    /** Timetag referene entry. */
    TIMETAG_REF((event, context, metadata) -> event.processAsNormalizedString(metadata::setTimetagRef)),

    /** Integration interval entry. */
    INTEGRATION_INTERVAL((event, context, metadata) -> event.processAsDouble(metadata::setIntegrationInterval)),

    /** Integration reference entry. */
    INTEGRATION_REF((event, context, metadata) -> event.processAsNormalizedString(metadata::setIntegrationRef)),

    /** Frequency offset entry. */
    FREQ_OFFSET((event, context, metadata) -> event.processAsDouble(metadata::setFreqOffset)),

    /** Range mode entry. */
    RANGE_MODE((event, context, metadata) -> event.processAsNormalizedString(metadata::setRangeMode)),

    /** Range modulus entry. */
    RANGE_MODULUS((event, context, metadata) -> event.processAsDouble(metadata::setRangeModulus)),

    /** Range units entry. */
    RANGE_UNITS((event, context, metadata) -> event.processAsNormalizedString(metadata::setRangeUnits)),

    /** Angle type entry. */
    ANGLE_TYPE((event, context, metadata) -> event.processAsNormalizedString(metadata::setAngleType)),

    /** reference frame entry. */
    REFERENCE_FRAME((event, context, metadata) -> event.processAsFrame(metadata::setReferenceFrame, context)),

    /** First transmit delay entry. */
    TRANSMIT_DELAY_1((event, context, metadata) -> event.processAsIndexedDouble(metadata::addTransmitDelay, 1)),

    /** Second transmit delay entry. */
    TRANSMIT_DELAY_2((event, context, metadata) -> event.processAsIndexedDouble(metadata::addTransmitDelay, 2)),

    /** Third transmit delay entry. */
    TRANSMIT_DELAY_3((event, context, metadata) -> event.processAsIndexedDouble(metadata::addTransmitDelay, 3)),

    /** Fourth transmit delay entry. */
    TRANSMIT_DELAY_4((event, context, metadata) -> event.processAsIndexedDouble(metadata::addTransmitDelay, 4)),

    /** Fifth transmit delay entry. */
    TRANSMIT_DELAY_5((event, context, metadata) -> event.processAsIndexedDouble(metadata::addTransmitDelay, 5)),

    /** First receive delay entry. */
    RECEIVE_DELAY_1((event, context, metadata) -> event.processAsIndexedDouble(metadata::addReceiveDelay, 1)),

    /** Second receive delay entry. */
    RECEIVE_DELAY_2((event, context, metadata) -> event.processAsIndexedDouble(metadata::addReceiveDelay, 2)),

    /** Third receive delay entry. */
    RECEIVE_DELAY_3((event, context, metadata) -> event.processAsIndexedDouble(metadata::addReceiveDelay, 3)),

    /** Fourth receive delay entry. */
    RECEIVE_DELAY_4((event, context, metadata) -> event.processAsIndexedDouble(metadata::addReceiveDelay, 4)),

    /** Fifth receive delay entry. */
    RECEIVE_DELAY_5((event, context, metadata) -> event.processAsIndexedDouble(metadata::addReceiveDelay, 5)),

    /** data quality entry. */
    DATA_QUALITY((event, context, metadata) -> event.processAsNormalizedString(metadata::setDataQuality)),

    /** Angle 1 correction entry. */
    CORRECTION_ANGLE_1((event, context, metadata) -> event.processAsDouble(metadata::setCorrectionAngle1)),

    /** Angle 2 correction entry. */
    CORRECTION_ANGLE_2((event, context, metadata) -> event.processAsDouble(metadata::setCorrectionAngle2)),

    /** Doppler correction entry. */
    CORRECTION_DOPPLER((event, context, metadata) -> event.processAsDouble(metadata::setCorrectionDoppler)),

    /** Range correction entry. */
    CORRECTION_RANGE((event, context, metadata) -> event.processAsDouble(metadata::setCorrectionRange)),

    /** Recive correction entry. */
    CORRECTION_RECEIVE((event, context, metadata) -> event.processAsDouble(metadata::setCorrectionReceive)),

    /** Transmit correction entry. */
    CORRECTION_TRANSMIT((event, context, metadata) -> event.processAsDouble(metadata::setCorrectionTransmit)),

    /** Applied correction entry. */
    CORRECTIONS_APPLIED((event, context, metadata) -> event.processAsNormalizedString(metadata::setCorrectionsApplied));

    /** Parsing method. */
    private final MetadataEntryParser parser;

    /** Simple constructor.
     * @param parser parsing method
     */
    TDMMetadataKey(final MetadataEntryParser parser) {
        this.parser = parser;
    }

    /** Parse an event.
     * @param event event to parse
     * @param context parsing context
     * @param metadata metadata to fill
     */
    public void parse(final ParseEvent event, final ParsingContext context, final TDMMetadata metadata) {
        parser.parse(event, context, metadata);
    }

    /** Interface for parsing one metadata entry. */
    interface MetadataEntryParser {
        /** Parse one metadata entry.
         * @param event parse event
         * @param context parsing context
         * @param metadata metadata to fill
         */
        void parse(ParseEvent event, ParsingContext context, TDMMetadata metadata);
    }

}
