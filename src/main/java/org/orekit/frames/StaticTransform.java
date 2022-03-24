/* Contributed in the public domain.
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
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/**
 * A transform that only includes translation and rotation. It is static in the
 * sense that no rates thereof are included.
 *
 * @author Evan Ward
 * @see Transform
 * @since 11.2
 */
public interface StaticTransform extends TimeStamped {

    /**
     * Get the identity static transform.
     *
     * @return identity transform.
     */
    static StaticTransform getIdentity() {
        return Transform.IDENTITY;
    }

    /**
     * Transform a position vector (including translation effects).
     *
     * @param position vector to transform
     * @return transformed position
     */
    default Vector3D transformPosition(final Vector3D position) {
        return getRotation().applyTo(getTranslation().add(position));
    }

    /**
     * Transform a position vector (including translation effects).
     *
     * @param position vector to transform
     * @param <T>      the type of the field elements
     * @return transformed position
     */
    default  <T extends CalculusFieldElement<T>> FieldVector3D<T> transformPosition(
            final FieldVector3D<T> position) {
        return FieldRotation.applyTo(getRotation(), position.add(getTranslation()));
    }

    /**
     * Transform a vector (ignoring translation effects).
     *
     * @param vector vector to transform
     * @return transformed vector
     */
    default  Vector3D transformVector(final Vector3D vector) {
        return getRotation().applyTo(vector);
    }

    /**
     * Transform a vector (ignoring translation effects).
     *
     * @param vector vector to transform
     * @param <T>    the type of the field elements
     * @return transformed vector
     */
    default <T extends CalculusFieldElement<T>> FieldVector3D<T> transformVector(
            final FieldVector3D<T> vector) {
        return FieldRotation.applyTo(getRotation(), vector);
    }

    /**
     * Transform a line.
     *
     * @param line to transform
     * @return transformed line
     */
    default Line transformLine(final Line line) {
        final Vector3D transformedP0 = transformPosition(line.getOrigin());
        final Vector3D transformedD  = transformVector(line.getDirection());
        return Line.fromDirection(transformedP0, transformedD, line.getTolerance());
    }

    /**
     * Get the underlying elementary translation.
     * <p>A transform can be uniquely represented as an elementary
     * translation followed by an elementary rotation. This method returns this
     * unique elementary translation.</p>
     *
     * @return underlying elementary translation
     */
    Vector3D getTranslation();

    /**
     * Get the underlying elementary rotation.
     * <p>A transform can be uniquely represented as an elementary
     * translation followed by an elementary rotation. This method returns this
     * unique elementary rotation.</p>
     *
     * @return underlying elementary rotation
     */
    Rotation getRotation();

    /**
     * Get the inverse transform of the instance.
     *
     * @return inverse transform of the instance
     */
    StaticTransform getInverse();

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
     * @return the newly created static transform that has the same effect as
     * applying {@code first}, then {@code second}.
     * @see #of(AbsoluteDate, Vector3D, Rotation)
     */
    static StaticTransform compose(final AbsoluteDate date,
                                   final StaticTransform first,
                                   final StaticTransform second) {
        return of(date,
                compositeTranslation(first, second),
                compositeRotation(first, second));
    }

    /**
     * Compute a composite translation.
     *
     * @param first  first applied transform
     * @param second second applied transform
     * @return translation part of the composite transform
     */
    static Vector3D compositeTranslation(
            final StaticTransform first,
            final StaticTransform second) {
        final Vector3D p1 = first.getTranslation();
        final Rotation r1 = first.getRotation();
        final Vector3D p2 = second.getTranslation();

        return p1.add(r1.applyInverseTo(p2));
    }

    /**
     * Compute a composite rotation.
     *
     * @param first  first applied transform
     * @param second second applied transform
     * @return rotation part of the composite transform
     */
    static Rotation compositeRotation(final StaticTransform first,
                                      final StaticTransform second) {
        final Rotation r1 = first.getRotation();
        final Rotation r2 = second.getRotation();
        return r1.compose(r2, RotationConvention.FRAME_TRANSFORM);

    }

    /**
     * Create a new static transform from a rotation and zero translation.
     *
     * @param date     of translation.
     * @param rotation to apply after the translation. That is after translating
     *                 applying this rotation produces positions expressed in
     *                 the new frame.
     * @return the newly created static transform.
     * @see #of(AbsoluteDate, Vector3D, Rotation)
     */
    static StaticTransform of(final AbsoluteDate date,
                              final Rotation rotation) {
        return of(date, Vector3D.ZERO, rotation);
    }

    /**
     * Create a new static transform from a translation and rotation.
     *
     * @param date        of translation.
     * @param translation to apply, expressed in the old frame. That is, the
     *                    opposite of the coordinates of the new origin in the
     *                    old frame.
     * @return the newly created static transform.
     * @see #of(AbsoluteDate, Vector3D, Rotation)
     */
    static StaticTransform of(final AbsoluteDate date,
                              final Vector3D translation) {
        return of(date, translation, Rotation.IDENTITY);
    }

    /**
     * Create a new static transform from a translation and rotation.
     *
     * @param date        of translation.
     * @param translation to apply, expressed in the old frame. That is, the
     *                    opposite of the coordinates of the new origin in the
     *                    old frame.
     * @param rotation    to apply after the translation. That is after
     *                    translating applying this rotation produces positions
     *                    expressed in the new frame.
     * @return the newly created static transform.
     * @see #compose(AbsoluteDate, StaticTransform, StaticTransform)
     * @see #of(AbsoluteDate, Rotation)
     * @see #of(AbsoluteDate, Vector3D)
     */
    static StaticTransform of(final AbsoluteDate date,
                              final Vector3D translation,
                              final Rotation rotation) {
        return new StaticTransform() {

            @Override
            public StaticTransform getInverse() {
                final Rotation r = getRotation();
                final Vector3D rp = r.applyTo(getTranslation());
                final Vector3D pInv = rp.negate();
                return StaticTransform.of(date, pInv, rotation.revert());
            }

            @Override
            public AbsoluteDate getDate() {
                return date;
            }

            @Override
            public Vector3D getTranslation() {
                return translation;
            }

            @Override
            public Rotation getRotation() {
                return rotation;
            }

        };
    }

}
