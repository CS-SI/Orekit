/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.ConstantPressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.water.CIPM2007;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;
import org.orekit.utils.units.Unit;
import org.orekit.utils.units.UnitsConverter;

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
public class MendesPavlisModel implements TroposphericModel, TroposphereMappingFunction {

    /** Coefficients for the dispersion equation for the hydrostatic component [µm<sup>-2</sup>]. */
    private static final double[] K_COEFFICIENTS = {
        238.0185, 19990.975, 57.362, 579.55174
    };

    /** Coefficients for the dispersion equation for the non-hydrostatic component. */
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

    /** Dispersion equation for the hydrostatic component. */
    private final double fLambdaH;

    /** Dispersion equation for the non-hydrostatic component. */
    private final double fLambdaNH;

    /** Provider for pressure, temperature and humidity. */
    private final PressureTemperatureHumidityProvider pthProvider;

    /** Create a new Mendes-Pavlis model for the troposphere.
     * @param pthProvider provider for atmospheric pressure, temperature and humidity at the station
     * @param lambda laser wavelength
     * @param lambdaUnits units in which {@code lambda} is given
     * @see TroposphericModelUtils#MICRO_M
     * @see TroposphericModelUtils#NANO_M
     * @since 12.1
     * */
    public MendesPavlisModel(final PressureTemperatureHumidityProvider pthProvider,
                             final double lambda, final Unit lambdaUnits) {
        this.pthProvider = pthProvider;

        // Dispersion equation for the hydrostatic component
        final double lambdaMicrometer = new UnitsConverter(lambdaUnits, TroposphericModelUtils.MICRO_M).convert(lambda);
        final double sigma  = 1.0 / lambdaMicrometer;
        final double sigma2 = sigma * sigma;
        final double coef1  = K_COEFFICIENTS[0] + sigma2;
        final double coef2  = K_COEFFICIENTS[0] - sigma2;
        final double coef3  = K_COEFFICIENTS[2] + sigma2;
        final double coef4  = K_COEFFICIENTS[2] - sigma2;
        final double frac1 = coef1 / (coef2 * coef2);
        final double frac2 = coef3 / (coef4 * coef4);
        fLambdaH = 0.01 * (K_COEFFICIENTS[1] * frac1 + K_COEFFICIENTS[3] * frac2) * C02;

        // Dispersion equation for the non-hydrostatic component
        final double sigma4 = sigma2 * sigma2;
        final double sigma6 = sigma4 * sigma2;
        final double w1s2  = 3 * W_COEFFICIENTS[1] * sigma2;
        final double w2s4  = 5 * W_COEFFICIENTS[2] * sigma4;
        final double w3s6  = 7 * W_COEFFICIENTS[3] * sigma6;

        fLambdaNH = 0.003101 * (W_COEFFICIENTS[0] + w1s2 + w2s4 + w3s6);

    }

    /** Create a new Mendes-Pavlis model using a standard atmosphere model.
     *
     * <ul>
     * <li>altitude: 0m</li>
     * <li>temperature: 18 degree Celsius</li>
     * <li>pressure: 1013.25 hPa</li>
     * <li>humidity: 50%</li>
     * </ul>
     *
     * @param lambda laser wavelength, µm
     * @param lambdaUnits units in which {@code lambda} is given
     * @return a Mendes-Pavlis model with standard environmental values
     * @see TroposphericModelUtils#MICRO_M
     * @see TroposphericModelUtils#NANO_M
     * @since 12.1
     */
    public static MendesPavlisModel getStandardModel(final double lambda, final Unit lambdaUnits) {
        final double h  = 0;
        final double p  = TroposphericModelUtils.HECTO_PASCAL.toSI(1013.25);
        final double t  = 273.15 + 18;
        final double rh = 0.5;
        final PressureTemperatureHumidity pth = new PressureTemperatureHumidity(h, p, t,
                                                                                new CIPM2007().waterVaporPressure(p, t, rh),
                                                                                Double.NaN,
                                                                                Double.NaN);
        return new MendesPavlisModel(new ConstantPressureTemperatureHumidityProvider(pth),
                                     lambda, lambdaUnits);
    }

    /** {@inheritDoc} */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates,
                                       final GeodeticPoint point,
                                       final PressureTemperatureHumidity weather,
                                       final double[] parameters, final AbsoluteDate date) {
        // Zenith delay
        final double[] zenithDelay = computeZenithDelay(point, date);
        // Mapping function
        final double[] mappingFunction = mappingFactors(trackingCoordinates, point, weather, date);
        // Tropospheric path delay
        return new TroposphericDelay(zenithDelay[0],
                                     zenithDelay[1],
                                     zenithDelay[0] * mappingFunction[0],
                                     zenithDelay[1] * mappingFunction[1]);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final FieldPressureTemperatureHumidity<T> weather,
                                                                                   final T[] parameters, final FieldAbsoluteDate<T> date) {
        // Zenith delay
        final T[] zenithDelay = computeZenithDelay(point, date);
        // Mapping function
        final T[] mappingFunction = mappingFactors(trackingCoordinates, point, weather, date);
        // Tropospheric path delay
        return new FieldTroposphericDelay<>(zenithDelay[0],
                                            zenithDelay[1],
                                            zenithDelay[0].multiply(mappingFunction[0]),
                                            zenithDelay[1].multiply(mappingFunction[1]));
    }

    /**
     * This method allows the  computation of the zenith hydrostatic and
     * zenith wet delay. The resulting element is an array having the following form:
     * <ul>
     * <li>double[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>double[1] = D<sub>wz</sub> → zenith wet delay
     * </ul>
     *
     * @param point station location
     * @param date  current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    public double[] computeZenithDelay(final GeodeticPoint point, final AbsoluteDate date) {

        final PressureTemperatureHumidity pth = pthProvider.getWeatherParameters(point, date);
        final double fsite   = getSiteFunctionValue(point);

        // Array for zenith delay
        final double[] delay = new double[2];

        // Zenith delay for the hydrostatic component
        // beware since version 12.1 pressure is in Pa and not in hPa, hence the scaling has changed
        delay[0] = pth.getPressure() * 0.00002416579 * (fLambdaH / fsite);

        // Zenith delay for the non-hydrostatic component
        // beware since version 12.1 e0 is in Pa and not in hPa, hence the scaling has changed
        delay[1] = 0.000001 * (5.316 * fLambdaNH - 3.759 * fLambdaH) * (pth.getWaterVaporPressure() / fsite);

        return delay;
    }

    /**
     * This method allows the  computation of the zenith hydrostatic and
     * zenith wet delay. The resulting element is an array having the following form:
     * <ul>
     * <li>T[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>T[1] = D<sub>wz</sub> → zenith wet delay
     * </ul>
     *
     * @param <T>   type of the elements
     * @param point station location
     * @param date  current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    public <T extends CalculusFieldElement<T>> T[] computeZenithDelay(final FieldGeodeticPoint<T> point,
                                                                      final FieldAbsoluteDate<T> date) {

        final FieldPressureTemperatureHumidity<T> pth = pthProvider.getWeatherParameters(point, date);

        final T fsite   = getSiteFunctionValue(point);

        // Array for zenith delay
        final T[] delay = MathArrays.buildArray(date.getField(), 2);

        // Zenith delay for the hydrostatic component
        // beware since version 12.1 pressure is in Pa and not in hPa, hence the scaling has changed
        delay[0] =  pth.getPressure().multiply(0.00002416579).multiply(fLambdaH).divide(fsite);

        // Zenith delay for the non-hydrostatic component
        // beware since version 12.1 e0 is in Pa and not in hPa, hence the scaling has changed
        delay[1] = pth.getWaterVaporPressure().divide(fsite).
                   multiply(0.000001 * (5.316 * fLambdaNH - 3.759 * fLambdaH));

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
    public double[] mappingFactors(final TrackingCoordinates trackingCoordinates,
                                   final GeodeticPoint point,
                                   final PressureTemperatureHumidity weather,
                                   final AbsoluteDate date) {
        final double sinE = FastMath.sin(trackingCoordinates.getElevation());

        final PressureTemperatureHumidity pth = pthProvider.getWeatherParameters(point, date);
        final double T2degree = pth.getTemperature() - 273.15;

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
    public <T extends CalculusFieldElement<T>> T[] mappingFactors(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                  final FieldGeodeticPoint<T> point,
                                                                  final FieldPressureTemperatureHumidity<T> weather,
                                                                  final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();

        final T sinE = FastMath.sin(trackingCoordinates.getElevation());

        final FieldPressureTemperatureHumidity<T> pth = pthProvider.getWeatherParameters(point, date);
        final T T2degree = pth.getTemperature().subtract(273.15);

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

    /** Get the site parameter.
     *
     * @param point station location
     * @return the site parameter.
     */
    private double getSiteFunctionValue(final GeodeticPoint point) {
        return 1. - 0.00266 * FastMath.cos(2. * point.getLatitude()) - 0.00000028 * point.getAltitude();
    }

    /** Get the site parameter.
     *
     * @param <T> type of the elements
     * @param point station location
     * @return the site parameter.
     */
    private <T extends CalculusFieldElement<T>> T getSiteFunctionValue(final FieldGeodeticPoint<T> point) {
        return FastMath.cos(point.getLatitude().multiply(2.)).multiply(0.00266).add(point.getAltitude().multiply(0.00000028)).negate().add(1.);
    }

    /** Compute the coefficients of the Mapping Function.
     *
     * @param t the temperature at the station site, °C
     * @param a0 first coefficient
     * @param a1 second coefficient
     * @param a2 third coefficient
     * @param a3 fourth coefficient
     * @param point station location
     * @return the value of the coefficient
     */
    private double computeMFCoeffient(final double a0, final double a1, final double a2, final double a3,
                                      final double t, final GeodeticPoint point) {
        return a0 + a1 * t + a2 * FastMath.cos(point.getLatitude()) + a3 * point.getAltitude();
    }

    /** Compute the coefficients of the Mapping Function.
     *
     * @param <T> type of the elements
     * @param t the temperature at the station site, °C
     * @param a0 first coefficient
     * @param a1 second coefficient
     * @param a2 third coefficient
     * @param a3 fourth coefficient
     * @param point station location
     * @return the value of the coefficient
     */
    private <T extends CalculusFieldElement<T>> T computeMFCoeffient(final double a0, final double a1, final double a2, final double a3,
                                                                     final T t, final FieldGeodeticPoint<T> point) {
        return point.getAltitude().multiply(a3).add(FastMath.cos(point.getLatitude()).multiply(a2)).add(t.multiply(a1).add(a0));
    }

}
