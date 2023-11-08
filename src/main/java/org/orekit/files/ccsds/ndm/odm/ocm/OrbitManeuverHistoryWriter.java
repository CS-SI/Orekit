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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.io.IOException;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.files.ccsds.definitions.DutyCycleType;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Writer for maneuvers history data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class OrbitManeuverHistoryWriter extends AbstractWriter {

    /** Maneuvers history block. */
    private final OrbitManeuverHistory history;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param maneuverHistory maneuvers history to write
     * @param timeConverter converter for dates
     */
    OrbitManeuverHistoryWriter(final OrbitManeuverHistory maneuverHistory,
                          final TimeConverter timeConverter) {
        super(OcmDataSubStructureKey.man.name(), OcmDataSubStructureKey.MAN.name());
        this.history       = maneuverHistory;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // maneuvers history block
        final OrbitManeuverHistoryMetadata metadata = history.getMetadata();
        generator.writeComments(metadata.getComments());

        // identifiers
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_ID.name(),        metadata.getManID(),       null, false);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_PREV_ID.name(),   metadata.getManPrevID(),   null, false);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_NEXT_ID.name(),   metadata.getManNextID(),   null, false);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_BASIS.name(),     metadata.getManBasis(),          false);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_BASIS_ID.name(),  metadata.getManBasisID(),  null, false);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_DEVICE_ID.name(), metadata.getManDeviceID(), null, false);

        // time
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_PREV_EPOCH.name(), timeConverter, metadata.getManPrevEpoch(), true, false);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_NEXT_EPOCH.name(), timeConverter, metadata.getManNextEpoch(), true, false);

        // references
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_PURPOSE.name(),      metadata.getManPurpose(),                          false);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_PRED_SOURCE.name(),  metadata.getManPredSource(),                 null, false);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_REF_FRAME.name(),    metadata.getManReferenceFrame().getName(),   null, false);
        if (!metadata.getManFrameEpoch().equals(timeConverter.getReferenceDate()) &&
            metadata.getManReferenceFrame().asOrbitRelativeFrame() == null &&
            metadata.getManReferenceFrame().asSpacecraftBodyFrame() == null) {
            generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_FRAME_EPOCH.name(),  timeConverter, metadata.getManFrameEpoch(),  true, false);
        }
        if (metadata.getGravitationalAssist() != null) {
            generator.writeEntry(OrbitManeuverHistoryMetadataKey.GRAV_ASSIST_NAME.name(), metadata.getGravitationalAssist().getName(), null, false);
        }

        // duty cycle
        final boolean notContinuous = metadata.getDcType() != DutyCycleType.CONTINUOUS;
        final boolean timeAndAngle  = metadata.getDcType() == DutyCycleType.TIME_AND_ANGLE;
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_TYPE.name(), metadata.getDcType(), false);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_WIN_OPEN.name(),  timeConverter, metadata.getDcWindowOpen(),  false, notContinuous);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_WIN_CLOSE.name(), timeConverter, metadata.getDcWindowClose(), false, notContinuous);
        if (metadata.getDcMinCycles() >= 0) {
            generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_MIN_CYCLES.name(), metadata.getDcMinCycles(), false);
        }
        if (metadata.getDcMaxCycles() >= 0) {
            generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_MAX_CYCLES.name(), metadata.getDcMaxCycles(), false);
        }
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_EXEC_START.name(),          timeConverter, metadata.getDcExecStart(), false, notContinuous);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_EXEC_STOP.name(),           timeConverter, metadata.getDcExecStop(),  false, notContinuous);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_REF_TIME.name(),            timeConverter, metadata.getDcRefTime(),   false, notContinuous);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_TIME_PULSE_DURATION.name(), metadata.getDcTimePulseDuration(), Unit.SECOND,  notContinuous);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_TIME_PULSE_PERIOD.name(),   metadata.getDcTimePulsePeriod(),   Unit.SECOND,  notContinuous);
        if (timeAndAngle) {
            generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_REF_DIR.name(), toString(metadata.getDcRefDir()), null, timeAndAngle);
            generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_BODY_FRAME.name(),
                                 metadata.getDcBodyFrame().toString().replace(' ', '_'),
                                 null, timeAndAngle);
            generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_BODY_TRIGGER.name(),   toString(metadata.getDcBodyTrigger()), null,   timeAndAngle);
            generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_PA_START_ANGLE.name(), metadata.getDcPhaseStartAngle(), Unit.DEGREE,  timeAndAngle);
            generator.writeEntry(OrbitManeuverHistoryMetadataKey.DC_PA_STOP_ANGLE.name(),  metadata.getDcPhaseStopAngle(),  Unit.DEGREE,  timeAndAngle);
        }

        // elements
        final List<ManeuverFieldType> types       = metadata.getManComposition();
        final StringBuilder           composition = new StringBuilder();
        for (int i = 0; i < types.size(); ++i) {
            if (i > 0) {
                composition.append(',');
            }
            composition.append(types.get(i).name());
        }
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_COMPOSITION.name(), composition.toString(),                        null, false);
        generator.writeEntry(OrbitManeuverHistoryMetadataKey.MAN_UNITS.name(), generator.unitsListToString(metadata.getManUnits()), null, false);

        // data
        for (final OrbitManeuver maneuver : history.getManeuvers()) {
            final StringBuilder line = new StringBuilder();
            for (int i = 0; i < types.size(); ++i) {
                if (i > 0) {
                    line.append(' ');
                }
                line.append(types.get(i).outputField(timeConverter, maneuver));
            }
            if (generator.getFormat() == FileFormat.XML) {
                generator.writeEntry(Ocm.MAN_LINE, line.toString(), null, true);
            } else {
                generator.writeRawData(line);
                generator.newLine();
            }
        }
    }

    /** Convert a vector to a space separated string.
     * @param vector vector to convert
     * @return orrespondong string
     */
    private String toString(final Vector3D vector) {
        final StringBuilder builder = new StringBuilder();
        builder.append(AccurateFormatter.format(Unit.ONE.fromSI(vector.getX())));
        builder.append(' ');
        builder.append(AccurateFormatter.format(Unit.ONE.fromSI(vector.getY())));
        builder.append(' ');
        builder.append(AccurateFormatter.format(Unit.ONE.fromSI(vector.getZ())));
        return builder.toString();
    }

}
