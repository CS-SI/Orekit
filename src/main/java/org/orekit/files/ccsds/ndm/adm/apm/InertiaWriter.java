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

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;

/** Writer for inertia data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class InertiaWriter extends AbstractWriter {

    /** Format version.
     * @since 12.0
     */
    private final double formatVersion;

    /** Inertia block. */
    private final Inertia inertia;

    /** Create a writer.
     * @param formatVersion format version
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param spacecraftParameters spacecraft parameters data to write
     */
    InertiaWriter(final double formatVersion, final String xmlTag, final String kvnTag,
                  final Inertia spacecraftParameters) {
        super(xmlTag, kvnTag);
        this.formatVersion = formatVersion;
        this.inertia = spacecraftParameters;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(inertia.getComments());

        // frame
        if (inertia.getFrame() != null) {
            generator.writeEntry(InertiaKey.INERTIA_REF_FRAME.name(),
                                 inertia.getFrame().getName(),
                                 null, false);
        }

        // inertia matrix
        if (formatVersion < 2.0) {
            generator.writeEntry(InertiaKey.I11.name(), inertia.getInertiaMatrix().getEntry(0, 0), Units.KG_M2, true);
            generator.writeEntry(InertiaKey.I22.name(), inertia.getInertiaMatrix().getEntry(1, 1), Units.KG_M2, true);
            generator.writeEntry(InertiaKey.I33.name(), inertia.getInertiaMatrix().getEntry(2, 2), Units.KG_M2, true);
            generator.writeEntry(InertiaKey.I12.name(), inertia.getInertiaMatrix().getEntry(0, 1), Units.KG_M2, true);
            generator.writeEntry(InertiaKey.I13.name(), inertia.getInertiaMatrix().getEntry(0, 2), Units.KG_M2, true);
            generator.writeEntry(InertiaKey.I23.name(), inertia.getInertiaMatrix().getEntry(1, 2), Units.KG_M2, true);
        } else {
            generator.writeEntry(InertiaKey.IXX.name(), inertia.getInertiaMatrix().getEntry(0, 0), Units.KG_M2, true);
            generator.writeEntry(InertiaKey.IYY.name(), inertia.getInertiaMatrix().getEntry(1, 1), Units.KG_M2, true);
            generator.writeEntry(InertiaKey.IZZ.name(), inertia.getInertiaMatrix().getEntry(2, 2), Units.KG_M2, true);
            generator.writeEntry(InertiaKey.IXY.name(), inertia.getInertiaMatrix().getEntry(0, 1), Units.KG_M2, true);
            generator.writeEntry(InertiaKey.IXZ.name(), inertia.getInertiaMatrix().getEntry(0, 2), Units.KG_M2, true);
            generator.writeEntry(InertiaKey.IYZ.name(), inertia.getInertiaMatrix().getEntry(1, 2), Units.KG_M2, true);
        }

    }

}
