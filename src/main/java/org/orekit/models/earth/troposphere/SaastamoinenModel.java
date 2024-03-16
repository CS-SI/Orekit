/* Copyright 2002-2024 Thales Alenia Space
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
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.models.earth.weather.ConstantPressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.water.Wang1988;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

/** The modified Saastamoinen model.
 * @author Luc Maisonobe
 * @deprecated as of 12.1, replaced by {@link ModifiedSaastamoinenModel}
 */
@Deprecated
public class SaastamoinenModel extends ModifiedSaastamoinenModel implements DiscreteTroposphericModel {

    /** Default file name for δR correction term table. */
    public static final String DELTA_R_FILE_NAME = ModifiedSaastamoinenModel.DELTA_R_FILE_NAME;

    /** Default lowest acceptable elevation angle [rad]. */
    public static final double DEFAULT_LOW_ELEVATION_THRESHOLD = ModifiedSaastamoinenModel.DEFAULT_LOW_ELEVATION_THRESHOLD;

    /**
     * Create a new Saastamoinen model for the troposphere using the given environmental
     * conditions and table from the reference book.
     *
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [fraction] (50% → 0.5)
     * @see ModifiedSaastamoinenModel#ModifiedSaastamoinenModel(PressureTemperatureHumidityProvider, String, DataProvidersManager)
     * @since 10.1
     */
    @DefaultDataContext
    public SaastamoinenModel(final double t0, final double p0, final double r0) {
        this(t0, p0, r0, DELTA_R_FILE_NAME);
    }

    /** Create a new Saastamoinen model for the troposphere using the given
     * environmental conditions. This constructor uses the {@link DataContext#getDefault()
     * default data context} if {@code deltaRFileName != null}.
     *
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [fraction] (50% → 0.5)
     * @param deltaRFileName regular expression for filename containing δR
     * correction term table (typically {@link #DELTA_R_FILE_NAME}), if null
     * default values from the reference book are used
     * @since 7.1
     * @see ModifiedSaastamoinenModel#ModifiedSaastamoinenModel(PressureTemperatureHumidityProvider, String, DataProvidersManager)
     */
    @DefaultDataContext
    public SaastamoinenModel(final double t0, final double p0, final double r0,
                             final String deltaRFileName) {
        this(t0, p0, r0, deltaRFileName, DataContext.getDefault().getDataProvidersManager());
    }

    /** Create a new Saastamoinen model for the troposphere using the given
     * environmental conditions. This constructor allows the user to specify the source of
     * of the δR file.
     *
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [fraction] (50% → 0.5)
     * @param deltaRFileName regular expression for filename containing δR
     * correction term table (typically {@link #DELTA_R_FILE_NAME}), if null
     * default values from the reference book are used
     * @param dataProvidersManager provides access to auxiliary data.
     * @since 10.1
     */
    public SaastamoinenModel(final double t0,
                             final double p0,
                             final double r0,
                             final String deltaRFileName,
                             final DataProvidersManager dataProvidersManager) {
        super(new ConstantPressureTemperatureHumidityProvider(new PressureTemperatureHumidity(0.0,
                                                                                              TroposphericModelUtils.HECTO_PASCAL.toSI(p0),
                                                                                              t0,
                                                                                              new Wang1988().
                                                                                              waterVaporPressure(TroposphericModelUtils.HECTO_PASCAL.toSI(p0),
                                                                                                                 t0,
                                                                                                                 r0),
                                                                                              Double.NaN,
                                                                                              Double.NaN)),
              deltaRFileName, dataProvidersManager);
    }

    /** Create a new Saastamoinen model using a standard atmosphere model.
    *
    * <ul>
    * <li>altitude: 0m</li>
    * <li>temperature: 18 degree Celsius
    * <li>pressure: 1013.25 mbar
    * <li>humidity: 50%
    * </ul>
    *
    * @return a Saastamoinen model with standard environmental values
    */
    @DefaultDataContext
    public static SaastamoinenModel getStandardModel() {
        return new SaastamoinenModel(273.16 + 18, 1013.25, 0.5);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public double pathDelay(final double elevation, final GeodeticPoint point,
                            final double[] parameters, final AbsoluteDate date) {
        return pathDelay(new TrackingCoordinates(0.0, elevation, 0.0), point,
                         getPth0Provider().getWeatherParamerers(point, date), parameters, date).getDelay();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation,
                                                           final FieldGeodeticPoint<T> point,
                                                           final T[] parameters,
                                                           final FieldAbsoluteDate<T> date) {
        return pathDelay(new FieldTrackingCoordinates<>(date.getField().getZero(), elevation, date.getField().getZero()),
                         point,
                         getPth0Provider().getWeatherParamerers(point, date),
                         parameters, date).getDelay();
    }

}
