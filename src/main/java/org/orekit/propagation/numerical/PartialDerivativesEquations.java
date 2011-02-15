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
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.SpacecraftState;

/** Set of {@link AdditionalEquations additional equations} computing the partial derivatives
 * of the state (orbit) with respect to initial state and force models parameters.
 * <p>
 * This set of equations can be added to a {@link NumericalPropagator numerical propagator}
 * in order to compute partial derivatives of the orbit along with the orbit itself. This is
 * useful for example in orbit determination applications.
 * </p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class PartialDerivativesEquations implements AdditionalEquations {

    /** Serializable UID. */
    private static final long serialVersionUID = 8373349999733456541L;

    /** Selected parameters for jacobian computation. */
    private NumericalPropagator propagator;

    /** Jacobians providers. */
    private final List<AccelerationJacobiansProvider> jacobiansProviders;

    /** List of parameters selected for jacobians computation. */
    private List<ParameterConfiguration> selectedParameters;

    /** State vector dimension without additional parameters
     * (either 6 or 7 depending on mass derivatives being included or not). */
    private int stateDim;

    /** Parameters vector dimension. */
    private int paramDim;

    /** Step used for finite difference computation with respect to spacecraft position. */
    private double hPos;

    /** Step used for finite difference computation with respect to spacecraft velocity. */
    private double hVel;

    /** Step used for finite difference computation with respect to spacecraft mass. */
    private double hM;

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
     * @param propagator the propagator that will handle the orbit propagation
     */
    public PartialDerivativesEquations(final NumericalPropagator propagator) {
        jacobiansProviders = new ArrayList<AccelerationJacobiansProvider>();
        dirty = true;
        this.propagator = propagator;
        selectedParameters = new ArrayList<ParameterConfiguration>();
        stateDim = -1;
        paramDim = -1;
        hPos  = Double.NaN;
        hVel  = Double.NaN;
        hM = Double.NaN;
        propagator.addAdditionalEquations(this);
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

    /** Select the parameters to consider for jacobian processing.
     * <p>Parameters names have to be consistent with some
     * {@link ForceModel} added elsewhere.</p>
     * @param parameters parameters to consider for jacobian processing
     * @see NumericalPropagator#addForceModel(ForceModel)
     * @see #setInitialJacobians(double[][], double[][])
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

    /** Select the parameters to consider for jacobian processing.
     * <p>Parameters names have to be consistent with some
     * {@link ForceModel} added elsewhere.</p>
     * @param parameter parameter to consider for jacobian processing
     * @param hP step to use for computing jacobian column with respect to the specified parameter
     * @see NumericalPropagator#addForceModel(ForceModel)
     * @see #setInitialJacobians(double[][], double[][])
     * @see ForceModel
     * @see org.orekit.forces.Parameterizable
     */
    public void selectParamAndStep(final String parameter, final double hP) {
        selectedParameters.add(new ParameterConfiguration(parameter, hP));
        dirty = true;
    }

    /** Set the steps for finite differences with respect to spacecraft state.
     * @param hPosition step used for finite difference computation with respect to spacecraft position (m)
     * @param hVelocity step used for finite difference computation with respect to spacecraft velocity (m/s)
     * @param hMass step used for finite difference computation with respect to spacecraft mass (kg)
     */
    public void setSteps(final double hPosition, final double hVelocity, final double hMass) {
        this.hPos = hPosition;
        this.hVel = hVelocity;
        this.hM   = hMass;
    }

    /** Set the initial value of the jacobian with respect to state and parameter.
     * <p>
     * This method is equivalent to call {@link #setInitialJacobians(double[][], double[][])}
     * with dYdY0 set to the identity matrix and dYdP set to a zero matrix.
     * </p>
     * @param stateDimension state dimension, must be either 6 for orbit only or 7 for orbit and mass
     * @param paramDimension parameters dimension
     * @exception OrekitException if the partial equation has not been registered in
     * the propagator or if matrices dimensions are incorrect
     * @see #selectedParameters
     * @see #selectParamAndStep(String, double)
     */
    public void setInitialJacobians(final int stateDimension, final int paramDimension)
        throws OrekitException {
        final double[][] dYdY0 = new double[stateDimension][stateDimension];
        final double[][] dYdP  = new double[stateDimension][paramDimension];
        for (int i = 0; i < stateDimension; ++i) {
            dYdY0[i][i] = 1.0;
        }
        setInitialJacobians(dYdY0, dYdP);
    }

    /** Set the initial value of the jacobian with respect to state and parameter.
     * @param dYdY0 initial jacobian w.r to state (may be either 6x6 for orbit only
     * or 7x7 for orbit and mass)
     * @param dYdP initial jacobian w.r to parameter (may be null if no parameters are selected)
     * @exception OrekitException if the partial equation has not been registered in
     * the propagator or if matrices dimensions are incorrect
     * @see #selectedParameters
     * @see #selectParamAndStep(String, double)
     */
    public void setInitialJacobians(final double[][] dYdY0, final double[][] dYdP)
        throws OrekitException {

        // Check dimensions
        stateDim = dYdY0.length;
        if ((stateDim < 6) || (stateDim > 7) || (stateDim != dYdY0[0].length)) {
            throw new OrekitException(OrekitMessages.STATE_JACOBIAN_NEITHER_6X6_NOR_7X7,
                                      stateDim, dYdY0[0].length);
        }
        if ((dYdP != null) && (stateDim != dYdP.length)) {
            throw new OrekitException(OrekitMessages.STATE_AND_PARAMETERS_JACOBIANS_ROWS_MISMATCH,
                                      stateDim, dYdP.length);
        }

        paramDim = (dYdP == null) ? 0 : dYdP[0].length;

        // store the matrices in row major order as a single dimension array
        final double[] p = new double[stateDim * (stateDim + paramDim)];
        int index = 0;
        for (final double[] row : dYdY0) {
            System.arraycopy(row, 0, p, index, stateDim);
            index += stateDim;
        }

        if (dYdP != null) {
            for (final double[] row : dYdP) {
                System.arraycopy(row, 0, p, index, paramDim);
                index += paramDim;
            }
        }

        // set value in propagator
        propagator.setInitialAdditionalState(p, this);

    }

    /** Get the initial value of the jacobian with respect to state and parameter.
     * @param dYdY0 current jacobian w.r to state.
     * @param dYdP current jacobian w.r to parameter (may be null if no parameters are selected)
     * @exception OrekitException if the partial equation has not been registered in
     * the propagator
     */
    public void getCurrentJacobians(final double[][] dYdY0, final double[][] dYdP)
        throws OrekitException {

        // get current state from propagator
        final double[] p = propagator.getCurrentAdditionalState(this);

        int index = 0;
        for (int i = 0; i < stateDim; i++) {
            System.arraycopy(p, index, dYdY0[i], 0, stateDim);
            index += stateDim;
        }

        if (paramDim != 0) {
            for (int i = 0; i < stateDim; i++) {
                System.arraycopy(p, index, dYdP[i], 0, paramDim);
                index += paramDim;
            }
        }

    }

    /** {@inheritDoc} */
    public void computeDerivatives(final SpacecraftState s, final TimeDerivativesEquations adder,
                                   final double[] p, final double[] pDot) throws OrekitException {

        final int dim = 3;

        // Lazy initialization
        if (dirty) {

            if (propagator.getPropagationParametersType() != NumericalPropagator.PropagationParametersType.CARTESIAN) {
                throw new OrekitException(OrekitMessages.PARTIAL_DERIVATIVES_ONLY_IN_CARTESIAN);
            }

            // if steps have not been set by user, set default values
            if (Double.isNaN(hPos)) {
                final double factor = FastMath.sqrt(MathUtils.EPSILON);
                hPos  = factor * s.getPVCoordinates().getPosition().getNorm();
                hVel  = factor * s.getPVCoordinates().getVelocity().getNorm();
                hM = factor * s.getMass();
            }

             // set up jacobians providers
            jacobiansProviders.clear();
            for (final ForceModel model : propagator.getForceModels()) {
                if (model instanceof AccelerationJacobiansProvider) {

                    // the force model already provides the jacobians by itself
                    jacobiansProviders.add((AccelerationJacobiansProvider) model);

                } else {

                    // wrap the force model to compute the jacobians by finite differences
                    jacobiansProviders.add(new Jacobianizer(model, selectedParameters, hPos, hVel, hM));

                }
            }
            jacobiansProviders.add(propagator.getNewtonianAttractionForceModel());

            // check all parameters are handled by at least one jacobian provider
            for (final ParameterConfiguration param : selectedParameters) {
                final String name = param.getParameterName();
                boolean found = false;
                for (final AccelerationJacobiansProvider provider : jacobiansProviders) {
                    if (provider.isSupported(name)) {
                        param.setProvider(provider);
                        found = true;
                    }
                }
                if (!found) {
                    throw new OrekitException(OrekitMessages.UNKNOWN_PARAMETER, name);
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

        // the variational equations of the complete state jacobian matrix have the
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

            // the variational equations of the parameters jacobian matrix are computed
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

