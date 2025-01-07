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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetectionSettings;

/**
 * Abstract class for energy cost with Cartesian coordinates and non-zero mass flow rate.
 * An energy cost is proportional to the integral over time of the squared Euclidean norm of the control vector, often scaled with 1/2.
 * This type of cost is not optimal in terms of mass consumption, however its solutions showcase a smoother behavior favorable for convergence in shooting techniques.
 *
 * @param <T> field type
 * @author Romain Serra
 * @see FieldCartesianCost
 * @see CartesianEnergyConsideringMass
 * @since 13.0
 */
abstract class FieldCartesianEnergyConsideringMass<T extends CalculusFieldElement<T>> implements FieldCartesianCost<T> {

    /** Name of adjoint vector. */
    private final String name;

    /** Mass flow rate factor (always positive). */
    private final T massFlowRateFactor;

    /** Detection settings for singularity detection. */
    private final FieldEventDetectionSettings<T> eventDetectionSettings;

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param eventDetectionSettings settings for singularity detections
     */
    protected FieldCartesianEnergyConsideringMass(final String name, final T massFlowRateFactor,
                                                  final FieldEventDetectionSettings<T> eventDetectionSettings) {
        this.name = name;
        this.massFlowRateFactor = massFlowRateFactor;
        this.eventDetectionSettings = eventDetectionSettings;
    }

    /**
     * Getter for adjoint vector name.
     * @return name
     */
    @Override
    public String getAdjointName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public T getMassFlowRateFactor() {
        return massFlowRateFactor;
    }

    /**
     * Getter for event detection settings.
     * @return detection settings.
     */
    public FieldEventDetectionSettings<T> getEventDetectionSettings() {
        return eventDetectionSettings;
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector3D<T> getFieldThrustAccelerationVector(final T[] adjointVariables, final T mass) {
        return getFieldThrustDirection(adjointVariables).scalarMultiply(getFieldThrustForceNorm(adjointVariables, mass).divide(mass));
    }

    /**
     * Computes the direction of thrust.
     * @param adjointVariables adjoint vector
     * @return thrust direction
     */
    protected FieldVector3D<T> getFieldThrustDirection(final T[] adjointVariables) {
        return new FieldVector3D<>(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize();
    }

    /**
     * Computes the Euclidean norm of the thrust force.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @return norm of thrust
     */
    protected abstract T getFieldThrustForceNorm(T[] adjointVariables, T mass);

    /** {@inheritDoc} */
    @Override
    public void updateFieldAdjointDerivatives(final T[] adjointVariables, final T mass,
                                                                                  final T[] adjointDerivatives) {
        adjointDerivatives[6] = adjointDerivatives[6].add(getFieldThrustForceNorm(adjointVariables, mass)
            .multiply(getFieldAdjointVelocityNorm(adjointVariables)).divide(mass.square()));
    }

    /**
     * Computes the Euclidean norm of the adjoint velocity vector.
     * @param adjointVariables adjoint vector
     * @return norm of adjoint velocity
     */
    protected T getFieldAdjointVelocityNorm(final T[] adjointVariables) {
        return FastMath.sqrt(adjointVariables[3].square().add(adjointVariables[4].square()).add(adjointVariables[5].square()));
    }

    /** {@inheritDoc} */
    @Override
    public T getFieldHamiltonianContribution(final T[] adjointVariables, final T mass) {
        final FieldVector3D<T> thrustForce = getFieldThrustAccelerationVector(adjointVariables, mass).scalarMultiply(mass);
        return thrustForce.getNormSq().multiply(-1. / 2.);
    }

    /**
     * Field event detector for singularities in adjoint dynamics.
     */
    class FieldSingularityDetector extends FieldControlSwitchDetector<T> {

        /** Value to detect. */
        private final T detectionValue;

        /**
         * Constructor.
         * @param detectionSettings detection settings
         * @param detectionValue value to detect
         */
        FieldSingularityDetector(final FieldEventDetectionSettings<T> detectionSettings, final T detectionValue) {
            super(detectionSettings);
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

    }
}
