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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.orekit.time.DateTimeComponents;

/**
 * Fourier time series for the NeQuick model.
 * @see NeQuickModel#computeFourierTimeSeries(DateTimeComponents, double)
 * @author Luc Maisonobe
 * @since 13.1
 * @param <T> type of the field elements
 */
public class FieldFourierTimeSeries<T extends CalculusFieldElement<T>> {

    /** Date. */
    private final DateTimeComponents dateTime;

    /** Effective ionisation level. */
    private final T az;

    /** Effective sunspot number (Eq. 19). */
    private final T azr;

    /** Fourier time series for foF2. */
    private final T[] cf2;

    /** Fourier time series for M(3000)F2. */
    private final T[] cm3;

    /**
     * Simple constructor.
     * @param dateTime   current date time components
     * @param az         effective ionisation level
     * @param flattenF2  F2 coefficients used by the F2 layer (flatten array)
     * @param flattenFm3 Fm3 coefficients used by the M(3000)F2 layer (flatten array)
     */
    FieldFourierTimeSeries(final DateTimeComponents dateTime, final T az,
                           final double[] flattenF2, final double[] flattenFm3) {

        this.dateTime = dateTime;
        this.az       = az;

        // Effective sunspot number (Eq. 19)
        this.azr = FastMath.sqrt(az.subtract(63.7).multiply(1123.6).add(167273.0)).subtract(408.99);

        // Hours
        final double hours = dateTime.getTime().getSecondsInUTCDay() / 3600.0;

        // Time argument (Eq. 49)
        final double t = FastMath.toRadians(15 * hours) - FastMath.PI;

        // Compute Fourier time series for foF2 and M(3000)F2
        final T[] scT = sinCos(az.newInstance(t), 6);
        this.cf2 = computeCF2(flattenF2, scT, azr);
        this.cm3 = computeCm3(flattenFm3, scT, azr);

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
    public T getAz() {
        return az;
    }

    /** Get effective sunspot number.
     * @return effective sunspot number
     */
    public T getAzr() {
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
    T[] getCf2Reference() {
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
    T[] getCm3Reference() {
        return cm3;
    }

    /** Computes cf2 coefficients.
     * @param flattenF2 F2 coefficients used by the F2 layer (flatten array)
     * @param scT       sines/cosines array of time argument
     * @param azr       effective sunspot number
     * @return the cf2 coefficients array
     */
    private T[] computeCF2(final double[] flattenF2, final T[] scT, final T azr) {

        // interpolation coefficients for effective spot number
        final T azr01 = azr.multiply(0.01);
        final T omazr01 = azr01.negate().add(1);

        // Eq. 44 and Eq. 50 merged into one loop
        final T[] cf2 = MathArrays.buildArray(azr.getField(), 76);
        int index = 0;
        for (int i = 0; i < cf2.length; i++) {
            // CHECKSTYLE: stop Indentation check
            cf2[i] = omazr01.multiply(flattenF2[index     ]).add(azr01.multiply(flattenF2[index +  1])).
                 add(omazr01.multiply(flattenF2[index +  2]).add(azr01.multiply(flattenF2[index +  3])).multiply(scT[ 0])).
                 add(omazr01.multiply(flattenF2[index +  4]).add(azr01.multiply(flattenF2[index +  5])).multiply(scT[ 1])).
                 add(omazr01.multiply(flattenF2[index +  6]).add(azr01.multiply(flattenF2[index +  7])).multiply(scT[ 2])).
                 add(omazr01.multiply(flattenF2[index +  8]).add(azr01.multiply(flattenF2[index +  9])).multiply(scT[ 3])).
                 add(omazr01.multiply(flattenF2[index + 10]).add(azr01.multiply(flattenF2[index + 11])).multiply(scT[ 4])).
                 add(omazr01.multiply(flattenF2[index + 12]).add(azr01.multiply(flattenF2[index + 13])).multiply(scT[ 5])).
                 add(omazr01.multiply(flattenF2[index + 14]).add(azr01.multiply(flattenF2[index + 15])).multiply(scT[ 6])).
                 add(omazr01.multiply(flattenF2[index + 16]).add(azr01.multiply(flattenF2[index + 17])).multiply(scT[ 7])).
                 add(omazr01.multiply(flattenF2[index + 18]).add(azr01.multiply(flattenF2[index + 19])).multiply(scT[ 8])).
                 add(omazr01.multiply(flattenF2[index + 20]).add(azr01.multiply(flattenF2[index + 21])).multiply(scT[ 9])).
                 add(omazr01.multiply(flattenF2[index + 22]).add(azr01.multiply(flattenF2[index + 23])).multiply(scT[10])).
                 add(omazr01.multiply(flattenF2[index + 24]).add(azr01.multiply(flattenF2[index + 25])).multiply(scT[11]));
            index += 26;
            // CHECKSTYLE: resume Indentation check
        }
        return cf2;
    }

    /** Computes Cm3 coefficients.
     * @param flattenFm3 Fm3 coefficients used by the M(3000)F2 layer (flatten array)
     * @param scT        sines/cosines array of time argument
     * @param azr        effective sunspot number
     * @return the Cm3 coefficients array
     */
    private T[] computeCm3(final double[] flattenFm3, final T[] scT, final T azr) {

        // interpolation coefficients for effective spot number
        final T azr01 = azr.multiply(0.01);
        final T omazr01 = azr01.negate().add(1);

        // Eq. 44 and Eq. 51 merged into one loop
        final T[] cm3 = MathArrays.buildArray(azr.getField(), 49);
        int index = 0;
        for (int i = 0; i < cm3.length; i++) {
            cm3[i] = omazr01.multiply(flattenFm3[index     ]).add(azr01.multiply(flattenFm3[index +  1])).
                 add(omazr01.multiply(flattenFm3[index +  2]).add(azr01.multiply(flattenFm3[index +  3])).multiply(scT[ 0])).
                 add(omazr01.multiply(flattenFm3[index +  4]).add(azr01.multiply(flattenFm3[index +  5])).multiply(scT[ 1])).
                 add(omazr01.multiply(flattenFm3[index +  6]).add(azr01.multiply(flattenFm3[index +  7])).multiply(scT[ 2])).
                 add(omazr01.multiply(flattenFm3[index +  8]).add(azr01.multiply(flattenFm3[index +  9])).multiply(scT[ 3])).
                 add(omazr01.multiply(flattenFm3[index + 10]).add(azr01.multiply(flattenFm3[index + 11])).multiply(scT[ 4])).
                 add(omazr01.multiply(flattenFm3[index + 12]).add(azr01.multiply(flattenFm3[index + 13])).multiply(scT[ 5])).
                 add(omazr01.multiply(flattenFm3[index + 14]).add(azr01.multiply(flattenFm3[index + 15])).multiply(scT[ 6])).
                 add(omazr01.multiply(flattenFm3[index + 16]).add(azr01.multiply(flattenFm3[index + 17])).multiply(scT[ 7]));
            index += 18;
        }
        return cm3;
    }

    /** Compute sines and cosines.
     * @param <T> type of the field elements
     * @param a argument
     * @param n number of terms
     * @return sin(a), cos(a), sin(2a), cos(2a) … sin(n a), cos(n a) array
     */
    static <T extends CalculusFieldElement<T>> T[] sinCos(final T a, final int n) {

        final FieldSinCos<T> sc0 = FastMath.sinCos(a);
        FieldSinCos<T>       sci = sc0;
        final T[] sc = MathArrays.buildArray(a.getField(), 2 * n);
        int isc = 0;
        sc[isc++] = sci.sin();
        sc[isc++] = sci.cos();
        for (int i = 1; i < n; i++) {
            sci = FieldSinCos.sum(sc0, sci);
            sc[isc++] = sci.sin();
            sc[isc++] = sci.cos();
        }

        return sc;

    }

}
