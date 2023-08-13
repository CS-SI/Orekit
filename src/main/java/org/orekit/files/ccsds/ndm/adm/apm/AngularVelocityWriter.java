/* Copyright 2023 Luc Maisonobe
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
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;

/** Writer for angular velocity data.
 * @author Luc Maisonobe
 * @since 12.0
 */
class AngularVelocityWriter extends AbstractWriter {

    /** Angular velocity block. */
    private final AngularVelocity angularVelocity;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param angularVelocity angular velocity block
     */
    AngularVelocityWriter(final String xmlTag, final String kvnTag,
                          final AngularVelocity angularVelocity) {
        super(xmlTag, kvnTag);
        this.angularVelocity = angularVelocity;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(angularVelocity.getComments());

        // endpoints
        generator.writeEntry(AngularVelocityKey.REF_FRAME_A.name(), angularVelocity.getEndpoints().getFrameA().getName(), null, true);
        generator.writeEntry(AngularVelocityKey.REF_FRAME_B.name(), angularVelocity.getEndpoints().getFrameB().getName(), null, true);

        // frame
        generator.writeEntry(AngularVelocityKey.ANGVEL_FRAME.name(), angularVelocity.getFrame().getName(), null, true);

        // velocity
        generator.writeEntry(AngularVelocityKey.ANGVEL_X.name(), angularVelocity.getAngVelX(), Units.DEG_PER_S, true);
        generator.writeEntry(AngularVelocityKey.ANGVEL_Y.name(), angularVelocity.getAngVelY(), Units.DEG_PER_S, true);
        generator.writeEntry(AngularVelocityKey.ANGVEL_Z.name(), angularVelocity.getAngVelZ(), Units.DEG_PER_S, true);

    }

}
