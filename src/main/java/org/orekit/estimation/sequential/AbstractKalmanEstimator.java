/* Copyright 2002-2026 CS GROUP
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

import java.util.List;

import org.hipparchus.filtering.kalman.KalmanFilter;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.estimation.ParameterEstimator;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversList;

/**
 * Base class for Kalman estimators.
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @author Luc Maisonobe
 * @since 11.3
 */
public abstract class AbstractKalmanEstimator implements ParameterEstimator {

    /** List of propagator builder. */
    private final List<? extends PropagatorBuilder> builders;

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

    /** Matrix decomposer for filter. */
    private final MatrixDecomposer decomposer;

    /**
     * Constructor.
     * @param decomposer matrix decomposer for filter
     * @param builders list of propagator builders
     */
    protected AbstractKalmanEstimator(final MatrixDecomposer decomposer,
                                      final List<? extends PropagatorBuilder> builders) {
        this.builders = builders;
        this.referenceDate = builders.get(0).getInitialOrbitDate();
        this.decomposer = decomposer;
        this.observer = null;
    }

    /** Get the current measurement number.
     * @return current measurement number
     */
    public int getCurrentMeasurementNumber() {
        return getKalmanEstimation().getCurrentMeasurementNumber();
    }

    /** Get the current date.
     * @return current date
     */
    public AbsoluteDate getCurrentDate() {
        return getKalmanEstimation().getCurrentDate();
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

    /** Get the "physical" estimated state (i.e. not normalized)
     * <p>
     * For the Semi-analytical Kalman Filters
     * it corresponds to the corrected filter correction.
     * In other words, it doesn't represent an orbital state.
     * </p>
     * @return the "physical" estimated state
     */
    public RealVector getPhysicalEstimatedState() {
        return getKalmanEstimation().getPhysicalEstimatedState();
    }

    /** Get the "physical" estimated covariance matrix (i.e. not normalized)
     * @return the "physical" estimated covariance matrix
     */
    public RealMatrix getPhysicalEstimatedCovarianceMatrix() {
        return getKalmanEstimation().getPhysicalEstimatedCovarianceMatrix();
    }

    /** Get the list of estimated measurements parameters.
     * @return the list of estimated measurements parameters
     */
    public ParameterDriversList getEstimatedMeasurementsParameters() {
        return getKalmanEstimation().getEstimatedMeasurementsParameters();
    }

    /** Get the list of propagator builders.
     * @return the list of propagator builders
     */
    protected List<? extends PropagatorBuilder> getBuilders() {
        return builders;
    }

    @Override
    public PropagatorBuilder[] getPropagatorBuilders() {
        return getBuilders().toArray(new PropagatorBuilder[0]);
    }

    /** Get the provider for kalman filter estimations.
     * @return the provider for Kalman filter estimations
     */
    protected abstract KalmanEstimation getKalmanEstimation();

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

    /** Get the Hipparchus filter.
     * @return the filter
     */
    protected abstract KalmanFilter<MeasurementDecorator> getKalmanFilter();

    /** Get the parameter scaling factors.
     * @return the parameters scale
     */
    protected abstract double[] getScale();

}
