/* Copyright 2022-2024 Romain Serra
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A transform that only includes translation and rotation as well as their respective rates.
 * It is kinematic in the sense that it cannot transform an acceleration vector.
 *
 * @author Romain Serra
 * @see FieldStaticTransform
 * @see FieldTransform
 * @see KinematicTransform
 * @since 12.1
 */
public interface FieldKinematicTransform<T extends CalculusFieldElement<T>> extends FieldStaticTransform<T> {

    /**
     * Get the identity kinematic transform.
     *
     * @param <T> type of the elements
     * @param field field used by default
     * @return identity transform.
     */
    static <T extends CalculusFieldElement<T>> FieldKinematicTransform<T> getIdentity(final Field<T> field) {
        return FieldTransform.getIdentity(field);
    }

    /** Compute a composite velocity.
     * @param first first applied transform
     * @param second second applied transform
     * @param <T> the type of the field elements
     * @return velocity part of the composite transform
     */
    static <T extends CalculusFieldElement<T>> FieldVector3D<T> compositeVelocity(final FieldKinematicTransform<T> first,
                                                                                  final FieldKinematicTransform<T> second) {

        final FieldVector3D<T> v1 = first.getVelocity();
        final FieldRotation<T> r1 = first.getRotation();
        final FieldVector3D<T> o1 = first.getRotationRate();
        final FieldVector3D<T> p2 = second.getTranslation();
        final FieldVector3D<T> v2 = second.getVelocity();

        final FieldVector3D<T> crossP = FieldVector3D.crossProduct(o1, p2);

        return v1.add(r1.applyInverseTo(v2.add(crossP)));
    }

    /** Compute a composite rotation rate.
     * @param <T> type of the elements
     * @param first first applied transform
     * @param second second applied transform
     * @return rotation rate part of the composite transform
     */
    static <T extends CalculusFieldElement<T>> FieldVector3D<T> compositeRotationRate(final FieldKinematicTransform<T> first,
                                                                                      final FieldKinematicTransform<T> second) {

        final FieldVector3D<T> o1 = first.getRotationRate();
        final FieldRotation<T> r2 = second.getRotation();
        final FieldVector3D<T> o2 = second.getRotationRate();

        return o2.add(r2.applyTo(o1));
    }

    /** Transform {@link PVCoordinates}, without the acceleration vector.
     * @param pv the position-velocity couple to transform.
     * @return transformed position-velocity
     */
    default FieldPVCoordinates<T> transformOnlyPV(final FieldPVCoordinates<T> pv) {
        final FieldVector3D<T> transformedP = transformPosition(pv.getPosition());
        final FieldVector3D<T> crossP       = FieldVector3D.crossProduct(getRotationRate(), transformedP);
        final FieldVector3D<T> transformedV = getRotation().applyTo(pv.getVelocity().add(getVelocity())).subtract(crossP);
        return new FieldPVCoordinates<>(transformedP, transformedV);
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
    default TimeStampedFieldPVCoordinates<T> transformOnlyPV(final TimeStampedFieldPVCoordinates<T> pv) {
        final FieldVector3D<T> transformedP = transformPosition(pv.getPosition());
        final FieldVector3D<T> crossP       = FieldVector3D.crossProduct(getRotationRate(), transformedP);
        final FieldVector3D<T> transformedV = getRotation().applyTo(pv.getVelocity().add(getVelocity())).subtract(crossP);
        return new TimeStampedFieldPVCoordinates<>(pv.getDate(), transformedP, transformedV,
                FieldVector3D.getZero(pv.getDate().getField()));
    }

    /** Get the first time derivative of the translation.
     * @return first time derivative of the translation
     * @see #getTranslation()
     */
    FieldVector3D<T> getVelocity();

    /** Get the first time derivative of the rotation.
     * <p>The norm represents the angular rate.</p>
     * @return First time derivative of the rotation
     * @see #getRotation()
     */
    FieldVector3D<T> getRotationRate();

    /**
     * Get the inverse transform of the instance.
     *
     * @return inverse transform of the instance
     */
    FieldKinematicTransform<T> getInverse();

    /**
     * Build a transform by combining two existing ones.
     * <p>
     * Note that the dates of the two existing transformed are <em>ignored</em>,
     * and the combined transform date is set to the date supplied in this
     * constructor without any attempt to shift the raw transforms. This is a
     * design choice allowing user full control of the combination.
     * </p>
     *
     * @param <T> type of the elements
     * @param date   date of the transform
     * @param first  first transform applied
     * @param second second transform applied
     * @return the newly created kinematic transform that has the same effect as
     * applying {@code first}, then {@code second}.
     * @see #of(FieldAbsoluteDate, FieldPVCoordinates, FieldRotation, FieldVector3D)
     */
    static <T extends CalculusFieldElement<T>> FieldKinematicTransform<T> compose(final FieldAbsoluteDate<T> date,
                                                                                  final FieldKinematicTransform<T> first,
                                                                                  final FieldKinematicTransform<T> second) {
        final FieldVector3D<T> composedTranslation = FieldStaticTransform.compositeTranslation(first, second);
        final FieldVector3D<T> composedTranslationRate = FieldKinematicTransform.compositeVelocity(first, second);
        return of(date, new FieldPVCoordinates<>(composedTranslation, composedTranslationRate),
                FieldStaticTransform.compositeRotation(first, second),
                FieldKinematicTransform.compositeRotationRate(first, second));
    }

    /**
     * Create a new kinematic transform from a rotation and zero, constant translation.
     *
     * @param <T> type of the elements
     * @param date     of translation.
     * @param rotation to apply after the translation. That is after translating
     *                 applying this rotation produces positions expressed in
     *                 the new frame.
     * @param rotationRate rate of rotation
     * @return the newly created kinematic transform.
     * @see #of(FieldAbsoluteDate, FieldPVCoordinates, FieldRotation, FieldVector3D)
     */
    static <T extends CalculusFieldElement<T>> FieldKinematicTransform<T> of(final FieldAbsoluteDate<T> date,
                                                                             final FieldRotation<T> rotation,
                                                                             final FieldVector3D<T> rotationRate) {
        return of(date, FieldPVCoordinates.getZero(date.getField()), rotation, rotationRate);
    }

    /**
     * Create a new kinematic transform from a translation and its rate.
     *
     * @param <T> type of the elements
     * @param date        of translation.
     * @param pvCoordinates translation (with rate) to apply, expressed in the old frame. That is, the
     *                    opposite of the coordinates of the new origin in the
     *                    old frame.
     * @return the newly created kinematic transform.
     * @see #of(FieldAbsoluteDate, FieldPVCoordinates, FieldRotation, FieldVector3D)
     */
    static <T extends CalculusFieldElement<T>> FieldKinematicTransform<T> of(final FieldAbsoluteDate<T> date,
                                                                             final FieldPVCoordinates<T> pvCoordinates) {
        final Field<T> field = date.getField();
        return of(date, pvCoordinates, FieldRotation.getIdentity(field), FieldVector3D.getZero(field));
    }

    /**
     * Create a new kinematic transform from a non-Field version.
     *
     * @param <T> type of the elements
     * @param field field.
     * @param kinematicTransform non-Field kinematic transform
     * @return the newly created kinematic transform.
     * @see #of(FieldAbsoluteDate, FieldPVCoordinates, FieldRotation, FieldVector3D)
     */
    static <T extends CalculusFieldElement<T>> FieldKinematicTransform<T> of(final Field<T> field,
                                                                             final KinematicTransform kinematicTransform) {
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, kinematicTransform.getDate());
        final FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(field,
            new PVCoordinates(kinematicTransform.getTranslation(), kinematicTransform.getVelocity()));
        final FieldRotation<T> rotation = new FieldRotation<>(field, kinematicTransform.getRotation());
        final FieldVector3D<T> rotationRate = new FieldVector3D<>(field, kinematicTransform.getRotationRate());
        return of(date, pvCoordinates, rotation, rotationRate);
    }

    /**
     * Create a new kinematic transform from a translation and rotation.
     *
     * @param <T> type of the elements
     * @param date        of translation.
     * @param pvCoordinates translation (with rate) to apply, expressed in the old frame. That is, the
     *                    opposite of the coordinates of the new origin in the
     *                    old frame.
     * @param rotation    to apply after the translation. That is after
     *                    translating applying this rotation produces positions
     *                    expressed in the new frame.
     * @param rotationRate rate of rotation
     * @return the newly created kinematic transform.
     * @see #compose(FieldAbsoluteDate, FieldKinematicTransform, FieldKinematicTransform)
     * @see #of(FieldAbsoluteDate, FieldPVCoordinates, FieldRotation, FieldVector3D)
     * @see #of(FieldAbsoluteDate, FieldPVCoordinates, FieldRotation, FieldVector3D)
     */
    static <T extends CalculusFieldElement<T>> FieldKinematicTransform<T> of(final FieldAbsoluteDate<T> date,
                                                                          final FieldPVCoordinates<T> pvCoordinates,
                                                                          final FieldRotation<T> rotation,
                                                                          final FieldVector3D<T> rotationRate) {
        return new FieldKinematicTransform<T>() {

            @Override
            public FieldKinematicTransform<T> getInverse() {
                final FieldRotation<T> r = getRotation();
                final FieldVector3D<T> rp = r.applyTo(getTranslation());
                final FieldVector3D<T> pInv = rp.negate();
                final FieldVector3D<T> crossP      = FieldVector3D.crossProduct(getRotationRate(), rp);
                final FieldVector3D<T> vInv        = crossP.subtract(getRotation().applyTo(getVelocity()));
                final FieldRotation<T> rInv = r.revert();
                return FieldKinematicTransform.of(date, new FieldPVCoordinates<>(pInv, vInv),
                        rInv, rInv.applyTo(getRotationRate()).negate());
            }

            @Override
            public AbsoluteDate getDate() {
                return date.toAbsoluteDate();
            }

            @Override
            public FieldAbsoluteDate<T> getFieldDate() {
                return date;
            }

            @Override
            public FieldVector3D<T> getTranslation() {
                return pvCoordinates.getPosition();
            }

            @Override
            public FieldRotation<T> getRotation() {
                return rotation;
            }

            @Override
            public FieldVector3D<T> getVelocity() {
                return pvCoordinates.getVelocity();
            }

            @Override
            public FieldVector3D<T> getRotationRate() {
                return rotationRate;
            }
        };
    }

}
