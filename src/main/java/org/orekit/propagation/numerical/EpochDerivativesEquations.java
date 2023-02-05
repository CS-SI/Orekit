/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.numerical;

import java.util.IdentityHashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.ThirdBodyAttractionEpoch;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.TimeSpanMap.Span;

/** Computes derivatives of the acceleration, including ThirdBodyAttraction.
 *
 * {@link AdditionalDerivativesProvider Provider} computing the partial derivatives
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
 * @since 10.2
 */
public class EpochDerivativesEquations
    implements AdditionalDerivativesProvider  {

    /** State dimension, fixed to 6. */
    public static final int STATE_DIMENSION = 6;

    /** Propagator computing state evolution. */
    private final NumericalPropagator propagator;

    /** Selected parameters for Jacobian computation. */
    private ParameterDriversList selected;

    /** Parameters map. */
    private Map<String, Integer> map;

    /** Name. */
    private final String name;

    /** Simple constructor.
     * <p>
     * Upon construction, this set of equations is <em>automatically</em> added to
     * the propagator by calling its {@link
     * NumericalPropagator#addAdditionalDerivativesProvider(AdditionalDerivativesProvider)} method. So
     * there is no need to call this method explicitly for these equations.
     * </p>
     * @param name name of the partial derivatives equations
     * @param propagator the propagator that will handle the orbit propagation
     */
    public EpochDerivativesEquations(final String name, final NumericalPropagator propagator) {
        this.name                   = name;
        this.selected               = null;
        this.map                    = null;
        this.propagator             = propagator;
        propagator.addAdditionalDerivativesProvider(this);
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        freezeParametersSelection();
        return 6 * (6 + selected.getNbParams() + 1);
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
            map = new IdentityHashMap<>();
            int parameterIndex = 0;
            int previousParameterIndex = 0;
            for (final ParameterDriver selectedDriver : selected.getDrivers()) {
                for (final ForceModel provider : propagator.getAllForceModels()) {
                    for (final ParameterDriver driver : provider.getParametersDrivers()) {
                        if (driver.getName().equals(selectedDriver.getName())) {
                            previousParameterIndex = parameterIndex;
                            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                                map.put(span.getData(), previousParameterIndex++);
                            }
                        }
                    }
                }
                parameterIndex = previousParameterIndex;
            }

        }
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
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s0) {
        freezeParametersSelection();
        final int epochStateDimension = 6;
        final double[][] dYdY0 = new double[epochStateDimension][epochStateDimension];
        final double[][] dYdP  = new double[epochStateDimension][selected.getNbValuesToEstimate() + 6];
        for (int i = 0; i < epochStateDimension; ++i) {
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
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s1,
                                               final double[][] dY1dY0, final double[][] dY1dP) {

        freezeParametersSelection();

        // Check dimensions
        final int stateDimEpoch = dY1dY0.length;
        if (stateDimEpoch != 6 || stateDimEpoch != dY1dY0[0].length) {
            throw new OrekitException(OrekitMessages.STATE_JACOBIAN_NOT_6X6,
                                      stateDimEpoch, dY1dY0[0].length);
        }
        if (dY1dP != null && stateDimEpoch != dY1dP.length) {
            throw new OrekitException(OrekitMessages.STATE_AND_PARAMETERS_JACOBIANS_ROWS_MISMATCH,
                                      stateDimEpoch, dY1dP.length);
        }

        // store the matrices as a single dimension array
        final double[] p = new double[STATE_DIMENSION * (STATE_DIMENSION + selected.getNbValuesToEstimate()) + 6];
        setInitialJacobians(s1, dY1dY0, dY1dP, p);

        // set value in propagator
        return s1.addAdditionalState(name, p);

    }

    /** Set the Jacobian with respect to state into a one-dimensional additional state array.
     * <p>
     * This method converts the Jacobians to Cartesian parameters and put the converted data
     * in the one-dimensional {@code p} array.
     * </p>
     * @param state spacecraft state
     * @param dY1dY0 Jacobian of current state at time t₁
     * with respect to state at some previous time t₀
     * @param dY1dP Jacobian of current state at time t₁
     * with respect to parameters (may be null if there are no parameters)
     * @param p placeholder where to put the one-dimensional additional state
     */
    public void setInitialJacobians(final SpacecraftState state, final double[][] dY1dY0,
                                    final double[][] dY1dP, final double[] p) {

        // set up a converter
        final RealMatrix dY1dC1 = MatrixUtils.createRealIdentityMatrix(STATE_DIMENSION);
        final DecompositionSolver solver = new QRDecomposition(dY1dC1).getSolver();

        // convert the provided state Jacobian
        final RealMatrix dC1dY0 = solver.solve(new Array2DRowRealMatrix(dY1dY0, false));

        // map the converted state Jacobian to one-dimensional array
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                p[index++] = dC1dY0.getEntry(i, j);
            }
        }

        if (selected.getNbValuesToEstimate() != 0) {
            // convert the provided state Jacobian
            final RealMatrix dC1dP = solver.solve(new Array2DRowRealMatrix(dY1dP, false));

            // map the converted parameters Jacobian to one-dimensional array
            for (int i = 0; i < STATE_DIMENSION; ++i) {
                for (int j = 0; j < selected.getNbValuesToEstimate(); ++j) {
                    p[index++] = dC1dP.getEntry(i, j);
                }
            }
        }

    }

    /** {@inheritDoc} */
    public CombinedDerivatives combinedDerivatives(final SpacecraftState s) {

        // initialize acceleration Jacobians to zero
        final int paramDimEpoch = selected.getNbValuesToEstimate() + 1; // added epoch
        final int dimEpoch      = 3;
        final double[][] dAccdParam = new double[dimEpoch][paramDimEpoch];
        final double[][] dAccdPos   = new double[dimEpoch][dimEpoch];
        final double[][] dAccdVel   = new double[dimEpoch][dimEpoch];

        final NumericalGradientConverter fullConverter    = new NumericalGradientConverter(s, 6, propagator.getAttitudeProvider());
        final NumericalGradientConverter posOnlyConverter = new NumericalGradientConverter(s, 3, propagator.getAttitudeProvider());

        // compute acceleration Jacobians, finishing with the largest force: Newtonian attraction
        for (final ForceModel forceModel : propagator.getAllForceModels()) {
            final NumericalGradientConverter converter = forceModel.dependsOnPositionOnly() ? posOnlyConverter : fullConverter;
            final FieldSpacecraftState<Gradient> dsState = converter.getState(forceModel);
            final Gradient[] parameters = converter.getParametersAtStateDate(dsState, forceModel);

            final FieldVector3D<Gradient> acceleration = forceModel.acceleration(dsState, parameters);
            final double[] derivativesX = acceleration.getX().getGradient();
            final double[] derivativesY = acceleration.getY().getGradient();
            final double[] derivativesZ = acceleration.getZ().getGradient();

            // update Jacobians with respect to state
            addToRow(derivativesX, 0, converter.getFreeStateParameters(), dAccdPos, dAccdVel);
            addToRow(derivativesY, 1, converter.getFreeStateParameters(), dAccdPos, dAccdVel);
            addToRow(derivativesZ, 2, converter.getFreeStateParameters(), dAccdPos, dAccdVel);

            int index = converter.getFreeStateParameters();
            for (ParameterDriver driver : forceModel.getParametersDrivers()) {
                if (driver.isSelected()) {
                    for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                        final int parameterIndex = map.get(span.getData());
                        dAccdParam[0][parameterIndex] += derivativesX[index];
                        dAccdParam[1][parameterIndex] += derivativesY[index];
                        dAccdParam[2][parameterIndex] += derivativesZ[index];
                        ++index;
                    }
                }
            }

            // Add the derivatives of the acceleration w.r.t. the Epoch
            if (forceModel instanceof ThirdBodyAttractionEpoch) {
                final double[] parametersValues = new double[] {parameters[0].getValue()};
                final double[] derivatives = ((ThirdBodyAttractionEpoch) forceModel).getDerivativesToEpoch(s, parametersValues);
                dAccdParam[0][paramDimEpoch - 1] += derivatives[0];
                dAccdParam[1][paramDimEpoch - 1] += derivatives[1];
                dAccdParam[2][paramDimEpoch - 1] += derivatives[2];
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
        final double[] pDot = new double[p.length];
        System.arraycopy(p, dimEpoch * stateDim, pDot, 0, dimEpoch * stateDim);

        // compute Cdot and Ddot
        for (int i = 0; i < dimEpoch; ++i) {
            final double[] dAdPi = dAccdPos[i];
            final double[] dAdVi = dAccdVel[i];
            for (int j = 0; j < stateDim; ++j) {
                pDot[(dimEpoch + i) * stateDim + j] =
                    dAdPi[0] * p[j]                + dAdPi[1] * p[j +     stateDim] + dAdPi[2] * p[j + 2 * stateDim] +
                    dAdVi[0] * p[j + 3 * stateDim] + dAdVi[1] * p[j + 4 * stateDim] + dAdVi[2] * p[j + 5 * stateDim];
            }
        }

        for (int k = 0; k < paramDimEpoch; ++k) {
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
            pDot[columnTop]                     = p[columnTop + 3 * paramDimEpoch];
            pDot[columnTop +     paramDimEpoch] = p[columnTop + 4 * paramDimEpoch];
            pDot[columnTop + 2 * paramDimEpoch] = p[columnTop + 5 * paramDimEpoch];

            // compute Fdot
            for (int i = 0; i < dimEpoch; ++i) {
                final double[] dAdP = dAccdPos[i];
                final double[] dAdV = dAccdVel[i];
                pDot[columnTop + (dimEpoch + i) * paramDimEpoch] =
                    dAccdParam[i][k] +
                    dAdP[0] * p[columnTop]                     + dAdP[1] * p[columnTop +     paramDimEpoch] + dAdP[2] * p[columnTop + 2 * paramDimEpoch] +
                    dAdV[0] * p[columnTop + 3 * paramDimEpoch] + dAdV[1] * p[columnTop + 4 * paramDimEpoch] + dAdV[2] * p[columnTop + 5 * paramDimEpoch];
            }

        }

        return new CombinedDerivatives(pDot, null);

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
            dAccdPos[index][i] += derivatives[i];
        }
        if (freeStateParameters > 3) {
            for (int i = 0; i < 3; ++i) {
                dAccdVel[index][i] += derivatives[i + 3];
            }
        }

    }

}

