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

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.ndm.odm.UserDefinedWriter;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.AbstractMessageWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.IERSConventions;


/**
 * Writer for CCSDS Orbit Comprehensive Message.
 *
 * @see EphemerisOcmWriter
 * @see StreamingOcmWriter
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OcmWriter extends AbstractMessageWriter<OdmHeader, Segment<OcmMetadata, OcmData>, Ocm> {

    /** Version number implemented. **/
    public static final double CCSDS_OCM_VERS = 3.0;

    /** Padding width for aligning the '=' sign. */
    public static final int KVN_PADDING_WIDTH = 24;

    /** Central body equatorial radius.
     * @since 12.0
     */
    private final double equatorialRadius;

    /** Central body flattening.
     * @since 12.0
     */
    private final double flattening;

    /** Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.WriterBuilder#buildOcmWriter()
     * writerBuilder.buildOcmWriter()}.
     * </p>
     * @param conventions IERS Conventions
     * @param equatorialRadius central body equatorial radius
     * @param flattening central body flattening
     * @param dataContext used to retrieve frames, time scales, etc.
     */
    public OcmWriter(final IERSConventions conventions,
                     final double equatorialRadius, final double flattening,
                     final DataContext dataContext) {
        super(Ocm.ROOT, Ocm.FORMAT_VERSION_KEY, CCSDS_OCM_VERS,
              new ContextBinding(
                  () -> conventions, () -> false, () -> dataContext,
                  () -> ParsedUnitsBehavior.STRICT_COMPLIANCE,
                  () -> null, () -> TimeSystem.UTC,
                  () -> 0.0, () -> 1.0));
        this.equatorialRadius = equatorialRadius;
        this.flattening       = flattening;
    }

    /** Get the central body equatorial radius.
     * @return central body equatorial radius
     * @since 12.0
     */
    public double getEquatorialRadius() {
        return equatorialRadius;
    }

    /** Get the central body flattening.
     * @return central body flattening
     * @since 12.0
     */
    public double getFlattening() {
        return flattening;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeSegmentContent(final Generator generator, final double formatVersion,
                                       final Segment<OcmMetadata, OcmData> segment)
        throws IOException {

        // write the metadata
        final ContextBinding oldContext = getContext();
        final OcmMetadata    metadata   = segment.getMetadata();
        setContext(new ContextBinding(oldContext::getConventions,
                                      oldContext::isSimpleEOP,
                                      oldContext::getDataContext,
                                      oldContext::getParsedUnitsBehavior,
                                      metadata::getEpochT0,
                                      metadata::getTimeSystem,
                                      metadata::getSclkOffsetAtEpoch,
                                      metadata::getSclkSecPerSISec));
        new OcmMetadataWriter(metadata, getTimeConverter()).write(generator);

        // start data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(XmlStructureKey.data.name());
        }

        // trajectory history
        if (segment.getData().getTrajectoryBlocks() != null && !segment.getData().getTrajectoryBlocks().isEmpty()) {
            for (final TrajectoryStateHistory history : segment.getData().getTrajectoryBlocks()) {
                // write optional trajectory history block
                new TrajectoryStateHistoryWriter(history, getTimeConverter()).write(generator);
            }
        }

        if (segment.getData().getPhysicBlock() != null) {
            // write optional physical properties block
            new OrbitPhysicalPropertiesWriter(segment.getData().getPhysicBlock(),
                                         getTimeConverter()).
            write(generator);
        }

        // covariance history
        if (segment.getData().getCovarianceBlocks() != null && !segment.getData().getCovarianceBlocks().isEmpty()) {
            for (final OrbitCovarianceHistory history : segment.getData().getCovarianceBlocks()) {
                // write optional covariance history block
                new OrbitCovarianceHistoryWriter(history, getTimeConverter()).write(generator);
            }
        }

        if (segment.getData().getManeuverBlocks() != null && !segment.getData().getManeuverBlocks().isEmpty()) {
            for (final OrbitManeuverHistory maneuver : segment.getData().getManeuverBlocks()) {
                // write optional maneuver block
                new OrbitManeuverHistoryWriter(maneuver, getTimeConverter()).write(generator);
            }
        }

        if (segment.getData().getPerturbationsBlock() != null) {
            // write optional perturbation parameters block
            new PerturbationsWriter(segment.getData().getPerturbationsBlock(),
                                    getTimeConverter()).
            write(generator);
        }

        if (segment.getData().getOrbitDeterminationBlock() != null) {
            // write optional orbit determination block
            new OrbitDeterminationWriter(segment.getData().getOrbitDeterminationBlock(),
                                         getTimeConverter()).
            write(generator);
        }

        if (segment.getData().getUserDefinedBlock() != null) {
            // write optional user defined parameters block
            new UserDefinedWriter(OcmDataSubStructureKey.user.name(),
                                  OcmDataSubStructureKey.USER.name(),
                                  segment.getData().getUserDefinedBlock()).
            write(generator);
        }

        // stop data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }

    }

}
