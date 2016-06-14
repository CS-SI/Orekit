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
import org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem;
import org.hipparchus.util.Incrementor;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.EvaluationsProvider;
import org.orekit.estimation.measurements.Measurement;
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
    private final List<Measurement<?>> measurements;

    /** Solver for least squares problem. */
    private final LeastSquaresOptimizer optimizer;

    /** Convergence threshold on normalized parameters. */
    private double parametersConvergenceThreshold;

    /** Builder for the least squares problem. */
    private final LeastSquaresBuilder lsBuilder;

    /** Oberver for iterations. */
    private BatchLSObserver observer;

    /** Last evaluations. */
    private Map<Measurement<?>, Evaluation<?>> evaluations;

    /** Last orbit. */
    private Orbit orbit;

    /** Last least squares problem evaluation. */
    private LeastSquaresProblem.Evaluation lspEvaluation;

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
        this.measurements                   = new ArrayList<Measurement<?>>();
        this.optimizer                      = optimizer;
        this.parametersConvergenceThreshold = Double.NaN;
        this.lsBuilder                      = new LeastSquaresBuilder();
        this.evaluations                    = null;
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
    public void addMeasurement(final Measurement<?> measurement)
      throws OrekitException {
        measurements.add(measurement);
    }

    /** Set the maximum number of iterations.
     * @param maxIterations maxIterations maximum number of iterations
     */
    public void setMaxIterations(final int maxIterations) {
        lsBuilder.maxIterations(maxIterations);
    }

    /** Get the orbital parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return orbital parameters supported by this estimator
     * @exception OrekitException if different parameters have the same name
     */
    public ParameterDriversList getOrbitalParameters(final boolean estimatedOnly)
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
    public ParameterDriversList getPropagatorParameters(final boolean estimatedOnly)
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
    public ParameterDriversList getMeasurementsParameters(final boolean estimatedOnly)
        throws OrekitException {

        final ParameterDriversList parameters =  new ParameterDriversList();
        for (final  Measurement<?> measurement : measurements) {
            for (final ParameterDriver driver : measurement.getParametersDrivers()) {
                if ((!estimatedOnly) || driver.isSelected()) {
                    parameters.add(driver);
                }
            }
        }

        parameters.sort();

        return parameters;

    }

    /** Set the maximum number of model evaluations.
     * @param maxEvaluations maximum number of model evaluations
     */
    public void setMaxEvaluations(final int maxEvaluations) {
        lsBuilder.maxEvaluations(maxEvaluations);
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
     * Normalized values are computed as {@code (current - initial)/scale},
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
     * </>
     * @param parametersConvergenceThreshold convergence threshold on
     * normalized parameters (dimensionless, related to parameters scales)
     * @see EvaluationRmsChecker
     */
    public void setParametersConvergenceThreshold(final double parametersConvergenceThreshold) {
        this.parametersConvergenceThreshold = parametersConvergenceThreshold;
    }

    /** Estimate the orbit and the parameters.
     * <p>
     * The initial guess for all parameters must have been set before calling this method
     * using {@link #getOrbitalParameters(boolean)}, {@link #getPropagatorParameters(boolean)},
     * and {@link #getMeasurementsParameters(boolean)} and then {@link ParameterDriver#setValue(double)
     * setting the values} of the parameters.
     * </p>
     * <p>
     * After this method returns, the estimated parameters can be retrieved using
     * {@link #getOrbitalParameters(boolean)}, {@link #getPropagatorParameters(boolean)},
     * and {@link #getMeasurementsParameters(boolean)} and then {@link ParameterDriver#getValue()
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
        final ParameterDriversList estimatedOrbitalParameters      = getOrbitalParameters(true);
        final ParameterDriversList estimatedPropagatorParameters   = getPropagatorParameters(true);
        final ParameterDriversList estimatedMeasurementsParameters = getMeasurementsParameters(true);

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
        for (final Measurement<?> measurement : measurements) {
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
                                    final Map<Measurement<?>, Evaluation<?>> newEvaluations) {
                BatchLSEstimator.this.orbit       = newOrbit;
                BatchLSEstimator.this.evaluations = newEvaluations;
            }
        };
        final Model model = new Model(propagatorBuilder, measurements, estimatedMeasurementsParameters,
                                      modelObserver);
        lsBuilder.model(model);

        lsBuilder.checker(new ConvergenceChecker<LeastSquaresProblem.Evaluation>() {
            /** {@inheritDoc} */
            @Override
            public boolean converged(final int iteration,
                                     final LeastSquaresProblem.Evaluation previous,
                                     final LeastSquaresProblem.Evaluation current) {
                // save the last evaluations
                lspEvaluation = current;

                // notify the observer
                if (observer != null) {
                    try {
                        observer.iterationPerformed(iterationsCounter.getCount(),
                                                    evaluationsCounter.getCount(),
                                                    orbit,
                                                    estimatedPropagatorParameters,
                                                    estimatedMeasurementsParameters,
                                                    new Provider(),
                                                    lspEvaluation);
                    } catch (OrekitException oe) {
                        throw new OrekitExceptionWrapper(oe);
                    }
                }

                final double lInf = current.getPoint().getLInfDistance(previous.getPoint());
                return lInf <= parametersConvergenceThreshold;

            }
        });
        try {

            // solve the problem
            optimizer.optimize(new TappedLSProblem(lsBuilder.build(), model));

            // create a new configured propagator with all estimated parameters
            return model.createPropagator(lspEvaluation.getPoint());

        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        } catch (OrekitExceptionWrapper oew) {
            throw oew.getException();
        }

    }

    /** Get the last evaluations performed.
     * @return last evaluations performed
     */
    public Map<Measurement<?>, Evaluation<?>> getLastEvaluations() {
        return Collections.unmodifiableMap(evaluations);
    }

    /** Get the last {@link org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem.Evaluation
     * least squares problem evaluation}.
     * @return last least squares problem evaluation
     */
    public LeastSquaresProblem.Evaluation getLastLSPEvaluation() {
        return lspEvaluation;
    }

    /** Get the number of iterations used for last estimation.
     * @return number of iterations used for last estimation
     */
    public int getIterationsCount() {
        return iterationsCounter.getCount();
    }

    /** Get the number of evaluations used for last estimation.
     * @return number of evaluations used for last estimation
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

        /** Simple constructor.
         * @param problem underlying problem
         * @param model multivariate function model
         */
        TappedLSProblem(final LeastSquaresProblem problem,
                        final Model model) {
            this.problem = problem;
            this.model   = model;
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
            return problem.evaluate(point);
        }

    }

    /** Provider for evaluations. */
    private class Provider implements EvaluationsProvider {

        /** Sorted evaluations. */
        private Evaluation<?>[] sortedEvaluations;

        /** {@inheritDoc} */
        @Override
        public int getNumber() {
            return evaluations.size();
        }

        /** {@inheritDoc} */
        @Override
        public Evaluation<?> getEvaluation(final int index)
            throws OrekitException {

            // safety checks
            if (index < 0 || index >= evaluations.size()) {
                throw new OrekitException(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE,
                                          index, 0, evaluations.size());
            }

            if (sortedEvaluations == null) {

                // lazy evaluation of the sorted array
                sortedEvaluations = new Evaluation<?>[evaluations.size()];
                int i = 0;
                for (final Map.Entry<Measurement<?>, Evaluation<?>> entry : evaluations.entrySet()) {
                    sortedEvaluations[i++] = entry.getValue();
                }

                // sort the array chronologically
                Arrays.sort(sortedEvaluations, 0, sortedEvaluations.length,
                            new ChronologicalComparator());

            }

            return sortedEvaluations[index];

        }

    }

}
