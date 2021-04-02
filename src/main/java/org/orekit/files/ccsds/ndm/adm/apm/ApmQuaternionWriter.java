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

import org.hipparchus.complex.Quaternion;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndoints;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/** Writer for quaternion data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class ApmQuaternionWriter extends AbstractWriter {

    /** Quaternion block. */
    private final ApmQuaternion quaternion;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param quaternion quaternion to write
     * @param timeConverter converter for dates
     */
    ApmQuaternionWriter(final String xmlTag, final String kvnTag,
                        final ApmQuaternion quaternion, final TimeConverter timeConverter) {
        super(xmlTag, kvnTag);
        this.quaternion    = quaternion;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeEntry(ApmQuaternionKey.EPOCH.name(), timeConverter, quaternion.getEpoch(), true);

        // endpoints
        generator.writeEntry(ApmQuaternionKey.Q_FRAME_A.name(), quaternion.getEndpoints().getFrameA().getName(), true);
        generator.writeEntry(ApmQuaternionKey.Q_FRAME_B.name(), quaternion.getEndpoints().getFrameB().getName(), true);
        generator.writeEntry(ApmQuaternionKey.Q_DIR.name(),
                             quaternion.getEndpoints().isA2b() ? AttitudeEndoints.A2B : AttitudeEndoints.B2A,
                             true);

        // quaternion
        final Quaternion q = quaternion.getQuaternion();
        generator.writeEntry(ApmQuaternionKey.Q1.name(), Unit.ONE.fromSI(q.getQ1()), true);
        generator.writeEntry(ApmQuaternionKey.Q2.name(), Unit.ONE.fromSI(q.getQ2()), true);
        generator.writeEntry(ApmQuaternionKey.Q3.name(), Unit.ONE.fromSI(q.getQ3()), true);
        generator.writeEntry(ApmQuaternionKey.QC.name(), Unit.ONE.fromSI(q.getQ0()), true);

        // quaternion derivative
        if (quaternion.hasRates()) {
            final Quaternion qDot = quaternion.getQuaternionDot();
            generator.writeEntry(ApmQuaternionKey.Q1_DOT.name(), Unit.ONE.fromSI(qDot.getQ1()), true);
            generator.writeEntry(ApmQuaternionKey.Q2_DOT.name(), Unit.ONE.fromSI(qDot.getQ2()), true);
            generator.writeEntry(ApmQuaternionKey.Q3_DOT.name(), Unit.ONE.fromSI(qDot.getQ3()), true);
            generator.writeEntry(ApmQuaternionKey.QC_DOT.name(), Unit.ONE.fromSI(qDot.getQ0()), true);
        }

    }

}
