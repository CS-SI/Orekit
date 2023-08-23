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
package org.orekit.files.ccsds.ndm.adm;

import java.io.IOException;

import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.section.MetadataKey;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.generation.Generator;

/**
 * Writer for Common metadata for CCSDS Attitude Parameter/Ephemeris Messages.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
public class AdmCommonMetadataWriter extends AbstractWriter {

    /** Metadata. */
    private final AdmMetadata metadata;

    /** Simple constructor.
     * @param metadata metadata to write
     */
    public AdmCommonMetadataWriter(final AdmMetadata metadata) {
        super(XmlStructureKey.metadata.name(), null);
        this.metadata      = metadata;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(metadata.getComments());

        // object
        generator.writeEntry(AdmMetadataKey.OBJECT_NAME.name(),     metadata.getObjectName(), null, true);
        generator.writeEntry(AdmCommonMetadataKey.OBJECT_ID.name(), metadata.getObjectID(),   null, true);

        if (metadata.getCenter() != null) {
            // center
            generator.writeEntry(AdmMetadataKey.CENTER_NAME.name(), metadata.getCenter().getName(), null, true);
        }

        // time
        generator.writeEntry(MetadataKey.TIME_SYSTEM.name(),    metadata.getTimeSystem(), true);

    }

}
