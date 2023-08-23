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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldLine;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeStamped;

/**
 * A transform that only includes translation and rotation. It is static in the
 * sense that no rates thereof are included.
 *
 * @param <T> the type of the field elements
 * @author Bryan Cazabonne
 * @see FieldTransform
 * @since 12.0
 */
public interface FieldStaticTransform<T extends CalculusFieldElement<T>> extends TimeStamped {

    /**
     * Get the identity static transform.
     *
     * @param <T> type of the elements
     * @param field field used by default
     * @return identity transform.
     */
    static <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getIdentity(final Field<T> field) {
        return FieldTransform.getIdentity(field);
    }

    /**
     * Transform a position vector (including translation effects).
     *
     * @param position vector to transform
     * @return transformed position
     */
    default FieldVector3D<T> transformPosition(final Vector3D position) {
        return getRotation().applyTo(getTranslation().add(position));
    }

    /**
     * Transform a position vector (including translation effects).
     *
     * @param position vector to transform
     * @return transformed position
     */
    default FieldVector3D<T> transformPosition(final FieldVector3D<T> position) {
        return getRotation().applyTo(position.add(getTranslation()));
    }

    /**
     * Transform a vector (ignoring translation effects).
     *
     * @param vector vector to transform
     * @return transformed vector
     */
    default FieldVector3D<T> transformVector(final Vector3D vector) {
        return getRotation().applyTo(vector);
    }

    /**
     * Transform a vector (ignoring translation effects).
     *
     * @param vector vector to transform
     * @return transformed vector
     */
    default FieldVector3D<T> transformVector(final FieldVector3D<T> vector) {
        return getRotation().applyTo(vector);
    }

    /**
     * Transform a line.
     *
     * @param line to transform
     * @return transformed line
     */
    default FieldLine<T> transformLine(final Line line) {
        final FieldVector3D<T> transformedP0 = transformPosition(line.getOrigin());
        final FieldVector3D<T> transformedP1 = transformPosition(line.pointAt(1.0e6));
        return new FieldLine<>(transformedP0, transformedP1, line.getTolerance());
    }

    /**
     * Transform a line.
     *
     * @param line to transform
     * @return transformed line
     */
    default FieldLine<T> transformLine(final FieldLine<T> line) {
        final FieldVector3D<T> transformedP0 = transformPosition(line.getOrigin());
        final FieldVector3D<T> transformedP1 = transformPosition(line.pointAt(1.0e6));
        return new FieldLine<>(transformedP0, transformedP1, line.getTolerance());
    }

    /**
     * Get the underlying elementary translation.
     * <p>A transform can be uniquely represented as an elementary
     * translation followed by an elementary rotation. This method returns this
     * unique elementary translation.</p>
     *
     * @return underlying elementary translation
     */
    FieldVector3D<T> getTranslation();

    /**
     * Get the underlying elementary rotation.
     * <p>A transform can be uniquely represented as an elementary
     * translation followed by an elementary rotation. This method returns this
     * unique elementary rotation.</p>
     *
     * @return underlying elementary rotation
     */
    FieldRotation<T> getRotation();

    /**
     * Get the inverse transform of the instance.
     *
     * @return inverse transform of the instance
     */
    FieldStaticTransform<T> getInverse();

    /**
     * Build a transform by combining two existing ones.
     * <p>
     * Note that the dates of the two existing transformed are <em>ignored</em>,
     * and the combined transform date is set to the date supplied in this
     * constructor without any attempt to shift the raw transforms. This is a
     * design choice allowing user full control of the combination.
     * </p>
     *
     * @param <T>    type of the elements
     * @param date   date of the transform
     * @param first  first transform applied
     * @param second second transform applied
     * @return the newly created static transform that has the same effect as
     * applying {@code first}, then {@code second}.
     * @see #of(FieldAbsoluteDate, FieldVector3D, FieldRotation)
     */
    static <T extends CalculusFieldElement<T>> FieldStaticTransform<T> compose(final FieldAbsoluteDate<T> date,
                                                                               final FieldStaticTransform<T> first,
                                                                               final FieldStaticTransform<T> second) {
        return of(date,
                  compositeTranslation(first, second),
                  compositeRotation(first, second));
    }

    /**
     * Compute a composite translation.
     *
     * @param first first applied transform
     * @param second second applied transform
     * @param <T> the type of the field elements
     * @return translation part of the composite transform
     */
    static <T extends CalculusFieldElement<T>> FieldVector3D<T> compositeTranslation(final FieldStaticTransform<T> first,
                                                                                     final FieldStaticTransform<T> second) {

        final FieldVector3D<T> p1 = first.getTranslation();
        final FieldRotation<T> r1 = first.getRotation();
        final FieldVector3D<T> p2 = second.getTranslation();

        return p1.add(r1.applyInverseTo(p2));

    }

    /**
     * Compute a composite rotation.
     *
     * @param first first applied transform
     * @param second second applied transform
     * @param <T> the type of the field elements
     * @return rotation part of the composite transform
     */
    static <T extends CalculusFieldElement<T>> FieldRotation<T> compositeRotation(final FieldStaticTransform<T> first,
                                                                                  final FieldStaticTransform<T> second) {
        final FieldRotation<T> r1 = first.getRotation();
        final FieldRotation<T> r2 = second.getRotation();
        return r1.compose(r2, RotationConvention.FRAME_TRANSFORM);
    }

    /**
     * Create a new static transform from a rotation and zero translation.
     *
     * @param <T>         type of the elements
     * @param date     of translation.
     * @param rotation to apply after the translation. That is after translating
     *                 applying this rotation produces positions expressed in
     *                 the new frame.
     * @return the newly created static transform.
     * @see #of(FieldAbsoluteDate, FieldVector3D, FieldRotation)
     */
    static <T extends CalculusFieldElement<T>> FieldStaticTransform<T> of(final FieldAbsoluteDate<T> date,
                                                                          final FieldRotation<T> rotation) {
        return of(date, FieldVector3D.getZero(date.getField()), rotation);
    }

    /**
     * Create a new static transform from a translation and rotation.
     *
     * @param <T>         type of the elements
     * @param date        of translation.
     * @param translation to apply, expressed in the old frame. That is, the
     *                    opposite of the coordinates of the new origin in the
     *                    old frame.
     * @return the newly created static transform.
     * @see #of(FieldAbsoluteDate, FieldVector3D, FieldRotation)
     */
    static <T extends CalculusFieldElement<T>> FieldStaticTransform<T> of(final FieldAbsoluteDate<T> date,
                                                                          final FieldVector3D<T> translation) {
        return of(date, translation, FieldRotation.getIdentity(date.getField()));
    }

    /**
     * Create a new static transform from an {@link FieldAbsoluteDate} and a {@link StaticTransform}.
     *
     * @param <T>         type of the elements
     * @param date        of translation.
     * @param staticTransform to apply
     * @return the newly created static transform.
     * @see #of(FieldAbsoluteDate, FieldVector3D, FieldRotation)
     */
    static <T extends CalculusFieldElement<T>> FieldStaticTransform<T> of(final FieldAbsoluteDate<T> date,
                                                                          final StaticTransform staticTransform) {
        return of(date,
                  new FieldVector3D<>(date.getField(), staticTransform.getTranslation()),
                  new FieldRotation<>(date.getField(), staticTransform.getRotation()));
    }

    /**
     * Create a new static transform from a translation and rotation.
     *
     * @param <T>         type of the elements
     * @param date        of translation.
     * @param translation to apply, expressed in the old frame. That is, the
     *                    opposite of the coordinates of the new origin in the
     *                    old frame.
     * @param rotation    to apply after the translation. That is after
     *                    translating applying this rotation produces positions
     *                    expressed in the new frame.
     * @return the newly created static transform.
     * @see #compose(FieldAbsoluteDate, FieldStaticTransform, FieldStaticTransform)
     * @see #of(FieldAbsoluteDate, FieldRotation)
     * @see #of(FieldAbsoluteDate, FieldVector3D)
     */
    static <T extends CalculusFieldElement<T>> FieldStaticTransform<T> of(final FieldAbsoluteDate<T> date,
                                                                          final FieldVector3D<T> translation,
                                                                          final FieldRotation<T> rotation) {
        return new FieldStaticTransform<T>() {

            @Override
            public FieldStaticTransform<T> getInverse() {
                final FieldRotation<T> r = getRotation();
                final FieldVector3D<T> rp = r.applyTo(getTranslation());
                final FieldVector3D<T> pInv = rp.negate();
                return FieldStaticTransform.of(date, pInv, rotation.revert());
            }

            @Override
            public AbsoluteDate getDate() {
                return date.toAbsoluteDate();
            }

            @Override
            public FieldVector3D<T> getTranslation() {
                return translation;
            }

            @Override
            public FieldRotation<T> getRotation() {
                return rotation;
            }

        };
    }

}
