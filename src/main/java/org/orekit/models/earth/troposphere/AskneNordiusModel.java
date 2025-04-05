/* Copyright 2022-2025 Thales Alenia Space
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
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** The Askne Nordius model.
 * <p>
 * The hydrostatic part is equivalent to Saastamoinen, whereas the wet part takes
 * into account {@link PressureTemperatureHumidity#getTm() mean temperature weighted
 * with water vapor pressure} and {@link PressureTemperatureHumidity#getLambda() water
 * vapor decrease factor}.
 * </p>
 * @author Luc Maisonobe
 * @see "J. Askne and H. Nordius, Estimation of tropospheric delay for microwaves
 *      from surface weather data, Radio Science, volume 22, number 3, pages 379-386,
 *      May-June 1987"
 * @see "Landskron D (2017) Modeling tropospheric delays for space geodetic
 *      techniques. Dissertation, Department of Geodesy and Geoinformation, TU Wien, Supervisor: J. Böhm.
 *      http://repositum.tuwien.ac.at/urn:nbn:at:at-ubtuw:1-100249"
 * @since 12.1
 */
public class AskneNordiusModel implements TroposphericModel {

    /** Lowest acceptable elevation angle [rad]. */
    public static final double LOW_ELEVATION_THRESHOLD = 0.05;

    /** Base delay coefficient (from Saastamoninen model). */
    private static final double L0 = 2.2768e-5;

    /** Askne-Nordius coefficient k'₂. */
    private static final double K_PRIME_2 = 16.5203;

    /** Askne-Nordius coefficient k₃. */
    private static final double K_3 = 377600;

    /** Gas constant for dry components. */
    private static final double RD = 287.0464;

    /** Unit consversion factor. */
    private static final double FACTOR = 1.0e-6;

    /** Mapping function. */
    private final TroposphereMappingFunction mappingFunction;

    /** Provider for pressure, temperature and humidity.
     * @since 13.0
     */
    private final PressureTemperatureHumidityProvider pthProvider;

    /** Create a new Askne Nordius model.
     * @param mappingFunction mapping function
     * @param pthProvider provider for pressure, temperature and humidity
     */
    public AskneNordiusModel(final TroposphereMappingFunction mappingFunction,
                             final PressureTemperatureHumidityProvider pthProvider) {
        this.mappingFunction = mappingFunction;
        this.pthProvider     = pthProvider;
    }

    /** {@inheritDoc} */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                       final double[] parameters, final AbsoluteDate date) {

        final double[] mf = mappingFunction.mappingFactors(trackingCoordinates, point, date);

        // compute weather parameters
        final PressureTemperatureHumidity weather = pthProvider.getWeatherParameters(point, date);

        // compute the path delay
        final double zh     = L0 * weather.getPressure();
        final double zw     = FACTOR * (K_PRIME_2 + K_3 / weather.getTm()) *
                              RD * weather.getWaterVaporPressure() /
                              (Constants.G0_STANDARD_GRAVITY * (weather.getLambda() + 1.0));
        final double sh     = zh * mf[0];
        final double sw     = zw * mf[1];
        return new TroposphericDelay(zh, zw, sh, sw);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final T[] parameters, final FieldAbsoluteDate<T> date) {

        final T[] mf = mappingFunction.mappingFactors(trackingCoordinates, point, date);

        // compute weather parameters
        final FieldPressureTemperatureHumidity<T> weather = pthProvider.getWeatherParameters(point, date);

        // compute the path delay
        final T zh     = weather.getPressure().multiply(L0);
        final T zw     = weather.getTm().reciprocal().multiply(K_3).add(K_PRIME_2).
                         multiply(weather.getWaterVaporPressure().multiply(RD)).
                         divide(weather.getLambda().add(1.0).multiply(Constants.G0_STANDARD_GRAVITY)).
                         multiply(FACTOR);
        final T sh     = zh.multiply(mf[0]);
        final T sw     = zw.multiply(mf[1]);
        return new FieldTroposphericDelay<>(zh, zw, sh, sw);

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

}
