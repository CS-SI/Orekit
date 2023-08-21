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

import org.hipparchus.util.FastMath;
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
class NeQuickParameters {

    /** Solar zenith angle at day night transition, degrees. */
    private static final double X0 = 86.23292796211615;

    /** F2 layer maximum density. */
    private final double nmF2;

    /** F2 layer maximum density height [km]. */
    private final double hmF2;

    /** F1 layer maximum density height [km]. */
    private final double hmF1;

    /** E layer maximum density height [km]. */
    private final double hmE;

    /** F2 layer bottom thickness parameter [km]. */
    private final double b2Bot;

    /** F1 layer top thickness parameter [km]. */
    private final double b1Top;

    /** F1 layer bottom thickness parameter [km]. */
    private final double b1Bot;

    /** E layer top thickness parameter [km]. */
    private final double beTop;

    /** E layer bottom thickness parameter [km]. */
    private final double beBot;

    /** topside thickness parameter [km]. */
    private final double h0;

    /** Layer amplitudes. */
    private final double[] amplitudes;

    /**
     * Build a new instance.
     * @param dateTime current date time components
     * @param f2 F2 coefficients used by the F2 layer
     * @param fm3 Fm3 coefficients used by the F2 layer
     * @param latitude latitude of a point along the integration path, in radians
     * @param longitude longitude of a point along the integration path, in radians
     * @param alpha effective ionisation level coefficients
     * @param modipGrip modip grid
     */
    NeQuickParameters(final DateTimeComponents dateTime, final double[][][] f2,
                      final double[][][] fm3, final double latitude, final double longitude,
                      final double[] alpha, final double[][] modipGrip) {

        // MODIP in degrees
        final double modip = computeMODIP(latitude, longitude, modipGrip);
        // Effective ionisation level Az
        final double az = computeAz(modip, alpha);
        // Effective sunspot number (Eq. 19)
        final double azr = FastMath.sqrt(167273.0 + (az - 63.7) * 1123.6) - 408.99;
        // Date and Time components
        final DateComponents date = dateTime.getDate();
        final TimeComponents time = dateTime.getTime();
        // Hours
        final double hours  = time.getSecondsInUTCDay() / 3600.0;
        // Effective solar zenith angle in radians
        final double xeff = computeEffectiveSolarAngle(date.getMonth(), hours, latitude, longitude);

        // Coefficients for F2 layer parameters
        // Compute the array of interpolated coefficients for foF2 (Eq. 44)
        final double[][] af2 = new double[76][13];
        for (int j = 0; j < 76; j++) {
            for (int k = 0; k < 13; k++ ) {
                af2[j][k] = f2[0][j][k] * (1.0 - (azr * 0.01)) + f2[1][j][k] * (azr * 0.01);
            }
        }

        // Compute the array of interpolated coefficients for M(3000)F2 (Eq. 46)
        final  double[][] am3 = new double[49][9];
        for (int j = 0; j < 49; j++) {
            for (int k = 0; k < 9; k++ ) {
                am3[j][k] = fm3[0][j][k] * (1.0 - (azr * 0.01)) + fm3[1][j][k] * (azr * 0.01);
            }
        }

        // E layer maximum density height in km (Eq. 78)
        this.hmE = 120.0;
        // E layer critical frequency in MHz
        final double foE = computefoE(date.getMonth(), az, xeff, latitude);
        // E layer maximum density in 10^11 m-3 (Eq. 36)
        final double nmE = 0.124 * foE * foE;

        // Time argument (Eq. 49)
        final double t = FastMath.toRadians(15 * hours) - FastMath.PI;
        // Compute Fourier time series for foF2 and M(3000)F2
        final double[] cf2 = computeCF2(af2, t);
        final double[] cm3 = computeCm3(am3, t);
        // F2 layer critical frequency in MHz
        final double foF2 = computefoF2(modip, cf2, latitude, longitude);
        // Maximum Usable Frequency factor
        final double mF2  = computeMF2(modip, cm3, latitude, longitude);
        // F2 layer maximum density in 10^11 m-3
        this.nmF2 = 0.124 * foF2 * foF2;
        // F2 layer maximum density height in km
        this.hmF2 = computehmF2(foE, foF2, mF2);

        // F1 layer critical frequency in MHz
        final double foF1 = computefoF1(foE, foF2);
        // F1 layer maximum density in 10^11 m-3
        final double nmF1;
        if (foF1 <= 0.0 && foE > 2.0) {
            final double foEpopf = foE + 0.5;
            nmF1 = 0.124 * foEpopf * foEpopf;
        } else {
            nmF1 = 0.124 * foF1 * foF1;
        }
        // F1 layer maximum density height in km
        this.hmF1 = 0.5 * (hmF2 + hmE);

        // Thickness parameters (Eq. 85 to 89)
        final double a = 0.01 * clipExp(-3.467 + 0.857 * FastMath.log(foF2 * foF2) + 2.02 * FastMath.log(mF2));
        this.b2Bot = 0.385 * nmF2 / a;
        this.b1Top = 0.3 * (hmF2 - hmF1);
        this.b1Bot = 0.5 * (hmF1 - hmE);
        this.beTop = FastMath.max(b1Bot, 7.0);
        this.beBot = 5.0;

        // Layer amplitude coefficients
        this.amplitudes = computeLayerAmplitudes(nmE, nmF1, foF1);

        // Topside thickness parameter
        this.h0 = computeH0(date.getMonth(), azr);
    }

    /**
     * Get the F2 layer maximum density.
     * @return nmF2
     */
    public double getNmF2() {
        return nmF2;
    }

    /**
     * Get the F2 layer maximum density height.
     * @return hmF2 in km
     */
    public double getHmF2() {
        return hmF2;
    }

    /**
     * Get the F1 layer maximum density height.
     * @return hmF1 in km
     */
    public double getHmF1() {
        return hmF1;
    }

    /**
     * Get the E layer maximum density height.
     * @return hmE in km
     */
    public double getHmE() {
        return hmE;
    }

    /**
     * Get the F2 layer thickness parameter (bottom).
     * @return B2Bot in km
     */
    public double getB2Bot() {
        return b2Bot;
    }

    /**
     * Get the F1 layer thickness parameter (top).
     * @return B1Top in km
     */
    public double getB1Top() {
        return b1Top;
    }

    /**
     * Get the F1 layer thickness parameter (bottom).
     * @return B1Bot in km
     */
    public double getB1Bot() {
        return b1Bot;
    }

    /**
     * Get the E layer thickness parameter (bottom).
     * @return BeBot in km
     */
    public double getBEBot() {
        return beBot;
    }

    /**
     * Get the E layer thickness parameter (top).
     * @return BeTop in km
     */
    public double getBETop() {
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
    public double[] getLayerAmplitudes() {
        return amplitudes.clone();
    }

    /**
     * Get the topside thickness parameter H0.
     * @return H0 in km
     */
    public double getH0() {
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
    private double computeMODIP(final double lat, final double lon, final double[][] stModip) {

        // For the MODIP computation, latitude and longitude have to be converted in degrees
        final double latitude = FastMath.toDegrees(lat);
        final double longitude = FastMath.toDegrees(lon);

        // Extreme cases
        if (latitude == 90.0 || latitude == -90.0) {
            return latitude;
        }

        // Auxiliary parameter l (Eq. 6 to 8)
        final int lF = (int) ((longitude + 180) * 0.1);
        int l = lF - 2;
        if (l < -2) {
            l += 36;
        } else if (l > 33) {
            l -= 36;
        }

        // Auxiliary parameter a (Eq. 9 to 11)
        final double a  = 0.2 * (latitude + 90) + 1.0;
        final double aF = FastMath.floor(a);
        // Eq. 10
        final double x = a - aF;
        // Eq. 11
        final int i = (int) aF - 2;

        // zi coefficients (Eq. 12 and 13)
        final double z1 = interpolate(stModip[i + 1][l + 2], stModip[i + 2][l + 2], stModip[i + 3][l + 2], stModip[i + 4][l + 2], x);
        final double z2 = interpolate(stModip[i + 1][l + 3], stModip[i + 2][l + 3], stModip[i + 3][l + 3], stModip[i + 4][l + 3], x);
        final double z3 = interpolate(stModip[i + 1][l + 4], stModip[i + 2][l + 4], stModip[i + 3][l + 4], stModip[i + 4][l + 4], x);
        final double z4 = interpolate(stModip[i + 1][l + 5], stModip[i + 2][l + 5], stModip[i + 3][l + 5], stModip[i + 4][l + 5], x);

        // Auxiliary parameter b (Eq. 14 and 15)
        final double b  = (longitude + 180) * 0.1;
        final double bF = FastMath.floor(b);
        final double y  = b - bF;

        // MODIP (Ref Eq. 16)
        final double modip = interpolate(z1, z2, z3, z4, y);

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
    private double computeAz(final double modip, final double[] alpha) {
        // Particular condition (Eq. 17)
        if (alpha[0] == 0.0 && alpha[1] == 0.0 && alpha[2] == 0.0) {
            return 63.7;
        }
        // Az = a0 + modip * a1 + modip^2 * a2 (Eq. 18)
        double az = alpha[0] + modip * (alpha[1] + modip * alpha[2]);
        // If Az < 0 -> Az = 0
        az = FastMath.max(0.0, az);
        // If Az > 400 -> Az = 400
        az = FastMath.min(400.0, az);
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
    private double computeEffectiveSolarAngle(final int month,
                                              final double hours,
                                              final double latitude,
                                              final double longitude) {
        // Local time (Eq.4)
        final double lt = hours + longitude / FastMath.toRadians(15.0);
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
        final SinCos scLat   = FastMath.sinCos(latitude);
        final double coef    = (FastMath.PI / 12) * (12 - lt);
        final double cZenith = scLat.sin() * sDec + scLat.cos() * cDec * FastMath.cos(coef);
        final double angle   = FastMath.atan2(FastMath.sqrt(1.0 - cZenith * cZenith), cZenith);
        final double x       = FastMath.toDegrees(angle);
        // Effective solar zenith angle (Eq. 28)
        final double xeff = join(90.0 - 0.24 * clipExp(20.0 - 0.2 * x), x, 12.0, x - X0);
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
    private double computefoE(final int month, final double az,
                              final double xeff, final double latitude) {
        // The latitude has to be converted in degrees
        final double lat = FastMath.toDegrees(latitude);
        // Square root of the effective ionisation level
        final double sqAz = FastMath.sqrt(az);
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
        final double ee = clipExp(0.3 * lat);
        final double seasp = seas * ((ee - 1.0) / (ee + 1.0));
        // Critical frequency (Eq. 35)
        final double coef = 1.112 - 0.019 * seasp;
        final double foE = FastMath.sqrt(coef * coef * sqAz * FastMath.pow(FastMath.cos(xeff), 0.6) + 0.49);
        return foE;
    }

    /**
     * Computes the F2 layer height of maximum electron density.
     * @param foE E layer layer critical frequency in MHz
     * @param foF2 F2 layer layer critical frequency in MHz
     * @param mF2 maximum usable frequency factor
     * @return hmF2 in km
     */
    private double computehmF2(final double foE, final double foF2, final double mF2) {
        // Ratio
        final double fo = foF2 / foE;
        final double ratio = join(fo, 1.75, 20.0, fo - 1.75);

        // deltaM parameter
        double deltaM = -0.012;
        if (foE >= 1e-30) {
            deltaM += 0.253 / (ratio - 1.215);
        }

        // hmF2 Eq. 80
        final double mF2Sq = mF2 * mF2;
        final double temp  = FastMath.sqrt((0.0196 * mF2Sq + 1) / (1.2967 * mF2Sq - 1.0));
        final double height  = ((1490.0 * mF2 * temp) / (mF2 + deltaM)) - 176.0;
        return height;
    }

    /**
     * Computes cf2 coefficients.
     * @param af2 interpolated coefficients for foF2
     * @param t time argument
     * @return the cf2 coefficients array
     */
    private double[] computeCF2(final double[][] af2, final double t) {
        // Eq. 50
        final double[] cf2 = new double[76];
        for (int i = 0; i < cf2.length; i++) {
            double sum = 0.0;
            for (int k = 0; k < 6; k++) {
                final SinCos sc = FastMath.sinCos((k + 1) * t);
                sum += af2[i][2 * k + 1] * sc.sin() + af2[i][2 * (k + 1)] * sc.cos();
            }
            cf2[i] = af2[i][0] + sum;
        }
        return cf2;
    }

    /**
     * Computes Cm3 coefficients.
     * @param am3 interpolated coefficients for foF2
     * @param t time argument
     * @return the Cm3 coefficients array
     */
    private double[] computeCm3(final double[][] am3, final double t) {
        // Eq. 51
        final double[] cm3 = new double[49];
        for (int i = 0; i < cm3.length; i++) {
            double sum = 0.0;
            for (int k = 0; k < 4; k++) {
                final SinCos sc = FastMath.sinCos((k + 1) * t);
                sum += am3[i][2 * k + 1] * sc.sin() + am3[i][2 * (k + 1)] * sc.cos();
            }
            cm3[i] = am3[i][0] + sum;
        }
        return cm3;
    }

    /**
     * This method computes the F2 layer critical frequency.
     * @param modip modified DIP latitude, in degrees
     * @param cf2 Fourier time series for foF2
     * @param latitude latitude in radians
     * @param longitude longitude in radians
     * @return the F2 layer critical frequency, MHz
     */
    private double computefoF2(final double modip, final double[] cf2,
                               final double latitude, final double longitude) {

        // Legendre grades (Eq. 63)
        final int[] q = new int[] {
            12, 12, 9, 5, 2, 1, 1, 1, 1
        };

        // Array for geographic terms
        final double[] g = new double[cf2.length];
        g[0] = 1.0;

        // MODIP coefficients Eq. 57
        final double sinMODIP = FastMath.sin(FastMath.toRadians(modip));
        final double[] m = new double[12];
        m[0] = 1.0;
        for (int i = 1; i < q[0]; i++) {
            m[i] = sinMODIP * m[i - 1];
            g[i] = m[i];
        }

        // Latitude coefficients (Eq. 58)
        final double cosLat = FastMath.cos(latitude);
        final double[] p = new double[8];
        p[0] = cosLat;
        for (int n = 2; n < 9; n++) {
            p[n - 1] = cosLat * p[n - 2];
        }

        // latitude and longitude terms
        int index = 12;
        for (int i = 1; i < q.length; i++) {
            final SinCos sc = FastMath.sinCos(i * longitude);
            for (int j = 0; j < q[i]; j++) {
                g[index++] = m[j] * p[i - 1] * sc.cos();
                g[index++] = m[j] * p[i - 1] * sc.sin();
            }
        }

        // Compute foF2 by linear combination
        final double frequency = MathArrays.linearCombination(cf2, g);
        return frequency;
    }

    /**
     * This method computes the Maximum Usable Frequency factor.
     * @param modip modified DIP latitude, in degrees
     * @param cm3 Fourier time series for M(3000)F2
     * @param latitude latitude in radians
     * @param longitude longitude in radians
     * @return the Maximum Usable Frequency factor
     */
    private double computeMF2(final double modip, final double[] cm3,
                              final double latitude, final double longitude) {

        // Legendre grades (Eq. 71)
        final int[] r = new int[] {
            7, 8, 6, 3, 2, 1, 1
        };

        // Array for geographic terms
        final double[] g = new double[cm3.length];
        g[0] = 1.0;

        // MODIP coefficients Eq. 57
        final double sinMODIP = FastMath.sin(FastMath.toRadians(modip));
        final double[] m = new double[12];
        m[0] = 1.0;
        for (int i = 1; i < 12; i++) {
            m[i] = sinMODIP * m[i - 1];
            if (i < 7) {
                g[i] = m[i];
            }
        }

        // Latitude coefficients (Eq. 58)
        final double cosLat = FastMath.cos(latitude);
        final double[] p = new double[8];
        p[0] = cosLat;
        for (int n = 2; n < 9; n++) {
            p[n - 1] = cosLat * p[n - 2];
        }

        // latitude and longitude terms
        int index = 7;
        for (int i = 1; i < r.length; i++) {
            final SinCos sc = FastMath.sinCos(i * longitude);
            for (int j = 0; j < r[i]; j++) {
                g[index++] = m[j] * p[i - 1] * sc.cos();
                g[index++] = m[j] * p[i - 1] * sc.sin();
            }
        }

        // Compute m3000 by linear combination
        final double m3000 = MathArrays.linearCombination(g, cm3);
        return m3000;
    }

    /**
     * This method computes the F1 layer critical frequency.
     * <p>
     * This computation performs the algorithm exposed in Annex F
     * of the reference document.
     * </p>
     * @param foE the E layer critical frequency, MHz
     * @return the F1 layer critical frequency, MHz
     * @param foF2 the F2 layer critical frequency, MHz
     */
    private double computefoF1(final double foE, final double foF2) {
        final double temp  = join(1.4 * foE, 0.0, 1000.0, foE - 2.0);
        final double temp2 = join(0.0, temp, 1000.0, foE - temp);
        final double value = join(temp2, 0.85 * temp2, 60.0, 0.85 * foF2 - temp2);
        if (value < 1.0E-6) {
            return 0.0;
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
     * @param nmE E layer maximum density in 10^11 m-3
     * @param nmF1 F1 layer maximum density in 10^11 m-3
     * @param foF1 F1 layer critical frequency in MHz
     * @return a three components array containing the layer amplitudes
     */
    private double[] computeLayerAmplitudes(final double nmE, final double nmF1, final double foF1) {
        // Initialize array
        final double[] amplitude = new double[3];

        // F2 layer amplitude (Eq. 90)
        final double a1 = 4.0 * nmF2;
        amplitude[0]   = a1;

        // F1 and E layer amplitudes (Eq. 91 to 98)
        if (foF1 < 0.5) {
            amplitude[1] = 0.0;
            amplitude[2] = 4.0 * (nmE - epst(a1, hmF2, b2Bot, hmE));
        } else {
            double a2a = 0.0;
            double a3a = 4.0 * nmE;
            for (int i = 0; i < 5; i++) {
                a2a = 4.0 * (nmF1 - epst(a1, hmF2, b2Bot, hmF1) - epst(a3a, hmE, beTop, hmF1));
                a2a = join(a2a, 0.8 * nmF1, 1.0, a2a - 0.8 * nmF1);
                a3a = 4.0 * (nmE - epst(a2a, hmF1, b1Bot, hmE) - epst(a1, hmF2, b2Bot, hmE));
            }
            amplitude[1] = a2a;
            amplitude[2] = join(a3a, 0.05, 60.0, a3a - 0.005);
        }

        return amplitude;
    }

    /**
     * This method computes the topside thickness parameter H0.
     *
     * @param month current month
     * @param azr effective sunspot number
     * @return H0 in km
     */
    private double computeH0(final int month, final double azr) {

        // Auxiliary parameter ka (Eq. 99 and 100)
        final double ka;
        if (month > 3 && month < 10) {
            // month = 4,5,6,7,8,9
            ka = 6.705 - 0.014 * azr - 0.008 * hmF2;
        } else {
            // month = 1,2,3,10,11,12
            final double ratio = hmF2 / b2Bot;
            ka = -7.77 + 0.097 * ratio * ratio + 0.153 * nmF2;
        }

        // Auxiliary parameter kb (Eq. 101 and 102)
        double kb = join(ka, 2.0, 1.0, ka - 2.0);
        kb = join(8.0, kb, 1.0, kb - 8.0);

        // Auxiliary parameter Ha (Eq. 103)
        final double hA = kb * b2Bot;

        // Auxiliary parameters x and v (Eq. 104 and 105)
        final double x = 0.01 * (hA - 150.0);
        final double v = (0.041163 * x - 0.183981) * x + 1.424472;

        // Topside thickness parameter (Eq. 106)
        final double h = hA / v;
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
    private double clipExp(final double power) {
        if (power > 80.0) {
            return 5.5406E34;
        } else if (power < -80) {
            return 1.8049E-35;
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
    private double interpolate(final double z1, final double z2,
                               final double z3, final double z4,
                               final double x) {

        if (FastMath.abs(2.0 * x) < 1e-10) {
            return z2;
        }

        final double delta = 2.0 * x - 1.0;
        final double g1 = z3 + z2;
        final double g2 = z3 - z2;
        final double g3 = z4 + z1;
        final double g4 = (z4 - z1) / 3.0;
        final double a0 = 9.0 * g1 - g3;
        final double a1 = 9.0 * g2 - g4;
        final double a2 = g3 - g1;
        final double a3 = g4 - g2;
        final double zx = 0.0625 * (a0 + delta * (a1 + delta * (a2 + delta * a3)));

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
    private double join(final double dF1, final double dF2,
                        final double dA, final double dX) {
        final double ee = clipExp(dA * dX);
        return (dF1 * ee + dF2) / (ee + 1.0);
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
    private double epst(final double x, final double y,
                        final double z, final double w) {
        final double ex  = clipExp((w - y) / z);
        final double opex = 1.0 + ex;
        final double epst = x * ex / (opex * opex);
        return epst;
    }

}
