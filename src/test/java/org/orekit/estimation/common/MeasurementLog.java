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

package org.orekit.estimation.common;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;

/** Local class for measurement-specific log.
 * @param T type of mesurement
 * @author Luc Maisonobe
 */
abstract class MeasurementLog<T extends ObservedMeasurement<T>> {

    /** Residuals. */
    private final SortedSet<EstimatedMeasurement<T>> evaluations;

    /** Simple constructor.
     */
    MeasurementLog() {
        this.evaluations = new TreeSet<EstimatedMeasurement<T>>(Comparator.naturalOrder());
    }

    /** Compute residual value.
     * @param evaluation evaluation to consider
     * @return residual value
     */
    abstract double residual(EstimatedMeasurement<T> evaluation);

    /** Add an evaluation.
     * @param evaluation evaluation to add
     */
    void add(final EstimatedMeasurement<T> evaluation) {
        evaluations.add(evaluation);
    }

    /**Create a  statistics summary
     */
    StreamingStatistics createStatisticsSummary() {

        if (evaluations.isEmpty()) {
            return null;
        }

        // compute statistics
        final StreamingStatistics stats = new StreamingStatistics();
        for (final EstimatedMeasurement<T> evaluation : evaluations) {
            stats.addValue(residual(evaluation));
        }
        return stats;

    }

}
