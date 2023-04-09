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

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.ccsds.ndm.odm.UserDefinedWriter;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.AbstractMessageWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.IERSConventions;


/**
 * Writer for CCSDS Attitude Comprehensive Message.
 *
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AcmWriter extends AbstractMessageWriter<AdmHeader, Segment<AcmMetadata, AcmData>, Acm> {

    /** Version number implemented. **/
    public static final double CCSDS_ACM_VERS = 2.0;

    /** Padding width for aligning the '=' sign. */
    public static final int KVN_PADDING_WIDTH = 33;

    /** Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.WriterBuilder#buildAcmWriter()
     * writerBuilder.buildAcmWriter()}.
     * </p>
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     */
    public AcmWriter(final IERSConventions conventions, final DataContext dataContext) {
        super(Acm.ROOT, Acm.FORMAT_VERSION_KEY, CCSDS_ACM_VERS,
              new ContextBinding(
                  () -> conventions, () -> false, () -> dataContext,
                  () -> ParsedUnitsBehavior.STRICT_COMPLIANCE,
                  () -> null, () -> TimeSystem.UTC,
                  () -> 0.0, () -> 1.0));
    }

    /** {@inheritDoc} */
    @Override
    protected void writeSegmentContent(final Generator generator, final double formatVersion,
                                       final Segment<AcmMetadata, AcmData> segment)
        throws IOException {

        // write the metadata
        final ContextBinding oldContext = getContext();
        final AcmMetadata    metadata   = segment.getMetadata();
        setContext(new ContextBinding(oldContext::getConventions,
                                      oldContext::isSimpleEOP,
                                      oldContext::getDataContext,
                                      oldContext::getParsedUnitsBehavior,
                                      metadata::getEpochT0,
                                      metadata::getTimeSystem,
                                      () -> 0.0, () -> 1.0));
        new AcmMetadataWriter(metadata, getTimeConverter()).write(generator);

        // start data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(XmlStructureKey.data.name());
        }

        // attitude history
        if (segment.getData().getAttitudeBlocks() != null && !segment.getData().getAttitudeBlocks().isEmpty()) {
            for (final AttitudeStateHistory history : segment.getData().getAttitudeBlocks()) {
                // write optional attitude history block
                new AttitudeStateHistoryWriter(history, getTimeConverter()).write(generator);
            }
        }

        if (segment.getData().getPhysicBlock() != null) {
            // write optional physical properties block
            new AttitudePhysicalPropertiesWriter(segment.getData().getPhysicBlock()).
            write(generator);
        }

        // covariance history
        if (segment.getData().getCovarianceBlocks() != null && !segment.getData().getCovarianceBlocks().isEmpty()) {
            for (final AttitudeCovarianceHistory history : segment.getData().getCovarianceBlocks()) {
                // write optional covariance history block
                new AttitudeCovarianceHistoryWriter(history, getTimeConverter()).write(generator);
            }
        }

        if (segment.getData().getManeuverBlocks() != null && !segment.getData().getManeuverBlocks().isEmpty()) {
            for (final AttitudeManeuver maneuver : segment.getData().getManeuverBlocks()) {
                // write optional maneuver block
                new AttitudeManeuverWriter(maneuver).write(generator);
            }
        }

        if (segment.getData().getAttitudeDeterminationBlock() != null) {
            // write optional attitude determination block
            new AttitudeDeterminationWriter(segment.getData().getAttitudeDeterminationBlock()).
            write(generator);
        }

        if (segment.getData().getUserDefinedBlock() != null) {
            // write optional user defined parameters block
            new UserDefinedWriter(AcmDataSubStructureKey.user.name(),
                                  AcmDataSubStructureKey.USER.name(),
                                  segment.getData().getUserDefinedBlock()).
            write(generator);
        }

        // stop data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }

    }

}
