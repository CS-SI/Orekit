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

import java.io.IOException;
import java.util.List;

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Writer for maneuvers history data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class ManeuverHistoryWriter extends AbstractWriter {

    /** Maneuvers history block. */
    private final ManeuverHistory history;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param maneuverHistory maneuvers history to write
     * @param timeConverter converter for dates
     */
    ManeuverHistoryWriter(final ManeuverHistory maneuverHistory,
                          final TimeConverter timeConverter) {
        super(OcmDataSubStructureKey.man.name(), OcmDataSubStructureKey.MAN.name());
        this.history       = maneuverHistory;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // maneuvers history block
        final ManeuverHistoryMetadata metadata = history.getMetadata();
        generator.writeComments(metadata.getComments());

        // identifiers
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_ID.name(),        metadata.getManID(),       false);
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_PREV_ID.name(),   metadata.getManPrevID(),   false);
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_NEXT_ID.name(),   metadata.getManNextID(),   false);
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_BASIS.name(),     metadata.getManBasis(),    false);
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_BASIS_ID.name(),  metadata.getManBasisID(),  false);
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_DEVICE_ID.name(), metadata.getManDeviceID(), false);

        // time
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_PREV_EPOCH.name(), timeConverter, metadata.getManPrevEpoch(), false);
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_NEXT_EPOCH.name(), timeConverter, metadata.getManNextEpoch(), false);

        // references
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_PURPOSE.name(),      metadata.getManPurpose(),                    false);
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_PRED_SOURCE.name(),  metadata.getManPredSource(),                 false);
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_REF_FRAME.name(),    metadata.getManReferenceFrame().getName(),   false);
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_FRAME_EPOCH.name(),  timeConverter, metadata.getManFrameEpoch(),  false);
        if (metadata.getGravitationalAssist() != null) {
            generator.writeEntry(ManeuverHistoryMetadataKey.GRAV_ASSIST_NAME.name(), metadata.getGravitationalAssist().getName(), false);
        }

        // duty cycle
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_TYPE.name(),                metadata.getDcType(),                           false);
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_WIN_OPEN.name(),            timeConverter, metadata.getDcWindowOpen(),      false);
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_WIN_CLOSE.name(),           timeConverter, metadata.getDcWindowClose(),     false);
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_MIN_CYCLES.name(),          metadata.getDcMinCycles(),                      false);
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_MAX_CYCLES.name(),          metadata.getDcMaxCycles(),                      false);
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_EXEC_START.name(),          timeConverter, metadata.getDcExecStart(),       false);
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_EXEC_STOP.name(),           timeConverter, metadata.getDcExecStop(),        false);
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_REF_TIME.name(),            timeConverter, metadata.getDcRefTime(),         false);
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_TIME_PULSE_DURATION.name(), metadata.getDcTimePulseDuration(), Unit.SECOND, false);
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_TIME_PULSE_PERIOD.name(),   metadata.getDcTimePulsePeriod(),   Unit.SECOND, false);
        if (metadata.getDcRefDir() != null) {
            final StringBuilder value = new StringBuilder();
            value.append(AccurateFormatter.format(Unit.ONE.fromSI(metadata.getDcRefDir().getX())));
            value.append(' ');
            value.append(AccurateFormatter.format(Unit.ONE.fromSI(metadata.getDcRefDir().getY())));
            value.append(' ');
            value.append(AccurateFormatter.format(Unit.ONE.fromSI(metadata.getDcRefDir().getZ())));
            generator.writeEntry(ManeuverHistoryMetadataKey.DC_REF_DIR.name(), value.toString(), false);
        }
        if (metadata.getDcBodyFrame() != null) {
            generator.writeEntry(ManeuverHistoryMetadataKey.DC_BODY_FRAME.name(),
                                 metadata.getDcBodyFrame().toString().replace(' ', '_'),
                                 false);
        }
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_PA_START_ANGLE.name(), metadata.getDcPhaseStartAngle(), Unit.DEGREE, false);
        generator.writeEntry(ManeuverHistoryMetadataKey.DC_PA_STOP_ANGLE.name(),  metadata.getDcPhaseStopAngle(), Unit.DEGREE,  false);

        // elements
        final List<ManeuverFieldType> types       = metadata.getManComposition();
        final StringBuilder           composition = new StringBuilder();
        for (int i = 0; i < types.size(); ++i) {
            if (i > 0) {
                composition.append(' ');
            }
            composition.append(types.get(i).name());
        }
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_COMPOSITION.name(), composition.toString(),                  false);
        generator.writeEntry(ManeuverHistoryMetadataKey.MAN_UNITS.name(), Units.outputBracketed(metadata.getManUnits()), false);

        // data
        for (final Maneuver maneuver : history.getManeuvers()) {
            final StringBuilder line = new StringBuilder();
            for (int i = 0; i < types.size(); ++i) {
                if (i > 0) {
                    line.append(' ');
                }
                line.append(types.get(i).outputField(timeConverter, maneuver));
            }
            if (generator.getFormat() == FileFormat.XML) {
                generator.writeEntry(OcmFile.MAN_LINE, line.toString(), true);
            } else {
                generator.writeRawData(line);
                generator.newLine();
            }
        }
    }

}
