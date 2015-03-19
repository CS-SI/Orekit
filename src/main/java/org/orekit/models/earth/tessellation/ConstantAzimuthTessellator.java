/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.models.earth.tessellation;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;

/** Ellipsoid tessellator aligning tiles with respect to a geographic azimuth.
 * @see AlongTrackTessellator
 * @author Luc Maisonobe
 */
public class ConstantAzimuthTessellator extends EllipsoidTessellator {

    /** Cosine of the azimuth. */
    private final double cos;

    /** Sine of the azimuth. */
    private final double sin;

    /** Simple constructor.
     * @param ellipsoid ellipsoid body on which the zone is defined
     * @param fullWidth full tiles width as a distance on surface, including overlap (in meters)
     * @param fullLength full tiles length as a distance on surface, including overlap (in meters)
     * @param widthOverlap overlap between adjacent tiles (in meters)
     * @param lengthOverlap overlap between adjacent tiles (in meters)
     * @param azimuth geographic azimuth of the tiles
     */
    public ConstantAzimuthTessellator(final OneAxisEllipsoid ellipsoid,
                                      final double fullWidth, final double fullLength,
                                      final double widthOverlap, final double lengthOverlap,
                                      final double azimuth) {
        super(ellipsoid, fullWidth, fullLength, widthOverlap, lengthOverlap);
        this.cos = FastMath.cos(azimuth);
        this.sin = FastMath.sin(azimuth);
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D alongTileDirection(final Vector3D point, final GeodeticPoint gp) {

        // compute the horizontal direction at fixed azimuth
        return new Vector3D(cos, gp.getNorth(), sin, gp.getEast());

    }

}
