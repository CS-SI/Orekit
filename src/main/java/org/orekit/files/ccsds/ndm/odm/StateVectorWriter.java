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

package org.orekit.files.ccsds.ndm.odm;

import java.io.IOException;

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.units.Unit;

/** Writer for state vector data.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class StateVectorWriter extends AbstractWriter {

    /** State vector block. */
    private final StateVector stateVector;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param stateVector state vector to write
     * @param timeConverter converter for dates
     */
    public StateVectorWriter(final String xmlTag, final String kvnTag,
                             final StateVector stateVector, final TimeConverter timeConverter) {
        super(xmlTag, kvnTag);
        this.stateVector   = stateVector;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // state vector block
        final TimeStampedPVCoordinates pv = stateVector.toTimeStampedPVCoordinates();
        generator.writeComments(stateVector.getComments());
        generator.writeEntry(StateVectorKey.EPOCH.name(), timeConverter, pv.getDate(), true, true);
        generator.writeEntry(StateVectorKey.X.name(),     pv.getPosition().getX(), Unit.KILOMETRE, true);
        generator.writeEntry(StateVectorKey.Y.name(),     pv.getPosition().getY(), Unit.KILOMETRE, true);
        generator.writeEntry(StateVectorKey.Z.name(),     pv.getPosition().getZ(), Unit.KILOMETRE, true);
        generator.writeEntry(StateVectorKey.X_DOT.name(), pv.getVelocity().getX(), Units.KM_PER_S, true);
        generator.writeEntry(StateVectorKey.Y_DOT.name(), pv.getVelocity().getY(), Units.KM_PER_S, true);
        generator.writeEntry(StateVectorKey.Z_DOT.name(), pv.getVelocity().getZ(), Units.KM_PER_S, true);
        // note that OPM format does not use X_DDOT, Y_DDOT, Z_DDOT, they are used only in OEM format

    }

}
