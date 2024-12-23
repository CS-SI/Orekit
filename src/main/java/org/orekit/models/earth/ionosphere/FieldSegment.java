/* Copyright 2002-2024 CS GROUP
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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;

/** Performs the computation of the coordinates along the integration path.
 * @author Bryan Cazabonne
 * @since 13.0
 */
class FieldSegment<T extends CalculusFieldElement<T>> {

    /** Threshold for zenith segment. */
    private static final double THRESHOLD = 1.0e-3;

    /** Supporting ray. */
    private final FieldRay<T> ray;

    /** Integration start. */
    private final T y;

    /** Odd points offset. */
    private final T g;

    /** Integration step [m]. */
    private final T deltaN;

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
    FieldSegment(final int n, final FieldRay<T> ray,
                 final T s1, final T s2) {

        this.ray = ray;

        // Integration step (Eq. 195)
        deltaN = s2.subtract(s1).divide(n);

        // Eq. 196
        g = deltaN.multiply(0.5773502691896);

        // Eq. 197
        y = s1.add(deltaN.subtract(g).multiply(0.5));

        nbPoints = 2 * n;

    }

    /** Get point along the ray.
     * @param index point index (between O included and {@link #getNbPoints()} excluded)
     * @return point on ray
     * @since 13.0
     */
    public FieldGeodeticPoint<T> getPoint(int index) {

        final int p = index / 2;
        final T   s = y.add(deltaN.multiply(p)).add(g.multiply(index % 2));

        // Heights (Eq. 178)
        final T height = FastMath.sqrt(s.multiply(s).add(ray.getRadius().multiply(ray.getRadius()))).
                         subtract(NeQuickModel.RE);

        if (ray.getRadius().getReal() < THRESHOLD) {
            // zenith segment
            return new FieldGeodeticPoint<>(ray.getLatitude(),  ray.getLongitude(), height);
        } else {
            // Great circle parameters (Eq. 179 to 181)
            final T tanDs = s.divide(ray.getRadius());
            final T cosDs = FastMath.sqrt(tanDs.multiply(tanDs).add(1.0)).reciprocal();
            final T sinDs = tanDs.multiply(cosDs);

            // Latitude (Eq. 182 to 183)
            final T sinLatS =
                ray.getScLat().sin().multiply(cosDs).add(ray.getScLat().cos().multiply(sinDs).multiply(ray.getCosineAz()));
            final T cosLatS = FastMath.sqrt(sinLatS.multiply(sinLatS).negate().add(1.0));

            // Longitude (Eq. 184 to 187)
            final T sinLonS = sinDs.multiply(ray.getSineAz()).multiply(ray.getScLat().cos());
            final T cosLonS = cosDs.subtract(ray.getScLat().sin().multiply(sinLatS));

            return new FieldGeodeticPoint<>(FastMath.atan2(sinLatS, cosLatS),
                                            FastMath.atan2(sinLonS, cosLonS).add(ray.getLongitude()),
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
    public T getInterval() {
        return deltaN;
    }

}
