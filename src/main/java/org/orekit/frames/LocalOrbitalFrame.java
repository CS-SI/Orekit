/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.apache.commons.math.geometry.euclidean.threed.Rotation;
import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Class for frames moving with an orbiting satellite.
 * <p>There are two main local orbital frames:</p>
 * <ul>
 *   <li>the (t, n, w) frame has its X axis along velocity (tangential), its
 *   Z axis along orbital momentum and its Y axis completes the right-handed
 *   trihedra (it is roughly pointing towards the central body)</li>
 *   <li>the (q, s, w) frame has its X axis along position (radial), its
 *   Z axis along orbital momentum and its Y axis completes the right-handed
 *   trihedra (it is roughly along velocity)</li>
 * </ul>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class LocalOrbitalFrame extends Frame {

    /** List of supported frames types. */
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
         * </p>
         * @see #VNC
         */
        TNW,

        /** Constant for QSW frame
         * (X axis aligned with position, Z axis aligned with orbital momentum).
         * <p>
         * This frame is also known as the {@link LVLH} frame, both constants are equivalent.
         * </p>
         * <p>
         * The axes of these frames are parallel to the axes of the {@link #VVLH} frame:
         * <ul>
         *   <li>X<sub>QSW/LVLH</sub> = -Z<sub>VVLH</sub></li>
         *   <li>Y<sub>QSW/LVLH</sub> =  X<sub>VVLH</sub></li>
         *   <li>Z<sub>QSW/LVLH</sub> = -Y<sub>VVLH</sub></li>
         * </ul>
         * </p>
         * @see #LVLH
         * @see #VVLH
         */
        QSW,

        /** Constant for Local Vertical, Local Horizontal frame
         * (X axis aligned with position, Z axis aligned with orbital momentum).
         * <p>
         * This frame is also known as the {@link QSW} frame, both constants are equivalent.
         * </p>
         * <p>
         * The axes of these frames are parallel to the axes of the {@link #VVLH} frame:
         * <ul>
         *   <li>X<sub>LVLH/QSW</sub> = -Z<sub>VVLH</sub></li>
         *   <li>Y<sub>LVLH/QSW</sub> =  X<sub>VVLH</sub></li>
         *   <li>Z<sub>LVLH/QSW</sub> = -Y<sub>VVLH</sub></li>
         * </ul>
         * </p>
         * @see #QSW
         * @see #VVLH
         */
        LVLH,

        /** Constant for Vehicle Velocity, Local Horizontal frame
         * (Z axis aligned with opposite of position, Y axis aligned with opposite of orbital momentum).
         * <p>
         * The axes of this frame are parallel to the axes of both the {@link #QSW} and {@link #LVLH} frames:
         * <ul>
         *   <li>X<sub>VVLH</sub> =  Y<sub>QSW/LVLH</sub></li>
         *   <li>Y<sub>VVLH</sub> = -Z<sub>QSW/LVLH</sub></li>
         *   <li>Z<sub>VVLH</sub> = -X<sub>QSW/LVLH</sub></li>
         * </ul>
         * </p>
         * @see #QSW
         * @see #LVLH
         */
        VVLH,

        /** Constant for Velocity - Normal - Co-normal frame
         * (X axis aligned with velocity, Y axis aligned with orbital momentum).
         * <p>
         * The axes of this frame are parallel to the axes of the {@link #TNW} frame:
         * <ul>
         *   <li>X<sub>VNC</sub> =  X<sub>TNW</sub></li>
         *   <li>Y<sub>VNC</sub> =  Z<sub>TNW</sub></li>
         *   <li>Z<sub>VNC</sub> = -Y<sub>TNW</sub></li>
         * </ul>
         * </p>
         * @see #TNW
         */
        VNC;

    }

    /** Serializable UID. */
    private static final long serialVersionUID = 4815246887625815981L;

    /** Frame type. */
    private final LOFType type;

    /** Provider used to compute frame motion. */
    private final PVCoordinatesProvider provider;

    /** Build a new instance.
     * @param parent parent frame (must be non-null)
     * @param type frame type
     * @param provider provider used to compute frame motion
     * @param name name of the frame
     * @exception IllegalArgumentException if the parent frame is null
     */
    public LocalOrbitalFrame(final Frame parent, final LOFType type,
                             final PVCoordinatesProvider provider,
                             final String name)
        throws IllegalArgumentException {
        super(parent, Transform.IDENTITY, name, false);
        this.type     = type;
        this.provider = provider;
    }

    /** {@inheritDoc} */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        // get position/velocity with respect to parent frame
        final PVCoordinates pv = provider.getPVCoordinates(date, getParent());
        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();
        final Vector3D momentum = pv.getMomentum();

        // compute the translation part of the transform
        final Transform translation = new Transform(p.negate(), v.negate());

        // compute the rotation part of the transform
        final Rotation r;
        switch (type) {
        case TNW:
            r = new Rotation(v, momentum, Vector3D.PLUS_I, Vector3D.PLUS_K);
            break;
        case QSW :
        case LVLH :
            r = new Rotation(p, momentum, Vector3D.PLUS_I, Vector3D.PLUS_K);
            break;
        case VVLH :
            r = new Rotation(p, momentum, Vector3D.MINUS_K, Vector3D.MINUS_J);
            break;
        case VNC :
            r = new Rotation(v, momentum, Vector3D.PLUS_I, Vector3D.PLUS_J);
            break;
        default :
            // this should never happen
            throw OrekitException.createInternalError(null);
        }
        final Transform rotation =
            new Transform(r, new Vector3D(1.0 / p.getNormSq(), r.applyTo(momentum)));

        // update the frame defining transform
        setTransform(new Transform(translation, rotation));

    }

}
