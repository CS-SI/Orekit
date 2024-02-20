/* Copyright 2002-2024 Thales Alenia Space
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

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** Adapter between {@link DiscreteTroposphericModel} and {@link TroposphericModel}.
 * <p>
 * This class is a temporary adapter, it will be removed when
 * {@link DiscreteTroposphericModel} is removed.
 * </p>
 * @author Luc Maisonobe
 * @since 12.1
 * @deprecated temporary adapter to be removed when {@link DiscreteTroposphericModel} is removed
 */
@Deprecated
public class TroposphericModelAdapter implements TroposphericModel {

    /** Underlying model. */
    private final DiscreteTroposphericModel model;

    /** Simple constructor.
     * @param model underlying model
     */
    public TroposphericModelAdapter(final DiscreteTroposphericModel model) {
        this.model = model;
    }

    /** {@inheritDoc}
     * <p>
     * All delays are affected to {@link TroposphericDelay#getZh() hydrostatic zenith}
     * and {@link TroposphericDelay#getSh() hydrostatic slanted} delays, the wet delays
     * are arbitrarily set to 0.
     * </p>
     */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates,
                                       final GeodeticPoint point,
                                       final PressureTemperatureHumidity weather,
                                       final double[] parameters,
                                       final AbsoluteDate date) {
        return new TroposphericDelay(model.pathDelay(MathUtils.SEMI_PI,
                                                     point, parameters, date),
                                     0.0,
                                     model.pathDelay(trackingCoordinates.getElevation(),
                                                     point, parameters, date),
                                     0.0);
    }

    /** {@inheritDoc}
     * <p>
     * All delays are affected to {@link FieldTroposphericDelay#getZh() hydrostatic zenith}
     * and {@link FieldTroposphericDelay#getSh() hydrostatic slanted} delays, the wet delays
     * are arbitrarily set to 0.
     * </p>
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final FieldPressureTemperatureHumidity<T> weather,
                                                                                   final T[] parameters,
                                                                                   final FieldAbsoluteDate<T> date) {
        return new FieldTroposphericDelay<>(model.pathDelay(date.getField().getZero().newInstance(MathUtils.SEMI_PI),
                                                            point, parameters, date),
                                            date.getField().getZero(),
                                            model.pathDelay(trackingCoordinates.getElevation(),
                                                            point, parameters, date),
                                            date.getField().getZero());
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return model.getParametersDrivers();
    }

}
