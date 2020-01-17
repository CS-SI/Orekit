/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.nonlinear.vector.leastsquares.MultivariateJacobianFunction;
import org.hipparchus.util.Incrementor;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.utils.ParameterDriversList;

/** Interface for models used in the batch least squares orbit determination process.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public interface BatchLSODModel extends MultivariateJacobianFunction {

    /** Get the selected propagation drivers for a propagatorBuilder.
     * @param iBuilder index of the builder in the builders' array
     * @return the list of selected propagation drivers for propagatorBuilder of index iBuilder
     */
    ParameterDriversList getSelectedPropagationDriversForBuilder(int iBuilder);

    /** Create the propagators and parameters corresponding to an evaluation point.
     * @param point evaluation point
     * @return an array of new propagators
     */
    AbstractIntegratedPropagator[] createPropagators(RealVector point);

    /** Fetch a measurement that was evaluated during propagation.
     * @param index index of the measurement first component
     * @param evaluation measurement evaluation
     */
    void fetchEvaluatedMeasurement(int index, EstimatedMeasurement<?> evaluation);

    /** Set the counter for evaluations.
     * @param evaluationsCounter counter for evaluations
     */
    void setEvaluationsCounter(Incrementor evaluationsCounter);

    /** Set the counter for iterations.
     * @param iterationsCounter counter for iterations
     */
    void setIterationsCounter(Incrementor iterationsCounter);

    /** Get the iterations count.
     * @return iterations count
     */
    int getIterationsCount();

    /** Get the evaluations count.
     * @return evaluations count
     */
    int getEvaluationsCount();

    /** Return the forward propagation flag.
     * @return the forward propagation flag
     */
    boolean isForwardPropagation();
}
