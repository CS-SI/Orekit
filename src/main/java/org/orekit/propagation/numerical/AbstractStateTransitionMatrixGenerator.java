/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.AttitudeProviderModifier;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Abstract generator for numerical State Transition Matrix.
 * @author Luc Maisonobe
 * @author Melina Vanel
 * @author Romain Serra
 * @since 13.1
 */
abstract class AbstractStateTransitionMatrixGenerator implements AdditionalDerivativesProvider {

    /** Space dimension. */
    protected static final int SPACE_DIMENSION = 3;

    /** Threshold for matrix solving. */
    private static final double THRESHOLD = Precision.SAFE_MIN;

    /** Name of the Cartesian STM additional state. */
    private final String stmName;

    /** Force models used in propagation. */
    private final List<ForceModel> forceModels;

    /** Attitude provider used in propagation. */
    private final AttitudeProvider attitudeProvider;

    /** Observers for partial derivatives. */
    private final Map<String, PartialsObserver> partialsObservers;

    /** Number of state variables. */
    private final int stateDimension;

    /** Dimension of flatten STM. */
    private final int dimension;

    /** Simple constructor.
     * @param stmName name of the Cartesian STM additional state
     * @param forceModels force models used in propagation
     * @param attitudeProvider attitude provider used in propagation
     * @param stateDimension dimension of state vector
     */
    AbstractStateTransitionMatrixGenerator(final String stmName, final List<ForceModel> forceModels,
                                           final AttitudeProvider attitudeProvider, final int stateDimension) {
        this.stmName           = stmName;
        this.forceModels       = forceModels;
        this.attitudeProvider  = attitudeProvider;
        this.stateDimension    = stateDimension;
        this.dimension         = stateDimension * stateDimension;
        this.partialsObservers = new HashMap<>();
    }

    /** Register an observer for partial derivatives.
     * <p>
     * The observer {@link PartialsObserver#partialsComputed(SpacecraftState, double[], double[])} partialsComputed}
     * method will be called when partial derivatives are computed, as a side effect of
     * calling {@link #computePartials(SpacecraftState)} (SpacecraftState)}
     * </p>
     * @param name name of the parameter driver this observer is interested in (may be null)
     * @param observer observer to register
     */
    void addObserver(final String name, final PartialsObserver observer) {
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
        return dimension;
    }

    /**
     * Getter for the number of state variables.
     * @return state vector dimension
     */
    public int getStateDimension() {
        return stateDimension;
    }

    /**
     * Protected getter for the force models.
     * @return forces
     */
    protected List<ForceModel> getForceModels() {
        return forceModels;
    }

    /**
     * Protected getter for the partials observers map.
     * @return map
     */
    protected Map<String, PartialsObserver> getPartialsObservers() {
        return partialsObservers;
    }

    /**
     * Method to build a linear system solver.
     * @param matrix equations matrix
     * @return solver
     */
    private DecompositionSolver getDecompositionSolver(final RealMatrix matrix) {
        return new QRDecomposition(matrix, THRESHOLD).getSolver();
    }

    /** Set the initial value of the State Transition Matrix.
     * <p>
     * The returned state must be added to the propagator.
     * </p>
     * @param state initial state
     * @param dYdY0 initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @param orbitType orbit type used for states Y and Y₀ in {@code dYdY0}
     * @param positionAngleType position angle used states Y and Y₀ in {@code dYdY0}
     * @return state with initial STM (converted to Cartesian ∂C/∂Y₀) added
     */
    SpacecraftState setInitialStateTransitionMatrix(final SpacecraftState state, final RealMatrix dYdY0,
                                                    final OrbitType orbitType,
                                                    final PositionAngleType positionAngleType) {

        final RealMatrix nonNullDYdY0;
        if (dYdY0 == null) {
            nonNullDYdY0 = MatrixUtils.createRealIdentityMatrix(getStateDimension());
        } else {
            if (dYdY0.getRowDimension() != getStateDimension() ||
                    dYdY0.getColumnDimension() != getStateDimension()) {
                throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2,
                        dYdY0.getRowDimension(), dYdY0.getColumnDimension(),
                        getStateDimension(), getStateDimension());
            }
            nonNullDYdY0 = dYdY0;
        }

        // convert to Cartesian STM
        final RealMatrix dCdY0;
        if (state.isOrbitDefined()) {
            final RealMatrix dYdC = MatrixUtils.createRealIdentityMatrix(getStateDimension());
            final Orbit orbit = orbitType.convertType(state.getOrbit());
            final double[][] jacobian = new double[6][6];
            orbit.getJacobianWrtCartesian(positionAngleType, jacobian);
            dYdC.setSubMatrix(jacobian, 0, 0);
            final DecompositionSolver decomposition = getDecompositionSolver(dYdC);
            dCdY0 = decomposition.solve(nonNullDYdY0);
        } else {
            dCdY0 = nonNullDYdY0;
        }

        // set additional state
        return state.addAdditionalData(getName(), flatten(dCdY0));

    }

    /**
     * Flattens a matrix into an 1-D array.
     * @param matrix matrix to be flatten
     * @return array
     */
    double[] flatten(final RealMatrix matrix) {
        final double[] flat = new double[getDimension()];
        int k = 0;
        for (int i = 0; i < getStateDimension(); ++i) {
            for (int j = 0; j < getStateDimension(); ++j) {
                flat[k++] = matrix.getEntry(i, j);
            }
        }
        return flat;
    }

    /** {@inheritDoc} */
    @Override
    public boolean yields(final SpacecraftState state) {
        return !state.hasAdditionalData(getName());
    }

    /** {@inheritDoc} */
    public CombinedDerivatives combinedDerivatives(final SpacecraftState state) {
        final double[] factor = computePartials(state);

        // retrieve current State Transition Matrix
        final double[] p    = state.getAdditionalState(getName());
        final double[] pDot = new double[p.length];

        // perform multiplication
        multiplyMatrix(factor, p, pDot, getStateDimension());

        return new CombinedDerivatives(pDot, null);

    }

    /** Compute evolution matrix product.
     * @param factor factor matrix
     * @param x right factor of the multiplication, as a flatten array in row major order
     * @param y placeholder where to put the result, as a flatten array in row major order
     * @param columns number of columns of both x and y (so their dimensions are the state one times the columns)
     */
    abstract void multiplyMatrix(double[] factor, double[] x, double[] y, int columns);

    /** Compute the various partial derivatives.
     * @param state current spacecraft state
     * @return factor matrix
     */
    double[] computePartials(final SpacecraftState state) {

        // set up containers for partial derivatives
        final double[]              factor               = new double[(stateDimension - SPACE_DIMENSION) * stateDimension];
        final DoubleArrayDictionary partialsDictionary = new DoubleArrayDictionary();

        // evaluate contribution of all force models
        final AttitudeProvider equivalentAttitudeProvider = wrapAttitudeProviderIfPossible();
        final boolean isThereAnyForceNotDependingOnlyOnPosition = getForceModels().stream().anyMatch(force -> !force.dependsOnPositionOnly());
        final NumericalGradientConverter posOnlyConverter = new NumericalGradientConverter(state, SPACE_DIMENSION, equivalentAttitudeProvider);
        final NumericalGradientConverter fullConverter = isThereAnyForceNotDependingOnlyOnPosition ?
                new NumericalGradientConverter(state, getStateDimension(), equivalentAttitudeProvider) : posOnlyConverter;

        for (final ForceModel forceModel : getForceModels()) {

            final NumericalGradientConverter     converter    = forceModel.dependsOnPositionOnly() ? posOnlyConverter : fullConverter;
            final FieldSpacecraftState<Gradient> dsState      = converter.getState(forceModel);
            final Gradient[]                     parameters   = converter.getParametersAtStateDate(dsState, forceModel);

            // update partial derivatives w.r.t. state variables
            final Gradient[] ratesPartials = computeRatesPartialsAndUpdateFactor(forceModel, dsState, parameters, factor);

            // partials derivatives with respect to parameters
            updateFactorForParameters(forceModel, converter, ratesPartials, partialsDictionary, state, factor);

        }

        return factor;

    }

    /**
     * Compute with automatic differentiation the partial derivatives of state variables' rate
     * that are not part of the position vector.
     * @param forceModel force model
     * @param fieldState state in Taylor differential algebra
     * @param parameters force parameters in Taylor differential algebra
     * @param factor factor matrix to update
     * @return array of rates in Taylor differential algebra
     */
    abstract Gradient[] computeRatesPartialsAndUpdateFactor(ForceModel forceModel,
                                                            FieldSpacecraftState<Gradient> fieldState,
                                                            Gradient[] parameters, double[] factor);

    /**
     * Update factor regarding partials of force model parameters.
     * @param forceModel force
     * @param converter gradient converter
     * @param ratesPartials state variables' rates evaluated in the Taylor differential algebra
     * @param partialsDictionary dictionary storing the partials
     * @param state spacecraft state
     * @param factor factor matrix (flattened)
     */
    private void updateFactorForParameters(final ForceModel forceModel, final NumericalGradientConverter converter,
                                           final Gradient[] ratesPartials, final DoubleArrayDictionary partialsDictionary,
                                           final SpacecraftState state, final double[] factor) {
        int paramsIndex = converter.getFreeStateParameters();
        for (ParameterDriver driver : forceModel.getParametersDrivers()) {
            if (driver.isSelected()) {

                // for each span (for each estimated value) corresponding name is added
                for (TimeSpanMap.Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    updateDictionaryEntry(partialsDictionary, span, ratesPartials, paramsIndex);
                    ++paramsIndex;
                }
            }
        }

        // notify observers
        for (Map.Entry<String, PartialsObserver> observersEntry : getPartialsObservers().entrySet()) {
            final DoubleArrayDictionary.Entry entry = partialsDictionary.getEntry(observersEntry.getKey());
            observersEntry.getValue().partialsComputed(state, factor, entry == null ? new double[ratesPartials.length] : entry.getValue());
        }
    }

    /**
     * Update entry of dictionary with derivative information.
     * @param partialsDictionary dictionary
     * @param span time span
     * @param ratesPartials state variables' rates evaluated in the Taylor differential algebra
     * @param paramsIndex index of parameter as an independent variable of the differential algebra
     */
    private void updateDictionaryEntry(final DoubleArrayDictionary partialsDictionary, final TimeSpanMap.Span<String> span,
                                       final Gradient[] ratesPartials, final int paramsIndex) {
        // get the partials derivatives for this driver
        DoubleArrayDictionary.Entry entry = partialsDictionary.getEntry(span.getData());
        if (entry == null) {
            // create an entry filled with zeroes
            partialsDictionary.put(span.getData(), new double[ratesPartials.length]);
            entry = partialsDictionary.getEntry(span.getData());
        }

        // add the contribution of the current force model
        final double[] increment = new double[ratesPartials.length];
        for (int i = 0; i < ratesPartials.length; ++i) {
            increment[i] = ratesPartials[i].getGradient()[paramsIndex];
        }
        entry.increment(increment);
    }

    /**
     * Method that first checks if it is possible to replace the attitude provider with a computationally cheaper one
     * to evaluate. If applicable, the new provider only computes the rotation and uses dummy rate and acceleration,
     * since they should not be used later on.
     * @return same provider if at least one forces used attitude derivatives, otherwise one wrapping the old one for
     * the rotation
     */
    AttitudeProvider wrapAttitudeProviderIfPossible() {
        if (forceModels.stream().anyMatch(ForceModel::dependsOnAttitudeRate)) {
            // at least one force uses an attitude rate, need to keep the original provider
            return attitudeProvider;
        } else {
            // the original provider can be replaced by a lighter one for performance
            return AttitudeProviderModifier.getFrozenAttitudeProvider(attitudeProvider);
        }
    }

    /** Interface for observing partials derivatives. */
    @FunctionalInterface
    public interface PartialsObserver {

        /** Callback called when partial derivatives have been computed.
         * @param state current spacecraft state
         * @param factor factor matrix, flattened along rows
         * @param partials partials derivatives of all state variables' rates (except from position) w.r.t. the parameter driver
         * that was registered (zero if no parameters were not selected or parameter is unknown)
         */
        void partialsComputed(SpacecraftState state, double[] factor, double[] partials);

    }

}

