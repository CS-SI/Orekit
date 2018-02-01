/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.estimation.sequential;

import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.sequential.KalmanEstimator.KalmanEvaluation;
import org.orekit.orbits.Orbit;
import org.orekit.utils.ParameterDriversList;

/** Observer for {@link KalmanEstimatorReal batch least squares estimator} iterations.
 * <p>
 * This interface is intended to be implemented by users to monitor
 * the progress of the Kalman filter estimator during estimation.
 * </p>
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 9.2
 */
public interface KalmanObserver {

    /** Notification callback for the end of each evaluation.
     * @param predictedOrbits current orbits predicted by the filter
     * @param estimatedOrbits current orbits estimated by the filter
     * @param estimatedOrbitalParameters estimated orbital parameters
     * @param estimatedPropagationParameters estimated propagation parameters
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param predictedMeasurement current predicted measurement in the filter
     * @param estimatedMeasurement current estimated measurement in the filter
     * @param kalmanEvaluation current evaluation of the filter
     * @exception OrekitException if some problem occurs (for example evaluationProviders not
     * being able to provide an evaluation)
     */
    void evaluationPerformed(Orbit[] predictedOrbits,
                             Orbit[] estimatedOrbits,
                             ParameterDriversList estimatedOrbitalParameters,
                             ParameterDriversList estimatedPropagationParameters,
                             ParameterDriversList estimatedMeasurementsParameters,
                             EstimatedMeasurement<?>  predictedMeasurement,
                             EstimatedMeasurement<?>  estimatedMeasurement,
                             KalmanEvaluation kalmanEvaluation)
        throws OrekitException;

}
