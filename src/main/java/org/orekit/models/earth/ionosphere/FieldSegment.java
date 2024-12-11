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
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;

/** Performs the computation of the coordinates along the integration path.
 * @author Bryan Cazabonne
 * @since 13.0
 */
class FieldSegment<T extends CalculusFieldElement<T>> {

    /** Threshold for zenith segment. */
    private static final double THRESHOLD = 1.0e-3;

    /** Latitudes [rad]. */
    private final T[] latitudes;

    /** Longitudes [rad]. */
    private final T[] longitudes;

    /** Heights [m]. */
    private final T[] heights;

    /** Integration step [m]. */
    private final T deltaN;

    /**
     * Constructor.
     *
     * @param field field of the elements
     * @param n     number of points used for the integration
     * @param ray   ray-perigee parameters
     */
    FieldSegment(final Field<T> field, final int n, final FieldRay<T> ray) {
        // Integration en points
        final T s1 = ray.getS1();
        final T s2 = ray.getS2();

        // Integration step (Eq. 195)
        this.deltaN = s2.subtract(s1).divide(n);

        // Segments
        final T[] s = getSegments(field, n, s1);

        // Useful parameters
        final T rp = ray.getRadius();
        final FieldSinCos<T> scLatP = FastMath.sinCos(ray.getLatitude());

        // Geodetic coordinates
        final int size = s.length;
        heights = MathArrays.buildArray(field, size);
        latitudes = MathArrays.buildArray(field, size);
        longitudes = MathArrays.buildArray(field, size);
        for (int i = 0; i < size; i++) {
            // Heights (Eq. 178)
            heights[i] = FastMath.sqrt(s[i].multiply(s[i]).add(rp.multiply(rp))).subtract(NeQuickModel.RE);

            if (rp.getReal() < THRESHOLD) {
                // zenith segment
                latitudes[i] = ray.getLatitude();
                longitudes[i] = ray.getLongitude();
            } else {
                // Great circle parameters (Eq. 179 to 181)
                final T tanDs = s[i].divide(rp);
                final T cosDs = FastMath.sqrt(tanDs.multiply(tanDs).add(1.0)).reciprocal();
                final T sinDs = tanDs.multiply(cosDs);

                // Latitude (Eq. 182 to 183)
                final T
                    sinLatS =
                    scLatP.sin().multiply(cosDs).add(scLatP.cos().multiply(sinDs).multiply(ray.getCosineAz()));
                final T cosLatS = FastMath.sqrt(sinLatS.multiply(sinLatS).negate().add(1.0));
                latitudes[i] = FastMath.atan2(sinLatS, cosLatS);

                // Longitude (Eq. 184 to 187)
                final T sinLonS = sinDs.multiply(ray.getSineAz()).multiply(scLatP.cos());
                final T cosLonS = cosDs.subtract(scLatP.sin().multiply(sinLatS));
                longitudes[i] = FastMath.atan2(sinLonS, cosLonS).add(ray.getLongitude());
            }
        }
    }

    /**
     * Computes the distance of a point from the ray-perigee.
     *
     * @param field field of the elements
     * @param n     number of points used for the integration
     * @param s1    lower boundary
     * @return the distance of a point from the ray-perigee in km
     */
    private T[] getSegments(final Field<T> field, final int n, final T s1) {
        // Eq. 196
        final T g = deltaN.multiply(0.5773502691896);
        // Eq. 197
        final T y = s1.add(deltaN.subtract(g).multiply(0.5));
        final T[] segments = MathArrays.buildArray(field, 2 * n);
        int index = 0;
        for (int i = 0; i < n; i++) {
            // Eq. 198
            segments[index] = y.add(deltaN.multiply(i));
            index++;
            segments[index] = y.add(deltaN.multiply(i)).add(g);
            index++;
        }
        return segments;
    }

    /**
     * Get the latitudes of the coordinates along the integration path.
     *
     * @return the latitudes in radians
     */
    public T[] getLatitudes() {
        return latitudes;
    }

    /**
     * Get the longitudes of the coordinates along the integration path.
     *
     * @return the longitudes in radians
     */
    public T[] getLongitudes() {
        return longitudes;
    }

    /**
     * Get the heights of the coordinates along the integration path.
     *
     * @return the heights in m
     */
    public T[] getHeights() {
        return heights;
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
