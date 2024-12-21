/* Copyright 2022-2024 Romain Serra
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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.events.EventDetectionSettings;

/**
 * Abstract class for fuel cost with a penalty term proportional to a weight parameter epsilon.
 * This is typically used in a continuation method, starting from epsilon equal to 1
 * and going towards 0 where the fuel cost is recovered. The point is to enhance convergence.
 * The control vector is the normalized (by the upper bound on magnitude) thrust force in propagation frame.
 *
 * @author Romain Serra
 * @since 13.0
 * @see CartesianFuelCost
 */
public abstract class PenalizedCartesianFuelCost extends AbstractCartesianCost {

    /** Maximum value of thrust force Euclidean norm. */
    private final double maximumThrustMagnitude;

    /** Penalty weight. */
    private final double epsilon;

    /** Detection settings for singularity detection. */
    private final EventDetectionSettings eventDetectionSettings;

    /**
     * Constructor.
     *
     * @param name               adjoint name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param epsilon penalty weight
     * @param eventDetectionSettings detection settings
     */
    protected PenalizedCartesianFuelCost(final String name, final double massFlowRateFactor,
                                         final double maximumThrustMagnitude, final double epsilon,
                                         final EventDetectionSettings eventDetectionSettings) {
        super(name, massFlowRateFactor);
        if (epsilon < 0 || epsilon > 1) {
            throw new OrekitException(OrekitMessages.INVALID_PARAMETER_RANGE, "epsilon", epsilon, 0, 1);
        }
        this.maximumThrustMagnitude = maximumThrustMagnitude;
        this.epsilon = epsilon;
        this.eventDetectionSettings = eventDetectionSettings;
    }

    /** Getter for the penalty weight epsilon.
     * @return epsilon
     */
    public double getEpsilon() {
        return epsilon;
    }

    /** Getter for maximum thrust magnitude.
     * @return maximum thrust
     */
    public double getMaximumThrustMagnitude() {
        return maximumThrustMagnitude;
    }

    /**
     * Getter for the event detection settings.
     * @return detection settings
     */
    public EventDetectionSettings getEventDetectionSettings() {
        return eventDetectionSettings;
    }

    /**
     * Evaluate the penalty term (without the weight), assumed to be a function of the control norm.
     * @param controlNorm Euclidean norm of control vector
     * @return penalty function
     */
    public abstract double evaluatePenaltyFunction(double controlNorm);

    /**
     * Computes the direction of thrust.
     * @param adjointVariables adjoint vector
     * @return thrust direction
     */
    protected Vector3D getThrustDirection(final double[] adjointVariables) {
        return new Vector3D(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize();
    }

    /** {@inheritDoc} */
    @Override
    public double getHamiltonianContribution(final double[] adjointVariables, final double mass) {
        final Vector3D thrustForce = getThrustAccelerationVector(adjointVariables, mass).scalarMultiply(mass);
        final double controlNorm = thrustForce.getNorm() / getMaximumThrustMagnitude();
        return controlNorm + getEpsilon() * evaluatePenaltyFunction(controlNorm);
    }

}
