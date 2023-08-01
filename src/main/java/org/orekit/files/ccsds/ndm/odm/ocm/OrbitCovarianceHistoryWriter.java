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

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Writer for covariance history data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class OrbitCovarianceHistoryWriter extends AbstractWriter {

    /** Covariance history block. */
    private final OrbitCovarianceHistory history;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param covarianceHistory covariance history to write
     * @param timeConverter converter for dates
     */
    OrbitCovarianceHistoryWriter(final OrbitCovarianceHistory covarianceHistory,
                            final TimeConverter timeConverter) {
        super(OcmDataSubStructureKey.cov.name(), OcmDataSubStructureKey.COV.name());
        this.history       = covarianceHistory;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // covariance history block
        final OrbitCovarianceHistoryMetadata metadata = history.getMetadata();
        generator.writeComments(metadata.getComments());

        // identifiers
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_ID.name(),       metadata.getCovID(),      null, false);
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_PREV_ID.name(),  metadata.getCovPrevID(),  null, false);
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_NEXT_ID.name(),  metadata.getCovNextID(),  null, false);
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_BASIS.name(),    metadata.getCovBasis(),   null, false);
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_BASIS_ID.name(), metadata.getCovBasisID(), null, false);

        // references
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_REF_FRAME.name(),   metadata.getCovReferenceFrame().getName(),  null, false);
        if (!metadata.getCovFrameEpoch().equals(timeConverter.getReferenceDate()) &&
            metadata.getCovReferenceFrame().asOrbitRelativeFrame() == null &&
            metadata.getCovReferenceFrame().asSpacecraftBodyFrame() == null) {
            generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_FRAME_EPOCH.name(), timeConverter, metadata.getCovFrameEpoch(), true, false);
        }

        // scaling
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_SCALE_MIN.name(),  metadata.getCovScaleMin(), Unit.ONE,       false);
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_SCALE_MAX.name(),  metadata.getCovScaleMax(), Unit.ONE,       false);
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_CONFIDENCE.name(), metadata.getCovConfidence(), Unit.PERCENT, false);

        // elements
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_TYPE.name(),     metadata.getCovType(),                                     false);
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_ORDERING.name(), metadata.getCovOrdering(),                                 false);
        generator.writeEntry(OrbitCovarianceHistoryMetadataKey.COV_UNITS.name(),    generator.unitsListToString(metadata.getCovUnits()), null, false);

        // data
        final List<Unit> units = metadata.getCovType().getUnits();
        for (final OrbitCovariance covariance : history.getCovariances()) {
            final RealMatrix        matrix   = covariance.getMatrix();
            final Ordering          ordering = metadata.getCovOrdering();
            final CovarianceIndexer indexer  = new CovarianceIndexer(units.size());
            final StringBuilder     line     = new StringBuilder();
            line.append(generator.dateToString(timeConverter, covariance.getDate()));
            for (int k = 0; k < ordering.nbElements(units.size()); ++k) {
                final int i = indexer.getRow();
                final int j = indexer.getColumn();
                final double cij;
                if (indexer.isCrossCorrelation()) {
                    // we need to compute the cross-correlation
                    cij = matrix.getEntry(i, j) /
                                    FastMath.sqrt(matrix.getEntry(i, i) * matrix.getEntry(j, j));
                } else {
                    // we need to get the covariance
                    cij = units.get(i).fromSI(units.get(j).fromSI(matrix.getEntry(i, j)));
                }
                line.append(' ');
                line.append(AccurateFormatter.format(cij));
                ordering.update(indexer);
            }
            if (generator.getFormat() == FileFormat.XML) {
                generator.writeEntry(Ocm.COV_LINE, line.toString(), null, true);
            } else {
                generator.writeRawData(line);
                generator.newLine();
            }

        }

    }

}
