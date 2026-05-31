/* Copyright 2022-2026 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import java.io.IOException;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/** Writer for attitude maneuver data.
 * @author Luc Maisonobe
 * @since 12.0
 */
class AttitudeManeuverWriter extends AbstractWriter {

    /** Attitude maneuver block. */
    private final AttitudeManeuver man;

    /** Create a writer.
     * @param attitudeManeuver attitude maneuver to write
     */
    AttitudeManeuverWriter(final AttitudeManeuver attitudeManeuver) {
        super(AcmDataSubStructureKey.man.name(), AcmDataSubStructureKey.MAN.name());
        this.man = attitudeManeuver;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // attitude maneuver block
        generator.writeComments(man.getComments());

        // identifiers
        generator.writeOptionalStringEntry(AttitudeManeuverKey.MAN_ID.name(),      man.getID(),         null, false);
        generator.writeOptionalStringEntry(AttitudeManeuverKey.MAN_PREV_ID.name(), man.getPrevID(),     null, false);
        generator.writeEntry(AttitudeManeuverKey.MAN_PURPOSE.name(),               man.getManPurpose(), null, true);

        // time
        generator.writeEntry(AttitudeManeuverKey.MAN_BEGIN_TIME.name(),              man.getBeginTime(), Unit.SECOND,      false);
        generator.writeOptionalDoubleEntry(AttitudeManeuverKey.MAN_END_TIME.name(),  man.getEndTime(),   Unit.SECOND,      false);
        generator.writeOptionalDoubleEntry(AttitudeManeuverKey.MAN_DURATION.name(),  man.getDuration(),  Unit.SECOND,      false);

        // actuator
        generator.writeOptionalStringEntry(AttitudeManeuverKey.ACTUATOR_USED.name(), man.getActuatorUsed(), null, false);

        // target
        if (man.getTargetMomentum().isPresent()) {
            final StringBuilder momentum = new StringBuilder();
            momentum.append(generator.doubleToString(Units.N_M_S.fromSI(man.getTargetMomentum().get().getX())));
            momentum.append(' ');
            momentum.append(generator.doubleToString(Units.N_M_S.fromSI(man.getTargetMomentum().get().getY())));
            momentum.append(' ');
            momentum.append(generator.doubleToString(Units.N_M_S.fromSI(man.getTargetMomentum().get().getZ())));
            generator.writeEntry(AttitudeManeuverKey.TARGET_MOMENTUM.name(), momentum.toString(),                Units.N_M_S, true);
            if (man.getTargetMomFrame().isPresent()) {
                generator.writeEntry(AttitudeManeuverKey.TARGET_MOM_FRAME.name(), man.getTargetMomFrame().get().getName(), null,    false);
            }
        }

        if (man.getTargetAttitude().isPresent()) {
            final StringBuilder attitude = new StringBuilder();
            attitude.append(generator.doubleToString(man.getTargetAttitude().get().getQ1()));
            attitude.append(' ');
            attitude.append(generator.doubleToString(man.getTargetAttitude().get().getQ2()));
            attitude.append(' ');
            attitude.append(generator.doubleToString(man.getTargetAttitude().get().getQ3()));
            attitude.append(' ');
            attitude.append(generator.doubleToString(man.getTargetAttitude().get().getQ0()));
            generator.writeEntry(AttitudeManeuverKey.TARGET_ATTITUDE.name(), attitude.toString(), null, true);
        }

        generator.writeOptionalDoubleEntry(AttitudeManeuverKey.TARGET_SPINRATE.name(), man.getTargetSpinRate(), Units.DEG_PER_S, false);

    }

}
