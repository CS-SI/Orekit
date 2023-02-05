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
package org.orekit.models.earth.tessellation;

import java.util.Arrays;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;

/** Class used to orient tiles with respect to a geographic azimuth.
 * @see AlongTrackAiming
 * @see DivertedSingularityAiming
 * @author Luc Maisonobe
 */
public class ConstantAzimuthAiming implements TileAiming {

    /** Sine and Cosine of the azimuth. */
    private final SinCos sc;

    /** Simple constructor.
     * @param ellipsoid ellipsoid body on which the zone is defined
     * @param azimuth geographic azimuth of the tiles
     */
    public ConstantAzimuthAiming(final OneAxisEllipsoid ellipsoid, final double azimuth) {
        this.sc = FastMath.sinCos(azimuth);
    }

    /** {@inheritDoc} */
    @Override
    public List<GeodeticPoint> getSingularPoints() {
        return Arrays.asList(GeodeticPoint.NORTH_POLE, GeodeticPoint.SOUTH_POLE);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D alongTileDirection(final Vector3D point, final GeodeticPoint gp) {

        // compute the horizontal direction at fixed azimuth
        return new Vector3D(sc.cos(), gp.getNorth(), sc.sin(), gp.getEast());

    }

}
