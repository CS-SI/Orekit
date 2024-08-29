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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.AdaptableInterval;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldAdaptableInterval;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;

/**
 * Abstract class for energy cost with Cartesian coordinates and non-zero mass flow rate.
 * @author Romain Serra
 * @see AbstractCartesianEnergy
 * @since 12.2
 */
abstract class CartesianEnergyConsideringMass extends AbstractCartesianEnergy {

    /** Detection settings for singularity detection. */
    private final EventDetectionSettings eventDetectionSettings;

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param eventDetectionSettings settings for singularity detections
     */
    protected CartesianEnergyConsideringMass(final String name, final double massFlowRateFactor,
                                             final EventDetectionSettings eventDetectionSettings) {
        super(name, massFlowRateFactor);
        this.eventDetectionSettings = eventDetectionSettings;
    }

    /**
     * Getter for event detection settings.
     * @return detection settings.
     */
    public EventDetectionSettings getEventDetectionSettings() {
        return eventDetectionSettings;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustAccelerationVector(final double[] adjointVariables, final double mass) {
        return getThrustDirection(adjointVariables).scalarMultiply(getThrustForceNorm(adjointVariables, mass) / mass);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldThrustAccelerationVector(final T[] adjointVariables,
                                                                                                 final T mass) {
        return getFieldThrustDirection(adjointVariables).scalarMultiply(getFieldThrustForceNorm(adjointVariables, mass).divide(mass));
    }

    /**
     * Computes the direction of thrust.
     * @param adjointVariables adjoint vector
     * @return thrust direction
     */
    protected Vector3D getThrustDirection(final double[] adjointVariables) {
        return new Vector3D(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize();
    }

    /**
     * Computes the direction of thrust.
     * @param adjointVariables adjoint vector
     * @param <T> field type
     * @return thrust direction
     */
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldThrustDirection(final T[] adjointVariables) {
        return new FieldVector3D<>(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize();
    }

    /**
     * Computes the Euclidean norm of the thrust force.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @return norm of thrust
     */
    protected abstract double getThrustForceNorm(double[] adjointVariables, double mass);

    /**
     * Computes the Euclidean norm of the thrust force.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @param <T> field type
     * @return norm of thrust
     */
    protected abstract <T extends CalculusFieldElement<T>> T getFieldThrustForceNorm(T[] adjointVariables, T mass);

    /** {@inheritDoc} */
    @Override
    public void updateAdjointDerivatives(final double[] adjointVariables, final double mass,
                                         final double[] adjointDerivatives) {
        adjointDerivatives[6] += getThrustForceNorm(adjointVariables, mass) * getAdjointVelocityNorm(adjointVariables) / (mass * mass);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void updateFieldAdjointDerivatives(final T[] adjointVariables, final T mass,
                                                                                  final T[] adjointDerivatives) {
        adjointDerivatives[6] = adjointDerivatives[6].add(getFieldThrustForceNorm(adjointVariables, mass)
            .multiply(getFieldAdjointVelocityNorm(adjointVariables)).divide(mass.square()));
    }

    /** {@inheritDoc} */
    @Override
    public double getHamiltonianContribution(final double[] adjointVariables, final double mass) {
        final Vector3D thrustForce = getThrustAccelerationVector(adjointVariables, mass).scalarMultiply(mass);
        return -thrustForce.getNormSq() / 2.;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T getFieldHamiltonianContribution(final T[] adjointVariables, final T mass) {
        final FieldVector3D<T> thrustForce = getFieldThrustAccelerationVector(adjointVariables, mass).scalarMultiply(mass);
        return thrustForce.getNormSq().multiply(-1. / 2.);
    }

    /**
     * Event detector for singularities in adjoint dynamics.
     */
    class SingularityDetector extends AbstractDetector<SingularityDetector> {

        /** Value to detect. */
        private final double detectionValue;

        /**
         * Constructor.
         * @param eventDetectionSettings detection settings
         * @param handler event handler
         * @param detectionValue value to detect
         */
        SingularityDetector(final EventDetectionSettings eventDetectionSettings, final EventHandler handler,
                            final double detectionValue) {
            super(eventDetectionSettings, handler);
            this.detectionValue = detectionValue;
        }

        /** {@inheritDoc} */
        @Override
        public double g(final SpacecraftState state) {
            final double[] adjoint = state.getAdditionalState(getAdjointName());
            return evaluateVariablePart(adjoint, state.getMass()) - detectionValue;
        }

        /**
         * Evaluate variable part of singularity function.
         * @param adjointVariables adjoint vector
         * @param mass mass
         * @return singularity function without the constant part
         */
        private double evaluateVariablePart(final double[] adjointVariables, final double mass) {
            final double adjointVelocityNorm = getAdjointVelocityNorm(adjointVariables);
            return adjointVelocityNorm / mass - getMassFlowRateFactor() * adjointVariables[6];
        }

        @Override
        protected SingularityDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                             final int newMaxIter, final EventHandler newHandler) {
            return new SingularityDetector(new EventDetectionSettings(newMaxCheck, newThreshold, newMaxIter), newHandler,
                detectionValue);
        }
    }

    /**
     * Field event detector for singularities in adjoint dynamics.
     */
    class FieldSingularityDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldSingularityDetector<T>, T> {

        /** Value to detect. */
        private final T detectionValue;

        /**
         * Constructor.
         * @param eventDetectionSettings detection settings
         * @param handler event handler
         * @param detectionValue value to detect
         */
        protected FieldSingularityDetector(final FieldEventDetectionSettings<T> eventDetectionSettings,
                                           final FieldEventHandler<T> handler, final T detectionValue) {
            super(eventDetectionSettings, handler);
            this.detectionValue = detectionValue;
        }

        /** {@inheritDoc} */
        @Override
        public T g(final FieldSpacecraftState<T> state) {
            final T[] adjoint = state.getAdditionalState(getAdjointName());
            return evaluateVariablePart(adjoint, state.getMass()).subtract(detectionValue);
        }

        /**
         * Evaluate variable part of singularity function.
         * @param adjointVariables adjoint vector
         * @param mass mass
         * @return singularity function without the constant part
         */
        private T evaluateVariablePart(final T[] adjointVariables, final T mass) {
            final T adjointVelocityNorm = getFieldAdjointVelocityNorm(adjointVariables);
            return adjointVelocityNorm.divide(mass).subtract(adjointVariables[6].multiply(getMassFlowRateFactor()));
        }

        @Override
        protected FieldSingularityDetector<T> create(final FieldAdaptableInterval<T> newMaxCheck, final T newThreshold,
                                                     final int newMaxIter, final FieldEventHandler<T> newHandler) {
            return new FieldSingularityDetector<>(new FieldEventDetectionSettings<>(newMaxCheck, newThreshold, newMaxIter),
                newHandler, detectionValue);
        }
    }
}
