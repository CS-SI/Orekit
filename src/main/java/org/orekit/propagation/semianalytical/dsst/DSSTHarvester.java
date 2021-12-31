/* Copyright 2002-2022 CS GROUP
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
package org.orekit.propagation.semianalytical.dsst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.linear.RealMatrix;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.ParameterDriver;

/** Harvester between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * @author Luc Maisonobe
 * @since 11.1
 */
class DSSTHarvester extends AbstractMatricesHarvester {

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

    /** Propagator bound to this harvester. */
    private final DSSTPropagator propagator;

    /** Derivatives of the short period terms that apply to State Transition Matrix.*/
    private final double[][] shortPeriodDerivativesStm;

    /** Derivatives of the short period terms that apply to Jacobians columns. */
    private final DoubleArrayDictionary shortPeriodDerivativesJacobianColumns;

    /** Columns names for parameters. */
    private List<String> columnsNames;

    /** Simple constructor.
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the
     * {@link org.orekit.orbits.OrbitType#EQUINOCTIAL equinoctial orbit type}
     * and {@link org.orekit.orbits.PositionAngle position angle} that will be used by propagator
     * </p>
     * @param propagator propagator bound to this harvester
     * @param stmName State Transition Matrix state name
     * @param initialStm initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @param initialJacobianColumns initial columns of the Jacobians matrix with respect to parameters,
     * if null or if some selected parameters are missing from the dictionary, the corresponding
     * initial column is assumed to be 0
     */
    DSSTHarvester(final DSSTPropagator propagator, final String stmName,
                  final RealMatrix initialStm, final DoubleArrayDictionary initialJacobianColumns) {
        super(stmName, initialStm, initialJacobianColumns);
        this.propagator                            = propagator;
        this.shortPeriodDerivativesStm             = new double[STATE_DIMENSION][STATE_DIMENSION];
        this.shortPeriodDerivativesJacobianColumns = new DoubleArrayDictionary();
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getStateTransitionMatrix(final SpacecraftState state) {

        final RealMatrix stm = super.getStateTransitionMatrix(state);

        if (propagator.getPropagationType() == PropagationType.OSCULATING) {
            // add the short period terms
            for (int i = 0; i < STATE_DIMENSION; i++) {
                for (int j = 0; j < STATE_DIMENSION; j++) {
                    stm.addToEntry(i, j, shortPeriodDerivativesStm[i][j]);
                }
            }
        }

        return stm;

    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getParametersJacobian(final SpacecraftState state) {

        final RealMatrix jacobian = super.getParametersJacobian(state);
        if (jacobian != null && propagator.getPropagationType() == PropagationType.OSCULATING) {

            // add the short period terms
            final List<String> names = getJacobiansColumnsNames();
            for (int j = 0; j < names.size(); ++j) {
                final double[] column = shortPeriodDerivativesJacobianColumns.get(names.get(j));
                for (int i = 0; i < STATE_DIMENSION; i++) {
                    jacobian.addToEntry(i, j, column[i]);
                }
            }

        }

        return jacobian;

    }

    /** Freeze the names of the Jacobian columns.
     * <p>
     * This method is called when proagation starts, i.e. when configuration is completed
     * </p>
     */
    public void freezeColumnsNames() {
        columnsNames = getJacobiansColumnsNames();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getJacobiansColumnsNames() {
        return columnsNames == null ? propagator.getJacobiansColumnsNames() : columnsNames;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void setReferenceState(final SpacecraftState reference) {

        if (propagator.getPropagationType() == PropagationType.MEAN) {
            // nothing to do
            return;
        }

        // reset derivatives to zero
        for (final double[] row : shortPeriodDerivativesStm) {
            Arrays.fill(row, 0.0);
        }
        shortPeriodDerivativesJacobianColumns.clear();

        final DSSTGradientConverter converter = new DSSTGradientConverter(reference, propagator.getAttitudeProvider());

        // Compute Jacobian
        for (final DSSTForceModel forceModel : propagator.getAllForceModels()) {

            final FieldSpacecraftState<Gradient> dsState = converter.getState(forceModel);
            final Gradient[] dsParameters = converter.getParameters(dsState, forceModel);
            final FieldAuxiliaryElements<Gradient> auxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), I);

            final Gradient zero = dsState.getDate().getField().getZero();
            final List<FieldShortPeriodTerms<Gradient>> shortPeriodTerms = new ArrayList<>();
            shortPeriodTerms.addAll(forceModel.initializeShortPeriodTerms(auxiliaryElements, propagator.getPropagationType(), dsParameters));
            forceModel.updateShortPeriodTerms(dsParameters, dsState);
            final Gradient[] shortPeriod = new Gradient[6];
            Arrays.fill(shortPeriod, zero);
            for (final FieldShortPeriodTerms<Gradient> spt : shortPeriodTerms) {
                final Gradient[] spVariation = spt.value(dsState.getOrbit());
                for (int i = 0; i < spVariation .length; i++) {
                    shortPeriod[i] = shortPeriod[i].add(spVariation[i]);
                }
            }

            final double[] derivativesASP  = shortPeriod[0].getGradient();
            final double[] derivativesExSP = shortPeriod[1].getGradient();
            final double[] derivativesEySP = shortPeriod[2].getGradient();
            final double[] derivativesHxSP = shortPeriod[3].getGradient();
            final double[] derivativesHySP = shortPeriod[4].getGradient();
            final double[] derivativesLSP  = shortPeriod[5].getGradient();

            // update Jacobian with respect to state
            addToRow(derivativesASP,  0);
            addToRow(derivativesExSP, 1);
            addToRow(derivativesEySP, 2);
            addToRow(derivativesHxSP, 3);
            addToRow(derivativesHySP, 4);
            addToRow(derivativesLSP,  5);

            int paramsIndex = converter.getFreeStateParameters();
            for (ParameterDriver driver : forceModel.getParametersDrivers()) {
                if (driver.isSelected()) {

                    // get the partials derivatives for this driver
                    DoubleArrayDictionary.Entry entry = shortPeriodDerivativesJacobianColumns.getEntry(driver.getName());
                    if (entry == null) {
                        // create an entry filled with zeroes
                        shortPeriodDerivativesJacobianColumns.put(driver.getName(), new double[STATE_DIMENSION]);
                        entry = shortPeriodDerivativesJacobianColumns.getEntry(driver.getName());
                    }

                    // add the contribution of the current force model
                    entry.increment(new double[] {
                        derivativesASP[paramsIndex], derivativesExSP[paramsIndex], derivativesEySP[paramsIndex],
                        derivativesHxSP[paramsIndex], derivativesHySP[paramsIndex], derivativesLSP[paramsIndex]
                    });
                    ++paramsIndex;

                }
            }
        }

    }

    /** Fill State Transition Matrix rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for a, 1 for ex, 2 for ey, 3 for hx, 4 for hy, 5 for l)
     */
    private void addToRow(final double[] derivatives, final int index) {
        for (int i = 0; i < 6; i++) {
            shortPeriodDerivativesStm[index][i] += derivatives[i];
        }
    }

}
