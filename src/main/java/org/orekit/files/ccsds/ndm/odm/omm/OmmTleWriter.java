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

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/** Writer for Two-Line Elements data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class OmmTleWriter extends AbstractWriter {

    /** Two-Lines Elements block. */
    private final OmmTle tleBlock;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param tleBlock Two-Lines Elements to write
     */
    OmmTleWriter(final String xmlTag, final String kvnTag,
                 final OmmTle tleBlock) {
        super(xmlTag, kvnTag);
        this.tleBlock = tleBlock;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // Keplerian elements block
        generator.writeComments(tleBlock.getComments());
        generator.writeEntry(OmmTleKey.EPHEMERIS_TYPE.name(),      tleBlock.getEphemerisType(),      true);
        generator.writeEntry(OmmTleKey.CLASSIFICATION_TYPE.name(), tleBlock.getClassificationType(), true);
        generator.writeEntry(OmmTleKey.NORAD_CAT_ID.name(),        tleBlock.getNoradID(),            true);
        generator.writeEntry(OmmTleKey.ELEMENT_SET_NO.name(),      tleBlock.getElementSetNumber(),   true);
        generator.writeEntry(OmmTleKey.REV_AT_EPOCH.name(),        tleBlock.getRevAtEpoch(),         true);
        generator.writeEntry(OmmTleKey.BSTAR.name(),               tleBlock.getBStar(),            Unit.ONE,                  true);
        generator.writeEntry(OmmTleKey.MEAN_MOTION_DOT.name(),     tleBlock.getMeanMotionDot(),    Units.REV_PER_DAY2_SCALED, true);
        generator.writeEntry(OmmTleKey.MEAN_MOTION_DDOT.name(),    tleBlock.getMeanMotionDotDot(), Units.REV_PER_DAY3_SCALED, true);

    }

}
