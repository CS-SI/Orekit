/* Copyright 2002-2022 CS GROUP
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
import org.hipparchus.optim.nonlinear.vector.leastsquares.MultivariateJacobianFunction;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Incrementor;
import org.hipparchus.util.Pair;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.OrbitDeterminationPropagatorBuilder;
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
public abstract class AbstractBatchLSModel implements MultivariateJacobianFunction {

    /** Builders for propagators. */
    private final OrbitDeterminationPropagatorBuilder[] builders;

    /** Array of each builder's selected orbit drivers.
     * @since 11.1
     */
    private final ParameterDriversList[] estimatedOrbitalParameters;

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

    /** Indirection array in measurements jacobians.
     * @since 11.2
     */
    private final int[] orbitsJacobianColumns;

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

    /** Harvesters for extracting State Transition Matrices and Jacobians from integrated states.
     * @since 11.1
     */
    private final MatricesHarvester[] harvesters;

    /** Model function Jacobian. */
    private RealMatrix jacobian;

    /**
     * Constructor.
     * @param propagatorBuilders builders to use for propagation
     * @param measurements measurements
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param harvesters harvesters for matrices (ignored since 11.1)
     * @param observer observer to be notified at model calls
     * @deprecated as of 11.1, replaced by [@link #AbstractBatchLSModel(OrbitDeterminationPropagatorBuilder[],
     * List, ParameterDriversList, ModelObserver)}
     */
    @Deprecated
    public AbstractBatchLSModel(final OrbitDeterminationPropagatorBuilder[] propagatorBuilders,
                                final List<ObservedMeasurement<?>> measurements,
                                final ParameterDriversList estimatedMeasurementsParameters,
                                final MatricesHarvester[] harvesters,
                                final ModelObserver observer) {
        this(propagatorBuilders, measurements, estimatedMeasurementsParameters, observer);
    }

    /**
     * Constructor.
     * @param propagatorBuilders builders to use for propagation
     * @param measurements measurements
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param observer observer to be notified at model calls
     */
    public AbstractBatchLSModel(final OrbitDeterminationPropagatorBuilder[] propagatorBuilders,
                                final List<ObservedMeasurement<?>> measurements,
                                final ParameterDriversList estimatedMeasurementsParameters,
                                final ModelObserver observer) {

        this.builders                        = propagatorBuilders.clone();
        this.measurements                    = measurements;
        this.estimatedMeasurementsParameters = estimatedMeasurementsParameters;
        this.measurementParameterColumns     = new HashMap<>(estimatedMeasurementsParameters.getDrivers().size());
        this.estimatedOrbitalParameters      = new ParameterDriversList[builders.length];
        this.estimatedPropagationParameters  = new ParameterDriversList[builders.length];
        this.evaluations                     = new IdentityHashMap<>(measurements.size());
        this.observer                        = observer;
        this.harvesters                      = new MatricesHarvester[builders.length];

        // allocate vector and matrix
        int rows = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {
            rows += measurement.getDimension();
        }

        this.orbitsStartColumns    = new int[builders.length];
        this.orbitsEndColumns      = new int[builders.length];
        this.orbitsJacobianColumns = new int[builders.length * 6];
        Arrays.fill(orbitsJacobianColumns, -1);
        int columns = 0;
        for (int i = 0; i < builders.length; ++i) {
            this.orbitsStartColumns[i] = columns;
            final List<ParameterDriversList.DelegatingDriver> orbitalParametersDrivers =
                            builders[i].getOrbitalParametersDrivers().getDrivers();
            for (int j = 0; j < orbitalParametersDrivers.size(); ++j) {
                if (orbitalParametersDrivers.get(j).isSelected()) {
                    orbitsJacobianColumns[columns] = j;
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

    /** Set the counter for evaluations.
     * @param evaluationsCounter counter for evaluations
     */
    public void setEvaluationsCounter(final Incrementor evaluationsCounter) {
        this.evaluationsCounter = evaluationsCounter;
    }

    /** Set the counter for iterations.
     * @param iterationsCounter counter for iterations
     */
    public void setIterationsCounter(final Incrementor iterationsCounter) {
        this.iterationsCounter = iterationsCounter;
    }

    /** Return the forward propagation flag.
     * @return the forward propagation flag
     */
    public boolean isForwardPropagation() {
        return forwardPropagation;
    }

    /** Configure the propagator to compute derivatives.
     * @param propagator {@link Propagator} to configure
     * @return harvester harvester to retrive the State Transition Matrix and Jacobian Matrix
     */
    protected MatricesHarvester configureHarvester(final Propagator propagator) {
        // FIXME: this default implementation is only intended for version 11.1 to delegate to a deprecated method
        // it should be removed in 12.0 when configureDerivatives is removed
        return configureDerivatives(propagator);
    }

    /** Configure the propagator to compute derivatives.
     * @param propagators {@link Propagator} to configure
     * @return mapper for this propagator
     * @deprecated as of 11.1, replaced by {@link #configureHarvester(Propagator)}
     */
    @Deprecated
    protected abstract AbstractJacobiansMapper configureDerivatives(Propagator propagators);

    /** Configure the current estimated orbits.
     * <p>
     * For DSST orbit determination, short period derivatives are also calculated.
     * </p>
     * @param harvester harvester for matrices
     * @param propagator the orbit propagator
     * @return the current estimated orbits
     */
    protected abstract Orbit configureOrbits(MatricesHarvester harvester, Propagator propagator);

    /** {@inheritDoc} */
    @Override
    public Pair<RealVector, RealMatrix> value(final RealVector point) {

        // Set up the propagators parallelizer
        final Propagator[] propagators = createPropagators(point);
        final Orbit[] orbits = new Orbit[propagators.length];
        for (int i = 0; i < propagators.length; ++i) {
            harvesters[i] = configureHarvester(propagators[i]);
            orbits[i]     = configureOrbits(harvesters[i], propagators[i]);
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
        if (isForwardPropagation()) {
            // Propagate forward from firstDate
            parallelizer.propagate(firstDate.shiftedBy(-1.0), lastDate.shiftedBy(+1.0));
        } else {
            // Propagate backward from lastDate
            parallelizer.propagate(lastDate.shiftedBy(+1.0), firstDate.shiftedBy(-1.0));
        }

        observer.modelCalled(orbits, evaluations);

        return new Pair<RealVector, RealMatrix>(value, jacobian);

    }

    /** Get the selected orbital drivers for a propagatorBuilder.
     * @param iBuilder index of the builder in the builders' array
     * @return the list of selected orbital drivers for propagatorBuilder of index iBuilder
     * @since 11.1
     */
    public ParameterDriversList getSelectedOrbitalParametersDriversForBuilder(final int iBuilder) {

        // Lazy evaluation, create the list only if it hasn't been created yet
        if (estimatedOrbitalParameters[iBuilder] == null) {

            // Gather the drivers
            final ParameterDriversList selectedOrbitalDrivers = new ParameterDriversList();
            for (final DelegatingDriver delegating : builders[iBuilder].getOrbitalParametersDrivers().getDrivers()) {
                if (delegating.isSelected()) {
                    for (final ParameterDriver driver : delegating.getRawDrivers()) {
                        selectedOrbitalDrivers.add(driver);
                    }
                }
            }

            // Add the list of selected orbital parameters drivers to the array
            estimatedOrbitalParameters[iBuilder] = selectedOrbitalDrivers;
        }
        return estimatedOrbitalParameters[iBuilder];
    }

    /** Get the selected propagation drivers for a propagatorBuilder.
     * @param iBuilder index of the builder in the builders' array
     * @return the list of selected propagation drivers for propagatorBuilder of index iBuilder
     */
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

    /** Create the propagators and parameters corresponding to an evaluation point.
     * @param point evaluation point
     * @return an array of new propagators
     */
    public Propagator[] createPropagators(final RealVector point) {

        final Propagator[] propagators = new Propagator[builders.length];

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
            propagators[i] = builders[i].buildPropagator(propagatorArray);
        }

        return propagators;

    }

    /** Fetch a measurement that was evaluated during propagation.
     * @param index index of the measurement first component
     * @param evaluation measurement evaluation
     */
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

            // Jacobian of the measurement with respect to initial orbital state
            final ParameterDriversList selectedOrbitalDrivers = getSelectedOrbitalParametersDriversForBuilder(p);
            final int nbOrbParams = selectedOrbitalDrivers.getNbParams();
            if (nbOrbParams > 0) {
                final RealMatrix dYdY0 = harvesters[p].getStateTransitionMatrix(evaluationStates[k]);
                final RealMatrix dMdY0 = dMdY.multiply(dYdY0);
                for (int i = 0; i < dMdY0.getRowDimension(); ++i) {
                    for (int j = orbitsStartColumns[p]; j < orbitsEndColumns[p]; ++j) {
                        final ParameterDriver driver =
                                        selectedOrbitalDrivers.getDrivers().get(j - orbitsStartColumns[p]);
                        final double partial = dMdY0.getEntry(i, orbitsJacobianColumns[j]);
                        jacobian.setEntry(index + i, j,
                                          weight[i] * partial / sigma[i] * driver.getScale());
                    }
                }
            }

            // Jacobian of the measurement with respect to propagation parameters
            final ParameterDriversList selectedPropagationDrivers = getSelectedPropagationDriversForBuilder(p);
            final int nbParams = selectedPropagationDrivers.getNbParams();
            if (nbParams > 0) {
                final RealMatrix dYdPp = harvesters[p].getParametersJacobian(evaluationStates[k]);
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

    /** Get the iterations count.
     * @return iterations count
     */
    public int getIterationsCount() {
        return iterationsCounter.getCount();
    }

    /** Get the evaluations count.
     * @return evaluations count
     */
    public int getEvaluationsCount() {
        return evaluationsCounter.getCount();
    }

}
