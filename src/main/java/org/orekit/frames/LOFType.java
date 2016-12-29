/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
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
        protected Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getVelocity(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_K);
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
        protected Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getPosition(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_K);
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
        protected Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getPosition(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_K);
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
        protected Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getPosition(), pv.getMomentum(),
                                Vector3D.MINUS_K, Vector3D.MINUS_J);
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
        protected Rotation rotationFromInertial(final PVCoordinates pv) {
            return new Rotation(pv.getVelocity(), pv.getMomentum(),
                                Vector3D.PLUS_I, Vector3D.PLUS_J);
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

    /** Get the rotation from inertial frame to local orbital frame.
     * @param pv position-velocity of the spacecraft in some inertial frame
     * @return rotation from inertial frame to local orbital frame
     */
    protected abstract Rotation rotationFromInertial(PVCoordinates pv);

}
