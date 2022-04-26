/* Copyright 2002-2022 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm;

import java.util.regex.Pattern;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AccurateFormatter;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.units.Unit;

/** Enumerate for ADM attitude type.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum AttitudeType {

    /** Quaternion. */
    QUATERNION("QUATERNION", AngularDerivativesFilter.USE_R,
               Unit.ONE, Unit.ONE, Unit.ONE, Unit.ONE) {

        /** {@inheritDoc} */
        @Override
        public String[] createDataFields(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                         final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                         final TimeStampedAngularCoordinates coordinates) {

            // Initialize the array of attitude data
            final double[] data = new double[4];

            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3} : new int[] {3, 0, 1, 2};

            // Fill the array
            Rotation rotation  = coordinates.getRotation();
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }
            data[quaternionIndex[0]] = rotation.getQ0();
            data[quaternionIndex[1]] = rotation.getQ1();
            data[quaternionIndex[2]] = rotation.getQ2();
            data[quaternionIndex[3]] = rotation.getQ3();

            // Convert units and format
            return QUATERNION.formatData(data);

        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates build(final boolean isFirst,
                                                   final boolean isExternal2SpacecraftBody,
                                                   final RotationOrder eulerRotSequence,
                                                   final boolean isSpacecraftBodyRate,
                                                   final AbsoluteDate date,
                                                   final double... components) {

            Rotation rotation = isFirst ?
                                new Rotation(components[0], components[1], components[2], components[3], true) :
                                new Rotation(components[3], components[0], components[1], components[2], true);
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }

            // Return
            return new TimeStampedAngularCoordinates(date, rotation, Vector3D.ZERO, Vector3D.ZERO);

        }

    },

    /** Quaternion and derivatives. */
    QUATERNION_DERIVATIVE("QUATERNION/DERIVATIVE", AngularDerivativesFilter.USE_RR,
                          Unit.ONE, Unit.ONE, Unit.ONE, Unit.ONE,
                          Units.ONE_PER_S, Units.ONE_PER_S, Units.ONE_PER_S, Units.ONE_PER_S) {

        /** {@inheritDoc} */
        @Override
        public String[] createDataFields(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                         final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                         final TimeStampedAngularCoordinates coordinates) {

            // Initialize the array of attitude data
            final double[] data = new double[8];

            FieldRotation<UnivariateDerivative1> rotation = coordinates.toUnivariateDerivative1Rotation();
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }

            // Data index
            final int[] quaternionIndex = isFirst ?
                                          new int[] {0, 1, 2, 3, 4, 5, 6, 7} :
                                          new int[] {3, 0, 1, 2, 7, 4, 5, 6};

            // Fill the array
            data[quaternionIndex[0]] = rotation.getQ0().getValue();
            data[quaternionIndex[1]] = rotation.getQ1().getValue();
            data[quaternionIndex[2]] = rotation.getQ2().getValue();
            data[quaternionIndex[3]] = rotation.getQ3().getValue();
            data[quaternionIndex[4]] = rotation.getQ0().getFirstDerivative();
            data[quaternionIndex[5]] = rotation.getQ1().getFirstDerivative();
            data[quaternionIndex[6]] = rotation.getQ2().getFirstDerivative();
            data[quaternionIndex[7]] = rotation.getQ3().getFirstDerivative();

            // Convert units and format
            return QUATERNION_DERIVATIVE.formatData(data);

        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates build(final boolean isFirst,
                                                   final boolean isExternal2SpacecraftBody,
                                                   final RotationOrder eulerRotSequence,
                                                   final boolean isSpacecraftBodyRate,
                                                   final AbsoluteDate date,
                                                   final double... components) {
            FieldRotation<UnivariateDerivative1> rotation =
                            isFirst ?
                            new FieldRotation<>(new UnivariateDerivative1(components[0], components[4]),
                                                new UnivariateDerivative1(components[1], components[5]),
                                                new UnivariateDerivative1(components[2], components[6]),
                                                new UnivariateDerivative1(components[3], components[7]),
                                                true) :
                            new FieldRotation<>(new UnivariateDerivative1(components[3], components[7]),
                                                new UnivariateDerivative1(components[0], components[4]),
                                                new UnivariateDerivative1(components[1], components[5]),
                                                new UnivariateDerivative1(components[2], components[6]),
                                                true);
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }

            return new TimeStampedAngularCoordinates(date, rotation);

        }

    },

    /** Quaternion and rotation rate. */
    QUATERNION_RATE("QUATERNION/RATE", AngularDerivativesFilter.USE_RR,
                    Unit.ONE, Unit.ONE, Unit.ONE, Unit.ONE,
                    Units.DEG_PER_S, Units.DEG_PER_S, Units.DEG_PER_S) {

        /** {@inheritDoc} */
        @Override
        public String[] createDataFields(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                         final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                         final TimeStampedAngularCoordinates coordinates) {

            // Initialize the array of attitude data
            final double[] data = new double[7];

            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3} : new int[] {3, 0, 1, 2};

            // Attitude
            final TimeStampedAngularCoordinates c = isExternal2SpacecraftBody ? coordinates : coordinates.revert();
            final Vector3D rotationRate = QUATERNION_RATE.metadataRate(isSpacecraftBodyRate, c.getRotationRate(), c.getRotation());

            // Fill the array
            data[quaternionIndex[0]] = c.getRotation().getQ0();
            data[quaternionIndex[1]] = c.getRotation().getQ1();
            data[quaternionIndex[2]] = c.getRotation().getQ2();
            data[quaternionIndex[3]] = c.getRotation().getQ3();
            data[4] = rotationRate.getX();
            data[5] = rotationRate.getY();
            data[6] = rotationRate.getZ();

            // Convert units and format
            return QUATERNION_RATE.formatData(data);

        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates build(final boolean isFirst,
                                                   final boolean isExternal2SpacecraftBody,
                                                   final RotationOrder eulerRotSequence,
                                                   final boolean isSpacecraftBodyRate,
                                                   final AbsoluteDate date,
                                                   final double... components) {
            // Build the needed objects
            final Rotation rotation = isFirst ?
                                      new Rotation(components[0], components[1], components[2], components[3], true) :
                                      new Rotation(components[3], components[0], components[1], components[2], true);
            final Vector3D rotationRate = QUATERNION_RATE.orekitRate(isSpacecraftBodyRate,
                                                                     new Vector3D(components[4],
                                                                                  components[5],
                                                                                  components[6]),
                                                                     rotation);

            // Return
            final TimeStampedAngularCoordinates ac =
                            new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);
            return isExternal2SpacecraftBody ? ac : ac.revert();

        }

    },

    /** Euler angles. */
    EULER_ANGLE("EULER ANGLE", AngularDerivativesFilter.USE_R,
                Unit.DEGREE, Unit.DEGREE, Unit.DEGREE) {

        /** {@inheritDoc} */
        @Override
        public String[] createDataFields(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                         final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                         final TimeStampedAngularCoordinates coordinates) {

            // Attitude
            Rotation rotation = coordinates.getRotation();
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }

            final double[] data = rotation.getAngles(eulerRotSequence, RotationConvention.FRAME_TRANSFORM);

            // Convert units and format
            return EULER_ANGLE.formatData(data);

        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates build(final boolean isFirst,
                                                   final boolean isExternal2SpacecraftBody,
                                                   final RotationOrder eulerRotSequence,
                                                   final boolean isSpacecraftBodyRate,
                                                   final AbsoluteDate date,
                                                   final double... components) {

            // Build the needed objects
            Rotation rotation = new Rotation(eulerRotSequence, RotationConvention.FRAME_TRANSFORM,
                                             components[0], components[1], components[2]);
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }

            // Return
            return new TimeStampedAngularCoordinates(date, rotation, Vector3D.ZERO, Vector3D.ZERO);
        }

    },

    /** Euler angles and rotation rate. */
    EULER_ANGLE_RATE("EULER ANGLE/RATE", AngularDerivativesFilter.USE_RR,
                     Unit.DEGREE, Unit.DEGREE, Unit.DEGREE,
                     Units.DEG_PER_S, Units.DEG_PER_S, Units.DEG_PER_S) {

        /** {@inheritDoc} */
        @Override
        public String[] createDataFields(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                         final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                         final TimeStampedAngularCoordinates coordinates) {

            // Initialize the array of attitude data
            final double[] data = new double[6];

            // Attitude
            final TimeStampedAngularCoordinates c = isExternal2SpacecraftBody ? coordinates : coordinates.revert();
            final Vector3D rotationRate = EULER_ANGLE_RATE.metadataRate(isSpacecraftBodyRate, c.getRotationRate(), c.getRotation());
            final double[] angles       = c.getRotation().getAngles(eulerRotSequence, RotationConvention.FRAME_TRANSFORM);

            // Fill the array
            data[0] = angles[0];
            data[1] = angles[1];
            data[2] = angles[2];
            data[3] = Vector3D.dotProduct(rotationRate, eulerRotSequence.getA1());
            data[4] = Vector3D.dotProduct(rotationRate, eulerRotSequence.getA2());
            data[5] = Vector3D.dotProduct(rotationRate, eulerRotSequence.getA3());

            // Convert units and format
            return EULER_ANGLE_RATE.formatData(data);

        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates build(final boolean isFirst,
                                                   final boolean isExternal2SpacecraftBody,
                                                   final RotationOrder eulerRotSequence,
                                                   final boolean isSpacecraftBodyRate,
                                                   final AbsoluteDate date,
                                                   final double... components) {

            // Build the needed objects
            final Rotation rotation = new Rotation(eulerRotSequence,
                                                   RotationConvention.FRAME_TRANSFORM,
                                                   components[0],
                                                   components[1],
                                                   components[2]);
            final Vector3D rotationRate = EULER_ANGLE_RATE.orekitRate(isSpacecraftBodyRate,
                                                                      new Vector3D(components[3], eulerRotSequence.getA1(),
                                                                                   components[4], eulerRotSequence.getA2(),
                                                                                   components[5], eulerRotSequence.getA3()),
                                                                      rotation);
            // Return
            final TimeStampedAngularCoordinates ac =
                            new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);
            return isExternal2SpacecraftBody ? ac : ac.revert();

        }

    },

    /** Spin.
     * <p>
     * CCSDS enforces that spin axis is +Z, so if {@link #createDataFields(boolean, boolean, RotationOrder, boolean,
     * TimeStampedAngularCoordinates) createDataFields} is called with {@code coordinates} with {@link
     * TimeStampedAngularCoordinates#getRotationRate() rotation rate} that is not along the Z axis, result is
     * undefined.
     * </p>
     */
    SPIN("SPIN", AngularDerivativesFilter.USE_RR,
         Unit.DEGREE, Unit.DEGREE, Unit.DEGREE, Units.DEG_PER_S) {

        /** {@inheritDoc} */
        @Override
        public String[] createDataFields(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                         final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                         final TimeStampedAngularCoordinates coordinates) {

            // Initialize the array of attitude data
            final double[] data = new double[4];

            // Attitude
            final double[] angles = coordinates.getRotation().getAngles(RotationOrder.ZYZ, RotationConvention.FRAME_TRANSFORM);

            // Fill the array
            data[0] = angles[0];
            data[1] = 0.5 * FastMath.PI - angles[1];
            data[2] = angles[2];
            data[3] = coordinates.getRotationRate().getZ();

            // Convert units and format
            return SPIN.formatData(data);

        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates build(final boolean isFirst,
                                                   final boolean isExternal2SpacecraftBody,
                                                   final RotationOrder eulerRotSequence,
                                                   final boolean isSpacecraftBodyRate,
                                                   final AbsoluteDate date,
                                                   final double... components) {

            // Build the needed objects
            final Rotation rotation = new Rotation(RotationOrder.ZYZ,
                                                   RotationConvention.FRAME_TRANSFORM,
                                                   components[0],
                                                   0.5 * FastMath.PI - components[1],
                                                   components[2]);
            final Vector3D rotationRate = new Vector3D(0, 0, components[3]);

            // Return
            return new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);

        }

    },

    /** Spin and nutation. */
    SPIN_NUTATION("SPIN/NUTATION", AngularDerivativesFilter.USE_RR,
                  Unit.DEGREE, Unit.DEGREE, Unit.DEGREE, Units.DEG_PER_S,
                  Unit.DEGREE, Unit.SECOND, Unit.DEGREE) {

        /** {@inheritDoc} */
        @Override
        public String[] createDataFields(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                         final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                         final TimeStampedAngularCoordinates coordinates) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, name());
        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates build(final boolean isFirst,
                                                   final boolean isExternal2SpacecraftBody,
                                                   final RotationOrder eulerRotSequence,
                                                   final boolean isSpacecraftBodyRate,
                                                   final AbsoluteDate date,
                                                   final double... components) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, name());
        }

    };

    /** Pattern for normalizing attitude types. */
    private static final Pattern TYPE_SEPARATORS = Pattern.compile("[ _/]+");

    /** CCSDS name of the attitude type. */
    private final String ccsdsName;

    /** Derivatives filter. */
    private final AngularDerivativesFilter filter;

    /** Components units (used only for parsing). */
    private final Unit[] units;

    /** Private constructor.
     * @param ccsdsName CCSDS name of the attitude type
     * @param filter derivative filter
     * @param units components units (used only for parsing)
     */
    AttitudeType(final String ccsdsName, final AngularDerivativesFilter filter, final Unit... units) {
        this.ccsdsName = ccsdsName;
        this.filter    = filter;
        this.units     = units.clone();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return ccsdsName;
    }

    /** Parse an attitude type.
     * @param type unnormalized type name
     * @return parsed type
     */
    public static AttitudeType parseType(final String type) {
        return AttitudeType.valueOf(TYPE_SEPARATORS.matcher(type).replaceAll("_"));
    }

    /**
     * Get the attitude data fields corresponding to the attitude type.
     * <p>
     * This method returns the components in CCSDS units (i.e. degrees, degrees per seconds…).
     * </p>
     * @param isFirst if true the first quaternion component is the scalar component
     * @param isExternal2SpacecraftBody true attitude is from external frame to spacecraft body frame
     * @param eulerRotSequence sequance of Euler angles
     * @param isSpacecraftBodyRate if true Euler rates are specified in spacecraft body frame
     * @param attitude angular coordinates, using {@link Attitude Attitude} convention
     * (i.e. from inertial frame to spacecraft frame)
     * @return the attitude data in CCSDS units
     */
    public abstract String[] createDataFields(boolean isFirst, boolean isExternal2SpacecraftBody,
                                              RotationOrder eulerRotSequence, boolean isSpacecraftBodyRate,
                                              TimeStampedAngularCoordinates attitude);

    /**
     * Get the angular coordinates corresponding to the attitude data.
     * <p>
     * This method assumes the text fields are in CCSDS units and will convert to SI units.
     * </p>
     * @param isFirst if true the first quaternion component is the scalar component
     * @param isExternal2SpacecraftBody true attitude is from external frame to spacecraft body frame
     * @param eulerRotSequence sequance of Euler angles
     * @param isSpacecraftBodyRate if true Euler rates are specified in spacecraft body frame
     * @param context context binding
     * @param fields raw data fields
     * @return the angular coordinates, using {@link Attitude Attitude} convention
     * (i.e. from inertial frame to spacecraft frame)
     */
    public TimeStampedAngularCoordinates parse(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                               final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                               final ContextBinding context, final String[] fields) {

        // parse the text fields
        final AbsoluteDate date = context.getTimeSystem().getConverter(context).parse(fields[0]);
        final double[] components = new double[fields.length - 1];
        for (int i = 0; i < components.length; ++i) {
            components[i] = units[i].toSI(Double.parseDouble(fields[i + 1]));
        }

        // build the coordinates
        return build(isFirst, isExternal2SpacecraftBody, eulerRotSequence, isSpacecraftBodyRate,
                     date, components);

    }

    /** Get the angular coordinates corresponding to the attitude data.
     * @param isFirst if true the first quaternion component is the scalar component
     * @param isExternal2SpacecraftBody true attitude is from external frame to spacecraft body frame
     * @param eulerRotSequence sequance of Euler angles
     * @param isSpacecraftBodyRate if true Euler rates are specified in spacecraft body frame
     * @param date entry date
     * @param components entry components with CCSDS units (i.e. angles
     * <em>must</em> still be in degrees here), semantic depends on attitude type
     * @return the angular coordinates, using {@link Attitude Attitude} convention
     * (i.e. from inertial frame to spacecraft frame)
     */
    public abstract TimeStampedAngularCoordinates build(boolean isFirst, boolean isExternal2SpacecraftBody,
                                                        RotationOrder eulerRotSequence, boolean isSpacecraftBodyRate,
                                                        AbsoluteDate date, double... components);

    /**
     * Get the angular derivative filter corresponding to the attitude data.
     * @return the angular derivative filter corresponding to the attitude data
     */
    public AngularDerivativesFilter getAngularDerivativesFilter() {
        return filter;
    }

    private String[] formatData(final double[] data) {
        final String[] fields = new String[data.length];
        for (int i = 0; i < data.length; ++i) {
            fields[i] = AccurateFormatter.format(units[i].fromSI(data[i]));
        }
        return fields;
    }

    /** Convert a rotation rate for Orekit convention to metadata convention.
     * @param isSpacecraftBodyRate if true Euler rates are specified in spacecraft body frame
     * @param rate rotation rate from Orekit attitude
     * @param rotation corresponding rotation
     * @return rotation rate in metadata convention
     */
    private Vector3D metadataRate(final boolean isSpacecraftBodyRate, final Vector3D rate, final Rotation rotation) {
        return isSpacecraftBodyRate ? rate : rotation.applyInverseTo(rate);
    }

    /** Convert a rotation rate for metadata convention to Orekit convention.
     * @param isSpacecraftBodyRate if true Euler rates are specified in spacecraft body frame
     * @param rate rotation rate read from the data line
     * @param rotation corresponding rotation
     * @return rotation rate in Orekit convention (i.e. in spacecraft body local frame)
     */
    private Vector3D orekitRate(final boolean isSpacecraftBodyRate, final Vector3D rate, final Rotation rotation) {
        return isSpacecraftBodyRate ? rate : rotation.applyTo(rate);
    }

}
