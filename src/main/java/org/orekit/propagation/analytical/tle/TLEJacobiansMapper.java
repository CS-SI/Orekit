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
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractJacobiansMapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
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
public class TLEJacobiansMapper extends AbstractJacobiansMapper {

    /** State dimension, fixed to 6. */
    public static final int STATE_DIMENSION = 6;

    /** Selected parameters for Jacobian computation. */
    private final ParameterDriversList parameters;

    /** TLE propagator. */
    private final FieldTLEPropagator<Gradient> gPropagator;

    /** Parameters. */
    private final Gradient[] gParameters;

    /** Placeholder for the derivatives of state. */
    private double[] stateTransition;

    /** Simple constructor.
     * @param name name of the Jacobians
     * @param parameters selected parameters for Jacobian computation
     * @param propagator the propagator that will handle the orbit propagation
     */
    public TLEJacobiansMapper(final String name,
                              final ParameterDriversList parameters,
                              final TLEPropagator propagator) {
        super(name, parameters);

        // Initialize fields
        this.parameters      = parameters;
        this.stateTransition = null;

        // Intialize "field" propagator
        final TLEGradientConverter           converter   = new TLEGradientConverter(propagator);
        final FieldSpacecraftState<Gradient> gState      = converter.getState();
        this.gParameters = converter.getParameters(gState);
        this.gPropagator = converter.getPropagator(gState, gParameters);
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

            for (int i = 0; i < STATE_DIMENSION; i++) {
                final double[] row = dYdP[i];
                for (int j = 0; j < parameters.getNbParams(); j++) {
                    row[j] = stateTransition[STATE_DIMENSION * STATE_DIMENSION + (j + parameters.getNbParams() * i)];
                }
            }

        }

    }

    /** {@inheritDoc} */
    @Override
    public void analyticalDerivatives(final SpacecraftState s) {

        // Initialize Jacobians to zero
        final int dim = STATE_DIMENSION;
        final int paramDim = parameters.getNbParams();
        final double[][] stateGrad = new double[dim][dim];
        final double[][] paramGrad = new double[dim][paramDim];

        // Initialize matrix
        if (stateTransition == null) {
            stateTransition = s.getAdditionalState(getName());
        }

        // Compute Jacobian
        final AbsoluteDate target = s.getDate();
        final FieldAbsoluteDate<Gradient> init = gPropagator.getTLE().getDate();
        final double dt = target.durationFrom(init.toAbsoluteDate());
        final FieldOrbit<Gradient> gOrbit = gPropagator.propagateOrbit(init.shiftedBy(dt), gParameters);
        final FieldPVCoordinates<Gradient> gPv = gOrbit.getPVCoordinates();

        final double[] derivativesX   = gPv.getPosition().getX().getGradient();
        final double[] derivativesY   = gPv.getPosition().getY().getGradient();
        final double[] derivativesZ   = gPv.getPosition().getZ().getGradient();
        final double[] derivativesVx  = gPv.getVelocity().getX().getGradient();
        final double[] derivativesVy  = gPv.getVelocity().getY().getGradient();
        final double[] derivativesVz  = gPv.getVelocity().getZ().getGradient();

        // Update Jacobian with respect to state
        addToRow(derivativesX,  0, stateGrad);
        addToRow(derivativesY,  1, stateGrad);
        addToRow(derivativesZ,  2, stateGrad);
        addToRow(derivativesVx, 3, stateGrad);
        addToRow(derivativesVy, 4, stateGrad);
        addToRow(derivativesVz, 5, stateGrad);

        int index = TLEGradientConverter.FREE_STATE_PARAMETERS;
        int parameterIndex = 0;
        for (ParameterDriver driver : parameters.getDrivers()) {
            if (driver.isSelected()) {
                paramGrad[0][parameterIndex] += derivativesX[index];
                paramGrad[1][parameterIndex] += derivativesY[index];
                paramGrad[2][parameterIndex] += derivativesZ[index];
                paramGrad[3][parameterIndex] += derivativesVx[index];
                paramGrad[4][parameterIndex] += derivativesVy[index];
                paramGrad[5][parameterIndex] += derivativesVz[index];
                ++index;
            }
            ++parameterIndex;
        }

        // State derivatives
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                stateTransition[j + dim * i] = stateGrad[i][j];
            }
        }

        // Propagation parameters derivatives
        final int columnTop = dim * dim;
        for (int k = 0; k < paramDim; k++) {
            for (int i = 0; i < dim; ++i) {
                stateTransition[columnTop + (i + dim * k)] = paramGrad[i][k];
            }
        }

    }

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for X, 1 for Y, 2 for Z, 3 for Vx, 4 for Vy, 5 for Vz)
     * @param grad Jacobian of mean elements rate with respect to mean elements
     */
    private void addToRow(final double[] derivatives, final int index,
                          final double[][] grad) {
        for (int i = 0; i < 6; i++) {
            grad[index][i] += derivatives[i];
        }
    }

}
