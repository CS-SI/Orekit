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

package org.orekit.files.ccsds.ndm.adm.apm;

import java.io.IOException;

import org.hipparchus.complex.Quaternion;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndpoints;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.units.Unit;

/** Writer for quaternion data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class ApmQuaternionWriter extends AbstractWriter {

    /** Format version.
     * @since 12.0
     */
    private final double formatVersion;

    /** Quaternion block. */
    private final ApmQuaternion quaternion;

    /** Quaternion epoch. */
    private final AbsoluteDate epoch;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param formatVersion format version
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param quaternion quaternion to write
     * @param epoch quaternion epoch (only for ADM V1)
     * @param timeConverter converter for dates
     */
    ApmQuaternionWriter(final double formatVersion, final String xmlTag, final String kvnTag,
                        final ApmQuaternion quaternion,
                        final AbsoluteDate epoch, final TimeConverter timeConverter) {
        super(xmlTag, kvnTag);
        this.formatVersion = formatVersion;
        this.quaternion    = quaternion;
        this.epoch         = epoch;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(quaternion.getComments());

        if (epoch != null) {
            // epoch is in the quaternion block only in ADM V1
            generator.writeEntry(ApmQuaternionKey.EPOCH.name(), timeConverter, epoch, false, true);
        }

        // endpoints
        if (formatVersion < 2.0) {
            generator.writeEntry(ApmQuaternionKey.Q_FRAME_A.name(), quaternion.getEndpoints().getFrameA().getName(), null, true);
            generator.writeEntry(ApmQuaternionKey.Q_FRAME_B.name(), quaternion.getEndpoints().getFrameB().getName(), null, true);
            generator.writeEntry(ApmQuaternionKey.Q_DIR.name(),
                                 quaternion.getEndpoints().isA2b() ? AttitudeEndpoints.A2B : AttitudeEndpoints.B2A,
                                                                   null, true);
        } else {
            generator.writeEntry(ApmQuaternionKey.REF_FRAME_A.name(), quaternion.getEndpoints().getFrameA().getName(), null, true);
            generator.writeEntry(ApmQuaternionKey.REF_FRAME_B.name(), quaternion.getEndpoints().getFrameB().getName(), null, true);
        }

        // quaternion
        if (generator.getFormat() == FileFormat.XML) {
            generator.enterSection(ApmQuaternionKey.quaternion.name());
        }
        final Quaternion q = quaternion.getQuaternion();
        generator.writeEntry(ApmQuaternionKey.Q1.name(), q.getQ1(), Unit.ONE, true);
        generator.writeEntry(ApmQuaternionKey.Q2.name(), q.getQ2(), Unit.ONE, true);
        generator.writeEntry(ApmQuaternionKey.Q3.name(), q.getQ3(), Unit.ONE, true);
        generator.writeEntry(ApmQuaternionKey.QC.name(), q.getQ0(), Unit.ONE, true);
        if (generator.getFormat() == FileFormat.XML) {
            generator.exitSection();
        }

        // quaternion derivative
        if (quaternion.hasRates()) {
            if (generator.getFormat() == FileFormat.XML) {
                generator.enterSection(formatVersion < 2.0 ?
                                       ApmQuaternionKey.quaternionRate.name() :
                                       ApmQuaternionKey.quaternionDot.name());
            }
            final Quaternion qDot = quaternion.getQuaternionDot();
            generator.writeEntry(ApmQuaternionKey.Q1_DOT.name(), qDot.getQ1(), Unit.ONE, true);
            generator.writeEntry(ApmQuaternionKey.Q2_DOT.name(), qDot.getQ2(), Unit.ONE, true);
            generator.writeEntry(ApmQuaternionKey.Q3_DOT.name(), qDot.getQ3(), Unit.ONE, true);
            generator.writeEntry(ApmQuaternionKey.QC_DOT.name(), qDot.getQ0(), Unit.ONE, true);
            if (generator.getFormat() == FileFormat.XML) {
                generator.exitSection();
            }
        }

    }

}
