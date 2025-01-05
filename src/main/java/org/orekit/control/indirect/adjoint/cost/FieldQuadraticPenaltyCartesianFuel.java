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
package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;

import java.util.stream.Stream;

/**
 * Fuel cost penalized with a quadratic term. For epsilon equal to 1, one gets the bounded energy cost.
 *
 * @author Romain Serra
 * @since 13.0
 * @see BoundedCartesianEnergy
 */
public class FieldQuadraticPenaltyCartesianFuel<T extends CalculusFieldElement<T>>
        extends FieldPenalizedCartesianFuelCost<T> {

    /** Detection settings for singularity detection. */
    private final FieldEventDetectionSettings<T> eventDetectionSettings;

    /**
     * Constructor.
     *
     * @param name                   adjoint name
     * @param massFlowRateFactor     mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param epsilon                penalty weight
     * @param eventDetectionSettings detection settings
     */
    public FieldQuadraticPenaltyCartesianFuel(final String name, final T massFlowRateFactor,
                                              final T maximumThrustMagnitude, final T epsilon,
                                              final FieldEventDetectionSettings<T> eventDetectionSettings) {
        super(name, massFlowRateFactor, maximumThrustMagnitude, epsilon);
        this.eventDetectionSettings = eventDetectionSettings;
    }

    /**
     * Constructor with default event detection settings.
     *
     * @param name                   adjoint name
     * @param massFlowRateFactor     mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param epsilon                penalty weight
     */
    public FieldQuadraticPenaltyCartesianFuel(final String name, final T massFlowRateFactor,
                                              final T maximumThrustMagnitude, final T epsilon) {
        this(name, massFlowRateFactor, maximumThrustMagnitude, epsilon, new FieldEventDetectionSettings<>(massFlowRateFactor.getField(),
                EventDetectionSettings.getDefaultEventDetectionSettings()));
    }

    /**
     * Getter for the event detection settings.
     * @return detection settings
     */
    public FieldEventDetectionSettings<T> getEventDetectionSettings() {
        return eventDetectionSettings;
    }

    /** {@inheritDoc} */
    @Override
    public T evaluateFieldPenaltyFunction(final T controlNorm) {
        return controlNorm.multiply(controlNorm.multiply(getMaximumThrustMagnitude()).divide(2).subtract(1.));
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector3D<T> getFieldThrustAccelerationVector(final T[] adjointVariables, final T mass) {
        final T switchFunction = evaluateSwitchFunction(adjointVariables, mass);
        if (switchFunction.getReal() > 0) {
            final T thrustForceMagnitude = FastMath.min(switchFunction, getMaximumThrustMagnitude());
            return getFieldThrustDirection(adjointVariables).scalarMultiply(thrustForceMagnitude.divide(mass));
        } else {
            return FieldVector3D.getZero(mass.getField());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateFieldAdjointDerivatives(final T[] adjointVariables, final T mass, final T[] adjointDerivatives) {
        final T switchFunction = evaluateSwitchFunction(adjointVariables, mass);
        if (switchFunction.getReal() > 0.) {
            final T minimum = FastMath.min(switchFunction, getMaximumThrustMagnitude());
            adjointDerivatives[6] = adjointDerivatives[6].add(getFieldAdjointVelocityNorm(adjointVariables)
                    .multiply(minimum).divide(mass.square()));
        }
    }

    /**
     * Evaluate switching function (whose value determines the control profile).
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @return value of switch function
     */
    private T evaluateSwitchFunction(final T[] adjointVariables, final T mass) {
        return (getFieldAdjointVelocityNorm(adjointVariables).divide(mass).subtract(adjointVariables[6].multiply(getMassFlowRateFactor())).subtract(1.)).divide(getEpsilon()).add(1);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        return Stream.of(new QuadraticallyPenalizedSwitchDetector(getEventDetectionSettings(), field.getZero()),
                new QuadraticallyPenalizedSwitchDetector(getEventDetectionSettings(), getMaximumThrustMagnitude()));
    }

    /**
     * Event detector for bang-bang switches.
     */
    private class QuadraticallyPenalizedSwitchDetector extends FieldControlSwitchDetector<T> {

        /** Critical value at which the switching function has an event. */
        private final T criticalValue;

        /**
         * Constructor.
         * @param detectionSettings detection settings.
         * @param criticalValue switch function value to detect
         */
        QuadraticallyPenalizedSwitchDetector(final FieldEventDetectionSettings<T> detectionSettings,
                                             final T criticalValue) {
            super(detectionSettings);
            this.criticalValue = criticalValue;
        }

        /** {@inheritDoc} */
        @Override
        public T g(final FieldSpacecraftState<T> state) {
            final T[] adjoint = state.getAdditionalState(getAdjointName());
            return evaluateSwitchFunction(adjoint, state.getMass()).subtract(criticalValue);
        }
    }

    /** {@inheritDoc} */
    @Override
    public QuadraticPenaltyCartesianFuel toCartesianCost() {
        return new QuadraticPenaltyCartesianFuel(getAdjointName(), getMassFlowRateFactor().getReal(),
                getMaximumThrustMagnitude().getReal(), getEpsilon().getReal(),
                getEventDetectionSettings().toEventDetectionSettings());
    }
}
