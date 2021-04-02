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

import org.hipparchus.linear.RealMatrix;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Writer for covariance history data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class CovarianceHistoryWriter extends AbstractWriter {

    /** Covariance history block. */
    private final CovarianceHistory history;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param covarianceHistory covariance history to write
     * @param timeConverter converter for dates
     */
    CovarianceHistoryWriter(final CovarianceHistory covarianceHistory,
                            final TimeConverter timeConverter) {
        super(OcmDataSubStructureKey.covar.name(), OcmDataSubStructureKey.COV.name());
        this.history       = covarianceHistory;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // covariance history block
        final CovarianceHistoryMetadata metadata = history.getMetadata();
        generator.writeComments(metadata.getComments());

        // identifiers
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_ID.name(),       metadata.getCovID(),      false);
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_PREV_ID.name(),  metadata.getCovPrevID(),  false);
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_NEXT_ID.name(),  metadata.getCovNextID(),  false);
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_BASIS.name(),    metadata.getCovBasis(),   false);
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_BASIS_ID.name(), metadata.getCovBasisID(), false);

        // references
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_REF_FRAME.name(),   metadata.getCovReferenceFrame().getName(),  false);
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_FRAME_EPOCH.name(), timeConverter, metadata.getCovFrameEpoch(), false);

        // scaling
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_SCALE_MIN.name(),  Unit.ONE.fromSI(metadata.getCovScaleMin()),       false);
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_SCALE_MAX.name(),  Unit.ONE.fromSI(metadata.getCovScaleMax()),       false);
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_CONFIDENCE.name(), Unit.PERCENT.fromSI(metadata.getCovConfidence()), false);

        // elements
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_TYPE.name(),  metadata.getCovType(),                         false);
        generator.writeEntry(CovarianceHistoryMetadataKey.COV_UNITS.name(), Units.outputBracketed(metadata.getCovUnits()), false);

        // data
        final List<Unit> units = metadata.getCovType().getUnits();
        for (final Covariance covariance : history.getCovariances()) {
            final RealMatrix    matrix = covariance.getMatrix();
            final StringBuilder line   = new StringBuilder();
            line.append(generator.dateToString(timeConverter, covariance.getDate()));
            for (int i = 0; i < units.size(); ++i) {
                for (int j = 0; j <= i; ++j) {
                    line.append(' ');
                    line.append(AccurateFormatter.format(units.get(i).fromSI(units.get(j).fromSI(matrix.getEntry(i, j)))));
                }
                generator.writeRawData(line);
                generator.newLine();
            }

        }

    }

}
