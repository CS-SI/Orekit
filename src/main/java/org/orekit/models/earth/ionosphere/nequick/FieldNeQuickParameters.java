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
import org.hipparchus.util.MathArrays;
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
 * @param <T> type of the field elements
 */
class FieldNeQuickParameters <T extends CalculusFieldElement<T>> {

    /** Solar zenith angle at day night transition, degrees. */
    private static final double X0 = 86.23292796211615;

    /** Current date time components.
     * @since 13.0
     */
    private final DateTimeComponents dateTime;

    /** Effective sunspot number.
     * @since 13.0
     */
    private final T azr;

    /** F2 layer critical frequency.
     * @since 13.0
     */
    private final T foF2;

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

    /** Layer amplitudes. */
    private final T[] amplitudes;

    /**
     * Build a new instance.
     * @param dateTime current date time components
     * @param flattenF2 F2 coefficients used by the F2 layer (flatten array)
     * @param flattenFm3 Fm3 coefficients used by the F2 layer (flatten array)
     * @param latitude latitude of a point along the integration path, in radians
     * @param longitude longitude of a point along the integration path, in radians
     * @param az effective ionisation level
     * @param modip modip
     */
    FieldNeQuickParameters(final DateTimeComponents dateTime, final double[] flattenF2,
                           final double[] flattenFm3, final T latitude, final T longitude, final T az,
                           final T modip) {
        this(new FieldFourierTimeSeries<>(dateTime, az, flattenF2, flattenFm3),
             latitude, longitude, modip);
    }

    /**
     * Build a new instance.
     * @param fourierTimeSeries Fourier time series for foF2 and M(3000)F2 layer
     * @param latitude latitude of a point along the integration path, in radians
     * @param longitude longitude of a point along the integration path, in radians
     * @param modip modip
     */
    FieldNeQuickParameters(final FieldFourierTimeSeries<T> fourierTimeSeries,
                           final T latitude, final T longitude, final T modip) {

        // Zero
        final T zero = latitude.getField().getZero();

        this.dateTime = fourierTimeSeries.getDateTime();
        this.azr      = fourierTimeSeries.getAzr();

        // Date and Time components
        final DateComponents date = dateTime.getDate();
        final TimeComponents time = dateTime.getTime();
        // Hours
        final double hours  = time.getSecondsInUTCDay() / 3600.0;
        // Effective solar zenith angle in radians
        final T xeff = computeEffectiveSolarAngle(date.getMonth(), hours, latitude, longitude);

        // E layer maximum density height in km (Eq. 78)
        this.hmE = zero.newInstance(120.0);
        // E layer critical frequency in MHz
        final T foE = computefoE(date.getMonth(), fourierTimeSeries.getAz(), xeff, latitude);
        // E layer maximum density in 10^11 m-3 (Eq. 36)
        final T nmE = foE.multiply(foE).multiply(0.124);

        // F2 layer critical frequency in MHz
        final T[] scL = FieldFourierTimeSeries.sinCos(longitude, 8);
        this.foF2 = computefoF2(modip, fourierTimeSeries.getCf2Reference(), latitude, scL);
        // Maximum Usable Frequency factor
        final T mF2  = computeMF2(modip, fourierTimeSeries.getCm3Reference(), latitude, scL);
        // F2 layer maximum density in 10^11 m-3
        this.nmF2 = foF2.multiply(foF2).multiply(0.124);
        // F2 layer maximum density height in km
        this.hmF2 = computehmF2(foE, mF2);

        // F1 layer critical frequency in MHz
        final T foF1 = computefoF1(foE);
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
        this.beTop = FastMath.max(b1Bot, zero.newInstance(7.0));
        this.beBot = zero.newInstance(5.0);

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
    public T getAzr() {
        return azr;
    }

    /**
     * Get F2 layer critical frequency.
     * @return F2 layer critical frequency
     * @since 13.0
     */
    public T getFoF2() {
        return foF2;
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
     * Get the F1 layer thickness parameter.
     * @param h current height (km)
     * @return B1 in km
     * @since 13.0
     */
    public T getBF1(final T h) {
        // Eq. 110
        return (h.getReal() > hmF1.getReal()) ? b1Top : b1Bot;
    }

    /**
     * Get the E layer thickness parameter.
     * @param h current height (km)
     * @return Be in km
     * @since 13.0
     */
    public T getBE(final T h) {
        // Eq. 109
        return (h.getReal() > hmE.getReal()) ? beTop : beBot;
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
        final T xeff = join(clipExp(x.multiply(0.2).negate().add(20.0)).multiply(0.24).negate().add(90.0), x,
                zero.newInstance(12.0), x.subtract(X0));
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
        return FastMath.sqrt(coef .multiply(coef).multiply(sqAz).multiply(FastMath.cos(xeff).pow(0.6)).add(0.49));

    }

    /**
     * Computes the F2 layer height of maximum electron density.
     * @param foE E layer layer critical frequency in MHz
     * @param mF2 maximum usable frequency factor
     * @return hmF2 in km
     */
    private T computehmF2(final T foE, final T mF2) {
        // Zero
        final T zero = foE.getField().getZero();
        // Ratio
        final T fo = foF2.divide(foE);
        final T ratio = join(fo, zero.newInstance(1.75), zero.newInstance(20.0), fo.subtract(1.75));

        // deltaM parameter
        T deltaM = zero.subtract(0.012);
        if (foE.getReal() >= 1e-30) {
            deltaM = deltaM.add(ratio.subtract(1.215).divide(0.253).reciprocal());
        }

        // hmF2 Eq. 80
        final T mF2Sq = mF2.square();
        final T temp  = FastMath.sqrt(mF2Sq.multiply(0.0196).add(1.0).divide(mF2Sq.multiply(1.2967).subtract(1.0)));
        return mF2.multiply(1490.0).multiply(temp).divide(mF2.add(deltaM)).subtract(176.0);

    }

    /**
     * This method computes the F2 layer critical frequency.
     * @param modip modified DIP latitude, in degrees
     * @param cf2 Fourier time series for foF2
     * @param latitude latitude in radians
     * @param scL sines/cosines array of longitude argument
     * @return the F2 layer critical frequency, MHz
     */
    private T computefoF2(final T modip, final T[] cf2,
                          final T latitude, final T[] scL) {

        // Legendre grades (Eq. 63)
        final int[] q = new int[] {
            12, 12, 9, 5, 2, 1, 1, 1, 1
        };

        T frequency = cf2[0];

        // ModipGrid coefficients Eq. 57
        final T sinMODIP = FastMath.sin(FastMath.toRadians(modip));
        final T[] m = MathArrays.buildArray(latitude.getField(), 12);
        m[0] = latitude.getField().getOne();
        for (int i = 1; i < q[0]; i++) {
            m[i] = sinMODIP.multiply(m[i - 1]);
            frequency = frequency.add(m[i].multiply(cf2[i]));
        }

        // latitude and longitude terms
        int index = 12;
        final T cosLat1 = FastMath.cos(latitude);
        T cosLatI = cosLat1;
        for (int i = 1; i < q.length; i++) {
            final T c = cosLatI.multiply(scL[2 * i - 1]);
            final T s = cosLatI.multiply(scL[2 * i - 2]);
            for (int j = 0; j < q[i]; j++) {
                frequency = frequency.add(m[j].multiply(c).multiply(cf2[index++]));
                frequency = frequency.add(m[j].multiply(s).multiply(cf2[index++]));
            }
            cosLatI = cosLatI.multiply(cosLat1);
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
    private T computeMF2(final T modip, final T[] cm3,
                         final T latitude, final T[] scL) {

        // Legendre grades (Eq. 71)
        final int[] r = new int[] {
            7, 8, 6, 3, 2, 1, 1
        };

        T m3000 = cm3[0];

        // ModipGrid coefficients Eq. 57
        final T sinMODIP = FastMath.sin(FastMath.toRadians(modip));
        final T[] m = MathArrays.buildArray(latitude.getField(), 12);
        m[0] = latitude.getField().getOne();
        for (int i = 1; i < 12; i++) {
            m[i] = sinMODIP.multiply(m[i - 1]);
            if (i < 7) {
                m3000 = m3000.add(m[i].multiply(cm3[i]));
            }
        }

        // latitude and longitude terms
        int index = 7;
        final T cosLat1 = FastMath.cos(latitude);
        T cosLatI = cosLat1;
        for (int i = 1; i < r.length; i++) {
            final T c = cosLatI.multiply(scL[2 * i - 1]);
            final T s = cosLatI.multiply(scL[2 * i - 2]);
            for (int j = 0; j < r[i]; j++) {
                m3000 = m3000.add(m[j].multiply(c).multiply(cm3[index++]));
                m3000 = m3000.add(m[j].multiply(s).multiply(cm3[index++]));
            }
            cosLatI = cosLatI.multiply(cosLat1); // Eq. 58
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
     */
    private T computefoF1(final T foE) {
        final T zero = foE.getField().getZero();
        final T temp  = join(foE.multiply(1.4), zero, zero.newInstance(1000.0), foE.subtract(2.0));
        final T temp2 = join(zero, temp, zero.newInstance(1000.0), foE.subtract(temp));
        final T value = join(temp2, temp2.multiply(0.85), zero.newInstance(60.0), foF2.multiply(0.85).subtract(temp2));
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
     * @param nmE E layer maximum density in 10^11 m-3
     * @param nmF1 F1 layer maximum density in 10^11 m-3
     * @param foF1 F1 layer critical frequency in MHz
     * @return a three components array containing the layer amplitudes
     */
    private T[] computeLayerAmplitudes(final T nmE, final T nmF1, final T foF1) {
        // Zero
        final T zero = nmE.getField().getZero();

        // Initialize array
        final T[] amplitude = MathArrays.buildArray(nmE.getField(), 3);

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
                a2a = join(a2a, nmF1.multiply(0.8), nmE.getField().getOne(), a2a.subtract(nmF1.multiply(0.8)));
                a3a = nmE.subtract(epst(a2a, hmF1, b1Bot, hmE)).subtract(epst(a1, hmF2, b2Bot, hmE)).multiply(4.0);
            }
            amplitude[1] = a2a;
            amplitude[2] = join(a3a, zero.newInstance(0.05), zero.newInstance(60.0), a3a.subtract(0.005));
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
    private T clipExp(final T power) {
        final T zero = power.getField().getZero();
        if (power.getReal() > 80.0) {
            return zero.newInstance(5.5406E34);
        } else if (power.getReal() < -80) {
            return zero.newInstance(1.8049E-35);
        } else {
            return FastMath.exp(power);
        }
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
    T join(final T dF1, final T dF2, final T dA, final T dX) {
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
        return x.multiply(ex).divide(opex.multiply(opex));

    }

}
