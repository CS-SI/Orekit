/* Copyright 2002-2022 CS GROUP
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
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Writer for trajectory state history data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class TrajectoryStateHistoryWriter extends AbstractWriter {

    /** Trajectory state history block. */
    private final TrajectoryStateHistory history;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param trajectoryStateHistory trajectory state history to write
     * @param timeConverter converter for dates
     */
    TrajectoryStateHistoryWriter(final TrajectoryStateHistory trajectoryStateHistory,
                                 final TimeConverter timeConverter) {
        super(OcmDataSubStructureKey.traj.name(), OcmDataSubStructureKey.TRAJ.name());
        this.history       = trajectoryStateHistory;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // trajectory state history block
        final TrajectoryStateHistoryMetadata metadata = history.getMetadata();
        generator.writeComments(metadata.getComments());

        // identifiers
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.TRAJ_ID.name(),       metadata.getTrajID(),      null, false);
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.TRAJ_PREV_ID.name(),  metadata.getTrajPrevID(),  null, false);
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.TRAJ_NEXT_ID.name(),  metadata.getTrajNextID(),  null, false);
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.TRAJ_BASIS.name(),    metadata.getTrajBasis(),   null, false);
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.TRAJ_BASIS_ID.name(), metadata.getTrajBasisID(), null, false);

        // interpolation
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.INTERPOLATION.name(),        metadata.getInterpolationMethod(), false);
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.INTERPOLATION_DEGREE.name(), metadata.getInterpolationDegree(), false);
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.ORB_AVERAGING.name(),        metadata.getOrbAveraging(), null,  false);

        // references
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.CENTER_NAME.name(),      metadata.getCenter().getName(),              null, false);
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.TRAJ_REF_FRAME.name(),   metadata.getTrajReferenceFrame().getName(),  null, false);
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.TRAJ_FRAME_EPOCH.name(), timeConverter, metadata.getTrajFrameEpoch(),       false);

        // time
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.USEABLE_START_TIME.name(), timeConverter, metadata.getUseableStartTime(), false);
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.USEABLE_STOP_TIME.name(),  timeConverter, metadata.getUseableStopTime(),  false);

        // revolution  numbers
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.ORB_REVNUM.name(),       metadata.getOrbRevNum(),      false);
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.ORB_REVNUM_BASIS.name(), metadata.getOrbRevNumBasis(), false);

        // elements
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.TRAJ_TYPE.name(),   metadata.getTrajType(),                                     true);
        generator.writeEntry(TrajectoryStateHistoryMetadataKey.TRAJ_UNITS.name(), generator.unitsListToString(metadata.getTrajUnits()), null, false);

        // data
        final List<Unit> units = metadata.getTrajType().getUnits();
        for (final TrajectoryState state : history.getTrajectoryStates()) {
            final double[]      elements = state.getElements();
            final StringBuilder line     = new StringBuilder();
            line.append(generator.dateToString(timeConverter, state.getDate()));
            for (int i = 0; i < units.size(); ++i) {
                line.append(' ');
                line.append(AccurateFormatter.format(units.get(i).fromSI(elements[i])));
            }
            if (generator.getFormat() == FileFormat.XML) {
                generator.writeEntry(Ocm.TRAJ_LINE, line.toString(), null, true);
            } else {
                generator.writeRawData(line);
                generator.newLine();
            }
        }

    }

}
