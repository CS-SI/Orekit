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

import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.OnOff;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.time.DateTimeComponents;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.units.Unit;

/** Maneuver field type used in CCSDS {@link Ocm Orbit Comprehensive Messages}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum ManeuverFieldType {

    // CHECKSTYLE: stop MultipleStringLiterals check

    /** Absolute epoch time. */
    TIME_ABSOLUTE("n/a",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDate(context.getTimeSystem().getConverter(context).parse(field)),
        (u, converter, maneuver) -> {
            final DateTimeComponents dt = converter.components(maneuver.getDate());
            return AccurateFormatter.format(dt.getDate().getYear(), dt.getDate().getMonth(), dt.getDate().getDay(),
                                            dt.getTime().getHour(), dt.getTime().getMinute(), dt.getTime().getSecond());
        }),

    /** Relative epoch time. */
    TIME_RELATIVE("s",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDate(context.getReferenceDate().shiftedBy(toSI(field, u))),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(converter.offset(maneuver.getDate())))),

    /** Maneuver duration. */
    MAN_DURA("s",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDuration(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDuration()))),

    /** Mass change. */
    DELTA_MASS("kg",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDeltaMass(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDeltaMass()))),

    /** Acceleration along X axis. */
    ACC_X("km/s²",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setAcceleration(0, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getAcceleration().getX()))),

    /** Acceleration along Y axis. */
    ACC_Y("km/s²",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setAcceleration(1, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getAcceleration().getY()))),

    /** Acceleration along Z axis. */
    ACC_Z("km/s²",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setAcceleration(2, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getAcceleration().getZ()))),

    /** Interpolation mode between current and next acceleration line. */
    ACC_INTERP("n/a",
        (field, u, context, maneuver, lineNumber, fileName) -> {
            try {
                maneuver.setAccelerationInterpolation(OnOff.valueOf(field));
            } catch (IllegalArgumentException iae) {
                throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD,
                                          lineNumber, fileName, field);
            }
        },
        (u, converter, maneuver) -> maneuver.getAccelerationInterpolation().name()),

    /** One σ percent error on acceleration magnitude. */
    ACC_MAG_SIGMA("%",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setAccelerationMagnitudeSigma(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getAccelerationMagnitudeSigma()))),

    /** One σ off-nominal acceleration direction. */
    ACC_DIR_SIGMA("°",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setAccelerationDirectionSigma(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getAccelerationDirectionSigma()))),

    /** Velocity increment along X axis. */
    DV_X("km/s",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDv(0, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDv().getX()))),

    /** Velocity increment along Y axis. */
    DV_Y("km/s",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDv(1, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDv().getY()))),

    /** Velocity increment along Z axis. */
    DV_Z("km/s",
        (field, u, context, maneuver, lineNumber, fileName) ->  maneuver.setDv(2, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDv().getZ()))),

    /** One σ percent error on ΔV magnitude. */
    DV_MAG_SIGMA("%",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDvMagSigma(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDvMagSigma()))),

    /** One σ angular off-nominal ΔV direction. */
    DV_DIR_SIGMA("°",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDvDirSigma(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDvDirSigma()))),

    /** Thrust component along X axis. */
    THR_X("N",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setThrust(0, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getThrust().getX()))),

    /** Thrust component along Y axis. */
    THR_Y("N",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setThrust(1, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getThrust().getY()))),

    /** Thrust component along Z axis. */
    THR_Z("N",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setThrust(2, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getThrust().getZ()))),

    /** Thrust efficiency η typically between 0.0 and 1.0. */
    THR_EFFIC("n/a",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setThrustEfficiency(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getThrustEfficiency()))),

    /** Interpolation mode between current and next acceleration line. */
    THR_INTERP("n/a",
        (field, u, context, maneuver, lineNumber, fileName) -> {
            try {
                maneuver.setThrustInterpolation(OnOff.valueOf(field));
            } catch (IllegalArgumentException iae) {
                throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD,
                                          lineNumber, fileName, field);
            }
        },
        (u, converter, maneuver) -> maneuver.getThrustInterpolation().name()),

    /** Thrust specific impulse. */
    THR_ISP("s",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setThrustIsp(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getThrustIsp()))),

    /** One σ percent error on thrust magnitude. */
    THR_MAG_SIGMA("%",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setThrustMagnitudeSigma(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getThrustMagnitudeSigma()))),

    /** One σ angular off-nominal thrust direction. */
    THR_DIR_SIGMA("°",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setThrustDirectionSigma(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getThrustDirectionSigma()))),

    /** Identifier of resulting "child" object deployed from this host. */
    DEPLOY_ID("n/a",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDeployId(field),
        (u, converter, maneuver) -> maneuver.getDeployId()),

    /** Velocity increment of deployed "child" object along X axis. */
    DEPLOY_DV_X("km/s",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDeployDv(0, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDeployDv().getX()))),

    /** Velocity increment of deployed "child" object along Y axis. */
    DEPLOY_DV_Y("km/s",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDeployDv(1, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDeployDv().getY()))),

    /** Velocity increment of deployed "child" object along Z axis. */
    DEPLOY_DV_Z("km/s",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDeployDv(2, toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDeployDv().getZ()))),

    /** Decrement in host mass as a result of deployment (shall be ≤ 0). */
    DEPLOY_MASS("kg",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDeployMass(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDeployMass()))),

    /** One σ percent error on deployment ΔV magnitude. */
    DEPLOY_DV_SIGMA("%",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDeployDvSigma(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDeployDvSigma()))),

    /** One σ angular off-nominal deployment vector direction. */
    DEPLOY_DIR_SIGMA("°",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDeployDirSigma(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDeployDirSigma()))),

    /** Ratio of child-to-host ΔV vectors. */
    DEPLOY_DV_RATIO("n/a",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDeployDvRatio(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDeployDvRatio()))),

    /** Typical (50th percentile) product of drag coefficient times cross-sectional area of deployed "child" object. */
    DEPLOY_DV_CDA("m²",
        (field, u, context, maneuver, lineNumber, fileName) -> maneuver.setDeployDvCda(toSI(field, u)),
        (u, converter, maneuver) -> AccurateFormatter.format(u.fromSI(maneuver.getDeployDvCda())));

    // CHECKSTYLE: resume MultipleStringLiterals check

    /** Elements units. */
    private final Unit unit;

    /** Processing method. */
    private final transient FieldProcessor processor;

    /** Writing method. */
    private final transient FieldWriter writer;

    /** Simple constructor.
     * @param unitSpecifications field unit specifications
     * @param processor field processing method
     * @param writer field writing method
     */
    ManeuverFieldType(final String unitSpecifications, final FieldProcessor processor, final FieldWriter writer) {
        this.unit      = Unit.parse(unitSpecifications);
        this.processor = processor;
        this.writer    = writer;
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
        if (unit == Unit.NONE ^ parsedUnit == Unit.NONE ||
            !(unit.sameDimension(parsedUnit) && Precision.equals(unit.getScale(), parsedUnit.getScale(), 1))) {
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
     * @param context context binding
     * @param maneuver maneuver to fill
     * @param lineNumber line number at which the field occurs
     * @param fileName name of the file in which the field occurs
     */
    public void process(final String field, final ContextBinding context, final OrbitManeuver maneuver,
                        final int lineNumber, final String fileName) {
        processor.process(field, unit, context, maneuver, lineNumber, fileName);
    }

    /** Output one maneuver field.
         * @param converter converter for dates
     * @param maneuver maneuver containing the field to output
     * @return output field
     */
    public String outputField(final TimeConverter converter, final OrbitManeuver maneuver) {
        return writer.output(unit, converter, maneuver);
    }

    /** Interface for processing one field. */
    interface FieldProcessor {
        /** Process one field.
         * @param field field to process
         * @param unit unit to use
         * @param context context binding
         * @param maneuver maneuver to fill
         * @param lineNumber line number at which the field occurs
         * @param fileName name of the file in which the field occurs
         */
        void process(String field, Unit unit, ContextBinding context, OrbitManeuver maneuver,
                     int lineNumber, String fileName);
    }

    /** Interface for writing one field. */
    interface FieldWriter {
        /** Process one field.
         * @param unit unit to use
         * @param converter converter for dates
         * @param maneuver maneuver containing the field to output
         * @return output field
         */
        String output(Unit unit, TimeConverter converter, OrbitManeuver maneuver);
    }

}
