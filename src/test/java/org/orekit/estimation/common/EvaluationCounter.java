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

import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;

/** Evaluation counting.
 * @param T type of mesurement
 * @author Luc Maisonobe
 */
class EvaluationCounter<T extends ObservedMeasurement<T>> implements EvaluationLogger<T> {

    /** Total number of measurements. */
    private int total;

    /** Number of active (i.e. positive weight) measurements. */
    private int active;

    /** {@inheritDoc} */
    @Override
    public void log(final EstimatedMeasurement<T> evaluation) {
        ++total;
        if (evaluation.getStatus() == EstimatedMeasurement.Status.PROCESSED) {
            ++active;
        }
    }

    /** Format an active/total count.
     * @param size field minimum size
     * @return formatted count
     */
    public String format(final int size) {
        final StringBuilder builder = new StringBuilder();
        builder.append(active);
        builder.append('/');
        builder.append(total);
        while (builder.length() < size) {
            if (builder.length() % 2 == 0) {
                builder.insert(0, ' ');
            } else {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

}
