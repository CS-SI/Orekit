/* Copyright 2002-2020 CS GROUP
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

import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
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
 * @see org.orekit.propagation.semianalytical.dsst.DSSTPartialDerivativesEquations
 * @see org.orekit.propagation.semianalytical.dsst.DSSTPropagator
 * @see SpacecraftState#getAdditionalState(String)
 * @see org.orekit.propagation.AbstractPropagator
 */
public class TLEJacobiansMapper extends AbstractJacobiansMapper {

    /** State dimension, fixed to 6.
     */
    public static final int STATE_DIMENSION = 6;

    /** Name. */
    private String name;

    /** Selected parameters for Jacobian computation. */
    private final ParameterDriversList parameters;

    /** Propagator computing state evolution. */
    private final TLEPropagator propagator;

    /** Placeholder for the derivatives of state. */
    private double[] stateTransition;


    /** Simple constructor.
     * @param name name of the Jacobians
     * @param parameters selected parameters for Jacobian computation
     * @param propagator the propagator that will handle the orbit propagation
     * @param map parameters map
     */
    public TLEJacobiansMapper(final String name,
                        final ParameterDriversList parameters,
                        final TLEPropagator propagator,
                        final Map<ParameterDriver, Integer> map) {

        super(name, parameters);

        this.parameters      = parameters;
        this.name            = name;
        this.propagator      = propagator;

        stateTransition = null;

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] getConversionJacobian(final SpacecraftState state) {

        final double[][] identity = new double[STATE_DIMENSION][STATE_DIMENSION];

        for (int i = 0; i < STATE_DIMENSION; ++i) {
            identity[i][i] = 1.0;
        }

        return identity;

    }

    /** {@inheritDoc} */
    @Override
    public void setInitialJacobians(final SpacecraftState state, final double[][] dY1dY0,
                                    final double[][] dY1dP, final double[] p) {

        // map the converted state Jacobian to one-dimensional array
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                p[index++] = (i == j) ? 1.0 : 0.0;
            }
        }

        if (parameters.getNbParams() != 0) {

            // map the converted parameters Jacobian to one-dimensional array
            for (int i = 0; i < STATE_DIMENSION; ++i) {
                for (int j = 0; j < parameters.getNbParams(); ++j) {
                    p[index++] = dY1dP[i][j];
                }
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void getStateJacobian(final SpacecraftState state, final double[][] dYdY0) {

        for (int i = 0; i < STATE_DIMENSION; i++) {
            final double[] row = dYdY0[i];
            for (int j = 0; j < STATE_DIMENSION; j++) {
                row[j] = stateTransition[i * STATE_DIMENSION + j];
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    public void getParametersJacobian(final SpacecraftState state, final double[][] dYdP) {

        if (parameters.getNbParams() != 0) {

            // extract the additional state
            final double[] p = state.getAdditionalState(name);

            for (int i = 0; i < STATE_DIMENSION; i++) {
                final double[] row = dYdP[i];
                for (int j = 0; j < parameters.getNbParams(); j++) {
                    row[j] = p[STATE_DIMENSION * STATE_DIMENSION + (j + parameters.getNbParams() * i)];
                }
            }

        }

    }

    /** {@inheritDoc} */
    @Override
    public int getAdditionalStateDimension() {
        return STATE_DIMENSION * (STATE_DIMENSION + parameters.getNbParams());
    }

    /** Compute the derivatives of the orbital parameters with respect to orbital parameters.
     * @param s initial spacecraft state with respect to which calculate derivatives
     * @param dt propagation time to propagate initial state
     */
    public void computeDerivatives(final SpacecraftState s, final double dt) {

        final double[] p = s.getAdditionalState(name);
        if (stateTransition == null) {
            stateTransition = new double[p.length];
        }

        // initialize Jacobians to zero
        final int dim = 6;
        final double[][] grad = new double[dim][dim];
        final TLEGradientConverter converter = new TLEGradientConverter();
        final FieldTLE<Gradient> gTLE = converter.getGradientTLE(propagator.getTLE());
        final FieldTLEPropagator<Gradient> gPropagator = FieldTLEPropagator.selectExtrapolator(gTLE);

        // Compute Jacobian
        final FieldKeplerianOrbit<Gradient> gOrbit = (FieldKeplerianOrbit<Gradient>) OrbitType.KEPLERIAN.convertType(gPropagator.propagateOrbit(
                                                                                                        gPropagator.getTLE().getDate().shiftedBy(dt)));

        final Gradient a = TLEGradientConverter.computeA(gOrbit.getKeplerianMeanMotion());
        final double[] derivativesA           = a.getGradient();
        final double[] derivativesE           = gOrbit.getE().getGradient();
        final double[] derivativesI           = gOrbit.getI().getGradient();
        final double[] derivativesRAAN        = gOrbit.getRightAscensionOfAscendingNode().getGradient();
        final double[] derivativesPA          = gOrbit.getPerigeeArgument().getGradient();
        final double[] derivativesMeanAnomaly = gOrbit.getMeanAnomaly().getGradient();

        // as mean motion was used to build the gradient, chain derivative rule is applied to retrieve derivatives with respect to semi major axis
        final double dAdMeanMotion = derivativesA[0];

        // update Jacobian with respect to state
        addToRow(derivativesA,            0, grad);
        addToRow(derivativesE,            1, grad);
        addToRow(derivativesI,            2, grad);
        addToRow(derivativesRAAN,         3, grad);
        addToRow(derivativesPA,           4, grad);
        addToRow(derivativesMeanAnomaly,  5, grad);

        // the previous derivatives correspond to state transition matrix with mean motion as 1rst element instead of semi major axis
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                stateTransition[j + dim * i] += grad[i][j];

                // retrieving dElement/dA from dElement/dMeanMotion
                if (j == 0) {
                    stateTransition[j + dim * i] /= dAdMeanMotion;
                }
            }
        }
    }

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for a, 1 for e, 2 for i, 3 for RAAN, 4 for PA, 5 for M)
     * @param grad Jacobian of mean elements rate with respect to mean elements
     */

    private void addToRow(final double[] derivatives, final int index,
                          final double[][] grad) {

        for (int i = 0; i < 6; i++) {
            grad[index][i] += derivatives[i];
        }
    }

   /** Getter for initial propagator state.
    * @return the propagator initial state
    */

    public SpacecraftState getInitialState() {
        return propagator.getInitialState();
    }
}
