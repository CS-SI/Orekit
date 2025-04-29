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
import org.hipparchus.util.SinCos;
import org.orekit.bodies.GeodeticPoint;

/** Container for ray-perigee parameters.
 * <p>By convention, point 1 is at lower height.</p>
 * @author Bryan Cazabonne
 * @since 13.0
 */
public class Ray {

    /** Threshold for ray-perigee parameters computation. */
    private static final double THRESHOLD = 1.0e-10;

    /** Receiver altitude [m].
     * @since 13.0
     */
    private final double recH;

    /** Satellite altitude [m].
     * @since 13.0
     */
    private final double satH;

    /** Distance of the first point from the ray perigee [m]. */
    private final double s1;

    /** Distance of the second point from the ray perigee [m]. */
    private final double s2;

    /** Ray-perigee radius [m]. */
    private final double rp;

    /** Ray-perigee latitude [rad]. */
    private final double latP;

    /** Ray-perigee longitude [rad]. */
    private final double lonP;

    /** Sine and cosine of ray-perigee latitude. */
    private final SinCos scLatP;

    /** Sine of azimuth of satellite as seen from ray-perigee. */
    private final double sinAzP;

    /** Cosine of azimuth of satellite as seen from ray-perigee. */
    private final double cosAzP;

    /**
     * Constructor.
     *
     * @param recP receiver position
     * @param satP satellite position
     */
    public Ray(final GeodeticPoint recP, final GeodeticPoint satP) {

        // Integration limits in meters (Eq. 140 and 141)
        this.recH       = recP.getAltitude();
        this.satH       = satP.getAltitude();
        final double r1 = NeQuickModel.RE + recH;
        final double r2 = NeQuickModel.RE + satH;

        // Useful parameters
        final double lat1     = recP.getLatitude();
        final double lat2     = satP.getLatitude();
        final double lon1     = recP.getLongitude();
        final double lon2     = satP.getLongitude();
        final SinCos scLatSat = FastMath.sinCos(lat2);
        final SinCos scLatRec = FastMath.sinCos(lat1);
        final SinCos scLon21  = FastMath.sinCos(lon2 - lon1);

        // Zenith angle computation, using:
        //   - Eq. 153 with added protection against numerical noise near zenith observation
        //   - replacing Eq. 154 by a different one more stable near zenith
        //   - replacing Eq. 155 by different ones avoiding trigonometric functions
        final double cosD     = FastMath.min(1.0,
                                             scLatRec.sin() * scLatSat.sin() +
                                             scLatRec.cos() * scLatSat.cos() * scLon21.cos());
        final double sinDSinM = scLatSat.cos() * scLon21.sin();
        final double sinDCosM = scLatSat.sin() * scLatRec.cos() - scLatSat.cos() * scLatRec.sin() * scLon21.cos();
        final double sinD     = FastMath.sqrt(sinDSinM * sinDSinM + sinDCosM * sinDCosM);
        final double u        = r2 * sinD;
        final double v        = r2 * cosD - r1;
        final double inv      = 1.0 / FastMath.sqrt(u * u + v * v);
        final double sinZ     = u * inv;
        final double cosZ     = v * inv;


        // Ray-perigee computation in meters (Eq. 156)
        this.rp = r1 * sinZ;

        // Ray-perigee latitude and longitude
        if (FastMath.abs(FastMath.abs(lat1) - 0.5 * FastMath.PI) < THRESHOLD) {
            // receiver is almost at North or South pole

            // Ray-perigee latitude (Eq. 157)
            this.latP = FastMath.copySign(FastMath.atan2(u, v), lat1);

            // Ray-perigee longitude (Eq. 164)
            this.lonP = lon2 + FastMath.PI;

        } else if (FastMath.abs(sinZ) < THRESHOLD) {
            // satellite is almost on receiver zenith

            this.latP = recP.getLatitude();
            this.lonP = recP.getLongitude();

        } else {

            // Ray-perigee latitude (Eq. 158 to 163)
            final double sinAz   = scLon21.sin() * scLatSat.cos() / sinD;
            final double cosAz   = (scLatSat.sin() - cosD * scLatRec.sin()) / (sinD * scLatRec.cos());
            final double sinLatP = scLatRec.sin() * sinZ - scLatRec.cos() * cosZ * cosAz;
            final double cosLatP = FastMath.sqrt(1.0 - sinLatP * sinLatP);
            this.latP = FastMath.atan2(sinLatP, cosLatP);

            // Ray-perigee longitude (Eq. 165 to 167, plus protection against ray-perigee along polar axis)
            if (cosLatP < THRESHOLD) {
                this.lonP = 0.0;
            } else {
                final double sinLonP = -sinAz * cosZ / cosLatP;
                final double cosLonP = (sinZ - scLatRec.sin() * sinLatP) / (scLatRec.cos() * cosLatP);
                this.lonP = FastMath.atan2(sinLonP, cosLonP) + lon1;
            }

        }

        // Sine and cosine of ray-perigee latitude
        this.scLatP = FastMath.sinCos(latP);

        if (FastMath.abs(FastMath.abs(latP) - 0.5 * FastMath.PI) < THRESHOLD || FastMath.abs(sinZ) < THRESHOLD) {
            // Eq. 172 and 173
            this.sinAzP = 0.0;
            this.cosAzP = -FastMath.copySign(1, latP);
        } else {
            final SinCos scLon = FastMath.sinCos(lon2 - lonP);
            // Sine and cosine of azimuth of satellite as seen from ray-perigee
            final SinCos scPsi = FastMath.sinCos(greatCircleAngle(scLatSat, scLon));
            // Eq. 174 and 175
            this.sinAzP = scLatSat.cos() * scLon.sin() / scPsi.sin();
            this.cosAzP = (scLatSat.sin() - scLatP.sin() * scPsi.cos()) / (scLatP.cos() * scPsi.sin());
        }

        // Integration end points s1 and s2 in meters (Eq. 176 and 177)
        this.s1 = FastMath.sqrt(r1 * r1 - rp * rp);
        this.s2 = FastMath.sqrt(r2 * r2 - rp * rp);
    }

    /**
     * Get receiver altitude.
     * @return receiver altitude
     * @since 13.0
     */
    public double getRecH() {
        return recH;
    }

    /**
     * Get satellite altitude.
     * @return satellite altitude
     * @since 13.0
     */
    public double getSatH() {
        return satH;
    }

    /**
     * Get the distance of the first point from the ray perigee.
     *
     * @return s1 in meters
     */
    public double getS1() {
        return s1;
    }

    /**
     * Get the distance of the second point from the ray perigee.
     *
     * @return s2 in meters
     */
    public double getS2() {
        return s2;
    }

    /**
     * Get the ray-perigee radius.
     *
     * @return the ray-perigee radius in meters
     */
    public double getRadius() {
        return rp;
    }

    /**
     * Get the ray-perigee latitude.
     *
     * @return the ray-perigee latitude in radians
     */
    public double getLatitude() {
        return latP;
    }

    /**
     * Get the ray-perigee latitude sin/cos.
     *
     * @return the ray-perigee latitude sin/cos
     * @since 13.0
     */
    public SinCos getScLat() {
        return scLatP;
    }

    /**
     * Get the ray-perigee longitude.
     *
     * @return the ray-perigee longitude in radians
     */
    public double getLongitude() {
        return lonP;
    }

    /**
     * Get the sine of azimuth of satellite as seen from ray-perigee.
     *
     * @return the sine of azimuth
     */
    public double getSineAz() {
        return sinAzP;
    }

    /**
     * Get the cosine of azimuth of satellite as seen from ray-perigee.
     *
     * @return the cosine of azimuth
     */
    public double getCosineAz() {
        return cosAzP;
    }

    /**
     * Compute the great circle angle from ray-perigee to satellite.
     * <p>
     * This method used the equations 168 to 171 of the reference document.
     * </p>
     *
     * @param scLat sine and cosine of satellite latitude
     * @param scLon sine and cosine of satellite longitude minus receiver longitude
     * @return the great circle angle in radians
     */
    private double greatCircleAngle(final SinCos scLat, final SinCos scLon) {
        if (FastMath.abs(FastMath.abs(latP) - 0.5 * FastMath.PI) < THRESHOLD) {
            return FastMath.abs(FastMath.asin(scLat.sin()) - latP);
        } else {
            final double cosPhi = scLatP.sin() * scLat.sin() + scLatP.cos() * scLat.cos() * scLon.cos();
            final double sinPhi = FastMath.sqrt(1.0 - cosPhi * cosPhi);
            return FastMath.atan2(sinPhi, cosPhi);
        }
    }

}
