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
 * Class for unbounded energy cost with Cartesian coordinates.
 * Here, the control vector is chosen as the thrust force, expressed in the propagation frame.
 * This leads to the optimal thrust being in the same direction as the adjoint velocity.
 * @author Romain Serra
 * @see UnboundedCartesianEnergyNeglectingMass
 * @since 12.2
 */
public class UnboundedCartesianEnergy extends CartesianEnergyConsideringMass {

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param eventDetectionSettings detection settings for singularity detections
     */
    public UnboundedCartesianEnergy(final String name, final double massFlowRateFactor,
                                    final EventDetectionSettings eventDetectionSettings) {
        super(name, massFlowRateFactor, eventDetectionSettings);
    }

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     */
    public UnboundedCartesianEnergy(final String name, final double massFlowRateFactor) {
        this(name, massFlowRateFactor, EventDetectionSettings.getDefaultEventDetectionSettings());
    }

    /** {@inheritDoc} */
    @Override
    protected double getThrustForceNorm(final double[] adjointVariables, final double mass) {
        final double adjointVelocityNorm = getAdjointVelocityNorm(adjointVariables);
        final double factor = adjointVelocityNorm / mass - getMassFlowRateFactor() * adjointVariables[6];
        return FastMath.max(0., factor);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.of(new SingularityDetector(0.));
    }
}
