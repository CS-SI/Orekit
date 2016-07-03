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

import org.hipparchus.util.FastMath;

/** The Marini-Murray tropospheric delay model for laser ranging.
 *
 * @see "Marini, J.W., and C.W. Murray, correction of Laser Range Tracking Data for
 *      Atmospheric Refraction at Elevations Above 10 degrees, X-591-73-351, NASA GSFC, 1973"
 *
 * @author Joris Olympio
 */
public class MariniMurrayModel implements TroposphericModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 8442906721207317886L;

    /** The temperature at the station, K. */
    private double T0;

    /** The atmospheric pressure, mbar. */
    private double P0;

    /** water vapor pressure at the laser site, mbar. */
    private double e0;

    /** Geodetic site latitude, radians. */
    private double latitude;

    /** Laser wavelength, micrometers. */
    private double lambda;

    /** Create a new Marini-Murray model for the troposphere using the given
     * environmental conditions.
     * @param t0 the temperature at the station, K
     * @param p0 the atmospheric pressure at the station, mbar
     * @param rh the humidity at the station, percent (50% -&gt; 0.5)
     * @param latitude site latitude
     * @param lambda laser wavelength (c/f), nm
     */
    public MariniMurrayModel(final double t0, final double p0, final double rh, final double latitude, final double lambda) {
        this.T0 = t0;
        this.P0 = p0;

        this.e0 = getWaterVapor(rh);

        this.latitude = latitude;

        this.lambda = lambda * 1e-3;
    }

    /** Create a new Marini-Murray model using a standard atmosphere model.
     *
     * <ul>
     * <li>temperature: 20 degree Celsius
     * <li>pressure: 1013.25 mbar
     * <li>humidity: 50%
     * </ul>
     *
     * @param latitude site latitude
     * @param frequency laser frequency, Hz
     *
     * @return a Saastamoinen model with standard environmental values
     */
    public static MariniMurrayModel getStandardModel(final double latitude, final double frequency) {
        return new MariniMurrayModel(273.15 + 20, 1013.25, 0.5, latitude, frequency);
    }

    @Override
    public double pathDelay(final double elevation, final double height) {
        final double A = 0.002357 * P0 + 0.000141 * e0;
        final double K = 1.163 * 0.00968 * FastMath.cos(2 * latitude) - 0.00104 * T0 + 0.00001435 * P0;
        final double B = (1.084 * 1e-8) * P0 * T0 * K + (4.734 * 1e-8) * P0 * (P0 / T0) * (2 * K) / (3 * K - 1);
        final double flambda = getLaserFrequencyParameter();

        final double fsite = getSiteFunctionValue(height / 1000.);

        final double sinE = FastMath.sin(elevation);
        final double dR = (flambda / fsite) * (A + B) / (sinE + B / ((A + B) * (sinE + 0.01)) );
        return dR;
    }

    /** Get the laser frequency parameter f(lambda).
     * It is one for Ruby laser (lambda = 0.6943 micron)
     * For infrared lasers, f(lambda) = 0.97966.
     *
     * @return the laser frequency parameter f(lambda).
     */
    private double getLaserFrequencyParameter() {
        return 0.9650 + 0.0164 * FastMath.pow(lambda, -2) + 0.000228 * FastMath.pow(lambda, -4);
    }

    /** Get the laser frequency parameter f(lambda).
     *
     * @param height height above the geoid, km
     * @return the laser frequency parameter f(lambda).
     */
    private double getSiteFunctionValue(final double height) {
        return 1. - 0.0026 * FastMath.cos(2 * latitude) - 0.00031 * height;
    }

    /** Get the water vapor.
     * The water vapor model is the one of Giacomo and Davis as indicated in IERS TN 32, chap. 9.
     *
     * See: Giacomo, P., Equation for the dertermination of the density of moist air, Metrologia, V. 18, 1982
     *
     * @param rh relative humidity, in percent.
     * @return the water vapor, in mbar (1 mbar = 100 Pa).
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
