/* Copyright 2002-2016 CS Systèmes d'Information
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.ConvergenceChecker;
import org.hipparchus.optim.nonlinear.vector.leastsquares.EvaluationRmsChecker;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresBuilder;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresOptimizer.Optimum;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem;
import org.hipparchus.optim.nonlinear.vector.leastsquares.ParameterValidator;
import org.hipparchus.util.Incrementor;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;


/** Least squares estimator for orbit determination.
 * @author Luc Maisonobe
 * @since 8.0
 */
public class BatchLSEstimator {

    /** Builder for propagator. */
    private final NumericalPropagatorBuilder propagatorBuilder;

    /** Measurements. */
    private final List<ObservedMeasurement<?>> measurements;

    /** Solver for least squares problem. */
    private final LeastSquaresOptimizer optimizer;

    /** Convergence threshold on normalized parameters. */
    private double parametersConvergenceThreshold;

    /** Builder for the least squares problem. */
    private final LeastSquaresBuilder lsBuilder;

    /** Oberver for iterations. */
    private BatchLSObserver observer;

    /** Last estimations. */
    private Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> estimations;

    /** Last orbit. */
    private Orbit orbit;

    /** Optimum found. */
    private Optimum optimum;

    /** Counter for the evaluations. */
    private Incrementor evaluationsCounter;

    /** Counter for the iterations. */
    private Incrementor iterationsCounter;

    /** Simple constructor.
     * @param propagatorBuilder builder to user for propagation
     * @param optimizer solver for least squares problem
     * @exception OrekitException if some propagator parameter cannot be retrieved
     */
    public BatchLSEstimator(final NumericalPropagatorBuilder propagatorBuilder,
                            final LeastSquaresOptimizer optimizer)
        throws OrekitException {

        this.propagatorBuilder              = propagatorBuilder;
        this.measurements                   = new ArrayList<ObservedMeasurement<?>>();
        this.optimizer                      = optimizer;
        this.parametersConvergenceThreshold = Double.NaN;
        this.lsBuilder                      = new LeastSquaresBuilder();
        this.estimations                    = null;
        this.observer                       = null;

        // our model computes value and Jacobian in one call,
        // so we don't use the lazy evaluation feature
        lsBuilder.lazyEvaluation(false);

        // we manage weight by ourselves, as we change them during
        // iterations (setting to 0 the identified outliers measurements)
        // so the least squares problem should not see our weights
        lsBuilder.weight(null);

    }

    /** Set an observer for iterations.
     * @param observer observer to be notified at the end of each iteration
     */
    public void setObserver(final BatchLSObserver observer) {
        this.observer = observer;
    }

    /** Add a measurement.
     * @param measurement measurement to add
     * @exception OrekitException if the measurement has a parameter
     * that is already used
     */
    public void addMeasurement(final ObservedMeasurement<?> measurement)
        throws OrekitException {
        measurements.add(measurement);
    }

    /** Set the maximum number of iterations.
     * <p>
     * The iterations correspond to the top level iterations of
     * the {@link LeastSquaresOptimizer least squares optimizer}.
     * </p>
     * @param maxIterations maxIterations maximum number of iterations
     * @see #setMaxEvaluations(int)
     * @see #getIterationsCount()
     */
    public void setMaxIterations(final int maxIterations) {
        lsBuilder.maxIterations(maxIterations);
    }

    /** Set the maximum number of model evaluations.
     * <p>
     * The evaluations correspond to the orbit propagations and
     * measurements estimations performed with a set of estimated
     * parameters.
     * </p>
     * <p>
     * For {@link org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer
     * Gauss-Newton optimizer} there is one evaluation at each iteration,
     * so the maximum numbers may be set to the same value. For {@link
     * org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer
     * Levenberg-Marquardt optimizer}, there can be several evaluations at
     * some iterations (typically for the first couple of iterations), so the
     * maximum number of evaluations may be set to a higher value than the
     * maximum number of iterations.
     * </p>
     * @param maxEvaluations maximum number of model evaluations
     * @see #setMaxIterations(int)
     * @see #getEvaluationsCount()
     */
    public void setMaxEvaluations(final int maxEvaluations) {
        lsBuilder.maxEvaluations(maxEvaluations);
    }

    /** Get the orbital parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return orbital parameters supported by this estimator
     * @exception OrekitException if different parameters have the same name
     */
    public ParameterDriversList getOrbitalParametersDrivers(final boolean estimatedOnly)
        throws OrekitException {

        if (estimatedOnly) {
            final ParameterDriversList estimated = new ParameterDriversList();
            for (final DelegatingDriver delegating : propagatorBuilder.getOrbitalParametersDrivers().getDrivers()) {
                if (delegating.isSelected()) {
                    for (final ParameterDriver driver : delegating.getRawDrivers()) {
                        estimated.add(driver);
                    }
                }
            }
            return estimated;
        } else {
            return propagatorBuilder.getOrbitalParametersDrivers();
        }

    }

    /** Get the propagator parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return propagator parameters supported by this estimator
     * @exception OrekitException if different parameters have the same name
     */
    public ParameterDriversList getPropagatorParametersDrivers(final boolean estimatedOnly)
        throws OrekitException {

        if (estimatedOnly) {
            final ParameterDriversList estimated = new ParameterDriversList();
            for (final DelegatingDriver delegating : propagatorBuilder.getPropagationParametersDrivers().getDrivers()) {
                if (delegating.isSelected()) {
                    for (final ParameterDriver driver : delegating.getRawDrivers()) {
                        estimated.add(driver);
                    }
                }
            }
            return estimated;
        } else {
            return propagatorBuilder.getPropagationParametersDrivers();
        }

    }

    /** Get the measurements parameters supported by this estimator (including measurements and modifiers).
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return measurements parameters supported by this estimator
     * @exception OrekitException if different parameters have the same name
     */
    public ParameterDriversList getMeasurementsParametersDrivers(final boolean estimatedOnly)
        throws OrekitException {

        final ParameterDriversList parameters =  new ParameterDriversList();
        for (final  ObservedMeasurement<?> measurement : measurements) {
            for (final ParameterDriver driver : measurement.getParametersDrivers()) {
                if ((!estimatedOnly) || driver.isSelected()) {
                    parameters.add(driver);
                }
            }
        }

        parameters.sort();

        return parameters;

    }

    /**
     * Set convergence threshold.
     * <p>
     * The convergence used for estimation is based on the estimated
     * parameters {@link ParameterDriver#getNormalizedValue() normalized values}.
     * Convergence is considered to have been reached when the difference
     * between previous and current normalized value is less than the
     * convergence threshold for all parameters. The same value is used
     * for all parameters since they are normalized and hence dimensionless.
     * </p>
     * <p>
     * Normalized values are computed as {@code (current - reference)/scale},
     * so convergence is reached when the following condition holds for
     * all estimated parameters:
     * {@code |current[i] - previous[i]| <= threshold * scale[i]}
     * </p>
     * <p>
     * So the convergence threshold specified here can be considered as
     * a multiplication factor applied to scale. Since for all parameters
     * the scale is often small (typically about 1 m for orbital positions
     * for example), then the threshold should not be too small. A value
     * of 10⁻³ is often quite accurate.
     *
     * @param parametersConvergenceThreshold convergence threshold on
     * normalized parameters (dimensionless, related to parameters scales)
     * @see EvaluationRmsChecker
     */
    public void setParametersConvergenceThreshold(final double parametersConvergenceThreshold) {
        this.parametersConvergenceThreshold = parametersConvergenceThreshold;
    }

    /** Estimate the orbital, propagation and measurements parameters.
     * <p>
     * The initial guess for all parameters must have been set before calling this method
     * using {@link #getOrbitalParametersDrivers(boolean)}, {@link #getPropagatorParametersDrivers(boolean)},
     * and {@link #getMeasurementsParametersDrivers(boolean)} and then {@link ParameterDriver#setValue(double)
     * setting the values} of the parameters.
     * </p>
     * <p>
     * After this method returns, the estimated parameters can be retrieved using
     * {@link #getOrbitalParametersDrivers(boolean)}, {@link #getPropagatorParametersDrivers(boolean)},
     * and {@link #getMeasurementsParametersDrivers(boolean)} and then {@link ParameterDriver#getValue()
     * getting the values} of the parameters.
     * </p>
     * <p>
     * As a convenience, the method also returns a fully configured and ready to use
     * propagator set up with all the estimated values.
     * </p>
     * @return propagator configured with estimated orbit as initial state, and all
     * propagator estimated parameters also set
     * @exception OrekitException if there is a conflict in parameters names
     * or if orbit cannot be determined
     */
    public NumericalPropagator estimate() throws OrekitException {

        // get all estimated parameters
        final ParameterDriversList estimatedOrbitalParameters      = getOrbitalParametersDrivers(true);
        final ParameterDriversList estimatedPropagatorParameters   = getPropagatorParametersDrivers(true);
        final ParameterDriversList estimatedMeasurementsParameters = getMeasurementsParametersDrivers(true);

        // create start point
        final double[] start = new double[estimatedOrbitalParameters.getNbParams() +
                                          estimatedPropagatorParameters.getNbParams() +
                                          estimatedMeasurementsParameters.getNbParams()];
        int iStart = 0;
        for (final ParameterDriver driver : estimatedOrbitalParameters.getDrivers()) {
            start[iStart++] = driver.getNormalizedValue();
        }
        for (final ParameterDriver driver : estimatedPropagatorParameters.getDrivers()) {
            start[iStart++] = driver.getNormalizedValue();
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            start[iStart++] = driver.getNormalizedValue();
        }
        lsBuilder.start(start);

        // create target (which is an array set to 0, as we compute weighted residuals ourselves)
        int p = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {
            if (measurement.isEnabled()) {
                p += measurement.getDimension();
            }
        }
        final double[] target = new double[p];
        lsBuilder.target(target);

        // set up the model
        final ModelObserver modelObserver = new ModelObserver() {
            /** {@inheritDoc} */
            @Override
            public void modelCalled(final Orbit newOrbit,
                                    final Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> newEstimations) {
                BatchLSEstimator.this.orbit       = newOrbit;
                BatchLSEstimator.this.estimations = newEstimations;
            }
        };
        final Model model = new Model(propagatorBuilder, measurements, estimatedMeasurementsParameters,
                                      modelObserver);
        lsBuilder.model(model);

        // add a validator for orbital parameters
        lsBuilder.parameterValidator(new Validator(estimatedOrbitalParameters,
                                                   estimatedPropagatorParameters,
                                                   estimatedMeasurementsParameters));

        lsBuilder.checker(new ConvergenceChecker<LeastSquaresProblem.Evaluation>() {
            /** {@inheritDoc} */
            @Override
            public boolean converged(final int iteration,
                                     final LeastSquaresProblem.Evaluation previous,
                                     final LeastSquaresProblem.Evaluation current) {
                final double lInf = current.getPoint().getLInfDistance(previous.getPoint());
                return lInf <= parametersConvergenceThreshold;
            }
        });

        // set up the problem to solve
        final LeastSquaresProblem problem = new TappedLSProblem(lsBuilder.build(),
                                                                model,
                                                                estimatedOrbitalParameters,
                                                                estimatedPropagatorParameters,
                                                                estimatedMeasurementsParameters);

        try {

            // solve the problem
            optimum = optimizer.optimize(problem);

            // create a new configured propagator with all estimated parameters
            return model.createPropagator(optimum.getPoint());

        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        } catch (OrekitExceptionWrapper oew) {
            throw oew.getException();
        }

    }

    /** Get the last estimations performed.
     * @return last estimations performed
     */
    public Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> getLastEstimations() {
        return Collections.unmodifiableMap(estimations);
    }

    /** Get the optimum found.
     * @return optimum found after last call to {@link #estimate()}
     */
    public Optimum getOptimum() {
        return optimum;
    }

    /** Get the number of iterations used for last estimation.
     * @return number of iterations used for last estimation
     * @see #setMaxIterations(int)
     */
    public int getIterationsCount() {
        return iterationsCounter.getCount();
    }

    /** Get the number of evaluations used for last estimation.
     * @return number of evaluations used for last estimation
     * @see #setMaxEvaluations(int)
     */
    public int getEvaluationsCount() {
        return evaluationsCounter.getCount();
    }

    /** Wrapper used to tap the various counters. */
    private class TappedLSProblem implements LeastSquaresProblem {

        /** Underlying problem. */
        private final LeastSquaresProblem problem;

        /** Multivariate function model. */
        private final Model model;

        /** Estimated orbital parameters. */
        private final ParameterDriversList estimatedOrbitalParameters;

        /** Estimated propagator parameters. */
        private final ParameterDriversList estimatedPropagatorParameters;

        /** Estimated measurements parameters. */
        private final ParameterDriversList estimatedMeasurementsParameters;

        /** Simple constructor.
         * @param problem underlying problem
         * @param model multivariate function model
         * @param estimatedOrbitalParameters estimated orbital parameters
         * @param estimatedPropagatorParameters estimated propagator parameters
         * @param estimatedMeasurementsParameters estimated measurements parameters
         */
        TappedLSProblem(final LeastSquaresProblem problem,
                        final Model model,
                        final ParameterDriversList estimatedOrbitalParameters,
                        final ParameterDriversList estimatedPropagatorParameters,
                        final ParameterDriversList estimatedMeasurementsParameters) {
            this.problem                         = problem;
            this.model                           = model;
            this.estimatedOrbitalParameters      = estimatedOrbitalParameters;
            this.estimatedPropagatorParameters   = estimatedPropagatorParameters;
            this.estimatedMeasurementsParameters = estimatedMeasurementsParameters;
        }

        /** {@inheritDoc} */
        @Override
        public Incrementor getEvaluationCounter() {
            // tap the evaluations counter
            BatchLSEstimator.this.evaluationsCounter = problem.getEvaluationCounter();
            model.setEvaluationsCounter(BatchLSEstimator.this.evaluationsCounter);
            return BatchLSEstimator.this.evaluationsCounter;
        }

        /** {@inheritDoc} */
        @Override
        public Incrementor getIterationCounter() {
            // tap the iterations counter
            BatchLSEstimator.this.iterationsCounter = problem.getIterationCounter();
            model.setIterationsCounter(BatchLSEstimator.this.iterationsCounter);
            return BatchLSEstimator.this.iterationsCounter;
        }

        /** {@inheritDoc} */
        @Override
        public ConvergenceChecker<Evaluation> getConvergenceChecker() {
            return problem.getConvergenceChecker();
        }

        /** {@inheritDoc} */
        @Override
        public RealVector getStart() {
            return problem.getStart();
        }

        /** {@inheritDoc} */
        @Override
        public int getObservationSize() {
            return problem.getObservationSize();
        }

        /** {@inheritDoc} */
        @Override
        public int getParameterSize() {
            return problem.getParameterSize();
        }

        /** {@inheritDoc} */
        @Override
        public Evaluation evaluate(final RealVector point) {

            // perform the evaluation
            final Evaluation evaluation = problem.evaluate(point);

            // notify the observer
            if (observer != null) {
                try {
                    observer.evaluationPerformed(iterationsCounter.getCount(),
                                                 evaluationsCounter.getCount(),
                                                 orbit,
                                                 estimatedOrbitalParameters,
                                                 estimatedPropagatorParameters,
                                                 estimatedMeasurementsParameters,
                                                 new Provider(),
                                                 evaluation);
                } catch (OrekitException oe) {
                    throw new OrekitExceptionWrapper(oe);
                }
            }

            return evaluation;

        }

    }

    /** Provider for evaluations. */
    private class Provider implements EstimationsProvider {

        /** Sorted estimations. */
        private EstimatedMeasurement<?>[] sortedEstimations;

        /** {@inheritDoc} */
        @Override
        public int getNumber() {
            return estimations.size();
        }

        /** {@inheritDoc} */
        @Override
        public EstimatedMeasurement<?> getEstimatedMeasurement(final int index)
            throws OrekitException {

            // safety checks
            if (index < 0 || index >= estimations.size()) {
                throw new OrekitException(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE,
                                          index, 0, estimations.size());
            }

            if (sortedEstimations == null) {

                // lazy evaluation of the sorted array
                sortedEstimations = new EstimatedMeasurement<?>[estimations.size()];
                int i = 0;
                for (final Map.Entry<ObservedMeasurement<?>, EstimatedMeasurement<?>> entry : estimations.entrySet()) {
                    sortedEstimations[i++] = entry.getValue();
                }

                // sort the array chronologically
                Arrays.sort(sortedEstimations, 0, sortedEstimations.length,
                            new ChronologicalComparator());

            }

            return sortedEstimations[index];

        }

    }

    /** Validator for estimated parameters. */
    private static class Validator implements ParameterValidator {

        /** Estimated orbital parameters. */
        private final ParameterDriversList estimatedOrbitalParameters;

        /** Estimated propagator parameters. */
        private final ParameterDriversList estimatedPropagatorParameters;

        /** Estimated measurements parameters. */
        private final ParameterDriversList estimatedMeasurementsParameters;

        /** Simple constructor.
         * @param estimatedOrbitalParameters estimated orbital parameters
         * @param estimatedPropagatorParameters estimated propagator parameters
         * @param estimatedMeasurementsParameters estimated measurements parameters
         */
        Validator(final ParameterDriversList estimatedOrbitalParameters,
                  final ParameterDriversList estimatedPropagatorParameters,
                  final ParameterDriversList estimatedMeasurementsParameters) {
            this.estimatedOrbitalParameters      = estimatedOrbitalParameters;
            this.estimatedPropagatorParameters   = estimatedPropagatorParameters;
            this.estimatedMeasurementsParameters = estimatedMeasurementsParameters;
        }

        /** {@inheritDoc} */
        @Override
        public RealVector validate(final RealVector params)
            throws OrekitExceptionWrapper {

            try {
                int i = 0;
                for (final ParameterDriver driver : estimatedOrbitalParameters.getDrivers()) {
                    // let the parameter handle min/max clipping
                    driver.setNormalizedValue(params.getEntry(i));
                    params.setEntry(i++, driver.getNormalizedValue());
                }
                for (final ParameterDriver driver : estimatedPropagatorParameters.getDrivers()) {
                    // let the parameter handle min/max clipping
                    driver.setNormalizedValue(params.getEntry(i));
                    params.setEntry(i++, driver.getNormalizedValue());
                }
                for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
                    // let the parameter handle min/max clipping
                    driver.setNormalizedValue(params.getEntry(i));
                    params.setEntry(i++, driver.getNormalizedValue());
                }

                return params;
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }
    }

}
