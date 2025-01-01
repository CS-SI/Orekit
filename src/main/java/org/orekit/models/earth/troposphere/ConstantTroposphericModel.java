/* Copyright 2022-2025 Thales Alenia Space
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
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** Defines a constant tropospheric model.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class ConstantTroposphericModel implements TroposphericModel {

    /** Constant delay. */
    private final TroposphericDelay delay;

    /** Simple constructor.
     * @param delay constant delay
     */
    public ConstantTroposphericModel(final TroposphericDelay delay) {
        this.delay = delay;
    }

    /** {@inheritDoc} */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates,
                                       final GeodeticPoint point,
                                       final PressureTemperatureHumidity weather,
                                       final double[] parameters,
                                       final AbsoluteDate date) {
        return delay;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final FieldPressureTemperatureHumidity<T> weather,
                                                                                   final T[] parameters,
                                                                                   final FieldAbsoluteDate<T> date) {
        final T zero = date.getField().getZero();
        return new FieldTroposphericDelay<>(zero.newInstance(delay.getZh()),
                                            zero.newInstance(delay.getZw()),
                                            zero.newInstance(delay.getSh()),
                                            zero.newInstance(delay.getSw()));
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

}
