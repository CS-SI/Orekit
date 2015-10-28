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

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.Parameter;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.Measurement;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.numerical.JacobiansMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.time.AbsoluteDate;

/** Bridge between {@link Measurement measurements} and {@link
 * org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem
 * least squares problems}.
 * @author Luc Maisonobe
 * @since 7.1
 */
class Model implements MultivariateJacobianFunction {

    /** Builder for propagator. */
    private final NumericalPropagatorBuilder propagatorBuilder;

    /** Propagator parameters. */
    private final List<String> propagatorParameters;

    /** Measurements. */
    private final List<Measurement<?>> measurements;

    /** Measurements parameters. */
    private final List<Parameter> measurementsParameters;

    /** Map for measurements parameters columns. */
    private final Map<String, Integer> parameterColumns;

    /** Last orbit. */
    private Orbit orbit;

    /** Last evaluations. */
    private final Map<Measurement<?>, Evaluation<?>> evaluations;

    /** Orbit date. */
    private final AbsoluteDate orbitDate;

    /** Iteration number. */
    private int iteration;

    /** Date of the first enabled measurement. */
    private AbsoluteDate firstDate;

    /** Date of the last enabled measurement. */
    private AbsoluteDate lastDate;

    /** Mapper for Jacobians. */
    private JacobiansMapper mapper;

    /** Model function value. */
    private RealVector value;

    /** Model function Jacobian. */
    private RealMatrix jacobian;

    /** Simple constructor.
     * @param propagatorBuilder builder to user for propagation
     * @param propagatorParameters propagator parameters
     * @param measurements measurements
     * @param measurementsParameters measurements parameters
     * @param orbitDate orbit date
     */
    Model(final NumericalPropagatorBuilder propagatorBuilder, final List<String> propagatorParameters,
          final List<Measurement<?>> measurements, final List<Parameter> measurementsParameters,
          final AbsoluteDate orbitDate) {

        this.propagatorBuilder      = propagatorBuilder;
        this.propagatorParameters   = propagatorParameters;
        this.measurements           = measurements;
        this.measurementsParameters = measurementsParameters;
        this.parameterColumns       = new HashMap<String, Integer>(measurementsParameters.size());
        this.evaluations            = new IdentityHashMap<Measurement<?>, Evaluation<?>>(measurements.size());
        this.orbitDate              = orbitDate;
        this.iteration              = 0;

        // allocate vector and matrix
        int rows = 0;
        for (final Measurement<?> measurement : measurements) {
            if (measurement.isEnabled()) {
                rows += measurement.getDimension();
            }
        }

        int columns = 6 + propagatorParameters.size();
        for (final Parameter parameter : measurementsParameters) {
            if (parameter.isEstimated()) {
                parameterColumns.put(parameter.getName(), columns);
                columns += parameter.getDimension();
            }
        }

        value    = new ArrayRealVector(rows);
        jacobian = MatrixUtils.createRealMatrix(rows, columns);

    }

    /** {@inheritDoc} */
    @Override
    public Pair<RealVector, RealMatrix> value(final RealVector point)
        throws OrekitExceptionWrapper {
        try {

            ++iteration;

            // set up the propagator
            final NumericalPropagator propagator = createPropagator(point);
            configureDerivatives(propagator);
            configureMeasurements(propagator);

            // reset value and Jacobian
            evaluations.clear();
            value.set(0.0);
            for (int i = 0; i < jacobian.getRowDimension(); ++i) {
                for (int j = 0; j < jacobian.getColumnDimension(); ++j) {
                    jacobian.setEntry(i, j, 0.0);
                }
            }

            // run the propagation, gathering residuals on the fly
            propagator.propagate(firstDate.shiftedBy(-1.0), lastDate.shiftedBy(+1.0));

            return new Pair<RealVector, RealMatrix>(value, jacobian);

        } catch (OrekitException oe) {
            throw new OrekitExceptionWrapper(oe);
        }
    }

    /** Get the iteration number.
     * @return iteration number
     */
    public int getIteration() {
        return iteration;
    }

    /** Get the last evaluated orbit.
     * @return last evaluated orbit
     */
    public Orbit getLastOrbit() {
        return orbit;
    }

    /** Get the last evaluations performed.
     * @return last evaluations performed
     */
    public Map<Measurement<?>, Evaluation<?>> getLastEvaluations() {
        return Collections.unmodifiableMap(evaluations);
    }

    /** Create the propagator and parameters corresponding to an evaluation point.
     * @param point evaluation point
     * @return a new propagator
     * @exception OrekitException if orbit cannot be created with the current point
     */
    private NumericalPropagator createPropagator(final RealVector point)
        throws OrekitException {

        // set up the propagator
        final double[] propagatorArray = new double[6 + propagatorParameters.size()];
        for (int i = 0; i < propagatorArray.length; ++i) {
            propagatorArray[i] = point.getEntry(i);
        }
        final NumericalPropagator propagator =
                        propagatorBuilder.buildPropagator(orbitDate, propagatorArray);
        orbit = propagator.getInitialState().getOrbit();

        // set up the measurement parameters
        int index = propagatorArray.length;
        for (final Parameter parameter : measurementsParameters) {
            if (parameter.isEstimated()) {
                final double[] parameterValue = new double[parameter.getDimension()];
                for (int i = 0; i < parameterValue.length; ++i) {
                    parameterValue[i] = point.getEntry(index++);
                }
                parameter.setValue(parameterValue);
            }
        }

        return propagator;

    }

    /** Configure the propagator to handle measurements.
     * @param propagator {@link Propagator} to configure
     */
    private void configureMeasurements(final Propagator propagator) {

        firstDate = AbsoluteDate.FUTURE_INFINITY;
        lastDate  = AbsoluteDate.PAST_INFINITY;

        // set up events to handle measurements
        int p = 0;
        for (final Measurement<?> measurement : measurements) {
            if (measurement.isEnabled()) {
                AbsoluteDate md = measurement.getDate();
                final Evaluation<?> previousEvaluation = evaluations.get(measurement);
                if (previousEvaluation != null) {
                    // pre-compensate signal transit time
                    md = md.shiftedBy(-previousEvaluation.getTimeOffset());
                }
                if (md.compareTo(firstDate) < 0) {
                    firstDate = md;
                }
                if (md.compareTo(lastDate) > 0) {
                    lastDate = md;
                }
                final MeasurementHandler mh = new MeasurementHandler(this, measurement, p);
                propagator.addEventDetector(new DateDetector(md).withHandler(mh));
                p += measurement.getDimension();
            }
        }

    }

    /** Configure the propagator to compute derivatives.
     * @param propagator {@link Propagator} to configure
     * @exception OrekitException if orbit cannot be created with the current point
     */
    private void configureDerivatives(final NumericalPropagator propagator)
        throws OrekitException {

        final String equationName = Model.class.getName() + "-derivatives";
        final PartialDerivativesEquations partials = new PartialDerivativesEquations(equationName, propagator);
        partials.selectParameters(propagatorParameters);

        // add the derivatives to the initial state
        final SpacecraftState rawState = propagator.getInitialState();
        final SpacecraftState stateWithDerivatives =
                        partials.setInitialJacobians(rawState, 6, propagatorParameters.size());
        propagator.resetInitialState(stateWithDerivatives);

        mapper = partials.getMapper();

    }

    /** Fetch a measurement that was evaluated during propagation.
     * @param index index of the measurement first component
     * @param evaluation measurement evaluation
     * @exception OrekitException if Jacobians cannot be computed
     */
    void fetchEvaluatedMeasurement(final int index, final Evaluation<?> evaluation)
        throws OrekitException {

        // compute weighted residuals
        evaluations.put(evaluation.getMeasurement(), evaluation);
        final double[] evaluated = evaluation.getValue();
        final double[] observed  = evaluation.getMeasurement().getObservedValue();
        final double[] sigma     = evaluation.getMeasurement().getTheoreticalStandardDeviation();
        final double[] weight    = evaluation.getCurrentWeight();
        for (int i = 0; i < evaluated.length; ++i) {
            value.setEntry(index + i, weight[i] * (evaluated[i] - observed[i]) / sigma[i]);
        }

        // partial derivatives of the current Cartesian coordinates with respect to current orbital state
        final double[][] aCY = new double[6][6];
        final Orbit currentOrbit = evaluation.getState().getOrbit();
        currentOrbit.getJacobianWrtParameters(propagatorBuilder.getPositionAngle(), aCY);
        final RealMatrix dCdY = new Array2DRowRealMatrix(aCY, false);

        // Jacobian of the measurement with respect to current orbital state
        final RealMatrix dMdC = new Array2DRowRealMatrix(evaluation.getStateDerivatives(), false);
        final RealMatrix dMdY = dMdC.multiply(dCdY);

        // Jacobian of the measurement with respect to initial orbital state
        final double[][] aYY0 = new double[6][6];
        mapper.getStateJacobian(evaluation.getState(), aYY0);
        final RealMatrix dYdY0 = new Array2DRowRealMatrix(aYY0, false);
        final RealMatrix dMdY0 = dMdY.multiply(dYdY0);
        for (int i = 0; i < dMdY0.getRowDimension(); ++i) {
            for (int j = 0; j < dMdY0.getColumnDimension(); ++j) {
                jacobian.setEntry(index + i, j, weight[i] * dMdY0.getEntry(i, j) / sigma[i]);
            }
        }

        if (!propagatorParameters.isEmpty()) {
            // Jacobian of the measurement with respect to propagator parameters
            final double[][] aYPp  = new double[6][propagatorParameters.size()];
            mapper.getParametersJacobian(evaluation.getState(), aYPp);
            final RealMatrix dYdPp = new Array2DRowRealMatrix(aYPp, false);
            final RealMatrix dMdPp = dMdY.multiply(dYdPp);
            for (int i = 0; i < dMdPp.getRowDimension(); ++i) {
                for (int j = 0; j < propagatorParameters.size(); ++j) {
                    jacobian.setEntry(index + i, 6 + j, weight[i] * dMdPp.getEntry(i, j) / sigma[i]);
                }
            }
        }

        // Jacobian of the measurement with respect to measurements parameters
        final Measurement<?> measurement = evaluation.getMeasurement();
        for (final Parameter parameter : measurement.getSupportedParameters()) {
            if (parameter.isEstimated()) {
                final double[][] aMPm = evaluation.getParameterDerivatives(parameter.getName());
                for (int i = 0; i < aMPm.length; ++i) {
                    for (int j = 0; j < aMPm[i].length; ++j) {
                        jacobian.setEntry(index + i, parameterColumns.get(parameter.getName()) + j,
                                          weight[i] * aMPm[i][j] / sigma[i]);
                    }
                }
            }
        }

    }

}
