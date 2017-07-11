/* Copyright 2011-2012 Space Applications Services
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

/** Used to calculate the geomagnetic field at a given geodetic point on earth.
 * The calculation is estimated using spherical harmonic expansion of the
 * geomagnetic potential with coefficients provided by an actual geomagnetic
 * field model (e.g. IGRF, WMM).
 * <p>
 * Based on original software written by Manoj Nair from the National
 * Geophysical Data Center, NOAA, as part of the WMM 2010 software release
 * (WMM_SubLibrary.c)
 * </p>
 * @see <a href="http://www.ngdc.noaa.gov/geomag/WMM/DoDWMM.shtml">World Magnetic Model Overview</a>
 * @see <a href="http://www.ngdc.noaa.gov/geomag/WMM/soft.shtml">WMM Software Downloads</a>
 * @author Thomas Neidhart
 */
public class GeoMagneticField {

    /** Semi major-axis of WGS-84 ellipsoid in km. */
    private static double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS / 1000d;

    /** The first eccentricity squared. */
    private static double epssq = 0.0066943799901413169961;

    /** Mean radius of IAU-66 ellipsoid, in km. */
    private static double ellipsoidRadius = 6371.2;

    /** The model name. */
    private String modelName;

    /** Base time of magnetic field model epoch (yrs). */
    private double epoch;

    /** C - Gauss coefficients of main geomagnetic model (nT). */
    private double[] g;

    /** C - Gauss coefficients of main geomagnetic model (nT). */
    private double[] h;

    /** CD - Gauss coefficients of secular geomagnetic model (nT/yr). */
    private double[] dg;

    /** CD - Gauss coefficients of secular geomagnetic model (nT/yr). */
    private double[] dh;

    /** maximum degree of spherical harmonic model. */
    private int maxN;

    /** maximum degree of spherical harmonic secular variations. */
    private int maxNSec;

    /** the validity start of this magnetic field model. */
    private double validityStart;
    /** the validity end of this magnetic field model. */
    private double validityEnd;

    /** Pre-calculated ratio between gauss-normalized and schmidt quasi-normalized
     * associated Legendre functions.
     */
    private double[] schmidtQuasiNorm;

    /** Create a new geomagnetic field model with the given parameters. Internal
     * structures are initialized according to the specified degrees of the main
     * and secular variations.
     * @param modelName the model name
     * @param epoch the epoch of the model
     * @param maxN the maximum degree of the main model
     * @param maxNSec the maximum degree of the secular variations
     * @param validityStart validity start of this model
     * @param validityEnd validity end of this model
     */
    protected GeoMagneticField(final String modelName, final double epoch,
                               final int maxN, final int maxNSec,
                               final double validityStart, final double validityEnd) {

        this.modelName = modelName;
        this.epoch = epoch;
        this.maxN = maxN;
        this.maxNSec = maxNSec;

        this.validityStart = validityStart;
        this.validityEnd = validityEnd;

        // initialize main and secular field coefficient arrays
        final int maxMainFieldTerms = (maxN + 1) * (maxN + 2) / 2;
        g = new double[maxMainFieldTerms];
        h = new double[maxMainFieldTerms];

        final int maxSecularFieldTerms = (maxNSec + 1) * (maxNSec + 2) / 2;
        dg = new double[maxSecularFieldTerms];
        dh = new double[maxSecularFieldTerms];

        // pre-calculate the ratio between gauss-normalized and schmidt quasi-normalized
        // associated Legendre functions as they depend only on the degree of the model.

        schmidtQuasiNorm = new double[maxMainFieldTerms + 1];
        schmidtQuasiNorm[0] = 1.0;

        int index;
        int index1;
        for (int n = 1; n <= maxN; n++) {
            index = n * (n + 1) / 2;
            index1 = (n - 1) * n / 2;

            // for m = 0
            schmidtQuasiNorm[index] =
                schmidtQuasiNorm[index1] * (double) (2 * n - 1) / (double) n;

            for (int m = 1; m <= n; m++) {
                index = n * (n + 1) / 2 + m;
                index1 = n * (n + 1) / 2 + m - 1;
                schmidtQuasiNorm[index] =
                    schmidtQuasiNorm[index1] *
                    FastMath.sqrt((double) ((n - m + 1) * (m == 1 ? 2 : 1)) / (double) (n + m));
            }
        }
    }

    /** Returns the epoch for this magnetic field model.
     * @return the epoch
     */
    public double getEpoch() {
        return epoch;
    }

    /** Returns the model name.
     * @return the model name
     */
    public String getModelName() {
        return modelName;
    }

    /** Returns the start of the validity period for this model.
     * @return the validity start as decimal year
     */
    public double validFrom() {
        return validityStart;
    }

    /** Returns the end of the validity period for this model.
     * @return the validity end as decimal year
     */
    public double validTo() {
        return validityEnd;
    }

    /** Indicates whether this model supports time transformation or not.
     * @return <code>true</code> if this model can be transformed within its
     *         validity period, <code>false</code> otherwise
     */
    public boolean supportsTimeTransform() {
        return maxNSec > 0;
    }

    /** Set the given main field coefficients.
     * @param n the n index
     * @param m the m index
     * @param gnm the g coefficient at position n,m
     * @param hnm the h coefficient at position n,m
     */
    protected void setMainFieldCoefficients(final int n, final int m,
                                         final double gnm, final double hnm) {
        final int index = n * (n + 1) / 2 + m;
        g[index] = gnm;
        h[index] = hnm;
    }

    /** Set the given secular variation coefficients.
     * @param n the n index
     * @param m the m index
     * @param dgnm the dg coefficient at position n,m
     * @param dhnm the dh coefficient at position n,m
     */
    protected void setSecularVariationCoefficients(final int n, final int m,
                                                final double dgnm, final double dhnm) {
        final int index = n * (n + 1) / 2 + m;
        dg[index] = dgnm;
        dh[index] = dhnm;
    }

    /** Calculate the magnetic field at the specified geodetic point identified
     * by latitude, longitude and altitude.
     * @param latitude the WGS84 latitude in decimal degrees
     * @param longitude the WGS84 longitude in decimal degrees
     * @param height the height above the WGS84 ellipsoid in kilometers
     * @return the {@link GeoMagneticElements} at the given geodetic point
     */
    public GeoMagneticElements calculateField(final double latitude,
                                              final double longitude,
                                              final double height) {

        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(latitude),
                                                   FastMath.toRadians(longitude),
                                                   height * 1000d);

        final SphericalCoordinates sph = transformToSpherical(gp);
        final SphericalHarmonicVars vars = new SphericalHarmonicVars(sph);
        final LegendreFunction legendre = new LegendreFunction(FastMath.sin(sph.phi));

        // sum up the magnetic field vector components
        final Vector3D magFieldSph = summation(sph, vars, legendre);
        // rotate the field to geodetic coordinates
        final Vector3D magFieldGeo = rotateMagneticVector(sph, gp, magFieldSph);
        // return the magnetic elements
        return new GeoMagneticElements(magFieldGeo);
    }

    /** Time transform the model coefficients from the base year of the model
     * using secular variation coefficients.
     * @param year the year to which the model shall be transformed
     * @return a time-transformed magnetic field model
     * @throws OrekitException if the specified year is outside the validity period of the
     *                         model or the model does not support time transformations
     *                         (i.e. no secular variations available)
     */
    public GeoMagneticField transformModel(final double year) throws OrekitException {

        if (!supportsTimeTransform()) {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_TIME_TRANSFORM, modelName, String.valueOf(epoch));
        }

        // the model can only be transformed within its validity period
        if (year < validityStart || year > validityEnd) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_TIME_TRANSFORM,
                                      modelName, String.valueOf(epoch), year, validityStart, validityEnd);
        }

        final double dt = year - epoch;
        final int maxSecIndex = maxNSec * (maxNSec + 1) / 2 + maxNSec;

        final GeoMagneticField transformed = new GeoMagneticField(modelName, year, maxN, maxNSec,
                                                                  validityStart, validityEnd);

        for (int n = 1; n <= maxN; n++) {
            for (int m = 0; m <= n; m++) {
                final int index = n * (n + 1) / 2 + m;
                if (index <= maxSecIndex) {
                    transformed.h[index] = h[index] + dt * dh[index];
                    transformed.g[index] = g[index] + dt * dg[index];
                    // we need a copy of the secular var coef to calculate secular change
                    transformed.dh[index] = dh[index];
                    transformed.dg[index] = dg[index];
                } else {
                    // just copy the parts that do not have corresponding secular variation coefficients
                    transformed.h[index] = h[index];
                    transformed.g[index] = g[index];
                }
            }
        }

        return transformed;
    }

    /** Time transform the model coefficients from the base year of the model
     * using a linear interpolation with a second model. The second model is
     * required to have an adjacent validity period.
     * @param otherModel the other magnetic field model
     * @param year the year to which the model shall be transformed
     * @return a time-transformed magnetic field model
     * @throws OrekitException if the specified year is outside the validity period of the
     *                         model or the model does not support time transformations
     *                         (i.e. no secular variations available)
     */
    public GeoMagneticField transformModel(final GeoMagneticField otherModel, final double year)
        throws OrekitException {

        // the model can only be transformed within its validity period
        if (year < validityStart || year > validityEnd) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_TIME_TRANSFORM,
                                      modelName, String.valueOf(epoch), year, validityStart, validityEnd);
        }

        final double factor = (year - epoch) / (otherModel.epoch - epoch);
        final int maxNCommon = FastMath.min(maxN, otherModel.maxN);
        final int maxNCommonIndex = maxNCommon * (maxNCommon + 1) / 2 + maxNCommon;

        final int newMaxN = FastMath.max(maxN, otherModel.maxN);

        final GeoMagneticField transformed = new GeoMagneticField(modelName, year, newMaxN, 0,
                                                                  validityStart, validityEnd);

        for (int n = 1; n <= newMaxN; n++) {
            for (int m = 0; m <= n; m++) {
                final int index = n * (n + 1) / 2 + m;
                if (index <= maxNCommonIndex) {
                    transformed.h[index] = h[index] + factor * (otherModel.h[index] - h[index]);
                    transformed.g[index] = g[index] + factor * (otherModel.g[index] - g[index]);
                } else {
                    if (maxN < otherModel.maxN) {
                        transformed.h[index] = factor * otherModel.h[index];
                        transformed.g[index] = factor * otherModel.g[index];
                    } else {
                        transformed.h[index] = h[index] + factor * -h[index];
                        transformed.g[index] = g[index] + factor * -g[index];
                    }
                }
            }
        }

        return transformed;
    }

    /** Utility function to get a decimal year for a given day.
     * @param day the day (1-31)
     * @param month the month (1-12)
     * @param year the year
     * @return the decimal year represented by the given day
     */
    public static double getDecimalYear(final int day, final int month, final int year) {
        final int[] days = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
        final int leapYear = (((year % 4) == 0) && (((year % 100) != 0) || ((year % 400) == 0))) ? 1 : 0;

        final double dayInYear = days[month - 1] + (day - 1) + (month > 2 ? leapYear : 0);
        return (double) year + (dayInYear / (365.0d + leapYear));
    }

    /** Transform geodetic coordinates to spherical coordinates.
     * @param gp the geodetic point
     * @return the spherical coordinates wrt to the reference ellipsoid of the model
     */
    private SphericalCoordinates transformToSpherical(final GeodeticPoint gp) {

        // Convert geodetic coordinates (defined by the WGS-84 reference ellipsoid)
        // to Earth Centered Earth Fixed Cartesian coordinates, and then to spherical coordinates.

        final double lat = gp.getLatitude();
        final double heightAboveEllipsoid = gp.getAltitude() / 1000d;
        final double sinLat = FastMath.sin(lat);

        // compute the local radius of curvature on the reference ellipsoid
        final double rc = a / FastMath.sqrt(1.0d - epssq * sinLat * sinLat);

        // compute ECEF Cartesian coordinates of specified point (for longitude=0)
        final double xp = (rc + heightAboveEllipsoid) * FastMath.cos(lat);
        final double zp = (rc * (1.0d - epssq) + heightAboveEllipsoid) * sinLat;

        // compute spherical radius and angle lambda and phi of specified point
        final double r = FastMath.hypot(xp, zp);
        return new SphericalCoordinates(r, gp.getLongitude(), FastMath.asin(zp / r));
    }

    /** Rotate the magnetic vectors to geodetic coordinates.
     * @param sph the spherical coordinates
     * @param gp the geodetic point
     * @param field the magnetic field in spherical coordinates
     * @return the magnetic field in geodetic coordinates
     */
    private Vector3D rotateMagneticVector(final SphericalCoordinates sph,
                                          final GeodeticPoint gp,
                                          final Vector3D field) {

        // difference between the spherical and geodetic latitudes
        final double psi = sph.phi - gp.getLatitude();

        // rotate spherical field components to the geodetic system
        final double Bz = field.getX() * FastMath.sin(psi) + field.getZ() * FastMath.cos(psi);
        final double Bx = field.getX() * FastMath.cos(psi) - field.getZ() * FastMath.sin(psi);
        final double By = field.getY();

        return new Vector3D(Bx, By, Bz);
    }

    /** Computes Geomagnetic Field Elements X, Y and Z in spherical coordinate
     * system using spherical harmonic summation.
     * The vector Magnetic field is given by -grad V, where V is geomagnetic
     * scalar potential. The gradient in spherical coordinates is given by:
     * <pre>
     *          dV ^   1 dV ^       1    dV ^
     * grad V = -- r + - -- t + -------- -- p
     *          dr     r dt     r sin(t) dp
     * </pre>
     * @param sph the spherical coordinates
     * @param vars the spherical harmonic variables
     * @param legendre the legendre function
     * @return the magnetic field vector in spherical coordinates
     */
    private Vector3D summation(final SphericalCoordinates sph, final SphericalHarmonicVars vars,
                               final LegendreFunction legendre) {

        int index;
        double Bx = 0.0;
        double By = 0.0;
        double Bz = 0.0;

        for (int n = 1; n <= maxN; n++) {
            for (int m = 0; m <= n; m++) {
                index = n * (n + 1) / 2 + m;

                /**
                 * <pre>
                 *       nMax               (n+2)   n    m            m           m
                 * Bz = -SUM (n + 1) * (a/r)     * SUM [g cos(m p) + h sin(m p)] P (sin(phi))
                 *       n=1                       m=0   n            n           n
                 * </pre>
                 * Equation 12 in the WMM Technical report. Derivative with respect to radius.
                 */
                Bz -= vars.relativeRadiusPower[n] *
                      (g[index] * vars.cmLambda[m] + h[index] * vars.smLambda[m]) * (1d + n) * legendre.mP[index];

                /**
                 * <pre>
                 *      nMax     (n+2)   n    m            m            m
                 * By = SUM (a/r)     * SUM [g cos(m p) + h sin(m p)] dP (sin(phi))
                 *      n=1             m=0   n            n            n
                 * </pre>
                 * Equation 11 in the WMM Technical report. Derivative with respect to longitude, divided by radius.
                 */
                By += vars.relativeRadiusPower[n] *
                      (g[index] * vars.smLambda[m] - h[index] * vars.cmLambda[m]) * (double) m * legendre.mP[index];
                /**
                 * <pre>
                 *        nMax     (n+2)   n    m            m            m
                 * Bx = - SUM (a/r)     * SUM [g cos(m p) + h sin(m p)] dP (sin(phi))
                 *        n=1             m=0   n            n            n
                 * </pre>
                 * Equation 10 in the WMM Technical report. Derivative with respect to latitude, divided by radius.
                 */
                Bx -= vars.relativeRadiusPower[n] *
                      (g[index] * vars.cmLambda[m] + h[index] * vars.smLambda[m]) * legendre.mPDeriv[index];
            }
        }

        final double cosPhi = FastMath.cos(sph.phi);
        if (FastMath.abs(cosPhi) > 1.0e-10) {
            By = By / cosPhi;
        } else {
            // special calculation for component - By - at geographic poles.
            // To avoid using this function, make sure that the latitude is not
            // exactly +/-90.
            By = summationSpecial(sph, vars);
        }

        return new Vector3D(Bx, By, Bz);
    }

    /** Special calculation for the component By at geographic poles.
     * @param sph the spherical coordinates
     * @param vars the spherical harmonic variables
     * @return the By component of the magnetic field
     */
    private double summationSpecial(final SphericalCoordinates sph, final SphericalHarmonicVars vars) {

        double k;
        final double sinPhi = FastMath.sin(sph.phi);
        final double[] mPcupS = new double[maxN + 1];
        mPcupS[0] = 1;
        double By = 0.0;

        for (int n = 1; n <= maxN; n++) {
            final int index = n * (n + 1) / 2 + 1;
            if (n == 1) {
                mPcupS[n] = mPcupS[n - 1];
            } else {
                k = (double) (((n - 1) * (n - 1)) - 1) / (double) ((2 * n - 1) * (2 * n - 3));
                mPcupS[n] = sinPhi * mPcupS[n - 1] - k * mPcupS[n - 2];
            }

            /**
             * <pre>
             *      nMax     (n+2)   n    m            m            m
             * By = SUM (a/r)     * SUM [g cos(m p) + h sin(m p)] dP (sin(phi))
             *      n=1             m=0   n            n            n
             * </pre>
             * Equation 11 in the WMM Technical report. Derivative with respect to longitude, divided by radius.
             */
            By += vars.relativeRadiusPower[n] *
                  (g[index] * vars.smLambda[1] - h[index] * vars.cmLambda[1]) * mPcupS[n] * schmidtQuasiNorm[index];
        }

        return By;
    }

    /** Utility class to hold spherical coordinates. */
    private static class SphericalCoordinates {

        /** the radius. */
        private double r;

        /** the azimuth angle. */
        private double lambda;

        /** the polar angle. */
        private double phi;

        /** Create a new spherical coordinate object.
         * @param r the radius
         * @param lambda the lambda angle
         * @param phi the phi angle
         */
        private SphericalCoordinates(final double r, final double lambda, final double phi) {
            this.r = r;
            this.lambda = lambda;
            this.phi = phi;
        }
    }

    /** Utility class to compute certain variables for magnetic field summation. */
    private class SphericalHarmonicVars {

        /** (Radius of Earth / Spherical radius r)^(n+2). */
        private double[] relativeRadiusPower;

        /** cos(m*lambda). */
        private double[] cmLambda;

        /** sin(m*lambda). */
        private double[] smLambda;

        /** Calculates the spherical harmonic variables for a given spherical coordinate.
         * @param sph the spherical coordinate
         */
        private SphericalHarmonicVars(final SphericalCoordinates sph) {

            relativeRadiusPower = new double[maxN + 1];

            // Compute a table of (EARTH_REFERENCE_RADIUS_KM / radius)^n for i in
            // 0 .. maxN (this is much faster than calling FastMath.pow maxN+1 times).

            final double p = ellipsoidRadius / sph.r;
            relativeRadiusPower[0] = p * p;
            for (int n = 1; n <= maxN; n++) {
                relativeRadiusPower[n] = relativeRadiusPower[n - 1] * (ellipsoidRadius / sph.r);
            }

            // Compute tables of sin(lon * m) and cos(lon * m) for m = 0 .. maxN
            // this is much faster than calling FastMath.sin and FastMath.cos maxN+1 times.

            cmLambda = new double[maxN + 1];
            smLambda = new double[maxN + 1];

            cmLambda[0] = 1.0d;
            smLambda[0] = 0.0d;

            final double cosLambda = FastMath.cos(sph.lambda);
            final double sinLambda = FastMath.sin(sph.lambda);
            cmLambda[1] = cosLambda;
            smLambda[1] = sinLambda;

            for (int m = 2; m <= maxN; m++) {
                cmLambda[m] = cmLambda[m - 1] * cosLambda - smLambda[m - 1] * sinLambda;
                smLambda[m] = cmLambda[m - 1] * sinLambda + smLambda[m - 1] * cosLambda;
            }
        }
    }

    /** Utility class to compute a table of Schmidt-semi normalized associated Legendre functions. */
    private class LegendreFunction {

        /** the vector of all associated Legendre polynomials. */
        private double[] mP;

        /** the vector of derivatives of the Legendre polynomials wrt latitude. */
        private double[] mPDeriv;

        /** Calculate the Schmidt-semi normalized Legendre function.
         * <p>
         * <b>Note:</b> In geomagnetism, the derivatives of ALF are usually
         * found with respect to the colatitudes. Here the derivatives are found
         * with respect to the latitude. The difference is a sign reversal for
         * the derivative of the Associated Legendre Functions.
         * </p>
         * @param x sinus of the spherical latitude (or cosinus of the spherical colatitude)
         */
        private LegendreFunction(final double x) {

            final int numTerms = (maxN + 1) * (maxN + 2) / 2;

            mP = new double[numTerms + 1];
            mPDeriv = new double[numTerms + 1];

            mP[0] = 1.0;
            mPDeriv[0] = 0.0;

            // sin (geocentric latitude) - sin_phi
            final double z = FastMath.sqrt((1.0d - x) * (1.0d + x));

            int index;
            int index1;
            int index2;

            // First, compute the Gauss-normalized associated Legendre functions
            for (int n = 1; n <= maxN; n++) {
                for (int m = 0; m <= n; m++) {
                    index = n * (n + 1) / 2 + m;
                    if (n == m) {
                        index1 = (n - 1) * n / 2 + m - 1;
                        mP[index] = z * mP[index1];
                        mPDeriv[index] = z * mPDeriv[index1] + x * mP[index1];
                    } else if (n == 1 && m == 0) {
                        index1 = (n - 1) * n / 2 + m;
                        mP[index] = x * mP[index1];
                        mPDeriv[index] = x * mPDeriv[index1] - z * mP[index1];
                    } else if (n > 1 && n != m) {
                        index1 = (n - 2) * (n - 1) / 2 + m;
                        index2 = (n - 1) * n / 2 + m;
                        if (m > n - 2) {
                            mP[index] = x * mP[index2];
                            mPDeriv[index] = x * mPDeriv[index2] - z * mP[index2];
                        } else {
                            final double k = (double) ((n - 1) * (n - 1) - (m * m)) /
                                             (double) ((2 * n - 1) * (2 * n - 3));

                            mP[index] = x * mP[index2] - k * mP[index1];
                            mPDeriv[index] = x * mPDeriv[index2] - z * mP[index2] - k * mPDeriv[index1];
                        }
                    }

                }
            }

            // Converts the Gauss-normalized associated Legendre functions to the Schmidt quasi-normalized
            // version using pre-computed relation stored in the variable schmidtQuasiNorm

            for (int n = 1; n <= maxN; n++) {
                for (int m = 0; m <= n; m++) {
                    index = n * (n + 1) / 2 + m;

                    mP[index] = mP[index] * schmidtQuasiNorm[index];
                    // The sign is changed since the new WMM routines use derivative with
                    // respect to latitude instead of co-latitude
                    mPDeriv[index] = -mPDeriv[index] * schmidtQuasiNorm[index];
                }
            }
        }
    }
}
