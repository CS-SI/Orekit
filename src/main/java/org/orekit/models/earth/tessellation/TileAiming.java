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

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;

/** Interface defining the aiming direction of {@link Tile tiles}.
 * @author Luc Maisonobe
 */
public interface TileAiming {

    /** Get points at which aiming direction cannot be computed.
     * <p>
     * As per Brouwer's <a href="http://mathworld.wolfram.com/HairyBallTheorem.html">hairy
     * ball theorem</a>, any vector field on the 2-sphere has at least one zero.
     * This implies that any implementation of this interface has at least one point
     * where the aiming direction cannot be computed. The most typical example is aiming
     * always towards North pole, for which both poles are singular points.
     * </p>
     * @return a non-empty (as per hairy ball theorem) list of points where aiming direction
     * is either zero or cannot be computed
     * @since 10.0
     */
    List<GeodeticPoint> getSingularPoints();

    /** Find the along tile direction for tessellation at specified point.
     * @param point point on the ellipsoid (Cartesian coordinates)
     * @param gp point on the ellipsoid (geodetic coordinates)
     * @return normalized along tile direction
     */
    Vector3D alongTileDirection(Vector3D point, GeodeticPoint gp);

}
