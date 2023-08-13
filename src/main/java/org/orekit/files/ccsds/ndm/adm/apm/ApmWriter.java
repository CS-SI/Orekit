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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.io.IOException;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.adm.AdmCommonMetadataWriter;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
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
public class ApmWriter extends AbstractMessageWriter<AdmHeader, Segment<AdmMetadata, ApmData>, Apm> {

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
    protected void writeSegmentContent(final Generator generator, final double formatVersion,
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
        new AdmCommonMetadataWriter(metadata).write(generator);

        // start data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(XmlStructureKey.data.name());
        }

        generator.writeComments(segment.getData().getComments());
        if (formatVersion >= 2.0) {
            // starting with version 2, epoch is outside of other blocks
            generator.writeEntry("EPOCH", getTimeConverter(), segment.getData().getEpoch(), false, true);
        }

        if (segment.getData().getQuaternionBlock() != null) {
            // write quaternion block
            final String xmlTag = ApmDataSubStructureKey.quaternionState.name();
            final String kvnTag = formatVersion < 2.0 ? null : ApmDataSubStructureKey.QUAT.name();
            new ApmQuaternionWriter(formatVersion, xmlTag, kvnTag,
                                    segment.getData().getQuaternionBlock(),
                                    formatVersion >= 2.0 ? null : segment.getData().getEpoch(),
                                                         getTimeConverter()).
            write(generator);
        }

        if (segment.getData().getEulerBlock() != null) {
            // write optional Euler block for three axis stabilized satellites
            final String xmlTag = formatVersion < 2.0 ?
                                  ApmDataSubStructureKey.eulerElementsThree.name() :
                                  ApmDataSubStructureKey.eulerAngleState.name();
            final String kvnTag = formatVersion < 2.0 ? null : ApmDataSubStructureKey.EULER.name();
            new EulerWriter(formatVersion, xmlTag, kvnTag,
                            segment.getData().getEulerBlock()).
            write(generator);
        }

        if (segment.getData().getAngularVelocityBlock() != null) {
            // write optional angular velocity block
            final String xmlTag = ApmDataSubStructureKey.angularVelocity.name();
            final String kvnTag = ApmDataSubStructureKey.ANGVEL.name();
            new AngularVelocityWriter(xmlTag, kvnTag,
                                      segment.getData().getAngularVelocityBlock()).
            write(generator);
        }

        if (segment.getData().getSpinStabilizedBlock() != null) {
            // write optional block for spin stabilized satellites
            final String xmlTag;
            final String kvnTag;
            if (formatVersion < 2.0) {
                xmlTag = ApmDataSubStructureKey.eulerElementsSpin.name();
                kvnTag = null;
            } else {
                xmlTag = ApmDataSubStructureKey.spin.name();
                kvnTag = ApmDataSubStructureKey.SPIN.name();
            }
            new SpinStabilizedWriter(formatVersion, xmlTag, kvnTag,
                                     segment.getData().getSpinStabilizedBlock()).
            write(generator);
        }

        if (segment.getData().getInertiaBlock() != null) {
            // write optional spacecraft parameters block
            final String xmlTag = formatVersion < 2.0 ?
                                  ApmDataSubStructureKey.spacecraftParameters.name() :
                                  ApmDataSubStructureKey.inertia.name();
            final String kvnTag = formatVersion < 2.0 ? null : ApmDataSubStructureKey.INERTIA.name();
            new InertiaWriter(formatVersion, xmlTag, kvnTag,
                              segment.getData().getInertiaBlock()).
            write(generator);
        }

        if (!segment.getData().getManeuvers().isEmpty()) {
            for (final Maneuver maneuver : segment.getData().getManeuvers()) {
                // write optional maneuver block
                final String xmlTag = ApmDataSubStructureKey.maneuverParameters.name();
                final String kvnTag = formatVersion < 2.0 ? null : ApmDataSubStructureKey.MAN.name();
                new ManeuverWriter(formatVersion, xmlTag, kvnTag,
                                   maneuver, getTimeConverter()).write(generator);
            }
        }

        // stop data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }

    }

}
