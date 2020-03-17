/* Copyright 2010-2011 Centre National d'Études Spatiales
 * Licensed to CS Group (CS) under one or more
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

import java.util.IdentityHashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Set of {@link AdditionalEquations additional equations} computing the partial derivatives
 * of the state (orbit) with respect to initial state and force models parameters.
 * <p>
 * This set of equations are automatically added to a {@link NumericalPropagator numerical propagator}
 * in order to compute partial derivatives of the orbit along with the orbit itself. This is
 * useful for example in orbit determination applications.
 * </p>
 * <p>
 * The partial derivatives with respect to initial state can be either dimension 6
 * (orbit only) or 7 (orbit and mass).
 * </p>
 * <p>
 * The partial derivatives with respect to force models parameters has a dimension
 * equal to the number of selected parameters. Parameters selection is implemented at
 * {@link ForceModel force models} level. Users must retrieve a {@link ParameterDriver
 * parameter driver} using {@link ForceModel#getParameterDriver(String)} and then
 * select it by calling {@link ParameterDriver#setSelected(boolean) setSelected(true)}.
 * </p>
 * <p>
 * If several force models provide different {@link ParameterDriver drivers} for the
 * same parameter name, selecting any of these drivers has the side effect of
 * selecting all the drivers for this shared parameter. In this case, the partial
 * derivatives will be the sum of the partial derivatives contributed by the
 * corresponding force models. This case typically arises for central attraction
 * coefficient, which has an influence on {@link org.orekit.forces.gravity.NewtonianAttraction
 * Newtonian attraction}, {@link org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel
 * gravity field}, and {@link org.orekit.forces.gravity.Relativity relativity}.
 * </p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public class PartialDerivativesEquations implements AdditionalEquations {

    /** Propagator computing state evolution. */
    private final NumericalPropagator propagator;

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
     * NumericalPropagator#addAdditionalEquations(AdditionalEquations)} method. So
     * there is no need to call this method explicitly for these equations.
     * </p>
     * @param name name of the partial derivatives equations
     * @param propagator the propagator that will handle the orbit propagation
     */
    public PartialDerivativesEquations(final String name, final NumericalPropagator propagator) {
        this.name                   = name;
        this.selected               = null;
        this.map                    = null;
        this.propagator             = propagator;
        this.initialized            = false;
        propagator.addAdditionalEquations(this);
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** Freeze the selected parameters from the force models.
     */
    private void freezeParametersSelection() {
        if (selected == null) {

            // first pass: gather all parameters, binding similar names together
            selected = new ParameterDriversList();
            for (final ForceModel provider : propagator.getAllForceModels()) {
                for (final ParameterDriver driver : provider.getParametersDrivers()) {
                    selected.add(driver);
                }
            }

            // second pass: now that shared parameter names are bound together,
            // their selections status have been synchronized, we can filter them
            selected.filter(true);

            // third pass: sort parameters lexicographically
            selected.sort();

            // fourth pass: set up a map between parameters drivers and matrices columns
            map = new IdentityHashMap<ParameterDriver, Integer>();
            int parameterIndex = 0;
            for (final ParameterDriver selectedDriver : selected.getDrivers()) {
                for (final ForceModel provider : propagator.getAllForceModels()) {
                    for (final ParameterDriver driver : provider.getParametersDrivers()) {
                        if (driver.getName().equals(selectedDriver.getName())) {
                            map.put(driver, parameterIndex);
                        }
                    }
                }
                ++parameterIndex;
            }

        }
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
        freezeParametersSelection();
        return selected;
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
     * @since 9.0
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s0) {
        freezeParametersSelection();
        final int stateDimension = 6;
        final double[][] dYdY0 = new double[stateDimension][stateDimension];
        final double[][] dYdP  = new double[stateDimension][selected.getNbParams()];
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
     * <p>
     * The force models parameters for which partial derivatives are desired,
     * <em>must</em> have been {@link ParameterDriver#setSelected(boolean) selected}
     * before this method is called, and the {@code dY1dP} matrix dimension <em>must</em>
     * be consistent with the selection.
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

        freezeParametersSelection();

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
        final JacobiansMapper mapper = getMapper();
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
    public JacobiansMapper getMapper() {
        if (!initialized) {
            throw new OrekitException(OrekitMessages.STATE_JACOBIAN_NOT_INITIALIZED);
        }
        return new JacobiansMapper(name, selected,
                                   propagator.getOrbitType(),
                                   propagator.getPositionAngleType());
    }

    /** {@inheritDoc} */
    public double[] computeDerivatives(final SpacecraftState s, final double[] pDot) {

        // initialize acceleration Jacobians to zero
        final int paramDim = selected.getNbParams();
        final int dim = 3;
        final double[][] dAccdParam = new double[dim][paramDim];
        final double[][] dAccdPos   = new double[dim][dim];
        final double[][] dAccdVel   = new double[dim][dim];

        final DSConverter fullConverter    = new DSConverter(s, 6, propagator.getAttitudeProvider());
        final DSConverter posOnlyConverter = new DSConverter(s, 3, propagator.getAttitudeProvider());

        // compute acceleration Jacobians, finishing with the largest force: Newtonian attraction
        for (final ForceModel forceModel : propagator.getAllForceModels()) {

            final DSConverter converter = forceModel.dependsOnPositionOnly() ? posOnlyConverter : fullConverter;
            final FieldSpacecraftState<DerivativeStructure> dsState = converter.getState(forceModel);
            final DerivativeStructure[] parameters = converter.getParameters(dsState, forceModel);

            final FieldVector3D<DerivativeStructure> acceleration = forceModel.acceleration(dsState, parameters);
            final double[] derivativesX = acceleration.getX().getAllDerivatives();
            final double[] derivativesY = acceleration.getY().getAllDerivatives();
            final double[] derivativesZ = acceleration.getZ().getAllDerivatives();

            // update Jacobians with respect to state
            addToRow(derivativesX, 0, converter.getFreeStateParameters(), dAccdPos, dAccdVel);
            addToRow(derivativesY, 1, converter.getFreeStateParameters(), dAccdPos, dAccdVel);
            addToRow(derivativesZ, 2, converter.getFreeStateParameters(), dAccdPos, dAccdVel);

            int index = converter.getFreeStateParameters();
            for (ParameterDriver driver : forceModel.getParametersDrivers()) {
                if (driver.isSelected()) {
                    final int parameterIndex = map.get(driver);
                    ++index;
                    dAccdParam[0][parameterIndex] += derivativesX[index];
                    dAccdParam[1][parameterIndex] += derivativesY[index];
                    dAccdParam[2][parameterIndex] += derivativesZ[index];
                }
            }

        }

        // the variational equations of the complete state Jacobian matrix have the following form:

        // [        |        ]   [                 |                  ]   [     |     ]
        // [  Adot  |  Bdot  ]   [  dVel/dPos = 0  |  dVel/dVel = Id  ]   [  A  |  B  ]
        // [        |        ]   [                 |                  ]   [     |     ]
        // ---------+---------   ------------------+------------------- * ------+------
        // [        |        ]   [                 |                  ]   [     |     ]
        // [  Cdot  |  Ddot  ] = [    dAcc/dPos    |     dAcc/dVel    ]   [  C  |  D  ]
        // [        |        ]   [                 |                  ]   [     |     ]

        // The A, B, C and D sub-matrices and their derivatives (Adot ...) are 3x3 matrices

        // The expanded multiplication above can be rewritten to take into account
        // the fixed values found in the sub-matrices in the left factor. This leads to:

        //     [ Adot ] = [ C ]
        //     [ Bdot ] = [ D ]
        //     [ Cdot ] = [ dAcc/dPos ] * [ A ] + [ dAcc/dVel ] * [ C ]
        //     [ Ddot ] = [ dAcc/dPos ] * [ B ] + [ dAcc/dVel ] * [ D ]

        // The following loops compute these expressions taking care of the mapping of the
        // (A, B, C, D) matrices into the single dimension array p and of the mapping of the
        // (Adot, Bdot, Cdot, Ddot) matrices into the single dimension array pDot.

        // copy C and E into Adot and Bdot
        final int stateDim = 6;
        final double[] p = s.getAdditionalState(getName());
        System.arraycopy(p, dim * stateDim, pDot, 0, dim * stateDim);

        // compute Cdot and Ddot
        for (int i = 0; i < dim; ++i) {
            final double[] dAdPi = dAccdPos[i];
            final double[] dAdVi = dAccdVel[i];
            for (int j = 0; j < stateDim; ++j) {
                pDot[(dim + i) * stateDim + j] =
                    dAdPi[0] * p[j]                + dAdPi[1] * p[j +     stateDim] + dAdPi[2] * p[j + 2 * stateDim] +
                    dAdVi[0] * p[j + 3 * stateDim] + dAdVi[1] * p[j + 4 * stateDim] + dAdVi[2] * p[j + 5 * stateDim];
            }
        }

        for (int k = 0; k < paramDim; ++k) {
            // the variational equations of the parameters Jacobian matrix are computed
            // one column at a time, they have the following form:
            // [      ]   [                 |                  ]   [   ]   [                  ]
            // [ Edot ]   [  dVel/dPos = 0  |  dVel/dVel = Id  ]   [ E ]   [  dVel/dParam = 0 ]
            // [      ]   [                 |                  ]   [   ]   [                  ]
            // --------   ------------------+------------------- * ----- + --------------------
            // [      ]   [                 |                  ]   [   ]   [                  ]
            // [ Fdot ] = [    dAcc/dPos    |     dAcc/dVel    ]   [ F ]   [    dAcc/dParam   ]
            // [      ]   [                 |                  ]   [   ]   [                  ]

            // The E and F sub-columns and their derivatives (Edot, Fdot) are 3 elements columns.

            // The expanded multiplication and addition above can be rewritten to take into
            // account the fixed values found in the sub-matrices in the left factor. This leads to:

            //     [ Edot ] = [ F ]
            //     [ Fdot ] = [ dAcc/dPos ] * [ E ] + [ dAcc/dVel ] * [ F ] + [ dAcc/dParam ]

            // The following loops compute these expressions taking care of the mapping of the
            // (E, F) columns into the single dimension array p and of the mapping of the
            // (Edot, Fdot) columns into the single dimension array pDot.

            // copy F into Edot
            final int columnTop = stateDim * stateDim + k;
            pDot[columnTop]                = p[columnTop + 3 * paramDim];
            pDot[columnTop +     paramDim] = p[columnTop + 4 * paramDim];
            pDot[columnTop + 2 * paramDim] = p[columnTop + 5 * paramDim];

            // compute Fdot
            for (int i = 0; i < dim; ++i) {
                final double[] dAdPi = dAccdPos[i];
                final double[] dAdVi = dAccdVel[i];
                pDot[columnTop + (dim + i) * paramDim] =
                    dAccdParam[i][k] +
                    dAdPi[0] * p[columnTop]                + dAdPi[1] * p[columnTop +     paramDim] + dAdPi[2] * p[columnTop + 2 * paramDim] +
                    dAdVi[0] * p[columnTop + 3 * paramDim] + dAdVi[1] * p[columnTop + 4 * paramDim] + dAdVi[2] * p[columnTop + 5 * paramDim];
            }

        }

        // these equations have no effect on the main state itself
        return null;

    }

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component of acceleration (along either x, y or z)
     * @param index component index (0 for x, 1 for y, 2 for z)
     * @param freeStateParameters number of free parameters, either 3 (position),
     * 6 (position-velocity) or 7 (position-velocity-mass)
     * @param dAccdPos Jacobian of acceleration with respect to spacecraft position
     * @param dAccdVel Jacobian of acceleration with respect to spacecraft velocity
     */
    private void addToRow(final double[] derivatives, final int index, final int freeStateParameters,
                          final double[][] dAccdPos, final double[][] dAccdVel) {

        for (int i = 0; i < 3; ++i) {
            dAccdPos[index][i] += derivatives[i + 1];
        }
        if (freeStateParameters > 3) {
            for (int i = 0; i < 3; ++i) {
                dAccdVel[index][i] += derivatives[i + 4];
            }
        }

    }

}

