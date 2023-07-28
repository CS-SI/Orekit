/* Copyright 2023 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Writer for attitude state history data.
 * @author Luc Maisonobe
 * @since 12.0
 */
class AttitudeStateHistoryWriter extends AbstractWriter {

    /** Attitude state history block. */
    private final AttitudeStateHistory history;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param attitudeStateHistory trajectory state history to write
     * @param timeConverter converter for dates
     */
    AttitudeStateHistoryWriter(final AttitudeStateHistory attitudeStateHistory,
                               final TimeConverter timeConverter) {
        super(AcmDataSubStructureKey.att.name(), AcmDataSubStructureKey.ATT.name());
        this.history       = attitudeStateHistory;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // trajectory state history block
        final AttitudeStateHistoryMetadata metadata = history.getMetadata();
        generator.writeComments(metadata.getComments());

        // identifiers
        generator.writeEntry(AttitudeStateHistoryMetadataKey.ATT_ID.name(),       metadata.getAttID(),      null, false);
        generator.writeEntry(AttitudeStateHistoryMetadataKey.ATT_PREV_ID.name(),  metadata.getAttPrevID(),  null, false);
        generator.writeEntry(AttitudeStateHistoryMetadataKey.ATT_BASIS.name(),    metadata.getAttBasis(),   null, false);
        generator.writeEntry(AttitudeStateHistoryMetadataKey.ATT_BASIS_ID.name(), metadata.getAttBasisID(), null, false);

        // references
        generator.writeEntry(AttitudeStateHistoryMetadataKey.REF_FRAME_A.name(),  metadata.getEndpoints().getFrameA().getName(),  null, true);
        generator.writeEntry(AttitudeStateHistoryMetadataKey.REF_FRAME_B.name(),  metadata.getEndpoints().getFrameB().getName(),  null, true);

        // types
        if (metadata.getAttitudeType() == AttitudeElementsType.EULER_ANGLES) {
            generator.writeEntry(AttitudeStateHistoryMetadataKey.EULER_ROT_SEQ.name(), metadata.getEulerRotSeq(), true);
        }
        generator.writeEntry(AttitudeStateHistoryMetadataKey.NUMBER_STATES.name(), metadata.getNbStates(),     true);
        generator.writeEntry(AttitudeStateHistoryMetadataKey.ATT_TYPE.name(),      metadata.getAttitudeType(), true);
        if (metadata.getRateType() != null) {
            generator.writeEntry(AttitudeStateHistoryMetadataKey.RATE_TYPE.name(), metadata.getRateType(),     true);
        }

        // data
        final List<Unit> attUnits  = metadata.getAttitudeType().getUnits();
        final List<Unit> rateUnits = metadata.getRateType() == null ?
                                     Collections.emptyList() : metadata.getRateType().getUnits();
        for (final AttitudeState state : history.getAttitudeStates()) {
            final double[]      elements = state.getElements();
            final StringBuilder line     = new StringBuilder();
            line.append(generator.dateToString(timeConverter, state.getDate()));
            for (int i = 0; i < attUnits.size(); ++i) {
                line.append(' ');
                line.append(AccurateFormatter.format(attUnits.get(i).fromSI(elements[i])));
            }
            for (int i = 0; i < rateUnits.size(); ++i) {
                line.append(' ');
                line.append(AccurateFormatter.format(rateUnits.get(i).fromSI(elements[attUnits.size() + i])));
            }
            if (generator.getFormat() == FileFormat.XML) {
                generator.writeEntry(Acm.ATT_LINE, line.toString(), null, true);
            } else {
                generator.writeRawData(line);
                generator.newLine();
            }
        }

    }

}
