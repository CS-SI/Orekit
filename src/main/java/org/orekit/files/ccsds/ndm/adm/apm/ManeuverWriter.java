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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/** Writer for maneuver data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class ManeuverWriter extends AbstractWriter {

    /** Maneuver block. */
    private final Maneuver maneuver;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param maneuver maneuver data to write
     * @param timeConverter converter for dates
     */
    ManeuverWriter(final String xmlTag, final String kvnTag,
                   final Maneuver maneuver, final TimeConverter timeConverter) {
        super(xmlTag, kvnTag);
        this.maneuver      = maneuver;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(maneuver.getComments());

        // time
        generator.writeEntry(ManeuverKey.MAN_EPOCH_START.name(), timeConverter, maneuver.getEpochStart(), true);
        generator.writeEntry(ManeuverKey.MAN_DURATION.name(),    maneuver.getDuration(), Unit.SECOND,     true);

        // frame
        generator.writeEntry(ManeuverKey.MAN_REF_FRAME.name(), maneuver.getRefFrameString(), null, false);

        // torque
        final Vector3D torque = maneuver.getTorque();
        generator.writeEntry(ManeuverKey.MAN_TOR_1.name(), torque.getX(), Units.N_M, true);
        generator.writeEntry(ManeuverKey.MAN_TOR_2.name(), torque.getY(), Units.N_M, true);
        generator.writeEntry(ManeuverKey.MAN_TOR_3.name(), torque.getZ(), Units.N_M, true);

    }

}
