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
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;

/**
 * This class performs the computation of the parameters used by the NeQuick model.
 *
 * @author Bryan Cazabonne
 *
 * @see "European Union (2016). European GNSS (Galileo) Open Service-Ionospheric Correction
 *       Algorithm for Galileo Single Frequency Users. 1.2."
 * @see <a href="https://www.itu.int/rec/R-REC-P.531/en">ITU-R P.531</a>
 *
 * @since 10.1
 */
public class NeQuickParameters {

    /** Solar zenith angle at day night transition, degrees. */
    private static final double X0 = 86.23292796211615;

    /** Current date time components.
     * @since 13.0
     */
    private final DateTimeComponents dateTime;

    /** Effective sunspot number.
     * @since 13.0
     */
    private final double azr;

    /** F2 layer critical frequency.
     * @since 13.0
     */
    private final double foF2;

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

    /** Layer amplitudes. */
    private final double[] amplitudes;

    /**
     * Build a new instance.
     * @param dateTime current date time components
     * @param flattenF2 F2 coefficients used by the F2 layer (flatten array)
     * @param flattenFm3 Fm3 coefficients used by the M(3000)F2 layer (flatten array)
     * @param latitude latitude of a point along the integration path, in radians
     * @param longitude longitude of a point along the integration path, in radians
     * @param az effective ionisation level
     * @param modip modip
     */
    public NeQuickParameters(final DateTimeComponents dateTime, final double[] flattenF2, final double[] flattenFm3,
                             final double latitude, final double longitude, final double az, final double modip) {

        this.dateTime = dateTime;

        // Effective sunspot number (Eq. 19)
        azr = FastMath.sqrt(167273.0 + (az - 63.7) * 1123.6) - 408.99;

        // Date and Time components
        final DateComponents date = dateTime.getDate();
        final TimeComponents time = dateTime.getTime();
        // Hours
        final double hours  = time.getSecondsInUTCDay() / 3600.0;
        // Effective solar zenith angle in radians
        final double xeff = computeEffectiveSolarAngle(date.getMonth(), hours, latitude, longitude);

        // E layer maximum density height in km (Eq. 78)
        this.hmE = 120.0;
        // E layer critical frequency in MHz
        final double foE = computefoE(date.getMonth(), az, xeff, latitude);
        // E layer maximum density in 10^11 m⁻³ (Eq. 36)
        final double nmE = 0.124 * foE * foE;

        // Time argument (Eq. 49)
        final double t = FastMath.toRadians(15 * hours) - FastMath.PI;
        // Compute Fourier time series for foF2 and M(3000)F2
        final double[] scT = sinCos(t, 6);
        final double[] cf2 = computeCF2(flattenF2, azr, scT);
        final double[] cm3 = computeCm3(flattenFm3, azr, scT);
        // F2 layer critical frequency in MHz
        final double[] scL = sinCos(longitude, 8);
        this.foF2 = computefoF2(modip, cf2, latitude, scL);
        // Maximum Usable Frequency factor
        final double mF2  = computeMF2(modip, cm3, latitude, scL);
        // F2 layer maximum density in 10^11 m⁻³
        this.nmF2 = 0.124 * foF2 * foF2;
        // F2 layer maximum density height in km
        this.hmF2 = computehmF2(foE, foF2, mF2);

        // F1 layer critical frequency in MHz
        final double foF1 = computefoF1(foE, foF2);
        // F1 layer maximum density in 10^11 m⁻³
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

    }

    /**
     * Get current date time components.
     * @return current date time components
     * @since 13.0
     */
    public DateTimeComponents getDateTime() {
        return dateTime;
    }

    /**
     * Get effective sunspot number.
     * @return effective sunspot number
     * @since 13.0
     */
    public double getAzr() {
        return azr;
    }

    /**
     * Get F2 layer critical frequency.
     * @return F2 layer critical frequency
     * @since 13.0
     */
    public double getFoF2() {
        return foF2;
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
        return FastMath.sqrt(coef * coef * sqAz * FastMath.pow(FastMath.cos(xeff), 0.6) + 0.49);

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
        return ((1490.0 * mF2 * temp) / (mF2 + deltaM)) - 176.0;

    }

    /** Compute sines and cosines.
     * @param a argument
     * @param n number of terms
     * @return sin(a), cos(a), sin(2a), cos(2a) … sin(n a), cos(n a) array
     * @since 12.1.3
     */
    private double[] sinCos(final double a, final int n) {

        final SinCos sc0 = FastMath.sinCos(a);
        SinCos sci = sc0;
        final double[] sc = new double[2 * n];
        int isc = 0;
        sc[isc++] = sci.sin();
        sc[isc++] = sci.cos();
        for (int i = 1; i < n; i++) {
            sci = SinCos.sum(sc0, sci);
            sc[isc++] = sci.sin();
            sc[isc++] = sci.cos();
        }

        return sc;

    }

    /**
     * Computes cf2 coefficients.
     * @param flattenF2 F2 coefficients used by the F2 layer (flatten array)
     * @param azr effective sunspot number (Eq. 19)
     * @param scT sines/cosines array of time argument
     * @return the cf2 coefficients array
     */
    private double[] computeCF2(final double[] flattenF2, final double azr, final double[] scT) {

        // interpolation coefficients for effective spot number
        final double azr01 = azr * 0.01;
        final double omazr01 = 1 - azr01;

        // Eq. 44 and Eq. 50 merged into one loop
        final double[] cf2 = new double[76];
        int index = 0;
        for (int i = 0; i < cf2.length; i++) {
            cf2[i] = omazr01 * flattenF2[index     ] + azr01 * flattenF2[index +  1] +
                    (omazr01 * flattenF2[index +  2] + azr01 * flattenF2[index +  3]) * scT[ 0] +
                    (omazr01 * flattenF2[index +  4] + azr01 * flattenF2[index +  5]) * scT[ 1] +
                    (omazr01 * flattenF2[index +  6] + azr01 * flattenF2[index +  7]) * scT[ 2] +
                    (omazr01 * flattenF2[index +  8] + azr01 * flattenF2[index +  9]) * scT[ 3] +
                    (omazr01 * flattenF2[index + 10] + azr01 * flattenF2[index + 11]) * scT[ 4] +
                    (omazr01 * flattenF2[index + 12] + azr01 * flattenF2[index + 13]) * scT[ 5] +
                    (omazr01 * flattenF2[index + 14] + azr01 * flattenF2[index + 15]) * scT[ 6] +
                    (omazr01 * flattenF2[index + 16] + azr01 * flattenF2[index + 17]) * scT[ 7] +
                    (omazr01 * flattenF2[index + 18] + azr01 * flattenF2[index + 19]) * scT[ 8] +
                    (omazr01 * flattenF2[index + 20] + azr01 * flattenF2[index + 21]) * scT[ 9] +
                    (omazr01 * flattenF2[index + 22] + azr01 * flattenF2[index + 23]) * scT[10] +
                    (omazr01 * flattenF2[index + 24] + azr01 * flattenF2[index + 25]) * scT[11];
            index += 26;
        }
        return cf2;
    }

    /**
     * Computes Cm3 coefficients.
     * @param flattenFm3 Fm3 coefficients used by the M(3000)F2 layer (flatten array)
     * @param azr effective sunspot number (Eq. 19)
     * @param scT sines/cosines array of time argument
     * @return the Cm3 coefficients array
     */
    private double[] computeCm3(final double[] flattenFm3, final double azr, final double[] scT) {

        // interpolation coefficients for effective spot number
        final double azr01 = azr * 0.01;
        final double omazr01 = 1 - azr01;

        // Eq. 44 and Eq. 51 merged into one loop
        final double[] cm3 = new double[49];
        int index = 0;
        for (int i = 0; i < cm3.length; i++) {
            cm3[i] = omazr01 * flattenFm3[index     ] + azr01 * flattenFm3[index +  1] +
                    (omazr01 * flattenFm3[index +  2] + azr01 * flattenFm3[index +  3]) * scT[ 0] +
                    (omazr01 * flattenFm3[index +  4] + azr01 * flattenFm3[index +  5]) * scT[ 1] +
                    (omazr01 * flattenFm3[index +  6] + azr01 * flattenFm3[index +  7]) * scT[ 2] +
                    (omazr01 * flattenFm3[index +  8] + azr01 * flattenFm3[index +  9]) * scT[ 3] +
                    (omazr01 * flattenFm3[index + 10] + azr01 * flattenFm3[index + 11]) * scT[ 4] +
                    (omazr01 * flattenFm3[index + 12] + azr01 * flattenFm3[index + 13]) * scT[ 5] +
                    (omazr01 * flattenFm3[index + 14] + azr01 * flattenFm3[index + 15]) * scT[ 6] +
                    (omazr01 * flattenFm3[index + 16] + azr01 * flattenFm3[index + 17]) * scT[ 7];
            index += 18;
        }
        return cm3;
    }

    /**
     * This method computes the F2 layer critical frequency.
     * @param modip modified DIP latitude, in degrees
     * @param cf2 Fourier time series for foF2
     * @param latitude latitude in radians
     * @param scL sines/cosines array of longitude argument
     * @return the F2 layer critical frequency, MHz
     */
    private double computefoF2(final double modip, final double[] cf2,
                               final double latitude, final double[] scL) {

        // Legendre grades (Eq. 63)
        final int[] q = new int[] {
            12, 12, 9, 5, 2, 1, 1, 1, 1
        };

        double frequency = cf2[0];

        // ModipGrid coefficients Eq. 57
        final double sinMODIP = FastMath.sin(FastMath.toRadians(modip));
        final double[] m = new double[12];
        m[0] = 1.0;
        for (int i = 1; i < q[0]; i++) {
            m[i] = sinMODIP * m[i - 1];
            frequency += m[i] * cf2[i];
        }

        // latitude and longitude terms
        int index = 12;
        final double cosLat1 = FastMath.cos(latitude);
        double cosLatI = cosLat1;
        for (int i = 1; i < q.length; i++) {
            final double c = cosLatI * scL[2 * i - 1];
            final double s = cosLatI * scL[2 * i - 2];
            for (int j = 0; j < q[i]; j++) {
                frequency += m[j] * c * cf2[index++];
                frequency += m[j] * s * cf2[index++];
            }
            cosLatI *= cosLat1; // Eq. 58
        }

        return frequency;

    }

    /**
     * This method computes the Maximum Usable Frequency factor.
     * @param modip modified DIP latitude, in degrees
     * @param cm3 Fourier time series for M(3000)F2
     * @param latitude latitude in radians
     * @param scL sines/cosines array of longitude argument
     * @return the Maximum Usable Frequency factor
     */
    private double computeMF2(final double modip, final double[] cm3,
                              final double latitude, final double[] scL) {

        // Legendre grades (Eq. 71)
        final int[] r = new int[] {
            7, 8, 6, 3, 2, 1, 1
        };

        double m3000 = cm3[0];

        // ModipGrid coefficients Eq. 57
        final double sinMODIP = FastMath.sin(FastMath.toRadians(modip));
        final double[] m = new double[12];
        m[0] = 1.0;
        for (int i = 1; i < 12; i++) {
            m[i] = sinMODIP * m[i - 1];
            if (i < 7) {
                m3000 += m[i] * cm3[i];
            }
        }

        // latitude and longitude terms
        int index = 7;
        final double cosLat1 = FastMath.cos(latitude);
        double cosLatI = cosLat1;
        for (int i = 1; i < r.length; i++) {
            final double c = cosLatI * scL[2 * i - 1];
            final double s = cosLatI * scL[2 * i - 2];
            for (int j = 0; j < r[i]; j++) {
                m3000 += m[j] * c * cm3[index++];
                m3000 += m[j] * s * cm3[index++];
            }
            cosLatI *= cosLat1; // Eq. 58
        }

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
     * @param nmE E layer maximum density in 10^11 m⁻³
     * @param nmF1 F1 layer maximum density in 10^11 m⁻³
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
     * Allows smooth joining of functions f1 and f2
     * (i.e. continuous first derivatives) at origin.
     * <p>
     * This function, describe in section F.2.12.1 of the reference document, is
     * recommended for computational efficiency.
     * </p>
     * @param dF1 first function
     * @param dF2 second function
     * @param dA width of transition region
     * @param dX x value
     * @return the computed value
     */
    double join(final double dF1, final double dF2, final double dA, final double dX) {
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
        return x * ex / (opex * opex);

    }

}
