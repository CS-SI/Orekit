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

import org.hipparchus.util.FastMath;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;

import java.util.stream.Stream;

/**
 * Class for bounded energy cost with Cartesian coordinates.
 * An energy cost is proportional to the integral over time of the squared Euclidean norm of the control vector, often scaled with 1/2.
 * This type of cost is not optimal in terms of mass consumption, however its solutions showcase a smoother behavior favorable for convergence in shooting techniques.
 * Here, the control vector is chosen as the thrust force divided by the maximum thrust magnitude and expressed in the propagation frame.
 *
 * @author Romain Serra
 * @see UnboundedCartesianEnergy
 * @since 12.2
 */
public class BoundedCartesianEnergy extends CartesianEnergyConsideringMass {

    /** Maximum value of thrust force Euclidean norm. */
    private final double maximumThrustMagnitude;

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param eventDetectionSettings singularity event detection settings
     */
    public BoundedCartesianEnergy(final String name, final double massFlowRateFactor,
                                  final double maximumThrustMagnitude,
                                  final EventDetectionSettings eventDetectionSettings) {
        super(name, massFlowRateFactor, eventDetectionSettings);
        this.maximumThrustMagnitude = FastMath.abs(maximumThrustMagnitude);
    }

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     */
    public BoundedCartesianEnergy(final String name, final double massFlowRateFactor,
                                  final double maximumThrustMagnitude) {
        this(name, massFlowRateFactor, maximumThrustMagnitude, EventDetectionSettings.getDefaultEventDetectionSettings());
    }

    /** Getter for maximum thrust magnitude.
     * @return maximum thrust
     * @since 13.0
     */
    public double getMaximumThrustMagnitude() {
        return maximumThrustMagnitude;
    }

    /** {@inheritDoc} */
    @Override
    protected double getThrustForceNorm(final double[] adjointVariables, final double mass) {
        final double adjointVelocityNorm = getAdjointVelocityNorm(adjointVariables);
        final double factor = adjointVelocityNorm / mass - getMassFlowRateFactor() * adjointVariables[6];
        if (factor > maximumThrustMagnitude) {
            return maximumThrustMagnitude;
        } else {
            return FastMath.max(0., factor);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.of(new SingularityDetector(getEventDetectionSettings(), 0.),
                new SingularityDetector(getEventDetectionSettings(), maximumThrustMagnitude));
    }
}
