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
import org.orekit.orbits.PositionAngle;
import org.orekit.utils.units.Unit;

/** Writer for Keplerian elements data.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class KeplerianElementsWriter extends AbstractWriter {

    /** Keplerian elements block. */
    private final KeplerianElements keplerianElements;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param keplerianElements Keplerian elements to write
     */
    public KeplerianElementsWriter(final String xmlTag, final String kvnTag,
                                   final KeplerianElements keplerianElements) {
        super(xmlTag, kvnTag);
        this.keplerianElements = keplerianElements;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // Keplerian elements block
        generator.writeComments(keplerianElements.getComments());
        generator.writeEntry(KeplerianElementsKey.SEMI_MAJOR_AXIS.name(), Unit.KILOMETRE.fromSI(keplerianElements.getA()), true);
        generator.writeEntry(KeplerianElementsKey.ECCENTRICITY.name(),    Unit.ONE.fromSI(keplerianElements.getE()),       true);
        generator.writeEntry(KeplerianElementsKey.INCLINATION.name(),     Unit.DEGREE.fromSI(keplerianElements.getI()),    true);
        generator.writeEntry(KeplerianElementsKey.RA_OF_ASC_NODE.name(),  Unit.DEGREE.fromSI(keplerianElements.getRaan()), true);
        generator.writeEntry(keplerianElements.getAnomalyType() == PositionAngle.TRUE ?
                             KeplerianElementsKey.TRUE_ANOMALY.name() : KeplerianElementsKey.MEAN_ANOMALY.name(),
                             Unit.DEGREE.fromSI(keplerianElements.getAnomaly()),
                             true);
        generator.writeEntry(KeplerianElementsKey.GM.name(), Units.KM3_PER_S2.fromSI(keplerianElements.getMu()), true);

    }

}
