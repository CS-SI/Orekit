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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.orekit.bodies.FieldGeodeticPoint;

/** Container for ray-perigee parameters.
 * <p>By convention, point 1 is at lower height.</p>
 * @author Bryan Cazabonne
 * @since 13.0
 */
class FieldRay<T extends CalculusFieldElement<T>> {

    /** Threshold for ray-perigee parameters computation. */
    private static final double THRESHOLD = 1.0e-10;

    /** Receiver altitude [m].
     * @since 13.0
     */
    private final T recH;

    /** Satellite altitude [m].
     * @since 13.0
     */
    private final T satH;

    /** Distance of the first point from the ray perigee [m]. */
    private final T s1;

    /** Distance of the second point from the ray perigee [m]. */
    private final T s2;

    /** Ray-perigee radius [m]. */
    private final T rp;

    /** Ray-perigee latitude [rad]. */
    private final T latP;

    /** Ray-perigee longitude [rad]. */
    private final T lonP;

    /** Sine and cosine of ray-perigee latitude. */
    private final FieldSinCos<T> scLatP;

    /** Sine of azimuth of satellite as seen from ray-perigee. */
    private final T sinAzP;

    /** Cosine of azimuth of satellite as seen from ray-perigee. */
    private final T cosAzP;

    /**
     * Constructor.
     *
     * @param recP  receiver position
     * @param satP  satellite position
     */
    FieldRay(final FieldGeodeticPoint<T> recP, final FieldGeodeticPoint<T> satP) {

        // Integration limits in meters (Eq. 140 and 141)
        this.recH  = recP.getAltitude();
        this.satH  = satP.getAltitude();
        final T r1 = recH.add(NeQuickModel.RE);
        final T r2 = satH.add(NeQuickModel.RE);

        // Useful parameters
        final T pi = r1.getPi();
        final T lat1 = recP.getLatitude();
        final T lat2 = satP.getLatitude();
        final T lon1 = recP.getLongitude();
        final T lon2 = satP.getLongitude();
        final FieldSinCos<T> scLatSat = FastMath.sinCos(lat2);
        final FieldSinCos<T> scLatRec = FastMath.sinCos(lat1);
        final FieldSinCos<T> scLon21 = FastMath.sinCos(lon2.subtract(lon1));

        // Zenith angle computation (Eq. 153 to 155)
        final T cosD = scLatRec.sin().multiply(scLatSat.sin()).
                       add(scLatRec.cos().multiply(scLatSat.cos()).multiply(scLon21.cos()));
        final T sinD = FastMath.sqrt(cosD.multiply(cosD).negate().add(1.0));
        final T z    = FastMath.atan2(sinD, cosD.subtract(r1.divide(r2)));
        final FieldSinCos<T> scZ = FastMath.sinCos(z);

        // Ray-perigee computation in meters (Eq. 156)
        this.rp = r1.multiply(scZ.sin());

        // Ray-perigee latitude and longitude
        if (FastMath.abs(FastMath.abs(lat1).subtract(pi.multiply(0.5)).getReal()) < THRESHOLD) {

            // Ray-perigee latitude (Eq. 157)
            this.latP = FastMath.copySign(z, lat1);

            // Ray-perigee longitude (Eq. 164)
            if (z.getReal() < 0) {
                this.lonP = lon2;
            } else {
                this.lonP = lon2.add(pi);
            }

        } else if (FastMath.abs(scZ.sin().getReal()) < THRESHOLD) {
            // satellite is almost on receiver zenith

            this.latP = recP.getLatitude();
            this.lonP = recP.getLongitude();

        } else {

            // Ray-perigee latitude (Eq. 158 to 163)
            final T sinAz   = FastMath.sin(lon2.subtract(lon1)).multiply(scLatSat.cos()).divide(sinD);
            final T cosAz   = scLatSat.sin().subtract(cosD.multiply(scLatRec.sin())).
                              divide(sinD.multiply(scLatRec.cos()));
            final T sinLatP = scLatRec.sin().multiply(scZ.sin()).
                              subtract(scLatRec.cos().multiply(scZ.cos()).multiply(cosAz));
            final T cosLatP = FastMath.sqrt(sinLatP.multiply(sinLatP).negate().add(1.0));
            this.latP       = FastMath.atan2(sinLatP, cosLatP);

            // Ray-perigee longitude (Eq. 165 to 167)
            final T sinLonP = sinAz.negate().multiply(scZ.cos()).divide(cosLatP);
            final T cosLonP = scZ.sin().subtract(scLatRec.sin().multiply(sinLatP)).
                              divide(scLatRec.cos().multiply(cosLatP));
            this.lonP       = FastMath.atan2(sinLonP, cosLonP).add(lon1);

        }

        // Sine and cosine of ray-perigee latitude
        this.scLatP = FastMath.sinCos(latP);

        if (FastMath.abs(FastMath.abs(latP).subtract(pi.multiply(0.5)).getReal()) < THRESHOLD || FastMath.abs(
            scZ.sin().getReal()) < THRESHOLD) {
            // Eq. 172 and 173
            this.sinAzP = pi.getField().getZero();
            this.cosAzP = FastMath.copySign(pi.getField().getOne(), latP).negate();
        } else {
            final FieldSinCos<T> scLon = FastMath.sinCos(lon2.subtract(lonP));
            // Sine and cosine of azimuth of satellite as seen from ray-perigee
            final FieldSinCos<T> scPsi = FastMath.sinCos(greatCircleAngle(scLatSat, scLon));
            // Eq. 174 and 175
            this.sinAzP = scLatSat.cos().multiply(scLon.sin()).divide(scPsi.sin());
            this.cosAzP = scLatSat.sin().subtract(scLatP.sin().multiply(scPsi.cos())).
                          divide(scLatP.cos().multiply(scPsi.sin()));
        }

        // Integration end points s1 and s2 in meters (Eq. 176 and 177)
        this.s1 = FastMath.sqrt(r1.multiply(r1).subtract(rp.multiply(rp)));
        this.s2 = FastMath.sqrt(r2.multiply(r2).subtract(rp.multiply(rp)));
    }

    /**
     * Get receiver altitude.
     * @return receiver altitude
     * @since 13.0
     */
    public T getRecH() {
        return recH;
    }

    /**
     * Get satellite altitude.
     * @return satellite altitude
     * @since 13.0
     */
    public T getSatH() {
        return satH;
    }

    /**
     * Get the distance of the first point from the ray perigee.
     *
     * @return s1 in meters
     */
    public T getS1() {
        return s1;
    }

    /**
     * Get the distance of the second point from the ray perigee.
     *
     * @return s2 in meters
     */
    public T getS2() {
        return s2;
    }

    /**
     * Get the ray-perigee radius.
     *
     * @return the ray-perigee radius in meters
     */
    public T getRadius() {
        return rp;
    }

    /**
     * Get the ray-perigee latitude.
     *
     * @return the ray-perigee latitude in radians
     */
    public T getLatitude() {
        return latP;
    }

    /**
     * Get the ray-perigee latitude sin/cos.
     *
     * @return the ray-perigee latitude sin/cos
     * @since 13.0
     */
    public FieldSinCos<T> getScLat() {
        return scLatP;
    }

    /**
     * Get the ray-perigee longitude.
     *
     * @return the ray-perigee longitude in radians
     */
    public T getLongitude() {
        return lonP;
    }

    /**
     * Get the sine of azimuth of satellite as seen from ray-perigee.
     *
     * @return the sine of azimuth
     */
    public T getSineAz() {
        return sinAzP;
    }

    /**
     * Get the cosine of azimuth of satellite as seen from ray-perigee.
     *
     * @return the cosine of azimuth
     */
    public T getCosineAz() {
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
    private T greatCircleAngle(final FieldSinCos<T> scLat, final FieldSinCos<T> scLon) {
        if (FastMath.abs(FastMath.abs(latP).getReal() - 0.5 * FastMath.PI) < THRESHOLD) {
            return FastMath.abs(FastMath.asin(scLat.sin()).subtract(latP));
        } else {
            final T cosPhi = scLatP.sin().multiply(scLat.sin()).
                             add(scLatP.cos().multiply(scLat.cos()).multiply(scLon.cos()));
            final T sinPhi = FastMath.sqrt(cosPhi.multiply(cosPhi).negate().add(1.0));
            return FastMath.atan2(sinPhi, cosPhi);
        }
    }

}
