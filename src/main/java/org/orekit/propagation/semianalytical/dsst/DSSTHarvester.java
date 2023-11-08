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
package org.orekit.propagation.semianalytical.dsst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;

/** Harvester between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @since 11.1
 */
public class DSSTHarvester extends AbstractMatricesHarvester {

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

    /** Field short periodic terms. */
    private List<FieldShortPeriodTerms<Gradient>> fieldShortPeriodTerms;

    /** Simple constructor.
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the
     * {@link org.orekit.orbits.OrbitType#EQUINOCTIAL equinoctial orbit type}
     * and {@link PositionAngleType position angle} that will be used by propagator
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
        this.fieldShortPeriodTerms                 = new ArrayList<>();
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

    /** Get the Jacobian matrix B1 (B1 = ∂εη/∂Y).
     * <p>
     * B1 represents the partial derivatives of the short period motion
     * with respect to the mean equinoctial elements.
     * </p>
     * @return the B1 jacobian matrix
     */
    public RealMatrix getB1() {

        // Initialize B1
        final RealMatrix B1 = MatrixUtils.createRealMatrix(STATE_DIMENSION, STATE_DIMENSION);

        // add the short period terms
        for (int i = 0; i < STATE_DIMENSION; i++) {
            for (int j = 0; j < STATE_DIMENSION; j++) {
                B1.addToEntry(i, j, shortPeriodDerivativesStm[i][j]);
            }
        }

        // Return B1
        return B1;

    }

    /** Get the Jacobian matrix B2 (B2 = ∂Y/∂Y₀).
     * <p>
     * B2 represents the partial derivatives of the mean equinoctial elements
     * with respect to the initial ones.
     * </p>
     * @param state spacecraft state
     * @return the B2 jacobian matrix
     */
    public RealMatrix getB2(final SpacecraftState state) {
        return super.getStateTransitionMatrix(state);
    }

    /** Get the Jacobian matrix B3 (B3 = ∂Y/∂P).
     * <p>
     * B3 represents the partial derivatives of the mean equinoctial elements
     * with respect to the estimated propagation parameters.
     * </p>
     * @param state spacecraft state
     * @return the B3 jacobian matrix
     */
    public RealMatrix getB3(final SpacecraftState state) {
        return super.getParametersJacobian(state);
    }

    /** Get the Jacobian matrix B4 (B4 = ∂εη/∂c).
     * <p>
     * B4 represents the partial derivatives of the short period motion
     * with respect to the estimated propagation parameters.
     * </p>
     * @return the B4 jacobian matrix
     */
    public RealMatrix getB4() {

        // Initialize B4
        final List<String> names = getJacobiansColumnsNames();
        final RealMatrix B4 = MatrixUtils.createRealMatrix(STATE_DIMENSION, names.size());

        // add the short period terms
        for (int j = 0; j < names.size(); ++j) {
            final double[] column = shortPeriodDerivativesJacobianColumns.get(names.get(j));
            for (int i = 0; i < STATE_DIMENSION; i++) {
                B4.addToEntry(i, j, column[i]);
            }
        }

        // Return B4
        return B4;

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

    /** Initialize the short periodic terms for the "field" elements.
     * @param reference current mean spacecraft state
     */
    public void initializeFieldShortPeriodTerms(final SpacecraftState reference) {

        // Converter
        final DSSTGradientConverter converter = new DSSTGradientConverter(reference, propagator.getAttitudeProvider());

        // Loop on force models
        for (final DSSTForceModel forceModel : propagator.getAllForceModels()) {

            // Convert to Gradient
            final FieldSpacecraftState<Gradient> dsState = converter.getState(forceModel);
            final Gradient[] dsParameters = converter.getParametersAtStateDate(dsState, forceModel);
            final FieldAuxiliaryElements<Gradient> auxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), I);

            // Initialize the "Field" short periodic terms in OSCULATING mode
            fieldShortPeriodTerms.addAll(forceModel.initializeShortPeriodTerms(auxiliaryElements, PropagationType.OSCULATING, dsParameters));

        }

    }

    /** Update the short periodic terms for the "field" elements.
     * @param reference current mean spacecraft state
     */
    @SuppressWarnings("unchecked")
    public void updateFieldShortPeriodTerms(final SpacecraftState reference) {

        // Converter
        final DSSTGradientConverter converter = new DSSTGradientConverter(reference, propagator.getAttitudeProvider());

        // Loop on force models
        for (final DSSTForceModel forceModel : propagator.getAllForceModels()) {

            // Convert to Gradient
            final FieldSpacecraftState<Gradient> dsState = converter.getState(forceModel);
            final Gradient[] dsParameters = converter.getParameters(dsState, forceModel);

            // Update the short periodic terms for the current force model
            forceModel.updateShortPeriodTerms(dsParameters, dsState);

        }

    }

    /** {@inheritDoc} */
    @Override
    public void setReferenceState(final SpacecraftState reference) {

        // reset derivatives to zero
        for (final double[] row : shortPeriodDerivativesStm) {
            Arrays.fill(row, 0.0);
        }

        shortPeriodDerivativesJacobianColumns.clear();

        final DSSTGradientConverter converter = new DSSTGradientConverter(reference, propagator.getAttitudeProvider());

        // Compute Jacobian
        for (final DSSTForceModel forceModel : propagator.getAllForceModels()) {

            final FieldSpacecraftState<Gradient> dsState = converter.getState(forceModel);
            final Gradient zero = dsState.getDate().getField().getZero();
            final Gradient[] shortPeriod = new Gradient[6];
            Arrays.fill(shortPeriod, zero);
            for (final FieldShortPeriodTerms<Gradient> spt : fieldShortPeriodTerms) {
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

                    final TimeSpanMap<String> driverNameSpanMap = driver.getNamesSpanMap();
                    // for each span (for each estimated value) corresponding name is added

                    for (Span<String> span = driverNameSpanMap.getFirstSpan(); span != null; span = span.next()) {
                        // get the partials derivatives for this driver
                        DoubleArrayDictionary.Entry entry = shortPeriodDerivativesJacobianColumns.getEntry(span.getData());
                        if (entry == null) {
                            // create an entry filled with zeroes
                            shortPeriodDerivativesJacobianColumns.put(span.getData(), new double[STATE_DIMENSION]);
                            entry = shortPeriodDerivativesJacobianColumns.getEntry(span.getData());
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

    /** {@inheritDoc} */
    @Override
    public OrbitType getOrbitType() {
        return propagator.getOrbitType();
    }

    /** {@inheritDoc} */
    @Override
    public PositionAngleType getPositionAngleType() {
        return propagator.getPositionAngleType();
    }

}
