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
package org.orekit.estimation.leastsquares;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.linear.RealMatrix;
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
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationsProvider;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.AbstractPropagatorBuilder;
import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.TimeSpanMap.Span;


/** Least squares estimator for orbit determination.
 * <p>
 * The least squares estimator can be used with different orbit propagators
 * in Orekit. Current propagators list of usable propagators are {@link NumericalPropagator numerical},
 * {@link DSSTPropagator DSST}, {@link BrouwerLyddanePropagator Brouwer-Lyddane},
 * {@link EcksteinHechlerPropagator Eckstein-Hechler}, {@link TLEPropagator SGP4},
 * {@link KeplerianPropagator Keplerian}, and {@link Ephemeris ephemeris-based}.
 * </p>
 * @author Luc Maisonobe
 * @since 8.0
 */
public class BatchLSEstimator {

    /** Builders for propagator. */
    private final PropagatorBuilder[] builders;

    /** Measurements. */
    private final List<ObservedMeasurement<?>> measurements;

    /** Solver for least squares problem. */
    private final LeastSquaresOptimizer optimizer;

    /** Convergence checker. */
    private ConvergenceChecker<LeastSquaresProblem.Evaluation> convergenceChecker;

    /** Builder for the least squares problem. */
    private final LeastSquaresBuilder lsBuilder;

    /** Observer for iterations. */
    private BatchLSObserver observer;

    /** Last estimations. */
    private Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> estimations;

    /** Last orbits. */
    private Orbit[] orbits;

    /** Optimum found. */
    private Optimum optimum;

    /** Counter for the evaluations. */
    private Incrementor evaluationsCounter;

    /** Counter for the iterations. */
    private Incrementor iterationsCounter;

    /** Simple constructor.
     * <p>
     * If multiple {@link PropagatorBuilder propagator builders} are set up,
     * the orbits of several spacecrafts will be used simultaneously.
     * This is useful if the propagators share some model or measurements
     * parameters (typically pole motion, prime meridian correction or
     * ground stations positions).
     * </p>
     * <p>
     * Setting up multiple {@link PropagatorBuilder propagator builders} is
     * also useful when inter-satellite measurements are used, even if only one
     * of the orbit is estimated and the other ones are fixed. This is typically
     * used when very high accuracy GNSS measurements are needed and the
     * navigation bulletins are not considered accurate enough and the navigation
     * constellation must be propagated numerically.
     * </p>
     * @param optimizer solver for least squares problem
     * @param propagatorBuilder builders to use for propagation
     */
    public BatchLSEstimator(final LeastSquaresOptimizer optimizer,
                            final PropagatorBuilder... propagatorBuilder) {

        this.builders                       = propagatorBuilder;
        this.measurements                   = new ArrayList<ObservedMeasurement<?>>();
        this.optimizer                      = optimizer;
        this.lsBuilder                      = new LeastSquaresBuilder();
        this.observer                       = null;
        this.estimations                    = null;
        this.orbits                         = new Orbit[builders.length];

        setParametersConvergenceThreshold(Double.NaN);

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
     */
    public void addMeasurement(final ObservedMeasurement<?> measurement) {
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
        for (int i = 0; i < builders.length; ++i) {
            final String suffix = builders.length > 1 ? "[" + i + "]" : null;
            for (final DelegatingDriver delegating : builders[i].getOrbitalParametersDrivers().getDrivers()) {
                if (delegating.isSelected() || !estimatedOnly) {
                    for (final ParameterDriver driver : delegating.getRawDrivers()) {
                        if (suffix != null && !driver.getName().endsWith(suffix)) {
                            // we add suffix only conditionally because the method may already have been called
                            // and suffixes may have already been appended
                            driver.setName(driver.getName() + suffix);
                        }
                        estimated.add(driver);
                    }
                }
            }
        }
        return estimated;

    }

    /** Get the propagator parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return propagator parameters supported by this estimator
     */
    public ParameterDriversList getPropagatorParametersDrivers(final boolean estimatedOnly) {

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

    /** Get the measurements parameters supported by this estimator (including measurements and modifiers).
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return measurements parameters supported by this estimator
     */
    public ParameterDriversList getMeasurementsParametersDrivers(final boolean estimatedOnly) {

        final ParameterDriversList parameters =  new ParameterDriversList();
        for (final  ObservedMeasurement<?> measurement : measurements) {
            for (final ParameterDriver driver : measurement.getParametersDrivers()) {
                if (!estimatedOnly || driver.isSelected()) {
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
     * </p>
     * <p>
     * Calling this method overrides any checker that could have been set
     * beforehand by calling {@link #setConvergenceChecker(ConvergenceChecker)}.
     * Both methods are mutually exclusive.
     * </p>
     *
     * @param parametersConvergenceThreshold convergence threshold on
     * normalized parameters (dimensionless, related to parameters scales)
     * @see #setConvergenceChecker(ConvergenceChecker)
     * @see EvaluationRmsChecker
     */
    public void setParametersConvergenceThreshold(final double parametersConvergenceThreshold) {
        setConvergenceChecker((iteration, previous, current) ->
                              current.getPoint().getLInfDistance(previous.getPoint()) <= parametersConvergenceThreshold);
    }

    /** Set a custom convergence checker.
     * <p>
     * Calling this method overrides any checker that could have been set
     * beforehand by calling {@link #setParametersConvergenceThreshold(double)}.
     * Both methods are mutually exclusive.
     * </p>
     * @param convergenceChecker convergence checker to set
     * @see #setParametersConvergenceThreshold(double)
     * @since 10.1
     */
    public void setConvergenceChecker(final ConvergenceChecker<LeastSquaresProblem.Evaluation> convergenceChecker) {
        this.convergenceChecker = convergenceChecker;
    }

    /** Estimate the orbital, propagation and measurements parameters.
     * <p>
     * The initial guess for all parameters must have been set before calling this method
     * using {@link #getOrbitalParametersDrivers(boolean)}, {@link #getPropagatorParametersDrivers(boolean)},
     * and {@link #getMeasurementsParametersDrivers(boolean)} and then {@link ParameterDriver#setValue(double)
     * setting the values} of the parameters.
     * </p>
     * <p>
     * For parameters whose reference date has not been set to a non-null date beforehand (i.e.
     * the parameters for which {@link ParameterDriver#getReferenceDate()} returns {@code null},
     * a default reference date will be set automatically at the start of the estimation to the
     * {@link AbstractPropagatorBuilder#getInitialOrbitDate() initial orbit date} of the first
     * propagator builder. For parameters whose reference date has been set to a non-null date,
     * this reference date is untouched.
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
     * <p>
     * For even more in-depth information, the {@link #getOptimum()} method provides detailed
     * elements (covariance matrix, estimated parameters standard deviation, weighted Jacobian, RMS,
     * χ², residuals and more).
     * </p>
     * @return propagators configured with estimated orbits as initial states, and all
     * propagators estimated parameters also set
     */
    public Propagator[] estimate() {

        // set reference date for all parameters that lack one (including the not estimated parameters)
        for (final ParameterDriver driver : getOrbitalParametersDrivers(false).getDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(builders[0].getInitialOrbitDate());
            }
        }
        for (final ParameterDriver driver : getPropagatorParametersDrivers(false).getDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(builders[0].getInitialOrbitDate());
            }
        }
        for (final ParameterDriver driver : getMeasurementsParametersDrivers(false).getDrivers()) {
            if (driver.getReferenceDate() == null) {
                driver.setReferenceDate(builders[0].getInitialOrbitDate());
            }
        }

        // get all estimated parameters
        final ParameterDriversList estimatedOrbitalParameters      = getOrbitalParametersDrivers(true);
        final ParameterDriversList estimatedPropagatorParameters   = getPropagatorParametersDrivers(true);
        final ParameterDriversList estimatedMeasurementsParameters = getMeasurementsParametersDrivers(true);

        // create start point
        final double[] start = new double[estimatedOrbitalParameters.getNbValuesToEstimate() +
                                          estimatedPropagatorParameters.getNbValuesToEstimate() +
                                          estimatedMeasurementsParameters.getNbValuesToEstimate()];

        int iStart = 0;
        for (final ParameterDriver driver : estimatedOrbitalParameters.getDrivers()) {
            Span<Double> span = driver.getValueSpanMap().getFirstSpan();
            start[iStart++] = driver.getNormalizedValue(span.getStart());
            for (int spanNumber = 0; spanNumber < driver.getNbOfValues() - 1; ++spanNumber) {
                span = driver.getValueSpanMap().getSpan(span.getEnd());
                start[iStart++] = driver.getNormalizedValue(span.getStart());
            }
        }
        for (final ParameterDriver driver : estimatedPropagatorParameters.getDrivers()) {
            Span<Double> span = driver.getValueSpanMap().getFirstSpan();
            start[iStart++] = driver.getNormalizedValue(span.getStart());
            for (int spanNumber = 0; spanNumber < driver.getNbOfValues() - 1; ++spanNumber) {
                span = driver.getValueSpanMap().getSpan(span.getEnd());
                start[iStart++] = driver.getNormalizedValue(span.getStart());
            }
        }
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            Span<Double> span = driver.getValueSpanMap().getFirstSpan();
            start[iStart++] = driver.getNormalizedValue(span.getStart());
            for (int spanNumber = 0; spanNumber < driver.getNbOfValues() - 1; ++spanNumber) {
                span = driver.getValueSpanMap().getSpan(span.getEnd());
                start[iStart++] = driver.getNormalizedValue(span.getStart());
            }
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
            public void modelCalled(final Orbit[] newOrbits,
                                    final Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> newEstimations) {
                BatchLSEstimator.this.orbits      = newOrbits;
                BatchLSEstimator.this.estimations = newEstimations;
            }
        };
        final AbstractBatchLSModel model = builders[0].buildLeastSquaresModel(builders, measurements, estimatedMeasurementsParameters, modelObserver);

        lsBuilder.model(model);

        // add a validator for orbital parameters
        lsBuilder.parameterValidator(new Validator(estimatedOrbitalParameters,
                                                   estimatedPropagatorParameters,
                                                   estimatedMeasurementsParameters));

        lsBuilder.checker(convergenceChecker);

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
            return model.createPropagators(optimum.getPoint());

        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        }
    }

    /** Get the last estimations performed.
     * @return last estimations performed
     */
    public Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> getLastEstimations() {
        return Collections.unmodifiableMap(estimations);
    }

    /** Get the optimum found.
     * <p>
     * The {@link Optimum} object contains detailed elements (covariance matrix, estimated
     * parameters standard deviation, weighted Jacobian, RMS, χ², residuals and more).
     * </p>
     * <p>
     * Beware that the returned object is the raw view from the underlying mathematical
     * library. At this raw level, parameters have {@link ParameterDriver#getNormalizedValue()
     * normalized values} whereas the space flight parameters have {@link ParameterDriver#getValue()
     * physical values} with their units. So there are {@link ParameterDriver#getScale() scaling
     * factors} to apply when using these elements.
     * </p>
     * @return optimum found after last call to {@link #estimate()}
     */
    public Optimum getOptimum() {
        return optimum;
    }

    /** Get the covariances matrix in space flight dynamics physical units.
     * <p>
     * This method retrieve the {@link
     * org.hipparchus.optim.nonlinear.vector.leastsquares.LeastSquaresProblem.Evaluation#getCovariances(double)
     * covariances} from the [@link {@link #getOptimum() optimum} and applies the scaling factors
     * to it in order to convert it from raw normalized values back to physical values.
     * </p>
     * @param threshold threshold to identify matrix singularity
     * @return covariances matrix in space flight dynamics physical units
     * @since 9.1
     */
    public RealMatrix getPhysicalCovariances(final double threshold) {
        final RealMatrix covariances;
        try {
            // get the normalized matrix
            covariances = optimum.getCovariances(threshold).copy();
        } catch (MathIllegalArgumentException miae) {
            // the problem is singular
            throw new OrekitException(miae);
        }

        // retrieve the scaling factors
        final double[] scale = new double[covariances.getRowDimension()];
        int index = 0;
        for (final ParameterDriver driver : getOrbitalParametersDrivers(true).getDrivers()) {
            for (int i = 0; i < driver.getNbOfValues(); ++i) {
                scale[index++] = driver.getScale();
            }
        }
        for (final ParameterDriver driver : getPropagatorParametersDrivers(true).getDrivers()) {
            for (int i = 0; i < driver.getNbOfValues(); ++i) {
                scale[index++] = driver.getScale();
            }
        }
        for (final ParameterDriver driver : getMeasurementsParametersDrivers(true).getDrivers()) {
            for (int i = 0; i < driver.getNbOfValues(); ++i) {
                scale[index++] = driver.getScale();
            }
        }

        // unnormalize the matrix, to retrieve physical covariances
        for (int i = 0; i < covariances.getRowDimension(); ++i) {
            for (int j = 0; j < covariances.getColumnDimension(); ++j) {
                covariances.setEntry(i, j, scale[i] * scale[j] * covariances.getEntry(i, j));
            }
        }

        return covariances;

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
        private final AbstractBatchLSModel model;

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
                        final AbstractBatchLSModel model,
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
                observer.evaluationPerformed(iterationsCounter.getCount(),
                                             evaluationsCounter.getCount(),
                                             orbits,
                                             estimatedOrbitalParameters,
                                             estimatedPropagatorParameters,
                                             estimatedMeasurementsParameters,
                                             new Provider(),
                                             evaluation);
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
        public EstimatedMeasurement<?> getEstimatedMeasurement(final int index) {

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

                // sort the array, primarily chronologically
                Arrays.sort(sortedEstimations, 0, sortedEstimations.length, Comparator.naturalOrder());

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
        public RealVector validate(final RealVector params) {

            int i = 0;
            for (final ParameterDriver driver : estimatedOrbitalParameters.getDrivers()) {
                // let the parameter handle min/max clipping
                if (driver.getNbOfValues() == 1) {
                    driver.setNormalizedValue(params.getEntry(i), null);
                    params.setEntry(i++, driver.getNormalizedValue(null));

                // If the parameter driver contains only 1 value to estimate over the all time range
                } else {
                    // initialization getting the value of the first Span
                    Span<Double> span = driver.getValueSpanMap().getFirstSpan();
                    driver.setNormalizedValue(params.getEntry(i), span.getStart());
                    params.setEntry(i++, driver.getNormalizedValue(span.getStart()));

                    for (int spanNumber = 0; spanNumber < driver.getNbOfValues() - 1; ++spanNumber) {
                        final AbsoluteDate modificationDate = span.getEnd();
                        // get next span, previousSpan.getEnd = span.getStart
                        span = driver.getValueSpanMap().getSpan(modificationDate);
                        driver.setNormalizedValue(params.getEntry(i), modificationDate);
                        params.setEntry(i++, driver.getNormalizedValue(modificationDate));
                    }
                }

            }
            for (final ParameterDriver driver : estimatedPropagatorParameters.getDrivers()) {
                // let the parameter handle min/max clipping
                if (driver.getNbOfValues() == 1) {
                    driver.setNormalizedValue(params.getEntry(i), null);
                    params.setEntry(i++, driver.getNormalizedValue(null));

                // If the parameter driver contains only 1 value to estimate over the all time range
                } else {
                    // initialization getting the value of the first Span
                    Span<Double> span = driver.getValueSpanMap().getFirstSpan();
                    driver.setNormalizedValue(params.getEntry(i), span.getStart());
                    params.setEntry(i++, driver.getNormalizedValue(span.getStart()));

                    for (int spanNumber = 0; spanNumber < driver.getNbOfValues() - 1; ++spanNumber) {
                        final AbsoluteDate modificationDate = span.getEnd();
                        // get next span, previousSpan.getEnd = span.getStart
                        span = driver.getValueSpanMap().getSpan(modificationDate);
                        driver.setNormalizedValue(params.getEntry(i), modificationDate);
                        params.setEntry(i++, driver.getNormalizedValue(modificationDate));
                    }
                }
            }
            for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
                // let the parameter handle min/max clipping
                if (driver.getNbOfValues() == 1) {
                    driver.setNormalizedValue(params.getEntry(i), null);
                    params.setEntry(i++, driver.getNormalizedValue(null));

                // If the parameter driver contains only 1 value to estimate over the all time range
                } else {
                    // initialization getting the value of the first Span
                    Span<Double> span = driver.getValueSpanMap().getFirstSpan();
                    driver.setNormalizedValue(params.getEntry(i), span.getStart());
                    params.setEntry(i++, driver.getNormalizedValue(span.getStart()));

                    for (int spanNumber = 0; spanNumber < driver.getNbOfValues() - 1; ++spanNumber) {
                        final AbsoluteDate modificationDate = span.getEnd();
                        // get next span, previousSpan.getEnd = span.getStart
                        span = driver.getValueSpanMap().getSpan(modificationDate);
                        driver.setNormalizedValue(params.getEntry(i), modificationDate);
                        params.setEntry(i++, driver.getNormalizedValue(modificationDate));
                    }
                }
            }
            return params;
        }
    }

}
