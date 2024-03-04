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

import org.hipparchus.CalculusFieldElement;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.water.CIPM2007;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

/** The Marini-Murray tropospheric delay model for laser ranging.
 *
 * @see "Marini, J.W., and C.W. Murray, correction of Laser Range Tracking Data for
 *      Atmospheric Refraction at Elevations Above 10 degrees, X-591-73-351, NASA GSFC, 1973"
 *
 * @author Joris Olympio
 * @deprecated as of 12.1, replaced by {@link MariniMurray}
 */
@Deprecated
public class MariniMurrayModel extends MariniMurray implements DiscreteTroposphericModel {

    /** Constant pressure, temperature and humidity. */
    private final PressureTemperatureHumidity pth;

    /** Create a new Marini-Murray model for the troposphere using the given
     * environmental conditions.
     * @param t0 the temperature at the station, K
     * @param p0 the atmospheric pressure at the station, mbar
     * @param rh the humidity at the station, as a ratio (50% → 0.5)
     * @param lambda laser wavelength (c/f), nm
     */
    public MariniMurrayModel(final double t0, final double p0, final double rh, final double lambda) {
        super(lambda, TroposphericModelUtils.NANO_M);
        this.pth = new PressureTemperatureHumidity(0,
                                                   TroposphericModelUtils.HECTO_PASCAL.toSI(p0),
                                                   t0,
                                                   new CIPM2007().
                                                   waterVaporPressure(TroposphericModelUtils.HECTO_PASCAL.toSI(p0),
                                                                      t0,
                                                                      rh),
                                                   Double.NaN,
                                                   Double.NaN);
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
        final double p  = TroposphericModelUtils.HECTO_PASCAL.toSI(1013.25);
        final double t  = 273.15 + 20;
        final double rh = 0.5;
        return new MariniMurrayModel(t, p, rh, lambda);
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final GeodeticPoint point,
                            final double[] parameters, final AbsoluteDate date) {
        return pathDelay(new TrackingCoordinates(0.0, elevation, 0.0), point,
                         pth, parameters, date).
               getDelay();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation,
                                                           final FieldGeodeticPoint<T> point,
                                                           final T[] parameters,
                                                           final FieldAbsoluteDate<T> date) {
        return pathDelay(new FieldTrackingCoordinates<>(date.getField().getZero(), elevation, date.getField().getZero()),
                         point,
                         new FieldPressureTemperatureHumidity<>(date.getField(), pth),
                         parameters, date).
               getDelay();
    }

}
