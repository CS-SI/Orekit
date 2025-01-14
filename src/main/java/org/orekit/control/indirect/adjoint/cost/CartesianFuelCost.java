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
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;

import java.util.stream.Stream;

/**
 * Class for fuel cost with Cartesian coordinates.
 * It is the integral over time of the Euclidean norm of the thrust vector.
 *
 * @author Romain Serra
 * @see CartesianCost
 * @since 13.0
 */
public class CartesianFuelCost extends AbstractCartesianCost {

    /** Maximum value of thrust force Euclidean norm. */
    private final double maximumThrustMagnitude;

    /** Detection settings for singularity detection. */
    private final EventDetectionSettings eventDetectionSettings;

    /**
     * Constructor with default detection settings.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     */
    public CartesianFuelCost(final String name, final double massFlowRateFactor, final double maximumThrustMagnitude) {
        this(name, massFlowRateFactor, maximumThrustMagnitude, EventDetectionSettings.getDefaultEventDetectionSettings());
    }

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param eventDetectionSettings singularity event detection settings
     */
    public CartesianFuelCost(final String name, final double massFlowRateFactor, final double maximumThrustMagnitude,
                             final EventDetectionSettings eventDetectionSettings) {
        super(name, massFlowRateFactor);
        this.maximumThrustMagnitude = maximumThrustMagnitude;
        this.eventDetectionSettings = eventDetectionSettings;
    }

    /** Getter for maximum thrust magnitude.
     * @return maximum thrust
     */
    public double getMaximumThrustMagnitude() {
        return maximumThrustMagnitude;
    }

    /**
     * Getter for event detection settings.
     * @return detection settings.
     */
    public EventDetectionSettings getEventDetectionSettings() {
        return eventDetectionSettings;
    }

    /**
     * Evaluate switching function (whose sign determines the bang-bang control profile).
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @return value of switch function
     */
    private double evaluateSwitchFunction(final double[] adjointVariables, final double mass) {
        double switchFunction = getAdjointVelocityNorm(adjointVariables) / mass - 1.;
        if (getAdjointDimension() > 6) {
            switchFunction -= adjointVariables[6] * getMassFlowRateFactor();
        }
        return switchFunction;
    }

    /**
     * Computes the direction of thrust.
     * @param adjointVariables adjoint vector
     * @return thrust direction
     */
    private Vector3D getThrustDirection(final double[] adjointVariables) {
        return new Vector3D(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustAccelerationVector(final double[] adjointVariables, final double mass) {
        final double switchFunction = evaluateSwitchFunction(adjointVariables, mass);
        if (switchFunction > 0.) {
            return getThrustDirection(adjointVariables).scalarMultiply(maximumThrustMagnitude / mass);
        } else {
            return Vector3D.ZERO;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateAdjointDerivatives(final double[] adjointVariables, final double mass,
                                         final double[] adjointDerivatives) {
        if (getAdjointDimension() > 6) {
            final double switchFunction = evaluateSwitchFunction(adjointVariables, mass);
            if (switchFunction > 0.) {
                adjointDerivatives[6] += getAdjointVelocityNorm(adjointVariables) * maximumThrustMagnitude / (mass * mass);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getHamiltonianContribution(final double[] adjointVariables, final double mass) {
        final Vector3D thrustForce = getThrustAccelerationVector(adjointVariables, mass).scalarMultiply(mass);
        return -thrustForce.getNorm();
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.of(new FuelCostSwitchDetector(eventDetectionSettings));
    }

    /**
     * Event detector for bang-bang switches.
     */
    private class FuelCostSwitchDetector extends ControlSwitchDetector {

        /**
         * Constructor.
         * @param detectionSettings detection settings.
         */
        FuelCostSwitchDetector(final EventDetectionSettings detectionSettings) {
            super(detectionSettings);
        }

        /** {@inheritDoc} */
        @Override
        public double g(final SpacecraftState state) {
            final double[] adjoint = state.getAdditionalState(getAdjointName());
            return evaluateSwitchFunction(adjoint, state.getMass());
        }
    }

}
