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

package org.orekit.files.ccsds.ndm.odm.opm;

import java.io.IOException;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.odm.KeplerianElements;
import org.orekit.files.ccsds.ndm.odm.KeplerianElementsKey;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.orbits.PositionAngleType;
import org.orekit.utils.units.Unit;

/** Writer for Keplerian elements data in OPM files.
 * @author Luc Maisonobe
 * @since 11.0
 */
class OsculationgKeplerianElementsWriter extends AbstractWriter {

    /** Keplerian elements block. */
    private final KeplerianElements keplerianElements;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param keplerianElements Keplerian elements to write
     */
    OsculationgKeplerianElementsWriter(final String xmlTag, final String kvnTag,
                                       final KeplerianElements keplerianElements) {
        super(xmlTag, kvnTag);
        this.keplerianElements = keplerianElements;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // Keplerian elements block
        generator.writeComments(keplerianElements.getComments());
        generator.writeEntry(KeplerianElementsKey.SEMI_MAJOR_AXIS.name(),   keplerianElements.getA(), Unit.KILOMETRE, true);
        generator.writeEntry(KeplerianElementsKey.ECCENTRICITY.name(),      keplerianElements.getE(), Unit.ONE,       true);
        generator.writeEntry(KeplerianElementsKey.INCLINATION.name(),       keplerianElements.getI(), Unit.DEGREE,    true);
        generator.writeEntry(KeplerianElementsKey.RA_OF_ASC_NODE.name(),    keplerianElements.getRaan(), Unit.DEGREE, true);
        generator.writeEntry(KeplerianElementsKey.ARG_OF_PERICENTER.name(), keplerianElements.getPa(), Unit.DEGREE,   true);
        generator.writeEntry(keplerianElements.getAnomalyType() == PositionAngleType.TRUE ?
                                                                   KeplerianElementsKey.TRUE_ANOMALY.name() : KeplerianElementsKey.MEAN_ANOMALY.name(),
                                                                   keplerianElements.getAnomaly(),
                                                                   Unit.DEGREE,
                                                                   true);
        generator.writeEntry(KeplerianElementsKey.GM.name(), keplerianElements.getMu(), Units.KM3_PER_S2, true);

    }

}
