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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.io.IOException;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
import org.orekit.files.ccsds.ndm.adm.AdmMetadataWriter;
import org.orekit.files.ccsds.section.Header;
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
public class ApmWriter extends AbstractMessageWriter<Header, Segment<AdmMetadata, ApmData>, Apm> {

    /** Version number implemented. **/
    public static final double CCSDS_APM_VERS = 1.0;

    /** Padding width for aligning the '=' sign. */
    public static final int KVN_PADDING_WIDTH = 17;

    /** Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.WriterBuilder#buildApmWriter()
     * writerBuilder.buildOpmWriter()}.
     * </p>
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     */
    public ApmWriter(final IERSConventions conventions, final DataContext dataContext,
                     final AbsoluteDate missionReferenceDate) {
        super(Apm.ROOT, Apm.FORMAT_VERSION_KEY, CCSDS_APM_VERS,
              new ContextBinding(
                  () -> conventions,
                  () -> false, () -> dataContext, () -> ParsedUnitsBehavior.STRICT_COMPLIANCE,
                  () -> missionReferenceDate, () -> TimeSystem.UTC,
                  () -> 0.0, () -> 1.0));
    }

    /** {@inheritDoc} */
    @Override
    public void writeSegmentContent(final Generator generator, final double formatVersion,
                                    final Segment<AdmMetadata, ApmData> segment)
        throws IOException {

        // write the metadata
        final ContextBinding oldContext = getContext();
        final AdmMetadata    metadata   = segment.getMetadata();
        setContext(new ContextBinding(oldContext::getConventions,
                                      oldContext::isSimpleEOP,
                                      oldContext::getDataContext,
                                      oldContext::getParsedUnitsBehavior,
                                      oldContext::getReferenceDate,
                                      metadata::getTimeSystem,
                                      oldContext::getClockCount,
                                      oldContext::getClockRate));
        new AdmMetadataWriter(metadata).write(generator);

        // start data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(XmlStructureKey.data.name());
        }

        generator.writeComments(segment.getData().getComments());

        // write mandatory quaternion block
        new ApmQuaternionWriter(XmlSubStructureKey.quaternionState.name(), null,
                                segment.getData().getQuaternionBlock(), getTimeConverter()).
        write(generator);

        if (segment.getData().getEulerBlock() != null) {
            // write optional Euler block for three axis stabilized satellites
            new EulerWriter(XmlSubStructureKey.eulerElementsThree.name(), null,
                            segment.getData().getEulerBlock()).
            write(generator);
        }

        if (segment.getData().getSpinStabilizedBlock() != null) {
            // write optional Euler block for spin stabilized satellites
            new SpinStabilizedWriter(XmlSubStructureKey.eulerElementsSpin.name(), null,
                                     segment.getData().getSpinStabilizedBlock()).
            write(generator);
        }

        if (segment.getData().getSpacecraftParametersBlock() != null) {
            // write optional spacecraft parameters block
            new SpacecraftParametersWriter(XmlSubStructureKey.spacecraftParameters.name(), null,
                                           segment.getData().getSpacecraftParametersBlock()).
            write(generator);
        }

        if (!segment.getData().getManeuvers().isEmpty()) {
            for (final Maneuver maneuver : segment.getData().getManeuvers()) {
                // write optional maneuver block
                new ManeuverWriter(XmlSubStructureKey.maneuverParameters.name(), null,
                                   maneuver, getTimeConverter()).write(generator);
            }
        }

        // stop data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }

    }

}
