/* Copyright 2022-2025 Thales Alenia Space
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
import org.orekit.time.DateTimeComponents;

/**
 * Fourier time series for the NeQuick model.
 * @see NeQuickModel#computeFourierTimeSeries(DateTimeComponents, double)
 * @author Luc Maisonobe
 * @since 13.0.1
 */
public class FourierTimeSeries {

    /** Date. */
    private final DateTimeComponents dateTime;

    /** Effective ionisation level. */
    private final double az;

    /** Effective sunspot number (Eq. 19). */
    private final double azr;

    /** Fourier time series for foF2. */
    private final double[] cf2;

    /** Fourier time series for M(3000)F2. */
    private final double[] cm3;

    /**
     * Simple constructor.
     * @param dateTime   current date time components
     * @param az         effective ionisation level
     * @param flattenF2  F2 coefficients used by the F2 layer (flatten array)
     * @param flattenFm3 Fm3 coefficients used by the M(3000)F2 layer (flatten array)
     */
    FourierTimeSeries(final DateTimeComponents dateTime, final double az,
                      final double[] flattenF2, final double[] flattenFm3) {

        this.dateTime = dateTime;
        this.az       = az;

        // Effective sunspot number (Eq. 19)
        this.azr = FastMath.sqrt(167273.0 + (az - 63.7) * 1123.6) - 408.99;

        // Hours
        final double hours = dateTime.getTime().getSecondsInUTCDay() / 3600.0;

        // Time argument (Eq. 49)
        final double t = FastMath.toRadians(15 * hours) - FastMath.PI;

        // Compute Fourier time series for foF2 and M(3000)F2
        final double[] scT = sinCos(t, 6);
        this.cf2 = computeCF2(flattenF2, scT);
        this.cm3 = computeCm3(flattenFm3, scT);

    }

    /** Get date time components.
     * @return date time components
     */
    public DateTimeComponents getDateTime() {
        return dateTime;
    }

    /** Get effective ionisation level.
     * @return effective ionisation level
     */
    public double getAz() {
        return az;
    }

    /** Get effective sunspot number.
     * @return effective sunspot number
     */
    public double getAzr() {
        return azr;
    }

    /** Get Fourier time series for foF2.
     * <p>
     * Beware that for efficiency purposes, this method returns
     * a reference to an internal array; this is the reason why
     * this method visibility is limited to package level.
     * </p>
     * @return Fourier time series for foF2 (reference to an internal array)
     */
    double[] getCf2Reference() {
        return cf2;
    }

    /** Get Fourier time series for M(3000)F2.
     * <p>
     * Beware that for efficiency purposes, this method returns
     * a reference to an internal array; this is the reason why
     * this method visibility is limited to package level.
     * </p>
     * @return Fourier time series for M(3000)F2 (reference to an internal array)
     */
    double[] getCm3Reference() {
        return cm3;
    }

    /** Computes cf2 coefficients.
     * @param flattenF2 F2 coefficients used by the F2 layer (flatten array)
     * @param scT       sines/cosines array of time argument
     * @return the cf2 coefficients array
     */
    private double[] computeCF2(final double[] flattenF2, final double[] scT) {

        // interpolation coefficients for effective spot number
        final double azr01   = azr * 0.01;
        final double omazr01 = 1 - azr01;

        // Eq. 44 and Eq. 50 merged into one loop
        final double[] array   = new double[76];
        int            index = 0;
        for (int i = 0; i < array.length; i++) {
            array[i] =
                    omazr01 * flattenF2[index] + azr01 * flattenF2[index + 1] +
                   (omazr01 * flattenF2[index + 2] + azr01 * flattenF2[index + 3]) * scT[0] +
                   (omazr01 * flattenF2[index + 4] + azr01 * flattenF2[index + 5]) * scT[1] +
                   (omazr01 * flattenF2[index + 6] + azr01 * flattenF2[index + 7]) * scT[2] +
                   (omazr01 * flattenF2[index + 8] + azr01 * flattenF2[index + 9]) * scT[3] +
                   (omazr01 * flattenF2[index + 10] + azr01 * flattenF2[index + 11]) * scT[4] +
                   (omazr01 * flattenF2[index + 12] + azr01 * flattenF2[index + 13]) * scT[5] +
                   (omazr01 * flattenF2[index + 14] + azr01 * flattenF2[index + 15]) * scT[6] +
                   (omazr01 * flattenF2[index + 16] + azr01 * flattenF2[index + 17]) * scT[7] +
                   (omazr01 * flattenF2[index + 18] + azr01 * flattenF2[index + 19]) * scT[8] +
                   (omazr01 * flattenF2[index + 20] + azr01 * flattenF2[index + 21]) * scT[9] +
                   (omazr01 * flattenF2[index + 22] + azr01 * flattenF2[index + 23]) * scT[10] +
                   (omazr01 * flattenF2[index + 24] + azr01 * flattenF2[index + 25]) * scT[11];
            index += 26;
        }
        return array;
    }

    /** Computes Cm3 coefficients.
     * @param flattenFm3 Fm3 coefficients used by the M(3000)F2 layer (flatten array)
     * @param scT        sines/cosines array of time argument
     * @return the Cm3 coefficients array
     */
    private double[] computeCm3(final double[] flattenFm3, final double[] scT) {

        // interpolation coefficients for effective spot number
        final double azr01   = azr * 0.01;
        final double omazr01 = 1 - azr01;

        // Eq. 44 and Eq. 51 merged into one loop
        final double[] array   = new double[49];
        int            index = 0;
        for (int i = 0; i < array.length; i++) {
            array[i] =
                    omazr01 * flattenFm3[index] + azr01 * flattenFm3[index + 1] +
                   (omazr01 * flattenFm3[index + 2] + azr01 * flattenFm3[index + 3]) * scT[0] +
                   (omazr01 * flattenFm3[index + 4] + azr01 * flattenFm3[index + 5]) * scT[1] +
                   (omazr01 * flattenFm3[index + 6] + azr01 * flattenFm3[index + 7]) * scT[2] +
                   (omazr01 * flattenFm3[index + 8] + azr01 * flattenFm3[index + 9]) * scT[3] +
                   (omazr01 * flattenFm3[index + 10] + azr01 * flattenFm3[index + 11]) * scT[4] +
                   (omazr01 * flattenFm3[index + 12] + azr01 * flattenFm3[index + 13]) * scT[5] +
                   (omazr01 * flattenFm3[index + 14] + azr01 * flattenFm3[index + 15]) * scT[6] +
                   (omazr01 * flattenFm3[index + 16] + azr01 * flattenFm3[index + 17]) * scT[7];
            index += 18;
        }
        return array;
    }

    /** Compute sines and cosines.
     * @param a argument
     * @param n number of terms
     * @return sin(a), cos(a), sin(2a), cos(2a) … sin(n a), cos(n a) array
     */
    static double[] sinCos(final double a, final int n) {

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

}
