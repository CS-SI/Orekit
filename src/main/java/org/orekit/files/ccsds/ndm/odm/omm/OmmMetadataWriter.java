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
package org.orekit.files.ccsds.ndm.odm.omm;

import java.io.IOException;

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.ndm.odm.CommonMetadataWriter;
import org.orekit.files.ccsds.utils.generation.Generator;

/**
 * Writer for Common metadata for CCSDS Orbit Mean-Elements Messages.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
class OmmMetadataWriter extends CommonMetadataWriter {

    /** Metadata. */
    private final OmmMetadata metadata;

    /** Simple constructor.
     * @param metadata metadata to write
     * @param timeConverter converter for dates
     */
    OmmMetadataWriter(final OmmMetadata metadata, final TimeConverter timeConverter) {
        super(metadata, timeConverter);
        this.metadata = metadata;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        super.writeContent(generator);

        // mean elements theory
        generator.writeEntry(OmmMetadataKey.MEAN_ELEMENT_THEORY.name(), metadata.getMeanElementTheory(), null, true);

    }

}
