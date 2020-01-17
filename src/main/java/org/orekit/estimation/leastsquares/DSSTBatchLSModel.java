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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Incrementor;
import org.hipparchus.util.Pair;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.IntegratedPropagatorBuilder;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.propagation.semianalytical.dsst.DSSTJacobiansMapper;
import org.orekit.propagation.semianalytical.dsst.DSSTPartialDerivativesEquations;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

/** Bridge between {@link ObservedMeasurement measurements} and {@link
 * org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem
 * least squares problems}.
 * <p>
 * This class is an adaption of the {@link BatchLSModel} class
 * but for the {@link DSSTPropagator DSST propagator}.
 * </p>
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class DSSTBatchLSModel implements BatchLSODModel {

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

    /** Mappers for Jacobians. */
    private DSSTJacobiansMapper[] mappers;

    /** Model function value. */
    private RealVector value;

    /** Model function Jacobian. */
    private RealMatrix jacobian;

    /** Type of the orbit used for the propagation.*/
    private PropagationType propagationType;

    /** Type of the elements used to define the orbital state.*/
    private PropagationType stateType;

    /** Simple constructor.
     * @param propagatorBuilders builders to use for propagation
     * @param measurements measurements
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param observer observer to be notified at model calls
     * @param propagationType type of the orbit used for the propagation (mean or osculating)
     * @param stateType type of the elements used to define the orbital state (mean or osculating)
     */
    public DSSTBatchLSModel(final IntegratedPropagatorBuilder[] propagatorBuilders,
                     final List<ObservedMeasurement<?>> measurements,
                     final ParameterDriversList estimatedMeasurementsParameters,
                     final ModelObserver observer,
                     final PropagationType propagationType,
                     final PropagationType stateType) {

        this.builders                        = propagatorBuilders.clone();
        this.measurements                    = measurements;
        this.estimatedMeasurementsParameters = estimatedMeasurementsParameters;
        this.measurementParameterColumns     = new HashMap<>(estimatedMeasurementsParameters.getDrivers().size());
        this.estimatedPropagationParameters  = new ParameterDriversList[builders.length];
        this.evaluations                     = new IdentityHashMap<>(measurements.size());
        this.observer                        = observer;
        this.mappers                         = new DSSTJacobiansMapper[builders.length];
        this.propagationType                 = propagationType;
        this.stateType                       = stateType;

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
    public void setEvaluationsCounter(final Incrementor evaluationsCounter) {
        this.evaluationsCounter = evaluationsCounter;
    }

    /** {@inheritDoc} */
    public void setIterationsCounter(final Incrementor iterationsCounter) {
        this.iterationsCounter = iterationsCounter;
    }

    /** {@inheritDoc} */
    public boolean isForwardPropagation() {
        return forwardPropagation;
    }

    /** {@inheritDoc} */
    @Override
    public Pair<RealVector, RealMatrix> value(final RealVector point) {

        // Set up the propagators parallelizer
        final DSSTPropagator[] propagators = createPropagators(point);
        final Orbit[] orbits = new Orbit[propagators.length];
        for (int i = 0; i < propagators.length; ++i) {
            mappers[i] = configureDerivatives(propagators[i]);
            orbits[i]  = propagators[i].getInitialState().getOrbit();
        }
        final PropagatorsParallelizer parallelizer =
                        new PropagatorsParallelizer(Arrays.asList(propagators), configureMeasurements(point));

        // Reset value and Jacobian
        evaluations.clear();
        value.set(0.0);
        for (int i = 0; i < jacobian.getRowDimension(); ++i) {
            for (int j = 0; j < jacobian.getColumnDimension(); ++j) {
                jacobian.setEntry(i, j, 0.0);
            }
        }

        // Run the propagation, gathering residuals on the fly
        if (forwardPropagation) {
            // Propagate forward from firstDate
            parallelizer.propagate(firstDate.shiftedBy(-1.0), lastDate.shiftedBy(+1.0));
        } else {
            // Propagate backward from lastDate
            parallelizer.propagate(lastDate.shiftedBy(+1.0), firstDate.shiftedBy(-1.0));
        }

        observer.modelCalled(orbits, evaluations);

        return new Pair<RealVector, RealMatrix>(value, jacobian);

    }

    /** {@inheritDoc} */
    public int getIterationsCount() {
        return iterationsCounter.getCount();
    }

    /** {@inheritDoc} */
    public int getEvaluationsCount() {
        return evaluationsCounter.getCount();
    }

    /** {@inheritDoc} */
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
    public DSSTPropagator[] createPropagators(final RealVector point) {

        final DSSTPropagator[] propagators = new DSSTPropagator[builders.length];

        // Set up the propagators
        for (int i = 0; i < builders.length; ++i) {

            // Get the number of selected orbital drivers in the builder
            final int nbOrb    = orbitsEndColumns[i] - orbitsStartColumns[i];

            // Get the list of selected propagation drivers in the builder and its size
            final ParameterDriversList selectedPropagationDrivers = getSelectedPropagationDriversForBuilder(i);
            final int nbParams = selectedPropagationDrivers.getNbParams();

            // Init the array of normalized parameters for the builder
            final double[] propagatorArray = new double[nbOrb + nbParams];

            // Add the orbital drivers normalized values
            for (int j = 0; j < nbOrb; ++j) {
                propagatorArray[j] = point.getEntry(orbitsStartColumns[i] + j);
            }

            // Add the propagation drivers normalized values
            for (int j = 0; j < nbParams; ++j) {
                propagatorArray[nbOrb + j] =
                                point.getEntry(propagationParameterColumns.get(selectedPropagationDrivers.getDrivers().get(j).getName()));
            }

            // Build the propagator
            propagators[i] = (DSSTPropagator) builders[i].buildPropagator(propagatorArray);
        }

        return propagators;

    }

    /** Configure the multi-satellites handler to handle measurements.
     * @param point evaluation point
     * @return multi-satellites handler to handle measurements
     */
    private MultiSatStepHandler configureMeasurements(final RealVector point) {

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
    private DSSTJacobiansMapper configureDerivatives(final DSSTPropagator propagators) {

        final String equationName = DSSTBatchLSModel.class.getName() + "-derivatives";

        final DSSTPartialDerivativesEquations partials = new DSSTPartialDerivativesEquations(equationName, propagators, propagationType);

        // add the derivatives to the initial state
        final SpacecraftState rawState = propagators.getInitialState();
        final SpacecraftState stateWithDerivatives = partials.setInitialJacobians(rawState);
        propagators.setInitialState(stateWithDerivatives, stateType);

        return partials.getMapper();

    }

    /** {@inheritDoc} */
    public void fetchEvaluatedMeasurement(final int index, final EstimatedMeasurement<?> evaluation) {

        // States and observed measurement
        final SpacecraftState[]      evaluationStates    = evaluation.getStates();
        final ObservedMeasurement<?> observedMeasurement = evaluation.getObservedMeasurement();

        // compute weighted residuals
        evaluations.put(observedMeasurement, evaluation);
        if (evaluation.getStatus() == EstimatedMeasurement.Status.REJECTED) {
            return;
        }

        final double[] evaluated = evaluation.getEstimatedValue();
        final double[] observed  = observedMeasurement.getObservedValue();
        final double[] sigma     = observedMeasurement.getTheoreticalStandardDeviation();
        final double[] weight    = evaluation.getObservedMeasurement().getBaseWeight();
        for (int i = 0; i < evaluated.length; ++i) {
            value.setEntry(index + i, weight[i] * (evaluated[i] - observed[i]) / sigma[i]);
        }

        for (int k = 0; k < evaluationStates.length; ++k) {

            final int p = observedMeasurement.getSatellites().get(k).getPropagatorIndex();

            // partial derivatives of the current Cartesian coordinates with respect to current orbital state
            final double[][] aCY = new double[6][6];
            final Orbit currentOrbit = evaluationStates[k].getOrbit();
            currentOrbit.getJacobianWrtParameters(builders[p].getPositionAngle(), aCY);
            final RealMatrix dCdY = new Array2DRowRealMatrix(aCY, false);

            // Jacobian of the measurement with respect to current orbital state
            final RealMatrix dMdC = new Array2DRowRealMatrix(evaluation.getStateDerivatives(k), false);
            final RealMatrix dMdY = dMdC.multiply(dCdY);

            // short period derivatives
            mappers[p].setShortPeriodJacobians(evaluationStates[k]);

            // Jacobian of the measurement with respect to initial orbital state
            final double[][] aYY0 = new double[6][6];
            mappers[p].getStateJacobian(evaluationStates[k], aYY0);
            final RealMatrix dYdY0 = new Array2DRowRealMatrix(aYY0, false);
            final RealMatrix dMdY0 = dMdY.multiply(dYdY0);
            for (int i = 0; i < dMdY0.getRowDimension(); ++i) {
                int jOrb = orbitsStartColumns[p];
                for (int j = 0; j < dMdY0.getColumnDimension(); ++j) {
                    final ParameterDriver driver = builders[p].getOrbitalParametersDrivers().getDrivers().get(j);
                    if (driver.isSelected()) {
                        jacobian.setEntry(index + i, jOrb++,
                                          weight[i] * dMdY0.getEntry(i, j) / sigma[i] * driver.getScale());
                    }
                }
            }

            // Jacobian of the measurement with respect to propagation parameters
            final ParameterDriversList selectedPropagationDrivers = getSelectedPropagationDriversForBuilder(p);
            final int nbParams = selectedPropagationDrivers.getNbParams();
            if ( nbParams > 0) {
                final double[][] aYPp  = new double[6][nbParams];
                mappers[p].getParametersJacobian(evaluationStates[k], aYPp);
                final RealMatrix dYdPp = new Array2DRowRealMatrix(aYPp, false);
                final RealMatrix dMdPp = dMdY.multiply(dYdPp);
                for (int i = 0; i < dMdPp.getRowDimension(); ++i) {
                    for (int j = 0; j < nbParams; ++j) {
                        final ParameterDriver delegating = selectedPropagationDrivers.getDrivers().get(j);
                        jacobian.addToEntry(index + i, propagationParameterColumns.get(delegating.getName()),
                                            weight[i] * dMdPp.getEntry(i, j) / sigma[i] * delegating.getScale());
                    }
                }
            }

        }

        // Jacobian of the measurement with respect to measurements parameters
        for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
            if (driver.isSelected()) {
                final double[] aMPm = evaluation.getParameterDerivatives(driver);
                for (int i = 0; i < aMPm.length; ++i) {
                    jacobian.setEntry(index + i, measurementParameterColumns.get(driver.getName()),
                                      weight[i] * aMPm[i] / sigma[i] * driver.getScale());
                }
            }
        }

    }

}
