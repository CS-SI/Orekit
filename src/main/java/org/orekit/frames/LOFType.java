/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/** Enumerate for different types of Local Orbital Frames.
 * @author Luc Maisonobe
 */
public enum LOFType {

    /** Constant for TNW frame
     * (X axis aligned with velocity, Z axis aligned with orbital momentum).
     * <p>
     * The axes of this frame are parallel to the axes of the {@link #VNC} frame:
     * <ul>
     *   <li>X<sub>TNW</sub> =  X<sub>VNC</sub></li>
     *   <li>Y<sub>TNW</sub> = -Z<sub>VNC</sub></li>
     *   <li>Z<sub>TNW</sub> =  Y<sub>VNC</sub></li>
     * </ul>
     *
     * @see #VNC
     */
    TNW {

        /** {@inheritDoc} */
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getVelocity(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_K);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                     final FieldPVCoordinates<T> pv) {
            return new FieldRotation<>(pv.getVelocity(), pv.getMomentum(),
                                       new FieldVector3D<>(field, Vector3D.PLUS_I),
                                       new FieldVector3D<>(field, Vector3D.PLUS_K));
        }

    },

    /** Constant for QSW frame
     * (X axis aligned with position, Z axis aligned with orbital momentum).
     * <p>
     * This frame is also known as the {@link #LVLH} frame, both constants are equivalent.
     * </p>
     * <p>
     * The axes of these frames are parallel to the axes of the {@link #VVLH} frame:
     * <ul>
     *   <li>X<sub>QSW/LVLH</sub> = -Z<sub>VVLH</sub></li>
     *   <li>Y<sub>QSW/LVLH</sub> =  X<sub>VVLH</sub></li>
     *   <li>Z<sub>QSW/LVLH</sub> = -Y<sub>VVLH</sub></li>
     * </ul>
     *
     * @see #LVLH
     * @see #VVLH
     */
    QSW {

        /** {@inheritDoc} */
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getPosition(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_K);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                     final FieldPVCoordinates<T> pv) {
            return new FieldRotation<>(pv.getPosition(), pv.getMomentum(),
                                       new FieldVector3D<>(field, Vector3D.PLUS_I),
                                       new FieldVector3D<>(field, Vector3D.PLUS_K));
        }

    },

    /** Constant for Local Vertical, Local Horizontal frame
     * (X axis aligned with position, Z axis aligned with orbital momentum).
     * <p>
     * This frame is also known as the {@link #QSW} frame, both constants are equivalent.
     * </p>
     * <p>
     * The axes of these frames are parallel to the axes of the {@link #VVLH} frame:
     * <ul>
     *   <li>X<sub>LVLH/QSW</sub> = -Z<sub>VVLH</sub></li>
     *   <li>Y<sub>LVLH/QSW</sub> =  X<sub>VVLH</sub></li>
     *   <li>Z<sub>LVLH/QSW</sub> = -Y<sub>VVLH</sub></li>
     * </ul>
     *
     * @see #QSW
     * @see #VVLH
     */
    LVLH {

        /** {@inheritDoc} */
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getPosition(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_K);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                     final FieldPVCoordinates<T> pv) {
            return new FieldRotation<>(pv.getPosition(), pv.getMomentum(),
                                       new FieldVector3D<>(field, Vector3D.PLUS_I),
                                       new FieldVector3D<>(field, Vector3D.PLUS_K));
        }

    },

    /** Constant for Vehicle Velocity, Local Horizontal frame
     * (Z axis aligned with opposite of position, Y axis aligned with opposite of orbital momentum).
     * <p>
     * The axes of this frame are parallel to the axes of both the {@link #QSW} and {@link #LVLH} frames:
     * <ul>
     *   <li>X<sub>VVLH</sub> =  Y<sub>QSW/LVLH</sub></li>
     *   <li>Y<sub>VVLH</sub> = -Z<sub>QSW/LVLH</sub></li>
     *   <li>Z<sub>VVLH</sub> = -X<sub>QSW/LVLH</sub></li>
     * </ul>
     *
     * @see #QSW
     * @see #LVLH
     */
    VVLH {

        /** {@inheritDoc} */
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getPosition(), pv.getMomentum(),
                                Vector3D.MINUS_K, Vector3D.MINUS_J);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                     final FieldPVCoordinates<T> pv) {
            return new FieldRotation<>(pv.getPosition(), pv.getMomentum(),
                                       new FieldVector3D<>(field, Vector3D.MINUS_K),
                                       new FieldVector3D<>(field, Vector3D.MINUS_J));
        }

    },

    /** Constant for Velocity - Normal - Co-normal frame
     * (X axis aligned with velocity, Y axis aligned with orbital momentum).
     * <p>
     * The axes of this frame are parallel to the axes of the {@link #TNW} frame:
     * <ul>
     *   <li>X<sub>VNC</sub> =  X<sub>TNW</sub></li>
     *   <li>Y<sub>VNC</sub> =  Z<sub>TNW</sub></li>
     *   <li>Z<sub>VNC</sub> = -Y<sub>TNW</sub></li>
     * </ul>
     *
     * @see #TNW
     */
    VNC {

        /** {@inheritDoc} */
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getVelocity(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_J);
        }

        @Override
        public <T extends RealFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                     final FieldPVCoordinates<T> pv) {
            return new FieldRotation<>(pv.getVelocity(), pv.getMomentum(),
                                       new FieldVector3D<>(field, Vector3D.PLUS_I),
                                       new FieldVector3D<>(field, Vector3D.PLUS_J));
        }

    };

    /** Get the transform from an inertial frame defining position-velocity and the local orbital frame.
     * @param date current date
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @return transform from the frame where position-velocity are defined to local orbital frame
     */
    public Transform transformFromInertial(final AbsoluteDate date, final PVCoordinates pv) {

        // compute the translation part of the transform
        final Transform translation = new Transform(date, pv.negate());

        // compute the rotation part of the transform
        final Rotation r = rotationFromInertial(pv);
        final Vector3D p = pv.getPosition();
        final Vector3D momentum = pv.getMomentum();
        final Transform rotation =
                new Transform(date, r, new Vector3D(1.0 / p.getNormSq(), r.applyTo(momentum)));

        return new Transform(date, translation, rotation);

    }

    /** Get the transform from an inertial frame defining position-velocity and the local orbital frame.
     * @param date current date
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param <T> type of the fiels elements
     * @return transform from the frame where position-velocity are defined to local orbital frame
     * @since 9.0
     */
    public <T extends RealFieldElement<T>> FieldTransform<T> transformFromInertial(final FieldAbsoluteDate<T> date,
                                                                                   final FieldPVCoordinates<T> pv) {

        // compute the translation part of the transform
        final FieldTransform<T> translation = new FieldTransform<>(date, pv.negate());

        // compute the rotation part of the transform
        final FieldRotation<T> r = rotationFromInertial(date.getField(), pv);
        final FieldVector3D<T> p = pv.getPosition();
        final FieldVector3D<T> momentum = pv.getMomentum();
        final FieldTransform<T> rotation =
                new FieldTransform<T>(date, r, new FieldVector3D<>(p.getNormSq().reciprocal(), r.applyTo(momentum)));

        return new FieldTransform<>(date, translation, rotation);

    }

    /** Get the rotation from inertial frame to local orbital frame.
     * <p>
     * This rotation does not include any time derivatives. If first
     * time derivatives (i.e. rotation rate) is needed as well, the full
     * {@link #transformFromInertial(AbsoluteDate, PVCoordinates) transformFromInertial}
     * method must be called and the complete rotation transform must be extracted
     * from it.
     * </p>
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @return rotation from inertial frame to local orbital frame
     */
    public abstract Rotation rotationFromInertial(PVCoordinates pv);

    /** Get the rotation from inertial frame to local orbital frame.
     * <p>
     * This rotation does not include any time derivatives. If first
     * time derivatives (i.e. rotation rate) is needed as well, the full
     * {@link #transformFromInertial(FieldAbsoluteDate, FieldPVCoordinates) transformFromInertial}
     * method must be called and the complete rotation transform must be extracted
     * from it.
     * </p>
     * @param field field to which the elements belong
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param <T> type of the field elements
     * @return rotation from inertial frame to local orbital frame
     * @since 9.0
     */
    public abstract <T extends RealFieldElement<T>> FieldRotation<T> rotationFromInertial(Field<T> field,
                                                                                          FieldPVCoordinates<T> pv);

}
