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

package org.orekit.estimation.common;

import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/** Local class for measurement-specific log.
 * @param T type of mesurement
 * @author Luc Maisonobe
 */
abstract class MeasurementLog<T extends ObservedMeasurement<T>> implements EvaluationLogger<T> {

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

    /** {@inheritDoc} */
    @Override
    public void log(final EstimatedMeasurement<T> evaluation) {
        evaluations.add(evaluation);
    }

    /** Create a  statistics summary
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

    /** Display summary statistics in the general log file.
     * @param logStream log stream
     */
    public void displaySummary(final PrintStream logStream) {
        if (!evaluations.isEmpty()) {

            // Compute statistics
            final StreamingStatistics stats = createStatisticsSummary();

            // Display statistics
            final String name = evaluations.first().getObservedMeasurement().getClass().getSimpleName();
            logStream.println("Measurements type: " + name);
            logStream.println("   number of measurements: " + stats.getN() + "/" + evaluations.size());
            logStream.println("   residuals min  value  : " + stats.getMin());
            logStream.println("   residuals max  value  : " + stats.getMax());
            logStream.println("   residuals mean value  : " + stats.getMean());
            logStream.println("   residuals σ           : " + stats.getStandardDeviation());
            logStream.println("   residuals median      : " + stats.getMedian());

        }
    }

}
