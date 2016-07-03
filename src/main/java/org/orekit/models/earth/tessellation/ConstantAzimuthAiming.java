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
package org.orekit.models.earth.tessellation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;

/** Class used to orient tiles with respect to a geographic azimuth.
 * @see AlongTrackAiming
 * @author Luc Maisonobe
 */
public class ConstantAzimuthAiming implements TileAiming {

    /** Cosine of the azimuth. */
    private final double cos;

    /** Sine of the azimuth. */
    private final double sin;

    /** Simple constructor.
     * @param ellipsoid ellipsoid body on which the zone is defined
     * @param azimuth geographic azimuth of the tiles
     */
    public ConstantAzimuthAiming(final OneAxisEllipsoid ellipsoid, final double azimuth) {
        this.cos = FastMath.cos(azimuth);
        this.sin = FastMath.sin(azimuth);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D alongTileDirection(final Vector3D point, final GeodeticPoint gp) {

        // compute the horizontal direction at fixed azimuth
        return new Vector3D(cos, gp.getNorth(), sin, gp.getEast());

    }

}
