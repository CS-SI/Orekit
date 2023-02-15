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
package org.orekit.models.earth.weather;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.models.earth.Geoid;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.utils.LegendrePolynomials;

/** The Global Pressure and Temperature model.
 * This model is an empirical model that provides the temperature and the pressure depending
 * the latitude and the longitude of the station.
 * <p>
 * The Global Pressure and Temperature model is based on spherical harmonics up
 * to degree and order of 9. The residual values ​​of this model can reach 20 hPa
 * for pressure and 10 ° C for temperature. They are significant for higher latitudes and
 * small near the equator (Böhm, 2007)
 * </p>
 *
 * @see "J. Böhm, R. Heinkelmann, and H. Schuh (2007),
 * Short Note: A global model of pressure and temperature for geodetic applications. J Geod,
 * doi:10.1007/s00190-007-0135-3."
 *
 * @author Bryan Cazabonne
 *
 */
public class GlobalPressureTemperatureModel implements WeatherModel {

    /** Temperature gradient (°C/m). */
    private static final double TEMPERATURE_GRADIENT = -6.5e-3;

    /** Geodetic latitude, in radians. */
    private final double latitude;

    /** Geodetic longitude, in radians. */
    private final double longitude;

    /** Temperature site, in kelvins. */
    private double temperature;

    /** Pressure site, in hPa. */
    private double pressure;

    /** Body frame related to body shape. */
    private final Frame bodyFrame;

    /** Data context for time and gravity. */
    private final DataContext dataContext;

    /** Build a new instance.
     * <p>
     * At the initialization the values of the pressure and the temperature are set to NaN.
     * The user has to call {@link #weatherParameters(double, AbsoluteDate)} method before using
     * the values of the pressure and the temperature.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param latitude geodetic latitude, in radians
     * @param longitude geodetic longitude, in radians
     * @param bodyFrame the frame to attach to the ellipsoid. The origin is at
     *                  the center of mass, the z axis is the minor axis.
     * @see #GlobalPressureTemperatureModel(double, double, Frame, DataContext)
     */
    @DefaultDataContext
    public GlobalPressureTemperatureModel(final double latitude, final double longitude, final Frame bodyFrame) {
        this(latitude, longitude, bodyFrame, DataContext.getDefault());
    }

    /** Build a new instance.
     * <p>
     * At the initialization the values of the pressure and the temperature are set to NaN.
     * The user has to call {@link #weatherParameters(double, AbsoluteDate)} method before using
     * the values of the pressure and the temperature.
     * </p>
     * @param latitude geodetic latitude, in radians
     * @param longitude geodetic longitude, in radians
     * @param bodyFrame the frame to attach to the ellipsoid. The origin is at
     * @param dataContext to use for time and gravity.
     * @since 10.1
     */
    public GlobalPressureTemperatureModel(final double latitude,
                                          final double longitude,
                                          final Frame bodyFrame,
                                          final DataContext dataContext) {
        this.bodyFrame   = bodyFrame;
        this.latitude    = latitude;
        this.longitude   = longitude;
        this.temperature = Double.NaN;
        this.pressure    = Double.NaN;
        this.dataContext = dataContext;
    }

    /** Get the atmospheric temperature of the station depending its position.
     * @return the temperature in kelvins
     */
    public double getTemperature() {
        return temperature;
    }

    /** Get the atmospheric pressure of the station depending its position.
     * @return the pressure in hPa
     */
    public double getPressure() {
        return pressure;
    }

    @Override
    @DefaultDataContext
    public void weatherParameters(final double height, final AbsoluteDate date) {

        // Day of year computation
        final DateTimeComponents dtc =
                date.getComponents(dataContext.getTimeScales().getUTC());
        final int dofyear = dtc.getDate().getDayOfYear();

        // Reference day: 28 January 1980 (Niell, 1996)
        final int t0 = 28;
        final double coef = ((dofyear + 1 - t0) / 365.25) * 2 * FastMath.PI;
        final double cosCoef = FastMath.cos(coef);

        // Compute Legendre Polynomials Pnm(sin(phi))
        final int degree = 9;
        final int order  = 9;
        final LegendrePolynomials p = new LegendrePolynomials(degree, order, FastMath.sin(latitude));

        // Geoid for height computation
        final Geoid geoid = new Geoid(
                dataContext.getGravityFields().getNormalizedProvider(degree, order),
                ReferenceEllipsoid.getWgs84(bodyFrame));

        // Corrected height
        final double correctedheight = FastMath.max(0.0, height - geoid.getUndulation(latitude, longitude, date));

        // Eq. 4 (Ref)
        double meanT0      = 0.0;
        double amplitudeT0 = 0.0;
        double meanP0      = 0.0;
        double amplitudeP0 = 0.0;
        final ABCoefficients abCoef = new ABCoefficients();
        int j = 0;
        for (int n = 0; n <= 9; n++) {
            for (int m = 0; m <= n; m++) {
                final SinCos sc = FastMath.sinCos(m * longitude);
                final double pCosmLambda = p.getPnm(n, m) * sc.cos();
                final double pSinmLambda = p.getPnm(n, m) * sc.sin();

                meanT0      = meanT0 +
                                (abCoef.getAnmTemperatureMean(j) * pCosmLambda + abCoef.getBnmTemperatureMean(j) * pSinmLambda);
                amplitudeT0 = amplitudeT0 +
                                (abCoef.getAnmTemperatureAmpl(j) * pCosmLambda + abCoef.getBnmTemperatureAmpl(j) * pSinmLambda);
                meanP0      = meanP0 +
                                (abCoef.getAnmPressureMean(j) * pCosmLambda + abCoef.getBnmPressureMean(j) * pSinmLambda);
                amplitudeP0 = amplitudeP0 +
                                (abCoef.getAnmPressureAmpl(j) * pCosmLambda + abCoef.getBnmPressureAmpl(j) * pSinmLambda);

                j = j + 1;
            }
        }

        // Eq. 3 (Ref)
        final double temp0 = meanT0 + amplitudeT0 * cosCoef;
        final double pres0 = meanP0 + amplitudeP0 * cosCoef;

        // Compute pressure and temperature Eq. 1 and 2 (Ref)
        final double degrees = temp0 + TEMPERATURE_GRADIENT * correctedheight;
        this.temperature = degrees + 273.15;
        this.pressure    = pres0 * FastMath.pow(1.0 - correctedheight * 0.0000226, 5.225);
    }

    private static class ABCoefficients {

        /** Mean A<sub>nm</sub> coefficients for the pressure. */
        private static final double[] A_PRESSURE_MEAN = {
            1.0108e+03,
            8.4886e+00,
            1.4799e+00,
            -1.3897e+01,
            3.7516e-03,
            -1.4936e-01,
            1.2232e+01,
            -7.6615e-01,
            -6.7699e-02,
            8.1002e-03,
            -1.5874e+01,
            3.6614e-01,
            -6.7807e-02,
            -3.6309e-03,
            5.9966e-04,
            4.8163e+00,
            -3.7363e-01,
            -7.2071e-02,
            1.9998e-03,
            -6.2385e-04,
            -3.7916e-04,
            4.7609e+00,
            -3.9534e-01,
            8.6667e-03,
            1.1569e-02,
            1.1441e-03,
            -1.4193e-04,
            -8.5723e-05,
            6.5008e-01,
            -5.0889e-01,
            -1.5754e-02,
            -2.8305e-03,
            5.7458e-04,
            3.2577e-05,
            -9.6052e-06,
            -2.7974e-06,
            1.3530e+00,
            -2.7271e-01,
            -3.0276e-04,
            3.6286e-03,
            -2.0398e-04,
            1.5846e-05,
            -7.7787e-06,
            1.1210e-06,
            9.9020e-08,
            5.5046e-01,
            -2.7312e-01,
            3.2532e-03,
            -2.4277e-03,
            1.1596e-04,
            2.6421e-07,
            -1.3263e-06,
            2.7322e-07,
            1.4058e-07,
            4.9414e-09
        };

        /** Mean B<sub>nm</sub> coefficients for the pressure. */
        private static final double[] B_PRESSURE_MEAN = {
            0.0000e+00,
            0.0000e+00,
            -1.2878e+00,
            0.0000e+00,
            7.0444e-01,
            3.3222e-01,
            0.0000e+00,
            -2.9636e-01,
            7.2248e-03,
            7.9655e-03,
            0.0000e+00,
            1.0854e+00,
            1.1145e-02,
            -3.6513e-02,
            3.1527e-03,
            0.0000e+00,
            -4.8434e-01,
            5.2023e-02,
            -1.3091e-02,
            1.8515e-03,
            1.5422e-04,
            0.0000e+00,
            6.8298e-01,
            2.5261e-03,
            -9.9703e-04,
            -1.0829e-03,
            +1.7688e-04,
            -3.1418e-05,
            +0.0000e+00,
            -3.7018e-01,
            4.3234e-02,
            7.2559e-03,
            3.1516e-04,
            2.0024e-05,
            -8.0581e-06,
            -2.3653e-06,
            0.0000e+00,
            1.0298e-01,
            -1.5086e-02,
            5.6186e-03,
            3.2613e-05,
            4.0567e-05,
            -1.3925e-06,
            -3.6219e-07,
            -2.0176e-08,
            0.0000e+00,
            -1.8364e-01,
            1.8508e-02,
            7.5016e-04,
            -9.6139e-05,
            -3.1995e-06,
            1.3868e-07,
            -1.9486e-07,
            3.0165e-10,
            -6.4376e-10
        };

        /** Amplitude A<sub>nm</sub> coefficients for the pressure. */
        private static final double[] A_PRESSURE_AMPLITUDE = {
            -1.0444e-01,
            1.6618e-01,
            -6.3974e-02,
            1.0922e+00,
            5.7472e-01,
            -3.0277e-01,
            -3.5087e+00,
            7.1264e-03,
            -1.4030e-01,
            3.7050e-02,
            4.0208e-01,
            -3.0431e-01,
            -1.3292e-01,
            4.6746e-03,
            -1.5902e-04,
            2.8624e+00,
            -3.9315e-01,
            -6.4371e-02,
            1.6444e-02,
            -2.3403e-03,
            4.2127e-05,
            1.9945e+00,
            -6.0907e-01,
            -3.5386e-02,
            -1.0910e-03,
            -1.2799e-04,
            4.0970e-05,
            2.2131e-05,
            -5.3292e-01,
            -2.9765e-01,
            -3.2877e-02,
            1.7691e-03,
            5.9692e-05,
            3.1725e-05,
            2.0741e-05,
            -3.7622e-07,
            2.6372e+00,
            -3.1165e-01,
            1.6439e-02,
            2.1633e-04,
            1.7485e-04,
            2.1587e-05,
            6.1064e-06,
            -1.3755e-08,
            -7.8748e-08,
            -5.9152e-01,
            -1.7676e-01,
            8.1807e-03,
            1.0445e-03,
            2.3432e-04,
            9.3421e-06,
            2.8104e-06,
            -1.5788e-07,
            -3.0648e-08,
            2.6421e-10
        };

        /** Amplitude B<sub>nm</sub> coefficients for the pressure. */
        private static final double[] B_PRESSURE_AMPLITUDE = {
            0.0000e+00,
            0.0000e+00,
            9.3340e-01,
            0.0000e+00,
            8.2346e-01,
            2.2082e-01,
            0.0000e+00,
            9.6177e-01,
            -1.5650e-02,
            1.2708e-03,
            0.0000e+00,
            -3.9913e-01,
            2.8020e-02,
            2.8334e-02,
            8.5980e-04,
            0.0000e+00,
            3.0545e-01,
            -2.1691e-02,
            6.4067e-04,
            -3.6528e-05,
            -1.1166e-04,
            0.0000e+00,
            -7.6974e-02,
            -1.8986e-02,
            +5.6896e-03,
            -2.4159e-04,
            -2.3033e-04,
            -9.6783e-06,
            0.0000e+00,
            -1.0218e-01,
            -1.3916e-02,
            -4.1025e-03,
            -5.1340e-05,
            -7.0114e-05,
            -3.3152e-07,
            1.6901e-06,
            0.0000e+00,
            -1.2422e-02,
            +2.5072e-03,
            +1.1205e-03,
            -1.3034e-04,
            -2.3971e-05,
            -2.6622e-06,
            5.7852e-07,
            4.5847e-08,
            0.0000e+00,
            4.4777e-02,
            -3.0421e-03,
            2.6062e-05,
            -7.2421e-05,
            1.9119e-06,
            3.9236e-07,
            2.2390e-07,
            2.9765e-09,
            -4.6452e-09
        };

        /** Mean A<sub>nm</sub> coefficients for the temperature. */
        private static final double[] A_TEMPERATURE_MEAN = {
            1.6257e+01,
            2.1224e+00,
            9.2569e-01,
            -2.5974e+01,
            1.4510e+00,
            9.2468e-02,
            -5.3192e-01,
            2.1094e-01,
            -6.9210e-02,
            -3.4060e-02,
            -4.6569e+00,
            2.6385e-01,
            -3.6093e-02,
            1.0198e-02,
            -1.8783e-03,
            7.4983e-01,
            1.1741e-01,
            3.9940e-02,
            5.1348e-03,
            5.9111e-03,
            8.6133e-06,
            6.3057e-01,
            1.5203e-01,
            3.9702e-02,
            4.6334e-03,
            2.4406e-04,
            1.5189e-04,
            1.9581e-07,
            5.4414e-01,
            3.5722e-01,
            5.2763e-02,
            4.1147e-03,
            -2.7239e-04,
            -5.9957e-05,
            1.6394e-06,
            -7.3045e-07,
            -2.9394e+00,
            5.5579e-02,
            1.8852e-02,
            3.4272e-03,
            -2.3193e-05,
            -2.9349e-05,
            3.6397e-07,
            2.0490e-06,
            -6.4719e-08,
            -5.2225e-01,
            2.0799e-01,
            1.3477e-03,
            3.1613e-04,
            -2.2285e-04,
            -1.8137e-05,
            -1.5177e-07,
            6.1343e-07,
            7.8566e-08,
            1.0749e-09
        };

        /** Mean B<sub>nm</sub> coefficients for the temperature. */
        private static final double[] B_TEMPERATURE_MEAN = {
            0.0000e+00,
            0.0000e+00,
            1.0210e+00,
            0.0000e+00,
            6.0194e-01,
            1.2292e-01,
            0.0000e+00,
            -4.2184e-01,
            1.8230e-01,
            4.2329e-02,
            0.0000e+00,
            9.3312e-02,
            9.5346e-02,
            -1.9724e-03,
            5.8776e-03,
            0.0000e+00,
            -2.0940e-01,
            3.4199e-02,
            -5.7672e-03,
            -2.1590e-03,
            5.6815e-04,
            0.0000e+00,
            2.2858e-01,
            1.2283e-02,
            -9.3679e-03,
            -1.4233e-03,
            -1.5962e-04,
            4.0160e-05,
            0.0000e+00,
            3.6353e-02,
            -9.4263e-04,
            -3.6762e-03,
            5.8608e-05,
            -2.6391e-05,
            3.2095e-06,
            -1.1605e-06,
            0.0000e+00,
            1.6306e-01,
            1.3293e-02,
            -1.1395e-03,
            5.1097e-05,
            3.3977e-05,
            7.6449e-06,
            -1.7602e-07,
            -7.6558e-08,
            0.0000e+00,
            -4.5415e-02,
            -1.8027e-02,
            3.6561e-04,
            -1.1274e-04,
            1.3047e-05,
            2.0001e-06,
            -1.5152e-07,
            -2.7807e-08,
            7.7491e-09
        };

        /** Amplitude A<sub>nm</sub> coefficients for the temperature. */
        private static final double[] A_TEMPERATURE_AMPLITUDE = {
            -1.8654e+00,
            -9.0041e+00,
            -1.2974e-01,
            -3.6053e+00,
            2.0284e-02,
            2.1872e-01,
            -1.3015e+00,
            4.0355e-01,
            2.2216e-01,
            -4.0605e-03,
            1.9623e+00,
            4.2887e-01,
            2.1437e-01,
            -1.0061e-02,
            -1.1368e-03,
            -6.9235e-02,
            5.6758e-01,
            1.1917e-01,
            -7.0765e-03,
            3.0017e-04,
            3.0601e-04,
            1.6559e+00,
            2.0722e-01,
            6.0013e-02,
            1.7023e-04,
            -9.2424e-04,
            1.1269e-05,
            -6.9911e-06,
            -2.0886e+00,
            -6.7879e-02,
            -8.5922e-04,
            -1.6087e-03,
            -4.5549e-05,
            3.3178e-05,
            -6.1715e-06,
            -1.4446e-06,
            -3.7210e-01,
            1.5775e-01,
            -1.7827e-03,
            -4.4396e-04,
            2.2844e-04,
            -1.1215e-05,
            -2.1120e-06,
            -9.6421e-07,
            -1.4170e-08,
            7.8720e-01,
            -4.4238e-02,
            -1.5120e-03,
            -9.4119e-04,
            4.0645e-06,
            -4.9253e-06,
            -1.8656e-06,
            -4.0736e-07,
            -4.9594e-08,
            1.6134e-09
        };

        /** Amplitude B<sub>nm</sub> coefficients for the temperature. */
        private static final double[] B_TEMPERATURE_AMPLITUDE = {
            0.0000e+00,
            0.0000e+00,
            -8.9895e-01,
            0.0000e+00,
            -1.0790e+00,
            -1.2699e-01,
            0.0000e+00,
            -5.9033e-01,
            3.4865e-02,
            -3.2614e-02,
            0.0000e+00,
            -2.4310e-02,
            1.5607e-02,
            -2.9833e-02,
            -5.9048e-03,
            0.0000e+00,
            2.8383e-01,
            4.0509e-02,
            -1.8834e-02,
            -1.2654e-03,
            -1.3794e-04,
            0.0000e+00,
            1.3306e-01,
            3.4960e-02,
            -3.6799e-03,
            -3.5626e-04,
            1.4814e-04,
            3.7932e-06,
            0.0000e+00,
            2.0801e-01,
            6.5640e-03,
            -3.4893e-03,
            -2.7395e-04,
            7.4296e-05,
            -7.9927e-06,
            -1.0277e-06,
            0.0000e+00,
            3.6515e-02,
            -7.4319e-03,
            -6.2873e-04,
            8.2461e-05,
            3.1095e-05,
            -5.3860e-07,
            -1.2055e-07,
            -1.1517e-07,
            0.0000e+00,
            3.1404e-02,
            1.5580e-02,
            -1.1428e-03,
            3.3529e-05,
            1.0387e-05,
            -1.9378e-06,
            -2.7327e-07,
            7.5833e-09,
            -9.2323e-09
        };

        /** Build a new instance. */
        ABCoefficients() {

        }

        /** Get the value of the mean A<sub>nm</sub> pressure coefficient for the given index.
         * @param index index
         * @return the mean A<sub>nm</sub> pressure coefficient for the given index
         */
        public double getAnmPressureMean(final int index) {
            return A_PRESSURE_MEAN[index];
        }

        /** Get the value of the mean B<sub>nm</sub> pressure coefficient for the given index.
         * @param index index
         * @return the mean B<sub>nm</sub> pressure coefficient for the given index
         */
        public double getBnmPressureMean(final int index) {
            return B_PRESSURE_MEAN[index];
        }

        /** Get the value of the amplitude A<sub>nm</sub> pressure coefficient for the given index.
         * @param index index
         * @return the amplitude A<sub>nm</sub> pressure coefficient for the given index.
         */
        public double getAnmPressureAmpl(final int index) {
            return A_PRESSURE_AMPLITUDE[index];
        }

        /** Get the value of the amplitude B<sub>nm</sub> pressure coefficient for the given index.
         * @param index index
         * @return the amplitude B<sub>nm</sub> pressure coefficient for the given index
         */
        public double getBnmPressureAmpl(final int index) {
            return B_PRESSURE_AMPLITUDE[index];
        }

        /** Get the value of the mean A<sub>nm</sub> temperature coefficient for the given index.
         * @param index index
         * @return the mean A<sub>nm</sub> temperature coefficient for the given index
         */
        public double getAnmTemperatureMean(final int index) {
            return A_TEMPERATURE_MEAN[index];
        }

        /** Get the value of the mean B<sub>nm</sub> temperature coefficient for the given index.
         * @param index index
         * @return the mean B<sub>nm</sub> temperature coefficient for the given index
         */
        public double getBnmTemperatureMean(final int index) {
            return B_TEMPERATURE_MEAN[index];
        }

        /** Get the value of the amplitude A<sub>nm</sub> temperature coefficient for the given index.
         * @param index index
         * @return the amplitude A<sub>nm</sub> temperature coefficient for the given index.
         */
        public double getAnmTemperatureAmpl(final int index) {
            return A_TEMPERATURE_AMPLITUDE[index];
        }

        /** Get the value of the amplitude B<sub>nm</sub> temperature coefficient for the given index.
         * @param index index
         * @return the amplitude B<sub>nm</sub> temperature coefficient for the given index
         */
        public double getBnmTemperatureAmpl(final int index) {
            return B_TEMPERATURE_AMPLITUDE[index];
        }
    }

}
