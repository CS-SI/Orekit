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
package org.orekit.files.ccsds.ndm.tdm;

import java.io.IOException;

import org.orekit.errors.OrekitInternalError;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.section.KvnStructureKey;
import org.orekit.files.ccsds.section.XmlStructureKey;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;


/**
 * Writer for CCSDS Tracking Data Message observation block.
 *
 * @author Luc Maisonobe
 * @since 11.0
 */
class ObservationsBlockWriter extends AbstractWriter {

    /** Observation block. */
    private final ObservationsBlock observationsBlock;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Simple constructor.
     * @param observationsBlock observation block to write
     * @param timeConverter converter for dates
     */
    ObservationsBlockWriter(final ObservationsBlock observationsBlock, final TimeConverter timeConverter) {
        super(XmlStructureKey.data.name(), KvnStructureKey.DATA.name());
        this.observationsBlock = observationsBlock;
        this.timeConverter     = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(observationsBlock);

        // write the data
        for (final Observation observation : observationsBlock.getObservations()) {
            if (generator.getFormat() == FileFormat.KVN) {
                generator.writeEntry(observation.getType().name(),
                                     generator.dateToString(timeConverter, observation.getEpoch()) +
                                     " " +
                                     generator.doubleToString(observation.getMeasurement()),
                                     false);
            } else {
                // TODO
                throw new OrekitInternalError(null);
            }
        }

    }

}
