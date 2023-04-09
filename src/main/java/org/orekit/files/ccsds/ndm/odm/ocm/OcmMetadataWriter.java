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
import java.util.stream.Collectors;

import org.hipparchus.util.Precision;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.ndm.odm.OdmMetadataKey;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.section.KvnStructureKey;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/**
 * Writer for Common metadata for CCSDS Orbit Comprehensive Messages.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
class OcmMetadataWriter extends AbstractWriter {

    /** Metadata. */
    private final OcmMetadata metadata;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Simple constructor.
     * @param metadata metadata to write
     * @param timeConverter converter for dates
     */
    OcmMetadataWriter(final OcmMetadata metadata, final TimeConverter timeConverter) {
        super(XmlStructureKey.metadata.name(), KvnStructureKey.META.name());
        this.metadata      = metadata;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(metadata.getComments());

        // object
        generator.writeEntry(OdmMetadataKey.OBJECT_NAME.name(),
                             metadata.getObjectName(), null, false);
        generator.writeEntry(OcmMetadataKey.INTERNATIONAL_DESIGNATOR.name(),
                             metadata.getInternationalDesignator(), null, false);
        generator.writeEntry(OcmMetadataKey.CATALOG_NAME.name(),
                             metadata.getCatalogName(), null, false);
        generator.writeEntry(OcmMetadataKey.OBJECT_DESIGNATOR.name(),
                             metadata.getObjectDesignator(), null, false);
        generator.writeEntry(OcmMetadataKey.ALTERNATE_NAMES.name(),
                             metadata.getAlternateNames(), false);

        // originator
        generator.writeEntry(OcmMetadataKey.ORIGINATOR_POC.name(),
                             metadata.getOriginatorPOC(), null, false);
        generator.writeEntry(OcmMetadataKey.ORIGINATOR_POSITION.name(),
                             metadata.getOriginatorPosition(), null, false);
        generator.writeEntry(OcmMetadataKey.ORIGINATOR_PHONE.name(),
                             metadata.getOriginatorPhone(), null, false);
        generator.writeEntry(OcmMetadataKey.ORIGINATOR_EMAIL.name(),
                             metadata.getOriginatorEmail(), null, false);
        generator.writeEntry(OcmMetadataKey.ORIGINATOR_ADDRESS.name(),
                             metadata.getOriginatorAddress(), null, false);

        // technical
        generator.writeEntry(OcmMetadataKey.TECH_ORG.name(),
                             metadata.getTechOrg(), null, false);
        generator.writeEntry(OcmMetadataKey.TECH_POC.name(),
                             metadata.getTechPOC(), null, false);
        generator.writeEntry(OcmMetadataKey.TECH_POSITION.name(),
                             metadata.getTechPosition(), null, false);
        generator.writeEntry(OcmMetadataKey.TECH_PHONE.name(),
                             metadata.getTechPhone(), null, false);
        generator.writeEntry(OcmMetadataKey.TECH_EMAIL.name(),
                             metadata.getTechEmail(), null, false);
        generator.writeEntry(OcmMetadataKey.TECH_ADDRESS.name(),
                             metadata.getTechAddress(), null, false);

        // messages
        generator.writeEntry(OcmMetadataKey.PREVIOUS_MESSAGE_ID.name(),
                             metadata.getPreviousMessageID(), null, false);
        generator.writeEntry(OcmMetadataKey.NEXT_MESSAGE_ID.name(),
                             metadata.getNextMessageID(), null, false);
        generator.writeEntry(OcmMetadataKey.ADM_MSG_LINK.name(),
                             metadata.getAdmMessageLink(), null, false);
        generator.writeEntry(OcmMetadataKey.CDM_MSG_LINK.name(),
                             metadata.getCdmMessageLink(), null, false);
        generator.writeEntry(OcmMetadataKey.PRM_MSG_LINK.name(),
                             metadata.getPrmMessageLink(), null, false);
        generator.writeEntry(OcmMetadataKey.RDM_MSG_LINK.name(),
                             metadata.getRdmMessageLink(), null, false);
        generator.writeEntry(OcmMetadataKey.TDM_MSG_LINK.name(),
                             metadata.getTdmMessageLink(), null, false);

        // operator
        generator.writeEntry(OcmMetadataKey.OPERATOR.name(),
                             metadata.getOperator(), null, false);
        generator.writeEntry(OcmMetadataKey.OWNER.name(),
                             metadata.getOwner(), null, false);
        generator.writeEntry(OcmMetadataKey.COUNTRY.name(),
                             metadata.getCountry(), null, false);
        generator.writeEntry(OcmMetadataKey.CONSTELLATION.name(),
                             metadata.getConstellation(), null, false);
        generator.writeEntry(OcmMetadataKey.OBJECT_TYPE.name(),
                             metadata.getObjectType(), false);

        // time
        generator.writeEntry(MetadataKey.TIME_SYSTEM.name(),
                             metadata.getTimeSystem(), false);
        generator.writeEntry(OcmMetadataKey.EPOCH_TZERO.name(), timeConverter,
                             metadata.getEpochT0(), true, true);

        // definitions
        generator.writeEntry(OcmMetadataKey.OPS_STATUS.name(),
                             metadata.getOpsStatus(), false);
        generator.writeEntry(OcmMetadataKey.ORBIT_CATEGORY.name(),
                             metadata.getOrbitCategory(), false);
        if (metadata.getOcmDataElements() != null) {
            generator.writeEntry(OcmMetadataKey.OCM_DATA_ELEMENTS.name(),
                                 metadata.getOcmDataElements().stream().map(e -> e.name()).collect(Collectors.toList()), false);
        }

        // other times
        if (!(Precision.equals(metadata.getSclkOffsetAtEpoch(), OcmMetadata.DEFAULT_SCLK_OFFSET_AT_EPOCH) &&
              Precision.equals(metadata.getSclkSecPerSISec(),   OcmMetadata.DEFAULT_SCLK_SEC_PER_SI_SEC))) {
            generator.writeEntry(OcmMetadataKey.SCLK_OFFSET_AT_EPOCH.name(), metadata.getSclkOffsetAtEpoch(), Unit.SECOND, false);
            generator.writeEntry(OcmMetadataKey.SCLK_SEC_PER_SI_SEC.name(),  metadata.getSclkSecPerSISec(),   Unit.SECOND, false);
        }
        generator.writeEntry(OcmMetadataKey.PREVIOUS_MESSAGE_EPOCH.name(), timeConverter,
                             metadata.getPreviousMessageEpoch(), true, false);
        generator.writeEntry(OcmMetadataKey.NEXT_MESSAGE_EPOCH.name(), timeConverter,
                             metadata.getNextMessageEpoch(), true, false);
        generator.writeEntry(OcmMetadataKey.START_TIME.name(), timeConverter,
                             metadata.getStartTime(), false, false);
        generator.writeEntry(OcmMetadataKey.STOP_TIME.name(), timeConverter,
                             metadata.getStopTime(), false, false);
        generator.writeEntry(OcmMetadataKey.TIME_SPAN.name(),        metadata.getTimeSpan(),  Unit.DAY,    false);
        generator.writeEntry(OcmMetadataKey.TAIMUTC_AT_TZERO.name(), metadata.getTaimutcT0(), Unit.SECOND, false);
        if (metadata.getNextLeapEpoch() != null) {
            generator.writeEntry(OcmMetadataKey.NEXT_LEAP_EPOCH.name(), timeConverter,
                                 metadata.getNextLeapEpoch(), true, true);
            generator.writeEntry(OcmMetadataKey.NEXT_LEAP_TAIMUTC.name(), metadata.getNextLeapTaimutc(), Unit.SECOND, true);
        }
        generator.writeEntry(OcmMetadataKey.UT1MUTC_AT_TZERO.name(), metadata.getUt1mutcT0(), Unit.SECOND, false);

        // data sources
        generator.writeEntry(OcmMetadataKey.EOP_SOURCE.name(),
                             metadata.getEopSource(), null, false);
        generator.writeEntry(OcmMetadataKey.INTERP_METHOD_EOP.name(),
                             metadata.getInterpMethodEOP(), null, false);
        generator.writeEntry(OcmMetadataKey.CELESTIAL_SOURCE.name(),
                             metadata.getCelestialSource(), null, false);

    }

}
