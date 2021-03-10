/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.parsing.ParsingContext;
import org.orekit.utils.units.PredefinedUnit;
import org.orekit.utils.units.Unit;

/** Maneuver field type used in CCSDS {@link OcmFile Orbit Comprehensive Messages}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum ManeuverFieldType {

    // CHECKSTYLE: stop MultipleStringLiterals check

    /** Absolute epoch time. */
    TIME_ABSOLUTE("n/a",
        (field, u, context, maneuver) -> maneuver.setDate(context.getTimeSystem().parse(field))),

    /** Relative epoch time. */
    TIME_RELATIVE("s",
        (field, u, context, maneuver) -> maneuver.setDate(context.getReferenceDate().shiftedBy(toSI(field, u)))),

    /** Maneuver duration. */
    MAN_DURA("s",
        (field, u, context, maneuver) -> maneuver.setDuration(toSI(field, u))),

    /** Mass change. */
    DELTA_MASS("kg",
        (field, u, context, maneuver) -> maneuver.setDeltaMass(toSI(field, u))),

    /** Acceleration along X axis. */
    ACC_X("km/s²",
        (field, u, context, maneuver) -> maneuver.setAcceleration(0, toSI(field, u))),

    /** Acceleration along Y axis. */
    ACC_Y("km/s²",
        (field, u, context, maneuver) -> maneuver.setAcceleration(1, toSI(field, u))),

    /** Acceleration along Z axis. */
    ACC_Z("km/s²",
        (field, u, context, maneuver) -> maneuver.setAcceleration(2, toSI(field, u))),

    /** Interpolation mode between current and next acceleration line. */
    ACC_INTERP("n/a",
        (field, u, context, maneuver) -> maneuver.setAccelerationInterpolation(field)),

    /** One σ percent error on acceleration magnitude. */
    ACC_SIGMA("%",
        (field, u, context, maneuver) -> maneuver.setAccelerationSigma(toSI(field, u))),

    /** Velocity increment along X axis. */
    DV_X("km/s",
        (field, u, context, maneuver) -> maneuver.setDv(0, toSI(field, u))),

    /** Velocity increment along Y axis. */
    DV_Y("km/s",
        (field, u, context, maneuver) -> maneuver.setDv(1, toSI(field, u))),

    /** Velocity increment along Z axis. */
    DV_Z("km/s",
        (field, u, context, maneuver) -> maneuver.setDv(2, toSI(field, u))),

    /** One σ percent error on velocity magnitude. */
    DV_SIGMA("%",
        (field, u, context, maneuver) -> maneuver.setDvSigma(toSI(field, u))),

    /** Thrust component along X axis. */
    THR_X("N",
        (field, u, context, maneuver) -> maneuver.setThrust(0, toSI(field, u))),

    /** Thrust component along Y axis. */
    THR_Y("N",
        (field, u, context, maneuver) -> maneuver.setThrust(1, toSI(field, u))),

    /** Thrust component along Z axis. */
    THR_Z("N",
        (field, u, context, maneuver) -> maneuver.setThrust(2, toSI(field, u))),

    /** Thrust efficiency η typically between 0.0 and 1.0. */
    THR_EFFIC("n/a",
        (field, u, context, maneuver) -> maneuver.setThrustEfficiency(toSI(field, u))),

    /** Interpolation mode between current and next acceleration line. */
    THR_INTERP("n/a",
        (field, u, context, maneuver) -> maneuver.setThrustInterpolation(field)),

    /** Thrust specific impulse. */
    THR_ISP("s",
        (field, u, context, maneuver) -> maneuver.setThrustIsp(toSI(field, u))),

    /** One σ percent error on thrust magnitude. */
    THR_SIGMA("%",
        (field, u, context, maneuver) -> maneuver.setThrustSigma(toSI(field, u))),

    /** Identifier of resulting "child" object deployed from this host. */
    DEPLOY_ID("n/a",
        (field, u, context, maneuver) -> maneuver.setDeployId(field)),

    /** Velocity increment of deployed "child" object along X axis. */
    DEPLOY_DV_X("km/s",
        (field, u, context, maneuver) -> maneuver.setDeployDv(0, toSI(field, u))),

    /** Velocity increment of deployed "child" object along Y axis. */
    DEPLOY_DV_Y("km/s",
        (field, u, context, maneuver) -> maneuver.setDeployDv(1, toSI(field, u))),

    /** Velocity increment of deployed "child" object along Z axis. */
    DEPLOY_DV_Z("km/s",
        (field, u, context, maneuver) -> maneuver.setDeployDv(2, toSI(field, u))),

    /** Decrement in host mass as a result of deployment (shall be ≤ 0). */
    DEPLOY_MASS("kg",
        (field, u, context, maneuver) -> maneuver.setDeployMass(toSI(field, u))),

    /** One σ percent error on deployment velocity magnitude. */
    DEPLOY_DV_SIGMA("%",
        (field, u, context, maneuver) -> maneuver.setDeployDvSigma(toSI(field, u))),

    /** Ratio of child-to-host ΔV vectors. */
    DEPLOY_DV_RATIO("n/a",
        (field, u, context, maneuver) -> maneuver.setDeployDvRatio(toSI(field, u))),

    /** Typical (50th percentile) product of drag coefficient times cross-sectional area of deployed "child" object. */
    DEPLOY_DV_CDA("m²",
        (field, u, context, maneuver) -> maneuver.setDeployDvCda(toSI(field, u)));

    // CHECKSTYLE: resume MultipleStringLiterals check

    /** Elements units. */
    private final Unit unit;

    /** Processing method. */
    private final FieldProcessor processor;

    /** Simple constructor.
     * @param unitSpecifications field unit specifications
     * @param processor field processing method
     */
    ManeuverFieldType(final String unitSpecifications, final FieldProcessor processor) {
        this.unit      = Unit.parse(unitSpecifications);
        this.processor = processor;
    }

    /** Get the field unit.
     * @return field unit
     */
    public Unit getUnit() {
        return unit;
    }

    /** Check if parsed unit is compatible with field type.
     * @param parsedUnit unit to check
     */
    public void checkUnit(final Unit parsedUnit) {
        if ((unit == PredefinedUnit.NONE.toUnit()) ^ (parsedUnit == PredefinedUnit.NONE.toUnit()) ||
            (!(unit.sameDimension(parsedUnit) && Precision.equals(unit.getScale(), parsedUnit.getScale(), 1)))) {
            throw new OrekitException(OrekitMessages.INCOMPATIBLE_UNITS,
                                      unit.getName(), parsedUnit.getName());
        }
    }

    /** Check if a field is a time field.
     * @return true if field is a time field
     */
    public boolean isTime() {
        return this == TIME_ABSOLUTE || this == TIME_RELATIVE;
    }

    /** Get the value in SI units corresponding to a field.
     * @param field text field
     * @param unit unit to use
     * @return double value in SI units
     */
    private static double toSI(final String field, final Unit unit) {
        return unit.toSI(Double.parseDouble(field));
    }

    /** Process one field.
     * @param field field to process
     * @param context parsing context
     * @param maneuver maneuver to fill
     */
    public void process(final String field, final ParsingContext context, final Maneuver maneuver) {
        processor.process(field, unit, context, maneuver);
    }

    /** Interface for processing one field. */
    interface FieldProcessor {
        /** Process one field.
         * @param field field to process
         * @param unit unit to use
         * @param context parsing context
         * @param maneuver maneuver to fill
         */
        void process(String field, Unit unit, ParsingContext context, Maneuver maneuver);
    }

}
