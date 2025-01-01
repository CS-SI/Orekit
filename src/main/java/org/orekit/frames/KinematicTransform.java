/* Copyright 2022-2025 Romain Serra
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
package org.orekit.frames;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Arrays;

/**
 * A transform that only includes translation and rotation as well as their respective rates.
 * It is kinematic in the sense that it cannot transform an acceleration vector.
 *
 * @author Romain Serra
 * @see StaticTransform
 * @see Transform
 * @since 12.1
 */
public interface KinematicTransform extends StaticTransform {

    /**
     * Get the identity kinematic transform.
     *
     * @return identity transform.
     */
    static KinematicTransform getIdentity() {
        return Transform.IDENTITY;
    }

    /** Compute a composite velocity.
     * @param first first applied transform
     * @param second second applied transform
     * @return velocity part of the composite transform
     */
    static Vector3D compositeVelocity(final KinematicTransform first, final KinematicTransform second) {

        final Vector3D v1 = first.getVelocity();
        final Rotation r1 = first.getRotation();
        final Vector3D o1 = first.getRotationRate();
        final Vector3D p2 = second.getTranslation();
        final Vector3D v2 = second.getVelocity();

        final Vector3D crossP = Vector3D.crossProduct(o1, p2);

        return v1.add(r1.applyInverseTo(v2.add(crossP)));
    }

    /** Compute a composite rotation rate.
     * @param first first applied transform
     * @param second second applied transform
     * @return rotation rate part of the composite transform
     */
    static Vector3D compositeRotationRate(final KinematicTransform first, final KinematicTransform second) {

        final Vector3D o1 = first.getRotationRate();
        final Rotation r2 = second.getRotation();
        final Vector3D o2 = second.getRotationRate();

        return o2.add(r2.applyTo(o1));
    }

    /** Transform {@link PVCoordinates}, without the acceleration vector.
     * @param pv the position-velocity couple to transform.
     * @return transformed position-velocity
     */
    default PVCoordinates transformOnlyPV(final PVCoordinates pv) {
        final Vector3D transformedP = transformPosition(pv.getPosition());
        final Vector3D crossP       = Vector3D.crossProduct(getRotationRate(), transformedP);
        final Vector3D transformedV = getRotation().applyTo(pv.getVelocity().add(getVelocity())).subtract(crossP);
        return new PVCoordinates(transformedP, transformedV);
    }

    /** Transform {@link TimeStampedPVCoordinates}, without the acceleration vector.
     * <p>
     * In order to allow the user more flexibility, this method does <em>not</em> check for
     * consistency between the transform {@link #getDate() date} and the time-stamped
     * position-velocity {@link TimeStampedPVCoordinates#getDate() date}. The returned
     * value will always have the same {@link TimeStampedPVCoordinates#getDate() date} as
     * the input argument, regardless of the instance {@link #getDate() date}.
     * </p>
     * @param pv the position-velocity couple to transform.
     * @return transformed position-velocity
     */
    default TimeStampedPVCoordinates transformOnlyPV(final TimeStampedPVCoordinates pv) {
        final Vector3D transformedP = transformPosition(pv.getPosition());
        final Vector3D crossP       = Vector3D.crossProduct(getRotationRate(), transformedP);
        final Vector3D transformedV = getRotation().applyTo(pv.getVelocity().add(getVelocity())).subtract(crossP);
        return new TimeStampedPVCoordinates(pv.getDate(), transformedP, transformedV);
    }

    /** Compute the Jacobian of the {@link #transformOnlyPV(PVCoordinates)} (PVCoordinates)}
     * method of the transform.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of Cartesian coordinate i
     * of the transformed {@link PVCoordinates} with respect to Cartesian coordinate j
     * of the input {@link PVCoordinates} in method {@link #transformOnlyPV(PVCoordinates)}.
     * </p>
     * <p>
     * This definition implies that if we define position-velocity coordinates
     * <pre>
     * PV₁ = transform.transformPVCoordinates(PV₀), then
     * </pre>
     * <p> their differentials dPV₁ and dPV₀ will obey the following relation
     * where J is the matrix computed by this method:
     * <pre>
     * dPV₁ = J &times; dPV₀
     * </pre>
     *
     * @return Jacobian matrix
     */
    default double[][] getPVJacobian() {
        final double[][] jacobian = new double[6][6];

        // elementary matrix for rotation
        final double[][] mData = getRotation().getMatrix();

        // dP1/dP0
        System.arraycopy(mData[0], 0, jacobian[0], 0, 3);
        System.arraycopy(mData[1], 0, jacobian[1], 0, 3);
        System.arraycopy(mData[2], 0, jacobian[2], 0, 3);

        // dP1/dV0
        Arrays.fill(jacobian[0], 3, 6, 0.0);
        Arrays.fill(jacobian[1], 3, 6, 0.0);
        Arrays.fill(jacobian[2], 3, 6, 0.0);

        // dV1/dP0
        final Vector3D o = getRotationRate();
        final double ox = o.getX();
        final double oy = o.getY();
        final double oz = o.getZ();
        for (int i = 0; i < 3; ++i) {
            jacobian[3][i] = -(oy * mData[2][i] - oz * mData[1][i]);
            jacobian[4][i] = -(oz * mData[0][i] - ox * mData[2][i]);
            jacobian[5][i] = -(ox * mData[1][i] - oy * mData[0][i]);
        }

        // dV1/dV0
        System.arraycopy(mData[0], 0, jacobian[3], 3, 3);
        System.arraycopy(mData[1], 0, jacobian[4], 3, 3);
        System.arraycopy(mData[2], 0, jacobian[5], 3, 3);

        return jacobian;
    }

    /** Get the first time derivative of the translation.
     * @return first time derivative of the translation
     * @see #getTranslation()
     */
    Vector3D getVelocity();

    /** Get the first time derivative of the rotation.
     * <p>The norm represents the angular rate.</p>
     * @return First time derivative of the rotation
     * @see #getRotation()
     */
    Vector3D getRotationRate();

    /**
     * Get the inverse transform of the instance.
     *
     * @return inverse transform of the instance
     */
    KinematicTransform getInverse();

    /**
     * Build a transform by combining two existing ones.
     * <p>
     * Note that the dates of the two existing transformed are <em>ignored</em>,
     * and the combined transform date is set to the date supplied in this
     * constructor without any attempt to shift the raw transforms. This is a
     * design choice allowing user full control of the combination.
     * </p>
     *
     * @param date   date of the transform
     * @param first  first transform applied
     * @param second second transform applied
     * @return the newly created kinematic transform that has the same effect as
     * applying {@code first}, then {@code second}.
     * @see #of(AbsoluteDate, PVCoordinates, Rotation, Vector3D)
     */
    static KinematicTransform compose(final AbsoluteDate date,
                                      final KinematicTransform first,
                                      final KinematicTransform second) {
        final Vector3D composedTranslation = StaticTransform.compositeTranslation(first, second);
        final Vector3D composedTranslationRate = KinematicTransform.compositeVelocity(first, second);
        return of(date, new PVCoordinates(composedTranslation, composedTranslationRate),
                StaticTransform.compositeRotation(first, second),
                KinematicTransform.compositeRotationRate(first, second));
    }

    /**
     * Create a new kinematic transform from a rotation and zero, constant translation.
     *
     * @param date     of translation.
     * @param rotation to apply after the translation. That is after translating
     *                 applying this rotation produces positions expressed in
     *                 the new frame.
     * @param rotationRate rate of rotation
     * @return the newly created kinematic transform.
     * @see #of(AbsoluteDate, PVCoordinates, Rotation, Vector3D)
     */
    static KinematicTransform of(final AbsoluteDate date,
                                 final Rotation rotation,
                                 final Vector3D rotationRate) {
        return of(date, PVCoordinates.ZERO, rotation, rotationRate);
    }

    /**
     * Create a new kinematic transform from a translation and its rate.
     *
     * @param date        of translation.
     * @param pvCoordinates translation (with rate) to apply, expressed in the old frame. That is, the
     *                    opposite of the coordinates of the new origin in the
     *                    old frame.
     * @return the newly created kinematic transform.
     * @see #of(AbsoluteDate, PVCoordinates, Rotation, Vector3D)
     */
    static KinematicTransform of(final AbsoluteDate date,
                                 final PVCoordinates pvCoordinates) {
        return of(date, pvCoordinates, Rotation.IDENTITY, Vector3D.ZERO);
    }

    /**
     * Create a new kinematic transform from a translation and rotation.
     *
     * @param date        of translation.
     * @param pvCoordinates translation (with rate) to apply, expressed in the old frame. That is, the
     *                    opposite of the coordinates of the new origin in the
     *                    old frame.
     * @param rotation    to apply after the translation. That is after
     *                    translating applying this rotation produces positions
     *                    expressed in the new frame.
     * @param rotationRate rate of rotation
     * @return the newly created kinematic transform.
     * @see #compose(AbsoluteDate, KinematicTransform, KinematicTransform)
     * @see #of(AbsoluteDate, PVCoordinates, Rotation, Vector3D)
     * @see #of(AbsoluteDate, PVCoordinates, Rotation, Vector3D)
     */
    static KinematicTransform of(final AbsoluteDate date, final PVCoordinates pvCoordinates,
                                 final Rotation rotation, final Vector3D rotationRate) {
        return new KinematicTransform() {

            @Override
            public KinematicTransform getInverse() {
                final Rotation r = getRotation();
                final Vector3D rp = r.applyTo(getTranslation());
                final Vector3D pInv = rp.negate();
                final Vector3D crossP      = Vector3D.crossProduct(getRotationRate(), rp);
                final Vector3D vInv        = crossP.subtract(getRotation().applyTo(getVelocity()));
                final Rotation rInv = r.revert();
                return KinematicTransform.of(getDate(), new PVCoordinates(pInv, vInv),
                        rInv, rInv.applyTo(getRotationRate()).negate());
            }

            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            @Override
            public Vector3D getTranslation() {
                return pvCoordinates.getPosition();
            }

            @Override
            public Rotation getRotation() {
                return rotation;
            }

            @Override
            public Vector3D getVelocity() {
                return pvCoordinates.getVelocity();
            }

            @Override
            public Vector3D getRotationRate() {
                return rotationRate;
            }
        };
    }

}
