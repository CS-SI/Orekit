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

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/**
 * Writer for additional parameters data block for CCSDS Conjunction Data Messages.
 *
 * @author Melina Vanel
 * @since 11.2
 */
public class AdditionalParametersWriter extends AbstractWriter {

    /** Additional parameters block. */
    private final AdditionalParameters additionalParameters;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param additionalParameters additional parameters data to write
     */
    AdditionalParametersWriter(final String xmlTag, final String kvnTag,
                       final AdditionalParameters additionalParameters) {
        super(xmlTag, kvnTag);
        this.additionalParameters = additionalParameters;

    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(additionalParameters.getComments());

        // additional parameters
        generator.writeEntry(AdditionalParametersKey.AREA_PC.name(),             additionalParameters.getAreaPC(),             Units.M2,        false);
        generator.writeEntry(AdditionalParametersKey.AREA_DRG.name(),            additionalParameters.getAreaDRG(),            Units.M2,        false);
        generator.writeEntry(AdditionalParametersKey.AREA_SRP.name(),            additionalParameters.getAreaSRP(),            Units.M2,        false);
        generator.writeEntry(AdditionalParametersKey.MASS.name(),                additionalParameters.getMass(),               Unit.KILOGRAM,   false);
        generator.writeEntry(AdditionalParametersKey.CD_AREA_OVER_MASS.name(),   additionalParameters.getCDAreaOverMass(),     Units.M2_PER_KG, false);
        generator.writeEntry(AdditionalParametersKey.CR_AREA_OVER_MASS.name(),   additionalParameters.getCRAreaOverMass(),     Units.M2_PER_KG, false);
        generator.writeEntry(AdditionalParametersKey.THRUST_ACCELERATION.name(), additionalParameters.getThrustAcceleration(), Units.M_PER_S2,  false);
        generator.writeEntry(AdditionalParametersKey.SEDR.name(),                additionalParameters.getSedr(),               Units.W_PER_KG,  false);

    }

}
