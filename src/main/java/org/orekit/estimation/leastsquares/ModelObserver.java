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
package org.orekit.estimation.leastsquares;

import java.util.Map;

import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;

/** Observer for {@link BatchLSModel model} calls.
 * <p>
 * This interface is an internal one intended to pass the orbit
 * back from {@link BatchLSModel model} to {@link BatchLSEstimator estimator}.
 * </p>
 * @author Luc Maisonobe
 * @since 8.0
 */
public interface ModelObserver {

    /** Notification callback for orbit changes.
     * @param orbits current estimated orbits
     * @param estimations map of measurements estimations resulting from
     * the current estimated orbit (this is an unmodifiable view of the
     * current estimations, its content is changed at each iteration)
     */
    void modelCalled(Orbit[] orbits, Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> estimations);

}
