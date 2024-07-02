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

/** Perform calculations on a loxodrome (commonly, a rhumb line) on an ellipsoid.
 * <p>
 * A <a href="https://en.wikipedia.org/wiki/Rhumb_line">loxodrome or rhumb line</a>
 * is an arc on an ellipsoid's surface that intersects every meridian at the same angle.
 *
 * @author Joe Reed
 * @since 11.3
 */
public class Loxodrome {

    /** Threshold for cos angles being equal. */
    private static final double COS_ANGLE_THRESHOLD = 1e-6;

    /** Threshold for when distances are close enough to zero. */
    private static final double DISTANCE_THRESHOLD = 1e-9;

    /** Reference point for the loxodrome. */
    private final GeodeticPoint point;

    /** Azimuth-off-north angle of the loxodrome. */
    private final double azimuth;

    /** Reference body. */
    private final OneAxisEllipsoid body;

    /** Altitude above the body. */
    private final double altitude;

    /** Constructor building a loxodrome from an initial point and an azimuth-off-local-north heading.
     *
     * This method is an equivalent to {@code new Loxodrome(point, azimuth, body, point.getAltitude())}
     *
     * @param point the initial loxodrome point
     * @param azimuth the heading, clockwise angle from north (radians, {@code [0,2pi]})
     * @param body ellipsoid body on which the loxodrome is defined
     */
    public Loxodrome(final GeodeticPoint point, final double azimuth, final OneAxisEllipsoid body) {
        this(point, azimuth, body, point.getAltitude());
    }

    /** Constructor building a loxodrome from an initial point and an azimuth-off-local-north heading.
     *
     * @param point the initial loxodrome point
     * @param azimuth the heading, clockwise angle from north (radians, {@code [0,2pi]})
     * @param body ellipsoid body on which the loxodrome is defined
     * @param altitude altitude above the reference body
     */
    public Loxodrome(final GeodeticPoint point, final double azimuth, final OneAxisEllipsoid body,
                     final double altitude) {
        this.point    = point;
        this.azimuth  = azimuth;
        this.body     = body;
        this.altitude = altitude;
    }

    /** Get the geodetic point defining the loxodrome.
     * @return the geodetic point defining the loxodrome
     */
    public GeodeticPoint getPoint() {
        return this.point;
    }

    /** Get the azimuth.
     * @return the azimuth
     */
    public double getAzimuth() {
        return this.azimuth;
    }

    /** Get the body on which the loxodrome is defined.
     * @return the body on which the loxodrome is defined
     */
    public OneAxisEllipsoid getBody() {
        return this.body;
    }

    /** Get the altitude above the reference body.
     * @return the altitude above the reference body
     */
    public double getAltitude() {
        return this.altitude;
    }

    /** Calculate the point at the specified distance from the origin point along the loxodrome.
     *
     * A positive distance follows the line in the azumuth direction (i.e. northward for arcs with azimuth
     * angles {@code [3pi/2, 2pi]} or {@code [0, pi/2]}). Negative distances travel in the opposite direction along
     * the rhumb line.
     *
     * Distance is computed at the altitude of the origin point.
     *
     * @param distance the distance to travel (meters)
     * @return the point at the specified distance from the origin
     */
    public GeodeticPoint pointAtDistance(final double distance) {
        if (FastMath.abs(distance) < DISTANCE_THRESHOLD) {
            return this.point;
        }

        // compute the e sin(lat)^2
        final double sinLat = FastMath.sin(point.getLatitude());
        final double eccSinLatSq = body.getEccentricitySquared() * sinLat * sinLat;

        // compute intermediate values
        final double t1 = 1. - body.getEccentricitySquared();
        final double t2 = 1. - eccSinLatSq;
        final double t3 = FastMath.sqrt(t2);

        final double semiMajorAxis = getBody().getEquatorialRadius() + getAltitude();

        final double meridianCurve = (semiMajorAxis * t1) / (t2 * t3);

        final double cosAzimuth = FastMath.cos(azimuth);

        final double lat;
        final double lon;
        if (FastMath.abs(cosAzimuth) < COS_ANGLE_THRESHOLD) {
            lat = point.getLatitude();
            lon = point.getLongitude() + ((distance * FastMath.sin(azimuth) * t3) / semiMajorAxis * FastMath.cos(point.getLatitude()));
        }
        else {
            final double eccSq34 = 0.75 * body.getEccentricitySquared();
            final double halfEccSq34 = eccSq34 / 2.;
            final double t4 = meridianCurve / (t1 * semiMajorAxis);

            final double latPrime = point.getLatitude() + distance * cosAzimuth / meridianCurve;
            final double latOffset = t4 * (
                ((1. - eccSq34) * (latPrime - point.getLatitude())) +
                        (halfEccSq34 * (FastMath.sin(2. * latPrime) - FastMath.sin(2. * point.getLatitude()))));

            lat = fixLatitude(point.getLatitude() + latOffset);

            final double lonOffset = FastMath.tan(azimuth) * (body.geodeticToIsometricLatitude(lat) - body.geodeticToIsometricLatitude(point.getLatitude()));
            lon = point.getLongitude() + lonOffset;
        }

        return new GeodeticPoint(lat, lon, getAltitude());
    }

    /** Adjust the latitude if necessary, ensuring it's always between -pi/2 and +pi/2.
     *
     * @param lat the latitude value
     * @return the latitude, within {@code [-pi/2,+pi/2]}
     */
    static double fixLatitude(final double lat) {
        if (lat < -MathUtils.SEMI_PI) {
            return -MathUtils.SEMI_PI;
        }
        else if (lat > MathUtils.SEMI_PI) {
            return MathUtils.SEMI_PI;
        }
        else {
            return lat;
        }
    }
}
