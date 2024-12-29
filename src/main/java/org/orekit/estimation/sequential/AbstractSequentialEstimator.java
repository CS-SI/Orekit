/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.filtering.kalman.KalmanFilter;
import org.hipparchus.filtering.kalman.ProcessEstimate;
import org.hipparchus.linear.MatrixDecomposer;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;

import java.util.List;

public abstract class AbstractSequentialEstimator extends AbstractKalmanEstimator {

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

    /** Matrix decomposer for filter. */
    private final MatrixDecomposer decomposer;

    /** Constructor.
     * @param decomposer matrix decomposer for filter
     * @param builders list of propagator builders
     */
    protected AbstractSequentialEstimator(final MatrixDecomposer decomposer,
                                          final List<? extends PropagatorBuilder> builders) {
        super(builders);

        this.referenceDate = builders.get(0).getInitialOrbitDate();
        this.decomposer = decomposer;
        this.observer = null;
    }

    /** Set the observer.
     * @param observer the observer
     */
    public void setObserver(final KalmanObserver observer) {
        this.observer = observer;
        observer.init(getKalmanEstimation());
    }

    /** Get the observer.
     * @return the observer
     */
    public KalmanObserver getObserver() {
        return observer;
    }

    /** Get the matrix decomposer.
     * @return the decomposer
     */
    protected MatrixDecomposer getMatrixDecomposer() {
        return decomposer;
    }

    /** Get the reference date.
     * @return the date
     */
    protected AbsoluteDate getReferenceDate() {
        return referenceDate;
    }

    /** Process a single measurement.
     * <p>
     * Update the filter with the new measurement by calling the estimate method.
     * </p>
     * @param observedMeasurement the measurement to process
     * @return estimated propagators
     */
    public Propagator[] estimationStep(final ObservedMeasurement<?> observedMeasurement) {
        try {
            final ProcessEstimate estimate =
                    getKalmanFilter().estimationStep(KalmanEstimatorUtil.decorate(observedMeasurement, referenceDate));
            getProcessModel().finalizeEstimation(observedMeasurement, estimate);
            if (getObserver() != null) {
                getObserver().evaluationPerformed(getKalmanEstimation());
            }
            return getProcessModel().getEstimatedPropagators();
        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        }
    }

    /** Process several measurements.
     * @param observedMeasurements the measurements to process in <em>chronologically sorted</em> order
     * @return estimated propagators
     */
    public Propagator[] processMeasurements(final Iterable<ObservedMeasurement<?>> observedMeasurements) {
        Propagator[] propagators = null;
        for (ObservedMeasurement<?> observedMeasurement : observedMeasurements) {
            propagators = estimationStep(observedMeasurement);
        }
        return propagators;
    }

    /** Get the Hipparchus filter.
     * @return the filter
     */
    protected abstract KalmanFilter<MeasurementDecorator> getKalmanFilter();

    /** Get the process model.
     * @return the process model
     */
    protected abstract SequentialModel getProcessModel();

}
