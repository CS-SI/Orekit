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
import java.util.ArrayList;
import java.util.List;

import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.generation.Generator;

/**
 * Writer for Metadata for CCSDS Conjunction Data Messages.
 *
 * @author Melina Vanel
 * @since 11.2
 */
public class CdmMetadataWriter extends AbstractWriter {

    /** Metadata. */
    private final CdmMetadata metadata;

    /** Simple constructor.
     * @param metadata metadata to write
     */
    public CdmMetadataWriter(final CdmMetadata metadata) {
        super(XmlStructureKey.metadata.name(), null);
        this.metadata      = metadata;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(metadata.getComments());

        // object
        generator.writeEntry(CdmMetadataKey.OBJECT.name(),
                             metadata.getObject(), null, true);
        generator.writeEntry(CdmMetadataKey.OBJECT_DESIGNATOR.name(),
                             metadata.getObjectDesignator(), null, true);
        generator.writeEntry(CdmMetadataKey.CATALOG_NAME.name(),
                             metadata.getCatalogName(), null, true);
        generator.writeEntry(CdmMetadataKey.OBJECT_NAME.name(),
                             metadata.getObjectName(), null, true);
        generator.writeEntry(CdmMetadataKey.INTERNATIONAL_DESIGNATOR.name(),
                             metadata.getInternationalDes(), null, true);
        generator.writeEntry(CdmMetadataKey.OBJECT_TYPE.name(),
                             metadata.getObjectType(), false);

        // originator
        generator.writeEntry(CdmMetadataKey.OPERATOR_CONTACT_POSITION.name(),
                             metadata.getOperatorContactPosition(), null, false);
        generator.writeEntry(CdmMetadataKey.OPERATOR_ORGANIZATION.name(),
                             metadata.getOperatorOrganization(), null, false);
        generator.writeEntry(CdmMetadataKey.OPERATOR_PHONE.name(),
                             metadata.getOperatorPhone(), null, false);
        generator.writeEntry(CdmMetadataKey.OPERATOR_EMAIL.name(),
                             metadata.getOperatorEmail(), null, false);

        // other information
        generator.writeEntry(CdmMetadataKey.EPHEMERIS_NAME.name(),
                             metadata.getEphemName(), null, true);
        generator.writeEntry(CdmMetadataKey.COVARIANCE_METHOD.name(),
                             metadata.getCovarianceMethod(), true);
        generator.writeEntry(CdmMetadataKey.MANEUVERABLE.name(),
                             metadata.getManeuverable().getValue(), null, true);
        if (metadata.getOrbitCenter() != null) {
            generator.writeEntry(CdmMetadataKey.ORBIT_CENTER.name(),
                                 metadata.getOrbitCenter().getName(), null, false);
        }
        generator.writeEntry(CdmMetadataKey.REF_FRAME.name(),
                             metadata.getRefFrame().getName(), null, true);
        // gravity
        if (metadata.getGravityModel() != null) {
            final String model =
                            new StringBuilder().
                            append(metadata.getGravityModel()).
                            append(": ").
                            append(metadata.getGravityDegree()).
                            append("D ").
                            append(metadata.getGravityOrder()).
                            append('O').
                            toString();
            generator.writeEntry(CdmMetadataKey.GRAVITY_MODEL.name(), model, null, false);
        }

        // atmosphere
        generator.writeEntry(CdmMetadataKey.ATMOSPHERIC_MODEL.name(), metadata.getAtmosphericModel(), null, false);

        // N body perturbation
        if (metadata.getNBodyPerturbations() != null && !metadata.getNBodyPerturbations().isEmpty()) {
            final List<String> names = new ArrayList<>();
            for (BodyFacade bf : metadata.getNBodyPerturbations()) {
                names.add(bf.getName());
            }
            generator.writeEntry(CdmMetadataKey.N_BODY_PERTURBATIONS.name(), names, false);
        }

        if (metadata.getSolarRadiationPressure() != null) {
            generator.writeEntry(CdmMetadataKey.SOLAR_RAD_PRESSURE.name(), metadata.getSolarRadiationPressure().name(),
                            null, false);
        }
        if (metadata.getEarthTides() != null) {
            generator.writeEntry(CdmMetadataKey.EARTH_TIDES.name(), metadata.getEarthTides().name(),
                            null, false);
        }
        if (metadata.getIntrackThrust() != null) {
            generator.writeEntry(CdmMetadataKey.INTRACK_THRUST.name(), metadata.getIntrackThrust().name(),
                            null, false);
        }

    }

}

