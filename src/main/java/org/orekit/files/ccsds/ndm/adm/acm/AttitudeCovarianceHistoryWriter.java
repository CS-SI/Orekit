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
import java.util.List;

import org.hipparchus.linear.DiagonalMatrix;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Writer for covariance history data.
 * @author Luc Maisonobe
 * @since 12.0
 */
class AttitudeCovarianceHistoryWriter extends AbstractWriter {

    /** Covariance history block. */
    private final AttitudeCovarianceHistory history;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param covarianceHistory covariance history to write
     * @param timeConverter converter for dates
     */
    AttitudeCovarianceHistoryWriter(final AttitudeCovarianceHistory covarianceHistory,
                                    final TimeConverter timeConverter) {
        super(AcmDataSubStructureKey.cov.name(), AcmDataSubStructureKey.COV.name());
        this.history       = covarianceHistory;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // covariance history block
        final AttitudeCovarianceHistoryMetadata metadata = history.getMetadata();
        generator.writeComments(metadata.getComments());

        // identifiers
        generator.writeEntry(AttitudeCovarianceHistoryMetadataKey.COV_ID.name(),       metadata.getCovID(),      null, false);
        generator.writeEntry(AttitudeCovarianceHistoryMetadataKey.COV_PREV_ID.name(),  metadata.getCovPrevID(),  null, false);
        generator.writeEntry(AttitudeCovarianceHistoryMetadataKey.COV_BASIS.name(),    metadata.getCovBasis(),   null, false);
        generator.writeEntry(AttitudeCovarianceHistoryMetadataKey.COV_BASIS_ID.name(), metadata.getCovBasisID(), null, false);

        // references
        if (metadata.getCovReferenceFrame() != null) {
            generator.writeEntry(AttitudeCovarianceHistoryMetadataKey.COV_REF_FRAME.name(), metadata.getCovReferenceFrame().getName(), null, false);
        }

        // elements
        generator.writeEntry(AttitudeCovarianceHistoryMetadataKey.COV_TYPE.name(), metadata.getCovType(), false);

        // data
        final List<Unit> units = metadata.getCovType().getUnits();
        for (final AttitudeCovariance covariance : history.getCovariances()) {
            final DiagonalMatrix matrix = covariance.getMatrix();
            final StringBuilder  line   = new StringBuilder();
            line.append(generator.dateToString(timeConverter, covariance.getDate()));
            for (int k = 0; k < units.size(); ++k) {
                line.append(' ');
                line.append(AccurateFormatter.format(units.get(k).fromSI(matrix.getEntry(k, k))));
            }
            if (generator.getFormat() == FileFormat.XML) {
                generator.writeEntry(Acm.COV_LINE, line.toString(), null, true);
            } else {
                generator.writeRawData(line);
                generator.newLine();
            }

        }

    }

}
