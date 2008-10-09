/* Copyright 2002-2008 CS Communication & Systèmes
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

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Class for frames moving with an orbiting satellite.
 * <p>There are two main local orbital frames:</p>
 * <ul>
 *   <li>the (t, n, w) frame has its X axis along velocity (tangential), its
 *   Z axis along orbital momentum and its Y axis completes the right-handed
 *   trihedra (it is roughly pointing towards the central body)</li>
 *   <li>the (q, s, w) frame has its X axis along position (radial), its
 *   Z axis along orbital momentum and its Y axis completes the right-handed
 *   trihedra (it is roughly along velocity)</li>
 * <ul>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class LocalOrbitalFrame extends Frame {

    /** List of supported frames types. */
    public enum LOFType {

        /** Constant for TNW frame (X axis aligned with velocity). */
        TNW,

        /** Constant for QSW frame (X axis aligned with position). */
        QSW

    }

    /** Serialiazable UID. */
    private static final long serialVersionUID = 5413386845678794203L;

    /** Frame type. */
    private final LOFType type;

    /** Propagator used to compute frame motion. */
    private final Propagator propagator;

    /** Build a new instance.
     * @param parent parent frame (must be non-null)
     * @param type frame type
     * @param propagator propagator used to compute frame motion
     * @param name name of the frame
     * @exception IllegalArgumentException if the parent frame is null
     */
    public LocalOrbitalFrame(final Frame parent, final LOFType type,
                             final Propagator propagator, final String name)
        throws IllegalArgumentException {
        super(parent, Transform.IDENTITY, name);
        this.type       = type;
        this.propagator = propagator;
    }

    /** {@inheritDoc} */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        // get position/velocity with respect to parent frame
        final PVCoordinates pv = propagator.propagate(date).getPVCoordinates(getParent());
        final Vector3D p = pv.getPosition();
        final Vector3D v = pv.getVelocity();
        final Vector3D momentum = Vector3D.crossProduct(p, v);

        // compute the translation part of the transform
        final Transform translation = new Transform(p.negate(), v.negate());

        // compute the rotation part of the transform
        final Transform rotation =
            new Transform(new Rotation((type == LOFType.TNW) ? v : p, momentum,
                                        Vector3D.PLUS_I, Vector3D.PLUS_K),
                          new Vector3D(1.0 / p.getNormSq(), momentum));

        // update the frame defining transform
        setTransform(new Transform(translation, rotation));

    }

}
