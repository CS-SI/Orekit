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
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldResetDerivativesOnEvent;

import java.util.stream.Stream;

/**
 * Class for fuel cost with Cartesian coordinates.
 * It is the integral over time of the Euclidean norm of the thrust vector.
 *
 * @author Romain Serra
 * @see CartesianCost
 * @since 13.0
 */
public class FieldCartesianFuelCost<T extends CalculusFieldElement<T>> extends FieldAbstractCartesianCost<T> {

    /** Maximum value of thrust force Euclidean norm. */
    private final T maximumThrustMagnitude;

    /** Detection settings for singularity detection. */
    private final FieldEventDetectionSettings<T> eventDetectionSettings;

    /**
     * Constructor with default detection settings.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     */
    public FieldCartesianFuelCost(final String name, final T massFlowRateFactor, final T maximumThrustMagnitude) {
        this(name, massFlowRateFactor, maximumThrustMagnitude, new FieldEventDetectionSettings<>(massFlowRateFactor.getField(),
                EventDetectionSettings.getDefaultEventDetectionSettings()));
    }

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param eventDetectionSettings singularity event detection settings
     */
    public FieldCartesianFuelCost(final String name, final T massFlowRateFactor, final T maximumThrustMagnitude,
                                  final FieldEventDetectionSettings<T> eventDetectionSettings) {
        super(name, massFlowRateFactor);
        this.maximumThrustMagnitude = maximumThrustMagnitude;
        this.eventDetectionSettings = eventDetectionSettings;
    }

    /**
     * Getter for event detection settings.
     * @return detection settings.
     */
    public FieldEventDetectionSettings<T> getEventDetectionSettings() {
        return eventDetectionSettings;
    }

    /** Getter for maximum thrust magnitude.
     * @return maximum thrust
     */
    public T getMaximumThrustMagnitude() {
        return maximumThrustMagnitude;
    }

    /**
     * Evaluate switching function (whose sign determines the bang-bang control profile).
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @return value of switch function
     */
    private T evaluateFieldSwitchFunction(final T[] adjointVariables, final T mass) {
        return getFieldAdjointVelocityNorm(adjointVariables).divide(mass).subtract(adjointVariables[6].multiply(getMassFlowRateFactor())).subtract(1.);
    }

    /**
     * Computes the direction of thrust.
     * @param adjointVariables adjoint vector
     * @return thrust direction
     */
    private FieldVector3D<T> getFieldThrustDirection(final T[] adjointVariables) {
        return new FieldVector3D<>(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize();
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector3D<T> getFieldThrustAccelerationVector(final T[] adjointVariables, final T mass) {
        final T switchFunction = evaluateFieldSwitchFunction(adjointVariables, mass);
        if (switchFunction.getReal() > 0.) {
            return getFieldThrustDirection(adjointVariables).scalarMultiply(mass.reciprocal().multiply(maximumThrustMagnitude));
        } else {
            return FieldVector3D.getZero(mass.getField());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateFieldAdjointDerivatives(final T[] adjointVariables, final T mass,
                                              final T[] adjointDerivatives) {
        final T switchFunction = evaluateFieldSwitchFunction(adjointVariables, mass);
        if (switchFunction.getReal() > 0.) {
            adjointDerivatives[6] = adjointDerivatives[6].add(getFieldAdjointVelocityNorm(adjointVariables).multiply(maximumThrustMagnitude).divide(mass.square()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public T getFieldHamiltonianContribution(final T[] adjointVariables, final T mass) {
        final FieldVector3D<T> thrustForce = getFieldThrustAccelerationVector(adjointVariables, mass).scalarMultiply(mass);
        return thrustForce.getNorm().negate();
    }

    /** {@inheritDoc} */
    @Override
    public Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        return Stream.of(new FieldSwitchDetector());
    }

    @Override
    public CartesianFuelCost toCartesianCost() {
        return new CartesianFuelCost(getAdjointName(), getMassFlowRateFactor().getReal(), maximumThrustMagnitude.getReal(),
                getEventDetectionSettings().toEventDetectionSettings());
    }

    /**
     * Field event detector for bang-bang switches.
     */
    private class FieldSwitchDetector implements FieldEventDetector<T> {

        /** Event handler. */
        private final FieldResetDerivativesOnEvent<T> handler = new FieldResetDerivativesOnEvent<>();

        /** {@inheritDoc} */
        @Override
        public T g(final FieldSpacecraftState<T> state) {
            final T[] adjoint = state.getAdditionalState(getAdjointName());
            return evaluateFieldSwitchFunction(adjoint, state.getMass());
        }

        @Override
        public FieldEventDetectionSettings<T> getDetectionSettings() {
            return eventDetectionSettings;
        }

        @Override
        public FieldEventHandler<T> getHandler() {
            return handler;
        }
    }
}
