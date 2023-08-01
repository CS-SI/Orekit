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
package org.orekit.files.ccsds.ndm.adm;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
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
    QUATERNION(Collections.singleton(new VersionedName(1.0, "QUATERNION")),
               AngularDerivativesFilter.USE_R,
               Unit.ONE, Unit.ONE, Unit.ONE, Unit.ONE) {

        /** {@inheritDoc} */
        @Override
        public double[] generateData(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                     final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                     final TimeStampedAngularCoordinates coordinates) {

            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3} : new int[] {3, 0, 1, 2};

            Rotation rotation  = coordinates.getRotation();
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }

            // Fill the array, taking care of quaternion ordering
            final double[] data = new double[4];
            data[quaternionIndex[0]] = rotation.getQ0();
            data[quaternionIndex[1]] = rotation.getQ1();
            data[quaternionIndex[2]] = rotation.getQ2();
            data[quaternionIndex[3]] = rotation.getQ3();

            return data;

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
    QUATERNION_DERIVATIVE(Collections.singleton(new VersionedName(1.0, "QUATERNION/DERIVATIVE")),
                          AngularDerivativesFilter.USE_RR,
                          Unit.ONE, Unit.ONE, Unit.ONE, Unit.ONE,
                          Units.ONE_PER_S, Units.ONE_PER_S, Units.ONE_PER_S, Units.ONE_PER_S) {

        /** {@inheritDoc} */
        @Override
        public double[] generateData(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                     final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                     final TimeStampedAngularCoordinates coordinates) {

            FieldRotation<UnivariateDerivative1> rotation = coordinates.toUnivariateDerivative1Rotation();
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }

            // Data index
            final int[] quaternionIndex = isFirst ?
                                          new int[] {0, 1, 2, 3, 4, 5, 6, 7} :
                                          new int[] {3, 0, 1, 2, 7, 4, 5, 6};

            // Fill the array, taking care of quaternion ordering
            final double[] data = new double[8];
            data[quaternionIndex[0]] = rotation.getQ0().getValue();
            data[quaternionIndex[1]] = rotation.getQ1().getValue();
            data[quaternionIndex[2]] = rotation.getQ2().getValue();
            data[quaternionIndex[3]] = rotation.getQ3().getValue();
            data[quaternionIndex[4]] = rotation.getQ0().getFirstDerivative();
            data[quaternionIndex[5]] = rotation.getQ1().getFirstDerivative();
            data[quaternionIndex[6]] = rotation.getQ2().getFirstDerivative();
            data[quaternionIndex[7]] = rotation.getQ3().getFirstDerivative();

            return data;

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

    /** Quaternion and Euler angles rates (only in ADM V1). */
    QUATERNION_EULER_RATES(Collections.singleton(new VersionedName(1.0, "QUATERNION/RATE")),
                           AngularDerivativesFilter.USE_RR,
                           Unit.ONE, Unit.ONE, Unit.ONE, Unit.ONE,
                           Units.DEG_PER_S, Units.DEG_PER_S, Units.DEG_PER_S) {

        /** {@inheritDoc} */
        @Override
        public double[] generateData(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                     final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                     final TimeStampedAngularCoordinates coordinates) {

            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3} : new int[] {3, 0, 1, 2};

            // Attitude
            FieldRotation<UnivariateDerivative1> rotation = coordinates.toUnivariateDerivative1Rotation();
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }
            final UnivariateDerivative1[] euler = rotation.getAngles(eulerRotSequence, RotationConvention.FRAME_TRANSFORM);

            // Fill the array, taking care of quaternion ordering
            final double[] data = new double[7];
            data[quaternionIndex[0]] = rotation.getQ0().getValue();
            data[quaternionIndex[1]] = rotation.getQ1().getValue();
            data[quaternionIndex[2]] = rotation.getQ2().getValue();
            data[quaternionIndex[3]] = rotation.getQ3().getValue();
            data[4]                  = euler[0].getFirstDerivative();
            data[5]                  = euler[1].getFirstDerivative();
            data[6]                  = euler[2].getFirstDerivative();

            return data;

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
            final double[] euler = rotation.getAngles(eulerRotSequence, RotationConvention.FRAME_TRANSFORM);
            final FieldRotation<UnivariateDerivative1> rUD1 =
                            new FieldRotation<>(eulerRotSequence, RotationConvention.FRAME_TRANSFORM,
                                                new UnivariateDerivative1(euler[0], components[4]),
                                                new UnivariateDerivative1(euler[1], components[5]),
                                                new UnivariateDerivative1(euler[2], components[6]));

            // Return
            final TimeStampedAngularCoordinates ac = new TimeStampedAngularCoordinates(date, rUD1);
            return isExternal2SpacecraftBody ? ac : ac.revert();

        }

    },

    /** Quaternion and angular velocity. */
    QUATERNION_ANGVEL(Collections.singleton(new VersionedName(2.0, "QUATERNION/ANGVEL")),
                      AngularDerivativesFilter.USE_RR,
                      Unit.ONE, Unit.ONE, Unit.ONE, Unit.ONE,
                      Units.DEG_PER_S, Units.DEG_PER_S, Units.DEG_PER_S) {

        /** {@inheritDoc} */
        @Override
        public double[] generateData(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                     final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                     final TimeStampedAngularCoordinates coordinates) {

            // Data index
            final int[] quaternionIndex = isFirst ? new int[] {0, 1, 2, 3} : new int[] {3, 0, 1, 2};

            // Attitude
            final TimeStampedAngularCoordinates c = isExternal2SpacecraftBody ? coordinates : coordinates.revert();
            final Vector3D rotationRate = QUATERNION_ANGVEL.metadataRate(isSpacecraftBodyRate, c.getRotationRate(), c.getRotation());

            // Fill the array, taking care of quaternion ordering
            final double[] data = new double[7];
            data[quaternionIndex[0]] = c.getRotation().getQ0();
            data[quaternionIndex[1]] = c.getRotation().getQ1();
            data[quaternionIndex[2]] = c.getRotation().getQ2();
            data[quaternionIndex[3]] = c.getRotation().getQ3();
            data[4] = rotationRate.getX();
            data[5] = rotationRate.getY();
            data[6] = rotationRate.getZ();

            return data;

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
            final Vector3D rotationRate = QUATERNION_ANGVEL.orekitRate(isSpacecraftBodyRate,
                                                                       new Vector3D(components[4], components[5], components[6]),
                                                                       rotation);

            // Return
            final TimeStampedAngularCoordinates ac =
                            new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);
            return isExternal2SpacecraftBody ? ac : ac.revert();

        }

    },

    /** Euler angles. */
    EULER_ANGLE(Collections.singleton(new VersionedName(1.0, "EULER_ANGLE")),
                AngularDerivativesFilter.USE_R,
                Unit.DEGREE, Unit.DEGREE, Unit.DEGREE) {

        /** {@inheritDoc} */
        @Override
        public double[] generateData(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                     final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                     final TimeStampedAngularCoordinates coordinates) {

            // Attitude
            Rotation rotation = coordinates.getRotation();
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }

            return rotation.getAngles(eulerRotSequence, RotationConvention.FRAME_TRANSFORM);

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
    EULER_ANGLE_DERIVATIVE(Arrays.asList(new VersionedName(1.0, "EULER_ANGLE/RATE"),
                                         new VersionedName(2.0, "EULER_ANGLE/DERIVATIVE")),
                           AngularDerivativesFilter.USE_RR,
                           Unit.DEGREE, Unit.DEGREE, Unit.DEGREE,
                           Units.DEG_PER_S, Units.DEG_PER_S, Units.DEG_PER_S) {

        /** {@inheritDoc} */
        @Override
        public double[] generateData(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                     final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                     final TimeStampedAngularCoordinates coordinates) {

            // Attitude
            FieldRotation<UnivariateDerivative1> rotation = coordinates.toUnivariateDerivative1Rotation();
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }

            final UnivariateDerivative1[] angles = rotation.getAngles(eulerRotSequence, RotationConvention.FRAME_TRANSFORM);

            return new double[] {
                angles[0].getValue(),
                angles[1].getValue(),
                angles[2].getValue(),
                angles[0].getFirstDerivative(),
                angles[1].getFirstDerivative(),
                angles[2].getFirstDerivative()
            };

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
            FieldRotation<UnivariateDerivative1> rotation =
                            new FieldRotation<>(eulerRotSequence, RotationConvention.FRAME_TRANSFORM,
                                                new UnivariateDerivative1(components[0], components[3]),
                                                new UnivariateDerivative1(components[1], components[4]),
                                                new UnivariateDerivative1(components[2], components[5]));
            if (!isExternal2SpacecraftBody) {
                rotation = rotation.revert();
            }

            return new TimeStampedAngularCoordinates(date, rotation);

        }

    },

    /** Euler angles and angular velocity.
     * @since 12.0
     */
    EULER_ANGLE_ANGVEL(Collections.singleton(new VersionedName(2.0, "EULER_ANGLE/ANGVEL")),
                       AngularDerivativesFilter.USE_RR,
                       Unit.DEGREE, Unit.DEGREE, Unit.DEGREE,
                       Units.DEG_PER_S, Units.DEG_PER_S, Units.DEG_PER_S) {

        /** {@inheritDoc} */
        @Override
        public double[] generateData(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                     final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                     final TimeStampedAngularCoordinates coordinates) {

            // Attitude
            final TimeStampedAngularCoordinates c = isExternal2SpacecraftBody ? coordinates : coordinates.revert();
            final Vector3D rotationRate = EULER_ANGLE_ANGVEL.metadataRate(isSpacecraftBodyRate, c.getRotationRate(), c.getRotation());
            final double[] angles       = c.getRotation().getAngles(eulerRotSequence, RotationConvention.FRAME_TRANSFORM);

            return new double[] {
                angles[0],
                angles[1],
                angles[2],
                rotationRate.getX(),
                rotationRate.getY(),
                rotationRate.getZ()
            };

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
            final Vector3D rotationRate = EULER_ANGLE_ANGVEL.orekitRate(isSpacecraftBodyRate,
                                                                        new Vector3D(components[3], components[4], components[5]),
                                                                        rotation);
            // Return
            final TimeStampedAngularCoordinates ac =
                            new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);
            return isExternal2SpacecraftBody ? ac : ac.revert();

        }

    },

    /** Spin. */
    SPIN(Collections.singleton(new VersionedName(1.0, "SPIN")),
         AngularDerivativesFilter.USE_R,
         Unit.DEGREE, Unit.DEGREE, Unit.DEGREE, Units.DEG_PER_S) {

        /** {@inheritDoc} */
        @Override
        public double[] generateData(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                     final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                     final TimeStampedAngularCoordinates coordinates) {

            // spin axis is forced to Z (but it is not the instantaneous rotation rate as it also moves)
            final TimeStampedAngularCoordinates c = isExternal2SpacecraftBody ? coordinates : coordinates.revert();
            final SpinFinder sf = new SpinFinder(c);
            final double spinAngleVel = coordinates.getRotationRate().getZ();

            return new double[] {
                sf.getSpinAlpha(), sf.getSpinDelta(), sf.getSpinAngle(), spinAngleVel
            };

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
            final Rotation rotation = new Rotation(RotationOrder.ZXZ,
                                                   RotationConvention.FRAME_TRANSFORM,
                                                   MathUtils.SEMI_PI + components[0],
                                                   MathUtils.SEMI_PI - components[1],
                                                   components[2]);
            final Vector3D rotationRate = new Vector3D(0, 0, components[3]);

            // Return
            final TimeStampedAngularCoordinates ac =
                            new TimeStampedAngularCoordinates(date, rotation, rotationRate, Vector3D.ZERO);
            return isExternal2SpacecraftBody ? ac : ac.revert();

        }

    },

    /** Spin and nutation. */
    SPIN_NUTATION(Collections.singleton(new VersionedName(1.0, "SPIN/NUTATION")),
                  AngularDerivativesFilter.USE_RR,
                  Unit.DEGREE, Unit.DEGREE, Unit.DEGREE, Units.DEG_PER_S,
                  Unit.DEGREE, Unit.SECOND, Unit.DEGREE) {

        /** {@inheritDoc} */
        @Override
        public double[] generateData(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                     final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                     final TimeStampedAngularCoordinates coordinates) {

            // spin data
            final TimeStampedAngularCoordinates c = isExternal2SpacecraftBody ? coordinates : coordinates.revert();
            final SpinFinder sf = new SpinFinder(c);

            // Orekit/CCSDS naming difference: for CCSDS this is nutation, for Orekit this is precession
            final FieldRotation<UnivariateDerivative2> c2       = c.toUnivariateDerivative2Rotation();
            final FieldVector3D<UnivariateDerivative2> spinAxis = c2.applyInverseTo(Vector3D.PLUS_K);
            final PrecessionFinder                     pf       = new PrecessionFinder(spinAxis);

            // intermediate inertial frame, with Z axis aligned with angular momentum
            final Rotation intermediate2Inert = new Rotation(Vector3D.PLUS_K, pf.getAxis());

            // recover Euler rotations starting from frame aligned with angular momentum
            final FieldRotation<UnivariateDerivative2> intermediate2Body = c2.applyTo(intermediate2Inert);
            final UnivariateDerivative2[] euler = intermediate2Body.
                                                  getAngles(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM);

            return new double[] {
                sf.getSpinAlpha(),
                sf.getSpinDelta(),
                sf.getSpinAngle(),
                euler[2].getFirstDerivative(),
                pf.getPrecessionAngle(),
                MathUtils.TWO_PI / pf.getAngularVelocity(),
                euler[2].getValue() - MathUtils.SEMI_PI
            };

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
            final Rotation inert2Body0 = new Rotation(RotationOrder.ZXZ,
                                                      RotationConvention.FRAME_TRANSFORM,
                                                      MathUtils.SEMI_PI + components[0],
                                                      MathUtils.SEMI_PI - components[1],
                                                      components[2]);

            // intermediate inertial frame, with Z axis aligned with angular momentum
            final SinCos   scNutation         = FastMath.sinCos(components[4]);
            final SinCos   scPhase            = FastMath.sinCos(components[6]);
            final Vector3D momentumBody       = new Vector3D( scNutation.sin() * scPhase.cos(),
                                                             -scNutation.sin() * scPhase.sin(),
                                                              scNutation.cos());
            final Vector3D momentumInert      = inert2Body0.applyInverseTo(momentumBody);
            final Rotation inert2Intermediate = new Rotation(momentumInert, Vector3D.PLUS_K);

            // base Euler angles from the intermediate frame to body
            final Rotation intermediate2Body0 = inert2Body0.applyTo(inert2Intermediate.revert());
            final double[] euler0             = intermediate2Body0.getAngles(RotationOrder.ZXZ,
                                                                             RotationConvention.FRAME_TRANSFORM);

            // add Euler angular rates to base Euler angles
            final FieldRotation<UnivariateDerivative2> intermediate2Body =
                            new FieldRotation<>(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM,
                                                new UnivariateDerivative2(euler0[0], MathUtils.TWO_PI / components[5], 0.0),
                                                new UnivariateDerivative2(euler0[1], 0.0,           0.0),
                                                new UnivariateDerivative2(euler0[2], components[3], 0.0));

            // final rotation, including derivatives
            final FieldRotation<UnivariateDerivative2> inert2Body = intermediate2Body.applyTo(inert2Intermediate);

            final TimeStampedAngularCoordinates ac =
                            new TimeStampedAngularCoordinates(date, inert2Body);
            return isExternal2SpacecraftBody ? ac : ac.revert();

        }

    },

    /** Spin and momentum.
     * @since 12.0
     */
    SPIN_NUTATION_MOMENTUM(Collections.singleton(new VersionedName(2.0, "SPIN/NUTATION_MOM")),
                           AngularDerivativesFilter.USE_RR,
                           Unit.DEGREE, Unit.DEGREE, Unit.DEGREE, Units.DEG_PER_S,
                           Unit.DEGREE, Unit.DEGREE, Units.DEG_PER_S) {

        /** {@inheritDoc} */
        @Override
        public double[] generateData(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                     final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                     final TimeStampedAngularCoordinates coordinates) {

            // spin data
            final TimeStampedAngularCoordinates c = isExternal2SpacecraftBody ? coordinates : coordinates.revert();
            final SpinFinder sf = new SpinFinder(c);

            // Orekit/CCSDS naming difference: for CCSDS this is nutation, for Orekit this is precession
            final FieldRotation<UnivariateDerivative2> c2       = c.toUnivariateDerivative2Rotation();
            final FieldVector3D<UnivariateDerivative2> spinAxis = c2.applyInverseTo(Vector3D.PLUS_K);
            final PrecessionFinder                     pf       = new PrecessionFinder(spinAxis);

            // intermediate inertial frame, with Z axis aligned with angular momentum
            final Rotation intermediate2Inert = new Rotation(Vector3D.PLUS_K, pf.getAxis());

            // recover spin angle velocity
            final FieldRotation<UnivariateDerivative2> intermediate2Body = c2.applyTo(intermediate2Inert);
            final double spinAngleVel = intermediate2Body.
                                        getAngles(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM)[2].
                                        getFirstDerivative();

            return new double[] {
                sf.getSpinAlpha(),
                sf.getSpinDelta(),
                sf.getSpinAngle(),
                spinAngleVel,
                pf.getAxis().getAlpha(),
                pf.getAxis().getDelta(),
                pf.getAngularVelocity()
            };

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
            final SinCos   scAlpha            = FastMath.sinCos(components[4]);
            final SinCos   scDelta            = FastMath.sinCos(components[5]);
            final Vector3D momentumInert      = new Vector3D(scAlpha.cos() * scDelta.cos(),
                                                             scAlpha.sin() * scDelta.cos(),
                                                             scDelta.sin());
            final Rotation inert2Intermediate = new Rotation(momentumInert, Vector3D.PLUS_K);

            // base Euler angles from the intermediate frame to body
            final Rotation inert2Body0 = new Rotation(RotationOrder.ZXZ,
                                                      RotationConvention.FRAME_TRANSFORM,
                                                      MathUtils.SEMI_PI + components[0],
                                                      MathUtils.SEMI_PI - components[1],
                                                      components[2]);
            final Rotation intermediate2Body0 = inert2Body0.applyTo(inert2Intermediate.revert());
            final double[] euler0             = intermediate2Body0.getAngles(RotationOrder.ZXZ,
                                                                             RotationConvention.FRAME_TRANSFORM);

            // add Euler angular rates to base Euler angles
            final FieldRotation<UnivariateDerivative2> intermediate2Body =
                            new FieldRotation<>(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM,
                                                new UnivariateDerivative2(euler0[0], components[6], 0.0),
                                                new UnivariateDerivative2(euler0[1], 0.0,           0.0),
                                                new UnivariateDerivative2(euler0[2], components[3], 0.0));

            // final rotation, including derivatives
            final FieldRotation<UnivariateDerivative2> inert2Body = intermediate2Body.applyTo(inert2Intermediate);

            // return
            final TimeStampedAngularCoordinates ac =
                            new TimeStampedAngularCoordinates(date, inert2Body);
            return isExternal2SpacecraftBody ? ac : ac.revert();

        }

    };

    /** Names map.
     * @since 12.0
     */
    private static final Map<String, AttitudeType> MAP = new HashMap<>();
    static {
        for (final AttitudeType type : values()) {
            for (final VersionedName vn : type.ccsdsNames) {
                MAP.put(vn.name, type);
            }
        }
    }

    /** CCSDS names of the attitude type. */
    private final Iterable<VersionedName> ccsdsNames;

    /** Derivatives filter. */
    private final AngularDerivativesFilter filter;

    /** Components units (used only for parsing). */
    private final Unit[] units;

    /** Private constructor.
     * @param ccsdsNames CCSDS names of the attitude type
     * @param filter derivative filter
     * @param units components units (used only for parsing)
     */
    AttitudeType(final Iterable<VersionedName> ccsdsNames, final AngularDerivativesFilter filter, final Unit... units) {
        this.ccsdsNames = ccsdsNames;
        this.filter     = filter;
        this.units      = units.clone();
    }

    /** Get the type name for a given format version.
     * @param formatVersion format version
     * @return type name
     * @since 12.0
     */
    public String getName(final double formatVersion) {
        String name = null;
        for (final VersionedName vn : ccsdsNames) {
            if (name == null || formatVersion >= vn.since) {
                name = vn.name;
            }
        }
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // use the most recent name by default
        return getName(Double.POSITIVE_INFINITY);
    }

    /** Parse an attitude type.
     * @param typeSpecification unnormalized type name
     * @return parsed type
     */
    public static AttitudeType parseType(final String typeSpecification) {
        final AttitudeType type = MAP.get(typeSpecification);
        if (type == null) {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_ATTITUDE_TYPE, typeSpecification);
        }
        return type;
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
    public String[] createDataFields(final boolean isFirst, final boolean isExternal2SpacecraftBody,
                                     final RotationOrder eulerRotSequence, final boolean isSpacecraftBodyRate,
                                     final TimeStampedAngularCoordinates attitude) {

        // generate the double data
        final double[] data = generateData(isFirst, isExternal2SpacecraftBody,
                                           eulerRotSequence, isSpacecraftBodyRate, attitude);

        // format as string array with CCSDS units
        final String[] fields = new String[data.length];
        for (int i = 0; i < data.length; ++i) {
            fields[i] = AccurateFormatter.format(units[i].fromSI(data[i]));
        }

        return fields;

    }

    /**
     * Generate the attitude data corresponding to the attitude type.
     * <p>
     * This method returns the components in SI units.
     * </p>
     * @param isFirst if true the first quaternion component is the scalar component
     * @param isExternal2SpacecraftBody true attitude is from external frame to spacecraft body frame
     * @param eulerRotSequence sequance of Euler angles
     * @param isSpacecraftBodyRate if true Euler rates are specified in spacecraft body frame
     * @param attitude angular coordinates, using {@link Attitude Attitude} convention
     * (i.e. from inertial frame to spacecraft frame)
     * @return the attitude data in CCSDS units
     * @since 12.0
     */
    public abstract double[] generateData(boolean isFirst, boolean isExternal2SpacecraftBody,
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
     * @param components entry components with SI units, semantic depends on attitude type
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

    /** Container for a name associated to a format version.
     * @since 12.0
     */
    private static class VersionedName {

        /** Version at which this name was defined. */
        private final double since;

        /** Name. */
        private final String name;

        /** Simple constructor.
         * @param since version at which this name was defined
         * @param name name
         */
        VersionedName(final double since, final String name) {
            this.since = since;
            this.name  = name;
        }

    }

}
