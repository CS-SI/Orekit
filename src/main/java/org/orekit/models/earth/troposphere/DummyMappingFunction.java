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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

/** Dummy mapping function.
 * <p>
 * This mapping function just uses 1.0 as constant mapping factors, which
 * implies the slanted tropospheric delays are equal to the zenith delays.
 * This is mainly useful when only zenith delays are needed.
 * </p>
 * @author Luc Maisonobe
 * @since 12.1
 */
public class DummyMappingFunction implements TroposphereMappingFunction {

    /** Builds a new instance.
     */
    protected DummyMappingFunction() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                   final PressureTemperatureHumidity weather,
                                   final AbsoluteDate date) {
        return new double[] {
            1.0, 1.0
        };
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] mappingFactors(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                  final FieldGeodeticPoint<T> point,
                                                                  final FieldPressureTemperatureHumidity<T> weather,
                                                                  final FieldAbsoluteDate<T> date) {
        final T[] mapping = MathArrays.buildArray(date.getField(), 2);
        mapping[0] = date.getField().getOne();
        mapping[1] = date.getField().getOne();
        return mapping;
    }

}
