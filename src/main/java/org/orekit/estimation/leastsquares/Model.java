/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.estimation.leastsquares;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.hipparchus.util.Incrementor;
import org.hipparchus.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.JacobiansMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Bridge between {@link ObservedMeasurement measurements} and {@link
 * org.hipparchus.fitting.leastsquares.LeastSquaresProblem
 * least squares problems}.
 * @author Luc Maisonobe
 * @since 8.0
 */
class Model implements MultivariateJacobianFunction {

    /** Estimated propagator parameters. */
    private final ParameterDriversList estimatedPropagatorParameters;

    /** Builder for propagator. */
    private final NumericalPropagatorBuilder[] builders;

    /** Measurements. */
    private final List<ObservedMeasurement<?>> measurements;

    /** Estimated measurements parameters. */
    private final ParameterDriversList estimatedMeasurementsParameters;

    /** Start columns for each estimated orbit. */
    private final int[] orbitsStartColumns;

    /** End columns for each estimated orbit. */
    private final int[] orbitsEndColumns;

    /** Map for measurements parameters columns. */
    private final Map<String, Integer> parameterColumns;

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

    /** Mappers for Jacobians. */
    private JacobiansMapper[] mappers;

    /** Model function value. */
    private RealVector value;

    /** Model function Jacobian. */
    private RealMatrix jacobian;

    /** Simple constructor.
     * @param builders builders to use for propagation
     * @param measurements measurements
     * @param estimatedMeasurementsParameters estimated measurements parameters
     * @param observer observer to be notified at model calls
     * @exception OrekitException if some propagator parameter cannot be set properly
     */
    Model(final NumericalPropagatorBuilder[] builders,
          final List<ObservedMeasurement<?>> measurements, final ParameterDriversList estimatedMeasurementsParameters,
          final ModelObserver observer)
        throws OrekitException {

        this.builders                        = builders;
        this.measurements                    = measurements;
        this.estimatedMeasurementsParameters = estimatedMeasurementsParameters;
        this.parameterColumns                = new HashMap<String, Integer>(estimatedMeasurementsParameters.getDrivers().size());
        this.evaluations                     = new IdentityHashMap<ObservedMeasurement<?>, EstimatedMeasurement<?>>(measurements.size());
        this.observer                        = observer;
        this.mappers                         = new JacobiansMapper[builders.length];

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

        this.estimatedPropagatorParameters = new ParameterDriversList();
        for (int i = 0; i < builders.length; ++i) {
            for (final ParameterDriver driver : builders[i].getPropagationParametersDrivers().getDrivers()) {
                if (driver.isSelected()) {
                    estimatedPropagatorParameters.add(driver);
                    ++columns;
                }
            }
        }

        for (final ParameterDriver parameter : estimatedMeasurementsParameters.getDrivers()) {
            parameterColumns.put(parameter.getName(), columns);
            ++columns;
        }

        value    = new ArrayRealVector(rows);
        jacobian = MatrixUtils.createRealMatrix(rows, columns);

    }

    /** Set the counter for evaluations.
     * @param evaluationsCounter counter for evaluations
     */
    void setEvaluationsCounter(final Incrementor evaluationsCounter) {
        this.evaluationsCounter = evaluationsCounter;
    }

    /** Set the counter for iterations.
     * @param iterationsCounter counter for iterations
     */
    void setIterationsCounter(final Incrementor iterationsCounter) {
        this.iterationsCounter = iterationsCounter;
    }

    /** {@inheritDoc} */
    @Override
    public Pair<RealVector, RealMatrix> value(final RealVector point)
        throws OrekitExceptionWrapper {
        try {

            // set up the propagators parallelizer
            final NumericalPropagator[] propagators = createPropagators(point);
            final Orbit[] orbits = new Orbit[propagators.length];
            for (int i = 0; i < propagators.length; ++i) {
                mappers[i] = configureDerivatives(propagators[i]);
                orbits[i]  = propagators[i].getInitialState().getOrbit();
            }
            final PropagatorsParallelizer parallelizer =
                            new PropagatorsParallelizer(Arrays.asList(propagators), configureMeasurements(point));

            // reset value and Jacobian
            evaluations.clear();
            value.set(0.0);
            for (int i = 0; i < jacobian.getRowDimension(); ++i) {
                for (int j = 0; j < jacobian.getColumnDimension(); ++j) {
                    jacobian.setEntry(i, j, 0.0);
                }
            }

            // run the propagation, gathering residuals on the fly
            parallelizer.propagate(firstDate.shiftedBy(-1.0), lastDate.shiftedBy(+1.0));

            observer.modelCalled(orbits, evaluations);

            return new Pair<RealVector, RealMatrix>(value, jacobian);

        } catch (OrekitException oe) {
            throw new OrekitExceptionWrapper(oe);
        }
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

    /** Create the propagators and parameters corresponding to an evaluation point.
     * @param point evaluation point
     * @return an array of new propagators
     * @exception OrekitException if orbit cannot be created with the current point
     */
    public NumericalPropagator[] createPropagators(final RealVector point)
        throws OrekitException {

        final NumericalPropagator[] propagators = new NumericalPropagator[builders.length];

        for (int i = 0; i < builders.length; ++i) {
            // set up the propagator
            final int nbOrb    = orbitsEndColumns[i] - orbitsStartColumns[i];
            final int nbParams = estimatedPropagatorParameters.getNbParams();
            final double[] propagatorArray = new double[nbOrb + nbParams];
            for (int j = 0; j < nbOrb; ++j) {
                propagatorArray[j] = point.getEntry(orbitsStartColumns[i] + j);
            }
            for (int j = 0; j < nbParams; ++j) {
                propagatorArray[nbOrb + j] = point.getEntry(orbitsEndColumns[builders.length - 1] + j);
            }
            propagators[i] = builders[i].buildPropagator(propagatorArray);
        }

        return propagators;

    }

    /** Configure the multi-satellites handler to handle measurements.
     * @param point evaluation point
     * @return multi-satellites handler to handle measurements
     * @exception OrekitException if measurements parameters cannot be set with the current point
     */
    private MultiSatStepHandler configureMeasurements(final RealVector point)
        throws OrekitException {

        // set up the measurement parameters
        int index = orbitsEndColumns[builders.length - 1] + estimatedPropagatorParameters.getNbParams();
        for (final ParameterDriver parameter : estimatedMeasurementsParameters.getDrivers()) {
            parameter.setNormalizedValue(point.getEntry(index++));
        }

        // set up measurements handler
        final List<PreCompensation> precompensated = new ArrayList<>();
        for (final ObservedMeasurement<?> measurement : measurements) {
            if (measurement.isEnabled()) {
                precompensated.add(new PreCompensation(measurement, evaluations.get(measurement)));
            }
        }
        precompensated.sort(new ChronologicalComparator());

        firstDate = precompensated.get(0).getDate();
        lastDate  = precompensated.get(precompensated.size() - 1).getDate();

        return new MeasurementHandler(this, precompensated);

    }

    /** Configure the propagator to compute derivatives.
     * @param propagator {@link Propagator} to configure
     * @return mapper for this propagator
     * @exception OrekitException if orbit cannot be created with the current point
     */
    private JacobiansMapper configureDerivatives(final NumericalPropagator propagator)
        throws OrekitException {

        final String equationName = Model.class.getName() + "-derivatives";
        final PartialDerivativesEquations partials = new PartialDerivativesEquations(equationName, propagator);

        // add the derivatives to the initial state
        final SpacecraftState rawState = propagator.getInitialState();
        final SpacecraftState stateWithDerivatives = partials.setInitialJacobians(rawState);
        propagator.resetInitialState(stateWithDerivatives);

        return partials.getMapper();

    }

    /** Fetch a measurement that was evaluated during propagation.
     * @param index index of the measurement first component
     * @param evaluation measurement evaluation
     * @exception OrekitException if Jacobians cannot be computed
     */
    void fetchEvaluatedMeasurement(final int index, final EstimatedMeasurement<?> evaluation)
        throws OrekitException {

        // States and observed measurement
        final SpacecraftState[]      evaluationStates    = evaluation.getStates();
        final ObservedMeasurement<?> observedMeasurement = evaluation.getObservedMeasurement();

        // compute weighted residuals
        evaluations.put(observedMeasurement, evaluation);
        final double[] evaluated = evaluation.getEstimatedValue();
        final double[] observed  = observedMeasurement.getObservedValue();
        final double[] sigma     = observedMeasurement.getTheoreticalStandardDeviation();
        final double[] weight    = evaluation.getCurrentWeight();
        for (int i = 0; i < evaluated.length; ++i) {
            value.setEntry(index + i, weight[i] * (evaluated[i] - observed[i]) / sigma[i]);
        }

        for (int k = 0; k < evaluationStates.length; ++k) {

            final int p = observedMeasurement.getPropagatorsIndices().get(k);

            // partial derivatives of the current Cartesian coordinates with respect to current orbital state
            final double[][] aCY = new double[6][6];
            final Orbit currentOrbit = evaluationStates[k].getOrbit();
            currentOrbit.getJacobianWrtParameters(builders[p].getPositionAngle(), aCY);
            final RealMatrix dCdY = new Array2DRowRealMatrix(aCY, false);

            // Jacobian of the measurement with respect to current orbital state
            final RealMatrix dMdC = new Array2DRowRealMatrix(evaluation.getStateDerivatives(k), false);
            final RealMatrix dMdY = dMdC.multiply(dCdY);

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

            if (estimatedPropagatorParameters.getNbParams() > 0) {
                // Jacobian of the measurement with respect to propagator parameters
                final double[][] aYPp  = new double[6][estimatedPropagatorParameters.getNbParams()];
                mappers[p].getParametersJacobian(evaluationStates[k], aYPp);
                final RealMatrix dYdPp = new Array2DRowRealMatrix(aYPp, false);
                final RealMatrix dMdPp = dMdY.multiply(dYdPp);
                for (int i = 0; i < dMdPp.getRowDimension(); ++i) {
                    int jPar = orbitsEndColumns[builders.length - 1];
                    for (int j = 0; j < estimatedPropagatorParameters.getNbParams(); ++j) {
                        final ParameterDriver driver = estimatedPropagatorParameters.getDrivers().get(j);
                        jacobian.addToEntry(index + i, jPar++,
                                            weight[i] * dMdPp.getEntry(i, j) / sigma[i] * driver.getScale());
                    }
                }
            }

        }

        // Jacobian of the measurement with respect to measurements parameters
        for (final ParameterDriver driver : observedMeasurement.getParametersDrivers()) {
            if (driver.isSelected()) {
                final double[] aMPm = evaluation.getParameterDerivatives(driver);
                for (int i = 0; i < aMPm.length; ++i) {
                    jacobian.setEntry(index + i, parameterColumns.get(driver.getName()),
                                      weight[i] * aMPm[i] / sigma[i] * driver.getScale());
                }
            }
        }

    }

}
