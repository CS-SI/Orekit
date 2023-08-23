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
 * Writer for state vector data for CCSDS Conjunction Data Messages.
 *
 * @author Melina Vanel
 * @since 11.2
 */
public class StateVectorWriter extends AbstractWriter {

    /** State vector block. */
    private final StateVector stateVector;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param StateVector state vector data to write
     */
    StateVectorWriter(final String xmlTag, final String kvnTag,
                       final StateVector StateVector) {
        super(xmlTag, kvnTag);
        this.stateVector = StateVector;

    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(stateVector.getComments());

        // position
        generator.writeEntry(StateVectorKey.X.name(),   stateVector.getPositionVector().getX(), Unit.KILOMETRE, true);
        generator.writeEntry(StateVectorKey.Y.name(),   stateVector.getPositionVector().getY(), Unit.KILOMETRE, true);
        generator.writeEntry(StateVectorKey.Z.name(),   stateVector.getPositionVector().getZ(), Unit.KILOMETRE, true);
        // velocity
        generator.writeEntry(StateVectorKey.X_DOT.name(),   stateVector.getVelocityVector().getX(), Units.KM_PER_S, true);
        generator.writeEntry(StateVectorKey.Y_DOT.name(),   stateVector.getVelocityVector().getY(), Units.KM_PER_S, true);
        generator.writeEntry(StateVectorKey.Z_DOT.name(),   stateVector.getVelocityVector().getZ(), Units.KM_PER_S, true);

    }
}
