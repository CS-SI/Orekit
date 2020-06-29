/* Copyright 2002-2020 CS GROUP
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
package org.orekit.estimation.leastsquares;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Incrementor;
import org.hipparchus.util.Pair;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.IntegratedPropagatorBuilder;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

/** Bridge between {@link ObservedMeasurement measurements} and {@link
 * org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem
 * least squares problems}.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.0
 */
public abstract class AbstractBatchLSModel implements BatchLSODModel {

    /** Builders for propagators. */
    private final IntegratedPropagatorBuilder[] builders;

    /** Array of each builder's selected propagation drivers. */
    private final ParameterDriversList[] estimatedPropagationParameters;

    /** Estimated measurements parameters. */
    private final ParameterDriversList estimatedMeasurementsParameters;

    /** Measurements. */
    private final List<ObservedMeasurement<?>> measurements;

    /** Start columns for each estimated orbit. */
    private final int[] orbitsStartColumns;

    /** End columns for each estimated orbit. */
    private final int[] orbitsEndColumns;

    /** Map for propagation parameters columns. */
    private final Map<String, Integer> propagationParameterColumns;

    /** Map for measurements parameters columns. */
    private final Map<String, Integer> measurementParameterColumns;

    /** Last evaluations. */
    private final Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> evaluations;

    /** Observer to be notified at orbit changes. */
    private final ModelObserver observer;

    /** Counter for the evaluations. */
    private Incrementor evaluationsCounter;

    /** Counter for the iterations. */
    private Incrementor iterationsCounter;

    /** Date of the first enabled measurement. */
    private AbsoluteDate firstDate;

    /** Date of the last enabled measurement. */
    private AbsoluteDate lastDate;

    /** Boolean indicating if the propagation will go forward or backward. */
    private final boolean forwardPropagation;

    /** Model function value. */
    private RealVector value;

    /** Model function Jacobian. */
    private RealMatrix jacobian;

    public AbstractBatchLSModel(final IntegratedPropagatorBuilder[] propagatorBuilders,
                        final List<ObservedMeasurement<?>> measurements,
                        final ParameterDriversList estimatedMeasurementsParameters,
                        final ModelObserver observer) {

        this.builders                        = propagatorBuilders.clone();
        this.measurements                    = measurements;
        this.estimatedMeasurementsParameters = estimatedMeasurementsParameters;
        this.measurementParameterColumns     = new HashMap<>(estimatedMeasurementsParameters.getDrivers().size());
        this.estimatedPropagationParameters  = new ParameterDriversList[builders.length];
        this.evaluations                     = new IdentityHashMap<>(measurements.size());
        this.observer                        = observer;

        // allocate vector and matrix
        int rows = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {
            rows += measurement.getDimension();
        }

        this.orbitsStartColumns = new int[builders.length];
        this.orbitsEndColumns   = new int[builders.length];
        int columns = 0;
        for (int i = 0; i < builders.length; ++i) {
            this.orbitsStartColumns[i] = columns;
            for (final ParameterDriver driver : builders[i].getOrbitalParametersDrivers().getDrivers()) {
                if (driver.isSelected()) {
                    ++columns;
                }
            }
            this.orbitsEndColumns[i] = columns;
        }

        // Gather all the propagation drivers names in a list
        final List<String> estimatedPropagationParametersNames = new ArrayList<>();
        for (int i = 0; i < builders.length; ++i) {
            // The index i in array estimatedPropagationParameters (attribute of the class) is populated
            // when the first call to getSelectedPropagationDriversForBuilder(i) is made
            for (final DelegatingDriver delegating : getSelectedPropagationDriversForBuilder(i).getDrivers()) {
                final String driverName = delegating.getName();
                // Add the driver name if it has not been added yet
                if (!estimatedPropagationParametersNames.contains(driverName)) {
                    estimatedPropagationParametersNames.add(driverName);
                }
            }
        }
        // Populate the map of propagation drivers' columns and update the total number of columns
        propagationParameterColumns = new HashMap<>(estimatedPropagationParametersNames.size());
        for (final String driverName : estimatedPropagationParametersNames) {
            propagationParameterColumns.put(driverName, columns);
            ++columns;
        }

        // Populate the map of measurement drivers' columns and update the total number of columns
        for (final ParameterDriver parameter : estimatedMeasurementsParameters.getDrivers()) {
            measurementParameterColumns.put(parameter.getName(), columns);
            ++columns;
        }

        // Initialize point and value
        value    = new ArrayRealVector(rows);
        jacobian = MatrixUtils.createRealMatrix(rows, columns);

        // Decide whether the propagation will be done forward or backward.
        // Minimize the duration between first measurement treated and orbit determination date
        // Propagator builder number 0 holds the reference date for orbit determination
        final AbsoluteDate refDate = builders[0].getInitialOrbitDate();

        // Sort the measurement list chronologically
        measurements.sort(new ChronologicalComparator());
        firstDate = measurements.get(0).getDate();
        lastDate  = measurements.get(measurements.size() - 1).getDate();

        // Decide the direction of propagation
        if (FastMath.abs(refDate.durationFrom(firstDate)) <= FastMath.abs(refDate.durationFrom(lastDate))) {
            // Propagate forward from firstDate
            forwardPropagation = true;
        } else {
            // Propagate backward from lastDate
            forwardPropagation = false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setEvaluationsCounter(final Incrementor evaluationsCounter) {
        this.evaluationsCounter = evaluationsCounter;
    }

    /** {@inheritDoc} */
    @Override
    public void setIterationsCounter(final Incrementor iterationsCounter) {
        this.iterationsCounter = iterationsCounter;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isForwardPropagation() {
        return forwardPropagation;
    }

    public abstract Pair<RealVector, RealMatrix> value(RealVector point);

    /** {@inheritDoc} */
    @Override
    public int getIterationsCount() {
        return iterationsCounter.getCount();
    }

    /** {@inheritDoc} */
    @Override
    public int getEvaluationsCount() {
        return evaluationsCounter.getCount();
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriversList getSelectedPropagationDriversForBuilder(final int iBuilder) {

        // Lazy evaluation, create the list only if it hasn't been created yet
        if (estimatedPropagationParameters[iBuilder] == null) {

            // Gather the drivers
            final ParameterDriversList selectedPropagationDrivers = new ParameterDriversList();
            for (final DelegatingDriver delegating : builders[iBuilder].getPropagationParametersDrivers().getDrivers()) {
                if (delegating.isSelected()) {
                    for (final ParameterDriver driver : delegating.getRawDrivers()) {
                        selectedPropagationDrivers.add(driver);
                    }
                }
            }

            // List of propagation drivers are sorted in the BatchLSEstimator class.
            // Hence we need to sort this list so the parameters' indexes match
            selectedPropagationDrivers.sort();

            // Add the list of selected propagation drivers to the array
            estimatedPropagationParameters[iBuilder] = selectedPropagationDrivers;
        }
        return estimatedPropagationParameters[iBuilder];
    }

    /** {@inheritDoc} */
    @Override
    public abstract AbstractPropagator[] createPropagators(RealVector point);

    /** Configure the multi-satellites handler to handle measurements.
     * @param point evaluation point
     * @return multi-satellites handler to handle measurements
     */
    protected MultiSatStepHandler configureMeasurements(final RealVector point) {

        // Set up the measurement parameters
        int index = orbitsEndColumns[builders.length - 1] + propagationParameterColumns.size();
        for (final ParameterDriver parameter : estimatedMeasurementsParameters.getDrivers()) {
            parameter.setNormalizedValue(point.getEntry(index++));
        }

        // Set up measurements handler
        final List<PreCompensation> precompensated = new ArrayList<>();
        for (final ObservedMeasurement<?> measurement : measurements) {
            if (measurement.isEnabled()) {
                precompensated.add(new PreCompensation(measurement, evaluations.get(measurement)));
            }
        }
        precompensated.sort(new ChronologicalComparator());

        // Assign first and last date
        firstDate = precompensated.get(0).getDate();
        lastDate  = precompensated.get(precompensated.size() - 1).getDate();

        // Reverse the list in case of backward propagation
        if (!forwardPropagation) {
            Collections.reverse(precompensated);
        }

        return new MeasurementHandler(this, precompensated);

    }

    /** Configure the propagator to compute derivatives.
     * @param propagators {@link Propagator} to configure
     * @return mapper for this propagator
     */
    protected abstract AbstractJacobiansMapper configureDerivatives(AbstractPropagator propagators);

    /** {@inheritDoc} */
    @Override
    public abstract void fetchEvaluatedMeasurement(int index, EstimatedMeasurement<?> evaluation);

    /** Getter for the date of the first enabled meausurement.
     * @return date of the first enabled measurement
     */
    public AbsoluteDate getFirstDate() {
        return firstDate;
    }

    /** Getter for the date of the last enabled measurment.
     * @return date of the last enabled measurement.
     */
    public AbsoluteDate getLastDate() {
        return lastDate;
    }

    /** Getter for the value fonction of the model.
     * @return the value function of the model
     */
    public RealVector getValue() {
        return value;
    }

    /**Getter for the jacobian matrix.
     * @return the jacobian matrix
     */
    public RealMatrix getJacobian() {
        return jacobian;
    }

    /**Getter for propagator builders.
     * @return an array of the propagator builders
     */
    public IntegratedPropagatorBuilder[] getBuilders() {
        return builders;
    }

    /** Getter for each builder's selected propagation drivers.
     * @return an array of each builder's selected propagation drivers.
     */
    public ParameterDriversList[] getEstimatedPropagationParameters() {
        return estimatedPropagationParameters;
    }

    /** Getter for estimated measurements parameters.
     * @return the estimated measurements parameters
     */
    public ParameterDriversList getEstimatedMeasurementsParameters() {
        return estimatedMeasurementsParameters;
    }

    /** Getter for the measurments.
     * @return the list of measurements
     */
    public List<ObservedMeasurement<?>> getMeasurements() {
        return measurements;
    }

    /** Getter for start columns of each estimated orbit.
     * @return an array with start index of each estimated orbit
     */
    public int[] getOrbitsStartColumns() {
        return orbitsStartColumns;
    }

    /** Getter for end columns of each estimated orbit.
     * @return an array with end index of each estimated orbit
     */
    public int[] getOrbitsEndColumns() {
        return orbitsEndColumns;
    }

    /** Getter for the map of propagation parameters columns.
     * @return the map of propagation parameters columns
     */
    public Map<String, Integer> getPropagationParameterColumns() {
        return propagationParameterColumns;
    }

    /** Getter for the map of measurements parameters columns.
     * @return the map of measurements parameters columns
     */
    public Map<String, Integer> getMeasurementParameterColumns() {
        return measurementParameterColumns;
    }

    /** Getter for the last evaluations.
     * @return the map of the last evalutaions
     */
    public Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> getEvaluations() {
        return evaluations;
    }

    /** Getter for the observer to be notified at orbit changes.
     * @return the observer to be notified at orbit changes.
     */
    public ModelObserver getObserver() {
        return observer;
    }

    /** Getter for evaluation counter.
     * @return the evaluation counter
     */
    public Incrementor getEvaluationsCounter() {
        return evaluationsCounter;
    }

    /** Getter for the iteration counter.
     * @return the iteration counter
     */
    public Incrementor getIterationsCounter() {
        return iterationsCounter;
    }


}
