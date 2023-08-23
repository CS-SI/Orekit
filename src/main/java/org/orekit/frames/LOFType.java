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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/**
 * Enumerate for different types of Local Orbital Frames.
 *
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @author Vincent Cucchietti
 */
public enum LOFType implements LOF {

    /** Constant for TNW frame
     * (X axis aligned with velocity, Z axis aligned with orbital momentum).
     * <p>
     * The axes of this frame are parallel to the axes of the {@link #VNC}
     * and {@link #NTW} frames:
     * <ul>
     *   <li>X<sub>TNW</sub> =  X<sub>VNC</sub> =  Y<sub>NTW</sub></li>
     *   <li>Y<sub>TNW</sub> = -Z<sub>VNC</sub> = -X<sub>NTW</sub></li>
     *   <li>Z<sub>TNW</sub> =  Y<sub>VNC</sub> =  Z<sub>NTW</sub></li>
     * </ul>
     *
     * @see #VNC
     * @see #NTW
     */
    TNW {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getVelocity(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_K);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return new FieldRotation<>(pv.getVelocity(), pv.getMomentum(),
                                       new FieldVector3D<>(field, Vector3D.PLUS_I),
                                       new FieldVector3D<>(field, Vector3D.PLUS_K));
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.TNW;
        }

    },

    /**
     * Constant for TNW frame considered inertial (X axis aligned with velocity, Z axis aligned with orbital momentum).
     * <p>
     * The axes of this frame are parallel to the axes of the {@link #VNC} and {@link #NTW} frames:
     * <ul>
     *   <li>X<sub>TNW</sub> =  X<sub>VNC</sub> =  Y<sub>NTW</sub></li>
     *   <li>Y<sub>TNW</sub> = -Z<sub>VNC</sub> = -X<sub>NTW</sub></li>
     *   <li>Z<sub>TNW</sub> =  Y<sub>VNC</sub> =  Z<sub>NTW</sub></li>
     * </ul>
     *
     * @see #VNC
     * @see #NTW
     */
    TNW_INERTIAL {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return TNW.rotationFromInertial(pv);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return TNW.rotationFromInertial(field, pv);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isQuasiInertial() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.TNW_INERTIAL;
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
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getPosition(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_K);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return new FieldRotation<>(pv.getPosition(), pv.getMomentum(),
                                       new FieldVector3D<>(field, Vector3D.PLUS_I),
                                       new FieldVector3D<>(field, Vector3D.PLUS_K));
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.QSW;
        }

    },

    /**
     * Constant for QSW frame considered inertial (X axis aligned with position, Z axis aligned with orbital momentum).
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
    QSW_INERTIAL {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return QSW.rotationFromInertial(pv);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return QSW.rotationFromInertial(field, pv);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isQuasiInertial() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.RSW_INERTIAL;
        }

    },

    /** Constant for Local Vertical, Local Horizontal frame
     * (X axis aligned with position, Z axis aligned with orbital momentum).
     * <p>
     * BEWARE! Depending on the background (software used, textbook, community),
     * different incompatible definitions for LVLH are used. This one is consistent
     * with Vallado's book and with AGI's STK. However CCSDS standard, Wertz, and
     * a.i. solutions' FreeFlyer use another definition (see {@link #LVLH_CCSDS}).
     * </p>
     * <p>
     * This frame is also known as the {@link #QSW} frame, both constants are equivalent.
     * </p>
     * <p>
     * The axes of these frames are parallel to the axes of the {@link #LVLH_CCSDS} frame:
     * <ul>
     *   <li>X<sub>LVLH/QSW</sub> = -Z<sub>LVLH_CCSDS</sub></li>
     *   <li>Y<sub>LVLH/QSW</sub> =  X<sub>LVLH_CCSDS</sub></li>
     *   <li>Z<sub>LVLH/QSW</sub> = -Y<sub>LVLH_CCSDS</sub></li>
     * </ul>
     *
     * @see #QSW
     * @see #VVLH
     */
    LVLH {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getPosition(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_K);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return new FieldRotation<>(pv.getPosition(), pv.getMomentum(),
                                       new FieldVector3D<>(field, Vector3D.PLUS_I),
                                       new FieldVector3D<>(field, Vector3D.PLUS_K));
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            throw new OrekitException(OrekitMessages.CCSDS_DIFFERENT_LVLH_DEFINITION);
        }

    },

    /**
     * Constant for Local Vertical, Local Horizontal frame considered inertial (X axis aligned with position, Z axis
     * aligned with orbital momentum).
     * <p>
     * BEWARE! Depending on the background (software used, textbook, community), different incompatible definitions for
     * LVLH are used. This one is consistent with Vallado's book and with AGI's STK. However CCSDS standard, Wertz, and
     * a.i. solutions' FreeFlyer use another definition (see {@link #LVLH_CCSDS}).
     * </p>
     * <p>
     * This frame is also known as the {@link #QSW} frame, both constants are equivalent.
     * </p>
     * <p>
     * The axes of these frames are parallel to the axes of the {@link #LVLH_CCSDS} frame:
     * <ul>
     *   <li>X<sub>LVLH/QSW</sub> = -Z<sub>LVLH_CCSDS</sub></li>
     *   <li>Y<sub>LVLH/QSW</sub> =  X<sub>LVLH_CCSDS</sub></li>
     *   <li>Z<sub>LVLH/QSW</sub> = -Y<sub>LVLH_CCSDS</sub></li>
     * </ul>
     *
     * @see #QSW
     * @see #VVLH
     */
    LVLH_INERTIAL {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return LVLH.rotationFromInertial(pv);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return LVLH.rotationFromInertial(field, pv);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isQuasiInertial() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            throw new OrekitException(OrekitMessages.CCSDS_DIFFERENT_LVLH_DEFINITION);
        }

    },

    /** Constant for Local Vertical, Local Horizontal frame as defined by CCSDS
     * (Z axis aligned with opposite of position, Y axis aligned with opposite of orbital momentum).
     * <p>
     * BEWARE! Depending on the background (software used, textbook, community),
     * different incompatible definitions for LVLH are used. This one is consistent
     * with CCSDS standard, Wertz, and a.i. solutions' FreeFlyer. However Vallado's
     * book and with AGI's STK use another definition (see {@link #LVLH}).
     * </p>
     * <p>
     * The axes of this frame are parallel to the axes of both the {@link #QSW} and {@link #LVLH} frames:
     * <ul>
     *   <li>X<sub>LVLH_CCSDS/VVLH</sub> =  Y<sub>QSW/LVLH</sub></li>
     *   <li>Y<sub>LVLH_CCSDS/VVLH</sub> = -Z<sub>QSW/LVLH</sub></li>
     *   <li>Z<sub>LVLH_CCSDS/VVLH</sub> = -X<sub>QSW/LVLH</sub></li>
     * </ul>
     *
     * @see #QSW
     * @see #LVLH
     * @since 11.0
     */
    LVLH_CCSDS {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getPosition(), pv.getMomentum(),
                                Vector3D.MINUS_K, Vector3D.MINUS_J);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return new FieldRotation<>(pv.getPosition(), pv.getMomentum(),
                                       new FieldVector3D<>(field, Vector3D.MINUS_K),
                                       new FieldVector3D<>(field, Vector3D.MINUS_J));
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.LVLH;
        }

    },

    /**
     * Constant for Local Vertical, Local Horizontal frame as defined by CCSDS considered inertial (Z axis aligned with
     * opposite of position, Y axis aligned with opposite of orbital momentum).
     * <p>
     * BEWARE! Depending on the background (software used, textbook, community), different incompatible definitions for
     * LVLH are used. This one is consistent with CCSDS standard, Wertz, and a.i. solutions' FreeFlyer. However
     * Vallado's book and with AGI's STK use another definition (see {@link #LVLH}).
     * </p>
     * <p>
     * The axes of this frame are parallel to the axes of both the {@link #QSW} and {@link #LVLH} frames:
     * <ul>
     *   <li>X<sub>LVLH_CCSDS/VVLH</sub> =  Y<sub>QSW/LVLH</sub></li>
     *   <li>Y<sub>LVLH_CCSDS/VVLH</sub> = -Z<sub>QSW/LVLH</sub></li>
     *   <li>Z<sub>LVLH_CCSDS/VVLH</sub> = -X<sub>QSW/LVLH</sub></li>
     * </ul>
     *
     * @see #QSW
     * @see #LVLH
     * @since 11.0
     */
    LVLH_CCSDS_INERTIAL {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return LVLH_CCSDS.rotationFromInertial(pv);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return LVLH_CCSDS.rotationFromInertial(field, pv);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isQuasiInertial() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.LVLH_INERTIAL;
        }

    },

    /** Constant for Vehicle Velocity, Local Horizontal frame
     * (Z axis aligned with opposite of position, Y axis aligned with opposite of orbital momentum).
     * <p>
     * This is another name for {@link #LVLH_CCSDS}, kept here for compatibility with STK.
     * </p>
     * <p>
     * Beware that the name is misleading: in the general case (i.e. not perfectly circular),
     * none of the axes is perfectly aligned with velocity! The preferred name for this
     * should be {@link #LVLH_CCSDS}.
     * </p>
     * <p>
     * The axes of this frame are parallel to the axes of both the {@link #QSW} and {@link #LVLH} frames:
     * <ul>
     *   <li>X<sub>LVLH_CCSDS/VVLH</sub> =  Y<sub>QSW/LVLH</sub></li>
     *   <li>Y<sub>LVLH_CCSDS/VVLH</sub> = -Z<sub>QSW/LVLH</sub></li>
     *   <li>Z<sub>LVLH_CCSDS/VVLH</sub> = -X<sub>QSW/LVLH</sub></li>
     * </ul>
     * @see #LVLH_CCSDS
     */
    VVLH {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return LVLH_CCSDS.rotationFromInertial(pv);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return LVLH_CCSDS.rotationFromInertial(field, pv);
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.LVLH;
        }

    },

    /**
     * Constant for Vehicle Velocity, Local Horizontal frame considered inertial (Z axis aligned with opposite of
     * position, Y axis aligned with opposite of orbital momentum).
     * <p>
     * This is another name for {@link #LVLH_CCSDS}, kept here for compatibility with STK.
     * </p>
     * <p>
     * Beware that the name is misleading: in the general case (i.e. not perfectly circular), none of the axes is
     * perfectly aligned with velocity! The preferred name for this should be {@link #LVLH_CCSDS}.
     * </p>
     * <p>
     * The axes of this frame are parallel to the axes of both the {@link #QSW} and {@link #LVLH} frames:
     * <ul>
     *   <li>X<sub>LVLH_CCSDS/VVLH</sub> =  Y<sub>QSW/LVLH</sub></li>
     *   <li>Y<sub>LVLH_CCSDS/VVLH</sub> = -Z<sub>QSW/LVLH</sub></li>
     *   <li>Z<sub>LVLH_CCSDS/VVLH</sub> = -X<sub>QSW/LVLH</sub></li>
     * </ul>
     *
     * @see #LVLH_CCSDS
     */
    VVLH_INERTIAL {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return VVLH.rotationFromInertial(pv);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return VVLH.rotationFromInertial(field, pv);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isQuasiInertial() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.LVLH_INERTIAL;
        }

    },

    /** Constant for Velocity - Normal - Co-normal frame
     * (X axis aligned with velocity, Y axis aligned with orbital momentum).
     * <p>
     * The axes of this frame are parallel to the axes of the {@link #TNW}
     * and {@link #NTW} frames:
     * <ul>
     *   <li>X<sub>VNC</sub> =  X<sub>TNW</sub> = Y<sub>NTW</sub></li>
     *   <li>Y<sub>VNC</sub> =  Z<sub>TNW</sub> = Z<sub>NTW</sub></li>
     *   <li>Z<sub>VNC</sub> = -Y<sub>TNW</sub> = X<sub>NTW</sub></li>
     * </ul>
     *
     * @see #TNW
     * @see #NTW
     */
    VNC {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getVelocity(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_J);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return new FieldRotation<>(pv.getVelocity(), pv.getMomentum(),
                                       new FieldVector3D<>(field, Vector3D.PLUS_I),
                                       new FieldVector3D<>(field, Vector3D.PLUS_J));
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.VNC_ROTATING;
        }

    },

    /**
     * Constant for Velocity - Normal - Co-normal frame considered inertial (X axis aligned with velocity, Y axis
     * aligned with orbital momentum).
     * <p>
     * The axes of this frame are parallel to the axes of the {@link #TNW} and {@link #NTW} frames:
     * <ul>
     *   <li>X<sub>VNC</sub> =  X<sub>TNW</sub> = Y<sub>NTW</sub></li>
     *   <li>Y<sub>VNC</sub> =  Z<sub>TNW</sub> = Z<sub>NTW</sub></li>
     *   <li>Z<sub>VNC</sub> = -Y<sub>TNW</sub> = X<sub>NTW</sub></li>
     * </ul>
     *
     * @see #TNW
     * @see #NTW
     */
    VNC_INERTIAL {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return VNC.rotationFromInertial(pv);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return VNC.rotationFromInertial(field, pv);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isQuasiInertial() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.VNC_INERTIAL;
        }

    },

    /**
     * Constant for Equinoctial Coordinate System (X axis aligned with ascending node, Z axis aligned with orbital
     * momentum).
     *
     * @since 11.0
     */
    EQW {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            final Vector3D m = pv.getMomentum();
            return new Rotation(new Vector3D(-m.getY(), m.getX(), 0), m,
                                Vector3D.PLUS_I, Vector3D.PLUS_J);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            final FieldVector3D<T> m = pv.getMomentum();
            return new FieldRotation<>(new FieldVector3D<>(m.getY().negate(), m.getX(), field.getZero()),
                                       m,
                                       new FieldVector3D<>(field, Vector3D.PLUS_I),
                                       new FieldVector3D<>(field, Vector3D.PLUS_J));
        }

        /** {@inheritDoc} */
        @Override
        public boolean isQuasiInertial() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.EQW_INERTIAL;
        }

    },

    /** Constant for Transverse Velocity Normal coordinate system
     * (Y axis aligned with velocity, Z axis aligned with orbital momentum).
     * <p>
     * The axes of this frame are parallel to the axes of the {@link #TNW}
     * and {@link #VNC} frames:
     * <ul>
     *   <li>X<sub>NTW</sub> = -Y<sub>TNW</sub> = Z<sub>VNC</sub></li>
     *   <li>Y<sub>NTW</sub> =  X<sub>TNW</sub> = X<sub>VNC</sub></li>
     *   <li>Z<sub>NTW</sub> =  Z<sub>TNW</sub> = Y<sub>VNC</sub></li>
     * </ul>
     * @see #TNW
     * @see #VNC
     * @since 11.0
     */
    NTW {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getVelocity(), pv.getMomentum(),
                                Vector3D.PLUS_J, Vector3D.PLUS_K);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return new FieldRotation<>(pv.getVelocity(), pv.getMomentum(),
                                       new FieldVector3D<>(field, Vector3D.PLUS_J),
                                       new FieldVector3D<>(field, Vector3D.PLUS_K));
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.NTW_ROTATING;
        }

    },

    /**
     * Constant for Transverse Velocity Normal coordinate system considered inertial (Y axis aligned with velocity, Z
     * axis aligned with orbital momentum).
     * <p>
     * The axes of this frame are parallel to the axes of the {@link #TNW} and {@link #VNC} frames:
     * <ul>
     *   <li>X<sub>NTW</sub> = -Y<sub>TNW</sub> = Z<sub>VNC</sub></li>
     *   <li>Y<sub>NTW</sub> =  X<sub>TNW</sub> = X<sub>VNC</sub></li>
     *   <li>Z<sub>NTW</sub> =  Z<sub>TNW</sub> = Y<sub>VNC</sub></li>
     * </ul>
     *
     * @see #TNW
     * @see #VNC
     * @since 11.0
     */
    NTW_INERTIAL {
        /** {@inheritDoc} */
        @Override
        public Rotation rotationFromInertial(final PVCoordinates pv) {
            return NTW.rotationFromInertial(pv);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                         final FieldPVCoordinates<T> pv) {
            return NTW.rotationFromInertial(field, pv);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isQuasiInertial() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public OrbitRelativeFrame toOrbitRelativeFrame() {
            return OrbitRelativeFrame.NTW_INERTIAL;
        }

    };

    /** {@inheritDoc} */
    public String getName() {
        return this.name();
    };

    /**
     * Get the rotation from input to output {@link LOFType local orbital frame}.
     * <p>
     * This rotation does not include any time derivatives. If first time derivatives (i.e. rotation rate) is needed as well,
     * the full {@link #transformFromLOFInToLOFOut(LOF, LOF, AbsoluteDate, PVCoordinates)} method must be called and
     * the complete rotation transform must be extracted from it.
     *
     * @param in input commonly used local orbital frame
     * @param out output commonly used local orbital frame
     * @param pv position-velocity of the spacecraft in some inertial frame
     *
     * @return rotation from input to output local orbital frame
     */
    static Rotation rotationFromLOFInToLOFOut(final LOFType in, final LOFType out, final PVCoordinates pv) {
        return out.rotationFromLOF(in, pv);
    }

    /**
     * Get the rotation from input to output {@link LOFType local orbital frame}.
     * <p>
     * This rotation does not include any time derivatives. If first time derivatives (i.e. rotation rate) is needed as well,
     * the full {@link #transformFromLOFInToLOFOut(LOF, LOF, FieldAbsoluteDate, FieldPVCoordinates)}  method must be called and
     * the complete rotation transform must be extracted from it.
     *
     * @param field field to which the elements belong
     * @param in input commonly used local orbital frame
     * @param out output commonly used local orbital frame
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param <T> type of the field elements
     *
     * @return rotation from input to output local orbital frame
     */
    static <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromLOFInToLOFOut(final Field<T> field,
                                                                                          final LOFType in,
                                                                                          final LOFType out,
                                                                                          final FieldPVCoordinates<T> pv) {
        return out.rotationFromLOF(field, in, pv);
    }

    /**
     * Get the rotation from input {@link LOF local orbital frame} to the instance.
     * <p>
     * This rotation does not include any time derivatives. If first time derivatives (i.e. rotation rate) is needed as well,
     * the full {@link #transformFromLOF(LOF, AbsoluteDate, PVCoordinates)} method must be called and the complete rotation
     * transform must be extracted from it.
     *
     * @param fromLOF input local orbital frame
     * @param pv position-velocity of the spacecraft in some inertial frame
     *
     * @return rotation from input local orbital frame to the instance
     */
    public Rotation rotationFromLOF(final LOFType fromLOF, final PVCoordinates pv) {

        // First compute the rotation from the input LOF to the pivot inertial
        final Rotation fromLOFToInertial = fromLOF.rotationFromInertial(pv).revert();

        // Then compute the rotation from the pivot inertial to the output LOF
        final Rotation inertialToThis = this.rotationFromInertial(pv);

        // Output composed rotation
        return fromLOFToInertial.compose(inertialToThis, RotationConvention.FRAME_TRANSFORM);
    }

    /**
     * Get the rotation from input {@link LOFType local orbital frame} to the instance.
     * <p>
     * This rotation does not include any time derivatives. If first time derivatives (i.e. rotation rate) is needed as well,
     * the full {@link #transformFromLOF(LOF, FieldAbsoluteDate, FieldPVCoordinates)} method must be called and the complete
     * rotation transform must be extracted from it.
     *
     * @param field field to which the elements belong
     * @param fromLOF input local orbital frame
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param <T> type of the field elements
     *
     * @return rotation from input local orbital frame to the instance
     */
    public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromLOF(final Field<T> field,
                                                                                final LOFType fromLOF,
                                                                                final FieldPVCoordinates<T> pv) {

        // First compute the rotation from the input LOF to the pivot inertial
        final FieldRotation<T> fromLOFToInertial = fromLOF.rotationFromInertial(field, pv).revert();

        // Then compute the rotation from the pivot inertial to the output LOF
        final FieldRotation<T> inertialToThis = this.rotationFromInertial(field, pv);

        // Output composed rotation
        return fromLOFToInertial.compose(inertialToThis, RotationConvention.FRAME_TRANSFORM);
    }

    /**
     * {@inheritDoc} It is unnecessary to use this method when dealing with {@link LOFType}, use
     * {@link #rotationFromInertial(PVCoordinates)} instead.
     */
    @Override
    public Rotation rotationFromInertial(final AbsoluteDate date, final PVCoordinates pv) {
        return rotationFromInertial(pv);
    }

    /**
     * Get the rotation from inertial frame to local orbital frame.
     * <p>
     * This rotation does not include any time derivatives. If first time derivatives (i.e. rotation rate) is needed as well,
     * the full {@link #transformFromInertial(AbsoluteDate, PVCoordinates)} method must be called and
     * the complete rotation transform must be extracted from it.
     * </p>
     *
     * @param pv position-velocity of the spacecraft in some inertial frame
     *
     * @return rotation from inertial frame to local orbital frame
     */
    public abstract Rotation rotationFromInertial(PVCoordinates pv);

    /**
     * {@inheritDoc} It is unnecessary to use this method when dealing with {@link LOFType}, use
     * {@link #rotationFromInertial(Field, FieldPVCoordinates)} instead.
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(final Field<T> field,
                                                                                     final FieldAbsoluteDate<T> date,
                                                                                     final FieldPVCoordinates<T> pv) {
        return rotationFromInertial(field, pv);
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
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @param <T> type of the field elements
     *
     * @return rotation from inertial frame to local orbital frame
     */
    public abstract <T extends CalculusFieldElement<T>> FieldRotation<T> rotationFromInertial(Field<T> field,
                                                                                              FieldPVCoordinates<T> pv);

    /**
     * Convert current local orbital frame to CCSDS equivalent orbit relative frame when possible, null otherwise.
     *
     * @return CCSDS equivalent orbit relative frame when possible, null otherwise
     *
     * @see OrbitRelativeFrame
     */
    public abstract OrbitRelativeFrame toOrbitRelativeFrame();

}
