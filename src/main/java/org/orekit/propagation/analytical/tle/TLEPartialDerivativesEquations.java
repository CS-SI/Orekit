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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.AnalyticalJacobiansMapper;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Set of {@link AdditionalEquations additional equations} computing the partial derivatives
 * of the state (orbit) with respect to initial state.
 * <p>
 * This set of equations are automatically added to a {@link AbstractAnalyticalPropagator analytical propagator}
 * in order to compute partial derivatives of the orbit along with the orbit itself. This is
 * useful for example in orbit determination applications.
 * </p>
 * <p>
 * The partial derivatives with respect to initial state are dimension 6 (orbit only).
 * </p>
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.0
 */
public class TLEPartialDerivativesEquations implements AdditionalEquations {


    /** Propagator computing state evolution. */
    private final TLEPropagator propagator;

    /** Selected parameters for Jacobian computation. */
    private ParameterDriversList selected;

    /** Parameters map. */
    private Map<ParameterDriver, Integer> map;

    /** Name. */
    private final String name;

    /** Flag for Jacobian matrices initialization. */
    private boolean initialized;


    /** Simple constructor.
     * <p>
     * Upon construction, this set of equations is <em>automatically</em> added to
     * the propagator by calling its {@link
     * AbstractAnalyticalPropagator#addAdditionalEquations(AdditionalEquations)} method. So
     * there is no need to call this method explicitly for these equations.
     * </p>
     * @param name name of the partial derivatives equations
     * @param propagator the propagator that will handle the orbit propagation
     */
    public TLEPartialDerivativesEquations(final String name,
                                           final TLEPropagator propagator) {
        this.name                   = name;
        this.selected               = new ParameterDriversList();
        this.map                    = null;
        this.propagator             = propagator;
        this.initialized            = false;
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }


    /** Set the initial value of the Jacobian with respect to state and parameter.
     * <p>
     * This method is equivalent to call {@link #setInitialJacobians(SpacecraftState,
     * double[][], double[][])} with dYdY0 set to the identity matrix and dYdP set
     * to a zero matrix.
     * </p>
     * <p>
     * The force models parameters for which partial derivatives are desired,
     * <em>must</em> have been {@link ParameterDriver#setSelected(boolean) selected}
     * before this method is called, so proper matrices dimensions are used.
     * </p>
     * @param s0 initial state
     * @return state with initial Jacobians added
     * @see #getSelectedParameters()
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s0) {
        final int stateDimension = 6;
        final int nbParameters = 0;
        final double[][] dYdY0 = new double[stateDimension][stateDimension];
        final double[][] dYdP  = new double[stateDimension][nbParameters];
        for (int i = 0; i < stateDimension; ++i) {
            dYdY0[i][i] = 1.0;
        }
        return setInitialJacobians(s0, dYdY0, dYdP);
    }

    /** Set the initial value of the Jacobian with respect to state and parameter.
     * <p>
     * The returned state must be added to the propagator (it is not done
     * automatically, as the user may need to add more states to it).
     * </p>
     * @param s1 current state
     * @param dY1dY0 Jacobian of current state at time t₁ with respect
     * to state at some previous time t₀ (must be 6x6)
     * @param dY1dP Jacobian of current state at time t₁ with respect
     * to parameters (may be null if no parameters are selected)
     * @return state with initial Jacobians added
     * @see #getSelectedParameters()
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s1,
                                               final double[][] dY1dY0, final double[][] dY1dP) {


        // Check dimensions
        final int stateDim = dY1dY0.length;
        if (stateDim != 6 || stateDim != dY1dY0[0].length) {
            throw new OrekitException(OrekitMessages.STATE_JACOBIAN_NOT_6X6,
                                      stateDim, dY1dY0[0].length);
        }
        if (dY1dP != null && stateDim != dY1dP.length) {
            throw new OrekitException(OrekitMessages.STATE_AND_PARAMETERS_JACOBIANS_ROWS_MISMATCH,
                                      stateDim, dY1dP.length);
        }
        if ((dY1dP == null && selected.getNbParams() != 0) ||
            (dY1dP != null && selected.getNbParams() != dY1dP[0].length)) {
            throw new OrekitException(new OrekitException(OrekitMessages.INITIAL_MATRIX_AND_PARAMETERS_NUMBER_MISMATCH,
                                                          dY1dP == null ? 0 : dY1dP[0].length, selected.getNbParams()));
        }

        // store the matrices as a single dimension array
        initialized = true;
        final AnalyticalJacobiansMapper mapper = getMapper();
        final double[] p = new double[mapper.getAdditionalStateDimension()];
        mapper.setInitialJacobians(s1, dY1dY0, dY1dP, p);

        // set value in propagator
        return s1.addAdditionalState(name, p);

    }

    /** Get a mapper between two-dimensional Jacobians and one-dimensional additional state.
     * @return a mapper between two-dimensional Jacobians and one-dimensional additional state,
     * with the same name as the instance
     * @see #setInitialJacobians(SpacecraftState)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     */
    public AnalyticalJacobiansMapper getMapper() {
        if (!initialized) {
            throw new OrekitException(OrekitMessages.STATE_JACOBIAN_NOT_INITIALIZED);
        }
        return new AnalyticalJacobiansMapper(name, selected, propagator, map);
    }

    /** Get the selected parameters, in Jacobian matrix column order.
     * <p>
     * The force models parameters for which partial derivatives are desired,
     * <em>must</em> have been {@link ParameterDriver#setSelected(boolean) selected}
     * before this method is called, so the proper list is returned.
     * </p>
     * @return selected parameters, in Jacobian matrix column order which
     * is lexicographic order
     */
    public ParameterDriversList getSelectedParameters() {
        return selected;
    }

    /** {@inheritDoc} */
    public double[] computeDerivatives(final SpacecraftState s, final double[] pDot) {

        // initialize Jacobians to zero
        final int dim = 6;
        final double[][] dMeanElementRatedElement = new double[dim][dim];
        final TLEGradientConverter converter = new TLEGradientConverter(propagator);
        final FieldTLEPropagator<Gradient> gPropagator = converter.getPropagator();

        // Compute Jacobian
        final FieldKeplerianOrbit<Gradient> gOrbit = (FieldKeplerianOrbit<Gradient>) OrbitType.KEPLERIAN.convertType(gPropagator.propagateOrbit(
                                                                                                                     gPropagator.getTLE().getDate()));

        final double[] derivativesA           = gOrbit.getA().getGradient();
        final double[] derivativesE           = gOrbit.getE().getGradient();
        final double[] derivativesI           = gOrbit.getI().getGradient();
        final double[] derivativesRAAN        = gOrbit.getRightAscensionOfAscendingNode().getGradient();
        final double[] derivativesPA          = gOrbit.getPerigeeArgument().getGradient();
        final double[] derivativesMeanAnomaly = gOrbit.getMeanAnomaly().getGradient();

        // update Jacobian with respect to state
        addToRow(derivativesA,            0, dMeanElementRatedElement);
        addToRow(derivativesE,            1, dMeanElementRatedElement);
        addToRow(derivativesI,            2, dMeanElementRatedElement);
        addToRow(derivativesRAAN,         3, dMeanElementRatedElement);
        addToRow(derivativesPA,           4, dMeanElementRatedElement);
        addToRow(derivativesMeanAnomaly,  5, dMeanElementRatedElement);

        // The variational equations of the complete state Jacobian matrix have the following form:

        //                     [ Adot ] = [ dMeanElementRatedElement ] * [ A ]

        // The A matrix and its derivative (Adot) are 6 * 6 matrices

        // The following loops compute these expression taking care of the mapping of the
        // A matrix into the single dimension array p and of the mapping of the
        // Adot matrix into the single dimension array pDot.

        final double[] p = s.getAdditionalState(getName());

        for (int i = 0; i < dim; i++) {
            final double[] dMeanElementRatedElementi = dMeanElementRatedElement[i];
            for (int j = 0; j < dim; j++) {
                pDot[j + dim * i] =
                    dMeanElementRatedElementi[0] * p[j]           + dMeanElementRatedElementi[1] * p[j +     dim] + dMeanElementRatedElementi[2] * p[j + 2 * dim] +
                    dMeanElementRatedElementi[3] * p[j + 3 * dim] + dMeanElementRatedElementi[4] * p[j + 4 * dim] + dMeanElementRatedElementi[5] * p[j + 5 * dim];
            }
        }


        // these equations have no effect on the main propagator itself
        return null;

    }

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for a, 1 for ex, 2 for ey, 3 for hx, 4 for hy, 5 for l)
     * @param dMeanElementRatedElement Jacobian of mean elements rate with respect to mean elements
     */
    private void addToRow(final double[] derivatives, final int index,
                          final double[][] dMeanElementRatedElement) {

        for (int i = 0; i < 6; i++) {
            dMeanElementRatedElement[index][i] += derivatives[i];
        }

    }

}
