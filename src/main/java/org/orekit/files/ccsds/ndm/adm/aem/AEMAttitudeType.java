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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.util.regex.Pattern;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;

/** Enumerate for AEM attitude type.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public enum AEMAttitudeType {

    /** Quaternion. */
    QUATERNION {

        /** {@inheritDoc} */
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

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AEMMetadata metadata, final ParsingContext context, final String[] fields) {

            // Build the needed objects
            final AbsoluteDate date = parseDate(context, fields[0]);
            final Rotation rotation = metadata.isFirst() ?
                                      new Rotation(Double.parseDouble(fields[1]),
                                                   Double.parseDouble(fields[2]),
                                                   Double.parseDouble(fields[3]),
                                                   Double.parseDouble(fields[4]),
                                                   true) :
                                      new Rotation(Double.parseDouble(fields[4]),
                                                   Double.parseDouble(fields[1]),
                                                   Double.parseDouble(fields[2]),
                                                   Double.parseDouble(fields[3]),
                                                   true);

            // Return
            return new TimeStampedAngularCoordinates(date, rotation, Vector3D.ZERO, Vector3D.ZERO);

        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_R;
        }

    },

    /** Quaternion and derivatives. */
    QUATERNION_DERIVATIVE {

        /** {@inheritDoc} */
        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates,
                                        final boolean isFirst, final RotationOrder order) {
            // Initialize the array of attitude data
            final double[] data = new double[8];

            final FieldRotation<UnivariateDerivative1> fieldRotation = coordinates.toUnivariateDerivative1Rotation();

            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3, 4, 5, 6, 7} : new int[] {3, 0, 1, 2, 7, 4, 5, 6};

            // Fill the array
            data[quaternionIndex[0]] = fieldRotation.getQ0().getValue();
            data[quaternionIndex[1]] = fieldRotation.getQ1().getValue();
            data[quaternionIndex[2]] = fieldRotation.getQ2().getValue();
            data[quaternionIndex[3]] = fieldRotation.getQ3().getValue();
            data[quaternionIndex[4]] = fieldRotation.getQ0().getFirstDerivative();
            data[quaternionIndex[5]] = fieldRotation.getQ1().getFirstDerivative();
            data[quaternionIndex[6]] = fieldRotation.getQ2().getFirstDerivative();
            data[quaternionIndex[7]] = fieldRotation.getQ3().getFirstDerivative();

            // Return
            return data;
        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AEMMetadata metadata, final ParsingContext context, final String[] fields) {

            // Build the needed objects
            final AbsoluteDate date = parseDate(context, fields[0]);
            final FieldRotation<UnivariateDerivative1> rotation =
                            metadata.isFirst() ?
                            new FieldRotation<>(new UnivariateDerivative1(Double.parseDouble(fields[1]), Double.parseDouble(fields[5])),
                                                new UnivariateDerivative1(Double.parseDouble(fields[2]), Double.parseDouble(fields[6])),
                                                new UnivariateDerivative1(Double.parseDouble(fields[3]), Double.parseDouble(fields[7])),
                                                new UnivariateDerivative1(Double.parseDouble(fields[4]), Double.parseDouble(fields[8])),
                                                true) :
                            new FieldRotation<>(new UnivariateDerivative1(Double.parseDouble(fields[4]), Double.parseDouble(fields[8])),
                                                new UnivariateDerivative1(Double.parseDouble(fields[1]), Double.parseDouble(fields[5])),
                                                new UnivariateDerivative1(Double.parseDouble(fields[2]), Double.parseDouble(fields[6])),
                                                new UnivariateDerivative1(Double.parseDouble(fields[3]), Double.parseDouble(fields[7])),
                                                true);

            return new TimeStampedAngularCoordinates(date, rotation);

        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_RR;
        }

    },

    /** Quaternion and rotation rate. */
    QUATERNION_RATE {

        /** {@inheritDoc} */
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

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AEMMetadata metadata, final ParsingContext context,
                                                   final String[] fields) {
            // Build the needed objects
            final AbsoluteDate date = parseDate(context, fields[0]);
            final Rotation rotation = metadata.isFirst() ?
                                      new Rotation(Double.parseDouble(fields[1]),
                                                   Double.parseDouble(fields[2]),
                                                   Double.parseDouble(fields[3]),
                                                   Double.parseDouble(fields[4]),
                                                   true) :
                                      new Rotation(Double.parseDouble(fields[4]),
                                                   Double.parseDouble(fields[1]),
                                                   Double.parseDouble(fields[2]),
                                                   Double.parseDouble(fields[3]),
                                                   true);
            final Vector3D rotationRate = new Vector3D(FastMath.toRadians(Double.parseDouble(fields[5])),
                                                       FastMath.toRadians(Double.parseDouble(fields[6])),
                                                       FastMath.toRadians(Double.parseDouble(fields[7])));

            // Return
            return new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);
        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_RR;
        }

    },

    /** Euler angles. */
    EULER_ANGLE {

        /** {@inheritDoc} */
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

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AEMMetadata metadata, final ParsingContext context,
                                                   final String[] fields) {

            // Build the needed objects
            final AbsoluteDate date = parseDate(context, fields[0]);
            final Rotation rotation = new Rotation(metadata.getEulerRotSeq(),
                                                   RotationConvention.FRAME_TRANSFORM,
                                                   FastMath.toRadians(Double.parseDouble(fields[1])),
                                                   FastMath.toRadians(Double.parseDouble(fields[2])),
                                                   FastMath.toRadians(Double.parseDouble(fields[3])));
            // Return
            return new TimeStampedAngularCoordinates(date, rotation, Vector3D.ZERO, Vector3D.ZERO);
        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_R;
        }

    },

    /** Euler angles and rotation rate. */
    EULER_ANGLE_RATE {

        /** {@inheritDoc} */
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

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AEMMetadata metadata, final ParsingContext context,
                                                   final String[] fields) {

            // Build the needed objects
            final AbsoluteDate date = parseDate(context, fields[0]);
            final Rotation rotation = new Rotation(metadata.getEulerRotSeq(),
                                                   RotationConvention.FRAME_TRANSFORM,
                                                   FastMath.toRadians(Double.parseDouble(fields[1])),
                                                   FastMath.toRadians(Double.parseDouble(fields[2])),
                                                   FastMath.toRadians(Double.parseDouble(fields[3])));
            final Vector3D rotationRate = new Vector3D(FastMath.toRadians(Double.parseDouble(fields[4])),
                                                       FastMath.toRadians(Double.parseDouble(fields[5])),
                                                       FastMath.toRadians(Double.parseDouble(fields[6])));
            // Return
            return new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);
        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_RR;
        }

    },

    /** Spin. */
    SPIN {

        /** {@inheritDoc} */
        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates,
                                        final boolean isFirst, final RotationOrder order) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, name());
        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AEMMetadata metadata, final ParsingContext context,
                                                   final String[] fields) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, name());
        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, name());
        }

    },

    /** Spin and nutation. */
    SPIN_NUTATION {

        /** {@inheritDoc} */
        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates,
                                        final boolean isFirst, final RotationOrder order) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, name());
        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AEMMetadata metadata, final ParsingContext context,
                                                   final String[] fields) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, name());
        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, name());
        }

    };

    /** Pattern for normalizing attitude types. */
    private static final Pattern TYPE_SEPARATORS = Pattern.compile("[ _/]+");

    /** Parse an attitude type.
     * @param type unnormalized type name
     * @return parsed type
     */
    public static AEMAttitudeType parseType(final String type) {
        return AEMAttitudeType.valueOf(TYPE_SEPARATORS.matcher(type).replaceAll("_"));
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
     * @param metadata metadata used to interprete the data fields
     * @param context parsing context
     * @param fields raw data fields
     * @return the angular coordinates
     */
    public abstract TimeStampedAngularCoordinates parse(AEMMetadata metadata, ParsingContext context, String[] fields);

    /**
     * Get the angular derivative filter corresponding to the attitude data.
     * @return the angular derivative filter corresponding to the attitude data
     */
    public abstract AngularDerivativesFilter getAngularDerivativesFilter();

    /** Parse a date.
     * @param context parsing context
     * @param date date field
     * @return parsed date
     */
    private static AbsoluteDate parseDate(final ParsingContext context, final String date) {
        return context.getTimeScale().parseDate(date,
                                                context.getConventions(),
                                                context.getMissionReferenceDate(),
                                                context.getDataContext().getTimeScales());
    }

}
