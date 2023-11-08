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
package org.orekit.estimation.measurements.generation;

/** Enumerate for the semantic of the {@code g} function sign during measurements generation.
 * @see EventBasedScheduler
 * @author Luc Maisonobe
 * @since 9.3
 */
public enum SignSemantic {

    /** Semantic for detectors that have positive g function when measurements are feasible. */
    FEASIBLE_MEASUREMENT_WHEN_POSITIVE(+1.0),

    /** Semantic for detectors that have negative g function when measurements are feasible. */
    FEASIBLE_MEASUREMENT_WHEN_NEGATIVE(-1.0);

    /** Reference sign. */
    private final double sign;

    /** Simple constructor.
     * @param sign reference sign
     */
    SignSemantic(final double sign) {
        this.sign = sign;
    }

    /** Check if measurement is feasible.
     * @param g value of the detector g function
     * @return true if measurement is feasible
     */
    public boolean measurementIsFeasible(final double g) {
        return sign * g >= 0;
    }

}
