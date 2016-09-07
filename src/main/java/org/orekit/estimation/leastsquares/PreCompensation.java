/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation.leastsquares;

import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** Class holding compensation of time offset for measurements.
 * @author Luc Maisonobe
 * @since 8.1
 */
class PreCompensation implements TimeStamped {

    /** Underlying measurement. */
    private final ObservedMeasurement<?> observed;

    /** State pick-up dates, including pre-compensation. */
    private final AbsoluteDate precompensated;

    /** Simple constructor.
     * @param observed underlying measurement
     * @param previousEstimation previous estimation of the measurement (may be null)
     */
    PreCompensation(final ObservedMeasurement<?> observed,
                    final EstimatedMeasurement<?> previousEstimation) {
        this.observed = observed;
        if (previousEstimation == null) {
            // no previous estimate of transit time, we will pickup state at observed measurement date
            precompensated = observed.getDate();
        } else {
            // pre-compensate signal transit time
            precompensated = observed.getDate().shiftedBy(-previousEstimation.getTimeOffset());
        }
    }

    /** Get the observed measurement.
     * @return observed measurement
     */
    public ObservedMeasurement<?> getMeasurement() {
        return observed;
    }

    /** Get the state date, i.e. the measurement date pre-compensated
     * by time offset.
     * @return pre-compensated state date
     */
    public AbsoluteDate getDate() {
        return precompensated;
    }

}
