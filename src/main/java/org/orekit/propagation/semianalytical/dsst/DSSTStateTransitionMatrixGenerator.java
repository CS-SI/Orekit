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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;

/** Generator for State Transition Matrix.
 * @author Luc Maisonobe
 * @since 11.1
 */
class DSSTStateTransitionMatrixGenerator implements AdditionalDerivativesProvider {

    /** Space dimension. */
    private static final int SPACE_DIMENSION = 3;

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

    /** State dimension. */
    public static final int STATE_DIMENSION = 2 * SPACE_DIMENSION;

    /** Name of the Cartesian STM additional state. */
    private final String stmName;

    /** Force models used in propagation. */
    private final List<DSSTForceModel> forceModels;

    /** Attitude provider used in propagation. */
    private final AttitudeProvider attitudeProvider;

    /** Observers for partial derivatives. */
    private final Map<String, DSSTPartialsObserver> partialsObservers;

    /** Simple constructor.
     * @param stmName name of the Cartesian STM additional state
     * @param forceModels force models used in propagation
     * @param attitudeProvider attitude provider used in propagation
     */
    DSSTStateTransitionMatrixGenerator(final String stmName, final List<DSSTForceModel> forceModels,
                                       final AttitudeProvider attitudeProvider) {
        this.stmName           = stmName;
        this.forceModels       = forceModels;
        this.attitudeProvider  = attitudeProvider;
        this.partialsObservers = new HashMap<>();
    }

    /** Register an observer for partial derivatives.
     * <p>
     * The observer {@link DSSTPartialsObserver#partialsComputed(double[], double[]) partialsComputed}
     * method will be called when partial derivatives are computed, as a side effect of
     * calling {@link #generate(SpacecraftState)}
     * </p>
     * @param name name of the parameter driver this observer is interested in (may be null)
     * @param observer observer to register
     */
    void addObserver(final String name, final DSSTPartialsObserver observer) {
        partialsObservers.put(name, observer);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return stmName;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return STATE_DIMENSION * STATE_DIMENSION;
    }

    /** {@inheritDoc} */
    @Override
    public boolean yields(final SpacecraftState state) {
        return !state.hasAdditionalState(getName());
    }

    /** Set the initial value of the State Transition Matrix.
     * <p>
     * The returned state must be added to the propagator.
     * </p>
     * @param state initial state
     * @param dYdY0 initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @return state with initial STM (converted to Cartesian ∂C/∂Y₀) added
     */
    SpacecraftState setInitialStateTransitionMatrix(final SpacecraftState state, final RealMatrix dYdY0) {

        if (dYdY0 != null) {
            if (dYdY0.getRowDimension() != STATE_DIMENSION ||
                            dYdY0.getColumnDimension() != STATE_DIMENSION) {
                throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2,
                                          dYdY0.getRowDimension(), dYdY0.getColumnDimension(),
                                          STATE_DIMENSION, STATE_DIMENSION);
            }
        }

        // flatten matrix
        final double[] flat = new double[STATE_DIMENSION * STATE_DIMENSION];
        int k = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                flat[k++] = dYdY0.getEntry(i, j);
            }
        }

        // set additional state
        return state.addAdditionalState(stmName, flat);

    }

    /** {@inheritDoc} */
    public CombinedDerivatives combinedDerivatives(final SpacecraftState state) {

        final double[] p = state.getAdditionalState(getName());
        final double[] res = new double[p.length];

        // perform matrix multiplication with matrices flatten
        final RealMatrix factor = computePartials(state);
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                double sum = 0;
                for (int k = 0; k < STATE_DIMENSION; ++k) {
                    sum += factor.getEntry(i, k) * p[j + k * STATE_DIMENSION];
                }
                res[index++] = sum;
            }
        }

        return new CombinedDerivatives(res, null);

    }

    /** Compute the various partial derivatives.
     * @param state current spacecraft state
     * @return factor matrix
     */
    private RealMatrix computePartials(final SpacecraftState state) {

        // set up containers for partial derivatives
        final RealMatrix            factor               = MatrixUtils.createRealMatrix(STATE_DIMENSION, STATE_DIMENSION);
        final DoubleArrayDictionary meanElementsPartials = new DoubleArrayDictionary();
        final DSSTGradientConverter converter            = new DSSTGradientConverter(state, attitudeProvider);

        // Compute Jacobian
        for (final DSSTForceModel forceModel : forceModels) {

            final FieldSpacecraftState<Gradient> dsState = converter.getState(forceModel);
            final Gradient[] parameters = converter.getParametersAtStateDate(dsState, forceModel);
            final FieldAuxiliaryElements<Gradient> auxiliaryElements = new FieldAuxiliaryElements<>(dsState.getOrbit(), I);

            final Gradient[] meanElementRate = forceModel.getMeanElementRate(dsState, auxiliaryElements, parameters);
            final double[] derivativesA  = meanElementRate[0].getGradient();
            final double[] derivativesEx = meanElementRate[1].getGradient();
            final double[] derivativesEy = meanElementRate[2].getGradient();
            final double[] derivativesHx = meanElementRate[3].getGradient();
            final double[] derivativesHy = meanElementRate[4].getGradient();
            final double[] derivativesL  = meanElementRate[5].getGradient();

            // update Jacobian with respect to state
            addToRow(derivativesA,  0, factor);
            addToRow(derivativesEx, 1, factor);
            addToRow(derivativesEy, 2, factor);
            addToRow(derivativesHx, 3, factor);
            addToRow(derivativesHy, 4, factor);
            addToRow(derivativesL,  5, factor);

            // partials derivatives with respect to parameters
            int paramsIndex = converter.getFreeStateParameters();
            for (ParameterDriver driver : forceModel.getParametersDrivers()) {
                if (driver.isSelected()) {
                    // for each span (for each estimated value) corresponding name is added
                    for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {

                        // get the partials derivatives for this driver
                        DoubleArrayDictionary.Entry entry = meanElementsPartials.getEntry(span.getData());
                        if (entry == null) {
                            // create an entry filled with zeroes
                            meanElementsPartials.put(span.getData(), new double[STATE_DIMENSION]);
                            entry = meanElementsPartials.getEntry(span.getData());
                        }
                        // add the contribution of the current force model
                        entry.increment(new double[] {
                            derivativesA[paramsIndex], derivativesEx[paramsIndex], derivativesEy[paramsIndex],
                            derivativesHx[paramsIndex], derivativesHy[paramsIndex], derivativesL[paramsIndex]
                        });
                        ++paramsIndex;
                    }

                }
            }

        }

        // notify observers
        for (Map.Entry<String, DSSTPartialsObserver> observersEntry : partialsObservers.entrySet()) {
            final DoubleArrayDictionary.Entry entry = meanElementsPartials.getEntry(observersEntry.getKey());
            observersEntry.getValue().partialsComputed(state, factor, entry == null ? new double[STATE_DIMENSION] : entry.getValue());
        }

        return factor;

    }

    /** Fill Jacobians rows.
     * @param derivatives derivatives of a component
     * @param index component index (0 for a, 1 for ex, 2 for ey, 3 for hx, 4 for hy, 5 for l)
     * @param factor Jacobian of mean elements rate with respect to mean elements
     */
    private void addToRow(final double[] derivatives, final int index, final RealMatrix factor) {
        for (int i = 0; i < 6; i++) {
            factor.addToEntry(index, i, derivatives[i]);
        }
    }

    /** Interface for observing partials derivatives. */
    public interface DSSTPartialsObserver {

        /** Callback called when partial derivatives have been computed.
         * @param state current spacecraft state
         * @param factor factor matrix
         * @param meanElementsPartials partials derivatives of mean elements rates with respect to the parameter driver
         * that was registered (zero if no parameters were not selected or parameter is unknown)
         */
        void partialsComputed(SpacecraftState state, RealMatrix factor, double[] meanElementsPartials);

    }

}

