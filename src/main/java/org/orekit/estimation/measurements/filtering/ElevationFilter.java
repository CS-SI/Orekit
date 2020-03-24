/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements.filtering;

import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;

public class ElevationFilter<T extends ObservedMeasurement<T>> implements MeasurementFilter<T> {

    /** Elevation threshold under which the measurement will be rejected (angle in rad). */
    private final double threshold;

    /** Ground station considered. */
    private final GroundStation station;

    /**Contructor.
     * @param station considered by the filter
     * @param threshold minimum elevation for an measurements to be accepted (rad)
     */
    public ElevationFilter(final GroundStation station, final double threshold) {
        this.station        = station;
        this.threshold      = threshold;
    }

    @Override
    public void filter(final ObservedMeasurement<T> measurement, final SpacecraftState state) {
        final double trueElevation = station.getBaseFrame().getElevation(state.getPVCoordinates().getPosition(),
                                                                         state.getFrame(), state.getDate());
        if (trueElevation < threshold) {
            measurement.setEnabled(false);
        }
    }
}
