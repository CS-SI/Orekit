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
package org.orekit.models.earth.troposphere;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.water.CIPM2007;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.units.Unit;

/** The Marini-Murray tropospheric delay model for laser ranging.
 *
 * @see "Marini, J.W., and C.W. Murray, correction of Laser Range Tracking Data for
 *      Atmospheric Refraction at Elevations Above 10 degrees, X-591-73-351, NASA GSFC, 1973"
 *
 * @author Joris Olympio
 */
public class MariniMurrayModel implements DiscreteTroposphericModel {

    /** Pressure, temperature and humidity. */
    private PressureTemperatureHumidity pth;

    /** Laser wavelength, micrometers. */
    private double lambda;

    /** Create a new Marini-Murray model for the troposphere using the given
     * environmental conditions.
     * @param t0 the temperature at the station, K
     * @param p0 the atmospheric pressure at the station, mbar
     * @param rh the humidity at the station, percent (50% -&gt; 0.5)
     * @param lambda laser wavelength (c/f), nm
     */
    public MariniMurrayModel(final double t0, final double p0, final double rh, final double lambda) {
        this(new PressureTemperatureHumidity(Unit.HECTO_PASCAL.toSI(p0),
                                             t0,
                                             new CIPM2007().waterVaporPressure(Unit.HECTO_PASCAL.toSI(p0), t0, rh)),
             lambda);
    }

    /** Create a new Marini-Murray model for the troposphere.
     * @param pth atmospheric pressure, temperature and humidity at the station
     * @param lambda laser wavelength, nm
     * */
    public MariniMurrayModel(final PressureTemperatureHumidity pth, final double lambda) {
        this.pth    = pth;
        this.lambda = lambda * 1.0e-3;
    }

    /** Create a new Marini-Murray model using a standard atmosphere model.
     *
     * <ul>
     * <li>temperature: 20 degree Celsius</li>
     * <li>pressure: 1013.25 mbar</li>
     * <li>humidity: 50%</li>
     * </ul>
     *
     * @param lambda laser wavelength (c/f), nm
     *
     * @return a Marini-Murray model with standard environmental values
     */
    public static MariniMurrayModel getStandardModel(final double lambda) {
        final double p  = Unit.HECTO_PASCAL.toSI(1013.25);
        final double t  = 273.15 + 20;
        final double rh = 0.5;
        return new MariniMurrayModel(new PressureTemperatureHumidity(p, t,
                                                                     new CIPM2007().waterVaporPressure(p, t, rh)),
                                     lambda);
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final GeodeticPoint point,
                            final double[] parameters, final AbsoluteDate date) {

        final double p = pth.getPressure();
        final double t = pth.getTemperature();
        final double e = pth.getWaterVaporPressure();

        // beware since version 12.1 pressures are in Pa and not in hPa, hence the scaling has changed
        final double A = 0.00002357 * p + 0.00000141 * e;
        final double K = 1.163 - 0.00968 * FastMath.cos(2 * point.getLatitude()) - 0.00104 * t + 0.0000001435 * p;
        final double B = 1.084e-10 * p * t * K + 4.734e-12 * p * (p / t) * (2 * K) / (3 * K - 1);
        final double flambda = getLaserFrequencyParameter();

        final double fsite = getSiteFunctionValue(point);

        final double sinE = FastMath.sin(elevation);
        final double dR = (flambda / fsite) * (A + B) / (sinE + B / ((A + B) * (sinE + 0.01)) );
        return dR;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation, final FieldGeodeticPoint<T> point,
                                                       final T[] parameters, final FieldAbsoluteDate<T> date) {

        final double p = pth.getPressure();
        final double t = pth.getTemperature();
        final double e = pth.getWaterVaporPressure();

        // beware since version 12.1 pressures are in Pa and not in hPa, hence the scaling has changed
        final double A = 0.00002357 * p + 0.00000141 * e;
        final T K = FastMath.cos(point.getLatitude().multiply(2.)).multiply(0.00968).negate().add(1.163).subtract(0.00104 * t).add(0.0000001435 * p);
        final T B = K.multiply(1.084e-10 * p * t).add(K.multiply(2.).multiply(4.734e-12 * p * (p / t)).divide(K.multiply(3.).subtract(1.)));
        final double flambda = getLaserFrequencyParameter();

        final T fsite = getSiteFunctionValue(point);

        final T sinE = FastMath.sin(elevation);
        final T dR = fsite.divide(flambda).reciprocal().multiply(B.add(A)).divide(sinE.add(sinE.add(0.01).multiply(B.add(A)).divide(B).reciprocal()));
        return dR;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
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
     * @param point station location
     * @return the laser frequency parameter f(lambda).
     */
    private double getSiteFunctionValue(final GeodeticPoint point) {
        return 1. - 0.0026 * FastMath.cos(2 * point.getLatitude()) - 0.00031 * 0.001 * point.getAltitude();
    }

    /** Get the laser frequency parameter f(lambda).
    *
    * @param <T> type of the elements
    * @param point station location
    * @return the laser frequency parameter f(lambda).
    */
    private <T extends CalculusFieldElement<T>> T getSiteFunctionValue(final FieldGeodeticPoint<T> point) {
        return FastMath.cos(point.getLatitude().multiply(2)).multiply(0.0026).add(point.getAltitude().multiply(0.001).multiply(0.00031)).negate().add(1.);
    }

}
