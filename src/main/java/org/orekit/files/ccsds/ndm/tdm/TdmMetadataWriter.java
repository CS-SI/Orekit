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

import java.io.IOException;
import java.util.List;

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.section.KvnStructureKey;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;


/**
 * Writer for CCSDS Tracking Data Message metadata.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
class TdmMetadataWriter extends AbstractWriter {

    /** Metadata. */
    private final TdmMetadata metadata;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Simple constructor.
     * @param metadata metadata to write
     * @param timeConverter converter for dates
     */
    TdmMetadataWriter(final TdmMetadata metadata, final TimeConverter timeConverter) {
        super(XmlStructureKey.metadata.name(), KvnStructureKey.META.name());
        this.metadata     = metadata;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(metadata.getComments());

        generator.writeEntry(TdmMetadataKey.TRACK_ID.name(), metadata.getTrackId(), null, false);
        final List<ObservationType> dataTypes = metadata.getDataTypes();
        if (dataTypes != null && !dataTypes.isEmpty()) {
            final StringBuilder dataTypesNames = new StringBuilder();
            for (int i = 0; i < dataTypes.size(); ++i) {
                if (i > 0) {
                    dataTypesNames.append(',');
                }
                dataTypesNames.append(dataTypes.get(i).name());
            }
            generator.writeEntry(TdmMetadataKey.DATA_TYPES.name(), dataTypesNames.toString(), null, false);
        }

        // time
        generator.writeEntry(MetadataKey.TIME_SYSTEM.name(),                  metadata.getTimeSystem(), true);
        generator.writeEntry(TdmMetadataKey.START_TIME.name(), timeConverter, metadata.getStartTime(),  false, false);
        generator.writeEntry(TdmMetadataKey.STOP_TIME.name(),  timeConverter, metadata.getStopTime(),   false, false);

        // participants
        generator.writeEntry(TdmMetadataKey.PARTICIPANT_1.name(), metadata.getParticipants().get(1), null, true);
        generator.writeEntry(TdmMetadataKey.PARTICIPANT_2.name(), metadata.getParticipants().get(2), null, false);
        generator.writeEntry(TdmMetadataKey.PARTICIPANT_3.name(), metadata.getParticipants().get(3), null, false);
        generator.writeEntry(TdmMetadataKey.PARTICIPANT_4.name(), metadata.getParticipants().get(4), null, false);
        generator.writeEntry(TdmMetadataKey.PARTICIPANT_5.name(), metadata.getParticipants().get(5), null, false);

        final TrackingMode mode = metadata.getMode();
        generator.writeEntry(TdmMetadataKey.MODE.name(), mode, false);
        if (mode == TrackingMode.SEQUENTIAL) {
            generator.writeEntry(TdmMetadataKey.PATH.name(), intArrayToString(metadata.getPath()),  null, true);
        } else if (mode == TrackingMode.SINGLE_DIFF) {
            generator.writeEntry(TdmMetadataKey.PATH_1.name(), intArrayToString(metadata.getPath1()), null, true);
            generator.writeEntry(TdmMetadataKey.PATH_2.name(), intArrayToString(metadata.getPath2()), null, true);
        }

        generator.writeEntry(TdmMetadataKey.EPHEMERIS_NAME_1.name(),       metadata.getEphemerisNames().get(1), null, false);
        generator.writeEntry(TdmMetadataKey.EPHEMERIS_NAME_2.name(),       metadata.getEphemerisNames().get(2), null, false);
        generator.writeEntry(TdmMetadataKey.EPHEMERIS_NAME_3.name(),       metadata.getEphemerisNames().get(3), null, false);
        generator.writeEntry(TdmMetadataKey.EPHEMERIS_NAME_4.name(),       metadata.getEphemerisNames().get(4), null, false);
        generator.writeEntry(TdmMetadataKey.EPHEMERIS_NAME_5.name(),       metadata.getEphemerisNames().get(5), null, false);

        generator.writeEntry(TdmMetadataKey.TRANSMIT_BAND.name(),          metadata.getTransmitBand(), null, false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_BAND.name(),           metadata.getReceiveBand(),  null, false);
        if (metadata.getTurnaroundNumerator() != 0 || metadata.getTurnaroundDenominator() != 0) {
            generator.writeEntry(TdmMetadataKey.TURNAROUND_NUMERATOR.name(),   metadata.getTurnaroundNumerator(),   false);
            generator.writeEntry(TdmMetadataKey.TURNAROUND_DENOMINATOR.name(), metadata.getTurnaroundDenominator(), false);
        }
        generator.writeEntry(TdmMetadataKey.TIMETAG_REF.name(),            metadata.getTimetagRef(),                       false);
        generator.writeEntry(TdmMetadataKey.INTEGRATION_INTERVAL.name(),   metadata.getIntegrationInterval(), Unit.SECOND, false);
        generator.writeEntry(TdmMetadataKey.INTEGRATION_REF.name(),        metadata.getIntegrationRef(),                   false);
        generator.writeEntry(TdmMetadataKey.FREQ_OFFSET.name(),            metadata.getFreqOffset(),          Unit.HERTZ,  false);
        generator.writeEntry(TdmMetadataKey.RANGE_MODE.name(),             metadata.getRangeMode(),                        false);
        if (metadata.getRawRangeModulus() != 0) {
            generator.writeEntry(TdmMetadataKey.RANGE_MODULUS.name(),      metadata.getRawRangeModulus(),     Unit.ONE,    false);
        }
        generator.writeEntry(TdmMetadataKey.RANGE_UNITS.name(),            metadata.getRangeUnits(),                       false);
        generator.writeEntry(TdmMetadataKey.ANGLE_TYPE.name(),             metadata.getAngleType(),                        false);
        if (metadata.getReferenceFrame() != null) {
            generator.writeEntry(TdmMetadataKey.REFERENCE_FRAME.name(),    metadata.getReferenceFrame().getName(), null, false);
        }

        // interpolation
        if (metadata.getInterpolationMethod() != null) {
            generator.writeEntry(TdmMetadataKey.INTERPOLATION.name(),
                                 metadata.getInterpolationMethod(),
                                 null, true);
            generator.writeEntry(TdmMetadataKey.INTERPOLATION_DEGREE.name(),
                                 Integer.toString(metadata.getInterpolationDegree()),
                                 null, true);
        }

        // Doppler
        if (metadata.getDopplerCountBias() != 0) {
            generator.writeEntry(TdmMetadataKey.DOPPLER_COUNT_BIAS.name(),  metadata.getDopplerCountBias(),  Unit.HERTZ, false);
        }
        if (metadata.getDopplerCountScale() != 1) {
            generator.writeEntry(TdmMetadataKey.DOPPLER_COUNT_SCALE.name(), metadata.getDopplerCountScale(), Unit.ONE,   false);
        }
        if (metadata.hasDopplerCountRollover()) {
            generator.writeEntry(TdmMetadataKey.DOPPLER_COUNT_BIAS.name(),  metadata.hasDopplerCountRollover() ? "YES" : "NO", null, false);
        }

        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_1.name(),       metadata.getTransmitDelays().get(1), Unit.SECOND, false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_2.name(),       metadata.getTransmitDelays().get(2), Unit.SECOND, false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_3.name(),       metadata.getTransmitDelays().get(3), Unit.SECOND, false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_4.name(),       metadata.getTransmitDelays().get(4), Unit.SECOND, false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_5.name(),       metadata.getTransmitDelays().get(5), Unit.SECOND, false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_1.name(),        metadata.getReceiveDelays().get(1),  Unit.SECOND, false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_2.name(),        metadata.getReceiveDelays().get(2),  Unit.SECOND, false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_3.name(),        metadata.getReceiveDelays().get(3),  Unit.SECOND, false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_4.name(),        metadata.getReceiveDelays().get(4),  Unit.SECOND, false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_5.name(),        metadata.getReceiveDelays().get(5),  Unit.SECOND, false);
        generator.writeEntry(TdmMetadataKey.DATA_QUALITY.name(),           metadata.getDataQuality(),                        false);
        if (metadata.getCorrectionAngle1() != 0) {
            generator.writeEntry(TdmMetadataKey.CORRECTION_ANGLE_1.name(),  metadata.getCorrectionAngle1(), Unit.DEGREE,     false);
        }
        if (metadata.getCorrectionAngle2() != 0) {
            generator.writeEntry(TdmMetadataKey.CORRECTION_ANGLE_2.name(),  metadata.getCorrectionAngle2(), Unit.DEGREE,     false);
        }
        if (metadata.getCorrectionDoppler() != 0) {
            generator.writeEntry(TdmMetadataKey.CORRECTION_DOPPLER.name(),  metadata.getCorrectionDoppler(), Units.KM_PER_S, false);
        }
        if (metadata.getCorrectionMagnitude() != 0) {
            generator.writeEntry(TdmMetadataKey.CORRECTION_MAG.name(),      metadata.getCorrectionMagnitude(), Unit.ONE,     false);
        }
        if (metadata.getRawCorrectionRange() != 0) {
            generator.writeEntry(TdmMetadataKey.CORRECTION_RANGE.name(),    metadata.getRawCorrectionRange(), Unit.ONE,      false);
        }
        if (metadata.getCorrectionRcs() != 0) {
            generator.writeEntry(TdmMetadataKey.CORRECTION_RCS.name(),      metadata.getCorrectionRcs(),      Units.M2,      false);
        }
        if (metadata.getCorrectionReceive() != 0) {
            generator.writeEntry(TdmMetadataKey.CORRECTION_RECEIVE.name(),  metadata.getCorrectionReceive(), Unit.HERTZ,     false);
        }
        if (metadata.getCorrectionTransmit() != 0) {
            generator.writeEntry(TdmMetadataKey.CORRECTION_TRANSMIT.name(), metadata.getCorrectionTransmit(), Unit.HERTZ,    false);
        }
        if (metadata.getCorrectionAberrationYearly() != 0) {
            generator.writeEntry(TdmMetadataKey.CORRECTION_ABERRATION_YEARLY.name(), metadata.getCorrectionAberrationYearly(), Unit.DEGREE,    false);
        }
        if (metadata.getCorrectionAberrationDiurnal() != 0) {
            generator.writeEntry(TdmMetadataKey.CORRECTION_ABERRATION_DIURNAL.name(), metadata.getCorrectionAberrationDiurnal(), Unit.DEGREE,    false);
        }
        generator.writeEntry(TdmMetadataKey.CORRECTIONS_APPLIED.name(),     metadata.getCorrectionsApplied(),                false);

    }

}
