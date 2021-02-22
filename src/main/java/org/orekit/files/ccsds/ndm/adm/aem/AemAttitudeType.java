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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
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
public enum AemAttitudeType {

    /** Quaternion. */
    QUATERNION("QUATERNION") {

        /** {@inheritDoc} */
        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates, final AemMetadata metadata) {
            // Initialize the array of attitude data
            final double[] data = new double[4];

            // Data index
            final int[] quaternionIndex = metadata.isFirst() ? new int[] {0, 1, 2, 3} : new int[] {3, 0, 1, 2};

            // Fill the array
            Rotation rotation  = coordinates.getRotation();
            if (!metadata.getEndPoints().isExternal2Local()) {
                rotation = rotation.revert();
            }
            data[quaternionIndex[0]] = rotation.getQ0();
            data[quaternionIndex[1]] = rotation.getQ1();
            data[quaternionIndex[2]] = rotation.getQ2();
            data[quaternionIndex[3]] = rotation.getQ3();

            // Return
            return data;
        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AemMetadata metadata, final ParsingContext context,
                                                   final String[] fields, final String fileName) {

            // Build the needed objects
            final AbsoluteDate date = context.getTimeScale().parseDate(fields[0], context);
            Rotation rotation       = metadata.isFirst() ?
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
            if (!metadata.getEndPoints().isExternal2Local()) {
                rotation = rotation.revert();
            }

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
    QUATERNION_DERIVATIVE("QUATERNION/DERIVATIVE") {

        /** {@inheritDoc} */
        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates, final AemMetadata metadata) {
            // Initialize the array of attitude data
            final double[] data = new double[8];

            FieldRotation<UnivariateDerivative1> rotation = coordinates.toUnivariateDerivative1Rotation();
            if (!metadata.getEndPoints().isExternal2Local()) {
                rotation = rotation.revert();
            }

            // Data index
            final int[] quaternionIndex = metadata.isFirst() ? new int[] {0, 1, 2, 3, 4, 5, 6, 7} : new int[] {3, 0, 1, 2, 7, 4, 5, 6};

            // Fill the array
            data[quaternionIndex[0]] = rotation.getQ0().getValue();
            data[quaternionIndex[1]] = rotation.getQ1().getValue();
            data[quaternionIndex[2]] = rotation.getQ2().getValue();
            data[quaternionIndex[3]] = rotation.getQ3().getValue();
            data[quaternionIndex[4]] = rotation.getQ0().getFirstDerivative();
            data[quaternionIndex[5]] = rotation.getQ1().getFirstDerivative();
            data[quaternionIndex[6]] = rotation.getQ2().getFirstDerivative();
            data[quaternionIndex[7]] = rotation.getQ3().getFirstDerivative();

            // Return
            return data;
        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AemMetadata metadata, final ParsingContext context,
                                                   final String[] fields, final String fileName) {

            // Build the needed objects
            final AbsoluteDate date = context.getTimeScale().parseDate(fields[0], context);
            FieldRotation<UnivariateDerivative1> rotation =
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
            if (!metadata.getEndPoints().isExternal2Local()) {
                rotation = rotation.revert();
            }

            return new TimeStampedAngularCoordinates(date, rotation);

        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_RR;
        }

    },

    /** Quaternion and rotation rate. */
    QUATERNION_RATE("QUATERNION/RATE") {

        /** {@inheritDoc} */
        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates, final AemMetadata metadata) {
            // Initialize the array of attitude data
            final double[] data = new double[7];

            // Data index
            final int[] quaternionIndex = metadata.isFirst() ? new int[] {0, 1, 2, 3} : new int[] {3, 0, 1, 2};

            // Attitude
            final TimeStampedAngularCoordinates c =
                            metadata.getEndPoints().isExternal2Local() ? coordinates : coordinates.revert();
            final Rotation rotation     = c.getRotation();
            final Vector3D rawRate      = c.getRotationRate();
            final Vector3D rotationRate = metadata.localRates() ? rawRate : rotation.applyInverseTo(rawRate);

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
        public TimeStampedAngularCoordinates parse(final AemMetadata metadata, final ParsingContext context,
                                                   final String[] fields, final String fileName) {
            // Build the needed objects
            final AbsoluteDate date = context.getTimeScale().parseDate(fields[0], context);
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
            final Vector3D rotationRate = localRate(new Vector3D(FastMath.toRadians(Double.parseDouble(fields[5])),
                                                                 FastMath.toRadians(Double.parseDouble(fields[6])),
                                                                 FastMath.toRadians(Double.parseDouble(fields[7]))),
                                                    rotation, metadata, fileName);

            // Return
            final TimeStampedAngularCoordinates ac =
                            new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);
            return metadata.getEndPoints().isExternal2Local() ? ac : ac.revert();

        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_RR;
        }

    },

    /** Euler angles. */
    EULER_ANGLE("EULER ANGLE") {

        /** {@inheritDoc} */
        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates, final AemMetadata metadata) {
            // Initialize the array of attitude data
            final double[] data = new double[3];

            // Attitude
            Rotation rotation = coordinates.getRotation();
            if (!metadata.getEndPoints().isExternal2Local()) {
                rotation = rotation.revert();
            }
            final double[] angles   = rotation.getAngles(metadata.getEulerRotSeq(), RotationConvention.FRAME_TRANSFORM);

            // Fill the array
            data[0] = FastMath.toDegrees(angles[0]);
            data[1] = FastMath.toDegrees(angles[1]);
            data[2] = FastMath.toDegrees(angles[2]);

            // Return
            return data;
        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AemMetadata metadata, final ParsingContext context,
                                                   final String[] fields, final String fileName) {

            // Build the needed objects
            final AbsoluteDate date = context.getTimeScale().parseDate(fields[0], context);
            Rotation rotation = new Rotation(metadata.getEulerRotSeq(),
                                             RotationConvention.FRAME_TRANSFORM,
                                             FastMath.toRadians(Double.parseDouble(fields[1])),
                                             FastMath.toRadians(Double.parseDouble(fields[2])),
                                             FastMath.toRadians(Double.parseDouble(fields[3])));
            if (!metadata.getEndPoints().isExternal2Local()) {
                rotation = rotation.revert();
            }

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
    EULER_ANGLE_RATE("EULER ANGLE/RATE") {

        /** {@inheritDoc} */
        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates, final AemMetadata metadata) {
            // Initialize the array of attitude data
            final double[] data = new double[6];

            // Attitude
            final TimeStampedAngularCoordinates c =
                            metadata.getEndPoints().isExternal2Local() ? coordinates : coordinates.revert();
            final Rotation rotation     = c.getRotation();
            final Vector3D rawRate      = c.getRotationRate();
            final Vector3D rotationRate = metadata.localRates() ? rawRate : rotation.applyInverseTo(rawRate);
            final double[] angles       = rotation.getAngles(metadata.getEulerRotSeq(), RotationConvention.FRAME_TRANSFORM);

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
        public TimeStampedAngularCoordinates parse(final AemMetadata metadata, final ParsingContext context,
                                                   final String[] fields, final String fileName) {

            // Build the needed objects
            final AbsoluteDate date = context.getTimeScale().parseDate(fields[0], context);
            final Rotation rotation = new Rotation(metadata.getEulerRotSeq(),
                                                   RotationConvention.FRAME_TRANSFORM,
                                                   FastMath.toRadians(Double.parseDouble(fields[1])),
                                                   FastMath.toRadians(Double.parseDouble(fields[2])),
                                                   FastMath.toRadians(Double.parseDouble(fields[3])));
            final Vector3D rotationRate = localRate(new Vector3D(FastMath.toRadians(Double.parseDouble(fields[4])),
                                                                 FastMath.toRadians(Double.parseDouble(fields[5])),
                                                                 FastMath.toRadians(Double.parseDouble(fields[6]))),
                                                    rotation, metadata, fileName);
            // Return
            final TimeStampedAngularCoordinates ac =
                            new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);
            return metadata.getEndPoints().isExternal2Local() ? ac : ac.revert();

        }

        /** {@inheritDoc} */
        @Override
        public AngularDerivativesFilter getAngularDerivativesFilter() {
            return AngularDerivativesFilter.USE_RR;
        }

    },

    /** Spin. */
    SPIN("SPIN") {

        /** {@inheritDoc} */
        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates, final AemMetadata metadata) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, name());
        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AemMetadata metadata, final ParsingContext context,
                                                   final String[] fields, final String fileName) {
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
    SPIN_NUTATION("SPIN/NUTATION") {

        /** {@inheritDoc} */
        @Override
        public double[] getAttitudeData(final TimeStampedAngularCoordinates coordinates,
                                        final AemMetadata metadata) {
            // Attitude parameters in the Specified Reference Frame for a Spin Stabilized Satellite
            // are optional in CCSDS AEM format. Support for this attitude type is not implemented
            // yet in Orekit.
            throw new OrekitException(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, name());
        }

        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates parse(final AemMetadata metadata, final ParsingContext context,
                                                   final String[] fields, final String fileName) {
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

    /** CCSDS name of the attitude type. */
    private final String ccsdsName;

    /** Private constructor.
     * @param ccsdsName CCSDS name of the attitude type
     */
    AemAttitudeType(final String ccsdsName) {
        this.ccsdsName = ccsdsName;
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
    public static AemAttitudeType parseType(final String type) {
        return AemAttitudeType.valueOf(TYPE_SEPARATORS.matcher(type).replaceAll("_"));
    }

    /**
     * Get the attitude data corresponding to the attitude type.
     * <p>
     * Note that, according to the CCSDS ADM documentation, angles values
     * are given in degrees.
     * </p>
     * @param attitude angular coordinates, using {@link Attitude Attitude} convention
     * (i.e. from inertial frame to spacecraft frame)
     * @param metadata metadata used to interpret the data fields
     * @return the attitude data (see ADM standard table 4-4)
     */
    public abstract double[] getAttitudeData(TimeStampedAngularCoordinates attitude, AemMetadata metadata);

    /**
     * Get the angular coordinates corresponding to the attitude data.
     * <p>
     * Note that, according to the CCSDS ADM documentation, angles values
     * must be given in degrees.
     * </p>
     * @param metadata metadata used to interpret the data fields
     * @param context parsing context
     * @param fields raw data fields
     * @param fileName name of the file
     * @return the angular coordinates, using {@link Attitude Attitude} convention
     * (i.e. from inertial frame to spacecraft frame)
     */
    public abstract TimeStampedAngularCoordinates parse(AemMetadata metadata, ParsingContext context,
                                                        String[] fields, String fileName);

    /**
     * Get the angular derivative filter corresponding to the attitude data.
     * @return the angular derivative filter corresponding to the attitude data
     */
    public abstract AngularDerivativesFilter getAngularDerivativesFilter();

    /** Convert a rotation rate to spacecraft body local frame.
     * @param rate rotation rate read from the data line
     * @param rotation corresponding rotation
     * @param metadata segment metadata
     * @return rotation rate in spacecraft body local frame
     * @param fileName name of the file
     */
    private static Vector3D localRate(final Vector3D rate, final Rotation rotation,
                                      final AemMetadata metadata, final String fileName) {

        if (metadata.localRates() == null) {
            throw new OrekitException(OrekitMessages.CCSDS_MISSING_KEYWORD,
                                      AemMetadataKey.RATE_FRAME.name(), fileName);
        }

        return metadata.localRates() ? rate : rotation.applyTo(rate);

    }

}
