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
package org.orekit.models.earth.weather;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.models.earth.Geoid;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.time.AbsoluteDate;

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
 * @deprecated as of 12.1, replaced by {@link GlobalPressureTemperature}
 */
@Deprecated
public class GlobalPressureTemperatureModel extends GlobalPressureTemperature implements WeatherModel {

    /** Spherical harmonics degree. */
    private static final int DEGREE = 9;

    /** Spherical harmonics order. */
    private static final int ORDER = 9;

    /** Geodetic latitude, in radians. */
    private final double latitude;

    /** Geodetic longitude, in radians. */
    private final double longitude;

    /** Temperature site, in kelvins. */
    private double temperature;

    /** Pressure site, in hPa. */
    private double pressure;

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
        super(new Geoid(dataContext.getGravityFields().getNormalizedProvider(DEGREE, ORDER),
                        ReferenceEllipsoid.getWgs84(bodyFrame)),
              dataContext.getTimeScales().getUTC());
        this.latitude    = latitude;
        this.longitude   = longitude;
        this.temperature = Double.NaN;
        this.pressure    = Double.NaN;
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
    public void weatherParameters(final double height, final AbsoluteDate date) {

        final GeodeticPoint location = new GeodeticPoint(latitude, longitude, height);

        // Pressure and temperature
        final PressureTemperature pt = getWeatherParameters(location, date);
        this.temperature = pt.getTemperature();
        this.pressure    = TroposphericModelUtils.HECTO_PASCAL.fromSI(pt.getPressure());

    }

}
