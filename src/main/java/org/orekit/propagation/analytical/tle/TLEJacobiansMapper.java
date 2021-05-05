/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.analytical.tle;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalJacobiansMapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.0
 * @see org.orekit.propagation.analytical.tle.TLEPartialDerivativesEquations
 * @see org.orekit.propagation.analytical.tle.TLEPropagator
 * @see SpacecraftState#getAdditionalState(String)
 * @see org.orekit.propagation.AbstractPropagator
 */
public class TLEJacobiansMapper extends AbstractAnalyticalJacobiansMapper {

    /** Placeholder for the derivatives of state. */
    private double[] stateTransition;

    /** Name. */
    private String name;

    /** Selected parameters for Jacobian computation. */
    private final ParameterDriversList parameters;

    /** Propagator computing state evolution. */
    private final TLEPropagator propagator;

    /** Simple constructor.
     * @param name name of the Jacobians
     * @param parameters selected parameters for Jacobian computation
     * @param propagator the propagator that will handle the orbit propagation
     */
    public TLEJacobiansMapper(final String name,
                              final ParameterDriversList parameters,
                              final TLEPropagator propagator) {

        super(name, parameters, TLEGradientConverter.FREE_STATE_PARAMETERS);
        this.parameters      = parameters;
        this.name            = name;
        this.propagator      = propagator;
        this.stateTransition = null;
    }

    /** {@inheritDoc} */
    @Override
    public void getStateJacobian(final SpacecraftState state, final double[][] dYdY0) {

        for (int i = 0; i < getStateDimension(); i++) {
            final double[] row = dYdY0[i];
            for (int j = 0; j < getStateDimension(); j++) {
                row[j] = stateTransition[i * getStateDimension() + j];
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void getParametersJacobian(final SpacecraftState state, final double[][] dYdP) {

        if (parameters.getNbParams() != 0) {

            for (int i = 0; i < getStateDimension(); i++) {
                final double[] row = dYdP[i];
                for (int j = 0; j < parameters.getNbParams(); j++) {
                    row[j] = stateTransition[getStateDimension() * getStateDimension() + (j + parameters.getNbParams() * i)];
                }
            }

        }
    }

    /** {@inheritDoc} */
    @Override
    @DefaultDataContext
    public void analyticalDerivatives(final SpacecraftState s) {

        final double[] p = s.getAdditionalState(name);
        if (stateTransition == null) {
            stateTransition = new double[p.length];
        }

        // initialize Jacobians to zero
        final int dim = getStateDimension();
        final int paramDim = parameters.getNbParams();
        final double[][] stateGrad = new double[dim][dim];
        final double[][] paramGrad = new double[dim][paramDim];
        final TLEGradientConverter converter = new TLEGradientConverter(propagator.getTLE());
        final FieldTLEPropagator<Gradient> gPropagator = converter.getPropagator();
        final Gradient[] gParameters = converter.getParameters(gPropagator.getTLE());

        // Compute Jacobian
        final AbsoluteDate init = getInitialState().getDate();
        final AbsoluteDate end  = s.getDate();
        final double dt = end.durationFrom(init);
        final FieldOrbit<Gradient> orbit = gPropagator.propagateOrbit(gPropagator.getTLE().getDate().shiftedBy(dt), gParameters);
        final FieldKeplerianOrbit<Gradient> gOrbit = (FieldKeplerianOrbit<Gradient>) OrbitType.KEPLERIAN.convertType(orbit);

        final Gradient a = TLEGradientConverter.computeA(gOrbit.getKeplerianMeanMotion());
        final double[] derivativesA           = a.getGradient();
        final double[] derivativesE           = gOrbit.getE().getGradient();
        final double[] derivativesI           = gOrbit.getI().getGradient();
        final double[] derivativesRAAN        = gOrbit.getRightAscensionOfAscendingNode().getGradient();
        final double[] derivativesPA          = gOrbit.getPerigeeArgument().getGradient();
        final double[] derivativesMeanAnomaly = gOrbit.getMeanAnomaly().getGradient();

        // as mean motion was used to build the gradient, chain rule is applied to retrieve derivatives with respect to semi major axis
        final double dAdMeanMotion = derivativesA[0];

        // update Jacobian with respect to state
        addToRow(derivativesA,            0, stateGrad);
        addToRow(derivativesE,            1, stateGrad);
        addToRow(derivativesI,            2, stateGrad);
        addToRow(derivativesRAAN,         3, stateGrad);
        addToRow(derivativesPA,           4, stateGrad);
        addToRow(derivativesMeanAnomaly,  5, stateGrad);

        int index = converter.getFreeStateParameters();
        int parameterIndex = 0;
        for (ParameterDriver driver : propagator.getTLE().getParametersDrivers()) {
            if (driver.isSelected()) {
                paramGrad[0][parameterIndex] += derivativesA[index];
                paramGrad[1][parameterIndex] += derivativesE[index];
                paramGrad[2][parameterIndex] += derivativesI[index];
                paramGrad[3][parameterIndex] += derivativesRAAN[index];
                paramGrad[4][parameterIndex] += derivativesPA[index];
                paramGrad[5][parameterIndex] += derivativesMeanAnomaly[index];
                ++index;
            }
            ++parameterIndex;
        }

        // the previous derivatives correspond to state transition matrix with mean motion as 1rst element instead of semi major axis
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                stateTransition[j + dim * i] = stateGrad[i][j];

                // retrieving dElement/dA from dElement/dMeanMotion
                if (j == 0) {
                    stateTransition[j + dim * i] /= dAdMeanMotion;
                }
            }
        }
        final int columnTop = dim * dim;
        for (int k = 0; k < paramDim; k++) {
            for (int i = 0; i < dim; ++i) {
                stateTransition[columnTop + (i + dim * k)] = paramGrad[i][k];
            }
        }
    }

    /** Getter for initial propagator state.
     * @return the propagator initial state
     */
    public SpacecraftState getInitialState() {
        return propagator.getInitialState();
    }
}
