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
package org.orekit.files.ccsds.ndm.tdm;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.utils.units.Unit;


/** Keys for {@link TdmMetadata TDM container} entries.
 * @author Maxime Journot
 * @since 11.0
 */
public enum TdmMetadataKey {

    /** Identifier for the tracking data. */
    TRACK_ID((token, context, container) -> token.processAsNormalizedString(container::setTrackId)),

    /** Lit of data types in the data section. */
    DATA_TYPES((token, context, container) -> token.processAsEnumsList(ObservationType.class, container::setDataTypes)),

    /** Start time entry. */
    START_TIME((token, context, container) -> token.processAsDate(container::setStartTime, context)),

    /** Stop time entry. */
    STOP_TIME((token, context, container) -> token.processAsDate(container::setStopTime, context)),

    /** First participant entry. */
    PARTICIPANT_1((token, context, container) -> token.processAsIndexedNormalizedString(1, container::addParticipant)),

    /** Second participant entry. */
    PARTICIPANT_2((token, context, container) -> token.processAsIndexedNormalizedString(2, container::addParticipant)),

    /** Third participant entry. */
    PARTICIPANT_3((token, context, container) -> token.processAsIndexedNormalizedString(3, container::addParticipant)),

    /** Fourth participant entry. */
    PARTICIPANT_4((token, context, container) -> token.processAsIndexedNormalizedString(4, container::addParticipant)),

    /** Fifth participant entry. */
    PARTICIPANT_5((token, context, container) -> token.processAsIndexedNormalizedString(5, container::addParticipant)),

    /** Mode entry. */
    MODE((token, context, container) -> token.processAsEnum(TrackingMode.class, container::setMode)),

    /** Path entry. */
    PATH((token, context, container) -> token.processAsIntegerArrayNoSpace(container::setPath)),

    /** Path 1 entry. */
    PATH_1((token, context, container) -> token.processAsIntegerArrayNoSpace(container::setPath1)),

    /** Path 2 entry. */
    PATH_2((token, context, container) -> token.processAsIntegerArrayNoSpace(container::setPath2)),

    /** External ephemeris file for the participant 1. */
    EPHEMERIS_NAME_1((token, context, container) -> token.processAsIndexedNormalizedString(1, container::addEphemerisName)),

    /** External ephemeris file for the participant 2. */
    EPHEMERIS_NAME_2((token, context, container) -> token.processAsIndexedNormalizedString(2, container::addEphemerisName)),

    /** External ephemeris file for the participant 3. */
    EPHEMERIS_NAME_3((token, context, container) -> token.processAsIndexedNormalizedString(3, container::addEphemerisName)),

    /** External ephemeris file for the participant 4. */
    EPHEMERIS_NAME_4((token, context, container) -> token.processAsIndexedNormalizedString(4, container::addEphemerisName)),

    /** External ephemeris file for the participant 5. */
    EPHEMERIS_NAME_5((token, context, container) -> token.processAsIndexedNormalizedString(5, container::addEphemerisName)),

    /** Transmit band entry. */
    TRANSMIT_BAND((token, context, container) -> token.processAsUppercaseString(container::setTransmitBand)),

    /** Receive band entry. */
    RECEIVE_BAND((token, context, container) -> token.processAsUppercaseString(container::setReceiveBand)),

    /** Turnaround numerator entry. */
    TURNAROUND_NUMERATOR((token, context, container) -> token.processAsInteger(container::setTurnaroundNumerator)),

    /** turnaround denominator entry. */
    TURNAROUND_DENOMINATOR((token, context, container) -> token.processAsInteger(container::setTurnaroundDenominator)),

    /** Timetag reference entry. */
    TIMETAG_REF((token, context, container) -> token.processAsEnum(TimetagReference.class, container::setTimetagRef)),

    /** Integration interval entry. */
    INTEGRATION_INTERVAL((token, context, container) -> token.processAsDouble(Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                              container::setIntegrationInterval)),

    /** Integration reference entry. */
    INTEGRATION_REF((token, context, container) -> token.processAsEnum(IntegrationReference.class, container::setIntegrationRef)),

    /** Frequency offset entry. */
    FREQ_OFFSET((token, context, container) -> token.processAsDouble(Unit.HERTZ, context.getParsedUnitsBehavior(),
                                                                     container::setFreqOffset)),

    /** Range mode entry. */
    RANGE_MODE((token, context, container) -> token.processAsEnum(RangeMode.class, container::setRangeMode)),

    /** Range modulus entry (beware the unit is Range Units here). */
    RANGE_MODULUS((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                       container::setRawRangeModulus)),

    /** Range units entry. */
    RANGE_UNITS((token, context, container) -> token.processAsEnum(RangeUnits.class, container::setRangeUnits)),

    /** Angle type entry. */
    ANGLE_TYPE((token, context, container) -> token.processAsEnum(AngleType.class, container::setAngleType)),

    /** reference frame entry. */
    REFERENCE_FRAME((token, context, container) -> token.processAsFrame(container::setReferenceFrame, context, true, false, false)),

    /** Interpolation method for transmit phase count. */
    INTERPOLATION((token, context, container) -> token.processAsUppercaseString(container::setInterpolationMethod)),

    /** Interpolation degree for transmit phase count. */
    INTERPOLATION_DEGREE((token, context, container) -> token.processAsInteger(container::setInterpolationDegree)),

    /** Bias that was added to Doppler count in the data section. */
    DOPPLER_COUNT_BIAS((token, context, container) -> token.processAsDouble(Unit.HERTZ, context.getParsedUnitsBehavior(),
                                                                            container::setDopplerCountBias)),

    /** Scaled by which Doppler count was multiplied in the data section. */
    DOPPLER_COUNT_SCALE((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                            container::setDopplerCountScale)),

    /** Indicator for occurred rollover in Doppler count. */
    DOPPLER_COUNT_ROLLOVER((token, context, container) -> token.processAsBoolean(container::setDopplerCountRollover)),

    /** First transmit delay entry. */
    TRANSMIT_DELAY_1((token, context, container) -> token.processAsIndexedDouble(1, Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                                 container::addTransmitDelay)),

    /** Second transmit delay entry. */
    TRANSMIT_DELAY_2((token, context, container) -> token.processAsIndexedDouble(2, Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                                 container::addTransmitDelay)),

    /** Third transmit delay entry. */
    TRANSMIT_DELAY_3((token, context, container) -> token.processAsIndexedDouble(3, Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                                 container::addTransmitDelay)),

    /** Fourth transmit delay entry. */
    TRANSMIT_DELAY_4((token, context, container) -> token.processAsIndexedDouble(4, Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                                 container::addTransmitDelay)),

    /** Fifth transmit delay entry. */
    TRANSMIT_DELAY_5((token, context, container) -> token.processAsIndexedDouble(5, Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                                 container::addTransmitDelay)),

    /** First receive delay entry. */
    RECEIVE_DELAY_1((token, context, container) -> token.processAsIndexedDouble(1, Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                                container::addReceiveDelay)),

    /** Second receive delay entry. */
    RECEIVE_DELAY_2((token, context, container) -> token.processAsIndexedDouble(2, Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                                container::addReceiveDelay)),

    /** Third receive delay entry. */
    RECEIVE_DELAY_3((token, context, container) -> token.processAsIndexedDouble(3, Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                                container::addReceiveDelay)),

    /** Fourth receive delay entry. */
    RECEIVE_DELAY_4((token, context, container) -> token.processAsIndexedDouble(4, Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                                container::addReceiveDelay)),

    /** Fifth receive delay entry. */
    RECEIVE_DELAY_5((token, context, container) -> token.processAsIndexedDouble(5, Unit.SECOND, context.getParsedUnitsBehavior(),
                                                                                container::addReceiveDelay)),

    /** data quality entry. */
    DATA_QUALITY((token, context, container) -> token.processAsEnum(DataQuality.class, container::setDataQuality)),

    /** Angle 1 correction entry. */
    CORRECTION_ANGLE_1((token, context, container) -> token.processAsDouble(Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                            container::setCorrectionAngle1)),

    /** Angle 2 correction entry. */
    CORRECTION_ANGLE_2((token, context, container) -> token.processAsDouble(Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                            container::setCorrectionAngle2)),

    /** Doppler correction entry. */
    CORRECTION_DOPPLER((token, context, container) -> token.processAsDouble(Units.KM_PER_S, context.getParsedUnitsBehavior(),
                                                                            container::setCorrectionDoppler)),

    /** Magnitude correction entry. */
    CORRECTION_MAG((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                        container::setCorrectionMagnitude)),

    /** Range correction entry (beware the unit is Range Units here). */
    CORRECTION_RANGE((token, context, container) -> token.processAsDouble(Unit.ONE, context.getParsedUnitsBehavior(),
                                                                          container::setRawCorrectionRange)),

    /** Radar Cross Section correction entry. */
    CORRECTION_RCS((token, context, container) -> token.processAsDouble(Units.M2, context.getParsedUnitsBehavior(),
                                                                        container::setCorrectionRcs)),

    /** Receive correction entry. */
    CORRECTION_RECEIVE((token, context, container) -> token.processAsDouble(Unit.HERTZ, context.getParsedUnitsBehavior(),
                                                                            container::setCorrectionReceive)),

    /** Transmit correction entry. */
    CORRECTION_TRANSMIT((token, context, container) -> token.processAsDouble(Unit.HERTZ, context.getParsedUnitsBehavior(),
                                                                             container::setCorrectionTransmit)),

    /** Yearly aberration correction entry. */
    CORRECTION_ABERRATION_YEARLY((token, context, container) -> token.processAsDouble(Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                                      container::setCorrectionAberrationYearly)),

    /** Diurnal aberration correction entry. */
    CORRECTION_ABERRATION_DIURNAL((token, context, container) -> token.processAsDouble(Unit.DEGREE, context.getParsedUnitsBehavior(),
                                                                                       container::setCorrectionAberrationDiurnal)),

    /** Applied correction entry. */
    CORRECTIONS_APPLIED((token, context, container) -> token.processAsEnum(CorrectionApplied.class, container::setCorrectionsApplied));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    TdmMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true if token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final TdmMetadata container) {
        return processor.process(token, context, container);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param container container to fill
     * @return true if token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, TdmMetadata container);
    }

}
