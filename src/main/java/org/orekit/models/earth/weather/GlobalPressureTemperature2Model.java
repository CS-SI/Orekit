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
import org.orekit.data.DataProvidersManager;
import org.orekit.models.earth.Geoid;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.models.earth.troposphere.ViennaACoefficients;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

/** The Global Pressure and Temperature 2 (GPT2) model.
 * This model is an empirical model that provides the temperature, the pressure and the water vapor pressure
 * of a site depending its latitude and  longitude. This model also provides the a<sub>h</sub>
 * and a<sub>w</sub> coefficients used for the {@link
 * org.orekit.models.earth.troposphere.ViennaOneModel Vienna 1} model.
 * <p>
 * The requisite coefficients for the computation of the weather parameters are provided by the
 * Department of Geodesy and Geoinformation of the Vienna University. They are based on an
 * external grid file like "gpt2_1.grd" (1° x 1°) or "gpt2_5.grd" (5° x 5°) available at:
 * <a href="http://vmf.geo.tuwien.ac.at/codes/"> link</a>
 * </p>
 * <p>
 * A bilinear interpolation is performed in order to obtained the correct values of the weather parameters.
 * </p>
 * <p>
 * The format is always the same, with and example shown below for the pressure and the temperature.
 * <p>
 * Example:
 * </p>
 * <pre>
 * %  lat    lon   p:a0    A1   B1   A2   B2  T:a0    A1   B1   A2   B2
 *   87.5    2.5 101421    21  409 -217 -122 259.2 -13.2 -6.1  2.6  0.3
 *   87.5    7.5 101416    21  411 -213 -120 259.3 -13.1 -6.1  2.6  0.3
 *   87.5   12.5 101411    22  413 -209 -118 259.3 -13.1 -6.1  2.6  0.3
 *   87.5   17.5 101407    23  415 -205 -116 259.4 -13.0 -6.1  2.6  0.3
 *   ...
 * </pre>
 *
 * @see "K. Lagler, M. Schindelegger, J. Böhm, H. Krasna, T. Nilsson (2013),
 * GPT2: empirical slant delay model for radio space geodetic techniques. Geophys
 * Res Lett 40(6):1069–1073. doi:10.1002/grl.50288"
 *
 * @author Bryan Cazabonne
 * as of 12.1, replaced by {@link GlobalPressureTemperature2}
 */
@Deprecated
public class GlobalPressureTemperature2Model extends GlobalPressureTemperature2 implements WeatherModel {

    /** Default supported files name pattern. */
    public static final String DEFAULT_SUPPORTED_NAMES = "gpt2_\\d+.grd";

    /** The hydrostatic and wet a coefficients loaded. */
    private double[] coefficientsA;

    /** Geodetic site latitude, radians.*/
    private final double latitude;

    /** Geodetic site longitude, radians.*/
    private final double longitude;

    /** Temperature site, in kelvins. */
    private double temperature;

    /** Pressure site, in hPa. */
    private double pressure;

    /** water vapour pressure, in hPa. */
    private double e0;

    /**
     * Constructor with supported names given by user. This constructor uses the {@link
     * DataContext#getDefault() default data context}.
     *
     * @param supportedNames supported names (files with extra columns like GPT2w or GPT3 can be used here)
     * @param latitude geodetic latitude of the station, in radians
     * @param longitude longitude geodetic longitude of the station, in radians
     * @param geoid level surface of the gravity potential of a body (ignored since 12.1)
     * @see #GlobalPressureTemperature2Model(String, double, double, Geoid,
     * DataProvidersManager, TimeScale)
     */
    @DefaultDataContext
    public GlobalPressureTemperature2Model(final String supportedNames, final double latitude,
                                           final double longitude, final Geoid geoid) {
        this(supportedNames, latitude, longitude, geoid,
             DataContext.getDefault().getDataProvidersManager(),
             DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor with supported names and source of GPT2 auxiliary data given by user.
     *
     * @param supportedNames supported names (files with extra columns like GPT2w or GPT3 can be used here)
     * @param latitude geodetic latitude of the station, in radians
     * @param longitude longitude geodetic longitude of the station, in radians
     * @param ignoredGeoid level surface of the gravity potential of a body (ignored since 12.1)
     * @param dataProvidersManager provides access to auxiliary data.
     * @param utc UTC time scale.
     * @since 10.1
     */
    public GlobalPressureTemperature2Model(final String supportedNames,
                                           final double latitude,
                                           final double longitude,
                                           final Geoid ignoredGeoid,
                                           final DataProvidersManager dataProvidersManager,
                                           final TimeScale utc) {
        super(supportedNames, dataProvidersManager, utc);
        this.coefficientsA = null;
        this.temperature   = Double.NaN;
        this.pressure      = Double.NaN;
        this.e0            = Double.NaN;
        this.latitude      = latitude;
        this.longitude     = longitude;
    }

    /**
     * Constructor with default supported names. This constructor uses the {@link
     * DataContext#getDefault() default data context}.
     *
     * @param latitude geodetic latitude of the station, in radians
     * @param longitude geodetic latitude of the station, in radians
     * @param geoid level surface of the gravity potential of a body (ignored since 12.1)
     * @see #GlobalPressureTemperature2Model(String, double, double, Geoid,
     * DataProvidersManager, TimeScale)
     */
    @DefaultDataContext
    public GlobalPressureTemperature2Model(final double latitude, final double longitude, final Geoid geoid) {
        this(DEFAULT_SUPPORTED_NAMES, latitude, longitude, geoid);
    }

    /** Returns the a coefficients array.
     * <ul>
     * <li>double[0] = a<sub>h</sub>
     * <li>double[1] = a<sub>w</sub>
     * </ul>
     * @return the a coefficients array
     */
    public double[] getA() {
        return coefficientsA.clone();
    }

    /** Returns the temperature at the station [K].
     * @return the temperature at the station [K]
     */
    public double getTemperature() {
        return temperature;
    }

    /** Returns the pressure at the station [hPa].
     * @return the pressure at the station [hPa]
     */
    public double getPressure() {
        return pressure;
    }

    /** Returns the water vapor pressure at the station [hPa].
     * @return the water vapor pressure at the station [hPa]
     */
    public double getWaterVaporPressure() {
        return e0;
    }

    @Override
    public void weatherParameters(final double stationHeight, final AbsoluteDate currentDate) {

        final GeodeticPoint location = new GeodeticPoint(latitude, longitude, stationHeight);

        // ah and aw coefficients
        final ViennaACoefficients aC = getA(location, currentDate);
        coefficientsA = new double[] {
            aC.getAh(), aC.getAw()
        };

        // Pressure, temperature, humidity
        final PressureTemperatureHumidity pth = getWeatherParamerers(location, currentDate);
        this.temperature = pth.getTemperature();
        this.pressure    = TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getPressure());
        this.e0          = TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getWaterVaporPressure());

    }

}
