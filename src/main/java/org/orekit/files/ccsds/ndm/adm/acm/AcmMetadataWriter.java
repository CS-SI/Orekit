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
import java.util.stream.Collectors;

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.ndm.adm.AdmMetadataKey;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.section.KvnStructureKey;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/**
 * Writer for Common metadata for CCSDS Attitude Comprehensive Messages.
 *
 * @author Luc Maisonobe
 * @since 12.0
 */
class AcmMetadataWriter extends AbstractWriter {

    /** Metadata. */
    private final AcmMetadata metadata;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Simple constructor.
     * @param metadata metadata to write
     * @param timeConverter converter for dates
     */
    AcmMetadataWriter(final AcmMetadata metadata, final TimeConverter timeConverter) {
        super(XmlStructureKey.metadata.name(), KvnStructureKey.META.name());
        this.metadata      = metadata;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(metadata.getComments());

        // object
        generator.writeEntry(AdmMetadataKey.OBJECT_NAME.name(),
                             metadata.getObjectName(), null, false);
        generator.writeEntry(AcmMetadataKey.INTERNATIONAL_DESIGNATOR.name(),
                             metadata.getInternationalDesignator(), null, false);
        generator.writeEntry(AcmMetadataKey.CATALOG_NAME.name(),
                             metadata.getCatalogName(), null, false);
        generator.writeEntry(AcmMetadataKey.OBJECT_DESIGNATOR.name(),
                             metadata.getObjectDesignator(), null, false);

        // originator
        generator.writeEntry(AcmMetadataKey.ORIGINATOR_POC.name(),
                             metadata.getOriginatorPOC(), null, false);
        generator.writeEntry(AcmMetadataKey.ORIGINATOR_POSITION.name(),
                             metadata.getOriginatorPosition(), null, false);
        generator.writeEntry(AcmMetadataKey.ORIGINATOR_PHONE.name(),
                             metadata.getOriginatorPhone(), null, false);
        generator.writeEntry(AcmMetadataKey.ORIGINATOR_EMAIL.name(),
                             metadata.getOriginatorEmail(), null, false);
        generator.writeEntry(AcmMetadataKey.ORIGINATOR_ADDRESS.name(),
                             metadata.getOriginatorAddress(), null, false);

        // messages
        generator.writeEntry(AcmMetadataKey.ODM_MSG_LINK.name(),
                             metadata.getOdmMessageLink(), null, false);

        // time
        if (metadata.getCenter() != null) {
            generator.writeEntry(AdmMetadataKey.CENTER_NAME.name(), metadata.getCenter().getName(), null, false);
        }
        generator.writeEntry(MetadataKey.TIME_SYSTEM.name(),
                             metadata.getTimeSystem(), false);
        generator.writeEntry(AcmMetadataKey.EPOCH_TZERO.name(), timeConverter,
                             metadata.getEpochT0(), true, true);

        // definitions
        if (metadata.getAcmDataElements() != null) {
            generator.writeEntry(AcmMetadataKey.ACM_DATA_ELEMENTS.name(),
                                 metadata.getAcmDataElements().stream().map(e -> e.name()).collect(Collectors.toList()), false);
        }

        // other times
        generator.writeEntry(AcmMetadataKey.START_TIME.name(), timeConverter,
                             metadata.getStartTime(), false, false);
        generator.writeEntry(AcmMetadataKey.STOP_TIME.name(), timeConverter,
                             metadata.getStopTime(), false, false);
        generator.writeEntry(AcmMetadataKey.TAIMUTC_AT_TZERO.name(), metadata.getTaimutcT0(), Unit.SECOND, false);
        if (metadata.getNextLeapEpoch() != null) {
            generator.writeEntry(AcmMetadataKey.NEXT_LEAP_EPOCH.name(), timeConverter,
                                 metadata.getNextLeapEpoch(), true, true);
            generator.writeEntry(AcmMetadataKey.NEXT_LEAP_TAIMUTC.name(), metadata.getNextLeapTaimutc(), Unit.SECOND, true);
        }

    }

}
