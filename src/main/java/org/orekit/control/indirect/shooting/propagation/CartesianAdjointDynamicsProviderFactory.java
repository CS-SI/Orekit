/* Copyright 2022-2025 Romain Serra
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
package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.CartesianAdjointEquationTerm;
import org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.cost.BoundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.CartesianFlightDurationCost;
import org.orekit.control.indirect.adjoint.cost.CartesianFuelCost;
import org.orekit.control.indirect.adjoint.cost.FieldBoundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.FieldCartesianFlightDurationCost;
import org.orekit.control.indirect.adjoint.cost.FieldCartesianFuelCost;
import org.orekit.control.indirect.adjoint.cost.FieldLogarithmicBarrierCartesianFuel;
import org.orekit.control.indirect.adjoint.cost.FieldQuadraticPenaltyCartesianFuel;
import org.orekit.control.indirect.adjoint.cost.FieldUnboundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.FieldUnboundedCartesianEnergyNeglectingMass;
import org.orekit.control.indirect.adjoint.cost.LogarithmicBarrierCartesianFuel;
import org.orekit.control.indirect.adjoint.cost.QuadraticPenaltyCartesianFuel;
import org.orekit.control.indirect.adjoint.cost.UnboundedCartesianEnergy;
import org.orekit.control.indirect.adjoint.cost.UnboundedCartesianEnergyNeglectingMass;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetectionSettings;

/**
 * Factory for common Cartesian adjoint dynamics providers.
 *
 * @see AdjointDynamicsProvider
 * @author Romain Serra
 * @since 13.0
 */
public class CartesianAdjointDynamicsProviderFactory {

    /**
     * Private constructor.
     */
    private CartesianAdjointDynamicsProviderFactory() {
        // factory class
    }

    /**
     * Method building a provider with unbounded Cartesian energy and vanishing mass flow as cost.
     * @param adjointName adjoint name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param cartesianAdjointEquationTerms Cartesian adjoint equation terms
     * @return provider
     */
    public static CartesianAdjointDynamicsProvider buildFlightDurationProvider(final String adjointName,
                                                                               final double massFlowRateFactor,
                                                                               final double maximumThrustMagnitude,
                                                                               final CartesianAdjointEquationTerm... cartesianAdjointEquationTerms) {
        return new CartesianAdjointDynamicsProvider(adjointName, getDimension(massFlowRateFactor)) {

            @Override
            public CartesianAdjointDerivativesProvider buildAdditionalDerivativesProvider() {
                return new CartesianAdjointDerivativesProvider(new CartesianFlightDurationCost(adjointName, massFlowRateFactor, maximumThrustMagnitude),
                        cartesianAdjointEquationTerms);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldCartesianAdjointDerivativesProvider<T> buildFieldAdditionalDerivativesProvider(final Field<T> field) {
                return new FieldCartesianAdjointDerivativesProvider<>(new FieldCartesianFlightDurationCost<>(adjointName,
                        field.getZero().newInstance(massFlowRateFactor), field.getZero().newInstance(maximumThrustMagnitude)),
                        cartesianAdjointEquationTerms);
            }
        };
    }

    /**
     * Method building a provider with unbounded Cartesian energy and vanishing mass flow as cost.
     * @param adjointName adjoint name
     * @param cartesianAdjointEquationTerms Cartesian adjoint equation terms
     * @return provider
     */
    public static CartesianAdjointDynamicsProvider buildUnboundedEnergyProviderNeglectingMass(final String adjointName,
                                                                                              final CartesianAdjointEquationTerm... cartesianAdjointEquationTerms) {
        return new CartesianAdjointDynamicsProvider(adjointName, 6) {

            @Override
            public CartesianAdjointDerivativesProvider buildAdditionalDerivativesProvider() {
                return new CartesianAdjointDerivativesProvider(new UnboundedCartesianEnergyNeglectingMass(adjointName),
                        cartesianAdjointEquationTerms);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldCartesianAdjointDerivativesProvider<T> buildFieldAdditionalDerivativesProvider(final Field<T> field) {
                return new FieldCartesianAdjointDerivativesProvider<>(new FieldUnboundedCartesianEnergyNeglectingMass<>(adjointName, field),
                        cartesianAdjointEquationTerms);
            }
        };
    }

    /**
     * Method building a provider with unbounded Cartesian energy as cost.
     * @param adjointName adjoint name
     * @param massFlowRateFactor mass flow rate factor
     * @param eventDetectionSettings detection settings for adjoint-related events
     * @param cartesianAdjointEquationTerms Cartesian adjoint equation terms
     * @return provider
     */
    public static CartesianAdjointDynamicsProvider buildUnboundedEnergyProvider(final String adjointName,
                                                                                final double massFlowRateFactor,
                                                                                final EventDetectionSettings eventDetectionSettings,
                                                                                final CartesianAdjointEquationTerm... cartesianAdjointEquationTerms) {
        return new CartesianAdjointDynamicsProvider(adjointName, getDimension(massFlowRateFactor)) {

            @Override
            public CartesianAdjointDerivativesProvider buildAdditionalDerivativesProvider() {
                return new CartesianAdjointDerivativesProvider(new UnboundedCartesianEnergy(adjointName,
                        massFlowRateFactor, eventDetectionSettings), cartesianAdjointEquationTerms);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldCartesianAdjointDerivativesProvider<T> buildFieldAdditionalDerivativesProvider(final Field<T> field) {
                return new FieldCartesianAdjointDerivativesProvider<>(new FieldUnboundedCartesianEnergy<>(adjointName,
                        field.getZero().newInstance(massFlowRateFactor), new FieldEventDetectionSettings<>(field,
                        eventDetectionSettings)), cartesianAdjointEquationTerms);
            }
        };
    }

    /**
     * Method building a provider with bounded Cartesian energy as cost.
     * @param adjointName adjoint name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param eventDetectionSettings detection settings for adjoint-related events
     * @param cartesianAdjointEquationTerms Cartesian adjoint equation terms
     * @return provider
     */
    public static CartesianAdjointDynamicsProvider buildBoundedEnergyProvider(final String adjointName,
                                                                              final double massFlowRateFactor,
                                                                              final double maximumThrustMagnitude,
                                                                              final EventDetectionSettings eventDetectionSettings,
                                                                              final CartesianAdjointEquationTerm... cartesianAdjointEquationTerms) {
        return new CartesianAdjointDynamicsProvider(adjointName, getDimension(massFlowRateFactor)) {

            @Override
            public CartesianAdjointDerivativesProvider buildAdditionalDerivativesProvider() {
                return new CartesianAdjointDerivativesProvider(new BoundedCartesianEnergy(adjointName, massFlowRateFactor,
                        maximumThrustMagnitude, eventDetectionSettings), cartesianAdjointEquationTerms);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldCartesianAdjointDerivativesProvider<T> buildFieldAdditionalDerivativesProvider(final Field<T> field) {
                final T zero = field.getZero();
                return new FieldCartesianAdjointDerivativesProvider<>(new FieldBoundedCartesianEnergy<>(adjointName,
                        zero.newInstance(massFlowRateFactor), zero.newInstance(maximumThrustMagnitude),
                        new FieldEventDetectionSettings<>(field, eventDetectionSettings)), cartesianAdjointEquationTerms);
            }
        };
    }

    /**
     * Method building a provider with bounded Cartesian fuel as cost.
     * @param adjointName adjoint name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param eventDetectionSettings detection settings for adjoint-related events
     * @param cartesianAdjointEquationTerms Cartesian adjoint equation terms
     * @return provider
     */
    public static CartesianAdjointDynamicsProvider buildBoundedFuelCostProvider(final String adjointName,
                                                                                final double massFlowRateFactor,
                                                                                final double maximumThrustMagnitude,
                                                                                final EventDetectionSettings eventDetectionSettings,
                                                                                final CartesianAdjointEquationTerm... cartesianAdjointEquationTerms) {
        return new CartesianAdjointDynamicsProvider(adjointName, getDimension(massFlowRateFactor)) {

            @Override
            public CartesianAdjointDerivativesProvider buildAdditionalDerivativesProvider() {
                return new CartesianAdjointDerivativesProvider(new CartesianFuelCost(adjointName, massFlowRateFactor,
                        maximumThrustMagnitude, eventDetectionSettings), cartesianAdjointEquationTerms);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldCartesianAdjointDerivativesProvider<T> buildFieldAdditionalDerivativesProvider(final Field<T> field) {
                final T zero = field.getZero();
                return new FieldCartesianAdjointDerivativesProvider<>(new FieldCartesianFuelCost<>(adjointName,
                        zero.newInstance(massFlowRateFactor), zero.newInstance(maximumThrustMagnitude),
                        new FieldEventDetectionSettings<>(field, eventDetectionSettings)), cartesianAdjointEquationTerms);
            }
        };
    }

    /**
     * Method building a provider with bounded Cartesian fuel penalized with a quadratic term.
     * @param adjointName adjoint name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param epsilon penalty weight
     * @param eventDetectionSettings detection settings for adjoint-related events
     * @param cartesianAdjointEquationTerms Cartesian adjoint equation terms
     * @return provider
     */
    public static CartesianAdjointDynamicsProvider buildQuadraticPenaltyFuelCostProvider(final String adjointName,
                                                                                         final double massFlowRateFactor,
                                                                                         final double maximumThrustMagnitude,
                                                                                         final double epsilon,
                                                                                         final EventDetectionSettings eventDetectionSettings,
                                                                                         final CartesianAdjointEquationTerm... cartesianAdjointEquationTerms) {
        return new CartesianAdjointDynamicsProvider(adjointName, getDimension(massFlowRateFactor)) {

            @Override
            public CartesianAdjointDerivativesProvider buildAdditionalDerivativesProvider() {
                return new CartesianAdjointDerivativesProvider(new QuadraticPenaltyCartesianFuel(adjointName, massFlowRateFactor,
                        maximumThrustMagnitude, epsilon, eventDetectionSettings), cartesianAdjointEquationTerms);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldCartesianAdjointDerivativesProvider<T> buildFieldAdditionalDerivativesProvider(final Field<T> field) {
                final T zero = field.getZero();
                return new FieldCartesianAdjointDerivativesProvider<>(new FieldQuadraticPenaltyCartesianFuel<>(adjointName,
                        zero.newInstance(massFlowRateFactor), zero.newInstance(maximumThrustMagnitude),
                        zero.newInstance(epsilon), new FieldEventDetectionSettings<>(field, eventDetectionSettings)),
                        cartesianAdjointEquationTerms);
            }
        };
    }

    /**
     * Method building a provider with bounded Cartesian fuel penalized with a logarithmic barrier.
     * @param adjointName adjoint name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param epsilon penalty weight
     * @param cartesianAdjointEquationTerms Cartesian adjoint equation terms
     * @return provider
     */
    public static CartesianAdjointDynamicsProvider buildLogarithmicBarrierFuelCostProvider(final String adjointName,
                                                                                           final double massFlowRateFactor,
                                                                                           final double maximumThrustMagnitude,
                                                                                           final double epsilon,
                                                                                           final CartesianAdjointEquationTerm... cartesianAdjointEquationTerms) {
        return new CartesianAdjointDynamicsProvider(adjointName, getDimension(massFlowRateFactor)) {

            @Override
            public CartesianAdjointDerivativesProvider buildAdditionalDerivativesProvider() {
                return new CartesianAdjointDerivativesProvider(new LogarithmicBarrierCartesianFuel(adjointName, massFlowRateFactor,
                        maximumThrustMagnitude, epsilon), cartesianAdjointEquationTerms);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldCartesianAdjointDerivativesProvider<T> buildFieldAdditionalDerivativesProvider(final Field<T> field) {
                final T zero = field.getZero();
                return new FieldCartesianAdjointDerivativesProvider<>(new FieldLogarithmicBarrierCartesianFuel<>(adjointName,
                        zero.newInstance(massFlowRateFactor), zero.newInstance(maximumThrustMagnitude),
                        zero.newInstance(epsilon)), cartesianAdjointEquationTerms);
            }
        };
    }

    /**
     * Get the adjoint dimension.
     * @param massFlowRateFactor mass flow rate factor
     * @return dimension
     */
    private static int getDimension(final double massFlowRateFactor) {
        return massFlowRateFactor == 0. ? 6 : 7;
    }
}
