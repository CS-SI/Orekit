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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.SinCos;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;

/**
 * This class perfoms the computation of the parameters used by the NeQuick model.
 *
 * @author Bryan Cazabonne
 *
 * @see "European Union (2016). European GNSS (Galileo) Open Service-Ionospheric Correction
 *       Algorithm for Galileo Single Frequency Users. 1.2."
 *
 * @since 10.1
 */
class FieldNeQuickParameters <T extends CalculusFieldElement<T>> {

    /** Solar zenith angle at day night transition, degrees. */
    private static final double X0 = 86.23292796211615;

    /** F2 layer maximum density. */
    private final T nmF2;

    /** F2 layer maximum density height [km]. */
    private final T hmF2;

    /** F1 layer maximum density height [km]. */
    private final T hmF1;

    /** E layer maximum density height [km]. */
    private final T hmE;

    /** F2 layer bottom thickness parameter [km]. */
    private final T b2Bot;

    /** F1 layer top thickness parameter [km]. */
    private final T b1Top;

    /** F1 layer bottom thickness parameter [km]. */
    private final T b1Bot;

    /** E layer top thickness parameter [km]. */
    private final T beTop;

    /** E layer bottom thickness parameter [km]. */
    private final T beBot;

    /** topside thickness parameter [km]. */
    private final T h0;

    /** Layer amplitudes. */
    private final T[] amplitudes;

    /**
     * Build a new instance.
     * @param field field of the elements
     * @param dateTime current date time components
     * @param f2 F2 coefficients used by the F2 layer
     * @param fm3 Fm3 coefficients used by the F2 layer
     * @param latitude latitude of a point along the integration path, in radians
     * @param longitude longitude of a point along the integration path, in radians
     * @param alpha effective ionisation level coefficients
     * @param modipGrip modip grid
     */
    FieldNeQuickParameters(final Field<T> field, final DateTimeComponents dateTime, final double[][][] f2,
                           final double[][][] fm3, final T latitude, final T longitude,
                           final double[] alpha, final double[][] modipGrip) {

        // Zero
        final T zero = field.getZero();

        // MODIP in degrees
        final T modip = computeMODIP(latitude, longitude, modipGrip);
        // Effective ionisation level Az
        final T az = computeAz(modip, alpha);
        // Effective sunspot number (Eq. 19)
        final T azr = FastMath.sqrt(az.subtract(63.7).multiply(1123.6).add(167273.0)).subtract(408.99);
        // Date and Time components
        final DateComponents date = dateTime.getDate();
        final TimeComponents time = dateTime.getTime();
        // Hours
        final double hours  = time.getSecondsInUTCDay() / 3600.0;
        // Effective solar zenith angle in radians
        final T xeff = computeEffectiveSolarAngle(date.getMonth(), hours, latitude, longitude);

        // Coefficients for F2 layer parameters
        // Compute the array of interpolated coefficients for foF2 (Eq. 44)
        final T[][] af2 = MathArrays.buildArray(field, 76, 13);
        for (int j = 0; j < 76; j++) {
            for (int k = 0; k < 13; k++ ) {
                af2[j][k] = azr.multiply(0.01).negate().add(1.0).multiply(f2[0][j][k]).add(azr.multiply(0.01).multiply(f2[1][j][k]));
            }
        }

        // Compute the array of interpolated coefficients for M(3000)F2 (Eq. 46)
        final T[][] am3 = MathArrays.buildArray(field, 49, 9);
        for (int j = 0; j < 49; j++) {
            for (int k = 0; k < 9; k++ ) {
                am3[j][k] = azr.multiply(0.01).negate().add(1.0).multiply(fm3[0][j][k]).add(azr.multiply(0.01).multiply(fm3[1][j][k]));
            }
        }

        // E layer maximum density height in km (Eq. 78)
        this.hmE = field.getZero().add(120.0);
        // E layer critical frequency in MHz
        final T foE = computefoE(date.getMonth(), az, xeff, latitude);
        // E layer maximum density in 10^11 m-3 (Eq. 36)
        final T nmE = foE.multiply(foE).multiply(0.124);

        // Time argument (Eq. 49)
        final double t = FastMath.toRadians(15 * hours) - FastMath.PI;
        // Compute Fourier time series for foF2 and M(3000)F2
        final T[] cf2 = computeCF2(field, af2, t);
        final T[] cm3 = computeCm3(field, am3, t);
        // F2 layer critical frequency in MHz
        final T foF2 = computefoF2(field, modip, cf2, latitude, longitude);
        // Maximum Usable Frequency factor
        final T mF2  = computeMF2(field, modip, cm3, latitude, longitude);
        // F2 layer maximum density in 10^11 m-3
        this.nmF2 = foF2.multiply(foF2).multiply(0.124);
        // F2 layer maximum density height in km
        this.hmF2 = computehmF2(field, foE, foF2, mF2);

        // F1 layer critical frequency in MHz
        final T foF1 = computefoF1(field, foE, foF2);
        // F1 layer maximum density in 10^11 m-3
        final T nmF1;
        if (foF1.getReal() <= 0.0 && foE.getReal() > 2.0) {
            final T foEpopf = foE.add(0.5);
            nmF1 = foEpopf.multiply(foEpopf).multiply(0.124);
        } else {
            nmF1 = foF1.multiply(foF1).multiply(0.124);
        }
        // F1 layer maximum density height in km
        this.hmF1 = hmF2.add(hmE).multiply(0.5);

        // Thickness parameters (Eq. 85 to 89)
        final T a = clipExp(FastMath.log(foF2.multiply(foF2)).multiply(0.857).add(FastMath.log(mF2).multiply(2.02)).add(-3.467)).multiply(0.01);
        this.b2Bot = nmF2.divide(a).multiply(0.385);
        this.b1Top = hmF2.subtract(hmF1).multiply(0.3);
        this.b1Bot = hmF1.subtract(hmE).multiply(0.5);
        this.beTop = FastMath.max(b1Bot, zero.add(7.0));
        this.beBot = zero.add(5.0);

        // Layer amplitude coefficients
        this.amplitudes = computeLayerAmplitudes(field, nmE, nmF1, foF1);

        // Topside thickness parameter
        this.h0 = computeH0(field, date.getMonth(), azr);
    }

    /**
     * Get the F2 layer maximum density.
     * @return nmF2
     */
    public T getNmF2() {
        return nmF2;
    }

    /**
     * Get the F2 layer maximum density height.
     * @return hmF2 in km
     */
    public T getHmF2() {
        return hmF2;
    }

    /**
     * Get the F1 layer maximum density height.
     * @return hmF1 in km
     */
    public T getHmF1() {
        return hmF1;
    }

    /**
     * Get the E layer maximum density height.
     * @return hmE in km
     */
    public T getHmE() {
        return hmE;
    }

    /**
     * Get the F2 layer thickness parameter (bottom).
     * @return B2Bot in km
     */
    public T getB2Bot() {
        return b2Bot;
    }

    /**
     * Get the F1 layer thickness parameter (top).
     * @return B1Top in km
     */
    public T getB1Top() {
        return b1Top;
    }

    /**
     * Get the F1 layer thickness parameter (bottom).
     * @return B1Bot in km
     */
    public T getB1Bot() {
        return b1Bot;
    }

    /**
     * Get the E layer thickness parameter (bottom).
     * @return BeBot in km
     */
    public T getBEBot() {
        return beBot;
    }

    /**
     * Get the E layer thickness parameter (top).
     * @return BeTop in km
     */
    public T getBETop() {
        return beTop;
    }

    /**
     * Get the F2, F1 and E layer amplitudes.
     * <p>
     * The resulting element is an array having the following form:
     * <ul>
     * <li>double[0] = A1 → F2 layer amplitude
     * <li>double[1] = A2 → F1 layer amplitude
     * <li>double[2] = A3 → E  layer amplitude
     * </ul>
     * @return layer amplitudes
     */
    public T[] getLayerAmplitudes() {
        return amplitudes.clone();
    }

    /**
     * Get the topside thickness parameter H0.
     * @return H0 in km
     */
    public T getH0() {
        return h0;
    }

    /**
     * Computes the value of the modified dip latitude (MODIP) for the
     * given latitude and longitude.
     *
     * @param lat receiver latitude, radians
     * @param lon receiver longitude, radians
     * @param stModip modip grid
     * @return the MODIP in degrees
     */
    private T computeMODIP(final T lat, final T lon, final double[][] stModip) {

        // Zero
        final T zero = lat.getField().getZero();

        // For the MODIP computation, the latitude and longitude have to be converted in degrees
        final T latitude  = FastMath.toDegrees(lat);
        final T longitude = FastMath.toDegrees(lon);

        // Extreme cases
        if (latitude.getReal() == 90.0 || latitude.getReal() == -90.0) {
            return latitude;
        }

        // Auxiliary parameter l (Eq. 6 to 8)
        final int lF = (int) ((longitude.getReal() + 180) * 0.1);
        int l = lF - 2;
        if (l < -2) {
            l += 36;
        } else if (l > 33) {
            l -= 36;
        }

        // Auxiliary parameter a (Eq. 9 to 11)
        final T a  = latitude.add(90).multiply(0.2).add(1.0);
        final T aF = FastMath.floor(a);
        // Eq. 10
        final T x = a.subtract(aF);
        // Eq. 11
        final int i = (int) aF.getReal() - 2;

        // zi coefficients (Eq. 12 and 13)
        final T z1 = interpolate(zero.add(stModip[i + 1][l + 2]), zero.add(stModip[i + 2][l + 2]),
                                      zero.add(stModip[i + 3][l + 2]), zero.add(stModip[i + 4][l + 2]), x);
        final T z2 = interpolate(zero.add(stModip[i + 1][l + 3]), zero.add(stModip[i + 2][l + 3]),
                                      zero.add(stModip[i + 3][l + 3]), zero.add(stModip[i + 4][l + 3]), x);
        final T z3 = interpolate(zero.add(stModip[i + 1][l + 4]), zero.add(stModip[i + 2][l + 4]),
                                      zero.add(stModip[i + 3][l + 4]), zero.add(stModip[i + 4][l + 4]), x);
        final T z4 = interpolate(zero.add(stModip[i + 1][l + 5]), zero.add(stModip[i + 2][l + 5]),
                                      zero.add(stModip[i + 3][l + 5]), zero.add(stModip[i + 4][l + 5]), x);

        // Auxiliary parameter b (Eq. 14 and 15)
        final T b  = longitude.add(180).multiply(0.1);
        final T bF = FastMath.floor(b);
        final T y  = b.subtract(bF);

        // MODIP (Ref Eq. 16)
        final T modip = interpolate(z1, z2, z3, z4, y);

        return modip;
    }

    /**
     * This method computes the effective ionisation level Az.
     * <p>
     * This parameter is used for the computation of the Total Electron Content (TEC).
     * </p>
     * @param modip modified dip latitude (MODIP) in degrees
     * @param alpha effective ionisation level coefficients
     * @return the ionisation level Az
     */
    private T computeAz(final T modip, final double[] alpha) {
        // Field
        final Field<T> field = modip.getField();
        // Zero
        final T zero = field.getZero();
        // Particular condition (Eq. 17)
        if (alpha[0] == 0.0 && alpha[1] == 0.0 && alpha[2] == 0.0) {
            return zero.add(63.7);
        }
        // Az = a0 + modip * a1 + modip^2 * a2 (Eq. 18)
        T az = modip.multiply(alpha[2]).add(alpha[1]).multiply(modip).add(alpha[0]);
        // If Az < 0 -> Az = 0
        az = FastMath.max(zero, az);
        // If Az > 400 -> Az = 400
        az = FastMath.min(zero.add(400.0), az);
        return az;
    }

    /**
     * This method computes the effective solar zenith angle.
     * <p>
     * The effective solar zenith angle is compute as a function of the
     * solar zenith angle and the solar zenith angle at day night transition.
     * </p>
     * @param month current month of the year
     * @param hours universal time (hours)
     * @param latitude in radians
     * @param longitude in radians
     * @return the effective solar zenith angle, radians
     */
    private T computeEffectiveSolarAngle(final int month,
                                         final double hours,
                                         final T latitude,
                                         final T longitude) {
        // Zero
        final T zero = latitude.getField().getZero();
        // Local time (Eq.4)
        final T lt = longitude.divide(FastMath.toRadians(15.0)).add(hours);
        // Day of year at the middle of the month (Eq. 20)
        final double dy = 30.5 * month - 15.0;
        // Time (Eq. 21)
        final double t = dy + (18 - hours) / 24;
        // Arguments am and al (Eq. 22 and 23)
        final double am = FastMath.toRadians(0.9856 * t - 3.289);
        final double al = am + FastMath.toRadians(1.916 * FastMath.sin(am) + 0.020 * FastMath.sin(2.0 * am) + 282.634);
        // Sine and cosine of solar declination (Eq. 24 and 25)
        final double sDec = 0.39782 * FastMath.sin(al);
        final double cDec = FastMath.sqrt(1. - sDec * sDec);
        // Solar zenith angle, deg (Eq. 26 and 27)
        final FieldSinCos<T> scLat   = FastMath.sinCos(latitude);
        final T coef    = lt.negate().add(12.0).multiply(FastMath.PI / 12);
        final T cZenith = scLat.sin().multiply(sDec).add(scLat.cos().multiply(cDec).multiply(FastMath.cos(coef)));
        final T angle   = FastMath.atan2(FastMath.sqrt(cZenith.multiply(cZenith).negate().add(1.0)), cZenith);
        final T x       = FastMath.toDegrees(angle);
        // Effective solar zenith angle (Eq. 28)
        final T xeff = join(clipExp(x.multiply(0.2).negate().add(20.0)).multiply(0.24).negate().add(90.0), x, zero.add(12.0), x.subtract(X0));
        return FastMath.toRadians(xeff);
    }

    /**
     * This method computes the E layer critical frequency at a given location.
     * @param month current month
     * @param az ffective ionisation level
     * @param xeff effective solar zenith angle in radians
     * @param latitude latitude in radians
     * @return the E layer critical frequency at a given location in MHz
     */
    private T computefoE(final int month, final T az,
                         final T xeff, final T latitude) {
        // The latitude has to be converted in degrees
        final T lat = FastMath.toDegrees(latitude);
        // Square root of the effective ionisation level
        final T sqAz = FastMath.sqrt(az);
        // seas parameter (Eq. 30 to 32)
        final int seas;
        if (month == 1 || month == 2 || month == 11 || month == 12) {
            seas = -1;
        } else if (month == 3 || month == 4 || month == 9 || month == 10) {
            seas = 0;
        } else {
            seas = 1;
        }
        // Latitudinal dependence (Eq. 33 and 34)
        final T ee = clipExp(lat.multiply(0.3));
        final T seasp = ee.subtract(1.0).divide(ee.add(1.0)).multiply(seas);
        // Critical frequency (Eq. 35)
        final T coef = seasp.multiply(0.019).negate().add(1.112);
        final T foE = FastMath.sqrt(coef .multiply(coef).multiply(sqAz).multiply(FastMath.cos(xeff).pow(0.6)).add(0.49));
        return foE;
    }

    /**
     * Computes the F2 layer height of maximum electron density.
     * @param field field of the elements
     * @param foE E layer layer critical frequency in MHz
     * @param foF2 F2 layer layer critical frequency in MHz
     * @param mF2 maximum usable frequency factor
     * @return hmF2 in km
     */
    private T computehmF2(final Field<T> field, final T foE, final T foF2, final T mF2) {
        // Zero
        final T zero = field.getZero();
        // Ratio
        final T fo = foF2.divide(foE);
        final T ratio = join(fo, zero.add(1.75), zero.add(20.0), fo.subtract(1.75));

        // deltaM parameter
        T deltaM = zero.subtract(0.012);
        if (foE.getReal() >= 1e-30) {
            deltaM = deltaM.add(ratio.subtract(1.215).divide(0.253).reciprocal());
        }

        // hmF2 Eq. 80
        final T mF2Sq = mF2.multiply(mF2);
        final T temp  = FastMath.sqrt(mF2Sq.multiply(0.0196).add(1.0).divide(mF2Sq.multiply(1.2967).subtract(1.0)));
        final T height  = mF2.multiply(1490.0).multiply(temp).divide(mF2.add(deltaM)).subtract(176.0);
        return height;
    }

    /**
     * Computes cf2 coefficients.
     * @param field field of the elements
     * @param af2 interpolated coefficients for foF2
     * @param t time argument
     * @return the cf2 coefficients array
     */
    private T[] computeCF2(final Field<T> field, final T[][] af2, final double t) {
        // Eq. 50
        final T[] cf2 = MathArrays.buildArray(field, 76);
        for (int i = 0; i < cf2.length; i++) {
            T sum = field.getZero();
            for (int k = 0; k < 6; k++) {
                final SinCos sc = FastMath.sinCos((k + 1) * t);
                sum = sum.add(af2[i][2 * k + 1].multiply(sc.sin()).add(af2[i][2 * (k + 1)].multiply(sc.cos())));
            }
            cf2[i] = af2[i][0].add(sum);
        }
        return cf2;
    }

    /**
     * Computes Cm3 coefficients.
     * @param field field of the elements
     * @param am3 interpolated coefficients for foF2
     * @param t time argument
     * @return the Cm3 coefficients array
     */
    private T[] computeCm3(final Field<T> field, final T[][] am3, final double t) {
        // Eq. 51
        final T[] cm3 = MathArrays.buildArray(field, 49);
        for (int i = 0; i < cm3.length; i++) {
            T sum = field.getZero();
            for (int k = 0; k < 4; k++) {
                final SinCos sc = FastMath.sinCos((k + 1) * t);
                sum = sum.add(am3[i][2 * k + 1].multiply(sc.sin()).add(am3[i][2 * (k + 1)].multiply(sc.cos())));
            }
            cm3[i] = am3[i][0].add(sum);
        }
        return cm3;
    }

    /**
     * This method computes the F2 layer critical frequency.
     * @param field field of the elements
     * @param modip modified DIP latitude, in degrees
     * @param cf2 Fourier time series for foF2
     * @param latitude latitude in radians
     * @param longitude longitude in radians
     * @return the F2 layer critical frequency, MHz
     */
    private T computefoF2(final Field<T> field, final T modip, final T[] cf2,
                          final T latitude, final T longitude) {

        // One
        final T one = field.getOne();

        // Legendre grades (Eq. 63)
        final int[] q = new int[] {
            12, 12, 9, 5, 2, 1, 1, 1, 1
        };

        // Array for geographic terms
        final T[] g = MathArrays.buildArray(field, cf2.length);
        g[0] = one;

        // MODIP coefficients Eq. 57
        final T sinMODIP = FastMath.sin(FastMath.toRadians(modip));
        final T[] m = MathArrays.buildArray(field, 12);
        m[0] = one;
        for (int i = 1; i < q[0]; i++) {
            m[i] = sinMODIP.multiply(m[i - 1]);
            g[i] = m[i];
        }

        // Latitude coefficients (Eq. 58)
        final T cosLat = FastMath.cos(latitude);
        final T[] p = MathArrays.buildArray(field, 8);
        p[0] = cosLat;
        for (int n = 2; n < 9; n++) {
            p[n - 1] = cosLat.multiply(p[n - 2]);
        }

        // latitude and longitude terms
        int index = 12;
        for (int i = 1; i < q.length; i++) {
            final FieldSinCos<T> sc = FastMath.sinCos(longitude.multiply(i));
            for (int j = 0; j < q[i]; j++) {
                g[index++] = m[j].multiply(p[i - 1]).multiply(sc.cos());
                g[index++] = m[j].multiply(p[i - 1]).multiply(sc.sin());
            }
        }

        // Compute foF2 by linear combination
        final T frequency = one.linearCombination(g, cf2);
        return frequency;
    }

    /**
     * This method computes the Maximum Usable Frequency factor.
     * @param field field of the elements
     * @param modip modified DIP latitude, in degrees
     * @param cm3 Fourier time series for M(3000)F2
     * @param latitude latitude in radians
     * @param longitude longitude in radians
     * @return the Maximum Usable Frequency factor
     */
    private T computeMF2(final Field<T> field, final T modip, final T[] cm3,
                         final T latitude, final T longitude) {

        // One
        final T one = field.getOne();
        // Legendre grades (Eq. 71)
        final int[] r = new int[] {
            7, 8, 6, 3, 2, 1, 1
        };

        // Array for geographic terms
        final T[] g = MathArrays.buildArray(field, cm3.length);
        g[0] = one;

        // MODIP coefficients Eq. 57
        final T sinMODIP = FastMath.sin(FastMath.toRadians(modip));
        final T[] m = MathArrays.buildArray(field, 12);
        m[0] = one;
        for (int i = 1; i < 12; i++) {
            m[i] = sinMODIP.multiply(m[i - 1]);
            if (i < 7) {
                g[i] = m[i];
            }
        }

        // Latitude coefficients (Eq. 58)
        final T cosLat = FastMath.cos(latitude);
        final T[] p = MathArrays.buildArray(field, 8);
        p[0] = cosLat;
        for (int n = 2; n < 9; n++) {
            p[n - 1] = cosLat.multiply(p[n - 2]);
        }

        // latitude and longitude terms
        int index = 7;
        for (int i = 1; i < r.length; i++) {
            final FieldSinCos<T> sc = FastMath.sinCos(longitude.multiply(i));
            for (int j = 0; j < r[i]; j++) {
                g[index++] = m[j].multiply(p[i - 1]).multiply(sc.cos());
                g[index++] = m[j].multiply(p[i - 1]).multiply(sc.sin());
            }
        }

        // Compute m3000 by linear combination
        final T m3000 = one.linearCombination(g, cm3);
        return m3000;
    }

    /**
     * This method computes the F1 layer critical frequency.
     * <p>
     * This computation performs the algorithm exposed in Annex F
     * of the reference document.
     * </p>
     * @param field field of the elements
     * @param foE the E layer critical frequency, MHz
     * @return the F1 layer critical frequency, MHz
     * @param foF2 the F2 layer critical frequency, MHz
     */
    private T computefoF1(final Field<T> field, final T foE, final T foF2) {
        final T zero = field.getZero();
        final T temp  = join(foE.multiply(1.4), zero, zero.add(1000.0), foE.subtract(2.0));
        final T temp2 = join(zero, temp, zero.add(1000.0), foE.subtract(temp));
        final T value = join(temp2, temp2.multiply(0.85), zero.add(60.0), foF2.multiply(0.85).subtract(temp2));
        if (value.getReal() < 1.0E-6) {
            return zero;
        } else {
            return value;
        }
    }

    /**
     * This method allows the computation of the F2, F1 and E layer amplitudes.
     * <p>
     * The resulting element is an array having the following form:
     * <ul>
     * <li>double[0] = A1 → F2 layer amplitude
     * <li>double[1] = A2 → F1 layer amplitude
     * <li>double[2] = A3 → E  layer amplitude
     * </ul>
     * </p>
     * @param field field of the elements
     * @param nmE E layer maximum density in 10^11 m-3
     * @param nmF1 F1 layer maximum density in 10^11 m-3
     * @param foF1 F1 layer critical frequency in MHz
     * @return a three components array containing the layer amplitudes
     */
    private T[] computeLayerAmplitudes(final Field<T> field, final T nmE, final T nmF1, final T foF1) {
        // Zero
        final T zero = field.getZero();

        // Initialize array
        final T[] amplitude = MathArrays.buildArray(field, 3);

        // F2 layer amplitude (Eq. 90)
        final T a1 = nmF2.multiply(4.0);
        amplitude[0] = a1;

        // F1 and E layer amplitudes (Eq. 91 to 98)
        if (foF1.getReal() < 0.5) {
            amplitude[1] = zero;
            amplitude[2] = nmE.subtract(epst(a1, hmF2, b2Bot, hmE)).multiply(4.0);
        } else {
            T a2a = zero;
            T a3a = nmE.multiply(4.0);
            for (int i = 0; i < 5; i++) {
                a2a = nmF1.subtract(epst(a1, hmF2, b2Bot, hmF1)).subtract(epst(a3a, hmE, beTop, hmF1)).multiply(4.0);
                a2a = join(a2a, nmF1.multiply(0.8), field.getOne(), a2a.subtract(nmF1.multiply(0.8)));
                a3a = nmE.subtract(epst(a2a, hmF1, b1Bot, hmE)).subtract(epst(a1, hmF2, b2Bot, hmE)).multiply(4.0);
            }
            amplitude[1] = a2a;
            amplitude[2] = join(a3a, zero.add(0.05), zero.add(60.0), a3a.subtract(0.005));
        }

        return amplitude;
    }

    /**
     * This method computes the topside thickness parameter H0.
     *
     * @param field field of the elements
     * @param month current month
     * @param azr effective sunspot number
     * @return H0 in km
     */
    private T computeH0(final Field<T> field, final int month, final T azr) {

        // One
        final T one = field.getOne();

        // Auxiliary parameter ka (Eq. 99 and 100)
        final T ka;
        if (month > 3 && month < 10) {
            // month = 4,5,6,7,8,9
            ka = azr.multiply(0.014).add(hmF2.multiply(0.008)).negate().add(6.705);
        } else {
            // month = 1,2,3,10,11,12
            final T ratio = hmF2.divide(b2Bot);
            ka = ratio.multiply(ratio).multiply(0.097).add(nmF2.multiply(0.153)).add(-7.77);
        }

        // Auxiliary parameter kb (Eq. 101 and 102)
        T kb = join(ka, one.multiply(2.0), one, ka.subtract(2.0));
        kb = join(one.multiply(8.0), kb, one, kb.subtract(8.0));

        // Auxiliary parameter Ha (Eq. 103)
        final T hA = kb.multiply(b2Bot);

        // Auxiliary parameters x and v (Eq. 104 and 105)
        final T x = hA.subtract(150.0).multiply(0.01);
        final T v = x.multiply(0.041163).subtract(0.183981).multiply(x).add(1.424472);

        // Topside thickness parameter (Eq. 106)
        final T h = hA.divide(v);
        return h;
    }

    /**
     * A clipped exponential function.
     * <p>
     * This function, describe in section F.2.12.2 of the reference document, is
     * recommanded for the computation of exponential values.
     * </p>
     * @param power power for exponential function
     * @return clipped exponential value
     */
    private T clipExp(final T power) {
        final T zero = power.getField().getZero();
        if (power.getReal() > 80.0) {
            return zero.add(5.5406E34);
        } else if (power.getReal() < -80) {
            return zero.add(1.8049E-35);
        } else {
            return FastMath.exp(power);
        }
    }

    /**
     * This method provides a third order interpolation function
     * as recommended in the reference document (Ref Eq. 128 to Eq. 138)
     *
     * @param z1 z1 coefficient
     * @param z2 z2 coefficient
     * @param z3 z3 coefficient
     * @param z4 z4 coefficient
     * @param x position
     * @return a third order interpolation
     */
    private T interpolate(final T z1, final T z2,
                          final T z3, final T z4,
                          final T x) {

        if (FastMath.abs(2.0 * x.getReal()) < 1e-10) {
            return z2;
        }

        final T delta = x.multiply(2.0).subtract(1.0);
        final T g1 = z3.add(z2);
        final T g2 = z3.subtract(z2);
        final T g3 = z4.add(z1);
        final T g4 = z4.subtract(z1).divide(3.0);
        final T a0 = g1.multiply(9.0).subtract(g3);
        final T a1 = g2.multiply(9.0).subtract(g4);
        final T a2 = g3.subtract(g1);
        final T a3 = g4.subtract(g2);
        final T zx = delta.multiply(a3).add(a2).multiply(delta).add(a1).multiply(delta).add(a0).multiply(0.0625);

        return zx;
    }

    /**
     * Allows smooth joining of functions f1 and f2
     * (i.e. continuous first derivatives) at origin.
     * <p>
     * This function, describe in section F.2.12.1 of the reference document, is
     * recommanded for computational efficiency.
     * </p>
     * @param dF1 first function
     * @param dF2 second function
     * @param dA width of transition region
     * @param dX x value
     * @return the computed value
     */
    private T join(final T dF1, final T dF2,
                   final T dA, final T dX) {
        final T ee = clipExp(dA.multiply(dX));
        return dF1.multiply(ee).add(dF2).divide(ee.add(1.0));
    }

    /**
     * The Epstein function.
     * <p>
     * This function, describe in section 2.5.1 of the reference document, is used
     * as a basis analytical function in NeQuick for the construction of the ionospheric layers.
     * </p>
     * @param x x parameter
     * @param y y parameter
     * @param z z parameter
     * @param w w parameter
     * @return value of the epstein function
     */
    private T epst(final T x, final T y,
                   final T z, final T w) {
        final T ex  = clipExp(w.subtract(y).divide(z));
        final T opex = ex.add(1.0);
        final T epst = x.multiply(ex).divide(opex.multiply(opex));
        return epst;
    }

}
