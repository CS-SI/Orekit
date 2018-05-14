/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst;

import java.util.IdentityHashMap;
import java.util.Map;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Set of {@link AdditionalEquations additional equations} computing the partial derivatives
 * of the state (orbit) with respect to initial state and force models parameters.
 * <p>
 * This set of equations are automatically added to a {@link DSSTPropagator DSST propagator}
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
 * {@link DSSTForceModel DSST force models} level. Users must retrieve a {@link ParameterDriver
 * parameter driver} using {@link DSSTForceModel#getParameterDriver(String)} and then
 * select it by calling {@link ParameterDriver#setSelected(boolean) setSelected(true)}.
 * </p>
 * <p>
 */
public class DSSTPartialDerivativesEquations implements AdditionalEquations {

    /** Retrograde factor I.
     *  <p>
     *  DSST model needs equinoctial orbit as internal representation.
     *  Classical equinoctial elements have discontinuities when inclination
     *  is close to zero. In this representation, I = +1. <br>
     *  To avoid this discontinuity, another representation exists and equinoctial
     *  elements can be expressed in a different way, called "retrograde" orbit.
     *  This implies I = -1. <br>
     *  As Orekit doesn't implement the retrograde orbit, I is always set to +1.
     *  But for the sake of consistency with the theory, the retrograde factor
     *  has been kept in the formulas.
     *  </p>
     */
    private static final int I = 1;

    /** Propagator computing state evolution. */
    private final DSSTPropagator propagator;

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
     * DSSTPropagator#addAdditionalEquations(AdditionalEquations)} method. So
     * there is no need to call this method explicitly for these equations.
     * </p>
     * @param name name of the partial derivatives equations
     * @param propagator the propagator that will handle the orbit propagation
     * @throws OrekitException if a set of equations with the same name is already present
     */
    public DSSTPartialDerivativesEquations(final String name, final DSSTPropagator propagator)
        throws OrekitException {
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
     * @exception OrekitException if an existing driver for a
     * parameter throws one when its value is reset using the value
     * from another driver managing the same parameter
     */
    private void freezeParametersSelection()
        throws OrekitException {
        if (selected == null) {

            // first pass: gather all parameters, binding similar names together
            selected = new ParameterDriversList();
            for (final DSSTForceModel provider : propagator.getAllForceModels()) {
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
                for (final DSSTForceModel provider : propagator.getAllForceModels()) {
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

    /** Set the initial value of the Jacobian with respect to state and parameter.
     * <p>
     * This method is equivalent to call {@link #setInitialJacobians(SpacecraftState,
     * double[][])} with dYdY0 set to the identity matrix.
     * </p>
     * <p>
     * The force models parameters for which partial derivatives are desired,
     * <em>must</em> have been {@link ParameterDriver#setSelected(boolean) selected}
     * before this method is called, so proper matrices dimensions are used.
     * </p>
     * @param s0 initial state
     * @return state with initial Jacobians added
     * @exception OrekitException if the partial equation has not been registered in
     * the propagator or if matrices dimensions are incorrect
     * @see #getSelectedParameters()
     * @since 9.0
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s0)
        throws OrekitException {
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
     * @exception OrekitException if the partial equation has not been registered in
     * the propagator or if matrices dimensions are incorrect
     * @see #getSelectedParameters()
     */
    public SpacecraftState setInitialJacobians(final SpacecraftState s1,
                                               final double[][] dY1dY0, final double[][] dY1dP)
        throws OrekitException {

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
        final DSSTJacobiansMapper mapper = getMapper();
        final double[] p = new double[mapper.getAdditionalStateDimension()];
        mapper.setInitialJacobians(s1, dY1dY0, dY1dP, p);

        // set value in propagator
        return s1.addAdditionalState(name, p);

    }

    /** Get a mapper between two-dimensional Jacobians and one-dimensional additional state.
     * @return a mapper between two-dimensional Jacobians and one-dimensional additional state,
     * with the same name as the instance
     * @exception OrekitException if the initial Jacobians have not been initialized yet
     * @see #setInitialJacobians(SpacecraftState, int)
     * @see #setInitialJacobians(SpacecraftState, double[][], double[][])
     */
    public DSSTJacobiansMapper getMapper() throws OrekitException {
        if (!initialized) {
            throw new OrekitException(OrekitMessages.STATE_JACOBIAN_NOT_INITIALIZED);
        }
        return new DSSTJacobiansMapper(name, selected,
                                   propagator.getOrbitType(),
                                   propagator.getPositionAngleType());
    }

    /** Get the selected parameters, in Jacobian matrix column order.
     * <p>
     * The force models parameters for which partial derivatives are desired,
     * <em>must</em> have been {@link ParameterDriver#setSelected(boolean) selected}
     * before this method is called, so the proper list is returned.
     * </p>
     * @return selected parameters, in Jacobian matrix column order which
     * is lexicographic order
     * @exception OrekitException if an existing driver for a
     * parameter throws one when its value is reset using the value
     * from another driver managing the same parameter
     */
    public ParameterDriversList getSelectedParameters()
        throws OrekitException {
        freezeParametersSelection();
        return selected;
    }

    /** {@inheritDoc} */
    public double[] computeDerivatives(final SpacecraftState s, final double[] pDot)
        throws OrekitException {

        // initialize Jacobians to zero
        final int paramDim = selected.getNbParams();
        final int dim = 6;
        final double[][] dMeanElementsdParam = new double[dim][paramDim];
        final double[][] dMeanElementsdElement = new double[dim][dim];

        final DSSTDSConverter converter = new DSSTDSConverter(s, 6, propagator.getAttitudeProvider());

        // Compute Jacobian
        for (final DSSTForceModel forceModel : propagator.getAllForceModels()) {

            final FieldSpacecraftState<DerivativeStructure> dsState = converter.getState(forceModel);
            final FieldAuxiliaryElements<DerivativeStructure> auxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), I);

            final DerivativeStructure[] meanElements = forceModel.getMeanElementRate(dsState, auxiliaryElements);
            final double[] derivativesA  = meanElements[0].getAllDerivatives();
            final double[] derivativesEx = meanElements[1].getAllDerivatives();
            final double[] derivativesEy = meanElements[2].getAllDerivatives();
            final double[] derivativesHx = meanElements[3].getAllDerivatives();
            final double[] derivativesHy = meanElements[4].getAllDerivatives();
            final double[] derivativesL  = meanElements[5].getAllDerivatives();

            // update Jacobian with respect to state
            addToRow(derivativesA,  0, dMeanElementsdElement);
            addToRow(derivativesEx, 1, dMeanElementsdElement);
            addToRow(derivativesEy, 2, dMeanElementsdElement);
            addToRow(derivativesHx, 3, dMeanElementsdElement);
            addToRow(derivativesHy, 4, dMeanElementsdElement);
            addToRow(derivativesL,  5, dMeanElementsdElement);

            int index = converter.getFreeStateParameters();
            for (ParameterDriver driver : forceModel.getParametersDrivers()) {
                if (driver.isSelected()) {
                    final int parameterIndex = map.get(driver);
                    ++index;
                    dMeanElementsdParam[0][parameterIndex] += derivativesA[index];
                    dMeanElementsdParam[1][parameterIndex] += derivativesEx[index];
                    dMeanElementsdParam[2][parameterIndex] += derivativesEy[index];
                    dMeanElementsdParam[3][parameterIndex] += derivativesHx[index];
                    dMeanElementsdParam[4][parameterIndex] += derivativesHy[index];
                    dMeanElementsdParam[5][parameterIndex] += derivativesL[index];
                }
            }

        }

        // The variational equations of the complete state Jacobian matrix have the following form:

        //                     [ Adot ] = [ dMeanElementsdElement ] * [ A ]

        // The A matrix and its derivative (Adot) are 6 * 6 matrices

        // The following loops compute these expression taking care of the mapping of the
        // A matrix into the single dimension array p and of the mapping of the
        // Adot matrix into the single dimension array pDot.

        final double[] p = s.getAdditionalState(getName());

        for (int i = 0; i < dim; i++) {
            final double[] dMeanElementsdElementi = dMeanElementsdElement[i];
            for (int j = 0; j < dim; j++) {
                pDot[j + dim * i] =
                    dMeanElementsdElementi[0] * p[j]           + dMeanElementsdElementi[1] * p[j +     dim] + dMeanElementsdElementi[2] * p[j + 2 * dim] +
                    dMeanElementsdElementi[3] * p[j + 3 * dim] + dMeanElementsdElementi[4] * p[j + 4 * dim] + dMeanElementsdElementi[5] * p[j + 5 * dim];
            }
        }

        for (int k = 0; k < paramDim; k++) {
            // the variational equations of the parameters Jacobian matrix are computed
            // one column at a time, they have the following form:

            //             [ Bdot ] = [ dMeanElementsdElement ] * [ B ] + [ dMeanElementsdParam ]

            // The B sub-columns and its derivative (Bdot) are 6 elements columns.

            // The following loops compute this expression taking care of the mapping of the
            // B columns into the single dimension array p and of the mapping of the
            // Bdot columns into the single dimension array pDot.

            final int columnTop = dim * dim;

            for (int i = 0; i < dim; ++i) {
                final double[] dMeanElementsdElementi = dMeanElementsdElement[i];
                pDot[columnTop + (i + dim * k)] =
                    dMeanElementsdParam[i][k] +
                    dMeanElementsdElementi[0] * p[columnTop + k]                + dMeanElementsdElementi[1] * p[columnTop + k +     paramDim] + dMeanElementsdElementi[2] * p[columnTop + k + 2 * paramDim] +
                    dMeanElementsdElementi[3] * p[columnTop + k + 3 * paramDim] + dMeanElementsdElementi[4] * p[columnTop + k + 4 * paramDim] + dMeanElementsdElementi[5] * p[columnTop + k + 5 * paramDim];
            }
        }

        // these equations have no effect on the main state itself
        return null;

    }

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for a, 1 for ex, 2 for ey, 3 for hx, 4 for hy, 5 for l)
     * 6 (equinoctial parameters (EP)) or 7 (EP and mass)
     * @param dMeanElementsdElement Jacobian of mean elements rate with respect to spacecraft position
     */
    private void addToRow(final double[] derivatives, final int index,
                          final double[][] dMeanElementsdElement) {

        for (int i = 0; i < 6; i++) {
            dMeanElementsdElement[index][i] += derivatives[i + 1];
        }

    }

}
