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
import org.orekit.utils.units.PredefinedUnit;
import org.orekit.utils.units.Unit;

/** Maneuver field type used in CCSDS {@link OcmFile Orbit Comprehensive Messages}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum ManeuverFieldType {

    // CHECKSTYLE: stop MultipleStringLiterals check

    /** Absolute epoch time. */
    TIME_ABSOLUTE("n/a"),

    /** Relative epoch time. */
    TIME_RELATIVE("s"),

    /** Maneuver duration. */
    MAN_DURA("s"),

    /** Mass change. */
    DELTA_MASS("kg"),

    /** Acceleration along X axis. */
    ACC_X("km/s²"),

    /** Acceleration along Y axis. */
    ACC_Y("km/s²"),

    /** Acceleration along Z axis. */
    ACC_Z("km/s²"),

    /** Interpolation mode between current and next acceleration line. */
    ACC_INTERP("n/a"),

    /** One σ percent error on acceleration magnitude. */
    ACC_SIGMA("%"),

    /** Velocity increment along X axis. */
    DV_X("km/s"),

    /** Velocity increment along Y axis. */
    DV_Y("km/s"),

    /** Velocity increment along Z axis. */
    DV_Z("km/s"),

    /** One σ percent error on velocity magnitude. */
    DV_SIGMA("%"),

    /** Thrust component along X axis. */
    THR_X("N"),

    /** Thrust component along Y axis. */
    THR_Y("N"),

    /** Thrust component along Z axis. */
    THR_Z("N"),

    /** Thrust efficiency η typically between 0.0 and 1.0. */
    THR_EFFIC("n/a"),

    /** Interpolation mode between current and next acceleration line. */
    THR_INTERP("n/a"),

    /** Thrust specific impulse. */
    THR_ISP("s"),

    /** One σ percent error on thrust magnitude. */
    THR_SIGMA("%"),

    /** Identifier of resulting "child" object deployed from this host. */
    DEPLOY_ID("n/a"),

    /** Velocity increment of deployed "child" object along X axis. */
    DEPLOY_DV_X("km/s"),

    /** Velocity increment of deployed "child" object along Y axis. */
    DEPLOY_DV_Y("km/s"),

    /** Velocity increment of deployed "child" object along Z axis. */
    DEPLOY_DV_Z("km/s"),

    /** Decrement in host mass as a result of deployment (shall be ≤ 0). */
    DEPLOY_MASS("kg"),

    /** One σ percent error on deployment velocity magnitude. */
    DEPLOY_DV_SIGMA("%"),

    /** Ratio of child-to-host ΔV vectors. */
    DEPLOY_DV_RATIO("n/a"),

    /** Typical (50th percentile) product of drag coefficient times cross-sectional area of deployed "child" object. */
    DEPLOY_DV_CDA("n/a");

    // CHECKSTYLE: resume MultipleStringLiterals check

    /** Elements units. */
    private final Unit unit;

    /** Simple constructor.
     * @param unitSpecifications field unit specifications
     */
    ManeuverFieldType(final String unitSpecifications) {
        this.unit = Unit.parse(unitSpecifications);
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

}
