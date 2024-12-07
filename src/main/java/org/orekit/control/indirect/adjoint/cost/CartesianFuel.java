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


import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldResetDerivativesOnEvent;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;

import java.util.stream.Stream;

/**
 * Class for fuel cost with Cartesian coordinates.
 * It is the integral over time of the Euclidean norm of the thrust vector.
 *
 * @author Romain Serra
 * @see CartesianCost
 * @since 13.0
 */
public class CartesianFuel extends AbstractCartesianCost {

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
    public CartesianFuel(final String name, final double massFlowRateFactor, final double maximumThrustMagnitude) {
        this(name, massFlowRateFactor, maximumThrustMagnitude, EventDetectionSettings.getDefaultEventDetectionSettings());
    }

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param eventDetectionSettings singularity event detection settings
     */
    public CartesianFuel(final String name, final double massFlowRateFactor, final double maximumThrustMagnitude,
                         final EventDetectionSettings eventDetectionSettings) {
        super(name, massFlowRateFactor);
        this.maximumThrustMagnitude = maximumThrustMagnitude;
        this.eventDetectionSettings = eventDetectionSettings;
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
        return getAdjointVelocityNorm(adjointVariables) / mass - adjointVariables[6] * getMassFlowRateFactor() - 1.;
    }

    /**
     * Evaluate switching function (whose sign determines the bang-bang control profile).
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @param <T> field type
     * @return value of switch function
     */
    private <T extends CalculusFieldElement<T>> T evaluateFieldSwitchFunction(final T[] adjointVariables, final T mass) {
        return getFieldAdjointVelocityNorm(adjointVariables).divide(mass).subtract(adjointVariables[6].multiply(getMassFlowRateFactor())).subtract(1.);
    }

    /**
     * Computes the direction of thrust.
     * @param adjointVariables adjoint vector
     * @return thrust direction
     */
    private Vector3D getThrustDirection(final double[] adjointVariables) {
        return new Vector3D(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize();
    }

    /**
     * Computes the direction of thrust.
     * @param adjointVariables adjoint vector
     * @param <T> field type
     * @return thrust direction
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldThrustDirection(final T[] adjointVariables) {
        return new FieldVector3D<>(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize();
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
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldThrustAccelerationVector(final T[] adjointVariables,
                                                                                                 final T mass) {
        final T switchFunction = evaluateFieldSwitchFunction(adjointVariables, mass);
        if (switchFunction.getReal() > 0.) {
            return getFieldThrustDirection(adjointVariables).scalarMultiply(mass.reciprocal().multiply(maximumThrustMagnitude));
        } else {
            return FieldVector3D.getZero(mass.getField());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateAdjointDerivatives(final double[] adjointVariables, final double mass,
                                         final double[] adjointDerivatives) {
        final double switchFunction = evaluateSwitchFunction(adjointVariables, mass);
        if (switchFunction > 0.) {
            adjointDerivatives[6] += getAdjointVelocityNorm(adjointVariables) * maximumThrustMagnitude / (mass * mass);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void updateFieldAdjointDerivatives(final T[] adjointVariables,
                                                                                  final T mass,
                                                                                  final T[] adjointDerivatives) {
        final T switchFunction = evaluateFieldSwitchFunction(adjointVariables, mass);
        if (switchFunction.getReal() > 0.) {
            adjointDerivatives[6] = adjointDerivatives[6].add(getFieldAdjointVelocityNorm(adjointVariables).multiply(maximumThrustMagnitude).divide(mass.square()));
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
    public <T extends CalculusFieldElement<T>> T getFieldHamiltonianContribution(final T[] adjointVariables, final T mass) {
        final FieldVector3D<T> thrustForce = getFieldThrustAccelerationVector(adjointVariables, mass).scalarMultiply(mass);
        return thrustForce.getNorm().negate();
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.of(new SwitchDetector(getEventDetectionSettings()));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        return Stream.of(new FieldSwitchDetector<>(new FieldEventDetectionSettings<>(field, getEventDetectionSettings())));
    }

    /**
     * Event detector for bang-bang switches.
     */
    private class SwitchDetector implements EventDetector {

        /** Event detection settings. */
        private final EventDetectionSettings eventDetectionSettings;

        /**
         * Constructor.
         * @param eventDetectionSettings detection settings
         */
        SwitchDetector(final EventDetectionSettings eventDetectionSettings) {
            this.eventDetectionSettings = eventDetectionSettings;
        }

        /** {@inheritDoc} */
        @Override
        public double g(final SpacecraftState state) {
            final double[] adjoint = state.getAdditionalState(getAdjointName());
            return evaluateSwitchFunction(adjoint, state.getMass());
        }

        @Override
        public EventDetectionSettings getDetectionSettings() {
            return eventDetectionSettings;
        }

        @Override
        public EventHandler getHandler() {
            return new ResetDerivativesOnEvent();
        }
    }

    /**
     * Field event detector for bang-bang switches.
     */
    private class FieldSwitchDetector<T extends CalculusFieldElement<T>> implements FieldEventDetector<T> {

        /** Event detection settings. */
        private final FieldEventDetectionSettings<T> eventDetectionSettings;

        /**
         * Constructor.
         * @param eventDetectionSettings detection settings
         */
        FieldSwitchDetector(final FieldEventDetectionSettings<T> eventDetectionSettings) {
            this.eventDetectionSettings = eventDetectionSettings;
        }

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
            return new FieldResetDerivativesOnEvent<>();
        }
    }
}
