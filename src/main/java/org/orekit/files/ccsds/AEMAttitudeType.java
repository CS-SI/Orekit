/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds;

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;

/** Enumerate for AEM attitude type.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum AEMAttitudeType {

    /** Quaternion. */
    QUATERNION("QUATERNION") {

        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates,
                                        final boolean isFirst, final RotationOrder order) {
            // Initialize the array of attitude data
            final double[] data = new double[4];

            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3} : new int[] {3, 0, 1, 2};

            // Fill the array
            final Rotation rotation  = coordinates.getRotation();
            data[quaternionIndex[0]] = rotation.getQ0();
            data[quaternionIndex[1]] = rotation.getQ1();
            data[quaternionIndex[2]] = rotation.getQ2();
            data[quaternionIndex[3]] = rotation.getQ3();

            // Return
            return data;
        }

        @Override
        public TimeStampedAngularCoordinates getAngularCoordinates(final AbsoluteDate date, final double[] data,
                                                                   final boolean isFirst, final RotationOrder order) {
            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3} : new int[] {3, 0, 1, 2};

            // Build the needed objects
            final Rotation rotation = new Rotation(data[quaternionIndex[0]],
                                                   data[quaternionIndex[1]],
                                                   data[quaternionIndex[2]],
                                                   data[quaternionIndex[3]],
                                                   false);

            // Return
            return new TimeStampedAngularCoordinates(date, rotation, Vector3D.ZERO, Vector3D.ZERO);
        }

        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_R;
        }

    },

    /** Quaternion and derivatives. */
    QUATERNION_DERIVATIVE("QUATERNION DERIVATIVE") {

        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates,
                                        final boolean isFirst, final RotationOrder order) {
            // Initialize the array of attitude data
            final double[] data = new double[8];

            final FieldRotation<UnivariateDerivative1> fieldRotation = coordinates.toUnivariateDerivative1Rotation();
            // Quaternion components
            final double q0    = fieldRotation.getQ0().getValue();
            final double q1    = fieldRotation.getQ1().getValue();
            final double q2    = fieldRotation.getQ2().getValue();
            final double q3    = fieldRotation.getQ3().getValue();
            final double q0Dot = fieldRotation.getQ0().getFirstDerivative();
            final double q1Dot = fieldRotation.getQ1().getFirstDerivative();
            final double q2Dot = fieldRotation.getQ2().getFirstDerivative();
            final double q3Dot = fieldRotation.getQ3().getFirstDerivative();

            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3, 4, 5, 6, 7} : new int[] {3, 0, 1, 2, 7, 4, 5, 6};

            // Fill the array
            data[quaternionIndex[0]] = q0;
            data[quaternionIndex[1]] = q1;
            data[quaternionIndex[2]] = q2;
            data[quaternionIndex[3]] = q3;
            data[quaternionIndex[4]] = q0Dot;
            data[quaternionIndex[5]] = q1Dot;
            data[quaternionIndex[6]] = q2Dot;
            data[quaternionIndex[7]] = q3Dot;

            // Return
            return data;
        }

        @Override
        public TimeStampedAngularCoordinates getAngularCoordinates(final AbsoluteDate date, final double[] data,
                                                                   final boolean isFirst, final RotationOrder order) {
            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3, 4, 5, 6, 7} : new int[] {3, 0, 1, 2, 7, 4, 5, 6};

            // Quaternion components
            final DSFactory factory = new DSFactory(1, 1);
            final DerivativeStructure q0DS = factory.build(data[quaternionIndex[0]], data[quaternionIndex[4]]);
            final DerivativeStructure q1DS = factory.build(data[quaternionIndex[1]], data[quaternionIndex[5]]);
            final DerivativeStructure q2DS = factory.build(data[quaternionIndex[2]], data[quaternionIndex[6]]);
            final DerivativeStructure q3DS = factory.build(data[quaternionIndex[3]], data[quaternionIndex[7]]);

            // Rotation
            final FieldRotation<DerivativeStructure> fieldRotation = new FieldRotation<>(q0DS, q1DS, q2DS, q3DS, false);

            // Return
            return new TimeStampedAngularCoordinates(date, fieldRotation);
        }

        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_RR;
        }

    },

    /** Quaternion and rotation rate. */
    QUATERNION_RATE("QUATERNION RATE") {

        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates,
                                        final boolean isFirst, final RotationOrder order) {
            // Initialize the array of attitude data
            final double[] data = new double[7];

            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3} : new int[] {3, 0, 1, 2};

            // Attitude
            final Rotation rotation     = coordinates.getRotation();
            final Vector3D rotationRate = coordinates.getRotationRate();

            // Fill the array
            data[quaternionIndex[0]] = rotation.getQ0();
            data[quaternionIndex[1]] = rotation.getQ1();
            data[quaternionIndex[2]] = rotation.getQ2();
            data[quaternionIndex[3]] = rotation.getQ3();
            data[4] = FastMath.toDegrees(rotationRate.getX());
            data[5] = FastMath.toDegrees(rotationRate.getY());
            data[6] = FastMath.toDegrees(rotationRate.getZ());

            // Return
            return data;
        }

        @Override
        public TimeStampedAngularCoordinates getAngularCoordinates(final AbsoluteDate date, final double[] data,
                                                                   final boolean isFirst, final RotationOrder order) {
            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3} : new int[] {3, 0, 1, 2};

            // Quaternion components
            final double q0    = data[quaternionIndex[0]];
            final double q1    = data[quaternionIndex[1]];
            final double q2    = data[quaternionIndex[2]];
            final double q3    = data[quaternionIndex[3]];

            // Rotation rate in radians
            final double xRate = FastMath.toRadians(data[4]);
            final double yRate = FastMath.toRadians(data[5]);
            final double zRate = FastMath.toRadians(data[6]);

            // Build the needed objects
            final Rotation rotation     = new Rotation(q0, q1, q2, q3, false);
            final Vector3D rotationRate = new Vector3D(xRate, yRate, zRate);

            // Return
            return new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);
        }

        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_RR;
        }

    },

    /** Euler angles. */
    EULER_ANGLE("EULER ANGLE") {

        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates,
                                        final boolean isFirst, final RotationOrder order) {
            // Initialize the array of attitude data
            final double[] data = new double[3];

            // Attitude
            final Rotation rotation = coordinates.getRotation();
            final double[] angles   = rotation.getAngles(order, RotationConvention.FRAME_TRANSFORM);

            // Fill the array
            data[0] = FastMath.toDegrees(angles[0]);
            data[1] = FastMath.toDegrees(angles[1]);
            data[2] = FastMath.toDegrees(angles[2]);

            // Return
            return data;
        }

        @Override
        public TimeStampedAngularCoordinates getAngularCoordinates(final AbsoluteDate date, final double[] data,
                                                                   final boolean isFirst, final RotationOrder order) {
            // Euler angles. They are given in degrees in CCSDS AEM files
            final double alpha1 = FastMath.toRadians(data[0]);
            final double alpha2 = FastMath.toRadians(data[1]);
            final double alpha3 = FastMath.toRadians(data[2]);

            // Build the needed objects
            final Rotation rotation = new Rotation(order, RotationConvention.FRAME_TRANSFORM,
                                                   alpha1, alpha2, alpha3);
            // Return
            return new TimeStampedAngularCoordinates(date, rotation, Vector3D.ZERO, Vector3D.ZERO);
        }

        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_R;
        }

    },

    /** Euler angles and rotation rate. */
    EULER_ANGLE_RATE("EULER ANGLE RATE") {

        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates,
                                        final boolean isFirst, final RotationOrder order) {
            // Initialize the array of attitude data
            final double[] data = new double[6];

            // Attitude
            final Rotation rotation     = coordinates.getRotation();
            final Vector3D rotationRate = coordinates.getRotationRate();
            final double[] angles       = rotation.getAngles(order, RotationConvention.FRAME_TRANSFORM);

            // Fill the array
            data[0] = FastMath.toDegrees(angles[0]);
            data[1] = FastMath.toDegrees(angles[1]);
            data[2] = FastMath.toDegrees(angles[2]);
            data[3] = FastMath.toDegrees(rotationRate.getX());
            data[4] = FastMath.toDegrees(rotationRate.getY());
            data[5] = FastMath.toDegrees(rotationRate.getZ());

            // Return
            return data;
        }

        @Override
        public TimeStampedAngularCoordinates getAngularCoordinates(final AbsoluteDate date, final double[] data,
                                                                   final boolean isFirst, final RotationOrder order) {
            // Euler angles
            final double alpha1 = FastMath.toRadians(data[0]);
            final double alpha2 = FastMath.toRadians(data[1]);
            final double alpha3 = FastMath.toRadians(data[2]);
            // Rotation rate
            final double xRate = FastMath.toRadians(data[3]);
            final double yRate = FastMath.toRadians(data[4]);
            final double zRate = FastMath.toRadians(data[5]);

            // Build the needed objects
            final Rotation rotation     = new Rotation(order, RotationConvention.FRAME_TRANSFORM,
                                                   alpha1, alpha2, alpha3);
            final Vector3D rotationRate = new Vector3D(xRate, yRate, zRate);
            // Return
            return new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);
        }

        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_RR;
        }

    },

    /** Spin. */
    SPIN("SPIN") {

        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates,
                                        final boolean isFirst, final RotationOrder order) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, getName());
        }

        @Override
        public TimeStampedAngularCoordinates getAngularCoordinates(final AbsoluteDate date, final double[] data,
                                                                   final boolean isFirst, final RotationOrder order) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, getName());
        }

        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, getName());
        }

    },

    /** Spin and nutation. */
    SPIN_NUTATION("SPIN NUTATION") {

        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates,
                                        final boolean isFirst, final RotationOrder order) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, getName());
        }

        @Override
        public TimeStampedAngularCoordinates getAngularCoordinates(final AbsoluteDate date, final double[] data,
                                                                   final boolean isFirst, final RotationOrder order) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, getName());
        }

        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, getName());
        }

    };

    /** Codes map. */
    private static final Map<String, AEMAttitudeType> CODES_MAP = new HashMap<String, AEMAttitudeType>();
    static {
        for (final AEMAttitudeType type : values()) {
            CODES_MAP.put(type.getName(), type);
        }
    }

    /** Name of the attitude type. */
    private final String name;

    /**
     * Constructor.
     * @param name name of the attitude type
     */
    AEMAttitudeType(final String name) {
        this.name = name;
    }

    /**
     * Get the name of the attitude type.
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the attitude type corresponding to the given name.
     * @param name given name
     * @return attitude type
     */
    public static AEMAttitudeType getAttitudeType(final String name) {
        final AEMAttitudeType type = CODES_MAP.get(name);
        if (type == null) {
            // An exception is thrown if the attitude type is null
            throw new OrekitException(OrekitMessages.CCSDS_AEM_NULL_ATTITUDE_TYPE, name);
        }
        return type;
    }

    /**
     * Get the attitude data corresponding to the attitude type.
     * <p>
     * Note that, according to the CCSDS ADM documentation, angles values
     * are given in degrees.
     * </p>
     * @param attitude angular coordinates
     * @param isFirst true if QC is the first element in the attitude data
     * @param order rotation order of the Euler angles
     * @return the attitude data (see 4-4)
     */
    public abstract double[] getAttitudeData(TimeStampedAngularCoordinates attitude, boolean isFirst,
                                             RotationOrder order);

    /**
     * Get the angular coordinates corresponding to the attitude data.
     * <p>
     * Note that, according to the CCSDS ADM documentation, angles values
     * must be given in degrees.
     * </p>
     * @param date coordinates date
     * @param attitudeData attitude data
     * @param isFirst true if QC is the first element in the attitude data
     * @param order rotation order of the Euler angles
     * @return the angular coordinates
     */
    public abstract TimeStampedAngularCoordinates getAngularCoordinates(AbsoluteDate date, double[] attitudeData,
                                                                        boolean isFirst, RotationOrder order);

    /**
     * Get the angular derivative filter corresponding to the attitude data.
     * @return the angular derivative filter corresponding to the attitude data
     */
    public abstract AngularDerivativesFilter getAngularDerivativesFilter();

}
