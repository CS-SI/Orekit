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

/** Writer for orbit state history data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class OrbitStateHistoryWriter extends AbstractWriter {

    /** Orbit state history block. */
    private final OrbitStateHistory history;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param orbitStateHistory orbit state history to write
     * @param timeConverter converter for dates
     */
    OrbitStateHistoryWriter(final OrbitStateHistory orbitStateHistory,
                            final TimeConverter timeConverter) {
        super(OcmDataSubStructureKey.orb.name(), OcmDataSubStructureKey.ORB.name());
        this.history       = orbitStateHistory;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // orbit state history block
        final OrbitStateHistoryMetadata metadata = history.getMetadata();
        generator.writeComments(metadata.getComments());

        // identifiers
        generator.writeEntry(OrbitStateHistoryMetadataKey.ORB_ID.name(),       metadata.getOrbID(),      false);
        generator.writeEntry(OrbitStateHistoryMetadataKey.ORB_PREV_ID.name(),  metadata.getOrbPrevID(),  false);
        generator.writeEntry(OrbitStateHistoryMetadataKey.ORB_NEXT_ID.name(),  metadata.getOrbNextID(),  false);
        generator.writeEntry(OrbitStateHistoryMetadataKey.ORB_BASIS.name(),    metadata.getOrbBasis(),   false);
        generator.writeEntry(OrbitStateHistoryMetadataKey.ORB_BASIS_ID.name(), metadata.getOrbBasisID(), false);

        // interpolation
        generator.writeEntry(OrbitStateHistoryMetadataKey.INTERPOLATION.name(),        metadata.getInterpolationMethod(), false);
        generator.writeEntry(OrbitStateHistoryMetadataKey.INTERPOLATION_DEGREE.name(), metadata.getInterpolationDegree(), false);
        generator.writeEntry(OrbitStateHistoryMetadataKey.ORB_AVERAGING.name(),        metadata.getOrbAveraging(),        false);

        // references
        generator.writeEntry(OrbitStateHistoryMetadataKey.CENTER_NAME.name(),   metadata.getCenter().getName(),               false);
        generator.writeEntry(OrbitStateHistoryMetadataKey.ORB_REF_FRAME.name(), metadata.getOrbReferenceFrame().getName(),    false);
        generator.writeEntry(OrbitStateHistoryMetadataKey.ORB_FRAME_EPOCH.name(), timeConverter, metadata.getOrbFrameEpoch(), false);

        // time
        generator.writeEntry(OrbitStateHistoryMetadataKey.USEABLE_START_TIME.name(), timeConverter, metadata.getUseableStartTime(), false);
        generator.writeEntry(OrbitStateHistoryMetadataKey.USEABLE_STOP_TIME.name(),  timeConverter, metadata.getUseableStopTime(),  false);

        // elements
        generator.writeEntry(OrbitStateHistoryMetadataKey.ORB_TYPE.name(), metadata.getOrbType(), true);
        generator.writeEntry(OrbitStateHistoryMetadataKey.ORB_UNITS.name(), Units.outputBracketed(metadata.getOrbUnits()), false);

        // data
        final List<Unit> units = metadata.getOrbType().getUnits();
        for (final OrbitState state : history.getOrbitalStates()) {
            final double[]      elements = state.getElements();
            final StringBuilder line     = new StringBuilder();
            line.append(generator.dateToString(timeConverter, state.getDate()));
            for (int i = 0; i < units.size(); ++i) {
                line.append(' ');
                line.append(AccurateFormatter.format(units.get(i).fromSI(elements[i])));
            }
            if (generator.getFormat() == FileFormat.XML) {
                generator.writeEntry(OcmFile.ORB_LINE, line.toString(), true);
            } else {
                generator.writeRawData(line);
                generator.newLine();
            }
        }

    }

}
