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

import java.util.List;

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

/**
 * Base class for Kalman estimators.
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @author Luc Maisonobe
 * @since 11.3
 */
public abstract class AbstractKalmanEstimator {

    /** List of propagator builder. */
    private final List<? extends PropagatorBuilder> builders;

    /**
     * Constructor.
     * @param builders list of propagator builders
     */
    protected AbstractKalmanEstimator(final List<? extends PropagatorBuilder> builders) {
        this.builders = builders;
    }

    /** Get the orbital parameters supported by this estimator.
     * <p>
     * If there are more than one propagator builder, then the names
     * of the drivers have an index marker in square brackets appended
     * to them in order to distinguish the various orbits. So for example
     * with one builder generating Keplerian orbits the names would be
     * simply "a", "e", "i"... but if there are several builders the
     * names would be "a[0]", "e[0]", "i[0]"..."a[1]", "e[1]", "i[1]"...
     * </p>
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return orbital parameters supported by this estimator
     */
    public ParameterDriversList getOrbitalParametersDrivers(final boolean estimatedOnly) {

        final ParameterDriversList estimated = new ParameterDriversList();
        for (int i = 0; i < builders.size(); ++i) {
            final String suffix = builders.size() > 1 ? "[" + i + "]" : null;
            for (final ParameterDriver driver : builders.get(i).getOrbitalParametersDrivers().getDrivers()) {
                if (driver.isSelected() || !estimatedOnly) {
                    if (suffix != null && !driver.getName().endsWith(suffix)) {
                        // we add suffix only conditionally because the method may already have been called
                        // and suffixes may have already been appended
                        driver.setName(driver.getName() + suffix);
                    }
                    estimated.add(driver);
                }
            }
        }
        return estimated;
    }

    /** Get the propagator parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return propagator parameters supported by this estimator
     */
    public ParameterDriversList getPropagationParametersDrivers(final boolean estimatedOnly) {

        final ParameterDriversList estimated = new ParameterDriversList();
        for (PropagatorBuilder builder : builders) {
            for (final DelegatingDriver delegating : builder.getPropagationParametersDrivers().getDrivers()) {
                if (delegating.isSelected() || !estimatedOnly) {
                    for (final ParameterDriver driver : delegating.getRawDrivers()) {
                        estimated.add(driver);
                    }
                }
            }
        }
        return estimated;
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

    /** Get the provider for kalman filter estimations.
     * @return the provider for Kalman filter estimations
     */
    protected abstract KalmanEstimation getKalmanEstimation();

}
