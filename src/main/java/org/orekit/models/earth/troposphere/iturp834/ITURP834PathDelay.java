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
package org.orekit.models.earth.troposphere.iturp834;

import org.hipparchus.CalculusFieldElement;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.ITURP834AtmosphericRefraction;
import org.orekit.models.earth.troposphere.FieldTroposphericDelay;
import org.orekit.models.earth.troposphere.TroposphereMappingFunction;
import org.orekit.models.earth.troposphere.TroposphericDelay;
import org.orekit.models.earth.troposphere.TroposphericModel;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;
import org.orekit.utils.units.Unit;

import java.util.Collections;
import java.util.List;

/** The ITU-R P.834 tropospheric model.
 * <p>
 * This class implements the excess radio path length part of the model,
 * i.e. section 6 of the recommendation. The ray bending part of the model,
 * i.e. section 1 of the recommendation, is implemented in the
 * {@link ITURP834AtmosphericRefraction} class.
 * </p>
 * @see ITURP834WeatherParametersProvider
 * @see ITURP834MappingFunction
 * @author Luc Maisonobe
 * @see <a href="https://www.itu.int/rec/R-REC-P.834/en">P.834 : Effects of tropospheric refraction on radiowave propagation</a>
 * @since 13.0
 */
public class ITURP834PathDelay implements TroposphericModel {

    /** Molar gas constant (J/mol K). */
    private static final double R = 8.314;

    /** Dry air molar mass (kg/mol). */
    private static final double MD = Unit.GRAM.toSI(28.9644);

    /** Kelvin per hecto-Pascal. */
    private static final Unit K_PER_HPA = Unit.parse("hPa⁻¹");

    /** Hydrostatic factor (K/Pa). */
    private static final double K1 = K_PER_HPA.toSI(76.604);

    /** Wet factor (K²/Pa). */
    private static final double K2 = K_PER_HPA.toSI(373900);

    /** Provider for pressure, temperature and humidity. */
    private final PressureTemperatureHumidityProvider pthProvider;

    /** Mapping function. */
    private final TroposphereMappingFunction mappingFunction;

    /** Simple constructor.
     * @param pthProvider provider for pressure, temperature and humidity
     * @param utc UTC time scale
     */
    public ITURP834PathDelay(final PressureTemperatureHumidityProvider pthProvider,
                             final TimeScale utc) {
        this.pthProvider     = pthProvider;
        this.mappingFunction = new ITURP834MappingFunction(utc);
    }

    /** {@inheritDoc} */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                       final double[] parameters, final AbsoluteDate date) {

        // compute weather parameters
        final PressureTemperatureHumidity weather = pthProvider.getWeatherParameters(point, date);

        // calculate path delay
        final double gm       = Gravity.getGravityAtAltitude(point).evaluate();
        final double deltaLvh = 1.0e-6 * R * K1 * weather.getPressure() / (MD * gm);
        final double deltaLvw = 1.0e-6 * R * K2 * weather.getWaterVaporPressure() /
                                (MD * gm * (1 + weather.getLambda()) * weather.getTm());

        // apply mapping function
        final double[] mapping = mappingFunction.mappingFactors(trackingCoordinates, point, date);
        return new TroposphericDelay(deltaLvh, deltaLvw,
                                     mapping[0] * deltaLvh, mapping[1] * deltaLvw);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final T[] parameters, final FieldAbsoluteDate<T> date) {

        // compute weather parameters
        final FieldPressureTemperatureHumidity<T> weather = pthProvider.getWeatherParameters(point, date);

        // calculate path delay
        final T gm       = Gravity.getGravityAtAltitude(point).evaluate();
        final T deltaLvh = weather.getPressure().multiply(1.0e-6 * R * K1).
                           divide(gm.multiply(MD));
        final T deltaLvw = weather.getWaterVaporPressure().multiply(1.0e-6 * R * K2).
                           divide(weather.getTm().multiply(weather.getLambda().add(1)).multiply(gm).multiply(MD));

        // apply mapping function
        final T[] mapping = mappingFunction.mappingFactors(trackingCoordinates, point, date);
        return new FieldTroposphericDelay<>(deltaLvh, deltaLvw,
                                            mapping[0].multiply(deltaLvh), mapping[1].multiply(deltaLvw));

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

}
