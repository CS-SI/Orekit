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
package org.orekit.estimation.sequential;

import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;

/**
 * Interface for both {@link SemiAnalyticalUnscentedKalmanModel} and {@link SemiAnalyticalKalmanModel}.
 * @author Gaëtan Pierre
 * @since 11.3
 */
public interface SemiAnalyticalProcess {

    /** Get the observer for Kalman Filter estimations.
     * @return the observer for Kalman Filter estimations
     */
    KalmanObserver getObserver();

    /** Initialize the short periodic terms for the Kalman Filter.
     * @param meanState mean state for auxiliary elements
     */
    void initializeShortPeriodicTerms(SpacecraftState meanState);

    /**
     * Update the DSST short periodic terms.
     * @param state current mean state
     */
    void updateShortPeriods(SpacecraftState state);

    /**
     * Update the nominal spacecraft state.
     * @param nominal nominal spacecraft state
     */
    void updateNominalSpacecraftState(SpacecraftState nominal);

    /**
     * Finalize estimation.
     * @param observedMeasurement measurement that has just been processed
     * @param estimate corrected estimate
     */
    void finalizeEstimation(ObservedMeasurement<?> observedMeasurement, ProcessEstimate estimate);

    /** Finalize estimation operations on the observation grid. */
    void finalizeOperationsObservationGrid();

}
