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

import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/** Writer for maneuver parameters data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class ManeuverWriter extends AbstractWriter {

    /** Maneuver parameters block. */
    private final Maneuver maneuver;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param maneuver maneuver to write
     * @param timeConverter converter for dates
     */
    ManeuverWriter(final Maneuver maneuver, final TimeConverter timeConverter) {
        super(XmlSubStructureKey.maneuverParameters.name(), null);
        this.maneuver      = maneuver;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // maneuver block
        generator.writeComments(maneuver.getComments());

        generator.writeEntry(ManeuverKey.MAN_EPOCH_IGNITION.name(), timeConverter, maneuver.getEpochIgnition(),   false, true);
        generator.writeEntry(ManeuverKey.MAN_DURATION.name(),       maneuver.getDuration(), Unit.SECOND,          true);
        generator.writeEntry(ManeuverKey.MAN_DELTA_MASS.name(),     maneuver.getDeltaMass(), Unit.KILOGRAM,       true);
        generator.writeEntry(ManeuverKey.MAN_REF_FRAME.name(),      maneuver.getReferenceFrame().getName(), null, true);
        generator.writeEntry(ManeuverKey.MAN_DV_1.name(),           maneuver.getDV().getX(), Units.KM_PER_S,      true);
        generator.writeEntry(ManeuverKey.MAN_DV_2.name(),           maneuver.getDV().getY(), Units.KM_PER_S,      true);
        generator.writeEntry(ManeuverKey.MAN_DV_3.name(),           maneuver.getDV().getZ(), Units.KM_PER_S,      true);

    }

}
