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

package org.orekit.files.ccsds.ndm.odm;

import java.io.IOException;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/** Writer for spacecraft parameters data.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class SpacecraftParametersWriter extends AbstractWriter {

    /** Spacecraft parameters block. */
    private final SpacecraftParameters spacecraftParameters;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param spacecraftParameters spacecraft parameters to write
     */
    public SpacecraftParametersWriter(final String xmlTag, final String kvnTag,
                                      final SpacecraftParameters spacecraftParameters) {
        super(xmlTag, kvnTag);
        this.spacecraftParameters = spacecraftParameters;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // spacecraft parameters block
        generator.writeComments(spacecraftParameters.getComments());

        // mass
        generator.writeEntry(SpacecraftParametersKey.MASS.name(),
                             Unit.KILOGRAM.fromSI(spacecraftParameters.getMass()),
                             false);

        // solar parameters
        generator.writeEntry(SpacecraftParametersKey.SOLAR_RAD_AREA.name(),
                             Units.M2.fromSI(spacecraftParameters.getSolarRadArea()),
                             false);
        generator.writeEntry(SpacecraftParametersKey.SOLAR_RAD_COEFF.name(),
                             Unit.ONE.fromSI(spacecraftParameters.getSolarRadCoeff()),
                             false);

        // drag parameters
        generator.writeEntry(SpacecraftParametersKey.DRAG_AREA.name(),
                             Units.M2.fromSI(spacecraftParameters.getDragArea()),
                             false);
        generator.writeEntry(SpacecraftParametersKey.DRAG_COEFF.name(),
                             Unit.ONE.fromSI(spacecraftParameters.getDragCoeff()),
                             false);

    }

}
