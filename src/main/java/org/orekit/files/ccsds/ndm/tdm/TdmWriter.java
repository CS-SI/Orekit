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

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitInternalError;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.KvnStructureKey;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.AbstractMessageWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;


/**
 * Writer for CCSDS Tracking Data Message.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class TdmWriter extends AbstractMessageWriter {

    /** Version number implemented. **/
    public static final double CCSDS_TDM_VERS = 1.0;

    /** Key width for aligning the '=' sign. */
    public static final int KEY_WIDTH = 25;

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param header file header (may be null)
     * @param fileName file name for error messages
     */
    public TdmWriter(final IERSConventions conventions, final DataContext dataContext,
                     final Header header, final String fileName) {
        super(TdmFile.FORMAT_VERSION_KEY, CCSDS_TDM_VERS, header,
              new ContextBinding(
                  () -> conventions, () -> false, () -> dataContext,
                  () -> null, () -> TimeSystem.UTC,
                  () -> 0.0, () -> 1.0),
              fileName);
    }

    /** Write one segment.
     * @param generator generator to use for producing output
     * @param segment segment to write
     * @throws IOException if any buffer writing operations fails
     */
    public void writeSegment(final Generator generator, final Segment<TdmMetadata, ObservationsBlock> segment)
        throws IOException {

        // prepare time converter
        // TODO: check if TDM standard allows or forbids changing time-systems between segment
        // TdmFile.checkTimeSystems says so, but I didn't find any reference in the standard
        final IERSConventions conventions   = getContext().getConventions();
        final boolean         simpleEOP     = getContext().isSimpleEOP();
        final DataContext     dataContext   = getContext().getDataContext();
        final AbsoluteDate    referenceDate = getContext().getReferenceDate();
        final double          clockCount    = getContext().getClockCount();
        final double          clockRate     = getContext().getClockRate();
        setContext(new ContextBinding(
            () -> conventions, () -> simpleEOP, () -> dataContext,
            () -> referenceDate, segment.getMetadata()::getTimeSystem,
            () -> clockCount, () -> clockRate));

        // write the metadata
        writeMetadata(generator, segment.getMetadata());

        // write the observations block
        writeObservationsBlock(generator, segment.getData());

    }

    /** Write one segment metadata.
     * @param generator generator to use for producing output
     * @param metadata metadata to write
     * @throws IOException if any buffer writing operations fails
     */
    private void writeMetadata(final Generator generator, final TdmMetadata metadata) throws IOException {

        // Start metadata
        generator.enterSection(generator.getFormat() == FileFormat.KVN ?
                               KvnStructureKey.META.name() :
                               XmlStructureKey.metadata.name());

        generator.writeComments(metadata);

        // time
        generator.writeEntry(MetadataKey.TIME_SYSTEM.name(),   metadata.getTimeSystem(), true);
        generator.writeEntry(TdmMetadataKey.START_TIME.name(), dateToString(metadata.getStartTime()), false);
        generator.writeEntry(TdmMetadataKey.STOP_TIME.name(),  dateToString(metadata.getStopTime()),  false);

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

        generator.writeEntry(TdmMetadataKey.TRANSMIT_BAND.name(),          metadata.getTransmitBand(),                  false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_BAND.name(),           metadata.getReceiveBand(),                   false);
        generator.writeEntry(TdmMetadataKey.TURNAROUND_NUMERATOR.name(),   metadata.getTurnaroundNumerator(),           false);
        generator.writeEntry(TdmMetadataKey.TURNAROUND_DENOMINATOR.name(), metadata.getTurnaroundDenominator(),         false);
        generator.writeEntry(TdmMetadataKey.TIMETAG_REF.name(),            metadata.getTimetagRef(),                    false);
        generator.writeEntry(TdmMetadataKey.INTEGRATION_INTERVAL.name(),   format(metadata.getIntegrationInterval()),   false);
        generator.writeEntry(TdmMetadataKey.INTEGRATION_REF.name(),        metadata.getIntegrationRef(),                false);
        generator.writeEntry(TdmMetadataKey.FREQ_OFFSET.name(),            format(metadata.getFreqOffset()),            false);
        generator.writeEntry(TdmMetadataKey.RANGE_MODE.name(),             metadata.getRangeMode(),                     false);
        generator.writeEntry(TdmMetadataKey.RANGE_MODULUS.name(),          format(metadata.getRangeModulus()),          false);
        generator.writeEntry(TdmMetadataKey.RANGE_UNITS.name(),            metadata.getRangeUnits(),                    false);
        generator.writeEntry(TdmMetadataKey.ANGLE_TYPE.name(),             metadata.getAngleType(),                     false);
        if (metadata.getReferenceFrame() != null) {
            generator.writeEntry(TdmMetadataKey.REFERENCE_FRAME.name(),    metadata.getReferenceFrame().getName(),      false);
        }
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_1.name(),       format(metadata.getTransmitDelays().get(1)), false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_2.name(),       format(metadata.getTransmitDelays().get(2)), false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_3.name(),       format(metadata.getTransmitDelays().get(3)), false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_4.name(),       format(metadata.getTransmitDelays().get(4)), false);
        generator.writeEntry(TdmMetadataKey.TRANSMIT_DELAY_5.name(),       format(metadata.getTransmitDelays().get(5)), false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_1.name(),        format(metadata.getReceiveDelays().get(1)),  false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_2.name(),        format(metadata.getReceiveDelays().get(2)),  false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_3.name(),        format(metadata.getReceiveDelays().get(3)),  false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_4.name(),        format(metadata.getReceiveDelays().get(4)),  false);
        generator.writeEntry(TdmMetadataKey.RECEIVE_DELAY_5.name(),        format(metadata.getReceiveDelays().get(5)),  false);
        generator.writeEntry(TdmMetadataKey.DATA_QUALITY.name(),           metadata.getDataQuality(),                   false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_ANGLE_1.name(),     format(metadata.getCorrectionAngle1()),      false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_ANGLE_2.name(),     format(metadata.getCorrectionAngle2()),      false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_DOPPLER.name(),     format(metadata.getCorrectionDoppler()),     false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_RANGE.name(),       format(metadata.getCorrectionRange()),       false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_RECEIVE.name(),     format(metadata.getCorrectionReceive()),     false);
        generator.writeEntry(TdmMetadataKey.CORRECTION_TRANSMIT.name(),    format(metadata.getCorrectionTransmit()),    false);
        generator.writeEntry(TdmMetadataKey.CORRECTIONS_APPLIED.name(),    metadata.getCorrectionsApplied(),            false);

        // Stop metadata
        generator.exitSection();

    }

    /** Write one segment observations block.
     * @param generator generator to use for producing output
     * @param observationsBlock observations block
     * @throws IOException if any buffer writing operations fails
     */
    private void writeObservationsBlock(final Generator generator, final ObservationsBlock observationsBlock) throws IOException {

        // Start block
        generator.enterSection(generator.getFormat() == FileFormat.KVN ?
                               KvnStructureKey.DATA.name() :
                               XmlStructureKey.data.name());

        generator.writeComments(observationsBlock);

        // write the data
        for (final Observation observation : observationsBlock.getObservations()) {
            if (generator.getFormat() == FileFormat.KVN) {
                generator.writeEntry(observation.getKeyword(),
                                     dateToString(observation.getEpoch()) + " " + format(observation.getMeasurement()),
                                     false);
            } else {
                // TODO
                throw new OrekitInternalError(null);
            }
        }

        // Stop block
        generator.exitSection();

    }

}
