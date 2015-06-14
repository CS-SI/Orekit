/* Copyright 2002-2015 CS Systèmes d'Information
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
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math3.fitting.leastsquares.EvaluationRmsChecker;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Parameter;
import org.orekit.estimation.measurements.EvaluationModifier;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.conversion.PropagatorBuilder;


/** Least squares estimator for orbit determination.
 * @author Luc Maisonobe
 * @since 7.1
 */
public class BatchLSEstimator {

    /** Builder for propagator. */
    private final PropagatorBuilder propagatorBuilder;

    /** Parameters. */
    private final SortedSet<Parameter> parameters;

    /** Measurements. */
    private final List<Measurement> measurements;

    /** Solver for least squares problem. */
    private final LeastSquaresOptimizer optimizer;

    /** Builder for the least squares problem. */
    private final LeastSquaresBuilder lsBuilder;

    /** Low level view of the optimum. */
    private LeastSquaresProblem.Evaluation optimum;

    /** Simple constructor.
     * @param propagatorBuilder builder to user for propagation
     * @param optimizer solver for least squares problem
     */
    public BatchLSEstimator(final PropagatorBuilder propagatorBuilder,
                            final LeastSquaresOptimizer optimizer) {
        this.measurements      = new ArrayList<Measurement>();
        this.parameters        = new TreeSet<Parameter>();
        this.optimizer         = optimizer;
        this.lsBuilder         = new LeastSquaresBuilder();
        this.propagatorBuilder = propagatorBuilder;

        // our model computes value and Jacobian in one call,
        // so we don't use the lazy evaluation feature
        lsBuilder.lazyEvaluation(false);

        // we manage weight by ourselves, as we change them during
        // iterations (setting to 0 the identified outliers measurements)
        // so the least squares problem should not see our weights
        lsBuilder.weight(null);

    }

    /** Get the parameters supported by this estimator (including measurements and modifiers).
     * @return parameters supported by this estimator (including measurements and modifiers)
     */
    public SortedSet<Parameter> getSupportedParameters() {
        return Collections.unmodifiableSortedSet(parameters);
    }

    /** Add a measurement.
     * @param measurement measurement to add
     * @exception OrekitException if the measurement has a parameter
     * that is already used
     */
    public void addMeasurement(final Measurement measurement)
      throws OrekitException {

        // add the measurement
        measurements.add(measurement);

        // add parameters
        for (final EvaluationModifier modifier : measurement.getModifiers()) {
            for (final Parameter parameter : modifier.getSupportedParameters()) {
                if (parameters.contains(parameter)) {
                    // a parameter with this name already exists in the set,
                    // check if it is really the same parameter or a duplicated name
                    if (parameters.tailSet(parameter).first() != parameter) {
                        // we have two different parameters sharing the same name
                        throw new OrekitException(OrekitMessages.DUPLICATED_PARAMETER_NAME,
                                                  parameter.getName());
                    }
                } else {
                    parameters.add(parameter);
                }
            }
        }

    }

    /** Set the maximum number of iterations.
     * @param maxIterations maxIterations maximum number of iterations
     */
    public void setMaxIterations(final int maxIterations) {
        lsBuilder.maxIterations(maxIterations);
    }

    /**
     * Set convergence thresholds on RMS.
     * @param relTol the relative tolerance.
     * @param absTol the absolute tolerance.
     * @see EvaluationRmsChecker
     */
    public void setConvergenceThreshold(final double relTol, final double absTol) {
        lsBuilder.checker(new EvaluationRmsChecker(relTol, absTol));
    }

    /** Estimate the orbit and the parameters.
     * <p>
     * The estimated parameters are available using {@link #getParameters()}
     * </p>
     * @param initialGuess initial guess for the orbit
     * @return estimated orbit
     * @exception OrekitException if orbit cannot be determined
     */
    public Orbit estimate(final Orbit initialGuess) throws OrekitException {

        // compute problem dimension
        int n = 6;
        for (final Parameter parameter : parameters) {
            if (parameter.isEstimated()) {
                n += parameter.getDimension();
            }
        }

        // create start point
        final double[] start = new double[n];
        propagatorBuilder.getOrbitType().mapOrbitToArray(initialGuess,
                                                         propagatorBuilder.getPositionAngle(),
                                                         start);
        n = 6;
        for (final Parameter parameter : parameters) {
            if (parameter.isEstimated()) {
                System.arraycopy(parameter.getValue(), 0, start, n, parameter.getDimension());
                n += parameter.getDimension();
            }
        }
        lsBuilder.start(start);

        // create target
        int p = 0;
        for (final Measurement measurement : measurements) {
            if (measurement.isEnabled()) {
                p += measurement.getDimension();
            }
        }
        final double[] target = new double[p];
        p = 0;
        for (final Measurement measurement : measurements) {
            if (measurement.isEnabled()) {
                System.arraycopy(measurement.getObservedValue(), 0, target, p, measurement.getDimension());
                p += measurement.getDimension();
            }
        }
        lsBuilder.target(target);

        // set up the model
        final Model model = new Model(propagatorBuilder, initialGuess.getDate(), parameters, measurements);
        lsBuilder.model(model);

        // solve the problem
        try {
            optimum = optimizer.optimize(lsBuilder.build());
        } catch (OrekitExceptionWrapper oew) {
            throw oew.getException();
        }

        // extract the orbit (the parameters are also set to optimum as a side effect)
        return model.createPropagator(optimum.getPoint()).getInitialState().getOrbit();

    }

}
