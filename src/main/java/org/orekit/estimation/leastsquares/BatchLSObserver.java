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

import java.util.List;
import java.util.Map;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.orbits.Orbit;
import org.orekit.utils.ParameterDriver;

/** Observer for {@link BatchLSEstimator batch least squares estimator} iterations.
 * <p>
 * This interface is intended to be implemented by users to monitor
 * the progress of the estimator during estimation.
 * </p>
 * @author Luc Maisonobe
 * @since 7.2
 */
public interface BatchLSObserver {

    /** Notification callback for the end of each iteration.
     * @param iterationsCount iterations count
     * @param evaluationscount evaluations count
     * @param orbit current estimated orbit
     * @param estimatedPropagatorParameters estimated propagator parameters
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param evaluations map of measurements evaluations resulting from
     * the current estimated orbit (this is an unmodifiable view of the
     * current evaluations, its content is changed at each iteration)
     * @param lspEvaluation current evaluation of the underlying {@link LeastSquaresProblem
     * least squares problem}
     */
    void iterationPerformed(int iterationsCount, int evaluationscount, Orbit orbit,
                            List<ParameterDriver> estimatedPropagatorParameters,
                            List<ParameterDriver> estimatedMeasurementsParameters,
                            Map<Measurement<?>, Evaluation<?>> evaluations,
                            LeastSquaresProblem.Evaluation lspEvaluation);

}
