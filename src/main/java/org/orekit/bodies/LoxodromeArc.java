/* Copyright 2022 Joseph Reed
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Joseph Reed licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.bodies;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;

/** Loxodrome defined by a start and ending point.
 *
 * @author Joe Reed
 * @since 11.3
 */
public class LoxodromeArc extends Loxodrome {

    /** Threshold for considering latitudes equal, in radians. */
    private static final double LATITUDE_THRESHOLD = 1e-6;

    /** Maximum number of iterations used when computing distance between points. */
    private static final int MAX_ITER = 50;

    /** Ending point of the arc. */
    private final GeodeticPoint endPoint;

    /** Delta longitude, cached, radians. */
    private final double deltaLon;

    /** Cached arc distance, meters. */
    private double distance = -1;

    /** Class constructor where the arc's altitude is the average of the initial and final points.
     * @param point the starting point
     * @param endPoint the ending point
     * @param body the body on which the loxodrome is defined
     */
    public LoxodromeArc(final GeodeticPoint point, final GeodeticPoint endPoint, final OneAxisEllipsoid body) {
        this(point, endPoint, body, (point.getAltitude() + endPoint.getAltitude()) / 2.);
    }

    /** Class constructor.
     * @param point the starting point
     * @param endPoint the ending point
     * @param body the body on which the loxodrome is defined
     * @param altitude the altitude above the reference body (meters)
     */
    public LoxodromeArc(final GeodeticPoint point, final GeodeticPoint endPoint, final OneAxisEllipsoid body,
                        final double altitude) {
        super(point, body.azimuthBetweenPoints(point, endPoint), body, altitude);
        this.endPoint = endPoint;
        this.deltaLon = MathUtils.normalizeAngle(endPoint.getLongitude(), point.getLongitude()) -
                        point.getLongitude();
    }

    /** Get the final point of the arc.
     *
     * @return the ending point of the arc
     */
    public GeodeticPoint getFinalPoint() {
        return this.endPoint;
    }

    /** Compute the distance of the arc along the surface of the ellipsoid.
     *
     * @return the distance (meters)
     */
    public double getDistance() {
        if (distance >= 0) {
            return distance;
        }

        // compute the e sin(lat)^2
        final double ptLat  = getPoint().getLatitude();
        final double sinLat = FastMath.sin(ptLat);
        final double eccSinLatSq = getBody().getEccentricitySquared() * sinLat * sinLat;

        // compute intermediate values
        final double t1 = 1. - getBody().getEccentricitySquared();
        final double t2 = 1. - eccSinLatSq;
        final double t3 = FastMath.sqrt(t2);

        final double semiMajorAxis = getBody().getEquatorialRadius() + getAltitude();

        final double meridianCurve = (semiMajorAxis * t1) / (t2 * t3);

        if (FastMath.abs(endPoint.getLatitude() - ptLat) < LATITUDE_THRESHOLD) {
            distance = (semiMajorAxis / t3) * FastMath.abs(FastMath.cos(ptLat) * deltaLon);
        }
        else {
            final double eccSq34 = 0.75 * getBody().getEccentricitySquared();
            final double halfEccSq34 = eccSq34 / 2.;
            final double t6 = 1. / (1. - eccSq34);
            final double t7 = t1 * semiMajorAxis / meridianCurve;

            final double t8 = ptLat + t6 *
                (t7 * (endPoint.getLatitude() - ptLat) + halfEccSq34 * FastMath.sin(ptLat * 2.));
            final double t9 = halfEccSq34 * t6;

            double guess = 0;
            double lat = endPoint.getLatitude();
            for (int i = 0; i < MAX_ITER; i++) {
                guess = lat;
                lat = t8 - t9 * FastMath.sin(2. * guess);

                if (FastMath.abs(lat - guess) < LATITUDE_THRESHOLD) {
                    break;
                }
            }

            final double azimuth = FastMath.atan2(deltaLon,
                getBody().geodeticToIsometricLatitude(lat) - getBody().geodeticToIsometricLatitude(ptLat));
            distance = meridianCurve * FastMath.abs((lat - ptLat) / FastMath.cos(azimuth));
        }

        return distance;
    }

    /** Calculate a point at a specific percentage along the arc.
     *
     * @param fraction the fraction along the arc to compute the point
     * @return the point along the arc
     */
    public GeodeticPoint calculatePointAlongArc(final double fraction) {
        if (fraction == 0.) {
            return getPoint();
        }
        else if (fraction == 1.) {
            return getFinalPoint();
        }
        else {
            final double d = getDistance() * fraction;
            return this.pointAtDistance(d);
        }
    }
}
