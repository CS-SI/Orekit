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
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/**
 * Interface for local orbital frame.
 *
 * @author Vincent Cucchietti
 */
public interface LOF {

    /**
     * Get the rotation from input to output {@link LOF local orbital frame}.
     * <p>
     * This rotation does not include any time derivatives. If first time derivatives (i.e. rotation rate) is needed as well,
     * the full {@link #transformFromLOFInToLOFOut(LOF, LOF, FieldAbsoluteDate, FieldPVCoordinates)} method must be called and
     * the complete rotation transform must be extracted from it.
     *
     * @param field field to which the elements belong
     * @param in input commonly used local orbital frame
     * @param out output commonly used local orbital frame
     * @param date date of the rotation
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param <T> type of the field elements
     *
     * @return rotation from input to output local orbital frame
     *
     * @since 11.3
     */
    static <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromLOFInToLOFOut(final Field<T> field,
                                                                                          final LOF in, final LOF out,
                                                                                          final FieldAbsoluteDate<T> date,
                                                                                          final FieldPVCoordinates<T> pv) {
        return out.rotationFromLOF(field, in, date, pv);
    }

    /**
     * Get the transform from input to output {@link LOF local orbital frame}.
     *
     * @param in input commonly used local orbital frame
     * @param out output commonly used local orbital frame
     * @param date date of the transform
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param <T> type of the field elements
     *
     * @return rotation from input to output local orbital frame.
     *
     * @since 11.3
     */
    static <T extends CalculusFieldElement<T>> FieldTransform<T> transformFromLOFInToLOFOut(final LOF in, final LOF out,
                                                                                            final FieldAbsoluteDate<T> date,
                                                                                            final FieldPVCoordinates<T> pv) {
        return out.transformFromLOF(in, date, pv);
    }

    /**
     * Get the rotation from input to output {@link LOF local orbital frame}.
     * <p>
     * This rotation does not include any time derivatives. If first time derivatives (i.e. rotation rate) is needed as well,
     * the full {@link #transformFromLOFInToLOFOut(LOF, LOF, AbsoluteDate, PVCoordinates)}   method must be called and
     * the complete rotation transform must be extracted from it.
     *
     * @param in input commonly used local orbital frame
     * @param out output commonly used local orbital frame
     * @param date date of the rotation
     * @param pv position-velocity of the spacecraft in some inertial frame
     *
     * @return rotation from input to output local orbital frame.
     *
     * @since 11.3
     */
    static Rotation rotationFromLOFInToLOFOut(final LOF in, final LOF out, final AbsoluteDate date, final PVCoordinates pv) {
        return out.rotationFromLOF(in, date, pv);
    }

    /**
     * Get the transform from input to output {@link LOF local orbital frame}.
     *
     * @param in input commonly used local orbital frame
     * @param out output commonly used local orbital frame
     * @param date date of the transform
     * @param pv position-velocity of the spacecraft in some inertial frame
     *
     * @return rotation from input to output local orbital frame
     *
     * @since 11.3
     */
    static Transform transformFromLOFInToLOFOut(final LOF in, final LOF out, final AbsoluteDate date,
                                                final PVCoordinates pv) {
        return out.transformFromLOF(in, date, pv);
    }

    /**
     * Get the rotation from input {@link LOF local orbital frame} to the instance.
     * <p>
     * This rotation does not include any time derivatives. If first time derivatives (i.e. rotation rate) is needed as well,
     * the full {@link #transformFromLOF(LOF, FieldAbsoluteDate, FieldPVCoordinates)}   method must be called and
     * the complete rotation transform must be extracted from it.
     *
     * @param field field to which the elements belong
     * @param fromLOF input local orbital frame
     * @param date date of the rotation
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param <T> type of the field elements
     *
     * @return rotation from input local orbital frame to the instance
     *
     * @since 11.3
     */
    default <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromLOF(final Field<T> field,
                                                                                 final LOF fromLOF,
                                                                                 final FieldAbsoluteDate<T> date,
                                                                                 final FieldPVCoordinates<T> pv) {

        // First compute the rotation from the input LOF to the pivot inertial
        final FieldRotation<T> fromLOFToInertial = fromLOF.rotationFromInertial(field, date, pv).revert();

        // Then compute the rotation from the pivot inertial to the output LOF
        final FieldRotation<T> inertialToThis = this.rotationFromInertial(field, date, pv);

        // Output composed rotation
        return fromLOFToInertial.compose(inertialToThis, RotationConvention.FRAME_TRANSFORM);
    }

    /**
     * Get the rotation from input {@link LOF commonly used local orbital frame} to the instance.
     *
     * @param fromLOF input local orbital frame
     * @param date date of the transform
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param <T> type of the field elements
     *
     * @return rotation from input local orbital frame to the instance
     *
     * @since 11.3
     */
    default <T extends CalculusFieldElement<T>> FieldTransform<T> transformFromLOF(final LOF fromLOF,
                                                                                   final FieldAbsoluteDate<T> date,
                                                                                   final FieldPVCoordinates<T> pv) {

        // Get transform from input local orbital frame to inertial
        final FieldTransform<T> fromLOFToInertial = fromLOF.transformFromInertial(date, pv).getInverse();

        // Get transform from inertial to output local orbital frame
        final FieldTransform<T> inertialToLOFOut = this.transformFromInertial(date, pv);

        // Output composition of both transforms
        return new FieldTransform<>(date, fromLOFToInertial, inertialToLOFOut);
    }

    /**
     * Get the transform from an inertial frame defining position-velocity and the local orbital frame.
     *
     * @param date current date
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param <T> type of the fields elements
     *
     * @return transform from the frame where position-velocity are defined to local orbital frame
     *
     * @since 9.0
     */
    default <T extends CalculusFieldElement<T>> FieldTransform<T> transformFromInertial(final FieldAbsoluteDate<T> date,
                                                                                        final FieldPVCoordinates<T> pv) {

        // compute the translation part of the transform
        final FieldTransform<T> translation = new FieldTransform<>(date, pv.negate());

        // compute the rotation part of the transform
        final FieldRotation<T> r        = rotationFromInertial(date.getField(), date, pv);
        final FieldVector3D<T> p        = pv.getPosition();
        final FieldVector3D<T> momentum = pv.getMomentum();
        final FieldTransform<T> rotation = new FieldTransform<>(date, r,
                                                                new FieldVector3D<>(p.getNormSq().reciprocal(),
                                                                                    r.applyTo(momentum)));

        final FieldTransform<T> transform = new FieldTransform<>(date, translation, rotation);

        // If LOF is considered pseudo-inertial, freeze transform
        return isQuasiInertial() ? transform.freeze() : transform;

    }

    /**
     * Get the rotation from inertial frame to local orbital frame.
     * <p>
     * This rotation does not include any time derivatives. If first time derivatives (i.e. rotation rate) is needed as well,
     * the full {@link #transformFromInertial(FieldAbsoluteDate, FieldPVCoordinates)} method must be
     * called and the complete rotation transform must be extracted from it.
     * </p>
     *
     * @param field field to which the elements belong
     * @param date date of the rotation
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param <T> type of the field elements
     *
     * @return rotation from inertial frame to local orbital frame
     *
     * @since 9.0
     */
    <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(Field<T> field, FieldAbsoluteDate<T> date,
                                                                              FieldPVCoordinates<T> pv);

    /**
     * Get the rotation from input {@link LOF local orbital frame} to the instance.
     * <p>
     * This rotation does not include any time derivatives. If first time derivatives (i.e. rotation rate) is needed as well,
     * the full {@link #transformFromLOF(LOF, AbsoluteDate, PVCoordinates)}  method must be called and
     * the complete rotation transform must be extracted from it.
     *
     * @param fromLOF input local orbital frame
     * @param date date of the rotation
     * @param pv position-velocity of the spacecraft in some inertial frame
     *
     * @return rotation from input local orbital frame to the instance
     *
     * @since 11.3
     */
    default Rotation rotationFromLOF(final LOF fromLOF, final AbsoluteDate date, final PVCoordinates pv) {

        // First compute the rotation from the input LOF to the pivot inertial
        final Rotation fromLOFToInertial = fromLOF.rotationFromInertial(date, pv).revert();

        // Then compute the rotation from the pivot inertial to the output LOF
        final Rotation inertialToThis = this.rotationFromInertial(date, pv);

        // Output composed rotation
        return fromLOFToInertial.compose(inertialToThis, RotationConvention.FRAME_TRANSFORM);
    }

    /**
     * Get the rotation from input {@link LOF local orbital frame} to the instance.
     *
     * @param fromLOF input local orbital frame
     * @param date date of the transform
     * @param pv position-velocity of the spacecraft in some inertial frame
     *
     * @return rotation from input local orbital frame to the instance
     *
     * @since 11.3
     */
    default Transform transformFromLOF(final LOF fromLOF, final AbsoluteDate date, final PVCoordinates pv) {

        // First compute the rotation from the input LOF to the pivot inertial
        final Transform fromLOFToInertial = fromLOF.transformFromInertial(date, pv).getInverse();

        // Then compute the rotation from the pivot inertial to the output LOF
        final Transform inertialToThis = this.transformFromInertial(date, pv);

        // Output composed rotation
        return new Transform(date, fromLOFToInertial, inertialToThis);
    }

    /**
     * Get the transform from an inertial frame defining position-velocity and the local orbital frame.
     *
     * @param date current date
     * @param pv position-velocity of the spacecraft in some inertial frame
     *
     * @return transform from the frame where position-velocity are defined to local orbital frame
     */
    default Transform transformFromInertial(final AbsoluteDate date, final PVCoordinates pv) {

        // compute the translation part of the transform
        final Transform translation = new Transform(date, pv.negate());

        // compute the rotation part of the transform
        final Rotation  r        = rotationFromInertial(date, pv);
        final Vector3D  p        = pv.getPosition();
        final Vector3D  momentum = pv.getMomentum();
        final Transform rotation = new Transform(date, r, new Vector3D(1.0 / p.getNormSq(), r.applyTo(momentum)));

        final Transform transform = new Transform(date, translation, rotation);

        // If LOF is considered pseudo-inertial, freeze transform
        return isQuasiInertial() ? transform.freeze() : transform;

    }

    /**
     * Get the rotation from inertial frame to local orbital frame.
     * <p>
     * This rotation does not include any time derivatives. If first time derivatives (i.e. rotation rate) is needed as well,
     * the full {@link #transformFromInertial(AbsoluteDate, PVCoordinates) transformFromInertial} method must be called and
     * the complete rotation transform must be extracted from it.
     *
     * @param date date of the rotation
     * @param pv position-velocity of the spacecraft in some inertial frame
     *
     * @return rotation from inertial frame to local orbital frame
     */
    Rotation rotationFromInertial(AbsoluteDate date, PVCoordinates pv);

    /** Get flag that indicates if current local orbital frame shall be treated as pseudo-inertial.
     * @return flag that indicates if current local orbital frame shall be treated as pseudo-inertial
     */
    default boolean isQuasiInertial() {
        return false;
    }

    /** Get name of the local orbital frame.
     * @return name of the local orbital frame
     */
    String getName();

}
