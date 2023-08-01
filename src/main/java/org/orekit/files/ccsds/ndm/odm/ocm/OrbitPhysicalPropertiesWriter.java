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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.io.IOException;

import org.hipparchus.linear.RealMatrix;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.utils.units.Unit;

/** Writer for physical properties data.
 * @author Luc Maisonobe
 * @since 11.0
 */
class OrbitPhysicalPropertiesWriter extends AbstractWriter {

    /** Physical properties block. */
    private final OrbitPhysicalProperties phys;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param phys physical properties to write
     * @param timeConverter converter for dates
     */
    OrbitPhysicalPropertiesWriter(final OrbitPhysicalProperties phys, final TimeConverter timeConverter) {
        super(OcmDataSubStructureKey.phys.name(), OcmDataSubStructureKey.PHYS.name());
        this.phys          = phys;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // physical properties block
        generator.writeComments(phys.getComments());

        generator.writeEntry(OrbitPhysicalPropertiesKey.MANUFACTURER.name(), phys.getManufacturer(), null, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.BUS_MODEL.name(),    phys.getBusModel(),     null, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.DOCKED_WITH.name(),  phys.getDockedWith(),         false);

        // drag
        generator.writeEntry(OrbitPhysicalPropertiesKey.DRAG_CONST_AREA.name(),  phys.getDragConstantArea(), Units.M2,    false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.DRAG_COEFF_NOM.name(),   phys.getDragCoefficient(), Unit.ONE,     false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.DRAG_UNCERTAINTY.name(), phys.getDragUncertainty(), Unit.PERCENT, false);

        // mass
        generator.writeEntry(OrbitPhysicalPropertiesKey.INITIAL_WET_MASS.name(), phys.getInitialWetMass(), Unit.KILOGRAM, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.WET_MASS.name(),         phys.getWetMass(), Unit.KILOGRAM,        false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.DRY_MASS.name(),         phys.getDryMass(), Unit.KILOGRAM,        false);

        // Optimally Enclosing Box
        generator.writeEntry(OrbitPhysicalPropertiesKey.OEB_PARENT_FRAME.name(),       phys.getOebParentFrame().getName(),           null, false);
        if (!phys.getOebParentFrameEpoch().equals(timeConverter.getReferenceDate()) &&
            phys.getOebParentFrame().asOrbitRelativeFrame() == null &&
            phys.getOebParentFrame().asSpacecraftBodyFrame() == null) {
            generator.writeEntry(OrbitPhysicalPropertiesKey.OEB_PARENT_FRAME_EPOCH.name(), timeConverter, phys.getOebParentFrameEpoch(), true, false);
        }
        generator.writeEntry(OrbitPhysicalPropertiesKey.OEB_Q1.name(),                 phys.getOebQ().getQ1(), Unit.ONE,                   false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.OEB_Q2.name(),                 phys.getOebQ().getQ2(), Unit.ONE,                   false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.OEB_Q3.name(),                 phys.getOebQ().getQ3(), Unit.ONE,                   false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.OEB_QC.name(),                 phys.getOebQ().getQ0(), Unit.ONE,                   false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.OEB_MAX.name(),                phys.getOebMax(), Unit.METRE,                       false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.OEB_INT.name(),                phys.getOebIntermediate(), Unit.METRE,              false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.OEB_MIN.name(),                phys.getOebMin(), Unit.METRE,                       false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.AREA_ALONG_OEB_MAX.name(),     phys.getOebAreaAlongMax(), Units.M2,                false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.AREA_ALONG_OEB_INT.name(),     phys.getOebAreaAlongIntermediate(), Units.M2,       false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.AREA_ALONG_OEB_MIN.name(),     phys.getOebAreaAlongMin(), Units.M2,                false);

        // collision probability
        generator.writeEntry(OrbitPhysicalPropertiesKey.AREA_MIN_FOR_PC.name(), phys.getMinAreaForCollisionProbability(), Units.M2, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.AREA_MAX_FOR_PC.name(), phys.getMaxAreaForCollisionProbability(), Units.M2, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.AREA_TYP_FOR_PC.name(), phys.getTypAreaForCollisionProbability(), Units.M2, false);

        // radar cross section
        generator.writeEntry(OrbitPhysicalPropertiesKey.RCS.name(),     phys.getRcs(), Units.M2,    false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.RCS_MIN.name(), phys.getMinRcs(), Units.M2, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.RCS_MAX.name(), phys.getMaxRcs(), Units.M2, false);

        // solar radiation pressure
        generator.writeEntry(OrbitPhysicalPropertiesKey.SRP_CONST_AREA.name(),        phys.getSrpConstantArea(), Units.M2,    false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.SOLAR_RAD_COEFF.name(),       phys.getSrpCoefficient(), Unit.ONE,     false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.SOLAR_RAD_UNCERTAINTY.name(), phys.getSrpUncertainty(), Unit.PERCENT, false);

        // visual magnitude
        generator.writeEntry(OrbitPhysicalPropertiesKey.VM_ABSOLUTE.name(),     phys.getVmAbsolute(),    Unit.ONE, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.VM_APPARENT_MIN.name(), phys.getVmApparentMin(), Unit.ONE, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.VM_APPARENT.name(),     phys.getVmApparent(),    Unit.ONE, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.VM_APPARENT_MAX.name(), phys.getVmApparentMax(), Unit.ONE, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.REFLECTANCE.name(),     phys.getReflectance(),   Unit.ONE, false);

        // attitude
        generator.writeEntry(OrbitPhysicalPropertiesKey.ATT_CONTROL_MODE.name(),  phys.getAttitudeControlMode(),       null,        false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.ATT_ACTUATOR_TYPE.name(), phys.getAttitudeActuatorType(),      null,        false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.ATT_KNOWLEDGE.name(),     phys.getAttitudeKnowledgeAccuracy(), Unit.DEGREE, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.ATT_CONTROL.name(),       phys.getAttitudeControlAccuracy(),   Unit.DEGREE, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.ATT_POINTING.name(),      phys.getAttitudePointingAccuracy(),  Unit.DEGREE, false);

        // maneuvers
        generator.writeEntry(OrbitPhysicalPropertiesKey.AVG_MANEUVER_FREQ.name(), phys.getManeuversFrequency(), Units.NB_PER_Y, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.MAX_THRUST.name(),        phys.getMaxThrust(),          Unit.NEWTON,    false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.DV_BOL.name(),            phys.getBolDv(),              Units.KM_PER_S, false);
        generator.writeEntry(OrbitPhysicalPropertiesKey.DV_REMAINING.name(),      phys.getRemainingDv(),        Units.KM_PER_S, false);

        // inertia
        final RealMatrix inertia = phys.getInertiaMatrix();
        if (inertia != null) {
            generator.writeEntry(OrbitPhysicalPropertiesKey.IXX.name(), inertia.getEntry(0, 0), Units.KG_M2, true);
            generator.writeEntry(OrbitPhysicalPropertiesKey.IYY.name(), inertia.getEntry(1, 1), Units.KG_M2, true);
            generator.writeEntry(OrbitPhysicalPropertiesKey.IZZ.name(), inertia.getEntry(2, 2), Units.KG_M2, true);
            generator.writeEntry(OrbitPhysicalPropertiesKey.IXY.name(), inertia.getEntry(0, 1), Units.KG_M2, true);
            generator.writeEntry(OrbitPhysicalPropertiesKey.IXZ.name(), inertia.getEntry(0, 2), Units.KG_M2, true);
            generator.writeEntry(OrbitPhysicalPropertiesKey.IYZ.name(), inertia.getEntry(1, 2), Units.KG_M2, true);
        }

    }

}
