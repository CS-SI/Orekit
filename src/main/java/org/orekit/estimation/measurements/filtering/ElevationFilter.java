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
package org.orekit.estimation.measurements.filtering;

import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;

/**
 * Elevation pre-processing filter.
 * @param <T> the type of the measurement
 * @author Bryan Cazabonne
 * @author David Soulard
 * @since 10.2
 */
public class ElevationFilter<T extends ObservedMeasurement<T>> implements MeasurementFilter<T> {

    /** Elevation threshold under which the measurement will be rejected [rad]. */
    private final double threshold;

    /** Ground station considered. */
    private final GroundStation station;

    /**
     * Constructor.
     * @param station considered by the filter
     * @param threshold minimum elevation for a measurements to be accepted, in radians
     */
    public ElevationFilter(final GroundStation station, final double threshold) {
        this.station   = station;
        this.threshold = threshold;
    }

    /** {@inheritDoc} */
    @Override
    public void filter(final ObservedMeasurement<T> measurement, final SpacecraftState state) {
        // Current elevation of the satellite
        final double trueElevation = station.getBaseFrame().
                                     getTrackingCoordinates(state.getPosition(), state.getFrame(), state.getDate()).
                                     getElevation();
        // Filter
        if (trueElevation < threshold) {
            measurement.setEnabled(false);
        }
    }
}
