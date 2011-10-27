/* Copyright 2010-2011 Centre National d'Études Spatiales
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.propagation.numerical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.SpacecraftState;

/** Set of {@link AdditionalEquations additional equations} computing the partial derivatives
 * of the state (orbit) with respect to initial state and force models parameters.
 * <p>
 * This set of equations are automaticall added to a {@link NumericalPropagator numerical propagator}
 * in order to compute partial derivatives of the orbit along with the orbit itself. This is
 * useful for example in orbit determination applications.
 * </p>
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class PartialDerivativesEquations implements AdditionalEquations {

    /** Serializable UID. */
    private static final long serialVersionUID = -556926704905099805L;

    /** Selected parameters for Jacobian computation. */
    private NumericalPropagator propagator;

    /** Jacobians providers. */
    private final List<AccelerationJacobiansProvider> jacobiansProviders;

    /** List of parameters selected for Jacobians computation. */
    private List<ParameterConfiguration> selectedParameters;

    /** Name. */
    private String name;

    /** State vector dimension without additional parameters
     * (either 6 or 7 depending on mass derivatives being included or not). */
    private int stateDim;

    /** Parameters vector dimension. */
    private int paramDim;

    /** Step used for finite difference computation with respect to spacecraft position. */
    private double hPos;

    /** Boolean for force models / selected parameters consistency. */
    private boolean dirty = false;

    /** Jacobian of acceleration with respect to spacecraft position. */
    private transient double[][] dAccdPos;

    /** Jacobian of acceleration with respect to spacecraft velocity. */
    private transient double[][] dAccdVel;

    /** Jacobian of acceleration with respect to spacecraft mass. */
    private transient double[]   dAccdM;

    /** Jacobian of acceleration with respect to one force model parameter (array reused for all parameters). */
    private transient double[]   dAccdParam;

    /** Simple constructor.
     * <p>
     * Upon construction, this set of equations is <em>automatically</em> added to
     * the propagator by calling its {@link
     * NumericalPropagator#addAdditionalEquations(AdditionalEquations)} method. So
     * there is no need to call this method explicitly for these equations.
     * </p>
     * @param name name of the partial derivatives equations
     * @param propagator the propagator that will handle the orbit propagation
     * @exception OrekitException if a set of equations with the same name is already present
     */
    public PartialDerivativesEquations(final String name, final NumericalPropagator propagator)
        throws OrekitException {
        this.name = name;
        jacobiansProviders = new ArrayList<AccelerationJacobiansProvider>();
        dirty = true;
        this.propagator = propagator;
        selectedParameters = new ArrayList<ParameterConfiguration>();
        stateDim = -1;
        paramDim = -1;
        hPos     = Double.NaN;
        propagator.addAdditionalEquations(this);
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** Get the names of the available parameters in the propagator.
     * <p>
     * The names returned depend on the force models set up in the propagator,
     * including the Newtonian attraction from the central body.
     * </p>
     * @return available parameters
     */
    public List<String> getAvailableParameters() {
        final List<String> available = new ArrayList<String>();
        available.addAll(propagator.getNewtonianAttractionForceModel().getParametersNames());
        for (final ForceModel model : propagator.getForceModels()) {
            available.addAll(model.getParametersNames());
        }
        return available;
    }

    /** Select the parameters to consider for Jacobian processing.
     * <p>Parameters names have to be consistent with some
     * {@link ForceModel} added elsewhere.</p>
     * @param parameters parameters to consider for Jacobian processing
     * @see NumericalPropagator#addForceModel(ForceModel)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     * @see ForceModel
     * @see org.orekit.forces.Parameterizable
     */
    public void selectParameters(final String ... parameters) {

        selectedParameters.clear();
        for (String param : parameters) {
            selectedParameters.add(new ParameterConfiguration(param, Double.NaN));
        }

        dirty = true;

    }

    /** Select the parameters to consider for Jacobian processing.
     * <p>Parameters names have to be consistent with some
     * {@link ForceModel} added elsewhere.</p>
     * @param parameter parameter to consider for Jacobian processing
     * @param hP step to use for computing Jacobian column with respect to the specified parameter
     * @see NumericalPropagator#addForceModel(ForceModel)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     * @see ForceModel
     * @see org.orekit.forces.Parameterizable
     */
    public void selectParamAndStep(final String parameter, final double hP) {
        selectedParameters.add(new ParameterConfiguration(parameter, hP));
        dirty = true;
    }

    /** Set the step for finite differences with respect to spacecraft position.
     * @param hPosition step used for finite difference computation with respect to spacecraft position (m)
     */
    public void setSteps(final double hPosition) {
        this.hPos = hPosition;
    }

    /** Set the initial value of the Jacobian with respect to state and parameter.
     * <p>
     * This method is equivalent to call {@link #setInitialJacobians(SpacecraftState,
     * double[][], double[][])} with dYdY0 set to the identity matrix and dYdP set
     * to a zero matrix.
     * </p>
     * @param s0 initial state
     * @param stateDimension state dimension, must be either 6 for orbit only or 7 for orbit and mass
     * @param paramDimension parameters dimension
     * @exception OrekitException if the partial equation has not been registered in
     * the propagator or if matrices dimensions are incorrect
     * @see #selectedParameters
     * @see #selectParamAndStep(String, double)
     */
    public void setInitialJacobians(final SpacecraftState s0, final int stateDimension, final int paramDimension)
        throws OrekitException {
        final double[][] dYdY0 = new double[stateDimension][stateDimension];
        final double[][] dYdP  = new double[stateDimension][paramDimension];
        for (int i = 0; i < stateDimension; ++i) {
            dYdY0[i][i] = 1.0;
        }
        setInitialJacobians(s0, dYdY0, dYdP);
    }

    /** Set the initial value of the Jacobian with respect to state and parameter.
     * @param s1 current state
     * @param dY1dY0 Jacobian of current state at time t<sub>1</sub> with respect
     * to state at some previous time t<sub>0</sub> (may be either 6x6 for orbit
     * only or 7x7 for orbit and mass)
     * @param dY1dP Jacobian of current state at time t<sub>1</sub> with respect
     * to parameters (may be null if no parameters are selected)
     * @exception OrekitException if the partial equation has not been registered in
     * the propagator or if matrices dimensions are incorrect
     * @see #selectedParameters
     * @see #selectParamAndStep(String, double)
     */
    public void setInitialJacobians(final SpacecraftState s1,
                                    final double[][] dY1dY0, final double[][] dY1dP)
        throws OrekitException {

        // Check dimensions
        stateDim = dY1dY0.length;
        if ((stateDim < 6) || (stateDim > 7) || (stateDim != dY1dY0[0].length)) {
            throw new OrekitException(OrekitMessages.STATE_JACOBIAN_NEITHER_6X6_NOR_7X7,
                                      stateDim, dY1dY0[0].length);
        }
        if ((dY1dP != null) && (stateDim != dY1dP.length)) {
            throw new OrekitException(OrekitMessages.STATE_AND_PARAMETERS_JACOBIANS_ROWS_MISMATCH,
                                      stateDim, dY1dP.length);
        }

        paramDim = (dY1dP == null) ? 0 : dY1dP[0].length;

        // store the matrices as a single dimension array
        final JacobiansMapper mapper = getMapper();
        final double[] p = new double[mapper.getAdditionalStateDimension()];
        mapper.setInitialJacobians(s1, dY1dY0, dY1dP, p);

        // set value in propagator
        propagator.setInitialAdditionalState(name, p);

    }

    /** Get a mapper between two-dimensional Jacobians and one-dimensional additional state.
     * @return a mapper between two-dimensional Jacobians and one-dimensional additional state,
     * with the same name as the instance
     * @exception OrekitException if the initial Jacobians have not been initialized yet
     * @see #setInitialJacobians(SpacecraftState, int, int)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     */
    public JacobiansMapper getMapper() throws OrekitException {
        if (stateDim < 0) {
            throw new OrekitException(OrekitMessages.STATE_JACOBIAN_NOT_INITIALIZED);
        }
        return new JacobiansMapper(name, stateDim, paramDim,
                                   propagator.getOrbitType(),
                                   propagator.getPositionAngleType());
    }

    /** {@inheritDoc} */
    public void computeDerivatives(final SpacecraftState s, final TimeDerivativesEquations adder,
                                   final double[] p, final double[] pDot) throws OrekitException {

        final int dim = 3;

        // Lazy initialization
        if (dirty) {

            // if step has not been set by user, set a default value
            if (Double.isNaN(hPos)) {
                hPos = FastMath.sqrt(Precision.EPSILON) * s.getPVCoordinates().getPosition().getNorm();
            }

             // set up Jacobians providers
            jacobiansProviders.clear();
            for (final ForceModel model : propagator.getForceModels()) {
                if (model instanceof AccelerationJacobiansProvider) {

                    // the force model already provides the Jacobians by itself
                    jacobiansProviders.add((AccelerationJacobiansProvider) model);

                } else {

                    // wrap the force model to compute the Jacobians by finite differences
                    jacobiansProviders.add(new Jacobianizer(model, selectedParameters, hPos));

                }
            }
            jacobiansProviders.add(propagator.getNewtonianAttractionForceModel());

            // check all parameters are handled by at least one Jacobian provider
            for (final ParameterConfiguration param : selectedParameters) {
                final String parameterName = param.getParameterName();
                boolean found = false;
                for (final AccelerationJacobiansProvider provider : jacobiansProviders) {
                    if (provider.isSupported(parameterName)) {
                        param.setProvider(provider);
                        found = true;
                    }
                }
                if (!found) {
                    throw new OrekitException(OrekitMessages.UNKNOWN_PARAMETER, parameterName);
                }
            }

            // check the numbers of parameters and matrix size agree
            if (selectedParameters.size() != paramDim) {
                throw new OrekitException(OrekitMessages.INITIAL_MATRIX_AND_PARAMETERS_NUMBER_MISMATCH,
                                          paramDim, selectedParameters.size());
            }

            dAccdParam = new double[dim];
            dAccdPos   = new double[dim][dim];
            dAccdVel   = new double[dim][dim];
            dAccdM     = (stateDim > 6) ? new double[dim] : null;

            dirty = false;

        }

        // compute forces gradients dAccDState
        for (final double[] row : dAccdPos) {
            Arrays.fill(row, 0.0);
        }
        for (final double[] row : dAccdVel) {
            Arrays.fill(row, 0.0);
        }
        if (dAccdM != null) {
            Arrays.fill(dAccdM, 0.0);
        }
        for (final AccelerationJacobiansProvider jacobProv : jacobiansProviders) {
            jacobProv.addDAccDState(s, dAccdPos, dAccdVel, dAccdM);
        }

        // the variational equations of the complete state Jacobian matrix have the
        // following form for 7x7, i.e. when mass partial derivatives are also considered
        // (when mass is not considered, only the A, B, D and E matrices are used along
        // with their derivatives):

        // [      ] [      ] [      ]   [                ] [                ] [              ]   [   ] [   ] [   ]
        // [ Adot ] [ Bdot ] [ Cdot ]   [  dVel/dPos = 0 ] [ dVel/dVel = Id ] [  dVel/dm = 0 ]   [ A ] [ B ] [ C ]
        // [      ] [      ] [      ]   [                ] [                ] [              ]   [   ] [   ] [   ]
        // --------+--------+--- ----   ------------------+------------------+----------------   -----+-----+-----
        // [      ] [      ] [      ]   [                ] [                ] [              ]   [   ] [   ] [   ]
        // [ Ddot ] [ Edot ] [ Fdot ] = [    dAcc/dPos   ] [    dAcc/dVel   ] [   dAcc/dm    ] * [ D ] [ E ] [ F ]
        // [      ] [      ] [      ]   [                ] [                ] [              ]   [   ] [   ] [   ]
        // --------+--------+--- ----   ------------------+------------------+----------------   -----+-----+-----
        // [ Gdot ] [ Hdot ] [ Idot ]   [ dmDot/dPos = 0 ] [ dmDot/dVel = 0 ] [ dmDot/dm = 0 ]   [ G ] [ H ] [ I ]

        // The A, B, D and E sub-matrices and their derivatives (Adot ...) are 3x3 matrices,
        // the C and F sub-matrices and their derivatives (Cdot ...) are 3x1 matrices,
        // the G and H sub-matrices and their derivatives (Gdot ...) are 1x3 matrices,
        // the I sub-matrix and its derivative (Idot) are 1x1 matrices.

        // The expanded multiplication above can be rewritten to take into account
        // the fixed values found in the sub-matrices in the left factor. This leads to:

        //     [ Adot ] = [ D ]
        //     [ Bdot ] = [ E ]
        //     [ Cdot ] = [ F ]
        //     [ Ddot ] = [ dAcc/dPos ] * [ A ] + [ dAcc/dVel ] * [ D ] + [ dAcc/dm ] * [ G ]
        //     [ Edot ] = [ dAcc/dPos ] * [ B ] + [ dAcc/dVel ] * [ E ] + [ dAcc/dm ] * [ H ]
        //     [ Fdot ] = [ dAcc/dPos ] * [ C ] + [ dAcc/dVel ] * [ F ] + [ dAcc/dm ] * [ I ]
        //     [ Gdot ] = [ 0 ]
        //     [ Hdot ] = [ 0 ]
        //     [ Idot ] = [ 0 ]

        // The following loops compute these expressions taking care of the mapping of the
        // (A, B, ... I) matrices into the single dimension array p and of the mapping of the
        // (Adot, Bdot, ... Idot) matrices into the single dimension array pDot.

        // copy D, E and F into Adot, Bdot and Cdot
        System.arraycopy(p, dim * stateDim, pDot, 0, dim * stateDim);

        // compute Ddot, Edot and Fdot
        for (int i = 0; i < dim; ++i) {
            final double[] dAdPi = dAccdPos[i];
            final double[] dAdVi = dAccdVel[i];
            for (int j = 0; j < stateDim; ++j) {
                pDot[(dim + i) * stateDim + j] =
                    dAdPi[0] * p[j]                + dAdPi[1] * p[j +     stateDim] + dAdPi[2] * p[j + 2 * stateDim] +
                    dAdVi[0] * p[j + 3 * stateDim] + dAdVi[1] * p[j + 4 * stateDim] + dAdVi[2] * p[j + 5 * stateDim] +
                    ((dAccdM == null) ? 0.0 : dAccdM[i] * p[j + 6 * stateDim]);
            }
        }

        if (dAccdM != null) {
            // set Gdot, Hdot and Idot to 0
            Arrays.fill(pDot, 6 * stateDim, 7 * stateDim, 0.0);
        }

        for (int k = 0; k < paramDim; ++k) {

            // compute the acceleration gradient with respect to current parameter
            final ParameterConfiguration param = selectedParameters.get(k);
            final AccelerationJacobiansProvider provider = param.getProvider();
            Arrays.fill(dAccdParam, 0.0);
            provider.addDAccDParam(s, param.getParameterName(), dAccdParam);

            // the variational equations of the parameters Jacobian matrix are computed
            // one column at a time, they have the following form:
            // [      ]   [                ] [                ] [              ]   [   ]   [                  ]
            // [ Jdot ]   [  dVel/dPos = 0 ] [ dVel/dVel = Id ] [  dVel/dm = 0 ]   [ J ]   [  dVel/dParam = 0 ]
            // [      ]   [                ] [                ] [              ]   [   ]   [                  ]
            // --------   ------------------+------------------+----------------   -----   --------------------
            // [      ]   [                ] [                ] [              ]   [   ]   [                  ]
            // [ Kdot ] = [    dAcc/dPos   ] [    dAcc/dVel   ] [   dAcc/dm    ] * [ K ] + [    dAcc/dParam   ]
            // [      ]   [                ] [                ] [              ]   [   ]   [                  ]
            // --------   ------------------+------------------+----------------   -----   --------------------
            // [ Ldot ]   [ dmDot/dPos = 0 ] [ dmDot/dVel = 0 ] [ dmDot/dm = 0 ]   [ L ]   [ dmDot/dParam = 0 ]

            // The J and K sub-columns and their derivatives (Jdot ...) are 3 elements columns,
            // the L sub-colums and its derivative (Ldot) are 1 elements columns.

            // The expanded multiplication and addition above can be rewritten to take into
            // account the fixed values found in the sub-matrices in the left factor. This leads to:

            //     [ Jdot ] = [ K ]
            //     [ Kdot ] = [ dAcc/dPos ] * [ J ] + [ dAcc/dVel ] * [ K ] + [ dAcc/dm ] * [ L ] + [ dAcc/dParam ]
            //     [ Ldot ] = [ 0 ]

            // The following loops compute these expressions taking care of the mapping of the
            // (J, K, L) columns into the single dimension array p and of the mapping of the
            // (Jdot, Kdot, Ldot) columns into the single dimension array pDot.

            // copy K into Jdot
            final int columnTop = stateDim * stateDim + k;
            pDot[columnTop]                = p[columnTop + 3 * paramDim];
            pDot[columnTop +     paramDim] = p[columnTop + 4 * paramDim];
            pDot[columnTop + 2 * paramDim] = p[columnTop + 5 * paramDim];

            // compute Kdot
            for (int i = 0; i < dim; ++i) {
                final double[] dAdPi = dAccdPos[i];
                final double[] dAdVi = dAccdVel[i];
                pDot[columnTop + (dim + i) * paramDim] =
                    dAccdParam[i] +
                    dAdPi[0] * p[columnTop]                + dAdPi[1] * p[columnTop +     paramDim] + dAdPi[2] * p[columnTop + 2 * paramDim] +
                    dAdVi[0] * p[columnTop + 3 * paramDim] + dAdVi[1] * p[columnTop + 4 * paramDim] + dAdVi[2] * p[columnTop + 5 * paramDim] +
                    ((dAccdM == null) ? 0.0 : dAccdM[i] * p[columnTop + 6 * paramDim]);
            }

            if (dAccdM != null) {
                // set Ldot to 0
                pDot[columnTop + 6 * paramDim] = 0;
            }

        }

    }

}

