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
package org.orekit.geometry.fov;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.propagation.events.VisibilityTrigger;

/** Class representing a spacecraft sensor Field Of View with circular shape.
 * <p>The field of view is defined by an axis and an half-aperture angle.</p>
 * @author Luc Maisonobe
 * @since 10.1
 */
public class CircularFieldOfView extends SmoothFieldOfView {

    /** FOV half aperture angle. */
    private final double halfAperture;

    /** Scaled X axis defining FoV boundary. */
    private final Vector3D scaledX;

    /** Scaled Y axis defining FoV boundary. */
    private final Vector3D scaledY;

    /** Scaled Z axis defining FoV boundary. */
    private final Vector3D scaledZ;

    /** Build a new instance.
     * @param center direction of the FOV center, in spacecraft frame
     * @param halfAperture FOV half aperture angle
     * @param margin angular margin to apply to the zone (if positive,
     * the Field Of View will consider points slightly outside of the
     * zone are still visible)
     */
    public CircularFieldOfView(final Vector3D center, final double halfAperture,
                               final double margin) {

        super(center, center.orthogonal(), margin);
        this.halfAperture = halfAperture;

        // precompute utility vectors for walking around the FoV
        final SinCos sc = FastMath.sinCos(halfAperture);
        scaledX   = new Vector3D(sc.sin(), getX());
        scaledY   = new Vector3D(sc.sin(), getY());
        scaledZ   = new Vector3D(sc.cos(), getZ());

    }

    /** get the FOV half aperture angle.
     * @return FOV half aperture angle
     */
    public double getHalfAperture() {
        return halfAperture;
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromBoundary(final Vector3D lineOfSight, final double angularRadius,
                                     final VisibilityTrigger trigger) {
        return Vector3D.angle(getCenter(), lineOfSight) - halfAperture +
               trigger.radiusCorrection(angularRadius) - getMargin();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D projectToBoundary(final Vector3D lineOfSight) {
        return directionAt(FastMath.atan2(Vector3D.dotProduct(lineOfSight, getY()),
                                          Vector3D.dotProduct(lineOfSight, getX())));
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D directionAt(final double angle) {
        final SinCos sc = FastMath.sinCos(angle);
        return new Vector3D(sc.cos(), scaledX, sc.sin(), scaledY, 1.0, scaledZ);
    }

}
