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

import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;

/** Performs the computation of the coordinates along the integration path.
 * @author Bryan Cazabonne
 * @since 13.0
 */
class Segment {

    /** Threshold for zenith segment. */
    private static final double THRESHOLD = 1.0;

    /** Latitudes [rad]. */
    private final double[] latitudes;

    /** Longitudes [rad]. */
    private final double[] longitudes;

    /** Heights [m]. */
    private final double[] heights;

    /** Integration step [m]. */
    private final double deltaN;

    /**
     * Constructor.
     *
     * @param n   number of points used for the integration
     * @param ray ray-perigee parameters
     */
    Segment(final int n, final Ray ray) {
        // Integration en points
        final double s1 = ray.getS1();
        final double s2 = ray.getS2();

        // Integration step (Eq. 195)
        this.deltaN = (s2 - s1) / n;

        // Segments
        final double[] s = getSegments(n, s1);

        // Useful parameters
        final double rp = ray.getRadius();
        final SinCos scLatP = FastMath.sinCos(ray.getLatitude());

        // Geodetic coordinates
        final int size = s.length;
        heights = new double[size];
        latitudes = new double[size];
        longitudes = new double[size];
        for (int i = 0; i < size; i++) {
            // Heights (Eq. 178)
            heights[i] = FastMath.sqrt(s[i] * s[i] + rp * rp) - NeQuickModel.RE;

            if (rp < THRESHOLD) {
                // zenith segment
                latitudes[i] = ray.getLatitude();
                longitudes[i] = ray.getLongitude();
            } else {
                // Great circle parameters (Eq. 179 to 181)
                final double tanDs = s[i] / rp;
                final double cosDs = 1.0 / FastMath.sqrt(1.0 + tanDs * tanDs);
                final double sinDs = tanDs * cosDs;

                // Latitude (Eq. 182 to 183)
                final double sinLatS = scLatP.sin() * cosDs + scLatP.cos() * sinDs * ray.getCosineAz();
                final double cosLatS = FastMath.sqrt(1.0 - sinLatS * sinLatS);
                latitudes[i] = FastMath.atan2(sinLatS, cosLatS);

                // Longitude (Eq. 184 to 187)
                final double sinLonS = sinDs * ray.getSineAz() * scLatP.cos();
                final double cosLonS = cosDs - scLatP.sin() * sinLatS;
                longitudes[i] = FastMath.atan2(sinLonS, cosLonS) + ray.getLongitude();
            }
        }
    }

    /**
     * Computes the distance of a point from the ray-perigee.
     *
     * @param n  number of points used for the integration
     * @param s1 lower boundary
     * @return the distance of a point from the ray-perigee in km
     */
    private double[] getSegments(final int n, final double s1) {
        // Eq. 196
        final double g = 0.5773502691896 * deltaN;
        // Eq. 197
        final double y = s1 + (deltaN - g) * 0.5;
        final double[] segments = new double[2 * n];
        int index = 0;
        for (int i = 0; i < n; i++) {
            // Eq. 198
            segments[index] = y + i * deltaN;
            index++;
            segments[index] = y + i * deltaN + g;
            index++;
        }
        return segments;
    }

    /**
     * Get the latitudes of the coordinates along the integration path.
     *
     * @return the latitudes in radians
     */
    public double[] getLatitudes() {
        return latitudes;
    }

    /**
     * Get the longitudes of the coordinates along the integration path.
     *
     * @return the longitudes in radians
     */
    public double[] getLongitudes() {
        return longitudes;
    }

    /**
     * Get the heights of the coordinates along the integration path.
     *
     * @return the heights in m
     */
    public double[] getHeights() {
        return heights;
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
