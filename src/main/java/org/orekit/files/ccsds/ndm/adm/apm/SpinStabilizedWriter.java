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

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndoints;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/** Writer for spin stabilized data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class SpinStabilizedWriter extends AbstractWriter {

    /** Spin stabilized block. */
    private final SpinStabilized spinStabilized;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param spinStabilized spin stabilized data to write
     */
    SpinStabilizedWriter(final String xmlTag, final String kvnTag,
                        final SpinStabilized spinStabilized) {
        super(xmlTag, kvnTag);
        this.spinStabilized = spinStabilized;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(spinStabilized.getComments());

        // endpoints
        generator.writeEntry(SpinStabilizedKey.SPIN_FRAME_A.name(), spinStabilized.getEndpoints().getFrameA().getName(), null, true);
        generator.writeEntry(SpinStabilizedKey.SPIN_FRAME_B.name(), spinStabilized.getEndpoints().getFrameB().getName(), null, true);
        generator.writeEntry(SpinStabilizedKey.SPIN_DIR.name(),
                             spinStabilized.getEndpoints().isA2b() ? AttitudeEndoints.A2B : AttitudeEndoints.B2A,
                             null, true);

        // spin
        generator.writeEntry(SpinStabilizedKey.SPIN_ALPHA.name(),     spinStabilized.getSpinAlpha(), Unit.DEGREE,        true);
        generator.writeEntry(SpinStabilizedKey.SPIN_DELTA.name(),     spinStabilized.getSpinDelta(), Unit.DEGREE,        true);
        generator.writeEntry(SpinStabilizedKey.SPIN_ANGLE.name(),     spinStabilized.getSpinAngle(), Unit.DEGREE,        true);
        generator.writeEntry(SpinStabilizedKey.SPIN_ANGLE_VEL.name(), spinStabilized.getSpinAngleVel(), Units.DEG_PER_S, true);

        // nutation
        generator.writeEntry(SpinStabilizedKey.NUTATION.name(),       spinStabilized.getNutation(), Unit.DEGREE,       false);
        generator.writeEntry(SpinStabilizedKey.NUTATION_PER.name(),   spinStabilized.getNutationPeriod(), Unit.SECOND, false);
        generator.writeEntry(SpinStabilizedKey.NUTATION_PHASE.name(), spinStabilized.getNutationPhase(), Unit.DEGREE,  false);

    }

}
