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
import java.util.HashMap;
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
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.OrbitValidator;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.EvaluationsProvider;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.ParameterDriver;


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

    /** Relative tolerance. */
    private double relativeTolerance;

    /** Absolute tolerance. */
    private double absoluteTolerance;

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

        this.propagatorBuilder      = propagatorBuilder;
        this.measurements           = new ArrayList<Measurement<?>>();
        this.optimizer              = optimizer;
        this.relativeTolerance      = Double.NaN;
        this.absoluteTolerance      = Double.NaN;
        this.lsBuilder              = new LeastSquaresBuilder();
        this.evaluations            = null;
        this.observer               = null;

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

    /** Check for parameteres names conflicts.
     * @exception OrekitException if different parameters have the same name
     */
    private void checkParameters() throws OrekitException {

        final Map<String, ParameterDriver> map = new HashMap<String, ParameterDriver>();
        for (final ParameterDriver parameter : propagatorBuilder.getParametersDrivers()) {

            final ParameterDriver existing = map.get(parameter.getName());
            if (existing != null) {
                // the name already exists
                if (existing != parameter) {
                    // it is a different parameter with the same name
                    throw new OrekitException(OrekitMessages.DUPLICATED_PARAMETER_NAME,
                                              parameter.getName());
                }
            } else {
                // it is a new parameter
                map.put(parameter.getName(), parameter);
            }

        }

        for (final  Measurement<?> measurement : measurements) {
            for (final ParameterDriver parameter : measurement.getParametersDrivers()) {

                final ParameterDriver existing = map.get(parameter.getName());
                if (existing != null) {
                    // the name already exists
                    if (existing != parameter) {
                        // it is a different parameter with the same name
                        throw new OrekitException(OrekitMessages.DUPLICATED_PARAMETER_NAME,
                                                  parameter.getName());
                    }
                } else {
                    // it is a new parameter
                    map.put(parameter.getName(), parameter);
                }

            }
        }

    }

    /** Get the propagator parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return propagator parameters supported by this estimator
     * @exception OrekitException if different parameters have the same name
     */
    public List<ParameterDriver> getPropagatorParameters(final boolean estimatedOnly)
        throws OrekitException {

        final List<ParameterDriver> parameters;
        if (estimatedOnly) {
            parameters = new ArrayList<ParameterDriver>();
            for (final ParameterDriver parameterDriver : propagatorBuilder.getParametersDrivers()) {
                if (parameterDriver.isEstimated()) {
                    parameterDriver.checkAndAddSelf(parameters);
                }
            }
        } else {
            parameters = new ArrayList<ParameterDriver>(propagatorBuilder.getParametersDrivers());
        }

        return parameters;

    }

    /** Get the measurements parameters supported by this estimator (including measurements and modifiers).
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return measurements parameters supported by this estimator
     * @exception OrekitException if different parameters have the same name
     */
    public List<ParameterDriver> getMeasurementsParameters(final boolean estimatedOnly)
        throws OrekitException {

        final List<ParameterDriver> parameters;
        if (estimatedOnly) {
            parameters = new ArrayList<ParameterDriver>();
            for (final  Measurement<?> measurement : measurements) {
                for (final ParameterDriver parameterDriver : measurement.getParametersDrivers()) {
                    if (parameterDriver.isEstimated()) {
                        parameterDriver.checkAndAddSelf(parameters);
                    }
                }
            }
        } else {
            parameters = new ArrayList<ParameterDriver>(propagatorBuilder.getParametersDrivers());
        }

        return parameters;

    }

    /** Set the maximum number of model evaluations.
     * @param maxEvaluations maximum number of model evaluations
     */
    public void setMaxEvaluations(final int maxEvaluations) {
        lsBuilder.maxEvaluations(maxEvaluations);
    }

    /**
     * Set convergence thresholds on RMS.
     * @param relTol the relative tolerance.
     * @param absTol the absolute tolerance.
     * @see EvaluationRmsChecker
     */
    public void setConvergenceThreshold(final double relTol, final double absTol) {
        this.relativeTolerance = relTol;
        this.absoluteTolerance = absTol;
    }

    /** Estimate the orbit and the parameters.
     * <p>
     * The estimated parameters are available using {@link #getPropagatorParameters(boolean)}
     * and {@link #getMeasurementsParameters(boolean)}.
     * </p>
     * @param initialGuess initial guess for the orbit
     * @return propagator configured with estimated orbit as initial state, and all
     * propagator estimated parameters also set
     * @exception OrekitException if there is a conflict in parameters names
     * or if orbit cannot be determined
     */
    public NumericalPropagator estimate(final Orbit initialGuess) throws OrekitException {

        checkParameters();

        // compute problem dimension:
        // orbital parameters + estimated propagator parameters + estimated measurements parameters
        final int                   nbOrbitalParameters      = 6;
        final List<ParameterDriver> estimatedPropagatorParameters = getPropagatorParameters(true);
        int dimensionPropagator   = 0;
        for (final ParameterDriver parameter : estimatedPropagatorParameters) {
            dimensionPropagator += parameter.getDimension();
        }
        final List<ParameterDriver> estimatedMeasurementsParameters = getMeasurementsParameters(true);
        int dimensionMeasurements = 0;
        for (final ParameterDriver parameter : estimatedMeasurementsParameters) {
            if (parameter.isEstimated()) {
                dimensionMeasurements += parameter.getDimension();
            }
        }
        final int dimension = nbOrbitalParameters + dimensionPropagator + dimensionMeasurements;

        // create start point
        final double[] start = new double[dimension];
        propagatorBuilder.getOrbitType().mapOrbitToArray(initialGuess,
                                                         propagatorBuilder.getPositionAngle(),
                                                         start);
        int index = nbOrbitalParameters;
        for (final ParameterDriver propagatorParameter : estimatedPropagatorParameters) {
            System.arraycopy(propagatorParameter.getValue(), 0, start, index, propagatorParameter.getDimension());
            index += propagatorParameter.getDimension();
        }
        for (final ParameterDriver parameter : estimatedMeasurementsParameters) {
            System.arraycopy(parameter.getValue(), 0, start, index, parameter.getDimension());
            index += parameter.getDimension();
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
        final Model model = new Model(propagatorBuilder, estimatedPropagatorParameters,
                                      measurements, estimatedMeasurementsParameters,
                                      initialGuess.getDate(), modelObserver);
        lsBuilder.model(model);

        // add a validator for orbital parameters
        lsBuilder.parameterValidator(OrbitValidator.getValidator(propagatorBuilder.getOrbitType()));

        lsBuilder.checker(new EvaluationRmsChecker(relativeTolerance, absoluteTolerance) {
            /** {@inheritDoc} */
            @Override
            public boolean converged(final int iteration,
                                     final LeastSquaresProblem.Evaluation previous,
                                     final LeastSquaresProblem.Evaluation current) {

                // save the last evaluations
                lspEvaluation = current;

                // notify the observer
                if (observer != null) {
                    observer.iterationPerformed(iterationsCounter.getCount(),
                                                evaluationsCounter.getCount(),
                                                orbit,
                                                estimatedPropagatorParameters,
                                                estimatedMeasurementsParameters,
                                                new Provider(),
                                                lspEvaluation);
                }

                return super.converged(iteration, previous, current);
            }
        });
        try {

            // solve the problem
            optimizer.optimize(new TappedLSProblem(lsBuilder.build(), model));

            // create a new configured propagtor with all estimated parameters
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
            if (index < 0 || index > evaluations.size()) {
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
