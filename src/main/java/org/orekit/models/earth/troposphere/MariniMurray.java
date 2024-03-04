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
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;
import org.orekit.utils.units.Unit;
import org.orekit.utils.units.UnitsConverter;

/** The Marini-Murray tropospheric delay model for laser ranging.
 *
 * @see "Marini, J.W., and C.W. Murray, correction of Laser Range Tracking Data for
 *      Atmospheric Refraction at Elevations Above 10 degrees, X-591-73-351, NASA GSFC, 1973"
 *
 * @author Joris Olympio
 * @author Luc Maisonobe
 * @since 12.1
 */
public class MariniMurray implements TroposphericModel {

    /** Laser frequency parameter. */
    private final double fLambda;

    /** Create a new Marini-Murray model for the troposphere.
     * @param lambda laser wavelength
     * @param lambdaUnits units in which {@code lambda} is given
     * @see TroposphericModelUtils#MICRO_M
     * @see TroposphericModelUtils#NANO_M
     * @since 12.1
     * */
    public MariniMurray(final double lambda, final Unit lambdaUnits) {

        // compute laser frequency parameter
        final double lambdaMicrometer = new UnitsConverter(lambdaUnits, TroposphericModelUtils.MICRO_M).convert(lambda);
        final double l2 = lambdaMicrometer  * lambdaMicrometer;
        fLambda = 0.9650 + (0.0164 + 0.000228 / l2) / l2;

    }

    /** {@inheritDoc} */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                       final PressureTemperatureHumidity weather,
                                       final double[] parameters, final AbsoluteDate date) {

        final double p = weather.getPressure();
        final double t = weather.getTemperature();
        final double e = weather.getWaterVaporPressure();

        // beware since version 12.1 pressures are in Pa and not in hPa, hence the scaling has changed
        final double Ah = 0.00002357 * p;
        final double Aw = 0.00000141 * e;
        final double K = 1.163 - 0.00968 * FastMath.cos(2 * point.getLatitude()) - 0.00104 * t + 0.0000001435 * p;
        final double B = 1.084e-10 * p * t * K + 4.734e-12 * p * (p / t) * (2 * K) / (3 * K - 1);
        final double flambda = getLaserFrequencyParameter();

        final double fsite = getSiteFunctionValue(point);

        final double sinE = FastMath.sin(trackingCoordinates.getElevation());
        final double totalZenith       = (flambda / fsite) * (Ah + Aw + B) / (1.0   + B / ((Ah + Aw + B) * (1.0   + 0.01)));
        final double totalElev         = (flambda / fsite) * (Ah + Aw + B) / (sinE  + B / ((Ah + Aw + B) * (sinE  + 0.01)));
        final double hydrostaticZenith = (flambda / fsite) * (Ah +      B) / (1.0   + B / ((Ah +      B) * (1.0   + 0.01)));
        final double hydrostaticElev   = (flambda / fsite) * (Ah +      B) / (sinE  + B / ((Ah +      B) * (sinE  + 0.01)));
        return new TroposphericDelay(hydrostaticZenith, totalZenith - hydrostaticZenith,
                                     hydrostaticElev,   totalElev   - hydrostaticElev);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final FieldPressureTemperatureHumidity<T> weather,
                                                                                   final T[] parameters, final FieldAbsoluteDate<T> date) {

        final T p = weather.getPressure();
        final T t = weather.getTemperature();
        final T e = weather.getWaterVaporPressure();

        // beware since version 12.1 pressures are in Pa and not in hPa, hence the scaling has changed
        final T Ah = p.multiply(0.00002357);
        final T Aw = e.multiply(0.00000141);
        final T K = FastMath.cos(point.getLatitude().multiply(2.)).multiply(0.00968).negate().
                    add(1.163).
                    subtract(t.multiply(0.00104)).
                    add(p.multiply(0.0000001435));
        final T B = K.multiply(t.multiply(p).multiply(1.084e-10 )).
                               add(K.multiply(2.).multiply(p.multiply(p).divide(t).multiply(4.734e-12)).divide(K.multiply(3.).subtract(1.)));
        final double flambda = getLaserFrequencyParameter();

        final T fsite = getSiteFunctionValue(point);

        final T sinE = FastMath.sin(trackingCoordinates.getElevation());
        final T one  = date.getField().getOne();
        final T totalZenith       = fsite.divide(flambda).reciprocal().
                                    multiply(B.add(Ah).add(Aw)).
                                    divide(one.add(one.add(0.01).multiply(B.add(Ah).add(Aw)).divide(B).reciprocal()));
        final T totalElev         = fsite.divide(flambda).reciprocal().
                                    multiply(B.add(Ah).add(Aw)).
                                    divide(sinE.add(sinE.add(0.01).multiply(B.add(Ah).add(Aw)).divide(B).reciprocal()));
        final T hydrostaticZenith = fsite.divide(flambda).reciprocal().
                                    multiply(B.add(Ah)).
                                    divide(one.add(one.add(0.01).multiply(B.add(Ah)).divide(B).reciprocal()));
        final T hydrostaticElev   = fsite.divide(flambda).reciprocal().
                                    multiply(B.add(Ah)).
                                    divide(sinE.add(sinE.add(0.01).multiply(B.add(Ah)).divide(B).reciprocal()));
        return new FieldTroposphericDelay<>(hydrostaticZenith, totalZenith.subtract(hydrostaticZenith),
                                            hydrostaticElev,   totalElev.subtract(hydrostaticElev));
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
        return fLambda;
    }

    /** Get the site parameter.
     *
     * @param point station location
     * @return the site parameter.
     */
    private double getSiteFunctionValue(final GeodeticPoint point) {
        return 1. - 0.0026 * FastMath.cos(2 * point.getLatitude()) - 0.00031 * 0.001 * point.getAltitude();
    }

    /** Get the site parameter.
    *
    * @param <T> type of the elements
    * @param point station location
    * @return the site parameter.
    */
    private <T extends CalculusFieldElement<T>> T getSiteFunctionValue(final FieldGeodeticPoint<T> point) {
        return FastMath.cos(point.getLatitude().multiply(2)).multiply(0.0026).add(point.getAltitude().multiply(0.001).multiply(0.00031)).negate().add(1.);
    }

}
