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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;

import java.util.stream.Stream;

/**
 * Fuel cost penalized with a quadratic term. For epsilon equal to 1, one gets the bounded energy cost.
 *
 * @author Romain Serra
 * @since 13.0
 * @see BoundedCartesianEnergy
 */
public class QuadraticPenaltyCartesianFuel extends PenalizedCartesianFuelCost {

    /** Detection settings for singularity events. */
    private final EventDetectionSettings eventDetectionSettings;

    /**
     * Constructor.
     *
     * @param name                   adjoint name
     * @param massFlowRateFactor     mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param epsilon                penalty weight
     * @param eventDetectionSettings detection settings
     */
    public QuadraticPenaltyCartesianFuel(final String name, final double massFlowRateFactor,
                                         final double maximumThrustMagnitude, final double epsilon,
                                         final EventDetectionSettings eventDetectionSettings) {
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
    public QuadraticPenaltyCartesianFuel(final String name, final double massFlowRateFactor,
                                         final double maximumThrustMagnitude, final double epsilon) {
        this(name, massFlowRateFactor, maximumThrustMagnitude, epsilon,
                EventDetectionSettings.getDefaultEventDetectionSettings());
    }

    /**
     * Getter for the event detection settings.
     * @return detection settings
     */
    public EventDetectionSettings getEventDetectionSettings() {
        return eventDetectionSettings;
    }

    /** {@inheritDoc} */
    @Override
    public double evaluatePenaltyFunction(final double controlNorm) {
        return controlNorm * controlNorm / 2;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustAccelerationVector(final double[] adjointVariables, final double mass) {
        final double switchFunction = evaluateSwitchFunction(adjointVariables, mass);
        if (switchFunction > 0) {
            final double thrustForceMagnitude = FastMath.min(switchFunction, getMaximumThrustMagnitude());
            return getThrustDirection(adjointVariables).scalarMultiply(thrustForceMagnitude / mass);
        } else {
            return Vector3D.ZERO;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateAdjointDerivatives(final double[] adjointVariables, final double mass,
                                         final double[] adjointDerivatives) {
        final double switchFunction = evaluateSwitchFunction(adjointVariables, mass);
        if (switchFunction > 0.) {
            adjointDerivatives[6] += getAdjointVelocityNorm(adjointVariables) *
                    FastMath.min(switchFunction, getMaximumThrustMagnitude()) / (mass * mass);
        }
    }

    /**
     * Evaluate switching function (whose value determines the control profile).
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @return value of switch function
     */
    private double evaluateSwitchFunction(final double[] adjointVariables, final double mass) {
        return (getAdjointVelocityNorm(adjointVariables) / mass - adjointVariables[6] * getMassFlowRateFactor() - 1.) /
                getEpsilon() + 1;
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.of(new QuadraticallyPenalizedSwitchDetector(getEventDetectionSettings(), 0),
                new QuadraticallyPenalizedSwitchDetector(getEventDetectionSettings(), getMaximumThrustMagnitude()));
    }

    /**
     * Event detector for bang-bang switches.
     */
    private class QuadraticallyPenalizedSwitchDetector extends ControlSwitchDetector {

        /** Critical value at which the switching function has an event. */
        private final double criticalValue;

        /**
         * Constructor.
         * @param detectionSettings detection settings.
         * @param criticalValue switch function value to detect
         */
        QuadraticallyPenalizedSwitchDetector(final EventDetectionSettings detectionSettings,
                                             final double criticalValue) {
            super(detectionSettings);
            this.criticalValue = criticalValue;
        }

        /** {@inheritDoc} */
        @Override
        public double g(final SpacecraftState state) {
            final double[] adjoint = state.getAdditionalState(getAdjointName());
            return evaluateSwitchFunction(adjoint, state.getMass()) - criticalValue;
        }
    }
}
