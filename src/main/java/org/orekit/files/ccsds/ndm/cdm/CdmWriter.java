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
package org.orekit.files.ccsds.ndm.cdm;

import java.io.IOException;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.units.Unit;

/**
 * Writer for CCSDS Conjunction Data Message.
 *
 * @author Melina Vanel
 * @since 11.2
 */
public class CdmWriter extends CdmMessageWriter {

    /** Version number implemented. **/
    public static final double CCSDS_CDM_VERS = 1.0;

    /** Padding width for aligning the '=' sign. */
    public static final int KVN_PADDING_WIDTH = 17;

    /** */
    private final Vector3D NANVECTOR = new Vector3D(Double.NaN, Double.NaN, Double.NaN);

    /** Complete constructor.
     * <p>
     * Calling this constructor directly is not recommended. Users should rather use
     * {@link org.orekit.files.ccsds.ndm.WriterBuilder#buildCdmWriter()
     * writerBuilder.buildCdmWriter()}.
     * </p>
     * @param conventions IERS Conventions
     * @param dataContext used to retrieve frames, time scales, etc.
     */
    public CdmWriter(final IERSConventions conventions, final DataContext dataContext) {
        super(Cdm.ROOT, Cdm.FORMAT_VERSION_KEY, CCSDS_CDM_VERS,
              new ContextBinding(
                  () -> conventions,
                  () -> false, () -> dataContext, () -> ParsedUnitsBehavior.STRICT_COMPLIANCE,
                  () -> null, () -> TimeSystem.UTC,
                  () -> 0.0, () -> 1.0));
    }

    /** {@inheritDoc} */
    @Override
    public void writeSegmentContent(final Generator generator, final double formatVersion,
                                    final Segment<CdmMetadata, CdmData> segment)
        throws IOException {

        // write the metadata
        final ContextBinding oldContext = getContext();
        final CdmMetadata    metadata   = segment.getMetadata();
        setContext(new ContextBinding(oldContext::getConventions,
                                      oldContext::isSimpleEOP,
                                      oldContext::getDataContext,
                                      oldContext::getParsedUnitsBehavior,
                                      oldContext::getReferenceDate,
                                      metadata::getTimeSystem,
                                      oldContext::getClockCount,
                                      oldContext::getClockRate));
        new CdmMetadataWriter(metadata).write(generator);

        // start data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(XmlStructureKey.data.name());
        }

        if (segment.getData().getODParametersBlock() != null) {
            // write optional OD parameters block
            generator.writeComments(segment.getData().getComments());
            new ODParametersWriter(XmlSubStructureKey.odParameters.name(), null,
                                   segment.getData().getODParametersBlock(), getTimeConverter()).
            write(generator);
        }

        if (segment.getData().getAdditionalParametersBlock() != null) {
            // write optional additional parameters block
            new AdditionalParametersWriter(XmlSubStructureKey.additionalParameters.name(), null,
                            segment.getData().getAdditionalParametersBlock()).
            write(generator);
        }

        // write mandatory state vector block
        new StateVectorWriter(XmlSubStructureKey.stateVector.name(), null,
                                 segment.getData().getStateVectorBlock()).
        write(generator);

        // write mandatory RTN covariance block
        new RTNCovarianceWriter(XmlSubStructureKey.covarianceMatrix.name(), null,
                                 segment.getData().getRTNCovarianceBlock()).
        write(generator);

        // stop data block
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }

    }

    @Override
    public void writeRelativeMetadataContent(final Generator generator, final double formatVersion,
                                             final CdmRelativeMetadata relativeMetadata) throws IOException {

        // Is written only once
        // start relative metadata block
        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(XmlSubStructureKey.relativeMetadataData.name());
        }
        generator.writeComments(relativeMetadata.getComment());

        generator.writeEntry(CdmRelativeMetadataKey.TCA.name(), getTimeConverter(), relativeMetadata.getTca(), true, true);
        generator.writeEntry(CdmRelativeMetadataKey.MISS_DISTANCE.name(), relativeMetadata.getMissDistance(), Unit.METRE, true);
        generator.writeEntry(CdmRelativeMetadataKey.RELATIVE_SPEED.name(), relativeMetadata.getRelativeSpeed(), Units.M_PER_S, false);

        // start relative state vector block
        if (generator.getFormat() == FileFormat.XML && (relativeMetadata.getRelativePosition() != NANVECTOR ||
            relativeMetadata.getRelativeVelocity() != NANVECTOR)) {
            generator.enterSection(XmlSubStructureKey.relativeStateVector.name());
        }

        if (relativeMetadata.getRelativePosition() != NANVECTOR) {
            generator.writeEntry(CdmRelativeMetadataKey.RELATIVE_POSITION_R.name(),
                                 relativeMetadata.getRelativePosition().getX(), Unit.METRE, false);
            generator.writeEntry(CdmRelativeMetadataKey.RELATIVE_POSITION_T.name(),
                                 relativeMetadata.getRelativePosition().getY(), Unit.METRE, false);
            generator.writeEntry(CdmRelativeMetadataKey.RELATIVE_POSITION_N.name(),
                                 relativeMetadata.getRelativePosition().getZ(), Unit.METRE, false);
        }
        if (relativeMetadata.getRelativeVelocity() != NANVECTOR) {
            generator.writeEntry(CdmRelativeMetadataKey.RELATIVE_VELOCITY_R.name(),
                                 relativeMetadata.getRelativeVelocity().getX(), Units.M_PER_S, false);
            generator.writeEntry(CdmRelativeMetadataKey.RELATIVE_VELOCITY_T.name(),
                                 relativeMetadata.getRelativeVelocity().getY(), Units.M_PER_S, false);
            generator.writeEntry(CdmRelativeMetadataKey.RELATIVE_VELOCITY_N.name(),
                                 relativeMetadata.getRelativeVelocity().getZ(), Units.M_PER_S, false);
        }

        // stop relative state vector block
        if (generator.getFormat() == FileFormat.XML &&
            (relativeMetadata.getRelativePosition() != NANVECTOR || relativeMetadata.getRelativeVelocity() != NANVECTOR)) {
            generator.exitSection();
        }

        generator.writeEntry(CdmRelativeMetadataKey.START_SCREEN_PERIOD.name(), getTimeConverter(),
                             relativeMetadata.getStartScreenPeriod(), true, false);
        generator.writeEntry(CdmRelativeMetadataKey.STOP_SCREEN_PERIOD.name(),  getTimeConverter(),
                             relativeMetadata.getStopScreenPeriod(), true, false);
        generator.writeEntry(CdmRelativeMetadataKey.SCREEN_VOLUME_FRAME.name(), relativeMetadata.getScreenVolumeFrame(),         false);
        generator.writeEntry(CdmRelativeMetadataKey.SCREEN_VOLUME_SHAPE.name(), relativeMetadata.getScreenVolumeShape(),         false);
        generator.writeEntry(CdmRelativeMetadataKey.SCREEN_VOLUME_X.name(),     relativeMetadata.getScreenVolumeX(), Unit.METRE, false);
        generator.writeEntry(CdmRelativeMetadataKey.SCREEN_VOLUME_Y.name(),     relativeMetadata.getScreenVolumeY(), Unit.METRE, false);
        generator.writeEntry(CdmRelativeMetadataKey.SCREEN_VOLUME_Z.name(),     relativeMetadata.getScreenVolumeZ(), Unit.METRE, false);
        generator.writeEntry(CdmRelativeMetadataKey.SCREEN_ENTRY_TIME.name(),   getTimeConverter(),
                             relativeMetadata.getScreenEntryTime(), true, false);
        generator.writeEntry(CdmRelativeMetadataKey.SCREEN_EXIT_TIME.name(),    getTimeConverter(),
                             relativeMetadata.getScreenExitTime(), true, false);
        generator.writeEntry(CdmRelativeMetadataKey.COLLISION_PROBABILITY.name(), relativeMetadata.getCollisionProbability(), Unit.ONE, false);
        if (relativeMetadata.getCollisionProbaMethod() != null)  {
            generator.writeEntry(CdmRelativeMetadataKey.COLLISION_PROBABILITY_METHOD.name(),
                                 relativeMetadata.getCollisionProbaMethod().getName(), null, false);
        }

        // stop relative metadata block
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }

    }

}

