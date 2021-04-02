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
class PhysicalPropertiesWriter extends AbstractWriter {

    /** Physical properties block. */
    private final PhysicalProperties phys;

    /** Converter for dates. */
    private final TimeConverter timeConverter;

    /** Create a writer.
     * @param phys physical properties to write
     * @param timeConverter converter for dates
     */
    PhysicalPropertiesWriter(final PhysicalProperties phys, final TimeConverter timeConverter) {
        super(OcmDataSubStructureKey.phys.name(), OcmDataSubStructureKey.PHYS.name());
        this.phys          = phys;
        this.timeConverter = timeConverter;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        // physical properties block
        generator.writeComments(phys.getComments());

        generator.writeEntry(PhysicalPropertiesKey.MANUFACTURER.name(), phys.getManufacturer(), false);
        generator.writeEntry(PhysicalPropertiesKey.BUS_MODEL.name(),    phys.getBusModel(),     false);
        generator.writeEntry(PhysicalPropertiesKey.DOCKED_WITH.name(),  phys.getDockedWith(),   false);

        // drag
        generator.writeEntry(PhysicalPropertiesKey.DRAG_CONST_AREA.name(),  Units.M2.fromSI(phys.getDragConstantArea()),        false);
        generator.writeEntry(PhysicalPropertiesKey.DRAG_COEFF_NOM.name(),   Unit.ONE.fromSI(phys.getNominalDragCoefficient()),  false);
        generator.writeEntry(PhysicalPropertiesKey.DRAG_UNCERTAINTY.name(), Unit.PERCENT.fromSI(phys.getDragUncertainty()),     false);

        // mass
        generator.writeEntry(PhysicalPropertiesKey.INITIAL_WET_MASS.name(), Unit.KILOGRAM.fromSI(phys.getInitialWetMass()), false);
        generator.writeEntry(PhysicalPropertiesKey.WET_MASS.name(),         Unit.KILOGRAM.fromSI(phys.getWetMass()),        false);
        generator.writeEntry(PhysicalPropertiesKey.DRY_MASS.name(),         Unit.KILOGRAM.fromSI(phys.getDryMass()),        false);

        // Optimally Enclosing Box
        generator.writeEntry(PhysicalPropertiesKey.OEB_PARENT_FRAME.name(),       phys.getOebParentFrame().getName(),                  false);
        generator.writeEntry(PhysicalPropertiesKey.OEB_PARENT_FRAME_EPOCH.name(), timeConverter, phys.getOebParentFrameEpoch(),        false);
        generator.writeEntry(PhysicalPropertiesKey.OEB_Q1.name(),                 Unit.ONE.fromSI(phys.getOebQ().getQ1()),             false);
        generator.writeEntry(PhysicalPropertiesKey.OEB_Q2.name(),                 Unit.ONE.fromSI(phys.getOebQ().getQ2()),             false);
        generator.writeEntry(PhysicalPropertiesKey.OEB_Q3.name(),                 Unit.ONE.fromSI(phys.getOebQ().getQ3()),             false);
        generator.writeEntry(PhysicalPropertiesKey.OEB_QC.name(),                 Unit.ONE.fromSI(phys.getOebQ().getQ0()),             false);
        generator.writeEntry(PhysicalPropertiesKey.OEB_MAX.name(),                Unit.METRE.fromSI(phys.getOebMax()),                 false);
        generator.writeEntry(PhysicalPropertiesKey.OEB_INT.name(),                Unit.METRE.fromSI(phys.getOebIntermediate()),        false);
        generator.writeEntry(PhysicalPropertiesKey.OEB_MIN.name(),                Unit.METRE.fromSI(phys.getOebMin()),                 false);
        generator.writeEntry(PhysicalPropertiesKey.AREA_ALONG_OEB_MAX.name(),     Units.M2.fromSI(phys.getOebAreaAlongMax()),          false);
        generator.writeEntry(PhysicalPropertiesKey.AREA_ALONG_OEB_INT.name(),     Units.M2.fromSI(phys.getOebAreaAlongIntermediate()), false);
        generator.writeEntry(PhysicalPropertiesKey.AREA_ALONG_OEB_MIN.name(),     Units.M2.fromSI(phys.getOebAreaAlongMin()),          false);

        // collision probability
        generator.writeEntry(PhysicalPropertiesKey.AREA_MIN_FOR_PC.name(), Units.M2.fromSI(phys.getMinAreaForCollisionProbability()), false);
        generator.writeEntry(PhysicalPropertiesKey.AREA_MAX_FOR_PC.name(), Units.M2.fromSI(phys.getMaxAreaForCollisionProbability()), false);
        generator.writeEntry(PhysicalPropertiesKey.AREA_TYP_FOR_PC.name(), Units.M2.fromSI(phys.getTypAreaForCollisionProbability()), false);

        // radar cross section
        generator.writeEntry(PhysicalPropertiesKey.RCS.name(),     Units.M2.fromSI(phys.getRcs()),    false);
        generator.writeEntry(PhysicalPropertiesKey.RCS_MIN.name(), Units.M2.fromSI(phys.getMinRcs()), false);
        generator.writeEntry(PhysicalPropertiesKey.RCS_MAX.name(), Units.M2.fromSI(phys.getMaxRcs()), false);

        // solar radiation pressure
        generator.writeEntry(PhysicalPropertiesKey.SRP_CONST_AREA.name(),        Units.M2.fromSI(phys.getSrpConstantArea()),        false);
        generator.writeEntry(PhysicalPropertiesKey.SOLAR_RAD_COEFF.name(),       Unit.ONE.fromSI(phys.getNominalSrpCoefficient()),  false);
        generator.writeEntry(PhysicalPropertiesKey.SOLAR_RAD_UNCERTAINTY.name(), Unit.PERCENT.fromSI(phys.getSrpUncertainty()),     false);

        // visual magnitude
        generator.writeEntry(PhysicalPropertiesKey.VM_ABSOLUTE.name(),     Unit.ONE.fromSI(phys.getVmAbsolute()),    false);
        generator.writeEntry(PhysicalPropertiesKey.VM_APPARENT_MIN.name(), Unit.ONE.fromSI(phys.getVmApparentMin()), false);
        generator.writeEntry(PhysicalPropertiesKey.VM_APPARENT.name(),     Unit.ONE.fromSI(phys.getVmApparent()),    false);
        generator.writeEntry(PhysicalPropertiesKey.VM_APPARENT_MAX.name(), Unit.ONE.fromSI(phys.getVmApparentMax()), false);
        generator.writeEntry(PhysicalPropertiesKey.REFLECTIVITY.name(),    Unit.ONE.fromSI(phys.getReflectivity()),  false);

        // attitude
        generator.writeEntry(PhysicalPropertiesKey.ATT_CONTROL_MODE.name(),  phys.getAttitudeControlMode(),                           false);
        generator.writeEntry(PhysicalPropertiesKey.ATT_ACTUATOR_TYPE.name(), phys.getAttitudeActuatorType(),                          false);
        generator.writeEntry(PhysicalPropertiesKey.ATT_KNOWLEDGE.name(),     Unit.DEGREE.fromSI(phys.getAttitudeKnowledgeAccuracy()), false);
        generator.writeEntry(PhysicalPropertiesKey.ATT_CONTROL.name(),       Unit.DEGREE.fromSI(phys.getAttitudeControlAccuracy()),   false);
        generator.writeEntry(PhysicalPropertiesKey.ATT_POINTING.name(),      Unit.DEGREE.fromSI(phys.getAttitudePointingAccuracy()),  false);

        // maneuvers
        generator.writeEntry(PhysicalPropertiesKey.AVG_MANEUVER_FREQ.name(), Units.NB_PER_Y.fromSI(phys.getManeuversPerYear()), false);
        generator.writeEntry(PhysicalPropertiesKey.MAX_THRUST.name(),        Unit.NEWTON.fromSI(phys.getMaxThrust()),           false);
        generator.writeEntry(PhysicalPropertiesKey.DV_BOL.name(),            Units.KM_PER_S.fromSI(phys.getBolDv()),            false);
        generator.writeEntry(PhysicalPropertiesKey.DV_REMAINING.name(),      Units.KM_PER_S.fromSI(phys.getRemainingDv()),      false);

        // inertia
        final RealMatrix inertia = phys.getInertiaMatrix();
        if (inertia != null) {
            generator.writeEntry(PhysicalPropertiesKey.IXX.name(), Units.KG_M2.fromSI(inertia.getEntry(0, 0)), true);
            generator.writeEntry(PhysicalPropertiesKey.IYY.name(), Units.KG_M2.fromSI(inertia.getEntry(1, 1)), true);
            generator.writeEntry(PhysicalPropertiesKey.IZZ.name(), Units.KG_M2.fromSI(inertia.getEntry(2, 2)), true);
            generator.writeEntry(PhysicalPropertiesKey.IXY.name(), Units.KG_M2.fromSI(inertia.getEntry(0, 1)), true);
            generator.writeEntry(PhysicalPropertiesKey.IXZ.name(), Units.KG_M2.fromSI(inertia.getEntry(0, 2)), true);
            generator.writeEntry(PhysicalPropertiesKey.IYZ.name(), Units.KG_M2.fromSI(inertia.getEntry(1, 2)), true);
        }

    }

}
