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
import java.util.Optional;

import org.hipparchus.linear.RealMatrix;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
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
        generator.writeOptionalDoubleEntry(AttitudePhysicalPropertiesKey.DRAG_COEFF.name(), phys.getDragCoefficient(), Unit.ONE,     false);

        // mass
        generator.writeOptionalDoubleEntry(AttitudePhysicalPropertiesKey.WET_MASS.name(),   phys.getWetMass(), Unit.KILOGRAM,        false);
        generator.writeOptionalDoubleEntry(AttitudePhysicalPropertiesKey.DRY_MASS.name(),   phys.getDryMass(), Unit.KILOGRAM,        false);

        // center of pressure
        if (phys.getCenterOfPressureReferenceFrame().isPresent()) {
            generator.writeEntry(AttitudePhysicalPropertiesKey.CP_REF_FRAME.name(), phys.getCenterOfPressureReferenceFrame().get().getName(), null, false);
        }
        if (phys.getCenterOfPressure().isPresent()) {
            final StringBuilder cp = new StringBuilder();
            cp.append(generator.doubleToString(Unit.METRE.fromSI(phys.getCenterOfPressure().get().getX())));
            cp.append(' ');
            cp.append(generator.doubleToString(Unit.METRE.fromSI(phys.getCenterOfPressure().get().getY())));
            cp.append(' ');
            cp.append(generator.doubleToString(Unit.METRE.fromSI(phys.getCenterOfPressure().get().getZ())));
            generator.writeEntry(AttitudePhysicalPropertiesKey.CP.name(), cp.toString(), Unit.METRE, false);
        }

        // inertia
        if (phys.getInertiaReferenceFrame().isPresent()) {
            generator.writeEntry(AttitudePhysicalPropertiesKey.INERTIA_REF_FRAME.name(), phys.getInertiaReferenceFrame().get().getName(), null, false);
        }
        final Optional<RealMatrix> inertia = phys.getInertiaMatrix();
        if (inertia.isPresent()) {
            generator.writeEntry(AttitudePhysicalPropertiesKey.IXX.name(), inertia.get().getEntry(0, 0), Units.KG_M2, true);
            generator.writeEntry(AttitudePhysicalPropertiesKey.IYY.name(), inertia.get().getEntry(1, 1), Units.KG_M2, true);
            generator.writeEntry(AttitudePhysicalPropertiesKey.IZZ.name(), inertia.get().getEntry(2, 2), Units.KG_M2, true);
            generator.writeEntry(AttitudePhysicalPropertiesKey.IXY.name(), inertia.get().getEntry(0, 1), Units.KG_M2, true);
            generator.writeEntry(AttitudePhysicalPropertiesKey.IXZ.name(), inertia.get().getEntry(0, 2), Units.KG_M2, true);
            generator.writeEntry(AttitudePhysicalPropertiesKey.IYZ.name(), inertia.get().getEntry(1, 2), Units.KG_M2, true);
        }

    }

}
