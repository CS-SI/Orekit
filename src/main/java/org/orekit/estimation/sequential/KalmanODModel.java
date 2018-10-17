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

import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.filtering.kalman.extended.NonLinearProcess;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;

public interface KalmanODModel extends KalmanEstimation, NonLinearProcess<MeasurementDecorator> {

    /** Get the current corrected estimate.
     * @return current corrected estimate
     */
    ProcessEstimate getEstimate();

    /** Get the propagators estimated with the values set in the propagators builders.
     * @return propagators based on the current values in the builder
     * @throws OrekitException if propagators cannot be build
     */
    AbstractIntegratedPropagator[] getEstimatedPropagators();

    /** Finalize estimation.
     * @param observedMeasurement measurement that has just been processed
     * @param estimate corrected estimate
     */
    void finalizeEstimation(ObservedMeasurement<?> observedMeasurement,
                            ProcessEstimate estimate);
}
