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
package org.orekit.propagation.analytical;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversList;

/** Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Nicolas Fialton
 */
public class KeplerianJacobiansMapper extends AbstractJacobiansMapper {

    /** State dimension, fixed to 6. */
    public static final int STATE_DIMENSION = KeplerianGradientConverter.FREE_STATE_PARAMETERS;

    /** Name. */
    private String name;

    /** Propagator computing state evolution. */
    private final KeplerianPropagator propagator;

    /** Placeholder for the derivatives of state. */
    private double[] stateTransition;

    /** Simple constructor.
     * @param name name of the Jacobians
     * @param propagator the propagator that will handle the orbit propagation
     */
    public KeplerianJacobiansMapper(final String name,
                                    final KeplerianPropagator propagator) {

        super(name, new ParameterDriversList());

        this.name            = name;
        this.propagator      = propagator;

        stateTransition = null;

    }

    /** {@inheritDoc} */
    @Override
    public void setInitialJacobians(final SpacecraftState state,
                                    final double[][] dY1dY0,
                                    final double[][] dY1dP,
                                    final double[] p) {

        // map the converted state Jacobian to one-dimensional array
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                p[index++] = (i == j) ? 1.0 : 0.0;
            }
        }

        // No propagator parameters therefore there is no dY1dP

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

     // No propagator parameters therefore there is no dY1dP

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
        final int dim = STATE_DIMENSION;
        final double[][] stateGrad = new double[dim][dim];
        final KeplerianGradientConverter converter = new KeplerianGradientConverter(s, propagator);
        final FieldKeplerianPropagator<Gradient> gPropagator = converter.getPropagator();
        final Gradient[] gParameters = converter.getParameters();

        // Compute Jacobian
        final AbsoluteDate init = getInitialState().getDate();
        final AbsoluteDate end  = s.getDate();
        final double dt = end.durationFrom(init);
        final FieldOrbit<Gradient> orbit = gPropagator.propagateOrbit(gPropagator.getInitialState().getDate().shiftedBy(dt), gParameters);
        final FieldKeplerianOrbit<Gradient> gOrbit = (FieldKeplerianOrbit<Gradient>) OrbitType.KEPLERIAN.convertType(orbit);

        final double[] derivativesA           = gOrbit.getA().getGradient();
        final double[] derivativesE           = gOrbit.getE().getGradient();
        final double[] derivativesI           = gOrbit.getI().getGradient();
        final double[] derivativesRAAN        = gOrbit.getRightAscensionOfAscendingNode().getGradient();
        final double[] derivativesPA          = gOrbit.getPerigeeArgument().getGradient();
        final double[] derivativesTrueAnomaly = gOrbit.getTrueAnomaly().getGradient();

        // update Jacobian with respect to state
        addToRow(derivativesA,            0, stateGrad);
        addToRow(derivativesE,            1, stateGrad);
        addToRow(derivativesI,            2, stateGrad);
        addToRow(derivativesRAAN,         3, stateGrad);
        addToRow(derivativesPA,           4, stateGrad);
        addToRow(derivativesTrueAnomaly,  5, stateGrad);

        // the previous derivatives correspond to the state transition matrix
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                stateTransition[j + dim * i] = stateGrad[i][j];
            }
        }

    }

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for a, 1 for e, 2 for i, 3 for RAAN, 4 for PA, 5 for TrueAnomaly)
     * @param grad Jacobian of mean elements rate with respect to mean elements
     */
    private void addToRow(final double[] derivatives,
                          final int index,
                          final double[][] grad) {

        for (int i = 0; i < STATE_DIMENSION; i++) {
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
