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
package org.orekit.models.earth.troposphere;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** The Vienna tropospheric delay model for radio techniques.
 * @since 12.1
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 */
public abstract class AbstractVienna implements TroposphericModel, TroposphereMappingFunction {

    /** Provider for a<sub>h</sub> and a<sub>w</sub> coefficients. */
    private final ViennaAProvider aProvider;

    /** Provider for zenith delays. */
    private final TroposphericModel zenithDelayProvider;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Build a new instance.
     * @param aProvider provider for a<sub>h</sub> and a<sub>w</sub> coefficients
     * @param zenithDelayProvider provider for zenith delays
     * @param utc                 UTC time scale
     */
    protected AbstractVienna(final ViennaAProvider aProvider,
                             final TroposphericModel zenithDelayProvider,
                             final TimeScale utc) {
        this.aProvider           = aProvider;
        this.zenithDelayProvider = zenithDelayProvider;
        this.utc                 = utc;
    }

    /** {@inheritDoc} */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates,
                                       final GeodeticPoint point,
                                       final PressureTemperatureHumidity weather,
                                       final double[] parameters, final AbsoluteDate date) {
        // zenith delay
        final TroposphericDelay delays =
                        zenithDelayProvider.pathDelay(trackingCoordinates, point, weather, parameters, date);

        // mapping function
        final double[] mappingFunction =
                        mappingFactors(trackingCoordinates, point, weather, date);

        // Tropospheric path delay
        return new TroposphericDelay(delays.getZh(),
                                     delays.getZw(),
                                     delays.getZh() * mappingFunction[0],
                                     delays.getZw() * mappingFunction[1]);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final FieldPressureTemperatureHumidity<T> weather,
                                                                                   final T[] parameters, final FieldAbsoluteDate<T> date) {
        // zenith delay
        final FieldTroposphericDelay<T> delays =
                        zenithDelayProvider.pathDelay(trackingCoordinates, point, weather, parameters, date);

        // mapping function
        final T[] mappingFunction =
                        mappingFactors(trackingCoordinates, point, weather, date);

        // Tropospheric path delay
        return new FieldTroposphericDelay<>(delays.getZh(),
                                            delays.getZw(),
                                            delays.getZh().multiply(mappingFunction[0]),
                                            delays.getZw().multiply(mappingFunction[1]));

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** Get provider for Vienna a<sub>h</sub> and a<sub>w</sub> coefficients.
     * @return provider for Vienna a<sub>h</sub> and a<sub>w</sub> coefficients
     */
    protected ViennaAProvider getAProvider() {
        return aProvider;
    }

    /** Get day of year.
     * @param date date
     * @return day of year
     */
    protected int getDayOfYear(final AbsoluteDate date) {
        return date.getComponents(utc).getDate().getDayOfYear();
    }

}
