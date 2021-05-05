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

package org.orekit.files.ccsds.ndm.adm.apm;

import java.io.IOException;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndoints;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/** Writer for Euler data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class EulerWriter extends AbstractWriter {

    /** Suffix for angles. */
    private static String ANGLE = "_ANGLE";

    /** Suffix for rates. */
    private static String RATE = "_RATE";

    /** Euler block. */
    private final Euler euler;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param euler Euler data to write
     */
    EulerWriter(final String xmlTag, final String kvnTag,
                        final Euler euler) {
        super(xmlTag, kvnTag);
        this.euler = euler;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(euler.getComments());

        // endpoints
        generator.writeEntry(EulerKey.EULER_FRAME_A.name(), euler.getEndpoints().getFrameA().getName(), null, true);
        generator.writeEntry(EulerKey.EULER_FRAME_B.name(), euler.getEndpoints().getFrameB().getName(), null, true);
        generator.writeEntry(EulerKey.EULER_DIR.name(),
                             euler.getEndpoints().isA2b() ? AttitudeEndoints.A2B : AttitudeEndoints.B2A,
                             null, true);

        // angles
        final String   seq    = euler.getEulerRotSeq().name();
        final double[] angles = euler.getRotationAngles();
        generator.writeEntry(EulerKey.EULER_ROT_SEQ.name(),
                             seq.replace('X', '1').replace('Y', '2').replace('Z', '3'),
                             null, true);
        generator.writeEntry(EulerKey.RATE_FRAME.name(),
                             euler.rateFrameIsA() ? EulerKey.EULER_FRAME_A.name() : EulerKey.EULER_FRAME_B.name(),
                             null, true);

        // if we don't have rates, at least we need angles
        // (we may have only rates, as orientation is already given by mandatory quaternion)
        final boolean needsAngles = !euler.hasRates();
        generator.writeEntry(seq.charAt(0) + ANGLE, angles[0], Unit.DEGREE, needsAngles);
        generator.writeEntry(seq.charAt(1) + ANGLE, angles[1], Unit.DEGREE, needsAngles);
        generator.writeEntry(seq.charAt(2) + ANGLE, angles[2], Unit.DEGREE, needsAngles);

        // rates
        if (euler.hasRates()) {
            final double[] rates = euler.getRotationRates();
            generator.writeEntry(seq.charAt(0) + RATE, rates[0], Units.DEG_PER_S, true);
            generator.writeEntry(seq.charAt(1) + RATE, rates[1], Units.DEG_PER_S, true);
            generator.writeEntry(seq.charAt(2) + RATE, rates[2], Units.DEG_PER_S, true);
        }

    }

}
