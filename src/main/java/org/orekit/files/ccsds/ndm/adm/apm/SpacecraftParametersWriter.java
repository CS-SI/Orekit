/* Copyright 2002-2022 CS GROUP
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

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;

/** Writer for spacecraft parameters data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class SpacecraftParametersWriter extends AbstractWriter {

    /** Spacecraft parameters block. */
    private final SpacecraftParameters spacecraftParameters;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param spacecraftParameters spacecraft parameters data to write
     */
    SpacecraftParametersWriter(final String xmlTag, final String kvnTag,
                               final SpacecraftParameters spacecraftParameters) {
        super(xmlTag, kvnTag);
        this.spacecraftParameters = spacecraftParameters;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(spacecraftParameters.getComments());

        // frame
        if (spacecraftParameters.getInertiaReferenceFrame() != null) {
            generator.writeEntry(SpacecraftParametersKey.INERTIA_REF_FRAME.name(),
                                 spacecraftParameters.getInertiaReferenceFrame().getName(),
                                 null, false);
        }

        // inertia matrix
        generator.writeEntry(SpacecraftParametersKey.I11.name(), spacecraftParameters.getI11(), Units.KG_M2, true);
        generator.writeEntry(SpacecraftParametersKey.I22.name(), spacecraftParameters.getI22(), Units.KG_M2, true);
        generator.writeEntry(SpacecraftParametersKey.I33.name(), spacecraftParameters.getI33(), Units.KG_M2, true);
        generator.writeEntry(SpacecraftParametersKey.I12.name(), spacecraftParameters.getI12(), Units.KG_M2, true);
        generator.writeEntry(SpacecraftParametersKey.I13.name(), spacecraftParameters.getI13(), Units.KG_M2, true);
        generator.writeEntry(SpacecraftParametersKey.I23.name(), spacecraftParameters.getI23(), Units.KG_M2, true);

    }

}
