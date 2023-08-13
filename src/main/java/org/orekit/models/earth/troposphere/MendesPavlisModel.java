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
package org.orekit.models.earth.troposphere;

import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** The Mendes - Pavlis tropospheric delay model for optical techniques.
* It is valid for a wide range of wavelengths from 0.355µm to 1.064µm (Mendes and Pavlis, 2003)
*
* @see "Mendes, V. B., & Pavlis, E. C. (2004). High‐accuracy zenith delay prediction at
*      optical wavelengths. Geophysical Research Letters, 31(14)."
*
* @see "Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
*      IERS Technical Note No. 36, BKG (2010)"
*
* @author Bryan Cazabonne
*/
public class MendesPavlisModel implements DiscreteTroposphericModel, MappingFunction {

    /** Coefficients for the dispertion equation for the hydrostatic component [µm<sup>-2</sup>]. */
    private static final double[] K_COEFFICIENTS = {
        238.0185, 19990.975, 57.362, 579.55174
    };

    /** Coefficients for the dispertion equation for the non-hydrostatic component. */
    private static final double[] W_COEFFICIENTS = {
        295.235, 2.6422, -0.032380, 0.004028
    };

    /** Coefficients for the mapping function. */
    private static final double[][] A_COEFFICIENTS = {
        {12100.8e-7, 1729.5e-9, 319.1e-7, -1847.8e-11},
        {30496.5e-7, 234.4e-8, -103.5e-6, -185.6e-10},
        {6877.7e-5, 197.2e-7, -345.8e-5, 106.0e-9}
    };

    /** Carbon dioxyde content (IAG recommendations). */
    private static final double C02 = 0.99995995;

    /** Laser wavelength [µm]. */
    private double lambda;

    /** The atmospheric pressure [hPa]. */
    private double P0;

    /** The temperature at the station [K]. */
    private double T0;

    /** Water vapor pressure at the laser site [hPa]. */
    private double e0;

    /** Create a new Mendes-Pavlis model for the troposphere.
     * This initialisation will compute the water vapor pressure
     * thanks to the values of the pressure, the temperature and the humidity
     * @param t0 the temperature at the station, K
     * @param p0 the atmospheric pressure at the station, hPa
     * @param rh the humidity at the station, percent (50% → 0.5)
     * @param lambda laser wavelength, µm
     * */
    public MendesPavlisModel(final double t0, final double p0,
                             final double rh, final double lambda) {
        this.P0     = p0;
        this.T0     = t0;
        this.e0     = getWaterVapor(rh);
        this.lambda = lambda;
    }

    /** Create a new Mendes-Pavlis model using a standard atmosphere model.
    *
    * <ul>
    * <li>temperature: 18 degree Celsius
    * <li>pressure: 1013.25 hPa
    * <li>humidity: 50%
    * </ul>
    *
    * @param lambda laser wavelength, µm
    *
    * @return a Mendes-Pavlis model with standard environmental values
    */
    public static MendesPavlisModel getStandardModel(final double lambda) {
        return new MendesPavlisModel(273.15 + 18, 1013.25, 0.5, lambda);
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final GeodeticPoint point,
                            final double[] parameters, final AbsoluteDate date) {
        // Zenith delay
        final double[] zenithDelay = computeZenithDelay(point, parameters, date);
        // Mapping function
        final double[] mappingFunction = mappingFactors(elevation, point, date);
        // Tropospheric path delay
        return zenithDelay[0] * mappingFunction[0] + zenithDelay[1] * mappingFunction[1];
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation, final FieldGeodeticPoint<T> point,
                                                       final T[] parameters, final FieldAbsoluteDate<T> date) {
        // Zenith delay
        final T[] delays = computeZenithDelay(point, parameters, date);
        // Mapping function
        final T[] mappingFunction = mappingFactors(elevation, point, date);
        // Tropospheric path delay
        return delays[0].multiply(mappingFunction[0]).add(delays[1].multiply(mappingFunction[1]));
    }

    /** This method allows the  computation of the zenith hydrostatic and
     * zenith wet delay. The resulting element is an array having the following form:
     * <ul>
     * <li>double[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>double[1] = D<sub>wz</sub> → zenith wet delay
     * </ul>
     * @param point station location
     * @param parameters tropospheric model parameters
     * @param date current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    public double[] computeZenithDelay(final GeodeticPoint point, final double[] parameters, final AbsoluteDate date) {
        final double fsite   = getSiteFunctionValue(point);

        // Array for zenith delay
        final double[] delay = new double[2];

        // Dispertion Equation for the Hydrostatic component
        final double sigma  = 1 / lambda;
        final double sigma2 = sigma * sigma;
        final double coef1  = K_COEFFICIENTS[0] + sigma2;
        final double coef2  = K_COEFFICIENTS[0] - sigma2;
        final double coef3  = K_COEFFICIENTS[2] + sigma2;
        final double coef4  = K_COEFFICIENTS[2] - sigma2;

        final double frac1 = coef1 / (coef2 * coef2);
        final double frac2 = coef3 / (coef4 * coef4);

        final double fLambdaH = 0.01 * (K_COEFFICIENTS[1] * frac1 + K_COEFFICIENTS[3] * frac2) * C02;

        // Zenith delay for the hydrostatic component
        delay[0] = 0.002416579 * (fLambdaH / fsite) * P0;

        // Dispertion Equation for the Non-Hydrostatic component
        final double sigma4 = sigma2 * sigma2;
        final double sigma6 = sigma4 * sigma2;
        final double w1s2  = 3 * W_COEFFICIENTS[1] * sigma2;
        final double w2s4  = 5 * W_COEFFICIENTS[2] * sigma4;
        final double w3s6  = 7 * W_COEFFICIENTS[3] * sigma6;

        final double fLambdaNH = 0.003101 * (W_COEFFICIENTS[0] + w1s2 + w2s4 + w3s6);

        // Zenith delay for the non-hydrostatic component
        delay[1] = 0.0001 * (5.316 * fLambdaNH - 3.759 * fLambdaH) * (e0 / fsite);

        return delay;
    }

    /** This method allows the  computation of the zenith hydrostatic and
     * zenith wet delay. The resulting element is an array having the following form:
     * <ul>
     * <li>T[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>T[1] = D<sub>wz</sub> → zenith wet delay
     * </ul>
     * @param <T> type of the elements
     * @param point station location
     * @param parameters tropospheric model parameters
     * @param date current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    public <T extends CalculusFieldElement<T>> T[] computeZenithDelay(final FieldGeodeticPoint<T> point, final T[] parameters,
                                                                  final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final T zero = field.getZero();

        final T fsite   = getSiteFunctionValue(point);

        // Array for zenith delay
        final T[] delay = MathArrays.buildArray(field, 2);

        // Dispertion Equation for the Hydrostatic component
        final T sigma  = zero.add(1 / lambda);
        final T sigma2 = sigma.multiply(sigma);
        final T coef1  = sigma2.add(K_COEFFICIENTS[0]);
        final T coef2  = sigma2.negate().add(K_COEFFICIENTS[0]);
        final T coef3  = sigma2.add(K_COEFFICIENTS[2]);
        final T coef4  = sigma2.negate().add(K_COEFFICIENTS[2]);

        final T frac1 = coef1.divide(coef2.multiply(coef2));
        final T frac2 = coef3.divide(coef4.multiply(coef4));

        final T fLambdaH = frac1.multiply(K_COEFFICIENTS[1]).add(frac2.multiply(K_COEFFICIENTS[3])).multiply(0.01 * C02);

        // Zenith delay for the hydrostatic component
        delay[0] =  fLambdaH.divide(fsite).multiply(P0).multiply(0.002416579);

        // Dispertion Equation for the Non-Hydrostatic component
        final T sigma4 = sigma2.multiply(sigma2);
        final T sigma6 = sigma4.multiply(sigma2);
        final T w1s2   = sigma2.multiply(3 * W_COEFFICIENTS[1]);
        final T w2s4   = sigma4.multiply(5 * W_COEFFICIENTS[2]);
        final T w3s6   = sigma6.multiply(7 * W_COEFFICIENTS[3]);

        final T fLambdaNH = w1s2.add(w2s4).add(w3s6).add(W_COEFFICIENTS[0]).multiply(0.003101);

        // Zenith delay for the non-hydrostatic component
        delay[1] = fLambdaNH.multiply(5.316).subtract(fLambdaH.multiply(3.759)).multiply(fsite.divide(e0).reciprocal()).multiply(0.0001);

        return delay;
    }

    /** With the Mendes Pavlis tropospheric model, the mapping
     * function is not split into hydrostatic and wet component.
     * <p>
     * Therefore, the two components of the resulting array are equals.
     * <ul>
     * <li>double[0] = m(e) → total mapping function
     * <li>double[1] = m(e) → total mapping function
     * </ul>
     * <p>
     * The total delay will thus be computed as:<br>
     * δ = D<sub>hz</sub> * m(e) + D<sub>wz</sub> * m(e)<br>
     * δ = (D<sub>hz</sub> + D<sub>wz</sub>) * m(e) = δ<sub>z</sub> * m(e)
     */
    @Override
    public double[] mappingFactors(final double elevation, final GeodeticPoint point,
                                   final AbsoluteDate date) {
        final double sinE = FastMath.sin(elevation);

        final double T2degree = T0 - 273.15;

        // Mapping function coefficients
        final double a1 = computeMFCoeffient(A_COEFFICIENTS[0][0], A_COEFFICIENTS[0][1],
                                             A_COEFFICIENTS[0][2], A_COEFFICIENTS[0][3],
                                             T2degree, point);
        final double a2 = computeMFCoeffient(A_COEFFICIENTS[1][0], A_COEFFICIENTS[1][1],
                                             A_COEFFICIENTS[1][2], A_COEFFICIENTS[1][3],
                                             T2degree, point);
        final double a3 = computeMFCoeffient(A_COEFFICIENTS[2][0], A_COEFFICIENTS[2][1],
                                             A_COEFFICIENTS[2][2], A_COEFFICIENTS[2][3],
                                             T2degree, point);

        // Numerator
        final double numMP = 1 + a1 / (1 + a2 / (1 + a3));
        // Denominator
        final double denMP = sinE + a1 / (sinE + a2 / (sinE + a3));

        final double factor = numMP / denMP;

        return new double[] {
            factor,
            factor
        };
    }

    /** With the Mendes Pavlis tropospheric model, the mapping
     * function is not split into hydrostatic and wet component.
     * <p>
     * Therefore, the two components of the resulting array are equals.
     * <ul>
     * <li>double[0] = m(e) → total mapping function
     * <li>double[1] = m(e) → total mapping function
     * </ul>
     * <p>
     * The total delay will thus be computed as:<br>
     * δ = D<sub>hz</sub> * m(e) + D<sub>wz</sub> * m(e)<br>
     * δ = (D<sub>hz</sub> + D<sub>wz</sub>) * m(e) = δ<sub>z</sub> * m(e)
     */
    @Override
    public <T extends CalculusFieldElement<T>> T[] mappingFactors(final T elevation, final FieldGeodeticPoint<T> point,
                                                              final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();

        final T sinE = FastMath.sin(elevation);

        final double T2degree = T0 - 273.15;

        // Mapping function coefficients
        final T a1 = computeMFCoeffient(A_COEFFICIENTS[0][0], A_COEFFICIENTS[0][1],
                                        A_COEFFICIENTS[0][2], A_COEFFICIENTS[0][3],
                                        T2degree, point);
        final T a2 = computeMFCoeffient(A_COEFFICIENTS[1][0], A_COEFFICIENTS[1][1],
                                        A_COEFFICIENTS[1][2], A_COEFFICIENTS[1][3],
                                        T2degree, point);
        final T a3 = computeMFCoeffient(A_COEFFICIENTS[2][0], A_COEFFICIENTS[2][1],
                                        A_COEFFICIENTS[2][2], A_COEFFICIENTS[2][3],
                                        T2degree, point);

        // Numerator
        final T numMP = a1.divide(a2.divide(a3.add(1.0)).add(1.0)).add(1.0);
        // Denominator
        final T denMP = a1.divide(a2.divide(a3.add(sinE)).add(sinE)).add(sinE);

        final T factor = numMP.divide(denMP);

        final T[] mapping = MathArrays.buildArray(field, 2);
        mapping[0] = factor;
        mapping[1] = factor;

        return mapping;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** Get the laser frequency parameter f(lambda).
    *
    * @param point station location
    * @return the laser frequency parameter f(lambda).
    */
    private double getSiteFunctionValue(final GeodeticPoint point) {
        return 1. - 0.00266 * FastMath.cos(2. * point.getLatitude()) - 0.00000028 * point.getAltitude();
    }

    /** Get the laser frequency parameter f(lambda).
    *
    * @param <T> type of the elements
    * @param point station location
    * @return the laser frequency parameter f(lambda).
    */
    private <T extends CalculusFieldElement<T>> T getSiteFunctionValue(final FieldGeodeticPoint<T> point) {
        return FastMath.cos(point.getLatitude().multiply(2.)).multiply(0.00266).add(point.getAltitude().multiply(0.00000028)).negate().add(1.);
    }

    /** Compute the coefficients of the Mapping Function.
    *
    * @param T the temperature at the station site, °C
    * @param a0 first coefficient
    * @param a1 second coefficient
    * @param a2 third coefficient
    * @param a3 fourth coefficient
    * @param point station location
    * @return the value of the coefficient
    */
    private double computeMFCoeffient(final double a0, final double a1, final double a2, final double a3,
                                      final double T, final GeodeticPoint point) {
        return a0 + a1 * T + a2 * FastMath.cos(point.getLatitude()) + a3 * point.getAltitude();
    }

   /** Compute the coefficients of the Mapping Function.
   *
   * @param <T> type of the elements
   * @param T the temperature at the station site, °C
   * @param a0 first coefficient
   * @param a1 second coefficient
   * @param a2 third coefficient
   * @param a3 fourth coefficient
   * @param point station location
   * @return the value of the coefficient
   */
    private <T extends CalculusFieldElement<T>> T computeMFCoeffient(final double a0, final double a1, final double a2, final double a3,
                                                                 final double T, final FieldGeodeticPoint<T> point) {
        return point.getAltitude().multiply(a3).add(FastMath.cos(point.getLatitude()).multiply(a2)).add(a0 + a1 * T);
    }

    /** Get the water vapor.
     * The water vapor model is the one of Giacomo and Davis as indicated in IERS TN 32, chap. 9.
     *
     * See: Giacomo, P., Equation for the dertermination of the density of moist air, Metrologia, V. 18, 1982
     *
     * @param rh relative humidity, in percent (50% → 0.5).
     * @return the water vapor, in mbar (1 mbar = 1 hPa).
     */
    private double getWaterVapor(final double rh) {

        // saturation water vapor, equation (3) of reference paper, in mbar
        // with amended 1991 values (see reference paper)
        final double es = 0.01 * FastMath.exp((1.2378847 * 1e-5) * T0 * T0 -
                                              (1.9121316 * 1e-2) * T0 +
                                              33.93711047 -
                                              (6.3431645 * 1e3) * 1. / T0);

        // enhancement factor, equation (4) of reference paper
        final double fw = 1.00062 + (3.14 * 1e-6) * P0 + (5.6 * 1e-7) * FastMath.pow(T0 - 273.15, 2);

        final double e = rh * fw * es;
        return e;
    }
}
