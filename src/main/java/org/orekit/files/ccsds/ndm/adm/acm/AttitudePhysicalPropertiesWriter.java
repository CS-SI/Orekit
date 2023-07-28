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

package org.orekit.files.ccsds.ndm.adm.acm;

import java.io.IOException;

import org.hipparchus.linear.RealMatrix;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Writer for physical properties data.
 * @author Luc Maisonobe
 * @since 12.0
 */
class AttitudePhysicalPropertiesWriter extends AbstractWriter {

    /** Physical properties block. */
    private final AttitudePhysicalProperties phys;

    /** Create a writer.
     * @param phys physical properties to write
     */
    AttitudePhysicalPropertiesWriter(final AttitudePhysicalProperties phys) {
        super(AcmDataSubStructureKey.phys.name(), AcmDataSubStructureKey.PHYS.name());
        this.phys = phys;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // physical properties block
        generator.writeComments(phys.getComments());

        // drag
        generator.writeEntry(AttitudePhysicalPropertiesKey.DRAG_COEFF.name(), phys.getDragCoefficient(), Unit.ONE,     false);

        // mass
        generator.writeEntry(AttitudePhysicalPropertiesKey.WET_MASS.name(),   phys.getWetMass(), Unit.KILOGRAM,        false);
        generator.writeEntry(AttitudePhysicalPropertiesKey.DRY_MASS.name(),   phys.getDryMass(), Unit.KILOGRAM,        false);

        // center of pressure
        if (phys.getCenterOfPressureReferenceFrame() != null) {
            generator.writeEntry(AttitudePhysicalPropertiesKey.CP_REF_FRAME.name(), phys.getCenterOfPressureReferenceFrame().getName(), null, false);
        }
        if (phys.getCenterOfPressure() != null) {
            final StringBuilder cp = new StringBuilder();
            cp.append(AccurateFormatter.format(Unit.METRE.fromSI(phys.getCenterOfPressure().getX())));
            cp.append(' ');
            cp.append(AccurateFormatter.format(Unit.METRE.fromSI(phys.getCenterOfPressure().getY())));
            cp.append(' ');
            cp.append(AccurateFormatter.format(Unit.METRE.fromSI(phys.getCenterOfPressure().getZ())));
            generator.writeEntry(AttitudePhysicalPropertiesKey.CP.name(), cp.toString(), Unit.METRE, false);
        }

        // inertia
        if (phys.getInertiaReferenceFrame() != null) {
            generator.writeEntry(AttitudePhysicalPropertiesKey.INERTIA_REF_FRAME.name(), phys.getInertiaReferenceFrame().getName(), null, false);
        }
        final RealMatrix inertia = phys.getInertiaMatrix();
        if (inertia != null) {
            generator.writeEntry(AttitudePhysicalPropertiesKey.IXX.name(), inertia.getEntry(0, 0), Units.KG_M2, true);
            generator.writeEntry(AttitudePhysicalPropertiesKey.IYY.name(), inertia.getEntry(1, 1), Units.KG_M2, true);
            generator.writeEntry(AttitudePhysicalPropertiesKey.IZZ.name(), inertia.getEntry(2, 2), Units.KG_M2, true);
            generator.writeEntry(AttitudePhysicalPropertiesKey.IXY.name(), inertia.getEntry(0, 1), Units.KG_M2, true);
            generator.writeEntry(AttitudePhysicalPropertiesKey.IXZ.name(), inertia.getEntry(0, 2), Units.KG_M2, true);
            generator.writeEntry(AttitudePhysicalPropertiesKey.IYZ.name(), inertia.getEntry(1, 2), Units.KG_M2, true);
        }

    }

}
