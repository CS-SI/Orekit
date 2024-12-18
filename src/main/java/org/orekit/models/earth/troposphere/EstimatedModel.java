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
package org.orekit.models.earth.troposphere;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.ConstantPressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** An estimated tropospheric model. The tropospheric delay is computed according to the formula:
 * <p>
 * δ = δ<sub>h</sub> * m<sub>h</sub> + (δ<sub>t</sub> - δ<sub>h</sub>) * m<sub>w</sub>
 * <p>
 * With:
 * <ul>
 * <li>δ<sub>h</sub>: Tropospheric zenith hydro-static delay.</li>
 * <li>δ<sub>t</sub>: Tropospheric total zenith delay.</li>
 * <li>m<sub>h</sub>: Hydro-static mapping function.</li>
 * <li>m<sub>w</sub>: Wet mapping function.</li>
 * </ul>
 * <p>
 * The mapping functions m<sub>h</sub>(e) and m<sub>w</sub>(e) are
 * computed thanks to a {@link #model} initialized by the user.
 * The user has the possibility to use several mapping function models for the computations:
 * the {@link GlobalMappingFunctionModel Global Mapping Function}, or
 * the {@link NiellMappingFunctionModel Niell Mapping Function}
 * </p> <p>
 * The tropospheric zenith delay δ<sub>h</sub> is computed empirically with a
 * {@link TroposphericModel tropospheric model}
 * while the tropospheric total zenith delay δ<sub>t</sub> is estimated as a {@link ParameterDriver},
 * hence the wet part is the difference between the two.
 * @since 12.1
 */
public class EstimatedModel implements TroposphericModel {

    /** Name of the parameter of this model: the total zenith delay. */
    public static final String TOTAL_ZENITH_DELAY = "total zenith delay";

    /** Mapping Function model. */
    private final TroposphereMappingFunction model;

    /** Driver for the tropospheric zenith total delay.*/
    private final ParameterDriver totalZenithDelay;

    /** Model for hydrostatic component. */
    private final TroposphericModel hydrostatic;

    /** Build a new instance using the given environmental conditions.
     * <p>
     * This constructor uses a {@link ModifiedSaastamoinenModel} for the hydrostatic contribution.
     * </p>
     * @param h0 altitude of the station [m]
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param model mapping function model.
     * @param totalDelay initial value for the tropospheric zenith total delay [m]
     */
    @DefaultDataContext
    public EstimatedModel(final double h0, final double t0, final double p0,
                          final TroposphereMappingFunction model, final double totalDelay) {
        this(new ModifiedSaastamoinenModel(new ConstantPressureTemperatureHumidityProvider(new PressureTemperatureHumidity(h0,
                                                                                                                           TroposphericModelUtils.HECTO_PASCAL.toSI(p0),
                                                                                                                           t0,
                                                                                                                           0.0,
                                                                                                                           Double.NaN,
                                                                                                                           Double.NaN))),
             model, totalDelay);
    }

    /** Build a new instance using the given environmental conditions.
     * @param hydrostatic model for hydrostatic component
     * @param model mapping function model.
     * @param totalDelay initial value for the tropospheric zenith total delay [m]
     * @since 12.1
     */
    public EstimatedModel(final TroposphericModel hydrostatic,
                          final TroposphereMappingFunction model,
                          final double totalDelay) {

        totalZenithDelay = new ParameterDriver(EstimatedModel.TOTAL_ZENITH_DELAY,
                                               totalDelay, FastMath.scalb(1.0, 0), 0.0, Double.POSITIVE_INFINITY);

        this.hydrostatic = hydrostatic;
        this.model = model;
    }

    /** Build a new instance using a standard atmosphere model.
     * <ul>
     * <li>altitude: 0m
     * <li>temperature: 18 degree Celsius
     * <li>pressure: 1013.25 mbar
     * </ul>
     * @param model mapping function model.
     * @param totalDelay initial value for the tropospheric zenith total delay [m]
     */
    @DefaultDataContext
    public EstimatedModel(final TroposphereMappingFunction model, final double totalDelay) {
        this(0.0, 273.15 + 18.0, 1013.25, model, totalDelay);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(totalZenithDelay);
    }

    /** {@inheritDoc} */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates,
                                       final GeodeticPoint point,
                                       final PressureTemperatureHumidity weather,
                                       final double[] parameters, final AbsoluteDate date) {

        // zenith hydrostatic delay
        final double zd = hydrostatic.pathDelay(trackingCoordinates, point, weather, parameters, date).getZh();

        // zenith wet delay
        final double wd = parameters[0] - zd;

        // mapping functions
        final double[] mf = model.mappingFactors(trackingCoordinates, point, weather, date);

        // composite delay
        return new TroposphericDelay(zd, wd, mf[0] * zd, mf[1] * wd);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final FieldPressureTemperatureHumidity<T> weather,
                                                                                   final T[] parameters, final FieldAbsoluteDate<T> date) {

        // zenith hydrostatic delay
        final T zd = hydrostatic.pathDelay(trackingCoordinates, point, weather, parameters, date).getZh();

        // zenith wet delay
        final T wd = parameters[0].subtract(zd);

        // mapping functions
        final T[] mf = model.mappingFactors(trackingCoordinates, point, weather, date);

        // composite delay
        return new FieldTroposphericDelay<>(zd, wd, mf[0].multiply(zd), mf[1].multiply(wd));

    }

}
