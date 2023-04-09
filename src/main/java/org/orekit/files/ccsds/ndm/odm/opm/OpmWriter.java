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
package org.orekit.files.ccsds.ndm.odm.opm;

import java.io.IOException;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.odm.CartesianCovarianceWriter;
import org.orekit.files.ccsds.ndm.odm.OdmCommonMetadata;
import org.orekit.files.ccsds.ndm.odm.CommonMetadataWriter;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.ndm.odm.SpacecraftParametersWriter;
import org.orekit.files.ccsds.ndm.odm.StateVectorWriter;
import org.orekit.files.ccsds.ndm.odm.UserDefinedWriter;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.AbstractMessageWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;


/**
 * Writer for CCSDS Orbit Parameter Message.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OpmWriter extends AbstractMessageWriter<OdmHeader, Segment<OdmCommonMetadata, OpmData>, Opm> {

    /** Version number implemented. **/
    public static final double CCSDS_OPM_VERS = 3.0;

    /** Padding width for aligning the '=' sign. */
    public static final int KVN_PADDING_WIDTH = 18;

    /** Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.WriterBuilder#buildOpmWriter()
     * writerBuilder.buildOpmWriter()}.
     * </p>
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     */
    public OpmWriter(final IERSConventions conventions, final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate) {
        super(Opm.ROOT, Opm.FORMAT_VERSION_KEY, CCSDS_OPM_VERS,
              new ContextBinding(
                  () -> conventions, () -> false, () -> dataContext,
                  () -> ParsedUnitsBehavior.STRICT_COMPLIANCE,
                  () -> missionReferenceDate, () -> TimeSystem.UTC,
                  () -> 0.0, () -> 1.0));
    }

    /** {@inheritDoc} */
    @Override
    protected void writeSegmentContent(final Generator generator, final double formatVersion,
                                       final Segment<OdmCommonMetadata, OpmData> segment)
        throws IOException {

        // write the metadata
        final ContextBinding oldContext = getContext();
        final OdmCommonMetadata metadata   = segment.getMetadata();
        setContext(new ContextBinding(oldContext::getConventions,
                                      oldContext::isSimpleEOP,
                                      oldContext::getDataContext,
                                      oldContext::getParsedUnitsBehavior,
                                      oldContext::getReferenceDate,
                                      metadata::getTimeSystem,
                                      oldContext::getClockCount,
                                      oldContext::getClockRate));
        new CommonMetadataWriter(metadata, getTimeConverter()).write(generator);

        // start data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(XmlStructureKey.data.name());
        }

        // write mandatory state vector block
        new StateVectorWriter(XmlSubStructureKey.stateVector.name(), null,
                              segment.getData().getStateVectorBlock(), getTimeConverter()).
        write(generator);

        if (segment.getData().getKeplerianElementsBlock() != null) {
            // write optional Keplerian elements block
            new OsculationgKeplerianElementsWriter(XmlSubStructureKey.keplerianElements.name(), null,
                                                   segment.getData().getKeplerianElementsBlock()).
            write(generator);
        }

        if (segment.getData().getSpacecraftParametersBlock() != null) {
            // write optional spacecraft parameters block
            new SpacecraftParametersWriter(XmlSubStructureKey.spacecraftParameters.name(), null,
                                           segment.getData().getSpacecraftParametersBlock()).
            write(generator);
        }

        if (segment.getData().getCovarianceBlock() != null) {
            // write optional spacecraft parameters block
            new CartesianCovarianceWriter(XmlSubStructureKey.covarianceMatrix.name(), null,
                                          segment.getData().getCovarianceBlock()).
            write(generator);
        }

        if (!segment.getData().getManeuvers().isEmpty()) {
            for (final Maneuver maneuver : segment.getData().getManeuvers()) {
                // write optional maneuver block
                new ManeuverWriter(maneuver, getTimeConverter()).write(generator);
            }
        }

        if (segment.getData().getUserDefinedBlock() != null) {
            // write optional user defined parameters block
            new UserDefinedWriter(XmlSubStructureKey.userDefinedParameters.name(), null,
                                  segment.getData().getUserDefinedBlock()).
            write(generator);
        }

        // stop data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }

    }

}
