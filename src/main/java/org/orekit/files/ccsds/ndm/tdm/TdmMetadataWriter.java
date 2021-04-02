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

import java.io.IOException;

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

        // time
        generator.writeEntry(MetadataKey.TIME_SYSTEM.name(),                  metadata.getTimeSystem(), true);
        generator.writeEntry(TdmMetadataKey.START_TIME.name(), timeConverter, metadata.getStartTime(),  false);
        generator.writeEntry(TdmMetadataKey.STOP_TIME.name(),  timeConverter, metadata.getStopTime(),   false);

        // participants
        generator.writeEntry(TdmMetadataKey.PARTICIPANT_1.name(), metadata.getParticipants().get(1), true);
        generator.writeEntry(TdmMetadataKey.PARTICIPANT_2.name(), metadata.getParticipants().get(2), false);
        generator.writeEntry(TdmMetadataKey.PARTICIPANT_3.name(), metadata.getParticipants().get(3), false);
        generator.writeEntry(TdmMetadataKey.PARTICIPANT_4.name(), metadata.getParticipants().get(4), false);
        generator.writeEntry(TdmMetadataKey.PARTICIPANT_5.name(), metadata.getParticipants().get(5), false);

        final TrackingMode mode = metadata.getMode();
        generator.writeEntry(TdmMetadataKey.MODE.name(), mode, false);
        if (mode == TrackingMode.SEQUENTIAL) {
            generator.writeEntry(TdmMetadataKey.PATH.name(), intArrayToString(metadata.getPath()), true);
        } else if (mode == TrackingMode.SINGLE_DIFF) {
            generator.writeEntry(TdmMetadataKey.PATH_1.name(), intArrayToString(metadata.getPath1()), true);
            generator.writeEntry(TdmMetadataKey.PATH_2.name(), intArrayToString(metadata.getPath2()), true);
        }

        generator.writeEntry(TdmMetadataKey.TRANSMIT_BAND.name(),          metadata.getTransmitBand(),          false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_BAND.name(),           metadata.getReceiveBand(),           false);
        generator.writeEntry(TdmMetadataKey.TURNAROUND_NUMERATOR.name(),   metadata.getTurnaroundNumerator(),   false);
        generator.writeEntry(TdmMetadataKey.TURNAROUND_DENOMINATOR.name(), metadata.getTurnaroundDenominator(), false);
        generator.writeEntry(TdmMetadataKey.TIMETAG_REF.name(),            metadata.getTimetagRef(),            false);
        generator.writeEntry(TdmMetadataKey.INTEGRATION_INTERVAL.name(),   metadata.getIntegrationInterval(),   false);
        generator.writeEntry(TdmMetadataKey.INTEGRATION_REF.name(),        metadata.getIntegrationRef(),        false);
        generator.writeEntry(TdmMetadataKey.FREQ_OFFSET.name(),            metadata.getFreqOffset(),            false);
        generator.writeEntry(TdmMetadataKey.RANGE_MODE.name(),             metadata.getRangeMode(),             false);
        generator.writeEntry(TdmMetadataKey.RANGE_MODULUS.name(),          metadata.getRawRangeModulus(),       false);
        generator.writeEntry(TdmMetadataKey.RANGE_UNITS.name(),            metadata.getRangeUnits(),            false);
        generator.writeEntry(TdmMetadataKey.ANGLE_TYPE.name(),             metadata.getAngleType(),             false);
        if (metadata.getReferenceFrame() != null) {
            generator.writeEntry(TdmMetadataKey.REFERENCE_FRAME.name(),    metadata.getReferenceFrame().getName(), false);
        }
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_1.name(),       Unit.SECOND.fromSI(metadata.getTransmitDelays().get(1)), false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_2.name(),       Unit.SECOND.fromSI(metadata.getTransmitDelays().get(2)), false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_3.name(),       Unit.SECOND.fromSI(metadata.getTransmitDelays().get(3)), false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_4.name(),       Unit.SECOND.fromSI(metadata.getTransmitDelays().get(4)), false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_5.name(),       Unit.SECOND.fromSI(metadata.getTransmitDelays().get(5)), false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_1.name(),        Unit.SECOND.fromSI(metadata.getReceiveDelays().get(1)),  false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_2.name(),        Unit.SECOND.fromSI(metadata.getReceiveDelays().get(2)),  false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_3.name(),        Unit.SECOND.fromSI(metadata.getReceiveDelays().get(3)),  false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_4.name(),        Unit.SECOND.fromSI(metadata.getReceiveDelays().get(4)),  false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_5.name(),        Unit.SECOND.fromSI(metadata.getReceiveDelays().get(5)),  false);
        generator.writeEntry(TdmMetadataKey.DATA_QUALITY.name(),           metadata.getDataQuality(),                               false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_ANGLE_1.name(),     Unit.DEGREE.fromSI(metadata.getCorrectionAngle1()),      false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_ANGLE_2.name(),     Unit.DEGREE.fromSI(metadata.getCorrectionAngle2()),      false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_DOPPLER.name(),     Units.KM_PER_S.fromSI(metadata.getCorrectionDoppler()),  false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_RANGE.name(),       Unit.ONE.fromSI(metadata.getRawCorrectionRange()),       false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_RECEIVE.name(),     Unit.HERTZ.fromSI(metadata.getCorrectionReceive()),      false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_TRANSMIT.name(),    Unit.HERTZ.fromSI(metadata.getCorrectionTransmit()),     false);
        generator.writeEntry(TdmMetadataKey.CORRECTIONS_APPLIED.name(),    metadata.getCorrectionsApplied(),                        false);

    }

}
