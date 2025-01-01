/* Copyright 2002-2025 CS GROUP
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
package org.orekit.models.earth.ionosphere.nequick;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;

/** Performs the computation of the coordinates along the integration path.
 * @author Bryan Cazabonne
 * @since 13.0
 */
public class Segment {

    /** Threshold for zenith segment. */
    private static final double THRESHOLD = 1.0;

    /** Supporting ray. */
    private final Ray ray;

    /** Integration start. */
    private final double y;

    /** Odd points offset. */
    private final double g;

    /** Integration step [m]. */
    private final double deltaN;

    /** Number of points. */
    private final int nbPoints;

    /**
     * Constructor.
     *
     * @param n   number of intervals for integration (2 points per interval, hence 2n points will be generated)
     * @param ray ray-perigee parameters
     * @param s1  lower boundary of integration
     * @param s2  upper boundary for integration
     */
    public Segment(final int n, final Ray ray, final double s1, final double s2) {

        this.ray = ray;

        // Integration step (Eq. 195)
        deltaN = (s2 - s1) / n;

        // Eq. 196
        g = 0.5773502691896 * deltaN;

        // Eq. 197
        y = s1 + (deltaN - g) * 0.5;

        nbPoints = 2 * n;

    }

    /** Get point along the ray.
     * @param index point index (between O included and {@link #getNbPoints()} excluded)
     * @return point on ray
     * @since 13.0
     */
    public GeodeticPoint getPoint(int index) {

        final int    p = index / 2;
        final double s = y + p * deltaN + (index % 2) * g;

        // Heights (Eq. 178)
        final double height = FastMath.sqrt(s * s + ray.getRadius() * ray.getRadius()) -
                              NeQuickModel.RE;

        if (ray.getRadius() < THRESHOLD) {
            // zenith segment
            return new GeodeticPoint(ray.getLatitude(), ray.getLongitude(), height);
        } else {
            // Great circle parameters (Eq. 179 to 181)
            final double tanDs = s / ray.getRadius();
            final double cosDs = 1.0 / FastMath.sqrt(1.0 + tanDs * tanDs);
            final double sinDs = tanDs * cosDs;

            // Latitude (Eq. 182 to 183)
            final double sinLatS = ray.getScLat().sin() * cosDs + ray.getScLat().cos() * sinDs * ray.getCosineAz();
            final double cosLatS = FastMath.sqrt(1.0 - sinLatS * sinLatS);

            // Longitude (Eq. 184 to 187)
            final double sinLonS = sinDs * ray.getSineAz() * ray.getScLat().cos();
            final double cosLonS = cosDs - ray.getScLat().sin() * sinLatS;

            return new GeodeticPoint(FastMath.atan2(sinLatS, cosLatS),
                                     FastMath.atan2(sinLonS, cosLonS) + ray.getLongitude(),
                                     height);
        }
    }

    /** Get number of points.
     * <p>
     * Note there are 2 points per interval, so {@code index} must be between 0 (included)
     * and 2n (excluded) for a segment built with {@code n} intervals
     * </p>
     * @return number of points
     */
    public int getNbPoints() {
        return nbPoints;
    }

    /**
     * Get the integration step.
     *
     * @return the integration step in meters
     */
    public double getInterval() {
        return deltaN;
    }

}
